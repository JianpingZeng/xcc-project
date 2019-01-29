; ModuleID = 'fileio.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct._IO_FILE.2 = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker.3*, %struct._IO_FILE.2*, i32, i32, i32, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i32, i32, [40 x i8] }
%struct._IO_marker.3 = type { %struct._IO_marker.3*, %struct._IO_FILE.2*, i32 }

@.str = private unnamed_addr constant [32 x i8] c"Error reading byte image file.\0A\00", align 1
@stdout = external global %struct._IO_FILE.2*
@.str1 = private unnamed_addr constant [56 x i8] c"ReadMatrixFromPGMStream(): File not P2 Or P5 PGM image\0A\00", align 1
@.str2 = private unnamed_addr constant [7 x i8] c" %d %d\00", align 1
@.str3 = private unnamed_addr constant [4 x i8] c"255\00", align 1
@.str4 = private unnamed_addr constant [63 x i8] c"ReadMatrixFromPGMStream():  File is not a 255-shade PGM image\0A\00", align 1
@.str5 = private unnamed_addr constant [61 x i8] c"ReadMatrixFromPGMStream():  Unable to alocate enough memory\0A\00", align 1
@.str6 = private unnamed_addr constant [4 x i8] c" %d\00", align 1
@.str7 = private unnamed_addr constant [32 x i8] c"Error writing byte image file.\0A\00", align 1
@.str8 = private unnamed_addr constant [4 x i8] c"P5\0A\00", align 1
@.str9 = private unnamed_addr constant [33 x i8] c"# CREATOR: UNEPIC, Version %.2f\0A\00", align 1
@.str10 = private unnamed_addr constant [7 x i8] c"%d %d\0A\00", align 1
@.str11 = private unnamed_addr constant [5 x i8] c"255\0A\00", align 1

define arm_aapcscc i32 @read_byte_image(%struct._IO_FILE.2* %stream, float* %image, i32 %x_size, i32 %y_size) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %stream.addr = alloca %struct._IO_FILE.2*, align 4
  %image.addr = alloca float*, align 4
  %x_size.addr = alloca i32, align 4
  %y_size.addr = alloca i32, align 4
  %i = alloca i32, align 4
  %im_size = alloca i32, align 4
  %byte_image = alloca i8*, align 4
  %status = alloca i32, align 4
  store %struct._IO_FILE.2* %stream, %struct._IO_FILE.2** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.2** %stream.addr}, metadata !28), !dbg !88
  store float* %image, float** %image.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %image.addr}, metadata !89), !dbg !90
  store i32 %x_size, i32* %x_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_size.addr}, metadata !91), !dbg !92
  store i32 %y_size, i32* %y_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_size.addr}, metadata !93), !dbg !94
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !95), !dbg !97
  call void @llvm.dbg.declare(metadata !{i32* %im_size}, metadata !98), !dbg !99
  %0 = load i32* %x_size.addr, align 4, !dbg !100
  %1 = load i32* %y_size.addr, align 4, !dbg !100
  %mul = mul nsw i32 %0, %1, !dbg !100
  store i32 %mul, i32* %im_size, align 4, !dbg !100
  call void @llvm.dbg.declare(metadata !{i8** %byte_image}, metadata !101), !dbg !105
  call void @llvm.dbg.declare(metadata !{i32* %status}, metadata !106), !dbg !107
  %2 = load i32* %im_size, align 4, !dbg !108
  %mul1 = mul i32 %2, 1, !dbg !108
  %call = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul1), !dbg !108
  store i8* %call, i8** %byte_image, align 4, !dbg !108
  %3 = load i8** %byte_image, align 4, !dbg !109
  %4 = load i32* %im_size, align 4, !dbg !109
  %5 = load %struct._IO_FILE.2** %stream.addr, align 4, !dbg !109
  %call2 = call arm_aapcscc  i32 @fread(i8* %3, i32 1, i32 %4, %struct._IO_FILE.2* %5), !dbg !109
  store i32 %call2, i32* %status, align 4, !dbg !109
  %6 = load i32* %status, align 4, !dbg !110
  %7 = load i32* %im_size, align 4, !dbg !110
  %cmp = icmp ne i32 %6, %7, !dbg !110
  br i1 %cmp, label %if.then, label %if.end, !dbg !110

if.then:                                          ; preds = %entry
  %call3 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([32 x i8]* @.str, i32 0, i32 0)), !dbg !111
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !113
  unreachable, !dbg !113

if.end:                                           ; preds = %entry
  store i32 0, i32* %i, align 4, !dbg !114
  br label %for.cond, !dbg !114

for.cond:                                         ; preds = %for.inc, %if.end
  %8 = load i32* %i, align 4, !dbg !114
  %9 = load i32* %im_size, align 4, !dbg !114
  %cmp4 = icmp slt i32 %8, %9, !dbg !114
  br i1 %cmp4, label %for.body, label %for.end, !dbg !114

for.body:                                         ; preds = %for.cond
  %10 = load i32* %i, align 4, !dbg !116
  %11 = load i8** %byte_image, align 4, !dbg !116
  %arrayidx = getelementptr inbounds i8* %11, i32 %10, !dbg !116
  %12 = load i8* %arrayidx, !dbg !116
  %conv = uitofp i8 %12 to float, !dbg !116
  %13 = load i32* %i, align 4, !dbg !116
  %14 = load float** %image.addr, align 4, !dbg !116
  %arrayidx5 = getelementptr inbounds float* %14, i32 %13, !dbg !116
  store float %conv, float* %arrayidx5, !dbg !116
  br label %for.inc, !dbg !116

for.inc:                                          ; preds = %for.body
  %15 = load i32* %i, align 4, !dbg !117
  %inc = add nsw i32 %15, 1, !dbg !117
  store i32 %inc, i32* %i, align 4, !dbg !117
  br label %for.cond, !dbg !117

for.end:                                          ; preds = %for.cond
  %16 = load i8** %byte_image, align 4, !dbg !118
  %call6 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %16), !dbg !118
  %17 = load i32* %retval, !dbg !119
  ret i32 %17, !dbg !119
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc i8* @check_malloc(...)

declare arm_aapcscc i32 @fread(i8*, i32, i32, %struct._IO_FILE.2*)

declare arm_aapcscc i32 @printf(i8*, ...)

declare arm_aapcscc void @exit(i32) noreturn

declare arm_aapcscc i32 @check_free(...)

define arm_aapcscc i32 @PGMStream(%struct._IO_FILE.2* %infile) nounwind uwtable {
entry:
  %infile.addr = alloca %struct._IO_FILE.2*, align 4
  %c = alloca i8, align 1
  store %struct._IO_FILE.2* %infile, %struct._IO_FILE.2** %infile.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.2** %infile.addr}, metadata !120), !dbg !121
  call void @llvm.dbg.declare(metadata !{i8* %c}, metadata !122), !dbg !124
  %0 = load %struct._IO_FILE.2** %infile.addr, align 4, !dbg !125
  %call = call arm_aapcscc  i32 @_IO_getc(%struct._IO_FILE.2* %0), !dbg !125
  %conv = trunc i32 %call to i8, !dbg !125
  store i8 %conv, i8* %c, align 1, !dbg !125
  %1 = load i8* %c, align 1, !dbg !126
  %conv1 = sext i8 %1 to i32, !dbg !126
  %2 = load %struct._IO_FILE.2** %infile.addr, align 4, !dbg !126
  %call2 = call arm_aapcscc  i32 @ungetc(i32 %conv1, %struct._IO_FILE.2* %2), !dbg !126
  %3 = load i8* %c, align 1, !dbg !127
  %conv3 = sext i8 %3 to i32, !dbg !127
  %cmp = icmp eq i32 %conv3, 80, !dbg !127
  %conv4 = zext i1 %cmp to i32, !dbg !127
  ret i32 %conv4, !dbg !127
}

