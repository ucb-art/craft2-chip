package craft

import chisel3._
import chisel3.experimental._
import chisel3.util._
import dsptools._
import freechips.rocketchip.config.{Parameters, Field}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.BaseSubsystem
import dsptools.numbers._
import dspjunctions._
import dspblocks._
import testchipip._

case object ChainKey extends Field[Seq[Parameters => TLDspBlock]]

trait HasPeripheryCraft2DSP { this: BaseSubsystem =>
  val p: Parameters

  val portName = "dspchain"

  val craftChain = LazyModule(new DspChainWithADC(p(ChainKey)))
  craftChain.mem.foreach { m => pbus.toVariableWidthSlave(Some(portName)) { m } }
  // println(s"craft2_control requests ${craftChain.ctrlMemSize.size}")
  // println(s"craft2_data    requests ${craftChain.dataMemSize.size}")
  // println(s"craft2_control gets
  // ${PageAligner(craftChain.ctrlMemSize).size}") println(s"craft2_data
  // gets ${PageAligner(craftChain.dataMemSize).size}")
  // pDevices.add(AddrMapEntry("craft2_control",
  // PageAligner(craftChain.ctrlMemSize)))
  // pDevices.add(AddrMapEntry("craft2_data",
  // PageAligner(craftChain.dataMemSize)))

  // craftChain.ctrlBaseAddr    = () => { val addrMap = p(GlobalAddrMap)
  // addrMap("io:pbus:craft2_control").start.toLong } craftChain.dataBaseAddr
  // = () => { val addrMap = p(GlobalAddrMap)
  // addrMap("io:pbus:craft2_data").start.toLong } object PageAligner {
  // //extends rocket.HasCoreParameters { val p = self.p def apply(x: BigInt):
  // BigInt = { val pgSize = 1L << 12 //pgIdxBits val xModPgSize = x % pgSize
  // if (xModPgSize == 0) x else x + pgSize - xModPgSize } def
  // apply(x:MemSize): MemSize = { MemSize(apply(x.size), x.attr) } }
}


trait HasDspReset {
  val dsp_reset = IO(Input(Bool()))
}

trait HasPeripheryCraft2DSPModule extends LazyModuleImp with HasADCTopLevelIO
with HasDspOutputClock with HasDspReset {
  implicit val p: Parameters
  val outer: HasPeripheryCraft2DSP

  val craftChain = outer.craftChain
  val craftChainModule = craftChain.module
  // val control_port = pBus.port("craft2_control")
  // val data_port    = pBus.port("craft2_data")


  //val dsp_clock = craftChainModule.io.adc_clk_out
  val dsp_clock = ADCCLKP.asClock
  val dsp_reset_sync = ResetSync(dsp_reset, dsp_clock)
  craftChainModule.clock := dsp_clock
  craftChainModule.reset := dsp_reset_sync

  // craftChainModule.io.control_axi <> AsyncNastiTo(to_clock=dsp_clock, to_reset=dsp_reset, source=PeripheryUtils.convertTLtoAXI(control_port), depth=16)
  // craftChainModule.io.data_axi <> AsyncNastiTo(to_clock=dsp_clock, to_reset=dsp_reset, source=PeripheryUtils.convertTLtoAXI(data_port), depth=16)
  // io <> craftChainModule.io
}

