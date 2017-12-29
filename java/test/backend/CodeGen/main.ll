; ModuleID = 'main.c'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-a0:0:64-f80:32:32"
target triple = "i686-linux-gnu"

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

define i32 @sum(i32 %len) nounwind {
  %1 = alloca i32                                 ; <i32*> [#uses=2]
  %len.addr = alloca i32                          ; <i32*> [#uses=2]
  %res = alloca i32, align 4                      ; <i32*> [#uses=4]
  %i = alloca i32, align 4                        ; <i32*> [#uses=5]
  store i32 %len, i32* %len.addr
  store i32 0, i32* %res
  store i32 0, i32* %i
  br label %2

; <label>:2                                       ; preds = %10, %0
  %3 = load i32* %i                               ; <i32> [#uses=1]
  %4 = load i32* %len.addr                        ; <i32> [#uses=1]
  %5 = icmp slt i32 %3, %4                        ; <i1> [#uses=1]
  br i1 %5, label %6, label %13

; <label>:6                                       ; preds = %2
  %7 = load i32* %i                               ; <i32> [#uses=1]
  %8 = load i32* %res                             ; <i32> [#uses=1]
  %9 = add nsw i32 %8, %7                         ; <i32> [#uses=1]
  store i32 %9, i32* %res
  br label %10

; <label>:10                                      ; preds = %6
  %11 = load i32* %i                              ; <i32> [#uses=1]
  %12 = add nsw i32 %11, 1                        ; <i32> [#uses=1]
  store i32 %12, i32* %i
  br label %2

; <label>:13                                      ; preds = %2
  %14 = load i32* %res                            ; <i32> [#uses=1]
  store i32 %14, i32* %1
  %15 = load i32* %1                              ; <i32> [#uses=1]
  ret i32 %15
}

define i32 @main() nounwind {
  %1 = alloca i32                                 ; <i32*> [#uses=3]
  %a = alloca i32, align 4                        ; <i32*> [#uses=2]
  store i32 0, i32* %1
  %2 = call i32 @max(i32 1, i32 2)                ; <i32> [#uses=1]
  store i32 %2, i32* %a
  %3 = load i32* %a                               ; <i32> [#uses=1]
  %4 = call i32 @sum(i32 %3)                      ; <i32> [#uses=1]
  store i32 %4, i32* %1
  %5 = load i32* %1                               ; <i32> [#uses=1]
  ret i32 %5
}
