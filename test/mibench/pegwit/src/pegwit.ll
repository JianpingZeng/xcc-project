; ModuleID = 'pegwit.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct._IO_FILE = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker*, %struct._IO_FILE*, i32, i32, i32, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i32, i32, [40 x i8] }
%struct._IO_marker = type { %struct._IO_marker*, %struct._IO_FILE*, i32 }
%struct.timeval = type { i32, i32 }
%struct.hash_context = type { [5 x i32], [2 x i32], [64 x i8] }
%struct.prng = type { i32, [17 x i32] }
%struct.squareCtsContext = type { [9 x [4 x i32]], [9 x [4 x i32]], [16 x i8] }
%struct.cpPair = type { [19 x i16], [19 x i16] }

@manual = constant [374 x i8] c"Pegwit v8.7\0AUsage (init/encrypt/decrypt/sign/verify) :\0A-i <secret-key >public-key\0A-e public-key plain cipher <random-junk\0A-d cipher plain <secret-key\0A-s plain <secret-key >signature\0A-v public-key plain <signature\0A-E plain cipher <key\0A-D cipher plain <key\0A-S text <secret-key >clearsigned-text\0A-V public-key clearsigned-text >text\0A-f[operation] [type pegwit -f for details]\0A\00", align 1
@filterManual = constant [284 x i8] c"Pegwit [filter sub-mode]\0AUsage (encrypt/decrypt/sign/verify) :\0A-fe public-key random-junk <plain >ascii-cipher\0A-fd secret-key <ascii-cipher >plain\0A-fE key <plain >ascii-cipher\0A-fD key <ascii-cipher >plain\0A-fS secret-key <text >clearsigned-text\0A-fV public-key <clearsigned-text >text\0A\00", align 1
@pubkey_magic = constant [23 x i8] c"pegwit v8 public key =\00", align 1
@err_output = constant [41 x i8] c"Pegwit, error writing output, disk full?\00", align 1
@err_open_failed = constant [32 x i8] c"Pegwit, error : failed to open \00", align 1
@err_bad_public_key = constant [45 x i8] c"Pegwit, error : public key must start with \22\00", align 1
@err_signature = constant [29 x i8] c"signature did not verify\07\07\07\0A\00", align 1
@err_decrypt = constant [22 x i8] c"decryption failed\07\07\07\0A\00", align 1
@begin_clearsign = constant [5 x i8] c"###\0A\00", align 1
@end_clearsign = constant [31 x i8] c"### end pegwit v8 signed text\0A\00", align 1
@end_ckarmour = constant [40 x i8] c"### end pegwit v8.7 -fE encrypted text\0A\00", align 1
@end_pkarmour = constant [40 x i8] c"### end pegwit v8.7 -fe encrypted text\0A\00", align 1
@escape = constant [4 x i8] c"## \00", align 1
@warn_long_line = constant [70 x i8] c"Very long line - > 8k bytes.  Binary file?\0AClearsignature dubious\07\07\07\0A\00", align 1
@warn_control_chars = constant [77 x i8] c"Large number of control characters.  Binary file?\0AClearsignature dubious\07\07\07\0A\00", align 1
@err_clearsig_header_not_found = constant [42 x i8] c"Clearsignature header \22###\22 not found\07\07\07\0A\00", align 1
@.str = private unnamed_addr constant [5 x i8] c"from\00", align 1
@stderr = external global %struct._IO_FILE*
@hex = constant [16 x i8] c"0123456789abcdef", align 1
@stdout = external global %struct._IO_FILE*
@stdin = external global %struct._IO_FILE*
@.str1 = private unnamed_addr constant [2 x i8] c"\0A\00", align 1
@.str2 = private unnamed_addr constant [2 x i8] c":\00", align 1
@main.openForRead = private unnamed_addr constant [3 x i8] c"rb\00", align 1
@main.openForWrite = private unnamed_addr constant [3 x i8] c"wb\00", align 1
@main.openKey = private unnamed_addr constant [3 x i8] c"rb\00", align 1
@ss_time_start = common global %struct.timeval zeroinitializer, align 4
@ss_time_end = common global %struct.timeval zeroinitializer, align 4

define arm_aapcscc void @hash_process_file(%struct.hash_context* %c, %struct._IO_FILE* %f_inp, i32 %barrel) nounwind uwtable {
entry:
  %c.addr = alloca %struct.hash_context*, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %barrel.addr = alloca i32, align 4
  %n = alloca i32, align 4
  %buffer = alloca [16384 x i8], align 1
  %j = alloca i32, align 4
  store %struct.hash_context* %c, %struct.hash_context** %c.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.hash_context** %c.addr}, metadata !253), !dbg !270
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !271), !dbg !272
  store i32 %barrel, i32* %barrel.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %barrel.addr}, metadata !273), !dbg !274
  call void @llvm.dbg.declare(metadata !{i32* %n}, metadata !275), !dbg !277
  call void @llvm.dbg.declare(metadata !{[16384 x i8]* %buffer}, metadata !278), !dbg !282
  br label %while.body, !dbg !283

while.body:                                       ; preds = %entry, %if.end5
  %arraydecay = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !284
  %0 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !284
  %call = call arm_aapcscc  i32 @fread(i8* %arraydecay, i32 1, i32 16384, %struct._IO_FILE* %0), !dbg !284
  store i32 %call, i32* %n, align 4, !dbg !284
  %1 = load i32* %n, align 4, !dbg !286
  %cmp = icmp eq i32 %1, 0, !dbg !286
  br i1 %cmp, label %if.then, label %if.end, !dbg !286

if.then:                                          ; preds = %while.body
  br label %while.end, !dbg !287

if.end:                                           ; preds = %while.body
  call void @llvm.dbg.declare(metadata !{i32* %j}, metadata !288), !dbg !290
  store i32 0, i32* %j, align 4, !dbg !291
  br label %for.cond, !dbg !291

for.cond:                                         ; preds = %for.inc, %if.end
  %2 = load i32* %j, align 4, !dbg !291
  %3 = load i32* %barrel.addr, align 4, !dbg !291
  %cmp1 = icmp ult i32 %2, %3, !dbg !291
  br i1 %cmp1, label %for.body, label %for.end, !dbg !291

for.body:                                         ; preds = %for.cond
  %4 = load %struct.hash_context** %c.addr, align 4, !dbg !293
  %5 = load i32* %j, align 4, !dbg !293
  %add.ptr = getelementptr inbounds %struct.hash_context* %4, i32 %5, !dbg !293
  %arraydecay2 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !293
  %6 = load i32* %n, align 4, !dbg !293
  call arm_aapcscc  void @hash_process(%struct.hash_context* %add.ptr, i8* %arraydecay2, i32 %6), !dbg !293
  br label %for.inc, !dbg !295

for.inc:                                          ; preds = %for.body
  %7 = load i32* %j, align 4, !dbg !296
  %add = add i32 %7, 1, !dbg !296
  store i32 %add, i32* %j, align 4, !dbg !296
  br label %for.cond, !dbg !296

for.end:                                          ; preds = %for.cond
  %8 = load i32* %n, align 4, !dbg !297
  %cmp3 = icmp ult i32 %8, 16384, !dbg !297
  br i1 %cmp3, label %if.then4, label %if.end5, !dbg !297

if.then4:                                         ; preds = %for.end
  br label %while.end, !dbg !298

if.end5:                                          ; preds = %for.end
  br label %while.body, !dbg !299

while.end:                                        ; preds = %if.then4, %if.then
  %arraydecay6 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !300
  call void @llvm.memset.p0i8.i32(i8* %arraydecay6, i8 0, i32 0, i32 1, i1 false), !dbg !300
  %9 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !301
  %call7 = call arm_aapcscc  i32 @fseek(%struct._IO_FILE* %9, i32 0, i32 0), !dbg !301
  ret void, !dbg !302
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc i32 @fread(i8*, i32, i32, %struct._IO_FILE*)

declare arm_aapcscc void @hash_process(%struct.hash_context*, i8*, i32)

declare void @llvm.memset.p0i8.i32(i8* nocapture, i8, i32, i32, i1) nounwind

declare arm_aapcscc i32 @fseek(%struct._IO_FILE*, i32, i32)

define arm_aapcscc i32 @downcase(i8 signext %c) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %c.addr = alloca i8, align 1
  store i8 %c, i8* %c.addr, align 1
  call void @llvm.dbg.declare(metadata !{i8* %c.addr}, metadata !303), !dbg !304
  %0 = load i8* %c.addr, align 1, !dbg !305
  %conv = sext i8 %0 to i32, !dbg !305
  %and = and i32 %conv, -128, !dbg !305
  %cmp = icmp eq i32 %and, 0, !dbg !305
  br i1 %cmp, label %if.then, label %if.end8, !dbg !305

if.then:                                          ; preds = %entry
  %1 = load i8* %c.addr, align 1, !dbg !307
  %conv2 = sext i8 %1 to i32, !dbg !307
  %call = call arm_aapcscc  i16** @__ctype_b_loc() nounwind readnone, !dbg !308
  %2 = load i16** %call, !dbg !308
  %arrayidx = getelementptr inbounds i16* %2, i32 %conv2, !dbg !308
  %3 = load i16* %arrayidx, !dbg !308
  %conv3 = zext i16 %3 to i32, !dbg !308
  %and4 = and i32 %conv3, 256, !dbg !308
  %tobool = icmp ne i32 %and4, 0, !dbg !308
  br i1 %tobool, label %if.then5, label %if.end, !dbg !308

if.then5:                                         ; preds = %if.then
  %4 = load i8* %c.addr, align 1, !dbg !309
  %conv6 = sext i8 %4 to i32, !dbg !309
  %call7 = call arm_aapcscc  i32 @tolower(i32 %conv6) nounwind, !dbg !309
  store i32 %call7, i32* %retval, !dbg !309
  br label %return, !dbg !309

if.end:                                           ; preds = %if.then
  br label %if.end8, !dbg !309

if.end8:                                          ; preds = %if.end, %entry
  %5 = load i8* %c.addr, align 1, !dbg !310
  %conv9 = sext i8 %5 to i32, !dbg !310
  store i32 %conv9, i32* %retval, !dbg !310
  br label %return, !dbg !310

return:                                           ; preds = %if.end8, %if.then5
  %6 = load i32* %retval, !dbg !311
  ret i32 %6, !dbg !311
}

declare arm_aapcscc i16** @__ctype_b_loc() nounwind readnone

declare arm_aapcscc i32 @tolower(i32) nounwind

define arm_aapcscc i32 @case_blind_compare(i8* %a, i8* %b) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %a.addr = alloca i8*, align 4
  %b.addr = alloca i8*, align 4
  store i8* %a, i8** %a.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %a.addr}, metadata !312), !dbg !314
  store i8* %b, i8** %b.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %b.addr}, metadata !315), !dbg !316
  br label %while.cond, !dbg !317

while.cond:                                       ; preds = %if.end10, %entry
  %0 = load i8** %a.addr, align 4, !dbg !317
  %1 = load i8* %0, !dbg !317
  %conv = sext i8 %1 to i32, !dbg !317
  %tobool = icmp ne i32 %conv, 0, !dbg !317
  br i1 %tobool, label %land.rhs, label %land.end, !dbg !317

land.rhs:                                         ; preds = %while.cond
  %2 = load i8** %b.addr, align 4, !dbg !317
  %3 = load i8* %2, !dbg !317
  %conv1 = sext i8 %3 to i32, !dbg !317
  %tobool2 = icmp ne i32 %conv1, 0, !dbg !317
  br label %land.end

land.end:                                         ; preds = %land.rhs, %while.cond
  %4 = phi i1 [ false, %while.cond ], [ %tobool2, %land.rhs ]
  br i1 %4, label %while.body, label %while.end

while.body:                                       ; preds = %land.end
  %5 = load i8** %a.addr, align 4, !dbg !319
  %6 = load i8* %5, !dbg !319
  %call = call arm_aapcscc  i32 @downcase(i8 signext %6), !dbg !319
  %7 = load i8** %b.addr, align 4, !dbg !321
  %8 = load i8* %7, !dbg !321
  %call3 = call arm_aapcscc  i32 @downcase(i8 signext %8), !dbg !321
  %cmp = icmp slt i32 %call, %call3, !dbg !321
  br i1 %cmp, label %if.then, label %if.end, !dbg !321

if.then:                                          ; preds = %while.body
  store i32 -1, i32* %retval, !dbg !322
  br label %return, !dbg !322

if.end:                                           ; preds = %while.body
  %9 = load i8** %a.addr, align 4, !dbg !323
  %10 = load i8* %9, !dbg !323
  %call5 = call arm_aapcscc  i32 @downcase(i8 signext %10), !dbg !323
  %11 = load i8** %b.addr, align 4, !dbg !324
  %12 = load i8* %11, !dbg !324
  %call6 = call arm_aapcscc  i32 @downcase(i8 signext %12), !dbg !324
  %cmp7 = icmp sgt i32 %call5, %call6, !dbg !324
  br i1 %cmp7, label %if.then9, label %if.end10, !dbg !324

if.then9:                                         ; preds = %if.end
  store i32 1, i32* %retval, !dbg !325
  br label %return, !dbg !325

if.end10:                                         ; preds = %if.end
  %13 = load i8** %a.addr, align 4, !dbg !326
  %add.ptr = getelementptr inbounds i8* %13, i32 1, !dbg !326
  store i8* %add.ptr, i8** %a.addr, align 4, !dbg !326
  %14 = load i8** %b.addr, align 4, !dbg !327
  %add.ptr11 = getelementptr inbounds i8* %14, i32 1, !dbg !327
  store i8* %add.ptr11, i8** %b.addr, align 4, !dbg !327
  br label %while.cond, !dbg !328

while.end:                                        ; preds = %land.end
  %15 = load i8** %a.addr, align 4, !dbg !329
  %16 = load i8* %15, !dbg !329
  %tobool12 = icmp ne i8 %16, 0, !dbg !329
  br i1 %tobool12, label %if.then13, label %if.end14, !dbg !329

if.then13:                                        ; preds = %while.end
  store i32 1, i32* %retval, !dbg !330
  br label %return, !dbg !330

if.end14:                                         ; preds = %while.end
  %17 = load i8** %b.addr, align 4, !dbg !331
  %18 = load i8* %17, !dbg !331
  %tobool15 = icmp ne i8 %18, 0, !dbg !331
  br i1 %tobool15, label %if.then16, label %if.end17, !dbg !331

if.then16:                                        ; preds = %if.end14
  store i32 -1, i32* %retval, !dbg !332
  br label %return, !dbg !332

if.end17:                                         ; preds = %if.end14
  store i32 0, i32* %retval, !dbg !333
  br label %return, !dbg !333

return:                                           ; preds = %if.end17, %if.then16, %if.then13, %if.then9, %if.then
  %19 = load i32* %retval, !dbg !334
  ret i32 %19, !dbg !334
}

define arm_aapcscc void @hash_process_ascii(%struct.hash_context* %c, %struct._IO_FILE* %f_inp, %struct._IO_FILE* %f_out, i32 %barrel, i32 %write) nounwind uwtable {
entry:
  %c.addr = alloca %struct.hash_context*, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %f_out.addr = alloca %struct._IO_FILE*, align 4
  %barrel.addr = alloca i32, align 4
  %write.addr = alloca i32, align 4
  %n = alloca i32, align 4
  %buffer = alloca [16384 x i8], align 1
  %begin = alloca i8*, align 4
  %bytes = alloca i32, align 4
  %control = alloca i32, align 4
  %i = alloca i32, align 4
  store %struct.hash_context* %c, %struct.hash_context** %c.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.hash_context** %c.addr}, metadata !335), !dbg !336
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !337), !dbg !338
  store %struct._IO_FILE* %f_out, %struct._IO_FILE** %f_out.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_out.addr}, metadata !339), !dbg !340
  store i32 %barrel, i32* %barrel.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %barrel.addr}, metadata !341), !dbg !342
  store i32 %write, i32* %write.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %write.addr}, metadata !343), !dbg !344
  call void @llvm.dbg.declare(metadata !{i32* %n}, metadata !345), !dbg !347
  call void @llvm.dbg.declare(metadata !{[16384 x i8]* %buffer}, metadata !348), !dbg !349
  call void @llvm.dbg.declare(metadata !{i8** %begin}, metadata !350), !dbg !352
  call void @llvm.dbg.declare(metadata !{i32* %bytes}, metadata !353), !dbg !354
  store i32 0, i32* %bytes, align 4, !dbg !355
  call void @llvm.dbg.declare(metadata !{i32* %control}, metadata !356), !dbg !357
  store i32 0, i32* %control, align 4, !dbg !355
  br label %while.body, !dbg !358

while.body:                                       ; preds = %entry, %for.end68
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !359), !dbg !361
  %arraydecay = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !362
  %0 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !362
  %call = call arm_aapcscc  i8* @fgets(i8* %arraydecay, i32 16384, %struct._IO_FILE* %0), !dbg !362
  %1 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !363
  %call1 = call arm_aapcscc  i32 @feof(%struct._IO_FILE* %1) nounwind, !dbg !363
  %tobool = icmp ne i32 %call1, 0, !dbg !363
  br i1 %tobool, label %if.then, label %if.end, !dbg !363

if.then:                                          ; preds = %while.body
  br label %while.end, !dbg !364

if.end:                                           ; preds = %while.body
  %arraydecay2 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !365
  %call3 = call arm_aapcscc  i32 @strlen(i8* %arraydecay2) nounwind readonly, !dbg !365
  store i32 %call3, i32* %n, align 4, !dbg !365
  %arraydecay4 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !366
  store i8* %arraydecay4, i8** %begin, align 4, !dbg !366
  %2 = load i32* %n, align 4, !dbg !367
  %cmp = icmp ugt i32 %2, 8192, !dbg !367
  br i1 %cmp, label %if.then5, label %if.end7, !dbg !367

if.then5:                                         ; preds = %if.end
  %3 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !368
  %call6 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([70 x i8]* @warn_long_line, i32 0, i32 0), %struct._IO_FILE* %3), !dbg !368
  br label %if.end7, !dbg !370

if.end7:                                          ; preds = %if.then5, %if.end
  %4 = load i32* %n, align 4, !dbg !371
  %5 = load i32* %bytes, align 4, !dbg !371
  %add = add i32 %5, %4, !dbg !371
  store i32 %add, i32* %bytes, align 4, !dbg !371
  store i32 0, i32* %i, align 4, !dbg !372
  br label %for.cond, !dbg !372

for.cond:                                         ; preds = %for.inc, %if.end7
  %6 = load i32* %i, align 4, !dbg !372
  %7 = load i32* %n, align 4, !dbg !372
  %cmp8 = icmp ult i32 %6, %7, !dbg !372
  br i1 %cmp8, label %for.body, label %for.end, !dbg !372

for.body:                                         ; preds = %for.cond
  %8 = load i32* %i, align 4, !dbg !374
  %arrayidx = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 %8, !dbg !374
  %9 = load i8* %arrayidx, align 1, !dbg !374
  %conv = zext i8 %9 to i32, !dbg !374
  %cmp9 = icmp sge i32 %conv, 127, !dbg !374
  br i1 %cmp9, label %if.then11, label %if.end12, !dbg !374

if.then11:                                        ; preds = %for.body
  %10 = load i32* %control, align 4, !dbg !376
  %inc = add i32 %10, 1, !dbg !376
  store i32 %inc, i32* %control, align 4, !dbg !376
  br label %if.end12, !dbg !376

if.end12:                                         ; preds = %if.then11, %for.body
  %11 = load i32* %i, align 4, !dbg !377
  %arrayidx13 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 %11, !dbg !377
  %12 = load i8* %arrayidx13, align 1, !dbg !377
  %conv14 = zext i8 %12 to i32, !dbg !377
  %cmp15 = icmp slt i32 %conv14, 32, !dbg !377
  br i1 %cmp15, label %land.lhs.true, label %if.end33, !dbg !377

land.lhs.true:                                    ; preds = %if.end12
  %13 = load i32* %i, align 4, !dbg !377
  %arrayidx17 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 %13, !dbg !377
  %14 = load i8* %arrayidx17, align 1, !dbg !377
  %conv18 = zext i8 %14 to i32, !dbg !377
  %cmp19 = icmp ne i32 %conv18, 10, !dbg !377
  br i1 %cmp19, label %land.lhs.true21, label %if.end33, !dbg !377

land.lhs.true21:                                  ; preds = %land.lhs.true
  %15 = load i32* %i, align 4, !dbg !377
  %arrayidx22 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 %15, !dbg !377
  %16 = load i8* %arrayidx22, align 1, !dbg !377
  %conv23 = zext i8 %16 to i32, !dbg !377
  %cmp24 = icmp ne i32 %conv23, 13, !dbg !377
  br i1 %cmp24, label %land.lhs.true26, label %if.end33, !dbg !377

land.lhs.true26:                                  ; preds = %land.lhs.true21
  %17 = load i32* %i, align 4, !dbg !377
  %arrayidx27 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 %17, !dbg !377
  %18 = load i8* %arrayidx27, align 1, !dbg !377
  %conv28 = zext i8 %18 to i32, !dbg !377
  %cmp29 = icmp ne i32 %conv28, 9, !dbg !377
  br i1 %cmp29, label %if.then31, label %if.end33, !dbg !377

if.then31:                                        ; preds = %land.lhs.true26
  %19 = load i32* %control, align 4, !dbg !378
  %inc32 = add i32 %19, 1, !dbg !378
  store i32 %inc32, i32* %control, align 4, !dbg !378
  br label %if.end33, !dbg !378

if.end33:                                         ; preds = %if.then31, %land.lhs.true26, %land.lhs.true21, %land.lhs.true, %if.end12
  br label %for.inc, !dbg !379

for.inc:                                          ; preds = %if.end33
  %20 = load i32* %i, align 4, !dbg !380
  %inc34 = add i32 %20, 1, !dbg !380
  store i32 %inc34, i32* %i, align 4, !dbg !380
  br label %for.cond, !dbg !380

for.end:                                          ; preds = %for.cond
  %21 = load i32* %write.addr, align 4, !dbg !381
  %tobool35 = icmp ne i32 %21, 0, !dbg !381
  br i1 %tobool35, label %if.then36, label %if.else, !dbg !381

if.then36:                                        ; preds = %for.end
  %arraydecay37 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !382
  %call38 = call arm_aapcscc  i32 @strncmp(i8* %arraydecay37, i8* getelementptr inbounds ([4 x i8]* @escape, i32 0, i32 0), i32 2) nounwind readonly, !dbg !382
  %tobool39 = icmp ne i32 %call38, 0, !dbg !382
  br i1 %tobool39, label %lor.lhs.false, label %if.then43, !dbg !382

lor.lhs.false:                                    ; preds = %if.then36
  %arraydecay40 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !384
  %call41 = call arm_aapcscc  i32 @case_blind_compare(i8* %arraydecay40, i8* getelementptr inbounds ([5 x i8]* @.str, i32 0, i32 0)), !dbg !384
  %tobool42 = icmp ne i32 %call41, 0, !dbg !384
  br i1 %tobool42, label %if.end45, label %if.then43, !dbg !384

if.then43:                                        ; preds = %lor.lhs.false, %if.then36
  %22 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !385
  %call44 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([4 x i8]* @escape, i32 0, i32 0), %struct._IO_FILE* %22), !dbg !385
  br label %if.end45, !dbg !387

if.end45:                                         ; preds = %if.then43, %lor.lhs.false
  %arraydecay46 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !388
  %23 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !388
  %call47 = call arm_aapcscc  i32 @fputs(i8* %arraydecay46, %struct._IO_FILE* %23), !dbg !388
  br label %if.end60, !dbg !389

if.else:                                          ; preds = %for.end
  %arraydecay48 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !390
  %call49 = call arm_aapcscc  i32 @strncmp(i8* %arraydecay48, i8* getelementptr inbounds ([4 x i8]* @escape, i32 0, i32 0), i32 3) nounwind readonly, !dbg !390
  %tobool50 = icmp ne i32 %call49, 0, !dbg !390
  br i1 %tobool50, label %if.else52, label %if.then51, !dbg !390

if.then51:                                        ; preds = %if.else
  %24 = load i32* %n, align 4, !dbg !392
  %sub = sub i32 %24, 3, !dbg !392
  store i32 %sub, i32* %n, align 4, !dbg !392
  %25 = load i8** %begin, align 4, !dbg !392
  %add.ptr = getelementptr inbounds i8* %25, i32 3, !dbg !392
  store i8* %add.ptr, i8** %begin, align 4, !dbg !392
  br label %if.end58, !dbg !394

if.else52:                                        ; preds = %if.else
  %arraydecay53 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !395
  %call54 = call arm_aapcscc  i32 @strncmp(i8* %arraydecay53, i8* getelementptr inbounds ([31 x i8]* @end_clearsign, i32 0, i32 0), i32 3) nounwind readonly, !dbg !395
  %tobool55 = icmp ne i32 %call54, 0, !dbg !395
  br i1 %tobool55, label %if.end57, label %if.then56, !dbg !395

if.then56:                                        ; preds = %if.else52
  br label %while.end, !dbg !396

if.end57:                                         ; preds = %if.else52
  br label %if.end58

if.end58:                                         ; preds = %if.end57, %if.then51
  %26 = load i8** %begin, align 4, !dbg !397
  %27 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !397
  %call59 = call arm_aapcscc  i32 @fputs(i8* %26, %struct._IO_FILE* %27), !dbg !397
  br label %if.end60

if.end60:                                         ; preds = %if.end58, %if.end45
  store i32 0, i32* %i, align 4, !dbg !398
  br label %for.cond61, !dbg !398

for.cond61:                                       ; preds = %for.inc66, %if.end60
  %28 = load i32* %i, align 4, !dbg !398
  %29 = load i32* %barrel.addr, align 4, !dbg !398
  %cmp62 = icmp ult i32 %28, %29, !dbg !398
  br i1 %cmp62, label %for.body64, label %for.end68, !dbg !398

for.body64:                                       ; preds = %for.cond61
  %30 = load %struct.hash_context** %c.addr, align 4, !dbg !400
  %31 = load i32* %i, align 4, !dbg !400
  %add.ptr65 = getelementptr inbounds %struct.hash_context* %30, i32 %31, !dbg !400
  %32 = load i8** %begin, align 4, !dbg !400
  %33 = load i32* %n, align 4, !dbg !400
  call arm_aapcscc  void @hash_process(%struct.hash_context* %add.ptr65, i8* %32, i32 %33), !dbg !400
  br label %for.inc66, !dbg !402

for.inc66:                                        ; preds = %for.body64
  %34 = load i32* %i, align 4, !dbg !403
  %inc67 = add i32 %34, 1, !dbg !403
  store i32 %inc67, i32* %i, align 4, !dbg !403
  br label %for.cond61, !dbg !403

for.end68:                                        ; preds = %for.cond61
  br label %while.body, !dbg !404

while.end:                                        ; preds = %if.then56, %if.then
  %35 = load i32* %control, align 4, !dbg !405
  %mul = mul i32 %35, 6, !dbg !405
  %36 = load i32* %bytes, align 4, !dbg !405
  %cmp69 = icmp ugt i32 %mul, %36, !dbg !405
  br i1 %cmp69, label %if.then71, label %if.end73, !dbg !405

if.then71:                                        ; preds = %while.end
  %37 = load %struct._IO_FILE** @stderr, align 4, !dbg !406
  %call72 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([77 x i8]* @warn_control_chars, i32 0, i32 0), %struct._IO_FILE* %37), !dbg !406
  br label %if.end73, !dbg !408

if.end73:                                         ; preds = %if.then71, %while.end
  %arraydecay74 = getelementptr inbounds [16384 x i8]* %buffer, i32 0, i32 0, !dbg !409
  call void @llvm.memset.p0i8.i32(i8* %arraydecay74, i8 0, i32 0, i32 1, i1 false), !dbg !409
  ret void, !dbg !410
}

declare arm_aapcscc i8* @fgets(i8*, i32, %struct._IO_FILE*)

declare arm_aapcscc i32 @feof(%struct._IO_FILE*) nounwind

declare arm_aapcscc i32 @strlen(i8*) nounwind readonly

declare arm_aapcscc i32 @fputs(i8*, %struct._IO_FILE*)

declare arm_aapcscc i32 @strncmp(i8*, i8*, i32) nounwind readonly

define arm_aapcscc void @prng_init(%struct.prng* %p) nounwind uwtable {
entry:
  %p.addr = alloca %struct.prng*, align 4
  store %struct.prng* %p, %struct.prng** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.prng** %p.addr}, metadata !411), !dbg !421
  %0 = load %struct.prng** %p.addr, align 4, !dbg !422
  %1 = bitcast %struct.prng* %0 to i8*, !dbg !422
  call void @llvm.memset.p0i8.i32(i8* %1, i8 0, i32 72, i32 1, i1 false), !dbg !422
  ret void, !dbg !424
}

define arm_aapcscc void @prng_set_secret(%struct.prng* %p, %struct._IO_FILE* %f_key) nounwind uwtable {
entry:
  %p.addr = alloca %struct.prng*, align 4
  %f_key.addr = alloca %struct._IO_FILE*, align 4
  %c = alloca [1 x %struct.hash_context], align 4
  store %struct.prng* %p, %struct.prng** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.prng** %p.addr}, metadata !425), !dbg !426
  store %struct._IO_FILE* %f_key, %struct._IO_FILE** %f_key.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_key.addr}, metadata !427), !dbg !428
  call void @llvm.dbg.declare(metadata !{[1 x %struct.hash_context]* %c}, metadata !429), !dbg !432
  %arraydecay = getelementptr inbounds [1 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !433
  call arm_aapcscc  void @hash_initial(%struct.hash_context* %arraydecay), !dbg !433
  %arraydecay1 = getelementptr inbounds [1 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !434
  %0 = load %struct._IO_FILE** %f_key.addr, align 4, !dbg !434
  call arm_aapcscc  void @hash_process_file(%struct.hash_context* %arraydecay1, %struct._IO_FILE* %0, i32 1), !dbg !434
  %arraydecay2 = getelementptr inbounds [1 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !435
  %1 = load %struct.prng** %p.addr, align 4, !dbg !435
  %seed = getelementptr inbounds %struct.prng* %1, i32 0, i32 1, !dbg !435
  %arraydecay3 = getelementptr inbounds [17 x i32]* %seed, i32 0, i32 0, !dbg !435
  %add.ptr = getelementptr inbounds i32* %arraydecay3, i32 1, !dbg !435
  call arm_aapcscc  void @hash_final(%struct.hash_context* %arraydecay2, i32* %add.ptr), !dbg !435
  %2 = load %struct.prng** %p.addr, align 4, !dbg !436
  %count = getelementptr inbounds %struct.prng* %2, i32 0, i32 0, !dbg !436
  store i32 6, i32* %count, align 4, !dbg !436
  ret void, !dbg !437
}

declare arm_aapcscc void @hash_initial(%struct.hash_context*)

declare arm_aapcscc void @hash_final(%struct.hash_context*, i32*)

define arm_aapcscc void @prng_init_mac(%struct.hash_context* %c) nounwind uwtable {
entry:
  %c.addr = alloca %struct.hash_context*, align 4
  %b = alloca i8, align 1
  store %struct.hash_context* %c, %struct.hash_context** %c.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.hash_context** %c.addr}, metadata !438), !dbg !439
  call void @llvm.dbg.declare(metadata !{i8* %b}, metadata !440), !dbg !442
  store i8 0, i8* %b, align 1, !dbg !443
  br label %for.cond, !dbg !443

for.cond:                                         ; preds = %for.inc, %entry
  %0 = load i8* %b, align 1, !dbg !443
  %conv = zext i8 %0 to i32, !dbg !443
  %cmp = icmp slt i32 %conv, 2, !dbg !443
  br i1 %cmp, label %for.body, label %for.end, !dbg !443

for.body:                                         ; preds = %for.cond
  %1 = load %struct.hash_context** %c.addr, align 4, !dbg !445
  %2 = load i8* %b, align 1, !dbg !445
  %conv2 = zext i8 %2 to i32, !dbg !445
  %add.ptr = getelementptr inbounds %struct.hash_context* %1, i32 %conv2, !dbg !445
  call arm_aapcscc  void @hash_initial(%struct.hash_context* %add.ptr), !dbg !445
  %3 = load %struct.hash_context** %c.addr, align 4, !dbg !447
  %add.ptr3 = getelementptr inbounds %struct.hash_context* %3, i32 1, !dbg !447
  call arm_aapcscc  void @hash_process(%struct.hash_context* %add.ptr3, i8* %b, i32 1), !dbg !447
  br label %for.inc, !dbg !448

for.inc:                                          ; preds = %for.body
  %4 = load i8* %b, align 1, !dbg !449
  %inc = add i8 %4, 1, !dbg !449
  store i8 %inc, i8* %b, align 1, !dbg !449
  br label %for.cond, !dbg !449

for.end:                                          ; preds = %for.cond
  ret void, !dbg !450
}

define arm_aapcscc void @prng_set_mac(%struct.prng* %p, %struct._IO_FILE* %f_inp, i32 %barrel) nounwind uwtable {
entry:
  %p.addr = alloca %struct.prng*, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %barrel.addr = alloca i32, align 4
  %b = alloca i8, align 1
  %c = alloca [2 x %struct.hash_context], align 4
  store %struct.prng* %p, %struct.prng** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.prng** %p.addr}, metadata !451), !dbg !452
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !453), !dbg !454
  store i32 %barrel, i32* %barrel.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %barrel.addr}, metadata !455), !dbg !456
  call void @llvm.dbg.declare(metadata !{i8* %b}, metadata !457), !dbg !459
  call void @llvm.dbg.declare(metadata !{[2 x %struct.hash_context]* %c}, metadata !460), !dbg !462
  store i8 0, i8* %b, align 1, !dbg !463
  br label %for.cond, !dbg !463

for.cond:                                         ; preds = %for.inc, %entry
  %0 = load i8* %b, align 1, !dbg !463
  %conv = zext i8 %0 to i32, !dbg !463
  %1 = load i32* %barrel.addr, align 4, !dbg !463
  %cmp = icmp slt i32 %conv, %1, !dbg !463
  br i1 %cmp, label %for.body, label %for.end, !dbg !463

for.body:                                         ; preds = %for.cond
  %arraydecay = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !465
  %2 = load i8* %b, align 1, !dbg !465
  %conv2 = zext i8 %2 to i32, !dbg !465
  %add.ptr = getelementptr inbounds %struct.hash_context* %arraydecay, i32 %conv2, !dbg !465
  call arm_aapcscc  void @hash_initial(%struct.hash_context* %add.ptr), !dbg !465
  %3 = load i8* %b, align 1, !dbg !467
  %conv3 = zext i8 %3 to i32, !dbg !467
  %cmp4 = icmp eq i32 %conv3, 1, !dbg !467
  br i1 %cmp4, label %if.then, label %if.end, !dbg !467

if.then:                                          ; preds = %for.body
  %arraydecay6 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !468
  %add.ptr7 = getelementptr inbounds %struct.hash_context* %arraydecay6, i32 1, !dbg !468
  call arm_aapcscc  void @hash_process(%struct.hash_context* %add.ptr7, i8* %b, i32 1), !dbg !468
  br label %if.end, !dbg !468

if.end:                                           ; preds = %if.then, %for.body
  br label %for.inc, !dbg !469

for.inc:                                          ; preds = %if.end
  %4 = load i8* %b, align 1, !dbg !470
  %conv8 = zext i8 %4 to i32, !dbg !470
  %add = add nsw i32 %conv8, 1, !dbg !470
  %conv9 = trunc i32 %add to i8, !dbg !470
  store i8 %conv9, i8* %b, align 1, !dbg !470
  br label %for.cond, !dbg !470

for.end:                                          ; preds = %for.cond
  %arraydecay10 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !471
  %5 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !471
  %6 = load i32* %barrel.addr, align 4, !dbg !471
  call arm_aapcscc  void @hash_process_file(%struct.hash_context* %arraydecay10, %struct._IO_FILE* %5, i32 %6), !dbg !471
  store i8 0, i8* %b, align 1, !dbg !472
  br label %for.cond11, !dbg !472

for.cond11:                                       ; preds = %for.inc22, %for.end
  %7 = load i8* %b, align 1, !dbg !472
  %conv12 = zext i8 %7 to i32, !dbg !472
  %8 = load i32* %barrel.addr, align 4, !dbg !472
  %cmp13 = icmp slt i32 %conv12, %8, !dbg !472
  br i1 %cmp13, label %for.body15, label %for.end26, !dbg !472

for.body15:                                       ; preds = %for.cond11
  %arraydecay16 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !474
  %9 = load %struct.prng** %p.addr, align 4, !dbg !474
  %seed = getelementptr inbounds %struct.prng* %9, i32 0, i32 1, !dbg !474
  %arraydecay17 = getelementptr inbounds [17 x i32]* %seed, i32 0, i32 0, !dbg !474
  %add.ptr18 = getelementptr inbounds i32* %arraydecay17, i32 1, !dbg !474
  %10 = load i8* %b, align 1, !dbg !474
  %conv19 = zext i8 %10 to i32, !dbg !474
  %add20 = add nsw i32 %conv19, 1, !dbg !474
  %mul = mul nsw i32 5, %add20, !dbg !474
  %add.ptr21 = getelementptr inbounds i32* %add.ptr18, i32 %mul, !dbg !474
  call arm_aapcscc  void @hash_final(%struct.hash_context* %arraydecay16, i32* %add.ptr21), !dbg !474
  br label %for.inc22, !dbg !476

