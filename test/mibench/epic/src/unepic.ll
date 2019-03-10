; ModuleID = 'unepic.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct.timeval.11 = type { i32, i32 }
%struct._IO_FILE.8 = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker.9*, %struct._IO_FILE.8*, i32, i32, i32, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i32, i32, [40 x i8] }
%struct._IO_marker.9 = type { %struct._IO_marker.9*, %struct._IO_FILE.8*, i32 }
%struct.code_node.10 = type { %struct.code_node.10*, %struct.code_node.10*, i16 }

@.str = private unnamed_addr constant [23 x i8] c"Read %ld byte header.\0A\00", align 1
@.str1 = private unnamed_addr constant [53 x i8] c"num_levels: %d, x_size: %d, y_size: %d, scale: %8lf\0A\00", align 1
@temp_byte = external global i8
@temp_short = external global i16
@.str2 = private unnamed_addr constant [33 x i8] c"Read %ld bytes of Binsize Info.\0A\00", align 1
@.str3 = private unnamed_addr constant [29 x i8] c"Read %ld bytes of Raw data.\0A\00", align 1
@temp_int = external global i32
@.str4 = private unnamed_addr constant [29 x i8] c"Read %ld byte huffman tree.\0A\00", align 1
@.str5 = private unnamed_addr constant [33 x i8] c"Read %ld bytes of encoded data.\0A\00", align 1
@.str6 = private unnamed_addr constant [35 x i8] c"ERROR: Bad data tag in file: %02x\0A\00", align 1
@.str7 = private unnamed_addr constant [28 x i8] c"Read a total of %ld bytes.\0A\00", align 1
@.str8 = private unnamed_addr constant [26 x i8] c"Unquantizing pyramid ...\0A\00", align 1
@.str9 = private unnamed_addr constant [25 x i8] c"Collapsing pyramid ... \0A\00", align 1
@.str10 = private unnamed_addr constant [32 x i8] c"UNEPIC time = %ld milliseconds\0A\00", align 1
@.str11 = private unnamed_addr constant [26 x i8] c"Writing output file ... \0A\00", align 1
@ss_time_start = common global %struct.timeval.11 zeroinitializer, align 4
@ss_time_end = common global %struct.timeval.11 zeroinitializer, align 4

define arm_aapcscc i32 @main(i32 %argc, i8** %argv) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %argc.addr = alloca i32, align 4
  %argv.addr = alloca i8**, align 4
  %infile = alloca %struct._IO_FILE.8*, align 4
  %outfile = alloca %struct._IO_FILE.8*, align 4
  %result = alloca i32*, align 4
  %pyr = alloca i32*, align 4
  %q_pyr = alloca i16*, align 4
  %x_size = alloca i32, align 4
  %y_size = alloca i32, align 4
  %num_levels = alloca i32, align 4
  %scale_factor = alloca double, align 8
  %dtemp = alloca double, align 8
  %bin_size = alloca i16*, align 4
  %the_tag = alloca i8, align 1
  %temp = alloca i32, align 4
  %i = alloca i32, align 4
  %index = alloca i32, align 4
  %im_count = alloca i32, align 4
  %block_size = alloca i32, align 4
  %block_pos = alloca i32, align 4
  %first_image_flag = alloca i32, align 4
  %symbol_stream_length = alloca i32, align 4
  %symbol_stream = alloca i16*, align 4
  %huffman_tree = alloca %struct.code_node.10*, align 4
  store i32 0, i32* %retval
  store i32 %argc, i32* %argc.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %argc.addr}, metadata !25), !dbg !26
  store i8** %argv, i8*** %argv.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8*** %argv.addr}, metadata !27), !dbg !31
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.8** %infile}, metadata !32), !dbg !90
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.8** %outfile}, metadata !91), !dbg !92
  call void @llvm.dbg.declare(metadata !{i32** %result}, metadata !93), !dbg !95
  call void @llvm.dbg.declare(metadata !{i32** %pyr}, metadata !96), !dbg !97
  call void @llvm.dbg.declare(metadata !{i16** %q_pyr}, metadata !98), !dbg !102
  call void @llvm.dbg.declare(metadata !{i32* %x_size}, metadata !103), !dbg !104
  call void @llvm.dbg.declare(metadata !{i32* %y_size}, metadata !105), !dbg !106
  call void @llvm.dbg.declare(metadata !{i32* %num_levels}, metadata !107), !dbg !108
  call void @llvm.dbg.declare(metadata !{double* %scale_factor}, metadata !109), !dbg !111
  call void @llvm.dbg.declare(metadata !{double* %dtemp}, metadata !112), !dbg !113
  call void @llvm.dbg.declare(metadata !{i16** %bin_size}, metadata !114), !dbg !117
  call void @llvm.dbg.declare(metadata !{i8* %the_tag}, metadata !118), !dbg !121
  call void @llvm.dbg.declare(metadata !{i32* %temp}, metadata !122), !dbg !123
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !124), !dbg !125
  call void @llvm.dbg.declare(metadata !{i32* %index}, metadata !126), !dbg !127
  call void @llvm.dbg.declare(metadata !{i32* %im_count}, metadata !128), !dbg !129
  store i32 0, i32* %im_count, align 4, !dbg !130
  call void @llvm.dbg.declare(metadata !{i32* %block_size}, metadata !131), !dbg !132
  call void @llvm.dbg.declare(metadata !{i32* %block_pos}, metadata !133), !dbg !134
  call void @llvm.dbg.declare(metadata !{i32* %first_image_flag}, metadata !135), !dbg !136
  call void @llvm.dbg.declare(metadata !{i32* %symbol_stream_length}, metadata !137), !dbg !138
  call void @llvm.dbg.declare(metadata !{i16** %symbol_stream}, metadata !139), !dbg !142
  call void @llvm.dbg.declare(metadata !{%struct.code_node.10** %huffman_tree}, metadata !143), !dbg !151
  %0 = load i32* %argc.addr, align 4, !dbg !152
  %1 = load i8*** %argv.addr, align 4, !dbg !152
  %call = call arm_aapcscc  i32 bitcast (i32 (...)* @parse_unepic_args to i32 (i32, i8**, %struct._IO_FILE.8**, i32*, i32*, i32*, double*, %struct._IO_FILE.8**)*)(i32 %0, i8** %1, %struct._IO_FILE.8** %infile, i32* %num_levels, i32* %x_size, i32* %y_size, double* %scale_factor, %struct._IO_FILE.8** %outfile), !dbg !152
  %2 = load %struct._IO_FILE.8** %infile, align 4, !dbg !153
  %call1 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %2), !dbg !153
  store i32 %call1, i32* %temp, align 4, !dbg !153
  %call2 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([23 x i8]* @.str, i32 0, i32 0), i32 %call1), !dbg !153
  %3 = load i32* %num_levels, align 4, !dbg !154
  %4 = load i32* %x_size, align 4, !dbg !154
  %5 = load i32* %y_size, align 4, !dbg !154
  %6 = load double* %scale_factor, align 8, !dbg !154
  %call3 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([53 x i8]* @.str1, i32 0, i32 0), i32 %3, i32 %4, i32 %5, double %6), !dbg !154
  %7 = load i32* %x_size, align 4, !dbg !155
  %8 = load i32* %y_size, align 4, !dbg !155
  %mul = mul nsw i32 %7, %8, !dbg !155
  %mul4 = mul i32 %mul, 2, !dbg !155
  %call5 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul4), !dbg !155
  %9 = bitcast i8* %call5 to i16*, !dbg !155
  store i16* %9, i16** %q_pyr, align 4, !dbg !155
  %10 = load i32* %num_levels, align 4, !dbg !156
  %mul6 = mul nsw i32 3, %10, !dbg !156
  %add = add nsw i32 %mul6, 1, !dbg !156
  %mul7 = mul i32 %add, 2, !dbg !156
  %call8 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul7), !dbg !156
  %11 = bitcast i8* %call8 to i16*, !dbg !156
  store i16* %11, i16** %bin_size, align 4, !dbg !156
  br label %while.cond, !dbg !157

