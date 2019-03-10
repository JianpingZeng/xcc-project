; ModuleID = 'ec_curve.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct.ecPoint.1 = type { [36 x i16], [36 x i16] }

define arm_aapcscc void @ecCopy(%struct.ecPoint.1* %p, %struct.ecPoint.1* %q) nounwind uwtable {
entry:
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %q.addr = alloca %struct.ecPoint.1*, align 4
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !38), !dbg !53
  store %struct.ecPoint.1* %q, %struct.ecPoint.1** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %q.addr}, metadata !54), !dbg !57
  %0 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !58
  %x = getelementptr inbounds %struct.ecPoint.1* %0, i32 0, i32 0, !dbg !58
  %arraydecay = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !58
  %1 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !58
  %x1 = getelementptr inbounds %struct.ecPoint.1* %1, i32 0, i32 0, !dbg !58
  %arraydecay2 = getelementptr inbounds [36 x i16]* %x1, i32 0, i32 0, !dbg !58
  call arm_aapcscc  void @gfCopy(i16* %arraydecay, i16* %arraydecay2), !dbg !58
  %2 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !60
  %y = getelementptr inbounds %struct.ecPoint.1* %2, i32 0, i32 1, !dbg !60
  %arraydecay3 = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !60
  %3 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !60
  %y4 = getelementptr inbounds %struct.ecPoint.1* %3, i32 0, i32 1, !dbg !60
  %arraydecay5 = getelementptr inbounds [36 x i16]* %y4, i32 0, i32 0, !dbg !60
  call arm_aapcscc  void @gfCopy(i16* %arraydecay3, i16* %arraydecay5), !dbg !60
  ret void, !dbg !61
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc void @gfCopy(i16*, i16*)

define arm_aapcscc i32 @ecCalcY(%struct.ecPoint.1* %p, i32 %ybit) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %ybit.addr = alloca i32, align 4
  %a = alloca [36 x i16], align 2
  %b = alloca [36 x i16], align 2
  %t = alloca [36 x i16], align 2
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !62), !dbg !63
  store i32 %ybit, i32* %ybit.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %ybit.addr}, metadata !64), !dbg !65
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %a}, metadata !66), !dbg !68
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %b}, metadata !69), !dbg !70
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %t}, metadata !71), !dbg !72
  %arrayidx = getelementptr inbounds [36 x i16]* %b, i32 0, i32 0, !dbg !73
  store i16 1, i16* %arrayidx, align 2, !dbg !73
  %arrayidx1 = getelementptr inbounds [36 x i16]* %b, i32 0, i32 1, !dbg !74
  store i16 161, i16* %arrayidx1, align 2, !dbg !74
  %0 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !75
  %x = getelementptr inbounds %struct.ecPoint.1* %0, i32 0, i32 0, !dbg !75
  %arrayidx2 = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !75
  %1 = load i16* %arrayidx2, align 2, !dbg !75
  %conv = zext i16 %1 to i32, !dbg !75
  %cmp = icmp eq i32 %conv, 0, !dbg !75
  br i1 %cmp, label %if.then, label %if.end, !dbg !75

if.then:                                          ; preds = %entry
  %2 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !76
  %y = getelementptr inbounds %struct.ecPoint.1* %2, i32 0, i32 1, !dbg !76
  %arraydecay = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !76
  call arm_aapcscc  void @gfSquareRoot(i16* %arraydecay, i16 zeroext 161), !dbg !76
  store i32 1, i32* %retval, !dbg !78
  br label %return, !dbg !78

if.end:                                           ; preds = %entry
  %arraydecay4 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !79
  %3 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !79
  %x5 = getelementptr inbounds %struct.ecPoint.1* %3, i32 0, i32 0, !dbg !79
  %arraydecay6 = getelementptr inbounds [36 x i16]* %x5, i32 0, i32 0, !dbg !79
  call arm_aapcscc  void @gfSquare(i16* %arraydecay4, i16* %arraydecay6), !dbg !79
  %arraydecay7 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !80
  %arraydecay8 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !80
  %4 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !80
  %x9 = getelementptr inbounds %struct.ecPoint.1* %4, i32 0, i32 0, !dbg !80
  %arraydecay10 = getelementptr inbounds [36 x i16]* %x9, i32 0, i32 0, !dbg !80
  call arm_aapcscc  void @gfMultiply(i16* %arraydecay7, i16* %arraydecay8, i16* %arraydecay10), !dbg !80
  %arraydecay11 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !81
  %arraydecay12 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !81
  %arraydecay13 = getelementptr inbounds [36 x i16]* %b, i32 0, i32 0, !dbg !81
  call arm_aapcscc  void @gfAdd(i16* %arraydecay11, i16* %arraydecay12, i16* %arraydecay13), !dbg !81
  %arrayidx14 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !82
  %5 = load i16* %arrayidx14, align 2, !dbg !82
  %conv15 = zext i16 %5 to i32, !dbg !82
  %cmp16 = icmp eq i32 %conv15, 0, !dbg !82
  br i1 %cmp16, label %if.then18, label %if.end23, !dbg !82

if.then18:                                        ; preds = %if.end
  %6 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !83
  %y19 = getelementptr inbounds %struct.ecPoint.1* %6, i32 0, i32 1, !dbg !83
  %arrayidx20 = getelementptr inbounds [36 x i16]* %y19, i32 0, i32 0, !dbg !83
  store i16 0, i16* %arrayidx20, align 2, !dbg !83
  %arraydecay21 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !85
  call arm_aapcscc  void @gfClear(i16* %arraydecay21), !dbg !85
  %arraydecay22 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !86
  call arm_aapcscc  void @gfClear(i16* %arraydecay22), !dbg !86
  store i32 1, i32* %retval, !dbg !87
  br label %return, !dbg !87

if.end23:                                         ; preds = %if.end
  %arraydecay24 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !88
  call arm_aapcscc  void @gfSmallDiv(i16* %arraydecay24, i16 zeroext 161), !dbg !88
  %arraydecay25 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !89
  %arraydecay26 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !89
  %call = call arm_aapcscc  i32 @gfInvert(i16* %arraydecay25, i16* %arraydecay26), !dbg !89
  %arraydecay27 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !90
  %7 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !90
  %x28 = getelementptr inbounds %struct.ecPoint.1* %7, i32 0, i32 0, !dbg !90
  %arraydecay29 = getelementptr inbounds [36 x i16]* %x28, i32 0, i32 0, !dbg !90
  %arraydecay30 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !90
  call arm_aapcscc  void @gfAdd(i16* %arraydecay27, i16* %arraydecay29, i16* %arraydecay30), !dbg !90
  %arraydecay31 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !91
  %call32 = call arm_aapcscc  i32 @gfTrace(i16* %arraydecay31), !dbg !91
  %cmp33 = icmp ne i32 %call32, 0, !dbg !91
  br i1 %cmp33, label %if.then35, label %if.end38, !dbg !91

if.then35:                                        ; preds = %if.end23
  %arraydecay36 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !92
  call arm_aapcscc  void @gfClear(i16* %arraydecay36), !dbg !92
  %arraydecay37 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !94
  call arm_aapcscc  void @gfClear(i16* %arraydecay37), !dbg !94
  store i32 0, i32* %retval, !dbg !95
  br label %return, !dbg !95

if.end38:                                         ; preds = %if.end23
  %arraydecay39 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !96
  %arraydecay40 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !96
  %call41 = call arm_aapcscc  i32 @gfQuadSolve(i16* %arraydecay39, i16* %arraydecay40), !dbg !96
  %arraydecay42 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !97
  %call43 = call arm_aapcscc  i32 @gfYbit(i16* %arraydecay42), !dbg !97
  %8 = load i32* %ybit.addr, align 4, !dbg !97
  %cmp44 = icmp ne i32 %call43, %8, !dbg !97
  br i1 %cmp44, label %if.then46, label %if.end50, !dbg !97

if.then46:                                        ; preds = %if.end38
  %arrayidx47 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 1, !dbg !98
  %9 = load i16* %arrayidx47, align 2, !dbg !98
  %conv48 = zext i16 %9 to i32, !dbg !98
  %xor = xor i32 %conv48, 1, !dbg !98
  %conv49 = trunc i32 %xor to i16, !dbg !98
  store i16 %conv49, i16* %arrayidx47, align 2, !dbg !98
  br label %if.end50, !dbg !100

if.end50:                                         ; preds = %if.then46, %if.end38
  %10 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !101
  %y51 = getelementptr inbounds %struct.ecPoint.1* %10, i32 0, i32 1, !dbg !101
  %arraydecay52 = getelementptr inbounds [36 x i16]* %y51, i32 0, i32 0, !dbg !101
  %11 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !101
  %x53 = getelementptr inbounds %struct.ecPoint.1* %11, i32 0, i32 0, !dbg !101
  %arraydecay54 = getelementptr inbounds [36 x i16]* %x53, i32 0, i32 0, !dbg !101
  %arraydecay55 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !101
  call arm_aapcscc  void @gfMultiply(i16* %arraydecay52, i16* %arraydecay54, i16* %arraydecay55), !dbg !101
  %arraydecay56 = getelementptr inbounds [36 x i16]* %a, i32 0, i32 0, !dbg !102
  call arm_aapcscc  void @gfClear(i16* %arraydecay56), !dbg !102
  %arraydecay57 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !103
  call arm_aapcscc  void @gfClear(i16* %arraydecay57), !dbg !103
  store i32 1, i32* %retval, !dbg !104
  br label %return, !dbg !104

