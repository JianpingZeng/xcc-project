; ModuleID = 'run_length.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

@.str = private unnamed_addr constant [56 x i8] c"Zero length overflow error in run_length_encode_zeros!\0A\00", align 1
@.str1 = private unnamed_addr constant [50 x i8] c"Value overflow error in run_length_encode_zeros!\0A\00", align 1

define arm_aapcscc i32 @run_length_encode_zeros(i16* %block, i32 %block_size, i16* %encoded_stream) nounwind uwtable {
entry:
  %block.addr = alloca i16*, align 4
  %block_size.addr = alloca i32, align 4
  %encoded_stream.addr = alloca i16*, align 4
  %value = alloca i16, align 2
  %block_pos = alloca i32, align 4
  %stream_pos = alloca i32, align 4
  %count = alloca i32, align 4
  %mask = alloca i32, align 4
  %sign_mask = alloca i16, align 2
  %value_bits_mask = alloca i16, align 2
  %bit_position = alloca i16, align 2
  %zero_run_mask = alloca i16, align 2
  store i16* %block, i16** %block.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %block.addr}, metadata !15), !dbg !19
  store i32 %block_size, i32* %block_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %block_size.addr}, metadata !20), !dbg !21
  store i16* %encoded_stream, i16** %encoded_stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %encoded_stream.addr}, metadata !22), !dbg !26
  call void @llvm.dbg.declare(metadata !{i16* %value}, metadata !27), !dbg !29
  call void @llvm.dbg.declare(metadata !{i32* %block_pos}, metadata !30), !dbg !32
  store i32 0, i32* %block_pos, align 4, !dbg !33
  call void @llvm.dbg.declare(metadata !{i32* %stream_pos}, metadata !34), !dbg !35
  store i32 0, i32* %stream_pos, align 4, !dbg !33
  call void @llvm.dbg.declare(metadata !{i32* %count}, metadata !36), !dbg !37
  call void @llvm.dbg.declare(metadata !{i32* %mask}, metadata !38), !dbg !39
  call void @llvm.dbg.declare(metadata !{i16* %sign_mask}, metadata !40), !dbg !41
  store i16 16384, i16* %sign_mask, align 2, !dbg !42
  call void @llvm.dbg.declare(metadata !{i16* %value_bits_mask}, metadata !43), !dbg !44
  %0 = load i16* %sign_mask, align 2, !dbg !45
  %conv = zext i16 %0 to i32, !dbg !45
  %sub = sub nsw i32 %conv, 1, !dbg !45
  %conv1 = trunc i32 %sub to i16, !dbg !45
  store i16 %conv1, i16* %value_bits_mask, align 2, !dbg !45
  call void @llvm.dbg.declare(metadata !{i16* %bit_position}, metadata !46), !dbg !47
  call void @llvm.dbg.declare(metadata !{i16* %zero_run_mask}, metadata !48), !dbg !49
  store i16 -32768, i16* %zero_run_mask, align 2, !dbg !50
  br label %while.cond, !dbg !51

while.cond:                                       ; preds = %if.end66, %entry
  %1 = load i32* %block_pos, align 4, !dbg !51
  %2 = load i32* %block_size.addr, align 4, !dbg !51
  %cmp = icmp ult i32 %1, %2, !dbg !51
  br i1 %cmp, label %while.body, label %while.end, !dbg !51

while.body:                                       ; preds = %while.cond
  %3 = load i32* %block_pos, align 4, !dbg !52
  %4 = load i16** %block.addr, align 4, !dbg !52
  %arrayidx = getelementptr inbounds i16* %4, i32 %3, !dbg !52
  %5 = load i16* %arrayidx, !dbg !52
  store i16 %5, i16* %value, align 2, !dbg !52
  %conv3 = sext i16 %5 to i32, !dbg !52
  %cmp4 = icmp eq i32 %conv3, 0, !dbg !52
  br i1 %cmp4, label %if.then, label %if.else, !dbg !52

if.then:                                          ; preds = %while.body
  store i32 0, i32* %count, align 4, !dbg !54
  br label %for.cond, !dbg !54

