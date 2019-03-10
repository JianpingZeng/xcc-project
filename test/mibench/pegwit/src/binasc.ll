; ModuleID = 'binasc.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct._IO_FILE.5 = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker.6*, %struct._IO_FILE.5*, i32, i32, i32, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i32, i32, [40 x i8] }
%struct._IO_marker.6 = type { %struct._IO_marker.6*, %struct._IO_FILE.5*, i32 }

@asciiBuffer = internal global [66 x i8] zeroinitializer, align 1
@bin = internal global [3 x i8] zeroinitializer, align 1
@inBin = internal global i32 0, align 4
@stdout = external global %struct._IO_FILE.5*
@stdin = external global %struct._IO_FILE.5*
@bytesLeft = internal global i32 0, align 4
@readHead = internal global i8* getelementptr inbounds ([48 x i8]* @binaryBuffer, i32 0, i32 0), align 4
@binaryBuffer = internal global [48 x i8] zeroinitializer, align 1
@more = internal global i32 1, align 4
@.str = private unnamed_addr constant [7 x i8] c"=3D=3D\00", align 1
@asctobin = internal global [128 x i8] c"\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80\80>\80\80\80?456789:;<=\80\80\80\80\80\80\80\00\01\02\03\04\05\06\07\08\09\0A\0B\0C\0D\0E\0F\10\11\12\13\14\15\16\17\18\19\80\80\80\80\80\80\1A\1B\1C\1D\1E\1F !\22#$%&'()*+,-./0123\80\80\80\80\80", align 1
@stderr = external global %struct._IO_FILE.5*
@err_decode_failed = internal global [78 x i8] c"Pegwit; Out of range characters encountered in ASCII armouring.\0ATerminating.\0A\00", align 1
@writeHead = internal global i8* getelementptr inbounds ([66 x i8]* @asciiBuffer, i32 0, i32 0), align 4
@space = internal global i32 64, align 4
@bintoasc = internal global [65 x i8] c"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\00", align 1

define arm_aapcscc void @burnBinasc() nounwind uwtable {
entry:
  call void @llvm.memset.p0i8.i32(i8* getelementptr inbounds ([66 x i8]* @asciiBuffer, i32 0, i32 0), i8 0, i32 66, i32 1, i1 false), !dbg !79
  call void @llvm.memset.p0i8.i32(i8* getelementptr inbounds ([3 x i8]* @bin, i32 0, i32 0), i8 0, i32 3, i32 1, i1 false), !dbg !81
  ret void, !dbg !82
}

declare void @llvm.memset.p0i8.i32(i8* nocapture, i8, i32, i32, i1) nounwind

define arm_aapcscc i32 @flushArmour(%struct._IO_FILE.5* %stream) nounwind uwtable {
entry:
  %stream.addr = alloca %struct._IO_FILE.5*, align 4
  %result = alloca i32, align 4
  store %struct._IO_FILE.5* %stream, %struct._IO_FILE.5** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.5** %stream.addr}, metadata !83), !dbg !138
  call void @llvm.dbg.declare(metadata !{i32* %result}, metadata !139), !dbg !141
  store i32 1, i32* %result, align 4, !dbg !142
  %0 = load i32* @inBin, align 4, !dbg !143
  %tobool = icmp ne i32 %0, 0, !dbg !143
  br i1 %tobool, label %if.then, label %if.end, !dbg !143

if.then:                                          ; preds = %entry
  %1 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !144
  %call = call arm_aapcscc  i32 @push3bytes(%struct._IO_FILE.5* %1), !dbg !144
  store i32 %call, i32* %result, align 4, !dbg !144
  br label %if.end, !dbg !144

if.end:                                           ; preds = %if.then, %entry
  %2 = load i32* %result, align 4, !dbg !145
  %tobool1 = icmp ne i32 %2, 0, !dbg !145
  br i1 %tobool1, label %if.then2, label %if.end4, !dbg !145

if.then2:                                         ; preds = %if.end
  %3 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !146
  %call3 = call arm_aapcscc  i32 @flushBuffer(%struct._IO_FILE.5* %3), !dbg !146
  store i32 %call3, i32* %result, align 4, !dbg !146
  br label %if.end4, !dbg !146

if.end4:                                          ; preds = %if.then2, %if.end
  %4 = load i32* %result, align 4, !dbg !147
  ret i32 %4, !dbg !147
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

define internal arm_aapcscc i32 @push3bytes(%struct._IO_FILE.5* %stream) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %stream.addr = alloca %struct._IO_FILE.5*, align 4
  %push = alloca i32, align 4
  store %struct._IO_FILE.5* %stream, %struct._IO_FILE.5** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.5** %stream.addr}, metadata !148), !dbg !149
  %0 = load i32* @space, align 4, !dbg !150
  %cmp = icmp slt i32 %0, 4, !dbg !150
  br i1 %cmp, label %if.then, label %if.end2, !dbg !150

if.then:                                          ; preds = %entry
  call void @llvm.dbg.declare(metadata !{i32* %push}, metadata !152), !dbg !154
  %1 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !155
  %call = call arm_aapcscc  i32 @flushBuffer(%struct._IO_FILE.5* %1), !dbg !155
  store i32 %call, i32* %push, align 4, !dbg !155
  %2 = load i32* %push, align 4, !dbg !156
  %tobool = icmp ne i32 %2, 0, !dbg !156
  br i1 %tobool, label %if.end, label %if.then1, !dbg !156

if.then1:                                         ; preds = %if.then
  store i32 0, i32* %retval, !dbg !157
  br label %return, !dbg !157

if.end:                                           ; preds = %if.then
  br label %if.end2, !dbg !158

if.end2:                                          ; preds = %if.end, %entry
  %3 = load i8** @writeHead, align 4, !dbg !159
  %4 = load i32* @inBin, align 4, !dbg !159
  call arm_aapcscc  void @encode(i8* getelementptr inbounds ([3 x i8]* @bin, i32 0, i32 0), i8* %3, i32 %4), !dbg !159
  store i32 0, i32* @inBin, align 4, !dbg !160
  %5 = load i8** @writeHead, align 4, !dbg !161
  %add.ptr = getelementptr inbounds i8* %5, i32 4, !dbg !161
  store i8* %add.ptr, i8** @writeHead, align 4, !dbg !161
  %6 = load i32* @space, align 4, !dbg !162
  %sub = sub nsw i32 %6, 4, !dbg !162
  store i32 %sub, i32* @space, align 4, !dbg !162
  store i32 1, i32* %retval, !dbg !163
  br label %return, !dbg !163

return:                                           ; preds = %if.end2, %if.then1
  %7 = load i32* %retval, !dbg !164
  ret i32 %7, !dbg !164
}

define internal arm_aapcscc i32 @flushBuffer(%struct._IO_FILE.5* %stream) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %stream.addr = alloca %struct._IO_FILE.5*, align 4
  store %struct._IO_FILE.5* %stream, %struct._IO_FILE.5** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.5** %stream.addr}, metadata !165), !dbg !166
  %0 = load i8** @writeHead, align 4, !dbg !167
  %cmp = icmp eq i8* getelementptr inbounds ([66 x i8]* @asciiBuffer, i32 0, i32 0), %0, !dbg !167
  br i1 %cmp, label %if.then, label %if.end, !dbg !167

if.then:                                          ; preds = %entry
  store i32 1, i32* %retval, !dbg !169
  br label %return, !dbg !169

if.end:                                           ; preds = %entry
  %1 = load i8** @writeHead, align 4, !dbg !170
  %arrayidx = getelementptr inbounds i8* %1, i32 0, !dbg !170
  store i8 10, i8* %arrayidx, !dbg !170
  %2 = load i8** @writeHead, align 4, !dbg !171
  %arrayidx1 = getelementptr inbounds i8* %2, i32 1, !dbg !171
  store i8 0, i8* %arrayidx1, !dbg !171
  store i8* getelementptr inbounds ([66 x i8]* @asciiBuffer, i32 0, i32 0), i8** @writeHead, align 4, !dbg !172
  store i32 64, i32* @space, align 4, !dbg !173
  %3 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !174
  %call = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([66 x i8]* @asciiBuffer, i32 0, i32 0), %struct._IO_FILE.5* %3), !dbg !174
  %cmp2 = icmp sge i32 %call, 0, !dbg !174
  %conv = zext i1 %cmp2 to i32, !dbg !174
  store i32 %conv, i32* %retval, !dbg !174
  br label %return, !dbg !174

return:                                           ; preds = %if.end, %if.then
  %4 = load i32* %retval, !dbg !175
  ret i32 %4, !dbg !175
}

define arm_aapcscc i32 @fwritePlus(i8* %ptr, i32 %size, i32 %n, %struct._IO_FILE.5* %stream) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %ptr.addr = alloca i8*, align 4
  %size.addr = alloca i32, align 4
  %n.addr = alloca i32, align 4
  %stream.addr = alloca %struct._IO_FILE.5*, align 4
  %result = alloca i32, align 4
  %bytesOver = alloca i32, align 4
  %out = alloca i8*, align 4
  store i8* %ptr, i8** %ptr.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %ptr.addr}, metadata !176), !dbg !179
  store i32 %size, i32* %size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %size.addr}, metadata !180), !dbg !181
  store i32 %n, i32* %n.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %n.addr}, metadata !182), !dbg !183
  store %struct._IO_FILE.5* %stream, %struct._IO_FILE.5** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.5** %stream.addr}, metadata !184), !dbg !185
  call void @llvm.dbg.declare(metadata !{i32* %result}, metadata !186), !dbg !188
  store i32 0, i32* %result, align 4, !dbg !189
  call void @llvm.dbg.declare(metadata !{i32* %bytesOver}, metadata !190), !dbg !191
  store i32 0, i32* %bytesOver, align 4, !dbg !192
  call void @llvm.dbg.declare(metadata !{i8** %out}, metadata !193), !dbg !194
  %0 = load i8** %ptr.addr, align 4, !dbg !195
  store i8* %0, i8** %out, align 4, !dbg !195
  %1 = load %struct._IO_FILE.5** @stdout, align 4, !dbg !196
  %2 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !196
  %cmp = icmp ne %struct._IO_FILE.5* %1, %2, !dbg !196
  br i1 %cmp, label %if.then, label %if.end, !dbg !196

if.then:                                          ; preds = %entry
  %3 = load i8** %ptr.addr, align 4, !dbg !197
  %4 = load i32* %size.addr, align 4, !dbg !197
  %5 = load i32* %n.addr, align 4, !dbg !197
  %6 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !197
  %call = call arm_aapcscc  i32 @fwrite(i8* %3, i32 %4, i32 %5, %struct._IO_FILE.5* %6), !dbg !197
  store i32 %call, i32* %retval, !dbg !197
  br label %return, !dbg !197

if.end:                                           ; preds = %entry
  br label %while.cond, !dbg !198

while.cond:                                       ; preds = %if.end12, %if.end
  %7 = load i32* %result, align 4, !dbg !198
  %8 = load i32* %n.addr, align 4, !dbg !198
  %cmp1 = icmp ult i32 %7, %8, !dbg !198
  br i1 %cmp1, label %while.body, label %while.end, !dbg !198

