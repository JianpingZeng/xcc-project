# Overview
This is a C language compiler named of **xcc** for researching and studying
machine-independent optimization on IR, like SCCP, GVN, LICM, IndVarSimply,
ConstantPropagate, ConstantFolding, DCE, and so many Loop Analysis & 
transforms. In the other hand, it also includes a little of machine-
dependent optimization, such as peephole opt on X86 platform.

In order to focus on optimization, the implementation is derived from 
[Clang project](https://clang.llvm.org/) rather than do it by myself 
since lacking of sufficient time and energy.  

## Current work
The frontend for the C language has been finished with respect to Clang
besides a little code used for semantic checking. But the work of generating 
LLVM IR from frontend AST is completed, I also have finished some important
analysis and transforms, including Loop detection, Loop transform, dominator
tree and domination frontier etc.

In the side of backend, First, a local register allocation is added, but
no instruction scheduling. I just takes the simple code generation from
IR rather than automatic technique based **Burg**. In order to obtain
better performance, a simple peephole optimizer for X86 target was added

## Future plan
Continue to fix the drawbacks of the Jlang(our frontend for this project).
Refines the register allocation, it is possible that we would introduce a
greedy allocator or LSRA, add a instruction selector based on Burg.

## Usage
Great announcement! We build Jlang and Backend for Ubuntu 14.04/Ubuntu 16.04
64 bit OS with OracleJDK/OpenJDK 1.8, GCC 5.4/6.3 or Clang 3.9/4.0 successfully.
###Prerequisites
1. 64 bit Ubuntu 14.04 or 16.04 OS.
2. Install [OracleJDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
or [OpenJDK 1.8](https://github.com/alexkasko/openjdk-unofficial-builds).
3. Install any version of GCC supports c++ 11 features or latest version of [Clang](https://apt.llvm.org/).

### Build
1. Clone this repository into your local directory.
2. Build this project with following command.
   ````bash
    cd xcc && make
   ````
3. After finishing step 2, the executable file and jar file would be generated and 
   resided in directory out/bin, out/lib respectively.