while.cond:                                       ; preds = %if.end97, %entry
  %12 = load i32* %im_count, align 4, !dbg !157
  %13 = load i32* %num_levels, align 4, !dbg !157
  %mul9 = mul nsw i32 3, %13, !dbg !157
  %add10 = add nsw i32 %mul9, 1, !dbg !157
  %cmp = icmp slt i32 %12, %add10, !dbg !157
  br i1 %cmp, label %while.body, label %while.end, !dbg !157

while.body:                                       ; preds = %while.cond
  %14 = load %struct._IO_FILE.8** %infile, align 4, !dbg !158
  %call11 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.8* %14), !dbg !158
  %15 = load i8* @temp_byte, align 1, !dbg !160
  store i8 %15, i8* %the_tag, align 1, !dbg !160
  store i32 1, i32* %first_image_flag, align 4, !dbg !161
  br label %for.cond, !dbg !161

for.cond:                                         ; preds = %if.end35, %while.body
  %16 = load i8* %the_tag, align 1, !dbg !161
  %conv = zext i8 %16 to i32, !dbg !161
  %and = and i32 %conv, -128, !dbg !161
  %tobool = icmp ne i32 %and, 0, !dbg !161
  br i1 %tobool, label %for.body, label %for.end, !dbg !161

for.body:                                         ; preds = %for.cond
  %17 = load i8* %the_tag, align 1, !dbg !163
  %conv12 = zext i8 %17 to i32, !dbg !163
  %and13 = and i32 %conv12, 127, !dbg !163
  store i32 %and13, i32* %index, align 4, !dbg !163
  %18 = load i32* %first_image_flag, align 4, !dbg !165
  %tobool14 = icmp ne i32 %18, 0, !dbg !165
  br i1 %tobool14, label %if.then, label %if.else27, !dbg !165

if.then:                                          ; preds = %for.body
  store i32 0, i32* %first_image_flag, align 4, !dbg !166
  %19 = load i32* %index, align 4, !dbg !168
  %cmp15 = icmp eq i32 %19, 0, !dbg !168
  br i1 %cmp15, label %if.then17, label %if.else, !dbg !168

if.then17:                                        ; preds = %if.then
  %20 = load i32* %x_size, align 4, !dbg !169
  %21 = load i32* %y_size, align 4, !dbg !169
  %mul18 = mul nsw i32 %20, %21, !dbg !169
  %22 = load i32* %num_levels, align 4, !dbg !169
  %mul19 = mul nsw i32 2, %22, !dbg !169
  %shr = ashr i32 %mul18, %mul19, !dbg !169
  store i32 %shr, i32* %block_size, align 4, !dbg !169
  store i32 0, i32* %block_pos, align 4, !dbg !171
  br label %if.end, !dbg !172

if.else:                                          ; preds = %if.then
  %23 = load i32* %x_size, align 4, !dbg !173
  %24 = load i32* %y_size, align 4, !dbg !173
  %mul20 = mul nsw i32 %23, %24, !dbg !173
  %25 = load i32* %num_levels, align 4, !dbg !173
  %26 = load i32* %index, align 4, !dbg !173
  %sub = sub nsw i32 %26, 1, !dbg !173
  %div = sdiv i32 %sub, 3, !dbg !173
  %sub21 = sub nsw i32 %25, %div, !dbg !173
  %mul22 = mul nsw i32 2, %sub21, !dbg !173
  %shr23 = ashr i32 %mul20, %mul22, !dbg !173
  store i32 %shr23, i32* %block_size, align 4, !dbg !173
  %27 = load i32* %block_size, align 4, !dbg !175
  %28 = load i32* %index, align 4, !dbg !175
  %sub24 = sub nsw i32 %28, 1, !dbg !175
  %rem = srem i32 %sub24, 3, !dbg !175
  %add25 = add nsw i32 1, %rem, !dbg !175
  %mul26 = mul nsw i32 %27, %add25, !dbg !175
  store i32 %mul26, i32* %block_pos, align 4, !dbg !175
  br label %if.end

if.end:                                           ; preds = %if.else, %if.then17
  br label %if.end35, !dbg !176

if.else27:                                        ; preds = %for.body
  %29 = load i32* %x_size, align 4, !dbg !177
  %30 = load i32* %y_size, align 4, !dbg !177
  %mul28 = mul nsw i32 %29, %30, !dbg !177
  %31 = load i32* %num_levels, align 4, !dbg !177
  %32 = load i32* %index, align 4, !dbg !177
  %sub29 = sub nsw i32 %32, 1, !dbg !177
  %div30 = sdiv i32 %sub29, 3, !dbg !177
  %sub31 = sub nsw i32 %31, %div30, !dbg !177
  %mul32 = mul nsw i32 2, %sub31, !dbg !177
  %shr33 = ashr i32 %mul28, %mul32, !dbg !177
  %33 = load i32* %block_size, align 4, !dbg !177
  %add34 = add nsw i32 %33, %shr33, !dbg !177
  store i32 %add34, i32* %block_size, align 4, !dbg !177
  br label %if.end35

if.end35:                                         ; preds = %if.else27, %if.end
  %34 = load %struct._IO_FILE.8** %infile, align 4, !dbg !178
  %call36 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.8* %34), !dbg !178
  %35 = load i8* @temp_byte, align 1, !dbg !179
  %conv37 = zext i8 %35 to i16, !dbg !179
  store i16 %conv37, i16* @temp_short, align 2, !dbg !179
  %36 = load i16* @temp_short, align 2, !dbg !180
  %conv38 = sext i16 %36 to i32, !dbg !180
  %shl = shl i32 %conv38, 8, !dbg !180
  %conv39 = trunc i32 %shl to i16, !dbg !180
  store i16 %conv39, i16* @temp_short, align 2, !dbg !180
  %37 = load %struct._IO_FILE.8** %infile, align 4, !dbg !181
  %call40 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.8* %37), !dbg !181
  %38 = load i8* @temp_byte, align 1, !dbg !182
  %conv41 = zext i8 %38 to i32, !dbg !182
  %39 = load i16* @temp_short, align 2, !dbg !182
  %conv42 = sext i16 %39 to i32, !dbg !182
  %or = or i32 %conv42, %conv41, !dbg !182
  %conv43 = trunc i32 %or to i16, !dbg !182
  store i16 %conv43, i16* @temp_short, align 2, !dbg !182
  %40 = load i16* @temp_short, align 2, !dbg !183
  %41 = load i32* %index, align 4, !dbg !183
  %42 = load i16** %bin_size, align 4, !dbg !183
  %arrayidx = getelementptr inbounds i16* %42, i32 %41, !dbg !183
  store i16 %40, i16* %arrayidx, !dbg !183
  %43 = load i32* %im_count, align 4, !dbg !184
  %inc = add nsw i32 %43, 1, !dbg !184
  store i32 %inc, i32* %im_count, align 4, !dbg !184
  %44 = load %struct._IO_FILE.8** %infile, align 4, !dbg !185
  %call44 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.8* %44), !dbg !185
  %45 = load i8* @temp_byte, align 1, !dbg !186
  store i8 %45, i8* %the_tag, align 1, !dbg !186
  br label %for.cond, !dbg !187