return:                                           ; preds = %if.end50, %if.then35, %if.then18, %if.then
  %12 = load i32* %retval, !dbg !105
  ret i32 %12, !dbg !105
}

declare arm_aapcscc void @gfSquareRoot(i16*, i16 zeroext)

declare arm_aapcscc void @gfSquare(i16*, i16*)

declare arm_aapcscc void @gfMultiply(i16*, i16*, i16*)

declare arm_aapcscc void @gfAdd(i16*, i16*, i16*)

declare arm_aapcscc void @gfClear(i16*)

declare arm_aapcscc void @gfSmallDiv(i16*, i16 zeroext)

declare arm_aapcscc i32 @gfInvert(i16*, i16*)

declare arm_aapcscc i32 @gfTrace(i16*)

declare arm_aapcscc i32 @gfQuadSolve(i16*, i16*)

declare arm_aapcscc i32 @gfYbit(i16*)

define arm_aapcscc void @ecAdd(%struct.ecPoint.1* %p, %struct.ecPoint.1* %q) nounwind uwtable {
entry:
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %q.addr = alloca %struct.ecPoint.1*, align 4
  %lambda = alloca [36 x i16], align 2
  %t = alloca [36 x i16], align 2
  %tx = alloca [36 x i16], align 2
  %ty = alloca [36 x i16], align 2
  %x3 = alloca [36 x i16], align 2
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !106), !dbg !107
  store %struct.ecPoint.1* %q, %struct.ecPoint.1** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %q.addr}, metadata !108), !dbg !109
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %lambda}, metadata !110), !dbg !112
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %t}, metadata !113), !dbg !114
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %tx}, metadata !115), !dbg !116
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %ty}, metadata !117), !dbg !118
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %x3}, metadata !119), !dbg !120
  %0 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !121
  %x = getelementptr inbounds %struct.ecPoint.1* %0, i32 0, i32 0, !dbg !121
  %arrayidx = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !121
  %1 = load i16* %arrayidx, align 2, !dbg !121
  %conv = zext i16 %1 to i32, !dbg !121
  %cmp = icmp ne i32 %conv, 0, !dbg !121
  br i1 %cmp, label %if.then, label %lor.lhs.false, !dbg !121

lor.lhs.false:                                    ; preds = %entry
  %2 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !121
  %y = getelementptr inbounds %struct.ecPoint.1* %2, i32 0, i32 1, !dbg !121
  %arrayidx2 = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !121
  %3 = load i16* %arrayidx2, align 2, !dbg !121
  %conv3 = zext i16 %3 to i32, !dbg !121
  %cmp4 = icmp ne i32 %conv3, 0, !dbg !121
  br i1 %cmp4, label %if.then, label %if.end87, !dbg !121

if.then:                                          ; preds = %lor.lhs.false, %entry
  %4 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !122
  %x6 = getelementptr inbounds %struct.ecPoint.1* %4, i32 0, i32 0, !dbg !122
  %arrayidx7 = getelementptr inbounds [36 x i16]* %x6, i32 0, i32 0, !dbg !122
  %5 = load i16* %arrayidx7, align 2, !dbg !122
  %conv8 = zext i16 %5 to i32, !dbg !122
  %cmp9 = icmp ne i32 %conv8, 0, !dbg !122
  br i1 %cmp9, label %if.then17, label %lor.lhs.false11, !dbg !122

lor.lhs.false11:                                  ; preds = %if.then
  %6 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !122
  %y12 = getelementptr inbounds %struct.ecPoint.1* %6, i32 0, i32 1, !dbg !122
  %arrayidx13 = getelementptr inbounds [36 x i16]* %y12, i32 0, i32 0, !dbg !122
  %7 = load i16* %arrayidx13, align 2, !dbg !122
  %conv14 = zext i16 %7 to i32, !dbg !122
  %cmp15 = icmp ne i32 %conv14, 0, !dbg !122
  br i1 %cmp15, label %if.then17, label %if.else77, !dbg !122

if.then17:                                        ; preds = %lor.lhs.false11, %if.then
  %8 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !124
  %x18 = getelementptr inbounds %struct.ecPoint.1* %8, i32 0, i32 0, !dbg !124
  %arraydecay = getelementptr inbounds [36 x i16]* %x18, i32 0, i32 0, !dbg !124
  %9 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !124
  %x19 = getelementptr inbounds %struct.ecPoint.1* %9, i32 0, i32 0, !dbg !124
  %arraydecay20 = getelementptr inbounds [36 x i16]* %x19, i32 0, i32 0, !dbg !124
  %call = call arm_aapcscc  i32 @gfEqual(i16* %arraydecay, i16* %arraydecay20), !dbg !124
  %tobool = icmp ne i32 %call, 0, !dbg !124
  br i1 %tobool, label %if.then21, label %if.else33, !dbg !124

if.then21:                                        ; preds = %if.then17
  %10 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !126
  %y22 = getelementptr inbounds %struct.ecPoint.1* %10, i32 0, i32 1, !dbg !126
  %arraydecay23 = getelementptr inbounds [36 x i16]* %y22, i32 0, i32 0, !dbg !126
  %11 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !126
  %y24 = getelementptr inbounds %struct.ecPoint.1* %11, i32 0, i32 1, !dbg !126
  %arraydecay25 = getelementptr inbounds [36 x i16]* %y24, i32 0, i32 0, !dbg !126
  %call26 = call arm_aapcscc  i32 @gfEqual(i16* %arraydecay23, i16* %arraydecay25), !dbg !126
  %tobool27 = icmp ne i32 %call26, 0, !dbg !126
  br i1 %tobool27, label %if.then28, label %if.else, !dbg !126

if.then28:                                        ; preds = %if.then21
  %12 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !128
  call arm_aapcscc  void @ecDouble(%struct.ecPoint.1* %12), !dbg !128
  br label %if.end, !dbg !130

if.else:                                          ; preds = %if.then21
  %13 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !131
  %y29 = getelementptr inbounds %struct.ecPoint.1* %13, i32 0, i32 1, !dbg !131
  %arrayidx30 = getelementptr inbounds [36 x i16]* %y29, i32 0, i32 0, !dbg !131
  store i16 0, i16* %arrayidx30, align 2, !dbg !131
  %14 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !131
  %x31 = getelementptr inbounds %struct.ecPoint.1* %14, i32 0, i32 0, !dbg !131
  %arrayidx32 = getelementptr inbounds [36 x i16]* %x31, i32 0, i32 0, !dbg !131
  store i16 0, i16* %arrayidx32, align 2, !dbg !131
  br label %if.end

if.end:                                           ; preds = %if.else, %if.then28
  br label %if.end76, !dbg !133