declare arm_aapcscc i32 @_IO_getc(%struct._IO_FILE.2*)

declare arm_aapcscc i32 @ungetc(i32, %struct._IO_FILE.2*)

define arm_aapcscc float* @ReadMatrixFromPGMStream(%struct._IO_FILE.2* %Infile, i32* %xsize, i32* %ysize) nounwind uwtable {
entry:
  %retval = alloca float*, align 4
  %Infile.addr = alloca %struct._IO_FILE.2*, align 4
  %xsize.addr = alloca i32*, align 4
  %ysize.addr = alloca i32*, align 4
  %M = alloca float*, align 4
  %buf = alloca [80 x i8], align 1
  %rows = alloca i32, align 4
  %columns = alloca i32, align 4
  %i = alloca i32, align 4
  %value = alloca i32, align 4
  %ascii_pgm = alloca i32, align 4
  store %struct._IO_FILE.2* %Infile, %struct._IO_FILE.2** %Infile.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.2** %Infile.addr}, metadata !128), !dbg !129
  store i32* %xsize, i32** %xsize.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %xsize.addr}, metadata !130), !dbg !132
  store i32* %ysize, i32** %ysize.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %ysize.addr}, metadata !133), !dbg !134
  call void @llvm.dbg.declare(metadata !{float** %M}, metadata !135), !dbg !137
  call void @llvm.dbg.declare(metadata !{[80 x i8]* %buf}, metadata !138), !dbg !142
  call void @llvm.dbg.declare(metadata !{i32* %rows}, metadata !143), !dbg !144
  call void @llvm.dbg.declare(metadata !{i32* %columns}, metadata !145), !dbg !146
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !147), !dbg !148
  call void @llvm.dbg.declare(metadata !{i32* %value}, metadata !149), !dbg !150
  call void @llvm.dbg.declare(metadata !{i32* %ascii_pgm}, metadata !151), !dbg !152
  %0 = load %struct._IO_FILE.2** %Infile.addr, align 4, !dbg !153
  %cmp = icmp eq %struct._IO_FILE.2* %0, null, !dbg !153
  br i1 %cmp, label %if.then, label %if.end, !dbg !153

if.then:                                          ; preds = %entry
  store float* null, float** %retval, !dbg !154
  br label %return, !dbg !154

if.end:                                           ; preds = %entry
  %arraydecay = getelementptr inbounds [80 x i8]* %buf, i32 0, i32 0, !dbg !155
  %1 = load %struct._IO_FILE.2** %Infile.addr, align 4, !dbg !155
  %call = call arm_aapcscc  i8* @fgets(i8* %arraydecay, i32 80, %struct._IO_FILE.2* %1), !dbg !155
  %arrayidx = getelementptr inbounds [80 x i8]* %buf, i32 0, i32 1, !dbg !156
  %2 = load i8* %arrayidx, align 1, !dbg !156
  %conv = sext i8 %2 to i32, !dbg !156
  %cmp1 = icmp eq i32 %conv, 50, !dbg !156
  br i1 %cmp1, label %if.then3, label %if.else, !dbg !156

if.then3:                                         ; preds = %if.end
  store i32 1, i32* %ascii_pgm, align 4, !dbg !157
  br label %if.end12, !dbg !159

if.else:                                          ; preds = %if.end
  %arrayidx4 = getelementptr inbounds [80 x i8]* %buf, i32 0, i32 1, !dbg !160
  %3 = load i8* %arrayidx4, align 1, !dbg !160
  %conv5 = sext i8 %3 to i32, !dbg !160
  %cmp6 = icmp eq i32 %conv5, 53, !dbg !160
  br i1 %cmp6, label %if.then8, label %if.else9, !dbg !160

if.then8:                                         ; preds = %if.else
  store i32 0, i32* %ascii_pgm, align 4, !dbg !161
  br label %if.end11, !dbg !163

if.else9:                                         ; preds = %if.else
  %4 = load %struct._IO_FILE.2** @stdout, align 4, !dbg !164
  %call10 = call arm_aapcscc  i32 (%struct._IO_FILE.2*, i8*, ...)* @fprintf(%struct._IO_FILE.2* %4, i8* getelementptr inbounds ([56 x i8]* @.str1, i32 0, i32 0)), !dbg !164
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !166
  unreachable, !dbg !166

if.end11:                                         ; preds = %if.then8
  br label %if.end12

if.end12:                                         ; preds = %if.end11, %if.then3
  br label %do.body, !dbg !167

do.body:                                          ; preds = %do.cond, %if.end12
  %arraydecay13 = getelementptr inbounds [80 x i8]* %buf, i32 0, i32 0, !dbg !168
  %5 = load %struct._IO_FILE.2** %Infile.addr, align 4, !dbg !168
  %call14 = call arm_aapcscc  i8* @fgets(i8* %arraydecay13, i32 80, %struct._IO_FILE.2* %5), !dbg !168
  br label %do.cond, !dbg !170

do.cond:                                          ; preds = %do.body
  %arrayidx15 = getelementptr inbounds [80 x i8]* %buf, i32 0, i32 0, !dbg !170
  %6 = load i8* %arrayidx15, align 1, !dbg !170
  %conv16 = sext i8 %6 to i32, !dbg !170
  %cmp17 = icmp eq i32 %conv16, 35, !dbg !170
  br i1 %cmp17, label %do.body, label %do.end, !dbg !170

do.end:                                           ; preds = %do.cond
  %arraydecay19 = getelementptr inbounds [80 x i8]* %buf, i32 0, i32 0, !dbg !171
  %7 = load i32** %xsize.addr, align 4, !dbg !171
  %8 = load i32** %ysize.addr, align 4, !dbg !171
  %call20 = call arm_aapcscc  i32 (i8*, i8*, ...)* @__isoc99_sscanf(i8* %arraydecay19, i8* getelementptr inbounds ([7 x i8]* @.str2, i32 0, i32 0), i32* %7, i32* %8) nounwind, !dbg !171
  %arraydecay21 = getelementptr inbounds [80 x i8]* %buf, i32 0, i32 0, !dbg !172
  %9 = load %struct._IO_FILE.2** %Infile.addr, align 4, !dbg !172
  %call22 = call arm_aapcscc  i8* @fgets(i8* %arraydecay21, i32 80, %struct._IO_FILE.2* %9), !dbg !172
  %arraydecay23 = getelementptr inbounds [80 x i8]* %buf, i32 0, i32 0, !dbg !173
  %call24 = call arm_aapcscc  i32 bitcast (i32 (...)* @strncmp to i32 (i8*, i8*, i32)*)(i8* %arraydecay23, i8* getelementptr inbounds ([4 x i8]* @.str3, i32 0, i32 0), i32 3), !dbg !173
  %cmp25 = icmp ne i32 %call24, 0, !dbg !173
  br i1 %cmp25, label %if.then27, label %if.end29, !dbg !173

