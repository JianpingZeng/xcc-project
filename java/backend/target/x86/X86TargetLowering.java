package backend.target.x86;
/*
 * Xlous C language Compiler
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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineModuleInfo;
import backend.codegen.selectDAG.FastISel;
import backend.target.TargetLowering;
import backend.value.BasicBlock;
import backend.value.Instruction.AllocaInst;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.HashMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86TargetLowering extends TargetLowering
{
    public X86TargetLowering(X86TargetMachine tm)
    {
        super();
    }

    /**
     * This method returns a target specific FastISel object,
     * or null if the target does not support "fast" ISel.
     * @param mf
     * @param mmi
     * @param vm
     * @param bm
     * @param am
     * @return
     */
    @Override
    public FastISel createFastISel(MachineFunction mf,
            MachineModuleInfo mmi,
            TObjectIntHashMap vm,
            HashMap<BasicBlock, MachineBasicBlock> bm,
            TObjectIntHashMap<AllocaInst> am)
    {
        return new X86GenFastISel(mf, mmi, vm, bm, am);
    }
}
