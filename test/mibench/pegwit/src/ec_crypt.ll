; ModuleID = 'ec_crypt.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct.ecPoint = type { [36 x i16], [36 x i16] }
%struct.cpPair.0 = type { [19 x i16], [19 x i16] }

@curve_point = external constant %struct.ecPoint
@prime_order = external constant [19 x i16]

define arm_aapcscc void @cpMakePublicKey(i16* %vlPublicKey, i16* %vlPrivateKey) nounwind uwtable {
entry:
  %vlPublicKey.addr = alloca i16*, align 4
  %vlPrivateKey.addr = alloca i16*, align 4
  %ecPublicKey = alloca %struct.ecPoint, align 2
  store i16* %vlPublicKey, i16** %vlPublicKey.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlPublicKey.addr}, metadata !26), !dbg !30
  store i16* %vlPrivateKey, i16** %vlPrivateKey.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlPrivateKey.addr}, metadata !31), !dbg !34
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint* %ecPublicKey}, metadata !35), !dbg !48
  call arm_aapcscc  void @ecCopy(%struct.ecPoint* %ecPublicKey, %struct.ecPoint* @curve_point), !dbg !49
  %0 = load i16** %vlPrivateKey.addr, align 4, !dbg !50
  call arm_aapcscc  void @ecMultiply(%struct.ecPoint* %ecPublicKey, i16* %0), !dbg !50
  %1 = load i16** %vlPublicKey.addr, align 4, !dbg !51
  call arm_aapcscc  void @ecPack(%struct.ecPoint* %ecPublicKey, i16* %1), !dbg !51
  ret void, !dbg !52
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc void @ecCopy(%struct.ecPoint*, %struct.ecPoint*)

declare arm_aapcscc void @ecMultiply(%struct.ecPoint*, i16*)

declare arm_aapcscc void @ecPack(%struct.ecPoint*, i16*)

define arm_aapcscc void @cpEncodeSecret(i16* %vlPublicKey, i16* %vlMessage, i16* %vlSecret) nounwind uwtable {
entry:
  %vlPublicKey.addr = alloca i16*, align 4
  %vlMessage.addr = alloca i16*, align 4
  %vlSecret.addr = alloca i16*, align 4
  %q = alloca %struct.ecPoint, align 2
  store i16* %vlPublicKey, i16** %vlPublicKey.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlPublicKey.addr}, metadata !53), !dbg !54
  store i16* %vlMessage, i16** %vlMessage.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlMessage.addr}, metadata !55), !dbg !56
  store i16* %vlSecret, i16** %vlSecret.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlSecret.addr}, metadata !57), !dbg !58
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint* %q}, metadata !59), !dbg !61
  call arm_aapcscc  void @ecCopy(%struct.ecPoint* %q, %struct.ecPoint* @curve_point), !dbg !62
  %0 = load i16** %vlSecret.addr, align 4, !dbg !63
  call arm_aapcscc  void @ecMultiply(%struct.ecPoint* %q, i16* %0), !dbg !63
  %1 = load i16** %vlMessage.addr, align 4, !dbg !64
  call arm_aapcscc  void @ecPack(%struct.ecPoint* %q, i16* %1), !dbg !64
  %2 = load i16** %vlPublicKey.addr, align 4, !dbg !65
  call arm_aapcscc  void @ecUnpack(%struct.ecPoint* %q, i16* %2), !dbg !65
  %3 = load i16** %vlSecret.addr, align 4, !dbg !66
  call arm_aapcscc  void @ecMultiply(%struct.ecPoint* %q, i16* %3), !dbg !66
  %x = getelementptr inbounds %struct.ecPoint* %q, i32 0, i32 0, !dbg !67
  %arraydecay = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !67
  %4 = load i16** %vlSecret.addr, align 4, !dbg !67
  call arm_aapcscc  void @gfPack(i16* %arraydecay, i16* %4), !dbg !67
  ret void, !dbg !68
}

