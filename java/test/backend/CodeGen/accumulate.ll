; ModuleID = 'java/test/backend/CodeGen/accumulate.c'
; ModuleID = '<stdin>'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-a0:0:64-f80:32:32"
target triple = "i686-linux-gnu"

define i32 @sum(i32 %x) nounwind {
; <label>:0
  br label %1

; <label>:1                                       ; preds = %5, %0
  %res.0 = phi i32 [ 0, %0 ], [ %4, %5 ]          ; <i32> [#uses=2]
  %i.0 = phi i32 [ 0, %0 ], [ %6, %5 ]            ; <i32> [#uses=3]
  %2 = icmp slt i32 %i.0, %x                      ; <i1> [#uses=1]
  br i1 %2, label %3, label %7

; <label>:3                                       ; preds = %1
  %4 = add nsw i32 %res.0, %i.0                   ; <i32> [#uses=1]
  br label %5

; <label>:5                                       ; preds = %3
  %6 = add nsw i32 %i.0, 1                        ; <i32> [#uses=1]
  br label %1

; <label>:7                                       ; preds = %1
  ret i32 %res.0
}

