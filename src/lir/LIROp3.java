package lir;

import lir.ci.CiValue;

/**
 * The {@code LIROp3} class definition.
 */
public class LIROp3 extends LIRInstruction
{

	/**
	 * Creates a new LIROp3 instruction. A LIROp3 instruction represents a LIR instruction
	 * that has three input operands.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr1   the first input operand
	 * @param opr2   the second input operand
	 * @param opr3   the third input operand
	 * @param result the result operand
	 */
	public LIROp3(LIROpcode opcode, CiValue opr1, CiValue opr2, CiValue opr3,
			CiValue result)
	{
		super(opcode, result, false, 1, 1, opr1, opr2, opr3);
		assert isInRange(opcode, LIROpcode.BeginOp3, LIROpcode.EndOp3) :
				"The " + opcode + " is not a valid LIROp3 opcode";
	}

	/**
	 * Gets the opr1 of this class.
	 *
	 * @return the opr1
	 */
	public CiValue opr1()
	{
		return operand(0);
	}

	/**
	 * Gets the opr2 of this class.
	 *
	 * @return the opr2
	 */
	public CiValue opr2()
	{
		return operand(1);
	}

	/**
	 * Emits assembly code for this instruction.
	 *
	 * @param masm the target assembler
	 */
	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitOp3(this);
	}
}

