#include <stdio.h>
#include <stdint.h>
#include "memcpy.h"

#define MAX_SIZE 128

int main(void)
{

  uint64_t src[MAX_SIZE];
  uint64_t dest[MAX_SIZE];

  unsigned long i;
  for (i=0L; i<MAX_SIZE; i++) {
    src[i] = 2*i-1;
  }

  test_memcpy(src, (unsigned long*)dest, MAX_SIZE * sizeof(uint64_t));

  /*for (i=0L; i<write_count * 8; i++) {
    printf("arr[%d] = 0x%llx\n", i, arr[i]);
  }*/
  

  return 0;
}

