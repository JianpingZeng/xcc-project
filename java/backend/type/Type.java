package backend.type;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2017, Xlous
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

import backend.hir.Module;
import backend.support.TypePrinting;
import tools.Util;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This is a core base class for representing backend type of value.
 * <p>
 * The instances of the Type class are immutable: once they are created,
 * they are never changed.  Also note that only one instance of a particular
 * type is ever created.  Thus seeing if two types are equal is a matter of
 * doing a trivial pointer comparison. To enforce that no two equal instances
 * are created, Type instances can only be created via static factory methods
 * in class Type and in derived classes.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public class Type implements LLVMTypeID, AbstractTypeUser
{
    public static final Type VoidTy = new Type(VoidTyID);
    public static final Type LabelTy = new Type(LabelTyID);

    public static final IntegerType Int1Ty = IntegerType.get(1);
    public static final IntegerType Int8Ty = IntegerType.get(8);
    public static final IntegerType Int16Ty = IntegerType.get(16);
    public static final IntegerType Int32Ty = IntegerType.get(32);
    public static final IntegerType Int64Ty = IntegerType.get(64);

    public static final Type FloatTy = new Type(FloatTyID);
    public static final Type DoubleTy = new Type(DoubleTyID);
    public static final Type FP128Ty = new Type(FP128TyID);
    public static final Type X86_FP80Ty = new Type(X86_FP80TyID);

    /**
     * The current base type of this type.
     */
    private int id;
    protected boolean isAbstract;

    /**
     * Implement a list of the users that need to be notified if i am a type, and
     * i get resolved into a more concrete type.
     */
    protected LinkedList<AbstractTypeUser> abstractTypeUsers;

    private static HashMap<Type, String> concreteTypeDescription;
    static
    {
        concreteTypeDescription = new HashMap<>();
    }

    protected Type(int typeID)
    {
        id = typeID;
        isAbstract = false;
        abstractTypeUsers = new LinkedList<>();
    }

    public void setAbstract(boolean anAbstract)
    {
        isAbstract = anAbstract;
    }

    public boolean isAbstract()
    {
        return isAbstract;
    }

    public int getTypeID()
    {
        return id;
    }

    public void print(PrintStream os)
    {
        new TypePrinting().print(this, os);
    }

    public void dump()
    {
        dump(null);
    }

    public void dump(Module context)
    {
        // TODO: 17-6-11 writeTypeSymbolic(System.err, this, context);
        //
        System.err.println();
    }

    public String getDescription()
    {
        switch (getTypeID())
        {
            case VoidTyID:
                return "void";
            case IntegerTyID:
                return "i" + ((IntegerType)this).getBitWidth();
            case FloatTyID:
                return "f32";
            case DoubleTyID:
                return "f64";
            case LabelTyID:
                return "label";
            default:
                return "<unkown type>";
        }
    }

    public boolean isInteger()
    {
        return id == IntegerTyID;
    }

    public boolean isFloatingPoint()
    {
        return id == FloatTyID || id == DoubleTyID
                || id == X86_FP80TyID || id == FP128TyID;
    }

    public int getPrimitiveSizeInBits()
    {
        switch (getTypeID())
        {
            case FloatTyID: return 32;
            case DoubleTyID: return 64;
            case X86_FP80TyID: return 80;
            case FP128TyID: return 128;
            case IntegerTyID: return ((IntegerType)this).getBitWidth();
            default: return 0;
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
    */
    public boolean isFirstClassType()
    {
        // There are more first-class kinds than non-first-class kinds, so a
        // negative test is simpler than a positive one.
        return id != FunctionTyID && id != VoidTyID && id != OpaqueTyID;
    }
	/**
     * Return true if the type is a valid type for a virtual register in codegen.
     * This include all first-class type except struct and array type.
     * @return
     */
    public boolean isSingleValueType()
    {
        return id != VoidTyID && id <= LastPrimitiveTyID
                || (id>= IntegerTyID && id<= IntegerTyID) ||
                id == PointerTyID;
    }

	/**
     * Return true if the type is an aggregate type. it means it is valid as
     * the first operand of an insertValue or extractValue instruction.
     * This includes struct and array types.
     * @return
     */
    public boolean isAggregateType()
    {
        return id == StructTyID || id == ArrayTyID;
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
     * Return true if it makes sense to take the getNumOfSubLoop of this type.
     * To get the actual getNumOfSubLoop for a particular TargetData, it is reasonable
     * to use the TargetData subsystem to do that.
     * @return
     */
    public boolean isSized()
    {
        if ((id >= IntegerTyID && id <= IntegerTyID)
                || isFloatingPointType() || id == PointerTyID)
            return true;

        if (id != StructTyID && id != ArrayTyID)
            return false;
        // Otherwise we have to try harder to decide.
        return isSizedDerivedType() || !isAbstract();
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

    public Type getScalarType()
    {
        return this;
    }

    public int getScalarSizeBits()
    {
        return getScalarType().getPrimitiveSizeInBits();
    }

    @Override
    public void refineAbstractType(DerivedType oldTy, Type newTy)
    {
        Util.shouldNotReachHere("Attempting to refine a derived type!");
    }

    @Override
    public void typeBecameConcrete(DerivedType absTy)
    {
        Util.shouldNotReachHere("DerivedType is already a concrete type!");
    }
}