declare arm_aapcscc void @ecUnpack(%struct.ecPoint*, i16*)

declare arm_aapcscc void @gfPack(i16*, i16*)

define arm_aapcscc void @cpDecodeSecret(i16* %vlPrivateKey, i16* %vlMessage, i16* %d) nounwind uwtable {
entry:
  %vlPrivateKey.addr = alloca i16*, align 4
  %vlMessage.addr = alloca i16*, align 4
  %d.addr = alloca i16*, align 4
  %q = alloca %struct.ecPoint, align 2
  store i16* %vlPrivateKey, i16** %vlPrivateKey.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlPrivateKey.addr}, metadata !69), !dbg !70
  store i16* %vlMessage, i16** %vlMessage.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlMessage.addr}, metadata !71), !dbg !72
  store i16* %d, i16** %d.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %d.addr}, metadata !73), !dbg !74
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint* %q}, metadata !75), !dbg !77
  %0 = load i16** %vlMessage.addr, align 4, !dbg !78
  call arm_aapcscc  void @ecUnpack(%struct.ecPoint* %q, i16* %0), !dbg !78
  %1 = load i16** %vlPrivateKey.addr, align 4, !dbg !79
  call arm_aapcscc  void @ecMultiply(%struct.ecPoint* %q, i16* %1), !dbg !79
  %x = getelementptr inbounds %struct.ecPoint* %q, i32 0, i32 0, !dbg !80
  %arraydecay = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !80
  %2 = load i16** %d.addr, align 4, !dbg !80
  call arm_aapcscc  void @gfPack(i16* %arraydecay, i16* %2), !dbg !80
  ret void, !dbg !81
}

define arm_aapcscc void @cpSign(i16* %vlPrivateKey, i16* %k, i16* %vlMac, %struct.cpPair.0* %sig) nounwind uwtable {
entry:
  %vlPrivateKey.addr = alloca i16*, align 4
  %k.addr = alloca i16*, align 4
  %vlMac.addr = alloca i16*, align 4
  %sig.addr = alloca %struct.cpPair.0*, align 4
  %q = alloca %struct.ecPoint, align 2
  %tmp = alloca [19 x i16], align 2
  store i16* %vlPrivateKey, i16** %vlPrivateKey.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlPrivateKey.addr}, metadata !82), !dbg !83
  store i16* %k, i16** %k.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %k.addr}, metadata !84), !dbg !85
  store i16* %vlMac, i16** %vlMac.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlMac.addr}, metadata !86), !dbg !87
  store %struct.cpPair.0* %sig, %struct.cpPair.0** %sig.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.cpPair.0** %sig.addr}, metadata !88), !dbg !100
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint* %q}, metadata !101), !dbg !103
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %tmp}, metadata !104), !dbg !105
  call arm_aapcscc  void @ecCopy(%struct.ecPoint* %q, %struct.ecPoint* @curve_point), !dbg !106
  %0 = load i16** %k.addr, align 4, !dbg !107
  call arm_aapcscc  void @ecMultiply(%struct.ecPoint* %q, i16* %0), !dbg !107
  %x = getelementptr inbounds %struct.ecPoint* %q, i32 0, i32 0, !dbg !108
  %arraydecay = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !108
  %1 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !108
  %r = getelementptr inbounds %struct.cpPair.0* %1, i32 0, i32 0, !dbg !108
  %arraydecay1 = getelementptr inbounds [19 x i16]* %r, i32 0, i32 0, !dbg !108
  call arm_aapcscc  void @gfPack(i16* %arraydecay, i16* %arraydecay1), !dbg !108
  %2 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !109
  %r2 = getelementptr inbounds %struct.cpPair.0* %2, i32 0, i32 0, !dbg !109
  %arraydecay3 = getelementptr inbounds [19 x i16]* %r2, i32 0, i32 0, !dbg !109
  %3 = load i16** %vlMac.addr, align 4, !dbg !109
  call arm_aapcscc  void @vlAdd(i16* %arraydecay3, i16* %3), !dbg !109
  %4 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !110
  %r4 = getelementptr inbounds %struct.cpPair.0* %4, i32 0, i32 0, !dbg !110
  %arraydecay5 = getelementptr inbounds [19 x i16]* %r4, i32 0, i32 0, !dbg !110
  call arm_aapcscc  void @vlRemainder(i16* %arraydecay5, i16* getelementptr inbounds ([19 x i16]* @prime_order, i32 0, i32 0)), !dbg !110
  %5 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !111
  %r6 = getelementptr inbounds %struct.cpPair.0* %5, i32 0, i32 0, !dbg !111
  %arrayidx = getelementptr inbounds [19 x i16]* %r6, i32 0, i32 0, !dbg !111
  %6 = load i16* %arrayidx, align 2, !dbg !111
  %conv = zext i16 %6 to i32, !dbg !111
  %cmp = icmp eq i32 %conv, 0, !dbg !111
  br i1 %cmp, label %if.then, label %if.end, !dbg !111

