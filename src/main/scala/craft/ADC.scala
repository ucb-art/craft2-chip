// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._

class ADCIO extends Bundle {
  val ADCBIAS     = Analog(1.W)
  val ADCINP      = Analog(1.W)
  val ADCINM      = Analog(1.W)
  val ADCCLKP     = Input(Bool())
  val ADCCLKM     = Input(Bool())
  val osp0        = Input(UInt(8.W))
  val osp1        = Input(UInt(8.W))
  val osp2        = Input(UInt(8.W))
  val osp3        = Input(UInt(8.W))
  val osp4        = Input(UInt(8.W))
  val osp5        = Input(UInt(8.W))
  val osp6        = Input(UInt(8.W))
  val osp7        = Input(UInt(8.W))
  val osm0        = Input(UInt(8.W))
  val osm1        = Input(UInt(8.W))
  val osm2        = Input(UInt(8.W))
  val osm3        = Input(UInt(8.W))
  val osm4        = Input(UInt(8.W))
  val osm5        = Input(UInt(8.W))
  val osm6        = Input(UInt(8.W))
  val osm7        = Input(UInt(8.W))
  val asclkd0     = Input(UInt(4.W))
  val asclkd1     = Input(UInt(4.W))
  val asclkd2     = Input(UInt(4.W))
  val asclkd3     = Input(UInt(4.W))
  val asclkd4     = Input(UInt(4.W))
  val asclkd5     = Input(UInt(4.W))
  val asclkd6     = Input(UInt(4.W))
  val asclkd7     = Input(UInt(4.W))
  val extsel_clk0 = Input(Bool())
  val extsel_clk1 = Input(Bool())
  val extsel_clk2 = Input(Bool())
  val extsel_clk3 = Input(Bool())
  val extsel_clk4 = Input(Bool())
  val extsel_clk5 = Input(Bool())
  val extsel_clk6 = Input(Bool())
  val extsel_clk7 = Input(Bool())
  val vref0       = Input(UInt(8.W))
  val vref1       = Input(UInt(8.W))
  val vref2       = Input(UInt(8.W))
  val clkgcal0    = Input(UInt(8.W))
  val clkgcal1    = Input(UInt(8.W))
  val clkgcal2    = Input(UInt(8.W))
  val clkgcal3    = Input(UInt(8.W))
  val clkgcal4    = Input(UInt(8.W))
  val clkgcal5    = Input(UInt(8.W))
  val clkgcal6    = Input(UInt(8.W))
  val clkgcal7    = Input(UInt(8.W))
  val clkgbias    = Input(UInt(8.W))
  val adcout0     = Output(UInt(9.W))
  val adcout1     = Output(UInt(9.W))
  val adcout2     = Output(UInt(9.W))
  val adcout3     = Output(UInt(9.W))
  val adcout4     = Output(UInt(9.W))
  val adcout5     = Output(UInt(9.W))
  val adcout6     = Output(UInt(9.W))
  val adcout7     = Output(UInt(9.W))
  val clkout_des  = Output(Clock())
  val clkrst      = Input(Bool())
  val clkbout_nc  = Output(Bool()) // no connection
}

class TISARADC extends BlackBox {
  val io = IO(new ADCIO)
}

class DeserIO extends Bundle {
  val in           = Input(Vec(8, UInt(9.W)))
  val out          = Output(Vec(64, UInt(9.W)))
  val clk          = Input(Clock())
  val rst          = Input(Bool())
  // clock that follows data to fifo
  val clkout_data = Output(Clock())
  // clock that goes to rest of the dsp chain
  val clkout_dsp = Output(Clock())
  // set the phase during reset
  val phi_init = Input(UInt(3.W))
}

class des72to576 extends BlackBox {
  val io = IO(new DeserIO)
}
