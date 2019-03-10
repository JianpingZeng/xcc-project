; ModuleID = 'epic.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct._IO_FILE.6 = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker.7*, %struct._IO_FILE.6*, i32, i32, i32, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i32, i32, [40 x i8] }
%struct._IO_marker.7 = type { %struct._IO_marker.7*, %struct._IO_FILE.6*, i32 }
%struct.timeval = type { i32, i32 }

@stdout = external global %struct._IO_FILE.6*
@.str = private unnamed_addr constant [23 x i8] c"Xsize: %d, Ysize: %d.\0A\00", align 1
@stderr = external global %struct._IO_FILE.6*
@.str1 = private unnamed_addr constant [93 x i8] c"Error: attempting to construct pyramid with %d levels.\0A      Are your image dimensions odd?\0A\00", align 1
@.str2 = private unnamed_addr constant [33 x i8] c"Building pyramid, %d levels ...\0A\00", align 1
@lo_filter = internal global [15 x float] [float 0xBF54707CA0000000, float 0xBF647095A0000000, float 0x3F81E188A0000000, float 0x3F946FDBE0000000, float 0xBFA9DEEFE0000000, float 0xBFBEDC6DA0000000, float 0x3FD2C14240000000, float 0x3FE698FEC0000000, float 0x3FD2C14240000000, float 0xBFBEDC6DA0000000, float 0xBFA9DEEFE0000000, float 0x3F946FDBE0000000, float 0x3F81E188A0000000, float 0xBF647095A0000000, float 0xBF54707CA0000000], align 4
@hi_filter = internal global [15 x float] [float 0x3F54707CA0000000, float 0xBF647095A0000000, float 0xBF81E188A0000000, float 0x3F946FDBE0000000, float 0x3FA9DEEFE0000000, float 0xBFBEDC6DA0000000, float 0xBFD2C14240000000, float 0x3FE698FEC0000000, float 0xBFD2C14240000000, float 0xBFBEDC6DA0000000, float 0x3FA9DEEFE0000000, float 0x3F946FDBE0000000, float 0xBF81E188A0000000, float 0xBF647095A0000000, float 0x3F54707CA0000000], align 4
@.str3 = private unnamed_addr constant [32 x i8] c"Quantizing, binsize = %4lf ...\0A\00", align 1
@.str4 = private unnamed_addr constant [23 x i8] c"Run-length coding ...\0A\00", align 1
@.str5 = private unnamed_addr constant [21 x i8] c"Huffman coding ... \0A\00", align 1
@.str6 = private unnamed_addr constant [74 x i8] c"Low_pass_size: %d,  Symbol_stream_length: %d,  Huffman_stream_length: %d\0A\00", align 1
@.str7 = private unnamed_addr constant [23 x i8] c"Writing EPIC file ...\0A\00", align 1
@temp_byte = external global i8
@temp_short = external global i16
@.str8 = private unnamed_addr constant [25 x i8] c"Stored %ld byte header.\0A\00", align 1
@.str9 = private unnamed_addr constant [41 x i8] c"Stored %ld bytes of Raw (lowpass) Data.\0A\00", align 1
@.str10 = private unnamed_addr constant [35 x i8] c"Stored %ld bytes of Binsize Info.\0A\00", align 1
@temp_int = external global i32
@.str11 = private unnamed_addr constant [32 x i8] c"Stored %ld byte Huffman tree .\0A\00", align 1
@.str12 = private unnamed_addr constant [35 x i8] c"Stored %ld bytes of encoded data.\0A\00", align 1
@.str13 = private unnamed_addr constant [70 x i8] c"Storage total: %ld bytes (%3f bits/pixel)\0ACompression ratio = %2f:1.\0A\00", align 1
@ss_time_start = common global %struct.timeval zeroinitializer, align 4
@ss_time_end = common global %struct.timeval zeroinitializer, align 4

define arm_aapcscc i32 @main(i32 %argc, i8** %argv) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %argc.addr = alloca i32, align 4
  %argv.addr = alloca i8**, align 4
  %outfile = alloca %struct._IO_FILE.6*, align 4
  %image = alloca float*, align 4
  %scale_factor = alloca i16, align 2
  %i = alloca i32, align 4
  %j = alloca i32, align 4
  %level = alloca i32, align 4
  %im_count = alloca i32, align 4
  %x_size = alloca i32, align 4
  %y_size = alloca i32, align 4
  %num_levels = alloca i32, align 4
  %lopass_im_size = alloca i32, align 4
  %temp = alloca i32, align 4
  %compression_factor = alloca double, align 8
  %huffman_tree_size = alloca i32, align 4
  %bin_size = alloca i16*, align 4
  %q_pyr = alloca i16*, align 4
  %symbol_stream_length = alloca i32, align 4
  %encoded_stream_length = alloca i32, align 4
  %symbol_stream = alloca i16*, align 4
  %huffman_tree = alloca i8*, align 4
  %encoded_stream = alloca i8*, align 4
  store i32 0, i32* %retval
  store i32 %argc, i32* %argc.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %argc.addr}, metadata !31), !dbg !32
  store i8** %argv, i8*** %argv.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8*** %argv.addr}, metadata !33), !dbg !37
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.6** %outfile}, metadata !38), !dbg !96
  call void @llvm.dbg.declare(metadata !{float** %image}, metadata !97), !dbg !99
  call void @llvm.dbg.declare(metadata !{i16* %scale_factor}, metadata !100), !dbg !101
  store i16 128, i16* %scale_factor, align 2, !dbg !102
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !103), !dbg !104
  call void @llvm.dbg.declare(metadata !{i32* %j}, metadata !105), !dbg !106
  call void @llvm.dbg.declare(metadata !{i32* %level}, metadata !107), !dbg !108
  call void @llvm.dbg.declare(metadata !{i32* %im_count}, metadata !109), !dbg !110
  call void @llvm.dbg.declare(metadata !{i32* %x_size}, metadata !111), !dbg !112
  call void @llvm.dbg.declare(metadata !{i32* %y_size}, metadata !113), !dbg !114
  call void @llvm.dbg.declare(metadata !{i32* %num_levels}, metadata !115), !dbg !116
  call void @llvm.dbg.declare(metadata !{i32* %lopass_im_size}, metadata !117), !dbg !118
  call void @llvm.dbg.declare(metadata !{i32* %temp}, metadata !119), !dbg !120
  call void @llvm.dbg.declare(metadata !{double* %compression_factor}, metadata !121), !dbg !123
  call void @llvm.dbg.declare(metadata !{i32* %huffman_tree_size}, metadata !124), !dbg !125
  call void @llvm.dbg.declare(metadata !{i16** %bin_size}, metadata !126), !dbg !129
  call void @llvm.dbg.declare(metadata !{i16** %q_pyr}, metadata !130), !dbg !134
  call void @llvm.dbg.declare(metadata !{i32* %symbol_stream_length}, metadata !135), !dbg !136
  call void @llvm.dbg.declare(metadata !{i32* %encoded_stream_length}, metadata !137), !dbg !138
  call void @llvm.dbg.declare(metadata !{i16** %symbol_stream}, metadata !139), !dbg !142
  call void @llvm.dbg.declare(metadata !{i8** %huffman_tree}, metadata !143), !dbg !147
  call void @llvm.dbg.declare(metadata !{i8** %encoded_stream}, metadata !148), !dbg !151
  %0 = load i32* %argc.addr, align 4, !dbg !152
  %1 = load i8*** %argv.addr, align 4, !dbg !152
  %call = call arm_aapcscc  i32 bitcast (i32 (...)* @parse_epic_args to i32 (i32, i8**, float**, i32*, i32*, i32*, double*, %struct._IO_FILE.6**)*)(i32 %0, i8** %1, float** %image, i32* %x_size, i32* %y_size, i32* %num_levels, double* %compression_factor, %struct._IO_FILE.6** %outfile), !dbg !152
  %2 = load i32* %x_size, align 4, !dbg !153
  %3 = load i32* %y_size, align 4, !dbg !153
  %mul = mul nsw i32 %2, %3, !dbg !153
  %4 = load i32* %num_levels, align 4, !dbg !153
  %mul1 = mul nsw i32 2, %4, !dbg !153
  %shl = shl i32 1, %mul1, !dbg !153
  %div = sdiv i32 %mul, %shl, !dbg !153
  store i32 %div, i32* %lopass_im_size, align 4, !dbg !153
  %5 = load i32* %x_size, align 4, !dbg !154
  %6 = load i32* %y_size, align 4, !dbg !154
  %mul2 = mul nsw i32 %5, %6, !dbg !154
  %mul3 = mul i32 %mul2, 2, !dbg !154
  %call4 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul3), !dbg !154
  %7 = bitcast i8* %call4 to i16*, !dbg !154
  store i16* %7, i16** %q_pyr, align 4, !dbg !154
  %8 = load i32* %num_levels, align 4, !dbg !155
  %mul5 = mul nsw i32 3, %8, !dbg !155
  %add = add nsw i32 %mul5, 1, !dbg !155
  %mul6 = mul i32 %add, 2, !dbg !155
  %call7 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul6), !dbg !155
  %9 = bitcast i8* %call7 to i16*, !dbg !155
  store i16* %9, i16** %bin_size, align 4, !dbg !155
  %10 = load %struct._IO_FILE.6** @stdout, align 4, !dbg !156
  %11 = load i32* %x_size, align 4, !dbg !156
  %12 = load i32* %y_size, align 4, !dbg !156
  %call8 = call arm_aapcscc  i32 (%struct._IO_FILE.6*, i8*, ...)* @fprintf(%struct._IO_FILE.6* %10, i8* getelementptr inbounds ([23 x i8]* @.str, i32 0, i32 0), i32 %11, i32 %12), !dbg !156
  %13 = load i32* %num_levels, align 4, !dbg !157
  %cmp = icmp sle i32 %13, 0, !dbg !157
  br i1 %cmp, label %if.then, label %if.end, !dbg !157

