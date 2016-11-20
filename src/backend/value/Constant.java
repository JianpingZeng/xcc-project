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

import backend.hir.InstVisitor;
import backend.lir.ci.LIRConstant;
import backend.type.IntegerType;
import backend.type.PointerType;
import backend.type.Type;
import frontend.sema.APInt;

import java.math.BigDecimal;

/**
 * The {@code Constant} instruction represents a constants such as an integer
 * inst, long, float, object reference, address, etc.
 */
public abstract class Constant extends User
{
    public static backend.value.Constant CONSTANT_INT_0 = backend.value.Constant
            .forInt(0);
    public static backend.value.Constant CONSTANT_INT_1 = backend.value.Constant
            .forInt(1);
    public static backend.value.Constant CONSTANT_INT_MINUS_1 = backend.value.Constant
            .forInt(-1);

    /**
     * The constants inst keeped with {@code Constant} instance.
     */
    public LIRConstant value;

    /**
     * Constructs a new instruction representing the specified constants.
     */
    public Constant(Type ty, int valueKind)
    {
        super(ty, valueKind);
    }

    public void setValue(LIRConstant value)
    {
        this.value = value;
    }

    /**
     * Creates an instruction for a double constants.
     *
     * @param d the double inst for which to create the instruction
     * @return an instruction representing the double
     */
    public static backend.value.Constant forDouble(double d)
    {
        return new backend.value.Constant(LIRConstant.forDouble(d));
    }

    /**
     * Creates an instruction for a float constants.
     *
     * @param f the float inst for which to create the instruction
     * @return an instruction representing the float
     */
    public static backend.value.Constant forFloat(float f)
    {
        return new backend.value.Constant(LIRConstant.forFloat(f));
    }

    /**
     * Creates an instruction for an long constants.
     *
     * @param i the long inst for which to create the instruction
     * @return an instruction representing the long
     */
    public static backend.value.Constant forLong(long i)
    {
        return new backend.value.Constant(LIRConstant.forLong(i));
    }

    /**
     * Creates an instruction for an integer constants.
     *
     * @param i the integer inst for which to create the instruction
     * @return an instruction representing the integer
     */
    public static backend.value.Constant forInt(int i)
    {
        return new backend.value.Constant(LIRConstant.forInt(i));
    }

    /**
     * Creates an instruction for a boolean constants.
     *
     * @param i the boolean inst for which to create the instruction
     * @return an instruction representing the boolean
     */
    public static backend.value.Constant forBoolean(boolean i)
    {
        return new backend.value.Constant(LIRConstant.forBoolean(i));
    }

    /**
     * Creates an instruction for an object constants.
     *
     * @param o the object inst for which to create the instruction
     * @return an instruction representing the object
     */
    public static backend.value.Constant forObject(Object o)
    {
        return new backend.value.Constant(LIRConstant.forObject(o));
    }

    public String toString()
    {
        return super.toString() + "(" + value + ")";
    }

    public int valueNumber()
    {
        return 0x50000000 | value.hashCode();
    }

    public backend.value.Constant clone()
    {
        return new backend.value.Constant(this.value);
    }

    public void accept(InstVisitor visitor)
    {
        visitor.visitConstant(this);
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null)
            return false;
        if (other == this)
            return true;
        if (!(other instanceof backend.value.Constant))
            return false;
        backend.value.Constant c = (backend.value.Constant) other;
        return c.value.equals(c.value);
    }

    /**
     * ReturnInst the production getReturnValue of both Constant, c1 and c2.
     *
     * @param c1
     * @param c2
     */
    public static backend.value.Constant multiple(backend.value.Constant c1,
            backend.value.Constant c2)
    {
        assert c1.kind.isPrimitive() && c2.kind
                .isPrimitive() : "No non-primitive frontend.type allowed for induction variable";
        long l1 = c1.value.asPrimitive();
        long l2 = c2.value.asPrimitive();

        return backend.value.Constant.forLong(l1 * l2);
    }

    /**
     * ReturnInst the sum of both Constant, c1 and c2.
     *
     * @param c1
     * @param c2
     */
    public static backend.value.Constant add(backend.value.Constant c1,
            backend.value.Constant c2)
    {
        assert c1.kind.isPrimitive() && c2.kind
                .isPrimitive() : "No non-primitive frontend.type allowed for induction variable";
        long l1 = c1.value.asPrimitive();
        long l2 = c2.value.asPrimitive();

        return backend.value.Constant.forLong(l1 + l2);
    }

    public static backend.value.Constant sub(backend.value.Constant c1,
            backend.value.Constant c2)
    {
        assert c1.kind.isPrimitive() && c2.kind
                .isPrimitive() : "No non-primitive frontend.type allowed for induction variable";
        long l1 = c1.value.asPrimitive();
        long l2 = c2.value.asPrimitive();

        return backend.value.Constant.forLong(l1 - l2);
    }

    public static backend.value.Constant sub(int c1, backend.value.Constant c2)
    {
        assert c2.kind
                .isPrimitive() : "No non-primitive frontend.type allowed for induction variable";
        long l2 = c2.value.asPrimitive();

        return backend.value.Constant.forLong(c1 - l2);
    }

    public static Constant getNullValue(Type type)
    {
        switch (type.getPrimitiveID())
        {
            case Type.Int1TyID:
            case Type.Int8TyID:
            case Type.Int16TyID:
            case Type.Int32TyID:
            case Type.Int64TyID:
                return ConstantInt.get((IntegerType) type, 0);

            case Type.FloatTyID:
                return ConstantFP.get(Type.FloatTy, BigDecimal.ZERO);
            case Type.DoubleTyID:
                return ConstantFP.get(Type.DoubleTy,BigDecimal.ZERO);
            case Type.PointerTyID:
                return ConstantPointerNull.get((PointerType)type);
            case Type.StructTyID:
            case Type.ArrayTyID:
                return ConstantAggregateZero.get(type);
            default:
                return null;
        }
    }

    public abstract boolean isNullValue();

    public static Constant getAllOnesValue(Type ty)
    {
        if (ty instanceof IntegerType)
        {
            APInt val = APInt.getAllOnesValue(((IntegerType) ty).getBitWidth());
            return ConstantInt.get(val);
        }
        return null;
    }
}