if.else33:                                        ; preds = %if.then17
  %arraydecay34 = getelementptr inbounds [36 x i16]* %ty, i32 0, i32 0, !dbg !134
  %15 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !134
  %y35 = getelementptr inbounds %struct.ecPoint.1* %15, i32 0, i32 1, !dbg !134
  %arraydecay36 = getelementptr inbounds [36 x i16]* %y35, i32 0, i32 0, !dbg !134
  %16 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !134
  %y37 = getelementptr inbounds %struct.ecPoint.1* %16, i32 0, i32 1, !dbg !134
  %arraydecay38 = getelementptr inbounds [36 x i16]* %y37, i32 0, i32 0, !dbg !134
  call arm_aapcscc  void @gfAdd(i16* %arraydecay34, i16* %arraydecay36, i16* %arraydecay38), !dbg !134
  %arraydecay39 = getelementptr inbounds [36 x i16]* %tx, i32 0, i32 0, !dbg !136
  %17 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !136
  %x40 = getelementptr inbounds %struct.ecPoint.1* %17, i32 0, i32 0, !dbg !136
  %arraydecay41 = getelementptr inbounds [36 x i16]* %x40, i32 0, i32 0, !dbg !136
  %18 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !136
  %x42 = getelementptr inbounds %struct.ecPoint.1* %18, i32 0, i32 0, !dbg !136
  %arraydecay43 = getelementptr inbounds [36 x i16]* %x42, i32 0, i32 0, !dbg !136
  call arm_aapcscc  void @gfAdd(i16* %arraydecay39, i16* %arraydecay41, i16* %arraydecay43), !dbg !136
  %arraydecay44 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !137
  %arraydecay45 = getelementptr inbounds [36 x i16]* %tx, i32 0, i32 0, !dbg !137
  %call46 = call arm_aapcscc  i32 @gfInvert(i16* %arraydecay44, i16* %arraydecay45), !dbg !137
  %arraydecay47 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !138
  %arraydecay48 = getelementptr inbounds [36 x i16]* %ty, i32 0, i32 0, !dbg !138
  %arraydecay49 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !138
  call arm_aapcscc  void @gfMultiply(i16* %arraydecay47, i16* %arraydecay48, i16* %arraydecay49), !dbg !138
  %arraydecay50 = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !139
  %arraydecay51 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !139
  call arm_aapcscc  void @gfSquare(i16* %arraydecay50, i16* %arraydecay51), !dbg !139
  %arraydecay52 = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !140
  %arraydecay53 = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !140
  %arraydecay54 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !140
  call arm_aapcscc  void @gfAdd(i16* %arraydecay52, i16* %arraydecay53, i16* %arraydecay54), !dbg !140
  %arraydecay55 = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !141
  %arraydecay56 = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !141
  %arraydecay57 = getelementptr inbounds [36 x i16]* %tx, i32 0, i32 0, !dbg !141
  call arm_aapcscc  void @gfAdd(i16* %arraydecay55, i16* %arraydecay56, i16* %arraydecay57), !dbg !141
  %arraydecay58 = getelementptr inbounds [36 x i16]* %tx, i32 0, i32 0, !dbg !142
  %19 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !142
  %x59 = getelementptr inbounds %struct.ecPoint.1* %19, i32 0, i32 0, !dbg !142
  %arraydecay60 = getelementptr inbounds [36 x i16]* %x59, i32 0, i32 0, !dbg !142
  %arraydecay61 = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !142
  call arm_aapcscc  void @gfAdd(i16* %arraydecay58, i16* %arraydecay60, i16* %arraydecay61), !dbg !142
  %arraydecay62 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !143
  %arraydecay63 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !143
  %arraydecay64 = getelementptr inbounds [36 x i16]* %tx, i32 0, i32 0, !dbg !143
  call arm_aapcscc  void @gfMultiply(i16* %arraydecay62, i16* %arraydecay63, i16* %arraydecay64), !dbg !143
  %arraydecay65 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !144
  %arraydecay66 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !144
  %arraydecay67 = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !144
  call arm_aapcscc  void @gfAdd(i16* %arraydecay65, i16* %arraydecay66, i16* %arraydecay67), !dbg !144
  %20 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !145
  %y68 = getelementptr inbounds %struct.ecPoint.1* %20, i32 0, i32 1, !dbg !145
  %arraydecay69 = getelementptr inbounds [36 x i16]* %y68, i32 0, i32 0, !dbg !145
  %arraydecay70 = getelementptr inbounds [36 x i16]* %t, i32 0, i32 0, !dbg !145
  %21 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !145
  %y71 = getelementptr inbounds %struct.ecPoint.1* %21, i32 0, i32 1, !dbg !145
  %arraydecay72 = getelementptr inbounds [36 x i16]* %y71, i32 0, i32 0, !dbg !145
  call arm_aapcscc  void @gfAdd(i16* %arraydecay69, i16* %arraydecay70, i16* %arraydecay72), !dbg !145
  %22 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !146
  %x73 = getelementptr inbounds %struct.ecPoint.1* %22, i32 0, i32 0, !dbg !146
  %arraydecay74 = getelementptr inbounds [36 x i16]* %x73, i32 0, i32 0, !dbg !146
  %arraydecay75 = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !146
  call arm_aapcscc  void @gfCopy(i16* %arraydecay74, i16* %arraydecay75), !dbg !146
  br label %if.end76

if.end76:                                         ; preds = %if.else33, %if.end
  br label %if.end86, !dbg !147

if.else77:                                        ; preds = %lor.lhs.false11
  %23 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !148
  %x78 = getelementptr inbounds %struct.ecPoint.1* %23, i32 0, i32 0, !dbg !148
  %arraydecay79 = getelementptr inbounds [36 x i16]* %x78, i32 0, i32 0, !dbg !148
  %24 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !148
  %x80 = getelementptr inbounds %struct.ecPoint.1* %24, i32 0, i32 0, !dbg !148
  %arraydecay81 = getelementptr inbounds [36 x i16]* %x80, i32 0, i32 0, !dbg !148
  call arm_aapcscc  void @gfCopy(i16* %arraydecay79, i16* %arraydecay81), !dbg !148
  %25 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !150
  %y82 = getelementptr inbounds %struct.ecPoint.1* %25, i32 0, i32 1, !dbg !150
  %arraydecay83 = getelementptr inbounds [36 x i16]* %y82, i32 0, i32 0, !dbg !150
  %26 = load %struct.ecPoint.1** %q.addr, align 4, !dbg !150
  %y84 = getelementptr inbounds %struct.ecPoint.1* %26, i32 0, i32 1, !dbg !150
  %arraydecay85 = getelementptr inbounds [36 x i16]* %y84, i32 0, i32 0, !dbg !150
  call arm_aapcscc  void @gfCopy(i16* %arraydecay83, i16* %arraydecay85), !dbg !150
  br label %if.end86

if.end86:                                         ; preds = %if.else77, %if.end76
  br label %if.end87, !dbg !151

if.end87:                                         ; preds = %if.end86, %lor.lhs.false
  ret void, !dbg !152
}

declare arm_aapcscc i32 @gfEqual(i16*, i16*)

define arm_aapcscc void @ecDouble(%struct.ecPoint.1* %p) nounwind uwtable {
entry:
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %lambda = alloca [36 x i16], align 2
  %t1 = alloca [36 x i16], align 2
  %t2 = alloca [36 x i16], align 2
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !153), !dbg !154
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %lambda}, metadata !155), !dbg !157
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %t1}, metadata !158), !dbg !159
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %t2}, metadata !160), !dbg !161
  %arraydecay = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !162
  %0 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !162
  %x = getelementptr inbounds %struct.ecPoint.1* %0, i32 0, i32 0, !dbg !162
  %arraydecay1 = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !162
  %call = call arm_aapcscc  i32 @gfInvert(i16* %arraydecay, i16* %arraydecay1), !dbg !162
  %arraydecay2 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !163
  %1 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !163
  %y = getelementptr inbounds %struct.ecPoint.1* %1, i32 0, i32 1, !dbg !163
  %arraydecay3 = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !163
  %arraydecay4 = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !163
  call arm_aapcscc  void @gfMultiply(i16* %arraydecay2, i16* %arraydecay3, i16* %arraydecay4), !dbg !163
  %arraydecay5 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !164
  %arraydecay6 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !164
  %2 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !164
  %x7 = getelementptr inbounds %struct.ecPoint.1* %2, i32 0, i32 0, !dbg !164
  %arraydecay8 = getelementptr inbounds [36 x i16]* %x7, i32 0, i32 0, !dbg !164
  call arm_aapcscc  void @gfAdd(i16* %arraydecay5, i16* %arraydecay6, i16* %arraydecay8), !dbg !164
  %arraydecay9 = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !165
  %arraydecay10 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !165
  call arm_aapcscc  void @gfSquare(i16* %arraydecay9, i16* %arraydecay10), !dbg !165
  %arraydecay11 = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !166
  %arraydecay12 = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !166
  %arraydecay13 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !166
  call arm_aapcscc  void @gfAdd(i16* %arraydecay11, i16* %arraydecay12, i16* %arraydecay13), !dbg !166
  %3 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !167
  %y14 = getelementptr inbounds %struct.ecPoint.1* %3, i32 0, i32 1, !dbg !167
  %arraydecay15 = getelementptr inbounds [36 x i16]* %y14, i32 0, i32 0, !dbg !167
  %4 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !167
  %x16 = getelementptr inbounds %struct.ecPoint.1* %4, i32 0, i32 0, !dbg !167
  %arraydecay17 = getelementptr inbounds [36 x i16]* %x16, i32 0, i32 0, !dbg !167
  call arm_aapcscc  void @gfSquare(i16* %arraydecay15, i16* %arraydecay17), !dbg !167
  %arraydecay18 = getelementptr inbounds [36 x i16]* %t2, i32 0, i32 0, !dbg !168
  %arraydecay19 = getelementptr inbounds [36 x i16]* %lambda, i32 0, i32 0, !dbg !168
  %arraydecay20 = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !168
  call arm_aapcscc  void @gfMultiply(i16* %arraydecay18, i16* %arraydecay19, i16* %arraydecay20), !dbg !168
  %5 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !169
  %y21 = getelementptr inbounds %struct.ecPoint.1* %5, i32 0, i32 1, !dbg !169
  %arraydecay22 = getelementptr inbounds [36 x i16]* %y21, i32 0, i32 0, !dbg !169
  %6 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !169
  %y23 = getelementptr inbounds %struct.ecPoint.1* %6, i32 0, i32 1, !dbg !169
  %arraydecay24 = getelementptr inbounds [36 x i16]* %y23, i32 0, i32 0, !dbg !169
  %arraydecay25 = getelementptr inbounds [36 x i16]* %t2, i32 0, i32 0, !dbg !169
  call arm_aapcscc  void @gfAdd(i16* %arraydecay22, i16* %arraydecay24, i16* %arraydecay25), !dbg !169
  %7 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !170
  %y26 = getelementptr inbounds %struct.ecPoint.1* %7, i32 0, i32 1, !dbg !170
  %arraydecay27 = getelementptr inbounds [36 x i16]* %y26, i32 0, i32 0, !dbg !170
  %8 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !170
  %y28 = getelementptr inbounds %struct.ecPoint.1* %8, i32 0, i32 1, !dbg !170
  %arraydecay29 = getelementptr inbounds [36 x i16]* %y28, i32 0, i32 0, !dbg !170
  %arraydecay30 = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !170
  call arm_aapcscc  void @gfAdd(i16* %arraydecay27, i16* %arraydecay29, i16* %arraydecay30), !dbg !170
  %9 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !171
  %x31 = getelementptr inbounds %struct.ecPoint.1* %9, i32 0, i32 0, !dbg !171
  %arraydecay32 = getelementptr inbounds [36 x i16]* %x31, i32 0, i32 0, !dbg !171
  %arraydecay33 = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !171
  call arm_aapcscc  void @gfCopy(i16* %arraydecay32, i16* %arraydecay33), !dbg !171
  ret void, !dbg !172
}

