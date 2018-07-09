/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

package backend.codegen.linearscan;

import backend.codegen.MachineBasicBlock;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MoveResolver
{
    private ArrayList<Pair<LiveInterval, LiveInterval>> mappings;
    private boolean multipleReadsAllowed;
    private int[] registerBlocked;
    private MachineBasicBlock insertedMBB;
    private int insertedIndex;
    private IntervalLocKeeper ilk;

    public MoveResolver(WimmerLinearScanRegAllocator allocator)
    {
        mappings = new ArrayList<>();
        multipleReadsAllowed = false;
        registerBlocked = new int[allocator.getRegisterInfo().getNumRegs()];
        ilk = allocator.getIntervalLocKeeper();
    }

    public void clear()
    {
        mappings.clear();
        multipleReadsAllowed = false;
        registerBlocked = null;
        insertedIndex = -1;
        ilk = null;
    }

    // mark assignedReg and assignedRegHi of the interval as blocked
    private void blockRegisters(LiveInterval it)
    {
        if (ilk.isAssignedPhyReg(it))
        {
            int reg = ilk.getPhyReg(it);
            assert multipleReadsAllowed || registerBlocked(reg)
                    == 0 : "register already marked as used";
            setRegisterBlocked(reg, 1);
        }
    }

    private void setRegisterBlocked(int reg, int direction)
    {

    }

    private int registerBlocked(int reg)
    {
        return 0;
    }

    // mark assignedReg and assignedRegHi of the it as unblocked
    private void unblockRegisters(LiveInterval it)
    {
        if (ilk.isAssignedPhyReg(it))
        {
            int reg = ilk.getPhyReg(it);
            assert registerBlocked(reg) > 0 : "register already marked as unused";
            setRegisterBlocked(reg, -1);
        }
    }

    /**
     * Checks if the assigned location is not blocked or is only blocked by {@code from}.
     */
    private boolean safeToProcessMove(LiveInterval from, LiveInterval to)
    {
        Util.assertion(ilk.isAssignedPhyReg(from));
        int fromReg = ilk.getPhyReg(from);

        if (ilk.isAssignedPhyReg(to))
        {
            int toReg = ilk.getPhyReg(to);
            if (registerBlocked(toReg) > 1 ||
                    (registerBlocked(toReg) == 1 && toReg != fromReg))
            {
                return false;
            }
        }

        return true;
    }

    private void insertMove(LiveInterval srcIt, LiveInterval dstIt)
    {}

    private void  resolveMappings()
    {}

    public void insertMoveInstr(MachineBasicBlock mbb, int index)
    {}

    public void addMapping(LiveInterval srcIt, LiveInterval dstIt)
    {}


}
