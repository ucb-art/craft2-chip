package craft

import chisel3._
import chisel3.util._
import dsptools._
import dsptools.numbers._
import dspjunctions._
import dspblocks._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import scala.collection.mutable.Map

case class AccumulatorConfig[T <: Data](
  genIn: T,
  genOut: T,
  lanes: Int = 8,
  outputWindowSize: Int = 8,
  maxSpectra: Int = 128,
  quadrature: Boolean = true
) {
  val lanes_new = if (quadrature) lanes/2 else lanes
  require(lanes_new > 0, "Accumulator block must have more than 0 input lanes")
  require(maxSpectra > 1, "Accumulating only 1 spectrum is trivial")
  require(outputWindowSize >= lanes_new, "Must have bigger or equally sized window than number of lanes")
}

class AccumulatorBlock[T <: Data:Real](val config: AccumulatorConfig[T])(implicit p: Parameters) extends TLDspBlock with TLHasCSR {
  override val streamNode = AXI4StreamIdentityNode()

  val csrAddress = AddressSet(0x3000, 0x0fff)
  val beatBytes = 8
  val devname = "tlaccum"
  val devcompat = Seq("ucb-art", "accum")
  val device = new SimpleDevice(devname, devcompat) {
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping)
    }
  }

  override val mem = Some(TLRegisterNode(address = Seq(csrAddress), device = device, beatBytes = beatBytes))

  lazy val module = new AccumulatorModule(this)
}

class AccumulatorModule[T <: Data:Real](outer: AccumulatorBlock[T])(implicit p: Parameters) extends LazyModuleImp(outer) {
  val (in, inP) = outer.streamNode.in.head
  val (out, outP) = outer.streamNode.out.head

  val config = outer.config
  val module = Module(new Accumulator[T](config))

  module.io.in.valid := in.valid
  module.io.in.sync := in.bits.last
  module.io.in.bits := in.bits.data.asTypeOf(module.io.in.bits.cloneType)

  out.valid := module.io.out.valid
  out.bits.last := module.io.out.sync
  out.bits.data := module.io.out.bits.asUInt

  in.ready := out.ready

  val dataSetEndClear = RegInit(false.B)
  val numSpectraToAccumulate = RegInit(0.U(64.W))
  val resetAccumulations = RegInit(false.B)

  module.io.data_set_end_clear := dataSetEndClear
  module.io.num_spectra_to_accumulate := numSpectraToAccumulate
  module.io.reset_accumulations := resetAccumulations

  outer.regmap(
    0x00 -> Seq(RegField.r(64, module.io.num_spectra_accumulated)),
    0x08 -> Seq(RegField(1, dataSetEndClear)),
    0x10 -> Seq(RegField(64, numSpectraToAccumulate)),
    0x18 -> Seq(RegField(1, resetAccumulations)),
    0x20 -> Seq(RegField.r(1, module.io.data_set_end_status)),
  )
}

