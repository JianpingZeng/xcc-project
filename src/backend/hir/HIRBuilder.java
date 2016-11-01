package backend.hir;
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

import backend.value.Constant;
import backend.value.Instruction;
import backend.value.Value;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class HIRBuilder
{
    /**
     * The basic block where all instruction will be inserted.
     */
    private BasicBlock curBB;
    private int insertPtr;

    public HIRBuilder() {}

    public HIRBuilder(BasicBlock bb)
    {
        setInsertPoint(bb);
    }

    public void setInsertPoint(BasicBlock insertPoint)
    {
        curBB = insertPoint;
        insertPtr = curBB.getNumOfInsts();
    }

    public Instruction.StoreInst createStore(Value val, Value ptr)
    {
        return insert(new Instruction.StoreInst(val, ptr, ""));
    }

    private <InstTy extends Instruction> InstTy insert(InstTy inst)
    {
        insertHelper(inst, curBB, "", insertPtr);
        return inst;
    }

    private <InstTy extends Instruction> InstTy insert(InstTy inst, String name)
    {
        insertHelper(inst, curBB, name, insertPtr);
        return inst;
    }

    private Constant insert(Constant c, String name)
    {
        return c;
    }

    private <InstTy extends Instruction> void insertHelper(InstTy inst,
            BasicBlock bb,
            String name,
            int insertPtr)
    {
        if (insertPtr == bb.getNumOfInsts())
        {
            bb.appendInst(inst);
            return;
        }
        bb.insertAfter(inst, insertPtr);
    }
}
