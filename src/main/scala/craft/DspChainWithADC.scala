// See LICENSE for license details.

package craft

import cde._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import dspblocks._
import dspjunctions._
import testchipip._

trait ADCTopLevelIO {
  val ADCVDDHADC    = Analog(1.W)
  val ADCVDDADC     = Analog(1.W)
  val ADCVSS        = Analog(1.W)
  val ADCBIAS    = Analog(1.W)
  val ADCEXTCLK     = Input(Bool())
  val ADCINP      = Analog(1.W)
  val ADCINM      = Analog(1.W)
  val ADCCLKP     = Analog(1.W)
  val ADCCLKM     = Analog(1.W)
  val ADCCLKRST      = Input(Bool())
}

trait LazyADC {
  def scrbuilder: SCRBuilder

  scrbuilder.addControl("OSP")
  scrbuilder.addControl("OSM")
  scrbuilder.addControl("ASCLKD")
  scrbuilder.addControl("EXTSEL_CLK")
  scrbuilder.addControl("VREF0")
  scrbuilder.addControl("VREF1")
  scrbuilder.addControl("VREF2")
  scrbuilder.addControl("IREF")
  scrbuilder.addControl("CLKGCAL")
  scrbuilder.addControl("CLKGBIAS")
  scrbuilder.addControl("ADC_VALID")
  scrbuilder.addControl("ADC_SYNC")
}

trait LazyCAL {
  def scrbuilder: SCRBuilder

