package craft

import chisel3._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.DontTouch
import testchipip._

class CraftP1Core(implicit p: Parameters) extends RocketSubsystem
    with CanHaveMasterAXI4MemPort
    with HasPeripheryBootROM
    with HasSyncExtInterrupts
    with HasNoDebug
    with HasPeripheryCraft2DSP
    with HasPeripherySerial {
  override lazy val module = new CraftP1CoreTopModule(this)
}

class CraftP1CoreTopModule[+L <: CraftP1Core](l: L) extends RocketSubsystemModuleImp(l)
    with HasRTCModuleImp
    with CanHaveMasterAXI4MemPortModuleImp
    with HasPeripheryBootROMModuleImp
    with HasNoDebugModuleImp
    with HasExtInterruptsModuleImp
    with HasPeripherySerialModuleImp
    with DontTouch
    // with HasPeripherySerialModuleImp
    // with HasPeripherySerialModuleImp
    // with DontTouch
    with HasADCTopLevelIO
    with HasCoreReset
    with HasDspReset
    with HasPeripheryCraft2DSPModule
    // with HardwiredResetVector with DirectConnection
    // with RealAnalogAnnotator
    with CLKRX {

  // val deserio = IO(new DeserIO)



  // var string = ""
  // for (entry <- p(GlobalAddrMap).flatten) {
  //   val name = entry.name
  //   val start = entry.region.start
  //   val end = entry.region.start + entry.region.size - 1
  //   val prot = entry.region.attr.prot
  //   val protStr = (if ((prot & AddrMapProt.R) > 0) "R" else "") +
  //                 (if ((prot & AddrMapProt.W) > 0) "W" else "") +
  //                 (if ((prot & AddrMapProt.X) > 0) "X" else "")
  //   val cacheable = if (entry.region.attr.cacheable) " [C]" else ""
  //   string = string + f"$name%s $start%x - $end%x, $protStr$cacheable\n"
  // }

  // AddrMapStringOutput.contents = Some(string) 
    
  // annotateReal()
}

trait HasCoreReset extends experimental.MultiIOModule {
  val core_reset = IO(Input(Bool()))
}
