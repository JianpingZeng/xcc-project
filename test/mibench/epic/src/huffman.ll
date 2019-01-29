; ModuleID = 'huffman.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct.node = type { i32, %struct.node*, %struct.node*, %struct.node*, i16, i8* }
%struct.code_node = type { %struct.code_node*, %struct.code_node*, i16 }
%struct._IO_FILE.4 = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker.5*, %struct._IO_FILE.4*, i32, i32, i32, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i32, i32, [40 x i8] }
%struct._IO_marker.5 = type { %struct._IO_marker.5*, %struct._IO_FILE.4*, i32 }

@temp_byte = external global i8
@.str = private unnamed_addr constant [29 x i8] c"Error reading Huffman Tree!\0A\00", align 1
@.str1 = private unnamed_addr constant [40 x i8] c"Error in histogram.  Unable to encode.\0A\00", align 1
@.str2 = private unnamed_addr constant [20 x i8] c"Symbol Histogram: \0A\00", align 1
@.str3 = private unnamed_addr constant [11 x i8] c"%04x: %d  \00", align 1
@.str4 = private unnamed_addr constant [2 x i8] c"\0A\00", align 1

define arm_aapcscc i32 @huffman_encode(i16* %symbol_stream, i32 %symbol_stream_length, i8** %packed_huffman_tree, i32* %huffman_tree_size, i8* %encoded_stream) nounwind uwtable {
entry:
  %symbol_stream.addr = alloca i16*, align 4
  %symbol_stream_length.addr = alloca i32, align 4
  %packed_huffman_tree.addr = alloca i8**, align 4
  %huffman_tree_size.addr = alloca i32*, align 4
  %encoded_stream.addr = alloca i8*, align 4
  %histogram = alloca [65536 x %struct.node*], align 4
  %huffman_tree = alloca %struct.node*, align 4
  %encoded_stream_length = alloca i32, align 4
  %i = alloca i32, align 4
  store i16* %symbol_stream, i16** %symbol_stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %symbol_stream.addr}, metadata !59), !dbg !61
  store i32 %symbol_stream_length, i32* %symbol_stream_length.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %symbol_stream_length.addr}, metadata !62), !dbg !63
  store i8** %packed_huffman_tree, i8*** %packed_huffman_tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8*** %packed_huffman_tree.addr}, metadata !64), !dbg !69
  store i32* %huffman_tree_size, i32** %huffman_tree_size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32** %huffman_tree_size.addr}, metadata !70), !dbg !72
  store i8* %encoded_stream, i8** %encoded_stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %encoded_stream.addr}, metadata !73), !dbg !76
  call void @llvm.dbg.declare(metadata !{[65536 x %struct.node*]* %histogram}, metadata !77), !dbg !94
  call void @llvm.dbg.declare(metadata !{%struct.node** %huffman_tree}, metadata !95), !dbg !96
  call void @llvm.dbg.declare(metadata !{i32* %encoded_stream_length}, metadata !97), !dbg !98
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !99), !dbg !100
  store i32 0, i32* %i, align 4, !dbg !101
  br label %for.cond, !dbg !101

for.cond:                                         ; preds = %for.inc, %entry
  %0 = load i32* %i, align 4, !dbg !101
  %cmp = icmp slt i32 %0, 65536, !dbg !101
  br i1 %cmp, label %for.body, label %for.end, !dbg !101

for.body:                                         ; preds = %for.cond
  %1 = load i32* %i, align 4, !dbg !103
  %arrayidx = getelementptr inbounds [65536 x %struct.node*]* %histogram, i32 0, i32 %1, !dbg !103
  store %struct.node* null, %struct.node** %arrayidx, align 4, !dbg !103
  br label %for.inc, !dbg !103

for.inc:                                          ; preds = %for.body
  %2 = load i32* %i, align 4, !dbg !104
  %inc = add nsw i32 %2, 1, !dbg !104
  store i32 %inc, i32* %i, align 4, !dbg !104
  br label %for.cond, !dbg !104

for.end:                                          ; preds = %for.cond
  %3 = load i16** %symbol_stream.addr, align 4, !dbg !105
  %4 = load i32* %symbol_stream_length.addr, align 4, !dbg !105
  %arraydecay = getelementptr inbounds [65536 x %struct.node*]* %histogram, i32 0, i32 0, !dbg !105
  %call = call arm_aapcscc  i32 @compute_histogram(i16* %3, i32 %4, %struct.node** %arraydecay), !dbg !105
  %arraydecay1 = getelementptr inbounds [65536 x %struct.node*]* %histogram, i32 0, i32 0, !dbg !106
  %call2 = call arm_aapcscc  i32 @build_huffman_tree(%struct.node** %arraydecay1, %struct.node** %huffman_tree), !dbg !106
  %5 = load i16** %symbol_stream.addr, align 4, !dbg !107
  %6 = load i32* %symbol_stream_length.addr, align 4, !dbg !107
  %arraydecay3 = getelementptr inbounds [65536 x %struct.node*]* %histogram, i32 0, i32 0, !dbg !107
  %7 = load i8** %encoded_stream.addr, align 4, !dbg !107
  %call4 = call arm_aapcscc  i32 @encode_stream(i16* %5, i32 %6, %struct.node** %arraydecay3, i8* %7), !dbg !107
  store i32 %call4, i32* %encoded_stream_length, align 4, !dbg !107
  %8 = load %struct.node** %huffman_tree, align 4, !dbg !108
  %9 = load i8*** %packed_huffman_tree.addr, align 4, !dbg !108
  %call5 = call arm_aapcscc  i32 @pack_huffman_tree(%struct.node* %8, i8** %9), !dbg !108
  %10 = load i32** %huffman_tree_size.addr, align 4, !dbg !108
  store i32 %call5, i32* %10, !dbg !108
  %11 = load %struct.node** %huffman_tree, align 4, !dbg !109
  %call6 = call arm_aapcscc  i32 @free_tree_nodes(%struct.node* %11), !dbg !109
  %12 = load i32* %encoded_stream_length, align 4, !dbg !110
  ret i32 %12, !dbg !110
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

define arm_aapcscc i32 @compute_histogram(i16* %symbol_stream, i32 %symbol_stream_length, %struct.node** %histogram) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %symbol_stream.addr = alloca i16*, align 4
  %symbol_stream_length.addr = alloca i32, align 4
  %histogram.addr = alloca %struct.node**, align 4
  %i = alloca i32, align 4
  %the_leaf = alloca %struct.node*, align 4
  store i16* %symbol_stream, i16** %symbol_stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %symbol_stream.addr}, metadata !111), !dbg !112
  store i32 %symbol_stream_length, i32* %symbol_stream_length.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %symbol_stream_length.addr}, metadata !113), !dbg !114
  store %struct.node** %histogram, %struct.node*** %histogram.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node*** %histogram.addr}, metadata !115), !dbg !117
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !118), !dbg !120
  call void @llvm.dbg.declare(metadata !{%struct.node** %the_leaf}, metadata !121), !dbg !122
  store i32 0, i32* %i, align 4, !dbg !123
  br label %for.cond, !dbg !123

for.cond:                                         ; preds = %for.inc, %entry
  %0 = load i32* %i, align 4, !dbg !123
  %1 = load i32* %symbol_stream_length.addr, align 4, !dbg !123
  %cmp = icmp slt i32 %0, %1, !dbg !123
  br i1 %cmp, label %for.body, label %for.end, !dbg !123

for.body:                                         ; preds = %for.cond
  %2 = load i16** %symbol_stream.addr, align 4, !dbg !125
  %3 = load i16* %2, !dbg !125
  %idxprom = zext i16 %3 to i32, !dbg !125
  %4 = load %struct.node*** %histogram.addr, align 4, !dbg !125
  %arrayidx = getelementptr inbounds %struct.node** %4, i32 %idxprom, !dbg !125
  %5 = load %struct.node** %arrayidx, !dbg !125
  store %struct.node* %5, %struct.node** %the_leaf, align 4, !dbg !125
  %6 = load %struct.node** %the_leaf, align 4, !dbg !127
  %cmp1 = icmp eq %struct.node* %6, null, !dbg !127
  br i1 %cmp1, label %if.then, label %if.else, !dbg !127

if.then:                                          ; preds = %for.body
  %call = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 24), !dbg !128
  %7 = bitcast i8* %call to %struct.node*, !dbg !128
  %8 = load i16** %symbol_stream.addr, align 4, !dbg !128
  %9 = load i16* %8, !dbg !128
  %idxprom2 = zext i16 %9 to i32, !dbg !128
  %10 = load %struct.node*** %histogram.addr, align 4, !dbg !128
  %arrayidx3 = getelementptr inbounds %struct.node** %10, i32 %idxprom2, !dbg !128
  store %struct.node* %7, %struct.node** %arrayidx3, !dbg !128
  store %struct.node* %7, %struct.node** %the_leaf, align 4, !dbg !128
  %11 = load i16** %symbol_stream.addr, align 4, !dbg !130
  %12 = load i16* %11, !dbg !130
  %13 = load %struct.node** %the_leaf, align 4, !dbg !130
  %symbol = getelementptr inbounds %struct.node* %13, i32 0, i32 4, !dbg !130
  store i16 %12, i16* %symbol, align 2, !dbg !130
  %14 = load %struct.node** %the_leaf, align 4, !dbg !131
  %frequency = getelementptr inbounds %struct.node* %14, i32 0, i32 0, !dbg !131
  store i32 1, i32* %frequency, align 4, !dbg !131
  %15 = load %struct.node** %the_leaf, align 4, !dbg !132
  %one_child = getelementptr inbounds %struct.node* %15, i32 0, i32 3, !dbg !132
  store %struct.node* null, %struct.node** %one_child, align 4, !dbg !132
  %16 = load %struct.node** %the_leaf, align 4, !dbg !132
  %zero_child = getelementptr inbounds %struct.node* %16, i32 0, i32 2, !dbg !132
  store %struct.node* null, %struct.node** %zero_child, align 4, !dbg !132
  br label %if.end, !dbg !133

if.else:                                          ; preds = %for.body
  %17 = load %struct.node** %the_leaf, align 4, !dbg !134
  %frequency4 = getelementptr inbounds %struct.node* %17, i32 0, i32 0, !dbg !134
  %18 = load i32* %frequency4, align 4, !dbg !134
  %inc = add i32 %18, 1, !dbg !134
  store i32 %inc, i32* %frequency4, align 4, !dbg !134
  br label %if.end

if.end:                                           ; preds = %if.else, %if.then
  br label %for.inc, !dbg !135

for.inc:                                          ; preds = %if.end
  %19 = load i32* %i, align 4, !dbg !136
  %inc5 = add nsw i32 %19, 1, !dbg !136
  store i32 %inc5, i32* %i, align 4, !dbg !136
  %20 = load i16** %symbol_stream.addr, align 4, !dbg !136
  %incdec.ptr = getelementptr inbounds i16* %20, i32 1, !dbg !136
  store i16* %incdec.ptr, i16** %symbol_stream.addr, align 4, !dbg !136
  br label %for.cond, !dbg !136

for.end:                                          ; preds = %for.cond
  %21 = load i32* %retval, !dbg !137
  ret i32 %21, !dbg !137
}

declare arm_aapcscc i8* @check_malloc(...)

define arm_aapcscc i32 @build_huffman_tree(%struct.node** %histogram, %struct.node** %tree_ptr) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %histogram.addr = alloca %struct.node**, align 4
  %tree_ptr.addr = alloca %struct.node**, align 4
  %tree = alloca %struct.node*, align 4
  %new_node = alloca %struct.node*, align 4
  %temp_code = alloca [65536 x i8], align 1
  %i = alloca i32, align 4
  store %struct.node** %histogram, %struct.node*** %histogram.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node*** %histogram.addr}, metadata !138), !dbg !139
  store %struct.node** %tree_ptr, %struct.node*** %tree_ptr.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node*** %tree_ptr.addr}, metadata !140), !dbg !141
  call void @llvm.dbg.declare(metadata !{%struct.node** %tree}, metadata !142), !dbg !144
  store %struct.node* null, %struct.node** %tree, align 4, !dbg !145
  call void @llvm.dbg.declare(metadata !{%struct.node** %new_node}, metadata !146), !dbg !147
  call void @llvm.dbg.declare(metadata !{[65536 x i8]* %temp_code}, metadata !148), !dbg !150
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !151), !dbg !152
  store i32 0, i32* %i, align 4, !dbg !153
  br label %for.cond, !dbg !153

for.cond:                                         ; preds = %for.inc, %entry
  %0 = load i32* %i, align 4, !dbg !153
  %cmp = icmp slt i32 %0, 65536, !dbg !153
  br i1 %cmp, label %for.body, label %for.end, !dbg !153

for.body:                                         ; preds = %for.cond
  %1 = load i32* %i, align 4, !dbg !155
  %2 = load %struct.node*** %histogram.addr, align 4, !dbg !155
  %arrayidx = getelementptr inbounds %struct.node** %2, i32 %1, !dbg !155
  %3 = load %struct.node** %arrayidx, !dbg !155
  %cmp1 = icmp ne %struct.node* %3, null, !dbg !155
  br i1 %cmp1, label %if.then, label %if.end, !dbg !155

if.then:                                          ; preds = %for.body
  %4 = load i32* %i, align 4, !dbg !156
  %5 = load %struct.node*** %histogram.addr, align 4, !dbg !156
  %arrayidx2 = getelementptr inbounds %struct.node** %5, i32 %4, !dbg !156
  %6 = load %struct.node** %arrayidx2, !dbg !156
  %call = call arm_aapcscc  i32 @insert_in_ordered_list(%struct.node* %6, %struct.node** %tree), !dbg !156
  br label %if.end, !dbg !156

