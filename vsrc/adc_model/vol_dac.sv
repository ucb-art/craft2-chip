////////////////////////////////////////////////////////////////////
// 
// File:        vol_dac.sv
// Module:      vol_dac
// Project:     SAR-ADC modeling
// Description: Voltage DAC modeling
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    03/20/2017
// Date modefied:   03/21/2017
// -----------------------------------------------------------------
// Change history:  03/20/2017 - First Created
//
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      DAC_BITS        (0:inf)     10                  integer DAC bits
//      DAC_HIGH        (-inf:inf)  0.9         V       real    reference-p voltage
//      DAC_LOW         (-inf:inf)  0.0         V       real    reference-n voltage
//
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module vol_dac #(
    parameter DAC_BITS      = 8,
    parameter DAC_HIGH      = 0.9,        //real, V
    parameter DAC_LOW       = 0.0          //real, V
)(
    //input
    input [DAC_BITS-1:0] data_in,
    //output
    output real vol_out
); 

integer sum, i;
real vref, deltaV;

initial begin
    vref = DAC_HIGH-DAC_LOW;      //Get total range
    deltaV = vref/(2**DAC_BITS);        //Get step size
end

always @(*) begin
    sum = 0;
    for (i=0;i<DAC_BITS;i=i+1)
        if (data_in[i] == 1'd1)
            sum = sum + 2**i;
        else
            sum = sum;

    vol_out = deltaV*sum + DAC_LOW;
end

endmodule
