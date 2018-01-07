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

package backend.ir;

import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import tools.Util;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class AllocationInst extends Instruction.UnaryOps
{
    protected int align;

    protected AllocationInst(
            Type ty,
            Operator opcode,
            Value arraySize,
            int align,
            String name,
            Instruction insertBefore)
    {
        super(PointerType.getUnqual(ty), opcode, arraySize, name, insertBefore);
        setAlignment(align);
    }

    protected AllocationInst(Type ty, Operator opcode, Value arraySize,
            int align, String name, BasicBlock insertAtEnd)
    {
        super(PointerType.getUnqual(ty), opcode, arraySize, name, insertAtEnd);
        setAlignment(align);
    }

    public void setAlignment(int align)
    {
        assert (align & (align - 1)) == 0 : "Alignment is not a power of 2";
        this.align = Util.log2(align) + 1;
    }

    public int getAlignment()
    {
        return align;
    }

    public boolean isArrayAllocation()
    {
        if (operand(0) instanceof ConstantInt)
            return ((ConstantInt) operand(0)).getZExtValue() != 1;
        return true;
    }

    public Value getArraySize()
    {
        return operand(0);
    }

    public PointerType getType()
    {
        return (PointerType)super.getType();
    }

    public Type getAllocatedType()
    {
        return getType().getElementType();
    }
}