if.end:                                           ; preds = %if.then, %for.body
  br label %for.inc, !dbg !156

for.inc:                                          ; preds = %if.end
  %7 = load i32* %i, align 4, !dbg !157
  %inc = add nsw i32 %7, 1, !dbg !157
  store i32 %inc, i32* %i, align 4, !dbg !157
  br label %for.cond, !dbg !157

for.end:                                          ; preds = %for.cond
  br label %while.cond, !dbg !158

while.cond:                                       ; preds = %while.body, %for.end
  %8 = load %struct.node** %tree, align 4, !dbg !158
  %next = getelementptr inbounds %struct.node* %8, i32 0, i32 1, !dbg !158
  %9 = load %struct.node** %next, align 4, !dbg !158
  %cmp3 = icmp ne %struct.node* %9, null, !dbg !158
  br i1 %cmp3, label %while.body, label %while.end, !dbg !158

while.body:                                       ; preds = %while.cond
  %call4 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 24), !dbg !159
  %10 = bitcast i8* %call4 to %struct.node*, !dbg !159
  store %struct.node* %10, %struct.node** %new_node, align 4, !dbg !159
  %11 = load %struct.node** %tree, align 4, !dbg !161
  %12 = load %struct.node** %new_node, align 4, !dbg !161
  %zero_child = getelementptr inbounds %struct.node* %12, i32 0, i32 2, !dbg !161
  store %struct.node* %11, %struct.node** %zero_child, align 4, !dbg !161
  %13 = load %struct.node** %tree, align 4, !dbg !162
  %next5 = getelementptr inbounds %struct.node* %13, i32 0, i32 1, !dbg !162
  %14 = load %struct.node** %next5, align 4, !dbg !162
  %15 = load %struct.node** %new_node, align 4, !dbg !162
  %one_child = getelementptr inbounds %struct.node* %15, i32 0, i32 3, !dbg !162
  store %struct.node* %14, %struct.node** %one_child, align 4, !dbg !162
  %16 = load %struct.node** %new_node, align 4, !dbg !163
  %zero_child6 = getelementptr inbounds %struct.node* %16, i32 0, i32 2, !dbg !163
  %17 = load %struct.node** %zero_child6, align 4, !dbg !163
  %frequency = getelementptr inbounds %struct.node* %17, i32 0, i32 0, !dbg !163
  %18 = load i32* %frequency, align 4, !dbg !163
  %19 = load %struct.node** %new_node, align 4, !dbg !163
  %one_child7 = getelementptr inbounds %struct.node* %19, i32 0, i32 3, !dbg !163
  %20 = load %struct.node** %one_child7, align 4, !dbg !163
  %frequency8 = getelementptr inbounds %struct.node* %20, i32 0, i32 0, !dbg !163
  %21 = load i32* %frequency8, align 4, !dbg !163
  %add = add i32 %18, %21, !dbg !163
  %22 = load %struct.node** %new_node, align 4, !dbg !163
  %frequency9 = getelementptr inbounds %struct.node* %22, i32 0, i32 0, !dbg !163
  store i32 %add, i32* %frequency9, align 4, !dbg !163
  %23 = load %struct.node** %tree, align 4, !dbg !164
  %next10 = getelementptr inbounds %struct.node* %23, i32 0, i32 1, !dbg !164
  %24 = load %struct.node** %next10, align 4, !dbg !164
  %next11 = getelementptr inbounds %struct.node* %24, i32 0, i32 1, !dbg !164
  %25 = load %struct.node** %next11, align 4, !dbg !164
  store %struct.node* %25, %struct.node** %tree, align 4, !dbg !164
  %26 = load %struct.node** %new_node, align 4, !dbg !165
  %call12 = call arm_aapcscc  i32 @insert_in_ordered_list(%struct.node* %26, %struct.node** %tree), !dbg !165
  br label %while.cond, !dbg !166

while.end:                                        ; preds = %while.cond
  %27 = load %struct.node** %tree, align 4, !dbg !167
  %arraydecay = getelementptr inbounds [65536 x i8]* %temp_code, i32 0, i32 0, !dbg !167
  %call13 = call arm_aapcscc  i32 @compute_code_strings(%struct.node* %27, i8* %arraydecay, i32 0), !dbg !167
  %28 = load %struct.node** %tree, align 4, !dbg !168
  %29 = load %struct.node*** %tree_ptr.addr, align 4, !dbg !168
  store %struct.node* %28, %struct.node** %29, !dbg !168
  %30 = load i32* %retval, !dbg !169
  ret i32 %30, !dbg !169
}

define arm_aapcscc i32 @insert_in_ordered_list(%struct.node* %the_node, %struct.node** %list) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %the_node.addr = alloca %struct.node*, align 4
  %list.addr = alloca %struct.node**, align 4
  %the_list = alloca %struct.node*, align 4
  store %struct.node* %the_node, %struct.node** %the_node.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node** %the_node.addr}, metadata !170), !dbg !171
  store %struct.node** %list, %struct.node*** %list.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node*** %list.addr}, metadata !172), !dbg !173
  call void @llvm.dbg.declare(metadata !{%struct.node** %the_list}, metadata !174), !dbg !176
  %0 = load %struct.node*** %list.addr, align 4, !dbg !177
  %1 = load %struct.node** %0, !dbg !177
  store %struct.node* %1, %struct.node** %the_list, align 4, !dbg !177
  %2 = load %struct.node** %the_list, align 4, !dbg !178
  %cmp = icmp eq %struct.node* %2, null, !dbg !178
  br i1 %cmp, label %if.then, label %lor.lhs.false, !dbg !178

lor.lhs.false:                                    ; preds = %entry
  %3 = load %struct.node** %the_node.addr, align 4, !dbg !178
  %frequency = getelementptr inbounds %struct.node* %3, i32 0, i32 0, !dbg !178
  %4 = load i32* %frequency, align 4, !dbg !178
  %5 = load %struct.node** %the_list, align 4, !dbg !178
  %frequency1 = getelementptr inbounds %struct.node* %5, i32 0, i32 0, !dbg !178
  %6 = load i32* %frequency1, align 4, !dbg !178
  %cmp2 = icmp ule i32 %4, %6, !dbg !178
  br i1 %cmp2, label %if.then, label %if.else, !dbg !178

if.then:                                          ; preds = %lor.lhs.false, %entry
  %7 = load %struct.node** %the_list, align 4, !dbg !179
  %8 = load %struct.node** %the_node.addr, align 4, !dbg !179
  %next = getelementptr inbounds %struct.node* %8, i32 0, i32 1, !dbg !179
  store %struct.node* %7, %struct.node** %next, align 4, !dbg !179
  %9 = load %struct.node** %the_node.addr, align 4, !dbg !181
  %10 = load %struct.node*** %list.addr, align 4, !dbg !181
  store %struct.node* %9, %struct.node** %10, !dbg !181
  br label %if.end, !dbg !182

if.else:                                          ; preds = %lor.lhs.false
  br label %while.cond, !dbg !183

while.cond:                                       ; preds = %while.body, %if.else
  %11 = load %struct.node** %the_list, align 4, !dbg !183
  %next3 = getelementptr inbounds %struct.node* %11, i32 0, i32 1, !dbg !183
  %12 = load %struct.node** %next3, align 4, !dbg !183
  %cmp4 = icmp ne %struct.node* %12, null, !dbg !183
  br i1 %cmp4, label %land.rhs, label %land.end, !dbg !183

land.rhs:                                         ; preds = %while.cond
  %13 = load %struct.node** %the_node.addr, align 4, !dbg !183
  %frequency5 = getelementptr inbounds %struct.node* %13, i32 0, i32 0, !dbg !183
  %14 = load i32* %frequency5, align 4, !dbg !183
  %15 = load %struct.node** %the_list, align 4, !dbg !183
  %next6 = getelementptr inbounds %struct.node* %15, i32 0, i32 1, !dbg !183
  %16 = load %struct.node** %next6, align 4, !dbg !183
  %frequency7 = getelementptr inbounds %struct.node* %16, i32 0, i32 0, !dbg !183
  %17 = load i32* %frequency7, align 4, !dbg !183
  %cmp8 = icmp ugt i32 %14, %17, !dbg !183
  br label %land.end

land.end:                                         ; preds = %land.rhs, %while.cond
  %18 = phi i1 [ false, %while.cond ], [ %cmp8, %land.rhs ]
  br i1 %18, label %while.body, label %while.end

while.body:                                       ; preds = %land.end
  %19 = load %struct.node** %the_list, align 4, !dbg !185
  %next9 = getelementptr inbounds %struct.node* %19, i32 0, i32 1, !dbg !185
  %20 = load %struct.node** %next9, align 4, !dbg !185
  store %struct.node* %20, %struct.node** %the_list, align 4, !dbg !185
  br label %while.cond, !dbg !185

while.end:                                        ; preds = %land.end
  %21 = load %struct.node** %the_list, align 4, !dbg !186
  %next10 = getelementptr inbounds %struct.node* %21, i32 0, i32 1, !dbg !186
  %22 = load %struct.node** %next10, align 4, !dbg !186
  %23 = load %struct.node** %the_node.addr, align 4, !dbg !186
  %next11 = getelementptr inbounds %struct.node* %23, i32 0, i32 1, !dbg !186
  store %struct.node* %22, %struct.node** %next11, align 4, !dbg !186
  %24 = load %struct.node** %the_node.addr, align 4, !dbg !187
  %25 = load %struct.node** %the_list, align 4, !dbg !187
  %next12 = getelementptr inbounds %struct.node* %25, i32 0, i32 1, !dbg !187
  store %struct.node* %24, %struct.node** %next12, align 4, !dbg !187
  br label %if.end

if.end:                                           ; preds = %while.end, %if.then
  %26 = load i32* %retval, !dbg !188
  ret i32 %26, !dbg !188
}

define arm_aapcscc i32 @compute_code_strings(%struct.node* %tree, i8* %code_string, i32 %code_string_pos) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %tree.addr = alloca %struct.node*, align 4
  %code_string.addr = alloca i8*, align 4
  %code_string_pos.addr = alloca i32, align 4
  store %struct.node* %tree, %struct.node** %tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node** %tree.addr}, metadata !189), !dbg !190
  store i8* %code_string, i8** %code_string.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %code_string.addr}, metadata !191), !dbg !192
  store i32 %code_string_pos, i32* %code_string_pos.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %code_string_pos.addr}, metadata !193), !dbg !194
  %0 = load %struct.node** %tree.addr, align 4, !dbg !195
  %zero_child = getelementptr inbounds %struct.node* %0, i32 0, i32 2, !dbg !195
  %1 = load %struct.node** %zero_child, align 4, !dbg !195
  %cmp = icmp eq %struct.node* %1, null, !dbg !195
  br i1 %cmp, label %if.then, label %if.else, !dbg !195

if.then:                                          ; preds = %entry
  %2 = load i32* %code_string_pos.addr, align 4, !dbg !197
  %3 = load i8** %code_string.addr, align 4, !dbg !197
  %arrayidx = getelementptr inbounds i8* %3, i32 %2, !dbg !197
  store i8 0, i8* %arrayidx, !dbg !197
  %4 = load i32* %code_string_pos.addr, align 4, !dbg !199
  %add = add nsw i32 %4, 1, !dbg !199
  %call = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %add), !dbg !199
  %5 = load i8** %code_string.addr, align 4, !dbg !199
  %call1 = call arm_aapcscc  i8* @strcpy(i8* %call, i8* %5) nounwind, !dbg !199
  %6 = load %struct.node** %tree.addr, align 4, !dbg !199
  %code = getelementptr inbounds %struct.node* %6, i32 0, i32 5, !dbg !199
  store i8* %call1, i8** %code, align 4, !dbg !199
  br label %if.end, !dbg !200

if.else:                                          ; preds = %entry
  %7 = load i32* %code_string_pos.addr, align 4, !dbg !201
  %8 = load i8** %code_string.addr, align 4, !dbg !201
  %arrayidx2 = getelementptr inbounds i8* %8, i32 %7, !dbg !201
  store i8 48, i8* %arrayidx2, !dbg !201
  %9 = load %struct.node** %tree.addr, align 4, !dbg !203
  %zero_child3 = getelementptr inbounds %struct.node* %9, i32 0, i32 2, !dbg !203
  %10 = load %struct.node** %zero_child3, align 4, !dbg !203
  %11 = load i8** %code_string.addr, align 4, !dbg !203
  %12 = load i32* %code_string_pos.addr, align 4, !dbg !203
  %add4 = add nsw i32 %12, 1, !dbg !203
  %call5 = call arm_aapcscc  i32 @compute_code_strings(%struct.node* %10, i8* %11, i32 %add4), !dbg !203
  %13 = load i32* %code_string_pos.addr, align 4, !dbg !204
  %14 = load i8** %code_string.addr, align 4, !dbg !204
  %arrayidx6 = getelementptr inbounds i8* %14, i32 %13, !dbg !204
  store i8 49, i8* %arrayidx6, !dbg !204
  %15 = load %struct.node** %tree.addr, align 4, !dbg !205
  %one_child = getelementptr inbounds %struct.node* %15, i32 0, i32 3, !dbg !205
  %16 = load %struct.node** %one_child, align 4, !dbg !205
  %17 = load i8** %code_string.addr, align 4, !dbg !205
  %18 = load i32* %code_string_pos.addr, align 4, !dbg !205
  %add7 = add nsw i32 %18, 1, !dbg !205
  %call8 = call arm_aapcscc  i32 @compute_code_strings(%struct.node* %16, i8* %17, i32 %add7), !dbg !205
  br label %if.end

if.end:                                           ; preds = %if.else, %if.then
  %19 = load i32* %retval, !dbg !206
  ret i32 %19, !dbg !206
}

