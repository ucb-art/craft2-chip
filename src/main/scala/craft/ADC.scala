// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._

trait HasADCIO {
  val ADCBIAS     = IO(Analog(1.W))
  val ADCINP      = IO(Analog(1.W))
  val ADCINM      = IO(Analog(1.W))
  val ADCCLKP     = IO(Input(Bool()))
  val ADCCLKM     = IO(Input(Bool()))
  val osp0        = IO(Input(UInt(8.W)))
  val osp1        = IO(Input(UInt(8.W)))
  val osp2        = IO(Input(UInt(8.W)))
  val osp3        = IO(Input(UInt(8.W)))
  val osp4        = IO(Input(UInt(8.W)))
  val osp5        = IO(Input(UInt(8.W)))
  val osp6        = IO(Input(UInt(8.W)))
  val osp7        = IO(Input(UInt(8.W)))
  val osm0        = IO(Input(UInt(8.W)))
  val osm1        = IO(Input(UInt(8.W)))
  val osm2        = IO(Input(UInt(8.W)))
  val osm3        = IO(Input(UInt(8.W)))
  val osm4        = IO(Input(UInt(8.W)))
  val osm5        = IO(Input(UInt(8.W)))
  val osm6        = IO(Input(UInt(8.W)))
  val osm7        = IO(Input(UInt(8.W)))
  val asclkd0     = IO(Input(UInt(4.W)))
  val asclkd1     = IO(Input(UInt(4.W)))
  val asclkd2     = IO(Input(UInt(4.W)))
  val asclkd3     = IO(Input(UInt(4.W)))
  val asclkd4     = IO(Input(UInt(4.W)))
  val asclkd5     = IO(Input(UInt(4.W)))
  val asclkd6     = IO(Input(UInt(4.W)))
  val asclkd7     = IO(Input(UInt(4.W)))
  val extsel_clk0 = IO(Input(Bool()))
  val extsel_clk1 = IO(Input(Bool()))
  val extsel_clk2 = IO(Input(Bool()))
  val extsel_clk3 = IO(Input(Bool()))
  val extsel_clk4 = IO(Input(Bool()))
  val extsel_clk5 = IO(Input(Bool()))
  val extsel_clk6 = IO(Input(Bool()))
  val extsel_clk7 = IO(Input(Bool()))
  val vref0       = IO(Input(UInt(8.W)))
  val vref1       = IO(Input(UInt(8.W)))
  val vref2       = IO(Input(UInt(8.W)))
  val clkgcal0    = IO(Input(UInt(8.W)))
  val clkgcal1    = IO(Input(UInt(8.W)))
  val clkgcal2    = IO(Input(UInt(8.W)))
  val clkgcal3    = IO(Input(UInt(8.W)))
  val clkgcal4    = IO(Input(UInt(8.W)))
  val clkgcal5    = IO(Input(UInt(8.W)))
  val clkgcal6    = IO(Input(UInt(8.W)))
  val clkgcal7    = IO(Input(UInt(8.W)))
  val clkgbias    = IO(Input(UInt(8.W)))
  val adcout0     = IO(Output(UInt(9.W)))
  val adcout1     = IO(Output(UInt(9.W)))
  val adcout2     = IO(Output(UInt(9.W)))
  val adcout3     = IO(Output(UInt(9.W)))
  val adcout4     = IO(Output(UInt(9.W)))
  val adcout5     = IO(Output(UInt(9.W)))
  val adcout6     = IO(Output(UInt(9.W)))
  val adcout7     = IO(Output(UInt(9.W)))
  val clkout_des  = IO(Output(Clock()))
  val clkrst      = IO(Input(Bool()))
  val clkbout_nc  = IO(Output(Bool())) // no connection
}

class TISARADC extends BlackBox {
  val io = IO(new Bundle with HasADCIO)
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
