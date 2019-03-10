; ModuleID = 'parse_args.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct._IO_FILE.0 = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker.1*, %struct._IO_FILE.0*, i32, i32, i32, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i32, i32, [40 x i8] }
%struct._IO_marker.1 = type { %struct._IO_marker.1*, %struct._IO_FILE.0*, i32 }

@.str = private unnamed_addr constant [2 x i8] c"r\00", align 1
@.str1 = private unnamed_addr constant [3 x i8] c".E\00", align 1
@.str2 = private unnamed_addr constant [3 x i8] c"-o\00", align 1
@.str3 = private unnamed_addr constant [3 x i8] c"-l\00", align 1
@.str4 = private unnamed_addr constant [3 x i8] c"-b\00", align 1
@.str5 = private unnamed_addr constant [3 x i8] c"-x\00", align 1
@.str6 = private unnamed_addr constant [3 x i8] c"-y\00", align 1
@.str7 = private unnamed_addr constant [27 x i8] c"Unrecognized argument: %s\0A\00", align 1
@.str8 = private unnamed_addr constant [42 x i8] c"Error: num_levels must be greater than 0\0A\00", align 1
@.str9 = private unnamed_addr constant [53 x i8] c"Error: cannot build pyramid to more than %d levels.\0A\00", align 1
@.str10 = private unnamed_addr constant [36 x i8] c"Error: dimensions too big (%d,%d).\0A\00", align 1
@.str11 = private unnamed_addr constant [78 x i8] c"Error: dimensions (%d,%d) are not divisible by 2^num_levels (num_levels=%d).\0A\00", align 1
@.str12 = private unnamed_addr constant [76 x i8] c"Error: dimensions (%d,%d) divided by 2^num_levels must be greater than %d.\0A\00", align 1
@.str13 = private unnamed_addr constant [2 x i8] c"w\00", align 1
@.str14 = private unnamed_addr constant [79 x i8] c"Usage: \0Aepic infile [-o outfile] [-x xdim] [-y ydim] [-l levels] [-b binsize]\0A\00", align 1
@.str15 = private unnamed_addr constant [35 x i8] c"Usage:  unepic epicfile [outfile]\0A\00", align 1
@temp_byte = external global i8
@.str16 = private unnamed_addr constant [34 x i8] c"The file %s is not an EPIC file!\0A\00", align 1
@temp_short = external global i16
@.str17 = private unnamed_addr constant [3 x i8] c".U\00", align 1

define arm_aapcscc i32 @parse_epic_args(i32 %argc, i8** %argv, float** %image, i32* %x_size, i32* %y_size, i32* %num_levels, double* %compression_factor, %struct._IO_FILE.0** %outfile) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %argc.addr = alloca i32, align 4
  %argv.addr = alloca i8**, align 4
  %image.addr = alloca float**, align 4
  %x_size.addr = alloca i32*, align 4
  %y_size.addr = alloca i32*, align 4
  %num_levels.addr = alloca i32*, align 4
  %compression_factor.addr = alloca double*, align 4
  %outfile.addr = alloca %struct._IO_FILE.0**, align 4
  %infile = alloca %struct._IO_FILE.0*, align 4
  %filename = alloca i8*, align 4
  %arg_ptr = alloca i32, align 4
  store i32 %argc, i32* %argc.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %argc.addr}, metadata !18), !dbg !19
  store i8** %argv, i8*** %argv.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8*** %argv.addr}, metadata !20), !dbg !24
  store float** %image, float*** %image.addr, align 4
  call void @llvm.dbg.declare(metadata !{float*** %image.addr}, metadata !25), !dbg !29
  store i32* %x_size, i32** %x_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %x_size.addr}, metadata !30), !dbg !32
  store i32* %y_size, i32** %y_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %y_size.addr}, metadata !33), !dbg !34
  store i32* %num_levels, i32** %num_levels.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %num_levels.addr}, metadata !35), !dbg !36
  store double* %compression_factor, double** %compression_factor.addr, align 4
  call void @llvm.dbg.declare(metadata !{double** %compression_factor.addr}, metadata !37), !dbg !40
  store %struct._IO_FILE.0** %outfile, %struct._IO_FILE.0*** %outfile.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.0*** %outfile.addr}, metadata !41), !dbg !100
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.0** %infile}, metadata !101), !dbg !103
  call void @llvm.dbg.declare(metadata !{i8** %filename}, metadata !104), !dbg !105
  call void @llvm.dbg.declare(metadata !{i32* %arg_ptr}, metadata !106), !dbg !107
  store i32 2, i32* %arg_ptr, align 4, !dbg !108
  %0 = load i32** %y_size.addr, align 4, !dbg !109
  store i32 256, i32* %0, !dbg !109
  %1 = load i32** %x_size.addr, align 4, !dbg !109
  store i32 256, i32* %1, !dbg !109
  %2 = load i32** %num_levels.addr, align 4, !dbg !110
  store i32 -1, i32* %2, !dbg !110
  %3 = load double** %compression_factor.addr, align 4, !dbg !111
  store double 2.000000e+01, double* %3, !dbg !111
  %4 = load i32* %argc.addr, align 4, !dbg !112
  %cmp = icmp slt i32 %4, 2, !dbg !112
  br i1 %cmp, label %if.then, label %if.end, !dbg !112

if.then:                                          ; preds = %entry
  %call = call arm_aapcscc  i32 @epic_usage(), !dbg !113
  br label %if.end, !dbg !113

if.end:                                           ; preds = %if.then, %entry
  %5 = load i8*** %argv.addr, align 4, !dbg !114
  %arrayidx = getelementptr inbounds i8** %5, i32 1, !dbg !114
  %6 = load i8** %arrayidx, !dbg !114
  store i8* %6, i8** %filename, align 4, !dbg !114
  %7 = load i8** %filename, align 4, !dbg !115
  %call1 = call arm_aapcscc  %struct._IO_FILE.0* bitcast (%struct._IO_FILE.0* (...)* @check_fopen to %struct._IO_FILE.0* (i8*, i8*)*)(i8* %7, i8* getelementptr inbounds ([2 x i8]* @.str, i32 0, i32 0)), !dbg !115
  store %struct._IO_FILE.0* %call1, %struct._IO_FILE.0** %infile, align 4, !dbg !115
  %8 = load i8*** %argv.addr, align 4, !dbg !116
  %arrayidx2 = getelementptr inbounds i8** %8, i32 1, !dbg !116
  %9 = load i8** %arrayidx2, !dbg !116
  %call3 = call arm_aapcscc  i8* bitcast (i8* (...)* @concatenate to i8* (i8*, i8*)*)(i8* %9, i8* getelementptr inbounds ([3 x i8]* @.str1, i32 0, i32 0)), !dbg !116
  store i8* %call3, i8** %filename, align 4, !dbg !116
  br label %while.cond, !dbg !117

while.cond:                                       ; preds = %if.end48, %if.end
  %10 = load i32* %arg_ptr, align 4, !dbg !117
  %11 = load i32* %argc.addr, align 4, !dbg !117
  %cmp4 = icmp slt i32 %10, %11, !dbg !117
  br i1 %cmp4, label %while.body, label %while.end, !dbg !117

while.body:                                       ; preds = %while.cond
  %12 = load i32* %arg_ptr, align 4, !dbg !118
  %13 = load i8*** %argv.addr, align 4, !dbg !118
  %arrayidx5 = getelementptr inbounds i8** %13, i32 %12, !dbg !118
  %14 = load i8** %arrayidx5, !dbg !118
  %call6 = call arm_aapcscc  i32 @strcmp(i8* %14, i8* getelementptr inbounds ([3 x i8]* @.str2, i32 0, i32 0)) nounwind readonly, !dbg !118
  %tobool = icmp ne i32 %call6, 0, !dbg !118
  br i1 %tobool, label %if.else, label %if.then7, !dbg !118

if.then7:                                         ; preds = %while.body
  %15 = load i32* %arg_ptr, align 4, !dbg !120
  %inc = add nsw i32 %15, 1, !dbg !120
  store i32 %inc, i32* %arg_ptr, align 4, !dbg !120
  %16 = load i32* %arg_ptr, align 4, !dbg !122
  %17 = load i8*** %argv.addr, align 4, !dbg !122
  %arrayidx8 = getelementptr inbounds i8** %17, i32 %16, !dbg !122
  %18 = load i8** %arrayidx8, !dbg !122
  store i8* %18, i8** %filename, align 4, !dbg !122
  br label %if.end48, !dbg !123

