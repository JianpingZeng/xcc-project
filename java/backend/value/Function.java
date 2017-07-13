package backend.value;

import backend.codegen.MachineFunction;
import backend.type.FunctionType;
import backend.type.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class is representation at the Module(high-level IR) of a function or method.
 * @author Xlous.zeng  
 * @version 0.1
 */
public class Function extends GlobalValue implements Iterable<BasicBlock>
{
	/**
	 * For the function return value, it is null iff there is no return value
	 * of this function.
	 */
	public Instruction.AllocaInst ReturnValue;

    private ArrayList<Argument> argumentList;

    private LinkedList<BasicBlock> basicBlockList;

	private MachineFunction mf;
	
	public Function(FunctionType ty, LinkageType linkage,
            String name, Module parentModule)
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

    public FunctionType getFunctionType()
    {
        return (FunctionType) super.getType().getElemType();
    }

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

	/**
	 * Gets the entry block of the CFG of this function.
	 */
	public BasicBlock getEntryBlock()
	{
		return basicBlockList.getFirst();
	}

	/**
	 * Returns an iterator over elements of jlang.type {@code T}.
	 *
	 * @return an Iterator.
	 */
	@Override
	public Iterator<BasicBlock> iterator()
	{
		return basicBlockList.iterator();
	}

    public LinkedList<BasicBlock> getBasicBlockList()
    {
        return basicBlockList;
    }

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

	public boolean doesNotAccessMemory()
	{
		// TODO
		return false;
	}

	public boolean onlyReadsMemory()
	{
		// TODO
		return false;
	}

	public Argument argAt(int index)
	{
		assert index >= 0 && index < getNumOfArgs();
		return argumentList.get(index);
	}
}
