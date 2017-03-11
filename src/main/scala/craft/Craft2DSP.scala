package craft

import chisel3._
import chisel3.util._
import cde.{Parameters, Field}
import dsptools._
import uncore.tilelink._
import uncore.converters._
import _root_.junctions._
import diplomacy._
import rocketchip._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspjunctions._
import dspblocks._

trait PeripheryCraft2DSP extends LazyModule { self =>
  val p: Parameters
  val pDevices: ResourceManager[AddrMapEntry]

  implicit val dspChainParams = p.alterPartial {
    case TLId => p(dspblocks.DspChainId)
  }
  val craftChain = LazyModule(new DspChainWithADC())
  println(s"craft2_control requests ${craftChain.ctrlMemSize.size}")
  println(s"craft2_data    requests ${craftChain.dataMemSize.size}")
  println(s"craft2_control gets ${PageAligner(craftChain.ctrlMemSize).size}")
  println(s"craft2_data    gets ${PageAligner(craftChain.dataMemSize).size}")
  pDevices.add(AddrMapEntry("craft2_control", PageAligner(craftChain.ctrlMemSize)))
  pDevices.add(AddrMapEntry("craft2_data",    PageAligner(craftChain.dataMemSize)))

  craftChain.ctrlBaseAddr    = () => {
    val addrMap = p(GlobalAddrMap)
    addrMap("io:pbus:craft2_control").start.toLong
  }
  craftChain.dataBaseAddr    = () => {
    val addrMap = p(GlobalAddrMap)
    addrMap("io:pbus:craft2_data").start.toLong
  }
  object PageAligner { //extends rocket.HasCoreParameters {
    val p = self.p
    def apply(x: BigInt): BigInt = {
      val pgSize = 1L << 12 //pgIdxBits
      val xModPgSize = x % pgSize
      if (xModPgSize == 0)
        x
      else
        x + pgSize - xModPgSize
    }
    def apply(x:MemSize): MemSize = {
      MemSize(apply(x.size), x.attr)
    }
  }
}

case object BuildCraft2DSP extends Field[(ClientUncachedTileLinkIO, ClientUncachedTileLinkIO, Bundle with ADCTopLevelIO, Parameters) => Unit]


trait PeripheryCraft2DSPModule extends HasPeripheryParameters {
  import chisel3.core.ExplicitCompileOptions.NotStrict

  implicit val p: Parameters
  val pBus: TileLinkRecursiveInterconnect
  def io: Bundle with ADCTopLevelIO with HasDspOutputClock
  val outer: PeripheryCraft2DSP
  val craftChain = outer.craftChain
  val control_port = pBus.port("craft2_control")
  val data_port    = pBus.port("craft2_data")

  println(s"Set base addresses in the lazy chain module")
  val craftChainModule = craftChain.module

  val dsp_clock = craftChainModule.io.adc_clk_out
  craftChainModule.clock := dsp_clock
  io.adc_clk_out := dsp_clock

  // add width adapter because Hwacha needs 128-bit TL
  craftChainModule.io.control_axi <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkTo(to_clock=dsp_clock, to_reset=craftChainModule.reset, TileLinkWidthAdapter(control_port, craftChainModule.ctrlXbarParams)))
  craftChainModule.io.data_axi <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkTo(to_clock=dsp_clock, to_reset=craftChainModule.reset, TileLinkWidthAdapter(data_port, craftChainModule.dataXbarParams)))
  io <> craftChainModule.io
}

