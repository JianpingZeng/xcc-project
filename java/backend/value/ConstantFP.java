package backend.value;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Xlous
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

import tools.Util;
import backend.support.LLVMContext;
import backend.type.Type;
import backend.value.UniqueConstantValueImpl.APFloatKeyType;
import tools.APFloat;
import tools.APSInt;
import tools.FltSemantics;
import tools.OutRef;

import static backend.type.LLVMTypeID.*;
import static tools.APFloat.RoundingMode.rmNearestTiesToEven;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantFP extends Constant
{
    private APFloat val;

    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param ty
     * @param v
     */
    ConstantFP(Type ty, APFloat v)
    {
        super(ty, ValueKind.ConstantFPVal);
        Util.assertion(v.getSemantics() == typeToFloatSemantics(ty), "FP type mismatch!");
        val = v.clone();
    }

    private static FltSemantics typeToFloatSemantics(Type ty)
    {
        if (ty.equals(LLVMContext.FloatTy))
            return APFloat.IEEEsingle;
        if (ty.equals(LLVMContext.DoubleTy))
            return APFloat.IEEEdouble;
        if (ty.equals(LLVMContext.X86_FP80Ty))
            return APFloat.x87DoubleExtended;
        if (ty.equals(LLVMContext.FP128Ty))
            return APFloat.IEEEquad;

        Util.assertion(false, "Unkown FP format");
        return null;
    }

    public static Constant get(Type ty, double v)
    {
        APFloat fv = new APFloat(v);
        OutRef<Boolean> ignored = new OutRef<>();
        fv.convert(typeToFloatSemantics(ty.getScalarType()),
                rmNearestTiesToEven, ignored);

        return get(fv);
    }

    public static ConstantFP get(Type ty, APFloat v)
    {
        return new ConstantFP(ty, v);
    }

    public static ConstantFP get(APFloat flt)
    {
        APFloatKeyType key = new APFloatKeyType(flt);
        return UniqueConstantValueImpl.getUniqueImpl().getOrCreate(key);
    }

    @Override
    public boolean isNullValue()
    {
        // positive zero
        return val.isZero() &&!val.isNegative();
    }

    public static Constant get(APSInt complexIntReal)
    {
        // TODO: 17-11-28
        return null;
    }

    public double getValue()
    {
        return val.convertToDouble();
    }

    public APFloat getValueAPF()
    {
        return val;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (obj == this)
            return true;

        if (getClass() != obj.getClass())
            return false;
        ConstantFP o = (ConstantFP)obj;
        return new APFloatKeyType(val).equals(new APFloatKeyType(o.val));
    }

    public static boolean isValueValidForType(Type ty, APFloat val)
    {
        APFloat val2 = new APFloat(val);
        OutRef<Boolean> loseInfo = new OutRef<>(false);

        switch (ty.getTypeID())
        {
            default: return false;
            case FloatTyID:
            {
                if (val2.getSemantics() == APFloat.IEEEsingle)
                    return true;
                val2.convert(APFloat.IEEEsingle, rmNearestTiesToEven, loseInfo);
                return !loseInfo.get();
            }
            case DoubleTyID:
            {
                if (val2.getSemantics() == APFloat.IEEEsingle ||
                        val2.getSemantics() == APFloat.IEEEdouble)
                    return true;

                val2.convert(APFloat.IEEEdouble, rmNearestTiesToEven, loseInfo);
                return !loseInfo.get();
            }
            case X86_FP80TyID:
                return val2.getSemantics() == APFloat.IEEEsingle ||
                        val2.getSemantics() == APFloat.IEEEdouble ||
                        val2.getSemantics() == APFloat.x87DoubleExtended;
            case FP128TyID:
                return val2.getSemantics() == APFloat.IEEEsingle ||
                        val2.getSemantics() == APFloat.IEEEdouble ||
                        val2.getSemantics() == APFloat.IEEEquad;
        }
    }
}
