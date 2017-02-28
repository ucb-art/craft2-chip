package craft

import chisel3._
import rocketchip._
import coreplex.BaseCoreplexBundle
import cde.Parameters
import testchipip._
import diplomacy.LazyModule
import uncore.util.{UncachedTileLinkDepths, TileLinkEnqueuer}
import uncore.tilelink.TLId

trait PeripherySRAM extends LazyModule {
  // Nothin'
}

trait PeripherySRAMBundle {
  // Nothin'
}

trait PeripherySRAMModule {
  implicit val p: Parameters
  val outer: PeripherySRAM
  val io: PeripherySRAMBundle
  val coreplexIO: BaseCoreplexBundle

  val depths = UncachedTileLinkDepths(1, 2)

  coreplexIO.master.mem.foreach { master =>
    val memParams = p.alterPartial({ case TLId => "L2toMC" })
    val sram = Module(new MainSRAMChannel()(memParams))
    sram.io <> TileLinkEnqueuer(master, depths)
  }
}
