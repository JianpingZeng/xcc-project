package backend.value;

import tools.Util;
import backend.support.AssemblyWriter;
import backend.support.AttrList;
import tools.FormattedOutputStream;
import backend.support.SlotTracker;
import backend.support.ValueSymbolTable;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
 * @author Jianping Zeng
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
	private ValueSymbolTable valSymTable;

	/**
	 * Symbol table for types.
	 */
	private HashMap<String, Type> typeSymbolTable;

    /**
     * Human readable unique identifier for this module.
     */
    private String moduleID;

    private String dataLayout;

    private String targetTriple;

	/**
	 * Constructor.
	 * @param globalVariableList
	 * @param functions
	 */
	private Module(ArrayList<GlobalVariable> globalVariableList, ArrayList<Function> functions)
	{
		this.globalVariableList = globalVariableList;
		this.functionList = functions;
		valSymTable = new ValueSymbolTable();
		typeSymbolTable = new HashMap<>();
	}

	public Module(String moduleID)
    {
        this.moduleID = moduleID;
        globalVariableList = new ArrayList<>(32);
        functionList = new ArrayList<>(32);
	    valSymTable = new ValueSymbolTable();
	    typeSymbolTable = new HashMap<>();
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
	private GlobalValue getValueByName(String name)
	{
		Value val = valSymTable.getValue(name);
		if (val instanceof GlobalValue)
			return (GlobalValue)val;
		return null;
	}
	public Constant getOrInsertFunction(String name,
										FunctionType type)
	{
		AttrList attrs = new AttrList(new ArrayList<>());
		return getOrInsertFunction(name, type, attrs);
	}

	/**
	 * Look up the specified function in the module symbol table.
	 * If it does not exist, add a prototype for the function and return it.
	 * @param name
	 * @param type
	 * @return
	 */
	public Constant getOrInsertFunction(String name,
										FunctionType type,
										AttrList attrs)
	{
		GlobalValue f = getValueByName(name);
		if (f == null)
		{
			// not found ,add it into valSymTable.
			Function newFunc = new Function(type, ExternalLinkage, name, this);
			if (!newFunc.isIntrinsicID())
				newFunc.setAttributes(attrs);
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

	public Type getTypeByName(String name)
	{
		if (typeSymbolTable.containsKey(name))
			return typeSymbolTable.get(name);
		return null;
	}

	public void print(FormattedOutputStream os) throws IOException
	{
        new AssemblyWriter(os, this, new SlotTracker(this)).write(this);
	}

	public Function getFunction(String funcName)
	{
		GlobalValue gv = getValueByName(funcName);
		return gv instanceof Function ? (Function)gv : null;
	}

	public ValueSymbolTable getValueSymbolTable()
	{
		return valSymTable;
	}

	public GlobalVariable getGlobalVariable(String name, boolean allowLocal)
	{
		GlobalValue gv = getValueByName(name);
		if (gv instanceof GlobalVariable)
		{
			GlobalVariable gvv = (GlobalVariable)gv;
			if (allowLocal || !gvv.hasLocalLinkage())
				return gvv;
		}

		return null;
	}

    public void addFunction(Function fn)
    {
        Util.assertion( fn != null && !functionList.contains(fn)		        && fn.getName() != null);

        functionList.add(fn);
        fn.setParent(this);
        valSymTable.createValueName(fn.getName(), fn);
    }

    public void addGlobalVariable(GlobalVariable gv)
    {
        Util.assertion( gv != null && !globalVariableList.contains(gv)                && gv.getName() != null && !gv.getName().isEmpty());

        globalVariableList.add(gv);
        valSymTable.createValueName(gv.getName(), gv);
        gv.setParent(this);
    }
}