declare arm_aapcscc i8* @strcpy(i8*, i8*) nounwind

define arm_aapcscc %struct.code_node* @read_huffman_tree(%struct._IO_FILE.4* %file) nounwind uwtable {
entry:
  %file.addr = alloca %struct._IO_FILE.4*, align 4
  %the_node = alloca %struct.code_node*, align 4
  %the_byte = alloca i8, align 1
  %the_symbol = alloca i16, align 2
  %exp = alloca i16, align 2
  %i = alloca i32, align 4
  store %struct._IO_FILE.4* %file, %struct._IO_FILE.4** %file.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.4** %file.addr}, metadata !207), !dbg !263
  call void @llvm.dbg.declare(metadata !{%struct.code_node** %the_node}, metadata !264), !dbg !266
  call void @llvm.dbg.declare(metadata !{i8* %the_byte}, metadata !267), !dbg !268
  call void @llvm.dbg.declare(metadata !{i16* %the_symbol}, metadata !269), !dbg !270
  call void @llvm.dbg.declare(metadata !{i16* %exp}, metadata !271), !dbg !272
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !273), !dbg !274
  %call = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 12), !dbg !275
  %0 = bitcast i8* %call to %struct.code_node*, !dbg !275
  store %struct.code_node* %0, %struct.code_node** %the_node, align 4, !dbg !275
  %1 = load %struct._IO_FILE.4** %file.addr, align 4, !dbg !276
  %call1 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.4* %1), !dbg !276
  %2 = load i8* @temp_byte, align 1, !dbg !277
  store i8 %2, i8* %the_byte, align 1, !dbg !277
  %3 = load i8* %the_byte, align 1, !dbg !278
  %conv = zext i8 %3 to i32, !dbg !278
  %cmp = icmp eq i32 %conv, 0, !dbg !278
  br i1 %cmp, label %if.then, label %if.else, !dbg !278

if.then:                                          ; preds = %entry
  %4 = load %struct._IO_FILE.4** %file.addr, align 4, !dbg !279
  %call3 = call arm_aapcscc  %struct.code_node* @read_huffman_tree(%struct._IO_FILE.4* %4), !dbg !279
  %5 = load %struct.code_node** %the_node, align 4, !dbg !279
  %zero_child = getelementptr inbounds %struct.code_node* %5, i32 0, i32 0, !dbg !279
  store %struct.code_node* %call3, %struct.code_node** %zero_child, align 4, !dbg !279
  %6 = load %struct._IO_FILE.4** %file.addr, align 4, !dbg !281
  %call4 = call arm_aapcscc  %struct.code_node* @read_huffman_tree(%struct._IO_FILE.4* %6), !dbg !281
  %7 = load %struct.code_node** %the_node, align 4, !dbg !281
  %one_child = getelementptr inbounds %struct.code_node* %7, i32 0, i32 1, !dbg !281
  store %struct.code_node* %call4, %struct.code_node** %one_child, align 4, !dbg !281
  br label %if.end22, !dbg !282

if.else:                                          ; preds = %entry
  %8 = load i8* %the_byte, align 1, !dbg !283
  %conv5 = zext i8 %8 to i32, !dbg !283
  %cmp6 = icmp eq i32 %conv5, 1, !dbg !283
  br i1 %cmp6, label %if.then8, label %if.else20, !dbg !283

if.then8:                                         ; preds = %if.else
  %9 = load %struct.code_node** %the_node, align 4, !dbg !284
  %one_child9 = getelementptr inbounds %struct.code_node* %9, i32 0, i32 1, !dbg !284
  store %struct.code_node* null, %struct.code_node** %one_child9, align 4, !dbg !284
  %10 = load %struct.code_node** %the_node, align 4, !dbg !284
  %zero_child10 = getelementptr inbounds %struct.code_node* %10, i32 0, i32 0, !dbg !284
  store %struct.code_node* null, %struct.code_node** %zero_child10, align 4, !dbg !284
  store i16 0, i16* %the_symbol, align 2, !dbg !286
  store i32 0, i32* %i, align 4, !dbg !287
  store i16 1, i16* %exp, align 2, !dbg !287
  br label %for.cond, !dbg !287

for.cond:                                         ; preds = %for.inc, %if.then8
  %11 = load i32* %i, align 4, !dbg !287
  %cmp11 = icmp ult i32 %11, 2, !dbg !287
  br i1 %cmp11, label %for.body, label %for.end, !dbg !287

for.body:                                         ; preds = %for.cond
  %12 = load %struct._IO_FILE.4** %file.addr, align 4, !dbg !289
  %call13 = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.4* %12), !dbg !289
  %13 = load i8* @temp_byte, align 1, !dbg !291
  store i8 %13, i8* %the_byte, align 1, !dbg !291
  %14 = load i8* %the_byte, align 1, !dbg !292
  %conv14 = zext i8 %14 to i32, !dbg !292
  %15 = load i16* %exp, align 2, !dbg !292
  %conv15 = zext i16 %15 to i32, !dbg !292
  %mul = mul nsw i32 %conv14, %conv15, !dbg !292
  %16 = load i16* %the_symbol, align 2, !dbg !292
  %conv16 = zext i16 %16 to i32, !dbg !292
  %add = add nsw i32 %conv16, %mul, !dbg !292
  %conv17 = trunc i32 %add to i16, !dbg !292
  store i16 %conv17, i16* %the_symbol, align 2, !dbg !292
  br label %for.inc, !dbg !293

for.inc:                                          ; preds = %for.body
  %17 = load i32* %i, align 4, !dbg !294
  %inc = add nsw i32 %17, 1, !dbg !294
  store i32 %inc, i32* %i, align 4, !dbg !294
  %18 = load i16* %exp, align 2, !dbg !294
  %conv18 = zext i16 %18 to i32, !dbg !294
  %shl = shl i32 %conv18, 8, !dbg !294
  %conv19 = trunc i32 %shl to i16, !dbg !294
  store i16 %conv19, i16* %exp, align 2, !dbg !294
  br label %for.cond, !dbg !294

for.end:                                          ; preds = %for.cond
  %19 = load i16* %the_symbol, align 2, !dbg !295
  %20 = load %struct.code_node** %the_node, align 4, !dbg !295
  %symbol = getelementptr inbounds %struct.code_node* %20, i32 0, i32 2, !dbg !295
  store i16 %19, i16* %symbol, align 2, !dbg !295
  br label %if.end, !dbg !296

if.else20:                                        ; preds = %if.else
  %call21 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([29 x i8]* @.str, i32 0, i32 0)), !dbg !297
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !299
  unreachable, !dbg !299

if.end:                                           ; preds = %for.end
  br label %if.end22

if.end22:                                         ; preds = %if.end, %if.then
  %21 = load %struct.code_node** %the_node, align 4, !dbg !300
  ret %struct.code_node* %21, !dbg !300
}

declare arm_aapcscc i32 @fread(i8*, i32, i32, %struct._IO_FILE.4*)

declare arm_aapcscc i32 @printf(i8*, ...)

declare arm_aapcscc void @exit(i32) noreturn

define arm_aapcscc i32 @read_and_huffman_decode(%struct._IO_FILE.4* %file, %struct.code_node* %tree, i16* %symbol_stream, i32 %count) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %file.addr = alloca %struct._IO_FILE.4*, align 4
  %tree.addr = alloca %struct.code_node*, align 4
  %symbol_stream.addr = alloca i16*, align 4
  %count.addr = alloca i32, align 4
  %the_byte = alloca i8, align 1
  %bit_mask = alloca i8, align 1
  %the_node = alloca %struct.code_node*, align 4
  %i = alloca i32, align 4
  store %struct._IO_FILE.4* %file, %struct._IO_FILE.4** %file.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE.4** %file.addr}, metadata !301), !dbg !302
  store %struct.code_node* %tree, %struct.code_node** %tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.code_node** %tree.addr}, metadata !303), !dbg !304
  store i16* %symbol_stream, i16** %symbol_stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %symbol_stream.addr}, metadata !305), !dbg !306
  store i32 %count, i32* %count.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %count.addr}, metadata !307), !dbg !308
  call void @llvm.dbg.declare(metadata !{i8* %the_byte}, metadata !309), !dbg !311
  call void @llvm.dbg.declare(metadata !{i8* %bit_mask}, metadata !312), !dbg !313
  store i8 0, i8* %bit_mask, align 1, !dbg !314
  call void @llvm.dbg.declare(metadata !{%struct.code_node** %the_node}, metadata !315), !dbg !316
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !317), !dbg !318
  store i32 0, i32* %i, align 4, !dbg !319
  br label %for.cond, !dbg !319

for.cond:                                         ; preds = %for.inc, %entry
  %0 = load i32* %i, align 4, !dbg !319
  %1 = load i32* %count.addr, align 4, !dbg !319
  %cmp = icmp slt i32 %0, %1, !dbg !319
  br i1 %cmp, label %for.body, label %for.end, !dbg !319

for.body:                                         ; preds = %for.cond
  %2 = load %struct.code_node** %tree.addr, align 4, !dbg !321
  store %struct.code_node* %2, %struct.code_node** %the_node, align 4, !dbg !321
  br label %while.cond, !dbg !323

while.cond:                                       ; preds = %if.end8, %for.body
  %3 = load %struct.code_node** %the_node, align 4, !dbg !323
  %zero_child = getelementptr inbounds %struct.code_node* %3, i32 0, i32 0, !dbg !323
  %4 = load %struct.code_node** %zero_child, align 4, !dbg !323
  %cmp1 = icmp ne %struct.code_node* %4, null, !dbg !323
  br i1 %cmp1, label %while.body, label %while.end, !dbg !323

while.body:                                       ; preds = %while.cond
  %5 = load i8* %bit_mask, align 1, !dbg !324
  %conv = zext i8 %5 to i32, !dbg !324
  %cmp2 = icmp eq i32 %conv, 0, !dbg !324
  br i1 %cmp2, label %if.then, label %if.end, !dbg !324

if.then:                                          ; preds = %while.body
  %6 = load %struct._IO_FILE.4** %file.addr, align 4, !dbg !326
  %call = call arm_aapcscc  i32 @fread(i8* @temp_byte, i32 1, i32 1, %struct._IO_FILE.4* %6), !dbg !326
  %7 = load i8* @temp_byte, align 1, !dbg !328
  store i8 %7, i8* %the_byte, align 1, !dbg !328
  store i8 1, i8* %bit_mask, align 1, !dbg !329
  br label %if.end, !dbg !330

if.end:                                           ; preds = %if.then, %while.body
  %8 = load i8* %bit_mask, align 1, !dbg !331
  %conv4 = zext i8 %8 to i32, !dbg !331
  %9 = load i8* %the_byte, align 1, !dbg !331
  %conv5 = zext i8 %9 to i32, !dbg !331
  %and = and i32 %conv4, %conv5, !dbg !331
  %tobool = icmp ne i32 %and, 0, !dbg !331
  br i1 %tobool, label %if.then6, label %if.else, !dbg !331

if.then6:                                         ; preds = %if.end
  %10 = load %struct.code_node** %the_node, align 4, !dbg !332
  %one_child = getelementptr inbounds %struct.code_node* %10, i32 0, i32 1, !dbg !332
  %11 = load %struct.code_node** %one_child, align 4, !dbg !332
  store %struct.code_node* %11, %struct.code_node** %the_node, align 4, !dbg !332
  br label %if.end8, !dbg !332

if.else:                                          ; preds = %if.end
  %12 = load %struct.code_node** %the_node, align 4, !dbg !333
  %zero_child7 = getelementptr inbounds %struct.code_node* %12, i32 0, i32 0, !dbg !333
  %13 = load %struct.code_node** %zero_child7, align 4, !dbg !333
  store %struct.code_node* %13, %struct.code_node** %the_node, align 4, !dbg !333
  br label %if.end8

if.end8:                                          ; preds = %if.else, %if.then6
  %14 = load i8* %bit_mask, align 1, !dbg !334
  %conv9 = zext i8 %14 to i32, !dbg !334
  %shl = shl i32 %conv9, 1, !dbg !334
  %conv10 = trunc i32 %shl to i8, !dbg !334
  store i8 %conv10, i8* %bit_mask, align 1, !dbg !334
  br label %while.cond, !dbg !335

while.end:                                        ; preds = %while.cond
  %15 = load %struct.code_node** %the_node, align 4, !dbg !336
  %symbol = getelementptr inbounds %struct.code_node* %15, i32 0, i32 2, !dbg !336
  %16 = load i16* %symbol, align 2, !dbg !336
  %17 = load i32* %i, align 4, !dbg !336
  %18 = load i16** %symbol_stream.addr, align 4, !dbg !336
  %arrayidx = getelementptr inbounds i16* %18, i32 %17, !dbg !336
  store i16 %16, i16* %arrayidx, !dbg !336
  br label %for.inc, !dbg !337

for.inc:                                          ; preds = %while.end
  %19 = load i32* %i, align 4, !dbg !338
  %inc = add nsw i32 %19, 1, !dbg !338
  store i32 %inc, i32* %i, align 4, !dbg !338
  br label %for.cond, !dbg !338

for.end:                                          ; preds = %for.cond
  %20 = load i32* %retval, !dbg !339
  ret i32 %20, !dbg !339
}

