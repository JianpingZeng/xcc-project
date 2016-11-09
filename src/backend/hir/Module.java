package backend.hir;

import backend.type.FunctionType;
import backend.type.PointerType;
import backend.value.*;
import tools.Context;

import java.util.*;

import static backend.value.GlobalValue.LinkageType.ExternalLinkage;

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
	 * Symbol table for values.
	 */
	private HashMap<String, Value> valSymTable;

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
			instance = null;// = new Module(vars, functions);
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
		valSymTable = new HashMap<>();
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

	public ArrayList<GlobalVariable> getGlobalVariableList()
	{
		return globalVariableList;
	}

	/**
	 * Return the first global value in the module with the specified name, of
	 * arbitrary type.  This method returns null if a global with the specified
	 * name is not found.
	 * @param name
	 * @return
	 */
	private GlobalValue getNameOfValue(String name)
	{
		Value val = valSymTable.get(name);
		if (val instanceof GlobalValue)
			return (GlobalValue)val;
		return null;
	}

	/**
	 * Look up the specified function in the module symbol table.
	 * If it does not exist, add a prototype for the function and return it.
	 * @param name
	 * @param type
	 * @return
	 */
	public Constant getOrInsertFunction(String name, FunctionType type)
	{
		GlobalValue f = getNameOfValue(name);
		if (f == null)
		{
			// not found ,add it into valSymTable.
			Function newFunc = new Function(type, ExternalLinkage, name, this);
			// add it into functions list.
			functionList.add(newFunc);
			return newFunc;
		}

		// Okay, the found function is exist. Does it has external linkage?
		if (f.hasLocalLinkage())
		{
			// Clear the function's name.
			f.setName("");

			// Retry, now there won't be a conflict.
			Function newF = (Function) getOrInsertFunction(name, type);
			f.setName(name);
			return newF;
		}

		// If the function exists but has the wrong type, return a bitcast to the
		// right type.
		if (f.getType() != PointerType.get(type))
			return null;

		// Otherwise, we just found the existing function.
		return f;
	}
}
