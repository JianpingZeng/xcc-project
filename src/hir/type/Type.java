package hir.type; 

/** 
 * A enumerator that represents type in targeted machine.
 * Usually, specified bit-width is reasonable selection.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2015年12月23日 上午11:28:29 
 */

public enum Type
{
	INT8, INT16, INT32, INT64;

	static public Type get(long size)
	{
		switch ((int) size)
		{
			case 1:
				return INT8;
			case 2:
				return INT16;
			case 4:
				return INT32;
			case 8:
				return INT64;
			default:
				throw new Error("unsupported asm type size: " + size);
		}
	}

	/**
	 * Gains the size in Byte of this type.
	 * @return	a integer stands for the size of memory space filled.
	 */
	public int size()
	{
		switch (this)
		{
			case INT8:
				return 1;
			case INT16:
				return 2;
			case INT32:
				return 4;
			case INT64:
				return 8;
			default:
				throw new Error("must not happen");
		}
	}
}
