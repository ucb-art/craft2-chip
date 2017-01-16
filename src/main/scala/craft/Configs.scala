package craft

import chisel3._
import cde.{Parameters, Config, CDEMatchError}
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspjunctions._
import _root_.junctions._
import rocketchip.PeripheryUtils
import testchipip.WithSerialAdapter
import uncore.tilelink.ClientUncachedTileLinkIO

import pfb._
import sam._

import chisel3.core.ExplicitCompileOptions.NotStrict

class WithCraft2DSP extends Config(
  (pname, site, here) => pname match {
    case BuildCraft2DSP => (control_port: ClientUncachedTileLinkIO, data_port: ClientUncachedTileLinkIO, p: Parameters) => {
      implicit val q = p
      val chain = Module(new DspChain)
      chain.io.control_axi <> PeripheryUtils.convertTLtoAXI(control_port)
      chain.io.data_axi <> PeripheryUtils.convertTLtoAXI(data_port)
      ()
    }
    case SAMKey => SAMConfig(16, 16, 16)
    case _ => throw new CDEMatchError
  })

object ChainBuilder {
  def pfbChain(pfbConfig: PFBConfig = PFBConfig()): Config = {
    new Config(
      (pname, site, here) => pname match {
        case PFBKey => PFBConfig()
        case GenKey => new GenParameters {
          def getReal(): DspReal = DspReal(0.0).cloneType
          def genIn [T <: Data] = getReal().asInstanceOf[T]
          override def genOut[T <: Data] = getReal().asInstanceOf[T]
          val lanesIn = 8
          override val lanesOut = 8
        }
        case DspChainKey => DspChainParameters(
          blocks = Seq(
            q => {
              implicit val p = q
              new LazyPFBBlock[DspReal]
            }
          ),
          baseAddr = 0
        )
        case _ => throw new CDEMatchError
      }
    )
  }
}

class Craft2Config extends Config(ChainBuilder.pfbChain() ++ new WithCraft2DSP ++ new example.DefaultExampleConfig
  ++ new WithCraft)