define arm_aapcscc void @ecSub(%struct.ecPoint.1* %p, %struct.ecPoint.1* %r) nounwind uwtable {
entry:
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %r.addr = alloca %struct.ecPoint.1*, align 4
  %t = alloca %struct.ecPoint.1, align 2
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !173), !dbg !174
  store %struct.ecPoint.1* %r, %struct.ecPoint.1** %r.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %r.addr}, metadata !175), !dbg !176
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1* %t}, metadata !177), !dbg !179
  %x = getelementptr inbounds %struct.ecPoint.1* %t, i32 0, i32 0, !dbg !180
  %arraydecay = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !180
  %0 = load %struct.ecPoint.1** %r.addr, align 4, !dbg !180
  %x1 = getelementptr inbounds %struct.ecPoint.1* %0, i32 0, i32 0, !dbg !180
  %arraydecay2 = getelementptr inbounds [36 x i16]* %x1, i32 0, i32 0, !dbg !180
  call arm_aapcscc  void @gfCopy(i16* %arraydecay, i16* %arraydecay2), !dbg !180
  %y = getelementptr inbounds %struct.ecPoint.1* %t, i32 0, i32 1, !dbg !181
  %arraydecay3 = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !181
  %1 = load %struct.ecPoint.1** %r.addr, align 4, !dbg !181
  %x4 = getelementptr inbounds %struct.ecPoint.1* %1, i32 0, i32 0, !dbg !181
  %arraydecay5 = getelementptr inbounds [36 x i16]* %x4, i32 0, i32 0, !dbg !181
  %2 = load %struct.ecPoint.1** %r.addr, align 4, !dbg !181
  %y6 = getelementptr inbounds %struct.ecPoint.1* %2, i32 0, i32 1, !dbg !181
  %arraydecay7 = getelementptr inbounds [36 x i16]* %y6, i32 0, i32 0, !dbg !181
  call arm_aapcscc  void @gfAdd(i16* %arraydecay3, i16* %arraydecay5, i16* %arraydecay7), !dbg !181
  %3 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !182
  call arm_aapcscc  void @ecAdd(%struct.ecPoint.1* %3, %struct.ecPoint.1* %t), !dbg !182
  ret void, !dbg !183
}

define arm_aapcscc void @ecMultiply(%struct.ecPoint.1* %p, i16* %k) nounwind uwtable {
entry:
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %k.addr = alloca i16*, align 4
  %h = alloca [19 x i16], align 2
  %z = alloca i32, align 4
  %hi = alloca i32, align 4
  %ki = alloca i32, align 4
  %i = alloca i16, align 2
  %r = alloca %struct.ecPoint.1, align 2
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !184), !dbg !185
  store i16* %k, i16** %k.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %k.addr}, metadata !186), !dbg !189
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %h}, metadata !190), !dbg !196
  call void @llvm.dbg.declare(metadata !{i32* %z}, metadata !197), !dbg !198
  call void @llvm.dbg.declare(metadata !{i32* %hi}, metadata !199), !dbg !200
  call void @llvm.dbg.declare(metadata !{i32* %ki}, metadata !201), !dbg !202
  call void @llvm.dbg.declare(metadata !{i16* %i}, metadata !203), !dbg !204
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1* %r}, metadata !205), !dbg !206
  %x = getelementptr inbounds %struct.ecPoint.1* %r, i32 0, i32 0, !dbg !207
  %arraydecay = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !207
  %0 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !207
  %x1 = getelementptr inbounds %struct.ecPoint.1* %0, i32 0, i32 0, !dbg !207
  %arraydecay2 = getelementptr inbounds [36 x i16]* %x1, i32 0, i32 0, !dbg !207
  call arm_aapcscc  void @gfCopy(i16* %arraydecay, i16* %arraydecay2), !dbg !207
  %1 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !208
  %x3 = getelementptr inbounds %struct.ecPoint.1* %1, i32 0, i32 0, !dbg !208
  %arrayidx = getelementptr inbounds [36 x i16]* %x3, i32 0, i32 0, !dbg !208
  store i16 0, i16* %arrayidx, align 2, !dbg !208
  %y = getelementptr inbounds %struct.ecPoint.1* %r, i32 0, i32 1, !dbg !209
  %arraydecay4 = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !209
  %2 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !209
  %y5 = getelementptr inbounds %struct.ecPoint.1* %2, i32 0, i32 1, !dbg !209
  %arraydecay6 = getelementptr inbounds [36 x i16]* %y5, i32 0, i32 0, !dbg !209
  call arm_aapcscc  void @gfCopy(i16* %arraydecay4, i16* %arraydecay6), !dbg !209
  %3 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !210
  %y7 = getelementptr inbounds %struct.ecPoint.1* %3, i32 0, i32 1, !dbg !210
  %arrayidx8 = getelementptr inbounds [36 x i16]* %y7, i32 0, i32 0, !dbg !210
  store i16 0, i16* %arrayidx8, align 2, !dbg !210
  %arraydecay9 = getelementptr inbounds [19 x i16]* %h, i32 0, i32 0, !dbg !211
  %4 = load i16** %k.addr, align 4, !dbg !211
  %call = call arm_aapcscc  i32 @vlShortMultiply(i16* %arraydecay9, i16* %4, i16 zeroext 3), !dbg !211
  %arraydecay10 = getelementptr inbounds [19 x i16]* %h, i32 0, i32 0, !dbg !212
  %call11 = call arm_aapcscc  i32 @vlNumBits(i16* %arraydecay10), !dbg !212
  %sub = sub nsw i32 %call11, 1, !dbg !212
  store i32 %sub, i32* %z, align 4, !dbg !212
  store i16 1, i16* %i, align 2, !dbg !213
  br label %for.cond, !dbg !214

for.cond:                                         ; preds = %if.end24, %entry
  %arraydecay12 = getelementptr inbounds [19 x i16]* %h, i32 0, i32 0, !dbg !216
  %5 = load i16* %i, align 2, !dbg !216
  %call13 = call arm_aapcscc  i32 @vlTakeBit(i16* %arraydecay12, i16 zeroext %5), !dbg !216
  store i32 %call13, i32* %hi, align 4, !dbg !216
  %6 = load i16** %k.addr, align 4, !dbg !218
  %7 = load i16* %i, align 2, !dbg !218
  %call14 = call arm_aapcscc  i32 @vlTakeBit(i16* %6, i16 zeroext %7), !dbg !218
  store i32 %call14, i32* %ki, align 4, !dbg !218
  %8 = load i32* %hi, align 4, !dbg !219
  %cmp = icmp eq i32 %8, 1, !dbg !219
  br i1 %cmp, label %land.lhs.true, label %if.end, !dbg !219

land.lhs.true:                                    ; preds = %for.cond
  %9 = load i32* %ki, align 4, !dbg !219
  %cmp15 = icmp eq i32 %9, 0, !dbg !219
  br i1 %cmp15, label %if.then, label %if.end, !dbg !219

if.then:                                          ; preds = %land.lhs.true
  %10 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !220
  call arm_aapcscc  void @ecAdd(%struct.ecPoint.1* %10, %struct.ecPoint.1* %r), !dbg !220
  br label %if.end, !dbg !222

if.end:                                           ; preds = %if.then, %land.lhs.true, %for.cond
  %11 = load i32* %hi, align 4, !dbg !223
  %cmp16 = icmp eq i32 %11, 0, !dbg !223
  br i1 %cmp16, label %land.lhs.true17, label %if.end20, !dbg !223

land.lhs.true17:                                  ; preds = %if.end
  %12 = load i32* %ki, align 4, !dbg !223
  %cmp18 = icmp eq i32 %12, 1, !dbg !223
  br i1 %cmp18, label %if.then19, label %if.end20, !dbg !223

if.then19:                                        ; preds = %land.lhs.true17
  %13 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !224
  call arm_aapcscc  void @ecSub(%struct.ecPoint.1* %13, %struct.ecPoint.1* %r), !dbg !224
  br label %if.end20, !dbg !226

if.end20:                                         ; preds = %if.then19, %land.lhs.true17, %if.end
  %14 = load i16* %i, align 2, !dbg !227
  %conv = zext i16 %14 to i32, !dbg !227
  %15 = load i32* %z, align 4, !dbg !227
  %cmp21 = icmp sge i32 %conv, %15, !dbg !227
  br i1 %cmp21, label %if.then23, label %if.end24, !dbg !227

if.then23:                                        ; preds = %if.end20
  br label %for.end, !dbg !228

