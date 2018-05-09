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

case class PowerKey(id: String) extends Field[PowerConfig]

case class PowerConfig(val lanes: Int = 8, val pipelineDepth: Int = 0, quadrature: Boolean = true) {
  require(lanes > 0, "Power block must have more than 0 input lanes")
  require(pipelineDepth >= 0, "Power block must have positive pipelining")
  // note quadrature does nothing since this is a lane-sliced function
}

class PowerBlock[T <: Data:Real]()(implicit p: Parameters) extends DspBlock()(p) {
  def controls = Seq()
  def statuses = Seq()

  lazy val module = new PowerModule(this)

  addStatus("Data_Set_End_Status")
  addControl("Data_Set_End_Clear", 0.U)

}

class PowerModule[T <: Data:Real](outer: PowerBlock[T])(implicit p: Parameters) extends GenDspBlockModule[T, T](outer)(p) {
  val config = p(PowerKey(p(DspBlockId)))
  val module = Module(new Power[T])
  module.io.in <> unpackInput(lanesIn, genIn())
  unpackOutput(lanesOut, genOut()) <> module.io.out
  status("Data_Set_End_Status") := module.io.data_set_end_status
  module.io.data_set_end_clear := control("Data_Set_End_Clear")
}

class Power[T <: Data:Real]()(implicit val p: Parameters) extends Module with HasGenParameters[T, T] {
  val config: PowerConfig = p(PowerKey(p(DspBlockId)))

  val io = IO(new Bundle {
    val in = Input(ValidWithSync(Vec(config.lanes, genIn())))
    val out = Output(ValidWithSync(Vec(config.lanes, genOut())))
    val data_set_end_status = Output(Bool())
    val data_set_end_clear = Input(Bool())
  })

  // delay the data set signals
  val latency = config.pipelineDepth
  io.out.sync := ShiftRegisterWithReset(io.in.sync, latency, 0.U)
  io.out.valid := ShiftRegisterWithReset(io.in.valid, latency, 0.U)

  // feed in zeros when invalid
  val in = Wire(Vec(config.lanes, genIn()))
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

object PowerConfigBuilder {
  def apply[T <: Data:Real] (
    id: String, powerConfig: PowerConfig, genIn: () => T, genOut: Option[() => T]): Config = new Config(
      (pname, site, here) => pname match {
        case PowerKey(_id) if _id == id => powerConfig
        case IPXactParameters(_id) if _id == id => Map[String, String]()
        case _ => throw new CDEMatchError
      }
    ) ++
    ConfigBuilder.genParams(id, powerConfig.lanes, () => DspComplex(genIn(), genIn()), genOutFunc = genOut)

  def standalone[T <: Data : Real](id: String, powerConfig: PowerConfig, genIn: () => T, genOut: Option[() => T] = None): Config =
    apply(id, powerConfig, genIn, genOut) ++
    ConfigBuilder.buildDSP(id, {implicit p: Parameters => new PowerBlock[T]})
}
