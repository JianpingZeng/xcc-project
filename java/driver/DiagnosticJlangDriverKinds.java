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

import cfe.diag.Diagnostic;

import static cfe.diag.Diagnostic.DiagnosticClass.CLASS_ERROR;
import static cfe.diag.Diagnostic.Mapping.MAP_ERROR;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public enum DiagnosticJlangDriverKinds implements DiagnosticJlangDriverTag {
  ERR_DRV_NO_SUCH_FILE(err_drv_no_such_file, CLASS_ERROR, MAP_ERROR, "no such file or directory: '%0'", true, null),
  ERR_DRV_UNSUPPORTED_OPT(err_drv_unsupported_opt, CLASS_ERROR, MAP_ERROR, "unsupported option '%0'", true, null),
  ERR_DRV_UNKNOWN_STDIN_TYPE(err_drv_unknown_stdin_type, CLASS_ERROR, MAP_ERROR, "-E or -x required when input is from standard input", true, null),
  ERR_DRV_UNKNOWN_LANGUAGE(err_drv_unknown_language, CLASS_ERROR, MAP_ERROR, "language not recognized: '%0'", true, null),
  ERR_DRV_INVALID_OPT_WITH_MULTIPLE_ARCHS(err_drv_invalid_opt_with_multiple_archs, CLASS_ERROR, MAP_ERROR, "option '%0' cannot be used with multiple -arch options", true, null),
  ERR_DRV_INVALID_OUTPUT_WITH_MULTIPLE_ARCHS(err_drv_invalid_output_with_multiple_archs, CLASS_ERROR, MAP_ERROR, "cannot use '%0' output with multiple -arch options", true, null),
  ERR_DRV_NO_INPUT_FILES(err_drv_no_input_files, CLASS_ERROR, MAP_ERROR, "no input files", true, null),
  ERR_DRV_USE_OF_Z_OPTION(err_drv_use_of_Z_option, CLASS_ERROR, MAP_ERROR, "unsupported use of internal gcc -Z option '%0'", true, null),
  ERR_DRV_OUTPUT_ARGUMENT_WITH_MULTIPLE_FILES(err_drv_output_argument_with_multiple_files, CLASS_ERROR, MAP_ERROR, "cannot specify -o when generating multiple output files", true, null),
  ERR_DRV_UNABLE_TO_MAKE_TEMP(err_drv_unable_to_make_temp, CLASS_ERROR, MAP_ERROR, "unable to make temporary file: %0", true, null),
  ERR_DRV_UNABLE_TO_REMOVE_FILE(err_drv_unable_to_remove_file, CLASS_ERROR, MAP_ERROR, "unable to remove file: %0", true, null),
  ERR_DRV_COMMAND_FAILURE(err_drv_command_failure, CLASS_ERROR, MAP_ERROR, "unable to execute command: %0", true, null),
  ERR_DRV_INVALID_DARWIN_VERSION(err_drv_invalid_darwin_version, CLASS_ERROR, MAP_ERROR, "invalid Darwin version number: %0", true, null),
  ERR_DRV_MISSING_ARGUMENT(err_drv_missing_argument, CLASS_ERROR, MAP_ERROR, "argument to '%0' is missing (expected %1 %plural{1:value|:values}1)", true, null),
  ERR_DRV_INVALID_XARCH_ARGUMENT(err_drv_invalid_Xarch_argument, CLASS_ERROR, MAP_ERROR, "invalid Xarch argument: '%0'", true, null),
  ERR_DRV_ARGUMENT_ONLY_ALLOWED_WITH(err_drv_argument_only_allowed_with, CLASS_ERROR, MAP_ERROR, "invalid argument '%0' only allowed with '%1'", true, null),
  ERR_DRV_ARGUMENT_NOT_ALLOWED_WITH(err_drv_argument_not_allowed_with, CLASS_ERROR, MAP_ERROR, "invalid argument '%0' not allowed with '%1'", true, null),
  ERR_DRV_INVALID_VERSION_NUMBER(err_drv_invalid_version_number, CLASS_ERROR, MAP_ERROR, "invalid version number in '%0'", true, null),
  ERR_DRV_NO_LINKER_LLVM_SUPPORT(err_drv_no_linker_llvm_support, CLASS_ERROR, MAP_ERROR, "'%0': unable to pass LLVM bit-code files to linker", true, null),
  ERR_DRV_JLANG_UNSUPPORTED(err_drv_jlang_unsupported, CLASS_ERROR, MAP_ERROR, "the jlang compiler does not support '%0'", true, null),
  ERR_DRV_COMMAND_FAILED(err_drv_command_failed, CLASS_ERROR, MAP_ERROR, "%0 command failed with exit code %1 (use -v to see invocation)", true, null),
  ERR_DRV_COMMAND_SIGNALLED(err_drv_command_signalled, CLASS_ERROR, MAP_ERROR, "%0 command failed due to signal %1 (use -v to see invocation)", true, null),
  WARN_DRV_INPUT_FILE_UNUSED(warn_drv_input_file_unused, CLASS_ERROR, MAP_ERROR, "%0: '%1' input unused when '%2' is present", true, null),
  WARN_DRV_UNUSED_ARGUMENT(warn_drv_unused_argument, CLASS_ERROR, MAP_ERROR, "argument unused during compilation: '%0'", true, null),
  WARN_DRV_PIPE_IGNORED_WITH_SAVE_TEMPS(warn_drv_pipe_ignored_with_save_temps, CLASS_ERROR, MAP_ERROR, "-pipe ignored because -save-temps specified", true, null),
  WARN_DRV_NOT_USING_JLANG_CPP(warn_drv_not_using_jlang_cpp, CLASS_ERROR, MAP_ERROR, "not using the jlang prepreprocessor due to user override", true, null),
  WARN_DRV_NOT_USING_JLANG_CXX(warn_drv_not_using_jlang_cxx, CLASS_ERROR, MAP_ERROR, "not using the jlang compiler for C++ inputs", true, null),
  WARN_DRV_NOT_USING_JLANG_ARCH(warn_drv_not_using_jlang_arch, CLASS_ERROR, MAP_ERROR, "not using the jlang compiler for the '%0' architecture", true, null),
  WARN_DRV_JLANG_UNSUPPORTED(warn_drv_jlang_unsupported, CLASS_ERROR, MAP_ERROR, "the jlang compiler does not support '%0'", true, null);

  public int diagID;
  public Diagnostic.DiagnosticClass diagClass;
  public Diagnostic.Mapping diagMapping;
  public String text;
  public final boolean sfinae;
  public final String optionGroup;

  DiagnosticJlangDriverKinds(int diagID, Diagnostic.DiagnosticClass diagClass,
                             Diagnostic.Mapping diagMapping, String text, boolean sfinae,
                             String optionGroup) {
    this.diagID = diagID;
    this.diagClass = diagClass;
    this.diagMapping = diagMapping;
    this.text = text;
    this.sfinae = sfinae;
    this.optionGroup = optionGroup;
  }
}