if.then27:                                        ; preds = %do.end
  %10 = load %struct._IO_FILE.2** @stdout, align 4, !dbg !174
  %call28 = call arm_aapcscc  i32 (%struct._IO_FILE.2*, i8*, ...)* @fprintf(%struct._IO_FILE.2* %10, i8* getelementptr inbounds ([63 x i8]* @.str4, i32 0, i32 0)), !dbg !174
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !176
  unreachable, !dbg !176

if.end29:                                         ; preds = %do.end
  %11 = load i32** %xsize.addr, align 4, !dbg !177
  %12 = load i32* %11, !dbg !177
  %13 = load i32** %ysize.addr, align 4, !dbg !177
  %14 = load i32* %13, !dbg !177
  %mul = mul nsw i32 %12, %14, !dbg !177
  %mul30 = mul i32 %mul, 4, !dbg !177
  %call31 = call arm_aapcscc  i8* @malloc(i32 %mul30), !dbg !177
  %15 = bitcast i8* %call31 to float*, !dbg !177
  store float* %15, float** %M, align 4, !dbg !177
  %16 = load float** %M, align 4, !dbg !178
  %cmp32 = icmp eq float* %16, null, !dbg !178
  br i1 %cmp32, label %if.then34, label %if.end37, !dbg !178

if.then34:                                        ; preds = %if.end29
  %17 = load %struct._IO_FILE.2** @stdout, align 4, !dbg !179
  %call35 = call arm_aapcscc  i32 (%struct._IO_FILE.2*, i8*, ...)* @fprintf(%struct._IO_FILE.2* %17, i8* getelementptr inbounds ([61 x i8]* @.str5, i32 0, i32 0)), !dbg !179
  %18 = load %struct._IO_FILE.2** %Infile.addr, align 4, !dbg !181
  %call36 = call arm_aapcscc  i32 bitcast (i32 (...)* @close to i32 (%struct._IO_FILE.2*)*)(%struct._IO_FILE.2* %18), !dbg !181
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !182
  unreachable, !dbg !182

if.end37:                                         ; preds = %if.end29
  store i32 0, i32* %i, align 4, !dbg !183
  br label %for.cond, !dbg !183

for.cond:                                         ; preds = %for.inc, %if.end37
  %19 = load i32* %i, align 4, !dbg !183
  %20 = load i32** %xsize.addr, align 4, !dbg !183
  %21 = load i32* %20, !dbg !183
  %22 = load i32** %ysize.addr, align 4, !dbg !183
  %23 = load i32* %22, !dbg !183
  %mul38 = mul nsw i32 %21, %23, !dbg !183
  %cmp39 = icmp slt i32 %19, %mul38, !dbg !183
  br i1 %cmp39, label %for.body, label %for.end, !dbg !183

for.body:                                         ; preds = %for.cond
  %24 = load i32* %ascii_pgm, align 4, !dbg !185
  %tobool = icmp ne i32 %24, 0, !dbg !185
  br i1 %tobool, label %if.then41, label %if.else43, !dbg !185

if.then41:                                        ; preds = %for.body
  %25 = load %struct._IO_FILE.2** %Infile.addr, align 4, !dbg !187
  %call42 = call arm_aapcscc  i32 (%struct._IO_FILE.2*, i8*, ...)* @__isoc99_fscanf(%struct._IO_FILE.2* %25, i8* getelementptr inbounds ([4 x i8]* @.str6, i32 0, i32 0), i32* %value), !dbg !187
  br label %if.end45, !dbg !189

if.else43:                                        ; preds = %for.body
  %26 = load %struct._IO_FILE.2** %Infile.addr, align 4, !dbg !190
  %call44 = call arm_aapcscc  i32 @fgetc(%struct._IO_FILE.2* %26), !dbg !190
  store i32 %call44, i32* %value, align 4, !dbg !190
  br label %if.end45

if.end45:                                         ; preds = %if.else43, %if.then41
  %27 = load i32* %value, align 4, !dbg !192
  %conv46 = sitofp i32 %27 to float, !dbg !192
  %28 = load i32* %i, align 4, !dbg !192
  %29 = load float** %M, align 4, !dbg !192
  %arrayidx47 = getelementptr inbounds float* %29, i32 %28, !dbg !192
  store float %conv46, float* %arrayidx47, !dbg !192
  br label %for.inc, !dbg !193

for.inc:                                          ; preds = %if.end45
  %30 = load i32* %i, align 4, !dbg !194
  %inc = add nsw i32 %30, 1, !dbg !194
  store i32 %inc, i32* %i, align 4, !dbg !194
  br label %for.cond, !dbg !194

for.end:                                          ; preds = %for.cond
  %31 = load float** %M, align 4, !dbg !195
  store float* %31, float** %retval, !dbg !195
  br label %return, !dbg !195

return:                                           ; preds = %for.end, %if.then
  %32 = load float** %retval, !dbg !196
  ret float* %32, !dbg !196
}

declare arm_aapcscc i8* @fgets(i8*, i32, %struct._IO_FILE.2*)

declare arm_aapcscc i32 @fprintf(%struct._IO_FILE.2*, i8*, ...)

declare arm_aapcscc i32 @__isoc99_sscanf(i8*, i8*, ...) nounwind

declare arm_aapcscc i32 @strncmp(...)

declare arm_aapcscc i8* @malloc(i32)

declare arm_aapcscc i32 @close(...)

declare arm_aapcscc i32 @__isoc99_fscanf(%struct._IO_FILE.2*, i8*, ...)

declare arm_aapcscc i32 @fgetc(%struct._IO_FILE.2*)

define arm_aapcscc i32 @write_byte_image(%struct._IO_FILE.2* %stream, i32* %image, i32 %x_size, i32 %y_size) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %stream.addr = alloca %struct._IO_FILE.2*, align 4
  %image.addr = alloca i32*, align 4
  %x_size.addr = alloca i32, align 4
  %y_size.addr = alloca i32, align 4
  %i = alloca i32, align 4
  %im_size = alloca i32, align 4
  %byte_image = alloca i8*, align 4
  %status = alloca i32, align 4
  store %struct._IO_FILE.2* %stream, %struct._IO_FILE.2** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.2** %stream.addr}, metadata !197), !dbg !198
  store i32* %image, i32** %image.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %image.addr}, metadata !199), !dbg !200
  store i32 %x_size, i32* %x_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_size.addr}, metadata !201), !dbg !202
  store i32 %y_size, i32* %y_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_size.addr}, metadata !203), !dbg !204
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !205), !dbg !207
  call void @llvm.dbg.declare(metadata !{i32* %im_size}, metadata !208), !dbg !209
  %0 = load i32* %x_size.addr, align 4, !dbg !210
  %1 = load i32* %y_size.addr, align 4, !dbg !210
  %mul = mul nsw i32 %0, %1, !dbg !210
  store i32 %mul, i32* %im_size, align 4, !dbg !210
  call void @llvm.dbg.declare(metadata !{i8** %byte_image}, metadata !211), !dbg !212
  call void @llvm.dbg.declare(metadata !{i32* %status}, metadata !213), !dbg !214
  %2 = load i32* %im_size, align 4, !dbg !215
  %mul1 = mul i32 %2, 1, !dbg !215
  %call = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul1), !dbg !215
  store i8* %call, i8** %byte_image, align 4, !dbg !215
  store i32 0, i32* %i, align 4, !dbg !216
  br label %for.cond, !dbg !216