while.body:                                       ; preds = %while.cond
  %9 = load i8** %out, align 4, !dbg !199
  %10 = load i8* %9, !dbg !199
  %11 = load i32* @inBin, align 4, !dbg !199
  %arrayidx = getelementptr inbounds [3 x i8]* @bin, i32 0, i32 %11, !dbg !199
  store i8 %10, i8* %arrayidx, align 1, !dbg !199
  %12 = load i32* @inBin, align 4, !dbg !201
  %inc = add nsw i32 %12, 1, !dbg !201
  store i32 %inc, i32* @inBin, align 4, !dbg !201
  %13 = load i8** %out, align 4, !dbg !202
  %incdec.ptr = getelementptr inbounds i8* %13, i32 1, !dbg !202
  store i8* %incdec.ptr, i8** %out, align 4, !dbg !202
  %14 = load i32* %bytesOver, align 4, !dbg !203
  %inc2 = add nsw i32 %14, 1, !dbg !203
  store i32 %inc2, i32* %bytesOver, align 4, !dbg !203
  %15 = load i32* @inBin, align 4, !dbg !204
  %cmp3 = icmp eq i32 3, %15, !dbg !204
  br i1 %cmp3, label %if.then4, label %if.end8, !dbg !204

if.then4:                                         ; preds = %while.body
  %16 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !205
  %call5 = call arm_aapcscc  i32 @push3bytes(%struct._IO_FILE.5* %16), !dbg !205
  %tobool = icmp ne i32 %call5, 0, !dbg !205
  br i1 %tobool, label %if.end7, label %if.then6, !dbg !205

if.then6:                                         ; preds = %if.then4
  %17 = load i32* %result, align 4, !dbg !207
  store i32 %17, i32* %retval, !dbg !207
  br label %return, !dbg !207

if.end7:                                          ; preds = %if.then4
  store i32 0, i32* @inBin, align 4, !dbg !208
  br label %if.end8, !dbg !209

if.end8:                                          ; preds = %if.end7, %while.body
  %18 = load i32* %bytesOver, align 4, !dbg !210
  %19 = load i32* %size.addr, align 4, !dbg !210
  %cmp9 = icmp eq i32 %18, %19, !dbg !210
  br i1 %cmp9, label %if.then10, label %if.end12, !dbg !210

if.then10:                                        ; preds = %if.end8
  %20 = load i32* %result, align 4, !dbg !211
  %inc11 = add i32 %20, 1, !dbg !211
  store i32 %inc11, i32* %result, align 4, !dbg !211
  store i32 0, i32* %bytesOver, align 4, !dbg !213
  br label %if.end12, !dbg !214

if.end12:                                         ; preds = %if.then10, %if.end8
  br label %while.cond, !dbg !215

while.end:                                        ; preds = %while.cond
  %21 = load i32* %n.addr, align 4, !dbg !216
  store i32 %21, i32* %retval, !dbg !216
  br label %return, !dbg !216

return:                                           ; preds = %while.end, %if.then6, %if.then
  %22 = load i32* %retval, !dbg !217
  ret i32 %22, !dbg !217
}

declare arm_aapcscc i32 @fwrite(i8*, i32, i32, %struct._IO_FILE.5*)

define arm_aapcscc i32 @fputcPlus(i32 %c, %struct._IO_FILE.5* %stream) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %c.addr = alloca i32, align 4
  %stream.addr = alloca %struct._IO_FILE.5*, align 4
  store i32 %c, i32* %c.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %c.addr}, metadata !218), !dbg !219
  store %struct._IO_FILE.5* %stream, %struct._IO_FILE.5** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.5** %stream.addr}, metadata !220), !dbg !221
  %0 = load %struct._IO_FILE.5** @stdout, align 4, !dbg !222
  %1 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !222
  %cmp = icmp ne %struct._IO_FILE.5* %0, %1, !dbg !222
  br i1 %cmp, label %if.then, label %if.end, !dbg !222

if.then:                                          ; preds = %entry
  %2 = load i32* %c.addr, align 4, !dbg !224
  %3 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !224
  %call = call arm_aapcscc  i32 @fputc(i32 %2, %struct._IO_FILE.5* %3), !dbg !224
  store i32 %call, i32* %retval, !dbg !224
  br label %return, !dbg !224

if.end:                                           ; preds = %entry
  %4 = load i32* %c.addr, align 4, !dbg !225
  %and = and i32 %4, 255, !dbg !225
  %conv = trunc i32 %and to i8, !dbg !225
  %5 = load i32* @inBin, align 4, !dbg !225
  %arrayidx = getelementptr inbounds [3 x i8]* @bin, i32 0, i32 %5, !dbg !225
  store i8 %conv, i8* %arrayidx, align 1, !dbg !225
  %6 = load i32* @inBin, align 4, !dbg !226
  %inc = add nsw i32 %6, 1, !dbg !226
  store i32 %inc, i32* @inBin, align 4, !dbg !226
  %7 = load i32* @inBin, align 4, !dbg !227
  %cmp1 = icmp eq i32 3, %7, !dbg !227
  br i1 %cmp1, label %if.then3, label %if.end7, !dbg !227

if.then3:                                         ; preds = %if.end
  %8 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !228
  %call4 = call arm_aapcscc  i32 @push3bytes(%struct._IO_FILE.5* %8), !dbg !228
  %tobool = icmp ne i32 %call4, 0, !dbg !228
  br i1 %tobool, label %if.end6, label %if.then5, !dbg !228

if.then5:                                         ; preds = %if.then3
  store i32 -1, i32* %retval, !dbg !230
  br label %return, !dbg !230

if.end6:                                          ; preds = %if.then3
  store i32 0, i32* @inBin, align 4, !dbg !231
  br label %if.end7, !dbg !232

if.end7:                                          ; preds = %if.end6, %if.end
  %9 = load i32* %c.addr, align 4, !dbg !233
  store i32 %9, i32* %retval, !dbg !233
  br label %return, !dbg !233

return:                                           ; preds = %if.end7, %if.then5, %if.then
  %10 = load i32* %retval, !dbg !234
  ret i32 %10, !dbg !234
}

declare arm_aapcscc i32 @fputc(i32, %struct._IO_FILE.5*)

define arm_aapcscc i32 @freadPlus(i8* %ptr, i32 %size, i32 %n, %struct._IO_FILE.5* %stream) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %ptr.addr = alloca i8*, align 4
  %size.addr = alloca i32, align 4
  %n.addr = alloca i32, align 4
  %stream.addr = alloca %struct._IO_FILE.5*, align 4
  %result = alloca i32, align 4
  %bytesOver = alloca i32, align 4
  %out = alloca i8*, align 4
  %l = alloca i32, align 4
  %inBuf = alloca [66 x i8], align 1
  store i8* %ptr, i8** %ptr.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %ptr.addr}, metadata !235), !dbg !236
  store i32 %size, i32* %size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %size.addr}, metadata !237), !dbg !238
  store i32 %n, i32* %n.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %n.addr}, metadata !239), !dbg !240
  store %struct._IO_FILE.5* %stream, %struct._IO_FILE.5** %stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.5** %stream.addr}, metadata !241), !dbg !242
  call void @llvm.dbg.declare(metadata !{i32* %result}, metadata !243), !dbg !245
  store i32 0, i32* %result, align 4, !dbg !246
  call void @llvm.dbg.declare(metadata !{i32* %bytesOver}, metadata !247), !dbg !248
  store i32 0, i32* %bytesOver, align 4, !dbg !249
  call void @llvm.dbg.declare(metadata !{i8** %out}, metadata !250), !dbg !251
  %0 = load i8** %ptr.addr, align 4, !dbg !252
  store i8* %0, i8** %out, align 4, !dbg !252
  %1 = load %struct._IO_FILE.5** @stdin, align 4, !dbg !253
  %2 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !253
  %cmp = icmp ne %struct._IO_FILE.5* %1, %2, !dbg !253
  br i1 %cmp, label %if.then, label %if.end, !dbg !253

if.then:                                          ; preds = %entry
  %3 = load i8** %ptr.addr, align 4, !dbg !254
  %4 = load i32* %size.addr, align 4, !dbg !254
  %5 = load i32* %n.addr, align 4, !dbg !254
  %6 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !254
  %call = call arm_aapcscc  i32 @fread(i8* %3, i32 %4, i32 %5, %struct._IO_FILE.5* %6), !dbg !254
  store i32 %call, i32* %retval, !dbg !254
  br label %return, !dbg !254

if.end:                                           ; preds = %entry
  br label %while.cond, !dbg !255

while.cond:                                       ; preds = %if.end41, %if.end
  %7 = load i32* %result, align 4, !dbg !255
  %8 = load i32* %n.addr, align 4, !dbg !255
  %cmp1 = icmp ult i32 %7, %8, !dbg !255
  br i1 %cmp1, label %while.body, label %while.end42, !dbg !255

while.body:                                       ; preds = %while.cond
  %9 = load i32* @bytesLeft, align 4, !dbg !256
  %10 = load i32* %size.addr, align 4, !dbg !256
  %11 = load i32* %bytesOver, align 4, !dbg !256
  %sub = sub i32 %10, %11, !dbg !256
  %cmp2 = icmp uge i32 %9, %sub, !dbg !256
  br i1 %cmp2, label %if.then3, label %if.else, !dbg !256

if.then3:                                         ; preds = %while.body
  %12 = load i8** %out, align 4, !dbg !258
  %13 = load i8** @readHead, align 4, !dbg !258
  %14 = load i32* %size.addr, align 4, !dbg !258
  %15 = load i32* %bytesOver, align 4, !dbg !258
  %sub4 = sub i32 %14, %15, !dbg !258
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %12, i8* %13, i32 %sub4, i32 1, i1 false), !dbg !258
  %16 = load i32* %size.addr, align 4, !dbg !260
  %17 = load i32* %bytesOver, align 4, !dbg !260
  %sub5 = sub i32 %16, %17, !dbg !260
  %18 = load i32* @bytesLeft, align 4, !dbg !260
  %sub6 = sub i32 %18, %sub5, !dbg !260
  store i32 %sub6, i32* @bytesLeft, align 4, !dbg !260
  %19 = load i32* %size.addr, align 4, !dbg !261
  %20 = load i32* %bytesOver, align 4, !dbg !261
  %sub7 = sub i32 %19, %20, !dbg !261
  %21 = load i8** @readHead, align 4, !dbg !261
  %add.ptr = getelementptr inbounds i8* %21, i32 %sub7, !dbg !261
  store i8* %add.ptr, i8** @readHead, align 4, !dbg !261
  %22 = load i32* %size.addr, align 4, !dbg !262
  %23 = load i32* %bytesOver, align 4, !dbg !262
  %sub8 = sub i32 %22, %23, !dbg !262
  %24 = load i8** %out, align 4, !dbg !262
  %add.ptr9 = getelementptr inbounds i8* %24, i32 %sub8, !dbg !262
  store i8* %add.ptr9, i8** %out, align 4, !dbg !262
  %25 = load i32* %result, align 4, !dbg !263
  %inc = add i32 %25, 1, !dbg !263
  store i32 %inc, i32* %result, align 4, !dbg !263
  store i32 0, i32* %bytesOver, align 4, !dbg !264
  br label %if.end11, !dbg !265

