; ModuleID = 'ec_vlong.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

@.str = private unnamed_addr constant [17 x i8] c"p != ((void *)0)\00", align 1
@.str1 = private unnamed_addr constant [11 x i8] c"ec_vlong.c\00", align 1
@__PRETTY_FUNCTION__.vlEqual = private unnamed_addr constant [44 x i8] c"int vlEqual(const word16 *, const word16 *)\00", align 1
@.str2 = private unnamed_addr constant [17 x i8] c"q != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.vlClear = private unnamed_addr constant [23 x i8] c"void vlClear(word16 *)\00", align 1
@__PRETTY_FUNCTION__.vlCopy = private unnamed_addr constant [38 x i8] c"void vlCopy(word16 *, const word16 *)\00", align 1
@__PRETTY_FUNCTION__.vlShortSet = private unnamed_addr constant [34 x i8] c"void vlShortSet(word16 *, word16)\00", align 1
@.str3 = private unnamed_addr constant [17 x i8] c"k != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.vlNumBits = private unnamed_addr constant [30 x i8] c"int vlNumBits(const word16 *)\00", align 1
@__PRETTY_FUNCTION__.vlTakeBit = private unnamed_addr constant [38 x i8] c"int vlTakeBit(const word16 *, word16)\00", align 1
@.str4 = private unnamed_addr constant [17 x i8] c"u != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.vlAdd = private unnamed_addr constant [37 x i8] c"void vlAdd(word16 *, const word16 *)\00", align 1
@.str5 = private unnamed_addr constant [17 x i8] c"v != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.vlSubtract = private unnamed_addr constant [42 x i8] c"void vlSubtract(word16 *, const word16 *)\00", align 1
@__PRETTY_FUNCTION__.vlShortLshift = private unnamed_addr constant [34 x i8] c"void vlShortLshift(word16 *, int)\00", align 1
@__PRETTY_FUNCTION__.vlShortRshift = private unnamed_addr constant [34 x i8] c"void vlShortRshift(word16 *, int)\00", align 1
@__PRETTY_FUNCTION__.vlShortMultiply = private unnamed_addr constant [54 x i8] c"int vlShortMultiply(word16 *, const word16 *, word16)\00", align 1
@.str6 = private unnamed_addr constant [43 x i8] c"ERROR: not enough room for multiplication\0A\00", align 1
@__PRETTY_FUNCTION__.vlGreater = private unnamed_addr constant [46 x i8] c"int vlGreater(const word16 *, const word16 *)\00", align 1
@__PRETTY_FUNCTION__.vlRemainder = private unnamed_addr constant [43 x i8] c"void vlRemainder(word16 *, const word16 *)\00", align 1
@.str7 = private unnamed_addr constant [10 x i8] c"v[0] != 0\00", align 1
@__PRETTY_FUNCTION__.vlMulMod = private unnamed_addr constant [72 x i8] c"void vlMulMod(word16 *, const word16 *, const word16 *, const word16 *)\00", align 1
@.str8 = private unnamed_addr constant [17 x i8] c"w != ((void *)0)\00", align 1
@.str9 = private unnamed_addr constant [17 x i8] c"m != ((void *)0)\00", align 1
@.str10 = private unnamed_addr constant [10 x i8] c"m[0] != 0\00", align 1

define arm_aapcscc i32 @vlEqual(i16* %p, i16* %q) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %q.addr = alloca i16*, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !53), !dbg !58
  store i16* %q, i16** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q.addr}, metadata !59), !dbg !60
  %0 = load i16** %p.addr, align 4, !dbg !61
  %cmp = icmp ne i16* %0, null, !dbg !61
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !61

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !61

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 80, i8* getelementptr inbounds ([44 x i8]* @__PRETTY_FUNCTION__.vlEqual, i32 0, i32 0)) noreturn nounwind, !dbg !63
  unreachable, !dbg !63
                                                  ; No predecessors!
  br label %cond.end, !dbg !63

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %q.addr, align 4, !dbg !64
  %cmp1 = icmp ne i16* %2, null, !dbg !64
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !64

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !64

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 81, i8* getelementptr inbounds ([44 x i8]* @__PRETTY_FUNCTION__.vlEqual, i32 0, i32 0)) noreturn nounwind, !dbg !65
  unreachable, !dbg !65
                                                  ; No predecessors!
  br label %cond.end4, !dbg !65

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %p.addr, align 4, !dbg !66
  %5 = bitcast i16* %4 to i8*, !dbg !66
  %6 = load i16** %q.addr, align 4, !dbg !66
  %7 = bitcast i16* %6 to i8*, !dbg !66
  %8 = load i16** %p.addr, align 4, !dbg !66
  %arrayidx = getelementptr inbounds i16* %8, i32 0, !dbg !66
  %9 = load i16* %arrayidx, !dbg !66
  %conv = zext i16 %9 to i32, !dbg !66
  %add = add nsw i32 %conv, 1, !dbg !66
  %mul = mul i32 %add, 2, !dbg !66
  %call = call arm_aapcscc  i32 @memcmp(i8* %5, i8* %7, i32 %mul) nounwind readonly, !dbg !66
  %cmp5 = icmp eq i32 %call, 0, !dbg !66
  %cond = select i1 %cmp5, i32 1, i32 0, !dbg !66
  ret i32 %cond, !dbg !66
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc void @__assert_fail(i8*, i8*, i32, i8*) noreturn nounwind

declare arm_aapcscc i32 @memcmp(i8*, i8*, i32) nounwind readonly

define arm_aapcscc void @vlClear(i16* %p) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !67), !dbg !69
  %0 = load i16** %p.addr, align 4, !dbg !70
  %cmp = icmp ne i16* %0, null, !dbg !70
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !70

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !70

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 88, i8* getelementptr inbounds ([23 x i8]* @__PRETTY_FUNCTION__.vlClear, i32 0, i32 0)) noreturn nounwind, !dbg !72
  unreachable, !dbg !72
                                                  ; No predecessors!
  br label %cond.end, !dbg !72

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %p.addr, align 4, !dbg !73
  %3 = bitcast i16* %2 to i8*, !dbg !73
  call void @llvm.memset.p0i8.i32(i8* %3, i8 0, i32 38, i32 1, i1 false), !dbg !73
  ret void, !dbg !74
}

declare void @llvm.memset.p0i8.i32(i8* nocapture, i8, i32, i32, i1) nounwind

define arm_aapcscc void @vlCopy(i16* %p, i16* %q) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %q.addr = alloca i16*, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !75), !dbg !76
  store i16* %q, i16** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q.addr}, metadata !77), !dbg !78
  %0 = load i16** %p.addr, align 4, !dbg !79
  %cmp = icmp ne i16* %0, null, !dbg !79
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !79

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !79

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 96, i8* getelementptr inbounds ([38 x i8]* @__PRETTY_FUNCTION__.vlCopy, i32 0, i32 0)) noreturn nounwind, !dbg !81
  unreachable, !dbg !81
                                                  ; No predecessors!
  br label %cond.end, !dbg !81

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %q.addr, align 4, !dbg !82
  %cmp1 = icmp ne i16* %2, null, !dbg !82
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !82

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !82

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 97, i8* getelementptr inbounds ([38 x i8]* @__PRETTY_FUNCTION__.vlCopy, i32 0, i32 0)) noreturn nounwind, !dbg !83
  unreachable, !dbg !83
                                                  ; No predecessors!
  br label %cond.end4, !dbg !83

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %p.addr, align 4, !dbg !84
  %5 = bitcast i16* %4 to i8*, !dbg !84
  %6 = load i16** %q.addr, align 4, !dbg !84
  %7 = bitcast i16* %6 to i8*, !dbg !84
  %8 = load i16** %q.addr, align 4, !dbg !84
  %arrayidx = getelementptr inbounds i16* %8, i32 0, !dbg !84
  %9 = load i16* %arrayidx, !dbg !84
  %conv = zext i16 %9 to i32, !dbg !84
  %add = add nsw i32 %conv, 1, !dbg !84
  %mul = mul i32 %add, 2, !dbg !84
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %5, i8* %7, i32 %mul, i32 1, i1 false), !dbg !84
  ret void, !dbg !85
}

declare void @llvm.memcpy.p0i8.p0i8.i32(i8* nocapture, i8* nocapture, i32, i32, i1) nounwind

define arm_aapcscc void @vlShortSet(i16* %p, i16 zeroext %u) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %u.addr = alloca i16, align 2
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !86), !dbg !87
  store i16 %u, i16* %u.addr, align 2
  call void @llvm.dbg.declare(metadata !{i16* %u.addr}, metadata !88), !dbg !89
  %0 = load i16** %p.addr, align 4, !dbg !90
  %cmp = icmp ne i16* %0, null, !dbg !90
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !90

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !90

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 105, i8* getelementptr inbounds ([34 x i8]* @__PRETTY_FUNCTION__.vlShortSet, i32 0, i32 0)) noreturn nounwind, !dbg !92
  unreachable, !dbg !92
                                                  ; No predecessors!
  br label %cond.end, !dbg !92

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %p.addr, align 4, !dbg !93
  %arrayidx = getelementptr inbounds i16* %2, i32 0, !dbg !93
  store i16 1, i16* %arrayidx, !dbg !93
  %3 = load i16* %u.addr, align 2, !dbg !94
  %4 = load i16** %p.addr, align 4, !dbg !94
  %arrayidx1 = getelementptr inbounds i16* %4, i32 1, !dbg !94
  store i16 %3, i16* %arrayidx1, !dbg !94
  ret void, !dbg !95
}

define arm_aapcscc i32 @vlNumBits(i16* %k) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %k.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  %m = alloca i16, align 2
  %w = alloca i16, align 2
  store i16* %k, i16** %k.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %k.addr}, metadata !96), !dbg !97
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !98), !dbg !100
  call void @llvm.dbg.declare(metadata !{i16* %m}, metadata !101), !dbg !102
  call void @llvm.dbg.declare(metadata !{i16* %w}, metadata !103), !dbg !104
  %0 = load i16** %k.addr, align 4, !dbg !105
  %cmp = icmp ne i16* %0, null, !dbg !105
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !105

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !105

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 116, i8* getelementptr inbounds ([30 x i8]* @__PRETTY_FUNCTION__.vlNumBits, i32 0, i32 0)) noreturn nounwind, !dbg !106
  unreachable, !dbg !106
                                                  ; No predecessors!
  br label %cond.end, !dbg !106

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %k.addr, align 4, !dbg !107
  %arrayidx = getelementptr inbounds i16* %2, i32 0, !dbg !107
  %3 = load i16* %arrayidx, !dbg !107
  %conv = zext i16 %3 to i32, !dbg !107
  %cmp1 = icmp eq i32 %conv, 0, !dbg !107
  br i1 %cmp1, label %if.then, label %if.end, !dbg !107

if.then:                                          ; preds = %cond.end
  store i32 0, i32* %retval, !dbg !108
  br label %return, !dbg !108

if.end:                                           ; preds = %cond.end
  %4 = load i16** %k.addr, align 4, !dbg !110
  %arrayidx3 = getelementptr inbounds i16* %4, i32 0, !dbg !110
  %5 = load i16* %arrayidx3, !dbg !110
  %idxprom = zext i16 %5 to i32, !dbg !110
  %6 = load i16** %k.addr, align 4, !dbg !110
  %arrayidx4 = getelementptr inbounds i16* %6, i32 %idxprom, !dbg !110
  %7 = load i16* %arrayidx4, !dbg !110
  store i16 %7, i16* %w, align 2, !dbg !110
  %8 = load i16** %k.addr, align 4, !dbg !111
  %arrayidx5 = getelementptr inbounds i16* %8, i32 0, !dbg !111
  %9 = load i16* %arrayidx5, !dbg !111
  %conv6 = zext i16 %9 to i32, !dbg !111
  %shl = shl i32 %conv6, 4, !dbg !111
  store i32 %shl, i32* %i, align 4, !dbg !111
  store i16 -32768, i16* %m, align 2, !dbg !111
  br label %for.cond, !dbg !111

for.cond:                                         ; preds = %for.inc, %if.end
  %10 = load i16* %m, align 2, !dbg !111
  %tobool = icmp ne i16 %10, 0, !dbg !111
  br i1 %tobool, label %for.body, label %for.end, !dbg !111

for.body:                                         ; preds = %for.cond
  %11 = load i16* %w, align 2, !dbg !113
  %conv7 = zext i16 %11 to i32, !dbg !113
  %12 = load i16* %m, align 2, !dbg !113
  %conv8 = zext i16 %12 to i32, !dbg !113
  %and = and i32 %conv7, %conv8, !dbg !113
  %tobool9 = icmp ne i32 %and, 0, !dbg !113
  br i1 %tobool9, label %if.then10, label %if.end11, !dbg !113

if.then10:                                        ; preds = %for.body
  %13 = load i32* %i, align 4, !dbg !115
  store i32 %13, i32* %retval, !dbg !115
  br label %return, !dbg !115

if.end11:                                         ; preds = %for.body
  br label %for.inc, !dbg !117

for.inc:                                          ; preds = %if.end11
  %14 = load i32* %i, align 4, !dbg !118
  %dec = add nsw i32 %14, -1, !dbg !118
  store i32 %dec, i32* %i, align 4, !dbg !118
  %15 = load i16* %m, align 2, !dbg !118
  %conv12 = zext i16 %15 to i32, !dbg !118
  %shr = ashr i32 %conv12, 1, !dbg !118
  %conv13 = trunc i32 %shr to i16, !dbg !118
  store i16 %conv13, i16* %m, align 2, !dbg !118
  br label %for.cond, !dbg !118

for.end:                                          ; preds = %for.cond
  store i32 0, i32* %retval, !dbg !119
  br label %return, !dbg !119

return:                                           ; preds = %for.end, %if.then10, %if.then
  %16 = load i32* %retval, !dbg !120
  ret i32 %16, !dbg !120
}

