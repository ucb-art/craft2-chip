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

  //val dsp_clock = craftChainModule.io.adc_clk_out
  val dsp_clock = ADCCLKP.asClock
  val dsp_reset_sync = ResetSync(dsp_reset, dsp_clock)
  craftChainModule.clock := dsp_clock
  craftChainModule.reset := dsp_reset_sync
}

