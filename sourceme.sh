export RISCV=`pwd`/install
export PATH=~rigge/gcc/bin:$RISCV/bin:$PATH


export LD_LIBRARY_PATH=~rigge/gcc/lib64:~rigge/gcc/lib:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=$RISCV/lib64:$RISCV/lib:$LD_LIBRARY_PATH

# synopsys vcs, also for dve
export PATH=/tools/synopsys/vcs/G-2012.09/bin:$PATH
export VCS_HOME=/tools/synopsys/vcs/G-2012.09/
export VCS_64=1
