package jlang.type;

import tools.Util;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IntegerType extends PrimitiveType
{

    private static String[] LengthName = {"char", "short", "int", "long"};
    /**
     * The getNumOfSubLoop of memory space that this jlang.type allocated in bits.
     */
    private long size;
    private boolean isSigned;
    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param size	the getTypeSize of specified integer in Byte.
     * @param isSigned	whether is signed or not.
     */
    public IntegerType(long size, boolean isSigned)
    {
        super(Int, isSigned?"signed ":"unsigned " + LengthName[Util.log2(size)]);
        this.size = size*8;
        this.isSigned = isSigned;
        assert Util.isPowerOf2(size):"The getNumOfSubLoop must be power of 2.";
    }
    @Override
    public boolean isSignedType()
    {
        return isSigned;
    }

    public long minValue()
    {
        return isSigned? (long)-Math.pow(2, (size << 3) -1) : 0;
    }

    public long maxValue()
    {
        return isSigned ? (long)Math.pow(2, (size << 3) - 1)-1 :
                (long)Math.pow(2, size << 3)-1;
    }

    /**
     * Checks if specified value is range from minimum value to maximum value.
     * @param val
     * @return
     */
    public boolean isInDomain(long val)
    {
        return (minValue() <= val && val <= maxValue());
    }

    @Override
    public long getTypeSize()
    {
        return size;
    }

    @Override
    public boolean isSameType(Type other)
    {
        if (!other.isIntegerType())
            return false;

        return equals(other.getIntegerType());
    }

    @Override
    public boolean isCastableTo(Type target)
    {
        return false;
    }

    public String toString()
    {
        return name;
    }
}
