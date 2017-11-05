package backend.support;
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

import backend.value.*;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Enumerates slot number for unnamed values.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SlotTracker
{
    /**
     * The module for which we are holding slot numbers.
     */
    private Module theModule;
    /**
     * The function for which we are holding slot numbers.
     */
    private Function theFunction;

    private boolean functionProcessed;
    /**
     * The mapping from Value to its slot number in module level.
     */
    private TObjectIntHashMap<Value> mMap;
    /**
     * The next slot number of value in module level.
     */
    private int mNext;
    /**
     * The mapping from Value to its slot number in function level.
     */
    private TObjectIntHashMap<Value> fMap;
    /**
     * The next slot number of value in function level.
     */
    private int fNext;

    public SlotTracker(Module m)
    {
        theModule = m;
        mMap = new TObjectIntHashMap<>();
        fMap = new TObjectIntHashMap<>();
    }

    public SlotTracker(Function f)
    {
        this.theFunction = f;
        mMap = new TObjectIntHashMap<>();
        fMap = new TObjectIntHashMap<>();
    }

    public int getLocalSlot(Value v)
    {
        assert !(v instanceof Constant):
                "Can't get a constant or global slot with this!";

        initialize();
        return fMap.containsKey(v) ? fMap.get(v) : -1;
    }

    public int getGlobalSlot(GlobalValue gv)
    {
        initialize();
        return mMap.containsKey(gv) ? mMap.get(gv) : -1;
    }

    public void incorporateFunction(Function f)
    {
        theFunction = f;
        functionProcessed = false;
    }

    /**
     * After calling incorporateFunction, use this method to remove the
     * most recently incorporated function from the SlotTracker. This
     * will reset the state of the machine back to just the module contents.
     */
    public void pruneFunction()
    {
        fMap.clear();
        theFunction = null;
        functionProcessed = false;
    }

    private void initialize()
    {
        if (theModule != null)
        {
            processModule();
            theModule = null;
        }

        if (theFunction != null && !functionProcessed)
        {
            processFunction();
        }
    }

    private void createModuleSlot(GlobalValue gv)
    {
        assert gv != null;
        assert !gv.getType().equals(LLVMContext.VoidTy);
        assert !gv.hasName();

        int destSlot = mNext++;
        mMap.put(gv, destSlot);
    }

    private void createFunctionSlot(Value v)
    {
        assert v != null;
        assert !v.getType().equals(LLVMContext.VoidTy);
        assert !v.hasName();

        int destSlot = fNext++;
        fMap.put(v, destSlot);
    }

    private void processModule()
    {
        // Add all of the unnamed global variables to the value table.
        for (GlobalVariable gv : theModule.getGlobalVariableList())
        {
            if (!gv.hasName())
            {
                createModuleSlot(gv);
            }
        }

        for (Function f : theModule.getFunctionList())
        {
            if (!f.hasName())
                createModuleSlot(f);
        }
    }

    private void processFunction()
    {
        fNext = 0;
        // Add all the function arguments with no names.
        for (Argument arg : theFunction.getArgumentList())
        {
            if (!arg.hasName())
                createFunctionSlot(arg);
        }

        // Add all basic blocks and instructions don't have name.
        for (BasicBlock bb : theFunction.getBasicBlockList())
        {
            if (!bb.hasName())
            {
                createFunctionSlot(bb);
            }
            for (Instruction inst : bb)
            {
                if (!inst.getType().equals(LLVMContext.VoidTy) && !inst.hasName())
                    createFunctionSlot(inst);
            }
        }
        functionProcessed = true;
    }
}
