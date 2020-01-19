package backend.target;
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

import tools.commandline.*;

import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionHidden.Hidden;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class TargetOptions {
  public static final BooleanOpt EnablePerformTailCallOpt =
      new BooleanOpt(optionName("tailcallopt"),
          desc("Turn on tail call optimization"),
          init(false));
  /**
   * Disable frame pointer elimination optimization.
   */
  public static final BooleanOpt DisableFPElim =
      new BooleanOpt(optionName("disable-fp-elim"),
          desc("Disable frame pointer elimination optimization"),
          init(false));

  public static final BooleanOpt DisableFPEliMLeaf =
      new BooleanOpt(optionName("disable-non-leaf-fp-elim"),
          desc("Disable frame pointer elimination optimization for non-leaf funcs"),
          init(false));

  public static final BooleanOpt EnableRealignStack =
      new BooleanOpt(optionName("realign-stack"),
          desc("Realign stack if needed"),
          init(true));

  public static final IntOpt OverrideStackAlignment =
      new IntOpt(optionName("stack-alignment"),
          desc("Override default stack alignment"),
          init(0));

  public static final BooleanOpt DisableJumpTables =
      new BooleanOpt(new OptionHiddenApplicator(Hidden),
          optionName("disable-jump-tables"),
          desc("Do not generate jump tables."),
          init(false));

  public static final BooleanOpt EnableStrongPhiElim =
      new BooleanOpt(optionName("strong-phi-elim"),
          new OptionHiddenApplicator(Hidden),
          desc("Use strong phi elimination"),
          init(false));

  public static BooleanOpt DontPlaceZerosInBSS =
      new BooleanOpt(optionName("nozero=initialized-in=bss"),
          desc("Don't place zero-initialized symbols into bss section"),
          init(false));
  /**
   * This flag is enabled when the -print-machineinstrs
   * option is specified on the command line, and should enable debugging
   * output from the code generator.
   */
  public static final BooleanOpt PrintMachineCode =
      new BooleanOpt(optionName("print-machineinstrs"),
          desc("Print generated machine code"),
          init(false));

  /**
   * To tell backend whether it should verify machine code after each pass executed.
   */
  public static final BooleanOpt VerifyMachineCode =
      new BooleanOpt(optionName("verify-machineinstrs"),
          new OptionHiddenApplicator(Hidden),
          desc("Verify generated machine code"),
          init(false));
  public static final BooleanOpt ViewDAGBeforeCodeGen =
      new BooleanOpt(optionName("view-dags-before-code-gen"),
          new OptionHiddenApplicator(Hidden),
          desc("Pop up a window to show dags before codegen"),
          init(false));

  public static final BooleanOpt ViewDAGAfterFirstLegalizeTypes =
      new BooleanOpt(optionName("view-dags-first-legalize-types"),
          new OptionHiddenApplicator(Hidden),
          desc("Pop up a window to show dags after first legalize types"),
          init(false));

  public static final BooleanOpt ViewDAGBeforeISel =
      new BooleanOpt(optionName("view-dags-before-isel"),
          new OptionHiddenApplicator(Hidden),
          desc("Pop up a window to show dags before isel"),
          init(false));
  public static final BooleanOpt ViewDAGBeforeSched =
      new BooleanOpt(optionName("view-dags-before-sched"),
          new OptionHiddenApplicator(Hidden),
          desc("Pop up a window to show dags before sched"),
          init(false));
  public static final BooleanOpt ViewDAGAfterSched =
      new BooleanOpt(optionName("view-dags-after-sched"),
          new OptionHiddenApplicator(Hidden),
          desc("Pop up a window to show dags after sched"),
          init(false));

  public static final BooleanOpt GenerateSoftFloatCalls =
      new BooleanOpt(optionName("soft-float"),
          desc("Generate software floating point library calls"),
          init(false));
  public static final Opt<FloatABI> FloatABIForType =
      new Opt<FloatABI>(new Parser<>(),
          optionName("float-abi"),
          desc("Choose float ABI type"),
          init(FloatABI.Default),
          new ValueClass<>(
              new ValueClass.Entry<>(FloatABI.Default, "default", "Target default float ABI type"),
              new ValueClass.Entry<>(FloatABI.Soft, "soft", "Soft float ABI (implied by -soft-float)"),
              new ValueClass.Entry<>(FloatABI.Hard, "hard", "Hard float ABI (uses FP registers)")));

  public static final BooleanOpt DisableMMX =
      new BooleanOpt(optionName("disable-mmx"),
          desc("Disable use of MMX"),
          new OptionHiddenApplicator(Hidden),
          init(false));

  public static final BooleanOpt DataSections =
      new BooleanOpt(optionName("fdata-sections"),
          desc("Emit data into separate section"),
          init(false));

  public static final BooleanOpt FunctionSections =
      new BooleanOpt(optionName("ffunction-sections"),
          desc("Emit function into seperate sections"),
          init(false));

  public static void registerTargetOptions() {
  }
}
