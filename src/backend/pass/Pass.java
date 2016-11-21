package backend.pass;

import backend.hir.Module;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class Pass
{
	private PassInfo passInfo;

	public abstract String getPassName();

	public PassInfo getPassInfo() {return passInfo;}
}
