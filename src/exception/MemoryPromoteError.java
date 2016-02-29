package exception;

/**
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/2/29.
 */
public class MemoryPromoteError extends Error
{
	public MemoryPromoteError(String mesg)
	{
		super(mesg);
	}
}
