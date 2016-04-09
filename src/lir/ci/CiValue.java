package lir.ci;

import java.io.Serializable;

/**
 * Abstract base class for values manipulated by the compiler.
 * All values have a {@linkplain CiKind kind} and are immutable.
 *
 * @author Jianping Zeng
 * @version 1.0
 */
public abstract class CiValue implements Serializable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static CiValue IllegalValue = new CiValue(CiKind.Illegal)
	{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override public String name()
		{
			return "<illegal>";
		}

		@Override public CiRegister asRegister()
		{
			return CiRegister.None;
		}

		@Override public int hashCode()
		{
			return -1;
		}

		@Override public boolean equals(Object obj)
		{
			return obj == this;
		}

		@Override public boolean equalsIgnoringKind(CiValue other)
		{
			return other == this;
		}
	};

	/**
	 * The kind of this value.
	 */
	public final CiKind kind;

	/**
	 * Initializes a new value of the specified kind.
	 *
	 * @param kind the kind
	 */
	protected CiValue(CiKind kind)
	{
		this.kind = kind;
	}

	public final boolean isVariableOrRegister()
	{
		return this instanceof CiVariable || this instanceof CiRegisterValue;
	}

	public CiRegister asRegister()
	{
		throw new InternalError("Not a register: " + this);
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
	 * Determines if this value represents a slot on a stack. These values are created
	 * by the register allocator for spill slots. They are also used to model method
	 * parameters passed on the stack according to a specific calling convention.
	 */
	public final boolean isStackSlot()
	{
		return this instanceof CiStackSlot;
	}

	public final boolean isRegister()
	{
		return this instanceof CiRegisterValue;
	}

	public final boolean isVariable()
	{
		return this instanceof CiVariable;
	}

	public final boolean isAddress()
	{
		return this instanceof CiAddress;
	}

	public final boolean isConstant()
	{
		return this instanceof CiConstant;
	}

	/**
	 * Gets a string name for this value without indicating its {@linkplain #kind kind}.
	 */
	public abstract String name();

	@Override public abstract boolean equals(Object obj);

	public abstract boolean equalsIgnoringKind(CiValue other);

	@Override public abstract int hashCode();

	@Override public final String toString()
	{
		return name() + kindSuffix();
	}

	public final String kindSuffix()
	{
		if (kind == CiKind.Illegal)
		{
			return "";
		}
		return ":" + kind.typeChar;
	}

	public final boolean isConstant0()
	{
		return isConstant() && ((CiConstant) this).asInt() == 0;
	}

	/**
	 * Utility for specializing how a {@linkplain CiValue LIR operand} is formatted to a string.
	 * The {@linkplain Formatter#DEFAULT default formatter} returns the value of
	 * {@link CiValue#toString()}.
	 */
	public static class Formatter
	{
		public static final Formatter DEFAULT = new Formatter();

		/**
		 * Formats a given operand as a string.
		 *
		 * @param operand the operand to format
		 * @return {@code operand} as a string
		 */
		public String format(CiValue operand)
		{
			return operand.toString();
		}
	}
}
