////////////////////////////////////////////////////////////////////
// 
// File:        cap_dac.sv
// Module:      cap_dac
// Project:     SAR-ADC modeling
// Description: Capacitor DAC model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/05/2016
// Date modefied:   03/24/2017
// -----------------------------------------------------------------
// Change history:  12/05/2016 - First Created
//                  12/16/2016 - Add two refrence voltage, and cm voltage
//                               /  ------ vrefp
//                        CAPDAC -- ------ vcm
//                               \  ------ vrefn
//                  01/13/2017 - The cap dac design is from 'A 10-bit 50-MS/s
//                               SAR ADC with a monotonic capacitor switching
//                               procedure'
//                               With 8 bits, we only need *7* caps
//                               Add input and output description
//                  01/29/2017 - Add parameter description
//                  03/19/2017 - Add cap array for calibaration
//                               Change DAC_CAP to UNIT CAP
//                  03/24/2017 - Swap LSB and MSB
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION 
//      PAR_CAP         [0:inf)     0           F       real    parasitic cap at comparator input
//      UNIT_CAP        (0:inf)     1.0e-15     F       real    CAP DAC unit capacitance
//      ADC_BITS        (0:inf)     8                   integer ADC bits
//      DAC_CAPS        [0:inf)     next row            array   If the value is 0 will use inside calculation
//                                  '{1, 2, 4, 8, 16, 32, 64, 128}                      
//  
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module cap_dac #(
    parameter PAR_CAP       = 0,          //F
    parameter ADC_BITS      = 8, 
    parameter UNIT_CAP      = 1.0e-15,    //F
    parameter integer DAC_CAPS [1:ADC_BITS-1]   = '{1, 2, 4, 8, 16, 32, 64, 128}
    //parameter DAC_CAP1 [1:ADC_BITS-1]   = '{0, 0, 0, 0, 0, 0, 0, 0}
)(
    //input
    input [ADC_BITS-1:1] dac_data_h,
    input [ADC_BITS-1:1] dac_data_l,    //two bits control for each cap
    input real vi,                      //input sampling voltage
    input real vcm,                     //cm voltage, might =(vrefp+vrefn)/2             
    input real vrefp,                   //high reference
    input real vrefn,                   //low reference
    //output
    output real vo                      //output voltage (or input to comparator)
); 

real total_cap, inst_cap, inst_can, inst_cax;
real par_cap;
real cap [ADC_BITS-1:1];    //cap dac capacitor values (ADC-BITS-1)
real sh_cap;                //shield cap
integer i;

//Initial
initial begin
    vo = 0;
    inst_cap = 0;
    inst_can = 0;
    total_cap = 0;
    //assign value for cap dac, mainly for future use with mismatch
    for(i=1;i<ADC_BITS;i=i+1) begin
        if (DAC_CAPS[i]!=0)
            cap[i] = DAC_CAPS[i]*UNIT_CAP;
        else
            cap[i] = (2**(i-1))*UNIT_CAP;
        //$display("cap[%d] is %e", i, cap[i]);     //for debugging
        total_cap = total_cap + cap[i];
    end
    sh_cap = UNIT_CAP;
    par_cap = PAR_CAP;
    total_cap = total_cap + sh_cap + par_cap;
end

//Calculate output voltage
always @(*) begin
    inst_cap = 0;
    inst_can = 0;
    inst_cax = sh_cap;
    for(i=1;i<ADC_BITS;i=i+1) 
        if (dac_data_h[ADC_BITS-i] == 1 && dac_data_l[ADC_BITS-i] == 0)
            inst_cap = inst_cap + cap[ADC_BITS-i];   //Calculate the instanous capacitance connecting to vrefp
        else if (dac_data_h[ADC_BITS-i] == 0 && dac_data_l[ADC_BITS-i] == 1)
            inst_can = inst_can + cap[ADC_BITS-i];   //Calculate the instanous capacitance connecting to vrefn
        else if (dac_data_h[ADC_BITS-i] == 0 && dac_data_l[ADC_BITS-i] == 0)
            inst_cax = inst_cax + cap[ADC_BITS-i];   //Calculate the instanous capacitance connecting to vcm
        else
            if ((dac_data_h[ADC_BITS-i] == 1'dx) || (dac_data_l[ADC_BITS-i] == 1'dx))
                $display("You've got some errors with dac_data_h or dac_data_l!\n");

    vo = (total_cap*vi + inst_cap*vrefp + inst_can*vrefn - (total_cap-inst_cax)*vcm) /
            total_cap;                      //calculate output voltage from charge conservation
end

endmodule
