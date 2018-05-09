base_dir = $(abspath .)

default: top

include $(base_dir)/Makefrag

clean:
	rm -rf $(lib_dir) $(ivy_dir)/local $(build_dir)
