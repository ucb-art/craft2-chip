#define FFT_BASE 0x2000

static inline void write_reg(unsigned long addr, unsigned long data)
{
	volatile unsigned long *ptr = (volatile unsigned long *) addr;
	*ptr = data;
}

static inline unsigned long read_reg(unsigned long addr)
{
	volatile unsigned long *ptr = (volatile unsigned long *) addr;
	return *ptr;
}

void run_fft(double *bufin, double *bufout, int n) {
    int i;
    for (i=0; i < 2 * n; i++) {
        long in = *(unsigned long *)&(bufin[i]);
        write_reg(FFT_BASE + i * 8, in);
    }
    for (i=0; i < 2 * n; i++) {
        i[bufout] = read_reg(FFT_BASE + i * 8);
    }
}

int main(void)
{
    int i;
    double in[8] = {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    double out[8];

    run_fft(in, out, 4);

    /* for (i=0; i < 2 * 4; i++) {
        printf("%f\t", out[i]);
    } */
}
