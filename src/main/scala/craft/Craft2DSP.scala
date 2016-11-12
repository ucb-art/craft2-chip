package craft

import Chisel._
//import chisel3.util._
import cde.{Parameters, Field}
import uncore.tilelink._
import junctions._
import diplomacy._
import fft._
import rocketchip._
import dsptools.numbers._
import dsptools.numbers.implicits._

// FFT with stream interface converted to AXI connected to MMIO manager
class Craft2DSP(implicit p: Parameters) extends Module {
  val io = new Bundle {
    val axi = new NastiIO().flip
  }
  val nfft = 4 // TODO allow parameters to be set from top level

  // Inputs to FFT, written via AXI
  val inputs = Reg(Vec(nfft, DspComplex(DspReal(0.0), DspReal(0.0))))

  val fft_config = new FFTConfig(n = nfft, p = nfft)
  val fft = Module(new DirectFFT(genIn = DspComplex(DspReal(0.0), DspReal(0.0)), config = fft_config))
  
  fft.io.in.valid := true.B
  fft.io.in.sync := false.B
  for (i <- 0 until nfft) fft.io.in.bits(i) := inputs(i)

  val ar = Queue(io.axi.ar, 1)
  val aw = Queue(io.axi.aw, 1)
  val w = Queue(io.axi.w, 1)

  // Start from 3rd bit since 64-bit words
  // Only need log2Up(nfft)+1 bits, since 2*nfft registers
  val read_index  = ar.bits.addr(4 + log2Up(nfft), 3)
  val write_index = aw.bits.addr(4 + log2Up(nfft), 3)

  io.axi.r.valid := ar.valid
  ar.ready := io.axi.r.ready
  io.axi.r.bits := NastiReadDataChannel(
    id = ar.bits.id,
    data = MuxLookup(read_index, UInt(0),
      (0 until 2*nfft).map (i => {
        if (i % 2 == 0)
          i.U     -> fft.io.out.bits(i / 2).real.toBits
        else
          i.U -> fft.io.out.bits(i / 2).imaginary.toBits
      })))

  io.axi.b.valid := aw.valid && w.valid
  aw.ready := io.axi.b.ready && w.valid
  w.ready := io.axi.b.ready && aw.valid
  io.axi.b.bits := NastiWriteResponseChannel(id = aw.bits.id)

  when (io.axi.b.fire()) {
    for (i <- 0 until nfft * 2) {
      val wire = DspReal(0.0).fromBits(w.bits.data)
      if (i % 2 == 0)
        when (write_index === i.U) { inputs(i / 2).real      := wire }
      else 
        when (write_index === i.U) { inputs(i / 2).imaginary := wire }
    }
  }

  require(io.axi.w.bits.nastiXDataBits == 64)

  assert(!io.axi.ar.valid || (io.axi.ar.bits.len === UInt(0) && io.axi.ar.bits.size === UInt(3)))
  assert(!io.axi.aw.valid || (io.axi.aw.bits.len === UInt(0) && io.axi.aw.bits.size === UInt(3)))
  assert(!io.axi.w.valid || PopCount(io.axi.w.bits.strb) === UInt(8))

}

trait PeripheryCraft2DSP extends LazyModule {
  val pDevices: ResourceManager[AddrMapEntry]

  pDevices.add(AddrMapEntry("craft2", MemSize(4096, MemAttr(AddrMapProt.RW))))
}

case object BuildCraft2DSP extends Field[(ClientUncachedTileLinkIO, Parameters) => Unit]

trait PeripheryCraft2DSPModule extends HasPeripheryParameters {
  implicit val p: Parameters
  val pBus: TileLinkRecursiveInterconnect

  p(BuildCraft2DSP)(pBus.port("craft2"), outerMMIOParams)
}