define arm_aapcscc i32 @vlTakeBit(i16* %k, i16 zeroext %i) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %k.addr = alloca i16*, align 4
  %i.addr = alloca i16, align 2
  store i16* %k, i16** %k.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %k.addr}, metadata !121), !dbg !122
  store i16 %i, i16* %i.addr, align 2
  call void @llvm.dbg.declare(metadata !{i16* %i.addr}, metadata !123), !dbg !124
  %0 = load i16** %k.addr, align 4, !dbg !125
  %cmp = icmp ne i16* %0, null, !dbg !125
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !125

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !125

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 133, i8* getelementptr inbounds ([38 x i8]* @__PRETTY_FUNCTION__.vlTakeBit, i32 0, i32 0)) noreturn nounwind, !dbg !127
  unreachable, !dbg !127
                                                  ; No predecessors!
  br label %cond.end, !dbg !127

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16* %i.addr, align 2, !dbg !128
  %conv = zext i16 %2 to i32, !dbg !128
  %3 = load i16** %k.addr, align 4, !dbg !128
  %arrayidx = getelementptr inbounds i16* %3, i32 0, !dbg !128
  %4 = load i16* %arrayidx, !dbg !128
  %conv1 = zext i16 %4 to i32, !dbg !128
  %shl = shl i32 %conv1, 4, !dbg !128
  %cmp2 = icmp sge i32 %conv, %shl, !dbg !128
  br i1 %cmp2, label %if.then, label %if.end, !dbg !128

if.then:                                          ; preds = %cond.end
  store i32 0, i32* %retval, !dbg !129
  br label %return, !dbg !129

if.end:                                           ; preds = %cond.end
  %5 = load i16* %i.addr, align 2, !dbg !131
  %conv4 = zext i16 %5 to i32, !dbg !131
  %shr = ashr i32 %conv4, 4, !dbg !131
  %add = add nsw i32 %shr, 1, !dbg !131
  %6 = load i16** %k.addr, align 4, !dbg !131
  %arrayidx5 = getelementptr inbounds i16* %6, i32 %add, !dbg !131
  %7 = load i16* %arrayidx5, !dbg !131
  %conv6 = zext i16 %7 to i32, !dbg !131
  %8 = load i16* %i.addr, align 2, !dbg !131
  %conv7 = zext i16 %8 to i32, !dbg !131
  %and = and i32 %conv7, 15, !dbg !131
  %shr8 = ashr i32 %conv6, %and, !dbg !131
  %and9 = and i32 %shr8, 1, !dbg !131
  store i32 %and9, i32* %retval, !dbg !131
  br label %return, !dbg !131

return:                                           ; preds = %if.end, %if.then
  %9 = load i32* %retval, !dbg !132
  ret i32 %9, !dbg !132
}

define arm_aapcscc void @vlAdd(i16* %u, i16* %v) nounwind uwtable {
entry:
  %u.addr = alloca i16*, align 4
  %v.addr = alloca i16*, align 4
  %i = alloca i16, align 2
  %t = alloca i32, align 4
  store i16* %u, i16** %u.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %u.addr}, metadata !133), !dbg !134
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !135), !dbg !136
  call void @llvm.dbg.declare(metadata !{i16* %i}, metadata !137), !dbg !139
  call void @llvm.dbg.declare(metadata !{i32* %t}, metadata !140), !dbg !143
  %0 = load i16** %u.addr, align 4, !dbg !144
  %cmp = icmp ne i16* %0, null, !dbg !144
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !144

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !144

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str4, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 146, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.vlAdd, i32 0, i32 0)) noreturn nounwind, !dbg !145
  unreachable, !dbg !145
                                                  ; No predecessors!
  br label %cond.end, !dbg !145

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %v.addr, align 4, !dbg !146
  %cmp1 = icmp ne i16* %2, null, !dbg !146
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !146

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !146

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str5, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 147, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.vlAdd, i32 0, i32 0)) noreturn nounwind, !dbg !147
  unreachable, !dbg !147
                                                  ; No predecessors!
  br label %cond.end4, !dbg !147

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %u.addr, align 4, !dbg !148
  %arrayidx = getelementptr inbounds i16* %4, i32 0, !dbg !148
  %5 = load i16* %arrayidx, !dbg !148
  %conv = zext i16 %5 to i32, !dbg !148
  %add = add nsw i32 %conv, 1, !dbg !148
  %conv5 = trunc i32 %add to i16, !dbg !148
  store i16 %conv5, i16* %i, align 2, !dbg !148
  br label %for.cond, !dbg !148

for.cond:                                         ; preds = %for.inc, %cond.end4
  %6 = load i16* %i, align 2, !dbg !148
  %conv6 = zext i16 %6 to i32, !dbg !148
  %7 = load i16** %v.addr, align 4, !dbg !148
  %arrayidx7 = getelementptr inbounds i16* %7, i32 0, !dbg !148
  %8 = load i16* %arrayidx7, !dbg !148
  %conv8 = zext i16 %8 to i32, !dbg !148
  %cmp9 = icmp sle i32 %conv6, %conv8, !dbg !148
  br i1 %cmp9, label %for.body, label %for.end, !dbg !148

for.body:                                         ; preds = %for.cond
  %9 = load i16* %i, align 2, !dbg !150
  %idxprom = zext i16 %9 to i32, !dbg !150
  %10 = load i16** %u.addr, align 4, !dbg !150
  %arrayidx11 = getelementptr inbounds i16* %10, i32 %idxprom, !dbg !150
  store i16 0, i16* %arrayidx11, !dbg !150
  br label %for.inc, !dbg !152

for.inc:                                          ; preds = %for.body
  %11 = load i16* %i, align 2, !dbg !153
  %inc = add i16 %11, 1, !dbg !153
  store i16 %inc, i16* %i, align 2, !dbg !153
  br label %for.cond, !dbg !153

for.end:                                          ; preds = %for.cond
  %12 = load i16** %u.addr, align 4, !dbg !154
  %arrayidx12 = getelementptr inbounds i16* %12, i32 0, !dbg !154
  %13 = load i16* %arrayidx12, !dbg !154
  %conv13 = zext i16 %13 to i32, !dbg !154
  %14 = load i16** %v.addr, align 4, !dbg !154
  %arrayidx14 = getelementptr inbounds i16* %14, i32 0, !dbg !154
  %15 = load i16* %arrayidx14, !dbg !154
  %conv15 = zext i16 %15 to i32, !dbg !154
  %cmp16 = icmp slt i32 %conv13, %conv15, !dbg !154
  br i1 %cmp16, label %if.then, label %if.end, !dbg !154

if.then:                                          ; preds = %for.end
  %16 = load i16** %v.addr, align 4, !dbg !155
  %arrayidx18 = getelementptr inbounds i16* %16, i32 0, !dbg !155
  %17 = load i16* %arrayidx18, !dbg !155
  %18 = load i16** %u.addr, align 4, !dbg !155
  %arrayidx19 = getelementptr inbounds i16* %18, i32 0, !dbg !155
  store i16 %17, i16* %arrayidx19, !dbg !155
  br label %if.end, !dbg !155

if.end:                                           ; preds = %if.then, %for.end
  store i32 0, i32* %t, align 4, !dbg !156
  store i16 1, i16* %i, align 2, !dbg !157
  br label %for.cond20, !dbg !157

for.cond20:                                       ; preds = %for.inc38, %if.end
  %19 = load i16* %i, align 2, !dbg !157
  %conv21 = zext i16 %19 to i32, !dbg !157
  %20 = load i16** %v.addr, align 4, !dbg !157
  %arrayidx22 = getelementptr inbounds i16* %20, i32 0, !dbg !157
  %21 = load i16* %arrayidx22, !dbg !157
  %conv23 = zext i16 %21 to i32, !dbg !157
  %cmp24 = icmp sle i32 %conv21, %conv23, !dbg !157
  br i1 %cmp24, label %for.body26, label %for.end40, !dbg !157

for.body26:                                       ; preds = %for.cond20
  %22 = load i32* %t, align 4, !dbg !159
  %23 = load i16* %i, align 2, !dbg !159
  %idxprom27 = zext i16 %23 to i32, !dbg !159
  %24 = load i16** %u.addr, align 4, !dbg !159
  %arrayidx28 = getelementptr inbounds i16* %24, i32 %idxprom27, !dbg !159
  %25 = load i16* %arrayidx28, !dbg !159
  %conv29 = zext i16 %25 to i32, !dbg !159
  %add30 = add i32 %22, %conv29, !dbg !159
  %26 = load i16* %i, align 2, !dbg !159
  %idxprom31 = zext i16 %26 to i32, !dbg !159
  %27 = load i16** %v.addr, align 4, !dbg !159
  %arrayidx32 = getelementptr inbounds i16* %27, i32 %idxprom31, !dbg !159
  %28 = load i16* %arrayidx32, !dbg !159
  %conv33 = zext i16 %28 to i32, !dbg !159
  %add34 = add i32 %add30, %conv33, !dbg !159
  store i32 %add34, i32* %t, align 4, !dbg !159
  %29 = load i32* %t, align 4, !dbg !161
  %and = and i32 %29, 65535, !dbg !161
  %conv35 = trunc i32 %and to i16, !dbg !161
  %30 = load i16* %i, align 2, !dbg !161
  %idxprom36 = zext i16 %30 to i32, !dbg !161
  %31 = load i16** %u.addr, align 4, !dbg !161
  %arrayidx37 = getelementptr inbounds i16* %31, i32 %idxprom36, !dbg !161
  store i16 %conv35, i16* %arrayidx37, !dbg !161
  %32 = load i32* %t, align 4, !dbg !162
  %shr = lshr i32 %32, 16, !dbg !162
  store i32 %shr, i32* %t, align 4, !dbg !162
  br label %for.inc38, !dbg !163

for.inc38:                                        ; preds = %for.body26
  %33 = load i16* %i, align 2, !dbg !164
  %inc39 = add i16 %33, 1, !dbg !164
  store i16 %inc39, i16* %i, align 2, !dbg !164
  br label %for.cond20, !dbg !164

for.end40:                                        ; preds = %for.cond20
  %34 = load i16** %v.addr, align 4, !dbg !165
  %arrayidx41 = getelementptr inbounds i16* %34, i32 0, !dbg !165
  %35 = load i16* %arrayidx41, !dbg !165
  %conv42 = zext i16 %35 to i32, !dbg !165
  %add43 = add nsw i32 %conv42, 1, !dbg !165
  %conv44 = trunc i32 %add43 to i16, !dbg !165
  store i16 %conv44, i16* %i, align 2, !dbg !165
  br label %while.cond, !dbg !166

while.cond:                                       ; preds = %if.end57, %for.end40
  %36 = load i32* %t, align 4, !dbg !166
  %tobool = icmp ne i32 %36, 0, !dbg !166
  br i1 %tobool, label %while.body, label %while.end, !dbg !166

while.body:                                       ; preds = %while.cond
  %37 = load i16* %i, align 2, !dbg !167
  %conv45 = zext i16 %37 to i32, !dbg !167
  %38 = load i16** %u.addr, align 4, !dbg !167
  %arrayidx46 = getelementptr inbounds i16* %38, i32 0, !dbg !167
  %39 = load i16* %arrayidx46, !dbg !167
  %conv47 = zext i16 %39 to i32, !dbg !167
  %cmp48 = icmp sgt i32 %conv45, %conv47, !dbg !167
  br i1 %cmp48, label %if.then50, label %if.end57, !dbg !167

if.then50:                                        ; preds = %while.body
  %40 = load i16* %i, align 2, !dbg !169
  %idxprom51 = zext i16 %40 to i32, !dbg !169
  %41 = load i16** %u.addr, align 4, !dbg !169
  %arrayidx52 = getelementptr inbounds i16* %41, i32 %idxprom51, !dbg !169
  store i16 0, i16* %arrayidx52, !dbg !169
  %42 = load i16** %u.addr, align 4, !dbg !171
  %arrayidx53 = getelementptr inbounds i16* %42, i32 0, !dbg !171
  %43 = load i16* %arrayidx53, !dbg !171
  %conv54 = zext i16 %43 to i32, !dbg !171
  %add55 = add nsw i32 %conv54, 1, !dbg !171
  %conv56 = trunc i32 %add55 to i16, !dbg !171
  store i16 %conv56, i16* %arrayidx53, !dbg !171
  br label %if.end57, !dbg !172

if.end57:                                         ; preds = %if.then50, %while.body
  %44 = load i16* %i, align 2, !dbg !173
  %idxprom58 = zext i16 %44 to i32, !dbg !173
  %45 = load i16** %u.addr, align 4, !dbg !173
  %arrayidx59 = getelementptr inbounds i16* %45, i32 %idxprom58, !dbg !173
  %46 = load i16* %arrayidx59, !dbg !173
  %conv60 = zext i16 %46 to i32, !dbg !173
  %add61 = add i32 %conv60, 1, !dbg !173
  store i32 %add61, i32* %t, align 4, !dbg !173
  %47 = load i32* %t, align 4, !dbg !174
  %and62 = and i32 %47, 65535, !dbg !174
  %conv63 = trunc i32 %and62 to i16, !dbg !174
  %48 = load i16* %i, align 2, !dbg !174
  %idxprom64 = zext i16 %48 to i32, !dbg !174
  %49 = load i16** %u.addr, align 4, !dbg !174
  %arrayidx65 = getelementptr inbounds i16* %49, i32 %idxprom64, !dbg !174
  store i16 %conv63, i16* %arrayidx65, !dbg !174
  %50 = load i32* %t, align 4, !dbg !175
  %shr66 = lshr i32 %50, 16, !dbg !175
  store i32 %shr66, i32* %t, align 4, !dbg !175
  %51 = load i16* %i, align 2, !dbg !176
  %conv67 = zext i16 %51 to i32, !dbg !176
  %add68 = add nsw i32 %conv67, 1, !dbg !176
  %conv69 = trunc i32 %add68 to i16, !dbg !176
  store i16 %conv69, i16* %i, align 2, !dbg !176
  br label %while.cond, !dbg !177

while.end:                                        ; preds = %while.cond
  ret void, !dbg !178
}

