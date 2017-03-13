package craft

import chisel3._
import rocketchip._
import cde.Parameters
import testchipip._
import uart._

// import chisel3.core.ExplicitCompileOptions.NotStrict

class CraftP1CoreTop(q: Parameters) extends BaseTop(q)
    with PeripheryBootROM with PeripheryCoreplexLocalInterrupter
    with PeripherySerial
    with PeripheryCraft2DSP
    with PeripheryUART
    with PeripherySRAM {
  // it looks like module is invoked by the constructor,
  // so adc_clk_out needs to be a lazy val to avoid null
  // pointer exceptions
  lazy val adc_clk_out = Wire(Clock())
  override lazy val module = {
    val mod = Module(
      new CraftP1CoreTopModule(p, this, new CraftP1CoreTopBundle(p))
    )
    adc_clk_out := mod.io.adc_clk_out
    mod
  }
}

trait WithCraftP1CoreBundle extends PeripheryBootROMBundle
  with PeripheryCoreplexLocalInterrupterBundle
  with PeripherySerialBundle
  with ADCTopLevelIO
  with CLKRXTopLevelInIO
  with PeripherySRAMBundle
  with PeripheryUARTBundle

// excludes success and clk receiver output
class CraftP1CoreBundle(val p: Parameters) extends Bundle
  with WithCraftP1CoreBundle

// add success and clock receiver output pins
class CraftP1CoreTopBundle(p: Parameters) extends BaseTopBundle(p)
  with WithCraftP1CoreBundle
  with CLKRXTopLevelOutIO
  with HasDspOutputClock


class CraftP1CoreTopModule(p: Parameters, l: CraftP1CoreTop, b: CraftP1CoreTopBundle)
  extends BaseTopModule(p, l, b)
  with PeripheryBootROMModule
  with PeripheryCoreplexLocalInterrupterModule
  with PeripherySerialModule
  with PeripheryCraft2DSPModule
  with PeripherySRAMModule
  with HardwiredResetVector with DirectConnection with NoDebug
  with PeripheryUARTModule
  with CLKRXModule

