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

package utils.llc;

import backend.codegen.RegAllocLocal;
import backend.codegen.RegisterRegAlloc;
import backend.codegen.dagisel.RegisterScheduler;
import backend.codegen.dagisel.ScheduleDAGFast;
import backend.passManaging.FunctionPassManager;
import backend.support.BackendCmdOptions;
import backend.support.LLVMContext;
import backend.support.Triple;
import backend.target.*;
import backend.value.Function;
import backend.value.Module;
import cfe.basic.TargetInfo;
import cfe.system.Process;
import tools.OutRef;
import tools.PrintStackTraceProgram;
import tools.SMDiagnostic;
import tools.Util;
import tools.commandline.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static backend.target.TargetMachine.CodeGenOpt.*;
import static cfe.driver.CFrontEnd.MacOSVersionMin;
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
 * @version 0.4
 */
public class LLC {
  private static final StringOpt InputFilename =
      new StringOpt(new FormattingFlagsApplicator(Positional),
          desc("<input LLVM IR file>"),
          init("-"),
          valueDesc("filename"));
  private static final StringOpt OutputFilename =
      new StringOpt(optionName("o"), desc("Override output filename"),
          valueDesc("filename"),
          init(""));

  private static final Opt<TargetMachine.CodeGenFileType> Filetype =
      new Opt<TargetMachine.CodeGenFileType>(
          new Parser<>(),
          optionName("filetype"),
          desc("Specify the type of generated file, default to 'asm'"),
          new ValueClass<>(new ValueClass.Entry<>(TargetMachine.CodeGenFileType.CGFT_AssemblyFile, "asm", "Generate assembly code"),
              new ValueClass.Entry<>(TargetMachine.CodeGenFileType.CGFT_ObjectFile, "obj(experimental)", "Generate object code")),
          init(TargetMachine.CodeGenFileType.CGFT_AssemblyFile));

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

  public static CharOpt OptLevel = new CharOpt(
      new OptionNameApplicator("O"),
      desc("Optimization level. [-O0, -O1, -O2, or -O3] (default = '-O2')"),
      new FormattingFlagsApplicator(FormattingFlags.Prefix),
      new NumOccurrencesApplicator(NumOccurrences.ZeroOrMore),
      init(' '));

  public static BooleanOpt OptSize = new BooleanOpt(
      new OptionNameApplicator("Os"),
      desc("Optimize for size"),
      init(false));

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

  public static StringOpt TargetTriple = new StringOpt(
      new OptionNameApplicator("mtriple"),
      desc("Specify target triple (e.g. x86_64-unknown-linux-gnu)"),
      init(""));

  private static StringOpt MArch = new StringOpt(
      new OptionNameApplicator("march"),
      desc("Architecture to generate code for (see --version)"),
      init(""));

  public static final StringOpt MCPU = new StringOpt(
      new OptionNameApplicator("mcpu"),
      desc("Target a specific cpu type (-mcpu=help for details)"),
      init(""));

  public static final ListOpt<String> MAttrs = new ListOpt<String>(
      new ParserString(),
      new MiscFlagsApplicator(CommaSeparated),
      new OptionNameApplicator("mattr"),
      desc("Target specific attributes"),
      valueDesc("+a1,+a2,-a3,..."));

  // FIXME, This flag would be turn off in the release.
  public static final BooleanOpt DebugMode =
      new BooleanOpt(optionName("debug"),
          desc("Enable output debug informaton"),
          init(false));

  private static final Opt<TargetMachine.RelocModel> RelocModel = new Opt<>(
      new Parser<>(),
      optionName("relocation-model"),
      desc("Choose relocation model"),
      init(TargetMachine.RelocModel.Default),
      new ValueClass<>(
          new ValueClass.Entry<>(TargetMachine.RelocModel.Default,
              "default", "Target default relocation model"),
          new ValueClass.Entry<>(TargetMachine.RelocModel.Static,
              "static", "Non-relocable code"),
          new ValueClass.Entry<>(TargetMachine.RelocModel.PIC_,
              "pic", "Fully relocable position independent code"),
          new ValueClass.Entry<>(TargetMachine.RelocModel.DynamicNoPIC,
              "dynamic-no-pic",
              "Relocable external reference, non-relocable code")
      )
  );

