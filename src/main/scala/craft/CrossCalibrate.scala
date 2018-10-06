package craft

import chisel3._
import chisel3.experimental._
import chisel3.internal.firrtl.KnownBinaryPoint
import chisel3.util._
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dspjunctions._
import dspblocks._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
// import ipxact._
import scala.collection.mutable.Map


// TODO : different gen for calibration coefficients

case class CrossCalibrateConfig[T <: Data](
  gen: DspComplex[T],
  lanes: Int = 8,
  channels: Int = 16,
  pipelineDepth: Int = 0,
) {
  require(lanes > 0, "CrossCalibrate block must have more than 0 input lanes")
  require(pipelineDepth >= 0, "CrossCalibrate block must have positive pipelining")
  val bp = channels/(lanes/2)

  val genIn = gen
  val genOut = gen
}

class CrossCalibrateBlock[T <: Data:Real](val config: CrossCalibrateConfig[T])(implicit p: Parameters) extends TLDspBlock /*with TLHasCSR*/ {
  override val streamNode = AXI4StreamIdentityNode()
  override val mem = None

  lazy val module = new CrossCalibrateModule(this)

  // addStatus("Data_Set_End_Status")
  // addControl("Data_Set_End_Clear", 0.U)

  // addControl("Mode", 0.U)
  // addControl("Addr", 0.U)
  // addControl("Write_En", 0.U)
  // (0 until config.lanes/2).map(i => {
  //   //addControl(s"C0_Lane${i}_Write_Data")
  //   addControl(s"C1_Lane${i}_Write_Data")
  //   addControl(s"C2_Lane${i}_Write_Data")
  //   //addControl(s"C3_Lane${i}_Write_Data")
  // })
}

class CrossCalibrateModule[T <: Data:Real](outer: CrossCalibrateBlock[T])(implicit p: Parameters) extends LazyModuleImp(outer) {
  val config = outer.config
  val module = Module(new CrossCalibrate[T](config))
  // module.io.in <> unpackInput(lanesIn, genIn())
  // unpackOutput(lanesOut, genOut()) <> module.io.out
  // status("Data_Set_End_Status") := module.io.data_set_end_status
  // module.io.data_set_end_clear := control("Data_Set_End_Clear")

  // module.io.mode := control("Mode")
  // module.io.addr := control("Addr")
  // module.io.wen := control("Write_En")

  // TODO will need to figure out how "fromBits" maps a 64-bit UInt into an n-bit DspComplex when writing c code

  //val c0_wire = Wire(Vec(config.lanes/2, genIn()))
  //val c0 = c0_wire.zipWithIndex.map{case (x, i) => x.fromBits(control(s"C0_Lane${i}_Write_Data"))}
  //module.io.c0_wdata := c0

  // val c1_wire = Wire(Vec(config.lanes/2, config.genIn))
  // val c1 = c1_wire.zipWithIndex.map{case (x, i) => x.fromBits(control(s"C1_Lane${i}_Write_Data"))}
  // module.io.c1_wdata := c1

  // val c2_wire = Wire(Vec(config.lanes/2, config.genIn))
  // val c2 = c2_wire.zipWithIndex.map{case (x, i) => x.fromBits(control(s"C2_Lane${i}_Write_Data"))}
  // module.io.c2_wdata := c2

  //val c3_wire = Wire(Vec(config.lanes/2, genIn()))
  //val c3 = c3_wire.zipWithIndex.map{case (x, i) => x.fromBits(control(s"C3_Lane${i}_Write_Data"))}
  //module.io.c3_wdata := c3
}

