package exception;

public class CompileException extends RuntimeException
{
	/**
	 * A generated serial ID for serialization.
	 */
    private static final long serialVersionUID = -443787992749883463L;
	/**
	 * A localized string describing the failure.
	 */
	public String errmsg;
	public CompileException(String errmsg)
	{
		this.errmsg = errmsg;
	}
}
