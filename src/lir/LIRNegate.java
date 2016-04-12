package lir;

import lir.ci.CiValue;

/**
 * The {@code LIRNegate} class definition.
 */
public class LIRNegate extends LIROp1
{
	/**
	 * Constructs a new instruction LIRNegate for a given LIROperand.
	 *
	 * @param operand the input LIROperand for this instruction
	 * @param result  the result LIROperand for this instruction
	 */
	public LIRNegate(CiValue operand, CiValue result)
	{
		super(LIROpcode.Neg, operand, result);
	}
}