if.then:                                          ; preds = %entry
  br label %return, !dbg !112

if.end:                                           ; preds = %entry
  %arraydecay8 = getelementptr inbounds [19 x i16]* %tmp, i32 0, i32 0, !dbg !113
  %7 = load i16** %vlPrivateKey.addr, align 4, !dbg !113
  %8 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !113
  %r9 = getelementptr inbounds %struct.cpPair.0* %8, i32 0, i32 0, !dbg !113
  %arraydecay10 = getelementptr inbounds [19 x i16]* %r9, i32 0, i32 0, !dbg !113
  call arm_aapcscc  void @vlMulMod(i16* %arraydecay8, i16* %7, i16* %arraydecay10, i16* getelementptr inbounds ([19 x i16]* @prime_order, i32 0, i32 0)), !dbg !113
  %9 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !114
  %s = getelementptr inbounds %struct.cpPair.0* %9, i32 0, i32 1, !dbg !114
  %arraydecay11 = getelementptr inbounds [19 x i16]* %s, i32 0, i32 0, !dbg !114
  %10 = load i16** %k.addr, align 4, !dbg !114
  call arm_aapcscc  void @vlCopy(i16* %arraydecay11, i16* %10), !dbg !114
  %arraydecay12 = getelementptr inbounds [19 x i16]* %tmp, i32 0, i32 0, !dbg !115
  %11 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !115
  %s13 = getelementptr inbounds %struct.cpPair.0* %11, i32 0, i32 1, !dbg !115
  %arraydecay14 = getelementptr inbounds [19 x i16]* %s13, i32 0, i32 0, !dbg !115
  %call = call arm_aapcscc  i32 @vlGreater(i16* %arraydecay12, i16* %arraydecay14), !dbg !115
  %tobool = icmp ne i32 %call, 0, !dbg !115
  br i1 %tobool, label %if.then15, label %if.end18, !dbg !115

if.then15:                                        ; preds = %if.end
  %12 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !116
  %s16 = getelementptr inbounds %struct.cpPair.0* %12, i32 0, i32 1, !dbg !116
  %arraydecay17 = getelementptr inbounds [19 x i16]* %s16, i32 0, i32 0, !dbg !116
  call arm_aapcscc  void @vlAdd(i16* %arraydecay17, i16* getelementptr inbounds ([19 x i16]* @prime_order, i32 0, i32 0)), !dbg !116
  br label %if.end18, !dbg !116

if.end18:                                         ; preds = %if.then15, %if.end
  %13 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !117
  %s19 = getelementptr inbounds %struct.cpPair.0* %13, i32 0, i32 1, !dbg !117
  %arraydecay20 = getelementptr inbounds [19 x i16]* %s19, i32 0, i32 0, !dbg !117
  %arraydecay21 = getelementptr inbounds [19 x i16]* %tmp, i32 0, i32 0, !dbg !117
  call arm_aapcscc  void @vlSubtract(i16* %arraydecay20, i16* %arraydecay21), !dbg !117
  br label %return, !dbg !118

