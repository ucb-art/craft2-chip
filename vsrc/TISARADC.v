////////////////////////////////////////////////////////////////////
// 
// File:        ti_adc.sv
// Module:      ti_adc
// Project:     TI-ADC modeling
// Description: time interleave SAR-ADC model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    01/29/2017
// Date modefied:   03/23/2017
// -----------------------------------------------------------------
// Change history:  01/29/2017 - First Created
//                  02/21/2017 - Add another parameter 'CLK_INIT'
//                               Move clock out of generate block, as it can generate multi-clock
//                               Delete adc_compl from output
//                               Change parameters
//                  02/25/2017 - Change default values
//                               Add pins from real ADC
//                               Change pin names!!
//                  03/21/2017 - Remove VDD/VSS
//                               Change pin name from uppercase to lowercase
//                               Connect vref and offset control to inside block
//                  03/23/2017 - Add input clock/rst part as real circuit
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
////////////////////////////////////////////////////////////////////

//Things to notice:
//In each, Sub-ADC BIT[`ADC_BITS-1] is the highest bit
//In TI-ADC, ADC[0] is the first ADC

//`timescale 1ps/1fs
//`default_nettype none


`define PI 3.1415926535897932
`define NS_TO_FS 1e6
`define S_TO_FS 1e15
`define S_TO_PS 1e12
`define ADC_WAYS    8   
`define ADC_BITS    9


module TISARADC (
    input ADCINP,
    input ADCINM,
    //""clock will have problem, as this is sinusoid wave!!
    input ADCCLKP,
    input ADCCLKM,
    //output
    //highest bits ADC_BITS-1
    output [`ADC_BITS-1:0] adcout0,
    output [`ADC_BITS-1:0] adcout1,
    output [`ADC_BITS-1:0] adcout2,
    output [`ADC_BITS-1:0] adcout3,
    output [`ADC_BITS-1:0] adcout4,
    output [`ADC_BITS-1:0] adcout5,
    output [`ADC_BITS-1:0] adcout6,
    output [`ADC_BITS-1:0] adcout7,
    //ADC slicers
    input [7:0] osp0,
    input [7:0] osp1,
    input [7:0] osp2,
    input [7:0] osp3,
    input [7:0] osp4,
    input [7:0] osp5,
    input [7:0] osp6,
    input [7:0] osp7,
    
    input [7:0] osm0,
    input [7:0] osm1,
    input [7:0] osm2,
    input [7:0] osm3,
    input [7:0] osm4,
    input [7:0] osm5,
    input [7:0] osm6,
    input [7:0] osm7,

    //""what is 3 here, we might need a parameter for it
    input [3:0] asclkd0,
    input [3:0] asclkd1,
    input [3:0] asclkd2,
    input [3:0] asclkd3,
    input [3:0] asclkd4,
    input [3:0] asclkd5,
    input [3:0] asclkd6,
    input [3:0] asclkd7,

    //About Internal Clocks
    input extsel_clk0,
    input extsel_clk1,
    input extsel_clk2,
    input extsel_clk3,
    input extsel_clk4,
    input extsel_clk5,
    input extsel_clk6,
    input extsel_clk7,

    //ADC REF
    input [7:0] vref0,
    input [7:0] vref1,
    input [7:0] vref2,
    
    //CLK outputs
    output clkout_des,
    output clkbout_nc,

    //CKK Calibration
    input [7:0] clkgcal0,
    input [7:0] clkgcal1,
    input [7:0] clkgcal2,
    input [7:0] clkgcal3,
    input [7:0] clkgcal4,
    input [7:0] clkgcal5,
    input [7:0] clkgcal6,
    input [7:0] clkgcal7,
    input [7:0] clkgbias,
    input clkrst,

    //Source Follower
    input ADCBIAS
); 

reg clkrstP_s1, clkrstP_s2, clkrstN_s2;
wire clk_gatedP, clk_gatedN;
reg [1:0] cntP, cntN;

always @(negedge ADCCLKP) begin
    clkrstP_s1 <= clkrst;
    clkrstP_s2 <= clkrstP_s1;   
end
 
always @(negedge ADCCLKM) begin
    clkrstN_s2 <= clkrstP_s2;   
end

assign clk_gatedP = ~(~ADCCLKP | clkrstP_s2);
assign clk_gatedN = ~(~ADCCLKM | clkrstN_s2);
 
// This always block not needed, but including it in case I got the wrong phasing for the output clock
always @(negedge clk_gatedP or posedge clkrstP_s2) begin
    if (clkrstP_s2)  begin
        cntP <= 2'b00;
    end else begin
        cntP <= cntP+1;
    end
end
 
always @(negedge clk_gatedN or posedge clkrstN_s2) begin
    if (clkrstN_s2)  begin
        cntN <= 2'b00;
    end else begin
        cntN <= cntN+1;
    end
end
 
assign clkout_des = ((cntN == 2'b01) ? 1'b1 : 1'b0) & clk_gatedN;

endmodule
