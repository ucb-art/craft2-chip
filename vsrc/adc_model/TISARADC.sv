////////////////////////////////////////////////////////////////////
// 
// File:        TISARADC.sv
// Module:      TISARADC
// Project:     TI-ADC modeling
// Description: time interleave SAR-ADC model, wrap
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    02/24/2017
// Date modefied:   03/24/2017
// -----------------------------------------------------------------
// Change history:  02/24/2017 - First created
//                  02/25/2017 - Change default values
//                               Add pins from real ADC
//                               Change pin names!!
//                  03/21/2017 - Remove VDD/VSS
//                               Change pin name from uppercase to lowercase
//                               Connect vref and offset control to inside block
//                  03/23/2017 - Add input clock/rst part as real circuit
//                  03/23/2017 - Add some parameters
//                  03/24/2017 - Solve bug on retimer
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
////////////////////////////////////////////////////////////////////

//Things to notice:
//In each, Sub-ADC BIT[0] is the highest bit
//In TI-ADC, ADC[0] is the first ADC

`include "verilog_header.vh"

`define ADC_WAYS    8   
`define ADC_BITS    9
`define DAC_CAPS    '{1, 2, 4, 8, 16, 32, 64, 128}
`define CLK_CORE    3



module TISARADC (
    inout real ADCINP,
    inout real ADCINM,
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
    //for vrefn
    input [7:0] vref0,

    //for vcm
    input [7:0] vref1,

    //for vrefp
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

wire [`ADC_BITS-1:0] adc_data [0:`ADC_WAYS-1];
wire [0:`ADC_WAYS-1] subadc_clk;

reg [`ADC_BITS-1:0] adc_data_mid [0:`ADC_WAYS-1];
reg [`ADC_BITS-1:0] adc_data_pre [0:`ADC_WAYS-1];
reg [`ADC_BITS-1:0] adc_data_fin [0:`ADC_WAYS-1];


reg [7:0] data_vcm [0:`ADC_WAYS-1];
reg [7:0] data_vrefp [0:`ADC_WAYS-1];
reg [7:0] data_vrefn [0:`ADC_WAYS-1];

reg [7:0] data_vosp [0:`ADC_WAYS-1];
reg [7:0] data_vosn [0:`ADC_WAYS-1];

//assign adc data to output, swap the high and low bits
assign adcout0 = adc_data_fin[0];
assign adcout1 = adc_data_fin[1];
assign adcout2 = adc_data_fin[2];
assign adcout3 = adc_data_fin[3];
assign adcout4 = adc_data_fin[4];
assign adcout5 = adc_data_fin[5];
assign adcout6 = adc_data_fin[6];
assign adcout7 = adc_data_fin[7];

//reference control
assign data_vrefn[0]=vref0;
assign data_vrefn[1]=vref0;
assign data_vrefn[2]=vref0;
assign data_vrefn[3]=vref0;
assign data_vrefn[4]=vref0;
assign data_vrefn[5]=vref0;
assign data_vrefn[6]=vref0;
assign data_vrefn[7]=vref0;
 
assign data_vcm[0]=vref1;
assign data_vcm[1]=vref1;
assign data_vcm[2]=vref1;
assign data_vcm[3]=vref1;
assign data_vcm[4]=vref1;
assign data_vcm[5]=vref1;
assign data_vcm[6]=vref1;
assign data_vcm[7]=vref1;

assign data_vrefp[0]=vref2;
assign data_vrefp[1]=vref2;
assign data_vrefp[2]=vref2;
assign data_vrefp[3]=vref2;
assign data_vrefp[4]=vref2;
assign data_vrefp[5]=vref2;
assign data_vrefp[6]=vref2;
assign data_vrefp[7]=vref2;

//offset control
assign data_vosp[0] = osp0;
assign data_vosp[1] = osp1;
assign data_vosp[2] = osp2;
assign data_vosp[3] = osp3;
assign data_vosp[4] = osp4;
assign data_vosp[5] = osp5;
assign data_vosp[6] = osp6;
assign data_vosp[7] = osp7;

assign data_vosn[0] = osm0;
assign data_vosn[1] = osm1;
assign data_vosn[2] = osm2;
assign data_vosn[3] = osm3;
assign data_vosn[4] = osm4;
assign data_vosn[5] = osm5;
assign data_vosn[6] = osm6;
assign data_vosn[7] = osm7;

//Input clk buffer
reg clkrstP_s1, clkrstP_s2, clkrstN_s1, clkrstN_s2;
wire clk_gatedP, clk_gatedN;
reg [1:0] cntP, cntN;
wire clkout_des_dig;
wire clkbout_nc_dig;

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
 
assign clkout_des_dig = ((cntN == 2'b01) ? 1'b1 : 1'b0) & clk_gatedN;
assign clkbout_nc_dig = ((cntN == 2'b11) ? 1'b1 : 1'b0) & clk_gatedN;
//Input clk buffer ends

//sub_adc instantiation
ti_adc_hfck#(
    .ADC_WAYS       (`ADC_WAYS),
    .ADC_BITS       (`ADC_BITS),
    .DAC_CAPS       (`DAC_CAPS),
    .CLK_CORE       (`CLK_CORE)
)ti_adc_hfck(        
    //input
    .rst            (clkrstP_s2),
    .adc_clkp       (clk_gatedP),
    .adc_clkn       (clk_gatedN),
    .adc_vip        (ADCINP),
    .adc_vin        (ADCINM),
    .data_vcm       (data_vcm),
    .data_vrefp     (data_vrefp), 
    .data_vrefn     (data_vrefn),
    .data_vosp      (data_vosp),
    .data_vosn      (data_vosn),
    //output
    .adc_data       (adc_data),
    //output [0:ADC_WAYS-1] adc_compl,
    .subadc_clk     (subadc_clk),
    .adc_coreclk    (clkout_des),
    .adc_coreclkb   (clkbout_nc)
); 

//retimer 1st stage
always @(subadc_clk[5] or adc_data)
    if (subadc_clk[5] == 1'd1) adc_data_mid[0:3] = adc_data[0:3];
always @(subadc_clk[1] or adc_data)
    if (subadc_clk[1] == 1'd1) adc_data_mid[4:7] = adc_data[4:7];

//retimer 2nd stage
always @(subadc_clk[3] or adc_data_mid) 
    if (subadc_clk[3] == 1'd1) adc_data_pre = adc_data_mid;

//retimer 3rd stage
always @(subadc_clk[7] or adc_data_pre)
    if (subadc_clk[7] == 1'd1) adc_data_fin = adc_data_pre;
//Retimer ends

endmodule
