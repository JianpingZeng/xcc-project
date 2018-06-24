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

package backend.value;

import backend.type.VectorType;

import java.util.ArrayList;
import java.util.HashMap;

import static backend.value.ValueKind.ConstantVectorVal;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantVector extends Constant
{
    private static class ConstantVectorKey
    {
        VectorType vt;
        ArrayList<Constant> vals;
        public ConstantVectorKey(VectorType ty, ArrayList<Constant> v)
        {
            vt = ty;
            vals = new ArrayList<>();
            vals.addAll(v);
        }

        @Override
        public int hashCode()
        {
            return vt.hashCode() << 13 + vals.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            if (this == obj) return true;
            if (getClass() != obj.getClass())
                return false;
            ConstantVectorKey key = (ConstantVectorKey)obj;
            return vt.equals(key.vt) && vals.equals(key.vals);
        }
    }

    private static final HashMap<ConstantVectorKey, ConstantVector> vectorConstants =
            new HashMap<>();
    /**
     * Constructs a new instruction representing the specified constants.
     *
     */
    protected ConstantVector(VectorType vt, ArrayList<Constant> vals)
    {
        super(vt, ConstantVectorVal);
        reserve(vals.size());
        int idx = 0;
        for (Constant c : vals)
        {
            assert c.getType() == vt.getElementType() ||
                    (vt.isAbstract() && c.getType().getTypeID() == vt.getElementType().getTypeID())
                    : "Initializer for struct element doesn't match struct element type!";
            setOperand(idx++, c, this);
        }
    }

    public static Constant get(VectorType vt, ArrayList<Constant> vals)
    {
        assert vals != null && !vals.isEmpty();
        Constant c = vals.get(0);
        boolean isZero = c.isNullValue();
        boolean isUndef = c instanceof UndefValue;
        if (isZero || isUndef)
        {
            for (int i = 1, e = vals.size(); i < e; i++)
            {
                if (!vals.get(i).equals(c))
                {
                    isZero = isUndef = false;
                    break;
                }
            }
        }

        if (isZero)
            return ConstantAggregateZero.get(vt);
        if (isUndef)
            return UndefValue.get(vt);

        ConstantVectorKey key = new ConstantVectorKey(vt, vals);
        if (vectorConstants.containsKey(key))
            return vectorConstants.get(key);
        ConstantVector cv = new ConstantVector(vt, vals);
        vectorConstants.put(key, cv);
        return cv;
    }

    public static Constant get(ArrayList<Constant> vals)
    {
        assert vals != null && !vals.isEmpty();
        return get(VectorType.get(vals.get(0).getType(), vals.size()), vals);
    }

    public static Constant get(Constant... vals)
    {
        assert vals != null && vals.length > 0;
        ArrayList<Constant> t = new ArrayList<>();
        for (Constant c : vals)
            t.add(c);
        return get(t);
    }

    @Override
    public VectorType getType()
    {
        return (VectorType) super.getType();
    }

    @Override
    public boolean isNullValue()
    {
        return false;
    }

    public boolean isAllOnesValue()
    {
        Constant c = operand(0);
        if (!(c instanceof ConstantInt) || !((ConstantInt)c).isAllOnesValue())
            return false;
        for (int i = 1, e = getNumOfOperands(); i < e; i++)
            if (!operand(i).equals(c))
                return false;
        return true;
    }

    public Constant getSplatValue()
    {
        Constant c = operand(0);
        for (int i = 1, e = getNumOfOperands(); i < e; i++)
            if (!operand(i).equals(c))
                return null;
        return c;
    }

    public void destroyConstant()
    {
        // TODO: 18-6-24
    }

    @Override
    public void replaceUsesOfWithOnConstant(Value from, Value to, Use u)
    {
        assert to instanceof Constant;
        ArrayList<Constant> values = new ArrayList<>();
        for (int i = 0, e = getNumOfOperands(); i < e; i++)
        {
            Constant val = operand(i);
            if (val.equals(from))
                val = (Constant) to;
            values.add(val);
        }

        Constant replacement = get(getType(), values);
        assert !replacement.equals(this);
        destroyConstant();
    }
}
