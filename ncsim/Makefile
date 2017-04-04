base_dir=$(abspath ..)
sim_dir=$(abspath .)
output_dir=output

default: sim

include $(base_dir)/Makefrag

simv = $(sim_dir)/simv-$(PROJECT)-$(CONFIG)
simv_debug = $(sim_dir)/simv-$(PROJECT)-$(CONFIG)-debug

sim: $(simv)
debug: $(simv_debug)


sim_vsrcs = \
	$(build_dir)/$(long_name).v \
	$(build_dir)/$(long_name).harness.v \
	$(build_dir)/$(long_name).mems.v \
	$(build_dir)/$(long_name).top.v \
	$(build_dir)/$(long_name).pads.v \
	$(build_dir)/mems.behav.v \
	$(build_dir)/pads.behav.v \
	$(base_dir)/vsrc/TestDriver.v \
	$(base_dir)/vsrc/SimSerial.v \
	$(base_dir)/vsrc/des72to288.v \
	$(base_dir)/vsrc/CLKRX.v \
	$(base_dir)/vsrc/UART.v \
	$(base_dir)/vsrc/AsyncResetReg.v

sim_csrcs = \
	$(base_dir)/csrc/SimSerial.cc

# [stevo]: add adc model, but copy here because each one has "include verilog_header" in it
# assumes you're running this inside the sim_dir
sim_svsrcs_orig = $(wildcard $(base_dir)/vsrc/adc_model/*)
sim_svsrcs = $(addprefix $(build_dir)/,$(notdir $(sim_svsrcs_orig)))
$(sim_svsrcs): $(sim_svsrcs_orig)
	cp $(sim_svsrcs_orig) $(build_dir)
	sed -e 's*verilog_header.vh*$(build_dir)/verilog_header.vh*' -i $(foreach src,$(sim_svsrcs),$(addprefix -i ,$(src)))

CUSTOM_FLAGS := \
  +define+CORE_CLOCK_PERIOD=$(core_clock_period) \
  +define+SERIAL_CLOCK_PERIOD=$(serial_clock_period) \
  +define+DSP_CLOCK_PERIOD=$(dsp_clock_period) \
	+define+TSMC_CM_UNIT_DELAY \
	+define+TSMC_CM_UNIT_DELAY \
	+define+TSMC_INITIALIZE_MEM \

IRUN = irun -64

IRUN_OPTS = \
	+rad +v2k +vc+list \
	"-I$(RISCV)/include" \
	-Wld,-Xlinker,-rpath,$(RISCV)/lib \
	-Wcxx,-std=c++11 \
	-I$(sim_dir)/generated-src \
	$(RISCV)/lib/libfesvr.so \
	-sv \
	+incdir+$(sim_dir)/generated-src \
	$(sim_vsrcs) $(sim_csrcs) \
	$(filter-out %.vh,$(sim_svsrcs)) \
	+define+PRINTF_COND=$(TB).printf_cond \
	+define+STOP_COND=!$(TB).reset \
	+define+RANDOMIZE_MEM_INIT \
	+define+RANDOMIZE_REG_INIT \
	+define+RANDOMIZE_GARBAGE_ASSIGN \
	+define+RANDOMIZE_INVALID_ASSIGN \
	+libext+.v \
	+vpi \
	-DVCS_VPI \
	-timescale 1ns/10ps \
	-ccext .c \
	-access +R \
	$(CUSTOM_FLAGS)

$(simv): $(sim_vsrcs) $(sim_csrcs) $(sim_svsrcs) $(sim_dir)/Makefile
	rm -rf csrc && $(IRUN) $(IRUN_OPTS) -elaborate -nclibdirname $@.daidir
	rm -f $@
	touch $@
	@echo '#!/bin/bash' >> $@
	@echo '' >> $@
	@echo 'VARS=""' >> $@
	@echo 'DEFINE=""' >> $@
	@echo 'while [[ $$# -gt 0 ]]' >> $@
	@echo 'do' >> $@
	@echo '  arg="$$1"' >> $@
	@echo '  if [[ "$$arg" == +* ]] ; then' >> $@
	@echo '    VARS="$$VARS $$arg"' >> $@
	@echo '  else' >> $@
	@echo '    DEFINE="$$arg"' >> $@
	@echo '  fi' >> $@
	@echo '  shift' >> $@
	@echo 'done' >> $@
	@echo '' >> $@
	@echo 'echo irun -define "$$DEFINE" "$$VARS" -64 -R -nclibdirname $@.daidir' >> $@
	@echo 'irun -define "$$DEFINE" "$$VARS" -64 -R -nclibdirname $@.daidir' >> $@
	chmod +x $@
	cp $(build_dir)/*.cde .

$(simv_debug) : $(sim_vsrcs) $(sim_csrcs) $(sim_svsrcs) $(sim_dir)/Makefile
	rm -rf csrc && $(IRUN) $(IRUN_OPTS) +define+DEBUG -linedebug -elaborate -nclibdirname $@.daidir
	rm -f $@
	touch $@
	@echo '#!/bin/bash' >> $@
	@echo '' >> $@
	@echo 'VARS=""' >> $@
	@echo 'DEFINE=""' >> $@
	@echo 'while [[ $$# -gt 0 ]]' >> $@
	@echo 'do' >> $@
	@echo '  arg="$$1"' >> $@
	@echo '  if [[ "$$arg" == +* ]] ; then' >> $@
	@echo '    VARS="$$VARS $$arg"' >> $@
	@echo '  else' >> $@
	@echo '    DEFINE="$$arg"' >> $@
	@echo '  fi' >> $@
	@echo '  shift' >> $@
	@echo 'done' >> $@
	@echo '' >> $@
	@echo 'echo irun -define "$$DEFINE" "$$VARS" -64 -R -input probe.tcl -nclibdirname $@.daidir' >> $@
	@echo 'irun -define "$$DEFINE" "$$VARS" -64 -R -input probe.tcl -nclibdirname $@.daidir' >> $@
	chmod +x $@
	cp $(build_dir)/*.cde .

clean:
	rm -rf csrc simv-* ucli.key vc_hdrs.h $(build_dir) output DVEfiles $(sim_svsrcs) *.cde

.PHONY: clean


ifneq ($(filter run% %.run %.out %.vpd %.vcd,$(MAKECMDGOALS)),)
-include $(build_dir)/$(long_name).d
endif

#--------------------------------------------------------------------
# Run
#--------------------------------------------------------------------

seed = $(shell date +%s)
exec_simv = $(simv)
exec_simv_debug = $(simv_debug)

$(build_dir)/consts.$(CONFIG).vh: $(build_dir)/$(long_name).prm
	echo "\`ifndef CONST_VH" > $@
	echo "\`define CONST_VH" >> $@
	sed -r 's/\(([A-Za-z0-9_]+),([A-Za-z0-9_]+)\)/`define \1 \2/' $(patsubst %.v,%.prm,$<) >> $@
	echo "\`endif // CONST_VH" >> $@


$(output_dir)/%.run: $(output_dir)/% $(simv)
	cd $(sim_dir) && $(exec_simv) +max-cycles=$(timeout_cycles) $< 2> /dev/null 2> $@ && [ $$PIPESTATUS -eq 0 ]

$(output_dir)/%.out: $(output_dir)/% $(simv)
	cd $(sim_dir) && $(exec_simv) +verbose +max-cycles=$(timeout_cycles) $< $(disasm) $@ && [ $$PIPESTATUS -eq 0 ]

$(output_dir)/%.vcd: $(output_dir)/% $(simv_debug)
	cd $(sim_dir) && $(exec_simv_debug) +verbose +vcdfile=$@ +max-cycles=$(timeout_cycles) $< $(disasm) $(patsubst %.vcd,%.out,$@) && [ $$PIPESTATUS -eq 0 ]

$(output_dir)/%.vpd: $(output_dir)/% $(simv_debug)
	cd $(sim_dir) && $(exec_simv_debug) +verbose +vcdplusfile=$@ +max-cycles=$(timeout_cycles) $< $(disasm) $(patsubst %.vpd,%.out,$@) && [ $$PIPESTATUS -eq 0 ]

$(output_dir)/%.saif: $(output_dir)/% $(simv_debug)
	cd $(sim_dir) && rm -f $(output_dir)/pipe-$*.vcd && vcd2saif -input $(output_dir)/pipe-$*.vcd -pipe "$(exec_simv_debug) +verbose +vcdfile=$(output_dir)/pipe-$*.vcd +max-cycles=$(bmark_timeout_cycles) $<" -output $@ > $(patsubst %.saif,%.out,$@) 2>&1

run: run-asm-tests run-bmark-tests
run-debug: run-asm-tests-debug run-bmark-tests-debug
run-fast: run-asm-tests-fast run-bmark-tests-fast

.PHONY: run-asm-tests run-bmark-tests
.PHONY: run-asm-tests-debug run-bmark-tests-debug
.PHONY: run run-debug run-fast

junk += $(output_dir)
