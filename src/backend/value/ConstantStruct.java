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

import java.util.ArrayList;

/**
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

    public static Constant get(Constant[] complex)
    {
        return null;
    }

    @Override
    public StructType getType() { return (StructType)super.getType();}
    @Override
    public Constant operand(int idx) { return (Constant)super.operand(idx); }
}
