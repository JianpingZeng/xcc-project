; ModuleID = 'i8toi32.c'
target datalayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-n8:16:32:64"
target triple = "x86_64-unknown-linux-gnu"

@chs = global [2 x i8] c"\01\02", align 1         ; <[2 x i8]*> [#uses=1]

define i32 @i8_to_i32(i32 %x) nounwind readonly {
entry:
  %tmp1 = load i8* getelementptr inbounds ([2 x i8]* @chs, i64 0, i64 0) ; <i8> [#uses=1]
  %conv = zext i8 %tmp1 to i32                    ; <i32> [#uses=1]
  %add = add i32 %conv, %x                        ; <i32> [#uses=1]
  ret i32 %add
}
