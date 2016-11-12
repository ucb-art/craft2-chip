package craft

import chisel3._
import example._
import cde.Parameters

class ExampleTopWithCraft2DSP(q: Parameters) extends ExampleTop(q)
    with PeripheryCraft2DSP {
  override lazy val module = Module(
    new ExampleTopWithCraft2DSPModule(p, this, new ExampleTopBundle(p)))
}

class ExampleTopWithCraft2DSPModule(p: Parameters, l: ExampleTopWithCraft2DSP, b: ExampleTopBundle)
  extends ExampleTopModule(p, l, b) with PeripheryCraft2DSPModule