if.else:                                          ; preds = %while.body
  %19 = load i32* %arg_ptr, align 4, !dbg !124
  %20 = load i8*** %argv.addr, align 4, !dbg !124
  %arrayidx9 = getelementptr inbounds i8** %20, i32 %19, !dbg !124
  %21 = load i8** %arrayidx9, !dbg !124
  %call10 = call arm_aapcscc  i32 @strcmp(i8* %21, i8* getelementptr inbounds ([3 x i8]* @.str3, i32 0, i32 0)) nounwind readonly, !dbg !124
  %tobool11 = icmp ne i32 %call10, 0, !dbg !124
  br i1 %tobool11, label %if.else16, label %if.then12, !dbg !124

if.then12:                                        ; preds = %if.else
  %22 = load i32* %arg_ptr, align 4, !dbg !125
  %inc13 = add nsw i32 %22, 1, !dbg !125
  store i32 %inc13, i32* %arg_ptr, align 4, !dbg !125
  %23 = load i32* %arg_ptr, align 4, !dbg !127
  %24 = load i8*** %argv.addr, align 4, !dbg !127
  %arrayidx14 = getelementptr inbounds i8** %24, i32 %23, !dbg !127
  %25 = load i8** %arrayidx14, !dbg !127
  %call15 = call arm_aapcscc  i32 @atoi(i8* %25) nounwind readonly, !dbg !127
  %26 = load i32** %num_levels.addr, align 4, !dbg !127
  store i32 %call15, i32* %26, !dbg !127
  br label %if.end47, !dbg !128

if.else16:                                        ; preds = %if.else
  %27 = load i32* %arg_ptr, align 4, !dbg !129
  %28 = load i8*** %argv.addr, align 4, !dbg !129
  %arrayidx17 = getelementptr inbounds i8** %28, i32 %27, !dbg !129
  %29 = load i8** %arrayidx17, !dbg !129
  %call18 = call arm_aapcscc  i32 @strcmp(i8* %29, i8* getelementptr inbounds ([3 x i8]* @.str4, i32 0, i32 0)) nounwind readonly, !dbg !129
  %tobool19 = icmp ne i32 %call18, 0, !dbg !129
  br i1 %tobool19, label %if.else24, label %if.then20, !dbg !129

if.then20:                                        ; preds = %if.else16
  %30 = load i32* %arg_ptr, align 4, !dbg !130
  %inc21 = add nsw i32 %30, 1, !dbg !130
  store i32 %inc21, i32* %arg_ptr, align 4, !dbg !130
  %31 = load i32* %arg_ptr, align 4, !dbg !132
  %32 = load i8*** %argv.addr, align 4, !dbg !132
  %arrayidx22 = getelementptr inbounds i8** %32, i32 %31, !dbg !132
  %33 = load i8** %arrayidx22, !dbg !132
  %call23 = call arm_aapcscc  double @atof(i8* %33) nounwind readonly, !dbg !132
  %34 = load double** %compression_factor.addr, align 4, !dbg !132
  store double %call23, double* %34, !dbg !132
  br label %if.end46, !dbg !133

if.else24:                                        ; preds = %if.else16
  %35 = load i32* %arg_ptr, align 4, !dbg !134
  %36 = load i8*** %argv.addr, align 4, !dbg !134
  %arrayidx25 = getelementptr inbounds i8** %36, i32 %35, !dbg !134
  %37 = load i8** %arrayidx25, !dbg !134
  %call26 = call arm_aapcscc  i32 @strcmp(i8* %37, i8* getelementptr inbounds ([3 x i8]* @.str5, i32 0, i32 0)) nounwind readonly, !dbg !134
  %tobool27 = icmp ne i32 %call26, 0, !dbg !134
  br i1 %tobool27, label %if.else32, label %if.then28, !dbg !134

if.then28:                                        ; preds = %if.else24
  %38 = load i32* %arg_ptr, align 4, !dbg !135
  %inc29 = add nsw i32 %38, 1, !dbg !135
  store i32 %inc29, i32* %arg_ptr, align 4, !dbg !135
  %39 = load i32* %arg_ptr, align 4, !dbg !137
  %40 = load i8*** %argv.addr, align 4, !dbg !137
  %arrayidx30 = getelementptr inbounds i8** %40, i32 %39, !dbg !137
  %41 = load i8** %arrayidx30, !dbg !137
  %call31 = call arm_aapcscc  i32 @atoi(i8* %41) nounwind readonly, !dbg !137
  %42 = load i32** %x_size.addr, align 4, !dbg !137
  store i32 %call31, i32* %42, !dbg !137
  br label %if.end45, !dbg !138

if.else32:                                        ; preds = %if.else24
  %43 = load i32* %arg_ptr, align 4, !dbg !139
  %44 = load i8*** %argv.addr, align 4, !dbg !139
  %arrayidx33 = getelementptr inbounds i8** %44, i32 %43, !dbg !139
  %45 = load i8** %arrayidx33, !dbg !139
  %call34 = call arm_aapcscc  i32 @strcmp(i8* %45, i8* getelementptr inbounds ([3 x i8]* @.str6, i32 0, i32 0)) nounwind readonly, !dbg !139
  %tobool35 = icmp ne i32 %call34, 0, !dbg !139
  br i1 %tobool35, label %if.else40, label %if.then36, !dbg !139

if.then36:                                        ; preds = %if.else32
  %46 = load i32* %arg_ptr, align 4, !dbg !140
  %inc37 = add nsw i32 %46, 1, !dbg !140
  store i32 %inc37, i32* %arg_ptr, align 4, !dbg !140
  %47 = load i32* %arg_ptr, align 4, !dbg !142
  %48 = load i8*** %argv.addr, align 4, !dbg !142
  %arrayidx38 = getelementptr inbounds i8** %48, i32 %47, !dbg !142
  %49 = load i8** %arrayidx38, !dbg !142
  %call39 = call arm_aapcscc  i32 @atoi(i8* %49) nounwind readonly, !dbg !142
  %50 = load i32** %y_size.addr, align 4, !dbg !142
  store i32 %call39, i32* %50, !dbg !142
  br label %if.end44, !dbg !143

if.else40:                                        ; preds = %if.else32
  %51 = load i32* %arg_ptr, align 4, !dbg !144
  %52 = load i8*** %argv.addr, align 4, !dbg !144
  %arrayidx41 = getelementptr inbounds i8** %52, i32 %51, !dbg !144
  %53 = load i8** %arrayidx41, !dbg !144
  %call42 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([27 x i8]* @.str7, i32 0, i32 0), i8* %53), !dbg !144
  %call43 = call arm_aapcscc  i32 @epic_usage(), !dbg !146
  br label %if.end44

if.end44:                                         ; preds = %if.else40, %if.then36
  br label %if.end45

if.end45:                                         ; preds = %if.end44, %if.then28
  br label %if.end46

if.end46:                                         ; preds = %if.end45, %if.then20
  br label %if.end47

if.end47:                                         ; preds = %if.end46, %if.then12
  br label %if.end48

if.end48:                                         ; preds = %if.end47, %if.then7
  %54 = load i32* %arg_ptr, align 4, !dbg !147
  %inc49 = add nsw i32 %54, 1, !dbg !147
  store i32 %inc49, i32* %arg_ptr, align 4, !dbg !147
  br label %while.cond, !dbg !148

while.end:                                        ; preds = %while.cond
  %55 = load %struct._IO_FILE.0** %infile, align 4, !dbg !149
  %call50 = call arm_aapcscc  i32 bitcast (i32 (...)* @PGMStream to i32 (%struct._IO_FILE.0*)*)(%struct._IO_FILE.0* %55), !dbg !149
  %tobool51 = icmp ne i32 %call50, 0, !dbg !149
  br i1 %tobool51, label %if.then52, label %if.else54, !dbg !149

if.then52:                                        ; preds = %while.end
  %56 = load %struct._IO_FILE.0** %infile, align 4, !dbg !150
  %57 = load i32** %x_size.addr, align 4, !dbg !150
  %58 = load i32** %y_size.addr, align 4, !dbg !150
  %call53 = call arm_aapcscc  float* bitcast (float* (...)* @ReadMatrixFromPGMStream to float* (%struct._IO_FILE.0*, i32*, i32*)*)(%struct._IO_FILE.0* %56, i32* %57, i32* %58), !dbg !150
  %59 = load float*** %image.addr, align 4, !dbg !150
  store float* %call53, float** %59, !dbg !150
  br label %if.end58, !dbg !152

