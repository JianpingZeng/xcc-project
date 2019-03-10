; ModuleID = 'ec_field.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

@logt = internal global i16* null, align 4
@expt = internal global i16* null, align 4
@.str = private unnamed_addr constant [17 x i8] c"p != ((void *)0)\00", align 1
@.str1 = private unnamed_addr constant [11 x i8] c"ec_field.c\00", align 1
@__PRETTY_FUNCTION__.gfEqual = private unnamed_addr constant [42 x i8] c"int gfEqual(const lunit *, const lunit *)\00", align 1
@.str2 = private unnamed_addr constant [17 x i8] c"q != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.gfClear = private unnamed_addr constant [22 x i8] c"void gfClear(lunit *)\00", align 1
@__PRETTY_FUNCTION__.gfCopy = private unnamed_addr constant [36 x i8] c"void gfCopy(lunit *, const lunit *)\00", align 1
@.str3 = private unnamed_addr constant [43 x i8] c"logt != ((void *)0) && expt != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.gfAdd = private unnamed_addr constant [50 x i8] c"void gfAdd(lunit *, const lunit *, const lunit *)\00", align 1
@.str4 = private unnamed_addr constant [17 x i8] c"r != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.gfMultiply = private unnamed_addr constant [55 x i8] c"void gfMultiply(lunit *, const lunit *, const lunit *)\00", align 1
@.str5 = private unnamed_addr constant [7 x i8] c"r != p\00", align 1
@.str6 = private unnamed_addr constant [7 x i8] c"r != q\00", align 1
@__PRETTY_FUNCTION__.gfSquare = private unnamed_addr constant [38 x i8] c"void gfSquare(lunit *, const lunit *)\00", align 1
@__PRETTY_FUNCTION__.gfSmallDiv = private unnamed_addr constant [32 x i8] c"void gfSmallDiv(lunit *, lunit)\00", align 1
@.str7 = private unnamed_addr constant [7 x i8] c"b != 0\00", align 1
@__PRETTY_FUNCTION__.gfInvert = private unnamed_addr constant [37 x i8] c"int gfInvert(lunit *, const lunit *)\00", align 1
@.str8 = private unnamed_addr constant [17 x i8] c"b != ((void *)0)\00", align 1
@.str9 = private unnamed_addr constant [17 x i8] c"a != ((void *)0)\00", align 1
@.str10 = private unnamed_addr constant [7 x i8] c"b != a\00", align 1
@.str11 = private unnamed_addr constant [10 x i8] c"f[1] != 0\00", align 1
@.str12 = private unnamed_addr constant [10 x i8] c"g[1] != 0\00", align 1
@__PRETTY_FUNCTION__.gfSquareRoot = private unnamed_addr constant [34 x i8] c"void gfSquareRoot(lunit *, lunit)\00", align 1
@__PRETTY_FUNCTION__.gfTrace = private unnamed_addr constant [27 x i8] c"int gfTrace(const lunit *)\00", align 1
@__PRETTY_FUNCTION__.gfQuadSolve = private unnamed_addr constant [40 x i8] c"int gfQuadSolve(lunit *, const lunit *)\00", align 1
@.str13 = private unnamed_addr constant [20 x i8] c"beta != ((void *)0)\00", align 1
@.str14 = private unnamed_addr constant [10 x i8] c"p != beta\00", align 1
@__PRETTY_FUNCTION__.gfYbit = private unnamed_addr constant [26 x i8] c"int gfYbit(const lunit *)\00", align 1
@__PRETTY_FUNCTION__.gfPack = private unnamed_addr constant [37 x i8] c"void gfPack(const lunit *, word16 *)\00", align 1
@.str15 = private unnamed_addr constant [17 x i8] c"k != ((void *)0)\00", align 1
@__PRETTY_FUNCTION__.gfUnpack = private unnamed_addr constant [39 x i8] c"void gfUnpack(lunit *, const word16 *)\00", align 1
@__PRETTY_FUNCTION__.gfAddMul = private unnamed_addr constant [46 x i8] c"void gfAddMul(lunit *, ltemp, ltemp, lunit *)\00", align 1

define arm_aapcscc i32 @gfInit() nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %root = alloca i16, align 2
  %i = alloca i16, align 2
  %j = alloca i16, align 2
  call void @llvm.dbg.declare(metadata !{i16* %root}, metadata !73), !dbg !76
  call void @llvm.dbg.declare(metadata !{i16* %i}, metadata !77), !dbg !78
  call void @llvm.dbg.declare(metadata !{i16* %j}, metadata !79), !dbg !80
  %0 = load i16** @logt, align 4, !dbg !81
  %cmp = icmp ne i16* %0, null, !dbg !81
  br i1 %cmp, label %land.lhs.true, label %if.end, !dbg !81

land.lhs.true:                                    ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !81
  %cmp1 = icmp ne i16* %1, null, !dbg !81
  br i1 %cmp1, label %if.then, label %if.end, !dbg !81

if.then:                                          ; preds = %land.lhs.true
  store i32 0, i32* %retval, !dbg !82
  br label %return, !dbg !82

if.end:                                           ; preds = %land.lhs.true, %entry
  %2 = load i16** @logt, align 4, !dbg !84
  %cmp2 = icmp ne i16* %2, null, !dbg !84
  br i1 %cmp2, label %land.lhs.true3, label %lor.lhs.false, !dbg !84

land.lhs.true3:                                   ; preds = %if.end
  %3 = load i16** @expt, align 4, !dbg !84
  %cmp4 = icmp eq i16* %3, null, !dbg !84
  br i1 %cmp4, label %if.then8, label %lor.lhs.false, !dbg !84

lor.lhs.false:                                    ; preds = %land.lhs.true3, %if.end
  %4 = load i16** @logt, align 4, !dbg !84
  %cmp5 = icmp eq i16* %4, null, !dbg !84
  br i1 %cmp5, label %land.lhs.true6, label %if.end9, !dbg !84

land.lhs.true6:                                   ; preds = %lor.lhs.false
  %5 = load i16** @expt, align 4, !dbg !84
  %cmp7 = icmp ne i16* %5, null, !dbg !84
  br i1 %cmp7, label %if.then8, label %if.end9, !dbg !84

if.then8:                                         ; preds = %land.lhs.true6, %land.lhs.true3
  store i32 2, i32* %retval, !dbg !85
  br label %return, !dbg !85

if.end9:                                          ; preds = %land.lhs.true6, %lor.lhs.false
  %call = call arm_aapcscc  noalias i8* @malloc(i32 65536) nounwind, !dbg !87
  %6 = bitcast i8* %call to i16*, !dbg !87
  store i16* %6, i16** @logt, align 4, !dbg !87
  %cmp10 = icmp eq i16* %6, null, !dbg !87
  br i1 %cmp10, label %if.then11, label %if.end12, !dbg !87

if.then11:                                        ; preds = %if.end9
  store i32 1, i32* %retval, !dbg !88
  br label %return, !dbg !88

if.end12:                                         ; preds = %if.end9
  %call13 = call arm_aapcscc  noalias i8* @malloc(i32 65536) nounwind, !dbg !90
  %7 = bitcast i8* %call13 to i16*, !dbg !90
  store i16* %7, i16** @expt, align 4, !dbg !90
  %cmp14 = icmp eq i16* %7, null, !dbg !90
  br i1 %cmp14, label %if.then15, label %if.end16, !dbg !90

if.then15:                                        ; preds = %if.end12
  %8 = load i16** @logt, align 4, !dbg !91
  %9 = bitcast i16* %8 to i8*, !dbg !91
  call arm_aapcscc  void @free(i8* %9) nounwind, !dbg !91
  store i16* null, i16** @logt, align 4, !dbg !93
  store i32 1, i32* %retval, !dbg !94
  br label %return, !dbg !94

if.end16:                                         ; preds = %if.end12
  store i16 -32765, i16* %root, align 2, !dbg !95
  %10 = load i16** @expt, align 4, !dbg !96
  %arrayidx = getelementptr inbounds i16* %10, i32 0, !dbg !96
  store i16 1, i16* %arrayidx, !dbg !96
  store i16 1, i16* %i, align 2, !dbg !97
  br label %for.cond, !dbg !97

for.cond:                                         ; preds = %for.inc, %if.end16
  %11 = load i16* %i, align 2, !dbg !97
  %conv = zext i16 %11 to i32, !dbg !97
  %cmp17 = icmp ult i32 %conv, 32768, !dbg !97
  br i1 %cmp17, label %for.body, label %for.end, !dbg !97

for.body:                                         ; preds = %for.cond
  %12 = load i16* %i, align 2, !dbg !99
  %conv19 = zext i16 %12 to i32, !dbg !99
  %sub = sub nsw i32 %conv19, 1, !dbg !99
  %13 = load i16** @expt, align 4, !dbg !99
  %arrayidx20 = getelementptr inbounds i16* %13, i32 %sub, !dbg !99
  %14 = load i16* %arrayidx20, !dbg !99
  %conv21 = zext i16 %14 to i32, !dbg !99
  %shl = shl i32 %conv21, 1, !dbg !99
  %conv22 = trunc i32 %shl to i16, !dbg !99
  store i16 %conv22, i16* %j, align 2, !dbg !99
  %15 = load i16* %j, align 2, !dbg !101
  %conv23 = zext i16 %15 to i32, !dbg !101
  %and = and i32 %conv23, 32768, !dbg !101
  %tobool = icmp ne i32 %and, 0, !dbg !101
  br i1 %tobool, label %if.then24, label %if.end28, !dbg !101

if.then24:                                        ; preds = %for.body
  %16 = load i16* %root, align 2, !dbg !102
  %conv25 = zext i16 %16 to i32, !dbg !102
  %17 = load i16* %j, align 2, !dbg !102
  %conv26 = zext i16 %17 to i32, !dbg !102
  %xor = xor i32 %conv26, %conv25, !dbg !102
  %conv27 = trunc i32 %xor to i16, !dbg !102
  store i16 %conv27, i16* %j, align 2, !dbg !102
  br label %if.end28, !dbg !104

if.end28:                                         ; preds = %if.then24, %for.body
  %18 = load i16* %j, align 2, !dbg !105
  %19 = load i16* %i, align 2, !dbg !105
  %idxprom = zext i16 %19 to i32, !dbg !105
  %20 = load i16** @expt, align 4, !dbg !105
  %arrayidx29 = getelementptr inbounds i16* %20, i32 %idxprom, !dbg !105
  store i16 %18, i16* %arrayidx29, !dbg !105
  br label %for.inc, !dbg !106

for.inc:                                          ; preds = %if.end28
  %21 = load i16* %i, align 2, !dbg !107
  %inc = add i16 %21, 1, !dbg !107
  store i16 %inc, i16* %i, align 2, !dbg !107
  br label %for.cond, !dbg !107

for.end:                                          ; preds = %for.cond
  store i16 0, i16* %i, align 2, !dbg !108
  br label %for.cond30, !dbg !108

for.cond30:                                       ; preds = %for.inc39, %for.end
  %22 = load i16* %i, align 2, !dbg !108
  %conv31 = zext i16 %22 to i32, !dbg !108
  %cmp32 = icmp ult i32 %conv31, 32767, !dbg !108
  br i1 %cmp32, label %for.body34, label %for.end41, !dbg !108

for.body34:                                       ; preds = %for.cond30
  %23 = load i16* %i, align 2, !dbg !110
  %24 = load i16* %i, align 2, !dbg !110
  %idxprom35 = zext i16 %24 to i32, !dbg !110
  %25 = load i16** @expt, align 4, !dbg !110
  %arrayidx36 = getelementptr inbounds i16* %25, i32 %idxprom35, !dbg !110
  %26 = load i16* %arrayidx36, !dbg !110
  %idxprom37 = zext i16 %26 to i32, !dbg !110
  %27 = load i16** @logt, align 4, !dbg !110
  %arrayidx38 = getelementptr inbounds i16* %27, i32 %idxprom37, !dbg !110
  store i16 %23, i16* %arrayidx38, !dbg !110
  br label %for.inc39, !dbg !112

for.inc39:                                        ; preds = %for.body34
  %28 = load i16* %i, align 2, !dbg !113
  %inc40 = add i16 %28, 1, !dbg !113
  store i16 %inc40, i16* %i, align 2, !dbg !113
  br label %for.cond30, !dbg !113

for.end41:                                        ; preds = %for.cond30
  %29 = load i16** @logt, align 4, !dbg !114
  %arrayidx42 = getelementptr inbounds i16* %29, i32 0, !dbg !114
  store i16 32767, i16* %arrayidx42, !dbg !114
  store i32 0, i32* %retval, !dbg !115
  br label %return, !dbg !115

return:                                           ; preds = %for.end41, %if.then15, %if.then11, %if.then8, %if.then
  %30 = load i32* %retval, !dbg !116
  ret i32 %30, !dbg !116
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc noalias i8* @malloc(i32) nounwind

declare arm_aapcscc void @free(i8*) nounwind

define arm_aapcscc void @gfQuit() nounwind uwtable {
entry:
  %0 = load i16** @expt, align 4, !dbg !117
  %tobool = icmp ne i16* %0, null, !dbg !117
  br i1 %tobool, label %if.then, label %if.end, !dbg !117

if.then:                                          ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !119
  %2 = bitcast i16* %1 to i8*, !dbg !119
  call arm_aapcscc  void @free(i8* %2) nounwind, !dbg !119
  store i16* null, i16** @expt, align 4, !dbg !121
  br label %if.end, !dbg !122

if.end:                                           ; preds = %if.then, %entry
  %3 = load i16** @logt, align 4, !dbg !123
  %tobool1 = icmp ne i16* %3, null, !dbg !123
  br i1 %tobool1, label %if.then2, label %if.end3, !dbg !123

if.then2:                                         ; preds = %if.end
  %4 = load i16** @logt, align 4, !dbg !124
  %5 = bitcast i16* %4 to i8*, !dbg !124
  call arm_aapcscc  void @free(i8* %5) nounwind, !dbg !124
  store i16* null, i16** @logt, align 4, !dbg !126
  br label %if.end3, !dbg !127

if.end3:                                          ; preds = %if.then2, %if.end
  ret void, !dbg !128
}

define arm_aapcscc i32 @gfEqual(i16* %p, i16* %q) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %q.addr = alloca i16*, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !129), !dbg !132
  store i16* %q, i16** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q.addr}, metadata !133), !dbg !134
  %0 = load i16** %p.addr, align 4, !dbg !135
  %cmp = icmp ne i16* %0, null, !dbg !135
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !135

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !135

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 183, i8* getelementptr inbounds ([42 x i8]* @__PRETTY_FUNCTION__.gfEqual, i32 0, i32 0)) noreturn nounwind, !dbg !137
  unreachable, !dbg !137
                                                  ; No predecessors!
  br label %cond.end, !dbg !137

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %q.addr, align 4, !dbg !138
  %cmp1 = icmp ne i16* %2, null, !dbg !138
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !138

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !138

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 184, i8* getelementptr inbounds ([42 x i8]* @__PRETTY_FUNCTION__.gfEqual, i32 0, i32 0)) noreturn nounwind, !dbg !139
  unreachable, !dbg !139
                                                  ; No predecessors!
  br label %cond.end4, !dbg !139

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %p.addr, align 4, !dbg !140
  %5 = bitcast i16* %4 to i8*, !dbg !140
  %6 = load i16** %q.addr, align 4, !dbg !140
  %7 = bitcast i16* %6 to i8*, !dbg !140
  %8 = load i16** %p.addr, align 4, !dbg !140
  %arrayidx = getelementptr inbounds i16* %8, i32 0, !dbg !140
  %9 = load i16* %arrayidx, !dbg !140
  %conv = zext i16 %9 to i32, !dbg !140
  %add = add nsw i32 %conv, 1, !dbg !140
  %call = call arm_aapcscc  i32 @memcmp(i8* %5, i8* %7, i32 %add) nounwind readonly, !dbg !140
  %tobool = icmp ne i32 %call, 0, !dbg !140
  %cond = select i1 %tobool, i32 0, i32 1, !dbg !140
  ret i32 %cond, !dbg !140
}

declare arm_aapcscc void @__assert_fail(i8*, i8*, i32, i8*) noreturn nounwind

declare arm_aapcscc i32 @memcmp(i8*, i8*, i32) nounwind readonly

define arm_aapcscc void @gfClear(i16* %p) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !141), !dbg !142
  %0 = load i16** %p.addr, align 4, !dbg !143
  %cmp = icmp ne i16* %0, null, !dbg !143
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !143

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !143

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 192, i8* getelementptr inbounds ([22 x i8]* @__PRETTY_FUNCTION__.gfClear, i32 0, i32 0)) noreturn nounwind, !dbg !145
  unreachable, !dbg !145
                                                  ; No predecessors!
  br label %cond.end, !dbg !145

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %p.addr, align 4, !dbg !146
  %3 = bitcast i16* %2 to i8*, !dbg !146
  call void @llvm.memset.p0i8.i32(i8* %3, i8 0, i32 72, i32 1, i1 false), !dbg !146
  ret void, !dbg !147
}

declare void @llvm.memset.p0i8.i32(i8* nocapture, i8, i32, i32, i1) nounwind

define arm_aapcscc void @gfCopy(i16* %p, i16* %q) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %q.addr = alloca i16*, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !148), !dbg !149
  store i16* %q, i16** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q.addr}, metadata !150), !dbg !151
  %0 = load i16** %p.addr, align 4, !dbg !152
  %cmp = icmp ne i16* %0, null, !dbg !152
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !152

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !152

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 200, i8* getelementptr inbounds ([36 x i8]* @__PRETTY_FUNCTION__.gfCopy, i32 0, i32 0)) noreturn nounwind, !dbg !154
  unreachable, !dbg !154
                                                  ; No predecessors!
  br label %cond.end, !dbg !154

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %q.addr, align 4, !dbg !155
  %cmp1 = icmp ne i16* %2, null, !dbg !155
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !155

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !155

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 201, i8* getelementptr inbounds ([36 x i8]* @__PRETTY_FUNCTION__.gfCopy, i32 0, i32 0)) noreturn nounwind, !dbg !156
  unreachable, !dbg !156
                                                  ; No predecessors!
  br label %cond.end4, !dbg !156

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %p.addr, align 4, !dbg !157
  %5 = bitcast i16* %4 to i8*, !dbg !157
  %6 = load i16** %q.addr, align 4, !dbg !157
  %7 = bitcast i16* %6 to i8*, !dbg !157
  %8 = load i16** %q.addr, align 4, !dbg !157
  %arrayidx = getelementptr inbounds i16* %8, i32 0, !dbg !157
  %9 = load i16* %arrayidx, !dbg !157
  %conv = zext i16 %9 to i32, !dbg !157
  %add = add nsw i32 %conv, 1, !dbg !157
  %mul = mul i32 %add, 2, !dbg !157
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %5, i8* %7, i32 %mul, i32 1, i1 false), !dbg !157
  ret void, !dbg !158
}

declare void @llvm.memcpy.p0i8.p0i8.i32(i8* nocapture, i8* nocapture, i32, i32, i1) nounwind

define arm_aapcscc void @gfAdd(i16* %p, i16* %q, i16* %r) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %q.addr = alloca i16*, align 4
  %r.addr = alloca i16*, align 4
  %i = alloca i16, align 2
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !159), !dbg !160
  store i16* %q, i16** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q.addr}, metadata !161), !dbg !162
  store i16* %r, i16** %r.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %r.addr}, metadata !163), !dbg !164
  call void @llvm.dbg.declare(metadata !{i16* %i}, metadata !165), !dbg !167
  %0 = load i16** @logt, align 4, !dbg !168
  %cmp = icmp ne i16* %0, null, !dbg !168
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !168

land.lhs.true:                                    ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !168
  %cmp1 = icmp ne i16* %1, null, !dbg !168
  br i1 %cmp1, label %cond.true, label %cond.false, !dbg !168

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !168

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 211, i8* getelementptr inbounds ([50 x i8]* @__PRETTY_FUNCTION__.gfAdd, i32 0, i32 0)) noreturn nounwind, !dbg !169
  unreachable, !dbg !169
                                                  ; No predecessors!
  br label %cond.end, !dbg !169

cond.end:                                         ; preds = %2, %cond.true
  %3 = load i16** %p.addr, align 4, !dbg !170
  %cmp2 = icmp ne i16* %3, null, !dbg !170
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !170

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !170

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 212, i8* getelementptr inbounds ([50 x i8]* @__PRETTY_FUNCTION__.gfAdd, i32 0, i32 0)) noreturn nounwind, !dbg !171
  unreachable, !dbg !171
                                                  ; No predecessors!
  br label %cond.end5, !dbg !171

cond.end5:                                        ; preds = %4, %cond.true3
  %5 = load i16** %q.addr, align 4, !dbg !172
  %cmp6 = icmp ne i16* %5, null, !dbg !172
  br i1 %cmp6, label %cond.true7, label %cond.false8, !dbg !172

cond.true7:                                       ; preds = %cond.end5
  br label %cond.end9, !dbg !172

cond.false8:                                      ; preds = %cond.end5
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 213, i8* getelementptr inbounds ([50 x i8]* @__PRETTY_FUNCTION__.gfAdd, i32 0, i32 0)) noreturn nounwind, !dbg !173
  unreachable, !dbg !173
                                                  ; No predecessors!
  br label %cond.end9, !dbg !173

cond.end9:                                        ; preds = %6, %cond.true7
  %7 = load i16** %r.addr, align 4, !dbg !174
  %cmp10 = icmp ne i16* %7, null, !dbg !174
  br i1 %cmp10, label %cond.true11, label %cond.false12, !dbg !174

cond.true11:                                      ; preds = %cond.end9
  br label %cond.end13, !dbg !174

cond.false12:                                     ; preds = %cond.end9
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str4, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 214, i8* getelementptr inbounds ([50 x i8]* @__PRETTY_FUNCTION__.gfAdd, i32 0, i32 0)) noreturn nounwind, !dbg !175
  unreachable, !dbg !175
                                                  ; No predecessors!
  br label %cond.end13, !dbg !175

cond.end13:                                       ; preds = %8, %cond.true11
  %9 = load i16** %q.addr, align 4, !dbg !176
  %arrayidx = getelementptr inbounds i16* %9, i32 0, !dbg !176
  %10 = load i16* %arrayidx, !dbg !176
  %conv = zext i16 %10 to i32, !dbg !176
  %11 = load i16** %r.addr, align 4, !dbg !176
  %arrayidx14 = getelementptr inbounds i16* %11, i32 0, !dbg !176
  %12 = load i16* %arrayidx14, !dbg !176
  %conv15 = zext i16 %12 to i32, !dbg !176
  %cmp16 = icmp sgt i32 %conv, %conv15, !dbg !176
  br i1 %cmp16, label %if.then, label %if.else, !dbg !176

if.then:                                          ; preds = %cond.end13
  store i16 1, i16* %i, align 2, !dbg !177
  br label %for.cond, !dbg !177

