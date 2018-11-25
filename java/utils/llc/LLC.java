/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package utils.llc;

import backend.codegen.MachineCodeEmitter;
import backend.codegen.RegAllocLinearScan;
import backend.codegen.RegAllocLocal;
import backend.codegen.RegisterRegAlloc;
import backend.pass.Pass;
import backend.pass.PassCreator;
import backend.pass.PassRegisterationUtility;
import backend.passManaging.FunctionPassManager;
import backend.passManaging.PassManager;
import backend.support.BackendCmdOptions;
import backend.support.ErrorHandling;
import backend.target.*;
import backend.value.Module;
import jlang.basic.TargetInfo;
import jlang.driver.JlangCC;
import jlang.system.Process;
import tools.OutRef;
import tools.SMDiagnostic;
import tools.Util;
import tools.commandline.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static backend.target.TargetMachine.CodeGenFileType.AssemblyFile;
import static backend.target.TargetMachine.CodeGenFileType.ObjectFile;
import static backend.target.TargetMachine.CodeGenOpt.*;
import static jlang.driver.JlangCC.MacOSVersionMin;
import static tools.commandline.Desc.desc;
import static tools.commandline.FormattingFlags.Positional;
import static tools.commandline.Initializer.init;
import static tools.commandline.MiscFlags.CommaSeparated;
import static tools.commandline.OptionNameApplicator.optionName;
import static tools.commandline.ValueDesc.valueDesc;

