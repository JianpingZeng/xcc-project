package jlang.diag;
/*
 * Extremely C language Compiler.
 * Copyright (c), 2015-2017, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"),;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
import static jlang.diag.Diagnostic.DiagnosticClass.*;
import static jlang.diag.Diagnostic.Mapping.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum  DiagnosticFrontendKinds implements DiagnosticFrontendKindsTag 
{
    ERR_FE_ERROR_BACKEND(err_fe_error_backend, CLASS_ERROR, MAP_FATAL, "error in backend: %0", null, true),
    ERR_FE_ERROR_READING(err_fe_error_reading, CLASS_ERROR, MAP_ERROR, "error reading '%0'", null, true),
    ERR_FE_ERROR_READING_STDIN(err_fe_error_reading_stdin, CLASS_ERROR, MAP_ERROR, "error reading stdin", null, true),
    ERR_FE_UNKNOWN_TRIPLE(err_fe_unknown_triple, CLASS_ERROR, MAP_ERROR, "unknown target triple '%0', please use -triple or -arch", null, true),
    ERR_NOT_A_PCH_FILE(err_not_a_pch_file, CLASS_ERROR, MAP_FATAL, "'%0' does not appear to be a precompiled header file", null, true),
    ERR_RELOCATABLE_WITHOUT_ISYSROOT(err_relocatable_without_without_isysroot, CLASS_ERROR, MAP_ERROR, "must specify system root with -isysroot when building a relocatable PCH file", null, true),
    NOTE_FIXIT_APPLIED(note_fixit_applied, CLASS_NOTE, MAP_FATAL, "FIX-IT applied suggested code changes", null, true),
    NOTE_FIXIT_FALIED(note_fixit_failed, CLASS_NOTE, MAP_FATAL, "FIX-IT unable to apply suggested code changes", null, true),
    NOTE_FIXIT_IN_MACROS(note_fixit_in_macro, CLASS_NOTE, MAP_FATAL, "FIX-IT unable to apply suggested code changes in a macro", null, true),
    NOTE_FIXIT_UNFIXED_ERROR(note_fixit_unfixed_error, CLASS_NOTE, MAP_FATAL, "FIX-IT detected an error it cannot fix", null, true),
    NOTE_PCH_MACRO_DEFINED_AS(note_pch_macro_defined_as, CLASS_NOTE, MAP_FATAL, "definition of macro '%0' in the precompiled header", null, true),
    DIAG2(note_using_macro_def_from_pch, CLASS_NOTE, MAP_FATAL, "using this macro definition from precompiled header", null, true),
    DIAG3(warn_cmdline_conflicting_macro_def, CLASS_ERROR, MAP_ERROR, "definition of the macro '%0' conflicts with the definition used to build the precompiled header", null, true),
    DIAG4(warn_cmdline_missing_macro_defs, CLASS_WARNING, MAP_WARNING, "macro definitions used to build the precompiled header are missing", null, true),
    DIAG5(warn_fixit_no_changes, CLASS_NOTE, MAP_FATAL, "FIX-IT detected errors it could not fix; no output will be generated", null, true),
    DIAG6(warn_macro_name_used_in_pch, CLASS_ERROR, MAP_ERROR, "definition of macro %0 conflicts with an identifier used in the precompiled header", null, true),
    DIAG7(warn_pch_access_control, CLASS_ERROR, MAP_ERROR, "C++ access control was %select{disabled|enabled}0 in the PCH file but is currently %select{disabled|enabled}1", null, true),
    DIAG8(warn_pch_altivec, CLASS_ERROR, MAP_ERROR, "AltiVec initializers were %select{disabled|enabled}0 in PCH file but are currently %select{disabled|enabled}1", null, true),
    DIAG9(warn_pch_blocks, CLASS_ERROR, MAP_ERROR, "blocks were %select{disabled|enabled}0 in PCH file but are currently %select{disabled|enabled}1", null, true),
    DIAG10(warn_pch_builtins, CLASS_ERROR, MAP_ERROR, "PCH file was compiled with builtins %select{enabled|disabled}0 but builtins are currently %select{enabled|disabled}1", null, true),
    DIAG11(warn_pch_c99, CLASS_ERROR, MAP_ERROR, "C99 support was %select{disabled|enabled}0 in PCH file but is currently %select{disabled|enabled}1", null, true),
    DIAG12(warn_pch_char_signed, CLASS_ERROR, MAP_ERROR, "char was %select{unsigned|signed}0 in the PCH file but is currently %select{unsigned|signed}1", null, true),
    DIAG13(warn_pch_compiler_options_mismatch, CLASS_ERROR, MAP_ERROR, "compiler options used when building the precompiled header differ from the options used when using the precompiled header", null, true),
    DIAG14(warn_pch_cplusplus, CLASS_ERROR, MAP_ERROR, "C++ support was %select{disabled|enabled}0 in PCH file but is currently %select{disabled|enabled}1", null, true),
    DIAG15(warn_pch_cplusplus0x, CLASS_ERROR, MAP_ERROR, "C++0x support was %select{disabled|enabled}0 in PCH file but is currently %select{disabled|enabled}1", null, true),
    DIAG16(warn_pch_exceptions, CLASS_ERROR, MAP_ERROR, "exceptions were %select{disabled|enabled}0 in PCH file but are currently %select{disabled|enabled}1", null, true),
    DIAG17(warn_pch_extensions, CLASS_ERROR, MAP_ERROR, "extensions were %select{enabled|disabled}0 in PCH file but are currently %select{enabled|disabled}1", null, true),
    DIAG18(warn_pch_freestanding, CLASS_ERROR, MAP_ERROR, "PCH file was compiled with a %select{hosted|freestanding}0  implementation but a %select{hosted|freestanding}1 implementation is selected", null, true),
    DIAG19(warn_pch_gc_mode, CLASS_ERROR, MAP_ERROR, "the PCH file was built with %select{no||hybrid}0 garbage collection but the current translation unit will compiled with %select{no||hybrid}1 garbage collection", null, true),
    DIAG20(warn_pch_gnu_extensions, CLASS_ERROR, MAP_ERROR, "GNU extensions were %select{disabled|enabled}0 in PCH file but are currently %select{disabled|enabled}1", null, true),
    DIAG21(warn_pch_gnu_inline, CLASS_ERROR, MAP_ERROR, "PCH file was compiled with %select{C99|GNU|}0 inline semantics but %select{C99|GNU}1 inline semantics are currently selected", null, true),
    DIAG22(warn_pch_heinous_extensions, CLASS_ERROR, MAP_ERROR, "heinous extensions were %select{disabled|enabled}0 in PCH file but are currently %select{disabled|enabled}1", null, true),
    DIAG23(warn_pch_lax_vector_conversions, CLASS_ERROR, MAP_ERROR, "lax vector conversions were %select{disabled|enabled}0 in PCH file but are currently %select{disabled|enabled}1", null, true),
    DIAG24(warn_pch_math_errno, CLASS_ERROR, MAP_ERROR, "math functions %select{do not respect|respect}0 'errno' in PCH file but they are currently set to %select{not respect|respect}1 'errno'", null, true),
    DIAG25(warn_pch_microsoft_extensions, CLASS_ERROR, MAP_ERROR, "Microsoft extensions were %select{disabled|enabled}0 in PCH file but are currently %select{disabled|enabled}1", null, true),
    DIAG26(warn_pch_no_inline, CLASS_ERROR, MAP_ERROR, "the macro '__NO_INLINE__' was %select{not defined|defined}0 in the PCH file but is currently %select{undefined|defined}1", null, true),
    DIAG27(warn_pch_nonfragile_abi, CLASS_ERROR, MAP_ERROR, "PCH file was compiled with the %select{32-bit|non-fragile}0 Objective-C ABI but the %select{32-bit|non-fragile}1 Objective-C ABI is selected", null, true),
    DIAG28(warn_pch_objc_runtime, CLASS_ERROR, MAP_ERROR, "PCH file was compiled with the %select{NeXT|GNU}0 runtime but the %select{NeXT|GNU}1 runtime is selected", null, true),
    DIAG29(warn_pch_objective_c, CLASS_ERROR, MAP_ERROR, "Objective-C support was %select{disabled|enabled}0 in PCH file but is currently %select{disabled|enabled}1", null, true),
    DIAG30(warn_pch_objective_c2, CLASS_ERROR, MAP_ERROR, "Objective-C 2.0 support was %select{disabled|enabled}0 in PCH file but is currently %select{disabled|enabled}1", null, true),
    DIAG31(warn_pch_opencl, CLASS_ERROR, MAP_ERROR, "OpenCL language extensions were %select{disabled|enabled}0 in PCH file but are currently %select{disabled|enabled}1", null, true),
    DIAG32(warn_pch_optimize, CLASS_ERROR, MAP_ERROR, "the macro '__OPTIMIZE__' was %select{not defined|defined}0 in the PCH file but is currently %select{undefined|defined}1", null, true),
    DIAG33(warn_pch_optimize_size, CLASS_ERROR, MAP_ERROR, "the macro '__OPTIMIZE_SIZE__' was %select{not defined|defined}0 in the PCH file but is currently %select{undefined|defined}1", null, true),
    DIAG34(warn_pch_overflow_checking, CLASS_ERROR, MAP_ERROR, "signed integer overflow checking was %select{disabled|enabled}0 in PCH file but is currently %select{disabled|enabled}1", null, true),
    DIAG35(warn_pch_pic_level, CLASS_ERROR, MAP_ERROR, "PCH file was compiled with PIC level %0, but the current translation unit will be compiled with PIC level %1", null, true),
    DIAG36(warn_pch_static, CLASS_ERROR, MAP_ERROR, "the PCH file was compiled %select{dynamic|static}0 but the current translation unit is being compiled as %select{dynamic|static}1", null, true),
    DIAG37(warn_pch_target_triple, CLASS_ERROR, MAP_ERROR, "PCH file was compiled for the target '%0' but the current translation unit is being compiled for target '%1'", null, true),
    DIAG38(warn_pch_thread_safe_statics, CLASS_ERROR, MAP_ERROR, "PCH file was compiled %select{without|with}0 thread-safe statics butthread-safe statics are currently %select{disabled|enabled}1", null, true),
    DIAG39(warn_pch_version_too_new, CLASS_ERROR, MAP_ERROR, "PCH file uses a newer PCH format that cannot be read", null, true),
    DIAG40(warn_pch_version_too_old, CLASS_ERROR, MAP_ERROR, "PCH file uses an older PCH format that is no longer supported", null, true),
    DIAG41(warn_unknown_warning_option, CLASS_WARNING, MAP_WARNING, "unknown warning option '%0'", "unknown-warning-option", true);

    public final int diagID;
    public final Diagnostic.DiagnosticClass diagClass;
    public final Diagnostic.Mapping mapping;
    public final String description;
    public final boolean sfinae;
    public final String optionGroup;

    DiagnosticFrontendKinds(int diagID,
            Diagnostic.DiagnosticClass diagClass,
            Diagnostic.Mapping mapping,
            String description,
            String optionGroup,
            boolean sfinae)
    {
        this.diagID = diagID;
        this.diagClass = diagClass;
        this.mapping = mapping;
        this.description = description;
        this.sfinae = sfinae;
        this.optionGroup = optionGroup;
    }
}