for.cond:                                         ; preds = %for.inc, %if.then
  %13 = load i16* %i, align 2, !dbg !177
  %conv18 = zext i16 %13 to i32, !dbg !177
  %14 = load i16** %r.addr, align 4, !dbg !177
  %arrayidx19 = getelementptr inbounds i16* %14, i32 0, !dbg !177
  %15 = load i16* %arrayidx19, !dbg !177
  %conv20 = zext i16 %15 to i32, !dbg !177
  %cmp21 = icmp sle i32 %conv18, %conv20, !dbg !177
  br i1 %cmp21, label %for.body, label %for.end, !dbg !177

for.body:                                         ; preds = %for.cond
  %16 = load i16* %i, align 2, !dbg !180
  %idxprom = zext i16 %16 to i32, !dbg !180
  %17 = load i16** %q.addr, align 4, !dbg !180
  %arrayidx23 = getelementptr inbounds i16* %17, i32 %idxprom, !dbg !180
  %18 = load i16* %arrayidx23, !dbg !180
  %conv24 = zext i16 %18 to i32, !dbg !180
  %19 = load i16* %i, align 2, !dbg !180
  %idxprom25 = zext i16 %19 to i32, !dbg !180
  %20 = load i16** %r.addr, align 4, !dbg !180
  %arrayidx26 = getelementptr inbounds i16* %20, i32 %idxprom25, !dbg !180
  %21 = load i16* %arrayidx26, !dbg !180
  %conv27 = zext i16 %21 to i32, !dbg !180
  %xor = xor i32 %conv24, %conv27, !dbg !180
  %conv28 = trunc i32 %xor to i16, !dbg !180
  %22 = load i16* %i, align 2, !dbg !180
  %idxprom29 = zext i16 %22 to i32, !dbg !180
  %23 = load i16** %p.addr, align 4, !dbg !180
  %arrayidx30 = getelementptr inbounds i16* %23, i32 %idxprom29, !dbg !180
  store i16 %conv28, i16* %arrayidx30, !dbg !180
  br label %for.inc, !dbg !182

for.inc:                                          ; preds = %for.body
  %24 = load i16* %i, align 2, !dbg !183
  %inc = add i16 %24, 1, !dbg !183
  store i16 %inc, i16* %i, align 2, !dbg !183
  br label %for.cond, !dbg !183

for.end:                                          ; preds = %for.cond
  %25 = load i16* %i, align 2, !dbg !184
  %idxprom31 = zext i16 %25 to i32, !dbg !184
  %26 = load i16** %p.addr, align 4, !dbg !184
  %arrayidx32 = getelementptr inbounds i16* %26, i32 %idxprom31, !dbg !184
  %27 = bitcast i16* %arrayidx32 to i8*, !dbg !184
  %28 = load i16* %i, align 2, !dbg !184
  %idxprom33 = zext i16 %28 to i32, !dbg !184
  %29 = load i16** %q.addr, align 4, !dbg !184
  %arrayidx34 = getelementptr inbounds i16* %29, i32 %idxprom33, !dbg !184
  %30 = bitcast i16* %arrayidx34 to i8*, !dbg !184
  %31 = load i16** %q.addr, align 4, !dbg !184
  %arrayidx35 = getelementptr inbounds i16* %31, i32 0, !dbg !184
  %32 = load i16* %arrayidx35, !dbg !184
  %conv36 = zext i16 %32 to i32, !dbg !184
  %33 = load i16** %r.addr, align 4, !dbg !184
  %arrayidx37 = getelementptr inbounds i16* %33, i32 0, !dbg !184
  %34 = load i16* %arrayidx37, !dbg !184
  %conv38 = zext i16 %34 to i32, !dbg !184
  %sub = sub nsw i32 %conv36, %conv38, !dbg !184
  %mul = mul i32 %sub, 2, !dbg !184
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %27, i8* %30, i32 %mul, i32 1, i1 false), !dbg !184
  %35 = load i16** %q.addr, align 4, !dbg !185
  %arrayidx39 = getelementptr inbounds i16* %35, i32 0, !dbg !185
  %36 = load i16* %arrayidx39, !dbg !185
  %37 = load i16** %p.addr, align 4, !dbg !185
  %arrayidx40 = getelementptr inbounds i16* %37, i32 0, !dbg !185
  store i16 %36, i16* %arrayidx40, !dbg !185
  br label %if.end117, !dbg !186

if.else:                                          ; preds = %cond.end13
  %38 = load i16** %q.addr, align 4, !dbg !187
  %arrayidx41 = getelementptr inbounds i16* %38, i32 0, !dbg !187
  %39 = load i16* %arrayidx41, !dbg !187
  %conv42 = zext i16 %39 to i32, !dbg !187
  %40 = load i16** %r.addr, align 4, !dbg !187
  %arrayidx43 = getelementptr inbounds i16* %40, i32 0, !dbg !187
  %41 = load i16* %arrayidx43, !dbg !187
  %conv44 = zext i16 %41 to i32, !dbg !187
  %cmp45 = icmp slt i32 %conv42, %conv44, !dbg !187
  br i1 %cmp45, label %if.then47, label %if.else80, !dbg !187

if.then47:                                        ; preds = %if.else
  store i16 1, i16* %i, align 2, !dbg !188
  br label %for.cond48, !dbg !188

for.cond48:                                       ; preds = %for.inc65, %if.then47
  %42 = load i16* %i, align 2, !dbg !188
  %conv49 = zext i16 %42 to i32, !dbg !188
  %43 = load i16** %q.addr, align 4, !dbg !188
  %arrayidx50 = getelementptr inbounds i16* %43, i32 0, !dbg !188
  %44 = load i16* %arrayidx50, !dbg !188
  %conv51 = zext i16 %44 to i32, !dbg !188
  %cmp52 = icmp sle i32 %conv49, %conv51, !dbg !188
  br i1 %cmp52, label %for.body54, label %for.end67, !dbg !188

for.body54:                                       ; preds = %for.cond48
  %45 = load i16* %i, align 2, !dbg !191
  %idxprom55 = zext i16 %45 to i32, !dbg !191
  %46 = load i16** %q.addr, align 4, !dbg !191
  %arrayidx56 = getelementptr inbounds i16* %46, i32 %idxprom55, !dbg !191
  %47 = load i16* %arrayidx56, !dbg !191
  %conv57 = zext i16 %47 to i32, !dbg !191
  %48 = load i16* %i, align 2, !dbg !191
  %idxprom58 = zext i16 %48 to i32, !dbg !191
  %49 = load i16** %r.addr, align 4, !dbg !191
  %arrayidx59 = getelementptr inbounds i16* %49, i32 %idxprom58, !dbg !191
  %50 = load i16* %arrayidx59, !dbg !191
  %conv60 = zext i16 %50 to i32, !dbg !191
  %xor61 = xor i32 %conv57, %conv60, !dbg !191
  %conv62 = trunc i32 %xor61 to i16, !dbg !191
  %51 = load i16* %i, align 2, !dbg !191
  %idxprom63 = zext i16 %51 to i32, !dbg !191
  %52 = load i16** %p.addr, align 4, !dbg !191
  %arrayidx64 = getelementptr inbounds i16* %52, i32 %idxprom63, !dbg !191
  store i16 %conv62, i16* %arrayidx64, !dbg !191
  br label %for.inc65, !dbg !193

for.inc65:                                        ; preds = %for.body54
  %53 = load i16* %i, align 2, !dbg !194
  %inc66 = add i16 %53, 1, !dbg !194
  store i16 %inc66, i16* %i, align 2, !dbg !194
  br label %for.cond48, !dbg !194

for.end67:                                        ; preds = %for.cond48
  %54 = load i16* %i, align 2, !dbg !195
  %idxprom68 = zext i16 %54 to i32, !dbg !195
  %55 = load i16** %p.addr, align 4, !dbg !195
  %arrayidx69 = getelementptr inbounds i16* %55, i32 %idxprom68, !dbg !195
  %56 = bitcast i16* %arrayidx69 to i8*, !dbg !195
  %57 = load i16* %i, align 2, !dbg !195
  %idxprom70 = zext i16 %57 to i32, !dbg !195
  %58 = load i16** %r.addr, align 4, !dbg !195
  %arrayidx71 = getelementptr inbounds i16* %58, i32 %idxprom70, !dbg !195
  %59 = bitcast i16* %arrayidx71 to i8*, !dbg !195
  %60 = load i16** %r.addr, align 4, !dbg !195
  %arrayidx72 = getelementptr inbounds i16* %60, i32 0, !dbg !195
  %61 = load i16* %arrayidx72, !dbg !195
  %conv73 = zext i16 %61 to i32, !dbg !195
  %62 = load i16** %q.addr, align 4, !dbg !195
  %arrayidx74 = getelementptr inbounds i16* %62, i32 0, !dbg !195
  %63 = load i16* %arrayidx74, !dbg !195
  %conv75 = zext i16 %63 to i32, !dbg !195
  %sub76 = sub nsw i32 %conv73, %conv75, !dbg !195
  %mul77 = mul i32 %sub76, 2, !dbg !195
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %56, i8* %59, i32 %mul77, i32 1, i1 false), !dbg !195
  %64 = load i16** %r.addr, align 4, !dbg !196
  %arrayidx78 = getelementptr inbounds i16* %64, i32 0, !dbg !196
  %65 = load i16* %arrayidx78, !dbg !196
  %66 = load i16** %p.addr, align 4, !dbg !196
  %arrayidx79 = getelementptr inbounds i16* %66, i32 0, !dbg !196
  store i16 %65, i16* %arrayidx79, !dbg !196
  br label %if.end116, !dbg !197

if.else80:                                        ; preds = %if.else
  %67 = load i16** %q.addr, align 4, !dbg !198
  %arrayidx81 = getelementptr inbounds i16* %67, i32 0, !dbg !198
  %68 = load i16* %arrayidx81, !dbg !198
  store i16 %68, i16* %i, align 2, !dbg !198
  br label %for.cond82, !dbg !198

for.cond82:                                       ; preds = %for.inc95, %if.else80
  %69 = load i16* %i, align 2, !dbg !198
  %conv83 = zext i16 %69 to i32, !dbg !198
  %cmp84 = icmp sgt i32 %conv83, 0, !dbg !198
  br i1 %cmp84, label %for.body86, label %for.end96, !dbg !198

for.body86:                                       ; preds = %for.cond82
  %70 = load i16* %i, align 2, !dbg !201
  %idxprom87 = zext i16 %70 to i32, !dbg !201
  %71 = load i16** %q.addr, align 4, !dbg !201
  %arrayidx88 = getelementptr inbounds i16* %71, i32 %idxprom87, !dbg !201
  %72 = load i16* %arrayidx88, !dbg !201
  %conv89 = zext i16 %72 to i32, !dbg !201
  %73 = load i16* %i, align 2, !dbg !201
  %idxprom90 = zext i16 %73 to i32, !dbg !201
  %74 = load i16** %r.addr, align 4, !dbg !201
  %arrayidx91 = getelementptr inbounds i16* %74, i32 %idxprom90, !dbg !201
  %75 = load i16* %arrayidx91, !dbg !201
  %conv92 = zext i16 %75 to i32, !dbg !201
  %xor93 = xor i32 %conv89, %conv92, !dbg !201
  %tobool = icmp ne i32 %xor93, 0, !dbg !201
  br i1 %tobool, label %if.then94, label %if.end, !dbg !201

if.then94:                                        ; preds = %for.body86
  br label %for.end96, !dbg !203

if.end:                                           ; preds = %for.body86
  br label %for.inc95, !dbg !205

for.inc95:                                        ; preds = %if.end
  %76 = load i16* %i, align 2, !dbg !206
  %dec = add i16 %76, -1, !dbg !206
  store i16 %dec, i16* %i, align 2, !dbg !206
  br label %for.cond82, !dbg !206

for.end96:                                        ; preds = %if.then94, %for.cond82
  %77 = load i16* %i, align 2, !dbg !207
  %78 = load i16** %p.addr, align 4, !dbg !207
  %arrayidx97 = getelementptr inbounds i16* %78, i32 0, !dbg !207
  store i16 %77, i16* %arrayidx97, !dbg !207
  br label %for.cond98, !dbg !207

for.cond98:                                       ; preds = %for.inc113, %for.end96
  %79 = load i16* %i, align 2, !dbg !207
  %conv99 = zext i16 %79 to i32, !dbg !207
  %cmp100 = icmp sgt i32 %conv99, 0, !dbg !207
  br i1 %cmp100, label %for.body102, label %for.end115, !dbg !207

for.body102:                                      ; preds = %for.cond98
  %80 = load i16* %i, align 2, !dbg !209
  %idxprom103 = zext i16 %80 to i32, !dbg !209
  %81 = load i16** %q.addr, align 4, !dbg !209
  %arrayidx104 = getelementptr inbounds i16* %81, i32 %idxprom103, !dbg !209
  %82 = load i16* %arrayidx104, !dbg !209
  %conv105 = zext i16 %82 to i32, !dbg !209
  %83 = load i16* %i, align 2, !dbg !209
  %idxprom106 = zext i16 %83 to i32, !dbg !209
  %84 = load i16** %r.addr, align 4, !dbg !209
  %arrayidx107 = getelementptr inbounds i16* %84, i32 %idxprom106, !dbg !209
  %85 = load i16* %arrayidx107, !dbg !209
  %conv108 = zext i16 %85 to i32, !dbg !209
  %xor109 = xor i32 %conv105, %conv108, !dbg !209
  %conv110 = trunc i32 %xor109 to i16, !dbg !209
  %86 = load i16* %i, align 2, !dbg !209
  %idxprom111 = zext i16 %86 to i32, !dbg !209
  %87 = load i16** %p.addr, align 4, !dbg !209
  %arrayidx112 = getelementptr inbounds i16* %87, i32 %idxprom111, !dbg !209
  store i16 %conv110, i16* %arrayidx112, !dbg !209
  br label %for.inc113, !dbg !211

for.inc113:                                       ; preds = %for.body102
  %88 = load i16* %i, align 2, !dbg !212
  %dec114 = add i16 %88, -1, !dbg !212
  store i16 %dec114, i16* %i, align 2, !dbg !212
  br label %for.cond98, !dbg !212

for.end115:                                       ; preds = %for.cond98
  br label %if.end116

if.end116:                                        ; preds = %for.end115, %for.end67
  br label %if.end117

if.end117:                                        ; preds = %if.end116, %for.end
  ret void, !dbg !213
}

define arm_aapcscc void @gfMultiply(i16* %r, i16* %p, i16* %q) nounwind uwtable {
entry:
  %r.addr = alloca i16*, align 4
  %p.addr = alloca i16*, align 4
  %q.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  %j = alloca i32, align 4
  %x = alloca i16, align 2
  %log_pi = alloca i16, align 2
  %log_qj = alloca i16, align 2
  %lg = alloca [19 x i16], align 2
  store i16* %r, i16** %r.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %r.addr}, metadata !214), !dbg !215
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !216), !dbg !217
  store i16* %q, i16** %q.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %q.addr}, metadata !218), !dbg !219
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !220), !dbg !222
  call void @llvm.dbg.declare(metadata !{i32* %j}, metadata !223), !dbg !224
  call void @llvm.dbg.declare(metadata !{i16* %x}, metadata !225), !dbg !226
  call void @llvm.dbg.declare(metadata !{i16* %log_pi}, metadata !227), !dbg !228
  call void @llvm.dbg.declare(metadata !{i16* %log_qj}, metadata !229), !dbg !230
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %lg}, metadata !231), !dbg !235
  %0 = load i16** @logt, align 4, !dbg !236
  %cmp = icmp ne i16* %0, null, !dbg !236
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !236

land.lhs.true:                                    ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !236
  %cmp1 = icmp ne i16* %1, null, !dbg !236
  br i1 %cmp1, label %cond.true, label %cond.false, !dbg !236

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !236

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 275, i8* getelementptr inbounds ([55 x i8]* @__PRETTY_FUNCTION__.gfMultiply, i32 0, i32 0)) noreturn nounwind, !dbg !237
  unreachable, !dbg !237
                                                  ; No predecessors!
  br label %cond.end, !dbg !237

cond.end:                                         ; preds = %2, %cond.true
  %3 = load i16** %p.addr, align 4, !dbg !238
  %cmp2 = icmp ne i16* %3, null, !dbg !238
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !238

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !238

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 276, i8* getelementptr inbounds ([55 x i8]* @__PRETTY_FUNCTION__.gfMultiply, i32 0, i32 0)) noreturn nounwind, !dbg !239
  unreachable, !dbg !239
                                                  ; No predecessors!
  br label %cond.end5, !dbg !239

cond.end5:                                        ; preds = %4, %cond.true3
  %5 = load i16** %q.addr, align 4, !dbg !240
  %cmp6 = icmp ne i16* %5, null, !dbg !240
  br i1 %cmp6, label %cond.true7, label %cond.false8, !dbg !240

cond.true7:                                       ; preds = %cond.end5
  br label %cond.end9, !dbg !240

cond.false8:                                      ; preds = %cond.end5
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str2, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 277, i8* getelementptr inbounds ([55 x i8]* @__PRETTY_FUNCTION__.gfMultiply, i32 0, i32 0)) noreturn nounwind, !dbg !241
  unreachable, !dbg !241
                                                  ; No predecessors!
  br label %cond.end9, !dbg !241

cond.end9:                                        ; preds = %6, %cond.true7
  %7 = load i16** %r.addr, align 4, !dbg !242
  %8 = load i16** %p.addr, align 4, !dbg !242
  %cmp10 = icmp ne i16* %7, %8, !dbg !242
  br i1 %cmp10, label %cond.true11, label %cond.false12, !dbg !242

cond.true11:                                      ; preds = %cond.end9
  br label %cond.end13, !dbg !242

cond.false12:                                     ; preds = %cond.end9
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([7 x i8]* @.str5, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 278, i8* getelementptr inbounds ([55 x i8]* @__PRETTY_FUNCTION__.gfMultiply, i32 0, i32 0)) noreturn nounwind, !dbg !243
  unreachable, !dbg !243
                                                  ; No predecessors!
  br label %cond.end13, !dbg !243

cond.end13:                                       ; preds = %9, %cond.true11
  %10 = load i16** %r.addr, align 4, !dbg !244
  %11 = load i16** %q.addr, align 4, !dbg !244
  %cmp14 = icmp ne i16* %10, %11, !dbg !244
  br i1 %cmp14, label %cond.true15, label %cond.false16, !dbg !244

cond.true15:                                      ; preds = %cond.end13
  br label %cond.end17, !dbg !244

cond.false16:                                     ; preds = %cond.end13
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([7 x i8]* @.str6, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 279, i8* getelementptr inbounds ([55 x i8]* @__PRETTY_FUNCTION__.gfMultiply, i32 0, i32 0)) noreturn nounwind, !dbg !245
  unreachable, !dbg !245
                                                  ; No predecessors!
  br label %cond.end17, !dbg !245

cond.end17:                                       ; preds = %12, %cond.true15
  %13 = load i16** %p.addr, align 4, !dbg !246
  %arrayidx = getelementptr inbounds i16* %13, i32 0, !dbg !246
  %14 = load i16* %arrayidx, !dbg !246
  %conv = zext i16 %14 to i32, !dbg !246
  %tobool = icmp ne i32 %conv, 0, !dbg !246
  br i1 %tobool, label %land.lhs.true18, label %if.else, !dbg !246

land.lhs.true18:                                  ; preds = %cond.end17
  %15 = load i16** %q.addr, align 4, !dbg !246
  %arrayidx19 = getelementptr inbounds i16* %15, i32 0, !dbg !246
  %16 = load i16* %arrayidx19, !dbg !246
  %conv20 = zext i16 %16 to i32, !dbg !246
  %tobool21 = icmp ne i32 %conv20, 0, !dbg !246
  br i1 %tobool21, label %if.then, label %if.else, !dbg !246

if.then:                                          ; preds = %land.lhs.true18
  %17 = load i16** %q.addr, align 4, !dbg !247
  %arrayidx22 = getelementptr inbounds i16* %17, i32 0, !dbg !247
  %18 = load i16* %arrayidx22, !dbg !247
  %conv23 = zext i16 %18 to i32, !dbg !247
  store i32 %conv23, i32* %j, align 4, !dbg !247
  br label %for.cond, !dbg !247

for.cond:                                         ; preds = %for.inc, %if.then
  %19 = load i32* %j, align 4, !dbg !247
  %tobool24 = icmp ne i32 %19, 0, !dbg !247
  br i1 %tobool24, label %for.body, label %for.end, !dbg !247

for.body:                                         ; preds = %for.cond
  %20 = load i32* %j, align 4, !dbg !250
  %21 = load i16** %q.addr, align 4, !dbg !250
  %arrayidx25 = getelementptr inbounds i16* %21, i32 %20, !dbg !250
  %22 = load i16* %arrayidx25, !dbg !250
  %idxprom = zext i16 %22 to i32, !dbg !250
  %23 = load i16** @logt, align 4, !dbg !250
  %arrayidx26 = getelementptr inbounds i16* %23, i32 %idxprom, !dbg !250
  %24 = load i16* %arrayidx26, !dbg !250
  %25 = load i32* %j, align 4, !dbg !250
  %arrayidx27 = getelementptr inbounds [19 x i16]* %lg, i32 0, i32 %25, !dbg !250
  store i16 %24, i16* %arrayidx27, align 2, !dbg !250
  br label %for.inc, !dbg !252

for.inc:                                          ; preds = %for.body
  %26 = load i32* %j, align 4, !dbg !253
  %dec = add nsw i32 %26, -1, !dbg !253
  store i32 %dec, i32* %j, align 4, !dbg !253
  br label %for.cond, !dbg !253

for.end:                                          ; preds = %for.cond
  %27 = load i16** %r.addr, align 4, !dbg !254
  call arm_aapcscc  void @gfClear(i16* %27), !dbg !254
  %28 = load i16** %p.addr, align 4, !dbg !255
  %arrayidx28 = getelementptr inbounds i16* %28, i32 0, !dbg !255
  %29 = load i16* %arrayidx28, !dbg !255
  %conv29 = zext i16 %29 to i32, !dbg !255
  store i32 %conv29, i32* %i, align 4, !dbg !255
  br label %for.cond30, !dbg !255

for.cond30:                                       ; preds = %for.inc72, %for.end
  %30 = load i32* %i, align 4, !dbg !255
  %tobool31 = icmp ne i32 %30, 0, !dbg !255
  br i1 %tobool31, label %for.body32, label %for.end74, !dbg !255

for.body32:                                       ; preds = %for.cond30
  %31 = load i32* %i, align 4, !dbg !257
  %32 = load i16** %p.addr, align 4, !dbg !257
  %arrayidx33 = getelementptr inbounds i16* %32, i32 %31, !dbg !257
  %33 = load i16* %arrayidx33, !dbg !257
  %idxprom34 = zext i16 %33 to i32, !dbg !257
  %34 = load i16** @logt, align 4, !dbg !257
  %arrayidx35 = getelementptr inbounds i16* %34, i32 %idxprom34, !dbg !257
  %35 = load i16* %arrayidx35, !dbg !257
  store i16 %35, i16* %log_pi, align 2, !dbg !257
  %conv36 = zext i16 %35 to i32, !dbg !257
  %cmp37 = icmp ne i32 %conv36, 32767, !dbg !257
  br i1 %cmp37, label %if.then39, label %if.end71, !dbg !257