if.else54:                                        ; preds = %while.end
  %60 = load i32** %x_size.addr, align 4, !dbg !153
  %61 = load i32* %60, !dbg !153
  %62 = load i32** %y_size.addr, align 4, !dbg !153
  %63 = load i32* %62, !dbg !153
  %mul = mul nsw i32 %61, %63, !dbg !153
  %mul55 = mul i32 %mul, 4, !dbg !153
  %call56 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul55), !dbg !153
  %64 = bitcast i8* %call56 to float*, !dbg !153
  %65 = load float*** %image.addr, align 4, !dbg !153
  store float* %64, float** %65, !dbg !153
  %66 = load %struct._IO_FILE.0** %infile, align 4, !dbg !155
  %67 = load float*** %image.addr, align 4, !dbg !155
  %68 = load float** %67, !dbg !155
  %69 = load i32** %x_size.addr, align 4, !dbg !155
  %70 = load i32* %69, !dbg !155
  %71 = load i32** %y_size.addr, align 4, !dbg !155
  %72 = load i32* %71, !dbg !155
  %call57 = call arm_aapcscc  i32 bitcast (i32 (...)* @read_byte_image to i32 (%struct._IO_FILE.0*, float*, i32, i32)*)(%struct._IO_FILE.0* %66, float* %68, i32 %70, i32 %72), !dbg !155
  br label %if.end58

if.end58:                                         ; preds = %if.else54, %if.then52
  %73 = load %struct._IO_FILE.0** %infile, align 4, !dbg !156
  %call59 = call arm_aapcscc  i32 @fclose(%struct._IO_FILE.0* %73), !dbg !156
  %74 = load i32** %num_levels.addr, align 4, !dbg !157
  %75 = load i32* %74, !dbg !157
  %cmp60 = icmp eq i32 %75, -1, !dbg !157
  br i1 %cmp60, label %if.then61, label %if.end75, !dbg !157

if.then61:                                        ; preds = %if.end58
  %76 = load i32** %num_levels.addr, align 4, !dbg !158
  store i32 1, i32* %76, !dbg !158
  br label %while.cond62, !dbg !160

while.cond62:                                     ; preds = %while.body73, %if.then61
  %77 = load i32** %x_size.addr, align 4, !dbg !160
  %78 = load i32* %77, !dbg !160
  %79 = load i32** %num_levels.addr, align 4, !dbg !160
  %80 = load i32* %79, !dbg !160
  %shl = shl i32 1, %80, !dbg !160
  %rem = srem i32 %78, %shl, !dbg !160
  %cmp63 = icmp eq i32 %rem, 0, !dbg !160
  br i1 %cmp63, label %land.lhs.true, label %land.end, !dbg !160

land.lhs.true:                                    ; preds = %while.cond62
  %81 = load i32** %y_size.addr, align 4, !dbg !160
  %82 = load i32* %81, !dbg !160
  %83 = load i32** %num_levels.addr, align 4, !dbg !160
  %84 = load i32* %83, !dbg !160
  %shl64 = shl i32 1, %84, !dbg !160
  %rem65 = srem i32 %82, %shl64, !dbg !160
  %cmp66 = icmp eq i32 %rem65, 0, !dbg !160
  br i1 %cmp66, label %land.lhs.true67, label %land.end, !dbg !160

land.lhs.true67:                                  ; preds = %land.lhs.true
  %85 = load i32** %x_size.addr, align 4, !dbg !160
  %86 = load i32* %85, !dbg !160
  %87 = load i32** %num_levels.addr, align 4, !dbg !160
  %88 = load i32* %87, !dbg !160
  %shl68 = shl i32 1, %88, !dbg !160
  %div = sdiv i32 %86, %shl68, !dbg !160
  %cmp69 = icmp sge i32 %div, 15, !dbg !160
  br i1 %cmp69, label %land.rhs, label %land.end, !dbg !160

land.rhs:                                         ; preds = %land.lhs.true67
  %89 = load i32** %y_size.addr, align 4, !dbg !160
  %90 = load i32* %89, !dbg !160
  %91 = load i32** %num_levels.addr, align 4, !dbg !160
  %92 = load i32* %91, !dbg !160
  %shl70 = shl i32 1, %92, !dbg !160
  %div71 = sdiv i32 %90, %shl70, !dbg !160
  %cmp72 = icmp sge i32 %div71, 15, !dbg !160
  br label %land.end

land.end:                                         ; preds = %land.rhs, %land.lhs.true67, %land.lhs.true, %while.cond62
  %93 = phi i1 [ false, %land.lhs.true67 ], [ false, %land.lhs.true ], [ false, %while.cond62 ], [ %cmp72, %land.rhs ]
  br i1 %93, label %while.body73, label %while.end74

while.body73:                                     ; preds = %land.end
  %94 = load i32** %num_levels.addr, align 4, !dbg !161
  %95 = load i32* %94, !dbg !161
  %add = add nsw i32 %95, 1, !dbg !161
  store i32 %add, i32* %94, !dbg !161
  br label %while.cond62, !dbg !163

while.end74:                                      ; preds = %land.end
  %96 = load i32** %num_levels.addr, align 4, !dbg !164
  %97 = load i32* %96, !dbg !164
  %sub = sub nsw i32 %97, 1, !dbg !164
  store i32 %sub, i32* %96, !dbg !164
  br label %if.end75, !dbg !165

if.end75:                                         ; preds = %while.end74, %if.end58
  %98 = load i32** %num_levels.addr, align 4, !dbg !166
  %99 = load i32* %98, !dbg !166
  %cmp76 = icmp slt i32 %99, 1, !dbg !166
  br i1 %cmp76, label %if.then77, label %if.end79, !dbg !166

if.then77:                                        ; preds = %if.end75
  %call78 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([42 x i8]* @.str8, i32 0, i32 0)), !dbg !167
  call arm_aapcscc  void @exit(i32 -1) noreturn nounwind, !dbg !169
  unreachable, !dbg !169

if.end79:                                         ; preds = %if.end75
  %100 = load i32** %num_levels.addr, align 4, !dbg !170
  %101 = load i32* %100, !dbg !170
  %cmp80 = icmp sgt i32 %101, 10, !dbg !170
  br i1 %cmp80, label %if.then81, label %if.end83, !dbg !170

if.then81:                                        ; preds = %if.end79
  %call82 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([53 x i8]* @.str9, i32 0, i32 0), i32 10), !dbg !171
  call arm_aapcscc  void @exit(i32 -1) noreturn nounwind, !dbg !173
  unreachable, !dbg !173

if.end83:                                         ; preds = %if.end79
  %102 = load i32** %x_size.addr, align 4, !dbg !174
  %103 = load i32* %102, !dbg !174
  %cmp84 = icmp sgt i32 %103, 65535, !dbg !174
  br i1 %cmp84, label %if.then86, label %lor.lhs.false, !dbg !174

lor.lhs.false:                                    ; preds = %if.end83
  %104 = load i32** %y_size.addr, align 4, !dbg !174
  %105 = load i32* %104, !dbg !174
  %cmp85 = icmp sgt i32 %105, 65535, !dbg !174
  br i1 %cmp85, label %if.then86, label %if.end88, !dbg !174

if.then86:                                        ; preds = %lor.lhs.false, %if.end83
  %106 = load i32** %x_size.addr, align 4, !dbg !175
  %107 = load i32* %106, !dbg !175
  %108 = load i32** %y_size.addr, align 4, !dbg !175
  %109 = load i32* %108, !dbg !175
  %call87 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([36 x i8]* @.str10, i32 0, i32 0), i32 %107, i32 %109), !dbg !175
  call arm_aapcscc  void @exit(i32 -1) noreturn nounwind, !dbg !177
  unreachable, !dbg !177

if.end88:                                         ; preds = %lor.lhs.false
  %110 = load i32** %x_size.addr, align 4, !dbg !178
  %111 = load i32* %110, !dbg !178
  %112 = load i32** %num_levels.addr, align 4, !dbg !178
  %113 = load i32* %112, !dbg !178
  %shl89 = shl i32 1, %113, !dbg !178
  %rem90 = srem i32 %111, %shl89, !dbg !178
  %cmp91 = icmp ne i32 %rem90, 0, !dbg !178
  br i1 %cmp91, label %if.then96, label %lor.lhs.false92, !dbg !178

