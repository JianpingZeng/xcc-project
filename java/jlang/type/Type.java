package jlang.type;

import jlang.cparser.DeclSpec;
import jlang.exception.SemanticError;
import jlang.type.ArrayType.ConstantArrayType;
import jlang.type.ArrayType.VariableArrayType;
import tools.Util;

/**
 * The abstract root class of various jlang.type. It provides different definitions
 * for it's concrete subclass.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class Type implements TypeClass
{
    public static QualType VoidTy = new QualType(VoidType.New());

    public static QualType BoolTy = new QualType(new IntegerType(1, false));

    public static QualType CharTy = new QualType(new IntegerType(1, true));
    public static QualType SignedCharTy = new QualType(new IntegerType(1, true));
    public static QualType UnsignedCharTy = new QualType(new IntegerType(1, false));

    public static QualType ShortTy = new QualType(new IntegerType(2, true));
    public static QualType UnsignedShortTy = new QualType(
            new IntegerType(2, false));

    public static QualType IntTy = new QualType(new IntegerType(4, true));
    public static QualType UnsignedIntTy = new QualType(
            new IntegerType(4, false));

    public static QualType LongTy = new QualType(new IntegerType(4, true));
    public static QualType UnsignedLongTy = new QualType(new IntegerType(4, false));

    public static QualType LongLongTy = new QualType(new IntegerType(8, true));
    public static QualType UnsignedLongLongTy = new QualType(new IntegerType(8, false));

    public static QualType FloatTy = new QualType(new RealType(4, "float"));
    public static QualType DoubleTy = new QualType(new RealType(8, "double"));

    /**
     * The kind of a tag jlang.type.
     */
    public enum TagTypeKind
    {
        TTK_struct, TTK_union, TTK_enum;

        public static TagTypeKind getTagTypeKindForTypeSpec(DeclSpec.TST tagType)
        {
            switch (tagType)
            {
                case TST_struct:
                    return TTK_struct;
                case TST_union:
                    return TTK_union;
                case TST_enum:
                    return TTK_enum;
                default:
                    Util.shouldNotReachHere("Type specifier is not a tag jlang.type kind");
                    return TTK_union;
            }
        }
    }

    protected int tag;

    private QualType canonicalType;

    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param tag
     */
    public Type(int tag)
    {
        this.tag = tag;
    }

	public Type(int typeClass, QualType canonical)
	{
		tag = typeClass;
		canonicalType = canonical.isNull()?new QualType(this, 0):canonical;
	}

    public int getTypeClass(){return tag;}

	public boolean isCanonical() {return canonicalType.getType() == this;}

    /**
     * Returns the getNumOfSubLoop of the specified jlang.type in bits.
     * </br>
     * This method doesn't work on incomplete types.
     *
     * @return
     */
    public abstract long getTypeSize();

    /**
     * Indicates the number of memory spaces in bytes.
     *
     * @return
     */
    public long allocSize()
    {
        return getTypeSize();
    }

    /**
     * The getSize of memory alignment in bytes.
     *
     * @return
     */
    public long alignment()
    {
        return allocSize();
    }

    /*
     * Checks if this the kind of this jlang.type is same as other.
     */
    public abstract boolean isSameType(Type other);

    /**
     * Checks if this {@linkplain Type} is primitive jlang.type.
     *
     * @return
     */
    public boolean isPrimitiveType()
    {
        return tag >= Bool && tag <= Real;
    }

    /**
     * Returns true if this jlang.type is void.
     *
     * @return
     */
    public boolean isVoidType()
    {
        return tag == Void;
    }

    /**
     * Returns true if this tpe is integral jlang.type.
     *
     * @return
     */
    public boolean isIntegerType()
    {
        return false;
    }

    /**
     * Returns true if this jlang.type is real jlang.type.
     */
    public boolean isRealType()
    {
        return tag == Real;
    }

    /**
     * Returns true if this jlang.type is complex jlang.type.
     *
     * @return
     */
    public boolean isComplexType()
    {
        return tag == Complex;
    }

    /**
     * Returns true if this jlang.type is boolean jlang.type.
     *
     * @return
     */
    public boolean isBooleanType()
    {
        return tag == Bool;
    }

    /**
     * Checks whether this jlang.type is integral and qualified with signed.
     *
     * @return
     * @throws Error
     */
    public boolean isSignedType()
    {
        throw new Error("#isSignedType for non-integer jlang.type");
    }

    /**
     * Checks if this jlang.type is a pointer to actual jlang.type object.
     *
     * @return
     */
    public boolean isPointerType()
    {
        return tag == Pointer;
    }

    /**
     * Checks if this jlang.type is reference jlang.type.
     *
     * @return
     */
    public boolean isReferenceType()
    {
        return tag == Reference;
    }

    /**
     * Checks if this jlang.type is a formal function jlang.type in C or static member function jlang.type in C++.
     *
     * @return
     */
    public boolean isFunctionType()
    {
        return tag == Function;
    }

    /**
     * Checks if this jlang.type is member function jlang.type of a class in C++.
     *
     * @return
     */
    public boolean isMethodType()
    {
        return tag == Method;
    }

    public boolean isArrayType()
    {
        return this instanceof ArrayType;
    }

    /**
     * Checks if this jlang.type is array jlang.type.
     *
     * @return
     */
    public boolean isConstantArrayType() { return tag == ConstantArray;}

    public boolean isVariableArrayType() {return tag == VariableArray;}

    public boolean isIncompleteArrayType() { return tag == IncompleteArray;}

    /**
     * Determine whether this jlang.type is record jlang.type or not.
     *
     * @return return true if it is record, otherwise return false.
     */
    public boolean isRecordType()
    {
        return tag == Struct || tag == Union;
    }

    /**
     * Checks if this jlang.type is enumeration jlang.type.
     *
     * @return
     */
    public boolean isEnumType()
    {
        return tag == Enum;
    }

    /**
     * Checks if this jlang.type is jlang.type-getIdentifier jlang.type.
     *
     * @return
     */
    public boolean isUserType()
    {
        return tag == TypeDef;
    }

    // Ability methods (unary)
    public boolean isAllocatedArray()
    {
        return false;
    }

    public boolean isScalarType()
    {
        if (isPrimitiveType())
            return tag > Void && tag <= Real;
        if (isEnumType())
        {
            return ((EnumType) this).getDecl().isCompleteDefinition();
        }
        return isPointerType() || isComplexType();
    }

    public boolean isCallable()
    {
        return false;
    }

    /**
     * Indicates if this jlang.type can be casted into TargetData jlang.type.
     *
     * @param target
     * @return
     */
    public abstract boolean isCastableTo(Type target);

    /**
     * 对于引用类型返回起基类型，该方法需要具体的子类进行覆盖。
     *
     * @return
     */
    public Type baseType()
    {
        throw new SemanticError("#baseType called for undereferable jlang.type");
    }

    // Cast methods
    public IntegerType getIntegerType()
    {
        return (IntegerType) this;
    }

    public RealType getRealType()
    {
        return (RealType) this;
    }

    public ComplexType getComplexTye()
    {
        return (ComplexType) this;
    }

    public PointerType getPointerType()
    {
        return (PointerType) this;
    }

    public FunctionType getFunctionType()
    {
        return (FunctionType) this;
    }

    public RecordType getRecordType()
    {
        return (RecordType) this;
    }

    public EnumType getEnumType()
    {
        return (EnumType) this;
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
        return tag;
    }

    public boolean isArithmeticType()
    {
        if (isPrimitiveType())
        {
            return tag >= Bool && tag <= Real;
        }
        if (isEnumType())
        {
            // GCC allows forward declaration of enum types (forbid by C99 6.7.2.3p2).
            // If a body isn't seen by the time we get here, return false.
            return getEnumType().getDecl().isCompleteDefinition();
        }
        return isComplexType();
    }

    public QualType getPointee()
    {
        if (isPointerType())
            return getPointerType().getPointee();
        return new QualType();
    }

    /**
     * Return true if this is an incomplete jlang.type (C99 6.2.5p1)
     * <br>
     * a jlang.type that can describe objects, but which lacks information needed to
     * determine its getNumOfSubLoop.
     * @return
     */
    public boolean isIncompleteType()
    {
        if (isPrimitiveType())
        {
            // Void is the only incomplete builtin jlang.type.  Per C99 6.2.5p19,
            // it can never be completed.
            return isVoidType();
        }
        switch (tag)
        {
            case Struct:
            case Union:
                return !((TagType)this).getDecl().isCompleteDefinition();
            case ConstantArray:
                // An array is incomplete if its element jlang.type is incomplete
                return ((ConstantArrayType)this).getElemType().isIncompleteType();
            case IncompleteArray:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return true if this is not a variable sized jlang.type,
     * according to the rules of C99 6.7.5p3.  It is not legal to call this on
     * incomplete types
     * @return
     */
    public boolean isConstantSizeType()
    {
        assert !isIncompleteType():"This doesn't make sense for incomplete types";

        return !(this instanceof VariableArrayType);
    }

    public Type getArrayElementTypeNoQuals()
    {
        if (this instanceof ArrayType)
            return ((ArrayType)this).getElemType().getType();

        // TODO If this is a typedef for an array type, strip the typedef off without
        // losing all typedef information.
        return null;
    }

    /**
     * C99 6.7.5p3.
     * Return true for variable length array types and types that contain
     * variable array types in their declarator.
     * @return
     */
    public boolean isVariablyModifiedType()
    {
        // A VLA is a variably modified type.
        if (isVariableArrayType())
            return true;

        jlang.type.Type ty = getArrayElementTypeNoQuals();
        if (ty !=null)
            return ty.isVariablyModifiedType();

        PointerType pt = getPointerType();
        if (pt != null)
            return pt.getPointeeType().isVariablyModifiedType();

        FunctionType ft = getFunctionType();
        if (ft != null)
            return ft.getReturnType().isVariablyModifiedType();
        return false;
    }

	/**
	 * Obtains the canonical type underlying this type.
	 * @return
	 */
	public QualType getCanonicalTypeInternal()
	{
		return canonicalType;
	}

    public boolean isUnionType()
    {
        RecordType rt = getRecordType();
        if (rt != null)
            return rt.getDecl().isUnion();
        return false;
    }
}
