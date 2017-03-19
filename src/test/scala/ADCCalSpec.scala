package adc.sar


import chisel3._
import firrtl._
// Use DspTester, specify options for testing (i.e. expect tolerances on fixed point, etc.)
import dsptools.{DspTester, DspTesterOptionsManager, DspTesterOptions}
// Allows you to modify default Chisel tester behavior (note that DspTester is a special version of Chisel tester)
import iotesters.TesterOptions
// Scala unit testing style
import org.scalatest.{FlatSpec, Matchers}

class ADCCalSpec extends FlatSpec with Matchers {
  val testoptions = new DspTesterOptionsManager {
    dspTesterOptions = DspTesterOptions( 
      genVerilogTb = true
    )
//    testerOptions = TesterOptions(
//      backendName = "verilator")
  }
  behavior of "adccal dsp module"

  it should "calibrate" in {
    dsptools.Driver.execute(() => new ADCCal(9, 9, 8), testoptions) { c =>
      new ADCCalTester(c)
    } should be (true)
  }

  it should "generate verilog" in {
    val optionsManager = new ExecutionOptionsManager("barstools") with HasChiselExecutionOptions with HasFirrtlOptions {
      firrtlOptions = firrtlOptions.copy(
        compilerName = "verilog"
      )
      commonOptions = commonOptions.copy(targetDirName = "test_run_dir/Verilog")
      //commonOptions = commonOptions.copy(globalLogLevel = logger.LogLevel.Info)
    }
    val success = chisel3.Driver.execute(optionsManager, () => new ADCCal(9, 9, 8)) match {
      case ChiselExecutionSuccess(_, chirrtl, Some(FirrtlExecutionSuccess(_, verilog))) => 
        true
      case _ => false
    } 
    success should be (true)
  }
}

class ADCCalTester(c: ADCCal) extends DspTester(c) {
  val idle = 0
  val normal = 1
  val lutset = 2
  val adctest = 3
  val addrrange = math.pow(2, 9).toInt //address range
  //val addrrange = 30
  val datarange = math.pow(2, 8).toInt //calibration data range (2**cal bit width)
  val rawdatarange = math.pow(2, 9).toInt //raw data (before cal) range (2**input bit width)
  //val rawdatarange = 30

  //initialize test variables
  val r = scala.util.Random
  var codearr0 = new Array[Int](addrrange)
  var codearr1 = new Array[Int](addrrange)
  var codearr2 = new Array[Int](addrrange)
  var codearr3 = new Array[Int](addrrange)
  var codearr4 = new Array[Int](addrrange)
  var codearr5 = new Array[Int](addrrange)
  var codearr6 = new Array[Int](addrrange)
  var codearr7 = new Array[Int](addrrange)
  for (i <- 0 until addrrange ) {
    codearr0(i)=r.nextInt(datarange-1)
    codearr1(i)=r.nextInt(datarange-1)
    codearr2(i)=r.nextInt(datarange-1)
    codearr3(i)=r.nextInt(datarange-1)
    codearr4(i)=r.nextInt(datarange-1)
    codearr5(i)=r.nextInt(datarange-1)
    codearr6(i)=r.nextInt(datarange-1)
    codearr7(i)=r.nextInt(datarange-1)
  }
  var rawdataarr0 = new Array[Int](rawdatarange)
  var rawdataarr1 = new Array[Int](rawdatarange)
  var rawdataarr2 = new Array[Int](rawdatarange)
  var rawdataarr3 = new Array[Int](rawdatarange)
  var rawdataarr4 = new Array[Int](rawdatarange)
  var rawdataarr5 = new Array[Int](rawdatarange)
  var rawdataarr6 = new Array[Int](rawdatarange)
  var rawdataarr7 = new Array[Int](rawdatarange)
  for (i <- 0 until rawdatarange ) {
    rawdataarr0(i)=r.nextInt(rawdatarange-1)
    rawdataarr1(i)=r.nextInt(rawdatarange-1)
    rawdataarr2(i)=r.nextInt(rawdatarange-1)
    rawdataarr3(i)=r.nextInt(rawdatarange-1)
    rawdataarr4(i)=r.nextInt(rawdatarange-1)
    rawdataarr5(i)=r.nextInt(rawdatarange-1)
    rawdataarr6(i)=r.nextInt(rawdatarange-1)
    rawdataarr7(i)=r.nextInt(rawdatarange-1)
    //rawdataarr0(i)=i
    //rawdataarr1(i)=i
    //rawdataarr2(i)=i
    //rawdataarr3(i)=i
    //rawdataarr4(i)=i
    //rawdataarr5(i)=i
    //rawdataarr6(i)=i
    //rawdataarr7(i)=i
  }
  
