package lir;

import lir.ci.CiKind;
import lir.ci.CiValue;
import utils.Utils;

/**
 * @author Jianping Zeng
 */
public class LIROp1 extends LIRInstruction
{
	public enum LIRMoveKind
	{
		Normal, Volatile, Unaligned
	}

	public final CiKind kind;          // the operand type
	public final LIRMoveKind moveKind; // flag that indicate the kind of move

	/**
	 * Constructs a new LIROp1 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr    the first input operand
	 * @param result the operand that holds the result of this instruction
	 * @param kind   the kind of this instruction
	 */
	public LIROp1(LIROpcode opcode, CiValue opr, CiValue result, CiKind kind)
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
	 * @param opr    the first input operand
	 * @param result the operand that holds the result of this instruction
	 */
	public LIROp1(LIROpcode opcode, CiValue opr, CiValue result)
	{
		this(opcode, opr, result, CiKind.Illegal);
	}


	/**
	 * Constructs a new LIROp1 instruction.
	 *
	 * @param moveKind the kind of move the instruction represents
	 * @param operand  the single input operand
	 * @param result   the operand that holds the result of this instruction
	 * @param kind     the kind of this instruction
	 */
	public LIROp1(LIRMoveKind moveKind, CiValue operand, CiValue result,
			CiKind kind)
	{
		super(LIROpcode.Move, result, false, 0, 0, operand);
		this.kind = kind;
		this.moveKind = moveKind;
	}

	/**
	 * Constructs a new LIROp1 instruction.
	 *
	 * @param opcode the instruction's opcode
	 * @param opr    the first input operand
	 */
	public LIROp1(LIROpcode opcode, CiValue opr)
	{
		super(opcode, CiValue.IllegalValue, false, 0, 0, opr);
		this.kind = CiKind.Illegal;
		this.moveKind = LIRMoveKind.Normal;
		assert isInRange(opcode, LIROpcode.BeginOp1, LIROpcode.EndOp1) :
				"The " + opcode + " is not a valid LIROp1 opcode";
	}

	/**
	 * Gets the input operand of this instruction.
	 *
	 * @return opr the input operand.
	 */
	public CiValue operand()
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
					throw Utils.shouldNotReachHere();
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