if.then39:                                        ; preds = %for.body32
  %36 = load i16** %q.addr, align 4, !dbg !259
  %arrayidx40 = getelementptr inbounds i16* %36, i32 0, !dbg !259
  %37 = load i16* %arrayidx40, !dbg !259
  %conv41 = zext i16 %37 to i32, !dbg !259
  store i32 %conv41, i32* %j, align 4, !dbg !259
  br label %for.cond42, !dbg !259

for.cond42:                                       ; preds = %for.inc68, %if.then39
  %38 = load i32* %j, align 4, !dbg !259
  %tobool43 = icmp ne i32 %38, 0, !dbg !259
  br i1 %tobool43, label %for.body44, label %for.end70, !dbg !259

for.body44:                                       ; preds = %for.cond42
  %39 = load i32* %j, align 4, !dbg !262
  %arrayidx45 = getelementptr inbounds [19 x i16]* %lg, i32 0, i32 %39, !dbg !262
  %40 = load i16* %arrayidx45, align 2, !dbg !262
  store i16 %40, i16* %log_qj, align 2, !dbg !262
  %conv46 = zext i16 %40 to i32, !dbg !262
  %cmp47 = icmp ne i32 %conv46, 32767, !dbg !262
  br i1 %cmp47, label %if.then49, label %if.end, !dbg !262

if.then49:                                        ; preds = %for.body44
  %41 = load i16* %log_pi, align 2, !dbg !264
  %conv50 = zext i16 %41 to i32, !dbg !264
  %42 = load i16* %log_qj, align 2, !dbg !264
  %conv51 = zext i16 %42 to i32, !dbg !264
  %add = add nsw i32 %conv50, %conv51, !dbg !264
  %conv52 = trunc i32 %add to i16, !dbg !264
  store i16 %conv52, i16* %x, align 2, !dbg !264
  %conv53 = zext i16 %conv52 to i32, !dbg !264
  %cmp54 = icmp uge i32 %conv53, 32767, !dbg !264
  br i1 %cmp54, label %cond.true56, label %cond.false58, !dbg !264

cond.true56:                                      ; preds = %if.then49
  %43 = load i16* %x, align 2, !dbg !264
  %conv57 = zext i16 %43 to i32, !dbg !264
  %sub = sub i32 %conv57, 32767, !dbg !264
  br label %cond.end60, !dbg !264

cond.false58:                                     ; preds = %if.then49
  %44 = load i16* %x, align 2, !dbg !264
  %conv59 = zext i16 %44 to i32, !dbg !264
  br label %cond.end60, !dbg !264

cond.end60:                                       ; preds = %cond.false58, %cond.true56
  %cond = phi i32 [ %sub, %cond.true56 ], [ %conv59, %cond.false58 ], !dbg !264
  %45 = load i16** @expt, align 4, !dbg !264
  %arrayidx61 = getelementptr inbounds i16* %45, i32 %cond, !dbg !264
  %46 = load i16* %arrayidx61, !dbg !264
  %conv62 = zext i16 %46 to i32, !dbg !264
  %47 = load i32* %i, align 4, !dbg !264
  %48 = load i32* %j, align 4, !dbg !264
  %add63 = add nsw i32 %47, %48, !dbg !264
  %sub64 = sub nsw i32 %add63, 1, !dbg !264
  %49 = load i16** %r.addr, align 4, !dbg !264
  %arrayidx65 = getelementptr inbounds i16* %49, i32 %sub64, !dbg !264
  %50 = load i16* %arrayidx65, !dbg !264
  %conv66 = zext i16 %50 to i32, !dbg !264
  %xor = xor i32 %conv66, %conv62, !dbg !264
  %conv67 = trunc i32 %xor to i16, !dbg !264
  store i16 %conv67, i16* %arrayidx65, !dbg !264
  br label %if.end, !dbg !266

if.end:                                           ; preds = %cond.end60, %for.body44
  br label %for.inc68, !dbg !267

for.inc68:                                        ; preds = %if.end
  %51 = load i32* %j, align 4, !dbg !268
  %dec69 = add nsw i32 %51, -1, !dbg !268
  store i32 %dec69, i32* %j, align 4, !dbg !268
  br label %for.cond42, !dbg !268

for.end70:                                        ; preds = %for.cond42
  br label %if.end71, !dbg !269

if.end71:                                         ; preds = %for.end70, %for.body32
  br label %for.inc72, !dbg !270

for.inc72:                                        ; preds = %if.end71
  %52 = load i32* %i, align 4, !dbg !271
  %dec73 = add nsw i32 %52, -1, !dbg !271
  store i32 %dec73, i32* %i, align 4, !dbg !271
  br label %for.cond30, !dbg !271

for.end74:                                        ; preds = %for.cond30
  %53 = load i16** %p.addr, align 4, !dbg !272
  %arrayidx75 = getelementptr inbounds i16* %53, i32 0, !dbg !272
  %54 = load i16* %arrayidx75, !dbg !272
  %conv76 = zext i16 %54 to i32, !dbg !272
  %55 = load i16** %q.addr, align 4, !dbg !272
  %arrayidx77 = getelementptr inbounds i16* %55, i32 0, !dbg !272
  %56 = load i16* %arrayidx77, !dbg !272
  %conv78 = zext i16 %56 to i32, !dbg !272
  %add79 = add nsw i32 %conv76, %conv78, !dbg !272
  %sub80 = sub nsw i32 %add79, 1, !dbg !272
  %conv81 = trunc i32 %sub80 to i16, !dbg !272
  %57 = load i16** %r.addr, align 4, !dbg !272
  %arrayidx82 = getelementptr inbounds i16* %57, i32 0, !dbg !272
  store i16 %conv81, i16* %arrayidx82, !dbg !272
  %58 = load i16** %r.addr, align 4, !dbg !273
  call arm_aapcscc  void @gfReduce(i16* %58), !dbg !273
  br label %if.end84, !dbg !274

if.else:                                          ; preds = %land.lhs.true18, %cond.end17
  %59 = load i16** %r.addr, align 4, !dbg !275
  %arrayidx83 = getelementptr inbounds i16* %59, i32 0, !dbg !275
  store i16 0, i16* %arrayidx83, !dbg !275
  br label %if.end84

if.end84:                                         ; preds = %if.else, %for.end74
  store i16 0, i16* %log_qj, align 2, !dbg !277
  store i16 0, i16* %log_pi, align 2, !dbg !277
  store i16 0, i16* %x, align 2, !dbg !277
  %arraydecay = getelementptr inbounds [19 x i16]* %lg, i32 0, i32 0, !dbg !278
  %60 = bitcast i16* %arraydecay to i8*, !dbg !278
  call void @llvm.memset.p0i8.i32(i8* %60, i8 0, i32 38, i32 1, i1 false), !dbg !278
  ret void, !dbg !279
}

define internal arm_aapcscc void @gfReduce(i16* %p) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !280), !dbg !281
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !282), !dbg !284
  %0 = load i16** %p.addr, align 4, !dbg !285
  %arrayidx = getelementptr inbounds i16* %0, i32 0, !dbg !285
  %1 = load i16* %arrayidx, !dbg !285
  %conv = zext i16 %1 to i32, !dbg !285
  store i32 %conv, i32* %i, align 4, !dbg !285
  br label %for.cond, !dbg !285

for.cond:                                         ; preds = %for.inc, %entry
  %2 = load i32* %i, align 4, !dbg !285
  %cmp = icmp sgt i32 %2, 17, !dbg !285
  br i1 %cmp, label %for.body, label %for.end, !dbg !285

for.body:                                         ; preds = %for.cond
  %3 = load i32* %i, align 4, !dbg !287
  %4 = load i16** %p.addr, align 4, !dbg !287
  %arrayidx2 = getelementptr inbounds i16* %4, i32 %3, !dbg !287
  %5 = load i16* %arrayidx2, !dbg !287
  %conv3 = zext i16 %5 to i32, !dbg !287
  %6 = load i32* %i, align 4, !dbg !287
  %sub = sub nsw i32 %6, 17, !dbg !287
  %7 = load i16** %p.addr, align 4, !dbg !287
  %arrayidx4 = getelementptr inbounds i16* %7, i32 %sub, !dbg !287
  %8 = load i16* %arrayidx4, !dbg !287
  %conv5 = zext i16 %8 to i32, !dbg !287
  %xor = xor i32 %conv5, %conv3, !dbg !287
  %conv6 = trunc i32 %xor to i16, !dbg !287
  store i16 %conv6, i16* %arrayidx4, !dbg !287
  %9 = load i32* %i, align 4, !dbg !289
  %10 = load i16** %p.addr, align 4, !dbg !289
  %arrayidx7 = getelementptr inbounds i16* %10, i32 %9, !dbg !289
  %11 = load i16* %arrayidx7, !dbg !289
  %conv8 = zext i16 %11 to i32, !dbg !289
  %12 = load i32* %i, align 4, !dbg !289
  %add = add nsw i32 %12, 3, !dbg !289
  %sub9 = sub nsw i32 %add, 17, !dbg !289
  %13 = load i16** %p.addr, align 4, !dbg !289
  %arrayidx10 = getelementptr inbounds i16* %13, i32 %sub9, !dbg !289
  %14 = load i16* %arrayidx10, !dbg !289
  %conv11 = zext i16 %14 to i32, !dbg !289
  %xor12 = xor i32 %conv11, %conv8, !dbg !289
  %conv13 = trunc i32 %xor12 to i16, !dbg !289
  store i16 %conv13, i16* %arrayidx10, !dbg !289
  %15 = load i32* %i, align 4, !dbg !290
  %16 = load i16** %p.addr, align 4, !dbg !290
  %arrayidx14 = getelementptr inbounds i16* %16, i32 %15, !dbg !290
  store i16 0, i16* %arrayidx14, !dbg !290
  br label %for.inc, !dbg !291

for.inc:                                          ; preds = %for.body
  %17 = load i32* %i, align 4, !dbg !292
  %dec = add nsw i32 %17, -1, !dbg !292
  store i32 %dec, i32* %i, align 4, !dbg !292
  br label %for.cond, !dbg !292

for.end:                                          ; preds = %for.cond
  %18 = load i16** %p.addr, align 4, !dbg !293
  %arrayidx15 = getelementptr inbounds i16* %18, i32 0, !dbg !293
  %19 = load i16* %arrayidx15, !dbg !293
  %conv16 = zext i16 %19 to i32, !dbg !293
  %cmp17 = icmp sgt i32 %conv16, 17, !dbg !293
  br i1 %cmp17, label %if.then, label %if.end, !dbg !293

if.then:                                          ; preds = %for.end
  %20 = load i16** %p.addr, align 4, !dbg !294
  %arrayidx19 = getelementptr inbounds i16* %20, i32 0, !dbg !294
  store i16 17, i16* %arrayidx19, !dbg !294
  br label %while.cond, !dbg !296

while.cond:                                       ; preds = %while.body, %if.then
  %21 = load i16** %p.addr, align 4, !dbg !296
  %arrayidx20 = getelementptr inbounds i16* %21, i32 0, !dbg !296
  %22 = load i16* %arrayidx20, !dbg !296
  %conv21 = zext i16 %22 to i32, !dbg !296
  %tobool = icmp ne i32 %conv21, 0, !dbg !296
  br i1 %tobool, label %land.rhs, label %land.end, !dbg !296

land.rhs:                                         ; preds = %while.cond
  %23 = load i16** %p.addr, align 4, !dbg !296
  %arrayidx22 = getelementptr inbounds i16* %23, i32 0, !dbg !296
  %24 = load i16* %arrayidx22, !dbg !296
  %idxprom = zext i16 %24 to i32, !dbg !296
  %25 = load i16** %p.addr, align 4, !dbg !296
  %arrayidx23 = getelementptr inbounds i16* %25, i32 %idxprom, !dbg !296
  %26 = load i16* %arrayidx23, !dbg !296
  %conv24 = zext i16 %26 to i32, !dbg !296
  %cmp25 = icmp eq i32 %conv24, 0, !dbg !296
  br label %land.end

land.end:                                         ; preds = %land.rhs, %while.cond
  %27 = phi i1 [ false, %while.cond ], [ %cmp25, %land.rhs ]
  br i1 %27, label %while.body, label %while.end

while.body:                                       ; preds = %land.end
  %28 = load i16** %p.addr, align 4, !dbg !297
  %arrayidx27 = getelementptr inbounds i16* %28, i32 0, !dbg !297
  %29 = load i16* %arrayidx27, !dbg !297
  %dec28 = add i16 %29, -1, !dbg !297
  store i16 %dec28, i16* %arrayidx27, !dbg !297
  br label %while.cond, !dbg !299

while.end:                                        ; preds = %land.end
  br label %if.end, !dbg !300

if.end:                                           ; preds = %while.end, %for.end
  ret void, !dbg !301
}

define arm_aapcscc void @gfSquare(i16* %r, i16* %p) nounwind uwtable {
entry:
  %r.addr = alloca i16*, align 4
  %p.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  %x = alloca i16, align 2
  store i16* %r, i16** %r.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %r.addr}, metadata !302), !dbg !303
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !304), !dbg !305
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !306), !dbg !308
  call void @llvm.dbg.declare(metadata !{i16* %x}, metadata !309), !dbg !310
  %0 = load i16** @logt, align 4, !dbg !311
  %cmp = icmp ne i16* %0, null, !dbg !311
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !311

land.lhs.true:                                    ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !311
  %cmp1 = icmp ne i16* %1, null, !dbg !311
  br i1 %cmp1, label %cond.true, label %cond.false, !dbg !311

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !311

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 316, i8* getelementptr inbounds ([38 x i8]* @__PRETTY_FUNCTION__.gfSquare, i32 0, i32 0)) noreturn nounwind, !dbg !312
  unreachable, !dbg !312
                                                  ; No predecessors!
  br label %cond.end, !dbg !312

cond.end:                                         ; preds = %2, %cond.true
  %3 = load i16** %r.addr, align 4, !dbg !313
  %cmp2 = icmp ne i16* %3, null, !dbg !313
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !313

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !313

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str4, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 317, i8* getelementptr inbounds ([38 x i8]* @__PRETTY_FUNCTION__.gfSquare, i32 0, i32 0)) noreturn nounwind, !dbg !314
  unreachable, !dbg !314
                                                  ; No predecessors!
  br label %cond.end5, !dbg !314

cond.end5:                                        ; preds = %4, %cond.true3
  %5 = load i16** %p.addr, align 4, !dbg !315
  %cmp6 = icmp ne i16* %5, null, !dbg !315
  br i1 %cmp6, label %cond.true7, label %cond.false8, !dbg !315

cond.true7:                                       ; preds = %cond.end5
  br label %cond.end9, !dbg !315

cond.false8:                                      ; preds = %cond.end5
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 318, i8* getelementptr inbounds ([38 x i8]* @__PRETTY_FUNCTION__.gfSquare, i32 0, i32 0)) noreturn nounwind, !dbg !316
  unreachable, !dbg !316
                                                  ; No predecessors!
  br label %cond.end9, !dbg !316

cond.end9:                                        ; preds = %6, %cond.true7
  %7 = load i16** %p.addr, align 4, !dbg !317
  %arrayidx = getelementptr inbounds i16* %7, i32 0, !dbg !317
  %8 = load i16* %arrayidx, !dbg !317
  %tobool = icmp ne i16 %8, 0, !dbg !317
  br i1 %tobool, label %if.then, label %if.else76, !dbg !317

if.then:                                          ; preds = %cond.end9
  %9 = load i16** %p.addr, align 4, !dbg !318
  %arrayidx10 = getelementptr inbounds i16* %9, i32 0, !dbg !318
  %10 = load i16* %arrayidx10, !dbg !318
  %conv = zext i16 %10 to i32, !dbg !318
  store i32 %conv, i32* %i, align 4, !dbg !318
  %11 = load i32* %i, align 4, !dbg !320
  %12 = load i16** %p.addr, align 4, !dbg !320
  %arrayidx11 = getelementptr inbounds i16* %12, i32 %11, !dbg !320
  %13 = load i16* %arrayidx11, !dbg !320
  %idxprom = zext i16 %13 to i32, !dbg !320
  %14 = load i16** @logt, align 4, !dbg !320
  %arrayidx12 = getelementptr inbounds i16* %14, i32 %idxprom, !dbg !320
  %15 = load i16* %arrayidx12, !dbg !320
  store i16 %15, i16* %x, align 2, !dbg !320
  %conv13 = zext i16 %15 to i32, !dbg !320
  %cmp14 = icmp ne i32 %conv13, 32767, !dbg !320
  br i1 %cmp14, label %if.then16, label %if.else, !dbg !320

if.then16:                                        ; preds = %if.then
  %16 = load i16* %x, align 2, !dbg !321
  %conv17 = zext i16 %16 to i32, !dbg !321
  %17 = load i16* %x, align 2, !dbg !321
  %conv18 = zext i16 %17 to i32, !dbg !321
  %add = add nsw i32 %conv18, %conv17, !dbg !321
  %conv19 = trunc i32 %add to i16, !dbg !321
  store i16 %conv19, i16* %x, align 2, !dbg !321
  %conv20 = zext i16 %conv19 to i32, !dbg !321
  %cmp21 = icmp uge i32 %conv20, 32767, !dbg !321
  br i1 %cmp21, label %cond.true23, label %cond.false25, !dbg !321

cond.true23:                                      ; preds = %if.then16
  %18 = load i16* %x, align 2, !dbg !321
  %conv24 = zext i16 %18 to i32, !dbg !321
  %sub = sub i32 %conv24, 32767, !dbg !321
  br label %cond.end27, !dbg !321

cond.false25:                                     ; preds = %if.then16
  %19 = load i16* %x, align 2, !dbg !321
  %conv26 = zext i16 %19 to i32, !dbg !321
  br label %cond.end27, !dbg !321

cond.end27:                                       ; preds = %cond.false25, %cond.true23
  %cond = phi i32 [ %sub, %cond.true23 ], [ %conv26, %cond.false25 ], !dbg !321
  %20 = load i16** @expt, align 4, !dbg !321
  %arrayidx28 = getelementptr inbounds i16* %20, i32 %cond, !dbg !321
  %21 = load i16* %arrayidx28, !dbg !321
  %22 = load i32* %i, align 4, !dbg !321
  %mul = mul nsw i32 2, %22, !dbg !321
  %sub29 = sub nsw i32 %mul, 1, !dbg !321
  %23 = load i16** %r.addr, align 4, !dbg !321
  %arrayidx30 = getelementptr inbounds i16* %23, i32 %sub29, !dbg !321
  store i16 %21, i16* %arrayidx30, !dbg !321
  br label %if.end, !dbg !323

if.else:                                          ; preds = %if.then
  %24 = load i32* %i, align 4, !dbg !324
  %mul31 = mul nsw i32 2, %24, !dbg !324
  %sub32 = sub nsw i32 %mul31, 1, !dbg !324
  %25 = load i16** %r.addr, align 4, !dbg !324
  %arrayidx33 = getelementptr inbounds i16* %25, i32 %sub32, !dbg !324
  store i16 0, i16* %arrayidx33, !dbg !324
  br label %if.end

if.end:                                           ; preds = %if.else, %cond.end27
  %26 = load i16** %p.addr, align 4, !dbg !326
  %arrayidx34 = getelementptr inbounds i16* %26, i32 0, !dbg !326
  %27 = load i16* %arrayidx34, !dbg !326
  %conv35 = zext i16 %27 to i32, !dbg !326
  %sub36 = sub nsw i32 %conv35, 1, !dbg !326
  store i32 %sub36, i32* %i, align 4, !dbg !326
  br label %for.cond, !dbg !326

for.cond:                                         ; preds = %for.inc, %if.end
  %28 = load i32* %i, align 4, !dbg !326
  %tobool37 = icmp ne i32 %28, 0, !dbg !326
  br i1 %tobool37, label %for.body, label %for.end, !dbg !326

for.body:                                         ; preds = %for.cond
  %29 = load i32* %i, align 4, !dbg !328
  %mul38 = mul nsw i32 2, %29, !dbg !328
  %30 = load i16** %r.addr, align 4, !dbg !328
  %arrayidx39 = getelementptr inbounds i16* %30, i32 %mul38, !dbg !328
  store i16 0, i16* %arrayidx39, !dbg !328
  %31 = load i32* %i, align 4, !dbg !330
  %32 = load i16** %p.addr, align 4, !dbg !330
  %arrayidx40 = getelementptr inbounds i16* %32, i32 %31, !dbg !330
  %33 = load i16* %arrayidx40, !dbg !330
  %idxprom41 = zext i16 %33 to i32, !dbg !330
  %34 = load i16** @logt, align 4, !dbg !330
  %arrayidx42 = getelementptr inbounds i16* %34, i32 %idxprom41, !dbg !330
  %35 = load i16* %arrayidx42, !dbg !330
  store i16 %35, i16* %x, align 2, !dbg !330
  %conv43 = zext i16 %35 to i32, !dbg !330
  %cmp44 = icmp ne i32 %conv43, 32767, !dbg !330
  br i1 %cmp44, label %if.then46, label %if.else65, !dbg !330

if.then46:                                        ; preds = %for.body
  %36 = load i16* %x, align 2, !dbg !331
  %conv47 = zext i16 %36 to i32, !dbg !331
  %37 = load i16* %x, align 2, !dbg !331
  %conv48 = zext i16 %37 to i32, !dbg !331
  %add49 = add nsw i32 %conv48, %conv47, !dbg !331
  %conv50 = trunc i32 %add49 to i16, !dbg !331
  store i16 %conv50, i16* %x, align 2, !dbg !331
  %conv51 = zext i16 %conv50 to i32, !dbg !331
  %cmp52 = icmp uge i32 %conv51, 32767, !dbg !331
  br i1 %cmp52, label %cond.true54, label %cond.false57, !dbg !331

cond.true54:                                      ; preds = %if.then46
  %38 = load i16* %x, align 2, !dbg !331
  %conv55 = zext i16 %38 to i32, !dbg !331
  %sub56 = sub i32 %conv55, 32767, !dbg !331
  br label %cond.end59, !dbg !331

cond.false57:                                     ; preds = %if.then46
  %39 = load i16* %x, align 2, !dbg !331
  %conv58 = zext i16 %39 to i32, !dbg !331
  br label %cond.end59, !dbg !331

cond.end59:                                       ; preds = %cond.false57, %cond.true54
  %cond60 = phi i32 [ %sub56, %cond.true54 ], [ %conv58, %cond.false57 ], !dbg !331
  %40 = load i16** @expt, align 4, !dbg !331
  %arrayidx61 = getelementptr inbounds i16* %40, i32 %cond60, !dbg !331
  %41 = load i16* %arrayidx61, !dbg !331
  %42 = load i32* %i, align 4, !dbg !331
  %mul62 = mul nsw i32 2, %42, !dbg !331
  %sub63 = sub nsw i32 %mul62, 1, !dbg !331
  %43 = load i16** %r.addr, align 4, !dbg !331
  %arrayidx64 = getelementptr inbounds i16* %43, i32 %sub63, !dbg !331
  store i16 %41, i16* %arrayidx64, !dbg !331
  br label %if.end69, !dbg !333

