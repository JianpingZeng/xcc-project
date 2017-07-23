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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import gnu.trove.list.array.TIntArrayList;
import tools.OutParamWrapper;

import java.util.ArrayList;

/**
 * This is the default implementation of
 * TargetInstrInfo, which just provides a couple of default implementations
 * for various methods.
 * @author Xlous.zeng
 * @version 0.1
 */
public class TargetInstrInfoImpl extends TargetInstrInfo
{
    protected TargetInstrInfoImpl(TargetInstrDesc[] desc)
    {
        super(desc);
    }

    @Override
    public void reMaterialize(MachineBasicBlock mbb, int insertPos, int destReg,
            int subIdx, MachineInstr origin)
    {

    }

    public MachineInstr commuteInstruction(MachineInstr mi)
    {
        return commuteInstruction(mi, false);
    }

    @Override
    public MachineInstr commuteInstruction(MachineInstr mi, boolean newMI)
    {

        return null;
    }

    @Override public boolean findCommutedOpIndices(MachineInstr MI,
            OutParamWrapper<Integer> SrcOpIdx1,
            OutParamWrapper<Integer> SrcOpIdx2)
    {
        return false;
    }

    @Override
    public MachineInstr foldMemoryOperand(MachineFunction MF, MachineInstr MI,
            TIntArrayList Ops, int FrameIndex)
    {
        return null;
    }

    @Override
    public MachineInstr foldMemoryOperand(MachineFunction MF, MachineInstr MI,
            TIntArrayList Ops, MachineInstr LoadMI)
    {
        return null;
    }

    @Override public void insertNoop(MachineBasicBlock MBB, int pos)
    {

    }

    @Override public boolean isUnpredicatedTerminator(MachineInstr MI)
    {
        return false;
    }

    @Override public boolean PredicateInstruction(MachineInstr MI,
            ArrayList<MachineOperand> Pred)
    {
        return false;
    }

    @Override public boolean isDeadInstruction(MachineInstr MI)
    {
        return false;
    }

    @Override public int GetFunctionSizeInBytes(MachineFunction MF)
    {
        return 0;
    }
}