if.end24:                                         ; preds = %if.end20
  %16 = load i16* %i, align 2, !dbg !230
  %inc = add i16 %16, 1, !dbg !230
  store i16 %inc, i16* %i, align 2, !dbg !230
  call arm_aapcscc  void @ecDouble(%struct.ecPoint.1* %r), !dbg !231
  br label %for.cond, !dbg !232

for.end:                                          ; preds = %if.then23
  ret void, !dbg !233
}

declare arm_aapcscc i32 @vlShortMultiply(i16*, i16*, i16 zeroext)

declare arm_aapcscc i32 @vlNumBits(i16*)

declare arm_aapcscc i32 @vlTakeBit(i16*, i16 zeroext)

define arm_aapcscc i32 @ecYbit(%struct.ecPoint.1* %p) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %t1 = alloca [36 x i16], align 2
  %t2 = alloca [36 x i16], align 2
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !234), !dbg !235
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %t1}, metadata !236), !dbg !238
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %t2}, metadata !239), !dbg !240
  %0 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !241
  %x = getelementptr inbounds %struct.ecPoint.1* %0, i32 0, i32 0, !dbg !241
  %arrayidx = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !241
  %1 = load i16* %arrayidx, align 2, !dbg !241
  %conv = zext i16 %1 to i32, !dbg !241
  %cmp = icmp eq i32 %conv, 0, !dbg !241
  br i1 %cmp, label %if.then, label %if.else, !dbg !241

if.then:                                          ; preds = %entry
  store i32 0, i32* %retval, !dbg !242
  br label %return, !dbg !242

if.else:                                          ; preds = %entry
  %arraydecay = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !244
  %2 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !244
  %x2 = getelementptr inbounds %struct.ecPoint.1* %2, i32 0, i32 0, !dbg !244
  %arraydecay3 = getelementptr inbounds [36 x i16]* %x2, i32 0, i32 0, !dbg !244
  %call = call arm_aapcscc  i32 @gfInvert(i16* %arraydecay, i16* %arraydecay3), !dbg !244
  %arraydecay4 = getelementptr inbounds [36 x i16]* %t2, i32 0, i32 0, !dbg !246
  %3 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !246
  %y = getelementptr inbounds %struct.ecPoint.1* %3, i32 0, i32 1, !dbg !246
  %arraydecay5 = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !246
  %arraydecay6 = getelementptr inbounds [36 x i16]* %t1, i32 0, i32 0, !dbg !246
  call arm_aapcscc  void @gfMultiply(i16* %arraydecay4, i16* %arraydecay5, i16* %arraydecay6), !dbg !246
  %arraydecay7 = getelementptr inbounds [36 x i16]* %t2, i32 0, i32 0, !dbg !247
  %call8 = call arm_aapcscc  i32 @gfYbit(i16* %arraydecay7), !dbg !247
  store i32 %call8, i32* %retval, !dbg !247
  br label %return, !dbg !247

return:                                           ; preds = %if.else, %if.then
  %4 = load i32* %retval, !dbg !248
  ret i32 %4, !dbg !248
}

define arm_aapcscc void @ecPack(%struct.ecPoint.1* %p, i16* %k) nounwind uwtable {
entry:
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %k.addr = alloca i16*, align 4
  %a = alloca [19 x i16], align 2
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !249), !dbg !250
  store i16* %k, i16** %k.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %k.addr}, metadata !251), !dbg !253
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %a}, metadata !254), !dbg !256
  %0 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !257
  %x = getelementptr inbounds %struct.ecPoint.1* %0, i32 0, i32 0, !dbg !257
  %arrayidx = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !257
  %1 = load i16* %arrayidx, align 2, !dbg !257
  %tobool = icmp ne i16 %1, 0, !dbg !257
  br i1 %tobool, label %if.then, label %if.else, !dbg !257

if.then:                                          ; preds = %entry
  %2 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !258
  %x1 = getelementptr inbounds %struct.ecPoint.1* %2, i32 0, i32 0, !dbg !258
  %arraydecay = getelementptr inbounds [36 x i16]* %x1, i32 0, i32 0, !dbg !258
  %3 = load i16** %k.addr, align 4, !dbg !258
  call arm_aapcscc  void @gfPack(i16* %arraydecay, i16* %3), !dbg !258
  %4 = load i16** %k.addr, align 4, !dbg !260
  call arm_aapcscc  void @vlShortLshift(i16* %4, i32 1), !dbg !260
  %arraydecay2 = getelementptr inbounds [19 x i16]* %a, i32 0, i32 0, !dbg !261
  %5 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !262
  %call = call arm_aapcscc  i32 @ecYbit(%struct.ecPoint.1* %5), !dbg !262
  %conv = trunc i32 %call to i16, !dbg !262
  call arm_aapcscc  void @vlShortSet(i16* %arraydecay2, i16 zeroext %conv), !dbg !262
  %6 = load i16** %k.addr, align 4, !dbg !263
  %arraydecay3 = getelementptr inbounds [19 x i16]* %a, i32 0, i32 0, !dbg !263
  call arm_aapcscc  void @vlAdd(i16* %6, i16* %arraydecay3), !dbg !263
  br label %if.end9, !dbg !264

if.else:                                          ; preds = %entry
  %7 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !265
  %y = getelementptr inbounds %struct.ecPoint.1* %7, i32 0, i32 1, !dbg !265
  %arrayidx4 = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !265
  %8 = load i16* %arrayidx4, align 2, !dbg !265
  %tobool5 = icmp ne i16 %8, 0, !dbg !265
  br i1 %tobool5, label %if.then6, label %if.else7, !dbg !265

if.then6:                                         ; preds = %if.else
  %9 = load i16** %k.addr, align 4, !dbg !266
  call arm_aapcscc  void @vlShortSet(i16* %9, i16 zeroext 1), !dbg !266
  br label %if.end, !dbg !268

if.else7:                                         ; preds = %if.else
  %10 = load i16** %k.addr, align 4, !dbg !269
  %arrayidx8 = getelementptr inbounds i16* %10, i32 0, !dbg !269
  store i16 0, i16* %arrayidx8, !dbg !269
  br label %if.end

if.end:                                           ; preds = %if.else7, %if.then6
  br label %if.end9

if.end9:                                          ; preds = %if.end, %if.then
  ret void, !dbg !271
}

declare arm_aapcscc void @gfPack(i16*, i16*)

declare arm_aapcscc void @vlShortLshift(i16*, i32)

declare arm_aapcscc void @vlShortSet(i16*, i16 zeroext)

declare arm_aapcscc void @vlAdd(i16*, i16*)

define arm_aapcscc void @ecUnpack(%struct.ecPoint.1* %p, i16* %k) nounwind uwtable {
entry:
  %p.addr = alloca %struct.ecPoint.1*, align 4
  %k.addr = alloca i16*, align 4
  %yb = alloca i32, align 4
  %a = alloca [19 x i16], align 2
  store %struct.ecPoint.1* %p, %struct.ecPoint.1** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint.1** %p.addr}, metadata !272), !dbg !273
  store i16* %k, i16** %k.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %k.addr}, metadata !274), !dbg !275
  call void @llvm.dbg.declare(metadata !{i32* %yb}, metadata !276), !dbg !278
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %a}, metadata !279), !dbg !280
  %arraydecay = getelementptr inbounds [19 x i16]* %a, i32 0, i32 0, !dbg !281
  %0 = load i16** %k.addr, align 4, !dbg !281
  call arm_aapcscc  void @vlCopy(i16* %arraydecay, i16* %0), !dbg !281
  %arrayidx = getelementptr inbounds [19 x i16]* %a, i32 0, i32 0, !dbg !282
  %1 = load i16* %arrayidx, align 2, !dbg !282
  %conv = zext i16 %1 to i32, !dbg !282
  %tobool = icmp ne i32 %conv, 0, !dbg !282
  br i1 %tobool, label %cond.true, label %cond.false, !dbg !282

cond.true:                                        ; preds = %entry
  %arrayidx1 = getelementptr inbounds [19 x i16]* %a, i32 0, i32 1, !dbg !282
  %2 = load i16* %arrayidx1, align 2, !dbg !282
  %conv2 = zext i16 %2 to i32, !dbg !282
  %and = and i32 %conv2, 1, !dbg !282
  br label %cond.end, !dbg !282

cond.false:                                       ; preds = %entry
  br label %cond.end, !dbg !282

cond.end:                                         ; preds = %cond.false, %cond.true
  %cond = phi i32 [ %and, %cond.true ], [ 0, %cond.false ], !dbg !282
  store i32 %cond, i32* %yb, align 4, !dbg !282
  %arraydecay3 = getelementptr inbounds [19 x i16]* %a, i32 0, i32 0, !dbg !283
  call arm_aapcscc  void @vlShortRshift(i16* %arraydecay3, i32 1), !dbg !283
  %3 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !284
  %x = getelementptr inbounds %struct.ecPoint.1* %3, i32 0, i32 0, !dbg !284
  %arraydecay4 = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !284
  %arraydecay5 = getelementptr inbounds [19 x i16]* %a, i32 0, i32 0, !dbg !284
  call arm_aapcscc  void @gfUnpack(i16* %arraydecay4, i16* %arraydecay5), !dbg !284
  %4 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !285
  %x6 = getelementptr inbounds %struct.ecPoint.1* %4, i32 0, i32 0, !dbg !285
  %arrayidx7 = getelementptr inbounds [36 x i16]* %x6, i32 0, i32 0, !dbg !285
  %5 = load i16* %arrayidx7, align 2, !dbg !285
  %conv8 = zext i16 %5 to i32, !dbg !285
  %tobool9 = icmp ne i32 %conv8, 0, !dbg !285
  br i1 %tobool9, label %if.then, label %lor.lhs.false, !dbg !285

