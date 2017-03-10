// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._
import cde._
import chisel3.core.ExplicitCompileOptions.NotStrict

case object BuildCLKRX extends Field[(Bundle with CLKRXTopLevelInIO with CLKRXTopLevelOutIO) => CLK_RX_amp_buf]

class CLKRXIO extends Bundle {
  val VIN = Analog(1.W)
  val VIP = Analog(1.W)
  val VOBUF = Output(Clock())
}

class CLK_RX_amp_buf extends BlackBox {
  val io = IO(new CLKRXIO)
}

trait CLKRXTopLevelInIO {
  val VIN = Analog(1.W)
  val VIP = Analog(1.W)
}

trait CLKRXTopLevelOutIO {
  val VOBUF = Output(Clock())
}

trait CLKRXModule {
  implicit val p: Parameters
  def io: Bundle with CLKRXTopLevelInIO with CLKRXTopLevelOutIO
  val m = Module(new CLK_RX_amp_buf)
  attach(io.VIN, m.io.VIN)
  attach(io.VIP, m.io.VIP)
  io.VOBUF := m.io.VOBUF
  //io <> m.io
}

