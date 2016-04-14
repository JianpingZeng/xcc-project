package lir.ci;

/**
 * Denotes a register that stores a value of a fixed kind. There is exactly one
 * (canonical) instance of {@code LIRRegisterValue} for each ({@link LIRRegister},
 * {@link LIRKind}) pair. Use {@link LIRRegister#asValue(LIRKind)} to retrieve the
 * canonical {@link LIRRegisterValue} instance for a given (register,kind) pair.
 */
public final class LIRRegisterValue extends LIRValue
{
	/**
	 * The register.
	 */
	public final LIRRegister reg;

	/**
	 * Should only be called from {@link LIRRegister#LIRRegister} to ensure canonicalization.
	 */
	LIRRegisterValue(LIRKind kind, LIRRegister LIRRegister)
	{
		super(kind);
		this.reg = LIRRegister;
	}

	@Override public int hashCode()
	{
		return kind.ordinal() ^ reg.number;
	}

	@Override public boolean equals(Object o)
	{
		return o == this;
	}

	@Override public boolean equalsIgnoringKind(LIRValue other)
	{
		if (other instanceof LIRRegisterValue)
		{
			return ((LIRRegisterValue) other).reg == reg;
		}
		return false;
	}

	@Override public String name()
	{
		return reg.name;
	}

	@Override public LIRRegister asRegister()
	{
		return reg;
	}
}
