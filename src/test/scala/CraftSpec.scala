// See LICENSE for license details.

package dspblocks

import cde._
import chisel3._
import chisel3.iotesters._
import craft._
import diplomacy._
import dsptools._
import firrtl_interpreter.InterpreterOptions
import org.scalatest._
import sam._

class CraftUUIDTester(dut: DspChainModule) extends DspChainTester(dut) {
  val streamIn = Seq((0 until 48).map(x=>BigInt(x)).toSeq)
  pauseStream()

  for ( (mod, map) <- testchipip.SCRAddressMap.contents) {
    println(s"Module: $mod")
    for ( (reg, addr) <- map ) {
      println(s"\t$reg\t@\t$addr")
    }
  }

  for (m <- dut.modules) {
    val id = m.id
    val uuid_addr = addrmap(s"$id:uuid")
    println(s"UUID for module $id is $uuid_addr")

    val readUUID = axiRead(uuid_addr)
    require (readUUID == m.hashCode,
      s"UUID for $id should be ${m.hashCode}, was $readUUID")
  }
}

class CraftAll1sTester(dut: DspChainModule) extends DspChainTester(dut) {
  val streamIn = Seq( (0 until 2048).map(x=>BigInt(65536)).toSeq )
  initiateSamCapture(5, prefix=Some("craft-afb:fft:sam"), waitForSync = true)
  playStream()

  step(2048)

  for (i <- 65536 until 65536 + 5 * 8 by 8) {
    axi = dataAXI
    println(s"Read addr $i = ${axiRead(i)}")
  }
}


class CraftTesterSpec extends FlatSpec with Matchers {
  val manager = new TesterOptionsManager {
    testerOptions = TesterOptions(backendName = "verilator", testerSeed = 7L, isVerbose = true)
    interpreterOptions = InterpreterOptions(setVerbose = false, writeVCD = true)
  }



  implicit val p: Parameters = Parameters.root(ChainBuilder.afbChain().toInstance)

  val dut = () => {
    val lazyChain = LazyModule(new DspChain(0x0000, 0x1000))
    lazyChain.module
  }

  behavior of "Craft"

  it should "be able to read UUIDs" in {
    chisel3.iotesters.Driver.execute( dut, manager ) {
      c => new CraftUUIDTester(c) }
  }

  it should "read something after streaming in all 1's" in {
    chisel3.iotesters.Driver.execute( dut, manager ) {
      c => new CraftAll1sTester(c) }
  }
}