if.else65:                                        ; preds = %for.body
  %44 = load i32* %i, align 4, !dbg !334
  %mul66 = mul nsw i32 2, %44, !dbg !334
  %sub67 = sub nsw i32 %mul66, 1, !dbg !334
  %45 = load i16** %r.addr, align 4, !dbg !334
  %arrayidx68 = getelementptr inbounds i16* %45, i32 %sub67, !dbg !334
  store i16 0, i16* %arrayidx68, !dbg !334
  br label %if.end69

if.end69:                                         ; preds = %if.else65, %cond.end59
  br label %for.inc, !dbg !336

for.inc:                                          ; preds = %if.end69
  %46 = load i32* %i, align 4, !dbg !337
  %dec = add nsw i32 %46, -1, !dbg !337
  store i32 %dec, i32* %i, align 4, !dbg !337
  br label %for.cond, !dbg !337

for.end:                                          ; preds = %for.cond
  %47 = load i16** %p.addr, align 4, !dbg !338
  %arrayidx70 = getelementptr inbounds i16* %47, i32 0, !dbg !338
  %48 = load i16* %arrayidx70, !dbg !338
  %conv71 = zext i16 %48 to i32, !dbg !338
  %mul72 = mul nsw i32 2, %conv71, !dbg !338
  %sub73 = sub nsw i32 %mul72, 1, !dbg !338
  %conv74 = trunc i32 %sub73 to i16, !dbg !338
  %49 = load i16** %r.addr, align 4, !dbg !338
  %arrayidx75 = getelementptr inbounds i16* %49, i32 0, !dbg !338
  store i16 %conv74, i16* %arrayidx75, !dbg !338
  %50 = load i16** %r.addr, align 4, !dbg !339
  call arm_aapcscc  void @gfReduce(i16* %50), !dbg !339
  br label %if.end78, !dbg !340

if.else76:                                        ; preds = %cond.end9
  %51 = load i16** %r.addr, align 4, !dbg !341
  %arrayidx77 = getelementptr inbounds i16* %51, i32 0, !dbg !341
  store i16 0, i16* %arrayidx77, !dbg !341
  br label %if.end78

if.end78:                                         ; preds = %if.else76, %for.end
  ret void, !dbg !343
}

define arm_aapcscc void @gfSmallDiv(i16* %p, i16 zeroext %b) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %b.addr = alloca i16, align 2
  %i = alloca i32, align 4
  %x = alloca i16, align 2
  %lb = alloca i16, align 2
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !344), !dbg !345
  store i16 %b, i16* %b.addr, align 2
  call void @llvm.dbg.declare(metadata !{i16* %b.addr}, metadata !346), !dbg !347
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !348), !dbg !350
  call void @llvm.dbg.declare(metadata !{i16* %x}, metadata !351), !dbg !352
  call void @llvm.dbg.declare(metadata !{i16* %lb}, metadata !353), !dbg !354
  %0 = load i16* %b.addr, align 2, !dbg !355
  %idxprom = zext i16 %0 to i32, !dbg !355
  %1 = load i16** @logt, align 4, !dbg !355
  %arrayidx = getelementptr inbounds i16* %1, i32 %idxprom, !dbg !355
  %2 = load i16* %arrayidx, !dbg !355
  store i16 %2, i16* %lb, align 2, !dbg !355
  %3 = load i16** @logt, align 4, !dbg !356
  %cmp = icmp ne i16* %3, null, !dbg !356
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !356

land.lhs.true:                                    ; preds = %entry
  %4 = load i16** @expt, align 4, !dbg !356
  %cmp1 = icmp ne i16* %4, null, !dbg !356
  br i1 %cmp1, label %cond.true, label %cond.false, !dbg !356

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !356

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 350, i8* getelementptr inbounds ([32 x i8]* @__PRETTY_FUNCTION__.gfSmallDiv, i32 0, i32 0)) noreturn nounwind, !dbg !357
  unreachable, !dbg !357
                                                  ; No predecessors!
  br label %cond.end, !dbg !357

cond.end:                                         ; preds = %5, %cond.true
  %6 = load i16** %p.addr, align 4, !dbg !358
  %cmp2 = icmp ne i16* %6, null, !dbg !358
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !358

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !358

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 351, i8* getelementptr inbounds ([32 x i8]* @__PRETTY_FUNCTION__.gfSmallDiv, i32 0, i32 0)) noreturn nounwind, !dbg !359
  unreachable, !dbg !359
                                                  ; No predecessors!
  br label %cond.end5, !dbg !359

cond.end5:                                        ; preds = %7, %cond.true3
  %8 = load i16* %b.addr, align 2, !dbg !360
  %conv = zext i16 %8 to i32, !dbg !360
  %cmp6 = icmp ne i32 %conv, 0, !dbg !360
  br i1 %cmp6, label %cond.true8, label %cond.false9, !dbg !360

cond.true8:                                       ; preds = %cond.end5
  br label %cond.end10, !dbg !360

cond.false9:                                      ; preds = %cond.end5
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([7 x i8]* @.str7, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 352, i8* getelementptr inbounds ([32 x i8]* @__PRETTY_FUNCTION__.gfSmallDiv, i32 0, i32 0)) noreturn nounwind, !dbg !361
  unreachable, !dbg !361
                                                  ; No predecessors!
  br label %cond.end10, !dbg !361

cond.end10:                                       ; preds = %9, %cond.true8
  %10 = load i16** %p.addr, align 4, !dbg !362
  %arrayidx11 = getelementptr inbounds i16* %10, i32 0, !dbg !362
  %11 = load i16* %arrayidx11, !dbg !362
  %conv12 = zext i16 %11 to i32, !dbg !362
  store i32 %conv12, i32* %i, align 4, !dbg !362
  br label %for.cond, !dbg !362

for.cond:                                         ; preds = %for.inc, %cond.end10
  %12 = load i32* %i, align 4, !dbg !362
  %tobool = icmp ne i32 %12, 0, !dbg !362
  br i1 %tobool, label %for.body, label %for.end, !dbg !362

for.body:                                         ; preds = %for.cond
  %13 = load i32* %i, align 4, !dbg !364
  %14 = load i16** %p.addr, align 4, !dbg !364
  %arrayidx13 = getelementptr inbounds i16* %14, i32 %13, !dbg !364
  %15 = load i16* %arrayidx13, !dbg !364
  %idxprom14 = zext i16 %15 to i32, !dbg !364
  %16 = load i16** @logt, align 4, !dbg !364
  %arrayidx15 = getelementptr inbounds i16* %16, i32 %idxprom14, !dbg !364
  %17 = load i16* %arrayidx15, !dbg !364
  store i16 %17, i16* %x, align 2, !dbg !364
  %conv16 = zext i16 %17 to i32, !dbg !364
  %cmp17 = icmp ne i32 %conv16, 32767, !dbg !364
  br i1 %cmp17, label %if.then, label %if.end, !dbg !364

if.then:                                          ; preds = %for.body
  %18 = load i16* %lb, align 2, !dbg !366
  %conv19 = zext i16 %18 to i32, !dbg !366
  %sub = sub i32 32767, %conv19, !dbg !366
  %19 = load i16* %x, align 2, !dbg !366
  %conv20 = zext i16 %19 to i32, !dbg !366
  %add = add i32 %conv20, %sub, !dbg !366
  %conv21 = trunc i32 %add to i16, !dbg !366
  store i16 %conv21, i16* %x, align 2, !dbg !366
  %conv22 = zext i16 %conv21 to i32, !dbg !366
  %cmp23 = icmp uge i32 %conv22, 32767, !dbg !366
  br i1 %cmp23, label %cond.true25, label %cond.false28, !dbg !366

cond.true25:                                      ; preds = %if.then
  %20 = load i16* %x, align 2, !dbg !366
  %conv26 = zext i16 %20 to i32, !dbg !366
  %sub27 = sub i32 %conv26, 32767, !dbg !366
  br label %cond.end30, !dbg !366

cond.false28:                                     ; preds = %if.then
  %21 = load i16* %x, align 2, !dbg !366
  %conv29 = zext i16 %21 to i32, !dbg !366
  br label %cond.end30, !dbg !366

cond.end30:                                       ; preds = %cond.false28, %cond.true25
  %cond = phi i32 [ %sub27, %cond.true25 ], [ %conv29, %cond.false28 ], !dbg !366
  %22 = load i16** @expt, align 4, !dbg !366
  %arrayidx31 = getelementptr inbounds i16* %22, i32 %cond, !dbg !366
  %23 = load i16* %arrayidx31, !dbg !366
  %24 = load i32* %i, align 4, !dbg !366
  %25 = load i16** %p.addr, align 4, !dbg !366
  %arrayidx32 = getelementptr inbounds i16* %25, i32 %24, !dbg !366
  store i16 %23, i16* %arrayidx32, !dbg !366
  br label %if.end, !dbg !368

if.end:                                           ; preds = %cond.end30, %for.body
  br label %for.inc, !dbg !369

for.inc:                                          ; preds = %if.end
  %26 = load i32* %i, align 4, !dbg !370
  %dec = add nsw i32 %26, -1, !dbg !370
  store i32 %dec, i32* %i, align 4, !dbg !370
  br label %for.cond, !dbg !370

for.end:                                          ; preds = %for.cond
  ret void, !dbg !371
}

define arm_aapcscc i32 @gfInvert(i16* %b, i16* %a) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %b.addr = alloca i16*, align 4
  %a.addr = alloca i16*, align 4
  %c = alloca [36 x i16], align 2
  %f = alloca [36 x i16], align 2
  %g = alloca [36 x i16], align 2
  %x = alloca i16, align 2
  %j = alloca i16, align 2
  %alpha = alloca i16, align 2
  store i16* %b, i16** %b.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %b.addr}, metadata !372), !dbg !373
  store i16* %a, i16** %a.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %a.addr}, metadata !374), !dbg !375
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %c}, metadata !376), !dbg !382
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %f}, metadata !383), !dbg !384
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %g}, metadata !385), !dbg !386
  call void @llvm.dbg.declare(metadata !{i16* %x}, metadata !387), !dbg !388
  call void @llvm.dbg.declare(metadata !{i16* %j}, metadata !389), !dbg !390
  call void @llvm.dbg.declare(metadata !{i16* %alpha}, metadata !391), !dbg !392
  %0 = load i16** @logt, align 4, !dbg !393
  %cmp = icmp ne i16* %0, null, !dbg !393
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !393

land.lhs.true:                                    ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !393
  %cmp1 = icmp ne i16* %1, null, !dbg !393
  br i1 %cmp1, label %cond.true, label %cond.false, !dbg !393

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !393

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 388, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.gfInvert, i32 0, i32 0)) noreturn nounwind, !dbg !394
  unreachable, !dbg !394
                                                  ; No predecessors!
  br label %cond.end, !dbg !394

cond.end:                                         ; preds = %2, %cond.true
  %3 = load i16** %b.addr, align 4, !dbg !395
  %cmp2 = icmp ne i16* %3, null, !dbg !395
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !395

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !395

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str8, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 389, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.gfInvert, i32 0, i32 0)) noreturn nounwind, !dbg !396
  unreachable, !dbg !396
                                                  ; No predecessors!
  br label %cond.end5, !dbg !396

cond.end5:                                        ; preds = %4, %cond.true3
  %5 = load i16** %a.addr, align 4, !dbg !397
  %cmp6 = icmp ne i16* %5, null, !dbg !397
  br i1 %cmp6, label %cond.true7, label %cond.false8, !dbg !397

cond.true7:                                       ; preds = %cond.end5
  br label %cond.end9, !dbg !397

cond.false8:                                      ; preds = %cond.end5
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str9, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 390, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.gfInvert, i32 0, i32 0)) noreturn nounwind, !dbg !398
  unreachable, !dbg !398
                                                  ; No predecessors!
  br label %cond.end9, !dbg !398

cond.end9:                                        ; preds = %6, %cond.true7
  %7 = load i16** %b.addr, align 4, !dbg !399
  %8 = load i16** %a.addr, align 4, !dbg !399
  %cmp10 = icmp ne i16* %7, %8, !dbg !399
  br i1 %cmp10, label %cond.true11, label %cond.false12, !dbg !399

cond.true11:                                      ; preds = %cond.end9
  br label %cond.end13, !dbg !399

cond.false12:                                     ; preds = %cond.end9
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([7 x i8]* @.str10, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 391, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.gfInvert, i32 0, i32 0)) noreturn nounwind, !dbg !400
  unreachable, !dbg !400
                                                  ; No predecessors!
  br label %cond.end13, !dbg !400

cond.end13:                                       ; preds = %9, %cond.true11
  %10 = load i16** %a.addr, align 4, !dbg !401
  %arrayidx = getelementptr inbounds i16* %10, i32 0, !dbg !401
  %11 = load i16* %arrayidx, !dbg !401
  %conv = zext i16 %11 to i32, !dbg !401
  %cmp14 = icmp eq i32 %conv, 0, !dbg !401
  br i1 %cmp14, label %if.then, label %if.end, !dbg !401

if.then:                                          ; preds = %cond.end13
  store i32 1, i32* %retval, !dbg !402
  br label %return, !dbg !402

if.end:                                           ; preds = %cond.end13
  %12 = load i16** %b.addr, align 4, !dbg !404
  %arrayidx16 = getelementptr inbounds i16* %12, i32 0, !dbg !404
  store i16 1, i16* %arrayidx16, !dbg !404
  %13 = load i16** %b.addr, align 4, !dbg !405
  %arrayidx17 = getelementptr inbounds i16* %13, i32 1, !dbg !405
  store i16 1, i16* %arrayidx17, !dbg !405
  %arrayidx18 = getelementptr inbounds [36 x i16]* %c, i32 0, i32 0, !dbg !406
  store i16 0, i16* %arrayidx18, align 2, !dbg !406
  %arraydecay = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !407
  %14 = load i16** %a.addr, align 4, !dbg !407
  call arm_aapcscc  void @gfCopy(i16* %arraydecay, i16* %14), !dbg !407
  %arraydecay19 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !408
  call arm_aapcscc  void @gfClear(i16* %arraydecay19), !dbg !408
  %arrayidx20 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !409
  store i16 18, i16* %arrayidx20, align 2, !dbg !409
  %arrayidx21 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 1, !dbg !410
  store i16 1, i16* %arrayidx21, align 2, !dbg !410
  %arrayidx22 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 4, !dbg !411
  store i16 1, i16* %arrayidx22, align 2, !dbg !411
  %arrayidx23 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 18, !dbg !412
  store i16 1, i16* %arrayidx23, align 2, !dbg !412
  br label %for.cond, !dbg !413

for.cond:                                         ; preds = %cond.end75, %if.end
  %arrayidx24 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !415
  %15 = load i16* %arrayidx24, align 2, !dbg !415
  %conv25 = zext i16 %15 to i32, !dbg !415
  %cmp26 = icmp eq i32 %conv25, 1, !dbg !415
  br i1 %cmp26, label %if.then28, label %if.end40, !dbg !415

if.then28:                                        ; preds = %for.cond
  %arrayidx29 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 1, !dbg !417
  %16 = load i16* %arrayidx29, align 2, !dbg !417
  %conv30 = zext i16 %16 to i32, !dbg !417
  %cmp31 = icmp ne i32 %conv30, 0, !dbg !417
  br i1 %cmp31, label %cond.true33, label %cond.false34, !dbg !417

cond.true33:                                      ; preds = %if.then28
  br label %cond.end35, !dbg !417

cond.false34:                                     ; preds = %if.then28
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([10 x i8]* @.str11, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 406, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.gfInvert, i32 0, i32 0)) noreturn nounwind, !dbg !419
  unreachable, !dbg !419
                                                  ; No predecessors!
  br label %cond.end35, !dbg !419

cond.end35:                                       ; preds = %17, %cond.true33
  %18 = load i16** %b.addr, align 4, !dbg !420
  %arrayidx36 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 1, !dbg !420
  %19 = load i16* %arrayidx36, align 2, !dbg !420
  call arm_aapcscc  void @gfSmallDiv(i16* %18, i16 zeroext %19), !dbg !420
  %arraydecay37 = getelementptr inbounds [36 x i16]* %c, i32 0, i32 0, !dbg !421
  call arm_aapcscc  void @gfClear(i16* %arraydecay37), !dbg !421
  %arraydecay38 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !422
  call arm_aapcscc  void @gfClear(i16* %arraydecay38), !dbg !422
  %arraydecay39 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !423
  call arm_aapcscc  void @gfClear(i16* %arraydecay39), !dbg !423
  store i16 0, i16* %alpha, align 2, !dbg !424
  store i16 0, i16* %j, align 2, !dbg !424
  store i16 0, i16* %x, align 2, !dbg !424
  store i32 0, i32* %retval, !dbg !425
  br label %return, !dbg !425

if.end40:                                         ; preds = %for.cond
  %arrayidx41 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !426
  %20 = load i16* %arrayidx41, align 2, !dbg !426
  %conv42 = zext i16 %20 to i32, !dbg !426
  %arrayidx43 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !426
  %21 = load i16* %arrayidx43, align 2, !dbg !426
  %conv44 = zext i16 %21 to i32, !dbg !426
  %cmp45 = icmp slt i32 %conv42, %conv44, !dbg !426
  br i1 %cmp45, label %if.then47, label %if.end48, !dbg !426

if.then47:                                        ; preds = %if.end40
  br label %SWAP_FG, !dbg !427

if.end48:                                         ; preds = %if.end40
  br label %SWAP_GF, !dbg !429

SWAP_GF:                                          ; preds = %if.then106, %if.end48
  %arrayidx49 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !430
  %22 = load i16* %arrayidx49, align 2, !dbg !430
  %conv50 = zext i16 %22 to i32, !dbg !430
  %arrayidx51 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !430
  %23 = load i16* %arrayidx51, align 2, !dbg !430
  %conv52 = zext i16 %23 to i32, !dbg !430
  %sub = sub nsw i32 %conv50, %conv52, !dbg !430
  %conv53 = trunc i32 %sub to i16, !dbg !430
  store i16 %conv53, i16* %j, align 2, !dbg !430
  %arrayidx54 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !431
  %24 = load i16* %arrayidx54, align 2, !dbg !431
  %idxprom = zext i16 %24 to i32, !dbg !431
  %arrayidx55 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 %idxprom, !dbg !431
  %25 = load i16* %arrayidx55, align 2, !dbg !431
  %idxprom56 = zext i16 %25 to i32, !dbg !431
  %26 = load i16** @logt, align 4, !dbg !431
  %arrayidx57 = getelementptr inbounds i16* %26, i32 %idxprom56, !dbg !431
  %27 = load i16* %arrayidx57, !dbg !431
  %conv58 = zext i16 %27 to i32, !dbg !431
  %arrayidx59 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !431
  %28 = load i16* %arrayidx59, align 2, !dbg !431
  %idxprom60 = zext i16 %28 to i32, !dbg !431
  %arrayidx61 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 %idxprom60, !dbg !431
  %29 = load i16* %arrayidx61, align 2, !dbg !431
  %idxprom62 = zext i16 %29 to i32, !dbg !431
  %30 = load i16** @logt, align 4, !dbg !431
  %arrayidx63 = getelementptr inbounds i16* %30, i32 %idxprom62, !dbg !431
  %31 = load i16* %arrayidx63, !dbg !431
  %conv64 = zext i16 %31 to i32, !dbg !431
  %sub65 = sub nsw i32 %conv58, %conv64, !dbg !431
  %add = add i32 %sub65, 32767, !dbg !431
  %conv66 = trunc i32 %add to i16, !dbg !431
  store i16 %conv66, i16* %x, align 2, !dbg !431
  %32 = load i16* %x, align 2, !dbg !432
  %conv67 = zext i16 %32 to i32, !dbg !432
  %cmp68 = icmp uge i32 %conv67, 32767, !dbg !432
  br i1 %cmp68, label %cond.true70, label %cond.false73, !dbg !432

cond.true70:                                      ; preds = %SWAP_GF
  %33 = load i16* %x, align 2, !dbg !432
  %conv71 = zext i16 %33 to i32, !dbg !432
  %sub72 = sub i32 %conv71, 32767, !dbg !432
  br label %cond.end75, !dbg !432

cond.false73:                                     ; preds = %SWAP_GF
  %34 = load i16* %x, align 2, !dbg !432
  %conv74 = zext i16 %34 to i32, !dbg !432
  br label %cond.end75, !dbg !432

cond.end75:                                       ; preds = %cond.false73, %cond.true70
  %cond = phi i32 [ %sub72, %cond.true70 ], [ %conv74, %cond.false73 ], !dbg !432
  %35 = load i16** @expt, align 4, !dbg !432
  %arrayidx76 = getelementptr inbounds i16* %35, i32 %cond, !dbg !432
  %36 = load i16* %arrayidx76, !dbg !432
  store i16 %36, i16* %alpha, align 2, !dbg !432
  %arraydecay77 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !433
  %37 = load i16* %alpha, align 2, !dbg !433
  %38 = load i16* %j, align 2, !dbg !433
  %arraydecay78 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !433
  call arm_aapcscc  void @gfAddMul(i16* %arraydecay77, i16 zeroext %37, i16 zeroext %38, i16* %arraydecay78), !dbg !433
  %39 = load i16** %b.addr, align 4, !dbg !434
  %40 = load i16* %alpha, align 2, !dbg !434
  %41 = load i16* %j, align 2, !dbg !434
  %arraydecay79 = getelementptr inbounds [36 x i16]* %c, i32 0, i32 0, !dbg !434
  call arm_aapcscc  void @gfAddMul(i16* %39, i16 zeroext %40, i16 zeroext %41, i16* %arraydecay79), !dbg !434
  br label %for.cond, !dbg !435
                                                  ; No predecessors!
  br label %for.cond80, !dbg !436

for.cond80:                                       ; preds = %cond.end137, %42
  %arrayidx81 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !438
  %43 = load i16* %arrayidx81, align 2, !dbg !438
  %conv82 = zext i16 %43 to i32, !dbg !438
  %cmp83 = icmp eq i32 %conv82, 1, !dbg !438
  br i1 %cmp83, label %if.then85, label %if.end99, !dbg !438

if.then85:                                        ; preds = %for.cond80
  %arrayidx86 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 1, !dbg !440
  %44 = load i16* %arrayidx86, align 2, !dbg !440
  %conv87 = zext i16 %44 to i32, !dbg !440
  %cmp88 = icmp ne i32 %conv87, 0, !dbg !440
  br i1 %cmp88, label %cond.true90, label %cond.false91, !dbg !440

cond.true90:                                      ; preds = %if.then85
  br label %cond.end92, !dbg !440

cond.false91:                                     ; preds = %if.then85
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([10 x i8]* @.str12, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 426, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.gfInvert, i32 0, i32 0)) noreturn nounwind, !dbg !442
  unreachable, !dbg !442
                                                  ; No predecessors!
  br label %cond.end92, !dbg !442

