package lir.ci;

import java.util.Arrays;

import utils.Util;

/**
 * The callee save area (CSA) is a contiguous space in a stack frame
 * used to save (and restore) the VALUES of the caller's LIRRegisters.
 * This class describes the layout of a CSA in terms of its
 * {@linkplain #size getArraySize}, {@linkplain #slotSize slot getArraySize} and
 * the {@linkplain #LIRRegisters callee save LIRRegisters} covered by the CSA.
 *
 * @author Xlous.zeng
 */
public class CalleeSaveLayout
{

	/**
	 * The getArraySize (in bytes) of the CSA.
	 */
	public final int size;

	/**
	 * The getArraySize (in bytes) of an {@linkplain #registerAt(int)} indexable}
	 * slot in the CSA.
	 */
	public final int slotSize;

	/**
	 * Map from {@linkplain LIRRegister#number register numbers} to slot indexes
	 * in the CSA.
	 */
	private final int[] regNumToIndex;

	private final LIRRegister[] indexToReg;

	/**
	 * The list of LIRRegisters {@linkplain ##contains(LIRRegister)}contained}
	 * by this CSA.
	 */
	public final LIRRegister[] LIRRegisters;

	/**
	 * The offset from the frame pointer(also %rbp register in AMD64) to the CSA.
	 * IfStmt this is not known, then this field will have the value
	 * {@link Integer#MAX_VALUE}.
	 */
	public final int frameOffsetToCSA;

	/**
	 * Creates a CSA layout.
	 *
	 * @param frameOffsetToCSA
	 * @param size      getArraySize (in bytes) of the CSA. IfStmt this is {@code -1}, then
	 *                     the CSA getArraySize will be computed from {@code LIRRegisters}.
	 * @param slotSize  the getArraySize (in bytes) of an {@linkplain #registerAt(int)}
	 *                  indexable} slot in the CSA
	 * @param LIRRegisters the LIRRegisters that can be saved in the CSA
	 */
	public CalleeSaveLayout(int frameOffsetToCSA, int size, int slotSize,
			LIRRegister... LIRRegisters)
	{
		this.frameOffsetToCSA = frameOffsetToCSA;
		assert slotSize == 0 || Util.isPowerOf2(slotSize);
		this.slotSize = slotSize;
		int maxRegNum = -1;
		int maxOffset = 0;
		this.LIRRegisters = LIRRegisters;
		int offset = 0;
		for (LIRRegister reg : LIRRegisters)
		{
			assert offset % slotSize == 0;
			assert reg.number >= 0;
			if (reg.number > maxRegNum)
			{
				maxRegNum = reg.number;
			}
			if (offset > maxOffset)
			{
				maxOffset = offset;
			}
			offset += reg.spillSlotSize;
		}
		if (size == -1)
		{
			this.size = offset;
		}
		else
		{
			assert offset <= size;
			this.size = size;
		}
		size = this.size;

		this.regNumToIndex = new int[maxRegNum + 1];
		this.indexToReg = offset == 0 ?
				new LIRRegister[0] :
				new LIRRegister[offset / slotSize];
		Arrays.fill(regNumToIndex, -1);
		offset = 0;
		for (LIRRegister reg : LIRRegisters)
		{
			int index = offset / slotSize;
			regNumToIndex[reg.number] = index;
			indexToReg[index] = reg;
			offset += reg.spillSlotSize;
		}
	}

	/**
	 * Gets the offset of a given register in the CSA.
	 *
	 * @return the offset (in bytes) of {@code reg} in the CSA
	 * @throws IllegalArgumentException if {@code reg} does not have a slot in the CSA
	 */
	public int offsetOf(int reg)
	{
		return indexOf(reg) * slotSize;
	}

	/**
	 * Gets the index of a given register in the CSA.
	 *
	 * @return the index of {@code reg} in the CSA
	 * @throws IllegalArgumentException if {@code reg} does not have a slot in the CSA
	 */
	public int indexOf(int reg)
	{
		if (!contains(reg))
		{
			throw new IllegalArgumentException(String.valueOf(reg));
		}
		return regNumToIndex[reg];
	}

	/**
	 * Gets the offset of a given register in the CSA.
	 *
	 * @return the offset (in bytes) of {@code reg} in the CSA
	 * @throws IllegalArgumentException if {@code reg} does not have a slot in the CSA
	 */
	public int offsetOf(LIRRegister reg)
	{
		return offsetOf(reg.number);
	}

	/**
	 * Determines if the CSA includes a slot for a given register.
	 *
	 * @param reg the register to test
	 * @return true if the CSA isDeclScope a slot for {@code reg}
	 */
	public boolean contains(int reg)
	{
		return reg >= 0 && reg < regNumToIndex.length
				&& regNumToIndex[reg] != -1;
	}

	/**
	 * Gets the register whose slot in the CSA is at a given index.
	 *
	 * @param index an index of a slot in the CSA
	 * @return the register whose slot in the CSA is at  {@code index} or
	 * {@code null} if {@code index} does not denote a
	 * slot in the CSA aligned with a register
	 */
	public LIRRegister registerAt(int index)
	{
		if (index < 0 || index >= indexToReg.length)
		{
			return null;
		}
		return indexToReg[index];
	}

	@Override public String toString()
	{
		StringBuilder sb = new StringBuilder("[");
		for (LIRRegister reg : LIRRegisters)
		{
			if (sb.length() != 1)
			{
				sb.append(", ");
			}
			sb.append(reg).append("{+").append(offsetOf(reg)).append('}');
		}
		return sb.append("] getArraySize=").append(size).toString();
	}
}

