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

`timescale 1ps/1fs
`default_nettype none

`define PI 3.1415926535897932
`define NS_TO_FS 1e6
`define S_TO_FS 1e15
`define S_TO_PS 1e12
`define ADC_WAYS    8   
`define ADC_BITS    9


module TISARADC (
    //input
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


    input extclk0,
    input extclk1,
    input extclk2,
    input extclk3,
    input extclk4,
    input extclk5,
    input extclk6,
    input extclk7,
    
    //ADC REF
    input [7:0] vref00,
    input [7:0] vref01,
    input [7:0] vref02,
    input [7:0] vref03,
    input [7:0] vref04,
    input [7:0] vref05,
    input [7:0] vref06,
    input [7:0] vref07,

    input [7:0] vref10,
    input [7:0] vref11,
    input [7:0] vref12,
    input [7:0] vref13,
    input [7:0] vref14,
    input [7:0] vref15,
    input [7:0] vref16,
    input [7:0] vref17,

    input [7:0] vref20,
    input [7:0] vref21,
    input [7:0] vref22,
    input [7:0] vref23,
    input [7:0] vref24,
    input [7:0] vref25,
    input [7:0] vref26,
    input [7:0] vref27,

    input [7:0] iref0,
    input [7:0] iref1,
    input [7:0] iref2,
    
    //CLK outputs
    output clkout_des,

    //ClK Calibration
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


reg clk = 1'b0;
always #3000 clk = !clk;
assign clkout_des = clk;

endmodule
