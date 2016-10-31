package frontend.exception;

public class SemanticException extends CompileException
{
	
    private static final long serialVersionUID = 3569426310328024471L;
	
	public SemanticException(String errmsg)
	{
		super(errmsg);
	}
}
