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


    input EXTCLK0,
    input EXTCLK1,
    input EXTCLK2,
    input EXTCLK3,
    input EXTCLK4,
    input EXTCLK5,
    input EXTCLK6,
    input EXTCLK7,
    
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
    output CLKOUT_DES,

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


endmodule