lor.lhs.false92:                                  ; preds = %if.end88
  %114 = load i32** %y_size.addr, align 4, !dbg !178
  %115 = load i32* %114, !dbg !178
  %116 = load i32** %num_levels.addr, align 4, !dbg !178
  %117 = load i32* %116, !dbg !178
  %shl93 = shl i32 1, %117, !dbg !178
  %rem94 = srem i32 %115, %shl93, !dbg !178
  %cmp95 = icmp ne i32 %rem94, 0, !dbg !178
  br i1 %cmp95, label %if.then96, label %if.end98, !dbg !178

if.then96:                                        ; preds = %lor.lhs.false92, %if.end88
  %118 = load i32** %x_size.addr, align 4, !dbg !179
  %119 = load i32* %118, !dbg !179
  %120 = load i32** %y_size.addr, align 4, !dbg !179
  %121 = load i32* %120, !dbg !179
  %122 = load i32** %num_levels.addr, align 4, !dbg !179
  %123 = load i32* %122, !dbg !179
  %call97 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([78 x i8]* @.str11, i32 0, i32 0), i32 %119, i32 %121, i32 %123), !dbg !179
  call arm_aapcscc  void @exit(i32 -1) noreturn nounwind, !dbg !181
  unreachable, !dbg !181

if.end98:                                         ; preds = %lor.lhs.false92
  %124 = load i32** %x_size.addr, align 4, !dbg !182
  %125 = load i32* %124, !dbg !182
  %126 = load i32** %num_levels.addr, align 4, !dbg !182
  %127 = load i32* %126, !dbg !182
  %shl99 = shl i32 1, %127, !dbg !182
  %div100 = sdiv i32 %125, %shl99, !dbg !182
  %cmp101 = icmp slt i32 %div100, 15, !dbg !182
  br i1 %cmp101, label %if.then106, label %lor.lhs.false102, !dbg !182

lor.lhs.false102:                                 ; preds = %if.end98
  %128 = load i32** %y_size.addr, align 4, !dbg !182
  %129 = load i32* %128, !dbg !182
  %130 = load i32** %num_levels.addr, align 4, !dbg !182
  %131 = load i32* %130, !dbg !182
  %shl103 = shl i32 1, %131, !dbg !182
  %div104 = sdiv i32 %129, %shl103, !dbg !182
  %cmp105 = icmp slt i32 %div104, 15, !dbg !182
  br i1 %cmp105, label %if.then106, label %if.end108, !dbg !182

if.then106:                                       ; preds = %lor.lhs.false102, %if.end98
  %132 = load i32** %x_size.addr, align 4, !dbg !183
  %133 = load i32* %132, !dbg !183
  %134 = load i32** %y_size.addr, align 4, !dbg !183
  %135 = load i32* %134, !dbg !183
  %call107 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([76 x i8]* @.str12, i32 0, i32 0), i32 %133, i32 %135, i32 15), !dbg !183
  call arm_aapcscc  void @exit(i32 -1) noreturn nounwind, !dbg !185
  unreachable, !dbg !185

if.end108:                                        ; preds = %lor.lhs.false102
  %136 = load i8** %filename, align 4, !dbg !186
  %call109 = call arm_aapcscc  %struct._IO_FILE.0* bitcast (%struct._IO_FILE.0* (...)* @check_fopen to %struct._IO_FILE.0* (i8*, i8*)*)(i8* %136, i8* getelementptr inbounds ([2 x i8]* @.str13, i32 0, i32 0)), !dbg !186
  %137 = load %struct._IO_FILE.0*** %outfile.addr, align 4, !dbg !186
  store %struct._IO_FILE.0* %call109, %struct._IO_FILE.0** %137, !dbg !186
  %138 = load i32* %retval, !dbg !187
  ret i32 %138, !dbg !187
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc %struct._IO_FILE.0* @check_fopen(...)

declare arm_aapcscc i8* @concatenate(...)

declare arm_aapcscc i32 @strcmp(i8*, i8*) nounwind readonly

declare arm_aapcscc i32 @atoi(i8*) nounwind readonly

declare arm_aapcscc double @atof(i8*) nounwind readonly

declare arm_aapcscc i32 @printf(i8*, ...)

declare arm_aapcscc i32 @PGMStream(...)

declare arm_aapcscc float* @ReadMatrixFromPGMStream(...)

declare arm_aapcscc i8* @check_malloc(...)

declare arm_aapcscc i32 @read_byte_image(...)

declare arm_aapcscc i32 @fclose(%struct._IO_FILE.0*)

declare arm_aapcscc void @exit(i32) noreturn nounwind

define arm_aapcscc i32 @epic_usage() nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %call = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([79 x i8]* @.str14, i32 0, i32 0)), !dbg !188
  call arm_aapcscc  void @exit(i32 -1) noreturn nounwind, !dbg !190
  unreachable, !dbg !190

return:                                           ; No predecessors!
  %0 = load i32* %retval, !dbg !191
  ret i32 %0, !dbg !191
}

define arm_aapcscc i32 @parse_unepic_args(i32 %argc, i8** %argv, %struct._IO_FILE.0** %epicfile, i32* %num_levels, i32* %x_size, i32* %y_size, double* %scale_factor, %struct._IO_FILE.0** %outfile) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %argc.addr = alloca i32, align 4
  %argv.addr = alloca i8**, align 4
  %epicfile.addr = alloca %struct._IO_FILE.0**, align 4
  %num_levels.addr = alloca i32*, align 4
  %x_size.addr = alloca i32*, align 4
  %y_size.addr = alloca i32*, align 4
  %scale_factor.addr = alloca double*, align 4
  %outfile.addr = alloca %struct._IO_FILE.0**, align 4
  %filename = alloca i8*, align 4
  %the_byte = alloca i8, align 1
  store i32 %argc, i32* %argc.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %argc.addr}, metadata !192), !dbg !193
  store i8** %argv, i8*** %argv.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8*** %argv.addr}, metadata !194), !dbg !195
  store %struct._IO_FILE.0** %epicfile, %struct._IO_FILE.0*** %epicfile.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.0*** %epicfile.addr}, metadata !196), !dbg !197
  store i32* %num_levels, i32** %num_levels.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %num_levels.addr}, metadata !198), !dbg !199
  store i32* %x_size, i32** %x_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %x_size.addr}, metadata !200), !dbg !201
  store i32* %y_size, i32** %y_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %y_size.addr}, metadata !202), !dbg !203
  store double* %scale_factor, double** %scale_factor.addr, align 4
  call void @llvm.dbg.declare(metadata !{double** %scale_factor.addr}, metadata !204), !dbg !205
  store %struct._IO_FILE.0** %outfile, %struct._IO_FILE.0*** %outfile.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.0*** %outfile.addr}, metadata !206), !dbg !207
  call void @llvm.dbg.declare(metadata !{i8** %filename}, metadata !208), !dbg !210
  call void @llvm.dbg.declare(metadata !{i8* %the_byte}, metadata !211), !dbg !214
  %0 = load i32* %argc.addr, align 4, !dbg !215
  %cmp = icmp slt i32 %0, 2, !dbg !215
  br i1 %cmp, label %if.then, label %if.end, !dbg !215

if.then:                                          ; preds = %entry
  %call = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([35 x i8]* @.str15, i32 0, i32 0)), !dbg !216
  call arm_aapcscc  void @exit(i32 -1) noreturn nounwind, !dbg !218
  unreachable, !dbg !218

if.end:                                           ; preds = %entry
  %1 = load i8*** %argv.addr, align 4, !dbg !219
  %arrayidx = getelementptr inbounds i8** %1, i32 1, !dbg !219
  %2 = load i8** %arrayidx, !dbg !219
  %call1 = call arm_aapcscc  %struct._IO_FILE.0* bitcast (%struct._IO_FILE.0* (...)* @check_fopen to %struct._IO_FILE.0* (i8*, i8*)*)(i8* %2, i8* getelementptr inbounds ([2 x i8]* @.str, i32 0, i32 0)), !dbg !219
  %3 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !219
  store %struct._IO_FILE.0* %call1, %struct._IO_FILE.0** %3, !dbg !219
  %4 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !220
  %5 = load %struct._IO_FILE.0** %4, !dbg !220
  %call2 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.0* %5), !dbg !220
  %6 = load i8* @temp_byte, align 1, !dbg !221
  store i8 %6, i8* @temp_byte, align 1, !dbg !221
  %7 = load i8* @temp_byte, align 1, !dbg !222
  %conv = zext i8 %7 to i32, !dbg !222
  store i8 -1, i8* %the_byte, align 1, !dbg !222
  %cmp3 = icmp ne i32 %conv, 255, !dbg !222
  br i1 %cmp3, label %if.then5, label %if.end8, !dbg !222