for.end:                                          ; preds = %for.cond
  %46 = load %struct._IO_FILE.8** %infile, align 4, !dbg !188
  %call45 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %46), !dbg !188
  %47 = load i32* %temp, align 4, !dbg !188
  %sub46 = sub nsw i32 %call45, %47, !dbg !188
  %sub47 = sub nsw i32 %sub46, 1, !dbg !188
  %call48 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([33 x i8]* @.str2, i32 0, i32 0), i32 %sub47), !dbg !188
  %48 = load %struct._IO_FILE.8** %infile, align 4, !dbg !189
  %call49 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %48), !dbg !189
  %sub50 = sub nsw i32 %call49, 1, !dbg !189
  store i32 %sub50, i32* %temp, align 4, !dbg !189
  %49 = load i8* %the_tag, align 1, !dbg !190
  %conv51 = zext i8 %49 to i32, !dbg !190
  %cmp52 = icmp eq i32 %conv51, 0, !dbg !190
  br i1 %cmp52, label %if.then54, label %if.else61, !dbg !190

if.then54:                                        ; preds = %for.end
  %50 = load i16** %q_pyr, align 4, !dbg !191
  %51 = load i32* %block_pos, align 4, !dbg !191
  %add.ptr = getelementptr inbounds i16* %50, i32 %51, !dbg !191
  %52 = bitcast i16* %add.ptr to i8*, !dbg !191
  %53 = load i32* %block_size, align 4, !dbg !191
  %mul55 = mul i32 2, %53, !dbg !191
  %54 = load %struct._IO_FILE.8** %infile, align 4, !dbg !191
  %call56 = call arm_aapcscc  i32 @fread(i8* %52, i32 1, i32 %mul55, %struct._IO_FILE.8* %54), !dbg !191
  %55 = load %struct._IO_FILE.8** %infile, align 4, !dbg !193
  %call57 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %55), !dbg !193
  %56 = load i32* %temp, align 4, !dbg !193
  %sub58 = sub nsw i32 %call57, %56, !dbg !193
  %call59 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([29 x i8]* @.str3, i32 0, i32 0), i32 %sub58), !dbg !193
  %57 = load %struct._IO_FILE.8** %infile, align 4, !dbg !194
  %call60 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %57), !dbg !194
  store i32 %call60, i32* %temp, align 4, !dbg !194
  br label %if.end97, !dbg !195

if.else61:                                        ; preds = %for.end
  %58 = load i8* %the_tag, align 1, !dbg !196
  %conv62 = zext i8 %58 to i32, !dbg !196
  %cmp63 = icmp eq i32 %conv62, 1, !dbg !196
  br i1 %cmp63, label %if.then65, label %if.else93, !dbg !196

if.then65:                                        ; preds = %if.else61
  %59 = load %struct._IO_FILE.8** %infile, align 4, !dbg !197
  %call66 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.8* %59), !dbg !197
  %60 = load i8* @temp_byte, align 1, !dbg !199
  %conv67 = zext i8 %60 to i32, !dbg !199
  store i32 %conv67, i32* @temp_int, align 4, !dbg !199
  %61 = load i32* @temp_int, align 4, !dbg !200
  %shl68 = shl i32 %61, 8, !dbg !200
  store i32 %shl68, i32* @temp_int, align 4, !dbg !200
  %62 = load %struct._IO_FILE.8** %infile, align 4, !dbg !201
  %call69 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.8* %62), !dbg !201
  %63 = load i8* @temp_byte, align 1, !dbg !202
  %conv70 = zext i8 %63 to i32, !dbg !202
  %64 = load i32* @temp_int, align 4, !dbg !202
  %or71 = or i32 %64, %conv70, !dbg !202
  store i32 %or71, i32* @temp_int, align 4, !dbg !202
  %65 = load i32* @temp_int, align 4, !dbg !203
  %shl72 = shl i32 %65, 8, !dbg !203
  store i32 %shl72, i32* @temp_int, align 4, !dbg !203
  %66 = load %struct._IO_FILE.8** %infile, align 4, !dbg !204
  %call73 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.8* %66), !dbg !204
  %67 = load i8* @temp_byte, align 1, !dbg !205
  %conv74 = zext i8 %67 to i32, !dbg !205
  %68 = load i32* @temp_int, align 4, !dbg !205
  %or75 = or i32 %68, %conv74, !dbg !205
  store i32 %or75, i32* @temp_int, align 4, !dbg !205
  %69 = load i32* @temp_int, align 4, !dbg !206
  %shl76 = shl i32 %69, 8, !dbg !206
  store i32 %shl76, i32* @temp_int, align 4, !dbg !206
  %70 = load %struct._IO_FILE.8** %infile, align 4, !dbg !207
  %call77 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.8* %70), !dbg !207
  %71 = load i8* @temp_byte, align 1, !dbg !208
  %conv78 = zext i8 %71 to i32, !dbg !208
  %72 = load i32* @temp_int, align 4, !dbg !208
  %or79 = or i32 %72, %conv78, !dbg !208
  store i32 %or79, i32* @temp_int, align 4, !dbg !208
  %73 = load i32* @temp_int, align 4, !dbg !209
  store i32 %73, i32* %symbol_stream_length, align 4, !dbg !209
  %74 = load i32* %symbol_stream_length, align 4, !dbg !210
  %mul80 = mul i32 %74, 2, !dbg !210
  %call81 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul80), !dbg !210
  %75 = bitcast i8* %call81 to i16*, !dbg !210
  store i16* %75, i16** %symbol_stream, align 4, !dbg !210
  %76 = load %struct._IO_FILE.8** %infile, align 4, !dbg !211
  %call82 = call arm_aapcscc  %struct.code_node.10* bitcast (%struct.code_node.10* (...)* @read_huffman_tree to %struct.code_node.10* (%struct._IO_FILE.8*)*)(%struct._IO_FILE.8* %76), !dbg !211
  store %struct.code_node.10* %call82, %struct.code_node.10** %huffman_tree, align 4, !dbg !211
  %77 = load %struct._IO_FILE.8** %infile, align 4, !dbg !212
  %call83 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %77), !dbg !212
  %78 = load i32* %temp, align 4, !dbg !212
  %sub84 = sub nsw i32 %call83, %78, !dbg !212
  %call85 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([29 x i8]* @.str4, i32 0, i32 0), i32 %sub84), !dbg !212
  %79 = load %struct._IO_FILE.8** %infile, align 4, !dbg !213
  %call86 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %79), !dbg !213
  store i32 %call86, i32* %temp, align 4, !dbg !213
  %80 = load %struct._IO_FILE.8** %infile, align 4, !dbg !214
  %81 = load %struct.code_node.10** %huffman_tree, align 4, !dbg !214
  %82 = load i16** %symbol_stream, align 4, !dbg !214
  %83 = load i32* %symbol_stream_length, align 4, !dbg !214
  %call87 = call arm_aapcscc  i32 bitcast (i32 (...)* @read_and_huffman_decode to i32 (%struct._IO_FILE.8*, %struct.code_node.10*, i16*, i32)*)(%struct._IO_FILE.8* %80, %struct.code_node.10* %81, i16* %82, i32 %83), !dbg !214
  %84 = load %struct._IO_FILE.8** %infile, align 4, !dbg !215
  %call88 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %84), !dbg !215
  %85 = load i32* %temp, align 4, !dbg !215
  %sub89 = sub nsw i32 %call88, %85, !dbg !215
  %call90 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([33 x i8]* @.str5, i32 0, i32 0), i32 %sub89), !dbg !215
  %86 = load i16** %symbol_stream, align 4, !dbg !216
  %87 = load i32* %block_size, align 4, !dbg !216
  %88 = load i16** %q_pyr, align 4, !dbg !216
  %89 = load i32* %block_pos, align 4, !dbg !216
  %add.ptr91 = getelementptr inbounds i16* %88, i32 %89, !dbg !216
  %call92 = call arm_aapcscc  i32 bitcast (i32 (...)* @run_length_decode_zeros to i32 (i16*, i32, i16*)*)(i16* %86, i32 %87, i16* %add.ptr91), !dbg !216
  br label %if.end96, !dbg !217

