eXtremely Compiler Collection (XCC)

## Overview

Nowdays, most of production compilers, e.g., LLVM, GCC, IBM XL, and ICC, are written by
C/C++ due to high efficiency of resulting in binary. However, it is not benefitial at all
times due to strange and subtle bugs casued by C/C++ itself for such large software project.
Inspired by safe programming feature provided by Java, we think it is starting point to use
re-invent an new compiler infrastructure which can provide the community with a concept-of-proof.

XCC is a research compiler written in Java for translating [LLVM IR](https://llvm.org/docs/LangRef.html) into assembly code,
which accepts LLVM IR compatible with LLVM 3.0. Currently, xcc naively translates
LLVM IR to unoptimized machine code except for the directed-acyclic-graph (DAG) based instruction
selection which does instruction folding for most instruction patterns. For the sake of simplicity,
XCC currently uses a simple up-bottom local register allocation which keeps the lifetime of live
interval within basic block. It means the local register allocation always spills the value of
live interval to the stack and reload it from stack in the beginning of next basic block.


Currently, XCC compiler can successfully compile all applications of SPEC CPU2006 even though
some generates binaries crash but it is a good sign.

## Future plan
1. Fix existing bugs which is our first priority without exception.
2. Complete tehe support of debug information and C++ exception handling.
2. Using an advancing register allocator, such as Greedy allocator of LLVM,
graph coloring, or IL-Based allocator. 
4. Advance the optimization strategies so as to improve its performance.


## Usage

### Prerequisites
1. 64 bit Ubuntu 14.04 or 16.04 OS.
2. Install [OracleJDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) or [OpenJDK 1.8](https://github.com/alexkasko/openjdk-unofficial-builds).
3. Install gcc-4.6.4, g++-4.6.4, gfortran-4.6.4 because we reuse the existing dragonegg 3.0 as
   frontend of C/C++/Fortran language.

### Build
1. Clone this repository into your local directory.
2. Build this project with the following command.
   ```bash
    cd xcc-project
    ./build.sh [ARM or X86]  Note that: It fails to build X86 target due to some bugs
   ```

### Compilation
1. Compile the C/C++ and Fortran program with the provided gcc.sh script. Some examples shown as follows.
   ``` bash
   xcc-project/xcc/java/utils/gcc-driver/gcc.sh --target=armv7 -static example.c -o example -O3
   xcc-project/xcc/java/utils/gcc-driver/gcc.sh --target=armv7 -static --linker=c++ example.cpp -o example -O3
   xcc-project/xcc/java/utils/gcc-driver/gcc.sh --target=armv7 -static -linker=fortran example.f90 -o example -O3
   ```

   After above commands, there will be some binaries generated in the current directory. Note that, XCC doesn't support
   c++ 11 standard because it uses a very old GCC version, 4.6.4, which doesn't support c++11 standard.
