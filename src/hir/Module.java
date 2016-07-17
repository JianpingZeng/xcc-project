package hir; 

import java.util.Iterator;
import java.util.List;
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
public final class Module implements Iterable<Method>
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
	}
	public Iterator<Method> iterator()
	{
		return methods.iterator();
	}
}
