; ModuleID = 'build_pyr.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

@.str = private unnamed_addr constant [9 x i8] c"reflect1\00", align 1

define arm_aapcscc i32 @build_pyr(float* %image, i32 %x_size, i32 %y_size, i32 %num_levels, float* %lo_filter, float* %hi_filter, i32 %filter_size) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %image.addr = alloca float*, align 4
  %x_size.addr = alloca i32, align 4
  %y_size.addr = alloca i32, align 4
  %num_levels.addr = alloca i32, align 4
  %lo_filter.addr = alloca float*, align 4
  %hi_filter.addr = alloca float*, align 4
  %filter_size.addr = alloca i32, align 4
  %x_level = alloca i32, align 4
  %y_level = alloca i32, align 4
  %level = alloca i32, align 4
  store float* %image, float** %image.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %image.addr}, metadata !18), !dbg !21
  store i32 %x_size, i32* %x_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_size.addr}, metadata !22), !dbg !23
  store i32 %y_size, i32* %y_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_size.addr}, metadata !24), !dbg !25
  store i32 %num_levels, i32* %num_levels.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %num_levels.addr}, metadata !26), !dbg !27
  store float* %lo_filter, float** %lo_filter.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %lo_filter.addr}, metadata !28), !dbg !29
  store float* %hi_filter, float** %hi_filter.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %hi_filter.addr}, metadata !30), !dbg !31
  store i32 %filter_size, i32* %filter_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %filter_size.addr}, metadata !32), !dbg !33
  call void @llvm.dbg.declare(metadata !{i32* %x_level}, metadata !34), !dbg !36
  call void @llvm.dbg.declare(metadata !{i32* %y_level}, metadata !37), !dbg !38
  call void @llvm.dbg.declare(metadata !{i32* %level}, metadata !39), !dbg !40
  store i32 0, i32* %level, align 4, !dbg !41
  %0 = load i32* %x_size.addr, align 4, !dbg !41
  store i32 %0, i32* %x_level, align 4, !dbg !41
  %1 = load i32* %y_size.addr, align 4, !dbg !41
  store i32 %1, i32* %y_level, align 4, !dbg !41
  br label %for.cond, !dbg !41

for.cond:                                         ; preds = %for.inc, %entry
  %2 = load i32* %level, align 4, !dbg !41
  %3 = load i32* %num_levels.addr, align 4, !dbg !41
  %cmp = icmp slt i32 %2, %3, !dbg !41
  br i1 %cmp, label %for.body, label %for.end, !dbg !41

for.body:                                         ; preds = %for.cond
  %4 = load float** %image.addr, align 4, !dbg !43
  %5 = load i32* %x_level, align 4, !dbg !43
  %6 = load i32* %y_level, align 4, !dbg !43
  %7 = load float** %lo_filter.addr, align 4, !dbg !43
  %8 = load float** %hi_filter.addr, align 4, !dbg !43
  %9 = load i32* %filter_size.addr, align 4, !dbg !43
  %10 = load float** %image.addr, align 4, !dbg !43
  %call = call arm_aapcscc  i32 @build_level(float* %4, i32 %5, i32 %6, float* %7, float* %8, i32 %9, float* %10), !dbg !43
  br label %for.inc, !dbg !43

for.inc:                                          ; preds = %for.body
  %11 = load i32* %level, align 4, !dbg !44
  %inc = add nsw i32 %11, 1, !dbg !44
  store i32 %inc, i32* %level, align 4, !dbg !44
  %12 = load i32* %x_level, align 4, !dbg !44
  %div = sdiv i32 %12, 2, !dbg !44
  store i32 %div, i32* %x_level, align 4, !dbg !44
  %13 = load i32* %y_level, align 4, !dbg !44
  %div1 = sdiv i32 %13, 2, !dbg !44
  store i32 %div1, i32* %y_level, align 4, !dbg !44
  br label %for.cond, !dbg !44

for.end:                                          ; preds = %for.cond
  %14 = load i32* %retval, !dbg !45
  ret i32 %14, !dbg !45
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

