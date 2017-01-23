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
This assumes you are in a bash shell.

    git clone git@github.com:ucb-art/craft2-chip.git
    cd craft2-chip
    git submodule update --init
    cd dsp-framework
    ./update.bash

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

    # build the tools
    mkdir install
    source /opt/rh/devtoolset-2/enable
    cd rocket-chip/riscv-tools
    ./build.sh

### Choosing Fixed or Floating Point

This project can generate the craft2 design using fixed or floating point.
You can switch between the two in `src/main/scala/craft/Config.scala`.
The relevant lines are:

```
object ChainBuilder {
  // def getReal(): DspReal = DspReal()
  def getReal(): FixedPoint = FixedPoint(32.W, 16.BP)
...
    case DspChainKey("craft-afb") => DspChainParameters(
      blocks = Seq(
        (implicit p => new LazyPFBBlock[DspComplex[FixedPoint]], "pfb"),
        (implicit p => new LazyFFTBlock[FixedPoint],             "fft")
      ),
      dataBaseAddr = 0x2000,
      ctrlBaseAddr = 0x3000
    )
...
}
```

Uncommenting/commenting the desired `getReal()` switches between `FixedPoint` and `DspReal` (floating point).
Then you also need to change the types of the blocks in `DspChainKey`: `LazyPFBBlock[DspComplex[XXX]]` and `LazyFFTBlock[XXX]` should have `XXX` consistent with whichever `getReal()` you chose.

### Compiling Verilog

To compile just the Verilog (without running any tests), type `make verilog` in the top-level directory.
Results are placed in the generated-src directory.

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

Compile the dependencies.
A temporary bug requires you to delete existing lib directories in submodules.

    rm -rf fft/lib
    rm -rf pfb/lib
    make libs

There's no need to build the RISC-V tools, as we've already installed them in the Chamber.
Sourcing the setup.sh file puts them into your path.

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

