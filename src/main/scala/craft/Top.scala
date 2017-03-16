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

  override lazy val module = Module(new CraftP1CoreTopModule(p, this, new CraftP1CoreTopBundle(p)))
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
  with CoreResetBundle

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

trait CoreResetBundle {
  val core_reset = Input(Bool())
}
