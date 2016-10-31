package backend.lir;

import backend.hir.BasicBlock;
import tools.BitMap;
import backend.asm.Label;

/**
 * This file defines a class that represents a subsequnce of machine instruction
 * but no terminal instruction in middle.
 * @author Xlous.zeng
 */
public final class LIRBlock
{
	public final BasicBlock attachedBlock;
	public final Label label;

	private LIRList lir;

	/**
	 * <p>Bit set specifying which reservedOperands are live in the entry to this
	 * machine block. There are VALUES used in this block or any of its
	 * successors where such value are not defined in this block.
	 * <p> The bit index of an LIROperand is its OperandPool.
	 */
	public BitMap livein;
	/**
	 * <p>Bit set specifying which reservedOperands are live in the exit from this
	 * machine block. There are VALUES used in this block or any of its
	 * successors where such value are not defined in this block.
	 * <p> The bit index of an LIROperand is its OperandPool.
	 */
	public BitMap liveout;
	/**
	 * <p>Bit set specifying which reservedOperands are used in this machine block.
	 * There are VALUES live in the entry to this block.
	 * <p> The bit index of an LIROperand is its OperandPool.
	 */
	public BitMap livegen;

	/**
	 * Bitset specifying which reservedOperands are defined/rewrited in this block.
	 */
	public BitMap livekill;
	public int firstLIRInstructionID;
	public int lastLIRInstructionID;

	public LIRBlock(BasicBlock block)
	{
		attachedBlock = block;
		label = new Label();
	}

	public LIRList lir()
	{
		return lir;
	}

	public void setLIR(LIRList lir)
	{
		this.lir = lir;
	}
}
