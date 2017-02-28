package craft

import util.GeneratorApp
import cde.Parameters
import diplomacy.LazyModule

import chisel3._
import dspblocks._
import dspjunctions._


class TestHarness(q: Parameters) extends example.TestHarness()(q) {
  implicit val options = chisel3.core.ExplicitCompileOptions.NotStrict
  override def buildTop(p: Parameters) =
    LazyModule(new ExampleTopWithCraft2DSP(p))
}

object Generator extends GeneratorApp {
  val longName = names.topModuleProject + "." +
                 names.topModuleClass + "." +
                 names.configs
  generateFirrtl
}
