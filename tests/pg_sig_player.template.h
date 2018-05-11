//========================================================================
// Program pattern generator to play sinusoid indefinitely
//========================================================================
#define CRAFT_DEBUG

#include <stdio.h>
#include <stdint.h>
#include "pg_prog.h"
#include "memcpy.h"

// This header file defines the API for interacting with a DSP Chain
#include "chain_api.h"

static void play_pg_sig()
{

  // initialize PG
  pg_init();

  // generated data goes here:

  // this gets put into the PG
  //pg_create_data(pg_mem, test_arr, samples);
  pg_load_data(0L, samples, test_arr);

  // play forever
  pg_play();
}
