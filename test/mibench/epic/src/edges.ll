; ModuleID = 'edges.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct.EDGE_HANDLER = type { i8*, i32 (...)* }

@edge_foos = internal global [9 x %struct.EDGE_HANDLER] [%struct.EDGE_HANDLER { i8* getelementptr inbounds ([9 x i8]* @.str1, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @reflect1 to i32 (...)*) }, %struct.EDGE_HANDLER { i8* getelementptr inbounds ([9 x i8]* @.str2, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @reflect2 to i32 (...)*) }, %struct.EDGE_HANDLER { i8* getelementptr inbounds ([7 x i8]* @.str3, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @repeat to i32 (...)*) }, %struct.EDGE_HANDLER { i8* getelementptr inbounds ([5 x i8]* @.str4, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @zero to i32 (...)*) }, %struct.EDGE_HANDLER { i8* getelementptr inbounds ([7 x i8]* @.str5, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @extend to i32 (...)*) }, %struct.EDGE_HANDLER { i8* getelementptr inbounds ([13 x i8]* @.str6, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @nocompute to i32 (...)*) }, %struct.EDGE_HANDLER { i8* getelementptr inbounds ([8 x i8]* @.str7, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @predict to i32 (...)*) }, %struct.EDGE_HANDLER { i8* getelementptr inbounds ([9 x i8]* @.str8, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @ereflect to i32 (...)*) }, %struct.EDGE_HANDLER { i8* getelementptr inbounds ([9 x i8]* @.str9, i32 0, i32 0), i32 (...)* bitcast (i32 (float*, i32, i32, i32, i32, float*, i32)* @reflect1 to i32 (...)*) }], align 4
@.str = private unnamed_addr constant [31 x i8] c"No such edge handler routine!\0A\00", align 1
@.str1 = private unnamed_addr constant [9 x i8] c"reflect1\00", align 1
@.str2 = private unnamed_addr constant [9 x i8] c"reflect2\00", align 1
@.str3 = private unnamed_addr constant [7 x i8] c"repeat\00", align 1
@.str4 = private unnamed_addr constant [5 x i8] c"zero\00", align 1
@.str5 = private unnamed_addr constant [7 x i8] c"extend\00", align 1
@.str6 = private unnamed_addr constant [13 x i8] c"dont-compute\00", align 1
@.str7 = private unnamed_addr constant [8 x i8] c"predict\00", align 1
@.str8 = private unnamed_addr constant [9 x i8] c"ereflect\00", align 1
@.str9 = private unnamed_addr constant [9 x i8] c"treflect\00", align 1

define arm_aapcscc i32 (...)* @edge_function(i8* %edges) nounwind uwtable {
entry:
  %retval = alloca i32 (...)*, align 4
  %edges.addr = alloca i8*, align 4
  %i = alloca i32, align 4
  store i8* %edges, i8** %edges.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %edges.addr}, metadata !57), !dbg !58
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !59), !dbg !61
  store i32 0, i32* %i, align 4, !dbg !62
  br label %for.cond, !dbg !62

for.cond:                                         ; preds = %for.inc, %entry
  %0 = load i32* %i, align 4, !dbg !62
  %cmp = icmp ult i32 %0, 9, !dbg !62
  br i1 %cmp, label %for.body, label %for.end, !dbg !62

for.body:                                         ; preds = %for.cond
  %1 = load i8** %edges.addr, align 4, !dbg !64
  %2 = load i32* %i, align 4, !dbg !64
  %arrayidx = getelementptr inbounds [9 x %struct.EDGE_HANDLER]* @edge_foos, i32 0, i32 %2, !dbg !64
  %name = getelementptr inbounds %struct.EDGE_HANDLER* %arrayidx, i32 0, i32 0, !dbg !64
  %3 = load i8** %name, align 4, !dbg !64
  %call = call arm_aapcscc  i32 bitcast (i32 (...)* @strcmp to i32 (i8*, i8*)*)(i8* %1, i8* %3), !dbg !64
  %tobool = icmp ne i32 %call, 0, !dbg !64
  br i1 %tobool, label %if.end, label %if.then, !dbg !64

if.then:                                          ; preds = %for.body
  %4 = load i32* %i, align 4, !dbg !65
  %arrayidx1 = getelementptr inbounds [9 x %struct.EDGE_HANDLER]* @edge_foos, i32 0, i32 %4, !dbg !65
  %func = getelementptr inbounds %struct.EDGE_HANDLER* %arrayidx1, i32 0, i32 1, !dbg !65
  %5 = load i32 (...)** %func, align 4, !dbg !65
  store i32 (...)* %5, i32 (...)** %retval, !dbg !65
  br label %return, !dbg !65

if.end:                                           ; preds = %for.body
  br label %for.inc, !dbg !65

for.inc:                                          ; preds = %if.end
  %6 = load i32* %i, align 4, !dbg !66
  %inc = add nsw i32 %6, 1, !dbg !66
  store i32 %inc, i32* %i, align 4, !dbg !66
  br label %for.cond, !dbg !66

for.end:                                          ; preds = %for.cond
  %call2 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([31 x i8]* @.str, i32 0, i32 0)), !dbg !67
  store i32 (...)* null, i32 (...)** %retval, !dbg !68
  br label %return, !dbg !68

return:                                           ; preds = %for.end, %if.then
  %7 = load i32 (...)** %retval, !dbg !69
  ret i32 (...)* %7, !dbg !69
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc i32 @strcmp(...)

declare arm_aapcscc i32 @printf(i8*, ...)

define arm_aapcscc i32 @zero(float* %filt, i32 %x_dim, i32 %y_dim, i32 %x_pos, i32 %y_pos, float* %result, i32 %f_or_e) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %filt.addr = alloca float*, align 4
  %x_dim.addr = alloca i32, align 4
  %y_dim.addr = alloca i32, align 4
  %x_pos.addr = alloca i32, align 4
  %y_pos.addr = alloca i32, align 4
  %result.addr = alloca float*, align 4
  %f_or_e.addr = alloca i32, align 4
  %y_filt = alloca i32, align 4
  %x_filt = alloca i32, align 4
  %y_res = alloca i32, align 4
  %x_res = alloca i32, align 4
  %filt_sz = alloca i32, align 4
  %x_start = alloca i32, align 4
  %y_start = alloca i32, align 4
  %i = alloca i32, align 4
  store float* %filt, float** %filt.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %filt.addr}, metadata !70), !dbg !73
  store i32 %x_dim, i32* %x_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_dim.addr}, metadata !74), !dbg !75
  store i32 %y_dim, i32* %y_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_dim.addr}, metadata !76), !dbg !77
  store i32 %x_pos, i32* %x_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_pos.addr}, metadata !78), !dbg !79
  store i32 %y_pos, i32* %y_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_pos.addr}, metadata !80), !dbg !81
  store float* %result, float** %result.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result.addr}, metadata !82), !dbg !83
  store i32 %f_or_e, i32* %f_or_e.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %f_or_e.addr}, metadata !84), !dbg !85
  call void @llvm.dbg.declare(metadata !{i32* %y_filt}, metadata !86), !dbg !88
  call void @llvm.dbg.declare(metadata !{i32* %x_filt}, metadata !89), !dbg !90
  call void @llvm.dbg.declare(metadata !{i32* %y_res}, metadata !91), !dbg !92
  call void @llvm.dbg.declare(metadata !{i32* %x_res}, metadata !93), !dbg !94
  call void @llvm.dbg.declare(metadata !{i32* %filt_sz}, metadata !95), !dbg !96
  %0 = load i32* %x_dim.addr, align 4, !dbg !97
  %1 = load i32* %y_dim.addr, align 4, !dbg !97
  %mul = mul nsw i32 %0, %1, !dbg !97
  store i32 %mul, i32* %filt_sz, align 4, !dbg !97
  call void @llvm.dbg.declare(metadata !{i32* %x_start}, metadata !98), !dbg !99
  %2 = load i32* %x_pos.addr, align 4, !dbg !100
  %cmp = icmp sgt i32 %2, 0, !dbg !100
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !100

cond.true:                                        ; preds = %entry
  %3 = load i32* %x_pos.addr, align 4, !dbg !100
  %sub = sub nsw i32 %3, 1, !dbg !100
  br label %cond.end4, !dbg !100

cond.false:                                       ; preds = %entry
  %4 = load i32* %x_pos.addr, align 4, !dbg !100
  %cmp1 = icmp slt i32 %4, 0, !dbg !100
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !100

cond.true2:                                       ; preds = %cond.false
  %5 = load i32* %x_pos.addr, align 4, !dbg !100
  %add = add nsw i32 %5, 1, !dbg !100
  br label %cond.end, !dbg !100

cond.false3:                                      ; preds = %cond.false
  br label %cond.end, !dbg !100

cond.end:                                         ; preds = %cond.false3, %cond.true2
  %cond = phi i32 [ %add, %cond.true2 ], [ 0, %cond.false3 ], !dbg !100
  br label %cond.end4, !dbg !100

cond.end4:                                        ; preds = %cond.end, %cond.true
  %cond5 = phi i32 [ %sub, %cond.true ], [ %cond, %cond.end ], !dbg !100
  store i32 %cond5, i32* %x_start, align 4, !dbg !100
  call void @llvm.dbg.declare(metadata !{i32* %y_start}, metadata !101), !dbg !102
  %6 = load i32* %x_dim.addr, align 4, !dbg !103
  %7 = load i32* %y_pos.addr, align 4, !dbg !103
  %cmp6 = icmp sgt i32 %7, 0, !dbg !103
  br i1 %cmp6, label %cond.true7, label %cond.false9, !dbg !103

cond.true7:                                       ; preds = %cond.end4
  %8 = load i32* %y_pos.addr, align 4, !dbg !103
  %sub8 = sub nsw i32 %8, 1, !dbg !103
  br label %cond.end16, !dbg !103

cond.false9:                                      ; preds = %cond.end4
  %9 = load i32* %y_pos.addr, align 4, !dbg !103
  %cmp10 = icmp slt i32 %9, 0, !dbg !103
  br i1 %cmp10, label %cond.true11, label %cond.false13, !dbg !103

cond.true11:                                      ; preds = %cond.false9
  %10 = load i32* %y_pos.addr, align 4, !dbg !103
  %add12 = add nsw i32 %10, 1, !dbg !103
  br label %cond.end14, !dbg !103

cond.false13:                                     ; preds = %cond.false9
  br label %cond.end14, !dbg !103

cond.end14:                                       ; preds = %cond.false13, %cond.true11
  %cond15 = phi i32 [ %add12, %cond.true11 ], [ 0, %cond.false13 ], !dbg !103
  br label %cond.end16, !dbg !103

cond.end16:                                       ; preds = %cond.end14, %cond.true7
  %cond17 = phi i32 [ %sub8, %cond.true7 ], [ %cond15, %cond.end14 ], !dbg !103
  %mul18 = mul nsw i32 %6, %cond17, !dbg !103
  store i32 %mul18, i32* %y_start, align 4, !dbg !103
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !104), !dbg !105
  store i32 0, i32* %i, align 4, !dbg !106
  br label %for.cond, !dbg !106

for.cond:                                         ; preds = %for.inc, %cond.end16
  %11 = load i32* %i, align 4, !dbg !106
  %12 = load i32* %filt_sz, align 4, !dbg !106
  %cmp19 = icmp slt i32 %11, %12, !dbg !106
  br i1 %cmp19, label %for.body, label %for.end, !dbg !106

for.body:                                         ; preds = %for.cond
  %13 = load i32* %i, align 4, !dbg !108
  %14 = load float** %result.addr, align 4, !dbg !108
  %arrayidx = getelementptr inbounds float* %14, i32 %13, !dbg !108
  store float 0.000000e+00, float* %arrayidx, !dbg !108
  br label %for.inc, !dbg !108

for.inc:                                          ; preds = %for.body
  %15 = load i32* %i, align 4, !dbg !109
  %inc = add nsw i32 %15, 1, !dbg !109
  store i32 %inc, i32* %i, align 4, !dbg !109
  br label %for.cond, !dbg !109

for.end:                                          ; preds = %for.cond
  store i32 0, i32* %y_filt, align 4, !dbg !110
  %16 = load i32* %y_start, align 4, !dbg !110
  store i32 %16, i32* %y_res, align 4, !dbg !110
  br label %for.cond20, !dbg !110

for.cond20:                                       ; preds = %for.inc41, %for.end
  %17 = load i32* %y_filt, align 4, !dbg !110
  %18 = load i32* %filt_sz, align 4, !dbg !110
  %cmp21 = icmp slt i32 %17, %18, !dbg !110
  br i1 %cmp21, label %for.body22, label %for.end44, !dbg !110

for.body22:                                       ; preds = %for.cond20
  %19 = load i32* %y_res, align 4, !dbg !112
  %cmp23 = icmp sge i32 %19, 0, !dbg !112
  br i1 %cmp23, label %land.lhs.true, label %if.end40, !dbg !112

land.lhs.true:                                    ; preds = %for.body22
  %20 = load i32* %y_res, align 4, !dbg !112
  %21 = load i32* %filt_sz, align 4, !dbg !112
  %cmp24 = icmp slt i32 %20, %21, !dbg !112
  br i1 %cmp24, label %if.then, label %if.end40, !dbg !112

if.then:                                          ; preds = %land.lhs.true
  %22 = load i32* %y_filt, align 4, !dbg !113
  store i32 %22, i32* %x_filt, align 4, !dbg !113
  %23 = load i32* %x_start, align 4, !dbg !113
  store i32 %23, i32* %x_res, align 4, !dbg !113
  br label %for.cond25, !dbg !113

for.cond25:                                       ; preds = %for.inc36, %if.then
  %24 = load i32* %x_filt, align 4, !dbg !113
  %25 = load i32* %y_filt, align 4, !dbg !113
  %26 = load i32* %x_dim.addr, align 4, !dbg !113
  %add26 = add nsw i32 %25, %26, !dbg !113
  %cmp27 = icmp slt i32 %24, %add26, !dbg !113
  br i1 %cmp27, label %for.body28, label %for.end39, !dbg !113

for.body28:                                       ; preds = %for.cond25
  %27 = load i32* %x_res, align 4, !dbg !115
  %cmp29 = icmp sge i32 %27, 0, !dbg !115
  br i1 %cmp29, label %land.lhs.true30, label %if.end, !dbg !115

land.lhs.true30:                                  ; preds = %for.body28
  %28 = load i32* %x_res, align 4, !dbg !115
  %29 = load i32* %x_dim.addr, align 4, !dbg !115
  %cmp31 = icmp slt i32 %28, %29, !dbg !115
  br i1 %cmp31, label %if.then32, label %if.end, !dbg !115

if.then32:                                        ; preds = %land.lhs.true30
  %30 = load i32* %x_filt, align 4, !dbg !116
  %31 = load float** %filt.addr, align 4, !dbg !116
  %arrayidx33 = getelementptr inbounds float* %31, i32 %30, !dbg !116
  %32 = load float* %arrayidx33, !dbg !116
  %33 = load i32* %y_res, align 4, !dbg !116
  %34 = load i32* %x_res, align 4, !dbg !116
  %add34 = add nsw i32 %33, %34, !dbg !116
  %35 = load float** %result.addr, align 4, !dbg !116
  %arrayidx35 = getelementptr inbounds float* %35, i32 %add34, !dbg !116
  store float %32, float* %arrayidx35, !dbg !116
  br label %if.end, !dbg !116

if.end:                                           ; preds = %if.then32, %land.lhs.true30, %for.body28
  br label %for.inc36, !dbg !116

for.inc36:                                        ; preds = %if.end
  %36 = load i32* %x_filt, align 4, !dbg !117
  %inc37 = add nsw i32 %36, 1, !dbg !117
  store i32 %inc37, i32* %x_filt, align 4, !dbg !117
  %37 = load i32* %x_res, align 4, !dbg !117
  %inc38 = add nsw i32 %37, 1, !dbg !117
  store i32 %inc38, i32* %x_res, align 4, !dbg !117
  br label %for.cond25, !dbg !117

for.end39:                                        ; preds = %for.cond25
  br label %if.end40, !dbg !118

if.end40:                                         ; preds = %for.end39, %land.lhs.true, %for.body22
  br label %for.inc41, !dbg !118

for.inc41:                                        ; preds = %if.end40
  %38 = load i32* %x_dim.addr, align 4, !dbg !119
  %39 = load i32* %y_filt, align 4, !dbg !119
  %add42 = add nsw i32 %39, %38, !dbg !119
  store i32 %add42, i32* %y_filt, align 4, !dbg !119
  %40 = load i32* %x_dim.addr, align 4, !dbg !119
  %41 = load i32* %y_res, align 4, !dbg !119
  %add43 = add nsw i32 %41, %40, !dbg !119
  store i32 %add43, i32* %y_res, align 4, !dbg !119
  br label %for.cond20, !dbg !119

for.end44:                                        ; preds = %for.cond20
  %42 = load i32* %retval, !dbg !120
  ret i32 %42, !dbg !120
}

define arm_aapcscc i32 @repeat(float* %filt, i32 %x_dim, i32 %y_dim, i32 %x_pos, i32 %y_pos, float* %result, i32 %f_or_e) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %filt.addr = alloca float*, align 4
  %x_dim.addr = alloca i32, align 4
  %y_dim.addr = alloca i32, align 4
  %x_pos.addr = alloca i32, align 4
  %y_pos.addr = alloca i32, align 4
  %result.addr = alloca float*, align 4
  %f_or_e.addr = alloca i32, align 4
  %y_filt = alloca i32, align 4
  %x_filt = alloca i32, align 4
  %y_res = alloca i32, align 4
  %x_res = alloca i32, align 4
  %filt_sz = alloca i32, align 4
  %x_start = alloca i32, align 4
  %y_start = alloca i32, align 4
  %i = alloca i32, align 4
  store float* %filt, float** %filt.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %filt.addr}, metadata !121), !dbg !122
  store i32 %x_dim, i32* %x_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_dim.addr}, metadata !123), !dbg !124
  store i32 %y_dim, i32* %y_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_dim.addr}, metadata !125), !dbg !126
  store i32 %x_pos, i32* %x_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_pos.addr}, metadata !127), !dbg !128
  store i32 %y_pos, i32* %y_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_pos.addr}, metadata !129), !dbg !130
  store float* %result, float** %result.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result.addr}, metadata !131), !dbg !132
  store i32 %f_or_e, i32* %f_or_e.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %f_or_e.addr}, metadata !133), !dbg !134
  call void @llvm.dbg.declare(metadata !{i32* %y_filt}, metadata !135), !dbg !137
  call void @llvm.dbg.declare(metadata !{i32* %x_filt}, metadata !138), !dbg !139
  call void @llvm.dbg.declare(metadata !{i32* %y_res}, metadata !140), !dbg !141
  call void @llvm.dbg.declare(metadata !{i32* %x_res}, metadata !142), !dbg !143
  call void @llvm.dbg.declare(metadata !{i32* %filt_sz}, metadata !144), !dbg !145
  %0 = load i32* %x_dim.addr, align 4, !dbg !146
  %1 = load i32* %y_dim.addr, align 4, !dbg !146
  %mul = mul nsw i32 %0, %1, !dbg !146
  store i32 %mul, i32* %filt_sz, align 4, !dbg !146
  call void @llvm.dbg.declare(metadata !{i32* %x_start}, metadata !147), !dbg !148
  %2 = load i32* %x_pos.addr, align 4, !dbg !149
  %cmp = icmp sgt i32 %2, 0, !dbg !149
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !149

cond.true:                                        ; preds = %entry
  %3 = load i32* %x_pos.addr, align 4, !dbg !149
  %sub = sub nsw i32 %3, 1, !dbg !149
  br label %cond.end4, !dbg !149

cond.false:                                       ; preds = %entry
  %4 = load i32* %x_pos.addr, align 4, !dbg !149
  %cmp1 = icmp slt i32 %4, 0, !dbg !149
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !149

cond.true2:                                       ; preds = %cond.false
  %5 = load i32* %x_pos.addr, align 4, !dbg !149
  %add = add nsw i32 %5, 1, !dbg !149
  br label %cond.end, !dbg !149

cond.false3:                                      ; preds = %cond.false
  br label %cond.end, !dbg !149

cond.end:                                         ; preds = %cond.false3, %cond.true2
  %cond = phi i32 [ %add, %cond.true2 ], [ 0, %cond.false3 ], !dbg !149
  br label %cond.end4, !dbg !149

cond.end4:                                        ; preds = %cond.end, %cond.true
  %cond5 = phi i32 [ %sub, %cond.true ], [ %cond, %cond.end ], !dbg !149
  store i32 %cond5, i32* %x_start, align 4, !dbg !149
  call void @llvm.dbg.declare(metadata !{i32* %y_start}, metadata !150), !dbg !151
  %6 = load i32* %x_dim.addr, align 4, !dbg !152
  %7 = load i32* %y_pos.addr, align 4, !dbg !152
  %cmp6 = icmp sgt i32 %7, 0, !dbg !152
  br i1 %cmp6, label %cond.true7, label %cond.false9, !dbg !152

cond.true7:                                       ; preds = %cond.end4
  %8 = load i32* %y_pos.addr, align 4, !dbg !152
  %sub8 = sub nsw i32 %8, 1, !dbg !152
  br label %cond.end16, !dbg !152

cond.false9:                                      ; preds = %cond.end4
  %9 = load i32* %y_pos.addr, align 4, !dbg !152
  %cmp10 = icmp slt i32 %9, 0, !dbg !152
  br i1 %cmp10, label %cond.true11, label %cond.false13, !dbg !152

cond.true11:                                      ; preds = %cond.false9
  %10 = load i32* %y_pos.addr, align 4, !dbg !152
  %add12 = add nsw i32 %10, 1, !dbg !152
  br label %cond.end14, !dbg !152

cond.false13:                                     ; preds = %cond.false9
  br label %cond.end14, !dbg !152

cond.end14:                                       ; preds = %cond.false13, %cond.true11
  %cond15 = phi i32 [ %add12, %cond.true11 ], [ 0, %cond.false13 ], !dbg !152
  br label %cond.end16, !dbg !152

cond.end16:                                       ; preds = %cond.end14, %cond.true7
  %cond17 = phi i32 [ %sub8, %cond.true7 ], [ %cond15, %cond.end14 ], !dbg !152
  %mul18 = mul nsw i32 %6, %cond17, !dbg !152
  store i32 %mul18, i32* %y_start, align 4, !dbg !152
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !153), !dbg !154
  store i32 0, i32* %i, align 4, !dbg !155
  br label %for.cond, !dbg !155

for.cond:                                         ; preds = %for.inc, %cond.end16
  %11 = load i32* %i, align 4, !dbg !155
  %12 = load i32* %filt_sz, align 4, !dbg !155
  %cmp19 = icmp slt i32 %11, %12, !dbg !155
  br i1 %cmp19, label %for.body, label %for.end, !dbg !155

for.body:                                         ; preds = %for.cond
  %13 = load i32* %i, align 4, !dbg !157
  %14 = load float** %result.addr, align 4, !dbg !157
  %arrayidx = getelementptr inbounds float* %14, i32 %13, !dbg !157
  store float 0.000000e+00, float* %arrayidx, !dbg !157
  br label %for.inc, !dbg !157

for.inc:                                          ; preds = %for.body
  %15 = load i32* %i, align 4, !dbg !158
  %inc = add nsw i32 %15, 1, !dbg !158
  store i32 %inc, i32* %i, align 4, !dbg !158
  br label %for.cond, !dbg !158

for.end:                                          ; preds = %for.cond
  store i32 0, i32* %y_filt, align 4, !dbg !159
  %16 = load i32* %y_start, align 4, !dbg !159
  store i32 %16, i32* %y_res, align 4, !dbg !159
  br label %for.cond20, !dbg !159

for.cond20:                                       ; preds = %for.inc57, %for.end
  %17 = load i32* %y_filt, align 4, !dbg !159
  %18 = load i32* %filt_sz, align 4, !dbg !159
  %cmp21 = icmp slt i32 %17, %18, !dbg !159
  br i1 %cmp21, label %for.body22, label %for.end60, !dbg !159

for.body22:                                       ; preds = %for.cond20
  %19 = load i32* %y_filt, align 4, !dbg !161
  store i32 %19, i32* %x_filt, align 4, !dbg !161
  %20 = load i32* %x_start, align 4, !dbg !161
  store i32 %20, i32* %x_res, align 4, !dbg !161
  br label %for.cond23, !dbg !161

for.cond23:                                       ; preds = %for.inc53, %for.body22
  %21 = load i32* %x_filt, align 4, !dbg !161
  %22 = load i32* %y_filt, align 4, !dbg !161
  %23 = load i32* %x_dim.addr, align 4, !dbg !161
  %add24 = add nsw i32 %22, %23, !dbg !161
  %cmp25 = icmp slt i32 %21, %add24, !dbg !161
  br i1 %cmp25, label %for.body26, label %for.end56, !dbg !161

for.body26:                                       ; preds = %for.cond23
  %24 = load i32* %x_filt, align 4, !dbg !163
  %25 = load float** %filt.addr, align 4, !dbg !163
  %arrayidx27 = getelementptr inbounds float* %25, i32 %24, !dbg !163
  %26 = load float* %arrayidx27, !dbg !163
  %27 = load i32* %y_res, align 4, !dbg !163
  %cmp28 = icmp sge i32 %27, 0, !dbg !163
  br i1 %cmp28, label %cond.true29, label %cond.false36, !dbg !163

cond.true29:                                      ; preds = %for.body26
  %28 = load i32* %y_res, align 4, !dbg !163
  %29 = load i32* %filt_sz, align 4, !dbg !163
  %cmp30 = icmp slt i32 %28, %29, !dbg !163
  br i1 %cmp30, label %cond.true31, label %cond.false32, !dbg !163

cond.true31:                                      ; preds = %cond.true29
  %30 = load i32* %y_res, align 4, !dbg !163
  br label %cond.end34, !dbg !163

cond.false32:                                     ; preds = %cond.true29
  %31 = load i32* %filt_sz, align 4, !dbg !163
  %32 = load i32* %x_dim.addr, align 4, !dbg !163
  %sub33 = sub nsw i32 %31, %32, !dbg !163
  br label %cond.end34, !dbg !163

cond.end34:                                       ; preds = %cond.false32, %cond.true31
  %cond35 = phi i32 [ %30, %cond.true31 ], [ %sub33, %cond.false32 ], !dbg !163
  br label %cond.end37, !dbg !163

cond.false36:                                     ; preds = %for.body26
  br label %cond.end37, !dbg !163

cond.end37:                                       ; preds = %cond.false36, %cond.end34
  %cond38 = phi i32 [ %cond35, %cond.end34 ], [ 0, %cond.false36 ], !dbg !163
  %33 = load i32* %x_res, align 4, !dbg !163
  %cmp39 = icmp sge i32 %33, 0, !dbg !163
  br i1 %cmp39, label %cond.true40, label %cond.false47, !dbg !163

cond.true40:                                      ; preds = %cond.end37
  %34 = load i32* %x_res, align 4, !dbg !163
  %35 = load i32* %x_dim.addr, align 4, !dbg !163
  %cmp41 = icmp slt i32 %34, %35, !dbg !163
  br i1 %cmp41, label %cond.true42, label %cond.false43, !dbg !163

cond.true42:                                      ; preds = %cond.true40
  %36 = load i32* %x_res, align 4, !dbg !163
  br label %cond.end45, !dbg !163

cond.false43:                                     ; preds = %cond.true40
  %37 = load i32* %x_dim.addr, align 4, !dbg !163
  %sub44 = sub nsw i32 %37, 1, !dbg !163
  br label %cond.end45, !dbg !163

cond.end45:                                       ; preds = %cond.false43, %cond.true42
  %cond46 = phi i32 [ %36, %cond.true42 ], [ %sub44, %cond.false43 ], !dbg !163
  br label %cond.end48, !dbg !163

cond.false47:                                     ; preds = %cond.end37
  br label %cond.end48, !dbg !163

