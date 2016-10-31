package backend.hir;

/**
 * Condition codes used in conditionals.
 * Created by Jianping Zeng  
 */
public enum Condition
{
	/**
	 * Equal.
	 */
	EQ("=="),

	/**
	 * Not equal.
	 */
	NE("!="),

	/**
	 * Signed less than.
	 */
	LT("<"),

	/**
	 * Signed less than or equal.
	 */
	LE("<="),

	/**
	 * Signed greater than.
	 */
	GT(">"),

	/**
	 * Signed greater than or equal.
	 */
	GE(">="),
	/**
	 *
	 */
	TRUE("TRUE"),
	FALSE("FALSE");

	public final String operator;

	Condition(String operator)
	{
		this.operator = operator;
	}

	public boolean check(int left, int right)
	{
		// Checkstyle: off
		switch (this)
		{
			case EQ:
				return left == right;
			case NE:
				return left != right;
			case LT:
				return left < right;
			case LE:
				return left <= right;
			case GT:
				return left > right;
			case GE:
				return left >= right;
		}
		// Checkstyle: on
		throw new IllegalArgumentException();
	}

	/**
	 * Negate this conditional.
	 *
	 * @return the condition that represents the negation
	 */
	public final Condition negate()
	{
		// Checkstyle: off
		switch (this)
		{
			case EQ:
				return NE;
			case NE:
				return EQ;
			case LT:
				return GE;
			case LE:
				return GT;
			case GT:
				return LE;
			case GE:
				return LT;
		}
		// Checkstyle: on
		throw new IllegalArgumentException();
	}

	/**
	 * Mirror this conditional (i.e. commute "a op b" to "b op' a")
	 *
	 * @return the condition representing the equivalent commuted operation
	 */
	public final Condition mirror()
	{
		// Checkstyle: off
		switch (this)
		{
			case EQ:
				return EQ;
			case NE:
				return NE;
			case LT:
				return GT;
			case LE:
				return GE;
			case GT:
				return LT;
			case GE:
				return LE;
		}
		// Checkstyle: on
		throw new IllegalArgumentException();
	}

	/**
	 * Checks if this conditional operation is commutative.
	 *
	 * @return {@code true} if this operation is commutative
	 */
	public final boolean isCommutative()
	{
		return this == EQ || this == NE;
	}

}