cond.end92:                                       ; preds = %45, %cond.true90
  %arraydecay93 = getelementptr inbounds [36 x i16]* %c, i32 0, i32 0, !dbg !443
  %arrayidx94 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 1, !dbg !443
  %46 = load i16* %arrayidx94, align 2, !dbg !443
  call arm_aapcscc  void @gfSmallDiv(i16* %arraydecay93, i16 zeroext %46), !dbg !443
  %47 = load i16** %b.addr, align 4, !dbg !444
  %arraydecay95 = getelementptr inbounds [36 x i16]* %c, i32 0, i32 0, !dbg !444
  call arm_aapcscc  void @gfCopy(i16* %47, i16* %arraydecay95), !dbg !444
  %arraydecay96 = getelementptr inbounds [36 x i16]* %c, i32 0, i32 0, !dbg !445
  call arm_aapcscc  void @gfClear(i16* %arraydecay96), !dbg !445
  %arraydecay97 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !446
  call arm_aapcscc  void @gfClear(i16* %arraydecay97), !dbg !446
  %arraydecay98 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !447
  call arm_aapcscc  void @gfClear(i16* %arraydecay98), !dbg !447
  store i16 0, i16* %alpha, align 2, !dbg !448
  store i16 0, i16* %j, align 2, !dbg !448
  store i16 0, i16* %x, align 2, !dbg !448
  store i32 0, i32* %retval, !dbg !449
  br label %return, !dbg !449

if.end99:                                         ; preds = %for.cond80
  %arrayidx100 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !450
  %48 = load i16* %arrayidx100, align 2, !dbg !450
  %conv101 = zext i16 %48 to i32, !dbg !450
  %arrayidx102 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !450
  %49 = load i16* %arrayidx102, align 2, !dbg !450
  %conv103 = zext i16 %49 to i32, !dbg !450
  %cmp104 = icmp slt i32 %conv101, %conv103, !dbg !450
  br i1 %cmp104, label %if.then106, label %if.end107, !dbg !450

if.then106:                                       ; preds = %if.end99
  br label %SWAP_GF, !dbg !451

if.end107:                                        ; preds = %if.end99
  br label %SWAP_FG, !dbg !453

SWAP_FG:                                          ; preds = %if.end107, %if.then47
  %arrayidx108 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !454
  %50 = load i16* %arrayidx108, align 2, !dbg !454
  %conv109 = zext i16 %50 to i32, !dbg !454
  %arrayidx110 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !454
  %51 = load i16* %arrayidx110, align 2, !dbg !454
  %conv111 = zext i16 %51 to i32, !dbg !454
  %sub112 = sub nsw i32 %conv109, %conv111, !dbg !454
  %conv113 = trunc i32 %sub112 to i16, !dbg !454
  store i16 %conv113, i16* %j, align 2, !dbg !454
  %arrayidx114 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !455
  %52 = load i16* %arrayidx114, align 2, !dbg !455
  %idxprom115 = zext i16 %52 to i32, !dbg !455
  %arrayidx116 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 %idxprom115, !dbg !455
  %53 = load i16* %arrayidx116, align 2, !dbg !455
  %idxprom117 = zext i16 %53 to i32, !dbg !455
  %54 = load i16** @logt, align 4, !dbg !455
  %arrayidx118 = getelementptr inbounds i16* %54, i32 %idxprom117, !dbg !455
  %55 = load i16* %arrayidx118, !dbg !455
  %conv119 = zext i16 %55 to i32, !dbg !455
  %arrayidx120 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !455
  %56 = load i16* %arrayidx120, align 2, !dbg !455
  %idxprom121 = zext i16 %56 to i32, !dbg !455
  %arrayidx122 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 %idxprom121, !dbg !455
  %57 = load i16* %arrayidx122, align 2, !dbg !455
  %idxprom123 = zext i16 %57 to i32, !dbg !455
  %58 = load i16** @logt, align 4, !dbg !455
  %arrayidx124 = getelementptr inbounds i16* %58, i32 %idxprom123, !dbg !455
  %59 = load i16* %arrayidx124, !dbg !455
  %conv125 = zext i16 %59 to i32, !dbg !455
  %sub126 = sub nsw i32 %conv119, %conv125, !dbg !455
  %add127 = add i32 %sub126, 32767, !dbg !455
  %conv128 = trunc i32 %add127 to i16, !dbg !455
  store i16 %conv128, i16* %x, align 2, !dbg !455
  %60 = load i16* %x, align 2, !dbg !456
  %conv129 = zext i16 %60 to i32, !dbg !456
  %cmp130 = icmp uge i32 %conv129, 32767, !dbg !456
  br i1 %cmp130, label %cond.true132, label %cond.false135, !dbg !456

cond.true132:                                     ; preds = %SWAP_FG
  %61 = load i16* %x, align 2, !dbg !456
  %conv133 = zext i16 %61 to i32, !dbg !456
  %sub134 = sub i32 %conv133, 32767, !dbg !456
  br label %cond.end137, !dbg !456

cond.false135:                                    ; preds = %SWAP_FG
  %62 = load i16* %x, align 2, !dbg !456
  %conv136 = zext i16 %62 to i32, !dbg !456
  br label %cond.end137, !dbg !456

cond.end137:                                      ; preds = %cond.false135, %cond.true132
  %cond138 = phi i32 [ %sub134, %cond.true132 ], [ %conv136, %cond.false135 ], !dbg !456
  %63 = load i16** @expt, align 4, !dbg !456
  %arrayidx139 = getelementptr inbounds i16* %63, i32 %cond138, !dbg !456
  %64 = load i16* %arrayidx139, !dbg !456
  store i16 %64, i16* %alpha, align 2, !dbg !456
  %arraydecay140 = getelementptr inbounds [36 x i16]* %g, i32 0, i32 0, !dbg !457
  %65 = load i16* %alpha, align 2, !dbg !457
  %66 = load i16* %j, align 2, !dbg !457
  %arraydecay141 = getelementptr inbounds [36 x i16]* %f, i32 0, i32 0, !dbg !457
  call arm_aapcscc  void @gfAddMul(i16* %arraydecay140, i16 zeroext %65, i16 zeroext %66, i16* %arraydecay141), !dbg !457
  %arraydecay142 = getelementptr inbounds [36 x i16]* %c, i32 0, i32 0, !dbg !458
  %67 = load i16* %alpha, align 2, !dbg !458
  %68 = load i16* %j, align 2, !dbg !458
  %69 = load i16** %b.addr, align 4, !dbg !458
  call arm_aapcscc  void @gfAddMul(i16* %arraydecay142, i16 zeroext %67, i16 zeroext %68, i16* %69), !dbg !458
  br label %for.cond80, !dbg !459

return:                                           ; preds = %cond.end92, %cond.end35, %if.then
  %70 = load i32* %retval, !dbg !460
  ret i32 %70, !dbg !460
}

define internal arm_aapcscc void @gfAddMul(i16* %a, i16 zeroext %alpha, i16 zeroext %j, i16* %b) nounwind uwtable {
entry:
  %a.addr = alloca i16*, align 4
  %alpha.addr = alloca i16, align 2
  %j.addr = alloca i16, align 2
  %b.addr = alloca i16*, align 4
  %i = alloca i16, align 2
  %x = alloca i16, align 2
  %la = alloca i16, align 2
  %aj = alloca i16*, align 4
  store i16* %a, i16** %a.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %a.addr}, metadata !461), !dbg !462
  store i16 %alpha, i16* %alpha.addr, align 2
  call void @llvm.dbg.declare(metadata !{i16* %alpha.addr}, metadata !463), !dbg !464
  store i16 %j, i16* %j.addr, align 2
  call void @llvm.dbg.declare(metadata !{i16* %j.addr}, metadata !465), !dbg !466
  store i16* %b, i16** %b.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %b.addr}, metadata !467), !dbg !468
  call void @llvm.dbg.declare(metadata !{i16* %i}, metadata !469), !dbg !471
  call void @llvm.dbg.declare(metadata !{i16* %x}, metadata !472), !dbg !473
  call void @llvm.dbg.declare(metadata !{i16* %la}, metadata !474), !dbg !475
  %0 = load i16* %alpha.addr, align 2, !dbg !476
  %idxprom = zext i16 %0 to i32, !dbg !476
  %1 = load i16** @logt, align 4, !dbg !476
  %arrayidx = getelementptr inbounds i16* %1, i32 %idxprom, !dbg !476
  %2 = load i16* %arrayidx, !dbg !476
  store i16 %2, i16* %la, align 2, !dbg !476
  call void @llvm.dbg.declare(metadata !{i16** %aj}, metadata !477), !dbg !478
  %3 = load i16* %j.addr, align 2, !dbg !479
  %idxprom1 = zext i16 %3 to i32, !dbg !479
  %4 = load i16** %a.addr, align 4, !dbg !479
  %arrayidx2 = getelementptr inbounds i16* %4, i32 %idxprom1, !dbg !479
  store i16* %arrayidx2, i16** %aj, align 4, !dbg !479
  %5 = load i16** @logt, align 4, !dbg !480
  %cmp = icmp ne i16* %5, null, !dbg !480
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !480

land.lhs.true:                                    ; preds = %entry
  %6 = load i16** @expt, align 4, !dbg !480
  %cmp3 = icmp ne i16* %6, null, !dbg !480
  br i1 %cmp3, label %cond.true, label %cond.false, !dbg !480

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !480

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 366, i8* getelementptr inbounds ([46 x i8]* @__PRETTY_FUNCTION__.gfAddMul, i32 0, i32 0)) noreturn nounwind, !dbg !481
  unreachable, !dbg !481
                                                  ; No predecessors!
  br label %cond.end, !dbg !481

cond.end:                                         ; preds = %7, %cond.true
  br label %while.cond, !dbg !482

while.cond:                                       ; preds = %while.body, %cond.end
  %8 = load i16** %a.addr, align 4, !dbg !482
  %arrayidx4 = getelementptr inbounds i16* %8, i32 0, !dbg !482
  %9 = load i16* %arrayidx4, !dbg !482
  %conv = zext i16 %9 to i32, !dbg !482
  %10 = load i16* %j.addr, align 2, !dbg !482
  %conv5 = zext i16 %10 to i32, !dbg !482
  %11 = load i16** %b.addr, align 4, !dbg !482
  %arrayidx6 = getelementptr inbounds i16* %11, i32 0, !dbg !482
  %12 = load i16* %arrayidx6, !dbg !482
  %conv7 = zext i16 %12 to i32, !dbg !482
  %add = add nsw i32 %conv5, %conv7, !dbg !482
  %cmp8 = icmp slt i32 %conv, %add, !dbg !482
  br i1 %cmp8, label %while.body, label %while.end, !dbg !482

while.body:                                       ; preds = %while.cond
  %13 = load i16** %a.addr, align 4, !dbg !483
  %arrayidx10 = getelementptr inbounds i16* %13, i32 0, !dbg !483
  %14 = load i16* %arrayidx10, !dbg !483
  %inc = add i16 %14, 1, !dbg !483
  store i16 %inc, i16* %arrayidx10, !dbg !483
  %15 = load i16** %a.addr, align 4, !dbg !485
  %arrayidx11 = getelementptr inbounds i16* %15, i32 0, !dbg !485
  %16 = load i16* %arrayidx11, !dbg !485
  %idxprom12 = zext i16 %16 to i32, !dbg !485
  %17 = load i16** %a.addr, align 4, !dbg !485
  %arrayidx13 = getelementptr inbounds i16* %17, i32 %idxprom12, !dbg !485
  store i16 0, i16* %arrayidx13, !dbg !485
  br label %while.cond, !dbg !486

while.end:                                        ; preds = %while.cond
  %18 = load i16** %b.addr, align 4, !dbg !487
  %arrayidx14 = getelementptr inbounds i16* %18, i32 0, !dbg !487
  %19 = load i16* %arrayidx14, !dbg !487
  store i16 %19, i16* %i, align 2, !dbg !487
  br label %for.cond, !dbg !487

for.cond:                                         ; preds = %for.inc, %while.end
  %20 = load i16* %i, align 2, !dbg !487
  %tobool = icmp ne i16 %20, 0, !dbg !487
  br i1 %tobool, label %for.body, label %for.end, !dbg !487

for.body:                                         ; preds = %for.cond
  %21 = load i16* %i, align 2, !dbg !489
  %idxprom15 = zext i16 %21 to i32, !dbg !489
  %22 = load i16** %b.addr, align 4, !dbg !489
  %arrayidx16 = getelementptr inbounds i16* %22, i32 %idxprom15, !dbg !489
  %23 = load i16* %arrayidx16, !dbg !489
  %idxprom17 = zext i16 %23 to i32, !dbg !489
  %24 = load i16** @logt, align 4, !dbg !489
  %arrayidx18 = getelementptr inbounds i16* %24, i32 %idxprom17, !dbg !489
  %25 = load i16* %arrayidx18, !dbg !489
  store i16 %25, i16* %x, align 2, !dbg !489
  %conv19 = zext i16 %25 to i32, !dbg !489
  %cmp20 = icmp ne i32 %conv19, 32767, !dbg !489
  br i1 %cmp20, label %if.then, label %if.end, !dbg !489

if.then:                                          ; preds = %for.body
  %26 = load i16* %la, align 2, !dbg !491
  %conv22 = zext i16 %26 to i32, !dbg !491
  %27 = load i16* %x, align 2, !dbg !491
  %conv23 = zext i16 %27 to i32, !dbg !491
  %add24 = add nsw i32 %conv23, %conv22, !dbg !491
  %conv25 = trunc i32 %add24 to i16, !dbg !491
  store i16 %conv25, i16* %x, align 2, !dbg !491
  %conv26 = zext i16 %conv25 to i32, !dbg !491
  %cmp27 = icmp uge i32 %conv26, 32767, !dbg !491
  br i1 %cmp27, label %cond.true29, label %cond.false31, !dbg !491

cond.true29:                                      ; preds = %if.then
  %28 = load i16* %x, align 2, !dbg !491
  %conv30 = zext i16 %28 to i32, !dbg !491
  %sub = sub i32 %conv30, 32767, !dbg !491
  br label %cond.end33, !dbg !491

cond.false31:                                     ; preds = %if.then
  %29 = load i16* %x, align 2, !dbg !491
  %conv32 = zext i16 %29 to i32, !dbg !491
  br label %cond.end33, !dbg !491

cond.end33:                                       ; preds = %cond.false31, %cond.true29
  %cond = phi i32 [ %sub, %cond.true29 ], [ %conv32, %cond.false31 ], !dbg !491
  %30 = load i16** @expt, align 4, !dbg !491
  %arrayidx34 = getelementptr inbounds i16* %30, i32 %cond, !dbg !491
  %31 = load i16* %arrayidx34, !dbg !491
  %conv35 = zext i16 %31 to i32, !dbg !491
  %32 = load i16* %i, align 2, !dbg !491
  %idxprom36 = zext i16 %32 to i32, !dbg !491
  %33 = load i16** %aj, align 4, !dbg !491
  %arrayidx37 = getelementptr inbounds i16* %33, i32 %idxprom36, !dbg !491
  %34 = load i16* %arrayidx37, !dbg !491
  %conv38 = zext i16 %34 to i32, !dbg !491
  %xor = xor i32 %conv38, %conv35, !dbg !491
  %conv39 = trunc i32 %xor to i16, !dbg !491
  store i16 %conv39, i16* %arrayidx37, !dbg !491
  br label %if.end, !dbg !493

if.end:                                           ; preds = %cond.end33, %for.body
  br label %for.inc, !dbg !494

for.inc:                                          ; preds = %if.end
  %35 = load i16* %i, align 2, !dbg !495
  %dec = add i16 %35, -1, !dbg !495
  store i16 %dec, i16* %i, align 2, !dbg !495
  br label %for.cond, !dbg !495

for.end:                                          ; preds = %for.cond
  br label %while.cond40, !dbg !496

while.cond40:                                     ; preds = %while.body50, %for.end
  %36 = load i16** %a.addr, align 4, !dbg !496
  %arrayidx41 = getelementptr inbounds i16* %36, i32 0, !dbg !496
  %37 = load i16* %arrayidx41, !dbg !496
  %conv42 = zext i16 %37 to i32, !dbg !496
  %tobool43 = icmp ne i32 %conv42, 0, !dbg !496
  br i1 %tobool43, label %land.rhs, label %land.end, !dbg !496

land.rhs:                                         ; preds = %while.cond40
  %38 = load i16** %a.addr, align 4, !dbg !496
  %arrayidx44 = getelementptr inbounds i16* %38, i32 0, !dbg !496
  %39 = load i16* %arrayidx44, !dbg !496
  %idxprom45 = zext i16 %39 to i32, !dbg !496
  %40 = load i16** %a.addr, align 4, !dbg !496
  %arrayidx46 = getelementptr inbounds i16* %40, i32 %idxprom45, !dbg !496
  %41 = load i16* %arrayidx46, !dbg !496
  %conv47 = zext i16 %41 to i32, !dbg !496
  %cmp48 = icmp eq i32 %conv47, 0, !dbg !496
  br label %land.end

land.end:                                         ; preds = %land.rhs, %while.cond40
  %42 = phi i1 [ false, %while.cond40 ], [ %cmp48, %land.rhs ]
  br i1 %42, label %while.body50, label %while.end53

while.body50:                                     ; preds = %land.end
  %43 = load i16** %a.addr, align 4, !dbg !497
  %arrayidx51 = getelementptr inbounds i16* %43, i32 0, !dbg !497
  %44 = load i16* %arrayidx51, !dbg !497
  %dec52 = add i16 %44, -1, !dbg !497
  store i16 %dec52, i16* %arrayidx51, !dbg !497
  br label %while.cond40, !dbg !499

while.end53:                                      ; preds = %land.end
  ret void, !dbg !500
}

define arm_aapcscc void @gfSquareRoot(i16* %p, i16 zeroext %b) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %b.addr = alloca i16, align 2
  %i = alloca i32, align 4
  %q = alloca [36 x i16], align 2
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !501), !dbg !502
  store i16 %b, i16* %b.addr, align 2
  call void @llvm.dbg.declare(metadata !{i16* %b.addr}, metadata !503), !dbg !504
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !505), !dbg !507
  call void @llvm.dbg.declare(metadata !{[36 x i16]* %q}, metadata !508), !dbg !509
  %0 = load i16** @logt, align 4, !dbg !510
  %cmp = icmp ne i16* %0, null, !dbg !510
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !510

land.lhs.true:                                    ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !510
  %cmp1 = icmp ne i16* %1, null, !dbg !510
  br i1 %cmp1, label %cond.true, label %cond.false, !dbg !510

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !510

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 452, i8* getelementptr inbounds ([34 x i8]* @__PRETTY_FUNCTION__.gfSquareRoot, i32 0, i32 0)) noreturn nounwind, !dbg !511
  unreachable, !dbg !511
                                                  ; No predecessors!
  br label %cond.end, !dbg !511

cond.end:                                         ; preds = %2, %cond.true
  %3 = load i16** %p.addr, align 4, !dbg !512
  %cmp2 = icmp ne i16* %3, null, !dbg !512
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !512

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !512

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 453, i8* getelementptr inbounds ([34 x i8]* @__PRETTY_FUNCTION__.gfSquareRoot, i32 0, i32 0)) noreturn nounwind, !dbg !513
  unreachable, !dbg !513
                                                  ; No predecessors!
  br label %cond.end5, !dbg !513

cond.end5:                                        ; preds = %4, %cond.true3
  %arrayidx = getelementptr inbounds [36 x i16]* %q, i32 0, i32 0, !dbg !514
  store i16 1, i16* %arrayidx, align 2, !dbg !514
  %5 = load i16* %b.addr, align 2, !dbg !515
  %arrayidx6 = getelementptr inbounds [36 x i16]* %q, i32 0, i32 1, !dbg !515
  store i16 %5, i16* %arrayidx6, align 2, !dbg !515
  %6 = load i16** %p.addr, align 4, !dbg !516
  %arraydecay = getelementptr inbounds [36 x i16]* %q, i32 0, i32 0, !dbg !516
  call arm_aapcscc  void @gfCopy(i16* %6, i16* %arraydecay), !dbg !516
  store i32 254, i32* %i, align 4, !dbg !518
  br label %while.cond, !dbg !519

while.cond:                                       ; preds = %while.body, %cond.end5
  %7 = load i32* %i, align 4, !dbg !519
  %tobool = icmp ne i32 %7, 0, !dbg !519
  br i1 %tobool, label %while.body, label %while.end, !dbg !519

while.body:                                       ; preds = %while.cond
  %8 = load i16** %p.addr, align 4, !dbg !520
  %9 = load i16** %p.addr, align 4, !dbg !520
  call arm_aapcscc  void @gfSquare(i16* %8, i16* %9), !dbg !520
  %10 = load i16** %p.addr, align 4, !dbg !522
  %11 = load i16** %p.addr, align 4, !dbg !522
  call arm_aapcscc  void @gfSquare(i16* %10, i16* %11), !dbg !522
  %12 = load i32* %i, align 4, !dbg !523
  %sub = sub nsw i32 %12, 2, !dbg !523
  store i32 %sub, i32* %i, align 4, !dbg !523
  br label %while.cond, !dbg !524

while.end:                                        ; preds = %while.cond
  ret void, !dbg !525
}

define arm_aapcscc i32 @gfTrace(i16* %p) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !526), !dbg !527
  %0 = load i16** @logt, align 4, !dbg !528
  %cmp = icmp ne i16* %0, null, !dbg !528
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !528

land.lhs.true:                                    ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !528
  %cmp1 = icmp ne i16* %1, null, !dbg !528
  br i1 %cmp1, label %cond.true, label %cond.false, !dbg !528

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !528

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 492, i8* getelementptr inbounds ([27 x i8]* @__PRETTY_FUNCTION__.gfTrace, i32 0, i32 0)) noreturn nounwind, !dbg !530
  unreachable, !dbg !530
                                                  ; No predecessors!
  br label %cond.end, !dbg !530

cond.end:                                         ; preds = %2, %cond.true
  %3 = load i16** %p.addr, align 4, !dbg !531
  %cmp2 = icmp ne i16* %3, null, !dbg !531
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !531

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !531

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 493, i8* getelementptr inbounds ([27 x i8]* @__PRETTY_FUNCTION__.gfTrace, i32 0, i32 0)) noreturn nounwind, !dbg !532
  unreachable, !dbg !532
                                                  ; No predecessors!
  br label %cond.end5, !dbg !532

cond.end5:                                        ; preds = %4, %cond.true3
  %5 = load i16** %p.addr, align 4, !dbg !533
  %arrayidx = getelementptr inbounds i16* %5, i32 0, !dbg !533
  %6 = load i16* %arrayidx, !dbg !533
  %conv = zext i16 %6 to i32, !dbg !533
  %tobool = icmp ne i32 %conv, 0, !dbg !533
  br i1 %tobool, label %cond.true6, label %cond.false9, !dbg !533

cond.true6:                                       ; preds = %cond.end5
  %7 = load i16** %p.addr, align 4, !dbg !533
  %arrayidx7 = getelementptr inbounds i16* %7, i32 1, !dbg !533
  %8 = load i16* %arrayidx7, !dbg !533
  %conv8 = zext i16 %8 to i32, !dbg !533
  %and = and i32 %conv8, 1, !dbg !533
  br label %cond.end10, !dbg !533