for.inc22:                                        ; preds = %for.body15
  %11 = load i8* %b, align 1, !dbg !477
  %conv23 = zext i8 %11 to i32, !dbg !477
  %add24 = add nsw i32 %conv23, 1, !dbg !477
  %conv25 = trunc i32 %add24 to i8, !dbg !477
  store i8 %conv25, i8* %b, align 1, !dbg !477
  br label %for.cond11, !dbg !477

for.end26:                                        ; preds = %for.cond11
  %12 = load i32* %barrel.addr, align 4, !dbg !478
  %add27 = add nsw i32 %12, 1, !dbg !478
  %mul28 = mul nsw i32 %add27, 5, !dbg !478
  %add29 = add nsw i32 1, %mul28, !dbg !478
  %13 = load %struct.prng** %p.addr, align 4, !dbg !478
  %count = getelementptr inbounds %struct.prng* %13, i32 0, i32 0, !dbg !478
  store i32 %add29, i32* %count, align 4, !dbg !478
  ret void, !dbg !479
}

define arm_aapcscc void @clearsign(%struct.prng* %p, %struct._IO_FILE* %f_inp, %struct._IO_FILE* %f_out) nounwind uwtable {
entry:
  %p.addr = alloca %struct.prng*, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %f_out.addr = alloca %struct._IO_FILE*, align 4
  %c = alloca [2 x %struct.hash_context], align 4
  store %struct.prng* %p, %struct.prng** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.prng** %p.addr}, metadata !480), !dbg !481
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !482), !dbg !483
  store %struct._IO_FILE* %f_out, %struct._IO_FILE** %f_out.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_out.addr}, metadata !484), !dbg !485
  call void @llvm.dbg.declare(metadata !{[2 x %struct.hash_context]* %c}, metadata !486), !dbg !488
  %arraydecay = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !489
  call arm_aapcscc  void @prng_init_mac(%struct.hash_context* %arraydecay), !dbg !489
  %0 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !490
  %call = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([5 x i8]* @begin_clearsign, i32 0, i32 0), %struct._IO_FILE* %0), !dbg !490
  %arraydecay1 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !491
  %1 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !491
  %2 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !491
  call arm_aapcscc  void @hash_process_ascii(%struct.hash_context* %arraydecay1, %struct._IO_FILE* %1, %struct._IO_FILE* %2, i32 2, i32 1), !dbg !491
  %3 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !492
  %call2 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([31 x i8]* @end_clearsign, i32 0, i32 0), %struct._IO_FILE* %3), !dbg !492
  %arraydecay3 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !493
  %4 = load %struct.prng** %p.addr, align 4, !dbg !493
  %seed = getelementptr inbounds %struct.prng* %4, i32 0, i32 1, !dbg !493
  %arraydecay4 = getelementptr inbounds [17 x i32]* %seed, i32 0, i32 0, !dbg !493
  %add.ptr = getelementptr inbounds i32* %arraydecay4, i32 1, !dbg !493
  %add.ptr5 = getelementptr inbounds i32* %add.ptr, i32 5, !dbg !493
  call arm_aapcscc  void @hash_final(%struct.hash_context* %arraydecay3, i32* %add.ptr5), !dbg !493
  %arraydecay6 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !494
  %5 = load %struct.prng** %p.addr, align 4, !dbg !494
  %seed7 = getelementptr inbounds %struct.prng* %5, i32 0, i32 1, !dbg !494
  %arraydecay8 = getelementptr inbounds [17 x i32]* %seed7, i32 0, i32 0, !dbg !494
  %add.ptr9 = getelementptr inbounds i32* %arraydecay8, i32 1, !dbg !494
  %add.ptr10 = getelementptr inbounds i32* %add.ptr9, i32 10, !dbg !494
  call arm_aapcscc  void @hash_final(%struct.hash_context* %arraydecay6, i32* %add.ptr10), !dbg !494
  %6 = load %struct.prng** %p.addr, align 4, !dbg !495
  %count = getelementptr inbounds %struct.prng* %6, i32 0, i32 0, !dbg !495
  store i32 16, i32* %count, align 4, !dbg !495
  ret void, !dbg !496
}

define arm_aapcscc i32 @position(%struct._IO_FILE* %f_inp) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %buffer = alloca [1024 x i8], align 1
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !497), !dbg !498
  br label %while.cond, !dbg !499

while.cond:                                       ; preds = %if.end, %entry
  %0 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !501
  %call = call arm_aapcscc  i32 @feof(%struct._IO_FILE* %0) nounwind, !dbg !501
  %tobool = icmp ne i32 %call, 0, !dbg !501
  %lnot = xor i1 %tobool, true, !dbg !501
  br i1 %lnot, label %while.body, label %while.end, !dbg !501

while.body:                                       ; preds = %while.cond
  call void @llvm.dbg.declare(metadata !{[1024 x i8]* %buffer}, metadata !502), !dbg !507
  %arraydecay = getelementptr inbounds [1024 x i8]* %buffer, i32 0, i32 0, !dbg !508
  %1 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !508
  %call1 = call arm_aapcscc  i8* @fgets(i8* %arraydecay, i32 1024, %struct._IO_FILE* %1), !dbg !508
  %arraydecay2 = getelementptr inbounds [1024 x i8]* %buffer, i32 0, i32 0, !dbg !509
  %call3 = call arm_aapcscc  i32 @strncmp(i8* %arraydecay2, i8* getelementptr inbounds ([5 x i8]* @begin_clearsign, i32 0, i32 0), i32 3) nounwind readonly, !dbg !509
  %tobool4 = icmp ne i32 %call3, 0, !dbg !509
  br i1 %tobool4, label %if.end, label %if.then, !dbg !509

if.then:                                          ; preds = %while.body
  br label %while.end, !dbg !510

if.end:                                           ; preds = %while.body
  br label %while.cond, !dbg !511

while.end:                                        ; preds = %if.then, %while.cond
  %2 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !512
  %call5 = call arm_aapcscc  i32 @feof(%struct._IO_FILE* %2) nounwind, !dbg !512
  %tobool6 = icmp ne i32 %call5, 0, !dbg !512
  br i1 %tobool6, label %if.then7, label %if.end9, !dbg !512

if.then7:                                         ; preds = %while.end
  %3 = load %struct._IO_FILE** @stderr, align 4, !dbg !513
  %call8 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([42 x i8]* @err_clearsig_header_not_found, i32 0, i32 0), %struct._IO_FILE* %3), !dbg !513
  store i32 0, i32* %retval, !dbg !515
  br label %return, !dbg !515

if.end9:                                          ; preds = %while.end
  store i32 1, i32* %retval, !dbg !516
  br label %return, !dbg !516

return:                                           ; preds = %if.end9, %if.then7
  %4 = load i32* %retval, !dbg !517
  ret i32 %4, !dbg !517
}

define arm_aapcscc i32 @readsign(%struct.prng* %p, %struct._IO_FILE* %f_inp, %struct._IO_FILE* %f_out) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %p.addr = alloca %struct.prng*, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %f_out.addr = alloca %struct._IO_FILE*, align 4
  %c = alloca [2 x %struct.hash_context], align 4
  store %struct.prng* %p, %struct.prng** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.prng** %p.addr}, metadata !518), !dbg !519
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !520), !dbg !521
  store %struct._IO_FILE* %f_out, %struct._IO_FILE** %f_out.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_out.addr}, metadata !522), !dbg !523
  call void @llvm.dbg.declare(metadata !{[2 x %struct.hash_context]* %c}, metadata !524), !dbg !526
  %arraydecay = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !527
  call arm_aapcscc  void @prng_init_mac(%struct.hash_context* %arraydecay), !dbg !527
  %0 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !528
  %call = call arm_aapcscc  i32 @position(%struct._IO_FILE* %0), !dbg !528
  %tobool = icmp ne i32 %call, 0, !dbg !528
  br i1 %tobool, label %if.end, label %if.then, !dbg !528

if.then:                                          ; preds = %entry
  store i32 1, i32* %retval, !dbg !529
  br label %return, !dbg !529

if.end:                                           ; preds = %entry
  %arraydecay1 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !530
  %1 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !530
  %2 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !530
  call arm_aapcscc  void @hash_process_ascii(%struct.hash_context* %arraydecay1, %struct._IO_FILE* %1, %struct._IO_FILE* %2, i32 2, i32 0), !dbg !530
  %arraydecay2 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !531
  %3 = load %struct.prng** %p.addr, align 4, !dbg !531
  %seed = getelementptr inbounds %struct.prng* %3, i32 0, i32 1, !dbg !531
  %arraydecay3 = getelementptr inbounds [17 x i32]* %seed, i32 0, i32 0, !dbg !531
  %add.ptr = getelementptr inbounds i32* %arraydecay3, i32 1, !dbg !531
  %add.ptr4 = getelementptr inbounds i32* %add.ptr, i32 5, !dbg !531
  call arm_aapcscc  void @hash_final(%struct.hash_context* %arraydecay2, i32* %add.ptr4), !dbg !531
  %arraydecay5 = getelementptr inbounds [2 x %struct.hash_context]* %c, i32 0, i32 0, !dbg !532
  %4 = load %struct.prng** %p.addr, align 4, !dbg !532
  %seed6 = getelementptr inbounds %struct.prng* %4, i32 0, i32 1, !dbg !532
  %arraydecay7 = getelementptr inbounds [17 x i32]* %seed6, i32 0, i32 0, !dbg !532
  %add.ptr8 = getelementptr inbounds i32* %arraydecay7, i32 1, !dbg !532
  %add.ptr9 = getelementptr inbounds i32* %add.ptr8, i32 10, !dbg !532
  call arm_aapcscc  void @hash_final(%struct.hash_context* %arraydecay5, i32* %add.ptr9), !dbg !532
  %5 = load %struct.prng** %p.addr, align 4, !dbg !533
  %count = getelementptr inbounds %struct.prng* %5, i32 0, i32 0, !dbg !533
  store i32 16, i32* %count, align 4, !dbg !533
  store i32 0, i32* %retval, !dbg !534
  br label %return, !dbg !534

return:                                           ; preds = %if.end, %if.then
  %6 = load i32* %retval, !dbg !535
  ret i32 %6, !dbg !535
}

define arm_aapcscc void @prng_set_time(%struct.prng* %p) nounwind uwtable {
entry:
  %p.addr = alloca %struct.prng*, align 4
  store %struct.prng* %p, %struct.prng** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.prng** %p.addr}, metadata !536), !dbg !537
  %call = call arm_aapcscc  i32 @time(i32* null) nounwind, !dbg !538
  %0 = load %struct.prng** %p.addr, align 4, !dbg !538
  %seed = getelementptr inbounds %struct.prng* %0, i32 0, i32 1, !dbg !538
  %arrayidx = getelementptr inbounds [17 x i32]* %seed, i32 0, i32 16, !dbg !538
  store i32 %call, i32* %arrayidx, align 4, !dbg !538
  %1 = load %struct.prng** %p.addr, align 4, !dbg !540
  %count = getelementptr inbounds %struct.prng* %1, i32 0, i32 0, !dbg !540
  store i32 17, i32* %count, align 4, !dbg !540
  ret void, !dbg !541
}

declare arm_aapcscc i32 @time(i32*) nounwind

define arm_aapcscc i32 @prng_next(%struct.prng* %p) nounwind uwtable {
entry:
  %p.addr = alloca %struct.prng*, align 4
  %tmp = alloca [5 x i32], align 4
  %buffer = alloca [68 x i8], align 1
  %i = alloca i32, align 4
  %j = alloca i32, align 4
  %c = alloca %struct.hash_context, align 4
  store %struct.prng* %p, %struct.prng** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.prng** %p.addr}, metadata !542), !dbg !543
  call void @llvm.dbg.declare(metadata !{[5 x i32]* %tmp}, metadata !544), !dbg !547
  call void @llvm.dbg.declare(metadata !{[68 x i8]* %buffer}, metadata !548), !dbg !553
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !554), !dbg !555
  call void @llvm.dbg.declare(metadata !{i32* %j}, metadata !556), !dbg !557
  call void @llvm.dbg.declare(metadata !{%struct.hash_context* %c}, metadata !558), !dbg !559
  %0 = load %struct.prng** %p.addr, align 4, !dbg !560
  %seed = getelementptr inbounds %struct.prng* %0, i32 0, i32 1, !dbg !560
  %arrayidx = getelementptr inbounds [17 x i32]* %seed, i32 0, i32 0, !dbg !560
  %1 = load i32* %arrayidx, align 4, !dbg !560
  %add = add i32 %1, 1, !dbg !560
  store i32 %add, i32* %arrayidx, align 4, !dbg !560
  store i32 0, i32* %i, align 4, !dbg !561
  br label %for.cond, !dbg !561

for.cond:                                         ; preds = %for.inc14, %entry
  %2 = load i32* %i, align 4, !dbg !561
  %3 = load %struct.prng** %p.addr, align 4, !dbg !561
  %count = getelementptr inbounds %struct.prng* %3, i32 0, i32 0, !dbg !561
  %4 = load i32* %count, align 4, !dbg !561
  %cmp = icmp ult i32 %2, %4, !dbg !561
  br i1 %cmp, label %for.body, label %for.end16, !dbg !561

for.body:                                         ; preds = %for.cond
  store i32 0, i32* %j, align 4, !dbg !563
  br label %for.cond5, !dbg !563

for.cond5:                                        ; preds = %for.inc, %for.body
  %5 = load i32* %j, align 4, !dbg !563
  %cmp6 = icmp ult i32 %5, 4, !dbg !563
  br i1 %cmp6, label %for.body7, label %for.end, !dbg !563

for.body7:                                        ; preds = %for.cond5
  %6 = load i32* %i, align 4, !dbg !566
  %7 = load %struct.prng** %p.addr, align 4, !dbg !566
  %seed8 = getelementptr inbounds %struct.prng* %7, i32 0, i32 1, !dbg !566
  %arrayidx9 = getelementptr inbounds [17 x i32]* %seed8, i32 0, i32 %6, !dbg !566
  %8 = load i32* %arrayidx9, align 4, !dbg !566
  %9 = load i32* %j, align 4, !dbg !566
  %mul = mul i32 %9, 8, !dbg !566
  %shr = lshr i32 %8, %mul, !dbg !566
  %conv = trunc i32 %shr to i8, !dbg !566
  %10 = load i32* %i, align 4, !dbg !566
  %mul10 = mul i32 %10, 4, !dbg !566
  %11 = load i32* %j, align 4, !dbg !566
  %add11 = add i32 %mul10, %11, !dbg !566
  %arrayidx12 = getelementptr inbounds [68 x i8]* %buffer, i32 0, i32 %add11, !dbg !566
  store i8 %conv, i8* %arrayidx12, align 1, !dbg !566
  br label %for.inc, !dbg !568

for.inc:                                          ; preds = %for.body7
  %12 = load i32* %j, align 4, !dbg !569
  %add13 = add i32 %12, 1, !dbg !569
  store i32 %add13, i32* %j, align 4, !dbg !569
  br label %for.cond5, !dbg !569

for.end:                                          ; preds = %for.cond5
  br label %for.inc14, !dbg !570

for.inc14:                                        ; preds = %for.end
  %13 = load i32* %i, align 4, !dbg !571
  %add15 = add i32 %13, 1, !dbg !571
  store i32 %add15, i32* %i, align 4, !dbg !571
  br label %for.cond, !dbg !571

for.end16:                                        ; preds = %for.cond
  call arm_aapcscc  void @hash_initial(%struct.hash_context* %c), !dbg !572
  %arraydecay = getelementptr inbounds [68 x i8]* %buffer, i32 0, i32 0, !dbg !573
  %14 = load %struct.prng** %p.addr, align 4, !dbg !573
  %count17 = getelementptr inbounds %struct.prng* %14, i32 0, i32 0, !dbg !573
  %15 = load i32* %count17, align 4, !dbg !573
  %mul18 = mul i32 %15, 4, !dbg !573
  call arm_aapcscc  void @hash_process(%struct.hash_context* %c, i8* %arraydecay, i32 %mul18), !dbg !573
  %arraydecay19 = getelementptr inbounds [5 x i32]* %tmp, i32 0, i32 0, !dbg !574
  call arm_aapcscc  void @hash_final(%struct.hash_context* %c, i32* %arraydecay19), !dbg !574
  %arraydecay20 = getelementptr inbounds [68 x i8]* %buffer, i32 0, i32 0, !dbg !575
  call void @llvm.memset.p0i8.i32(i8* %arraydecay20, i8 0, i32 68, i32 1, i1 false), !dbg !575
  %arrayidx21 = getelementptr inbounds [5 x i32]* %tmp, i32 0, i32 0, !dbg !576
  %16 = load i32* %arrayidx21, align 4, !dbg !576
  ret i32 %16, !dbg !576
}

define arm_aapcscc void @prng_to_vlong(%struct.prng* %p, i16* %V) nounwind uwtable {
entry:
  %p.addr = alloca %struct.prng*, align 4
  %V.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  store %struct.prng* %p, %struct.prng** %p.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.prng** %p.addr}, metadata !577), !dbg !578
  store i16* %V, i16** %V.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %V.addr}, metadata !579), !dbg !582
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !583), !dbg !585
  %0 = load i16** %V.addr, align 4, !dbg !586
  %arrayidx = getelementptr inbounds i16* %0, i32 0, !dbg !586
  store i16 15, i16* %arrayidx, !dbg !586
  store i32 1, i32* %i, align 4, !dbg !587
  br label %for.cond, !dbg !587

for.cond:                                         ; preds = %for.inc, %entry
  %1 = load i32* %i, align 4, !dbg !587
  %cmp = icmp ult i32 %1, 16, !dbg !587
  br i1 %cmp, label %for.body, label %for.end, !dbg !587

for.body:                                         ; preds = %for.cond
  %2 = load %struct.prng** %p.addr, align 4, !dbg !589
  %call = call arm_aapcscc  i32 @prng_next(%struct.prng* %2), !dbg !589
  %conv = trunc i32 %call to i16, !dbg !589
  %3 = load i32* %i, align 4, !dbg !589
  %4 = load i16** %V.addr, align 4, !dbg !589
  %arrayidx1 = getelementptr inbounds i16* %4, i32 %3, !dbg !589
  store i16 %conv, i16* %arrayidx1, !dbg !589
  br label %for.inc, !dbg !589

for.inc:                                          ; preds = %for.body
  %5 = load i32* %i, align 4, !dbg !590
  %add = add i32 %5, 1, !dbg !590
  store i32 %add, i32* %i, align 4, !dbg !590
  br label %for.cond, !dbg !590

for.end:                                          ; preds = %for.cond
  ret void, !dbg !591
}

define arm_aapcscc void @hash_to_vlong(i32* %mac, i16* %V) nounwind uwtable {
entry:
  %mac.addr = alloca i32*, align 4
  %V.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  %x = alloca i32, align 4
  store i32* %mac, i32** %mac.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %mac.addr}, metadata !592), !dbg !594
  store i16* %V, i16** %V.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %V.addr}, metadata !595), !dbg !596
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !597), !dbg !599
  %0 = load i16** %V.addr, align 4, !dbg !600
  %arrayidx = getelementptr inbounds i16* %0, i32 0, !dbg !600
  store i16 15, i16* %arrayidx, !dbg !600
  store i32 0, i32* %i, align 4, !dbg !601
  br label %for.cond, !dbg !601

for.cond:                                         ; preds = %for.inc, %entry
  %1 = load i32* %i, align 4, !dbg !601
  %cmp = icmp ult i32 %1, 8, !dbg !601
  br i1 %cmp, label %for.body, label %for.end, !dbg !601

for.body:                                         ; preds = %for.cond
  call void @llvm.dbg.declare(metadata !{i32* %x}, metadata !603), !dbg !605
  %2 = load i32* %i, align 4, !dbg !606
  %3 = load i32** %mac.addr, align 4, !dbg !606
  %arrayidx1 = getelementptr inbounds i32* %3, i32 %2, !dbg !606
  %4 = load i32* %arrayidx1, !dbg !606
  store i32 %4, i32* %x, align 4, !dbg !606
  %5 = load i32* %x, align 4, !dbg !607
  %conv = trunc i32 %5 to i16, !dbg !607
  %6 = load i32* %i, align 4, !dbg !607
  %mul = mul i32 %6, 2, !dbg !607
  %add = add i32 %mul, 1, !dbg !607
  %7 = load i16** %V.addr, align 4, !dbg !607
  %arrayidx2 = getelementptr inbounds i16* %7, i32 %add, !dbg !607
  store i16 %conv, i16* %arrayidx2, !dbg !607
  %8 = load i32* %x, align 4, !dbg !608
  %shr = lshr i32 %8, 16, !dbg !608
  %conv3 = trunc i32 %shr to i16, !dbg !608
  %9 = load i32* %i, align 4, !dbg !608
  %mul4 = mul i32 %9, 2, !dbg !608
  %add5 = add i32 %mul4, 2, !dbg !608
  %10 = load i16** %V.addr, align 4, !dbg !608
  %arrayidx6 = getelementptr inbounds i16* %10, i32 %add5, !dbg !608
  store i16 %conv3, i16* %arrayidx6, !dbg !608
  br label %for.inc, !dbg !609

for.inc:                                          ; preds = %for.body
  %11 = load i32* %i, align 4, !dbg !610
  %add7 = add i32 %11, 1, !dbg !610
  store i32 %add7, i32* %i, align 4, !dbg !610
  br label %for.cond, !dbg !610

for.end:                                          ; preds = %for.cond
  ret void, !dbg !611
}

define arm_aapcscc void @get_vlong(%struct._IO_FILE* %f, i16* %v) nounwind uwtable {
entry:
  %f.addr = alloca %struct._IO_FILE*, align 4
  %v.addr = alloca i16*, align 4
  %u = alloca i32, align 4
  %w = alloca [19 x i16], align 2
  store %struct._IO_FILE* %f, %struct._IO_FILE** %f.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f.addr}, metadata !612), !dbg !613
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !614), !dbg !615
  call void @llvm.dbg.declare(metadata !{i32* %u}, metadata !616), !dbg !618
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %w}, metadata !619), !dbg !624
  %0 = load i16** %v.addr, align 4, !dbg !625
  call arm_aapcscc  void @vlClear(i16* %0), !dbg !625
  %arrayidx = getelementptr inbounds [19 x i16]* %w, i32 0, i32 0, !dbg !626
  store i16 1, i16* %arrayidx, align 2, !dbg !626
  br label %while.body, !dbg !627

while.body:                                       ; preds = %entry, %if.then15, %if.end18
  %1 = load %struct._IO_FILE** %f.addr, align 4, !dbg !628
  %call = call arm_aapcscc  i32 @fgetc(%struct._IO_FILE* %1), !dbg !628
  store i32 %call, i32* %u, align 4, !dbg !628
  %2 = load i32* %u, align 4, !dbg !630
  %cmp = icmp uge i32 %2, 48, !dbg !630
  br i1 %cmp, label %land.lhs.true, label %if.else, !dbg !630

land.lhs.true:                                    ; preds = %while.body
  %3 = load i32* %u, align 4, !dbg !630
  %cmp1 = icmp ule i32 %3, 57, !dbg !630
  br i1 %cmp1, label %if.then, label %if.else, !dbg !630

if.then:                                          ; preds = %land.lhs.true
  %4 = load i32* %u, align 4, !dbg !631
  %sub = sub i32 %4, 48, !dbg !631
  store i32 %sub, i32* %u, align 4, !dbg !631
  br label %if.end18, !dbg !631

if.else:                                          ; preds = %land.lhs.true, %while.body
  %5 = load i32* %u, align 4, !dbg !632
  %cmp2 = icmp uge i32 %5, 97, !dbg !632
  br i1 %cmp2, label %land.lhs.true3, label %if.else7, !dbg !632

land.lhs.true3:                                   ; preds = %if.else
  %6 = load i32* %u, align 4, !dbg !632
  %cmp4 = icmp ule i32 %6, 122, !dbg !632
  br i1 %cmp4, label %if.then5, label %if.else7, !dbg !632

if.then5:                                         ; preds = %land.lhs.true3
  %7 = load i32* %u, align 4, !dbg !633
  %sub6 = sub i32 %7, 87, !dbg !633
  store i32 %sub6, i32* %u, align 4, !dbg !633
  br label %if.end17, !dbg !633

if.else7:                                         ; preds = %land.lhs.true3, %if.else
  %8 = load i32* %u, align 4, !dbg !634
  %cmp8 = icmp uge i32 %8, 65, !dbg !634
  br i1 %cmp8, label %land.lhs.true9, label %if.else13, !dbg !634

land.lhs.true9:                                   ; preds = %if.else7
  %9 = load i32* %u, align 4, !dbg !634
  %cmp10 = icmp ule i32 %9, 90, !dbg !634
  br i1 %cmp10, label %if.then11, label %if.else13, !dbg !634

if.then11:                                        ; preds = %land.lhs.true9
  %10 = load i32* %u, align 4, !dbg !635
  %sub12 = sub i32 %10, 55, !dbg !635
  store i32 %sub12, i32* %u, align 4, !dbg !635
  br label %if.end, !dbg !635

if.else13:                                        ; preds = %land.lhs.true9, %if.else7
  %11 = load i32* %u, align 4, !dbg !636
  %cmp14 = icmp ule i32 %11, 32, !dbg !636
  br i1 %cmp14, label %if.then15, label %if.else16, !dbg !636

if.then15:                                        ; preds = %if.else13
  br label %while.body, !dbg !637

if.else16:                                        ; preds = %if.else13
  br label %while.end, !dbg !638

if.end:                                           ; preds = %if.then11
  br label %if.end17

if.end17:                                         ; preds = %if.end, %if.then5
  br label %if.end18

if.end18:                                         ; preds = %if.end17, %if.then
  %12 = load i16** %v.addr, align 4, !dbg !639
  call arm_aapcscc  void @vlShortLshift(i16* %12, i32 4), !dbg !639
  %13 = load i32* %u, align 4, !dbg !640
  %conv = trunc i32 %13 to i16, !dbg !640
  %arrayidx19 = getelementptr inbounds [19 x i16]* %w, i32 0, i32 1, !dbg !640
  store i16 %conv, i16* %arrayidx19, align 2, !dbg !640
  %14 = load i16** %v.addr, align 4, !dbg !641
  %arraydecay = getelementptr inbounds [19 x i16]* %w, i32 0, i32 0, !dbg !641
  call arm_aapcscc  void @vlAdd(i16* %14, i16* %arraydecay), !dbg !641
  br label %while.body, !dbg !642

while.end:                                        ; preds = %if.else16
  ret void, !dbg !643
}

declare arm_aapcscc void @vlClear(i16*)

declare arm_aapcscc i32 @fgetc(%struct._IO_FILE*)

declare arm_aapcscc void @vlShortLshift(i16*, i32)

declare arm_aapcscc void @vlAdd(i16*, i16*)

define arm_aapcscc void @get_vlong_a(%struct._IO_FILE* %f, i16* %v) nounwind uwtable {
entry:
  %f.addr = alloca %struct._IO_FILE*, align 4
  %v.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  %buffer = alloca [256 x i8], align 1
  %u = alloca i8, align 1
  %w = alloca [19 x i16], align 2
  store %struct._IO_FILE* %f, %struct._IO_FILE** %f.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f.addr}, metadata !644), !dbg !645
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !646), !dbg !647
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !648), !dbg !650
  store i32 0, i32* %i, align 4, !dbg !651
  call void @llvm.dbg.declare(metadata !{[256 x i8]* %buffer}, metadata !652), !dbg !656
  call void @llvm.dbg.declare(metadata !{i8* %u}, metadata !657), !dbg !658
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %w}, metadata !659), !dbg !660
  %0 = load i16** %v.addr, align 4, !dbg !661
  call arm_aapcscc  void @vlClear(i16* %0), !dbg !661
  %arrayidx = getelementptr inbounds [19 x i16]* %w, i32 0, i32 0, !dbg !662
  store i16 1, i16* %arrayidx, align 2, !dbg !662
  %arrayidx1 = getelementptr inbounds [256 x i8]* %buffer, i32 0, i32 0, !dbg !663
  store i8 0, i8* %arrayidx1, align 1, !dbg !663
  %arraydecay = getelementptr inbounds [256 x i8]* %buffer, i32 0, i32 0, !dbg !664
  %1 = load %struct._IO_FILE** %f.addr, align 4, !dbg !664
  %call = call arm_aapcscc  i8* @fgets(i8* %arraydecay, i32 256, %struct._IO_FILE* %1), !dbg !664
  br label %while.cond, !dbg !665

while.cond:                                       ; preds = %if.end42, %if.then39, %entry
  %2 = load i32* %i, align 4, !dbg !665
  %inc = add i32 %2, 1, !dbg !665
  store i32 %inc, i32* %i, align 4, !dbg !665
  %arrayidx2 = getelementptr inbounds [256 x i8]* %buffer, i32 0, i32 %2, !dbg !665
  %3 = load i8* %arrayidx2, align 1, !dbg !665
  store i8 %3, i8* %u, align 1, !dbg !665
  %conv = sext i8 %3 to i32, !dbg !665
  %cmp = icmp ne i32 %conv, 0, !dbg !665
  br i1 %cmp, label %while.body, label %while.end, !dbg !665

while.body:                                       ; preds = %while.cond
  %4 = load i8* %u, align 1, !dbg !666
  %conv4 = sext i8 %4 to i32, !dbg !666
  %cmp5 = icmp sge i32 %conv4, 48, !dbg !666
  br i1 %cmp5, label %land.lhs.true, label %if.else, !dbg !666

land.lhs.true:                                    ; preds = %while.body
  %5 = load i8* %u, align 1, !dbg !666
  %conv7 = sext i8 %5 to i32, !dbg !666
  %cmp8 = icmp sle i32 %conv7, 57, !dbg !666
  br i1 %cmp8, label %if.then, label %if.else, !dbg !666

if.then:                                          ; preds = %land.lhs.true
  %6 = load i8* %u, align 1, !dbg !668
  %conv10 = sext i8 %6 to i32, !dbg !668
  %sub = sub nsw i32 %conv10, 48, !dbg !668
  %conv11 = trunc i32 %sub to i8, !dbg !668
  store i8 %conv11, i8* %u, align 1, !dbg !668
  br label %if.end42, !dbg !668

if.else:                                          ; preds = %land.lhs.true, %while.body
  %7 = load i8* %u, align 1, !dbg !669
  %conv12 = sext i8 %7 to i32, !dbg !669
  %cmp13 = icmp sge i32 %conv12, 97, !dbg !669
  br i1 %cmp13, label %land.lhs.true15, label %if.else23, !dbg !669

land.lhs.true15:                                  ; preds = %if.else
  %8 = load i8* %u, align 1, !dbg !669
  %conv16 = sext i8 %8 to i32, !dbg !669
  %cmp17 = icmp sle i32 %conv16, 122, !dbg !669
  br i1 %cmp17, label %if.then19, label %if.else23, !dbg !669

if.then19:                                        ; preds = %land.lhs.true15
  %9 = load i8* %u, align 1, !dbg !670
  %conv20 = sext i8 %9 to i32, !dbg !670
  %sub21 = sub nsw i32 %conv20, 87, !dbg !670
  %conv22 = trunc i32 %sub21 to i8, !dbg !670
  store i8 %conv22, i8* %u, align 1, !dbg !670
  br label %if.end41, !dbg !670

if.else23:                                        ; preds = %land.lhs.true15, %if.else
  %10 = load i8* %u, align 1, !dbg !671
  %conv24 = sext i8 %10 to i32, !dbg !671
  %cmp25 = icmp sge i32 %conv24, 65, !dbg !671
  br i1 %cmp25, label %land.lhs.true27, label %if.else35, !dbg !671

land.lhs.true27:                                  ; preds = %if.else23
  %11 = load i8* %u, align 1, !dbg !671
  %conv28 = sext i8 %11 to i32, !dbg !671
  %cmp29 = icmp sle i32 %conv28, 90, !dbg !671
  br i1 %cmp29, label %if.then31, label %if.else35, !dbg !671

if.then31:                                        ; preds = %land.lhs.true27
  %12 = load i8* %u, align 1, !dbg !672
  %conv32 = sext i8 %12 to i32, !dbg !672
  %sub33 = sub nsw i32 %conv32, 55, !dbg !672
  %conv34 = trunc i32 %sub33 to i8, !dbg !672
  store i8 %conv34, i8* %u, align 1, !dbg !672
  br label %if.end, !dbg !672

if.else35:                                        ; preds = %land.lhs.true27, %if.else23
  %13 = load i8* %u, align 1, !dbg !673
  %conv36 = sext i8 %13 to i32, !dbg !673
  %cmp37 = icmp sle i32 %conv36, 32, !dbg !673
  br i1 %cmp37, label %if.then39, label %if.else40, !dbg !673

if.then39:                                        ; preds = %if.else35
  br label %while.cond, !dbg !674

if.else40:                                        ; preds = %if.else35
  br label %while.end, !dbg !675

if.end:                                           ; preds = %if.then31
  br label %if.end41

if.end41:                                         ; preds = %if.end, %if.then19
  br label %if.end42

if.end42:                                         ; preds = %if.end41, %if.then
  %14 = load i16** %v.addr, align 4, !dbg !676
  call arm_aapcscc  void @vlShortLshift(i16* %14, i32 4), !dbg !676
  %15 = load i8* %u, align 1, !dbg !677
  %conv43 = sext i8 %15 to i16, !dbg !677
  %arrayidx44 = getelementptr inbounds [19 x i16]* %w, i32 0, i32 1, !dbg !677
  store i16 %conv43, i16* %arrayidx44, align 2, !dbg !677
  %16 = load i16** %v.addr, align 4, !dbg !678
  %arraydecay45 = getelementptr inbounds [19 x i16]* %w, i32 0, i32 0, !dbg !678
  call arm_aapcscc  void @vlAdd(i16* %16, i16* %arraydecay45), !dbg !678
  br label %while.cond, !dbg !679

while.end:                                        ; preds = %if.else40, %while.cond
  ret void, !dbg !680
}

define arm_aapcscc void @put_vlong(i16* %v) nounwind uwtable {
entry:
  %v.addr = alloca i16*, align 4
  %i = alloca i32, align 4
  %j = alloca i32, align 4
  %x = alloca i32, align 4
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !681), !dbg !682
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !683), !dbg !685
  call void @llvm.dbg.declare(metadata !{i32* %j}, metadata !686), !dbg !687
  %0 = load i16** %v.addr, align 4, !dbg !688
  %arrayidx = getelementptr inbounds i16* %0, i32 0, !dbg !688
  %1 = load i16* %arrayidx, !dbg !688
  %conv = zext i16 %1 to i32, !dbg !688
  store i32 %conv, i32* %i, align 4, !dbg !688
  br label %for.cond, !dbg !688

for.cond:                                         ; preds = %for.inc10, %entry
  %2 = load i32* %i, align 4, !dbg !688
  %cmp = icmp ugt i32 %2, 0, !dbg !688
  br i1 %cmp, label %for.body, label %for.end11, !dbg !688

for.body:                                         ; preds = %for.cond
  call void @llvm.dbg.declare(metadata !{i32* %x}, metadata !690), !dbg !692
  %3 = load i32* %i, align 4, !dbg !693
  %4 = load i16** %v.addr, align 4, !dbg !693
  %arrayidx2 = getelementptr inbounds i16* %4, i32 %3, !dbg !693
  %5 = load i16* %arrayidx2, !dbg !693
  %conv3 = zext i16 %5 to i32, !dbg !693
  store i32 %conv3, i32* %x, align 4, !dbg !693
  store i32 0, i32* %j, align 4, !dbg !694
  br label %for.cond4, !dbg !694

for.cond4:                                        ; preds = %for.inc, %for.body
  %6 = load i32* %j, align 4, !dbg !694
  %cmp5 = icmp ult i32 %6, 4, !dbg !694
  br i1 %cmp5, label %for.body7, label %for.end, !dbg !694

for.body7:                                        ; preds = %for.cond4
  %7 = load i32* %x, align 4, !dbg !696
  %8 = load i32* %j, align 4, !dbg !696
  %mul = mul i32 4, %8, !dbg !696
  %sub = sub i32 12, %mul, !dbg !696
  %shr = lshr i32 %7, %sub, !dbg !696
  %rem = urem i32 %shr, 16, !dbg !696
  %arrayidx8 = getelementptr inbounds [16 x i8]* @hex, i32 0, i32 %rem, !dbg !696
  %9 = load i8* %arrayidx8, align 1, !dbg !696
  %conv9 = sext i8 %9 to i32, !dbg !696
  %call = call arm_aapcscc  i32 @putchar(i32 %conv9), !dbg !696
  br label %for.inc, !dbg !696

for.inc:                                          ; preds = %for.body7
  %10 = load i32* %j, align 4, !dbg !697
  %add = add i32 %10, 1, !dbg !697
  store i32 %add, i32* %j, align 4, !dbg !697
  br label %for.cond4, !dbg !697

for.end:                                          ; preds = %for.cond4
  br label %for.inc10, !dbg !698

