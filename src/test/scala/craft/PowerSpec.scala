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

class PowerTester[T <: Data:Ring](c: PowerModule[T])(implicit p: Parameters) extends DspBlockTester(c) {
  val config = p(PowerKey(p(DspBlockId)))
  val gk = p(GenKey(p(DspBlockId)))
  val sync_period = 1
  val test_length = 1
  
  // define input datasets here
  val ins = Seq.fill(test_length)(Seq.fill(sync_period)(Seq.fill(gk.lanesIn)(Complex(Random.nextDouble*2-1, Random.nextDouble*2-1))))
  //val ins = Seq.fill(test_length)(Seq.fill(sync_period)(Seq.fill(gk.lanesIn)(Complex(-1.2, -0.7))))
  def streamIn = ins.map(packInputStream(_, gk.genIn))

  // reset 5 cycles
  reset(5)

  pauseStream
  // setup SCR stuff
  playStream
  step(test_length*sync_period + config.pipelineDepth)
  val output = unpackOutputStream(gk.genOut.asInstanceOf[T], gk.lanesOut)

  println("Input:")
  println(ins.toArray.flatten.deep.mkString("\n"))
  println("Chisel Output")
  println(output.toArray.deep.mkString("\n"))
}

class PowerSpec extends FlatSpec with Matchers {
  behavior of "Power"
  val manager = new TesterOptionsManager {
    testerOptions = TesterOptions(backendName = "firrtl", testerSeed = 7L)
    interpreterOptions = InterpreterOptions(setVerbose = false, writeVCD = true)
  }

  //import ComplexModuleImpl._

  it should "work with DspBlockTester" in {
    implicit val p: Parameters = Parameters.root(PowerConfigBuilder.standalone(
      "power", 
      PowerConfig(
        pipelineDepth = 2,
        lanes = 2),
      genIn = () => FixedPoint(13.W, 9.BP),
      genOut = Some(() => FixedPoint(13.W, 9.BP)))
      .toInstance)
    val dut = () => LazyModule(new PowerBlock[FixedPoint]).module
    chisel3.iotesters.Driver.execute(dut, manager) { c => new PowerTester(c) } should be (true)
  }
}