define arm_aapcscc void @vlSubtract(i16* %u, i16* %v) nounwind uwtable {
entry:
  %u.addr = alloca i16*, align 4
  %v.addr = alloca i16*, align 4
  %carry = alloca i32, align 4
  %tmp = alloca i32, align 4
  %i = alloca i32, align 4
  store i16* %u, i16** %u.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %u.addr}, metadata !179), !dbg !180
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !181), !dbg !182
  call void @llvm.dbg.declare(metadata !{i32* %carry}, metadata !183), !dbg !185
  store i32 0, i32* %carry, align 4, !dbg !186
  call void @llvm.dbg.declare(metadata !{i32* %tmp}, metadata !187), !dbg !188
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !189), !dbg !190
  %0 = load i16** %u.addr, align 4, !dbg !191
  %cmp = icmp ne i16* %0, null, !dbg !191
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !191

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !191

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str4, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 181, i8* getelementptr inbounds ([42 x i8]* @__PRETTY_FUNCTION__.vlSubtract, i32 0, i32 0)) noreturn nounwind, !dbg !192
  unreachable, !dbg !192
                                                  ; No predecessors!
  br label %cond.end, !dbg !192

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %v.addr, align 4, !dbg !193
  %cmp2 = icmp ne i16* %2, null, !dbg !193
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !193

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !193

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str5, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 182, i8* getelementptr inbounds ([42 x i8]* @__PRETTY_FUNCTION__.vlSubtract, i32 0, i32 0)) noreturn nounwind, !dbg !194
  unreachable, !dbg !194
                                                  ; No predecessors!
  br label %cond.end5, !dbg !194

cond.end5:                                        ; preds = %3, %cond.true3
  store i32 1, i32* %i, align 4, !dbg !195
  br label %for.cond, !dbg !195

for.cond:                                         ; preds = %for.inc, %cond.end5
  %4 = load i32* %i, align 4, !dbg !195
  %5 = load i16** %v.addr, align 4, !dbg !195
  %arrayidx = getelementptr inbounds i16* %5, i32 0, !dbg !195
  %6 = load i16* %arrayidx, !dbg !195
  %conv = zext i16 %6 to i32, !dbg !195
  %cmp6 = icmp sle i32 %4, %conv, !dbg !195
  br i1 %cmp6, label %for.body, label %for.end, !dbg !195

for.body:                                         ; preds = %for.cond
  %7 = load i32* %i, align 4, !dbg !197
  %8 = load i16** %u.addr, align 4, !dbg !197
  %arrayidx8 = getelementptr inbounds i16* %8, i32 %7, !dbg !197
  %9 = load i16* %arrayidx8, !dbg !197
  %conv9 = zext i16 %9 to i32, !dbg !197
  %add = add i32 65536, %conv9, !dbg !197
  %10 = load i32* %i, align 4, !dbg !197
  %11 = load i16** %v.addr, align 4, !dbg !197
  %arrayidx10 = getelementptr inbounds i16* %11, i32 %10, !dbg !197
  %12 = load i16* %arrayidx10, !dbg !197
  %conv11 = zext i16 %12 to i32, !dbg !197
  %sub = sub i32 %add, %conv11, !dbg !197
  %13 = load i32* %carry, align 4, !dbg !197
  %sub12 = sub i32 %sub, %13, !dbg !197
  store i32 %sub12, i32* %tmp, align 4, !dbg !197
  store i32 1, i32* %carry, align 4, !dbg !199
  %14 = load i32* %tmp, align 4, !dbg !200
  %cmp13 = icmp uge i32 %14, 65536, !dbg !200
  br i1 %cmp13, label %if.then, label %if.end, !dbg !200

if.then:                                          ; preds = %for.body
  %15 = load i32* %tmp, align 4, !dbg !201
  %sub15 = sub i32 %15, 65536, !dbg !201
  store i32 %sub15, i32* %tmp, align 4, !dbg !201
  store i32 0, i32* %carry, align 4, !dbg !203
  br label %if.end, !dbg !204

if.end:                                           ; preds = %if.then, %for.body
  %16 = load i32* %tmp, align 4, !dbg !205
  %conv16 = trunc i32 %16 to i16, !dbg !205
  %17 = load i32* %i, align 4, !dbg !205
  %18 = load i16** %u.addr, align 4, !dbg !205
  %arrayidx17 = getelementptr inbounds i16* %18, i32 %17, !dbg !205
  store i16 %conv16, i16* %arrayidx17, !dbg !205
  br label %for.inc, !dbg !206

for.inc:                                          ; preds = %if.end
  %19 = load i32* %i, align 4, !dbg !207
  %inc = add nsw i32 %19, 1, !dbg !207
  store i32 %inc, i32* %i, align 4, !dbg !207
  br label %for.cond, !dbg !207

for.end:                                          ; preds = %for.cond
  %20 = load i32* %carry, align 4, !dbg !208
  %tobool = icmp ne i32 %20, 0, !dbg !208
  br i1 %tobool, label %if.then18, label %if.end25, !dbg !208

if.then18:                                        ; preds = %for.end
  br label %while.cond, !dbg !209

while.cond:                                       ; preds = %while.body, %if.then18
  %21 = load i32* %i, align 4, !dbg !209
  %22 = load i16** %u.addr, align 4, !dbg !209
  %arrayidx19 = getelementptr inbounds i16* %22, i32 %21, !dbg !209
  %23 = load i16* %arrayidx19, !dbg !209
  %conv20 = zext i16 %23 to i32, !dbg !209
  %cmp21 = icmp eq i32 %conv20, 0, !dbg !209
  br i1 %cmp21, label %while.body, label %while.end, !dbg !209

while.body:                                       ; preds = %while.cond
  %24 = load i32* %i, align 4, !dbg !211
  %inc23 = add nsw i32 %24, 1, !dbg !211
  store i32 %inc23, i32* %i, align 4, !dbg !211
  br label %while.cond, !dbg !213

while.end:                                        ; preds = %while.cond
  %25 = load i32* %i, align 4, !dbg !214
  %26 = load i16** %u.addr, align 4, !dbg !214
  %arrayidx24 = getelementptr inbounds i16* %26, i32 %25, !dbg !214
  %27 = load i16* %arrayidx24, !dbg !214
  %dec = add i16 %27, -1, !dbg !214
  store i16 %dec, i16* %arrayidx24, !dbg !214
  br label %if.end25, !dbg !215

if.end25:                                         ; preds = %while.end, %for.end
  br label %while.cond26, !dbg !216

while.cond26:                                     ; preds = %while.body35, %if.end25
  %28 = load i16** %u.addr, align 4, !dbg !216
  %arrayidx27 = getelementptr inbounds i16* %28, i32 0, !dbg !216
  %29 = load i16* %arrayidx27, !dbg !216
  %idxprom = zext i16 %29 to i32, !dbg !216
  %30 = load i16** %u.addr, align 4, !dbg !216
  %arrayidx28 = getelementptr inbounds i16* %30, i32 %idxprom, !dbg !216
  %31 = load i16* %arrayidx28, !dbg !216
  %conv29 = zext i16 %31 to i32, !dbg !216
  %cmp30 = icmp eq i32 %conv29, 0, !dbg !216
  br i1 %cmp30, label %land.rhs, label %land.end, !dbg !216

land.rhs:                                         ; preds = %while.cond26
  %32 = load i16** %u.addr, align 4, !dbg !216
  %arrayidx32 = getelementptr inbounds i16* %32, i32 0, !dbg !216
  %33 = load i16* %arrayidx32, !dbg !216
  %conv33 = zext i16 %33 to i32, !dbg !216
  %tobool34 = icmp ne i32 %conv33, 0, !dbg !216
  br label %land.end

land.end:                                         ; preds = %land.rhs, %while.cond26
  %34 = phi i1 [ false, %while.cond26 ], [ %tobool34, %land.rhs ]
  br i1 %34, label %while.body35, label %while.end38

while.body35:                                     ; preds = %land.end
  %35 = load i16** %u.addr, align 4, !dbg !217
  %arrayidx36 = getelementptr inbounds i16* %35, i32 0, !dbg !217
  %36 = load i16* %arrayidx36, !dbg !217
  %dec37 = add i16 %36, -1, !dbg !217
  store i16 %dec37, i16* %arrayidx36, !dbg !217
  br label %while.cond26, !dbg !219

while.end38:                                      ; preds = %land.end
  ret void, !dbg !220
}

define arm_aapcscc void @vlShortLshift(i16* %p, i32 %n) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %n.addr = alloca i32, align 4
  %i = alloca i16, align 2
  %T = alloca i16, align 2
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !221), !dbg !222
  store i32 %n, i32* %n.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %n.addr}, metadata !223), !dbg !224
  call void @llvm.dbg.declare(metadata !{i16* %i}, metadata !225), !dbg !227
  call void @llvm.dbg.declare(metadata !{i16* %T}, metadata !228), !dbg !229
  store i16 0, i16* %T, align 2, !dbg !230
  %0 = load i16** %p.addr, align 4, !dbg !231
  %cmp = icmp ne i16* %0, null, !dbg !231
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !231

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !231

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 208, i8* getelementptr inbounds ([34 x i8]* @__PRETTY_FUNCTION__.vlShortLshift, i32 0, i32 0)) noreturn nounwind, !dbg !232
  unreachable, !dbg !232
                                                  ; No predecessors!
  br label %cond.end, !dbg !232

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %p.addr, align 4, !dbg !233
  %arrayidx = getelementptr inbounds i16* %2, i32 0, !dbg !233
  %3 = load i16* %arrayidx, !dbg !233
  %conv = zext i16 %3 to i32, !dbg !233
  %cmp1 = icmp eq i32 %conv, 0, !dbg !233
  br i1 %cmp1, label %if.then, label %if.end, !dbg !233

if.then:                                          ; preds = %cond.end
  br label %return, !dbg !234

if.end:                                           ; preds = %cond.end
  %4 = load i16** %p.addr, align 4, !dbg !236
  %arrayidx3 = getelementptr inbounds i16* %4, i32 0, !dbg !236
  %5 = load i16* %arrayidx3, !dbg !236
  %idxprom = zext i16 %5 to i32, !dbg !236
  %6 = load i16** %p.addr, align 4, !dbg !236
  %arrayidx4 = getelementptr inbounds i16* %6, i32 %idxprom, !dbg !236
  %7 = load i16* %arrayidx4, !dbg !236
  %conv5 = zext i16 %7 to i32, !dbg !236
  %8 = load i32* %n.addr, align 4, !dbg !236
  %sub = sub nsw i32 16, %8, !dbg !236
  %shr = ashr i32 %conv5, %sub, !dbg !236
  %tobool = icmp ne i32 %shr, 0, !dbg !236
  br i1 %tobool, label %if.then6, label %if.end17, !dbg !236

if.then6:                                         ; preds = %if.end
  %9 = load i16** %p.addr, align 4, !dbg !237
  %arrayidx7 = getelementptr inbounds i16* %9, i32 0, !dbg !237
  %10 = load i16* %arrayidx7, !dbg !237
  %conv8 = zext i16 %10 to i32, !dbg !237
  %cmp9 = icmp sle i32 %conv8, 18, !dbg !237
  br i1 %cmp9, label %if.then11, label %if.end16, !dbg !237

if.then11:                                        ; preds = %if.then6
  %11 = load i16** %p.addr, align 4, !dbg !239
  %arrayidx12 = getelementptr inbounds i16* %11, i32 0, !dbg !239
  %12 = load i16* %arrayidx12, !dbg !239
  %inc = add i16 %12, 1, !dbg !239
  store i16 %inc, i16* %arrayidx12, !dbg !239
  %13 = load i16** %p.addr, align 4, !dbg !241
  %arrayidx13 = getelementptr inbounds i16* %13, i32 0, !dbg !241
  %14 = load i16* %arrayidx13, !dbg !241
  %idxprom14 = zext i16 %14 to i32, !dbg !241
  %15 = load i16** %p.addr, align 4, !dbg !241
  %arrayidx15 = getelementptr inbounds i16* %15, i32 %idxprom14, !dbg !241
  store i16 0, i16* %arrayidx15, !dbg !241
  br label %if.end16, !dbg !242

if.end16:                                         ; preds = %if.then11, %if.then6
  br label %if.end17, !dbg !243

if.end17:                                         ; preds = %if.end16, %if.end
  %16 = load i16** %p.addr, align 4, !dbg !244
  %arrayidx18 = getelementptr inbounds i16* %16, i32 0, !dbg !244
  %17 = load i16* %arrayidx18, !dbg !244
  store i16 %17, i16* %i, align 2, !dbg !244
  br label %for.cond, !dbg !244

for.cond:                                         ; preds = %for.inc, %if.end17
  %18 = load i16* %i, align 2, !dbg !244
  %conv19 = zext i16 %18 to i32, !dbg !244
  %cmp20 = icmp sgt i32 %conv19, 1, !dbg !244
  br i1 %cmp20, label %for.body, label %for.end, !dbg !244

for.body:                                         ; preds = %for.cond
  %19 = load i16* %i, align 2, !dbg !246
  %idxprom22 = zext i16 %19 to i32, !dbg !246
  %20 = load i16** %p.addr, align 4, !dbg !246
  %arrayidx23 = getelementptr inbounds i16* %20, i32 %idxprom22, !dbg !246
  %21 = load i16* %arrayidx23, !dbg !246
  %conv24 = zext i16 %21 to i32, !dbg !246
  %22 = load i32* %n.addr, align 4, !dbg !246
  %shl = shl i32 %conv24, %22, !dbg !246
  %23 = load i16* %i, align 2, !dbg !246
  %conv25 = zext i16 %23 to i32, !dbg !246
  %sub26 = sub nsw i32 %conv25, 1, !dbg !246
  %24 = load i16** %p.addr, align 4, !dbg !246
  %arrayidx27 = getelementptr inbounds i16* %24, i32 %sub26, !dbg !246
  %25 = load i16* %arrayidx27, !dbg !246
  %conv28 = zext i16 %25 to i32, !dbg !246
  %26 = load i32* %n.addr, align 4, !dbg !246
  %sub29 = sub nsw i32 16, %26, !dbg !246
  %shr30 = ashr i32 %conv28, %sub29, !dbg !246
  %or = or i32 %shl, %shr30, !dbg !246
  %conv31 = trunc i32 %or to i16, !dbg !246
  %27 = load i16* %i, align 2, !dbg !246
  %idxprom32 = zext i16 %27 to i32, !dbg !246
  %28 = load i16** %p.addr, align 4, !dbg !246
  %arrayidx33 = getelementptr inbounds i16* %28, i32 %idxprom32, !dbg !246
  store i16 %conv31, i16* %arrayidx33, !dbg !246
  br label %for.inc, !dbg !248

for.inc:                                          ; preds = %for.body
  %29 = load i16* %i, align 2, !dbg !249
  %dec = add i16 %29, -1, !dbg !249
  store i16 %dec, i16* %i, align 2, !dbg !249
  br label %for.cond, !dbg !249