  private static final Opt<TargetMachine.CodeModel> CMModel = new Opt<>(
      new Parser<>(),
      optionName("code-model"),
      desc("Choose code model"),
      init(TargetMachine.CodeModel.Default),
      new ValueClass<>(
          new ValueClass.Entry<>(TargetMachine.CodeModel.Default,
              "default", "Target default code model"),
          new ValueClass.Entry<>(TargetMachine.CodeModel.Small,
              "small", "Small code model"),
          new ValueClass.Entry<>(TargetMachine.CodeModel.Kernel,
              "kernel", "Kernel code model"),
          new ValueClass.Entry<>(TargetMachine.CodeModel.Medium,
              "medium", "Medium code model"),
          new ValueClass.Entry<>(TargetMachine.CodeModel.Large,
              "large", "Large code model")
      )
  );

  /**
   * This static code block is attempted to add some desired XCCTool command line
   * options into CommandLine DataBase.
   */
  static {
    BackendCmdOptions.registerBackendCommandLineOptions();
    TargetOptions.registerTargetOptions();
  }

  private static Module theModule;

  public static void main(String[] args) throws IOException {
    String[] temp;
    if (args[0].equals("llc")) {
      temp = args;
    } else {
      temp = new String[args.length + 1];
      temp[0] = "llc";
      System.arraycopy(args, 0, temp, 1, args.length);
    }
    new PrintStackTraceProgram(temp);

    // Initialize Target machine
    TargetSelect.InitializeAllTargetInfo();
    TargetSelect.InitializeAllTarget();

    CL.parseCommandLineOptions(args, "The Compiler for LLVM IR");

    Util.DEBUG = DebugMode.value;
    OutRef<SMDiagnostic> diag = new OutRef<>();
    theModule = backend.llReader.Parser.parseAssemblyFile(InputFilename.value, diag, LLVMContext.getGlobalContext());
    if (theModule == null) {
      diag.get().print("llc", System.err);
      System.exit(0);
    }

    Triple theTriple = new Triple(theModule.getTargetTriple());
    if (theTriple.getTriple().isEmpty())
      theTriple.setTriple(Process.getHostTriple());

    // Allocate target machine.  First, check whether the user has explicitly
    // specified an architecture to compile for. If so we have to look it up by
    // name, because it might be a backend that has no mapping to a target triple.
    Target theTarget = null;
    if (!MArch.value.isEmpty()) {
      for (Iterator<Target> itr = Target.TargetRegistry.iterator(); itr.hasNext(); ) {
        Target t = itr.next();
        if (t.getName().equals(MArch.value)) {
          theTarget = t;
          break;
        }
      }
      if (theTarget == null) {
        System.err.printf("llc:error: invalid target'%s'.\n", MArch.value);
        System.exit(1);
      }

      // Adjust the triple to match (if known), otherwise stick with the
      // module/host triple.
      Triple.ArchType type = Triple.getArchTypeForLLVMName(MArch.value);
      if (type != Triple.ArchType.UnknownArch)
        theTriple.setArch(type);
    }
    else {
      OutRef<String> error = new OutRef<>("");
      theTarget = Target.TargetRegistry.lookupTarget(theTriple.getTriple(), error);
      if (theTarget == null) {
        System.err.printf("llc:error: auto-selecting target for module '%s'." +
            " Please use the -march option to explicitly pick a target.\n", error.get());
        System.exit(1);
      }
    }
    // Package up features to be passed to target/subtarget
    String featureStr = "";
    if (!MCPU.value.isEmpty() || !MAttrs.isEmpty()) {
      SubtargetFeatures features = new SubtargetFeatures();
      features.setCPU(MCPU.value);
      for (int i = 0, e = MAttrs.size(); i < e; i++) {
        features.addFeature(MAttrs.get(i));
      }
      featureStr = features.getString();
    }

    TargetMachine tm = theTarget.createTargetMachine(theTriple.getTriple(), MCPU.value,
        featureStr, RelocModel.value, CMModel.value);
    Util.assertion(tm != null, "could not allocate a target machine");

    // Figure out where we should write the result file

    PrintStream os = computeOutFile(theTarget.getName());
    if (os == null) System.exit(1);

    TargetMachine.CodeGenOpt oLvl = TargetMachine.CodeGenOpt.None;
    switch (OptLevel.value) {
      default:
        System.err.println("llc: invalid optimization level.");
        System.exit(1);
        break;
      case ' ': break;
      case '0': oLvl = None; break;
      case '1': oLvl = Less; break;
      case '2': oLvl = Default; break;
      case '3': oLvl = Aggressive; break;
    }
    FunctionPassManager passes = new FunctionPassManager(theModule);

    // Add the target data from the target machine, if it exists, or the module.
    TargetData td = tm.getTargetData();
    if (td != null)
      passes.add(new TargetData(td));
    else
      passes.add(new TargetData(theModule));

    // Override default to generate verbose assembly.
    tm.setAsmVerbosityDefault(true);

    // Set the default register allocator.
    RegisterRegAlloc.setDefault(RegAllocLocal::createLocalRegAllocator);
    // Set the default instruction scheduler.
    RegisterScheduler.setDefault(ScheduleDAGFast::createFastDAGScheduler);

    if (tm.addPassesToEmitFile(passes, os, Filetype.value, oLvl)) {
        System.err.println("llc: Unable to generate this kind of file in this target!");
        if (os != System.out && os != System.err)
          Files.delete(Paths.get(OutputFilename.value));
        System.exit(1);
    }

    passes.doInitialization();
    // Run our queue of passes all at once now, efficiently.
    for (Function fn : theModule.getFunctionList()) {
      if (!fn.isDeclaration()) {
        passes.run(fn);
      }
    }
    passes.doFinalization();
  }

