// See LICENSE for license details.

package craft

import chisel3._
import chisel3.experimental.FixedPoint
import dspblocks._
import dsptools.numbers._
import fft._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._

class FFTChainConfig extends Config((site, here, up) => {
    case ChainKey => Seq(
    { implicit p: Parameters => {
      val bitmanip = LazyModule(new BitManipulationBlock(BitManipulationConfig(UInt(8.W), UInt(8.W), 8)))
      bitmanip 
    }},
    { implicit p: Parameters => {
      val fft = LazyModule(new FFTBlock(FFTConfig(
        genIn = DspComplex(FixedPoint(9.W, 8.BP), FixedPoint(9.W, 8.BP)),
        genOut = DspComplex(FixedPoint(16.W, 12.BP), FixedPoint(9.W, 8.BP)),
    ))) 
      fft
    }},
    { implicit p: Parameters => {
      val end = LazyModule(new TLDspBlock {
        val streamNode = AXI4StreamSlaveNode(AXI4StreamSlaveParameters())
        val mem = None
        override lazy val module = new LazyModuleImp(this) {
          val (in, _) = streamNode.in.head
          in.ready := true.B
        }
      })
    end
    }},
    )
})

class AcmesBaseConfig extends Config(new DefaultConfig ++ ChainBuilder.acmes())

object ChainBuilder {
  type T = FixedPoint

  ///////////////////////////////////////////////////////////////
  ////////////            Here be acmes configuration parameters
  ///////////////////////////////////////////////////////////////

  def acmes(id: String = "acmes", channels: Int = 64): Config = {

    val lanes = 64
    val numTaps = 4
    val quadrature = true

    // Here be the bit manipulation 1 block, this is just to add a SAM to the ADC output, it does nothing to the bits
    def bm1Input():T = FixedPoint(9.W, 0.BP)
    def bm1Output():T = FixedPoint(9.W, 0.BP)
    def bm1Config() = BitManipulationConfig(genIn = bm1Input(), genOut = bm1Output(), lanes = lanes)
    // def bm1Connect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)

    // Here be the filterbank
    // notes on windows: the sincHamming doesn't go to zero on the edges, so Hanning is preferred
    val pd = if (quadrature) channels/(lanes/2)*(numTaps-1) else channels/lanes*(numTaps-1)
    // def pfbConfig() = PFBConfig(windowFunc = sincHanning.apply, processingDelay = pd, numTaps = numTaps, outputWindowSize = channels, lanes = lanes, multiplyPipelineDepth = 1, outputPipelineDepth = 1, genTap = Some(pfbTap), quadrature = quadrature)
    def pfbInput():T = FixedPoint(9.W, 8.BP)
    // [stevo]: make sure pfbTap and pfbConvert use the same width and binary point
    def pfbTap:T = FixedPoint(9.W, 8.BP)
    def pfbConvert(x: Double):T = FixedPoint.fromDouble(x, 9.W, 8.BP)
    def pfbOutput():T = FixedPoint(12.W, 7.BP) // loss of 1 LSB precision
    // def pfbConnect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)

    // Here be the Fourier transform
    def fftConfig() = FFTConfig(genIn = DspComplex(fftInput()), genOut = DspComplex(fftOutput()), n = channels, lanes = lanes, pipelineDepth = 13, quadrature = quadrature)
    def fftInput():T = FixedPoint(12.W, 7.BP) // gets complexed automatically
    def fftOutput():T = FixedPoint(18.W, 6.BP) // gets complexed automatically // loss of 1 LSB precision, plus only 1 bit growth every other stage
    // def fftConnect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)

    // Here be the cross calibration block
    def ccInput():T = FixedPoint(18.W, 6.BP) // gets complexed automatically
    // note: as a hack, the calibration coefficient bitwidth is set in the CrossCalibrate file as 2 total bits less than the input bitwidth (same fractional width)
    def ccOutput():T = FixedPoint(18.W, 6.BP) // gets complexed automatically
    def ccConfig() = CrossCalibrateConfig(gen = DspComplex(ccInput()), channels = channels, lanes = lanes, pipelineDepth = 1)
    // def ccConnect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)

    // Here be the power block
    def powerInput():T = FixedPoint(18.W, 6.BP) // gets complexed automatically
    def powerOutput():T = FixedPoint(34.W, 10.BP) // loss of 3 LSB precision?
    def powerConfig() = PowerConfig(genIn = DspComplex(powerInput()), genOut = powerOutput(), lanes = lanes, pipelineDepth = 1, quadrature = quadrature)
    // def powerConnect() = BlockConnectionParameters(connectPG = true, connectLA = true, addSAM = false)

    // Here be the accumulator block
    def accumInput():T = FixedPoint(34.W, 10.BP)
    def accumOutput():T = FixedPoint(64.W, 10.BP)
    def accumConfig() = AccumulatorConfig(
      genIn = accumInput(), genOut = accumOutput(),
      lanes = lanes, outputWindowSize = channels, maxSpectra = 2048, quadrature = quadrature)
    // def accumConnect() = BlockConnectionParameters(connectPG = true, connectLA = false, addSAM = true)
    new Config((site, here, up) => {
      case ChainKey => 
        Seq(
          { implicit p: Parameters => {
            val bitManip = LazyModule(new BitManipulationBlock(bm1Config()))
            bitManip
          }},
          // { implicit p: Parameters => LazyModule(new PFBBlock(pfbConfig())) },
          { implicit p: Parameters => {
            val fft = LazyModule(new FFTBlock(fftConfig()))
            fft
          }},
          { implicit p: Parameters => {
            val cc = LazyModule(new CrossCalibrateBlock(ccConfig()))
            cc
          }},
          { implicit p: Parameters => {
            val power = LazyModule(new PowerBlock(powerConfig()))
            power
          }},
          { implicit p: Parameters => {
            val accum = LazyModule(new AccumulatorBlock(accumConfig()))
            accum
          }},
          { implicit p: Parameters => {
            val end = LazyModule(new TLDspBlock {
              val streamNode = AXI4StreamSlaveNode(AXI4StreamSlaveParameters())
              val mem = None
              override lazy val module = new LazyModuleImp(this) {
                val (in, _) = streamNode.in.head
                in.ready := true.B
              }
            })
          end
          }},
        )
    })
  }
}