for.end:                                          ; preds = %for.cond
  %30 = load i32* %n.addr, align 4, !dbg !250
  %31 = load i16** %p.addr, align 4, !dbg !250
  %arrayidx34 = getelementptr inbounds i16* %31, i32 1, !dbg !250
  %32 = load i16* %arrayidx34, !dbg !250
  %conv35 = zext i16 %32 to i32, !dbg !250
  %shl36 = shl i32 %conv35, %30, !dbg !250
  %conv37 = trunc i32 %shl36 to i16, !dbg !250
  store i16 %conv37, i16* %arrayidx34, !dbg !250
  br label %return, !dbg !251

return:                                           ; preds = %for.end, %if.then
  ret void, !dbg !251
}

define arm_aapcscc void @vlShortRshift(i16* %p, i32 %n) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %n.addr = alloca i32, align 4
  %i = alloca i16, align 2
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !252), !dbg !253
  store i32 %n, i32* %n.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %n.addr}, metadata !254), !dbg !255
  call void @llvm.dbg.declare(metadata !{i16* %i}, metadata !256), !dbg !258
  %0 = load i16** %p.addr, align 4, !dbg !259
  %cmp = icmp ne i16* %0, null, !dbg !259
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !259

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !259

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 231, i8* getelementptr inbounds ([34 x i8]* @__PRETTY_FUNCTION__.vlShortRshift, i32 0, i32 0)) noreturn nounwind, !dbg !260
  unreachable, !dbg !260
                                                  ; No predecessors!
  br label %cond.end, !dbg !260

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %p.addr, align 4, !dbg !261
  %arrayidx = getelementptr inbounds i16* %2, i32 0, !dbg !261
  %3 = load i16* %arrayidx, !dbg !261
  %conv = zext i16 %3 to i32, !dbg !261
  %cmp1 = icmp eq i32 %conv, 0, !dbg !261
  br i1 %cmp1, label %if.then, label %if.end, !dbg !261

if.then:                                          ; preds = %cond.end
  br label %if.end30, !dbg !262

if.end:                                           ; preds = %cond.end
  store i16 1, i16* %i, align 2, !dbg !264
  br label %for.cond, !dbg !264

for.cond:                                         ; preds = %for.inc, %if.end
  %4 = load i16* %i, align 2, !dbg !264
  %conv3 = zext i16 %4 to i32, !dbg !264
  %5 = load i16** %p.addr, align 4, !dbg !264
  %arrayidx4 = getelementptr inbounds i16* %5, i32 0, !dbg !264
  %6 = load i16* %arrayidx4, !dbg !264
  %conv5 = zext i16 %6 to i32, !dbg !264
  %cmp6 = icmp slt i32 %conv3, %conv5, !dbg !264
  br i1 %cmp6, label %for.body, label %for.end, !dbg !264

for.body:                                         ; preds = %for.cond
  %7 = load i16* %i, align 2, !dbg !266
  %conv8 = zext i16 %7 to i32, !dbg !266
  %add = add nsw i32 %conv8, 1, !dbg !266
  %8 = load i16** %p.addr, align 4, !dbg !266
  %arrayidx9 = getelementptr inbounds i16* %8, i32 %add, !dbg !266
  %9 = load i16* %arrayidx9, !dbg !266
  %conv10 = zext i16 %9 to i32, !dbg !266
  %10 = load i32* %n.addr, align 4, !dbg !266
  %sub = sub nsw i32 16, %10, !dbg !266
  %shl = shl i32 %conv10, %sub, !dbg !266
  %11 = load i16* %i, align 2, !dbg !266
  %idxprom = zext i16 %11 to i32, !dbg !266
  %12 = load i16** %p.addr, align 4, !dbg !266
  %arrayidx11 = getelementptr inbounds i16* %12, i32 %idxprom, !dbg !266
  %13 = load i16* %arrayidx11, !dbg !266
  %conv12 = zext i16 %13 to i32, !dbg !266
  %14 = load i32* %n.addr, align 4, !dbg !266
  %shr = ashr i32 %conv12, %14, !dbg !266
  %or = or i32 %shl, %shr, !dbg !266
  %conv13 = trunc i32 %or to i16, !dbg !266
  %15 = load i16* %i, align 2, !dbg !266
  %idxprom14 = zext i16 %15 to i32, !dbg !266
  %16 = load i16** %p.addr, align 4, !dbg !266
  %arrayidx15 = getelementptr inbounds i16* %16, i32 %idxprom14, !dbg !266
  store i16 %conv13, i16* %arrayidx15, !dbg !266
  br label %for.inc, !dbg !268

for.inc:                                          ; preds = %for.body
  %17 = load i16* %i, align 2, !dbg !269
  %inc = add i16 %17, 1, !dbg !269
  store i16 %inc, i16* %i, align 2, !dbg !269
  br label %for.cond, !dbg !269

for.end:                                          ; preds = %for.cond
  %18 = load i32* %n.addr, align 4, !dbg !270
  %19 = load i16** %p.addr, align 4, !dbg !270
  %arrayidx16 = getelementptr inbounds i16* %19, i32 0, !dbg !270
  %20 = load i16* %arrayidx16, !dbg !270
  %idxprom17 = zext i16 %20 to i32, !dbg !270
  %21 = load i16** %p.addr, align 4, !dbg !270
  %arrayidx18 = getelementptr inbounds i16* %21, i32 %idxprom17, !dbg !270
  %22 = load i16* %arrayidx18, !dbg !270
  %conv19 = zext i16 %22 to i32, !dbg !270
  %shr20 = ashr i32 %conv19, %18, !dbg !270
  %conv21 = trunc i32 %shr20 to i16, !dbg !270
  store i16 %conv21, i16* %arrayidx18, !dbg !270
  %23 = load i16** %p.addr, align 4, !dbg !271
  %arrayidx22 = getelementptr inbounds i16* %23, i32 0, !dbg !271
  %24 = load i16* %arrayidx22, !dbg !271
  %idxprom23 = zext i16 %24 to i32, !dbg !271
  %25 = load i16** %p.addr, align 4, !dbg !271
  %arrayidx24 = getelementptr inbounds i16* %25, i32 %idxprom23, !dbg !271
  %26 = load i16* %arrayidx24, !dbg !271
  %conv25 = zext i16 %26 to i32, !dbg !271
  %cmp26 = icmp eq i32 %conv25, 0, !dbg !271
  br i1 %cmp26, label %if.then28, label %if.end30, !dbg !271

if.then28:                                        ; preds = %for.end
  %27 = load i16** %p.addr, align 4, !dbg !272
  %arrayidx29 = getelementptr inbounds i16* %27, i32 0, !dbg !272
  %28 = load i16* %arrayidx29, !dbg !272
  %dec = add i16 %28, -1, !dbg !272
  store i16 %dec, i16* %arrayidx29, !dbg !272
  br label %if.end30, !dbg !274

if.end30:                                         ; preds = %if.then, %if.then28, %for.end
  ret void, !dbg !275
}

define arm_aapcscc i32 @vlShortMultiply(i16* %p, i16* %q, i16 zeroext %d) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %p.addr = alloca i16*, align 4
  %q.addr = alloca i16*, align 4
  %d.addr = alloca i16, align 2
  %i = alloca i32, align 4
  %t = alloca i32, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !276), !dbg !277
  store i16* %q, i16** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q.addr}, metadata !278), !dbg !279
  store i16 %d, i16* %d.addr, align 2
  call void @llvm.dbg.declare(metadata !{i16* %d.addr}, metadata !280), !dbg !281
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !282), !dbg !284
  call void @llvm.dbg.declare(metadata !{i32* %t}, metadata !285), !dbg !286
  %0 = load i16** %p.addr, align 4, !dbg !287
  %cmp = icmp ne i16* %0, null, !dbg !287
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !287

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !287

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 252, i8* getelementptr inbounds ([54 x i8]* @__PRETTY_FUNCTION__.vlShortMultiply, i32 0, i32 0)) noreturn nounwind, !dbg !288
  unreachable, !dbg !288
                                                  ; No predecessors!
  br label %cond.end, !dbg !288

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %q.addr, align 4, !dbg !289
  %cmp1 = icmp ne i16* %2, null, !dbg !289
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !289

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !289

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 253, i8* getelementptr inbounds ([54 x i8]* @__PRETTY_FUNCTION__.vlShortMultiply, i32 0, i32 0)) noreturn nounwind, !dbg !290
  unreachable, !dbg !290
                                                  ; No predecessors!
  br label %cond.end4, !dbg !290

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %q.addr, align 4, !dbg !291
  %arrayidx = getelementptr inbounds i16* %4, i32 0, !dbg !291
  %5 = load i16* %arrayidx, !dbg !291
  %conv = zext i16 %5 to i32, !dbg !291
  %cmp5 = icmp sgt i32 %conv, 17, !dbg !291
  br i1 %cmp5, label %if.then, label %if.end, !dbg !291

if.then:                                          ; preds = %cond.end4
  %call = call arm_aapcscc  i32 @puts(i8* getelementptr inbounds ([43 x i8]* @.str6, i32 0, i32 0)), !dbg !292
  store i32 -1, i32* %retval, !dbg !294
  br label %return, !dbg !294

if.end:                                           ; preds = %cond.end4
  %6 = load i16* %d.addr, align 2, !dbg !295
  %conv7 = zext i16 %6 to i32, !dbg !295
  %cmp8 = icmp sgt i32 %conv7, 1, !dbg !295
  br i1 %cmp8, label %if.then10, label %if.else33, !dbg !295

if.then10:                                        ; preds = %if.end
  store i32 0, i32* %t, align 4, !dbg !296
  store i32 1, i32* %i, align 4, !dbg !298
  br label %for.cond, !dbg !298

for.cond:                                         ; preds = %for.inc, %if.then10
  %7 = load i32* %i, align 4, !dbg !298
  %8 = load i16** %q.addr, align 4, !dbg !298
  %arrayidx11 = getelementptr inbounds i16* %8, i32 0, !dbg !298
  %9 = load i16* %arrayidx11, !dbg !298
  %conv12 = zext i16 %9 to i32, !dbg !298
  %cmp13 = icmp sle i32 %7, %conv12, !dbg !298
  br i1 %cmp13, label %for.body, label %for.end, !dbg !298

for.body:                                         ; preds = %for.cond
  %10 = load i32* %i, align 4, !dbg !300
  %11 = load i16** %q.addr, align 4, !dbg !300
  %arrayidx15 = getelementptr inbounds i16* %11, i32 %10, !dbg !300
  %12 = load i16* %arrayidx15, !dbg !300
  %conv16 = zext i16 %12 to i32, !dbg !300
  %13 = load i16* %d.addr, align 2, !dbg !300
  %conv17 = zext i16 %13 to i32, !dbg !300
  %mul = mul i32 %conv16, %conv17, !dbg !300
  %14 = load i32* %t, align 4, !dbg !300
  %add = add i32 %14, %mul, !dbg !300
  store i32 %add, i32* %t, align 4, !dbg !300
  %15 = load i32* %t, align 4, !dbg !302
  %and = and i32 %15, 65535, !dbg !302
  %conv18 = trunc i32 %and to i16, !dbg !302
  %16 = load i32* %i, align 4, !dbg !302
  %17 = load i16** %p.addr, align 4, !dbg !302
  %arrayidx19 = getelementptr inbounds i16* %17, i32 %16, !dbg !302
  store i16 %conv18, i16* %arrayidx19, !dbg !302
  %18 = load i32* %t, align 4, !dbg !303
  %shr = lshr i32 %18, 16, !dbg !303
  store i32 %shr, i32* %t, align 4, !dbg !303
  br label %for.inc, !dbg !304

for.inc:                                          ; preds = %for.body
  %19 = load i32* %i, align 4, !dbg !305
  %inc = add nsw i32 %19, 1, !dbg !305
  store i32 %inc, i32* %i, align 4, !dbg !305
  br label %for.cond, !dbg !305

for.end:                                          ; preds = %for.cond
  %20 = load i32* %t, align 4, !dbg !306
  %tobool = icmp ne i32 %20, 0, !dbg !306
  br i1 %tobool, label %if.then20, label %if.else, !dbg !306

if.then20:                                        ; preds = %for.end
  %21 = load i16** %q.addr, align 4, !dbg !307
  %arrayidx21 = getelementptr inbounds i16* %21, i32 0, !dbg !307
  %22 = load i16* %arrayidx21, !dbg !307
  %conv22 = zext i16 %22 to i32, !dbg !307
  %add23 = add nsw i32 %conv22, 1, !dbg !307
  %conv24 = trunc i32 %add23 to i16, !dbg !307
  %23 = load i16** %p.addr, align 4, !dbg !307
  %arrayidx25 = getelementptr inbounds i16* %23, i32 0, !dbg !307
  store i16 %conv24, i16* %arrayidx25, !dbg !307
  %24 = load i32* %t, align 4, !dbg !309
  %and26 = and i32 %24, 65535, !dbg !309
  %conv27 = trunc i32 %and26 to i16, !dbg !309
  %25 = load i16** %p.addr, align 4, !dbg !309
  %arrayidx28 = getelementptr inbounds i16* %25, i32 0, !dbg !309
  %26 = load i16* %arrayidx28, !dbg !309
  %idxprom = zext i16 %26 to i32, !dbg !309
  %27 = load i16** %p.addr, align 4, !dbg !309
  %arrayidx29 = getelementptr inbounds i16* %27, i32 %idxprom, !dbg !309
  store i16 %conv27, i16* %arrayidx29, !dbg !309
  br label %if.end32, !dbg !310

if.else:                                          ; preds = %for.end
  %28 = load i16** %q.addr, align 4, !dbg !311
  %arrayidx30 = getelementptr inbounds i16* %28, i32 0, !dbg !311
  %29 = load i16* %arrayidx30, !dbg !311
  %30 = load i16** %p.addr, align 4, !dbg !311
  %arrayidx31 = getelementptr inbounds i16* %30, i32 0, !dbg !311
  store i16 %29, i16* %arrayidx31, !dbg !311
  br label %if.end32

if.end32:                                         ; preds = %if.else, %if.then20
  br label %if.end39, !dbg !313

if.else33:                                        ; preds = %if.end
  %31 = load i16* %d.addr, align 2, !dbg !314
  %tobool34 = icmp ne i16 %31, 0, !dbg !314
  br i1 %tobool34, label %if.then35, label %if.else36, !dbg !314

