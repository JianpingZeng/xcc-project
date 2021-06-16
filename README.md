eXtremely Compiler Collection (XCC)

## Overview

Nowdays, most of production compilers, e.g., LLVM, GCC, IBM XL, and ICC, are written in
C/C++ thanks to high efficiency of resulting binaries. However, it is not always benefitial
due to strange and subtle bugs introduced by C/C++ itself for large software projects.
Inspired by safe programming features provided by Java, we think it is a good starting point to
devise a new compiler infrastructure which provides the community with a concept-of-proof compiler.

XCC is a research compiler written in Java, which accepts [LLVM IR](https://llvm.org/docs/LangRef.html)
as an input and translates LLVM IR into assembly code. To make it simple, currently, xcc naively translates
LLVM IR to unoptimized machine code except for the directed-acyclic-graph (DAG) based instruction
selection which does instruction folding for most instruction patterns. For the sake of simplicity,
XCC uses a simple up-bottom local register allocator which keeps the lifetime of a live
interval within basic block, meaning that live interval always gets spilled at basic block boundary.

As of the latest commit, XCC successfully compiles all applications of SPEC CPU2006 for ARM target
though some generated binaries crash but it is a good sign. In addition, we have ongoing experimental
backend support for X86.

## Future plan
1. Fixing existing bugs which is our first priority without exception.
2. Completing the support of debug information and C++ exception handling.
2. Using an advancing register allocator, e.g., Greedy allocator of LLVM, graph coloring, and IL-Based allocator. 
4. Advancing the optimization strategies so as to improve its performance.

## Usage

### Prerequisites
1. 64 bit Ubuntu 14.04 or 16.04 OS.
2. Install [OracleJDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) or [OpenJDK 1.8](https://github.com/alexkasko/openjdk-unofficial-builds).
3. Install gcc-4.6.4, g++-4.6.4, gfortran-4.6.4 because we reuse the existing dragonegg 3.0 as
   frontend of C/C++/Fortran language.

### Build
1. Clone this repository into your local directory.
2. Build this project with following command under the build directory.
   ```bash
    cd xcc-project
    ./build.sh [ARM, X86, or Mips]  Note that: X86 target has many bugs and Mips is experimental
   ```

### Compilation
1. Compile C/C++ and Fortran programs with provided gcc.sh script. Some examples are shown as follows.
   ``` bash
   xcc-project/xcc/java/utils/gcc-driver/gcc.sh --target=armv7 -static example.c -o example -O3
   xcc-project/xcc/java/utils/gcc-driver/gcc.sh --target=armv7 -static --linker=c++ example.cpp -o example -O3
   xcc-project/xcc/java/utils/gcc-driver/gcc.sh --target=armv7 -static -linker=fortran example.f90 -o example -O3
   ```

   After using above commands, there will be some binaries generated under the working directory.
   Note that, XCC doesn't support c++ 11 standard because it uses a pretty old GCC version as a frontend, 4.6.4, which doesn't support c++11 standard.
