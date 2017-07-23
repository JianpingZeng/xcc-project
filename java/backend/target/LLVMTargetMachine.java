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
import backend.pass.PassManagerBase;

import java.io.OutputStream;

import static backend.codegen.LocalRegAllocator.createLocalRegAllocator;
import static backend.codegen.RegAllocSimple.createSimpleRegAllocator;
import static backend.target.x86.PEI.createPrologEpilogEmitter;
import static backend.target.x86.X86PeepholeOptimizer.createX86PeepholeOptimizer;
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

        if (addPreRegAlloc(pm, level))
            return true;


        // Perform register allocation to convert to a concrete x86 representation
        if (level == CodeGenOpt.None)
            pm.add(createSimpleRegAllocator());
        else
            pm.add(createLocalRegAllocator());

        if (addPostRegAlloc(pm, level))
            return true;

        pm.add(createPrologEpilogEmitter());
        pm.add(createX86PeepholeOptimizer());

        return false;
    }

    @Override
    public FileModel addPassesToEmitFile(PassManagerBase pm,
            OutputStream asmOutStream, CodeGenFileType fileType,
            CodeGenOpt optLevel)
    {
        if (addCommonCodeGenPasses(pm, optLevel))
            return FileModel.Error;

        if (addPreEmitPass(pm, optLevel))
            return FileModel.Error;

        switch (fileType)
        {
            default: return FileModel.Error;
            case AssemblyFile:
                if (addAssemblyEmitter(pm, optLevel, false, asmOutStream))
                    return FileModel.Error;
                return FileModel.AsmFile;
            case ObjectFile:
                return FileModel.ElfFile;
        }
    }

    public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level)
    {
        return true;
    }

    public boolean addPreRegAlloc(PassManagerBase pm, CodeGenOpt level)
    {
        return true;
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
