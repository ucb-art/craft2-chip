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
import uncore.coherence._
import coreplex._
import scala.math.max
import util._

import rocket._
import groundtest._
import rocketchip.DefaultTestSuites._

//import ComplexModuleImpl._
import fft._
import pfb._
import sam._
import dma._

import chisel3.core.ExplicitCompileOptions.NotStrict

object ChainBuilder {
  type T = FixedPoint

  ///////////////////////////////////////////////////////////////
  ////////////            Here be acmes configuration parameters
  ///////////////////////////////////////////////////////////////

  def acmes(id: String = "acmes", channels: Int = 64): Config = {

    val lanes = 32
    val numTaps = 4
    val quadrature = true

    // Here be the bit manipulation 1 block, this is just to add a SAM to the ADC output, it does nothing to the bits
    def bm1Config() = BitManipulationConfig(lanes = lanes)
    def bm1Input():T = FixedPoint(9.W, 0.BP)
    def bm1Output():T = FixedPoint(9.W, 0.BP)
    def bm1Connect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)
    def bm1SAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 1024)) // TODO was 4096

    // Here be the filterbank
    // notes on windows: the sincHamming doesn't go to zero on the edges, so Hanning is preferred
    val pd = if (quadrature) channels/(lanes/2)*(numTaps-1) else channels/lanes*(numTaps-1)
    def pfbConfig() = PFBConfig(windowFunc = sincHanning.apply, processingDelay = pd, numTaps = numTaps, outputWindowSize = channels, lanes = lanes, multiplyPipelineDepth = 1, outputPipelineDepth = 1, genTap = Some(pfbTap), quadrature = quadrature)
    def pfbInput():T = FixedPoint(9.W, 8.BP)
    // [stevo]: make sure pfbTap and pfbConvert use the same width and binary point
    def pfbTap:T = FixedPoint(10.W, 7.BP)
    def pfbConvert(x: Double):T = FixedPoint.fromDouble(x, 10.W, 7.BP)
    def pfbOutput():T = FixedPoint(12.W, 8.BP)
    def pfbConnect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)
    def pfbSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 1024)) // TODO was 4096

    // Here be the Fourier transform
    def fftConfig() = FFTConfig(n = channels, lanes = lanes, pipelineDepth = 13, real = true, quadrature = quadrature)
    def fftInput():T = FixedPoint(12.W, 8.BP) // gets complexed automatically
    def fftOutput():T = FixedPoint(18.W, 8.BP) // gets complexed automatically
    def fftConnect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)
    def fftSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 1024)) // TODO was 4096

    // Here be the cross calibration block
    def ccConfig() = CrossCalibrateConfig(channels = channels, lanes = lanes, pipelineDepth = 1)
    def ccInput():T = FixedPoint(18.W, 8.BP) // gets complexed automatically
    // note: as a hack, the calibration coefficient bitwidth is set in the CrossCalibrate file as 2 total bits less than the input bitwidth (same fractional width)
    def ccOutput():T = FixedPoint(18.W, 8.BP) // gets complexed automatically
    def ccConnect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)
    def ccSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 1024)) // TODO was 4096

    // Here be the power block
    def powerConfig() = PowerConfig(lanes = lanes, pipelineDepth = 1, quadrature = quadrature)
    def powerInput():T = FixedPoint(18.W, 8.BP) // gets complexed automatically
    def powerOutput():T = FixedPoint(36.W, 8.BP)
    def powerConnect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)
    def powerSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 1024)) // TODO was 4096

    // Here be the accumulator block
    def accumConfig() = AccumulatorConfig(lanes = lanes, outputWindowSize = channels, maxSpectra = 2048, quadrature = quadrature)
    def accumInput():T = FixedPoint(36.W, 8.BP)
    def accumOutput():T = FixedPoint(64.W, 8.BP)
    def accumConnect() = BlockConnectionParameters(connectPG = true, connectLA = false, addSAM = true)
    def accumSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 512)) // TODO was 4096 then 512

  ///////////////////////////////////////////////////////////////
  ////////////                                     Here be acmes 
  ///////////////////////////////////////////////////////////////

    new Config(
      (pname, site, here) => pname match {
        case DefaultSAMKey => SAMConfig(1, 4096)
        case DspChainId => id
        case DspChainKey(_id) if _id == id => DspChainParameters(
          blocks = Seq(
            // (parameter to block function, unique id, BlockConnectionParameters, SAM Config (optional))
            (implicit p => new BitManipulationBlock[T], id + ":bm1", bm1Connect(), bm1SAMConfig()),
            (implicit p => new PFBBlock[T], id + ":pfb", pfbConnect(), pfbSAMConfig()),
            (implicit p => new FFTBlock[T], id + ":fft", fftConnect(), fftSAMConfig()),
            (implicit p => new CrossCalibrateBlock[T], id + ":cc", ccConnect(), ccSAMConfig()),
            (implicit p => new PowerBlock[T], id + ":power", powerConnect(), powerSAMConfig()),
            (implicit p => new AccumulatorBlock[T], id + ":accum", accumConnect(), accumSAMConfig())
          ),
          logicAnalyzerSamples = 8192, 
          logicAnalyzerUseCombinationalTrigger = true, 
          patternGeneratorSamples = 8192, // TODO what should it be? was 8192
          patternGeneratorUseCombinationalTrigger = true,
          biggestWidth = 2048
        )
        case _ => throw new CDEMatchError
      }
    ) ++
    ConfigBuilder.writeHeaders("./tests") ++
    ConfigBuilder.nastiTLParams(id) ++
    BitManipulationConfigBuilder(id + ":bm1", bm1Config(), bm1Input, bm1Output) ++ 
    PFBConfigBuilder(id + ":pfb", pfbConfig(), pfbInput, pfbConvert, Some(() => pfbOutput), Some(pfbTap)) ++
    FFTConfigBuilder(id + ":fft", fftConfig(), fftInput, Some(() => fftOutput)) ++
    CrossCalibrateConfigBuilder(id + ":cc", ccConfig(), ccInput, Some(() => ccOutput)) ++
    PowerConfigBuilder(id + ":power", powerConfig(), powerInput, Some(() => powerOutput)) ++
    AccumulatorConfigBuilder(id + ":accum", accumConfig(), accumInput, Some(() => accumOutput))
  }

}

