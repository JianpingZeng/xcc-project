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
    public static final int err_fe_error_reading = 0;
    public static final int err_fe_error_reading_stdin = 0;
    public static final int err_fe_unknown_triple = 0;
    public static final int err_not_a_pch_file = 0;
    public static final int err_relocatable_without_without_isysroot = 0;
    public static final int note_fixit_applied = 0;
    public static final int note_fixit_failed = 0;
    public static final int note_fixit_in_macro = 0;
    public static final int note_fixit_unfixed_error = 0;
    public static final int note_pch_macro_defined_as = 0;
    public static final int note_using_macro_def_from_pch = 0;
    public static final int warn_cmdline_conflicting_macro_def = 0;
    public static final int warn_cmdline_missing_macro_defs = 0;
    public static final int warn_fixit_no_changes = 0;
    public static final int warn_macro_name_used_in_pch = 0;
    public static final int warn_pch_access_control = 0;
    public static final int warn_pch_altivec = 0;
    public static final int warn_pch_blocks = 0;
    public static final int warn_pch_builtins = 0;
    public static final int warn_pch_c99 = 0;
    public static final int warn_pch_char_signed = 0;
    public static final int warn_pch_compiler_options_mismatch = 0;
    public static final int warn_pch_cplusplus = 0;
    public static final int warn_pch_cplusplus0x = 0;
    public static final int warn_pch_exceptions = 0;
    public static final int warn_pch_extensions = 0;
    public static final int warn_pch_freestanding = 0;
    public static final int warn_pch_gc_mode = 0;
    public static final int warn_pch_gnu_extensions = 0;
    public static final int warn_pch_gnu_inline = 0;
    public static final int warn_pch_heinous_extensions = 0;
    public static final int warn_pch_lax_vector_conversions = 0;
    public static final int warn_pch_math_errno = 0;
    public static final int warn_pch_microsoft_extensions = 0;
    public static final int warn_pch_no_inline = 0;
    public static final int warn_pch_nonfragile_abi = 0;
    public static final int warn_pch_objc_runtime = 0;
    public static final int warn_pch_objective_c = 0;
    public static final int warn_pch_objective_c2 = 0;
    public static final int warn_pch_opencl = 0;
    public static final int warn_pch_optimize = 0;
    public static final int warn_pch_optimize_size = 0;
    public static final int warn_pch_overflow_checking = 0;
    public static final int warn_pch_pic_level = 0;
    public static final int warn_pch_static = 0;
    public static final int warn_pch_target_triple = 0;
    public static final int warn_pch_thread_safe_statics = 0;
    public static final int warn_pch_version_too_new = 0;
    public static final int warn_pch_version_too_old = 0;
    public static final int warn_unknown_warning_option = 0;
}