for.cond:                                         ; preds = %for.inc, %if.then
  %6 = load i32* %block_pos, align 4, !dbg !54
  %7 = load i16** %block.addr, align 4, !dbg !54
  %arrayidx6 = getelementptr inbounds i16* %7, i32 %6, !dbg !54
  %8 = load i16* %arrayidx6, !dbg !54
  %conv7 = sext i16 %8 to i32, !dbg !54
  %cmp8 = icmp eq i32 %conv7, 0, !dbg !54
  br i1 %cmp8, label %for.body, label %for.end, !dbg !54

for.body:                                         ; preds = %for.cond
  %9 = load i32* %block_pos, align 4, !dbg !57
  %inc = add i32 %9, 1, !dbg !57
  store i32 %inc, i32* %block_pos, align 4, !dbg !57
  br label %for.inc, !dbg !57

for.inc:                                          ; preds = %for.body
  %10 = load i32* %count, align 4, !dbg !58
  %inc10 = add i32 %10, 1, !dbg !58
  store i32 %inc10, i32* %count, align 4, !dbg !58
  br label %for.cond, !dbg !58

for.end:                                          ; preds = %for.cond
  store i32 1, i32* %mask, align 4, !dbg !59
  store i16 0, i16* %bit_position, align 2, !dbg !59
  br label %for.cond11, !dbg !59

for.cond11:                                       ; preds = %for.inc28, %for.end
  %11 = load i16* %bit_position, align 2, !dbg !59
  %conv12 = zext i16 %11 to i32, !dbg !59
  %cmp13 = icmp slt i32 %conv12, 32, !dbg !59
  br i1 %cmp13, label %for.body15, label %for.end30, !dbg !59

for.body15:                                       ; preds = %for.cond11
  %12 = load i32* %mask, align 4, !dbg !61
  %13 = load i32* %count, align 4, !dbg !61
  %and = and i32 %12, %13, !dbg !61
  %tobool = icmp ne i32 %and, 0, !dbg !61
  br i1 %tobool, label %if.then16, label %if.end27, !dbg !61

if.then16:                                        ; preds = %for.body15
  %14 = load i16* %bit_position, align 2, !dbg !63
  %conv17 = zext i16 %14 to i32, !dbg !63
  %15 = load i16* %sign_mask, align 2, !dbg !63
  %conv18 = zext i16 %15 to i32, !dbg !63
  %cmp19 = icmp sge i32 %conv17, %conv18, !dbg !63
  br i1 %cmp19, label %if.then21, label %if.end, !dbg !63

if.then21:                                        ; preds = %if.then16
  %call = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([56 x i8]* @.str, i32 0, i32 0)), !dbg !65
  br label %if.end, !dbg !65

if.end:                                           ; preds = %if.then21, %if.then16
  %16 = load i16* %bit_position, align 2, !dbg !66
  %conv22 = zext i16 %16 to i32, !dbg !66
  %17 = load i16* %zero_run_mask, align 2, !dbg !66
  %conv23 = zext i16 %17 to i32, !dbg !66
  %or = or i32 %conv22, %conv23, !dbg !66
  %conv24 = trunc i32 %or to i16, !dbg !66
  %18 = load i32* %stream_pos, align 4, !dbg !66
  %19 = load i16** %encoded_stream.addr, align 4, !dbg !66
  %arrayidx25 = getelementptr inbounds i16* %19, i32 %18, !dbg !66
  store i16 %conv24, i16* %arrayidx25, !dbg !66
  %20 = load i32* %stream_pos, align 4, !dbg !67
  %inc26 = add i32 %20, 1, !dbg !67
  store i32 %inc26, i32* %stream_pos, align 4, !dbg !67
  br label %if.end27, !dbg !68

if.end27:                                         ; preds = %if.end, %for.body15
  br label %for.inc28, !dbg !69

