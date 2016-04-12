package lir.ci;

/**
 * Denotes a register that stores a value of a fixed kind. There is exactly one
 * (canonical) instance of {@code CiRegisterValue} for each ({@link Register},
 * {@link CiKind}) pair. Use {@link Register#asValue(CiKind)} to retrieve the
 * canonical {@link CiRegisterValue} instance for a given (register,kind) pair.
 */
public final class CiRegisterValue extends CiValue
{
	/**
	 * The register.
	 */
	public final Register reg;

	/**
	 * Should only be called from {@link Register#Register} to ensure canonicalization.
	 */
	CiRegisterValue(CiKind kind, Register register)
	{
		super(kind);
		this.reg = register;
	}

	@Override public int hashCode()
	{
		return kind.ordinal() ^ reg.number;
	}

	@Override public boolean equals(Object o)
	{
		return o == this;
	}

	@Override public boolean equalsIgnoringKind(CiValue other)
	{
		if (other instanceof CiRegisterValue)
		{
			return ((CiRegisterValue) other).reg == reg;
		}
		return false;
	}

	@Override public String name()
	{
		return reg.name;
	}

	@Override public Register asRegister()
	{
		return reg;
	}
}