cond.end48:                                       ; preds = %cond.false47, %cond.end45
  %cond49 = phi i32 [ %cond46, %cond.end45 ], [ 0, %cond.false47 ], !dbg !163
  %add50 = add nsw i32 %cond38, %cond49, !dbg !163
  %38 = load float** %result.addr, align 4, !dbg !163
  %arrayidx51 = getelementptr inbounds float* %38, i32 %add50, !dbg !163
  %39 = load float* %arrayidx51, !dbg !163
  %add52 = fadd float %39, %26, !dbg !163
  store float %add52, float* %arrayidx51, !dbg !163
  br label %for.inc53, !dbg !163

for.inc53:                                        ; preds = %cond.end48
  %40 = load i32* %x_filt, align 4, !dbg !164
  %inc54 = add nsw i32 %40, 1, !dbg !164
  store i32 %inc54, i32* %x_filt, align 4, !dbg !164
  %41 = load i32* %x_res, align 4, !dbg !164
  %inc55 = add nsw i32 %41, 1, !dbg !164
  store i32 %inc55, i32* %x_res, align 4, !dbg !164
  br label %for.cond23, !dbg !164

for.end56:                                        ; preds = %for.cond23
  br label %for.inc57, !dbg !165

for.inc57:                                        ; preds = %for.end56
  %42 = load i32* %x_dim.addr, align 4, !dbg !166
  %43 = load i32* %y_filt, align 4, !dbg !166
  %add58 = add nsw i32 %43, %42, !dbg !166
  store i32 %add58, i32* %y_filt, align 4, !dbg !166
  %44 = load i32* %x_dim.addr, align 4, !dbg !166
  %45 = load i32* %y_res, align 4, !dbg !166
  %add59 = add nsw i32 %45, %44, !dbg !166
  store i32 %add59, i32* %y_res, align 4, !dbg !166
  br label %for.cond20, !dbg !166

for.end60:                                        ; preds = %for.cond20
  %46 = load i32* %retval, !dbg !167
  ret i32 %46, !dbg !167
}

define arm_aapcscc i32 @reflect2(float* %filt, i32 %x_dim, i32 %y_dim, i32 %x_pos, i32 %y_pos, float* %result, i32 %f_or_e) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %filt.addr = alloca float*, align 4
  %x_dim.addr = alloca i32, align 4
  %y_dim.addr = alloca i32, align 4
  %x_pos.addr = alloca i32, align 4
  %y_pos.addr = alloca i32, align 4
  %result.addr = alloca float*, align 4
  %f_or_e.addr = alloca i32, align 4
  %y_filt = alloca i32, align 4
  %x_filt = alloca i32, align 4
  %y_edge = alloca i32, align 4
  %x_edge = alloca i32, align 4
  %x_base = alloca i32, align 4
  %y_base = alloca i32, align 4
  %filt_sz = alloca i32, align 4
  %x_edge_dist = alloca i32, align 4
  %y_edge_dist = alloca i32, align 4
  %i = alloca i32, align 4
  store float* %filt, float** %filt.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %filt.addr}, metadata !168), !dbg !169
  store i32 %x_dim, i32* %x_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_dim.addr}, metadata !170), !dbg !171
  store i32 %y_dim, i32* %y_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_dim.addr}, metadata !172), !dbg !173
  store i32 %x_pos, i32* %x_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_pos.addr}, metadata !174), !dbg !175
  store i32 %y_pos, i32* %y_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_pos.addr}, metadata !176), !dbg !177
  store float* %result, float** %result.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result.addr}, metadata !178), !dbg !179
  store i32 %f_or_e, i32* %f_or_e.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %f_or_e.addr}, metadata !180), !dbg !181
  call void @llvm.dbg.declare(metadata !{i32* %y_filt}, metadata !182), !dbg !184
  call void @llvm.dbg.declare(metadata !{i32* %x_filt}, metadata !185), !dbg !186
  call void @llvm.dbg.declare(metadata !{i32* %y_edge}, metadata !187), !dbg !188
  call void @llvm.dbg.declare(metadata !{i32* %x_edge}, metadata !189), !dbg !190
  call void @llvm.dbg.declare(metadata !{i32* %x_base}, metadata !191), !dbg !192
  %0 = load i32* %x_pos.addr, align 4, !dbg !193
  %cmp = icmp sgt i32 %0, 0, !dbg !193
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !193

cond.true:                                        ; preds = %entry
  %1 = load i32* %x_dim.addr, align 4, !dbg !193
  %sub = sub nsw i32 %1, 1, !dbg !193
  br label %cond.end, !dbg !193

cond.false:                                       ; preds = %entry
  br label %cond.end, !dbg !193

cond.end:                                         ; preds = %cond.false, %cond.true
  %cond = phi i32 [ %sub, %cond.true ], [ 0, %cond.false ], !dbg !193
  store i32 %cond, i32* %x_base, align 4, !dbg !193
  call void @llvm.dbg.declare(metadata !{i32* %y_base}, metadata !194), !dbg !195
  %2 = load i32* %y_pos.addr, align 4, !dbg !196
  %cmp1 = icmp sgt i32 %2, 0, !dbg !196
  br i1 %cmp1, label %cond.true2, label %cond.false4, !dbg !196

cond.true2:                                       ; preds = %cond.end
  %3 = load i32* %x_dim.addr, align 4, !dbg !196
  %4 = load i32* %y_dim.addr, align 4, !dbg !196
  %sub3 = sub nsw i32 %4, 1, !dbg !196
  %mul = mul nsw i32 %3, %sub3, !dbg !196
  br label %cond.end5, !dbg !196

cond.false4:                                      ; preds = %cond.end
  br label %cond.end5, !dbg !196

cond.end5:                                        ; preds = %cond.false4, %cond.true2
  %cond6 = phi i32 [ %mul, %cond.true2 ], [ 0, %cond.false4 ], !dbg !196
  store i32 %cond6, i32* %y_base, align 4, !dbg !196
  call void @llvm.dbg.declare(metadata !{i32* %filt_sz}, metadata !197), !dbg !198
  %5 = load i32* %x_dim.addr, align 4, !dbg !199
  %6 = load i32* %y_dim.addr, align 4, !dbg !199
  %mul7 = mul nsw i32 %5, %6, !dbg !199
  store i32 %mul7, i32* %filt_sz, align 4, !dbg !199
  call void @llvm.dbg.declare(metadata !{i32* %x_edge_dist}, metadata !200), !dbg !201
  %7 = load i32* %x_pos.addr, align 4, !dbg !202
  %cmp8 = icmp sgt i32 %7, 0, !dbg !202
  br i1 %cmp8, label %cond.true9, label %cond.false12, !dbg !202

cond.true9:                                       ; preds = %cond.end5
  %8 = load i32* %x_pos.addr, align 4, !dbg !202
  %9 = load i32* %x_dim.addr, align 4, !dbg !202
  %sub10 = sub nsw i32 %8, %9, !dbg !202
  %sub11 = sub nsw i32 %sub10, 1, !dbg !202
  br label %cond.end13, !dbg !202

cond.false12:                                     ; preds = %cond.end5
  %10 = load i32* %x_pos.addr, align 4, !dbg !202
  %add = add nsw i32 %10, 1, !dbg !202
  br label %cond.end13, !dbg !202

cond.end13:                                       ; preds = %cond.false12, %cond.true9
  %cond14 = phi i32 [ %sub11, %cond.true9 ], [ %add, %cond.false12 ], !dbg !202
  store i32 %cond14, i32* %x_edge_dist, align 4, !dbg !202
  call void @llvm.dbg.declare(metadata !{i32* %y_edge_dist}, metadata !203), !dbg !204
  %11 = load i32* %x_dim.addr, align 4, !dbg !205
  %12 = load i32* %y_pos.addr, align 4, !dbg !205
  %cmp15 = icmp sgt i32 %12, 0, !dbg !205
  br i1 %cmp15, label %cond.true16, label %cond.false19, !dbg !205

cond.true16:                                      ; preds = %cond.end13
  %13 = load i32* %y_pos.addr, align 4, !dbg !205
  %14 = load i32* %y_dim.addr, align 4, !dbg !205
  %sub17 = sub nsw i32 %13, %14, !dbg !205
  %sub18 = sub nsw i32 %sub17, 1, !dbg !205
  br label %cond.end21, !dbg !205

cond.false19:                                     ; preds = %cond.end13
  %15 = load i32* %y_pos.addr, align 4, !dbg !205
  %add20 = add nsw i32 %15, 1, !dbg !205
  br label %cond.end21, !dbg !205

cond.end21:                                       ; preds = %cond.false19, %cond.true16
  %cond22 = phi i32 [ %sub18, %cond.true16 ], [ %add20, %cond.false19 ], !dbg !205
  %mul23 = mul nsw i32 %11, %cond22, !dbg !205
  store i32 %mul23, i32* %y_edge_dist, align 4, !dbg !205
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !206), !dbg !207
  store i32 0, i32* %i, align 4, !dbg !208
  br label %for.cond, !dbg !208

for.cond:                                         ; preds = %for.inc, %cond.end21
  %16 = load i32* %i, align 4, !dbg !208
  %17 = load i32* %filt_sz, align 4, !dbg !208
  %cmp24 = icmp slt i32 %16, %17, !dbg !208
  br i1 %cmp24, label %for.body, label %for.end, !dbg !208

for.body:                                         ; preds = %for.cond
  %18 = load i32* %i, align 4, !dbg !210
  %19 = load float** %result.addr, align 4, !dbg !210
  %arrayidx = getelementptr inbounds float* %19, i32 %18, !dbg !210
  store float 0.000000e+00, float* %arrayidx, !dbg !210
  br label %for.inc, !dbg !210

for.inc:                                          ; preds = %for.body
  %20 = load i32* %i, align 4, !dbg !211
  %inc = add nsw i32 %20, 1, !dbg !211
  store i32 %inc, i32* %i, align 4, !dbg !211
  br label %for.cond, !dbg !211

for.end:                                          ; preds = %for.cond
  store i32 0, i32* %y_filt, align 4, !dbg !212
  %21 = load i32* %y_edge_dist, align 4, !dbg !212
  store i32 %21, i32* %y_edge, align 4, !dbg !212
  br label %for.cond25, !dbg !212

for.cond25:                                       ; preds = %for.inc106, %for.end
  %22 = load i32* %y_filt, align 4, !dbg !212
  %23 = load i32* %filt_sz, align 4, !dbg !212
  %cmp26 = icmp slt i32 %22, %23, !dbg !212
  br i1 %cmp26, label %for.body27, label %for.end109, !dbg !212

for.body27:                                       ; preds = %for.cond25
  %24 = load i32* %y_edge, align 4, !dbg !214
  %cmp28 = icmp eq i32 %24, 0, !dbg !214
  br i1 %cmp28, label %if.then, label %if.end, !dbg !214

if.then:                                          ; preds = %for.body27
  %25 = load i32* %x_dim.addr, align 4, !dbg !216
  %26 = load i32* %y_edge, align 4, !dbg !216
  %add29 = add nsw i32 %26, %25, !dbg !216
  store i32 %add29, i32* %y_edge, align 4, !dbg !216
  br label %if.end, !dbg !216

if.end:                                           ; preds = %if.then, %for.body27
  %27 = load i32* %y_filt, align 4, !dbg !217
  store i32 %27, i32* %x_filt, align 4, !dbg !217
  %28 = load i32* %x_edge_dist, align 4, !dbg !217
  store i32 %28, i32* %x_edge, align 4, !dbg !217
  br label %for.cond30, !dbg !217

for.cond30:                                       ; preds = %for.inc102, %if.end
  %29 = load i32* %x_filt, align 4, !dbg !217
  %30 = load i32* %y_filt, align 4, !dbg !217
  %31 = load i32* %x_dim.addr, align 4, !dbg !217
  %add31 = add nsw i32 %30, %31, !dbg !217
  %cmp32 = icmp slt i32 %29, %add31, !dbg !217
  br i1 %cmp32, label %for.body33, label %for.end105, !dbg !217

for.body33:                                       ; preds = %for.cond30
  %32 = load i32* %x_edge, align 4, !dbg !219
  %cmp34 = icmp eq i32 %32, 0, !dbg !219
  br i1 %cmp34, label %if.then35, label %if.end37, !dbg !219

if.then35:                                        ; preds = %for.body33
  %33 = load i32* %x_edge, align 4, !dbg !221
  %inc36 = add nsw i32 %33, 1, !dbg !221
  store i32 %inc36, i32* %x_edge, align 4, !dbg !221
  br label %if.end37, !dbg !221

if.end37:                                         ; preds = %if.then35, %for.body33
  %34 = load i32* %x_filt, align 4, !dbg !222
  %35 = load float** %filt.addr, align 4, !dbg !222
  %arrayidx38 = getelementptr inbounds float* %35, i32 %34, !dbg !222
  %36 = load float* %arrayidx38, !dbg !222
  %37 = load i32* %y_base, align 4, !dbg !222
  %38 = load i32* %y_edge, align 4, !dbg !222
  %cmp39 = icmp sge i32 %38, 0, !dbg !222
  br i1 %cmp39, label %cond.true40, label %cond.false41, !dbg !222

cond.true40:                                      ; preds = %if.end37
  %39 = load i32* %y_edge, align 4, !dbg !222
  br label %cond.end43, !dbg !222

cond.false41:                                     ; preds = %if.end37
  %40 = load i32* %y_edge, align 4, !dbg !222
  %sub42 = sub nsw i32 0, %40, !dbg !222
  br label %cond.end43, !dbg !222

cond.end43:                                       ; preds = %cond.false41, %cond.true40
  %cond44 = phi i32 [ %39, %cond.true40 ], [ %sub42, %cond.false41 ], !dbg !222
  %sub45 = sub nsw i32 %37, %cond44, !dbg !222
  %41 = load i32* %x_dim.addr, align 4, !dbg !222
  %add46 = add nsw i32 %sub45, %41, !dbg !222
  %cmp47 = icmp sge i32 %add46, 0, !dbg !222
  br i1 %cmp47, label %cond.true48, label %cond.false57, !dbg !222

cond.true48:                                      ; preds = %cond.end43
  %42 = load i32* %y_base, align 4, !dbg !222
  %43 = load i32* %y_edge, align 4, !dbg !222
  %cmp49 = icmp sge i32 %43, 0, !dbg !222
  br i1 %cmp49, label %cond.true50, label %cond.false51, !dbg !222

cond.true50:                                      ; preds = %cond.true48
  %44 = load i32* %y_edge, align 4, !dbg !222
  br label %cond.end53, !dbg !222

cond.false51:                                     ; preds = %cond.true48
  %45 = load i32* %y_edge, align 4, !dbg !222
  %sub52 = sub nsw i32 0, %45, !dbg !222
  br label %cond.end53, !dbg !222

cond.end53:                                       ; preds = %cond.false51, %cond.true50
  %cond54 = phi i32 [ %44, %cond.true50 ], [ %sub52, %cond.false51 ], !dbg !222
  %sub55 = sub nsw i32 %42, %cond54, !dbg !222
  %46 = load i32* %x_dim.addr, align 4, !dbg !222
  %add56 = add nsw i32 %sub55, %46, !dbg !222
  br label %cond.end67, !dbg !222

cond.false57:                                     ; preds = %cond.end43
  %47 = load i32* %y_base, align 4, !dbg !222
  %48 = load i32* %y_edge, align 4, !dbg !222
  %cmp58 = icmp sge i32 %48, 0, !dbg !222
  br i1 %cmp58, label %cond.true59, label %cond.false60, !dbg !222

cond.true59:                                      ; preds = %cond.false57
  %49 = load i32* %y_edge, align 4, !dbg !222
  br label %cond.end62, !dbg !222

cond.false60:                                     ; preds = %cond.false57
  %50 = load i32* %y_edge, align 4, !dbg !222
  %sub61 = sub nsw i32 0, %50, !dbg !222
  br label %cond.end62, !dbg !222

cond.end62:                                       ; preds = %cond.false60, %cond.true59
  %cond63 = phi i32 [ %49, %cond.true59 ], [ %sub61, %cond.false60 ], !dbg !222
  %sub64 = sub nsw i32 %47, %cond63, !dbg !222
  %51 = load i32* %x_dim.addr, align 4, !dbg !222
  %add65 = add nsw i32 %sub64, %51, !dbg !222
  %sub66 = sub nsw i32 0, %add65, !dbg !222
  br label %cond.end67, !dbg !222

cond.end67:                                       ; preds = %cond.end62, %cond.end53
  %cond68 = phi i32 [ %add56, %cond.end53 ], [ %sub66, %cond.end62 ], !dbg !222
  %52 = load i32* %x_base, align 4, !dbg !222
  %53 = load i32* %x_edge, align 4, !dbg !222
  %cmp69 = icmp sge i32 %53, 0, !dbg !222
  br i1 %cmp69, label %cond.true70, label %cond.false71, !dbg !222

cond.true70:                                      ; preds = %cond.end67
  %54 = load i32* %x_edge, align 4, !dbg !222
  br label %cond.end73, !dbg !222

cond.false71:                                     ; preds = %cond.end67
  %55 = load i32* %x_edge, align 4, !dbg !222
  %sub72 = sub nsw i32 0, %55, !dbg !222
  br label %cond.end73, !dbg !222

cond.end73:                                       ; preds = %cond.false71, %cond.true70
  %cond74 = phi i32 [ %54, %cond.true70 ], [ %sub72, %cond.false71 ], !dbg !222
  %sub75 = sub nsw i32 %52, %cond74, !dbg !222
  %add76 = add nsw i32 %sub75, 1, !dbg !222
  %cmp77 = icmp sge i32 %add76, 0, !dbg !222
  br i1 %cmp77, label %cond.true78, label %cond.false87, !dbg !222

cond.true78:                                      ; preds = %cond.end73
  %56 = load i32* %x_base, align 4, !dbg !222
  %57 = load i32* %x_edge, align 4, !dbg !222
  %cmp79 = icmp sge i32 %57, 0, !dbg !222
  br i1 %cmp79, label %cond.true80, label %cond.false81, !dbg !222

cond.true80:                                      ; preds = %cond.true78
  %58 = load i32* %x_edge, align 4, !dbg !222
  br label %cond.end83, !dbg !222

cond.false81:                                     ; preds = %cond.true78
  %59 = load i32* %x_edge, align 4, !dbg !222
  %sub82 = sub nsw i32 0, %59, !dbg !222
  br label %cond.end83, !dbg !222

cond.end83:                                       ; preds = %cond.false81, %cond.true80
  %cond84 = phi i32 [ %58, %cond.true80 ], [ %sub82, %cond.false81 ], !dbg !222
  %sub85 = sub nsw i32 %56, %cond84, !dbg !222
  %add86 = add nsw i32 %sub85, 1, !dbg !222
  br label %cond.end97, !dbg !222

cond.false87:                                     ; preds = %cond.end73
  %60 = load i32* %x_base, align 4, !dbg !222
  %61 = load i32* %x_edge, align 4, !dbg !222
  %cmp88 = icmp sge i32 %61, 0, !dbg !222
  br i1 %cmp88, label %cond.true89, label %cond.false90, !dbg !222

cond.true89:                                      ; preds = %cond.false87
  %62 = load i32* %x_edge, align 4, !dbg !222
  br label %cond.end92, !dbg !222

cond.false90:                                     ; preds = %cond.false87
  %63 = load i32* %x_edge, align 4, !dbg !222
  %sub91 = sub nsw i32 0, %63, !dbg !222
  br label %cond.end92, !dbg !222

cond.end92:                                       ; preds = %cond.false90, %cond.true89
  %cond93 = phi i32 [ %62, %cond.true89 ], [ %sub91, %cond.false90 ], !dbg !222
  %sub94 = sub nsw i32 %60, %cond93, !dbg !222
  %add95 = add nsw i32 %sub94, 1, !dbg !222
  %sub96 = sub nsw i32 0, %add95, !dbg !222
  br label %cond.end97, !dbg !222

cond.end97:                                       ; preds = %cond.end92, %cond.end83
  %cond98 = phi i32 [ %add86, %cond.end83 ], [ %sub96, %cond.end92 ], !dbg !222
  %add99 = add nsw i32 %cond68, %cond98, !dbg !222
  %64 = load float** %result.addr, align 4, !dbg !222
  %arrayidx100 = getelementptr inbounds float* %64, i32 %add99, !dbg !222
  %65 = load float* %arrayidx100, !dbg !222
  %add101 = fadd float %65, %36, !dbg !222
  store float %add101, float* %arrayidx100, !dbg !222
  br label %for.inc102, !dbg !223

for.inc102:                                       ; preds = %cond.end97
  %66 = load i32* %x_filt, align 4, !dbg !224
  %inc103 = add nsw i32 %66, 1, !dbg !224
  store i32 %inc103, i32* %x_filt, align 4, !dbg !224
  %67 = load i32* %x_edge, align 4, !dbg !224
  %inc104 = add nsw i32 %67, 1, !dbg !224
  store i32 %inc104, i32* %x_edge, align 4, !dbg !224
  br label %for.cond30, !dbg !224

for.end105:                                       ; preds = %for.cond30
  br label %for.inc106, !dbg !225

for.inc106:                                       ; preds = %for.end105
  %68 = load i32* %x_dim.addr, align 4, !dbg !226
  %69 = load i32* %y_filt, align 4, !dbg !226
  %add107 = add nsw i32 %69, %68, !dbg !226
  store i32 %add107, i32* %y_filt, align 4, !dbg !226
  %70 = load i32* %x_dim.addr, align 4, !dbg !226
  %71 = load i32* %y_edge, align 4, !dbg !226
  %add108 = add nsw i32 %71, %70, !dbg !226
  store i32 %add108, i32* %y_edge, align 4, !dbg !226
  br label %for.cond25, !dbg !226

for.end109:                                       ; preds = %for.cond25
  %72 = load i32* %retval, !dbg !227
  ret i32 %72, !dbg !227
}

define arm_aapcscc i32 @reflect1(float* %filt, i32 %x_dim, i32 %y_dim, i32 %x_pos, i32 %y_pos, float* %result, i32 %f_or_e) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %filt.addr = alloca float*, align 4
  %x_dim.addr = alloca i32, align 4
  %y_dim.addr = alloca i32, align 4
  %x_pos.addr = alloca i32, align 4
  %y_pos.addr = alloca i32, align 4
  %result.addr = alloca float*, align 4
  %f_or_e.addr = alloca i32, align 4
  %filt_sz = alloca i32, align 4
  %x_start = alloca i32, align 4
  %y_start = alloca i32, align 4
  %x_stop = alloca i32, align 4
  %y_stop = alloca i32, align 4
  %y_filt = alloca i32, align 4
  %x_filt = alloca i32, align 4
  %y_edge = alloca i32, align 4
  %x_edge = alloca i32, align 4
  %x_base = alloca i32, align 4
  %y_base = alloca i32, align 4
  %x_edge_dist = alloca i32, align 4
  %y_edge_dist = alloca i32, align 4
  %i = alloca i32, align 4
  %mx_pos = alloca i32, align 4
  %my_pos = alloca i32, align 4
  store float* %filt, float** %filt.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %filt.addr}, metadata !228), !dbg !229
  store i32 %x_dim, i32* %x_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_dim.addr}, metadata !230), !dbg !231
  store i32 %y_dim, i32* %y_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_dim.addr}, metadata !232), !dbg !233
  store i32 %x_pos, i32* %x_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_pos.addr}, metadata !234), !dbg !235
  store i32 %y_pos, i32* %y_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_pos.addr}, metadata !236), !dbg !237
  store float* %result, float** %result.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result.addr}, metadata !238), !dbg !239
  store i32 %f_or_e, i32* %f_or_e.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %f_or_e.addr}, metadata !240), !dbg !241
  call void @llvm.dbg.declare(metadata !{i32* %filt_sz}, metadata !242), !dbg !244
  %0 = load i32* %x_dim.addr, align 4, !dbg !245
  %1 = load i32* %y_dim.addr, align 4, !dbg !245
  %mul = mul nsw i32 %0, %1, !dbg !245
  store i32 %mul, i32* %filt_sz, align 4, !dbg !245
  call void @llvm.dbg.declare(metadata !{i32* %x_start}, metadata !246), !dbg !247
  store i32 0, i32* %x_start, align 4, !dbg !248
  call void @llvm.dbg.declare(metadata !{i32* %y_start}, metadata !249), !dbg !250
  store i32 0, i32* %y_start, align 4, !dbg !248
  call void @llvm.dbg.declare(metadata !{i32* %x_stop}, metadata !251), !dbg !252
  %2 = load i32* %x_dim.addr, align 4, !dbg !248
  store i32 %2, i32* %x_stop, align 4, !dbg !248
  call void @llvm.dbg.declare(metadata !{i32* %y_stop}, metadata !253), !dbg !254
  %3 = load i32* %filt_sz, align 4, !dbg !248
  store i32 %3, i32* %y_stop, align 4, !dbg !248
  call void @llvm.dbg.declare(metadata !{i32* %y_filt}, metadata !255), !dbg !256
  call void @llvm.dbg.declare(metadata !{i32* %x_filt}, metadata !257), !dbg !258
  call void @llvm.dbg.declare(metadata !{i32* %y_edge}, metadata !259), !dbg !260
  call void @llvm.dbg.declare(metadata !{i32* %x_edge}, metadata !261), !dbg !262
  call void @llvm.dbg.declare(metadata !{i32* %x_base}, metadata !263), !dbg !264
  %4 = load i32* %x_pos.addr, align 4, !dbg !265
  %cmp = icmp sgt i32 %4, 0, !dbg !265
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !265

cond.true:                                        ; preds = %entry
  %5 = load i32* %x_dim.addr, align 4, !dbg !265
  %sub = sub nsw i32 %5, 1, !dbg !265
  br label %cond.end, !dbg !265

cond.false:                                       ; preds = %entry
  br label %cond.end, !dbg !265

cond.end:                                         ; preds = %cond.false, %cond.true
  %cond = phi i32 [ %sub, %cond.true ], [ 0, %cond.false ], !dbg !265
  store i32 %cond, i32* %x_base, align 4, !dbg !265
  call void @llvm.dbg.declare(metadata !{i32* %y_base}, metadata !266), !dbg !267
  %6 = load i32* %y_pos.addr, align 4, !dbg !268
  %cmp1 = icmp sgt i32 %6, 0, !dbg !268
  br i1 %cmp1, label %cond.true2, label %cond.false5, !dbg !268

cond.true2:                                       ; preds = %cond.end
  %7 = load i32* %x_dim.addr, align 4, !dbg !268
  %8 = load i32* %y_dim.addr, align 4, !dbg !268
  %sub3 = sub nsw i32 %8, 1, !dbg !268
  %mul4 = mul nsw i32 %7, %sub3, !dbg !268
  br label %cond.end6, !dbg !268

cond.false5:                                      ; preds = %cond.end
  br label %cond.end6, !dbg !268

cond.end6:                                        ; preds = %cond.false5, %cond.true2
  %cond7 = phi i32 [ %mul4, %cond.true2 ], [ 0, %cond.false5 ], !dbg !268
  store i32 %cond7, i32* %y_base, align 4, !dbg !268
  call void @llvm.dbg.declare(metadata !{i32* %x_edge_dist}, metadata !269), !dbg !270
  %9 = load i32* %x_pos.addr, align 4, !dbg !271
  %cmp8 = icmp sgt i32 %9, 0, !dbg !271
  br i1 %cmp8, label %cond.true9, label %cond.false11, !dbg !271

cond.true9:                                       ; preds = %cond.end6
  %10 = load i32* %x_pos.addr, align 4, !dbg !271
  %11 = load i32* %x_dim.addr, align 4, !dbg !271
  %sub10 = sub nsw i32 %10, %11, !dbg !271
  br label %cond.end17, !dbg !271

cond.false11:                                     ; preds = %cond.end6
  %12 = load i32* %x_pos.addr, align 4, !dbg !271
  %cmp12 = icmp slt i32 %12, -1, !dbg !271
  br i1 %cmp12, label %cond.true13, label %cond.false14, !dbg !271

