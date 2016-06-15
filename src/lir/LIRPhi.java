package lir;

import hir.BasicBlock;
import lir.ci.LIRValue;

/**
 * The class definition of {@linkplain LIRPhi}.
 * @author Xlous.zeng
 */
public final class LIRPhi extends LIRInstruction
{
	private final int incomings;
	private final LIRValue[] args;
	private final BasicBlock[] blocks;

	public LIRPhi(LIROpcode opcode, LIRValue[] values, BasicBlock[] blocks, LIRValue result)
	{
		super(opcode, result, false, 1, 1, values);
		assert values.length == blocks.length;
		assert opcode == LIROpcode.Phi : "This must be Phi instruction";

		incomings = values.length;
		this.args = values;
		this.blocks = blocks;
	}

	public int numOfIncomingValues()
	{
		return incomings;
	}

	public LIRValue incomingValueAt(int index)
	{
		assert index>= 0 && index < incomings :
				"index into incoming value out of range";
		return args[index];
	}

	public BasicBlock basicBlockAt(int index)
	{
		assert index>= 0 && index < incomings :
				"index into basic block out of range";
		return blocks[index];
	}

	/**
	 * Abstract method to be used to emit targetAbstractLayer code for this instruction.
	 *
	 * @param masm the targetAbstractLayer assembler.
	 */
	@Override public void emitCode(LIRAssembler masm)
	{
	}
}
