package backend.target;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.codegen.MachineCodeEmitter;
import backend.codegen.MachineFunctionAnalysis;
import backend.passManaging.PassManagerBase;
import backend.support.BackendCmdOptions;

import java.io.OutputStream;

import static backend.codegen.MachineCodeVerifier.createMachineVerifierPass;
import static backend.codegen.PrintMachineFunctionPass.createMachineFunctionPrinterPass;
import static backend.codegen.PrologEpilogInserter.createPrologEpilogEmitter;
import static backend.target.TargetOptions.PrintMachineCode;
import static backend.target.TargetOptions.VerifyMachineCode;
import static backend.transform.scalars.LowerSwitch.createLowerSwitchPass;
import static backend.transform.scalars.UnreachableBlockElim.createUnreachableBlockEliminationPass;

/**
 * This class describes a target machine that is
 * implemented with the LLVM target-independent code generator.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class LLVMTargetMachine extends TargetMachine
{
    protected LLVMTargetMachine(Target target, String triple)
    {
        super(target);
        initAsmInfo(target, triple);
    }

    private void initAsmInfo(Target target, String triple)
    {
        asmInfo = target.createAsmInfo(triple);
        assert asmInfo != null:"Must initialize the TargetAsmInfo for AsmPrinter!";
    }

    private static void printAndVerify(PassManagerBase pm,
            boolean allowDoubleDefs, String banner)
    {
        if (PrintMachineCode.value)
            pm.add(createMachineFunctionPrinterPass(System.err, banner));

        if (VerifyMachineCode.value)
            pm.add(createMachineVerifierPass(allowDoubleDefs));
    }

    private static void printAndVerify(
            PassManagerBase pm,
            boolean allowDoubleDefs)
    {
        if (PrintMachineCode.value)
            pm.add(createMachineFunctionPrinterPass(System.err));

        if (VerifyMachineCode.value)
            pm.add(createMachineVerifierPass(allowDoubleDefs));
    }

    /**
     * Add standard LLVM codegen passes used for both emitting to assembly file
     * or machine code output.
     * @param pm
     * @param level
     * @return
     */
    protected boolean addCommonCodeGenPasses(PassManagerBase pm, CodeGenOpt level)
    {
        // lowers switch instr into chained branch instr.
        pm.add(createLowerSwitchPass());

        if (level != CodeGenOpt.None)
        {
            // todo pm.add(createLoopStrengthReducePass(getTargetLowering()));
        }

        pm.add(createUnreachableBlockEliminationPass());

        pm.add(new MachineFunctionAnalysis(this, level));

        // Ask the target for an isel.
        if (addInstSelector(pm, level))
            return true;

        // print the machine instructions.
        printAndVerify(pm, true,
                "# *** IR dump after Instruction Selection ***:\n");

        if (addPreRegAlloc(pm, level))
        {
            printAndVerify(pm, true,
                    "# *** IR dump after Pre-Register allocation ***:\n");
            return true;
        }

        // Perform register allocation to convert to a concrete x86 representation
        pm.add(BackendCmdOptions.createRegisterAllocator());

        // Print machine code after register allocation.
        printAndVerify(pm, false,
                "# *** IR dump after Register Allocator ***:\n");

        if (addPostRegAlloc(pm, level))
        {
            return true;
        }

        pm.add(createPrologEpilogEmitter());
        printAndVerify(pm, false,
                "# *** IR Dump After Prologue/Epilogue Insertion & Frame Finalization ***:\n");
        return false;
    }

    @Override
    public FileModel addPassesToEmitFile(PassManagerBase pm,
            OutputStream asmOutStream, CodeGenFileType fileType,
            CodeGenOpt optLevel)
    {
        if (addCommonCodeGenPasses(pm, optLevel))
            return FileModel.Error;

        if (PrintMachineCode.value)
        {
            pm.add(createMachineFunctionPrinterPass(System.err,
                    "# *** IR dump before emitting code ***:\n"));
        }

        if (addPreEmitPass(pm, optLevel) && PrintMachineCode.value)
        {
            pm.add(createMachineFunctionPrinterPass(System.err,
                    "# *** IR dump after emitting code ***:\n"));
            return FileModel.Error;
        }

        switch (fileType)
        {
            default: return FileModel.Error;
            case AssemblyFile:
                if (addAssemblyEmitter(pm, optLevel, theTarget.getAsmVerbosityDefault(), asmOutStream))
                    return FileModel.Error;
                return FileModel.AsmFile;
            case ObjectFile:
                return FileModel.ElfFile;
        }
    }

    @Override
    public boolean addPassesToEmitFileFinish(PassManagerBase pm,
            MachineCodeEmitter mce, CodeGenOpt opt)
    {
        if (mce != null)
            addSimpleCodeEmitter(pm, opt, mce);
        // success!
        return false;
    }

    public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level)
    {
        return false;
    }

    public boolean addPreRegAlloc(PassManagerBase pm, CodeGenOpt level)
    {
        return false;
    }

    public boolean addPostRegAlloc(PassManagerBase pm, CodeGenOpt level)
    {
        return false;
    }

    public boolean addPreEmitPass(PassManagerBase pm, CodeGenOpt level)
    {
        return false;
    }

    /**
     * This pass should be overridden by the target to add
     * a code emitter (without setting flags), if supported.  If this is not
     * supported, 'true' should be returned.
     * @param pm
     * @param level
     * @param mce
     * @return
     */
    public boolean addSimpleCodeEmitter(PassManagerBase pm, CodeGenOpt level,
            MachineCodeEmitter mce)
    {
        return true;
    }

    public boolean addAssemblyEmitter(PassManagerBase pm, CodeGenOpt level,
            boolean verbose,
            OutputStream os)
    {
        return true;
    }
}
