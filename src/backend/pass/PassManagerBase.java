package backend.pass;

import java.util.ArrayList;

/**
 * An abstract interface to allow code to add
 * passes to a pass manager without having to hard-code what
 * kind of pass manager it is.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class PassManagerBase
{
	protected ArrayList<Pass> passesList;

	protected PassManagerBase() {passesList = new ArrayList<>();}
	/**
	 * Add a pass tot he queue of passes to be run.
	 * These passes ownership of the pass to the PassManager.
	 * @param p
	 */
	public void add(Pass p)
	{
		passesList.add(p);
	}
}
