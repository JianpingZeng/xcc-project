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

Apart from fore-end and middle optimizer, Implementing the same semantic of
IR code with machine-specific instructions is also greatly important part of
complete compiler. In "XCC", all of those work was implemented in directory backend,
it consists of instruction selection, [TODO]instruction scheduling,
register allocation. Currently, a naive instruction selection based on macro
expansion has been completed. "XCC" supports three kind of register allocator,
SimpleAllocator scoped in single instruction, LocalAllocator focused on basic block,
and linear scan register allocator focused on whole machine function.

Currently, only X86 target is supported.

## Future plan
Make the performance improvement of phase of instruction selection by leverage of
tree-pattern matching. Add a default instruction scheduling to reduce pipeline stall
as minimal as possible. Also, there are some valuable points to improve register
allocator, for example, we can take the use point into consideration to avoid
code spill and to split whole LiveInterval into multiple parts

### Todo List
For the register allocation, a live interval coalescing and live interval spliting
should be added to avoiding redundant move instruction. Another point should be
improved is peephole optimization after instruction inselection, currently naive
instruction selection generates very poorly machine code.

Supports generation of conversion instruction between fp and integer for FastISel.

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

