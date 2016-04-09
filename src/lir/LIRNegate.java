package lir;

import lir.ci.CiValue;

/**
 * The {@code LIRNegate} class definition.
 */
public class LIRNegate extends LIROp1
{
	/**
	 * Constructs a new instruction LIRNegate for a given operand.
	 *
	 * @param operand the input operand for this instruction
	 * @param result  the result operand for this instruction
	 */
	public LIRNegate(CiValue operand, CiValue result)
	{
		super(LIROpcode.Neg, operand, result);
	}
}