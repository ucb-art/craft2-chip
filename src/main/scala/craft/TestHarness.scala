package craft

import chisel3._
import dspblocks._
import dspjunctions._
// import testchipip._
import chisel3.experimental._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.DontTouch

class TestHarness(implicit val p: Parameters) extends MultiIOModule with HasADCTopLevelIO with RealAnalogAnnotator with HasCoreReset with HasDspReset with CLKRXTopLevelInIO {
  val io = IO(new Bundle {
    val success = Output(Bool())
  })
  annotateReal()

  val dut = Module(LazyModule(new CraftP1Core).module)

  dut.CLKRXVIP := CLKRXVIP
  dut.CLKRXVIN := CLKRXVIN
  attach(dut.ADCBIAS, ADCBIAS)
  attach(dut.ADCINP, ADCINP)
  attach(dut.ADCINM, ADCINM)
  dut.ADCCLKP := ADCCLKP
  dut.ADCCLKM := ADCCLKM
  dut.adcclkreset := adcclkreset
  dut.core_reset := core_reset
  dut.dsp_reset := dsp_reset

  dut.debug := DontCare
  dut.connectSimAXIMem()
  dut.dontTouchPorts()
  dut.tieOffInterrupts()
  io.success := dut.connectSimSerial()
}

// class CraftP1Core(implicit val p: Parameters) extends Module with RealAnalogAnnotator {
//   val io = IO(new CraftP1CoreBundle(p))
//   val craft = LazyModule(new CraftP1CoreTop(p)).module
//   annotateReal()
// 
//   // core clock and reset
//   // loopback the clock receiver output into the core top
//   val core_clock = craft.io.clkrxvobuf
//   craft.clock := core_clock
//   val core_reset = ResetSync(io.core_reset, core_clock)
//   craft.reset := core_reset
// 
//   // ADC
//   // adc digital
//   craft.io.adcclkreset := io.adcclkreset
//   craft.io.dsp_reset := io.dsp_reset
//   // adc analog
//   attach(craft.io.ADCBIAS, io.ADCBIAS)
//   attach(craft.io.ADCINP, io.ADCINP)
//   attach(craft.io.ADCINM, io.ADCINM)
//   craft.io.ADCCLKP := io.ADCCLKP
//   craft.io.ADCCLKM := io.ADCCLKM
// 
//   // CLKRX
//   craft.io.CLKRXVIN := io.CLKRXVIN
//   craft.io.CLKRXVIP := io.CLKRXVIP
// 
//   // TSI, with reset synchronizers and Async FIFO
//   // note: TSI uses the normal "clock" and "reset" to this module
//   val tsi_reset = ResetSync(reset, clock)
//   craft.io.serial.in <> AsyncDecoupledCrossing(to_clock = core_clock, to_reset = core_reset, 
//     from_source = io.serial.in, from_clock = clock, from_reset = tsi_reset,
//     depth = 8, sync = 3)
//   io.serial.out <> AsyncDecoupledCrossing(to_clock = clock, to_reset = tsi_reset,
//     from_source = craft.io.serial.out, from_clock = core_clock, from_reset = core_reset,
//     depth = 8, sync = 3)
// 
// }


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
