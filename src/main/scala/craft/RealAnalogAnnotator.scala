package craft

import chisel3._
import chisel3.experimental._
// import chisel3.experimental.ChiselAnnotation
// import chisel3.util._
// import chisel3.testers.BasicTester
// import chisel3.experimental.{Analog, attach}
// import firrtl.ir.{AnalogType, Circuit, DefModule, Expression, HasName, Port, Statement, Type}
// import firrtl.{CircuitForm, CircuitState, LowForm, Transform}
// import firrtl.annotations.{Annotation, ModuleName, Named, ComponentName}
// import firrtl.Mappers._
// import barstools.tapeout.transforms._

trait RealAnalogAnnotator /*extends AnalogAnnotator */ { self: MultiIOModule =>

  def annotateReal(): Unit = {
    // io.elements.foreach {
    //   case (_, d) => {
    //     d match {
    //       case a: Analog =>
    //         renameAnalog(a, "input\n`ifndef SYNTHESIS\n  real\n`endif\n       ")
    //       case _ =>
    //     }
    //   }
    // }
  }

}