// class WithMiniSerialAdapter extends Config(
//   (pname, site, here) => pname match {
//     case SerialInterfaceWidth => 32
//     case _ => throw new CDEMatchError
//   }
// )
// 
// //class WithFPGAOptions(fpga: Boolean) extends Config(
// //  (pname, site, here) => pname match {
// //    case FPGA => fpga
// //    case _ => throw new CDEMatchError
// //  }
// //)
// 
// class AcmesBaseConfig extends Config(
//   new WithDma ++
//   new WithL2Capacity(512) ++
//   new WithL2Cache ++
//   new WithExtMemSize(1L * 1024L * 1024L) ++
//   new WithNL2AcquireXacts(4) ++ 
//   new WithNMemoryChannels(8) ++
//   new WithSRAM(4) ++
//   //new WithSRAM(1) ++
//   new WithMiniSerialAdapter ++
//   new rocketchip.BaseConfig)
// 
// class AcmesTinyBaseConfig extends Config(
//   new WithDma ++
//   new WithL2Capacity(8) ++
//   new WithL2Cache ++
//   new WithExtMemSize(256L * 1024L) ++
//   new WithNL2AcquireXacts(4) ++ 
//   new WithNMemoryChannels(8) ++
//   new WithSRAM(4) ++
//   new WithMiniSerialAdapter ++
//   new rocketchip.BaseConfig)
// 
// class AcmesConfig extends Config(ChainBuilder.acmes(channels=8192) ++ new AcmesBaseConfig)
// class AcmesTinyConfig extends Config(ChainBuilder.acmes(channels=64) ++ new AcmesTinyBaseConfig)
// class AcmesFPGAConfig extends Config(ChainBuilder.acmes(channels=256) ++ new AcmesTinyBaseConfig)