cond.false9:                                      ; preds = %cond.end5
  br label %cond.end10, !dbg !533

cond.end10:                                       ; preds = %cond.false9, %cond.true6
  %cond = phi i32 [ %and, %cond.true6 ], [ 0, %cond.false9 ], !dbg !533
  ret i32 %cond, !dbg !533
}

define arm_aapcscc i32 @gfQuadSolve(i16* %p, i16* %beta) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %p.addr = alloca i16*, align 4
  %beta.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !534), !dbg !535
  store i16* %beta, i16** %beta.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %beta.addr}, metadata !536), !dbg !537
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !538), !dbg !540
  %0 = load i16** @logt, align 4, !dbg !541
  %cmp = icmp ne i16* %0, null, !dbg !541
  br i1 %cmp, label %land.lhs.true, label %cond.false, !dbg !541

land.lhs.true:                                    ; preds = %entry
  %1 = load i16** @expt, align 4, !dbg !541
  %cmp1 = icmp ne i16* %1, null, !dbg !541
  br i1 %cmp1, label %cond.true, label %cond.false, !dbg !541

cond.true:                                        ; preds = %land.lhs.true
  br label %cond.end, !dbg !541

cond.false:                                       ; preds = %land.lhs.true, %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([43 x i8]* @.str3, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 528, i8* getelementptr inbounds ([40 x i8]* @__PRETTY_FUNCTION__.gfQuadSolve, i32 0, i32 0)) noreturn nounwind, !dbg !542
  unreachable, !dbg !542
                                                  ; No predecessors!
  br label %cond.end, !dbg !542

cond.end:                                         ; preds = %2, %cond.true
  %3 = load i16** %p.addr, align 4, !dbg !543
  %cmp2 = icmp ne i16* %3, null, !dbg !543
  br i1 %cmp2, label %cond.true3, label %cond.false4, !dbg !543

cond.true3:                                       ; preds = %cond.end
  br label %cond.end5, !dbg !543

cond.false4:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 529, i8* getelementptr inbounds ([40 x i8]* @__PRETTY_FUNCTION__.gfQuadSolve, i32 0, i32 0)) noreturn nounwind, !dbg !544
  unreachable, !dbg !544
                                                  ; No predecessors!
  br label %cond.end5, !dbg !544

cond.end5:                                        ; preds = %4, %cond.true3
  %5 = load i16** %beta.addr, align 4, !dbg !545
  %cmp6 = icmp ne i16* %5, null, !dbg !545
  br i1 %cmp6, label %cond.true7, label %cond.false8, !dbg !545

cond.true7:                                       ; preds = %cond.end5
  br label %cond.end9, !dbg !545

cond.false8:                                      ; preds = %cond.end5
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([20 x i8]* @.str13, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 530, i8* getelementptr inbounds ([40 x i8]* @__PRETTY_FUNCTION__.gfQuadSolve, i32 0, i32 0)) noreturn nounwind, !dbg !546
  unreachable, !dbg !546
                                                  ; No predecessors!
  br label %cond.end9, !dbg !546

cond.end9:                                        ; preds = %6, %cond.true7
  %7 = load i16** %p.addr, align 4, !dbg !547
  %8 = load i16** %beta.addr, align 4, !dbg !547
  %cmp10 = icmp ne i16* %7, %8, !dbg !547
  br i1 %cmp10, label %cond.true11, label %cond.false12, !dbg !547

cond.true11:                                      ; preds = %cond.end9
  br label %cond.end13, !dbg !547

cond.false12:                                     ; preds = %cond.end9
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([10 x i8]* @.str14, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 531, i8* getelementptr inbounds ([40 x i8]* @__PRETTY_FUNCTION__.gfQuadSolve, i32 0, i32 0)) noreturn nounwind, !dbg !548
  unreachable, !dbg !548
                                                  ; No predecessors!
  br label %cond.end13, !dbg !548

cond.end13:                                       ; preds = %9, %cond.true11
  %10 = load i16** %beta.addr, align 4, !dbg !549
  %call = call arm_aapcscc  i32 @gfTrace(i16* %10), !dbg !549
  %cmp14 = icmp ne i32 %call, 0, !dbg !549
  br i1 %cmp14, label %if.then, label %if.end, !dbg !549

if.then:                                          ; preds = %cond.end13
  store i32 1, i32* %retval, !dbg !550
  br label %return, !dbg !550

if.end:                                           ; preds = %cond.end13
  %11 = load i16** %p.addr, align 4, !dbg !552
  %12 = load i16** %beta.addr, align 4, !dbg !552
  call arm_aapcscc  void @gfCopy(i16* %11, i16* %12), !dbg !552
  store i32 0, i32* %i, align 4, !dbg !553
  br label %for.cond, !dbg !553

for.cond:                                         ; preds = %for.inc, %if.end
  %13 = load i32* %i, align 4, !dbg !553
  %cmp15 = icmp slt i32 %13, 127, !dbg !553
  br i1 %cmp15, label %for.body, label %for.end, !dbg !553

for.body:                                         ; preds = %for.cond
  %14 = load i16** %p.addr, align 4, !dbg !555
  %15 = load i16** %p.addr, align 4, !dbg !555
  call arm_aapcscc  void @gfSquare(i16* %14, i16* %15), !dbg !555
  %16 = load i16** %p.addr, align 4, !dbg !557
  %17 = load i16** %p.addr, align 4, !dbg !557
  call arm_aapcscc  void @gfSquare(i16* %16, i16* %17), !dbg !557
  %18 = load i16** %p.addr, align 4, !dbg !558
  %19 = load i16** %p.addr, align 4, !dbg !558
  %20 = load i16** %beta.addr, align 4, !dbg !558
  call arm_aapcscc  void @gfAdd(i16* %18, i16* %19, i16* %20), !dbg !558
  br label %for.inc, !dbg !559

for.inc:                                          ; preds = %for.body
  %21 = load i32* %i, align 4, !dbg !560
  %inc = add nsw i32 %21, 1, !dbg !560
  store i32 %inc, i32* %i, align 4, !dbg !560
  br label %for.cond, !dbg !560

for.end:                                          ; preds = %for.cond
  store i32 0, i32* %retval, !dbg !561
  br label %return, !dbg !561

return:                                           ; preds = %for.end, %if.then
  %22 = load i32* %retval, !dbg !562
  ret i32 %22, !dbg !562
}

define arm_aapcscc i32 @gfYbit(i16* %p) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !563), !dbg !564
  %0 = load i16** %p.addr, align 4, !dbg !565
  %cmp = icmp ne i16* %0, null, !dbg !565
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !565

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !565

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 566, i8* getelementptr inbounds ([26 x i8]* @__PRETTY_FUNCTION__.gfYbit, i32 0, i32 0)) noreturn nounwind, !dbg !567
  unreachable, !dbg !567
                                                  ; No predecessors!
  br label %cond.end, !dbg !567

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %p.addr, align 4, !dbg !568
  %arrayidx = getelementptr inbounds i16* %2, i32 0, !dbg !568
  %3 = load i16* %arrayidx, !dbg !568
  %conv = zext i16 %3 to i32, !dbg !568
  %tobool = icmp ne i32 %conv, 0, !dbg !568
  br i1 %tobool, label %cond.true1, label %cond.false4, !dbg !568

cond.true1:                                       ; preds = %cond.end
  %4 = load i16** %p.addr, align 4, !dbg !568
  %arrayidx2 = getelementptr inbounds i16* %4, i32 1, !dbg !568
  %5 = load i16* %arrayidx2, !dbg !568
  %conv3 = zext i16 %5 to i32, !dbg !568
  %and = and i32 %conv3, 1, !dbg !568
  br label %cond.end5, !dbg !568

cond.false4:                                      ; preds = %cond.end
  br label %cond.end5, !dbg !568

cond.end5:                                        ; preds = %cond.false4, %cond.true1
  %cond = phi i32 [ %and, %cond.true1 ], [ 0, %cond.false4 ], !dbg !568
  ret i32 %cond, !dbg !568
}

define arm_aapcscc void @gfPack(i16* %p, i16* %k) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %k.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  %a = alloca [19 x i16], align 2
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !569), !dbg !570
  store i16* %k, i16** %k.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %k.addr}, metadata !571), !dbg !573
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !574), !dbg !576
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %a}, metadata !577), !dbg !580
  %0 = load i16** %p.addr, align 4, !dbg !581
  %cmp = icmp ne i16* %0, null, !dbg !581
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !581

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !581

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 577, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.gfPack, i32 0, i32 0)) noreturn nounwind, !dbg !582
  unreachable, !dbg !582
                                                  ; No predecessors!
  br label %cond.end, !dbg !582

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %k.addr, align 4, !dbg !583
  %cmp1 = icmp ne i16* %2, null, !dbg !583
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !583

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !583

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str15, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 578, i8* getelementptr inbounds ([37 x i8]* @__PRETTY_FUNCTION__.gfPack, i32 0, i32 0)) noreturn nounwind, !dbg !584
  unreachable, !dbg !584
                                                  ; No predecessors!
  br label %cond.end4, !dbg !584

cond.end4:                                        ; preds = %3, %cond.true2
  %4 = load i16** %k.addr, align 4, !dbg !585
  call arm_aapcscc  void @vlClear(i16* %4), !dbg !585
  %arrayidx = getelementptr inbounds [19 x i16]* %a, i32 0, i32 0, !dbg !586
  store i16 1, i16* %arrayidx, align 2, !dbg !586
  %5 = load i16** %p.addr, align 4, !dbg !587
  %arrayidx5 = getelementptr inbounds i16* %5, i32 0, !dbg !587
  %6 = load i16* %arrayidx5, !dbg !587
  %conv = zext i16 %6 to i32, !dbg !587
  store i32 %conv, i32* %i, align 4, !dbg !587
  br label %for.cond, !dbg !587

for.cond:                                         ; preds = %for.inc, %cond.end4
  %7 = load i32* %i, align 4, !dbg !587
  %cmp6 = icmp sgt i32 %7, 0, !dbg !587
  br i1 %cmp6, label %for.body, label %for.end, !dbg !587

for.body:                                         ; preds = %for.cond
  %8 = load i16** %k.addr, align 4, !dbg !589
  call arm_aapcscc  void @vlShortLshift(i16* %8, i32 15), !dbg !589
  %9 = load i32* %i, align 4, !dbg !591
  %10 = load i16** %p.addr, align 4, !dbg !591
  %arrayidx8 = getelementptr inbounds i16* %10, i32 %9, !dbg !591
  %11 = load i16* %arrayidx8, !dbg !591
  %arrayidx9 = getelementptr inbounds [19 x i16]* %a, i32 0, i32 1, !dbg !591
  store i16 %11, i16* %arrayidx9, align 2, !dbg !591
  %12 = load i16** %k.addr, align 4, !dbg !592
  %arraydecay = getelementptr inbounds [19 x i16]* %a, i32 0, i32 0, !dbg !592
  call arm_aapcscc  void @vlAdd(i16* %12, i16* %arraydecay), !dbg !592
  br label %for.inc, !dbg !593

for.inc:                                          ; preds = %for.body
  %13 = load i32* %i, align 4, !dbg !594
  %dec = add nsw i32 %13, -1, !dbg !594
  store i32 %dec, i32* %i, align 4, !dbg !594
  br label %for.cond, !dbg !594

for.end:                                          ; preds = %for.cond
  ret void, !dbg !595
}

declare arm_aapcscc void @vlClear(i16*)

declare arm_aapcscc void @vlShortLshift(i16*, i32)

declare arm_aapcscc void @vlAdd(i16*, i16*)

define arm_aapcscc void @gfUnpack(i16* %p, i16* %k) nounwind uwtable {
entry:
  %p.addr = alloca i16*, align 4
  %k.addr = alloca i16*, align 4
  %x = alloca [19 x i16], align 2
  %n = alloca i16, align 2
  store i16* %p, i16** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %p.addr}, metadata !596), !dbg !597
  store i16* %k, i16** %k.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %k.addr}, metadata !598), !dbg !601
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %x}, metadata !602), !dbg !604
  call void @llvm.dbg.declare(metadata !{i16* %n}, metadata !605), !dbg !606
  %0 = load i16** %p.addr, align 4, !dbg !607
  %cmp = icmp ne i16* %0, null, !dbg !607
  br i1 %cmp, label %cond.true, label %cond.false, !dbg !607

cond.true:                                        ; preds = %entry
  br label %cond.end, !dbg !607

cond.false:                                       ; preds = %entry
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 594, i8* getelementptr inbounds ([39 x i8]* @__PRETTY_FUNCTION__.gfUnpack, i32 0, i32 0)) noreturn nounwind, !dbg !608
  unreachable, !dbg !608
                                                  ; No predecessors!
  br label %cond.end, !dbg !608

cond.end:                                         ; preds = %1, %cond.true
  %2 = load i16** %k.addr, align 4, !dbg !609
  %cmp1 = icmp ne i16* %2, null, !dbg !609
  br i1 %cmp1, label %cond.true2, label %cond.false3, !dbg !609

cond.true2:                                       ; preds = %cond.end
  br label %cond.end4, !dbg !609

cond.false3:                                      ; preds = %cond.end
  call arm_aapcscc  void @__assert_fail(i8* getelementptr inbounds ([17 x i8]* @.str15, i32 0, i32 0), i8* getelementptr inbounds ([11 x i8]* @.str1, i32 0, i32 0), i32 595, i8* getelementptr inbounds ([39 x i8]* @__PRETTY_FUNCTION__.gfUnpack, i32 0, i32 0)) noreturn nounwind, !dbg !610
  unreachable, !dbg !610
                                                  ; No predecessors!
  br label %cond.end4, !dbg !610

cond.end4:                                        ; preds = %3, %cond.true2
  %arraydecay = getelementptr inbounds [19 x i16]* %x, i32 0, i32 0, !dbg !611
  %4 = load i16** %k.addr, align 4, !dbg !611
  call arm_aapcscc  void @vlCopy(i16* %arraydecay, i16* %4), !dbg !611
  store i16 0, i16* %n, align 2, !dbg !612
  br label %for.cond, !dbg !612

for.cond:                                         ; preds = %for.inc, %cond.end4
  %arrayidx = getelementptr inbounds [19 x i16]* %x, i32 0, i32 0, !dbg !612
  %5 = load i16* %arrayidx, align 2, !dbg !612
  %tobool = icmp ne i16 %5, 0, !dbg !612
  br i1 %tobool, label %for.body, label %for.end, !dbg !612

for.body:                                         ; preds = %for.cond
  %arrayidx5 = getelementptr inbounds [19 x i16]* %x, i32 0, i32 1, !dbg !614
  %6 = load i16* %arrayidx5, align 2, !dbg !614
  %conv = zext i16 %6 to i32, !dbg !614
  %and = and i32 %conv, 32767, !dbg !614
  %conv6 = trunc i32 %and to i16, !dbg !614
  %7 = load i16* %n, align 2, !dbg !614
  %conv7 = zext i16 %7 to i32, !dbg !614
  %add = add nsw i32 %conv7, 1, !dbg !614
  %8 = load i16** %p.addr, align 4, !dbg !614
  %arrayidx8 = getelementptr inbounds i16* %8, i32 %add, !dbg !614
  store i16 %conv6, i16* %arrayidx8, !dbg !614
  %arraydecay9 = getelementptr inbounds [19 x i16]* %x, i32 0, i32 0, !dbg !616
  call arm_aapcscc  void @vlShortRshift(i16* %arraydecay9, i32 15), !dbg !616
  br label %for.inc, !dbg !617

for.inc:                                          ; preds = %for.body
  %9 = load i16* %n, align 2, !dbg !618
  %inc = add i16 %9, 1, !dbg !618
  store i16 %inc, i16* %n, align 2, !dbg !618
  br label %for.cond, !dbg !618

for.end:                                          ; preds = %for.cond
  %10 = load i16* %n, align 2, !dbg !619
  %11 = load i16** %p.addr, align 4, !dbg !619
  %arrayidx10 = getelementptr inbounds i16* %11, i32 0, !dbg !619
  store i16 %10, i16* %arrayidx10, !dbg !619
  ret void, !dbg !620
}

declare arm_aapcscc void @vlCopy(i16*, i16*)

