package exception; 

import symbol.Symbol;

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

	public Symbol sym;
	
	/**
	 * A localized string describing the failure.
	 */
	public String errmsg;
	
	public CompletionFailure(String errmsg)
	{
		this.errmsg = errmsg;
	}
}
