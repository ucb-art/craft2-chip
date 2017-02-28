////////////////////////////////////////////////////////////////////
// 
// File:        sar_logic.v
// Module:      sar_logic
// Project:     SAR-ADC modeling
// Description: SAR logic
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/06/2016
// Date modefied:   02/21/2017
// -----------------------------------------------------------------
// Change history:  12/06/2016 - First Created
//                  12/07/2016 - Add 'init' signal generation
//                             - Add rst pin
//                  01/17/2017 - Change from synchronous to asyn
//                             - Change input/output
//                  01/23/2017 - Add 'clk' as reset for i and compl signal
//                  01/26/2017 - Rewrite all stuff, using FSM method
//                             - add rst signal
//                             - add more judgment on 'posedge asyn_clk', line 63
//                  01/29/2017 - Add parameter description
//                  02/21/2017 - Add state when 'rst' goes to 0
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      ADC_BITS        [1:inf)     8           1       integer ADC bits
//
////////////////////////////////////////////////////////////////////

`include "../../verilog-src/verilog_header.vh"

module sar_logic #(
    parameter ADC_BITS      = 8
)(
    //input
    input rst,
    input clk,
    input asyn_clk,
    input senamp_out,
    //output
    output reg [1:ADC_BITS-1] dac_data_h,   //data to cap dac
    output reg [1:ADC_BITS-1] dac_data_l,
    output reg [0:ADC_BITS-1] adc_data,     //adc output
    output reg compl                        //adc complete flag
); 

parameter INIT      = 2'b00;
parameter COMP      = 2'b01;
parameter DAC       = 2'b10;
parameter FINISH    = 2'b11;

parameter WIDTH = $clog2(ADC_BITS);

reg [WIDTH:0] i;
reg [1:0] state;
reg [0:ADC_BITS-1] adc_data_m;

//Calculating states
always @(posedge clk or negedge rst)           //state FINISH 
        state <= INIT;
always @(posedge asyn_clk or negedge rst)      //state INIT or DAC or even in state FINISH
    if (rst == 1'd1)
        //there's some cases that in state FINISH, avoid it go to state COMP
        //corresponding to the dash line in FSM figure
        if (i< ADC_BITS)
            state <= COMP;
        else
            state <= FINISH;
    else
        state <= INIT;
always @(negedge asyn_clk or negedge rst)      //state COMP 
    if (rst == 1'd1)
        if (i < ADC_BITS)
            state <= DAC;
        else
            state <= FINISH;
    else
        state <= INIT;

//calculation ouput
always @(state)
    case (state)
        INIT: begin
            adc_data <= adc_data_m;
            dac_data_h <= {(ADC_BITS-1){1'd0}};
            dac_data_l <= {(ADC_BITS-1){1'd0}};
            adc_data_m <= {ADC_BITS{1'd0}};
            i <= 2'd0;
            compl <= 1'd0;
        end
        COMP: begin
            i <= i + 1'd1;
            //compl <= 1'd0;
        end
        DAC: begin
            dac_data_h[i] <= senamp_out;
            dac_data_l[i] <= ~senamp_out;
            adc_data_m[i-1] <= senamp_out;
            //compl <= 1'd0;
        end
        FINISH: begin
            adc_data_m[i-1] <= senamp_out;
            compl <= 1'd1;
        end
    endcase

endmodule