if.then:                                          ; preds = %entry
  %14 = load %struct._IO_FILE.6** @stderr, align 4, !dbg !158
  %15 = load i32* %num_levels, align 4, !dbg !158
  %call9 = call arm_aapcscc  i32 (%struct._IO_FILE.6*, i8*, ...)* @fprintf(%struct._IO_FILE.6* %14, i8* getelementptr inbounds ([93 x i8]* @.str1, i32 0, i32 0), i32 %15), !dbg !158
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !160
  unreachable, !dbg !160

if.end:                                           ; preds = %entry
  %16 = load i32* %num_levels, align 4, !dbg !161
  %call10 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([33 x i8]* @.str2, i32 0, i32 0), i32 %16), !dbg !161
  store i32 0, i32* %i, align 4, !dbg !162
  br label %for.cond, !dbg !162

for.cond:                                         ; preds = %for.inc, %if.end
  %17 = load i32* %i, align 4, !dbg !162
  %18 = load i32* %x_size, align 4, !dbg !162
  %19 = load i32* %y_size, align 4, !dbg !162
  %mul11 = mul nsw i32 %18, %19, !dbg !162
  %cmp12 = icmp slt i32 %17, %mul11, !dbg !162
  br i1 %cmp12, label %for.body, label %for.end, !dbg !162

for.body:                                         ; preds = %for.cond
  %20 = load i16* %scale_factor, align 2, !dbg !164
  %conv = zext i16 %20 to i32, !dbg !164
  %conv13 = sitofp i32 %conv to float, !dbg !164
  %21 = load i32* %i, align 4, !dbg !164
  %22 = load float** %image, align 4, !dbg !164
  %arrayidx = getelementptr inbounds float* %22, i32 %21, !dbg !164
  %23 = load float* %arrayidx, !dbg !164
  %mul14 = fmul float %23, %conv13, !dbg !164
  store float %mul14, float* %arrayidx, !dbg !164
  br label %for.inc, !dbg !164

for.inc:                                          ; preds = %for.body
  %24 = load i32* %i, align 4, !dbg !165
  %inc = add nsw i32 %24, 1, !dbg !165
  store i32 %inc, i32* %i, align 4, !dbg !165
  br label %for.cond, !dbg !165