  //lut set mode
  poke(c.io.wen, false) //wen reset
  step(1)
  poke(c.io.mode, lutset)
  step(1)
  poke(c.io.wen, true)
  step(1)
  for (i <- 0 until addrrange ) {
    poke(c.io.addr, i)
    poke(c.io.calcoeff(0), codearr0(i))
    poke(c.io.calcoeff(1), codearr1(i))
    poke(c.io.calcoeff(2), codearr2(i))
    poke(c.io.calcoeff(3), codearr3(i))
    poke(c.io.calcoeff(4), codearr4(i))
    poke(c.io.calcoeff(5), codearr5(i))
    poke(c.io.calcoeff(6), codearr6(i))
    poke(c.io.calcoeff(7), codearr7(i))
    step(1)
  }
  //calibration mode
  poke(c.io.wen, false) //wen reset
  step(1)
  poke(c.io.mode, normal)
  for (i <- 0 until addrrange ) {
    poke(c.io.adcdata(0), i)
    poke(c.io.adcdata(1), i)
    poke(c.io.adcdata(2), i)
    poke(c.io.adcdata(3), i)
    poke(c.io.adcdata(4), i)
    poke(c.io.adcdata(5), i)
    poke(c.io.adcdata(6), i)
    poke(c.io.adcdata(7), i)
    step(1)
    expect(c.io.calout(0), codearr0(i))
    expect(c.io.calout(1), codearr1(i))
    expect(c.io.calout(2), codearr2(i))
    expect(c.io.calout(3), codearr3(i))
    expect(c.io.calout(4), codearr4(i))
    expect(c.io.calout(5), codearr5(i))
    expect(c.io.calout(6), codearr6(i))
    expect(c.io.calout(7), codearr7(i))
  }
  //adc test mode
  poke(c.io.wen, false) //wen reset
  step(1)
  poke(c.io.mode, adctest)
  step(1)
  poke(c.io.wen, true)
  step(1)
  for (i <- 0 until rawdatarange ) {
    poke(c.io.adcdata(0), rawdataarr0(i))
    poke(c.io.adcdata(1), rawdataarr1(i))
    poke(c.io.adcdata(2), rawdataarr2(i))
    poke(c.io.adcdata(3), rawdataarr3(i))
    poke(c.io.adcdata(4), rawdataarr4(i))
    poke(c.io.adcdata(5), rawdataarr5(i))
    poke(c.io.adcdata(6), rawdataarr6(i))
    poke(c.io.adcdata(7), rawdataarr7(i))
    step(1)
  }
  //adc readout mode
  poke(c.io.wen, false) //wen reset
  step(1)
  //this requires synchronized cycles because internal address counter is keep running
  poke(c.io.mode, idle)
  for (i <- 7 until addrrange ) {
    poke(c.io.addr, i)
    step(1)
    //peek(c.io.calout(0))
    //peek(c.io.calout(1))
    //peek(c.io.calout(2))
    //peek(c.io.calout(3))
    //peek(c.io.calout(4))
    //peek(c.io.calout(5))
    //peek(c.io.calout(6))
    //peek(c.io.calout(7))
    expect(c.io.calout(0), rawdataarr0(i-7))
    expect(c.io.calout(1), rawdataarr1(i-7))
    expect(c.io.calout(2), rawdataarr2(i-7))
    expect(c.io.calout(3), rawdataarr3(i-7))
    expect(c.io.calout(4), rawdataarr4(i-7))
    expect(c.io.calout(5), rawdataarr5(i-7))
    expect(c.io.calout(6), rawdataarr6(i-7))
    expect(c.io.calout(7), rawdataarr7(i-7))
  }

  //peek(c.io.calout(0))
  //expect(c.io.callout(0),3)
  //poke(c.io.addr(1))
}
