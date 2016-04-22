package utils;

import hir.Method;
import hir.Signature;
import lir.LIRInstruction;
import lir.alloc.Interval;
import lir.ci.LIRKind;

import java.util.List;

/**
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/2/26.
 */
public class Util
{
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

	public static boolean archKindEqual(LIRKind k1, LIRKind k2)
	{
		return k1 == k2;
	}

	public static RuntimeException unimplemented()
	{
		throw new InternalError("unimplemented");
	}

	public static int roundUp(int number, int mod)
	{
		return ((number + mod - 1) / mod) * mod;
	}

	public static LIRKind[] signatureToKinds(Method method)
	{
		return signatureToKinds(method.signature());
	}
	public static LIRKind[] signatureToKinds(Signature signature)
	{
		int args = signature.argumentCount();
		LIRKind[] result = new LIRKind[args];
		for (int i = 0; i < args; i++)
		{
			result[i]  = signature.argumentKindAt(i);
		}
		return result;
	}

	/**
	 * Sets the element at a given position of a list and ensures that this
	 * position exists. If the list is current shorter than the position, intermediate
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
}
