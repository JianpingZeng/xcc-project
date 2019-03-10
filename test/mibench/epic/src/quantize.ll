; ModuleID = 'quantize.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

@.str = private unnamed_addr constant [52 x i8] c"QUANTIZE: bin_size %d is too small. Changed to %d.\0A\00", align 1

define arm_aapcscc i32 @quantize_pyr(float* %float_pyr, i16* %q_pyr, i32 %im_size, i32 %num_levels, double %compression_factor, i16* %bin_size) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %float_pyr.addr = alloca float*, align 4
  %q_pyr.addr = alloca i16*, align 4
  %im_size.addr = alloca i32, align 4
  %num_levels.addr = alloca i32, align 4
  %compression_factor.addr = alloca double, align 8
  %bin_size.addr = alloca i16*, align 4
  %level = alloca i32, align 4
  %bin_index = alloca i32, align 4
  %im_num = alloca i32, align 4
  %pyr_offset = alloca i32, align 4
  %the_bin_size = alloca double, align 8
  %max_bin_size = alloca i16, align 2
  store float* %float_pyr, float** %float_pyr.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %float_pyr.addr}, metadata !21), !dbg !24
  store i16* %q_pyr, i16** %q_pyr.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q_pyr.addr}, metadata !25), !dbg !29
  store i32 %im_size, i32* %im_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %im_size.addr}, metadata !30), !dbg !31
  store i32 %num_levels, i32* %num_levels.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %num_levels.addr}, metadata !32), !dbg !33
  store double %compression_factor, double* %compression_factor.addr, align 8
  call void @llvm.dbg.declare(metadata !{double* %compression_factor.addr}, metadata !34), !dbg !36
  store i16* %bin_size, i16** %bin_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %bin_size.addr}, metadata !37), !dbg !41
  call void @llvm.dbg.declare(metadata !{i32* %level}, metadata !42), !dbg !44
  call void @llvm.dbg.declare(metadata !{i32* %bin_index}, metadata !45), !dbg !46
  call void @llvm.dbg.declare(metadata !{i32* %im_num}, metadata !47), !dbg !48
  call void @llvm.dbg.declare(metadata !{i32* %pyr_offset}, metadata !49), !dbg !50
  call void @llvm.dbg.declare(metadata !{double* %the_bin_size}, metadata !51), !dbg !52
  %0 = load double* %compression_factor.addr, align 8, !dbg !53
  store double %0, double* %the_bin_size, align 8, !dbg !53
  call void @llvm.dbg.declare(metadata !{i16* %max_bin_size}, metadata !54), !dbg !55
  store i16 0, i16* %max_bin_size, align 2, !dbg !56
  %1 = load i16* %max_bin_size, align 2, !dbg !57
  %conv = zext i16 %1 to i32, !dbg !57
  %neg = xor i32 %conv, -1, !dbg !57
  %conv1 = trunc i32 %neg to i16, !dbg !57
  store i16 %conv1, i16* %max_bin_size, align 2, !dbg !57
  %2 = load i32* %im_size.addr, align 4, !dbg !58
  %div = sdiv i32 %2, 4, !dbg !58
  store i32 %div, i32* %im_size.addr, align 4, !dbg !58
  store i32 1, i32* %level, align 4, !dbg !58
  %3 = load i32* %num_levels.addr, align 4, !dbg !58
  %mul = mul nsw i32 3, %3, !dbg !58
  store i32 %mul, i32* %bin_index, align 4, !dbg !58
  br label %for.cond, !dbg !58

for.cond:                                         ; preds = %for.inc25, %entry
  %4 = load i32* %level, align 4, !dbg !58
  %5 = load i32* %num_levels.addr, align 4, !dbg !58
  %cmp = icmp sle i32 %4, %5, !dbg !58
  br i1 %cmp, label %for.body, label %for.end28, !dbg !58

for.body:                                         ; preds = %for.cond
  store i32 3, i32* %im_num, align 4, !dbg !60
  %6 = load i32* %im_size.addr, align 4, !dbg !60
  %mul3 = mul nsw i32 3, %6, !dbg !60
  store i32 %mul3, i32* %pyr_offset, align 4, !dbg !60
  br label %for.cond4, !dbg !60

for.cond4:                                        ; preds = %for.inc, %for.body
  %7 = load i32* %im_num, align 4, !dbg !60
  %8 = load i32* %level, align 4, !dbg !60
  %9 = load i32* %num_levels.addr, align 4, !dbg !60
  %cmp5 = icmp eq i32 %8, %9, !dbg !60
  %cond = select i1 %cmp5, i32 0, i32 1, !dbg !60
  %cmp7 = icmp sge i32 %7, %cond, !dbg !60
  br i1 %cmp7, label %for.body9, label %for.end, !dbg !60

for.body9:                                        ; preds = %for.cond4
  %10 = load double* %the_bin_size, align 8, !dbg !62
  %cmp10 = fcmp olt double %10, 0.000000e+00, !dbg !62
  br i1 %cmp10, label %if.then, label %if.else, !dbg !62

if.then:                                          ; preds = %for.body9
  %11 = load i32* %bin_index, align 4, !dbg !64
  %12 = load i16** %bin_size.addr, align 4, !dbg !64
  %arrayidx = getelementptr inbounds i16* %12, i32 %11, !dbg !64
  store i16 0, i16* %arrayidx, !dbg !64
  br label %if.end21, !dbg !64

if.else:                                          ; preds = %for.body9
  %13 = load double* %the_bin_size, align 8, !dbg !65
  %14 = load i16* %max_bin_size, align 2, !dbg !65
  %conv12 = zext i16 %14 to i32, !dbg !65
  %conv13 = sitofp i32 %conv12 to double, !dbg !65
  %cmp14 = fcmp ogt double %13, %conv13, !dbg !65
  br i1 %cmp14, label %if.then16, label %if.else18, !dbg !65

