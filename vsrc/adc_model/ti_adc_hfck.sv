////////////////////////////////////////////////////////////////////
// 
// File:        ti_adc_hfck.sv
// Module:      ti_adc_hfck
// Project:     TI-ADC modeling
// Description: time interleave SAR-ADC model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    03/04/2017
// Date modefied:   03/04/2017
// -----------------------------------------------------------------
// Change history:  03/04/2017 - First Created
//                  
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      ADC_SR          (0:inf)     8e9         samp/s  real    ADC sample rate
//      ADC_BITS        (0:inf)     8                   integer ADC bits
//      ADC_WAYS        (0:inf)     8                   integer adc ways
//      ASYN_DEL        (0:inf)     10.0        ps      real    delay between up and down pulses,
//                                                              also the time for DAC settling
//      PAR_CAP         [0:inf)     0           F       real    parasitic cap at comparator input
//      DAC_CAP         (0:inf)     1.0e-15     F       real    CAP DAC unit capacitance
//      VCM_VOL         (-inf:inf)  0.5         V       real    common mode voltage
//      REFP_VOL        (-inf:inf)  0.5         V       real    reference-p voltage
//      REFP_VOL        (-inf:inf)  0.5         V       real    reference-n voltage
//      SENAMP_DEL      (0:inf)     1.0         ps      real    sense amplifier delay
//      CLK_INIT        [0:ADCWAYS-1]   0               integer reset clk, 
//                                                              to designate which clk reset to 1
//      CLK_CORE        [0:ADCWAYS-1]   3               integer core clock choosing
//      CLK_DFFD        [0:inf)     1                   real    delay of DFF in clock distribution
// -----------------------------------------------------------------
// Notes:
//      Only works for even number of ways for now
////////////////////////////////////////////////////////////////////

//Things to notice:
//In each, Sub-ADC BIT[0] is the highest bit
//In TI-ADC, ADC[0] is the first ADC

`include "verilog_header.vh"

module ti_adc_hfck #(
    parameter ADC_SR        = 9.6e9,   //sample/s, assuming each sub_adc is 1Gsample/s    
    parameter ADC_WAYS      = 8,   
    parameter ADC_BITS      = 9,
    //from asyn_clk
    parameter ASYN_DEL      = 10.0,     //real, ps
    //from cap_dac
    parameter PAR_CAP       = 0.0,      //real, F
    parameter DAC_CAP       = 0.3e-15,  //real, F
    //from refrence
    parameter VCM_VOL       = 0.1,      //real, V
    parameter REFP_VOL      = 0.2,      //real, V
    parameter REFN_VOL      = 0.0,      //real, V
    //from sense_amp
    parameter SENAMP_DEL    = 0.02,     //real, ps
    //from ti_clk
    parameter CLK_INIT      = 0,        //integer
    parameter CLK_CORE      = 3,        //integer
    parameter CLK_DFFD      = 1         //real, ps
)(
    //input
    input rst,
    input adc_clkp,
    input adc_clkn,
    input real adc_vip,
    input real adc_vin,
    //output
    output [0:ADC_BITS-1] adc_data [0:ADC_WAYS-1],
    //output [0:ADC_WAYS-1] adc_compl,
    output [0:ADC_WAYS-1] subadc_clk,
    output adc_coreclk
); 

//parameter DELAY = 1/ADC_SR*`S_TO_PS;

wire [0:ADC_WAYS-1] adc_compl;

//ti_clock instantiation
ti_clock_half #(
        .ADC_WAYS       (ADC_WAYS),
        .CLK_INIT       (CLK_INIT),
        .CLK_CORE       (CLK_CORE),
        .CLK_DFFD       (CLK_DFFD)
)ti_clock_half(
        .rst            (rst),
        .clkp           (adc_clkp),
        .clkn           (adc_clkn),
        .ti_clk         (subadc_clk),
        .core_clk       (adc_coreclk)
    );


genvar i;

//sub_adc instantiation
generate for(i=0; i<ADC_WAYS; i=i+1) begin
  
    sub_adc #(
        .ADC_BITS       (ADC_BITS),
        //from asyn_clk
        .ASYN_DEL       (ASYN_DEL),         //ps
        //from cap_dac
        .PAR_CAP        (PAR_CAP),          //F
        .DAC_CAP        (DAC_CAP),          //F
        //from refrence
        .VCM_VOL        (VCM_VOL),          //V
        .REFP_VOL       (REFP_VOL),         //V
        .REFN_VOL       (REFN_VOL),         //V
        //from sense_amp
        .SENAMP_DEL     (SENAMP_DEL)        //e-12s
    )sub_adc(
        //input
        .rst            (rst),
        .subadc_vip     (adc_vip),
        .subadc_vin     (adc_vin),
        .subadc_clk     (subadc_clk[i]),
        //output
        .subadc_data    (adc_data[i]),
        .subadc_compl   (adc_compl[i])
    ); 
end
endgenerate

endmodule
