; ModuleID = 'test/example.i'
target datalayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%struct._IO_FILE = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker*, %struct._IO_FILE*, i32, i32, i64, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i64, i32, [20 x i8] }
%struct._IO_marker = type { %struct._IO_marker*, %struct._IO_FILE*, i32 }

@stdout = external global %struct._IO_FILE*
@.str = private unnamed_addr constant [6 x i8] c"HELLO\00", align 1
@.str1 = private unnamed_addr constant [8 x i8] c" WORLD\0A\00", align 1

define i32 @main(i32 %argc, i8** nocapture %argv) nounwind {
entry:
  %0 = load %struct._IO_FILE** @stdout, align 8, !tbaa !0
  %call = tail call i32 (i32, %struct._IO_FILE*, i8*, ...)* @cc_fprintf(i32 426, %struct._IO_FILE* %0, i8* getelementptr inbounds ([6 x i8]* @.str, i64 0, i64 0)) nounwind
  %1 = load %struct._IO_FILE** @stdout, align 8, !tbaa !0
  %call1 = tail call i32 (i32, %struct._IO_FILE*, i8*, ...)* @cc_fprintf(i32 142, %struct._IO_FILE* %1, i8* getelementptr inbounds ([8 x i8]* @.str1, i64 0, i64 0)) nounwind
  ret i32 0
}

declare i32 @cc_fprintf(i32, %struct._IO_FILE*, i8*, ...)

!0 = metadata !{metadata !"any pointer", metadata !1}
!1 = metadata !{metadata !"omnipotent char", metadata !2}
!2 = metadata !{metadata !"Simple C/C++ TBAA", null}
