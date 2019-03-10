; ModuleID = 'sqcts.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct.squareCtsContext.3 = type { [9 x [4 x i32]], [9 x [4 x i32]], [16 x i8] }

@.str = private unnamed_addr constant [22 x i8] c"ctxCts != ((void *)0)\00", align 1
@.str1 = private unnamed_addr constant [8 x i8] c"sqcts.c\00", align 1
@__PRETTY_FUNCTION__.squareCtsInit = private unnamed_addr constant [53 x i8] c"void squareCtsInit(squareCtsContext *, const byte *)\00", align 1
@.str2 = private unnamed_addr constant [19 x i8] c"key != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.squareCtsSetIV = private unnamed_addr constant [54 x i8] c"void squareCtsSetIV(squareCtsContext *, const byte *)\00", align 1
@__PRETTY_FUNCTION__.squareCtsEncrypt = private unnamed_addr constant [64 x i8] c"void squareCtsEncrypt(squareCtsContext *, byte *, unsigned int)\00", align 1
@.str3 = private unnamed_addr constant [22 x i8] c"buffer != ((void *)0)\00", align 1
@.str4 = private unnamed_addr constant [29 x i8] c"length >= (4*sizeof(word32))\00", align 1
@__PRETTY_FUNCTION__.squareCtsDecrypt = private unnamed_addr constant [64 x i8] c"void squareCtsDecrypt(squareCtsContext *, byte *, unsigned int)\00", align 1
@__PRETTY_FUNCTION__.squareCtsFinal = private unnamed_addr constant [40 x i8] c"void squareCtsFinal(squareCtsContext *)\00", align 1

define arm_aapcscc void @squareCtsInit(%struct.squareCtsContext.3* %ctxCts, i8* %key) nounwind uwtable {
entry:
  %ctxCts.addr = alloca %struct.squareCtsContext.3*, align 4
  %key.addr = alloca i8*, align 4
  store %struct.squareCtsContext.3* %ctxCts, %struct.squareCtsContext.3** %ctxCts.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.squareCtsContext.3** %ctxCts.addr}, metadata !23), !dbg !44
  store i8* %key, i8** %key.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %key.addr}, metadata !45), !dbg !48
  %0 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !49
  %cmp = icmp ne %struct.squareCtsContext.3* %0, null, !dbg !49
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !49

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !49

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([22 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 25, i8* getelementptr inbounds ([53 x i8]* @__PRETTY_FUNCTION__.squareCtsInit, i32 0, i32 0)) noreturn nounwind, !dbg !51
  unreachable, !dbg !51
                                                  ; No predecessors!
  br label %cond.end, !dbg !51

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i8** %key.addr, align 4, !dbg !52
  %cmp1 = icmp ne i8* %2, null, !dbg !52
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !52

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !52

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([19 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 26, i8* getelementptr inbounds ([53 x i8]* @__PRETTY_FUNCTION__.squareCtsInit, i32 0, i32 0)) noreturn nounwind, !dbg !53
  unreachable, !dbg !53
                                                  ; No predecessors!
  br label %cond.end4, !dbg !53

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !54
  %5 = bitcast %struct.squareCtsContext.3* %4 to i8*, !dbg !54
  call void @llvm.memset.p0i8.i32(i8* %5, i8 0, i32 304, i32 1, i1 false), !dbg !54
  %6 = load i8** %key.addr, align 4, !dbg !55
  %7 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !55
  %roundKeys_e = getelementptr inbounds %struct.squareCtsContext.3* %7, i32 0, i32 0, !dbg !55
  %arraydecay = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_e, i32 0, i32 0, !dbg !55
  %8 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !55
  %roundKeys_d = getelementptr inbounds %struct.squareCtsContext.3* %8, i32 0, i32 1, !dbg !55
  %arraydecay5 = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_d, i32 0, i32 0, !dbg !55
  call arm_aapcscc  void @squareGenerateRoundKeys(i8* %6, [4 x i32]* %arraydecay, [4 x i32]* %arraydecay5), !dbg !55
  ret void, !dbg !56
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc void @__assert_fail(i8*, i8*, i32, i8*) noreturn nounwind

declare void @llvm.memset.p0i8.i32(i8* nocapture, i8, i32, i32, i1) nounwind

declare arm_aapcscc void @squareGenerateRoundKeys(i8*, [4 x i32]*, [4 x i32]*)

define arm_aapcscc void @squareCtsSetIV(%struct.squareCtsContext.3* %ctxCts, i8* %iv) nounwind uwtable {
entry:
  %ctxCts.addr = alloca %struct.squareCtsContext.3*, align 4
  %iv.addr = alloca i8*, align 4
  store %struct.squareCtsContext.3* %ctxCts, %struct.squareCtsContext.3** %ctxCts.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.squareCtsContext.3** %ctxCts.addr}, metadata !57), !dbg !58
  store i8* %iv, i8** %iv.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %iv.addr}, metadata !59), !dbg !60
  %0 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !61
  %cmp = icmp ne %struct.squareCtsContext.3* %0, null, !dbg !61
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !61

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !61

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([22 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 34, i8* getelementptr inbounds ([54 x i8]* @__PRETTY_FUNCTION__.squareCtsSetIV, i32 0, i32 0)) noreturn nounwind, !dbg !63
  unreachable, !dbg !63
                                                  ; No predecessors!
  br label %cond.end, !dbg !63

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i8** %iv.addr, align 4, !dbg !64
  %cmp1 = icmp ne i8* %2, null, !dbg !64
  br i1 %cmp1, label %if.then, label %if.end, !dbg !64

if.then:                                          ; preds = %cond.end
  %3 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !65
  %mask = getelementptr inbounds %struct.squareCtsContext.3* %3, i32 0, i32 2, !dbg !65
  %arraydecay = getelementptr inbounds [16 x i8]* %mask, i32 0, i32 0, !dbg !65
  %4 = load i8** %iv.addr, align 4, !dbg !65
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %arraydecay, i8* %4, i32 16, i32 1, i1 false), !dbg !65
  br label %if.end, !dbg !67

if.end:                                           ; preds = %if.then, %cond.end
  %5 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !68
  %mask2 = getelementptr inbounds %struct.squareCtsContext.3* %5, i32 0, i32 2, !dbg !68
  %arraydecay3 = getelementptr inbounds [16 x i8]* %mask2, i32 0, i32 0, !dbg !68
  %6 = bitcast i8* %arraydecay3 to i32*, !dbg !68
  %7 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !68
  %roundKeys_e = getelementptr inbounds %struct.squareCtsContext.3* %7, i32 0, i32 0, !dbg !68
  %arraydecay4 = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_e, i32 0, i32 0, !dbg !68
  call arm_aapcscc  void @squareEncrypt(i32* %6, [4 x i32]* %arraydecay4), !dbg !68
  ret void, !dbg !69
}

declare void @llvm.memcpy.p0i8.p0i8.i32(i8* nocapture, i8* nocapture, i32, i32, i1) nounwind

declare arm_aapcscc void @squareEncrypt(i32*, [4 x i32]*)

