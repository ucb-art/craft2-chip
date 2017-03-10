package craft

import util.GeneratorApp
import cde.Parameters
import diplomacy.LazyModule

import chisel3._
import dspblocks._
import dspjunctions._
import testchipip._
import chisel3.experimental._

class TestHarnessIO extends Bundle with CLKRXTopLevelInIO with ADCTopLevelIO {
  val success = Output(Bool())
}

class TestHarness(implicit val p: Parameters) extends Module {

  val io = IO(new TestHarnessIO)

  val dut = Module(new CraftP1Core)
  attach(dut.io.VIP, io.VIP)
  attach(dut.io.VIN, io.VIN)

  val ser = Module(new SimSerialWrapper(p(SerialInterfaceWidth)))
  ser.io.serial <> dut.io.serial
  io.success := ser.io.exit
}

class CraftP1Core(implicit val p: Parameters) extends Module{
  val io = IO(new CraftP1CoreBundle(p))
  val craft = LazyModule(new CraftP1CoreTop(p)).module
  craft.clock := craft.io.VOBUF

  // bulk connect doesn't work anymore :(
  craft.io.serial <> io.serial
  craft.io.EXTCLK := io.EXTCLK
  craft.io.CLKRST := io.CLKRST

  attach(craft.io.ADCBIAS, io.ADCBIAS)
  attach(craft.io.ADCINP, io.ADCINP)
  attach(craft.io.ADCINM, io.ADCINM)
  attach(craft.io.ADCCLKP, io.ADCCLKP)
  attach(craft.io.ADCCLKM, io.ADCCLKM)
  attach(craft.io.VIN, io.VIN)
  attach(craft.io.VIP, io.VIP)


  // Pads go here
}


// here's a diagram:

//---------------------------|--|--|--------------------------------------
//|  CraftP1Core:            |  |  |      
//|                          |  |  |      ----------      (floating)
//|                          |  |  |      |        ^          ^ 
//|     ---------------------|--|--|------|--------|----------|------------
//|     | CraftP1CoreTop     |  |  |      v        |          |
//|     |                   misc pins   clock  clkrx_out   success
//|     |
//|     |
//|     |
//|     |
//|     |