if.else:                                          ; preds = %while.body
  %26 = load i8** %out, align 4, !dbg !266
  %27 = load i8** @readHead, align 4, !dbg !266
  %28 = load i32* @bytesLeft, align 4, !dbg !266
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %26, i8* %27, i32 %28, i32 1, i1 false), !dbg !266
  %29 = load i32* @bytesLeft, align 4, !dbg !268
  %30 = load i32* %bytesOver, align 4, !dbg !268
  %add = add nsw i32 %30, %29, !dbg !268
  store i32 %add, i32* %bytesOver, align 4, !dbg !268
  %31 = load i32* @bytesLeft, align 4, !dbg !269
  %32 = load i8** %out, align 4, !dbg !269
  %add.ptr10 = getelementptr inbounds i8* %32, i32 %31, !dbg !269
  store i8* %add.ptr10, i8** %out, align 4, !dbg !269
  store i32 0, i32* @bytesLeft, align 4, !dbg !270
  br label %if.end11

if.end11:                                         ; preds = %if.else, %if.then3
  %33 = load i32* @bytesLeft, align 4, !dbg !271
  %cmp12 = icmp eq i32 0, %33, !dbg !271
  br i1 %cmp12, label %if.then13, label %if.end41, !dbg !271

if.then13:                                        ; preds = %if.end11
  call void @llvm.dbg.declare(metadata !{i32* %l}, metadata !272), !dbg !274
  call void @llvm.dbg.declare(metadata !{[66 x i8]* %inBuf}, metadata !275), !dbg !276
  call void @llvm.memset.p0i8.i32(i8* getelementptr inbounds ([48 x i8]* @binaryBuffer, i32 0, i32 0), i8 0, i32 48, i32 1, i1 false), !dbg !277
  %34 = load i32* @more, align 4, !dbg !278
  %tobool = icmp ne i32 %34, 0, !dbg !278
  br i1 %tobool, label %if.end15, label %if.then14, !dbg !278

if.then14:                                        ; preds = %if.then13
  br label %while.end42, !dbg !279

if.end15:                                         ; preds = %if.then13
  %35 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !280
  %call16 = call arm_aapcscc  i32 @feof(%struct._IO_FILE.5* %35) nounwind, !dbg !280
  %tobool17 = icmp ne i32 %call16, 0, !dbg !280
  br i1 %tobool17, label %if.then18, label %if.end19, !dbg !280

if.then18:                                        ; preds = %if.end15
  br label %while.end42, !dbg !281

if.end19:                                         ; preds = %if.end15
  %arrayidx = getelementptr inbounds [66 x i8]* %inBuf, i32 0, i32 0, !dbg !282
  store i8 0, i8* %arrayidx, align 1, !dbg !282
  %arraydecay = getelementptr inbounds [66 x i8]* %inBuf, i32 0, i32 0, !dbg !283
  %36 = load %struct._IO_FILE.5** %stream.addr, align 4, !dbg !283
  %call20 = call arm_aapcscc  i8* @fgets(i8* %arraydecay, i32 66, %struct._IO_FILE.5* %36), !dbg !283
  %arrayidx21 = getelementptr inbounds [66 x i8]* %inBuf, i32 0, i32 0, !dbg !284
  %37 = load i8* %arrayidx21, align 1, !dbg !284
  %conv = sext i8 %37 to i32, !dbg !284
  %cmp22 = icmp eq i32 35, %conv, !dbg !284
  br i1 %cmp22, label %if.then24, label %if.end25, !dbg !284

if.then24:                                        ; preds = %if.end19
  br label %while.end42, !dbg !285

if.end25:                                         ; preds = %if.end19
  %arraydecay26 = getelementptr inbounds [66 x i8]* %inBuf, i32 0, i32 0, !dbg !286
  %call27 = call arm_aapcscc  i32 @strlen(i8* %arraydecay26) nounwind readonly, !dbg !286
  store i32 %call27, i32* %l, align 4, !dbg !286
  br label %while.cond28, !dbg !287

while.cond28:                                     ; preds = %while.body36, %if.end25
  %38 = load i32* %l, align 4, !dbg !287
  %sub29 = sub nsw i32 %38, 1, !dbg !287
  %arrayidx30 = getelementptr inbounds [66 x i8]* %inBuf, i32 0, i32 %sub29, !dbg !287
  %39 = load i8* %arrayidx30, align 1, !dbg !287
  %conv31 = sext i8 %39 to i32, !dbg !287
  %cmp32 = icmp slt i32 %conv31, 32, !dbg !287
  br i1 %cmp32, label %land.rhs, label %land.end, !dbg !287

land.rhs:                                         ; preds = %while.cond28
  %40 = load i32* %l, align 4, !dbg !287
  %cmp34 = icmp sgt i32 %40, 0, !dbg !287
  br label %land.end

land.end:                                         ; preds = %land.rhs, %while.cond28
  %41 = phi i1 [ false, %while.cond28 ], [ %cmp34, %land.rhs ]
  br i1 %41, label %while.body36, label %while.end

while.body36:                                     ; preds = %land.end
  %42 = load i32* %l, align 4, !dbg !288
  %dec = add nsw i32 %42, -1, !dbg !288
  store i32 %dec, i32* %l, align 4, !dbg !288
  %43 = load i32* %l, align 4, !dbg !290
  %arrayidx37 = getelementptr inbounds [66 x i8]* %inBuf, i32 0, i32 %43, !dbg !290
  store i8 0, i8* %arrayidx37, align 1, !dbg !290
  br label %while.cond28, !dbg !291

while.end:                                        ; preds = %land.end
  %arraydecay38 = getelementptr inbounds [66 x i8]* %inBuf, i32 0, i32 0, !dbg !292
  %call39 = call arm_aapcscc  i32 @decodeBuffer(i8* %arraydecay38, i8* getelementptr inbounds ([48 x i8]* @binaryBuffer, i32 0, i32 0), i32* @bytesLeft), !dbg !292
  store i32 %call39, i32* @more, align 4, !dbg !292
  %arraydecay40 = getelementptr inbounds [66 x i8]* %inBuf, i32 0, i32 0, !dbg !293
  call void @llvm.memset.p0i8.i32(i8* %arraydecay40, i8 0, i32 66, i32 1, i1 false), !dbg !293
  store i8* getelementptr inbounds ([48 x i8]* @binaryBuffer, i32 0, i32 0), i8** @readHead, align 4, !dbg !294
  br label %if.end41, !dbg !295

if.end41:                                         ; preds = %while.end, %if.end11
  br label %while.cond, !dbg !296

while.end42:                                      ; preds = %if.then24, %if.then18, %if.then14, %while.cond
  %44 = load i32* %result, align 4, !dbg !297
  store i32 %44, i32* %retval, !dbg !297
  br label %return, !dbg !297

return:                                           ; preds = %while.end42, %if.then
  %45 = load i32* %retval, !dbg !298
  ret i32 %45, !dbg !298
}

declare arm_aapcscc i32 @fread(i8*, i32, i32, %struct._IO_FILE.5*)

declare void @llvm.memcpy.p0i8.p0i8.i32(i8* nocapture, i8* nocapture, i32, i32, i1) nounwind

declare arm_aapcscc i32 @feof(%struct._IO_FILE.5*) nounwind

declare arm_aapcscc i8* @fgets(i8*, i32, %struct._IO_FILE.5*)

declare arm_aapcscc i32 @strlen(i8*) nounwind readonly

define internal arm_aapcscc i32 @decodeBuffer(i8* %inbuf, i8* %outbuf, i32* %outlength) nounwind uwtable {
entry:
  %inbuf.addr = alloca i8*, align 4
  %outbuf.addr = alloca i8*, align 4
  %outlength.addr = alloca i32*, align 4
  %bp = alloca i8*, align 4
  %length = alloca i32, align 4
  %c1 = alloca i32, align 4
  %c2 = alloca i32, align 4
  %c3 = alloca i32, align 4
  %c4 = alloca i32, align 4
  %hit_padding = alloca i32, align 4
  store i8* %inbuf, i8** %inbuf.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %inbuf.addr}, metadata !299), !dbg !300
  store i8* %outbuf, i8** %outbuf.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %outbuf.addr}, metadata !301), !dbg !302
  store i32* %outlength, i32** %outlength.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %outlength.addr}, metadata !303), !dbg !305
  call void @llvm.dbg.declare(metadata !{i8** %bp}, metadata !306), !dbg !308
  call void @llvm.dbg.declare(metadata !{i32* %length}, metadata !309), !dbg !310
  call void @llvm.dbg.declare(metadata !{i32* %c1}, metadata !311), !dbg !312
  call void @llvm.dbg.declare(metadata !{i32* %c2}, metadata !313), !dbg !314
  call void @llvm.dbg.declare(metadata !{i32* %c3}, metadata !315), !dbg !316
  call void @llvm.dbg.declare(metadata !{i32* %c4}, metadata !317), !dbg !318
  call void @llvm.dbg.declare(metadata !{i32* %hit_padding}, metadata !319), !dbg !320
  store i32 0, i32* %hit_padding, align 4, !dbg !321
  store i32 0, i32* %length, align 4, !dbg !322
  %0 = load i8** %inbuf.addr, align 4, !dbg !323
  store i8* %0, i8** %bp, align 4, !dbg !323
  br label %while.cond, !dbg !324

while.cond:                                       ; preds = %if.end64, %entry
  %1 = load i8** %bp, align 4, !dbg !324
  %2 = load i8* %1, !dbg !324
  %conv = zext i8 %2 to i32, !dbg !324
  %cmp = icmp ne i32 %conv, 0, !dbg !324
  br i1 %cmp, label %land.rhs, label %land.end, !dbg !324

land.rhs:                                         ; preds = %while.cond
  %3 = load i32* %hit_padding, align 4, !dbg !324
  %tobool = icmp ne i32 %3, 0, !dbg !324
  %lnot = xor i1 %tobool, true, !dbg !324
  br label %land.end

land.end:                                         ; preds = %land.rhs, %while.cond
  %4 = phi i1 [ false, %while.cond ], [ %lnot, %land.rhs ]
  br i1 %4, label %while.body, label %while.end

while.body:                                       ; preds = %land.end
  %5 = load i8** %bp, align 4, !dbg !325
  %arrayidx = getelementptr inbounds i8* %5, i32 3, !dbg !325
  %6 = load i8* %arrayidx, !dbg !325
  %conv2 = zext i8 %6 to i32, !dbg !325
  %cmp3 = icmp eq i32 %conv2, 61, !dbg !325
  br i1 %cmp3, label %if.then, label %if.else14, !dbg !325

if.then:                                          ; preds = %while.body
  store i32 1, i32* %hit_padding, align 4, !dbg !327
  %7 = load i8** %bp, align 4, !dbg !329
  %arrayidx5 = getelementptr inbounds i8* %7, i32 2, !dbg !329
  %8 = load i8* %arrayidx5, !dbg !329
  %conv6 = zext i8 %8 to i32, !dbg !329
  %cmp7 = icmp eq i32 %conv6, 61, !dbg !329
  br i1 %cmp7, label %if.then10, label %lor.lhs.false, !dbg !329