class Accumulator[T <: Data:Real](val config: AccumulatorConfig[T])(implicit val p: Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Input(ValidWithSync(Vec(config.lanes, config.genIn)))
    val out = Output(ValidWithSync(Vec(config.lanes, config.genOut)))

    val data_set_end_status = Output(Bool())
    val data_set_end_clear = Input(Bool())

    val num_spectra_to_accumulate = Input(UInt(64.W))
    val num_spectra_accumulated = Output(UInt(64.W))
    val reset_accumulations = Input(Bool())
  })
  val lanes_new = config.lanes_new

  // feed in zeros when invalid
  val in = Wire(Vec(config.lanes, config.genIn))
  when (io.in.valid) {
    in := io.in.bits
  } .otherwise {
    in := Vec.fill(config.lanes)(Real[T].zero)
  }

  // data set end flag
  val valid_delay = Reg(next=io.out.valid)
  val dses = Reg(init=false.B)
  when (io.data_set_end_clear) {
    dses := false.B
  } .elsewhen (valid_delay & ~io.out.valid) {
    dses := true.B
  }
  io.data_set_end_status := dses

  // state machine and counter wires
  val sIdle :: sAccum :: sOutput :: sWait :: sReset :: Nil = Enum(Bits(), 5)
  val sState = Reg(UInt(3.W), init = sIdle)
  val subspectrum_counter_sync_reset = Wire(Bool())
  subspectrum_counter_sync_reset := false.B
  val subspectrum_counter_comb_reset = Wire(Bool())
  subspectrum_counter_comb_reset := false.B
  val spectrum_counter_sync_reset = Wire(Bool())
  spectrum_counter_sync_reset := false.B
  val spectrum_counter_comb_reset = Wire(Bool())
  spectrum_counter_comb_reset := false.B
  val in_valid_delay = Reg(next=io.in.valid)
  val reset_accumulations_delay = Reg(next=io.reset_accumulations)
  val accum = Wire(Bool())
  accum := false.B

  // counters
  // increment condition, length, synchronous reset, combinational reset
  val subspectrum_counter = CounterWithReset(true.B, config.outputWindowSize/lanes_new, subspectrum_counter_sync_reset, subspectrum_counter_comb_reset)
  val spectrum_counter = CounterWithReset(subspectrum_counter._2, config.maxSpectra, spectrum_counter_sync_reset, spectrum_counter_comb_reset)

  // always move to idle when invalid
  when (~io.in.valid | io.reset_accumulations) {
    sState := sIdle
  }

  // data set signals
  io.out.sync := ShiftRegister(io.in.sync, config.outputWindowSize/lanes_new)
  io.out.valid := false.B // default

  // should we output the spectrum?
  // note: when num_spectra_to_accumulate is zero we never output it
  val toOutput = (spectrum_counter._2 || (subspectrum_counter._2 && spectrum_counter._1 >= (io.num_spectra_to_accumulate - 1.U))) && io.num_spectra_to_accumulate != 0.U

  // state machine
  // note this generally assumes valid is high for long periods of time. if valid toggles often, the accumulator will reset often and not work
  // TODO: add option to output shift register accumulator every spectrum instead of only when finished
  when (sState === sIdle) {
    printf("state = idle\n")
    // start accumulating on rising valid
    when (~in_valid_delay && io.in.valid && ~io.reset_accumulations) {
      when (io.num_spectra_to_accumulate != 0.U) {
        accum := true.B
        sState := sReset
        subspectrum_counter_comb_reset := true.B
        spectrum_counter_sync_reset := true.B
      } .otherwise {
        sState := sWait
      }
    // wait for sync when reset_accumulations signal goes low
    } .elsewhen (io.in.valid && reset_accumulations_delay && ~io.reset_accumulations) {
      sState := sWait
    }
  // used when coming out of idle; flushes the accumulators but doesn't output data
  } .elsewhen (sState === sReset) {
    printf("state = reset\n")
    printf("subspectrum counter = %x\n", subspectrum_counter._1)
    printf("spectrum counter = %x\n", spectrum_counter._1)
    accum := true.B
    when (subspectrum_counter._2) {
      sState := sAccum
    }
    when (toOutput) {
      sState := sOutput
      spectrum_counter_sync_reset := true.B
    }
  // do the accumulate thing
  } .elsewhen (sState === sAccum) {
    printf("state = accum\n")
    printf("subspectrum counter = %x\n", subspectrum_counter._1)
    printf("spectrum counter = %x\n", spectrum_counter._1)
    accum := true.B
    when (toOutput) {
      sState := sOutput
      spectrum_counter_sync_reset := true.B
    }
  // done the accumulate thing, so output the results and flush the accumulator memory
  } .elsewhen (sState === sOutput) {
    printf("state = output\n")
    printf("subspectrum counter = %x\n", subspectrum_counter._1)
    printf("spectrum counter = %x\n", spectrum_counter._1)
    accum := true.B
    io.out.valid := true.B
    when (subspectrum_counter._2) {
      sState := sAccum
    }
    when (toOutput) {
      sState := sOutput
      spectrum_counter_sync_reset := true.B
    }
  } .elsewhen (sState === sWait) {
    printf("state = wait\n")
    when (io.in.sync && io.num_spectra_to_accumulate != 0.U) {
      sState := sReset
      subspectrum_counter_sync_reset := true.B
      spectrum_counter_sync_reset := true.B
    }
  }

  // always go to idle on invalid input
  when (~io.in.valid || io.reset_accumulations) {
    sState := sIdle
  }

  io.num_spectra_accumulated := spectrum_counter._1

  // reset when outputting accumulated spectra
  val accum_in = Vec(in.zip(io.out.bits).map { case (i, o) => Mux(sState != sAccum, i, DspContext.withOverflowType(dsptools.Wrap) {o+i} ) })
  // TODO : undo this when real SRAMs are available 
  // [stevo]: large SRAMs have max width of 64; there is a 512x128 though, so if the depth is 512, this should be combined
  io.out.bits.zip(accum_in).foreach { case (o,i) =>
    o := ShiftRegisterMem(i, config.outputWindowSize/lanes_new, accum)
  }
  //io.out.bits := ShiftRegisterMem(accum_in, config.outputWindowSize/lanes_new, accum)
}
