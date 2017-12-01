package backend.value;

import backend.support.FormattedOutputStream;
import backend.support.LLVMContext;
import backend.support.ValueSymbolTable;
import backend.type.Type;
import backend.utils.InstVisitor;
import backend.utils.SuccIterator;
import backend.value.Instruction.PhiNode;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author xlous.zeng
 * @version 0.1
 */
public class Value implements Cloneable
{
	/**
	 * Obtains the getIdentifier of variable. it is null for other instruction.
	 *
	 * <p>
	 * The name of variable, which is similar to IR in LLVM.
	 * For global variable and local variable, those are starts with jlang.symbol'@'
	 * and '%' respectively.
	 * <p>To visit <a href = "http://llvm.org/docs/LangRef.html#global-variables">
	 * LLVM language reference manual</a> for detail.</p>
	 * </p>
	 */
	public String name;

    private int subclassID;

	/**
	 * The list of user who usesList this value.
	 */
	public final LinkedList<Use> usesList;

    private Type ty;

	public Value(Type ty, int valueType)
	{
		this.ty = ty;
        subclassID = valueType;
		this.usesList = new LinkedList<>();
	}

	public Type getType() { return ty;}

	public LinkedList<Use> getUseList() {return usesList;}
	
	/**
	 * For value number to determine whether this instruction is equivalent to
	 * that value.
	 *
	 * @param value Targeted instruction to be checked.
	 * @return return false by default.
	 */
	public boolean valueEqual(Value value)
	{
		return false;
	}
	
	/**
	 * Go through the usesList list for this definition and make each use point
	 * to "value" of "this". After this completes, this's usesList list is empty.
	 * @param newValue
	 */
	public void replaceAllUsesWith(Value newValue)
	{
		assert newValue != null
				: "Instruction.replaceAllusesWith(<null>) is invalid.";
		assert getType() == newValue.getType()
                : "replaceAllUses of value with new value of different jlang.type";

		// replaces all old uses with new one.
		while (!usesList.isEmpty())
		{
			usesList.get(0).setValue(newValue);
		}

		if (this instanceof Instruction)
		{
			BasicBlock BB = ((Instruction)this).getParent();
            SuccIterator itr = BB.succIterator();
			while (itr.hasNext())
			{
			    BasicBlock succ = itr.next();
				for (int i = 0, e =  succ.getNumOfInsts(); i < e; i++)
				{
					Instruction inst = succ.getInstAt(i);
					if (!(inst instanceof PhiNode))
						break;

					int j;
					PhiNode PN = (PhiNode) inst;
					if ((j = PN.getBasicBlockIndex(BB)) >= 0)
						PN.setIncomingValue(j, newValue);
				}
			}
		}
	}

	public boolean isUseEmpty()
	{
		return usesList.isEmpty();
	}

	public Use useAt(int index)
    {
        assert(index >= 0 && index < usesList.size());
        return usesList.get(index);
    }

	/**
	 * The numbers of this other value who usesList this.
	 * @return
	 */
	public int getNumUses()
	{
		return usesList.size();
	}

	/**
	 * Whether or not that there is exactly one user of this value.
	 * @return
	 */
	public boolean hasOneUses()
	{
		return usesList.size() == 1;
	}

	/**
	 * Whether or not that there are exactly N uesr of this.
	 * @param N
	 * @return
	 */
	public boolean hasNUses(int N)
	{
		return usesList.size() == N;
	}

	/**
	 * Determines if this value has N users or more.
	 * @param N
	 * @return
	 */
	public boolean hasNMoreUsers(int N)
	{
		return usesList.size() > N;
	}

	/**
	 * Adds one use instance into use list that represents def-use chain
	 * between value definition and value use.
	 * @param use  The instance of use.
	 */
	public void addUse(Use use)
	{
		assert use != null : "the use chain must be no null";
		usesList.add(use);
	}

