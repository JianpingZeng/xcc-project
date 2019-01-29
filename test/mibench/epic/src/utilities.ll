; ModuleID = 'utilities.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct._IO_FILE = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, %struct._IO_marker*, %struct._IO_FILE*, i32, i32, i32, i16, i8, [1 x i8], i8*, i64, i8*, i8*, i8*, i8*, i32, i32, [40 x i8] }
%struct._IO_marker = type { %struct._IO_marker*, %struct._IO_FILE*, i32 }

@.str = private unnamed_addr constant [47 x i8] c"\0AERROR: unable to allocate sufficient memory!\0A\00", align 1
@.str1 = private unnamed_addr constant [30 x i8] c"\0AError opening file %s (%s).\0A\00", align 1
@temp_byte = common global i8 0, align 1
@temp_short = common global i16 0, align 2
@temp_int = common global i32 0, align 4

define arm_aapcscc i8* @check_malloc(i32 %size) nounwind uwtable {
entry:
  %size.addr = alloca i32, align 4
  %the_block = alloca i8*, align 4
  store i32 %size, i32* %size.addr, align 4
  call void @llvm.dbg.declare(metadata !{i32* %size.addr}, metadata !92), !dbg !93
  call void @llvm.dbg.declare(metadata !{i8** %the_block}, metadata !94), !dbg !96
  %0 = load i32* %size.addr, align 4, !dbg !97
  %call = call arm_aapcscc  noalias i8* @malloc(i32 %0) nounwind, !dbg !97
  store i8* %call, i8** %the_block, align 4, !dbg !97
  %1 = load i8** %the_block, align 4, !dbg !98
  %cmp = icmp eq i8* %1, null, !dbg !98
  br i1 %cmp, label %if.then, label %if.end, !dbg !98

if.then:                                          ; preds = %entry
  %call1 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([47 x i8]* @.str, i32 0, i32 0)), !dbg !99
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !101
  unreachable, !dbg !101

if.end:                                           ; preds = %entry
  %2 = load i8** %the_block, align 4, !dbg !102
  ret i8* %2, !dbg !102
}

declare void @llvm.dbg.declare(metadata, metadata) nounwind readnone

declare arm_aapcscc noalias i8* @malloc(i32) nounwind

declare arm_aapcscc i32 @printf(i8*, ...)

declare arm_aapcscc void @exit(i32) noreturn

define arm_aapcscc i32 @check_free(i8* %ptr) nounwind uwtable {
entry:
  %retval = alloca i32, align 4
  %ptr.addr = alloca i8*, align 4
  store i8* %ptr, i8** %ptr.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %ptr.addr}, metadata !103), !dbg !104
  %0 = load i8** %ptr.addr, align 4, !dbg !105
  %cmp = icmp ne i8* %0, null, !dbg !105
  br i1 %cmp, label %if.then, label %if.end, !dbg !105

if.then:                                          ; preds = %entry
  %1 = load i8** %ptr.addr, align 4, !dbg !107
  call arm_aapcscc  void @free(i8* %1) nounwind, !dbg !107
  br label %if.end, !dbg !107

if.end:                                           ; preds = %if.then, %entry
  %2 = load i32* %retval, !dbg !108
  ret i32 %2, !dbg !108
}

declare arm_aapcscc void @free(i8*) nounwind

define arm_aapcscc %struct._IO_FILE* @check_fopen(i8* %filename, i8* %read_write_flag) nounwind uwtable {
entry:
  %filename.addr = alloca i8*, align 4
  %read_write_flag.addr = alloca i8*, align 4
  %fp = alloca %struct._IO_FILE*, align 4
  store i8* %filename, i8** %filename.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %filename.addr}, metadata !109), !dbg !110
  store i8* %read_write_flag, i8** %read_write_flag.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %read_write_flag.addr}, metadata !111), !dbg !112
  call void @llvm.dbg.declare(metadata !{%struct._IO_FILE** %fp}, metadata !113), !dbg !115
  %0 = load i8** %filename.addr, align 4, !dbg !116
  %1 = load i8** %read_write_flag.addr, align 4, !dbg !116
  %call = call arm_aapcscc  %struct._IO_FILE* @fopen(i8* %0, i8* %1), !dbg !116
  store %struct._IO_FILE* %call, %struct._IO_FILE** %fp, align 4, !dbg !116
  %2 = load %struct._IO_FILE** %fp, align 4, !dbg !117
  %cmp = icmp eq %struct._IO_FILE* %2, null, !dbg !117
  br i1 %cmp, label %if.then, label %if.end, !dbg !117

