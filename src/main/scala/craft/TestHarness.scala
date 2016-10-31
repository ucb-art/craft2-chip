package craft

import util.GeneratorApp
import cde.Parameters
import diplomacy.LazyModule

class TestHarness(q: Parameters) extends example.TestHarness()(q) {
  override def buildTop(p: Parameters) =
    LazyModule(new ExampleTopWithTest(p))
}

object Generator extends GeneratorApp {
  val longName = names.topModuleProject + "." +
                 names.topModuleClass + "." +
                 names.configs
  generateFirrtl
}
