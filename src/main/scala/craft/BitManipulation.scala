package craft

import cde.{Parameters, Field, Config, CDEMatchError}
import chisel3._
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspjunctions._
import dspblocks._
import ipxact._
import craft._
import scala.collection.mutable.Map

case class BitManipulationKey(id: String) extends Field[BitManipulationConfig]

case class BitManipulationConfig(val lanes: Int = 8) {
  require(lanes > 0, "Bit manipulation block must have more than 0 input lanes")
}

class BitManipulationBlock[T <: Data:Ring]()(implicit p: Parameters) extends DspBlock()(p) {
  lazy val module = new BitManipulationModule(this)
}

class BitManipulationModule[T <: Data:Ring](outer: BitManipulationBlock[T])(implicit p: Parameters) extends GenDspBlockModule[T, T](outer)(p) {
  val module = Module(new BitManipulation[T])
  module.io.in <> unpackInput(lanesIn, genIn())
  unpackOutput(lanesOut, genOut()) <> module.io.out
}

class BitManipulation[T <: Data:Ring]()(implicit val p: Parameters) extends Module with HasGenParameters[T, T] {
  val config: BitManipulationConfig = p(BitManipulationKey(p(DspBlockId)))

  val io = IO(new Bundle {
    val in = Input(ValidWithSync(Vec(config.lanes, genIn())))
    val out = Output(ValidWithSync(Vec(config.lanes, genOut())))
  })

  io.out := io.in
}

object BitManipulationConfigBuilder {
  def apply[T <: Data:Ring] (
    id: String, bmConfig: BitManipulationConfig, genIn: () => T, genOut: () => T): Config = new Config(
      (pname, site, here) => pname match {
        case BitManipulationKey(_id) if _id == id => bmConfig
        case IPXactParameters(_id) if _id == id => Map[String, String]()
        case _ => throw new CDEMatchError
      }
    ) ++
    ConfigBuilder.genParams(id, bmConfig.lanes, genIn, genOutFunc = Some(genOut))
}
