////////////////////////////////////////////////////////////////////
// 
// File:        sample_hold.sv
// Module:      sample_hold
// Project:     SAR-ADC modeling
// Description: Sample and hold model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/05/2016
// Date modefied:   01/29/2017
// -----------------------------------------------------------------
// Change history:  12/05/2016 - First Created
//                  01/18/2017 - Change the concept of writting, 
//                               using a latch instead of FF
//                  01/19/2017 - Change to differential
//                  01/29/2017 - Delete initial part
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE  DEFAULT  UNIT    DESCRIPTION 
//
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module sample_hold #(
)(
    //input
    input rst,
    input clk,
    input real vip,
    input real vin,
    //output
    output real vop,
    output real von
); 

//Calculate outputp
always @(*)    //latch
    if (rst == 1'd1)
        if (clk == 1'd1)
            vop = vip;
        else
            vop = vop;      //explicitly latch
    else
        vop = 0;

//Calculate outputn
always @(*)    //latch
    if (rst == 1'd1)
        if (clk == 1'd1)
            von = vin;
        else
            von = von;      //explicitly latch
    else
        von = 0;

endmodule