if.then35:                                        ; preds = %if.else33
  %32 = load i16** %p.addr, align 4, !dbg !315
  %33 = load i16** %q.addr, align 4, !dbg !315
  call arm_aapcscc  void @vlCopy(i16* %32, i16* %33), !dbg !315
  br label %if.end38, !dbg !317

if.else36:                                        ; preds = %if.else33
  %34 = load i16** %p.addr, align 4, !dbg !318
  %arrayidx37 = getelementptr inbounds i16* %34, i32 0, !dbg !318
  store i16 0, i16* %arrayidx37, !dbg !318
  br label %if.end38

if.end38:                                         ; preds = %if.else36, %if.then35
  br label %if.end39

if.end39:                                         ; preds = %if.end38, %if.end32
  store i32 0, i32* %retval, !dbg !320
  br label %return, !dbg !320

return:                                           ; preds = %if.end39, %if.then
  %35 = load i32* %retval, !dbg !321
  ret i32 %35, !dbg !321
}

declare arm_aapcscc i32 @puts(i8*)

define arm_aapcscc i32 @vlGreater(i16* %p, i16* %q) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %p.addr = alloca i16*, align 4
  %q.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !322), !dbg !323
  store i16* %q, i16** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q.addr}, metadata !324), !dbg !325
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !326), !dbg !328
  %0 = load i16** %p.addr, align 4, !dbg !329
  %cmp = icmp ne i16* %0, null, !dbg !329
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !329

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !329

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 284, i8* getelementptr inbounds ([46 x i8]* @__PRETTY_FUNCTION__.vlGreater, i32 0, i32 0)) noreturn nounwind, !dbg !330
  unreachable, !dbg !330
                                                  ; No predecessors!
  br label %cond.end, !dbg !330

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %q.addr, align 4, !dbg !331
  %cmp1 = icmp ne i16* %2, null, !dbg !331
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !331

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !331

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 285, i8* getelementptr inbounds ([46 x i8]* @__PRETTY_FUNCTION__.vlGreater, i32 0, i32 0)) noreturn nounwind, !dbg !332
  unreachable, !dbg !332
                                                  ; No predecessors!
  br label %cond.end4, !dbg !332

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %p.addr, align 4, !dbg !333
  %arrayidx = getelementptr inbounds i16* %4, i32 0, !dbg !333
  %5 = load i16* %arrayidx, !dbg !333
  %conv = zext i16 %5 to i32, !dbg !333
  %6 = load i16** %q.addr, align 4, !dbg !333
  %arrayidx5 = getelementptr inbounds i16* %6, i32 0, !dbg !333
  %7 = load i16* %arrayidx5, !dbg !333
  %conv6 = zext i16 %7 to i32, !dbg !333
  %cmp7 = icmp sgt i32 %conv, %conv6, !dbg !333
  br i1 %cmp7, label %if.then, label %if.end, !dbg !333

if.then:                                          ; preds = %cond.end4
  store i32 1, i32* %retval, !dbg !334
  br label %return, !dbg !334

if.end:                                           ; preds = %cond.end4
  %8 = load i16** %p.addr, align 4, !dbg !335
  %arrayidx9 = getelementptr inbounds i16* %8, i32 0, !dbg !335
  %9 = load i16* %arrayidx9, !dbg !335
  %conv10 = zext i16 %9 to i32, !dbg !335
  %10 = load i16** %q.addr, align 4, !dbg !335
  %arrayidx11 = getelementptr inbounds i16* %10, i32 0, !dbg !335
  %11 = load i16* %arrayidx11, !dbg !335
  %conv12 = zext i16 %11 to i32, !dbg !335
  %cmp13 = icmp slt i32 %conv10, %conv12, !dbg !335
  br i1 %cmp13, label %if.then15, label %if.end16, !dbg !335

if.then15:                                        ; preds = %if.end
  store i32 0, i32* %retval, !dbg !336
  br label %return, !dbg !336

if.end16:                                         ; preds = %if.end
  %12 = load i16** %p.addr, align 4, !dbg !337
  %arrayidx17 = getelementptr inbounds i16* %12, i32 0, !dbg !337
  %13 = load i16* %arrayidx17, !dbg !337
  %conv18 = zext i16 %13 to i32, !dbg !337
  store i32 %conv18, i32* %i, align 4, !dbg !337
  br label %for.cond, !dbg !337

for.cond:                                         ; preds = %for.inc, %if.end16
  %14 = load i32* %i, align 4, !dbg !337
  %cmp19 = icmp sgt i32 %14, 0, !dbg !337
  br i1 %cmp19, label %for.body, label %for.end, !dbg !337

for.body:                                         ; preds = %for.cond
  %15 = load i32* %i, align 4, !dbg !339
  %16 = load i16** %p.addr, align 4, !dbg !339
  %arrayidx21 = getelementptr inbounds i16* %16, i32 %15, !dbg !339
  %17 = load i16* %arrayidx21, !dbg !339
  %conv22 = zext i16 %17 to i32, !dbg !339
  %18 = load i32* %i, align 4, !dbg !339
  %19 = load i16** %q.addr, align 4, !dbg !339
  %arrayidx23 = getelementptr inbounds i16* %19, i32 %18, !dbg !339
  %20 = load i16* %arrayidx23, !dbg !339
  %conv24 = zext i16 %20 to i32, !dbg !339
  %cmp25 = icmp sgt i32 %conv22, %conv24, !dbg !339
  br i1 %cmp25, label %if.then27, label %if.end28, !dbg !339

if.then27:                                        ; preds = %for.body
  store i32 1, i32* %retval, !dbg !341
  br label %return, !dbg !341

if.end28:                                         ; preds = %for.body
  %21 = load i32* %i, align 4, !dbg !342
  %22 = load i16** %p.addr, align 4, !dbg !342
  %arrayidx29 = getelementptr inbounds i16* %22, i32 %21, !dbg !342
  %23 = load i16* %arrayidx29, !dbg !342
  %conv30 = zext i16 %23 to i32, !dbg !342
  %24 = load i32* %i, align 4, !dbg !342
  %25 = load i16** %q.addr, align 4, !dbg !342
  %arrayidx31 = getelementptr inbounds i16* %25, i32 %24, !dbg !342
  %26 = load i16* %arrayidx31, !dbg !342
  %conv32 = zext i16 %26 to i32, !dbg !342
  %cmp33 = icmp slt i32 %conv30, %conv32, !dbg !342
  br i1 %cmp33, label %if.then35, label %if.end36, !dbg !342

if.then35:                                        ; preds = %if.end28
  store i32 0, i32* %retval, !dbg !343
  br label %return, !dbg !343

if.end36:                                         ; preds = %if.end28
  br label %for.inc, !dbg !344

for.inc:                                          ; preds = %if.end36
  %27 = load i32* %i, align 4, !dbg !345
  %dec = add nsw i32 %27, -1, !dbg !345
  store i32 %dec, i32* %i, align 4, !dbg !345
  br label %for.cond, !dbg !345

for.end:                                          ; preds = %for.cond
  store i32 0, i32* %retval, !dbg !346
  br label %return, !dbg !346

return:                                           ; preds = %for.end, %if.then35, %if.then27, %if.then15, %if.then
  %28 = load i32* %retval, !dbg !347
  ret i32 %28, !dbg !347
}

define arm_aapcscc void @vlRemainder(i16* %u, i16* %v) nounwind uwtable {
entry:
  %u.addr = alloca i16*, align 4
  %v.addr = alloca i16*, align 4
  %t = alloca [19 x i16], align 2
  %shift = alloca i32, align 4
  store i16* %u, i16** %u.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %u.addr}, metadata !348), !dbg !349
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !350), !dbg !351
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %t}, metadata !352), !dbg !358
  call void @llvm.dbg.declare(metadata !{i32* %shift}, metadata !359), !dbg !360
  store i32 0, i32* %shift, align 4, !dbg !361
  %0 = load i16** %u.addr, align 4, !dbg !362
  %cmp = icmp ne i16* %0, null, !dbg !362
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !362

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !362

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str4, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 301, i8* getelementptr inbounds ([43 x i8]* @__PRETTY_FUNCTION__.vlRemainder, i32 0, i32 0)) noreturn nounwind, !dbg !363
  unreachable, !dbg !363
                                                  ; No predecessors!
  br label %cond.end, !dbg !363

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %v.addr, align 4, !dbg !364
  %cmp1 = icmp ne i16* %2, null, !dbg !364
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !364

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !364

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str5, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 302, i8* getelementptr inbounds ([43 x i8]* @__PRETTY_FUNCTION__.vlRemainder, i32 0, i32 0)) noreturn nounwind, !dbg !365
  unreachable, !dbg !365
                                                  ; No predecessors!
  br label %cond.end4, !dbg !365

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %v.addr, align 4, !dbg !366
  %arrayidx = getelementptr inbounds i16* %4, i32 0, !dbg !366
  %5 = load i16* %arrayidx, !dbg !366
  %conv = zext i16 %5 to i32, !dbg !366
  %cmp5 = icmp ne i32 %conv, 0, !dbg !366
  br i1 %cmp5, label %cond.true7, label %cond.false8, !dbg !366

cond.true7:                                       ; preds = %cond.end4
  br label %cond.end9, !dbg !366

cond.false8:                                      ; preds = %cond.end4
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([10 x i8]* @.str7, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 303, i8* getelementptr inbounds ([43 x i8]* @__PRETTY_FUNCTION__.vlRemainder, i32 0, i32 0)) noreturn nounwind, !dbg !367
  unreachable, !dbg !367
                                                  ; No predecessors!
  br label %cond.end9, !dbg !367

cond.end9:                                        ; preds = %6, %cond.true7
  %arraydecay = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !368
  %7 = load i16** %v.addr, align 4, !dbg !368
  call arm_aapcscc  void @vlCopy(i16* %arraydecay, i16* %7), !dbg !368
  br label %while.cond, !dbg !369

while.cond:                                       ; preds = %while.body, %cond.end9
  %8 = load i16** %u.addr, align 4, !dbg !370
  %arraydecay10 = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !370
  %call = call arm_aapcscc  i32 @vlGreater(i16* %8, i16* %arraydecay10), !dbg !370
  %tobool = icmp ne i32 %call, 0, !dbg !370
  br i1 %tobool, label %while.body, label %while.end, !dbg !370

while.body:                                       ; preds = %while.cond
  %arraydecay11 = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !371
  call arm_aapcscc  void @vlShortLshift(i16* %arraydecay11, i32 1), !dbg !371
  %9 = load i32* %shift, align 4, !dbg !373
  %add = add nsw i32 %9, 1, !dbg !373
  store i32 %add, i32* %shift, align 4, !dbg !373
  br label %while.cond, !dbg !374

while.end:                                        ; preds = %while.cond
  br label %while.body13, !dbg !375

while.body13:                                     ; preds = %while.end, %if.end22
  %arraydecay14 = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !376
  %10 = load i16** %u.addr, align 4, !dbg !376
  %call15 = call arm_aapcscc  i32 @vlGreater(i16* %arraydecay14, i16* %10), !dbg !376
  %tobool16 = icmp ne i32 %call15, 0, !dbg !376
  br i1 %tobool16, label %if.then, label %if.else20, !dbg !376

if.then:                                          ; preds = %while.body13
  %11 = load i32* %shift, align 4, !dbg !378
  %tobool17 = icmp ne i32 %11, 0, !dbg !378
  br i1 %tobool17, label %if.then18, label %if.else, !dbg !378

if.then18:                                        ; preds = %if.then
  %arraydecay19 = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !380
  call arm_aapcscc  void @vlShortRshift(i16* %arraydecay19, i32 1), !dbg !380
  %12 = load i32* %shift, align 4, !dbg !382
  %sub = sub nsw i32 %12, 1, !dbg !382
  store i32 %sub, i32* %shift, align 4, !dbg !382
  br label %if.end, !dbg !383

if.else:                                          ; preds = %if.then
  br label %while.end23, !dbg !384

if.end:                                           ; preds = %if.then18
  br label %if.end22, !dbg !385

if.else20:                                        ; preds = %while.body13
  %13 = load i16** %u.addr, align 4, !dbg !386
  %arraydecay21 = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !386
  call arm_aapcscc  void @vlSubtract(i16* %13, i16* %arraydecay21), !dbg !386
  br label %if.end22

if.end22:                                         ; preds = %if.else20, %if.end
  br label %while.body13, !dbg !387

while.end23:                                      ; preds = %if.else
  ret void, !dbg !388
}

define arm_aapcscc void @vlMulMod(i16* %u, i16* %v, i16* %w, i16* %m) nounwind uwtable {
entry:
  %u.addr = alloca i16*, align 4
  %v.addr = alloca i16*, align 4
  %w.addr = alloca i16*, align 4
  %m.addr = alloca i16*, align 4
  %t = alloca [19 x i16], align 2
  %i = alloca i32, align 4
  %j = alloca i32, align 4
  store i16* %u, i16** %u.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %u.addr}, metadata !389), !dbg !390
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !391), !dbg !392
  store i16* %w, i16** %w.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %w.addr}, metadata !393), !dbg !394
  store i16* %m, i16** %m.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %m.addr}, metadata !395), !dbg !396
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %t}, metadata !397), !dbg !399
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !400), !dbg !401
  call void @llvm.dbg.declare(metadata !{i32* %j}, metadata !402), !dbg !403
  %0 = load i16** %u.addr, align 4, !dbg !404
  %cmp = icmp ne i16* %0, null, !dbg !404
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !404

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !404

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str4, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 392, i8* getelementptr inbounds ([72 x i8]* @__PRETTY_FUNCTION__.vlMulMod, i32 0, i32 0)) noreturn nounwind, !dbg !405
  unreachable, !dbg !405
                                                  ; No predecessors!
  br label %cond.end, !dbg !405

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %v.addr, align 4, !dbg !406
  %cmp1 = icmp ne i16* %2, null, !dbg !406
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !406

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !406

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str5, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 393, i8* getelementptr inbounds ([72 x i8]* @__PRETTY_FUNCTION__.vlMulMod, i32 0, i32 0)) noreturn nounwind, !dbg !407
  unreachable, !dbg !407
                                                  ; No predecessors!
  br label %cond.end4, !dbg !407

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %w.addr, align 4, !dbg !408
  %cmp5 = icmp ne i16* %4, null, !dbg !408
  br i1 %cmp5, label %cond.true6, label %cond.false7, !dbg !408