lor.lhs.false:                                    ; preds = %if.then
  %9 = load i8** %bp, align 4, !dbg !330
  %add.ptr = getelementptr inbounds i8* %9, i32 2, !dbg !330
  %call = call arm_aapcscc  i32 @strcmp(i8* %add.ptr, i8* getelementptr inbounds ([7 x i8]* @.str, i32 0, i32 0)) nounwind readonly, !dbg !330
  %tobool9 = icmp ne i32 %call, 0, !dbg !330
  br i1 %tobool9, label %if.else, label %if.then10, !dbg !330

if.then10:                                        ; preds = %lor.lhs.false, %if.then
  %10 = load i32* %length, align 4, !dbg !331
  %add = add nsw i32 %10, 1, !dbg !331
  store i32 %add, i32* %length, align 4, !dbg !331
  %11 = load i8** %bp, align 4, !dbg !333
  %arrayidx11 = getelementptr inbounds i8* %11, i32 2, !dbg !333
  store i8 65, i8* %arrayidx11, !dbg !333
  br label %if.end, !dbg !334

if.else:                                          ; preds = %lor.lhs.false
  %12 = load i32* %length, align 4, !dbg !335
  %add12 = add nsw i32 %12, 2, !dbg !335
  store i32 %add12, i32* %length, align 4, !dbg !335
  br label %if.end

if.end:                                           ; preds = %if.else, %if.then10
  %13 = load i8** %bp, align 4, !dbg !336
  %arrayidx13 = getelementptr inbounds i8* %13, i32 3, !dbg !336
  store i8 65, i8* %arrayidx13, !dbg !336
  br label %if.end16, !dbg !337

if.else14:                                        ; preds = %while.body
  %14 = load i32* %length, align 4, !dbg !338
  %add15 = add nsw i32 %14, 3, !dbg !338
  store i32 %add15, i32* %length, align 4, !dbg !338
  br label %if.end16

if.end16:                                         ; preds = %if.else14, %if.end
  %15 = load i8** %bp, align 4, !dbg !339
  %arrayidx17 = getelementptr inbounds i8* %15, i32 0, !dbg !339
  %16 = load i8* %arrayidx17, !dbg !339
  %conv18 = zext i8 %16 to i32, !dbg !339
  %and = and i32 %conv18, 128, !dbg !339
  %tobool19 = icmp ne i32 %and, 0, !dbg !339
  br i1 %tobool19, label %if.then62, label %lor.lhs.false20, !dbg !339

lor.lhs.false20:                                  ; preds = %if.end16
  %17 = load i8** %bp, align 4, !dbg !339
  %arrayidx21 = getelementptr inbounds i8* %17, i32 0, !dbg !339
  %18 = load i8* %arrayidx21, !dbg !339
  %idxprom = zext i8 %18 to i32, !dbg !339
  %arrayidx22 = getelementptr inbounds [128 x i8]* @asctobin, i32 0, i32 %idxprom, !dbg !339
  %19 = load i8* %arrayidx22, align 1, !dbg !339
  %conv23 = zext i8 %19 to i32, !dbg !339
  store i32 %conv23, i32* %c1, align 4, !dbg !339
  %and24 = and i32 %conv23, 128, !dbg !339
  %tobool25 = icmp ne i32 %and24, 0, !dbg !339
  br i1 %tobool25, label %if.then62, label %lor.lhs.false26, !dbg !339

lor.lhs.false26:                                  ; preds = %lor.lhs.false20
  %20 = load i8** %bp, align 4, !dbg !339
  %arrayidx27 = getelementptr inbounds i8* %20, i32 1, !dbg !339
  %21 = load i8* %arrayidx27, !dbg !339
  %conv28 = zext i8 %21 to i32, !dbg !339
  %and29 = and i32 %conv28, 128, !dbg !339
  %tobool30 = icmp ne i32 %and29, 0, !dbg !339
  br i1 %tobool30, label %if.then62, label %lor.lhs.false31, !dbg !339

lor.lhs.false31:                                  ; preds = %lor.lhs.false26
  %22 = load i8** %bp, align 4, !dbg !339
  %arrayidx32 = getelementptr inbounds i8* %22, i32 1, !dbg !339
  %23 = load i8* %arrayidx32, !dbg !339
  %idxprom33 = zext i8 %23 to i32, !dbg !339
  %arrayidx34 = getelementptr inbounds [128 x i8]* @asctobin, i32 0, i32 %idxprom33, !dbg !339
  %24 = load i8* %arrayidx34, align 1, !dbg !339
  %conv35 = zext i8 %24 to i32, !dbg !339
  store i32 %conv35, i32* %c2, align 4, !dbg !339
  %and36 = and i32 %conv35, 128, !dbg !339
  %tobool37 = icmp ne i32 %and36, 0, !dbg !339
  br i1 %tobool37, label %if.then62, label %lor.lhs.false38, !dbg !339

lor.lhs.false38:                                  ; preds = %lor.lhs.false31
  %25 = load i8** %bp, align 4, !dbg !339
  %arrayidx39 = getelementptr inbounds i8* %25, i32 2, !dbg !339
  %26 = load i8* %arrayidx39, !dbg !339
  %conv40 = zext i8 %26 to i32, !dbg !339
  %and41 = and i32 %conv40, 128, !dbg !339
  %tobool42 = icmp ne i32 %and41, 0, !dbg !339
  br i1 %tobool42, label %if.then62, label %lor.lhs.false43, !dbg !339

lor.lhs.false43:                                  ; preds = %lor.lhs.false38
  %27 = load i8** %bp, align 4, !dbg !339
  %arrayidx44 = getelementptr inbounds i8* %27, i32 2, !dbg !339
  %28 = load i8* %arrayidx44, !dbg !339
  %idxprom45 = zext i8 %28 to i32, !dbg !339
  %arrayidx46 = getelementptr inbounds [128 x i8]* @asctobin, i32 0, i32 %idxprom45, !dbg !339
  %29 = load i8* %arrayidx46, align 1, !dbg !339
  %conv47 = zext i8 %29 to i32, !dbg !339
  store i32 %conv47, i32* %c3, align 4, !dbg !339
  %and48 = and i32 %conv47, 128, !dbg !339
  %tobool49 = icmp ne i32 %and48, 0, !dbg !339
  br i1 %tobool49, label %if.then62, label %lor.lhs.false50, !dbg !339

lor.lhs.false50:                                  ; preds = %lor.lhs.false43
  %30 = load i8** %bp, align 4, !dbg !339
  %arrayidx51 = getelementptr inbounds i8* %30, i32 3, !dbg !339
  %31 = load i8* %arrayidx51, !dbg !339
  %conv52 = zext i8 %31 to i32, !dbg !339
  %and53 = and i32 %conv52, 128, !dbg !339
  %tobool54 = icmp ne i32 %and53, 0, !dbg !339
  br i1 %tobool54, label %if.then62, label %lor.lhs.false55, !dbg !339

lor.lhs.false55:                                  ; preds = %lor.lhs.false50
  %32 = load i8** %bp, align 4, !dbg !339
  %arrayidx56 = getelementptr inbounds i8* %32, i32 3, !dbg !339
  %33 = load i8* %arrayidx56, !dbg !339
  %idxprom57 = zext i8 %33 to i32, !dbg !339
  %arrayidx58 = getelementptr inbounds [128 x i8]* @asctobin, i32 0, i32 %idxprom57, !dbg !339
  %34 = load i8* %arrayidx58, align 1, !dbg !339
  %conv59 = zext i8 %34 to i32, !dbg !339
  store i32 %conv59, i32* %c4, align 4, !dbg !339
  %and60 = and i32 %conv59, 128, !dbg !339
  %tobool61 = icmp ne i32 %and60, 0, !dbg !339
  br i1 %tobool61, label %if.then62, label %if.end64, !dbg !339

if.then62:                                        ; preds = %lor.lhs.false55, %lor.lhs.false50, %lor.lhs.false43, %lor.lhs.false38, %lor.lhs.false31, %lor.lhs.false26, %lor.lhs.false20, %if.end16
  %35 = load %struct._IO_FILE.5** @stderr, align 4, !dbg !340
  %call63 = call arm_aapcscc  i32 (%struct._IO_FILE.5*, i8*, ...)* @fprintf(%struct._IO_FILE.5* %35, i8* getelementptr inbounds ([78 x i8]* @err_decode_failed, i32 0, i32 0)), !dbg !340
  call arm_aapcscc  void @exit(i32 1) noreturn nounwind, !dbg !342
  unreachable, !dbg !342

if.end64:                                         ; preds = %lor.lhs.false55
  %36 = load i8** %bp, align 4, !dbg !343
  %add.ptr65 = getelementptr inbounds i8* %36, i32 4, !dbg !343
  store i8* %add.ptr65, i8** %bp, align 4, !dbg !343
  %37 = load i32* %c1, align 4, !dbg !344
  %shl = shl i32 %37, 2, !dbg !344
  %38 = load i32* %c2, align 4, !dbg !344
  %shr = lshr i32 %38, 4, !dbg !344
  %or = or i32 %shl, %shr, !dbg !344
  %conv66 = trunc i32 %or to i8, !dbg !344
  %39 = load i8** %outbuf.addr, align 4, !dbg !344
  %incdec.ptr = getelementptr inbounds i8* %39, i32 1, !dbg !344
  store i8* %incdec.ptr, i8** %outbuf.addr, align 4, !dbg !344
  store i8 %conv66, i8* %39, !dbg !344
  %40 = load i32* %c2, align 4, !dbg !345
  %shl67 = shl i32 %40, 4, !dbg !345
  %41 = load i32* %c3, align 4, !dbg !345
  %shr68 = lshr i32 %41, 2, !dbg !345
  %or69 = or i32 %shl67, %shr68, !dbg !345
  %conv70 = trunc i32 %or69 to i8, !dbg !345
  %42 = load i8** %outbuf.addr, align 4, !dbg !345
  %incdec.ptr71 = getelementptr inbounds i8* %42, i32 1, !dbg !345
  store i8* %incdec.ptr71, i8** %outbuf.addr, align 4, !dbg !345
  store i8 %conv70, i8* %42, !dbg !345
  %43 = load i32* %c3, align 4, !dbg !346
  %shl72 = shl i32 %43, 6, !dbg !346
  %44 = load i32* %c4, align 4, !dbg !346
  %or73 = or i32 %shl72, %44, !dbg !346
  %conv74 = trunc i32 %or73 to i8, !dbg !346
  %45 = load i8** %outbuf.addr, align 4, !dbg !346
  %incdec.ptr75 = getelementptr inbounds i8* %45, i32 1, !dbg !346
  store i8* %incdec.ptr75, i8** %outbuf.addr, align 4, !dbg !346
  store i8 %conv74, i8* %45, !dbg !346
  br label %while.cond, !dbg !347

while.end:                                        ; preds = %land.end
  %46 = load i32* %length, align 4, !dbg !348
  %47 = load i32** %outlength.addr, align 4, !dbg !348
  store i32 %46, i32* %47, !dbg !348
  %48 = load i32* %hit_padding, align 4, !dbg !349
  %tobool76 = icmp ne i32 %48, 0, !dbg !349
  %lnot77 = xor i1 %tobool76, true, !dbg !349
  %lnot.ext = zext i1 %lnot77 to i32, !dbg !349
  ret i32 %lnot.ext, !dbg !349
}

declare arm_aapcscc i32 @strcmp(i8*, i8*) nounwind readonly

