package craft

import chisel3._
import chisel3.experimental._
import cde.{Parameters, Config, CDEMatchError}
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspblocks._
import dspjunctions._
import _root_.junctions._
import rocketchip.PeripheryUtils
import rocketchip.HwachaConfig
import testchipip.WithSerialAdapter
import uncore.tilelink.ClientUncachedTileLinkIO
import uncore.converters.TileLinkWidthAdapter
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
      val chain = Module(new DspChain()(p))
      // add width adapter because Hwacha needs 128-bit TL
      chain.io.control_axi <> PeripheryUtils.convertTLtoAXI(TileLinkWidthAdapter(control_port, chain.ctrlXbarParams))
      chain.io.data_axi <> PeripheryUtils.convertTLtoAXI(TileLinkWidthAdapter(data_port, chain.dataXbarParams))
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
          dataBaseAddr = 0x2000,
          ctrlBaseAddr = 0x3000
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
  new WithDma ++
  new WithSerialAdapter ++
  new HwachaConfig ++
  // new WithNL2AcquireXacts(4) ++
  // new Process28nmConfig ++  // uncomment if the critical path is in the FMA in Hwacha
  // new WithL2Cache ++ // defined in HwachaConfig
  new rocketchip.BaseConfig)


class Craft2Config extends Config(ChainBuilder.afbChain() ++ new Craft2BaseConfig)
