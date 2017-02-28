// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._

class ADCIO extends Bundle {
  val VDDHADC = Analog(1.W)
  val VDDADC  = Analog(1.W)
  val VSS     = Analog(1.W)
  val ADCBIAS = Analog(1.W)
  val ADCINP      = Analog(1.W)
  val ADCINM      = Analog(1.W)
  val ADCCLKP     = Analog(1.W)
  val ADCCLKM     = Analog(1.W)
  val EXT_CLK0     = Input(Bool())
  val EXT_CLK1     = Input(Bool())
  val EXT_CLK2     = Input(Bool())
  val EXT_CLK3     = Input(Bool())
  val EXT_CLK4     = Input(Bool())
  val EXT_CLK5     = Input(Bool())
  val EXT_CLK6     = Input(Bool())
  val EXT_CLK7     = Input(Bool())
  val OSP0        = Input(UInt(8.W))
  val OSP1        = Input(UInt(8.W))
  val OSP2        = Input(UInt(8.W))
  val OSP3        = Input(UInt(8.W))
  val OSP4        = Input(UInt(8.W))
  val OSP5        = Input(UInt(8.W))
  val OSP6        = Input(UInt(8.W))
  val OSP7        = Input(UInt(8.W))
  val OSM0        = Input(UInt(8.W))
  val OSM1        = Input(UInt(8.W))
  val OSM2        = Input(UInt(8.W))
  val OSM3        = Input(UInt(8.W))
  val OSM4        = Input(UInt(8.W))
  val OSM5        = Input(UInt(8.W))
  val OSM6        = Input(UInt(8.W))
  val OSM7        = Input(UInt(8.W))
  val ASCLKD0     = Input(UInt(4.W))
  val ASCLKD1     = Input(UInt(4.W))
  val ASCLKD2     = Input(UInt(4.W))
  val ASCLKD3     = Input(UInt(4.W))
  val ASCLKD4     = Input(UInt(4.W))
  val ASCLKD5     = Input(UInt(4.W))
  val ASCLKD6     = Input(UInt(4.W))
  val ASCLKD7     = Input(UInt(4.W))
  val EXTSEL_CLK0 = Input(Bool())
  val EXTSEL_CLK1 = Input(Bool())
  val EXTSEL_CLK2 = Input(Bool())
  val EXTSEL_CLK3 = Input(Bool())
  val EXTSEL_CLK4 = Input(Bool())
  val EXTSEL_CLK5 = Input(Bool())
  val EXTSEL_CLK6 = Input(Bool())
  val EXTSEL_CLK7 = Input(Bool())
  val VREF00      = Input(UInt(8.W))
  val VREF01      = Input(UInt(8.W))
  val VREF02      = Input(UInt(8.W))
  val VREF03      = Input(UInt(8.W))
  val VREF04      = Input(UInt(8.W))
  val VREF05      = Input(UInt(8.W))
  val VREF06      = Input(UInt(8.W))
  val VREF07      = Input(UInt(8.W))
  val VREF10      = Input(UInt(8.W))
  val VREF11      = Input(UInt(8.W))
  val VREF12      = Input(UInt(8.W))
  val VREF13      = Input(UInt(8.W))
  val VREF14      = Input(UInt(8.W))
  val VREF15      = Input(UInt(8.W))
  val VREF16      = Input(UInt(8.W))
  val VREF17      = Input(UInt(8.W))
  val VREF20      = Input(UInt(8.W))
  val VREF21      = Input(UInt(8.W))
  val VREF22      = Input(UInt(8.W))
  val VREF23      = Input(UInt(8.W))
  val VREF24      = Input(UInt(8.W))
  val VREF25      = Input(UInt(8.W))
  val VREF26      = Input(UInt(8.W))
  val VREF27      = Input(UInt(8.W))
  val IREF0       = Input(UInt(8.W))
  val IREF1       = Input(UInt(8.W))
  val IREF2       = Input(UInt(8.W))
  val CLKGCCAL0    = Input(UInt(8.W))
  val CLKGCCAL1    = Input(UInt(8.W))
  val CLKGCCAL2    = Input(UInt(8.W))
  val CLKGCCAL3    = Input(UInt(8.W))
  val CLKGCCAL4    = Input(UInt(8.W))
  val CLKGCCAL5    = Input(UInt(8.W))
  val CLKGCCAL6    = Input(UInt(8.W))
  val CLKGCCAL7    = Input(UInt(8.W))
  val ADCOUT0     = Output(UInt(9.W))
  val ADCOUT1     = Output(UInt(9.W))
  val ADCOUT2     = Output(UInt(9.W))
  val ADCOUT3     = Output(UInt(9.W))
  val ADCOUT4     = Output(UInt(9.W))
  val ADCOUT5     = Output(UInt(9.W))
  val ADCOUT6     = Output(UInt(9.W))
  val ADCOUT7     = Output(UInt(9.W))
  val CLKOUT  = Output(Clock())
}

class TISARADC extends BlackBox {
  val io = IO(new ADCIO)
}

class DeserIO extends Bundle {
  val in           = Input(Vec(8, UInt(9.W)))
  val out          = Output(Vec(32, UInt(9.W)))
  val clk          = Input(Clock())
  // clock that follows data to fifo
  val clkout_data = Output(Clock())
  // clock that goes to rest of the dsp chain
  val clkout_dsp = Output(Clock())
  // set the phase during reset
  val phi_init = Input(UInt(2.W))
}

class des72to288 extends BlackBox {
  val io = IO(new DeserIO)
}

class ADCCalIO extends Bundle {
  val in  = Input(Vec(32, UInt(9.W)))
  val out = Output(Vec(32, UInt(8.W)))
}

class ADCCal extends Module {
  val io = IO(new ADCCalIO)
  io.out := io.in
}
