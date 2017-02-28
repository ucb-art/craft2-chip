base_dir=$(abspath ..)
sim_dir=$(abspath .)

PROJECT ?= craft
MODEL ?= TestHarness
CONFIG ?= Craft2Config
CFG_PROJECT ?= $(PROJECT)
TB ?= TestDriver

simv = simv-$(PROJECT)-$(CONFIG)
simv_debug = simv-$(PROJECT)-$(CONFIG)-debug

default: $(simv)

debug: $(simv_debug)

include $(base_dir)/Makefrag

sim_vsrcs = \
	$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).v \
	$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).mems.v \
	$(base_dir)/vsrc/TestDriver.v \
	$(base_dir)/vsrc/SimSerial.v \
	$(base_dir)/vsrc/des72to288.v \
	$(base_dir)/vsrc/TISARADC.v \
	$(base_dir)/vsrc/AsyncResetReg.v

sim_csrcs = \
	$(base_dir)/csrc/SimSerial.cc

$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).fir: $(call lookup_scala_srcs, $(base_dir)/src) $(top_all_stamps)
	mkdir -p $(build_dir)
	cd $(base_dir) && $(SBT) "run-main $(PROJECT).DspGenerator $(CHISEL_ARGS) $(build_dir) $(PROJECT) $(MODEL) $(CFG_PROJECT) $(CONFIG)"

$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).v: $(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).fir
	$(FIRRTL) -i $< -o $@ -X verilog -firw $(MODEL) -frsq -c:$(MODEL):-o:$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).conf

$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).mems.v: $(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).v
	cd $(build_dir) && $(MEM_GEN) --conf $(PROJECT).$(MODEL).$(CONFIG).conf --v $(PROJECT).$(MODEL).$(CONFIG).mems.v --ff



VCS = vcs -full64

VCS_OPTS = -notice -line +lint=all,noVCDE,noONGS,noUI -error=PCWM-L -timescale=1ns/10ps -quiet \
	+rad +v2k +vcs+lic+wait \
	+vc+list -CC "-I$(VCS_HOME)/include" \
	-CC "-I$(RISCV)/include" \
	-CC "-std=c++11" \
	-CC "-Wl,-rpath,$(RISCV)/lib" \
	$(RISCV)/lib/libfesvr.so \
	-sverilog \
	+incdir+$(generated_dir) \
	+define+CLOCK_PERIOD=1.0 $(sim_vsrcs) $(sim_csrcs) \
	+define+PRINTF_COND=$(TB).printf_cond \
	+define+STOP_COND=!$(TB).reset \
	+define+RANDOMIZE_MEM_INIT \
	+define+RANDOMIZE_REG_INIT \
	+define+RANDOMIZE_GARBAGE_ASSIGN \
	+define+RANDOMIZE_INVALID_ASSIGN \
	+libext+.v \

verilog: $(sim_vsrcs)

$(simv): $(sim_vsrcs) $(sim_csrcs)
	rm -rf csrc && $(VCS) $(VCS_OPTS) -o $@ \
	-debug_pp

$(simv_debug) : $(sim_vsrcs) $(sim_csrcs)
	rm -rf csrc && $(VCS) $(VCS_OPTS) -o $@ \
	+define+DEBUG -debug_pp

clean:
	rm -rf csrc simv-* ucli.key vc_hdrs.h generated-src output

.PHONY: clean