if.then16:                                        ; preds = %if.else
  %15 = load i16* %max_bin_size, align 2, !dbg !66
  %16 = load i32* %bin_index, align 4, !dbg !66
  %17 = load i16** %bin_size.addr, align 4, !dbg !66
  %arrayidx17 = getelementptr inbounds i16* %17, i32 %16, !dbg !66
  store i16 %15, i16* %arrayidx17, !dbg !66
  br label %if.end, !dbg !66

if.else18:                                        ; preds = %if.else
  %18 = load double* %the_bin_size, align 8, !dbg !67
  %add = fadd double %18, 5.000000e-01, !dbg !67
  %conv19 = fptoui double %add to i16, !dbg !67
  %19 = load i32* %bin_index, align 4, !dbg !67
  %20 = load i16** %bin_size.addr, align 4, !dbg !67
  %arrayidx20 = getelementptr inbounds i16* %20, i32 %19, !dbg !67
  store i16 %conv19, i16* %arrayidx20, !dbg !67
  br label %if.end

if.end:                                           ; preds = %if.else18, %if.then16
  br label %if.end21

if.end21:                                         ; preds = %if.end, %if.then
  %21 = load float** %float_pyr.addr, align 4, !dbg !68
  %22 = load i32* %pyr_offset, align 4, !dbg !68
  %add.ptr = getelementptr inbounds float* %21, i32 %22, !dbg !68
  %23 = load i16** %q_pyr.addr, align 4, !dbg !68
  %24 = load i32* %pyr_offset, align 4, !dbg !68
  %add.ptr22 = getelementptr inbounds i16* %23, i32 %24, !dbg !68
  %25 = load i32* %im_size.addr, align 4, !dbg !68
  %26 = load i16** %bin_size.addr, align 4, !dbg !68
  %27 = load i32* %bin_index, align 4, !dbg !68
  %add.ptr23 = getelementptr inbounds i16* %26, i32 %27, !dbg !68
  %call = call arm_aapcscc  i32 @quantize_image(float* %add.ptr, i16* %add.ptr22, i32 %25, i16* %add.ptr23), !dbg !68
  br label %for.inc, !dbg !69

for.inc:                                          ; preds = %if.end21
  %28 = load i32* %im_num, align 4, !dbg !70
  %dec = add nsw i32 %28, -1, !dbg !70
  store i32 %dec, i32* %im_num, align 4, !dbg !70
  %29 = load i32* %im_size.addr, align 4, !dbg !70
  %30 = load i32* %pyr_offset, align 4, !dbg !70
  %sub = sub nsw i32 %30, %29, !dbg !70
  store i32 %sub, i32* %pyr_offset, align 4, !dbg !70
  %31 = load i32* %bin_index, align 4, !dbg !70
  %dec24 = add nsw i32 %31, -1, !dbg !70
  store i32 %dec24, i32* %bin_index, align 4, !dbg !70
  br label %for.cond4, !dbg !70

for.end:                                          ; preds = %for.cond4
  br label %for.inc25, !dbg !71

for.inc25:                                        ; preds = %for.end
  %32 = load i32* %level, align 4, !dbg !72
  %inc = add nsw i32 %32, 1, !dbg !72
  store i32 %inc, i32* %level, align 4, !dbg !72
  %33 = load i32* %im_size.addr, align 4, !dbg !72
  %div26 = sdiv i32 %33, 4, !dbg !72
  store i32 %div26, i32* %im_size.addr, align 4, !dbg !72
  %34 = load double* %the_bin_size, align 8, !dbg !72
  %div27 = fdiv double %34, 2.000000e+00, !dbg !72
  store double %div27, double* %the_bin_size, align 8, !dbg !72
  br label %for.cond, !dbg !72

for.end28:                                        ; preds = %for.cond
  %35 = load i32* %retval, !dbg !73
  ret i32 %35, !dbg !73
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

define arm_aapcscc i32 @quantize_image(float* %float_im, i16* %q_im, i32 %im_size, i16* %bin_size) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %float_im.addr = alloca float*, align 4
  %q_im.addr = alloca i16*, align 4
  %im_size.addr = alloca i32, align 4
  %bin_size.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  %the_bin_size = alloca double, align 8
  %ftemp = alloca double, align 8
  %max_abs = alloca double, align 8
  store float* %float_im, float** %float_im.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %float_im.addr}, metadata !74), !dbg !75
  store i16* %q_im, i16** %q_im.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q_im.addr}, metadata !76), !dbg !77
  store i32 %im_size, i32* %im_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %im_size.addr}, metadata !78), !dbg !79
  store i16* %bin_size, i16** %bin_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %bin_size.addr}, metadata !80), !dbg !81
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !82), !dbg !84
  call void @llvm.dbg.declare(metadata !{double* %the_bin_size}, metadata !85), !dbg !86
  call void @llvm.dbg.declare(metadata !{double* %ftemp}, metadata !87), !dbg !88
  call void @llvm.dbg.declare(metadata !{double* %max_abs}, metadata !89), !dbg !90
  store double 0.000000e+00, double* %max_abs, align 8, !dbg !91
  store i32 0, i32* %i, align 4, !dbg !92
  br label %for.cond, !dbg !92

for.cond:                                         ; preds = %for.inc, %entry
  %0 = load i32* %i, align 4, !dbg !92
  %1 = load i32* %im_size.addr, align 4, !dbg !92
  %cmp = icmp slt i32 %0, %1, !dbg !92
  br i1 %cmp, label %for.body, label %for.end, !dbg !92