for.inc28:                                        ; preds = %if.end27
  %21 = load i16* %bit_position, align 2, !dbg !70
  %inc29 = add i16 %21, 1, !dbg !70
  store i16 %inc29, i16* %bit_position, align 2, !dbg !70
  %22 = load i32* %mask, align 4, !dbg !70
  %shl = shl i32 %22, 1, !dbg !70
  store i32 %shl, i32* %mask, align 4, !dbg !70
  br label %for.cond11, !dbg !70

for.end30:                                        ; preds = %for.cond11
  br label %if.end66, !dbg !71

if.else:                                          ; preds = %while.body
  %23 = load i16* %value, align 2, !dbg !72
  %conv31 = sext i16 %23 to i32, !dbg !72
  %cmp32 = icmp slt i32 %conv31, 0, !dbg !72
  br i1 %cmp32, label %cond.true, label %cond.false, !dbg !72

cond.true:                                        ; preds = %if.else
  %24 = load i16* %value, align 2, !dbg !72
  %conv34 = sext i16 %24 to i32, !dbg !72
  %sub35 = sub nsw i32 0, %conv34, !dbg !72
  br label %cond.end, !dbg !72

cond.false:                                       ; preds = %if.else
  %25 = load i16* %value, align 2, !dbg !72
  %conv36 = sext i16 %25 to i32, !dbg !72
  br label %cond.end, !dbg !72

cond.end:                                         ; preds = %cond.false, %cond.true
  %cond = phi i32 [ %sub35, %cond.true ], [ %conv36, %cond.false ], !dbg !72
  %26 = load i16* %value_bits_mask, align 2, !dbg !72
  %conv37 = zext i16 %26 to i32, !dbg !72
  %cmp38 = icmp sgt i32 %cond, %conv37, !dbg !72
  br i1 %cmp38, label %if.then40, label %if.end42, !dbg !72

if.then40:                                        ; preds = %cond.end
  %call41 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([50 x i8]* @.str1, i32 0, i32 0)), !dbg !74
  br label %if.end42, !dbg !74

if.end42:                                         ; preds = %if.then40, %cond.end
  %27 = load i16* %value, align 2, !dbg !75
  %conv43 = sext i16 %27 to i32, !dbg !75
  %cmp44 = icmp slt i32 %conv43, 0, !dbg !75
  br i1 %cmp44, label %if.then46, label %if.else57, !dbg !75

if.then46:                                        ; preds = %if.end42
  %28 = load i16* %value, align 2, !dbg !76
  %conv47 = sext i16 %28 to i32, !dbg !76
  %sub48 = sub nsw i32 0, %conv47, !dbg !76
  %conv49 = trunc i32 %sub48 to i16, !dbg !76
  %conv50 = zext i16 %conv49 to i32, !dbg !76
  %29 = load i16* %value_bits_mask, align 2, !dbg !76
  %conv51 = zext i16 %29 to i32, !dbg !76
  %and52 = and i32 %conv50, %conv51, !dbg !76
  %30 = load i16* %sign_mask, align 2, !dbg !76
  %conv53 = zext i16 %30 to i32, !dbg !76
  %or54 = or i32 %and52, %conv53, !dbg !76
  %conv55 = trunc i32 %or54 to i16, !dbg !76
  %31 = load i32* %stream_pos, align 4, !dbg !76
  %32 = load i16** %encoded_stream.addr, align 4, !dbg !76
  %arrayidx56 = getelementptr inbounds i16* %32, i32 %31, !dbg !76
  store i16 %conv55, i16* %arrayidx56, !dbg !76
  br label %if.end63, !dbg !76

if.else57:                                        ; preds = %if.end42
  %33 = load i16* %value, align 2, !dbg !77
  %conv58 = zext i16 %33 to i32, !dbg !77
  %34 = load i16* %value_bits_mask, align 2, !dbg !77
  %conv59 = zext i16 %34 to i32, !dbg !77
  %and60 = and i32 %conv58, %conv59, !dbg !77
  %conv61 = trunc i32 %and60 to i16, !dbg !77
  %35 = load i32* %stream_pos, align 4, !dbg !77
  %36 = load i16** %encoded_stream.addr, align 4, !dbg !77
  %arrayidx62 = getelementptr inbounds i16* %36, i32 %35, !dbg !77
  store i16 %conv61, i16* %arrayidx62, !dbg !77
  br label %if.end63