for.cond:                                         ; preds = %for.inc, %entry
  %3 = load i32* %i, align 4, !dbg !216
  %4 = load i32* %im_size, align 4, !dbg !216
  %cmp = icmp slt i32 %3, %4, !dbg !216
  br i1 %cmp, label %for.body, label %for.end, !dbg !216

for.body:                                         ; preds = %for.cond
  %5 = load i32* %i, align 4, !dbg !218
  %6 = load i32** %image.addr, align 4, !dbg !218
  %arrayidx = getelementptr inbounds i32* %6, i32 %5, !dbg !218
  %7 = load i32* %arrayidx, !dbg !218
  %conv = trunc i32 %7 to i8, !dbg !218
  %8 = load i32* %i, align 4, !dbg !218
  %9 = load i8** %byte_image, align 4, !dbg !218
  %arrayidx2 = getelementptr inbounds i8* %9, i32 %8, !dbg !218
  store i8 %conv, i8* %arrayidx2, !dbg !218
  br label %for.inc, !dbg !218

for.inc:                                          ; preds = %for.body
  %10 = load i32* %i, align 4, !dbg !219
  %inc = add nsw i32 %10, 1, !dbg !219
  store i32 %inc, i32* %i, align 4, !dbg !219
  br label %for.cond, !dbg !219

for.end:                                          ; preds = %for.cond
  %11 = load i8** %byte_image, align 4, !dbg !220
  %12 = load i32* %im_size, align 4, !dbg !220
  %13 = load %struct._IO_FILE.2** %stream.addr, align 4, !dbg !220
  %call3 = call arm_aapcscc  i32 @fwrite(i8* %11, i32 1, i32 %12, %struct._IO_FILE.2* %13), !dbg !220
  store i32 %call3, i32* %status, align 4, !dbg !220
  %14 = load i8** %byte_image, align 4, !dbg !221
  %call4 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %14), !dbg !221
  %15 = load i32* %status, align 4, !dbg !222
  %16 = load i32* %im_size, align 4, !dbg !222
  %cmp5 = icmp ne i32 %15, %16, !dbg !222
  br i1 %cmp5, label %if.then, label %if.end, !dbg !222

if.then:                                          ; preds = %for.end
  %call7 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([32 x i8]* @.str7, i32 0, i32 0)), !dbg !223
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !225
  unreachable, !dbg !225

if.end:                                           ; preds = %for.end
  %17 = load i32* %retval, !dbg !226
  ret i32 %17, !dbg !226
}

declare arm_aapcscc i32 @fwrite(i8*, i32, i32, %struct._IO_FILE.2*)

define arm_aapcscc i32 @write_pgm_image(%struct._IO_FILE.2* %stream, i32* %image, i32 %x_size, i32 %y_size) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %stream.addr = alloca %struct._IO_FILE.2*, align 4
  %image.addr = alloca i32*, align 4
  %x_size.addr = alloca i32, align 4
  %y_size.addr = alloca i32, align 4
  %i = alloca i32, align 4
  %im_size = alloca i32, align 4
  %byte_image = alloca i8*, align 4
  %status = alloca i32, align 4
  store %struct._IO_FILE.2* %stream, %struct._IO_FILE.2** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.2** %stream.addr}, metadata !227), !dbg !228
  store i32* %image, i32** %image.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %image.addr}, metadata !229), !dbg !230
  store i32 %x_size, i32* %x_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_size.addr}, metadata !231), !dbg !232
  store i32 %y_size, i32* %y_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_size.addr}, metadata !233), !dbg !234
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !235), !dbg !237
  call void @llvm.dbg.declare(metadata !{i32* %im_size}, metadata !238), !dbg !239
  %0 = load i32* %x_size.addr, align 4, !dbg !240
  %1 = load i32* %y_size.addr, align 4, !dbg !240
  %mul = mul nsw i32 %0, %1, !dbg !240
  store i32 %mul, i32* %im_size, align 4, !dbg !240
  call void @llvm.dbg.declare(metadata !{i8** %byte_image}, metadata !241), !dbg !242
  call void @llvm.dbg.declare(metadata !{i32* %status}, metadata !243), !dbg !244
  %2 = load i32* %im_size, align 4, !dbg !245
  %mul1 = mul i32 %2, 1, !dbg !245
  %call = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul1), !dbg !245
  store i8* %call, i8** %byte_image, align 4, !dbg !245
  store i32 0, i32* %i, align 4, !dbg !246
  br label %for.cond, !dbg !246

for.cond:                                         ; preds = %for.inc, %entry
  %3 = load i32* %i, align 4, !dbg !246
  %4 = load i32* %im_size, align 4, !dbg !246
  %cmp = icmp slt i32 %3, %4, !dbg !246
  br i1 %cmp, label %for.body, label %for.end, !dbg !246

for.body:                                         ; preds = %for.cond
  %5 = load i32* %i, align 4, !dbg !248
  %6 = load i32** %image.addr, align 4, !dbg !248
  %arrayidx = getelementptr inbounds i32* %6, i32 %5, !dbg !248
  %7 = load i32* %arrayidx, !dbg !248
  %conv = trunc i32 %7 to i8, !dbg !248
  %8 = load i32* %i, align 4, !dbg !248
  %9 = load i8** %byte_image, align 4, !dbg !248
  %arrayidx2 = getelementptr inbounds i8* %9, i32 %8, !dbg !248
  store i8 %conv, i8* %arrayidx2, !dbg !248
  br label %for.inc, !dbg !248

for.inc:                                          ; preds = %for.body
  %10 = load i32* %i, align 4, !dbg !249
  %inc = add nsw i32 %10, 1, !dbg !249
  store i32 %inc, i32* %i, align 4, !dbg !249
  br label %for.cond, !dbg !249