define arm_aapcscc void @squareCtsEncrypt(%struct.squareCtsContext.3* %ctxCts, i8* %buffer, i32 %length) nounwind uwtable {
entry:
  %ctxCts.addr = alloca %struct.squareCtsContext.3*, align 4
  %buffer.addr = alloca i8*, align 4
  %length.addr = alloca i32, align 4
  %mask = alloca i8*, align 4
  %i = alloca i32, align 4
  store %struct.squareCtsContext.3* %ctxCts, %struct.squareCtsContext.3** %ctxCts.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.squareCtsContext.3** %ctxCts.addr}, metadata !70), !dbg !71
  store i8* %buffer, i8** %buffer.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %buffer.addr}, metadata !72), !dbg !74
  store i32 %length, i32* %length.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %length.addr}, metadata !75), !dbg !77
  call void @llvm.dbg.declare(metadata !{i8** %mask}, metadata !78), !dbg !80
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !81), !dbg !82
  %0 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !83
  %cmp = icmp ne %struct.squareCtsContext.3* %0, null, !dbg !83
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !83

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !83

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([22 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 48, i8* getelementptr inbounds ([64 x i8]* @__PRETTY_FUNCTION__.squareCtsEncrypt, i32 0, i32 0)) noreturn nounwind, !dbg !84
  unreachable, !dbg !84
                                                  ; No predecessors!
  br label %cond.end, !dbg !84

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i8** %buffer.addr, align 4, !dbg !85
  %cmp1 = icmp ne i8* %2, null, !dbg !85
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !85

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !85

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([22 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 49, i8* getelementptr inbounds ([64 x i8]* @__PRETTY_FUNCTION__.squareCtsEncrypt, i32 0, i32 0)) noreturn nounwind, !dbg !86
  unreachable, !dbg !86
                                                  ; No predecessors!
  br label %cond.end4, !dbg !86

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i32* %length.addr, align 4, !dbg !87
  %cmp5 = icmp uge i32 %4, 16, !dbg !87
  br i1 %cmp5, label %cond.true6, label %cond.false7, !dbg !87

cond.true6:                                       ; preds = %cond.end4
  br label %cond.end8, !dbg !87

cond.false7:                                      ; preds = %cond.end4
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([29 x i8]* @.str4, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 50, i8* getelementptr inbounds ([64 x i8]* @__PRETTY_FUNCTION__.squareCtsEncrypt, i32 0, i32 0)) noreturn nounwind, !dbg !88
  unreachable, !dbg !88
                                                  ; No predecessors!
  br label %cond.end8, !dbg !88

cond.end8:                                        ; preds = %5, %cond.true6
  %6 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !89
  %mask9 = getelementptr inbounds %struct.squareCtsContext.3* %6, i32 0, i32 2, !dbg !89
  %arraydecay = getelementptr inbounds [16 x i8]* %mask9, i32 0, i32 0, !dbg !89
  store i8* %arraydecay, i8** %mask, align 4, !dbg !89
  br label %while.cond, !dbg !90

while.cond:                                       ; preds = %while.body, %cond.end8
  %7 = load i32* %length.addr, align 4, !dbg !90
  %cmp10 = icmp uge i32 %7, 16, !dbg !90
  br i1 %cmp10, label %while.body, label %while.end, !dbg !90

while.body:                                       ; preds = %while.cond
  %8 = load i8** %mask, align 4, !dbg !91
  %9 = bitcast i8* %8 to i32*, !dbg !91
  %arrayidx = getelementptr inbounds i32* %9, i32 0, !dbg !91
  %10 = load i32* %arrayidx, !dbg !91
  %11 = load i8** %buffer.addr, align 4, !dbg !91
  %12 = bitcast i8* %11 to i32*, !dbg !91
  %arrayidx11 = getelementptr inbounds i32* %12, i32 0, !dbg !91
  %13 = load i32* %arrayidx11, !dbg !91
  %xor = xor i32 %13, %10, !dbg !91
  store i32 %xor, i32* %arrayidx11, !dbg !91
  %14 = load i8** %mask, align 4, !dbg !93
  %15 = bitcast i8* %14 to i32*, !dbg !93
  %arrayidx12 = getelementptr inbounds i32* %15, i32 1, !dbg !93
  %16 = load i32* %arrayidx12, !dbg !93
  %17 = load i8** %buffer.addr, align 4, !dbg !93
  %18 = bitcast i8* %17 to i32*, !dbg !93
  %arrayidx13 = getelementptr inbounds i32* %18, i32 1, !dbg !93
  %19 = load i32* %arrayidx13, !dbg !93
  %xor14 = xor i32 %19, %16, !dbg !93
  store i32 %xor14, i32* %arrayidx13, !dbg !93
  %20 = load i8** %mask, align 4, !dbg !94
  %21 = bitcast i8* %20 to i32*, !dbg !94
  %arrayidx15 = getelementptr inbounds i32* %21, i32 2, !dbg !94
  %22 = load i32* %arrayidx15, !dbg !94
  %23 = load i8** %buffer.addr, align 4, !dbg !94
  %24 = bitcast i8* %23 to i32*, !dbg !94
  %arrayidx16 = getelementptr inbounds i32* %24, i32 2, !dbg !94
  %25 = load i32* %arrayidx16, !dbg !94
  %xor17 = xor i32 %25, %22, !dbg !94
  store i32 %xor17, i32* %arrayidx16, !dbg !94
  %26 = load i8** %mask, align 4, !dbg !95
  %27 = bitcast i8* %26 to i32*, !dbg !95
  %arrayidx18 = getelementptr inbounds i32* %27, i32 3, !dbg !95
  %28 = load i32* %arrayidx18, !dbg !95
  %29 = load i8** %buffer.addr, align 4, !dbg !95
  %30 = bitcast i8* %29 to i32*, !dbg !95
  %arrayidx19 = getelementptr inbounds i32* %30, i32 3, !dbg !95
  %31 = load i32* %arrayidx19, !dbg !95
  %xor20 = xor i32 %31, %28, !dbg !95
  store i32 %xor20, i32* %arrayidx19, !dbg !95
  %32 = load i8** %buffer.addr, align 4, !dbg !96
  %33 = bitcast i8* %32 to i32*, !dbg !96
  %34 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !96
  %roundKeys_e = getelementptr inbounds %struct.squareCtsContext.3* %34, i32 0, i32 0, !dbg !96
  %arraydecay21 = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_e, i32 0, i32 0, !dbg !96
  call arm_aapcscc  void @squareEncrypt(i32* %33, [4 x i32]* %arraydecay21), !dbg !96
  %35 = load i8** %buffer.addr, align 4, !dbg !97
  store i8* %35, i8** %mask, align 4, !dbg !97
  %36 = load i8** %buffer.addr, align 4, !dbg !98
  %add.ptr = getelementptr inbounds i8* %36, i32 16, !dbg !98
  store i8* %add.ptr, i8** %buffer.addr, align 4, !dbg !98
  %37 = load i32* %length.addr, align 4, !dbg !99
  %sub = sub i32 %37, 16, !dbg !99
  store i32 %sub, i32* %length.addr, align 4, !dbg !99
  br label %while.cond, !dbg !100

while.end:                                        ; preds = %while.cond
  %38 = load i8** %mask, align 4, !dbg !101
  %39 = bitcast i8* %38 to i32*, !dbg !101
  %arrayidx22 = getelementptr inbounds i32* %39, i32 0, !dbg !101
  %40 = load i32* %arrayidx22, !dbg !101
  %41 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !101
  %mask23 = getelementptr inbounds %struct.squareCtsContext.3* %41, i32 0, i32 2, !dbg !101
  %arraydecay24 = getelementptr inbounds [16 x i8]* %mask23, i32 0, i32 0, !dbg !101
  %42 = bitcast i8* %arraydecay24 to i32*, !dbg !101
  %arrayidx25 = getelementptr inbounds i32* %42, i32 0, !dbg !101
  store i32 %40, i32* %arrayidx25, !dbg !101
  %43 = load i8** %mask, align 4, !dbg !103
  %44 = bitcast i8* %43 to i32*, !dbg !103
  %arrayidx26 = getelementptr inbounds i32* %44, i32 1, !dbg !103
  %45 = load i32* %arrayidx26, !dbg !103
  %46 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !103
  %mask27 = getelementptr inbounds %struct.squareCtsContext.3* %46, i32 0, i32 2, !dbg !103
  %arraydecay28 = getelementptr inbounds [16 x i8]* %mask27, i32 0, i32 0, !dbg !103
  %47 = bitcast i8* %arraydecay28 to i32*, !dbg !103
  %arrayidx29 = getelementptr inbounds i32* %47, i32 1, !dbg !103
  store i32 %45, i32* %arrayidx29, !dbg !103
  %48 = load i8** %mask, align 4, !dbg !104
  %49 = bitcast i8* %48 to i32*, !dbg !104
  %arrayidx30 = getelementptr inbounds i32* %49, i32 2, !dbg !104
  %50 = load i32* %arrayidx30, !dbg !104
  %51 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !104
  %mask31 = getelementptr inbounds %struct.squareCtsContext.3* %51, i32 0, i32 2, !dbg !104
  %arraydecay32 = getelementptr inbounds [16 x i8]* %mask31, i32 0, i32 0, !dbg !104
  %52 = bitcast i8* %arraydecay32 to i32*, !dbg !104
  %arrayidx33 = getelementptr inbounds i32* %52, i32 2, !dbg !104
  store i32 %50, i32* %arrayidx33, !dbg !104
  %53 = load i8** %mask, align 4, !dbg !105
  %54 = bitcast i8* %53 to i32*, !dbg !105
  %arrayidx34 = getelementptr inbounds i32* %54, i32 3, !dbg !105
  %55 = load i32* %arrayidx34, !dbg !105
  %56 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !105
  %mask35 = getelementptr inbounds %struct.squareCtsContext.3* %56, i32 0, i32 2, !dbg !105
  %arraydecay36 = getelementptr inbounds [16 x i8]* %mask35, i32 0, i32 0, !dbg !105
  %57 = bitcast i8* %arraydecay36 to i32*, !dbg !105
  %arrayidx37 = getelementptr inbounds i32* %57, i32 3, !dbg !105
  store i32 %55, i32* %arrayidx37, !dbg !105
  %58 = load i32* %length.addr, align 4, !dbg !106
  %cmp38 = icmp ne i32 %58, 0, !dbg !106
  br i1 %cmp38, label %if.then, label %if.end, !dbg !106

if.then:                                          ; preds = %while.end
  store i32 0, i32* %i, align 4, !dbg !107
  br label %for.cond, !dbg !107

for.cond:                                         ; preds = %for.inc, %if.then
  %59 = load i32* %i, align 4, !dbg !107
  %60 = load i32* %length.addr, align 4, !dbg !107
  %cmp39 = icmp ult i32 %59, %60, !dbg !107
  br i1 %cmp39, label %for.body, label %for.end, !dbg !107

for.body:                                         ; preds = %for.cond
  %61 = load i32* %i, align 4, !dbg !110
  %62 = load i8** %buffer.addr, align 4, !dbg !110
  %arrayidx40 = getelementptr inbounds i8* %62, i32 %61, !dbg !110
  %63 = load i8* %arrayidx40, !dbg !110
  %conv = zext i8 %63 to i32, !dbg !110
  %64 = load i32* %i, align 4, !dbg !110
  %65 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !110
  %mask41 = getelementptr inbounds %struct.squareCtsContext.3* %65, i32 0, i32 2, !dbg !110
  %arrayidx42 = getelementptr inbounds [16 x i8]* %mask41, i32 0, i32 %64, !dbg !110
  %66 = load i8* %arrayidx42, align 1, !dbg !110
  %conv43 = zext i8 %66 to i32, !dbg !110
  %xor44 = xor i32 %conv43, %conv, !dbg !110
  %conv45 = trunc i32 %xor44 to i8, !dbg !110
  store i8 %conv45, i8* %arrayidx42, align 1, !dbg !110
  br label %for.inc, !dbg !112

for.inc:                                          ; preds = %for.body
  %67 = load i32* %i, align 4, !dbg !113
  %inc = add i32 %67, 1, !dbg !113
  store i32 %inc, i32* %i, align 4, !dbg !113
  br label %for.cond, !dbg !113

for.end:                                          ; preds = %for.cond
  %68 = load i8** %buffer.addr, align 4, !dbg !114
  %69 = load i8** %mask, align 4, !dbg !114
  %70 = load i32* %length.addr, align 4, !dbg !114
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %68, i8* %69, i32 %70, i32 1, i1 false), !dbg !114
  %71 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !115
  %mask46 = getelementptr inbounds %struct.squareCtsContext.3* %71, i32 0, i32 2, !dbg !115
  %arraydecay47 = getelementptr inbounds [16 x i8]* %mask46, i32 0, i32 0, !dbg !115
  %72 = bitcast i8* %arraydecay47 to i32*, !dbg !115
  %73 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !115
  %roundKeys_e48 = getelementptr inbounds %struct.squareCtsContext.3* %73, i32 0, i32 0, !dbg !115
  %arraydecay49 = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_e48, i32 0, i32 0, !dbg !115
  call arm_aapcscc  void @squareEncrypt(i32* %72, [4 x i32]* %arraydecay49), !dbg !115
  %74 = load i8** %mask, align 4, !dbg !116
  %75 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !116
  %mask50 = getelementptr inbounds %struct.squareCtsContext.3* %75, i32 0, i32 2, !dbg !116
  %arraydecay51 = getelementptr inbounds [16 x i8]* %mask50, i32 0, i32 0, !dbg !116
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %74, i8* %arraydecay51, i32 16, i32 1, i1 false), !dbg !116
  br label %if.end, !dbg !117

