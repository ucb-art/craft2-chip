export RISCV=/tools/projects/zhemao/craft2-chip/install
export PATH=$PATH:$RISCV/bin

export LD_LIBRARY_PATH=~rigge/gcc/lib64:~rigge/gcc/lib:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=$RISCV/lib64:$RISCV/lib:$LD_LIBRARY_PATH

# synopsys vcs, also for dve
export PATH=/tools/synopsys/vcs/J-2014.12-SP1/bin:$PATH
export VCS_HOME=/tools/synopsys/vcs/J-2014.12-SP1/
export VCS_64=1

# memory compiler
# Insert environment setup commands for memory compilers here

# cadence incisive
export PATH=/tools/cadence/INCISIV/INCISIVE152/tools/bin:$PATH

# cadence tools
export PATH=$PATH:/tools/cadence/GENUS/GENUS162/tools/bin:/tools/cadence/INNOVUS/INNOVUS162/tools/bin:/tools/cadence/INCISIV/INCISIVE152/tools/bin

# For RHEL6, the following provides a compatible toolchain 
scl enable devtoolset-2 bash

