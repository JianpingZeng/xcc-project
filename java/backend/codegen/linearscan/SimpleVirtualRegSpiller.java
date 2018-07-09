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

import backend.codegen.MachineFunction;
import backend.codegen.MachineOperand;
import tools.Util;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class SimpleVirtualRegSpiller
{
    private IntervalLocKeeper ilk;
    public SimpleVirtualRegSpiller(IntervalLocKeeper ilk)
    {
        this.ilk = ilk;
    }

    public boolean runOnMachineFunction(MachineFunction mf, ArrayList<LiveInterval> handled)
    {
        if (handled == null || handled.isEmpty())
            return false;

        for (LiveInterval it : handled)
        {
            Util.assertion(ilk.isAssignedPhyReg(it), "No free register?");
            int reg = ilk.getPhyReg(it);
            for (UsePoint up : it.getUsePoints())
            {
                MachineOperand mo = up.mo;
                mo.setReg(reg);
            }
        }
        return true;
    }
}