	/**
	 * Removes and unlink specified use chain from uses list.
	 * @param use   The use to be unlinked.
	 */
	public void killUse(Use use)
	{
		usesList.remove(use);
	}

	public boolean isConstant(){return this instanceof Constant;}
	
	public Constant asConstant()
	{
		if (this instanceof Constant)
			return ((Constant)this);
		else
			return null;
	}

	public final boolean isNullConstant()
	{
		return (this instanceof Constant) && ((Constant) this).isNullValue();
	}
	
	@Override
	public Value clone()
	{
	    Value res = new Value(ty, subclassID);
	    res.name = name;
	    return res;
	}

	public static ValueSymbolTable getSymTab(Value val)
    {
        if (val instanceof Instruction)
        {
            Instruction inst = (Instruction)val;
            BasicBlock bb = inst.getParent();
            if (bb != null && bb.getParent() != null)
            {
                return bb.getParent().getValueSymbolTable();
            }
        }
        else if (val instanceof BasicBlock)
        {
            BasicBlock bb = (BasicBlock)val;
            Function f = bb.getParent();
            if (f != null)
                return f.getValueSymbolTable();
        }
        else if (val instanceof GlobalValue)
        {
            GlobalValue gv = (GlobalValue)val;
            Module m = gv.getParent();
            if (m != null)
                return m.getValueSymbolTable();
        }
        else if (val instanceof Argument)
        {
            Argument a = (Argument)val;
            Function f = a.getParent();
            if (f != null)
                return f.getValueSymbolTable();
        }
        else
        {
            assert val instanceof Constant : "Unknown value type!";
        }
        return null;
    }

    /**
     * Update the name with newName. Occasionally, the newName would be changed
     * when the set newName is same as old name.
     * @param newName
     */
	public void setName(String newName)
	{
	    if (newName == null || newName.isEmpty())
	        return;
	    assert !getType().equals(LLVMContext.VoidTy):
                "Can not assign name to void values!";
	    // get the symbol table to update for this object.
        ValueSymbolTable vt = getSymTab(this);
        if (vt == null)
        {
            name = newName;
            return;
        }

		name = vt.createValueName(newName, this);
	}

	public String getName()
    {
        return name == null ?"": name;
    }

	public boolean hasName() {return name != null && !name.isEmpty();}

	public void print(FormattedOutputStream os)
    {
        os.printf("0x%x", hashCode());
    }

	public void print(PrintStream os)
	{
        try(FormattedOutputStream out = new FormattedOutputStream(os))
        {
            print(out);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}

	/**
	 * Support for debugging, callable in GDB: this.dump().
	 */
	public void dump()
	{
		print(System.err);
	}

    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj);
    }

    /**
     * 'undef' values are things that do not have specified contents.
     * These are used for a variety of purposes, including global variable
     * initializers and operands to instructions.
     */
	public static class UndefValue extends Constant
	{
	    private static HashMap<Type, UndefValue> undefValueConstants;
        static
        {
            undefValueConstants = new HashMap<>();
        }
		private UndefValue(Type t)
		{
			super(t, ValueKind.UndefValueVal);
		}

		/**
		 * A static factory method for obtaining a instance of typed specified
		 * @param ty
		 * @return
		 */
		public static UndefValue get(Type ty)
		{
		    UndefValue val = undefValueConstants.get(ty);
            if (val != null)
                return val;

            val = new UndefValue(ty);
            undefValueConstants.put(ty, val);
            return val;
		}

		public UndefValue clone()
		{
			return get(getType());
		}

		public void accept(InstVisitor visitor){}

		@Override
		public boolean isNullValue()
		{
			return false;
		}

		@Override
        public boolean equals(Object obj)
		{
		    if (obj == null)
		        return false;
		    if (this == obj)
		        return true;

		    if (getClass() != obj.getClass())
		        return false;

		    UndefValue o = (UndefValue)obj;
		    return o == this;
        }
	}

	public int valueNumber() {return 0;}
}
