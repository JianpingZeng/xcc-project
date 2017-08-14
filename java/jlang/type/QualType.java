package jlang.type;

import jlang.support.LangOptions;
import jlang.support.PrintingPolicy;
import jlang.sema.ASTContext;

import static jlang.type.ArrayType.VariableArrayType.appendTypeQualList;

/**
 * This class represents a wrapper which combines CV-qualified types as nodes.
 * Which provides the same API as {@linkplain Type} by directly invoking those
 * methods of {@linkplain Type}.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class QualType implements Cloneable
{
    public static final int CONST_QUALIFIER = 0x1;
    public static final int VOLATILE_QUALIFIER = 0x2;
    public static final int RESTRICT_QUALIFIER = 0x4;
    private static final int MASK =
            CONST_QUALIFIER | VOLATILE_QUALIFIER | RESTRICT_QUALIFIER;

    public int getAddressSpace()
    {
        QualType ct = getType().getCanonicalTypeInternal();
        if (ct.isArrayType())
            return ((ArrayType)ct.getType()).getElemType().getAddressSpace();
        if (ct.isRecordType())
            return ((RecordType)ct.getType()).getAddressSpace();
        return 0;
    }

    public boolean isConstant(ASTContext ctx)
    {
        if (isConstQualifed())
            return true;
        if (getType().isArrayType())
            return ctx.getAsArrayType(this).getElemType().isConstant(ctx);

        return false;
    }

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
        this(null, 0);
    }

    public QualType(Type t)
    {
        this(t, 0);
    }

    public QualType(Type t, int quals)
    {
        type = t;
        qualsFlag = new Qualifier();
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

    public String getAsStringInternal(String s, PrintingPolicy policy)
    {
        if (isNull())
        {
            s += "NULL TYPE";
            return s;
        }

        if (policy.suppressSpecifiers && type.isSpecifierType())
            return s;

        int tq = getCVRQualifiers();
        if (tq != 0)
        {
            String tqs = appendTypeQualList("", tq);
            if (!tqs.isEmpty())
                s = tqs + " " + s;
            else
                s = tqs;
        }

        return type.getAsStringInternal(s, policy);
    }

    public String getAsString()
    {
        return getAsStringInternal("", new PrintingPolicy(new LangOptions()));
    }

    public void dump()
    {
        System.err.println(getAsString());
    }

    public boolean isCanonical()
    {
        return type.isCanonical();
    }

    /**
     * <p>
     * Determines whether the type describes an object in memory.
     * </p>
     * <p>
     * Types are partitioned into 3 broad categories (C99 6.2.5p1):
     * object types, function types, and incomplete types.
     * </p>
     * @return
     */
    public boolean isObjectType()
    {
        return type.isObjectType();
    }

    /**
     * Return true if this is an incomplete type.
     /// A type that can describe objects, but which lacks information needed to
     /// determine its size (e.g. void, or a fwd declared struct). Clients of this
     /// routine will need to determine if the size is actually required.
     * @return
     */
    public boolean isIncompleteType()
    {
        return type.isIncompleteType();
    }

    /**
     * Return true if this is an incomplete or object
     * type, in other words, not a function type.
     * @return
     */
    public boolean isIncompleteOrObjectType()
    {
        return type.isIncompleteOrObjectType();
    }

    /**
     * (C99 6.7.5.2p2) - Return true for variable array
     * types that have a non-constant expression. This does not include "[]".
     * @return
     */
    public boolean isVariablyModifiedType()
    {
        return type.isVariablyModifiedType();
    }

    /**
     * If this is an array type, return the
     /// element type of the array, potentially with type qualifiers missing.
     /// This method should never be used when type qualifiers are meaningful.
     * @return
     */
    public Type getArrayElementTypeNoTypeQual()
    {
        return type.getArrayElementTypeNoTypeQual();
    }

    public QualType getDesugaredType()
    {
        return type.getDesugaredType();
    }

    public boolean isBuiltinType()
    {
        return type.isBuiltinType();
    }

    /**
     * Helper methods to distinguish type categories. All type predicates
     /// operate on the canonical type, ignoring typedefs and qualifiers.
     * @param tk
     * @return
     */
    public boolean isSpecifiedBuiltinType(int tk)
    {
        return type.isSpecifiedBuiltinType(tk);
    }

    /**
     * // C99 6.2.5p17 (int, char, bool, enum)
     * @return
     */
    public boolean isIntegerType()
    {
        return type.isIntegerType();
    }

    public boolean isEnumeralType()
    {
        return type.isEnumeralType();
    }

    public boolean isStructureType()
    {
        return type.isStructureType();
    }

    public boolean isUnionType()
    {
        return type.isUnionType();
    }
    public boolean isBooleanType()
    {
        return type.isBooleanType();
    }

    public boolean isCharType()
    {
        return type.isCharType();
    }

    public boolean isIntegralType()
    {
        return type.isIntegerType();
    }

    /**
     * // C99 6.2.5p10 (float, double, long double)
     * @return
     */
    public  boolean isRealFloatingType()
    {
        return type.isRealFloatingType();
    }

    public boolean isComplexType()
    {
        return type.isComplexType();
    }

    public boolean isComplexIntegerType()
    {
        return type.isComplexIntegerType();
    }

    public boolean isAnyComplexType()
    {
        return type.isAnyComplexType();
    }

    public boolean isFloatingType()
    {
        return type.isFloatingType();
    }

    /**
     * C99 6.2.5p17 (real floating + integer)
     * @return
     */
    public boolean isRealType()
    {
        return type.isRealType();
    }

    /**
     * C99 6.2.5p18 (integer + floating)
     * @return
     */
    public boolean isArithmeticType()
    {
        return type.isArithmeticType();
    }

    public boolean isVoidType()
    {
        return type.isVoidType();
    }

    public boolean isDerivedType()
    {
        return type.isDerivedType();
    }

    public boolean isScalarType()
    {
        return type.isScalarType();
    }

    public boolean isAggregateType()
    {
        return type.isAggregateType();
    }

    public boolean isFunctionType()
    {
        return type.isFunctionType();
    }

    public boolean isFunctionNoProtoType()
    {
        return type.isFunctionNoProtoType();
    }

    public boolean isFunctionProtoType()
    {
        return type.isFunctionProtoType();
    }

    public boolean isPointerType()
    {
        return type.isPointerType();
    }

    public boolean isVoidPointerType()
    {
        return type.isVoidPointerType();
    }

    public boolean isFunctionPointerType()
    {
        return type.isFunctionPointerType();
    }

    public boolean isArrayType()
    {
        return type.isArrayType();
    }

    public boolean isConstantArrayType()
    {
        return type.isConstantArrayType();
    }

    public boolean isIncompleteArrayType()
    {
        return type.isIncompleteArrayType();
    }

    public boolean isVariableArrayType()
    {
        return type.isVariableArrayType();
    }

    public boolean isRecordType()
    {
        return type.isRecordType();
    }

    // Type Checking Functions: Check to see if this type is structurally the
    // specified type, ignoring typedefs and qualifiers, and return a pointer to
    // the best type we can.
    public BuiltinType getAsBuiltinType()
    {
        return type.getAsBuiltinType();
    }

    public FunctionType getAsFunctionType()
    {
        return type.getAsFunctionType();
    }

    public FunctionNoProtoType getAsFunctionNoProtoType()
    {
        return type.getAsFunctionNoProtoType();
    }

    public FunctionProtoType getAsFunctionProtoType()
    {
        return type.getAsFunctionProtoType();
    }

    public RecordType getAsStructureType()
    {
        return type.getAsStructureType();
    }

    public RecordType getAsUnionType()
    {
        return type.getAsUnionType();
    }

    public EnumType getAsEnumType()
    {
        return type.getAsEnumType();
    }

    public TypedefType getAsTypedefType()
    {
        return type.getAsTypedefType();
    }

    public ComplexType getAsComplexType()
    {
        return type.getAsComplexType();
    }

    public ComplexType getAsComplexIntegerType()
    {
        return type.getAsComplexIntegerType();
    }

    public PointerType getAsPointerType()
    {
        return type.getAsPointerType();
    }

    public RecordType getAsRecordType()
    {
        return type.getAsRecordType();
    }

    public QualType getPointeeType()
    {
        return type.getPointeeType();
    }

    /**
     * More type predicates useful for type checking/promotion.
     * @return
     */
    public boolean isPromotableIntegerType()
    {
        return type.isPromotableIntegerType();
    }

    /**
     * Return true if this is an integer type that is
     /// signed, according to C99 6.2.5p4 [char, signed char, short, int, long..],
     /// an enum decl which has a signed representation
     * @return
     */
    public boolean isSignedIntegerType()
    {
        return type.isSignedIntegerType();
    }

    public boolean isConstantSizeType()
    {
        return type.isConstantSizeType();
    }

    public boolean isIntegralOrEnumerationType()
    {
        return type.isIntegralOrEnumerationType();
    }

    public boolean isSpecifierType()
    {
        return type.isSpecifierType();
    }

    public int getTypeClass()
    {
        return type.getTypeClass();
    }

    @Override
    public int hashCode()
    {
        return getCVRQualifiers() << 11 + type.hashCode();
    }
}
