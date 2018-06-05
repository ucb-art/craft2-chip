cp generated-src/craft.TestHarness.AcmesFPGAConfig.v /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
cat generated-src/craft.TestHarness.AcmesFPGAConfig.mems.v >> /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
cat vsrc/AsyncResetReg.v >> /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
sed "s|Arbiter|StevoArbiter|g" -i /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
sed "s|Queue|StevoQueue|g" -i /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
sed "s|Async|StevoAsync|g" -i /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
sed "s|Nasti|StevoNasti|g" -i /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
sed "s|IdMapper|StevoIdMapper|g" -i /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
sed "s|SCRFile|StevoSCRFile|g" -i /tools/projects/stevo/acmes/acmes-fpga-testing/zc706/src/verilog/CraftP1Core.ZynqConfig.v
