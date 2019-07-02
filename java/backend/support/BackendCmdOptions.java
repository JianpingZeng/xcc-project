package backend.support;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.codegen.*;
import backend.codegen.dagisel.RegisterScheduler;
import backend.codegen.dagisel.ScheduleDAG;
import backend.codegen.dagisel.ScheduleDAGFast;
import backend.codegen.dagisel.SelectionDAGISel;
import backend.pass.PassInfo;
import backend.passManaging.PMDataManager;
import backend.target.TargetMachine;
import tools.commandline.*;

import java.util.Objects;
import java.util.TreeSet;

import static backend.codegen.AsmWriterFlavorTy.ATT;
import static backend.codegen.AsmWriterFlavorTy.Intel;
import static backend.codegen.PrologEpilogInserter.ShrinkWrapDebugLevel;
import static backend.passManaging.PMDataManager.PassDebugLevel;
import static backend.support.BackendCmdOptions.AliasAnalyzerKind.*;
import static backend.support.BackendCmdOptions.InstructionSelectorKind.DAGISel;
import static backend.support.BackendCmdOptions.InstructionSelectorKind.MacroExpandISel;
import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionHidden.Hidden;
import static tools.commandline.OptionNameApplicator.optionName;
import static tools.commandline.ValueDesc.valueDesc;

