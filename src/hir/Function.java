package hir; 

import ast.Tree.MethodDef;
import exception.SemanticError;
import lir.ci.LIRKind;
import type.ConstantArrayType;
import type.Type;
import ast.Tree;
import type.TypeClass;

import java.util.Iterator;
import java.util.List;

import opt.Loop;

/**
 * This class is representation at the Module(high-level IR) of a function or method.
 * @author Xlous.zeng  
 * @version 0.1
 */
public class Function extends Value implements Iterable<BasicBlock>
{
	private Loop[] loops;
	
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
	public Instruction.AllocaInst ReturnValue;
	
	public Function(MethodDef m)
    {
	    super(LIRKind.FunctionType);
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

	public void setLoops(Loop[] loops)
	{
		assert loops != null && loops.length > 0;
		this.loops = loops;
	}
	
	public Loop[] getLoops()
	{
		return loops;
	}
	
	/**
	 * Resolve type from specified abstract syntax tree.
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
			return new ConstantArrayType(elem, null);
		}
		else
			return resolveBasicType(ty);
	}
	/**
	 * Resolve basic type from specified abstract syntax tree.
	 * @param ty    Tree.
	 * @return  Basic type.
	 */
	private Type resolveBasicType(Tree ty)
	{
		switch (((Tree.TypeIdent)ty).typetag)
		{
			case TypeClass.BOOL:
				return Type.DOUBLEType;
			case TypeClass.Char:
				return  Type.CHARType;
			case TypeClass.BYTE:
				return Type.BYTEType;
			case TypeClass.Short:
				return Type.SHORTType;
			case TypeClass.Int:
				return Type.INTType;
			case TypeClass.LongInteger:
				return  Type.LONGType;
			case TypeClass.FLOAT:
				return Type.FLOATType;
			case TypeClass.DOUBLE:
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