for.body:                                         ; preds = %for.cond
  %2 = load i32* %i, align 4, !dbg !94
  %3 = load float** %float_im.addr, align 4, !dbg !94
  %arrayidx = getelementptr inbounds float* %3, i32 %2, !dbg !94
  %4 = load float* %arrayidx, !dbg !94
  %conv = fpext float %4 to double, !dbg !94
  store double %conv, double* %ftemp, align 8, !dbg !94
  %5 = load double* %ftemp, align 8, !dbg !96
  %cmp1 = fcmp olt double %5, 0.000000e+00, !dbg !96
  br i1 %cmp1, label %if.then, label %if.end, !dbg !96

if.then:                                          ; preds = %for.body
  %6 = load double* %ftemp, align 8, !dbg !97
  %sub = fsub double -0.000000e+00, %6, !dbg !97
  store double %sub, double* %ftemp, align 8, !dbg !97
  br label %if.end, !dbg !97

if.end:                                           ; preds = %if.then, %for.body
  %7 = load double* %ftemp, align 8, !dbg !98
  %8 = load double* %max_abs, align 8, !dbg !98
  %cmp3 = fcmp ogt double %7, %8, !dbg !98
  br i1 %cmp3, label %if.then5, label %if.end6, !dbg !98

if.then5:                                         ; preds = %if.end
  %9 = load double* %ftemp, align 8, !dbg !99
  store double %9, double* %max_abs, align 8, !dbg !99
  br label %if.end6, !dbg !99

if.end6:                                          ; preds = %if.then5, %if.end
  br label %for.inc, !dbg !100

for.inc:                                          ; preds = %if.end6
  %10 = load i32* %i, align 4, !dbg !101
  %inc = add nsw i32 %10, 1, !dbg !101
  store i32 %inc, i32* %i, align 4, !dbg !101
  br label %for.cond, !dbg !101

for.end:                                          ; preds = %for.cond
  %11 = load double* %max_abs, align 8, !dbg !102
  %12 = load i16** %bin_size.addr, align 4, !dbg !102
  %13 = load i16* %12, !dbg !102
  %conv7 = zext i16 %13 to i32, !dbg !102
  %mul = mul nsw i32 509, %conv7, !dbg !102
  %div = sdiv i32 %mul, 2, !dbg !102
  %conv8 = sitofp i32 %div to double, !dbg !102
  %cmp9 = fcmp oge double %11, %conv8, !dbg !102
  br i1 %cmp9, label %if.then11, label %if.end17, !dbg !102

if.then11:                                        ; preds = %for.end
  %14 = load i16** %bin_size.addr, align 4, !dbg !103
  %15 = load i16* %14, !dbg !103
  %conv12 = zext i16 %15 to i32, !dbg !103
  store i32 %conv12, i32* %i, align 4, !dbg !103
  %16 = load double* %max_abs, align 8, !dbg !105
  %mul13 = fmul double 2.000000e+00, %16, !dbg !105
  %div14 = fdiv double %mul13, 5.090000e+02, !dbg !105
  %add = fadd double %div14, 5.000000e-01, !dbg !105
  %conv15 = fptoui double %add to i16, !dbg !105
  %17 = load i16** %bin_size.addr, align 4, !dbg !105
  store i16 %conv15, i16* %17, !dbg !105
  %18 = load i32* %i, align 4, !dbg !106
  %19 = load i16** %bin_size.addr, align 4, !dbg !106
  %20 = load i16* %19, !dbg !106
  %conv16 = zext i16 %20 to i32, !dbg !106
  %call = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([52 x i8]* @.str, i32 0, i32 0), i32 %18, i32 %conv16), !dbg !106
  br label %if.end17, !dbg !107

if.end17:                                         ; preds = %if.then11, %for.end
  %21 = load i16** %bin_size.addr, align 4, !dbg !108
  %22 = load i16* %21, !dbg !108
  %conv18 = uitofp i16 %22 to double, !dbg !108
  store double %conv18, double* %the_bin_size, align 8, !dbg !108
  store i32 0, i32* %i, align 4, !dbg !109
  br label %for.cond19, !dbg !109

for.cond19:                                       ; preds = %for.inc32, %if.end17
  %23 = load i32* %i, align 4, !dbg !109
  %24 = load i32* %im_size.addr, align 4, !dbg !109
  %cmp20 = icmp slt i32 %23, %24, !dbg !109
  br i1 %cmp20, label %for.body22, label %for.end34, !dbg !109

for.body22:                                       ; preds = %for.cond19
  %25 = load i32* %i, align 4, !dbg !111
  %26 = load float** %float_im.addr, align 4, !dbg !111
  %arrayidx23 = getelementptr inbounds float* %26, i32 %25, !dbg !111
  %27 = load float* %arrayidx23, !dbg !111
  %conv24 = fpext float %27 to double, !dbg !111
  %28 = load double* %the_bin_size, align 8, !dbg !111
  %div25 = fdiv double %conv24, %28, !dbg !111
  store double %div25, double* %ftemp, align 8, !dbg !111
  %29 = load double* %ftemp, align 8, !dbg !113
  %cmp26 = fcmp olt double %29, 0.000000e+00, !dbg !113
  br i1 %cmp26, label %cond.true, label %cond.false, !dbg !113

cond.true:                                        ; preds = %for.body22
  %30 = load double* %ftemp, align 8, !dbg !113
  %sub28 = fsub double %30, 5.000000e-01, !dbg !113
  br label %cond.end, !dbg !113

cond.false:                                       ; preds = %for.body22
  %31 = load double* %ftemp, align 8, !dbg !113
  %add29 = fadd double %31, 5.000000e-01, !dbg !113
  br label %cond.end, !dbg !113