cond.true13:                                      ; preds = %cond.false11
  %13 = load i32* %x_pos.addr, align 4, !dbg !271
  %add = add nsw i32 %13, 1, !dbg !271
  br label %cond.end15, !dbg !271

cond.false14:                                     ; preds = %cond.false11
  br label %cond.end15, !dbg !271

cond.end15:                                       ; preds = %cond.false14, %cond.true13
  %cond16 = phi i32 [ %add, %cond.true13 ], [ 0, %cond.false14 ], !dbg !271
  br label %cond.end17, !dbg !271

cond.end17:                                       ; preds = %cond.end15, %cond.true9
  %cond18 = phi i32 [ %sub10, %cond.true9 ], [ %cond16, %cond.end15 ], !dbg !271
  store i32 %cond18, i32* %x_edge_dist, align 4, !dbg !271
  call void @llvm.dbg.declare(metadata !{i32* %y_edge_dist}, metadata !272), !dbg !273
  %14 = load i32* %x_dim.addr, align 4, !dbg !274
  %15 = load i32* %y_pos.addr, align 4, !dbg !274
  %cmp19 = icmp sgt i32 %15, 0, !dbg !274
  br i1 %cmp19, label %cond.true20, label %cond.false22, !dbg !274

cond.true20:                                      ; preds = %cond.end17
  %16 = load i32* %y_pos.addr, align 4, !dbg !274
  %17 = load i32* %y_dim.addr, align 4, !dbg !274
  %sub21 = sub nsw i32 %16, %17, !dbg !274
  br label %cond.end29, !dbg !274

cond.false22:                                     ; preds = %cond.end17
  %18 = load i32* %y_pos.addr, align 4, !dbg !274
  %cmp23 = icmp slt i32 %18, -1, !dbg !274
  br i1 %cmp23, label %cond.true24, label %cond.false26, !dbg !274

cond.true24:                                      ; preds = %cond.false22
  %19 = load i32* %y_pos.addr, align 4, !dbg !274
  %add25 = add nsw i32 %19, 1, !dbg !274
  br label %cond.end27, !dbg !274

cond.false26:                                     ; preds = %cond.false22
  br label %cond.end27, !dbg !274

cond.end27:                                       ; preds = %cond.false26, %cond.true24
  %cond28 = phi i32 [ %add25, %cond.true24 ], [ 0, %cond.false26 ], !dbg !274
  br label %cond.end29, !dbg !274

cond.end29:                                       ; preds = %cond.end27, %cond.true20
  %cond30 = phi i32 [ %sub21, %cond.true20 ], [ %cond28, %cond.end27 ], !dbg !274
  %mul31 = mul nsw i32 %14, %cond30, !dbg !274
  store i32 %mul31, i32* %y_edge_dist, align 4, !dbg !274
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !275), !dbg !276
  call void @llvm.dbg.declare(metadata !{i32* %mx_pos}, metadata !277), !dbg !278
  %20 = load i32* %x_dim.addr, align 4, !dbg !279
  %div = sdiv i32 %20, 2, !dbg !279
  %add32 = add nsw i32 %div, 1, !dbg !279
  store i32 %add32, i32* %mx_pos, align 4, !dbg !279
  call void @llvm.dbg.declare(metadata !{i32* %my_pos}, metadata !280), !dbg !281
  %21 = load i32* %y_dim.addr, align 4, !dbg !282
  %div33 = sdiv i32 %21, 2, !dbg !282
  %add34 = add nsw i32 %div33, 1, !dbg !282
  store i32 %add34, i32* %my_pos, align 4, !dbg !282
  store i32 0, i32* %i, align 4, !dbg !283
  br label %for.cond, !dbg !283

for.cond:                                         ; preds = %for.inc, %cond.end29
  %22 = load i32* %i, align 4, !dbg !283
  %23 = load i32* %filt_sz, align 4, !dbg !283
  %cmp35 = icmp slt i32 %22, %23, !dbg !283
  br i1 %cmp35, label %for.body, label %for.end, !dbg !283

for.body:                                         ; preds = %for.cond
  %24 = load i32* %i, align 4, !dbg !285
  %25 = load float** %result.addr, align 4, !dbg !285
  %arrayidx = getelementptr inbounds float* %25, i32 %24, !dbg !285
  store float 0.000000e+00, float* %arrayidx, !dbg !285
  br label %for.inc, !dbg !285

for.inc:                                          ; preds = %for.body
  %26 = load i32* %i, align 4, !dbg !286
  %inc = add nsw i32 %26, 1, !dbg !286
  store i32 %inc, i32* %i, align 4, !dbg !286
  br label %for.cond, !dbg !286

for.end:                                          ; preds = %for.cond
  %27 = load i32* %f_or_e.addr, align 4, !dbg !287
  %cmp36 = icmp eq i32 %27, 1, !dbg !287
  br i1 %cmp36, label %if.then, label %if.end59, !dbg !287

if.then:                                          ; preds = %for.end
  %28 = load i32* %x_pos.addr, align 4, !dbg !288
  %29 = load i32* %mx_pos, align 4, !dbg !288
  %cmp37 = icmp eq i32 %28, %29, !dbg !288
  br i1 %cmp37, label %if.then38, label %if.else, !dbg !288

if.then38:                                        ; preds = %if.then
  %30 = load i32* %x_dim.addr, align 4, !dbg !290
  %add39 = add nsw i32 %30, 1, !dbg !290
  %div40 = sdiv i32 %add39, 2, !dbg !290
  store i32 %div40, i32* %x_stop, align 4, !dbg !290
  br label %if.end45, !dbg !290

if.else:                                          ; preds = %if.then
  %31 = load i32* %x_pos.addr, align 4, !dbg !291
  %32 = load i32* %mx_pos, align 4, !dbg !291
  %sub41 = sub nsw i32 0, %32, !dbg !291
  %cmp42 = icmp eq i32 %31, %sub41, !dbg !291
  br i1 %cmp42, label %if.then43, label %if.end, !dbg !291

if.then43:                                        ; preds = %if.else
  %33 = load i32* %x_dim.addr, align 4, !dbg !292
  %div44 = sdiv i32 %33, 2, !dbg !292
  store i32 %div44, i32* %x_start, align 4, !dbg !292
  store i32 0, i32* %x_edge_dist, align 4, !dbg !294
  br label %if.end, !dbg !295

if.end:                                           ; preds = %if.then43, %if.else
  br label %if.end45

if.end45:                                         ; preds = %if.end, %if.then38
  %34 = load i32* %y_pos.addr, align 4, !dbg !296
  %35 = load i32* %my_pos, align 4, !dbg !296
  %cmp46 = icmp eq i32 %34, %35, !dbg !296
  br i1 %cmp46, label %if.then47, label %if.else51, !dbg !296

if.then47:                                        ; preds = %if.end45
  %36 = load i32* %x_dim.addr, align 4, !dbg !297
  %37 = load i32* %y_dim.addr, align 4, !dbg !297
  %add48 = add nsw i32 %37, 1, !dbg !297
  %div49 = sdiv i32 %add48, 2, !dbg !297
  %mul50 = mul nsw i32 %36, %div49, !dbg !297
  store i32 %mul50, i32* %y_stop, align 4, !dbg !297
  br label %if.end58, !dbg !297

if.else51:                                        ; preds = %if.end45
  %38 = load i32* %y_pos.addr, align 4, !dbg !298
  %39 = load i32* %my_pos, align 4, !dbg !298
  %sub52 = sub nsw i32 0, %39, !dbg !298
  %cmp53 = icmp eq i32 %38, %sub52, !dbg !298
  br i1 %cmp53, label %if.then54, label %if.end57, !dbg !298

if.then54:                                        ; preds = %if.else51
  %40 = load i32* %x_dim.addr, align 4, !dbg !299
  %41 = load i32* %y_dim.addr, align 4, !dbg !299
  %div55 = sdiv i32 %41, 2, !dbg !299
  %mul56 = mul nsw i32 %40, %div55, !dbg !299
  store i32 %mul56, i32* %y_start, align 4, !dbg !299
  store i32 0, i32* %y_edge_dist, align 4, !dbg !301
  br label %if.end57, !dbg !302

if.end57:                                         ; preds = %if.then54, %if.else51
  br label %if.end58

if.end58:                                         ; preds = %if.end57, %if.then47
  br label %if.end59, !dbg !303

if.end59:                                         ; preds = %if.end58, %for.end
  %42 = load i32* %y_start, align 4, !dbg !304
  store i32 %42, i32* %y_filt, align 4, !dbg !304
  %43 = load i32* %y_edge_dist, align 4, !dbg !304
  store i32 %43, i32* %y_edge, align 4, !dbg !304
  br label %for.cond60, !dbg !304

for.cond60:                                       ; preds = %for.inc130, %if.end59
  %44 = load i32* %y_filt, align 4, !dbg !304
  %45 = load i32* %y_stop, align 4, !dbg !304
  %cmp61 = icmp slt i32 %44, %45, !dbg !304
  br i1 %cmp61, label %for.body62, label %for.end133, !dbg !304

for.body62:                                       ; preds = %for.cond60
  %46 = load i32* %y_filt, align 4, !dbg !306
  %47 = load i32* %x_start, align 4, !dbg !306
  %add63 = add nsw i32 %46, %47, !dbg !306
  store i32 %add63, i32* %x_filt, align 4, !dbg !306
  %48 = load i32* %x_edge_dist, align 4, !dbg !306
  store i32 %48, i32* %x_edge, align 4, !dbg !306
  br label %for.cond64, !dbg !306

for.cond64:                                       ; preds = %for.inc126, %for.body62
  %49 = load i32* %x_filt, align 4, !dbg !306
  %50 = load i32* %y_filt, align 4, !dbg !306
  %51 = load i32* %x_stop, align 4, !dbg !306
  %add65 = add nsw i32 %50, %51, !dbg !306
  %cmp66 = icmp slt i32 %49, %add65, !dbg !306
  br i1 %cmp66, label %for.body67, label %for.end129, !dbg !306

for.body67:                                       ; preds = %for.cond64
  %52 = load i32* %x_filt, align 4, !dbg !308
  %53 = load float** %filt.addr, align 4, !dbg !308
  %arrayidx68 = getelementptr inbounds float* %53, i32 %52, !dbg !308
  %54 = load float* %arrayidx68, !dbg !308
  %55 = load i32* %y_base, align 4, !dbg !308
  %56 = load i32* %y_edge, align 4, !dbg !308
  %cmp69 = icmp sge i32 %56, 0, !dbg !308
  br i1 %cmp69, label %cond.true70, label %cond.false71, !dbg !308

cond.true70:                                      ; preds = %for.body67
  %57 = load i32* %y_edge, align 4, !dbg !308
  br label %cond.end73, !dbg !308

cond.false71:                                     ; preds = %for.body67
  %58 = load i32* %y_edge, align 4, !dbg !308
  %sub72 = sub nsw i32 0, %58, !dbg !308
  br label %cond.end73, !dbg !308

cond.end73:                                       ; preds = %cond.false71, %cond.true70
  %cond74 = phi i32 [ %57, %cond.true70 ], [ %sub72, %cond.false71 ], !dbg !308
  %sub75 = sub nsw i32 %55, %cond74, !dbg !308
  %cmp76 = icmp sge i32 %sub75, 0, !dbg !308
  br i1 %cmp76, label %cond.true77, label %cond.false85, !dbg !308

cond.true77:                                      ; preds = %cond.end73
  %59 = load i32* %y_base, align 4, !dbg !308
  %60 = load i32* %y_edge, align 4, !dbg !308
  %cmp78 = icmp sge i32 %60, 0, !dbg !308
  br i1 %cmp78, label %cond.true79, label %cond.false80, !dbg !308

cond.true79:                                      ; preds = %cond.true77
  %61 = load i32* %y_edge, align 4, !dbg !308
  br label %cond.end82, !dbg !308

cond.false80:                                     ; preds = %cond.true77
  %62 = load i32* %y_edge, align 4, !dbg !308
  %sub81 = sub nsw i32 0, %62, !dbg !308
  br label %cond.end82, !dbg !308

cond.end82:                                       ; preds = %cond.false80, %cond.true79
  %cond83 = phi i32 [ %61, %cond.true79 ], [ %sub81, %cond.false80 ], !dbg !308
  %sub84 = sub nsw i32 %59, %cond83, !dbg !308
  br label %cond.end94, !dbg !308

cond.false85:                                     ; preds = %cond.end73
  %63 = load i32* %y_base, align 4, !dbg !308
  %64 = load i32* %y_edge, align 4, !dbg !308
  %cmp86 = icmp sge i32 %64, 0, !dbg !308
  br i1 %cmp86, label %cond.true87, label %cond.false88, !dbg !308

cond.true87:                                      ; preds = %cond.false85
  %65 = load i32* %y_edge, align 4, !dbg !308
  br label %cond.end90, !dbg !308

cond.false88:                                     ; preds = %cond.false85
  %66 = load i32* %y_edge, align 4, !dbg !308
  %sub89 = sub nsw i32 0, %66, !dbg !308
  br label %cond.end90, !dbg !308

cond.end90:                                       ; preds = %cond.false88, %cond.true87
  %cond91 = phi i32 [ %65, %cond.true87 ], [ %sub89, %cond.false88 ], !dbg !308
  %sub92 = sub nsw i32 %63, %cond91, !dbg !308
  %sub93 = sub nsw i32 0, %sub92, !dbg !308
  br label %cond.end94, !dbg !308

cond.end94:                                       ; preds = %cond.end90, %cond.end82
  %cond95 = phi i32 [ %sub84, %cond.end82 ], [ %sub93, %cond.end90 ], !dbg !308
  %67 = load i32* %x_base, align 4, !dbg !308
  %68 = load i32* %x_edge, align 4, !dbg !308
  %cmp96 = icmp sge i32 %68, 0, !dbg !308
  br i1 %cmp96, label %cond.true97, label %cond.false98, !dbg !308

cond.true97:                                      ; preds = %cond.end94
  %69 = load i32* %x_edge, align 4, !dbg !308
  br label %cond.end100, !dbg !308

cond.false98:                                     ; preds = %cond.end94
  %70 = load i32* %x_edge, align 4, !dbg !308
  %sub99 = sub nsw i32 0, %70, !dbg !308
  br label %cond.end100, !dbg !308

cond.end100:                                      ; preds = %cond.false98, %cond.true97
  %cond101 = phi i32 [ %69, %cond.true97 ], [ %sub99, %cond.false98 ], !dbg !308
  %sub102 = sub nsw i32 %67, %cond101, !dbg !308
  %cmp103 = icmp sge i32 %sub102, 0, !dbg !308
  br i1 %cmp103, label %cond.true104, label %cond.false112, !dbg !308

cond.true104:                                     ; preds = %cond.end100
  %71 = load i32* %x_base, align 4, !dbg !308
  %72 = load i32* %x_edge, align 4, !dbg !308
  %cmp105 = icmp sge i32 %72, 0, !dbg !308
  br i1 %cmp105, label %cond.true106, label %cond.false107, !dbg !308

cond.true106:                                     ; preds = %cond.true104
  %73 = load i32* %x_edge, align 4, !dbg !308
  br label %cond.end109, !dbg !308

cond.false107:                                    ; preds = %cond.true104
  %74 = load i32* %x_edge, align 4, !dbg !308
  %sub108 = sub nsw i32 0, %74, !dbg !308
  br label %cond.end109, !dbg !308

cond.end109:                                      ; preds = %cond.false107, %cond.true106
  %cond110 = phi i32 [ %73, %cond.true106 ], [ %sub108, %cond.false107 ], !dbg !308
  %sub111 = sub nsw i32 %71, %cond110, !dbg !308
  br label %cond.end121, !dbg !308

cond.false112:                                    ; preds = %cond.end100
  %75 = load i32* %x_base, align 4, !dbg !308
  %76 = load i32* %x_edge, align 4, !dbg !308
  %cmp113 = icmp sge i32 %76, 0, !dbg !308
  br i1 %cmp113, label %cond.true114, label %cond.false115, !dbg !308

cond.true114:                                     ; preds = %cond.false112
  %77 = load i32* %x_edge, align 4, !dbg !308
  br label %cond.end117, !dbg !308

cond.false115:                                    ; preds = %cond.false112
  %78 = load i32* %x_edge, align 4, !dbg !308
  %sub116 = sub nsw i32 0, %78, !dbg !308
  br label %cond.end117, !dbg !308

cond.end117:                                      ; preds = %cond.false115, %cond.true114
  %cond118 = phi i32 [ %77, %cond.true114 ], [ %sub116, %cond.false115 ], !dbg !308
  %sub119 = sub nsw i32 %75, %cond118, !dbg !308
  %sub120 = sub nsw i32 0, %sub119, !dbg !308
  br label %cond.end121, !dbg !308

cond.end121:                                      ; preds = %cond.end117, %cond.end109
  %cond122 = phi i32 [ %sub111, %cond.end109 ], [ %sub120, %cond.end117 ], !dbg !308
  %add123 = add nsw i32 %cond95, %cond122, !dbg !308
  %79 = load float** %result.addr, align 4, !dbg !308
  %arrayidx124 = getelementptr inbounds float* %79, i32 %add123, !dbg !308
  %80 = load float* %arrayidx124, !dbg !308
  %add125 = fadd float %80, %54, !dbg !308
  store float %add125, float* %arrayidx124, !dbg !308
  br label %for.inc126, !dbg !308

for.inc126:                                       ; preds = %cond.end121
  %81 = load i32* %x_filt, align 4, !dbg !309
  %inc127 = add nsw i32 %81, 1, !dbg !309
  store i32 %inc127, i32* %x_filt, align 4, !dbg !309
  %82 = load i32* %x_edge, align 4, !dbg !309
  %inc128 = add nsw i32 %82, 1, !dbg !309
  store i32 %inc128, i32* %x_edge, align 4, !dbg !309
  br label %for.cond64, !dbg !309

for.end129:                                       ; preds = %for.cond64
  br label %for.inc130, !dbg !310

for.inc130:                                       ; preds = %for.end129
  %83 = load i32* %x_dim.addr, align 4, !dbg !311
  %84 = load i32* %y_filt, align 4, !dbg !311
  %add131 = add nsw i32 %84, %83, !dbg !311
  store i32 %add131, i32* %y_filt, align 4, !dbg !311
  %85 = load i32* %x_dim.addr, align 4, !dbg !311
  %86 = load i32* %y_edge, align 4, !dbg !311
  %add132 = add nsw i32 %86, %85, !dbg !311
  store i32 %add132, i32* %y_edge, align 4, !dbg !311
  br label %for.cond60, !dbg !311

for.end133:                                       ; preds = %for.cond60
  %87 = load i32* %f_or_e.addr, align 4, !dbg !312
  %cmp134 = icmp eq i32 %87, 1, !dbg !312
  br i1 %cmp134, label %if.then135, label %if.end176, !dbg !312

if.then135:                                       ; preds = %for.end133
  %88 = load i32* %x_pos.addr, align 4, !dbg !313
  %cmp136 = icmp sge i32 %88, 0, !dbg !313
  br i1 %cmp136, label %cond.true137, label %cond.false138, !dbg !313

cond.true137:                                     ; preds = %if.then135
  %89 = load i32* %x_pos.addr, align 4, !dbg !313
  br label %cond.end140, !dbg !313

cond.false138:                                    ; preds = %if.then135
  %90 = load i32* %x_pos.addr, align 4, !dbg !313
  %sub139 = sub nsw i32 0, %90, !dbg !313
  br label %cond.end140, !dbg !313

cond.end140:                                      ; preds = %cond.false138, %cond.true137
  %cond141 = phi i32 [ %89, %cond.true137 ], [ %sub139, %cond.false138 ], !dbg !313
  %91 = load i32* %mx_pos, align 4, !dbg !313
  %cmp142 = icmp ne i32 %cond141, %91, !dbg !313
  br i1 %cmp142, label %land.lhs.true, label %if.end154, !dbg !313

land.lhs.true:                                    ; preds = %cond.end140
  %92 = load i32* %x_pos.addr, align 4, !dbg !313
  %cmp143 = icmp ne i32 %92, 0, !dbg !313
  br i1 %cmp143, label %if.then144, label %if.end154, !dbg !313

if.then144:                                       ; preds = %land.lhs.true
  %93 = load i32* %x_base, align 4, !dbg !315
  store i32 %93, i32* %y_filt, align 4, !dbg !315
  br label %for.cond145, !dbg !315

for.cond145:                                      ; preds = %for.inc151, %if.then144
  %94 = load i32* %y_filt, align 4, !dbg !315
  %95 = load i32* %filt_sz, align 4, !dbg !315
  %cmp146 = icmp slt i32 %94, %95, !dbg !315
  br i1 %cmp146, label %for.body147, label %for.end153, !dbg !315

for.body147:                                      ; preds = %for.cond145
  %96 = load i32* %y_filt, align 4, !dbg !317
  %97 = load float** %result.addr, align 4, !dbg !317
  %arrayidx148 = getelementptr inbounds float* %97, i32 %96, !dbg !317
  %98 = load float* %arrayidx148, !dbg !317
  %99 = load i32* %y_filt, align 4, !dbg !317
  %100 = load float** %result.addr, align 4, !dbg !317
  %arrayidx149 = getelementptr inbounds float* %100, i32 %99, !dbg !317
  %101 = load float* %arrayidx149, !dbg !317
  %add150 = fadd float %101, %98, !dbg !317
  store float %add150, float* %arrayidx149, !dbg !317
  br label %for.inc151, !dbg !317

for.inc151:                                       ; preds = %for.body147
  %102 = load i32* %x_dim.addr, align 4, !dbg !318
  %103 = load i32* %y_filt, align 4, !dbg !318
  %add152 = add nsw i32 %103, %102, !dbg !318
  store i32 %add152, i32* %y_filt, align 4, !dbg !318
  br label %for.cond145, !dbg !318

for.end153:                                       ; preds = %for.cond145
  br label %if.end154, !dbg !319

if.end154:                                        ; preds = %for.end153, %land.lhs.true, %cond.end140
  %104 = load i32* %y_pos.addr, align 4, !dbg !320
  %cmp155 = icmp sge i32 %104, 0, !dbg !320
  br i1 %cmp155, label %cond.true156, label %cond.false157, !dbg !320

cond.true156:                                     ; preds = %if.end154
  %105 = load i32* %y_pos.addr, align 4, !dbg !320
  br label %cond.end159, !dbg !320

cond.false157:                                    ; preds = %if.end154
  %106 = load i32* %y_pos.addr, align 4, !dbg !320
  %sub158 = sub nsw i32 0, %106, !dbg !320
  br label %cond.end159, !dbg !320

cond.end159:                                      ; preds = %cond.false157, %cond.true156
  %cond160 = phi i32 [ %105, %cond.true156 ], [ %sub158, %cond.false157 ], !dbg !320
  %107 = load i32* %my_pos, align 4, !dbg !320
  %cmp161 = icmp ne i32 %cond160, %107, !dbg !320
  br i1 %cmp161, label %land.lhs.true162, label %if.end175, !dbg !320

land.lhs.true162:                                 ; preds = %cond.end159
  %108 = load i32* %y_pos.addr, align 4, !dbg !320
  %cmp163 = icmp ne i32 %108, 0, !dbg !320
  br i1 %cmp163, label %if.then164, label %if.end175, !dbg !320

if.then164:                                       ; preds = %land.lhs.true162
  %109 = load i32* %y_base, align 4, !dbg !321
  store i32 %109, i32* %x_filt, align 4, !dbg !321
  br label %for.cond165, !dbg !321

for.cond165:                                      ; preds = %for.inc172, %if.then164
  %110 = load i32* %x_filt, align 4, !dbg !321
  %111 = load i32* %y_base, align 4, !dbg !321
  %112 = load i32* %x_dim.addr, align 4, !dbg !321
  %add166 = add nsw i32 %111, %112, !dbg !321
  %cmp167 = icmp slt i32 %110, %add166, !dbg !321
  br i1 %cmp167, label %for.body168, label %for.end174, !dbg !321

for.body168:                                      ; preds = %for.cond165
  %113 = load i32* %x_filt, align 4, !dbg !323
  %114 = load float** %result.addr, align 4, !dbg !323
  %arrayidx169 = getelementptr inbounds float* %114, i32 %113, !dbg !323
  %115 = load float* %arrayidx169, !dbg !323
  %116 = load i32* %x_filt, align 4, !dbg !323
  %117 = load float** %result.addr, align 4, !dbg !323
  %arrayidx170 = getelementptr inbounds float* %117, i32 %116, !dbg !323
  %118 = load float* %arrayidx170, !dbg !323
  %add171 = fadd float %118, %115, !dbg !323
  store float %add171, float* %arrayidx170, !dbg !323
  br label %for.inc172, !dbg !323

for.inc172:                                       ; preds = %for.body168
  %119 = load i32* %x_filt, align 4, !dbg !324
  %inc173 = add nsw i32 %119, 1, !dbg !324
  store i32 %inc173, i32* %x_filt, align 4, !dbg !324
  br label %for.cond165, !dbg !324

for.end174:                                       ; preds = %for.cond165
  br label %if.end175, !dbg !325

if.end175:                                        ; preds = %for.end174, %land.lhs.true162, %cond.end159
  br label %if.end176, !dbg !326

if.end176:                                        ; preds = %if.end175, %for.end133
  %120 = load i32* %retval, !dbg !327
  ret i32 %120, !dbg !327
}

define arm_aapcscc i32 @extend(float* %filt, i32 %x_dim, i32 %y_dim, i32 %x_pos, i32 %y_pos, float* %result, i32 %f_or_e) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %filt.addr = alloca float*, align 4
  %x_dim.addr = alloca i32, align 4
  %y_dim.addr = alloca i32, align 4
  %x_pos.addr = alloca i32, align 4
  %y_pos.addr = alloca i32, align 4
  %result.addr = alloca float*, align 4
  %f_or_e.addr = alloca i32, align 4
  %filt_sz = alloca i32, align 4
  %x_start = alloca i32, align 4
  %y_start = alloca i32, align 4
  %x_stop = alloca i32, align 4
  %y_stop = alloca i32, align 4
  %y_filt = alloca i32, align 4
  %x_filt = alloca i32, align 4
  %y_edge = alloca i32, align 4
  %x_edge = alloca i32, align 4
  %x_base = alloca i32, align 4
  %y_base = alloca i32, align 4
  %x_edge_dist = alloca i32, align 4
  %y_edge_dist = alloca i32, align 4
  %i = alloca i32, align 4
  %mx_pos = alloca i32, align 4
  %my_pos = alloca i32, align 4
  store float* %filt, float** %filt.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %filt.addr}, metadata !328), !dbg !329
  store i32 %x_dim, i32* %x_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_dim.addr}, metadata !330), !dbg !331
  store i32 %y_dim, i32* %y_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_dim.addr}, metadata !332), !dbg !333
  store i32 %x_pos, i32* %x_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_pos.addr}, metadata !334), !dbg !335
  store i32 %y_pos, i32* %y_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_pos.addr}, metadata !336), !dbg !337
  store float* %result, float** %result.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result.addr}, metadata !338), !dbg !339
  store i32 %f_or_e, i32* %f_or_e.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %f_or_e.addr}, metadata !340), !dbg !341
  call void @llvm.dbg.declare(metadata !{i32* %filt_sz}, metadata !342), !dbg !344
  %0 = load i32* %x_dim.addr, align 4, !dbg !345
  %1 = load i32* %y_dim.addr, align 4, !dbg !345
  %mul = mul nsw i32 %0, %1, !dbg !345
  store i32 %mul, i32* %filt_sz, align 4, !dbg !345
  call void @llvm.dbg.declare(metadata !{i32* %x_start}, metadata !346), !dbg !347
  store i32 0, i32* %x_start, align 4, !dbg !348
  call void @llvm.dbg.declare(metadata !{i32* %y_start}, metadata !349), !dbg !350
  store i32 0, i32* %y_start, align 4, !dbg !348
  call void @llvm.dbg.declare(metadata !{i32* %x_stop}, metadata !351), !dbg !352
  %2 = load i32* %x_dim.addr, align 4, !dbg !348
  store i32 %2, i32* %x_stop, align 4, !dbg !348
  call void @llvm.dbg.declare(metadata !{i32* %y_stop}, metadata !353), !dbg !354
  %3 = load i32* %filt_sz, align 4, !dbg !348
  store i32 %3, i32* %y_stop, align 4, !dbg !348
  call void @llvm.dbg.declare(metadata !{i32* %y_filt}, metadata !355), !dbg !356
  call void @llvm.dbg.declare(metadata !{i32* %x_filt}, metadata !357), !dbg !358
  call void @llvm.dbg.declare(metadata !{i32* %y_edge}, metadata !359), !dbg !360
  call void @llvm.dbg.declare(metadata !{i32* %x_edge}, metadata !361), !dbg !362
  call void @llvm.dbg.declare(metadata !{i32* %x_base}, metadata !363), !dbg !364
  %4 = load i32* %x_pos.addr, align 4, !dbg !365
  %cmp = icmp sgt i32 %4, 0, !dbg !365
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !365

