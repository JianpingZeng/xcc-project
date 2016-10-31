package backend.lir;

import hir.Operator;
import backend.lir.ci.LIRValue;

/**
 * The {@code LIRConvert} class definition.
 * @author Xlous.zeng
 */
public class LIRConvert extends LIROp1
{

	public final Operator opcode;

	/**
	 * Constructs a new instruction LIRConvert for a given LIROperand.
	 *
	 * @param opcode the opcode of this conversion
	 * @param operand  the input LIROperand for this instruction
	 * @param result   the getReturnValue LIROperand for this instruction
	 */
	public LIRConvert(Operator opcode, LIRValue operand, LIRValue result)
	{
		super(LIROpcode.Convert, operand, result);
		this.opcode = opcode;
	}

	/**
	 * Emits targetAbstractLayer assembly code for this LIRConvert instruction.
	 *
	 * @param masm the LIRAssembler
	 */
	@Override 
	public void emitCode(LIRAssembler masm)
	{
		//masm.emitConvert(this);
	}

	/**
	 * Prints this instruction to a LogStream.
	 */
	@Override public String operationString(LIRValue.Formatter operandFmt)
	{
		return "[" + opcode.name() + "] " + super.operationString(operandFmt);
	}
}