return:                                           ; preds = %if.end18, %if.then
  ret void, !dbg !118
}

declare arm_aapcscc void @vlAdd(i16*, i16*)

declare arm_aapcscc void @vlRemainder(i16*, i16*)

declare arm_aapcscc void @vlMulMod(i16*, i16*, i16*, i16*)

declare arm_aapcscc void @vlCopy(i16*, i16*)

declare arm_aapcscc i32 @vlGreater(i16*, i16*)

declare arm_aapcscc void @vlSubtract(i16*, i16*)

define arm_aapcscc i32 @cpVerify(i16* %vlPublicKey, i16* %vlMac, %struct.cpPair.0* %sig) nounwind uwtable {
entry:
  %vlPublicKey.addr = alloca i16*, align 4
  %vlMac.addr = alloca i16*, align 4
  %sig.addr = alloca %struct.cpPair.0*, align 4
  %t1 = alloca %struct.ecPoint, align 2
  %t2 = alloca %struct.ecPoint, align 2
  %t3 = alloca [19 x i16], align 2
  %t4 = alloca [19 x i16], align 2
  store i16* %vlPublicKey, i16** %vlPublicKey.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlPublicKey.addr}, metadata !119), !dbg !120
  store i16* %vlMac, i16** %vlMac.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %vlMac.addr}, metadata !121), !dbg !122
  store %struct.cpPair.0* %sig, %struct.cpPair.0** %sig.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.cpPair.0** %sig.addr}, metadata !123), !dbg !124
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint* %t1}, metadata !125), !dbg !127
  call void @llvm.dbg.declare(metadata !{%struct.ecPoint* %t2}, metadata !128), !dbg !129
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %t3}, metadata !130), !dbg !131
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %t4}, metadata !132), !dbg !133
  call arm_aapcscc  void @ecCopy(%struct.ecPoint* %t1, %struct.ecPoint* @curve_point), !dbg !134
  %0 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !135
  %s = getelementptr inbounds %struct.cpPair.0* %0, i32 0, i32 1, !dbg !135
  %arraydecay = getelementptr inbounds [19 x i16]* %s, i32 0, i32 0, !dbg !135
  call arm_aapcscc  void @ecMultiply(%struct.ecPoint* %t1, i16* %arraydecay), !dbg !135
  %1 = load i16** %vlPublicKey.addr, align 4, !dbg !136
  call arm_aapcscc  void @ecUnpack(%struct.ecPoint* %t2, i16* %1), !dbg !136
  %2 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !137
  %r = getelementptr inbounds %struct.cpPair.0* %2, i32 0, i32 0, !dbg !137
  %arraydecay1 = getelementptr inbounds [19 x i16]* %r, i32 0, i32 0, !dbg !137
  call arm_aapcscc  void @ecMultiply(%struct.ecPoint* %t2, i16* %arraydecay1), !dbg !137
  call arm_aapcscc  void @ecAdd(%struct.ecPoint* %t1, %struct.ecPoint* %t2), !dbg !138
  %x = getelementptr inbounds %struct.ecPoint* %t1, i32 0, i32 0, !dbg !139
  %arraydecay2 = getelementptr inbounds [36 x i16]* %x, i32 0, i32 0, !dbg !139
  %arraydecay3 = getelementptr inbounds [19 x i16]* %t4, i32 0, i32 0, !dbg !139
  call arm_aapcscc  void @gfPack(i16* %arraydecay2, i16* %arraydecay3), !dbg !139
  %arraydecay4 = getelementptr inbounds [19 x i16]* %t4, i32 0, i32 0, !dbg !140
  call arm_aapcscc  void @vlRemainder(i16* %arraydecay4, i16* getelementptr inbounds ([19 x i16]* @prime_order, i32 0, i32 0)), !dbg !140
  %arraydecay5 = getelementptr inbounds [19 x i16]* %t3, i32 0, i32 0, !dbg !141
  %3 = load %struct.cpPair.0** %sig.addr, align 4, !dbg !141
  %r6 = getelementptr inbounds %struct.cpPair.0* %3, i32 0, i32 0, !dbg !141
  %arraydecay7 = getelementptr inbounds [19 x i16]* %r6, i32 0, i32 0, !dbg !141
  call arm_aapcscc  void @vlCopy(i16* %arraydecay5, i16* %arraydecay7), !dbg !141
  %arraydecay8 = getelementptr inbounds [19 x i16]* %t4, i32 0, i32 0, !dbg !142
  %arraydecay9 = getelementptr inbounds [19 x i16]* %t3, i32 0, i32 0, !dbg !142
  %call = call arm_aapcscc  i32 @vlGreater(i16* %arraydecay8, i16* %arraydecay9), !dbg !142
  %tobool = icmp ne i32 %call, 0, !dbg !142
  br i1 %tobool, label %if.then, label %if.end, !dbg !142

