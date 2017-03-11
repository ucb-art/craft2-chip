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
import rssi._

import chisel3.core.ExplicitCompileOptions.NotStrict

class WithCraft2DSP extends Config(
  (pname, site, here) => pname match {
    case BuildCraft2DSP => (control_port: ClientUncachedTileLinkIO, data_port: ClientUncachedTileLinkIO, io: Bundle with ADCTopLevelIO, p: Parameters) => {
      /*implicit val q = p
      val dataBaseAddr = 0x3000
      val ctrlBaseAddr = 0x2000
      val dsp_clock = Wire(Clock())
      val lazyChain = LazyModule(new DspChainWithADC(ctrlBaseAddr, dataBaseAddr, override_clock= Some(dsp_clock)))
      val chain = Module(lazyChain.module)
      dsp_clock := chain.io.adc_clk_out
      // add width adapter because Hwacha needs 128-bit TL
      chain.io.control_axi <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkTo(to_clock=dsp_clock, to_reset=chain.reset, TileLinkWidthAdapter(control_port, chain.ctrlXbarParams)))
      chain.io.data_axi <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkTo(to_clock=dsp_clock, to_reset=chain.reset, TileLinkWidthAdapter(data_port, chain.dataXbarParams)))
      io <> chain.io*/
      ()
    }
    case BuildCLKRX => (io: Bundle with CLKRXTopLevelInIO with CLKRXTopLevelOutIO) => {
      val m = Module(new CLK_RX_amp_buf)
      io <> m.io
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
            (implicit p => new PFBBlock[DspComplex[T]], id + ":pfb", BlockConnectEverything, site(DefaultSAMKey)),
            (implicit p => new FFTBlock[T],             id + ":fft", BlockConnectEverything, site(DefaultSAMKey))
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
            (implicit p => new TunerBlock[T, T], id + ":tuner1", BlockConnectEverything, site(DefaultSAMKey)),
            (implicit p => new FIRBlock[DspComplex[T]], id + ":fir1", BlockConnectEverything, site(DefaultSAMKey)),
            (implicit p => new TunerBlock[DspComplex[T], T], id + ":tuner2", BlockConnectEverything, site(DefaultSAMKey)),
            (implicit p => new FIRBlock[DspComplex[T]], id + ":fir2", BlockConnectEverything, site(DefaultSAMKey)),
            (implicit p => new PFBBlock[DspComplex[T]], id + ":pfb", BlockConnectEverything, site(DefaultSAMKey)),
            (implicit p => new FFTBlock[T],             id + ":fft", BlockConnectEverything, site(DefaultSAMKey))
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

  ///////////////////////////////////////////////////////////////
  ////////////            Here be radar configuration parameters
  ///////////////////////////////////////////////////////////////

  // Here be the bit manipulation 1 block, this is just to add a SAM to the ADC output, it does nothing to the bits
  def bm1Config() = BitManipulationConfig(lanes = 32)
  def bm1Input():T = FixedPoint(9.W, 0.BP)
  def bm1Output():T = FixedPoint(9.W, 0.BP)
  def bm1Connect() = BlockConnectionParameters(connectPG = false, connectLA = true, addSAM = true)
  def bm1SAMConfig() = SAMConfig(subpackets = 1, bufferDepth = 128)

  // Here be the bit manipulation 2 block, which truncates the 9-bit ADC output into 8 bits
  def bm2Config() = BitManipulationConfig(lanes = 32)
  def bm2Input():T = FixedPoint(9.W, 0.BP)
  def bm2Output():T = FixedPoint(8.W, 0.BP)
  def bm2Connect() = BlockConnectNothing

  // Here be the tuner
  def tunerConfig() = TunerConfig(pipelineDepth = 4, lanes = 32, phaseGenerator = "Fixed", mixerTableSize = 32)
  def tunerInput():T = FixedPoint(8.W, 7.BP)
  def tunerMixer():DspComplex[T] = DspComplex(FixedPoint(9.W, 7.BP), FixedPoint(9.W, 7.BP))
  def tunerOutput():DspComplex[T] = DspComplex(FixedPoint(8.W, 7.BP), FixedPoint(8.W, 7.BP))
  def tunerConnect() = BlockConnectionParameters(connectPG = true, connectLA = false, addSAM = false)

  // Here be the filter-decimator
  def firConfig() = FIRConfig(numberOfTaps = 136, lanesIn = 32, lanesOut = 4, processingDelay = 2)
  def firInput():DspComplex[T] = DspComplex(FixedPoint(8.W, 7.BP), FixedPoint(8.W, 7.BP))
  def firTaps():DspComplex[T] = DspComplex(FixedPoint(8.W, 10.BP), FixedPoint(8.W, 10.BP))
  def firOutput():DspComplex[T] = DspComplex(FixedPoint(11.W, 10.BP), FixedPoint(11.W, 10.BP))
  def firConnect() = BlockConnectionParameters(connectPG = false, connectLA = true, addSAM = true)
  def firSAMConfig() = SAMConfig(subpackets = 1, bufferDepth = 128)

  // Here be the filterbank
  def pfbConfig() = PFBConfig(processingDelay = 192, numTaps = 12, outputWindowSize = 128, lanes = 4)
  def pfbInput():DspComplex[T] = DspComplex(FixedPoint(11.W, 10.BP), FixedPoint(11.W, 10.BP))
  // [stevo]: this looks weird, but it sets the bitwidths of the taps
  def pfbTaps(x: Double):DspComplex[T] = DspComplex(FixedPoint.fromDouble(x, 11.W, 17.BP), FixedPoint.fromDouble(0.0, 11.W, 17.BP))
  def pfbOutput():DspComplex[T] = DspComplex(FixedPoint(11.W, 17.BP), FixedPoint(11.W, 17.BP))
  def pfbConnect() = BlockConnectionParameters(connectPG = true, connectLA = false, addSAM = false)

  // Here be the Fourier transform
  def fftConfig() = FFTConfig(n = 128, lanes = 4, pipelineDepth = 4)
  def fftInput():T = FixedPoint(11.W, 17.BP) // gets complexed automatically
  def fftOutput():T = FixedPoint(15.W, 14.BP) // gets complexed automatically
  def fftConnect() = BlockConnectionParameters(connectPG = false, connectLA = true, addSAM = true)
  def fftSAMConfig() = SAMConfig(subpackets = 128/4, bufferDepth = 16)

  // Here be the receive signal strength indicator
  def rssiConfig() = RSSIConfig(numChannels = 4, numLanes = 4, maxIntegrator2nLength = 4)
  def rssiInput(): T = FixedPoint(15.W, 14.BP) // gets complexed automatically
  def rssiThresh(): T = FixedPoint(35.W, 14.BP) // threshold type, should probably be (input_width*2+1+maxIntegrator2nLength).W, (input_bp*2).BP
  def rssiConnect() = BlockConnectEverything
  def rssiSAMConfig() = SAMConfig(subpackets = 128/4, bufferDepth = 16)


  ///////////////////////////////////////////////////////////////
  ////////////                                     Here be radar 
  ///////////////////////////////////////////////////////////////

  def radar(id: String = "craft-radar"): Config = {
    new Config(
      (pname, site, here) => pname match {
        case DefaultSAMKey => SAMConfig(16, 16)
        case DspChainId => id
        case DspChainKey(_id) if _id == id => DspChainParameters(
          blocks = Seq(
            (implicit p => new BitManipulationBlock[T], id + ":bm1", bm1Connect(), Some(bm1SAMConfig())),
            (implicit p => new BitManipulationBlock[T], id + ":bm2", bm2Connect(), None),
            (implicit p => new TunerBlock[T, T], id + ":tuner", tunerConnect(), None),
            (implicit p => new FIRBlock[DspComplex[T]], id + ":fir", firConnect(), None),
            (implicit p => new PFBBlock[DspComplex[T]], id + ":pfb", pfbConnect(), None),
            (implicit p => new FFTBlock[T], id + ":fft", fftConnect(), Some(fftSAMConfig())),
            (implicit p => new RSSIBlock[T], id + ":rssi", rssiConnect(), Some(rssiSAMConfig()))
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
    BitManipulationConfigBuilder(id + ":bm1", bm1Config(), bm1Input, bm1Output) ++ 
    BitManipulationConfigBuilder(id + ":bm2", bm2Config(), bm2Input, bm2Output) ++ 
    TunerConfigBuilder(id + ":tuner", tunerConfig(), tunerInput, tunerOutput, Some(() => tunerMixer)) ++
    FIRConfigBuilder(id + ":fir", firConfig(), firInput, Some(() => firOutput), Some(() => firTaps)) ++
    PFBConfigBuilder(id + ":pfb", pfbConfig(), pfbInput, pfbTaps, Some(() => pfbOutput)) ++
    FFTConfigBuilder(id + ":fft", fftConfig(), fftInput, Some(() => fftOutput)) ++
    RSSIConfigBuilder(id + ":rssi", rssiConfig(), rssiInput, rssiThresh)
  }
}

class Craft2BaseConfig extends Config(
  new WithCraft2DSP ++
  new WithSerialAdapter ++
  // new Process28nmConfig ++  // uncomment if the critical path is in the FMA in Hwacha
  new rocketchip.BaseConfig)

class Craft2DefaultConfig extends Config(
  new WithL2Capacity(512) ++
  new WithL2Cache ++
  new WithExtMemSize(8L * 1024L * 1024L) ++
  new WithNL2AcquireXacts(4) ++
  new WithNMemoryChannels(8) ++
  new WithSRAM(4) ++
  new WithHwachaAndDma ++
  new DefaultHwachaConfig ++
  new WithDma ++
  new Craft2BaseConfig)

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

class WithExtraMMIOOutputs(n: Int) extends Config(
  (pname, site, here) => pname match {
    case ExtraMMIOOutputs => Dump("EXTRA_MMIO_OUTPUTS", n)
    case BuildPeripheryExtra => (extra_ios: Seq[ClientUncachedTileLinkIO], io: Bundle with PeripheryExtraBundle, p: Parameters) => {
      // add width adapter because Hwacha needs 128-bit TL
      extra_ios.zipWithIndex.foreach { case(port, i) => {
        io.peripheryExtraAxi(i) <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkFrom(from_clock=io.peripheryExtraClock(i).asClock, from_reset=io.peripheryExtraReset(i), port))//TileLinkWidthAdapter(port, p)))
      }}
      ()
    }
    case _ => throw new CDEMatchError
  }
)


class WithSimpleOptions extends Config(
  new WithL2Capacity(512) ++
  new WithL2Cache ++
  new WithSRAM(1) ++
  new WithExtraMMIOOutputs(1) ++
  new WithExtMemSize(8 * 1024L * 1024L))

class Craft2Config extends Config(ChainBuilder.radar() ++ new Craft2DefaultConfig)
class Craft2SimpleConfig extends Config(ChainBuilder.radar() ++
  new WithSimpleOptions ++ new Craft2BaseConfig)
class Craft2DefaultChainConfig extends Config(ChainBuilder.fullChain() ++
  new Craft2DefaultConfig)
class Craft2DefaultAFBConfig extends Config(ChainBuilder.afbChain() ++
  new Craft2DefaultConfig)
