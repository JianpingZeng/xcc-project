package backend.value;

import backend.hir.*;
import backend.opt.Loop;
import backend.type.FunctionType;
import backend.type.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is representation at the Module(high-level IR) of a function or method.
 * @author Xlous.zeng  
 * @version 0.1
 */
public class Function extends GlobalValue implements Iterable<BasicBlock>
{
	private Loop[] loops;
	
	private Signature sign;
	/**
	 * An control flow graph corresponding to method compound 
	 * of this method declaration. 
	 */
	public ControlFlowGraph cfg;

	/**
	 * For the function return value, it is null iff there is no return value
	 * of this function.
	 */
	public Instruction.AllocaInst ReturnValue;

    private ArrayList<Argument> argumentList;

    private LinkedList<BasicBlock> basicBlockList;
	
	public Function(FunctionType ty, String name, Module parentModule)
    {
	    super(ty, ValueKind.FunctionVal, name);
		if (parentModule != null)
			parentModule.getFunctionList().add(this);

        for (int i = 0, e = ty.getNumParams(); i< e; i++)
        {
            Type t = ty.getParamType(i).backendType;
            assert !t.isVoidType():"Cann't have void typed argument!";
            argumentList.add(new Argument(t));
        }
        basicBlockList = new LinkedList<>();
    }

    public FunctionType getType() { return (FunctionType)super.getType();}

    public FunctionType getFunctionType() { return getType(); }

    public boolean isVarArg()
    {
        return getFunctionType().isVarArgs();
    }

    public Type getResultType()
    {
        return getFunctionType().getResultType().backendType;
    }

    public Module getParent() { return parent; }

    public void removeFromParent()
    {
        parent.getFunctionList().remove(this);
    }

    public ArrayList<Argument> getArgumentList()
    {
        return argumentList;
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
	 * Get the name of the function as a string.
	 * @return  The name of the function.
	 */
	public String name()
	{
		return name;
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
	 * Returns an iterator over elements of frontend.type {@code T}.
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

    public LinkedList<BasicBlock> getBasicBlockList() { return basicBlockList;}

    public boolean empty()
    {
        return basicBlockList.isEmpty();
    }

    public int getNumOfArgs()
    {
        return argumentList.size();
    }
}
