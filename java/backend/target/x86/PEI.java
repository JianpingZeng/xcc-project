package backend.target.x86;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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
import backend.pass.FunctionPass;
import backend.target.TargetFrameInfo;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.target.TargetRegisterClass;
import gnu.trove.list.array.TIntArrayList;
import tools.Util;

import static backend.target.TargetFrameInfo.StackDirection.StackGrowDown;
import static backend.target.TargetRegisterInfo.FirstVirtualRegister;

/**
 * This class responsible for finalizing the stack frame layout and emits
 * code for inserting prologue and epilogue code.
 *
 * Another important subtask is eliminating abstract frame index reference.
 *
 * <emp>Note that</emp> this pass must be runned after executing machine
 * instruction selector.
 * @author Xlous.zeng
 * @version 0.1
 */
public class PEI extends MachineFunctionPass
{
    @Override
    public String getPassName()
    {
        return "Prolog/Epilog Insertion & Frame Finalization";
    }

    /**
     * This method must be overridded by concrete subclass for performing
     * desired machine code transformation or analysis.
     *
     * @param mf
     * @return
     */
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        saveCalleeSavedRegisters(mf);

        // ALlow target machine to make final modification to the function
        // before the frame layout is finalized.
        mf.getTarget().getRegisterInfo().processFunctionBeforeFrameFinalized(mf);

        // Calculate actual frame offsets for all of the stack object.
        calculateFrameObjectOffsets(mf);

        // add prolog and epilog code to the function.
        insertPrologEpilogCode(mf);