define arm_aapcscc i32 @encode_stream(i16* %symbol_stream, i32 %symbol_stream_length, %struct.node** %histogram, i8* %encoded_stream) nounwind uwtable {
entry:
  %symbol_stream.addr = alloca i16*, align 4
  %symbol_stream_length.addr = alloca i32, align 4
  %histogram.addr = alloca %struct.node**, align 4
  %encoded_stream.addr = alloca i8*, align 4
  %i = alloca i32, align 4
  %the_node = alloca %struct.node*, align 4
  %the_code_string = alloca i8*, align 4
  %code_pointer = alloca i8*, align 4
  %bit_mask = alloca i8, align 1
  store i16* %symbol_stream, i16** %symbol_stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{i16** %symbol_stream.addr}, metadata !340), !dbg !341
  store i32 %symbol_stream_length, i32* %symbol_stream_length.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %symbol_stream_length.addr}, metadata !342), !dbg !343
  store %struct.node** %histogram, %struct.node*** %histogram.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node*** %histogram.addr}, metadata !344), !dbg !345
  store i8* %encoded_stream, i8** %encoded_stream.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %encoded_stream.addr}, metadata !346), !dbg !347
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !348), !dbg !350
  call void @llvm.dbg.declare(metadata !{%struct.node** %the_node}, metadata !351), !dbg !352
  call void @llvm.dbg.declare(metadata !{i8** %the_code_string}, metadata !353), !dbg !354
  call void @llvm.dbg.declare(metadata !{i8** %code_pointer}, metadata !355), !dbg !356
  %0 = load i8** %encoded_stream.addr, align 4, !dbg !357
  store i8* %0, i8** %code_pointer, align 4, !dbg !357
  call void @llvm.dbg.declare(metadata !{i8* %bit_mask}, metadata !358), !dbg !359
  store i8 1, i8* %bit_mask, align 1, !dbg !360
  %1 = load i8** %code_pointer, align 4, !dbg !361
  store i8 0, i8* %1, !dbg !361
  store i32 0, i32* %i, align 4, !dbg !362
  br label %for.cond, !dbg !362

for.cond:                                         ; preds = %for.inc, %entry
  %2 = load i32* %i, align 4, !dbg !362
  %3 = load i32* %symbol_stream_length.addr, align 4, !dbg !362
  %cmp = icmp slt i32 %2, %3, !dbg !362
  br i1 %cmp, label %for.body, label %for.end, !dbg !362

for.body:                                         ; preds = %for.cond
  %4 = load i16** %symbol_stream.addr, align 4, !dbg !364
  %5 = load i16* %4, !dbg !364
  %idxprom = zext i16 %5 to i32, !dbg !364
  %6 = load %struct.node*** %histogram.addr, align 4, !dbg !364
  %arrayidx = getelementptr inbounds %struct.node** %6, i32 %idxprom, !dbg !364
  %7 = load %struct.node** %arrayidx, !dbg !364
  store %struct.node* %7, %struct.node** %the_node, align 4, !dbg !364
  %8 = load %struct.node** %the_node, align 4, !dbg !366
  %cmp1 = icmp eq %struct.node* %8, null, !dbg !366
  br i1 %cmp1, label %if.then, label %if.end, !dbg !366

if.then:                                          ; preds = %for.body
  %call = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([40 x i8]* @.str1, i32 0, i32 0)), !dbg !367
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !369
  unreachable, !dbg !369

if.end:                                           ; preds = %for.body
  %9 = load %struct.node** %the_node, align 4, !dbg !370
  %code = getelementptr inbounds %struct.node* %9, i32 0, i32 5, !dbg !370
  %10 = load i8** %code, align 4, !dbg !370
  store i8* %10, i8** %the_code_string, align 4, !dbg !370
  br label %while.cond, !dbg !371

while.cond:                                       ; preds = %if.end16, %if.end
  %11 = load i8** %the_code_string, align 4, !dbg !371
  %12 = load i8* %11, !dbg !371
  %tobool = icmp ne i8 %12, 0, !dbg !371
  br i1 %tobool, label %while.body, label %while.end, !dbg !371

while.body:                                       ; preds = %while.cond
  %13 = load i8** %the_code_string, align 4, !dbg !372
  %14 = load i8* %13, !dbg !372
  %conv = sext i8 %14 to i32, !dbg !372
  %cmp2 = icmp eq i32 %conv, 49, !dbg !372
  br i1 %cmp2, label %if.then4, label %if.end8, !dbg !372

if.then4:                                         ; preds = %while.body
  %15 = load i8* %bit_mask, align 1, !dbg !374
  %conv5 = zext i8 %15 to i32, !dbg !374
  %16 = load i8** %code_pointer, align 4, !dbg !374
  %17 = load i8* %16, !dbg !374
  %conv6 = zext i8 %17 to i32, !dbg !374
  %or = or i32 %conv6, %conv5, !dbg !374
  %conv7 = trunc i32 %or to i8, !dbg !374
  store i8 %conv7, i8* %16, !dbg !374
  br label %if.end8, !dbg !374

if.end8:                                          ; preds = %if.then4, %while.body
  %18 = load i8** %the_code_string, align 4, !dbg !375
  %incdec.ptr = getelementptr inbounds i8* %18, i32 1, !dbg !375
  store i8* %incdec.ptr, i8** %the_code_string, align 4, !dbg !375
  %19 = load i8* %bit_mask, align 1, !dbg !376
  %conv9 = zext i8 %19 to i32, !dbg !376
  %shl = shl i32 %conv9, 1, !dbg !376
  %conv10 = trunc i32 %shl to i8, !dbg !376
  store i8 %conv10, i8* %bit_mask, align 1, !dbg !376
  %20 = load i8* %bit_mask, align 1, !dbg !377
  %conv11 = zext i8 %20 to i32, !dbg !377
  %cmp12 = icmp eq i32 %conv11, 0, !dbg !377
  br i1 %cmp12, label %if.then14, label %if.end16, !dbg !377

if.then14:                                        ; preds = %if.end8
  store i8 1, i8* %bit_mask, align 1, !dbg !378
  %21 = load i8** %code_pointer, align 4, !dbg !380
  %incdec.ptr15 = getelementptr inbounds i8* %21, i32 1, !dbg !380
  store i8* %incdec.ptr15, i8** %code_pointer, align 4, !dbg !380
  %22 = load i8** %code_pointer, align 4, !dbg !381
  store i8 0, i8* %22, !dbg !381
  br label %if.end16, !dbg !382

if.end16:                                         ; preds = %if.then14, %if.end8
  br label %while.cond, !dbg !383

while.end:                                        ; preds = %while.cond
  br label %for.inc, !dbg !384

for.inc:                                          ; preds = %while.end
  %23 = load i32* %i, align 4, !dbg !385
  %inc = add nsw i32 %23, 1, !dbg !385
  store i32 %inc, i32* %i, align 4, !dbg !385
  %24 = load i16** %symbol_stream.addr, align 4, !dbg !385
  %incdec.ptr17 = getelementptr inbounds i16* %24, i32 1, !dbg !385
  store i16* %incdec.ptr17, i16** %symbol_stream.addr, align 4, !dbg !385
  br label %for.cond, !dbg !385

for.end:                                          ; preds = %for.cond
  %25 = load i8** %code_pointer, align 4, !dbg !386
  %26 = load i8** %encoded_stream.addr, align 4, !dbg !386
  %sub.ptr.lhs.cast = ptrtoint i8* %25 to i32, !dbg !386
  %sub.ptr.rhs.cast = ptrtoint i8* %26 to i32, !dbg !386
  %sub.ptr.sub = sub i32 %sub.ptr.lhs.cast, %sub.ptr.rhs.cast, !dbg !386
  %add = add nsw i32 %sub.ptr.sub, 1, !dbg !386
  ret i32 %add, !dbg !386
}

define arm_aapcscc i32 @pack_huffman_tree(%struct.node* %tree, i8** %packed_tree) nounwind uwtable {
entry:
  %tree.addr = alloca %struct.node*, align 4
  %packed_tree.addr = alloca i8**, align 4
  %packed_tree_ptr = alloca i8*, align 4
  %num_leaves = alloca i32, align 4
  store %struct.node* %tree, %struct.node** %tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node** %tree.addr}, metadata !387), !dbg !388
  store i8** %packed_tree, i8*** %packed_tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8*** %packed_tree.addr}, metadata !389), !dbg !390
  call void @llvm.dbg.declare(metadata !{i8** %packed_tree_ptr}, metadata !391), !dbg !393
  call void @llvm.dbg.declare(metadata !{i32* %num_leaves}, metadata !394), !dbg !395
  %0 = load %struct.node** %tree.addr, align 4, !dbg !396
  %call = call arm_aapcscc  i32 @count_leaves(%struct.node* %0), !dbg !396
  store i32 %call, i32* %num_leaves, align 4, !dbg !396
  %1 = load i32* %num_leaves, align 4, !dbg !397
  %mul = mul nsw i32 2, %1, !dbg !397
  %2 = load i32* %num_leaves, align 4, !dbg !397
  %mul1 = mul i32 2, %2, !dbg !397
  %add = add i32 %mul, %mul1, !dbg !397
  %sub = sub i32 %add, 1, !dbg !397
  %call2 = call arm_aapcscc  i8* bitcast (i8* (...)* @check_malloc to i8* (i32)*)(i32 %sub), !dbg !397
  %3 = load i8*** %packed_tree.addr, align 4, !dbg !397
  store i8* %call2, i8** %3, !dbg !397
  store i8* %call2, i8** %packed_tree_ptr, align 4, !dbg !397
  %4 = load %struct.node** %tree.addr, align 4, !dbg !398
  %call3 = call arm_aapcscc  i32 @pack_tree_iter(%struct.node* %4, i8** %packed_tree_ptr), !dbg !398
  %5 = load i8** %packed_tree_ptr, align 4, !dbg !399
  %6 = load i8*** %packed_tree.addr, align 4, !dbg !399
  %7 = load i8** %6, !dbg !399
  %sub.ptr.lhs.cast = ptrtoint i8* %5 to i32, !dbg !399
  %sub.ptr.rhs.cast = ptrtoint i8* %7 to i32, !dbg !399
  %sub.ptr.sub = sub i32 %sub.ptr.lhs.cast, %sub.ptr.rhs.cast, !dbg !399
  ret i32 %sub.ptr.sub, !dbg !399
}

define arm_aapcscc i32 @pack_tree_iter(%struct.node* %tree, i8** %packed_tree_ptr) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %tree.addr = alloca %struct.node*, align 4
  %packed_tree_ptr.addr = alloca i8**, align 4
  %the_symbol = alloca i16, align 2
  %i = alloca i32, align 4
  store %struct.node* %tree, %struct.node** %tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node** %tree.addr}, metadata !400), !dbg !401
  store i8** %packed_tree_ptr, i8*** %packed_tree_ptr.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8*** %packed_tree_ptr.addr}, metadata !402), !dbg !403
  call void @llvm.dbg.declare(metadata !{i16* %the_symbol}, metadata !404), !dbg !406
  call void @llvm.dbg.declare(metadata !{i32* %i}, metadata !407), !dbg !408
  %0 = load %struct.node** %tree.addr, align 4, !dbg !409
  %zero_child = getelementptr inbounds %struct.node* %0, i32 0, i32 2, !dbg !409
  %1 = load %struct.node** %zero_child, align 4, !dbg !409
  %cmp = icmp eq %struct.node* %1, null, !dbg !409
  br i1 %cmp, label %if.then, label %if.else, !dbg !409

if.then:                                          ; preds = %entry
  %2 = load i8*** %packed_tree_ptr.addr, align 4, !dbg !410
  %3 = load i8** %2, !dbg !410
  store i8 1, i8* %3, !dbg !410
  %4 = load i8*** %packed_tree_ptr.addr, align 4, !dbg !412
  %5 = load i8** %4, !dbg !412
  %incdec.ptr = getelementptr inbounds i8* %5, i32 1, !dbg !412
  store i8* %incdec.ptr, i8** %4, !dbg !412
  %6 = load %struct.node** %tree.addr, align 4, !dbg !413
  %symbol = getelementptr inbounds %struct.node* %6, i32 0, i32 4, !dbg !413
  %7 = load i16* %symbol, align 2, !dbg !413
  store i16 %7, i16* %the_symbol, align 2, !dbg !413
  store i32 0, i32* %i, align 4, !dbg !414
  br label %for.cond, !dbg !414

for.cond:                                         ; preds = %for.inc, %if.then
  %8 = load i32* %i, align 4, !dbg !414
  %cmp1 = icmp ult i32 %8, 2, !dbg !414
  br i1 %cmp1, label %for.body, label %for.end, !dbg !414

for.body:                                         ; preds = %for.cond
  %9 = load i16* %the_symbol, align 2, !dbg !416
  %conv = zext i16 %9 to i32, !dbg !416
  %and = and i32 %conv, 255, !dbg !416
  %conv2 = trunc i32 %and to i8, !dbg !416
  %10 = load i8*** %packed_tree_ptr.addr, align 4, !dbg !416
  %11 = load i8** %10, !dbg !416
  store i8 %conv2, i8* %11, !dbg !416
  %12 = load i8*** %packed_tree_ptr.addr, align 4, !dbg !418
  %13 = load i8** %12, !dbg !418
  %incdec.ptr3 = getelementptr inbounds i8* %13, i32 1, !dbg !418
  store i8* %incdec.ptr3, i8** %12, !dbg !418
  %14 = load i16* %the_symbol, align 2, !dbg !419
  %conv4 = zext i16 %14 to i32, !dbg !419
  %shr = ashr i32 %conv4, 8, !dbg !419
  %conv5 = trunc i32 %shr to i16, !dbg !419
  store i16 %conv5, i16* %the_symbol, align 2, !dbg !419
  br label %for.inc, !dbg !420

for.inc:                                          ; preds = %for.body
  %15 = load i32* %i, align 4, !dbg !421
  %inc = add nsw i32 %15, 1, !dbg !421
  store i32 %inc, i32* %i, align 4, !dbg !421
  br label %for.cond, !dbg !421

for.end:                                          ; preds = %for.cond
  br label %if.end, !dbg !422

