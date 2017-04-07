package backend.pass;

import backend.hir.Module;
import tools.Pair;

import java.util.ArrayList;

/**
 * his class is used to provide information that does not need to be run.
 * @author Xlous.zeng
 * @version 0.1
 */
public interface ImmutablePass extends Pass
{
	default boolean run(Module m) {return false;}

	void initializePass();

}
