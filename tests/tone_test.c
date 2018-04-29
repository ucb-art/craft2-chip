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


void parse_sam_output(void *sam_output, uint64_t num_writes, uint64_t bitwidth, uint64_t lanes, uint64_t mul, int64_t *result) {

  uint64_t *buf = (uint64_t*)sam_output;

  uint64_t mask = 0;
  if (bitwidth < 64) {
    mask = (1L << bitwidth) - 1L;
  } else if (bitwidth == 64) {
    mask = 0xFFFFFFFFFFFFFFFFL; 
  } else {
    printf("Error: cannot parse bitwidth greater than 64");
  }

  for (int write_cnt = 0; write_cnt < num_writes; write_cnt++) {
    long base = mul*(write_cnt+1); 
    for (int lane = 0; lane < lanes; lane++) {

      // calculate start and end array locations
      long start = lane * bitwidth;
      int start_off = 0;
      while (start >= 64) {
        start = start - 64;
        start_off = start_off + 1;
      }
      long end = (lane+1)*bitwidth-1;
      int end_off = 0;
      while (end >= 64) {
        end = end - 64;
        end_off = end_off + 1;
      }

      // if they're in the same word, it's easy, otherwise
      // need to combine subsets of two words
      uint64_t base_res = 0;
      if (end_off == start_off) {
        base_res = (buf[base-start_off-1] >> start) & mask;
      } else {
        uint64_t first = buf[base-start_off-1] >> start;
        long second_shift = 64 - start;
        long second_mask = mask >> second_shift;
        uint64_t second = (buf[base-end_off-1] & second_mask) << second_shift;
        base_res = first + second;
      }

      if (base_res >= (1 << (bitwidth-1))) {
        result[lane+write_cnt*lanes] = ((int64_t)base_res) - ((int64_t)(1 << bitwidth));
      } else {
        result[lane+write_cnt*lanes] = (int64_t) base_res;
      }
    }
  }
}



