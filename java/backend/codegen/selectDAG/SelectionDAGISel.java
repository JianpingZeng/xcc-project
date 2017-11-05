package backend.codegen.selectDAG;
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

import backend.codegen.*;
import backend.support.LLVMContext;
import backend.target.TargetInstrInfo;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.target.TargetMachine.CodeGenOpt;
import backend.target.TargetRegisterInfo;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.TerminatorInst;
import tools.commandline.*;

import static backend.target.TargetMachine.CodeGenOpt.Default;
import static tools.commandline.Desc.desc;
import static tools.commandline.OptionHidden.Hidden;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * This is the common base class for target-specific DAG-based instruction
 * pattern selector.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class SelectionDAGISel extends MachineFunctionPass
{
    public static final BooleanOpt EnableFastISelVerbose =
            new BooleanOpt(
                    optionName("fast-isel-verbose"),
                    new OptionHiddenApplicator(Hidden),
                    desc("Enable verbose messages in the \"fast\" "
                            + "instruction selector"));

    public static final BooleanOpt EnableFastISelAbort =
            new BooleanOpt(optionName("fast-isel-abort"),
                    new OptionHiddenApplicator(Hidden),
                    desc("Enable about calls when \"fast\" instruction fails"));

    private TargetMachine tm;
    private TargetLowering tli;
    private FunctionLoweringInfo funcInfo;
    private MachineFunction mf;
    private MachineRegisterInfo machineRegInfo;
    private MachineBasicBlock mbb;
    private CodeGenOpt optLevel;

    public SelectionDAGISel(TargetMachine tm)
    {
        this(tm, Default);
    }

    public SelectionDAGISel(TargetMachine tm, CodeGenOpt optLevel)
    {
        this.tm = tm;
        tli = tm.getTargetLowering();
        funcInfo = new FunctionLoweringInfo(tli);
        this.optLevel = optLevel;
    }

    public TargetLowering getTargetLowering() {return tli;}

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        //FIXME Current FastISel was performed when do instruction selection.
        Function fn = mf.getFunction();

        this.mf = mf;
        TargetInstrInfo instrInfo = tm.getInstrInfo();
        TargetRegisterInfo registerInfo = tm.getRegisterInfo();
        machineRegInfo = mf.getMachineRegisterInfo();

        // for debug.
        System.err.println("\n\n\n===" + fn.getName());


        return false;
    }

    public void selectAllBasicBlocks(Function fn,
                                      MachineFunction mf,
                                      MachineModuleInfo mmi,
                                      TargetInstrInfo tii)
    {
        FastISel fastISel = tli.createFastISel(mf,
                mmi,
                funcInfo.valueMap,
                funcInfo.mbbmap,
                funcInfo.staticAllocaMap);

        // Iterate over all basic blocks in the function.
        for (BasicBlock llvmBB : fn.getBasicBlockList())
        {
            mbb = funcInfo.mbbmap.get(llvmBB);

            int begin = 0, end = llvmBB.size();
            int bi = begin;

            boolean suppressFastISel = false;
            // Lower any arguments needed in this block if this is entry block.
            if (llvmBB.equals(fn.getEntryBlock()))
            {
                lowerArguments(llvmBB);
            }

            // Do FastISel on as many instructions as possible.
            fastISel.startNewBlock(mbb);
            for (; bi != end; ++bi)
            {
                if (llvmBB.getInstAt(bi) instanceof TerminatorInst)
                {
                    if (!handlePHINodesInSuccessorBlocksFast(llvmBB, fastISel))
                    {
                        if (EnableFastISelVerbose.value || EnableFastISelAbort.value)
                        {
                            System.err.printf("FastISel miss: ");
                            llvmBB.getInstAt(bi).dump();
                        }
                        if (EnableFastISelAbort.value)
                            assert false:"FastISel didn't handle a PHI in a successor";
                        break;
                    }
                }

                // First try normal tablegen-generated "fast" selection.
                if (fastISel.selectInstruction(llvmBB.getInstAt(bi)))
                    continue;

                // Next, try calling the target to attempt to handle the instruction.
                if (fastISel.targetSelectInstruction(llvmBB.getInstAt(bi)))
                    continue;

                // Then handle certain instructions as single-LLVM-Instruction blocks.
                if (llvmBB.getInstAt(bi) instanceof CallInst)
                {
                    if (EnableFastISelVerbose.value || EnableFastISelAbort.value)
                    {
                        System.err.printf("FastISel missing call: ");
                        llvmBB.getInstAt(bi).dump();
                    }

                    if (!llvmBB.getInstAt(bi).getType().equals(LLVMContext.VoidTy))
                    {
                        Instruction inst = llvmBB.getInstAt(bi);
                        if (!funcInfo.valueMap.containsKey(inst))
                            funcInfo.valueMap.put(inst, funcInfo.createRegForValue(inst));
                    }

                    selectBasicBlock(llvmBB, bi, bi+1);
                    fastISel.setCUrrentBlock(mbb);
                    continue;
                }

                // Otherwise, give up on FastISel for the rest of the block.
                // For now, be a little lenient about non-branch terminators.
                Instruction inst = llvmBB.getInstAt(bi);
                if (!(inst instanceof TerminatorInst) || inst instanceof BranchInst)
                {
                    if (EnableFastISelVerbose.value || EnableFastISelAbort.value)
                    {
                        System.err.printf("FastISel missing call: ");
                        inst.dump();
                    }
                    if (EnableFastISelAbort.value)
                    {
                        assert false:"FastISel didn't select the entire block";
                    }
                }
                break;
            }

            if (bi != end)
            {
                assert false:"Must run SelectionDAG instruction selection on "
                        + "the remainder of the block not handled by FastISel";
            }
            finishBasicBlock();
        }
    }

    /**
     * This is the Fast-ISel version of HandlePHINodesInSuccessorBlocks. It only
     * supports legal types, and it emits MachineInstrs directly instead of
     * creating SelectionDAG nodes.
     * @param bb
     * @param fastISel
     * @return
     */
    private boolean handlePHINodesInSuccessorBlocksFast(
            BasicBlock bb,
            FastISel fastISel)
    {
        // TODO: 17-8-4
        return false;
    }

    private void selectBasicBlock(BasicBlock bb, int bi, int nextBI)
    {
        // TODO: 17-8-4
    }

    private void finishBasicBlock()
    {
        // TODO: 17-8-4
    }

    private void lowerArguments(BasicBlock llvmBB)
    {
        // TODO: 17-8-4
    }
}
