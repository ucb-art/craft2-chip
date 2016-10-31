package craft

import chisel3._
import example._
import cde.Parameters

class ExampleTopWithTest(q: Parameters) extends ExampleTop(q)
    with PeripheryTest {
  override lazy val module = Module(
    new ExampleTopWithTestModule(p, this, new ExampleTopBundle(p)))
}

class ExampleTopWithPWMModule(p: Parameters, l: ExampleTopWithTest, b: ExampledTopBundle)
  extends ExampleTopModule(p, l, b) with PeripheryTestModule