if.end63:                                         ; preds = %if.else57, %if.then46
  %37 = load i32* %stream_pos, align 4, !dbg !78
  %inc64 = add i32 %37, 1, !dbg !78
  store i32 %inc64, i32* %stream_pos, align 4, !dbg !78
  %38 = load i32* %block_pos, align 4, !dbg !79
  %inc65 = add i32 %38, 1, !dbg !79
  store i32 %inc65, i32* %block_pos, align 4, !dbg !79
  br label %if.end66

if.end66:                                         ; preds = %if.end63, %for.end30
  br label %while.cond, !dbg !80

while.end:                                        ; preds = %while.cond
  %39 = load i32* %stream_pos, align 4, !dbg !81
  ret i32 %39, !dbg !81
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc i32 @printf(i8*, ...)

define arm_aapcscc i32 @run_length_decode_zeros(i16* %symbol_stream, i32 %block_size, i16* %block) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %symbol_stream.addr = alloca i16*, align 4
  %block_size.addr = alloca i32, align 4
  %block.addr = alloca i16*, align 4
  %num_zeros = alloca i32, align 4
  %count = alloca i32, align 4
  %block_pos = alloca i32, align 4
  %stream_pos = alloca i32, align 4
  %the_symbol = alloca i16, align 2
  %sign_mask = alloca i16, align 2
  %value_bits_mask = alloca i16, align 2
  %zero_run_mask = alloca i16, align 2
  store i16* %symbol_stream, i16** %symbol_stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %symbol_stream.addr}, metadata !82), !dbg !83
  store i32 %block_size, i32* %block_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %block_size.addr}, metadata !84), !dbg !85
  store i16* %block, i16** %block.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %block.addr}, metadata !86), !dbg !87
  call void @llvm.dbg.declare(metadata !{i32* %num_zeros}, metadata !88), !dbg !91
  call void @llvm.dbg.declare(metadata !{i32* %count}, metadata !92), !dbg !93
  call void @llvm.dbg.declare(metadata !{i32* %block_pos}, metadata !94), !dbg !95
  store i32 0, i32* %block_pos, align 4, !dbg !96
  call void @llvm.dbg.declare(metadata !{i32* %stream_pos}, metadata !97), !dbg !98
  store i32 0, i32* %stream_pos, align 4, !dbg !96
  call void @llvm.dbg.declare(metadata !{i16* %the_symbol}, metadata !99), !dbg !100
  call void @llvm.dbg.declare(metadata !{i16* %sign_mask}, metadata !101), !dbg !102
  store i16 16384, i16* %sign_mask, align 2, !dbg !103
  call void @llvm.dbg.declare(metadata !{i16* %value_bits_mask}, metadata !104), !dbg !105
  %0 = load i16* %sign_mask, align 2, !dbg !106
  %conv = zext i16 %0 to i32, !dbg !106
  %sub = sub nsw i32 %conv, 1, !dbg !106
  %conv1 = trunc i32 %sub to i16, !dbg !106
  store i16 %conv1, i16* %value_bits_mask, align 2, !dbg !106
  call void @llvm.dbg.declare(metadata !{i16* %zero_run_mask}, metadata !107), !dbg !108
  store i16 -32768, i16* %zero_run_mask, align 2, !dbg !109
  br label %while.cond, !dbg !110

while.cond:                                       ; preds = %if.end30, %entry
  %1 = load i32* %block_pos, align 4, !dbg !110
  %2 = load i32* %block_size.addr, align 4, !dbg !110
  %cmp = icmp ult i32 %1, %2, !dbg !110
  br i1 %cmp, label %while.body, label %while.end, !dbg !110