cond.true6:                                       ; preds = %cond.end4
  br label %cond.end8, !dbg !408

cond.false7:                                      ; preds = %cond.end4
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str8, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 394, i8* getelementptr inbounds ([72 x i8]* @__PRETTY_FUNCTION__.vlMulMod, i32 0, i32 0)) noreturn nounwind, !dbg !409
  unreachable, !dbg !409
                                                  ; No predecessors!
  br label %cond.end8, !dbg !409

cond.end8:                                        ; preds = %5, %cond.true6
  %6 = load i16** %m.addr, align 4, !dbg !410
  %cmp9 = icmp ne i16* %6, null, !dbg !410
  br i1 %cmp9, label %cond.true10, label %cond.false11, !dbg !410

cond.true10:                                      ; preds = %cond.end8
  br label %cond.end12, !dbg !410

cond.false11:                                     ; preds = %cond.end8
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str9, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 395, i8* getelementptr inbounds ([72 x i8]* @__PRETTY_FUNCTION__.vlMulMod, i32 0, i32 0)) noreturn nounwind, !dbg !411
  unreachable, !dbg !411
                                                  ; No predecessors!
  br label %cond.end12, !dbg !411

cond.end12:                                       ; preds = %7, %cond.true10
  %8 = load i16** %m.addr, align 4, !dbg !412
  %arrayidx = getelementptr inbounds i16* %8, i32 0, !dbg !412
  %9 = load i16* %arrayidx, !dbg !412
  %conv = zext i16 %9 to i32, !dbg !412
  %cmp13 = icmp ne i32 %conv, 0, !dbg !412
  br i1 %cmp13, label %cond.true15, label %cond.false16, !dbg !412

cond.true15:                                      ; preds = %cond.end12
  br label %cond.end17, !dbg !412

cond.false16:                                     ; preds = %cond.end12
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([10 x i8]* @.str10, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 396, i8* getelementptr inbounds ([72 x i8]* @__PRETTY_FUNCTION__.vlMulMod, i32 0, i32 0)) noreturn nounwind, !dbg !413
  unreachable, !dbg !413
                                                  ; No predecessors!
  br label %cond.end17, !dbg !413

cond.end17:                                       ; preds = %10, %cond.true15
  %11 = load i16** %u.addr, align 4, !dbg !414
  call arm_aapcscc  void @vlClear(i16* %11), !dbg !414
  %arraydecay = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !415
  %12 = load i16** %w.addr, align 4, !dbg !415
  call arm_aapcscc  void @vlCopy(i16* %arraydecay, i16* %12), !dbg !415
  store i32 1, i32* %i, align 4, !dbg !416
  br label %for.cond, !dbg !416

for.cond:                                         ; preds = %for.inc31, %cond.end17
  %13 = load i32* %i, align 4, !dbg !416
  %14 = load i16** %v.addr, align 4, !dbg !416
  %arrayidx18 = getelementptr inbounds i16* %14, i32 0, !dbg !416
  %15 = load i16* %arrayidx18, !dbg !416
  %conv19 = zext i16 %15 to i32, !dbg !416
  %cmp20 = icmp sle i32 %13, %conv19, !dbg !416
  br i1 %cmp20, label %for.body, label %for.end33, !dbg !416

for.body:                                         ; preds = %for.cond
  store i32 0, i32* %j, align 4, !dbg !418
  br label %for.cond22, !dbg !418

for.cond22:                                       ; preds = %for.inc, %for.body
  %16 = load i32* %j, align 4, !dbg !418
  %cmp23 = icmp slt i32 %16, 16, !dbg !418
  br i1 %cmp23, label %for.body25, label %for.end, !dbg !418

for.body25:                                       ; preds = %for.cond22
  %17 = load i32* %i, align 4, !dbg !421
  %18 = load i16** %v.addr, align 4, !dbg !421
  %arrayidx26 = getelementptr inbounds i16* %18, i32 %17, !dbg !421
  %19 = load i16* %arrayidx26, !dbg !421
  %conv27 = zext i16 %19 to i32, !dbg !421
  %20 = load i32* %j, align 4, !dbg !421
  %shl = shl i32 1, %20, !dbg !421
  %and = and i32 %conv27, %shl, !dbg !421
  %tobool = icmp ne i32 %and, 0, !dbg !421
  br i1 %tobool, label %if.then, label %if.end, !dbg !421

if.then:                                          ; preds = %for.body25
  %21 = load i16** %u.addr, align 4, !dbg !423
  %arraydecay28 = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !423
  call arm_aapcscc  void @vlAdd(i16* %21, i16* %arraydecay28), !dbg !423
  %22 = load i16** %u.addr, align 4, !dbg !425
  %23 = load i16** %m.addr, align 4, !dbg !425
  call arm_aapcscc  void @vlRemainder(i16* %22, i16* %23), !dbg !425
  br label %if.end, !dbg !426

if.end:                                           ; preds = %if.then, %for.body25
  %arraydecay29 = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !427
  call arm_aapcscc  void @vlShortLshift(i16* %arraydecay29, i32 1), !dbg !427
  %arraydecay30 = getelementptr inbounds [19 x i16]* %t, i32 0, i32 0, !dbg !428
  %24 = load i16** %m.addr, align 4, !dbg !428
  call arm_aapcscc  void @vlRemainder(i16* %arraydecay30, i16* %24), !dbg !428
  br label %for.inc, !dbg !429

for.inc:                                          ; preds = %if.end
  %25 = load i32* %j, align 4, !dbg !430
  %add = add nsw i32 %25, 1, !dbg !430
  store i32 %add, i32* %j, align 4, !dbg !430
  br label %for.cond22, !dbg !430

for.end:                                          ; preds = %for.cond22
  br label %for.inc31, !dbg !431

for.inc31:                                        ; preds = %for.end
  %26 = load i32* %i, align 4, !dbg !432
  %add32 = add nsw i32 %26, 1, !dbg !432
  store i32 %add32, i32* %i, align 4, !dbg !432
  br label %for.cond, !dbg !432