declare arm_aapcscc i32 @fprintf(%struct._IO_FILE.5*, i8*, ...)

declare arm_aapcscc void @exit(i32) noreturn nounwind

declare arm_aapcscc i32 @fputs(i8*, %struct._IO_FILE.5*)

define internal arm_aapcscc void @encode(i8* %p, i8* %buffer, i32 %count) nounwind uwtable {
entry:
  %p.addr = alloca i8*, align 4
  %buffer.addr = alloca i8*, align 4
  %count.addr = alloca i32, align 4
  store i8* %p, i8** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %p.addr}, metadata !350), !dbg !351
  store i8* %buffer, i8** %buffer.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %buffer.addr}, metadata !352), !dbg !353
  store i32 %count, i32* %count.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %count.addr}, metadata !354), !dbg !355
  %0 = load i32* %count.addr, align 4, !dbg !356
  %cmp = icmp slt i32 %0, 3, !dbg !356
  br i1 %cmp, label %if.then, label %if.end, !dbg !356

if.then:                                          ; preds = %entry
  %1 = load i32* %count.addr, align 4, !dbg !358
  %2 = load i8** %p.addr, align 4, !dbg !358
  %arrayidx = getelementptr inbounds i8* %2, i32 %1, !dbg !358
  store i8 0, i8* %arrayidx, !dbg !358
  %3 = load i8** %buffer.addr, align 4, !dbg !360
  %arrayidx1 = getelementptr inbounds i8* %3, i32 3, !dbg !360
  store i8 61, i8* %arrayidx1, !dbg !360
  %4 = load i8** %buffer.addr, align 4, !dbg !360
  %arrayidx2 = getelementptr inbounds i8* %4, i32 2, !dbg !360
  store i8 61, i8* %arrayidx2, !dbg !360
  br label %if.end, !dbg !361

if.end:                                           ; preds = %if.then, %entry
  %5 = load i8** %p.addr, align 4, !dbg !362
  %arrayidx3 = getelementptr inbounds i8* %5, i32 0, !dbg !362
  %6 = load i8* %arrayidx3, !dbg !362
  %conv = zext i8 %6 to i32, !dbg !362
  %shr = ashr i32 %conv, 2, !dbg !362
  %arrayidx4 = getelementptr inbounds [65 x i8]* @bintoasc, i32 0, i32 %shr, !dbg !362
  %7 = load i8* %arrayidx4, align 1, !dbg !362
  %8 = load i8** %buffer.addr, align 4, !dbg !362
  %arrayidx5 = getelementptr inbounds i8* %8, i32 0, !dbg !362
  store i8 %7, i8* %arrayidx5, !dbg !362
  %9 = load i8** %p.addr, align 4, !dbg !363
  %arrayidx6 = getelementptr inbounds i8* %9, i32 0, !dbg !363
  %10 = load i8* %arrayidx6, !dbg !363
  %conv7 = zext i8 %10 to i32, !dbg !363
  %shl = shl i32 %conv7, 4, !dbg !363
  %and = and i32 %shl, 48, !dbg !363
  %11 = load i8** %p.addr, align 4, !dbg !363
  %arrayidx8 = getelementptr inbounds i8* %11, i32 1, !dbg !363
  %12 = load i8* %arrayidx8, !dbg !363
  %conv9 = zext i8 %12 to i32, !dbg !363
  %shr10 = ashr i32 %conv9, 4, !dbg !363
  %and11 = and i32 %shr10, 15, !dbg !363
  %or = or i32 %and, %and11, !dbg !363
  %arrayidx12 = getelementptr inbounds [65 x i8]* @bintoasc, i32 0, i32 %or, !dbg !363
  %13 = load i8* %arrayidx12, align 1, !dbg !363
  %14 = load i8** %buffer.addr, align 4, !dbg !363
  %arrayidx13 = getelementptr inbounds i8* %14, i32 1, !dbg !363
  store i8 %13, i8* %arrayidx13, !dbg !363
  %15 = load i32* %count.addr, align 4, !dbg !364
  %cmp14 = icmp sgt i32 %15, 1, !dbg !364
  br i1 %cmp14, label %if.then16, label %if.end37, !dbg !364

if.then16:                                        ; preds = %if.end
  %16 = load i8** %p.addr, align 4, !dbg !365
  %arrayidx17 = getelementptr inbounds i8* %16, i32 1, !dbg !365
  %17 = load i8* %arrayidx17, !dbg !365
  %conv18 = zext i8 %17 to i32, !dbg !365
  %shl19 = shl i32 %conv18, 2, !dbg !365
  %and20 = and i32 %shl19, 60, !dbg !365
  %18 = load i8** %p.addr, align 4, !dbg !365
  %arrayidx21 = getelementptr inbounds i8* %18, i32 2, !dbg !365
  %19 = load i8* %arrayidx21, !dbg !365
  %conv22 = zext i8 %19 to i32, !dbg !365
  %shr23 = ashr i32 %conv22, 6, !dbg !365
  %and24 = and i32 %shr23, 3, !dbg !365
  %or25 = or i32 %and20, %and24, !dbg !365
  %arrayidx26 = getelementptr inbounds [65 x i8]* @bintoasc, i32 0, i32 %or25, !dbg !365
  %20 = load i8* %arrayidx26, align 1, !dbg !365
  %21 = load i8** %buffer.addr, align 4, !dbg !365
  %arrayidx27 = getelementptr inbounds i8* %21, i32 2, !dbg !365
  store i8 %20, i8* %arrayidx27, !dbg !365
  %22 = load i32* %count.addr, align 4, !dbg !367
  %cmp28 = icmp sgt i32 %22, 2, !dbg !367
  br i1 %cmp28, label %if.then30, label %if.end36, !dbg !367

if.then30:                                        ; preds = %if.then16
  %23 = load i8** %p.addr, align 4, !dbg !368
  %arrayidx31 = getelementptr inbounds i8* %23, i32 2, !dbg !368
  %24 = load i8* %arrayidx31, !dbg !368
  %conv32 = zext i8 %24 to i32, !dbg !368
  %and33 = and i32 %conv32, 63, !dbg !368
  %arrayidx34 = getelementptr inbounds [65 x i8]* @bintoasc, i32 0, i32 %and33, !dbg !368
  %25 = load i8* %arrayidx34, align 1, !dbg !368
  %26 = load i8** %buffer.addr, align 4, !dbg !368
  %arrayidx35 = getelementptr inbounds i8* %26, i32 3, !dbg !368
  store i8 %25, i8* %arrayidx35, !dbg !368
  br label %if.end36, !dbg !368

if.end36:                                         ; preds = %if.then30, %if.then16
  br label %if.end37, !dbg !369

