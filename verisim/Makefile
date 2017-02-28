base_dir=$(abspath ..)
sim_dir=$(abspath .)
framework_dir=$(base_dir)/dsp-framework

PROJECT ?= craft
MODEL ?= TestHarness
CONFIG ?= Craft2Config
CFG_PROJECT ?= $(PROJECT)
TB ?= TestDriver

sim = simulator-$(PROJECT)-$(CONFIG)
sim_debug = simulator-$(PROJECT)-$(CONFIG)-debug

default: $(sim)

debug: $(sim_debug)

CXXFLAGS := $(CXXFLAGS) -O1 -std=c++11 -I$(RISCV)/include
LDFLAGS := $(LDFLAGS) -L$(RISCV)/lib -Wl,-rpath,$(RISCV)/lib -L$(sim_dir) -lfesvr -lpthread

include $(base_dir)/Makefrag
include $(sim_dir)/Makefrag-verilator

long_name = $(PROJECT).$(MODEL).$(CONFIG)

sim_vsrcs = \
	$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).v \
	$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).mems.v \
	$(base_dir)/vsrc/TestDriver.v \
	$(base_dir)/vsrc/SimSerial.v \
	$(base_dir)/vsrc/des72to288.v \
	$(base_dir)/vsrc/TISARADC.v \
	$(base_dir)/vsrc/AsyncResetReg.v

sim_csrcs = \
	$(base_dir)/csrc/SimSerial.cc \
	$(base_dir)/csrc/verilator-harness.cc

$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).fir: $(call lookup_scala_srcs, $(base_dir)/src) $(top_all_stamps)
	mkdir -p $(build_dir)
	cd $(base_dir) && $(SBT) "run-main $(PROJECT).DspGenerator $(CHISEL_ARGS) $(build_dir) $(PROJECT) $(MODEL) $(CFG_PROJECT) $(CONFIG)"

$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).v: $(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).fir
	$(FIRRTL) -i $< -o $@ -X verilog

$(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).mems.v: $(build_dir)/$(PROJECT).$(MODEL).$(CONFIG).v
	cd $(build_dir) && $(MEM_GEN) --conf $(PROJECT).$(MODEL).$(CONFIG).conf --v $(PROJECT).$(MODEL).$(CONFIG).mems.v --ff

model_dir = $(build_dir)/$(long_name)
model_dir_debug = $(build_dir)/$(long_name).debug

model_header = $(model_dir)/V$(MODEL).h
model_header_debug = $(model_dir_debug)/V$(MODEL).h

$(sim): $(sim_vsrcs) $(sim_csrcs) $(INSTALLED_VERILATOR)
	mkdir -p $(build_dir)/$(long_name)
	$(VERILATOR) $(VERILATOR_FLAGS) -Mdir $(build_dir)/$(long_name) \
	-o $(sim_dir)/$@ $< $(sim_vsrcs) $(sim_csrcs) -LDFLAGS "$(LDFLAGS)" \
	-CFLAGS "-I$(build_dir) -include $(model_header)"
	$(MAKE) VM_PARALLEL_BUILDS=1 -C $(build_dir)/$(long_name) -f V$(MODEL).mk

$(sim_debug): $(sim_vsrcs) $(sim_csrcs) $(INSTALLED_VERILATOR)
	mkdir -p $(build_dir)/$(long_name).debug
	$(VERILATOR) $(VERILATOR_FLAGS) -Mdir $(build_dir)/$(long_name).debug --trace \
	-o $(sim_dir)/$@ $< $(sim_vsrcs) $(sim_csrcs) -LDFLAGS "$(LDFLAGS)" \
	-CFLAGS "-I$(build_dir) -include $(model_header_debug) -DVM_TRACE=1"
	$(MAKE) VM_PARALLEL_BUILDS=1 -C $(build_dir)/$(long_name).debug -f V$(MODEL).mk