for.end:                                          ; preds = %for.cond
  %25 = load float** %image, align 4, !dbg !166
  %26 = load i32* %x_size, align 4, !dbg !166
  %27 = load i32* %y_size, align 4, !dbg !166
  %28 = load i32* %num_levels, align 4, !dbg !166
  %call15 = call arm_aapcscc  i32 bitcast (i32 (...)* @build_pyr to i32 (float*, i32, i32, i32, float*, float*, i32)*)(float* %25, i32 %26, i32 %27, i32 %28, float* getelementptr inbounds ([15 x float]* @lo_filter, i32 0, i32 0), float* getelementptr inbounds ([15 x float]* @hi_filter, i32 0, i32 0), i32 15), !dbg !166
  %29 = load double* %compression_factor, align 8, !dbg !167
  %call16 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([32 x i8]* @.str3, i32 0, i32 0), double %29), !dbg !167
  %30 = load i16* %scale_factor, align 2, !dbg !168
  %conv17 = zext i16 %30 to i32, !dbg !168
  %conv18 = sitofp i32 %conv17 to double, !dbg !168
  %31 = load double* %compression_factor, align 8, !dbg !168
  %mul19 = fmul double %31, %conv18, !dbg !168
  store double %mul19, double* %compression_factor, align 8, !dbg !168
  %32 = load float** %image, align 4, !dbg !169
  %33 = load i16** %q_pyr, align 4, !dbg !169
  %34 = load i32* %x_size, align 4, !dbg !169
  %35 = load i32* %y_size, align 4, !dbg !169
  %mul20 = mul nsw i32 %34, %35, !dbg !169
  %36 = load i32* %num_levels, align 4, !dbg !169
  %37 = load double* %compression_factor, align 8, !dbg !169
  %38 = load i16** %bin_size, align 4, !dbg !169
  %call21 = call arm_aapcscc  i32 bitcast (i32 (...)* @quantize_pyr to i32 (float*, i16*, i32, i32, double, i16*)*)(float* %32, i16* %33, i32 %mul20, i32 %36, double %37, i16* %38), !dbg !169
  %39 = load float** %image, align 4, !dbg !170
  %40 = bitcast float* %39 to i8*, !dbg !170
  %call22 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %40), !dbg !170
  %call23 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([23 x i8]* @.str4, i32 0, i32 0)), !dbg !171
  %41 = load i32* %x_size, align 4, !dbg !172
  %42 = load i32* %y_size, align 4, !dbg !172
  %mul24 = mul nsw i32 %41, %42, !dbg !172
  %mul25 = mul i32 %mul24, 2, !dbg !172
  %call26 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul25), !dbg !172
  %43 = bitcast i8* %call26 to i16*, !dbg !172
  store i16* %43, i16** %symbol_stream, align 4, !dbg !172
  %44 = load i16** %q_pyr, align 4, !dbg !173
  %45 = load i32* %lopass_im_size, align 4, !dbg !173
  %add.ptr = getelementptr inbounds i16* %44, i32 %45, !dbg !173
  %46 = load i32* %x_size, align 4, !dbg !173
  %47 = load i32* %y_size, align 4, !dbg !173
  %mul27 = mul nsw i32 %46, %47, !dbg !173
  %48 = load i32* %lopass_im_size, align 4, !dbg !173
  %sub = sub nsw i32 %mul27, %48, !dbg !173
  %49 = load i16** %symbol_stream, align 4, !dbg !173
  %call28 = call arm_aapcscc  i32 bitcast (i32 (...)* @run_length_encode_zeros to i32 (i16*, i32, i16*)*)(i16* %add.ptr, i32 %sub, i16* %49), !dbg !173
  store i32 %call28, i32* %symbol_stream_length, align 4, !dbg !173
  %call29 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([21 x i8]* @.str5, i32 0, i32 0)), !dbg !174
  %50 = load i32* %symbol_stream_length, align 4, !dbg !175
  %mul30 = mul i32 %50, 2, !dbg !175
  %call31 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul30), !dbg !175
  store i8* %call31, i8** %encoded_stream, align 4, !dbg !175
  %51 = load i16** %symbol_stream, align 4, !dbg !176
  %52 = load i32* %symbol_stream_length, align 4, !dbg !176
  %53 = load i8** %encoded_stream, align 4, !dbg !176
  %call32 = call arm_aapcscc  i32 bitcast (i32 (...)* @huffman_encode to i32 (i16*, i32, i8**, i32*, i8*)*)(i16* %51, i32 %52, i8** %huffman_tree, i32* %huffman_tree_size, i8* %53), !dbg !176
  store i32 %call32, i32* %encoded_stream_length, align 4, !dbg !176
  %54 = load i32* %lopass_im_size, align 4, !dbg !177
  %55 = load i32* %symbol_stream_length, align 4, !dbg !177
  %56 = load i32* %encoded_stream_length, align 4, !dbg !177
  %call33 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([74 x i8]* @.str6, i32 0, i32 0), i32 %54, i32 %55, i32 %56), !dbg !177
  %call34 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([23 x i8]* @.str7, i32 0, i32 0)), !dbg !178
  store i8 -1, i8* @temp_byte, align 1, !dbg !179
  %57 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !180
  %call35 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %57), !dbg !180
  %58 = load i32* %num_levels, align 4, !dbg !181
  %conv36 = trunc i32 %58 to i8, !dbg !181
  store i8 %conv36, i8* @temp_byte, align 1, !dbg !181
  %59 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !182
  %call37 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %59), !dbg !182
  %60 = load i32* %x_size, align 4, !dbg !183
  %conv38 = trunc i32 %60 to i16, !dbg !183
  store i16 %conv38, i16* @temp_short, align 2, !dbg !183
  %61 = load i16* @temp_short, align 2, !dbg !184
  %conv39 = sext i16 %61 to i32, !dbg !184
  %and = and i32 %conv39, 65280, !dbg !184
  %shr = ashr i32 %and, 8, !dbg !184
  %conv40 = trunc i32 %shr to i8, !dbg !184
  store i8 %conv40, i8* @temp_byte, align 1, !dbg !184
  %62 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !185
  %call41 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %62), !dbg !185
  %63 = load i16* @temp_short, align 2, !dbg !186
  %conv42 = sext i16 %63 to i32, !dbg !186
  %and43 = and i32 %conv42, 255, !dbg !186
  %conv44 = trunc i32 %and43 to i8, !dbg !186
  store i8 %conv44, i8* @temp_byte, align 1, !dbg !186
  %64 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !187
  %call45 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %64), !dbg !187
  %65 = load i32* %y_size, align 4, !dbg !188
  %conv46 = trunc i32 %65 to i16, !dbg !188
  store i16 %conv46, i16* @temp_short, align 2, !dbg !188
  %66 = load i16* @temp_short, align 2, !dbg !189
  %conv47 = sext i16 %66 to i32, !dbg !189
  %and48 = and i32 %conv47, 65280, !dbg !189
  %shr49 = ashr i32 %and48, 8, !dbg !189
  %conv50 = trunc i32 %shr49 to i8, !dbg !189
  store i8 %conv50, i8* @temp_byte, align 1, !dbg !189
  %67 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !190
  %call51 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %67), !dbg !190
  %68 = load i16* @temp_short, align 2, !dbg !191
  %conv52 = sext i16 %68 to i32, !dbg !191
  %and53 = and i32 %conv52, 255, !dbg !191
  %conv54 = trunc i32 %and53 to i8, !dbg !191
  store i8 %conv54, i8* @temp_byte, align 1, !dbg !191
  %69 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !192
  %call55 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %69), !dbg !192
  %70 = load i16* %scale_factor, align 2, !dbg !193
  store i16 %70, i16* @temp_short, align 2, !dbg !193
  %71 = load i16* @temp_short, align 2, !dbg !194
  %conv56 = sext i16 %71 to i32, !dbg !194
  %and57 = and i32 %conv56, 65280, !dbg !194
  %shr58 = ashr i32 %and57, 8, !dbg !194
  %conv59 = trunc i32 %shr58 to i8, !dbg !194
  store i8 %conv59, i8* @temp_byte, align 1, !dbg !194
  %72 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !195
  %call60 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %72), !dbg !195
  %73 = load i16* @temp_short, align 2, !dbg !196
  %conv61 = sext i16 %73 to i32, !dbg !196
  %and62 = and i32 %conv61, 255, !dbg !196
  %conv63 = trunc i32 %and62 to i8, !dbg !196
  store i8 %conv63, i8* @temp_byte, align 1, !dbg !196
  %74 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !197
  %call64 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %74), !dbg !197
  %75 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !198
  %call65 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %75), !dbg !198
  store i32 %call65, i32* %temp, align 4, !dbg !198
  %call66 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([25 x i8]* @.str8, i32 0, i32 0), i32 %call65), !dbg !198
  store i32 0, i32* %im_count, align 4, !dbg !199
  %76 = load i32* %im_count, align 4, !dbg !200
  %or = or i32 -128, %76, !dbg !200
  %conv67 = trunc i32 %or to i8, !dbg !200
  store i8 %conv67, i8* @temp_byte, align 1, !dbg !200
  %77 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !201
  %call68 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %77), !dbg !201
  %78 = load i32* %im_count, align 4, !dbg !202
  %79 = load i16** %bin_size, align 4, !dbg !202
  %arrayidx69 = getelementptr inbounds i16* %79, i32 %78, !dbg !202
  %80 = load i16* %arrayidx69, !dbg !202
  store i16 %80, i16* @temp_short, align 2, !dbg !202
  %81 = load i16* @temp_short, align 2, !dbg !203
  %conv70 = sext i16 %81 to i32, !dbg !203
  %and71 = and i32 %conv70, 65280, !dbg !203
  %shr72 = ashr i32 %and71, 8, !dbg !203
  %conv73 = trunc i32 %shr72 to i8, !dbg !203
  store i8 %conv73, i8* @temp_byte, align 1, !dbg !203
  %82 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !204
  %call74 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %82), !dbg !204
  %83 = load i16* @temp_short, align 2, !dbg !205
  %conv75 = sext i16 %83 to i32, !dbg !205
  %and76 = and i32 %conv75, 255, !dbg !205
  %conv77 = trunc i32 %and76 to i8, !dbg !205
  store i8 %conv77, i8* @temp_byte, align 1, !dbg !205
  %84 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !206
  %call78 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %84), !dbg !206
  store i8 0, i8* @temp_byte, align 1, !dbg !207
  %85 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !208
  %call79 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %85), !dbg !208
  %86 = load i16** %q_pyr, align 4, !dbg !209
  %87 = bitcast i16* %86 to i8*, !dbg !209
  %88 = load i32* %lopass_im_size, align 4, !dbg !209
  %mul80 = mul i32 2, %88, !dbg !209
  %89 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !209
  %call81 = call arm_aapcscc  i32 @fwrite(i8* %87, i32 1, i32 %mul80, %struct._IO_FILE.6* %89), !dbg !209
  %90 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !210
  %call82 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %90), !dbg !210
  %91 = load i32* %temp, align 4, !dbg !210
  %sub83 = sub nsw i32 %call82, %91, !dbg !210
  %call84 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([41 x i8]* @.str9, i32 0, i32 0), i32 %sub83), !dbg !210
  %92 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !211
  %call85 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %92), !dbg !211
  store i32 %call85, i32* %temp, align 4, !dbg !211
  store i32 1, i32* %im_count, align 4, !dbg !212
  %93 = load i32* %num_levels, align 4, !dbg !212
  store i32 %93, i32* %level, align 4, !dbg !212
  br label %for.cond86, !dbg !212