for.end:                                          ; preds = %for.cond
  %11 = load %struct._IO_FILE.2** %stream.addr, align 4, !dbg !250
  %call3 = call arm_aapcscc  i32 (%struct._IO_FILE.2*, i8*, ...)* @fprintf(%struct._IO_FILE.2* %11, i8* getelementptr inbounds ([4 x i8]* @.str8, i32 0, i32 0)), !dbg !250
  %12 = load %struct._IO_FILE.2** %stream.addr, align 4, !dbg !251
  %call4 = call arm_aapcscc  i32 (%struct._IO_FILE.2*, i8*, ...)* @fprintf(%struct._IO_FILE.2* %12, i8* getelementptr inbounds ([33 x i8]* @.str9, i32 0, i32 0), double 1.100000e+00), !dbg !251
  %13 = load %struct._IO_FILE.2** %stream.addr, align 4, !dbg !252
  %14 = load i32* %x_size.addr, align 4, !dbg !252
  %15 = load i32* %y_size.addr, align 4, !dbg !252
  %call5 = call arm_aapcscc  i32 (%struct._IO_FILE.2*, i8*, ...)* @fprintf(%struct._IO_FILE.2* %13, i8* getelementptr inbounds ([7 x i8]* @.str10, i32 0, i32 0), i32 %14, i32 %15), !dbg !252
  %16 = load %struct._IO_FILE.2** %stream.addr, align 4, !dbg !253
  %call6 = call arm_aapcscc  i32 (%struct._IO_FILE.2*, i8*, ...)* @fprintf(%struct._IO_FILE.2* %16, i8* getelementptr inbounds ([5 x i8]* @.str11, i32 0, i32 0)), !dbg !253
  %17 = load i8** %byte_image, align 4, !dbg !254
  %18 = load i32* %im_size, align 4, !dbg !254
  %19 = load %struct._IO_FILE.2** %stream.addr, align 4, !dbg !254
  %call7 = call arm_aapcscc  i32 @fwrite(i8* %17, i32 1, i32 %18, %struct._IO_FILE.2* %19), !dbg !254
  store i32 %call7, i32* %status, align 4, !dbg !254
  %20 = load i8** %byte_image, align 4, !dbg !255
  %call8 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %20), !dbg !255
  %21 = load i32* %status, align 4, !dbg !256
  %22 = load i32* %im_size, align 4, !dbg !256
  %cmp9 = icmp ne i32 %21, %22, !dbg !256
  br i1 %cmp9, label %if.then, label %if.end, !dbg !256

if.then:                                          ; preds = %for.end
  %call11 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([32 x i8]* @.str7, i32 0, i32 0)), !dbg !257
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !259
  unreachable, !dbg !259