define arm_aapcscc i32 @build_level(float* %image, i32 %level_x_size, i32 %level_y_size, float* %lo_filter, float* %hi_filter, i32 %filter_size, float* %result_block) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %image.addr = alloca float*, align 4
  %level_x_size.addr = alloca i32, align 4
  %level_y_size.addr = alloca i32, align 4
  %lo_filter.addr = alloca float*, align 4
  %hi_filter.addr = alloca float*, align 4
  %filter_size.addr = alloca i32, align 4
  %result_block.addr = alloca float*, align 4
  %hi_imagetemp = alloca float*, align 4
  %lo_imagetemp = alloca float*, align 4
  %filtertemp = alloca float*, align 4
  %total_size = alloca i32, align 4
  %i = alloca i32, align 4
  store float* %image, float** %image.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %image.addr}, metadata !46), !dbg !47
  store i32 %level_x_size, i32* %level_x_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %level_x_size.addr}, metadata !48), !dbg !49
  store i32 %level_y_size, i32* %level_y_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %level_y_size.addr}, metadata !50), !dbg !51
  store float* %lo_filter, float** %lo_filter.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %lo_filter.addr}, metadata !52), !dbg !53
  store float* %hi_filter, float** %hi_filter.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %hi_filter.addr}, metadata !54), !dbg !55
  store i32 %filter_size, i32* %filter_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %filter_size.addr}, metadata !56), !dbg !57
  store float* %result_block, float** %result_block.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result_block.addr}, metadata !58), !dbg !59
  call void @llvm.dbg.declare(metadata !{float** %hi_imagetemp}, metadata !60), !dbg !62
  call void @llvm.dbg.declare(metadata !{float** %lo_imagetemp}, metadata !63), !dbg !64
  call void @llvm.dbg.declare(metadata !{float** %filtertemp}, metadata !65), !dbg !66
  call void @llvm.dbg.declare(metadata !{i32* %total_size}, metadata !67), !dbg !68
  %0 = load i32* %level_x_size.addr, align 4, !dbg !69
  %1 = load i32* %level_y_size.addr, align 4, !dbg !69
  %mul = mul nsw i32 %0, %1, !dbg !69
  store i32 %mul, i32* %total_size, align 4, !dbg !69
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !70), !dbg !71
  %2 = load i32* %filter_size.addr, align 4, !dbg !72
  %mul1 = mul i32 %2, 4, !dbg !72
  %call = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %mul1), !dbg !72
  %3 = bitcast i8* %call to float*, !dbg !72
  store float* %3, float** %filtertemp, align 4, !dbg !72
  %4 = load i32* %total_size, align 4, !dbg !73
  %mul2 = mul i32 %4, 4, !dbg !73
  %div = udiv i32 %mul2, 2, !dbg !73
  %call3 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %div), !dbg !73
  %5 = bitcast i8* %call3 to float*, !dbg !73
  store float* %5, float** %hi_imagetemp, align 4, !dbg !73
  %6 = load i32* %total_size, align 4, !dbg !74
  %mul4 = mul i32 %6, 4, !dbg !74
  %div5 = udiv i32 %mul4, 2, !dbg !74
  %call6 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %div5), !dbg !74
  %7 = bitcast i8* %call6 to float*, !dbg !74
  store float* %7, float** %lo_imagetemp, align 4, !dbg !74
  %8 = load float** %image.addr, align 4, !dbg !75
  %9 = load i32* %level_x_size.addr, align 4, !dbg !75
  %10 = load i32* %level_y_size.addr, align 4, !dbg !75
  %11 = load float** %lo_filter.addr, align 4, !dbg !75
  %12 = load float** %filtertemp, align 4, !dbg !75
  %13 = load i32* %filter_size.addr, align 4, !dbg !75
  %14 = load float** %lo_imagetemp, align 4, !dbg !75
  %call7 = call arm_aapcscc  i32 bitcast (i32 (...)* @internal_filter to i32 (float*, i32, i32, float*, float*, i32, i32, i32, i32, i32, i32, float*, i8*, i32)*)(float* %8, i32 %9, i32 %10, float* %11, float* %12, i32 %13, i32 1, i32 0, i32 2, i32 0, i32 1, float* %14, i8* getelementptr inbounds ([9 x i8]* @.str, i32 0, i32 0), i32 0), !dbg !75
  %15 = load float** %image.addr, align 4, !dbg !76
  %16 = load i32* %level_x_size.addr, align 4, !dbg !76
  %17 = load i32* %level_y_size.addr, align 4, !dbg !76
  %18 = load float** %hi_filter.addr, align 4, !dbg !76
  %19 = load float** %filtertemp, align 4, !dbg !76
  %20 = load i32* %filter_size.addr, align 4, !dbg !76
  %21 = load float** %hi_imagetemp, align 4, !dbg !76
  %call8 = call arm_aapcscc  i32 bitcast (i32 (...)* @internal_filter to i32 (float*, i32, i32, float*, float*, i32, i32, i32, i32, i32, i32, float*, i8*, i32)*)(float* %15, i32 %16, i32 %17, float* %18, float* %19, i32 %20, i32 1, i32 1, i32 2, i32 0, i32 1, float* %21, i8* getelementptr inbounds ([9 x i8]* @.str, i32 0, i32 0), i32 0), !dbg !76
  %22 = load i32* %level_x_size.addr, align 4, !dbg !77
  %div9 = sdiv i32 %22, 2, !dbg !77
  store i32 %div9, i32* %level_x_size.addr, align 4, !dbg !77
  %23 = load float** %lo_imagetemp, align 4, !dbg !78
  %24 = load i32* %level_x_size.addr, align 4, !dbg !78
  %25 = load i32* %level_y_size.addr, align 4, !dbg !78
  %26 = load float** %lo_filter.addr, align 4, !dbg !78
  %27 = load float** %filtertemp, align 4, !dbg !78
  %28 = load i32* %filter_size.addr, align 4, !dbg !78
  %29 = load float** %result_block.addr, align 4, !dbg !78
  %call10 = call arm_aapcscc  i32 bitcast (i32 (...)* @internal_filter to i32 (float*, i32, i32, float*, float*, i32, i32, i32, i32, i32, i32, float*, i8*, i32)*)(float* %23, i32 %24, i32 %25, float* %26, float* %27, i32 1, i32 %28, i32 0, i32 1, i32 0, i32 2, float* %29, i8* getelementptr inbounds ([9 x i8]* @.str, i32 0, i32 0), i32 0), !dbg !78
  %30 = load float** %lo_imagetemp, align 4, !dbg !79
  %31 = load i32* %level_x_size.addr, align 4, !dbg !79
  %32 = load i32* %level_y_size.addr, align 4, !dbg !79
  %33 = load float** %hi_filter.addr, align 4, !dbg !79
  %34 = load float** %filtertemp, align 4, !dbg !79
  %35 = load i32* %filter_size.addr, align 4, !dbg !79
  %36 = load i32* %total_size, align 4, !dbg !79
  %div11 = sdiv i32 %36, 4, !dbg !79
  %37 = load float** %result_block.addr, align 4, !dbg !79
  %add.ptr = getelementptr inbounds float* %37, i32 %div11, !dbg !79
  store float* %add.ptr, float** %result_block.addr, align 4, !dbg !79
  %call12 = call arm_aapcscc  i32 bitcast (i32 (...)* @internal_filter to i32 (float*, i32, i32, float*, float*, i32, i32, i32, i32, i32, i32, float*, i8*, i32)*)(float* %30, i32 %31, i32 %32, float* %33, float* %34, i32 1, i32 %35, i32 0, i32 1, i32 1, i32 2, float* %add.ptr, i8* getelementptr inbounds ([9 x i8]* @.str, i32 0, i32 0), i32 0), !dbg !79
  %38 = load float** %hi_imagetemp, align 4, !dbg !80
  %39 = load i32* %level_x_size.addr, align 4, !dbg !80
  %40 = load i32* %level_y_size.addr, align 4, !dbg !80
  %41 = load float** %lo_filter.addr, align 4, !dbg !80
  %42 = load float** %filtertemp, align 4, !dbg !80
  %43 = load i32* %filter_size.addr, align 4, !dbg !80
  %44 = load i32* %total_size, align 4, !dbg !80
  %div13 = sdiv i32 %44, 4, !dbg !80
  %45 = load float** %result_block.addr, align 4, !dbg !80
  %add.ptr14 = getelementptr inbounds float* %45, i32 %div13, !dbg !80
  store float* %add.ptr14, float** %result_block.addr, align 4, !dbg !80
  %call15 = call arm_aapcscc  i32 bitcast (i32 (...)* @internal_filter to i32 (float*, i32, i32, float*, float*, i32, i32, i32, i32, i32, i32, float*, i8*, i32)*)(float* %38, i32 %39, i32 %40, float* %41, float* %42, i32 1, i32 %43, i32 0, i32 1, i32 0, i32 2, float* %add.ptr14, i8* getelementptr inbounds ([9 x i8]* @.str, i32 0, i32 0), i32 0), !dbg !80
  %46 = load float** %result_block.addr, align 4, !dbg !81
  %47 = load i32* %level_y_size.addr, align 4, !dbg !81
  %div16 = sdiv i32 %47, 2, !dbg !81
  %48 = load i32* %level_x_size.addr, align 4, !dbg !81
  %call17 = call arm_aapcscc  i32 @internal_transpose(float* %46, i32 %div16, i32 %48), !dbg !81
  %49 = load float** %hi_imagetemp, align 4, !dbg !82
  %50 = load i32* %level_x_size.addr, align 4, !dbg !82
  %51 = load i32* %level_y_size.addr, align 4, !dbg !82
  %52 = load float** %hi_filter.addr, align 4, !dbg !82
  %53 = load float** %filtertemp, align 4, !dbg !82
  %54 = load i32* %filter_size.addr, align 4, !dbg !82
  %55 = load i32* %total_size, align 4, !dbg !82
  %div18 = sdiv i32 %55, 4, !dbg !82
  %56 = load float** %result_block.addr, align 4, !dbg !82
  %add.ptr19 = getelementptr inbounds float* %56, i32 %div18, !dbg !82
  store float* %add.ptr19, float** %result_block.addr, align 4, !dbg !82
  %call20 = call arm_aapcscc  i32 bitcast (i32 (...)* @internal_filter to i32 (float*, i32, i32, float*, float*, i32, i32, i32, i32, i32, i32, float*, i8*, i32)*)(float* %49, i32 %50, i32 %51, float* %52, float* %53, i32 1, i32 %54, i32 0, i32 1, i32 1, i32 2, float* %add.ptr19, i8* getelementptr inbounds ([9 x i8]* @.str, i32 0, i32 0), i32 0), !dbg !82
  %57 = load float** %filtertemp, align 4, !dbg !83
  %58 = bitcast float* %57 to i8*, !dbg !83
  %call21 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %58), !dbg !83
  %59 = load float** %hi_imagetemp, align 4, !dbg !84
  %60 = bitcast float* %59 to i8*, !dbg !84
  %call22 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %60), !dbg !84
  %61 = load float** %lo_imagetemp, align 4, !dbg !85
  %62 = bitcast float* %61 to i8*, !dbg !85
  %call23 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %62), !dbg !85
  %63 = load i32* %retval, !dbg !86
  ret i32 %63, !dbg !86
}

