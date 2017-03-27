////////////////////////////////////////////////////////////////////
// 
// File:        sense_amp.sv
// Module:      sense_amp
// Project:     SAR-ADC modeling
// Description: Sense amplifier (comparator) model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/01/2016
// Date modefied:   03/21/2017
// -----------------------------------------------------------------
// Change history:  12/01/2016 - First Created
//                  12/16/2016 - Add delay to output
//                  01/13/2017 - Add vmp, vmn, vop, von and done
//                               Delete dout
//                  01/17/2017 - Add done signal
//                  01/24/2017 - Make change about delay,
//                               for rising edge, add delay to output,
//                               for falling edge, make delay to vmp, vmn be 0
//                  01/29/2017 - Add parameters
//                               Delete all things about dout
//                  03/21/2017 - Add offset voltages: vosp, vosn
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      SENAMP_DEL      (0:inf)     1.0         ps      real    sense amplifier delay
//      OFFSET_GAIN     [0:1]       0.25                real    offset gain for sense amplifier
//
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module sense_amp #(
    parameter SENAMP_DEL    = 1.0,  //e-12s
    parameter OFFSET_GAIN   = 0.25
)(
    //input
    input asyn_clk,
    input real vip,
    input real vin,
    input real vosp,
    input real vosn,
    //output
    output reg vmp,
    output reg vmn,
    output reg vop,
    output reg von,
    output reg done
); 

//Calculate ouput
always @(posedge asyn_clk) 
    if ((vip+OFFSET_GAIN*vosp) >= (vin+OFFSET_GAIN*vosn)) begin
        vmp <= #SENAMP_DEL 1'd1;
        vmn <= #SENAMP_DEL 1'd0;
        vop <= #SENAMP_DEL 1'd1;
        von <= #SENAMP_DEL 1'd0;
    end
    else begin
        vmp <= #SENAMP_DEL 1'd0;
        vmn <= #SENAMP_DEL 1'd1;
        vop <= #SENAMP_DEL 1'd0;
        von <= #SENAMP_DEL 1'd1;
    end
always @(negedge asyn_clk) begin
    vmp <= 1'd1;
    vmn <= 1'd1;
end

assign done = vmp^vmn;

endmodule
