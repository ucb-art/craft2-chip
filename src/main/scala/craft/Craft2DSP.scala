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
import testchipip._

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


trait HasDspReset {
  val dsp_reset = Input(Bool())
}

trait PeripheryCraft2DSPModule extends HasPeripheryParameters {
  import chisel3.core.ExplicitCompileOptions.NotStrict

  implicit val p: Parameters
  val pBus: TileLinkRecursiveInterconnect
  def io: Bundle with ADCTopLevelIO
    with HasDspOutputClock with HasDspReset
    with JTAGTopLevelIO
  val outer: PeripheryCraft2DSP
  val craftChain = outer.craftChain
  val control_port = pBus.port("craft2_control")
  val data_port    = pBus.port("craft2_data")

  val craftChainModule = craftChain.module

  val dsp_clock = craftChainModule.io.adc_clk_out
  val dsp_reset = ResetSync(io.dsp_reset, dsp_clock)
  craftChainModule.clock := dsp_clock
  craftChainModule.reset := dsp_reset

  // JTAG (if it exists)
  craftChainModule.io.jtag.map({ case jtag =>
    jtag.TCK      := io.tclk
    jtag.TMS      := io.tms
    jtag.TDI      := io.tdi
    io.tdo        := jtag.TDO.data
    io.tdo_driven := jtag.TDO.driven
    // TRST doesn't exist yet
  })

  // add width adapter because Hwacha needs 128-bit TL
  craftChainModule.io.control_axi <> AsyncNastiTo(to_clock=dsp_clock, to_reset=dsp_reset, source=PeripheryUtils.convertTLtoAXI(TileLinkWidthAdapter(control_port, craftChainModule.ctrlXbarParams)))
  craftChainModule.io.data_axi <> AsyncNastiTo(to_clock=dsp_clock, to_reset=dsp_reset, source=PeripheryUtils.convertTLtoAXI(TileLinkWidthAdapter(data_port, craftChainModule.ctrlXbarParams)))
  io <> craftChainModule.io
}