while.body:                                       ; preds = %while.cond
  %3 = load i32* %stream_pos, align 4, !dbg !111
  %4 = load i16** %symbol_stream.addr, align 4, !dbg !111
  %arrayidx = getelementptr inbounds i16* %4, i32 %3, !dbg !111
  %5 = load i16* %arrayidx, !dbg !111
  store i16 %5, i16* %the_symbol, align 2, !dbg !111
  %6 = load i16* %the_symbol, align 2, !dbg !113
  %conv3 = zext i16 %6 to i32, !dbg !113
  %7 = load i16* %zero_run_mask, align 2, !dbg !113
  %conv4 = zext i16 %7 to i32, !dbg !113
  %and = and i32 %conv3, %conv4, !dbg !113
  %tobool = icmp ne i32 %and, 0, !dbg !113
  br i1 %tobool, label %if.then, label %if.else, !dbg !113

if.then:                                          ; preds = %while.body
  %8 = load i16* %the_symbol, align 2, !dbg !114
  %conv5 = zext i16 %8 to i32, !dbg !114
  %9 = load i16* %value_bits_mask, align 2, !dbg !114
  %conv6 = zext i16 %9 to i32, !dbg !114
  %and7 = and i32 %conv5, %conv6, !dbg !114
  %shl = shl i32 1, %and7, !dbg !114
  store i32 %shl, i32* %num_zeros, align 4, !dbg !114
  store i32 0, i32* %count, align 4, !dbg !116
  br label %for.cond, !dbg !116

for.cond:                                         ; preds = %for.inc, %if.then
  %10 = load i32* %count, align 4, !dbg !116
  %11 = load i32* %num_zeros, align 4, !dbg !116
  %cmp8 = icmp ult i32 %10, %11, !dbg !116
  br i1 %cmp8, label %for.body, label %for.end, !dbg !116

for.body:                                         ; preds = %for.cond
  %12 = load i32* %block_pos, align 4, !dbg !118
  %13 = load i16** %block.addr, align 4, !dbg !118
  %arrayidx10 = getelementptr inbounds i16* %13, i32 %12, !dbg !118
  store i16 0, i16* %arrayidx10, !dbg !118
  br label %for.inc, !dbg !118

for.inc:                                          ; preds = %for.body
  %14 = load i32* %count, align 4, !dbg !119
  %inc = add i32 %14, 1, !dbg !119
  store i32 %inc, i32* %count, align 4, !dbg !119
  %15 = load i32* %block_pos, align 4, !dbg !119
  %inc11 = add i32 %15, 1, !dbg !119
  store i32 %inc11, i32* %block_pos, align 4, !dbg !119
  br label %for.cond, !dbg !119

for.end:                                          ; preds = %for.cond
  br label %if.end30, !dbg !120

if.else:                                          ; preds = %while.body
  %16 = load i16* %the_symbol, align 2, !dbg !121
  %conv12 = zext i16 %16 to i32, !dbg !121
  %17 = load i16* %sign_mask, align 2, !dbg !121
  %conv13 = zext i16 %17 to i32, !dbg !121
  %and14 = and i32 %conv12, %conv13, !dbg !121
  %tobool15 = icmp ne i32 %and14, 0, !dbg !121
  br i1 %tobool15, label %if.then16, label %if.else23, !dbg !121

if.then16:                                        ; preds = %if.else
  %18 = load i16* %the_symbol, align 2, !dbg !123
  %conv17 = zext i16 %18 to i32, !dbg !123
  %19 = load i16* %value_bits_mask, align 2, !dbg !123
  %conv18 = zext i16 %19 to i32, !dbg !123
  %and19 = and i32 %conv17, %conv18, !dbg !123
  %sub20 = sub nsw i32 0, %and19, !dbg !123
  %conv21 = trunc i32 %sub20 to i16, !dbg !123
  %20 = load i32* %block_pos, align 4, !dbg !123
  %21 = load i16** %block.addr, align 4, !dbg !123
  %arrayidx22 = getelementptr inbounds i16* %21, i32 %20, !dbg !123
  store i16 %conv21, i16* %arrayidx22, !dbg !123
  br label %if.end, !dbg !123