if.then:                                          ; preds = %entry
  %3 = load i8** %filename.addr, align 4, !dbg !118
  %4 = load i8** %read_write_flag.addr, align 4, !dbg !118
  %call1 = call arm_aapcscc  i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([30 x i8]* @.str1, i32 0, i32 0), i8* %3, i8* %4), !dbg !118
  call arm_aapcscc  void @exit(i32 -1) noreturn, !dbg !120
  unreachable, !dbg !120

if.end:                                           ; preds = %entry
  %5 = load %struct._IO_FILE** %fp, align 4, !dbg !121
  ret %struct._IO_FILE* %5, !dbg !121
}

declare arm_aapcscc %struct._IO_FILE* @fopen(i8*, i8*)

define arm_aapcscc i8* @concatenate(i8* %string1, i8* %string2) nounwind uwtable {
entry:
  %string1.addr = alloca i8*, align 4
  %string2.addr = alloca i8*, align 4
  %new_string = alloca i8*, align 4
  store i8* %string1, i8** %string1.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %string1.addr}, metadata !122), !dbg !123
  store i8* %string2, i8** %string2.addr, align 4
  call void @llvm.dbg.declare(metadata !{i8** %string2.addr}, metadata !124), !dbg !125
  call void @llvm.dbg.declare(metadata !{i8** %new_string}, metadata !126), !dbg !128
  %0 = load i8** %string1.addr, align 4, !dbg !129
  %call = call arm_aapcscc  i32 @strlen(i8* %0) nounwind readonly, !dbg !129
  %1 = load i8** %string2.addr, align 4, !dbg !130
  %call1 = call arm_aapcscc  i32 @strlen(i8* %1) nounwind readonly, !dbg !130
  %add = add i32 %call, %call1, !dbg !130
  %add2 = add i32 %add, 1, !dbg !130
  %call3 = call arm_aapcscc  i8* @check_malloc(i32 %add2), !dbg !130
  store i8* %call3, i8** %new_string, align 4, !dbg !130
  %2 = load i8** %new_string, align 4, !dbg !131
  %3 = load i8** %string1.addr, align 4, !dbg !131
  %call4 = call arm_aapcscc  i8* @strcpy(i8* %2, i8* %3) nounwind, !dbg !131
  %4 = load i8** %new_string, align 4, !dbg !132
  %5 = load i8** %string2.addr, align 4, !dbg !132
  %call5 = call arm_aapcscc  i8* @strcat(i8* %4, i8* %5) nounwind, !dbg !132
  %6 = load i8** %new_string, align 4, !dbg !133
  ret i8* %6, !dbg !133
}

declare arm_aapcscc i32 @strlen(i8*) nounwind readonly

declare arm_aapcscc i8* @strcpy(i8*, i8*) nounwind

