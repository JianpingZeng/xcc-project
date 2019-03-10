                    Extremely C Compiler (XCC)

## Overview
XCC is a research compiler written in Java for translating [LLVM IR](https://llvm.org/docs/LangRef.html) into
machine code or assembly code, which accepts LLVM IR compatible with LLVM 2.7 to 3.x, and features 
several important machine-independent and dependent optimization strategies, such as SCCP, GVN, LICM, 
Induction variable simplification, DCE etc. Also it can generate assembly code for X86 target on Linux
and Mac OSX platform.

## Why Java for compiler?
As we know, java is a "safe" programming language which can free programmer to care about memory 
management and some stupid system crash problem and let programmer focus on compiler-specific
field. Another important reason is I plan to implement a AOT compiler for Java which can take 
best advantage of infrastructure provided by XCC.

## Future plan
1. Improving completely support for LLVM IR, especial those IR specific to C++ except handling and
debug information.
2. Using an advancing register allocator, such as Greedy allocator of LLVM, graph coloring, or IL-Based
allocator. Currently, XCC use a simple local register allocator scoped in basic block.
3. Directly generating object code for X86 target on Linux platform instead of calling external 
assembler to translate assembly code into object code.
4. Advance the optimization strategies used by XCC so as to improve its performance.
5. Support Java in way of [Swift compiler](https://swift.org/) instead of using such gc.root stuff 
provided by LLVM infrastructure. 


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
    cmake ../ -DXCC_TARGETS_TO_BUILD=X86
    make all [-j8 depends on number of CPU cores in your local machine]
   ````
3. Using Clang 3.0 to generate a LLVM IR file from C source file. 
Save the following code into a c file called hello.c
```c
#include <stdio.h>
int main() {
  printf("hello xcc compiler\n");
  return 0;
}
```
generating LLVM IR from hello.c with the command "clang -cc1 -S -emit-llvm -O2 hello.c -o hello.ll". 
```llvm
; ModuleID = 'hello.c'
target datalayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

@str = internal constant [19 x i8] c"hello xcc compiler\00"

define i32 @main() nounwind uwtable {
entry:
  %puts = tail call i32 @puts(i8* getelementptr inbounds ([19 x i8]* @str, i64 0, i64 0))
  ret i32 0
}

declare i32 @puts(i8* nocapture) nounwind
```

with the following command to call llc provided by XCC to generate assembly code instead of using 
LLVM official llc tool.

```sbtshell
llc -filetype=asm hello.ll
```

If you want to view the output result of every called pass, adding a -print-after-all option as the official tool does.


