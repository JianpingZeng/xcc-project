package cfe.diag;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import static cfe.diag.Diagnostic.DiagnosticFrontendBegin;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public interface DiagnosticFrontendKindsTag {
  public static final int err_fe_error_backend = DiagnosticFrontendBegin;
  public static final int err_fe_error_reading = err_fe_error_backend + 1;
  public static final int err_fe_error_reading_stdin = err_fe_error_reading + 1;
  public static final int err_fe_unknown_triple = err_fe_error_reading_stdin + 1;
  public static final int err_not_a_pch_file = err_fe_unknown_triple + 1;
  public static final int err_relocatable_without_without_isysroot = err_not_a_pch_file + 1;
  public static final int note_fixit_applied = err_relocatable_without_without_isysroot + 1;
  public static final int note_fixit_failed = note_fixit_applied + 1;
  public static final int note_fixit_in_macro = note_fixit_failed + 1;
  public static final int note_fixit_unfixed_error = note_fixit_in_macro + 1;
  public static final int note_pch_macro_defined_as = note_fixit_unfixed_error + 1;
  public static final int note_using_macro_def_from_pch = note_pch_macro_defined_as + 1;
  public static final int warn_cmdline_conflicting_macro_def = note_using_macro_def_from_pch + 1;
  public static final int warn_cmdline_missing_macro_defs = warn_cmdline_conflicting_macro_def + 1;
  public static final int warn_fixit_no_changes = warn_cmdline_missing_macro_defs + 1;
  public static final int warn_macro_name_used_in_pch = warn_fixit_no_changes + 1;
  public static final int warn_pch_access_control = warn_macro_name_used_in_pch + 1;
  public static final int warn_pch_altivec = warn_pch_access_control + 1;
  public static final int warn_pch_blocks = warn_pch_altivec + 1;
  public static final int warn_pch_builtins = warn_pch_blocks + 1;
  public static final int warn_pch_c99 = warn_pch_builtins + 1;
  public static final int warn_pch_char_signed = warn_pch_c99 + 1;
  public static final int warn_pch_compiler_options_mismatch = warn_pch_char_signed + 1;
  public static final int warn_pch_cplusplus = warn_pch_compiler_options_mismatch + 1;
  public static final int warn_pch_cplusplus0x = warn_pch_cplusplus + 1;
  public static final int warn_pch_exceptions = warn_pch_cplusplus0x + 1;
  public static final int warn_pch_extensions = warn_pch_exceptions + 1;
  public static final int warn_pch_freestanding = warn_pch_extensions + 1;
  public static final int warn_pch_gc_mode = warn_pch_freestanding + 1;
  public static final int warn_pch_gnu_extensions = warn_pch_gc_mode + 1;
  public static final int warn_pch_gnu_inline = warn_pch_gnu_extensions + 1;
  public static final int warn_pch_heinous_extensions = warn_pch_gnu_inline + 1;
  public static final int warn_pch_lax_vector_conversions = warn_pch_heinous_extensions + 1;
  public static final int warn_pch_math_errno = warn_pch_lax_vector_conversions + 1;
  public static final int warn_pch_microsoft_extensions = warn_pch_math_errno + 1;
  public static final int warn_pch_no_inline = warn_pch_microsoft_extensions + 1;
  public static final int warn_pch_nonfragile_abi = warn_pch_no_inline + 1;
  public static final int warn_pch_objc_runtime = warn_pch_nonfragile_abi + 1;
  public static final int warn_pch_objective_c = warn_pch_objc_runtime + 1;
  public static final int warn_pch_objective_c2 = warn_pch_objective_c + 1;
  public static final int warn_pch_opencl = warn_pch_objective_c2 + 1;
  public static final int warn_pch_optimize = warn_pch_opencl + 1;
  public static final int warn_pch_optimize_size = warn_pch_optimize + 1;
  public static final int warn_pch_overflow_checking = warn_pch_optimize_size + 1;
  public static final int warn_pch_pic_level = warn_pch_overflow_checking + 1;
  public static final int warn_pch_static = warn_pch_pic_level + 1;
  public static final int warn_pch_target_triple = warn_pch_static + 1;
  public static final int warn_pch_thread_safe_statics = warn_pch_target_triple + 1;
  public static final int warn_pch_version_too_new = warn_pch_thread_safe_statics + 1;
  public static final int warn_pch_version_too_old = warn_pch_version_too_new + 1;
  public static final int warn_unknown_warning_option = warn_pch_version_too_old;
}
