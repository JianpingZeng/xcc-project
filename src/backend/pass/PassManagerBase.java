package backend.pass;

import tools.Pair;

import java.util.*;

import static backend.pass.PassInfoSupport.getPassInfo;

/**
 * An abstract interface to allow code to add
 * passes to a pass manager without having to hard-code what
 * kind of pass manager it is.
 *
 * @T This generic type parameter represents the type of entity on which different
 * Pass will operates.
 * @PassType Represents the type of pass.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public interface PassManagerBase
{
	enum PassManagerType
	{
		PMT_Unknow,

		PMT_ModulePassManager,

		PMT_FunctionPassManager,

		PMT_LoopPassManager,

		PMT_BasicBlockPassManager,
	}
	/**
	 * Add a pass to the queue of passes to run.
	 * @param p
	 */
	void add(Pass p);

	PassManagerType getPassManagerType();
}
