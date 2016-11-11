package backend.pass;

import backend.hir.Module;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class Pass
{
	private PassInfo passInfo;

	public String getPassName(){return null;}

	public PassInfo getPassInfo() {return passInfo;}
}
