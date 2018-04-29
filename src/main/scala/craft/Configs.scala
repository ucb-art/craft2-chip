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
import scala.math.max
import util._

import rocket._
import groundtest._
import rocketchip.DefaultTestSuites._

//import ComplexModuleImpl._
import fft._
import pfb._
import sam._

import chisel3.core.ExplicitCompileOptions.NotStrict

object ChainBuilder {
  type T = FixedPoint

  ///////////////////////////////////////////////////////////////
  ////////////            Here be acmes configuration parameters
  ///////////////////////////////////////////////////////////////

  val channels = 8192
  val lanes = 32
  val numTaps = 4

  // Here be the bit manipulation 1 block, this is just to add a SAM to the ADC output, it does nothing to the bits
  def bm1Config() = BitManipulationConfig(lanes = lanes)
  def bm1Input():T = FixedPoint(9.W, 0.BP)
  def bm1Output():T = FixedPoint(9.W, 0.BP)
  def bm1Connect() = BlockConnectionParameters(connectPG = false, connectLA = false, addSAM = true)
  def bm1SAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 4096))

  // Here be the filterbank
  def pfbInput():T = FixedPoint(9.W, 8.BP)
  // [stevo]: make sure pfbTap and pfbConvert use the same width and binary point
  def pfbTap:T = FixedPoint(10.W, 7.BP)
  def pfbConvert(x: Double):T = FixedPoint.fromDouble(x, 10.W, 7.BP)
  def pfbOutput():T = FixedPoint(11.W, 8.BP)
  def pfbConnect() = BlockConnectionParameters(connectPG = true, connectLA = false, addSAM = false)
  def pfbSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 4096))
  def pfbConfig() = PFBConfig(windowFunc = sincHamming.apply, processingDelay = channels/lanes*(numTaps-1), numTaps = numTaps, outputWindowSize = channels, lanes = lanes, multiplyPipelineDepth = 1, outputPipelineDepth = 1, genTap = Some(pfbTap))

  // Here be the Fourier transform
  def fftConfig() = FFTConfig(n = channels, lanes = lanes, pipelineDepth = 13, real = true)
  def fftInput():T = FixedPoint(11.W, 8.BP) // gets complexed automatically
  def fftOutput():T = FixedPoint(16.W, 4.BP) // gets complexed automatically
  def fftConnect() = BlockConnectionParameters(connectPG = true, connectLA = false, addSAM = true)
  def fftSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 4096))

  // Here be the power block
  def powerConfig() = PowerConfig(lanes = lanes, pipelineDepth = 1)
  def powerInput():T = FixedPoint(16.W, 4.BP) // gets complexed automatically
  def powerOutput():T = FixedPoint(32.W, 4.BP)
  def powerConnect() = BlockConnectionParameters(connectPG = true, connectLA = false, addSAM = false)
  def powerSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 4096))

  // Here be the accumulator block
  def accumConfig() = AccumulatorConfig(lanes = lanes, outputWindowSize = channels, maxSpectra = 1024)
  def accumInput():T = FixedPoint(32.W, 4.BP)
  def accumOutput():T = FixedPoint(64.W, 4.BP)
  def accumConnect() = BlockConnectionParameters(connectPG = true, connectLA = false, addSAM = true)
  def accumSAMConfig() = Some(SAMConfig(subpackets = 1, bufferDepth = 512))

  ///////////////////////////////////////////////////////////////
  ////////////                                     Here be acmes 
  ///////////////////////////////////////////////////////////////

  def acmes(id: String = "acmes"): Config = {
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
            (implicit p => new PowerBlock[T], id + ":power", powerConnect(), powerSAMConfig()),
            (implicit p => new AccumulatorBlock[T], id + ":accum", accumConnect(), accumSAMConfig())
          ),
          logicAnalyzerSamples = 8192, // does nothing
          logicAnalyzerUseCombinationalTrigger = true, // does nothing
          patternGeneratorSamples = 8192,
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
    PowerConfigBuilder(id + ":power", powerConfig(), powerInput, Some(() => powerOutput)) ++
    AccumulatorConfigBuilder(id + ":accum", accumConfig(), accumInput, Some(() => accumOutput))
  }

}

class AcmesBaseConfig extends Config(
  new WithL2Capacity(128) ++
  new WithL2Cache ++
  new WithExtMemSize(2L * 1024L * 1024L) ++
  new WithNMemoryChannels(8) ++
  new WithSRAM(4) ++
  new WithSerialAdapter ++
  new rocketchip.BaseConfig)

// TODO
class AcmesTinyBaseConfig extends Config(
  new WithL2Capacity(8) ++
  new WithL2Cache ++
  new WithExtMemSize(256L * 1024L) ++
  new WithNMemoryChannels(8) ++
  new WithSRAM(4) ++
  new WithSerialAdapter ++
  new rocketchip.BaseConfig)

class AcmesConfig extends Config(ChainBuilder.acmes() ++ new AcmesBaseConfig)
class AcmesTinyConfig extends Config(ChainBuilder.acmes() ++ new AcmesTinyBaseConfig)
