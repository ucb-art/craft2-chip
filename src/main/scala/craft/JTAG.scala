package craft

import chisel3._
import chisel3.experimental._
import cde._
import chisel3.core.ExplicitCompileOptions.NotStrict

trait JTAGTopLevelIO {
  val trst       = Input(Bool())
  val tms        = Input(Bool())
  val tdi        = Input(Bool())
  val tdo        = Output(Bool())
  val tclk       = Input(Bool())
  val tdo_driven = Output(Bool())
}

trait JTAGTestLevelIO {
  val trst       = Input(Bool())
  val tms        = Input(Bool())
  val tdi        = Input(Bool())
  val tdo        = Output(Bool())
  val tclk       = Input(Bool())
  val tdo_driven = Output(Bool())
}

