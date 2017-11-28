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

import backend.support.LLVMContext;
import backend.type.Type;
import jlang.type.FoldingSetNodeID;
import tools.APFloat;
import tools.APSInt;
import tools.FltSemantics;
import tools.OutParamWrapper;

import java.util.HashMap;

import static backend.type.LLVMTypeID.*;
import static tools.APFloat.RoundingMode.rmNearestTiesToEven;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantFP extends Constant
{
    private static class APFloatKeyInfo
    {
        private APFloat flt;
        public APFloatKeyInfo(APFloat flt)
        {
            this.flt = flt;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (this == obj)
                return true;
            if (getClass() != obj.getClass())
                return false;
            APFloatKeyInfo key = (APFloatKeyInfo)obj;
            return flt.bitwiseIsEqual(key.flt);
        }

        @Override
        public int hashCode()
        {
            FoldingSetNodeID id = new FoldingSetNodeID();
            id.addString(flt.toString());
            return id.computeHash();
        }
    }

    private APFloat val;

    private static final HashMap<APFloatKeyInfo, ConstantFP> FPConstants =
            new HashMap<>();

    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param ty
     * @param v
     */
    private ConstantFP(Type ty, APFloat v)
    {
        super(ty, ValueKind.ConstantFPVal);
        val = v;
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

        assert false:"Unkown FP format";
        return null;
    }

    private static Type floatSemanticsToType(FltSemantics semantics)
    {
        if (semantics == APFloat.IEEEsingle)
            return LLVMContext.FloatTy;
        if (semantics == APFloat.IEEEdouble)
            return LLVMContext.DoubleTy;
        if (semantics == APFloat.x87DoubleExtended)
            return LLVMContext.X86_FP80Ty;
        if (semantics == APFloat.IEEEquad)
            return LLVMContext.FP128Ty;

        assert false:"Unknown FP format";
        return null;
    }

    public static Constant get(Type ty, double v)
    {
        APFloat fv = new APFloat(v);
        OutParamWrapper<Boolean> ignored = new OutParamWrapper<>();
        fv.convert(typeToFloatSemantics(ty.getScalarType()),
                rmNearestTiesToEven, ignored);

        return get(fv);
    }

    public static ConstantFP get(Type ty, APFloat v)
    {
        return new ConstantFP(ty, v);
    }

    public static Constant get(APFloat flt)
    {
        ConstantFP fp;
        APFloatKeyInfo key = new APFloatKeyInfo(flt);
        if (!FPConstants.containsKey(key))
        {
            Type ty = floatSemanticsToType(flt.getSemantics());
            fp = new ConstantFP(ty, flt);
            FPConstants.put(key, fp);
        }
        else
        {
            fp = FPConstants.get(key);
        }
        return fp;
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
        return new APFloatKeyInfo(val).equals(new APFloatKeyInfo(o.val));
    }

    public static boolean isValueValidForType(Type ty, APFloat val)
    {
        APFloat val2 = new APFloat(val);
        OutParamWrapper<Boolean> loseInfo = new OutParamWrapper<>(false);

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