if.end:                                           ; preds = %for.end
  %23 = load i32* %retval, !dbg !260
  ret i32 %23, !dbg !260
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !12, metadata !15, metadata !22, metadata !25}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"read_byte_image", metadata !"read_byte_image", metadata !"", metadata !6, i32 39, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct._IO_FILE.2*, float*, i32, i32)* @read_byte_image, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"fileio.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{i32 720942, i32 0, metadata !6, metadata !"PGMStream", metadata !"PGMStream", metadata !"", metadata !6, i32 62, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct._IO_FILE.2*)* @PGMStream, null, null, metadata !13} ; [ DW_TAG_subprogram ]
!13 = metadata !{metadata !14}
!14 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!15 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ReadMatrixFromPGMStream", metadata !"ReadMatrixFromPGMStream", metadata !"", metadata !6, i32 80, metadata !16, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, float* (%struct._IO_FILE.2*, i32*, i32*)* @ReadMatrixFromPGMStream, null, null, metadata !20} ; [ DW_TAG_subprogram ]
!16 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !17, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!17 = metadata !{metadata !18}
!18 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !19} ; [ DW_TAG_pointer_type ]
!19 = metadata !{i32 720932, null, metadata !"float", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!20 = metadata !{metadata !21}
!21 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!22 = metadata !{i32 720942, i32 0, metadata !6, metadata !"write_byte_image", metadata !"write_byte_image", metadata !"", metadata !6, i32 142, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct._IO_FILE.2*, i32*, i32, i32)* @write_byte_image, null, null, metadata !23} ; [ DW_TAG_subprogram ]
!23 = metadata !{metadata !24}
!24 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!25 = metadata !{i32 720942, i32 0, metadata !6, metadata !"write_pgm_image", metadata !"write_pgm_image", metadata !"", metadata !6, i32 160, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct._IO_FILE.2*, i32*, i32, i32)* @write_pgm_image, null, null, metadata !26} ; [ DW_TAG_subprogram ]
!26 = metadata !{metadata !27}
!27 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!28 = metadata !{i32 721153, metadata !5, metadata !"stream", metadata !6, i32 16777252, metadata !29, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!29 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !30} ; [ DW_TAG_pointer_type ]
!30 = metadata !{i32 720918, null, metadata !"FILE", metadata !6, i32 49, i64 0, i64 0, i64 0, i32 0, metadata !31} ; [ DW_TAG_typedef ]
!31 = metadata !{i32 720915, null, metadata !"_IO_FILE", metadata !32, i32 271, i64 1216, i64 64, i32 0, i32 0, null, metadata !33, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!32 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!33 = metadata !{metadata !34, metadata !35, metadata !38, metadata !39, metadata !40, metadata !41, metadata !42, metadata !43, metadata !44, metadata !45, metadata !46, metadata !47, metadata !48, metadata !56, metadata !57, metadata !58, metadata !59, metadata !62, metadata !64, metadata !66, metadata !70, metadata !72, metadata !76, metadata !77, metadata !78, metadata !79, metadata !80, metadata !83, metadata !84}
!34 = metadata !{i32 720909, metadata !31, metadata !"_flags", metadata !32, i32 272, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_member ]
!35 = metadata !{i32 720909, metadata !31, metadata !"_IO_read_ptr", metadata !32, i32 277, i64 32, i64 32, i64 32, i32 0, metadata !36} ; [ DW_TAG_member ]
!36 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !37} ; [ DW_TAG_pointer_type ]
!37 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!38 = metadata !{i32 720909, metadata !31, metadata !"_IO_read_end", metadata !32, i32 278, i64 32, i64 32, i64 64, i32 0, metadata !36} ; [ DW_TAG_member ]
!39 = metadata !{i32 720909, metadata !31, metadata !"_IO_read_base", metadata !32, i32 279, i64 32, i64 32, i64 96, i32 0, metadata !36} ; [ DW_TAG_member ]
!40 = metadata !{i32 720909, metadata !31, metadata !"_IO_write_base", metadata !32, i32 280, i64 32, i64 32, i64 128, i32 0, metadata !36} ; [ DW_TAG_member ]
!41 = metadata !{i32 720909, metadata !31, metadata !"_IO_write_ptr", metadata !32, i32 281, i64 32, i64 32, i64 160, i32 0, metadata !36} ; [ DW_TAG_member ]
!42 = metadata !{i32 720909, metadata !31, metadata !"_IO_write_end", metadata !32, i32 282, i64 32, i64 32, i64 192, i32 0, metadata !36} ; [ DW_TAG_member ]
!43 = metadata !{i32 720909, metadata !31, metadata !"_IO_buf_base", metadata !32, i32 283, i64 32, i64 32, i64 224, i32 0, metadata !36} ; [ DW_TAG_member ]
!44 = metadata !{i32 720909, metadata !31, metadata !"_IO_buf_end", metadata !32, i32 284, i64 32, i64 32, i64 256, i32 0, metadata !36} ; [ DW_TAG_member ]
!45 = metadata !{i32 720909, metadata !31, metadata !"_IO_save_base", metadata !32, i32 286, i64 32, i64 32, i64 288, i32 0, metadata !36} ; [ DW_TAG_member ]
!46 = metadata !{i32 720909, metadata !31, metadata !"_IO_backup_base", metadata !32, i32 287, i64 32, i64 32, i64 320, i32 0, metadata !36} ; [ DW_TAG_member ]
!47 = metadata !{i32 720909, metadata !31, metadata !"_IO_save_end", metadata !32, i32 288, i64 32, i64 32, i64 352, i32 0, metadata !36} ; [ DW_TAG_member ]
!48 = metadata !{i32 720909, metadata !31, metadata !"_markers", metadata !32, i32 290, i64 32, i64 32, i64 384, i32 0, metadata !49} ; [ DW_TAG_member ]
!49 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !50} ; [ DW_TAG_pointer_type ]
!50 = metadata !{i32 720915, null, metadata !"_IO_marker", metadata !32, i32 186, i64 96, i64 32, i32 0, i32 0, null, metadata !51, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!51 = metadata !{metadata !52, metadata !53, metadata !55}
!52 = metadata !{i32 720909, metadata !50, metadata !"_next", metadata !32, i32 187, i64 32, i64 32, i64 0, i32 0, metadata !49} ; [ DW_TAG_member ]
!53 = metadata !{i32 720909, metadata !50, metadata !"_sbuf", metadata !32, i32 188, i64 32, i64 32, i64 32, i32 0, metadata !54} ; [ DW_TAG_member ]
!54 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !31} ; [ DW_TAG_pointer_type ]
!55 = metadata !{i32 720909, metadata !50, metadata !"_pos", metadata !32, i32 192, i64 32, i64 32, i64 64, i32 0, metadata !9} ; [ DW_TAG_member ]
!56 = metadata !{i32 720909, metadata !31, metadata !"_chain", metadata !32, i32 292, i64 32, i64 32, i64 416, i32 0, metadata !54} ; [ DW_TAG_member ]
!57 = metadata !{i32 720909, metadata !31, metadata !"_fileno", metadata !32, i32 294, i64 32, i64 32, i64 448, i32 0, metadata !9} ; [ DW_TAG_member ]
!58 = metadata !{i32 720909, metadata !31, metadata !"_flags2", metadata !32, i32 298, i64 32, i64 32, i64 480, i32 0, metadata !9} ; [ DW_TAG_member ]
!59 = metadata !{i32 720909, metadata !31, metadata !"_old_offset", metadata !32, i32 300, i64 32, i64 32, i64 512, i32 0, metadata !60} ; [ DW_TAG_member ]
!60 = metadata !{i32 720918, null, metadata !"__off_t", metadata !32, i32 141, i64 0, i64 0, i64 0, i32 0, metadata !61} ; [ DW_TAG_typedef ]
!61 = metadata !{i32 720932, null, metadata !"long int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!62 = metadata !{i32 720909, metadata !31, metadata !"_cur_column", metadata !32, i32 304, i64 16, i64 16, i64 544, i32 0, metadata !63} ; [ DW_TAG_member ]
!63 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!64 = metadata !{i32 720909, metadata !31, metadata !"_vtable_offset", metadata !32, i32 305, i64 8, i64 8, i64 560, i32 0, metadata !65} ; [ DW_TAG_member ]
!65 = metadata !{i32 720932, null, metadata !"signed char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!66 = metadata !{i32 720909, metadata !31, metadata !"_shortbuf", metadata !32, i32 306, i64 8, i64 8, i64 568, i32 0, metadata !67} ; [ DW_TAG_member ]
!67 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8, i64 8, i32 0, i32 0, metadata !37, metadata !68, i32 0, i32 0} ; [ DW_TAG_array_type ]
!68 = metadata !{metadata !69}
!69 = metadata !{i32 720929, i64 0, i64 0}        ; [ DW_TAG_subrange_type ]
!70 = metadata !{i32 720909, metadata !31, metadata !"_lock", metadata !32, i32 310, i64 32, i64 32, i64 576, i32 0, metadata !71} ; [ DW_TAG_member ]
!71 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, null} ; [ DW_TAG_pointer_type ]
!72 = metadata !{i32 720909, metadata !31, metadata !"_offset", metadata !32, i32 319, i64 64, i64 64, i64 640, i32 0, metadata !73} ; [ DW_TAG_member ]
!73 = metadata !{i32 720918, null, metadata !"__off64_t", metadata !32, i32 142, i64 0, i64 0, i64 0, i32 0, metadata !74} ; [ DW_TAG_typedef ]
!74 = metadata !{i32 720918, null, metadata !"__quad_t", metadata !32, i32 56, i64 0, i64 0, i64 0, i32 0, metadata !75} ; [ DW_TAG_typedef ]
!75 = metadata !{i32 720932, null, metadata !"long long int", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!76 = metadata !{i32 720909, metadata !31, metadata !"__pad1", metadata !32, i32 328, i64 32, i64 32, i64 704, i32 0, metadata !71} ; [ DW_TAG_member ]
!77 = metadata !{i32 720909, metadata !31, metadata !"__pad2", metadata !32, i32 329, i64 32, i64 32, i64 736, i32 0, metadata !71} ; [ DW_TAG_member ]
!78 = metadata !{i32 720909, metadata !31, metadata !"__pad3", metadata !32, i32 330, i64 32, i64 32, i64 768, i32 0, metadata !71} ; [ DW_TAG_member ]
!79 = metadata !{i32 720909, metadata !31, metadata !"__pad4", metadata !32, i32 331, i64 32, i64 32, i64 800, i32 0, metadata !71} ; [ DW_TAG_member ]
!80 = metadata !{i32 720909, metadata !31, metadata !"__pad5", metadata !32, i32 332, i64 32, i64 32, i64 832, i32 0, metadata !81} ; [ DW_TAG_member ]
!81 = metadata !{i32 720918, null, metadata !"size_t", metadata !32, i32 211, i64 0, i64 0, i64 0, i32 0, metadata !82} ; [ DW_TAG_typedef ]
!82 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!83 = metadata !{i32 720909, metadata !31, metadata !"_mode", metadata !32, i32 334, i64 32, i64 32, i64 864, i32 0, metadata !9} ; [ DW_TAG_member ]
!84 = metadata !{i32 720909, metadata !31, metadata !"_unused2", metadata !32, i32 336, i64 320, i64 8, i64 896, i32 0, metadata !85} ; [ DW_TAG_member ]
!85 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !37, metadata !86, i32 0, i32 0} ; [ DW_TAG_array_type ]
!86 = metadata !{metadata !87}
!87 = metadata !{i32 720929, i64 0, i64 39}       ; [ DW_TAG_subrange_type ]
!88 = metadata !{i32 36, i32 9, metadata !5, null}
!89 = metadata !{i32 721153, metadata !5, metadata !"image", metadata !6, i32 33554469, metadata !18, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!90 = metadata !{i32 37, i32 19, metadata !5, null}
!91 = metadata !{i32 721153, metadata !5, metadata !"x_size", metadata !6, i32 50331686, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!92 = metadata !{i32 38, i32 7, metadata !5, null}
!93 = metadata !{i32 721153, metadata !5, metadata !"y_size", metadata !6, i32 67108902, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!94 = metadata !{i32 38, i32 15, metadata !5, null}
!95 = metadata !{i32 721152, metadata !96, metadata !"i", metadata !6, i32 40, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!96 = metadata !{i32 720907, metadata !5, i32 39, i32 3, metadata !6, i32 27} ; [ DW_TAG_lexical_block ]
!97 = metadata !{i32 40, i32 16, metadata !96, null}
!98 = metadata !{i32 721152, metadata !96, metadata !"im_size", metadata !6, i32 40, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!99 = metadata !{i32 40, i32 19, metadata !96, null}
!100 = metadata !{i32 40, i32 44, metadata !96, null}
!101 = metadata !{i32 721152, metadata !96, metadata !"byte_image", metadata !6, i32 41, metadata !102, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!102 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !103} ; [ DW_TAG_pointer_type ]
!103 = metadata !{i32 720918, null, metadata !"Byte", metadata !6, i32 76, i64 0, i64 0, i64 0, i32 0, metadata !104} ; [ DW_TAG_typedef ]
!104 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!105 = metadata !{i32 41, i32 18, metadata !96, null}
!106 = metadata !{i32 721152, metadata !96, metadata !"status", metadata !6, i32 42, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!107 = metadata !{i32 42, i32 7, metadata !96, null}
!108 = metadata !{i32 44, i32 25, metadata !96, null}
!109 = metadata !{i32 45, i32 12, metadata !96, null}
!110 = metadata !{i32 46, i32 3, metadata !96, null}
!111 = metadata !{i32 46, i32 28, metadata !112, null}
!112 = metadata !{i32 720907, metadata !96, i32 46, i32 26, metadata !6, i32 28} ; [ DW_TAG_lexical_block ]
!113 = metadata !{i32 46, i32 73, metadata !112, null}
!114 = metadata !{i32 47, i32 8, metadata !115, null}
!115 = metadata !{i32 720907, metadata !96, i32 47, i32 3, metadata !6, i32 29} ; [ DW_TAG_lexical_block ]
!116 = metadata !{i32 47, i32 29, metadata !115, null}
!117 = metadata !{i32 47, i32 24, metadata !115, null}
!118 = metadata !{i32 48, i32 3, metadata !96, null}
!119 = metadata !{i32 49, i32 3, metadata !96, null}
!120 = metadata !{i32 721153, metadata !12, metadata !"infile", metadata !6, i32 16777277, metadata !29, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!121 = metadata !{i32 61, i32 9, metadata !12, null}
!122 = metadata !{i32 721152, metadata !123, metadata !"c", metadata !6, i32 63, metadata !37, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!123 = metadata !{i32 720907, metadata !12, i32 62, i32 3, metadata !6, i32 30} ; [ DW_TAG_lexical_block ]
!124 = metadata !{i32 63, i32 8, metadata !123, null}
!125 = metadata !{i32 64, i32 7, metadata !123, null}
!126 = metadata !{i32 65, i32 3, metadata !123, null}
!127 = metadata !{i32 66, i32 3, metadata !123, null}
!128 = metadata !{i32 721153, metadata !15, metadata !"Infile", metadata !6, i32 16777294, metadata !29, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!129 = metadata !{i32 78, i32 9, metadata !15, null}
!130 = metadata !{i32 721153, metadata !15, metadata !"xsize", metadata !6, i32 33554511, metadata !131, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!131 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_pointer_type ]
!132 = metadata !{i32 79, i32 8, metadata !15, null}
!133 = metadata !{i32 721153, metadata !15, metadata !"ysize", metadata !6, i32 50331727, metadata !131, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!134 = metadata !{i32 79, i32 16, metadata !15, null}
!135 = metadata !{i32 721152, metadata !136, metadata !"M", metadata !6, i32 81, metadata !18, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!136 = metadata !{i32 720907, metadata !15, i32 80, i32 3, metadata !6, i32 31} ; [ DW_TAG_lexical_block ]
!137 = metadata !{i32 81, i32 10, metadata !136, null}
!138 = metadata !{i32 721152, metadata !136, metadata !"buf", metadata !6, i32 82, metadata !139, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!139 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 640, i64 8, i32 0, i32 0, metadata !37, metadata !140, i32 0, i32 0} ; [ DW_TAG_array_type ]
!140 = metadata !{metadata !141}
!141 = metadata !{i32 720929, i64 0, i64 79}      ; [ DW_TAG_subrange_type ]
!142 = metadata !{i32 82, i32 8, metadata !136, null}
!143 = metadata !{i32 721152, metadata !136, metadata !"rows", metadata !6, i32 83, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!144 = metadata !{i32 83, i32 7, metadata !136, null}
!145 = metadata !{i32 721152, metadata !136, metadata !"columns", metadata !6, i32 83, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!146 = metadata !{i32 83, i32 12, metadata !136, null}
!147 = metadata !{i32 721152, metadata !136, metadata !"i", metadata !6, i32 84, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!148 = metadata !{i32 84, i32 7, metadata !136, null}
!149 = metadata !{i32 721152, metadata !136, metadata !"value", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!150 = metadata !{i32 85, i32 7, metadata !136, null}
!151 = metadata !{i32 721152, metadata !136, metadata !"ascii_pgm", metadata !6, i32 86, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!152 = metadata !{i32 86, i32 7, metadata !136, null}
!153 = metadata !{i32 88, i32 3, metadata !136, null}
!154 = metadata !{i32 88, i32 30, metadata !136, null}
!155 = metadata !{i32 89, i32 3, metadata !136, null}
!156 = metadata !{i32 91, i32 3, metadata !136, null}
!157 = metadata !{i32 92, i32 5, metadata !158, null}
!158 = metadata !{i32 720907, metadata !136, i32 91, i32 21, metadata !6, i32 32} ; [ DW_TAG_lexical_block ]
!159 = metadata !{i32 93, i32 3, metadata !158, null}
!160 = metadata !{i32 93, i32 10, metadata !136, null}
!161 = metadata !{i32 94, i32 5, metadata !162, null}
!162 = metadata !{i32 720907, metadata !136, i32 93, i32 28, metadata !6, i32 33} ; [ DW_TAG_lexical_block ]
!163 = metadata !{i32 95, i32 3, metadata !162, null}
!164 = metadata !{i32 96, i32 5, metadata !165, null}
!165 = metadata !{i32 720907, metadata !136, i32 95, i32 10, metadata !6, i32 34} ; [ DW_TAG_lexical_block ]
!166 = metadata !{i32 97, i32 5, metadata !165, null}
!167 = metadata !{i32 100, i32 3, metadata !136, null}
!168 = metadata !{i32 101, i32 5, metadata !169, null}
!169 = metadata !{i32 720907, metadata !136, i32 100, i32 6, metadata !6, i32 35} ; [ DW_TAG_lexical_block ]
!170 = metadata !{i32 102, i32 3, metadata !169, null}
!171 = metadata !{i32 104, i32 3, metadata !136, null}
!172 = metadata !{i32 106, i32 3, metadata !136, null}
!173 = metadata !{i32 107, i32 7, metadata !136, null}
!174 = metadata !{i32 108, i32 5, metadata !175, null}
!175 = metadata !{i32 720907, metadata !136, i32 107, i32 33, metadata !6, i32 36} ; [ DW_TAG_lexical_block ]
!176 = metadata !{i32 109, i32 5, metadata !175, null}
!177 = metadata !{i32 112, i32 17, metadata !136, null}
!178 = metadata !{i32 114, i32 3, metadata !136, null}
!179 = metadata !{i32 115, i32 5, metadata !180, null}
!180 = metadata !{i32 720907, metadata !136, i32 114, i32 25, metadata !6, i32 37} ; [ DW_TAG_lexical_block ]
!181 = metadata !{i32 116, i32 5, metadata !180, null}
!182 = metadata !{i32 117, i32 5, metadata !180, null}
!183 = metadata !{i32 120, i32 8, metadata !184, null}
!184 = metadata !{i32 720907, metadata !136, i32 120, i32 3, metadata !6, i32 38} ; [ DW_TAG_lexical_block ]
!185 = metadata !{i32 121, i32 5, metadata !186, null}
!186 = metadata !{i32 720907, metadata !184, i32 120, i32 36, metadata !6, i32 39} ; [ DW_TAG_lexical_block ]
!187 = metadata !{i32 122, i32 7, metadata !188, null}
!188 = metadata !{i32 720907, metadata !186, i32 121, i32 19, metadata !6, i32 40} ; [ DW_TAG_lexical_block ]
!189 = metadata !{i32 123, i32 5, metadata !188, null}
!190 = metadata !{i32 124, i32 15, metadata !191, null}
!191 = metadata !{i32 720907, metadata !186, i32 123, i32 12, metadata !6, i32 41} ; [ DW_TAG_lexical_block ]
!192 = metadata !{i32 126, i32 5, metadata !186, null}
!193 = metadata !{i32 127, i32 3, metadata !186, null}
!194 = metadata !{i32 120, i32 32, metadata !184, null}
!195 = metadata !{i32 129, i32 3, metadata !136, null}
!196 = metadata !{i32 130, i32 3, metadata !136, null}
!197 = metadata !{i32 721153, metadata !22, metadata !"stream", metadata !6, i32 16777355, metadata !29, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!198 = metadata !{i32 139, i32 9, metadata !22, null}
!199 = metadata !{i32 721153, metadata !22, metadata !"image", metadata !6, i32 33554572, metadata !131, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!200 = metadata !{i32 140, i32 17, metadata !22, null}
!201 = metadata !{i32 721153, metadata !22, metadata !"x_size", metadata !6, i32 50331789, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!202 = metadata !{i32 141, i32 7, metadata !22, null}
!203 = metadata !{i32 721153, metadata !22, metadata !"y_size", metadata !6, i32 67109005, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!204 = metadata !{i32 141, i32 15, metadata !22, null}
!205 = metadata !{i32 721152, metadata !206, metadata !"i", metadata !6, i32 143, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!206 = metadata !{i32 720907, metadata !22, i32 142, i32 3, metadata !6, i32 42} ; [ DW_TAG_lexical_block ]
!207 = metadata !{i32 143, i32 16, metadata !206, null}
!208 = metadata !{i32 721152, metadata !206, metadata !"im_size", metadata !6, i32 143, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!209 = metadata !{i32 143, i32 19, metadata !206, null}
!210 = metadata !{i32 143, i32 44, metadata !206, null}
!211 = metadata !{i32 721152, metadata !206, metadata !"byte_image", metadata !6, i32 144, metadata !102, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!212 = metadata !{i32 144, i32 18, metadata !206, null}
!213 = metadata !{i32 721152, metadata !206, metadata !"status", metadata !6, i32 145, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!214 = metadata !{i32 145, i32 7, metadata !206, null}
!215 = metadata !{i32 147, i32 25, metadata !206, null}
!216 = metadata !{i32 148, i32 8, metadata !217, null}
!217 = metadata !{i32 720907, metadata !206, i32 148, i32 3, metadata !6, i32 43} ; [ DW_TAG_lexical_block ]
!218 = metadata !{i32 148, i32 29, metadata !217, null}
!219 = metadata !{i32 148, i32 24, metadata !217, null}
!220 = metadata !{i32 149, i32 12, metadata !206, null}
!221 = metadata !{i32 150, i32 3, metadata !206, null}
!222 = metadata !{i32 152, i32 3, metadata !206, null}
!223 = metadata !{i32 152, i32 28, metadata !224, null}
!224 = metadata !{i32 720907, metadata !206, i32 152, i32 26, metadata !6, i32 44} ; [ DW_TAG_lexical_block ]
!225 = metadata !{i32 152, i32 73, metadata !224, null}
!226 = metadata !{i32 153, i32 3, metadata !206, null}
!227 = metadata !{i32 721153, metadata !25, metadata !"stream", metadata !6, i32 16777373, metadata !29, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!228 = metadata !{i32 157, i32 9, metadata !25, null}
!229 = metadata !{i32 721153, metadata !25, metadata !"image", metadata !6, i32 33554590, metadata !131, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!230 = metadata !{i32 158, i32 17, metadata !25, null}
!231 = metadata !{i32 721153, metadata !25, metadata !"x_size", metadata !6, i32 50331807, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!232 = metadata !{i32 159, i32 7, metadata !25, null}
!233 = metadata !{i32 721153, metadata !25, metadata !"y_size", metadata !6, i32 67109023, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!234 = metadata !{i32 159, i32 15, metadata !25, null}
!235 = metadata !{i32 721152, metadata !236, metadata !"i", metadata !6, i32 161, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!236 = metadata !{i32 720907, metadata !25, i32 160, i32 3, metadata !6, i32 45} ; [ DW_TAG_lexical_block ]
!237 = metadata !{i32 161, i32 16, metadata !236, null}
!238 = metadata !{i32 721152, metadata !236, metadata !"im_size", metadata !6, i32 161, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!239 = metadata !{i32 161, i32 19, metadata !236, null}
!240 = metadata !{i32 161, i32 44, metadata !236, null}
!241 = metadata !{i32 721152, metadata !236, metadata !"byte_image", metadata !6, i32 162, metadata !102, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!242 = metadata !{i32 162, i32 18, metadata !236, null}
!243 = metadata !{i32 721152, metadata !236, metadata !"status", metadata !6, i32 163, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!244 = metadata !{i32 163, i32 7, metadata !236, null}
!245 = metadata !{i32 165, i32 25, metadata !236, null}
!246 = metadata !{i32 166, i32 8, metadata !247, null}
!247 = metadata !{i32 720907, metadata !236, i32 166, i32 3, metadata !6, i32 46} ; [ DW_TAG_lexical_block ]
!248 = metadata !{i32 166, i32 29, metadata !247, null}
!249 = metadata !{i32 166, i32 24, metadata !247, null}
!250 = metadata !{i32 169, i32 3, metadata !236, null}
!251 = metadata !{i32 170, i32 3, metadata !236, null}
!252 = metadata !{i32 171, i32 3, metadata !236, null}
!253 = metadata !{i32 172, i32 3, metadata !236, null}
!254 = metadata !{i32 174, i32 12, metadata !236, null}
!255 = metadata !{i32 175, i32 3, metadata !236, null}
!256 = metadata !{i32 177, i32 3, metadata !236, null}
!257 = metadata !{i32 177, i32 28, metadata !258, null}
!258 = metadata !{i32 720907, metadata !236, i32 177, i32 26, metadata !6, i32 47} ; [ DW_TAG_lexical_block ]
!259 = metadata !{i32 177, i32 73, metadata !258, null}
!260 = metadata !{i32 178, i32 3, metadata !236, null}