declare arm_aapcscc i8* @check_malloc(...)

declare arm_aapcscc i32 @internal_filter(...)

declare arm_aapcscc i32 @check_free(...)

define arm_aapcscc i32 @internal_transpose(float* %mat, i32 %rows, i32 %cols) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %mat.addr = alloca float*, align 4
  %rows.addr = alloca i32, align 4
  %cols.addr = alloca i32, align 4
  %swap_pos = alloca i32, align 4
  %modulus = alloca i32, align 4
  %current_pos = alloca i32, align 4
  %swap_val = alloca float, align 4
  store float* %mat, float** %mat.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %mat.addr}, metadata !87), !dbg !88
  store i32 %rows, i32* %rows.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %rows.addr}, metadata !89), !dbg !90
  store i32 %cols, i32* %cols.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %cols.addr}, metadata !91), !dbg !92
  call void @llvm.dbg.declare(metadata !{i32* %swap_pos}, metadata !93), !dbg !95
  call void @llvm.dbg.declare(metadata !{i32* %modulus}, metadata !96), !dbg !97
  %0 = load i32* %rows.addr, align 4, !dbg !98
  %1 = load i32* %cols.addr, align 4, !dbg !98
  %mul = mul nsw i32 %0, %1, !dbg !98
  %sub = sub nsw i32 %mul, 1, !dbg !98
  store i32 %sub, i32* %modulus, align 4, !dbg !98
  call void @llvm.dbg.declare(metadata !{i32* %current_pos}, metadata !99), !dbg !100
  call void @llvm.dbg.declare(metadata !{float* %swap_val}, metadata !101), !dbg !102
  store i32 1, i32* %current_pos, align 4, !dbg !103
  br label %for.cond, !dbg !103

