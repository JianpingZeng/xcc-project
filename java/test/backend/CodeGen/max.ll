; ModuleID = 'max.c'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-a0:0:64-f80:32:32"
target triple = "i686-linux-gnu"

define i32 @max(i32 %x, i32 %y) nounwind {
  %1 = alloca i32                                 ; <i32*> [#uses=3]
  %x.addr = alloca i32                            ; <i32*> [#uses=3]
  %y.addr = alloca i32                            ; <i32*> [#uses=3]
  store i32 %x, i32* %x.addr
  store i32 %y, i32* %y.addr
  %2 = load i32* %x.addr                          ; <i32> [#uses=1]
  %3 = load i32* %y.addr                          ; <i32> [#uses=1]
  %4 = icmp sgt i32 %2, %3                        ; <i1> [#uses=1]
  br i1 %4, label %5, label %7

; <label>:5                                       ; preds = %0
  %6 = load i32* %x.addr                          ; <i32> [#uses=1]
  store i32 %6, i32* %1
  br label %9

; <label>:7                                       ; preds = %0
  %8 = load i32* %y.addr                          ; <i32> [#uses=1]
  store i32 %8, i32* %1
  br label %9

; <label>:9                                       ; preds = %7, %5
  %10 = load i32* %1                              ; <i32> [#uses=1]
  ret i32 %10
}