cond.end:                                         ; preds = %cond.false, %cond.true
  %cond = phi double [ %sub28, %cond.true ], [ %add29, %cond.false ], !dbg !113
  %conv30 = fptosi double %cond to i16, !dbg !113
  %32 = load i32* %i, align 4, !dbg !113
  %33 = load i16** %q_im.addr, align 4, !dbg !113
  %arrayidx31 = getelementptr inbounds i16* %33, i32 %32, !dbg !113
  store i16 %conv30, i16* %arrayidx31, !dbg !113
  br label %for.inc32, !dbg !114

for.inc32:                                        ; preds = %cond.end
  %34 = load i32* %i, align 4, !dbg !115
  %inc33 = add nsw i32 %34, 1, !dbg !115
  store i32 %inc33, i32* %i, align 4, !dbg !115
  br label %for.cond19, !dbg !115

for.end34:                                        ; preds = %for.cond19
  %35 = load i32* %retval, !dbg !116
  ret i32 %35, !dbg !116
}

declare arm_aapcscc i32 @printf(i8*, ...)

define arm_aapcscc i32 @unquantize_pyr(i16* %q_pyr, i32* %pyr, i32 %im_size, i32 %num_levels, i16* %bin_size) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %q_pyr.addr = alloca i16*, align 4
  %pyr.addr = alloca i32*, align 4
  %im_size.addr = alloca i32, align 4
  %num_levels.addr = alloca i32, align 4
  %bin_size.addr = alloca i16*, align 4
  %level = alloca i32, align 4
  %bin_index = alloca i32, align 4
  %im_num = alloca i32, align 4
  %pyr_offset = alloca i32, align 4
  store i16* %q_pyr, i16** %q_pyr.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q_pyr.addr}, metadata !117), !dbg !118
  store i32* %pyr, i32** %pyr.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %pyr.addr}, metadata !119), !dbg !121
  store i32 %im_size, i32* %im_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %im_size.addr}, metadata !122), !dbg !123
  store i32 %num_levels, i32* %num_levels.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %num_levels.addr}, metadata !124), !dbg !125
  store i16* %bin_size, i16** %bin_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %bin_size.addr}, metadata !126), !dbg !127
  call void @llvm.dbg.declare(metadata !{i32* %level}, metadata !128), !dbg !130
  call void @llvm.dbg.declare(metadata !{i32* %bin_index}, metadata !131), !dbg !132
  call void @llvm.dbg.declare(metadata !{i32* %im_num}, metadata !133), !dbg !134
  call void @llvm.dbg.declare(metadata !{i32* %pyr_offset}, metadata !135), !dbg !136
  store i32 0, i32* %pyr_offset, align 4, !dbg !137
  %0 = load i32* %num_levels.addr, align 4, !dbg !138
  %mul = mul nsw i32 %0, 2, !dbg !138
  %1 = load i32* %im_size.addr, align 4, !dbg !138
  %shr = ashr i32 %1, %mul, !dbg !138
  store i32 %shr, i32* %im_size.addr, align 4, !dbg !138
  %2 = load i32* %num_levels.addr, align 4, !dbg !138
  store i32 %2, i32* %level, align 4, !dbg !138
  store i32 0, i32* %bin_index, align 4, !dbg !138
  br label %for.cond, !dbg !138

for.cond:                                         ; preds = %for.inc7, %entry
  %3 = load i32* %level, align 4, !dbg !138
  %cmp = icmp sgt i32 %3, 0, !dbg !138
  br i1 %cmp, label %for.body, label %for.end8, !dbg !138

for.body:                                         ; preds = %for.cond
  %4 = load i32* %level, align 4, !dbg !140
  %5 = load i32* %num_levels.addr, align 4, !dbg !140
  %cmp1 = icmp eq i32 %4, %5, !dbg !140
  %cond = select i1 %cmp1, i32 0, i32 1, !dbg !140
  store i32 %cond, i32* %im_num, align 4, !dbg !140
  br label %for.cond2, !dbg !140

for.cond2:                                        ; preds = %for.inc, %for.body
  %6 = load i32* %im_num, align 4, !dbg !140
  %cmp3 = icmp slt i32 %6, 4, !dbg !140
  br i1 %cmp3, label %for.body4, label %for.end, !dbg !140

for.body4:                                        ; preds = %for.cond2
  %7 = load i16** %q_pyr.addr, align 4, !dbg !142
  %8 = load i32* %pyr_offset, align 4, !dbg !142
  %add.ptr = getelementptr inbounds i16* %7, i32 %8, !dbg !142
  %9 = load i32** %pyr.addr, align 4, !dbg !142
  %10 = load i32* %pyr_offset, align 4, !dbg !142
  %add.ptr5 = getelementptr inbounds i32* %9, i32 %10, !dbg !142
  %11 = load i32* %im_size.addr, align 4, !dbg !142
  %12 = load i32* %bin_index, align 4, !dbg !142
  %13 = load i16** %bin_size.addr, align 4, !dbg !142
  %arrayidx = getelementptr inbounds i16* %13, i32 %12, !dbg !142
  %14 = load i16* %arrayidx, !dbg !142
  %conv = zext i16 %14 to i32, !dbg !142
  %call = call arm_aapcscc  i32 @unquantize_image(i16* %add.ptr, i32* %add.ptr5, i32 %11, i32 %conv), !dbg !142
  br label %for.inc, !dbg !144

