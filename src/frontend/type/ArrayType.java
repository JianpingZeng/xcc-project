package frontend.type;

import frontend.ast.Tree;
import frontend.sema.APInt;

/**
 * ConstantArrayType - C99 6.7.5.2 - Array Declarators.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ArrayType extends Type
{
    /**
     * The frontend.type of element which Array holds.
     */
    private QualType elemType;

    /**
     * Constructor with one parameter which represents the kind of frontend.type
     * for reason of comparison convenient.
     */
    public ArrayType(int tag, QualType elemTy)
    {
        super(tag);
        this.elemType = elemTy;
    }

    public QualType getElemType()
    {
        return elemType;
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

    public abstract int getIndexTypeCVRQualifiers();

    /**
     * This class represents the canonical version of C arrays with a specified
     * constant getTypeSize or not.  For example, the canonical frontend.type for 'int A[4 + 4*100]'
     * is a ConstantArrayType where the element frontend.type is 'int' and the getTypeSize is
     * 404, or or 'int A[]' has an IncompleteArrayType where the element frontend.type is
     * 'int' and the getTypeSize is unspecified.
     */
    public static class ConstantArrayType extends frontend.type.ArrayType
    {
        private APInt size;

        /**
         * Constructor with one parameter which represents the kind of frontend.type
         * for reason of comparison convenient.
         *
         * @param elemTy
         */
        public ConstantArrayType(QualType elemTy, APInt length)
        {
            super(ConstantArray, elemTy);
            //assert getSize.ult(0) : "The getSize for array must greater than zero!";
            size = length;
        }

        public ConstantArrayType(QualType elemTy)
        {
            super(ConstantArray, elemTy);
            size = new APInt(32, 0);
        }

        public APInt getSize()
        {
            return size;
        }

        public boolean isAllocatedArray()
        {
            return size.ugt(0) && (!getElemType().isArrayType()
                    || getElemType().isAllocatedArray());
        }

        public boolean isIncompleteArrayArray()
        {
            if (!getElemType().isArrayType())
                return false;
            return !getElemType().isAllocatedArray();
        }
        /**
         * Gets the getTypeSize as allocated array.
         *
         * @return
         */
        @Override
        public long allocSize()
        {
            return size.mul(getElemType().getTypeSize());
        }

        public long alignment()
        {
            return getElemType().alignment();
        }

        @Override
        public boolean isSameType(Type other)
        {
            // getSize is not important
            if (!other.isPointerType() && !other.isArrayType())
                return false;
            return getElemType().isSameType(other.baseType());
        }

        @Override
        public Type baseType()
        {
            return getElemType();
        }


        /**
         * Indicates if this frontend.type can be casted into target frontend.type.
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
            return getElemType().toString() + "[" + size.toString(10) + "]";
        }

        /**
         * Returns the reference to the frontend.type for an array of the specified element frontend.type.
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

        @Override
        public int getIndexTypeCVRQualifiers()
        {
            return 0;
        }
    }

    /**
     * This class represents C arrays with a specified getTypeSize
     * which is not an integer-constant-expression.  For example, 'int s[x+foo()]'.
     * Since the getTypeSize expression is an arbitrary expression, we store it as such.
     *
     * Note: VariableArrayType's aren't uniqued (since the expressions aren't) and
     * should not be: two lexically equivalent variable array types could mean
     * different things, for example, these variables do not have the same frontend.type
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
    public static class VariableArrayType extends frontend.type.ArrayType
    {
        /**
         * An assignment expression, which are only permitted within function block.
         */
        private Tree.Expr sizeExpr;
        /**
         * Constructor with one parameter which represents the kind of frontend.type
         * for reason of comparison convenient.
         *
         * @param elemTy
         */
        public VariableArrayType(QualType elemTy, Tree.Expr sizeExpr)
        {
            super(VariableArray, elemTy);
            this.sizeExpr = sizeExpr;
        }

        public Tree.Expr getSizeExpr()
        {
            return sizeExpr;
        }

        @Override
        public boolean isSameType(Type other)
        {
            return false;
        }


        /**
         * Indicates if this frontend.type can be casted into target frontend.type.
         *
         * @param target
         * @return
         */
        @Override
        public boolean isCastableTo(Type target)
        {
            return false;
        }

        public int getIndexTypeCVRQualifiers()
        {
            return sizeExpr.getType().getCVRQualifiers();
        }
    }

    /**
     * This class represents a C array with an uncomplete size.
     * For example 'int A[]' has an {@code IncompleteArrayType} where the element
     * frontend.type is 'int' and the size of unspecifed.
     */
    public static class IncompleteArrayType extends frontend.type.ArrayType
    {
        /**
         * Constructor with one parameter which represents the kind of frontend.type
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
         * Indicates if this frontend.type can be casted into target frontend.type.
         *
         * @param target
         * @return
         */
        @Override
        public boolean isCastableTo(Type target)
        {
            return false;
        }

        @Override
        public int getIndexTypeCVRQualifiers()
        {
            return 0;
        }
    }
}
