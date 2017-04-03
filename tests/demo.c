//========================================================================
// Program pattern generator to accept test patterns over the XBAR to play
// on the DSP chain
//========================================================================
#define CRAFT_DEBUG

#include <stdio.h>
#include <stdint.h>

// This header file defines the API for interacting with a DSP Chain
#include "chain_api.h"

int main(void)
{
  printf("Configuring ADC");
  //write_reg(craft_radar_ASCLKD, 2863311530, "ADC ASCLKD") ;// [stevo]: not used in SV model
  write_reg(craft_radar_VREF1, 48);
  write_reg(craft_radar_VREF2, 96);
  printf("Configuring ADC Calibration");
  write_reg(craft_radar_MODE, 1);
  printf("Done\n");

  return 0;
}

