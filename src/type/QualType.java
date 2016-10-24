package type;

import ast.Tree;
import sema.APInt;
import sema.Decl;
import sema.Decl.RecordDecl;
import sema.Decl.TypedefNameDecl;
import utils.Util;
import type.ArrayType.*;

/**
 * This class represents a wrapper which combines CV-qualified types as nodes.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class QualType extends Type implements Cloneable
{
    public static final int CONST_QUALIFIER = 0x1;
    public static final int VOLATILE_QUALIFIER = 0x2;
    public static final int RESTRICT_QUALIFIER = 0x4;
    private static final int MASK =
            CONST_QUALIFIER | VOLATILE_QUALIFIER | RESTRICT_QUALIFIER;

    public static class Qualifier
    {
        int mask;

        void addCVQualifier(int flags)
        {
            assert (flags & ~MASK)
                    == 0 : "bitmask contains non-Type-Qualifiers";
            mask |= flags;
        }

        boolean isCVQualified(int flag)
        {
            return (mask & flag) != 0;
        }

        void removeCVQualified(int flag)
        {
            mask &= ~flag;
        }

        @Override
        public boolean equals(Object rhs)
        {
            if (rhs == null) return false;
            if (this == rhs) return true;
            if (!(rhs instanceof Qualifier))
                return false;

            Qualifier quals = (Qualifier)rhs;
            return mask == quals.mask;
        }

        public static Qualifier fromCVRMask(int cvr)
        {
            Qualifier res = new Qualifier();
            res.addCVQualifier(cvr);
            return res;
        }
    }

    private Type type;
    private Qualifier qualsFlag;

    public QualType()
    {
        super(0);
    }

    public QualType(final Type t)
    {
        this(t, 0);
    }

    public QualType(final Type t, int quals)
    {
        super(0);
        type = t;
        qualsFlag.addCVQualifier(quals);
    }

    public Type getType()
    {
        return type;
    }

    /**
     * ReturnStmt true if the underlying type is null.
     *
     * @return
     */
    public boolean isNull()
    {
        return type == null;
    }

    /**
     * Determines if this type is const-qualified.
     *
     * @return
     */
    public boolean isConstQualifed()
    {
        return qualsFlag.isCVQualified(CONST_QUALIFIER);
    }

    /**
     * Determines whether this type is restrict-qualified.
     *
     * @return
     */
    public boolean isRestrictQualified()
    {
        return qualsFlag.isCVQualified(RESTRICT_QUALIFIER);
    }

    /**
     * Checks if this type is static-qualified.
     *
     * @return
     */
    public boolean isStaticQualified()
    {
        return qualsFlag.isCVQualified(VOLATILE_QUALIFIER);
    }

    public void removeConstQualified()
    {
        qualsFlag.removeCVQualified(CONST_QUALIFIER);
    }

    public void removeVolatileQualified()
    {
        qualsFlag.removeCVQualified(VOLATILE_QUALIFIER);
    }

    public void removeRestrictQualified()
    {
        qualsFlag.removeCVQualified(RESTRICT_QUALIFIER);
    }

    /**
     * Checks if this type is qualified with type-qualifiers.
     *
     * @return
     */
    public boolean hasQualifiers()
    {
        return isConstQualifed() || isRestrictQualified()
                || isStaticQualified();
    }

    public boolean isPromotableIntegerType()
    {
        if (type instanceof PrimitiveType)
        {
            switch (type.tag)
            {
                case TypeClass.Bool:
                case TypeClass.Char:
                case TypeClass.UnsignedChar:
                case TypeClass.Short:
                case TypeClass.UnsignedShort:
                    return true;
                default:
                    return false;
            }
        }

        // Enumerated types are promotable to their compatible integer types
        // (C99 6.3.1.1)
        if (type.isEnumType())
        {
            EnumType et = type.getEnumType();
            QualType qt = et.getDecl().getIntegerType();
            if (qt.isNull())
                return false;

            return qt.getType().tag == TypeClass.Int
                    || qt.getType().tag == TypeClass.UnsignedInt;
        }
        return false;
    }

    public long getTypeSize()
    {
        return type.getTypeSize();
    }

    /**
     * Returns the type that promotable type promote to.
     * <br>
     * C99 6.3.1.1p2, assuming that promotable is promotable to integer.
     *
     * @return
     */
    public QualType getPromotedIntegerType()
    {
        assert !isNull() : "promotable can not be null!";
        assert isPromotableIntegerType();

        if (type instanceof EnumType)
            return ((EnumType) type).getDecl().getIntegerType();
        if (type.isSignedType())
            return IntTy;
        long promotableSize = type.getTypeSize();
        long intSize = IntTy.getTypeSize();
        assert !type.isSignedType() && promotableSize <= intSize;

        return promotableSize != intSize ? IntTy : UnsignedIntTy;
    }

    public boolean isScalarType()
    {
        return type.isScalarType();
    }

    public static QualType getPointerType(Type ty)
    {
        PointerType New = new PointerType(ty);
        return new QualType(New, 0);
    }

    /**
     * Returns the probably qualified result of specified array decaying into pointer
     * type.
     *
     * @param ty
     * @return
     */
    public static QualType getArrayDecayedType(QualType ty)
    {
        final ArrayType arrayType = ty.getAsArrayType(ty);
        assert arrayType != null : "Not an array type!";

        QualType ptrTy = getPointerType(arrayType.getElemType());

        // int x[restrict 4]-> int *restrict;
        // TODO unhandle type qualifier in array size expression.
        return ptrTy;
    }

    /**
     * Indicates the number of memory spaces in bytes.
     *
     * @return
     */
    public long allocSize()
    {
        return type.getTypeSize();
    }

    /**
     * The getSize of memory alignment in bytes.
     *
     * @return
     */
    public long alignment()
    {
        return type.alignment();
    }

    /*
     * Checks if this the kind of this type is same as other.
     */
    public boolean isSameType(Type other)
    {
        return type.isSameType(other);
    }

    /**
     * Checks if this {@linkplain Type} is primitive type.
     *
     * @return
     */
    public boolean isPrimitiveType()
    {
        return type.isPrimitiveType();
    }

    /**
     * Returns true if this type is void.
     *
     * @return
     */
    public boolean isVoidType()
    {
        return type.isVoidType();
    }

    /**
     * Returns true if this tpe is integral type.
     *
     * @return
     */
    public boolean isIntegerType()
    {
        return type.isIntegerType();
    }

    /**
     * Returns true if this type is real type.
     */
    public boolean isRealType()
    {
        return type.isRealType();
    }

    /**
     * Returns true if this type is complex type.
     *
     * @return
     */
    public boolean isComplexType()
    {
        return type.isComplexType();
    }

    /**
     * Returns true if this type is boolean type.
     *
     * @return
     */
    public boolean isBooleanType()
    {
        return type.isBooleanType();
    }

    /**
     * Checks whether this type is integral and qualified with signed.
     *
     * @return
     * @throws Error
     */
    public boolean isSignedType()
    {
        return type.isSignedType();
    }

    /**
     * Checks if this type is a pointer to actual type object.
     *
     * @return
     */
    public boolean isPointerType()
    {
        return type.isPointerType();
    }

    /**
     * Checks if this type is reference type.
     *
     * @return
     */
    public boolean isReferenceType()
    {
        return type.isReferenceType();
    }

    /**
     * Checks if this type is a formal function type in C or static member function type in C++.
     *
     * @return
     */
    public boolean isFunctionType()
    {
        return type.isFunctionType();
    }

    /**
     * Checks if this type is member function type of a class in C++.
     *
     * @return
     */
    public boolean isMethodType()
    {
        return type.isMethodType();
    }

    /**
     * Checks if this type is array type.
     *
     * @return
     */
    public boolean isArrayType()
    {
        return type.isArrayType();
    }

    /**
     * Determine whether this type is record type or not.
     *
     * @return return true if it is record, otherwise return false.
     */
    public boolean isRecordType()
    {
        return type.isRecordType();
    }

    /**
     * Checks if this type is enumeration type.
     *
     * @return
     */
    public boolean isEnumType()
    {
        return type.isEnumType();
    }

    /**
     * Checks if this type is type-name type.
     *
     * @return
     */
    public boolean isUserType()
    {
        return type.isUserType();
    }

    // Ability methods (unary)
    public boolean isAllocatedArray()
    {
        return type.isAllocatedArray();
    }

    public boolean isIncompleteArrayArray()
    {
        return type instanceof IncompleteArrayType;
    }

    public boolean isCallable()
    {
        return type.isCallable();
    }

    /**
     * Indicates if this type can be wrapped with other type.
     *
     * @param other
     * @return
     */
    public boolean isCompatible(QualType other)
    {
        return !mergeType(this, other, false).isNull();
    }

    private QualType mergeType(QualType lhs, QualType rhs, boolean unqualified)
    {
        if (unqualified)
        {
            lhs = lhs.getUnQualifiedType();
            rhs = rhs.getUnQualifiedType();
        }

        // If two types are identical. they are compatible.
        if (lhs.equals(rhs))
            return lhs;

        // If the qualifiers are different, the types aren't compatible...
        Qualifier lQuals = lhs.qualsFlag;
        Qualifier rQuals = rhs.qualsFlag;

        // If the type qualifiers are different, we get a mismatch.
        if (lQuals.equals(rQuals))
        {
            return new QualType();
        }

        // Get the point, qualifiers are equal.
        int lhsClass = lhs.getTypeClass();
        int rhsClass = rhs.getTypeClass();


        if (lhsClass == TypeClass.VariableArray)
            lhsClass = TypeClass.ConstantArray;
        if (rhsClass == VariableArray)
            rhsClass = ConstantArray;

        // If the type classes don't match.
        if (lhsClass != rhsClass)
        {
            // C99 6.7.2.2p4: Each enumerated type shall be compatible with char,
            // a signed integer type, or an unsigned integer type.
            // Compatibility is based on the underlying type, not the promotion
            // type.
            EnumType ety = lhs.getEnumType();
            if (ety != null)
            {
                if (ety.getDecl().getIntegerType().equals(rhs.getUnQualifiedType()))
                    return rhs;
            }
            ety = rhs.getEnumType();
            if (ety != null)
            {
                if (ety.getDecl().getIntegerType().equals(lhs.getUnQualifiedType()))
                    return rhs;
            }

            return new QualType();
        }

        switch (lhsClass)
        {
            case VariableArray:
            case Function:
                Util.shouldNotReachHere("Types are eliminated abo e");

            case Pointer:
            {
                // Merge two pointer types, while trying to preserve typedef info.
                QualType lhsPointee = lhs.getPointerType().getPointeeType();
                QualType rhsPointee = rhs.getPointerType().getPointeeType();

                if (unqualified)
                {
                    lhsPointee = lhsPointee.getUnQualifiedType();
                    rhsPointee = rhsPointee.getUnQualifiedType();
                }
                QualType resultType = mergeType(lhsPointee, rhsPointee, unqualified);
                if (resultType.isNull()) return new QualType();
                if (lhsPointee.equals(resultType))
                    return lhs;
                if (rhsPointee.equals(resultType))
                    return rhs;
                return getPointerType(resultType);
            }
            case ConstantArray:
            {
                final ConstantArrayType lcat = lhs.getAsConstantArrayType();
                final ConstantArrayType rcat = rhs.getAsConstantArrayType();
                if (lcat != null && rcat != null && rcat.getSize().ne(lcat.getSize()))
                    return new QualType();

                QualType lhsElem = getAsArrayType(lhs).getElemType();
                QualType rhsElem = getAsArrayType(rhs).getElemType();

                if (unqualified)
                {
                    rhsElem = rhsElem.getUnQualifiedType();
                    lhsElem = lhsElem.getUnQualifiedType();
                }

                QualType resultType  = mergeType(lhsElem, rhsElem, unqualified);
                if (resultType.isNull()) return new QualType();

                if (lcat != null && lhsElem.equals(resultType))
                    return lhs;

                if (rcat!= null && rhsElem.equals(resultType))
                    return rhs;

                if (lcat != null)
                    return new QualType(getConstantArrayType(resultType, lcat.getSize()));

                if (rcat != null)
                    return new QualType(getConstantArrayType(resultType, rcat.getSize()));

                VariableArrayType lvat = lhs.getAsVariableArrayType();
                VariableArrayType rvat = rhs.getAsVariableArrayType();
                if (lvat != null && lhsElem.equals(resultType))
                    return lhs;

                if (rvat != null && rhsElem.equals(resultType))
                    return rhs;

                if (lvat != null)
                {
                    // FIXME: This isn't correct! But tricky to implement because
                    // the array's size has to be the size of LHS, but the type
                    // has to be different.
                    return lhs;
                }
                if (rcat != null)
                {
                    // FIXME: This isn't correct! But tricky to implement because
                    // the array's size has to be the size of LHS, but the type
                    // has to be different.
                    return rhs;
                }

                if (lhsElem.equals(resultType)) return lhs;
                if (rhsElem.equals(resultType)) return rhs;

                return new QualType(getIncompleteArrayType(resultType));
            }

            case TypeClass.Struct:
            case TypeClass.Union:
            case TypeClass.Enum:

            case UnsignedChar:
            case UnsignedShort:
            case UnsignedInt:
            case UnsignedLong:
            case Char:
            case Short:
            case LongInteger:
            case Real:
                return new QualType();
        }
        return new QualType();
    }

    public ConstantArrayType getAsConstantArrayType()
    {
        ArrayType res = getAsArrayType(this);
        return (res instanceof ConstantArrayType)
                ? (ConstantArrayType)res : null;
    }

    public IncompleteArrayType getAsInompleteArrayType()
    {
        ArrayType res = getAsArrayType(this);
        return (res instanceof IncompleteArrayType)
                ? (IncompleteArrayType)res : null;
    }

    public VariableArrayType getAsVariableArrayType()
    {
        ArrayType res = getAsArrayType(this);
        return (res instanceof VariableArrayType)
                ? (VariableArrayType)res : null;
    }

    public ArrayType getAsArrayType(QualType t)
    {
        // Handle the non-qualified case efficiently.
        if (!t.hasQualifiers())
        {
            if (t.getType() instanceof ArrayType)
                return (ArrayType)t.getType();
        }

        if (!(t.getType() instanceof ArrayType))
            return null;
        ArrayType aty = (ArrayType) t.getType();
        // Otherwise, we have an array and we have qualifiers on it.
        // Push qualifiers into the array element type and return a new array type.
        QualType newElemTy = getQualifiedType(aty.getElemType(), t.qualsFlag);

        if (aty instanceof ConstantArrayType)
        {
            ConstantArrayType cat = (ConstantArrayType)aty;
            return getConstantArrayType(newElemTy, cat.getSize());
        }

        if (aty instanceof IncompleteArrayType)
        {
            IncompleteArrayType icat = (IncompleteArrayType)aty;
            return getIncompleteArrayType(newElemTy);
        }

        VariableArrayType vat = (VariableArrayType)aty;
        return getVariableArrayType(newElemTy, vat.getSizeExpr());
    }

    public VariableArrayType getVariableArrayType(QualType elemTy, Tree.Expr sizeExpr)
    {
        return new VariableArrayType(elemTy, sizeExpr);
    }

    public ConstantArrayType getConstantArrayType(QualType elemTy, final APInt arraySize)
    {
        assert elemTy.isIncompleteType()||elemTy.isConstantSizeType()
                :"Constant array of VLAs is illegal";

        APInt size = new APInt(arraySize);
        size = size.zextOrTrunc(32);
        ConstantArrayType New = new ConstantArrayType(elemTy, size);
        return New;
    }

    public IncompleteArrayType getIncompleteArrayType(QualType elemTy)
    {
        return new IncompleteArrayType(elemTy);
    }

    public static QualType getQualifiedType(QualType t, Qualifier qs)
    {
        return new QualType(t.getType(), qs.mask);
    }

    public static QualType getUnQualifiedType(QualType t)
    {
        return t.getUnQualifiedType();
    }

    public QualType getUnQualifiedType()
    {
        if (hasQualifiers())
            return new QualType(type, 0);
        return this;
    }

    /**
     * Indicates if this type can be casted into target type.
     *
     * @param target
     * @return
     */
    public boolean isCastableTo(Type target)
    {
        return type.isCastableTo(target);
    }

    /**
     * @return
     */
    public Type baseType()
    {
        return type.baseType();
    }

    // Cast methods
    public IntegerType getIntegerType()
    {
        return type.getIntegerType();
    }

    public RealType getRealType()
    {
        return type.getRealType();
    }

    public ComplexType getComplexTye()
    {
        return type.getComplexTye();
    }

    public PointerType getPointerType()
    {
        return type.getPointerType();
    }

    public FunctionType getFunctionType()
    {
        return type.getFunctionType();
    }

    public RecordType getRecordType()
    {
        return type.getRecordType();
    }

    public EnumType getEnumType()
    {
        return type.getEnumType();
    }

    /**
     * Checks if this type is integral or enumeration.
     *
     * @return {@code true} returned if this type is integral or enumeration,
     * otherwise, return {@code false}.
     */
    public boolean isIntegralOrEnumerationType()
    {
        if (isIntegerType() || isBooleanType())
            return true;
        if (isEnumType())
            return getEnumType().getDecl().isCompleteDefinition();
        return false;
    }

    public int getTypeKind()
    {
        return type.getTypeKind();
    }

    public QualType clone()
    {
        return new QualType(type, qualsFlag.mask);
    }

    /**
     * Clones a new instance of class {@code QualType} with the template of this.
     * Then clear all of its type-qualifiers.
     * @return
     */
    public QualType clearQualified()
    {
        QualType ty = clone();
        ty.qualsFlag.removeCVQualified(MASK);
        return ty;
    }
    @Override
    public boolean equals(Object t1)
    {
        if (t1 == null) return false;
        if (t1 == this) return true;
        if (!(t1 instanceof QualType))
            return false;

        QualType temp = (QualType)t1;
        return temp.type.equals(type);
    }

    public boolean isCForbiddenLVaue()
    {
        return (type.isVoidType() && !hasQualifiers())
                || type.isFunctionType();
    }

    /**
     * Returns the reference to the type for the specified type declaration.
     * @param decl
     * @return
     */
    public static QualType getTypeDeclType(Decl.TypeDecl decl)
    {
        assert decl != null:"Passed null for decl param";
        if (decl.getTypeForDecl() != null)
            return new QualType(decl.getTypeForDecl());

        return getTypeDeclTypeSlow(decl);
    }

    public static QualType getTypeDeclTypeSlow(Decl.TypeDecl decl)
    {
        assert decl!= null:"Passed null for decl param";
        assert decl.getTypeForDecl() ==null:"TypeForDecl present in slow case";

        if (decl instanceof TypedefNameDecl)
        {
            return getTypeDefType((TypedefNameDecl) decl);
        }
        if (decl instanceof RecordDecl)
        {
            RecordDecl record = (RecordDecl)decl;
            return getRecordType(record);
        }
        if (decl instanceof Decl.EnumDecl)
        {
            Decl.EnumDecl d = (Decl.EnumDecl)decl;
            return getEnumType(d);
        }
        else
        {
            Util.shouldNotReachHere("TypeDecl without a type?");
        }
        return new QualType(decl.getTypeForDecl());
    }

    public static QualType getTypeDefType(TypedefNameDecl decl)
    {
        if (decl.getTypeForDecl() != null)
            return new QualType(decl.getTypeForDecl());

        TypeDefType newType = new TypeDefType(TypeClass.TypeDef, decl);
        decl.setTyepForDecl(newType);
        return new QualType(newType);
    }

    public static QualType getRecordType(RecordDecl record)
    {
        if (record.getTypeForDecl() != null)
            return new QualType(record.getTypeForDecl());

        RecordType newType = new RecordType(record);
        record.setTyepForDecl(newType);
        return new QualType(newType);
    }

    public static QualType getEnumType(Decl.EnumDecl decl)
    {
        if (decl.getTypeForDecl() != null)
            return new QualType(decl.getTypeForDecl());

        EnumType newType = new EnumType(decl);
        decl.setTyepForDecl(newType);
        return new QualType(newType);
    }

    public boolean isIncompleteOrObjectType()
    {
        return !isFunctionType();
    }

    public static QualType getBaseElementType(QualType type)
    {
        Qualifier qs = new Qualifier();
        while (true)
        {
            if (!type.isArrayType())
                break;

            type = type.getAsArrayType(type).getElemType();
            qs.addCVQualifier(type.qualsFlag.mask);
        }
        return getQualifiedType(type, qs);
    }

    /**
     * A generic method for casting a type instance to target type.
     * If casting failed, return null, otherwise, return the instance
     * type required type.
     * @param <T>
     * @return
     */
    public <T extends Type> T getAs()
    {
        return QualType.convertInstanceOfObject(this, getClass());
    }

    public static  <T extends Type> T convertInstanceOfObject(
            Object o,
            Class<? extends Type> clazz)
    {
        try
        {
            return (T) clazz.cast(o);
        }
        catch (ClassCastException e)
        {
            return null;
        }
    }
}
