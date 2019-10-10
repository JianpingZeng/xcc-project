package backend.target;
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
import backend.mc.MCAsmInfo;
import backend.mc.MCAsmStreamer;
import backend.mc.MCInstPrinter;
import backend.mc.MCStreamer;
import backend.mc.MCSymbol.MCContext;
import backend.pass.FunctionPass;
import backend.passManaging.PassManagerBase;
import backend.support.BackendCmdOptions;
import tools.Util;

import java.io.OutputStream;
import java.io.PrintStream;

import static backend.codegen.MachineCodeVerifier.createMachineVerifierPass;
import static backend.codegen.PrologEpilogInserter.createPrologEpilogEmitter;
import static backend.support.BackendCmdOptions.DisableRearrangementMBB;
import static backend.support.BackendCmdOptions.PrintAfterAll;
import static backend.support.PrintMachineFunctionPass.createMachineFunctionPrinterPass;
import static backend.target.TargetOptions.PrintMachineCode;
import static backend.target.TargetOptions.VerifyMachineCode;
import static backend.transform.scalars.UnreachableBlockElim.createUnreachableBlockEliminationPass;

/**
 * This class describes a target machine that is
 * implemented with the LLVM target-independent code generator.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class LLVMTargetMachine extends TargetMachine {
  protected LLVMTargetMachine(Target target, String triple) {
    super(target);
    initAsmInfo(target, triple);
  }

  private void initAsmInfo(Target target, String triple) {
    asmInfo = target.createAsmInfo(triple);
    Util.assertion(asmInfo != null, "Must initialize the MCAsmInfo for AsmPrinter!");
  }

  private static void printAndVerify(PassManagerBase pm,
                                     boolean allowDoubleDefs, String banner) {
    if (PrintMachineCode.value || PrintAfterAll.value)
      pm.add(createMachineFunctionPrinterPass(System.err, banner));

    if (VerifyMachineCode.value)
      pm.add(createMachineVerifierPass(allowDoubleDefs));
  }

  private static void printAndVerify(
      PassManagerBase pm,
      boolean allowDoubleDefs) {
    if (PrintMachineCode.value)
      pm.add(createMachineFunctionPrinterPass(System.err));

    if (VerifyMachineCode.value)
      pm.add(createMachineVerifierPass(allowDoubleDefs));
  }

  /**
   * Add standard LLVM codegen passes used for both emitting to assembly file
   * or machine code output.
   *
   * @param pm
   * @param level
   * @return
   */
  protected boolean addCommonCodeGenPasses(PassManagerBase pm, CodeGenOpt level) {
    // lowers switch instr into chained branch instr.
    // pm.add(createLowerSwitchPass());
    if (level != CodeGenOpt.None) {
      // todo pm.add(createLoopStrengthReducePass(getTargetLowering()));
    }

    if (level.compareTo(CodeGenOpt.None) > 0)
      pm.add(createUnreachableBlockEliminationPass());

    pm.add(new MachineFunctionAnalysis(this, level));

    // Ask the target for an isel.
    if (addInstSelector(pm, level))
      return true;

    // Expand the pseudo instructions generated in isel phase.
    pm.add(ExpandISelPseudo.createExpandISelPseudoPass());

    if (!DisableRearrangementMBB.value) {
      // Before instruction selection, rearragement blocks.
      pm.add(RearrangementMBB.createRearrangeemntPass());
      printAndVerify(pm, true,
          "# *** IR dump after RearragementMBB pass ***:\n");
    }

    if (addPreRegAlloc(pm, level))
      return true;

    printAndVerify(pm, true,
        "# *** IR dump after Pre-Register allocation ***:\n");

    // Perform register allocation to convert to a concrete x86 representation
    pm.add(BackendCmdOptions.createRegisterAllocator());
    // Print machine code after register allocation.
    printAndVerify(pm, false,
        "# *** IR dump after Register Allocator ***:\n");

    if (addPostRegAlloc(pm, level))
      printAndVerify(pm, false, "# *** IR dump after Post-Register allocation ***:\n");

    pm.add(LowerSubregInstructionPass.createLowerSubregPass());
    printAndVerify(pm, false,
        "# *** IR dump after Subregister lowering ***:\n");

    pm.add(createPrologEpilogEmitter());
    printAndVerify(pm, false,
        "# *** IR Dump After Prologue/Epilogue Insertion & Frame Finalization ***:\n");
    return false;
  }

  @Override
  public boolean addPassesToEmitFile(PassManagerBase pm,
                                       OutputStream os, CodeGenFileType fileType,
                                       CodeGenOpt optLevel) {
    if (addCommonCodeGenPasses(pm, optLevel))
      return true;

    if (PrintMachineCode.value) {
      pm.add(createMachineFunctionPrinterPass(System.err,
          "# *** IR dump before emitting code ***:\n"));
    }

    if (addPreEmitPass(pm, optLevel) && PrintMachineCode.value) {
      pm.add(createMachineFunctionPrinterPass(System.err,
          "# *** IR dump after emitting code ***:\n"));
      return true;
    }

    MCContext ctx = new MCContext(getMCAsmInfo(), getRegisterInfo());
    MCStreamer streamer = null;
    PrintStream legacyOutput = null;

    switch (fileType) {
      default:
        return true;
      case CGFT_AssemblyFile: {
        MCAsmInfo mai = getMCAsmInfo();
        // Set the AsmPrinter's "O" to the output file.
        legacyOutput = new PrintStream(os);
        MCInstPrinter instPrinter =
            getTarget().createMCInstPrinter(mai.getAssemblerDialect(), legacyOutput, mai);
        streamer = MCAsmStreamer.createAsmStreamer(ctx, legacyOutput,
            mai, getTargetData().isLittleEndian(), getAsmVerbosityDefault(),
            instPrinter, null, false);
        break;
      }
      case CGFT_Null:
      case CGFT_ObjectFile: {
        Util.shouldNotReachHere("Object emit is not supported yet!");;
        return true;
      }
    }

    FunctionPass printer = getTarget().createAsmPrinter(legacyOutput,
        this, ctx, streamer, getMCAsmInfo());
    if (printer == null)
      return true;

    setCodeModelForStatic();
    pm.add(printer);
    return false;
  }

  public void setCodeModelForStatic() {
    setCodeModel(CodeModel.Small);
  }

  public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  public boolean addPreRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  public boolean addPostRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  public boolean addPreEmitPass(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  /**
   * This pass should be overridden by the target to add
   * a code emitter (without setting flags), if supported.  If this is not
   * supported, 'true' should be returned.
   *
   * @param pm
   * @param level
   * @param mce
   * @return
   */
  public boolean addSimpleCodeEmitter(PassManagerBase pm, CodeGenOpt level,
                                      MachineCodeEmitter mce) {
    return true;
  }
}