int main(void) {

  // should be multiple of 256 to get full spectrum if 8192
  int samples = 256;

  // TODO: get this from IP-XACT or somewhere 
  int lanes = 32;

  // go!
  //printf("\nConfig ADC\n");
  write_reg(acmes_VREF1, 48);
  write_reg(acmes_VREF2, 96);
  //printf("Configuring ADC Calibration\n");
  //printf("Done config ADC\nConfig SAM\n");

  // // BM1 - initiate SAM
  // sam_capture* bm1_cap = &acmes_bm1_sam_capture;
  // unsigned long bm1_mul = (bm1_cap->pow2_width)/sizeof(unsigned long)/8;
  // uint64_t bm1_sam_output_buffer[bm1_mul*samples];
  // bm1_cap->output = bm1_sam_output_buffer;
  // bm1_cap->output_size = sizeof(bm1_sam_output_buffer);
  // bm1_cap->n_samps    = samples;
  // bm1_cap->start_addr = 0;
  // bm1_cap->wait_for_sync = 1;
  // bm1_cap->use_dma = 0;
  // //printf("Done config SAM\nInit SAM capture\n");
  // initiate_sam_capture(bm1_cap);
  // write_reg(acmes_ADC_VALID, 1);

  // // TODO: get this from IP-XACT or somewhere
  // int bm1_total_bits = 9;

  // // read out bm1 results
  // unsigned long write_count = 0;
  // //printf("Wait for SAM to fill\n");
  // while (write_count < bm1_cap->n_samps) {
  //   write_count  = read_reg(bm1_cap->ctrl_base + SAM_W_WRITE_COUNT_OFFSET);
  // }
  // // write_count should be samples (4)
  // //printf("write count = %llx, bm1_mul = %llx\n", write_count, bm1_mul);
  // memcpy(bm1_sam_output_buffer, (unsigned long*)(bm1_cap->data_base), write_count * bm1_mul * sizeof(unsigned long));
  // long bm1_samples = samples*lanes;
  // int64_t bm1_result[bm1_samples];
  // printf("Parsing output\n");
  // parse_sam_output(bm1_sam_output_buffer, bm1_cap->n_samps, bm1_total_bits, lanes, bm1_mul, bm1_result);
  // printf("Printing output\n");
  // for (int i = 0; i < bm1_samples; i++) {
  //   printf("BM: %lld\n", bm1_result[i]);
  // }

  // // PFB - initiate SAM
  // sam_capture* pfb_cap = &acmes_pfb_sam_capture;
  // unsigned long pfb_mul = (pfb_cap->pow2_width)/sizeof(unsigned long)/8;
  // uint64_t pfb_sam_output_buffer[pfb_mul*samples];
  // pfb_cap->output = pfb_sam_output_buffer;
  // pfb_cap->output_size = sizeof(pfb_sam_output_buffer);
  // pfb_cap->n_samps    = samples;
  // pfb_cap->start_addr = 0;
  // pfb_cap->wait_for_sync = 1;
  // pfb_cap->use_dma = 0;
  // //printf("Done config SAM\nInit SAM capture\n");
  // initiate_sam_capture(pfb_cap);
  // write_reg(acmes_ADC_VALID, 1);

  // // TODO: get this from IP-XACT or somewhere
  // int pfb_total_bits = 11;

  // // read out pfb results
  // unsigned long write_count = 0;
  // //printf("Wait for SAM to fill\n");
  // while (write_count < pfb_cap->n_samps) {
  //   write_count  = read_reg(pfb_cap->ctrl_base + SAM_W_WRITE_COUNT_OFFSET);
  // }
  // // write_count should be samples (4)
  // //printf("write count = %llx, pfb_mul = %llx\n", write_count, pfb_mul);
  // memcpy(pfb_sam_output_buffer, (unsigned long*)(pfb_cap->data_base), write_count * pfb_mul * sizeof(unsigned long));
  // long pfb_samples = samples*lanes;
  // int64_t pfb_result[pfb_samples];
  // printf("Parsing output\n");
  // parse_sam_output(pfb_sam_output_buffer, pfb_cap->n_samps, pfb_total_bits, lanes, pfb_mul, pfb_result);
  // printf("Printing output\n");
  // for (int i = 0; i < pfb_samples; i++) {
  //   printf("PFB: %lld\n", pfb_result[i]);
  // }

  // // power - initiate SAM
  // sam_capture* power_cap = &acmes_power_sam_capture;
  // unsigned long power_mul = (power_cap->pow2_width)/sizeof(unsigned long)/8;
  // uint64_t power_sam_output_buffer[power_mul*samples];
  // power_cap->output = power_sam_output_buffer;
  // power_cap->output_size = sizeof(power_sam_output_buffer);
  // power_cap->n_samps    = samples;
  // power_cap->start_addr = 0;
  // power_cap->wait_for_sync = 1;
  // power_cap->use_dma = 0;
  // //printf("Done config SAM\nInit SAM capture\n");
  // initiate_sam_capture(power_cap);
  // write_reg(acmes_ADC_VALID, 1);

  // // TODO: get this from IP-XACT or somewhere
  // int power_total_bits = 32;

  // // read out power results
  // unsigned long write_count = 0;
  // // printf("Wait for SAM to fill\n");
  // while (write_count < power_cap->n_samps) {
  //   write_count  = read_reg(power_cap->ctrl_base + SAM_W_WRITE_COUNT_OFFSET);
  // }
  // // write_count should be samples (4)
  // // accum_mul should be 2048/64 = 32 
  // //get_sam_output(accum_cap); 
  // //printf("write count = %llx, power_mul = %llx\n", write_count, power_mul);
  // memcpy(power_sam_output_buffer, (unsigned long*)(power_cap->data_base), write_count * power_mul * sizeof(unsigned long));
  // long power_samples = samples*lanes;
  // int64_t power_result[power_samples];
  // printf("Parsing output\n");
  // parse_sam_output(power_sam_output_buffer, power_cap->n_samps, power_total_bits, lanes, power_mul, power_result);
  // printf("Printing output\n");
  // for (int i = 0; i < power_samples; i++) {
  //   printf("POW: %lld\n", power_result[i]);
  // }

  // accum - initiate SAM
  write_reg(acmes_accum_NumSpectraToAccumulate, 1);
  sam_capture* accum_cap = &acmes_accum_sam_capture;
  unsigned long accum_mul = (accum_cap->pow2_width)/sizeof(uint64_t)/8;
  uint64_t accum_sam_output_buffer[accum_mul*samples];
  accum_cap->output = accum_sam_output_buffer;
  accum_cap->output_size = sizeof(accum_sam_output_buffer);
  accum_cap->n_samps    = samples;
  accum_cap->start_addr = 0;
  accum_cap->wait_for_sync = 1;
  accum_cap->use_dma = 0;
  //printf("Done config SAM\nInit SAM capture\n");
  initiate_sam_capture(accum_cap);
  write_reg(acmes_ADC_VALID, 1);

  // TODO: get this from IP-XACT or somewhere
  int accum_total_bits = 64;

  // read out accum results
  unsigned long write_count = 0;
  // printf("Wait for SAM to fill\n");
  while (write_count < accum_cap->n_samps) {
    write_count  = read_reg(accum_cap->ctrl_base + SAM_W_WRITE_COUNT_OFFSET);
  }
  memcpy(accum_sam_output_buffer, (unsigned long*)(accum_cap->data_base), write_count * accum_mul * sizeof(unsigned long));
  long accum_samples = samples*lanes;
  int64_t accum_result[accum_samples];
  printf("Parsing output\n");
  parse_sam_output(accum_sam_output_buffer, accum_cap->n_samps, accum_total_bits, lanes, accum_mul, accum_result);
  printf("Printing output\n");
  for (int i = 0; i < accum_samples; i++) {
    printf("ACCUM: %lld\n", accum_result[i]);
  }
}
