////////////////////////////////////////////////////////////////////
// 
// File:        cap_logic.sv
// Module:      cap_logic
// Project:     SAR-ADC modeling
// Description: CAP DAC, aysn_clkgen and SAR logic, ideal
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    01/19/2017
// Date modefied:   03/24/2017
// -----------------------------------------------------------------
// Change history:  01/19/2017 - First Created
//                  01/29/2017 - Add parameters
//                  03/19/2017 - Add cap array for calibaration
//                               Change DAC_CAP to UNIT CAP
//                  03/23/2017 - Delete 'rst' signal
//                  03/24/2017 - Swap LSB and MSB
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE       DEFAULT     UNIT    DESCRIPTION
//      ASYN_DEL        (0:inf)     10.0        ps      delay between up and down pulses,
//                                                      also the time for DAC settling
//      PAR_CAP         [0:inf)     0           F       parasitic cap at comparator input
//      UNIT_CAP        (0:inf)     1.0e-15     F       CAP DAC unit capacitance
//      ADC_BITS        (0:inf)     8                   ADC bits
//      DAC_CAPS        [0:inf)     next row            array   If the value is 0 will use inside calculation
//                                  '{1, 2, 4, 8, 16, 32, 64, 128}  
//
////////////////////////////////////////////////////////////////////

module cap_logic #(
    parameter ASYN_DEL      = 10.0,     //e-12s
    //from cap_dac
    parameter ADC_BITS      = 8,        
    parameter PAR_CAP       = 0,        //F
    parameter UNIT_CAP      = 1.0e-15,    //F
    parameter integer DAC_CAPS [1:ADC_BITS-1]   = '{1, 2, 4, 8, 16, 32, 64, 128}
)(
    //input
    input senamp_done,                  //from asyn_clk
    input clk,                          //from sar_logic
    input senamp_out,
    input real sh_vop,                  //from cap_dac
    input real sh_von,
    input real vcm,          
    input real vrefp,
    input real vrefn,
    //output
    output real dac_vop,                //from cap_dac
    output real dac_von,
    output asyn_clk,                    //from asyn_clk, to senseamp
    output [ADC_BITS-1:0] adc_data,     //from sar_logic
    output compl
);

wire [1:ADC_BITS-1] dac_data_h, dac_data_l;

//asyn_clkgen instatiation
asyn_clkgen #(
    .ASYN_DEL       (ASYN_DEL) 
)asyn_clkgen(
    //input
    .clk            (clk),
    .compl          (compl),
    .senamp_done    (senamp_done),      //comparator senamp_done
    //output
    .asyn_clk       (asyn_clk)
); 

//sar_logic instantiation
sar_logic #(
    .ADC_BITS       (ADC_BITS)
)sar_logic(
    //input
    .clk            (clk),
    .asyn_clk       (asyn_clk),
    .senamp_out     (senamp_out),
    //ouput
    .dac_data_h     (dac_data_h),
    .dac_data_l     (dac_data_l),
    .adc_data       (adc_data),
    .compl          (compl)
);

//cap_dac instiation -- p path
cap_dac #(
    .PAR_CAP        (PAR_CAP),
    .UNIT_CAP       (UNIT_CAP),
    .DAC_CAPS       (DAC_CAPS),
    .ADC_BITS       (ADC_BITS) 
)cap_dacp(
    //input
    .dac_data_h     (dac_data_l),
    .dac_data_l     (dac_data_h),
    .vi             (sh_vop),          //data from sample and hold
    .vcm            (vcm),              //cm voltage, might =(vrefp+vrefn)/2 
    .vrefp          (vrefp),            //high reference
    .vrefn          (vrefn),            //low reference
    //output
    .vo             (dac_vop)          //output voltage (or input to comparator)
);
//n path
cap_dac #(
    .PAR_CAP        (PAR_CAP),
    .UNIT_CAP       (UNIT_CAP),
    .DAC_CAPS       (DAC_CAPS),
    .ADC_BITS       (ADC_BITS) 
)cap_dacn(
    //input
    .dac_data_h     (dac_data_h),
    .dac_data_l     (dac_data_l),
    .vi             (sh_von),          //data from sample and hold
    .vcm            (vcm),              //cm voltage, might =(vrefp+vrefn)/2 
    .vrefp          (vrefp),            //high reference
    .vrefn          (vrefn),            //low reference
    //output
    .vo             (dac_von)          //output voltage (or input to comparator)
);

endmodule

