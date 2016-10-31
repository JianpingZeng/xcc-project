package backend.lir;

import backend.lir.ci.LIRValue;

/**
 * The {@code LIRNegate} class definition.
 */
public class LIRNegate extends LIROp1
{
	/**
	 * Constructs a new instruction LIRNegate for a given LIROperand.
	 *
	 * @param operand the input LIROperand for this instruction
	 * @param result  the getReturnValue LIROperand for this instruction
	 */
	public LIRNegate(LIRValue operand, LIRValue result)
	{
		super(LIROpcode.Neg, operand, result);
	}
}