/**
 * This is a utility class for registering Backend command line options into
 * CommandLine DataBase.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class BackendCmdOptions {
  public static final Opt<PMDataManager.PassDebugLevel> PassDebugging =
      new Opt<PMDataManager.PassDebugLevel>(
          new Parser<>(),
          new OptionNameApplicator("debug-pass"),
          new OptionHiddenApplicator(Hidden),
          Desc.desc("Print PassManager debugging information"),
          Initializer.init(PassDebugLevel.None),
          new ValueClass<>(
              new ValueClass.Entry<>(PassDebugLevel.None, "none", "disable debug output"),
              new ValueClass.Entry<>(PassDebugLevel.Arguments, "arguments", "print pass arguments to pass to 'opt'"),
              new ValueClass.Entry<>(PassDebugLevel.Structures, "structures", "print pass structure before run()"),
              new ValueClass.Entry<>(PassDebugLevel.Executions, "executions", "print pass name before it is executed"),
              new ValueClass.Entry<>(PassDebugLevel.Details, "details", "print pass details when it is executed")
          )
      );
  public static final Opt<PrologEpilogInserter.ShrinkWrapDebugLevel> ShrinkWrapDebugging =
      new Opt<PrologEpilogInserter.ShrinkWrapDebugLevel>(
          new Parser<>(), optionName("shrink-wrap-dbg"),
          new OptionHiddenApplicator(Hidden),
          desc("Print shrink wrapping debugging information"),
          new ValueClass<>(
              new ValueClass.Entry<>(
                  PrologEpilogInserter.ShrinkWrapDebugLevel.None, "None",
                  "disable debug output"),
              new ValueClass.Entry<>(ShrinkWrapDebugLevel.BasicInfo, "BasicInfo",
                  "print basic DF sets"),
              new ValueClass.Entry<>(
                  PrologEpilogInserter.ShrinkWrapDebugLevel.Iterations,
                  "Iterations", "print SR sets for each iteration"),
              new ValueClass.Entry<>(ShrinkWrapDebugLevel.Details,
                  "Details", "print all DF sets")));
  // Shrink Wrapping:
  public static final BooleanOpt ShrinkWrapping = new BooleanOpt(optionName("shrink-wrap"),
      init(false),
      desc("Shrink wrap callee-saved register spills/restores"));

  // Shrink wrap only the specified function, a debugging aid.
  public static final StringOpt ShrinkWrapFunc = new StringOpt(optionName("shrink-wrap-func"),
      desc("Shrink wrap the specified function"), valueDesc("funcname"),
      init(""));

  public static final BooleanOpt NewAsmPrinter =
      new BooleanOpt(optionName("experimental-asm-printer"),
          new OptionHiddenApplicator(Hidden));

  public static final Opt<MachinePassCtor> RegAlloc =
      new Opt<MachinePassCtor>(new RegisterRegAllocParser(),
          optionName("regalloc"),
          desc("Register allocator to use: (default = local)"),
          init((MachinePassCtor) RegAllocLocal::createLocalRegAllocator));

  public static final BooleanOpt UseDFSOnNumberMI =
      new BooleanOpt(optionName("use-dfs-numbering-mi"),
          new OptionHiddenApplicator(Hidden),
          desc("Use dfs order to number machine instr(default to true)"),
          init(true));

  public static final BooleanOpt DisableRearrangementMBB =
      new BooleanOpt(optionName("disable-rearrangement-mbb"),
          new OptionHiddenApplicator(Hidden),
          desc("Disable rearrangement on machine function(default to true)"),
          init(true));
  /**
   * A command line option to control what assembly dialect would be emitedd.
   */
  public static final Opt<AsmWriterFlavorTy> AsmWriterFlavor =
      new Opt<AsmWriterFlavorTy>(new Parser<>(),
          optionName("x86-asm-syntax"),
          init(ATT),
          new ValueClass<>(
              new ValueClass.Entry<>(ATT, "att", "Emit AT&T-style assembly"),
              new ValueClass.Entry<>(Intel, "intel", "Emit Intel-style assembly"))
      );

  public enum AliasAnalyzerKind {
    BasicAA,
    PoorMan,
    Steensgaard,
  }

  /**
   * This command line option used for selecting a suitable alias analyzer for
   * specified purpose.
   */
  public static final Opt<AliasAnalyzerKind> AliasAnalyzer =
      new Opt<AliasAnalyzerKind>(new Parser<>(),
          optionName("aa"),
          init(BasicAA),
          new ValueClass<>(
              new ValueClass.Entry<>(BasicAA, "basic-aa", "Basic alias analysis"),
              new ValueClass.Entry<>(PoorMan, "poor-man", "Poor man alias analysis"),
              new ValueClass.Entry<>(Steensgaard, "steensgaard", "Steensgaard alias analysis")
          ));

  /**
   * Choose an appropriate register allocator according command line option.
   *
   * @return
   */
  public static MachineFunctionPass createRegisterAllocator() {
    MachinePassCtor ctor = RegisterRegAlloc.getDefault();
    if (ctor == null) {
      ctor = RegAlloc.value;
      RegisterRegAlloc.setDefault(ctor);
    }
    return ctor.apply();
  }

  public static final BooleanOpt EnableUnsafeFPMath =
      new BooleanOpt(optionName("enable-unsafe-fp-math"),
          desc("Enable optimization that may decrease FP precision"),
          init(false));

  public static final BooleanOpt EnableFiniteOnlyFPMath =
      new BooleanOpt(optionName("enable-finite-only-fp-math"),
          desc("Enable optimizations that assumes non- NaNs / +-Infs"),
          init(false));


  public static final Opt<SchedPassCtor> InstScheduler =
      new Opt<>(new RegisterSchedulerParser(),
          optionName("pre-ra-sched"), desc("Instruction scheduler to use: (default = fast)"),
          init((SchedPassCtor) ScheduleDAGFast::createFastDAGScheduler));

  /**
   * Define an enumeration for telling user there are many selector available.
   */
  public enum InstructionSelectorKind {
    MacroExpandISel,
    DAGISel
  }

  public static final Opt<InstructionSelectorKind> InstructionSelector =
      new Opt<>(new Parser<>(),
          optionName("isel"),
          desc("Instruction Selector to use: (default = dagisel)"),
          new ValueClass<>(
              new ValueClass.Entry<>(MacroExpandISel, "macroexpandisel", "Macro expanding based selector(experimental)"),
              new ValueClass.Entry<>(DAGISel, "dagisel", "DAG covering based selector")),
          init(DAGISel));

  /***
   * A static method for creating a Scheduler Based on DAG.
   * @param isel
   * @param level
   * @return
   */
  public static ScheduleDAG createScheduler(SelectionDAGISel isel, TargetMachine.CodeGenOpt level) {
    SchedPassCtor ctor = RegisterScheduler.getDefault();
    if (ctor == null) {
      ctor = InstScheduler.value;
      RegisterScheduler.setDefault(ctor);
    }
    return ctor.apply(isel, level);
  }

  /**
   * Checks if we want to increase optimization opportunity in the spense of
   * reducing floating point precision.
   *
   * @return
   */
  public static boolean finiteOnlyFPMath() {
    return EnableUnsafeFPMath.value || EnableFiniteOnlyFPMath.value;
  }

  /**
   * A method used for registering Backend command line options.
   * </br>
   * This method must be called before calling to
   * {@linkplain tools.commandline.CL#parseCommandLineOptions(String[])}.
   */
  public static void registerBackendCommandLineOptions() {
  }

  /**
   * A command line option used for indicateing we should print IR after what pass.
   */
  public final static ListOpt<PassInfo> PrintBefore = new ListOpt<PassInfo>(
      new PassNameParser(),
      optionName("print-before"),
      desc("Print IR before specified passes"),
      new OptionHiddenApplicator(Hidden));

  public final static ListOpt<PassInfo> PrintAfter = new ListOpt<PassInfo>(
      new PassNameParser(),
      optionName("print-after"),
      desc("Print IR after specified passes"),
      new OptionHiddenApplicator(Hidden));

  public final static BooleanOpt PrintBeforeAll = new BooleanOpt(
      optionName("print-before-all"),
      desc("Print IR before each pass"),
      init(false));

  public final static BooleanOpt PrintAfterAll = new BooleanOpt(
      optionName("print-after-all"),
      desc("Print IR after each pass"),
      init(false));

  public final static ListOpt<String> PrintFuncsList = new ListOpt<String>(
      new ParserString(),
      optionName("filter-print-funcs"), valueDesc("function names"),
      desc("Only print IR for functions whose name" +
          " match this for all print-[before|after][-all] options"),
      new MiscFlagsApplicator(MiscFlags.CommaSeparated));

  public static boolean shouldPrintBeforeOrAfterPass(PassInfo pi,
                                                     ListOpt<PassInfo> passesToPrint) {
    for (PassInfo info : passesToPrint) {
      if (info != null && Objects.equals(info.getPassArgument(), pi.getPassArgument())) {
        return true;
      }
    }
    return false;
  }

  public static boolean shouldPrintBeforePass(PassInfo pi) {
    return PrintBeforeAll.value || shouldPrintBeforeOrAfterPass(pi, PrintBefore);
  }

  public static boolean shouldPrintAfterPass(PassInfo pi) {
    return PrintAfterAll.value || shouldPrintBeforeOrAfterPass(pi, PrintAfter);
  }

  public static boolean isFunctionInPrintList(String functionName) {
    TreeSet<String> printFuncNames = new TreeSet<>();
    printFuncNames.addAll(PrintFuncsList);
    return printFuncNames.isEmpty() || printFuncNames.contains(functionName);
  }
}
