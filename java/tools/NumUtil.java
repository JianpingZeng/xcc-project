package tools;

/**
 * @author Xlous.zeng
 */
public class NumUtil
{
	/**
	 * Determines the specified number whether is integer or not.
	 * @param v
	 * @return
	 */
	public static boolean isInt(long v)
	{
		return (int)v == v;
	}

	public static boolean isUShort(int s)
	{
		return s == (s & 0xFFFF);
	}

	public static boolean isUByte(int x)
	{
		return (x & 0xff) == x;
	}
}
