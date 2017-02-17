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
	#$(build_dir)/$(long_name).v \
	$(sim_dir)/BlackBoxFloat.v \
	$(TESTCHIPIP_DIR)/vsrc/SimSerial.v \
	$(ROCKETCHIP_DIR)/vsrc/AsyncResetReg.v

sim_csrcs = \
	$(TESTCHIPIP_DIR)/csrc/SimSerial.cc \
	$(TESTCHIPIP_DIR)/csrc/verilator-harness.cc

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
