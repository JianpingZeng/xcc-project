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
    public static final OtherType VoidTy = new OtherType("void", Type.VoidTyID);
    public static final OtherType BoolTy = new OtherType("bool", Type.BoolTyID);
    public static final SignedIntType SByteTy = new SignedIntType("sbyte", Type.SByteTyID);
    public static final UnsignedIntType UByteTy = new UnsignedIntType("ubyte", Type.UByteTyID);
    public static final SignedIntType ShortTy = new SignedIntType("short", Type.ShortTyID);
    public static final UnsignedIntType UShortTy = new UnsignedIntType("ushort", Type.UShortTyID);
    public static final SignedIntType IntTy = new SignedIntType("int", Type.IntTyID);
    public static final UnsignedIntType UIntTy = new UnsignedIntType("uint", Type.UIntTyID);
    public static final SignedIntType LongTy = new SignedIntType("long", Type.LongTyID);
    public static final UnsignedIntType ULongTy = new UnsignedIntType("ulong", Type.ULongTyID);

    public static final OtherType FloatTy = new OtherType("float", Type.FloatTyID);
    public static final OtherType DoubleTy = new OtherType("double", Type.DoubleTyID);
    public static final OtherType LabelTy = new OtherType("label", Type.LabelTyID);


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
            case BoolTyID: return BoolTy;
            case UByteTyID: return UByteTy;
            case SByteTyID: return SByteTy;
            case UShortTyID: return UShortTy;
            case ShortTyID: return ShortTy;
            case UIntTyID: return UIntTy;
            case IntTyID: return IntTy;
            case ULongTyID: return ULongTy;
            case LongTyID: return LongTy;
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

            case BoolTyID:
            case SByteTyID:
            case UByteTyID:
                return 1;
            case ShortTyID:
            case UShortTyID:
                return 2;
            case IntTyID:
            case UIntTyID:
                return 4;
            case LongTyID:
            case ULongTyID:
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

    public boolean isInteger() {return false;}

    public boolean isIntegral()
    {
        return isInteger() || this == BoolTy;
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

    public boolean isVoidType() { return id == VoidTyID;}
    /**
     * Checks if this type could holded in register.
     * @return
     */
    public boolean isHoldableInRegister()
    {
        return isPrimitiveType() || id == PointerTyID;
    }
}
