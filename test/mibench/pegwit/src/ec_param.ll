; ModuleID = 'ec_param.i'
target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7--linux-gnu"

%struct.ecPoint.2 = type { [36 x i16], [36 x i16] }

@prime_order = constant [19 x i16] [i16 16, i16 -13007, i16 17083, i16 9604, i16 24077, i16 11659, i16 19447, i16 -31730, i16 1351, i16 -16701, i16 -4709, i16 26908, i16 8980, i16 -32328, i16 -10160, i16 621, i16 1, i16 0, i16 0], align 2
@curve_point = constant %struct.ecPoint.2 { [36 x i16] [i16 17, i16 14540, i16 1327, i16 9488, i16 17834, i16 7049, i16 17512, i16 18562, i16 3431, i16 20459, i16 21966, i16 37, i16 19639, i16 3266, i16 23004, i16 10398, i16 26083, i16 22269, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0], [36 x i16] [i16 17, i16 12711, i16 26098, i16 6340, i16 13330, i16 29576, i16 21697, i16 21403, i16 18946, i16 19719, i16 4822, i16 30993, i16 15198, i16 20238, i16 8559, i16 11250, i16 6516, i16 8410, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0, i16 0] }, align 2

!llvm.dbg.cu = !{!0}

!0 = metadata !{i32 720913, i32 0, i32 12, metadata !"<unknown>", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", metadata !"clang version 3.1 (tags/RELEASE_30/final)", i1 true, i1 false, metadata !"", i32 0, metadata !1, metadata !1, metadata !1, metadata !3} ; [ DW_TAG_compile_unit ]
!1 = metadata !{metadata !2}
!2 = metadata !{i32 0}
!3 = metadata !{metadata !4}
!4 = metadata !{metadata !5, metadata !14}
!5 = metadata !{i32 720948, i32 0, null, metadata !"prime_order", metadata !"prime_order", metadata !"", metadata !6, i32 414, metadata !7, i32 0, i32 1, [19 x i16]* @prime_order} ; [ DW_TAG_variable ]
!6 = metadata !{i32 720937, metadata !"ec_param.c", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!7 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !8} ; [ DW_TAG_const_type ]
!8 = metadata !{i32 720918, null, metadata !"vlPoint", metadata !6, i32 17, i64 0, i64 0, i64 0, i32 0, metadata !9} ; [ DW_TAG_typedef ]
!9 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 304, i64 16, i32 0, i32 0, metadata !10, metadata !12, i32 0, i32 0} ; [ DW_TAG_array_type ]
!10 = metadata !{i32 720918, null, metadata !"word16", metadata !6, i32 11, i64 0, i64 0, i64 0, i32 0, metadata !11} ; [ DW_TAG_typedef ]
!11 = metadata !{i32 720932, null, metadata !"unsigned short", null, i32 0, i64 16, i64 16, i64 0, i32 0, i32 7} ; [ DW_TAG_base_type ]
!12 = metadata !{metadata !13}
!13 = metadata !{i32 720929, i64 0, i64 18}       ; [ DW_TAG_subrange_type ]
!14 = metadata !{i32 720948, i32 0, null, metadata !"curve_point", metadata !"curve_point", metadata !"", metadata !6, i32 418, metadata !15, i32 0, i32 1, %struct.ecPoint.2* @curve_point} ; [ DW_TAG_variable ]
!15 = metadata !{i32 720934, null, metadata !"", null, i32 0, i64 0, i64 0, i64 0, i32 0, metadata !16} ; [ DW_TAG_const_type ]
!16 = metadata !{i32 720918, null, metadata !"ecPoint", metadata !6, i32 18, i64 0, i64 0, i64 0, i32 0, metadata !17} ; [ DW_TAG_typedef ]
!17 = metadata !{i32 720915, null, metadata !"", metadata !18, i32 16, i64 1152, i64 16, i32 0, i32 0, null, metadata !19, i32 0, i32 0} ; [ DW_TAG_structure_type ]
!18 = metadata !{i32 720937, metadata !"ec_curve.h", metadata !"/home/xlous/Development/experiment/mediabench4/pegwit/src", null} ; [ DW_TAG_file_type ]
!19 = metadata !{metadata !20, metadata !26}
!20 = metadata !{i32 720909, metadata !17, metadata !"x", metadata !18, i32 17, i64 576, i64 16, i64 0, i32 0, metadata !21} ; [ DW_TAG_member ]
!21 = metadata !{i32 720918, null, metadata !"gfPoint", metadata !18, i32 36, i64 0, i64 0, i64 0, i32 0, metadata !22} ; [ DW_TAG_typedef ]
!22 = metadata !{i32 720897, null, metadata !"", null, i32 0, i64 576, i64 16, i32 0, i32 0, metadata !23, metadata !24, i32 0, i32 0} ; [ DW_TAG_array_type ]
!23 = metadata !{i32 720918, null, metadata !"lunit", metadata !18, i32 27, i64 0, i64 0, i64 0, i32 0, metadata !10} ; [ DW_TAG_typedef ]
!24 = metadata !{metadata !25}
!25 = metadata !{i32 720929, i64 0, i64 35}       ; [ DW_TAG_subrange_type ]
!26 = metadata !{i32 720909, metadata !17, metadata !"y", metadata !18, i32 17, i64 576, i64 16, i64 576, i32 0, metadata !21} ; [ DW_TAG_member ]
