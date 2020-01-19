package utils.opt;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.pass.*;
import backend.passManaging.PassManager;
import backend.support.LLVMContext;
import backend.target.TargetData;
import backend.value.Module;
import tools.OutRef;
import tools.SMDiagnostic;
import tools.Util;
import tools.commandline.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import static backend.pass.PassCreator.createStandardModulePasses;
import static backend.support.LLVMContext.getGlobalContext;
import static backend.support.PrintModulePass.createPrintModulePass;
import static tools.commandline.Desc.desc;
import static tools.commandline.FormattingFlags.Positional;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionNameApplicator.optionName;
import static tools.commandline.ValueDesc.valueDesc;

/**
 * This class aimed to test and verify various of optimization pass in Backend.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class Optimizer {
  // The OptimizationList is automatically populated with registered Passes by
  // the PassNameParser.
  private static final ListOpt<PassInfo> AvailablePasses =
      new ListOpt<PassInfo>(new PassNameParser(),
          desc("Optimizations available:"));

  private static final StringOpt InputFilename =
      new StringOpt(new FormattingFlagsApplicator(Positional),
          desc("<input LLVM IR file>"),
          init("-"),
          valueDesc("filename"));
  private static final StringOpt OutputFilename =
      new StringOpt(optionName("o"), desc("Override output filename"),
          valueDesc("filename"));
  private static BooleanOpt PrintEachModule =
      new BooleanOpt(optionName("p"),
          desc("Print module after each transformed"),
          init(false));
  private static BooleanOpt StandardCompileOpts =
      new BooleanOpt(optionName("std-compile-opts"),
          desc("Include the standard compile time optimization"),
          init(false));
  private static BooleanOpt DisableOptimizations =
      new BooleanOpt(optionName("disable-opt"),
          desc("Don't run any optimization passes"),
          init(false));
  private static BooleanOpt VerifyEach =
      new BooleanOpt(optionName("verify-each"),
          desc("Verify after each transform"),
          init(false));

  private static FileOutputStream fos;

  public static void main(String[] args) {
    try {
      // Before parse command line options, register passes.
      PassRegisterationUtility.registerPasses();

      CL.parseCommandLineOptions(args, "An optimizer on LLVM IR");

      OutRef<SMDiagnostic> diag = new OutRef<>();
      Module m = backend.llReader.Parser
          .parseAssemblyFile(InputFilename.value, diag, getGlobalContext());
      if (m == null)
        diag.get().print("optimizer", System.err);

      PassManager pm = new PassManager();
      pm.add(new TargetData(m));


      if (PrintEachModule.value) {
        pm.add(createPrintModulePass(System.err));
      }

      boolean allAnalysis = true;

      // Create a new optimization pass for each one specified on the command line
      for (int i = 0, e = AvailablePasses.size(); i < e; i++) {
        if (StandardCompileOpts.value
            && StandardCompileOpts.getPosition() < AvailablePasses.getPosition(i)) {
          addStandardCompilePasses(pm);
          StandardCompileOpts.value = false;
        }

        PassInfo pi = AvailablePasses.get(i);
        if (!pi.isAnalysis()) allAnalysis = false;

        Pass p = null;
        if (pi.getKlass() != null) {
          p = pi.createPass();
        } else
          System.err.println("Optimizer: can't create pass: " + pi.getPassName());

        if (p != null) {
          boolean isBBPass = p instanceof BasicBlockPass;
          boolean isLoopPass = p instanceof LoopPass;
          boolean isFunctionPass = p instanceof FunctionPass;
          addPass(pm, p);
        }
      }
      if (PrintEachModule.value) {
        pm.add(createPrintModulePass(System.err));
      }
      if (StandardCompileOpts.value) {
        addStandardCompilePasses(pm);
        StandardCompileOpts.value = false;
      }

      // After transform passes runed output transformed LLVM IR to
      // output file (Only when all runed pass are transform pass)
      if (!allAnalysis)
        createOutputPass(pm);

      pm.run(m);

      // close the file output stream if destination is file but no stdou
      if (fos != null) fos.close();
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  private static String computeOuputFilename(String inputname) {
    Util.assertion(inputname != null && !inputname.isEmpty());
    int dotPos = inputname.lastIndexOf('.');
    if (dotPos < 0)
      dotPos = inputname.length();
    return inputname.substring(0, dotPos) + ".ll";
  }

  private static void createOutputPass(PassManager pm) {
    if (OutputFilename.value == null || !OutputFilename.value.equals("-")) {
      String destFile = OutputFilename.value != null ?
          OutputFilename.value : computeOuputFilename(InputFilename.value);
      try {
        fos = new FileOutputStream(destFile);
        pm.add(createPrintModulePass(new PrintStream(fos)));
      } catch (FileNotFoundException e) {
        System.err.println("error: " + e.getMessage());
      }
    } else {
      pm.add(createPrintModulePass(System.err));
    }
  }

  private static void addPass(PassManager pm, Pass p) {
    pm.add(p);
    //if (VerifyEach.value) pm.add(createVerifyPass());
  }

  private static Pass createVerifyPass() {
    // TODO: 2017/11/29
    return null;
  }

  private static void addStandardCompilePasses(PassManager pm) {
    pm.add(createVerifyPass());
    if (DisableOptimizations.value)
      return;

    // TODO InlinePass
    createStandardModulePasses(pm,
        1/*optimization level*/,
        false/*optimize size*/,
        false/*unroll loop*/,
        null/*inline pass*/);
  }
}