lor.lhs.false:                                    ; preds = %cond.end
  %6 = load i32* %yb, align 4, !dbg !285
  %tobool10 = icmp ne i32 %6, 0, !dbg !285
  br i1 %tobool10, label %if.then, label %if.else, !dbg !285

if.then:                                          ; preds = %lor.lhs.false, %cond.end
  %7 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !286
  %8 = load i32* %yb, align 4, !dbg !286
  %call = call arm_aapcscc  i32 @ecCalcY(%struct.ecPoint.1* %7, i32 %8), !dbg !286
  br label %if.end, !dbg !288

if.else:                                          ; preds = %lor.lhs.false
  %9 = load %struct.ecPoint.1** %p.addr, align 4, !dbg !289
  %y = getelementptr inbounds %struct.ecPoint.1* %9, i32 0, i32 1, !dbg !289
  %arrayidx11 = getelementptr inbounds [36 x i16]* %y, i32 0, i32 0, !dbg !289
  store i16 0, i16* %arrayidx11, align 2, !dbg !289
  br label %if.end

if.end:                                           ; preds = %if.else, %if.then
  ret void, !dbg !291
}

declare arm_aapcscc void @vlCopy(i16*, i16*)

declare arm_aapcscc void @vlShortRshift(i16*, i32)

declare arm_aapcscc void @gfUnpack(i16*, i16*)

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !11, metadata !17, metadata !20, metadata !23, metadata !26, metadata !29, metadata !32, metadata !35}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecCopy", metadata !"ecCopy", metadata !"", metadata !6, i32 106, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.ecPoint.1*, %struct.ecPoint.1*)* @ecCopy, null, null, metadata !9} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"ec_curve.c", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{null}
!9 = metadata !{metadata !10}
!10 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!11 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecCalcY", metadata !"ecCalcY", metadata !"", metadata !6, i32 114, metadata !12, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct.ecPoint.1*, i32)* @ecCalcY, null, null, metadata !15} ; [ DW_TAG_subprogram ]
!12 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !13, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!13 = metadata !{metadata !14}
!14 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!15 = metadata !{metadata !16}
!16 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!17 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecAdd", metadata !"ecAdd", metadata !"", metadata !6, i32 158, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.ecPoint.1*, %struct.ecPoint.1*)* @ecAdd, null, null, metadata !18} ; [ DW_TAG_subprogram ]
!18 = metadata !{metadata !19}
!19 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!20 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecSub", metadata !"ecSub", metadata !"", metadata !6, i32 205, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.ecPoint.1*, %struct.ecPoint.1*)* @ecSub, null, null, metadata !21} ; [ DW_TAG_subprogram ]
!21 = metadata !{metadata !22}
!22 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!23 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecDouble", metadata !"ecDouble", metadata !"", metadata !6, i32 225, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.ecPoint.1*)* @ecDouble, null, null, metadata !24} ; [ DW_TAG_subprogram ]
!24 = metadata !{metadata !25}
!25 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!26 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecMultiply", metadata !"ecMultiply", metadata !"", metadata !6, i32 247, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.ecPoint.1*, i16*)* @ecMultiply, null, null, metadata !27} ; [ DW_TAG_subprogram ]
!27 = metadata !{metadata !28}
!28 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!29 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecYbit", metadata !"ecYbit", metadata !"", metadata !6, i32 278, metadata !12, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct.ecPoint.1*)* @ecYbit, null, null, metadata !30} ; [ DW_TAG_subprogram ]
!30 = metadata !{metadata !31}
!31 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!32 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecPack", metadata !"ecPack", metadata !"", metadata !6, i32 293, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.ecPoint.1*, i16*)* @ecPack, null, null, metadata !33} ; [ DW_TAG_subprogram ]
!33 = metadata !{metadata !34}
!34 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!35 = metadata !{i32 720942, i32 0, metadata !6, metadata !"ecUnpack", metadata !"ecUnpack", metadata !"", metadata !6, i32 311, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.ecPoint.1*, i16*)* @ecUnpack, null, null, metadata !36} ; [ DW_TAG_subprogram ]
!36 = metadata !{metadata !37}
!37 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!38 = metadata !{i32 721153, metadata !5, metadata !"p", metadata !6, i32 16777320, metadata !39, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!39 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !40} ; [ DW_TAG_pointer_type ]
!40 = metadata !{i32 720918, null, metadata !"ecPoint", metadata !6, i32 18, i64 0, i64 0, i64 0, i32 0, metadata !41} ; [ DW_TAG_typedef ]
!41 = metadata !{i32 720915, null, metadata !"", metadata !42, i32 16, i64 1152, i64 16, i32 0, i32 0, null, metadata !43, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!42 = metadata !{i32 720937, metadata !"ec_curve.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!43 = metadata !{metadata !44, metadata !52}
!44 = metadata !{i32 720909, metadata !41, metadata !"x", metadata !42, i32 17, i64 576, i64 16, i64 0, i32 0, metadata !45} ; [ DW_TAG_member ]
!45 = metadata !{i32 720918, null, metadata !"gfPoint", metadata !42, i32 36, i64 0, i64 0, i64 0, i32 0, metadata !46} ; [ DW_TAG_typedef ]
!46 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 576, i64 16, i32 0, i32 0, metadata !47, metadata !50, i32 0, i32 0} ; [ DW_TAG_array_type ]
!47 = metadata !{i32 720918, null, metadata !"lunit", metadata !42, i32 27, i64 0, i64 0, i64 0, i32 0, metadata !48} ; [ DW_TAG_typedef ]
!48 = metadata !{i32 720918, null, metadata !"word16", metadata !42, i32 11, i64 0, i64 0, i64 0, i32 0, metadata !49} ; [ DW_TAG_typedef ]
!49 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!50 = metadata !{metadata !51}
!51 = metadata !{i32 720929, i64 0, i64 35}       ; [ DW_TAG_subrange_type ]
!52 = metadata !{i32 720909, metadata !41, metadata !"y", metadata !42, i32 17, i64 576, i64 16, i64 576, i32 0, metadata !45} ; [ DW_TAG_member ]
!53 = metadata !{i32 104, i32 23, metadata !5, null}
!54 = metadata !{i32 721153, metadata !5, metadata !"q", metadata !6, i32 33554536, metadata !55, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!55 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !56} ; [ DW_TAG_pointer_type ]
!56 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !40} ; [ DW_TAG_const_type ]
!57 = metadata !{i32 104, i32 41, metadata !5, null}
!58 = metadata !{i32 107, i32 2, metadata !59, null}
!59 = metadata !{i32 720907, metadata !5, i32 106, i32 1, metadata !6, i32 121} ; [ DW_TAG_lexical_block ]
!60 = metadata !{i32 108, i32 2, metadata !59, null}
!61 = metadata !{i32 109, i32 1, metadata !59, null}
!62 = metadata !{i32 721153, metadata !11, metadata !"p", metadata !6, i32 16777328, metadata !39, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!63 = metadata !{i32 112, i32 23, metadata !11, null}
!64 = metadata !{i32 721153, metadata !11, metadata !"ybit", metadata !6, i32 33554544, metadata !14, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!65 = metadata !{i32 112, i32 30, metadata !11, null}
!66 = metadata !{i32 721152, metadata !67, metadata !"a", metadata !6, i32 115, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!67 = metadata !{i32 720907, metadata !11, i32 114, i32 1, metadata !6, i32 122} ; [ DW_TAG_lexical_block ]
!68 = metadata !{i32 115, i32 10, metadata !67, null}
!69 = metadata !{i32 721152, metadata !67, metadata !"b", metadata !6, i32 115, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!70 = metadata !{i32 115, i32 13, metadata !67, null}
!71 = metadata !{i32 721152, metadata !67, metadata !"t", metadata !6, i32 115, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!72 = metadata !{i32 115, i32 16, metadata !67, null}
!73 = metadata !{i32 117, i32 2, metadata !67, null}
!74 = metadata !{i32 117, i32 12, metadata !67, null}
!75 = metadata !{i32 118, i32 2, metadata !67, null}
!76 = metadata !{i32 120, i32 3, metadata !77, null}
!77 = metadata !{i32 720907, metadata !67, i32 118, i32 20, metadata !6, i32 123} ; [ DW_TAG_lexical_block ]
!78 = metadata !{i32 121, i32 3, metadata !77, null}
!79 = metadata !{i32 124, i32 2, metadata !67, null}
!80 = metadata !{i32 125, i32 2, metadata !67, null}
!81 = metadata !{i32 126, i32 2, metadata !67, null}
!82 = metadata !{i32 127, i32 2, metadata !67, null}
!83 = metadata !{i32 128, i32 3, metadata !84, null}
!84 = metadata !{i32 720907, metadata !67, i32 127, i32 17, metadata !6, i32 124} ; [ DW_TAG_lexical_block ]
!85 = metadata !{i32 130, i32 3, metadata !84, null}
!86 = metadata !{i32 130, i32 16, metadata !84, null}
!87 = metadata !{i32 131, i32 3, metadata !84, null}
!88 = metadata !{i32 134, i32 2, metadata !67, null}
!89 = metadata !{i32 135, i32 2, metadata !67, null}
!90 = metadata !{i32 136, i32 2, metadata !67, null}
!91 = metadata !{i32 138, i32 6, metadata !67, null}
!92 = metadata !{i32 140, i32 3, metadata !93, null}
!93 = metadata !{i32 720907, metadata !67, i32 138, i32 24, metadata !6, i32 125} ; [ DW_TAG_lexical_block ]
!94 = metadata !{i32 140, i32 16, metadata !93, null}
!95 = metadata !{i32 141, i32 3, metadata !93, null}
!96 = metadata !{i32 144, i32 2, metadata !67, null}
!97 = metadata !{i32 145, i32 6, metadata !67, null}
!98 = metadata !{i32 146, i32 3, metadata !99, null}
!99 = metadata !{i32 720907, metadata !67, i32 145, i32 26, metadata !6, i32 126} ; [ DW_TAG_lexical_block ]
!100 = metadata !{i32 147, i32 2, metadata !99, null}
!101 = metadata !{i32 149, i32 2, metadata !67, null}
!102 = metadata !{i32 151, i32 2, metadata !67, null}
!103 = metadata !{i32 151, i32 15, metadata !67, null}
!104 = metadata !{i32 152, i32 2, metadata !67, null}
!105 = metadata !{i32 153, i32 1, metadata !67, null}
!106 = metadata !{i32 721153, metadata !17, metadata !"p", metadata !6, i32 16777372, metadata !39, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!107 = metadata !{i32 156, i32 22, metadata !17, null}
!108 = metadata !{i32 721153, metadata !17, metadata !"q", metadata !6, i32 33554588, metadata !55, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!109 = metadata !{i32 156, i32 40, metadata !17, null}
!110 = metadata !{i32 721152, metadata !111, metadata !"lambda", metadata !6, i32 159, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!111 = metadata !{i32 720907, metadata !17, i32 158, i32 1, metadata !6, i32 127} ; [ DW_TAG_lexical_block ]
!112 = metadata !{i32 159, i32 10, metadata !111, null}
!113 = metadata !{i32 721152, metadata !111, metadata !"t", metadata !6, i32 159, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!114 = metadata !{i32 159, i32 18, metadata !111, null}
!115 = metadata !{i32 721152, metadata !111, metadata !"tx", metadata !6, i32 159, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!116 = metadata !{i32 159, i32 21, metadata !111, null}
!117 = metadata !{i32 721152, metadata !111, metadata !"ty", metadata !6, i32 159, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!118 = metadata !{i32 159, i32 25, metadata !111, null}
!119 = metadata !{i32 721152, metadata !111, metadata !"x3", metadata !6, i32 159, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!120 = metadata !{i32 159, i32 29, metadata !111, null}
!121 = metadata !{i32 162, i32 2, metadata !111, null}
!122 = metadata !{i32 163, i32 3, metadata !123, null}
!123 = metadata !{i32 720907, metadata !111, i32 162, i32 36, metadata !6, i32 128} ; [ DW_TAG_lexical_block ]
!124 = metadata !{i32 165, i32 8, metadata !125, null}
!125 = metadata !{i32 720907, metadata !123, i32 163, i32 37, metadata !6, i32 129} ; [ DW_TAG_lexical_block ]
!126 = metadata !{i32 167, i32 9, metadata !127, null}
!127 = metadata !{i32 720907, metadata !125, i32 165, i32 30, metadata !6, i32 130} ; [ DW_TAG_lexical_block ]
!128 = metadata !{i32 169, i32 6, metadata !129, null}
!129 = metadata !{i32 720907, metadata !127, i32 167, i32 31, metadata !6, i32 131} ; [ DW_TAG_lexical_block ]
!130 = metadata !{i32 170, i32 5, metadata !129, null}
!131 = metadata !{i32 173, i32 6, metadata !132, null}
!132 = metadata !{i32 720907, metadata !127, i32 170, i32 12, metadata !6, i32 132} ; [ DW_TAG_lexical_block ]
!133 = metadata !{i32 175, i32 4, metadata !127, null}
!134 = metadata !{i32 178, i32 5, metadata !135, null}
!135 = metadata !{i32 720907, metadata !125, i32 175, i32 11, metadata !6, i32 133} ; [ DW_TAG_lexical_block ]
!136 = metadata !{i32 179, i32 5, metadata !135, null}
!137 = metadata !{i32 180, i32 5, metadata !135, null}
!138 = metadata !{i32 181, i32 5, metadata !135, null}
!139 = metadata !{i32 183, i32 5, metadata !135, null}
!140 = metadata !{i32 184, i32 5, metadata !135, null}
!141 = metadata !{i32 185, i32 5, metadata !135, null}
!142 = metadata !{i32 187, i32 5, metadata !135, null}
!143 = metadata !{i32 188, i32 5, metadata !135, null}
!144 = metadata !{i32 189, i32 5, metadata !135, null}
!145 = metadata !{i32 190, i32 5, metadata !135, null}
!146 = metadata !{i32 192, i32 5, metadata !135, null}
!147 = metadata !{i32 194, i32 3, metadata !125, null}
!148 = metadata !{i32 196, i32 4, metadata !149, null}
!149 = metadata !{i32 720907, metadata !123, i32 194, i32 10, metadata !6, i32 134} ; [ DW_TAG_lexical_block ]
!150 = metadata !{i32 197, i32 4, metadata !149, null}
!151 = metadata !{i32 199, i32 2, metadata !123, null}
!152 = metadata !{i32 200, i32 1, metadata !111, null}
!153 = metadata !{i32 721153, metadata !23, metadata !"p", metadata !6, i32 16777439, metadata !39, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!154 = metadata !{i32 223, i32 25, metadata !23, null}
!155 = metadata !{i32 721152, metadata !156, metadata !"lambda", metadata !6, i32 226, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!156 = metadata !{i32 720907, metadata !23, i32 225, i32 1, metadata !6, i32 136} ; [ DW_TAG_lexical_block ]
!157 = metadata !{i32 226, i32 10, metadata !156, null}
!158 = metadata !{i32 721152, metadata !156, metadata !"t1", metadata !6, i32 226, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!159 = metadata !{i32 226, i32 18, metadata !156, null}
!160 = metadata !{i32 721152, metadata !156, metadata !"t2", metadata !6, i32 226, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!161 = metadata !{i32 226, i32 22, metadata !156, null}
!162 = metadata !{i32 229, i32 2, metadata !156, null}
!163 = metadata !{i32 230, i32 2, metadata !156, null}
!164 = metadata !{i32 231, i32 2, metadata !156, null}
!165 = metadata !{i32 233, i32 2, metadata !156, null}
!166 = metadata !{i32 234, i32 2, metadata !156, null}
!167 = metadata !{i32 236, i32 2, metadata !156, null}
!168 = metadata !{i32 237, i32 2, metadata !156, null}
!169 = metadata !{i32 238, i32 2, metadata !156, null}
!170 = metadata !{i32 239, i32 2, metadata !156, null}
!171 = metadata !{i32 241, i32 2, metadata !156, null}
!172 = metadata !{i32 242, i32 1, metadata !156, null}
!173 = metadata !{i32 721153, metadata !20, metadata !"p", metadata !6, i32 16777419, metadata !39, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!174 = metadata !{i32 203, i32 22, metadata !20, null}
!175 = metadata !{i32 721153, metadata !20, metadata !"r", metadata !6, i32 33554635, metadata !55, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!176 = metadata !{i32 203, i32 40, metadata !20, null}
!177 = metadata !{i32 721152, metadata !178, metadata !"t", metadata !6, i32 206, metadata !40, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!178 = metadata !{i32 720907, metadata !20, i32 205, i32 1, metadata !6, i32 135} ; [ DW_TAG_lexical_block ]
!179 = metadata !{i32 206, i32 10, metadata !178, null}
!180 = metadata !{i32 208, i32 2, metadata !178, null}
!181 = metadata !{i32 209, i32 2, metadata !178, null}
!182 = metadata !{i32 210, i32 2, metadata !178, null}
!183 = metadata !{i32 211, i32 1, metadata !178, null}
!184 = metadata !{i32 721153, metadata !26, metadata !"p", metadata !6, i32 16777461, metadata !39, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!185 = metadata !{i32 245, i32 27, metadata !26, null}
!186 = metadata !{i32 721153, metadata !26, metadata !"k", metadata !6, i32 33554677, metadata !187, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!187 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !188} ; [ DW_TAG_pointer_type ]
!188 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !48} ; [ DW_TAG_const_type ]
!189 = metadata !{i32 245, i32 44, metadata !26, null}
!190 = metadata !{i32 721152, metadata !191, metadata !"h", metadata !6, i32 248, metadata !192, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!191 = metadata !{i32 720907, metadata !26, i32 247, i32 1, metadata !6, i32 137} ; [ DW_TAG_lexical_block ]
!192 = metadata !{i32 720918, null, metadata !"vlPoint", metadata !6, i32 17, i64 0, i64 0, i64 0, i32 0, metadata !193} ; [ DW_TAG_typedef ]
!193 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 304, i64 16, i32 0, i32 0, metadata !48, metadata !194, i32 0, i32 0} ; [ DW_TAG_array_type ]
!194 = metadata !{metadata !195}
!195 = metadata !{i32 720929, i64 0, i64 18}      ; [ DW_TAG_subrange_type ]
!196 = metadata !{i32 248, i32 10, metadata !191, null}
!197 = metadata !{i32 721152, metadata !191, metadata !"z", metadata !6, i32 249, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!198 = metadata !{i32 249, i32 6, metadata !191, null}
!199 = metadata !{i32 721152, metadata !191, metadata !"hi", metadata !6, i32 249, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!200 = metadata !{i32 249, i32 9, metadata !191, null}
!201 = metadata !{i32 721152, metadata !191, metadata !"ki", metadata !6, i32 249, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!202 = metadata !{i32 249, i32 13, metadata !191, null}
!203 = metadata !{i32 721152, metadata !191, metadata !"i", metadata !6, i32 250, metadata !48, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!204 = metadata !{i32 250, i32 9, metadata !191, null}
!205 = metadata !{i32 721152, metadata !191, metadata !"r", metadata !6, i32 251, metadata !40, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!206 = metadata !{i32 251, i32 10, metadata !191, null}
!207 = metadata !{i32 253, i32 2, metadata !191, null}
!208 = metadata !{i32 253, i32 22, metadata !191, null}
!209 = metadata !{i32 254, i32 2, metadata !191, null}
!210 = metadata !{i32 254, i32 22, metadata !191, null}
!211 = metadata !{i32 255, i32 2, metadata !191, null}
!212 = metadata !{i32 256, i32 6, metadata !191, null}
!213 = metadata !{i32 257, i32 2, metadata !191, null}
!214 = metadata !{i32 258, i32 2, metadata !215, null}
!215 = metadata !{i32 720907, metadata !191, i32 258, i32 2, metadata !6, i32 138} ; [ DW_TAG_lexical_block ]
!216 = metadata !{i32 259, i32 8, metadata !217, null}
!217 = metadata !{i32 720907, metadata !215, i32 258, i32 11, metadata !6, i32 139} ; [ DW_TAG_lexical_block ]
!218 = metadata !{i32 260, i32 8, metadata !217, null}
!219 = metadata !{i32 261, i32 3, metadata !217, null}
!220 = metadata !{i32 262, i32 4, metadata !221, null}
!221 = metadata !{i32 720907, metadata !217, i32 261, i32 27, metadata !6, i32 140} ; [ DW_TAG_lexical_block ]
!222 = metadata !{i32 263, i32 3, metadata !221, null}
!223 = metadata !{i32 264, i32 3, metadata !217, null}
!224 = metadata !{i32 265, i32 4, metadata !225, null}
!225 = metadata !{i32 720907, metadata !217, i32 264, i32 27, metadata !6, i32 141} ; [ DW_TAG_lexical_block ]
!226 = metadata !{i32 266, i32 3, metadata !225, null}
!227 = metadata !{i32 267, i32 3, metadata !217, null}
!228 = metadata !{i32 268, i32 4, metadata !229, null}
!229 = metadata !{i32 720907, metadata !217, i32 267, i32 15, metadata !6, i32 142} ; [ DW_TAG_lexical_block ]
!230 = metadata !{i32 270, i32 3, metadata !217, null}
!231 = metadata !{i32 271, i32 3, metadata !217, null}
!232 = metadata !{i32 272, i32 2, metadata !217, null}
!233 = metadata !{i32 273, i32 1, metadata !191, null}
!234 = metadata !{i32 721153, metadata !29, metadata !"p", metadata !6, i32 16777492, metadata !55, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!235 = metadata !{i32 276, i32 28, metadata !29, null}
!236 = metadata !{i32 721152, metadata !237, metadata !"t1", metadata !6, i32 279, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!237 = metadata !{i32 720907, metadata !29, i32 278, i32 1, metadata !6, i32 143} ; [ DW_TAG_lexical_block ]
!238 = metadata !{i32 279, i32 10, metadata !237, null}
!239 = metadata !{i32 721152, metadata !237, metadata !"t2", metadata !6, i32 279, metadata !45, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!240 = metadata !{i32 279, i32 14, metadata !237, null}
!241 = metadata !{i32 281, i32 2, metadata !237, null}
!242 = metadata !{i32 282, i32 3, metadata !243, null}
!243 = metadata !{i32 720907, metadata !237, i32 281, i32 20, metadata !6, i32 144} ; [ DW_TAG_lexical_block ]
!244 = metadata !{i32 284, i32 3, metadata !245, null}
!245 = metadata !{i32 720907, metadata !237, i32 283, i32 9, metadata !6, i32 145} ; [ DW_TAG_lexical_block ]
!246 = metadata !{i32 285, i32 3, metadata !245, null}
!247 = metadata !{i32 286, i32 10, metadata !245, null}
!248 = metadata !{i32 288, i32 1, metadata !237, null}
!249 = metadata !{i32 721153, metadata !32, metadata !"p", metadata !6, i32 16777507, metadata !55, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!250 = metadata !{i32 291, i32 29, metadata !32, null}
!251 = metadata !{i32 721153, metadata !32, metadata !"k", metadata !6, i32 33554723, metadata !252, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!252 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !48} ; [ DW_TAG_pointer_type ]
!253 = metadata !{i32 291, i32 40, metadata !32, null}
!254 = metadata !{i32 721152, metadata !255, metadata !"a", metadata !6, i32 294, metadata !192, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!255 = metadata !{i32 720907, metadata !32, i32 293, i32 1, metadata !6, i32 146} ; [ DW_TAG_lexical_block ]
!256 = metadata !{i32 294, i32 10, metadata !255, null}
!257 = metadata !{i32 296, i32 2, metadata !255, null}
!258 = metadata !{i32 297, i32 3, metadata !259, null}
!259 = metadata !{i32 720907, metadata !255, i32 296, i32 15, metadata !6, i32 147} ; [ DW_TAG_lexical_block ]
!260 = metadata !{i32 298, i32 3, metadata !259, null}
!261 = metadata !{i32 299, i32 3, metadata !259, null}
!262 = metadata !{i32 299, i32 27, metadata !259, null}
!263 = metadata !{i32 300, i32 3, metadata !259, null}
!264 = metadata !{i32 301, i32 2, metadata !259, null}
!265 = metadata !{i32 301, i32 9, metadata !255, null}
!266 = metadata !{i32 302, i32 3, metadata !267, null}
!267 = metadata !{i32 720907, metadata !255, i32 301, i32 22, metadata !6, i32 148} ; [ DW_TAG_lexical_block ]
!268 = metadata !{i32 303, i32 2, metadata !267, null}
!269 = metadata !{i32 304, i32 3, metadata !270, null}
!270 = metadata !{i32 720907, metadata !255, i32 303, i32 9, metadata !6, i32 149} ; [ DW_TAG_lexical_block ]
!271 = metadata !{i32 306, i32 1, metadata !255, null}
!272 = metadata !{i32 721153, metadata !35, metadata !"p", metadata !6, i32 16777525, metadata !39, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!273 = metadata !{i32 309, i32 25, metadata !35, null}
!274 = metadata !{i32 721153, metadata !35, metadata !"k", metadata !6, i32 33554741, metadata !187, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!275 = metadata !{i32 309, i32 42, metadata !35, null}
!276 = metadata !{i32 721152, metadata !277, metadata !"yb", metadata !6, i32 312, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!277 = metadata !{i32 720907, metadata !35, i32 311, i32 1, metadata !6, i32 150} ; [ DW_TAG_lexical_block ]
!278 = metadata !{i32 312, i32 6, metadata !277, null}
!279 = metadata !{i32 721152, metadata !277, metadata !"a", metadata !6, i32 313, metadata !192, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!280 = metadata !{i32 313, i32 10, metadata !277, null}
!281 = metadata !{i32 315, i32 2, metadata !277, null}
!282 = metadata !{i32 316, i32 2, metadata !277, null}
!283 = metadata !{i32 317, i32 2, metadata !277, null}
!284 = metadata !{i32 318, i32 2, metadata !277, null}
!285 = metadata !{i32 320, i32 2, metadata !277, null}
!286 = metadata !{i32 321, i32 3, metadata !287, null}
!287 = metadata !{i32 720907, metadata !277, i32 320, i32 21, metadata !6, i32 151} ; [ DW_TAG_lexical_block ]
!288 = metadata !{i32 322, i32 2, metadata !287, null}
!289 = metadata !{i32 323, i32 3, metadata !290, null}
!290 = metadata !{i32 720907, metadata !277, i32 322, i32 9, metadata !6, i32 152} ; [ DW_TAG_lexical_block ]
!291 = metadata !{i32 325, i32 1, metadata !277, null}
