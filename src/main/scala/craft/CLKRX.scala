// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._

trait CLKRXTopLevelInIO {
  val CLKRXVIN = IO(Input(Bool()))
  val CLKRXVIP = IO(Input(Bool()))
}

trait CLKRXTopLevelOutIO {
  val clkrxvobuf = IO(Output(Clock()))
}

class CLKRXIO extends Bundle {
  val inp = Input(Bool())
  val inn = Input(Bool())
  val out = Output(Clock())
}

class lvds_csda_dcc extends BlackBox {
  val io = IO(new CLKRXIO)
}

trait CLKRX extends CLKRXTopLevelInIO with CLKRXTopLevelOutIO {
  val clkrx = Module(new lvds_csda_dcc)
  clkrx.io.inn := CLKRXVIN
  clkrx.io.inp := CLKRXVIP
  clkrxvobuf := clkrx.io.out
}