if.then5:                                         ; preds = %if.end
  %8 = load i8*** %argv.addr, align 4, !dbg !223
  %arrayidx6 = getelementptr inbounds i8** %8, i32 1, !dbg !223
  %9 = load i8** %arrayidx6, !dbg !223
  %call7 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([34 x i8]* @.str16, i32 0, i32 0), i8* %9), !dbg !223
  call arm_aapcscc  void @exit(i32 -1) noreturn nounwind, !dbg !225
  unreachable, !dbg !225

if.end8:                                          ; preds = %if.end
  %10 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !226
  %11 = load %struct._IO_FILE.0** %10, !dbg !226
  %call9 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.0* %11), !dbg !226
  %12 = load i8* @temp_byte, align 1, !dbg !227
  store i8 %12, i8* @temp_byte, align 1, !dbg !227
  %13 = load i8* @temp_byte, align 1, !dbg !228
  %conv10 = zext i8 %13 to i32, !dbg !228
  %14 = load i32** %num_levels.addr, align 4, !dbg !228
  store i32 %conv10, i32* %14, !dbg !228
  %15 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !229
  %16 = load %struct._IO_FILE.0** %15, !dbg !229
  %call11 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.0* %16), !dbg !229
  %17 = load i8* @temp_byte, align 1, !dbg !230
  %conv12 = zext i8 %17 to i16, !dbg !230
  store i16 %conv12, i16* @temp_short, align 2, !dbg !230
  %18 = load i16* @temp_short, align 2, !dbg !231
  %conv13 = sext i16 %18 to i32, !dbg !231
  %shl = shl i32 %conv13, 8, !dbg !231
  %conv14 = trunc i32 %shl to i16, !dbg !231
  store i16 %conv14, i16* @temp_short, align 2, !dbg !231
  %19 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !232
  %20 = load %struct._IO_FILE.0** %19, !dbg !232
  %call15 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.0* %20), !dbg !232
  %21 = load i8* @temp_byte, align 1, !dbg !233
  %conv16 = zext i8 %21 to i32, !dbg !233
  %22 = load i16* @temp_short, align 2, !dbg !233
  %conv17 = sext i16 %22 to i32, !dbg !233
  %or = or i32 %conv17, %conv16, !dbg !233
  %conv18 = trunc i32 %or to i16, !dbg !233
  store i16 %conv18, i16* @temp_short, align 2, !dbg !233
  %23 = load i16* @temp_short, align 2, !dbg !234
  store i16 %23, i16* @temp_short, align 2, !dbg !234
  %24 = load i16* @temp_short, align 2, !dbg !235
  %conv19 = sext i16 %24 to i32, !dbg !235
  %25 = load i32** %x_size.addr, align 4, !dbg !235
  store i32 %conv19, i32* %25, !dbg !235
  %26 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !236
  %27 = load %struct._IO_FILE.0** %26, !dbg !236
  %call20 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.0* %27), !dbg !236
  %28 = load i8* @temp_byte, align 1, !dbg !237
  %conv21 = zext i8 %28 to i16, !dbg !237
  store i16 %conv21, i16* @temp_short, align 2, !dbg !237
  %29 = load i16* @temp_short, align 2, !dbg !238
  %conv22 = sext i16 %29 to i32, !dbg !238
  %shl23 = shl i32 %conv22, 8, !dbg !238
  %conv24 = trunc i32 %shl23 to i16, !dbg !238
  store i16 %conv24, i16* @temp_short, align 2, !dbg !238
  %30 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !239
  %31 = load %struct._IO_FILE.0** %30, !dbg !239
  %call25 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.0* %31), !dbg !239
  %32 = load i8* @temp_byte, align 1, !dbg !240
  %conv26 = zext i8 %32 to i32, !dbg !240
  %33 = load i16* @temp_short, align 2, !dbg !240
  %conv27 = sext i16 %33 to i32, !dbg !240
  %or28 = or i32 %conv27, %conv26, !dbg !240
  %conv29 = trunc i32 %or28 to i16, !dbg !240
  store i16 %conv29, i16* @temp_short, align 2, !dbg !240
  %34 = load i16* @temp_short, align 2, !dbg !241
  store i16 %34, i16* @temp_short, align 2, !dbg !241
  %35 = load i16* @temp_short, align 2, !dbg !242
  %conv30 = sext i16 %35 to i32, !dbg !242
  %36 = load i32** %y_size.addr, align 4, !dbg !242
  store i32 %conv30, i32* %36, !dbg !242
  %37 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !243
  %38 = load %struct._IO_FILE.0** %37, !dbg !243
  %call31 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.0* %38), !dbg !243
  %39 = load i8* @temp_byte, align 1, !dbg !244
  %conv32 = zext i8 %39 to i16, !dbg !244
  store i16 %conv32, i16* @temp_short, align 2, !dbg !244
  %40 = load i16* @temp_short, align 2, !dbg !245
  %conv33 = sext i16 %40 to i32, !dbg !245
  %shl34 = shl i32 %conv33, 8, !dbg !245
  %conv35 = trunc i32 %shl34 to i16, !dbg !245
  store i16 %conv35, i16* @temp_short, align 2, !dbg !245
  %41 = load %struct._IO_FILE.0*** %epicfile.addr, align 4, !dbg !246
  %42 = load %struct._IO_FILE.0** %41, !dbg !246
  %call36 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.0* %42), !dbg !246
  %43 = load i8* @temp_byte, align 1, !dbg !247
  %conv37 = zext i8 %43 to i32, !dbg !247
  %44 = load i16* @temp_short, align 2, !dbg !247
  %conv38 = sext i16 %44 to i32, !dbg !247
  %or39 = or i32 %conv38, %conv37, !dbg !247
  %conv40 = trunc i32 %or39 to i16, !dbg !247
  store i16 %conv40, i16* @temp_short, align 2, !dbg !247
  %45 = load i16* @temp_short, align 2, !dbg !248
  store i16 %45, i16* @temp_short, align 2, !dbg !248
  %46 = load i16* @temp_short, align 2, !dbg !249
  %conv41 = sitofp i16 %46 to double, !dbg !249
  %47 = load double** %scale_factor.addr, align 4, !dbg !249
  store double %conv41, double* %47, !dbg !249
  %48 = load i32* %argc.addr, align 4, !dbg !250
  %cmp42 = icmp eq i32 %48, 3, !dbg !250
  br i1 %cmp42, label %if.then44, label %if.else, !dbg !250

if.then44:                                        ; preds = %if.end8
  %49 = load i8*** %argv.addr, align 4, !dbg !251
  %arrayidx45 = getelementptr inbounds i8** %49, i32 2, !dbg !251
  %50 = load i8** %arrayidx45, !dbg !251
  store i8* %50, i8** %filename, align 4, !dbg !251
  br label %if.end48, !dbg !251

if.else:                                          ; preds = %if.end8
  %51 = load i8*** %argv.addr, align 4, !dbg !252
  %arrayidx46 = getelementptr inbounds i8** %51, i32 1, !dbg !252
  %52 = load i8** %arrayidx46, !dbg !252
  %call47 = call arm_aapcscc  i8* bitcast (i8* (...)* @concatenate to i8* (i8*, i8*)*)(i8* %52, i8* getelementptr inbounds ([3 x i8]* @.str17, i32 0, i32 0)), !dbg !252
  store i8* %call47, i8** %filename, align 4, !dbg !252
  br label %if.end48

if.end48:                                         ; preds = %if.else, %if.then44
  %53 = load i8** %filename, align 4, !dbg !253
  %call49 = call arm_aapcscc  %struct._IO_FILE.0* bitcast (%struct._IO_FILE.0* (...)* @check_fopen to %struct._IO_FILE.0* (i8*, i8*)*)(i8* %53, i8* getelementptr inbounds ([2 x i8]* @.str13, i32 0, i32 0)), !dbg !253
  %54 = load %struct._IO_FILE.0*** %outfile.addr, align 4, !dbg !253
  store %struct._IO_FILE.0* %call49, %struct._IO_FILE.0** %54, !dbg !253
  %55 = load i32* %retval, !dbg !254
  ret i32 %55, !dbg !254
}

