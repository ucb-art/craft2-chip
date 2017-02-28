// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental._
import scala.collection.mutable.Map
import cde.{Parameters, Config, Dump, Knob, CDEMatchError, Field}
import diplomacy.LazyModule
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspblocks._
import dspjunctions._
import _root_.junctions._
import rocketchip._
import testchipip._
import uncore.tilelink._
import uncore.converters._
import uncore.agents._
import uncore.devices._
import coreplex._
import hwacha._
import dma._
import scala.math.max
import util._

import rocket._
import groundtest._
import rocketchip.DefaultTestSuites._

import tuner._
import ComplexModuleImpl._
import fir._
import fft._
import pfb._
import sam._

import chisel3.core.ExplicitCompileOptions.NotStrict

class WithCraft2DSP extends Config(
  (pname, site, here) => pname match {
    case BuildCraft2DSP => (control_port: ClientUncachedTileLinkIO, data_port: ClientUncachedTileLinkIO, io: Bundle with ADCTopLevelIO, p: Parameters) => {
      implicit val q = p
      val dataBaseAddr = 0x3000
      val ctrlBaseAddr = 0x2000
      val dsp_clock = Wire(Clock())
      val lazyChain = LazyModule(new DspChainWithADC(ctrlBaseAddr, dataBaseAddr, override_clock= Some(dsp_clock)))
      val chain = Module(lazyChain.module)
      dsp_clock := chain.io.adc_clk_out
      // add width adapter because Hwacha needs 128-bit TL
      chain.io.control_axi <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkTo(to_clock=dsp_clock, to_reset=chain.reset, TileLinkWidthAdapter(control_port, chain.ctrlXbarParams)))
      chain.io.data_axi <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkTo(to_clock=dsp_clock, to_reset=chain.reset, TileLinkWidthAdapter(data_port, chain.dataXbarParams)))
      io <> chain.io
      ()
    }
    case _ => throw new CDEMatchError
  })

object ChainBuilder {
  type T = FixedPoint
  def doubleToGen(x: Double): DspComplex[T] = DspComplex(FixedPoint.fromDouble(x, 32.W, 16.BP), FixedPoint.fromDouble(0.0, 32.W, 16.BP))
  def getGenType(): T = FixedPoint(32.W, 16.BP)
  def afbChain(
    id: String = "craft-afb",
    fftConfig: FFTConfig = FFTConfig(),
    pfbConfig: PFBConfig = PFBConfig(),
    lanes: Int = 8): Config = {
    new Config(
      (pname, site, here) => pname match {
        case DefaultSAMKey => SAMConfig(16, 16)
        case DspChainId => id
        case DspChainKey(_id) if _id == id => DspChainParameters(
          blocks = Seq(
            (implicit p => new PFBBlock[DspComplex[T]], id + ":pfb"),
            (implicit p => new FFTBlock[T],             id + ":fft")
          ),
          logicAnalyzerSamples = 256,
          logicAnalyzerUseCombinationalTrigger = true,
          patternGeneratorSamples = 256,
          patternGeneratorUseCombinationalTrigger = true
        )
        case _ => throw new CDEMatchError
      }
    ) ++
    ConfigBuilder.nastiTLParams(id) ++
    PFBConfigBuilder(id + ":pfb", pfbConfig, () => DspComplex(getGenType(), getGenType()), doubleToGen) ++
    FFTConfigBuilder(id + ":fft", fftConfig, () => getGenType())
  }
  def fullChain(
    id: String = "craft-chain",
    tuner1Config: TunerConfig = TunerConfig(),
    fir1Config: FIRConfig = FIRConfig(),
    tuner2Config: TunerConfig = TunerConfig(),
    fir2Config: FIRConfig = FIRConfig(),
    fftConfig: FFTConfig = FFTConfig(),
    pfbConfig: PFBConfig = PFBConfig(),
    lanes: Int = 8): Config = {
    new Config(
      (pname, site, here) => pname match {
        case DefaultSAMKey => SAMConfig(16, 16)
        case DspChainId => id
        case DspChainKey(_id) if _id == id => DspChainParameters(
          blocks = Seq(
            (implicit p => new TunerBlock[T, T], id + ":tuner1"),
            (implicit p => new FIRBlock[DspComplex[T]], id + ":fir1"),
            (implicit p => new TunerBlock[DspComplex[T], T], id + ":tuner2"),
            (implicit p => new FIRBlock[DspComplex[T]], id + ":fir2"),
            (implicit p => new PFBBlock[DspComplex[T]], id + ":pfb"),
            (implicit p => new FFTBlock[T],             id + ":fft")
          ),
          logicAnalyzerSamples = 256,
          logicAnalyzerUseCombinationalTrigger = true,
          patternGeneratorSamples = 256,
          patternGeneratorUseCombinationalTrigger = true
        )
        case _ => throw new CDEMatchError
      }
    ) ++
    ConfigBuilder.nastiTLParams(id) ++
    TunerConfigBuilder(id + ":tuner1", tuner1Config, () => getGenType(), () => DspComplex(getGenType(), getGenType())) ++
    FIRConfigBuilder(id + ":fir1", fir1Config, () => DspComplex(getGenType(), getGenType()), Some(() => DspComplex(getGenType(), getGenType()))) ++
    TunerConfigBuilder(id + ":tuner2", tuner2Config, () => DspComplex(getGenType(), getGenType()), () => DspComplex(getGenType(), getGenType())) ++
    FIRConfigBuilder(id + ":fir2", fir2Config, () => DspComplex(getGenType(), getGenType()), Some(() => DspComplex(getGenType(), getGenType()))) ++
    PFBConfigBuilder(id + ":pfb", pfbConfig, () => DspComplex(getGenType(), getGenType()), doubleToGen) ++
    FFTConfigBuilder(id + ":fft", fftConfig, () => getGenType())
  }
}

