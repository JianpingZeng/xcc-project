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

package driver;

import backend.support.Triple;
import config.Config;
import cfe.diag.CompilationPhase;
import cfe.diag.Diagnostic;
import cfe.diag.Diagnostic.DiagnosticBuilder;
import cfe.diag.Diagnostic.StaticDiagInfoRec;
import cfe.diag.FullSourceLoc;
import tools.Pair;
import tools.Util;
import driver.Action.*;
import driver.Option.InputOption;
import driver.tool.Tool;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import static cfe.diag.CompilationPhase.*;
import static driver.DiagnosticJlangDriverTag.*;
import static driver.HostInfo.createLinuxHostInfo;
import static driver.HostInfo.createUnknownHostInfo;
import static driver.InputType.getNumCompilationPhases;
import static driver.InputType.getTypeName;
import static driver.OptionID.*;
import static driver.OptionKind.*;
import static driver.TypeID.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class Driver {
  /**
   * An static interface used for registering a initializer for the purpose of
   * adding some diagnostic kinds into Diagnostic related with XCCTool driver.
   */
  static {
    Diagnostic.DiagInitializer initializer = new Diagnostic.DiagInitializer() {
      @Override
      public StaticDiagInfoRec[] getDiagKinds() {
        DiagnosticJlangDriverKinds[] kinds = DiagnosticJlangDriverKinds.values();
        StaticDiagInfoRec[] recs = new StaticDiagInfoRec[kinds.length];
        int idx = 0;
        for (DiagnosticJlangDriverKinds kind : kinds) {
          recs[idx++] = new StaticDiagInfoRec(kind.diagID,
              kind.diagMapping, kind.diagClass, kind.sfinae, kind.text, kind.optionGroup);
        }
        return recs;
      }
    };

    Diagnostic.registerDiagInitialize(initializer);
  }

  private String defaultBasename;
  private String defaultDirname;
  private String defaultImagename;
  private Diagnostic theDiags;
  private String triple;
  private HostInfo host;
  private OptTable optTable;
  private boolean suppressMissingInputWarning;
  private ArrayList<String> installedDir, dir;
  private boolean cccPrintPhases;

  public Driver(String basename,
                String dirname,
                String hostTriple,
                String imageName,
                Diagnostic diags,
                boolean cccPrintPhases) {
    defaultBasename = basename;
    defaultDirname = dirname;
    defaultImagename = imageName;
    theDiags = diags;
    triple = hostTriple;
    optTable = new OptTable();
    installedDir = new ArrayList<>();
    dir = new ArrayList<>();
    installedDir.add(defaultDirname);
    this.cccPrintPhases = cccPrintPhases;
  }

  public DiagnosticBuilder diag(int diagID) {
    return theDiags.report(new FullSourceLoc(), diagID);
  }

  public OptTable getOptTable() {
    return optTable;
  }

  private InputArgList parseArgList(String[] args) {
    InputArgList argList = new InputArgList(args);
    for (int index = 0, sz = argList.getNumInputStrings(); index < sz; ) {
      if (argList.getArgString(index).isEmpty()) {
        ++index;
        continue;
      }

      int prev = argList.getIndex();
      Arg arg = getOptTable().parseOneArg(argList);
      int after = argList.getIndex();
      Util.assertion(after >= prev);
      if (arg == null) {
        diag(err_drv_missing_argument)
            .addString(argList.getArgString(prev))
            .addTaggedVal(after - prev - 1).emit();
        continue;
      }
      if (arg.getOption().isUnsupported()) {
        diag(err_drv_jlang_unsupported)
            .addString(argList.getArgString(prev)).emit();
        continue;
      }
      argList.add(arg);
      index = after;
    }
    return argList;
  }

  private HostInfo getHostInfo(String tripleStr) {
    Triple defaultTriple = new Triple(tripleStr);
    switch (defaultTriple.getArchName()) {
      case "i686":
      case "i386":
        defaultTriple.setArchName("i386");
        break;
      case "amd64":
      case "x86_64":
        defaultTriple.setArchName("x86_64");
        break;
      default:
        Util.assertion(false, "Unknown architecture name!");
    }
    switch (defaultTriple.getOS()) {
      case Linux:
        return createLinuxHostInfo(this, defaultTriple);
      default:
        return createUnknownHostInfo(this, defaultTriple);
    }
  }

  public boolean useJlangAsCompiler(Compilation comp, JobAction ja,
                                    String archName) {
    if (!archName.equals("x86_64") && !archName.equals("x86"))
      return false;
    if (ja instanceof PreprocessJobAction || ja instanceof PrecompileJobAction
        || ja instanceof CompileJobAction)
      return true;
    return false;
  }

  /**
   * Constructs an Action for each Compilation phase as follow.
   */
  private Action constructAction(ArgList args, int phase, Action input) {
    switch (phase) {
      case Preprocess: {
        int outputTy;
        if (args.hasArg(OPT__M) || args.hasArg(OPT__MM))
          outputTy = TY_Dependencies;
        else {
          outputTy = InputType
              .getPreprocessedType(input.getOutputType());
          Util.assertion(outputTy != TY_INVALID, "can't preprocess this input type!");

        }
        return new PreprocessJobAction(input, outputTy);
      }
      case Precompile:
        return new PrecompileJobAction(input, TY_PCH);
      case Compile: {
        if (args.hasArg(OPT__fsyntax_only)) {
          return new CompileJobAction(input, TY_Nothing);
        } else if (args.hasArg(OPT__emit_llvm) || args.hasArg(OPT__flto)
            || args.hasArg(OPT__O4)) {
          int outputTy = args.hasArg(OPT__S) ? TY_LLVMAsm : TY_LLVMBC;
          return new CompileJobAction(input, outputTy);
        } else
          return new CompileJobAction(input, TY_PP_Asm);
      }
      case Assemble: {
        return new AssembleJobAction(input, TY_Object);
      }
      default: {
        Util.assertion(phase == Link);
        Util.assertion(false, "Link should be handled in method buildActions!");
        return null;
      }
    }
  }

  private void buildActions(Compilation c) {
    ArgList args = c.getArgs();
    ToolChain tc = c.getToolChain();
    ArrayList<Action> linkerInputs = new ArrayList<>();

    int inputType = TY_Nothing;
    Arg inputTypeArg = null;

    ArrayList<Pair<Arg, Integer>> inputs = new ArrayList<>();

    for (int i = 0, e = args.size(); i < e; i++) {
      Arg arg = args.getArgs(i);
      Option opt = args.getOption(i);
      if (opt == null)
        continue;

      if (opt instanceof InputOption) {
        String value = arg.getValue(args, 0);
        int ty = TY_INVALID;

        if (inputType == TY_Nothing) {
          if (inputTypeArg != null)
            inputTypeArg.claim();

          if (value.equals("-")) {
            if (!args.hasArg(OPT__E, false))
              diag(err_drv_unknown_stdin_type).emit();
            ty = TY_C;
          } else {
            int lastDot = value.lastIndexOf('.');
            if (lastDot >= 0)
              ty = InputType.lookupTypeForExtension(
                  value.substring(lastDot + 1));
            if (ty == TY_INVALID)
              ty = TY_Object;
          }

          if (ty != TY_Object) {
            if (args.hasArg(OPT__ObjC))
              ty = TY_ObjC;
            else if (args.hasArg(OPT__ObjCXX))
              ty = TY_ObjCXX;
          }
        } else {
          inputTypeArg.claim();
          ty = inputType;
        }

        if (!value.equals("-") && !Files.exists(Paths.get(value))) {
          diag(err_drv_no_such_file).addString(arg.getValue(args, 0)).emit();
        } else {
          inputs.add(Pair.get(arg, ty));
        }
      } else if (opt.isLinkerInput()) {
        inputs.add(Pair.get(arg, TY_Object));
      } else if (arg.getOption().getID() == OPT__x) {
        inputTypeArg = arg;
        inputType = InputType.lookupTypeForExtension(
            arg.getValue(args, arg.getIndex() + 1));
      }
    }

    // Compute the final compilatio phase.
    int finalPhase;
    Arg finalPhaseArg = null;
    if ((finalPhaseArg = args.getLastArg(OPT__E, true)) != null
        || (finalPhaseArg = args.getLastArg(OPT__M, true)) != null
        || (finalPhaseArg = args.getLastArg(OPT__MM, true)) != null) {
      finalPhase = Preprocess;
    } else if ((finalPhaseArg = args.getLastArg(OPT__c, true)) != null) {
      finalPhase = Assemble;
    } else if ((finalPhaseArg = args.getLastArg(OPT__S, true)) != null
        || (finalPhaseArg = args.getLastArg(OPT__fsyntax_only, true)) != null) {
      finalPhase = Compile;
    } else
      // Other cases which we always treat as linker input.
      finalPhase = Link;

    for (Pair<Arg, Integer> entity : inputs) {
      int filetype = entity.second;
      Arg inputArg = entity.first;

      int numSteps = getNumCompilationPhases(filetype);
      Util.assertion(numSteps > 0, "Invalid number of steps!");

      int initialPhase = InputType.getCompilationPhase(filetype, 0);

      if (initialPhase > finalPhase) {
        inputArg.claim();
        diag(warn_drv_input_file_unused).addString(inputArg.getAsString(args))
            .addString(getPhaseName(initialPhase))
            .addString(finalPhaseArg.getOption().getName()).emit();
        continue;
      }

      Action current = new InputAction(inputArg, filetype);
      for (int i = 0; i < numSteps; i++) {
        int phase = InputType.getCompilationPhase(filetype, i);
        if (phase > finalPhase)
          break;

        if (phase == Link) {
          Util.assertion(i + 1 == numSteps, "Linker must be final compilation step!");

          linkerInputs.add(current);
          current = null;
          break;
        }
        if (phase == CompilationPhase.Assemble
            && current.getOutputType() != TY_PP_Asm)
          continue;

        current = constructAction(args, phase, current);
        Util.assertion(current != null);
        if (current.getOutputType() == TY_Nothing)
          break;

      }
      if (current != null)
        c.addAction(current);
    }
    if (!linkerInputs.isEmpty())
      c.addAction(new LinkJobAction(linkerInputs, TY_Image));
  }

  private String getPhaseName(int phase) {
    switch (phase) {
      case Preprocess:
        return "preprocess";
      case Precompile:
        return "precompile";
      case Compile:
        return "compile";
      case Assemble:
        return "assemble";
      case Link:
        return "link";
      default:
        Util.assertion(false, "Invalid phase");
        return "";
    }
  }

  private void buildJobs(Compilation c) {
    Arg finalOutput = c.getArgs().getLastArg(OPT__o, true);
    if (finalOutput != null) {
      long numOutputs = c.getActions().stream()
          .filter(act -> act.getOutputType() != TY_Nothing).count();
      if (numOutputs > 1) {
        diag(err_drv_output_argument_with_multiple_files).emit();
        finalOutput = null;
      }
    }

    for (Action act : c.getActions()) {
      buildJobsForAction(c, act, c.getToolChain(), true, null);
    }

    if (theDiags.getNumErrors() != 0 ||
        c.getArgs().hasArg(OPT__Qunused_arguments))
      return;

    // claim the option -###
    c.getArgs().hasArg(OPT___HASH_HASH_HASH);

    for (Arg arg : c.getArgs().getArgs()) {
      if (!arg.isClaimed()) {
        if (arg.getOption().isNoArgumentUnused())
          continue;

        Option opt = arg.getOption();
        if (opt instanceof Option.FlagOption) {
          boolean duplicatedClaimed = false;
          for (Arg a : c.getArgs().getArgs()) {
            if (a.isClaimed() && a.getOption().matches(opt.getID())) {
              duplicatedClaimed = true;
              break;
            }
          }
          if (duplicatedClaimed)
            continue;
        }
        diag(warn_drv_unused_argument)
            .addString(arg.getAsString(c.getArgs())).emit();
      }
    }
  }

  private InputInfo buildJobsForAction(Compilation c, Action act,
                                       ToolChain toolChain, boolean atTopLevel, String linkerOutput) {
    InputInfo result;
    if (act instanceof InputAction) {
      InputAction ia = (InputAction) act;
      Arg arg = ia.getInputArgs();
      arg.claim();
      if (arg instanceof Arg.PositionalArg) {
        String name = arg.getValue(c.getArgs(), 0);
        result = new InputInfo(name, act.getOutputType(), name);
      } else {
        result = new InputInfo(arg, act.getOutputType(), "");
      }
      return result;
    }
    if (act instanceof BindArchAction) {
      BindArchAction ba = (BindArchAction) act;
      String archNaem = ba.getArchName();
      if (archNaem == null || archNaem.isEmpty())
        archNaem = c.getToolChain().getArchName();

      return buildJobsForAction(c, ba.getInputs().get(0),
          host.getToolChain(c.getArgs(), archNaem), atTopLevel,
          linkerOutput);
    }

    JobAction ja = (JobAction) act;
    Tool t = toolChain.selectTool(c, ja);

    ArrayList<Action> inputs = act.getInputs();
    if (inputs.size() == 1 && inputs.get(0) instanceof PreprocessJobAction) {
      inputs = inputs.get(0).getInputs();
    }

    ArrayList<InputInfo> inputInfos = new ArrayList<>();
    for (Action action : inputs) {
      inputInfos.add(buildJobsForAction(c, action, toolChain, false,
          linkerOutput));
    }

    Job.JobList jobs = c.getJobs();
    String baseInput = inputInfos.get(0).getBaseInput();

    if (ja.getOutputType() == TY_Nothing)
      result = new InputInfo(act.getOutputType(), baseInput);
    else {
      Util.assertion(!inputInfos.get(0).isPipe(), "PipedJob not supported!");
      result = new InputInfo(
          getNamedOutputPath(c, ja, baseInput, atTopLevel),
          act.getOutputType(), baseInput);
    }

    jobs.add(t.constructJob(c, ja, result, inputInfos, c.getArgs(), linkerOutput));
    return result;
  }

  private String getNamedOutputPath(Compilation c, JobAction ja,
                                    String baseInput, boolean atTopLevel) {
    if (atTopLevel) {
      Arg finalOutput = c.getArgs().getLastArg(OPT__o, true);
      if (finalOutput != null)
        return c.addResultFile(finalOutput.getValue(c.getArgs(), 0));
    }

    if (!atTopLevel) {
      String tempName = getTemporaryPath("." + InputType.getTypeTempSuffix(ja.getOutputType()));
      ;
      return c.addTempFile(tempName);
    }

    Path basePath = Paths.get(baseInput);
    String baseName = basePath.getFileName().toString();
    String namedOutput;
    if (ja.getOutputType() == TY_Image)
      namedOutput = defaultImagename;
    else {
      String suffix = InputType.getTypeTempSuffix(ja.getOutputType());
      Util.assertion(suffix != null);
      int end = baseName.lastIndexOf('.');
      if (end < 0)
        end = baseName.length();
      String prefix = baseName.substring(0, end);
      namedOutput = prefix + "." + suffix;
    }

    return c.addResultFile(namedOutput);
  }

  private String getTemporaryPath(String suffix) {
    try {
      return Files.createTempFile(null, suffix).toAbsolutePath().toString();
    } catch (IOException e) {
      diag(err_drv_unable_to_make_temp).addString(e.getMessage()).emit();
    }
    return "";
  }

  private static String getOptionHelpName(OptTable table, int id) {
    String name = table.getOptionName(id);

    switch (table.getOptionKind(id)) {
      case KIND_Input:
      case KIND_Unknown:
        Util.assertion(false, "Invalid option with help text");
      case KIND_MultiArg:
      case KIND_JoinedAndSeparate:
        Util.assertion(false, "Can't print metavar for this kind of option");
      case KIND_Flag:
        break;
      case KIND_Separate:
      case KIND_JoinedOrSeparate:
        name += " ";
      case KIND_Joined:
      case KIND_CommaJoined:
        name += table.getOptionMetaVar(id);
        break;
    }
    return name;
  }

  private void printHelp(boolean printHidden) {
    System.out.println("OVERVIEW: jlang compiler driver");
    System.out.println();
    System.out.printf("USAGE: %s [options] <input files>%n%n",
        defaultBasename);
    System.out.println("OPTIONS:");

    ArrayList<Pair<String, String>> optionHelp = new ArrayList<>();
    int maximumOptionWidth = 0;
    for (int i = OPT__input_; i < OPT_LastOption; i++) {
      String text = getOptTable().getOptionHelpText(i);
      if (text != null) {
        String info = getOptionHelpName(getOptTable(), i);
        int length = info.length();
        maximumOptionWidth = Math.max(length, maximumOptionWidth);
        optionHelp.add(Pair.get(info, text));
      }
    }
    for (int i = 0, e = optionHelp.size(); i < e; i++) {
      String option = optionHelp.get(i).first;
      int length = option.length();
      System.out.printf("  %s%s", option, Util.fixedLengthString(maximumOptionWidth - length, ' '));
      System.out.printf(" %s%n", optionHelp.get(i).second);
    }
  }

  private void printVersion(Compilation c, PrintStream os) {
    os.printf("jlang version %s.%s%n", Config.XCC_Major, Config.XCC_Minor);
    ToolChain tc = c.getToolChain();
    os.printf("Target: %s%n", tc.getTripleString());
  }

  private boolean handleVersion(Compilation c) {
    if (c.getArgs().hasArg(OPT___help)
        || c.getArgs().hasArg(OPT___help_hidden)) {
      printHelp(c.getArgs().hasArg(OPT___help_hidden));
      return false;
    }

    if (c.getArgs().hasArg(OPT___version)) {
      printVersion(c, System.out);
      return false;
    }

    if (c.getArgs().hasArg(OPT__v) || c.getArgs().hasArg(OPT___HASH_HASH_HASH)) {
      printVersion(c, System.err);
      suppressMissingInputWarning = true;
    }
    return true;
  }

  public Compilation buildCompilation(String[] args) {
    InputArgList argList = parseArgList(args);
    host = getHostInfo(triple);

    Compilation c = new Compilation(this, host.getToolChain(argList, host.getArchName()),
        argList);

    if (!handleVersion(c))
      return c;

    // Builds a sequence of Actions to be performed, like preprocess,
    // precompile, compile, assembly, linking etc.
    buildActions(c);

    if (cccPrintPhases) {
      printActions(c);
      return c;
    }
    buildJobs(c);
    return c;
  }

  private static int printAction2(Compilation c, Action act, HashMap<Action, Integer> ids) {
    if (ids.containsKey(act))
      return ids.get(act);

    StringBuilder buf = new StringBuilder();
    buf.append(act.getKind().name);
    buf.append(", ");
    if (act instanceof InputAction) {
      InputAction ia = (InputAction) act;
      buf.append('"');
      buf.append(ia.getInputArgs().getValue(c.getArgs(), 0));
      buf.append('"');
    } else if (act instanceof BindArchAction) {
      BindArchAction ba = (BindArchAction) act;
      buf.append('"');
      buf.append(ba.getArchName() != null ? ba.getArchName() :
          c.getToolChain().getArchName());
      buf.append('"');
      buf.append(", {");
      buf.append(printAction2(c, ba.getInputs().get(0), ids));
      buf.append("}");
    } else {
      buf.append("{");
      for (int i = 0, e = act.getInputs().size(); i < e; i++) {
        Action a = act.getInputs().get(i);
        buf.append(printAction2(c, a, ids));
        if (i != e - 1)
          buf.append(", ");
      }
      buf.append("}");
    }
    int id = ids.size();
    ids.put(act, id);
    System.err.printf("%d: %s, %s%n", id, buf.toString(),
        getTypeName(act.getOutputType()));
    return id;
  }

  private void printActions(Compilation c) {
    HashMap<Action, Integer> ids = new HashMap<>();
    for (Action act : c.getActions()) {
      printAction2(c, act, ids);
    }
  }

  public int executeCompilation(Compilation c) {
    if (c.getArgs().hasArg(OPT___HASH_HASH_HASH)) {
      c.printJobs(System.err, c.getJobs(), false);
      return 0;
    }

    int res = c.executeJob();
    Job.Command failureCmd = c.getFailureCommand();
    if (res != 0) {
      clearTemporaryFiles(c);
    }

    if (res != 0) {
      Action source = failureCmd.getSource();
      boolean isFriendlyTool = source instanceof PreprocessJobAction ||
          source instanceof PrecompileJobAction ||
          source instanceof CompileJobAction;
      if (!isFriendlyTool || res != 1) {
        if (res < 0)
          diag(err_drv_command_signalled)
              .addString(source.getClassName()).emit();
        else
          diag(err_drv_command_failed)
              .addString(source.getClassName()).emit();
      }
    }
    return res;
  }

  private void clearTemporaryFiles(Compilation c) {
    c.clearTemporaryFiles();
  }

  public ArrayList<String> getInstalledDir() {
    if (!installedDir.isEmpty())
      return installedDir;
    return dir;
  }

  public ArrayList<String> getDir() {
    return dir;
  }

  public String getHostTriple() {
    return triple;
  }
}