declare arm_aapcscc i32 @fread(i8*, i32, i32, %struct._IO_FILE.0*)

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !12, metadata !15}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"parse_epic_args", metadata !"parse_epic_args", metadata !"", metadata !6, i32 47, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i32, i8**, float**, i32*, i32*, i32*, double*, %struct._IO_FILE.0**)* @parse_epic_args, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"parse_args.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{i32 720942, i32 0, metadata !6, metadata !"epic_usage", metadata !"epic_usage", metadata !"", metadata !6, i32 150, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 ()* @epic_usage, null, null, metadata !13} ; [ DW_TAG_subprogram ]
!13 = metadata !{metadata !14}
!14 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!15 = metadata !{i32 720942, i32 0, metadata !6, metadata !"parse_unepic_args", metadata !"parse_unepic_args", metadata !"", metadata !6, i32 161, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i32, i8**, %struct._IO_FILE.0**, i32*, i32*, i32*, double*, %struct._IO_FILE.0**)* @parse_unepic_args, null, null, metadata !16} ; [ DW_TAG_subprogram ]
!16 = metadata !{metadata !17}
!17 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!18 = metadata !{i32 721153, metadata !5, metadata !"argc", metadata !6, i32 16777258, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!19 = metadata !{i32 42, i32 7, metadata !5, null}
!20 = metadata !{i32 721153, metadata !5, metadata !"argv", metadata !6, i32 33554476, metadata !21, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!21 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !22} ; [ DW_TAG_pointer_type ]
!22 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !23} ; [ DW_TAG_pointer_type ]
!23 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!24 = metadata !{i32 44, i32 9, metadata !5, null}
!25 = metadata !{i32 721153, metadata !5, metadata !"image", metadata !6, i32 50331693, metadata !26, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!26 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !27} ; [ DW_TAG_pointer_type ]
!27 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !28} ; [ DW_TAG_pointer_type ]
!28 = metadata !{i32 720932, null, metadata !"float", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!29 = metadata !{i32 45, i32 11, metadata !5, null}
!30 = metadata !{i32 721153, metadata !5, metadata !"x_size", metadata !6, i32 67108906, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!31 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_pointer_type ]
!32 = metadata !{i32 42, i32 14, metadata !5, null}
!33 = metadata !{i32 721153, metadata !5, metadata !"y_size", metadata !6, i32 83886122, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!34 = metadata !{i32 42, i32 23, metadata !5, null}
!35 = metadata !{i32 721153, metadata !5, metadata !"num_levels", metadata !6, i32 100663338, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!36 = metadata !{i32 42, i32 32, metadata !5, null}
!37 = metadata !{i32 721153, metadata !5, metadata !"compression_factor", metadata !6, i32 117440555, metadata !38, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!38 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !39} ; [ DW_TAG_pointer_type ]
!39 = metadata !{i32 720932, null, metadata !"double", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!40 = metadata !{i32 43, i32 11, metadata !5, null}
!41 = metadata !{i32 721153, metadata !5, metadata !"outfile", metadata !6, i32 134217774, metadata !42, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!42 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !43} ; [ DW_TAG_pointer_type ]
!43 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !44} ; [ DW_TAG_pointer_type ]
!44 = metadata !{i32 720918, null, metadata !"FILE", metadata !6, i32 49, i64 0, i64 0, i64 0, i32 0, metadata !45} ; [ DW_TAG_typedef ]
!45 = metadata !{i32 720915, null, metadata !"_IO_FILE", metadata !46, i32 271, i64 1216, i64 64, i32 0, i32 0, null, metadata !47, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!46 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!47 = metadata !{metadata !48, metadata !49, metadata !50, metadata !51, metadata !52, metadata !53, metadata !54, metadata !55, metadata !56, metadata !57, metadata !58, metadata !59, metadata !60, metadata !68, metadata !69, metadata !70, metadata !71, metadata !74, metadata !76, metadata !78, metadata !82, metadata !84, metadata !88, metadata !89, metadata !90, metadata !91, metadata !92, metadata !95, metadata !96}
!48 = metadata !{i32 720909, metadata !45, metadata !"_flags", metadata !46, i32 272, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_member ]
!49 = metadata !{i32 720909, metadata !45, metadata !"_IO_read_ptr", metadata !46, i32 277, i64 32, i64 32, i64 32, i32 0, metadata !22} ; [ DW_TAG_member ]
!50 = metadata !{i32 720909, metadata !45, metadata !"_IO_read_end", metadata !46, i32 278, i64 32, i64 32, i64 64, i32 0, metadata !22} ; [ DW_TAG_member ]
!51 = metadata !{i32 720909, metadata !45, metadata !"_IO_read_base", metadata !46, i32 279, i64 32, i64 32, i64 96, i32 0, metadata !22} ; [ DW_TAG_member ]
!52 = metadata !{i32 720909, metadata !45, metadata !"_IO_write_base", metadata !46, i32 280, i64 32, i64 32, i64 128, i32 0, metadata !22} ; [ DW_TAG_member ]
!53 = metadata !{i32 720909, metadata !45, metadata !"_IO_write_ptr", metadata !46, i32 281, i64 32, i64 32, i64 160, i32 0, metadata !22} ; [ DW_TAG_member ]
!54 = metadata !{i32 720909, metadata !45, metadata !"_IO_write_end", metadata !46, i32 282, i64 32, i64 32, i64 192, i32 0, metadata !22} ; [ DW_TAG_member ]
!55 = metadata !{i32 720909, metadata !45, metadata !"_IO_buf_base", metadata !46, i32 283, i64 32, i64 32, i64 224, i32 0, metadata !22} ; [ DW_TAG_member ]
!56 = metadata !{i32 720909, metadata !45, metadata !"_IO_buf_end", metadata !46, i32 284, i64 32, i64 32, i64 256, i32 0, metadata !22} ; [ DW_TAG_member ]
!57 = metadata !{i32 720909, metadata !45, metadata !"_IO_save_base", metadata !46, i32 286, i64 32, i64 32, i64 288, i32 0, metadata !22} ; [ DW_TAG_member ]
!58 = metadata !{i32 720909, metadata !45, metadata !"_IO_backup_base", metadata !46, i32 287, i64 32, i64 32, i64 320, i32 0, metadata !22} ; [ DW_TAG_member ]
!59 = metadata !{i32 720909, metadata !45, metadata !"_IO_save_end", metadata !46, i32 288, i64 32, i64 32, i64 352, i32 0, metadata !22} ; [ DW_TAG_member ]
!60 = metadata !{i32 720909, metadata !45, metadata !"_markers", metadata !46, i32 290, i64 32, i64 32, i64 384, i32 0, metadata !61} ; [ DW_TAG_member ]
!61 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !62} ; [ DW_TAG_pointer_type ]
!62 = metadata !{i32 720915, null, metadata !"_IO_marker", metadata !46, i32 186, i64 96, i64 32, i32 0, i32 0, null, metadata !63, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!63 = metadata !{metadata !64, metadata !65, metadata !67}
!64 = metadata !{i32 720909, metadata !62, metadata !"_next", metadata !46, i32 187, i64 32, i64 32, i64 0, i32 0, metadata !61} ; [ DW_TAG_member ]
!65 = metadata !{i32 720909, metadata !62, metadata !"_sbuf", metadata !46, i32 188, i64 32, i64 32, i64 32, i32 0, metadata !66} ; [ DW_TAG_member ]
!66 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !45} ; [ DW_TAG_pointer_type ]
!67 = metadata !{i32 720909, metadata !62, metadata !"_pos", metadata !46, i32 192, i64 32, i64 32, i64 64, i32 0, metadata !9} ; [ DW_TAG_member ]
!68 = metadata !{i32 720909, metadata !45, metadata !"_chain", metadata !46, i32 292, i64 32, i64 32, i64 416, i32 0, metadata !66} ; [ DW_TAG_member ]
!69 = metadata !{i32 720909, metadata !45, metadata !"_fileno", metadata !46, i32 294, i64 32, i64 32, i64 448, i32 0, metadata !9} ; [ DW_TAG_member ]
!70 = metadata !{i32 720909, metadata !45, metadata !"_flags2", metadata !46, i32 298, i64 32, i64 32, i64 480, i32 0, metadata !9} ; [ DW_TAG_member ]
!71 = metadata !{i32 720909, metadata !45, metadata !"_old_offset", metadata !46, i32 300, i64 32, i64 32, i64 512, i32 0, metadata !72} ; [ DW_TAG_member ]
!72 = metadata !{i32 720918, null, metadata !"__off_t", metadata !46, i32 141, i64 0, i64 0, i64 0, i32 0, metadata !73} ; [ DW_TAG_typedef ]
!73 = metadata !{i32 720932, null, metadata !"long int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!74 = metadata !{i32 720909, metadata !45, metadata !"_cur_column", metadata !46, i32 304, i64 16, i64 16, i64 544, i32 0, metadata !75} ; [ DW_TAG_member ]
!75 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!76 = metadata !{i32 720909, metadata !45, metadata !"_vtable_offset", metadata !46, i32 305, i64 8, i64 8, i64 560, i32 0, metadata !77} ; [ DW_TAG_member ]
!77 = metadata !{i32 720932, null, metadata !"signed char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!78 = metadata !{i32 720909, metadata !45, metadata !"_shortbuf", metadata !46, i32 306, i64 8, i64 8, i64 568, i32 0, metadata !79} ; [ DW_TAG_member ]
!79 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8, i64 8, i32 0, i32 0, metadata !23, metadata !80, i32 0, i32 0} ; [ DW_TAG_array_type ]
!80 = metadata !{metadata !81}
!81 = metadata !{i32 720929, i64 0, i64 0}        ; [ DW_TAG_subrange_type ]
!82 = metadata !{i32 720909, metadata !45, metadata !"_lock", metadata !46, i32 310, i64 32, i64 32, i64 576, i32 0, metadata !83} ; [ DW_TAG_member ]
!83 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, null} ; [ DW_TAG_pointer_type ]
!84 = metadata !{i32 720909, metadata !45, metadata !"_offset", metadata !46, i32 319, i64 64, i64 64, i64 640, i32 0, metadata !85} ; [ DW_TAG_member ]
!85 = metadata !{i32 720918, null, metadata !"__off64_t", metadata !46, i32 142, i64 0, i64 0, i64 0, i32 0, metadata !86} ; [ DW_TAG_typedef ]
!86 = metadata !{i32 720918, null, metadata !"__quad_t", metadata !46, i32 56, i64 0, i64 0, i64 0, i32 0, metadata !87} ; [ DW_TAG_typedef ]
!87 = metadata !{i32 720932, null, metadata !"long long int", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!88 = metadata !{i32 720909, metadata !45, metadata !"__pad1", metadata !46, i32 328, i64 32, i64 32, i64 704, i32 0, metadata !83} ; [ DW_TAG_member ]
!89 = metadata !{i32 720909, metadata !45, metadata !"__pad2", metadata !46, i32 329, i64 32, i64 32, i64 736, i32 0, metadata !83} ; [ DW_TAG_member ]
!90 = metadata !{i32 720909, metadata !45, metadata !"__pad3", metadata !46, i32 330, i64 32, i64 32, i64 768, i32 0, metadata !83} ; [ DW_TAG_member ]
!91 = metadata !{i32 720909, metadata !45, metadata !"__pad4", metadata !46, i32 331, i64 32, i64 32, i64 800, i32 0, metadata !83} ; [ DW_TAG_member ]
!92 = metadata !{i32 720909, metadata !45, metadata !"__pad5", metadata !46, i32 332, i64 32, i64 32, i64 832, i32 0, metadata !93} ; [ DW_TAG_member ]
!93 = metadata !{i32 720918, null, metadata !"size_t", metadata !46, i32 211, i64 0, i64 0, i64 0, i32 0, metadata !94} ; [ DW_TAG_typedef ]
!94 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!95 = metadata !{i32 720909, metadata !45, metadata !"_mode", metadata !46, i32 334, i64 32, i64 32, i64 864, i32 0, metadata !9} ; [ DW_TAG_member ]
!96 = metadata !{i32 720909, metadata !45, metadata !"_unused2", metadata !46, i32 336, i64 320, i64 8, i64 896, i32 0, metadata !97} ; [ DW_TAG_member ]
!97 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !23, metadata !98, i32 0, i32 0} ; [ DW_TAG_array_type ]
!98 = metadata !{metadata !99}
!99 = metadata !{i32 720929, i64 0, i64 39}       ; [ DW_TAG_subrange_type ]
!100 = metadata !{i32 46, i32 10, metadata !5, null}
!101 = metadata !{i32 721152, metadata !102, metadata !"infile", metadata !6, i32 48, metadata !43, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!102 = metadata !{i32 720907, metadata !5, i32 47, i32 3, metadata !6, i32 6} ; [ DW_TAG_lexical_block ]
!103 = metadata !{i32 48, i32 9, metadata !102, null}
!104 = metadata !{i32 721152, metadata !102, metadata !"filename", metadata !6, i32 49, metadata !22, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!105 = metadata !{i32 49, i32 9, metadata !102, null}
!106 = metadata !{i32 721152, metadata !102, metadata !"arg_ptr", metadata !6, i32 50, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!107 = metadata !{i32 50, i32 7, metadata !102, null}
!108 = metadata !{i32 50, i32 16, metadata !102, null}
!109 = metadata !{i32 53, i32 3, metadata !102, null}
!110 = metadata !{i32 54, i32 3, metadata !102, null}
!111 = metadata !{i32 55, i32 3, metadata !102, null}
!112 = metadata !{i32 57, i32 3, metadata !102, null}
!113 = metadata !{i32 57, i32 17, metadata !102, null}
!114 = metadata !{i32 58, i32 3, metadata !102, null}
!115 = metadata !{i32 59, i32 12, metadata !102, null}
!116 = metadata !{i32 61, i32 14, metadata !102, null}
!117 = metadata !{i32 63, i32 3, metadata !102, null}
!118 = metadata !{i32 65, i32 12, metadata !119, null}
!119 = metadata !{i32 720907, metadata !102, i32 64, i32 7, metadata !6, i32 7} ; [ DW_TAG_lexical_block ]
!120 = metadata !{i32 67, i32 4, metadata !121, null}
!121 = metadata !{i32 720907, metadata !119, i32 66, i32 4, metadata !6, i32 8} ; [ DW_TAG_lexical_block ]
!122 = metadata !{i32 68, i32 4, metadata !121, null}
!123 = metadata !{i32 69, i32 4, metadata !121, null}
!124 = metadata !{i32 70, i32 17, metadata !119, null}
!125 = metadata !{i32 72, i32 4, metadata !126, null}
!126 = metadata !{i32 720907, metadata !119, i32 71, i32 4, metadata !6, i32 9} ; [ DW_TAG_lexical_block ]
!127 = metadata !{i32 73, i32 18, metadata !126, null}
!128 = metadata !{i32 74, i32 4, metadata !126, null}
!129 = metadata !{i32 75, i32 17, metadata !119, null}
!130 = metadata !{i32 77, i32 4, metadata !131, null}
!131 = metadata !{i32 720907, metadata !119, i32 76, i32 4, metadata !6, i32 10} ; [ DW_TAG_lexical_block ]
!132 = metadata !{i32 78, i32 35, metadata !131, null}
!133 = metadata !{i32 79, i32 4, metadata !131, null}
!134 = metadata !{i32 80, i32 17, metadata !119, null}
!135 = metadata !{i32 82, i32 4, metadata !136, null}
!136 = metadata !{i32 720907, metadata !119, i32 81, i32 4, metadata !6, i32 11} ; [ DW_TAG_lexical_block ]
!137 = metadata !{i32 83, i32 14, metadata !136, null}
!138 = metadata !{i32 84, i32 4, metadata !136, null}
!139 = metadata !{i32 85, i32 17, metadata !119, null}
!140 = metadata !{i32 87, i32 4, metadata !141, null}
!141 = metadata !{i32 720907, metadata !119, i32 86, i32 4, metadata !6, i32 12} ; [ DW_TAG_lexical_block ]
!142 = metadata !{i32 88, i32 14, metadata !141, null}
!143 = metadata !{i32 89, i32 4, metadata !141, null}
!144 = metadata !{i32 90, i32 14, metadata !145, null}
!145 = metadata !{i32 720907, metadata !119, i32 90, i32 12, metadata !6, i32 13} ; [ DW_TAG_lexical_block ]
!146 = metadata !{i32 90, i32 68, metadata !145, null}
!147 = metadata !{i32 91, i32 7, metadata !119, null}
!148 = metadata !{i32 92, i32 7, metadata !119, null}
!149 = metadata !{i32 95, i32 7, metadata !102, null}
!150 = metadata !{i32 97, i32 16, metadata !151, null}
!151 = metadata !{i32 720907, metadata !102, i32 96, i32 7, metadata !6, i32 14} ; [ DW_TAG_lexical_block ]
!152 = metadata !{i32 98, i32 7, metadata !151, null}
!153 = metadata !{i32 101, i32 26, metadata !154, null}
!154 = metadata !{i32 720907, metadata !102, i32 100, i32 7, metadata !6, i32 15} ; [ DW_TAG_lexical_block ]
!155 = metadata !{i32 102, i32 7, metadata !154, null}
!156 = metadata !{i32 104, i32 3, metadata !102, null}
!157 = metadata !{i32 106, i32 3, metadata !102, null}
!158 = metadata !{i32 108, i32 7, metadata !159, null}
!159 = metadata !{i32 720907, metadata !102, i32 107, i32 7, metadata !6, i32 16} ; [ DW_TAG_lexical_block ]
!160 = metadata !{i32 109, i32 7, metadata !159, null}
!161 = metadata !{i32 113, i32 4, metadata !162, null}
!162 = metadata !{i32 720907, metadata !159, i32 113, i32 2, metadata !6, i32 17} ; [ DW_TAG_lexical_block ]
!163 = metadata !{i32 113, i32 22, metadata !162, null}
!164 = metadata !{i32 114, i32 7, metadata !159, null}
!165 = metadata !{i32 115, i32 7, metadata !159, null}
!166 = metadata !{i32 116, i32 3, metadata !102, null}
!167 = metadata !{i32 118, i32 7, metadata !168, null}
!168 = metadata !{i32 720907, metadata !102, i32 117, i32 7, metadata !6, i32 18} ; [ DW_TAG_lexical_block ]
!169 = metadata !{i32 119, i32 7, metadata !168, null}
!170 = metadata !{i32 121, i32 3, metadata !102, null}
!171 = metadata !{i32 123, i32 7, metadata !172, null}
!172 = metadata !{i32 720907, metadata !102, i32 122, i32 7, metadata !6, i32 19} ; [ DW_TAG_lexical_block ]
!173 = metadata !{i32 124, i32 7, metadata !172, null}
!174 = metadata !{i32 126, i32 3, metadata !102, null}
!175 = metadata !{i32 129, i32 7, metadata !176, null}
!176 = metadata !{i32 720907, metadata !102, i32 128, i32 7, metadata !6, i32 20} ; [ DW_TAG_lexical_block ]
!177 = metadata !{i32 130, i32 7, metadata !176, null}
!178 = metadata !{i32 132, i32 3, metadata !102, null}
!179 = metadata !{i32 135, i32 7, metadata !180, null}
!180 = metadata !{i32 720907, metadata !102, i32 134, i32 7, metadata !6, i32 21} ; [ DW_TAG_lexical_block ]
!181 = metadata !{i32 137, i32 7, metadata !180, null}
!182 = metadata !{i32 139, i32 3, metadata !102, null}
!183 = metadata !{i32 142, i32 7, metadata !184, null}
!184 = metadata !{i32 720907, metadata !102, i32 141, i32 7, metadata !6, i32 22} ; [ DW_TAG_lexical_block ]
!185 = metadata !{i32 144, i32 7, metadata !184, null}
!186 = metadata !{i32 146, i32 14, metadata !102, null}
!187 = metadata !{i32 147, i32 3, metadata !102, null}
!188 = metadata !{i32 151, i32 3, metadata !189, null}
!189 = metadata !{i32 720907, metadata !12, i32 150, i32 3, metadata !6, i32 23} ; [ DW_TAG_lexical_block ]
!190 = metadata !{i32 152, i32 3, metadata !189, null}
!191 = metadata !{i32 153, i32 3, metadata !189, null}
!192 = metadata !{i32 721153, metadata !15, metadata !"argc", metadata !6, i32 16777373, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!193 = metadata !{i32 157, i32 7, metadata !15, null}
!194 = metadata !{i32 721153, metadata !15, metadata !"argv", metadata !6, i32 33554591, metadata !21, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!195 = metadata !{i32 159, i32 9, metadata !15, null}
!196 = metadata !{i32 721153, metadata !15, metadata !"epicfile", metadata !6, i32 50331808, metadata !42, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!197 = metadata !{i32 160, i32 10, metadata !15, null}
!198 = metadata !{i32 721153, metadata !15, metadata !"num_levels", metadata !6, i32 67109021, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!199 = metadata !{i32 157, i32 32, metadata !15, null}
!200 = metadata !{i32 721153, metadata !15, metadata !"x_size", metadata !6, i32 83886237, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!201 = metadata !{i32 157, i32 14, metadata !15, null}
!202 = metadata !{i32 721153, metadata !15, metadata !"y_size", metadata !6, i32 100663453, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!203 = metadata !{i32 157, i32 23, metadata !15, null}
!204 = metadata !{i32 721153, metadata !15, metadata !"scale_factor", metadata !6, i32 117440670, metadata !38, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!205 = metadata !{i32 158, i32 11, metadata !15, null}
!206 = metadata !{i32 721153, metadata !15, metadata !"outfile", metadata !6, i32 134217888, metadata !42, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!207 = metadata !{i32 160, i32 22, metadata !15, null}
!208 = metadata !{i32 721152, metadata !209, metadata !"filename", metadata !6, i32 162, metadata !22, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!209 = metadata !{i32 720907, metadata !15, i32 161, i32 3, metadata !6, i32 24} ; [ DW_TAG_lexical_block ]
!210 = metadata !{i32 162, i32 9, metadata !209, null}
!211 = metadata !{i32 721152, metadata !209, metadata !"the_byte", metadata !6, i32 163, metadata !212, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!212 = metadata !{i32 720918, null, metadata !"Byte", metadata !6, i32 76, i64 0, i64 0, i64 0, i32 0, metadata !213} ; [ DW_TAG_typedef ]
!213 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!214 = metadata !{i32 163, i32 8, metadata !209, null}
!215 = metadata !{i32 165, i32 3, metadata !209, null}
!216 = metadata !{i32 165, i32 19, metadata !217, null}
!217 = metadata !{i32 720907, metadata !209, i32 165, i32 17, metadata !6, i32 25} ; [ DW_TAG_lexical_block ]
!218 = metadata !{i32 165, i32 66, metadata !217, null}
!219 = metadata !{i32 167, i32 15, metadata !209, null}
!220 = metadata !{i32 169, i32 3, metadata !209, null}
!221 = metadata !{i32 169, i32 36, metadata !209, null}
!222 = metadata !{i32 170, i32 3, metadata !209, null}
!223 = metadata !{i32 172, i32 7, metadata !224, null}
!224 = metadata !{i32 720907, metadata !209, i32 171, i32 7, metadata !6, i32 26} ; [ DW_TAG_lexical_block ]
!225 = metadata !{i32 173, i32 7, metadata !224, null}
!226 = metadata !{i32 176, i32 3, metadata !209, null}
!227 = metadata !{i32 176, i32 36, metadata !209, null}
!228 = metadata !{i32 176, i32 57, metadata !209, null}
!229 = metadata !{i32 177, i32 3, metadata !209, null}
!230 = metadata !{i32 177, i32 36, metadata !209, null}
!231 = metadata !{i32 177, i32 60, metadata !209, null}
!232 = metadata !{i32 177, i32 77, metadata !209, null}
!233 = metadata !{i32 177, i32 110, metadata !209, null}
!234 = metadata !{i32 177, i32 135, metadata !209, null}
!235 = metadata !{i32 177, i32 158, metadata !209, null}
!236 = metadata !{i32 178, i32 3, metadata !209, null}
!237 = metadata !{i32 178, i32 36, metadata !209, null}
!238 = metadata !{i32 178, i32 60, metadata !209, null}
!239 = metadata !{i32 178, i32 77, metadata !209, null}
!240 = metadata !{i32 178, i32 110, metadata !209, null}
!241 = metadata !{i32 178, i32 135, metadata !209, null}
!242 = metadata !{i32 178, i32 158, metadata !209, null}
!243 = metadata !{i32 179, i32 3, metadata !209, null}
!244 = metadata !{i32 179, i32 36, metadata !209, null}
!245 = metadata !{i32 179, i32 60, metadata !209, null}
!246 = metadata !{i32 179, i32 77, metadata !209, null}
!247 = metadata !{i32 179, i32 110, metadata !209, null}
!248 = metadata !{i32 179, i32 135, metadata !209, null}
!249 = metadata !{i32 179, i32 158, metadata !209, null}
!250 = metadata !{i32 181, i32 3, metadata !209, null}
!251 = metadata !{i32 181, i32 18, metadata !209, null}
!252 = metadata !{i32 182, i32 19, metadata !209, null}
!253 = metadata !{i32 183, i32 14, metadata !209, null}
!254 = metadata !{i32 184, i32 3, metadata !209, null}