for.cond86:                                       ; preds = %for.inc111, %for.end
  %94 = load i32* %level, align 4, !dbg !212
  %cmp87 = icmp sgt i32 %94, 0, !dbg !212
  br i1 %cmp87, label %for.body89, label %for.end112, !dbg !212

for.body89:                                       ; preds = %for.cond86
  store i32 1, i32* %i, align 4, !dbg !214
  br label %for.cond90, !dbg !214

for.cond90:                                       ; preds = %for.inc107, %for.body89
  %95 = load i32* %i, align 4, !dbg !214
  %cmp91 = icmp slt i32 %95, 4, !dbg !214
  br i1 %cmp91, label %for.body93, label %for.end110, !dbg !214

for.body93:                                       ; preds = %for.cond90
  %96 = load i32* %im_count, align 4, !dbg !216
  %or94 = or i32 -128, %96, !dbg !216
  %conv95 = trunc i32 %or94 to i8, !dbg !216
  store i8 %conv95, i8* @temp_byte, align 1, !dbg !216
  %97 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !218
  %call96 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %97), !dbg !218
  %98 = load i32* %im_count, align 4, !dbg !219
  %99 = load i16** %bin_size, align 4, !dbg !219
  %arrayidx97 = getelementptr inbounds i16* %99, i32 %98, !dbg !219
  %100 = load i16* %arrayidx97, !dbg !219
  store i16 %100, i16* @temp_short, align 2, !dbg !219
  %101 = load i16* @temp_short, align 2, !dbg !220
  %conv98 = sext i16 %101 to i32, !dbg !220
  %and99 = and i32 %conv98, 65280, !dbg !220
  %shr100 = ashr i32 %and99, 8, !dbg !220
  %conv101 = trunc i32 %shr100 to i8, !dbg !220
  store i8 %conv101, i8* @temp_byte, align 1, !dbg !220
  %102 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !221
  %call102 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %102), !dbg !221
  %103 = load i16* @temp_short, align 2, !dbg !222
  %conv103 = sext i16 %103 to i32, !dbg !222
  %and104 = and i32 %conv103, 255, !dbg !222
  %conv105 = trunc i32 %and104 to i8, !dbg !222
  store i8 %conv105, i8* @temp_byte, align 1, !dbg !222
  %104 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !223
  %call106 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %104), !dbg !223
  br label %for.inc107, !dbg !224

for.inc107:                                       ; preds = %for.body93
  %105 = load i32* %i, align 4, !dbg !225
  %inc108 = add nsw i32 %105, 1, !dbg !225
  store i32 %inc108, i32* %i, align 4, !dbg !225
  %106 = load i32* %im_count, align 4, !dbg !225
  %inc109 = add nsw i32 %106, 1, !dbg !225
  store i32 %inc109, i32* %im_count, align 4, !dbg !225
  br label %for.cond90, !dbg !225

for.end110:                                       ; preds = %for.cond90
  br label %for.inc111, !dbg !226

for.inc111:                                       ; preds = %for.end110
  %107 = load i32* %level, align 4, !dbg !227
  %dec = add nsw i32 %107, -1, !dbg !227
  store i32 %dec, i32* %level, align 4, !dbg !227
  br label %for.cond86, !dbg !227

