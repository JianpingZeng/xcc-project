package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.pass.AnalysisUsage;

/**
 * This class implements a MachineFunctionPass used for performing linear scan
 * register allocation on each MachineFunction.
 * <pre>
 * If you want to learn more information, consult the paper
 * Poletto, Massimiliano, and Vivek Sarkar. "Linear scan register allocation.".
 * </pre>
 * @author Xlous.zeng
 * @version 0.1
 */
public class RegAllocLinearScan extends MachineFunctionPass
{
    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(LiveIntervals.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public String getPassName()
    {
        return "Linear scan register allocator";
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        return false;
    }
}
