package hir; 

import java.util.Iterator;
import java.util.List;

import optimization.ConstantProp;
import optimization.DCE;
import optimization.GVN;
import optimization.LICM;
import optimization.LoopAnalysis;
import optimization.LoopInversion;
import optimization.UCE;
import utils.Context;

/**
 * <p>
 * This class was served as representing a compilation unit, e.g. a source or header 
 * file in c/c++. and implementing a overall container for the Module(high-level IR)
 * </p>
 * <p>
 * There are multiple {@link Variable} and/or {@link Method} in this class, instead of 
 * only a control flow graph corresponding to each method declared at the AST.
 * </p>
 * <p>
 * Further, a sorts of basic block has filled into CFG in the execution
 * order of program. At the any basic block, a large amount of quads are ordered 
 * in execution order.
 * </p>
 * @author Xlous.zeng
 */
public class Module
{
	private static final Context.Key HIRKey = new Context.Key();
	/**
	 * A list of variable declaration.
	 */
	List<Variable> vars;
	/**
	 * A sorts of method declaration.
	 */
	List<Method> methods;
	
	/**
	 * An singleton method for instantiating an instance of this class.
	 * @param context	An context environment.
	 * @param vars	Variable declarations list.
	 * @param methods	Method declarations list
	 * @return	The instance of {@link Module}
	 */
	public static Module instance(Context context, List<Variable> vars,
			List<Method> methods)
	{
		Module instance = (Module)context.get(HIRKey);
		if (instance == null)
		{
			instance = new Module(vars, methods);
			context.put(HIRKey, instance);
		}
		return instance;
	}
	
	/**
	 * Constructor.
	 * @param vars
	 * @param methods
	 */
	private Module(List<Variable> vars, List<Method> methods)
	{
		this.vars = vars;
		this.methods = methods;

		optimize();
	}

	/**
	 * Performs several Optimization approaches over High level IR.
	 */
	private void optimize()
	{
		// performs constant folding and propagation
		ConstantProp prop = new ConstantProp();
		UCE uce = new UCE();
		for (Method m : methods)
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
    		// 2. perform loop inversion
    		new LoopInversion(m).runOnLoops();
    		// 3.perform loop invariant code motion
    		new LICM(m).runOnLoop();    	
    		
    		// performs dead code elimination.    		
			new DCE(m).runOnMethod();
			/** C3 optimization stage */
			
			/** C4 optimization stage */
			
		}	
	}

	public Iterator<Method> iterator()
	{
		return methods.iterator();
	}
}
