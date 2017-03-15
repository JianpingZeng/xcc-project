package jlang.exception;

import java.util.Locale;

/**
 * {@code CiBailout} is thrown when the jlang.driver refuses to compile a function
 * because of problems with the function. This jlang.exception is <i>not</i>
 * meant to indicate problems with the jlang.driver itself.
 */
public class CiBailout extends RuntimeException
{

	public static final long serialVersionUID = 8974598793458772L;

	/**
	 * Create a new {@code CiBailout}.
	 *
	 * @param reason a message indicating the reason
	 */
	public CiBailout(String reason)
	{
		super(reason);
	}

	/**
	 * Create a new {@code CiBailout}.
	 *
	 * @param format message indicating the reason with a String.format - syntax
	 * @param args   parameters to the formatter
	 */
	public CiBailout(String format, Object... args)
	{
		this(String.format(Locale.ENGLISH, format, args));
	}

	/**
	 * Create a new {@code CiBailout} t due to an internal jlang.exception being thrown.
	 *
	 * @param reason a message indicating the reason
	 * @param cause  the throwable that was the cause of the bailout
	 */
	public CiBailout(String reason, Throwable cause)
	{
		super(reason);
		initCause(cause);
	}
}
