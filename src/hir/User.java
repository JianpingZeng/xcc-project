package hir;

import lir.ci.LIRKind;

/**
 * @author Jianping Zeng
 */
public abstract class User extends Value
{
	public User(LIRKind kind)
	{
		super(kind);
	}
}
