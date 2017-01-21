package backend.analysis;

import backend.value.Constant;
import jlang.sema.APInt;

import java.util.Objects;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ConstantRange
{
    private APInt lower, upper;

    /**
     * Initialize a full (the default) or empty set for the specified bit width.
     * @param bitwidth
     * @param isFullSet
     */
    public ConstantRange(int bitwidth, boolean isFullSet)
    {}

    public ConstantRange(int bitwidth)
    {
        this(bitwidth, true);
    }

    public ConstantRange(APInt value)
    {

    }

    public ConstantRange(APInt lower, APInt upper)
    {

    }

    public APInt getLower()
    {
        return lower;
    }
    public APInt getUpper()
    {
        return upper;
    }

    public int getBitWidth()
    {
        return lower.getBitWidth();
    }

    /**
     * Return true if this set contains all of the elements possible
     * for this data-type
     * @return
     */
    public boolean isFullSet()
    {

    }

    public boolean isEmptySet()
    {

    }

    /**
     * Return true if this set wraps around the top of the range,
     * for example: [100, 8)
     * @return
     */
    public boolean isWrappedSet()
    {

    }

    public boolean contains(APInt val)
    {

    }

    public boolean contains(ConstantRange range)
    {

    }

    public APInt getSingleElement()
    {
        if (upper.eq(lower.add(1)))
            return lower;
        return null;
    }

    public boolean isSingleElement()
    {
        return getSingleElement() != null;
    }

    /**
     * Return the number of elements in this set.
     * @return
     */
    public APInt getSetSize()
    {

    }

    /**
     * Obtains the largest unsigned element in this set.
     * @return
     */
    public APInt getUnsignedMax()
    {

    }

    /**
     * Obtains the smallest unsigned element in this set.
     * @return
     */
    public APInt getUnsignedMin()
    {}

    /**
     * Gets the largest signed element in this set.
     * @return
     */
    public APInt getSignedMax()
    {}

    /**
     * Return the smallest signed number in this set.
     * @return
     */
    public APInt getSignedMin()
    {}

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof ConstantRange))
            return false;

        ConstantRange range = (ConstantRange)obj;
        return lower.eq(range.lower) && upper.eq(range.upper);
    }

    @Override
    public int hashCode()
    {
        return (lower.hashCode() << 31) ^ upper.hashCode();
    }

    public ConstantRange subtract(APInt ci)
    {

    }

    public ConstantRange intersectWith(ConstantRange range)
    {}

    public ConstantRange unionWith(ConstantRange range)
    {}

    public ConstantRange zeroExtend(int bitwidth)
    {}

    public ConstantRange signExtend(int bitwidth)
    {}

    public ConstantRange truncate(int bitwidth)
    {}

    public ConstantRange add(ConstantRange other)
    {}

    public ConstantRange multiply(ConstantRange rhs)
    {}

    public ConstantRange smax(ConstantRange other)
    {

    }

    public ConstantRange umax(ConstantRange other)
    {}

    public ConstantRange udiv(ConstantRange other)
    {}
}
