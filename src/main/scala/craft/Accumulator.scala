package craft

import cde.{Parameters, Field, Config, CDEMatchError}
import chisel3._
import chisel3.util._
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspjunctions._
import dspblocks._
import ipxact._
import craft._
import scala.collection.mutable.Map

case class AccumulatorKey(id: String) extends Field[AccumulatorConfig]

case class AccumulatorConfig(val lanes: Int = 8, outputWindowSize: Int = 4, maxSpectra: Int = 128) {
  require(lanes > 0, "Accumulator block must have more than 0 input lanes")
  require(maxSpectra > 1, "Accumulating only 1 spectrum is trivial")
  require(outputWindowSize > lanes, "Must have bigger window than number of lanes")
}

  class AccumulatorBlock[T <: Data:Real]()(implicit p: Parameters) extends DspBlock()(p) {
    def controls = Seq()
    def statuses = Seq()

    lazy val module = new AccumulatorModule(this)

    addStatus("Data_Set_End_Status")
    addControl("Data_Set_End_Clear", 0.U)

    addControl("NumSpectraToAccumulate", 0.U)
    addStatus("NumSpectraAccumulated")
    addControl("ResetAccumulations", 0.U)
  }

  class AccumulatorModule[T <: Data:Real](outer: AccumulatorBlock[T])(implicit p: Parameters) extends GenDspBlockModule[T, T](outer)(p) {
    val config = p(AccumulatorKey(p(DspBlockId)))
    val module = Module(new Accumulator[T])
    module.io.in <> unpackInput(lanesIn, genIn())
    unpackOutput(lanesOut, genOut()) <> module.io.out

    status("Data_Set_End_Status") := module.io.data_set_end_status
    module.io.data_set_end_clear := control("Data_Set_End_Clear")

    module.io.num_spectra_to_accumulate := control("NumSpectraToAccumulate")
    status("NumSpectraAccumulated") := module.io.num_spectra_accumulated
    module.io.reset_accumulations := control("ResetAccumulations")
  }

  class Accumulator[T <: Data:Real]()(implicit val p: Parameters) extends Module with HasGenParameters[T, T] {
    val config: AccumulatorConfig = p(AccumulatorKey(p(DspBlockId)))

    val io = IO(new Bundle {
      val in = Input(ValidWithSync(Vec(config.lanes, genIn())))
      val out = Output(ValidWithSync(Vec(config.lanes, genOut())))

      val data_set_end_status = Output(Bool())
      val data_set_end_clear = Input(Bool())

      val num_spectra_to_accumulate = Input(UInt(64.W))
      val num_spectra_accumulated = Output(UInt(64.W))
      val reset_accumulations = Input(Bool())
    })

    // feed in zeros when invalid
    val in = Wire(Vec(config.lanes, genIn()))
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
    val subspectrum_counter = CounterWithReset(true.B, config.outputWindowSize/config.lanes, subspectrum_counter_sync_reset, subspectrum_counter_comb_reset)
    val spectrum_counter = CounterWithReset(subspectrum_counter._2, config.maxSpectra, spectrum_counter_sync_reset, spectrum_counter_comb_reset)

    // always move to idle when invalid
    when (~io.in.valid | io.reset_accumulations) {
      sState := sIdle
    }

    // data set signals
    io.out.sync := ShiftRegister(io.in.sync, config.outputWindowSize/config.lanes)
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
    io.out.bits := ShiftRegisterMem(accum_in, config.outputWindowSize/config.lanes, accum)
}

object AccumulatorConfigBuilder {
  def apply[T <: Data:Real] (
    id: String, accumulatorConfig: AccumulatorConfig, genIn: () => T, genOut: Option[() => T]): Config = new Config(
      (pname, site, here) => pname match {
        case AccumulatorKey(_id) if _id == id => accumulatorConfig
        case IPXactParameters(_id) if _id == id => Map[String, String]()
        case _ => throw new CDEMatchError
      }
    ) ++
    ConfigBuilder.genParams(id, accumulatorConfig.lanes, genIn, genOutFunc = genOut)

  def standalone[T <: Data : Real](id: String, accumulatorConfig: AccumulatorConfig, genIn: () => T, genOut: Option[() => T] = None): Config =
    apply(id, accumulatorConfig, genIn, genOut) ++
    ConfigBuilder.buildDSP(id, {implicit p: Parameters => new AccumulatorBlock[T]})
}
