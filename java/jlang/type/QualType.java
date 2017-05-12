package jlang.type;

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
        public int mask;

        public void addCVQualifier(int flags)
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

        public Qualifier add(Qualifier other)
        {
            addCVQualifier(other.mask);
            return this;
        }
    }

    private Type type;

    public Qualifier qualsFlag;

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
     * ReturnStmt true if the underlying jlang.type is null.
     *
     * @return
     */
    public boolean isNull()
    {
        return type == null;
    }

    /**
     * Determines if this jlang.type is const-qualified.
     *
     * @return
     */
    public boolean isConstQualifed()
    {
        return qualsFlag.isCVQualified(CONST_QUALIFIER);
    }

    /**
     * Determines whether this jlang.type is restrict-qualified.
     *
     * @return
     */
    public boolean isRestrictQualified()
    {
        return qualsFlag.isCVQualified(RESTRICT_QUALIFIER);
    }

    /**
     * Checks if this type is volatile-qualified.
     *
     * @return
     */
    public boolean isVolatileQualified()
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

    public int getCVRQualifiers()
    {
        return qualsFlag.mask;
    }

    public void setCVRQualifiers(int flags)
    {
        qualsFlag.addCVQualifier(flags);
    }

    public Qualifier getQualifiers()
    {
        return qualsFlag;
    }

    /**
     * Checks if this jlang.type is qualified with jlang.type-qualifiers.
     *
     * @return
     */
    public boolean hasQualifiers()
    {
        return isConstQualifed() || isRestrictQualified()
                || isVolatileQualified();
    }

    public boolean isScalarType()
    {
        return type.isScalarType();
    }

    /**
     * Checks if this {@linkplain Type} is primitive jlang.type.
     *
     * @return
     */
    public boolean isPrimitiveType()
    {
        return type.isPrimitiveType();
    }

    /**
     * Returns true if this jlang.type is void.
     *
     * @return
     */
    public boolean isVoidType()
    {
        return type.isVoidType();
    }

    /**
     * Returns true if this tpe is integral jlang.type.
     *
     * @return
     */
    public boolean isIntegerType()
    {
        return type.isIntegerType();
    }

    /**
     * Returns true if this jlang.type is real jlang.type.
     */
    public boolean isRealType()
    {
        return type.isRealType();
    }

    /**
     * Returns true if this jlang.type is complex jlang.type.
     *
     * @return
     */
    public boolean isComplexType()
    {
        return type.isComplexType();
    }

    /**
     * Returns true if this jlang.type is boolean jlang.type.
     *
     * @return
     */
    public boolean isBooleanType()
    {
        return type.isBooleanType();
    }

    /**
     * Checks whether this jlang.type is integral and qualified with signed.
     *
     * @return
     * @throws Error
     */
    public boolean isSignedType()
    {
        return type.isSignedType();
    }

    /**
     * Checks if this jlang.type is a pointer to actual jlang.type object.
     *
     * @return
     */
    public boolean isPointerType()
    {
        return type.isPointerType();
    }

    /**
     * Checks if this jlang.type is reference jlang.type.
     *
     * @return
     */
    public boolean isReferenceType()
    {
        return type.isReferenceType();
    }

    /**
     * Checks if this jlang.type is a formal function jlang.type in C or static member function jlang.type in C++.
     *
     * @return
     */
    public boolean isFunctionType()
    {
        return type.isFunctionType();
    }

    /**
     * Checks if this jlang.type is member function jlang.type of a class in C++.
     *
     * @return
     */
    public boolean isMethodType()
    {
        return type.isMethodType();
    }

    /**
     * Checks if this jlang.type is array jlang.type.
     *
     * @return
     */
    public boolean isConstantArrayType(){return type.isConstantArrayType();}

    public boolean isVariableArrayType() {return type.isVariableArrayType();}

    public boolean isIncompleteArrayType(){return type.isIncompleteArrayType();}

    /**
     * Determine whether this jlang.type is record jlang.type or not.
     *
     * @return return true if it is record, otherwise return false.
     */
    public boolean isRecordType()
    {
        return type.isRecordType();
    }

    public boolean isStructureType()
    {
        return type.isRecordType() && type.getTypeClass() == TypeClass.Struct;
    }

    /**
     * Checks if this jlang.type is enumeration jlang.type.
     *
     * @return
     */
    public boolean isEnumType()
    {
        return type.isEnumType();
    }

    /**
     * Checks if this jlang.type is jlang.type-getIdentifier jlang.type.
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

    public boolean isCallable()
    {
        return type.isCallable();
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

    public QualType getQualifiedType(int quals)
    {
        return new QualType(type, quals);
    }

    public QualType getWithAdditionalQualifiers(int quals)
    {
        return new QualType(type, quals | getCVRQualifiers());
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
     * Checks if this jlang.type is integral or enumeration.
     *
     * @return {@code true} returned if this jlang.type is integral or enumeration,
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
     * Then clear all of its jlang.type-qualifiers.
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
     * Return true if this is an incomplete or object
     * type, in other words, not a function type.
     * @return
     */
    public boolean isIncompleteOrObjectType()
    {
        return !isFunctionType();
    }

    public boolean isVariableModifiedType()
    {
        return type.isVariablyModifiedType();
    }

    public PrimitiveType getAsBuiltinType()
    {
        if (isBuiltinType())
            return (PrimitiveType)type;
        return null;
    }
}
