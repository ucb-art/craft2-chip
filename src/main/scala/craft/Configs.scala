package craft

import cde.{Parameters, Config, CDEMatchError}
import testchipip.WithSerialAdapter
import uncore.tilelink.ClientUncachedTileLinkIO
import rocketchip.PeripheryUtils
import chisel3._

class WithCraft2DSP extends Config(
  (pname, site, here) => pname match {
    case BuildCraft2DSP => (port: ClientUncachedTileLinkIO, p: Parameters) => {
      val craft = Module(new Craft2DSP()(p))
      craft.io.axi <> PeripheryUtils.convertTLtoAXI(port)
      ()
    }
    case _ => throw new CDEMatchError
  })


class Craft2Config extends Config(new WithCraft2DSP ++ new example.DefaultExampleConfig)
