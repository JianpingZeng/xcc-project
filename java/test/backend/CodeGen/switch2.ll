; ModuleID = 'switch2.c'
target datalayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-n8:16:32:64"
target triple = "x86_64-unknown-linux-gnu"

@str = internal constant [13 x i8] c"hello from 1\00" ; <[13 x i8]*> [#uses=1]
@str12 = internal constant [13 x i8] c"hello from 2\00" ; <[13 x i8]*> [#uses=1]
@str13 = internal constant [13 x i8] c"hello from 3\00" ; <[13 x i8]*> [#uses=1]
@str14 = internal constant [13 x i8] c"hello from 4\00" ; <[13 x i8]*> [#uses=1]
@str15 = internal constant [13 x i8] c"hello from 5\00" ; <[13 x i8]*> [#uses=1]
@str16 = internal constant [13 x i8] c"hello from 6\00" ; <[13 x i8]*> [#uses=1]
@str17 = internal constant [13 x i8] c"hello from 7\00" ; <[13 x i8]*> [#uses=1]
@str18 = internal constant [13 x i8] c"hello from 8\00" ; <[13 x i8]*> [#uses=1]
@str19 = internal constant [13 x i8] c"hello from 9\00" ; <[13 x i8]*> [#uses=1]
@str20 = internal constant [14 x i8] c"hello from 10\00" ; <[14 x i8]*> [#uses=1]
@str21 = internal constant [14 x i8] c"hello from 11\00" ; <[14 x i8]*> [#uses=1]
@str22 = internal constant [19 x i8] c"hello from default\00" ; <[19 x i8]*> [#uses=1]

define void @bar(i32 %a) nounwind {
entry:
  switch i32 %a, label %sw.default [
    i32 1, label %sw.bb
    i32 2, label %sw.bb1
    i32 3, label %sw.bb3
    i32 4, label %sw.bb5
    i32 5, label %sw.bb7
    i32 6, label %sw.bb9
    i32 7, label %sw.bb11
    i32 8, label %sw.bb13
    i32 9, label %sw.bb15
    i32 10, label %sw.bb17
    i32 11, label %sw.bb19
  ]

sw.bb:                                            ; preds = %entry
  %puts = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb1:                                           ; preds = %entry
  %puts22 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str12, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb3:                                           ; preds = %entry
  %puts23 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str13, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb5:                                           ; preds = %entry
  %puts24 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str14, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb7:                                           ; preds = %entry
  %puts25 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str15, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb9:                                           ; preds = %entry
  %puts26 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str16, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb11:                                          ; preds = %entry
  %puts27 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str17, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb13:                                          ; preds = %entry
  %puts28 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str18, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb15:                                          ; preds = %entry
  %puts29 = tail call i32 @puts(i8* getelementptr inbounds ([13 x i8]* @str19, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb17:                                          ; preds = %entry
  %puts30 = tail call i32 @puts(i8* getelementptr inbounds ([14 x i8]* @str20, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.bb19:                                          ; preds = %entry
  %puts31 = tail call i32 @puts(i8* getelementptr inbounds ([14 x i8]* @str21, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void

sw.default:                                       ; preds = %entry
  %puts32 = tail call i32 @puts(i8* getelementptr inbounds ([19 x i8]* @str22, i64 0, i64 0)) ; <i32> [#uses=0]
  ret void
}

declare i32 @puts(i8* nocapture) nounwind
