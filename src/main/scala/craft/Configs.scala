package craft

import chisel3._
import cde.{Parameters, Config, CDEMatchError}
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import _root_.junctions._
import rocketchip.PeripheryUtils
import testchipip.WithSerialAdapter
import uncore.tilelink.ClientUncachedTileLinkIO

import pfb._
import sam._

import chisel3.core.ExplicitCompileOptions.NotStrict

class WithCraft2DSP extends Config(
  (pname, site, here) => pname match {
    case BuildCraft2DSP => (port: ClientUncachedTileLinkIO, p: Parameters) => {
      val chain = Module(new DspChain()(p))
      chain.io.axi <> PeripheryUtils.convertTLtoAXI(port)
      ()
    }
    case _ => throw new CDEMatchError
  })

object ChainBuilder {
  def pfbChain(pfbConfig: PFBConfig = PFBConfig()): Config = {
    new Config(
      (pname, site, here) => pname match {
        case DspChainKey => DspChainParameters(
          blocks = Seq(
            q => {
              implicit val p = q
              new PFBBlock[DspReal]
            }
          ),
          baseAddr = 0,
          samConfig = SAMConfig(subpackets=10, bufferDepth=10, baseAddr=0),
          logicAnalyzerSamples = 128,
          patternGeneratorSamples = 128
        )
        case _ => throw new CDEMatchError
      }
    ) ++ new pfb.DspConfig
  }
}

class Craft2Config extends Config(ChainBuilder.pfbChain() ++ new WithCraft2DSP ++ new example.DefaultExampleConfig)
