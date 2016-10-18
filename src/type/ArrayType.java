package type;

import ast.Tree.Expr;
import sema.APInt;

/**
 * ArrayType - C99 6.7.5.2 - Array Declarators.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ArrayType extends Type
{
    /**
     * The type of element which Array holds.
     */
    private QualType elementType;

    /**
     * Constructor with one parameter which represents the kind of type
     * for reason of comparison convenient.
     */
    public ArrayType(int tag, QualType elementType)
    {
        super(tag);
        this.elementType = elementType;
    }

    public QualType getElementType()
    {
        return elementType;
    }

    /**
     * gets the getTypeSize as a pointer.
     *
     * @return
     */
    @Override
    public long getTypeSize()
    {
        return 4;
    }

    /**
     * This class represents the canonical version of C arrays with a specified
     * constant getTypeSize or not.  For example, the canonical type for 'int A[4 + 4*100]'
     * is a ConstantArrayType where the element type is 'int' and the getTypeSize is
     * 404, or or 'int A[]' has an IncompleteArrayType where the element type is
     * 'int' and the getTypeSize is unspecified.
     */
    public static class ConstantArrayType extends ArrayType
    {
        private APInt size;

        /**
         * Constructor with one parameter which represents the kind of type
         * for reason of comparison convenient.
         *
         * @param elemTy
         */
        public ConstantArrayType(QualType elemTy, APInt length)
        {
            super(ConstantArray, elemTy);
            //assert length.ult(0) : "The length for array must greater than zero!";
            size = length;
        }

        public ConstantArrayType(QualType elemTy)
        {
            super(ConstantArray, elemTy);
        }

        public APInt length()
        {
            return size;
        }

        public boolean isAllocatedArray()
        {
            return length > 0 && (!getElementType().isArrayType()
                    || getElementType().isAllocatedArray());
        }

        public boolean isIncompleteArray()
        {
            if (!getElementType().isArrayType())
                return false;
            return !getElementType().isAllocatedArray();
        }
        /**
         * Gets the getTypeSize as allocated array.
         *
         * @return
         */
        @Override
        public long allocSize()
        {
            return size * getElementType().getTypeSize();
        }

        public long alignment()
        {
            return getElementType().alignment();
        }

        @Override
        public boolean isSameType(Type other)
        {
            // length is not important
            if (!other.isPointerType() && !other.isArrayType())
                return false;
            return getElementType().isSameType(other.baseType());
        }

        @Override
        public Type baseType()
        {
            return getElementType();
        }

        /**
         * Indicates if this type can be wrapped with other type.
         *
         * @param other
         * @return
         */
        @Override
        public boolean isCompatible(Type other)
        {
            if (!other.isPointerType() && !other.isArrayType())
                return false;
            if (other.baseType().isVoidType())
            {
                return true;
            }
            return baseType().isCompatible(other.baseType())
                    && baseType().getTypeSize() == other.baseType().getTypeSize();
        }

        /**
         * Indicates if this type can be casted into target type.
         *
         * @param target
         * @return
         */
        @Override
        public boolean isCastableTo(Type target)
        {
            return target.isPointerType() || target.isArrayType();
        }

        public String toString()
        {
            return getElementType().toString() + "[" + size.toString(10) + "]";
        }

        /**
         * Returns the reference to the type for an array of the specified element type.
         * @param elemTy
         * @param size
         * @return
         */
        public static QualType getConstantType(QualType elemTy, APInt size)
        {
            assert elemTy.isIncompleteType()
                    || elemTy.isConstantSizeType():"Constant arrays of VLAs is illegal!";

            ConstantArrayType New = new ConstantArrayType(elemTy, size);
            return new QualType(New, 0);
        }
    }

    /**
     * This class represents C arrays with a specified getTypeSize
     * which is not an integer-constant-expression.  For example, 'int s[x+foo()]'.
     * Since the getTypeSize expression is an arbitrary expression, we store it as such.
     *
     * Note: VariableArrayType's aren't uniqued (since the expressions aren't) and
     * should not be: two lexically equivalent variable array types could mean
     * different things, for example, these variables do not have the same type
     * dynamically:
     *
     * <pre>
     * void foo(int x) {
     *   int Y[x];
     *   ++x;
     *   int Z[x];
     * }
     </pre>
     */
    public static class VariableArrayType extends ArrayType
    {
        /**
         * An assignment expression, which are only permitted within function block.
         */
        private Expr sizeExpr;
        /**
         * Constructor with one parameter which represents the kind of type
         * for reason of comparison convenient.
         *
         * @param elemTy
         */
        public VariableArrayType(QualType elemTy, Expr sizeExpr)
        {
            super(VariableArray, elemTy);
            this.sizeExpr = sizeExpr;
        }

        public Expr getSizeExpr()
        {
            return sizeExpr;
        }

        @Override
        public boolean isSameType(Type other)
        {
            return false;
        }

        /**
         * Indicates if this type can be wrapped with other type.
         *
         * @param other
         * @return
         */
        @Override
        public boolean isCompatible(Type other)
        {
            return false;
        }

        /**
         * Indicates if this type can be casted into target type.
         *
         * @param target
         * @return
         */
        @Override
        public boolean isCastableTo(Type target)
        {
            return false;
        }
    }

    /**
     * This class represents a C array with an uncomplete size.
     * For example 'int A[]' has an {@code IncompleteArrayType} where the element
     * type is 'int' and the size of unspecifed.
     */
    public static class IncompleteArrayType extends ArrayType
    {
        /**
         * Constructor with one parameter which represents the kind of type
         * for reason of comparison convenient.
         *
         * @param elementType
         */
        public IncompleteArrayType(QualType elementType)
        {
            super(IncompleteArray, elementType);
        }

        @Override
        public boolean isSameType(Type other)
        {
            return false;
        }

        /**
         * Indicates if this type can be wrapped with other type.
         *
         * @param other
         * @return
         */
        @Override public boolean isCompatible(Type other)
        {
            return false;
        }

        /**
         * Indicates if this type can be casted into target type.
         *
         * @param target
         * @return
         */
        @Override public boolean isCastableTo(Type target)
        {
            return false;
        }
    }
}