class WithMiniSerialAdapter extends Config(
  (pname, site, here) => pname match {
    case SerialInterfaceWidth => 32
    case _ => throw new CDEMatchError
  }
)

//class WithFPGAOptions(fpga: Boolean) extends Config(
//  (pname, site, here) => pname match {
//    case FPGA => fpga
//    case _ => throw new CDEMatchError
//  }
//)

class AcmesBaseConfig extends Config(
  new WithL2Capacity(512) ++
  new WithL2Cache ++
  new WithExtMemSize(8L * 1024L * 1024L) ++
  new WithNMemoryChannels(8) ++
  new WithSRAM(4) ++
  new WithDma ++
  new WithMiniSerialAdapter ++
  new rocketchip.BaseConfig)

class AcmesTinyBaseConfig extends Config(
  new WithL2Capacity(8) ++
  new WithL2Cache ++
  new WithExtMemSize(256L * 1024L) ++
  new WithNMemoryChannels(8) ++
  new WithSRAM(4) ++
  new WithDma ++
  new WithMiniSerialAdapter ++
  new rocketchip.BaseConfig)

class AcmesConfig extends Config(ChainBuilder.acmes(channels=64) ++ new AcmesBaseConfig)
class AcmesTinyConfig extends Config(ChainBuilder.acmes(channels=64) ++ new AcmesTinyBaseConfig)
class AcmesFPGAConfig extends Config(ChainBuilder.acmes(channels=64) ++ new AcmesTinyBaseConfig)
