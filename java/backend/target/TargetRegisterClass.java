package backend.target;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.MachineFunction;
import gnu.trove.set.hash.TIntHashSet;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TargetRegisterClass
{
    private int id;
    private String name;
    private TIntHashSet regSet;
    private EVT[] vts;
    private TargetRegisterClass[] subClasses;
    private TargetRegisterClass[] superClasses;
    private TargetRegisterClass[] subRegClasses;
    private TargetRegisterClass[] superRegClasses;
    private int copyCost;

    /**
     * The register getNumOfSubLoop and alignment in Bytes.
     */
    private int regSize, regAlign;
    private int[] regs;

    protected TargetRegisterClass(int id,
            String name,
            EVT[] vts,
            TargetRegisterClass[] subcs,
            TargetRegisterClass[] supercs,
            TargetRegisterClass[] subregcs,
            TargetRegisterClass[] superregcs,
            int regsz, int regAlign,
            int copyCost,
            int[] regs)
    {
        this.id = id;
        this.name = name;
        this.vts = vts;
        subClasses  = subcs;
        superClasses = supercs;
        subRegClasses = subregcs;
        superRegClasses  = superregcs;
        regSize = regsz;
        this.regAlign = regAlign;
        this.regs = regs;
        this.copyCost = copyCost;
        regSet = new TIntHashSet();
        regSet.addAll(regs);
    }

    public int getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public int getNumRegs()
    {
        return regs.length;
    }

    public int getRegister(int i)
    {
        assert i >= 0 && i < regs.length;
        return regs[i];
    }

    /**
     * Return the getNumOfSubLoop of the register in bytes, which is also the getNumOfSubLoop
     * of a stack slot allocated to hold a spilled copy of this register.
     *
     * @return
     */
    public int getRegSize()
    {
        return regSize;
    }

    /**
     * Return the minimum required alignment for a register of
     * this class.
     *
     * @return
     */
    public int getRegAlign()
    {
        return regAlign;
    }

    public EVT[] getVTs()
    {
        return vts;
    }

    /**
     * Obtains the begin index of the allocatable registers group.
     *
     * @return
     */
    public int allocatableBegin(MachineFunction mf)
    {
        return 0;
    }

    /**
     * Obtains the index of machine register behind of the last allocatable
     * register. So the allocatable register is qualified with range
     * from [begin, end).
     *
     * @return
     */
    public int allocatableEnd(MachineFunction mf)
    {
        return regs.length;
    }

    /**
     * Return true if the specified register is included in this
     * register class.
     * @param reg
     * @return
     */
    public boolean contains(int reg)
    {
        return regSet.contains(reg);
    }

    public boolean hasType(EVT vt)
    {
        for (int i = 0; vts[i].getSimpleVT().simpleVT != MVT.Other; ++i)
        {
            if (vts[i].equals(vt))
                return true;
        }
        return false;
    }

    public boolean hasSuperClass(TargetRegisterClass rc)
    {
        for (TargetRegisterClass superRC : superRegClasses)
            if (superRC.equals(rc))
                return true;

        return false;
    }
}
