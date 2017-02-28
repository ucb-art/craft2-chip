framework_dir = $(abspath ./dsp-framework)
base_dir = $(abspath .)

include $(base_dir)/Makefrag


$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).fir: $(call lookup_scala_srcs, $(base_dir)/src) $(top_all_stamps)
	mkdir -p $(build_dir)
	cd $(base_dir) && $(SBT) "run-main $(PROJECT).DspGenerator $(CHISEL_ARGS) $(build_dir) $(PROJECT) $(MODEL) $(CFG_PROJECT) $(CONFIG)"

$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).v: $(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).fir
	$(FIRRTL) -i $< -o $@ -X verilog -firw $(MODEL) -frsq -c:$(MODEL):-o:$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).conf

$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).mems.v: $(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).v
	cd $(build_dir) && $(MEM_GEN) --conf $(PROJECT).$(MODEL).$(CONFIG).conf --v $(PROJECT).$(MODEL).$(CONFIG).mems.v --generate

