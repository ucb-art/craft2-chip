// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._
import cde._
import chisel3.core.ExplicitCompileOptions.NotStrict

trait CLKRXTopLevelInIO {
  val VIN = Analog(1.W)
  val VIP = Analog(1.W)
}

trait CLKRXTopLevelOutIO {
  val VOBUF = Output(Clock())
}

class CLKRXIO extends Bundle with CLKRXTopLevelInIO with CLKRXTopLevelOutIO

class CLK_RX_amp_buf extends BlackBox {
  val io = IO(new CLKRXIO)
}

trait CLKRXModule {
  implicit val p: Parameters
  def io: Bundle with CLKRXTopLevelInIO with CLKRXTopLevelOutIO
  val clkrx = Module(new CLK_RX_amp_buf)
  attach(io.VIN, clkrx.io.VIN)
  attach(io.VIP, clkrx.io.VIP)
  io.VOBUF := clkrx.io.VOBUF
}

