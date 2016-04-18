package hir; 

import java.util.Iterator;
import java.util.List;

import optimization.ConstantProp;
import optimization.DCE;
import optimization.GVN;
import optimization.UCE;
import utils.Context;

/**
 * <p>
 * This class implements overrall container for the HIR(high-level IR) and directs
 * its construction, optimization and finalization.
 * </p>
 * <p>
 * This class consists of multiple {@link Variable} and/or {@link Method}, there
 * is an only control flow graph attached to every method declaration at the AST.
 * </p>
 * <p>
 * Further, a sorts of basic block has filled into CFG in the execution
 * order of program. At the any basic block, a large amount of quads are ordered 
 * in execution order.
 * </p>
 * @author Jianping Zeng <z1215jping@hotmail.com>
 */
public class HIR
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
	 * @return	The instance of {@link HIR}
	 */
	public static HIR instance(Context context, List<Variable> vars,
			List<Method> methods)
	{
		HIR instance = (HIR)context.get(HIRKey);
		if (instance == null)
		{
			instance = new HIR(vars, methods);
			context.put(HIRKey, instance);
		}
		return instance;
	}
	
	/**
	 * Constructor.
	 * @param vars
	 * @param methods
	 */
	private HIR(List<Variable> vars, List<Method> methods)
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
		// performs dead code elimination.
		for (Method m : methods)
			new DCE(m).run();

		// performs constant folding and propagation
		ConstantProp prop = new ConstantProp();
		for (Method m : methods)
			prop.runOnMethod(m);


		// after DCE, There are useless control flow be introduced by other
		// optimization. So that the useless control flow elimination is desired
		// as follows.
		// 1.merges redundant branch instruction.
		// 2.unlinks empty basic block
		// 3.merges basic block
		// 4.hoist merge instruction
		UCE uce = new UCE();
		for (Method m : methods)
			uce.clean(m);

		// performs global common subexpression elimination through global value
		// numbering.
		for (Method m : methods)
			new GVN(m);


		
	}

	public Iterator<Method> iterator()
	{
		return methods.iterator();
	}
}
