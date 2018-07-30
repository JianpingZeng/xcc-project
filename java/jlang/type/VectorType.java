package jlang.type;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Jianping Zeng.
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

import jlang.support.PrintingPolicy;
import tools.FoldingSetNode;
import tools.FoldingSetNodeID;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class VectorType extends Type implements FoldingSetNode
{
    private QualType eleTypes;
    private int numElts;

    public VectorType(QualType eltType, int numElts, QualType canTy)
    {
        super(TypeClass.Vector, canTy);
        this.eleTypes = eltType;
        this.numElts = numElts;
    }

    public int getNumOfElements()
    {
        return numElts;
    }

    public QualType getElementTypes()
    {
        return eleTypes;
    }

    @Override
    public String getAsStringInternal(String inner, PrintingPolicy policy)
    {
        inner += " __attribute__((__vector_size__(" +
                numElts + " * sizeof(" + eleTypes.getAsString() + "))))";
        return eleTypes.getAsStringInternal(inner, policy);
    }

    @Override
    public void profile(FoldingSetNodeID id)
    {
        profile(id, getElementTypes(), getNumOfElements(), getTypeClass());
    }

    static void profile(FoldingSetNodeID id, QualType eleType, int numElts,
                        int typeClass)
    {
        id.addInteger(eleType.hashCode());
        id.addInteger(numElts);
        id.addInteger(typeClass);
    }
}
