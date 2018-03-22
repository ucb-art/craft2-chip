// See LICENSE for license details.

package craft

import chisel3.util._
import chisel3._
import math._

class ADCCalIO(numInBits: Int, numOutBits: Int, numSlices: Int) extends Bundle {
  val adcdata = Input(Vec(numSlices, UInt(numInBits.W)))
  val calcoeff = Input(Vec(numSlices, UInt(numOutBits.W)))
  val calout = Output(Vec(numSlices, UInt(numOutBits.W)))
  val mode = Input(UInt(2.W))
  val wen = Input(Bool()) //external wen
  val addr = Input(UInt(numInBits.W))
  override def cloneType = (new ADCCalIO(numInBits, numOutBits, numSlices)).asInstanceOf[this.type]
}

class SubADCCalIO(numInBits: Int, numOutBits: Int) extends Bundle {
  val adcdata = Input(UInt(numInBits.W))
  val calcoeff = Input(UInt(numOutBits.W))
  val calout = Output(UInt(numOutBits.W))
  val mode = Input(UInt(2.W))
  val wen = Input(Bool()) 
  val addr = Input(UInt(numInBits.W))
  override def cloneType = (new SubADCCalIO(numInBits, numOutBits)).asInstanceOf[this.type]
}

class SubADCCal(numInBits: Int, numOutBits: Int) extends Module {
  val io = IO(new SubADCCalIO(numInBits, numOutBits))
  val memaddr = Wire(UInt(numInBits.W))
  val memdata = Wire(UInt(numOutBits.W))
  
  //mode setting
  val idle = 0.U
  val normal = 1.U
  val lutset = 2.U
  val adctest = 3.U
  //counter for adctest
  val addrCounter = RegInit(0.U(numInBits.W))
  addrCounter := addrCounter +% 1.U
  //misc
  val dontcare = 0.U

  // mode selection
  when(io.mode === idle) {
    memaddr := io.addr
    memdata := dontcare
  } .elsewhen(io.mode === normal) {
    memaddr := io.adcdata
    memdata := dontcare
  } .elsewhen(io.mode === lutset) {
    memaddr := io.addr
    memdata := io.calcoeff
  } .elsewhen(io.mode === adctest) {
    memaddr := addrCounter
    memdata := io.adcdata
  }

  // memory
  val mem = SeqMem(math.pow(2, numInBits).toInt, UInt(numOutBits.W))
  mem.suggestName(this.name + "_sram")
  io.calout := mem.read(memaddr, !io.wen)

  when(io.wen) {
    mem.write(memaddr, memdata)
  }
}

class ADCCal(numInBits: Int, numOutBits: Int, numSlices: Int) extends Module {
  val io = IO(new ADCCalIO(numInBits, numOutBits, numSlices))
  //suggested values for this project
  //numInBits=9
  //numOutBits=9 (since we are using this for raw data readout as well, we need 9 instead of 8)
  //             (for normal operation, 8bits in LSB side should be fetched)
  //numSlices=8
 
  val subcal = Seq.fill(numSlices)(Module(new SubADCCal(numInBits, numOutBits)))
  subcal.zipWithIndex foreach { case(mod, i) => 
    mod.io.adcdata := io.adcdata(i)
    mod.io.calcoeff := io.calcoeff(i)
    io.calout(i) := mod.io.calout
    mod.io.mode := io.mode
    mod.io.wen := io.wen
    mod.io.addr := io.addr
  }
}

