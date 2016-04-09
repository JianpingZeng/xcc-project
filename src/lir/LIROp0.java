package lir;

import lir.ci.CiValue;

/**
 * @author Jianping Zeng
 */
public class LIROp0 extends LIRInstruction
{
	/**
	 * Creates a LIROp0 instruction.
	 *
	 * @param opcode the opcode of the new instruction
	 */
	public LIROp0(LIROpcode opcode)
	{
		this(opcode, CiValue.IllegalValue);
	}

	/**
	 * Creates a LIROp0 instruction.
	 *
	 * @param opcode the opcode of the new instruction
	 * @param result the result operand to the new instruction
	 */
	public LIROp0(LIROpcode opcode, CiValue result)
	{
		super(opcode, result, false);
		assert isInRange(opcode, LIROpcode.BeginOp0, LIROpcode.EndOp0) :
				"Opcode " + opcode + " is invalid for a LIROP0 instruction";
	}

	/**
	 * Emit assembly code for this instruction.
	 *
	 * @param masm the target assembler
	 */
	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitOp0(this);
	}
}