if.else93:                                        ; preds = %if.else61
  %90 = load i8* %the_tag, align 1, !dbg !218
  %conv94 = zext i8 %90 to i32, !dbg !218
  %call95 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([35 x i8]* @.str6, i32 0, i32 0), i32 %conv94), !dbg !218
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !220
  unreachable, !dbg !220

if.end96:                                         ; preds = %if.then65
  br label %if.end97

if.end97:                                         ; preds = %if.end96, %if.then54
  br label %while.cond, !dbg !221

while.end:                                        ; preds = %while.cond
  %91 = load %struct._IO_FILE.8** %infile, align 4, !dbg !222
  %call98 = call arm_aapcscc  i32 @ftell(%struct._IO_FILE.8* %91), !dbg !222
  %call99 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([28 x i8]* @.str7, i32 0, i32 0), i32 %call98), !dbg !222
  %92 = load %struct._IO_FILE.8** %infile, align 4, !dbg !223
  %call100 = call arm_aapcscc  i32 @fclose(%struct._IO_FILE.8* %92), !dbg !223
  %call101 = call arm_aapcscc  i32 bitcast (i32 (...)* @clock to i32 ()*)(), !dbg !224
  %call102 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([26 x i8]* @.str8, i32 0, i32 0)), !dbg !225
  %93 = load i32* %x_size, align 4, !dbg !226
  %94 = load i32* %y_size, align 4, !dbg !226
  %mul103 = mul nsw i32 %93, %94, !dbg !226
  %mul104 = mul i32 %mul103, 4, !dbg !226
  %call105 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul104), !dbg !226
  %95 = bitcast i8* %call105 to i32*, !dbg !226
  store i32* %95, i32** %pyr, align 4, !dbg !226
  %96 = load i16** %q_pyr, align 4, !dbg !227
  %97 = load i32** %pyr, align 4, !dbg !227
  %98 = load i32* %x_size, align 4, !dbg !227
  %99 = load i32* %y_size, align 4, !dbg !227
  %mul106 = mul nsw i32 %98, %99, !dbg !227
  %100 = load i32* %num_levels, align 4, !dbg !227
  %101 = load i16** %bin_size, align 4, !dbg !227
  %call107 = call arm_aapcscc  i32 bitcast (i32 (...)* @unquantize_pyr to i32 (i16*, i32*, i32, i32, i16*)*)(i16* %96, i32* %97, i32 %mul106, i32 %100, i16* %101), !dbg !227
  %102 = load i16** %q_pyr, align 4, !dbg !228
  %103 = bitcast i16* %102 to i8*, !dbg !228
  %call108 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %103), !dbg !228
  %call109 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([25 x i8]* @.str9, i32 0, i32 0)), !dbg !229
  %104 = load i32* %x_size, align 4, !dbg !230
  %105 = load i32* %y_size, align 4, !dbg !230
  %mul110 = mul nsw i32 %104, %105, !dbg !230
  %mul111 = mul i32 %mul110, 4, !dbg !230
  %call112 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul111), !dbg !230
  %106 = bitcast i8* %call112 to i32*, !dbg !230
  store i32* %106, i32** %result, align 4, !dbg !230
  %107 = load i32** %pyr, align 4, !dbg !231
  %108 = load i32** %result, align 4, !dbg !231
  %109 = load i32* %x_size, align 4, !dbg !231
  %110 = load i32* %y_size, align 4, !dbg !231
  %111 = load i32* %num_levels, align 4, !dbg !231
  %call113 = call arm_aapcscc  i32 bitcast (i32 (...)* @collapse_pyr to i32 (i32*, i32*, i32, i32, i32)*)(i32* %107, i32* %108, i32 %109, i32 %110, i32 %111), !dbg !231
  store i32 0, i32* %i, align 4, !dbg !232
  br label %for.cond114, !dbg !232

for.cond114:                                      ; preds = %for.inc, %while.end
  %112 = load i32* %i, align 4, !dbg !232
  %113 = load i32* %x_size, align 4, !dbg !232
  %114 = load i32* %y_size, align 4, !dbg !232
  %mul115 = mul nsw i32 %113, %114, !dbg !232
  %cmp116 = icmp slt i32 %112, %mul115, !dbg !232
  br i1 %cmp116, label %for.body118, label %for.end138, !dbg !232

for.body118:                                      ; preds = %for.cond114
  %115 = load i32* %i, align 4, !dbg !234
  %116 = load i32** %result, align 4, !dbg !234
  %arrayidx119 = getelementptr inbounds i32* %116, i32 %115, !dbg !234
  %117 = load i32* %arrayidx119, !dbg !234
  %conv120 = sitofp i32 %117 to double, !dbg !234
  %118 = load double* %scale_factor, align 8, !dbg !234
  %div121 = fdiv double %conv120, %118, !dbg !234
  store double %div121, double* %dtemp, align 8, !dbg !234
  %119 = load double* %dtemp, align 8, !dbg !236
  %cmp122 = fcmp olt double %119, 0.000000e+00, !dbg !236
  br i1 %cmp122, label %if.then124, label %if.else126, !dbg !236

if.then124:                                       ; preds = %for.body118
  %120 = load i32* %i, align 4, !dbg !237
  %121 = load i32** %result, align 4, !dbg !237
  %arrayidx125 = getelementptr inbounds i32* %121, i32 %120, !dbg !237
  store i32 0, i32* %arrayidx125, !dbg !237
  br label %if.end136, !dbg !237

if.else126:                                       ; preds = %for.body118
  %122 = load double* %dtemp, align 8, !dbg !238
  %cmp127 = fcmp ogt double %122, 2.550000e+02, !dbg !238
  br i1 %cmp127, label %if.then129, label %if.else131, !dbg !238

if.then129:                                       ; preds = %if.else126
  %123 = load i32* %i, align 4, !dbg !239
  %124 = load i32** %result, align 4, !dbg !239
  %arrayidx130 = getelementptr inbounds i32* %124, i32 %123, !dbg !239
  store i32 255, i32* %arrayidx130, !dbg !239
  br label %if.end135, !dbg !239