for.cond:                                         ; preds = %for.inc, %entry
  %2 = load i32* %current_pos, align 4, !dbg !103
  %3 = load i32* %modulus, align 4, !dbg !103
  %cmp = icmp slt i32 %2, %3, !dbg !103
  br i1 %cmp, label %for.body, label %for.end, !dbg !103

for.body:                                         ; preds = %for.cond
  %4 = load i32* %current_pos, align 4, !dbg !105
  store i32 %4, i32* %swap_pos, align 4, !dbg !105
  br label %do.body, !dbg !107

do.body:                                          ; preds = %do.cond, %for.body
  %5 = load i32* %swap_pos, align 4, !dbg !108
  %6 = load i32* %cols.addr, align 4, !dbg !108
  %mul1 = mul nsw i32 %5, %6, !dbg !108
  %7 = load i32* %modulus, align 4, !dbg !108
  %rem = srem i32 %mul1, %7, !dbg !108
  store i32 %rem, i32* %swap_pos, align 4, !dbg !108
  br label %do.cond, !dbg !110

do.cond:                                          ; preds = %do.body
  %8 = load i32* %swap_pos, align 4, !dbg !110
  %9 = load i32* %current_pos, align 4, !dbg !110
  %cmp2 = icmp slt i32 %8, %9, !dbg !110
  br i1 %cmp2, label %do.body, label %do.end, !dbg !110

