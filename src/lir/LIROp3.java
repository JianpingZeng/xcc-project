package lir;

import lir.ci.LIRValue;

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
	 * @param opr1   the first input LIROperand
	 * @param opr2   the second input LIROperand
	 * @param opr3   the third input LIROperand
	 * @param result the result LIROperand
	 */
	public LIROp3(LIROpcode opcode, LIRValue opr1, LIRValue opr2, LIRValue opr3,
			LIRValue result)
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
	public LIRValue opr1()
	{
		return operand(0);
	}

	/**
	 * Gets the opr2 of this class.
	 *
	 * @return the opr2
	 */
	public LIRValue opr2()
	{
		return operand(1);
	}

	/**
	 * Emits assembly code for this instruction.
	 *
	 * @param masm the targetAbstractLayer assembler
	 */
	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitOp3(this);
	}
}