for.inc10:                                        ; preds = %for.end
  %11 = load i32* %i, align 4, !dbg !699
  %dec = add i32 %11, -1, !dbg !699
  store i32 %dec, i32* %i, align 4, !dbg !699
  br label %for.cond, !dbg !699

for.end11:                                        ; preds = %for.cond
  ret void, !dbg !700
}

declare arm_aapcscc i32 @putchar(i32)

define arm_aapcscc void @put_binary_vlong(%struct._IO_FILE* %f, i16* %v) nounwind uwtable {
entry:
  %f.addr = alloca %struct._IO_FILE*, align 4
  %v.addr = alloca i16*, align 4
  %n = alloca i32, align 4
  store %struct._IO_FILE* %f, %struct._IO_FILE** %f.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f.addr}, metadata !701), !dbg !702
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !703), !dbg !704
  call void @llvm.dbg.declare(metadata !{i32* %n}, metadata !705), !dbg !707
  store i32 32, i32* %n, align 4, !dbg !708
  br label %while.cond, !dbg !709

while.cond:                                       ; preds = %if.end, %entry
  %0 = load i32* %n, align 4, !dbg !709
  %dec = add i32 %0, -1, !dbg !709
  store i32 %dec, i32* %n, align 4, !dbg !709
  %tobool = icmp ne i32 %0, 0, !dbg !709
  br i1 %tobool, label %while.body, label %while.end, !dbg !709

while.body:                                       ; preds = %while.cond
  %1 = load i16** %v.addr, align 4, !dbg !710
  %arrayidx = getelementptr inbounds i16* %1, i32 0, !dbg !710
  %2 = load i16* %arrayidx, !dbg !710
  %conv = zext i16 %2 to i32, !dbg !710
  %cmp = icmp eq i32 %conv, 0, !dbg !710
  br i1 %cmp, label %if.then, label %if.end, !dbg !710

if.then:                                          ; preds = %while.body
  %3 = load i16** %v.addr, align 4, !dbg !712
  %arrayidx2 = getelementptr inbounds i16* %3, i32 1, !dbg !712
  store i16 0, i16* %arrayidx2, !dbg !712
  br label %if.end, !dbg !712

if.end:                                           ; preds = %if.then, %while.body
  %4 = load i16** %v.addr, align 4, !dbg !713
  %arrayidx3 = getelementptr inbounds i16* %4, i32 1, !dbg !713
  %5 = load i16* %arrayidx3, !dbg !713
  %conv4 = zext i16 %5 to i32, !dbg !713
  %and = and i32 %conv4, 255, !dbg !713
  %6 = load %struct._IO_FILE** %f.addr, align 4, !dbg !713
  %call = call arm_aapcscc  i32 @fputcPlus(i32 %and, %struct._IO_FILE* %6), !dbg !713
  %7 = load i16** %v.addr, align 4, !dbg !714
  call arm_aapcscc  void @vlShortRshift(i16* %7, i32 8), !dbg !714
  br label %while.cond, !dbg !715

while.end:                                        ; preds = %while.cond
  ret void, !dbg !716
}

declare arm_aapcscc i32 @fputcPlus(i32, %struct._IO_FILE*)

declare arm_aapcscc void @vlShortRshift(i16*, i32)

define arm_aapcscc void @get_binary_vlong(%struct._IO_FILE* %f, i16* %v) nounwind uwtable {
entry:
  %f.addr = alloca %struct._IO_FILE*, align 4
  %v.addr = alloca i16*, align 4
  %u = alloca [32 x i8], align 1
  %w = alloca [19 x i16], align 2
  %n = alloca i32, align 4
  store %struct._IO_FILE* %f, %struct._IO_FILE** %f.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f.addr}, metadata !717), !dbg !718
  store i16* %v, i16** %v.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %v.addr}, metadata !719), !dbg !720
  call void @llvm.dbg.declare(metadata !{[32 x i8]* %u}, metadata !721), !dbg !724
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %w}, metadata !725), !dbg !726
  call void @llvm.dbg.declare(metadata !{i32* %n}, metadata !727), !dbg !728
  store i32 32, i32* %n, align 4, !dbg !729
  %arraydecay = getelementptr inbounds [32 x i8]* %u, i32 0, i32 0, !dbg !730
  %0 = load %struct._IO_FILE** %f.addr, align 4, !dbg !730
  %call = call arm_aapcscc  i32 @freadPlus(i8* %arraydecay, i32 1, i32 32, %struct._IO_FILE* %0), !dbg !730
  %1 = load i16** %v.addr, align 4, !dbg !731
  call arm_aapcscc  void @vlClear(i16* %1), !dbg !731
  %arrayidx = getelementptr inbounds [19 x i16]* %w, i32 0, i32 0, !dbg !732
  store i16 1, i16* %arrayidx, align 2, !dbg !732
  br label %while.cond, !dbg !733

while.cond:                                       ; preds = %while.body, %entry
  %2 = load i32* %n, align 4, !dbg !733
  %dec = add i32 %2, -1, !dbg !733
  store i32 %dec, i32* %n, align 4, !dbg !733
  %tobool = icmp ne i32 %2, 0, !dbg !733
  br i1 %tobool, label %while.body, label %while.end, !dbg !733

while.body:                                       ; preds = %while.cond
  %3 = load i16** %v.addr, align 4, !dbg !734
  call arm_aapcscc  void @vlShortLshift(i16* %3, i32 8), !dbg !734
  %4 = load i32* %n, align 4, !dbg !736
  %arrayidx1 = getelementptr inbounds [32 x i8]* %u, i32 0, i32 %4, !dbg !736
  %5 = load i8* %arrayidx1, align 1, !dbg !736
  %conv = zext i8 %5 to i16, !dbg !736
  %arrayidx2 = getelementptr inbounds [19 x i16]* %w, i32 0, i32 1, !dbg !736
  store i16 %conv, i16* %arrayidx2, align 2, !dbg !736
  %6 = load i16** %v.addr, align 4, !dbg !737
  %arraydecay3 = getelementptr inbounds [19 x i16]* %w, i32 0, i32 0, !dbg !737
  call arm_aapcscc  void @vlAdd(i16* %6, i16* %arraydecay3), !dbg !737
  br label %while.cond, !dbg !738

while.end:                                        ; preds = %while.cond
  ret void, !dbg !739
}

declare arm_aapcscc i32 @freadPlus(i8*, i32, i32, %struct._IO_FILE*)

define arm_aapcscc void @vlong_to_square_block(i16* %V, i8* %key) nounwind uwtable {
entry:
  %V.addr = alloca i16*, align 4
  %key.addr = alloca i8*, align 4
  %v = alloca [19 x i16], align 2
  %j = alloca i32, align 4
  store i16* %V, i16** %V.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %V.addr}, metadata !740), !dbg !743
  store i8* %key, i8** %key.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %key.addr}, metadata !744), !dbg !746
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %v}, metadata !747), !dbg !749
  call void @llvm.dbg.declare(metadata !{i32* %j}, metadata !750), !dbg !751
  %arraydecay = getelementptr inbounds [19 x i16]* %v, i32 0, i32 0, !dbg !752
  %0 = load i16** %V.addr, align 4, !dbg !752
  call arm_aapcscc  void @vlCopy(i16* %arraydecay, i16* %0), !dbg !752
  store i32 0, i32* %j, align 4, !dbg !753
  br label %for.cond, !dbg !753

for.cond:                                         ; preds = %for.inc, %entry
  %1 = load i32* %j, align 4, !dbg !753
  %cmp = icmp ult i32 %1, 16, !dbg !753
  br i1 %cmp, label %for.body, label %for.end, !dbg !753

for.body:                                         ; preds = %for.cond
  %arrayidx = getelementptr inbounds [19 x i16]* %v, i32 0, i32 0, !dbg !755
  %2 = load i16* %arrayidx, align 2, !dbg !755
  %conv = zext i16 %2 to i32, !dbg !755
  %cmp1 = icmp eq i32 %conv, 0, !dbg !755
  br i1 %cmp1, label %if.then, label %if.end, !dbg !755

if.then:                                          ; preds = %for.body
  %arrayidx3 = getelementptr inbounds [19 x i16]* %v, i32 0, i32 1, !dbg !757
  store i16 0, i16* %arrayidx3, align 2, !dbg !757
  br label %if.end, !dbg !757

if.end:                                           ; preds = %if.then, %for.body
  %arrayidx4 = getelementptr inbounds [19 x i16]* %v, i32 0, i32 1, !dbg !758
  %3 = load i16* %arrayidx4, align 2, !dbg !758
  %conv5 = trunc i16 %3 to i8, !dbg !758
  %4 = load i32* %j, align 4, !dbg !758
  %5 = load i8** %key.addr, align 4, !dbg !758
  %arrayidx6 = getelementptr inbounds i8* %5, i32 %4, !dbg !758
  store i8 %conv5, i8* %arrayidx6, !dbg !758
  %arraydecay7 = getelementptr inbounds [19 x i16]* %v, i32 0, i32 0, !dbg !759
  call arm_aapcscc  void @vlShortRshift(i16* %arraydecay7, i32 8), !dbg !759
  br label %for.inc, !dbg !760

for.inc:                                          ; preds = %if.end
  %6 = load i32* %j, align 4, !dbg !761
  %inc = add i32 %6, 1, !dbg !761
  store i32 %inc, i32* %j, align 4, !dbg !761
  br label %for.cond, !dbg !761

for.end:                                          ; preds = %for.cond
  ret void, !dbg !762
}

declare arm_aapcscc void @vlCopy(i16*, i16*)

define arm_aapcscc void @increment(i8* %iv) nounwind uwtable {
entry:
  %iv.addr = alloca i8*, align 4
  %i = alloca i32, align 4
  store i8* %iv, i8** %iv.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %iv.addr}, metadata !763), !dbg !764
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !765), !dbg !767
  store i32 0, i32* %i, align 4, !dbg !768
  br label %while.cond, !dbg !769

while.cond:                                       ; preds = %while.body, %entry
  %0 = load i32* %i, align 4, !dbg !769
  %1 = load i8** %iv.addr, align 4, !dbg !769
  %arrayidx = getelementptr inbounds i8* %1, i32 %0, !dbg !769
  %2 = load i8* %arrayidx, !dbg !769
  %conv = zext i8 %2 to i32, !dbg !769
  %cmp = icmp eq i32 %conv, 255, !dbg !769
  br i1 %cmp, label %while.body, label %while.end, !dbg !769

while.body:                                       ; preds = %while.cond
  %3 = load i32* %i, align 4, !dbg !770
  %inc = add nsw i32 %3, 1, !dbg !770
  store i32 %inc, i32* %i, align 4, !dbg !770
  %4 = load i8** %iv.addr, align 4, !dbg !770
  %arrayidx2 = getelementptr inbounds i8* %4, i32 %3, !dbg !770
  store i8 0, i8* %arrayidx2, !dbg !770
  br label %while.cond, !dbg !770

while.end:                                        ; preds = %while.cond
  %5 = load i32* %i, align 4, !dbg !771
  %6 = load i8** %iv.addr, align 4, !dbg !771
  %arrayidx3 = getelementptr inbounds i8* %6, i32 %5, !dbg !771
  %7 = load i8* %arrayidx3, !dbg !771
  %conv4 = zext i8 %7 to i32, !dbg !771
  %add = add nsw i32 %conv4, 1, !dbg !771
  %conv5 = trunc i32 %add to i8, !dbg !771
  store i8 %conv5, i8* %arrayidx3, !dbg !771
  ret void, !dbg !772
}

define arm_aapcscc i32 @sym_encrypt(i16* %secret, %struct._IO_FILE* %f_inp, %struct._IO_FILE* %f_out) nounwind uwtable {
entry:
  %secret.addr = alloca i16*, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %f_out.addr = alloca %struct._IO_FILE*, align 4
  %key = alloca [16 x i8], align 1
  %iv = alloca [16 x i8], align 1
  %ctx = alloca %struct.squareCtsContext, align 4
  %buffer = alloca [1025 x i32], align 4
  %n = alloca i32, align 4
  %err = alloca i32, align 4
  %pad = alloca i8, align 1
  %written = alloca i32, align 4
  store i16* %secret, i16** %secret.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %secret.addr}, metadata !773), !dbg !774
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !775), !dbg !776
  store %struct._IO_FILE* %f_out, %struct._IO_FILE** %f_out.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_out.addr}, metadata !777), !dbg !778
  call void @llvm.dbg.declare(metadata !{[16 x i8]* %key}, metadata !779), !dbg !783
  call void @llvm.dbg.declare(metadata !{[16 x i8]* %iv}, metadata !784), !dbg !785
  call void @llvm.dbg.declare(metadata !{%struct.squareCtsContext* %ctx}, metadata !786), !dbg !798
  call void @llvm.dbg.declare(metadata !{[1025 x i32]* %buffer}, metadata !799), !dbg !804
  call void @llvm.dbg.declare(metadata !{i32* %n}, metadata !805), !dbg !806
  call void @llvm.dbg.declare(metadata !{i32* %err}, metadata !807), !dbg !808
  store i32 0, i32* %err, align 4, !dbg !809
  call void @llvm.dbg.declare(metadata !{i8* %pad}, metadata !810), !dbg !811
  %arraydecay = getelementptr inbounds [16 x i8]* %iv, i32 0, i32 0, !dbg !812
  call void @llvm.memset.p0i8.i32(i8* %arraydecay, i8 0, i32 16, i32 1, i1 false), !dbg !812
  %0 = load i16** %secret.addr, align 4, !dbg !813
  %arraydecay1 = getelementptr inbounds [16 x i8]* %key, i32 0, i32 0, !dbg !813
  call arm_aapcscc  void @vlong_to_square_block(i16* %0, i8* %arraydecay1), !dbg !813
  %arraydecay2 = getelementptr inbounds [16 x i8]* %key, i32 0, i32 0, !dbg !814
  call arm_aapcscc  void @squareCtsInit(%struct.squareCtsContext* %ctx, i8* %arraydecay2), !dbg !814
  store i8 0, i8* %pad, align 1, !dbg !815
  br label %while.cond, !dbg !816

while.cond:                                       ; preds = %if.end23, %entry
  %arraydecay3 = getelementptr inbounds [1025 x i32]* %buffer, i32 0, i32 0, !dbg !817
  %1 = bitcast i32* %arraydecay3 to i8*, !dbg !817
  %2 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !817
  %call = call arm_aapcscc  i32 @fread(i8* %1, i32 1, i32 4096, %struct._IO_FILE* %2), !dbg !817
  store i32 %call, i32* %n, align 4, !dbg !817
  %tobool = icmp ne i32 %call, 0, !dbg !817
  br i1 %tobool, label %while.body, label %while.end, !dbg !817

while.body:                                       ; preds = %while.cond
  %3 = load i32* %n, align 4, !dbg !818
  %cmp = icmp slt i32 %3, 4096, !dbg !818
  br i1 %cmp, label %if.then, label %if.end13, !dbg !818

if.then:                                          ; preds = %while.body
  store i8 0, i8* %pad, align 1, !dbg !820
  %4 = load i32* %n, align 4, !dbg !822
  %cmp4 = icmp ult i32 %4, 16, !dbg !822
  br i1 %cmp4, label %if.then5, label %if.else, !dbg !822

if.then5:                                         ; preds = %if.then
  %5 = load i32* %n, align 4, !dbg !823
  %sub = sub nsw i32 17, %5, !dbg !823
  %conv = trunc i32 %sub to i8, !dbg !823
  store i8 %conv, i8* %pad, align 1, !dbg !823
  br label %if.end8, !dbg !823

if.else:                                          ; preds = %if.then
  %6 = load i32* %n, align 4, !dbg !824
  %and = and i32 %6, 1, !dbg !824
  %tobool6 = icmp ne i32 %and, 0, !dbg !824
  br i1 %tobool6, label %if.then7, label %if.end, !dbg !824

if.then7:                                         ; preds = %if.else
  store i8 2, i8* %pad, align 1, !dbg !825
  br label %if.end, !dbg !825

if.end:                                           ; preds = %if.then7, %if.else
  br label %if.end8

if.end8:                                          ; preds = %if.end, %if.then5
  %7 = load i32* %n, align 4, !dbg !826
  %arraydecay9 = getelementptr inbounds [1025 x i32]* %buffer, i32 0, i32 0, !dbg !826
  %8 = bitcast i32* %arraydecay9 to i8*, !dbg !826
  %add.ptr = getelementptr inbounds i8* %8, i32 %7, !dbg !826
  %9 = load i8* %pad, align 1, !dbg !826
  %conv10 = zext i8 %9 to i32, !dbg !826
  %10 = trunc i32 %conv10 to i8, !dbg !826
  %11 = load i8* %pad, align 1, !dbg !826
  %conv11 = zext i8 %11 to i32, !dbg !826
  call void @llvm.memset.p0i8.i32(i8* %add.ptr, i8 %10, i32 %conv11, i32 1, i1 false), !dbg !826
  %12 = load i8* %pad, align 1, !dbg !827
  %conv12 = zext i8 %12 to i32, !dbg !827
  %13 = load i32* %n, align 4, !dbg !827
  %add = add nsw i32 %13, %conv12, !dbg !827
  store i32 %add, i32* %n, align 4, !dbg !827
  br label %if.end13, !dbg !828

if.end13:                                         ; preds = %if.end8, %while.body
  %arraydecay14 = getelementptr inbounds [16 x i8]* %iv, i32 0, i32 0, !dbg !829
  call arm_aapcscc  void @squareCtsSetIV(%struct.squareCtsContext* %ctx, i8* %arraydecay14), !dbg !829
  %arraydecay15 = getelementptr inbounds [16 x i8]* %iv, i32 0, i32 0, !dbg !830
  call arm_aapcscc  void @increment(i8* %arraydecay15), !dbg !830
  %arraydecay16 = getelementptr inbounds [1025 x i32]* %buffer, i32 0, i32 0, !dbg !831
  %14 = bitcast i32* %arraydecay16 to i8*, !dbg !831
  %15 = load i32* %n, align 4, !dbg !831
  call arm_aapcscc  void @squareCtsEncrypt(%struct.squareCtsContext* %ctx, i8* %14, i32 %15), !dbg !831
  call void @llvm.dbg.declare(metadata !{i32* %written}, metadata !832), !dbg !834
  %arraydecay17 = getelementptr inbounds [1025 x i32]* %buffer, i32 0, i32 0, !dbg !835
  %16 = bitcast i32* %arraydecay17 to i8*, !dbg !835
  %17 = load i32* %n, align 4, !dbg !835
  %18 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !835
  %call18 = call arm_aapcscc  i32 @fwritePlus(i8* %16, i32 1, i32 %17, %struct._IO_FILE* %18), !dbg !835
  store i32 %call18, i32* %written, align 4, !dbg !835
  %19 = load i32* %written, align 4, !dbg !836
  %20 = load i32* %n, align 4, !dbg !836
  %cmp19 = icmp ne i32 %19, %20, !dbg !836
  br i1 %cmp19, label %if.then21, label %if.end23, !dbg !836

if.then21:                                        ; preds = %if.end13
  %21 = load %struct._IO_FILE** @stderr, align 4, !dbg !837
  %call22 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([41 x i8]* @err_output, i32 0, i32 0), %struct._IO_FILE* %21), !dbg !837
  store i32 1, i32* %err, align 4, !dbg !839
  br label %while.end, !dbg !840

if.end23:                                         ; preds = %if.end13
  br label %while.cond, !dbg !841

while.end:                                        ; preds = %if.then21, %while.cond
  call arm_aapcscc  void @squareCtsFinal(%struct.squareCtsContext* %ctx), !dbg !842
  %arraydecay24 = getelementptr inbounds [16 x i8]* %key, i32 0, i32 0, !dbg !843
  call void @llvm.memset.p0i8.i32(i8* %arraydecay24, i8 0, i32 16, i32 1, i1 false), !dbg !843
  %22 = load i32* %err, align 4, !dbg !844
  ret i32 %22, !dbg !844
}

declare arm_aapcscc void @squareCtsInit(%struct.squareCtsContext*, i8*)

declare arm_aapcscc void @squareCtsSetIV(%struct.squareCtsContext*, i8*)

declare arm_aapcscc void @squareCtsEncrypt(%struct.squareCtsContext*, i8*, i32)

declare arm_aapcscc i32 @fwritePlus(i8*, i32, i32, %struct._IO_FILE*)

declare arm_aapcscc void @squareCtsFinal(%struct.squareCtsContext*)

define arm_aapcscc i32 @sym_decrypt(i16* %secret, %struct._IO_FILE* %f_inp, %struct._IO_FILE* %f_out) nounwind uwtable {
entry:
  %secret.addr = alloca i16*, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %f_out.addr = alloca %struct._IO_FILE*, align 4
  %key = alloca [16 x i8], align 1
  %iv = alloca [16 x i8], align 1
  %b1 = alloca [1025 x i32], align 4
  %b2 = alloca [1025 x i32], align 4
  %buf1 = alloca i8*, align 4
  %buf2 = alloca i8*, align 4
  %ctx = alloca %struct.squareCtsContext, align 4
  %err = alloca i32, align 4
  %n = alloca i32, align 4
  %i = alloca i32, align 4
  %pad = alloca i8, align 1
  %j = alloca i32, align 4
  %written = alloca i32, align 4
  %tmp = alloca i8*, align 4
  store i16* %secret, i16** %secret.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %secret.addr}, metadata !845), !dbg !846
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !847), !dbg !848
  store %struct._IO_FILE* %f_out, %struct._IO_FILE** %f_out.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_out.addr}, metadata !849), !dbg !850
  call void @llvm.dbg.declare(metadata !{[16 x i8]* %key}, metadata !851), !dbg !853
  call void @llvm.dbg.declare(metadata !{[16 x i8]* %iv}, metadata !854), !dbg !855
  call void @llvm.dbg.declare(metadata !{[1025 x i32]* %b1}, metadata !856), !dbg !857
  call void @llvm.dbg.declare(metadata !{[1025 x i32]* %b2}, metadata !858), !dbg !859
  call void @llvm.dbg.declare(metadata !{i8** %buf1}, metadata !860), !dbg !861
  %arraydecay = getelementptr inbounds [1025 x i32]* %b1, i32 0, i32 0, !dbg !862
  %0 = bitcast i32* %arraydecay to i8*, !dbg !862
  store i8* %0, i8** %buf1, align 4, !dbg !862
  call void @llvm.dbg.declare(metadata !{i8** %buf2}, metadata !863), !dbg !864
  %arraydecay1 = getelementptr inbounds [1025 x i32]* %b2, i32 0, i32 0, !dbg !862
  %1 = bitcast i32* %arraydecay1 to i8*, !dbg !862
  store i8* %1, i8** %buf2, align 4, !dbg !862
  call void @llvm.dbg.declare(metadata !{%struct.squareCtsContext* %ctx}, metadata !865), !dbg !866
  call void @llvm.dbg.declare(metadata !{i32* %err}, metadata !867), !dbg !868
  store i32 0, i32* %err, align 4, !dbg !869
  call void @llvm.dbg.declare(metadata !{i32* %n}, metadata !870), !dbg !871
  store i32 0, i32* %n, align 4, !dbg !869
  %arraydecay2 = getelementptr inbounds [16 x i8]* %iv, i32 0, i32 0, !dbg !872
  call void @llvm.memset.p0i8.i32(i8* %arraydecay2, i8 0, i32 16, i32 1, i1 false), !dbg !872
  %2 = load i16** %secret.addr, align 4, !dbg !873
  %arraydecay3 = getelementptr inbounds [16 x i8]* %key, i32 0, i32 0, !dbg !873
  call arm_aapcscc  void @vlong_to_square_block(i16* %2, i8* %arraydecay3), !dbg !873
  %arraydecay4 = getelementptr inbounds [16 x i8]* %key, i32 0, i32 0, !dbg !874
  call arm_aapcscc  void @squareCtsInit(%struct.squareCtsContext* %ctx, i8* %arraydecay4), !dbg !874
  br label %while.body, !dbg !875

while.body:                                       ; preds = %entry, %if.end53
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !876), !dbg !878
  store i32 0, i32* %i, align 4, !dbg !879
  %3 = load i32* %n, align 4, !dbg !880
  %cmp = icmp eq i32 %3, 0, !dbg !880
  br i1 %cmp, label %if.then, label %lor.lhs.false, !dbg !880

lor.lhs.false:                                    ; preds = %while.body
  %4 = load i32* %n, align 4, !dbg !880
  %cmp5 = icmp eq i32 %4, 4096, !dbg !880
  br i1 %cmp5, label %if.then, label %if.end, !dbg !880

if.then:                                          ; preds = %lor.lhs.false, %while.body
  %5 = load i8** %buf1, align 4, !dbg !881
  %6 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !881
  %call = call arm_aapcscc  i32 @freadPlus(i8* %5, i32 1, i32 4096, %struct._IO_FILE* %6), !dbg !881
  store i32 %call, i32* %i, align 4, !dbg !881
  br label %if.end, !dbg !881

if.end:                                           ; preds = %if.then, %lor.lhs.false
  %7 = load i32* %n, align 4, !dbg !882
  %tobool = icmp ne i32 %7, 0, !dbg !882
  br i1 %tobool, label %if.then6, label %if.end49, !dbg !882

if.then6:                                         ; preds = %if.end
  %8 = load i32* %n, align 4, !dbg !883
  %cmp7 = icmp ult i32 %8, 16, !dbg !883
  br i1 %cmp7, label %if.then8, label %if.end10, !dbg !883

if.then8:                                         ; preds = %if.then6
  br label %decrypt_error, !dbg !885

decrypt_error:                                    ; preds = %if.then39, %if.then26, %if.then8
  %9 = load %struct._IO_FILE** @stderr, align 4, !dbg !887
  %call9 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([22 x i8]* @err_decrypt, i32 0, i32 0), %struct._IO_FILE* %9), !dbg !887
  store i32 1, i32* %err, align 4, !dbg !888
  br label %while.end, !dbg !889

if.end10:                                         ; preds = %if.then6
  %10 = load i32* %i, align 4, !dbg !890
  %cmp11 = icmp eq i32 %10, 1, !dbg !890
  br i1 %cmp11, label %if.then12, label %if.end14, !dbg !890

if.then12:                                        ; preds = %if.end10
  %11 = load i32* %n, align 4, !dbg !891
  %add = add nsw i32 %11, 1, !dbg !891
  store i32 %add, i32* %n, align 4, !dbg !891
  %12 = load i8** %buf1, align 4, !dbg !893
  %arrayidx = getelementptr inbounds i8* %12, i32 0, !dbg !893
  %13 = load i8* %arrayidx, !dbg !893
  %14 = load i8** %buf2, align 4, !dbg !893
  %arrayidx13 = getelementptr inbounds i8* %14, i32 4096, !dbg !893
  store i8 %13, i8* %arrayidx13, !dbg !893
  store i32 0, i32* %i, align 4, !dbg !894
  br label %if.end14, !dbg !895

if.end14:                                         ; preds = %if.then12, %if.end10
  %arraydecay15 = getelementptr inbounds [16 x i8]* %iv, i32 0, i32 0, !dbg !896
  call arm_aapcscc  void @squareCtsSetIV(%struct.squareCtsContext* %ctx, i8* %arraydecay15), !dbg !896
  %arraydecay16 = getelementptr inbounds [16 x i8]* %iv, i32 0, i32 0, !dbg !897
  call arm_aapcscc  void @increment(i8* %arraydecay16), !dbg !897
  %15 = load i8** %buf2, align 4, !dbg !898
  %16 = load i32* %n, align 4, !dbg !898
  call arm_aapcscc  void @squareCtsDecrypt(%struct.squareCtsContext* %ctx, i8* %15, i32 %16), !dbg !898
  %17 = load i32* %n, align 4, !dbg !899
  %and = and i32 %17, 1, !dbg !899
  %tobool17 = icmp ne i32 %and, 0, !dbg !899
  br i1 %tobool17, label %if.then18, label %if.end42, !dbg !899

if.then18:                                        ; preds = %if.end14
  call void @llvm.dbg.declare(metadata !{i8* %pad}, metadata !900), !dbg !902
  %18 = load i32* %n, align 4, !dbg !903
  %sub = sub nsw i32 %18, 1, !dbg !903
  %19 = load i8** %buf2, align 4, !dbg !903
  %arrayidx19 = getelementptr inbounds i8* %19, i32 %sub, !dbg !903
  %20 = load i8* %arrayidx19, !dbg !903
  store i8 %20, i8* %pad, align 1, !dbg !903
  %21 = load i8* %pad, align 1, !dbg !904
  %conv = zext i8 %21 to i32, !dbg !904
  %cmp20 = icmp slt i32 %conv, 1, !dbg !904
  br i1 %cmp20, label %if.then26, label %lor.lhs.false22, !dbg !904

lor.lhs.false22:                                  ; preds = %if.then18
  %22 = load i8* %pad, align 1, !dbg !904
  %conv23 = zext i8 %22 to i32, !dbg !904
  %cmp24 = icmp ugt i32 %conv23, 16, !dbg !904
  br i1 %cmp24, label %if.then26, label %if.end27, !dbg !904

if.then26:                                        ; preds = %lor.lhs.false22, %if.then18
  br label %decrypt_error, !dbg !905

if.end27:                                         ; preds = %lor.lhs.false22
  %23 = load i8* %pad, align 1, !dbg !906
  %conv28 = zext i8 %23 to i32, !dbg !906
  %24 = load i32* %n, align 4, !dbg !906
  %sub29 = sub nsw i32 %24, %conv28, !dbg !906
  store i32 %sub29, i32* %n, align 4, !dbg !906
  call void @llvm.dbg.declare(metadata !{i32* %j}, metadata !907), !dbg !909
  store i32 0, i32* %j, align 4, !dbg !910
  br label %for.cond, !dbg !910

for.cond:                                         ; preds = %for.inc, %if.end27
  %25 = load i32* %j, align 4, !dbg !910
  %26 = load i8* %pad, align 1, !dbg !910
  %conv30 = zext i8 %26 to i32, !dbg !910
  %cmp31 = icmp slt i32 %25, %conv30, !dbg !910
  br i1 %cmp31, label %for.body, label %for.end, !dbg !910

for.body:                                         ; preds = %for.cond
  %27 = load i32* %n, align 4, !dbg !912
  %28 = load i32* %j, align 4, !dbg !912
  %add33 = add nsw i32 %27, %28, !dbg !912
  %29 = load i8** %buf2, align 4, !dbg !912
  %arrayidx34 = getelementptr inbounds i8* %29, i32 %add33, !dbg !912
  %30 = load i8* %arrayidx34, !dbg !912
  %conv35 = zext i8 %30 to i32, !dbg !912
  %31 = load i8* %pad, align 1, !dbg !912
  %conv36 = zext i8 %31 to i32, !dbg !912
  %cmp37 = icmp ne i32 %conv35, %conv36, !dbg !912
  br i1 %cmp37, label %if.then39, label %if.end40, !dbg !912

if.then39:                                        ; preds = %for.body
  br label %decrypt_error, !dbg !913

if.end40:                                         ; preds = %for.body
  br label %for.inc, !dbg !913

for.inc:                                          ; preds = %if.end40
  %32 = load i32* %j, align 4, !dbg !914
  %add41 = add nsw i32 %32, 1, !dbg !914
  store i32 %add41, i32* %j, align 4, !dbg !914
  br label %for.cond, !dbg !914

for.end:                                          ; preds = %for.cond
  br label %if.end42, !dbg !915

if.end42:                                         ; preds = %for.end, %if.end14
  call void @llvm.dbg.declare(metadata !{i32* %written}, metadata !916), !dbg !918
  %33 = load i8** %buf2, align 4, !dbg !919
  %34 = load i32* %n, align 4, !dbg !919
  %35 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !919
  %call43 = call arm_aapcscc  i32 @fwrite(i8* %33, i32 1, i32 %34, %struct._IO_FILE* %35), !dbg !919
  store i32 %call43, i32* %written, align 4, !dbg !919
  %36 = load i32* %written, align 4, !dbg !920
  %37 = load i32* %n, align 4, !dbg !920
  %cmp44 = icmp ne i32 %36, %37, !dbg !920
  br i1 %cmp44, label %if.then46, label %if.end48, !dbg !920

if.then46:                                        ; preds = %if.end42
  %38 = load %struct._IO_FILE** @stderr, align 4, !dbg !921
  %call47 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([41 x i8]* @err_output, i32 0, i32 0), %struct._IO_FILE* %38), !dbg !921
  store i32 1, i32* %err, align 4, !dbg !923
  br label %while.end, !dbg !924

if.end48:                                         ; preds = %if.end42
  br label %if.end49, !dbg !925

if.end49:                                         ; preds = %if.end48, %if.end
  %39 = load i32* %i, align 4, !dbg !926
  %cmp50 = icmp eq i32 %39, 0, !dbg !926
  br i1 %cmp50, label %if.then52, label %if.end53, !dbg !926

if.then52:                                        ; preds = %if.end49
  br label %while.end, !dbg !927

if.end53:                                         ; preds = %if.end49
  call void @llvm.dbg.declare(metadata !{i8** %tmp}, metadata !928), !dbg !930
  %40 = load i8** %buf1, align 4, !dbg !931
  store i8* %40, i8** %tmp, align 4, !dbg !931
  %41 = load i8** %buf2, align 4, !dbg !932
  store i8* %41, i8** %buf1, align 4, !dbg !932
  %42 = load i8** %tmp, align 4, !dbg !933
  store i8* %42, i8** %buf2, align 4, !dbg !933
  %43 = load i32* %i, align 4, !dbg !934
  store i32 %43, i32* %n, align 4, !dbg !934
  br label %while.body, !dbg !935

while.end:                                        ; preds = %if.then52, %if.then46, %decrypt_error
  %arraydecay54 = getelementptr inbounds [16 x i8]* %key, i32 0, i32 0, !dbg !936
  call void @llvm.memset.p0i8.i32(i8* %arraydecay54, i8 0, i32 16, i32 1, i1 false), !dbg !936
  call arm_aapcscc  void @squareCtsFinal(%struct.squareCtsContext* %ctx), !dbg !937
  %44 = load i32* %err, align 4, !dbg !938
  ret i32 %44, !dbg !938
}

declare arm_aapcscc void @squareCtsDecrypt(%struct.squareCtsContext*, i8*, i32)

declare arm_aapcscc i32 @fwrite(i8*, i32, i32, %struct._IO_FILE*)

define arm_aapcscc i32 @do_operation(%struct._IO_FILE* %f_key, %struct._IO_FILE* %f_inp, %struct._IO_FILE* %f_out, %struct._IO_FILE* %f_sec, i32 %operation) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %f_key.addr = alloca %struct._IO_FILE*, align 4
  %f_inp.addr = alloca %struct._IO_FILE*, align 4
  %f_out.addr = alloca %struct._IO_FILE*, align 4
  %f_sec.addr = alloca %struct._IO_FILE*, align 4
  %operation.addr = alloca i32, align 4
  %p = alloca %struct.prng, align 4
  %pub = alloca [19 x i16], align 2
  %secret = alloca [19 x i16], align 2
  %session = alloca [19 x i16], align 2
  %mac = alloca [19 x i16], align 2
  %msg = alloca [19 x i16], align 2
  %sig = alloca %struct.cpPair, align 2
  %err = alloca i32, align 4
  store %struct._IO_FILE* %f_key, %struct._IO_FILE** %f_key.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_key.addr}, metadata !939), !dbg !940
  store %struct._IO_FILE* %f_inp, %struct._IO_FILE** %f_inp.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp.addr}, metadata !941), !dbg !942
  store %struct._IO_FILE* %f_out, %struct._IO_FILE** %f_out.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_out.addr}, metadata !943), !dbg !944
  store %struct._IO_FILE* %f_sec, %struct._IO_FILE** %f_sec.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_sec.addr}, metadata !945), !dbg !946
  store i32 %operation, i32* %operation.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %operation.addr}, metadata !947), !dbg !948
  call void @llvm.dbg.declare(metadata !{%struct.prng* %p}, metadata !949), !dbg !951
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %pub}, metadata !952), !dbg !953
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %secret}, metadata !954), !dbg !955
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %session}, metadata !956), !dbg !957
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %mac}, metadata !958), !dbg !959
  call void @llvm.dbg.declare(metadata !{[19 x i16]* %msg}, metadata !960), !dbg !961
  call void @llvm.dbg.declare(metadata !{%struct.cpPair* %sig}, metadata !962), !dbg !969
  call void @llvm.dbg.declare(metadata !{i32* %err}, metadata !970), !dbg !971
  store i32 0, i32* %err, align 4, !dbg !972
  call arm_aapcscc  void @prng_init(%struct.prng* %p), !dbg !973
  %0 = load i32* %operation.addr, align 4, !dbg !974
  %cmp = icmp eq i32 %0, 118, !dbg !974
  br i1 %cmp, label %if.then, label %lor.lhs.false, !dbg !974

lor.lhs.false:                                    ; preds = %entry
  %1 = load i32* %operation.addr, align 4, !dbg !974
  %cmp1 = icmp eq i32 %1, 101, !dbg !974
  br i1 %cmp1, label %if.then, label %lor.lhs.false2, !dbg !974

lor.lhs.false2:                                   ; preds = %lor.lhs.false
  %2 = load i32* %operation.addr, align 4, !dbg !974
  %cmp3 = icmp eq i32 86, %2, !dbg !974
  br i1 %cmp3, label %if.then, label %if.else, !dbg !974

