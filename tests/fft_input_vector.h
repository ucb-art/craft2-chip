
unsigned long my_arr2[10] = {10L, 0x2222222222222222, 0x3333333333333333, 40};

//PG can hold 256 samples. Each sample is 8 words wide (8*64=512bits).
// 256 * 512 = 131072  bits = 16384 bytes
// PG arrar size = 256 * 8 = 2048

#define PG_NUM_WORDS 2048
unsigned long pg_mem[PG_NUM_WORDS]; //global and static vars are automatically init to zero

#define MY_SIZE 11
unsigned long test_arr[MY_SIZE] = {0L,
				    0x1111111111111111, 
				    0x2222222222222222, 
				    0x3333333333333333, 
				    0x4444444444444444,
				    0x5555555555555555,
				    0x6666666666666666,
				    0x7777777777777777,
				    0x8888888888888888,
				    0x9999999999999999,
				    0xaaaaaaaaaaaaaaaa };


