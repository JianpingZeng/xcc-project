/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package driver;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public interface DiagnosticJlangDriverTag {
  int err_drv_no_such_file = 0;
  int err_drv_unsupported_opt = err_drv_no_such_file + 1;
  int err_drv_unknown_stdin_type = err_drv_unsupported_opt + 1;
  int err_drv_unknown_language = err_drv_unknown_stdin_type + 1;
  int err_drv_invalid_opt_with_multiple_archs = err_drv_unknown_language + 1;
  int err_drv_invalid_output_with_multiple_archs = err_drv_invalid_opt_with_multiple_archs + 1;
  int err_drv_no_input_files = err_drv_invalid_output_with_multiple_archs + 1;
  int err_drv_use_of_Z_option = err_drv_no_input_files + 1;
  int err_drv_output_argument_with_multiple_files = err_drv_use_of_Z_option + 1;
  int err_drv_unable_to_make_temp = err_drv_output_argument_with_multiple_files + 1;
  int err_drv_unable_to_remove_file = err_drv_unable_to_make_temp + 1;
  int err_drv_command_failure = err_drv_unable_to_remove_file + 1;
  int err_drv_invalid_darwin_version = err_drv_command_failure + 1;
  int err_drv_missing_argument = err_drv_invalid_darwin_version + 1;
  int err_drv_invalid_Xarch_argument = err_drv_missing_argument + 1;
  int err_drv_argument_only_allowed_with = err_drv_invalid_Xarch_argument + 1;
  int err_drv_argument_not_allowed_with = err_drv_argument_only_allowed_with + 1;
  int err_drv_invalid_version_number = err_drv_argument_not_allowed_with + 1;
  int err_drv_no_linker_llvm_support = err_drv_invalid_version_number + 1;
  int err_drv_jlang_unsupported = err_drv_no_linker_llvm_support + 1;
  int err_drv_command_failed = err_drv_jlang_unsupported + 1;
  int err_drv_command_signalled = err_drv_command_failed + 1;
  int warn_drv_input_file_unused = err_drv_command_signalled + 1;
  int warn_drv_unused_argument = warn_drv_input_file_unused + 1;
  int warn_drv_pipe_ignored_with_save_temps = warn_drv_unused_argument + 1;
  int warn_drv_not_using_jlang_cpp = warn_drv_pipe_ignored_with_save_temps + 1;
  int warn_drv_not_using_jlang_cxx = warn_drv_not_using_jlang_cpp + 1;
  int warn_drv_not_using_jlang_arch = warn_drv_not_using_jlang_cxx + 1;
  int warn_drv_jlang_unsupported = warn_drv_not_using_jlang_arch + 1;
}
