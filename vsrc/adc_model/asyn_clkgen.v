////////////////////////////////////////////////////////////////////
// 
// File:        asyn_clkgen.v
// Module:      asyn_clkgen
// Project:     SAR-ADC modeling
// Description: Asynchronous SAR logic, clock generation
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/16/2016
// Date modefied:   02/21/2017
// -----------------------------------------------------------------
// Change history:  12/16/2016 - First Created
//                  01/13/2017 - Ref is Yidu's thesis, Page 30
//                  01/19/2017 - Some bug at rst signal!!
//                               Just delete rst, use two signals to trigger senamp,
//                               see sen_amp for detail
//                  01/25/2017 - Rewriting all the stuff, using FSM method
//                  01/26/2017 - add rst singal
//                               add some delay to clock generation
//                  01/29/2017 - Add parameter description
//                  02/21/2017 - Add state when 'rst' goes to 0
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      ASYN_DEL        (0:inf)     10.0        ps      real    delay between up and down pulses,
//                                                              also the time for DAC settling
//
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module asyn_clkgen #(
    parameter ASYN_DEL      = 10.0  //e-12s 
)(
    //input
    input rst,
    input clk,
    input compl,
    input senamp_done,              //comparator done
    //output
    output reg asyn_clk
); 

parameter INIT1 = 2'b00;
parameter INIT2 = 2'b11;
parameter LOW   = 2'b01;
parameter HIGH  = 2'b10;

reg up, down;
reg [1:0] state;

always @(*) begin
    down <= senamp_done;
    up <= #ASYN_DEL senamp_done;
end

//Calculating states
always @(posedge clk or negedge rst)       //in state HIGH
        state <= INIT2;
always @(negedge clk or negedge rst)       //in state INIT1
    if (rst == 1'd1)
        state <= INIT1;
    else
        state <= INIT2;
always @(posedge down or negedge rst)      //in state INIT2 or state HIGH
    if (rst == 1'd1)
        if (compl == 1'd1)
            state <= HIGH;
        else
            state <= LOW;
    else
        state <= INIT2;
always @(posedge up or negedge rst)        //in state LOW
    if (rst == 1'd1)
        state <= HIGH;
    else
        state <= INIT2;

//Caluclating output
always @(state)
    case (state)
        INIT2:  asyn_clk <= #0.1 1'd0;
        INIT1:  asyn_clk <= #0.1 1'd1;
        LOW:    asyn_clk <= #0.1 1'd0;
        HIGH:   asyn_clk <= #0.1 1'd1;
        default:
                asyn_clk <= 1'd0;
    endcase

endmodule
