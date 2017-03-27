////////////////////////////////////////////////////////////////////
// 
// File:        sub_adc_tb_param.vh
// Project:     SAR-ADC modeling
// Description: Sub ADC testbench parameter
// Author:      Zhongkai Wang (zhongkai@eecs.berkeley.edu)
// -----------------------------------------------------------------
// Date created:    01/20/2017
// Date modefied:   03/19/2017
// -----------------------------------------------------------------
// Change history:  01/20/2017 - First Created
//                  01/20/2017 - Change glob_CLK_PERIOD to decimal
//                               Add sin_gen parameters
//		    02/21/2017 - Add 'CLK_INIT' to ti_clk generation
//				 Change some ref voltage values and 
//				 Change the dc voltage of input sine sigal
//                  03/04/2017 - Add 'CLK_CORE'
//                  03/19/2017 - Add cap array for calibaration
//                               Change DAC_CAP to UNIT CAP
// -----------------------------------------------------------------
// Parameters:
//      PARAMETER_NAME  RANGE  DEFAULT  UNIT    DESCRIPTION 
//      
////////////////////////////////////////////////////////////////////

//// simulation params /////
`define glob_MAX_CYCLES     1000        //integer, maximum simulation cycles after enable

//// top_level params ///
`define glob_ADC_SR         9.6e9       //sample/s, assuming each sub_adc is 1Gsample/s    
`define glob_ADC_WAYS       8 
`define glob_ADC_BITS       9           //ingeger, adc bits

//// sin_gen params /////    
`define glob_SIN_PERIOD     6666.0     //real, sinusoid input period, ps
`define glob_SIN_AMP        0.1         //real, sinusoid amplitude, V
`define glob_SIN_DC         0.1         //real, sinusoid cm voltage, V

//// asyn_clk params /////
`define glob_ASYN_DEL       10.0        //real, asynchrous clock delay, ps

//// cap_dac params /////
`define glob_PAR_CAP        0.0         //real, parasitic cap in cap-dac, F
`define glob_UNIT_CAP       0.2e-15     //real, cap dac cap, F
`define glob_DAC_CAP        '{1, 2, 4, 8, 16, 32, 64, 128}     //integer, cap dac cap = DAC_CAP * UNITCAP

//// reference params /////
`define glob_VCM_VOL        0.1         //real, cm voltage, V
`define glob_REFP_VOL       0.2         //real, high ref voltage, V
`define glob_REFN_VOL       0.0         //real, low ref voltage, V

`define glob_VRDAC_BITS     8           //integer, reference dac bits
`define glob_VRDAC_HIGH     0.9         //real, reference dac vrefp, V
`define glob_VRDAC_LOW      0.0         //real, reference dac vrefn, V

//// sense_amp params /////
`define glob_SENAMP_DEL     1.0         //real, sense amp delay, ps

`define glob_OSP_VOL       0.0          //real, high ref voltage, V
`define glob_OSN_VOL       0.0          //real, low ref voltage, V

`define glob_OSDAC_BITS     8           //integer, offset dac bits
`define glob_OSDAC_HIGH     0.9         //real, offset dac vrefp, V
`define glob_OSDAC_LOW      0.0         //real, offset dac vrefn, V
`define glob_OFFSET_GAIN    0.25        //real, offset gain

//// ti_clk params /////
`define glob_CLK_INIT	    0		//integer, initial state of ti-clock
`define glob_CLK_CORE       3           //integer, core clock choosing
`define glob_CLK_DFFD       1           //real, delay of DFF in clock distribution, ps
