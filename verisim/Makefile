base_dir=$(abspath ..)
sim_dir=$(abspath .)

default: sim

CXXFLAGS := $(CXXFLAGS) -O1 -std=c++11 -I$(RISCV)/include
LDFLAGS := $(LDFLAGS) -L$(RISCV)/lib -Wl,-rpath,$(RISCV)/lib -L$(sim_dir) -lfesvr -lpthread

include $(base_dir)/Makefrag
include $(sim_dir)/Makefrag-verilator

simulator = simulator-$(PROJECT)-$(CONFIG)
simulator_debug = simulator-$(PROJECT)-$(CONFIG)-debug

sim: $(simulator)
debug: $(simulator_debug)

# TODO: make this not a two-step process
mem_vsrcs = $(shell find $(build_dir)/*/VERILOG/*.v | grep -v pwr)

sim_vsrcs = \
	$(build_dir)/$(long_name).top.v \
	$(build_dir)/$(long_name).harness.v \
	$(build_dir)/$(long_name).mems.v \
	$(base_dir)/vsrc/SimSerial.v \
	$(base_dir)/vsrc/des72to288.v \
	$(base_dir)/vsrc/AsyncResetReg.v \
	$(base_dir)/vsrc/TISARADC.v \
	$(base_dir)/vsrc/CLKRX.v \
	$(mem_vsrcs)

sim_csrcs = \
	$(base_dir)/csrc/SimSerial.cc \
	$(base_dir)/csrc/verilator-harness.cc

model_dir = $(build_dir)/$(long_name)
model_dir_debug = $(build_dir)/$(long_name).debug

model_header = $(model_dir)/V$(MODEL).h
model_header_debug = $(model_dir_debug)/V$(MODEL).h

# [stevo]:
# Wno-fatal means don't die when you get warnings, which we get from importing TSMC memories
# rest of these defines are for TSMC memories (see their verilog for details)
CUSTOM_FLAGS := \
	-Wno-fatal \
  +define+CLOCK_PERIOD=$(clock_period) \
	+define+TSMC_CM_UNIT_DELAY \
	+define+TSMC_CM_NO_WARNING \
	+define+TSMC_NO_TESTPINS_DEFAULT_VALUE_CHECK


$(simulator): $(sim_vsrcs) $(sim_csrcs) $(INSTALLED_VERILATOR)
	mkdir -p $(build_dir)/$(long_name)
	$(VERILATOR) $(VERILATOR_FLAGS) $(CUSTOM_FLAGS) -Mdir $(build_dir)/$(long_name) \
	-o $(sim_dir)/$@ $(sim_vsrcs) $(sim_csrcs) -LDFLAGS "$(LDFLAGS)" \
	-CFLAGS "-I$(build_dir) -include $(model_header)"
	$(MAKE) VM_PARALLEL_BUILDS=1 -C $(build_dir)/$(long_name) -f V$(MODEL).mk

$(simulator_debug): $(sim_vsrcs) $(sim_csrcs) $(INSTALLED_VERILATOR)
	mkdir -p $(build_dir)/$(long_name).debug
	$(VERILATOR) $(VERILATOR_FLAGS) $(CUSTOM_FLAGS) -Mdir $(build_dir)/$(long_name).debug --trace \
	-o $(sim_dir)/$@ $(sim_vsrcs) $(sim_csrcs) -LDFLAGS "$(LDFLAGS)" \
	-CFLAGS "-I$(build_dir) -include $(model_header_debug) -DVM_TRACE=1"
	$(MAKE) VM_PARALLEL_BUILDS=1 -C $(build_dir)/$(long_name).debug -f V$(MODEL).mk

clean:
	rm -rf $(build_dir) simulator-* verilator
