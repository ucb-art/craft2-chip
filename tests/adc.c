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
  // ADC
  printf("\n\n\n");
  printf("Configuring ADC\n");
  write_reg(craft_radar_VREF1, 48);
  write_reg(craft_radar_VREF2, 96);
  printf("Configuring ADC Calibration\n");
  write_reg(craft_radar_MODE, 1);
  write_reg(craft_radar_ADC_VALID, 1);
  printf("Done configuring ADC\n");

  return 0;
}