if.else:                                          ; preds = %entry
  %16 = load i8*** %packed_tree_ptr.addr, align 4, !dbg !423
  %17 = load i8** %16, !dbg !423
  store i8 0, i8* %17, !dbg !423
  %18 = load i8*** %packed_tree_ptr.addr, align 4, !dbg !425
  %19 = load i8** %18, !dbg !425
  %incdec.ptr6 = getelementptr inbounds i8* %19, i32 1, !dbg !425
  store i8* %incdec.ptr6, i8** %18, !dbg !425
  %20 = load %struct.node** %tree.addr, align 4, !dbg !426
  %zero_child7 = getelementptr inbounds %struct.node* %20, i32 0, i32 2, !dbg !426
  %21 = load %struct.node** %zero_child7, align 4, !dbg !426
  %22 = load i8*** %packed_tree_ptr.addr, align 4, !dbg !426
  %call = call arm_aapcscc  i32 @pack_tree_iter(%struct.node* %21, i8** %22), !dbg !426
  %23 = load %struct.node** %tree.addr, align 4, !dbg !427
  %one_child = getelementptr inbounds %struct.node* %23, i32 0, i32 3, !dbg !427
  %24 = load %struct.node** %one_child, align 4, !dbg !427
  %25 = load i8*** %packed_tree_ptr.addr, align 4, !dbg !427
  %call8 = call arm_aapcscc  i32 @pack_tree_iter(%struct.node* %24, i8** %25), !dbg !427
  br label %if.end

if.end:                                           ; preds = %if.else, %for.end
  %26 = load i32* %retval, !dbg !428
  ret i32 %26, !dbg !428
}

define arm_aapcscc i32 @count_leaves(%struct.node* %tree) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %tree.addr = alloca %struct.node*, align 4
  store %struct.node* %tree, %struct.node** %tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node** %tree.addr}, metadata !429), !dbg !430
  %0 = load %struct.node** %tree.addr, align 4, !dbg !431
  %zero_child = getelementptr inbounds %struct.node* %0, i32 0, i32 2, !dbg !431
  %1 = load %struct.node** %zero_child, align 4, !dbg !431
  %cmp = icmp eq %struct.node* %1, null, !dbg !431
  br i1 %cmp, label %if.then, label %if.else, !dbg !431

if.then:                                          ; preds = %entry
  store i32 1, i32* %retval, !dbg !433
  br label %return, !dbg !433

if.else:                                          ; preds = %entry
  %2 = load %struct.node** %tree.addr, align 4, !dbg !434
  %zero_child1 = getelementptr inbounds %struct.node* %2, i32 0, i32 2, !dbg !434
  %3 = load %struct.node** %zero_child1, align 4, !dbg !434
  %call = call arm_aapcscc  i32 @count_leaves(%struct.node* %3), !dbg !434
  %4 = load %struct.node** %tree.addr, align 4, !dbg !435
  %one_child = getelementptr inbounds %struct.node* %4, i32 0, i32 3, !dbg !435
  %5 = load %struct.node** %one_child, align 4, !dbg !435
  %call2 = call arm_aapcscc  i32 @count_leaves(%struct.node* %5), !dbg !435
  %add = add nsw i32 %call, %call2, !dbg !435
  store i32 %add, i32* %retval, !dbg !435
  br label %return, !dbg !435

return:                                           ; preds = %if.else, %if.then
  %6 = load i32* %retval, !dbg !436
  ret i32 %6, !dbg !436
}

define arm_aapcscc i32 @free_tree_nodes(%struct.node* %tree) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %tree.addr = alloca %struct.node*, align 4
  store %struct.node* %tree, %struct.node** %tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node** %tree.addr}, metadata !437), !dbg !438
  %0 = load %struct.node** %tree.addr, align 4, !dbg !439
  %zero_child = getelementptr inbounds %struct.node* %0, i32 0, i32 2, !dbg !439
  %1 = load %struct.node** %zero_child, align 4, !dbg !439
  %cmp = icmp ne %struct.node* %1, null, !dbg !439
  br i1 %cmp, label %if.then, label %if.else, !dbg !439

if.then:                                          ; preds = %entry
  %2 = load %struct.node** %tree.addr, align 4, !dbg !441
  %zero_child1 = getelementptr inbounds %struct.node* %2, i32 0, i32 2, !dbg !441
  %3 = load %struct.node** %zero_child1, align 4, !dbg !441
  %call = call arm_aapcscc  i32 @free_tree_nodes(%struct.node* %3), !dbg !441
  %4 = load %struct.node** %tree.addr, align 4, !dbg !443
  %one_child = getelementptr inbounds %struct.node* %4, i32 0, i32 3, !dbg !443
  %5 = load %struct.node** %one_child, align 4, !dbg !443
  %call2 = call arm_aapcscc  i32 @free_tree_nodes(%struct.node* %5), !dbg !443
  br label %if.end, !dbg !444

if.else:                                          ; preds = %entry
  %6 = load %struct.node** %tree.addr, align 4, !dbg !445
  %code = getelementptr inbounds %struct.node* %6, i32 0, i32 5, !dbg !445
  %7 = load i8** %code, align 4, !dbg !445
  %call3 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %7), !dbg !445
  br label %if.end

if.end:                                           ; preds = %if.else, %if.then
  %8 = load %struct.node** %tree.addr, align 4, !dbg !446
  %9 = bitcast %struct.node* %8 to i8*, !dbg !446
  %call4 = call arm_aapcscc  i32 bitcast (i32 (...)* @check_free to i32 (i8*)*)(i8* %9), !dbg !446
  %10 = load i32* %retval, !dbg !447
  ret i32 %10, !dbg !447
}

declare arm_aapcscc i32 @check_free(...)

define arm_aapcscc i32 @print_list_nodes(%struct.node* %tree, i32 %flag) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %tree.addr = alloca %struct.node*, align 4
  %flag.addr = alloca i32, align 4
  store %struct.node* %tree, %struct.node** %tree.addr, align 4
  call void @llvm.dbg.declare(metadata !{%struct.node** %tree.addr}, metadata !448), !dbg !449
  store i32 %flag, i32* %flag.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %flag.addr}, metadata !450), !dbg !451
  %0 = load i32* %flag.addr, align 4, !dbg !452
  %tobool = icmp ne i32 %0, 0, !dbg !452
  br i1 %tobool, label %if.then, label %if.end, !dbg !452

if.then:                                          ; preds = %entry
  %call = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([20 x i8]* @.str2, i32 0, i32 0)), !dbg !454
  br label %if.end, !dbg !454

if.end:                                           ; preds = %if.then, %entry
  %1 = load %struct.node** %tree.addr, align 4, !dbg !455
  %symbol = getelementptr inbounds %struct.node* %1, i32 0, i32 4, !dbg !455
  %2 = load i16* %symbol, align 2, !dbg !455
  %conv = zext i16 %2 to i32, !dbg !455
  %3 = load %struct.node** %tree.addr, align 4, !dbg !455
  %frequency = getelementptr inbounds %struct.node* %3, i32 0, i32 0, !dbg !455
  %4 = load i32* %frequency, align 4, !dbg !455
  %call1 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([11 x i8]* @.str3, i32 0, i32 0), i32 %conv, i32 %4), !dbg !455
  %5 = load %struct.node** %tree.addr, align 4, !dbg !456
  %next = getelementptr inbounds %struct.node* %5, i32 0, i32 1, !dbg !456
  %6 = load %struct.node** %next, align 4, !dbg !456
  %cmp = icmp ne %struct.node* %6, null, !dbg !456
  br i1 %cmp, label %if.then3, label %if.else, !dbg !456

if.then3:                                         ; preds = %if.end
  %7 = load %struct.node** %tree.addr, align 4, !dbg !457
  %next4 = getelementptr inbounds %struct.node* %7, i32 0, i32 1, !dbg !457
  %8 = load %struct.node** %next4, align 4, !dbg !457
  %call5 = call arm_aapcscc  i32 @print_list_nodes(%struct.node* %8, i32 0), !dbg !457
  br label %if.end7, !dbg !457

if.else:                                          ; preds = %if.end
  %call6 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([2 x i8]* @.str4, i32 0, i32 0)), !dbg !458
  br label %if.end7

