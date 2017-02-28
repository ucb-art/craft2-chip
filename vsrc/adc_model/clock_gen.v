////////////////////////////////////////////////////////////////////
// 
// File:        clock_gen.v
// Module:      clock_gen
// Project:     SAR-ADC modeling
// Description: ideal clock generation
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/01/2016
// Date modefied:   01/29/2017
// -----------------------------------------------------------------
// Change history:  12/1/2016 - First Created
//                  12/6/2016 - Add rst signal
//                  01/29/2017 - Add parameter description
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION 
//      CLK_PERIOD      (0:inf)     1000.0      ps      real    clock period
//      CLK_DC          (0:1)       0.1                 real    clock duty cycle
////////////////////////////////////////////////////////////////////

`include "../../verilog-src/verilog_header.vh"

module clock_gen #(
    parameter CLK_PERIOD    = 1000.0,   //e-12s
    parameter CLK_DC        = 0.1
)(
    input rst,
    input clk_en,
    output reg clk
);

parameter CLK_T1 = CLK_PERIOD * CLK_DC;
parameter CLK_T2 = CLK_PERIOD * (1-CLK_DC);

reg clk_m;

always begin
    clk_m <= 1'd1;
    #CLK_T1;
    clk_m <= 1'd0;
    #CLK_T2;
end

always @(posedge clk_m)
    if (rst == 1'd0)
        clk <= 1'd0;
    else if (clk_en == 1'd0)
        clk <= clk;
    else
        clk <= 1'd1;

always @(negedge clk_m)
    if (rst == 1'd0)
        clk <= 1'd0;
    else if (clk_en == 1'd0)
        clk <= clk;
    else
        clk <= 1'd0;

endmodule
