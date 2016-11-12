package frontend.exception;

/**
 * 
 * @author Xlous.zeng  
 */
public class CompletionFailure extends RuntimeException
{
	/**
	 * A default generated serial ID. 
	 */
    private static final long serialVersionUID = 1L;
	
	/**
	 * A localized string describing the failure.
	 */
	public String errmsg;
	
	public CompletionFailure(String errmsg)
	{
		this.errmsg = errmsg;
	}
}