if.end7:                                          ; preds = %if.else, %if.then3
  %9 = load i32* %retval, !dbg !459
  ret i32 %9, !dbg !459
}

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !1} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !12, metadata !15, metadata !18, metadata !21, metadata !24, metadata !38, metadata !41, metadata !44, metadata !47, metadata !50, metadata !53, metadata !56}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"huffman_encode", metadata !"huffman_encode", metadata !"", metadata !6, i32 70, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i16*, i32, i8**, i32*, i8*)* @huffman_encode, null, null, metadata !10} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"huffman.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!10 = metadata !{metadata !11}
!11 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!12 = metadata !{i32 720942, i32 0, metadata !6, metadata !"compute_histogram", metadata !"compute_histogram", metadata !"", metadata !6, i32 92, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i16*, i32, %struct.node**)* @compute_histogram, null, null, metadata !13} ; [ DW_TAG_subprogram ]
!13 = metadata !{metadata !14}
!14 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!15 = metadata !{i32 720942, i32 0, metadata !6, metadata !"build_huffman_tree", metadata !"build_huffman_tree", metadata !"", metadata !6, i32 115, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct.node**, %struct.node**)* @build_huffman_tree, null, null, metadata !16} ; [ DW_TAG_subprogram ]
!16 = metadata !{metadata !17}
!17 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!18 = metadata !{i32 720942, i32 0, metadata !6, metadata !"insert_in_ordered_list", metadata !"insert_in_ordered_list", metadata !"", metadata !6, i32 146, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct.node*, %struct.node**)* @insert_in_ordered_list, null, null, metadata !19} ; [ DW_TAG_subprogram ]
!19 = metadata !{metadata !20}
!20 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!21 = metadata !{i32 720942, i32 0, metadata !6, metadata !"compute_code_strings", metadata !"compute_code_strings", metadata !"", metadata !6, i32 171, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct.node*, i8*, i32)* @compute_code_strings, null, null, metadata !22} ; [ DW_TAG_subprogram ]
!22 = metadata !{metadata !23}
!23 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!24 = metadata !{i32 720942, i32 0, metadata !6, metadata !"read_huffman_tree", metadata !"read_huffman_tree", metadata !"", metadata !6, i32 189, metadata !25, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, %struct.code_node* (%struct._IO_FILE.4*)* @read_huffman_tree, null, null, metadata !36} ; [ DW_TAG_subprogram ]
!25 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !26, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!26 = metadata !{metadata !27}
!27 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !28} ; [ DW_TAG_pointer_type ]
!28 = metadata !{i32 720915, null, metadata !"code_node", metadata !29, i32 142, i64 96, i64 32, i32 0, i32 0, null, metadata !30, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!29 = metadata !{i32 720937, metadata !"epic.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!30 = metadata !{metadata !31, metadata !32, metadata !33}
!31 = metadata !{i32 720909, metadata !28, metadata !"zero_child", metadata !29, i32 144, i64 32, i64 32, i64 0, i32 0, metadata !27} ; [ DW_TAG_member ]
!32 = metadata !{i32 720909, metadata !28, metadata !"one_child", metadata !29, i32 144, i64 32, i64 32, i64 32, i32 0, metadata !27} ; [ DW_TAG_member ]
!33 = metadata !{i32 720909, metadata !28, metadata !"symbol", metadata !29, i32 145, i64 16, i64 16, i64 64, i32 0, metadata !34} ; [ DW_TAG_member ]
!34 = metadata !{i32 720918, null, metadata !"SymbolType", metadata !6, i32 74, i64 0, i64 0, i64 0, i32 0, metadata !35} ; [ DW_TAG_typedef ]
!35 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!36 = metadata !{metadata !37}
!37 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!38 = metadata !{i32 720942, i32 0, metadata !6, metadata !"read_and_huffman_decode", metadata !"read_and_huffman_decode", metadata !"", metadata !6, i32 226, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct._IO_FILE.4*, %struct.code_node*, i16*, i32)* @read_and_huffman_decode, null, null, metadata !39} ; [ DW_TAG_subprogram ]
!39 = metadata !{metadata !40}
!40 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!41 = metadata !{i32 720942, i32 0, metadata !6, metadata !"encode_stream", metadata !"encode_stream", metadata !"", metadata !6, i32 254, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i16*, i32, %struct.node**, i8*)* @encode_stream, null, null, metadata !42} ; [ DW_TAG_subprogram ]
!42 = metadata !{metadata !43}
!43 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!44 = metadata !{i32 720942, i32 0, metadata !6, metadata !"pack_huffman_tree", metadata !"pack_huffman_tree", metadata !"", metadata !6, i32 283, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct.node*, i8**)* @pack_huffman_tree, null, null, metadata !45} ; [ DW_TAG_subprogram ]
!45 = metadata !{metadata !46}
!46 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!47 = metadata !{i32 720942, i32 0, metadata !6, metadata !"pack_tree_iter", metadata !"pack_tree_iter", metadata !"", metadata !6, i32 306, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct.node*, i8**)* @pack_tree_iter, null, null, metadata !48} ; [ DW_TAG_subprogram ]
!48 = metadata !{metadata !49}
!49 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!50 = metadata !{i32 720942, i32 0, metadata !6, metadata !"count_leaves", metadata !"count_leaves", metadata !"", metadata !6, i32 333, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct.node*)* @count_leaves, null, null, metadata !51} ; [ DW_TAG_subprogram ]
!51 = metadata !{metadata !52}
!52 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!53 = metadata !{i32 720942, i32 0, metadata !6, metadata !"free_tree_nodes", metadata !"free_tree_nodes", metadata !"", metadata !6, i32 340, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct.node*)* @free_tree_nodes, null, null, metadata !54} ; [ DW_TAG_subprogram ]
!54 = metadata !{metadata !55}
!55 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!56 = metadata !{i32 720942, i32 0, metadata !6, metadata !"print_list_nodes", metadata !"print_list_nodes", metadata !"", metadata !6, i32 353, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (%struct.node*, i32)* @print_list_nodes, null, null, metadata !57} ; [ DW_TAG_subprogram ]
!57 = metadata !{metadata !58}
!58 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!59 = metadata !{i32 721153, metadata !5, metadata !"symbol_stream", metadata !6, i32 16777281, metadata !60, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!60 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !34} ; [ DW_TAG_pointer_type ]
!61 = metadata !{i32 65, i32 15, metadata !5, null}
!62 = metadata !{i32 721153, metadata !5, metadata !"symbol_stream_length", metadata !6, i32 33554498, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!63 = metadata !{i32 66, i32 7, metadata !5, null}
!64 = metadata !{i32 721153, metadata !5, metadata !"packed_huffman_tree", metadata !6, i32 50331715, metadata !65, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!65 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !66} ; [ DW_TAG_pointer_type ]
!66 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !67} ; [ DW_TAG_pointer_type ]
!67 = metadata !{i32 720918, null, metadata !"Byte", metadata !6, i32 76, i64 0, i64 0, i64 0, i32 0, metadata !68} ; [ DW_TAG_typedef ]
!68 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!69 = metadata !{i32 67, i32 10, metadata !5, null}
!70 = metadata !{i32 721153, metadata !5, metadata !"huffman_tree_size", metadata !6, i32 67108932, metadata !71, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!71 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_pointer_type ]
!72 = metadata !{i32 68, i32 8, metadata !5, null}
!73 = metadata !{i32 721153, metadata !5, metadata !"encoded_stream", metadata !6, i32 83886149, metadata !74, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!74 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !75} ; [ DW_TAG_pointer_type ]
!75 = metadata !{i32 720918, null, metadata !"CodeType", metadata !6, i32 79, i64 0, i64 0, i64 0, i32 0, metadata !67} ; [ DW_TAG_typedef ]
!76 = metadata !{i32 69, i32 13, metadata !5, null}
!77 = metadata !{i32 721152, metadata !78, metadata !"histogram", metadata !6, i32 71, metadata !79, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!78 = metadata !{i32 720907, metadata !5, i32 70, i32 3, metadata !6, i32 78} ; [ DW_TAG_lexical_block ]
!79 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 2097152, i64 32, i32 0, i32 0, metadata !80, metadata !92, i32 0, i32 0} ; [ DW_TAG_array_type ]
!80 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !81} ; [ DW_TAG_pointer_type ]
!81 = metadata !{i32 720915, null, metadata !"node", metadata !6, i32 39, i64 192, i64 32, i32 0, i32 0, null, metadata !82, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!82 = metadata !{metadata !83, metadata !85, metadata !86, metadata !87, metadata !88, metadata !89}
!83 = metadata !{i32 720909, metadata !81, metadata !"frequency", metadata !6, i32 41, i64 32, i64 32, i64 0, i32 0, metadata !84} ; [ DW_TAG_member ]
!84 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!85 = metadata !{i32 720909, metadata !81, metadata !"next", metadata !6, i32 42, i64 32, i64 32, i64 32, i32 0, metadata !80} ; [ DW_TAG_member ]
!86 = metadata !{i32 720909, metadata !81, metadata !"zero_child", metadata !6, i32 43, i64 32, i64 32, i64 64, i32 0, metadata !80} ; [ DW_TAG_member ]
!87 = metadata !{i32 720909, metadata !81, metadata !"one_child", metadata !6, i32 43, i64 32, i64 32, i64 96, i32 0, metadata !80} ; [ DW_TAG_member ]
!88 = metadata !{i32 720909, metadata !81, metadata !"symbol", metadata !6, i32 44, i64 16, i64 16, i64 128, i32 0, metadata !34} ; [ DW_TAG_member ]
!89 = metadata !{i32 720909, metadata !81, metadata !"code", metadata !6, i32 45, i64 32, i64 32, i64 160, i32 0, metadata !90} ; [ DW_TAG_member ]
!90 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !91} ; [ DW_TAG_pointer_type ]
!91 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!92 = metadata !{metadata !93}
!93 = metadata !{i32 720929, i64 0, i64 65535}    ; [ DW_TAG_subrange_type ]
!94 = metadata !{i32 71, i32 16, metadata !78, null}
!95 = metadata !{i32 721152, metadata !78, metadata !"huffman_tree", metadata !6, i32 72, metadata !80, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!96 = metadata !{i32 72, i32 16, metadata !78, null}
!97 = metadata !{i32 721152, metadata !78, metadata !"encoded_stream_length", metadata !6, i32 73, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!98 = metadata !{i32 73, i32 7, metadata !78, null}
!99 = metadata !{i32 721152, metadata !78, metadata !"i", metadata !6, i32 74, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!100 = metadata !{i32 74, i32 16, metadata !78, null}
!101 = metadata !{i32 77, i32 8, metadata !102, null}
!102 = metadata !{i32 720907, metadata !78, i32 77, i32 3, metadata !6, i32 79} ; [ DW_TAG_lexical_block ]
!103 = metadata !{i32 77, i32 27, metadata !102, null}
!104 = metadata !{i32 77, i32 22, metadata !102, null}
!105 = metadata !{i32 79, i32 3, metadata !78, null}
!106 = metadata !{i32 80, i32 3, metadata !78, null}
!107 = metadata !{i32 82, i32 5, metadata !78, null}
!108 = metadata !{i32 83, i32 24, metadata !78, null}
!109 = metadata !{i32 84, i32 3, metadata !78, null}
!110 = metadata !{i32 85, i32 3, metadata !78, null}
!111 = metadata !{i32 721153, metadata !12, metadata !"symbol_stream", metadata !6, i32 16777305, metadata !60, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!112 = metadata !{i32 89, i32 24, metadata !12, null}
!113 = metadata !{i32 721153, metadata !12, metadata !"symbol_stream_length", metadata !6, i32 33554522, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!114 = metadata !{i32 90, i32 16, metadata !12, null}
!115 = metadata !{i32 721153, metadata !12, metadata !"histogram", metadata !6, i32 50331739, metadata !116, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!116 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !80} ; [ DW_TAG_pointer_type ]
!117 = metadata !{i32 91, i32 25, metadata !12, null}
!118 = metadata !{i32 721152, metadata !119, metadata !"i", metadata !6, i32 93, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!119 = metadata !{i32 720907, metadata !12, i32 92, i32 3, metadata !6, i32 80} ; [ DW_TAG_lexical_block ]
!120 = metadata !{i32 93, i32 16, metadata !119, null}
!121 = metadata !{i32 721152, metadata !119, metadata !"the_leaf", metadata !6, i32 94, metadata !80, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!122 = metadata !{i32 94, i32 25, metadata !119, null}
!123 = metadata !{i32 96, i32 8, metadata !124, null}
!124 = metadata !{i32 720907, metadata !119, i32 96, i32 3, metadata !6, i32 81} ; [ DW_TAG_lexical_block ]
!125 = metadata !{i32 98, i32 7, metadata !126, null}
!126 = metadata !{i32 720907, metadata !124, i32 97, i32 7, metadata !6, i32 82} ; [ DW_TAG_lexical_block ]
!127 = metadata !{i32 99, i32 7, metadata !126, null}
!128 = metadata !{i32 102, i32 22, metadata !129, null}
!129 = metadata !{i32 720907, metadata !126, i32 100, i32 4, metadata !6, i32 83} ; [ DW_TAG_lexical_block ]
!130 = metadata !{i32 103, i32 4, metadata !129, null}
!131 = metadata !{i32 104, i32 4, metadata !129, null}
!132 = metadata !{i32 105, i32 4, metadata !129, null}
!133 = metadata !{i32 106, i32 4, metadata !129, null}
!134 = metadata !{i32 108, i32 4, metadata !126, null}
!135 = metadata !{i32 109, i32 7, metadata !126, null}
!136 = metadata !{i32 96, i32 37, metadata !124, null}
!137 = metadata !{i32 110, i32 3, metadata !119, null}
!138 = metadata !{i32 721153, metadata !15, metadata !"histogram", metadata !6, i32 16777329, metadata !116, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!139 = metadata !{i32 113, i32 25, metadata !15, null}
!140 = metadata !{i32 721153, metadata !15, metadata !"tree_ptr", metadata !6, i32 33554546, metadata !116, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!141 = metadata !{i32 114, i32 17, metadata !15, null}
!142 = metadata !{i32 721152, metadata !143, metadata !"tree", metadata !6, i32 116, metadata !80, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!143 = metadata !{i32 720907, metadata !15, i32 115, i32 3, metadata !6, i32 84} ; [ DW_TAG_lexical_block ]
!144 = metadata !{i32 116, i32 16, metadata !143, null}
!145 = metadata !{i32 116, i32 34, metadata !143, null}
!146 = metadata !{i32 721152, metadata !143, metadata !"new_node", metadata !6, i32 117, metadata !80, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!147 = metadata !{i32 117, i32 25, metadata !143, null}
!148 = metadata !{i32 721152, metadata !143, metadata !"temp_code", metadata !6, i32 118, metadata !149, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!149 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 524288, i64 8, i32 0, i32 0, metadata !91, metadata !92, i32 0, i32 0} ; [ DW_TAG_array_type ]
!150 = metadata !{i32 118, i32 8, metadata !143, null}
!151 = metadata !{i32 721152, metadata !143, metadata !"i", metadata !6, i32 119, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!152 = metadata !{i32 119, i32 16, metadata !143, null}
!153 = metadata !{i32 122, i32 8, metadata !154, null}
!154 = metadata !{i32 720907, metadata !143, i32 122, i32 3, metadata !6, i32 85} ; [ DW_TAG_lexical_block ]
!155 = metadata !{i32 123, i32 5, metadata !154, null}
!156 = metadata !{i32 124, i32 2, metadata !154, null}
!157 = metadata !{i32 122, i32 23, metadata !154, null}
!158 = metadata !{i32 129, i32 3, metadata !143, null}
!159 = metadata !{i32 131, i32 34, metadata !160, null}
!160 = metadata !{i32 720907, metadata !143, i32 130, i32 7, metadata !6, i32 86} ; [ DW_TAG_lexical_block ]
!161 = metadata !{i32 132, i32 7, metadata !160, null}
!162 = metadata !{i32 133, i32 7, metadata !160, null}
!163 = metadata !{i32 134, i32 7, metadata !160, null}
!164 = metadata !{i32 136, i32 7, metadata !160, null}
!165 = metadata !{i32 137, i32 7, metadata !160, null}
!166 = metadata !{i32 138, i32 7, metadata !160, null}
!167 = metadata !{i32 140, i32 3, metadata !143, null}
!168 = metadata !{i32 141, i32 3, metadata !143, null}
!169 = metadata !{i32 142, i32 3, metadata !143, null}
!170 = metadata !{i32 721153, metadata !18, metadata !"the_node", metadata !6, i32 16777361, metadata !80, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!171 = metadata !{i32 145, i32 25, metadata !18, null}
!172 = metadata !{i32 721153, metadata !18, metadata !"list", metadata !6, i32 33554577, metadata !116, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!173 = metadata !{i32 145, i32 37, metadata !18, null}
!174 = metadata !{i32 721152, metadata !175, metadata !"the_list", metadata !6, i32 147, metadata !80, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!175 = metadata !{i32 720907, metadata !18, i32 146, i32 3, metadata !6, i32 87} ; [ DW_TAG_lexical_block ]
!176 = metadata !{i32 147, i32 25, metadata !175, null}
!177 = metadata !{i32 149, i32 3, metadata !175, null}
!178 = metadata !{i32 151, i32 3, metadata !175, null}
!179 = metadata !{i32 153, i32 7, metadata !180, null}
!180 = metadata !{i32 720907, metadata !175, i32 152, i32 7, metadata !6, i32 88} ; [ DW_TAG_lexical_block ]
!181 = metadata !{i32 154, i32 7, metadata !180, null}
!182 = metadata !{i32 155, i32 7, metadata !180, null}
!183 = metadata !{i32 158, i32 7, metadata !184, null}
!184 = metadata !{i32 720907, metadata !175, i32 157, i32 7, metadata !6, i32 89} ; [ DW_TAG_lexical_block ]
!185 = metadata !{i32 160, i32 2, metadata !184, null}
!186 = metadata !{i32 161, i32 7, metadata !184, null}
!187 = metadata !{i32 162, i32 7, metadata !184, null}
!188 = metadata !{i32 164, i32 3, metadata !175, null}
!189 = metadata !{i32 721153, metadata !21, metadata !"tree", metadata !6, i32 16777384, metadata !80, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!190 = metadata !{i32 168, i32 25, metadata !21, null}
!191 = metadata !{i32 721153, metadata !21, metadata !"code_string", metadata !6, i32 33554601, metadata !90, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!192 = metadata !{i32 169, i32 17, metadata !21, null}
!193 = metadata !{i32 721153, metadata !21, metadata !"code_string_pos", metadata !6, i32 50331818, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!194 = metadata !{i32 170, i32 16, metadata !21, null}
!195 = metadata !{i32 173, i32 3, metadata !196, null}
!196 = metadata !{i32 720907, metadata !21, i32 171, i32 3, metadata !6, i32 90} ; [ DW_TAG_lexical_block ]
!197 = metadata !{i32 175, i32 7, metadata !198, null}
!198 = metadata !{i32 720907, metadata !196, i32 174, i32 7, metadata !6, i32 91} ; [ DW_TAG_lexical_block ]
!199 = metadata !{i32 176, i32 27, metadata !198, null}
!200 = metadata !{i32 177, i32 7, metadata !198, null}
!201 = metadata !{i32 180, i32 7, metadata !202, null}
!202 = metadata !{i32 720907, metadata !196, i32 179, i32 7, metadata !6, i32 92} ; [ DW_TAG_lexical_block ]
!203 = metadata !{i32 181, i32 7, metadata !202, null}
!204 = metadata !{i32 182, i32 7, metadata !202, null}
!205 = metadata !{i32 183, i32 7, metadata !202, null}
!206 = metadata !{i32 185, i32 3, metadata !196, null}
!207 = metadata !{i32 721153, metadata !24, metadata !"file", metadata !6, i32 16777404, metadata !208, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!208 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !209} ; [ DW_TAG_pointer_type ]
!209 = metadata !{i32 720918, null, metadata !"FILE", metadata !6, i32 49, i64 0, i64 0, i64 0, i32 0, metadata !210} ; [ DW_TAG_typedef ]
!210 = metadata !{i32 720915, null, metadata !"_IO_FILE", metadata !211, i32 271, i64 1216, i64 64, i32 0, i32 0, null, metadata !212, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!211 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!212 = metadata !{metadata !213, metadata !214, metadata !215, metadata !216, metadata !217, metadata !218, metadata !219, metadata !220, metadata !221, metadata !222, metadata !223, metadata !224, metadata !225, metadata !233, metadata !234, metadata !235, metadata !236, metadata !239, metadata !240, metadata !242, metadata !246, metadata !248, metadata !252, metadata !253, metadata !254, metadata !255, metadata !256, metadata !258, metadata !259}
!213 = metadata !{i32 720909, metadata !210, metadata !"_flags", metadata !211, i32 272, i64 32, i64 32, i64 0, i32 0, metadata !9} ; [ DW_TAG_member ]
!214 = metadata !{i32 720909, metadata !210, metadata !"_IO_read_ptr", metadata !211, i32 277, i64 32, i64 32, i64 32, i32 0, metadata !90} ; [ DW_TAG_member ]
!215 = metadata !{i32 720909, metadata !210, metadata !"_IO_read_end", metadata !211, i32 278, i64 32, i64 32, i64 64, i32 0, metadata !90} ; [ DW_TAG_member ]
!216 = metadata !{i32 720909, metadata !210, metadata !"_IO_read_base", metadata !211, i32 279, i64 32, i64 32, i64 96, i32 0, metadata !90} ; [ DW_TAG_member ]
!217 = metadata !{i32 720909, metadata !210, metadata !"_IO_write_base", metadata !211, i32 280, i64 32, i64 32, i64 128, i32 0, metadata !90} ; [ DW_TAG_member ]
!218 = metadata !{i32 720909, metadata !210, metadata !"_IO_write_ptr", metadata !211, i32 281, i64 32, i64 32, i64 160, i32 0, metadata !90} ; [ DW_TAG_member ]
!219 = metadata !{i32 720909, metadata !210, metadata !"_IO_write_end", metadata !211, i32 282, i64 32, i64 32, i64 192, i32 0, metadata !90} ; [ DW_TAG_member ]
!220 = metadata !{i32 720909, metadata !210, metadata !"_IO_buf_base", metadata !211, i32 283, i64 32, i64 32, i64 224, i32 0, metadata !90} ; [ DW_TAG_member ]
!221 = metadata !{i32 720909, metadata !210, metadata !"_IO_buf_end", metadata !211, i32 284, i64 32, i64 32, i64 256, i32 0, metadata !90} ; [ DW_TAG_member ]
!222 = metadata !{i32 720909, metadata !210, metadata !"_IO_save_base", metadata !211, i32 286, i64 32, i64 32, i64 288, i32 0, metadata !90} ; [ DW_TAG_member ]
!223 = metadata !{i32 720909, metadata !210, metadata !"_IO_backup_base", metadata !211, i32 287, i64 32, i64 32, i64 320, i32 0, metadata !90} ; [ DW_TAG_member ]
!224 = metadata !{i32 720909, metadata !210, metadata !"_IO_save_end", metadata !211, i32 288, i64 32, i64 32, i64 352, i32 0, metadata !90} ; [ DW_TAG_member ]
!225 = metadata !{i32 720909, metadata !210, metadata !"_markers", metadata !211, i32 290, i64 32, i64 32, i64 384, i32 0, metadata !226} ; [ DW_TAG_member ]
!226 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !227} ; [ DW_TAG_pointer_type ]
!227 = metadata !{i32 720915, null, metadata !"_IO_marker", metadata !211, i32 186, i64 96, i64 32, i32 0, i32 0, null, metadata !228, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!228 = metadata !{metadata !229, metadata !230, metadata !232}
!229 = metadata !{i32 720909, metadata !227, metadata !"_next", metadata !211, i32 187, i64 32, i64 32, i64 0, i32 0, metadata !226} ; [ DW_TAG_member ]
!230 = metadata !{i32 720909, metadata !227, metadata !"_sbuf", metadata !211, i32 188, i64 32, i64 32, i64 32, i32 0, metadata !231} ; [ DW_TAG_member ]
!231 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !210} ; [ DW_TAG_pointer_type ]
!232 = metadata !{i32 720909, metadata !227, metadata !"_pos", metadata !211, i32 192, i64 32, i64 32, i64 64, i32 0, metadata !9} ; [ DW_TAG_member ]
!233 = metadata !{i32 720909, metadata !210, metadata !"_chain", metadata !211, i32 292, i64 32, i64 32, i64 416, i32 0, metadata !231} ; [ DW_TAG_member ]
!234 = metadata !{i32 720909, metadata !210, metadata !"_fileno", metadata !211, i32 294, i64 32, i64 32, i64 448, i32 0, metadata !9} ; [ DW_TAG_member ]
!235 = metadata !{i32 720909, metadata !210, metadata !"_flags2", metadata !211, i32 298, i64 32, i64 32, i64 480, i32 0, metadata !9} ; [ DW_TAG_member ]
!236 = metadata !{i32 720909, metadata !210, metadata !"_old_offset", metadata !211, i32 300, i64 32, i64 32, i64 512, i32 0, metadata !237} ; [ DW_TAG_member ]
!237 = metadata !{i32 720918, null, metadata !"__off_t", metadata !211, i32 141, i64 0, i64 0, i64 0, i32 0, metadata !238} ; [ DW_TAG_typedef ]
!238 = metadata !{i32 720932, null, metadata !"long int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!239 = metadata !{i32 720909, metadata !210, metadata !"_cur_column", metadata !211, i32 304, i64 16, i64 16, i64 544, i32 0, metadata !35} ; [ DW_TAG_member ]
!240 = metadata !{i32 720909, metadata !210, metadata !"_vtable_offset", metadata !211, i32 305, i64 8, i64 8, i64 560, i32 0, metadata !241} ; [ DW_TAG_member ]
!241 = metadata !{i32 720932, null, metadata !"signed char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!242 = metadata !{i32 720909, metadata !210, metadata !"_shortbuf", metadata !211, i32 306, i64 8, i64 8, i64 568, i32 0, metadata !243} ; [ DW_TAG_member ]
!243 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8, i64 8, i32 0, i32 0, metadata !91, metadata !244, i32 0, i32 0} ; [ DW_TAG_array_type ]
!244 = metadata !{metadata !245}
!245 = metadata !{i32 720929, i64 0, i64 0}       ; [ DW_TAG_subrange_type ]
!246 = metadata !{i32 720909, metadata !210, metadata !"_lock", metadata !211, i32 310, i64 32, i64 32, i64 576, i32 0, metadata !247} ; [ DW_TAG_member ]
!247 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, null} ; [ DW_TAG_pointer_type ]
!248 = metadata !{i32 720909, metadata !210, metadata !"_offset", metadata !211, i32 319, i64 64, i64 64, i64 640, i32 0, metadata !249} ; [ DW_TAG_member ]
!249 = metadata !{i32 720918, null, metadata !"__off64_t", metadata !211, i32 142, i64 0, i64 0, i64 0, i32 0, metadata !250} ; [ DW_TAG_typedef ]
!250 = metadata !{i32 720918, null, metadata !"__quad_t", metadata !211, i32 56, i64 0, i64 0, i64 0, i32 0, metadata !251} ; [ DW_TAG_typedef ]
!251 = metadata !{i32 720932, null, metadata !"long long int", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!252 = metadata !{i32 720909, metadata !210, metadata !"__pad1", metadata !211, i32 328, i64 32, i64 32, i64 704, i32 0, metadata !247} ; [ DW_TAG_member ]
!253 = metadata !{i32 720909, metadata !210, metadata !"__pad2", metadata !211, i32 329, i64 32, i64 32, i64 736, i32 0, metadata !247} ; [ DW_TAG_member ]
!254 = metadata !{i32 720909, metadata !210, metadata !"__pad3", metadata !211, i32 330, i64 32, i64 32, i64 768, i32 0, metadata !247} ; [ DW_TAG_member ]
!255 = metadata !{i32 720909, metadata !210, metadata !"__pad4", metadata !211, i32 331, i64 32, i64 32, i64 800, i32 0, metadata !247} ; [ DW_TAG_member ]
!256 = metadata !{i32 720909, metadata !210, metadata !"__pad5", metadata !211, i32 332, i64 32, i64 32, i64 832, i32 0, metadata !257} ; [ DW_TAG_member ]
!257 = metadata !{i32 720918, null, metadata !"size_t", metadata !211, i32 211, i64 0, i64 0, i64 0, i32 0, metadata !84} ; [ DW_TAG_typedef ]
!258 = metadata !{i32 720909, metadata !210, metadata !"_mode", metadata !211, i32 334, i64 32, i64 32, i64 864, i32 0, metadata !9} ; [ DW_TAG_member ]
!259 = metadata !{i32 720909, metadata !210, metadata !"_unused2", metadata !211, i32 336, i64 320, i64 8, i64 896, i32 0, metadata !260} ; [ DW_TAG_member ]
!260 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !91, metadata !261, i32 0, i32 0} ; [ DW_TAG_array_type ]
!261 = metadata !{metadata !262}
!262 = metadata !{i32 720929, i64 0, i64 39}      ; [ DW_TAG_subrange_type ]
!263 = metadata !{i32 188, i32 9, metadata !24, null}
!264 = metadata !{i32 721152, metadata !265, metadata !"the_node", metadata !6, i32 190, metadata !27, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!265 = metadata !{i32 720907, metadata !24, i32 189, i32 3, metadata !6, i32 93} ; [ DW_TAG_lexical_block ]
!266 = metadata !{i32 190, i32 30, metadata !265, null}
!267 = metadata !{i32 721152, metadata !265, metadata !"the_byte", metadata !6, i32 191, metadata !67, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!268 = metadata !{i32 191, i32 8, metadata !265, null}
!269 = metadata !{i32 721152, metadata !265, metadata !"the_symbol", metadata !6, i32 192, metadata !34, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!270 = metadata !{i32 192, i32 14, metadata !265, null}
!271 = metadata !{i32 721152, metadata !265, metadata !"exp", metadata !6, i32 192, metadata !34, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!272 = metadata !{i32 192, i32 26, metadata !265, null}
!273 = metadata !{i32 721152, metadata !265, metadata !"i", metadata !6, i32 193, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!274 = metadata !{i32 193, i32 7, metadata !265, null}
!275 = metadata !{i32 195, i32 35, metadata !265, null}
!276 = metadata !{i32 197, i32 3, metadata !265, null}
!277 = metadata !{i32 197, i32 31, metadata !265, null}
!278 = metadata !{i32 199, i32 3, metadata !265, null}
!279 = metadata !{i32 201, i32 30, metadata !280, null}
!280 = metadata !{i32 720907, metadata !265, i32 200, i32 7, metadata !6, i32 94} ; [ DW_TAG_lexical_block ]
!281 = metadata !{i32 202, i32 29, metadata !280, null}
!282 = metadata !{i32 203, i32 7, metadata !280, null}
!283 = metadata !{i32 204, i32 8, metadata !265, null}
!284 = metadata !{i32 206, i32 7, metadata !285, null}
!285 = metadata !{i32 720907, metadata !265, i32 205, i32 7, metadata !6, i32 95} ; [ DW_TAG_lexical_block ]
!286 = metadata !{i32 207, i32 7, metadata !285, null}
!287 = metadata !{i32 208, i32 12, metadata !288, null}
!288 = metadata !{i32 720907, metadata !285, i32 208, i32 7, metadata !6, i32 96} ; [ DW_TAG_lexical_block ]
!289 = metadata !{i32 210, i32 4, metadata !290, null}
!290 = metadata !{i32 720907, metadata !288, i32 209, i32 4, metadata !6, i32 97} ; [ DW_TAG_lexical_block ]
!291 = metadata !{i32 210, i32 32, metadata !290, null}
!292 = metadata !{i32 211, i32 4, metadata !290, null}
!293 = metadata !{i32 212, i32 4, metadata !290, null}
!294 = metadata !{i32 208, i32 46, metadata !288, null}
!295 = metadata !{i32 213, i32 7, metadata !285, null}
!296 = metadata !{i32 215, i32 7, metadata !285, null}
!297 = metadata !{i32 217, i32 9, metadata !298, null}
!298 = metadata !{i32 720907, metadata !265, i32 217, i32 7, metadata !6, i32 98} ; [ DW_TAG_lexical_block ]
!299 = metadata !{i32 217, i32 50, metadata !298, null}
!300 = metadata !{i32 218, i32 3, metadata !265, null}
!301 = metadata !{i32 721153, metadata !38, metadata !"file", metadata !6, i32 16777438, metadata !208, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!302 = metadata !{i32 222, i32 9, metadata !38, null}
!303 = metadata !{i32 721153, metadata !38, metadata !"tree", metadata !6, i32 33554655, metadata !27, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!304 = metadata !{i32 223, i32 21, metadata !38, null}
!305 = metadata !{i32 721153, metadata !38, metadata !"symbol_stream", metadata !6, i32 50331872, metadata !60, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!306 = metadata !{i32 224, i32 15, metadata !38, null}
!307 = metadata !{i32 721153, metadata !38, metadata !"count", metadata !6, i32 67109089, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!308 = metadata !{i32 225, i32 7, metadata !38, null}
!309 = metadata !{i32 721152, metadata !310, metadata !"the_byte", metadata !6, i32 227, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!310 = metadata !{i32 720907, metadata !38, i32 226, i32 3, metadata !6, i32 99} ; [ DW_TAG_lexical_block ]
!311 = metadata !{i32 227, i32 12, metadata !310, null}
!312 = metadata !{i32 721152, metadata !310, metadata !"bit_mask", metadata !6, i32 228, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!313 = metadata !{i32 228, i32 21, metadata !310, null}
!314 = metadata !{i32 228, i32 33, metadata !310, null}
!315 = metadata !{i32 721152, metadata !310, metadata !"the_node", metadata !6, i32 229, metadata !27, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!316 = metadata !{i32 229, i32 30, metadata !310, null}
!317 = metadata !{i32 721152, metadata !310, metadata !"i", metadata !6, i32 230, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!318 = metadata !{i32 230, i32 16, metadata !310, null}
!319 = metadata !{i32 232, i32 8, metadata !320, null}
!320 = metadata !{i32 720907, metadata !310, i32 232, i32 3, metadata !6, i32 100} ; [ DW_TAG_lexical_block ]
!321 = metadata !{i32 234, i32 7, metadata !322, null}
!322 = metadata !{i32 720907, metadata !320, i32 233, i32 7, metadata !6, i32 101} ; [ DW_TAG_lexical_block ]
!323 = metadata !{i32 235, i32 7, metadata !322, null}
!324 = metadata !{i32 237, i32 4, metadata !325, null}
!325 = metadata !{i32 720907, metadata !322, i32 236, i32 4, metadata !6, i32 102} ; [ DW_TAG_lexical_block ]
!326 = metadata !{i32 237, i32 25, metadata !327, null}
!327 = metadata !{i32 720907, metadata !325, i32 237, i32 23, metadata !6, i32 103} ; [ DW_TAG_lexical_block ]
!328 = metadata !{i32 237, i32 53, metadata !327, null}
!329 = metadata !{i32 237, i32 73, metadata !327, null}
!330 = metadata !{i32 237, i32 87, metadata !327, null}
!331 = metadata !{i32 238, i32 4, metadata !325, null}
!332 = metadata !{i32 239, i32 6, metadata !325, null}
!333 = metadata !{i32 240, i32 9, metadata !325, null}
!334 = metadata !{i32 241, i32 4, metadata !325, null}
!335 = metadata !{i32 242, i32 4, metadata !325, null}
!336 = metadata !{i32 243, i32 7, metadata !322, null}
!337 = metadata !{i32 244, i32 7, metadata !322, null}
!338 = metadata !{i32 232, i32 22, metadata !320, null}
!339 = metadata !{i32 245, i32 3, metadata !310, null}
!340 = metadata !{i32 721153, metadata !41, metadata !"symbol_stream", metadata !6, i32 16777466, metadata !60, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!341 = metadata !{i32 250, i32 15, metadata !41, null}
!342 = metadata !{i32 721153, metadata !41, metadata !"symbol_stream_length", metadata !6, i32 33554683, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!343 = metadata !{i32 251, i32 7, metadata !41, null}
!344 = metadata !{i32 721153, metadata !41, metadata !"histogram", metadata !6, i32 50331900, metadata !116, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!345 = metadata !{i32 252, i32 16, metadata !41, null}
!346 = metadata !{i32 721153, metadata !41, metadata !"encoded_stream", metadata !6, i32 67109117, metadata !74, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!347 = metadata !{i32 253, i32 13, metadata !41, null}
!348 = metadata !{i32 721152, metadata !349, metadata !"i", metadata !6, i32 255, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!349 = metadata !{i32 720907, metadata !41, i32 254, i32 3, metadata !6, i32 104} ; [ DW_TAG_lexical_block ]
!350 = metadata !{i32 255, i32 7, metadata !349, null}
!351 = metadata !{i32 721152, metadata !349, metadata !"the_node", metadata !6, i32 256, metadata !80, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!352 = metadata !{i32 256, i32 16, metadata !349, null}
!353 = metadata !{i32 721152, metadata !349, metadata !"the_code_string", metadata !6, i32 257, metadata !90, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!354 = metadata !{i32 257, i32 18, metadata !349, null}
!355 = metadata !{i32 721152, metadata !349, metadata !"code_pointer", metadata !6, i32 258, metadata !74, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!356 = metadata !{i32 258, i32 22, metadata !349, null}
!357 = metadata !{i32 258, i32 51, metadata !349, null}
!358 = metadata !{i32 721152, metadata !349, metadata !"bit_mask", metadata !6, i32 259, metadata !75, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!359 = metadata !{i32 259, i32 21, metadata !349, null}
!360 = metadata !{i32 259, i32 33, metadata !349, null}
!361 = metadata !{i32 261, i32 3, metadata !349, null}
!362 = metadata !{i32 262, i32 8, metadata !363, null}
!363 = metadata !{i32 720907, metadata !349, i32 262, i32 3, metadata !6, i32 105} ; [ DW_TAG_lexical_block ]
!364 = metadata !{i32 264, i32 7, metadata !365, null}
!365 = metadata !{i32 720907, metadata !363, i32 263, i32 7, metadata !6, i32 106} ; [ DW_TAG_lexical_block ]
!366 = metadata !{i32 265, i32 7, metadata !365, null}
!367 = metadata !{i32 266, i32 6, metadata !368, null}
!368 = metadata !{i32 720907, metadata !365, i32 266, i32 4, metadata !6, i32 107} ; [ DW_TAG_lexical_block ]
!369 = metadata !{i32 266, i32 58, metadata !368, null}
!370 = metadata !{i32 267, i32 7, metadata !365, null}
!371 = metadata !{i32 268, i32 7, metadata !365, null}
!372 = metadata !{i32 270, i32 4, metadata !373, null}
!373 = metadata !{i32 720907, metadata !365, i32 269, i32 4, metadata !6, i32 108} ; [ DW_TAG_lexical_block ]
!374 = metadata !{i32 270, i32 33, metadata !373, null}
!375 = metadata !{i32 271, i32 4, metadata !373, null}
!376 = metadata !{i32 272, i32 4, metadata !373, null}
!377 = metadata !{i32 273, i32 4, metadata !373, null}
!378 = metadata !{i32 273, i32 25, metadata !379, null}
!379 = metadata !{i32 720907, metadata !373, i32 273, i32 23, metadata !6, i32 109} ; [ DW_TAG_lexical_block ]
!380 = metadata !{i32 273, i32 39, metadata !379, null}
!381 = metadata !{i32 273, i32 55, metadata !379, null}
!382 = metadata !{i32 273, i32 74, metadata !379, null}
!383 = metadata !{i32 274, i32 4, metadata !373, null}
!384 = metadata !{i32 275, i32 7, metadata !365, null}
!385 = metadata !{i32 262, i32 37, metadata !363, null}
!386 = metadata !{i32 276, i32 3, metadata !349, null}
!387 = metadata !{i32 721153, metadata !44, metadata !"tree", metadata !6, i32 16777497, metadata !80, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!388 = metadata !{i32 281, i32 16, metadata !44, null}
!389 = metadata !{i32 721153, metadata !44, metadata !"packed_tree", metadata !6, i32 33554714, metadata !65, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!390 = metadata !{i32 282, i32 10, metadata !44, null}
!391 = metadata !{i32 721152, metadata !392, metadata !"packed_tree_ptr", metadata !6, i32 284, metadata !66, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!392 = metadata !{i32 720907, metadata !44, i32 283, i32 3, metadata !6, i32 110} ; [ DW_TAG_lexical_block ]
!393 = metadata !{i32 284, i32 9, metadata !392, null}
!394 = metadata !{i32 721152, metadata !392, metadata !"num_leaves", metadata !6, i32 285, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!395 = metadata !{i32 285, i32 7, metadata !392, null}
!396 = metadata !{i32 287, i32 16, metadata !392, null}
!397 = metadata !{i32 290, i32 14, metadata !392, null}
!398 = metadata !{i32 291, i32 3, metadata !392, null}
!399 = metadata !{i32 292, i32 3, metadata !392, null}
!400 = metadata !{i32 721153, metadata !47, metadata !"tree", metadata !6, i32 16777520, metadata !80, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!401 = metadata !{i32 304, i32 16, metadata !47, null}
!402 = metadata !{i32 721153, metadata !47, metadata !"packed_tree_ptr", metadata !6, i32 33554737, metadata !65, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!403 = metadata !{i32 305, i32 10, metadata !47, null}
!404 = metadata !{i32 721152, metadata !405, metadata !"the_symbol", metadata !6, i32 307, metadata !34, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!405 = metadata !{i32 720907, metadata !47, i32 306, i32 3, metadata !6, i32 111} ; [ DW_TAG_lexical_block ]
!406 = metadata !{i32 307, i32 14, metadata !405, null}
!407 = metadata !{i32 721152, metadata !405, metadata !"i", metadata !6, i32 308, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!408 = metadata !{i32 308, i32 7, metadata !405, null}
!409 = metadata !{i32 310, i32 3, metadata !405, null}
!410 = metadata !{i32 312, i32 7, metadata !411, null}
!411 = metadata !{i32 720907, metadata !405, i32 311, i32 7, metadata !6, i32 112} ; [ DW_TAG_lexical_block ]
!412 = metadata !{i32 313, i32 7, metadata !411, null}
!413 = metadata !{i32 314, i32 7, metadata !411, null}
!414 = metadata !{i32 315, i32 12, metadata !415, null}
!415 = metadata !{i32 720907, metadata !411, i32 315, i32 7, metadata !6, i32 113} ; [ DW_TAG_lexical_block ]
!416 = metadata !{i32 317, i32 4, metadata !417, null}
!417 = metadata !{i32 720907, metadata !415, i32 316, i32 4, metadata !6, i32 114} ; [ DW_TAG_lexical_block ]
!418 = metadata !{i32 318, i32 4, metadata !417, null}
!419 = metadata !{i32 319, i32 4, metadata !417, null}
!420 = metadata !{i32 320, i32 4, metadata !417, null}
!421 = metadata !{i32 315, i32 39, metadata !415, null}
!422 = metadata !{i32 321, i32 7, metadata !411, null}
!423 = metadata !{i32 324, i32 7, metadata !424, null}
!424 = metadata !{i32 720907, metadata !405, i32 323, i32 7, metadata !6, i32 115} ; [ DW_TAG_lexical_block ]
!425 = metadata !{i32 325, i32 7, metadata !424, null}
!426 = metadata !{i32 326, i32 7, metadata !424, null}
!427 = metadata !{i32 327, i32 7, metadata !424, null}
!428 = metadata !{i32 329, i32 3, metadata !405, null}
!429 = metadata !{i32 721153, metadata !50, metadata !"tree", metadata !6, i32 16777548, metadata !80, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!430 = metadata !{i32 332, i32 16, metadata !50, null}
!431 = metadata !{i32 334, i32 3, metadata !432, null}
!432 = metadata !{i32 720907, metadata !50, i32 333, i32 3, metadata !6, i32 116} ; [ DW_TAG_lexical_block ]
!433 = metadata !{i32 334, i32 40, metadata !432, null}
!434 = metadata !{i32 335, i32 16, metadata !432, null}
!435 = metadata !{i32 335, i32 49, metadata !432, null}
!436 = metadata !{i32 336, i32 3, metadata !432, null}
!437 = metadata !{i32 721153, metadata !53, metadata !"tree", metadata !6, i32 16777555, metadata !80, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!438 = metadata !{i32 339, i32 16, metadata !53, null}
!439 = metadata !{i32 341, i32 3, metadata !440, null}
!440 = metadata !{i32 720907, metadata !53, i32 340, i32 3, metadata !6, i32 117} ; [ DW_TAG_lexical_block ]
!441 = metadata !{i32 343, i32 7, metadata !442, null}
!442 = metadata !{i32 720907, metadata !440, i32 342, i32 7, metadata !6, i32 118} ; [ DW_TAG_lexical_block ]
!443 = metadata !{i32 344, i32 7, metadata !442, null}
!444 = metadata !{i32 345, i32 7, metadata !442, null}
!445 = metadata !{i32 346, i32 8, metadata !440, null}
!446 = metadata !{i32 347, i32 3, metadata !440, null}
!447 = metadata !{i32 348, i32 3, metadata !440, null}
!448 = metadata !{i32 721153, metadata !56, metadata !"tree", metadata !6, i32 16777567, metadata !80, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!449 = metadata !{i32 351, i32 16, metadata !56, null}
!450 = metadata !{i32 721153, metadata !56, metadata !"flag", metadata !6, i32 33554784, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!451 = metadata !{i32 352, i32 7, metadata !56, null}
!452 = metadata !{i32 354, i32 3, metadata !453, null}
!453 = metadata !{i32 720907, metadata !56, i32 353, i32 3, metadata !6, i32 119} ; [ DW_TAG_lexical_block ]
!454 = metadata !{i32 354, i32 13, metadata !453, null}
!455 = metadata !{i32 355, i32 3, metadata !453, null}
!456 = metadata !{i32 356, i32 3, metadata !453, null}
!457 = metadata !{i32 357, i32 5, metadata !453, null}
!458 = metadata !{i32 359, i32 5, metadata !453, null}
!459 = metadata !{i32 360, i32 3, metadata !453, null}
