; ModuleID = 'switch.c'
target datalayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-n8:16:32:64"
target triple = "x86_64-unknown-linux-gnu"

@str = internal constant [13 x i8] c"hello from 1\00" ; <[13 x i8]*> [#uses=1]
@str8 = internal constant [13 x i8] c"hello from 2\00" ; <[13 x i8]*> [#uses=1]
@str9 = internal constant [14 x i8] c"hello from 74\00" ; <[14 x i8]*> [#uses=1]
@str10 = internal constant [15 x i8] c"hello from 100\00" ; <[15 x i8]*> [#uses=1]
@str11 = internal constant [15 x i8] c"hello from 110\00" ; <[15 x i8]*> [#uses=1]
@str12 = internal constant [15 x i8] c"hello from 122\00" ; <[15 x i8]*> [#uses=1]
@str13 = internal constant [15 x i8] c"hello from 200\00" ; <[15 x i8]*> [#uses=1]
@str14 = internal constant [19 x i8] c"hello from default\00" ; <[19 x i8]*> [#uses=1]

define void @bar(i32 %a) nounwind {
entry:
  switch i32 %a, label %sw.default [
    i32 1, label %sw.bb
    i32 2, label %sw.bb1
    i32 74, label %sw.bb3
    i32 100, label %sw.bb5
    i32 110, label %sw.bb7
    i32 122, label %sw.bb9
    i32 200, label %sw.bb11
  ]

sw.bb:                                            ; preds = %entry
  %puts = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb1:                                           ; preds = %entry
  %puts14 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str8, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb3:                                           ; preds = %entry
  %puts15 = tail call i32 @puts(i8* getelementptr inbounds ([14 x i8]* @str9, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb5:                                           ; preds = %entry
  %puts16 = tail call i32 @puts(i8* getelementptr inbounds ([15 x i8]* @str10, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb7:                                           ; preds = %entry
  %puts17 = tail call i32 @puts(i8* getelementptr inbounds ([15 x i8]* @str11, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb9:                                           ; preds = %entry
  %puts18 = tail call i32 @puts(i8* getelementptr inbounds ([15 x i8]* @str12, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb11:                                          ; preds = %entry
  %puts19 = tail call i32 @puts(i8* getelementptr inbounds ([15 x i8]* @str13, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.default:                                       ; preds = %entry
  %puts20 = tail call i32 @puts(i8* getelementptr inbounds ([19 x i8]* @str14, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void
}

declare i32 @puts(i8* nocapture) nounwind
