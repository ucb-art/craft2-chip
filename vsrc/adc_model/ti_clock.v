////////////////////////////////////////////////////////////////////
// 
// File:        ti_clock.v
// Module:      ti_clock
// Project:     SAR-ADC modeling
// Description: time interleave clock generation
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    01/30/2017
// Date modefied:   02/21/2017
// -----------------------------------------------------------------
// Change history:  01/30/2017 - First Created
//                  02/21/2017 - Change one output to multi output
//                               Only need one block for ti-ADC
//                               Change parameters
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE           DEFAULT     UNIT    TYPE    DESCRIPTION 
//      ADC_WAYS        (0:inf)         8                   integer adc ways
//      CLK_INIT        [0:ADCWAYS-1]   0                   integer reset clk, 
//                                                                  to designate which clk reset to 1
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module ti_clock #(
    parameter ADC_WAYS      = 8,
    parameter CLK_INIT      = 0,
    parameter CLK_CORE      = 3
)(
    input rst,
    input clk,
    output [0:ADC_WAYS-1] ti_clk,
    output reg core_clk
);

integer HALF_WAY = ADC_WAYS/2;

reg [0:ADC_WAYS-1] clk_m;
//reg core_clk;
integer i;

always @(posedge clk or negedge rst) begin
    if (rst == 1'd0) 
        for (i=0; i<ADC_WAYS; i=i+1) 
            if (i == CLK_INIT)
                clk_m[i] <= 1'd1;
            else
                clk_m[i] <= 1'd0;
    else
        for (i=0; i<ADC_WAYS; i=i+1) 
            if (i == 0)
                clk_m[i] <= clk_m[ADC_WAYS-1];
            else
                clk_m[i] <= clk_m[i-1];
end

assign ti_clk = clk_m;

always @(posedge clk_m[CLK_CORE] or negedge rst)
    if (rst == 1'd0) 
        core_clk <= 1'd0;
    else
        core_clk <= 1'd1;
always @(posedge clk_m[CLK_CORE+HALF_WAY])
    core_clk <= 1'd0;

endmodule
