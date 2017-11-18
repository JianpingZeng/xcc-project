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
import backend.target.TargetInstrInfo;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.target.TargetMachine.CodeGenOpt;
import backend.target.TargetRegisterInfo;
import backend.value.BasicBlock;
import backend.value.Function;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHiddenApplicator;

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
        System.err.printf("%n%n%n==============%s===============%n", fn.getName());
        selectAllBasicBlocks(fn, mf, null, instrInfo);
        return false;
    }

    public void selectAllBasicBlocks(Function fn,
                                      MachineFunction mf,
                                      MachineModuleInfo mmi,
                                      TargetInstrInfo tii)
    {}

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

    /**
     * Prints out dump information when finishing codegen on specified BasicBlock.
     */
    private void finishBasicBlock()
    {
        if (Util.DEBUG)
        {
            System.err.println("Target-post-processed machine code:\n");
            mbb.dump();
        }
    }

    private void lowerArguments(BasicBlock llvmBB)
    {
        // TODO: 17-8-4
    }
}
