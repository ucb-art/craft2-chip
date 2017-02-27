// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._

class ADCIO extends Bundle {
  val VDDHADC = Analog(1.W)
  val VDDADC  = Analog(1.W)
  val VSS     = Analog(1.W)
  val ADCBIAS = Analog(1.W)
  val CLKRST  = Input(Bool())
  val ADCINP      = Analog(1.W)
  val ADCINM      = Analog(1.W)
  val ADCCLKP     = Analog(1.W)
  val ADCCLKM     = Analog(1.W)
  val EXTCLK0     = Input(Bool())
  val EXTCLK1     = Input(Bool())
  val EXTCLK2     = Input(Bool())
  val EXTCLK3     = Input(Bool())
  val EXTCLK4     = Input(Bool())
  val EXTCLK5     = Input(Bool())
  val EXTCLK6     = Input(Bool())
  val EXTCLK7     = Input(Bool())
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
  val CLKGCAL0    = Input(UInt(8.W))
  val CLKGCAL1    = Input(UInt(8.W))
  val CLKGCAL2    = Input(UInt(8.W))
  val CLKGCAL3    = Input(UInt(8.W))
  val CLKGCAL4    = Input(UInt(8.W))
  val CLKGCAL5    = Input(UInt(8.W))
  val CLKGCAL6    = Input(UInt(8.W))
  val CLKGCAL7    = Input(UInt(8.W))
  val CLKGBIAS    = Input(UInt(8.W))
  val ADCOUT0     = Output(UInt(8.W))
  val ADCOUT1     = Output(UInt(8.W))
  val ADCOUT2     = Output(UInt(8.W))
  val ADCOUT3     = Output(UInt(8.W))
  val ADCOUT4     = Output(UInt(8.W))
  val ADCOUT5     = Output(UInt(8.W))
  val ADCOUT6     = Output(UInt(8.W))
  val ADCOUT7     = Output(UInt(8.W))
  val CLKOUT_DES  = Output(Clock())
}

class ADC extends BlackBox {
  val io = IO(new ADCIO)
}

class DeserIO extends Bundle {
  val in           = Input(Vec(8, UInt(9.W)))
  val out          = Output(Vec(32, UInt(9.W)))
  val clk_in       = Input(Clock())
  // clock that follows data to fifo
  val clk_out_data = Output(Clock())
  // clock that goes to rest of the dsp chain
  val clk_out_chip = Output(Clock())
}

class Deser extends BlackBox {
  val io = IO(new DeserIO)
}

class ADCCalIO extends Bundle {
  val in  = Input(Vec(32, UInt(9.W)))
  val out = Output(Vec(32, UInt(9.W)))
}

class ADCCal extends Module {
  val io = IO(new ADCCalIO)
  io.out := io.in
}