        // Replace all MO_FrameIndex operands with physical register
        // references and actual offsets.
        replaceFrameIndices(mf);
        return true;
    }

    /**
     * Scans the entirely function for finding modified callee-saved registers,
     * and inserts spill and restore code for them when desired. Also calculate
     * the MaxFrameSize and hasCall variable for function's frame information
     * and eliminating call frame pseudo instructions.
     * @param mf
     */
    private void saveCalleeSavedRegisters(MachineFunction mf)
    {
        TargetRegisterInfo regInfo = mf.getTarget().getRegisterInfo();
        TargetFrameInfo frameInfo = mf.getTarget().getFrameInfo();

        int[] calleeSavedRegs = regInfo.getCalleeRegisters();
        int frameSetupOpcode = regInfo.getCallFrameSetupOpcode();
        int frameDestroyOpcode = regInfo.getCallFrameDestroyOpcode();

        // If the target machine don't support frame setup/destroy pseudo instr,
        // or there is no callee-saved registers.
        // return early.
        if ((calleeSavedRegs == null || calleeSavedRegs.length == 0)
                && frameSetupOpcode == -1 && frameDestroyOpcode == -1)
            return;

        long maxFrameSize = 0;
        boolean hasCall = false;

        // an array for holding which physical register is modified.
        boolean[] modifiedRegs = new boolean[FirstVirtualRegister];
        // walking through all machine basic block for computing maxFrameSize.
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            // mbb.getNumOfSubLoop() must be called each loop since there are maybe instr insert
            // and deletion.
            for (int i = 0; i < mbb.size(); i++)
            {
                MachineInstr mi = mbb.getInstAt(i);
                if (mi.getOpcode() == frameSetupOpcode
                        || mi.getOpcode() == frameDestroyOpcode)
                {
                    // eliminates frame setup/destroy pseudo instruction with
                    // concrete inst supported in the specified target.
                    assert mi.getNumOperands() == 1:"Call frame setup/destroy"
                            + " pseudo instruction must have a single immediate value";
                    long size = mi.getOperand(0).getImm();
                    maxFrameSize = Math.max(maxFrameSize, size);
                    hasCall = true;
                    regInfo.eliminateCallFramePseudoInstr(mf, mbb, i);
                }
                else
                {
                    for (int j = 0, sz = mi.getNumOperands(); j < sz; j++)
                    {
                        MachineOperand mo = mi.getOperand(j);
                        if (mo.isPhysicalRegister() && (mo.opIsDefAndUse()
                                || mo.opIsDef()))
                        {
                            // this register is modified.
                            modifiedRegs[mo.getReg()] = true;
                        }
                    }
                }
            }
        }

        MachineFrameInfo mfi = mf.getFrameInfo();
        mfi.setStackSize((int)maxFrameSize);
        mfi.setHasCalls(hasCall);

        // emit code for spill callee-saved registers.
        TIntArrayList regsToSaved = new TIntArrayList();
        for (int reg : calleeSavedRegs)
        {
            if (modifiedRegs[reg])
                regsToSaved.add(reg);
            else
            {
                // check to see if there are alias register were modified.
                if (regInfo.get(reg).subRegs != null)
                {
                    for (int subReg : regInfo.get(reg).subRegs)
                        if (modifiedRegs[subReg])
                            regsToSaved.add(subReg);
                }
                if (regInfo.get(reg).superRegs != null)
                {
                    for (int superReg : regInfo.get(reg).superRegs)
                        if (modifiedRegs[superReg])
                            regsToSaved.add(superReg);
                }
            }
        }

        // early exit if there is no callee register to be saved.
        if (regsToSaved.isEmpty())
            return;

        // Now that we know which register should be saved into stack slot,
        // allocate a new slot for them.
        TIntArrayList stackSlots = new TIntArrayList();
        MachineBasicBlock entry = mf.getEntryBlock();
        int insertPos = 0;
        for (int i = 0, e = regsToSaved.size(); i < e; i++)
        {
            int reg = regsToSaved.get(i);
            TargetRegisterClass rc = regInfo.getRegClass(reg);
            int frameIdx = mfi.createStackObject(rc);
            stackSlots.add(frameIdx);

            // Now that we have a stack slot for each register to be saved, insert spill
            // code into the entry block.
            insertPos = regInfo.storeRegToStackSlot(entry, insertPos, reg, frameIdx, rc);
        }

        // emit code for storing data from stack slot to register in each exit block.
        TargetInstrInfo instrInfo = mf.getTarget().getInstrInfo();
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            // if the last instruction is return, add an epilogue.
            if (!mbb.isEmpty() && instrInfo.isReturn(mbb.getInsts().getLast().getOpcode()))
            {
                insertPos = mbb.size() - 1;
                for (int i = 0, e = regsToSaved.size(); i < e; i++)
                {
                    int reg = regsToSaved.get(i);
                    TargetRegisterClass rc = regInfo.getRegClass(reg);
                    int frameIdx = stackSlots.get(i);
                    // Insert in reverse order
                    insertPos = regInfo.loadRegFromStackSlot(mbb, insertPos, reg, frameIdx, rc);
                    --insertPos;
                }
            }
        }
    }

    /**
     * Calculate actual frame offsets for all of the stack object.
     * @param mf
     */
    private void calculateFrameObjectOffsets(MachineFunction mf)
    {
        TargetFrameInfo tfi = mf.getTarget().getFrameInfo();
        boolean stackGrowDown = tfi.getStackGrowDirection() == StackGrowDown;

        assert stackGrowDown :"Only tested on stack down growing machien!";

        MachineFrameInfo mfi = mf.getFrameInfo();
        int stackAlign = tfi.getStackAlignment();
        int offset = tfi.getLocalAreaOffset();
        for (int i = 0, e = mfi.getObjectIndexEnd(); i< e; i++)
        {
            offset += mfi.getObjectSize(i);

            int align = mfi.getObjectAlignment(i);
            assert align <= stackAlign: "Cannot align stack object to higher "
                + "alignment boundary than the stack itself!";

            // add space padding.
            offset = Util.roundUp(offset, align);
            mfi.setObjectOffset(i, offset);
        }

        // align the final stack pointer offset.
        offset = Util.roundUp(offset, stackAlign);

        // set the final getNumOfSubLoop of the current function stack frame.
        mfi.setStackSize(offset-tfi.getLocalAreaOffset());
    }

    /**
     * Emits code for inserting prologue and epilogue code to mf.
     * @param mf
     */
    private void insertPrologEpilogCode(MachineFunction mf)
    {
        TargetRegisterInfo regInfo = mf.getTarget().getRegisterInfo();
        regInfo.emitPrologue(mf);

        TargetInstrInfo instInfo = mf.getTarget().getInstrInfo();
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            if (!mbb.isEmpty() && instInfo.isReturn(mbb.getInsts().getLast().getOpcode()))
                regInfo.emitEpilogue(mf, mbb);
        }
    }

    /**
     * Replace all MO_FrameIndex operands with physical register
     * references and actual offsets.
     * @param mf
     */
    private void replaceFrameIndices(MachineFunction mf)
    {
        if (!mf.getFrameInfo().hasStackObjects())
            return;

        TargetMachine tm = mf.getTarget();
        assert tm.getRegisterInfo() != null;
        TargetRegisterInfo regInfo = tm.getRegisterInfo();
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            for (int i = 0; i < mbb.size(); i++)
            {
                MachineInstr mi = mbb.getInstAt(i);
                for (int j = 0; j < mi.getNumOperands(); j++)
                    if (mi.getOperand(j).isFrameIndex())
                        regInfo.eliminateFrameIndex(mf, mbb, i);
            }
        }
    }

    /**
     * Creates a pass used for emitting prologue and epilogue code to function.
     * Also eliminating abstract frame index with actual stack slot reference.
     * @return
     */
    public static FunctionPass createPrologEpilogEmitter()
    {
        return new PEI();
    }
}