if.end:                                           ; preds = %for.end, %while.end
  store i8* null, i8** %mask, align 4, !dbg !118
  ret void, !dbg !119
}

define arm_aapcscc void @squareCtsDecrypt(%struct.squareCtsContext.3* %ctxCts, i8* %buffer, i32 %length) nounwind uwtable {
entry:
  %ctxCts.addr = alloca %struct.squareCtsContext.3*, align 4
  %buffer.addr = alloca i8*, align 4
  %length.addr = alloca i32, align 4
  %i = alloca i32, align 4
  %temp = alloca [16 x i8], align 1
  store %struct.squareCtsContext.3* %ctxCts, %struct.squareCtsContext.3** %ctxCts.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.squareCtsContext.3** %ctxCts.addr}, metadata !120), !dbg !121
  store i8* %buffer, i8** %buffer.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %buffer.addr}, metadata !122), !dbg !123
  store i32 %length, i32* %length.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %length.addr}, metadata !124), !dbg !125
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !126), !dbg !128
  call void @llvm.dbg.declare(metadata !{[16 x i8]* %temp}, metadata !129), !dbg !131
  %0 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !132
  %cmp = icmp ne %struct.squareCtsContext.3* %0, null, !dbg !132
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !132

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !132

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([22 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 86, i8* getelementptr inbounds ([64 x i8]* @__PRETTY_FUNCTION__.squareCtsDecrypt, i32 0, i32 0)) noreturn nounwind, !dbg !133
  unreachable, !dbg !133
                                                  ; No predecessors!
  br label %cond.end, !dbg !133

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i8** %buffer.addr, align 4, !dbg !134
  %cmp1 = icmp ne i8* %2, null, !dbg !134
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !134

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !134

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([22 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 87, i8* getelementptr inbounds ([64 x i8]* @__PRETTY_FUNCTION__.squareCtsDecrypt, i32 0, i32 0)) noreturn nounwind, !dbg !135
  unreachable, !dbg !135
                                                  ; No predecessors!
  br label %cond.end4, !dbg !135

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i32* %length.addr, align 4, !dbg !136
  %cmp5 = icmp uge i32 %4, 16, !dbg !136
  br i1 %cmp5, label %cond.true6, label %cond.false7, !dbg !136

cond.true6:                                       ; preds = %cond.end4
  br label %cond.end8, !dbg !136

cond.false7:                                      ; preds = %cond.end4
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([29 x i8]* @.str4, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 88, i8* getelementptr inbounds ([64 x i8]* @__PRETTY_FUNCTION__.squareCtsDecrypt, i32 0, i32 0)) noreturn nounwind, !dbg !137
  unreachable, !dbg !137
                                                  ; No predecessors!
  br label %cond.end8, !dbg !137

cond.end8:                                        ; preds = %5, %cond.true6
  br label %while.cond, !dbg !138

while.cond:                                       ; preds = %while.body, %cond.end8
  %6 = load i32* %length.addr, align 4, !dbg !138
  %cmp9 = icmp uge i32 %6, 32, !dbg !138
  br i1 %cmp9, label %while.body, label %while.end, !dbg !138

while.body:                                       ; preds = %while.cond
  %7 = load i8** %buffer.addr, align 4, !dbg !139
  %8 = bitcast i8* %7 to i32*, !dbg !139
  %arrayidx = getelementptr inbounds i32* %8, i32 0, !dbg !139
  %9 = load i32* %arrayidx, !dbg !139
  %arraydecay = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !139
  %10 = bitcast i8* %arraydecay to i32*, !dbg !139
  %arrayidx10 = getelementptr inbounds i32* %10, i32 0, !dbg !139
  store i32 %9, i32* %arrayidx10, !dbg !139
  %11 = load i8** %buffer.addr, align 4, !dbg !142
  %12 = bitcast i8* %11 to i32*, !dbg !142
  %arrayidx11 = getelementptr inbounds i32* %12, i32 1, !dbg !142
  %13 = load i32* %arrayidx11, !dbg !142
  %arraydecay12 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !142
  %14 = bitcast i8* %arraydecay12 to i32*, !dbg !142
  %arrayidx13 = getelementptr inbounds i32* %14, i32 1, !dbg !142
  store i32 %13, i32* %arrayidx13, !dbg !142
  %15 = load i8** %buffer.addr, align 4, !dbg !143
  %16 = bitcast i8* %15 to i32*, !dbg !143
  %arrayidx14 = getelementptr inbounds i32* %16, i32 2, !dbg !143
  %17 = load i32* %arrayidx14, !dbg !143
  %arraydecay15 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !143
  %18 = bitcast i8* %arraydecay15 to i32*, !dbg !143
  %arrayidx16 = getelementptr inbounds i32* %18, i32 2, !dbg !143
  store i32 %17, i32* %arrayidx16, !dbg !143
  %19 = load i8** %buffer.addr, align 4, !dbg !144
  %20 = bitcast i8* %19 to i32*, !dbg !144
  %arrayidx17 = getelementptr inbounds i32* %20, i32 3, !dbg !144
  %21 = load i32* %arrayidx17, !dbg !144
  %arraydecay18 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !144
  %22 = bitcast i8* %arraydecay18 to i32*, !dbg !144
  %arrayidx19 = getelementptr inbounds i32* %22, i32 3, !dbg !144
  store i32 %21, i32* %arrayidx19, !dbg !144
  %23 = load i8** %buffer.addr, align 4, !dbg !145
  %24 = bitcast i8* %23 to i32*, !dbg !145
  %25 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !145
  %roundKeys_d = getelementptr inbounds %struct.squareCtsContext.3* %25, i32 0, i32 1, !dbg !145
  %arraydecay20 = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_d, i32 0, i32 0, !dbg !145
  call arm_aapcscc  void @squareDecrypt(i32* %24, [4 x i32]* %arraydecay20), !dbg !145
  %26 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !146
  %mask = getelementptr inbounds %struct.squareCtsContext.3* %26, i32 0, i32 2, !dbg !146
  %arraydecay21 = getelementptr inbounds [16 x i8]* %mask, i32 0, i32 0, !dbg !146
  %27 = bitcast i8* %arraydecay21 to i32*, !dbg !146
  %arrayidx22 = getelementptr inbounds i32* %27, i32 0, !dbg !146
  %28 = load i32* %arrayidx22, !dbg !146
  %29 = load i8** %buffer.addr, align 4, !dbg !146
  %30 = bitcast i8* %29 to i32*, !dbg !146
  %arrayidx23 = getelementptr inbounds i32* %30, i32 0, !dbg !146
  %31 = load i32* %arrayidx23, !dbg !146
  %xor = xor i32 %31, %28, !dbg !146
  store i32 %xor, i32* %arrayidx23, !dbg !146
  %32 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !147
  %mask24 = getelementptr inbounds %struct.squareCtsContext.3* %32, i32 0, i32 2, !dbg !147
  %arraydecay25 = getelementptr inbounds [16 x i8]* %mask24, i32 0, i32 0, !dbg !147
  %33 = bitcast i8* %arraydecay25 to i32*, !dbg !147
  %arrayidx26 = getelementptr inbounds i32* %33, i32 1, !dbg !147
  %34 = load i32* %arrayidx26, !dbg !147
  %35 = load i8** %buffer.addr, align 4, !dbg !147
  %36 = bitcast i8* %35 to i32*, !dbg !147
  %arrayidx27 = getelementptr inbounds i32* %36, i32 1, !dbg !147
  %37 = load i32* %arrayidx27, !dbg !147
  %xor28 = xor i32 %37, %34, !dbg !147
  store i32 %xor28, i32* %arrayidx27, !dbg !147
  %38 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !148
  %mask29 = getelementptr inbounds %struct.squareCtsContext.3* %38, i32 0, i32 2, !dbg !148
  %arraydecay30 = getelementptr inbounds [16 x i8]* %mask29, i32 0, i32 0, !dbg !148
  %39 = bitcast i8* %arraydecay30 to i32*, !dbg !148
  %arrayidx31 = getelementptr inbounds i32* %39, i32 2, !dbg !148
  %40 = load i32* %arrayidx31, !dbg !148
  %41 = load i8** %buffer.addr, align 4, !dbg !148
  %42 = bitcast i8* %41 to i32*, !dbg !148
  %arrayidx32 = getelementptr inbounds i32* %42, i32 2, !dbg !148
  %43 = load i32* %arrayidx32, !dbg !148
  %xor33 = xor i32 %43, %40, !dbg !148
  store i32 %xor33, i32* %arrayidx32, !dbg !148
  %44 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !149
  %mask34 = getelementptr inbounds %struct.squareCtsContext.3* %44, i32 0, i32 2, !dbg !149
  %arraydecay35 = getelementptr inbounds [16 x i8]* %mask34, i32 0, i32 0, !dbg !149
  %45 = bitcast i8* %arraydecay35 to i32*, !dbg !149
  %arrayidx36 = getelementptr inbounds i32* %45, i32 3, !dbg !149
  %46 = load i32* %arrayidx36, !dbg !149
  %47 = load i8** %buffer.addr, align 4, !dbg !149
  %48 = bitcast i8* %47 to i32*, !dbg !149
  %arrayidx37 = getelementptr inbounds i32* %48, i32 3, !dbg !149
  %49 = load i32* %arrayidx37, !dbg !149
  %xor38 = xor i32 %49, %46, !dbg !149
  store i32 %xor38, i32* %arrayidx37, !dbg !149
  %arraydecay39 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !150
  %50 = bitcast i8* %arraydecay39 to i32*, !dbg !150
  %arrayidx40 = getelementptr inbounds i32* %50, i32 0, !dbg !150
  %51 = load i32* %arrayidx40, !dbg !150
  %52 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !150
  %mask41 = getelementptr inbounds %struct.squareCtsContext.3* %52, i32 0, i32 2, !dbg !150
  %arraydecay42 = getelementptr inbounds [16 x i8]* %mask41, i32 0, i32 0, !dbg !150
  %53 = bitcast i8* %arraydecay42 to i32*, !dbg !150
  %arrayidx43 = getelementptr inbounds i32* %53, i32 0, !dbg !150
  store i32 %51, i32* %arrayidx43, !dbg !150
  %arraydecay44 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !152
  %54 = bitcast i8* %arraydecay44 to i32*, !dbg !152
  %arrayidx45 = getelementptr inbounds i32* %54, i32 1, !dbg !152
  %55 = load i32* %arrayidx45, !dbg !152
  %56 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !152
  %mask46 = getelementptr inbounds %struct.squareCtsContext.3* %56, i32 0, i32 2, !dbg !152
  %arraydecay47 = getelementptr inbounds [16 x i8]* %mask46, i32 0, i32 0, !dbg !152
  %57 = bitcast i8* %arraydecay47 to i32*, !dbg !152
  %arrayidx48 = getelementptr inbounds i32* %57, i32 1, !dbg !152
  store i32 %55, i32* %arrayidx48, !dbg !152
  %arraydecay49 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !153
  %58 = bitcast i8* %arraydecay49 to i32*, !dbg !153
  %arrayidx50 = getelementptr inbounds i32* %58, i32 2, !dbg !153
  %59 = load i32* %arrayidx50, !dbg !153
  %60 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !153
  %mask51 = getelementptr inbounds %struct.squareCtsContext.3* %60, i32 0, i32 2, !dbg !153
  %arraydecay52 = getelementptr inbounds [16 x i8]* %mask51, i32 0, i32 0, !dbg !153
  %61 = bitcast i8* %arraydecay52 to i32*, !dbg !153
  %arrayidx53 = getelementptr inbounds i32* %61, i32 2, !dbg !153
  store i32 %59, i32* %arrayidx53, !dbg !153
  %arraydecay54 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !154
  %62 = bitcast i8* %arraydecay54 to i32*, !dbg !154
  %arrayidx55 = getelementptr inbounds i32* %62, i32 3, !dbg !154
  %63 = load i32* %arrayidx55, !dbg !154
  %64 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !154
  %mask56 = getelementptr inbounds %struct.squareCtsContext.3* %64, i32 0, i32 2, !dbg !154
  %arraydecay57 = getelementptr inbounds [16 x i8]* %mask56, i32 0, i32 0, !dbg !154
  %65 = bitcast i8* %arraydecay57 to i32*, !dbg !154
  %arrayidx58 = getelementptr inbounds i32* %65, i32 3, !dbg !154
  store i32 %63, i32* %arrayidx58, !dbg !154
  %66 = load i8** %buffer.addr, align 4, !dbg !155
  %add.ptr = getelementptr inbounds i8* %66, i32 16, !dbg !155
  store i8* %add.ptr, i8** %buffer.addr, align 4, !dbg !155
  %67 = load i32* %length.addr, align 4, !dbg !156
  %sub = sub i32 %67, 16, !dbg !156
  store i32 %sub, i32* %length.addr, align 4, !dbg !156
  br label %while.cond, !dbg !157

while.end:                                        ; preds = %while.cond
  %68 = load i8** %buffer.addr, align 4, !dbg !158
  %69 = bitcast i8* %68 to i32*, !dbg !158
  %arrayidx59 = getelementptr inbounds i32* %69, i32 0, !dbg !158
  %70 = load i32* %arrayidx59, !dbg !158
  %arraydecay60 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !158
  %71 = bitcast i8* %arraydecay60 to i32*, !dbg !158
  %arrayidx61 = getelementptr inbounds i32* %71, i32 0, !dbg !158
  store i32 %70, i32* %arrayidx61, !dbg !158
  %72 = load i8** %buffer.addr, align 4, !dbg !160
  %73 = bitcast i8* %72 to i32*, !dbg !160
  %arrayidx62 = getelementptr inbounds i32* %73, i32 1, !dbg !160
  %74 = load i32* %arrayidx62, !dbg !160
  %arraydecay63 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !160
  %75 = bitcast i8* %arraydecay63 to i32*, !dbg !160
  %arrayidx64 = getelementptr inbounds i32* %75, i32 1, !dbg !160
  store i32 %74, i32* %arrayidx64, !dbg !160
  %76 = load i8** %buffer.addr, align 4, !dbg !161
  %77 = bitcast i8* %76 to i32*, !dbg !161
  %arrayidx65 = getelementptr inbounds i32* %77, i32 2, !dbg !161
  %78 = load i32* %arrayidx65, !dbg !161
  %arraydecay66 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !161
  %79 = bitcast i8* %arraydecay66 to i32*, !dbg !161
  %arrayidx67 = getelementptr inbounds i32* %79, i32 2, !dbg !161
  store i32 %78, i32* %arrayidx67, !dbg !161
  %80 = load i8** %buffer.addr, align 4, !dbg !162
  %81 = bitcast i8* %80 to i32*, !dbg !162
  %arrayidx68 = getelementptr inbounds i32* %81, i32 3, !dbg !162
  %82 = load i32* %arrayidx68, !dbg !162
  %arraydecay69 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !162
  %83 = bitcast i8* %arraydecay69 to i32*, !dbg !162
  %arrayidx70 = getelementptr inbounds i32* %83, i32 3, !dbg !162
  store i32 %82, i32* %arrayidx70, !dbg !162
  %84 = load i32* %length.addr, align 4, !dbg !163
  %cmp71 = icmp ugt i32 %84, 16, !dbg !163
  br i1 %cmp71, label %if.then, label %if.else, !dbg !163

if.then:                                          ; preds = %while.end
  %85 = load i8** %buffer.addr, align 4, !dbg !164
  %86 = bitcast i8* %85 to i32*, !dbg !164
  %87 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !164
  %roundKeys_d72 = getelementptr inbounds %struct.squareCtsContext.3* %87, i32 0, i32 1, !dbg !164
  %arraydecay73 = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_d72, i32 0, i32 0, !dbg !164
  call arm_aapcscc  void @squareDecrypt(i32* %86, [4 x i32]* %arraydecay73), !dbg !164
  store i32 0, i32* %i, align 4, !dbg !166
  br label %for.cond, !dbg !166

for.cond:                                         ; preds = %for.inc, %if.then
  %88 = load i32* %i, align 4, !dbg !166
  %89 = load i32* %length.addr, align 4, !dbg !166
  %sub74 = sub i32 %89, 16, !dbg !166
  %cmp75 = icmp ult i32 %88, %sub74, !dbg !166
  br i1 %cmp75, label %for.body, label %for.end, !dbg !166

for.body:                                         ; preds = %for.cond
  %90 = load i32* %i, align 4, !dbg !168
  %91 = load i8** %buffer.addr, align 4, !dbg !168
  %arrayidx76 = getelementptr inbounds i8* %91, i32 %90, !dbg !168
  %92 = load i8* %arrayidx76, !dbg !168
  %conv = zext i8 %92 to i32, !dbg !168
  %93 = load i32* %i, align 4, !dbg !168
  %add = add i32 %93, 16, !dbg !168
  %94 = load i8** %buffer.addr, align 4, !dbg !168
  %arrayidx77 = getelementptr inbounds i8* %94, i32 %add, !dbg !168
  %95 = load i8* %arrayidx77, !dbg !168
  %conv78 = zext i8 %95 to i32, !dbg !168
  %xor79 = xor i32 %conv78, %conv, !dbg !168
  %conv80 = trunc i32 %xor79 to i8, !dbg !168
  store i8 %conv80, i8* %arrayidx77, !dbg !168
  %conv81 = zext i8 %conv80 to i32, !dbg !168
  %96 = load i32* %i, align 4, !dbg !168
  %97 = load i8** %buffer.addr, align 4, !dbg !168
  %arrayidx82 = getelementptr inbounds i8* %97, i32 %96, !dbg !168
  %98 = load i8* %arrayidx82, !dbg !168
  %conv83 = zext i8 %98 to i32, !dbg !168
  %xor84 = xor i32 %conv83, %conv81, !dbg !168
  %conv85 = trunc i32 %xor84 to i8, !dbg !168
  store i8 %conv85, i8* %arrayidx82, !dbg !168
  br label %for.inc, !dbg !170

for.inc:                                          ; preds = %for.body
  %99 = load i32* %i, align 4, !dbg !171
  %inc = add i32 %99, 1, !dbg !171
  store i32 %inc, i32* %i, align 4, !dbg !171
  br label %for.cond, !dbg !171

for.end:                                          ; preds = %for.cond
  %100 = load i8** %buffer.addr, align 4, !dbg !172
  %101 = bitcast i8* %100 to i32*, !dbg !172
  %102 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !172
  %roundKeys_d86 = getelementptr inbounds %struct.squareCtsContext.3* %102, i32 0, i32 1, !dbg !172
  %arraydecay87 = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_d86, i32 0, i32 0, !dbg !172
  call arm_aapcscc  void @squareDecrypt(i32* %101, [4 x i32]* %arraydecay87), !dbg !172
  br label %if.end, !dbg !173

if.else:                                          ; preds = %while.end
  %103 = load i8** %buffer.addr, align 4, !dbg !174
  %104 = bitcast i8* %103 to i32*, !dbg !174
  %105 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !174
  %roundKeys_d88 = getelementptr inbounds %struct.squareCtsContext.3* %105, i32 0, i32 1, !dbg !174
  %arraydecay89 = getelementptr inbounds [9 x [4 x i32]]* %roundKeys_d88, i32 0, i32 0, !dbg !174
  call arm_aapcscc  void @squareDecrypt(i32* %104, [4 x i32]* %arraydecay89), !dbg !174
  br label %if.end

if.end:                                           ; preds = %if.else, %for.end
  %106 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !176
  %mask90 = getelementptr inbounds %struct.squareCtsContext.3* %106, i32 0, i32 2, !dbg !176
  %arraydecay91 = getelementptr inbounds [16 x i8]* %mask90, i32 0, i32 0, !dbg !176
  %107 = bitcast i8* %arraydecay91 to i32*, !dbg !176
  %arrayidx92 = getelementptr inbounds i32* %107, i32 0, !dbg !176
  %108 = load i32* %arrayidx92, !dbg !176
  %109 = load i8** %buffer.addr, align 4, !dbg !176
  %110 = bitcast i8* %109 to i32*, !dbg !176
  %arrayidx93 = getelementptr inbounds i32* %110, i32 0, !dbg !176
  %111 = load i32* %arrayidx93, !dbg !176
  %xor94 = xor i32 %111, %108, !dbg !176
  store i32 %xor94, i32* %arrayidx93, !dbg !176
  %112 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !177
  %mask95 = getelementptr inbounds %struct.squareCtsContext.3* %112, i32 0, i32 2, !dbg !177
  %arraydecay96 = getelementptr inbounds [16 x i8]* %mask95, i32 0, i32 0, !dbg !177
  %113 = bitcast i8* %arraydecay96 to i32*, !dbg !177
  %arrayidx97 = getelementptr inbounds i32* %113, i32 1, !dbg !177
  %114 = load i32* %arrayidx97, !dbg !177
  %115 = load i8** %buffer.addr, align 4, !dbg !177
  %116 = bitcast i8* %115 to i32*, !dbg !177
  %arrayidx98 = getelementptr inbounds i32* %116, i32 1, !dbg !177
  %117 = load i32* %arrayidx98, !dbg !177
  %xor99 = xor i32 %117, %114, !dbg !177
  store i32 %xor99, i32* %arrayidx98, !dbg !177
  %118 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !178
  %mask100 = getelementptr inbounds %struct.squareCtsContext.3* %118, i32 0, i32 2, !dbg !178
  %arraydecay101 = getelementptr inbounds [16 x i8]* %mask100, i32 0, i32 0, !dbg !178
  %119 = bitcast i8* %arraydecay101 to i32*, !dbg !178
  %arrayidx102 = getelementptr inbounds i32* %119, i32 2, !dbg !178
  %120 = load i32* %arrayidx102, !dbg !178
  %121 = load i8** %buffer.addr, align 4, !dbg !178
  %122 = bitcast i8* %121 to i32*, !dbg !178
  %arrayidx103 = getelementptr inbounds i32* %122, i32 2, !dbg !178
  %123 = load i32* %arrayidx103, !dbg !178
  %xor104 = xor i32 %123, %120, !dbg !178
  store i32 %xor104, i32* %arrayidx103, !dbg !178
  %124 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !179
  %mask105 = getelementptr inbounds %struct.squareCtsContext.3* %124, i32 0, i32 2, !dbg !179
  %arraydecay106 = getelementptr inbounds [16 x i8]* %mask105, i32 0, i32 0, !dbg !179
  %125 = bitcast i8* %arraydecay106 to i32*, !dbg !179
  %arrayidx107 = getelementptr inbounds i32* %125, i32 3, !dbg !179
  %126 = load i32* %arrayidx107, !dbg !179
  %127 = load i8** %buffer.addr, align 4, !dbg !179
  %128 = bitcast i8* %127 to i32*, !dbg !179
  %arrayidx108 = getelementptr inbounds i32* %128, i32 3, !dbg !179
  %129 = load i32* %arrayidx108, !dbg !179
  %xor109 = xor i32 %129, %126, !dbg !179
  store i32 %xor109, i32* %arrayidx108, !dbg !179
  %arraydecay110 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !180
  %130 = bitcast i8* %arraydecay110 to i32*, !dbg !180
  %arrayidx111 = getelementptr inbounds i32* %130, i32 0, !dbg !180
  %131 = load i32* %arrayidx111, !dbg !180
  %132 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !180
  %mask112 = getelementptr inbounds %struct.squareCtsContext.3* %132, i32 0, i32 2, !dbg !180
  %arraydecay113 = getelementptr inbounds [16 x i8]* %mask112, i32 0, i32 0, !dbg !180
  %133 = bitcast i8* %arraydecay113 to i32*, !dbg !180
  %arrayidx114 = getelementptr inbounds i32* %133, i32 0, !dbg !180
  store i32 %131, i32* %arrayidx114, !dbg !180
  %arraydecay115 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !182
  %134 = bitcast i8* %arraydecay115 to i32*, !dbg !182
  %arrayidx116 = getelementptr inbounds i32* %134, i32 1, !dbg !182
  %135 = load i32* %arrayidx116, !dbg !182
  %136 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !182
  %mask117 = getelementptr inbounds %struct.squareCtsContext.3* %136, i32 0, i32 2, !dbg !182
  %arraydecay118 = getelementptr inbounds [16 x i8]* %mask117, i32 0, i32 0, !dbg !182
  %137 = bitcast i8* %arraydecay118 to i32*, !dbg !182
  %arrayidx119 = getelementptr inbounds i32* %137, i32 1, !dbg !182
  store i32 %135, i32* %arrayidx119, !dbg !182
  %arraydecay120 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !183
  %138 = bitcast i8* %arraydecay120 to i32*, !dbg !183
  %arrayidx121 = getelementptr inbounds i32* %138, i32 2, !dbg !183
  %139 = load i32* %arrayidx121, !dbg !183
  %140 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !183
  %mask122 = getelementptr inbounds %struct.squareCtsContext.3* %140, i32 0, i32 2, !dbg !183
  %arraydecay123 = getelementptr inbounds [16 x i8]* %mask122, i32 0, i32 0, !dbg !183
  %141 = bitcast i8* %arraydecay123 to i32*, !dbg !183
  %arrayidx124 = getelementptr inbounds i32* %141, i32 2, !dbg !183
  store i32 %139, i32* %arrayidx124, !dbg !183
  %arraydecay125 = getelementptr inbounds [16 x i8]* %temp, i32 0, i32 0, !dbg !184
  %142 = bitcast i8* %arraydecay125 to i32*, !dbg !184
  %arrayidx126 = getelementptr inbounds i32* %142, i32 3, !dbg !184
  %143 = load i32* %arrayidx126, !dbg !184
  %144 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !184
  %mask127 = getelementptr inbounds %struct.squareCtsContext.3* %144, i32 0, i32 2, !dbg !184
  %arraydecay128 = getelementptr inbounds [16 x i8]* %mask127, i32 0, i32 0, !dbg !184
  %145 = bitcast i8* %arraydecay128 to i32*, !dbg !184
  %arrayidx129 = getelementptr inbounds i32* %145, i32 3, !dbg !184
  store i32 %143, i32* %arrayidx129, !dbg !184
  ret void, !dbg !185
}

declare arm_aapcscc void @squareDecrypt(i32*, [4 x i32]*)

define arm_aapcscc void @squareCtsFinal(%struct.squareCtsContext.3* %ctxCts) nounwind uwtable {
entry:
  %ctxCts.addr = alloca %struct.squareCtsContext.3*, align 4
  store %struct.squareCtsContext.3* %ctxCts, %struct.squareCtsContext.3** %ctxCts.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.squareCtsContext.3** %ctxCts.addr}, metadata !186), !dbg !187
  %0 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !188
  %cmp = icmp ne %struct.squareCtsContext.3* %0, null, !dbg !188
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !188

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !188

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([22 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([8 x i8]* @.str1, i32 0, i32 0), i32 142, i8* getelementptr inbounds ([40 x i8]* @__PRETTY_FUNCTION__.squareCtsFinal, i32 0, i32 0)) noreturn nounwind, !dbg !190
  unreachable, !dbg !190
                                                  ; No predecessors!
  br label %cond.end, !dbg !190

cond.end:                                         ; preds = %1, %cond.true
  %2 = load %struct.squareCtsContext.3** %ctxCts.addr, align 4, !dbg !191
  %3 = bitcast %struct.squareCtsContext.3* %2 to i8*, !dbg !191
  call void @llvm.memset.p0i8.i32(i8* %3, i8 0, i32 304, i32 1, i1 false), !dbg !191
  ret void, !dbg !192
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !11, metadata !14, metadata !17, metadata !20}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"squareCtsInit", metadata !"squareCtsInit", metadata !"", metadata !6, i32 24, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.squareCtsContext.3*, i8*)* @squareCtsInit, null, null, metadata !9} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"sqcts.c", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{null}
!9 = metadata !{metadata !10}
!10 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!11 = metadata !{i32 720942, i32 0, metadata !6, metadata !"squareCtsSetIV", metadata !"squareCtsSetIV", metadata !"", metadata !6, i32 33, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.squareCtsContext.3*, i8*)* @squareCtsSetIV, null, null, metadata !12} ; [ DW_TAG_subprogram ]
!12 = metadata !{metadata !13}
!13 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!14 = metadata !{i32 720942, i32 0, metadata !6, metadata !"squareCtsEncrypt", metadata !"squareCtsEncrypt", metadata !"", metadata !6, i32 44, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.squareCtsContext.3*, i8*, i32)* @squareCtsEncrypt, null, null, metadata !15} ; [ DW_TAG_subprogram ]
!15 = metadata !{metadata !16}
!16 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!17 = metadata !{i32 720942, i32 0, metadata !6, metadata !"squareCtsDecrypt", metadata !"squareCtsDecrypt", metadata !"", metadata !6, i32 82, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.squareCtsContext.3*, i8*, i32)* @squareCtsDecrypt, null, null, metadata !18} ; [ DW_TAG_subprogram ]
!18 = metadata !{metadata !19}
!19 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!20 = metadata !{i32 720942, i32 0, metadata !6, metadata !"squareCtsFinal", metadata !"squareCtsFinal", metadata !"", metadata !6, i32 141, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.squareCtsContext.3*)* @squareCtsFinal, null, null, metadata !21} ; [ DW_TAG_subprogram ]
!21 = metadata !{metadata !22}
!22 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!23 = metadata !{i32 721153, metadata !5, metadata !"ctxCts", metadata !6, i32 16777239, metadata !24, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!24 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !25} ; [ DW_TAG_pointer_type ]
!25 = metadata !{i32 720918, null, metadata !"squareCtsContext", metadata !6, i32 21, i64 0, i64 0, i64 0, i32 0, metadata !26} ; [ DW_TAG_typedef ]
!26 = metadata !{i32 720915, null, metadata !"", metadata !27, i32 18, i64 2432, i64 32, i32 0, i32 0, null, metadata !28, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!27 = metadata !{i32 720937, metadata !"sqcts.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!28 = metadata !{metadata !29, metadata !37, metadata !38}
!29 = metadata !{i32 720909, metadata !26, metadata !"roundKeys_e", metadata !27, i32 19, i64 1152, i64 32, i64 0, i32 0, metadata !30} ; [ DW_TAG_member ]
!30 = metadata !{i32 720918, null, metadata !"squareKeySchedule", metadata !27, i32 21, i64 0, i64 0, i64 0, i32 0, metadata !31} ; [ DW_TAG_typedef ]
!31 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 1152, i64 32, i32 0, i32 0, metadata !32, metadata !34, i32 0, i32 0} ; [ DW_TAG_array_type ]
!32 = metadata !{i32 720918, null, metadata !"word32", metadata !27, i32 14, i64 0, i64 0, i64 0, i32 0, metadata !33} ; [ DW_TAG_typedef ]
!33 = metadata !{i32 720932, null, metadata !"long unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!34 = metadata !{metadata !35, metadata !36}
!35 = metadata !{i32 720929, i64 0, i64 8}        ; [ DW_TAG_subrange_type ]
!36 = metadata !{i32 720929, i64 0, i64 3}        ; [ DW_TAG_subrange_type ]
!37 = metadata !{i32 720909, metadata !26, metadata !"roundKeys_d", metadata !27, i32 19, i64 1152, i64 32, i64 1152, i32 0, metadata !30} ; [ DW_TAG_member ]
!38 = metadata !{i32 720909, metadata !26, metadata !"mask", metadata !27, i32 20, i64 128, i64 8, i64 2304, i32 0, metadata !39} ; [ DW_TAG_member ]
!39 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 128, i64 8, i32 0, i32 0, metadata !40, metadata !42, i32 0, i32 0} ; [ DW_TAG_array_type ]
!40 = metadata !{i32 720918, null, metadata !"byte", metadata !27, i32 9, i64 0, i64 0, i64 0, i32 0, metadata !41} ; [ DW_TAG_typedef ]
!41 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!42 = metadata !{metadata !43}
!43 = metadata !{i32 720929, i64 0, i64 15}       ; [ DW_TAG_subrange_type ]
!44 = metadata !{i32 23, i32 39, metadata !5, null}
!45 = metadata !{i32 721153, metadata !5, metadata !"key", metadata !6, i32 33554455, metadata !46, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!46 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !47} ; [ DW_TAG_pointer_type ]
!47 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !40} ; [ DW_TAG_const_type ]
!48 = metadata !{i32 23, i32 65, metadata !5, null}
!49 = metadata !{i32 25, i32 2, metadata !50, null}
!50 = metadata !{i32 720907, metadata !5, i32 24, i32 1, metadata !6, i32 337} ; [ DW_TAG_lexical_block ]
!51 = metadata !{i32 25, i32 42, metadata !50, null}
!52 = metadata !{i32 26, i32 2, metadata !50, null}
!53 = metadata !{i32 26, i32 39, metadata !50, null}
!54 = metadata !{i32 27, i32 2, metadata !50, null}
!55 = metadata !{i32 28, i32 2, metadata !50, null}
!56 = metadata !{i32 29, i32 1, metadata !50, null}
!57 = metadata !{i32 721153, metadata !11, metadata !"ctxCts", metadata !6, i32 16777248, metadata !24, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!58 = metadata !{i32 32, i32 40, metadata !11, null}
!59 = metadata !{i32 721153, metadata !11, metadata !"iv", metadata !6, i32 33554464, metadata !46, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!60 = metadata !{i32 32, i32 66, metadata !11, null}
!61 = metadata !{i32 34, i32 2, metadata !62, null}
!62 = metadata !{i32 720907, metadata !11, i32 33, i32 1, metadata !6, i32 338} ; [ DW_TAG_lexical_block ]
!63 = metadata !{i32 34, i32 42, metadata !62, null}
!64 = metadata !{i32 35, i32 2, metadata !62, null}
!65 = metadata !{i32 36, i32 3, metadata !66, null}
!66 = metadata !{i32 720907, metadata !62, i32 35, i32 25, metadata !6, i32 339} ; [ DW_TAG_lexical_block ]
!67 = metadata !{i32 37, i32 2, metadata !66, null}
!68 = metadata !{i32 39, i32 2, metadata !62, null}
!69 = metadata !{i32 40, i32 1, metadata !62, null}
!70 = metadata !{i32 721153, metadata !14, metadata !"ctxCts", metadata !6, i32 16777259, metadata !24, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!71 = metadata !{i32 43, i32 42, metadata !14, null}
!72 = metadata !{i32 721153, metadata !14, metadata !"buffer", metadata !6, i32 33554475, metadata !73, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!73 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !40} ; [ DW_TAG_pointer_type ]
!74 = metadata !{i32 43, i32 56, metadata !14, null}
!75 = metadata !{i32 721153, metadata !14, metadata !"length", metadata !6, i32 50331691, metadata !76, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!76 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!77 = metadata !{i32 43, i32 73, metadata !14, null}
!78 = metadata !{i32 721152, metadata !79, metadata !"mask", metadata !6, i32 45, metadata !73, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!79 = metadata !{i32 720907, metadata !14, i32 44, i32 1, metadata !6, i32 340} ; [ DW_TAG_lexical_block ]
!80 = metadata !{i32 45, i32 8, metadata !79, null}
!81 = metadata !{i32 721152, metadata !79, metadata !"i", metadata !6, i32 46, metadata !76, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!82 = metadata !{i32 46, i32 11, metadata !79, null}
!83 = metadata !{i32 48, i32 2, metadata !79, null}
!84 = metadata !{i32 48, i32 42, metadata !79, null}
!85 = metadata !{i32 49, i32 2, metadata !79, null}
!86 = metadata !{i32 49, i32 42, metadata !79, null}
!87 = metadata !{i32 50, i32 2, metadata !79, null}
!88 = metadata !{i32 50, i32 49, metadata !79, null}
!89 = metadata !{i32 51, i32 2, metadata !79, null}
!90 = metadata !{i32 52, i32 2, metadata !79, null}
!91 = metadata !{i32 54, i32 3, metadata !92, null}
!92 = metadata !{i32 720907, metadata !79, i32 52, i32 39, metadata !6, i32 341} ; [ DW_TAG_lexical_block ]
!93 = metadata !{i32 55, i32 3, metadata !92, null}
!94 = metadata !{i32 56, i32 3, metadata !92, null}
!95 = metadata !{i32 57, i32 3, metadata !92, null}
!96 = metadata !{i32 58, i32 3, metadata !92, null}
!97 = metadata !{i32 60, i32 3, metadata !92, null}
!98 = metadata !{i32 62, i32 3, metadata !92, null}
!99 = metadata !{i32 63, i32 3, metadata !92, null}
!100 = metadata !{i32 64, i32 2, metadata !92, null}
!101 = metadata !{i32 66, i32 4, metadata !102, null}
!102 = metadata !{i32 720907, metadata !79, i32 66, i32 2, metadata !6, i32 342} ; [ DW_TAG_lexical_block ]
!103 = metadata !{i32 66, i32 63, metadata !102, null}
!104 = metadata !{i32 66, i32 122, metadata !102, null}
!105 = metadata !{i32 66, i32 181, metadata !102, null}
!106 = metadata !{i32 67, i32 2, metadata !79, null}
!107 = metadata !{i32 69, i32 8, metadata !108, null}
!108 = metadata !{i32 720907, metadata !109, i32 69, i32 3, metadata !6, i32 344} ; [ DW_TAG_lexical_block ]
!109 = metadata !{i32 720907, metadata !79, i32 67, i32 19, metadata !6, i32 343} ; [ DW_TAG_lexical_block ]
!110 = metadata !{i32 70, i32 4, metadata !111, null}
!111 = metadata !{i32 720907, metadata !108, i32 69, i32 32, metadata !6, i32 345} ; [ DW_TAG_lexical_block ]
!112 = metadata !{i32 71, i32 3, metadata !111, null}
!113 = metadata !{i32 69, i32 27, metadata !108, null}
!114 = metadata !{i32 72, i32 3, metadata !109, null}
!115 = metadata !{i32 73, i32 3, metadata !109, null}
!116 = metadata !{i32 74, i32 3, metadata !109, null}
!117 = metadata !{i32 76, i32 2, metadata !109, null}
!118 = metadata !{i32 77, i32 2, metadata !79, null}
!119 = metadata !{i32 78, i32 1, metadata !79, null}
!120 = metadata !{i32 721153, metadata !17, metadata !"ctxCts", metadata !6, i32 16777297, metadata !24, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!121 = metadata !{i32 81, i32 42, metadata !17, null}
!122 = metadata !{i32 721153, metadata !17, metadata !"buffer", metadata !6, i32 33554513, metadata !73, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!123 = metadata !{i32 81, i32 56, metadata !17, null}
!124 = metadata !{i32 721153, metadata !17, metadata !"length", metadata !6, i32 50331729, metadata !76, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!125 = metadata !{i32 81, i32 73, metadata !17, null}
!126 = metadata !{i32 721152, metadata !127, metadata !"i", metadata !6, i32 83, metadata !76, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!127 = metadata !{i32 720907, metadata !17, i32 82, i32 1, metadata !6, i32 346} ; [ DW_TAG_lexical_block ]
!128 = metadata !{i32 83, i32 11, metadata !127, null}
!129 = metadata !{i32 721152, metadata !127, metadata !"temp", metadata !6, i32 84, metadata !130, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!130 = metadata !{i32 720918, null, metadata !"squareBlock", metadata !6, i32 20, i64 0, i64 0, i64 0, i32 0, metadata !39} ; [ DW_TAG_typedef ]
!131 = metadata !{i32 84, i32 14, metadata !127, null}
!132 = metadata !{i32 86, i32 2, metadata !127, null}
!133 = metadata !{i32 86, i32 42, metadata !127, null}
!134 = metadata !{i32 87, i32 2, metadata !127, null}
!135 = metadata !{i32 87, i32 42, metadata !127, null}
!136 = metadata !{i32 88, i32 2, metadata !127, null}
!137 = metadata !{i32 88, i32 49, metadata !127, null}
!138 = metadata !{i32 89, i32 2, metadata !127, null}
!139 = metadata !{i32 91, i32 5, metadata !140, null}
!140 = metadata !{i32 720907, metadata !141, i32 91, i32 3, metadata !6, i32 348} ; [ DW_TAG_lexical_block ]
!141 = metadata !{i32 720907, metadata !127, i32 89, i32 41, metadata !6, i32 347} ; [ DW_TAG_lexical_block ]
!142 = metadata !{i32 91, i32 58, metadata !140, null}
!143 = metadata !{i32 91, i32 111, metadata !140, null}
!144 = metadata !{i32 91, i32 164, metadata !140, null}
!145 = metadata !{i32 93, i32 3, metadata !141, null}
!146 = metadata !{i32 94, i32 3, metadata !141, null}
!147 = metadata !{i32 95, i32 3, metadata !141, null}
!148 = metadata !{i32 96, i32 3, metadata !141, null}
!149 = metadata !{i32 97, i32 3, metadata !141, null}
!150 = metadata !{i32 99, i32 5, metadata !151, null}
!151 = metadata !{i32 720907, metadata !141, i32 99, i32 3, metadata !6, i32 349} ; [ DW_TAG_lexical_block ]
!152 = metadata !{i32 99, i32 64, metadata !151, null}
!153 = metadata !{i32 99, i32 123, metadata !151, null}
!154 = metadata !{i32 99, i32 182, metadata !151, null}
!155 = metadata !{i32 101, i32 3, metadata !141, null}
!156 = metadata !{i32 102, i32 3, metadata !141, null}
!157 = metadata !{i32 103, i32 2, metadata !141, null}
!158 = metadata !{i32 106, i32 4, metadata !159, null}
!159 = metadata !{i32 720907, metadata !127, i32 106, i32 2, metadata !6, i32 350} ; [ DW_TAG_lexical_block ]
!160 = metadata !{i32 106, i32 57, metadata !159, null}
!161 = metadata !{i32 106, i32 110, metadata !159, null}
!162 = metadata !{i32 106, i32 163, metadata !159, null}
!163 = metadata !{i32 107, i32 2, metadata !127, null}
!164 = metadata !{i32 109, i32 3, metadata !165, null}
!165 = metadata !{i32 720907, metadata !127, i32 107, i32 35, metadata !6, i32 351} ; [ DW_TAG_lexical_block ]
!166 = metadata !{i32 110, i32 8, metadata !167, null}
!167 = metadata !{i32 720907, metadata !165, i32 110, i32 3, metadata !6, i32 352} ; [ DW_TAG_lexical_block ]
!168 = metadata !{i32 114, i32 4, metadata !169, null}
!169 = metadata !{i32 720907, metadata !167, i32 110, i32 53, metadata !6, i32 353} ; [ DW_TAG_lexical_block ]
!170 = metadata !{i32 117, i32 3, metadata !169, null}
!171 = metadata !{i32 110, i32 48, metadata !167, null}
!172 = metadata !{i32 119, i32 3, metadata !165, null}
!173 = metadata !{i32 120, i32 2, metadata !165, null}
!174 = metadata !{i32 122, i32 3, metadata !175, null}
!175 = metadata !{i32 720907, metadata !127, i32 120, i32 9, metadata !6, i32 354} ; [ DW_TAG_lexical_block ]
!176 = metadata !{i32 124, i32 2, metadata !127, null}
!177 = metadata !{i32 125, i32 2, metadata !127, null}
!178 = metadata !{i32 126, i32 2, metadata !127, null}
!179 = metadata !{i32 127, i32 2, metadata !127, null}
!180 = metadata !{i32 129, i32 4, metadata !181, null}
!181 = metadata !{i32 720907, metadata !127, i32 129, i32 2, metadata !6, i32 355} ; [ DW_TAG_lexical_block ]
!182 = metadata !{i32 129, i32 63, metadata !181, null}
!183 = metadata !{i32 129, i32 122, metadata !181, null}
!184 = metadata !{i32 129, i32 181, metadata !181, null}
!185 = metadata !{i32 137, i32 1, metadata !127, null}
!186 = metadata !{i32 721153, metadata !20, metadata !"ctxCts", metadata !6, i32 16777356, metadata !24, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!187 = metadata !{i32 140, i32 40, metadata !20, null}
!188 = metadata !{i32 142, i32 2, metadata !189, null}
!189 = metadata !{i32 720907, metadata !20, i32 141, i32 1, metadata !6, i32 356} ; [ DW_TAG_lexical_block ]
!190 = metadata !{i32 142, i32 42, metadata !189, null}
!191 = metadata !{i32 143, i32 2, metadata !189, null}
!192 = metadata !{i32 144, i32 1, metadata !189, null}
