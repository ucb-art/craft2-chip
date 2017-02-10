export RISCV=`pwd`/install
export PATH=~rigge/gcc/bin:$RISCV/bin:$PATH


export LD_LIBRARY_PATH=~rigge/gcc/lib64:~rigge/gcc/lib:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=$RISCV/lib64:$RISCV/lib:$LD_LIBRARY_PATH

# synopsys vcs, also for dve
export PATH=/tools/synopsys/vcs/G-2012.09/bin:$PATH
export VCS_HOME=/tools/synopsys/vcs/G-2012.09/
export VCS_64=1

# memory compiler
export INTERRAD_LICENSE_FILE=/tools/commercial/interra/flexlm/license_N28.dat
export TSMCHOME=/tools/tstech16/CLN16FFC/TSMCHOME

# temporary, hopefully
export MC2_INSTALL_DIR=~stevo.bailey/mc2/MC2_2013.12.00.f

export PATH=$PATH:$MC2_INSTALL_DIR/bin
