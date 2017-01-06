package driver;

import hir.Function;
import backend.transform.ConstantPropagation;
import backend.transform.DCE;
import backend.transform.GVN;
import backend.transform.InductionVarSimplify;
import backend.transform.LICM;
import backend.analysis.LoopInfo;
import backend.transform.LoopSimplify;
import backend.transform.UCE;
import hir.Module;
import tools.Context;

/**
 * This class just was used for calling different optimization procedures
 * in specific order by hand written. Maybe the order of optimization passes
 * were applied to would be re-written guided by machine learning algorithm
 * or other approaches.
 * 
 * @author Xlous.zeng
 */
public final class Optimizer
{
	final Context context;

	public Optimizer(Context context)
	{
		this.context = context;
	}

	public void runOnModules(Module[] hirs)
	{
		for (Module module : hirs)
			runOnModules(module);
	}

	public void runOnModules(Module hir)
	{
		optimize(hir);
	}
	/**
	 * Performs several Optimization approaches over High level IR.
	 */
	private void optimize(Module hir)
	{
		// performs constant folding and propagation
		ConstantPropagation prop = new ConstantPropagation();
		UCE uce = new UCE();
		LoopSimplify simplificator = new LoopSimplify();
		InductionVarSimplify ivSimplicator = new InductionVarSimplify();
		
		for (Function m : hir)
		{
			/****** C1 backend.transform stage ***/
    		// performs dead code elimination.    		
			new DCE(m).runOnFunction();

			prop.runOnFunction(m);
    
    		// after DCE, There are useless control flow be introduced by other
    		// backend.transform. So that the useless control flow elimination is desired
    		// as follows.
    		// 1.merges redundant branch instruction.
    		// 2.unlinks empty basic block
    		// 3.merges basic block
    		// 4.hoist merge instruction
			uce.clean(m);
			
			
			/** C2 backend.transform stage*/
    		// performs global common subexpression elimination through global value
    		// numbering.
			new GVN(m);
    
    		// perform loop analysis and backend.transform
			// 1. perform loop analysis
    		new LoopInfo(m).runOnFunction();
    		
    		// 2. perform loop simplification
    		simplificator.runOnFunction(m);
    		
    		// 3.perform loop invariant code motion
    		new LICM(m).runOnLoop();    	
    		
    		// performs dead code elimination.    		
			new DCE(m).runOnFunction();
			
			/** C3 backend.transform stage */
			ivSimplicator.runOnLoop(m);			
			
			// for removal of useless induction variables.
			new DCE(m).runOnFunction();
			
			/** C4 backend.transform stage */
		}	
	}
}