cond.true:                                        ; preds = %entry
  %5 = load i32* %x_dim.addr, align 4, !dbg !365
  %sub = sub nsw i32 %5, 1, !dbg !365
  br label %cond.end, !dbg !365

cond.false:                                       ; preds = %entry
  br label %cond.end, !dbg !365

cond.end:                                         ; preds = %cond.false, %cond.true
  %cond = phi i32 [ %sub, %cond.true ], [ 0, %cond.false ], !dbg !365
  store i32 %cond, i32* %x_base, align 4, !dbg !365
  call void @llvm.dbg.declare(metadata !{i32* %y_base}, metadata !366), !dbg !367
  %6 = load i32* %y_pos.addr, align 4, !dbg !368
  %cmp1 = icmp sgt i32 %6, 0, !dbg !368
  br i1 %cmp1, label %cond.true2, label %cond.false5, !dbg !368

cond.true2:                                       ; preds = %cond.end
  %7 = load i32* %x_dim.addr, align 4, !dbg !368
  %8 = load i32* %y_dim.addr, align 4, !dbg !368
  %sub3 = sub nsw i32 %8, 1, !dbg !368
  %mul4 = mul nsw i32 %7, %sub3, !dbg !368
  br label %cond.end6, !dbg !368

cond.false5:                                      ; preds = %cond.end
  br label %cond.end6, !dbg !368

cond.end6:                                        ; preds = %cond.false5, %cond.true2
  %cond7 = phi i32 [ %mul4, %cond.true2 ], [ 0, %cond.false5 ], !dbg !368
  store i32 %cond7, i32* %y_base, align 4, !dbg !368
  call void @llvm.dbg.declare(metadata !{i32* %x_edge_dist}, metadata !369), !dbg !370
  %9 = load i32* %x_pos.addr, align 4, !dbg !371
  %cmp8 = icmp sgt i32 %9, 0, !dbg !371
  br i1 %cmp8, label %cond.true9, label %cond.false11, !dbg !371

cond.true9:                                       ; preds = %cond.end6
  %10 = load i32* %x_pos.addr, align 4, !dbg !371
  %11 = load i32* %x_dim.addr, align 4, !dbg !371
  %sub10 = sub nsw i32 %10, %11, !dbg !371
  br label %cond.end17, !dbg !371

cond.false11:                                     ; preds = %cond.end6
  %12 = load i32* %x_pos.addr, align 4, !dbg !371
  %cmp12 = icmp slt i32 %12, -1, !dbg !371
  br i1 %cmp12, label %cond.true13, label %cond.false14, !dbg !371

cond.true13:                                      ; preds = %cond.false11
  %13 = load i32* %x_pos.addr, align 4, !dbg !371
  %add = add nsw i32 %13, 1, !dbg !371
  br label %cond.end15, !dbg !371

cond.false14:                                     ; preds = %cond.false11
  br label %cond.end15, !dbg !371

cond.end15:                                       ; preds = %cond.false14, %cond.true13
  %cond16 = phi i32 [ %add, %cond.true13 ], [ 0, %cond.false14 ], !dbg !371
  br label %cond.end17, !dbg !371

cond.end17:                                       ; preds = %cond.end15, %cond.true9
  %cond18 = phi i32 [ %sub10, %cond.true9 ], [ %cond16, %cond.end15 ], !dbg !371
  store i32 %cond18, i32* %x_edge_dist, align 4, !dbg !371
  call void @llvm.dbg.declare(metadata !{i32* %y_edge_dist}, metadata !372), !dbg !373
  %14 = load i32* %x_dim.addr, align 4, !dbg !374
  %15 = load i32* %y_pos.addr, align 4, !dbg !374
  %cmp19 = icmp sgt i32 %15, 0, !dbg !374
  br i1 %cmp19, label %cond.true20, label %cond.false22, !dbg !374

cond.true20:                                      ; preds = %cond.end17
  %16 = load i32* %y_pos.addr, align 4, !dbg !374
  %17 = load i32* %y_dim.addr, align 4, !dbg !374
  %sub21 = sub nsw i32 %16, %17, !dbg !374
  br label %cond.end29, !dbg !374

cond.false22:                                     ; preds = %cond.end17
  %18 = load i32* %y_pos.addr, align 4, !dbg !374
  %cmp23 = icmp slt i32 %18, -1, !dbg !374
  br i1 %cmp23, label %cond.true24, label %cond.false26, !dbg !374

cond.true24:                                      ; preds = %cond.false22
  %19 = load i32* %y_pos.addr, align 4, !dbg !374
  %add25 = add nsw i32 %19, 1, !dbg !374
  br label %cond.end27, !dbg !374

cond.false26:                                     ; preds = %cond.false22
  br label %cond.end27, !dbg !374

cond.end27:                                       ; preds = %cond.false26, %cond.true24
  %cond28 = phi i32 [ %add25, %cond.true24 ], [ 0, %cond.false26 ], !dbg !374
  br label %cond.end29, !dbg !374

cond.end29:                                       ; preds = %cond.end27, %cond.true20
  %cond30 = phi i32 [ %sub21, %cond.true20 ], [ %cond28, %cond.end27 ], !dbg !374
  %mul31 = mul nsw i32 %14, %cond30, !dbg !374
  store i32 %mul31, i32* %y_edge_dist, align 4, !dbg !374
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !375), !dbg !376
  call void @llvm.dbg.declare(metadata !{i32* %mx_pos}, metadata !377), !dbg !378
  %20 = load i32* %x_dim.addr, align 4, !dbg !379
  %div = sdiv i32 %20, 2, !dbg !379
  %add32 = add nsw i32 %div, 1, !dbg !379
  store i32 %add32, i32* %mx_pos, align 4, !dbg !379
  call void @llvm.dbg.declare(metadata !{i32* %my_pos}, metadata !380), !dbg !381
  %21 = load i32* %y_dim.addr, align 4, !dbg !382
  %div33 = sdiv i32 %21, 2, !dbg !382
  %add34 = add nsw i32 %div33, 1, !dbg !382
  store i32 %add34, i32* %my_pos, align 4, !dbg !382
  store i32 0, i32* %i, align 4, !dbg !383
  br label %for.cond, !dbg !383

for.cond:                                         ; preds = %for.inc, %cond.end29
  %22 = load i32* %i, align 4, !dbg !383
  %23 = load i32* %filt_sz, align 4, !dbg !383
  %cmp35 = icmp slt i32 %22, %23, !dbg !383
  br i1 %cmp35, label %for.body, label %for.end, !dbg !383

for.body:                                         ; preds = %for.cond
  %24 = load i32* %i, align 4, !dbg !385
  %25 = load float** %result.addr, align 4, !dbg !385
  %arrayidx = getelementptr inbounds float* %25, i32 %24, !dbg !385
  store float 0.000000e+00, float* %arrayidx, !dbg !385
  br label %for.inc, !dbg !385

for.inc:                                          ; preds = %for.body
  %26 = load i32* %i, align 4, !dbg !386
  %inc = add nsw i32 %26, 1, !dbg !386
  store i32 %inc, i32* %i, align 4, !dbg !386
  br label %for.cond, !dbg !386

for.end:                                          ; preds = %for.cond
  %27 = load i32* %f_or_e.addr, align 4, !dbg !387
  %cmp36 = icmp eq i32 %27, 1, !dbg !387
  br i1 %cmp36, label %if.then, label %if.end59, !dbg !387

if.then:                                          ; preds = %for.end
  %28 = load i32* %x_pos.addr, align 4, !dbg !388
  %29 = load i32* %mx_pos, align 4, !dbg !388
  %cmp37 = icmp eq i32 %28, %29, !dbg !388
  br i1 %cmp37, label %if.then38, label %if.else, !dbg !388

if.then38:                                        ; preds = %if.then
  %30 = load i32* %x_dim.addr, align 4, !dbg !390
  %add39 = add nsw i32 %30, 1, !dbg !390
  %div40 = sdiv i32 %add39, 2, !dbg !390
  store i32 %div40, i32* %x_stop, align 4, !dbg !390
  br label %if.end45, !dbg !390

if.else:                                          ; preds = %if.then
  %31 = load i32* %x_pos.addr, align 4, !dbg !391
  %32 = load i32* %mx_pos, align 4, !dbg !391
  %sub41 = sub nsw i32 0, %32, !dbg !391
  %cmp42 = icmp eq i32 %31, %sub41, !dbg !391
  br i1 %cmp42, label %if.then43, label %if.end, !dbg !391

if.then43:                                        ; preds = %if.else
  %33 = load i32* %x_dim.addr, align 4, !dbg !392
  %div44 = sdiv i32 %33, 2, !dbg !392
  store i32 %div44, i32* %x_start, align 4, !dbg !392
  store i32 0, i32* %x_edge_dist, align 4, !dbg !394
  br label %if.end, !dbg !395

if.end:                                           ; preds = %if.then43, %if.else
  br label %if.end45

if.end45:                                         ; preds = %if.end, %if.then38
  %34 = load i32* %y_pos.addr, align 4, !dbg !396
  %35 = load i32* %my_pos, align 4, !dbg !396
  %cmp46 = icmp eq i32 %34, %35, !dbg !396
  br i1 %cmp46, label %if.then47, label %if.else51, !dbg !396

if.then47:                                        ; preds = %if.end45
  %36 = load i32* %x_dim.addr, align 4, !dbg !397
  %37 = load i32* %y_dim.addr, align 4, !dbg !397
  %add48 = add nsw i32 %37, 1, !dbg !397
  %div49 = sdiv i32 %add48, 2, !dbg !397
  %mul50 = mul nsw i32 %36, %div49, !dbg !397
  store i32 %mul50, i32* %y_stop, align 4, !dbg !397
  br label %if.end58, !dbg !397

if.else51:                                        ; preds = %if.end45
  %38 = load i32* %y_pos.addr, align 4, !dbg !398
  %39 = load i32* %my_pos, align 4, !dbg !398
  %sub52 = sub nsw i32 0, %39, !dbg !398
  %cmp53 = icmp eq i32 %38, %sub52, !dbg !398
  br i1 %cmp53, label %if.then54, label %if.end57, !dbg !398

if.then54:                                        ; preds = %if.else51
  %40 = load i32* %x_dim.addr, align 4, !dbg !399
  %41 = load i32* %y_dim.addr, align 4, !dbg !399
  %div55 = sdiv i32 %41, 2, !dbg !399
  %mul56 = mul nsw i32 %40, %div55, !dbg !399
  store i32 %mul56, i32* %y_start, align 4, !dbg !399
  store i32 0, i32* %y_edge_dist, align 4, !dbg !401
  br label %if.end57, !dbg !402

if.end57:                                         ; preds = %if.then54, %if.else51
  br label %if.end58

if.end58:                                         ; preds = %if.end57, %if.then47
  br label %if.end59, !dbg !403

if.end59:                                         ; preds = %if.end58, %for.end
  %42 = load i32* %y_start, align 4, !dbg !404
  store i32 %42, i32* %y_filt, align 4, !dbg !404
  %43 = load i32* %y_edge_dist, align 4, !dbg !404
  store i32 %43, i32* %y_edge, align 4, !dbg !404
  br label %for.cond60, !dbg !404

for.cond60:                                       ; preds = %for.inc267, %if.end59
  %44 = load i32* %y_filt, align 4, !dbg !404
  %45 = load i32* %y_stop, align 4, !dbg !404
  %cmp61 = icmp slt i32 %44, %45, !dbg !404
  br i1 %cmp61, label %for.body62, label %for.end270, !dbg !404

for.body62:                                       ; preds = %for.cond60
  %46 = load i32* %y_filt, align 4, !dbg !406
  %47 = load i32* %x_start, align 4, !dbg !406
  %add63 = add nsw i32 %46, %47, !dbg !406
  store i32 %add63, i32* %x_filt, align 4, !dbg !406
  %48 = load i32* %x_edge_dist, align 4, !dbg !406
  store i32 %48, i32* %x_edge, align 4, !dbg !406
  br label %for.cond64, !dbg !406

for.cond64:                                       ; preds = %for.inc263, %for.body62
  %49 = load i32* %x_filt, align 4, !dbg !406
  %50 = load i32* %y_filt, align 4, !dbg !406
  %51 = load i32* %x_stop, align 4, !dbg !406
  %add65 = add nsw i32 %50, %51, !dbg !406
  %cmp66 = icmp slt i32 %49, %add65, !dbg !406
  br i1 %cmp66, label %for.body67, label %for.end266, !dbg !406

for.body67:                                       ; preds = %for.cond64
  %52 = load i32* %y_base, align 4, !dbg !408
  %tobool = icmp ne i32 %52, 0, !dbg !408
  br i1 %tobool, label %lor.rhs, label %land.lhs.true, !dbg !408

land.lhs.true:                                    ; preds = %for.body67
  %53 = load i32* %y_edge, align 4, !dbg !408
  %cmp68 = icmp sgt i32 %53, 0, !dbg !408
  br i1 %cmp68, label %cond.true69, label %cond.false70, !dbg !408

cond.true69:                                      ; preds = %land.lhs.true
  br label %cond.end73, !dbg !408

cond.false70:                                     ; preds = %land.lhs.true
  %54 = load i32* %y_edge, align 4, !dbg !408
  %cmp71 = icmp slt i32 %54, 0, !dbg !408
  %cond72 = select i1 %cmp71, i32 -1, i32 0, !dbg !408
  br label %cond.end73, !dbg !408

cond.end73:                                       ; preds = %cond.false70, %cond.true69
  %cond74 = phi i32 [ 1, %cond.true69 ], [ %cond72, %cond.false70 ], !dbg !408
  %cmp75 = icmp eq i32 %cond74, -1, !dbg !408
  br i1 %cmp75, label %lor.end, label %lor.rhs, !dbg !408

lor.rhs:                                          ; preds = %cond.end73, %for.body67
  %55 = load i32* %y_base, align 4, !dbg !408
  %tobool76 = icmp ne i32 %55, 0, !dbg !408
  br i1 %tobool76, label %land.rhs, label %land.end, !dbg !408

land.rhs:                                         ; preds = %lor.rhs
  %56 = load i32* %y_edge, align 4, !dbg !408
  %cmp77 = icmp sgt i32 %56, 0, !dbg !408
  br i1 %cmp77, label %cond.true78, label %cond.false79, !dbg !408

cond.true78:                                      ; preds = %land.rhs
  br label %cond.end82, !dbg !408

cond.false79:                                     ; preds = %land.rhs
  %57 = load i32* %y_edge, align 4, !dbg !408
  %cmp80 = icmp slt i32 %57, 0, !dbg !408
  %cond81 = select i1 %cmp80, i32 -1, i32 0, !dbg !408
  br label %cond.end82, !dbg !408

cond.end82:                                       ; preds = %cond.false79, %cond.true78
  %cond83 = phi i32 [ 1, %cond.true78 ], [ %cond81, %cond.false79 ], !dbg !408
  %cmp84 = icmp eq i32 %cond83, 1, !dbg !408
  br label %land.end

land.end:                                         ; preds = %cond.end82, %lor.rhs
  %58 = phi i1 [ false, %lor.rhs ], [ %cmp84, %cond.end82 ]
  br label %lor.end

lor.end:                                          ; preds = %land.end, %cond.end73
  %59 = phi i1 [ true, %cond.end73 ], [ %58, %land.end ]
  %lor.ext = zext i1 %59 to i32
  %60 = load i32* %x_base, align 4
  %tobool85 = icmp ne i32 %60, 0
  br i1 %tobool85, label %lor.rhs95, label %land.lhs.true86

land.lhs.true86:                                  ; preds = %lor.end
  %61 = load i32* %x_edge, align 4
  %cmp87 = icmp sgt i32 %61, 0
  br i1 %cmp87, label %cond.true88, label %cond.false89

cond.true88:                                      ; preds = %land.lhs.true86
  br label %cond.end92

cond.false89:                                     ; preds = %land.lhs.true86
  %62 = load i32* %x_edge, align 4
  %cmp90 = icmp slt i32 %62, 0
  %cond91 = select i1 %cmp90, i32 -1, i32 0
  br label %cond.end92

cond.end92:                                       ; preds = %cond.false89, %cond.true88
  %cond93 = phi i32 [ 1, %cond.true88 ], [ %cond91, %cond.false89 ]
  %cmp94 = icmp eq i32 %cond93, -1
  br i1 %cmp94, label %lor.end107, label %lor.rhs95

lor.rhs95:                                        ; preds = %cond.end92, %lor.end
  %63 = load i32* %x_base, align 4
  %tobool96 = icmp ne i32 %63, 0
  br i1 %tobool96, label %land.rhs97, label %land.end106

land.rhs97:                                       ; preds = %lor.rhs95
  %64 = load i32* %x_edge, align 4
  %cmp98 = icmp sgt i32 %64, 0
  br i1 %cmp98, label %cond.true99, label %cond.false100

cond.true99:                                      ; preds = %land.rhs97
  br label %cond.end103

cond.false100:                                    ; preds = %land.rhs97
  %65 = load i32* %x_edge, align 4
  %cmp101 = icmp slt i32 %65, 0
  %cond102 = select i1 %cmp101, i32 -1, i32 0
  br label %cond.end103

cond.end103:                                      ; preds = %cond.false100, %cond.true99
  %cond104 = phi i32 [ 1, %cond.true99 ], [ %cond102, %cond.false100 ]
  %cmp105 = icmp eq i32 %cond104, 1
  br label %land.end106

land.end106:                                      ; preds = %cond.end103, %lor.rhs95
  %66 = phi i1 [ false, %lor.rhs95 ], [ %cmp105, %cond.end103 ]
  br label %lor.end107

lor.end107:                                       ; preds = %land.end106, %cond.end92
  %67 = phi i1 [ true, %cond.end92 ], [ %66, %land.end106 ]
  %lor.ext108 = zext i1 %67 to i32
  %cmp109 = icmp ne i32 %lor.ext, %lor.ext108
  br i1 %cmp109, label %if.then110, label %if.else203

if.then110:                                       ; preds = %lor.end107
  %68 = load i32* %x_filt, align 4, !dbg !409
  %69 = load float** %filt.addr, align 4, !dbg !409
  %arrayidx111 = getelementptr inbounds float* %69, i32 %68, !dbg !409
  %70 = load float* %arrayidx111, !dbg !409
  %71 = load i32* %y_base, align 4, !dbg !409
  %72 = load i32* %y_edge, align 4, !dbg !409
  %cmp112 = icmp sge i32 %72, 0, !dbg !409
  br i1 %cmp112, label %cond.true113, label %cond.false114, !dbg !409

cond.true113:                                     ; preds = %if.then110
  %73 = load i32* %y_edge, align 4, !dbg !409
  br label %cond.end116, !dbg !409

cond.false114:                                    ; preds = %if.then110
  %74 = load i32* %y_edge, align 4, !dbg !409
  %sub115 = sub nsw i32 0, %74, !dbg !409
  br label %cond.end116, !dbg !409

cond.end116:                                      ; preds = %cond.false114, %cond.true113
  %cond117 = phi i32 [ %73, %cond.true113 ], [ %sub115, %cond.false114 ], !dbg !409
  %sub118 = sub nsw i32 %71, %cond117, !dbg !409
  %cmp119 = icmp sge i32 %sub118, 0, !dbg !409
  br i1 %cmp119, label %cond.true120, label %cond.false128, !dbg !409

cond.true120:                                     ; preds = %cond.end116
  %75 = load i32* %y_base, align 4, !dbg !409
  %76 = load i32* %y_edge, align 4, !dbg !409
  %cmp121 = icmp sge i32 %76, 0, !dbg !409
  br i1 %cmp121, label %cond.true122, label %cond.false123, !dbg !409

cond.true122:                                     ; preds = %cond.true120
  %77 = load i32* %y_edge, align 4, !dbg !409
  br label %cond.end125, !dbg !409

cond.false123:                                    ; preds = %cond.true120
  %78 = load i32* %y_edge, align 4, !dbg !409
  %sub124 = sub nsw i32 0, %78, !dbg !409
  br label %cond.end125, !dbg !409

cond.end125:                                      ; preds = %cond.false123, %cond.true122
  %cond126 = phi i32 [ %77, %cond.true122 ], [ %sub124, %cond.false123 ], !dbg !409
  %sub127 = sub nsw i32 %75, %cond126, !dbg !409
  br label %cond.end137, !dbg !409

cond.false128:                                    ; preds = %cond.end116
  %79 = load i32* %y_base, align 4, !dbg !409
  %80 = load i32* %y_edge, align 4, !dbg !409
  %cmp129 = icmp sge i32 %80, 0, !dbg !409
  br i1 %cmp129, label %cond.true130, label %cond.false131, !dbg !409

cond.true130:                                     ; preds = %cond.false128
  %81 = load i32* %y_edge, align 4, !dbg !409
  br label %cond.end133, !dbg !409

cond.false131:                                    ; preds = %cond.false128
  %82 = load i32* %y_edge, align 4, !dbg !409
  %sub132 = sub nsw i32 0, %82, !dbg !409
  br label %cond.end133, !dbg !409

cond.end133:                                      ; preds = %cond.false131, %cond.true130
  %cond134 = phi i32 [ %81, %cond.true130 ], [ %sub132, %cond.false131 ], !dbg !409
  %sub135 = sub nsw i32 %79, %cond134, !dbg !409
  %sub136 = sub nsw i32 0, %sub135, !dbg !409
  br label %cond.end137, !dbg !409

cond.end137:                                      ; preds = %cond.end133, %cond.end125
  %cond138 = phi i32 [ %sub127, %cond.end125 ], [ %sub136, %cond.end133 ], !dbg !409
  %83 = load i32* %x_base, align 4, !dbg !409
  %84 = load i32* %x_edge, align 4, !dbg !409
  %cmp139 = icmp sge i32 %84, 0, !dbg !409
  br i1 %cmp139, label %cond.true140, label %cond.false141, !dbg !409

cond.true140:                                     ; preds = %cond.end137
  %85 = load i32* %x_edge, align 4, !dbg !409
  br label %cond.end143, !dbg !409

cond.false141:                                    ; preds = %cond.end137
  %86 = load i32* %x_edge, align 4, !dbg !409
  %sub142 = sub nsw i32 0, %86, !dbg !409
  br label %cond.end143, !dbg !409

cond.end143:                                      ; preds = %cond.false141, %cond.true140
  %cond144 = phi i32 [ %85, %cond.true140 ], [ %sub142, %cond.false141 ], !dbg !409
  %sub145 = sub nsw i32 %83, %cond144, !dbg !409
  %cmp146 = icmp sge i32 %sub145, 0, !dbg !409
  br i1 %cmp146, label %cond.true147, label %cond.false155, !dbg !409

cond.true147:                                     ; preds = %cond.end143
  %87 = load i32* %x_base, align 4, !dbg !409
  %88 = load i32* %x_edge, align 4, !dbg !409
  %cmp148 = icmp sge i32 %88, 0, !dbg !409
  br i1 %cmp148, label %cond.true149, label %cond.false150, !dbg !409

cond.true149:                                     ; preds = %cond.true147
  %89 = load i32* %x_edge, align 4, !dbg !409
  br label %cond.end152, !dbg !409

cond.false150:                                    ; preds = %cond.true147
  %90 = load i32* %x_edge, align 4, !dbg !409
  %sub151 = sub nsw i32 0, %90, !dbg !409
  br label %cond.end152, !dbg !409

cond.end152:                                      ; preds = %cond.false150, %cond.true149
  %cond153 = phi i32 [ %89, %cond.true149 ], [ %sub151, %cond.false150 ], !dbg !409
  %sub154 = sub nsw i32 %87, %cond153, !dbg !409
  br label %cond.end164, !dbg !409

cond.false155:                                    ; preds = %cond.end143
  %91 = load i32* %x_base, align 4, !dbg !409
  %92 = load i32* %x_edge, align 4, !dbg !409
  %cmp156 = icmp sge i32 %92, 0, !dbg !409
  br i1 %cmp156, label %cond.true157, label %cond.false158, !dbg !409

cond.true157:                                     ; preds = %cond.false155
  %93 = load i32* %x_edge, align 4, !dbg !409
  br label %cond.end160, !dbg !409

cond.false158:                                    ; preds = %cond.false155
  %94 = load i32* %x_edge, align 4, !dbg !409
  %sub159 = sub nsw i32 0, %94, !dbg !409
  br label %cond.end160, !dbg !409

cond.end160:                                      ; preds = %cond.false158, %cond.true157
  %cond161 = phi i32 [ %93, %cond.true157 ], [ %sub159, %cond.false158 ], !dbg !409
  %sub162 = sub nsw i32 %91, %cond161, !dbg !409
  %sub163 = sub nsw i32 0, %sub162, !dbg !409
  br label %cond.end164, !dbg !409

cond.end164:                                      ; preds = %cond.end160, %cond.end152
  %cond165 = phi i32 [ %sub154, %cond.end152 ], [ %sub163, %cond.end160 ], !dbg !409
  %add166 = add nsw i32 %cond138, %cond165, !dbg !409
  %95 = load float** %result.addr, align 4, !dbg !409
  %arrayidx167 = getelementptr inbounds float* %95, i32 %add166, !dbg !409
  %96 = load float* %arrayidx167, !dbg !409
  %sub168 = fsub float %96, %70, !dbg !409
  store float %sub168, float* %arrayidx167, !dbg !409
  %97 = load i32* %x_filt, align 4, !dbg !411
  %98 = load float** %filt.addr, align 4, !dbg !411
  %arrayidx169 = getelementptr inbounds float* %98, i32 %97, !dbg !411
  %99 = load float* %arrayidx169, !dbg !411
  %100 = load i32* %x_filt, align 4, !dbg !411
  %101 = load float** %filt.addr, align 4, !dbg !411
  %arrayidx170 = getelementptr inbounds float* %101, i32 %100, !dbg !411
  %102 = load float* %arrayidx170, !dbg !411
  %add171 = fadd float %99, %102, !dbg !411
  %103 = load i32* %y_base, align 4, !dbg !411
  %104 = load i32* %y_edge, align 4, !dbg !411
  %add172 = add nsw i32 %103, %104, !dbg !411
  %cmp173 = icmp slt i32 %add172, 0, !dbg !411
  br i1 %cmp173, label %cond.true174, label %cond.false175, !dbg !411

cond.true174:                                     ; preds = %cond.end164
  br label %cond.end184, !dbg !411

cond.false175:                                    ; preds = %cond.end164
  %105 = load i32* %y_base, align 4, !dbg !411
  %106 = load i32* %y_edge, align 4, !dbg !411
  %add176 = add nsw i32 %105, %106, !dbg !411
  %107 = load i32* %y_dim.addr, align 4, !dbg !411
  %cmp177 = icmp sge i32 %add176, %107, !dbg !411
  br i1 %cmp177, label %cond.true178, label %cond.false180, !dbg !411

cond.true178:                                     ; preds = %cond.false175
  %108 = load i32* %y_dim.addr, align 4, !dbg !411
  %sub179 = sub nsw i32 %108, 1, !dbg !411
  br label %cond.end182, !dbg !411