for.inc:                                          ; preds = %for.body4
  %15 = load i32* %im_num, align 4, !dbg !145
  %inc = add nsw i32 %15, 1, !dbg !145
  store i32 %inc, i32* %im_num, align 4, !dbg !145
  %16 = load i32* %im_size.addr, align 4, !dbg !145
  %17 = load i32* %pyr_offset, align 4, !dbg !145
  %add = add nsw i32 %17, %16, !dbg !145
  store i32 %add, i32* %pyr_offset, align 4, !dbg !145
  %18 = load i32* %bin_index, align 4, !dbg !145
  %inc6 = add nsw i32 %18, 1, !dbg !145
  store i32 %inc6, i32* %bin_index, align 4, !dbg !145
  br label %for.cond2, !dbg !145

for.end:                                          ; preds = %for.cond2
  br label %for.inc7, !dbg !146

for.inc7:                                         ; preds = %for.end
  %19 = load i32* %level, align 4, !dbg !147
  %dec = add nsw i32 %19, -1, !dbg !147
  store i32 %dec, i32* %level, align 4, !dbg !147
  %20 = load i32* %im_size.addr, align 4, !dbg !147
  %shl = shl i32 %20, 2, !dbg !147
  store i32 %shl, i32* %im_size.addr, align 4, !dbg !147
  br label %for.cond, !dbg !147

for.end8:                                         ; preds = %for.cond
  %21 = load i32* %retval, !dbg !148
  ret i32 %21, !dbg !148
}

define arm_aapcscc i32 @unquantize_image(i16* %q_im, i32* %res, i32 %im_size, i32) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %q_im.addr = alloca i16*, align 4
  %res.addr = alloca i32*, align 4
  %im_size.addr = alloca i32, align 4
  %bin_size.addr = alloca i16, align 2
  %i = alloca i32, align 4
  %correction = alloca float, align 4
  store i16* %q_im, i16** %q_im.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q_im.addr}, metadata !149), !dbg !150
  store i32* %res, i32** %res.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %res.addr}, metadata !151), !dbg !152
  store i32 %im_size, i32* %im_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %im_size.addr}, metadata !153), !dbg !154
  %bin_size = trunc i32 %0 to i16
  store i16 %bin_size, i16* %bin_size.addr, align 2
  call void @llvm.dbg.declare(metadata !{i16* %bin_size.addr}, metadata !155), !dbg !156
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !157), !dbg !159
  call void @llvm.dbg.declare(metadata !{float* %correction}, metadata !160), !dbg !161
  %1 = load i16* %bin_size.addr, align 2, !dbg !162
  %conv = zext i16 %1 to i32, !dbg !162
  %conv1 = sitofp i32 %conv to double, !dbg !162
  %mul = fmul double %conv1, 1.800000e-01, !dbg !162
  %sub = fsub double %mul, 5.000000e-01, !dbg !162
  %conv2 = fptrunc double %sub to float, !dbg !162
  store float %conv2, float* %correction, align 4, !dbg !162
  store i32 0, i32* %i, align 4, !dbg !163
  br label %for.cond, !dbg !163

for.cond:                                         ; preds = %for.inc, %entry
  %2 = load i32* %i, align 4, !dbg !163
  %3 = load i32* %im_size.addr, align 4, !dbg !163
  %cmp = icmp slt i32 %2, %3, !dbg !163
  br i1 %cmp, label %for.body, label %for.end, !dbg !163

for.body:                                         ; preds = %for.cond
  %4 = load i32* %i, align 4, !dbg !165
  %5 = load i16** %q_im.addr, align 4, !dbg !165
  %arrayidx = getelementptr inbounds i16* %5, i32 %4, !dbg !165
  %6 = load i16* %arrayidx, !dbg !165
  %conv4 = sext i16 %6 to i32, !dbg !165
  %cmp5 = icmp sge i32 %conv4, 1, !dbg !165
  br i1 %cmp5, label %if.then, label %if.else, !dbg !165

if.then:                                          ; preds = %for.body
  %7 = load i32* %i, align 4, !dbg !167
  %8 = load i16** %q_im.addr, align 4, !dbg !167
  %arrayidx7 = getelementptr inbounds i16* %8, i32 %7, !dbg !167
  %9 = load i16* %arrayidx7, !dbg !167
  %conv8 = sext i16 %9 to i32, !dbg !167
  %10 = load i16* %bin_size.addr, align 2, !dbg !167
  %conv9 = zext i16 %10 to i32, !dbg !167
  %mul10 = mul nsw i32 %conv8, %conv9, !dbg !167
  %conv11 = sitofp i32 %mul10 to float, !dbg !167
  %11 = load float* %correction, align 4, !dbg !167
  %sub12 = fsub float %conv11, %11, !dbg !167
  %conv13 = fptosi float %sub12 to i32, !dbg !167
  %12 = load i32* %i, align 4, !dbg !167
  %13 = load i32** %res.addr, align 4, !dbg !167
  %arrayidx14 = getelementptr inbounds i32* %13, i32 %12, !dbg !167
  store i32 %conv13, i32* %arrayidx14, !dbg !167
  br label %if.end29, !dbg !167

if.else:                                          ; preds = %for.body
  %14 = load i32* %i, align 4, !dbg !168
  %15 = load i16** %q_im.addr, align 4, !dbg !168
  %arrayidx15 = getelementptr inbounds i16* %15, i32 %14, !dbg !168
  %16 = load i16* %arrayidx15, !dbg !168
  %conv16 = sext i16 %16 to i32, !dbg !168
  %cmp17 = icmp sle i32 %conv16, -1, !dbg !168
  br i1 %cmp17, label %if.then19, label %if.else27, !dbg !168

