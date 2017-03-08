package craft

import chisel3._
import chisel3.util._
import cde.{Parameters, Field}
import dsptools._
import uncore.tilelink._
import _root_.junctions._
import diplomacy._
import rocketchip._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspjunctions._
import dspblocks._

trait PeripheryExtra extends LazyModule {
  val pDevices: ResourceManager[AddrMapEntry]
  implicit val p: Parameters
  
  (0 until p(ExtraMMIOOutputs)).foreach { i =>
    pDevices.add(AddrMapEntry(s"periphery_extra_$i", MemSize(4096, MemAttr(AddrMapProt.RW))))
  }

}

trait PeripheryExtraBundle {
  implicit val p: Parameters
  
  val peripheryExtraAxi = Seq.fill(p(ExtraMMIOOutputs))(Flipped(new NastiIO()))
  val peripheryExtraClock = Seq.fill(p(ExtraMMIOOutputs))(Input(Bool()))
  val peripheryExtraReset = Seq.fill(p(ExtraMMIOOutputs))(Input(Bool()))

}

trait PeripheryExtraModule extends HasPeripheryParameters {
  implicit val p: Parameters
  val pBus: TileLinkRecursiveInterconnect
  def io: Bundle with PeripheryExtraBundle

  p(BuildPeripheryExtra)((0 until p(ExtraMMIOOutputs)).map(i => pBus.port(s"periphery_extra_$i")), io, p)
}

case object BuildPeripheryExtra extends Field[(Seq[ClientUncachedTileLinkIO], Bundle with PeripheryExtraBundle, Parameters) => Unit]

case object ExtraMMIOOutputs extends Field[Int]

