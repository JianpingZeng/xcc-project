/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

package driver.tool.xcc;

import tools.Util;
import driver.*;
import driver.tool.Tool;

import java.util.ArrayList;

import static driver.DiagnosticJlangDriverTag.err_drv_argument_only_allowed_with;
import static driver.OptionID.*;
import static driver.TypeID.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class XCCTool extends Tool {
  public XCCTool(ToolChain tc) {
    super("cfe", "xcc frontend", tc);
  }

  @Override
  public Job constructJob(Compilation c, Action.JobAction ja,
                          InputInfo output, ArrayList<InputInfo> inputs, ArgList args,
                          String linkerOutput) {
    Driver driver = c.getDriver();
    ArrayList<String> cmdStrings = new ArrayList<>();

    Util.assertion(inputs.size() == 1, "Unable to compile multiple files!");

    cmdStrings.add("-triple");
    cmdStrings.add(getToolChain().getTripleString());

    // Add filename to be compiled.
    cmdStrings.add(inputs.get(0).getFilename());
    if (ja instanceof Action.PreprocessJobAction) {
      if (output.getOutputType() == TY_Dependencies)
        cmdStrings.add("-Eonly");
      else
        cmdStrings.add("-E");
    } else if (ja instanceof Action.PrecompileJobAction) {
      cmdStrings.add("=emit=pch");
    } else {
      Util.assertion(ja instanceof Action.CompileJobAction, "Invalid action for XCC frontend!");

      if (ja.getOutputType() == TY_Nothing)
        cmdStrings.add("-fsyntax-only");
      else if (ja.getOutputType() == TY_LLVMAsm) {
        cmdStrings.add("-emit-llvm");
      } else if (ja.getOutputType() == TY_LLVMBC) {
        Util.assertion(false, "No able to generate llvm bitcode!");
      } else if (ja.getOutputType() == TY_PP_Asm)
        cmdStrings.add("-S");
    }

    //cmdStrings.add("-disable-free");

    if (output.isPipe()) {
      cmdStrings.add("-o");
      cmdStrings.add("-");
    } else if (output.isFilename()) {
      cmdStrings.add("-o");
      cmdStrings.add(output.getFilename());
    } else {
      Util.assertion(output.isNothing() || output.isInputArg(), "Unknown output type!");
    }
    cmdStrings.add("-main-file-name");
    cmdStrings.add(inputs.get(0).getFilename());

        /*
        if (args.hasArg(OPT__static))
            cmdStrings.add("-static-define");

        boolean picEnabled = args.hasArg(OPT__fpic) || args.hasArg(OPT__fPIC)
                || args.hasArg(OPT__fpie) || args.hasArg(OPT__fPIE);
        boolean picDisabled = args.hasArg(OPT__mkernel) || args.hasArg(OPT__static);
        String model = getToolChain().getForcedPicModel();
        if (model == null)
        {
            if (args.hasArg(OPT__mdynamic_no_pic))
                model = "dynamic-no-pic";
            else if (picDisabled)
                model = "static";
            else if (picEnabled)
                model = "pic";
            else
                model = getToolChain().getDefaultRelocationModel();
        }
        cmdStrings.add("--relocation-model");
        cmdStrings.add(model);

        if (model.equals("pic") || model.equals("dynamic-no-pic"))
        {
            if (args.hasArg(OPT__fPIC))
                cmdStrings.add("-pic-level=2");
            else
                cmdStrings.add("-pic-level=1");
        }

        if (args.hasFlag(OPT__fno_omit_frame_pointer, OPT__fomit_frame_pointer))
            cmdStrings.add("--disable-fp-elim");
        if (!args.hasFlag(OPT__fzero_initialized_in_bss, OPT__fno_zero_initialized_in_bss))
            cmdStrings.add("--nozero-initialized-in-bss");
        */
    if (args.hasArg(OPT__dA) || args.hasArg(OPT__fverbose_asm))
      cmdStrings.add("=-asm-verbose");
    if (args.hasArg(OPT__fdebug_pass_structure))
      cmdStrings.add("--debug-pass=Structure");
    if (args.hasArg(OPT__fdebug_pass_arguments))
      cmdStrings.add("--debug-pass=Arguments");

    // Ignores options for unwind table.
    Arg arg = args.getLastArg(OPT__march_EQ, true);
    if (arg != null) {
      cmdStrings.add("-mcpu");
      cmdStrings.add(arg.getValue(args, 0));
    } else {
      // Set a default CPU.
      if (getToolChain().getArchName().equals("i386"))
        cmdStrings.add("-mcpu=pentium4");
      else if (getToolChain().getArchName().equals("x86_64"))
        cmdStrings.add("-mcpu=x86-64");
    }
        /*
        arg = args.getLastArg(OPT__mcmodel_EQ, true);
        if (arg != null)
        {
            cmdStrings.add("-code-model");
            cmdStrings.add(arg.getValue(args, 0));
        }
        */
    if ((arg = args.getLastArg(OPT__O4, true)) != null)
      cmdStrings.add("-O3");
    if ((arg = args.getLastArg(OPT__O, true)) != null) {
      arg.render(args, cmdStrings);
    }
    Arg std;
    if ((std = args.getLastArg(OPT__std_EQ, OPT__ansi, true)) != null) {
      if (std.getOption().matches(OPT__ansi))
        cmdStrings.add("-std=gnu89");
      else
        std.render(args, cmdStrings);

      if ((arg = args.getLastArg(OPT__trigraphs, true)) != null) {
        if (arg.getIndex() > std.getIndex())
          arg.render(args, cmdStrings);
      }
    } else {
      args.addAllArgsTranslated(cmdStrings, OPT__std_default_EQ, "-std=", true);
      args.addLastArg(cmdStrings, OPT__trigraphs);
    }

    if ((arg = args.getLastArg(OPT__fsigned_char, OPT__funsigned_char, true)) != null) {
      if (arg.getOption().matches(OPT__fsigned_char))
        cmdStrings.add("-fsigned-char");
      else
        cmdStrings.add("-fsigned-char=0");
    }

    if (args.hasArg(OPT__fdiagnostics_show_option))
      cmdStrings.add("-fdiagnostics-show-option");
    if (args.hasArg(OPT__fno_color_diagnostics))
      cmdStrings.add("-fno-color-diagnostics");
    if (!args.hasArg(OPT__fno_show_source_location))
      cmdStrings.add("-fno-show-source-location");

    if ((arg = args.getLastArg(OPT__fdollars_in_identifiers,
        OPT__fno_dollars_in_identifiers)) != null) {
      if (arg.getOption().matches(OPT__fdollars_in_identifiers))
        cmdStrings.add("-fdollars-in-identifiers=1");
      else
        cmdStrings.add("-fno-dollars-in-identifiers=0");
    }
    addPreprocessingOptions(driver, args, cmdStrings, output, inputs);
    return new Job.Command(ja, getToolChain().getCompiler(), cmdStrings);
  }

  private void addPreprocessingOptions(
      Driver driver,
      ArgList args,
      ArrayList<String> cmdStrings,
      InputInfo output,
      ArrayList<InputInfo> inputs) {
    Arg arg;
    if ((arg = args.getLastArg(OPT__C, true)) != null ||
        (arg = args.getLastArg(OPT__CC, true)) != null) {
      if (!args.hasArg(OPT__E))
        driver.diag(err_drv_argument_only_allowed_with)
            .addString(arg.getAsString(args))
            .addString("-E").emit();
      arg.render(args, cmdStrings);
    }

    args.addAllArgs(cmdStrings, OPT__D, OPT__U);
    args.addAllArgs(cmdStrings, OPT__F, OPT__I);
    getToolChain().addSystemIncludeDir(cmdStrings);
  }
}