if.then19:                                        ; preds = %if.else
  %17 = load i32* %i, align 4, !dbg !169
  %18 = load i16** %q_im.addr, align 4, !dbg !169
  %arrayidx20 = getelementptr inbounds i16* %18, i32 %17, !dbg !169
  %19 = load i16* %arrayidx20, !dbg !169
  %conv21 = sext i16 %19 to i32, !dbg !169
  %20 = load i16* %bin_size.addr, align 2, !dbg !169
  %conv22 = zext i16 %20 to i32, !dbg !169
  %mul23 = mul nsw i32 %conv21, %conv22, !dbg !169
  %conv24 = sitofp i32 %mul23 to float, !dbg !169
  %21 = load float* %correction, align 4, !dbg !169
  %add = fadd float %conv24, %21, !dbg !169
  %conv25 = fptosi float %add to i32, !dbg !169
  %22 = load i32* %i, align 4, !dbg !169
  %23 = load i32** %res.addr, align 4, !dbg !169
  %arrayidx26 = getelementptr inbounds i32* %23, i32 %22, !dbg !169
  store i32 %conv25, i32* %arrayidx26, !dbg !169
  br label %if.end, !dbg !169

if.else27:                                        ; preds = %if.else
  %24 = load i32* %i, align 4, !dbg !170
  %25 = load i32** %res.addr, align 4, !dbg !170
  %arrayidx28 = getelementptr inbounds i32* %25, i32 %24, !dbg !170
  store i32 0, i32* %arrayidx28, !dbg !170
  br label %if.end

if.end:                                           ; preds = %if.else27, %if.then19
  br label %if.end29

if.end29:                                         ; preds = %if.end, %if.then
  br label %for.inc, !dbg !171

for.inc:                                          ; preds = %if.end29
  %26 = load i32* %i, align 4, !dbg !172
  %inc = add nsw i32 %26, 1, !dbg !172
  store i32 %inc, i32* %i, align 4, !dbg !172
  br label %for.cond, !dbg !172

