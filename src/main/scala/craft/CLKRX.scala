// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._
import cde._
import chisel3.core.ExplicitCompileOptions.NotStrict

trait CLKRXTopLevelInIO {
  val CLKRXVIN = Input(Bool())
  val CLKRXVIP = Input(Bool())
}

trait CLKRXTopLevelOutIO {
  val clkrxvobuf = Output(Clock())
}

class CLKRXIO extends Bundle {
  val inp = Input(Bool())
  val inn = Input(Bool())
  val out = Output(Clock())
}

class lvds_csda_dcc extends BlackBox {
  val io = IO(new CLKRXIO)
}

trait CLKRXModule {
  implicit val p: Parameters
  def io: Bundle with CLKRXTopLevelInIO with CLKRXTopLevelOutIO
  val clkrx = Module(new lvds_csda_dcc)
  clkrx.io.inn := io.CLKRXVIN
  clkrx.io.inp := io.CLKRXVIP
  io.clkrxvobuf := clkrx.io.out
}

