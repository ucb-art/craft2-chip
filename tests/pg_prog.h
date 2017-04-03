
#ifndef __PG_PROG_H
#define __PG_PROG_H

#include "chain_api.h"  // Get craft address

#define PGWriteData_BASE craft_radar_patternGeneratorWriteData_0

//Set up PG controls
static void pg_init() {
   int i, j;
   
   printf("Loading PG memory \n");

   write_reg_debug(
           craft_radar_patternGeneratorEnable, 0,
           "patternGeneratorEnable"
           );

   write_reg_debug(
           craft_radar_patternGeneratorControlEnable, 0,
           "patternGeneratorControlEnable"
           );

   /*
   for (j = 0; j < 8; j++) {  //There are 8 PG mem data
     write_reg_debug(PGWriteData_BASE + j*8, 0, "PG_WriteData");
   }
   */
   
   printf("Setting PG control defaults\n");
   write_reg_debug(
           craft_radar_patternGeneratorReadyBypass, 0,
           "PGReadyBypass"
           );
   write_reg_debug(
           craft_radar_patternGeneratorContinuous, 1,
           "PGContinuous"
           );

   write_reg_debug(
           craft_radar_patternGeneratorTriggerMode, 0,
           "PGTriggerMode"
           );

   write_reg_debug(
           craft_radar_patternGeneratorArm, 0,
           "PGArm"
           );

   write_reg_debug(
           craft_radar_patternGeneratorAbort, 1,
           "PGAbort"
           );
  if (!decoupledHelper(
              craft_radar_patternGeneratorControlEnable,
              craft_radar_patternGeneratorControlFinished,
              "patternGeneratorControl"
              )) {
      exit(-INITIATE_SAM_CAPTURE_ERROR);
  }

   write_reg_debug(
           craft_radar_patternGeneratorAbort, 0,
           "PGAbort"
           );

   /*if (!decoupledHelper(
               craft_radar_patternGeneratorControlEnable,
               craft_radar_patternGeneratorControlFinished,
               "patternGeneratorControl"
               )) {
       exit(-INITIATE_SAM_CAPTURE_ERROR);
   }*/

   printf("Done with PG init\n");
}

static void pg_create_data(unsigned long *arr, unsigned long *test, int sz) {
  int i, j;
  for (i = 0; i < sz; i++) {
    arr[i] = test[i];  //assign test data
  }
}

static void pg_load_data(unsigned long start_addr, int cnt) {
  int i, j;
  unsigned int sample_cnt    = 0;
  unsigned int idx           = 0;  //test data
  unsigned int WriteFinished = 0;

  unsigned long *arr = pg_mem;//vector file from fft_input_vector.h

  //Calculate the sample count. Count is multiple 8 words of 64 bits each = 512
  //sample_count = cnt/8
  // if (cnt Mod 8 ) > 1 
  // sample_count = sample_count + 1;
  if (cnt % 8 == 0) {
    sample_cnt = cnt/8;
  }
  else {
    sample_cnt = cnt/8 + 1;
  }

  /*
  printf("Test vector sample count = %d \n", sample_cnt);
  for (i = 0; i < 16; i++) {
    printf("Test data pg_mem[%0d] = 0x%llx\n",i, arr[i]);
  }
  */

  //BOZO: patternGeneratorWriteEnable needs to be 0 before trying to write new address
  write_reg_debug(
          craft_radar_patternGeneratorWriteEnable, 0,
          "patternGeneratorWriteEnable"
          );

  idx = 0;
  for (i = 0; i < sample_cnt; i++) {  //There are 8 PG mem data
    write_reg_debug(
            craft_radar_patternGeneratorWriteAddr, i,
            "indexr for each 512 word at PGWriteAddr"
            );

    for (j = 0; j < 8; j++) {  //There are 8 PG mem data
      write_reg_debug(
              PGWriteData_BASE + j*8, arr[idx],
              "PG_WriteData"
              );
      idx++;
    }
    
    if (!decoupledHelper(
            craft_radar_patternGeneratorWriteEnable,
            craft_radar_patternGeneratorWriteFinished,
            "patternGeneratorWrite"
            )) {
      exit(-INITIATE_SAM_CAPTURE_ERROR);
    }
  }

  write_reg_debug(
          craft_radar_patternGeneratorLastSample, sample_cnt-1,
          "patternGeneratorLastSample");
  if (!decoupledHelper(
              craft_radar_patternGeneratorControlEnable,
              craft_radar_patternGeneratorControlFinished,
              "patternGeneratorControl"
              )) {
      exit(-INITIATE_SAM_CAPTURE_ERROR);
  }

  printf("Done with PG load data\n"); 
}

static void pg_play() {
  //ARM the PG here??
  printf("ARMING patternGeneratorArm\n");
  write_reg(craft_radar_patternGeneratorArm, 1);

  if (!decoupledHelper(
              craft_radar_patternGeneratorControlEnable,
              craft_radar_patternGeneratorControlFinished,
              "patternGeneratorControl"
              )) {
      exit(-INITIATE_SAM_CAPTURE_ERROR);
  }

  write_reg_debug(
          craft_radar_patternGeneratorSelect, 0,
          "patternGeneratorSelect"
          );//tuner=0


  write_reg_debug(
          craft_radar_patternGeneratorEnable, 1,
          "patternGeneratorEnable"
          );
}

static void pg_end() {
   write_reg_debug(
           craft_radar_patternGeneratorEnable, 0,
           "patternGeneratorEnable"
           );

  write_reg_debug(craft_radar_patternGeneratorArm, 0, "patternGeneratorArm");
  write_reg_debug(craft_radar_patternGeneratorAbort, 1, "patternGeneratorAbort");

  if (!decoupledHelper(
              craft_radar_patternGeneratorControlEnable,
              craft_radar_patternGeneratorControlFinished,
              "patternGeneratorControl"
              )) {
      exit(-INITIATE_SAM_CAPTURE_ERROR);
  }
}

#endif /* __PG_PROG_H */
