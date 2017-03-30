////////////////////////////////////////////////////////////////////
// 
// File:        sub_adc.sv
// Module:      sub_adc
// Project:     SAR-ADC modeling
// Description: Sub ADC model, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    12/05/2016
// Date modefied:   03/24/2017
// -----------------------------------------------------------------
// Change history:  12/05/2016 - First Created
//                  01/19/2017 - Connect all blocks
//                  01/27/2017 - Delete some wires: senamp_clk, dac_data_h, dac_data_l
//                  01/29/2017 - Add parameter description
//                  03/19/2017 - Add cap array for calibaration
//                               Change DAC_CAP to UNIT CAP
//                  03/21/2017 - Add reference voltage DACs and control signals
//                               Add offset DACs and control signals
//                  03/23/2017 - Change 'rst' connection
//                  03/24/2017 - Swap LSB and MSB
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    TYPE    DESCRIPTION
//      ADC_BITS        (0:inf)     8                   integer ADC bits
//      ASYN_DEL        (0:inf)     10.0        ps      real    delay between up and down pulses,
//                                                              also the time for DAC settling
//      PAR_CAP         [0:inf)     0           F       real    parasitic cap at comparator input
//      UNIT_CAP        (0:inf)     1.0e-15     F       real    CAP DAC unit capacitance
//      DAC_CAPS        [0:inf)     next row            array   If the value is 0 will use inside calculation
//                                  '{1, 2, 4, 8, 16, 32, 64, 128}              
//      VRDAC_BITS      (0:inf)     8                   integer reference voltage DAC bits
//      VRDAC_HIGH      [0:inf)     0.9         V       real    reference DAC positive reference
//      VRDAC_LOW       [0:inf)     0           V       real    reference DAC negtive reference
//      SENAMP_DEL      (0:inf)     1.0         ps      real    sense amplifier delay
//      OSDAC_BITS      (0:inf)     8                   integer offset voltage DAC bits
//      OSDAC_HIGH      [0:inf)     0.9         V       real    offset DAC positive reference
//      OSDAC_LOW       [0:inf)     0           V       real    offset DAC negtive reference
//      OFFSET_GAIN     [0:1]       0.25                real    offset gain for sense amplifier
//
////////////////////////////////////////////////////////////////////

`include "verilog_header.vh"

module sub_adc #(
    parameter ADC_BITS      = 8,
    //from asyn_clk
    parameter ASYN_DEL      = 10.0,       //real, ps
    //from cap_dac
    parameter PAR_CAP       = 0.0,        //real, F
    parameter UNIT_CAP      = 1.0e-15,    //F
    parameter integer DAC_CAPS [1:ADC_BITS-1]   = '{1, 2, 4, 8, 16, 32, 64, 128},
    //from refrence
    parameter VRDAC_BITS    = 8,
    parameter VRDAC_HIGH    = 0.9,
    parameter VRDAC_LOW     = 0.0,
    //parameter VCM_VOL     = 0.1,        //real, V
    //parameter REFP_VOL    = 0.2,        //real, V
    //parameter REFN_VOL    = 0.0,        //real, V

    //from sense_amp
    parameter SENAMP_DEL    = 1.0,        //real, ps
    ////sense_amp offset control
    parameter OSDAC_BITS    = 8,
    parameter OSDAC_HIGH    = 0.9,
    parameter OSDAC_LOW     = 0.0,
    parameter OFFSET_GAIN   = 0.25        //real
)(
    //input
    input rst,
    input real subadc_vip,
    input real subadc_vin,
    input subadc_clk,
    input [VRDAC_BITS-1:0] data_vcm,
    input [VRDAC_BITS-1:0] data_vrefp, 
    input [VRDAC_BITS-1:0] data_vrefn,
    input [OSDAC_BITS-1:0] data_vosp,
    input [OSDAC_BITS-1:0] data_vosn,
    //output
    output [ADC_BITS-1:0] subadc_data,
    output subadc_compl
); 

real sh_vop, sh_von;            //sample and hold voltages
real vcm, vrefp, vrefn;         //references
real dac_vop, dac_von;          //dac voltages
real off_vosp, off_vosn;          //offset voltages

wire asyn_clk;
wire senamp_vmp, senamp_vmn;    //sense_amp mid ouput
wire senamp_vop, senamp_von;    //sense_amp output
wire senamp_done;

//sample_hold instantiation
sample_hold #(
)sample_hold(
    //input
    .clk            (subadc_clk),
    .vip            (subadc_vip),
    .vin            (subadc_vin),
    //output
    .vop            (sh_vop),
    .von            (sh_von)
); 

/*
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
*/

//Reference voltage DACs instantiation
vol_dac #(
    .DAC_BITS       (VRDAC_BITS),
    .DAC_HIGH       (VRDAC_HIGH),
    .DAC_LOW        (VRDAC_LOW)
)vol_dac_vcm(
    //input
    .data_in        (data_vcm),
    //ouput
    .vol_out        (vcm)
);
vol_dac #(
    .DAC_BITS       (VRDAC_BITS),
    .DAC_HIGH       (VRDAC_HIGH),
    .DAC_LOW        (VRDAC_LOW)
)vol_dac_vrefp(
    //input
    .data_in        (data_vrefp),
    //ouput
    .vol_out        (vrefp)
);
vol_dac #(
    .DAC_BITS       (VRDAC_BITS),
    .DAC_HIGH       (VRDAC_HIGH),
    .DAC_LOW        (VRDAC_LOW)
)vol_dac_vrefn(
    //input
    .data_in        (data_vrefn),
    //ouput
    .vol_out        (vrefn)
);

//cap_logic instiation
cap_logic #(
    .ASYN_DEL       (ASYN_DEL),     //ps
    //from cap_dac
    .PAR_CAP        (PAR_CAP),      //F
    .UNIT_CAP       (UNIT_CAP),     //F
    .DAC_CAPS       (DAC_CAPS),      
    .ADC_BITS       (ADC_BITS) 
)cap_logic(
    //input
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
    .SENAMP_DEL     (SENAMP_DEL),
    .OFFSET_GAIN    (OFFSET_GAIN)
)sense_amp(
    //input
    .asyn_clk       (asyn_clk),
    .vip            (dac_vop),
    .vin            (dac_von),
    .vosp           (off_vosp),
    .vosn           (off_vosn),
    //ouput
    .vmp            (senamp_vmp),
    .vmn            (senamp_vmn),
    .vop            (senamp_vop),
    .von            (senamp_von),
    .done           (senamp_done)
);


//Offset voltage DACs instiation
vol_dac #(
    .DAC_BITS       (OSDAC_BITS),
    .DAC_HIGH       (OSDAC_HIGH),
    .DAC_LOW        (OSDAC_LOW)
)vol_dac_vosp(
    //input
    .data_in        (data_vosp),
    //ouput
    .vol_out        (off_vosp)
);
vol_dac #(
    .DAC_BITS       (OSDAC_BITS),
    .DAC_HIGH       (OSDAC_HIGH),
    .DAC_LOW        (OSDAC_LOW)
)vol_dac_vosn(
    //input
    .data_in        (data_vosn),
    //ouput
    .vol_out        (off_vosn)
);

endmodule
