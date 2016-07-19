package compiler;

import optimization.ConstantProp;
import optimization.DCE;
import optimization.GVN;
import optimization.InductionVarSimplify;
import optimization.LICM;
import optimization.LoopAnalysis;
import optimization.LoopSimplify;
import optimization.UCE;
import hir.Method;
import hir.Module;
import utils.Context;

/**
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
		ConstantProp prop = new ConstantProp();
		UCE uce = new UCE();
		LoopSimplify simplificator = new LoopSimplify();
		InductionVarSimplify ivSimplicator = new InductionVarSimplify();
		
		for (Method m : hir)
		{
			/****** C1 optimization stage ***/
    		// performs dead code elimination.    		
			new DCE(m).runOnMethod();

			prop.runOnMethod(m);
    
    		// after DCE, There are useless control flow be introduced by other
    		// optimization. So that the useless control flow elimination is desired
    		// as follows.
    		// 1.merges redundant branch instruction.
    		// 2.unlinks empty basic block
    		// 3.merges basic block
    		// 4.hoist merge instruction
			uce.clean(m);
			
			
			/** C2 optimization stage*/
    		// performs global common subexpression elimination through global value
    		// numbering.
			new GVN(m);
    
    		// perform loop analysis and optimization
			// 1. perform loop analysis
    		new LoopAnalysis(m).runOnFunction();       	
    		
    		// 2. perform loop simplification
    		simplificator.runOnFunction(m);
    		
    		// 3.perform loop invariant code motion
    		new LICM(m).runOnLoop();    	
    		
    		// performs dead code elimination.    		
			new DCE(m).runOnMethod();
			
			/** C3 optimization stage */
			ivSimplicator.runOnLoop(m);			
			
			// for removal of useless induction variables.
			new DCE(m).runOnMethod();
			
			/** C4 optimization stage */			
		}	
	}

}
