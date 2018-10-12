package craft

import chisel3._
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspjunctions._
import dspblocks._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import scala.collection.mutable.Map

case class BitManipulationConfig[T](
  genIn: T,
  genOut: T,
  lanes: Int = 8
) {
  require(lanes > 0, "Bit manipulation block must have more than 0 input lanes")
}

class BitManipulationBlock[T <: Data:Ring](val config: BitManipulationConfig[T])(implicit p: Parameters) extends TLDspBlock /*with TLHasCSR*/ {
  override val streamNode = AXI4StreamIdentityNode()
  override val mem = None
  lazy val module = new BitManipulationModule(this)
}

class BitManipulationModule[T <: Data:Ring](val outer: BitManipulationBlock[T])(implicit p: Parameters) extends LazyModuleImp(outer) {
  val module = Module(new BitManipulation[T](outer.config))
  val (in, inP) = outer.streamNode.in.head
  val (out, outP) = outer.streamNode.out.head

  in.ready := out.ready

  module.io.in.valid := in.valid
  module.io.in.bits := in.bits.data.asTypeOf(Vec(outer.config.lanes, outer.config.genIn))
  module.io.in.sync := in.bits.last

  out.valid := module.io.out.valid
  out.bits.data := module.io.out.bits.asUInt
  out.bits.last := module.io.out.sync
}

class BitManipulation[T <: Data:Ring](config: BitManipulationConfig[T])(implicit val p: Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Input(ValidWithSync(Vec(config.lanes, config.genIn)))
    val out = Output(ValidWithSync(Vec(config.lanes, config.genOut)))
  })

  io.out := io.in
}

