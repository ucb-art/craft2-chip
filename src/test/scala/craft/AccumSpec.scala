// See LICENSE for license details.
package craft

import diplomacy.{LazyModule, LazyModuleImp}
import dsptools.{DspContext, Grow}
import spire.algebra.{Field, Ring}
import breeze.math.{Complex}
import breeze.linalg._
import breeze.signal._
import breeze.signal.support._
import breeze.signal.support.CanFilter._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import chisel3.iotesters._
import dspjunctions._
import dspblocks._
import firrtl_interpreter.InterpreterOptions
import dsptools.numbers.{DspReal, SIntOrder, SIntRing}
import dsptools.{DspContext, DspTester, Grow}
import org.scalatest.{FlatSpec, Matchers}
import dsptools.numbers.implicits._
import dsptools.numbers.{DspComplex, Real}
import chisel3.testers.BasicTester
import org.scalatest._
import scala.util.Random
import scala.math._
import testchipip._

import cde._
import junctions._
import uncore.tilelink._
import uncore.coherence._

import dsptools._

class AccumulatorTester[T <: Data:Ring](c: AccumulatorModule[T])(implicit p: Parameters) extends DspBlockTester(c) {
  val config = p(AccumulatorKey(p(DspBlockId)))
  val gk = p(GenKey(p(DspBlockId)))
  val sync_period = config.outputWindowSize/config.lanes
  val test_length = 18
  
  // define input datasets here
  val ins = Seq.fill(test_length)(Seq.fill(sync_period)(Seq.fill(gk.lanesIn)(3.7)))
  def streamIn = ins.map(packInputStream(_, gk.genIn))

  // reset 5 cycles
  reset(5)

  // should output nothing
  pauseStream
  axiWrite(addrmap("NumSpectraToAccumulate"), 0)
  playStream
  step(sync_period)
  println(gk.genOut.asInstanceOf[T].toString())
  var output = unpackOutputStream(gk.genOut.asInstanceOf[T], gk.lanesOut)
  pauseStream
  println("Chisel Output when accumulating 0 spectra (should be empty list)")
  println(output.toArray.deep.mkString("\n"))

  axiWrite(addrmap("NumSpectraToAccumulate"), 1)
  playStream
  step(2*sync_period) // have to wait 1 cycle for data to get through shift register
  pauseStream
  //output = unpackOutputStream(gk.genOut.asInstanceOf[T], gk.lanesOut)
  output = unpackOutputStream(FixedPoint(64.W, 9.BP), 8)
  println("Chisel Output when accumulating 1 spectrum (should be input)")
  println(output.toArray.deep.mkString("\n"))
  peek(c.io.out.bits)

  axiWrite(addrmap("NumSpectraToAccumulate"), 2)
  playStream
  step(3*sync_period)
  pauseStream
  output = unpackOutputStream(gk.genOut.asInstanceOf[T], gk.lanesOut)
  println("Chisel Output when accumulating 2 spectra (should be double input)")
  println(output.toArray.deep.mkString("\n"))

  axiWrite(addrmap("NumSpectraToAccumulate"), 7)
  playStream
  step(8*sync_period)
  pauseStream
  output = unpackOutputStream(gk.genOut.asInstanceOf[T], gk.lanesOut)
  println("Chisel Output when accumulating 7 spectra (should be 7x input)")
  println(output.toArray.deep.mkString("\n"))

  //println("Input:")
  //println(ins.toArray.flatten.deep.mkString("\n"))
  //println("Chisel Output")
  //println(output.toArray.deep.mkString("\n"))
}

class AccumulatorSpec extends FlatSpec with Matchers {
  behavior of "Accumulator"
  val manager = new TesterOptionsManager {
    testerOptions = TesterOptions(backendName = "firrtl", testerSeed = 7L)
    interpreterOptions = InterpreterOptions(setVerbose = false, writeVCD = true)
  }

  //import ComplexModuleImpl._

  it should "work with DspBlockTester" in {
    implicit val p: Parameters = Parameters.root(AccumulatorConfigBuilder.standalone(
      "accum", 
      AccumulatorConfig(
        outputWindowSize = 32,
        lanes = 8,
        maxSpectra = 16),
      genIn = () => FixedPoint(16.W, 9.BP),
      genOut = Some(() => FixedPoint(64.W, 9.BP)))
      .toInstance)
    val dut = () => LazyModule(new AccumulatorBlock[FixedPoint]).module
    chisel3.iotesters.Driver.execute(dut, manager) { c => new AccumulatorTester(c) } should be (true)
  }
}