class Craft2BaseConfig extends Config(
  new WithCraft2DSP ++
  new WithSerialAdapter ++
  //new WithL2Capacity(8192) ++
  //new WithHwachaAndDma ++
  //new HwachaConfig ++ // also inserts L2 Cache
  new WithDma ++
  new WithNL2AcquireXacts(4) ++
  new WithNBanksPerMemChannel(16) ++ // how many mem channels do we get?
  // new Process28nmConfig ++  // uncomment if the critical path is in the FMA in Hwacha
  new rocketchip.BaseConfig)

class WithHwachaAndDma extends Config (
  (pname, site, here) => pname match {
    case HwachaNLanes => 4
    case BuildRoCC => {
      import HwachaTestSuites._
      TestGeneration.addSuites(rv64uv.map(_("p")))
      TestGeneration.addSuites(rv64uv.map(_("vp")))
      // no excep or vm in v4 yet
      //TestGeneration.addSuites((if(site(UseVM)) List("pt","v") else List("pt")).flatMap(env => rv64uv.map(_(env))))
      TestGeneration.addSuite(rv64sv("p"))
      TestGeneration.addSuite(hwachaBmarks)
      TestGeneration.addVariable("SRC_EXTENSION", "$(base_dir)/hwacha/$(src_path)/*.scala")
      TestGeneration.addVariable("DISASM_EXTENSION", "--extension=hwacha")
      Seq(
        RoccParameters( // From hwacha/src/main/scala/configs.scala
          opcodes = OpcodeSet.custom0 | OpcodeSet.custom1,
          generator = (p: Parameters) => {
            Module(new Hwacha()(p.alterPartial({
            case FetchWidth => 1
            case CoreInstBits => 64
            })))
            },
          nMemChannels = site(HwachaNLanes),
          nPTWPorts = 2 + site(HwachaNLanes), // icache + vru + vmus
          useFPU = true),
        RoccParameters( // From dma/src/main/scala/Configs.scala
          opcodes = OpcodeSet.custom2,
          generator = (p: Parameters) => Module(new CopyAccelerator()(p)),
          nMemChannels = site(NDmaTrackers),
          nPTWPorts = 1))
    }
    case RoccMaxTaggedMemXacts => max(
      max(site(HwachaNVLTEntries), site(HwachaNSMUEntries)),
      3 * site(NDmaTrackerMemXacts))
    case _ => throw new CDEMatchError
  }
)


class Craft2Config extends Config(ChainBuilder.fullChain() ++ new Craft2BaseConfig)
class Craft2AFBConfig extends Config(ChainBuilder.afbChain() ++ new Craft2BaseConfig)
