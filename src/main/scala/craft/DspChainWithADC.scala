// See LICENSE for license details.

package craft

import cde._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import dspblocks._
import dspjunctions._
import testchipip._
import _root_.util._

trait ADCTopLevelIO {
  val ADCBIAS       = Analog(1.W)
  val ADCINP        = Analog(1.W)
  val ADCINM        = Analog(1.W)
  val ADCCLKP       = Input(Bool())
  val ADCCLKM       = Input(Bool())
  val adcclkreset   = Input(Bool())
}

trait LazyADC {
  def scrbuilder: SCRBuilder

  scrbuilder.addControl("OSP", 0.U)
  scrbuilder.addControl("OSM", 0.U)
  scrbuilder.addControl("ASCLKD", 0.U)
  scrbuilder.addControl("EXTSEL_CLK", 0.U)
  scrbuilder.addControl("VREF0", 0.U)
  scrbuilder.addControl("VREF1", 0.U)
  scrbuilder.addControl("VREF2", 0.U)
  //scrbuilder.addControl("IREF")
  scrbuilder.addControl("CLKGCAL", 0.U)
  scrbuilder.addControl("CLKGBIAS", 0.U)
  scrbuilder.addControl("ADC_VALID", 0.U)
  scrbuilder.addControl("ADC_SYNC", 0.U)
}

trait LazyCAL {
  def scrbuilder: SCRBuilder

  scrbuilder.addControl("MODE", 0.U)
  scrbuilder.addControl("ADDR", 0.U)
  scrbuilder.addControl("WEN", 0.U)
  (0 until 32).foreach { i =>
    scrbuilder.addControl(s"CALCOEFF$i", 0.U)
    scrbuilder.addStatus(s"CALOUT$i")
  }
}


trait HasDspOutputClock {
  val adc_clk_out = Output(Clock())
}

trait DspChainADCIO extends ADCTopLevelIO with HasDspOutputClock

trait ADCModule {
  def io: DspChainIO with DspChainADCIO
  def scrfile: SCRFile
  def clock: Clock // module's implicit clock
  def reset: Bool

  val adc = Module(new TISARADC)

  attach(io.ADCBIAS,    adc.io.ADCBIAS)
  attach(io.ADCINP,     adc.io.ADCINP)
  attach(io.ADCINM,     adc.io.ADCINM)
  adc.io.ADCCLKM := io.ADCCLKM
  adc.io.ADCCLKP := io.ADCCLKP

  def wordToByteVec(u: UInt): Vec[UInt] =
    u.asTypeOf(Vec(8, UInt(8.W)))
  def wordToNibbleVec(u: UInt): Vec[UInt] =
    u.asTypeOf(Vec(16, UInt(4.W)))
  def wordToBoolVec(u: UInt): Vec[Bool] =
    u.asTypeOf(Vec(64, Bool()))

  val osp = wordToByteVec(scrfile.control("OSP"))
  val osm = wordToByteVec(scrfile.control("OSM"))
  val asclkd = wordToNibbleVec(scrfile.control("ASCLKD"))
  val extsel_clk = wordToBoolVec(scrfile.control("EXTSEL_CLK"))
  val vref0 = scrfile.control("VREF0")
  val vref1 = scrfile.control("VREF1")
  val vref2 = scrfile.control("VREF2")
  val clkgcal = wordToByteVec(scrfile.control("CLKGCAL"))
  val clkgbias = scrfile.control("CLKGBIAS")

  adc.io.osp0 := osp(0)
  adc.io.osp1 := osp(1)
  adc.io.osp2 := osp(2)
  adc.io.osp3 := osp(3)
  adc.io.osp4 := osp(4)
  adc.io.osp5 := osp(5)
  adc.io.osp6 := osp(6)
  adc.io.osp7 := osp(7)

  adc.io.osm0 := osm(0)
  adc.io.osm1 := osm(1)
  adc.io.osm2 := osm(2)
  adc.io.osm3 := osm(3)
  adc.io.osm4 := osm(4)
  adc.io.osm5 := osm(5)
  adc.io.osm6 := osm(6)
  adc.io.osm7 := osm(7)

  adc.io.asclkd0 := asclkd(0)
  adc.io.asclkd1 := asclkd(1)
  adc.io.asclkd2 := asclkd(2)
  adc.io.asclkd3 := asclkd(3)
  adc.io.asclkd4 := asclkd(4)
  adc.io.asclkd5 := asclkd(5)
  adc.io.asclkd6 := asclkd(6)
  adc.io.asclkd7 := asclkd(7)

  adc.io.extsel_clk0 := extsel_clk(0)
  adc.io.extsel_clk1 := extsel_clk(1)
  adc.io.extsel_clk2 := extsel_clk(2)
  adc.io.extsel_clk3 := extsel_clk(3)
  adc.io.extsel_clk4 := extsel_clk(4)
  adc.io.extsel_clk5 := extsel_clk(5)
  adc.io.extsel_clk6 := extsel_clk(6)
  adc.io.extsel_clk7 := extsel_clk(7)

