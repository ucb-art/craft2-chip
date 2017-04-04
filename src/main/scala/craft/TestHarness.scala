package craft

import util.GeneratorApp
import cde.Parameters
import diplomacy.LazyModule

import chisel3._
import dspblocks._
import dspjunctions._
import testchipip._
import chisel3.experimental._
import uart._
import _root_.util._

class TestHarnessIO extends Bundle with CLKRXTopLevelInIO with ADCTopLevelIO with UARTIO with CoreResetBundle with HasDspReset with JTAGTestLevelIO {
  val success = Output(Bool())
}

class TestHarness(implicit val p: Parameters) extends Module with RealAnalogAnnotator {
  val io = IO(new TestHarnessIO)
  annotateReal()

  val dut = Module(new CraftP1Core)
  dut.io.CLKRXVIP := io.CLKRXVIP
  dut.io.CLKRXVIN := io.CLKRXVIN
  attach(dut.io.ADCBIAS, io.ADCBIAS)
  attach(dut.io.ADCINP, io.ADCINP)
  attach(dut.io.ADCINM, io.ADCINM)
  dut.io.ADCCLKP := io.ADCCLKP
  dut.io.ADCCLKM := io.ADCCLKM
  dut.io.adcclkreset := io.adcclkreset
  dut.io.ua_rxd := io.ua_rxd
  io.ua_int := dut.io.ua_int
  io.ua_txd := dut.io.ua_txd
  dut.io.ua_clock := io.ua_clock
  dut.io.ua_reset := io.ua_reset
  dut.io.core_reset := io.core_reset
  dut.io.dsp_reset := io.dsp_reset
  dut.io.trst := io.trst
  dut.io.tms  := io.tms
  dut.io.tdi  := io.tdi
  io.tdo        := dut.io.tdo
  // [stevo]: shouldn't be included 
  //io.tdo_driven := dut.io.tdo_driven
  dut.io.tclk := io.tclk

  val ser = Module(new SimSerialWrapper(p(SerialInterfaceWidth)))
  ser.io.serial <> dut.io.serial
  io.success := ser.io.exit
}

class CraftP1Core(implicit val p: Parameters) extends Module with RealAnalogAnnotator {
  val io = IO(new CraftP1CoreBundle(p))
  val craft = LazyModule(new CraftP1CoreTop(p)).module
  annotateReal()

  // core clock and reset
  // loopback the clock receiver output into the core top
  val core_clock = craft.io.clkrxvobuf
  craft.clock := core_clock
  val core_reset = ResetSync(io.core_reset, core_clock)
  craft.reset := core_reset

  // ADC
  // adc digital
  craft.io.adcclkreset := io.adcclkreset
  craft.io.dsp_reset := io.dsp_reset
  // adc analog
  attach(craft.io.ADCBIAS, io.ADCBIAS)
  attach(craft.io.ADCINP, io.ADCINP)
  attach(craft.io.ADCINM, io.ADCINM)
  craft.io.ADCCLKP := io.ADCCLKP
  craft.io.ADCCLKM := io.ADCCLKM

  // JTAG
  craft.io.trst := io.trst
  craft.io.tms  := io.tms
  craft.io.tdi  := io.tdi
  io.tdo        := craft.io.tdo
  io.tdo_driven := craft.io.tdo_driven
  craft.io.tclk := io.tclk

  // CLKRX
  craft.io.CLKRXVIN := io.CLKRXVIN
  craft.io.CLKRXVIP := io.CLKRXVIP

  // UART, with reset synchronizers
  craft.io.ua_rxd := io.ua_rxd
  io.ua_int := craft.io.ua_int
  io.ua_txd := craft.io.ua_txd
  craft.io.ua_clock := io.ua_clock
  craft.io.ua_reset := ResetSync(io.ua_reset, io.ua_clock)

  // TSI, with reset synchronizers and Async FIFO
  // note: TSI uses the normal "clock" and "reset" to this module
  val tsi_reset = ResetSync(reset, clock)
  craft.io.serial.in <> AsyncDecoupledCrossing(to_clock = core_clock, to_reset = core_reset, 
    from_source = io.serial.in, from_clock = clock, from_reset = tsi_reset,
    depth = 8, sync = 3)
  io.serial.out <> AsyncDecoupledCrossing(to_clock = clock, to_reset = tsi_reset,
    from_source = craft.io.serial.out, from_clock = core_clock, from_reset = core_reset,
    depth = 8, sync = 3)

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
