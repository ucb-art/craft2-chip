base_dir = $(abspath .)

include $(base_dir)/Makefrag

clean:
	rm -rf $(lib_dir) $(ivy_dir)/local $(build_dir)