for.end:                                          ; preds = %for.cond
  %27 = load i32* %retval, !dbg !173
  ret i32 %27, !dbg !173
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !12, metadata !15, metadata !18}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"quantize_pyr", metadata !"quantize_pyr", metadata !"", metadata !6, i32 49, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i16*, i32, i32, double, i16*)* @quantize_pyr, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"quantize.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{i32 720942, i32 0, metadata !6, metadata !"quantize_image", metadata !"quantize_image", metadata !"", metadata !6, i32 85, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i16*, i32, i16*)* @quantize_image, null, null, metadata !13} ; [ DW_TAG_subprogram ]
!13 = metadata !{metadata !14}
!14 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!15 = metadata !{i32 720942, i32 0, metadata !6, metadata !"unquantize_pyr", metadata !"unquantize_pyr", metadata !"", metadata !6, i32 128, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i16*, i32*, i32, i32, i16*)* @unquantize_pyr, null, null, metadata !16} ; [ DW_TAG_subprogram ]
!16 = metadata !{metadata !17}
!17 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!18 = metadata !{i32 720942, i32 0, metadata !6, metadata !"unquantize_image", metadata !"unquantize_image", metadata !"", metadata !6, i32 152, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i16*, i32*, i32, i32)* @unquantize_image, null, null, metadata !19} ; [ DW_TAG_subprogram ]
!19 = metadata !{metadata !20}
!20 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!21 = metadata !{i32 721153, metadata !5, metadata !"float_pyr", metadata !6, i32 16777260, metadata !22, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!22 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !23} ; [ DW_TAG_pointer_type ]
!23 = metadata !{i32 720932, null, metadata !"float", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!24 = metadata !{i32 44, i32 10, metadata !5, null}
!25 = metadata !{i32 721153, metadata !5, metadata !"q_pyr", metadata !6, i32 33554477, metadata !26, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!26 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !27} ; [ DW_TAG_pointer_type ]
!27 = metadata !{i32 720918, null, metadata !"BinIndexType", metadata !6, i32 59, i64 0, i64 0, i64 0, i32 0, metadata !28} ; [ DW_TAG_typedef ]
!28 = metadata !{i32 720932, null, metadata !"short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!29 = metadata !{i32 45, i32 17, metadata !5, null}
!30 = metadata !{i32 721153, metadata !5, metadata !"im_size", metadata !6, i32 50331695, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!31 = metadata !{i32 47, i32 7, metadata !5, null}
!32 = metadata !{i32 721153, metadata !5, metadata !"num_levels", metadata !6, i32 67108911, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!33 = metadata !{i32 47, i32 16, metadata !5, null}
!34 = metadata !{i32 721153, metadata !5, metadata !"compression_factor", metadata !6, i32 83886128, metadata !35, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!35 = metadata !{i32 720932, null, metadata !"double", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!36 = metadata !{i32 48, i32 10, metadata !5, null}
!37 = metadata !{i32 721153, metadata !5, metadata !"bin_size", metadata !6, i32 100663342, metadata !38, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!38 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !39} ; [ DW_TAG_pointer_type ]
!39 = metadata !{i32 720918, null, metadata !"BinValueType", metadata !6, i32 64, i64 0, i64 0, i64 0, i32 0, metadata !40} ; [ DW_TAG_typedef ]
!40 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!41 = metadata !{i32 46, i32 17, metadata !5, null}
!42 = metadata !{i32 721152, metadata !43, metadata !"level", metadata !6, i32 50, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!43 = metadata !{i32 720907, metadata !5, i32 49, i32 3, metadata !6, i32 48} ; [ DW_TAG_lexical_block ]
!44 = metadata !{i32 50, i32 7, metadata !43, null}
!45 = metadata !{i32 721152, metadata !43, metadata !"bin_index", metadata !6, i32 50, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!46 = metadata !{i32 50, i32 14, metadata !43, null}
!47 = metadata !{i32 721152, metadata !43, metadata !"im_num", metadata !6, i32 50, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!48 = metadata !{i32 50, i32 25, metadata !43, null}
!49 = metadata !{i32 721152, metadata !43, metadata !"pyr_offset", metadata !6, i32 50, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!50 = metadata !{i32 50, i32 33, metadata !43, null}
!51 = metadata !{i32 721152, metadata !43, metadata !"the_bin_size", metadata !6, i32 51, metadata !35, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!52 = metadata !{i32 51, i32 10, metadata !43, null}
!53 = metadata !{i32 51, i32 43, metadata !43, null}
!54 = metadata !{i32 721152, metadata !43, metadata !"max_bin_size", metadata !6, i32 52, metadata !39, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!55 = metadata !{i32 52, i32 16, metadata !43, null}
!56 = metadata !{i32 52, i32 32, metadata !43, null}
!57 = metadata !{i32 54, i32 3, metadata !43, null}
!58 = metadata !{i32 56, i32 8, metadata !59, null}
!59 = metadata !{i32 720907, metadata !43, i32 56, i32 3, metadata !6, i32 49} ; [ DW_TAG_lexical_block ]
!60 = metadata !{i32 59, i32 10, metadata !61, null}
!61 = metadata !{i32 720907, metadata !59, i32 59, i32 5, metadata !6, i32 50} ; [ DW_TAG_lexical_block ]
!62 = metadata !{i32 63, i32 2, metadata !63, null}
!63 = metadata !{i32 720907, metadata !61, i32 62, i32 2, metadata !6, i32 51} ; [ DW_TAG_lexical_block ]
!64 = metadata !{i32 63, i32 24, metadata !63, null}
!65 = metadata !{i32 64, i32 7, metadata !63, null}
!66 = metadata !{i32 64, i32 38, metadata !63, null}
!67 = metadata !{i32 65, i32 7, metadata !63, null}
!68 = metadata !{i32 67, i32 2, metadata !63, null}
!69 = metadata !{i32 70, i32 2, metadata !63, null}
!70 = metadata !{i32 61, i32 3, metadata !61, null}
!71 = metadata !{i32 70, i32 2, metadata !61, null}
!72 = metadata !{i32 58, i32 8, metadata !59, null}
!73 = metadata !{i32 71, i32 3, metadata !43, null}
!74 = metadata !{i32 721153, metadata !12, metadata !"float_im", metadata !6, i32 16777297, metadata !22, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!75 = metadata !{i32 81, i32 19, metadata !12, null}
!76 = metadata !{i32 721153, metadata !12, metadata !"q_im", metadata !6, i32 33554515, metadata !26, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!77 = metadata !{i32 83, i32 26, metadata !12, null}
!78 = metadata !{i32 721153, metadata !12, metadata !"im_size", metadata !6, i32 50331732, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!79 = metadata !{i32 84, i32 7, metadata !12, null}
!80 = metadata !{i32 721153, metadata !12, metadata !"bin_size", metadata !6, i32 67108946, metadata !38, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!81 = metadata !{i32 82, i32 26, metadata !12, null}
!82 = metadata !{i32 721152, metadata !83, metadata !"i", metadata !6, i32 86, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!83 = metadata !{i32 720907, metadata !12, i32 85, i32 3, metadata !6, i32 52} ; [ DW_TAG_lexical_block ]
!84 = metadata !{i32 86, i32 16, metadata !83, null}
!85 = metadata !{i32 721152, metadata !83, metadata !"the_bin_size", metadata !6, i32 87, metadata !35, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!86 = metadata !{i32 87, i32 19, metadata !83, null}
!87 = metadata !{i32 721152, metadata !83, metadata !"ftemp", metadata !6, i32 88, metadata !35, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!88 = metadata !{i32 88, i32 19, metadata !83, null}
!89 = metadata !{i32 721152, metadata !83, metadata !"max_abs", metadata !6, i32 88, metadata !35, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!90 = metadata !{i32 88, i32 26, metadata !83, null}
!91 = metadata !{i32 88, i32 39, metadata !83, null}
!92 = metadata !{i32 91, i32 8, metadata !93, null}
!93 = metadata !{i32 720907, metadata !83, i32 91, i32 3, metadata !6, i32 53} ; [ DW_TAG_lexical_block ]
!94 = metadata !{i32 93, i32 7, metadata !95, null}
!95 = metadata !{i32 720907, metadata !93, i32 92, i32 7, metadata !6, i32 54} ; [ DW_TAG_lexical_block ]
!96 = metadata !{i32 94, i32 7, metadata !95, null}
!97 = metadata !{i32 94, i32 24, metadata !95, null}
!98 = metadata !{i32 95, i32 7, metadata !95, null}
!99 = metadata !{i32 95, i32 28, metadata !95, null}
!100 = metadata !{i32 96, i32 7, metadata !95, null}
!101 = metadata !{i32 91, i32 24, metadata !93, null}
!102 = metadata !{i32 100, i32 3, metadata !83, null}
!103 = metadata !{i32 102, i32 7, metadata !104, null}
!104 = metadata !{i32 720907, metadata !83, i32 101, i32 7, metadata !6, i32 55} ; [ DW_TAG_lexical_block ]
!105 = metadata !{i32 103, i32 7, metadata !104, null}
!106 = metadata !{i32 104, i32 7, metadata !104, null}
!107 = metadata !{i32 106, i32 7, metadata !104, null}
!108 = metadata !{i32 107, i32 3, metadata !83, null}
!109 = metadata !{i32 111, i32 8, metadata !110, null}
!110 = metadata !{i32 720907, metadata !83, i32 111, i32 3, metadata !6, i32 56} ; [ DW_TAG_lexical_block ]
!111 = metadata !{i32 113, i32 7, metadata !112, null}
!112 = metadata !{i32 720907, metadata !110, i32 112, i32 7, metadata !6, i32 57} ; [ DW_TAG_lexical_block ]
!113 = metadata !{i32 114, i32 7, metadata !112, null}
!114 = metadata !{i32 115, i32 7, metadata !112, null}
!115 = metadata !{i32 111, i32 24, metadata !110, null}
!116 = metadata !{i32 116, i32 3, metadata !83, null}
!117 = metadata !{i32 721153, metadata !15, metadata !"q_pyr", metadata !6, i32 16777341, metadata !26, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!118 = metadata !{i32 125, i32 17, metadata !15, null}
!119 = metadata !{i32 721153, metadata !15, metadata !"pyr", metadata !6, i32 33554558, metadata !120, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!120 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_pointer_type ]
!121 = metadata !{i32 126, i32 8, metadata !15, null}
!122 = metadata !{i32 721153, metadata !15, metadata !"im_size", metadata !6, i32 50331774, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!123 = metadata !{i32 126, i32 13, metadata !15, null}
!124 = metadata !{i32 721153, metadata !15, metadata !"num_levels", metadata !6, i32 67108990, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!125 = metadata !{i32 126, i32 22, metadata !15, null}
!126 = metadata !{i32 721153, metadata !15, metadata !"bin_size", metadata !6, i32 83886207, metadata !38, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!127 = metadata !{i32 127, i32 17, metadata !15, null}
!128 = metadata !{i32 721152, metadata !129, metadata !"level", metadata !6, i32 129, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!129 = metadata !{i32 720907, metadata !15, i32 128, i32 3, metadata !6, i32 58} ; [ DW_TAG_lexical_block ]
!130 = metadata !{i32 129, i32 7, metadata !129, null}
!131 = metadata !{i32 721152, metadata !129, metadata !"bin_index", metadata !6, i32 129, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!132 = metadata !{i32 129, i32 14, metadata !129, null}
!133 = metadata !{i32 721152, metadata !129, metadata !"im_num", metadata !6, i32 129, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!134 = metadata !{i32 129, i32 25, metadata !129, null}
!135 = metadata !{i32 721152, metadata !129, metadata !"pyr_offset", metadata !6, i32 129, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!136 = metadata !{i32 129, i32 33, metadata !129, null}
!137 = metadata !{i32 129, i32 47, metadata !129, null}
!138 = metadata !{i32 131, i32 8, metadata !139, null}
!139 = metadata !{i32 720907, metadata !129, i32 131, i32 3, metadata !6, i32 59} ; [ DW_TAG_lexical_block ]
!140 = metadata !{i32 134, i32 10, metadata !141, null}
!141 = metadata !{i32 720907, metadata !139, i32 134, i32 5, metadata !6, i32 60} ; [ DW_TAG_lexical_block ]
!142 = metadata !{i32 139, i32 2, metadata !143, null}
!143 = metadata !{i32 720907, metadata !141, i32 137, i32 2, metadata !6, i32 61} ; [ DW_TAG_lexical_block ]
!144 = metadata !{i32 141, i32 2, metadata !143, null}
!145 = metadata !{i32 136, i32 3, metadata !141, null}
!146 = metadata !{i32 141, i32 2, metadata !141, null}
!147 = metadata !{i32 133, i32 8, metadata !139, null}
!148 = metadata !{i32 142, i32 3, metadata !129, null}
!149 = metadata !{i32 721153, metadata !18, metadata !"q_im", metadata !6, i32 16777365, metadata !26, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!150 = metadata !{i32 149, i32 26, metadata !18, null}
!151 = metadata !{i32 721153, metadata !18, metadata !"res", metadata !6, i32 33554582, metadata !120, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!152 = metadata !{i32 150, i32 17, metadata !18, null}
!153 = metadata !{i32 721153, metadata !18, metadata !"im_size", metadata !6, i32 50331798, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!154 = metadata !{i32 150, i32 22, metadata !18, null}
!155 = metadata !{i32 721153, metadata !18, metadata !"bin_size", metadata !6, i32 67109015, metadata !39, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!156 = metadata !{i32 151, i32 25, metadata !18, null}
!157 = metadata !{i32 721152, metadata !158, metadata !"i", metadata !6, i32 153, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!158 = metadata !{i32 720907, metadata !18, i32 152, i32 3, metadata !6, i32 62} ; [ DW_TAG_lexical_block ]
!159 = metadata !{i32 153, i32 16, metadata !158, null}
!160 = metadata !{i32 721152, metadata !158, metadata !"correction", metadata !6, i32 155, metadata !23, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!161 = metadata !{i32 155, i32 18, metadata !158, null}
!162 = metadata !{i32 155, i32 52, metadata !158, null}
!163 = metadata !{i32 157, i32 8, metadata !164, null}
!164 = metadata !{i32 720907, metadata !158, i32 157, i32 3, metadata !6, i32 63} ; [ DW_TAG_lexical_block ]
!165 = metadata !{i32 159, i32 7, metadata !166, null}
!166 = metadata !{i32 720907, metadata !164, i32 158, i32 7, metadata !6, i32 64} ; [ DW_TAG_lexical_block ]
!167 = metadata !{i32 160, i32 9, metadata !166, null}
!168 = metadata !{i32 161, i32 12, metadata !166, null}
!169 = metadata !{i32 162, i32 2, metadata !166, null}
!170 = metadata !{i32 164, i32 2, metadata !166, null}
!171 = metadata !{i32 165, i32 7, metadata !166, null}
!172 = metadata !{i32 157, i32 24, metadata !164, null}
!173 = metadata !{i32 166, i32 3, metadata !158, null}
