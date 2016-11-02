package frontend.type;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RealType extends PrimitiveType
{
    private long size;
    /**
     * Constructor with one parameter which represents the kind of frontend.type
     * for reason of comparison convenient.
     *
     * @param size
     * @param name
     */
    public RealType(long size, String name)
    {
        super(Real, name);
        this.size = size * 8;
    }

    /**
     * Checks if this frontend.type is single-precision float point frontend.type.
     * @return
     */
    public boolean isSinglePoint()
    {
        return size == 32;
    }
    public boolean isSignedType()
    {
        return true;
    }
    public double minValue()
    {
        return isSinglePoint()? Float.MIN_VALUE:Double.MIN_VALUE;
    }

    public double maxValue()
    {
        return isSinglePoint()?Float.MAX_VALUE : Double.MAX_VALUE;
    }
    /**
     * Checks if specified value is range from minimum value to maximum value.
     * @param val
     * @return
     */
    public boolean isInDomain(double val)
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
        if (!other.isRealType())
            return false;
        return equals(other.getRealType());
    }

    @Override
    public boolean isCastableTo(Type target)
    {
        return false;
    }
}
