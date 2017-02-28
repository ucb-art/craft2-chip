////////////////////////////////////////////////////////////////////
// 
// File:        sub_adc.sv
// Module:      sub_adc
// Project:     SAR-ADC modeling
// Description: Sub ADC model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/05/2016
// Date modefied:   01/29/2017
// -----------------------------------------------------------------
// Change history:  12/05/2016 - First Created
//                  01/19/2017 - Connect all blocks
//                  01/27/2017 - Delete some wires: senamp_clk, dac_data_h, dac_data_l
//                  01/29/2017 - Add parameter description
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      ADC_BITS        (0:inf)     8                   integer ADC bits
//      ASYN_DEL        (0:inf)     10.0        ps      real    delay between up and down pulses,
//                                                              also the time for DAC settling
//      PAR_CAP         [0:inf)     0           F       real    parasitic cap at comparator input
//      DAC_CAP         (0:inf)     1.0e-15     F       real    CAP DAC unit capacitance
//      VCM_VOL         (-inf:inf)  0.5         V       real    common mode voltage
//      REFP_VOL        (-inf:inf)  0.5         V       real    reference-p voltage
//      REFP_VOL        (-inf:inf)  0.5         V       real    reference-n voltage
//      SENAMP_DEL      (0:inf)     1.0         ps      real    sense amplifier delay
//
////////////////////////////////////////////////////////////////////

`include "../../verilog-src/verilog_header.vh"

module sub_adc #(
    parameter ADC_BITS    = 8,
    //from asyn_clk
    parameter ASYN_DEL    = 10.0,       //real, ps
    //from cap_dac
    parameter PAR_CAP     = 0.0,        //real, F
    parameter DAC_CAP     = 1.0e-15,    //real, F
    //from refrence
    parameter VCM_VOL     = 0.5,        //real, V
    parameter REFP_VOL    = 0.6,        //real, V
    parameter REFN_VOL    = 0.4,        //real, V
    //from sense_amp
    parameter SENAMP_DEL  = 1.0         //real, ps
)(
    //input
    input rst,
    input real subadc_vip,
    input real subadc_vin,
    input subadc_clk,
    //output
    output [0:ADC_BITS-1] subadc_data,
    output subadc_compl
); 

real sh_vop, sh_von;            //sample and hold voltage

real vcm, vrefp, vrefn;         //references

real dac_vop, dac_von;          //dac voltage

wire asyn_clk;
wire senamp_vmp, senamp_vmn;    //sense_amp mid ouput
wire senamp_vop, senamp_von;    //sense_amp output
wire senamp_done;

//sample_hold instantiation
sample_hold #(
)sample_hold(
    //input
    .rst            (rst),
    .clk            (subadc_clk),
    .vip            (subadc_vip),
    .vin            (subadc_vin),
    //output
    .vop            (sh_vop),
    .von            (sh_von)
); 

//ref_vol instationation
ref_vol #(
    .VCM_VOL        (VCM_VOL),        //V
    .REFP_VOL       (REFP_VOL),       //V
    .REFN_VOL       (REFN_VOL)        //V
)ref_vol(
    //output
    .vcm            (vcm),
    .vrefp          (vrefp),
    .vrefn          (vrefn)
); 

//cap_logic instiation
cap_logic #(
    .ASYN_DEL       (ASYN_DEL),     //ps
    //from cap_dac
    .PAR_CAP        (PAR_CAP),      //F
    .DAC_CAP        (DAC_CAP),      //F
    .ADC_BITS       (ADC_BITS) 
)cap_logic(
    //input
    .rst            (rst),
    .senamp_done    (senamp_done),  //from asyn_clk
    .clk            (subadc_clk),   //from sar_logic
    .senamp_out     (senamp_vop),
    .sh_vop         (sh_vop),       //from cap_dac
    .sh_von         (sh_von),
    .vcm            (vcm),          
    .vrefp          (vrefp),
    .vrefn          (vrefn),
    //output
    .dac_vop        (dac_vop),      //from cap_dac
    .dac_von        (dac_von),
    .asyn_clk       (asyn_clk),     //from asyn_clk, to senseamp
    .adc_data       (subadc_data),  //from sar_logic
    .compl          (subadc_compl)
);

//sense_amp instantiation
sense_amp #(
    .SENAMP_DEL     (SENAMP_DEL)
)sense_amp(
    //input
    .asyn_clk       (asyn_clk),
    .vip            (dac_vop),
    .vin            (dac_von),
    //ouput
    .vmp            (senamp_vmp),
    .vmn            (senamp_vmn),
    .vop            (senamp_vop),
    .von            (senamp_von),
    .done           (senamp_done)
);

endmodule
