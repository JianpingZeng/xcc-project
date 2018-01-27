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
	 * A flag indicates whether dump debug information.
	 */
	public static boolean DEBUG;
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
		return 31 - Integer.numberOfLeadingZeros(val);
	}

	public static int log2(long val)
	{
		return 63 - Long.numberOfLeadingZeros(val);
	}

	public static int log2Ceil(int val)
	{
		return 32 - countLeadingZeros32(val - 1);
	}

	public static int log2Ceil(long val)
	{
		return 64 - countLeadingZeros64(val - 1);
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
	 * Rounds the input getNumOfSubLoop up to the most least getNumOfSubLoop of pow of align,  which
	 * not less than {@code getNumOfSubLoop}.
	 * @param size
	 * @param align
	 * @return
	 */
	public static int roundUp(int size, int align)
	{
		// insufficient algorithm
		// return (getNumOfSubLoop/align + 1) * align;
		return (size+(align-1))&~(align-1);
	}
	/**
	 * Rounds the input getNumOfSubLoop up to the most least getNumOfSubLoop of pow of align,  which
	 * not less than {@code getNumOfSubLoop}.
	 * @param size
	 * @param align
	 * @return
	 */
	public static long roundUp(long size, long align)
	{
		// insufficient algorithm
		// return (getNumOfSubLoop/align + 1) * align;
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
	 * Computes the number of one bit about given value of jlang.type long when isUnsigned
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
     *      <caption>"Truth table" for an unsigned comparison x &lt; y using signed arithmetic</caption>
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

    private static int[] mod37BitPosition =
    {
		    32, 0, 1, 26, 2, 23, 27, 0, 3, 16, 24, 30, 28, 11, 0, 13,
		    4, 7, 17, 0, 25, 22, 31, 15, 29, 10, 12, 6, 0, 21, 14, 9,
		    5, 20, 8, 19, 18
    };
	public static int countTrailingZeros(int value)
	{
		return mod37BitPosition[(-value & value) % 37];
	}

	public static int countTrailingOnes(int value)
	{
		return countTrailingZeros(~value);
	}

	private static int[] mod67Position =
	{
			64, 0, 1, 39, 2, 15, 40, 23, 3, 12, 16, 59, 41, 19, 24, 54,
			4, 64, 13, 10, 17, 62, 60, 28, 42, 30, 20, 51, 25, 44, 55,
			47, 5, 32, 65, 38, 14, 22, 11, 58, 18, 53, 63, 9, 61, 27,
			29, 50, 43, 46, 31, 37, 21, 57, 52, 8, 26, 49, 45, 36, 56,
			7, 48, 35, 6, 34, 33, 0
	};
	public static int countTrailingZeros(long value)
	{
		return mod67Position[(int)((-value & value) % 67)];
	}

	public static int countTrailingOnes(long value)
	{
		return countTrailingZeros(~value);
	}

	public static int countPoplutation(int value)
	{
		int v = value - ((value >>> 1) & 0x55555555);
		v = (v & 0x33333333) + ((v >>> 2) & 0x33333333);
		return ((v + (v >>> 4) & 0xF0F0F0F) * 0x1010101) >>> 24;
	}

	public static int countPopulation(long value)
	{
		long v = value - ((value >> 1) & 0x5555555555555555L);
		v = (v & 0x3333333333333333L) + ((v >> 2) & 0x3333333333333333L);
		v = (v + (v >> 4)) & 0x0F0F0F0F0F0F0F0FL;
		return (int)(v * 0x0101010101010101L >> 56);
	}

	public static void Debug(Object... args)
	{
		if(Util.DEBUG)
		{
			for (Object arg : args)
				System.err.println(arg);
		}
	}

	public static String fixedLengthString(int repeatNum, String unit)
	{
		return String.format("%1$" + repeatNum + "s", unit);
	}

	public static String fixedLengthString(int repeatNum, char unit)
	{
		if (repeatNum <= 0)
			return "";
		return String.format("%1$" + repeatNum + "s", unit);
	}

	/**
	 * Checks the file path specified by arg {@code path} is absolutely or not.
	 * @param path
	 * @return
	 */
	public static boolean isAbsolutePath(String path)
	{
	    assert !(path == null || path.isEmpty());
		if (OSInfo.isWindows())
        {
            // Windows operation system.
            return path.startsWith("[A-Za-z]:");
        }
        else
        {
            // Unix like operation system.
            return path.startsWith("/");
        }
	}

	/**
	 * Return the next number that is power of 2 and greater than the given parameter.
	 * @param val
	 * @return
	 */
    public static int NextPowerOf2(long val)
    {
	    val |= (val >> 1);
	    val |= (val >> 2);
	    val |= (val >> 4);
	    val |= (val >> 8);
	    val |= (val >> 16);
	    val |= (val >> 32);
	    return (int) (val + 1);
    }

	public static int findFirstNonOf(String src, String delims)
	{
		return findFirstNonOf(src, delims, 0);
	}

	/**
	 * Return the index to the first sub-string isn't match the specified
	 * delim string from startIdx position.
	 * @param src
	 * @param delims
	 * @param startIdx
	 * @return Return -1 when no found. Otherwise return the specified location.
	 */
    public static int findFirstNonOf(String src, String delims, int startIdx)
    {
		for (int i = startIdx; i < src.length(); i++)
			if (delims.indexOf(src.charAt(i)) == -1)
				return i;

		return src.length();
    }

    public static int findFirstOf(String src, String delims, int startIdx)
    {
	    for (int i = startIdx; i < src.length(); i++)
		    if (delims.indexOf(src.charAt(i)) != -1)
			    return i;

	    return -1;
    }

    public static boolean isInt32(long val)
    {
    	return (int)val == val;
    }

	/***
	 * Computes the edit distance between two string. The edit distance is defined
	 * as follows.
	 * The number of operation needed to be performed to transfrom the str1 to str2
	 * 1.delete a character.
	 * 2.add a character.
	 * 3.replace the old one with new.
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static int getEditDistance(String str1, String str2)
	{
		if ((str2 == null || str1 == null))
			return 0;

		if (str1.isEmpty() && str2.isEmpty())
			return 0;
		if (str1.length() == 1 && str2.length() == 1)
			return 1;

        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();
        int[] costs = new int[str2.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= str1.length(); i++)
        {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= str2.length(); j++)
            {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        str1.charAt(i - 1) == str2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[str2.length()];
	}

	/**
	 * A function similar with strpbrk in C library.
	 * @param src
	 * @param pattern
	 * @return
	 */
	public static int strpbrk(String src, String pattern)
	{
		for (int i = 0, e = src.length(); i != e; i++)
		{
			if (pattern.indexOf(src.charAt(i)) != -1)
				return i;
		}
		return -1;
	}

	/**
	 * <pre>
	 * Converts a value of type long to double in bit sbuject to IEEE754 standard.
	 * It equivalence to following C code:
	 * double bitsToDouble(long val)
	 * {
	 *   union
	 *   {
	 *     long l;
	 *     double d;
	 *   }t;
	 *   t.l = val;
	 *   return t.d;
	 * }
	 * </pre>
	 * @param val
	 * @return
	 */
	public static double bitsToDouble(long val)
	{
		int sign = (int) ((val >>> 63)&0x1);
		int exp = (int)(val >>> 52) & ((1<<11) - 1) - 1023;
		long r = (val & ((1L << 52)-1));
		double t = 0;
		for (int i = 0; i < 52; i++)
		{
			t += r&0x1;
			t /= 2;
			r >>>= 1;
		}
		double res = Math.pow(2.0, exp) * (1.0 + t);
		return sign==1?-res:res;
	}

	/**
	 * <pre>
	 * Converts a value of type int to float in bit. It equivalence to following
	 * C code:
	 * float bitsToDouble(int val)
	 * {
	 *   union
	 *   {
	 *     int i;
	 *     float f;
	 *   }t;
	 *   t.i = val;
	 *   return t.f;
	 * }
	 * </pre>
	 * @param val
	 * @return
	 */
	public static float bitsToFloat(int val)
	{
		int sign = (val >>> 31)&0x1;
		int exp = val >>> 23 & ((1<<8) - 1) - 127;
		int r = (val & ((1 << 23)-1));
		float t = 0;
		for (int i = 0; i < 23; i++)
		{
			t += r&0x1;
			t /= 2;
			r >>>= 1;
		}
		float res = (float) (Math.pow(2.0f, exp) * (1.0 + t));
		return sign==1?-res:res;
	}

	/**
	 * Converts a float value into integer in bitwise.
	 * Likewise {@linkplain #bitsToFloat(int)}.
	 * @param val
	 * @return
	 */
	public static int floatToBits(float val)
	{
		return Float.floatToRawIntBits(val);
	}

	/**
	 * Converts a double value to long in bitwise. Like {@linkplain #bitsToDouble(long)}.
	 * @param val
	 * @return
	 */
	public static long doubleToBits(double val)
	{
		return Double.doubleToRawLongBits(val);
	}

	public static char hexDigit(int x)
	{
		return x < 10 ? (char)('0' + x) :(char)('A' + x - 10);
	}
}
