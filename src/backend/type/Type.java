package backend.type;
/*
 * Xlous C language Compiler
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

import backend.value.Value;
import backend.value.ValueKind;

import java.util.HashMap;

/**
 * This is a core base class for representing backend type of value.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class Type extends Value implements PrimitiveID
{
    public static final OtherType VoidTy = OtherType.getVoidType();
    public static final IntegerType Int1Ty = IntegerType.get(1);

    public static final IntegerType Int8Ty = IntegerType.get(8);
    public static final IntegerType Int16Ty = IntegerType.get(16);
    public static final IntegerType Int32Ty = IntegerType.get(32);
    public static final IntegerType Int64Ty = IntegerType.get(64);

    public static final OtherType FloatTy = OtherType.getFloatType(32);
    public static final OtherType DoubleTy = OtherType.getFloatType(64);
    public static final OtherType LabelTy = OtherType.getLabelType();

    public static class TypeType extends Type
    {
        TypeType()
        {
            super("type", Type.TypeTyID);
        }
    }

    public static final TypeType TypeTy = new TypeType();

    /**
     * The current base type of this type.
     */
    private int id;
    private static HashMap<Type, String> concreteTypeDescription;
    static
    {
        concreteTypeDescription = new HashMap<>();
    }

    protected Type(String name, int primitiveID)
    {
        super(TypeTy, ValueKind.TypeVal);
        if (name !=null && !name.isEmpty())
            concreteTypeDescription.put(this, name);
        id = primitiveID;
    }

    public static Type getPrimitiveType(int primitiveID)
    {
        switch (primitiveID)
        {
            case VoidTyID: return VoidTy;
            case Int1TyID: return Int1Ty;
            case Int8TyID: return Int8Ty;
            case Int16TyID: return Int16Ty;
            case Int32TyID: return Int32Ty;
            case Int64TyID: return Int64Ty;
            case FloatTyID: return FloatTy;
            case DoubleTyID: return DoubleTy;
            case LabelTyID: return LabelTy;
            default:
                return null;
        }
    }

    public int getPrimitiveID()
    {
        return id;
    }

    public int getPrimitiveSize()
    {
        switch (id)
        {
            case VoidTyID:
            case TypeTyID:
            case LabelTyID:
                return 0;

            case Int1TyID:
            case Int8TyID:
                return 1;
            case Int16TyID:
                return 2;
            case Int32TyID:
                return 4;
            case Int64TyID:
                return 8;
            case FloatTyID:
                return 4;
            case DoubleTyID:
                return 8;
            default:
                return 0;
        }
    }

    public boolean isSigned() {return false;}

    public boolean isUnsigned() {return false;}

    public boolean isIntegerType() {return false;}

    public boolean isIntegral()
    {
        return isIntegerType() || this == Int1Ty;
    }

    public boolean isPrimitiveType()
    {
        return id< FirstDerivedTyID;
    }

    public boolean isDerivedType()
    {
        return id >= FirstDerivedTyID;
    }

    public boolean isFunctionType()
    {
        return id == FunctionTyID;
    }

    public boolean isArrayType() { return id == ArrayTyID;}

    public boolean isPointerType() { return id == PointerTyID;}

    public boolean isStructType() { return id == StructTyID;}

    public boolean isVoidType() { return id == VoidTyID;}

    public boolean isFloatingPointType() { return id == FloatTyID || id == DoubleTyID;}

    /**
     * Return true if the type is "first class", meaning it
     * is a valid type for a Value.
     * @return
     */
    public boolean isFirstClassType()
    {
        return id != FunctionTyID && id != VoidTyID;
    }
    /**
     * Checks if this type could holded in register.
     * @return
     */
    public boolean isHoldableInRegister()
    {
        return isPrimitiveType() || id == PointerTyID;
    }

    /**
     * Return true if it makes sense to take the size of this type.
     * To get the actual size for a particular target, it is reasonable
     * to use the TargetData subsystem to do that.
     * @return
     */
    public boolean isSized()
    {
        if ((id >= Int1TyID && id <= Int64TyID)
                || isFloatingPointType() || id == PointerTyID)
            return true;

        if (id != StructTyID && id != ArrayTyID)
            return false;
        // Otherwise we have to try harder to decide.
        return isSizedDerivedType();
    }

    /**
     * Returns true if the derived type is sized.
     * DerivedType is sized if and only if all members of it are sized.
     * @return
     */
    private boolean isSizedDerivedType()
    {
        if (isIntegerType())
            return true;

        if (isArrayType())
        {
            return ((ArrayType)this).getElemType().isSized();
        }
        if (!isStructType())
            return false;
        StructType st = (StructType)this;
        for (Type type : st.getElementTypes())
            if (!type.isSized())
                return false;

        return true;
    }

    public int getScalarSizeBits()
    {
        return getPrimitiveSize() << 3;
    }
}