if.then:                                          ; preds = %lor.lhs.false2, %lor.lhs.false, %entry
  %3 = load %struct._IO_FILE** %f_key.addr, align 4, !dbg !975
  %arraydecay = getelementptr inbounds [19 x i16]* %pub, i32 0, i32 0, !dbg !975
  call arm_aapcscc  void @get_vlong(%struct._IO_FILE* %3, i16* %arraydecay), !dbg !975
  %4 = load i32* %operation.addr, align 4, !dbg !977
  %cmp4 = icmp eq i32 %4, 101, !dbg !977
  br i1 %cmp4, label %if.then5, label %if.end7, !dbg !977

if.then5:                                         ; preds = %if.then
  %5 = load %struct._IO_FILE** %f_sec.addr, align 4, !dbg !978
  %tobool = icmp ne %struct._IO_FILE* %5, null, !dbg !978
  br i1 %tobool, label %if.then6, label %if.end, !dbg !978

if.then6:                                         ; preds = %if.then5
  %6 = load %struct._IO_FILE** %f_sec.addr, align 4, !dbg !980
  call arm_aapcscc  void @prng_set_secret(%struct.prng* %p, %struct._IO_FILE* %6), !dbg !980
  br label %if.end, !dbg !980

if.end:                                           ; preds = %if.then6, %if.then5
  %7 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !981
  call arm_aapcscc  void @prng_set_mac(%struct.prng* %p, %struct._IO_FILE* %7, i32 1), !dbg !981
  br label %if.end7, !dbg !982

if.end7:                                          ; preds = %if.end, %if.then
  br label %if.end17, !dbg !983

if.else:                                          ; preds = %lor.lhs.false2
  %8 = load %struct._IO_FILE** %f_key.addr, align 4, !dbg !984
  call arm_aapcscc  void @setbuf(%struct._IO_FILE* %8, i8* null) nounwind, !dbg !984
  %9 = load %struct._IO_FILE** %f_key.addr, align 4, !dbg !986
  call arm_aapcscc  void @prng_set_secret(%struct.prng* %p, %struct._IO_FILE* %9), !dbg !986
  %10 = load i32* %operation.addr, align 4, !dbg !987
  %cmp8 = icmp eq i32 %10, 69, !dbg !987
  br i1 %cmp8, label %if.then11, label %lor.lhs.false9, !dbg !987

lor.lhs.false9:                                   ; preds = %if.else
  %11 = load i32* %operation.addr, align 4, !dbg !987
  %cmp10 = icmp eq i32 %11, 68, !dbg !987
  br i1 %cmp10, label %if.then11, label %if.else14, !dbg !987

if.then11:                                        ; preds = %lor.lhs.false9, %if.else
  %seed = getelementptr inbounds %struct.prng* %p, i32 0, i32 1, !dbg !988
  %arraydecay12 = getelementptr inbounds [17 x i32]* %seed, i32 0, i32 0, !dbg !988
  %add.ptr = getelementptr inbounds i32* %arraydecay12, i32 1, !dbg !988
  %arraydecay13 = getelementptr inbounds [19 x i16]* %secret, i32 0, i32 0, !dbg !988
  call arm_aapcscc  void @hash_to_vlong(i32* %add.ptr, i16* %arraydecay13), !dbg !988
  br label %if.end16, !dbg !988

if.else14:                                        ; preds = %lor.lhs.false9
  %arraydecay15 = getelementptr inbounds [19 x i16]* %secret, i32 0, i32 0, !dbg !989
  call arm_aapcscc  void @prng_to_vlong(%struct.prng* %p, i16* %arraydecay15), !dbg !989
  br label %if.end16

if.end16:                                         ; preds = %if.else14, %if.then11
  br label %if.end17

if.end17:                                         ; preds = %if.end16, %if.end7
  %12 = load i32* %operation.addr, align 4, !dbg !990
  %cmp18 = icmp eq i32 %12, 115, !dbg !990
  br i1 %cmp18, label %if.then21, label %lor.lhs.false19, !dbg !990

lor.lhs.false19:                                  ; preds = %if.end17
  %13 = load i32* %operation.addr, align 4, !dbg !990
  %cmp20 = icmp eq i32 %13, 118, !dbg !990
  br i1 %cmp20, label %if.then21, label %if.end27, !dbg !990

if.then21:                                        ; preds = %lor.lhs.false19, %if.end17
  %14 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !991
  call arm_aapcscc  void @prng_set_mac(%struct.prng* %p, %struct._IO_FILE* %14, i32 2), !dbg !991
  %seed22 = getelementptr inbounds %struct.prng* %p, i32 0, i32 1, !dbg !993
  %arraydecay23 = getelementptr inbounds [17 x i32]* %seed22, i32 0, i32 0, !dbg !993
  %add.ptr24 = getelementptr inbounds i32* %arraydecay23, i32 1, !dbg !993
  %add.ptr25 = getelementptr inbounds i32* %add.ptr24, i32 5, !dbg !993
  %arraydecay26 = getelementptr inbounds [19 x i16]* %mac, i32 0, i32 0, !dbg !993
  call arm_aapcscc  void @hash_to_vlong(i32* %add.ptr25, i16* %arraydecay26), !dbg !993
  br label %if.end27, !dbg !994

if.end27:                                         ; preds = %if.then21, %lor.lhs.false19
  %15 = load i32* %operation.addr, align 4, !dbg !995
  %cmp28 = icmp eq i32 83, %15, !dbg !995
  br i1 %cmp28, label %if.then29, label %if.end35, !dbg !995

if.then29:                                        ; preds = %if.end27
  %16 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !996
  %17 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !996
  call arm_aapcscc  void @clearsign(%struct.prng* %p, %struct._IO_FILE* %16, %struct._IO_FILE* %17), !dbg !996
  %seed30 = getelementptr inbounds %struct.prng* %p, i32 0, i32 1, !dbg !998
  %arraydecay31 = getelementptr inbounds [17 x i32]* %seed30, i32 0, i32 0, !dbg !998
  %add.ptr32 = getelementptr inbounds i32* %arraydecay31, i32 1, !dbg !998
  %add.ptr33 = getelementptr inbounds i32* %add.ptr32, i32 5, !dbg !998
  %arraydecay34 = getelementptr inbounds [19 x i16]* %mac, i32 0, i32 0, !dbg !998
  call arm_aapcscc  void @hash_to_vlong(i32* %add.ptr33, i16* %arraydecay34), !dbg !998
  br label %if.end35, !dbg !999

if.end35:                                         ; preds = %if.then29, %if.end27
  %18 = load i32* %operation.addr, align 4, !dbg !1000
  %cmp36 = icmp eq i32 86, %18, !dbg !1000
  br i1 %cmp36, label %if.then37, label %if.end46, !dbg !1000

if.then37:                                        ; preds = %if.end35
  %19 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1001
  %20 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1001
  %call = call arm_aapcscc  i32 @readsign(%struct.prng* %p, %struct._IO_FILE* %19, %struct._IO_FILE* %20), !dbg !1001
  %tobool38 = icmp ne i32 %call, 0, !dbg !1001
  br i1 %tobool38, label %if.then39, label %if.end40, !dbg !1001

if.then39:                                        ; preds = %if.then37
  store i32 2, i32* %retval, !dbg !1003
  br label %return, !dbg !1003

if.end40:                                         ; preds = %if.then37
  %seed41 = getelementptr inbounds %struct.prng* %p, i32 0, i32 1, !dbg !1004
  %arraydecay42 = getelementptr inbounds [17 x i32]* %seed41, i32 0, i32 0, !dbg !1004
  %add.ptr43 = getelementptr inbounds i32* %arraydecay42, i32 1, !dbg !1004
  %add.ptr44 = getelementptr inbounds i32* %add.ptr43, i32 5, !dbg !1004
  %arraydecay45 = getelementptr inbounds [19 x i16]* %mac, i32 0, i32 0, !dbg !1004
  call arm_aapcscc  void @hash_to_vlong(i32* %add.ptr44, i16* %arraydecay45), !dbg !1004
  br label %if.end46, !dbg !1005

if.end46:                                         ; preds = %if.end40, %if.end35
  %21 = load i32* %operation.addr, align 4, !dbg !1006
  %cmp47 = icmp eq i32 %21, 69, !dbg !1006
  br i1 %cmp47, label %if.then48, label %if.else63, !dbg !1006

if.then48:                                        ; preds = %if.end46
  %22 = load %struct._IO_FILE** @stdout, align 4, !dbg !1007
  %23 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1007
  %cmp49 = icmp eq %struct._IO_FILE* %22, %23, !dbg !1007
  br i1 %cmp49, label %if.then50, label %if.end52, !dbg !1007

if.then50:                                        ; preds = %if.then48
  %24 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1009
  %call51 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([5 x i8]* @begin_clearsign, i32 0, i32 0), %struct._IO_FILE* %24), !dbg !1009
  br label %if.end52, !dbg !1009

if.end52:                                         ; preds = %if.then50, %if.then48
  %arraydecay53 = getelementptr inbounds [19 x i16]* %secret, i32 0, i32 0, !dbg !1010
  %25 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1010
  %26 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1010
  %call54 = call arm_aapcscc  i32 @sym_encrypt(i16* %arraydecay53, %struct._IO_FILE* %25, %struct._IO_FILE* %26), !dbg !1010
  store i32 %call54, i32* %err, align 4, !dbg !1010
  %27 = load %struct._IO_FILE** @stdout, align 4, !dbg !1011
  %28 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1011
  %cmp55 = icmp eq %struct._IO_FILE* %27, %28, !dbg !1011
  br i1 %cmp55, label %if.then56, label %if.end62, !dbg !1011

if.then56:                                        ; preds = %if.end52
  %29 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1012
  %call57 = call arm_aapcscc  i32 @flushArmour(%struct._IO_FILE* %29), !dbg !1012
  %tobool58 = icmp ne i32 %call57, 0, !dbg !1012
  br i1 %tobool58, label %if.end60, label %if.then59, !dbg !1012

if.then59:                                        ; preds = %if.then56
  store i32 3, i32* %retval, !dbg !1014
  br label %return, !dbg !1014

if.end60:                                         ; preds = %if.then56
  %30 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1015
  %call61 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([40 x i8]* @end_ckarmour, i32 0, i32 0), %struct._IO_FILE* %30), !dbg !1015
  br label %if.end62, !dbg !1016

if.end62:                                         ; preds = %if.end60, %if.end52
  br label %if.end174, !dbg !1017

if.else63:                                        ; preds = %if.end46
  %31 = load i32* %operation.addr, align 4, !dbg !1018
  %cmp64 = icmp eq i32 %31, 68, !dbg !1018
  br i1 %cmp64, label %if.then65, label %if.else75, !dbg !1018

if.then65:                                        ; preds = %if.else63
  %32 = load %struct._IO_FILE** @stdin, align 4, !dbg !1019
  %33 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1019
  %cmp66 = icmp eq %struct._IO_FILE* %32, %33, !dbg !1019
  br i1 %cmp66, label %if.then67, label %if.end72, !dbg !1019

if.then67:                                        ; preds = %if.then65
  %34 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1021
  %call68 = call arm_aapcscc  i32 @position(%struct._IO_FILE* %34), !dbg !1021
  %tobool69 = icmp ne i32 %call68, 0, !dbg !1021
  br i1 %tobool69, label %if.end71, label %if.then70, !dbg !1021

if.then70:                                        ; preds = %if.then67
  store i32 2, i32* %retval, !dbg !1022
  br label %return, !dbg !1022

if.end71:                                         ; preds = %if.then67
  br label %if.end72, !dbg !1022

if.end72:                                         ; preds = %if.end71, %if.then65
  %arraydecay73 = getelementptr inbounds [19 x i16]* %secret, i32 0, i32 0, !dbg !1023
  %35 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1023
  %36 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1023
  %call74 = call arm_aapcscc  i32 @sym_decrypt(i16* %arraydecay73, %struct._IO_FILE* %35, %struct._IO_FILE* %36), !dbg !1023
  store i32 %call74, i32* %err, align 4, !dbg !1023
  br label %if.end173, !dbg !1024

if.else75:                                        ; preds = %if.else63
  %call76 = call arm_aapcscc  i32 @gfInit(), !dbg !1025
  %37 = load i32* %operation.addr, align 4, !dbg !1027
  %cmp77 = icmp eq i32 %37, 105, !dbg !1027
  br i1 %cmp77, label %if.then78, label %if.else83, !dbg !1027

if.then78:                                        ; preds = %if.else75
  %arraydecay79 = getelementptr inbounds [19 x i16]* %pub, i32 0, i32 0, !dbg !1028
  %arraydecay80 = getelementptr inbounds [19 x i16]* %secret, i32 0, i32 0, !dbg !1028
  call arm_aapcscc  void @cpMakePublicKey(i16* %arraydecay79, i16* %arraydecay80), !dbg !1028
  %38 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1030
  %call81 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([23 x i8]* @pubkey_magic, i32 0, i32 0), %struct._IO_FILE* %38), !dbg !1030
  %arraydecay82 = getelementptr inbounds [19 x i16]* %pub, i32 0, i32 0, !dbg !1031
  call arm_aapcscc  void @put_vlong(i16* %arraydecay82), !dbg !1031
  br label %if.end172, !dbg !1032

if.else83:                                        ; preds = %if.else75
  %39 = load i32* %operation.addr, align 4, !dbg !1033
  %cmp84 = icmp eq i32 %39, 101, !dbg !1033
  br i1 %cmp84, label %if.then85, label %if.else105, !dbg !1033

if.then85:                                        ; preds = %if.else83
  %40 = load %struct._IO_FILE** @stdout, align 4, !dbg !1034
  %41 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1034
  %cmp86 = icmp eq %struct._IO_FILE* %40, %41, !dbg !1034
  br i1 %cmp86, label %if.then87, label %if.end89, !dbg !1034

if.then87:                                        ; preds = %if.then85
  %42 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1036
  %call88 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([5 x i8]* @begin_clearsign, i32 0, i32 0), %struct._IO_FILE* %42), !dbg !1036
  br label %if.end89, !dbg !1036

if.end89:                                         ; preds = %if.then87, %if.then85
  call arm_aapcscc  void @prng_set_time(%struct.prng* %p), !dbg !1037
  %arraydecay90 = getelementptr inbounds [19 x i16]* %session, i32 0, i32 0, !dbg !1038
  call arm_aapcscc  void @prng_to_vlong(%struct.prng* %p, i16* %arraydecay90), !dbg !1038
  %arraydecay91 = getelementptr inbounds [19 x i16]* %pub, i32 0, i32 0, !dbg !1039
  %arraydecay92 = getelementptr inbounds [19 x i16]* %msg, i32 0, i32 0, !dbg !1039
  %arraydecay93 = getelementptr inbounds [19 x i16]* %session, i32 0, i32 0, !dbg !1039
  call arm_aapcscc  void @cpEncodeSecret(i16* %arraydecay91, i16* %arraydecay92, i16* %arraydecay93), !dbg !1039
  %43 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1040
  %arraydecay94 = getelementptr inbounds [19 x i16]* %msg, i32 0, i32 0, !dbg !1040
  call arm_aapcscc  void @put_binary_vlong(%struct._IO_FILE* %43, i16* %arraydecay94), !dbg !1040
  %arraydecay95 = getelementptr inbounds [19 x i16]* %session, i32 0, i32 0, !dbg !1041
  %44 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1041
  %45 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1041
  %call96 = call arm_aapcscc  i32 @sym_encrypt(i16* %arraydecay95, %struct._IO_FILE* %44, %struct._IO_FILE* %45), !dbg !1041
  store i32 %call96, i32* %err, align 4, !dbg !1041
  %46 = load %struct._IO_FILE** @stdout, align 4, !dbg !1042
  %47 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1042
  %cmp97 = icmp eq %struct._IO_FILE* %46, %47, !dbg !1042
  br i1 %cmp97, label %if.then98, label %if.end104, !dbg !1042

if.then98:                                        ; preds = %if.end89
  %48 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1043
  %call99 = call arm_aapcscc  i32 @flushArmour(%struct._IO_FILE* %48), !dbg !1043
  %tobool100 = icmp ne i32 %call99, 0, !dbg !1043
  br i1 %tobool100, label %if.end102, label %if.then101, !dbg !1043

if.then101:                                       ; preds = %if.then98
  store i32 3, i32* %retval, !dbg !1045
  br label %return, !dbg !1045

if.end102:                                        ; preds = %if.then98
  %49 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1046
  %call103 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([40 x i8]* @end_pkarmour, i32 0, i32 0), %struct._IO_FILE* %49), !dbg !1046
  br label %if.end104, !dbg !1047

if.end104:                                        ; preds = %if.end102, %if.end89
  br label %if.end171, !dbg !1048

if.else105:                                       ; preds = %if.else83
  %50 = load i32* %operation.addr, align 4, !dbg !1049
  %cmp106 = icmp eq i32 %50, 100, !dbg !1049
  br i1 %cmp106, label %if.then107, label %if.else121, !dbg !1049

if.then107:                                       ; preds = %if.else105
  %51 = load %struct._IO_FILE** @stdin, align 4, !dbg !1050
  %52 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1050
  %cmp108 = icmp eq %struct._IO_FILE* %51, %52, !dbg !1050
  br i1 %cmp108, label %if.then109, label %if.end114, !dbg !1050

if.then109:                                       ; preds = %if.then107
  %53 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1052
  %call110 = call arm_aapcscc  i32 @position(%struct._IO_FILE* %53), !dbg !1052
  %tobool111 = icmp ne i32 %call110, 0, !dbg !1052
  br i1 %tobool111, label %if.end113, label %if.then112, !dbg !1052

if.then112:                                       ; preds = %if.then109
  store i32 2, i32* %retval, !dbg !1053
  br label %return, !dbg !1053

if.end113:                                        ; preds = %if.then109
  br label %if.end114, !dbg !1053

if.end114:                                        ; preds = %if.end113, %if.then107
  %54 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1054
  %arraydecay115 = getelementptr inbounds [19 x i16]* %msg, i32 0, i32 0, !dbg !1054
  call arm_aapcscc  void @get_binary_vlong(%struct._IO_FILE* %54, i16* %arraydecay115), !dbg !1054
  %arraydecay116 = getelementptr inbounds [19 x i16]* %secret, i32 0, i32 0, !dbg !1055
  %arraydecay117 = getelementptr inbounds [19 x i16]* %msg, i32 0, i32 0, !dbg !1055
  %arraydecay118 = getelementptr inbounds [19 x i16]* %session, i32 0, i32 0, !dbg !1055
  call arm_aapcscc  void @cpDecodeSecret(i16* %arraydecay116, i16* %arraydecay117, i16* %arraydecay118), !dbg !1055
  %arraydecay119 = getelementptr inbounds [19 x i16]* %session, i32 0, i32 0, !dbg !1056
  %55 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1056
  %56 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1056
  %call120 = call arm_aapcscc  i32 @sym_decrypt(i16* %arraydecay119, %struct._IO_FILE* %55, %struct._IO_FILE* %56), !dbg !1056
  store i32 %call120, i32* %err, align 4, !dbg !1056
  br label %if.end170, !dbg !1057

if.else121:                                       ; preds = %if.else105
  %57 = load i32* %operation.addr, align 4, !dbg !1058
  %cmp122 = icmp eq i32 %57, 115, !dbg !1058
  br i1 %cmp122, label %if.then125, label %lor.lhs.false123, !dbg !1058

lor.lhs.false123:                                 ; preds = %if.else121
  %58 = load i32* %operation.addr, align 4, !dbg !1058
  %cmp124 = icmp eq i32 83, %58, !dbg !1058
  br i1 %cmp124, label %if.then125, label %if.else147, !dbg !1058

if.then125:                                       ; preds = %lor.lhs.false123, %if.else121
  br label %do.body, !dbg !1059

do.body:                                          ; preds = %do.cond, %if.then125
  %arraydecay126 = getelementptr inbounds [19 x i16]* %session, i32 0, i32 0, !dbg !1061
  call arm_aapcscc  void @prng_to_vlong(%struct.prng* %p, i16* %arraydecay126), !dbg !1061
  %arraydecay127 = getelementptr inbounds [19 x i16]* %secret, i32 0, i32 0, !dbg !1063
  %arraydecay128 = getelementptr inbounds [19 x i16]* %session, i32 0, i32 0, !dbg !1063
  %arraydecay129 = getelementptr inbounds [19 x i16]* %mac, i32 0, i32 0, !dbg !1063
  call arm_aapcscc  void @cpSign(i16* %arraydecay127, i16* %arraydecay128, i16* %arraydecay129, %struct.cpPair* %sig), !dbg !1063
  br label %do.cond, !dbg !1064

do.cond:                                          ; preds = %do.body
  %r = getelementptr inbounds %struct.cpPair* %sig, i32 0, i32 0, !dbg !1064
  %arrayidx = getelementptr inbounds [19 x i16]* %r, i32 0, i32 0, !dbg !1064
  %59 = load i16* %arrayidx, align 2, !dbg !1064
  %conv = zext i16 %59 to i32, !dbg !1064
  %cmp130 = icmp eq i32 %conv, 0, !dbg !1064
  br i1 %cmp130, label %do.body, label %do.end, !dbg !1064

do.end:                                           ; preds = %do.cond
  %s = getelementptr inbounds %struct.cpPair* %sig, i32 0, i32 1, !dbg !1065
  %arraydecay132 = getelementptr inbounds [19 x i16]* %s, i32 0, i32 0, !dbg !1065
  call arm_aapcscc  void @put_vlong(i16* %arraydecay132), !dbg !1065
  %60 = load i32* %operation.addr, align 4, !dbg !1066
  %cmp133 = icmp eq i32 83, %60, !dbg !1066
  br i1 %cmp133, label %if.then135, label %if.else137, !dbg !1066

if.then135:                                       ; preds = %do.end
  %61 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1067
  %call136 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([2 x i8]* @.str1, i32 0, i32 0), %struct._IO_FILE* %61), !dbg !1067
  br label %if.end139, !dbg !1067

if.else137:                                       ; preds = %do.end
  %62 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1068
  %call138 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([2 x i8]* @.str2, i32 0, i32 0), %struct._IO_FILE* %62), !dbg !1068
  br label %if.end139

if.end139:                                        ; preds = %if.else137, %if.then135
  %r140 = getelementptr inbounds %struct.cpPair* %sig, i32 0, i32 0, !dbg !1069
  %arraydecay141 = getelementptr inbounds [19 x i16]* %r140, i32 0, i32 0, !dbg !1069
  call arm_aapcscc  void @put_vlong(i16* %arraydecay141), !dbg !1069
  %63 = load i32* %operation.addr, align 4, !dbg !1070
  %cmp142 = icmp eq i32 83, %63, !dbg !1070
  br i1 %cmp142, label %if.then144, label %if.end146, !dbg !1070

if.then144:                                       ; preds = %if.end139
  %64 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1071
  %call145 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([2 x i8]* @.str1, i32 0, i32 0), %struct._IO_FILE* %64), !dbg !1071
  br label %if.end146, !dbg !1071

if.end146:                                        ; preds = %if.then144, %if.end139
  br label %if.end169, !dbg !1072

if.else147:                                       ; preds = %lor.lhs.false123
  %65 = load i32* %operation.addr, align 4, !dbg !1073
  %cmp148 = icmp eq i32 118, %65, !dbg !1073
  br i1 %cmp148, label %if.then150, label %if.else155, !dbg !1073

if.then150:                                       ; preds = %if.else147
  %66 = load %struct._IO_FILE** %f_sec.addr, align 4, !dbg !1075
  %s151 = getelementptr inbounds %struct.cpPair* %sig, i32 0, i32 1, !dbg !1075
  %arraydecay152 = getelementptr inbounds [19 x i16]* %s151, i32 0, i32 0, !dbg !1075
  call arm_aapcscc  void @get_vlong(%struct._IO_FILE* %66, i16* %arraydecay152), !dbg !1075
  %67 = load %struct._IO_FILE** %f_sec.addr, align 4, !dbg !1077
  %r153 = getelementptr inbounds %struct.cpPair* %sig, i32 0, i32 0, !dbg !1077
  %arraydecay154 = getelementptr inbounds [19 x i16]* %r153, i32 0, i32 0, !dbg !1077
  call arm_aapcscc  void @get_vlong(%struct._IO_FILE* %67, i16* %arraydecay154), !dbg !1077
  br label %if.end160, !dbg !1078

if.else155:                                       ; preds = %if.else147
  %68 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1079
  %s156 = getelementptr inbounds %struct.cpPair* %sig, i32 0, i32 1, !dbg !1079
  %arraydecay157 = getelementptr inbounds [19 x i16]* %s156, i32 0, i32 0, !dbg !1079
  call arm_aapcscc  void @get_vlong_a(%struct._IO_FILE* %68, i16* %arraydecay157), !dbg !1079
  %69 = load %struct._IO_FILE** %f_inp.addr, align 4, !dbg !1081
  %r158 = getelementptr inbounds %struct.cpPair* %sig, i32 0, i32 0, !dbg !1081
  %arraydecay159 = getelementptr inbounds [19 x i16]* %r158, i32 0, i32 0, !dbg !1081
  call arm_aapcscc  void @get_vlong_a(%struct._IO_FILE* %69, i16* %arraydecay159), !dbg !1081
  br label %if.end160

if.end160:                                        ; preds = %if.else155, %if.then150
  %arraydecay161 = getelementptr inbounds [19 x i16]* %pub, i32 0, i32 0, !dbg !1082
  %arraydecay162 = getelementptr inbounds [19 x i16]* %mac, i32 0, i32 0, !dbg !1082
  %call163 = call arm_aapcscc  i32 @cpVerify(i16* %arraydecay161, i16* %arraydecay162, %struct.cpPair* %sig), !dbg !1082
  %tobool164 = icmp ne i32 %call163, 0, !dbg !1082
  %lnot = xor i1 %tobool164, true, !dbg !1082
  %lnot.ext = zext i1 %lnot to i32, !dbg !1082
  store i32 %lnot.ext, i32* %err, align 4, !dbg !1082
  %70 = load i32* %err, align 4, !dbg !1083
  %tobool165 = icmp ne i32 %70, 0, !dbg !1083
  br i1 %tobool165, label %if.then166, label %if.end168, !dbg !1083

if.then166:                                       ; preds = %if.end160
  %71 = load %struct._IO_FILE** @stderr, align 4, !dbg !1084
  %call167 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([29 x i8]* @err_signature, i32 0, i32 0), %struct._IO_FILE* %71), !dbg !1084
  br label %if.end168, !dbg !1084

if.end168:                                        ; preds = %if.then166, %if.end160
  br label %if.end169

if.end169:                                        ; preds = %if.end168, %if.end146
  br label %if.end170

if.end170:                                        ; preds = %if.end169, %if.end114
  br label %if.end171

if.end171:                                        ; preds = %if.end170, %if.end104
  br label %if.end172

if.end172:                                        ; preds = %if.end171, %if.then78
  call arm_aapcscc  void @gfQuit(), !dbg !1085
  br label %if.end173

if.end173:                                        ; preds = %if.end172, %if.end72
  br label %if.end174

if.end174:                                        ; preds = %if.end173, %if.end62
  %72 = load %struct._IO_FILE** %f_out.addr, align 4, !dbg !1086
  %call175 = call arm_aapcscc  i32 @fflush(%struct._IO_FILE* %72), !dbg !1086
  call arm_aapcscc  void @prng_init(%struct.prng* %p), !dbg !1087
  %arraydecay176 = getelementptr inbounds [19 x i16]* %secret, i32 0, i32 0, !dbg !1088
  call arm_aapcscc  void @vlClear(i16* %arraydecay176), !dbg !1088
  %arraydecay177 = getelementptr inbounds [19 x i16]* %session, i32 0, i32 0, !dbg !1089
  call arm_aapcscc  void @vlClear(i16* %arraydecay177), !dbg !1089
  %73 = load i32* %err, align 4, !dbg !1090
  store i32 %73, i32* %retval, !dbg !1090
  br label %return, !dbg !1090

return:                                           ; preds = %if.end174, %if.then112, %if.then101, %if.then70, %if.then59, %if.then39
  %74 = load i32* %retval, !dbg !1091
  ret i32 %74, !dbg !1091
}

declare arm_aapcscc void @setbuf(%struct._IO_FILE*, i8*) nounwind

declare arm_aapcscc i32 @flushArmour(%struct._IO_FILE*)

declare arm_aapcscc i32 @gfInit()

declare arm_aapcscc void @cpMakePublicKey(i16*, i16*)

declare arm_aapcscc void @cpEncodeSecret(i16*, i16*, i16*)

declare arm_aapcscc void @cpDecodeSecret(i16*, i16*, i16*)

declare arm_aapcscc void @cpSign(i16*, i16*, i16*, %struct.cpPair*)

declare arm_aapcscc i32 @cpVerify(i16*, i16*, %struct.cpPair*)

declare arm_aapcscc void @gfQuit()

declare arm_aapcscc i32 @fflush(%struct._IO_FILE*)

define arm_aapcscc %struct._IO_FILE* @chkopen(i8* %s, i8* %mode) nounwind uwtable {
entry:
  %s.addr = alloca i8*, align 4
  %mode.addr = alloca i8*, align 4
  %result = alloca %struct._IO_FILE*, align 4
  store i8* %s, i8** %s.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %s.addr}, metadata !1092), !dbg !1093
  store i8* %mode, i8** %mode.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %mode.addr}, metadata !1094), !dbg !1095
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %result}, metadata !1096), !dbg !1098
  %0 = load i8** %s.addr, align 4, !dbg !1099
  %1 = load i8** %mode.addr, align 4, !dbg !1099
  %call = call arm_aapcscc  %struct._IO_FILE* @fopen(i8* %0, i8* %1), !dbg !1099
  store %struct._IO_FILE* %call, %struct._IO_FILE** %result, align 4, !dbg !1099
  %2 = load %struct._IO_FILE** %result, align 4, !dbg !1100
  %tobool = icmp ne %struct._IO_FILE* %2, null, !dbg !1100
  br i1 %tobool, label %if.end, label %if.then, !dbg !1100

if.then:                                          ; preds = %entry
  %3 = load %struct._IO_FILE** @stderr, align 4, !dbg !1101
  %call1 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([32 x i8]* @err_open_failed, i32 0, i32 0), %struct._IO_FILE* %3), !dbg !1101
  %4 = load i8** %s.addr, align 4, !dbg !1103
  %5 = load %struct._IO_FILE** @stderr, align 4, !dbg !1103
  %call2 = call arm_aapcscc  i32 @fputs(i8* %4, %struct._IO_FILE* %5), !dbg !1103
  br label %if.end, !dbg !1104

if.end:                                           ; preds = %if.then, %entry
  %6 = load %struct._IO_FILE** %result, align 4, !dbg !1105
  ret %struct._IO_FILE* %6, !dbg !1105
}

declare arm_aapcscc %struct._IO_FILE* @fopen(i8*, i8*)

define arm_aapcscc void @burn_stack() nounwind uwtable {
entry:
  %x = alloca [20000 x i8], align 1
  call void @llvm.dbg.declare(metadata !{[20000 x i8]* %x}, metadata !1106), !dbg !1111
  %arraydecay = getelementptr inbounds [20000 x i8]* %x, i32 0, i32 0, !dbg !1112
  call void @llvm.memset.p0i8.i32(i8* %arraydecay, i8 0, i32 20000, i32 1, i1 false), !dbg !1112
  ret void, !dbg !1113
}

define arm_aapcscc i32 @main(i32 %argc, i8** %argv) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %argc.addr = alloca i32, align 4
  %argv.addr = alloca i8**, align 4
  %err = alloca i32, align 4
  %operation = alloca i32, align 4
  %filter = alloca i32, align 4
  %expect = alloca i32, align 4
  %arg_ix = alloca i32, align 4
  %f_key = alloca %struct._IO_FILE*, align 4
  %f_inp = alloca %struct._IO_FILE*, align 4
  %f_out = alloca %struct._IO_FILE*, align 4
  %f_sec = alloca %struct._IO_FILE*, align 4
  %openForRead = alloca [3 x i8], align 1
  %openForWrite = alloca [3 x i8], align 1
  %openKey = alloca [3 x i8], align 1
  %i = alloca i32, align 4
  %isPub = alloca i32, align 4
  store i32 0, i32* %retval
  store i32 %argc, i32* %argc.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %argc.addr}, metadata !1114), !dbg !1115
  store i8** %argv, i8*** %argv.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8*** %argv.addr}, metadata !1116), !dbg !1118
  call void @llvm.dbg.declare(metadata !{i32* %err}, metadata !1119), !dbg !1121
  call void @llvm.dbg.declare(metadata !{i32* %operation}, metadata !1122), !dbg !1123
  call void @llvm.dbg.declare(metadata !{i32* %filter}, metadata !1124), !dbg !1125
  store i32 0, i32* %filter, align 4, !dbg !1126
  call void @llvm.dbg.declare(metadata !{i32* %expect}, metadata !1127), !dbg !1128
  call void @llvm.dbg.declare(metadata !{i32* %arg_ix}, metadata !1129), !dbg !1130
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_key}, metadata !1131), !dbg !1132
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_inp}, metadata !1133), !dbg !1134
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_out}, metadata !1135), !dbg !1136
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %f_sec}, metadata !1137), !dbg !1138
  call void @llvm.dbg.declare(metadata !{[3 x i8]* %openForRead}, metadata !1139), !dbg !1143
  %0 = bitcast [3 x i8]* %openForRead to i8*, !dbg !1144
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %0, i8* getelementptr inbounds ([3 x i8]* @main.openForRead, i32 0, i32 0), i32 3, i32 1, i1 false), !dbg !1144
  call void @llvm.dbg.declare(metadata !{[3 x i8]* %openForWrite}, metadata !1145), !dbg !1146
  %1 = bitcast [3 x i8]* %openForWrite to i8*, !dbg !1147
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %1, i8* getelementptr inbounds ([3 x i8]* @main.openForWrite, i32 0, i32 0), i32 3, i32 1, i1 false), !dbg !1147
  call void @llvm.dbg.declare(metadata !{[3 x i8]* %openKey}, metadata !1148), !dbg !1149
  %2 = bitcast [3 x i8]* %openKey to i8*, !dbg !1150
  call void @llvm.memcpy.p0i8.p0i8.i32(i8* %2, i8* getelementptr inbounds ([3 x i8]* @main.openKey, i32 0, i32 0), i32 3, i32 1, i1 false), !dbg !1150
  %3 = load i32* %argc.addr, align 4, !dbg !1151
  %cmp = icmp slt i32 %3, 2, !dbg !1151
  br i1 %cmp, label %if.then, label %lor.lhs.false, !dbg !1151

lor.lhs.false:                                    ; preds = %entry
  %4 = load i8*** %argv.addr, align 4, !dbg !1151
  %arrayidx = getelementptr inbounds i8** %4, i32 1, !dbg !1151
  %5 = load i8** %arrayidx, !dbg !1151
  %arrayidx1 = getelementptr inbounds i8* %5, i32 0, !dbg !1151
  %6 = load i8* %arrayidx1, !dbg !1151
  %conv = sext i8 %6 to i32, !dbg !1151
  %cmp2 = icmp ne i32 %conv, 45, !dbg !1151
  br i1 %cmp2, label %if.then, label %if.end5, !dbg !1151

if.then:                                          ; preds = %lor.lhs.false, %entry
  br label %error, !dbg !1152

error:                                            ; preds = %if.then99, %if.then35, %if.then28, %if.then
  %7 = load i32* %filter, align 4, !dbg !1154
  %tobool = icmp ne i32 %7, 0, !dbg !1154
  br i1 %tobool, label %if.then4, label %if.end, !dbg !1154

if.then4:                                         ; preds = %error
  br label %filterError, !dbg !1155

if.end:                                           ; preds = %error
  %8 = load %struct._IO_FILE** @stderr, align 4, !dbg !1156
  %call = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([374 x i8]* @manual, i32 0, i32 0), %struct._IO_FILE* %8), !dbg !1156
  store i32 1, i32* %retval, !dbg !1157
  br label %return, !dbg !1157

if.end5:                                          ; preds = %lor.lhs.false
  %9 = load i8*** %argv.addr, align 4, !dbg !1158
  %arrayidx6 = getelementptr inbounds i8** %9, i32 1, !dbg !1158
  %10 = load i8** %arrayidx6, !dbg !1158
  %arrayidx7 = getelementptr inbounds i8* %10, i32 1, !dbg !1158
  %11 = load i8* %arrayidx7, !dbg !1158
  %conv8 = sext i8 %11 to i32, !dbg !1158
  store i32 %conv8, i32* %operation, align 4, !dbg !1158
  %12 = load i32* %operation, align 4, !dbg !1159
  %cmp9 = icmp eq i32 102, %12, !dbg !1159
  br i1 %cmp9, label %if.then11, label %if.else, !dbg !1159