class CrossCalibrate[T <: Data:Real](val config: CrossCalibrateConfig[T])(implicit val p: Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Input(ValidWithSync(Vec(config.lanes, config.genIn)))
    val out = Output(ValidWithSync(Vec(config.lanes, config.genOut)))
    val data_set_end_status = Output(Bool())
    val data_set_end_clear = Input(Bool())

    // note: assumes genIn() will be 64 bits total or fewer
    val mode = Input(Bool())
    val addr = Input(UInt((log2Up(config.bp)).W))
    val wen = Input(Bool())
    //val c0_wdata = Vec(config.lanes/2, Input(genIn()))
    val c1_wdata = Vec(config.lanes/2, Input(config.genIn))
    val c2_wdata = Vec(config.lanes/2, Input(config.genIn))
    //val c3_wdata = Vec(config.lanes/2, Input(genIn()))
  })

  // delay the data set signals
  val latency = config.pipelineDepth
  io.out.sync := ShiftRegisterWithReset(io.in.sync, latency, 0.U)
  io.out.valid := ShiftRegisterWithReset(io.in.valid, latency, 0.U)

  // feed in zeros when invalid
  val in = Wire(Vec(config.lanes, config.genIn))
  when (io.in.valid) {
    in := io.in.bits
  } .otherwise {
    in := Vec.fill(config.lanes)(DspComplex(Real[T].zero, Real[T].zero))
  }

  // data set end flag
  val valid_delay = Reg(next=io.out.valid)
  val dses = Reg(init=false.B)
  when (io.data_set_end_clear) {
    dses := false.B
  } .elsewhen (valid_delay & ~io.out.valid) {
    dses := true.B
  }
  io.data_set_end_status := dses

  // memories
  val genCoeff: DspComplex[T] = {
    val growth = -2
    config.genIn.real match {
      case f: FixedPoint =>
        f.binaryPoint match {
          case KnownBinaryPoint(binaryPoint) =>
            val totalBits = f.getWidth + growth
            DspComplex(FixedPoint(totalBits.W, binaryPoint.BP), FixedPoint(totalBits.W, binaryPoint.BP)).asInstanceOf[DspComplex[T]]
          case _ => throw new DspException("Error: unknown binary point when calculating Coeff bitwdiths")
        }
      case s: SInt =>
        val totalBits = s.getWidth + growth
        DspComplex(SInt(totalBits.W), SInt(totalBits.W)).asInstanceOf[DspComplex[T]]
      case _ => throw new DspException("Error: unknown type when calculating Coeff bitwidths")
    }
  }
  val read_addr = CounterWithReset(true.B, config.bp, io.in.sync, ~valid_delay & io.in.valid)._1
  //val cross_calibrate0 = SyncReadMem(config.bp, Vec(config.lanes/2, genCoeff))
  val cross_calibrate1 = SyncReadMem(config.bp, Vec(config.lanes/2, genCoeff))
  val cross_calibrate2 = SyncReadMem(config.bp, Vec(config.lanes/2, genCoeff))
  //val cross_calibrate3 = SyncReadMem(config.bp, Vec(config.lanes/2, genCoeff))

  // write to memories
  val c0 = Wire(Vec(config.lanes/2, genCoeff))
  val c1 = Wire(Vec(config.lanes/2, genCoeff))
  val c2 = Wire(Vec(config.lanes/2, genCoeff))
  val c3 = Wire(Vec(config.lanes/2, genCoeff))
  // these are always 1+0j
  c0 := Vec.fill(config.lanes/2)(DspComplex(Real[T].one, Real[T].zero))
  c3 := Vec.fill(config.lanes/2)(DspComplex(Real[T].one, Real[T].zero))
  when (io.mode) {
    when (io.wen) {
      //cross_calibrate0.write(io.addr, io.c0_wdata)
      cross_calibrate1.write(io.addr, io.c1_wdata)
      cross_calibrate2.write(io.addr, io.c2_wdata)
      //cross_calibrate3.write(io.addr, io.c3_wdata)
    }
    // "passthrough" during write, which does no calibration, just acts like no modifier
    c1 := Vec.fill(config.lanes/2)(DspComplex(Real[T].zero, Real[T].zero))
    c2 := Vec.fill(config.lanes/2)(DspComplex(Real[T].zero, Real[T].zero))
  } .otherwise {
    c1 := cross_calibrate1.read(read_addr)
    c2 := cross_calibrate2.read(read_addr)
  }

  // take the cross calibrate of the input
  val in_grouped = in.grouped(config.lanes/2).toList
  in_grouped(0).zip(in_grouped(1)).zip(c0).zip(c1).zip(io.out.bits.take(config.lanes/2)).foreach { case ((((ix0, ix1), cx0), cx1), ox) =>
    // ox := ShiftRegister(ix0 * cx0 + ix1 * cx1, config.pipelineDepth)
  }
  in_grouped(0).zip(in_grouped(1)).zip(c2).zip(c3).zip(io.out.bits.drop(config.lanes/2)).foreach { case ((((ix0, ix1), cx0), cx1), ox) =>
    // ox := ShiftRegister(ix0 * cx0 + ix1 * cx1, config.pipelineDepth)
  }
}
