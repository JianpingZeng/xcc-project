package lir;

import lir.ci.LIRKind;
import lir.ci.LIRValue;
import utils.Util;

/**
 * @author Xlous.zeng
 */
public class LIROp1 extends LIRInstruction
{
	public enum LIRMoveKind
	{
		Normal, Volatile, Unaligned
	}

	public final LIRKind kind;          // the LIROperand type
	public final LIRMoveKind moveKind; // flag that indicate the kind of move

	/**
	 * Constructs a new LIROp1 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr    the first input LIROperand
	 * @param result the LIROperand that holds the result of this instruction
	 * @param kind   the kind of this instruction
	 */
	public LIROp1(LIROpcode opcode, LIRValue opr, LIRValue result, LIRKind kind)
	{
		super(opcode, result, false, 0, 0, opr);
		this.kind = kind;
		this.moveKind = LIRMoveKind.Normal;
		assert isInRange(opcode, LIROpcode.BeginOp1, LIROpcode.EndOp1) :
				"The " + opcode + " is not a valid LIROp1 opcode";
	}

	/**
	 * Constructs a new LIROp1 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr    the first input LIROperand
	 * @param result the LIROperand that holds the result of this instruction
	 * @param kind   the kind of this instruction
	 * @param moveKind the instance of {@code LIRMoveKind} for specifying the the kind of move
	 */
	public LIROp1(LIROpcode opcode, LIRValue opr, LIRValue result, LIRKind kind,
			LIRMoveKind moveKind)
	{
		super(opcode, result, false, 0, 0, opr);
		this.kind = kind;
		this.moveKind = moveKind;
		assert isInRange(opcode, LIROpcode.BeginOp1, LIROpcode.EndOp1) :
				"The " + opcode + " is not a valid LIROp1 opcode";
	}

	/**
	 * Constructs a new LIROp1 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr    the first input LIROperand
	 * @param result the LIROperand that holds the result of this instruction
	 */
	public LIROp1(LIROpcode opcode, LIRValue opr, LIRValue result)
	{
		this(opcode, opr, result, LIRKind.Illegal);
	}


	/**
	 * Constructs a new LIROp1 instruction.
	 *
	 * @param moveKind the kind of move the instruction represents
	 * @param operand  the single input LIROperand
	 * @param result   the LIROperand that holds the result of this instruction
	 * @param kind     the kind of this instruction
	 */
	public LIROp1(LIRMoveKind moveKind, LIRValue operand, LIRValue result,
			LIRKind kind)
	{
		super(LIROpcode.Move, result, false, 0, 0, operand);
		this.kind = kind;
		this.moveKind = moveKind;
	}

	/**
	 * Constructs a new LIROp1 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr    the first input LIROperand
	 */
	public LIROp1(LIROpcode opcode, LIRValue opr)
	{
		super(opcode, LIRValue.IllegalValue, false, 0, 0, opr);
		this.kind = LIRKind.Illegal;
		this.moveKind = LIRMoveKind.Normal;
		assert isInRange(opcode, LIROpcode.BeginOp1, LIROpcode.EndOp1) :
				"The " + opcode + " is not a valid LIROp1 opcode";
	}

	/**
	 * Gets the input LIROperand of this instruction.
	 *
	 * @return opr the input LIROperand.
	 */
	public LIRValue operand()
	{
		return operand(0);
	}

	/**
	 * Gets the kind of move of this instruction.
	 *
	 * @return flags the constant that represents the move kind.
	 */
	public LIRMoveKind moveKind()
	{
		assert opcode
				== LIROpcode.Move : "The opcode must be of type LIROpcode.Move in LIROp1";
		return moveKind;
	}

	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitOp1(this);
	}

	@Override public String name()
	{
		if (opcode == LIROpcode.Move)
		{
			switch (moveKind())
			{
				case Normal:
					return "move";
				case Unaligned:
					return "unaligned move";
				case Volatile:
					return "volatile_move";
				default:
					throw Util.shouldNotReachHere();
			}
		}
		else
		{
			return super.name();
		}
	}

	@Override public boolean verify()
	{
		switch (opcode)
		{
			case Move:
				assert (operand().isLegal()) && (result()
						.isLegal()) : "Operand and result must be valid in a LIROp1 move instruction.";
				break;
			case Return:
				assert operand().isVariableOrRegister() || operand()
						.isIllegal() : "Operand must be (register | illegal) in a LIROp1 return instruction.";
				break;
		}
		return true;
	}
}