  scrbuilder.addControl("MODE")
  scrbuilder.addControl("ADDR")
  (0 until 32).foreach { i =>
    scrbuilder.addControl(s"CALCOEFF$i")
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

  attach(io.ADCVDDHADC, adc.io.VDDHADC)
  attach(io.ADCVDDADC,  adc.io.VDDADC)
  attach(io.ADCVSS,     adc.io.VSS)
  attach(io.ADCBIAS, adc.io.ADCBIAS)
  attach(io.ADCINP,   adc.io.ADCINP)
  attach(io.ADCINM,   adc.io.ADCINM)
  attach(io.ADCCLKP,  adc.io.ADCCLKP)
  attach(io.ADCCLKM,  adc.io.ADCCLKM)

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
  val vref0 = wordToByteVec(scrfile.control("VREF0"))
  val vref1 = wordToByteVec(scrfile.control("VREF1"))
  val vref2 = wordToByteVec(scrfile.control("VREF2"))
  val iref = wordToByteVec(scrfile.control("IREF"))
  val clkgcal = wordToByteVec(scrfile.control("CLKGCAL"))
  val clkgbias = scrfile.control("CLKGBIAS")

  adc.io.OSP0 := osp(0)
  adc.io.OSP1 := osp(1)
  adc.io.OSP2 := osp(2)
  adc.io.OSP3 := osp(3)
  adc.io.OSP4 := osp(4)
  adc.io.OSP5 := osp(5)
  adc.io.OSP6 := osp(6)
  adc.io.OSP7 := osp(7)

  adc.io.OSM0 := osm(0)
  adc.io.OSM1 := osm(1)
  adc.io.OSM2 := osm(2)
  adc.io.OSM3 := osm(3)
  adc.io.OSM4 := osm(4)
  adc.io.OSM5 := osm(5)
  adc.io.OSM6 := osm(6)
  adc.io.OSM7 := osm(7)

  adc.io.EXTCLK0 := io.ADCEXTCLK
  adc.io.EXTCLK1 := io.ADCEXTCLK
  adc.io.EXTCLK2 := io.ADCEXTCLK
  adc.io.EXTCLK3 := io.ADCEXTCLK
  adc.io.EXTCLK4 := io.ADCEXTCLK
  adc.io.EXTCLK5 := io.ADCEXTCLK
  adc.io.EXTCLK6 := io.ADCEXTCLK
  adc.io.EXTCLK7 := io.ADCEXTCLK

  adc.io.ASCLKD0 := asclkd(0)
  adc.io.ASCLKD1 := asclkd(1)
  adc.io.ASCLKD2 := asclkd(2)
  adc.io.ASCLKD3 := asclkd(3)
  adc.io.ASCLKD4 := asclkd(4)
  adc.io.ASCLKD5 := asclkd(5)
  adc.io.ASCLKD6 := asclkd(6)
  adc.io.ASCLKD7 := asclkd(7)

  adc.io.EXTSEL_CLK0 := extsel_clk(0)
  adc.io.EXTSEL_CLK1 := extsel_clk(1)
  adc.io.EXTSEL_CLK2 := extsel_clk(2)
  adc.io.EXTSEL_CLK3 := extsel_clk(3)
  adc.io.EXTSEL_CLK4 := extsel_clk(4)
  adc.io.EXTSEL_CLK5 := extsel_clk(5)
  adc.io.EXTSEL_CLK6 := extsel_clk(6)
  adc.io.EXTSEL_CLK7 := extsel_clk(7)

  adc.io.VREF00 := vref0(0)
  adc.io.VREF01 := vref0(1)
  adc.io.VREF02 := vref0(2)
  adc.io.VREF03 := vref0(3)
  adc.io.VREF04 := vref0(4)
  adc.io.VREF05 := vref0(5)
  adc.io.VREF06 := vref0(6)
  adc.io.VREF07 := vref0(7)

  adc.io.VREF10 := vref1(0)
  adc.io.VREF11 := vref1(1)
  adc.io.VREF12 := vref1(2)
  adc.io.VREF13 := vref1(3)
  adc.io.VREF14 := vref1(4)
  adc.io.VREF15 := vref1(5)
  adc.io.VREF16 := vref1(6)
  adc.io.VREF17 := vref1(7)

  adc.io.VREF20 := vref2(0)
  adc.io.VREF21 := vref2(1)
  adc.io.VREF22 := vref2(2)
  adc.io.VREF23 := vref2(3)
  adc.io.VREF24 := vref2(4)
  adc.io.VREF25 := vref2(5)
  adc.io.VREF26 := vref2(6)
  adc.io.VREF27 := vref2(7)

  adc.io.IREF0 := iref(0)
  adc.io.IREF1 := iref(1)
  adc.io.IREF2 := iref(2)

  adc.io.CLKGCAL0 := clkgcal(0)
  adc.io.CLKGCAL1 := clkgcal(1)
  adc.io.CLKGCAL2 := clkgcal(2)
  adc.io.CLKGCAL3 := clkgcal(3)
  adc.io.CLKGCAL4 := clkgcal(4)
  adc.io.CLKGCAL5 := clkgcal(5)
  adc.io.CLKGCAL6 := clkgcal(6)
  adc.io.CLKGCAL7 := clkgcal(7)

  adc.io.CLKGBIAS := clkgbias

  adc.io.CLKRST := io.ADCCLKRST

  val adcout = Vec(
    adc.io.ADCOUT0,
    adc.io.ADCOUT1,
    adc.io.ADCOUT2,
    adc.io.ADCOUT3,
    adc.io.ADCOUT4,
    adc.io.ADCOUT5,
    adc.io.ADCOUT6,
    adc.io.ADCOUT7)

  val deser = Module(new des72to288)
  deser.io.in := adcout
  deser.io.clk := adc.io.CLKOUT_DES
  // [stevo]: wouldn't do anything, since it's only used on reset
  deser.io.phi_init := 0.U
  // unsynchronized ADC clock reset
  deser.io.rst := io.ADCCLKRST
  
  val des_sync_data = RegSync(deser.io.out, deser.io.clkout_data, 1)
  val des_sync_dsp  = RegSync(des_sync_data, deser.io.clkout_dsp, 1)
  
  io.adc_clk_out := deser.io.clkout_dsp

  lazy val numInBits = 9
  lazy val numOutBits = 9
  lazy val numSlices = 8*4
  lazy val cal = Module(new ADCCal(numInBits, numOutBits, numSlices))
  cal.io.adcdata := des_sync_dsp.asTypeOf(Vec(numSlices, UInt(numInBits.W)))

  cal.io.mode := scrfile.control("MODE")
  cal.io.addr := scrfile.control("ADDR")
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
    with ADCModule {
  override lazy val io: DspChainIO with DspChainADCIO = b.getOrElse(new DspChainIO with DspChainADCIO)
}

class RegSync[T <: Data](gen: => T, c: Clock, lat: Int = 2) extends Module(_clock = c) {
  val io = IO(new Bundle {
    val in = Input(gen)
    val out = Output(gen)
  })
  io.out := ShiftRegister(io.in,lat)
}

object RegSync {
  def apply[T <: Data](in: T, c: Clock, lat: Int = 2): T = {
    val sync = Module(new RegSync(in.cloneType,c,2))
    sync.suggestName("regSyncInst")
    sync.io.in := in
    sync.io.out
  }