/**
 * This class is an entry for compiling specified input LLVM assembly code into
 * machine code targeting specific machine (X86, X86_64 etc) in assembly or
 * object code.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class LLC {
  /**
   * The generated file type
   */
  enum FileType {
    Asm,
    Obj
  }

  private static final StringOpt InputFilename =
      new StringOpt(new FormattingFlagsApplicator(Positional),
          desc("<input LLVM IR file>"),
          init("-"),
          valueDesc("filename"));
  private static final StringOpt OutputFilename =
      new StringOpt(optionName("o"), desc("Override output filename"),
          valueDesc("filename"));

  private static final Opt<FileType> OutFiletype =
      new Opt<FileType>(
          new Parser<>(),
          optionName("filetype"),
          desc("Specify the type of generaed file"),
          new ValueClass<>(new ValueClass.Entry<>(FileType.Asm, "asm", "Generate assembly code"),
              new ValueClass.Entry<>(FileType.Obj, "obj", "Genrate object code")),
          init(FileType.Asm));

  public static class OptLevelParser extends ParserUInt {
    public boolean parse(Option<?> O, String ArgName,
                         String Arg, OutRef<Integer> Val) {
      if (super.parse(O, ArgName, Arg, Val))
        return true;
      if (Val.get() > 3)
        return O.error("'" + Arg + "' invalid optimization level!");
      return false;
    }
  }

  public static UIntOpt OptLevel = new UIntOpt(
      new JlangCC.OptLevelParser(),
      new OptionNameApplicator("O"),
      new FormattingFlagsApplicator(FormattingFlags.Prefix),
      desc("Optimization level"),
      init(0));

  public static BooleanOpt OptSize = new BooleanOpt(
      new OptionNameApplicator("Os"),
      desc("Optimize for size"),
      init(false));

  public static StringOpt TargetTriple = new StringOpt(
      new OptionNameApplicator("mtriple"),
      desc("Specify target triple (e.g. x86_64-unknown-linux-gnu)"),
      init(""));

  private static final BooleanOpt PrintEachModule =
      new BooleanOpt(optionName("p"),
          desc("Print module after each transformed"),
          init(false));
  private static final BooleanOpt StandardCompileOpts =
      new BooleanOpt(optionName("std-compile-opts"),
          desc("Include the standard compile time optimization"),
          init(false));
  private static final BooleanOpt DisableOptimizations =
      new BooleanOpt(optionName("disable-opt"),
          desc("Don't run any optimization passes"),
          init(false));
  private static final BooleanOpt VerifyEach =
      new BooleanOpt(optionName("verify-each"),
          desc("Verify after each transform"),
          init(false));


  public static final StringOpt TargetCPU = new StringOpt(
      new OptionNameApplicator("mcpu"),
      desc("Target a specific cpu type (-mcpu=help for details)"),
      init(""));

  public static final ListOpt<String> TargetFeatures = new ListOpt<String>(
      new ParserString(),
      new MiscFlagsApplicator(CommaSeparated),
      new OptionNameApplicator("target-feature"),
      desc("Target specific attributes"),
      valueDesc("+a1,+a2,-a3,..."));

  // FIXME, This flag would be turn off in the release.
  public static final BooleanOpt DebugMode =
      new BooleanOpt(optionName("debug"),
          desc("Enable output debug informaton"),
          init(false));

  /**
   * This static code block is attempted to add some desired JlangTool command line
   * options into CommandLine DataBase.
   */
  static {
    BackendCmdOptions.registerBackendCommandLineOptions();
    TargetOptions.registerTargetOptions();
  }

  private static Module theModule;
  private static FunctionPassManager perFunctionPasses;
  private static PassManager perModulePasses;
  private static FunctionPassManager perCodeGenPasses;

  public static void main(String[] args) {
    try {
      // Initialize Target machine
      TargetSelect ts = TargetSelect.create();
      ts.InitializeTargetInfo();
      ts.LLVMInitializeTarget();

      // Before parse command line options, register passes.
      PassRegisterationUtility.registerPasses();

      CL.parseCommandLineOptions(args, "The Compiler for LLVM IR");

      Util.DEBUG = DebugMode.value;

      OutRef<SMDiagnostic> diag = new OutRef<>();
      theModule = backend.llReader.Parser
          .parseAssemblyFile(InputFilename.value, diag);
      if (theModule == null)
        diag.get().print("llc", System.err);

      emitAssembly();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Handle to interactive with backend to generate actual machine code
   * or assembly code.
   */
  private static void emitAssembly() {
    // Silently ignore generating code, if backend.target data or module is null.
    if (theModule == null) return;

    // creates some necessary pass for code generation and optimization.
    createPass();
    OutRef<String> error = new OutRef<>("");
    if (!addEmitPasses(error)) {
      ErrorHandling.llvmReportError("UNKNOWN: " + error.get());
    }

    // Run passes. For now we do all passes at once.
    if (perFunctionPasses != null) {
      for (backend.value.Function f : theModule.getFunctionList())
        if (!f.isDeclaration())
          perFunctionPasses.run(f);
    }

    if (perModulePasses != null) {
      perModulePasses.run(theModule);
    }

    // performing a serial of code gen procedures, like instruction selection,
    // register allocation, and instruction scheduling etc.
    if (perCodeGenPasses != null) {
      // Performs initialization works before operating on Function.
      perCodeGenPasses.doInitialization();
      for (backend.value.Function f : theModule.getFunctionList())
        if (!f.isDeclaration())
          perCodeGenPasses.run(f);

      // Finalize!
      perCodeGenPasses.doFinalization();
    }
  }

  private static FunctionPassManager getPerFunctionPasses() {
    if (perFunctionPasses == null) {
      perFunctionPasses = new FunctionPassManager(theModule);
      perFunctionPasses.add(new TargetData(theModule));
    }
    return perFunctionPasses;
  }

  private static PassManager getPerModulePasses() {
    if (perModulePasses == null) {
      perModulePasses = new PassManager();
      perModulePasses.add(new TargetData(theModule));
      ;
    }
    return perModulePasses;
  }

  private static FunctionPassManager getCodeGenPasses() {
    if (perCodeGenPasses == null) {
      perCodeGenPasses = new FunctionPassManager(theModule);
      perCodeGenPasses.add(new TargetData(theModule));
    }
    return perCodeGenPasses;
  }

  private static void createPass() {
    // The optimization is not needed if optimization level is -O0.
    if (OptLevel.value > 0)
      PassCreator.createStandardFunctionPasses(getPerFunctionPasses(), OptLevel.value);

    // todo:add inline pass to function pass manager.

    Pass inliningPass = null;
    if (OptLevel.value > 0) {
      // inline small function.
      int threshold = (OptSize.value || OptLevel.value < 3) ? 50 : 200;
      inliningPass = PassCreator.createFunctionInliningPass(threshold);
    }

    // creates a module pass manager.
    PassManager pm = getPerModulePasses();
    PassCreator.createStandardModulePasses(pm,
        OptLevel.value,
        OptSize.value,
        false,
        inliningPass);
  }

  private static boolean addEmitPasses(OutRef<String> error) {
    switch (OutFiletype.value) {
      case Asm:
      case Obj:
        boolean fast = OptLevel.value < 1;
        FunctionPassManager pm = getCodeGenPasses();

        TargetMachine.CodeGenOpt optLevel = Default;
        switch (OptLevel.value) {
          default:
            break;
          case 0:
            optLevel = None;
            break;
          case 3:
            optLevel = Aggressive;
            break;
        }

        String triple = theModule.getTargetTriple();
        Target theTarget = Target.TargetRegistry.lookupTarget(triple, error);
        if (theTarget == null) {
          error.set("Unable to get target machine: " + error.get());
          return false;
        }

        String featureStr = "";
        ArrayList<String> featuresMap = computeCPUFeatures();
        if (!TargetCPU.value.isEmpty() || !featuresMap.isEmpty()) {
          SubtargetFeatures features = new SubtargetFeatures();
          features.setCPU(TargetCPU.value);
          for (String str : featuresMap) {
            features.addFeature(str);
          }
          featureStr = features.getString();
        }

        TargetMachine tm = theTarget.createTargetMachine(triple, featureStr);
        theTarget.setAsmVerbosityDefault(true);

        RegisterRegAlloc.setDefault(fast ?
            RegAllocLocal::createLocalRegAllocator :
            RegAllocLinearScan::createLinearScanRegAllocator);

        MachineCodeEmitter mce = null;
        TargetMachine.CodeGenFileType cft = OutFiletype.value == FileType.Asm
            ? AssemblyFile : ObjectFile;
        String ext = OutFiletype.value == FileType.Asm ? ".s" : ".o";
        PrintStream asmOutStream = computeOutFile(InputFilename.value, ext);
        switch (tm.addPassesToEmitFile(pm, asmOutStream, cft, optLevel)) {
          case AsmFile:
            break;
          case ElfFile:
            mce = tm.addELFWriter(pm, asmOutStream);
            break;
          default:
          case Error:
            error.set("Unable to interface with target machine!\n");
            return false;
        }
        if (tm.addPassesToEmitFileFinish(getCodeGenPasses(), mce, optLevel)) {
          error.set("Unable to interface with target machine!\n");
          return false;
        }
        return true;
      default:
        Util.assertion(false, "Unknown output file type");
        return false;
    }
  }

  private static PrintStream computeOutFile(String infile, String extension) {
    boolean usestdout = false;
    String outfile = OutputFilename.value;
    PrintStream os = null;
    String outputFile = "";
    if (outfile != null) {
      if (outfile.equals("-"))
        usestdout = true;
      else
        outputFile = outfile;
    } else {
      int dotPos = infile.lastIndexOf(".");
      if (dotPos >= 0)
        infile = infile.substring(0, dotPos);
      outputFile = infile + extension;
    }

    if (usestdout) {
      os = java.lang.System.out;
    } else {
      try {
        File f = new File(outputFile);
        if (f.exists())
          f.delete();

        f.createNewFile();
        //outPath.append(f.getAbsolutePath());
        os = new PrintStream(new FileOutputStream(f));
      } catch (IOException e) {
        System.err.println(e.getMessage());
        java.lang.System.exit(-1);
      }
    }
    return os;
  }

  /**
   * Recompute the target feature list to only be the list of things that are
   * enabled, based on the target cpu and feature list.
   *
   * @param target
   * @param features
   */
  private static void computeFeatureMap(TargetInfo target, HashMap<String, Boolean> features) {
    Util.assertion(features.isEmpty(), "Invalid map");

    // Initialze the feature map based on the target.
    String targetCPU = TargetCPU.value;
    target.getDefaultFeatures(targetCPU, features);

    if (TargetFeatures.isEmpty())
      return;

    for (int i = 0, e = TargetFeatures.size(); i != e; i++) {
      String name = TargetFeatures.get(i);
      char firstCh = name.charAt(0);
      if (firstCh != '-' && firstCh != '+') {
        java.lang.System.err.printf("error: xcc: invalid target features string: %s\n", name);
        java.lang.System.exit(-1);
      }
      if (!target.setFeatureEnabled(features, name.substring(1), firstCh == '+')) {
        java.lang.System.err.printf("error: xcc: invalid target features string: %s\n",
            name.substring(1));
        java.lang.System.exit(-1);
      }
    }
  }

  /**
   * If -mmacosx-version-min=10.12 is specified, change the triple
   * from being something like i386-apple-darwin17 to i386-apple-darwin16.
   *
   * @param triple
   * @return
   */
  private static String handleMacOSVersionMin(String triple) {
    int darwinDashIdx = triple.indexOf("-darwin");
    if (darwinDashIdx == -1) {
      java.lang.System.err.println("-mmacosx-version-min only valid for darwin (Mac OS X) targets");
      java.lang.System.exit(-1);
    }
    int darwinNumIdx = darwinDashIdx + "-darwin".length();
    // remove the darwin version number.
    triple = triple.substring(0, darwinNumIdx);
    String macosxmin = MacOSVersionMin.value;
    boolean macosxMinVersionInvalid = false;
    int versionNum = 0;

    // macos x version min must like this, 10.12.1
    if (macosxmin.length() < 4 || !macosxmin.startsWith("10.") ||
        !Process.isDigit(macosxmin.charAt(3))) {
      macosxMinVersionInvalid = true;
    } else {
      try {
        macosxmin = macosxmin.substring(3);
        int dotIdx = macosxmin.indexOf('.');
        if (dotIdx != -1)
          // like 10.12.1
          versionNum = Integer.parseInt(macosxmin.substring(0, dotIdx));
        else
          // like 10.12
          versionNum = Integer.parseInt(macosxmin);
        macosxMinVersionInvalid = versionNum > 13;
        triple += (versionNum + 4);
        if (dotIdx != -1) {
          triple += macosxmin.substring(dotIdx);
        }
      } catch (NumberFormatException e) {
        macosxMinVersionInvalid = true;
      }
    }

    if (macosxMinVersionInvalid) {
      java.lang.System.err.printf("-mmacosx-version-min=%s is invalid, expected something like '10.4'.\n",
          MacOSVersionMin.value);
      java.lang.System.exit(-1);
    } else if (versionNum < 4 && triple.startsWith("x86_64")) {
      java.lang.System.err.printf("-mmacosx-version-min=%s is invalid with -arch x86_64.\n",
          MacOSVersionMin.value);
      java.lang.System.exit(-1);
    }
    return triple;
  }

  /**
   * Process the various options that may affects the target triple and build a
   * final aggregate string that we are compiling for.
   *
   * @return
   */
  private static String createTargetTriple() {
    // Initialize base triple.  If a -triple option has been specified, use
    // that triple.  Otherwise, default to the host triple.
    String triple = TargetTriple.value;
    if (triple == null || triple.isEmpty())
      triple = Process.getHostTriple();

    if (!MacOSVersionMin.value.isEmpty())
      triple = handleMacOSVersionMin(triple);

    return triple;
  }

  private static ArrayList<String> computeCPUFeatures() {
    // Get information about the target being compiled for.
    String triple = createTargetTriple();
    TargetInfo target = TargetInfo.createTargetInfo(triple);

    HashMap<String, Boolean> features = new HashMap<>();
    computeFeatureMap(target, features);

    ArrayList<String> res = new ArrayList<>();
    for (Map.Entry<String, Boolean> entry : features.entrySet()) {
      String name = entry.getValue() ? "+" : "-";
      name += entry.getKey();
      res.add(name);
    }
    return res;
  }
}

