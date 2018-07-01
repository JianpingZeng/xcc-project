package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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
import backend.target.TargetRegisterInfo;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PhysRegTracker
{
    /**
     * A records the uses of number for each physical register.
     * Note that, it just cares about physical register rather
     * than virtual register.
     */
    private int[] physRegUsed;
    private TargetRegisterInfo tri;

    public PhysRegTracker(TargetRegisterInfo tri)
    {
        this.tri = tri;
        physRegUsed = new int[tri.getNumRegs()];
    }

    public boolean isRegAvail(int reg)
    {
        Util.assertion(isPhysicalRegister(reg), "should be physical register!");
        return physRegUsed[reg] == 0;
    }

    public void addRegUse(int phyReg)
    {
        Util.assertion(isPhysicalRegister(phyReg), "should be physical register!");
        physRegUsed[phyReg]++;
        for (int alias : tri.getAliasSet(phyReg))
            physRegUsed[alias]++;
    }

    public void delRegUse(int phyReg)
    {
        Util.assertion(isPhysicalRegister(phyReg), "should be physical register!");
        physRegUsed[phyReg]--;
        for (int alias : tri.getAliasSet(phyReg))
            physRegUsed[alias]--;
    }
}