cond.false180:                                    ; preds = %cond.false175
  %109 = load i32* %y_base, align 4, !dbg !411
  %110 = load i32* %y_edge, align 4, !dbg !411
  %add181 = add nsw i32 %109, %110, !dbg !411
  br label %cond.end182, !dbg !411

cond.end182:                                      ; preds = %cond.false180, %cond.true178
  %cond183 = phi i32 [ %sub179, %cond.true178 ], [ %add181, %cond.false180 ], !dbg !411
  br label %cond.end184, !dbg !411

cond.end184:                                      ; preds = %cond.end182, %cond.true174
  %cond185 = phi i32 [ 0, %cond.true174 ], [ %cond183, %cond.end182 ], !dbg !411
  %111 = load i32* %x_base, align 4, !dbg !411
  %112 = load i32* %x_edge, align 4, !dbg !411
  %add186 = add nsw i32 %111, %112, !dbg !411
  %cmp187 = icmp slt i32 %add186, 0, !dbg !411
  br i1 %cmp187, label %cond.true188, label %cond.false189, !dbg !411

cond.true188:                                     ; preds = %cond.end184
  br label %cond.end198, !dbg !411

cond.false189:                                    ; preds = %cond.end184
  %113 = load i32* %x_base, align 4, !dbg !411
  %114 = load i32* %x_edge, align 4, !dbg !411
  %add190 = add nsw i32 %113, %114, !dbg !411
  %115 = load i32* %x_dim.addr, align 4, !dbg !411
  %cmp191 = icmp sge i32 %add190, %115, !dbg !411
  br i1 %cmp191, label %cond.true192, label %cond.false194, !dbg !411

cond.true192:                                     ; preds = %cond.false189
  %116 = load i32* %x_dim.addr, align 4, !dbg !411
  %sub193 = sub nsw i32 %116, 1, !dbg !411
  br label %cond.end196, !dbg !411

cond.false194:                                    ; preds = %cond.false189
  %117 = load i32* %x_base, align 4, !dbg !411
  %118 = load i32* %x_edge, align 4, !dbg !411
  %add195 = add nsw i32 %117, %118, !dbg !411
  br label %cond.end196, !dbg !411

cond.end196:                                      ; preds = %cond.false194, %cond.true192
  %cond197 = phi i32 [ %sub193, %cond.true192 ], [ %add195, %cond.false194 ], !dbg !411
  br label %cond.end198, !dbg !411

cond.end198:                                      ; preds = %cond.end196, %cond.true188
  %cond199 = phi i32 [ 0, %cond.true188 ], [ %cond197, %cond.end196 ], !dbg !411
  %add200 = add nsw i32 %cond185, %cond199, !dbg !411
  %119 = load float** %result.addr, align 4, !dbg !411
  %arrayidx201 = getelementptr inbounds float* %119, i32 %add200, !dbg !411
  %120 = load float* %arrayidx201, !dbg !411
  %add202 = fadd float %120, %add171, !dbg !411
  store float %add202, float* %arrayidx201, !dbg !411
  br label %if.end262, !dbg !412

if.else203:                                       ; preds = %lor.end107
  %121 = load i32* %x_filt, align 4, !dbg !413
  %122 = load float** %filt.addr, align 4, !dbg !413
  %arrayidx204 = getelementptr inbounds float* %122, i32 %121, !dbg !413
  %123 = load float* %arrayidx204, !dbg !413
  %124 = load i32* %y_base, align 4, !dbg !413
  %125 = load i32* %y_edge, align 4, !dbg !413
  %cmp205 = icmp sge i32 %125, 0, !dbg !413
  br i1 %cmp205, label %cond.true206, label %cond.false207, !dbg !413

cond.true206:                                     ; preds = %if.else203
  %126 = load i32* %y_edge, align 4, !dbg !413
  br label %cond.end209, !dbg !413

cond.false207:                                    ; preds = %if.else203
  %127 = load i32* %y_edge, align 4, !dbg !413
  %sub208 = sub nsw i32 0, %127, !dbg !413
  br label %cond.end209, !dbg !413

cond.end209:                                      ; preds = %cond.false207, %cond.true206
  %cond210 = phi i32 [ %126, %cond.true206 ], [ %sub208, %cond.false207 ], !dbg !413
  %sub211 = sub nsw i32 %124, %cond210, !dbg !413
  %cmp212 = icmp sge i32 %sub211, 0, !dbg !413
  br i1 %cmp212, label %cond.true213, label %cond.false221, !dbg !413

cond.true213:                                     ; preds = %cond.end209
  %128 = load i32* %y_base, align 4, !dbg !413
  %129 = load i32* %y_edge, align 4, !dbg !413
  %cmp214 = icmp sge i32 %129, 0, !dbg !413
  br i1 %cmp214, label %cond.true215, label %cond.false216, !dbg !413

cond.true215:                                     ; preds = %cond.true213
  %130 = load i32* %y_edge, align 4, !dbg !413
  br label %cond.end218, !dbg !413

cond.false216:                                    ; preds = %cond.true213
  %131 = load i32* %y_edge, align 4, !dbg !413
  %sub217 = sub nsw i32 0, %131, !dbg !413
  br label %cond.end218, !dbg !413

cond.end218:                                      ; preds = %cond.false216, %cond.true215
  %cond219 = phi i32 [ %130, %cond.true215 ], [ %sub217, %cond.false216 ], !dbg !413
  %sub220 = sub nsw i32 %128, %cond219, !dbg !413
  br label %cond.end230, !dbg !413

cond.false221:                                    ; preds = %cond.end209
  %132 = load i32* %y_base, align 4, !dbg !413
  %133 = load i32* %y_edge, align 4, !dbg !413
  %cmp222 = icmp sge i32 %133, 0, !dbg !413
  br i1 %cmp222, label %cond.true223, label %cond.false224, !dbg !413

cond.true223:                                     ; preds = %cond.false221
  %134 = load i32* %y_edge, align 4, !dbg !413
  br label %cond.end226, !dbg !413

cond.false224:                                    ; preds = %cond.false221
  %135 = load i32* %y_edge, align 4, !dbg !413
  %sub225 = sub nsw i32 0, %135, !dbg !413
  br label %cond.end226, !dbg !413

cond.end226:                                      ; preds = %cond.false224, %cond.true223
  %cond227 = phi i32 [ %134, %cond.true223 ], [ %sub225, %cond.false224 ], !dbg !413
  %sub228 = sub nsw i32 %132, %cond227, !dbg !413
  %sub229 = sub nsw i32 0, %sub228, !dbg !413
  br label %cond.end230, !dbg !413

cond.end230:                                      ; preds = %cond.end226, %cond.end218
  %cond231 = phi i32 [ %sub220, %cond.end218 ], [ %sub229, %cond.end226 ], !dbg !413
  %136 = load i32* %x_base, align 4, !dbg !413
  %137 = load i32* %x_edge, align 4, !dbg !413
  %cmp232 = icmp sge i32 %137, 0, !dbg !413
  br i1 %cmp232, label %cond.true233, label %cond.false234, !dbg !413

cond.true233:                                     ; preds = %cond.end230
  %138 = load i32* %x_edge, align 4, !dbg !413
  br label %cond.end236, !dbg !413

cond.false234:                                    ; preds = %cond.end230
  %139 = load i32* %x_edge, align 4, !dbg !413
  %sub235 = sub nsw i32 0, %139, !dbg !413
  br label %cond.end236, !dbg !413

cond.end236:                                      ; preds = %cond.false234, %cond.true233
  %cond237 = phi i32 [ %138, %cond.true233 ], [ %sub235, %cond.false234 ], !dbg !413
  %sub238 = sub nsw i32 %136, %cond237, !dbg !413
  %cmp239 = icmp sge i32 %sub238, 0, !dbg !413
  br i1 %cmp239, label %cond.true240, label %cond.false248, !dbg !413

cond.true240:                                     ; preds = %cond.end236
  %140 = load i32* %x_base, align 4, !dbg !413
  %141 = load i32* %x_edge, align 4, !dbg !413
  %cmp241 = icmp sge i32 %141, 0, !dbg !413
  br i1 %cmp241, label %cond.true242, label %cond.false243, !dbg !413

cond.true242:                                     ; preds = %cond.true240
  %142 = load i32* %x_edge, align 4, !dbg !413
  br label %cond.end245, !dbg !413

cond.false243:                                    ; preds = %cond.true240
  %143 = load i32* %x_edge, align 4, !dbg !413
  %sub244 = sub nsw i32 0, %143, !dbg !413
  br label %cond.end245, !dbg !413

cond.end245:                                      ; preds = %cond.false243, %cond.true242
  %cond246 = phi i32 [ %142, %cond.true242 ], [ %sub244, %cond.false243 ], !dbg !413
  %sub247 = sub nsw i32 %140, %cond246, !dbg !413
  br label %cond.end257, !dbg !413

cond.false248:                                    ; preds = %cond.end236
  %144 = load i32* %x_base, align 4, !dbg !413
  %145 = load i32* %x_edge, align 4, !dbg !413
  %cmp249 = icmp sge i32 %145, 0, !dbg !413
  br i1 %cmp249, label %cond.true250, label %cond.false251, !dbg !413

cond.true250:                                     ; preds = %cond.false248
  %146 = load i32* %x_edge, align 4, !dbg !413
  br label %cond.end253, !dbg !413

cond.false251:                                    ; preds = %cond.false248
  %147 = load i32* %x_edge, align 4, !dbg !413
  %sub252 = sub nsw i32 0, %147, !dbg !413
  br label %cond.end253, !dbg !413

cond.end253:                                      ; preds = %cond.false251, %cond.true250
  %cond254 = phi i32 [ %146, %cond.true250 ], [ %sub252, %cond.false251 ], !dbg !413
  %sub255 = sub nsw i32 %144, %cond254, !dbg !413
  %sub256 = sub nsw i32 0, %sub255, !dbg !413
  br label %cond.end257, !dbg !413

cond.end257:                                      ; preds = %cond.end253, %cond.end245
  %cond258 = phi i32 [ %sub247, %cond.end245 ], [ %sub256, %cond.end253 ], !dbg !413
  %add259 = add nsw i32 %cond231, %cond258, !dbg !413
  %148 = load float** %result.addr, align 4, !dbg !413
  %arrayidx260 = getelementptr inbounds float* %148, i32 %add259, !dbg !413
  %149 = load float* %arrayidx260, !dbg !413
  %add261 = fadd float %149, %123, !dbg !413
  store float %add261, float* %arrayidx260, !dbg !413
  br label %if.end262

if.end262:                                        ; preds = %cond.end257, %cond.end198
  br label %for.inc263

for.inc263:                                       ; preds = %if.end262
  %150 = load i32* %x_filt, align 4, !dbg !414
  %inc264 = add nsw i32 %150, 1, !dbg !414
  store i32 %inc264, i32* %x_filt, align 4, !dbg !414
  %151 = load i32* %x_edge, align 4, !dbg !414
  %inc265 = add nsw i32 %151, 1, !dbg !414
  store i32 %inc265, i32* %x_edge, align 4, !dbg !414
  br label %for.cond64, !dbg !414

for.end266:                                       ; preds = %for.cond64
  br label %for.inc267, !dbg !415

for.inc267:                                       ; preds = %for.end266
  %152 = load i32* %x_dim.addr, align 4, !dbg !416
  %153 = load i32* %y_filt, align 4, !dbg !416
  %add268 = add nsw i32 %153, %152, !dbg !416
  store i32 %add268, i32* %y_filt, align 4, !dbg !416
  %154 = load i32* %x_dim.addr, align 4, !dbg !416
  %155 = load i32* %y_edge, align 4, !dbg !416
  %add269 = add nsw i32 %155, %154, !dbg !416
  store i32 %add269, i32* %y_edge, align 4, !dbg !416
  br label %for.cond60, !dbg !416

for.end270:                                       ; preds = %for.cond60
  %156 = load i32* %retval, !dbg !417
  ret i32 %156, !dbg !417
}

define arm_aapcscc i32 @nocompute(float* %filt, i32 %x_dim, i32 %y_dim, i32 %x_pos, i32 %y_pos, float* %result, i32 %f_or_e) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %filt.addr = alloca float*, align 4
  %x_dim.addr = alloca i32, align 4
  %y_dim.addr = alloca i32, align 4
  %x_pos.addr = alloca i32, align 4
  %y_pos.addr = alloca i32, align 4
  %result.addr = alloca float*, align 4
  %f_or_e.addr = alloca i32, align 4
  %i = alloca i32, align 4
  %size = alloca i32, align 4
  store float* %filt, float** %filt.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %filt.addr}, metadata !418), !dbg !419
  store i32 %x_dim, i32* %x_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_dim.addr}, metadata !420), !dbg !421
  store i32 %y_dim, i32* %y_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_dim.addr}, metadata !422), !dbg !423
  store i32 %x_pos, i32* %x_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_pos.addr}, metadata !424), !dbg !425
  store i32 %y_pos, i32* %y_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_pos.addr}, metadata !426), !dbg !427
  store float* %result, float** %result.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result.addr}, metadata !428), !dbg !429
  store i32 %f_or_e, i32* %f_or_e.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %f_or_e.addr}, metadata !430), !dbg !431
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !432), !dbg !434
  call void @llvm.dbg.declare(metadata !{i32* %size}, metadata !435), !dbg !436
  %0 = load i32* %x_dim.addr, align 4, !dbg !437
  %1 = load i32* %y_dim.addr, align 4, !dbg !437
  %mul = mul nsw i32 %0, %1, !dbg !437
  store i32 %mul, i32* %size, align 4, !dbg !437
  %2 = load i32* %x_pos.addr, align 4, !dbg !438
  %cmp = icmp sgt i32 %2, 1, !dbg !438
  br i1 %cmp, label %if.then, label %lor.lhs.false, !dbg !438

lor.lhs.false:                                    ; preds = %entry
  %3 = load i32* %x_pos.addr, align 4, !dbg !438
  %cmp1 = icmp slt i32 %3, -1, !dbg !438
  br i1 %cmp1, label %if.then, label %lor.lhs.false2, !dbg !438

lor.lhs.false2:                                   ; preds = %lor.lhs.false
  %4 = load i32* %y_pos.addr, align 4, !dbg !438
  %cmp3 = icmp sgt i32 %4, 1, !dbg !438
  br i1 %cmp3, label %if.then, label %lor.lhs.false4, !dbg !438

lor.lhs.false4:                                   ; preds = %lor.lhs.false2
  %5 = load i32* %y_pos.addr, align 4, !dbg !438
  %cmp5 = icmp slt i32 %5, -1, !dbg !438
  br i1 %cmp5, label %if.then, label %if.else, !dbg !438

if.then:                                          ; preds = %lor.lhs.false4, %lor.lhs.false2, %lor.lhs.false, %entry
  store i32 0, i32* %i, align 4, !dbg !439
  br label %for.cond, !dbg !439

for.cond:                                         ; preds = %for.inc, %if.then
  %6 = load i32* %i, align 4, !dbg !439
  %7 = load i32* %size, align 4, !dbg !439
  %cmp6 = icmp slt i32 %6, %7, !dbg !439
  br i1 %cmp6, label %for.body, label %for.end, !dbg !439

for.body:                                         ; preds = %for.cond
  %8 = load i32* %i, align 4, !dbg !441
  %9 = load float** %result.addr, align 4, !dbg !441
  %arrayidx = getelementptr inbounds float* %9, i32 %8, !dbg !441
  store float 0.000000e+00, float* %arrayidx, !dbg !441
  br label %for.inc, !dbg !441

for.inc:                                          ; preds = %for.body
  %10 = load i32* %i, align 4, !dbg !442
  %inc = add nsw i32 %10, 1, !dbg !442
  store i32 %inc, i32* %i, align 4, !dbg !442
  br label %for.cond, !dbg !442

for.end:                                          ; preds = %for.cond
  br label %if.end, !dbg !443

if.else:                                          ; preds = %lor.lhs.false4
  store i32 0, i32* %i, align 4, !dbg !444
  br label %for.cond7, !dbg !444

for.cond7:                                        ; preds = %for.inc12, %if.else
  %11 = load i32* %i, align 4, !dbg !444
  %12 = load i32* %size, align 4, !dbg !444
  %cmp8 = icmp slt i32 %11, %12, !dbg !444
  br i1 %cmp8, label %for.body9, label %for.end14, !dbg !444

for.body9:                                        ; preds = %for.cond7
  %13 = load i32* %i, align 4, !dbg !446
  %14 = load float** %filt.addr, align 4, !dbg !446
  %arrayidx10 = getelementptr inbounds float* %14, i32 %13, !dbg !446
  %15 = load float* %arrayidx10, !dbg !446
  %16 = load i32* %i, align 4, !dbg !446
  %17 = load float** %result.addr, align 4, !dbg !446
  %arrayidx11 = getelementptr inbounds float* %17, i32 %16, !dbg !446
  store float %15, float* %arrayidx11, !dbg !446
  br label %for.inc12, !dbg !446

for.inc12:                                        ; preds = %for.body9
  %18 = load i32* %i, align 4, !dbg !447
  %inc13 = add nsw i32 %18, 1, !dbg !447
  store i32 %inc13, i32* %i, align 4, !dbg !447
  br label %for.cond7, !dbg !447

for.end14:                                        ; preds = %for.cond7
  br label %if.end

if.end:                                           ; preds = %for.end14, %for.end
  %19 = load i32* %retval, !dbg !448
  ret i32 %19, !dbg !448
}

define arm_aapcscc i32 @ereflect(float* %filt, i32 %x_dim, i32 %y_dim, i32 %x_pos, i32 %y_pos, float* %result, i32 %f_or_e) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %filt.addr = alloca float*, align 4
  %x_dim.addr = alloca i32, align 4
  %y_dim.addr = alloca i32, align 4
  %x_pos.addr = alloca i32, align 4
  %y_pos.addr = alloca i32, align 4
  %result.addr = alloca float*, align 4
  %f_or_e.addr = alloca i32, align 4
  %y_filt = alloca i32, align 4
  %x_filt = alloca i32, align 4
  %y_edge = alloca i32, align 4
  %x_edge = alloca i32, align 4
  %x_base = alloca i32, align 4
  %y_base = alloca i32, align 4
  %filt_sz = alloca i32, align 4
  %x_edge_dist = alloca i32, align 4
  %y_edge_dist = alloca i32, align 4
  %i = alloca i32, align 4
  %norm = alloca double, align 8
  %onorm = alloca double, align 8
  store float* %filt, float** %filt.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %filt.addr}, metadata !449), !dbg !450
  store i32 %x_dim, i32* %x_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_dim.addr}, metadata !451), !dbg !452
  store i32 %y_dim, i32* %y_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_dim.addr}, metadata !453), !dbg !454
  store i32 %x_pos, i32* %x_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_pos.addr}, metadata !455), !dbg !456
  store i32 %y_pos, i32* %y_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_pos.addr}, metadata !457), !dbg !458
  store float* %result, float** %result.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result.addr}, metadata !459), !dbg !460
  store i32 %f_or_e, i32* %f_or_e.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %f_or_e.addr}, metadata !461), !dbg !462
  call void @llvm.dbg.declare(metadata !{i32* %y_filt}, metadata !463), !dbg !465
  call void @llvm.dbg.declare(metadata !{i32* %x_filt}, metadata !466), !dbg !467
  call void @llvm.dbg.declare(metadata !{i32* %y_edge}, metadata !468), !dbg !469
  call void @llvm.dbg.declare(metadata !{i32* %x_edge}, metadata !470), !dbg !471
  call void @llvm.dbg.declare(metadata !{i32* %x_base}, metadata !472), !dbg !473
  %0 = load i32* %x_pos.addr, align 4, !dbg !474
  %cmp = icmp sgt i32 %0, 0, !dbg !474
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !474

cond.true:                                        ; preds = %entry
  %1 = load i32* %x_dim.addr, align 4, !dbg !474
  %sub = sub nsw i32 %1, 1, !dbg !474
  br label %cond.end, !dbg !474

cond.false:                                       ; preds = %entry
  br label %cond.end, !dbg !474

cond.end:                                         ; preds = %cond.false, %cond.true
  %cond = phi i32 [ %sub, %cond.true ], [ 0, %cond.false ], !dbg !474
  store i32 %cond, i32* %x_base, align 4, !dbg !474
  call void @llvm.dbg.declare(metadata !{i32* %y_base}, metadata !475), !dbg !476
  %2 = load i32* %x_dim.addr, align 4, !dbg !477
  %3 = load i32* %y_pos.addr, align 4, !dbg !477
  %cmp1 = icmp sgt i32 %3, 0, !dbg !477
  br i1 %cmp1, label %cond.true2, label %cond.false4, !dbg !477

cond.true2:                                       ; preds = %cond.end
  %4 = load i32* %y_dim.addr, align 4, !dbg !477
  %sub3 = sub nsw i32 %4, 1, !dbg !477
  br label %cond.end5, !dbg !477

cond.false4:                                      ; preds = %cond.end
  br label %cond.end5, !dbg !477

cond.end5:                                        ; preds = %cond.false4, %cond.true2
  %cond6 = phi i32 [ %sub3, %cond.true2 ], [ 0, %cond.false4 ], !dbg !477
  %mul = mul nsw i32 %2, %cond6, !dbg !477
  store i32 %mul, i32* %y_base, align 4, !dbg !477
  call void @llvm.dbg.declare(metadata !{i32* %filt_sz}, metadata !478), !dbg !479
  %5 = load i32* %x_dim.addr, align 4, !dbg !480
  %6 = load i32* %y_dim.addr, align 4, !dbg !480
  %mul7 = mul nsw i32 %5, %6, !dbg !480
  store i32 %mul7, i32* %filt_sz, align 4, !dbg !480
  call void @llvm.dbg.declare(metadata !{i32* %x_edge_dist}, metadata !481), !dbg !482
  %7 = load i32* %x_pos.addr, align 4, !dbg !483
  %cmp8 = icmp sgt i32 %7, 1, !dbg !483
  br i1 %cmp8, label %cond.true9, label %cond.false11, !dbg !483

cond.true9:                                       ; preds = %cond.end5
  %8 = load i32* %x_pos.addr, align 4, !dbg !483
  %9 = load i32* %x_dim.addr, align 4, !dbg !483
  %sub10 = sub nsw i32 %8, %9, !dbg !483
  br label %cond.end17, !dbg !483

cond.false11:                                     ; preds = %cond.end5
  %10 = load i32* %x_pos.addr, align 4, !dbg !483
  %cmp12 = icmp slt i32 %10, -1, !dbg !483
  br i1 %cmp12, label %cond.true13, label %cond.false14, !dbg !483

cond.true13:                                      ; preds = %cond.false11
  %11 = load i32* %x_pos.addr, align 4, !dbg !483
  %add = add nsw i32 %11, 1, !dbg !483
  br label %cond.end15, !dbg !483

cond.false14:                                     ; preds = %cond.false11
  br label %cond.end15, !dbg !483

cond.end15:                                       ; preds = %cond.false14, %cond.true13
  %cond16 = phi i32 [ %add, %cond.true13 ], [ 0, %cond.false14 ], !dbg !483
  br label %cond.end17, !dbg !483

cond.end17:                                       ; preds = %cond.end15, %cond.true9
  %cond18 = phi i32 [ %sub10, %cond.true9 ], [ %cond16, %cond.end15 ], !dbg !483
  store i32 %cond18, i32* %x_edge_dist, align 4, !dbg !483
  call void @llvm.dbg.declare(metadata !{i32* %y_edge_dist}, metadata !484), !dbg !485
  %12 = load i32* %x_dim.addr, align 4, !dbg !486
  %13 = load i32* %y_pos.addr, align 4, !dbg !486
  %cmp19 = icmp sgt i32 %13, 1, !dbg !486
  br i1 %cmp19, label %cond.true20, label %cond.false22, !dbg !486

cond.true20:                                      ; preds = %cond.end17
  %14 = load i32* %y_pos.addr, align 4, !dbg !486
  %15 = load i32* %y_dim.addr, align 4, !dbg !486
  %sub21 = sub nsw i32 %14, %15, !dbg !486
  br label %cond.end29, !dbg !486

cond.false22:                                     ; preds = %cond.end17
  %16 = load i32* %y_pos.addr, align 4, !dbg !486
  %cmp23 = icmp slt i32 %16, -1, !dbg !486
  br i1 %cmp23, label %cond.true24, label %cond.false26, !dbg !486

cond.true24:                                      ; preds = %cond.false22
  %17 = load i32* %y_pos.addr, align 4, !dbg !486
  %add25 = add nsw i32 %17, 1, !dbg !486
  br label %cond.end27, !dbg !486

cond.false26:                                     ; preds = %cond.false22
  br label %cond.end27, !dbg !486

cond.end27:                                       ; preds = %cond.false26, %cond.true24
  %cond28 = phi i32 [ %add25, %cond.true24 ], [ 0, %cond.false26 ], !dbg !486
  br label %cond.end29, !dbg !486

cond.end29:                                       ; preds = %cond.end27, %cond.true20
  %cond30 = phi i32 [ %sub21, %cond.true20 ], [ %cond28, %cond.end27 ], !dbg !486
  %mul31 = mul nsw i32 %12, %cond30, !dbg !486
  store i32 %mul31, i32* %y_edge_dist, align 4, !dbg !486
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !487), !dbg !488
  call void @llvm.dbg.declare(metadata !{double* %norm}, metadata !489), !dbg !491
  call void @llvm.dbg.declare(metadata !{double* %onorm}, metadata !492), !dbg !493
  store i32 0, i32* %i, align 4, !dbg !494
  br label %for.cond, !dbg !494

for.cond:                                         ; preds = %for.inc, %cond.end29
  %18 = load i32* %i, align 4, !dbg !494
  %19 = load i32* %filt_sz, align 4, !dbg !494
  %cmp32 = icmp slt i32 %18, %19, !dbg !494
  br i1 %cmp32, label %for.body, label %for.end, !dbg !494

for.body:                                         ; preds = %for.cond
  %20 = load i32* %i, align 4, !dbg !496
  %21 = load float** %result.addr, align 4, !dbg !496
  %arrayidx = getelementptr inbounds float* %21, i32 %20, !dbg !496
  store float 0.000000e+00, float* %arrayidx, !dbg !496
  br label %for.inc, !dbg !496

for.inc:                                          ; preds = %for.body
  %22 = load i32* %i, align 4, !dbg !497
  %inc = add nsw i32 %22, 1, !dbg !497
  store i32 %inc, i32* %i, align 4, !dbg !497
  br label %for.cond, !dbg !497

for.end:                                          ; preds = %for.cond
  store i32 0, i32* %y_filt, align 4, !dbg !498
  %23 = load i32* %y_edge_dist, align 4, !dbg !498
  store i32 %23, i32* %y_edge, align 4, !dbg !498
  br label %for.cond33, !dbg !498

for.cond33:                                       ; preds = %for.inc102, %for.end
  %24 = load i32* %y_filt, align 4, !dbg !498
  %25 = load i32* %filt_sz, align 4, !dbg !498
  %cmp34 = icmp slt i32 %24, %25, !dbg !498
  br i1 %cmp34, label %for.body35, label %for.end105, !dbg !498

for.body35:                                       ; preds = %for.cond33
  %26 = load i32* %y_filt, align 4, !dbg !500
  store i32 %26, i32* %x_filt, align 4, !dbg !500
  %27 = load i32* %x_edge_dist, align 4, !dbg !500
  store i32 %27, i32* %x_edge, align 4, !dbg !500
  br label %for.cond36, !dbg !500

for.cond36:                                       ; preds = %for.inc98, %for.body35
  %28 = load i32* %x_filt, align 4, !dbg !500
  %29 = load i32* %y_filt, align 4, !dbg !500
  %30 = load i32* %x_dim.addr, align 4, !dbg !500
  %add37 = add nsw i32 %29, %30, !dbg !500
  %cmp38 = icmp slt i32 %28, %add37, !dbg !500
  br i1 %cmp38, label %for.body39, label %for.end101, !dbg !500

