package backend.hir;

import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;

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

    private String dataLayout;

    private String targetTriple;

	private HashMap<String, Type> typeSymbolTable;

	/**
	 * An singleton method for instantiating an instance of this class.
	 * @param context	An context environment.
	 * @param vars	GlobalVariable declarations list.
	 * @param functions	FunctionProto declarations list
	 * @return	The instance of {@link Module}
	 */
	public static Module instance(List<GlobalVariable> vars,
			List<Function> functions)
	{
		Module instance = null;
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
	 * Return the first global value in the module with the specified getIdentifier, of
	 * arbitrary type.  This method returns null if a global with the specified
	 * getIdentifier is not found.
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
			// Clear the function's getIdentifier.
			f.setName("");

			// Retry, now there won't be a conflict.
			Function newF = (Function) getOrInsertFunction(name, type);
			f.setName(name);
			return newF;
		}

		// If the function exists but has the wrong type, return a bitcast to the
		// right type.
		if (f.getType() != PointerType.get(type, 0))
			return null;

		// Otherwise, we just found the existing function.
		return f;
	}

	public void setTargetTriple(String targetTriple)
	{
		this.targetTriple = targetTriple;
	}

	public void setDataLayout(String dataLayout)
	{
		this.dataLayout = dataLayout;
	}

	public String getTargetTriple()
	{
		return targetTriple;
	}

	public String getDataLayout()
	{
		return dataLayout;
	}

	/**
	 * Insert an entry in the symbol table mapping from string to Type.  If there
	 * is already an entry for this type, true is returned and the symbol table is
	 * not modified.
	 *
	 * @param name
	 * @param type
	 * @return
	 */
	public boolean addTypeName(String name, Type type)
	{
		HashMap<String, Type> st = getTypeSymbolTable();
		if (st.containsKey(name)) return true;

		st.put(name, type);
		return false;
	}

	public HashMap<String,Type> getTypeSymbolTable()
	{
		if (typeSymbolTable == null)
			typeSymbolTable = new HashMap<>();

		return typeSymbolTable;
	}
}
