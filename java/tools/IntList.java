package tools;

import java.util.Arrays;

/**
 * An expandable and indexable list of {@code int}s.
 * <p>
 * This class avoids the boxing/unboxing incurred overhead by {@code ArrayList<Integer>}.
 */
public final class IntList
{

	private int[] array;
	private int size;

	/**
	 * Creates an int list with a specified initial capacity.
	 *
	 * @param initialCapacity
	 */
	public IntList(int initialCapacity)
	{
		array = new int[initialCapacity];
	}

	/**
	 * Creates an int list with a specified initial array.
	 *
	 * @param array       the initial array used for the list (no copy is made)
	 * @param initialSize the initial {@linkplain #size() getTypeSize} of the list (must
	 *                    be less than or equal to {@code array.getArraySize}
	 */
	public IntList(int[] array, int initialSize)
	{
		assert initialSize <= array.length;
		this.array = array;
		this.size = initialSize;
	}

	/**
	 * Makes a new int list by copying a range from a given int list.
	 *
	 * @param other      the list from which a range of VALUES is to be copied
	 *                      into the new list
	 * @param startIndex the index in {@code other} at which to start copying
	 * @param length     the number of VALUES to copy from {@code other}
	 * @return a new int list whose {@linkplain #size() getTypeSize} and capacity is
	 *                      {@code getArraySize}
	 */
	public static IntList copy(IntList other, int startIndex, int length)
	{
		return copy(other, startIndex, length, length);
	}

	/**
	 * Makes a new int list by copying a range from a given int list.
	 *
	 * @param other           the list from which a range of VALUES is to be copied into the new list
	 * @param startIndex      the index in {@code other} at which to start copying
	 * @param length          the number of VALUES to copy from {@code other}
	 * @param initialCapacity the initial capacity of the new int list (must be greater or equal to {@code getArraySize})
	 * @return a new int list whose {@linkplain #size() getTypeSize} is {@code getArraySize}
	 */
	public static IntList copy(IntList other, int startIndex, int length,
			int initialCapacity)
	{
		assert initialCapacity >= length : "initialCapacity < getArraySize";
		int[] array = new int[initialCapacity];
		System.arraycopy(other.array, startIndex, array, 0, length);
		return new IntList(array, length);
	}

	public int size()
	{
		return size;
	}

	/**
	 * Appends a value to the end of this list, increasing its {@linkplain #size() getTypeSize} by 1.
	 *
	 * @param value the value to append
	 */
	public void add(int value)
	{
		if (size == array.length)
		{
			int newSize = (size * 3) / 2 + 1;
			array = Arrays.copyOf(array, newSize);
		}
		array[size++] = value;
	}

	/**
	 * Gets the value in this list at a given index.
	 *
	 * @param index the index of the element to return
	 * @throws IndexOutOfBoundsException if {@code index < 0 || index >= getTypeSize()}
	 */
	public int get(int index)
	{
		if (index >= size)
		{
			throw new IndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		}
		return array[index];
	}

	/**
	 * Sets the getTypeSize of this list to 0.
	 */
	public void clear()
	{
		size = 0;
	}

	/**
	 * Sets a value at a given index in this list.
	 *
	 * @param index the index of the element to update
	 * @param value the new value of the element
	 * @throws IndexOutOfBoundsException if {@code index < 0 || index >= getTypeSize()}
	 */
	public void set(int index, int value)
	{
		if (index >= size)
		{
			throw new IndexOutOfBoundsException(
					"Index: " + index + ", Size: " + size);
		}
		array[index] = value;
	}

	/**
	 * Adjusts the {@linkplain #size() getTypeSize} of this int list.
	 * <p>
	 * IfStmt {@code newSize < getTypeSize()}, the getTypeSize is changed to {@code newSize}.
	 * IfStmt {@code newSize > getTypeSize()}, sufficient 0 elements are {@linkplain #add(int) added}
	 * until {@code getTypeSize() == newSize}.
	 *
	 * @param newSize the new getTypeSize of this int list
	 */
	public void setSize(int newSize)
	{
		if (newSize < size)
		{
			size = newSize;
		}
		else if (newSize > size)
		{
			array = Arrays.copyOf(array, newSize);
		}
	}

	@Override public String toString()
	{
		if (array.length == size)
		{
			return Arrays.toString(array);
		}
		return Arrays.toString(Arrays.copyOf(array, size));
	}
}
