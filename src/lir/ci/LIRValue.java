package lir.ci;

import java.io.Serializable;

/**
 * Abstract base class for VALUES manipulated by the compiler.
 * All VALUES have a {@linkplain LIRKind kind} and are immutable.
 *
 * @author Jianping Zeng
 * @version 1.0
 */
public abstract class LIRValue implements Serializable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static LIRValue IllegalValue = new LIRValue(LIRKind.Illegal)
	{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override public String name()
		{
			return "<illegal>";
		}

		@Override public LIRRegister asRegister()
		{
			return LIRRegister.None;
		}

		@Override public int hashCode()
		{
			return -1;
		}

		@Override public boolean equals(Object obj)
		{
			return obj == this;
		}

		@Override public boolean equalsIgnoringKind(LIRValue other)
		{
			return other == this;
		}
	};

	/**
	 * The kind of this value.
	 */
	public final LIRKind kind;

	/**
	 * Initializes a new value of the specified kind.
	 *
	 * @param kind the kind
	 */
	protected LIRValue(LIRKind kind)
	{
		this.kind = kind;
	}

	public final boolean isVariableOrRegister()
	{
		return this instanceof LIRVariable || this instanceof LIRRegisterValue;
	}

	public LIRRegister asRegister()
	{
		throw new InternalError("Not a register: " + this);
	}

	public LIRAddress asAddress()
	{
		throw new InternalError("Not a address: " + this);
	}

	public final boolean isIllegal()
	{
		return this == IllegalValue;
	}

	public final boolean isLegal()
	{
		return this != IllegalValue;
	}

	/**
	 * Determines if this value represents a slot on a stack. These VALUES are created
	 * by the register allocator for spill slots. They are also used to model method
	 * parameters passed on the stack according to a specific calling convention.
	 */
	public final boolean isStackSlot()
	{
		return this instanceof StackSlot;
	}

	public final boolean isRegister()
	{
		return this instanceof LIRRegisterValue;
	}

	public final boolean isVariable()
	{
		return this instanceof LIRVariable;
	}

	public final boolean isAddress()
	{
		return this instanceof LIRAddress;
	}

	public final boolean isConstant()
	{
		return this instanceof LIRConstant;
	}

	/**
	 * Gets a string name for this value without indicating its {@linkplain #kind kind}.
	 */
	public abstract String name();

	@Override public abstract boolean equals(Object obj);

	public abstract boolean equalsIgnoringKind(LIRValue other);

	@Override public abstract int hashCode();

	@Override public final String toString()
	{
		return name() + kindSuffix();
	}

	public final String kindSuffix()
	{
		if (kind == LIRKind.Illegal)
		{
			return "";
		}
		return ":" + kind.typeChar;
	}

	public final boolean isConstant0()
	{
		return isConstant() && ((LIRConstant) this).asInt() == 0;
	}

	/**
	 * Utility for specializing how a {@linkplain LIRValue LIR LIROperand} is formatted to a string.
	 * The {@linkplain Formatter#DEFAULT default formatter} returns the value of
	 * {@link LIRValue#toString()}.
	 */
	public static class Formatter
	{
		public static final Formatter DEFAULT = new Formatter();

		/**
		 * Formats a given LIROperand as a string.
		 *
		 * @param operand the LIROperand to format
		 * @return {@code LIROperand} as a string
		 */
		public String format(LIRValue operand)
		{
			return operand.toString();
		}
	}
}
