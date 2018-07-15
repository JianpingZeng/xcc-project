                    Extremely C Compiler (XCC)

## Overview
This is a C language compiler named of **XCC** for researching and studying
machine-independent optimization on IR, like SCCP, GVN, LICM, IndVarSimply,
ConstantPropagate, ConstantFolding, DCE, and so many Loop Analysis & 
transforms. In the other hand, it also includes a little of machine-
dependent optimization, such as peephole opt on X86 platform.

In order to focus on optimization, the implementation is derived from 
[Clang project](https://clang.llvm.org/) rather than do it by myself 
since lacking of sufficient time and energy.  

## Features
The fore-end named of "Jlang" is completed which can used to lex, parse input
source code. Then semantic action would be performed that operated on AST to
check the correctness, more important thing is to report precise diagnostics
information at the given source location.

In order to view the internal data structure about "Jlang", we provide various
of command line options to control the output result, e.g. dumping tokens with
"-E", dumping AST corresponding to source code with option "-ast-dump", emitting
LLVM 2.6-compatible IR with option "-emit-llvm" etc.

In terms of middle-end, the "XCC" is able to performance some important analysis
and transformation passes, such as basic alias analysis, induction variable
recognization, loop recognization, construction of dominator tree, dead code
elimination, global value numbering, induction variable simplification, lcssa,
loop invariant code motion, unused loop deletion, loop inversion, sparse conditional
constant propagation, scalar replacement of aggregate, tail call elimination,
unreachable basic block elimination etc.

Apart from fore-end and middle optimizer, implementing same semantic of
LLVM IR code with machine-specific instructions is also greatly important part of
a complete compiler. Regarding of "XCC", all of those work was implemented in
directory backend, which consists of instruction selector based dag covering to
accomplish the purpose, great code quality, maintenance-friendly, simple and fast
list-based instruction scheduler, and register allocator based on Christan Wimmer's paper.

Note that, only X86 target is supported on Linux and Mac OSX system. It is expected
to add supporting for MIPS or ARM in the future.

## Future plan
First of all, add a greedy list scheduler to reduce pipeline stall as small as possible
for X86 target. Secondly, considering iterated register coalescing to eliminate redundant
move instruction linked two different virtual or physical register. There are many benchmarks
presented by researchers indicated about 50% move instructions can be striped off, so
this technology is a powerful tool and desired to be implemented in the future.

Most importantly, supporting for other target, such as MIPS, ARM, is a interesting work to be
finished in the future, but I can't promise it.

## Usage
Great announcement! We build Jlang and Backend for Ubuntu 14.04/Ubuntu 16.04
64 bit OS with OracleJDK/OpenJDK 1.8, GCC 5.4/6.3 or Clang 3.9/4.0 successfully.

### Prerequisites
1. 64 bit Ubuntu 14.04 or 16.04 OS.
2. Install [OracleJDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
or [OpenJDK 1.8](https://github.com/alexkasko/openjdk-unofficial-builds).
3. Install any version of GCC supports c++ 11 features or latest version of [Clang](https://apt.llvm.org/).
4. If you run the native executable instead of jar package on Modern MacOSX (>= 10.8) and newer
   JDK(>= 1.7), the legacy JDK support must be installed before running native launcher.
   
   Supporting can be retrieved from [Java 6 on newer MacOSX](https://support.apple.com/kb/DL1572?locale=en_US&viewlocale=en_US). 

### Build
1. Clone this repository into your local directory.
2. Build this project with following command.
   ````bash
    cd xcc
    mkdir build && cd build
    cmake ../
    make all [-j8 depends on number of CPU cores in your local machine]
   ````
3. After finishing step 2, the executable file and jar file would be generated and 
   resided in directory build/bin, build/lib respectively.

