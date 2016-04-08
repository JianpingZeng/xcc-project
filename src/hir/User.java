package hir;

import lir.ci.CiKind;

/**
 * @author Jianping Zeng
 */
public abstract class User extends Value
{
	public User(CiKind kind)
	{
		super(kind);
	}
}
