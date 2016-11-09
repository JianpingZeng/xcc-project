package backend.target;

import backend.type.IntegerType;

/**
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class TargetData
{
	/**
	 * default to false.
	 */
	private boolean littleEndian;
	/**
	 * Pointer size in bytes
	 */
	private byte pointerMemSize;

	public IntegerType getIntPtrType()
	{
		return IntegerType.get(getPointerSizeInBits());
	}

	public int getPointerSizeInBits() {return pointerMemSize*8;}
}
