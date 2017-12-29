; ModuleID = 'ra.c'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-a0:0:64-f80:32:32"
target triple = "i686-linux-gnu"

define void @bar(i32 %m, i32 %n, i32** %lhs, i32** %rhs, i32** %result) nounwind {
  %m.addr = alloca i32                            ; <i32*> [#uses=2]
  %n.addr = alloca i32                            ; <i32*> [#uses=2]
  %lhs.addr = alloca i32**                        ; <i32***> [#uses=3]
  %rhs.addr = alloca i32**                        ; <i32***> [#uses=3]
  %result.addr = alloca i32**                     ; <i32***> [#uses=2]
  %i = alloca i32, align 4                        ; <i32*> [#uses=7]
  %j = alloca i32, align 4                        ; <i32*> [#uses=7]
  store i32 %m, i32* %m.addr
  store i32 %n, i32* %n.addr
  store i32** %lhs, i32*** %lhs.addr
  store i32** %rhs, i32*** %rhs.addr
  store i32** %result, i32*** %result.addr
  %1 = load i32*** %lhs.addr                      ; <i32**> [#uses=1]
  %2 = icmp ne i32** %1, null                     ; <i1> [#uses=1]
  br i1 %2, label %3, label %6

; <label>:3                                       ; preds = %0
  %4 = load i32*** %rhs.addr                      ; <i32**> [#uses=1]
  %5 = icmp ne i32** %4, null                     ; <i1> [#uses=1]
  br i1 %5, label %7, label %6

; <label>:6                                       ; preds = %3, %0
  br label %48

; <label>:7                                       ; preds = %3
  store i32 0, i32* %i
  br label %8

; <label>:8                                       ; preds = %45, %7
  %9 = load i32* %i                               ; <i32> [#uses=1]
  %10 = load i32* %m.addr                         ; <i32> [#uses=1]
  %11 = icmp slt i32 %9, %10                      ; <i1> [#uses=1]
  br i1 %11, label %12, label %48

; <label>:12                                      ; preds = %8
  store i32 0, i32* %j
  br label %13

; <label>:13                                      ; preds = %41, %12
  %14 = load i32* %j                              ; <i32> [#uses=1]
  %15 = load i32* %n.addr                         ; <i32> [#uses=1]
  %16 = icmp slt i32 %14, %15                     ; <i1> [#uses=1]
  br i1 %16, label %17, label %44

; <label>:17                                      ; preds = %13
  %18 = load i32* %j                              ; <i32> [#uses=1]
  %19 = load i32* %i                              ; <i32> [#uses=1]
  %20 = load i32*** %lhs.addr                     ; <i32**> [#uses=1]
  %21 = getelementptr inbounds i32** %20, i32 %19 ; <i32**> [#uses=1]
  %22 = load i32** %21                            ; <i32*> [#uses=1]
  %23 = getelementptr inbounds i32* %22, i32 %18  ; <i32*> [#uses=1]
  %24 = load i32* %23                             ; <i32> [#uses=1]
  %25 = load i32* %j                              ; <i32> [#uses=1]
  %26 = load i32* %i                              ; <i32> [#uses=1]
  %27 = load i32*** %rhs.addr                     ; <i32**> [#uses=1]
  %28 = getelementptr inbounds i32** %27, i32 %26 ; <i32**> [#uses=1]
  %29 = load i32** %28                            ; <i32*> [#uses=1]
  %30 = getelementptr inbounds i32* %29, i32 %25  ; <i32*> [#uses=1]
  %31 = load i32* %30                             ; <i32> [#uses=1]
  %32 = add nsw i32 %24, %31                      ; <i32> [#uses=1]
  %33 = load i32* %j                              ; <i32> [#uses=1]
  %34 = load i32* %i                              ; <i32> [#uses=1]
  %35 = load i32*** %result.addr                  ; <i32**> [#uses=1]
  %36 = getelementptr inbounds i32** %35, i32 %34 ; <i32**> [#uses=1]
  %37 = load i32** %36                            ; <i32*> [#uses=1]
  %38 = getelementptr inbounds i32* %37, i32 %33  ; <i32*> [#uses=2]
  %39 = load i32* %38                             ; <i32> [#uses=1]
  %40 = add nsw i32 %39, %32                      ; <i32> [#uses=1]
  store i32 %40, i32* %38
  br label %41

; <label>:41                                      ; preds = %17
  %42 = load i32* %j                              ; <i32> [#uses=1]
  %43 = add nsw i32 %42, 1                        ; <i32> [#uses=1]
  store i32 %43, i32* %j
  br label %13

; <label>:44                                      ; preds = %13
  br label %45

; <label>:45                                      ; preds = %44
  %46 = load i32* %i                              ; <i32> [#uses=1]
  %47 = add nsw i32 %46, 1                        ; <i32> [#uses=1]
  store i32 %47, i32* %i
  br label %8

; <label>:48                                      ; preds = %6, %8
  ret void
}
