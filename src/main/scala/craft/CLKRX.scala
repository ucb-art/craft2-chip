// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._
import cde._
import chisel3.core.ExplicitCompileOptions.NotStrict

case object BuildCLKRX extends Field[(Bundle with CLKRXTopLevelIO) => CLKRX]

class CLKRXIO extends Bundle {
  val VIN = Analog(1.W)
  val VIP = Analog(1.W)
  val CLKRXOUT = Output(Clock())
}

class CLKRX extends BlackBox {
  val io = IO(new CLKRXIO)
}

trait CLKRXTopLevelIO {
  val VIN = Analog(1.W)
  val VIP = Analog(1.W)
  val CLKRXOUT = Output(Clock())
}

trait CLKRXModule {
  implicit val p: Parameters
  def io: Bundle with CLKRXTopLevelIO
  val m = Module(new CLKRX)
  attach(io.VIN, m.io.VIN)
  attach(io.VIP, m.io.VIP)
  io.CLKRXOUT := m.io.CLKRXOUT
  //io <> m.io
}

