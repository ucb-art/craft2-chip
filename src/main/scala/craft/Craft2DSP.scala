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

trait PeripheryCraft2DSP extends LazyModule {
  val pDevices: ResourceManager[AddrMapEntry]

  pDevices.add(AddrMapEntry("craft2_control", MemSize(4096, MemAttr(AddrMapProt.RW))))
  pDevices.add(AddrMapEntry("craft2_data", MemSize(4096, MemAttr(AddrMapProt.RW))))
}

case object BuildCraft2DSP extends Field[(ClientUncachedTileLinkIO, ClientUncachedTileLinkIO, Bundle with ADCTopLevelIO, Parameters) => Unit]

trait PeripheryCraft2DSPBundle {
  implicit val p: Parameters
  val firstBlockId = p(DspChainKey(p(DspChainId))).blocks.head._2
  val firstBlockWidth = p(GenKey(firstBlockId)).genIn.getWidth * p(GenKey(firstBlockId)).lanesIn
  val stream_in = Flipped(ValidWithSync(UInt( firstBlockWidth.W )))
  val dsp_clock = Input(Bool())
}

trait PeripheryCraft2DSPModule extends HasPeripheryParameters {
  implicit val p: Parameters
  val pBus: TileLinkRecursiveInterconnect
  def io: Bundle with ADCTopLevelIO

  val dspChainParams = p.alterPartial {
    case TLId => p(dspblocks.DspChainId)
  }

  p(BuildCraft2DSP)(pBus.port("craft2_control"), pBus.port("craft2_data"), io, dspChainParams)

}