if.else131:                                       ; preds = %if.else126
  %125 = load double* %dtemp, align 8, !dbg !240
  %add132 = fadd double %125, 5.000000e-01, !dbg !240
  %conv133 = fptosi double %add132 to i32, !dbg !240
  %126 = load i32* %i, align 4, !dbg !240
  %127 = load i32** %result, align 4, !dbg !240
  %arrayidx134 = getelementptr inbounds i32* %127, i32 %126, !dbg !240
  store i32 %conv133, i32* %arrayidx134, !dbg !240
  br label %if.end135

if.end135:                                        ; preds = %if.else131, %if.then129
  br label %if.end136

if.end136:                                        ; preds = %if.end135, %if.then124
  br label %for.inc, !dbg !241

for.inc:                                          ; preds = %if.end136
  %128 = load i32* %i, align 4, !dbg !242
  %inc137 = add nsw i32 %128, 1, !dbg !242
  store i32 %inc137, i32* %i, align 4, !dbg !242
  br label %for.cond114, !dbg !242

for.end138:                                       ; preds = %for.cond114
  %call139 = call arm_aapcscc  i32 bitcast (i32 (...)* @clock to i32 ()*)(), !dbg !243
  %div140 = sdiv i32 %call139, 1000, !dbg !243
  %call141 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([32 x i8]* @.str10, i32 0, i32 0), i32 %div140), !dbg !243
  %call142 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([26 x i8]* @.str11, i32 0, i32 0)), !dbg !244
  %129 = load %struct._IO_FILE.8** %outfile, align 4, !dbg !245
  %130 = load i32** %result, align 4, !dbg !245
  %131 = load i32* %x_size, align 4, !dbg !245
  %132 = load i32* %y_size, align 4, !dbg !245
  %call143 = call arm_aapcscc  i32 bitcast (i32 (...)* @write_pgm_image to i32 (%struct._IO_FILE.8*, i32*, i32, i32)*)(%struct._IO_FILE.8* %129, i32* %130, i32 %131, i32 %132), !dbg !245
  %133 = load i32** %pyr, align 4, !dbg !246
  %134 = bitcast i32* %133 to i8*, !dbg !246
  %call144 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %134), !dbg !246
  %135 = load i32** %result, align 4, !dbg !247
  %136 = bitcast i32* %135 to i8*, !dbg !247
  %call145 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %136), !dbg !247
  %137 = load i16** %bin_size, align 4, !dbg !248
  %138 = bitcast i16* %137 to i8*, !dbg !248
  %call146 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %138), !dbg !248
  %139 = load i16** %symbol_stream, align 4, !dbg !249
  %140 = bitcast i16* %139 to i8*, !dbg !249
  %call147 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %140), !dbg !249
  %141 = load i32* %retval, !dbg !250
  ret i32 %141, !dbg !250
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc i32 @parse_unepic_args(...)

declare arm_aapcscc i32 @printf(i8*, ...)

declare arm_aapcscc i32 @ftell(%struct._IO_FILE.8*)

declare arm_aapcscc i8* @check_malloc(...)

declare arm_aapcscc i32 @fread(i8*, i32, i32, %struct._IO_FILE.8*)

declare arm_aapcscc %struct.code_node.10* @read_huffman_tree(...)

declare arm_aapcscc i32 @read_and_huffman_decode(...)

declare arm_aapcscc i32 @run_length_decode_zeros(...)

declare arm_aapcscc void @exit(i32) noreturn

declare arm_aapcscc i32 @fclose(%struct._IO_FILE.8*)

declare arm_aapcscc i32 @clock(...)

declare arm_aapcscc i32 @unquantize_pyr(...)

declare arm_aapcscc i32 @check_free(...)

declare arm_aapcscc i32 @collapse_pyr(...)