if.then11:                                        ; preds = %if.end5
  store i32 1, i32* %filter, align 4, !dbg !1160
  %13 = load i8*** %argv.addr, align 4, !dbg !1162
  %arrayidx12 = getelementptr inbounds i8** %13, i32 1, !dbg !1162
  %14 = load i8** %arrayidx12, !dbg !1162
  %arrayidx13 = getelementptr inbounds i8* %14, i32 2, !dbg !1162
  %15 = load i8* %arrayidx13, !dbg !1162
  %conv14 = sext i8 %15 to i32, !dbg !1162
  store i32 %conv14, i32* %operation, align 4, !dbg !1162
  %16 = load i8*** %argv.addr, align 4, !dbg !1163
  %arrayidx15 = getelementptr inbounds i8** %16, i32 1, !dbg !1163
  %17 = load i8** %arrayidx15, !dbg !1163
  %arrayidx16 = getelementptr inbounds i8* %17, i32 2, !dbg !1163
  %18 = load i8* %arrayidx16, !dbg !1163
  %conv17 = sext i8 %18 to i32, !dbg !1163
  %cmp18 = icmp eq i32 0, %conv17, !dbg !1163
  br i1 %cmp18, label %if.then20, label %if.end22, !dbg !1163

if.then20:                                        ; preds = %if.then11
  br label %filterError, !dbg !1164

filterError:                                      ; preds = %if.then20, %if.then4
  %19 = load %struct._IO_FILE** @stderr, align 4, !dbg !1166
  %call21 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([284 x i8]* @filterManual, i32 0, i32 0), %struct._IO_FILE* %19), !dbg !1166
  store i32 1, i32* %retval, !dbg !1167
  br label %return, !dbg !1167

if.end22:                                         ; preds = %if.then11
  %20 = load i8*** %argv.addr, align 4, !dbg !1168
  %arrayidx23 = getelementptr inbounds i8** %20, i32 1, !dbg !1168
  %21 = load i8** %arrayidx23, !dbg !1168
  %arrayidx24 = getelementptr inbounds i8* %21, i32 3, !dbg !1168
  %22 = load i8* %arrayidx24, !dbg !1168
  %conv25 = sext i8 %22 to i32, !dbg !1168
  %cmp26 = icmp ne i32 0, %conv25, !dbg !1168
  br i1 %cmp26, label %if.then28, label %if.end29, !dbg !1168

if.then28:                                        ; preds = %if.end22
  br label %error, !dbg !1169

if.end29:                                         ; preds = %if.end22
  br label %if.end37, !dbg !1170

if.else:                                          ; preds = %if.end5
  %23 = load i8*** %argv.addr, align 4, !dbg !1171
  %arrayidx30 = getelementptr inbounds i8** %23, i32 1, !dbg !1171
  %24 = load i8** %arrayidx30, !dbg !1171
  %arrayidx31 = getelementptr inbounds i8* %24, i32 2, !dbg !1171
  %25 = load i8* %arrayidx31, !dbg !1171
  %conv32 = sext i8 %25 to i32, !dbg !1171
  %cmp33 = icmp ne i32 %conv32, 0, !dbg !1171
  br i1 %cmp33, label %if.then35, label %if.end36, !dbg !1171

if.then35:                                        ; preds = %if.else
  br label %error, !dbg !1172

if.end36:                                         ; preds = %if.else
  br label %if.end37

if.end37:                                         ; preds = %if.end36, %if.end29
  store i32 0, i32* %expect, align 4, !dbg !1173
  %26 = load i32* %filter, align 4, !dbg !1174
  %tobool38 = icmp ne i32 %26, 0, !dbg !1174
  br i1 %tobool38, label %if.else74, label %if.then39, !dbg !1174

if.then39:                                        ; preds = %if.end37
  %27 = load i32* %operation, align 4, !dbg !1175
  %cmp40 = icmp eq i32 %27, 105, !dbg !1175
  br i1 %cmp40, label %if.then42, label %if.else43, !dbg !1175

if.then42:                                        ; preds = %if.then39
  store i32 2, i32* %expect, align 4, !dbg !1177
  br label %if.end73, !dbg !1177

if.else43:                                        ; preds = %if.then39
  %28 = load i32* %operation, align 4, !dbg !1178
  %cmp44 = icmp eq i32 %28, 115, !dbg !1178
  br i1 %cmp44, label %if.then49, label %lor.lhs.false46, !dbg !1178

lor.lhs.false46:                                  ; preds = %if.else43
  %29 = load i32* %operation, align 4, !dbg !1178
  %cmp47 = icmp eq i32 83, %29, !dbg !1178
  br i1 %cmp47, label %if.then49, label %if.else50, !dbg !1178

if.then49:                                        ; preds = %lor.lhs.false46, %if.else43
  store i32 3, i32* %expect, align 4, !dbg !1179
  br label %if.end72, !dbg !1179

if.else50:                                        ; preds = %lor.lhs.false46
  %30 = load i32* %operation, align 4, !dbg !1180
  %cmp51 = icmp eq i32 %30, 100, !dbg !1180
  br i1 %cmp51, label %if.then65, label %lor.lhs.false53, !dbg !1180

lor.lhs.false53:                                  ; preds = %if.else50
  %31 = load i32* %operation, align 4, !dbg !1180
  %cmp54 = icmp eq i32 %31, 118, !dbg !1180
  br i1 %cmp54, label %if.then65, label %lor.lhs.false56, !dbg !1180

lor.lhs.false56:                                  ; preds = %lor.lhs.false53
  %32 = load i32* %operation, align 4, !dbg !1180
  %cmp57 = icmp eq i32 86, %32, !dbg !1180
  br i1 %cmp57, label %if.then65, label %lor.lhs.false59, !dbg !1180

lor.lhs.false59:                                  ; preds = %lor.lhs.false56
  %33 = load i32* %operation, align 4, !dbg !1180
  %cmp60 = icmp eq i32 %33, 68, !dbg !1180
  br i1 %cmp60, label %if.then65, label %lor.lhs.false62, !dbg !1180

lor.lhs.false62:                                  ; preds = %lor.lhs.false59
  %34 = load i32* %operation, align 4, !dbg !1180
  %cmp63 = icmp eq i32 %34, 69, !dbg !1180
  br i1 %cmp63, label %if.then65, label %if.else66, !dbg !1180

if.then65:                                        ; preds = %lor.lhs.false62, %lor.lhs.false59, %lor.lhs.false56, %lor.lhs.false53, %if.else50
  store i32 4, i32* %expect, align 4, !dbg !1181
  br label %if.end71, !dbg !1181

if.else66:                                        ; preds = %lor.lhs.false62
  %35 = load i32* %operation, align 4, !dbg !1182
  %cmp67 = icmp eq i32 %35, 101, !dbg !1182
  br i1 %cmp67, label %if.then69, label %if.end70, !dbg !1182

if.then69:                                        ; preds = %if.else66
  store i32 5, i32* %expect, align 4, !dbg !1183
  br label %if.end70, !dbg !1183

if.end70:                                         ; preds = %if.then69, %if.else66
  br label %if.end71

if.end71:                                         ; preds = %if.end70, %if.then65
  br label %if.end72

if.end72:                                         ; preds = %if.end71, %if.then49
  br label %if.end73

if.end73:                                         ; preds = %if.end72, %if.then42
  br label %if.end96, !dbg !1184

if.else74:                                        ; preds = %if.end37
  %36 = load i32* %operation, align 4, !dbg !1185
  %cmp75 = icmp eq i32 86, %36, !dbg !1185
  br i1 %cmp75, label %if.then89, label %lor.lhs.false77, !dbg !1185

lor.lhs.false77:                                  ; preds = %if.else74
  %37 = load i32* %operation, align 4, !dbg !1185
  %cmp78 = icmp eq i32 83, %37, !dbg !1185
  br i1 %cmp78, label %if.then89, label %lor.lhs.false80, !dbg !1185

lor.lhs.false80:                                  ; preds = %lor.lhs.false77
  %38 = load i32* %operation, align 4, !dbg !1185
  %cmp81 = icmp eq i32 69, %38, !dbg !1185
  br i1 %cmp81, label %if.then89, label %lor.lhs.false83, !dbg !1185

lor.lhs.false83:                                  ; preds = %lor.lhs.false80
  %39 = load i32* %operation, align 4, !dbg !1185
  %cmp84 = icmp eq i32 68, %39, !dbg !1185
  br i1 %cmp84, label %if.then89, label %lor.lhs.false86, !dbg !1185

lor.lhs.false86:                                  ; preds = %lor.lhs.false83
  %40 = load i32* %operation, align 4, !dbg !1185
  %cmp87 = icmp eq i32 100, %40, !dbg !1185
  br i1 %cmp87, label %if.then89, label %if.else90, !dbg !1185

if.then89:                                        ; preds = %lor.lhs.false86, %lor.lhs.false83, %lor.lhs.false80, %lor.lhs.false77, %if.else74
  store i32 3, i32* %expect, align 4, !dbg !1187
  br label %if.end95, !dbg !1187

if.else90:                                        ; preds = %lor.lhs.false86
  %41 = load i32* %operation, align 4, !dbg !1188
  %cmp91 = icmp eq i32 101, %41, !dbg !1188
  br i1 %cmp91, label %if.then93, label %if.end94, !dbg !1188

if.then93:                                        ; preds = %if.else90
  store i32 4, i32* %expect, align 4, !dbg !1189
  br label %if.end94, !dbg !1189

if.end94:                                         ; preds = %if.then93, %if.else90
  br label %if.end95

if.end95:                                         ; preds = %if.end94, %if.then89
  br label %if.end96

if.end96:                                         ; preds = %if.end95, %if.end73
  %42 = load i32* %argc.addr, align 4, !dbg !1190
  %43 = load i32* %expect, align 4, !dbg !1190
  %cmp97 = icmp ne i32 %42, %43, !dbg !1190
  br i1 %cmp97, label %if.then99, label %if.end100, !dbg !1190

if.then99:                                        ; preds = %if.end96
  br label %error, !dbg !1191

if.end100:                                        ; preds = %if.end96
  store i32 2, i32* %arg_ix, align 4, !dbg !1192
  %44 = load %struct._IO_FILE** @stdin, align 4, !dbg !1193
  store %struct._IO_FILE* %44, %struct._IO_FILE** %f_key, align 4, !dbg !1193
  %45 = load i32* %operation, align 4, !dbg !1194
  %cmp101 = icmp eq i32 %45, 101, !dbg !1194
  br i1 %cmp101, label %if.then111, label %lor.lhs.false103, !dbg !1194

lor.lhs.false103:                                 ; preds = %if.end100
  %46 = load i32* %operation, align 4, !dbg !1194
  %cmp104 = icmp eq i32 %46, 118, !dbg !1194
  br i1 %cmp104, label %if.then111, label %lor.lhs.false106, !dbg !1194

lor.lhs.false106:                                 ; preds = %lor.lhs.false103
  %47 = load i32* %operation, align 4, !dbg !1194
  %cmp107 = icmp eq i32 86, %47, !dbg !1194
  br i1 %cmp107, label %if.then111, label %lor.lhs.false109, !dbg !1194

lor.lhs.false109:                                 ; preds = %lor.lhs.false106
  %48 = load i32* %filter, align 4, !dbg !1194
  %tobool110 = icmp ne i32 %48, 0, !dbg !1194
  br i1 %tobool110, label %if.then111, label %if.end147, !dbg !1194

if.then111:                                       ; preds = %lor.lhs.false109, %lor.lhs.false106, %lor.lhs.false103, %if.end100
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !1195), !dbg !1197
  call void @llvm.dbg.declare(metadata !{i32* %isPub}, metadata !1198), !dbg !1199
  store i32 1, i32* %isPub, align 4, !dbg !1200
  %49 = load i32* %operation, align 4, !dbg !1201
  %cmp112 = icmp eq i32 83, %49, !dbg !1201
  br i1 %cmp112, label %if.then117, label %lor.lhs.false114, !dbg !1201

lor.lhs.false114:                                 ; preds = %if.then111
  %50 = load i32* %operation, align 4, !dbg !1201
  %cmp115 = icmp eq i32 100, %50, !dbg !1201
  br i1 %cmp115, label %if.then117, label %if.end119, !dbg !1201

if.then117:                                       ; preds = %lor.lhs.false114, %if.then111
  %arrayidx118 = getelementptr inbounds [3 x i8]* %openKey, i32 0, i32 1, !dbg !1202
  store i8 0, i8* %arrayidx118, align 1, !dbg !1202
  br label %if.end119, !dbg !1202

if.end119:                                        ; preds = %if.then117, %lor.lhs.false114
  %51 = load i32* %arg_ix, align 4, !dbg !1203
  %inc = add i32 %51, 1, !dbg !1203
  store i32 %inc, i32* %arg_ix, align 4, !dbg !1203
  %52 = load i8*** %argv.addr, align 4, !dbg !1203
  %arrayidx120 = getelementptr inbounds i8** %52, i32 %51, !dbg !1203
  %53 = load i8** %arrayidx120, !dbg !1203
  %arraydecay = getelementptr inbounds [3 x i8]* %openKey, i32 0, i32 0, !dbg !1203
  %call121 = call arm_aapcscc  %struct._IO_FILE* @chkopen(i8* %53, i8* %arraydecay), !dbg !1203
  store %struct._IO_FILE* %call121, %struct._IO_FILE** %f_key, align 4, !dbg !1203
  %54 = load %struct._IO_FILE** %f_key, align 4, !dbg !1204
  %tobool122 = icmp ne %struct._IO_FILE* %54, null, !dbg !1204
  br i1 %tobool122, label %if.end124, label %if.then123, !dbg !1204

if.then123:                                       ; preds = %if.end119
  store i32 1, i32* %retval, !dbg !1205
  br label %return, !dbg !1205

if.end124:                                        ; preds = %if.end119
  %55 = load i32* %filter, align 4, !dbg !1206
  %tobool125 = icmp ne i32 %55, 0, !dbg !1206
  br i1 %tobool125, label %land.lhs.true, label %if.end132, !dbg !1206

land.lhs.true:                                    ; preds = %if.end124
  %56 = load i32* %operation, align 4, !dbg !1206
  %cmp126 = icmp ne i32 101, %56, !dbg !1206
  br i1 %cmp126, label %land.lhs.true128, label %if.end132, !dbg !1206

land.lhs.true128:                                 ; preds = %land.lhs.true
  %57 = load i32* %operation, align 4, !dbg !1206
  %cmp129 = icmp ne i32 86, %57, !dbg !1206
  br i1 %cmp129, label %if.then131, label %if.end132, !dbg !1206

if.then131:                                       ; preds = %land.lhs.true128
  store i32 0, i32* %isPub, align 4, !dbg !1207
  br label %if.end132, !dbg !1207

if.end132:                                        ; preds = %if.then131, %land.lhs.true128, %land.lhs.true, %if.end124
  store i32 0, i32* %i, align 4, !dbg !1208
  br label %for.cond, !dbg !1208

for.cond:                                         ; preds = %for.inc, %if.end132
  %58 = load i32* %isPub, align 4, !dbg !1208
  %tobool133 = icmp ne i32 %58, 0, !dbg !1208
  br i1 %tobool133, label %land.rhs, label %land.end, !dbg !1208

land.rhs:                                         ; preds = %for.cond
  %59 = load i32* %i, align 4, !dbg !1208
  %arrayidx134 = getelementptr inbounds [23 x i8]* @pubkey_magic, i32 0, i32 %59, !dbg !1208
  %60 = load i8* %arrayidx134, align 1, !dbg !1208
  %conv135 = sext i8 %60 to i32, !dbg !1208
  %tobool136 = icmp ne i32 %conv135, 0, !dbg !1208
  br label %land.end

land.end:                                         ; preds = %land.rhs, %for.cond
  %61 = phi i1 [ false, %for.cond ], [ %tobool136, %land.rhs ]
  br i1 %61, label %for.body, label %for.end

for.body:                                         ; preds = %land.end
  %62 = load %struct._IO_FILE** %f_key, align 4, !dbg !1210
  %call137 = call arm_aapcscc  i32 @fgetc(%struct._IO_FILE* %62), !dbg !1210
  %63 = load i32* %i, align 4, !dbg !1210
  %arrayidx138 = getelementptr inbounds [23 x i8]* @pubkey_magic, i32 0, i32 %63, !dbg !1210
  %64 = load i8* %arrayidx138, align 1, !dbg !1210
  %conv139 = sext i8 %64 to i32, !dbg !1210
  %cmp140 = icmp ne i32 %call137, %conv139, !dbg !1210
  br i1 %cmp140, label %if.then142, label %if.end146, !dbg !1210

if.then142:                                       ; preds = %for.body
  %65 = load %struct._IO_FILE** @stderr, align 4, !dbg !1212
  %call143 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([45 x i8]* @err_bad_public_key, i32 0, i32 0), %struct._IO_FILE* %65), !dbg !1212
  %66 = load %struct._IO_FILE** @stderr, align 4, !dbg !1214
  %call144 = call arm_aapcscc  i32 @fputs(i8* getelementptr inbounds ([23 x i8]* @pubkey_magic, i32 0, i32 0), %struct._IO_FILE* %66), !dbg !1214
  %67 = load %struct._IO_FILE** @stderr, align 4, !dbg !1215
  %call145 = call arm_aapcscc  i32 @fputc(i32 34, %struct._IO_FILE* %67), !dbg !1215
  store i32 1, i32* %retval, !dbg !1216
  br label %return, !dbg !1216

if.end146:                                        ; preds = %for.body
  br label %for.inc, !dbg !1217

for.inc:                                          ; preds = %if.end146
  %68 = load i32* %i, align 4, !dbg !1218
  %add = add i32 %68, 1, !dbg !1218
  store i32 %add, i32* %i, align 4, !dbg !1218
  br label %for.cond, !dbg !1218

for.end:                                          ; preds = %land.end
  br label %if.end147, !dbg !1219

if.end147:                                        ; preds = %for.end, %lor.lhs.false109
  %69 = load %struct._IO_FILE** @stdin, align 4, !dbg !1220
  store %struct._IO_FILE* %69, %struct._IO_FILE** %f_inp, align 4, !dbg !1220
  %70 = load %struct._IO_FILE** @stdout, align 4, !dbg !1221
  store %struct._IO_FILE* %70, %struct._IO_FILE** %f_out, align 4, !dbg !1221
  %71 = load i32* %filter, align 4, !dbg !1222
  %tobool148 = icmp ne i32 %71, 0, !dbg !1222
  br i1 %tobool148, label %if.else188, label %if.then149, !dbg !1222

if.then149:                                       ; preds = %if.end147
  %72 = load i32* %operation, align 4, !dbg !1223
  %cmp150 = icmp eq i32 86, %72, !dbg !1223
  br i1 %cmp150, label %if.then155, label %lor.lhs.false152, !dbg !1223

lor.lhs.false152:                                 ; preds = %if.then149
  %73 = load i32* %operation, align 4, !dbg !1223
  %cmp153 = icmp eq i32 83, %73, !dbg !1223
  br i1 %cmp153, label %if.then155, label %if.end158, !dbg !1223

if.then155:                                       ; preds = %lor.lhs.false152, %if.then149
  %arrayidx156 = getelementptr inbounds [3 x i8]* %openForWrite, i32 0, i32 1, !dbg !1225
  store i8 0, i8* %arrayidx156, align 1, !dbg !1225
  %arrayidx157 = getelementptr inbounds [3 x i8]* %openForRead, i32 0, i32 1, !dbg !1225
  store i8 0, i8* %arrayidx157, align 1, !dbg !1225
  br label %if.end158, !dbg !1225

if.end158:                                        ; preds = %if.then155, %lor.lhs.false152
  store %struct._IO_FILE* null, %struct._IO_FILE** %f_sec, align 4, !dbg !1226
  %74 = load i32* %operation, align 4, !dbg !1227
  %cmp159 = icmp eq i32 101, %74, !dbg !1227
  br i1 %cmp159, label %if.then164, label %lor.lhs.false161, !dbg !1227

lor.lhs.false161:                                 ; preds = %if.end158
  %75 = load i32* %operation, align 4, !dbg !1227
  %cmp162 = icmp eq i32 118, %75, !dbg !1227
  br i1 %cmp162, label %if.then164, label %if.end165, !dbg !1227

if.then164:                                       ; preds = %lor.lhs.false161, %if.end158
  %76 = load %struct._IO_FILE** @stdin, align 4, !dbg !1228
  store %struct._IO_FILE* %76, %struct._IO_FILE** %f_sec, align 4, !dbg !1228
  br label %if.end165, !dbg !1228

if.end165:                                        ; preds = %if.then164, %lor.lhs.false161
  %77 = load i32* %argc.addr, align 4, !dbg !1229
  %78 = load i32* %arg_ix, align 4, !dbg !1229
  %cmp166 = icmp ugt i32 %77, %78, !dbg !1229
  br i1 %cmp166, label %if.then168, label %if.end176, !dbg !1229

if.then168:                                       ; preds = %if.end165
  %79 = load i32* %arg_ix, align 4, !dbg !1230
  %inc169 = add i32 %79, 1, !dbg !1230
  store i32 %inc169, i32* %arg_ix, align 4, !dbg !1230
  %80 = load i8*** %argv.addr, align 4, !dbg !1230
  %arrayidx170 = getelementptr inbounds i8** %80, i32 %79, !dbg !1230
  %81 = load i8** %arrayidx170, !dbg !1230
  %arraydecay171 = getelementptr inbounds [3 x i8]* %openForRead, i32 0, i32 0, !dbg !1230
  %call172 = call arm_aapcscc  %struct._IO_FILE* @chkopen(i8* %81, i8* %arraydecay171), !dbg !1230
  store %struct._IO_FILE* %call172, %struct._IO_FILE** %f_inp, align 4, !dbg !1230
  %82 = load %struct._IO_FILE** %f_inp, align 4, !dbg !1232
  %tobool173 = icmp ne %struct._IO_FILE* %82, null, !dbg !1232
  br i1 %tobool173, label %if.end175, label %if.then174, !dbg !1232

if.then174:                                       ; preds = %if.then168
  store i32 1, i32* %retval, !dbg !1233
  br label %return, !dbg !1233

if.end175:                                        ; preds = %if.then168
  br label %if.end176, !dbg !1234

if.end176:                                        ; preds = %if.end175, %if.end165
  %83 = load i32* %argc.addr, align 4, !dbg !1235
  %84 = load i32* %arg_ix, align 4, !dbg !1235
  %cmp177 = icmp ugt i32 %83, %84, !dbg !1235
  br i1 %cmp177, label %if.then179, label %if.end187, !dbg !1235

if.then179:                                       ; preds = %if.end176
  %85 = load i32* %arg_ix, align 4, !dbg !1236
  %inc180 = add i32 %85, 1, !dbg !1236
  store i32 %inc180, i32* %arg_ix, align 4, !dbg !1236
  %86 = load i8*** %argv.addr, align 4, !dbg !1236
  %arrayidx181 = getelementptr inbounds i8** %86, i32 %85, !dbg !1236
  %87 = load i8** %arrayidx181, !dbg !1236
  %arraydecay182 = getelementptr inbounds [3 x i8]* %openForWrite, i32 0, i32 0, !dbg !1236
  %call183 = call arm_aapcscc  %struct._IO_FILE* @chkopen(i8* %87, i8* %arraydecay182), !dbg !1236
  store %struct._IO_FILE* %call183, %struct._IO_FILE** %f_out, align 4, !dbg !1236
  %88 = load %struct._IO_FILE** %f_out, align 4, !dbg !1238
  %tobool184 = icmp ne %struct._IO_FILE* %88, null, !dbg !1238
  br i1 %tobool184, label %if.end186, label %if.then185, !dbg !1238

if.then185:                                       ; preds = %if.then179
  store i32 1, i32* %retval, !dbg !1239
  br label %return, !dbg !1239

if.end186:                                        ; preds = %if.then179
  br label %if.end187, !dbg !1240

if.end187:                                        ; preds = %if.end186, %if.end176
  br label %if.end200, !dbg !1241

if.else188:                                       ; preds = %if.end147
  store %struct._IO_FILE* null, %struct._IO_FILE** %f_sec, align 4, !dbg !1242
  %89 = load i32* %operation, align 4, !dbg !1244
  %cmp189 = icmp eq i32 101, %89, !dbg !1244
  br i1 %cmp189, label %if.then191, label %if.end199, !dbg !1244

if.then191:                                       ; preds = %if.else188
  %90 = load i32* %arg_ix, align 4, !dbg !1245
  %inc192 = add i32 %90, 1, !dbg !1245
  store i32 %inc192, i32* %arg_ix, align 4, !dbg !1245
  %91 = load i8*** %argv.addr, align 4, !dbg !1245
  %arrayidx193 = getelementptr inbounds i8** %91, i32 %90, !dbg !1245
  %92 = load i8** %arrayidx193, !dbg !1245
  %arraydecay194 = getelementptr inbounds [3 x i8]* %openForRead, i32 0, i32 0, !dbg !1245
  %call195 = call arm_aapcscc  %struct._IO_FILE* @chkopen(i8* %92, i8* %arraydecay194), !dbg !1245
  store %struct._IO_FILE* %call195, %struct._IO_FILE** %f_sec, align 4, !dbg !1245
  %93 = load %struct._IO_FILE** %f_sec, align 4, !dbg !1247
  %tobool196 = icmp ne %struct._IO_FILE* %93, null, !dbg !1247
  br i1 %tobool196, label %if.end198, label %if.then197, !dbg !1247

if.then197:                                       ; preds = %if.then191
  store i32 1, i32* %retval, !dbg !1248
  br label %return, !dbg !1248

if.end198:                                        ; preds = %if.then191
  br label %if.end199, !dbg !1249

if.end199:                                        ; preds = %if.end198, %if.else188
  br label %if.end200

if.end200:                                        ; preds = %if.end199, %if.end187
  %94 = load %struct._IO_FILE** %f_key, align 4, !dbg !1250
  %95 = load %struct._IO_FILE** %f_inp, align 4, !dbg !1250
  %96 = load %struct._IO_FILE** %f_out, align 4, !dbg !1250
  %97 = load %struct._IO_FILE** %f_sec, align 4, !dbg !1250
  %98 = load i32* %operation, align 4, !dbg !1250
  %call201 = call arm_aapcscc  i32 @do_operation(%struct._IO_FILE* %94, %struct._IO_FILE* %95, %struct._IO_FILE* %96, %struct._IO_FILE* %97, i32 %98), !dbg !1250
  store i32 %call201, i32* %err, align 4, !dbg !1250
  call arm_aapcscc  void @burn_stack(), !dbg !1251
  call arm_aapcscc  void @burnBinasc(), !dbg !1252
  %99 = load i32* %err, align 4, !dbg !1253
  store i32 %99, i32* %retval, !dbg !1253
  br label %return, !dbg !1253

return:                                           ; preds = %if.end200, %if.then197, %if.then185, %if.then174, %if.then142, %if.then123, %filterError, %if.end
  %100 = load i32* %retval, !dbg !1254
  ret i32 %100, !dbg !1254
}

declare void @llvm.memcpy.p0i8.p0i8.i32(i8* nocapture, i8* nocapture, i32, i32, i1) nounwind

declare arm_aapcscc i32 @fputc(i32, %struct._IO_FILE*)