for.body39:                                       ; preds = %for.cond36
  %31 = load i32* %x_filt, align 4, !dbg !502
  %32 = load float** %filt.addr, align 4, !dbg !502
  %arrayidx40 = getelementptr inbounds float* %32, i32 %31, !dbg !502
  %33 = load float* %arrayidx40, !dbg !502
  %34 = load i32* %y_base, align 4, !dbg !502
  %35 = load i32* %y_edge, align 4, !dbg !502
  %cmp41 = icmp sge i32 %35, 0, !dbg !502
  br i1 %cmp41, label %cond.true42, label %cond.false43, !dbg !502

cond.true42:                                      ; preds = %for.body39
  %36 = load i32* %y_edge, align 4, !dbg !502
  br label %cond.end45, !dbg !502

cond.false43:                                     ; preds = %for.body39
  %37 = load i32* %y_edge, align 4, !dbg !502
  %sub44 = sub nsw i32 0, %37, !dbg !502
  br label %cond.end45, !dbg !502

cond.end45:                                       ; preds = %cond.false43, %cond.true42
  %cond46 = phi i32 [ %36, %cond.true42 ], [ %sub44, %cond.false43 ], !dbg !502
  %sub47 = sub nsw i32 %34, %cond46, !dbg !502
  %cmp48 = icmp sge i32 %sub47, 0, !dbg !502
  br i1 %cmp48, label %cond.true49, label %cond.false57, !dbg !502

cond.true49:                                      ; preds = %cond.end45
  %38 = load i32* %y_base, align 4, !dbg !502
  %39 = load i32* %y_edge, align 4, !dbg !502
  %cmp50 = icmp sge i32 %39, 0, !dbg !502
  br i1 %cmp50, label %cond.true51, label %cond.false52, !dbg !502

cond.true51:                                      ; preds = %cond.true49
  %40 = load i32* %y_edge, align 4, !dbg !502
  br label %cond.end54, !dbg !502

cond.false52:                                     ; preds = %cond.true49
  %41 = load i32* %y_edge, align 4, !dbg !502
  %sub53 = sub nsw i32 0, %41, !dbg !502
  br label %cond.end54, !dbg !502

cond.end54:                                       ; preds = %cond.false52, %cond.true51
  %cond55 = phi i32 [ %40, %cond.true51 ], [ %sub53, %cond.false52 ], !dbg !502
  %sub56 = sub nsw i32 %38, %cond55, !dbg !502
  br label %cond.end66, !dbg !502

cond.false57:                                     ; preds = %cond.end45
  %42 = load i32* %y_base, align 4, !dbg !502
  %43 = load i32* %y_edge, align 4, !dbg !502
  %cmp58 = icmp sge i32 %43, 0, !dbg !502
  br i1 %cmp58, label %cond.true59, label %cond.false60, !dbg !502

cond.true59:                                      ; preds = %cond.false57
  %44 = load i32* %y_edge, align 4, !dbg !502
  br label %cond.end62, !dbg !502

cond.false60:                                     ; preds = %cond.false57
  %45 = load i32* %y_edge, align 4, !dbg !502
  %sub61 = sub nsw i32 0, %45, !dbg !502
  br label %cond.end62, !dbg !502

cond.end62:                                       ; preds = %cond.false60, %cond.true59
  %cond63 = phi i32 [ %44, %cond.true59 ], [ %sub61, %cond.false60 ], !dbg !502
  %sub64 = sub nsw i32 %42, %cond63, !dbg !502
  %sub65 = sub nsw i32 0, %sub64, !dbg !502
  br label %cond.end66, !dbg !502

cond.end66:                                       ; preds = %cond.end62, %cond.end54
  %cond67 = phi i32 [ %sub56, %cond.end54 ], [ %sub65, %cond.end62 ], !dbg !502
  %46 = load i32* %x_base, align 4, !dbg !502
  %47 = load i32* %x_edge, align 4, !dbg !502
  %cmp68 = icmp sge i32 %47, 0, !dbg !502
  br i1 %cmp68, label %cond.true69, label %cond.false70, !dbg !502

cond.true69:                                      ; preds = %cond.end66
  %48 = load i32* %x_edge, align 4, !dbg !502
  br label %cond.end72, !dbg !502

cond.false70:                                     ; preds = %cond.end66
  %49 = load i32* %x_edge, align 4, !dbg !502
  %sub71 = sub nsw i32 0, %49, !dbg !502
  br label %cond.end72, !dbg !502

cond.end72:                                       ; preds = %cond.false70, %cond.true69
  %cond73 = phi i32 [ %48, %cond.true69 ], [ %sub71, %cond.false70 ], !dbg !502
  %sub74 = sub nsw i32 %46, %cond73, !dbg !502
  %cmp75 = icmp sge i32 %sub74, 0, !dbg !502
  br i1 %cmp75, label %cond.true76, label %cond.false84, !dbg !502

cond.true76:                                      ; preds = %cond.end72
  %50 = load i32* %x_base, align 4, !dbg !502
  %51 = load i32* %x_edge, align 4, !dbg !502
  %cmp77 = icmp sge i32 %51, 0, !dbg !502
  br i1 %cmp77, label %cond.true78, label %cond.false79, !dbg !502

cond.true78:                                      ; preds = %cond.true76
  %52 = load i32* %x_edge, align 4, !dbg !502
  br label %cond.end81, !dbg !502

cond.false79:                                     ; preds = %cond.true76
  %53 = load i32* %x_edge, align 4, !dbg !502
  %sub80 = sub nsw i32 0, %53, !dbg !502
  br label %cond.end81, !dbg !502

cond.end81:                                       ; preds = %cond.false79, %cond.true78
  %cond82 = phi i32 [ %52, %cond.true78 ], [ %sub80, %cond.false79 ], !dbg !502
  %sub83 = sub nsw i32 %50, %cond82, !dbg !502
  br label %cond.end93, !dbg !502

cond.false84:                                     ; preds = %cond.end72
  %54 = load i32* %x_base, align 4, !dbg !502
  %55 = load i32* %x_edge, align 4, !dbg !502
  %cmp85 = icmp sge i32 %55, 0, !dbg !502
  br i1 %cmp85, label %cond.true86, label %cond.false87, !dbg !502

cond.true86:                                      ; preds = %cond.false84
  %56 = load i32* %x_edge, align 4, !dbg !502
  br label %cond.end89, !dbg !502

cond.false87:                                     ; preds = %cond.false84
  %57 = load i32* %x_edge, align 4, !dbg !502
  %sub88 = sub nsw i32 0, %57, !dbg !502
  br label %cond.end89, !dbg !502

cond.end89:                                       ; preds = %cond.false87, %cond.true86
  %cond90 = phi i32 [ %56, %cond.true86 ], [ %sub88, %cond.false87 ], !dbg !502
  %sub91 = sub nsw i32 %54, %cond90, !dbg !502
  %sub92 = sub nsw i32 0, %sub91, !dbg !502
  br label %cond.end93, !dbg !502

cond.end93:                                       ; preds = %cond.end89, %cond.end81
  %cond94 = phi i32 [ %sub83, %cond.end81 ], [ %sub92, %cond.end89 ], !dbg !502
  %add95 = add nsw i32 %cond67, %cond94, !dbg !502
  %58 = load float** %result.addr, align 4, !dbg !502
  %arrayidx96 = getelementptr inbounds float* %58, i32 %add95, !dbg !502
  %59 = load float* %arrayidx96, !dbg !502
  %add97 = fadd float %59, %33, !dbg !502
  store float %add97, float* %arrayidx96, !dbg !502
  br label %for.inc98, !dbg !502

for.inc98:                                        ; preds = %cond.end93
  %60 = load i32* %x_filt, align 4, !dbg !503
  %inc99 = add nsw i32 %60, 1, !dbg !503
  store i32 %inc99, i32* %x_filt, align 4, !dbg !503
  %61 = load i32* %x_edge, align 4, !dbg !503
  %inc100 = add nsw i32 %61, 1, !dbg !503
  store i32 %inc100, i32* %x_edge, align 4, !dbg !503
  br label %for.cond36, !dbg !503

for.end101:                                       ; preds = %for.cond36
  br label %for.inc102, !dbg !504

for.inc102:                                       ; preds = %for.end101
  %62 = load i32* %x_dim.addr, align 4, !dbg !505
  %63 = load i32* %y_filt, align 4, !dbg !505
  %add103 = add nsw i32 %63, %62, !dbg !505
  store i32 %add103, i32* %y_filt, align 4, !dbg !505
  %64 = load i32* %x_dim.addr, align 4, !dbg !505
  %65 = load i32* %y_edge, align 4, !dbg !505
  %add104 = add nsw i32 %65, %64, !dbg !505
  store i32 %add104, i32* %y_edge, align 4, !dbg !505
  br label %for.cond33, !dbg !505

for.end105:                                       ; preds = %for.cond33
  %66 = load i32* %x_pos.addr, align 4, !dbg !506
  %cmp106 = icmp ne i32 %66, 0, !dbg !506
  br i1 %cmp106, label %if.then, label %if.end, !dbg !506

if.then:                                          ; preds = %for.end105
  %67 = load i32* %x_base, align 4, !dbg !507
  store i32 %67, i32* %y_filt, align 4, !dbg !507
  br label %for.cond107, !dbg !507

for.cond107:                                      ; preds = %for.inc113, %if.then
  %68 = load i32* %y_filt, align 4, !dbg !507
  %69 = load i32* %filt_sz, align 4, !dbg !507
  %cmp108 = icmp slt i32 %68, %69, !dbg !507
  br i1 %cmp108, label %for.body109, label %for.end115, !dbg !507

for.body109:                                      ; preds = %for.cond107
  %70 = load i32* %y_filt, align 4, !dbg !509
  %71 = load float** %result.addr, align 4, !dbg !509
  %arrayidx110 = getelementptr inbounds float* %71, i32 %70, !dbg !509
  %72 = load float* %arrayidx110, !dbg !509
  %conv = fpext float %72 to double, !dbg !509
  %mul111 = fmul double %conv, 0x3FF6A09E667F3BCD, !dbg !509
  %conv112 = fptrunc double %mul111 to float, !dbg !509
  store float %conv112, float* %arrayidx110, !dbg !509
  br label %for.inc113, !dbg !509

for.inc113:                                       ; preds = %for.body109
  %73 = load i32* %x_dim.addr, align 4, !dbg !510
  %74 = load i32* %y_filt, align 4, !dbg !510
  %add114 = add nsw i32 %74, %73, !dbg !510
  store i32 %add114, i32* %y_filt, align 4, !dbg !510
  br label %for.cond107, !dbg !510

for.end115:                                       ; preds = %for.cond107
  br label %if.end, !dbg !511

if.end:                                           ; preds = %for.end115, %for.end105
  %75 = load i32* %y_pos.addr, align 4, !dbg !512
  %cmp116 = icmp ne i32 %75, 0, !dbg !512
  br i1 %cmp116, label %if.then118, label %if.end131, !dbg !512

if.then118:                                       ; preds = %if.end
  %76 = load i32* %y_base, align 4, !dbg !513
  store i32 %76, i32* %x_filt, align 4, !dbg !513
  br label %for.cond119, !dbg !513

for.cond119:                                      ; preds = %for.inc128, %if.then118
  %77 = load i32* %x_filt, align 4, !dbg !513
  %78 = load i32* %y_base, align 4, !dbg !513
  %79 = load i32* %x_dim.addr, align 4, !dbg !513
  %add120 = add nsw i32 %78, %79, !dbg !513
  %cmp121 = icmp slt i32 %77, %add120, !dbg !513
  br i1 %cmp121, label %for.body123, label %for.end130, !dbg !513

for.body123:                                      ; preds = %for.cond119
  %80 = load i32* %x_filt, align 4, !dbg !515
  %81 = load float** %result.addr, align 4, !dbg !515
  %arrayidx124 = getelementptr inbounds float* %81, i32 %80, !dbg !515
  %82 = load float* %arrayidx124, !dbg !515
  %conv125 = fpext float %82 to double, !dbg !515
  %mul126 = fmul double %conv125, 0x3FF6A09E667F3BCD, !dbg !515
  %conv127 = fptrunc double %mul126 to float, !dbg !515
  store float %conv127, float* %arrayidx124, !dbg !515
  br label %for.inc128, !dbg !515

for.inc128:                                       ; preds = %for.body123
  %83 = load i32* %x_filt, align 4, !dbg !516
  %inc129 = add nsw i32 %83, 1, !dbg !516
  store i32 %inc129, i32* %x_filt, align 4, !dbg !516
  br label %for.cond119, !dbg !516

for.end130:                                       ; preds = %for.cond119
  br label %if.end131, !dbg !517

if.end131:                                        ; preds = %for.end130, %if.end
  store double 0.000000e+00, double* %norm, align 8, !dbg !518
  store i32 0, i32* %i, align 4, !dbg !518
  br label %for.cond132, !dbg !518

for.cond132:                                      ; preds = %for.inc141, %if.end131
  %84 = load i32* %i, align 4, !dbg !518
  %85 = load i32* %filt_sz, align 4, !dbg !518
  %cmp133 = icmp slt i32 %84, %85, !dbg !518
  br i1 %cmp133, label %for.body135, label %for.end143, !dbg !518

for.body135:                                      ; preds = %for.cond132
  %86 = load i32* %i, align 4, !dbg !520
  %87 = load float** %result.addr, align 4, !dbg !520
  %arrayidx136 = getelementptr inbounds float* %87, i32 %86, !dbg !520
  %88 = load float* %arrayidx136, !dbg !520
  %89 = load i32* %i, align 4, !dbg !520
  %90 = load float** %result.addr, align 4, !dbg !520
  %arrayidx137 = getelementptr inbounds float* %90, i32 %89, !dbg !520
  %91 = load float* %arrayidx137, !dbg !520
  %mul138 = fmul float %88, %91, !dbg !520
  %conv139 = fpext float %mul138 to double, !dbg !520
  %92 = load double* %norm, align 8, !dbg !520
  %add140 = fadd double %92, %conv139, !dbg !520
  store double %add140, double* %norm, align 8, !dbg !520
  br label %for.inc141, !dbg !520

for.inc141:                                       ; preds = %for.body135
  %93 = load i32* %i, align 4, !dbg !521
  %inc142 = add nsw i32 %93, 1, !dbg !521
  store i32 %inc142, i32* %i, align 4, !dbg !521
  br label %for.cond132, !dbg !521

for.end143:                                       ; preds = %for.cond132
  %94 = load double* %norm, align 8, !dbg !522
  %call = call arm_aapcscc  double @sqrt(double %94) nounwind, !dbg !522
  store double %call, double* %norm, align 8, !dbg !522
  store double 0.000000e+00, double* %onorm, align 8, !dbg !523
  store i32 0, i32* %i, align 4, !dbg !523
  br label %for.cond144, !dbg !523

for.cond144:                                      ; preds = %for.inc153, %for.end143
  %95 = load i32* %i, align 4, !dbg !523
  %96 = load i32* %filt_sz, align 4, !dbg !523
  %cmp145 = icmp slt i32 %95, %96, !dbg !523
  br i1 %cmp145, label %for.body147, label %for.end155, !dbg !523

for.body147:                                      ; preds = %for.cond144
  %97 = load i32* %i, align 4, !dbg !525
  %98 = load float** %filt.addr, align 4, !dbg !525
  %arrayidx148 = getelementptr inbounds float* %98, i32 %97, !dbg !525
  %99 = load float* %arrayidx148, !dbg !525
  %100 = load i32* %i, align 4, !dbg !525
  %101 = load float** %filt.addr, align 4, !dbg !525
  %arrayidx149 = getelementptr inbounds float* %101, i32 %100, !dbg !525
  %102 = load float* %arrayidx149, !dbg !525
  %mul150 = fmul float %99, %102, !dbg !525
  %conv151 = fpext float %mul150 to double, !dbg !525
  %103 = load double* %onorm, align 8, !dbg !525
  %add152 = fadd double %103, %conv151, !dbg !525
  store double %add152, double* %onorm, align 8, !dbg !525
  br label %for.inc153, !dbg !525

for.inc153:                                       ; preds = %for.body147
  %104 = load i32* %i, align 4, !dbg !526
  %inc154 = add nsw i32 %104, 1, !dbg !526
  store i32 %inc154, i32* %i, align 4, !dbg !526
  br label %for.cond144, !dbg !526

for.end155:                                       ; preds = %for.cond144
  %105 = load double* %onorm, align 8, !dbg !527
  %call156 = call arm_aapcscc  double @sqrt(double %105) nounwind, !dbg !527
  store double %call156, double* %onorm, align 8, !dbg !527
  %106 = load double* %norm, align 8, !dbg !528
  %107 = load double* %onorm, align 8, !dbg !528
  %div = fdiv double %106, %107, !dbg !528
  store double %div, double* %norm, align 8, !dbg !528
  store i32 0, i32* %i, align 4, !dbg !529
  br label %for.cond157, !dbg !529

for.cond157:                                      ; preds = %for.inc165, %for.end155
  %108 = load i32* %i, align 4, !dbg !529
  %109 = load i32* %filt_sz, align 4, !dbg !529
  %cmp158 = icmp slt i32 %108, %109, !dbg !529
  br i1 %cmp158, label %for.body160, label %for.end167, !dbg !529

for.body160:                                      ; preds = %for.cond157
  %110 = load double* %norm, align 8, !dbg !531
  %111 = load i32* %i, align 4, !dbg !531
  %112 = load float** %result.addr, align 4, !dbg !531
  %arrayidx161 = getelementptr inbounds float* %112, i32 %111, !dbg !531
  %113 = load float* %arrayidx161, !dbg !531
  %conv162 = fpext float %113 to double, !dbg !531
  %div163 = fdiv double %conv162, %110, !dbg !531
  %conv164 = fptrunc double %div163 to float, !dbg !531
  store float %conv164, float* %arrayidx161, !dbg !531
  br label %for.inc165, !dbg !531

for.inc165:                                       ; preds = %for.body160
  %114 = load i32* %i, align 4, !dbg !532
  %inc166 = add nsw i32 %114, 1, !dbg !532
  store i32 %inc166, i32* %i, align 4, !dbg !532
  br label %for.cond157, !dbg !532

for.end167:                                       ; preds = %for.cond157
  %115 = load i32* %retval, !dbg !533
  ret i32 %115, !dbg !533
}

declare arm_aapcscc double @sqrt(double) nounwind

define arm_aapcscc i32 @predict(float* %filt, i32 %x_dim, i32 %y_dim, i32 %x_pos, i32 %y_pos, float* %result, i32 %f_or_e) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %filt.addr = alloca float*, align 4
  %x_dim.addr = alloca i32, align 4
  %y_dim.addr = alloca i32, align 4
  %x_pos.addr = alloca i32, align 4
  %y_pos.addr = alloca i32, align 4
  %result.addr = alloca float*, align 4
  %f_or_e.addr = alloca i32, align 4
  %y_filt = alloca i32, align 4
  %x_filt = alloca i32, align 4
  %y_res = alloca i32, align 4
  %x_res = alloca i32, align 4
  %taps_used = alloca float, align 4
  %fraction = alloca float, align 4
  %filt_sz = alloca i32, align 4
  %x_start = alloca i32, align 4
  %y_start = alloca i32, align 4
  %i = alloca i32, align 4
  store float* %filt, float** %filt.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %filt.addr}, metadata !534), !dbg !535
  store i32 %x_dim, i32* %x_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_dim.addr}, metadata !536), !dbg !537
  store i32 %y_dim, i32* %y_dim.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_dim.addr}, metadata !538), !dbg !539
  store i32 %x_pos, i32* %x_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %x_pos.addr}, metadata !540), !dbg !541
  store i32 %y_pos, i32* %y_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %y_pos.addr}, metadata !542), !dbg !543
  store float* %result, float** %result.addr, align 4
  call void @llvm.dbg.declare(metadata !{float** %result.addr}, metadata !544), !dbg !545
  store i32 %f_or_e, i32* %f_or_e.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %f_or_e.addr}, metadata !546), !dbg !547
  call void @llvm.dbg.declare(metadata !{i32* %y_filt}, metadata !548), !dbg !550
  call void @llvm.dbg.declare(metadata !{i32* %x_filt}, metadata !551), !dbg !552
  call void @llvm.dbg.declare(metadata !{i32* %y_res}, metadata !553), !dbg !554
  call void @llvm.dbg.declare(metadata !{i32* %x_res}, metadata !555), !dbg !556
  call void @llvm.dbg.declare(metadata !{float* %taps_used}, metadata !557), !dbg !558
  store float 0.000000e+00, float* %taps_used, align 4, !dbg !559
  call void @llvm.dbg.declare(metadata !{float* %fraction}, metadata !560), !dbg !561
  call void @llvm.dbg.declare(metadata !{i32* %filt_sz}, metadata !562), !dbg !563
  %0 = load i32* %x_dim.addr, align 4, !dbg !564
  %1 = load i32* %y_dim.addr, align 4, !dbg !564
  %mul = mul nsw i32 %0, %1, !dbg !564
  store i32 %mul, i32* %filt_sz, align 4, !dbg !564
  call void @llvm.dbg.declare(metadata !{i32* %x_start}, metadata !565), !dbg !566
  %2 = load i32* %x_pos.addr, align 4, !dbg !567
  %cmp = icmp sgt i32 %2, 0, !dbg !567
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !567

cond.true:                                        ; preds = %entry
  %3 = load i32* %x_pos.addr, align 4, !dbg !567
  %sub = sub nsw i32 %3, 1, !dbg !567
  br label %cond.end4, !dbg !567

cond.false:                                       ; preds = %entry
  %4 = load i32* %x_pos.addr, align 4, !dbg !567
  %cmp1 = icmp slt i32 %4, 0, !dbg !567
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !567

cond.true2:                                       ; preds = %cond.false
  %5 = load i32* %x_pos.addr, align 4, !dbg !567
  %add = add nsw i32 %5, 1, !dbg !567
  br label %cond.end, !dbg !567

cond.false3:                                      ; preds = %cond.false
  br label %cond.end, !dbg !567

cond.end:                                         ; preds = %cond.false3, %cond.true2
  %cond = phi i32 [ %add, %cond.true2 ], [ 0, %cond.false3 ], !dbg !567
  br label %cond.end4, !dbg !567

cond.end4:                                        ; preds = %cond.end, %cond.true
  %cond5 = phi i32 [ %sub, %cond.true ], [ %cond, %cond.end ], !dbg !567
  store i32 %cond5, i32* %x_start, align 4, !dbg !567
  call void @llvm.dbg.declare(metadata !{i32* %y_start}, metadata !568), !dbg !569
  %6 = load i32* %x_dim.addr, align 4, !dbg !570
  %7 = load i32* %y_pos.addr, align 4, !dbg !570
  %cmp6 = icmp sgt i32 %7, 0, !dbg !570
  br i1 %cmp6, label %cond.true7, label %cond.false9, !dbg !570

cond.true7:                                       ; preds = %cond.end4
  %8 = load i32* %y_pos.addr, align 4, !dbg !570
  %sub8 = sub nsw i32 %8, 1, !dbg !570
  br label %cond.end16, !dbg !570

cond.false9:                                      ; preds = %cond.end4
  %9 = load i32* %y_pos.addr, align 4, !dbg !570
  %cmp10 = icmp slt i32 %9, 0, !dbg !570
  br i1 %cmp10, label %cond.true11, label %cond.false13, !dbg !570

cond.true11:                                      ; preds = %cond.false9
  %10 = load i32* %y_pos.addr, align 4, !dbg !570
  %add12 = add nsw i32 %10, 1, !dbg !570
  br label %cond.end14, !dbg !570

cond.false13:                                     ; preds = %cond.false9
  br label %cond.end14, !dbg !570

cond.end14:                                       ; preds = %cond.false13, %cond.true11
  %cond15 = phi i32 [ %add12, %cond.true11 ], [ 0, %cond.false13 ], !dbg !570
  br label %cond.end16, !dbg !570

cond.end16:                                       ; preds = %cond.end14, %cond.true7
  %cond17 = phi i32 [ %sub8, %cond.true7 ], [ %cond15, %cond.end14 ], !dbg !570
  %mul18 = mul nsw i32 %6, %cond17, !dbg !570
  store i32 %mul18, i32* %y_start, align 4, !dbg !570
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !571), !dbg !572
  store i32 0, i32* %i, align 4, !dbg !573
  br label %for.cond, !dbg !573

for.cond:                                         ; preds = %for.inc, %cond.end16
  %11 = load i32* %i, align 4, !dbg !573
  %12 = load i32* %filt_sz, align 4, !dbg !573
  %cmp19 = icmp slt i32 %11, %12, !dbg !573
  br i1 %cmp19, label %for.body, label %for.end, !dbg !573

for.body:                                         ; preds = %for.cond
  %13 = load i32* %i, align 4, !dbg !575
  %14 = load float** %result.addr, align 4, !dbg !575
  %arrayidx = getelementptr inbounds float* %14, i32 %13, !dbg !575
  store float 0.000000e+00, float* %arrayidx, !dbg !575
  br label %for.inc, !dbg !575

for.inc:                                          ; preds = %for.body
  %15 = load i32* %i, align 4, !dbg !576
  %inc = add nsw i32 %15, 1, !dbg !576
  store i32 %inc, i32* %i, align 4, !dbg !576
  br label %for.cond, !dbg !576

for.end:                                          ; preds = %for.cond
  store i32 0, i32* %y_filt, align 4, !dbg !577
  %16 = load i32* %y_start, align 4, !dbg !577
  store i32 %16, i32* %y_res, align 4, !dbg !577
  br label %for.cond20, !dbg !577

for.cond20:                                       ; preds = %for.inc43, %for.end
  %17 = load i32* %y_filt, align 4, !dbg !577
  %18 = load i32* %filt_sz, align 4, !dbg !577
  %cmp21 = icmp slt i32 %17, %18, !dbg !577
  br i1 %cmp21, label %for.body22, label %for.end46, !dbg !577

for.body22:                                       ; preds = %for.cond20
  %19 = load i32* %y_res, align 4, !dbg !579
  %cmp23 = icmp sge i32 %19, 0, !dbg !579
  br i1 %cmp23, label %land.lhs.true, label %if.end42, !dbg !579

land.lhs.true:                                    ; preds = %for.body22
  %20 = load i32* %y_res, align 4, !dbg !579
  %21 = load i32* %filt_sz, align 4, !dbg !579
  %cmp24 = icmp slt i32 %20, %21, !dbg !579
  br i1 %cmp24, label %if.then, label %if.end42, !dbg !579

if.then:                                          ; preds = %land.lhs.true
  %22 = load i32* %y_filt, align 4, !dbg !580
  store i32 %22, i32* %x_filt, align 4, !dbg !580
  %23 = load i32* %x_start, align 4, !dbg !580
  store i32 %23, i32* %x_res, align 4, !dbg !580
  br label %for.cond25, !dbg !580

for.cond25:                                       ; preds = %for.inc38, %if.then
  %24 = load i32* %x_filt, align 4, !dbg !580
  %25 = load i32* %y_filt, align 4, !dbg !580
  %26 = load i32* %x_dim.addr, align 4, !dbg !580
  %add26 = add nsw i32 %25, %26, !dbg !580
  %cmp27 = icmp slt i32 %24, %add26, !dbg !580
  br i1 %cmp27, label %for.body28, label %for.end41, !dbg !580

for.body28:                                       ; preds = %for.cond25
  %27 = load i32* %x_res, align 4, !dbg !582
  %cmp29 = icmp sge i32 %27, 0, !dbg !582
  br i1 %cmp29, label %land.lhs.true30, label %if.end, !dbg !582

land.lhs.true30:                                  ; preds = %for.body28
  %28 = load i32* %x_res, align 4, !dbg !582
  %29 = load i32* %x_dim.addr, align 4, !dbg !582
  %cmp31 = icmp slt i32 %28, %29, !dbg !582
  br i1 %cmp31, label %if.then32, label %if.end, !dbg !582

