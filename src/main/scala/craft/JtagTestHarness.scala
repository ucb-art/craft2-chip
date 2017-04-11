package craft


import cde._
import chisel3.iotesters._
import chisel3.iotesters.experimental._
import diplomacy._
import dspblocks._
import jtag._
import jtag.test._
import uncore.tilelink._
import _root_.junctions._

import org.scalatest._
object ReportVerilog {
  def getResource(name: String):String = {
	import scala.io.Source
	val stream = getClass.getResourceAsStream(name)
	Source.fromInputStream( stream ).getLines.mkString("\n")
  }

  def writeFile(name: String, contents:String): Unit = {
	import java.io.{File, PrintWriter}
	val writer = new PrintWriter(new File(name))
	writer.write(contents)
	writer.close()
  }

  def apply(stmts: Seq[Statement]): Unit = {
    // inputs of the testbench, outputs of the DUT
    val inputStrs = Set(stmts.map(_ match {
      case PokeStatement(s, _) => Some(s)
      case _                   => None
    }).flatten: _*)
    // outputs of the testbench, inputs of the DUT
    val outputStrs  = Set(stmts.map(_ match {
      case ExpectStatement(s, _, _) => Some(s)
      case _                        => None
    }).flatten: _*) -- inputStrs
    object JtagTest {
      val moduleName = "JtagTestHarness"
      val timeUnitPs = 1
      val timePrecisionFs = 100
      val clkMul = 16666
      val clkHalfMul = clkMul/2
      val resetTime = 0
      val initTime  = 0
      val end_on_finish = true
      val inputs = inputStrs.map(i => Map(
        "width"  -> 1,
        "signed" -> "",
        "name"   -> i))
      val outputs = outputStrs.map(o => Map(
        "width"  -> 1,
        "signed" -> "",
        "name"   -> o))
      val statements = stmts.map(s => { 
        val stepMap = s match {
          case StepStatement(n) => Map(
            "step"   -> true,
            "step_n" -> n)
          case _ => Map(
            "step"   -> false,
            "step_n" -> 0)
        }
        val resetMap = s match {
          case ResetStatement(n) => Map(
            "reset"   -> true,
            "reset_n" -> n)
          case _ => Map(
            "reset"   -> false,
            "reset_n" -> 0)
        }
        val pokeMap = s match {
          case PokeStatement(n, v) => Map(
            "poke"       -> true,
            "poke_name"  -> n,
            "poke_value" -> v)
          case _ => Map(
            "poke"       -> false,
            "poke_name"  -> "",
            "poke_value" -> 0)
        }
        val expectMap = s match {
          case ExpectStatement(n, v, m) => Map(
            "expect"         -> true,
            "expect_name"    -> n,
            "expect_value"   -> v,
            "expect_message" -> m)
          case _ => Map(
            "expect"         -> false,
            "expect_name"    -> "",
            "expect_value"   -> 0,
            "expect_message" -> "")
        }
        stepMap ++ resetMap ++ pokeMap ++ expectMap
      })
    }
	val testbench: String = {
	  import com.gilt.handlebars.scala.binding.dynamic._
	  import com.gilt.handlebars.scala.Handlebars

	  val template = getResource("/JtagTestHarness.v")
	  val t= Handlebars(template)
	  t(JtagTest)
	}

    writeFile("vsrc/JtagTestHarness.v", testbench)
    println(testbench)
  }
}

object JtagTestHarnessApp extends App with JtagAxiUtilities {
  val p = Parameters.root((new Craft2SimpleConfig).toInstance).alterPartial {
    case TLId => "craft-radar"
  }
  def dut(p: Parameters) = LazyModule(new DspChainWithADC()(p)).module
  test(dut(p), testerBackend=chisel3.iotesters.experimental.IntermediateBackend, options = new TesterOptionsManager {
    testerOptions = testerOptions.copy(intermediateReportFunc = ReportVerilog.apply _)
    interpreterOptions = interpreterOptions.copy(setVerbose = false, writeVCD = true)
    }) { implicit t => c =>
    implicit val n: HasNastiParameters = c.io.data_axi.ar.bits

    c.io.jtag map { j =>
      // let the chain come up
      step(20)

      axiRead(j, BigInt(0x7000L), BigInt(0x0))
      step(10)

      axiWrite(j, BigInt(0x7000L), BigInt(0x7), id = 1)
      step(10)

      axiRead(j, BigInt(0x7000L), BigInt(0x7))
      step(10)
    }
  }

}