for.end33:                                        ; preds = %for.cond
  ret void, !dbg !433
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !12, metadata !17, metadata !20, metadata !23, metadata !26, metadata !29, metadata !32, metadata !35, metadata !38, metadata !41, metadata !44, metadata !47, metadata !50}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlEqual", metadata !"vlEqual", metadata !"", metadata !6, i32 79, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, i16*)* @vlEqual, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"ec_vlong.c", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlClear", metadata !"vlClear", metadata !"", metadata !6, i32 87, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*)* @vlClear, null, null, metadata !15} ; [ DW_TAG_subprogram ]
!13 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !14, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!14 = metadata !{null}
!15 = metadata !{metadata !16}
!16 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!17 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlCopy", metadata !"vlCopy", metadata !"", metadata !6, i32 95, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @vlCopy, null, null, metadata !18} ; [ DW_TAG_subprogram ]
!18 = metadata !{metadata !19}
!19 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!20 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlShortSet", metadata !"vlShortSet", metadata !"", metadata !6, i32 104, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16)* @vlShortSet, null, null, metadata !21} ; [ DW_TAG_subprogram ]
!21 = metadata !{metadata !22}
!22 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!23 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlNumBits", metadata !"vlNumBits", metadata !"", metadata !6, i32 112, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*)* @vlNumBits, null, null, metadata !24} ; [ DW_TAG_subprogram ]
!24 = metadata !{metadata !25}
!25 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!26 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlTakeBit", metadata !"vlTakeBit", metadata !"", metadata !6, i32 132, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, i16)* @vlTakeBit, null, null, metadata !27} ; [ DW_TAG_subprogram ]
!27 = metadata !{metadata !28}
!28 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!29 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlAdd", metadata !"vlAdd", metadata !"", metadata !6, i32 142, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @vlAdd, null, null, metadata !30} ; [ DW_TAG_subprogram ]
!30 = metadata !{metadata !31}
!31 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!32 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlSubtract", metadata !"vlSubtract", metadata !"", metadata !6, i32 176, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @vlSubtract, null, null, metadata !33} ; [ DW_TAG_subprogram ]
!33 = metadata !{metadata !34}
!34 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!35 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlShortLshift", metadata !"vlShortLshift", metadata !"", metadata !6, i32 205, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i32)* @vlShortLshift, null, null, metadata !36} ; [ DW_TAG_subprogram ]
!36 = metadata !{metadata !37}
!37 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!38 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlShortRshift", metadata !"vlShortRshift", metadata !"", metadata !6, i32 228, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i32)* @vlShortRshift, null, null, metadata !39} ; [ DW_TAG_subprogram ]
!39 = metadata !{metadata !40}
!40 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!41 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlShortMultiply", metadata !"vlShortMultiply", metadata !"", metadata !6, i32 248, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, i16*, i16)* @vlShortMultiply, null, null, metadata !42} ; [ DW_TAG_subprogram ]
!42 = metadata !{metadata !43}
!43 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!44 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlGreater", metadata !"vlGreater", metadata !"", metadata !6, i32 281, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, i16*)* @vlGreater, null, null, metadata !45} ; [ DW_TAG_subprogram ]
!45 = metadata !{metadata !46}
!46 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!47 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlRemainder", metadata !"vlRemainder", metadata !"", metadata !6, i32 297, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @vlRemainder, null, null, metadata !48} ; [ DW_TAG_subprogram ]
!48 = metadata !{metadata !49}
!49 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!50 = metadata !{i32 720942, i32 0, metadata !6, metadata !"vlMulMod", metadata !"vlMulMod", metadata !"", metadata !6, i32 388, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*, i16*, i16*)* @vlMulMod, null, null, metadata !51} ; [ DW_TAG_subprogram ]
!51 = metadata !{metadata !52}
!52 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!53 = metadata !{i32 721153, metadata !5, metadata !"p", metadata !6, i32 16777294, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!54 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !55} ; [ DW_TAG_pointer_type ]
!55 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !56} ; [ DW_TAG_const_type ]
!56 = metadata !{i32 720918, null, metadata !"word16", metadata !6, i32 11, i64 0, i64 0, i64 0, i32 0, metadata !57} ; [ DW_TAG_typedef ]
!57 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!58 = metadata !{i32 78, i32 28, metadata !5, null}
!59 = metadata !{i32 721153, metadata !5, metadata !"q", metadata !6, i32 33554510, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!60 = metadata !{i32 78, i32 45, metadata !5, null}
!61 = metadata !{i32 80, i32 2, metadata !62, null}
!62 = metadata !{i32 720907, metadata !5, i32 79, i32 1, metadata !6, i32 242} ; [ DW_TAG_lexical_block ]
!63 = metadata !{i32 80, i32 37, metadata !62, null}
!64 = metadata !{i32 81, i32 2, metadata !62, null}
!65 = metadata !{i32 81, i32 37, metadata !62, null}
!66 = metadata !{i32 82, i32 9, metadata !62, null}
!67 = metadata !{i32 721153, metadata !12, metadata !"p", metadata !6, i32 16777302, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!68 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !56} ; [ DW_TAG_pointer_type ]
!69 = metadata !{i32 86, i32 23, metadata !12, null}
!70 = metadata !{i32 88, i32 2, metadata !71, null}
!71 = metadata !{i32 720907, metadata !12, i32 87, i32 1, metadata !6, i32 243} ; [ DW_TAG_lexical_block ]
!72 = metadata !{i32 88, i32 37, metadata !71, null}
!73 = metadata !{i32 89, i32 2, metadata !71, null}
!74 = metadata !{i32 90, i32 1, metadata !71, null}
!75 = metadata !{i32 721153, metadata !17, metadata !"p", metadata !6, i32 16777309, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!76 = metadata !{i32 93, i32 22, metadata !17, null}
!77 = metadata !{i32 721153, metadata !17, metadata !"q", metadata !6, i32 33554525, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!78 = metadata !{i32 93, i32 39, metadata !17, null}
!79 = metadata !{i32 96, i32 2, metadata !80, null}
!80 = metadata !{i32 720907, metadata !17, i32 95, i32 1, metadata !6, i32 244} ; [ DW_TAG_lexical_block ]
!81 = metadata !{i32 96, i32 37, metadata !80, null}
!82 = metadata !{i32 97, i32 2, metadata !80, null}
!83 = metadata !{i32 97, i32 37, metadata !80, null}
!84 = metadata !{i32 98, i32 2, metadata !80, null}
!85 = metadata !{i32 99, i32 1, metadata !80, null}
!86 = metadata !{i32 721153, metadata !20, metadata !"p", metadata !6, i32 16777318, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!87 = metadata !{i32 102, i32 26, metadata !20, null}
!88 = metadata !{i32 721153, metadata !20, metadata !"u", metadata !6, i32 33554534, metadata !56, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!89 = metadata !{i32 102, i32 36, metadata !20, null}
!90 = metadata !{i32 105, i32 2, metadata !91, null}
!91 = metadata !{i32 720907, metadata !20, i32 104, i32 1, metadata !6, i32 245} ; [ DW_TAG_lexical_block ]
!92 = metadata !{i32 105, i32 37, metadata !91, null}
!93 = metadata !{i32 106, i32 2, metadata !91, null}
!94 = metadata !{i32 106, i32 12, metadata !91, null}
!95 = metadata !{i32 107, i32 1, metadata !91, null}
!96 = metadata !{i32 721153, metadata !23, metadata !"k", metadata !6, i32 16777326, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!97 = metadata !{i32 110, i32 30, metadata !23, null}
!98 = metadata !{i32 721152, metadata !99, metadata !"i", metadata !6, i32 113, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!99 = metadata !{i32 720907, metadata !23, i32 112, i32 1, metadata !6, i32 246} ; [ DW_TAG_lexical_block ]
!100 = metadata !{i32 113, i32 6, metadata !99, null}
!101 = metadata !{i32 721152, metadata !99, metadata !"m", metadata !6, i32 114, metadata !56, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!102 = metadata !{i32 114, i32 9, metadata !99, null}
!103 = metadata !{i32 721152, metadata !99, metadata !"w", metadata !6, i32 114, metadata !56, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!104 = metadata !{i32 114, i32 12, metadata !99, null}
!105 = metadata !{i32 116, i32 2, metadata !99, null}
!106 = metadata !{i32 116, i32 37, metadata !99, null}
!107 = metadata !{i32 117, i32 2, metadata !99, null}
!108 = metadata !{i32 118, i32 3, metadata !109, null}
!109 = metadata !{i32 720907, metadata !99, i32 117, i32 17, metadata !6, i32 247} ; [ DW_TAG_lexical_block ]
!110 = metadata !{i32 120, i32 2, metadata !99, null}
!111 = metadata !{i32 121, i32 7, metadata !112, null}
!112 = metadata !{i32 720907, metadata !99, i32 121, i32 2, metadata !6, i32 248} ; [ DW_TAG_lexical_block ]
!113 = metadata !{i32 122, i32 3, metadata !114, null}
!114 = metadata !{i32 720907, metadata !112, i32 121, i32 59, metadata !6, i32 249} ; [ DW_TAG_lexical_block ]
!115 = metadata !{i32 123, i32 4, metadata !116, null}
!116 = metadata !{i32 720907, metadata !114, i32 122, i32 14, metadata !6, i32 250} ; [ DW_TAG_lexical_block ]
!117 = metadata !{i32 125, i32 2, metadata !114, null}
!118 = metadata !{i32 121, i32 45, metadata !112, null}
!119 = metadata !{i32 126, i32 2, metadata !99, null}
!120 = metadata !{i32 127, i32 1, metadata !99, null}
!121 = metadata !{i32 721153, metadata !26, metadata !"k", metadata !6, i32 16777346, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!122 = metadata !{i32 130, i32 30, metadata !26, null}
!123 = metadata !{i32 721153, metadata !26, metadata !"i", metadata !6, i32 33554562, metadata !56, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!124 = metadata !{i32 130, i32 40, metadata !26, null}
!125 = metadata !{i32 133, i32 2, metadata !126, null}
!126 = metadata !{i32 720907, metadata !26, i32 132, i32 1, metadata !6, i32 251} ; [ DW_TAG_lexical_block ]
!127 = metadata !{i32 133, i32 37, metadata !126, null}
!128 = metadata !{i32 134, i32 2, metadata !126, null}
!129 = metadata !{i32 135, i32 3, metadata !130, null}
!130 = metadata !{i32 720907, metadata !126, i32 134, i32 24, metadata !6, i32 252} ; [ DW_TAG_lexical_block ]
!131 = metadata !{i32 137, i32 2, metadata !126, null}
!132 = metadata !{i32 138, i32 1, metadata !126, null}
!133 = metadata !{i32 721153, metadata !29, metadata !"u", metadata !6, i32 16777357, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!134 = metadata !{i32 141, i32 21, metadata !29, null}
!135 = metadata !{i32 721153, metadata !29, metadata !"v", metadata !6, i32 33554573, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!136 = metadata !{i32 141, i32 38, metadata !29, null}
!137 = metadata !{i32 721152, metadata !138, metadata !"i", metadata !6, i32 143, metadata !56, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!138 = metadata !{i32 720907, metadata !29, i32 142, i32 1, metadata !6, i32 253} ; [ DW_TAG_lexical_block ]
!139 = metadata !{i32 143, i32 9, metadata !138, null}
!140 = metadata !{i32 721152, metadata !138, metadata !"t", metadata !6, i32 144, metadata !141, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!141 = metadata !{i32 720918, null, metadata !"word32", metadata !6, i32 12, i64 0, i64 0, i64 0, i32 0, metadata !142} ; [ DW_TAG_typedef ]
!142 = metadata !{i32 720932, null, metadata !"long unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!143 = metadata !{i32 144, i32 9, metadata !138, null}
!144 = metadata !{i32 146, i32 2, metadata !138, null}
!145 = metadata !{i32 146, i32 37, metadata !138, null}
!146 = metadata !{i32 147, i32 2, metadata !138, null}
!147 = metadata !{i32 147, i32 37, metadata !138, null}
!148 = metadata !{i32 149, i32 7, metadata !149, null}
!149 = metadata !{i32 720907, metadata !138, i32 149, i32 2, metadata !6, i32 254} ; [ DW_TAG_lexical_block ]
!150 = metadata !{i32 150, i32 3, metadata !151, null}
!151 = metadata !{i32 720907, metadata !149, i32 149, i32 37, metadata !6, i32 255} ; [ DW_TAG_lexical_block ]
!152 = metadata !{i32 151, i32 2, metadata !151, null}
!153 = metadata !{i32 149, i32 32, metadata !149, null}
!154 = metadata !{i32 152, i32 5, metadata !138, null}
!155 = metadata !{i32 153, i32 7, metadata !138, null}
!156 = metadata !{i32 154, i32 2, metadata !138, null}
!157 = metadata !{i32 155, i32 7, metadata !158, null}
!158 = metadata !{i32 720907, metadata !138, i32 155, i32 2, metadata !6, i32 256} ; [ DW_TAG_lexical_block ]
!159 = metadata !{i32 156, i32 3, metadata !160, null}
!160 = metadata !{i32 720907, metadata !158, i32 155, i32 30, metadata !6, i32 257} ; [ DW_TAG_lexical_block ]
!161 = metadata !{i32 157, i32 3, metadata !160, null}
!162 = metadata !{i32 158, i32 3, metadata !160, null}
!163 = metadata !{i32 159, i32 2, metadata !160, null}
!164 = metadata !{i32 155, i32 25, metadata !158, null}
!165 = metadata !{i32 160, i32 5, metadata !138, null}
!166 = metadata !{i32 161, i32 2, metadata !138, null}
!167 = metadata !{i32 162, i32 9, metadata !168, null}
!168 = metadata !{i32 720907, metadata !138, i32 161, i32 12, metadata !6, i32 258} ; [ DW_TAG_lexical_block ]
!169 = metadata !{i32 164, i32 11, metadata !170, null}
!170 = metadata !{i32 720907, metadata !168, i32 163, i32 9, metadata !6, i32 259} ; [ DW_TAG_lexical_block ]
!171 = metadata !{i32 165, i32 11, metadata !170, null}
!172 = metadata !{i32 166, i32 9, metadata !170, null}
!173 = metadata !{i32 167, i32 9, metadata !168, null}
!174 = metadata !{i32 168, i32 3, metadata !168, null}
!175 = metadata !{i32 169, i32 9, metadata !168, null}
!176 = metadata !{i32 170, i32 9, metadata !168, null}
!177 = metadata !{i32 171, i32 2, metadata !168, null}
!178 = metadata !{i32 172, i32 1, metadata !138, null}
!179 = metadata !{i32 721153, metadata !32, metadata !"u", metadata !6, i32 16777391, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!180 = metadata !{i32 175, i32 26, metadata !32, null}
!181 = metadata !{i32 721153, metadata !32, metadata !"v", metadata !6, i32 33554607, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!182 = metadata !{i32 175, i32 43, metadata !32, null}
!183 = metadata !{i32 721152, metadata !184, metadata !"carry", metadata !6, i32 178, metadata !141, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!184 = metadata !{i32 720907, metadata !32, i32 176, i32 1, metadata !6, i32 260} ; [ DW_TAG_lexical_block ]
!185 = metadata !{i32 178, i32 9, metadata !184, null}
!186 = metadata !{i32 178, i32 23, metadata !184, null}
!187 = metadata !{i32 721152, metadata !184, metadata !"tmp", metadata !6, i32 178, metadata !141, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!188 = metadata !{i32 178, i32 20, metadata !184, null}
!189 = metadata !{i32 721152, metadata !184, metadata !"i", metadata !6, i32 179, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!190 = metadata !{i32 179, i32 6, metadata !184, null}
!191 = metadata !{i32 181, i32 2, metadata !184, null}
!192 = metadata !{i32 181, i32 37, metadata !184, null}
!193 = metadata !{i32 182, i32 2, metadata !184, null}
!194 = metadata !{i32 182, i32 37, metadata !184, null}
!195 = metadata !{i32 183, i32 7, metadata !196, null}
!196 = metadata !{i32 720907, metadata !184, i32 183, i32 2, metadata !6, i32 261} ; [ DW_TAG_lexical_block ]
!197 = metadata !{i32 184, i32 3, metadata !198, null}
!198 = metadata !{i32 720907, metadata !196, i32 183, i32 30, metadata !6, i32 262} ; [ DW_TAG_lexical_block ]
!199 = metadata !{i32 185, i32 3, metadata !198, null}
!200 = metadata !{i32 186, i32 3, metadata !198, null}
!201 = metadata !{i32 187, i32 4, metadata !202, null}
!202 = metadata !{i32 720907, metadata !198, i32 186, i32 25, metadata !6, i32 263} ; [ DW_TAG_lexical_block ]
!203 = metadata !{i32 188, i32 4, metadata !202, null}
!204 = metadata !{i32 189, i32 3, metadata !202, null}
!205 = metadata !{i32 190, i32 3, metadata !198, null}
!206 = metadata !{i32 191, i32 2, metadata !198, null}
!207 = metadata !{i32 183, i32 25, metadata !196, null}
!208 = metadata !{i32 192, i32 2, metadata !184, null}
!209 = metadata !{i32 193, i32 3, metadata !210, null}
!210 = metadata !{i32 720907, metadata !184, i32 192, i32 13, metadata !6, i32 264} ; [ DW_TAG_lexical_block ]
!211 = metadata !{i32 194, i32 4, metadata !212, null}
!212 = metadata !{i32 720907, metadata !210, i32 193, i32 21, metadata !6, i32 265} ; [ DW_TAG_lexical_block ]
!213 = metadata !{i32 195, i32 3, metadata !212, null}
!214 = metadata !{i32 196, i32 3, metadata !210, null}
!215 = metadata !{i32 197, i32 2, metadata !210, null}
!216 = metadata !{i32 198, i32 2, metadata !184, null}
!217 = metadata !{i32 199, i32 3, metadata !218, null}
!218 = metadata !{i32 720907, metadata !184, i32 198, i32 31, metadata !6, i32 266} ; [ DW_TAG_lexical_block ]
!219 = metadata !{i32 200, i32 2, metadata !218, null}
!220 = metadata !{i32 201, i32 1, metadata !184, null}
!221 = metadata !{i32 721153, metadata !35, metadata !"p", metadata !6, i32 16777420, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!222 = metadata !{i32 204, i32 29, metadata !35, null}
!223 = metadata !{i32 721153, metadata !35, metadata !"n", metadata !6, i32 33554636, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!224 = metadata !{i32 204, i32 36, metadata !35, null}
!225 = metadata !{i32 721152, metadata !226, metadata !"i", metadata !6, i32 206, metadata !56, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!226 = metadata !{i32 720907, metadata !35, i32 205, i32 1, metadata !6, i32 267} ; [ DW_TAG_lexical_block ]
!227 = metadata !{i32 206, i32 9, metadata !226, null}
!228 = metadata !{i32 721152, metadata !226, metadata !"T", metadata !6, i32 206, metadata !56, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!229 = metadata !{i32 206, i32 12, metadata !226, null}
!230 = metadata !{i32 206, i32 15, metadata !226, null}
!231 = metadata !{i32 208, i32 2, metadata !226, null}
!232 = metadata !{i32 208, i32 37, metadata !226, null}
!233 = metadata !{i32 209, i32 2, metadata !226, null}
!234 = metadata !{i32 210, i32 3, metadata !235, null}
!235 = metadata !{i32 720907, metadata !226, i32 209, i32 17, metadata !6, i32 268} ; [ DW_TAG_lexical_block ]
!236 = metadata !{i32 213, i32 2, metadata !226, null}
!237 = metadata !{i32 215, i32 3, metadata !238, null}
!238 = metadata !{i32 720907, metadata !226, i32 213, i32 27, metadata !6, i32 269} ; [ DW_TAG_lexical_block ]
!239 = metadata !{i32 216, i32 4, metadata !240, null}
!240 = metadata !{i32 720907, metadata !238, i32 215, i32 42, metadata !6, i32 270} ; [ DW_TAG_lexical_block ]
!241 = metadata !{i32 217, i32 4, metadata !240, null}
!242 = metadata !{i32 218, i32 3, metadata !240, null}
!243 = metadata !{i32 219, i32 2, metadata !238, null}
!244 = metadata !{i32 220, i32 7, metadata !245, null}
!245 = metadata !{i32 720907, metadata !226, i32 220, i32 2, metadata !6, i32 271} ; [ DW_TAG_lexical_block ]
!246 = metadata !{i32 221, i32 3, metadata !247, null}
!247 = metadata !{i32 720907, metadata !245, i32 220, i32 29, metadata !6, i32 272} ; [ DW_TAG_lexical_block ]
!248 = metadata !{i32 222, i32 2, metadata !247, null}
!249 = metadata !{i32 220, i32 24, metadata !245, null}
!250 = metadata !{i32 223, i32 2, metadata !226, null}
!251 = metadata !{i32 224, i32 1, metadata !226, null}
!252 = metadata !{i32 721153, metadata !38, metadata !"p", metadata !6, i32 16777443, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!253 = metadata !{i32 227, i32 29, metadata !38, null}
!254 = metadata !{i32 721153, metadata !38, metadata !"n", metadata !6, i32 33554659, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!255 = metadata !{i32 227, i32 36, metadata !38, null}
!256 = metadata !{i32 721152, metadata !257, metadata !"i", metadata !6, i32 229, metadata !56, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!257 = metadata !{i32 720907, metadata !38, i32 228, i32 1, metadata !6, i32 273} ; [ DW_TAG_lexical_block ]
!258 = metadata !{i32 229, i32 9, metadata !257, null}
!259 = metadata !{i32 231, i32 2, metadata !257, null}
!260 = metadata !{i32 231, i32 37, metadata !257, null}
!261 = metadata !{i32 232, i32 2, metadata !257, null}
!262 = metadata !{i32 233, i32 3, metadata !263, null}
!263 = metadata !{i32 720907, metadata !257, i32 232, i32 17, metadata !6, i32 274} ; [ DW_TAG_lexical_block ]
!264 = metadata !{i32 236, i32 7, metadata !265, null}
!265 = metadata !{i32 720907, metadata !257, i32 236, i32 2, metadata !6, i32 275} ; [ DW_TAG_lexical_block ]
!266 = metadata !{i32 237, i32 3, metadata !267, null}
!267 = metadata !{i32 720907, metadata !265, i32 236, i32 29, metadata !6, i32 276} ; [ DW_TAG_lexical_block ]
!268 = metadata !{i32 238, i32 2, metadata !267, null}
!269 = metadata !{i32 236, i32 24, metadata !265, null}
!270 = metadata !{i32 239, i32 2, metadata !257, null}
!271 = metadata !{i32 240, i32 2, metadata !257, null}
!272 = metadata !{i32 241, i32 3, metadata !273, null}
!273 = metadata !{i32 720907, metadata !257, i32 240, i32 20, metadata !6, i32 277} ; [ DW_TAG_lexical_block ]
!274 = metadata !{i32 242, i32 2, metadata !273, null}
!275 = metadata !{i32 243, i32 1, metadata !257, null}
!276 = metadata !{i32 721153, metadata !41, metadata !"p", metadata !6, i32 16777462, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!277 = metadata !{i32 246, i32 30, metadata !41, null}
!278 = metadata !{i32 721153, metadata !41, metadata !"q", metadata !6, i32 33554678, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!279 = metadata !{i32 246, i32 47, metadata !41, null}
!280 = metadata !{i32 721153, metadata !41, metadata !"d", metadata !6, i32 50331894, metadata !56, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!281 = metadata !{i32 246, i32 57, metadata !41, null}
!282 = metadata !{i32 721152, metadata !283, metadata !"i", metadata !6, i32 249, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!283 = metadata !{i32 720907, metadata !41, i32 248, i32 1, metadata !6, i32 278} ; [ DW_TAG_lexical_block ]
!284 = metadata !{i32 249, i32 6, metadata !283, null}
!285 = metadata !{i32 721152, metadata !283, metadata !"t", metadata !6, i32 250, metadata !141, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!286 = metadata !{i32 250, i32 9, metadata !283, null}
!287 = metadata !{i32 252, i32 2, metadata !283, null}
!288 = metadata !{i32 252, i32 37, metadata !283, null}
!289 = metadata !{i32 253, i32 2, metadata !283, null}
!290 = metadata !{i32 253, i32 37, metadata !283, null}
!291 = metadata !{i32 254, i32 2, metadata !283, null}
!292 = metadata !{i32 255, i32 3, metadata !293, null}
!293 = metadata !{i32 720907, metadata !283, i32 254, i32 36, metadata !6, i32 279} ; [ DW_TAG_lexical_block ]
!294 = metadata !{i32 256, i32 3, metadata !293, null}
!295 = metadata !{i32 258, i32 2, metadata !283, null}
!296 = metadata !{i32 259, i32 3, metadata !297, null}
!297 = metadata !{i32 720907, metadata !283, i32 258, i32 13, metadata !6, i32 280} ; [ DW_TAG_lexical_block ]
!298 = metadata !{i32 260, i32 8, metadata !299, null}
!299 = metadata !{i32 720907, metadata !297, i32 260, i32 3, metadata !6, i32 281} ; [ DW_TAG_lexical_block ]
!300 = metadata !{i32 261, i32 4, metadata !301, null}
!301 = metadata !{i32 720907, metadata !299, i32 260, i32 31, metadata !6, i32 282} ; [ DW_TAG_lexical_block ]
!302 = metadata !{i32 262, i32 4, metadata !301, null}
!303 = metadata !{i32 263, i32 4, metadata !301, null}
!304 = metadata !{i32 264, i32 3, metadata !301, null}
!305 = metadata !{i32 260, i32 26, metadata !299, null}
!306 = metadata !{i32 265, i32 3, metadata !297, null}
!307 = metadata !{i32 266, i32 4, metadata !308, null}
!308 = metadata !{i32 720907, metadata !297, i32 265, i32 10, metadata !6, i32 283} ; [ DW_TAG_lexical_block ]
!309 = metadata !{i32 267, i32 4, metadata !308, null}
!310 = metadata !{i32 268, i32 3, metadata !308, null}
!311 = metadata !{i32 269, i32 4, metadata !312, null}
!312 = metadata !{i32 720907, metadata !297, i32 268, i32 10, metadata !6, i32 284} ; [ DW_TAG_lexical_block ]
!313 = metadata !{i32 271, i32 2, metadata !297, null}
!314 = metadata !{i32 271, i32 9, metadata !283, null}
!315 = metadata !{i32 272, i32 3, metadata !316, null}
!316 = metadata !{i32 720907, metadata !283, i32 271, i32 16, metadata !6, i32 285} ; [ DW_TAG_lexical_block ]
!317 = metadata !{i32 273, i32 2, metadata !316, null}
!318 = metadata !{i32 274, i32 3, metadata !319, null}
!319 = metadata !{i32 720907, metadata !283, i32 273, i32 9, metadata !6, i32 286} ; [ DW_TAG_lexical_block ]
!320 = metadata !{i32 276, i32 2, metadata !283, null}
!321 = metadata !{i32 277, i32 1, metadata !283, null}
!322 = metadata !{i32 721153, metadata !44, metadata !"p", metadata !6, i32 16777496, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!323 = metadata !{i32 280, i32 30, metadata !44, null}
!324 = metadata !{i32 721153, metadata !44, metadata !"q", metadata !6, i32 33554712, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!325 = metadata !{i32 280, i32 47, metadata !44, null}
!326 = metadata !{i32 721152, metadata !327, metadata !"i", metadata !6, i32 282, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!327 = metadata !{i32 720907, metadata !44, i32 281, i32 1, metadata !6, i32 287} ; [ DW_TAG_lexical_block ]
!328 = metadata !{i32 282, i32 6, metadata !327, null}
!329 = metadata !{i32 284, i32 2, metadata !327, null}
!330 = metadata !{i32 284, i32 37, metadata !327, null}
!331 = metadata !{i32 285, i32 2, metadata !327, null}
!332 = metadata !{i32 285, i32 37, metadata !327, null}
!333 = metadata !{i32 286, i32 2, metadata !327, null}
!334 = metadata !{i32 286, i32 19, metadata !327, null}
!335 = metadata !{i32 287, i32 2, metadata !327, null}
!336 = metadata !{i32 287, i32 19, metadata !327, null}
!337 = metadata !{i32 288, i32 7, metadata !338, null}
!338 = metadata !{i32 720907, metadata !327, i32 288, i32 2, metadata !6, i32 288} ; [ DW_TAG_lexical_block ]
!339 = metadata !{i32 289, i32 3, metadata !340, null}
!340 = metadata !{i32 720907, metadata !338, i32 288, i32 29, metadata !6, i32 289} ; [ DW_TAG_lexical_block ]
!341 = metadata !{i32 289, i32 20, metadata !340, null}
!342 = metadata !{i32 290, i32 3, metadata !340, null}
!343 = metadata !{i32 290, i32 20, metadata !340, null}
!344 = metadata !{i32 291, i32 2, metadata !340, null}
!345 = metadata !{i32 288, i32 24, metadata !338, null}
!346 = metadata !{i32 292, i32 2, metadata !327, null}
!347 = metadata !{i32 293, i32 1, metadata !327, null}
!348 = metadata !{i32 721153, metadata !47, metadata !"u", metadata !6, i32 16777512, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!349 = metadata !{i32 296, i32 27, metadata !47, null}
!350 = metadata !{i32 721153, metadata !47, metadata !"v", metadata !6, i32 33554728, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!351 = metadata !{i32 296, i32 44, metadata !47, null}
!352 = metadata !{i32 721152, metadata !353, metadata !"t", metadata !6, i32 298, metadata !354, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!353 = metadata !{i32 720907, metadata !47, i32 297, i32 1, metadata !6, i32 290} ; [ DW_TAG_lexical_block ]
!354 = metadata !{i32 720918, null, metadata !"vlPoint", metadata !6, i32 17, i64 0, i64 0, i64 0, i32 0, metadata !355} ; [ DW_TAG_typedef ]
!355 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 304, i64 16, i32 0, i32 0, metadata !56, metadata !356, i32 0, i32 0} ; [ DW_TAG_array_type ]
!356 = metadata !{metadata !357}
!357 = metadata !{i32 720929, i64 0, i64 18}      ; [ DW_TAG_subrange_type ]
!358 = metadata !{i32 298, i32 10, metadata !353, null}
!359 = metadata !{i32 721152, metadata !353, metadata !"shift", metadata !6, i32 299, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!360 = metadata !{i32 299, i32 6, metadata !353, null}
!361 = metadata !{i32 299, i32 15, metadata !353, null}
!362 = metadata !{i32 301, i32 2, metadata !353, null}
!363 = metadata !{i32 301, i32 37, metadata !353, null}
!364 = metadata !{i32 302, i32 2, metadata !353, null}
!365 = metadata !{i32 302, i32 37, metadata !353, null}
!366 = metadata !{i32 303, i32 2, metadata !353, null}
!367 = metadata !{i32 303, i32 30, metadata !353, null}
!368 = metadata !{i32 304, i32 2, metadata !353, null}
!369 = metadata !{i32 305, i32 2, metadata !353, null}
!370 = metadata !{i32 305, i32 10, metadata !353, null}
!371 = metadata !{i32 307, i32 3, metadata !372, null}
!372 = metadata !{i32 720907, metadata !353, i32 306, i32 2, metadata !6, i32 291} ; [ DW_TAG_lexical_block ]
!373 = metadata !{i32 308, i32 3, metadata !372, null}
!374 = metadata !{i32 309, i32 2, metadata !372, null}
!375 = metadata !{i32 310, i32 2, metadata !353, null}
!376 = metadata !{i32 312, i32 8, metadata !377, null}
!377 = metadata !{i32 720907, metadata !353, i32 311, i32 2, metadata !6, i32 292} ; [ DW_TAG_lexical_block ]
!378 = metadata !{i32 314, i32 4, metadata !379, null}
!379 = metadata !{i32 720907, metadata !377, i32 313, i32 3, metadata !6, i32 293} ; [ DW_TAG_lexical_block ]
!380 = metadata !{i32 316, i32 5, metadata !381, null}
!381 = metadata !{i32 720907, metadata !379, i32 315, i32 4, metadata !6, i32 294} ; [ DW_TAG_lexical_block ]
!382 = metadata !{i32 317, i32 5, metadata !381, null}
!383 = metadata !{i32 318, i32 4, metadata !381, null}
!384 = metadata !{i32 320, i32 5, metadata !379, null}
!385 = metadata !{i32 321, i32 3, metadata !379, null}
!386 = metadata !{i32 323, i32 4, metadata !377, null}
!387 = metadata !{i32 324, i32 2, metadata !377, null}
!388 = metadata !{i32 325, i32 1, metadata !353, null}
!389 = metadata !{i32 721153, metadata !50, metadata !"u", metadata !6, i32 16777603, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!390 = metadata !{i32 387, i32 24, metadata !50, null}
!391 = metadata !{i32 721153, metadata !50, metadata !"v", metadata !6, i32 33554819, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!392 = metadata !{i32 387, i32 41, metadata !50, null}
!393 = metadata !{i32 721153, metadata !50, metadata !"w", metadata !6, i32 50332035, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!394 = metadata !{i32 387, i32 58, metadata !50, null}
!395 = metadata !{i32 721153, metadata !50, metadata !"m", metadata !6, i32 67109251, metadata !54, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!396 = metadata !{i32 387, i32 75, metadata !50, null}
!397 = metadata !{i32 721152, metadata !398, metadata !"t", metadata !6, i32 389, metadata !354, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!398 = metadata !{i32 720907, metadata !50, i32 388, i32 1, metadata !6, i32 295} ; [ DW_TAG_lexical_block ]
!399 = metadata !{i32 389, i32 10, metadata !398, null}
!400 = metadata !{i32 721152, metadata !398, metadata !"i", metadata !6, i32 390, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!401 = metadata !{i32 390, i32 6, metadata !398, null}
!402 = metadata !{i32 721152, metadata !398, metadata !"j", metadata !6, i32 390, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!403 = metadata !{i32 390, i32 8, metadata !398, null}
!404 = metadata !{i32 392, i32 2, metadata !398, null}
!405 = metadata !{i32 392, i32 37, metadata !398, null}
!406 = metadata !{i32 393, i32 2, metadata !398, null}
!407 = metadata !{i32 393, i32 37, metadata !398, null}
!408 = metadata !{i32 394, i32 2, metadata !398, null}
!409 = metadata !{i32 394, i32 37, metadata !398, null}
!410 = metadata !{i32 395, i32 2, metadata !398, null}
!411 = metadata !{i32 395, i32 37, metadata !398, null}
!412 = metadata !{i32 396, i32 2, metadata !398, null}
!413 = metadata !{i32 396, i32 30, metadata !398, null}
!414 = metadata !{i32 397, i32 2, metadata !398, null}
!415 = metadata !{i32 398, i32 2, metadata !398, null}
!416 = metadata !{i32 399, i32 7, metadata !417, null}
!417 = metadata !{i32 720907, metadata !398, i32 399, i32 2, metadata !6, i32 296} ; [ DW_TAG_lexical_block ]
!418 = metadata !{i32 401, i32 8, metadata !419, null}
!419 = metadata !{i32 720907, metadata !420, i32 401, i32 3, metadata !6, i32 298} ; [ DW_TAG_lexical_block ]
!420 = metadata !{i32 720907, metadata !417, i32 400, i32 2, metadata !6, i32 297} ; [ DW_TAG_lexical_block ]
!421 = metadata !{i32 403, i32 4, metadata !422, null}
!422 = metadata !{i32 720907, metadata !419, i32 402, i32 3, metadata !6, i32 299} ; [ DW_TAG_lexical_block ]
!423 = metadata !{i32 405, i32 5, metadata !424, null}
!424 = metadata !{i32 720907, metadata !422, i32 404, i32 4, metadata !6, i32 300} ; [ DW_TAG_lexical_block ]
!425 = metadata !{i32 406, i32 5, metadata !424, null}
!426 = metadata !{i32 407, i32 4, metadata !424, null}
!427 = metadata !{i32 408, i32 4, metadata !422, null}
!428 = metadata !{i32 409, i32 4, metadata !422, null}
!429 = metadata !{i32 410, i32 3, metadata !422, null}
!430 = metadata !{i32 401, i32 17, metadata !419, null}
!431 = metadata !{i32 411, i32 2, metadata !420, null}
!432 = metadata !{i32 399, i32 19, metadata !417, null}
!433 = metadata !{i32 412, i32 1, metadata !398, null}
