package hir;

import lir.ci.LIRKind;

/**
 * @author Xlous.zeng
 */
public abstract class User extends Value
{
	public User(LIRKind kind)
	{
		super(kind);
	}
}
