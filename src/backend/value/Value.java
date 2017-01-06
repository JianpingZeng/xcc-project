package backend.value;

import backend.hir.BasicBlock;
import backend.hir.InstVisitor;
import backend.hir.SuccIterator;
import backend.type.Type;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author xlous.zeng
 * @version 0.1
 */
public class Value implements Cloneable
{
	/**
	 * Obtains the getName of variable. it is null for other instruction.
	 *
	 * <p>
	 * The getName of variable, which is similar to IR in LLVM.
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

	public List<Use> getUseList() {return usesList;}
	
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
			newValue.addUse(usesList.remove(0));
		}

		if (this instanceof Instruction)
		{
			BasicBlock BB = ((Instruction)this).getParent();
            SuccIterator itr = BB.succIterator();
			while (itr.hasNext())
			{
			    BasicBlock succ = itr.next();
				for (Value inst : succ)
				{
					if (!(inst instanceof Instruction.PhiNode))
						break;
					int i;
					Instruction.PhiNode PN = (Instruction.PhiNode) inst;
					if ((i = PN.getBasicBlockIndex(BB)) >= 0)
						PN.setIncomingValue(i, newValue);
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

	public void accept(InstVisitor visitor){}

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
	    return new Value(ty, subclassID);
	}

	public void setName(String newName)
	{
		name = newName;
	}

	public String getName(){return name;}

	public boolean hasName() {return name != null && !name.isEmpty();}
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

            return undefValueConstants.put(ty, new UndefValue(ty));
		}

		public UndefValue clone() {return new UndefValue(getType());}

		public void accept(InstVisitor visitor){}

		@Override
		public boolean isNullValue()
		{
			return false;
		}
	}

	public int valueNumber() {return 0;}
}
