package craft

import chisel3._
import cde.{Parameters, Config, CDEMatchError}
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspblocks._
import dspjunctions._
import _root_.junctions._
import rocketchip.PeripheryUtils
import testchipip.WithSerialAdapter
import uncore.tilelink.ClientUncachedTileLinkIO
import coreplex.WithL2Cache
import dma._

import fft._
import pfb._
import sam._

import chisel3.core.ExplicitCompileOptions.NotStrict

class WithCraft2DSP extends Config(
  (pname, site, here) => pname match {
    case BuildCraft2DSP => (control_port: ClientUncachedTileLinkIO, data_port: ClientUncachedTileLinkIO, streamIn: ValidWithSync[UInt], p: Parameters) => {
      implicit val q = p
      val chain = Module(new DspChain)
      chain.io.control_axi <> PeripheryUtils.convertTLtoAXI(control_port)
      chain.io.data_axi <> PeripheryUtils.convertTLtoAXI(data_port)
      chain.io.stream_in := streamIn
      ()
    }
    case _ => throw new CDEMatchError
  })

object ChainBuilder {
  type T = FixedPoint
  def getGenType(): T = FixedPoint(32.W, 16.BP)
  def afbChain(fftConfig: FFTConfig = FFTConfig(), pfbConfig: PFBConfig = PFBConfig(), lanes: Int = 8): Config = {
    new Config(
      (pname, site, here) => pname match {
        case SAMKey => SAMConfig(16, 16, 16)
        case FFTKey => fftConfig.copy(lanes = site(GenKey(site(DspBlockId))).lanesIn)
        case PFBKey => pfbConfig
        case GenKey("fft") => site(GenKey("pfb")) /*new GenParameters {
          def genIn [V <: Data] = getGenType().asInstanceOf[V]
          val lanesIn = lanes
          override val lanesOut = lanes
        }*/
        case GenKey("pfb") => new GenParameters { // site(GenKey("fft"))
          def genIn [V <: Data] = DspComplex(getGenType(), getGenType()).asInstanceOf[V]
          val lanesIn = lanes
          override val lanesOut = lanes
        }
          
        case DspChainId => "craft-afb"
        case DspChainKey("craft-afb") => DspChainParameters(
          blocks = Seq(
            (implicit p => new LazyPFBBlock[DspComplex[T]], "pfb"),
            (implicit p => new LazyFFTBlock[T],             "fft")
          ),
          dataBaseAddr = 0x2000,
          ctrlBaseAddr = 0x3000
        )
        case _ => throw new CDEMatchError
      }
    )
  }
  def pfbChain(pfbConfig: PFBConfig = PFBConfig()): Config = {
    new Config(
      (pname, site, here) => pname match {
        case SAMKey => SAMConfig(16, 16, 16)
        case PFBKey => PFBConfig()
        case DspBlockId => "pfb"
        case GenKey("pfb") => new GenParameters {
          def getGenType(): T = DspReal(0.0).cloneType
          def genIn [T <: Data] = getGenType().asInstanceOf[T]
          override def genOut[T <: Data] = getGenType().asInstanceOf[T]
          val lanesIn = 8
          override val lanesOut = 8
        }
        case DspChainId => "craft-pfb"
        case DspChainKey("craft-pfb") => DspChainParameters(
          blocks = Seq(
            (implicit q => new LazyPFBBlock[DspReal], "pfb")
          ),
          dataBaseAddr = 0,
          ctrlBaseAddr = 0x1000
        )
        case _ => throw new CDEMatchError
      }
    )
  }
}

class Craft2BaseConfig extends Config(
  new WithCraft2DSP ++
  // new WithCraft ++
  new WithDma ++
  new WithL2Cache ++
  new WithSerialAdapter ++
  new rocketchip.BaseConfig)


class Craft2Config extends Config(ChainBuilder.afbChain() ++ new Craft2BaseConfig)
