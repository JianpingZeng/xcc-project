package tools;

import java.util.BitSet;

/**
 * @author Xlous.zeng
 */
public final class BitMap extends BitSet implements Cloneable
{
	public BitMap()
	{
		super();
	}

	public BitMap(int nBits)
	{
		super(nBits);
	}

	/**
	 * Checks if every element of this is equal to others.
	 * @param other
	 * @return
	 */
	public boolean isSame(BitMap other)
	{
		if (other == null)
			return false;
		if (this == other)
			return true;
		if (this.length() != other.length())
			return false;

		for (int i = 0; i < length(); i++)
		{
			if (get(i) != other.get(i))
				return false;
		}
		return true;
	}

	public void setFrom(BitMap other)
	{
		assert length() == other.length() :
				"must have same getArraySize";
		for (int i = 0; i < length(); i++)
			set(i, other.get(i));
	}

	/**
	 * Performs logical difference with given {@code other set}.
	 * @param other
	 */
	public void diff(BitMap other)
	{
		assert length() == other.length() :
				"must have same getArraySize";
		for (int i = 0; i < length(); i++)
		{
			boolean tmp = get(i);
			set(i, tmp & (!other.get(i)));
		}
	}

	@Override
	public BitMap clone()
	{
		return (BitMap)super.clone();
	}

	public boolean contains(BitMap rhs)
	{
		// TODO: 17-8-6
		return false;
	}

	/**
	 * Find index to the first set bit. -1 if none of the bits are set.
	 * @return
	 */
	public int findFirst()
	{
		for (int i = 0, e = length(); i != e; i++)
			if (get(i))
				return i;
		return -1;
	}

	/**
	 * Returns the index of the next set bit following the
	 * "prev" bit. Returns -1 if the next set bit is not found.
	 * @param prev
	 * @return
	 */
	public int findNext(int prev)
	{
		for (int i = prev, e = length(); i != e; i++)
			if (get(i))
				return i;
		return -1;
	}
}