  private static PrintStream computeOutFile(String targetName) {
    if (!OutputFilename.value.isEmpty()) {
      if (OutputFilename.value.equals("-"))
        return System.out;

      try {
        File f = new File(OutputFilename.value);
        if (f.exists())
          f.delete();

        f.createNewFile();
        return new PrintStream(new FileOutputStream(f));
      } catch (IOException e) {
        System.err.println(e.getMessage());
        java.lang.System.exit(-1);
      }
    }

    if (InputFilename.value.equals("-")) {
      OutputFilename.value = "-";
      return System.out;
    }

    OutputFilename.value = getFileNameRoot(InputFilename.value);
    switch (Filetype.value) {
      case CGFT_AssemblyFile: {
        switch (targetName) {
          case "c":
            OutputFilename.value += ".cbe.c";
            break;
          case "cpp":
            OutputFilename.value += ".cpp";
          default:
            OutputFilename.value += ".s";
            break;
        }
        break;
      }
      case CGFT_ObjectFile:
        OutputFilename.value += ".o";
        break;
    }

    try {
      File f = new File(OutputFilename.value);
      if (f.exists())
        f.delete();

      f.createNewFile();
      //outPath.append(f.getAbsolutePath());
      return new PrintStream(new FileOutputStream(f));
    } catch (IOException e) {
      System.err.println(e.getMessage());
      java.lang.System.exit(-1);
    }
    return null;
  }

  private static String getFileNameRoot(String file) {
    File f = new File(file);
    if (!f.exists()) {
      System.err.println("input file doesn't exists!");
      System.exit(1);
    }
    String suffix = f.getName().substring(0, f.getName().lastIndexOf('.'));
    return f.getAbsoluteFile().getParent() + "/" + suffix;
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
    String targetCPU = MCPU.value;
    target.getDefaultFeatures(targetCPU, features);

    if (MAttrs.isEmpty())
      return;

    for (int i = 0, e = MAttrs.size(); i != e; i++) {
      String name = MAttrs.get(i);
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