do.end:                                           ; preds = %do.cond
  %10 = load i32* %current_pos, align 4, !dbg !111
  %11 = load i32* %swap_pos, align 4, !dbg !111
  %cmp3 = icmp ne i32 %10, %11, !dbg !111
  br i1 %cmp3, label %if.then, label %if.end, !dbg !111

if.then:                                          ; preds = %do.end
  %12 = load i32* %swap_pos, align 4, !dbg !112
  %13 = load float** %mat.addr, align 4, !dbg !112
  %arrayidx = getelementptr inbounds float* %13, i32 %12, !dbg !112
  %14 = load float* %arrayidx, !dbg !112
  store float %14, float* %swap_val, align 4, !dbg !112
  %15 = load i32* %current_pos, align 4, !dbg !114
  %16 = load float** %mat.addr, align 4, !dbg !114
  %arrayidx4 = getelementptr inbounds float* %16, i32 %15, !dbg !114
  %17 = load float* %arrayidx4, !dbg !114
  %18 = load i32* %swap_pos, align 4, !dbg !114
  %19 = load float** %mat.addr, align 4, !dbg !114
  %arrayidx5 = getelementptr inbounds float* %19, i32 %18, !dbg !114
  store float %17, float* %arrayidx5, !dbg !114
  %20 = load float* %swap_val, align 4, !dbg !115
  %21 = load i32* %current_pos, align 4, !dbg !115
  %22 = load float** %mat.addr, align 4, !dbg !115
  %arrayidx6 = getelementptr inbounds float* %22, i32 %21, !dbg !115
  store float %20, float* %arrayidx6, !dbg !115
  br label %if.end, !dbg !116

if.end:                                           ; preds = %if.then, %do.end
  br label %for.inc, !dbg !117

for.inc:                                          ; preds = %if.end
  %23 = load i32* %current_pos, align 4, !dbg !118
  %inc = add nsw i32 %23, 1, !dbg !118
  store i32 %inc, i32* %current_pos, align 4, !dbg !118
  br label %for.cond, !dbg !118

