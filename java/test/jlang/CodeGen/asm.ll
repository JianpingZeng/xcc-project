; ModuleID = 'Development/compiler/xcc/java/test/jlang/CodeGen/asm.c'
target datalayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128"
target triple = "x86_64-unknown-linux-gnu"

define i32 @foo(i32 %x, i32 %y) nounwind {
  %1 = alloca i32                                 ; <i32*> [#uses=2]
  %x.addr = alloca i32                            ; <i32*> [#uses=2]
  %y.addr = alloca i32                            ; <i32*> [#uses=2]
  %res = alloca i32, align 4                      ; <i32*> [#uses=2]
  store i32 %x, i32* %x.addr
  store i32 %y, i32* %y.addr
  %2 = load i32* %x.addr                          ; <i32> [#uses=1]
  %3 = load i32* %y.addr                          ; <i32> [#uses=1]
  %4 = add nsw i32 %2, %3                         ; <i32> [#uses=1]
  store i32 %4, i32* %res
  %5 = load i32* %res                             ; <i32> [#uses=1]
  store i32 %5, i32* %1
  %6 = load i32* %1                               ; <i32> [#uses=1]
  ret i32 %6
}
