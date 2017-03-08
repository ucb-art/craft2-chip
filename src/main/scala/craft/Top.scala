package craft

import chisel3._
import rocketchip._
import cde.Parameters
import testchipip._

// import chisel3.core.ExplicitCompileOptions.NotStrict

class CraftP1CoreTop(q: Parameters) extends BaseTop(q)
    with PeripheryBootROM with PeripheryCoreplexLocalInterrupter
    with PeripherySerial
    with PeripheryCraft2DSP
    with PeripherySRAM {
  override lazy val module = Module(
    new CraftTopModule(p, this, new CraftTopBundle(p)))
}

class CraftTopBundle(p: Parameters) extends BaseTopBundle(p)
  with PeripheryBootROMBundle
  with PeripheryCoreplexLocalInterrupterBundle
  with PeripherySerialBundle
  with ADCTopLevelIO
  with CLKRXTopLevelIO
  with PeripherySRAMBundle

class CraftTopModule(p: Parameters, l: CraftP1CoreTop, b: CraftTopBundle)
  extends BaseTopModule(p, l, b)
  with PeripheryBootROMModule
  with PeripheryCoreplexLocalInterrupterModule
  with PeripherySerialModule
  with PeripheryCraft2DSPModule
  with PeripherySRAMModule
  with HardwiredResetVector with DirectConnection with NoDebug
  with CLKRXModule

