package lir;

import lir.ci.*;

/**
 * An instruction LIROperand. If the register allocator can modify this LIROperand
 * (e.g. to replace a variable with a register), then it will have a corresponding
 * entry in the {@link LIRInstruction#allocatorOperands} list of an instruction.
 *
 * @author Jianping Zeng
 */
public class LIROperand
{
	/**
	 * The value of the LIROperand.
	 */
	LIRValue value;

	LIROperand(LIRValue value)
	{
		this.value = value;
	}

	/**
	 * Gets the value of this LIROperand. This may still be a {@linkplain LIRVariable}
	 * if the register allocator has not yet assigned a register or stack address
	 * to the LIROperand.
	 *
	 * @param inst the instruction containing this LIROperand
	 */
	public LIRValue value(LIRInstruction inst)
	{
		return value;
	}

	@Override public String toString()
	{
		return value.toString();
	}

	static class LIRVariableOperand extends LIROperand
	{
		/**
		 * Index into an instruction's {@linkplain LIRInstruction#allocatorOperands
		 * allocator operands}.
		 */
		final int index;

		LIRVariableOperand(int index)
		{
			super(null);
			this.index = index;
		}

		@Override public LIRValue value(LIRInstruction inst)
		{
			if (value == null)
			{
				LIRValue value = inst.allocatorOperands.get(index);
				if (value.isVariable())
				{
					return value;
				}
				this.value = value;
			}
			return value;
		}

		@Override public String toString()
		{
			if (value == null)
			{
				return "operands[" + index + "]";
			}
			return value.toString();
		}
	}

	/**
	 * An address LIROperand with at least one {@linkplain LIRVariable variable} constituent.
	 */
	static class LIRAddressOperand extends LIROperand
	{
		int base;
		int index;

		LIRAddressOperand(int base, int index, LIRAddress LIRAddress)
		{
			super(LIRAddress);
			assert base != -1 || index
					!= -1 : "LIRAddress should have at least one variable part";
			this.base = base;
			this.index = index;
		}

		@Override public LIRValue value(LIRInstruction inst)
		{
			if (base != -1 || index != -1)
			{
				LIRAddress LIRAddress = (LIRAddress) value;
				LIRValue baseOperand = base == -1 ?
						LIRAddress.base :
						inst.allocatorOperands.get(base);
				LIRValue indexOperand = index == -1 ?
						LIRAddress.index :
						inst.allocatorOperands.get(index);
				if (LIRAddress.index.isLegal())
				{
					assert indexOperand.isVariableOrRegister();
					if (baseOperand.isVariable() || indexOperand.isVariable())
					{
						return LIRAddress;
					}
				}
				else
				{
					if (baseOperand.isVariable())
					{
						return LIRAddress;
					}
				}
				value = new LIRAddress(LIRAddress.kind, baseOperand, indexOperand,
						LIRAddress.scale, LIRAddress.displacement);
				base = -1;
				index = -1;
			}
			return value;
		}

		@Override public String toString()
		{
			return value.toString();
		}
	}
}