declare arm_aapcscc i32 @write_pgm_image(...)

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !12} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"main", metadata !"main", metadata !"", metadata !6, i32 34, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i32, i8**)* @main, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"unepic.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{metadata !13}
!13 = metadata !{metadata !14, metadata !24}
!14 = metadata !{i32 720948, i32 0, null, metadata !"ss_time_start", metadata !"ss_time_start", metadata !"", metadata !15, i32 6, metadata !16, i32 0, i32 1, %struct.timeval.11* @ss_time_start} ; [ DW_TAG_variable ]
!15 = metadata !{i32 720937, metadata !"stats_time.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!16 = metadata !{i32 720915, null, metadata !"timeval", metadata !17, i32 69, i64 64, i64 32, i32 0, i32 0, null, metadata !18, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!17 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!18 = metadata !{metadata !19, metadata !22}
!19 = metadata !{i32 720909, metadata !16, metadata !"tv_sec", metadata !17, i32 71, i64 32, i64 32, i64 0, i32 0, metadata !20} ; [ DW_TAG_member ]
!20 = metadata !{i32 720918, null, metadata !"__time_t", metadata !17, i32 149, i64 0, i64 0, i64 0, i32 0, metadata !21} ; [ DW_TAG_typedef ]
!21 = metadata !{i32 720932, null, metadata !"long int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!22 = metadata !{i32 720909, metadata !16, metadata !"tv_usec", metadata !17, i32 72, i64 32, i64 32, i64 32, i32 0, metadata !23} ; [ DW_TAG_member ]
!23 = metadata !{i32 720918, null, metadata !"__suseconds_t", metadata !17, i32 151, i64 0, i64 0, i64 0, i32 0, metadata !21} ; [ DW_TAG_typedef ]
!24 = metadata !{i32 720948, i32 0, null, metadata !"ss_time_end", metadata !"ss_time_end", metadata !"", metadata !15, i32 7, metadata !16, i32 0, i32 1, %struct.timeval.11* @ss_time_end} ; [ DW_TAG_variable ]
!25 = metadata !{i32 721153, metadata !5, metadata !"argc", metadata !6, i32 16777248, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!26 = metadata !{i32 32, i32 7, metadata !5, null}
!27 = metadata !{i32 721153, metadata !5, metadata !"argv", metadata !6, i32 33554465, metadata !28, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!28 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !29} ; [ DW_TAG_pointer_type ]
!29 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !30} ; [ DW_TAG_pointer_type ]
!30 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!31 = metadata !{i32 33, i32 9, metadata !5, null}
!32 = metadata !{i32 721152, metadata !33, metadata !"infile", metadata !6, i32 36, metadata !34, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!33 = metadata !{i32 720907, metadata !5, i32 34, i32 3, metadata !6, i32 134} ; [ DW_TAG_lexical_block ]
!34 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !35} ; [ DW_TAG_pointer_type ]
!35 = metadata !{i32 720918, null, metadata !"FILE", metadata !6, i32 49, i64 0, i64 0, i64 0, i32 0, metadata !36} ; [ DW_TAG_typedef ]
!36 = metadata !{i32 720915, null, metadata !"_IO_FILE", metadata !37, i32 271, i64 1216, i64 64, i32 0, i32 0, null, metadata !38, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!37 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!38 = metadata !{metadata !39, metadata !40, metadata !41, metadata !42, metadata !43, metadata !44, metadata !45, metadata !46, metadata !47, metadata !48, metadata !49, metadata !50, metadata !51, metadata !59, metadata !60, metadata !61, metadata !62, metadata !64, metadata !66, metadata !68, metadata !72, metadata !74, metadata !78, metadata !79, metadata !80, metadata !81, metadata !82, metadata !85, metadata !86}
!39 = metadata !{i32 720909, metadata !36, metadata !"_flags", metadata !37, i32 272, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_member ]
!40 = metadata !{i32 720909, metadata !36, metadata !"_IO_read_ptr", metadata !37, i32 277, i64 32, i64 32, i64 32, i32 0, metadata !29} ; [ DW_TAG_member ]
!41 = metadata !{i32 720909, metadata !36, metadata !"_IO_read_end", metadata !37, i32 278, i64 32, i64 32, i64 64, i32 0, metadata !29} ; [ DW_TAG_member ]
!42 = metadata !{i32 720909, metadata !36, metadata !"_IO_read_base", metadata !37, i32 279, i64 32, i64 32, i64 96, i32 0, metadata !29} ; [ DW_TAG_member ]
!43 = metadata !{i32 720909, metadata !36, metadata !"_IO_write_base", metadata !37, i32 280, i64 32, i64 32, i64 128, i32 0, metadata !29} ; [ DW_TAG_member ]
!44 = metadata !{i32 720909, metadata !36, metadata !"_IO_write_ptr", metadata !37, i32 281, i64 32, i64 32, i64 160, i32 0, metadata !29} ; [ DW_TAG_member ]
!45 = metadata !{i32 720909, metadata !36, metadata !"_IO_write_end", metadata !37, i32 282, i64 32, i64 32, i64 192, i32 0, metadata !29} ; [ DW_TAG_member ]
!46 = metadata !{i32 720909, metadata !36, metadata !"_IO_buf_base", metadata !37, i32 283, i64 32, i64 32, i64 224, i32 0, metadata !29} ; [ DW_TAG_member ]
!47 = metadata !{i32 720909, metadata !36, metadata !"_IO_buf_end", metadata !37, i32 284, i64 32, i64 32, i64 256, i32 0, metadata !29} ; [ DW_TAG_member ]
!48 = metadata !{i32 720909, metadata !36, metadata !"_IO_save_base", metadata !37, i32 286, i64 32, i64 32, i64 288, i32 0, metadata !29} ; [ DW_TAG_member ]
!49 = metadata !{i32 720909, metadata !36, metadata !"_IO_backup_base", metadata !37, i32 287, i64 32, i64 32, i64 320, i32 0, metadata !29} ; [ DW_TAG_member ]
!50 = metadata !{i32 720909, metadata !36, metadata !"_IO_save_end", metadata !37, i32 288, i64 32, i64 32, i64 352, i32 0, metadata !29} ; [ DW_TAG_member ]
!51 = metadata !{i32 720909, metadata !36, metadata !"_markers", metadata !37, i32 290, i64 32, i64 32, i64 384, i32 0, metadata !52} ; [ DW_TAG_member ]
!52 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !53} ; [ DW_TAG_pointer_type ]
!53 = metadata !{i32 720915, null, metadata !"_IO_marker", metadata !37, i32 186, i64 96, i64 32, i32 0, i32 0, null, metadata !54, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!54 = metadata !{metadata !55, metadata !56, metadata !58}
!55 = metadata !{i32 720909, metadata !53, metadata !"_next", metadata !37, i32 187, i64 32, i64 32, i64 0, i32 0, metadata !52} ; [ DW_TAG_member ]
!56 = metadata !{i32 720909, metadata !53, metadata !"_sbuf", metadata !37, i32 188, i64 32, i64 32, i64 32, i32 0, metadata !57} ; [ DW_TAG_member ]
!57 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !36} ; [ DW_TAG_pointer_type ]
!58 = metadata !{i32 720909, metadata !53, metadata !"_pos", metadata !37, i32 192, i64 32, i64 32, i64 64, i32 0, metadata !9} ; [ DW_TAG_member ]
!59 = metadata !{i32 720909, metadata !36, metadata !"_chain", metadata !37, i32 292, i64 32, i64 32, i64 416, i32 0, metadata !57} ; [ DW_TAG_member ]
!60 = metadata !{i32 720909, metadata !36, metadata !"_fileno", metadata !37, i32 294, i64 32, i64 32, i64 448, i32 0, metadata !9} ; [ DW_TAG_member ]
!61 = metadata !{i32 720909, metadata !36, metadata !"_flags2", metadata !37, i32 298, i64 32, i64 32, i64 480, i32 0, metadata !9} ; [ DW_TAG_member ]
!62 = metadata !{i32 720909, metadata !36, metadata !"_old_offset", metadata !37, i32 300, i64 32, i64 32, i64 512, i32 0, metadata !63} ; [ DW_TAG_member ]
!63 = metadata !{i32 720918, null, metadata !"__off_t", metadata !37, i32 141, i64 0, i64 0, i64 0, i32 0, metadata !21} ; [ DW_TAG_typedef ]
!64 = metadata !{i32 720909, metadata !36, metadata !"_cur_column", metadata !37, i32 304, i64 16, i64 16, i64 544, i32 0, metadata !65} ; [ DW_TAG_member ]
!65 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!66 = metadata !{i32 720909, metadata !36, metadata !"_vtable_offset", metadata !37, i32 305, i64 8, i64 8, i64 560, i32 0, metadata !67} ; [ DW_TAG_member ]
!67 = metadata !{i32 720932, null, metadata !"signed char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!68 = metadata !{i32 720909, metadata !36, metadata !"_shortbuf", metadata !37, i32 306, i64 8, i64 8, i64 568, i32 0, metadata !69} ; [ DW_TAG_member ]
!69 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8, i64 8, i32 0, i32 0, metadata !30, metadata !70, i32 0, i32 0} ; [ DW_TAG_array_type ]
!70 = metadata !{metadata !71}
!71 = metadata !{i32 720929, i64 0, i64 0}        ; [ DW_TAG_subrange_type ]
!72 = metadata !{i32 720909, metadata !36, metadata !"_lock", metadata !37, i32 310, i64 32, i64 32, i64 576, i32 0, metadata !73} ; [ DW_TAG_member ]
!73 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, null} ; [ DW_TAG_pointer_type ]
!74 = metadata !{i32 720909, metadata !36, metadata !"_offset", metadata !37, i32 319, i64 64, i64 64, i64 640, i32 0, metadata !75} ; [ DW_TAG_member ]
!75 = metadata !{i32 720918, null, metadata !"__off64_t", metadata !37, i32 142, i64 0, i64 0, i64 0, i32 0, metadata !76} ; [ DW_TAG_typedef ]
!76 = metadata !{i32 720918, null, metadata !"__quad_t", metadata !37, i32 56, i64 0, i64 0, i64 0, i32 0, metadata !77} ; [ DW_TAG_typedef ]
!77 = metadata !{i32 720932, null, metadata !"long long int", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!78 = metadata !{i32 720909, metadata !36, metadata !"__pad1", metadata !37, i32 328, i64 32, i64 32, i64 704, i32 0, metadata !73} ; [ DW_TAG_member ]
!79 = metadata !{i32 720909, metadata !36, metadata !"__pad2", metadata !37, i32 329, i64 32, i64 32, i64 736, i32 0, metadata !73} ; [ DW_TAG_member ]
!80 = metadata !{i32 720909, metadata !36, metadata !"__pad3", metadata !37, i32 330, i64 32, i64 32, i64 768, i32 0, metadata !73} ; [ DW_TAG_member ]
!81 = metadata !{i32 720909, metadata !36, metadata !"__pad4", metadata !37, i32 331, i64 32, i64 32, i64 800, i32 0, metadata !73} ; [ DW_TAG_member ]
!82 = metadata !{i32 720909, metadata !36, metadata !"__pad5", metadata !37, i32 332, i64 32, i64 32, i64 832, i32 0, metadata !83} ; [ DW_TAG_member ]
!83 = metadata !{i32 720918, null, metadata !"size_t", metadata !37, i32 211, i64 0, i64 0, i64 0, i32 0, metadata !84} ; [ DW_TAG_typedef ]
!84 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!85 = metadata !{i32 720909, metadata !36, metadata !"_mode", metadata !37, i32 334, i64 32, i64 32, i64 864, i32 0, metadata !9} ; [ DW_TAG_member ]
!86 = metadata !{i32 720909, metadata !36, metadata !"_unused2", metadata !37, i32 336, i64 320, i64 8, i64 896, i32 0, metadata !87} ; [ DW_TAG_member ]
!87 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !30, metadata !88, i32 0, i32 0} ; [ DW_TAG_array_type ]
!88 = metadata !{metadata !89}
!89 = metadata !{i32 720929, i64 0, i64 39}       ; [ DW_TAG_subrange_type ]
!90 = metadata !{i32 36, i32 9, metadata !33, null}
!91 = metadata !{i32 721152, metadata !33, metadata !"outfile", metadata !6, i32 36, metadata !34, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!92 = metadata !{i32 36, i32 18, metadata !33, null}
!93 = metadata !{i32 721152, metadata !33, metadata !"result", metadata !6, i32 37, metadata !94, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!94 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_pointer_type ]
!95 = metadata !{i32 37, i32 8, metadata !33, null}
!96 = metadata !{i32 721152, metadata !33, metadata !"pyr", metadata !6, i32 37, metadata !94, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!97 = metadata !{i32 37, i32 17, metadata !33, null}
!98 = metadata !{i32 721152, metadata !33, metadata !"q_pyr", metadata !6, i32 38, metadata !99, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!99 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !100} ; [ DW_TAG_pointer_type ]
!100 = metadata !{i32 720918, null, metadata !"BinIndexType", metadata !6, i32 59, i64 0, i64 0, i64 0, i32 0, metadata !101} ; [ DW_TAG_typedef ]
!101 = metadata !{i32 720932, null, metadata !"short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!102 = metadata !{i32 38, i32 17, metadata !33, null}
!103 = metadata !{i32 721152, metadata !33, metadata !"x_size", metadata !6, i32 39, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!104 = metadata !{i32 39, i32 7, metadata !33, null}
!105 = metadata !{i32 721152, metadata !33, metadata !"y_size", metadata !6, i32 39, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!106 = metadata !{i32 39, i32 15, metadata !33, null}
!107 = metadata !{i32 721152, metadata !33, metadata !"num_levels", metadata !6, i32 39, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!108 = metadata !{i32 39, i32 23, metadata !33, null}
!109 = metadata !{i32 721152, metadata !33, metadata !"scale_factor", metadata !6, i32 40, metadata !110, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!110 = metadata !{i32 720932, null, metadata !"double", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!111 = metadata !{i32 40, i32 10, metadata !33, null}
!112 = metadata !{i32 721152, metadata !33, metadata !"dtemp", metadata !6, i32 40, metadata !110, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!113 = metadata !{i32 40, i32 24, metadata !33, null}
!114 = metadata !{i32 721152, metadata !33, metadata !"bin_size", metadata !6, i32 41, metadata !115, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!115 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !116} ; [ DW_TAG_pointer_type ]
!116 = metadata !{i32 720918, null, metadata !"BinValueType", metadata !6, i32 64, i64 0, i64 0, i64 0, i32 0, metadata !65} ; [ DW_TAG_typedef ]
!117 = metadata !{i32 41, i32 17, metadata !33, null}
!118 = metadata !{i32 721152, metadata !33, metadata !"the_tag", metadata !6, i32 42, metadata !119, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!119 = metadata !{i32 720918, null, metadata !"Byte", metadata !6, i32 76, i64 0, i64 0, i64 0, i32 0, metadata !120} ; [ DW_TAG_typedef ]
!120 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!121 = metadata !{i32 42, i32 8, metadata !33, null}
!122 = metadata !{i32 721152, metadata !33, metadata !"temp", metadata !6, i32 43, metadata !21, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!123 = metadata !{i32 43, i32 8, metadata !33, null}
!124 = metadata !{i32 721152, metadata !33, metadata !"i", metadata !6, i32 44, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!125 = metadata !{i32 44, i32 7, metadata !33, null}
!126 = metadata !{i32 721152, metadata !33, metadata !"index", metadata !6, i32 44, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!127 = metadata !{i32 44, i32 10, metadata !33, null}
!128 = metadata !{i32 721152, metadata !33, metadata !"im_count", metadata !6, i32 44, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!129 = metadata !{i32 44, i32 17, metadata !33, null}
!130 = metadata !{i32 44, i32 68, metadata !33, null}
!131 = metadata !{i32 721152, metadata !33, metadata !"block_size", metadata !6, i32 44, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!132 = metadata !{i32 44, i32 29, metadata !33, null}
!133 = metadata !{i32 721152, metadata !33, metadata !"block_pos", metadata !6, i32 44, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!134 = metadata !{i32 44, i32 41, metadata !33, null}
!135 = metadata !{i32 721152, metadata !33, metadata !"first_image_flag", metadata !6, i32 44, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!136 = metadata !{i32 44, i32 52, metadata !33, null}
!137 = metadata !{i32 721152, metadata !33, metadata !"symbol_stream_length", metadata !6, i32 45, metadata !84, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!138 = metadata !{i32 45, i32 16, metadata !33, null}
!139 = metadata !{i32 721152, metadata !33, metadata !"symbol_stream", metadata !6, i32 46, metadata !140, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!140 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !141} ; [ DW_TAG_pointer_type ]
!141 = metadata !{i32 720918, null, metadata !"SymbolType", metadata !6, i32 74, i64 0, i64 0, i64 0, i32 0, metadata !65} ; [ DW_TAG_typedef ]
!142 = metadata !{i32 46, i32 15, metadata !33, null}
!143 = metadata !{i32 721152, metadata !33, metadata !"huffman_tree", metadata !6, i32 47, metadata !144, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!144 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !145} ; [ DW_TAG_pointer_type ]
!145 = metadata !{i32 720915, null, metadata !"code_node", metadata !146, i32 142, i64 96, i64 32, i32 0, i32 0, null, metadata !147, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!146 = metadata !{i32 720937, metadata !"epic.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!147 = metadata !{metadata !148, metadata !149, metadata !150}
!148 = metadata !{i32 720909, metadata !145, metadata !"zero_child", metadata !146, i32 144, i64 32, i64 32, i64 0, i32 0, metadata !144} ; [ DW_TAG_member ]
!149 = metadata !{i32 720909, metadata !145, metadata !"one_child", metadata !146, i32 144, i64 32, i64 32, i64 32, i32 0, metadata !144} ; [ DW_TAG_member ]
!150 = metadata !{i32 720909, metadata !145, metadata !"symbol", metadata !146, i32 145, i64 16, i64 16, i64 64, i32 0, metadata !141} ; [ DW_TAG_member ]
!151 = metadata !{i32 47, i32 21, metadata !33, null}
!152 = metadata !{i32 51, i32 3, metadata !33, null}
!153 = metadata !{i32 54, i32 45, metadata !33, null}
!154 = metadata !{i32 55, i32 3, metadata !33, null}
!155 = metadata !{i32 59, i32 28, metadata !33, null}
!156 = metadata !{i32 60, i32 31, metadata !33, null}
!157 = metadata !{i32 62, i32 3, metadata !33, null}
!158 = metadata !{i32 64, i32 7, metadata !159, null}
!159 = metadata !{i32 720907, metadata !33, i32 63, i32 7, metadata !6, i32 135} ; [ DW_TAG_lexical_block ]
!160 = metadata !{i32 64, i32 37, metadata !159, null}
!161 = metadata !{i32 65, i32 12, metadata !162, null}
!162 = metadata !{i32 720907, metadata !159, i32 65, i32 7, metadata !6, i32 136} ; [ DW_TAG_lexical_block ]
!163 = metadata !{i32 68, i32 4, metadata !164, null}
!164 = metadata !{i32 720907, metadata !162, i32 67, i32 4, metadata !6, i32 137} ; [ DW_TAG_lexical_block ]
!165 = metadata !{i32 70, i32 4, metadata !164, null}
!166 = metadata !{i32 72, i32 8, metadata !167, null}
!167 = metadata !{i32 720907, metadata !164, i32 71, i32 8, metadata !6, i32 138} ; [ DW_TAG_lexical_block ]
!168 = metadata !{i32 73, i32 8, metadata !167, null}
!169 = metadata !{i32 74, i32 7, metadata !170, null}
!170 = metadata !{i32 720907, metadata !167, i32 74, i32 5, metadata !6, i32 139} ; [ DW_TAG_lexical_block ]
!171 = metadata !{i32 74, i32 53, metadata !170, null}
!172 = metadata !{i32 74, i32 68, metadata !170, null}
!173 = metadata !{i32 77, i32 5, metadata !174, null}
!174 = metadata !{i32 720907, metadata !167, i32 76, i32 5, metadata !6, i32 140} ; [ DW_TAG_lexical_block ]
!175 = metadata !{i32 78, i32 5, metadata !174, null}
!176 = metadata !{i32 80, i32 8, metadata !167, null}
!177 = metadata !{i32 81, i32 9, metadata !164, null}
!178 = metadata !{i32 82, i32 4, metadata !164, null}
!179 = metadata !{i32 82, i32 34, metadata !164, null}
!180 = metadata !{i32 82, i32 58, metadata !164, null}
!181 = metadata !{i32 82, i32 75, metadata !164, null}
!182 = metadata !{i32 82, i32 105, metadata !164, null}
!183 = metadata !{i32 82, i32 130, metadata !164, null}
!184 = metadata !{i32 83, i32 4, metadata !164, null}
!185 = metadata !{i32 84, i32 4, metadata !164, null}
!186 = metadata !{i32 84, i32 34, metadata !164, null}
!187 = metadata !{i32 85, i32 4, metadata !164, null}
!188 = metadata !{i32 86, i32 52, metadata !159, null}
!189 = metadata !{i32 87, i32 14, metadata !159, null}
!190 = metadata !{i32 90, i32 7, metadata !159, null}
!191 = metadata !{i32 92, i32 4, metadata !192, null}
!192 = metadata !{i32 720907, metadata !159, i32 91, i32 4, metadata !6, i32 141} ; [ DW_TAG_lexical_block ]
!193 = metadata !{i32 93, i32 45, metadata !192, null}
!194 = metadata !{i32 94, i32 11, metadata !192, null}
!195 = metadata !{i32 95, i32 4, metadata !192, null}
!196 = metadata !{i32 96, i32 12, metadata !159, null}
!197 = metadata !{i32 98, i32 4, metadata !198, null}
!198 = metadata !{i32 720907, metadata !159, i32 97, i32 4, metadata !6, i32 142} ; [ DW_TAG_lexical_block ]
!199 = metadata !{i32 98, i32 34, metadata !198, null}
!200 = metadata !{i32 98, i32 56, metadata !198, null}
!201 = metadata !{i32 98, i32 72, metadata !198, null}
!202 = metadata !{i32 98, i32 102, metadata !198, null}
!203 = metadata !{i32 98, i32 125, metadata !198, null}
!204 = metadata !{i32 98, i32 141, metadata !198, null}
!205 = metadata !{i32 98, i32 171, metadata !198, null}
!206 = metadata !{i32 98, i32 194, metadata !198, null}
!207 = metadata !{i32 98, i32 210, metadata !198, null}
!208 = metadata !{i32 98, i32 240, metadata !198, null}
!209 = metadata !{i32 98, i32 0, metadata !198, null}
!210 = metadata !{i32 100, i32 6, metadata !198, null}
!211 = metadata !{i32 101, i32 19, metadata !198, null}
!212 = metadata !{i32 102, i32 44, metadata !198, null}
!213 = metadata !{i32 103, i32 11, metadata !198, null}
!214 = metadata !{i32 104, i32 4, metadata !198, null}
!215 = metadata !{i32 106, i32 49, metadata !198, null}
!216 = metadata !{i32 107, i32 4, metadata !198, null}
!217 = metadata !{i32 108, i32 4, metadata !198, null}
!218 = metadata !{i32 109, i32 14, metadata !219, null}
!219 = metadata !{i32 720907, metadata !159, i32 109, i32 12, metadata !6, i32 143} ; [ DW_TAG_lexical_block ]
!220 = metadata !{i32 109, i32 70, metadata !219, null}
!221 = metadata !{i32 110, i32 7, metadata !159, null}
!222 = metadata !{i32 111, i32 42, metadata !33, null}
!223 = metadata !{i32 112, i32 3, metadata !33, null}
!224 = metadata !{i32 114, i32 3, metadata !33, null}
!225 = metadata !{i32 117, i32 3, metadata !33, null}
!226 = metadata !{i32 118, i32 17, metadata !33, null}
!227 = metadata !{i32 119, i32 3, metadata !33, null}
!228 = metadata !{i32 120, i32 3, metadata !33, null}
!229 = metadata !{i32 123, i32 3, metadata !33, null}
!230 = metadata !{i32 124, i32 20, metadata !33, null}
!231 = metadata !{i32 126, i32 3, metadata !33, null}
!232 = metadata !{i32 129, i32 8, metadata !233, null}
!233 = metadata !{i32 720907, metadata !33, i32 129, i32 3, metadata !6, i32 144} ; [ DW_TAG_lexical_block ]
!234 = metadata !{i32 131, i32 5, metadata !235, null}
!235 = metadata !{i32 720907, metadata !233, i32 130, i32 5, metadata !6, i32 145} ; [ DW_TAG_lexical_block ]
!236 = metadata !{i32 132, i32 5, metadata !235, null}
!237 = metadata !{i32 132, i32 20, metadata !235, null}
!238 = metadata !{i32 133, i32 10, metadata !235, null}
!239 = metadata !{i32 133, i32 27, metadata !235, null}
!240 = metadata !{i32 134, i32 10, metadata !235, null}
!241 = metadata !{i32 135, i32 5, metadata !235, null}
!242 = metadata !{i32 129, i32 30, metadata !233, null}
!243 = metadata !{i32 136, i32 47, metadata !33, null}
!244 = metadata !{i32 138, i32 3, metadata !33, null}
!245 = metadata !{i32 140, i32 10, metadata !33, null}
!246 = metadata !{i32 143, i32 3, metadata !33, null}
!247 = metadata !{i32 144, i32 3, metadata !33, null}
!248 = metadata !{i32 145, i32 3, metadata !33, null}
!249 = metadata !{i32 146, i32 3, metadata !33, null}
!250 = metadata !{i32 150, i32 3, metadata !33, null}