if.else23:                                        ; preds = %if.else
  %22 = load i16* %the_symbol, align 2, !dbg !124
  %conv24 = zext i16 %22 to i32, !dbg !124
  %23 = load i16* %value_bits_mask, align 2, !dbg !124
  %conv25 = zext i16 %23 to i32, !dbg !124
  %and26 = and i32 %conv24, %conv25, !dbg !124
  %conv27 = trunc i32 %and26 to i16, !dbg !124
  %24 = load i32* %block_pos, align 4, !dbg !124
  %25 = load i16** %block.addr, align 4, !dbg !124
  %arrayidx28 = getelementptr inbounds i16* %25, i32 %24, !dbg !124
  store i16 %conv27, i16* %arrayidx28, !dbg !124
  br label %if.end

if.end:                                           ; preds = %if.else23, %if.then16
  %26 = load i32* %block_pos, align 4, !dbg !125
  %inc29 = add i32 %26, 1, !dbg !125
  store i32 %inc29, i32* %block_pos, align 4, !dbg !125
  br label %if.end30

if.end30:                                         ; preds = %if.end, %for.end
  %27 = load i32* %stream_pos, align 4, !dbg !126
  %inc31 = add i32 %27, 1, !dbg !126
  store i32 %inc31, i32* %stream_pos, align 4, !dbg !126
  br label %while.cond, !dbg !127

