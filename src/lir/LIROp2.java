package lir;

import hir.Condition;
import lir.ci.*;
import lir.ci.CiValue.Formatter;

/**
 * The{@code LIROp2} class represents a LIR instruction that performs an operation on two operands.
 * @author Jianping Zeng
 */

public class LIROp2 extends LIRInstruction
{

	final Condition condition;

	/**
	 * Constructs a new LIROp2 instruction.
	 *
	 * @param opcode    the instruction's opcode
	 * @param condition the instruction's condition
	 * @param opr1      the first input LIROperand
	 * @param opr2      the second input LIROperand
	 */
	public LIROp2(LIROpcode opcode, Condition condition, CiValue opr1,
			CiValue opr2)
	{
		super(opcode, CiValue.IllegalValue, false, 0, 0, opr1, opr2);
		this.condition = condition;
		assert opcode
				== LIROpcode.Cmp : "Instruction opcode should be of type LIROpcode.Cmp";
	}

	/**
	 * Constructs a new LIROp2 instruction.
	 *
	 * @param opcode    the instruction's opcode
	 * @param condition the instruction's condition
	 * @param opr1      the first input LIROperand
	 * @param opr2      the second input LIROperand
	 * @param result    the LIROperand that holds the result of this instruction
	 */
	public LIROp2(LIROpcode opcode, Condition condition, CiValue opr1,
			CiValue opr2, CiValue result)
	{
		super(opcode, result, false, 0, 0, opr1, opr2);
		this.condition = condition;
		assert opcode
				== LIROpcode.Cmove : "Instruction opcode should be of type LIROpcode.Cmove";
	}

	/**
	 * Constructs a new LIROp2 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr1   the first input LIROperand
	 * @param opr2   the second input LIROperand
	 * @param result the LIROperand that holds the result of this instruction
	 * @param kind   the kind of this instruction
	 */
	public LIROp2(LIROpcode opcode, CiValue opr1, CiValue opr2, CiValue result,
			CiKind kind, boolean hasCall)
	{
		super(opcode, result, hasCall, 0, 0, opr1, opr2);
		this.condition = null;
		assert opcode != LIROpcode.Cmp && isInRange(opcode, LIROpcode.BeginOp2,
				LIROpcode.EndOp2) :
				"The " + opcode + " is not a valid LIROp2 opcode";
	}


	/**
	 * Constructs a new LIROp2 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr1   the first input LIROperand
	 * @param opr2   the second input LIROperand
	 * @param result the LIROperand that holds the result of this instruction
	 * @param tmp    the temporary LIROperand used by this instruction
	 */
	public LIROp2(LIROpcode opcode, CiValue opr1, CiValue opr2, CiValue result,
			CiValue tmp)
	{
		super(opcode, result, false, 0, 1, opr1, opr2, tmp);
		this.condition = null;
		assert opcode != LIROpcode.Cmp && isInRange(opcode, LIROpcode.BeginOp2,
				LIROpcode.EndOp2) :
				"The " + opcode + " is not a valid LIROp2 opcode";
	}
	/**
	 * Constructs a new LIROp2 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr1   the first input LIROperand
	 * @param opr2   the second input LIROperand
	 * @param result the LIROperand that holds the result of this instruction
	 */
	public LIROp2(LIROpcode opcode, CiValue opr1, CiValue opr2, CiValue result)
	{
		this(opcode, opr1, opr2, result, CiKind.Illegal, false);
	}

	/**
	 * Gets the first input LIROperand.
	 *
	 * @return opr1 the first input LIROperand
	 */
	public CiValue operand1()
	{
		return operand(0);
	}

	/**
	 * Gets the second input LIROperand.
	 *
	 * @return opr2 the second input LIROperand
	 */
	public CiValue operand2()
	{
		return operand(1);
	}

	/**
	 * Gets the temporary LIROperand of this instruction.
	 *
	 * @return tmp the temporary LIROperand of this instruction
	 */
	public CiValue tmp()
	{
		return operand(2);
	}

	/**
	 * Gets the condition of this instruction, if it is a Cmp or Cmove LIR instruction.
	 *
	 * @return condition the condition of this instruction
	 */
	public Condition condition()
	{
		assert opcode == LIROpcode.Cmp || opcode
				== LIROpcode.Cmove : "Field access only valid for cmp and cmove";
		return condition;
	}

	/**
	 * Emit targetAbstractLayer assembly code for this instruction.
	 *
	 * @param masm the targetAbstractLayer assembler
	 */
	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitOp2(this);
	}

	/**
	 * Prints this instruction.
	 */
	@Override public String operationString(Formatter operandFmt)
	{
		if (opcode == LIROpcode.Cmove)
		{
			return condition.toString() + " " + super
					.operationString(operandFmt);
		}
		return super.operationString(operandFmt);
	}
}

