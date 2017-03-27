////////////////////////////////////////////////////////////////////
// 
// File:        sample_hold.sv
// Module:      sample_hold
// Project:     SAR-ADC modeling
// Description: Sample and hold model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/05/2016
// Date modefied:   03/23/2017
// -----------------------------------------------------------------
// Change history:  12/05/2016 - First Created
//                  01/18/2017 - Change the concept of writting, 
//                               using a latch instead of FF
//                  01/19/2017 - Change to differential
//                  01/29/2017 - Delete initial part
//                  03/23/2017 - Delete 'rst' signal
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE  DEFAULT  UNIT    DESCRIPTION 
//
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module sample_hold #(
)(
    //input
    input clk,
    input real vip,
    input real vin,
    //output
    output real vop,
    output real von
); 

//Calculate outputp
always @(*)    //latch
    if (clk == 1'd1)
        vop = vip;
    else
        vop = vop;      //explicitly latch

//Calculate outputn
always @(*)    //latch
    if (clk == 1'd1)
        von = vin;
    else
        von = von;      //explicitly latch

endmodule