for.end:                                          ; preds = %for.cond
  %24 = load i32* %retval, !dbg !119
  ret i32 %24, !dbg !119
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !12, metadata !15}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"build_pyr", metadata !"build_pyr", metadata !"", metadata !6, i32 39, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, float*, float*, i32)* @build_pyr, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"build_pyr.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{i32 720942, i32 0, metadata !6, metadata !"build_level", metadata !"build_level", metadata !"", metadata !6, i32 60, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, float*, float*, i32, float*)* @build_level, null, null, metadata !13} ; [ DW_TAG_subprogram ]
!13 = metadata !{metadata !14}
!14 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!15 = metadata !{i32 720942, i32 0, metadata !6, metadata !"internal_transpose", metadata !"internal_transpose", metadata !"", metadata !6, i32 106, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32)* @internal_transpose, null, null, metadata !16} ; [ DW_TAG_subprogram ]
!16 = metadata !{metadata !17}
!17 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!18 = metadata !{i32 721153, metadata !5, metadata !"image", metadata !6, i32 16777254, metadata !19, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!19 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !20} ; [ DW_TAG_pointer_type ]
!20 = metadata !{i32 720932, null, metadata !"float", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!21 = metadata !{i32 38, i32 10, metadata !5, null}
!22 = metadata !{i32 721153, metadata !5, metadata !"x_size", metadata !6, i32 33554469, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!23 = metadata !{i32 37, i32 7, metadata !5, null}
!24 = metadata !{i32 721153, metadata !5, metadata !"y_size", metadata !6, i32 50331685, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!25 = metadata !{i32 37, i32 15, metadata !5, null}
!26 = metadata !{i32 721153, metadata !5, metadata !"num_levels", metadata !6, i32 67108901, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!27 = metadata !{i32 37, i32 23, metadata !5, null}
!28 = metadata !{i32 721153, metadata !5, metadata !"lo_filter", metadata !6, i32 83886118, metadata !19, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!29 = metadata !{i32 38, i32 18, metadata !5, null}
!30 = metadata !{i32 721153, metadata !5, metadata !"hi_filter", metadata !6, i32 100663334, metadata !19, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!31 = metadata !{i32 38, i32 30, metadata !5, null}
!32 = metadata !{i32 721153, metadata !5, metadata !"filter_size", metadata !6, i32 117440549, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!33 = metadata !{i32 37, i32 35, metadata !5, null}
!34 = metadata !{i32 721152, metadata !35, metadata !"x_level", metadata !6, i32 40, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!35 = metadata !{i32 720907, metadata !5, i32 39, i32 3, metadata !6, i32 126} ; [ DW_TAG_lexical_block ]
!36 = metadata !{i32 40, i32 7, metadata !35, null}
!37 = metadata !{i32 721152, metadata !35, metadata !"y_level", metadata !6, i32 40, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!38 = metadata !{i32 40, i32 16, metadata !35, null}
!39 = metadata !{i32 721152, metadata !35, metadata !"level", metadata !6, i32 40, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!40 = metadata !{i32 40, i32 25, metadata !35, null}
!41 = metadata !{i32 42, i32 8, metadata !42, null}
!42 = metadata !{i32 720907, metadata !35, i32 42, i32 3, metadata !6, i32 127} ; [ DW_TAG_lexical_block ]
!43 = metadata !{i32 45, i32 5, metadata !42, null}
!44 = metadata !{i32 44, i32 8, metadata !42, null}
!45 = metadata !{i32 46, i32 3, metadata !35, null}
!46 = metadata !{i32 721153, metadata !12, metadata !"image", metadata !6, i32 16777275, metadata !19, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!47 = metadata !{i32 59, i32 10, metadata !12, null}
!48 = metadata !{i32 721153, metadata !12, metadata !"level_x_size", metadata !6, i32 33554490, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!49 = metadata !{i32 58, i32 7, metadata !12, null}
!50 = metadata !{i32 721153, metadata !12, metadata !"level_y_size", metadata !6, i32 50331706, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!51 = metadata !{i32 58, i32 21, metadata !12, null}
!52 = metadata !{i32 721153, metadata !12, metadata !"lo_filter", metadata !6, i32 67108923, metadata !19, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!53 = metadata !{i32 59, i32 33, metadata !12, null}
!54 = metadata !{i32 721153, metadata !12, metadata !"hi_filter", metadata !6, i32 83886139, metadata !19, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!55 = metadata !{i32 59, i32 45, metadata !12, null}
!56 = metadata !{i32 721153, metadata !12, metadata !"filter_size", metadata !6, i32 100663354, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!57 = metadata !{i32 58, i32 35, metadata !12, null}
!58 = metadata !{i32 721153, metadata !12, metadata !"result_block", metadata !6, i32 117440571, metadata !19, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!59 = metadata !{i32 59, i32 18, metadata !12, null}
!60 = metadata !{i32 721152, metadata !61, metadata !"hi_imagetemp", metadata !6, i32 61, metadata !19, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!61 = metadata !{i32 720907, metadata !12, i32 60, i32 3, metadata !6, i32 128} ; [ DW_TAG_lexical_block ]
!62 = metadata !{i32 61, i32 10, metadata !61, null}
!63 = metadata !{i32 721152, metadata !61, metadata !"lo_imagetemp", metadata !6, i32 61, metadata !19, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!64 = metadata !{i32 61, i32 25, metadata !61, null}
!65 = metadata !{i32 721152, metadata !61, metadata !"filtertemp", metadata !6, i32 61, metadata !19, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!66 = metadata !{i32 61, i32 40, metadata !61, null}
!67 = metadata !{i32 721152, metadata !61, metadata !"total_size", metadata !6, i32 62, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!68 = metadata !{i32 62, i32 7, metadata !61, null}
!69 = metadata !{i32 62, i32 48, metadata !61, null}
!70 = metadata !{i32 721152, metadata !61, metadata !"i", metadata !6, i32 62, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!71 = metadata !{i32 62, i32 47, metadata !61, null}
!72 = metadata !{i32 64, i32 26, metadata !61, null}
!73 = metadata !{i32 65, i32 28, metadata !61, null}
!74 = metadata !{i32 66, i32 28, metadata !61, null}
!75 = metadata !{i32 68, i32 3, metadata !61, null}
!76 = metadata !{i32 71, i32 3, metadata !61, null}
!77 = metadata !{i32 75, i32 3, metadata !61, null}
!78 = metadata !{i32 77, i32 3, metadata !61, null}
!79 = metadata !{i32 80, i32 3, metadata !61, null}
!80 = metadata !{i32 83, i32 3, metadata !61, null}
!81 = metadata !{i32 87, i32 3, metadata !61, null}
!82 = metadata !{i32 88, i32 3, metadata !61, null}
!83 = metadata !{i32 91, i32 3, metadata !61, null}
!84 = metadata !{i32 92, i32 3, metadata !61, null}
!85 = metadata !{i32 93, i32 3, metadata !61, null}
!86 = metadata !{i32 94, i32 1, metadata !61, null}
!87 = metadata !{i32 721153, metadata !15, metadata !"mat", metadata !6, i32 16777319, metadata !19, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!88 = metadata !{i32 103, i32 19, metadata !15, null}
!89 = metadata !{i32 721153, metadata !15, metadata !"rows", metadata !6, i32 33554537, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!90 = metadata !{i32 105, i32 7, metadata !15, null}
!91 = metadata !{i32 721153, metadata !15, metadata !"cols", metadata !6, i32 50331752, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!92 = metadata !{i32 104, i32 16, metadata !15, null}
!93 = metadata !{i32 721152, metadata !94, metadata !"swap_pos", metadata !6, i32 107, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!94 = metadata !{i32 720907, metadata !15, i32 106, i32 3, metadata !6, i32 129} ; [ DW_TAG_lexical_block ]
!95 = metadata !{i32 107, i32 16, metadata !94, null}
!96 = metadata !{i32 721152, metadata !94, metadata !"modulus", metadata !6, i32 108, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!97 = metadata !{i32 108, i32 16, metadata !94, null}
!98 = metadata !{i32 108, i32 39, metadata !94, null}
!99 = metadata !{i32 721152, metadata !94, metadata !"current_pos", metadata !6, i32 109, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!100 = metadata !{i32 109, i32 16, metadata !94, null}
!101 = metadata !{i32 721152, metadata !94, metadata !"swap_val", metadata !6, i32 110, metadata !20, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!102 = metadata !{i32 110, i32 18, metadata !94, null}
!103 = metadata !{i32 113, i32 8, metadata !104, null}
!104 = metadata !{i32 720907, metadata !94, i32 113, i32 3, metadata !6, i32 130} ; [ DW_TAG_lexical_block ]
!105 = metadata !{i32 116, i32 7, metadata !106, null}
!106 = metadata !{i32 720907, metadata !104, i32 114, i32 7, metadata !6, i32 131} ; [ DW_TAG_lexical_block ]
!107 = metadata !{i32 117, i32 7, metadata !106, null}
!108 = metadata !{i32 119, i32 4, metadata !109, null}
!109 = metadata !{i32 720907, metadata !106, i32 118, i32 4, metadata !6, i32 132} ; [ DW_TAG_lexical_block ]
!110 = metadata !{i32 120, i32 4, metadata !109, null}
!111 = metadata !{i32 123, i32 7, metadata !106, null}
!112 = metadata !{i32 125, i32 4, metadata !113, null}
!113 = metadata !{i32 720907, metadata !106, i32 124, i32 4, metadata !6, i32 133} ; [ DW_TAG_lexical_block ]
!114 = metadata !{i32 126, i32 4, metadata !113, null}
!115 = metadata !{i32 127, i32 4, metadata !113, null}
!116 = metadata !{i32 128, i32 4, metadata !113, null}
!117 = metadata !{i32 129, i32 7, metadata !106, null}
!118 = metadata !{i32 113, i32 44, metadata !104, null}
!119 = metadata !{i32 130, i32 3, metadata !94, null}
