package craft

import chisel3._
import chisel3.util._
import dsptools._
import dsptools.numbers._
import dspjunctions._
import dspblocks._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import scala.collection.mutable.Map

case class PowerConfig[T <: Data](
  genIn: DspComplex[T],
  genOut: T,
  lanes: Int = 8,
  pipelineDepth: Int = 0,
  quadrature: Boolean = true
) {
  require(lanes > 0, "Power block must have more than 0 input lanes")
  require(pipelineDepth >= 0, "Power block must have positive pipelining")
  // note quadrature does nothing since this is a lane-sliced function
}

class PowerBlock[T <: Data:Real](val config: PowerConfig[T])(implicit p: Parameters) extends TLDspBlock with TLHasCSR {
  val streamNode = AXI4StreamIdentityNode()

  val csrAddress = AddressSet(0x5000, 0x0fff)
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

  lazy val module = new PowerModule(this)
}

class PowerModule[T <: Data:Real](outer: PowerBlock[T])(implicit p: Parameters) extends LazyModuleImp(outer) {
  val (in, inP) = outer.streamNode.in.head
  val (out, outP) = outer.streamNode.out.head

  val config = outer.config
  val module = Module(new Power[T](config))

  module.io.in.valid := in.valid
  module.io.in.sync := in.bits.last
  module.io.in.bits := in.bits.data.asTypeOf(module.io.in.bits.cloneType)

  out.valid := module.io.out.valid
  out.bits.last := module.io.out.sync
  out.bits.data := module.io.out.bits.asUInt

  in.ready := out.ready

  val dataSetEndClear = RegInit(false.B)
  module.io.data_set_end_clear := dataSetEndClear

  outer.regmap(
    0x0 -> Seq(RegField.r(1, module.io.data_set_end_status)),
    0x8 -> Seq(RegField(1, dataSetEndClear)),
  )
}

class Power[T <: Data:Real](val config: PowerConfig[T])(implicit val p: Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Input(ValidWithSync(Vec(config.lanes, config.genIn)))
    val out = Output(ValidWithSync(Vec(config.lanes, config.genOut)))
    val data_set_end_status = Output(Bool())
    val data_set_end_clear = Input(Bool())
  })

  // delay the data set signals
  val latency = config.pipelineDepth
  io.out.sync := ShiftRegisterWithReset(io.in.sync, latency, 0.U)
  io.out.valid := ShiftRegisterWithReset(io.in.valid, latency, 0.U)

  // feed in zeros when invalid
  val in = Wire(Vec(config.lanes, config.genIn))
  when (io.in.valid) {
    in := io.in.bits
  } .otherwise {
    in.foreach { _.real := Real[T].zero }
    in.foreach { _.imag := Real[T].zero }
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

  // take the power of the input
  io.out.bits.zip(in).map{case (o,i) => {
    val iReal = i.asInstanceOf[DspComplex[T]].real
    val iImag = i.asInstanceOf[DspComplex[T]].imag
    //o := ShiftRegister(iReal * iReal + iImag * iImag, config.pipelineDepth)
    o := ShiftRegister(iReal * iReal + iImag * iImag, config.pipelineDepth)
  }}
}
