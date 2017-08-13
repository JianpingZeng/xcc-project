/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen;

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
    /*
    private static class TableEntry implements Comparable<TableEntry>
    {
        int from;
        int to;

        @Override
        public int compareTo(TableEntry o)
        {
            return from - o.from;
        }

        TableEntry(int from, int to)
        {
            this.from = from;
            this.to = to;
        }
    }

    // Map: A = B op C  into: ST(0) = ST(0) op ST(i)
    private static final TableEntry[] forwardST0Table = {
            new TableEntry(X86GenInstrNames.FpADD, X86GenInstrNames.FADDST0r),
            new TableEntry(X86GenInstrNames.FpDIV, X86GenInstrNames.FDIVST0r),
            new TableEntry(X86GenInstrNames.FpMUL, X86GenInstrNames.FMULST0r),
            new TableEntry(X86GenInstrNames.FpSUB, X86GenInstrNames.FSUBST0r),
            new TableEntry(X86GenInstrNames.FpUCOM, X86GenInstrNames.FUCOMr) };
    // Map: A = B op C  into: ST(0) = ST(i) op ST(0)
    private static final TableEntry[] reverseST0Table =
    {
            // commutative
        new TableEntry(X86GenInstrNames.FpADD, X86GenInstrNames.FADDST0r),
        new TableEntry(X86GenInstrNames.FpDIV, X86GenInstrNames.FDIVRST0r),
            // commutative
        new TableEntry(X86GenInstrNames.FpMUL, X86GenInstrNames.FMULST0r),
        new TableEntry(X86GenInstrNames.FpSUB, X86GenInstrNames.FSUBRST0r),
        new TableEntry(X86GenInstrNames.FpUCOM, ~0)
    };

    // ForwardSTiTable - Map: A = B op C  into: ST(i) = ST(0) op ST(i)
    private static final TableEntry[] forwardSTiTable =
    {
        new TableEntry(X86GenInstrNames.FpADD, X86GenInstrNames.FADDrST0),
        new TableEntry(X86GenInstrNames.FpDIV, X86GenInstrNames.FDIVRrST0),
        new TableEntry(X86GenInstrNames.FpMUL, X86GenInstrNames.FMULrST0),
        // commutative
        new TableEntry(X86GenInstrNames.FpSUB, X86GenInstrNames.FSUBRrST0),
        new TableEntry(X86GenInstrNames.FpUCOM, X86GenInstrNames.FUCOMr)
    };

    // Map: A = B op C  into: ST(i) = ST(i) op ST(0)
    private static final TableEntry[] reverseSTiTable =
    {
            // commutative
        new TableEntry(X86GenInstrNames.FpADD, X86GenInstrNames.FADDrST0),
        new TableEntry(X86GenInstrNames.FpDIV, X86GenInstrNames.FDIVrST0),
            // commutative
        new TableEntry(X86GenInstrNames.FpMUL, X86GenInstrNames.FMULrST0),
        // commutative
        new TableEntry(X86GenInstrNames.FpSUB, X86GenInstrNames.FSUBrST0),
        new TableEntry(X86GenInstrNames.FpUCOM, ~0)
    };

    // ensure the table is sorted.
    private static boolean tableIsSorted(TableEntry[] tables)
    {
        for (int i = 0; i < tables.length; i++)
            if (tables[i].compareTo(tables[i+1]) >= 0)
                return false;

        return true;
    }
    private MachineBasicBlock mbb;
    private TargetMachine tm;
    /**
     * keeps track of mapping ST(x) (phyReg) to FP<n>(virReg) .
     *
    private int[] stack;
    /**
     * Mappings from FP(n) to ST(x).
     *
    private int[] virReg2PhyRegMap;
    /**
     * The current top of float stack.
     *
    private int stackTop;

    @Override
    public String getPassName(){return "X86 FP stackitifier pass.";}

    private int getFPReg(MachineOperand mo)
    {
        assert mo.isPhysicalRegister():"Expects a FP register!";
        int reg = mo.getMachineRegNum();
        assert reg>= X86GenRegisterNames.FP0 && reg <= X86GenRegisterNames.FP6
                : "Expects a FP register!";
        return reg - X86GenRegisterNames.FP0;
    }

    /***
     * Put the specified register FP<n> into the fp stack.
     * @param reg
     *
    private void pushReg(int reg)
    {
        assert reg < 8:"Register number out of range!";
        assert stackTop <= 8:"FP stack overflow!";
        stack[++stackTop] = reg;
        virReg2PhyRegMap[reg] = stackTop;
    }
    /**
     * Handle zero arg FP Inst which uses implicit ST(0) and a immediate number,
     * like FLD0, FLD1.
     * @param mi
     *
    private void handleZeroArgFPInst(MachineInstr mi)
    {
        int destReg = getFPReg(mi.getOperand(0));
        mi.removeOperand(0);

        // push result onto stack.
        pushReg(destReg);
    }

    private int getSlot(int regNo)
    {
        assert regNo < 8 :"regNo out of range!";
        return virReg2PhyRegMap[regNo];
    }

    private int getSTReg(int regNo)
    {
        return stackTop - getSlot(regNo) + X86GenRegisterNames.ST0;
    }

    /**
     * duplicate the content of regNo to the top of float stack.
     * @param regNo virtual floating point register to be assigned with ST(i).
     * @param asReg Push it onto the top of stack.
     * @param insertPos
     *
    private int duplicateToTop(int regNo, int asReg, int insertPos)
    {
        int stReg = getSTReg(regNo);
        pushReg(asReg);

        MachineInstr mi = buildMI(X86GenInstrNames.FLDrr, 1).addReg(stReg).getMInstr();
        mbb.insert(insertPos++, mi);
        return insertPos;
    }

    /**
     * Return the X86::FP<n> register in register ST(i).
     * @return
     *
    private int getStackEntry(int sti)
    {
        assert sti <= stackTop :" access out of range!";
        return stack[stackTop-sti];
    }

    private boolean isAtTop(int virReg)
    {
        return getSlot(virReg) == stackTop;
    }

    /**
     * Creates a FLD instruction to load the specified virReg (first assigned with
     * a ST(i) register) into the top of stack.
     * @param virReg
     * @param insertPos
     * @return
     *
    private int moveToTop(int virReg, int insertPos)
    {
        if (!isAtTop(virReg))
        {
            int slot = getSlot(virReg);
            int stReg = getSTReg(virReg);
            int regOnTop = getStackEntry(0);

            int tmp =  virReg2PhyRegMap[virReg];
            virReg2PhyRegMap[virReg] = virReg2PhyRegMap[regOnTop];
            virReg2PhyRegMap[regOnTop] = tmp;

            assert virReg2PhyRegMap[regOnTop] <= stackTop;
            tmp = stack[virReg2PhyRegMap[regOnTop]];
            stack[virReg2PhyRegMap[regOnTop]] = stack[stackTop];
            stack[stackTop] = tmp;

            // emit a fxch instr to update the runtime processors versio of
            // the status
            MachineInstr mi = buildMI(X86GenInstrNames.FXCH, 1).addReg(stReg).getMInstr();
            mbb.insert(insertPos++, mi);
        }
        return insertPos;
    }

    /**
     * Like fst ST(0), <mem>.
     * @param mi
     * @param idx
     *
    private int handleOneArgFPInst(MachineInstr mi, int idx)
    {
        assert mi.getNumOperands() == 5:"Can only handle fst ST(0) <mem> instr";

        // obtains the FP register.
        int reg = getFPReg(mi.getOperand(4));

        // FSTPr80 and FISTPr64 are strange because there are no non-popping versions.
        // If we have one _and_ we don't want to pop the operand, duplicate the value
        // on the stack instead of moving it.  This ensure that popping the value is
        // always ok.
        if (mi.getOpcode() == X86GenInstrNames.FSTPr80
                || mi.getOpcode() == X86GenInstrNames.FISTPr64)
        {
            idx = duplicateToTop(reg, 7, idx);
        }
        else
        {
            idx = moveToTop(reg, idx);
        }
        mi.removeOperand(4);

        if (mi.getOpcode() == X86GenInstrNames.FSTPr80
                || mi.getOpcode() == X86GenInstrNames.FISTPr64)
        {
            assert stackTop>= 0 :"Stack empty?";
            --stackTop;
        }
        return idx;
    }

    private int lookup(TableEntry[] instTable, int opcode)
    {
        for (int i = 0; i < instTable.length; i++)
            if(instTable[i].from == opcode)
                return instTable[i].to;
        return -1;
    }

    private int handleTwoArgFPInst(MachineInstr mi, int idx)
    {
        assert tableIsSorted(forwardST0Table):"";
        assert tableIsSorted(reverseST0Table):"";
        assert tableIsSorted(forwardSTiTable):"";
        assert tableIsSorted(reverseSTiTable):"";

        // save the index into mi for replacing mi with a new MI as follows.
        int indexOfMI = idx;

        int numOperands = mi.getNumOperands();
        assert numOperands == 3 || (numOperands == 2
                && mi.getOpcode() == X86GenInstrNames.FpUCOM):"Illegal twoArgsFP inst";

        int dest = getFPReg(mi.getOperand(0));
        int op0 = getFPReg(mi.getOperand(numOperands-2));
        int op1 = getFPReg(mi.getOperand(numOperands-1));

        // If this is an FpUCOM instruction, we must make sure the first operand is on
        // the top of stack, the other one can be anywhere...
        if (mi.getOpcode() == X86GenInstrNames.FpUCOM)
            idx = moveToTop(op0, idx);

        int tos = getStackEntry(0);

        // One of our operands must be on the top of the stack.  If neither is yet, we
        // need to move op0 to the top of stack.
        if (op0 != tos && op1 != tos)
        {
            idx = duplicateToTop(op0, dest, idx);
            op0 = tos = dest;
        }
        assert (tos == op0 || tos == op1) && mi.getOpcode() == X86GenInstrNames.FpUCOM;

        // We decide which form to use based on what is on the top of the stack.
        TableEntry[] instTable;
        boolean isForward = tos == op0;
        boolean updateST0 = dest == tos;
        if (updateST0)
        {
            if (isForward)
                instTable = forwardST0Table;
            else
                instTable = reverseST0Table;
        }
        else
        {
            if (isForward)
                instTable = forwardSTiTable;
            else
                instTable = reverseSTiTable;
        }

        int opcode = lookup(instTable, mi.getOpcode());
        assert opcode != -1 : "Unknown TwoArgFP pseudo instruction!";

        // nottos - The register which is not on the top of stack...
        int nottos = (tos == op0) ? op1 : op0;

        // Replace the old instruction with a new instruction
        MachineInstr newMI = buildMI(opcode, 1).addReg(getSTReg(nottos)).getMInstr();
        mbb.getInsts().set(indexOfMI, newMI);

        // Insert an explicit pop of the "updated" operand for FUCOM 
        if (mi.getOpcode() == X86InstrInfo.FpUCOM)
        {
            // Otherwise, move the top of stack into the dead slot, killing the
            // operand without having to add in an explicit xchg then pop.
            //
            int stReg    = getSTReg(op1);
            int oldSlot  = getSlot(op1);
            int topReg   = stack[stackTop];
            stack[oldSlot]    = topReg;
            virReg2PhyRegMap[topReg]    = oldSlot;
            virReg2PhyRegMap[op1]       = ~0;
            stack[stackTop--] = ~0;

            newMI = buildMI(X86GenInstrNames.FSTPrr, 1).addReg(stReg).getMInstr();
            mbb.insert(idx++, newMI);
        }

        // Update stack information so that we know the destination register is now on
        // the stack.
        if (mi.getOpcode() != X86GenInstrNames.FpUCOM)
        {
            int updatedSlot = getSlot(updateST0 ? tos : nottos);
            assert(updatedSlot <= stackTop && dest < 7);
            stack[updatedSlot] = dest;
            virReg2PhyRegMap[dest] = updatedSlot;
        }
        return idx;
    }

    /**
     * Handle special instruction which behave unlike other floating point
     * instruction. This is primarily intended for use by pseudo
     * instruction.
     * @param mi
     * @param idx
     * @return
     *
    private int handleSpecialFPInst(MachineInstr mi, int idx)
    {
        int oldIdx = idx;
        switch (mi.getOpcode())
        {
            default: assert false:"Unknwon SpecialFP instruction!";
            case X86GenInstrNames.FpGETRESULT:
                assert stackTop == -1:"Stack should be empty after a call";
                pushReg(getFPReg(mi.getOperand(0)));
                break;
            case X86GenInstrNames.FpSETRESULT:
                assert stackTop == 0:"Stack shoudl have one element on it for return ";
                --stackTop;
                break;
            case X86GenInstrNames.FpMOV:
            {
                // For FpMov we jsut duplicate the specified value
                // to a new stack slot.
                int srcReg = getFPReg(mi.getOperand(1));
                int destReg = getFPReg(mi.getOperand(0));
                idx = duplicateToTop(srcReg, destReg, idx);
                break;
            }
        }
        // remove this pseudo instruction.
        mbb.erase(oldIdx);
        --idx;
        return idx;
    }

    /**
     * Process a single machine basic block and loop over all machine instruction
     * contained in it.
     * @param mbb
     * @return
     *
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
            int flags = instInfo.get(mi.getOpcode()).tSFlags;

            // skips non-FP instruction.
            if ((flags & X86II.FPTypeMask) == 0)
                continue;

            switch (flags & X86II.FPTypeMask)
            {
                case X86II.ZeroArgFP:
                    handleZeroArgFPInst(mi);
                    break;
                case X86II.OneArgFP:
                    i = handleOneArgFPInst(mi, i);
                case X86II.OneArgFPRW:
                    assert false:"Currently not be supported!";
                    break;
                case X86II.TwoArgFP:
                    i = handleTwoArgFPInst(mi, i);
                    break;
                case X86II.SpecialFP:
                    i = handleSpecialFPInst(mi, i);
                    break;
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
     *
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        stackTop = -1;
        boolean changed = false;
        tm = mf.getTarget();

        for (MachineBasicBlock mbb : mf.getBasicBlocks())
            changed |= processMachineBasicBlock(mbb);
        return changed;
    }

    private FloatPointStackitifierPass()
    {
        stack = new int[8];
        virReg2PhyRegMap = new int[8];
    }*/

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

    @Override public String getPassName()
    {
        return null;
    }

    @Override public boolean runOnMachineFunction(MachineFunction mf)
    {
        return false;
    }
}
