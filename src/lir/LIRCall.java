package lir;

import lir.ci.CiValue;

import java.util.List;

/**
 * This class represents a call instruction which calls to any function.
 * @author Jianping Zeng
 */
public class LIRCall extends LIRInstruction
{
	/**
	 * The targetAbstractLayer of the call.
	 */
	public final Object target;

	private final int targetAddressIndex;

	public final List<CiValue> pointerSlots;

	private static CiValue[] toArray(List<CiValue> arguments)
	{
		return arguments.toArray(new CiValue[arguments.size()]);
	}

	public LIRCall(LIROpcode opcode, Object target, CiValue result,
			List<CiValue> arguments,
			boolean calleeSaved, List<CiValue> pointerSlots)
	{
		super(opcode, result, !calleeSaved, 0, 0, toArray(arguments));
		this.pointerSlots = pointerSlots;
		if (opcode == LIROpcode.DirectCall)
		{
			this.targetAddressIndex = -1;
		}
		else
		{
			// The last argument is the LIROperand holding the address for the
			// indirect call
			this.targetAddressIndex = arguments.size() - 1;
		}
		this.target = target;
	}

	/**
	 * Emits targetAbstractLayer assembly code for this instruction.
	 *
	 * @param masm the targetAbstractLayer assembler
	 */
	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitCall(this);
	}

	/**
	 * Returns the receiver for this method call.
	 *
	 * @return the receiver
	 */
	public CiValue receiver()
	{
		return operand(0);
	}

	public CiValue targetAddress()
	{
		if (targetAddressIndex >= 0)
		{
			return operand(targetAddressIndex);
		}
		return null;
	}

	@Override public String operationString(CiValue.Formatter operandFmt)
	{
		StringBuilder buf = new StringBuilder();
		if (result().isLegal())
		{
			buf.append(operandFmt.format(result())).append(" = ");
		}
		String targetAddress = null;
		if (opcode != LIROpcode.DirectCall)
		{
			if (targetAddressIndex >= 0)
			{
				targetAddress = operandFmt.format(targetAddress());
				buf.append(targetAddress);
			}
		}
		buf.append('(');
		boolean first = true;
		for (LIROperand operandSlot : operands)
		{
			String operand = operandFmt.format(operandSlot.value(this));
			if (!operand.isEmpty() && !operand.equals(targetAddress))
			{
				if (!first)
				{
					buf.append(", ");
				}
				else
				{
					first = false;
				}
				buf.append(operand);
			}
		}
		buf.append(')');
		return buf.toString();
	}
}
