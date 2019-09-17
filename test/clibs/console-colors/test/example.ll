; ModuleID = 'test/example.c'
target datalayout = "e-p:64:64:64-S128-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-f128:128:128-n8:16:32:64"
target triple = "x86_64-unknown-linux-gnu"

module asm "\09.ident\09\22GCC: (GNU) 4.6.4 LLVM: \22"

%struct._IO_FILE = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker*, %struct._IO_FILE*, i32, i32, i64, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i64, i32, [20 x i8] }
%struct._IO_marker = type { %struct._IO_marker*, %struct._IO_FILE*, i32, [4 x i8] }

@stdout = external global %struct._IO_FILE*
@.cst = linker_private unnamed_addr constant [6 x i8] c"HELLO\00", align 8
@.cst1 = linker_private unnamed_addr constant [8 x i8] c" WORLD\0A\00", align 8

define i32 @main(i32 %argc, i8** nocapture %argv) nounwind uwtable {
entry:
  %0 = load %struct._IO_FILE** @stdout, align 8
  %1 = tail call i32 (i32, %struct._IO_FILE*, i8*, ...)* @cc_fprintf(i32 426, %struct._IO_FILE* %0, i8* getelementptr inbounds ([6 x i8]* @.cst, i64 0, i64 0)) nounwind
  %2 = load %struct._IO_FILE** @stdout, align 8
  %3 = tail call i32 (i32, %struct._IO_FILE*, i8*, ...)* @cc_fprintf(i32 142, %struct._IO_FILE* %2, i8* getelementptr inbounds ([8 x i8]* @.cst1, i64 0, i64 0)) nounwind
  ret i32 0
}

declare i32 @cc_fprintf(i32, %struct._IO_FILE*, i8*, ...)
