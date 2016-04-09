package lir;

import lir.LIRInstruction;
import lir.ci.*;

/**
 * An instruction operand. If the register allocator can modify this operand
 * (e.g. to replace a variable with a register), then it will have a corresponding
 * entry in the {@link LIRInstruction#allocatorOperands} list of an instruction.
 *
 * @author Jianping Zeng
 */
public class LIROperand
{
	/**
	 * The value of the operand.
	 */
	CiValue value;

	LIROperand(CiValue value)
	{
		this.value = value;
	}

	/**
	 * Gets the value of this operand. This may still be a {@linkplain CiVariable}
	 * if the register allocator has not yet assigned a register or stack address to the operand.
	 *
	 * @param inst the instruction containing this operand
	 */
	public CiValue value(LIRInstruction inst)
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

		@Override public CiValue value(LIRInstruction inst)
		{
			if (value == null)
			{
				CiValue value = inst.allocatorOperands.get(index);
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
	 * An address operand with at least one {@linkplain CiVariable variable} constituent.
	 */
	static class LIRAddressOperand extends LIROperand
	{
		int base;
		int index;

		LIRAddressOperand(int base, int index, CiAddress address)
		{
			super(address);
			assert base != -1 || index
					!= -1 : "address should have at least one variable part";
			this.base = base;
			this.index = index;
		}

		@Override public CiValue value(LIRInstruction inst)
		{
			if (base != -1 || index != -1)
			{
				CiAddress address = (CiAddress) value;
				CiValue baseOperand = base == -1 ?
						address.base :
						inst.allocatorOperands.get(base);
				CiValue indexOperand = index == -1 ?
						address.index :
						inst.allocatorOperands.get(index);
				if (address.index.isLegal())
				{
					assert indexOperand.isVariableOrRegister();
					if (baseOperand.isVariable() || indexOperand.isVariable())
					{
						return address;
					}
				}
				else
				{
					if (baseOperand.isVariable())
					{
						return address;
					}
				}
				value = new CiAddress(address.kind, baseOperand, indexOperand,
						address.scale, address.displacement);
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
