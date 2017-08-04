package backend.codegen.selectDAG;
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

import backend.codegen.MVT;
import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineRegisterInfo;
import backend.target.TargetLowering;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction.AllocaInst;
import backend.value.Value;
import gnu.trove.map.hash.TObjectIntHashMap;
import jlang.support.APInt;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This contains information that is global to a function that is used when
 * lowering a region of the function.
 * @author Xlous.zeng
 * @version 0.1
 */
public class FunctionLoweringInfo
{
    public static class LiveOutInfo
    {
        public int numSignBits;
        public APInt knownOne, knownZero;
        public LiveOutInfo()
        {
            numSignBits = 0;
        }
    }


    public TargetLowering tli;
    public Function fn;
    public MachineFunction mf;
    public MachineRegisterInfo mri;
    /**
     * A mapping from LLVM basic block to their machine code entry.
     */
    public HashMap<BasicBlock, MachineBasicBlock> mbbmap;
    /**
     * Since we emit code for the function a basic block at a time, we must
     * remember which virtual registers hold the values for cross-basic-block
     * values.
     */
    public TObjectIntHashMap<Value> valueMap;

    /**
     * Keep track of frame indices for fixed sized allocas in the entry block.
     * This allows allocas to be efficiently referenced anywhere in the function.
     */
    public TObjectIntHashMap<AllocaInst> staticAllocaMap;

    /**
     * Information about live out vregs, indexed by their register number offset
     * by 'FirstVirtualRegister'.
     */
    public ArrayList<LiveOutInfo> liveOutRegInfo;

    public FunctionLoweringInfo(TargetLowering tli)
    {
        this.tli = tli;
        mbbmap = new HashMap<>();
        valueMap = new TObjectIntHashMap<>();
        staticAllocaMap = new TObjectIntHashMap<>();
        liveOutRegInfo = new ArrayList<>();
    }

    /**
     * Initiliaze this FunctionLoweringInfo with the given Function and its
     * associated MachineFunction.
     */
    public void set(Function fn, MachineFunction mf, boolean enableFastISel)
    {
        // TODO: 17-8-4
    }

    public int makeReg(MVT vt)
    {
        // TODO: 17-8-4
        return 0;
    }

    public boolean isExportedInst(Value v)
    {
        return valueMap.containsKey(v);
    }

    public int createRegForValue(Value v)
    {
        // TODO: 17-8-4
        return 0;
    }

    public int initializeRegForValue(Value v)
    {
        assert !valueMap.containsKey(v):"Already initialized this value register!";
        int r = createRegForValue(v);
        valueMap.put(v, r);
        return r;
    }

    /**
     * Clear out all the function-specific state. This returns this FunctionLoweringInfo
     * to an empty state, ready to be used for a different function.
     */
    public void clear()
    {
        mbbmap.clear();
        valueMap.clear();
        staticAllocaMap.clear();
        liveOutRegInfo.clear();
    }
}