for.end112:                                       ; preds = %for.cond86
  %108 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !228
  %call113 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %108), !dbg !228
  %109 = load i32* %temp, align 4, !dbg !228
  %sub114 = sub nsw i32 %call113, %109, !dbg !228
  %call115 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([35 x i8]* @.str10, i32 0, i32 0), i32 %sub114), !dbg !228
  %110 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !229
  %call116 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %110), !dbg !229
  store i32 %call116, i32* %temp, align 4, !dbg !229
  store i8 1, i8* @temp_byte, align 1, !dbg !230
  %111 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !231
  %call117 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %111), !dbg !231
  %112 = load i32* %symbol_stream_length, align 4, !dbg !232
  store i32 %112, i32* @temp_int, align 4, !dbg !232
  %113 = load i32* @temp_int, align 4, !dbg !233
  %and118 = and i32 %113, -16777216, !dbg !233
  %shr119 = lshr i32 %and118, 24, !dbg !233
  %conv120 = trunc i32 %shr119 to i8, !dbg !233
  store i8 %conv120, i8* @temp_byte, align 1, !dbg !233
  %114 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !234
  %call121 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %114), !dbg !234
  %115 = load i32* @temp_int, align 4, !dbg !235
  %and122 = and i32 %115, 16711680, !dbg !235
  %shr123 = ashr i32 %and122, 16, !dbg !235
  %conv124 = trunc i32 %shr123 to i8, !dbg !235
  store i8 %conv124, i8* @temp_byte, align 1, !dbg !235
  %116 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !236
  %call125 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %116), !dbg !236
  %117 = load i32* @temp_int, align 4, !dbg !237
  %and126 = and i32 %117, 65280, !dbg !237
  %shr127 = ashr i32 %and126, 8, !dbg !237
  %conv128 = trunc i32 %shr127 to i8, !dbg !237
  store i8 %conv128, i8* @temp_byte, align 1, !dbg !237
  %118 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !238
  %call129 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %118), !dbg !238
  %119 = load i32* @temp_int, align 4, !dbg !239
  %and130 = and i32 %119, 255, !dbg !239
  %conv131 = trunc i32 %and130 to i8, !dbg !239
  store i8 %conv131, i8* @temp_byte, align 1, !dbg !239
  %120 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !239
  %call132 = call arm_aapcscc  i32 @fwrite(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.6* %120), !dbg !239
  %121 = load i8** %huffman_tree, align 4, !dbg !240
  %122 = load i32* %huffman_tree_size, align 4, !dbg !240
  %mul133 = mul i32 1, %122, !dbg !240
  %123 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !240
  %call134 = call arm_aapcscc  i32 @fwrite(i8* %121, i32 1, i32 %mul133, %struct._IO_FILE.6* %123), !dbg !240
  %124 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !241
  %call135 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %124), !dbg !241
  %125 = load i32* %temp, align 4, !dbg !241
  %sub136 = sub nsw i32 %call135, %125, !dbg !241
  %call137 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([32 x i8]* @.str11, i32 0, i32 0), i32 %sub136), !dbg !241
  %126 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !242
  %call138 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %126), !dbg !242
  store i32 %call138, i32* %temp, align 4, !dbg !242
  %127 = load i8** %encoded_stream, align 4, !dbg !243
  %128 = load i32* %encoded_stream_length, align 4, !dbg !243
  %mul139 = mul i32 1, %128, !dbg !243
  %129 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !243
  %call140 = call arm_aapcscc  i32 @fwrite(i8* %127, i32 1, i32 %mul139, %struct._IO_FILE.6* %129), !dbg !243
  %130 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !244
  %call141 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %130), !dbg !244
  %131 = load i32* %temp, align 4, !dbg !244
  %sub142 = sub nsw i32 %call141, %131, !dbg !244
  %call143 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([35 x i8]* @.str12, i32 0, i32 0), i32 %sub142), !dbg !244
  %132 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !245
  %call144 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %132), !dbg !245
  %133 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !246
  %call145 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %133), !dbg !246
  %conv146 = sitofp i32 %call145 to float, !dbg !246
  %mul147 = fmul float 8.000000e+00, %conv146, !dbg !246
  %134 = load i32* %x_size, align 4, !dbg !246
  %135 = load i32* %y_size, align 4, !dbg !246
  %mul148 = mul nsw i32 %134, %135, !dbg !246
  %conv149 = sitofp i32 %mul148 to float, !dbg !246
  %div150 = fdiv float %mul147, %conv149, !dbg !246
  %conv151 = fpext float %div150 to double, !dbg !246
  %136 = load i32* %x_size, align 4, !dbg !246
  %137 = load i32* %y_size, align 4, !dbg !246
  %mul152 = mul nsw i32 %136, %137, !dbg !246
  %conv153 = sitofp i32 %mul152 to float, !dbg !246
  %138 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !247
  %call154 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.6* %138), !dbg !247
  %conv155 = sitofp i32 %call154 to float, !dbg !247
  %div156 = fdiv float %conv153, %conv155, !dbg !247
  %conv157 = fpext float %div156 to double, !dbg !247
  %call158 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([70 x i8]* @.str13, i32 0, i32 0), i32 %call144, double %conv151, double %conv157), !dbg !247
  %139 = load %struct._IO_FILE.6** %outfile, align 4, !dbg !248
  %call159 = call arm_aapcscc  i32 @fclose(%struct._IO_FILE.6* %139), !dbg !248
  %140 = load i16** %q_pyr, align 4, !dbg !249
  %141 = bitcast i16* %140 to i8*, !dbg !249
  %call160 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %141), !dbg !249
  %142 = load i16** %bin_size, align 4, !dbg !250
  %143 = bitcast i16* %142 to i8*, !dbg !250
  %call161 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %143), !dbg !250
  %144 = load i16** %symbol_stream, align 4, !dbg !251
  %145 = bitcast i16* %144 to i8*, !dbg !251
  %call162 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %145), !dbg !251
  %146 = load i8** %encoded_stream, align 4, !dbg !252
  %call163 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %146), !dbg !252
  %147 = load i32* %retval, !dbg !253
  ret i32 %147, !dbg !253
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc i32 @parse_epic_args(...)

declare arm_aapcscc i8* @check_malloc(...)

declare arm_aapcscc i32 @fprintf(%struct._IO_FILE.6*, i8*, ...)

declare arm_aapcscc void @exit(i32) noreturn

declare arm_aapcscc i32 @printf(i8*, ...)

declare arm_aapcscc i32 @build_pyr(...)

declare arm_aapcscc i32 @quantize_pyr(...)

declare arm_aapcscc i32 @check_free(...)

declare arm_aapcscc i32 @run_length_encode_zeros(...)

declare arm_aapcscc i32 @huffman_encode(...)

declare arm_aapcscc i32 @fwrite(i8*, i32, i32, %struct._IO_FILE.6*)

declare arm_aapcscc i32 @ftell(%struct._IO_FILE.6*)

