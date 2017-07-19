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
import backend.value.Function;

import static backend.target.TargetMachine.CodeGenOpt.Default;

/**
 * This is the common base class for target-specific DAG-based instruction
 * pattern selector.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class SelectionDAGISel extends MachineFunctionPass
{
    private TargetMachine tm;
    private TargetLowering tli;
    private FunctionLoweringInfo funcInfo;
    private MachineFunction mf;
    private MachineRegisterInfo machineRegInfo;
    private MachineBasicBlock mbb;
    CodeGenOpt optLevel;

    public SelectionDAGISel(TargetMachine tm)
    {
        this(tm, Default);
    }

    public SelectionDAGISel(TargetMachine tm, CodeGenOpt optLevel)
    {
        this.tm = tm;
        tli = tm.getTargetLowering();
        funcInfo = new FunctionLoweringInfo();
        this.optLevel = optLevel;
    }

    public int makeReg(EVT vt)
    {

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
        FastISel fastISel = tli.createFastISel();
    }
}