if.then32:                                        ; preds = %land.lhs.true30
  %30 = load i32* %x_filt, align 4, !dbg !583
  %31 = load float** %filt.addr, align 4, !dbg !583
  %arrayidx33 = getelementptr inbounds float* %31, i32 %30, !dbg !583
  %32 = load float* %arrayidx33, !dbg !583
  %33 = load i32* %y_res, align 4, !dbg !583
  %34 = load i32* %x_res, align 4, !dbg !583
  %add34 = add nsw i32 %33, %34, !dbg !583
  %35 = load float** %result.addr, align 4, !dbg !583
  %arrayidx35 = getelementptr inbounds float* %35, i32 %add34, !dbg !583
  store float %32, float* %arrayidx35, !dbg !583
  %36 = load i32* %x_filt, align 4, !dbg !585
  %37 = load float** %filt.addr, align 4, !dbg !585
  %arrayidx36 = getelementptr inbounds float* %37, i32 %36, !dbg !585
  %38 = load float* %arrayidx36, !dbg !585
  %39 = load float* %taps_used, align 4, !dbg !585
  %add37 = fadd float %39, %38, !dbg !585
  store float %add37, float* %taps_used, align 4, !dbg !585
  br label %if.end, !dbg !586

if.end:                                           ; preds = %if.then32, %land.lhs.true30, %for.body28
  br label %for.inc38, !dbg !586

for.inc38:                                        ; preds = %if.end
  %40 = load i32* %x_filt, align 4, !dbg !587
  %inc39 = add nsw i32 %40, 1, !dbg !587
  store i32 %inc39, i32* %x_filt, align 4, !dbg !587
  %41 = load i32* %x_res, align 4, !dbg !587
  %inc40 = add nsw i32 %41, 1, !dbg !587
  store i32 %inc40, i32* %x_res, align 4, !dbg !587
  br label %for.cond25, !dbg !587

for.end41:                                        ; preds = %for.cond25
  br label %if.end42, !dbg !588

if.end42:                                         ; preds = %for.end41, %land.lhs.true, %for.body22
  br label %for.inc43, !dbg !588

for.inc43:                                        ; preds = %if.end42
  %42 = load i32* %x_dim.addr, align 4, !dbg !589
  %43 = load i32* %y_filt, align 4, !dbg !589
  %add44 = add nsw i32 %43, %42, !dbg !589
  store i32 %add44, i32* %y_filt, align 4, !dbg !589
  %44 = load i32* %x_dim.addr, align 4, !dbg !589
  %45 = load i32* %y_res, align 4, !dbg !589
  %add45 = add nsw i32 %45, %44, !dbg !589
  store i32 %add45, i32* %y_res, align 4, !dbg !589
  br label %for.cond20, !dbg !589

for.end46:                                        ; preds = %for.cond20
  %46 = load i32* %f_or_e.addr, align 4, !dbg !590
  %cmp47 = icmp eq i32 %46, 0, !dbg !590
  br i1 %cmp47, label %if.then48, label %if.end57, !dbg !590

if.then48:                                        ; preds = %for.end46
  %47 = load float* %taps_used, align 4, !dbg !591
  %div = fdiv float 2.000000e+00, %47, !dbg !591
  store float %div, float* %fraction, align 4, !dbg !591
  store i32 0, i32* %i, align 4, !dbg !593
  br label %for.cond49, !dbg !593

for.cond49:                                       ; preds = %for.inc54, %if.then48
  %48 = load i32* %i, align 4, !dbg !593
  %49 = load i32* %filt_sz, align 4, !dbg !593
  %cmp50 = icmp slt i32 %48, %49, !dbg !593
  br i1 %cmp50, label %for.body51, label %for.end56, !dbg !593

for.body51:                                       ; preds = %for.cond49
  %50 = load float* %fraction, align 4, !dbg !595
  %51 = load i32* %i, align 4, !dbg !595
  %52 = load float** %result.addr, align 4, !dbg !595
  %arrayidx52 = getelementptr inbounds float* %52, i32 %51, !dbg !595
  %53 = load float* %arrayidx52, !dbg !595
  %mul53 = fmul float %53, %50, !dbg !595
  store float %mul53, float* %arrayidx52, !dbg !595
  br label %for.inc54, !dbg !595

for.inc54:                                        ; preds = %for.body51
  %54 = load i32* %i, align 4, !dbg !596
  %inc55 = add nsw i32 %54, 1, !dbg !596
  store i32 %inc55, i32* %i, align 4, !dbg !596
  br label %for.cond49, !dbg !596

for.end56:                                        ; preds = %for.cond49
  br label %if.end57, !dbg !597

