package backend.analysis;

import jlang.support.APInt;

import java.io.PrintStream;

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
    {
        if (isFullSet)
            lower = upper = APInt.getMaxValue(bitwidth);
        else
            lower = upper = APInt.getMinValue(bitwidth);
    }

    public ConstantRange(int bitwidth)
    {
        this(bitwidth, true);
    }

    /**
     * Creates a range with a specified element.
     * @param value
     */
    public ConstantRange(APInt value)
    {
        lower = value;
        upper = value.increase();
    }

    public ConstantRange(APInt lower, APInt upper)
    {
        this.lower = lower;
        this.upper = upper;
        int bits = lower.getBitWidth();
        assert lower.getBitWidth() == upper.getBitWidth();
        assert !lower.eq(upper) || (upper.isMaxValue() && lower.isMinValue());
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
        return lower.eq(upper) && lower.isMaxValue();
    }

    /**
     * Return true if this set contains no members.
     * @return
     */
    public boolean isEmptySet()
    {
        return lower.eq(upper) && lower.isMinValue();
    }

    /**
     * Return true if this set wraps around the top of the range,
     * for example: [100, 8)
     * @return
     */
    public boolean isWrappedSet()
    {
        return lower.ugt(upper);
    }

    /**
     * Return true if the specified value is in the set.
     * @param val
     * @return
     */
    public boolean contains(APInt val)
    {
        if (lower.eq(upper))
            return isFullSet();

        if (!isWrappedSet())
            return lower.ule(val) && val.ult(upper);
        else
            return lower.ule(val) || val.ult(upper);
    }

    /**
     * Return true if the argument is a subset of this range.
     * Two equal set contain each other. The empty set is considered to be
     * contained by all other sets.
     * @param range
     * @return
     */
    public boolean contains(ConstantRange range)
    {
        if(isFullSet()) return true;
        if (range.isFullSet()) return false;
        if (range.isEmptySet()) return true;
        if (isEmptySet()) return false;

        if (!isWrappedSet())
        {
            if (range.isWrappedSet())
                return false;

            return lower.ule(range.lower) && range.upper.ult(upper);
        }

        if (!range.isWrappedSet())
        {
            return range.upper.ule(upper) || lower.ule(range.lower);
        }
        return range.upper.ule(upper) && lower.ule(range.lower);
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
        if (isEmptySet())
            return new APInt(getBitWidth(), 0);
        if (getBitWidth() == 1)
        {
            if (!lower.eq(upper))
                return new APInt(2, 1);
            return new APInt(2, 2);
        }

        return upper.sub(lower);
    }

    /**
     * Obtains the largest unsigned element in this set.
     * @return
     */
    public APInt getUnsignedMax()
    {
        if (isFullSet() || isWrappedSet())
            return APInt.getMaxValue(getBitWidth());
        else
            return getUpper().decrease();
    }

    /**
     * Obtains the smallest unsigned element in this set.
     * @return
     */
    public APInt getUnsignedMin()
    {
        if (isFullSet() || (isWrappedSet()) && !upper.eq(0))
            return APInt.getMaxValue(getBitWidth());
        else
            return getLower();
    }

    /**
     * Gets the largest signed element in this set.
     * @return
     */
    public APInt getSignedMax()
    {
        APInt signedMax = APInt.getSignedMaxValue(getBitWidth());
        if (!isWrappedSet())
        {
            if (lower.sle(upper.decrease()))
                return upper.decrease();
            else
                return signedMax;
        }
        else
        {
            if (lower.isNegative() == upper.isNegative())
                return signedMax;
            else
                return upper.decrease();
        }
    }

    /**
     * Return the smallest signed number in this set.
     * @return
     */
    public APInt getSignedMin()
    {
        APInt signedMin = APInt.getSignedMinValue(getBitWidth());
        if (!isWrappedSet())
        {
            if (lower.sle(upper.decrease()))
                return lower;
            else
                return signedMin;
        }
        else
        {
            if (upper.decrease().slt(lower))
            {
                if (!upper.eq(signedMin))
                    return signedMin;
            }
            return lower;
        }
    }

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

    /**
     * Subtract the specified constant from the endpoints of this
     * constant range.
     * @param ci
     * @return
     */
    public ConstantRange subtract(APInt ci)
    {
        assert getBitWidth() == ci.getBitWidth();
        if (lower.eq(upper))
            return this;
        return new ConstantRange(lower.sub(ci), upper.sub(ci));
    }

    /**
     * Return the range that results from the intersection of this
     * range with another range.  The resultant range is guaranteed to include all
     * elements contained in both input ranges, and to have the smallest possible
     * set getNumOfSubLoop that does so.
     * @param range
     * @return
     */
    public ConstantRange intersectWith(ConstantRange range)
    {
        assert getBitWidth() == range.getBitWidth();

        // Handle the common cases.
        if (isEmptySet() || range.isFullSet()) return this;
        if (range.isEmptySet() || isFullSet()) return range;

        if (!isWrappedSet() && range.isWrappedSet())
        {
            return range.intersectWith(this);
        }

        if (!isWrappedSet() && !range.isWrappedSet())
        {
            // If the tow range are disjoint, return an empty set.
            if (upper.ult(range.getLower()) || lower.ugt(range.upper))
                return new ConstantRange(getBitWidth(), false);

            // At this point, the two range must are join.
            if (lower.ult(range.lower))
            {
                if (upper.ult(range.upper))
                    return new ConstantRange(range.lower, upper);
                return range;
            }
            else
            {
                if (upper.ult(range.upper))
                    return this;

                return new ConstantRange(lower, range.upper);
            }
        }

        if (isWrappedSet() && !range.isWrappedSet())
        {
            if (range.lower.ult(upper))
            {
                if (range.upper.ult(upper))
                    return range;
                if (range.upper.ult(lower))
                    return new ConstantRange(range.lower, upper);

                if (getSetSize().ult(range.getSetSize()))
                    return this;
                else
                    return range;
            }
            else if (range.lower.ult(lower))
            {
                if (range.upper.ule(lower))
                    return new ConstantRange(getBitWidth(), false);
                return new ConstantRange(lower, range.upper);
            }
            return range;
        }

        if (range.upper.ult(upper))
        {
            if (range.lower.ult(upper))
            {
                if (getSetSize().ult(range.getSetSize()))
                    return this;
                else
                    return range;
            }

            if (range.lower.ult(lower))
                return new ConstantRange(lower, range.upper);

            return range;
        }
        else if (range.upper.ult(lower))
        {
            if (range.lower.ult(lower))
                return this;

            return new ConstantRange(range.lower, upper);
        }
        if (getSetSize().ult(range.getSetSize()))
            return this;
        else
            return range;
    }

    /**
     * Returns a range resulting from the union of this range with another range.
     * <p>
     * he resultant range is guaranteed to include the elements of
     * both sets, but may contain more.  For example, [3, 9) union [12,15) is
     * [3, 15), which includes 9, 10, and 11, which were not included in either
     * set before.
     * </p>
     * @param range
     * @return
     */
    public ConstantRange unionWith(ConstantRange range)
    {
        assert getBitWidth() == range.getBitWidth();

        if (isFullSet() || range.isEmptySet()) return this;
        if (isEmptySet() || range.isFullSet()) return range;

        if (!isWrappedSet() && range.isWrappedSet())
            return range.unionWith(this);

        if (!isWrappedSet() && !range.isWrappedSet())
        {
            if (range.upper.ult(lower) || range.lower.ugt(upper))
            {
                // If the two ranges are disjoint, find the smaller gap
                // and bright it.
                APInt diff1 = range.lower.sub(upper);
                APInt diff2 = lower.sub(range.upper);
                if (diff1.ult(diff2))
                    return new ConstantRange(lower, range.upper);
                else
                    return new ConstantRange(range.lower, upper);
            }

            APInt l = lower, u = upper;
            if (range.lower.ult(l))
                l = range.lower;
            if (range.upper.decrease().ugt(u.decrease()))
                u = range.upper;

            if (l.eq(0) && u.eq(0))
                return new ConstantRange(getBitWidth());

            return new ConstantRange(l, u);
        }

        if (!range.isWrappedSet())
        {
            if (range.upper.ule(upper) || range.lower.uge(lower))
                return this;


            if (range.lower.ule(upper) && lower.ule(range.upper))
                return new ConstantRange(getBitWidth());

            if (upper.ule(range.lower) && range.upper.ule(lower))
            {
                APInt diff1 = range.lower.sub(upper);
                APInt diff2 = lower.sub(range.upper);
                if (diff1.ult(diff2))
                    return new ConstantRange(lower, range.upper);
                else
                    return new ConstantRange(range.lower, upper);
            }

            if (upper.ult(range.lower) && lower.ult(range.upper))
                return new ConstantRange(range.lower, upper);
            //
            if (range.lower.ult(upper) && range.upper.ult(lower))
                return new ConstantRange(lower, range.upper);
        }

        assert isWrappedSet() && range.isWrappedSet();

        if (range.lower.ule(upper) || lower.ule(range.upper))
            return new ConstantRange(getBitWidth());

        APInt l = lower, u = upper;
        if (range.upper.ugt(u))
            u = range.upper;
        if (range.lower.ult(l))
            l = range.lower;
        return new ConstantRange(l, u);
    }

    /**
     * Returns a new range in the specified integer type.
     * This method used for performing zero extending on
     * each number in this range to the specified dstTySize.
     * @param dstTySize
     * @return
     */
    public ConstantRange zeroExtend(int dstTySize)
    {
        int srcTySize = getBitWidth();
        assert dstTySize >= srcTySize:"Not a value extension.";
        if (isFullSet())
            return new ConstantRange(new APInt(dstTySize, 0), new APInt(dstTySize, 1).shl(srcTySize));

        APInt l = lower.zext(dstTySize);
        APInt u = upper.zext(dstTySize);
        return new ConstantRange(l, u);
    }

    /**
     * Returns a new range in the specified integer type.
     * This method used for performing signed extending on
     * each number in this range to the specified dstTySize.
     * @param dstTySize
     * @return
     */
    public ConstantRange signExtend(int dstTySize)
    {
        int srcTySize = getBitWidth();
        assert srcTySize < dstTySize :"Not a signed extension";
        if (isFullSet())
            return new ConstantRange(APInt.getHighBitsSet(dstTySize, dstTySize-srcTySize+1),
                    APInt.getLowBitsSet(dstTySize, srcTySize-1).increase());
        APInt l = lower.sext(dstTySize);
        APInt h = upper.sext(dstTySize);
        return new ConstantRange(l, h);
    }

    /**
     * Return a new constant range in the specified data type, which must
     * strictly smaller than the current type.
     * @param dstTySize
     * @return
     */
    public ConstantRange truncate(int dstTySize)
    {
        int srcTySize = getBitWidth();
        assert dstTySize < srcTySize :"Not a truncation";
        APInt size = APInt.getLowBitsSet(srcTySize, dstTySize);
        if (isFullSet() || getSetSize().ugt(size))
            return new ConstantRange(dstTySize);

        APInt l = lower.trunc(dstTySize);
        APInt h = upper.trunc(dstTySize);
        return new ConstantRange(l, h);
    }

    public ConstantRange add(ConstantRange other)
    {
        if (isEmptySet() || other.isEmptySet())
            return new ConstantRange(getBitWidth(), false);
        if (isFullSet() || other.isFullSet())
            return new ConstantRange(getBitWidth());

        APInt spreadX = getSetSize();
        APInt spreadY = other.getSetSize();
        APInt newL = lower.add(other.lower);
        APInt newH = upper.add(other.upper);
        if (newL.eq(newH))
        {
            // return full set.
            return new ConstantRange(getBitWidth());
        }

        ConstantRange x = new ConstantRange(newL, newH);
        if (x.getSetSize().ult(spreadX) || x.getSetSize().ult(spreadY))
        {
            // We've wrapped, therefore, full set.
            return new ConstantRange(getBitWidth());
        }
        return x;
    }

    public ConstantRange multiply(ConstantRange rhs)
    {
        if (isEmptySet() || rhs.isEmptySet())
            return new ConstantRange(getBitWidth(), false);
        if (isFullSet() || rhs.isFullSet())
            return new ConstantRange(getBitWidth());

        APInt thisMin = getUnsignedMin().zext(getBitWidth()*2);
        APInt thisMax = getUnsignedMax().zext(getBitWidth()*2);
        APInt rhsMin = rhs.getUnsignedMin().zext(getBitWidth()*2);
        APInt rhsMax = rhs.getUnsignedMax().zext(getBitWidth()*2);

        ConstantRange res = new ConstantRange(thisMin.mul(rhsMin),
                thisMax.mul(rhsMax).increase());
        return res.truncate(getBitWidth());
    }

    public ConstantRange smax(ConstantRange other)
    {
        // X smax Y is: range(smax(X_smin, Y_smin),
        //                    smax(X_smax, Y_smax))
        if (isEmptySet() || other.isEmptySet())
            return new ConstantRange(getBitWidth(), false);

        APInt newL = APInt.smax(getSignedMin(), other.getSignedMin());
        APInt newH = APInt.smax(getSignedMax(), other.getSignedMax());
        if (newL.eq(newH))
            return new ConstantRange(getBitWidth());
        return new ConstantRange(newL, newH);
    }

    public ConstantRange umax(ConstantRange other)
    {
        // X umax Y is: range(umax(X_umin, Y_umin),
        //                    umax(X_umax, Y_umax))
        if (isEmptySet() || other.isEmptySet())
            return new ConstantRange(getBitWidth(), false);

        APInt newL = APInt.umax(getUnsignedMin(), other.getUnsignedMin());
        APInt newH = APInt.umax(getUnsignedMax(), other.getUnsignedMax());
        if (newL.eq(newH))
            return new ConstantRange(getBitWidth());
        return new ConstantRange(newL, newH);
    }

    public ConstantRange udiv(ConstantRange rhs)
    {
        if (isEmptySet() || rhs.isEmptySet() || rhs.getUnsignedMax().eq(0))
            return new ConstantRange(getBitWidth());    // empty set.
        if (rhs.isFullSet())
            return new ConstantRange(getBitWidth());    // full set.

        APInt l = getUnsignedMin().udiv(rhs.getUnsignedMax());
        APInt rhsUmin = rhs.getUnsignedMin();
        if (rhsUmin.eq(0))
        {
            if (rhs.upper.eq(1))
                rhsUmin = rhs.lower;
            else
                rhsUmin = new APInt(getBitWidth(), 1);
        }

        APInt u = getUnsignedMax().udiv(rhsUmin).increase();
        // If the LHS is Full and the RHS is a wrapped interval containing 1 then
        // this could occur.
        if (l.eq(u))
            return new ConstantRange(getBitWidth());    // Full set.
        return new ConstantRange(l, u);
    }

    public void print(PrintStream os)
    {
        os.print("[");
        lower.print(os);
        os.print(",");
        upper.print(os);
        os.print(")");
    }

    public void dump()
    {
        print(System.err);
    }
}
