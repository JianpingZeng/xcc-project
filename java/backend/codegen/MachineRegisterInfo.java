package backend.codegen;
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

import backend.target.TargetRegisterClass;
import gnu.trove.list.array.TIntArrayList;
import tools.BitMap;
import tools.Pair;

import java.util.ArrayList;

import static backend.target.TargetRegisterInfo.FirstVirtualRegister;
import static backend.target.TargetRegisterInfo.NoRegister;

/**
 * Maps register number to register classes which used to assist register allocation.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineRegisterInfo
{
    public static class DefUseChainIterator
    {
        private MachineOperand op;
        private boolean returnUses;
        private boolean returnDefs;

        public DefUseChainIterator(MachineOperand mo, boolean uses, boolean defs)
        {
            op = mo;
            returnUses = uses;
            returnDefs = defs;
            if (op != null)
            {
                if ((!returnUses && op.isUse())
                        || (!returnDefs && op.isDef()))
                    next();
            }
        }

        public void next()
        {
            assert op != null:"Can not increment end iterator";
            op = op.getNextOperandForReg();

            // Skip those machine operand that We don't care about.
            while (op != null && ((!returnUses && op.isUse())
                    || (!returnDefs && op.isDef())))
                op = op.getNextOperandForReg();
        }

        public boolean atEnd()
        {
            return op != null;
        }

        public MachineOperand getOpearnd()
        {
            assert op != null:"Can not derefence end iterator!";
            return op;
        }

        public int getOperandNo()
        {
            assert op != null:"Can not derefence end iterator!";
            return getMachineInstr().getIndexOf(op);
        }

        public MachineInstr getMachineInstr()
        {
            assert op != null:"Can not derefence end iterator!";
            return op.getParent();
        }

        public boolean hasNext()
        {
            return op != null;
        }
    }

    /**
     * Mapping from virtual register number to its attached register class and
     * define machine operand.
     */
    private ArrayList<Pair<TargetRegisterClass, MachineOperand>> vregInfo;

    private ArrayList<TIntArrayList> regClass2VRegMap;

    private ArrayList<Pair<Integer, Integer>> regAllocHints;

    /**
     * This is an array of the head of the use/def list for
     * physical registers.
     */
    private ArrayList<MachineOperand> physRegUseDefLists;

    private BitMap usedPhysRegs;

    private ArrayList<Pair<Integer, Integer>> liveIns;
    private TIntArrayList liveOuts;

    public MachineRegisterInfo()
    {
        vregInfo = new ArrayList<>();
        regClass2VRegMap = new ArrayList<>();
        regAllocHints = new ArrayList<>();
        physRegUseDefLists = new ArrayList<>();
        usedPhysRegs = new BitMap();
        liveIns = new ArrayList<>();
        liveOuts = new TIntArrayList();
    }

    private int rescale(int reg)
    {
        return reg - FirstVirtualRegister;
    }

    /**
     * Obatins the target register class for the given virtual register.
     * @param reg
     * @return
     */
    public TargetRegisterClass getRegClass(int reg)
    {
        int actualReg = rescale(reg);
        assert actualReg< vregInfo.size():"Register out of bound!";
        return vregInfo.get(actualReg).first;
    }

    /**
     * Creates and returns a new virtual register in the current function with
     * specified target register class.
     * @param regClass
     * @return
     */
    public int createVirtualRegister(TargetRegisterClass regClass)
    {
        vregInfo.add(new Pair<>(regClass, null));
        return vregInfo.size() - 1 + FirstVirtualRegister;
    }

    public void clear()
    {
        vregInfo.clear();
    }

    /**
     * Gets the definition machine operand of the specified virtual register.
     * @param regNo
     * @return
     */
    public MachineOperand getDefMO(int regNo)
    {
        assert regNo >= FirstVirtualRegister
                : "the regNo is not a virtual register";
        int actualReg = rescale(regNo);
        assert actualReg< vregInfo.size():"Register out of bound!";
        return vregInfo.get(actualReg).second;
    }

    public MachineInstr getDefMI(int regNo)
    {
        return getDefMO(regNo).getParentMI();
    }

    public void setDefMO(int regNo, MachineOperand mo)
    {
        assert regNo >= FirstVirtualRegister
                : "the regNo is not a virtual register";
        int actualReg = rescale(regNo);
        assert actualReg< vregInfo.size():"Register out of bound!";
        vregInfo.get(regNo).second = mo;

    }

    /**
     * Checks to see if the specified register is a physical register or not.
     * @param regNo
     * @return
     */
    public boolean isPhysicalReg(int regNo)
    {
        return regNo>= NoRegister && regNo < FirstVirtualRegister;
    }
    /**
     * Checks to see if the specified register is a virtual register or not.
     * @param regNo
     * @return
     */
    public boolean isVirtualReg(int regNo)
    {
        return regNo >= FirstVirtualRegister;
    }

    public int getLastVirReg()
    {
        return vregInfo.size() - 1;
    }

    /**
     * Return the head pointer for the register use/def
     * list for the specified virtual or physical register.
     * @param regNo
     * @return
     */
    public MachineOperand getRegUseDefListHead(int regNo)
    {
        if (regNo < FirstVirtualRegister)
            return physRegUseDefLists.get(regNo);
        regNo -= FirstVirtualRegister;
        return vregInfo.get(regNo).second;
    }

    /**
     * Walk all defs and uses of the specified register.
     * @param regNo
     * @return
     */
    public DefUseChainIterator getRegIterator(int regNo)
    {
        return new DefUseChainIterator(getRegUseDefListHead(regNo), true, true);
    }

    /**
     * Checks if there is machine operand uses or defines the specified register.
     * @param regNo
     * @return  Return true if there is have def or uses of the spcified reg.
     */
    public boolean hasDefUseOperand(int regNo)
    {
        return !new DefUseChainIterator(getRegUseDefListHead(regNo), true, true).
                atEnd();
    }

    /**
     * Walk all defs of the specified register.
     * @param regNo
     * @return
     */
    public DefUseChainIterator getDefIterator(int regNo)
    {
        return new DefUseChainIterator(getRegUseDefListHead(regNo), false, true);
    }

    /**
     * Checks if it have any defs of specified register.
     * @param regNo
     * @return  Return true if have.
     */
    public boolean hasDefOperand(int regNo)
    {
        return !new DefUseChainIterator(getRegUseDefListHead(regNo), false, true).atEnd();
    }

    public DefUseChainIterator getUseIterator(int regNo)
    {
        return new DefUseChainIterator(getRegUseDefListHead(regNo), true, false);
    }

    /**
     * Checks if it have any uses of specified register.
     * @param regNo
     * @return  Return true if have.
     */
    public boolean hasUseOperand(int regNo)
    {
        return !new DefUseChainIterator(getRegUseDefListHead(regNo), true, false).atEnd();
    }

    /**
     * Return the machine instr that defines the specified virtual
     * register or null if none is found.  This assumes that the code is in SSA
     * form, so there should only be one definition.
     * @param reg
     * @return
     */
    public MachineInstr getVRegDef(int reg)
    {
        // TODO: 17-7-23
        return null;
    }


    /**
     * Replace all instances of FromReg with ToReg in the
     * machine function.  This is like llvm-level X->replaceAllUsesWith(Y),
     * except that it also changes any definitions of the register as well.
     * @param oldReg
     * @param newReg
     */
    public void replaceRegWith(int oldReg, int newReg)
    {
        assert oldReg != newReg :"It is not needed to replace the same reg";
        DefUseChainIterator itr = getRegIterator(oldReg);
        while (itr.hasNext())
        {
            MachineOperand mo = itr.getOpearnd();
            mo.setReg(newReg);
            itr.next();
        }
    }
}
