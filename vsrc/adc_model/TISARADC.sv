////////////////////////////////////////////////////////////////////
// 
// File:        ti_adc.sv
// Module:      ti_adc
// Project:     TI-ADC modeling
// Description: time interleave SAR-ADC model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    01/29/2017
// Date modefied:   02/25/2017
// -----------------------------------------------------------------
// Change history:  01/29/2017 - First Created
//                  02/21/2017 - Add another parameter 'CLK_INIT'
//                               Move clock out of generate block, as it can generate multi-clock
//                               Delete adc_compl from output
//                               Change parameters
//                  02/25/2017 - Change default values
//                               Add pins from real ADC
//                               Change pin names!!
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
////////////////////////////////////////////////////////////////////

//Things to notice:
//In each, Sub-ADC BIT[0] is the highest bit
//In TI-ADC, ADC[0] is the first ADC

`include "../../verilog-src/verilog_header.vh"
`define ADC_WAYS    8   
`define ADC_BITS    9


module TISARADC (
    //Supply
    inout VDDHADC,
    inout VDDADC,
    inout VSS,
    //input
    input real ADCINP,
    input real ADCINM,
    //""clock will have problem, as this is sinusoid wave!!
    input ADCCLKP,
    input ADCCLKM,
    //output
    //highest bits ADC_BITS-1
    output [`ADC_BITS-1:0] ADCOUT0,
    output [`ADC_BITS-1:0] ADCOUT1,
    output [`ADC_BITS-1:0] ADCOUT2,
    output [`ADC_BITS-1:0] ADCOUT3,
    output [`ADC_BITS-1:0] ADCOUT4,
    output [`ADC_BITS-1:0] ADCOUT5,
    output [`ADC_BITS-1:0] ADCOUT6,
    output [`ADC_BITS-1:0] ADCOUT7,
    //ADC slicers
    input [7:0] OSP0,
    input [7:0] OSP1,
    input [7:0] OSP2,
    input [7:0] OSP3,
    input [7:0] OSP4,
    input [7:0] OSP5,
    input [7:0] OSP6,
    input [7:0] OSP7,
    
    input [7:0] OSM0,
    input [7:0] OSM1,
    input [7:0] OSM2,
    input [7:0] OSM3,
    input [7:0] OSM4,
    input [7:0] OSM5,
    input [7:0] OSM6,
    input [7:0] OSM7,

    //""what is 3 here, we might need a parameter for it
    input [3:0] ASCLKD0,
    input [3:0] ASCLKD1,
    input [3:0] ASCLKD2,
    input [3:0] ASCLKD3,
    input [3:0] ASCLKD4,
    input [3:0] ASCLKD5,
    input [3:0] ASCLKD6,
    input [3:0] ASCLKD7,

    //About Internal Clocks
    input EXTSEL_CLK0,
    input EXTSEL_CLK1,
    input EXTSEL_CLK2,
    input EXTSEL_CLK3,
    input EXTSEL_CLK4,
    input EXTSEL_CLK5,
    input EXTSEL_CLK6,
    input EXTSEL_CLK7,


    input EXT_CLK0,
    input EXT_CLK1,
    input EXT_CLK2,
    input EXT_CLK3,
    input EXT_CLK4,
    input EXT_CLK5,
    input EXT_CLK6,
    input EXT_CLK7,
    
    //ADC REF
    input [7:0] VREF00,
    input [7:0] VREF01,
    input [7:0] VREF02,
    input [7:0] VREF03,
    input [7:0] VREF04,
    input [7:0] VREF05,
    input [7:0] VREF06,
    input [7:0] VREF07,

    input [7:0] VREF10,
    input [7:0] VREF11,
    input [7:0] VREF12,
    input [7:0] VREF13,
    input [7:0] VREF14,
    input [7:0] VREF15,
    input [7:0] VREF16,
    input [7:0] VREF17,

    input [7:0] VREF20,
    input [7:0] VREF21,
    input [7:0] VREF22,
    input [7:0] VREF23,
    input [7:0] VREF24,
    input [7:0] VREF25,
    input [7:0] VREF26,
    input [7:0] VREF27,

    input [7:0] IREF0,
    input [7:0] IREF1,
    input [7:0] IREF2,
    
    //CLK outputs
    output CLKOUT,

    //ClK Calibration
    input [7:0] CLKGCCAL0,
    input [7:0] CLKGCCAL1,
    input [7:0] CLKGCCAL2,
    input [7:0] CLKGCCAL3,
    input [7:0] CLKGCCAL4,
    input [7:0] CLKGCCAL5,
    input [7:0] CLKGCCAL6,
    input [7:0] CLKGCCAL7,
    input CLKRST,

    //Source Follower
    input real ADCBIAS
); 

wire [0:`ADC_BITS-1] adc_data [0:`ADC_WAYS-1];
wire [0:`ADC_WAYS-1] subadc_clk;


reg [0:`ADC_BITS-1] adc_data_mid [0:`ADC_WAYS-1];
reg [0:`ADC_BITS-1] adc_data_pre [0:`ADC_WAYS-1];
reg [0:`ADC_BITS-1] adc_data_fin [0:`ADC_WAYS-1];

//assign adc data to output, swap the high and low bits
assign ADCOUT0[`ADC_BITS-1:0] = adc_data_fin[0][0:`ADC_BITS-1];
assign ADCOUT1[`ADC_BITS-1:0] = adc_data_fin[1][0:`ADC_BITS-1];
assign ADCOUT2[`ADC_BITS-1:0] = adc_data_fin[2][0:`ADC_BITS-1];
assign ADCOUT3[`ADC_BITS-1:0] = adc_data_fin[3][0:`ADC_BITS-1];
assign ADCOUT4[`ADC_BITS-1:0] = adc_data_fin[4][0:`ADC_BITS-1];
assign ADCOUT5[`ADC_BITS-1:0] = adc_data_fin[5][0:`ADC_BITS-1];
assign ADCOUT6[`ADC_BITS-1:0] = adc_data_fin[6][0:`ADC_BITS-1];
assign ADCOUT7[`ADC_BITS-1:0] = adc_data_fin[7][0:`ADC_BITS-1];

//sub_adc instantiation
ti_adc ti_adc(
    //input
    .rst            (CLKRST),
    .adc_clk        (ADCCLKP),
    .adc_vip        (ADCINP),
    .adc_vin        (ADCINM),
    //output
    .adc_data       (adc_data),
    //output [0:ADC_WAYS-1] adc_compl,
    .subadc_clk     (subadc_clk),
    .adc_coreclk    (CLKOUT)

); 

//retimer 1st stage
always @(subadc_clk[5] or adc_data)
    if (subadc_clk[5] == 1'd1) adc_data_mid[0:3] = adc_data[0:3];
always @(subadc_clk[1] or adc_data)
    if (subadc_clk[5] == 1'd1) adc_data_mid[4:7] = adc_data[4:7];

//retimer 2nd stage
always @(subadc_clk[3] or adc_data_mid) 
    if (subadc_clk[3] == 1'd1) adc_data_pre = adc_data_mid;

//retimer 3rd stage
always @(subadc_clk[7] or adc_data_pre)
    if (subadc_clk[7] == 1'd1) adc_data_fin = adc_data_pre;

endmodule
