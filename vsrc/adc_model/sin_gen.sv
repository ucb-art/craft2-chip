////////////////////////////////////////////////////////////////////
// 
// File:        sin_gen.sv
// Module:      sin_gen
// Project:     SAR-ADC modeling
// Description: sinusoid wave generation
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/02/2016
// Date modefied:   01/29/2017
// -----------------------------------------------------------------
// Change history:  12/02/2016 - First Created
//                  01/29/2017 - Add parameter description
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION 
//      SIN_PERIOD      (0:inf)     10.0        ns      real    sinusoid period
//      SIN_AMP         (0:inf)     0.5         V       real    sinusoid amplitude
//      SIN_DC          (-inf:inf)  0.5         V       real    sinusoid dc offset
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module sin_gen #(
    parameter SIN_PERIOD = 10.0,
    parameter SIN_AMP    = 0.5,
    parameter SIN_DC     = 0.5
)(
    output real sin_out
);

import "DPI" function real sin(real x);

//Initial
initial sin_out = 0;

//Calculate Sinusoid 
always #(1) begin
    sin_out = SIN_DC + SIN_AMP*sin(2*`PI/SIN_PERIOD*$realtime());
    //$display(sin(2*`PI/SIN_PERIOD*$realtime()), "   ", $realtime());
end

endmodule
