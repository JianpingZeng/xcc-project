; ModuleID = 'main.c'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-a0:0:64-f80:32:32"
target triple = "i386-linux-gnu"

@.str = private constant [25 x i8] c"the maximum(1, 2) is %d\0A\00" ; <[25 x i8]*> [#uses=1]

define i32 @max(i32 %a, i32 %b) nounwind {
  %1 = alloca i32                                 ; <i32*> [#uses=3]
  %a.addr = alloca i32                            ; <i32*> [#uses=3]
  %b.addr = alloca i32                            ; <i32*> [#uses=3]
  store i32 %a, i32* %a.addr
  store i32 %b, i32* %b.addr
  %2 = load i32* %a.addr                          ; <i32> [#uses=1]
  %3 = load i32* %b.addr                          ; <i32> [#uses=1]
  %4 = icmp sgt i32 %2, %3                        ; <i1> [#uses=1]
  br i1 %4, label %5, label %7

; <label>:5                                       ; preds = %0
  %6 = load i32* %a.addr                          ; <i32> [#uses=1]
  store i32 %6, i32* %1
  br label %9

; <label>:7                                       ; preds = %0
  %8 = load i32* %b.addr                          ; <i32> [#uses=1]
  store i32 %8, i32* %1
  br label %9

; <label>:9                                       ; preds = %7, %5
  %10 = load i32* %1                              ; <i32> [#uses=1]
  ret i32 %10
}

define i32 @main() nounwind {
  %1 = alloca i32                                 ; <i32*> [#uses=2]
  store i32 0, i32* %1
  %2 = call i32 @max(i32 1, i32 2)                ; <i32> [#uses=1]
  %3 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([25 x i8]* @.str, i32 0, i32 0), i32 %2) ; <i32> [#uses=0]
  %4 = load i32* %1                               ; <i32> [#uses=1]
  ret i32 %4
}

declare i32 @printf(i8*, ...)
