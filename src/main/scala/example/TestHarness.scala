package example

import util.GeneratorApp
import diplomacy.LazyModule
import rocketchip._
import testchipip._
import chisel3._
import cde.Parameters

import dspblocks._
import dspjunctions._

class TestHarness(implicit val p: Parameters) extends Module {
  // only works for single-chain design for now...
  val firstBlockId = p(DspChainKey(p(DspChainId))).asInstanceOf[DspChainParameters].blocks(0)._2
  val firstBlockWidth = p(GenKey(firstBlockId)).genIn.getWidth * p(GenKey(firstBlockId)).lanesIn

  val io = IO(new Bundle {
    val success = Output(Bool())
    val stream_in = Flipped(ValidWithSync(UInt( firstBlockWidth.W )))
    val dsp_clock = Flipped(Bool())
  })

  def buildTop(p: Parameters): ExampleTop = LazyModule(new ExampleTop(p))

  val dut = buildTop(p).module
  val ser = Module(new SimSerialWrapper(p(SerialInterfaceWidth)))

  val nMemChannels = dut.io.mem_axi.size
  for (axi <- dut.io.mem_axi) {
    val mem = Module(new SimAXIMem(BigInt(p(ExtMemSize) / nMemChannels)))
    mem.io.axi <> axi
  }

  ser.io.serial <> dut.io.serial
  io.success := ser.io.exit
}

object Generator extends GeneratorApp {
  val longName = names.topModuleProject + "." +
                 names.topModuleClass + "." +
                 names.configs
  generateFirrtl
}
