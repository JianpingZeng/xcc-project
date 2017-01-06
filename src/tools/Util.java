package tools;

import java.util.List;


/**
 * @author Xlous.zeng.
 */
public class Util
{
	/* Minimum of signed integral types.  */
	public static final byte INT8_MIN	=	(-128);
	public static final short INT16_MIN	=	(-32767-1);
	public static final int INT32_MIN	=	(-2147483647-1);
	public static final long INT64_MIN	=	Long.MIN_VALUE;

	/* Maximum of signed integral types.  */
	public static final byte INT8_MAX  =		(127);
	public static final short  INT16_MAX =	(32767);
	public static final int INT32_MAX =		(2147483647);
	public static final long INT64_MAX	= 9223372036854775807L;

	/* Maximum of unsigned integral types.  */
	public static final short UINT8_MAX = 255;
	public static final int UINT16_MAX =		(65535);
	public static final long UINT32_MAX	= 4294967295L;
	public static final Long UINT64_MAX	= Long.parseUnsignedLong("18446744073709551615");
	/**
	 * Utility method to combine a base hash with the identity hash of one or more objects.
	 *
	 * @param hash the base hash
	 * @param x    the object to add to the hash
	 * @return the combined hash
	 */
	public static int hash1(int hash, Object x)
	{
		// always set at least one bit in case the hash wraps to zero
		return 0x10000000 | (hash + 7 * System.identityHashCode(x));
	}

	/**
	 * Utility method to combine a base hash with the identity hash of one or more objects.
	 *
	 * @param hash the base hash
	 * @param x    the first object to add to the hash
	 * @param y    the second object to add to the hash
	 * @return the combined hash
	 */
	public static int hash2(int hash, Object x, Object y)
	{
		// always set at least one bit in case the hash wraps to zero
		return 0x20000000 | (hash + 7 * System.identityHashCode(x) + 11 * System
				.identityHashCode(y));
	}

	/**
	 * Computes the log (base 2) of the specified integer, rounding down.
	 * (E.g {@code log2(8) = 3}, {@code log2(21) = 4})
	 *
	 * @param val the value
	 * @return the log base 2 of the value
	 */
	public static int log2(int val) {
		assert val > 0 && isPowerOf2(val);
		return 31 - Integer.numberOfLeadingZeros(val);
	}

	public static int log2(long val)
	{
		assert val > 0 && isPowerOf2(val);
		return 63 - Long.numberOfLeadingZeros(val);
	}

	public static boolean isPowerOf2(long val)
	{
		return val != 0 && (val & val -1) == 0;
	}

	public static RuntimeException shouldNotReachHere()
	{
		throw new InternalError("should not reach here");
	}

	public static RuntimeException shouldNotReachHere(String msg)
	{
		throw new InternalError("should not reach here, " + msg);
	}

	public static RuntimeException unimplemented()
	{
		throw new InternalError("unimplemented");
	}

	/**
	 * Rounds the input size up to the most least size of pow of align,  which
	 * not less than {@@code size}.
	 * @param size
	 * @param align
	 * @return
	 */
	public static int roundUp(int size, int align)
	{
		// insufficient algorithm
		// return (size/align + 1) * align;
		return (size+(align-1))&~(align-1);
	}
	/**
	 * Rounds the input size up to the most least size of pow of align,  which
	 * not less than {@@code size}.
	 * @param size
	 * @param align
	 * @return
	 */
	public static long roundUp(long size, long align)
	{
		// insufficient algorithm
		// return (size/align + 1) * align;
		return (size+(align-1))&~(align-1);
	}

	/**
	 * Sets the element at a given position of a list and ensures that this
	 * position exists. IfStmt the list is current shorter than the position, intermediate
	 * positions are filled with a given value.
	 *
	 * @param list the list to put the element into
	 * @param pos the position at which to insert the element
	 * @param x the element that should be inserted
	 * @param filler the filler element that is used for the intermediate positions in case the
	 *                  list is shorter than pos
	 */
	public static <T> void atPutGrow(List<T> list, int pos, T x, T filler)
	{
		if (list.size() < pos + 1)
		{
			while (list.size() < pos + 1)
			{
				list.add(filler);
			}
			assert list.size() == pos + 1;
		}

		assert list.size() >= pos + 1;
		list.set(pos, x);
	}