declare arm_aapcscc i32 @fclose(%struct._IO_FILE.6*)

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !12} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"main", metadata !"main", metadata !"", metadata !6, i32 80, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i32, i8**)* @main, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"epic.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{metadata !13}
!13 = metadata !{metadata !14, metadata !24, metadata !25, metadata !30}
!14 = metadata !{i32 720948, i32 0, null, metadata !"ss_time_start", metadata !"ss_time_start", metadata !"", metadata !15, i32 6, metadata !16, i32 0, i32 1, %struct.timeval* @ss_time_start} ; [ DW_TAG_variable ]
!15 = metadata !{i32 720937, metadata !"stats_time.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!16 = metadata !{i32 720915, null, metadata !"timeval", metadata !17, i32 69, i64 64, i64 32, i32 0, i32 0, null, metadata !18, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!17 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!18 = metadata !{metadata !19, metadata !22}
!19 = metadata !{i32 720909, metadata !16, metadata !"tv_sec", metadata !17, i32 71, i64 32, i64 32, i64 0, i32 0, metadata !20} ; [ DW_TAG_member ]
!20 = metadata !{i32 720918, null, metadata !"__time_t", metadata !17, i32 149, i64 0, i64 0, i64 0, i32 0, metadata !21} ; [ DW_TAG_typedef ]
!21 = metadata !{i32 720932, null, metadata !"long int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!22 = metadata !{i32 720909, metadata !16, metadata !"tv_usec", metadata !17, i32 72, i64 32, i64 32, i64 32, i32 0, metadata !23} ; [ DW_TAG_member ]
!23 = metadata !{i32 720918, null, metadata !"__suseconds_t", metadata !17, i32 151, i64 0, i64 0, i64 0, i32 0, metadata !21} ; [ DW_TAG_typedef ]
!24 = metadata !{i32 720948, i32 0, null, metadata !"ss_time_end", metadata !"ss_time_end", metadata !"", metadata !15, i32 7, metadata !16, i32 0, i32 1, %struct.timeval* @ss_time_end} ; [ DW_TAG_variable ]
!25 = metadata !{i32 720948, i32 0, null, metadata !"hi_filter", metadata !"hi_filter", metadata !"", metadata !6, i32 40, metadata !26, i32 1, i32 1, [15 x float]* @hi_filter} ; [ DW_TAG_variable ]
!26 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 480, i64 32, i32 0, i32 0, metadata !27, metadata !28, i32 0, i32 0} ; [ DW_TAG_array_type ]
!27 = metadata !{i32 720932, null, metadata !"float", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!28 = metadata !{metadata !29}
!29 = metadata !{i32 720929, i64 0, i64 14}       ; [ DW_TAG_subrange_type ]
!30 = metadata !{i32 720948, i32 0, null, metadata !"lo_filter", metadata !"lo_filter", metadata !"", metadata !6, i32 33, metadata !26, i32 1, i32 1, [15 x float]* @lo_filter} ; [ DW_TAG_variable ]
!31 = metadata !{i32 721153, metadata !5, metadata !"argc", metadata !6, i32 16777294, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!32 = metadata !{i32 78, i32 7, metadata !5, null}
!33 = metadata !{i32 721153, metadata !5, metadata !"argv", metadata !6, i32 33554511, metadata !34, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!34 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !35} ; [ DW_TAG_pointer_type ]
!35 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !36} ; [ DW_TAG_pointer_type ]
!36 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!37 = metadata !{i32 79, i32 9, metadata !5, null}
!38 = metadata !{i32 721152, metadata !39, metadata !"outfile", metadata !6, i32 82, metadata !40, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!39 = metadata !{i32 720907, metadata !5, i32 80, i32 3, metadata !6, i32 120} ; [ DW_TAG_lexical_block ]
!40 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !41} ; [ DW_TAG_pointer_type ]
!41 = metadata !{i32 720918, null, metadata !"FILE", metadata !6, i32 49, i64 0, i64 0, i64 0, i32 0, metadata !42} ; [ DW_TAG_typedef ]
!42 = metadata !{i32 720915, null, metadata !"_IO_FILE", metadata !43, i32 271, i64 1216, i64 64, i32 0, i32 0, null, metadata !44, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!43 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!44 = metadata !{metadata !45, metadata !46, metadata !47, metadata !48, metadata !49, metadata !50, metadata !51, metadata !52, metadata !53, metadata !54, metadata !55, metadata !56, metadata !57, metadata !65, metadata !66, metadata !67, metadata !68, metadata !70, metadata !72, metadata !74, metadata !78, metadata !80, metadata !84, metadata !85, metadata !86, metadata !87, metadata !88, metadata !91, metadata !92}
!45 = metadata !{i32 720909, metadata !42, metadata !"_flags", metadata !43, i32 272, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_member ]
!46 = metadata !{i32 720909, metadata !42, metadata !"_IO_read_ptr", metadata !43, i32 277, i64 32, i64 32, i64 32, i32 0, metadata !35} ; [ DW_TAG_member ]
!47 = metadata !{i32 720909, metadata !42, metadata !"_IO_read_end", metadata !43, i32 278, i64 32, i64 32, i64 64, i32 0, metadata !35} ; [ DW_TAG_member ]
!48 = metadata !{i32 720909, metadata !42, metadata !"_IO_read_base", metadata !43, i32 279, i64 32, i64 32, i64 96, i32 0, metadata !35} ; [ DW_TAG_member ]
!49 = metadata !{i32 720909, metadata !42, metadata !"_IO_write_base", metadata !43, i32 280, i64 32, i64 32, i64 128, i32 0, metadata !35} ; [ DW_TAG_member ]
!50 = metadata !{i32 720909, metadata !42, metadata !"_IO_write_ptr", metadata !43, i32 281, i64 32, i64 32, i64 160, i32 0, metadata !35} ; [ DW_TAG_member ]
!51 = metadata !{i32 720909, metadata !42, metadata !"_IO_write_end", metadata !43, i32 282, i64 32, i64 32, i64 192, i32 0, metadata !35} ; [ DW_TAG_member ]
!52 = metadata !{i32 720909, metadata !42, metadata !"_IO_buf_base", metadata !43, i32 283, i64 32, i64 32, i64 224, i32 0, metadata !35} ; [ DW_TAG_member ]
!53 = metadata !{i32 720909, metadata !42, metadata !"_IO_buf_end", metadata !43, i32 284, i64 32, i64 32, i64 256, i32 0, metadata !35} ; [ DW_TAG_member ]
!54 = metadata !{i32 720909, metadata !42, metadata !"_IO_save_base", metadata !43, i32 286, i64 32, i64 32, i64 288, i32 0, metadata !35} ; [ DW_TAG_member ]
!55 = metadata !{i32 720909, metadata !42, metadata !"_IO_backup_base", metadata !43, i32 287, i64 32, i64 32, i64 320, i32 0, metadata !35} ; [ DW_TAG_member ]
!56 = metadata !{i32 720909, metadata !42, metadata !"_IO_save_end", metadata !43, i32 288, i64 32, i64 32, i64 352, i32 0, metadata !35} ; [ DW_TAG_member ]
!57 = metadata !{i32 720909, metadata !42, metadata !"_markers", metadata !43, i32 290, i64 32, i64 32, i64 384, i32 0, metadata !58} ; [ DW_TAG_member ]
!58 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !59} ; [ DW_TAG_pointer_type ]
!59 = metadata !{i32 720915, null, metadata !"_IO_marker", metadata !43, i32 186, i64 96, i64 32, i32 0, i32 0, null, metadata !60, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!60 = metadata !{metadata !61, metadata !62, metadata !64}
!61 = metadata !{i32 720909, metadata !59, metadata !"_next", metadata !43, i32 187, i64 32, i64 32, i64 0, i32 0, metadata !58} ; [ DW_TAG_member ]
!62 = metadata !{i32 720909, metadata !59, metadata !"_sbuf", metadata !43, i32 188, i64 32, i64 32, i64 32, i32 0, metadata !63} ; [ DW_TAG_member ]
!63 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !42} ; [ DW_TAG_pointer_type ]
!64 = metadata !{i32 720909, metadata !59, metadata !"_pos", metadata !43, i32 192, i64 32, i64 32, i64 64, i32 0, metadata !9} ; [ DW_TAG_member ]
!65 = metadata !{i32 720909, metadata !42, metadata !"_chain", metadata !43, i32 292, i64 32, i64 32, i64 416, i32 0, metadata !63} ; [ DW_TAG_member ]
!66 = metadata !{i32 720909, metadata !42, metadata !"_fileno", metadata !43, i32 294, i64 32, i64 32, i64 448, i32 0, metadata !9} ; [ DW_TAG_member ]
!67 = metadata !{i32 720909, metadata !42, metadata !"_flags2", metadata !43, i32 298, i64 32, i64 32, i64 480, i32 0, metadata !9} ; [ DW_TAG_member ]
!68 = metadata !{i32 720909, metadata !42, metadata !"_old_offset", metadata !43, i32 300, i64 32, i64 32, i64 512, i32 0, metadata !69} ; [ DW_TAG_member ]
!69 = metadata !{i32 720918, null, metadata !"__off_t", metadata !43, i32 141, i64 0, i64 0, i64 0, i32 0, metadata !21} ; [ DW_TAG_typedef ]
!70 = metadata !{i32 720909, metadata !42, metadata !"_cur_column", metadata !43, i32 304, i64 16, i64 16, i64 544, i32 0, metadata !71} ; [ DW_TAG_member ]
!71 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!72 = metadata !{i32 720909, metadata !42, metadata !"_vtable_offset", metadata !43, i32 305, i64 8, i64 8, i64 560, i32 0, metadata !73} ; [ DW_TAG_member ]
!73 = metadata !{i32 720932, null, metadata !"signed char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!74 = metadata !{i32 720909, metadata !42, metadata !"_shortbuf", metadata !43, i32 306, i64 8, i64 8, i64 568, i32 0, metadata !75} ; [ DW_TAG_member ]
!75 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8, i64 8, i32 0, i32 0, metadata !36, metadata !76, i32 0, i32 0} ; [ DW_TAG_array_type ]
!76 = metadata !{metadata !77}
!77 = metadata !{i32 720929, i64 0, i64 0}        ; [ DW_TAG_subrange_type ]
!78 = metadata !{i32 720909, metadata !42, metadata !"_lock", metadata !43, i32 310, i64 32, i64 32, i64 576, i32 0, metadata !79} ; [ DW_TAG_member ]
!79 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, null} ; [ DW_TAG_pointer_type ]
!80 = metadata !{i32 720909, metadata !42, metadata !"_offset", metadata !43, i32 319, i64 64, i64 64, i64 640, i32 0, metadata !81} ; [ DW_TAG_member ]
!81 = metadata !{i32 720918, null, metadata !"__off64_t", metadata !43, i32 142, i64 0, i64 0, i64 0, i32 0, metadata !82} ; [ DW_TAG_typedef ]
!82 = metadata !{i32 720918, null, metadata !"__quad_t", metadata !43, i32 56, i64 0, i64 0, i64 0, i32 0, metadata !83} ; [ DW_TAG_typedef ]
!83 = metadata !{i32 720932, null, metadata !"long long int", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!84 = metadata !{i32 720909, metadata !42, metadata !"__pad1", metadata !43, i32 328, i64 32, i64 32, i64 704, i32 0, metadata !79} ; [ DW_TAG_member ]
!85 = metadata !{i32 720909, metadata !42, metadata !"__pad2", metadata !43, i32 329, i64 32, i64 32, i64 736, i32 0, metadata !79} ; [ DW_TAG_member ]
!86 = metadata !{i32 720909, metadata !42, metadata !"__pad3", metadata !43, i32 330, i64 32, i64 32, i64 768, i32 0, metadata !79} ; [ DW_TAG_member ]
!87 = metadata !{i32 720909, metadata !42, metadata !"__pad4", metadata !43, i32 331, i64 32, i64 32, i64 800, i32 0, metadata !79} ; [ DW_TAG_member ]
!88 = metadata !{i32 720909, metadata !42, metadata !"__pad5", metadata !43, i32 332, i64 32, i64 32, i64 832, i32 0, metadata !89} ; [ DW_TAG_member ]
!89 = metadata !{i32 720918, null, metadata !"size_t", metadata !43, i32 211, i64 0, i64 0, i64 0, i32 0, metadata !90} ; [ DW_TAG_typedef ]
!90 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!91 = metadata !{i32 720909, metadata !42, metadata !"_mode", metadata !43, i32 334, i64 32, i64 32, i64 864, i32 0, metadata !9} ; [ DW_TAG_member ]
!92 = metadata !{i32 720909, metadata !42, metadata !"_unused2", metadata !43, i32 336, i64 320, i64 8, i64 896, i32 0, metadata !93} ; [ DW_TAG_member ]
!93 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !36, metadata !94, i32 0, i32 0} ; [ DW_TAG_array_type ]
!94 = metadata !{metadata !95}
!95 = metadata !{i32 720929, i64 0, i64 39}       ; [ DW_TAG_subrange_type ]
!96 = metadata !{i32 82, i32 9, metadata !39, null}
!97 = metadata !{i32 721152, metadata !39, metadata !"image", metadata !6, i32 83, metadata !98, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!98 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !27} ; [ DW_TAG_pointer_type ]
!99 = metadata !{i32 83, i32 10, metadata !39, null}
!100 = metadata !{i32 721152, metadata !39, metadata !"scale_factor", metadata !6, i32 84, metadata !71, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!101 = metadata !{i32 84, i32 18, metadata !39, null}
!102 = metadata !{i32 84, i32 36, metadata !39, null}
!103 = metadata !{i32 721152, metadata !39, metadata !"i", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!104 = metadata !{i32 85, i32 7, metadata !39, null}
!105 = metadata !{i32 721152, metadata !39, metadata !"j", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!106 = metadata !{i32 85, i32 9, metadata !39, null}
!107 = metadata !{i32 721152, metadata !39, metadata !"level", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!108 = metadata !{i32 85, i32 12, metadata !39, null}
!109 = metadata !{i32 721152, metadata !39, metadata !"im_count", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!110 = metadata !{i32 85, i32 19, metadata !39, null}
!111 = metadata !{i32 721152, metadata !39, metadata !"x_size", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!112 = metadata !{i32 85, i32 29, metadata !39, null}
!113 = metadata !{i32 721152, metadata !39, metadata !"y_size", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!114 = metadata !{i32 85, i32 37, metadata !39, null}
!115 = metadata !{i32 721152, metadata !39, metadata !"num_levels", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!116 = metadata !{i32 85, i32 45, metadata !39, null}
!117 = metadata !{i32 721152, metadata !39, metadata !"lopass_im_size", metadata !6, i32 85, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!118 = metadata !{i32 85, i32 57, metadata !39, null}
!119 = metadata !{i32 721152, metadata !39, metadata !"temp", metadata !6, i32 86, metadata !21, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!120 = metadata !{i32 86, i32 8, metadata !39, null}
!121 = metadata !{i32 721152, metadata !39, metadata !"compression_factor", metadata !6, i32 87, metadata !122, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!122 = metadata !{i32 720932, null, metadata !"double", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!123 = metadata !{i32 87, i32 10, metadata !39, null}
!124 = metadata !{i32 721152, metadata !39, metadata !"huffman_tree_size", metadata !6, i32 88, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!125 = metadata !{i32 88, i32 7, metadata !39, null}
!126 = metadata !{i32 721152, metadata !39, metadata !"bin_size", metadata !6, i32 89, metadata !127, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!127 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !128} ; [ DW_TAG_pointer_type ]
!128 = metadata !{i32 720918, null, metadata !"BinValueType", metadata !6, i32 64, i64 0, i64 0, i64 0, i32 0, metadata !71} ; [ DW_TAG_typedef ]
!129 = metadata !{i32 89, i32 17, metadata !39, null}
!130 = metadata !{i32 721152, metadata !39, metadata !"q_pyr", metadata !6, i32 90, metadata !131, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!131 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !132} ; [ DW_TAG_pointer_type ]
!132 = metadata !{i32 720918, null, metadata !"BinIndexType", metadata !6, i32 59, i64 0, i64 0, i64 0, i32 0, metadata !133} ; [ DW_TAG_typedef ]
!133 = metadata !{i32 720932, null, metadata !"short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!134 = metadata !{i32 90, i32 17, metadata !39, null}
!135 = metadata !{i32 721152, metadata !39, metadata !"symbol_stream_length", metadata !6, i32 91, metadata !90, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!136 = metadata !{i32 91, i32 16, metadata !39, null}
!137 = metadata !{i32 721152, metadata !39, metadata !"encoded_stream_length", metadata !6, i32 91, metadata !90, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!138 = metadata !{i32 91, i32 38, metadata !39, null}
!139 = metadata !{i32 721152, metadata !39, metadata !"symbol_stream", metadata !6, i32 92, metadata !140, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!140 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !141} ; [ DW_TAG_pointer_type ]
!141 = metadata !{i32 720918, null, metadata !"SymbolType", metadata !6, i32 74, i64 0, i64 0, i64 0, i32 0, metadata !71} ; [ DW_TAG_typedef ]
!142 = metadata !{i32 92, i32 15, metadata !39, null}
!143 = metadata !{i32 721152, metadata !39, metadata !"huffman_tree", metadata !6, i32 93, metadata !144, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!144 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !145} ; [ DW_TAG_pointer_type ]
!145 = metadata !{i32 720918, null, metadata !"Byte", metadata !6, i32 76, i64 0, i64 0, i64 0, i32 0, metadata !146} ; [ DW_TAG_typedef ]
!146 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!147 = metadata !{i32 93, i32 9, metadata !39, null}
!148 = metadata !{i32 721152, metadata !39, metadata !"encoded_stream", metadata !6, i32 94, metadata !149, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!149 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !150} ; [ DW_TAG_pointer_type ]
!150 = metadata !{i32 720918, null, metadata !"CodeType", metadata !6, i32 79, i64 0, i64 0, i64 0, i32 0, metadata !145} ; [ DW_TAG_typedef ]
!151 = metadata !{i32 94, i32 13, metadata !39, null}
!152 = metadata !{i32 98, i32 3, metadata !39, null}
!153 = metadata !{i32 101, i32 3, metadata !39, null}
!154 = metadata !{i32 102, i32 28, metadata !39, null}
!155 = metadata !{i32 103, i32 31, metadata !39, null}
!156 = metadata !{i32 105, i32 3, metadata !39, null}
!157 = metadata !{i32 107, i32 3, metadata !39, null}
!158 = metadata !{i32 109, i32 7, metadata !159, null}
!159 = metadata !{i32 720907, metadata !39, i32 108, i32 7, metadata !6, i32 121} ; [ DW_TAG_lexical_block ]
!160 = metadata !{i32 111, i32 7, metadata !159, null}
!161 = metadata !{i32 116, i32 3, metadata !39, null}
!162 = metadata !{i32 117, i32 8, metadata !163, null}
!163 = metadata !{i32 720907, metadata !39, i32 117, i32 3, metadata !6, i32 122} ; [ DW_TAG_lexical_block ]
!164 = metadata !{i32 117, i32 33, metadata !163, null}
!165 = metadata !{i32 117, i32 28, metadata !163, null}
!166 = metadata !{i32 118, i32 3, metadata !39, null}
!167 = metadata !{i32 122, i32 3, metadata !39, null}
!168 = metadata !{i32 123, i32 3, metadata !39, null}
!169 = metadata !{i32 124, i32 3, metadata !39, null}
!170 = metadata !{i32 125, i32 3, metadata !39, null}
!171 = metadata !{i32 129, i32 3, metadata !39, null}
!172 = metadata !{i32 130, i32 34, metadata !39, null}
!173 = metadata !{i32 131, i32 26, metadata !39, null}
!174 = metadata !{i32 135, i32 3, metadata !39, null}
!175 = metadata !{i32 136, i32 33, metadata !39, null}
!176 = metadata !{i32 137, i32 27, metadata !39, null}
!177 = metadata !{i32 140, i32 3, metadata !39, null}
!178 = metadata !{i32 143, i32 3, metadata !39, null}
!179 = metadata !{i32 144, i32 3, metadata !39, null}
!180 = metadata !{i32 144, i32 30, metadata !39, null}
!181 = metadata !{i32 145, i32 3, metadata !39, null}
!182 = metadata !{i32 145, i32 34, metadata !39, null}
!183 = metadata !{i32 146, i32 3, metadata !39, null}
!184 = metadata !{i32 146, i32 41, metadata !39, null}
!185 = metadata !{i32 146, i32 79, metadata !39, null}
!186 = metadata !{i32 146, i32 111, metadata !39, null}
!187 = metadata !{i32 146, i32 146, metadata !39, null}
!188 = metadata !{i32 147, i32 3, metadata !39, null}
!189 = metadata !{i32 147, i32 41, metadata !39, null}
!190 = metadata !{i32 147, i32 79, metadata !39, null}
!191 = metadata !{i32 147, i32 111, metadata !39, null}
!192 = metadata !{i32 147, i32 146, metadata !39, null}
!193 = metadata !{i32 148, i32 3, metadata !39, null}
!194 = metadata !{i32 148, i32 47, metadata !39, null}
!195 = metadata !{i32 148, i32 85, metadata !39, null}
!196 = metadata !{i32 148, i32 117, metadata !39, null}
!197 = metadata !{i32 148, i32 152, metadata !39, null}
!198 = metadata !{i32 149, i32 47, metadata !39, null}
!199 = metadata !{i32 151, i32 3, metadata !39, null}
!200 = metadata !{i32 152, i32 3, metadata !39, null}
!201 = metadata !{i32 152, i32 43, metadata !39, null}
!202 = metadata !{i32 153, i32 3, metadata !39, null}
!203 = metadata !{i32 153, i32 53, metadata !39, null}
!204 = metadata !{i32 153, i32 91, metadata !39, null}
!205 = metadata !{i32 153, i32 123, metadata !39, null}
!206 = metadata !{i32 153, i32 158, metadata !39, null}
!207 = metadata !{i32 154, i32 3, metadata !39, null}
!208 = metadata !{i32 154, i32 30, metadata !39, null}
!209 = metadata !{i32 155, i32 3, metadata !39, null}
!210 = metadata !{i32 156, i32 56, metadata !39, null}
!211 = metadata !{i32 157, i32 10, metadata !39, null}
!212 = metadata !{i32 159, i32 8, metadata !213, null}
!213 = metadata !{i32 720907, metadata !39, i32 159, i32 3, metadata !6, i32 123} ; [ DW_TAG_lexical_block ]
!214 = metadata !{i32 160, i32 10, metadata !215, null}
!215 = metadata !{i32 720907, metadata !213, i32 160, i32 5, metadata !6, i32 124} ; [ DW_TAG_lexical_block ]
!216 = metadata !{i32 162, i32 2, metadata !217, null}
!217 = metadata !{i32 720907, metadata !215, i32 161, i32 2, metadata !6, i32 125} ; [ DW_TAG_lexical_block ]
!218 = metadata !{i32 162, i32 42, metadata !217, null}
!219 = metadata !{i32 163, i32 2, metadata !217, null}
!220 = metadata !{i32 163, i32 52, metadata !217, null}
!221 = metadata !{i32 163, i32 90, metadata !217, null}
!222 = metadata !{i32 163, i32 122, metadata !217, null}
!223 = metadata !{i32 163, i32 157, metadata !217, null}
!224 = metadata !{i32 164, i32 2, metadata !217, null}
!225 = metadata !{i32 160, i32 20, metadata !215, null}
!226 = metadata !{i32 164, i32 2, metadata !215, null}
!227 = metadata !{i32 159, i32 47, metadata !213, null}
!228 = metadata !{i32 165, i32 50, metadata !39, null}
!229 = metadata !{i32 166, i32 10, metadata !39, null}
!230 = metadata !{i32 168, i32 3, metadata !39, null}
!231 = metadata !{i32 168, i32 30, metadata !39, null}
!232 = metadata !{i32 169, i32 3, metadata !39, null}
!233 = metadata !{i32 169, i32 51, metadata !39, null}
!234 = metadata !{i32 169, i32 92, metadata !39, null}
!235 = metadata !{i32 169, i32 124, metadata !39, null}
!236 = metadata !{i32 169, i32 165, metadata !39, null}
!237 = metadata !{i32 169, i32 197, metadata !39, null}
!238 = metadata !{i32 169, i32 237, metadata !39, null}
!239 = metadata !{i32 169, i32 0, metadata !39, null}
!240 = metadata !{i32 170, i32 3, metadata !39, null}
!241 = metadata !{i32 171, i32 47, metadata !39, null}
!242 = metadata !{i32 172, i32 10, metadata !39, null}
!243 = metadata !{i32 173, i32 3, metadata !39, null}
!244 = metadata !{i32 174, i32 50, metadata !39, null}
!245 = metadata !{i32 177, i32 4, metadata !39, null}
!246 = metadata !{i32 178, i32 16, metadata !39, null}
!247 = metadata !{i32 179, i32 29, metadata !39, null}
!248 = metadata !{i32 180, i32 3, metadata !39, null}
!249 = metadata !{i32 182, i32 3, metadata !39, null}
!250 = metadata !{i32 183, i32 3, metadata !39, null}
!251 = metadata !{i32 184, i32 3, metadata !39, null}
!252 = metadata !{i32 185, i32 3, metadata !39, null}
!253 = metadata !{i32 189, i32 3, metadata !39, null}
