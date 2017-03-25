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

import backend.codegen.MVT;
import backend.codegen.MachineFunction;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TargetRegisterClass
{
    /**
     * The register getNumOfSubLoop and alignment in Bytes.
     */
    private int regSize, regAlign;
    private int[] regs;
    private MVT.ValueType[] vts;

    protected TargetRegisterClass(MVT.ValueType[] vts, int rs, int ra, int[] regs)
    {
        this.vts = vts;
        regSize = rs;
        regAlign = ra;
        this.regs = regs;
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
}
