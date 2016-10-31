package craft

import cde.{Parameters, Config, CDEMatchError}
import testchipip.WithSerialAdapter
import uncore.tilelink.ClientUncachedTileLinkIO
import rocketchip.PeripheryUtils
import chisel3._

class WithTest extends Config(
  (pname, site, here) => pname match {
    case BuildTest => (port: ClientUncachedTileLinkIO, p: Parameters) => {
      val test = Module(new Test()(p))
      test.io.axi <> PeripheryUtils.convertTLtoAXI(port)
    }
    case _ => throw new CDEMatchError
  })


class TestConfig extends Config(new WithTest ++ new example.DefaultExampleConfig)
