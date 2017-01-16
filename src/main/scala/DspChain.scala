// See LICENSE for license details

package craft

import cde._
import chisel3._
import craft._
import debuggers._
import _root_.junctions._
import diplomacy.LazyModule
import dspjunctions._
import sam._
import testchipip._
import uncore.converters._
import uncore.tilelink._
import junctions._
import rocketchip._

case class DspChainParameters (
  blocks: Seq[Parameters => LazyDspBlock],
  baseAddr: Int
)

case object DspChainKey extends Field[DspChainParameters]

trait HasDspChainParameters {
  implicit val p: Parameters
  val blocks = p(DspChainKey).blocks
  val baseAddr = p(DspChainKey).baseAddr
}

class DspChainIO()(implicit val p: Parameters) extends Bundle with HasDspBlockParameters {
  val control_axi = new NastiIO().flip
  val data_axi = new NastiIO().flip
}

class DspChain(
  b: => Option[DspChainIO] = None,
  override_clock: Option[Clock]=None,
  override_reset: Option[Bool]=None)(implicit val p: Parameters)
  extends Module(override_clock, override_reset)
  with HasDspChainParameters {
  val io = IO(b.getOrElse(new DspChainIO))

  require(blocks.length > 0)
  var addr = 0
  val lazy_mods = blocks.map(b => {
    val modParams = p.alterPartial({
      case BaseAddr => addr
    })
    val mod = LazyModule(b(modParams))
    addr += mod.size
    mod
  })
  val modules = lazy_mods.map(mod => mod.module)
  val mod_ios = modules.map(_.io)

  val maxDataWidth = mod_ios.map(i =>
      math.max(i.in.bits.getWidth, i.out.bits.getWidth)
  ).reduce(math.max(_, _))

  val lastDataWidth = mod_ios.last.out.bits.getWidth

  for (i <- 0 until mod_ios.length - 1) {
    require(mod_ios(i+1).in.bits.getWidth == mod_ios(i).out.bits.getWidth,
      "Connecting modules with different width IOs")
    mod_ios(i + 1).in <> mod_ios(i).out
  }

  val oldSamConfig: SAMConfig = p(SAMKey)
  val samParams = p.alterPartial({ 
    case SAMKey => oldSamConfig.copy(baseAddr = addr)
    case DspBlockKey => DspBlockParameters(lastDataWidth, lastDataWidth)
    case BaseAddr => addr
  })
  val lazy_sam = LazyModule( new LazySAM()(samParams) )
  addr += lazy_sam.size
  val sam = lazy_sam.module

  val control_axis = mod_ios.map(_.axi)
  val outPorts = control_axis.length + 1 // + 1 for sam

  val xbarParams = p.alterPartial({
    case TLKey("XBar") => p(TLKey("MCtoEdge")).copy(
      nCachingClients = 0,
      nCachelessClients = outPorts,
      maxClientXacts = 4,
      maxClientsPerPort = 2)
    case TLId => "XBar"
    case GlobalAddrMap => {
      val memSize = 0x1000L
      // TODO generate address map correctly
      AddrMap(
        AddrMapEntry(s"chan0", MemSize(memSize - 0x200, MemAttr(AddrMapProt.RWX))),
        AddrMapEntry(s"chan1", MemSize(0x200, MemAttr(AddrMapProt.RWX))))
    }
    case XBarQueueDepth => 2
    case ExtMemSize => 0x1000L
    case InPorts => 2
    case OutPorts => outPorts
  })

  println(s"After instantiating everything, addr is $addr")
  println(s"We have ${control_axis.length} OutPorts on the XBar")
  println(s"We have ${xbarParams(GlobalAddrMap).numSlaves} slaves in the AddrMap")


  val xbar = Module(new CraftXBar(xbarParams))

  xbar.io.in(0) <> io.control_axi
  xbar.io.out.zip(control_axis).foreach{ case (xbar_axi, control_axi) => xbar_axi <> control_axi }

}
