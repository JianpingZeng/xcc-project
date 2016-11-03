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

import backend.type.Type;
import backend.value.Constant;
import backend.value.Instruction;
import backend.value.Instruction.AllocaInst;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.CastInst;
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
        insertHelper(inst, curBB, insertPtr);
        return inst;
    }

    private <InstTy extends Instruction> InstTy insert(InstTy inst, String name)
    {
        insertHelper(inst, curBB, insertPtr);
        return inst;
    }

    private Constant insert(Constant c, String name)
    {
        return c;
    }

    private <InstTy extends Instruction> void insertHelper(InstTy inst,
            BasicBlock bb,
            int insertPtr)
    {
        if (insertPtr == bb.getNumOfInsts())
        {
            bb.appendInst(inst);
            return;
        }
        bb.insertAfter(inst, insertPtr);
    }

    public BasicBlock getInsertBlock()
    {
        return curBB;
    }

    /**
     * Create an unconditional branch instruction-'br label X'.
     * @param targetBB
     */
    public BranchInst createBr(BasicBlock targetBB)
    {
        return insert(new BranchInst(targetBB));
    }

    /**
     * Clear the current insertion point to let the newest created instruction
     * would be inserted into a block.
     *
     */
    public void clearInsertPoint()
    {
        curBB = null;
    }

    public Value createAlloca(Type type, Value arraySize, String name)
    {
        return insert(new AllocaInst(type, arraySize, name));
    }

    //============================================================//
    // Cast instruction.                                          //
    //============================================================//

    public Value createTrunc(Value val, Type destType, String name)
    {
        return createCast(Operator.Trunc, val, destType, name);
    }

    public Value createZExt(Value val, Type destType, String name)
    {
        return createCast(Operator.ZExt, val, destType, name);
    }

    public Value createSExt(Value val, Type destType, String name)
    {
        return createCast(Operator.SExt, val, destType, name);
    }

    public Value createFPToUI(Value val, Type destType, String name)
    {
        return createCast(Operator.FPToUI, val, destType, name);
    }

    public Value createFPToSI(Value val, Type destType, String name)
    {
        return createCast(Operator.FPToSI, val, destType, name);
    }

    public Value createUIToFP(Value val, Type destType, String name)
    {
        return createCast(Operator.UIToFP, val, destType, name);
    }

    public Value createSIToFP(Value val, Type destType, String name)
    {
        return createCast(Operator.SIToFP, val, destType, name);
    }

    public Value createFPTrunc(Value val, Type destType, String name)
    {
        return createCast(Operator.FPTrunc, val, destType, name);
    }

    public Value createFPExt(Value val, Type destType, String name)
    {
        return createCast(Operator.FPExt, val, destType, name);
    }

    public Value createPtrToInt(Value val, Type destType, String name)
    {
        return createCast(Operator.PtrToInt, val, destType, name);
    }

    public Value createIntToPtr(Value val, Type destType, String name)
    {
        return createCast(Operator.IntToPtr, val, destType, name);
    }

    public Value creatBitCast(Value val, Type destType, String name)
    {
        return createCast(Operator.BitCast, val, destType, name);
    }


    public Value createIntCast(Value value,
            backend.type.Type destTy,
            boolean isSigned)
    {
        return createIntCast(value, destTy, isSigned, "");
    }

    public Value createIntCast(Value value,
            backend.type.Type destTy,
            boolean isSigned,
            String name)
    {
        // if the type of source is equal to destination type
        // just return original value.
        if (value.getType() == destTy)
            return value;

        if (value instanceof Constant)
        {
            // TODO make constant folding.
        }
        return insert(CastInst.createIntegerCast(value, destTy, isSigned), name);
    }

    public Value createCast(Operator op, Value val, Type destType, String name)
    {
        if (val.getType() == destType)
            return val;

        if (val instanceof Constant)
        {
            // TODO make constant folding.
        }
        return insert(CastInst.create(op, val, destType, name, null));
    }

    public Value createBitCast(Value value, Type destTy, String name)
    {
        return createCast(Operator.BitCast, value, destTy, name);
    }

    public Value createMul(Value lhs, Value rhs, String name)
    {
        assert lhs.getType() == rhs.getType();
        return insert(new Instruction.Op2(lhs.getType(), Operator.Mul, lhs, rhs, name));
    }
}
