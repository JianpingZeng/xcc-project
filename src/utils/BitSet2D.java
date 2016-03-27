package utils;

import java.util.BitSet;

/**
 * /**
 * This class implements a two-dimensional bitset.
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/3/18.
 */
public final class BitSet2D
{
	private BitSet set;
	private final int bitsPerSlot;

	private int bitIndex(int slotIndex, int bitIndex)
	{
		return slotIndex * bitsPerSlot + bitIndex;
	}

	private boolean verifyBitWithinSlotIndex(int index)
	{
		return index < bitsPerSlot;
	}

	public BitSet2D(int sizeInSlots, int bitsInSlot)
	{
		set = new BitSet(sizeInSlots * bitsInSlot);
		this.bitsPerSlot = bitsInSlot;
	}

	public int sizeInBits()
	{
		return set.size();
	}

	public int sizeInSlots()
	{
		return set.size() / bitsPerSlot;
	}

	public boolean isValidIndex(int slotIndex, int bitWithinSlotIndex)
	{
		assert verifyBitWithinSlotIndex(bitWithinSlotIndex);
		return bitIndex(slotIndex, bitWithinSlotIndex) < sizeInBits();
	}

	public boolean at(int slotIndex, int bitWithinSlotIndex)
	{
		assert verifyBitWithinSlotIndex(bitWithinSlotIndex);
		return set.get(bitIndex(slotIndex, bitWithinSlotIndex));
	}

	public void setBit(int slotIndex, int bitWithinSlotIndex)
	{
		assert verifyBitWithinSlotIndex(bitWithinSlotIndex);
		set.set(bitIndex(slotIndex, bitWithinSlotIndex));
	}

	public void clearBit(int slotIndex, int bitWithinSlotIndex)
	{
		assert verifyBitWithinSlotIndex(bitWithinSlotIndex);
		set.clear(bitIndex(slotIndex, bitWithinSlotIndex));
	}

	public void atPutGrow(int slotIndex, int bitWithinSlotIndex, boolean value)
	{
		int size = sizeInSlots();
		// resize the bitset
		if (slotIndex > size)
		{
			while (size <= slotIndex)
			{
				size *= 2;
			}
			BitSet newBitMap = new BitSet(size * bitsPerSlot);
			newBitMap.or(set);
			set = newBitMap;
		}

		if (value) {
			setBit(slotIndex, bitWithinSlotIndex);
		} else {
			clearBit(slotIndex, bitWithinSlotIndex);
		}
	}

	public void clear() {
		set.clear();
	}
}