	/**
	 * checks out whether specified number is odd or not.
	 * @param num
	 * @return
	 */
	public static boolean isOdd(int num)
	{
		return (num & 0x1) != 0;
	}

	/**
	 * checks whether specified number is even or not.
	 * @param num
	 * @return
	 */
	public static boolean isEven(int num)
	{
		return (num & 0x1) == 0;
	}

	public static void truncate(List<?> list, int length)
	{
		while (list.size() > length)
		{
			list.remove(list.size() - 1);
		}
	}

    /**
     * Swap the reference of two object.
     * @param obj1
     * @param obj2
     */
	public static void swap(Object obj1, Object obj2)
	{
		Object temp = obj1;
        obj1 = obj2;
        obj2 = temp;
	}

	/**
	 * Computes the number of one bit about given val of jlang.type long when isUnsigned
     * determines if it is a unsigned.
	 * @param val
	 * @param isUnsigned
	 * @return
	 */
	public static int bitsOfOne(long val, boolean isUnsigned)
	{
	    int res = 0;
        while(val != 0)
        {
            res++;
            val = val & (val - 1);
        }
		return res;
	}

    /**
     * Checks if the first long number is less than second in unsigned comparison.
     *  <table border="1">
     *      <caption>"Truth table" for an unsigned comparison x < y using signed arithmetic</caption>
     *      <tr>
     *          <td></td>
     *          <th colspan="2">Top bit of x</th>
     *      </tr>
     *      <tr>
     *          <th>Top bit of y</th>
     *          <th>0</th>
     *          <th>1</th>
     *      </tr>
     *      </tr>
     *          <th>0</th>
     *          <td><tt>x &lt; y</tt><br><em>(Signed comparison)</em></td>
     *          <th>false</th>
     *      </tr>
     *      <tr>
     *          <th>1</th>
     *          <th>true</th>
     *          <td><em><tt>x &lt; y</tt><br><em>(Signed comparison)</em></td>
     *      </tr>
     *  </table>
     *
     * @param n1
     * @param n2
     * @return
     */
	public static boolean ult(long n1, long n2)
    {
        /**
        boolean cmp = (n1 < n2);
        if ((n1<0) != (n2<0))
            cmp = !cmp;
        return cmp;
         */
        // efficient method.
        return (n1<n2) ^ ((n1< 0) != (n2<0));
    }

    public static boolean ule(long n1, long n2)
    {
        return ult(n1, n2) || n1 == n2;
    }

    public static boolean uge(long n1, long n2)
    {
        return ule(n2, n1);
    }

    public static boolean ugt(long n1, long n2)
    {
        return ult(n2, n1);
    }

    public static boolean ult(int n1, int n2)
    {
        return (n1 & 0xffffffffL) < (n2 & 0xffffffffL);
    }

    public static int unsignedDiv(int i1, int i2)
    {
        long l1 = i1 & 0xffffffffL, l2 = i2 & 0xffffffffL;
        return (int) (l1 / l2);
    }

    public static long unsignedDiv(long l1, long l2)
    {
        return Long.divideUnsigned(l2, l1);
    }

	public static int countLeadingZeros64(long x)
    {
        if (x == 0)
            return 64;
        int count = 0;
        for (int shift = 64 >> 1; shift != 0; shift >>>= 1)
        {
            long temp = x >> shift;
            if (temp != 0)
                x = temp;
            else
                count |= shift;
        }
        return count;
    }

	/**
	 * this function performs the platform optimal form of
     * counting the number of zeros from the most significant bit to the first one
     * bit.
     * @param v
     * @return
	 */
    public static int countLeadingZeros32(int v)
    {
        if (v == 0) return 32;

        int count = 0;
        for (int shift = 32 >>> 1; shift!= 0; shift>>>= 1)
        {
            int temp = v>>>shift;
            if (temp!=0)
            {
                v = temp;
            }
            else
            {
                count |= shift;
            }
        }
        return count;
    }

	public static int countLeadingOnes32(int val)
    {
        return countLeadingZeros32(~val);
    }

	public static int countLeadingOnes64(long val)
    {
        return countLeadingZeros64(~val);
    }
}
