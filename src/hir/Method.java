package hir; 

import ast.Tree.MethodDef;
import exception.SemanticError;
import type.ArrayType;
import type.Type;
import ast.Tree;
import type.TypeTags;
import java.util.Iterator;
import java.util.List;

/**
 * This class is representation at the Module(high-level IR) of a function or method.
 * @author Xlous.zeng  
 * @version 2016年2月2日 下午9:10:07 
 */
public class Method implements Iterable<BasicBlock>
{
	private Signature sign;
	/**
	 * An control flow graph corresponding to method compound 
	 * of this method declaration. 
	 */
	public ControlFlowGraph cfg;
	/** 
	 * Keeps with all property including return type, method name 
	 * and  parameter list. 
	 */
	public MethodDef m;

	/**
	 * For the function return value, it is null iff there is no return value
	 * of this function.
	 */
	public Instruction.Alloca ReturnValue;
	
	public Method(MethodDef m)
    {
	    super();
		this.m = m;
	    // resolve return type
	    Type ret = resolveType(m.rettype);
		String name = m.name.toString();

	    // resolve formal parameter list.
        Type[] args = new Type[m.params.size()];
	    for (int idx = 0; idx < m.params.size(); idx++)
	    {
		    args[idx] = resolveType(m.params.get(idx));
	    }
		this.sign = new Signature(ret, name, args);
    }

	/**
	 * Resovles type from specified abstract syntax tree.
	 * @param ty    Tree.
	 * @return  Type.
	 */
	private Type resolveType(Tree ty)
	{
		if (ty instanceof  Tree.TypeIdent)
		{
			return resolveBasicType(ty);
		}
		if (ty instanceof Tree.TypeArray)
		{
			return resolveArrayType(ty);
		}
		throw new SemanticError("can not convert any Tree into Type.");
	}

	/**
	 * Resovles array type from specified abstract syntax tree.
	 * @param ty    Tree.
	 * @return  Array type.
	 */
	private Type resolveArrayType(Tree ty)
	{
		if (ty instanceof  Tree.TypeArray)
		{
			Tree.TypeArray tmp = (Tree.TypeArray)ty;
			Type elem = resolveType(tmp.elemtype);
			return new ArrayType(elem, null);
		}
		else
			return resolveBasicType(ty);
	}
	/**
	 * Resovles basic type from specified abstract syntax tree.
	 * @param ty    Tree.
	 * @return  Basic type.
	 */
	private Type resolveBasicType(Tree ty)
	{
		switch (((Tree.TypeIdent)ty).typetag)
		{
			case TypeTags.BOOL:
				return Type.DOUBLEType;
			case TypeTags.CHAR:
				return  Type.CHARType;
			case TypeTags.BYTE:
				return Type.BYTEType;
			case TypeTags.SHORT:
				return Type.SHORTType;
			case TypeTags.INT:
				return Type.INTType;
			case TypeTags.LONG:
				return  Type.LONGType;
			case TypeTags.FLOAT:
				return Type.FLOATType;
			case TypeTags.DOUBLE:
				return Type.DOUBLEType;
			default:
				return null;
		}
	}
	/**
	 * Get the name of the function as a string.
	 * @return  The name of the function.
	 */
	public String name()
	{
		return this.m.name.toString();
	}
	/**
	 * Obtains the signature {@code Signature} of this method object.
	 * @return  The signature.
	 */
	public Signature signature() {return sign;}

	/**
	 * Gets the entry block of the CFG of this function.
	 */
	public BasicBlock getEntryBlock()
	{
		return this.cfg.entry();
	}

	/**
	 * Gets the entry block of the CFG of this function.
	 */
	public BasicBlock getExitBlock()
	{
		return this.cfg.exit();
	}

	/**
	 * Returns an iterator over elements of type {@code T}.
	 *
	 * @return an Iterator.
	 */
	@Override
	public Iterator<BasicBlock> iterator()
	{
		return this.cfg.reversePostOrder().iterator();
	}

	/**
	 * Returns the linear scanning order of basic block at the CFG of function.
	 *
	 * Have not finished up to the date.
	 * @return
	 */
	public List<BasicBlock> linearScanOrder()
	{
		DominatorTree DT = new DominatorTree(false, this);
		DT.recalculate();
		return cfg.linearScanOrder(DT);
	}

	public int numLoops()
	{
		return cfg.stats.loopCount;
	}
}
