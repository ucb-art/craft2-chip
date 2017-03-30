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
  val VIN = Input(Bool())
  val VIP = Input(Bool())
  val VOBUF = Output(Clock())
}

class CLK_RX_amp_buf extends BlackBox {
  val io = IO(new CLKRXIO)
}

trait CLKRXModule {
  implicit val p: Parameters
  def io: Bundle with CLKRXTopLevelInIO with CLKRXTopLevelOutIO
  val clkrx = Module(new CLK_RX_amp_buf)
  clkrx.io.VIN := io.CLKRXVIN
  clkrx.io.VIP := io.CLKRXVIP
  io.clkrxvobuf := clkrx.io.VOBUF
}

