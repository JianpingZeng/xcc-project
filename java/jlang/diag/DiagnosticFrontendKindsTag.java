package jlang.diag;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface DiagnosticFrontendKindsTag
{
    public static final int err_fe_error_backend = 0;
    public static final int err_fe_error_reading = 1;
    public static final int err_fe_error_reading_stdin = 2;
    public static final int err_fe_unknown_triple = 3;
    public static final int err_not_a_pch_file = 4;
    public static final int err_relocatable_without_without_isysroot = 5;
    public static final int note_fixit_applied = 6;
    public static final int note_fixit_failed = 7;
    public static final int note_fixit_in_macro = 8;
    public static final int note_fixit_unfixed_error = 9;
    public static final int note_pch_macro_defined_as = 10;
    public static final int note_using_macro_def_from_pch = 11;
    public static final int warn_cmdline_conflicting_macro_def = 12;
    public static final int warn_cmdline_missing_macro_defs = 13;
    public static final int warn_fixit_no_changes = 14;
    public static final int warn_macro_name_used_in_pch = 15;
    public static final int warn_pch_access_control = 16;
    public static final int warn_pch_altivec = 17;
    public static final int warn_pch_blocks = 17;
    public static final int warn_pch_builtins = 19;
    public static final int warn_pch_c99 = 20;
    public static final int warn_pch_char_signed = 21;
    public static final int warn_pch_compiler_options_mismatch = 22;
    public static final int warn_pch_cplusplus = 23;
    public static final int warn_pch_cplusplus0x = 24;
    public static final int warn_pch_exceptions = 25;
    public static final int warn_pch_extensions = 26;
    public static final int warn_pch_freestanding = 27;
    public static final int warn_pch_gc_mode = 28;
    public static final int warn_pch_gnu_extensions = 29;
    public static final int warn_pch_gnu_inline = 30;
    public static final int warn_pch_heinous_extensions = 31;
    public static final int warn_pch_lax_vector_conversions = 32;
    public static final int warn_pch_math_errno = 33;
    public static final int warn_pch_microsoft_extensions = 34;
    public static final int warn_pch_no_inline = 35;
    public static final int warn_pch_nonfragile_abi = 36;
    public static final int warn_pch_objc_runtime = 37;
    public static final int warn_pch_objective_c = 38;
    public static final int warn_pch_objective_c2 = 39;
    public static final int warn_pch_opencl = 40;
    public static final int warn_pch_optimize = 41;
    public static final int warn_pch_optimize_size = 42;
    public static final int warn_pch_overflow_checking = 43;
    public static final int warn_pch_pic_level = 44;
    public static final int warn_pch_static = 45;
    public static final int warn_pch_target_triple = 46;
    public static final int warn_pch_thread_safe_statics = 47;
    public static final int warn_pch_version_too_new = 48;
    public static final int warn_pch_version_too_old = 49;
    public static final int warn_unknown_warning_option = 50;
}
