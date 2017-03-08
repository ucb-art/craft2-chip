# Craft2 Chip

This repo contains the infrastructure for designing, testing, and building the craft2 chip.

## Overview

Craft2 is a [RISC-V](https://riscv.org/) [Rocket](https://github.com/ucb-bar/rocket-chip) processor generator with DSP extensions.
These DSP extensions are memory-mapped peripherals, modeled as a contiguous digital signal processing chain with control access through simple memory-mapped registers and data access through a programmable, memory-mapped FIFO buffer.
The code is primarily written in Chisel3, leveraging a variety of Chisel3 extensions.

## Organization

The code is organized as follows. For more information, check out the READMEs in the various submodules.

 * [dsp-framework](https://github.com/ucb-art/dsp-framework/blob/master/README.md) - contains many of the code dependencies for this project
 * verisim, vsim, ncsim - simulation directories (described below)
 * bootrom - sources for the first-stage bootloader included in the Boot ROM
 * src/main/scala - scala source files specific to craft
 * various DSP blocks (fft, pfb, etc) - code for DSP blocks used in the design
 * riscv-dma2 - the memcpy DMA RoCC accelerator
 * tests - custom C code tests for peripherals

The dsp-framework repository contains the following things, so check there for more info.

 * rocket-chip (with firrtl, chisel3, and hwacha)
 * chisel-testers, firrtl-interpreter
 * testchipip (SCRFile, main memory SRAM)
 * dsptools (DspComplex type)
 * rocket-dsp-utils (tons of craft-related stuff, such as IP-Xact, SAM, AXI crossbar, DSP Stream, DSP block and DSP chain resources)
 * builtin-debugger (logic analyzer and pattern generator)

## Getting started

### Checking out the sources

After cloning this repo, you will need to initialize all of the submodules.

    git clone git@github.com:ucb-art/craft2-chip.git
    cd craft2-chip
    git submodule update --init --recursive

This can take a long time.
To avoid cloning the riscv-tools (in case you already have them built), use the following commands instead.

    git clone git@github.com:ucb-art/craft2-chip.git
    cd craft2-chip
    git submodule update --init
    cd dsp-framework
    ./update.bash

Also, Hwacha is currently private, so if you don't have access to Hwacha, run the following commands instead of the last one above.
This assumes you're in bash (use the setenv command if you're in csh).

    ./update.bash no_hwacha
    export ROCKETCHIP_ADDONS=


### Compiling the dependencies

This step is optional.
The dependencies will be automatically compiled the first time you try to compile the design.
But I recommend compiling them explicitly the first time.
In the top level directory, run the following command to compile the dependencies.

```
make libs
```

### Building the RISCV tools

The tools repo contains the cross-compiler toolchain, frontend server, and
proxy kernel, which you will need in order to compile code to RISC-V
instructions and run them on your design. There are detailed instructions at
https://github.com/riscv/riscv-tools. But to get a basic installation, just
the following steps are necessary.

    # setup your environment (do this every time you need to use the tools)
    source enter.bash

    # build the tools (on the BWRC servers)
    mkdir install
    source /opt/rh/devtoolset-2/enable
    cd rocket-chip/riscv-tools
    ./build.sh

### Compiling the design

Compilation involves a number of steps, enumerated here. 
Each one has the previous ones as a dependency, so compiling later views will automatically generate earlier views if required.
Results are placed in the `generated-src` directory (which is created if needed).

#### Generating FIRRTL 

[FIRRTL](https://github.com/ucb-bar/firrtl) is the intermediate representation between Chisel and Verilog.
To generate, run the following command.

```
make firrtl
```

This creates a `.fir` file in the `generated-src` directory.
This also creates IP-Xact XML files in the `generated-src` directory, currently only for each component (DSP block or crossbar) in the design.

#### Generating Verilog

FIRRTL is compiled into Verilog.
To do this, run the following command.

```
make verilog
```

This creates a number of files in the `generated-src` directory.

* `.v` = This contains the design itself in Verilog. FIRRTL runs memory inference, so memories become black boxes, as seen at the bottom of the generated Verilog file.
* `.harness.v` = This contains the test harness and any other testing modules. Do not include this when synthesizing.
* `.conf` = This contains memory configurations. Each requested SRAM has its name, size, and configurations enumerated here. This is used in the next step.
* `.domains` = This contains clock domain origins. It is useful for synthesis and place-and-route constraining, but is currently not used.

#### Generating memories

While FIRRTL black-boxes the memories, they must be mapped to your technology.
The script to do this is `vlsi/src/vlsi_mem_gen`.
Right now, this is done for TSMC 16nm FF.
Make sure you have the right permissions.
On the BWRC servers, source `sourceme.sh` in bash to set up a TSMCHOME environment variable, add the license, and add the memory compiler to your path.
Then run the following command, which reads the `.conf` file produced in the last step to generate technology-mapped memories.

```
make mems
```

Results are in `generated-src`.
This produces a `.mems.v` file.

#### Generating a pad frame and top-level Verilog

The final step is to generate a pad frame and module that hooks up the pad frame to the design.
Again, this is technology dependent, but it does not require any setup, only access to the vlsi submodule.
The script to do this is `vlsi/src/create_pads.py`.
Modify this script as needed to produce the correct pad frame.
Run the following command to perform this step.

```
make pads
```

This produces the following files, all placed in `generated-src`.

* `.pads.v` = This file contains the pad frame Verilog module.
* `.io` = This is a Innovus script that helps place the pads in the right locations.
* `.top.v` = This is the top-level Verilog module, which instantiates the design and the pad frame, and then connects them.


#### Generating everything

To perform all the above steps in order, run the following command.

```
make top
```


### Simulating

Simulation requires the RISCV toolchain to be installed (see __Building the RISCV tools__ above).
The project supports three simulators, though some many not be functional or thoroughly tested yet.
Each simulator regenerates its own Verilog used for simulation.
It runs the compilation steps up through memory generation currently, so tests include technology-mapped memories but not the pad frame.
The tests are placed in your toolchain directory (mapped to the environment variable RISCV).

#### Synopsys VCS

The VCS simulation directory is `vsim`.
To compile the VCS simulator, change into the `vsim` directory and run the following commands.
This must be a two-step compilation process as the first step produces the memories, and the uses those to compile the simulator.

```
make top
make
```

To actually run a test, execute the simulator, such as below.

```
./simv-craft-Craft2Config +max-cycles=50000 $RISCV/riscv64-unknown-elf/share/riscv-tests/isa/rv64ui-p-simple
```

#### Verilator

Verilator is a free, open source Verilog simulator.
We tried setting it up, but excessive compile times and runtime bugs prevented us from succeeding.
Currently, Verilator is not supported.

#### Cadence Incisive

The Incisive simulation directory is `ncsim`.
This simulator is still being set up.

### Working inside the Cadence VCAD Chamber

Setup in the Chamber has some differences.
First, set up everything as below.

    source /projects/craft_p1/tools/setup.sh
    git clone /projects/craft_p1/git/craft2-chip.git
    cd craft2-chip
    init_user_sbt  # Optional, if you do not have a ~/.sbt directory.
    init_project_ivy2 # This command copies a .ivy2 cache to the project directory; only needed once per project
    git_submodule_init
    cd dsp-framework
    git_submodule_init
    cd rocket-chip
    git_submodule_init
    cd ../..
    make libs

There's no need to build the RISC-V tools, as we've already installed them in the Chamber.
Sourcing the setup.sh file in the tools directory puts them into your path.
Proceed to compilation and testing as above, without the need for sourcing other setup scrips.
