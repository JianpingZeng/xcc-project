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
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;

/**
 * This file defines a pass named of <emp>FloatPointStackitifierPass</emp>,
 * which responsible of converting virtual float register of X86 FP instr to
 * concrete float stack slot.
 *
 * @see MachineFunctionPass
 * @author Xlous.zeng
 * @version 0.1
 */
public class FloatPointStackitifierPass extends MachineFunctionPass
{
    private MachineBasicBlock mbb;
    private TargetMachine tm;
    /**
     * keeps track of FP<n>(virReg) mapping to ST(x) (phyReg).
     */
    private int[] stack;
    /**
     * Mappings from ST(x) to FP<n>.
     */
    private int[] regMap;
    /**
     * The current top of float stack.
     */
    private int stackTop;

    @Override
    public String getPassName(){return "X86 FP stackitifier pass.";}

    private int getFPReg(MachineOperand mo)
    {
        assert mo.isPhysicalRegister():"Expects a FP register!";
        int reg = mo.getMachineRegNum();
        assert reg>= X86RegsSet.FP0 && reg <= X86RegsSet.FP6
                : "Expects a FP register!";
        return reg - X86RegsSet.FP0;
    }

    /***
     * Put the specified register FP<n> into the fp stack.
     * @param reg
     */
    private void pushReg(int reg)
    {
        assert reg < 8:"Register number out of range!";
        assert stackTop <= 8:"FP stack overflow!";
        stack[++stackTop] = reg;
        regMap[reg] = stackTop;
    }
    /**
     * Handle zero arg FP Inst which uses implicit ST(0) and a immediate number,
     * like FLD0, FLD1.
     * @param mi
     */
    private void handleZeroArgFPInst(MachineInstr mi)
    {
        int destReg = getFPReg(mi.getOperand(0));
        mi.removeOperand(0);

        // push result onto stack.
        pushReg(destReg);
    }

    /**
     * Like fst ST(0), r32/<mem>.
     * @param mi
     * @param idx
     */
    private void handleOneArgFPInst(MachineInstr mi, int idx)
    {
        assert mi.getNumOperands() == 5:"";
    }

    private void handleTwoArgFPInst(MachineInstr mi, int idx)
    {

    }

    private void handleSpecialFPInst(MachineInstr mi, int idx)
    {

    }

    /**
     * Process a single machine basic block and loop over all machine instruction
     * contained in it.
     * @param mbb
     * @return
     */
    private boolean processMachineBasicBlock(MachineBasicBlock mbb)
    {
        this.mbb = mbb;
        boolean changed = false;
        TargetInstrInfo instInfo = tm.getInstrInfo();

        // loop over all FP machine instruction contained in mbb.
        // Note that foreach grammar is not suitable here since there is
        // inst removed and inserted.
        for (int i = 0; i < mbb.size(); i++)
        {
            MachineInstr mi = mbb.getInstAt(i);
            MachineInstr prevMI = i == 0? null: mbb.getInstAt(i-1);
            int flags = instInfo.get(mi.getOpCode()).tSFlags;

            // skips non-FP instruction.
            if ((flags & X86II.FPTypeMask) == 0)
                continue;

            switch (flags & X86II.FPTypeMask)
            {
                case X86II.ZeroArgFP:
                    handleZeroArgFPInst(mi);
                    break;
                case X86II.OneArgFP:
                case X86II.TwoArgFP:
                case X86II.OneArgFPRW:
                case X86II.SpecialFP:
                    default:
                        assert false:"Unknown FP inst!";
            }
            changed = true;
        }

        assert stackTop == -1:"Stack not empty at end of basic block?";
        return changed;
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
        stackTop = -1;
        boolean changed = false;
        tm = mf.getTargetMachine();

        for (MachineBasicBlock mbb : mf.getBasicBlocks())
            changed |= processMachineBasicBlock(mbb);
        return changed;
    }

    private FloatPointStackitifierPass()
    {
        stack = new int[8];
        regMap = new int[8];
    }

    /**
     * This method is a static factory method used for creating an instance of
     * class {@linkplain FloatPointStackitifierPass}.
     * @return
     */
    public static FloatPointStackitifierPass
        createX86FloatingPointStackitifierPass()
    {
        return new FloatPointStackitifierPass();
    }
}
