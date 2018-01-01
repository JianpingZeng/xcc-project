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

package backend.target.x86;

import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoop;
import backend.codegen.*;
import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.target.TargetInstrInfo;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.support.ErrorHandling.llvmReportError;
import static backend.target.x86.X86GenInstrNames.*;
import static backend.target.x86.X86GenRegisterInfo.RFP80RegisterClass;

/**
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86FloatingPointStackifier extends MachineFunctionPass
{
    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.setPreservesCFG();
        au.addRequired(EdgeBundles.class);
        au.addPreserved(MachineLoop.class);
        au.addPreserved(MachineDomTree.class);
        super.getAnalysisUsage(au);
    }

    static class LiveBundle
    {
        int mask;
        int fixCount;
        int[] fixStack;

        public LiveBundle()
        {
            mask = 0;
            fixCount = 0;
            fixStack = new int[8];
        }

        public boolean isFixed()
        {
            return mask == 0 || fixCount != 0;
        }
    }

    private EdgeBundles edgeBundles;
    private TargetInstrInfo tii;
    private MachineBasicBlock mbb;
    private int[] stack = new int[8];
    private int stackTop;
    private static final int NumFPRegs = 16;
    private int[] regMap = new int[NumFPRegs];
    private int numPendingSTs;
    private int[] pendingST = new int[8];

    private void dumpStack()
    {
        System.err.print("Stack contents:");
        for (int i = 0; i < stackTop; i++)
        {
            System.err.printf(" FP%d", stack[i]);
            assert regMap[stack[i]] == i : "Stack[] doesn't match regMap[]!";
        }
        for (int i = 0; i < numPendingSTs; i++)
            System.err.printf(", ST%d in FP%d", i, pendingST[i]);
        System.err.println();
    }

    private ArrayList<LiveBundle> liveBundles;

    private void setupBlockStack()
    {
        if (Util.DEBUG)
        {
            System.err.printf("%nSetting up live-ins for BB#%d derived from %s.%n",
                    mbb.getNumber(), mbb.getName());
        }
        stackTop = 0;
        LiveBundle bundle = liveBundles
                .get(edgeBundles.getBundles(mbb.getNumber(), false));
        if (bundle.mask == 0)
        {
            if (Util.DEBUG)
                System.err.println("Block has no FP live-ins.");
            return;
        }

        assert bundle.isFixed() : "Reached block before any predecessors";
        for (int i = bundle.fixCount; i > 0; --i)
        {
            mbb.addLiveIn(X86GenRegisterNames.ST0 + i - 1);
            if (Util.DEBUG)
            {
                System.err.printf("Live-in st(%d): %%FP%d%n", i - 1,
                        bundle.fixStack[i - 1]);
                pushReg(bundle.fixStack[i - 1]);
            }
        }

        adjustLiveRegs(calcLiveInMask(mbb), 0);
        if (Util.DEBUG)
            mbb.dump();
    }

    private void pushReg(int reg)
    {
        assert reg < NumFPRegs : "Register number out of range!";
        if (stackTop >= 8)
            llvmReportError("Stack overflow!");
        stack[stackTop] = reg;
        regMap[reg] = stackTop++;
    }

    public int getSlot(int regNo)
    {
        assert regNo < NumFPRegs : "Regno out of range";
        return regMap[regNo];
    }

    private boolean isLive(int regNo)
    {
        int slot = getSlot(regNo);
        return slot < stackTop && stack[slot] == regNo;
    }

    private int getScratchReg()
    {
        for (int i = NumFPRegs - 1; i >= 8; --i)
        {
            if (!isLive(i))
                return i;
        }
        Util.shouldNotReachHere("Ran out of scratch FP registers");
        return -1;
    }

    public boolean isScratchReg(int regNo)
    {
        return regNo > 8 && regNo < NumFPRegs;
    }

    public int getStackEntry(int sti)
    {
        if (sti >= stackTop)
            llvmReportError("Access past stack top!");
        return stack[stackTop - 1 - sti];
    }

    public int getSTReg(int regNo)
    {
        return stackTop - 1 - getSlot(regNo) + X86GenRegisterNames.ST0;
    }

    public boolean isAtTop(int regNo)
    {
        return getSlot(regNo) == stackTop - 1;
    }

    public static final IntStatistic NumFXCH = new IntStatistic("NumFXCH",
            "Number of fxch instruction inserted");
    public static final IntStatistic NumFP = new IntStatistic("NumFP",
            "Number of floating point instructions");

    public void moveToTop(int regNo, int insertPos)
    {
        if (isAtTop(regNo))
            return;

        int streg = getSTReg(regNo);
        int regOnTop = getStackEntry(0);

        // swap teh slots the regs are in.
        int t = regMap[regNo];
        regMap[regNo] = regMap[regOnTop];
        regMap[regOnTop] = t;

        if (regMap[regOnTop] >= stackTop)
            llvmReportError("Access past stack top!");
        t = stack[regMap[regOnTop]];
        stack[regMap[regOnTop]] = stack[stackTop - 1];
        stack[stackTop - 1] = t;

        // Emit an fxch to update the runtime processors version of the state.
        buildMI(mbb, insertPos, tii.get(XCH_F)).addReg(streg);
        NumFXCH.inc();
    }

    private void duplicateToTop(int regNo, int asReg, int mi)
    {
        int stReg = getSTReg(regNo);
        pushReg(asReg);
        buildMI(mbb, mi, tii.get(LD_Frr)).addReg(stReg);
    }

    private void duplicatePendingSTBeforeKill(int regNo, int mi)
    {
        for (int i = 0; i < numPendingSTs; i++)
        {
            if (pendingST[i] != regNo)
                continue;

            int sr = getScratchReg();
            duplicateToTop(regNo, sr, mi);
            pendingST[i] = sr;
        }
    }

    // Efficient lookup Table support.
    static class TableEntry implements Comparable<TableEntry>
    {
        int from;
        int to;

        public TableEntry(int from, int to)
        {
            this.from = from;
            this.to = to;
        }

        @Override
        public int compareTo(TableEntry o)
        {
            return from - o.from;
        }
    }

    private static final TableEntry[] popTable = {
            new TableEntry(ADD_FrST0, ADD_FPrST0),
            new TableEntry(DIVR_FrST0, DIVR_FPrST0),
            new TableEntry(DIV_FrST0, DIV_FPrST0),

            new TableEntry(IST_F16m, IST_FP16m),
            new TableEntry(IST_F32m, IST_FP32m),

            new TableEntry(MUL_FrST0, MUL_FPrST0),

            new TableEntry(ST_F32m, ST_FP32m),
            new TableEntry(ST_F64m, ST_FP64m), new TableEntry(ST_Frr, ST_FPrr),

            new TableEntry(SUBR_FrST0, SUBR_FPrST0),
            new TableEntry(SUB_FrST0, SUB_FPrST0),

            new TableEntry(UCOM_FIr, UCOM_FIPr),

            new TableEntry(UCOM_FPr, UCOM_FPPr),
            new TableEntry(UCOM_Fr, UCOM_FPr), };

    private static int lookup(TableEntry[] table, int opcode)
    {
        for (int i = 0; i < table.length; i++)
            if (table[i].from == opcode)
                return table[i].to;
        return -1;
    }

    private int popStackAfter(int pos)
    {
        MachineInstr mi = mbb.getInstAt(pos);
        if (stackTop == 0)
            llvmReportError("Can't pop empty stack!");
        regMap[stack[--stackTop]] = ~0;

        int opc = lookup(popTable, mi.getOpcode());
        if (opc != -1)
        {
            mi.setDesc(tii.get(opc));
            if (opc == UCOM_FPPr)
                mi.removeOperand(0);
            return pos;
        }
        else
        {
            buildMI(mbb, ++pos, tii.get(ST_FPrr)).addReg(X86GenRegisterNames.ST0);
            return pos;
        }
    }

    private int freeStackSlotAfter(int mi, int fpRegNo)
    {
        if (getStackEntry(0) == fpRegNo)
        {
            return popStackAfter(mi);
        }

        return freeStackSlotBefore(1 + mi, fpRegNo);
    }

    private int freeStackSlotBefore(int pos, int fpRegNo)
    {
        int stReg = getSTReg(fpRegNo);
        int oldSlot = getSlot(fpRegNo);
        int topReg = stack[stackTop - 1];
        stack[oldSlot] = topReg;
        regMap[topReg] = oldSlot;
        regMap[fpRegNo] = ~0;
        stack[--stackTop] = ~0;
        buildMI(mbb, pos, tii.get(ST_FPrr)).addReg(stReg);
        return pos;
    }

    private int calcLiveInMask(MachineBasicBlock mbb)
    {
        int mask = 0;
        for(TIntIterator itr = mbb.getLiveIns().iterator(); itr.hasNext(); )
        {
            int reg = itr.next() - X86GenRegisterNames.FP0;
            if(reg < 8)
                mask |= 1<<reg;
        }
        return mask;
    }

    private void adjustLiveRegs(int mask, int insertPos)
    {
        int defs = mask;
        int kills = 0;
        for (int i = 0; i < stackTop; i++)
        {
            int regNo = stack[i];
            if ((defs & (1 << regNo)) == 0)
                kills |= (1 << regNo);
            else
                defs &= ~(1 << regNo);
        }
        assert (kills & defs) == 0 : "Register needs killing and def's?";
        while (kills != 0 && defs != 0)
        {
            int kreg = Util.countTrailingZeros(kills);
            int dreg = Util.countTrailingZeros(defs);
            if (Util.DEBUG)
                System.err.printf("Renaming %%FP%d as imp %%FP%d\n", kreg, dreg);
            int t = stack[getSlot(kreg)];
            stack[getSlot(kreg)] = stack[getSlot(dreg)];
            stack[getSlot(dreg)] = t;

            t = regMap[kreg];
            regMap[kreg] = regMap[dreg];
            regMap[dreg] = t;
            kills &= ~(1 << kreg);
            defs &= ~(1 << dreg);
        }

        if (kills != 0 && insertPos != 0)
        {
            int prior = insertPos - 1;
            while (stackTop != 0)
            {
                int kreg = getStackEntry(0);
                if ((kills & (1 << kreg)) == 0)
                    break;
                if (Util.DEBUG)
                    System.err.printf("Popping %%FP%d\n", kreg);
                popStackAfter(prior);
                kills &= ~(1 << kreg);
            }
        }

        while (kills != 0)
        {
            int kreg = Util.countTrailingZeros(kills);
            freeStackSlotBefore(insertPos, kreg);
            kills &= ~(1 << kreg);
        }

        while (defs != 0)
        {
            int dreg = Util.countTrailingZeros(defs);
            buildMI(mbb, insertPos, tii.get(LD_F0));
            pushReg(dreg);
            defs &= ~(1 << dreg);
        }

        if (Util.DEBUG)
            dumpStack();
        assert stackTop == Util.countPoplutation(mask) : "Live count mismatch";
    }

    private void shuffleStackTop(int[] fixStack, int fixCount, int insertPos)
    {
        while (fixCount-- != 0)
        {
            int oldReg = getStackEntry(fixCount);
            int reg = fixStack[fixCount];
            if (reg == oldReg)
                continue;
            moveToTop(reg, insertPos);
            if (fixCount > 0)
                moveToTop(oldReg, insertPos);
        }
        if (Util.DEBUG)
            dumpStack();
    }

    private static int getFPReg(MachineOperand mo)
    {
        assert mo.isRegister():"Expected an FP register!";
        int reg = mo.getReg();
        assert reg >= X86GenRegisterNames.FP0 && reg <= X86GenRegisterNames.FP6:
                "Expected FP register!";
        return reg - X86GenRegisterNames.FP0;
    }

    /**
     *
     * Sorted map of register instructions to their stack version.
     * The first element is an register file pseudo instruction, the second is the
     * concrete X86 instruction which uses the register stack.
     */
    private static final TableEntry[] opcodeTable =
    {
        new TableEntry( ABS_Fp32     , ABS_F     ),
        new TableEntry( ABS_Fp64     , ABS_F     ),
        new TableEntry( ABS_Fp80     , ABS_F     ),
        new TableEntry( ADD_Fp32m    , ADD_F32m  ),
        new TableEntry( ADD_Fp64m    , ADD_F64m  ),
        new TableEntry( ADD_Fp64m32  , ADD_F32m  ),
        new TableEntry( ADD_Fp80m32  , ADD_F32m  ),
        new TableEntry( ADD_Fp80m64  , ADD_F64m  ),
        new TableEntry( ADD_FpI16m32 , ADD_FI16m ),
        new TableEntry( ADD_FpI16m64 , ADD_FI16m ),
        new TableEntry( ADD_FpI16m80 , ADD_FI16m ),
        new TableEntry( ADD_FpI32m32 , ADD_FI32m ),
        new TableEntry( ADD_FpI32m64 , ADD_FI32m ),
        new TableEntry( ADD_FpI32m80 , ADD_FI32m ),
        new TableEntry( CHS_Fp32     , CHS_F     ),
        new TableEntry( CHS_Fp64     , CHS_F     ),
        new TableEntry( CHS_Fp80     , CHS_F     ),
        new TableEntry( CMOVBE_Fp32  , CMOVBE_F  ),
        new TableEntry( CMOVBE_Fp64  , CMOVBE_F  ),
        new TableEntry( CMOVBE_Fp80  , CMOVBE_F  ),
        new TableEntry( CMOVB_Fp32   , CMOVB_F   ),
        new TableEntry( CMOVB_Fp64   , CMOVB_F  ),
        new TableEntry( CMOVB_Fp80   , CMOVB_F  ),
        new TableEntry( CMOVE_Fp32   , CMOVE_F  ),
        new TableEntry( CMOVE_Fp64   , CMOVE_F   ),
        new TableEntry( CMOVE_Fp80   , CMOVE_F   ),
        new TableEntry( CMOVNBE_Fp32 , CMOVNBE_F ),
        new TableEntry( CMOVNBE_Fp64 , CMOVNBE_F ),
        new TableEntry( CMOVNBE_Fp80 , CMOVNBE_F ),
        new TableEntry( CMOVNB_Fp32  , CMOVNB_F  ),
        new TableEntry( CMOVNB_Fp64  , CMOVNB_F  ),
        new TableEntry( CMOVNB_Fp80  , CMOVNB_F  ),
        new TableEntry( CMOVNE_Fp32  , CMOVNE_F  ),
        new TableEntry( CMOVNE_Fp64  , CMOVNE_F  ),
        new TableEntry( CMOVNE_Fp80  , CMOVNE_F  ),
        new TableEntry( CMOVNP_Fp32  , CMOVNP_F  ),
        new TableEntry( CMOVNP_Fp64  , CMOVNP_F  ),
        new TableEntry( CMOVNP_Fp80  , CMOVNP_F  ),
        new TableEntry( CMOVP_Fp32   , CMOVP_F   ),
        new TableEntry( CMOVP_Fp64   , CMOVP_F   ),
        new TableEntry( CMOVP_Fp80   , CMOVP_F   ),
        new TableEntry( COS_Fp32     , COS_F     ),
        new TableEntry( COS_Fp64     , COS_F     ),
        new TableEntry( COS_Fp80     , COS_F     ),
        new TableEntry( DIVR_Fp32m   , DIVR_F32m ),
        new TableEntry( DIVR_Fp64m   , DIVR_F64m ),
        new TableEntry( DIVR_Fp64m32 , DIVR_F32m ),
        new TableEntry( DIVR_Fp80m32 , DIVR_F32m ),
        new TableEntry( DIVR_Fp80m64 , DIVR_F64m ),
        new TableEntry( DIVR_FpI16m32, DIVR_FI16m),
        new TableEntry( DIVR_FpI16m64, DIVR_FI16m),
        new TableEntry( DIVR_FpI16m80, DIVR_FI16m),
        new TableEntry( DIVR_FpI32m32, DIVR_FI32m),
        new TableEntry( DIVR_FpI32m64, DIVR_FI32m),
        new TableEntry( DIVR_FpI32m80, DIVR_FI32m),
        new TableEntry( DIV_Fp32m    , DIV_F32m  ),
        new TableEntry( DIV_Fp64m    , DIV_F64m  ),
        new TableEntry( DIV_Fp64m32  , DIV_F32m  ),
        new TableEntry( DIV_Fp80m32  , DIV_F32m  ),
        new TableEntry( DIV_Fp80m64  , DIV_F64m  ),
        new TableEntry( DIV_FpI16m32 , DIV_FI16m ),
        new TableEntry( DIV_FpI16m64 , DIV_FI16m ),
        new TableEntry( DIV_FpI16m80 , DIV_FI16m ),
        new TableEntry( DIV_FpI32m32 , DIV_FI32m ),
        new TableEntry( DIV_FpI32m64 , DIV_FI32m ),
        new TableEntry( DIV_FpI32m80 , DIV_FI32m ),
        new TableEntry( ILD_Fp16m32  , ILD_F16m  ),
        new TableEntry( ILD_Fp16m64  , ILD_F16m  ),
        new TableEntry( ILD_Fp16m80  , ILD_F16m  ),
        new TableEntry( ILD_Fp32m32  , ILD_F32m  ),
        new TableEntry( ILD_Fp32m64  , ILD_F32m  ),
        new TableEntry( ILD_Fp32m80  , ILD_F32m  ),
        new TableEntry( ILD_Fp64m32  , ILD_F64m  ),
        new TableEntry( ILD_Fp64m64  , ILD_F64m  ),
        new TableEntry( ILD_Fp64m80  , ILD_F64m  ),
        new TableEntry( ISTT_Fp16m32 , ISTT_FP16m),
        new TableEntry( ISTT_Fp16m64 , ISTT_FP16m),
        new TableEntry( ISTT_Fp16m80 , ISTT_FP16m),
        new TableEntry( ISTT_Fp32m32 , ISTT_FP32m),
        new TableEntry( ISTT_Fp32m64 , ISTT_FP32m),
        new TableEntry( ISTT_Fp32m80 , ISTT_FP32m),
        new TableEntry( ISTT_Fp64m32 , ISTT_FP64m),
        new TableEntry( ISTT_Fp64m64 , ISTT_FP64m),
        new TableEntry( ISTT_Fp64m80 , ISTT_FP64m),
        new TableEntry( IST_Fp16m32  , IST_F16m  ),
        new TableEntry( IST_Fp16m64  , IST_F16m  ),
        new TableEntry( IST_Fp16m80  , IST_F16m  ),
        new TableEntry( IST_Fp32m32  , IST_F32m  ),
        new TableEntry( IST_Fp32m64  , IST_F32m  ),
        new TableEntry( IST_Fp32m80  , IST_F32m  ),
        new TableEntry( IST_Fp64m32  , IST_FP64m ),
        new TableEntry( IST_Fp64m64  , IST_FP64m ),
        new TableEntry( IST_Fp64m80  , IST_FP64m ),
        new TableEntry( LD_Fp032     , LD_F0     ),
        new TableEntry( LD_Fp064     , LD_F0     ),
        new TableEntry( LD_Fp080     , LD_F0     ),
        new TableEntry( LD_Fp132     , LD_F1     ),
        new TableEntry( LD_Fp164     , LD_F1     ),
        new TableEntry( LD_Fp180     , LD_F1     ),
        new TableEntry( LD_Fp32m     , LD_F32m   ),
        new TableEntry( LD_Fp32m64   , LD_F32m   ),
        new TableEntry( LD_Fp32m80   , LD_F32m   ),
        new TableEntry( LD_Fp64m     , LD_F64m   ),
        new TableEntry( LD_Fp64m80   , LD_F64m   ),
        new TableEntry( LD_Fp80m     , LD_F80m   ),
        new TableEntry( MUL_Fp32m    , MUL_F32m  ),
        new TableEntry( MUL_Fp64m    , MUL_F64m  ),
        new TableEntry( MUL_Fp64m32  , MUL_F32m  ),
        new TableEntry( MUL_Fp80m32  , MUL_F32m  ),
        new TableEntry( MUL_Fp80m64  , MUL_F64m  ),
        new TableEntry( MUL_FpI16m32 , MUL_FI16m ),
        new TableEntry( MUL_FpI16m64 , MUL_FI16m ),
        new TableEntry( MUL_FpI16m80 , MUL_FI16m ),
        new TableEntry( MUL_FpI32m32 , MUL_FI32m ),
        new TableEntry( MUL_FpI32m64 , MUL_FI32m ),
        new TableEntry( MUL_FpI32m80 , MUL_FI32m ),
        new TableEntry( SIN_Fp32     , SIN_F     ),
        new TableEntry( SIN_Fp64     , SIN_F     ),
        new TableEntry( SIN_Fp80     , SIN_F     ),
        new TableEntry( SQRT_Fp32    , SQRT_F    ),
        new TableEntry( SQRT_Fp64    , SQRT_F    ),
        new TableEntry( SQRT_Fp80    , SQRT_F    ),
        new TableEntry( ST_Fp32m     , ST_F32m   ),
        new TableEntry( ST_Fp64m     , ST_F64m   ),
        new TableEntry( ST_Fp64m32   , ST_F32m   ),
        new TableEntry( ST_Fp80m32   , ST_F32m   ),
        new TableEntry( ST_Fp80m64   , ST_F64m   ),
        new TableEntry( ST_FpP80m    , ST_FP80m  ),
        new TableEntry( SUBR_Fp32m   , SUBR_F32m ),
        new TableEntry( SUBR_Fp64m   , SUBR_F64m ),
        new TableEntry( SUBR_Fp64m32 , SUBR_F32m ),
        new TableEntry( SUBR_Fp80m32 , SUBR_F32m ),
        new TableEntry( SUBR_Fp80m64 , SUBR_F64m ),
        new TableEntry( SUBR_FpI16m32, SUBR_FI16m),
        new TableEntry( SUBR_FpI16m64, SUBR_FI16m),
        new TableEntry( SUBR_FpI16m80, SUBR_FI16m),
        new TableEntry( SUBR_FpI32m32, SUBR_FI32m),
        new TableEntry( SUBR_FpI32m64, SUBR_FI32m),
        new TableEntry( SUBR_FpI32m80, SUBR_FI32m),
        new TableEntry( SUB_Fp32m    , SUB_F32m  ),
        new TableEntry( SUB_Fp64m    , SUB_F64m  ),
        new TableEntry( SUB_Fp64m32  , SUB_F32m  ),
        new TableEntry( SUB_Fp80m32  , SUB_F32m  ),
        new TableEntry( SUB_Fp80m64  , SUB_F64m  ),
        new TableEntry( SUB_FpI16m32 , SUB_FI16m ),
        new TableEntry( SUB_FpI16m64 , SUB_FI16m ),
        new TableEntry( SUB_FpI16m80 , SUB_FI16m ),
        new TableEntry( SUB_FpI32m32 , SUB_FI32m ),
        new TableEntry( SUB_FpI32m64 , SUB_FI32m ),
        new TableEntry( SUB_FpI32m80 , SUB_FI32m ),
        new TableEntry( TST_Fp32     , TST_F     ),
        new TableEntry( TST_Fp64     , TST_F     ),
        new TableEntry( TST_Fp80     , TST_F     ),
        new TableEntry( UCOM_FpIr32  , UCOM_FIr  ),
        new TableEntry( UCOM_FpIr64  , UCOM_FIr  ),
        new TableEntry( UCOM_FpIr80  , UCOM_FIr  ),
        new TableEntry( UCOM_Fpr32   , UCOM_Fr   ),
        new TableEntry( UCOM_Fpr64   , UCOM_Fr   ),
        new TableEntry( UCOM_Fpr80   , UCOM_Fr   ),
    };

    private static int getConcreteOpcode(int opcode)
    {
        int opc = lookup(opcodeTable, opcode);
        assert opc != -1 :"FP stack instruction not in opcodeTable!";
        return opc;
    }

    private void handleZeroArgFP(int insertPos)
    {
        MachineInstr mi = mbb.getInstAt(insertPos);
        int destReg = getFPReg(mi.getOperand(0));
        mi.removeOperand(0);
        mi.setDesc(tii.get(getConcreteOpcode(mi.getOpcode())));
        pushReg(destReg);
    }

    private void handleOneArgFP(int insertPos)
    {
        MachineInstr mi = mbb.getInstAt(insertPos);
        int numOps = mi.getDesc().getNumOperands();
        assert numOps == X86.AddrNumOperands + 1 || numOps == 1:
                "Can only handle fst* & ftst instructions!";
        int reg = getFPReg(mi.getOperand(numOps-1));
        boolean killsSrc = mi.killsRegister(X86GenRegisterNames.FP0+reg);
        if (killsSrc)
            duplicatePendingSTBeforeKill(reg, insertPos);

        if (!killsSrc && (mi.getOpcode() == IST_Fp64m32
                || mi.getOpcode() == ISTT_Fp16m32
                || mi.getOpcode() == ISTT_Fp32m32
                || mi.getOpcode() == ISTT_Fp64m32
                || mi.getOpcode() == IST_Fp64m64
                || mi.getOpcode() == ISTT_Fp16m64
                || mi.getOpcode() == ISTT_Fp32m64
                || mi.getOpcode() == ISTT_Fp64m64
                || mi.getOpcode() == IST_Fp64m80
                || mi.getOpcode() == ISTT_Fp16m80
                || mi.getOpcode() == ISTT_Fp32m80
                || mi.getOpcode() == ISTT_Fp64m80
                || mi.getOpcode() == ST_FpP80m))
            duplicateToTop(reg, getScratchReg(), insertPos);
        else
            moveToTop(reg, insertPos);

        mi.removeOperand(numOps-1);
        mi.setDesc(tii.get(getConcreteOpcode(mi.getOpcode())));
        if (mi.getOpcode() == IST_FP64m ||
                mi.getOpcode() == ISTT_FP16m ||
                mi.getOpcode() == ISTT_FP32m ||
                mi.getOpcode() == ISTT_FP64m ||
                mi.getOpcode() == ST_FP80m)
        {
            if (stackTop == 0)
                llvmReportError("Stack empty");
            --stackTop;
        }
        else if (killsSrc)
            insertPos = popStackAfter(insertPos);
    }

    private void handleOneArgFPRW(int insertPos)
    {
        MachineInstr mi = mbb.getInstAt(insertPos);
        int reg = getFPReg(mi.getOperand(1));
        boolean killsSrc = mi.killsRegister(X86GenRegisterNames.FP0 + reg);
        if (killsSrc)
        {
            duplicatePendingSTBeforeKill(reg, insertPos);
            moveToTop(reg, insertPos);
            if (stackTop == 0)
                llvmReportError("Stack can't be empty!");
            --stackTop;
            pushReg(getFPReg(mi.getOperand(0)));
        }
        else
        {
            duplicateToTop(reg, getFPReg(mi.getOperand(0)), insertPos);
        }

        mi.removeOperand(1);
        mi.removeOperand(0);
        mi.setDesc(tii.get(getConcreteOpcode(mi.getOpcode())));
    }

    // ForwardST0Table - Map: A = B op C  into: ST(0) = ST(0) op ST(i)
    private static TableEntry[] ForwardST0Table = {
            new TableEntry(ADD_Fp32, ADD_FST0r),
            new TableEntry(ADD_Fp64, ADD_FST0r),
            new TableEntry(ADD_Fp80, ADD_FST0r),
            new TableEntry(DIV_Fp32, DIV_FST0r),
            new TableEntry(DIV_Fp64, DIV_FST0r),
            new TableEntry(DIV_Fp80, DIV_FST0r),
            new TableEntry(MUL_Fp32, MUL_FST0r),
            new TableEntry(MUL_Fp64, MUL_FST0r),
            new TableEntry(MUL_Fp80, MUL_FST0r),
            new TableEntry(SUB_Fp32, SUB_FST0r),
            new TableEntry(SUB_Fp64, SUB_FST0r),
            new TableEntry(SUB_Fp80, SUB_FST0r), };

    // ReverseST0Table - Map: A = B op C  into: ST(0) = ST(i) op ST(0)
    private static TableEntry[] ReverseST0Table = {
            new TableEntry(ADD_Fp32, ADD_FST0r),   // commutative
            new TableEntry(ADD_Fp64, ADD_FST0r),   // commutative
            new TableEntry(ADD_Fp80, ADD_FST0r),   // commutative
            new TableEntry(DIV_Fp32, DIVR_FST0r),
            new TableEntry(DIV_Fp64, DIVR_FST0r),
            new TableEntry(DIV_Fp80, DIVR_FST0r),
            new TableEntry(MUL_Fp32, MUL_FST0r),   // commutative
            new TableEntry(MUL_Fp64, MUL_FST0r),   // commutative
            new TableEntry(MUL_Fp80, MUL_FST0r),   // commutative
            new TableEntry(SUB_Fp32, SUBR_FST0r),
            new TableEntry(SUB_Fp64, SUBR_FST0r),
            new TableEntry(SUB_Fp80, SUBR_FST0r), };

    // ForwardSTiTable - Map: A = B op C  into: ST(i) = ST(0) op ST(i)
    static TableEntry[] ForwardSTiTable = { new TableEntry(ADD_Fp32, ADD_FrST0),
            // commutative
            new TableEntry(ADD_Fp64, ADD_FrST0),   // commutative
            new TableEntry(ADD_Fp80, ADD_FrST0),   // commutative
            new TableEntry(DIV_Fp32, DIVR_FrST0),
            new TableEntry(DIV_Fp64, DIVR_FrST0),
            new TableEntry(DIV_Fp80, DIVR_FrST0),
            new TableEntry(MUL_Fp32, MUL_FrST0),   // commutative
            new TableEntry(MUL_Fp64, MUL_FrST0),   // commutative
            new TableEntry(MUL_Fp80, MUL_FrST0),   // commutative
            new TableEntry(SUB_Fp32, SUBR_FrST0),
            new TableEntry(SUB_Fp64, SUBR_FrST0),
            new TableEntry(SUB_Fp80, SUBR_FrST0), };

    // ReverseSTiTable - Map: A = B op C  into: ST(i) = ST(i) op ST(0)
    private static TableEntry[] ReverseSTiTable = {
            new TableEntry(ADD_Fp32, ADD_FrST0),
            new TableEntry(ADD_Fp64, ADD_FrST0),
            new TableEntry(ADD_Fp80, ADD_FrST0),
            new TableEntry(DIV_Fp32, DIV_FrST0),
            new TableEntry(DIV_Fp64, DIV_FrST0),
            new TableEntry(DIV_Fp80, DIV_FrST0),
            new TableEntry(MUL_Fp32, MUL_FrST0),
            new TableEntry(MUL_Fp64, MUL_FrST0),
            new TableEntry(MUL_Fp80, MUL_FrST0),
            new TableEntry(SUB_Fp32, SUB_FrST0),
            new TableEntry(SUB_Fp64, SUB_FrST0),
            new TableEntry(SUB_Fp80, SUB_FrST0), };
    
    private int handleTwoArgFP(int itr)
    {
        MachineInstr mi = mbb.getInstAt(itr);

        int numOperands = mi.getDesc().getNumOperands();
        assert numOperands == 3:"Illegal twoArgFP instruction!";
        int dest = getFPReg(mi.getOperand(0));
        int op0 = getFPReg(mi.getOperand(numOperands-2));
        int op1 = getFPReg(mi.getOperand(numOperands-1));
        boolean killsOp0 = mi.killsRegister(X86GenRegisterNames.FP0+op0);
        boolean killsOp1 = mi.killsRegister(X86GenRegisterNames.FP0+op1);

        int tos = getStackEntry(0);

        if (op0 != tos && tos != op1)
        {
            if (killsOp0)
            {
                moveToTop(op0, itr);
                tos = op0;
            }
            else if (killsOp1)
            {
                moveToTop(op1, itr);
                tos = op1;
            }
            else
            {
                duplicateToTop(op0, dest, itr);
                op0 = tos = dest;
                killsOp0 = true;
            }
        }
        else if (!killsOp0 && !killsOp1)
        {
            duplicateToTop(op0, dest, itr);
            op0 = tos = dest;
            killsOp0 = true;
        }

        assert (tos == op0 || tos == op1) && (killsOp0 || killsOp1)
                :"Stack conditions not set up right!";

        TableEntry[] instTable;
        boolean isForward = tos == op0;
        boolean updateST0 = (tos == op0 && !killsOp1) || (tos == op1 && !killsOp0);
        if (updateST0)
        {
            if (isForward)
                instTable = ForwardST0Table;
            else
                instTable = ReverseST0Table;
        }
        else
        {
            if (isForward)
                instTable = ForwardSTiTable;
            else
                instTable = ReverseSTiTable;
        }
        int opcode = lookup(instTable, mi.getOpcode());
        assert opcode != -1 :"Unknown TwoArgFP pseudo instruction!";
        int notTOS = (tos == op0) ? op1:op0;
        mbb.remove(itr);
        ++itr;
        buildMI(mbb, itr, tii.get(opcode)).addReg(getSTReg(notTOS));

        if (killsOp0 && killsOp1 && op0 != op1)
        {
            assert !updateST0:"Should have updated other operand!";
            popStackAfter(itr);
        }

        int updateSlot = getSlot(updateST0?tos:notTOS);
        assert updateSlot < stackTop && dest < 7;
        stack[updateSlot] = dest;
        regMap[dest] = updateSlot;
        mbb.eraseFromParent();
        return itr;
    }

    private int handleCompareFP(int itr)
    {
        MachineInstr mi = mbb.getInstAt(itr);
        int numOperands = mi.getDesc().getNumOperands();
        assert numOperands == 2:"Illegal PUCOM instruction!";
        int op0 = getFPReg(mi.getOperand(0));
        int op1 = getFPReg(mi.getOperand(1));
        boolean kiilsOp0 = mi.killsRegister(X86GenRegisterNames.FP0 +op0);
        boolean kiilsOp1 = mi.killsRegister(X86GenRegisterNames.FP0 +op1);

        moveToTop(op0, itr);

        mi.getOperand(0).setReg(getSTReg(op1));
        mi.removeOperand(1);
        mi.setDesc(tii.get(getConcreteOpcode(mi.getOpcode())));
        if (kiilsOp0) itr = freeStackSlotAfter(itr, op0);
        if (kiilsOp1 && op0 != op1) itr = freeStackSlotAfter(itr, op1);
        return itr;
    }

    private int handleCondMovFP(int itr)
    {
        MachineInstr mi = mbb.getInstAt(itr);
        int op0 = getFPReg(mi.getOperand(0));
        int op1 = getFPReg(mi.getOperand(2));
        boolean killsOp1 = mi.killsRegister(op1 + X86GenRegisterNames.FP0);

        moveToTop(op0, itr);

        mi.removeOperand(0);
        mi.removeOperand(1);
        mi.getOperand(0).setReg(getSTReg(op1));
        mi.setDesc(tii.get(getConcreteOpcode(mi.getOpcode())));

        if (op0 != op1 && killsOp1)
            itr = freeStackSlotAfter(itr, op1);
        return itr;
    }

    private int handleSpecialFP(int itr)
    {
        MachineInstr mi = mbb.getInstAt(itr);
        switch (mi.getOpcode())
        {
            case TargetInstrInfo.COPY:
            {
                MachineOperand mo1 = mi.getOperand(1);
                MachineOperand mo0 = mi.getOperand(0);
                int destST = mo0.getReg() - X86GenRegisterNames.ST0;
                int srcST = mo1.getReg() - X86GenRegisterNames.ST0;
                boolean killsSrc = mi.killsRegister(mo1.getReg());
                if (destST < 8)
                {
                     int srcFP = getFPReg(mo1);
                     assert isLive(srcFP):"Can't copy dead register";
                     assert !mo0.isDead():"Can't copy to dead ST register";

                     while (numPendingSTs <= destST)
                         pendingST[numPendingSTs++] = NumFPRegs;

                     if (isScratchReg(pendingST[destST]))
                     {
                         if (Util.DEBUG)
                             System.err.printf("Clobbering old ST in FP%d%n", pendingST[destST]);
                         itr = freeStackSlotBefore(itr, pendingST[destST]);
                     }

                     if (killsSrc)
                     {
                         duplicatePendingSTBeforeKill(srcFP, itr);
                         int slot = getSlot(srcFP);
                         int sr = getScratchReg();
                         pendingST[destST] = sr;
                         stack[slot] = sr;
                         regMap[sr] = slot;
                     }
                     else
                     {
                         pendingST[destST] =  srcFP;
                     }
                     break;
                }
                if(srcST < 8)
                {
                    int destFP = getFPReg(mo0);
                    assert !isLive(destFP):"Can't copy ST to live FP register";
                    assert numPendingSTs<srcST:"Can't copy from dead ST register";
                    int srcFP = pendingST[srcST];
                    assert isScratchReg(srcFP):"Expected ST in a scratch register";
                    assert isLive(srcFP):"Scratch holding ST is dead";

                    int slot = getSlot(srcFP);
                    stack[slot] = destFP;
                    regMap[destFP] = slot;
                    pendingST[srcST] = NumFPRegs;
                    while (numPendingSTs !=0 && pendingST[numPendingSTs-1] == NumFPRegs)
                        --numPendingSTs;
                    break;
                }

                int destFP = getFPReg(mo0);
                int srcFP = getFPReg(mo1);
                assert isLive(srcFP):"Can't copy dead register";
                if (killsSrc)
                {
                    int slot = getSlot(srcFP);
                    stack[slot] = destFP;
                    regMap[destFP] = slot;
                }
                else
                {
                    duplicateToTop(srcFP, destFP, itr);
                }
                break;
            }
            case TargetInstrInfo.IMPLICIT_DEF:
            {
                int reg = mi.getOperand(0).getReg() - X86GenRegisterNames.FP0;
                if (Util.DEBUG)
                    System.err.printf("Emitting LD_F0 for implicit FP%d%n", reg);
                buildMI(mbb, itr, tii.get(LD_F0));
                pushReg(reg);
                break;
            }
            case TargetInstrInfo.INLINEASM:
            {
                llvmReportError("inline asm not supported yet!");
                break;
            }
            case RET:
            case RETI:
                int firstFPRegOp = ~0, secondFPRegOp = ~0;
                int liveMask = 0;

                for (int i = 0, e = mi.getNumOperands(); i < e; i++)
                {
                    MachineOperand mo = mi.getOperand(i);
                    if (!mo.isRegister() || mo.getReg() == 0 || mo.getReg() < X86GenRegisterNames.FP0
                            || mo.getReg() > X86GenRegisterNames.FP6)
                        continue;

                    assert mo.isUse() && (mo.isKill() || getFPReg(mo) == firstFPRegOp
                    || mi.killsRegister(mo.getReg())):"Ret only defs operand, and values aren't live beyond it";

                    if (firstFPRegOp == ~0)
                        firstFPRegOp = getFPReg(mo);
                    else
                    {
                        assert secondFPRegOp == ~0:"More than two fp operands";
                        secondFPRegOp = getFPReg(mo);
                    }
                    liveMask |= (1<<getFPReg(mo));

                    mi.removeOperand(i);
                    --i;
                    --e;
                }

                adjustLiveRegs(liveMask, itr);
                if (liveMask ==0) return itr;

                if (secondFPRegOp == ~0)
                {
                    assert stackTop == 1&&firstFPRegOp == getStackEntry(0):
                            "Top of stack not the right register for RET!";

                    stackTop = 0;
                    return itr;
                }

                if (stackTop == 1)
                {
                    assert firstFPRegOp == secondFPRegOp && firstFPRegOp == getStackEntry(0)
                            :"Stack misconfiguration for RET!";
                    int newReg = getScratchReg();
                    duplicateToTop(firstFPRegOp, newReg, itr);
                    firstFPRegOp = newReg;
                }

                assert stackTop == 2:"Must have two values live!";

                if (getStackEntry(0) == secondFPRegOp)
                {
                    assert getStackEntry(1) == firstFPRegOp:"Unknown regs live";
                    moveToTop(firstFPRegOp, itr);
                }

                assert getStackEntry(0) == firstFPRegOp:"Unknown regs live";
                assert getStackEntry(1) == secondFPRegOp:"Unknown regs live";
                stackTop = 0;
                return itr;
        }

        // remove the pseudo instruction.
        mbb.remove(itr);

        if (itr == 0)
        {
            if (Util.DEBUG) System.err.println("Inserting dummy KILL");
            buildMI(mbb, itr, tii.get(TargetInstrInfo.KILL));
        }
        else
            --itr;
        return itr;
    }


    private void bundleCFG(MachineFunction mf)
    {
        assert liveBundles.isEmpty():"Stable data in liveBundles";
        for (int i = 0; i < edgeBundles.getNumBundles(); i++)
            liveBundles.add(new LiveBundle());

        for(MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            int mask = calcLiveInMask(mbb);
            if (mask == 0) continue;
            liveBundles.get(edgeBundles.getBundles(mbb.getNumber(), false)).mask |= mask;
        }
    }

    private boolean isFPCopy(MachineInstr mi)
    {
        int destReg = mi.getOperand(0).getReg();
        int srcReg = mi.getOperand(1).getReg();

        return RFP80RegisterClass.contains(destReg) ||
                RFP80RegisterClass.contains(srcReg);
    }

    private boolean processBasicBlock(MachineFunction mf, MachineBasicBlock mbb)
    {
        boolean changed = false;
        this.mbb = mbb;
        numPendingSTs = 0;
        setupBlockStack();

        for (int itr = 0; itr <mbb.size(); ++itr)
        {
            MachineInstr mi = mbb.getInstAt(itr);
            int flags = mi.getDesc().tSFlags;

            int fpInstClass = flags & X86II.FPTypeMask;
            if (mi.isCopy() && isFPCopy(mi))
                fpInstClass = X86II.SpecialFP;
            if (mi.isImplicitDef() && RFP80RegisterClass.contains(mi.getOperand(0).getReg()))
                fpInstClass = X86II.SpecialFP;

            if (fpInstClass == X86II.NotFP)
                continue;

            int prevItr = 0;
            if (itr != 0)
                prevItr = itr-1;

            NumFP.inc();
            if (Util.DEBUG)
            {
                System.err.print("\nFPInst:\t");
                mi.dump();
            }

            TIntArrayList deadRegs = new TIntArrayList();
            for (int i = 0,e = mi.getNumOperands(); i< e; i++)
            {
                MachineOperand mo = mi.getOperand(i);
                if (mo.isRegister() && mo.getReg() != 0 && mo.isDead())
                    deadRegs.add(mo.getReg());
            }

            switch (fpInstClass)
            {
                case X86II.ZeroArgFP:
                    handleZeroArgFP(itr);
                    break;
                case X86II.OneArgFP:
                    handleOneArgFP(itr);
                    break;
                case X86II.OneArgFPRW:
                    handleOneArgFPRW(itr);
                    break;
                case X86II.TwoArgFP:
                    itr = handleTwoArgFP(itr);
                    break;
                case X86II.CompareFP:
                    itr = handleCompareFP(itr);
                    break;
                case X86II.CondMovFP:
                    itr = handleCondMovFP(itr);
                    break;
                case X86II.SpecialFP:
                    itr = handleSpecialFP(itr);
                    break;
                default:
                    Util.shouldNotReachHere("Unknown FP type!");
            }

            for (int i = 0, e = deadRegs.size(); i < e; i++)
            {
                int reg = deadRegs.get(i);
                if (reg >= X86GenRegisterNames.FP0 && reg <= X86GenRegisterNames.FP6)
                {
                    if (Util.DEBUG) System.err.printf("Register FP#%d is dead!%n",
                            reg-X86GenRegisterNames.FP0);
                    itr = freeStackSlotAfter(itr, reg-X86GenRegisterNames.FP0);
                }
            }

            if (Util.DEBUG)
            {
                if (itr == prevItr)
                    System.err.println("Just deleted pseudo instruction");
                else
                {
                    int start = itr;
                    while (start != 0 && (start-1) != prevItr)
                        --start;
                    System.err.printf("Inserted instructions:%n\t");
                    mbb.getInstAt(start).print(System.err, mf.getTarget());
                    while (++start != itr+1);
                }
                dumpStack();
            }
            changed = true;
        }
        finishBlcokStack();
        return changed;
    }

    private void finishBlcokStack()
    {
        if (mbb.succIsEmpty())
            return;

        if (Util.DEBUG)
        {
            System.err.printf("Setting up live-outs for BB#%d derived from %s.\n",
                    mbb.getNumber(), mbb.getName());
        }

        int bundleIdx = edgeBundles.getBundles(mbb.getNumber(), true);
        LiveBundle bundle = liveBundles.get(bundleIdx);

        int termIdx = mbb.getFirstTerminator();
        adjustLiveRegs(bundle.mask, termIdx);

        if (bundle.mask == 0)
        {
            if (Util.DEBUG) System.err.println("No live-out.");
            return;
        }

        if (Util.DEBUG) System.err.printf("LB#%d: ", bundleIdx);
        if (bundle.isFixed())
        {
            if (Util.DEBUG) System.err.println("Shuffling stack to match.");
            shuffleStackTop(bundle.fixStack, bundle.fixCount, termIdx);
        }
        else
        {
            if (Util.DEBUG) System.err.println("Fixing stack order now.");
            bundle.fixCount = stackTop;
            for (int i = 0; i < stackTop; i++)
                bundle.fixStack[i] = getStackEntry(i);
        }
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        MachineRegisterInfo mri = mf.getMachineRegisterInfo();
        boolean fpIsUsed = false;
        for (int i = 0; i < 7; i++)
        {
            if (mri.isPhysRegUsed(X86GenRegisterNames.FP0+i))
            {
                fpIsUsed = true;
                break;
            }
        }
        // If no FP instruction used in mf, terminates early.
        if (!fpIsUsed)
            return false;

        return false;
    }

    @Override
    public String getPassName()
    {
        return "X86 FP stackifier Pass";
    }

    public static MachineFunctionPass createX86FPStackifierPass()
    {
        return new X86FloatingPointStackifier();
    }
}
