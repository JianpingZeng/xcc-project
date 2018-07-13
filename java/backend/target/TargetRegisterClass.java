package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

import tools.Util;
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

    public void setSubClasses(TargetRegisterClass[] subClasses)
    {
        this.subClasses = subClasses;
    }

    public void setSubRegClasses(TargetRegisterClass[] subRegClasses)
    {
        this.subRegClasses = subRegClasses;
    }

    public void setSuperClasses(TargetRegisterClass[] superClasses)
    {
        this.superClasses = superClasses;
    }

    public void setSuperRegClasses(TargetRegisterClass[] superRegClasses)
    {
        this.superRegClasses = superRegClasses;
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
        Util.assertion( i >= 0 && i < regs.length);
        return regs[i];
    }

    /**
     * Returns all of register in current target machine, and contains unavailable
     * register. If want to obtain all available registers, just consulting by
     * {@linkplain #getAllocableRegs(MachineFunction)}.
     * @return
     */
    public int[] getRegs()
    {
        return regs;
    }

    /**
     * Return the number of the register in bytes, which is also the number
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
    public int getAlignment()
    {
        return regAlign;
    }

    public EVT[] getVTs()
    {
        return vts;
    }

    /**
     * Obtains the allocatable registers of type array.
     * Default, returned array is as same as registers array contained in this
     * TargetRegisterClass. But it is may be altered for concrete sub class. e.g.
     * GR32RegisterClass have more register (R8D, R9D etc) in 64bit subtarget.
     * @return  An array of allocatable registers for specified sub-target.
     */
    public int[] getAllocableRegs(MachineFunction mf)
    {
        return regs;
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
        if (superClasses == null) return false;

        for (TargetRegisterClass superRC : superClasses)
            if (superRC.equals(rc))
                return true;

        return false;
    }

    public boolean hasSubClass(TargetRegisterClass subRC)
    {
        if (subClasses == null || subClasses.length <= 0)
            return false;
        for (TargetRegisterClass rc : subClasses)
            if (rc.equals(subRC))
                return true;
        return false;
    }

    public TargetRegisterClass[] getSubClasses()
    {
        return subClasses;
    }

    public TargetRegisterClass[] getSuperClasses()
    {
        return superClasses;
    }

    public TargetRegisterClass getSubRegisterRegClass(long subIdx)
    {
        for (int i = 0; i < subIdx-1; i++)
            if (subRegClasses[i] == null)
                return null;
        return subRegClasses[(int) (subIdx-1)];
    }

    public TargetRegisterClass getSuperRegisterRegClass(
            TargetRegisterClass rc,
            int subIdx, EVT vt)
    {
        for (TargetRegisterClass itr : superRegClasses)
        {
            if (itr.hasType(vt) && itr.getSubRegisterRegClass(subIdx).equals(rc))
                return itr;
        }
        Util.assertion(false, "Couldn't find the register class!");
        return null;
    }

    public int getCopyCost()
    {
        return copyCost;
    }
}