if.end37:                                         ; preds = %if.end36, %if.end
  ret void, !dbg !370
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !43} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !11, metadata !17, metadata !25, metadata !28, metadata !31, metadata !34, metadata !37, metadata !40}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"burnBinasc", metadata !"burnBinasc", metadata !"", metadata !6, i32 59, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void ()* @burnBinasc, null, null, metadata !9} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"binasc.c", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{null}
!9 = metadata !{metadata !10}
!10 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!11 = metadata !{i32 720942, i32 0, metadata !6, metadata !"flushArmour", metadata !"flushArmour", metadata !"", metadata !6, i32 114, metadata !12, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct._IO_FILE.5*)* @flushArmour, null, null, metadata !15} ; [ DW_TAG_subprogram ]
!12 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !13, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!13 = metadata !{metadata !14}
!14 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!15 = metadata !{metadata !16}
!16 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!17 = metadata !{i32 720942, i32 0, metadata !6, metadata !"fwritePlus", metadata !"fwritePlus", metadata !"", metadata !6, i32 122, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i8*, i32, i32, %struct._IO_FILE.5*)* @fwritePlus, null, null, metadata !23} ; [ DW_TAG_subprogram ]
!18 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !19, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!19 = metadata !{metadata !20}
!20 = metadata !{i32 720918, null, metadata !"size_t", metadata !21, i32 211, i64 0, i64 0, i64 0, i32 0, metadata !22} ; [ DW_TAG_typedef ]
!21 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!22 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!23 = metadata !{metadata !24}
!24 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!25 = metadata !{i32 720942, i32 0, metadata !6, metadata !"fputcPlus", metadata !"fputcPlus", metadata !"", metadata !6, i32 163, metadata !12, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i32, %struct._IO_FILE.5*)* @fputcPlus, null, null, metadata !26} ; [ DW_TAG_subprogram ]
!26 = metadata !{metadata !27}
!27 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!28 = metadata !{i32 720942, i32 0, metadata !6, metadata !"freadPlus", metadata !"freadPlus", metadata !"", metadata !6, i32 240, metadata !18, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i8*, i32, i32, %struct._IO_FILE.5*)* @freadPlus, null, null, metadata !29} ; [ DW_TAG_subprogram ]
!29 = metadata !{metadata !30}
!30 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!31 = metadata !{i32 720942, i32 0, metadata !6, metadata !"decodeBuffer", metadata !"decodeBuffer", metadata !"", metadata !6, i32 181, metadata !12, i1 true, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i8*, i8*, i32*)* @decodeBuffer, null, null, metadata !32} ; [ DW_TAG_subprogram ]
!32 = metadata !{metadata !33}
!33 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!34 = metadata !{i32 720942, i32 0, metadata !6, metadata !"flushBuffer", metadata !"flushBuffer", metadata !"", metadata !6, i32 65, metadata !12, i1 true, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct._IO_FILE.5*)* @flushBuffer, null, null, metadata !35} ; [ DW_TAG_subprogram ]
!35 = metadata !{metadata !36}
!36 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!37 = metadata !{i32 720942, i32 0, metadata !6, metadata !"push3bytes", metadata !"push3bytes", metadata !"", metadata !6, i32 97, metadata !12, i1 true, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct._IO_FILE.5*)* @push3bytes, null, null, metadata !38} ; [ DW_TAG_subprogram ]
!38 = metadata !{metadata !39}
!39 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!40 = metadata !{i32 720942, i32 0, metadata !6, metadata !"encode", metadata !"encode", metadata !"", metadata !6, i32 81, metadata !7, i1 true, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i8*, i8*, i32)* @encode, null, null, metadata !41} ; [ DW_TAG_subprogram ]
!41 = metadata !{metadata !42}
!42 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!43 = metadata !{metadata !44}
!44 = metadata !{metadata !45, metadata !50, metadata !55, metadata !59, metadata !63, metadata !67, metadata !68, metadata !70, metadata !71, metadata !72, metadata !74, metadata !78}
!45 = metadata !{i32 720948, i32 0, null, metadata !"asciiBuffer", metadata !"asciiBuffer", metadata !"", metadata !6, i32 52, metadata !46, i32 1, i32 1, [66 x i8]* @asciiBuffer} ; [ DW_TAG_variable ]
!46 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 528, i64 8, i32 0, i32 0, metadata !47, metadata !48, i32 0, i32 0} ; [ DW_TAG_array_type ]
!47 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!48 = metadata !{metadata !49}
!49 = metadata !{i32 720929, i64 0, i64 65}       ; [ DW_TAG_subrange_type ]
!50 = metadata !{i32 720948, i32 0, null, metadata !"bin", metadata !"bin", metadata !"", metadata !6, i32 55, metadata !51, i32 1, i32 1, [3 x i8]* @bin} ; [ DW_TAG_variable ]
!51 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 24, i64 8, i32 0, i32 0, metadata !52, metadata !53, i32 0, i32 0} ; [ DW_TAG_array_type ]
!52 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!53 = metadata !{metadata !54}
!54 = metadata !{i32 720929, i64 0, i64 2}        ; [ DW_TAG_subrange_type ]
!55 = metadata !{i32 720948, i32 0, null, metadata !"binaryBuffer", metadata !"binaryBuffer", metadata !"", metadata !6, i32 231, metadata !56, i32 1, i32 1, [48 x i8]* @binaryBuffer} ; [ DW_TAG_variable ]
!56 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 384, i64 8, i32 0, i32 0, metadata !52, metadata !57, i32 0, i32 0} ; [ DW_TAG_array_type ]
!57 = metadata !{metadata !58}
!58 = metadata !{i32 720929, i64 0, i64 47}       ; [ DW_TAG_subrange_type ]
!59 = metadata !{i32 720948, i32 0, null, metadata !"err_decode_failed", metadata !"err_decode_failed", metadata !"", metadata !6, i32 15, metadata !60, i32 1, i32 1, [78 x i8]* @err_decode_failed} ; [ DW_TAG_variable ]
!60 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 624, i64 8, i32 0, i32 0, metadata !47, metadata !61, i32 0, i32 0} ; [ DW_TAG_array_type ]
!61 = metadata !{metadata !62}
!62 = metadata !{i32 720929, i64 0, i64 77}       ; [ DW_TAG_subrange_type ]
!63 = metadata !{i32 720948, i32 0, null, metadata !"asctobin", metadata !"asctobin", metadata !"", metadata !6, i32 28, metadata !64, i32 1, i32 1, [128 x i8]* @asctobin} ; [ DW_TAG_variable ]
!64 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 1024, i64 8, i32 0, i32 0, metadata !52, metadata !65, i32 0, i32 0} ; [ DW_TAG_array_type ]
!65 = metadata !{metadata !66}
!66 = metadata !{i32 720929, i64 0, i64 127}      ; [ DW_TAG_subrange_type ]
!67 = metadata !{i32 720948, i32 0, null, metadata !"more", metadata !"more", metadata !"", metadata !6, i32 234, metadata !14, i32 1, i32 1, i32* @more} ; [ DW_TAG_variable ]
!68 = metadata !{i32 720948, i32 0, null, metadata !"readHead", metadata !"readHead", metadata !"", metadata !6, i32 232, metadata !69, i32 1, i32 1, i8** @readHead} ; [ DW_TAG_variable ]
!69 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !52} ; [ DW_TAG_pointer_type ]
!70 = metadata !{i32 720948, i32 0, null, metadata !"bytesLeft", metadata !"bytesLeft", metadata !"", metadata !6, i32 233, metadata !14, i32 1, i32 1, i32* @bytesLeft} ; [ DW_TAG_variable ]
!71 = metadata !{i32 720948, i32 0, null, metadata !"space", metadata !"space", metadata !"", metadata !6, i32 54, metadata !14, i32 1, i32 1, i32* @space} ; [ DW_TAG_variable ]
!72 = metadata !{i32 720948, i32 0, null, metadata !"writeHead", metadata !"writeHead", metadata !"", metadata !6, i32 53, metadata !73, i32 1, i32 1, i8** @writeHead} ; [ DW_TAG_variable ]
!73 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !47} ; [ DW_TAG_pointer_type ]
!74 = metadata !{i32 720948, i32 0, null, metadata !"bintoasc", metadata !"bintoasc", metadata !"", metadata !6, i32 21, metadata !75, i32 1, i32 1, [65 x i8]* @bintoasc} ; [ DW_TAG_variable ]
!75 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 520, i64 8, i32 0, i32 0, metadata !52, metadata !76, i32 0, i32 0} ; [ DW_TAG_array_type ]
!76 = metadata !{metadata !77}
!77 = metadata !{i32 720929, i64 0, i64 64}       ; [ DW_TAG_subrange_type ]
!78 = metadata !{i32 720948, i32 0, null, metadata !"inBin", metadata !"inBin", metadata !"", metadata !6, i32 56, metadata !14, i32 1, i32 1, i32* @inBin} ; [ DW_TAG_variable ]
!79 = metadata !{i32 60, i32 3, metadata !80, null}
!80 = metadata !{i32 720907, metadata !5, i32 59, i32 1, metadata !6, i32 369} ; [ DW_TAG_lexical_block ]
!81 = metadata !{i32 61, i32 3, metadata !80, null}
!82 = metadata !{i32 62, i32 1, metadata !80, null}
!83 = metadata !{i32 721153, metadata !11, metadata !"stream", metadata !6, i32 16777329, metadata !84, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!84 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !85} ; [ DW_TAG_pointer_type ]
!85 = metadata !{i32 720918, null, metadata !"FILE", metadata !6, i32 49, i64 0, i64 0, i64 0, i32 0, metadata !86} ; [ DW_TAG_typedef ]
!86 = metadata !{i32 720915, null, metadata !"_IO_FILE", metadata !21, i32 271, i64 1216, i64 64, i32 0, i32 0, null, metadata !87, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!87 = metadata !{metadata !88, metadata !89, metadata !90, metadata !91, metadata !92, metadata !93, metadata !94, metadata !95, metadata !96, metadata !97, metadata !98, metadata !99, metadata !100, metadata !108, metadata !109, metadata !110, metadata !111, metadata !114, metadata !116, metadata !118, metadata !122, metadata !124, metadata !128, metadata !129, metadata !130, metadata !131, metadata !132, metadata !133, metadata !134}
!88 = metadata !{i32 720909, metadata !86, metadata !"_flags", metadata !21, i32 272, i64 32, i64 32, i64 0, i32 0, metadata !14} ; [ DW_TAG_member ]
!89 = metadata !{i32 720909, metadata !86, metadata !"_IO_read_ptr", metadata !21, i32 277, i64 32, i64 32, i64 32, i32 0, metadata !73} ; [ DW_TAG_member ]
!90 = metadata !{i32 720909, metadata !86, metadata !"_IO_read_end", metadata !21, i32 278, i64 32, i64 32, i64 64, i32 0, metadata !73} ; [ DW_TAG_member ]
!91 = metadata !{i32 720909, metadata !86, metadata !"_IO_read_base", metadata !21, i32 279, i64 32, i64 32, i64 96, i32 0, metadata !73} ; [ DW_TAG_member ]
!92 = metadata !{i32 720909, metadata !86, metadata !"_IO_write_base", metadata !21, i32 280, i64 32, i64 32, i64 128, i32 0, metadata !73} ; [ DW_TAG_member ]
!93 = metadata !{i32 720909, metadata !86, metadata !"_IO_write_ptr", metadata !21, i32 281, i64 32, i64 32, i64 160, i32 0, metadata !73} ; [ DW_TAG_member ]
!94 = metadata !{i32 720909, metadata !86, metadata !"_IO_write_end", metadata !21, i32 282, i64 32, i64 32, i64 192, i32 0, metadata !73} ; [ DW_TAG_member ]
!95 = metadata !{i32 720909, metadata !86, metadata !"_IO_buf_base", metadata !21, i32 283, i64 32, i64 32, i64 224, i32 0, metadata !73} ; [ DW_TAG_member ]
!96 = metadata !{i32 720909, metadata !86, metadata !"_IO_buf_end", metadata !21, i32 284, i64 32, i64 32, i64 256, i32 0, metadata !73} ; [ DW_TAG_member ]
!97 = metadata !{i32 720909, metadata !86, metadata !"_IO_save_base", metadata !21, i32 286, i64 32, i64 32, i64 288, i32 0, metadata !73} ; [ DW_TAG_member ]
!98 = metadata !{i32 720909, metadata !86, metadata !"_IO_backup_base", metadata !21, i32 287, i64 32, i64 32, i64 320, i32 0, metadata !73} ; [ DW_TAG_member ]
!99 = metadata !{i32 720909, metadata !86, metadata !"_IO_save_end", metadata !21, i32 288, i64 32, i64 32, i64 352, i32 0, metadata !73} ; [ DW_TAG_member ]
!100 = metadata !{i32 720909, metadata !86, metadata !"_markers", metadata !21, i32 290, i64 32, i64 32, i64 384, i32 0, metadata !101} ; [ DW_TAG_member ]
!101 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !102} ; [ DW_TAG_pointer_type ]
!102 = metadata !{i32 720915, null, metadata !"_IO_marker", metadata !21, i32 186, i64 96, i64 32, i32 0, i32 0, null, metadata !103, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!103 = metadata !{metadata !104, metadata !105, metadata !107}
!104 = metadata !{i32 720909, metadata !102, metadata !"_next", metadata !21, i32 187, i64 32, i64 32, i64 0, i32 0, metadata !101} ; [ DW_TAG_member ]
!105 = metadata !{i32 720909, metadata !102, metadata !"_sbuf", metadata !21, i32 188, i64 32, i64 32, i64 32, i32 0, metadata !106} ; [ DW_TAG_member ]
!106 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !86} ; [ DW_TAG_pointer_type ]
!107 = metadata !{i32 720909, metadata !102, metadata !"_pos", metadata !21, i32 192, i64 32, i64 32, i64 64, i32 0, metadata !14} ; [ DW_TAG_member ]
!108 = metadata !{i32 720909, metadata !86, metadata !"_chain", metadata !21, i32 292, i64 32, i64 32, i64 416, i32 0, metadata !106} ; [ DW_TAG_member ]
!109 = metadata !{i32 720909, metadata !86, metadata !"_fileno", metadata !21, i32 294, i64 32, i64 32, i64 448, i32 0, metadata !14} ; [ DW_TAG_member ]
!110 = metadata !{i32 720909, metadata !86, metadata !"_flags2", metadata !21, i32 298, i64 32, i64 32, i64 480, i32 0, metadata !14} ; [ DW_TAG_member ]
!111 = metadata !{i32 720909, metadata !86, metadata !"_old_offset", metadata !21, i32 300, i64 32, i64 32, i64 512, i32 0, metadata !112} ; [ DW_TAG_member ]
!112 = metadata !{i32 720918, null, metadata !"__off_t", metadata !21, i32 141, i64 0, i64 0, i64 0, i32 0, metadata !113} ; [ DW_TAG_typedef ]
!113 = metadata !{i32 720932, null, metadata !"long int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!114 = metadata !{i32 720909, metadata !86, metadata !"_cur_column", metadata !21, i32 304, i64 16, i64 16, i64 544, i32 0, metadata !115} ; [ DW_TAG_member ]
!115 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!116 = metadata !{i32 720909, metadata !86, metadata !"_vtable_offset", metadata !21, i32 305, i64 8, i64 8, i64 560, i32 0, metadata !117} ; [ DW_TAG_member ]
!117 = metadata !{i32 720932, null, metadata !"signed char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!118 = metadata !{i32 720909, metadata !86, metadata !"_shortbuf", metadata !21, i32 306, i64 8, i64 8, i64 568, i32 0, metadata !119} ; [ DW_TAG_member ]
!119 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8, i64 8, i32 0, i32 0, metadata !47, metadata !120, i32 0, i32 0} ; [ DW_TAG_array_type ]
!120 = metadata !{metadata !121}
!121 = metadata !{i32 720929, i64 0, i64 0}       ; [ DW_TAG_subrange_type ]
!122 = metadata !{i32 720909, metadata !86, metadata !"_lock", metadata !21, i32 310, i64 32, i64 32, i64 576, i32 0, metadata !123} ; [ DW_TAG_member ]
!123 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, null} ; [ DW_TAG_pointer_type ]
!124 = metadata !{i32 720909, metadata !86, metadata !"_offset", metadata !21, i32 319, i64 64, i64 64, i64 640, i32 0, metadata !125} ; [ DW_TAG_member ]
!125 = metadata !{i32 720918, null, metadata !"__off64_t", metadata !21, i32 142, i64 0, i64 0, i64 0, i32 0, metadata !126} ; [ DW_TAG_typedef ]
!126 = metadata !{i32 720918, null, metadata !"__quad_t", metadata !21, i32 56, i64 0, i64 0, i64 0, i32 0, metadata !127} ; [ DW_TAG_typedef ]
!127 = metadata !{i32 720932, null, metadata !"long long int", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!128 = metadata !{i32 720909, metadata !86, metadata !"__pad1", metadata !21, i32 328, i64 32, i64 32, i64 704, i32 0, metadata !123} ; [ DW_TAG_member ]
!129 = metadata !{i32 720909, metadata !86, metadata !"__pad2", metadata !21, i32 329, i64 32, i64 32, i64 736, i32 0, metadata !123} ; [ DW_TAG_member ]
!130 = metadata !{i32 720909, metadata !86, metadata !"__pad3", metadata !21, i32 330, i64 32, i64 32, i64 768, i32 0, metadata !123} ; [ DW_TAG_member ]
!131 = metadata !{i32 720909, metadata !86, metadata !"__pad4", metadata !21, i32 331, i64 32, i64 32, i64 800, i32 0, metadata !123} ; [ DW_TAG_member ]
!132 = metadata !{i32 720909, metadata !86, metadata !"__pad5", metadata !21, i32 332, i64 32, i64 32, i64 832, i32 0, metadata !20} ; [ DW_TAG_member ]
!133 = metadata !{i32 720909, metadata !86, metadata !"_mode", metadata !21, i32 334, i64 32, i64 32, i64 864, i32 0, metadata !14} ; [ DW_TAG_member ]
!134 = metadata !{i32 720909, metadata !86, metadata !"_unused2", metadata !21, i32 336, i64 320, i64 8, i64 896, i32 0, metadata !135} ; [ DW_TAG_member ]
!135 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !47, metadata !136, i32 0, i32 0} ; [ DW_TAG_array_type ]
!136 = metadata !{metadata !137}
!137 = metadata !{i32 720929, i64 0, i64 39}      ; [ DW_TAG_subrange_type ]
!138 = metadata !{i32 113, i32 24, metadata !11, null}
!139 = metadata !{i32 721152, metadata !140, metadata !"result", metadata !6, i32 115, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!140 = metadata !{i32 720907, metadata !11, i32 114, i32 1, metadata !6, i32 370} ; [ DW_TAG_lexical_block ]
!141 = metadata !{i32 115, i32 8, metadata !140, null}
!142 = metadata !{i32 115, i32 18, metadata !140, null}
!143 = metadata !{i32 116, i32 4, metadata !140, null}
!144 = metadata !{i32 116, i32 23, metadata !140, null}
!145 = metadata !{i32 117, i32 4, metadata !140, null}
!146 = metadata !{i32 117, i32 24, metadata !140, null}
!147 = metadata !{i32 118, i32 4, metadata !140, null}
!148 = metadata !{i32 721153, metadata !37, metadata !"stream", metadata !6, i32 16777312, metadata !84, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!149 = metadata !{i32 96, i32 29, metadata !37, null}
!150 = metadata !{i32 99, i32 4, metadata !151, null}
!151 = metadata !{i32 720907, metadata !37, i32 97, i32 1, metadata !6, i32 389} ; [ DW_TAG_lexical_block ]
!152 = metadata !{i32 721152, metadata !153, metadata !"push", metadata !6, i32 101, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!153 = metadata !{i32 720907, metadata !151, i32 100, i32 4, metadata !6, i32 390} ; [ DW_TAG_lexical_block ]
!154 = metadata !{i32 101, i32 11, metadata !153, null}
!155 = metadata !{i32 101, i32 18, metadata !153, null}
!156 = metadata !{i32 102, i32 7, metadata !153, null}
!157 = metadata !{i32 102, i32 17, metadata !153, null}
!158 = metadata !{i32 103, i32 4, metadata !153, null}
!159 = metadata !{i32 104, i32 4, metadata !151, null}
!160 = metadata !{i32 105, i32 4, metadata !151, null}
!161 = metadata !{i32 106, i32 4, metadata !151, null}
!162 = metadata !{i32 107, i32 4, metadata !151, null}
!163 = metadata !{i32 108, i32 4, metadata !151, null}
!164 = metadata !{i32 109, i32 1, metadata !151, null}
!165 = metadata !{i32 721153, metadata !34, metadata !"stream", metadata !6, i32 16777280, metadata !84, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!166 = metadata !{i32 64, i32 31, metadata !34, null}
!167 = metadata !{i32 67, i32 4, metadata !168, null}
!168 = metadata !{i32 720907, metadata !34, i32 65, i32 1, metadata !6, i32 388} ; [ DW_TAG_lexical_block ]
!169 = metadata !{i32 67, i32 33, metadata !168, null}
!170 = metadata !{i32 69, i32 4, metadata !168, null}
!171 = metadata !{i32 70, i32 4, metadata !168, null}
!172 = metadata !{i32 72, i32 4, metadata !168, null}
!173 = metadata !{i32 73, i32 4, metadata !168, null}
!174 = metadata !{i32 75, i32 11, metadata !168, null}
!175 = metadata !{i32 76, i32 1, metadata !168, null}
!176 = metadata !{i32 721153, metadata !17, metadata !"ptr", metadata !6, i32 16777337, metadata !177, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!177 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !178} ; [ DW_TAG_pointer_type ]
!178 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, null} ; [ DW_TAG_const_type ]
!179 = metadata !{i32 121, i32 31, metadata !17, null}
!180 = metadata !{i32 721153, metadata !17, metadata !"size", metadata !6, i32 33554553, metadata !20, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!181 = metadata !{i32 121, i32 43, metadata !17, null}
!182 = metadata !{i32 721153, metadata !17, metadata !"n", metadata !6, i32 50331769, metadata !20, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!183 = metadata !{i32 121, i32 56, metadata !17, null}
!184 = metadata !{i32 721153, metadata !17, metadata !"stream", metadata !6, i32 67108985, metadata !84, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!185 = metadata !{i32 121, i32 65, metadata !17, null}
!186 = metadata !{i32 721152, metadata !187, metadata !"result", metadata !6, i32 123, metadata !20, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!187 = metadata !{i32 720907, metadata !17, i32 122, i32 1, metadata !6, i32 371} ; [ DW_TAG_lexical_block ]
!188 = metadata !{i32 123, i32 11, metadata !187, null}
!189 = metadata !{i32 123, i32 21, metadata !187, null}
!190 = metadata !{i32 721152, metadata !187, metadata !"bytesOver", metadata !6, i32 124, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!191 = metadata !{i32 124, i32 8, metadata !187, null}
!192 = metadata !{i32 124, i32 21, metadata !187, null}
!193 = metadata !{i32 721152, metadata !187, metadata !"out", metadata !6, i32 125, metadata !69, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!194 = metadata !{i32 125, i32 19, metadata !187, null}
!195 = metadata !{i32 125, i32 45, metadata !187, null}
!196 = metadata !{i32 139, i32 4, metadata !187, null}
!197 = metadata !{i32 140, i32 14, metadata !187, null}
!198 = metadata !{i32 142, i32 4, metadata !187, null}
!199 = metadata !{i32 144, i32 7, metadata !200, null}
!200 = metadata !{i32 720907, metadata !187, i32 143, i32 4, metadata !6, i32 372} ; [ DW_TAG_lexical_block ]
!201 = metadata !{i32 145, i32 7, metadata !200, null}
!202 = metadata !{i32 146, i32 7, metadata !200, null}
!203 = metadata !{i32 147, i32 7, metadata !200, null}
!204 = metadata !{i32 148, i32 7, metadata !200, null}
!205 = metadata !{i32 150, i32 14, metadata !206, null}
!206 = metadata !{i32 720907, metadata !200, i32 149, i32 7, metadata !6, i32 373} ; [ DW_TAG_lexical_block ]
!207 = metadata !{i32 150, i32 34, metadata !206, null}
!208 = metadata !{i32 151, i32 10, metadata !206, null}
!209 = metadata !{i32 152, i32 7, metadata !206, null}
!210 = metadata !{i32 153, i32 7, metadata !200, null}
!211 = metadata !{i32 155, i32 10, metadata !212, null}
!212 = metadata !{i32 720907, metadata !200, i32 154, i32 7, metadata !6, i32 374} ; [ DW_TAG_lexical_block ]
!213 = metadata !{i32 156, i32 10, metadata !212, null}
!214 = metadata !{i32 157, i32 7, metadata !212, null}
!215 = metadata !{i32 158, i32 4, metadata !200, null}
!216 = metadata !{i32 159, i32 4, metadata !187, null}
!217 = metadata !{i32 160, i32 1, metadata !187, null}
!218 = metadata !{i32 721153, metadata !25, metadata !"c", metadata !6, i32 16777378, metadata !14, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!219 = metadata !{i32 162, i32 19, metadata !25, null}
!220 = metadata !{i32 721153, metadata !25, metadata !"stream", metadata !6, i32 33554594, metadata !84, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!221 = metadata !{i32 162, i32 28, metadata !25, null}
!222 = metadata !{i32 164, i32 4, metadata !223, null}
!223 = metadata !{i32 720907, metadata !25, i32 163, i32 1, metadata !6, i32 375} ; [ DW_TAG_lexical_block ]
!224 = metadata !{i32 165, i32 14, metadata !223, null}
!225 = metadata !{i32 167, i32 4, metadata !223, null}
!226 = metadata !{i32 168, i32 4, metadata !223, null}
!227 = metadata !{i32 169, i32 4, metadata !223, null}
!228 = metadata !{i32 171, i32 11, metadata !229, null}
!229 = metadata !{i32 720907, metadata !223, i32 170, i32 4, metadata !6, i32 376} ; [ DW_TAG_lexical_block ]
!230 = metadata !{i32 171, i32 31, metadata !229, null}
!231 = metadata !{i32 172, i32 7, metadata !229, null}
!232 = metadata !{i32 173, i32 4, metadata !229, null}
!233 = metadata !{i32 174, i32 4, metadata !223, null}
!234 = metadata !{i32 175, i32 1, metadata !223, null}
!235 = metadata !{i32 721153, metadata !28, metadata !"ptr", metadata !6, i32 16777455, metadata !123, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!236 = metadata !{i32 239, i32 24, metadata !28, null}
!237 = metadata !{i32 721153, metadata !28, metadata !"size", metadata !6, i32 33554671, metadata !20, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!238 = metadata !{i32 239, i32 36, metadata !28, null}
!239 = metadata !{i32 721153, metadata !28, metadata !"n", metadata !6, i32 50331887, metadata !20, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!240 = metadata !{i32 239, i32 49, metadata !28, null}
!241 = metadata !{i32 721153, metadata !28, metadata !"stream", metadata !6, i32 67109103, metadata !84, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!242 = metadata !{i32 239, i32 58, metadata !28, null}
!243 = metadata !{i32 721152, metadata !244, metadata !"result", metadata !6, i32 241, metadata !20, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!244 = metadata !{i32 720907, metadata !28, i32 240, i32 1, metadata !6, i32 377} ; [ DW_TAG_lexical_block ]
!245 = metadata !{i32 241, i32 11, metadata !244, null}
!246 = metadata !{i32 241, i32 21, metadata !244, null}
!247 = metadata !{i32 721152, metadata !244, metadata !"bytesOver", metadata !6, i32 242, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!248 = metadata !{i32 242, i32 8, metadata !244, null}
!249 = metadata !{i32 242, i32 21, metadata !244, null}
!250 = metadata !{i32 721152, metadata !244, metadata !"out", metadata !6, i32 243, metadata !69, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!251 = metadata !{i32 243, i32 19, metadata !244, null}
!252 = metadata !{i32 243, i32 28, metadata !244, null}
!253 = metadata !{i32 245, i32 4, metadata !244, null}
!254 = metadata !{i32 246, i32 14, metadata !244, null}
!255 = metadata !{i32 248, i32 4, metadata !244, null}
!256 = metadata !{i32 251, i32 7, metadata !257, null}
!257 = metadata !{i32 720907, metadata !244, i32 249, i32 4, metadata !6, i32 378} ; [ DW_TAG_lexical_block ]
!258 = metadata !{i32 253, i32 10, metadata !259, null}
!259 = metadata !{i32 720907, metadata !257, i32 252, i32 7, metadata !6, i32 379} ; [ DW_TAG_lexical_block ]
!260 = metadata !{i32 254, i32 10, metadata !259, null}
!261 = metadata !{i32 255, i32 10, metadata !259, null}
!262 = metadata !{i32 256, i32 10, metadata !259, null}
!263 = metadata !{i32 258, i32 10, metadata !259, null}
!264 = metadata !{i32 259, i32 10, metadata !259, null}
!265 = metadata !{i32 260, i32 7, metadata !259, null}
!266 = metadata !{i32 263, i32 10, metadata !267, null}
!267 = metadata !{i32 720907, metadata !257, i32 262, i32 7, metadata !6, i32 380} ; [ DW_TAG_lexical_block ]
!268 = metadata !{i32 264, i32 10, metadata !267, null}
!269 = metadata !{i32 265, i32 10, metadata !267, null}
!270 = metadata !{i32 266, i32 10, metadata !267, null}
!271 = metadata !{i32 270, i32 7, metadata !257, null}
!272 = metadata !{i32 721152, metadata !273, metadata !"l", metadata !6, i32 272, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!273 = metadata !{i32 720907, metadata !257, i32 271, i32 7, metadata !6, i32 381} ; [ DW_TAG_lexical_block ]
!274 = metadata !{i32 272, i32 14, metadata !273, null}
!275 = metadata !{i32 721152, metadata !273, metadata !"inBuf", metadata !6, i32 273, metadata !46, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!276 = metadata !{i32 273, i32 15, metadata !273, null}
!277 = metadata !{i32 275, i32 10, metadata !273, null}
!278 = metadata !{i32 277, i32 10, metadata !273, null}
!279 = metadata !{i32 277, i32 20, metadata !273, null}
!280 = metadata !{i32 278, i32 13, metadata !273, null}
!281 = metadata !{i32 278, i32 27, metadata !273, null}
!282 = metadata !{i32 280, i32 10, metadata !273, null}
!283 = metadata !{i32 281, i32 10, metadata !273, null}
!284 = metadata !{i32 282, i32 10, metadata !273, null}
!285 = metadata !{i32 282, i32 30, metadata !273, null}
!286 = metadata !{i32 284, i32 14, metadata !273, null}
!287 = metadata !{i32 285, i32 10, metadata !273, null}
!288 = metadata !{i32 285, i32 41, metadata !289, null}
!289 = metadata !{i32 720907, metadata !273, i32 285, i32 40, metadata !6, i32 382} ; [ DW_TAG_lexical_block ]
!290 = metadata !{i32 285, i32 46, metadata !289, null}
!291 = metadata !{i32 285, i32 62, metadata !289, null}
!292 = metadata !{i32 287, i32 17, metadata !273, null}
!293 = metadata !{i32 288, i32 10, metadata !273, null}
!294 = metadata !{i32 289, i32 10, metadata !273, null}
!295 = metadata !{i32 290, i32 7, metadata !273, null}
!296 = metadata !{i32 291, i32 4, metadata !257, null}
!297 = metadata !{i32 305, i32 4, metadata !244, null}
!298 = metadata !{i32 306, i32 1, metadata !244, null}
!299 = metadata !{i32 721153, metadata !31, metadata !"inbuf", metadata !6, i32 16777396, metadata !73, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!300 = metadata !{i32 180, i32 31, metadata !31, null}
!301 = metadata !{i32 721153, metadata !31, metadata !"outbuf", metadata !6, i32 33554612, metadata !69, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!302 = metadata !{i32 180, i32 53, metadata !31, null}
!303 = metadata !{i32 721153, metadata !31, metadata !"outlength", metadata !6, i32 50331828, metadata !304, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!304 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !14} ; [ DW_TAG_pointer_type ]
!305 = metadata !{i32 180, i32 66, metadata !31, null}
!306 = metadata !{i32 721152, metadata !307, metadata !"bp", metadata !6, i32 182, metadata !69, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!307 = metadata !{i32 720907, metadata !31, i32 181, i32 1, metadata !6, i32 383} ; [ DW_TAG_lexical_block ]
!308 = metadata !{i32 182, i32 19, metadata !307, null}
!309 = metadata !{i32 721152, metadata !307, metadata !"length", metadata !6, i32 183, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!310 = metadata !{i32 183, i32 8, metadata !307, null}
!311 = metadata !{i32 721152, metadata !307, metadata !"c1", metadata !6, i32 184, metadata !22, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!312 = metadata !{i32 184, i32 17, metadata !307, null}
!313 = metadata !{i32 721152, metadata !307, metadata !"c2", metadata !6, i32 184, metadata !22, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!314 = metadata !{i32 184, i32 20, metadata !307, null}
!315 = metadata !{i32 721152, metadata !307, metadata !"c3", metadata !6, i32 184, metadata !22, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!316 = metadata !{i32 184, i32 23, metadata !307, null}
!317 = metadata !{i32 721152, metadata !307, metadata !"c4", metadata !6, i32 184, metadata !22, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!318 = metadata !{i32 184, i32 26, metadata !307, null}
!319 = metadata !{i32 721152, metadata !307, metadata !"hit_padding", metadata !6, i32 185, metadata !14, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!320 = metadata !{i32 185, i32 8, metadata !307, null}
!321 = metadata !{i32 185, i32 23, metadata !307, null}
!322 = metadata !{i32 187, i32 4, metadata !307, null}
!323 = metadata !{i32 188, i32 4, metadata !307, null}
!324 = metadata !{i32 194, i32 4, metadata !307, null}
!325 = metadata !{i32 197, i32 7, metadata !326, null}
!326 = metadata !{i32 720907, metadata !307, i32 195, i32 4, metadata !6, i32 384} ; [ DW_TAG_lexical_block ]
!327 = metadata !{i32 199, i32 10, metadata !328, null}
!328 = metadata !{i32 720907, metadata !326, i32 198, i32 7, metadata !6, i32 385} ; [ DW_TAG_lexical_block ]
!329 = metadata !{i32 200, i32 10, metadata !328, null}
!330 = metadata !{i32 200, i32 30, metadata !328, null}
!331 = metadata !{i32 202, i32 13, metadata !332, null}
!332 = metadata !{i32 720907, metadata !328, i32 201, i32 10, metadata !6, i32 386} ; [ DW_TAG_lexical_block ]
!333 = metadata !{i32 203, i32 13, metadata !332, null}
!334 = metadata !{i32 204, i32 10, metadata !332, null}
!335 = metadata !{i32 206, i32 13, metadata !328, null}
!336 = metadata !{i32 207, i32 10, metadata !328, null}
!337 = metadata !{i32 208, i32 7, metadata !328, null}
!338 = metadata !{i32 210, i32 10, metadata !326, null}
!339 = metadata !{i32 212, i32 7, metadata !326, null}
!340 = metadata !{i32 217, i32 10, metadata !341, null}
!341 = metadata !{i32 720907, metadata !326, i32 216, i32 7, metadata !6, i32 387} ; [ DW_TAG_lexical_block ]
!342 = metadata !{i32 218, i32 10, metadata !341, null}
!343 = metadata !{i32 220, i32 7, metadata !326, null}
!344 = metadata !{i32 221, i32 7, metadata !326, null}
!345 = metadata !{i32 222, i32 7, metadata !326, null}
!346 = metadata !{i32 223, i32 7, metadata !326, null}
!347 = metadata !{i32 224, i32 4, metadata !326, null}
!348 = metadata !{i32 226, i32 4, metadata !307, null}
!349 = metadata !{i32 227, i32 4, metadata !307, null}
!350 = metadata !{i32 721153, metadata !40, metadata !"p", metadata !6, i32 16777296, metadata !69, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!351 = metadata !{i32 80, i32 34, metadata !40, null}
!352 = metadata !{i32 721153, metadata !40, metadata !"buffer", metadata !6, i32 33554512, metadata !73, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!353 = metadata !{i32 80, i32 45, metadata !40, null}
!354 = metadata !{i32 721153, metadata !40, metadata !"count", metadata !6, i32 50331728, metadata !14, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!355 = metadata !{i32 80, i32 60, metadata !40, null}
!356 = metadata !{i32 82, i32 4, metadata !357, null}
!357 = metadata !{i32 720907, metadata !40, i32 81, i32 1, metadata !6, i32 391} ; [ DW_TAG_lexical_block ]
!358 = metadata !{i32 84, i32 7, metadata !359, null}
!359 = metadata !{i32 720907, metadata !357, i32 83, i32 4, metadata !6, i32 392} ; [ DW_TAG_lexical_block ]
!360 = metadata !{i32 85, i32 7, metadata !359, null}
!361 = metadata !{i32 86, i32 4, metadata !359, null}
!362 = metadata !{i32 87, i32 4, metadata !357, null}
!363 = metadata !{i32 88, i32 4, metadata !357, null}
!364 = metadata !{i32 89, i32 4, metadata !357, null}
!365 = metadata !{i32 91, i32 7, metadata !366, null}
!366 = metadata !{i32 720907, metadata !357, i32 90, i32 4, metadata !6, i32 393} ; [ DW_TAG_lexical_block ]
!367 = metadata !{i32 92, i32 7, metadata !366, null}
!368 = metadata !{i32 92, i32 21, metadata !366, null}
!369 = metadata !{i32 93, i32 4, metadata !366, null}
!370 = metadata !{i32 94, i32 1, metadata !357, null}
