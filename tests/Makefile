GCC=riscv64-unknown-elf-gcc
CFLAGS=-mcmodel=medany -std=gnu99 -O2 -fno-common -fno-builtin-printf
LDFLAGS=-static -nostdlib -nostartfiles -lgcc

default: memcpy-dma.riscv pwm.riscv noop.riscv fft.riscv adc.riscv pg_prog.riscv memcpy-test.riscv

%.o: %.S
	$(GCC) $(CFLAGS) -D__ASSEMBLY__=1 -c $< -o $@

%.o: %.c
	$(GCC) $(CFLAGS) -c $< -o $@

%.riscv: %.o crt.o syscalls.o memcpy-dma.o
	$(GCC) -T link.ld $(LDFLAGS) $^ -o $@

clean:
	rm -f *.riscv *.o
