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

import backend.type.Type;
import backend.value.BasicBlock;
import backend.value.Instruction;
import backend.value.Operator;
import backend.value.Value;

/**
 * An instruction to allocated memory on the heap.
 * @author Xlous.zeng
 * @version 0.1
 */
public class MallocInst extends AllocationInst
{
    public MallocInst(Type ty)
    {
        this(ty, null);
    }
    public MallocInst(Type ty, Value arraySize)
    {
        this(ty, arraySize, "");
    }

    public MallocInst(Type ty, Value arraySize, String name)
    {
        this(ty, arraySize, name, 0, null);
    }

    public MallocInst(Type ty, Value arraySize, String name, Instruction insertBefore)
    {
        this(ty, arraySize, name, 0, insertBefore);
    }


    public MallocInst(Type ty, Value arraySize,
            String name, int align, Instruction insertBefore)
    {
        super(ty, Operator.Malloc, arraySize, align, name, insertBefore);
    }

    public MallocInst(Type ty, Value arraySize, int align,
            String name, BasicBlock insertAtEnd)
    {
        super(ty, Operator.Malloc, arraySize, align, name, insertAtEnd);
    }
}
