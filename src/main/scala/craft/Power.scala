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
import craft._
import scala.collection.mutable.Map

case class PowerConfig[T <: Data](
  genIn: T,
  genOut: T,
  lanes: Int = 8,
  pipelineDepth: Int = 0,
  quadrature: Boolean = true
) {
  require(lanes > 0, "Power block must have more than 0 input lanes")
  require(pipelineDepth >= 0, "Power block must have positive pipelining")
  // note quadrature does nothing since this is a lane-sliced function
}

class PowerBlock[T <: Data:Real](val config: PowerConfig[T])(implicit p: Parameters) extends TLDspBlock /*with TLHasCSR*/ {
  val streamNode = AXI4StreamIdentityNode()
  val mem = None

  lazy val module = new PowerModule(this)

  // addStatus("Data_Set_End_Status")
  // addControl("Data_Set_End_Clear", 0.U)
}

class PowerModule[T <: Data:Real](outer: PowerBlock[T])(implicit p: Parameters) extends LazyModuleImp(outer) {
  val config = outer.config
  val module = Module(new Power[T](config))
  // module.io.in <> unpackInput(lanesIn, genIn())
  // unpackOutput(lanesOut, genOut()) <> module.io.out
  // status("Data_Set_End_Status") := module.io.data_set_end_status
  // module.io.data_set_end_clear := control("Data_Set_End_Clear")
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
    in := Vec.fill(config.lanes)(DspComplex(Real[T].zero, Real[T].zero))
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
