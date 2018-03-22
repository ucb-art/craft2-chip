#include <stdio.h>
#include <stdint.h>
#include <string.h>

#include "chain_api.h"


//int unscramble_fft_index(int bin) {
//  int map[64] = {0,32,16,48,8,40,24,56,4,36,20,52,12,44,28,60,2,34,18,
//    50,10,42,26,58,6,38,22,54,14,46,30,62,1,33,17,49,9,41,25,57,5,37,
//    21,53,13,45,29,61,3,35,19,51,11,43,27,59,7,39,23,55,15,47,31,63};
//
//  if ((bin > 64) | bin < 0) {
//    return 0;
//  } else {
//    return map[bin];
//  }
//}

int main(void) {

  // should be multiple of 256 to get full spectrum if 8192
  int samples = 2;

  // go!
  printf("\nConfig ADC\n");
  write_reg(acmes_VREF1, 48);
  write_reg(acmes_VREF2, 96);
  printf("Configuring ADC Calibration\n");
  write_reg(acmes_MODE, 1);
  write_reg(acmes_ADC_VALID, 1);
  printf("Done config ADC\nConfig SAM\n");

  // accum - initiate SAM
  write_reg(acmes_accum_NumSpectraToAccumulate, 1);
  sam_capture* accum_cap = &acmes_accum_sam_capture;
  unsigned long accum_mul = (accum_cap->pow2_width)/sizeof(unsigned long)/8;
  uint64_t accum_sam_output_buffer[accum_mul*samples];
  accum_cap->output = accum_sam_output_buffer;
  accum_cap->output_size = sizeof(accum_sam_output_buffer);
  accum_cap->n_samps    = samples;
  accum_cap->start_addr = 0;
  accum_cap->wait_for_sync = 1;
  accum_cap->use_dma = 0;
  printf("Done config SAM\nInit SAM capture\n");
  initiate_sam_capture(accum_cap);

  // read out accum results
  unsigned long *arr_accum = (unsigned long*)(accum_cap->output);
  unsigned long write_count = 0;
  printf("Wait for SAM to fill\n");
  while (write_count < accum_cap->n_samps) {
    write_count  = read_reg(accum_cap->ctrl_base + SAM_W_WRITE_COUNT_OFFSET);
  }
  // write_count should be samples (4)
  // accum_mul should be 2048/64 = 32 
  //get_sam_output(accum_cap); 
  printf("write count = %llx, accum_mul = %llx", write_count, accum_mul);
  memcpy(arr_accum, (unsigned long*)(accum_cap->data_base), write_count * accum_mul * sizeof(unsigned long));
  for (int i = 0; i < write_count; i=i+1) {
    long base = accum_mul*(i+1);
    for (int j = 0; j < accum_mul; j++) {
      //printf("ACCUM: %llx\n", arr_accum[base-j-1]);
      printf("%llx\n", arr_accum[base-j-1]);
    }
  }
  //for (int i = 0; i < write_count; i=i+1) {
  //  unsigned long base = accum_mul*(i+1);
  //  for (int j = 0; j < accum_mul; j++) {
  //    printf("ACCUM: %llx\n", read_reg( (unsigned long)(accum_cap->data_base) + base-j-1 ));
  //  }
  //}

}
