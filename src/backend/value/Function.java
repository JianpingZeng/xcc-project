package backend.value;

import backend.analysis.DominatorTree;
import backend.codegen.MachineFunction;
import backend.hir.*;
import backend.analysis.Loop;
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

	private MachineFunction mf;
	
	public Function(FunctionType ty, LinkageType linkage, String name, Module parentModule)
    {
	    super(ty, ValueKind.FunctionVal, linkage, name);
		if (parentModule != null)
			parentModule.getFunctionList().add(this);

        for (int i = 0, e = ty.getNumParams(); i< e; i++)
        {
            Type t = ty.getParamType(i);
            assert !t.isVoidType():"Cann't have void typed argument!";
            argumentList.add(new Argument(t));
        }
        basicBlockList = new LinkedList<>();
    }

	public Type getReturnType()
	{
		return getFunctionType().getReturnType();
	}

    public FunctionType getFunctionType() { return (FunctionType) super.getType().getElemType(); }

    public boolean isVarArg()
    {
        return getFunctionType().isVarArgs();
    }

	/**
	 * This method unlinks 'this' from the containing module
	 * and deletes it.
	 */
	@Override
	public void eraseFromParent()
	{
		getParent().getFunctionList().remove(this);
	}

	public Module getParent() { return parent; }

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
	 * Gets the entry block of the CFG of this function.
	 */
	public BasicBlock getEntryBlock()
	{
		return basicBlockList.getFirst();
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

	@Override
	public boolean isNullValue()
	{
		return false;
	}

	/**
	 * Return true if the primary definition of this global value is
	 * outside of the current translation unit.
	 *
	 * @return
	 */
	@Override
	public boolean isExternal()
	{
		return basicBlockList.isEmpty();
	}

	public MachineFunction getMachineFunc() {return mf;}

	public void setMachineFunc(MachineFunction newFunc) {mf = newFunc;}

	public int getIntrinsicID()
	{
		// TODO
		return 0;
	}
}
