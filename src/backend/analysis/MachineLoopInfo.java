package backend.analysis;
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

import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionPass;

/**
 * This class used for computing dominator tree on Machine CFG.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineLoopInfo extends MachineFunctionPass
{
    @Override
    public String getPassName()
    {
        return null;
    }

    /**
     * This method must be overridded by concrete subclass for performing
     * desired machine code transformation or analysis.
     *
     * @param mf
     * @return
     */
    @Override public boolean runOnMachineFunction(MachineFunction mf)
    {
        return false;
    }
}
