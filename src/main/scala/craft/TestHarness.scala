package craft

import util.GeneratorApp
import cde.Parameters
import diplomacy.LazyModule

import chisel3._
import dspblocks._
import dspjunctions._
import testchipip._

class TestHarness(implicit val p: Parameters) extends Module {
  //implicit val options = chisel3.core.ExplicitCompileOptions.NotStrict
  // only works for single-chain design for now...
  //val firstBlockId = p(DspChainKey(p(DspChainId))).asInstanceOf[DspChainParameters].blocks(0)._2
  //val firstBlockWidth = p(GenKey(firstBlockId)).genIn.getWidth * p(GenKey(firstBlockId)).lanesIn

  val io = IO(new Bundle {
    val success = Output(Bool())
    //val stream_in = Flipped(ValidWithSync(UInt( firstBlockWidth.W )))
    //val dsp_clock = Flipped(Bool())
  })

  val dsp_clock = Reg(init = false.B)
  dsp_clock := !dsp_clock

  val dut = Module(new CraftP1Top)

  val ser = Module(new SimSerialWrapper(p(SerialInterfaceWidth)))
  ser.io.serial <> dut.io.serial
  io.success := ser.io.exit
}

object Generator extends GeneratorApp {
  val longName = names.topModuleProject + "." +
                 names.topModuleClass + "." +
                 names.configs
  generateFirrtl
}

class CraftP1Top(implicit val p: Parameters) extends Module{
  val io = IO(new CraftTopBundle(p))
  val craft = LazyModule(new CraftP1Core(p)).module
  io <> craft.io
  io.elements.foreach{println(_)}
  
  // [stevo]: loopy loop
  craft.clock := craft.io.CLKRXOUT

  // Pads go here
}