declare arm_aapcscc void @vlShortRshift(i16*, i32)

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !65} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !12, metadata !17, metadata !20, metadata !23, metadata !26, metadata !29, metadata !32, metadata !35, metadata !38, metadata !41, metadata !44, metadata !47, metadata !50, metadata !53, metadata !56, metadata !59, metadata !62}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfInit", metadata !"gfInit", metadata !"", metadata !6, i32 47, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 ()* @gfInit, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"ec_field.c", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfQuit", metadata !"gfQuit", metadata !"", metadata !6, i32 105, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void ()* @gfQuit, null, null, metadata !15} ; [ DW_TAG_subprogram ]
!13 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !14, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!14 = metadata !{null}
!15 = metadata !{metadata !16}
!16 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!17 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfEqual", metadata !"gfEqual", metadata !"", metadata !6, i32 182, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, i16*)* @gfEqual, null, null, metadata !18} ; [ DW_TAG_subprogram ]
!18 = metadata !{metadata !19}
!19 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!20 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfClear", metadata !"gfClear", metadata !"", metadata !6, i32 191, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*)* @gfClear, null, null, metadata !21} ; [ DW_TAG_subprogram ]
!21 = metadata !{metadata !22}
!22 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!23 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfCopy", metadata !"gfCopy", metadata !"", metadata !6, i32 199, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @gfCopy, null, null, metadata !24} ; [ DW_TAG_subprogram ]
!24 = metadata !{metadata !25}
!25 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!26 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfAdd", metadata !"gfAdd", metadata !"", metadata !6, i32 208, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*, i16*)* @gfAdd, null, null, metadata !27} ; [ DW_TAG_subprogram ]
!27 = metadata !{metadata !28}
!28 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!29 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfMultiply", metadata !"gfMultiply", metadata !"", metadata !6, i32 270, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*, i16*)* @gfMultiply, null, null, metadata !30} ; [ DW_TAG_subprogram ]
!30 = metadata !{metadata !31}
!31 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!32 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfSquare", metadata !"gfSquare", metadata !"", metadata !6, i32 312, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @gfSquare, null, null, metadata !33} ; [ DW_TAG_subprogram ]
!33 = metadata !{metadata !34}
!34 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!35 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfSmallDiv", metadata !"gfSmallDiv", metadata !"", metadata !6, i32 346, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16)* @gfSmallDiv, null, null, metadata !36} ; [ DW_TAG_subprogram ]
!36 = metadata !{metadata !37}
!37 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!38 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfInvert", metadata !"gfInvert", metadata !"", metadata !6, i32 384, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, i16*)* @gfInvert, null, null, metadata !39} ; [ DW_TAG_subprogram ]
!39 = metadata !{metadata !40}
!40 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!41 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfSquareRoot", metadata !"gfSquareRoot", metadata !"", metadata !6, i32 448, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16)* @gfSquareRoot, null, null, metadata !42} ; [ DW_TAG_subprogram ]
!42 = metadata !{metadata !43}
!43 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!44 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfTrace", metadata !"gfTrace", metadata !"", metadata !6, i32 474, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*)* @gfTrace, null, null, metadata !45} ; [ DW_TAG_subprogram ]
!45 = metadata !{metadata !46}
!46 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!47 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfQuadSolve", metadata !"gfQuadSolve", metadata !"", metadata !6, i32 522, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, i16*)* @gfQuadSolve, null, null, metadata !48} ; [ DW_TAG_subprogram ]
!48 = metadata !{metadata !49}
!49 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!50 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfYbit", metadata !"gfYbit", metadata !"", metadata !6, i32 565, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*)* @gfYbit, null, null, metadata !51} ; [ DW_TAG_subprogram ]
!51 = metadata !{metadata !52}
!52 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!53 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfPack", metadata !"gfPack", metadata !"", metadata !6, i32 573, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @gfPack, null, null, metadata !54} ; [ DW_TAG_subprogram ]
!54 = metadata !{metadata !55}
!55 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!56 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfUnpack", metadata !"gfUnpack", metadata !"", metadata !6, i32 590, metadata !13, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16*)* @gfUnpack, null, null, metadata !57} ; [ DW_TAG_subprogram ]
!57 = metadata !{metadata !58}
!58 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!59 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfAddMul", metadata !"gfAddMul", metadata !"", metadata !6, i32 362, metadata !13, i1 true, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i16, i16, i16*)* @gfAddMul, null, null, metadata !60} ; [ DW_TAG_subprogram ]
!60 = metadata !{metadata !61}
!61 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!62 = metadata !{i32 720942, i32 0, metadata !6, metadata !"gfReduce", metadata !"gfReduce", metadata !"", metadata !6, i32 250, metadata !13, i1 true, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*)* @gfReduce, null, null, metadata !63} ; [ DW_TAG_subprogram ]
!63 = metadata !{metadata !64}
!64 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!65 = metadata !{metadata !66}
!66 = metadata !{metadata !67, metadata !72}
!67 = metadata !{i32 720948, i32 0, null, metadata !"expt", metadata !"expt", metadata !"", metadata !6, i32 41, metadata !68, i32 1, i32 1, i16** @expt} ; [ DW_TAG_variable ]
!68 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !69} ; [ DW_TAG_pointer_type ]
!69 = metadata !{i32 720918, null, metadata !"lunit", metadata !6, i32 27, i64 0, i64 0, i64 0, i32 0, metadata !70} ; [ DW_TAG_typedef ]
!70 = metadata !{i32 720918, null, metadata !"word16", metadata !6, i32 11, i64 0, i64 0, i64 0, i32 0, metadata !71} ; [ DW_TAG_typedef ]
!71 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!72 = metadata !{i32 720948, i32 0, null, metadata !"logt", metadata !"logt", metadata !"", metadata !6, i32 42, metadata !68, i32 1, i32 1, i16** @logt} ; [ DW_TAG_variable ]
!73 = metadata !{i32 721152, metadata !74, metadata !"root", metadata !6, i32 48, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!74 = metadata !{i32 720907, metadata !5, i32 47, i32 1, metadata !6, i32 153} ; [ DW_TAG_lexical_block ]
!75 = metadata !{i32 720918, null, metadata !"ltemp", metadata !6, i32 33, i64 0, i64 0, i64 0, i32 0, metadata !70} ; [ DW_TAG_typedef ]
!76 = metadata !{i32 48, i32 8, metadata !74, null}
!77 = metadata !{i32 721152, metadata !74, metadata !"i", metadata !6, i32 48, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!78 = metadata !{i32 48, i32 14, metadata !74, null}
!79 = metadata !{i32 721152, metadata !74, metadata !"j", metadata !6, i32 48, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!80 = metadata !{i32 48, i32 17, metadata !74, null}
!81 = metadata !{i32 50, i32 2, metadata !74, null}
!82 = metadata !{i32 52, i32 3, metadata !83, null}
!83 = metadata !{i32 720907, metadata !74, i32 50, i32 50, metadata !6, i32 154} ; [ DW_TAG_lexical_block ]
!84 = metadata !{i32 54, i32 2, metadata !74, null}
!85 = metadata !{i32 56, i32 3, metadata !86, null}
!86 = metadata !{i32 720907, metadata !74, i32 55, i32 47, metadata !6, i32 155} ; [ DW_TAG_lexical_block ]
!87 = metadata !{i32 58, i32 24, metadata !74, null}
!88 = metadata !{i32 59, i32 3, metadata !89, null}
!89 = metadata !{i32 720907, metadata !74, i32 58, i32 78, metadata !6, i32 156} ; [ DW_TAG_lexical_block ]
!90 = metadata !{i32 61, i32 24, metadata !74, null}
!91 = metadata !{i32 62, i32 3, metadata !92, null}
!92 = metadata !{i32 720907, metadata !74, i32 61, i32 78, metadata !6, i32 157} ; [ DW_TAG_lexical_block ]
!93 = metadata !{i32 62, i32 16, metadata !92, null}
!94 = metadata !{i32 63, i32 3, metadata !92, null}
!95 = metadata !{i32 65, i32 2, metadata !74, null}
!96 = metadata !{i32 66, i32 2, metadata !74, null}
!97 = metadata !{i32 67, i32 7, metadata !98, null}
!98 = metadata !{i32 720907, metadata !74, i32 67, i32 2, metadata !6, i32 158} ; [ DW_TAG_lexical_block ]
!99 = metadata !{i32 68, i32 3, metadata !100, null}
!100 = metadata !{i32 720907, metadata !98, i32 67, i32 35, metadata !6, i32 159} ; [ DW_TAG_lexical_block ]
!101 = metadata !{i32 69, i32 3, metadata !100, null}
!102 = metadata !{i32 70, i32 4, metadata !103, null}
!103 = metadata !{i32 720907, metadata !100, i32 69, i32 23, metadata !6, i32 160} ; [ DW_TAG_lexical_block ]
!104 = metadata !{i32 71, i32 3, metadata !103, null}
!105 = metadata !{i32 72, i32 3, metadata !100, null}
!106 = metadata !{i32 73, i32 2, metadata !100, null}
!107 = metadata !{i32 67, i32 30, metadata !98, null}
!108 = metadata !{i32 74, i32 7, metadata !109, null}
!109 = metadata !{i32 720907, metadata !74, i32 74, i32 2, metadata !6, i32 161} ; [ DW_TAG_lexical_block ]
!110 = metadata !{i32 75, i32 3, metadata !111, null}
!111 = metadata !{i32 720907, metadata !109, i32 74, i32 39, metadata !6, i32 162} ; [ DW_TAG_lexical_block ]
!112 = metadata !{i32 76, i32 2, metadata !111, null}
!113 = metadata !{i32 74, i32 34, metadata !109, null}
!114 = metadata !{i32 77, i32 2, metadata !74, null}
!115 = metadata !{i32 99, i32 2, metadata !74, null}
!116 = metadata !{i32 100, i32 1, metadata !74, null}
!117 = metadata !{i32 106, i32 2, metadata !118, null}
!118 = metadata !{i32 720907, metadata !12, i32 105, i32 1, metadata !6, i32 163} ; [ DW_TAG_lexical_block ]
!119 = metadata !{i32 107, i32 3, metadata !120, null}
!120 = metadata !{i32 720907, metadata !118, i32 106, i32 12, metadata !6, i32 164} ; [ DW_TAG_lexical_block ]
!121 = metadata !{i32 107, i32 16, metadata !120, null}
!122 = metadata !{i32 108, i32 2, metadata !120, null}
!123 = metadata !{i32 109, i32 2, metadata !118, null}
!124 = metadata !{i32 110, i32 3, metadata !125, null}
!125 = metadata !{i32 720907, metadata !118, i32 109, i32 12, metadata !6, i32 165} ; [ DW_TAG_lexical_block ]
!126 = metadata !{i32 110, i32 16, metadata !125, null}
!127 = metadata !{i32 111, i32 2, metadata !125, null}
!128 = metadata !{i32 112, i32 1, metadata !118, null}
!129 = metadata !{i32 721153, metadata !17, metadata !"p", metadata !6, i32 16777396, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!130 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !131} ; [ DW_TAG_pointer_type ]
!131 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !69} ; [ DW_TAG_const_type ]
!132 = metadata !{i32 180, i32 28, metadata !17, null}
!133 = metadata !{i32 721153, metadata !17, metadata !"q", metadata !6, i32 33554612, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!134 = metadata !{i32 180, i32 45, metadata !17, null}
!135 = metadata !{i32 183, i32 2, metadata !136, null}
!136 = metadata !{i32 720907, metadata !17, i32 182, i32 1, metadata !6, i32 166} ; [ DW_TAG_lexical_block ]
!137 = metadata !{i32 183, i32 37, metadata !136, null}
!138 = metadata !{i32 184, i32 2, metadata !136, null}
!139 = metadata !{i32 184, i32 37, metadata !136, null}
!140 = metadata !{i32 185, i32 9, metadata !136, null}
!141 = metadata !{i32 721153, metadata !20, metadata !"p", metadata !6, i32 16777405, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!142 = metadata !{i32 189, i32 23, metadata !20, null}
!143 = metadata !{i32 192, i32 2, metadata !144, null}
!144 = metadata !{i32 720907, metadata !20, i32 191, i32 1, metadata !6, i32 167} ; [ DW_TAG_lexical_block ]
!145 = metadata !{i32 192, i32 37, metadata !144, null}
!146 = metadata !{i32 193, i32 2, metadata !144, null}
!147 = metadata !{i32 194, i32 1, metadata !144, null}
!148 = metadata !{i32 721153, metadata !23, metadata !"p", metadata !6, i32 16777413, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!149 = metadata !{i32 197, i32 22, metadata !23, null}
!150 = metadata !{i32 721153, metadata !23, metadata !"q", metadata !6, i32 33554629, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!151 = metadata !{i32 197, i32 39, metadata !23, null}
!152 = metadata !{i32 200, i32 2, metadata !153, null}
!153 = metadata !{i32 720907, metadata !23, i32 199, i32 1, metadata !6, i32 168} ; [ DW_TAG_lexical_block ]
!154 = metadata !{i32 200, i32 37, metadata !153, null}
!155 = metadata !{i32 201, i32 2, metadata !153, null}
!156 = metadata !{i32 201, i32 37, metadata !153, null}
!157 = metadata !{i32 202, i32 2, metadata !153, null}
!158 = metadata !{i32 203, i32 1, metadata !153, null}
!159 = metadata !{i32 721153, metadata !26, metadata !"p", metadata !6, i32 16777422, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!160 = metadata !{i32 206, i32 21, metadata !26, null}
!161 = metadata !{i32 721153, metadata !26, metadata !"q", metadata !6, i32 33554638, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!162 = metadata !{i32 206, i32 38, metadata !26, null}
!163 = metadata !{i32 721153, metadata !26, metadata !"r", metadata !6, i32 50331854, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!164 = metadata !{i32 206, i32 55, metadata !26, null}
!165 = metadata !{i32 721152, metadata !166, metadata !"i", metadata !6, i32 209, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!166 = metadata !{i32 720907, metadata !26, i32 208, i32 1, metadata !6, i32 169} ; [ DW_TAG_lexical_block ]
!167 = metadata !{i32 209, i32 8, metadata !166, null}
!168 = metadata !{i32 211, i32 2, metadata !166, null}
!169 = metadata !{i32 211, i32 63, metadata !166, null}
!170 = metadata !{i32 212, i32 2, metadata !166, null}
!171 = metadata !{i32 212, i32 37, metadata !166, null}
!172 = metadata !{i32 213, i32 2, metadata !166, null}
!173 = metadata !{i32 213, i32 37, metadata !166, null}
!174 = metadata !{i32 214, i32 2, metadata !166, null}
!175 = metadata !{i32 214, i32 37, metadata !166, null}
!176 = metadata !{i32 215, i32 2, metadata !166, null}
!177 = metadata !{i32 217, i32 8, metadata !178, null}
!178 = metadata !{i32 720907, metadata !179, i32 217, i32 3, metadata !6, i32 171} ; [ DW_TAG_lexical_block ]
!179 = metadata !{i32 720907, metadata !166, i32 215, i32 19, metadata !6, i32 170} ; [ DW_TAG_lexical_block ]
!180 = metadata !{i32 218, i32 4, metadata !181, null}
!181 = metadata !{i32 720907, metadata !178, i32 217, i32 31, metadata !6, i32 172} ; [ DW_TAG_lexical_block ]
!182 = metadata !{i32 219, i32 3, metadata !181, null}
!183 = metadata !{i32 217, i32 26, metadata !178, null}
!184 = metadata !{i32 221, i32 3, metadata !179, null}
!185 = metadata !{i32 223, i32 3, metadata !179, null}
!186 = metadata !{i32 224, i32 2, metadata !179, null}
!187 = metadata !{i32 224, i32 9, metadata !166, null}
!188 = metadata !{i32 226, i32 8, metadata !189, null}
!189 = metadata !{i32 720907, metadata !190, i32 226, i32 3, metadata !6, i32 174} ; [ DW_TAG_lexical_block ]
!190 = metadata !{i32 720907, metadata !166, i32 224, i32 26, metadata !6, i32 173} ; [ DW_TAG_lexical_block ]
!191 = metadata !{i32 227, i32 4, metadata !192, null}
!192 = metadata !{i32 720907, metadata !189, i32 226, i32 31, metadata !6, i32 175} ; [ DW_TAG_lexical_block ]
!193 = metadata !{i32 228, i32 3, metadata !192, null}
!194 = metadata !{i32 226, i32 26, metadata !189, null}
!195 = metadata !{i32 230, i32 3, metadata !190, null}
!196 = metadata !{i32 232, i32 3, metadata !190, null}
!197 = metadata !{i32 233, i32 2, metadata !190, null}
!198 = metadata !{i32 235, i32 8, metadata !199, null}
!199 = metadata !{i32 720907, metadata !200, i32 235, i32 3, metadata !6, i32 177} ; [ DW_TAG_lexical_block ]
!200 = metadata !{i32 720907, metadata !166, i32 233, i32 9, metadata !6, i32 176} ; [ DW_TAG_lexical_block ]
!201 = metadata !{i32 236, i32 4, metadata !202, null}
!202 = metadata !{i32 720907, metadata !199, i32 235, i32 30, metadata !6, i32 178} ; [ DW_TAG_lexical_block ]
!203 = metadata !{i32 237, i32 5, metadata !204, null}
!204 = metadata !{i32 720907, metadata !202, i32 236, i32 21, metadata !6, i32 179} ; [ DW_TAG_lexical_block ]
!205 = metadata !{i32 239, i32 3, metadata !202, null}
!206 = metadata !{i32 235, i32 25, metadata !199, null}
!207 = metadata !{i32 241, i32 8, metadata !208, null}
!208 = metadata !{i32 720907, metadata !200, i32 241, i32 3, metadata !6, i32 180} ; [ DW_TAG_lexical_block ]
!209 = metadata !{i32 242, i32 4, metadata !210, null}
!210 = metadata !{i32 720907, metadata !208, i32 241, i32 37, metadata !6, i32 181} ; [ DW_TAG_lexical_block ]
!211 = metadata !{i32 243, i32 3, metadata !210, null}
!212 = metadata !{i32 241, i32 32, metadata !208, null}
!213 = metadata !{i32 245, i32 1, metadata !166, null}
!214 = metadata !{i32 721153, metadata !29, metadata !"r", metadata !6, i32 16777484, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!215 = metadata !{i32 268, i32 26, metadata !29, null}
!216 = metadata !{i32 721153, metadata !29, metadata !"p", metadata !6, i32 33554700, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!217 = metadata !{i32 268, i32 43, metadata !29, null}
!218 = metadata !{i32 721153, metadata !29, metadata !"q", metadata !6, i32 50331916, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!219 = metadata !{i32 268, i32 60, metadata !29, null}
!220 = metadata !{i32 721152, metadata !221, metadata !"i", metadata !6, i32 271, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!221 = metadata !{i32 720907, metadata !29, i32 270, i32 1, metadata !6, i32 182} ; [ DW_TAG_lexical_block ]
!222 = metadata !{i32 271, i32 6, metadata !221, null}
!223 = metadata !{i32 721152, metadata !221, metadata !"j", metadata !6, i32 271, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!224 = metadata !{i32 271, i32 9, metadata !221, null}
!225 = metadata !{i32 721152, metadata !221, metadata !"x", metadata !6, i32 272, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!226 = metadata !{i32 272, i32 8, metadata !221, null}
!227 = metadata !{i32 721152, metadata !221, metadata !"log_pi", metadata !6, i32 272, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!228 = metadata !{i32 272, i32 11, metadata !221, null}
!229 = metadata !{i32 721152, metadata !221, metadata !"log_qj", metadata !6, i32 272, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!230 = metadata !{i32 272, i32 19, metadata !221, null}
!231 = metadata !{i32 721152, metadata !221, metadata !"lg", metadata !6, i32 273, metadata !232, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!232 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 304, i64 16, i32 0, i32 0, metadata !69, metadata !233, i32 0, i32 0} ; [ DW_TAG_array_type ]
!233 = metadata !{metadata !234}
!234 = metadata !{i32 720929, i64 0, i64 18}      ; [ DW_TAG_subrange_type ]
!235 = metadata !{i32 273, i32 8, metadata !221, null}
!236 = metadata !{i32 275, i32 2, metadata !221, null}
!237 = metadata !{i32 275, i32 63, metadata !221, null}
!238 = metadata !{i32 276, i32 2, metadata !221, null}
!239 = metadata !{i32 276, i32 37, metadata !221, null}
!240 = metadata !{i32 277, i32 2, metadata !221, null}
!241 = metadata !{i32 277, i32 37, metadata !221, null}
!242 = metadata !{i32 278, i32 2, metadata !221, null}
!243 = metadata !{i32 278, i32 27, metadata !221, null}
!244 = metadata !{i32 279, i32 2, metadata !221, null}
!245 = metadata !{i32 279, i32 27, metadata !221, null}
!246 = metadata !{i32 280, i32 2, metadata !221, null}
!247 = metadata !{i32 282, i32 8, metadata !248, null}
!248 = metadata !{i32 720907, metadata !249, i32 282, i32 3, metadata !6, i32 184} ; [ DW_TAG_lexical_block ]
!249 = metadata !{i32 720907, metadata !221, i32 280, i32 20, metadata !6, i32 183} ; [ DW_TAG_lexical_block ]
!250 = metadata !{i32 283, i32 4, metadata !251, null}
!251 = metadata !{i32 720907, metadata !248, i32 282, i32 26, metadata !6, i32 185} ; [ DW_TAG_lexical_block ]
!252 = metadata !{i32 284, i32 3, metadata !251, null}
!253 = metadata !{i32 282, i32 21, metadata !248, null}
!254 = metadata !{i32 286, i32 3, metadata !249, null}
!255 = metadata !{i32 287, i32 8, metadata !256, null}
!256 = metadata !{i32 720907, metadata !249, i32 287, i32 3, metadata !6, i32 186} ; [ DW_TAG_lexical_block ]
!257 = metadata !{i32 288, i32 4, metadata !258, null}
!258 = metadata !{i32 720907, metadata !256, i32 287, i32 26, metadata !6, i32 187} ; [ DW_TAG_lexical_block ]
!259 = metadata !{i32 289, i32 10, metadata !260, null}
!260 = metadata !{i32 720907, metadata !261, i32 289, i32 5, metadata !6, i32 189} ; [ DW_TAG_lexical_block ]
!261 = metadata !{i32 720907, metadata !258, i32 288, i32 49, metadata !6, i32 188} ; [ DW_TAG_lexical_block ]
!262 = metadata !{i32 290, i32 6, metadata !263, null}
!263 = metadata !{i32 720907, metadata !260, i32 289, i32 28, metadata !6, i32 190} ; [ DW_TAG_lexical_block ]
!264 = metadata !{i32 292, i32 7, metadata !265, null}
!265 = metadata !{i32 720907, metadata !263, i32 290, i32 46, metadata !6, i32 191} ; [ DW_TAG_lexical_block ]
!266 = metadata !{i32 293, i32 6, metadata !265, null}
!267 = metadata !{i32 294, i32 5, metadata !263, null}
!268 = metadata !{i32 289, i32 23, metadata !260, null}
!269 = metadata !{i32 295, i32 4, metadata !261, null}
!270 = metadata !{i32 296, i32 3, metadata !258, null}
!271 = metadata !{i32 287, i32 21, metadata !256, null}
!272 = metadata !{i32 297, i32 3, metadata !249, null}
!273 = metadata !{i32 299, i32 3, metadata !249, null}
!274 = metadata !{i32 300, i32 2, metadata !249, null}
!275 = metadata !{i32 302, i32 3, metadata !276, null}
!276 = metadata !{i32 720907, metadata !221, i32 300, i32 9, metadata !6, i32 192} ; [ DW_TAG_lexical_block ]
!277 = metadata !{i32 305, i32 2, metadata !221, null}
!278 = metadata !{i32 306, i32 2, metadata !221, null}
!279 = metadata !{i32 307, i32 1, metadata !221, null}
!280 = metadata !{i32 721153, metadata !62, metadata !"p", metadata !6, i32 16777464, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!281 = metadata !{i32 248, i32 31, metadata !62, null}
!282 = metadata !{i32 721152, metadata !283, metadata !"i", metadata !6, i32 251, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!283 = metadata !{i32 720907, metadata !62, i32 250, i32 1, metadata !6, i32 237} ; [ DW_TAG_lexical_block ]
!284 = metadata !{i32 251, i32 6, metadata !283, null}
!285 = metadata !{i32 253, i32 7, metadata !286, null}
!286 = metadata !{i32 720907, metadata !283, i32 253, i32 2, metadata !6, i32 238} ; [ DW_TAG_lexical_block ]
!287 = metadata !{i32 254, i32 3, metadata !288, null}
!288 = metadata !{i32 720907, metadata !286, i32 253, i32 30, metadata !6, i32 239} ; [ DW_TAG_lexical_block ]
!289 = metadata !{i32 255, i32 3, metadata !288, null}
!290 = metadata !{i32 256, i32 3, metadata !288, null}
!291 = metadata !{i32 257, i32 2, metadata !288, null}
!292 = metadata !{i32 253, i32 25, metadata !286, null}
!293 = metadata !{i32 258, i32 2, metadata !283, null}
!294 = metadata !{i32 260, i32 3, metadata !295, null}
!295 = metadata !{i32 720907, metadata !283, i32 258, i32 17, metadata !6, i32 240} ; [ DW_TAG_lexical_block ]
!296 = metadata !{i32 261, i32 3, metadata !295, null}
!297 = metadata !{i32 262, i32 4, metadata !298, null}
!298 = metadata !{i32 720907, metadata !295, i32 261, i32 30, metadata !6, i32 241} ; [ DW_TAG_lexical_block ]
!299 = metadata !{i32 263, i32 3, metadata !298, null}
!300 = metadata !{i32 264, i32 2, metadata !295, null}
!301 = metadata !{i32 265, i32 1, metadata !283, null}
!302 = metadata !{i32 721153, metadata !32, metadata !"r", metadata !6, i32 16777526, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!303 = metadata !{i32 310, i32 24, metadata !32, null}
!304 = metadata !{i32 721153, metadata !32, metadata !"p", metadata !6, i32 33554742, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!305 = metadata !{i32 310, i32 41, metadata !32, null}
!306 = metadata !{i32 721152, metadata !307, metadata !"i", metadata !6, i32 313, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!307 = metadata !{i32 720907, metadata !32, i32 312, i32 1, metadata !6, i32 193} ; [ DW_TAG_lexical_block ]
!308 = metadata !{i32 313, i32 6, metadata !307, null}
!309 = metadata !{i32 721152, metadata !307, metadata !"x", metadata !6, i32 314, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!310 = metadata !{i32 314, i32 8, metadata !307, null}
!311 = metadata !{i32 316, i32 2, metadata !307, null}
!312 = metadata !{i32 316, i32 63, metadata !307, null}
!313 = metadata !{i32 317, i32 2, metadata !307, null}
!314 = metadata !{i32 317, i32 37, metadata !307, null}
!315 = metadata !{i32 318, i32 2, metadata !307, null}
!316 = metadata !{i32 318, i32 37, metadata !307, null}
!317 = metadata !{i32 319, i32 2, metadata !307, null}
!318 = metadata !{i32 321, i32 3, metadata !319, null}
!319 = metadata !{i32 720907, metadata !307, i32 319, i32 12, metadata !6, i32 194} ; [ DW_TAG_lexical_block ]
!320 = metadata !{i32 322, i32 3, metadata !319, null}
!321 = metadata !{i32 323, i32 4, metadata !322, null}
!322 = metadata !{i32 720907, metadata !319, i32 322, i32 43, metadata !6, i32 195} ; [ DW_TAG_lexical_block ]
!323 = metadata !{i32 324, i32 3, metadata !322, null}
!324 = metadata !{i32 325, i32 4, metadata !325, null}
!325 = metadata !{i32 720907, metadata !319, i32 324, i32 10, metadata !6, i32 196} ; [ DW_TAG_lexical_block ]
!326 = metadata !{i32 327, i32 8, metadata !327, null}
!327 = metadata !{i32 720907, metadata !319, i32 327, i32 3, metadata !6, i32 197} ; [ DW_TAG_lexical_block ]
!328 = metadata !{i32 328, i32 4, metadata !329, null}
!329 = metadata !{i32 720907, metadata !327, i32 327, i32 30, metadata !6, i32 198} ; [ DW_TAG_lexical_block ]
!330 = metadata !{i32 329, i32 4, metadata !329, null}
!331 = metadata !{i32 330, i32 5, metadata !332, null}
!332 = metadata !{i32 720907, metadata !329, i32 329, i32 44, metadata !6, i32 199} ; [ DW_TAG_lexical_block ]
!333 = metadata !{i32 331, i32 4, metadata !332, null}
!334 = metadata !{i32 332, i32 5, metadata !335, null}
!335 = metadata !{i32 720907, metadata !329, i32 331, i32 11, metadata !6, i32 200} ; [ DW_TAG_lexical_block ]
!336 = metadata !{i32 334, i32 3, metadata !329, null}
!337 = metadata !{i32 327, i32 25, metadata !327, null}
!338 = metadata !{i32 335, i32 3, metadata !319, null}
!339 = metadata !{i32 337, i32 3, metadata !319, null}
!340 = metadata !{i32 338, i32 2, metadata !319, null}
!341 = metadata !{i32 339, i32 3, metadata !342, null}
!342 = metadata !{i32 720907, metadata !307, i32 338, i32 9, metadata !6, i32 201} ; [ DW_TAG_lexical_block ]
!343 = metadata !{i32 341, i32 1, metadata !307, null}
!344 = metadata !{i32 721153, metadata !35, metadata !"p", metadata !6, i32 16777560, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!345 = metadata !{i32 344, i32 26, metadata !35, null}
!346 = metadata !{i32 721153, metadata !35, metadata !"b", metadata !6, i32 33554776, metadata !69, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!347 = metadata !{i32 344, i32 35, metadata !35, null}
!348 = metadata !{i32 721152, metadata !349, metadata !"i", metadata !6, i32 347, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!349 = metadata !{i32 720907, metadata !35, i32 346, i32 1, metadata !6, i32 202} ; [ DW_TAG_lexical_block ]
!350 = metadata !{i32 347, i32 6, metadata !349, null}
!351 = metadata !{i32 721152, metadata !349, metadata !"x", metadata !6, i32 348, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!352 = metadata !{i32 348, i32 8, metadata !349, null}
!353 = metadata !{i32 721152, metadata !349, metadata !"lb", metadata !6, i32 348, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!354 = metadata !{i32 348, i32 11, metadata !349, null}
!355 = metadata !{i32 348, i32 23, metadata !349, null}
!356 = metadata !{i32 350, i32 2, metadata !349, null}
!357 = metadata !{i32 350, i32 63, metadata !349, null}
!358 = metadata !{i32 351, i32 2, metadata !349, null}
!359 = metadata !{i32 351, i32 37, metadata !349, null}
!360 = metadata !{i32 352, i32 2, metadata !349, null}
!361 = metadata !{i32 352, i32 27, metadata !349, null}
!362 = metadata !{i32 353, i32 7, metadata !363, null}
!363 = metadata !{i32 720907, metadata !349, i32 353, i32 2, metadata !6, i32 203} ; [ DW_TAG_lexical_block ]
!364 = metadata !{i32 354, i32 3, metadata !365, null}
!365 = metadata !{i32 720907, metadata !363, i32 353, i32 25, metadata !6, i32 204} ; [ DW_TAG_lexical_block ]
!366 = metadata !{i32 355, i32 4, metadata !367, null}
!367 = metadata !{i32 720907, metadata !365, i32 354, i32 43, metadata !6, i32 205} ; [ DW_TAG_lexical_block ]
!368 = metadata !{i32 356, i32 3, metadata !367, null}
!369 = metadata !{i32 357, i32 2, metadata !365, null}
!370 = metadata !{i32 353, i32 20, metadata !363, null}
!371 = metadata !{i32 358, i32 1, metadata !349, null}
!372 = metadata !{i32 721153, metadata !38, metadata !"b", metadata !6, i32 16777597, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!373 = metadata !{i32 381, i32 23, metadata !38, null}
!374 = metadata !{i32 721153, metadata !38, metadata !"a", metadata !6, i32 33554813, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!375 = metadata !{i32 381, i32 40, metadata !38, null}
!376 = metadata !{i32 721152, metadata !377, metadata !"c", metadata !6, i32 385, metadata !378, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!377 = metadata !{i32 720907, metadata !38, i32 384, i32 1, metadata !6, i32 206} ; [ DW_TAG_lexical_block ]
!378 = metadata !{i32 720918, null, metadata !"gfPoint", metadata !6, i32 36, i64 0, i64 0, i64 0, i32 0, metadata !379} ; [ DW_TAG_typedef ]
!379 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 576, i64 16, i32 0, i32 0, metadata !69, metadata !380, i32 0, i32 0} ; [ DW_TAG_array_type ]
!380 = metadata !{metadata !381}
!381 = metadata !{i32 720929, i64 0, i64 35}      ; [ DW_TAG_subrange_type ]
!382 = metadata !{i32 385, i32 10, metadata !377, null}
!383 = metadata !{i32 721152, metadata !377, metadata !"f", metadata !6, i32 385, metadata !378, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!384 = metadata !{i32 385, i32 13, metadata !377, null}
!385 = metadata !{i32 721152, metadata !377, metadata !"g", metadata !6, i32 385, metadata !378, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!386 = metadata !{i32 385, i32 16, metadata !377, null}
!387 = metadata !{i32 721152, metadata !377, metadata !"x", metadata !6, i32 386, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!388 = metadata !{i32 386, i32 8, metadata !377, null}
!389 = metadata !{i32 721152, metadata !377, metadata !"j", metadata !6, i32 386, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!390 = metadata !{i32 386, i32 11, metadata !377, null}
!391 = metadata !{i32 721152, metadata !377, metadata !"alpha", metadata !6, i32 386, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!392 = metadata !{i32 386, i32 14, metadata !377, null}
!393 = metadata !{i32 388, i32 2, metadata !377, null}
!394 = metadata !{i32 388, i32 63, metadata !377, null}
!395 = metadata !{i32 389, i32 2, metadata !377, null}
!396 = metadata !{i32 389, i32 37, metadata !377, null}
!397 = metadata !{i32 390, i32 2, metadata !377, null}
!398 = metadata !{i32 390, i32 37, metadata !377, null}
!399 = metadata !{i32 391, i32 2, metadata !377, null}
!400 = metadata !{i32 391, i32 27, metadata !377, null}
!401 = metadata !{i32 392, i32 2, metadata !377, null}
!402 = metadata !{i32 394, i32 3, metadata !403, null}
!403 = metadata !{i32 720907, metadata !377, i32 392, i32 17, metadata !6, i32 207} ; [ DW_TAG_lexical_block ]
!404 = metadata !{i32 398, i32 2, metadata !377, null}
!405 = metadata !{i32 398, i32 12, metadata !377, null}
!406 = metadata !{i32 399, i32 2, metadata !377, null}
!407 = metadata !{i32 400, i32 2, metadata !377, null}
!408 = metadata !{i32 401, i32 2, metadata !377, null}
!409 = metadata !{i32 402, i32 2, metadata !377, null}
!410 = metadata !{i32 402, i32 17, metadata !377, null}
!411 = metadata !{i32 402, i32 27, metadata !377, null}
!412 = metadata !{i32 402, i32 41, metadata !377, null}
!413 = metadata !{i32 404, i32 2, metadata !414, null}
!414 = metadata !{i32 720907, metadata !377, i32 404, i32 2, metadata !6, i32 208} ; [ DW_TAG_lexical_block ]
!415 = metadata !{i32 405, i32 3, metadata !416, null}
!416 = metadata !{i32 720907, metadata !414, i32 404, i32 11, metadata !6, i32 209} ; [ DW_TAG_lexical_block ]
!417 = metadata !{i32 406, i32 4, metadata !418, null}
!418 = metadata !{i32 720907, metadata !416, i32 405, i32 18, metadata !6, i32 210} ; [ DW_TAG_lexical_block ]
!419 = metadata !{i32 406, i32 32, metadata !418, null}
!420 = metadata !{i32 407, i32 4, metadata !418, null}
!421 = metadata !{i32 409, i32 4, metadata !418, null}
!422 = metadata !{i32 409, i32 17, metadata !418, null}
!423 = metadata !{i32 409, i32 30, metadata !418, null}
!424 = metadata !{i32 409, i32 43, metadata !418, null}
!425 = metadata !{i32 410, i32 4, metadata !418, null}
!426 = metadata !{i32 412, i32 3, metadata !416, null}
!427 = metadata !{i32 413, i32 4, metadata !428, null}
!428 = metadata !{i32 720907, metadata !416, i32 412, i32 20, metadata !6, i32 211} ; [ DW_TAG_lexical_block ]
!429 = metadata !{i32 414, i32 3, metadata !428, null}
!430 = metadata !{i32 416, i32 3, metadata !416, null}
!431 = metadata !{i32 417, i32 3, metadata !416, null}
!432 = metadata !{i32 418, i32 3, metadata !416, null}
!433 = metadata !{i32 419, i32 3, metadata !416, null}
!434 = metadata !{i32 420, i32 3, metadata !416, null}
!435 = metadata !{i32 421, i32 2, metadata !416, null}
!436 = metadata !{i32 424, i32 2, metadata !437, null}
!437 = metadata !{i32 720907, metadata !377, i32 424, i32 2, metadata !6, i32 212} ; [ DW_TAG_lexical_block ]
!438 = metadata !{i32 425, i32 3, metadata !439, null}
!439 = metadata !{i32 720907, metadata !437, i32 424, i32 11, metadata !6, i32 213} ; [ DW_TAG_lexical_block ]
!440 = metadata !{i32 426, i32 4, metadata !441, null}
!441 = metadata !{i32 720907, metadata !439, i32 425, i32 18, metadata !6, i32 214} ; [ DW_TAG_lexical_block ]
!442 = metadata !{i32 426, i32 32, metadata !441, null}
!443 = metadata !{i32 427, i32 4, metadata !441, null}
!444 = metadata !{i32 428, i32 4, metadata !441, null}
!445 = metadata !{i32 430, i32 4, metadata !441, null}
!446 = metadata !{i32 430, i32 17, metadata !441, null}
!447 = metadata !{i32 430, i32 30, metadata !441, null}
!448 = metadata !{i32 430, i32 43, metadata !441, null}
!449 = metadata !{i32 431, i32 4, metadata !441, null}
!450 = metadata !{i32 433, i32 3, metadata !439, null}
!451 = metadata !{i32 434, i32 4, metadata !452, null}
!452 = metadata !{i32 720907, metadata !439, i32 433, i32 20, metadata !6, i32 215} ; [ DW_TAG_lexical_block ]
!453 = metadata !{i32 435, i32 3, metadata !452, null}
!454 = metadata !{i32 437, i32 3, metadata !439, null}
!455 = metadata !{i32 438, i32 3, metadata !439, null}
!456 = metadata !{i32 439, i32 3, metadata !439, null}
!457 = metadata !{i32 440, i32 3, metadata !439, null}
!458 = metadata !{i32 441, i32 3, metadata !439, null}
!459 = metadata !{i32 442, i32 2, metadata !439, null}
!460 = metadata !{i32 443, i32 1, metadata !377, null}
!461 = metadata !{i32 721153, metadata !59, metadata !"a", metadata !6, i32 16777577, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!462 = metadata !{i32 361, i32 31, metadata !59, null}
!463 = metadata !{i32 721153, metadata !59, metadata !"alpha", metadata !6, i32 33554793, metadata !75, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!464 = metadata !{i32 361, i32 40, metadata !59, null}
!465 = metadata !{i32 721153, metadata !59, metadata !"j", metadata !6, i32 50332009, metadata !75, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!466 = metadata !{i32 361, i32 53, metadata !59, null}
!467 = metadata !{i32 721153, metadata !59, metadata !"b", metadata !6, i32 67109225, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!468 = metadata !{i32 361, i32 64, metadata !59, null}
!469 = metadata !{i32 721152, metadata !470, metadata !"i", metadata !6, i32 363, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!470 = metadata !{i32 720907, metadata !59, i32 362, i32 1, metadata !6, i32 231} ; [ DW_TAG_lexical_block ]
!471 = metadata !{i32 363, i32 8, metadata !470, null}
!472 = metadata !{i32 721152, metadata !470, metadata !"x", metadata !6, i32 363, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!473 = metadata !{i32 363, i32 11, metadata !470, null}
!474 = metadata !{i32 721152, metadata !470, metadata !"la", metadata !6, i32 363, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!475 = metadata !{i32 363, i32 14, metadata !470, null}
!476 = metadata !{i32 363, i32 30, metadata !470, null}
!477 = metadata !{i32 721152, metadata !470, metadata !"aj", metadata !6, i32 364, metadata !68, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!478 = metadata !{i32 364, i32 9, metadata !470, null}
!479 = metadata !{i32 364, i32 19, metadata !470, null}
!480 = metadata !{i32 366, i32 2, metadata !470, null}
!481 = metadata !{i32 366, i32 63, metadata !470, null}
!482 = metadata !{i32 367, i32 2, metadata !470, null}
!483 = metadata !{i32 368, i32 3, metadata !484, null}
!484 = metadata !{i32 720907, metadata !470, i32 367, i32 26, metadata !6, i32 232} ; [ DW_TAG_lexical_block ]
!485 = metadata !{i32 368, i32 11, metadata !484, null}
!486 = metadata !{i32 369, i32 2, metadata !484, null}
!487 = metadata !{i32 370, i32 7, metadata !488, null}
!488 = metadata !{i32 720907, metadata !470, i32 370, i32 2, metadata !6, i32 233} ; [ DW_TAG_lexical_block ]
!489 = metadata !{i32 371, i32 3, metadata !490, null}
!490 = metadata !{i32 720907, metadata !488, i32 370, i32 25, metadata !6, i32 234} ; [ DW_TAG_lexical_block ]
!491 = metadata !{i32 372, i32 4, metadata !492, null}
!492 = metadata !{i32 720907, metadata !490, i32 371, i32 43, metadata !6, i32 235} ; [ DW_TAG_lexical_block ]
!493 = metadata !{i32 373, i32 3, metadata !492, null}
!494 = metadata !{i32 374, i32 2, metadata !490, null}
!495 = metadata !{i32 370, i32 20, metadata !488, null}
!496 = metadata !{i32 375, i32 2, metadata !470, null}
!497 = metadata !{i32 376, i32 3, metadata !498, null}
!498 = metadata !{i32 720907, metadata !470, i32 375, i32 29, metadata !6, i32 236} ; [ DW_TAG_lexical_block ]
!499 = metadata !{i32 377, i32 2, metadata !498, null}
!500 = metadata !{i32 378, i32 1, metadata !470, null}
!501 = metadata !{i32 721153, metadata !41, metadata !"p", metadata !6, i32 16777662, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!502 = metadata !{i32 446, i32 28, metadata !41, null}
!503 = metadata !{i32 721153, metadata !41, metadata !"b", metadata !6, i32 33554878, metadata !69, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!504 = metadata !{i32 446, i32 37, metadata !41, null}
!505 = metadata !{i32 721152, metadata !506, metadata !"i", metadata !6, i32 449, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!506 = metadata !{i32 720907, metadata !41, i32 448, i32 1, metadata !6, i32 216} ; [ DW_TAG_lexical_block ]
!507 = metadata !{i32 449, i32 6, metadata !506, null}
!508 = metadata !{i32 721152, metadata !506, metadata !"q", metadata !6, i32 450, metadata !378, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!509 = metadata !{i32 450, i32 10, metadata !506, null}
!510 = metadata !{i32 452, i32 2, metadata !506, null}
!511 = metadata !{i32 452, i32 63, metadata !506, null}
!512 = metadata !{i32 453, i32 2, metadata !506, null}
!513 = metadata !{i32 453, i32 37, metadata !506, null}
!514 = metadata !{i32 454, i32 2, metadata !506, null}
!515 = metadata !{i32 454, i32 12, metadata !506, null}
!516 = metadata !{i32 461, i32 3, metadata !517, null}
!517 = metadata !{i32 720907, metadata !506, i32 459, i32 9, metadata !6, i32 217} ; [ DW_TAG_lexical_block ]
!518 = metadata !{i32 462, i32 3, metadata !517, null}
!519 = metadata !{i32 464, i32 2, metadata !506, null}
!520 = metadata !{i32 465, i32 3, metadata !521, null}
!521 = metadata !{i32 720907, metadata !506, i32 464, i32 12, metadata !6, i32 218} ; [ DW_TAG_lexical_block ]
!522 = metadata !{i32 466, i32 3, metadata !521, null}
!523 = metadata !{i32 467, i32 3, metadata !521, null}
!524 = metadata !{i32 468, i32 2, metadata !521, null}
!525 = metadata !{i32 469, i32 1, metadata !506, null}
!526 = metadata !{i32 721153, metadata !44, metadata !"p", metadata !6, i32 16777688, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!527 = metadata !{i32 472, i32 28, metadata !44, null}
!528 = metadata !{i32 492, i32 2, metadata !529, null}
!529 = metadata !{i32 720907, metadata !44, i32 474, i32 1, metadata !6, i32 219} ; [ DW_TAG_lexical_block ]
!530 = metadata !{i32 492, i32 63, metadata !529, null}
!531 = metadata !{i32 493, i32 2, metadata !529, null}
!532 = metadata !{i32 493, i32 37, metadata !529, null}
!533 = metadata !{i32 496, i32 2, metadata !529, null}
!534 = metadata !{i32 721153, metadata !47, metadata !"p", metadata !6, i32 16777736, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!535 = metadata !{i32 520, i32 26, metadata !47, null}
!536 = metadata !{i32 721153, metadata !47, metadata !"beta", metadata !6, i32 33554952, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!537 = metadata !{i32 520, i32 43, metadata !47, null}
!538 = metadata !{i32 721152, metadata !539, metadata !"i", metadata !6, i32 523, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!539 = metadata !{i32 720907, metadata !47, i32 522, i32 1, metadata !6, i32 220} ; [ DW_TAG_lexical_block ]
!540 = metadata !{i32 523, i32 6, metadata !539, null}
!541 = metadata !{i32 528, i32 2, metadata !539, null}
!542 = metadata !{i32 528, i32 63, metadata !539, null}
!543 = metadata !{i32 529, i32 2, metadata !539, null}
!544 = metadata !{i32 529, i32 37, metadata !539, null}
!545 = metadata !{i32 530, i32 2, metadata !539, null}
!546 = metadata !{i32 530, i32 40, metadata !539, null}
!547 = metadata !{i32 531, i32 2, metadata !539, null}
!548 = metadata !{i32 531, i32 30, metadata !539, null}
!549 = metadata !{i32 533, i32 6, metadata !539, null}
!550 = metadata !{i32 534, i32 3, metadata !551, null}
!551 = metadata !{i32 720907, metadata !539, i32 533, i32 27, metadata !6, i32 221} ; [ DW_TAG_lexical_block ]
!552 = metadata !{i32 552, i32 2, metadata !539, null}
!553 = metadata !{i32 553, i32 7, metadata !554, null}
!554 = metadata !{i32 720907, metadata !539, i32 553, i32 2, metadata !6, i32 222} ; [ DW_TAG_lexical_block ]
!555 = metadata !{i32 554, i32 3, metadata !556, null}
!556 = metadata !{i32 720907, metadata !554, i32 553, i32 30, metadata !6, i32 223} ; [ DW_TAG_lexical_block ]
!557 = metadata !{i32 555, i32 3, metadata !556, null}
!558 = metadata !{i32 556, i32 3, metadata !556, null}
!559 = metadata !{i32 557, i32 2, metadata !556, null}
!560 = metadata !{i32 553, i32 25, metadata !554, null}
!561 = metadata !{i32 559, i32 2, metadata !539, null}
!562 = metadata !{i32 560, i32 1, metadata !539, null}
!563 = metadata !{i32 721153, metadata !50, metadata !"p", metadata !6, i32 16777779, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!564 = metadata !{i32 563, i32 27, metadata !50, null}
!565 = metadata !{i32 566, i32 2, metadata !566, null}
!566 = metadata !{i32 720907, metadata !50, i32 565, i32 1, metadata !6, i32 224} ; [ DW_TAG_lexical_block ]
!567 = metadata !{i32 566, i32 37, metadata !566, null}
!568 = metadata !{i32 567, i32 2, metadata !566, null}
!569 = metadata !{i32 721153, metadata !53, metadata !"p", metadata !6, i32 16777787, metadata !130, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!570 = metadata !{i32 571, i32 28, metadata !53, null}
!571 = metadata !{i32 721153, metadata !53, metadata !"k", metadata !6, i32 33555003, metadata !572, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!572 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !70} ; [ DW_TAG_pointer_type ]
!573 = metadata !{i32 571, i32 39, metadata !53, null}
!574 = metadata !{i32 721152, metadata !575, metadata !"i", metadata !6, i32 574, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!575 = metadata !{i32 720907, metadata !53, i32 573, i32 1, metadata !6, i32 225} ; [ DW_TAG_lexical_block ]
!576 = metadata !{i32 574, i32 6, metadata !575, null}
!577 = metadata !{i32 721152, metadata !575, metadata !"a", metadata !6, i32 575, metadata !578, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!578 = metadata !{i32 720918, null, metadata !"vlPoint", metadata !6, i32 17, i64 0, i64 0, i64 0, i32 0, metadata !579} ; [ DW_TAG_typedef ]
!579 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 304, i64 16, i32 0, i32 0, metadata !70, metadata !233, i32 0, i32 0} ; [ DW_TAG_array_type ]
!580 = metadata !{i32 575, i32 10, metadata !575, null}
!581 = metadata !{i32 577, i32 2, metadata !575, null}
!582 = metadata !{i32 577, i32 37, metadata !575, null}
!583 = metadata !{i32 578, i32 2, metadata !575, null}
!584 = metadata !{i32 578, i32 37, metadata !575, null}
!585 = metadata !{i32 579, i32 2, metadata !575, null}
!586 = metadata !{i32 579, i32 15, metadata !575, null}
!587 = metadata !{i32 580, i32 7, metadata !588, null}
!588 = metadata !{i32 720907, metadata !575, i32 580, i32 2, metadata !6, i32 226} ; [ DW_TAG_lexical_block ]
!589 = metadata !{i32 581, i32 3, metadata !590, null}
!590 = metadata !{i32 720907, metadata !588, i32 580, i32 29, metadata !6, i32 227} ; [ DW_TAG_lexical_block ]
!591 = metadata !{i32 582, i32 3, metadata !590, null}
!592 = metadata !{i32 583, i32 3, metadata !590, null}
!593 = metadata !{i32 584, i32 2, metadata !590, null}
!594 = metadata !{i32 580, i32 24, metadata !588, null}
!595 = metadata !{i32 585, i32 1, metadata !575, null}
!596 = metadata !{i32 721153, metadata !56, metadata !"p", metadata !6, i32 16777804, metadata !68, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!597 = metadata !{i32 588, i32 24, metadata !56, null}
!598 = metadata !{i32 721153, metadata !56, metadata !"k", metadata !6, i32 33555020, metadata !599, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!599 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !600} ; [ DW_TAG_pointer_type ]
!600 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !70} ; [ DW_TAG_const_type ]
!601 = metadata !{i32 588, i32 41, metadata !56, null}
!602 = metadata !{i32 721152, metadata !603, metadata !"x", metadata !6, i32 591, metadata !578, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!603 = metadata !{i32 720907, metadata !56, i32 590, i32 1, metadata !6, i32 228} ; [ DW_TAG_lexical_block ]
!604 = metadata !{i32 591, i32 10, metadata !603, null}
!605 = metadata !{i32 721152, metadata !603, metadata !"n", metadata !6, i32 592, metadata !69, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!606 = metadata !{i32 592, i32 8, metadata !603, null}
!607 = metadata !{i32 594, i32 2, metadata !603, null}
!608 = metadata !{i32 594, i32 37, metadata !603, null}
!609 = metadata !{i32 595, i32 2, metadata !603, null}
!610 = metadata !{i32 595, i32 37, metadata !603, null}
!611 = metadata !{i32 596, i32 2, metadata !603, null}
!612 = metadata !{i32 597, i32 7, metadata !613, null}
!613 = metadata !{i32 720907, metadata !603, i32 597, i32 2, metadata !6, i32 229} ; [ DW_TAG_lexical_block ]
!614 = metadata !{i32 598, i32 3, metadata !615, null}
!615 = metadata !{i32 720907, metadata !613, i32 597, i32 25, metadata !6, i32 230} ; [ DW_TAG_lexical_block ]
!616 = metadata !{i32 599, i32 3, metadata !615, null}
!617 = metadata !{i32 600, i32 2, metadata !615, null}
!618 = metadata !{i32 597, i32 20, metadata !613, null}
!619 = metadata !{i32 601, i32 2, metadata !603, null}
!620 = metadata !{i32 602, i32 1, metadata !603, null}