declare arm_aapcscc i8* @strcat(i8*, i8*) nounwind

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !3, metadata !84} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !13, metadata !19, metadata !81}
!5 = metadata !{i32 720942, i32 0, metadata !6, metadata !"check_malloc", metadata !"check_malloc", metadata !"", metadata !6, i32 37, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i8* (i32)* @check_malloc, null, null, metadata !11} ; [ DW_TAG_subprogram ]
!6 = metadata !{i32 720937, metadata !"utilities.c", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !8, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!8 = metadata !{metadata !9}
!9 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !10} ; [ DW_TAG_pointer_type ]
!10 = metadata !{i32 720932, null, metadata !"char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!11 = metadata !{metadata !12}
!12 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!13 = metadata !{i32 720942, i32 0, metadata !6, metadata !"check_free", metadata !"check_free", metadata !"", metadata !6, i32 51, metadata !14, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i32 (i8*)* @check_free, null, null, metadata !17} ; [ DW_TAG_subprogram ]
!14 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !15, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!15 = metadata !{metadata !16}
!16 = metadata !{i32 720932, null, metadata !"int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!17 = metadata !{metadata !18}
!18 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!19 = metadata !{i32 720942, i32 0, metadata !6, metadata !"check_fopen", metadata !"check_fopen", metadata !"", metadata !6, i32 58, metadata !20, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, %struct._IO_FILE* (i8*, i8*)* @check_fopen, null, null, metadata !79} ; [ DW_TAG_subprogram ]
!20 = metadata !{i32 720917, i32 0, metadata !"", i32 0, i32 0, i64 0, i64 0, i64 0, i32 0, null, metadata !21, i32 0, i32 0} ; [ DW_TAG_subroutine_type ]
!21 = metadata !{metadata !22}
!22 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !23} ; [ DW_TAG_pointer_type ]
!23 = metadata !{i32 720918, null, metadata !"FILE", metadata !6, i32 49, i64 0, i64 0, i64 0, i32 0, metadata !24} ; [ DW_TAG_typedef ]
!24 = metadata !{i32 720915, null, metadata !"_IO_FILE", metadata !25, i32 271, i64 1216, i64 64, i32 0, i32 0, null, metadata !26, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!25 = metadata !{i32 720937, metadata !"/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h", metadata !"/home/xlous/Development/experiment/mediabench4/epic/src", null} ; [ DW_TAG_file_type ]
!26 = metadata !{metadata !27, metadata !28, metadata !29, metadata !30, metadata !31, metadata !32, metadata !33, metadata !34, metadata !35, metadata !36, metadata !37, metadata !38, metadata !39, metadata !47, metadata !48, metadata !49, metadata !50, metadata !53, metadata !55, metadata !57, metadata !61, metadata !63, metadata !67, metadata !68, metadata !69, metadata !70, metadata !71, metadata !74, metadata !75}
!27 = metadata !{i32 720909, metadata !24, metadata !"_flags", metadata !25, i32 272, i64 32, i64 32, i64 0, i32 0, metadata !16} ; [ DW_TAG_member ]
!28 = metadata !{i32 720909, metadata !24, metadata !"_IO_read_ptr", metadata !25, i32 277, i64 32, i64 32, i64 32, i32 0, metadata !9} ; [ DW_TAG_member ]
!29 = metadata !{i32 720909, metadata !24, metadata !"_IO_read_end", metadata !25, i32 278, i64 32, i64 32, i64 64, i32 0, metadata !9} ; [ DW_TAG_member ]
!30 = metadata !{i32 720909, metadata !24, metadata !"_IO_read_base", metadata !25, i32 279, i64 32, i64 32, i64 96, i32 0, metadata !9} ; [ DW_TAG_member ]
!31 = metadata !{i32 720909, metadata !24, metadata !"_IO_write_base", metadata !25, i32 280, i64 32, i64 32, i64 128, i32 0, metadata !9} ; [ DW_TAG_member ]
!32 = metadata !{i32 720909, metadata !24, metadata !"_IO_write_ptr", metadata !25, i32 281, i64 32, i64 32, i64 160, i32 0, metadata !9} ; [ DW_TAG_member ]
!33 = metadata !{i32 720909, metadata !24, metadata !"_IO_write_end", metadata !25, i32 282, i64 32, i64 32, i64 192, i32 0, metadata !9} ; [ DW_TAG_member ]
!34 = metadata !{i32 720909, metadata !24, metadata !"_IO_buf_base", metadata !25, i32 283, i64 32, i64 32, i64 224, i32 0, metadata !9} ; [ DW_TAG_member ]
!35 = metadata !{i32 720909, metadata !24, metadata !"_IO_buf_end", metadata !25, i32 284, i64 32, i64 32, i64 256, i32 0, metadata !9} ; [ DW_TAG_member ]
!36 = metadata !{i32 720909, metadata !24, metadata !"_IO_save_base", metadata !25, i32 286, i64 32, i64 32, i64 288, i32 0, metadata !9} ; [ DW_TAG_member ]
!37 = metadata !{i32 720909, metadata !24, metadata !"_IO_backup_base", metadata !25, i32 287, i64 32, i64 32, i64 320, i32 0, metadata !9} ; [ DW_TAG_member ]
!38 = metadata !{i32 720909, metadata !24, metadata !"_IO_save_end", metadata !25, i32 288, i64 32, i64 32, i64 352, i32 0, metadata !9} ; [ DW_TAG_member ]
!39 = metadata !{i32 720909, metadata !24, metadata !"_markers", metadata !25, i32 290, i64 32, i64 32, i64 384, i32 0, metadata !40} ; [ DW_TAG_member ]
!40 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !41} ; [ DW_TAG_pointer_type ]
!41 = metadata !{i32 720915, null, metadata !"_IO_marker", metadata !25, i32 186, i64 96, i64 32, i32 0, i32 0, null, metadata !42, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!42 = metadata !{metadata !43, metadata !44, metadata !46}
!43 = metadata !{i32 720909, metadata !41, metadata !"_next", metadata !25, i32 187, i64 32, i64 32, i64 0, i32 0, metadata !40} ; [ DW_TAG_member ]
!44 = metadata !{i32 720909, metadata !41, metadata !"_sbuf", metadata !25, i32 188, i64 32, i64 32, i64 32, i32 0, metadata !45} ; [ DW_TAG_member ]
!45 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, metadata !24} ; [ DW_TAG_pointer_type ]
!46 = metadata !{i32 720909, metadata !41, metadata !"_pos", metadata !25, i32 192, i64 32, i64 32, i64 64, i32 0, metadata !16} ; [ DW_TAG_member ]
!47 = metadata !{i32 720909, metadata !24, metadata !"_chain", metadata !25, i32 292, i64 32, i64 32, i64 416, i32 0, metadata !45} ; [ DW_TAG_member ]
!48 = metadata !{i32 720909, metadata !24, metadata !"_fileno", metadata !25, i32 294, i64 32, i64 32, i64 448, i32 0, metadata !16} ; [ DW_TAG_member ]
!49 = metadata !{i32 720909, metadata !24, metadata !"_flags2", metadata !25, i32 298, i64 32, i64 32, i64 480, i32 0, metadata !16} ; [ DW_TAG_member ]
!50 = metadata !{i32 720909, metadata !24, metadata !"_old_offset", metadata !25, i32 300, i64 32, i64 32, i64 512, i32 0, metadata !51} ; [ DW_TAG_member ]
!51 = metadata !{i32 720918, null, metadata !"__off_t", metadata !25, i32 141, i64 0, i64 0, i64 0, i32 0, metadata !52} ; [ DW_TAG_typedef ]
!52 = metadata !{i32 720932, null, metadata !"long int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!53 = metadata !{i32 720909, metadata !24, metadata !"_cur_column", metadata !25, i32 304, i64 16, i64 16, i64 544, i32 0, metadata !54} ; [ DW_TAG_member ]
!54 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!55 = metadata !{i32 720909, metadata !24, metadata !"_vtable_offset", metadata !25, i32 305, i64 8, i64 8, i64 560, i32 0, metadata !56} ; [ DW_TAG_member ]
!56 = metadata !{i32 720932, null, metadata !"signed char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 6} ; [ DW_TAG_base_type ]
!57 = metadata !{i32 720909, metadata !24, metadata !"_shortbuf", metadata !25, i32 306, i64 8, i64 8, i64 568, i32 0, metadata !58} ; [ DW_TAG_member ]
!58 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 8, i64 8, i32 0, i32 0, metadata !10, metadata !59, i32 0, i32 0} ; [ DW_TAG_array_type ]
!59 = metadata !{metadata !60}
!60 = metadata !{i32 720929, i64 0, i64 0}        ; [ DW_TAG_subrange_type ]
!61 = metadata !{i32 720909, metadata !24, metadata !"_lock", metadata !25, i32 310, i64 32, i64 32, i64 576, i32 0, metadata !62} ; [ DW_TAG_member ]
!62 = metadata !{i32 720911, null, metadata !"", null, i32 0, i64 32, i64 32, i64 0, i32 0, null} ; [ DW_TAG_pointer_type ]
!63 = metadata !{i32 720909, metadata !24, metadata !"_offset", metadata !25, i32 319, i64 64, i64 64, i64 640, i32 0, metadata !64} ; [ DW_TAG_member ]
!64 = metadata !{i32 720918, null, metadata !"__off64_t", metadata !25, i32 142, i64 0, i64 0, i64 0, i32 0, metadata !65} ; [ DW_TAG_typedef ]
!65 = metadata !{i32 720918, null, metadata !"__quad_t", metadata !25, i32 56, i64 0, i64 0, i64 0, i32 0, metadata !66} ; [ DW_TAG_typedef ]
!66 = metadata !{i32 720932, null, metadata !"long long int", null, i32 0, i64 64, i64 64, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!67 = metadata !{i32 720909, metadata !24, metadata !"__pad1", metadata !25, i32 328, i64 32, i64 32, i64 704, i32 0, metadata !62} ; [ DW_TAG_member ]
!68 = metadata !{i32 720909, metadata !24, metadata !"__pad2", metadata !25, i32 329, i64 32, i64 32, i64 736, i32 0, metadata !62} ; [ DW_TAG_member ]
!69 = metadata !{i32 720909, metadata !24, metadata !"__pad3", metadata !25, i32 330, i64 32, i64 32, i64 768, i32 0, metadata !62} ; [ DW_TAG_member ]
!70 = metadata !{i32 720909, metadata !24, metadata !"__pad4", metadata !25, i32 331, i64 32, i64 32, i64 800, i32 0, metadata !62} ; [ DW_TAG_member ]
!71 = metadata !{i32 720909, metadata !24, metadata !"__pad5", metadata !25, i32 332, i64 32, i64 32, i64 832, i32 0, metadata !72} ; [ DW_TAG_member ]
!72 = metadata !{i32 720918, null, metadata !"size_t", metadata !25, i32 211, i64 0, i64 0, i64 0, i32 0, metadata !73} ; [ DW_TAG_typedef ]
!73 = metadata !{i32 720932, null, metadata !"unsigned int", null, i32 0, i64 32, i64 32, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!74 = metadata !{i32 720909, metadata !24, metadata !"_mode", metadata !25, i32 334, i64 32, i64 32, i64 864, i32 0, metadata !16} ; [ DW_TAG_member ]
!75 = metadata !{i32 720909, metadata !24, metadata !"_unused2", metadata !25, i32 336, i64 320, i64 8, i64 896, i32 0, metadata !76} ; [ DW_TAG_member ]
!76 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 320, i64 8, i32 0, i32 0, metadata !10, metadata !77, i32 0, i32 0} ; [ DW_TAG_array_type ]
!77 = metadata !{metadata !78}
!78 = metadata !{i32 720929, i64 0, i64 39}       ; [ DW_TAG_subrange_type ]
!79 = metadata !{metadata !80}
!80 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!81 = metadata !{i32 720942, i32 0, metadata !6, metadata !"concatenate", metadata !"concatenate", metadata !"", metadata !6, i32 72, metadata !7, i1 false, i1 true, i32 0, i32 0, null, i32 0, i1 false, i8* (i8*, i8*)* @concatenate, null, null, metadata !82} ; [ DW_TAG_subprogram ]
!82 = metadata !{metadata !83}
!83 = metadata !{i32 720932}                      ; [ DW_TAG_base_type ]
!84 = metadata !{metadata !85}
!85 = metadata !{metadata !86, metadata !89, metadata !91}
!86 = metadata !{i32 720948, i32 0, null, metadata !"temp_byte", metadata !"temp_byte", metadata !"", metadata !6, i32 31, metadata !87, i32 0, i32 1, i8* @temp_byte} ; [ DW_TAG_variable ]
!87 = metadata !{i32 720918, null, metadata !"Byte", metadata !6, i32 76, i64 0, i64 0, i64 0, i32 0, metadata !88} ; [ DW_TAG_typedef ]
!88 = metadata !{i32 720932, null, metadata !"unsigned char", null, i32 0, i64 8, i64 8, i64 0, i32 0, i32 8} ; [ DW_TAG_base_type ]
!89 = metadata !{i32 720948, i32 0, null, metadata !"temp_short", metadata !"temp_short", metadata !"", metadata !6, i32 32, metadata !90, i32 0, i32 1, i16* @temp_short} ; [ DW_TAG_variable ]
!90 = metadata !{i32 720932, null, metadata !"short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 5} ; [ DW_TAG_base_type ]
!91 = metadata !{i32 720948, i32 0, null, metadata !"temp_int", metadata !"temp_int", metadata !"", metadata !6, i32 33, metadata !16, i32 0, i32 1, i32* @temp_int} ; [ DW_TAG_variable ]
!92 = metadata !{i32 721153, metadata !5, metadata !"size", metadata !6, i32 16777252, metadata !16, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!93 = metadata !{i32 36, i32 7, metadata !5, null}
!94 = metadata !{i32 721152, metadata !95, metadata !"the_block", metadata !6, i32 38, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!95 = metadata !{i32 720907, metadata !5, i32 37, i32 3, metadata !6, i32 0} ; [ DW_TAG_lexical_block ]
!96 = metadata !{i32 38, i32 9, metadata !95, null}
!97 = metadata !{i32 40, i32 15, metadata !95, null}
!98 = metadata !{i32 41, i32 3, metadata !95, null}
!99 = metadata !{i32 43, i32 7, metadata !100, null}
!100 = metadata !{i32 720907, metadata !95, i32 42, i32 7, metadata !6, i32 1} ; [ DW_TAG_lexical_block ]
!101 = metadata !{i32 44, i32 7, metadata !100, null}
!102 = metadata !{i32 46, i32 3, metadata !95, null}
!103 = metadata !{i32 721153, metadata !13, metadata !"ptr", metadata !6, i32 16777266, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!104 = metadata !{i32 50, i32 9, metadata !13, null}
!105 = metadata !{i32 52, i32 3, metadata !106, null}
!106 = metadata !{i32 720907, metadata !13, i32 51, i32 3, metadata !6, i32 2} ; [ DW_TAG_lexical_block ]
!107 = metadata !{i32 53, i32 5, metadata !106, null}
!108 = metadata !{i32 54, i32 3, metadata !106, null}
!109 = metadata !{i32 721153, metadata !19, metadata !"filename", metadata !6, i32 16777273, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!110 = metadata !{i32 57, i32 9, metadata !19, null}
!111 = metadata !{i32 721153, metadata !19, metadata !"read_write_flag", metadata !6, i32 33554489, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!112 = metadata !{i32 57, i32 20, metadata !19, null}
!113 = metadata !{i32 721152, metadata !114, metadata !"fp", metadata !6, i32 59, metadata !22, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!114 = metadata !{i32 720907, metadata !19, i32 58, i32 3, metadata !6, i32 3} ; [ DW_TAG_lexical_block ]
!115 = metadata !{i32 59, i32 9, metadata !114, null}
!116 = metadata !{i32 61, i32 8, metadata !114, null}
!117 = metadata !{i32 62, i32 3, metadata !114, null}
!118 = metadata !{i32 64, i32 7, metadata !119, null}
!119 = metadata !{i32 720907, metadata !114, i32 63, i32 7, metadata !6, i32 4} ; [ DW_TAG_lexical_block ]
!120 = metadata !{i32 65, i32 7, metadata !119, null}
!121 = metadata !{i32 67, i32 3, metadata !114, null}
!122 = metadata !{i32 721153, metadata !81, metadata !"string1", metadata !6, i32 16777287, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!123 = metadata !{i32 71, i32 9, metadata !81, null}
!124 = metadata !{i32 721153, metadata !81, metadata !"string2", metadata !6, i32 33554503, metadata !9, i32 0, i32 0} ; [ DW_TAG_arg_variable ]
!125 = metadata !{i32 71, i32 19, metadata !81, null}
!126 = metadata !{i32 721152, metadata !127, metadata !"new_string", metadata !6, i32 73, metadata !9, i32 0, i32 0} ; [ DW_TAG_auto_variable ]
!127 = metadata !{i32 720907, metadata !81, i32 72, i32 3, metadata !6, i32 5} ; [ DW_TAG_lexical_block ]
!128 = metadata !{i32 73, i32 9, metadata !127, null}
!129 = metadata !{i32 73, i32 37, metadata !127, null}
!130 = metadata !{i32 73, i32 55, metadata !127, null}
!131 = metadata !{i32 75, i32 3, metadata !127, null}
!132 = metadata !{i32 76, i32 3, metadata !127, null}
!133 = metadata !{i32 77, i32 3, metadata !127, null}
