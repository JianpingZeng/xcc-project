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

