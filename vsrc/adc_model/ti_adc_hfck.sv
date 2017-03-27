////////////////////////////////////////////////////////////////////
// 
// File:        ti_adc_hfck.sv
// Module:      ti_adc_hfck
// Project:     TI-ADC modeling
// Description: time interleave SAR-ADC model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    03/04/2017
// Date modefied:   03/24/2017
// -----------------------------------------------------------------
// Change history:  03/04/2017 - First Created
//                  03/19/2017 - Add cap array for calibaration
//                               Change DAC_CAP to UNIT CAP
//                  03/19/2017 - Add cap array for calibaration
//                               Change DAC_CAP to UNIT CAP
//                  03/21/2017 - Add reference voltage DACs and control signals
//                               Add offset DACs and control signals
//                  03/24/2017 - Swap LSB and MSB
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      ADC_SR          (0:inf)     8e9         samp/s  real    ADC sample rate
//      ADC_BITS        (0:inf)     8                   integer ADC bits
//      ADC_WAYS        (0:inf)     8                   integer adc ways
//      ASYN_DEL        (0:inf)     10.0        ps      real    delay between up and down pulses,
//                                                              also the time for DAC settling
//      PAR_CAP         [0:inf)     0           F       real    parasitic cap at comparator input
//      UNIT_CAP         (0:inf)     1.0e-15     F       real    CAP DAC unit capacitance
//      DAC_CAPS        [0:inf)     next row            array   If the value is 0 will use inside calculation
//                                  '{1, 2, 4, 8, 16, 32, 64, 128}
//      VRDAC_BITS      (0:inf)     8                   integer reference voltage DAC bits
//      VRDAC_HIGH      [0:inf)     0.9         V       real    reference DAC positive reference
//      VRDAC_LOW       [0:inf)     0           V       real    reference DAC negtive reference
//      SENAMP_DEL      (0:inf)     1.0         ps      real    sense amplifier delay
//      OSDAC_BITS      (0:inf)     8                   integer offset voltage DAC bits
//      OSDAC_HIGH      [0:inf)     0.9         V       real    offset DAC positive reference
//      OSDAC_LOW       [0:inf)     0           V       real    offset DAC negtive reference
//      OFFSET_GAIN     [0:1]       0.25                real    offset gain for sense amplifier
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
    parameter UNIT_CAP      = 1.0e-15,   //F
    parameter DAC_CAPS [1:ADC_BITS-1]   = '{1, 2, 4, 8, 16, 32, 64, 128},
    //from refrence
    parameter VRDAC_BITS    = 8,        
    parameter VRDAC_HIGH    = 0.9,      //real, V
    parameter VRDAC_LOW     = 0.0,      //real, V
    //from sense_amp
    parameter SENAMP_DEL    = 0.02,     //real, ps
    parameter OSDAC_BITS    = 8,
    parameter OSDAC_HIGH    = 0.9,      //real, V
    parameter OSDAC_LOW     = 0.0,      //real, V
    parameter OFFSET_GAIN   = 0.25,     //real
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
    input [VRDAC_BITS-1:0] data_vcm [0:ADC_WAYS-1],
    input [VRDAC_BITS-1:0] data_vrefp [0:ADC_WAYS-1], 
    input [VRDAC_BITS-1:0] data_vrefn [0:ADC_WAYS-1],
    input [OSDAC_BITS-1:0] data_vosp [0:ADC_WAYS-1],
    input [OSDAC_BITS-1:0] data_vosn [0:ADC_WAYS-1],
    //output
    output [ADC_BITS-1:0] adc_data [0:ADC_WAYS-1],
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
        .UNIT_CAP       (UNIT_CAP),         //F
        .DAC_CAPS       (DAC_CAPS),
        //from refrence
        .VRDAC_BITS     (VRDAC_BITS),
        .VRDAC_HIGH     (VRDAC_HIGH),       //V
        .VRDAC_LOW      (VRDAC_LOW),        //V
        //from sense_amp
        .SENAMP_DEL     (SENAMP_DEL),       //e-12s
        .OSDAC_BITS     (OSDAC_BITS),
        .OSDAC_HIGH     (OSDAC_HIGH),       //V
        .OSDAC_LOW      (OSDAC_LOW)         //V
    )sub_adc(
        //input
        .rst            (rst),
        .subadc_vip     (adc_vip),
        .subadc_vin     (adc_vin),
        .subadc_clk     (subadc_clk[i]),
        .data_vcm       (data_vcm[i]),
        .data_vrefp     (data_vrefp[i]), 
        .data_vrefn     (data_vrefn[i]),
        .data_vosp      (data_vosp[i]),
        .data_vosn      (data_vosn[i]),
        //output
        .subadc_data    (adc_data[i]),
        .subadc_compl   (adc_compl[i])
    ); 
end
endgenerate

endmodule
