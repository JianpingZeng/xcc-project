package backend.value;
/*
 * Xlous C language CompilerInstance
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

import backend.type.StructType;
import backend.type.Type;
import backend.value.UniqueConstantValueImpl.ConstantStructKey;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines internal data structure for representing constant struct
 * in LLVM IR, like '{1, 2, 3}' defines a constant struct with 3 integer.
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantStruct extends Constant
{
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param ty
     * @param vals
     */
    protected ConstantStruct(StructType ty, ArrayList<Constant> vals)
    {
        super(ty, ValueKind.ConstantStructVal);
        assert vals.size()  == ty.getNumOfElements()
                :"Invalid initializer vector for constant structure";
        reserve(vals.size());
        int idx = 0;
        for (Constant c : vals)
        {
            assert c.getType() == ty.getElementType(idx)
                    :"Initializer for struct element doesn't match struct element type!";
            setOperand(idx++, c, this);
        }
    }

    @Override
    public boolean isNullValue()
    {
        return false;
    }

    public static Constant get(StructType type, List<Constant> elts)
    {
        // Create a ConstantAggregateZero value if all elements are zeros
        for (Constant elt : elts)
        {
            if (!elt.isNullValue())
            {
                ConstantStructKey key = new ConstantStructKey(type, elts);
                return UniqueConstantValueImpl.getUniqueImpl().getOrCreate(key);
            }
        }
        return ConstantAggregateZero.get(type);
    }

    public static Constant get(Constant[] elts, boolean packed)
    {
        ArrayList<Type> eltTypes = new ArrayList<>();
        ArrayList<Constant> indices = new ArrayList<>();
        for (Constant c : elts)
        {
            eltTypes.add(c.getType());
            indices.add(c);
        }
        return get(StructType.get(eltTypes, packed), indices);
    }

    public static Constant get(List<Constant> elts, boolean packed)
    {
        ArrayList<Type> eltTypes = new ArrayList<>();
        for (Constant c : elts)
        {
            eltTypes.add(c.getType());
        }
        return get(StructType.get(eltTypes, packed), elts);
    }

    @Override
    public StructType getType() { return (StructType)super.getType();}
    @Override
    public Constant operand(int idx) { return super.operand(idx); }
}
