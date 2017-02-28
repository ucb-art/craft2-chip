////////////////////////////////////////////////////////////////////
// 
// File:        ref_vol.v
// Module:      ref_vol
// Project:     SAR-ADC modeling
// Description: Voltage reference model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/5/2016
// Date modefied:   12/5/2016
// -----------------------------------------------------------------
// Change history:  12/5/2016 - First Created
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      VCM_VOL         (-inf:inf)  0.5         V       real    common mode voltage
//      REFP_VOL        (-inf:inf)  0.5         V       real    reference-p voltage
//      REFP_VOL        (-inf:inf)  0.5         V       real    reference-n voltage
//
////////////////////////////////////////////////////////////////////

`include "../../verilog-src/verilog_header.vh"

module ref_vol #(
    parameter VCM_VOL       = 0.5,    //V
    parameter REFP_VOL      = 0.6,    //V
    parameter REFN_VOL      = 0.4     //V
)(
    //output
    output real vcm,
    output real vrefp,
    output real vrefn
); 

//Calculate output
assign vcm = VCM_VOL;
assign vrefp = REFP_VOL;
assign vrefn = REFN_VOL;


endmodule
