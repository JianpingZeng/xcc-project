package type;

import cparser.DeclSpec;
import exception.SemanticError;
import utils.Util;

/**
 * The abstract root class of various type. It provides different definitions
 * for it's concrete subclass.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class Type implements TypeTags
{
    public static QualType VoidTy = new QualType(VoidType.New());

    public static QualType BoolTy = new QualType(new IntegerType(1, false));

    public static QualType CharTy = new QualType(new IntegerType(1, true));
    public static QualType UnsignedCharTy = new QualType(new IntegerType(1, false));

    public static QualType ShortTy = new QualType(new IntegerType(2, true));
    public static QualType UnsignedShortTy = new QualType(
            new IntegerType(2, false));

    public static QualType IntTy = new QualType(new IntegerType(4, true));
    public static QualType UnsignedIntTy = new QualType(
            new IntegerType(4, false));

    public static QualType LongTy = new QualType(new IntegerType(4, true));
    public static QualType UnsignedLongTy = new QualType(new IntegerType(4, false));

    public static QualType FloatTy = new QualType(new RealType(4, "float"));
    public static QualType DoubleTy = new QualType(new RealType(8, "double"));

    /**
     * The kind of a tag type.
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
                    Util.shouldNotReachHere("Type specifier is not a tag type kind");
                    return TTK_union;
            }
        }
    }

    static final public long sizeUnknown = -1;

    protected int tag;

    /**
     * Constructor with one parameter which represents the kind of type
     * for reason of comparison convenient.
     *
     * @param tag
     */
    public Type(int tag)
    {
        this.tag = tag;
    }

    /**
     * Returns the size of the specified type in bits.
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
     * The length of memory alignment in bytes.
     *
     * @return
     */
    public long alignment()
    {
        return allocSize();
    }

    /*
     * Checks if this the kind of this type is same as other.
     */
    public abstract boolean isSameType(Type other);

    /**
     * Checks if this {@linkplain Type} is primitive type.
     *
     * @return
     */
    public boolean isPrimitiveType()
    {
        return tag >= BOOLEAN && tag <= REAL || (tag == USER_DEF
                && ((UserType) this).getActual().isPrimitiveType());
    }

    /**
     * Returns true if this type is void.
     *
     * @return
     */
    public boolean isVoidType()
    {
        return tag == VOID || (tag == USER_DEF && ((UserType) this).getActual()
                .isVoidType());
    }

    /**
     * Returns true if this tpe is integral type.
     *
     * @return
     */
    public boolean isIntegerType()
    {
        return tag == INT || (tag == USER_DEF && ((UserType) this).getActual()
                .isIntegerType());
    }

    /**
     * Returns true if this type is real type.
     */
    public boolean isRealType()
    {
        return tag == REAL || (tag == USER_DEF && ((UserType) this).getActual()
                .isRealType());
    }

    /**
     * Returns true if this type is complex type.
     *
     * @return
     */
    public boolean isComplexType()
    {
        return tag == COMPLEX || (tag == USER_DEF && ((UserType) this)
                .getActual().isComplexType());
    }

    /**
     * Returns true if this type is boolean type.
     *
     * @return
     */
    public boolean isBooleanType()
    {
        return tag == BOOLEAN || (tag == USER_DEF && ((UserType) this)
                .getActual().isBooleanType());
    }

    /**
     * Checks whether this type is integral and qualified with signed.
     *
     * @return
     * @throws Error
     */
    public boolean isSignedType()
    {
        throw new Error("#isSignedType for non-integer type");
    }

    /**
     * Checks if this type is a pointer to actual type object.
     *
     * @return
     */
    public boolean isPointerType()
    {
        return tag == POINTER || (tag == USER_DEF && ((UserType) this)
                .getActual().isPointerType());
    }

    /**
     * Checks if this type is reference type.
     *
     * @return
     */
    public boolean isReferenceType()
    {
        return tag == REFERENCE || (tag == USER_DEF && ((UserType) this)
                .getActual().isReferenceType());
    }

    /**
     * Checks if this type is a formal function type in C or static member function type in C++.
     *
     * @return
     */
    public boolean isFunctionType()
    {
        return tag == FUNCTION || (tag == USER_DEF && ((UserType) this)
                .getActual().isFunctionType());
    }

    /**
     * Checks if this type is member function type of a class in C++.
     *
     * @return
     */
    public boolean isMethodType()
    {
        return tag == METHOD || (tag == USER_DEF && ((UserType) this)
                .getActual().isMethodType());
    }

    /**
     * Checks if this type is array type.
     *
     * @return
     */
    public boolean isArrayType()
    {
        return tag == ConstantArray || (tag == USER_DEF && ((UserType) this).getActual()
                .isArrayType());
    }

    /**
     * Determine whether this type is record type or not.
     *
     * @return return true if it is record, otherwise return false.
     */
    public boolean isRecordType()
    {
        return tag == STRUCT || (tag == USER_DEF && ((UserType) this)
                .getActual().isRecordType());
    }

    /**
     * Checks if this type is union type.
     *
     * @return
     */
    public boolean isUnionType()
    {
        return tag == UNION || (tag == USER_DEF && ((UserType) this).getActual()
                .isUnionType());
    }

    /**
     * Checks if this type is enumeration type.
     *
     * @return
     */
    public boolean isEnumType()
    {
        return tag == ENUM || (tag == USER_DEF && ((UserType) this).getActual()
                .isEnumType());
    }

    /**
     * Checks if this type is type-name type.
     *
     * @return
     */
    public boolean isUserType()
    {
        return tag == USER_DEF;
    }

    // Ability methods (unary)
    public boolean isAllocatedArray()
    {
        return false;
    }

    public boolean isIncompleteArray()
    {
        return false;
    }

    public boolean isScalarType()
    {
        if (isPrimitiveType())
            return tag > VOID && tag <= REAL;
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
     * Indicates if this type can be wrapped with other type.
     *
     * @param other
     * @return
     */
    public abstract boolean isCompatible(Type other);

    /**
     * Indicates if this type can be casted into target type.
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
        throw new SemanticError("#baseType called for undereferable type");
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

    public UnionType getUnionType()
    {
        return (UnionType) this;
    }

    public ArrayType getArrayType()
    {
        return (ArrayType) this;
    }

    public EnumType getEnumType()
    {
        return (EnumType) this;
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
        return tag;
    }

    public boolean isArithmeticType()
    {
        if (isPrimitiveType())
        {
            return tag >= BOOLEAN && tag <= REAL;
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
     * Return true if this is an incomplete type (C99 6.2.5p1)
     * <br>
     * a type that can describe objects, but which lacks information needed to
     * determine its size.
     * @return
     */
    public boolean isIncompleteType()
    {
        if (isPrimitiveType())
        {
            // Void is the only incomplete builtin type.  Per C99 6.2.5p19, it can never
            // be completed.
            return isVoidType();
        }
        switch (tag)
        {
            case STRUCT:
            case UNION:
                return !((TagType)this).getDecl().isCompleteDefinition();
            case ConstantArray:
                // An array is incomplete if its element type is incomplete
                return ((ArrayType)this).getElementType().isIncompleteType();
            case IncompleteArray:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return true if this is not a variable sized type,
     * according to the rules of C99 6.7.5p3.  It is not legal to call this on
     * incomplete types
     * @return
     */
    public boolean isConstantSizeType()
    {
        assert !isIncompleteType():"This doesn't make sense for incomplete types";

        return !(this instanceof ArrayType.VariableArrayType);
    }
}
