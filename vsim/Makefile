base_dir=$(abspath ..)
sim_dir=$(abspath .)

default: sim

include $(base_dir)/Makefrag

simv = simv-$(PROJECT)-$(CONFIG)
simv_debug = simv-$(PROJECT)-$(CONFIG)-debug

sim: $(simv)
debug: $(simv_debug)

# TODO: make this not a two-step process
mem_vsrcs = $(shell find $(build_dir)/*/VERILOG/*.v | grep -v pwr)

sim_vsrcs = \
	$(build_dir)/$(long_name).top.v \
	$(build_dir)/$(long_name).harness.v \
	$(build_dir)/$(long_name).mems.v \
	$(base_dir)/vsrc/TestDriver.v \
	$(base_dir)/vsrc/SimSerial.v \
	$(base_dir)/vsrc/des72to288.v \
	$(base_dir)/vsrc/TISARADC.v \
	$(base_dir)/vsrc/CLKRX.v \
	$(base_dir)/vsrc/AsyncResetReg.v \
	$(mem_vsrcs)

sim_csrcs = \
	$(base_dir)/csrc/SimSerial.cc


CUSTOM_FLAGS := \
  +define+CLOCK_PERIOD=$(clock_period) \
	+define+TSMC_CM_UNIT_DELAY \
	+define+TSMC_CM_NO_WARNING \
	+define+TSMC_NO_TESTPINS_DEFAULT_VALUE_CHECK


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
	$(CUSTOM_FLAGS)

verilog: $(sim_vsrcs)

$(simv): $(sim_vsrcs) $(sim_csrcs)
	rm -rf csrc && $(VCS) $(VCS_OPTS) -o $@ \
	-debug_pp

$(simv_debug) : $(sim_vsrcs) $(sim_csrcs)
	rm -rf csrc && $(VCS) $(VCS_OPTS) -o $@ \
	+define+DEBUG -debug_pp

clean:
	rm -rf csrc simv-* ucli.key vc_hdrs.h $(build_dir) output DVEfiles

.PHONY: clean