if.end57:                                         ; preds = %for.end56, %for.end46
  %55 = load i32* %retval, !dbg !598
  ret i32 %55, !dbg !598
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !43} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !17, metadata !22, metadata !25, metadata !28, metadata !31, metadata !34, metadata !37, metadata !40}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"edge_function", metadata !"edge_function", metadata !"", metadata !6, i32 60, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (...)* (i8*)* @edge_function, null, null, metadata !15} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"edges.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720918, null, metadata !"fptr", metadata !6, i32 23, i64 0, i64 0, i64 0, i32 0, metadata !10} ; [ DW_TAG_typedef ]
!10 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !11} ; [ DW_TAG_pointer_type ]
!11 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !12, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!12 = metadata !{metadata !13, metadata !14}
!13 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!14 = metadata !{i32 720920}                      ; [ DW_TAG_unspecified_parameters ]
!15 = metadata !{metadata !16}
!16 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!17 = metadata !{i32 720942, i32 0, metadata !6, metadata !"zero", metadata !"zero", metadata !"", metadata !6, i32 93, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, i32, float*, i32)* @zero, null, null, metadata !20} ; [ DW_TAG_subprogram ]
!18 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !19, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!19 = metadata !{metadata !13}
!20 = metadata !{metadata !21}
!21 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!22 = metadata !{i32 720942, i32 0, metadata !6, metadata !"repeat", metadata !"repeat", metadata !"", metadata !6, i32 122, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, i32, float*, i32)* @repeat, null, null, metadata !23} ; [ DW_TAG_subprogram ]
!23 = metadata !{metadata !24}
!24 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!25 = metadata !{i32 720942, i32 0, metadata !6, metadata !"reflect2", metadata !"reflect2", metadata !"", metadata !6, i32 152, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, i32, float*, i32)* @reflect2, null, null, metadata !26} ; [ DW_TAG_subprogram ]
!26 = metadata !{metadata !27}
!27 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!28 = metadata !{i32 720942, i32 0, metadata !6, metadata !"reflect1", metadata !"reflect1", metadata !"", metadata !6, i32 190, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, i32, float*, i32)* @reflect1, null, null, metadata !29} ; [ DW_TAG_subprogram ]
!29 = metadata !{metadata !30}
!30 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!31 = metadata !{i32 720942, i32 0, metadata !6, metadata !"extend", metadata !"extend", metadata !"", metadata !6, i32 246, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, i32, float*, i32)* @extend, null, null, metadata !32} ; [ DW_TAG_subprogram ]
!32 = metadata !{metadata !33}
!33 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!34 = metadata !{i32 720942, i32 0, metadata !6, metadata !"nocompute", metadata !"nocompute", metadata !"", metadata !6, i32 303, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, i32, float*, i32)* @nocompute, null, null, metadata !35} ; [ DW_TAG_subprogram ]
!35 = metadata !{metadata !36}
!36 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!37 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ereflect", metadata !"ereflect", metadata !"", metadata !6, i32 323, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, i32, float*, i32)* @ereflect, null, null, metadata !38} ; [ DW_TAG_subprogram ]
!38 = metadata !{metadata !39}
!39 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!40 = metadata !{i32 720942, i32 0, metadata !6, metadata !"predict", metadata !"predict", metadata !"", metadata !6, i32 379, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (float*, i32, i32, i32, i32, float*, i32)* @predict, null, null, metadata !41} ; [ DW_TAG_subprogram ]
!41 = metadata !{metadata !42}
!42 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!43 = metadata !{metadata !44}
!44 = metadata !{metadata !45}
!45 = metadata !{i32 720948, i32 0, null, metadata !"edge_foos", metadata !"edge_foos", metadata !"", metadata !6, i32 40, metadata !46, i32 1, i32 1, [9 x %struct.EDGE_HANDLER]* @edge_foos} ; [ DW_TAG_variable ]
!46 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 576, i64 32, i32 0, i32 0, metadata !47, metadata !55, i32 0, i32 0} ; [ DW_TAG_array_type ]
!47 = metadata !{i32 720918, null, metadata !"EDGE_HANDLER", metadata !6, i32 29, i64 0, i64 0, i64 0, i32 0, metadata !48} ; [ DW_TAG_typedef ]
!48 = metadata !{i32 720915, null, metadata !"", metadata !49, i32 25, i64 64, i64 32, i32 0, i32 0, null, metadata !50, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!49 = metadata !{i32 720937, metadata !"convolve.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!50 = metadata !{metadata !51, metadata !54}
!51 = metadata !{i32 720909, metadata !48, metadata !"name", metadata !49, i32 27, i64 32, i64 32, i64 0, i32 0, metadata !52} ; [ DW_TAG_member ]
!52 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !53} ; [ DW_TAG_pointer_type ]
!53 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!54 = metadata !{i32 720909, metadata !48, metadata !"func", metadata !49, i32 28, i64 32, i64 32, i64 32, i32 0, metadata !9} ; [ DW_TAG_member ]
!55 = metadata !{metadata !56}
!56 = metadata !{i32 720929, i64 0, i64 8}        ; [ DW_TAG_subrange_type ]
!57 = metadata !{i32 721153, metadata !5, metadata !"edges", metadata !6, i32 16777275, metadata !52, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!58 = metadata !{i32 59, i32 9, metadata !5, null}
!59 = metadata !{i32 721152, metadata !60, metadata !"i", metadata !6, i32 61, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!60 = metadata !{i32 720907, metadata !5, i32 60, i32 3, metadata !6, i32 328} ; [ DW_TAG_lexical_block ]
!61 = metadata !{i32 61, i32 7, metadata !60, null}
!62 = metadata !{i32 62, i32 8, metadata !63, null}
!63 = metadata !{i32 720907, metadata !60, i32 62, i32 3, metadata !6, i32 329} ; [ DW_TAG_lexical_block ]
!64 = metadata !{i32 63, i32 10, metadata !63, null}
!65 = metadata !{i32 64, i32 7, metadata !63, null}
!66 = metadata !{i32 62, i32 57, metadata !63, null}
!67 = metadata !{i32 65, i32 3, metadata !60, null}
!68 = metadata !{i32 66, i32 3, metadata !60, null}
!69 = metadata !{i32 67, i32 3, metadata !60, null}
!70 = metadata !{i32 721153, metadata !17, metadata !"filt", metadata !6, i32 16777306, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!71 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !72} ; [ DW_TAG_pointer_type ]
!72 = metadata !{i32 720932, null, metadata !"float", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!73 = metadata !{i32 90, i32 19, metadata !17, null}
!74 = metadata !{i32 721153, metadata !17, metadata !"x_dim", metadata !6, i32 33554523, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!75 = metadata !{i32 91, i32 16, metadata !17, null}
!76 = metadata !{i32 721153, metadata !17, metadata !"y_dim", metadata !6, i32 50331740, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!77 = metadata !{i32 92, i32 7, metadata !17, null}
!78 = metadata !{i32 721153, metadata !17, metadata !"x_pos", metadata !6, i32 67108956, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!79 = metadata !{i32 92, i32 14, metadata !17, null}
!80 = metadata !{i32 721153, metadata !17, metadata !"y_pos", metadata !6, i32 83886172, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!81 = metadata !{i32 92, i32 21, metadata !17, null}
!82 = metadata !{i32 721153, metadata !17, metadata !"result", metadata !6, i32 100663386, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!83 = metadata !{i32 90, i32 26, metadata !17, null}
!84 = metadata !{i32 721153, metadata !17, metadata !"f_or_e", metadata !6, i32 117440604, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!85 = metadata !{i32 92, i32 28, metadata !17, null}
!86 = metadata !{i32 721152, metadata !87, metadata !"y_filt", metadata !6, i32 94, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!87 = metadata !{i32 720907, metadata !17, i32 93, i32 3, metadata !6, i32 330} ; [ DW_TAG_lexical_block ]
!88 = metadata !{i32 94, i32 16, metadata !87, null}
!89 = metadata !{i32 721152, metadata !87, metadata !"x_filt", metadata !6, i32 94, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!90 = metadata !{i32 94, i32 23, metadata !87, null}
!91 = metadata !{i32 721152, metadata !87, metadata !"y_res", metadata !6, i32 94, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!92 = metadata !{i32 94, i32 31, metadata !87, null}
!93 = metadata !{i32 721152, metadata !87, metadata !"x_res", metadata !6, i32 94, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!94 = metadata !{i32 94, i32 37, metadata !87, null}
!95 = metadata !{i32 721152, metadata !87, metadata !"filt_sz", metadata !6, i32 95, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!96 = metadata !{i32 95, i32 7, metadata !87, null}
!97 = metadata !{i32 95, i32 28, metadata !87, null}
!98 = metadata !{i32 721152, metadata !87, metadata !"x_start", metadata !6, i32 96, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!99 = metadata !{i32 96, i32 7, metadata !87, null}
!100 = metadata !{i32 96, i32 62, metadata !87, null}
!101 = metadata !{i32 721152, metadata !87, metadata !"y_start", metadata !6, i32 97, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!102 = metadata !{i32 97, i32 7, metadata !87, null}
!103 = metadata !{i32 97, i32 70, metadata !87, null}
!104 = metadata !{i32 721152, metadata !87, metadata !"i", metadata !6, i32 98, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!105 = metadata !{i32 98, i32 7, metadata !87, null}
!106 = metadata !{i32 100, i32 8, metadata !107, null}
!107 = metadata !{i32 720907, metadata !87, i32 100, i32 3, metadata !6, i32 331} ; [ DW_TAG_lexical_block ]
!108 = metadata !{i32 100, i32 29, metadata !107, null}
!109 = metadata !{i32 100, i32 24, metadata !107, null}
!110 = metadata !{i32 102, i32 8, metadata !111, null}
!111 = metadata !{i32 720907, metadata !87, i32 102, i32 3, metadata !6, i32 332} ; [ DW_TAG_lexical_block ]
!112 = metadata !{i32 105, i32 7, metadata !111, null}
!113 = metadata !{i32 106, i32 7, metadata !114, null}
!114 = metadata !{i32 720907, metadata !111, i32 106, i32 2, metadata !6, i32 333} ; [ DW_TAG_lexical_block ]
!115 = metadata !{i32 109, i32 4, metadata !114, null}
!116 = metadata !{i32 110, i32 6, metadata !114, null}
!117 = metadata !{i32 108, i32 7, metadata !114, null}
!118 = metadata !{i32 110, i32 39, metadata !114, null}
!119 = metadata !{i32 104, i32 8, metadata !111, null}
!120 = metadata !{i32 111, i32 3, metadata !87, null}
!121 = metadata !{i32 721153, metadata !22, metadata !"filt", metadata !6, i32 16777335, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!122 = metadata !{i32 119, i32 19, metadata !22, null}
!123 = metadata !{i32 721153, metadata !22, metadata !"x_dim", metadata !6, i32 33554552, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!124 = metadata !{i32 120, i32 16, metadata !22, null}
!125 = metadata !{i32 721153, metadata !22, metadata !"y_dim", metadata !6, i32 50331769, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!126 = metadata !{i32 121, i32 7, metadata !22, null}
!127 = metadata !{i32 721153, metadata !22, metadata !"x_pos", metadata !6, i32 67108985, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!128 = metadata !{i32 121, i32 14, metadata !22, null}
!129 = metadata !{i32 721153, metadata !22, metadata !"y_pos", metadata !6, i32 83886201, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!130 = metadata !{i32 121, i32 21, metadata !22, null}
!131 = metadata !{i32 721153, metadata !22, metadata !"result", metadata !6, i32 100663415, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!132 = metadata !{i32 119, i32 26, metadata !22, null}
!133 = metadata !{i32 721153, metadata !22, metadata !"f_or_e", metadata !6, i32 117440633, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!134 = metadata !{i32 121, i32 28, metadata !22, null}
!135 = metadata !{i32 721152, metadata !136, metadata !"y_filt", metadata !6, i32 123, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!136 = metadata !{i32 720907, metadata !22, i32 122, i32 3, metadata !6, i32 334} ; [ DW_TAG_lexical_block ]
!137 = metadata !{i32 123, i32 16, metadata !136, null}
!138 = metadata !{i32 721152, metadata !136, metadata !"x_filt", metadata !6, i32 123, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!139 = metadata !{i32 123, i32 23, metadata !136, null}
!140 = metadata !{i32 721152, metadata !136, metadata !"y_res", metadata !6, i32 123, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!141 = metadata !{i32 123, i32 31, metadata !136, null}
!142 = metadata !{i32 721152, metadata !136, metadata !"x_res", metadata !6, i32 123, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!143 = metadata !{i32 123, i32 37, metadata !136, null}
!144 = metadata !{i32 721152, metadata !136, metadata !"filt_sz", metadata !6, i32 124, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!145 = metadata !{i32 124, i32 7, metadata !136, null}
!146 = metadata !{i32 124, i32 28, metadata !136, null}
!147 = metadata !{i32 721152, metadata !136, metadata !"x_start", metadata !6, i32 125, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!148 = metadata !{i32 125, i32 7, metadata !136, null}
!149 = metadata !{i32 125, i32 62, metadata !136, null}
!150 = metadata !{i32 721152, metadata !136, metadata !"y_start", metadata !6, i32 126, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!151 = metadata !{i32 126, i32 7, metadata !136, null}
!152 = metadata !{i32 126, i32 70, metadata !136, null}
!153 = metadata !{i32 721152, metadata !136, metadata !"i", metadata !6, i32 127, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!154 = metadata !{i32 127, i32 7, metadata !136, null}
!155 = metadata !{i32 129, i32 8, metadata !156, null}
!156 = metadata !{i32 720907, metadata !136, i32 129, i32 3, metadata !6, i32 335} ; [ DW_TAG_lexical_block ]
!157 = metadata !{i32 129, i32 29, metadata !156, null}
!158 = metadata !{i32 129, i32 24, metadata !156, null}
!159 = metadata !{i32 131, i32 8, metadata !160, null}
!160 = metadata !{i32 720907, metadata !136, i32 131, i32 3, metadata !6, i32 336} ; [ DW_TAG_lexical_block ]
!161 = metadata !{i32 134, i32 10, metadata !162, null}
!162 = metadata !{i32 720907, metadata !160, i32 134, i32 5, metadata !6, i32 337} ; [ DW_TAG_lexical_block ]
!163 = metadata !{i32 137, i32 7, metadata !162, null}
!164 = metadata !{i32 136, i32 3, metadata !162, null}
!165 = metadata !{i32 139, i32 20, metadata !162, null}
!166 = metadata !{i32 133, i32 8, metadata !160, null}
!167 = metadata !{i32 140, i32 3, metadata !136, null}
!168 = metadata !{i32 721153, metadata !25, metadata !"filt", metadata !6, i32 16777365, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!169 = metadata !{i32 149, i32 19, metadata !25, null}
!170 = metadata !{i32 721153, metadata !25, metadata !"x_dim", metadata !6, i32 33554582, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!171 = metadata !{i32 150, i32 16, metadata !25, null}
!172 = metadata !{i32 721153, metadata !25, metadata !"y_dim", metadata !6, i32 50331799, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!173 = metadata !{i32 151, i32 7, metadata !25, null}
!174 = metadata !{i32 721153, metadata !25, metadata !"x_pos", metadata !6, i32 67109015, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!175 = metadata !{i32 151, i32 14, metadata !25, null}
!176 = metadata !{i32 721153, metadata !25, metadata !"y_pos", metadata !6, i32 83886231, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!177 = metadata !{i32 151, i32 21, metadata !25, null}
!178 = metadata !{i32 721153, metadata !25, metadata !"result", metadata !6, i32 100663445, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!179 = metadata !{i32 149, i32 26, metadata !25, null}
!180 = metadata !{i32 721153, metadata !25, metadata !"f_or_e", metadata !6, i32 117440663, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!181 = metadata !{i32 151, i32 28, metadata !25, null}
!182 = metadata !{i32 721152, metadata !183, metadata !"y_filt", metadata !6, i32 153, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!183 = metadata !{i32 720907, metadata !25, i32 152, i32 3, metadata !6, i32 338} ; [ DW_TAG_lexical_block ]
!184 = metadata !{i32 153, i32 16, metadata !183, null}
!185 = metadata !{i32 721152, metadata !183, metadata !"x_filt", metadata !6, i32 153, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!186 = metadata !{i32 153, i32 23, metadata !183, null}
!187 = metadata !{i32 721152, metadata !183, metadata !"y_edge", metadata !6, i32 153, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!188 = metadata !{i32 153, i32 31, metadata !183, null}
!189 = metadata !{i32 721152, metadata !183, metadata !"x_edge", metadata !6, i32 153, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!190 = metadata !{i32 153, i32 38, metadata !183, null}
!191 = metadata !{i32 721152, metadata !183, metadata !"x_base", metadata !6, i32 154, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!192 = metadata !{i32 154, i32 16, metadata !183, null}
!193 = metadata !{i32 154, i32 46, metadata !183, null}
!194 = metadata !{i32 721152, metadata !183, metadata !"y_base", metadata !6, i32 155, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!195 = metadata !{i32 155, i32 16, metadata !183, null}
!196 = metadata !{i32 155, i32 54, metadata !183, null}
!197 = metadata !{i32 721152, metadata !183, metadata !"filt_sz", metadata !6, i32 156, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!198 = metadata !{i32 156, i32 7, metadata !183, null}
!199 = metadata !{i32 156, i32 28, metadata !183, null}
!200 = metadata !{i32 721152, metadata !183, metadata !"x_edge_dist", metadata !6, i32 157, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!201 = metadata !{i32 157, i32 7, metadata !183, null}
!202 = metadata !{i32 157, i32 56, metadata !183, null}
!203 = metadata !{i32 721152, metadata !183, metadata !"y_edge_dist", metadata !6, i32 158, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!204 = metadata !{i32 158, i32 7, metadata !183, null}
!205 = metadata !{i32 158, i32 66, metadata !183, null}
!206 = metadata !{i32 721152, metadata !183, metadata !"i", metadata !6, i32 159, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!207 = metadata !{i32 159, i32 7, metadata !183, null}
!208 = metadata !{i32 161, i32 8, metadata !209, null}
!209 = metadata !{i32 720907, metadata !183, i32 161, i32 3, metadata !6, i32 339} ; [ DW_TAG_lexical_block ]
!210 = metadata !{i32 161, i32 29, metadata !209, null}
!211 = metadata !{i32 161, i32 24, metadata !209, null}
!212 = metadata !{i32 164, i32 8, metadata !213, null}
!213 = metadata !{i32 720907, metadata !183, i32 164, i32 3, metadata !6, i32 340} ; [ DW_TAG_lexical_block ]
!214 = metadata !{i32 168, i32 7, metadata !215, null}
!215 = metadata !{i32 720907, metadata !213, i32 167, i32 7, metadata !6, i32 341} ; [ DW_TAG_lexical_block ]
!216 = metadata !{i32 168, i32 24, metadata !215, null}
!217 = metadata !{i32 169, i32 12, metadata !218, null}
!218 = metadata !{i32 720907, metadata !215, i32 169, i32 7, metadata !6, i32 342} ; [ DW_TAG_lexical_block ]
!219 = metadata !{i32 173, i32 4, metadata !220, null}
!220 = metadata !{i32 720907, metadata !218, i32 172, i32 4, metadata !6, i32 343} ; [ DW_TAG_lexical_block ]
!221 = metadata !{i32 173, i32 21, metadata !220, null}
!222 = metadata !{i32 174, i32 4, metadata !220, null}
!223 = metadata !{i32 176, i32 4, metadata !220, null}
!224 = metadata !{i32 171, i32 5, metadata !218, null}
!225 = metadata !{i32 177, i32 7, metadata !215, null}
!226 = metadata !{i32 166, i32 8, metadata !213, null}
!227 = metadata !{i32 178, i32 3, metadata !183, null}
!228 = metadata !{i32 721153, metadata !28, metadata !"filt", metadata !6, i32 16777403, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!229 = metadata !{i32 187, i32 19, metadata !28, null}
!230 = metadata !{i32 721153, metadata !28, metadata !"x_dim", metadata !6, i32 33554620, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!231 = metadata !{i32 188, i32 16, metadata !28, null}
!232 = metadata !{i32 721153, metadata !28, metadata !"y_dim", metadata !6, i32 50331837, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!233 = metadata !{i32 189, i32 7, metadata !28, null}
!234 = metadata !{i32 721153, metadata !28, metadata !"x_pos", metadata !6, i32 67109053, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!235 = metadata !{i32 189, i32 14, metadata !28, null}
!236 = metadata !{i32 721153, metadata !28, metadata !"y_pos", metadata !6, i32 83886269, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!237 = metadata !{i32 189, i32 21, metadata !28, null}
!238 = metadata !{i32 721153, metadata !28, metadata !"result", metadata !6, i32 100663483, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!239 = metadata !{i32 187, i32 26, metadata !28, null}
!240 = metadata !{i32 721153, metadata !28, metadata !"f_or_e", metadata !6, i32 117440701, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!241 = metadata !{i32 189, i32 28, metadata !28, null}
!242 = metadata !{i32 721152, metadata !243, metadata !"filt_sz", metadata !6, i32 191, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!243 = metadata !{i32 720907, metadata !28, i32 190, i32 3, metadata !6, i32 344} ; [ DW_TAG_lexical_block ]
!244 = metadata !{i32 191, i32 7, metadata !243, null}
!245 = metadata !{i32 191, i32 28, metadata !243, null}
!246 = metadata !{i32 721152, metadata !243, metadata !"x_start", metadata !6, i32 192, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!247 = metadata !{i32 192, i32 16, metadata !243, null}
!248 = metadata !{i32 192, i32 74, metadata !243, null}
!249 = metadata !{i32 721152, metadata !243, metadata !"y_start", metadata !6, i32 192, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!250 = metadata !{i32 192, i32 29, metadata !243, null}
!251 = metadata !{i32 721152, metadata !243, metadata !"x_stop", metadata !6, i32 192, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!252 = metadata !{i32 192, i32 42, metadata !243, null}
!253 = metadata !{i32 721152, metadata !243, metadata !"y_stop", metadata !6, i32 192, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!254 = metadata !{i32 192, i32 58, metadata !243, null}
!255 = metadata !{i32 721152, metadata !243, metadata !"y_filt", metadata !6, i32 193, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!256 = metadata !{i32 193, i32 16, metadata !243, null}
!257 = metadata !{i32 721152, metadata !243, metadata !"x_filt", metadata !6, i32 193, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!258 = metadata !{i32 193, i32 23, metadata !243, null}
!259 = metadata !{i32 721152, metadata !243, metadata !"y_edge", metadata !6, i32 193, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!260 = metadata !{i32 193, i32 31, metadata !243, null}
!261 = metadata !{i32 721152, metadata !243, metadata !"x_edge", metadata !6, i32 193, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!262 = metadata !{i32 193, i32 38, metadata !243, null}
!263 = metadata !{i32 721152, metadata !243, metadata !"x_base", metadata !6, i32 194, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!264 = metadata !{i32 194, i32 16, metadata !243, null}
!265 = metadata !{i32 194, i32 46, metadata !243, null}
!266 = metadata !{i32 721152, metadata !243, metadata !"y_base", metadata !6, i32 195, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!267 = metadata !{i32 195, i32 16, metadata !243, null}
!268 = metadata !{i32 195, i32 54, metadata !243, null}
!269 = metadata !{i32 721152, metadata !243, metadata !"x_edge_dist", metadata !6, i32 196, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!270 = metadata !{i32 196, i32 7, metadata !243, null}
!271 = metadata !{i32 196, i32 69, metadata !243, null}
!272 = metadata !{i32 721152, metadata !243, metadata !"y_edge_dist", metadata !6, i32 197, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!273 = metadata !{i32 197, i32 7, metadata !243, null}
!274 = metadata !{i32 197, i32 79, metadata !243, null}
!275 = metadata !{i32 721152, metadata !243, metadata !"i", metadata !6, i32 198, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!276 = metadata !{i32 198, i32 7, metadata !243, null}
!277 = metadata !{i32 721152, metadata !243, metadata !"mx_pos", metadata !6, i32 199, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!278 = metadata !{i32 199, i32 7, metadata !243, null}
!279 = metadata !{i32 199, i32 27, metadata !243, null}
!280 = metadata !{i32 721152, metadata !243, metadata !"my_pos", metadata !6, i32 200, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!281 = metadata !{i32 200, i32 7, metadata !243, null}
!282 = metadata !{i32 200, i32 27, metadata !243, null}
!283 = metadata !{i32 202, i32 8, metadata !284, null}
!284 = metadata !{i32 720907, metadata !243, i32 202, i32 3, metadata !6, i32 345} ; [ DW_TAG_lexical_block ]
!285 = metadata !{i32 202, i32 29, metadata !284, null}
!286 = metadata !{i32 202, i32 24, metadata !284, null}
!287 = metadata !{i32 205, i32 3, metadata !243, null}
!288 = metadata !{i32 207, i32 7, metadata !289, null}
!289 = metadata !{i32 720907, metadata !243, i32 206, i32 7, metadata !6, i32 346} ; [ DW_TAG_lexical_block ]
!290 = metadata !{i32 207, i32 28, metadata !289, null}
!291 = metadata !{i32 208, i32 12, metadata !289, null}
!292 = metadata !{i32 208, i32 36, metadata !293, null}
!293 = metadata !{i32 720907, metadata !289, i32 208, i32 34, metadata !6, i32 347} ; [ DW_TAG_lexical_block ]
!294 = metadata !{i32 208, i32 55, metadata !293, null}
!295 = metadata !{i32 208, i32 72, metadata !293, null}
!296 = metadata !{i32 210, i32 7, metadata !289, null}
!297 = metadata !{i32 210, i32 28, metadata !289, null}
!298 = metadata !{i32 211, i32 12, metadata !289, null}
!299 = metadata !{i32 211, i32 36, metadata !300, null}
!300 = metadata !{i32 720907, metadata !289, i32 211, i32 34, metadata !6, i32 348} ; [ DW_TAG_lexical_block ]
!301 = metadata !{i32 211, i32 63, metadata !300, null}
!302 = metadata !{i32 211, i32 79, metadata !300, null}
!303 = metadata !{i32 212, i32 7, metadata !289, null}
!304 = metadata !{i32 215, i32 8, metadata !305, null}
!305 = metadata !{i32 720907, metadata !243, i32 215, i32 3, metadata !6, i32 349} ; [ DW_TAG_lexical_block ]
!306 = metadata !{i32 218, i32 10, metadata !307, null}
!307 = metadata !{i32 720907, metadata !305, i32 218, i32 5, metadata !6, i32 350} ; [ DW_TAG_lexical_block ]
!308 = metadata !{i32 221, i32 7, metadata !307, null}
!309 = metadata !{i32 220, i32 3, metadata !307, null}
!310 = metadata !{i32 222, i32 16, metadata !307, null}
!311 = metadata !{i32 217, i32 8, metadata !305, null}
!312 = metadata !{i32 225, i32 3, metadata !243, null}
!313 = metadata !{i32 227, i32 7, metadata !314, null}
!314 = metadata !{i32 720907, metadata !243, i32 226, i32 7, metadata !6, i32 351} ; [ DW_TAG_lexical_block ]
!315 = metadata !{i32 228, i32 7, metadata !316, null}
!316 = metadata !{i32 720907, metadata !314, i32 228, i32 2, metadata !6, i32 352} ; [ DW_TAG_lexical_block ]
!317 = metadata !{i32 229, i32 4, metadata !316, null}
!318 = metadata !{i32 228, i32 38, metadata !316, null}
!319 = metadata !{i32 229, i32 35, metadata !316, null}
!320 = metadata !{i32 230, i32 7, metadata !314, null}
!321 = metadata !{i32 231, i32 7, metadata !322, null}
!322 = metadata !{i32 720907, metadata !314, i32 231, i32 2, metadata !6, i32 353} ; [ DW_TAG_lexical_block ]
!323 = metadata !{i32 232, i32 4, metadata !322, null}
!324 = metadata !{i32 231, i32 43, metadata !322, null}
!325 = metadata !{i32 232, i32 35, metadata !322, null}
!326 = metadata !{i32 233, i32 7, metadata !314, null}
!327 = metadata !{i32 234, i32 3, metadata !243, null}
!328 = metadata !{i32 721153, metadata !31, metadata !"filt", metadata !6, i32 16777459, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!329 = metadata !{i32 243, i32 19, metadata !31, null}
!330 = metadata !{i32 721153, metadata !31, metadata !"x_dim", metadata !6, i32 33554676, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!331 = metadata !{i32 244, i32 16, metadata !31, null}
!332 = metadata !{i32 721153, metadata !31, metadata !"y_dim", metadata !6, i32 50331893, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!333 = metadata !{i32 245, i32 7, metadata !31, null}
!334 = metadata !{i32 721153, metadata !31, metadata !"x_pos", metadata !6, i32 67109109, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!335 = metadata !{i32 245, i32 14, metadata !31, null}
!336 = metadata !{i32 721153, metadata !31, metadata !"y_pos", metadata !6, i32 83886325, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!337 = metadata !{i32 245, i32 21, metadata !31, null}
!338 = metadata !{i32 721153, metadata !31, metadata !"result", metadata !6, i32 100663539, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!339 = metadata !{i32 243, i32 26, metadata !31, null}
!340 = metadata !{i32 721153, metadata !31, metadata !"f_or_e", metadata !6, i32 117440757, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!341 = metadata !{i32 245, i32 28, metadata !31, null}
!342 = metadata !{i32 721152, metadata !343, metadata !"filt_sz", metadata !6, i32 247, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!343 = metadata !{i32 720907, metadata !31, i32 246, i32 3, metadata !6, i32 354} ; [ DW_TAG_lexical_block ]
!344 = metadata !{i32 247, i32 7, metadata !343, null}
!345 = metadata !{i32 247, i32 28, metadata !343, null}
!346 = metadata !{i32 721152, metadata !343, metadata !"x_start", metadata !6, i32 248, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!347 = metadata !{i32 248, i32 16, metadata !343, null}
!348 = metadata !{i32 248, i32 74, metadata !343, null}
!349 = metadata !{i32 721152, metadata !343, metadata !"y_start", metadata !6, i32 248, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!350 = metadata !{i32 248, i32 29, metadata !343, null}
!351 = metadata !{i32 721152, metadata !343, metadata !"x_stop", metadata !6, i32 248, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!352 = metadata !{i32 248, i32 42, metadata !343, null}
!353 = metadata !{i32 721152, metadata !343, metadata !"y_stop", metadata !6, i32 248, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!354 = metadata !{i32 248, i32 58, metadata !343, null}
!355 = metadata !{i32 721152, metadata !343, metadata !"y_filt", metadata !6, i32 249, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!356 = metadata !{i32 249, i32 16, metadata !343, null}
!357 = metadata !{i32 721152, metadata !343, metadata !"x_filt", metadata !6, i32 249, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!358 = metadata !{i32 249, i32 23, metadata !343, null}
!359 = metadata !{i32 721152, metadata !343, metadata !"y_edge", metadata !6, i32 249, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!360 = metadata !{i32 249, i32 31, metadata !343, null}
!361 = metadata !{i32 721152, metadata !343, metadata !"x_edge", metadata !6, i32 249, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!362 = metadata !{i32 249, i32 38, metadata !343, null}
!363 = metadata !{i32 721152, metadata !343, metadata !"x_base", metadata !6, i32 250, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!364 = metadata !{i32 250, i32 16, metadata !343, null}
!365 = metadata !{i32 250, i32 46, metadata !343, null}
!366 = metadata !{i32 721152, metadata !343, metadata !"y_base", metadata !6, i32 251, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!367 = metadata !{i32 251, i32 16, metadata !343, null}
!368 = metadata !{i32 251, i32 54, metadata !343, null}
!369 = metadata !{i32 721152, metadata !343, metadata !"x_edge_dist", metadata !6, i32 252, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!370 = metadata !{i32 252, i32 7, metadata !343, null}
!371 = metadata !{i32 252, i32 69, metadata !343, null}
!372 = metadata !{i32 721152, metadata !343, metadata !"y_edge_dist", metadata !6, i32 253, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!373 = metadata !{i32 253, i32 7, metadata !343, null}
!374 = metadata !{i32 253, i32 79, metadata !343, null}
!375 = metadata !{i32 721152, metadata !343, metadata !"i", metadata !6, i32 254, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!376 = metadata !{i32 254, i32 7, metadata !343, null}
!377 = metadata !{i32 721152, metadata !343, metadata !"mx_pos", metadata !6, i32 255, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!378 = metadata !{i32 255, i32 7, metadata !343, null}
!379 = metadata !{i32 255, i32 27, metadata !343, null}
!380 = metadata !{i32 721152, metadata !343, metadata !"my_pos", metadata !6, i32 256, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!381 = metadata !{i32 256, i32 7, metadata !343, null}
!382 = metadata !{i32 256, i32 27, metadata !343, null}
!383 = metadata !{i32 258, i32 8, metadata !384, null}
!384 = metadata !{i32 720907, metadata !343, i32 258, i32 3, metadata !6, i32 355} ; [ DW_TAG_lexical_block ]
!385 = metadata !{i32 258, i32 29, metadata !384, null}
!386 = metadata !{i32 258, i32 24, metadata !384, null}
!387 = metadata !{i32 261, i32 3, metadata !343, null}
!388 = metadata !{i32 263, i32 7, metadata !389, null}
!389 = metadata !{i32 720907, metadata !343, i32 262, i32 7, metadata !6, i32 356} ; [ DW_TAG_lexical_block ]
!390 = metadata !{i32 263, i32 28, metadata !389, null}
!391 = metadata !{i32 264, i32 12, metadata !389, null}
!392 = metadata !{i32 264, i32 36, metadata !393, null}
!393 = metadata !{i32 720907, metadata !389, i32 264, i32 34, metadata !6, i32 357} ; [ DW_TAG_lexical_block ]
!394 = metadata !{i32 264, i32 55, metadata !393, null}
!395 = metadata !{i32 264, i32 72, metadata !393, null}
!396 = metadata !{i32 266, i32 7, metadata !389, null}
!397 = metadata !{i32 266, i32 28, metadata !389, null}
!398 = metadata !{i32 267, i32 12, metadata !389, null}
!399 = metadata !{i32 267, i32 36, metadata !400, null}
!400 = metadata !{i32 720907, metadata !389, i32 267, i32 34, metadata !6, i32 358} ; [ DW_TAG_lexical_block ]
!401 = metadata !{i32 267, i32 63, metadata !400, null}
!402 = metadata !{i32 267, i32 79, metadata !400, null}
!403 = metadata !{i32 268, i32 7, metadata !389, null}
!404 = metadata !{i32 271, i32 8, metadata !405, null}
!405 = metadata !{i32 720907, metadata !343, i32 271, i32 3, metadata !6, i32 359} ; [ DW_TAG_lexical_block ]
!406 = metadata !{i32 274, i32 10, metadata !407, null}
!407 = metadata !{i32 720907, metadata !405, i32 274, i32 5, metadata !6, i32 360} ; [ DW_TAG_lexical_block ]
!408 = metadata !{i32 277, i32 7, metadata !407, null}
!409 = metadata !{i32 285, i32 4, metadata !410, null}
!410 = metadata !{i32 720907, metadata !407, i32 284, i32 4, metadata !6, i32 361} ; [ DW_TAG_lexical_block ]
!411 = metadata !{i32 287, i32 4, metadata !410, null}
!412 = metadata !{i32 289, i32 4, metadata !410, null}
!413 = metadata !{i32 290, i32 12, metadata !407, null}
!414 = metadata !{i32 276, i32 3, metadata !407, null}
!415 = metadata !{i32 291, i32 18, metadata !407, null}
!416 = metadata !{i32 273, i32 8, metadata !405, null}
!417 = metadata !{i32 293, i32 3, metadata !343, null}
!418 = metadata !{i32 721153, metadata !34, metadata !"filt", metadata !6, i32 16777516, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!419 = metadata !{i32 300, i32 19, metadata !34, null}
!420 = metadata !{i32 721153, metadata !34, metadata !"x_dim", metadata !6, i32 33554733, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!421 = metadata !{i32 301, i32 16, metadata !34, null}
!422 = metadata !{i32 721153, metadata !34, metadata !"y_dim", metadata !6, i32 50331950, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!423 = metadata !{i32 302, i32 7, metadata !34, null}
!424 = metadata !{i32 721153, metadata !34, metadata !"x_pos", metadata !6, i32 67109166, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!425 = metadata !{i32 302, i32 14, metadata !34, null}
!426 = metadata !{i32 721153, metadata !34, metadata !"y_pos", metadata !6, i32 83886382, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!427 = metadata !{i32 302, i32 21, metadata !34, null}
!428 = metadata !{i32 721153, metadata !34, metadata !"result", metadata !6, i32 100663596, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!429 = metadata !{i32 300, i32 26, metadata !34, null}
!430 = metadata !{i32 721153, metadata !34, metadata !"f_or_e", metadata !6, i32 117440814, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!431 = metadata !{i32 302, i32 28, metadata !34, null}
!432 = metadata !{i32 721152, metadata !433, metadata !"i", metadata !6, i32 304, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!433 = metadata !{i32 720907, metadata !34, i32 303, i32 3, metadata !6, i32 362} ; [ DW_TAG_lexical_block ]
!434 = metadata !{i32 304, i32 16, metadata !433, null}
!435 = metadata !{i32 721152, metadata !433, metadata !"size", metadata !6, i32 305, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!436 = metadata !{i32 305, i32 16, metadata !433, null}
!437 = metadata !{i32 305, i32 34, metadata !433, null}
!438 = metadata !{i32 307, i32 3, metadata !433, null}
!439 = metadata !{i32 308, i32 10, metadata !440, null}
!440 = metadata !{i32 720907, metadata !433, i32 308, i32 5, metadata !6, i32 363} ; [ DW_TAG_lexical_block ]
!441 = metadata !{i32 308, i32 28, metadata !440, null}
!442 = metadata !{i32 308, i32 23, metadata !440, null}
!443 = metadata !{i32 308, i32 40, metadata !440, null}
!444 = metadata !{i32 310, i32 10, metadata !445, null}
!445 = metadata !{i32 720907, metadata !433, i32 310, i32 5, metadata !6, i32 364} ; [ DW_TAG_lexical_block ]
!446 = metadata !{i32 310, i32 28, metadata !445, null}
!447 = metadata !{i32 310, i32 23, metadata !445, null}
!448 = metadata !{i32 311, i32 3, metadata !433, null}
!449 = metadata !{i32 721153, metadata !37, metadata !"filt", metadata !6, i32 16777536, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!450 = metadata !{i32 320, i32 19, metadata !37, null}
!451 = metadata !{i32 721153, metadata !37, metadata !"x_dim", metadata !6, i32 33554753, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!452 = metadata !{i32 321, i32 16, metadata !37, null}
!453 = metadata !{i32 721153, metadata !37, metadata !"y_dim", metadata !6, i32 50331970, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!454 = metadata !{i32 322, i32 7, metadata !37, null}
!455 = metadata !{i32 721153, metadata !37, metadata !"x_pos", metadata !6, i32 67109186, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!456 = metadata !{i32 322, i32 14, metadata !37, null}
!457 = metadata !{i32 721153, metadata !37, metadata !"y_pos", metadata !6, i32 83886402, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!458 = metadata !{i32 322, i32 21, metadata !37, null}
!459 = metadata !{i32 721153, metadata !37, metadata !"result", metadata !6, i32 100663616, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!460 = metadata !{i32 320, i32 26, metadata !37, null}
!461 = metadata !{i32 721153, metadata !37, metadata !"f_or_e", metadata !6, i32 117440834, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!462 = metadata !{i32 322, i32 28, metadata !37, null}
!463 = metadata !{i32 721152, metadata !464, metadata !"y_filt", metadata !6, i32 324, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!464 = metadata !{i32 720907, metadata !37, i32 323, i32 3, metadata !6, i32 365} ; [ DW_TAG_lexical_block ]
!465 = metadata !{i32 324, i32 16, metadata !464, null}
!466 = metadata !{i32 721152, metadata !464, metadata !"x_filt", metadata !6, i32 324, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!467 = metadata !{i32 324, i32 23, metadata !464, null}
!468 = metadata !{i32 721152, metadata !464, metadata !"y_edge", metadata !6, i32 324, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!469 = metadata !{i32 324, i32 31, metadata !464, null}
!470 = metadata !{i32 721152, metadata !464, metadata !"x_edge", metadata !6, i32 324, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!471 = metadata !{i32 324, i32 38, metadata !464, null}
!472 = metadata !{i32 721152, metadata !464, metadata !"x_base", metadata !6, i32 325, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!473 = metadata !{i32 325, i32 16, metadata !464, null}
!474 = metadata !{i32 325, i32 46, metadata !464, null}
!475 = metadata !{i32 721152, metadata !464, metadata !"y_base", metadata !6, i32 326, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!476 = metadata !{i32 326, i32 16, metadata !464, null}
!477 = metadata !{i32 326, i32 58, metadata !464, null}
!478 = metadata !{i32 721152, metadata !464, metadata !"filt_sz", metadata !6, i32 327, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!479 = metadata !{i32 327, i32 7, metadata !464, null}
!480 = metadata !{i32 327, i32 28, metadata !464, null}
!481 = metadata !{i32 721152, metadata !464, metadata !"x_edge_dist", metadata !6, i32 328, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!482 = metadata !{i32 328, i32 7, metadata !464, null}
!483 = metadata !{i32 328, i32 69, metadata !464, null}
!484 = metadata !{i32 721152, metadata !464, metadata !"y_edge_dist", metadata !6, i32 329, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!485 = metadata !{i32 329, i32 7, metadata !464, null}
!486 = metadata !{i32 329, i32 81, metadata !464, null}
!487 = metadata !{i32 721152, metadata !464, metadata !"i", metadata !6, i32 330, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!488 = metadata !{i32 330, i32 7, metadata !464, null}
!489 = metadata !{i32 721152, metadata !464, metadata !"norm", metadata !6, i32 331, metadata !490, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!490 = metadata !{i32 720932, null, metadata !"double", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 4} ; [ DW_TAG_base_type ]
!491 = metadata !{i32 331, i32 10, metadata !464, null}
!492 = metadata !{i32 721152, metadata !464, metadata !"onorm", metadata !6, i32 331, metadata !490, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!493 = metadata !{i32 331, i32 15, metadata !464, null}
!494 = metadata !{i32 333, i32 8, metadata !495, null}
!495 = metadata !{i32 720907, metadata !464, i32 333, i32 3, metadata !6, i32 366} ; [ DW_TAG_lexical_block ]
!496 = metadata !{i32 333, i32 29, metadata !495, null}
!497 = metadata !{i32 333, i32 24, metadata !495, null}
!498 = metadata !{i32 336, i32 8, metadata !499, null}
!499 = metadata !{i32 720907, metadata !464, i32 336, i32 3, metadata !6, i32 367} ; [ DW_TAG_lexical_block ]
!500 = metadata !{i32 339, i32 10, metadata !501, null}
!501 = metadata !{i32 720907, metadata !499, i32 339, i32 5, metadata !6, i32 368} ; [ DW_TAG_lexical_block ]
!502 = metadata !{i32 342, i32 7, metadata !501, null}
!503 = metadata !{i32 341, i32 3, metadata !501, null}
!504 = metadata !{i32 343, i32 16, metadata !501, null}
!505 = metadata !{i32 338, i32 8, metadata !499, null}
!506 = metadata !{i32 346, i32 3, metadata !464, null}
!507 = metadata !{i32 347, i32 10, metadata !508, null}
!508 = metadata !{i32 720907, metadata !464, i32 347, i32 5, metadata !6, i32 369} ; [ DW_TAG_lexical_block ]
!509 = metadata !{i32 348, i32 7, metadata !508, null}
!510 = metadata !{i32 347, i32 41, metadata !508, null}
!511 = metadata !{i32 348, i32 25, metadata !508, null}
!512 = metadata !{i32 349, i32 3, metadata !464, null}
!513 = metadata !{i32 350, i32 10, metadata !514, null}
!514 = metadata !{i32 720907, metadata !464, i32 350, i32 5, metadata !6, i32 370} ; [ DW_TAG_lexical_block ]
!515 = metadata !{i32 351, i32 7, metadata !514, null}
!516 = metadata !{i32 350, i32 46, metadata !514, null}
!517 = metadata !{i32 351, i32 25, metadata !514, null}
!518 = metadata !{i32 354, i32 8, metadata !519, null}
!519 = metadata !{i32 720907, metadata !464, i32 354, i32 3, metadata !6, i32 371} ; [ DW_TAG_lexical_block ]
!520 = metadata !{i32 355, i32 5, metadata !519, null}
!521 = metadata !{i32 354, i32 33, metadata !519, null}
!522 = metadata !{i32 356, i32 8, metadata !464, null}
!523 = metadata !{i32 358, i32 8, metadata !524, null}
!524 = metadata !{i32 720907, metadata !464, i32 358, i32 3, metadata !6, i32 372} ; [ DW_TAG_lexical_block ]
!525 = metadata !{i32 359, i32 5, metadata !524, null}
!526 = metadata !{i32 358, i32 34, metadata !524, null}
!527 = metadata !{i32 360, i32 11, metadata !464, null}
!528 = metadata !{i32 362, i32 3, metadata !464, null}
!529 = metadata !{i32 363, i32 8, metadata !530, null}
!530 = metadata !{i32 720907, metadata !464, i32 363, i32 3, metadata !6, i32 373} ; [ DW_TAG_lexical_block ]
!531 = metadata !{i32 364, i32 5, metadata !530, null}
!532 = metadata !{i32 363, i32 24, metadata !530, null}
!533 = metadata !{i32 365, i32 3, metadata !464, null}
!534 = metadata !{i32 721153, metadata !40, metadata !"filt", metadata !6, i32 16777592, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!535 = metadata !{i32 376, i32 19, metadata !40, null}
!536 = metadata !{i32 721153, metadata !40, metadata !"x_dim", metadata !6, i32 33554809, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!537 = metadata !{i32 377, i32 16, metadata !40, null}
!538 = metadata !{i32 721153, metadata !40, metadata !"y_dim", metadata !6, i32 50332026, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!539 = metadata !{i32 378, i32 7, metadata !40, null}
!540 = metadata !{i32 721153, metadata !40, metadata !"x_pos", metadata !6, i32 67109242, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!541 = metadata !{i32 378, i32 14, metadata !40, null}
!542 = metadata !{i32 721153, metadata !40, metadata !"y_pos", metadata !6, i32 83886458, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!543 = metadata !{i32 378, i32 21, metadata !40, null}
!544 = metadata !{i32 721153, metadata !40, metadata !"result", metadata !6, i32 100663672, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!545 = metadata !{i32 376, i32 26, metadata !40, null}
!546 = metadata !{i32 721153, metadata !40, metadata !"f_or_e", metadata !6, i32 117440890, metadata !13, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!547 = metadata !{i32 378, i32 28, metadata !40, null}
!548 = metadata !{i32 721152, metadata !549, metadata !"y_filt", metadata !6, i32 380, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!549 = metadata !{i32 720907, metadata !40, i32 379, i32 3, metadata !6, i32 374} ; [ DW_TAG_lexical_block ]
!550 = metadata !{i32 380, i32 16, metadata !549, null}
!551 = metadata !{i32 721152, metadata !549, metadata !"x_filt", metadata !6, i32 380, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!552 = metadata !{i32 380, i32 23, metadata !549, null}
!553 = metadata !{i32 721152, metadata !549, metadata !"y_res", metadata !6, i32 380, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!554 = metadata !{i32 380, i32 31, metadata !549, null}
!555 = metadata !{i32 721152, metadata !549, metadata !"x_res", metadata !6, i32 380, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!556 = metadata !{i32 380, i32 37, metadata !549, null}
!557 = metadata !{i32 721152, metadata !549, metadata !"taps_used", metadata !6, i32 381, metadata !72, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!558 = metadata !{i32 381, i32 18, metadata !549, null}
!559 = metadata !{i32 381, i32 31, metadata !549, null}
!560 = metadata !{i32 721152, metadata !549, metadata !"fraction", metadata !6, i32 382, metadata !72, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!561 = metadata !{i32 382, i32 18, metadata !549, null}
!562 = metadata !{i32 721152, metadata !549, metadata !"filt_sz", metadata !6, i32 383, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!563 = metadata !{i32 383, i32 7, metadata !549, null}
!564 = metadata !{i32 383, i32 28, metadata !549, null}
!565 = metadata !{i32 721152, metadata !549, metadata !"x_start", metadata !6, i32 384, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!566 = metadata !{i32 384, i32 7, metadata !549, null}
!567 = metadata !{i32 384, i32 62, metadata !549, null}
!568 = metadata !{i32 721152, metadata !549, metadata !"y_start", metadata !6, i32 385, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!569 = metadata !{i32 385, i32 7, metadata !549, null}
!570 = metadata !{i32 385, i32 70, metadata !549, null}
!571 = metadata !{i32 721152, metadata !549, metadata !"i", metadata !6, i32 386, metadata !13, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!572 = metadata !{i32 386, i32 7, metadata !549, null}
!573 = metadata !{i32 388, i32 8, metadata !574, null}
!574 = metadata !{i32 720907, metadata !549, i32 388, i32 3, metadata !6, i32 375} ; [ DW_TAG_lexical_block ]
!575 = metadata !{i32 388, i32 29, metadata !574, null}
!576 = metadata !{i32 388, i32 24, metadata !574, null}
!577 = metadata !{i32 390, i32 8, metadata !578, null}
!578 = metadata !{i32 720907, metadata !549, i32 390, i32 3, metadata !6, i32 376} ; [ DW_TAG_lexical_block ]
!579 = metadata !{i32 393, i32 7, metadata !578, null}
!580 = metadata !{i32 394, i32 7, metadata !581, null}
!581 = metadata !{i32 720907, metadata !578, i32 394, i32 2, metadata !6, i32 377} ; [ DW_TAG_lexical_block ]
!582 = metadata !{i32 397, i32 4, metadata !581, null}
!583 = metadata !{i32 399, i32 8, metadata !584, null}
!584 = metadata !{i32 720907, metadata !581, i32 398, i32 8, metadata !6, i32 378} ; [ DW_TAG_lexical_block ]
!585 = metadata !{i32 400, i32 8, metadata !584, null}
!586 = metadata !{i32 401, i32 8, metadata !584, null}
!587 = metadata !{i32 396, i32 7, metadata !581, null}
!588 = metadata !{i32 401, i32 8, metadata !581, null}
!589 = metadata !{i32 392, i32 8, metadata !578, null}
!590 = metadata !{i32 403, i32 3, metadata !549, null}
!591 = metadata !{i32 405, i32 7, metadata !592, null}
!592 = metadata !{i32 720907, metadata !549, i32 404, i32 7, metadata !6, i32 379} ; [ DW_TAG_lexical_block ]
!593 = metadata !{i32 406, i32 12, metadata !594, null}
!594 = metadata !{i32 720907, metadata !592, i32 406, i32 7, metadata !6, i32 380} ; [ DW_TAG_lexical_block ]
!595 = metadata !{i32 406, i32 33, metadata !594, null}
!596 = metadata !{i32 406, i32 28, metadata !594, null}
!597 = metadata !{i32 407, i32 7, metadata !592, null}
!598 = metadata !{i32 408, i32 3, metadata !549, null}
