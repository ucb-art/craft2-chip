package craft

import chisel3._
import example._
import cde.Parameters

import dspblocks._
import dspjunctions._

// import chisel3.core.ExplicitCompileOptions.NotStrict

class ExampleTopWithCraft2DSP(q: Parameters) extends ExampleTop(q)
    with PeripheryCraft2DSP {
  override lazy val module = Module(
    new ExampleTopWithCraft2DSPModule(p, this, new CraftTopBundle(p)))
}

class CraftTopBundle(p: Parameters) extends ExampleTopBundle(p) {
  val firstBlockId = p(DspChainKey(p(DspChainId))).blocks.head._2
  val firstBlockWidth = p(GenKey(firstBlockId)).genIn.getWidth * p(GenKey(firstBlockId)).lanesIn
  val stream_in = Flipped(ValidWithSync(UInt( firstBlockWidth.W )))
}

class ExampleTopWithCraft2DSPModule(p: Parameters, l: ExampleTopWithCraft2DSP, b: CraftTopBundle)
  extends ExampleTopModule(p, l, b) with PeripheryCraft2DSPModule