  adc.io.vref0 := vref0
  adc.io.vref1 := vref1
  adc.io.vref2 := vref2

  adc.io.clkgcal0 := clkgcal(0)
  adc.io.clkgcal1 := clkgcal(1)
  adc.io.clkgcal2 := clkgcal(2)
  adc.io.clkgcal3 := clkgcal(3)
  adc.io.clkgcal4 := clkgcal(4)
  adc.io.clkgcal5 := clkgcal(5)
  adc.io.clkgcal6 := clkgcal(6)
  adc.io.clkgcal7 := clkgcal(7)

  adc.io.clkgbias := clkgbias

  adc.io.clkrst := io.adcclkreset

  val adcout = Vec(
    adc.io.adcout0,
    adc.io.adcout1,
    adc.io.adcout2,
    adc.io.adcout3,
    adc.io.adcout4,
    adc.io.adcout5,
    adc.io.adcout6,
    adc.io.adcout7)

  val deser = Module(new des72to288)
  deser.io.in := adcout
  deser.io.clk := adc.io.clkout_des
  // [stevo]: wouldn't do anything, since it's only used on reset
  deser.io.phi_init := 0.U
  // unsynchronized ADC clock reset
  deser.io.rst := io.adcclkreset
  
  val des_sync = Vec(deser.io.out.map(s => SyncCrossing(from_clock=deser.io.clkout_data, to_clock=deser.io.clkout_dsp, in=s, sync=1)))
  
  io.adc_clk_out := deser.io.clkout_dsp

  lazy val numInBits = 9
  lazy val numOutBits = 9
  lazy val numSlices = 8*4
  lazy val cal = Module(new ADCCal(numInBits, numOutBits, numSlices))
  cal.io.adcdata := des_sync.asTypeOf(Vec(numSlices, UInt(numInBits.W)))

  cal.io.mode := scrfile.control("MODE")
  cal.io.addr := scrfile.control("ADDR")
  cal.io.wen := scrfile.control("WEN")
  cal.io.calcoeff.zipWithIndex.foreach{ case(port, i) => port := scrfile.control(s"CALCOEFF$i") }
  cal.io.calout.zipWithIndex.foreach{ case(port, i) => scrfile.status(s"CALOUT$i") := port }

  // this lazy weirdness is needed because other traits look at streamIn
  // before this code executes

  lazy val streamIn = Wire(ValidWithSync(cal.io.calout.asTypeOf(UInt())))
  streamIn.bits  := cal.io.calout.asTypeOf(UInt())
  streamIn.valid := scrfile.control("ADC_VALID")
  streamIn.sync  := scrfile.control("ADC_SYNC")

}

class DspChainWithADC(
  b: => Option[DspChainIO with DspChainADCIO] = None,
  override_clock: Option[Clock]=None,
  override_reset: Option[Bool]=None)(implicit p: Parameters) extends 
    DspChain() with LazyADC with LazyCAL {
  lazy val module: DspChainWithADCModule =
    new DspChainWithADCModule(this, b, override_clock, override_reset)
}

class DspChainWithADCModule(
  outer: DspChain,
  b: => Option[DspChainIO with DspChainADCIO] = None,
  override_clock: Option[Clock]=None,
  override_reset: Option[Bool]=None)(implicit p: Parameters)
  extends DspChainModule(outer, b, override_clock, override_reset)
    with ADCModule with RealAnalogAnnotator {
  override lazy val io: DspChainIO with DspChainADCIO = b.getOrElse(new DspChainIO with DspChainADCIO)
  annotateReal()
}

// [stevo]: copied from rocket-chip, but switched Bool input to Data
object SyncCrossing {
  class SynchronizerBackend[T<:Data](sync: Int, _clock: Clock, gen: T) extends Module(Some(_clock)) {
    val io = IO(new Bundle {
      val in = Input(gen)
      val out = Output(gen)
    })

    io.out := ShiftRegister(io.in, sync)
  }

  class SynchronizerFrontend[T<:Data](_clock: Clock, gen: T) extends Module(Some(_clock)) {
    val io = IO(new Bundle {
      val in = Input(gen)
      val out = Output(gen)
    })

    io.out := RegNext(io.in)
  }

  def apply[T<:Data](from_clock: Clock, to_clock: Clock, in: T, sync: Int = 2): T = {
    val front = Module(new SynchronizerFrontend(from_clock, in.cloneType))
    val back = Module(new SynchronizerBackend(sync, to_clock, in.cloneType))

    front.io.in := in
    back.io.in := front.io.out
    back.io.out
  }
}
