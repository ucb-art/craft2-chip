package craft

import chisel3._
import chisel3.experimental._
import scala.collection.mutable.{LinkedHashSet, ListBuffer}
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

import fft._
import pfb._
import sam._

import chisel3.core.ExplicitCompileOptions.NotStrict

class WithCraft2DSP extends Config(
  (pname, site, here) => pname match {
    case BuildCraft2DSP => (control_port: ClientUncachedTileLinkIO, data_port: ClientUncachedTileLinkIO, streamIn: ValidWithSync[UInt], dsp_clock: Clock, p: Parameters) => {
      implicit val q = p
      val dataBaseAddr = 0x2000
      val ctrlBaseAddr = 0x3000
      val lazyChain = LazyModule(new LazyDspChain(ctrlBaseAddr, dataBaseAddr, override_clock= Some(dsp_clock)))
      val chain = Module(lazyChain.module)
      // add width adapter because Hwacha needs 128-bit TL
      chain.io.control_axi <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkTo(to_clock=dsp_clock, to_reset=chain.reset, TileLinkWidthAdapter(control_port, chain.ctrlXbarParams)))
      chain.io.data_axi <> PeripheryUtils.convertTLtoAXI(AsyncUTileLinkTo(to_clock=dsp_clock, to_reset=chain.reset, TileLinkWidthAdapter(data_port, chain.dataXbarParams)))
      chain.io.stream_in := streamIn
      ()
    }
    case _ => throw new CDEMatchError
  })

object ChainBuilder {
  type T = FixedPoint
  def getGenType(): T = FixedPoint(32.W, 16.BP)
  def afbChain(
    id: String = "craft-afb",
    fftConfig: FFTConfig = FFTConfig(),
    pfbConfig: PFBConfig = PFBConfig(),
    lanes: Int = 8): Config = {
    new Config(
      (pname, site, here) => pname match {
        case SAMKey => SAMConfig(16, 16, 16)
        case DspChainId => id
        case DspChainKey(id) => DspChainParameters(
          blocks = Seq(
            (implicit p => new LazyPFBBlock[DspComplex[T]], id + ":pfb"),
            (implicit p => new LazyFFTBlock[T],             id + ":fft")
          ),
          logicAnalyzerSamples = 256,
          logicAnalyzerUseCombinationalTrigger = true,
          patternGeneratorSamples = 256,
          patternGeneratorUseCombinationalTrigger = true
        )
        case _ => throw new CDEMatchError
      }
    ) ++
    FFTConfigBuilder(id + ":fft", fftConfig, () => getGenType()) ++
    PFBConfigBuilder(id + ":pfb", pfbConfig, () => getGenType())
  }
}

class Craft2BaseConfig extends Config(
  new WithCraft2DSP ++
  new WithSerialAdapter ++
  new WithL2Capacity(8192) ++ 
  new WithHwachaAndDma ++ 
  new HwachaConfig ++ // also inserts L2 Cache
  new WithDma ++
  new WithNL2AcquireXacts(4) ++
  // new Process28nmConfig ++  // uncomment if the critical path is in the FMA in Hwacha
  new rocketchip.BaseConfig)

class WithHwachaAndDma extends Config (
  (pname, site, here) => pname match {
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


class Craft2Config extends Config(ChainBuilder.afbChain() ++ new Craft2BaseConfig)