declare arm_aapcscc void @burnBinasc()

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !18, metadata !20, metadata !177} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{metadata !3}
!3 = metadata !{i32 720900, null, metadata !"", metadata !4, i32 48, i64 32, i64 32, i32 0, i32 0, null, metadata !5, i32 0, i32 0} ; [ DW_TAG_enumeration_type ]
!4 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!5 = metadata !{metadata !6, metadata !7, metadata !8, metadata !9, metadata !10, metadata !11, metadata !12, metadata !13, metadata !14, metadata !15, metadata !16, metadata !17}
!6 = metadata !{i32 720936, metadata !"_ISupper", i64 256} ; [ DW_TAG_enumerator ]
!7 = metadata !{i32 720936, metadata !"_ISlower", i64 512} ; [ DW_TAG_enumerator ]
!8 = metadata !{i32 720936, metadata !"_ISalpha", i64 1024} ; [ DW_TAG_enumerator ]
!9 = metadata !{i32 720936, metadata !"_ISdigit", i64 2048} ; [ DW_TAG_enumerator ]
!10 = metadata !{i32 720936, metadata !"_ISxdigit", i64 4096} ; [ DW_TAG_enumerator ]
!11 = metadata !{i32 720936, metadata !"_ISspace", i64 8192} ; [ DW_TAG_enumerator ]
!12 = metadata !{i32 720936, metadata !"_ISprint", i64 16384} ; [ DW_TAG_enumerator ]
!13 = metadata !{i32 720936, metadata !"_ISgraph", i64 32768} ; [ DW_TAG_enumerator ]
!14 = metadata !{i32 720936, metadata !"_ISblank", i64 1} ; [ DW_TAG_enumerator ]
!15 = metadata !{i32 720936, metadata !"_IScntrl", i64 2} ; [ DW_TAG_enumerator ]
!16 = metadata !{i32 720936, metadata !"_ISpunct", i64 4} ; [ DW_TAG_enumerator ]
!17 = metadata !{i32 720936, metadata !"_ISalnum", i64 8} ; [ DW_TAG_enumerator ]
!18 = metadata !{metadata !19}
!19 = metadata !{i32 0}
!20 = metadata !{metadata !21}
!21 = metadata !{metadata !22, metadata !28, metadata !34, metadata !37, metadata !40, metadata !43, metadata !46, metadata !49, metadata !52, metadata !55, metadata !58, metadata !61, metadata !64, metadata !71, metadata !74, metadata !77, metadata !80, metadata !83, metadata !86, metadata !89, metadata !92, metadata !95, metadata !98, metadata !101, metadata !104, metadata !107, metadata !171, metadata !174}
!22 = metadata !{i32 720942, i32 0, metadata !23, metadata !"hash_process_file", metadata !"hash_process_file", metadata !"", metadata !23, i32 77, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.hash_context*, %struct._IO_FILE*, i32)* @hash_process_file, null, null, metadata !26} ; [ DW_TAG_subprogram ]
!23 = metadata !{i32 720937, metadata !"pegwit.c", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!24 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !25, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!25 = metadata !{null}
!26 = metadata !{metadata !27}
!27 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!28 = metadata !{i32 720942, i32 0, metadata !23, metadata !"downcase", metadata !"downcase", metadata !"", metadata !23, i32 98, metadata !29, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i8)* @downcase, null, null, metadata !32} ; [ DW_TAG_subprogram ]
!29 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !30, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!30 = metadata !{metadata !31}
!31 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!32 = metadata !{metadata !33}
!33 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!34 = metadata !{i32 720942, i32 0, metadata !23, metadata !"case_blind_compare", metadata !"case_blind_compare", metadata !"", metadata !23, i32 104, metadata !29, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i8*, i8*)* @case_blind_compare, null, null, metadata !35} ; [ DW_TAG_subprogram ]
!35 = metadata !{metadata !36}
!36 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!37 = metadata !{i32 720942, i32 0, metadata !23, metadata !"hash_process_ascii", metadata !"hash_process_ascii", metadata !"", metadata !23, i32 119, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.hash_context*, %struct._IO_FILE*, %struct._IO_FILE*, i32, i32)* @hash_process_ascii, null, null, metadata !38} ; [ DW_TAG_subprogram ]
!38 = metadata !{metadata !39}
!39 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!40 = metadata !{i32 720942, i32 0, metadata !23, metadata !"prng_init", metadata !"prng_init", metadata !"", metadata !23, i32 183, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.prng*)* @prng_init, null, null, metadata !41} ; [ DW_TAG_subprogram ]
!41 = metadata !{metadata !42}
!42 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!43 = metadata !{i32 720942, i32 0, metadata !23, metadata !"prng_set_secret", metadata !"prng_set_secret", metadata !"", metadata !23, i32 188, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.prng*, %struct._IO_FILE*)* @prng_set_secret, null, null, metadata !44} ; [ DW_TAG_subprogram ]
!44 = metadata !{metadata !45}
!45 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!46 = metadata !{i32 720942, i32 0, metadata !23, metadata !"prng_init_mac", metadata !"prng_init_mac", metadata !"", metadata !23, i32 197, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.hash_context*)* @prng_init_mac, null, null, metadata !47} ; [ DW_TAG_subprogram ]
!47 = metadata !{metadata !48}
!48 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!49 = metadata !{i32 720942, i32 0, metadata !23, metadata !"prng_set_mac", metadata !"prng_set_mac", metadata !"", metadata !23, i32 208, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.prng*, %struct._IO_FILE*, i32)* @prng_set_mac, null, null, metadata !50} ; [ DW_TAG_subprogram ]
!50 = metadata !{metadata !51}
!51 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!52 = metadata !{i32 720942, i32 0, metadata !23, metadata !"clearsign", metadata !"clearsign", metadata !"", metadata !23, i32 226, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.prng*, %struct._IO_FILE*, %struct._IO_FILE*)* @clearsign, null, null, metadata !53} ; [ DW_TAG_subprogram ]
!53 = metadata !{metadata !54}
!54 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!55 = metadata !{i32 720942, i32 0, metadata !23, metadata !"position", metadata !"position", metadata !"", metadata !23, i32 239, metadata !29, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct._IO_FILE*)* @position, null, null, metadata !56} ; [ DW_TAG_subprogram ]
!56 = metadata !{metadata !57}
!57 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!58 = metadata !{i32 720942, i32 0, metadata !23, metadata !"readsign", metadata !"readsign", metadata !"", metadata !23, i32 255, metadata !29, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct.prng*, %struct._IO_FILE*, %struct._IO_FILE*)* @readsign, null, null, metadata !59} ; [ DW_TAG_subprogram ]
!59 = metadata !{metadata !60}
!60 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!61 = metadata !{i32 720942, i32 0, metadata !23, metadata !"prng_set_time", metadata !"prng_set_time", metadata !"", metadata !23, i32 269, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.prng*)* @prng_set_time, null, null, metadata !62} ; [ DW_TAG_subprogram ]
!62 = metadata !{metadata !63}
!63 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!64 = metadata !{i32 720942, i32 0, metadata !23, metadata !"prng_next", metadata !"prng_next", metadata !"", metadata !23, i32 275, metadata !65, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct.prng*)* @prng_next, null, null, metadata !69} ; [ DW_TAG_subprogram ]
!65 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !66, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!66 = metadata !{metadata !67}
!67 = metadata !{i32 720918, null, metadata !"word32", metadata !23, i32 12, i64 0, i64 0, i64 0, i32 0, metadata !68} ; [ DW_TAG_typedef ]
!68 = metadata !{i32 720932, null, metadata !"long unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!69 = metadata !{metadata !70}
!70 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!71 = metadata !{i32 720942, i32 0, metadata !23, metadata !"prng_to_vlong", metadata !"prng_to_vlong", metadata !"", metadata !23, i32 298, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct.prng*, i16*)* @prng_to_vlong, null, null, metadata !72} ; [ DW_TAG_subprogram ]
!72 = metadata !{metadata !73}
!73 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!74 = metadata !{i32 720942, i32 0, metadata !23, metadata !"hash_to_vlong", metadata !"hash_to_vlong", metadata !"", metadata !23, i32 306, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i32*, i16*)* @hash_to_vlong, null, null, metadata !75} ; [ DW_TAG_subprogram ]
!75 = metadata !{metadata !76}
!76 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!77 = metadata !{i32 720942, i32 0, metadata !23, metadata !"get_vlong", metadata !"get_vlong", metadata !"", metadata !23, i32 318, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct._IO_FILE*, i16*)* @get_vlong, null, null, metadata !78} ; [ DW_TAG_subprogram ]
!78 = metadata !{metadata !79}
!79 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!80 = metadata !{i32 720942, i32 0, metadata !23, metadata !"get_vlong_a", metadata !"get_vlong_a", metadata !"", metadata !23, i32 343, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct._IO_FILE*, i16*)* @get_vlong_a, null, null, metadata !81} ; [ DW_TAG_subprogram ]
!81 = metadata !{metadata !82}
!82 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!83 = metadata !{i32 720942, i32 0, metadata !23, metadata !"put_vlong", metadata !"put_vlong", metadata !"", metadata !23, i32 374, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*)* @put_vlong, null, null, metadata !84} ; [ DW_TAG_subprogram ]
!84 = metadata !{metadata !85}
!85 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!86 = metadata !{i32 720942, i32 0, metadata !23, metadata !"put_binary_vlong", metadata !"put_binary_vlong", metadata !"", metadata !23, i32 385, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct._IO_FILE*, i16*)* @put_binary_vlong, null, null, metadata !87} ; [ DW_TAG_subprogram ]
!87 = metadata !{metadata !88}
!88 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!89 = metadata !{i32 720942, i32 0, metadata !23, metadata !"get_binary_vlong", metadata !"get_binary_vlong", metadata !"", metadata !23, i32 396, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (%struct._IO_FILE*, i16*)* @get_binary_vlong, null, null, metadata !90} ; [ DW_TAG_subprogram ]
!90 = metadata !{metadata !91}
!91 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!92 = metadata !{i32 720942, i32 0, metadata !23, metadata !"vlong_to_square_block", metadata !"vlong_to_square_block", metadata !"", metadata !23, i32 415, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i16*, i8*)* @vlong_to_square_block, null, null, metadata !93} ; [ DW_TAG_subprogram ]
!93 = metadata !{metadata !94}
!94 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!95 = metadata !{i32 720942, i32 0, metadata !23, metadata !"increment", metadata !"increment", metadata !"", metadata !23, i32 428, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void (i8*)* @increment, null, null, metadata !96} ; [ DW_TAG_subprogram ]
!96 = metadata !{metadata !97}
!97 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!98 = metadata !{i32 720942, i32 0, metadata !23, metadata !"sym_encrypt", metadata !"sym_encrypt", metadata !"", metadata !23, i32 435, metadata !29, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, %struct._IO_FILE*, %struct._IO_FILE*)* @sym_encrypt, null, null, metadata !99} ; [ DW_TAG_subprogram ]
!99 = metadata !{metadata !100}
!100 = metadata !{i32 720932}                     ; [ DW_TAG_base_type ]
!101 = metadata !{i32 720942, i32 0, metadata !23, metadata !"sym_decrypt", metadata !"sym_decrypt", metadata !"", metadata !23, i32 479, metadata !29, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i16*, %struct._IO_FILE*, %struct._IO_FILE*)* @sym_decrypt, null, null, metadata !102} ; [ DW_TAG_subprogram ]
!102 = metadata !{metadata !103}
!103 = metadata !{i32 720932}                     ; [ DW_TAG_base_type ]
!104 = metadata !{i32 720942, i32 0, metadata !23, metadata !"do_operation", metadata !"do_operation", metadata !"", metadata !23, i32 547, metadata !29, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (%struct._IO_FILE*, %struct._IO_FILE*, %struct._IO_FILE*, %struct._IO_FILE*, i32)* @do_operation, null, null, metadata !105} ; [ DW_TAG_subprogram ]
!105 = metadata !{metadata !106}
!106 = metadata !{i32 720932}                     ; [ DW_TAG_base_type ]
!107 = metadata !{i32 720942, i32 0, metadata !23, metadata !"chkopen", metadata !"chkopen", metadata !"", metadata !23, i32 679, metadata !108, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, %struct._IO_FILE* (i8*, i8*)* @chkopen, null, null, metadata !169} ; [ DW_TAG_subprogram ]
!108 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !109, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!109 = metadata !{metadata !110}
!110 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !111} ; [ DW_TAG_pointer_type ]
!111 = metadata !{i32 720918, null, metadata !"FILE", metadata !23, i32 49, i64 0, i64 0, i64 0, i32 0, metadata !112} ; [ DW_TAG_typedef ]
!112 = metadata !{i32 720915, null, metadata !"_IO_FILE", metadata !113, i32 271, i64 1216, i64 64, i32 0, i32 0, null, metadata !114, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!113 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!114 = metadata !{metadata !115, metadata !116, metadata !119, metadata !120, metadata !121, metadata !122, metadata !123, metadata !124, metadata !125, metadata !126, metadata !127, metadata !128, metadata !129, metadata !137, metadata !138, metadata !139, metadata !140, metadata !143, metadata !145, metadata !147, metadata !151, metadata !153, metadata !157, metadata !158, metadata !159, metadata !160, metadata !161, metadata !164, metadata !165}
!115 = metadata !{i32 720909, metadata !112, metadata !"_flags", metadata !113, i32 272, i64 32, i64 32, i64 0, i32 0, metadata !31} ; [ DW_TAG_member ]
!116 = metadata !{i32 720909, metadata !112, metadata !"_IO_read_ptr", metadata !113, i32 277, i64 32, i64 32, i64 32, i32 0, metadata !117} ; [ DW_TAG_member ]
!117 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !118} ; [ DW_TAG_pointer_type ]
!118 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!119 = metadata !{i32 720909, metadata !112, metadata !"_IO_read_end", metadata !113, i32 278, i64 32, i64 32, i64 64, i32 0, metadata !117} ; [ DW_TAG_member ]
!120 = metadata !{i32 720909, metadata !112, metadata !"_IO_read_base", metadata !113, i32 279, i64 32, i64 32, i64 96, i32 0, metadata !117} ; [ DW_TAG_member ]
!121 = metadata !{i32 720909, metadata !112, metadata !"_IO_write_base", metadata !113, i32 280, i64 32, i64 32, i64 128, i32 0, metadata !117} ; [ DW_TAG_member ]
!122 = metadata !{i32 720909, metadata !112, metadata !"_IO_write_ptr", metadata !113, i32 281, i64 32, i64 32, i64 160, i32 0, metadata !117} ; [ DW_TAG_member ]
!123 = metadata !{i32 720909, metadata !112, metadata !"_IO_write_end", metadata !113, i32 282, i64 32, i64 32, i64 192, i32 0, metadata !117} ; [ DW_TAG_member ]
!124 = metadata !{i32 720909, metadata !112, metadata !"_IO_buf_base", metadata !113, i32 283, i64 32, i64 32, i64 224, i32 0, metadata !117} ; [ DW_TAG_member ]
!125 = metadata !{i32 720909, metadata !112, metadata !"_IO_buf_end", metadata !113, i32 284, i64 32, i64 32, i64 256, i32 0, metadata !117} ; [ DW_TAG_member ]
!126 = metadata !{i32 720909, metadata !112, metadata !"_IO_save_base", metadata !113, i32 286, i64 32, i64 32, i64 288, i32 0, metadata !117} ; [ DW_TAG_member ]
!127 = metadata !{i32 720909, metadata !112, metadata !"_IO_backup_base", metadata !113, i32 287, i64 32, i64 32, i64 320, i32 0, metadata !117} ; [ DW_TAG_member ]
!128 = metadata !{i32 720909, metadata !112, metadata !"_IO_save_end", metadata !113, i32 288, i64 32, i64 32, i64 352, i32 0, metadata !117} ; [ DW_TAG_member ]
!129 = metadata !{i32 720909, metadata !112, metadata !"_markers", metadata !113, i32 290, i64 32, i64 32, i64 384, i32 0, metadata !130} ; [ DW_TAG_member ]
!130 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !131} ; [ DW_TAG_pointer_type ]
!131 = metadata !{i32 720915, null, metadata !"_IO_marker", metadata !113, i32 186, i64 96, i64 32, i32 0, i32 0, null, metadata !132, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!132 = metadata !{metadata !133, metadata !134, metadata !136}
!133 = metadata !{i32 720909, metadata !131, metadata !"_next", metadata !113, i32 187, i64 32, i64 32, i64 0, i32 0, metadata !130} ; [ DW_TAG_member ]
!134 = metadata !{i32 720909, metadata !131, metadata !"_sbuf", metadata !113, i32 188, i64 32, i64 32, i64 32, i32 0, metadata !135} ; [ DW_TAG_member ]
!135 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !112} ; [ DW_TAG_pointer_type ]
!136 = metadata !{i32 720909, metadata !131, metadata !"_pos", metadata !113, i32 192, i64 32, i64 32, i64 64, i32 0, metadata !31} ; [ DW_TAG_member ]
!137 = metadata !{i32 720909, metadata !112, metadata !"_chain", metadata !113, i32 292, i64 32, i64 32, i64 416, i32 0, metadata !135} ; [ DW_TAG_member ]
!138 = metadata !{i32 720909, metadata !112, metadata !"_fileno", metadata !113, i32 294, i64 32, i64 32, i64 448, i32 0, metadata !31} ; [ DW_TAG_member ]
!139 = metadata !{i32 720909, metadata !112, metadata !"_flags2", metadata !113, i32 298, i64 32, i64 32, i64 480, i32 0, metadata !31} ; [ DW_TAG_member ]
!140 = metadata !{i32 720909, metadata !112, metadata !"_old_offset", metadata !113, i32 300, i64 32, i64 32, i64 512, i32 0, metadata !141} ; [ DW_TAG_member ]
!141 = metadata !{i32 720918, null, metadata !"__off_t", metadata !113, i32 141, i64 0, i64 0, i64 0, i32 0, metadata !142} ; [ DW_TAG_typedef ]
!142 = metadata !{i32 720932, null, metadata !"long int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!143 = metadata !{i32 720909, metadata !112, metadata !"_cur_column", metadata !113, i32 304, i64 16, i64 16, i64 544, i32 0, metadata !144} ; [ DW_TAG_member ]
!144 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!145 = metadata !{i32 720909, metadata !112, metadata !"_vtable_offset", metadata !113, i32 305, i64 8, i64 8, i64 560, i32 0, metadata !146} ; [ DW_TAG_member ]
!146 = metadata !{i32 720932, null, metadata !"signed char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!147 = metadata !{i32 720909, metadata !112, metadata !"_shortbuf", metadata !113, i32 306, i64 8, i64 8, i64 568, i32 0, metadata !148} ; [ DW_TAG_member ]
!148 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8, i64 8, i32 0, i32 0, metadata !118, metadata !149, i32 0, i32 0} ; [ DW_TAG_array_type ]
!149 = metadata !{metadata !150}
!150 = metadata !{i32 720929, i64 0, i64 0}       ; [ DW_TAG_subrange_type ]
!151 = metadata !{i32 720909, metadata !112, metadata !"_lock", metadata !113, i32 310, i64 32, i64 32, i64 576, i32 0, metadata !152} ; [ DW_TAG_member ]
!152 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, null} ; [ DW_TAG_pointer_type ]
!153 = metadata !{i32 720909, metadata !112, metadata !"_offset", metadata !113, i32 319, i64 64, i64 64, i64 640, i32 0, metadata !154} ; [ DW_TAG_member ]
!154 = metadata !{i32 720918, null, metadata !"__off64_t", metadata !113, i32 142, i64 0, i64 0, i64 0, i32 0, metadata !155} ; [ DW_TAG_typedef ]
!155 = metadata !{i32 720918, null, metadata !"__quad_t", metadata !113, i32 56, i64 0, i64 0, i64 0, i32 0, metadata !156} ; [ DW_TAG_typedef ]
!156 = metadata !{i32 720932, null, metadata !"long long int", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!157 = metadata !{i32 720909, metadata !112, metadata !"__pad1", metadata !113, i32 328, i64 32, i64 32, i64 704, i32 0, metadata !152} ; [ DW_TAG_member ]
!158 = metadata !{i32 720909, metadata !112, metadata !"__pad2", metadata !113, i32 329, i64 32, i64 32, i64 736, i32 0, metadata !152} ; [ DW_TAG_member ]
!159 = metadata !{i32 720909, metadata !112, metadata !"__pad3", metadata !113, i32 330, i64 32, i64 32, i64 768, i32 0, metadata !152} ; [ DW_TAG_member ]
!160 = metadata !{i32 720909, metadata !112, metadata !"__pad4", metadata !113, i32 331, i64 32, i64 32, i64 800, i32 0, metadata !152} ; [ DW_TAG_member ]
!161 = metadata !{i32 720909, metadata !112, metadata !"__pad5", metadata !113, i32 332, i64 32, i64 32, i64 832, i32 0, metadata !162} ; [ DW_TAG_member ]
!162 = metadata !{i32 720918, null, metadata !"size_t", metadata !113, i32 211, i64 0, i64 0, i64 0, i32 0, metadata !163} ; [ DW_TAG_typedef ]
!163 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!164 = metadata !{i32 720909, metadata !112, metadata !"_mode", metadata !113, i32 334, i64 32, i64 32, i64 864, i32 0, metadata !31} ; [ DW_TAG_member ]
!165 = metadata !{i32 720909, metadata !112, metadata !"_unused2", metadata !113, i32 336, i64 320, i64 8, i64 896, i32 0, metadata !166} ; [ DW_TAG_member ]
!166 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !118, metadata !167, i32 0, i32 0} ; [ DW_TAG_array_type ]
!167 = metadata !{metadata !168}
!168 = metadata !{i32 720929, i64 0, i64 39}      ; [ DW_TAG_subrange_type ]
!169 = metadata !{metadata !170}
!170 = metadata !{i32 720932}                     ; [ DW_TAG_base_type ]
!171 = metadata !{i32 720942, i32 0, metadata !23, metadata !"burn_stack", metadata !"burn_stack", metadata !"", metadata !23, i32 690, metadata !24, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, void ()* @burn_stack, null, null, metadata !172} ; [ DW_TAG_subprogram ]
!172 = metadata !{metadata !173}
!173 = metadata !{i32 720932}                     ; [ DW_TAG_base_type ]
!174 = metadata !{i32 720942, i32 0, metadata !23, metadata !"main", metadata !"main", metadata !"", metadata !23, i32 706, metadata !29, i1 false, i1 true, i32 0, i32 0, null, i32 256, i1 false, i32 (i32, i8**)* @main, null, null, metadata !175} ; [ DW_TAG_subprogram ]
!175 = metadata !{metadata !176}
!176 = metadata !{i32 720932}                     ; [ DW_TAG_base_type ]
!177 = metadata !{metadata !178}
!178 = metadata !{metadata !179, metadata !184, metadata !188, metadata !192, metadata !196, metadata !200, metadata !204, metadata !208, metadata !212, metadata !216, metadata !220, metadata !222, metadata !223, metadata !227, metadata !231, metadata !235, metadata !239, metadata !243, metadata !252}
!179 = metadata !{i32 720948, i32 0, null, metadata !"manual", metadata !"manual", metadata !"", metadata !23, i32 27, metadata !180, i32 0, i32 1, [374 x i8]* @manual} ; [ DW_TAG_variable ]
!180 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 2992, i64 8, i32 0, i32 0, metadata !181, metadata !182, i32 0, i32 0} ; [ DW_TAG_array_type ]
!181 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !118} ; [ DW_TAG_const_type ]
!182 = metadata !{metadata !183}
!183 = metadata !{i32 720929, i64 0, i64 373}     ; [ DW_TAG_subrange_type ]
!184 = metadata !{i32 720948, i32 0, null, metadata !"filterManual", metadata !"filterManual", metadata !"", metadata !23, i32 40, metadata !185, i32 0, i32 1, [284 x i8]* @filterManual} ; [ DW_TAG_variable ]
!185 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 2272, i64 8, i32 0, i32 0, metadata !181, metadata !186, i32 0, i32 0} ; [ DW_TAG_array_type ]
!186 = metadata !{metadata !187}
!187 = metadata !{i32 720929, i64 0, i64 283}     ; [ DW_TAG_subrange_type ]
!188 = metadata !{i32 720948, i32 0, null, metadata !"pubkey_magic", metadata !"pubkey_magic", metadata !"", metadata !23, i32 51, metadata !189, i32 0, i32 1, [23 x i8]* @pubkey_magic} ; [ DW_TAG_variable ]
!189 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 184, i64 8, i32 0, i32 0, metadata !181, metadata !190, i32 0, i32 0} ; [ DW_TAG_array_type ]
!190 = metadata !{metadata !191}
!191 = metadata !{i32 720929, i64 0, i64 22}      ; [ DW_TAG_subrange_type ]
!192 = metadata !{i32 720948, i32 0, null, metadata !"err_output", metadata !"err_output", metadata !"", metadata !23, i32 52, metadata !193, i32 0, i32 1, [41 x i8]* @err_output} ; [ DW_TAG_variable ]
!193 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 328, i64 8, i32 0, i32 0, metadata !181, metadata !194, i32 0, i32 0} ; [ DW_TAG_array_type ]
!194 = metadata !{metadata !195}
!195 = metadata !{i32 720929, i64 0, i64 40}      ; [ DW_TAG_subrange_type ]
!196 = metadata !{i32 720948, i32 0, null, metadata !"err_open_failed", metadata !"err_open_failed", metadata !"", metadata !23, i32 53, metadata !197, i32 0, i32 1, [32 x i8]* @err_open_failed} ; [ DW_TAG_variable ]
!197 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 256, i64 8, i32 0, i32 0, metadata !181, metadata !198, i32 0, i32 0} ; [ DW_TAG_array_type ]
!198 = metadata !{metadata !199}
!199 = metadata !{i32 720929, i64 0, i64 31}      ; [ DW_TAG_subrange_type ]
!200 = metadata !{i32 720948, i32 0, null, metadata !"err_bad_public_key", metadata !"err_bad_public_key", metadata !"", metadata !23, i32 54, metadata !201, i32 0, i32 1, [45 x i8]* @err_bad_public_key} ; [ DW_TAG_variable ]
!201 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 360, i64 8, i32 0, i32 0, metadata !181, metadata !202, i32 0, i32 0} ; [ DW_TAG_array_type ]
!202 = metadata !{metadata !203}
!203 = metadata !{i32 720929, i64 0, i64 44}      ; [ DW_TAG_subrange_type ]
!204 = metadata !{i32 720948, i32 0, null, metadata !"err_signature", metadata !"err_signature", metadata !"", metadata !23, i32 55, metadata !205, i32 0, i32 1, [29 x i8]* @err_signature} ; [ DW_TAG_variable ]
!205 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 232, i64 8, i32 0, i32 0, metadata !181, metadata !206, i32 0, i32 0} ; [ DW_TAG_array_type ]
!206 = metadata !{metadata !207}
!207 = metadata !{i32 720929, i64 0, i64 28}      ; [ DW_TAG_subrange_type ]
!208 = metadata !{i32 720948, i32 0, null, metadata !"err_decrypt", metadata !"err_decrypt", metadata !"", metadata !23, i32 56, metadata !209, i32 0, i32 1, [22 x i8]* @err_decrypt} ; [ DW_TAG_variable ]
!209 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 176, i64 8, i32 0, i32 0, metadata !181, metadata !210, i32 0, i32 0} ; [ DW_TAG_array_type ]
!210 = metadata !{metadata !211}
!211 = metadata !{i32 720929, i64 0, i64 21}      ; [ DW_TAG_subrange_type ]
!212 = metadata !{i32 720948, i32 0, null, metadata !"begin_clearsign", metadata !"begin_clearsign", metadata !"", metadata !23, i32 58, metadata !213, i32 0, i32 1, [5 x i8]* @begin_clearsign} ; [ DW_TAG_variable ]
!213 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 40, i64 8, i32 0, i32 0, metadata !181, metadata !214, i32 0, i32 0} ; [ DW_TAG_array_type ]
!214 = metadata !{metadata !215}
!215 = metadata !{i32 720929, i64 0, i64 4}       ; [ DW_TAG_subrange_type ]
!216 = metadata !{i32 720948, i32 0, null, metadata !"end_clearsign", metadata !"end_clearsign", metadata !"", metadata !23, i32 59, metadata !217, i32 0, i32 1, [31 x i8]* @end_clearsign} ; [ DW_TAG_variable ]
!217 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 248, i64 8, i32 0, i32 0, metadata !181, metadata !218, i32 0, i32 0} ; [ DW_TAG_array_type ]
!218 = metadata !{metadata !219}
!219 = metadata !{i32 720929, i64 0, i64 30}      ; [ DW_TAG_subrange_type ]
!220 = metadata !{i32 720948, i32 0, null, metadata !"end_ckarmour", metadata !"end_ckarmour", metadata !"", metadata !23, i32 60, metadata !221, i32 0, i32 1, [40 x i8]* @end_ckarmour} ; [ DW_TAG_variable ]
!221 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !181, metadata !167, i32 0, i32 0} ; [ DW_TAG_array_type ]
!222 = metadata !{i32 720948, i32 0, null, metadata !"end_pkarmour", metadata !"end_pkarmour", metadata !"", metadata !23, i32 61, metadata !221, i32 0, i32 1, [40 x i8]* @end_pkarmour} ; [ DW_TAG_variable ]
!223 = metadata !{i32 720948, i32 0, null, metadata !"escape", metadata !"escape", metadata !"", metadata !23, i32 62, metadata !224, i32 0, i32 1, [4 x i8]* @escape} ; [ DW_TAG_variable ]
!224 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 32, i64 8, i32 0, i32 0, metadata !181, metadata !225, i32 0, i32 0} ; [ DW_TAG_array_type ]
!225 = metadata !{metadata !226}
!226 = metadata !{i32 720929, i64 0, i64 3}       ; [ DW_TAG_subrange_type ]
!227 = metadata !{i32 720948, i32 0, null, metadata !"warn_long_line", metadata !"warn_long_line", metadata !"", metadata !23, i32 63, metadata !228, i32 0, i32 1, [70 x i8]* @warn_long_line} ; [ DW_TAG_variable ]
!228 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 560, i64 8, i32 0, i32 0, metadata !181, metadata !229, i32 0, i32 0} ; [ DW_TAG_array_type ]
!229 = metadata !{metadata !230}
!230 = metadata !{i32 720929, i64 0, i64 69}      ; [ DW_TAG_subrange_type ]
!231 = metadata !{i32 720948, i32 0, null, metadata !"warn_control_chars", metadata !"warn_control_chars", metadata !"", metadata !23, i32 66, metadata !232, i32 0, i32 1, [77 x i8]* @warn_control_chars} ; [ DW_TAG_variable ]
!232 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 616, i64 8, i32 0, i32 0, metadata !181, metadata !233, i32 0, i32 0} ; [ DW_TAG_array_type ]
!233 = metadata !{metadata !234}
!234 = metadata !{i32 720929, i64 0, i64 76}      ; [ DW_TAG_subrange_type ]
!235 = metadata !{i32 720948, i32 0, null, metadata !"err_clearsig_header_not_found", metadata !"err_clearsig_header_not_found", metadata !"", metadata !23, i32 69, metadata !236, i32 0, i32 1, [42 x i8]* @err_clearsig_header_not_found} ; [ DW_TAG_variable ]
!236 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 336, i64 8, i32 0, i32 0, metadata !181, metadata !237, i32 0, i32 0} ; [ DW_TAG_array_type ]
!237 = metadata !{metadata !238}
!238 = metadata !{i32 720929, i64 0, i64 41}      ; [ DW_TAG_subrange_type ]
!239 = metadata !{i32 720948, i32 0, null, metadata !"hex", metadata !"hex", metadata !"", metadata !23, i32 371, metadata !240, i32 0, i32 1, [16 x i8]* @hex} ; [ DW_TAG_variable ]
!240 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 128, i64 8, i32 0, i32 0, metadata !181, metadata !241, i32 0, i32 0} ; [ DW_TAG_array_type ]
!241 = metadata !{metadata !242}
!242 = metadata !{i32 720929, i64 0, i64 15}      ; [ DW_TAG_subrange_type ]
!243 = metadata !{i32 720948, i32 0, null, metadata !"ss_time_start", metadata !"ss_time_start", metadata !"", metadata !244, i32 6, metadata !245, i32 0, i32 1, %struct.timeval* @ss_time_start} ; [ DW_TAG_variable ]
!244 = metadata !{i32 720937, metadata !"stats_time.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!245 = metadata !{i32 720915, null, metadata !"timeval", metadata !246, i32 69, i64 64, i64 32, i32 0, i32 0, null, metadata !247, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!246 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!247 = metadata !{metadata !248, metadata !250}
!248 = metadata !{i32 720909, metadata !245, metadata !"tv_sec", metadata !246, i32 71, i64 32, i64 32, i64 0, i32 0, metadata !249} ; [ DW_TAG_member ]
!249 = metadata !{i32 720918, null, metadata !"__time_t", metadata !246, i32 149, i64 0, i64 0, i64 0, i32 0, metadata !142} ; [ DW_TAG_typedef ]
!250 = metadata !{i32 720909, metadata !245, metadata !"tv_usec", metadata !246, i32 72, i64 32, i64 32, i64 32, i32 0, metadata !251} ; [ DW_TAG_member ]
!251 = metadata !{i32 720918, null, metadata !"__suseconds_t", metadata !246, i32 151, i64 0, i64 0, i64 0, i32 0, metadata !142} ; [ DW_TAG_typedef ]
!252 = metadata !{i32 720948, i32 0, null, metadata !"ss_time_end", metadata !"ss_time_end", metadata !"", metadata !244, i32 7, metadata !245, i32 0, i32 1, %struct.timeval* @ss_time_end} ; [ DW_TAG_variable ]
!253 = metadata !{i32 721153, metadata !22, metadata !"c", metadata !23, i32 16777292, metadata !254, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!254 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !255} ; [ DW_TAG_pointer_type ]
!255 = metadata !{i32 720918, null, metadata !"hash_context", metadata !23, i32 7, i64 0, i64 0, i64 0, i32 0, metadata !256} ; [ DW_TAG_typedef ]
!256 = metadata !{i32 720915, null, metadata !"", metadata !257, i32 3, i64 736, i64 32, i32 0, i32 0, null, metadata !258, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!257 = metadata !{i32 720937, metadata !"sha1.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!258 = metadata !{metadata !259, metadata !261, metadata !265}
!259 = metadata !{i32 720909, metadata !256, metadata !"state", metadata !257, i32 4, i64 160, i64 32, i64 0, i32 0, metadata !260} ; [ DW_TAG_member ]
!260 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 160, i64 32, i32 0, i32 0, metadata !68, metadata !214, i32 0, i32 0} ; [ DW_TAG_array_type ]
!261 = metadata !{i32 720909, metadata !256, metadata !"count", metadata !257, i32 5, i64 64, i64 32, i64 160, i32 0, metadata !262} ; [ DW_TAG_member ]
!262 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 64, i64 32, i32 0, i32 0, metadata !68, metadata !263, i32 0, i32 0} ; [ DW_TAG_array_type ]
!263 = metadata !{metadata !264}
!264 = metadata !{i32 720929, i64 0, i64 1}       ; [ DW_TAG_subrange_type ]
!265 = metadata !{i32 720909, metadata !256, metadata !"buffer", metadata !257, i32 6, i64 512, i64 8, i64 224, i32 0, metadata !266} ; [ DW_TAG_member ]
!266 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 512, i64 8, i32 0, i32 0, metadata !267, metadata !268, i32 0, i32 0} ; [ DW_TAG_array_type ]
!267 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!268 = metadata !{metadata !269}
!269 = metadata !{i32 720929, i64 0, i64 63}      ; [ DW_TAG_subrange_type ]
!270 = metadata !{i32 76, i32 40, metadata !22, null}
!271 = metadata !{i32 721153, metadata !22, metadata !"f_inp", metadata !23, i32 33554508, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!272 = metadata !{i32 76, i32 50, metadata !22, null}
!273 = metadata !{i32 721153, metadata !22, metadata !"barrel", metadata !23, i32 50331724, metadata !163, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!274 = metadata !{i32 76, i32 66, metadata !22, null}
!275 = metadata !{i32 721152, metadata !276, metadata !"n", metadata !23, i32 78, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!276 = metadata !{i32 720907, metadata !22, i32 77, i32 1, metadata !23, i32 0} ; [ DW_TAG_lexical_block ]
!277 = metadata !{i32 78, i32 12, metadata !276, null}
!278 = metadata !{i32 721152, metadata !276, metadata !"buffer", metadata !23, i32 79, metadata !279, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!279 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 131072, i64 8, i32 0, i32 0, metadata !267, metadata !280, i32 0, i32 0} ; [ DW_TAG_array_type ]
!280 = metadata !{metadata !281}
!281 = metadata !{i32 720929, i64 0, i64 16383}   ; [ DW_TAG_subrange_type ]
!282 = metadata !{i32 79, i32 17, metadata !276, null}
!283 = metadata !{i32 80, i32 3, metadata !276, null}
!284 = metadata !{i32 82, i32 9, metadata !285, null}
!285 = metadata !{i32 720907, metadata !276, i32 81, i32 3, metadata !23, i32 1} ; [ DW_TAG_lexical_block ]
!286 = metadata !{i32 83, i32 5, metadata !285, null}
!287 = metadata !{i32 83, i32 15, metadata !285, null}
!288 = metadata !{i32 721152, metadata !289, metadata !"j", metadata !23, i32 85, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!289 = metadata !{i32 720907, metadata !285, i32 84, i32 5, metadata !23, i32 2} ; [ DW_TAG_lexical_block ]
!290 = metadata !{i32 85, i32 16, metadata !289, null}
!291 = metadata !{i32 86, i32 13, metadata !292, null}
!292 = metadata !{i32 720907, metadata !289, i32 86, i32 7, metadata !23, i32 3} ; [ DW_TAG_lexical_block ]
!293 = metadata !{i32 88, i32 9, metadata !294, null}
!294 = metadata !{i32 720907, metadata !292, i32 87, i32 7, metadata !23, i32 4} ; [ DW_TAG_lexical_block ]
!295 = metadata !{i32 89, i32 7, metadata !294, null}
!296 = metadata !{i32 86, i32 28, metadata !292, null}
!297 = metadata !{i32 91, i32 5, metadata !285, null}
!298 = metadata !{i32 91, i32 21, metadata !285, null}
!299 = metadata !{i32 92, i32 3, metadata !285, null}
!300 = metadata !{i32 93, i32 3, metadata !276, null}
!301 = metadata !{i32 94, i32 3, metadata !276, null}
!302 = metadata !{i32 95, i32 1, metadata !276, null}
!303 = metadata !{i32 721153, metadata !28, metadata !"c", metadata !23, i32 16777313, metadata !118, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!304 = metadata !{i32 97, i32 19, metadata !28, null}
!305 = metadata !{i32 99, i32 7, metadata !306, null}
!306 = metadata !{i32 720907, metadata !28, i32 98, i32 1, metadata !23, i32 5} ; [ DW_TAG_lexical_block ]
!307 = metadata !{i32 99, i32 32, metadata !306, null}
!308 = metadata !{i32 99, i32 38, metadata !306, null}
!309 = metadata !{i32 99, i32 110, metadata !306, null}
!310 = metadata !{i32 100, i32 7, metadata !306, null}
!311 = metadata !{i32 101, i32 1, metadata !306, null}
!312 = metadata !{i32 721153, metadata !34, metadata !"a", metadata !23, i32 16777319, metadata !313, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!313 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !181} ; [ DW_TAG_pointer_type ]
!314 = metadata !{i32 103, i32 36, metadata !34, null}
!315 = metadata !{i32 721153, metadata !34, metadata !"b", metadata !23, i32 33554535, metadata !313, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!316 = metadata !{i32 103, i32 51, metadata !34, null}
!317 = metadata !{i32 105, i32 5, metadata !318, null}
!318 = metadata !{i32 720907, metadata !34, i32 104, i32 1, metadata !23, i32 6} ; [ DW_TAG_lexical_block ]
!319 = metadata !{i32 107, i32 12, metadata !320, null}
!320 = metadata !{i32 720907, metadata !318, i32 106, i32 5, metadata !23, i32 7} ; [ DW_TAG_lexical_block ]
!321 = metadata !{i32 107, i32 27, metadata !320, null}
!322 = metadata !{i32 107, i32 41, metadata !320, null}
!323 = metadata !{i32 108, i32 12, metadata !320, null}
!324 = metadata !{i32 108, i32 27, metadata !320, null}
!325 = metadata !{i32 108, i32 41, metadata !320, null}
!326 = metadata !{i32 109, i32 9, metadata !320, null}
!327 = metadata !{i32 110, i32 9, metadata !320, null}
!328 = metadata !{i32 111, i32 5, metadata !320, null}
!329 = metadata !{i32 112, i32 5, metadata !318, null}
!330 = metadata !{i32 112, i32 12, metadata !318, null}
!331 = metadata !{i32 113, i32 5, metadata !318, null}
!332 = metadata !{i32 113, i32 12, metadata !318, null}
!333 = metadata !{i32 114, i32 5, metadata !318, null}
!334 = metadata !{i32 115, i32 1, metadata !318, null}
!335 = metadata !{i32 721153, metadata !37, metadata !"c", metadata !23, i32 16777333, metadata !254, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!336 = metadata !{i32 117, i32 41, metadata !37, null}
!337 = metadata !{i32 721153, metadata !37, metadata !"f_inp", metadata !23, i32 33554549, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!338 = metadata !{i32 117, i32 51, metadata !37, null}
!339 = metadata !{i32 721153, metadata !37, metadata !"f_out", metadata !23, i32 50331766, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!340 = metadata !{i32 118, i32 10, metadata !37, null}
!341 = metadata !{i32 721153, metadata !37, metadata !"barrel", metadata !23, i32 67108982, metadata !163, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!342 = metadata !{i32 118, i32 26, metadata !37, null}
!343 = metadata !{i32 721153, metadata !37, metadata !"write", metadata !23, i32 83886198, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!344 = metadata !{i32 118, i32 38, metadata !37, null}
!345 = metadata !{i32 721152, metadata !346, metadata !"n", metadata !23, i32 120, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!346 = metadata !{i32 720907, metadata !37, i32 119, i32 1, metadata !23, i32 8} ; [ DW_TAG_lexical_block ]
!347 = metadata !{i32 120, i32 12, metadata !346, null}
!348 = metadata !{i32 721152, metadata !346, metadata !"buffer", metadata !23, i32 121, metadata !279, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!349 = metadata !{i32 121, i32 17, metadata !346, null}
!350 = metadata !{i32 721152, metadata !346, metadata !"begin", metadata !23, i32 121, metadata !351, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!351 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !267} ; [ DW_TAG_pointer_type ]
!352 = metadata !{i32 121, i32 34, metadata !346, null}
!353 = metadata !{i32 721152, metadata !346, metadata !"bytes", metadata !23, i32 122, metadata !68, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!354 = metadata !{i32 122, i32 17, metadata !346, null}
!355 = metadata !{i32 122, i32 35, metadata !346, null}
!356 = metadata !{i32 721152, metadata !346, metadata !"control", metadata !23, i32 122, metadata !68, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!357 = metadata !{i32 122, i32 26, metadata !346, null}
!358 = metadata !{i32 124, i32 3, metadata !346, null}
!359 = metadata !{i32 721152, metadata !360, metadata !"i", metadata !23, i32 126, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!360 = metadata !{i32 720907, metadata !346, i32 125, i32 3, metadata !23, i32 9} ; [ DW_TAG_lexical_block ]
!361 = metadata !{i32 126, i32 16, metadata !360, null}
!362 = metadata !{i32 128, i32 7, metadata !360, null}
!363 = metadata !{i32 129, i32 10, metadata !360, null}
!364 = metadata !{i32 129, i32 23, metadata !360, null}
!365 = metadata !{i32 131, i32 11, metadata !360, null}
!366 = metadata !{i32 132, i32 7, metadata !360, null}
!367 = metadata !{i32 134, i32 7, metadata !360, null}
!368 = metadata !{i32 136, i32 9, metadata !369, null}
!369 = metadata !{i32 720907, metadata !360, i32 135, i32 7, metadata !23, i32 10} ; [ DW_TAG_lexical_block ]
!370 = metadata !{i32 137, i32 7, metadata !369, null}
!371 = metadata !{i32 139, i32 7, metadata !360, null}
!372 = metadata !{i32 140, i32 11, metadata !373, null}
!373 = metadata !{i32 720907, metadata !360, i32 140, i32 7, metadata !23, i32 11} ; [ DW_TAG_lexical_block ]
!374 = metadata !{i32 142, i32 9, metadata !375, null}
!375 = metadata !{i32 720907, metadata !373, i32 141, i32 7, metadata !23, i32 12} ; [ DW_TAG_lexical_block ]
!376 = metadata !{i32 142, i32 31, metadata !375, null}
!377 = metadata !{i32 143, i32 9, metadata !375, null}
!378 = metadata !{i32 144, i32 33, metadata !375, null}
!379 = metadata !{i32 145, i32 7, metadata !375, null}
!380 = metadata !{i32 140, i32 21, metadata !373, null}
!381 = metadata !{i32 147, i32 7, metadata !360, null}
!382 = metadata !{i32 149, i32 14, metadata !383, null}
!383 = metadata !{i32 720907, metadata !360, i32 148, i32 7, metadata !23, i32 13} ; [ DW_TAG_lexical_block ]
!384 = metadata !{i32 150, i32 14, metadata !383, null}
!385 = metadata !{i32 152, i32 11, metadata !386, null}
!386 = metadata !{i32 720907, metadata !383, i32 151, i32 9, metadata !23, i32 14} ; [ DW_TAG_lexical_block ]
!387 = metadata !{i32 153, i32 9, metadata !386, null}
!388 = metadata !{i32 154, i32 9, metadata !383, null}
!389 = metadata !{i32 155, i32 7, metadata !383, null}
!390 = metadata !{i32 158, i32 13, metadata !391, null}
!391 = metadata !{i32 720907, metadata !360, i32 157, i32 7, metadata !23, i32 15} ; [ DW_TAG_lexical_block ]
!392 = metadata !{i32 158, i32 49, metadata !393, null}
!393 = metadata !{i32 720907, metadata !391, i32 158, i32 48, metadata !23, i32 16} ; [ DW_TAG_lexical_block ]
!394 = metadata !{i32 158, i32 64, metadata !393, null}
!395 = metadata !{i32 159, i32 18, metadata !391, null}
!396 = metadata !{i32 159, i32 60, metadata !391, null}
!397 = metadata !{i32 160, i32 9, metadata !391, null}
!398 = metadata !{i32 163, i32 13, metadata !399, null}
!399 = metadata !{i32 720907, metadata !360, i32 163, i32 7, metadata !23, i32 17} ; [ DW_TAG_lexical_block ]
!400 = metadata !{i32 165, i32 9, metadata !401, null}
!401 = metadata !{i32 720907, metadata !399, i32 164, i32 7, metadata !23, i32 18} ; [ DW_TAG_lexical_block ]
!402 = metadata !{i32 166, i32 7, metadata !401, null}
!403 = metadata !{i32 163, i32 28, metadata !399, null}
!404 = metadata !{i32 167, i32 3, metadata !360, null}
!405 = metadata !{i32 168, i32 3, metadata !346, null}
!406 = metadata !{i32 170, i32 5, metadata !407, null}
!407 = metadata !{i32 720907, metadata !346, i32 169, i32 3, metadata !23, i32 19} ; [ DW_TAG_lexical_block ]
!408 = metadata !{i32 171, i32 3, metadata !407, null}
!409 = metadata !{i32 173, i32 3, metadata !346, null}
!410 = metadata !{i32 174, i32 1, metadata !346, null}
!411 = metadata !{i32 721153, metadata !40, metadata !"p", metadata !23, i32 16777398, metadata !412, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!412 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !413} ; [ DW_TAG_pointer_type ]
!413 = metadata !{i32 720918, null, metadata !"prng", metadata !23, i32 180, i64 0, i64 0, i64 0, i32 0, metadata !414} ; [ DW_TAG_typedef ]
!414 = metadata !{i32 720915, null, metadata !"", metadata !23, i32 176, i64 576, i64 32, i32 0, i32 0, null, metadata !415, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!415 = metadata !{metadata !416, metadata !417}
!416 = metadata !{i32 720909, metadata !414, metadata !"count", metadata !23, i32 178, i64 32, i64 32, i64 0, i32 0, metadata !163} ; [ DW_TAG_member ]
!417 = metadata !{i32 720909, metadata !414, metadata !"seed", metadata !23, i32 179, i64 544, i64 32, i64 32, i32 0, metadata !418} ; [ DW_TAG_member ]
!418 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 544, i64 32, i32 0, i32 0, metadata !67, metadata !419, i32 0, i32 0} ; [ DW_TAG_array_type ]
!419 = metadata !{metadata !420}
!420 = metadata !{i32 720929, i64 0, i64 16}      ; [ DW_TAG_subrange_type ]
!421 = metadata !{i32 182, i32 24, metadata !40, null}
!422 = metadata !{i32 184, i32 3, metadata !423, null}
!423 = metadata !{i32 720907, metadata !40, i32 183, i32 1, metadata !23, i32 20} ; [ DW_TAG_lexical_block ]
!424 = metadata !{i32 185, i32 1, metadata !423, null}
!425 = metadata !{i32 721153, metadata !43, metadata !"p", metadata !23, i32 16777403, metadata !412, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!426 = metadata !{i32 187, i32 30, metadata !43, null}
!427 = metadata !{i32 721153, metadata !43, metadata !"f_key", metadata !23, i32 33554619, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!428 = metadata !{i32 187, i32 40, metadata !43, null}
!429 = metadata !{i32 721152, metadata !430, metadata !"c", metadata !23, i32 189, metadata !431, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!430 = metadata !{i32 720907, metadata !43, i32 188, i32 1, metadata !23, i32 21} ; [ DW_TAG_lexical_block ]
!431 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 736, i64 32, i32 0, i32 0, metadata !255, metadata !149, i32 0, i32 0} ; [ DW_TAG_array_type ]
!432 = metadata !{i32 189, i32 16, metadata !430, null}
!433 = metadata !{i32 190, i32 3, metadata !430, null}
!434 = metadata !{i32 191, i32 3, metadata !430, null}
!435 = metadata !{i32 192, i32 3, metadata !430, null}
!436 = metadata !{i32 193, i32 3, metadata !430, null}
!437 = metadata !{i32 194, i32 1, metadata !430, null}
!438 = metadata !{i32 721153, metadata !46, metadata !"c", metadata !23, i32 16777412, metadata !254, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!439 = metadata !{i32 196, i32 33, metadata !46, null}
!440 = metadata !{i32 721152, metadata !441, metadata !"b", metadata !23, i32 199, metadata !267, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!441 = metadata !{i32 720907, metadata !46, i32 197, i32 1, metadata !23, i32 22} ; [ DW_TAG_lexical_block ]
!442 = metadata !{i32 199, i32 17, metadata !441, null}
!443 = metadata !{i32 200, i32 9, metadata !444, null}
!444 = metadata !{i32 720907, metadata !441, i32 200, i32 3, metadata !23, i32 23} ; [ DW_TAG_lexical_block ]
!445 = metadata !{i32 202, i32 5, metadata !446, null}
!446 = metadata !{i32 720907, metadata !444, i32 201, i32 3, metadata !23, i32 24} ; [ DW_TAG_lexical_block ]
!447 = metadata !{i32 203, i32 5, metadata !446, null}
!448 = metadata !{i32 204, i32 3, metadata !446, null}
!449 = metadata !{i32 200, i32 19, metadata !444, null}
!450 = metadata !{i32 205, i32 1, metadata !441, null}
!451 = metadata !{i32 721153, metadata !49, metadata !"p", metadata !23, i32 16777423, metadata !412, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!452 = metadata !{i32 207, i32 27, metadata !49, null}
!453 = metadata !{i32 721153, metadata !49, metadata !"f_inp", metadata !23, i32 33554639, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!454 = metadata !{i32 207, i32 37, metadata !49, null}
!455 = metadata !{i32 721153, metadata !49, metadata !"barrel", metadata !23, i32 50331855, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!456 = metadata !{i32 207, i32 48, metadata !49, null}
!457 = metadata !{i32 721152, metadata !458, metadata !"b", metadata !23, i32 210, metadata !267, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!458 = metadata !{i32 720907, metadata !49, i32 208, i32 1, metadata !23, i32 25} ; [ DW_TAG_lexical_block ]
!459 = metadata !{i32 210, i32 17, metadata !458, null}
!460 = metadata !{i32 721152, metadata !458, metadata !"c", metadata !23, i32 211, metadata !461, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!461 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 1472, i64 32, i32 0, i32 0, metadata !255, metadata !263, i32 0, i32 0} ; [ DW_TAG_array_type ]
!462 = metadata !{i32 211, i32 16, metadata !458, null}
!463 = metadata !{i32 212, i32 9, metadata !464, null}
!464 = metadata !{i32 720907, metadata !458, i32 212, i32 3, metadata !23, i32 26} ; [ DW_TAG_lexical_block ]
!465 = metadata !{i32 214, i32 5, metadata !466, null}
!466 = metadata !{i32 720907, metadata !464, i32 213, i32 3, metadata !23, i32 27} ; [ DW_TAG_lexical_block ]
!467 = metadata !{i32 215, i32 5, metadata !466, null}
!468 = metadata !{i32 215, i32 17, metadata !466, null}
!469 = metadata !{i32 216, i32 3, metadata !466, null}
!470 = metadata !{i32 212, i32 24, metadata !464, null}
!471 = metadata !{i32 217, i32 3, metadata !458, null}
!472 = metadata !{i32 218, i32 9, metadata !473, null}
!473 = metadata !{i32 720907, metadata !458, i32 218, i32 3, metadata !23, i32 28} ; [ DW_TAG_lexical_block ]
!474 = metadata !{i32 220, i32 5, metadata !475, null}
!475 = metadata !{i32 720907, metadata !473, i32 219, i32 3, metadata !23, i32 29} ; [ DW_TAG_lexical_block ]
!476 = metadata !{i32 221, i32 3, metadata !475, null}
!477 = metadata !{i32 218, i32 24, metadata !473, null}
!478 = metadata !{i32 222, i32 3, metadata !458, null}
!479 = metadata !{i32 223, i32 1, metadata !458, null}
!480 = metadata !{i32 721153, metadata !52, metadata !"p", metadata !23, i32 16777441, metadata !412, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!481 = metadata !{i32 225, i32 24, metadata !52, null}
!482 = metadata !{i32 721153, metadata !52, metadata !"f_inp", metadata !23, i32 33554657, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!483 = metadata !{i32 225, i32 34, metadata !52, null}
!484 = metadata !{i32 721153, metadata !52, metadata !"f_out", metadata !23, i32 50331873, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!485 = metadata !{i32 225, i32 48, metadata !52, null}
!486 = metadata !{i32 721152, metadata !487, metadata !"c", metadata !23, i32 227, metadata !461, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!487 = metadata !{i32 720907, metadata !52, i32 226, i32 1, metadata !23, i32 30} ; [ DW_TAG_lexical_block ]
!488 = metadata !{i32 227, i32 16, metadata !487, null}
!489 = metadata !{i32 229, i32 3, metadata !487, null}
!490 = metadata !{i32 230, i32 3, metadata !487, null}
!491 = metadata !{i32 231, i32 3, metadata !487, null}
!492 = metadata !{i32 232, i32 3, metadata !487, null}
!493 = metadata !{i32 233, i32 3, metadata !487, null}
!494 = metadata !{i32 234, i32 3, metadata !487, null}
!495 = metadata !{i32 235, i32 3, metadata !487, null}
!496 = metadata !{i32 236, i32 1, metadata !487, null}
!497 = metadata !{i32 721153, metadata !55, metadata !"f_inp", metadata !23, i32 16777454, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!498 = metadata !{i32 238, i32 21, metadata !55, null}
!499 = metadata !{i32 240, i32 3, metadata !500, null}
!500 = metadata !{i32 720907, metadata !55, i32 239, i32 1, metadata !23, i32 31} ; [ DW_TAG_lexical_block ]
!501 = metadata !{i32 240, i32 10, metadata !500, null}
!502 = metadata !{i32 721152, metadata !503, metadata !"buffer", metadata !23, i32 242, metadata !504, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!503 = metadata !{i32 720907, metadata !500, i32 241, i32 3, metadata !23, i32 32} ; [ DW_TAG_lexical_block ]
!504 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8192, i64 8, i32 0, i32 0, metadata !118, metadata !505, i32 0, i32 0} ; [ DW_TAG_array_type ]
!505 = metadata !{metadata !506}
!506 = metadata !{i32 720929, i64 0, i64 1023}    ; [ DW_TAG_subrange_type ]
!507 = metadata !{i32 242, i32 12, metadata !503, null}
!508 = metadata !{i32 243, i32 7, metadata !503, null}
!509 = metadata !{i32 244, i32 11, metadata !503, null}
!510 = metadata !{i32 244, i32 48, metadata !503, null}
!511 = metadata !{i32 245, i32 3, metadata !503, null}
!512 = metadata !{i32 246, i32 6, metadata !500, null}
!513 = metadata !{i32 248, i32 5, metadata !514, null}
!514 = metadata !{i32 720907, metadata !500, i32 247, i32 3, metadata !23, i32 33} ; [ DW_TAG_lexical_block ]
!515 = metadata !{i32 249, i32 5, metadata !514, null}
!516 = metadata !{i32 251, i32 3, metadata !500, null}
!517 = metadata !{i32 252, i32 1, metadata !500, null}
!518 = metadata !{i32 721153, metadata !58, metadata !"p", metadata !23, i32 16777470, metadata !412, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!519 = metadata !{i32 254, i32 22, metadata !58, null}
!520 = metadata !{i32 721153, metadata !58, metadata !"f_inp", metadata !23, i32 33554686, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!521 = metadata !{i32 254, i32 32, metadata !58, null}
!522 = metadata !{i32 721153, metadata !58, metadata !"f_out", metadata !23, i32 50331902, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!523 = metadata !{i32 254, i32 46, metadata !58, null}
!524 = metadata !{i32 721152, metadata !525, metadata !"c", metadata !23, i32 256, metadata !461, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!525 = metadata !{i32 720907, metadata !58, i32 255, i32 1, metadata !23, i32 34} ; [ DW_TAG_lexical_block ]
!526 = metadata !{i32 256, i32 16, metadata !525, null}
!527 = metadata !{i32 257, i32 3, metadata !525, null}
!528 = metadata !{i32 259, i32 7, metadata !525, null}
!529 = metadata !{i32 259, i32 24, metadata !525, null}
!530 = metadata !{i32 260, i32 3, metadata !525, null}
!531 = metadata !{i32 261, i32 3, metadata !525, null}
!532 = metadata !{i32 262, i32 3, metadata !525, null}
!533 = metadata !{i32 263, i32 3, metadata !525, null}
!534 = metadata !{i32 265, i32 3, metadata !525, null}
!535 = metadata !{i32 266, i32 1, metadata !525, null}
!536 = metadata !{i32 721153, metadata !61, metadata !"p", metadata !23, i32 16777484, metadata !412, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!537 = metadata !{i32 268, i32 28, metadata !61, null}
!538 = metadata !{i32 270, i32 29, metadata !539, null}
!539 = metadata !{i32 720907, metadata !61, i32 269, i32 1, metadata !23, i32 35} ; [ DW_TAG_lexical_block ]
!540 = metadata !{i32 271, i32 3, metadata !539, null}
!541 = metadata !{i32 272, i32 1, metadata !539, null}
!542 = metadata !{i32 721153, metadata !64, metadata !"p", metadata !23, i32 16777490, metadata !412, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!543 = metadata !{i32 274, i32 26, metadata !64, null}
!544 = metadata !{i32 721152, metadata !545, metadata !"tmp", metadata !23, i32 276, metadata !546, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!545 = metadata !{i32 720907, metadata !64, i32 275, i32 1, metadata !23, i32 36} ; [ DW_TAG_lexical_block ]
!546 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 160, i64 32, i32 0, i32 0, metadata !67, metadata !214, i32 0, i32 0} ; [ DW_TAG_array_type ]
!547 = metadata !{i32 276, i32 10, metadata !545, null}
!548 = metadata !{i32 721152, metadata !545, metadata !"buffer", metadata !23, i32 277, metadata !549, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!549 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 544, i64 8, i32 0, i32 0, metadata !550, metadata !551, i32 0, i32 0} ; [ DW_TAG_array_type ]
!550 = metadata !{i32 720918, null, metadata !"byte", metadata !23, i32 10, i64 0, i64 0, i64 0, i32 0, metadata !267} ; [ DW_TAG_typedef ]
!551 = metadata !{metadata !552}
!552 = metadata !{i32 720929, i64 0, i64 67}      ; [ DW_TAG_subrange_type ]
!553 = metadata !{i32 277, i32 8, metadata !545, null}
!554 = metadata !{i32 721152, metadata !545, metadata !"i", metadata !23, i32 278, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!555 = metadata !{i32 278, i32 12, metadata !545, null}
!556 = metadata !{i32 721152, metadata !545, metadata !"j", metadata !23, i32 278, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!557 = metadata !{i32 278, i32 14, metadata !545, null}
!558 = metadata !{i32 721152, metadata !545, metadata !"c", metadata !23, i32 279, metadata !255, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!559 = metadata !{i32 279, i32 16, metadata !545, null}
!560 = metadata !{i32 281, i32 3, metadata !545, null}
!561 = metadata !{i32 282, i32 9, metadata !562, null}
!562 = metadata !{i32 720907, metadata !545, i32 282, i32 3, metadata !23, i32 37} ; [ DW_TAG_lexical_block ]
!563 = metadata !{i32 284, i32 11, metadata !564, null}
!564 = metadata !{i32 720907, metadata !565, i32 284, i32 5, metadata !23, i32 39} ; [ DW_TAG_lexical_block ]
!565 = metadata !{i32 720907, metadata !562, i32 283, i32 3, metadata !23, i32 38} ; [ DW_TAG_lexical_block ]
!566 = metadata !{i32 286, i32 7, metadata !567, null}
!567 = metadata !{i32 720907, metadata !564, i32 285, i32 5, metadata !23, i32 40} ; [ DW_TAG_lexical_block ]
!568 = metadata !{i32 287, i32 5, metadata !567, null}
!569 = metadata !{i32 284, i32 25, metadata !564, null}
!570 = metadata !{i32 288, i32 3, metadata !565, null}
!571 = metadata !{i32 282, i32 30, metadata !562, null}
!572 = metadata !{i32 290, i32 3, metadata !545, null}
!573 = metadata !{i32 291, i32 3, metadata !545, null}
!574 = metadata !{i32 292, i32 3, metadata !545, null}
!575 = metadata !{i32 293, i32 3, metadata !545, null}
!576 = metadata !{i32 294, i32 3, metadata !545, null}
!577 = metadata !{i32 721153, metadata !71, metadata !"p", metadata !23, i32 16777513, metadata !412, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!578 = metadata !{i32 297, i32 28, metadata !71, null}
!579 = metadata !{i32 721153, metadata !71, metadata !"V", metadata !23, i32 33554729, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!580 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !581} ; [ DW_TAG_pointer_type ]
!581 = metadata !{i32 720918, null, metadata !"word16", metadata !23, i32 11, i64 0, i64 0, i64 0, i32 0, metadata !144} ; [ DW_TAG_typedef ]
!582 = metadata !{i32 297, i32 39, metadata !71, null}
!583 = metadata !{i32 721152, metadata !584, metadata !"i", metadata !23, i32 299, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!584 = metadata !{i32 720907, metadata !71, i32 298, i32 1, metadata !23, i32 41} ; [ DW_TAG_lexical_block ]
!585 = metadata !{i32 299, i32 12, metadata !584, null}
!586 = metadata !{i32 300, i32 3, metadata !584, null}
!587 = metadata !{i32 301, i32 8, metadata !588, null}
!588 = metadata !{i32 720907, metadata !584, i32 301, i32 3, metadata !23, i32 42} ; [ DW_TAG_lexical_block ]
!589 = metadata !{i32 302, i32 29, metadata !588, null}
!590 = metadata !{i32 301, i32 17, metadata !588, null}
!591 = metadata !{i32 303, i32 1, metadata !584, null}
!592 = metadata !{i32 721153, metadata !74, metadata !"mac", metadata !23, i32 16777521, metadata !593, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!593 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !67} ; [ DW_TAG_pointer_type ]
!594 = metadata !{i32 305, i32 30, metadata !74, null}
!595 = metadata !{i32 721153, metadata !74, metadata !"V", metadata !23, i32 33554737, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!596 = metadata !{i32 305, i32 43, metadata !74, null}
!597 = metadata !{i32 721152, metadata !598, metadata !"i", metadata !23, i32 307, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!598 = metadata !{i32 720907, metadata !74, i32 306, i32 1, metadata !23, i32 43} ; [ DW_TAG_lexical_block ]
!599 = metadata !{i32 307, i32 12, metadata !598, null}
!600 = metadata !{i32 308, i32 3, metadata !598, null}
!601 = metadata !{i32 309, i32 8, metadata !602, null}
!602 = metadata !{i32 720907, metadata !598, i32 309, i32 3, metadata !23, i32 44} ; [ DW_TAG_lexical_block ]
!603 = metadata !{i32 721152, metadata !604, metadata !"x", metadata !23, i32 311, metadata !67, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!604 = metadata !{i32 720907, metadata !602, i32 310, i32 3, metadata !23, i32 45} ; [ DW_TAG_lexical_block ]
!605 = metadata !{i32 311, i32 12, metadata !604, null}
!606 = metadata !{i32 311, i32 22, metadata !604, null}
!607 = metadata !{i32 312, i32 5, metadata !604, null}
!608 = metadata !{i32 313, i32 5, metadata !604, null}
!609 = metadata !{i32 314, i32 3, metadata !604, null}
!610 = metadata !{i32 309, i32 16, metadata !602, null}
!611 = metadata !{i32 315, i32 1, metadata !598, null}
!612 = metadata !{i32 721153, metadata !77, metadata !"f", metadata !23, i32 16777533, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!613 = metadata !{i32 317, i32 23, metadata !77, null}
!614 = metadata !{i32 721153, metadata !77, metadata !"v", metadata !23, i32 33554749, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!615 = metadata !{i32 317, i32 34, metadata !77, null}
!616 = metadata !{i32 721152, metadata !617, metadata !"u", metadata !23, i32 319, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!617 = metadata !{i32 720907, metadata !77, i32 318, i32 1, metadata !23, i32 46} ; [ DW_TAG_lexical_block ]
!618 = metadata !{i32 319, i32 12, metadata !617, null}
!619 = metadata !{i32 721152, metadata !617, metadata !"w", metadata !23, i32 320, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!620 = metadata !{i32 720918, null, metadata !"vlPoint", metadata !23, i32 17, i64 0, i64 0, i64 0, i32 0, metadata !621} ; [ DW_TAG_typedef ]
!621 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 304, i64 16, i32 0, i32 0, metadata !581, metadata !622, i32 0, i32 0} ; [ DW_TAG_array_type ]
!622 = metadata !{metadata !623}
!623 = metadata !{i32 720929, i64 0, i64 18}      ; [ DW_TAG_subrange_type ]
!624 = metadata !{i32 320, i32 11, metadata !617, null}
!625 = metadata !{i32 321, i32 3, metadata !617, null}
!626 = metadata !{i32 322, i32 3, metadata !617, null}
!627 = metadata !{i32 323, i32 3, metadata !617, null}
!628 = metadata !{i32 325, i32 9, metadata !629, null}
!629 = metadata !{i32 720907, metadata !617, i32 324, i32 3, metadata !23, i32 47} ; [ DW_TAG_lexical_block ]
!630 = metadata !{i32 326, i32 5, metadata !629, null}
!631 = metadata !{i32 327, i32 7, metadata !629, null}
!632 = metadata !{i32 328, i32 10, metadata !629, null}
!633 = metadata !{i32 329, i32 7, metadata !629, null}
!634 = metadata !{i32 330, i32 10, metadata !629, null}
!635 = metadata !{i32 331, i32 7, metadata !629, null}
!636 = metadata !{i32 332, i32 10, metadata !629, null}
!637 = metadata !{i32 333, i32 7, metadata !629, null}
!638 = metadata !{i32 335, i32 7, metadata !629, null}
!639 = metadata !{i32 336, i32 5, metadata !629, null}
!640 = metadata !{i32 337, i32 5, metadata !629, null}
!641 = metadata !{i32 338, i32 5, metadata !629, null}
!642 = metadata !{i32 339, i32 3, metadata !629, null}
!643 = metadata !{i32 340, i32 1, metadata !617, null}
!644 = metadata !{i32 721153, metadata !80, metadata !"f", metadata !23, i32 16777558, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!645 = metadata !{i32 342, i32 25, metadata !80, null}
!646 = metadata !{i32 721153, metadata !80, metadata !"v", metadata !23, i32 33554774, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!647 = metadata !{i32 342, i32 36, metadata !80, null}
!648 = metadata !{i32 721152, metadata !649, metadata !"i", metadata !23, i32 344, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!649 = metadata !{i32 720907, metadata !80, i32 343, i32 1, metadata !23, i32 48} ; [ DW_TAG_lexical_block ]
!650 = metadata !{i32 344, i32 12, metadata !649, null}
!651 = metadata !{i32 344, i32 15, metadata !649, null}
!652 = metadata !{i32 721152, metadata !649, metadata !"buffer", metadata !23, i32 345, metadata !653, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!653 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 2048, i64 8, i32 0, i32 0, metadata !118, metadata !654, i32 0, i32 0} ; [ DW_TAG_array_type ]
!654 = metadata !{metadata !655}
!655 = metadata !{i32 720929, i64 0, i64 255}     ; [ DW_TAG_subrange_type ]
!656 = metadata !{i32 345, i32 8, metadata !649, null}
!657 = metadata !{i32 721152, metadata !649, metadata !"u", metadata !23, i32 345, metadata !118, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!658 = metadata !{i32 345, i32 21, metadata !649, null}
!659 = metadata !{i32 721152, metadata !649, metadata !"w", metadata !23, i32 347, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!660 = metadata !{i32 347, i32 11, metadata !649, null}
!661 = metadata !{i32 348, i32 3, metadata !649, null}
!662 = metadata !{i32 349, i32 3, metadata !649, null}
!663 = metadata !{i32 350, i32 3, metadata !649, null}
!664 = metadata !{i32 351, i32 3, metadata !649, null}
!665 = metadata !{i32 353, i32 3, metadata !649, null}
!666 = metadata !{i32 355, i32 5, metadata !667, null}
!667 = metadata !{i32 720907, metadata !649, i32 354, i32 3, metadata !23, i32 49} ; [ DW_TAG_lexical_block ]
!668 = metadata !{i32 356, i32 7, metadata !667, null}
!669 = metadata !{i32 357, i32 10, metadata !667, null}
!670 = metadata !{i32 358, i32 7, metadata !667, null}
!671 = metadata !{i32 359, i32 10, metadata !667, null}
!672 = metadata !{i32 360, i32 7, metadata !667, null}
!673 = metadata !{i32 361, i32 10, metadata !667, null}
!674 = metadata !{i32 362, i32 7, metadata !667, null}
!675 = metadata !{i32 364, i32 7, metadata !667, null}
!676 = metadata !{i32 365, i32 5, metadata !667, null}
!677 = metadata !{i32 366, i32 5, metadata !667, null}
!678 = metadata !{i32 367, i32 5, metadata !667, null}
!679 = metadata !{i32 368, i32 3, metadata !667, null}
!680 = metadata !{i32 369, i32 1, metadata !649, null}
!681 = metadata !{i32 721153, metadata !83, metadata !"v", metadata !23, i32 16777589, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!682 = metadata !{i32 373, i32 25, metadata !83, null}
!683 = metadata !{i32 721152, metadata !684, metadata !"i", metadata !23, i32 375, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!684 = metadata !{i32 720907, metadata !83, i32 374, i32 1, metadata !23, i32 50} ; [ DW_TAG_lexical_block ]
!685 = metadata !{i32 375, i32 12, metadata !684, null}
!686 = metadata !{i32 721152, metadata !684, metadata !"j", metadata !23, i32 375, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!687 = metadata !{i32 375, i32 14, metadata !684, null}
!688 = metadata !{i32 376, i32 8, metadata !689, null}
!689 = metadata !{i32 720907, metadata !684, i32 376, i32 3, metadata !23, i32 51} ; [ DW_TAG_lexical_block ]
!690 = metadata !{i32 721152, metadata !691, metadata !"x", metadata !23, i32 378, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!691 = metadata !{i32 720907, metadata !689, i32 377, i32 3, metadata !23, i32 52} ; [ DW_TAG_lexical_block ]
!692 = metadata !{i32 378, i32 14, metadata !691, null}
!693 = metadata !{i32 378, i32 22, metadata !691, null}
!694 = metadata !{i32 379, i32 10, metadata !695, null}
!695 = metadata !{i32 720907, metadata !691, i32 379, i32 5, metadata !23, i32 53} ; [ DW_TAG_lexical_block ]
!696 = metadata !{i32 380, i32 7, metadata !695, null}
!697 = metadata !{i32 379, i32 18, metadata !695, null}
!698 = metadata !{i32 381, i32 3, metadata !691, null}
!699 = metadata !{i32 376, i32 25, metadata !689, null}
!700 = metadata !{i32 382, i32 1, metadata !684, null}
!701 = metadata !{i32 721153, metadata !86, metadata !"f", metadata !23, i32 16777600, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!702 = metadata !{i32 384, i32 30, metadata !86, null}
!703 = metadata !{i32 721153, metadata !86, metadata !"v", metadata !23, i32 33554816, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!704 = metadata !{i32 384, i32 41, metadata !86, null}
!705 = metadata !{i32 721152, metadata !706, metadata !"n", metadata !23, i32 386, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!706 = metadata !{i32 720907, metadata !86, i32 385, i32 1, metadata !23, i32 54} ; [ DW_TAG_lexical_block ]
!707 = metadata !{i32 386, i32 12, metadata !706, null}
!708 = metadata !{i32 386, i32 30, metadata !706, null}
!709 = metadata !{i32 387, i32 3, metadata !706, null}
!710 = metadata !{i32 389, i32 5, metadata !711, null}
!711 = metadata !{i32 720907, metadata !706, i32 388, i32 3, metadata !23, i32 55} ; [ DW_TAG_lexical_block ]
!712 = metadata !{i32 389, i32 20, metadata !711, null}
!713 = metadata !{i32 390, i32 5, metadata !711, null}
!714 = metadata !{i32 391, i32 5, metadata !711, null}
!715 = metadata !{i32 392, i32 3, metadata !711, null}
!716 = metadata !{i32 393, i32 1, metadata !706, null}
!717 = metadata !{i32 721153, metadata !89, metadata !"f", metadata !23, i32 16777611, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!718 = metadata !{i32 395, i32 29, metadata !89, null}
!719 = metadata !{i32 721153, metadata !89, metadata !"v", metadata !23, i32 33554827, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!720 = metadata !{i32 395, i32 40, metadata !89, null}
!721 = metadata !{i32 721152, metadata !722, metadata !"u", metadata !23, i32 397, metadata !723, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!722 = metadata !{i32 720907, metadata !89, i32 396, i32 1, metadata !23, i32 56} ; [ DW_TAG_lexical_block ]
!723 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 256, i64 8, i32 0, i32 0, metadata !550, metadata !198, i32 0, i32 0} ; [ DW_TAG_array_type ]
!724 = metadata !{i32 397, i32 8, metadata !722, null}
!725 = metadata !{i32 721152, metadata !722, metadata !"w", metadata !23, i32 398, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!726 = metadata !{i32 398, i32 11, metadata !722, null}
!727 = metadata !{i32 721152, metadata !722, metadata !"n", metadata !23, i32 399, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!728 = metadata !{i32 399, i32 12, metadata !722, null}
!729 = metadata !{i32 399, i32 30, metadata !722, null}
!730 = metadata !{i32 400, i32 3, metadata !722, null}
!731 = metadata !{i32 401, i32 3, metadata !722, null}
!732 = metadata !{i32 401, i32 16, metadata !722, null}
!733 = metadata !{i32 402, i32 3, metadata !722, null}
!734 = metadata !{i32 404, i32 5, metadata !735, null}
!735 = metadata !{i32 720907, metadata !722, i32 403, i32 3, metadata !23, i32 57} ; [ DW_TAG_lexical_block ]
!736 = metadata !{i32 405, i32 5, metadata !735, null}
!737 = metadata !{i32 406, i32 5, metadata !735, null}
!738 = metadata !{i32 407, i32 3, metadata !735, null}
!739 = metadata !{i32 408, i32 1, metadata !722, null}
!740 = metadata !{i32 721153, metadata !92, metadata !"V", metadata !23, i32 16777630, metadata !741, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!741 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !742} ; [ DW_TAG_pointer_type ]
!742 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !581} ; [ DW_TAG_const_type ]
!743 = metadata !{i32 414, i32 43, metadata !92, null}
!744 = metadata !{i32 721153, metadata !92, metadata !"key", metadata !23, i32 33554846, metadata !745, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!745 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !550} ; [ DW_TAG_pointer_type ]
!746 = metadata !{i32 414, i32 58, metadata !92, null}
!747 = metadata !{i32 721152, metadata !748, metadata !"v", metadata !23, i32 416, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!748 = metadata !{i32 720907, metadata !92, i32 415, i32 1, metadata !23, i32 58} ; [ DW_TAG_lexical_block ]
!749 = metadata !{i32 416, i32 11, metadata !748, null}
!750 = metadata !{i32 721152, metadata !748, metadata !"j", metadata !23, i32 417, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!751 = metadata !{i32 417, i32 12, metadata !748, null}
!752 = metadata !{i32 418, i32 3, metadata !748, null}
!753 = metadata !{i32 419, i32 8, metadata !754, null}
!754 = metadata !{i32 720907, metadata !748, i32 419, i32 3, metadata !23, i32 59} ; [ DW_TAG_lexical_block ]
!755 = metadata !{i32 421, i32 5, metadata !756, null}
!756 = metadata !{i32 720907, metadata !754, i32 420, i32 3, metadata !23, i32 60} ; [ DW_TAG_lexical_block ]
!757 = metadata !{i32 421, i32 20, metadata !756, null}
!758 = metadata !{i32 422, i32 5, metadata !756, null}
!759 = metadata !{i32 423, i32 5, metadata !756, null}
!760 = metadata !{i32 424, i32 3, metadata !756, null}
!761 = metadata !{i32 419, i32 41, metadata !754, null}
!762 = metadata !{i32 425, i32 1, metadata !748, null}
!763 = metadata !{i32 721153, metadata !95, metadata !"iv", metadata !23, i32 16777643, metadata !745, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!764 = metadata !{i32 427, i32 29, metadata !95, null}
!765 = metadata !{i32 721152, metadata !766, metadata !"i", metadata !23, i32 429, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!766 = metadata !{i32 720907, metadata !95, i32 428, i32 1, metadata !23, i32 61} ; [ DW_TAG_lexical_block ]
!767 = metadata !{i32 429, i32 7, metadata !766, null}
!768 = metadata !{i32 429, i32 12, metadata !766, null}
!769 = metadata !{i32 430, i32 3, metadata !766, null}
!770 = metadata !{i32 430, i32 23, metadata !766, null}
!771 = metadata !{i32 431, i32 3, metadata !766, null}
!772 = metadata !{i32 432, i32 1, metadata !766, null}
!773 = metadata !{i32 721153, metadata !98, metadata !"secret", metadata !23, i32 16777650, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!774 = metadata !{i32 434, i32 26, metadata !98, null}
!775 = metadata !{i32 721153, metadata !98, metadata !"f_inp", metadata !23, i32 33554866, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!776 = metadata !{i32 434, i32 41, metadata !98, null}
!777 = metadata !{i32 721153, metadata !98, metadata !"f_out", metadata !23, i32 50332082, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!778 = metadata !{i32 434, i32 55, metadata !98, null}
!779 = metadata !{i32 721152, metadata !780, metadata !"key", metadata !23, i32 436, metadata !781, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!780 = metadata !{i32 720907, metadata !98, i32 435, i32 1, metadata !23, i32 62} ; [ DW_TAG_lexical_block ]
!781 = metadata !{i32 720918, null, metadata !"squareBlock", metadata !23, i32 20, i64 0, i64 0, i64 0, i32 0, metadata !782} ; [ DW_TAG_typedef ]
!782 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 128, i64 8, i32 0, i32 0, metadata !550, metadata !241, i32 0, i32 0} ; [ DW_TAG_array_type ]
!783 = metadata !{i32 436, i32 15, metadata !780, null}
!784 = metadata !{i32 721152, metadata !780, metadata !"iv", metadata !23, i32 436, metadata !781, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!785 = metadata !{i32 436, i32 19, metadata !780, null}
!786 = metadata !{i32 721152, metadata !780, metadata !"ctx", metadata !23, i32 437, metadata !787, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!787 = metadata !{i32 720918, null, metadata !"squareCtsContext", metadata !23, i32 21, i64 0, i64 0, i64 0, i32 0, metadata !788} ; [ DW_TAG_typedef ]
!788 = metadata !{i32 720915, null, metadata !"", metadata !789, i32 18, i64 2432, i64 32, i32 0, i32 0, null, metadata !790, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!789 = metadata !{i32 720937, metadata !"sqcts.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!790 = metadata !{metadata !791, metadata !796, metadata !797}
!791 = metadata !{i32 720909, metadata !788, metadata !"roundKeys_e", metadata !789, i32 19, i64 1152, i64 32, i64 0, i32 0, metadata !792} ; [ DW_TAG_member ]
!792 = metadata !{i32 720918, null, metadata !"squareKeySchedule", metadata !789, i32 21, i64 0, i64 0, i64 0, i32 0, metadata !793} ; [ DW_TAG_typedef ]
!793 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 1152, i64 32, i32 0, i32 0, metadata !67, metadata !794, i32 0, i32 0} ; [ DW_TAG_array_type ]
!794 = metadata !{metadata !795, metadata !226}
!795 = metadata !{i32 720929, i64 0, i64 8}       ; [ DW_TAG_subrange_type ]
!796 = metadata !{i32 720909, metadata !788, metadata !"roundKeys_d", metadata !789, i32 19, i64 1152, i64 32, i64 1152, i32 0, metadata !792} ; [ DW_TAG_member ]
!797 = metadata !{i32 720909, metadata !788, metadata !"mask", metadata !789, i32 20, i64 128, i64 8, i64 2304, i32 0, metadata !782} ; [ DW_TAG_member ]
!798 = metadata !{i32 437, i32 20, metadata !780, null}
!799 = metadata !{i32 721152, metadata !780, metadata !"buffer", metadata !23, i32 438, metadata !800, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!800 = metadata !{i32 720918, null, metadata !"big_buf", metadata !23, i32 411, i64 0, i64 0, i64 0, i32 0, metadata !801} ; [ DW_TAG_typedef ]
!801 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 32800, i64 32, i32 0, i32 0, metadata !67, metadata !802, i32 0, i32 0} ; [ DW_TAG_array_type ]
!802 = metadata !{metadata !803}
!803 = metadata !{i32 720929, i64 0, i64 1024}    ; [ DW_TAG_subrange_type ]
!804 = metadata !{i32 438, i32 11, metadata !780, null}
!805 = metadata !{i32 721152, metadata !780, metadata !"n", metadata !23, i32 440, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!806 = metadata !{i32 440, i32 7, metadata !780, null}
!807 = metadata !{i32 721152, metadata !780, metadata !"err", metadata !23, i32 440, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!808 = metadata !{i32 440, i32 9, metadata !780, null}
!809 = metadata !{i32 440, i32 16, metadata !780, null}
!810 = metadata !{i32 721152, metadata !780, metadata !"pad", metadata !23, i32 441, metadata !550, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!811 = metadata !{i32 441, i32 8, metadata !780, null}
!812 = metadata !{i32 443, i32 3, metadata !780, null}
!813 = metadata !{i32 444, i32 3, metadata !780, null}
!814 = metadata !{i32 445, i32 2, metadata !780, null}
!815 = metadata !{i32 446, i32 3, metadata !780, null}
!816 = metadata !{i32 447, i32 3, metadata !780, null}
!817 = metadata !{i32 447, i32 14, metadata !780, null}
!818 = metadata !{i32 449, i32 5, metadata !819, null}
!819 = metadata !{i32 720907, metadata !780, i32 448, i32 3, metadata !23, i32 63} ; [ DW_TAG_lexical_block ]
!820 = metadata !{i32 451, i32 7, metadata !821, null}
!821 = metadata !{i32 720907, metadata !819, i32 450, i32 5, metadata !23, i32 64} ; [ DW_TAG_lexical_block ]
!822 = metadata !{i32 452, i32 7, metadata !821, null}
!823 = metadata !{i32 453, i32 9, metadata !821, null}
!824 = metadata !{i32 454, i32 12, metadata !821, null}
!825 = metadata !{i32 455, i32 9, metadata !821, null}
!826 = metadata !{i32 456, i32 7, metadata !821, null}
!827 = metadata !{i32 457, i32 7, metadata !821, null}
!828 = metadata !{i32 458, i32 5, metadata !821, null}
!829 = metadata !{i32 459, i32 5, metadata !819, null}
!830 = metadata !{i32 460, i32 5, metadata !819, null}
!831 = metadata !{i32 461, i32 5, metadata !819, null}
!832 = metadata !{i32 721152, metadata !833, metadata !"written", metadata !23, i32 463, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!833 = metadata !{i32 720907, metadata !819, i32 462, i32 5, metadata !23, i32 65} ; [ DW_TAG_lexical_block ]
!834 = metadata !{i32 463, i32 11, metadata !833, null}
!835 = metadata !{i32 463, i32 21, metadata !833, null}
!836 = metadata !{i32 464, i32 7, metadata !833, null}
!837 = metadata !{i32 466, i32 9, metadata !838, null}
!838 = metadata !{i32 720907, metadata !833, i32 465, i32 7, metadata !23, i32 66} ; [ DW_TAG_lexical_block ]
!839 = metadata !{i32 467, i32 9, metadata !838, null}
!840 = metadata !{i32 468, i32 9, metadata !838, null}
!841 = metadata !{i32 471, i32 3, metadata !819, null}
!842 = metadata !{i32 473, i32 3, metadata !780, null}
!843 = metadata !{i32 474, i32 3, metadata !780, null}
!844 = metadata !{i32 475, i32 3, metadata !780, null}
!845 = metadata !{i32 721153, metadata !101, metadata !"secret", metadata !23, i32 16777694, metadata !580, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!846 = metadata !{i32 478, i32 26, metadata !101, null}
!847 = metadata !{i32 721153, metadata !101, metadata !"f_inp", metadata !23, i32 33554910, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!848 = metadata !{i32 478, i32 41, metadata !101, null}
!849 = metadata !{i32 721153, metadata !101, metadata !"f_out", metadata !23, i32 50332126, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!850 = metadata !{i32 478, i32 55, metadata !101, null}
!851 = metadata !{i32 721152, metadata !852, metadata !"key", metadata !23, i32 480, metadata !781, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!852 = metadata !{i32 720907, metadata !101, i32 479, i32 1, metadata !23, i32 67} ; [ DW_TAG_lexical_block ]
!853 = metadata !{i32 480, i32 15, metadata !852, null}
!854 = metadata !{i32 721152, metadata !852, metadata !"iv", metadata !23, i32 480, metadata !781, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!855 = metadata !{i32 480, i32 19, metadata !852, null}
!856 = metadata !{i32 721152, metadata !852, metadata !"b1", metadata !23, i32 481, metadata !800, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!857 = metadata !{i32 481, i32 11, metadata !852, null}
!858 = metadata !{i32 721152, metadata !852, metadata !"b2", metadata !23, i32 481, metadata !800, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!859 = metadata !{i32 481, i32 14, metadata !852, null}
!860 = metadata !{i32 721152, metadata !852, metadata !"buf1", metadata !23, i32 482, metadata !745, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!861 = metadata !{i32 482, i32 10, metadata !852, null}
!862 = metadata !{i32 482, i32 46, metadata !852, null}
!863 = metadata !{i32 721152, metadata !852, metadata !"buf2", metadata !23, i32 482, metadata !745, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!864 = metadata !{i32 482, i32 30, metadata !852, null}
!865 = metadata !{i32 721152, metadata !852, metadata !"ctx", metadata !23, i32 483, metadata !787, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!866 = metadata !{i32 483, i32 20, metadata !852, null}
!867 = metadata !{i32 721152, metadata !852, metadata !"err", metadata !23, i32 484, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!868 = metadata !{i32 484, i32 7, metadata !852, null}
!869 = metadata !{i32 484, i32 21, metadata !852, null}
!870 = metadata !{i32 721152, metadata !852, metadata !"n", metadata !23, i32 484, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!871 = metadata !{i32 484, i32 16, metadata !852, null}
!872 = metadata !{i32 486, i32 3, metadata !852, null}
!873 = metadata !{i32 487, i32 3, metadata !852, null}
!874 = metadata !{i32 488, i32 2, metadata !852, null}
!875 = metadata !{i32 490, i32 3, metadata !852, null}
!876 = metadata !{i32 721152, metadata !877, metadata !"i", metadata !23, i32 492, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!877 = metadata !{i32 720907, metadata !852, i32 491, i32 3, metadata !23, i32 68} ; [ DW_TAG_lexical_block ]
!878 = metadata !{i32 492, i32 9, metadata !877, null}
!879 = metadata !{i32 492, i32 14, metadata !877, null}
!880 = metadata !{i32 493, i32 5, metadata !877, null}
!881 = metadata !{i32 494, i32 11, metadata !877, null}
!882 = metadata !{i32 495, i32 5, metadata !877, null}
!883 = metadata !{i32 497, i32 7, metadata !884, null}
!884 = metadata !{i32 720907, metadata !877, i32 496, i32 5, metadata !23, i32 69} ; [ DW_TAG_lexical_block ]
!885 = metadata !{i32 498, i32 7, metadata !886, null}
!886 = metadata !{i32 720907, metadata !884, i32 498, i32 7, metadata !23, i32 70} ; [ DW_TAG_lexical_block ]
!887 = metadata !{i32 500, i32 9, metadata !886, null}
!888 = metadata !{i32 501, i32 9, metadata !886, null}
!889 = metadata !{i32 502, i32 9, metadata !886, null}
!890 = metadata !{i32 505, i32 7, metadata !884, null}
!891 = metadata !{i32 507, i32 9, metadata !892, null}
!892 = metadata !{i32 720907, metadata !884, i32 506, i32 7, metadata !23, i32 71} ; [ DW_TAG_lexical_block ]
!893 = metadata !{i32 508, i32 9, metadata !892, null}
!894 = metadata !{i32 509, i32 9, metadata !892, null}
!895 = metadata !{i32 510, i32 7, metadata !892, null}
!896 = metadata !{i32 511, i32 7, metadata !884, null}
!897 = metadata !{i32 512, i32 7, metadata !884, null}
!898 = metadata !{i32 513, i32 7, metadata !884, null}
!899 = metadata !{i32 515, i32 7, metadata !884, null}
!900 = metadata !{i32 721152, metadata !901, metadata !"pad", metadata !23, i32 517, metadata !550, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!901 = metadata !{i32 720907, metadata !884, i32 516, i32 7, metadata !23, i32 72} ; [ DW_TAG_lexical_block ]
!902 = metadata !{i32 517, i32 14, metadata !901, null}
!903 = metadata !{i32 517, i32 29, metadata !901, null}
!904 = metadata !{i32 519, i32 9, metadata !901, null}
!905 = metadata !{i32 519, i32 54, metadata !901, null}
!906 = metadata !{i32 520, i32 9, metadata !901, null}
!907 = metadata !{i32 721152, metadata !908, metadata !"j", metadata !23, i32 522, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!908 = metadata !{i32 720907, metadata !901, i32 521, i32 9, metadata !23, i32 73} ; [ DW_TAG_lexical_block ]
!909 = metadata !{i32 522, i32 15, metadata !908, null}
!910 = metadata !{i32 523, i32 16, metadata !911, null}
!911 = metadata !{i32 720907, metadata !908, i32 523, i32 11, metadata !23, i32 74} ; [ DW_TAG_lexical_block ]
!912 = metadata !{i32 524, i32 13, metadata !911, null}
!913 = metadata !{i32 524, i32 37, metadata !911, null}
!914 = metadata !{i32 523, i32 26, metadata !911, null}
!915 = metadata !{i32 526, i32 7, metadata !901, null}
!916 = metadata !{i32 721152, metadata !917, metadata !"written", metadata !23, i32 528, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!917 = metadata !{i32 720907, metadata !884, i32 527, i32 7, metadata !23, i32 75} ; [ DW_TAG_lexical_block ]
!918 = metadata !{i32 528, i32 13, metadata !917, null}
!919 = metadata !{i32 528, i32 23, metadata !917, null}
!920 = metadata !{i32 529, i32 9, metadata !917, null}
!921 = metadata !{i32 531, i32 11, metadata !922, null}
!922 = metadata !{i32 720907, metadata !917, i32 530, i32 9, metadata !23, i32 76} ; [ DW_TAG_lexical_block ]
!923 = metadata !{i32 532, i32 11, metadata !922, null}
!924 = metadata !{i32 533, i32 11, metadata !922, null}
!925 = metadata !{i32 536, i32 5, metadata !884, null}
!926 = metadata !{i32 537, i32 5, metadata !877, null}
!927 = metadata !{i32 537, i32 19, metadata !877, null}
!928 = metadata !{i32 721152, metadata !929, metadata !"tmp", metadata !23, i32 538, metadata !745, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!929 = metadata !{i32 720907, metadata !877, i32 538, i32 5, metadata !23, i32 77} ; [ DW_TAG_lexical_block ]
!930 = metadata !{i32 538, i32 14, metadata !929, null}
!931 = metadata !{i32 538, i32 24, metadata !929, null}
!932 = metadata !{i32 538, i32 26, metadata !929, null}
!933 = metadata !{i32 538, i32 37, metadata !929, null}
!934 = metadata !{i32 539, i32 5, metadata !877, null}
!935 = metadata !{i32 540, i32 3, metadata !877, null}
!936 = metadata !{i32 541, i32 3, metadata !852, null}
!937 = metadata !{i32 542, i32 3, metadata !852, null}
!938 = metadata !{i32 543, i32 3, metadata !852, null}
!939 = metadata !{i32 721153, metadata !104, metadata !"f_key", metadata !23, i32 16777762, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!940 = metadata !{i32 546, i32 26, metadata !104, null}
!941 = metadata !{i32 721153, metadata !104, metadata !"f_inp", metadata !23, i32 33554978, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!942 = metadata !{i32 546, i32 40, metadata !104, null}
!943 = metadata !{i32 721153, metadata !104, metadata !"f_out", metadata !23, i32 50332194, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!944 = metadata !{i32 546, i32 54, metadata !104, null}
!945 = metadata !{i32 721153, metadata !104, metadata !"f_sec", metadata !23, i32 67109410, metadata !110, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!946 = metadata !{i32 546, i32 68, metadata !104, null}
!947 = metadata !{i32 721153, metadata !104, metadata !"operation", metadata !23, i32 83886626, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!948 = metadata !{i32 546, i32 79, metadata !104, null}
!949 = metadata !{i32 721152, metadata !950, metadata !"p", metadata !23, i32 548, metadata !413, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!950 = metadata !{i32 720907, metadata !104, i32 547, i32 1, metadata !23, i32 78} ; [ DW_TAG_lexical_block ]
!951 = metadata !{i32 548, i32 8, metadata !950, null}
!952 = metadata !{i32 721152, metadata !950, metadata !"pub", metadata !23, i32 549, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!953 = metadata !{i32 549, i32 11, metadata !950, null}
!954 = metadata !{i32 721152, metadata !950, metadata !"secret", metadata !23, i32 549, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!955 = metadata !{i32 549, i32 15, metadata !950, null}
!956 = metadata !{i32 721152, metadata !950, metadata !"session", metadata !23, i32 549, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!957 = metadata !{i32 549, i32 22, metadata !950, null}
!958 = metadata !{i32 721152, metadata !950, metadata !"mac", metadata !23, i32 549, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!959 = metadata !{i32 549, i32 30, metadata !950, null}
!960 = metadata !{i32 721152, metadata !950, metadata !"msg", metadata !23, i32 549, metadata !620, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!961 = metadata !{i32 549, i32 34, metadata !950, null}
!962 = metadata !{i32 721152, metadata !950, metadata !"sig", metadata !23, i32 550, metadata !963, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!963 = metadata !{i32 720918, null, metadata !"cpPair", metadata !23, i32 9, i64 0, i64 0, i64 0, i32 0, metadata !964} ; [ DW_TAG_typedef ]
!964 = metadata !{i32 720915, null, metadata !"", metadata !965, i32 7, i64 608, i64 16, i32 0, i32 0, null, metadata !966, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!965 = metadata !{i32 720937, metadata !"ec_crypt.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!966 = metadata !{metadata !967, metadata !968}
!967 = metadata !{i32 720909, metadata !964, metadata !"r", metadata !965, i32 8, i64 304, i64 16, i64 0, i32 0, metadata !620} ; [ DW_TAG_member ]
!968 = metadata !{i32 720909, metadata !964, metadata !"s", metadata !965, i32 8, i64 304, i64 16, i64 304, i32 0, metadata !620} ; [ DW_TAG_member ]
!969 = metadata !{i32 550, i32 10, metadata !950, null}
!970 = metadata !{i32 721152, metadata !950, metadata !"err", metadata !23, i32 551, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!971 = metadata !{i32 551, i32 7, metadata !950, null}
!972 = metadata !{i32 551, i32 14, metadata !950, null}
!973 = metadata !{i32 554, i32 3, metadata !950, null}
!974 = metadata !{i32 555, i32 3, metadata !950, null}
!975 = metadata !{i32 557, i32 5, metadata !976, null}
!976 = metadata !{i32 720907, metadata !950, i32 556, i32 3, metadata !23, i32 79} ; [ DW_TAG_lexical_block ]
!977 = metadata !{i32 558, i32 5, metadata !976, null}
!978 = metadata !{i32 560, i32 7, metadata !979, null}
!979 = metadata !{i32 720907, metadata !976, i32 559, i32 5, metadata !23, i32 80} ; [ DW_TAG_lexical_block ]
!980 = metadata !{i32 560, i32 20, metadata !979, null}
!981 = metadata !{i32 561, i32 7, metadata !979, null}
!982 = metadata !{i32 562, i32 5, metadata !979, null}
!983 = metadata !{i32 563, i32 3, metadata !976, null}
!984 = metadata !{i32 566, i32 5, metadata !985, null}
!985 = metadata !{i32 720907, metadata !950, i32 565, i32 3, metadata !23, i32 81} ; [ DW_TAG_lexical_block ]
!986 = metadata !{i32 567, i32 5, metadata !985, null}
!987 = metadata !{i32 568, i32 5, metadata !985, null}
!988 = metadata !{i32 569, i32 7, metadata !985, null}
!989 = metadata !{i32 571, i32 7, metadata !985, null}
!990 = metadata !{i32 574, i32 3, metadata !950, null}
!991 = metadata !{i32 576, i32 5, metadata !992, null}
!992 = metadata !{i32 720907, metadata !950, i32 575, i32 3, metadata !23, i32 82} ; [ DW_TAG_lexical_block ]
!993 = metadata !{i32 577, i32 5, metadata !992, null}
!994 = metadata !{i32 578, i32 3, metadata !992, null}
!995 = metadata !{i32 579, i32 3, metadata !950, null}
!996 = metadata !{i32 581, i32 5, metadata !997, null}
!997 = metadata !{i32 720907, metadata !950, i32 580, i32 3, metadata !23, i32 83} ; [ DW_TAG_lexical_block ]
!998 = metadata !{i32 582, i32 5, metadata !997, null}
!999 = metadata !{i32 583, i32 3, metadata !997, null}
!1000 = metadata !{i32 584, i32 3, metadata !950, null}
!1001 = metadata !{i32 586, i32 10, metadata !1002, null}
!1002 = metadata !{i32 720907, metadata !950, i32 585, i32 3, metadata !23, i32 84} ; [ DW_TAG_lexical_block ]
!1003 = metadata !{i32 587, i32 7, metadata !1002, null}
!1004 = metadata !{i32 588, i32 5, metadata !1002, null}
!1005 = metadata !{i32 589, i32 3, metadata !1002, null}
!1006 = metadata !{i32 594, i32 3, metadata !950, null}
!1007 = metadata !{i32 596, i32 5, metadata !1008, null}
!1008 = metadata !{i32 720907, metadata !950, i32 595, i32 3, metadata !23, i32 85} ; [ DW_TAG_lexical_block ]
!1009 = metadata !{i32 596, i32 25, metadata !1008, null}
!1010 = metadata !{i32 597, i32 11, metadata !1008, null}
!1011 = metadata !{i32 598, i32 5, metadata !1008, null}
!1012 = metadata !{i32 600, i32 11, metadata !1013, null}
!1013 = metadata !{i32 720907, metadata !1008, i32 599, i32 5, metadata !23, i32 86} ; [ DW_TAG_lexical_block ]
!1014 = metadata !{i32 600, i32 31, metadata !1013, null}
!1015 = metadata !{i32 601, i32 7, metadata !1013, null}
!1016 = metadata !{i32 602, i32 5, metadata !1013, null}
!1017 = metadata !{i32 603, i32 3, metadata !1008, null}
!1018 = metadata !{i32 604, i32 8, metadata !950, null}
!1019 = metadata !{i32 606, i32 5, metadata !1020, null}
!1020 = metadata !{i32 720907, metadata !950, i32 605, i32 3, metadata !23, i32 87} ; [ DW_TAG_lexical_block ]
!1021 = metadata !{i32 606, i32 28, metadata !1020, null}
!1022 = metadata !{i32 606, i32 45, metadata !1020, null}
!1023 = metadata !{i32 607, i32 11, metadata !1020, null}
!1024 = metadata !{i32 608, i32 3, metadata !1020, null}
!1025 = metadata !{i32 611, i32 5, metadata !1026, null}
!1026 = metadata !{i32 720907, metadata !950, i32 610, i32 3, metadata !23, i32 88} ; [ DW_TAG_lexical_block ]
!1027 = metadata !{i32 612, i32 5, metadata !1026, null}
!1028 = metadata !{i32 614, i32 7, metadata !1029, null}
!1029 = metadata !{i32 720907, metadata !1026, i32 613, i32 5, metadata !23, i32 89} ; [ DW_TAG_lexical_block ]
!1030 = metadata !{i32 615, i32 7, metadata !1029, null}
!1031 = metadata !{i32 616, i32 7, metadata !1029, null}
!1032 = metadata !{i32 617, i32 5, metadata !1029, null}
!1033 = metadata !{i32 618, i32 10, metadata !1026, null}
!1034 = metadata !{i32 620, i32 7, metadata !1035, null}
!1035 = metadata !{i32 720907, metadata !1026, i32 619, i32 5, metadata !23, i32 90} ; [ DW_TAG_lexical_block ]
!1036 = metadata !{i32 620, i32 27, metadata !1035, null}
!1037 = metadata !{i32 621, i32 7, metadata !1035, null}
!1038 = metadata !{i32 622, i32 7, metadata !1035, null}
!1039 = metadata !{i32 623, i32 7, metadata !1035, null}
!1040 = metadata !{i32 624, i32 7, metadata !1035, null}
!1041 = metadata !{i32 625, i32 13, metadata !1035, null}
!1042 = metadata !{i32 626, i32 7, metadata !1035, null}
!1043 = metadata !{i32 628, i32 13, metadata !1044, null}
!1044 = metadata !{i32 720907, metadata !1035, i32 627, i32 7, metadata !23, i32 91} ; [ DW_TAG_lexical_block ]
!1045 = metadata !{i32 628, i32 33, metadata !1044, null}
!1046 = metadata !{i32 629, i32 9, metadata !1044, null}
!1047 = metadata !{i32 630, i32 7, metadata !1044, null}
!1048 = metadata !{i32 631, i32 5, metadata !1035, null}
!1049 = metadata !{i32 632, i32 10, metadata !1026, null}
!1050 = metadata !{i32 634, i32 7, metadata !1051, null}
!1051 = metadata !{i32 720907, metadata !1026, i32 633, i32 5, metadata !23, i32 92} ; [ DW_TAG_lexical_block ]
!1052 = metadata !{i32 634, i32 30, metadata !1051, null}
!1053 = metadata !{i32 634, i32 47, metadata !1051, null}
!1054 = metadata !{i32 635, i32 7, metadata !1051, null}
!1055 = metadata !{i32 636, i32 7, metadata !1051, null}
!1056 = metadata !{i32 637, i32 13, metadata !1051, null}
!1057 = metadata !{i32 638, i32 5, metadata !1051, null}
!1058 = metadata !{i32 639, i32 10, metadata !1026, null}
!1059 = metadata !{i32 641, i32 7, metadata !1060, null}
!1060 = metadata !{i32 720907, metadata !1026, i32 640, i32 5, metadata !23, i32 93} ; [ DW_TAG_lexical_block ]
!1061 = metadata !{i32 643, i32 9, metadata !1062, null}
!1062 = metadata !{i32 720907, metadata !1060, i32 642, i32 7, metadata !23, i32 94} ; [ DW_TAG_lexical_block ]
!1063 = metadata !{i32 644, i32 9, metadata !1062, null}
!1064 = metadata !{i32 645, i32 7, metadata !1062, null}
!1065 = metadata !{i32 646, i32 7, metadata !1060, null}
!1066 = metadata !{i32 647, i32 7, metadata !1060, null}
!1067 = metadata !{i32 647, i32 28, metadata !1060, null}
!1068 = metadata !{i32 648, i32 12, metadata !1060, null}
!1069 = metadata !{i32 649, i32 7, metadata !1060, null}
!1070 = metadata !{i32 650, i32 7, metadata !1060, null}
!1071 = metadata !{i32 650, i32 28, metadata !1060, null}
!1072 = metadata !{i32 651, i32 5, metadata !1060, null}
!1073 = metadata !{i32 654, i32 5, metadata !1074, null}
!1074 = metadata !{i32 720907, metadata !1026, i32 653, i32 3, metadata !23, i32 95} ; [ DW_TAG_lexical_block ]
!1075 = metadata !{i32 656, i32 9, metadata !1076, null}
!1076 = metadata !{i32 720907, metadata !1074, i32 655, i32 7, metadata !23, i32 96} ; [ DW_TAG_lexical_block ]
!1077 = metadata !{i32 657, i32 9, metadata !1076, null}
!1078 = metadata !{i32 658, i32 4, metadata !1076, null}
!1079 = metadata !{i32 661, i32 9, metadata !1080, null}
!1080 = metadata !{i32 720907, metadata !1074, i32 660, i32 7, metadata !23, i32 97} ; [ DW_TAG_lexical_block ]
!1081 = metadata !{i32 662, i32 9, metadata !1080, null}
!1082 = metadata !{i32 664, i32 14, metadata !1074, null}
!1083 = metadata !{i32 665, i32 7, metadata !1074, null}
!1084 = metadata !{i32 666, i32 6, metadata !1074, null}
!1085 = metadata !{i32 668, i32 5, metadata !1026, null}
!1086 = metadata !{i32 670, i32 3, metadata !950, null}
!1087 = metadata !{i32 672, i32 3, metadata !950, null}
!1088 = metadata !{i32 673, i32 3, metadata !950, null}
!1089 = metadata !{i32 674, i32 3, metadata !950, null}
!1090 = metadata !{i32 675, i32 3, metadata !950, null}
!1091 = metadata !{i32 676, i32 1, metadata !950, null}
!1092 = metadata !{i32 721153, metadata !107, metadata !"s", metadata !23, i32 16777894, metadata !117, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!1093 = metadata !{i32 678, i32 24, metadata !107, null}
!1094 = metadata !{i32 721153, metadata !107, metadata !"mode", metadata !23, i32 33555110, metadata !117, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!1095 = metadata !{i32 678, i32 34, metadata !107, null}
!1096 = metadata !{i32 721152, metadata !1097, metadata !"result", metadata !23, i32 680, metadata !110, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1097 = metadata !{i32 720907, metadata !107, i32 679, i32 1, metadata !23, i32 98} ; [ DW_TAG_lexical_block ]
!1098 = metadata !{i32 680, i32 10, metadata !1097, null}
!1099 = metadata !{i32 680, i32 19, metadata !1097, null}
!1100 = metadata !{i32 681, i32 3, metadata !1097, null}
!1101 = metadata !{i32 683, i32 5, metadata !1102, null}
!1102 = metadata !{i32 720907, metadata !1097, i32 682, i32 3, metadata !23, i32 99} ; [ DW_TAG_lexical_block ]
!1103 = metadata !{i32 684, i32 5, metadata !1102, null}
!1104 = metadata !{i32 685, i32 3, metadata !1102, null}
!1105 = metadata !{i32 686, i32 3, metadata !1097, null}
!1106 = metadata !{i32 721152, metadata !1107, metadata !"x", metadata !23, i32 693, metadata !1108, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1107 = metadata !{i32 720907, metadata !171, i32 690, i32 1, metadata !23, i32 100} ; [ DW_TAG_lexical_block ]
!1108 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 160000, i64 8, i32 0, i32 0, metadata !267, metadata !1109, i32 0, i32 0} ; [ DW_TAG_array_type ]
!1109 = metadata !{metadata !1110}
!1110 = metadata !{i32 720929, i64 0, i64 19999}  ; [ DW_TAG_subrange_type ]
!1111 = metadata !{i32 693, i32 17, metadata !1107, null}
!1112 = metadata !{i32 694, i32 3, metadata !1107, null}
!1113 = metadata !{i32 695, i32 1, metadata !1107, null}
!1114 = metadata !{i32 721153, metadata !174, metadata !"argc", metadata !23, i32 16777921, metadata !31, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!1115 = metadata !{i32 705, i32 14, metadata !174, null}
!1116 = metadata !{i32 721153, metadata !174, metadata !"argv", metadata !23, i32 33555137, metadata !1117, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!1117 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !117} ; [ DW_TAG_pointer_type ]
!1118 = metadata !{i32 705, i32 27, metadata !174, null}
!1119 = metadata !{i32 721152, metadata !1120, metadata !"err", metadata !23, i32 708, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1120 = metadata !{i32 720907, metadata !174, i32 706, i32 1, metadata !23, i32 101} ; [ DW_TAG_lexical_block ]
!1121 = metadata !{i32 708, i32 7, metadata !1120, null}
!1122 = metadata !{i32 721152, metadata !1120, metadata !"operation", metadata !23, i32 708, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1123 = metadata !{i32 708, i32 12, metadata !1120, null}
!1124 = metadata !{i32 721152, metadata !1120, metadata !"filter", metadata !23, i32 708, metadata !31, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1125 = metadata !{i32 708, i32 23, metadata !1120, null}
!1126 = metadata !{i32 708, i32 31, metadata !1120, null}
!1127 = metadata !{i32 721152, metadata !1120, metadata !"expect", metadata !23, i32 709, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1128 = metadata !{i32 709, i32 12, metadata !1120, null}
!1129 = metadata !{i32 721152, metadata !1120, metadata !"arg_ix", metadata !23, i32 709, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1130 = metadata !{i32 709, i32 20, metadata !1120, null}
!1131 = metadata !{i32 721152, metadata !1120, metadata !"f_key", metadata !23, i32 710, metadata !110, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1132 = metadata !{i32 710, i32 10, metadata !1120, null}
!1133 = metadata !{i32 721152, metadata !1120, metadata !"f_inp", metadata !23, i32 710, metadata !110, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1134 = metadata !{i32 710, i32 19, metadata !1120, null}
!1135 = metadata !{i32 721152, metadata !1120, metadata !"f_out", metadata !23, i32 710, metadata !110, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1136 = metadata !{i32 710, i32 28, metadata !1120, null}
!1137 = metadata !{i32 721152, metadata !1120, metadata !"f_sec", metadata !23, i32 710, metadata !110, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1138 = metadata !{i32 710, i32 36, metadata !1120, null}
!1139 = metadata !{i32 721152, metadata !1120, metadata !"openForRead", metadata !23, i32 711, metadata !1140, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1140 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 24, i64 8, i32 0, i32 0, metadata !118, metadata !1141, i32 0, i32 0} ; [ DW_TAG_array_type ]
!1141 = metadata !{metadata !1142}
!1142 = metadata !{i32 720929, i64 0, i64 2}      ; [ DW_TAG_subrange_type ]
!1143 = metadata !{i32 711, i32 8, metadata !1120, null}
!1144 = metadata !{i32 711, i32 30, metadata !1120, null}
!1145 = metadata !{i32 721152, metadata !1120, metadata !"openForWrite", metadata !23, i32 712, metadata !1140, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1146 = metadata !{i32 712, i32 8, metadata !1120, null}
!1147 = metadata !{i32 712, i32 31, metadata !1120, null}
!1148 = metadata !{i32 721152, metadata !1120, metadata !"openKey", metadata !23, i32 713, metadata !1140, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1149 = metadata !{i32 713, i32 8, metadata !1120, null}
!1150 = metadata !{i32 713, i32 25, metadata !1120, null}
!1151 = metadata !{i32 730, i32 3, metadata !1120, null}
!1152 = metadata !{i32 731, i32 3, metadata !1153, null}
!1153 = metadata !{i32 720907, metadata !1120, i32 731, i32 3, metadata !23, i32 102} ; [ DW_TAG_lexical_block ]
!1154 = metadata !{i32 733, i32 5, metadata !1153, null}
!1155 = metadata !{i32 733, i32 16, metadata !1153, null}
!1156 = metadata !{i32 734, i32 5, metadata !1153, null}
!1157 = metadata !{i32 737, i32 5, metadata !1153, null}
!1158 = metadata !{i32 739, i32 3, metadata !1120, null}
!1159 = metadata !{i32 741, i32 3, metadata !1120, null}
!1160 = metadata !{i32 743, i32 7, metadata !1161, null}
!1161 = metadata !{i32 720907, metadata !1120, i32 742, i32 3, metadata !23, i32 103} ; [ DW_TAG_lexical_block ]
!1162 = metadata !{i32 744, i32 7, metadata !1161, null}
!1163 = metadata !{i32 745, i32 7, metadata !1161, null}
!1164 = metadata !{i32 746, i32 7, metadata !1165, null}
!1165 = metadata !{i32 720907, metadata !1161, i32 746, i32 7, metadata !23, i32 104} ; [ DW_TAG_lexical_block ]
!1166 = metadata !{i32 748, i32 10, metadata !1165, null}
!1167 = metadata !{i32 749, i32 10, metadata !1165, null}
!1168 = metadata !{i32 751, i32 7, metadata !1161, null}
!1169 = metadata !{i32 751, i32 28, metadata !1161, null}
!1170 = metadata !{i32 752, i32 3, metadata !1161, null}
!1171 = metadata !{i32 753, i32 8, metadata !1120, null}
!1172 = metadata !{i32 753, i32 30, metadata !1120, null}
!1173 = metadata !{i32 756, i32 3, metadata !1120, null}
!1174 = metadata !{i32 758, i32 3, metadata !1120, null}
!1175 = metadata !{i32 760, i32 5, metadata !1176, null}
!1176 = metadata !{i32 720907, metadata !1120, i32 759, i32 3, metadata !23, i32 105} ; [ DW_TAG_lexical_block ]
!1177 = metadata !{i32 760, i32 29, metadata !1176, null}
!1178 = metadata !{i32 761, i32 10, metadata !1176, null}
!1179 = metadata !{i32 761, i32 54, metadata !1176, null}
!1180 = metadata !{i32 762, i32 10, metadata !1176, null}
!1181 = metadata !{i32 763, i32 44, metadata !1176, null}
!1182 = metadata !{i32 764, i32 10, metadata !1176, null}
!1183 = metadata !{i32 764, i32 34, metadata !1176, null}
!1184 = metadata !{i32 765, i32 3, metadata !1176, null}
!1185 = metadata !{i32 768, i32 5, metadata !1186, null}
!1186 = metadata !{i32 720907, metadata !1120, i32 767, i32 3, metadata !23, i32 106} ; [ DW_TAG_lexical_block ]
!1187 = metadata !{i32 769, i32 46, metadata !1186, null}
!1188 = metadata !{i32 770, i32 9, metadata !1186, null}
!1189 = metadata !{i32 770, i32 31, metadata !1186, null}
!1190 = metadata !{i32 773, i32 3, metadata !1120, null}
!1191 = metadata !{i32 773, i32 25, metadata !1120, null}
!1192 = metadata !{i32 775, i32 3, metadata !1120, null}
!1193 = metadata !{i32 777, i32 3, metadata !1120, null}
!1194 = metadata !{i32 778, i32 3, metadata !1120, null}
!1195 = metadata !{i32 721152, metadata !1196, metadata !"i", metadata !23, i32 780, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1196 = metadata !{i32 720907, metadata !1120, i32 779, i32 3, metadata !23, i32 107} ; [ DW_TAG_lexical_block ]
!1197 = metadata !{i32 780, i32 14, metadata !1196, null}
!1198 = metadata !{i32 721152, metadata !1196, metadata !"isPub", metadata !23, i32 780, metadata !163, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!1199 = metadata !{i32 780, i32 17, metadata !1196, null}
!1200 = metadata !{i32 780, i32 26, metadata !1196, null}
!1201 = metadata !{i32 782, i32 5, metadata !1196, null}
!1202 = metadata !{i32 782, i32 46, metadata !1196, null}
!1203 = metadata !{i32 784, i32 13, metadata !1196, null}
!1204 = metadata !{i32 786, i32 5, metadata !1196, null}
!1205 = metadata !{i32 786, i32 17, metadata !1196, null}
!1206 = metadata !{i32 787, i32 5, metadata !1196, null}
!1207 = metadata !{i32 787, i32 56, metadata !1196, null}
!1208 = metadata !{i32 789, i32 10, metadata !1209, null}
!1209 = metadata !{i32 720907, metadata !1196, i32 789, i32 5, metadata !23, i32 108} ; [ DW_TAG_lexical_block ]
!1210 = metadata !{i32 791, i32 12, metadata !1211, null}
!1211 = metadata !{i32 720907, metadata !1209, i32 790, i32 5, metadata !23, i32 109} ; [ DW_TAG_lexical_block ]
!1212 = metadata !{i32 793, i32 9, metadata !1213, null}
!1213 = metadata !{i32 720907, metadata !1211, i32 792, i32 7, metadata !23, i32 110} ; [ DW_TAG_lexical_block ]
!1214 = metadata !{i32 794, i32 9, metadata !1213, null}
!1215 = metadata !{i32 795, i32 9, metadata !1213, null}
!1216 = metadata !{i32 796, i32 9, metadata !1213, null}
!1217 = metadata !{i32 798, i32 5, metadata !1211, null}
!1218 = metadata !{i32 789, i32 39, metadata !1209, null}
!1219 = metadata !{i32 799, i32 3, metadata !1196, null}
!1220 = metadata !{i32 801, i32 3, metadata !1120, null}
!1221 = metadata !{i32 802, i32 3, metadata !1120, null}
!1222 = metadata !{i32 804, i32 3, metadata !1120, null}
!1223 = metadata !{i32 806, i32 5, metadata !1224, null}
!1224 = metadata !{i32 720907, metadata !1120, i32 805, i32 3, metadata !23, i32 111} ; [ DW_TAG_lexical_block ]
!1225 = metadata !{i32 807, i32 7, metadata !1224, null}
!1226 = metadata !{i32 809, i32 5, metadata !1224, null}
!1227 = metadata !{i32 810, i32 5, metadata !1224, null}
!1228 = metadata !{i32 810, i32 46, metadata !1224, null}
!1229 = metadata !{i32 811, i32 5, metadata !1224, null}
!1230 = metadata !{i32 813, i32 15, metadata !1231, null}
!1231 = metadata !{i32 720907, metadata !1224, i32 812, i32 5, metadata !23, i32 112} ; [ DW_TAG_lexical_block ]
!1232 = metadata !{i32 814, i32 7, metadata !1231, null}
!1233 = metadata !{i32 814, i32 19, metadata !1231, null}
!1234 = metadata !{i32 815, i32 5, metadata !1231, null}
!1235 = metadata !{i32 816, i32 5, metadata !1224, null}
!1236 = metadata !{i32 818, i32 15, metadata !1237, null}
!1237 = metadata !{i32 720907, metadata !1224, i32 817, i32 5, metadata !23, i32 113} ; [ DW_TAG_lexical_block ]
!1238 = metadata !{i32 819, i32 7, metadata !1237, null}
!1239 = metadata !{i32 819, i32 19, metadata !1237, null}
!1240 = metadata !{i32 820, i32 5, metadata !1237, null}
!1241 = metadata !{i32 821, i32 3, metadata !1224, null}
!1242 = metadata !{i32 824, i32 7, metadata !1243, null}
!1243 = metadata !{i32 720907, metadata !1120, i32 823, i32 3, metadata !23, i32 114} ; [ DW_TAG_lexical_block ]
!1244 = metadata !{i32 825, i32 7, metadata !1243, null}
!1245 = metadata !{i32 827, i32 17, metadata !1246, null}
!1246 = metadata !{i32 720907, metadata !1243, i32 826, i32 7, metadata !23, i32 115} ; [ DW_TAG_lexical_block ]
!1247 = metadata !{i32 828, i32 9, metadata !1246, null}
!1248 = metadata !{i32 828, i32 21, metadata !1246, null}
!1249 = metadata !{i32 829, i32 7, metadata !1246, null}
!1250 = metadata !{i32 832, i32 9, metadata !1120, null}
!1251 = metadata !{i32 834, i32 3, metadata !1120, null}
!1252 = metadata !{i32 835, i32 3, metadata !1120, null}
!1253 = metadata !{i32 838, i32 3, metadata !1120, null}
!1254 = metadata !{i32 839, i32 1, metadata !1120, null}
