# Craft2 Chip

This repo contains the infrastructure for designing, testing, and building the craft2 chip.

## Overview

TODO

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
In the top level directory, run `make libs` to compile the dependencies.

### Building the tools

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

### Choosing Fixed or Floating Point

This project can generate the craft2 design using fixed or floating point.
You can switch between the two in `src/main/scala/craft/Configs.scala`.
The relevant lines are:

```
object ChainBuilder {
  type T = FixedPoint
  def getGenType(): T = FixedPoint(32.W, 16.BP)
```

Change T to DspReal, and the next line to T = DspReal().

Note that for `FixedPoint`, you should select a width and binary point.
The example shown here has width 32 and binary point 16.

### Compiling Verilog

To compile just the Verilog (without running any tests), type `make verilog` in the top-level directory.
Results are placed in the generated-src directory.
By default, memories are black-boxed.
Sourcing `sourceme.sh` in bash and running `make libs` will create -mems.v file that maps the black box memories to TSMC memories,
and it runs the TSMC memory compiler for you.

### Compiling and running the Verilator simulation

To compile the craft2 design, run make in the "verisim" directory.
This will elaborate the DefaultExampleConfig in the example project.
It will produce an executable called simulator-example-DefaultExampleConfig.
You can then use this executable to run any compatible RV64 code. For instance,
to run one of the riscv-tools assembly tests. Note that there's no output upon
successful completion.

    ./simulator-example-DefaultExampleConfig $RISCV/riscv64-unknown-elf/share/riscv-tests/isa/rv64ui-p-simple

If you later create your own project, you can use environment variables to
build an alternate configuration.

    make PROJECT=yourproject CONFIG=YourConfig
    ./simulator-yourproject-YourConfig ...

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

## Submodules and Subdirectories

The submodules and subdirectories for craft2-chip are organized as follows.

 * [dsp-framework](https://github.com/ucb-art/dsp-framework/blob/master/README.md) - contains all the dependencies for this project, see its README for more info
 * verisim - directory in which Verilator simulations are compiled and run
 * vsim - directory in which Synopsys VCS simulations are compiled and run
 * bootrom - sources for the first-stage bootloader included in the Boot ROM
 * src/main/scala - scala source files specific to craft
 * various DSP blocks (fft, pfb, etc) - code for DSP blocks used in the design
 * riscv-dma2 - the memcpy DMA RoCC accelerator
 * tests - custom C code tests for peripherals

