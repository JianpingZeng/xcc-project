/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package backend.support;

import backend.type.OpaqueType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TypeFinder
{
    private HashSet<Value> visitedConstants;
    private HashSet<Type> visitedTypes;

    private TypePrinting printer;
    private ArrayList<Type> numberedTypes;

    public TypeFinder(TypePrinting printer, ArrayList<Type> fp)
    {
        visitedConstants = new HashSet<>();
        visitedTypes = new HashSet<>();
        this.printer = printer;
        numberedTypes = fp;
    }

    public void run(Module m)
    {
        // Get types for type definition in Module.
        HashMap<String, Type> st = m.getTypeSymbolTable();
        for (Type ty : st.values())
            incorperateType(ty);

        // Get the type of GlobalVariable.
        for (GlobalVariable gv : m.getGlobalVariableList())
        {
            incorperateType(gv.getType());
            if (gv.hasInitializer())
                incorperateValue(gv.getInitializer());
        }

        // Get types from functions.
        for (Function f : m.getFunctionList())
        {
            for (BasicBlock bb : f.getBasicBlockList())
            {
                for (Instruction inst:bb)
                {
                    incorperateType(inst.getType());
                    for (int i = 0, e = inst.getNumOfOperands(); i != e; i++)
                    {
                        incorperateValue(inst.operand(i));
                    }
                }
            }
        }
    }

    private void incorperateValue(Value val)
    {
        if (val == null || !(val instanceof Constant) || val instanceof GlobalValue)
            return;

        if (!visitedConstants.add(val))
            return;

        incorperateType(val.getType());

        Constant c = (Constant)val;
        for (int i = 0, e = c.getNumOfOperands(); i != e; i++)
            incorperateValue(c.operand(i));
    }

    private void incorperateType(Type ty)
    {
        if (!visitedTypes.add(ty))
            return;

        if (((ty instanceof StructType && ((StructType)ty).getNumOfElements() != 0)
                || ty instanceof OpaqueType) && !printer.hasTypeName(ty))
        {
            printer.addTypeName(ty, "%" + numberedTypes.size());
            numberedTypes.add(ty);
        }

        // Recursively walk all contained types.
        for (int i = 0, e = ty.getNumContainedTypes(); i != e; i++)
            incorperateType(ty.getContainedType(i));
    }

}