while.end:                                        ; preds = %while.cond
  %28 = load i32* %retval, !dbg !128
  ret i32 %28, !dbg !128
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !12}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"run_length_encode_zeros", metadata !"run_length_encode_zeros", metadata !"", metadata !6, i32 51, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i16*, i32, i16*)* @run_length_encode_zeros, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"run_length.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{i32 720942, i32 0, metadata !6, metadata !"run_length_decode_zeros", metadata !"run_length_decode_zeros", metadata !"", metadata !6, i32 98, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i16*, i32, i16*)* @run_length_decode_zeros, null, null, metadata !13} ; [ DW_TAG_subprogram ]
!13 = metadata !{metadata !14}
!14 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!15 = metadata !{i32 721153, metadata !5, metadata !"block", metadata !6, i32 16777264, metadata !16, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!16 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !17} ; [ DW_TAG_pointer_type ]
!17 = metadata !{i32 720918, null, metadata !"BinIndexType", metadata !6, i32 59, i64 0, i64 0, i64 0, i32 0, metadata !18} ; [ DW_TAG_typedef ]
!18 = metadata !{i32 720932, null, metadata !"short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!19 = metadata !{i32 48, i32 26, metadata !5, null}
!20 = metadata !{i32 721153, metadata !5, metadata !"block_size", metadata !6, i32 33554481, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!21 = metadata !{i32 49, i32 7, metadata !5, null}
!22 = metadata !{i32 721153, metadata !5, metadata !"encoded_stream", metadata !6, i32 50331698, metadata !23, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!23 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !24} ; [ DW_TAG_pointer_type ]
!24 = metadata !{i32 720918, null, metadata !"SymbolType", metadata !6, i32 74, i64 0, i64 0, i64 0, i32 0, metadata !25} ; [ DW_TAG_typedef ]
!25 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!26 = metadata !{i32 50, i32 24, metadata !5, null}
!27 = metadata !{i32 721152, metadata !28, metadata !"value", metadata !6, i32 52, metadata !17, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!28 = metadata !{i32 720907, metadata !5, i32 51, i32 3, metadata !6, i32 65} ; [ DW_TAG_lexical_block ]
!29 = metadata !{i32 52, i32 25, metadata !28, null}
!30 = metadata !{i32 721152, metadata !28, metadata !"block_pos", metadata !6, i32 53, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!31 = metadata !{i32 720932, null, metadata !"long unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!32 = metadata !{i32 53, i32 26, metadata !28, null}
!33 = metadata !{i32 53, i32 68, metadata !28, null}
!34 = metadata !{i32 721152, metadata !28, metadata !"stream_pos", metadata !6, i32 53, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!35 = metadata !{i32 53, i32 41, metadata !28, null}
!36 = metadata !{i32 721152, metadata !28, metadata !"count", metadata !6, i32 53, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!37 = metadata !{i32 53, i32 57, metadata !28, null}
!38 = metadata !{i32 721152, metadata !28, metadata !"mask", metadata !6, i32 53, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!39 = metadata !{i32 53, i32 64, metadata !28, null}
!40 = metadata !{i32 721152, metadata !28, metadata !"sign_mask", metadata !6, i32 54, metadata !24, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!41 = metadata !{i32 54, i32 23, metadata !28, null}
!42 = metadata !{i32 54, i32 64, metadata !28, null}
!43 = metadata !{i32 721152, metadata !28, metadata !"value_bits_mask", metadata !6, i32 55, metadata !24, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!44 = metadata !{i32 55, i32 23, metadata !28, null}
!45 = metadata !{i32 55, i32 54, metadata !28, null}
!46 = metadata !{i32 721152, metadata !28, metadata !"bit_position", metadata !6, i32 56, metadata !24, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!47 = metadata !{i32 56, i32 23, metadata !28, null}
!48 = metadata !{i32 721152, metadata !28, metadata !"zero_run_mask", metadata !6, i32 57, metadata !24, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!49 = metadata !{i32 57, i32 23, metadata !28, null}
!50 = metadata !{i32 57, i32 68, metadata !28, null}
!51 = metadata !{i32 59, i32 3, metadata !28, null}
!52 = metadata !{i32 61, i32 7, metadata !53, null}
!53 = metadata !{i32 720907, metadata !28, i32 60, i32 7, metadata !6, i32 66} ; [ DW_TAG_lexical_block ]
!54 = metadata !{i32 63, i32 9, metadata !55, null}
!55 = metadata !{i32 720907, metadata !56, i32 63, i32 4, metadata !6, i32 68} ; [ DW_TAG_lexical_block ]
!56 = metadata !{i32 720907, metadata !53, i32 62, i32 4, metadata !6, i32 67} ; [ DW_TAG_lexical_block ]
!57 = metadata !{i32 63, i32 48, metadata !55, null}
!58 = metadata !{i32 63, i32 39, metadata !55, null}
!59 = metadata !{i32 64, i32 9, metadata !60, null}
!60 = metadata !{i32 720907, metadata !56, i32 64, i32 4, metadata !6, i32 69} ; [ DW_TAG_lexical_block ]
!61 = metadata !{i32 68, i32 8, metadata !62, null}
!62 = metadata !{i32 720907, metadata !60, i32 67, i32 8, metadata !6, i32 70} ; [ DW_TAG_lexical_block ]
!63 = metadata !{i32 70, i32 5, metadata !64, null}
!64 = metadata !{i32 720907, metadata !62, i32 69, i32 5, metadata !6, i32 71} ; [ DW_TAG_lexical_block ]
!65 = metadata !{i32 71, i32 7, metadata !64, null}
!66 = metadata !{i32 72, i32 5, metadata !64, null}
!67 = metadata !{i32 73, i32 5, metadata !64, null}
!68 = metadata !{i32 74, i32 5, metadata !64, null}
!69 = metadata !{i32 75, i32 8, metadata !62, null}
!70 = metadata !{i32 66, i32 9, metadata !60, null}
!71 = metadata !{i32 76, i32 4, metadata !56, null}
!72 = metadata !{i32 79, i32 4, metadata !73, null}
!73 = metadata !{i32 720907, metadata !53, i32 78, i32 4, metadata !6, i32 72} ; [ DW_TAG_lexical_block ]
!74 = metadata !{i32 80, i32 6, metadata !73, null}
!75 = metadata !{i32 81, i32 4, metadata !73, null}
!76 = metadata !{i32 82, i32 6, metadata !73, null}
!77 = metadata !{i32 85, i32 6, metadata !73, null}
!78 = metadata !{i32 87, i32 4, metadata !73, null}
!79 = metadata !{i32 88, i32 4, metadata !73, null}
!80 = metadata !{i32 90, i32 7, metadata !53, null}
!81 = metadata !{i32 91, i32 3, metadata !28, null}
!82 = metadata !{i32 721153, metadata !12, metadata !"symbol_stream", metadata !6, i32 16777313, metadata !23, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!83 = metadata !{i32 97, i32 15, metadata !12, null}
!84 = metadata !{i32 721153, metadata !12, metadata !"block_size", metadata !6, i32 33554528, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!85 = metadata !{i32 96, i32 7, metadata !12, null}
!86 = metadata !{i32 721153, metadata !12, metadata !"block", metadata !6, i32 50331743, metadata !16, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!87 = metadata !{i32 95, i32 17, metadata !12, null}
!88 = metadata !{i32 721152, metadata !89, metadata !"num_zeros", metadata !6, i32 99, metadata !90, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!89 = metadata !{i32 720907, metadata !12, i32 98, i32 3, metadata !6, i32 73} ; [ DW_TAG_lexical_block ]
!90 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!91 = metadata !{i32 99, i32 21, metadata !89, null}
!92 = metadata !{i32 721152, metadata !89, metadata !"count", metadata !6, i32 99, metadata !90, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!93 = metadata !{i32 99, i32 32, metadata !89, null}
!94 = metadata !{i32 721152, metadata !89, metadata !"block_pos", metadata !6, i32 99, metadata !90, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!95 = metadata !{i32 99, i32 39, metadata !89, null}
!96 = metadata !{i32 99, i32 68, metadata !89, null}
!97 = metadata !{i32 721152, metadata !89, metadata !"stream_pos", metadata !6, i32 99, metadata !90, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!98 = metadata !{i32 99, i32 54, metadata !89, null}
!99 = metadata !{i32 721152, metadata !89, metadata !"the_symbol", metadata !6, i32 100, metadata !24, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!100 = metadata !{i32 100, i32 23, metadata !89, null}
!101 = metadata !{i32 721152, metadata !89, metadata !"sign_mask", metadata !6, i32 101, metadata !24, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!102 = metadata !{i32 101, i32 23, metadata !89, null}
!103 = metadata !{i32 101, i32 64, metadata !89, null}
!104 = metadata !{i32 721152, metadata !89, metadata !"value_bits_mask", metadata !6, i32 102, metadata !24, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!105 = metadata !{i32 102, i32 23, metadata !89, null}
!106 = metadata !{i32 102, i32 54, metadata !89, null}
!107 = metadata !{i32 721152, metadata !89, metadata !"zero_run_mask", metadata !6, i32 103, metadata !24, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!108 = metadata !{i32 103, i32 23, metadata !89, null}
!109 = metadata !{i32 103, i32 68, metadata !89, null}
!110 = metadata !{i32 105, i32 3, metadata !89, null}
!111 = metadata !{i32 107, i32 7, metadata !112, null}
!112 = metadata !{i32 720907, metadata !89, i32 106, i32 7, metadata !6, i32 74} ; [ DW_TAG_lexical_block ]
!113 = metadata !{i32 108, i32 7, metadata !112, null}
!114 = metadata !{i32 110, i32 4, metadata !115, null}
!115 = metadata !{i32 720907, metadata !112, i32 109, i32 4, metadata !6, i32 75} ; [ DW_TAG_lexical_block ]
!116 = metadata !{i32 111, i32 10, metadata !117, null}
!117 = metadata !{i32 720907, metadata !115, i32 111, i32 5, metadata !6, i32 76} ; [ DW_TAG_lexical_block ]
!118 = metadata !{i32 112, i32 8, metadata !117, null}
!119 = metadata !{i32 111, i32 40, metadata !117, null}
!120 = metadata !{i32 113, i32 4, metadata !115, null}
!121 = metadata !{i32 116, i32 4, metadata !122, null}
!122 = metadata !{i32 720907, metadata !112, i32 115, i32 4, metadata !6, i32 77} ; [ DW_TAG_lexical_block ]
!123 = metadata !{i32 117, i32 6, metadata !122, null}
!124 = metadata !{i32 119, i32 6, metadata !122, null}
!125 = metadata !{i32 120, i32 4, metadata !122, null}
!126 = metadata !{i32 122, i32 7, metadata !112, null}
!127 = metadata !{i32 123, i32 7, metadata !112, null}
!128 = metadata !{i32 124, i32 3, metadata !89, null}
