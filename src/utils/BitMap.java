package utils;

import java.util.BitSet;

/**
 * @author Xlous.zeng
 */
public final class BitMap extends BitSet
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
				"must have same length";
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
				"must have same length";
		for (int i = 0; i < length(); i++)
		{
			boolean tmp = get(i);
			set(i, tmp & (!other.get(i)));
		}
	}
}

