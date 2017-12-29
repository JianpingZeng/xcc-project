; ModuleID = 'call.c'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-a0:0:64-f80:32:32"
target triple = "i686-linux-gnu"

define i32 @main() nounwind {
  %1 = alloca i32                                 ; <i32*> [#uses=3]
  %a = alloca i32, align 4                        ; <i32*> [#uses=2]
  store i32 0, i32* %1
  %2 = call i32 @bar(i32 1)                       ; <i32> [#uses=1]
  store i32 %2, i32* %a
  %3 = load i32* %a                               ; <i32> [#uses=1]
  %4 = add nsw i32 %3, 1                          ; <i32> [#uses=1]
  store i32 %4, i32* %1
  %5 = load i32* %1                               ; <i32> [#uses=1]
  ret i32 %5
}

declare i32 @bar(i32 %x)
