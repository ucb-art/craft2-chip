//========================================================================
// Program pattern generator to accept test patterns over the XBAR to play
// on the DSP chain
//========================================================================
#define CRAFT_DEBUG

#include <stdio.h>
#include <stdint.h>

// This header file defines the API for interacting with a DSP Chain
#include "fft_input_vector.h"  //get vector file
#include "pg_prog.h"     //PG subroutines
#include "memcpy.h"

// BOZO test specific
static void set_tuner_ctrl() {
  // Now program the DSP blocks as necessary, e.g. multiplier for tuner
  write_reg_debug(craft_radar_tuner_FixedTunerMultiplier, 0, "tuner multiplier");
  printf("Done Tuner control\n");
}

int main(void)
{
  int i, j;
  uint8_t sam_output_buffer[64];
  sam_capture* cap = &craft_radar_tuner_sam_capture;

  cap->output = sam_output_buffer;
  cap->output_size = sizeof(sam_output_buffer);
  cap->n_samps    = 2;     //BOZO
  cap->start_addr = 0;    //BOZO
  
  //Pattern Generator
  pg_init();
  pg_create_data(pg_mem, test_arr, MY_SIZE);
  pg_load_data(0L, MY_SIZE);
  set_tuner_ctrl();

  printf("Initiate Tuner SAM capture\n");
  initiate_sam_capture(cap);

  pg_play();
  printf("Playing PG");
  pg_end();


  printf("calling Tuner SAM\n");
  cap->use_dma = 0;
  get_sam_output(cap);

  printf("calling Tuner SAM with DMA\n");
  cap->use_dma = 1;
  get_sam_output(cap);

  return 0;
}