if.then:                                          ; preds = %entry
  %arraydecay10 = getelementptr inbounds [19 x i16]* %t3, i32 0, i32 0, !dbg !143
  call arm_aapcscc  void @vlAdd(i16* %arraydecay10, i16* getelementptr inbounds ([19 x i16]* @prime_order, i32 0, i32 0)), !dbg !143
  br label %if.end, !dbg !143

if.end:                                           ; preds = %if.then, %entry
  %arraydecay11 = getelementptr inbounds [19 x i16]* %t3, i32 0, i32 0, !dbg !144
  %arraydecay12 = getelementptr inbounds [19 x i16]* %t4, i32 0, i32 0, !dbg !144
  call arm_aapcscc  void @vlSubtract(i16* %arraydecay11, i16* %arraydecay12), !dbg !144
  %arraydecay13 = getelementptr inbounds [19 x i16]* %t3, i32 0, i32 0, !dbg !145
  %4 = load i16** %vlMac.addr, align 4, !dbg !145
  %call14 = call arm_aapcscc  i32 @vlEqual(i16* %arraydecay13, i16* %4), !dbg !145
  ret i32 %call14, !dbg !145
}

declare arm_aapcscc void @ecAdd(%struct.ecPoint*, %struct.ecPoint*)

declare arm_aapcscc i32 @vlEqual(i16*, i16*)

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !11, metadata !14, metadata !17, metadata !20}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"cpMakePublicKey", metadata !"cpMakePublicKey", metadata !"", metadata !6, i32 26, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @cpMakePublicKey, null, null, metadata !9} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"ec_crypt.c", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{null}
!9 = metadata !{metadata !10}
!10 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!11 = metadata !{i32 720942, i32 0, metadata !6, metadata !"cpEncodeSecret", metadata !"cpEncodeSecret", metadata !"", metadata !6, i32 36, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*, i16*)* @cpEncodeSecret, null, null, metadata !12} ; [ DW_TAG_subprogram ]
!12 = metadata !{metadata !13}
!13 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!14 = metadata !{i32 720942, i32 0, metadata !6, metadata !"cpDecodeSecret", metadata !"cpDecodeSecret", metadata !"", metadata !6, i32 45, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*, i16*)* @cpDecodeSecret, null, null, metadata !15} ; [ DW_TAG_subprogram ]
!15 = metadata !{metadata !16}
!16 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!17 = metadata !{i32 720942, i32 0, metadata !6, metadata !"cpSign", metadata !"cpSign", metadata !"", metadata !6, i32 54, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*, i16*, %struct.cpPair.0*)* @cpSign, null, null, metadata !18} ; [ DW_TAG_subprogram ]
!18 = metadata !{metadata !19}
!19 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!20 = metadata !{i32 720942, i32 0, metadata !6, metadata !"cpVerify", metadata !"cpVerify", metadata !"", metadata !6, i32 72, metadata !21, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, i16*, %struct.cpPair.0*)* @cpVerify, null, null, metadata !24} ; [ DW_TAG_subprogram ]
!21 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !22, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!22 = metadata !{metadata !23}
!23 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!24 = metadata !{metadata !25}
!25 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!26 = metadata !{i32 721153, metadata !5, metadata !"vlPublicKey", metadata !6, i32 16777241, metadata !27, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!27 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !28} ; [ DW_TAG_pointer_type ]
!28 = metadata !{i32 720918, null, metadata !"word16", metadata !6, i32 11, i64 0, i64 0, i64 0, i32 0, metadata !29} ; [ DW_TAG_typedef ]
!29 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!30 = metadata !{i32 25, i32 31, metadata !5, null}
!31 = metadata !{i32 721153, metadata !5, metadata !"vlPrivateKey", metadata !6, i32 33554457, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!32 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !33} ; [ DW_TAG_pointer_type ]
!33 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !28} ; [ DW_TAG_const_type ]
!34 = metadata !{i32 25, i32 58, metadata !5, null}
!35 = metadata !{i32 721152, metadata !36, metadata !"ecPublicKey", metadata !6, i32 27, metadata !37, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!36 = metadata !{i32 720907, metadata !5, i32 26, i32 1, metadata !6, i32 116} ; [ DW_TAG_lexical_block ]
!37 = metadata !{i32 720918, null, metadata !"ecPoint", metadata !6, i32 18, i64 0, i64 0, i64 0, i32 0, metadata !38} ; [ DW_TAG_typedef ]
!38 = metadata !{i32 720915, null, metadata !"", metadata !39, i32 16, i64 1152, i64 16, i32 0, i32 0, null, metadata !40, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!39 = metadata !{i32 720937, metadata !"ec_curve.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!40 = metadata !{metadata !41, metadata !47}
!41 = metadata !{i32 720909, metadata !38, metadata !"x", metadata !39, i32 17, i64 576, i64 16, i64 0, i32 0, metadata !42} ; [ DW_TAG_member ]
!42 = metadata !{i32 720918, null, metadata !"gfPoint", metadata !39, i32 36, i64 0, i64 0, i64 0, i32 0, metadata !43} ; [ DW_TAG_typedef ]
!43 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 576, i64 16, i32 0, i32 0, metadata !44, metadata !45, i32 0, i32 0} ; [ DW_TAG_array_type ]
!44 = metadata !{i32 720918, null, metadata !"lunit", metadata !39, i32 27, i64 0, i64 0, i64 0, i32 0, metadata !28} ; [ DW_TAG_typedef ]
!45 = metadata !{metadata !46}
!46 = metadata !{i32 720929, i64 0, i64 35}       ; [ DW_TAG_subrange_type ]
!47 = metadata !{i32 720909, metadata !38, metadata !"y", metadata !39, i32 17, i64 576, i64 16, i64 576, i32 0, metadata !42} ; [ DW_TAG_member ]
!48 = metadata !{i32 27, i32 10, metadata !36, null}
!49 = metadata !{i32 29, i32 2, metadata !36, null}
!50 = metadata !{i32 30, i32 2, metadata !36, null}
!51 = metadata !{i32 31, i32 2, metadata !36, null}
!52 = metadata !{i32 32, i32 1, metadata !36, null}
!53 = metadata !{i32 721153, metadata !11, metadata !"vlPublicKey", metadata !6, i32 16777251, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!54 = metadata !{i32 35, i32 36, metadata !11, null}
!55 = metadata !{i32 721153, metadata !11, metadata !"vlMessage", metadata !6, i32 33554467, metadata !27, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!56 = metadata !{i32 35, i32 57, metadata !11, null}
!57 = metadata !{i32 721153, metadata !11, metadata !"vlSecret", metadata !6, i32 50331683, metadata !27, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!58 = metadata !{i32 35, i32 76, metadata !11, null}
!59 = metadata !{i32 721152, metadata !60, metadata !"q", metadata !6, i32 37, metadata !37, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!60 = metadata !{i32 720907, metadata !11, i32 36, i32 1, metadata !6, i32 117} ; [ DW_TAG_lexical_block ]
!61 = metadata !{i32 37, i32 10, metadata !60, null}
!62 = metadata !{i32 39, i32 2, metadata !60, null}
!63 = metadata !{i32 39, i32 29, metadata !60, null}
!64 = metadata !{i32 39, i32 56, metadata !60, null}
!65 = metadata !{i32 40, i32 2, metadata !60, null}
!66 = metadata !{i32 40, i32 30, metadata !60, null}
!67 = metadata !{i32 40, i32 57, metadata !60, null}
!68 = metadata !{i32 41, i32 1, metadata !60, null}
!69 = metadata !{i32 721153, metadata !14, metadata !"vlPrivateKey", metadata !6, i32 16777260, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!70 = metadata !{i32 44, i32 36, metadata !14, null}
!71 = metadata !{i32 721153, metadata !14, metadata !"vlMessage", metadata !6, i32 33554476, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!72 = metadata !{i32 44, i32 64, metadata !14, null}
!73 = metadata !{i32 721153, metadata !14, metadata !"d", metadata !6, i32 50331692, metadata !27, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!74 = metadata !{i32 44, i32 83, metadata !14, null}
!75 = metadata !{i32 721152, metadata !76, metadata !"q", metadata !6, i32 46, metadata !37, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!76 = metadata !{i32 720907, metadata !14, i32 45, i32 1, metadata !6, i32 118} ; [ DW_TAG_lexical_block ]
!77 = metadata !{i32 46, i32 10, metadata !76, null}
!78 = metadata !{i32 48, i32 3, metadata !76, null}
!79 = metadata !{i32 49, i32 2, metadata !76, null}
!80 = metadata !{i32 50, i32 2, metadata !76, null}
!81 = metadata !{i32 51, i32 1, metadata !76, null}
!82 = metadata !{i32 721153, metadata !17, metadata !"vlPrivateKey", metadata !6, i32 16777269, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!83 = metadata !{i32 53, i32 27, metadata !17, null}
!84 = metadata !{i32 721153, metadata !17, metadata !"k", metadata !6, i32 33554485, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!85 = metadata !{i32 53, i32 55, metadata !17, null}
!86 = metadata !{i32 721153, metadata !17, metadata !"vlMac", metadata !6, i32 50331701, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!87 = metadata !{i32 53, i32 72, metadata !17, null}
!88 = metadata !{i32 721153, metadata !17, metadata !"sig", metadata !6, i32 67108917, metadata !89, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!89 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !90} ; [ DW_TAG_pointer_type ]
!90 = metadata !{i32 720918, null, metadata !"cpPair", metadata !6, i32 9, i64 0, i64 0, i64 0, i32 0, metadata !91} ; [ DW_TAG_typedef ]
!91 = metadata !{i32 720915, null, metadata !"", metadata !92, i32 7, i64 608, i64 16, i32 0, i32 0, null, metadata !93, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!92 = metadata !{i32 720937, metadata !"ec_crypt.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!93 = metadata !{metadata !94, metadata !99}
!94 = metadata !{i32 720909, metadata !91, metadata !"r", metadata !92, i32 8, i64 304, i64 16, i64 0, i32 0, metadata !95} ; [ DW_TAG_member ]
!95 = metadata !{i32 720918, null, metadata !"vlPoint", metadata !92, i32 17, i64 0, i64 0, i64 0, i32 0, metadata !96} ; [ DW_TAG_typedef ]
!96 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 304, i64 16, i32 0, i32 0, metadata !28, metadata !97, i32 0, i32 0} ; [ DW_TAG_array_type ]
!97 = metadata !{metadata !98}
!98 = metadata !{i32 720929, i64 0, i64 18}       ; [ DW_TAG_subrange_type ]
!99 = metadata !{i32 720909, metadata !91, metadata !"s", metadata !92, i32 8, i64 304, i64 16, i64 304, i32 0, metadata !95} ; [ DW_TAG_member ]
!100 = metadata !{i32 53, i32 88, metadata !17, null}
!101 = metadata !{i32 721152, metadata !102, metadata !"q", metadata !6, i32 55, metadata !37, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!102 = metadata !{i32 720907, metadata !17, i32 54, i32 1, metadata !6, i32 119} ; [ DW_TAG_lexical_block ]
!103 = metadata !{i32 55, i32 10, metadata !102, null}
!104 = metadata !{i32 721152, metadata !102, metadata !"tmp", metadata !6, i32 56, metadata !95, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!105 = metadata !{i32 56, i32 10, metadata !102, null}
!106 = metadata !{i32 58, i32 2, metadata !102, null}
!107 = metadata !{i32 59, i32 2, metadata !102, null}
!108 = metadata !{i32 60, i32 2, metadata !102, null}
!109 = metadata !{i32 61, i32 2, metadata !102, null}
!110 = metadata !{i32 62, i32 2, metadata !102, null}
!111 = metadata !{i32 63, i32 2, metadata !102, null}
!112 = metadata !{i32 63, i32 24, metadata !102, null}
!113 = metadata !{i32 64, i32 2, metadata !102, null}
!114 = metadata !{i32 65, i32 2, metadata !102, null}
!115 = metadata !{i32 66, i32 7, metadata !102, null}
!116 = metadata !{i32 67, i32 3, metadata !102, null}
!117 = metadata !{i32 68, i32 2, metadata !102, null}
!118 = metadata !{i32 69, i32 1, metadata !102, null}
!119 = metadata !{i32 721153, metadata !20, metadata !"vlPublicKey", metadata !6, i32 16777287, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!120 = metadata !{i32 71, i32 28, metadata !20, null}
!121 = metadata !{i32 721153, metadata !20, metadata !"vlMac", metadata !6, i32 33554503, metadata !32, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!122 = metadata !{i32 71, i32 55, metadata !20, null}
!123 = metadata !{i32 721153, metadata !20, metadata !"sig", metadata !6, i32 50331719, metadata !89, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!124 = metadata !{i32 71, i32 71, metadata !20, null}
!125 = metadata !{i32 721152, metadata !126, metadata !"t1", metadata !6, i32 73, metadata !37, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!126 = metadata !{i32 720907, metadata !20, i32 72, i32 1, metadata !6, i32 120} ; [ DW_TAG_lexical_block ]
!127 = metadata !{i32 73, i32 10, metadata !126, null}
!128 = metadata !{i32 721152, metadata !126, metadata !"t2", metadata !6, i32 73, metadata !37, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!129 = metadata !{i32 73, i32 13, metadata !126, null}
!130 = metadata !{i32 721152, metadata !126, metadata !"t3", metadata !6, i32 74, metadata !95, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!131 = metadata !{i32 74, i32 10, metadata !126, null}
!132 = metadata !{i32 721152, metadata !126, metadata !"t4", metadata !6, i32 74, metadata !95, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!133 = metadata !{i32 74, i32 13, metadata !126, null}
!134 = metadata !{i32 76, i32 2, metadata !126, null}
!135 = metadata !{i32 77, i32 2, metadata !126, null}
!136 = metadata !{i32 78, i32 2, metadata !126, null}
!137 = metadata !{i32 79, i32 2, metadata !126, null}
!138 = metadata !{i32 80, i32 2, metadata !126, null}
!139 = metadata !{i32 81, i32 2, metadata !126, null}
!140 = metadata !{i32 82, i32 2, metadata !126, null}
!141 = metadata !{i32 83, i32 2, metadata !126, null}
!142 = metadata !{i32 84, i32 7, metadata !126, null}
!143 = metadata !{i32 85, i32 3, metadata !126, null}
!144 = metadata !{i32 86, i32 2, metadata !126, null}
!145 = metadata !{i32 87, i32 9, metadata !126, null}
