package hir; 

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import utils.Context;

/**
 * <p>
 * This class was served as representing a compilation unit, e.g. a source or header 
 * file in c/c++. and implementing a overall container for the Module(high-level IR)
 * </p>
 * <p>
 * There are multiple {@link GlobalVariable} and/or {@link Function} in this class, instead of
 * only a control flow graph corresponding to each method declared at the AST.
 * </p>
 * <p>
 * Further, a sorts of basic block has filled into CFG in the execution
 * order of program. At the any basic block, a large amount of quads are ordered 
 * in execution order.
 * </p>
 * @author Xlous.zeng
 */
public final class Module implements Iterable<Function>
{
	private static final Context.Key HIRKey = new Context.Key();
	/**
	 * A list of global variables.
	 */
	private ArrayList<GlobalVariable> globalVariableList;
	/**
	 * A sorts of function declaration.
	 */
	private ArrayList<Function> functionList;

    /**
     * Human readable unique identifier for this module.
     */
    private String moduleID;
	
	/**
	 * An singleton method for instantiating an instance of this class.
	 * @param context	An context environment.
	 * @param vars	GlobalVariable declarations list.
	 * @param functions	Function declarations list
	 * @return	The instance of {@link Module}
	 */
	public static Module instance(Context context, List<GlobalVariable> vars,
			List<Function> functions)
	{
		Module instance = (Module)context.get(HIRKey);
		if (instance == null)
		{
			instance = new Module(vars, functions);
			context.put(HIRKey, instance);
		}
		return instance;
	}
	
	/**
	 * Constructor.
	 * @param globalVariableList
	 * @param functions
	 */
	private Module(ArrayList<GlobalVariable> globalVariableList, ArrayList<Function> functions)
	{
		this.globalVariableList = globalVariableList;
		this.functionList = functions;
	}

	public Module(String moduleID)
    {
        this.moduleID = moduleID;
        globalVariableList = new ArrayList<>(32);
        functionList = new ArrayList<>(32);
    }

    public String getModuleIdentifier() { return moduleID;}

    public ArrayList<Function> getFunctionList() { return functionList;}

	public Iterator<Function> iterator()
	{
		return functionList.iterator();
	}
}
