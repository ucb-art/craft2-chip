////////////////////////////////////////////////////////////////////
// 
// File:        ti_clock.v
// Module:      ti_clock
// Project:     SAR-ADC modeling
// Description: time interleave clock generation
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    03/04/2017
// Date modefied:   03/23/2017
// -----------------------------------------------------------------
// Change history:  03/04/2017 - First Created
//                  03/04/2017 - Based on real design,
//                               use DFF and TGATE to work
//                  03/23/2017 - Change RST to 'high' sensitive
//                               Change core clock connection with inside sub-ADC clock
//                               w/o 50% duty cycle
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE           DEFAULT     UNIT    TYPE    DESCRIPTION 
//      ADC_WAYS        (0:inf)         8                   integer adc ways
//      CLK_INIT        [0:ADCWAYS-1]   0                   integer reset clk, 
//                                                                  to designate which clk reset to 1
//      CLK_CORE        [0:ADCWYS-1]    3                   integer core clock choosing
//      CLK_DFFD        [0:inf)         1                   real    delay of DFF in clock distribution
// -----------------------------------------------------------------
// Notes:
//      Only works for even number of ways for now
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module ti_clock_half #(
    parameter ADC_WAYS      = 8,
    parameter CLK_INIT      = 0,
    parameter CLK_CORE      = 3,
    parameter CLK_DFFD      = 1     //DFF in Clock Distribution delay, ps 
)(
    input rst,
    input clkp,
    input clkn,
    output [0:ADC_WAYS-1] ti_clk,
    output core_clk,
    output core_clkb
);

parameter HALF_WAY = ADC_WAYS/2;

reg [0:HALF_WAY-1] enp, enn;
reg [0:ADC_WAYS-1] clk_m;
integer i;

always @(negedge clkp or posedge rst) begin     //same as real design, genrating en signals
    if (rst == 1'd1) 
        for (i=0; i<HALF_WAY; i=i+1) 
            if (i == CLK_INIT)
                enp[i] <= #CLK_DFFD 1'd1;
            else
                enp[i] <= #CLK_DFFD 1'd0;
    else
        for (i=0; i<HALF_WAY; i=i+1) 
            if (i == 0)
                enp[i] <= #CLK_DFFD enp[HALF_WAY-1];
            else
                enp[i] <=  #CLK_DFFD enp[i-1];
end

always @(negedge clkn or posedge rst) begin     //same as real design, genrating en signals

    if (rst == 1'd1) 
        for (i=0; i<HALF_WAY; i=i+1) 
            if (i == CLK_INIT)
                enn[i] <= #CLK_DFFD 1'd1;
            else
                enn[i] <= #CLK_DFFD 1'd0;
    else
        for (i=0; i<HALF_WAY; i=i+1) 
            if (i == 0)
                enn[i] <= #CLK_DFFD enn[HALF_WAY-1];
            else
                enn[i] <= #CLK_DFFD enn[i-1];
end

always @(*)                                      //tgate
    for (i=0; i<HALF_WAY; i=i+1) begin
        if (enp[i] == 1'd1)
            clk_m[2*i] = clkp;
        else
            clk_m[2*i] = 1'd0;

        if (enn[i] == 1'd1)
            clk_m[2*i+1] = clkn;
        else 
            clk_m[2*i+1] = 1'd0;
    end

assign ti_clk = clk_m;

/*
always @(posedge clk_m[CLK_CORE] or negedge rst)
    if (rst == 1'd0) 
        core_clk <= 1'd0;
    else
        core_clk <= 1'd1;
always @(posedge clk_m[CLK_CORE+HALF_WAY])
    core_clk <= 1'd0;
*/
assign core_clk = clk_m[CLK_CORE];
assign core_clkb = clk_m[CLK_CORE+HALF_WAY];

endmodule
