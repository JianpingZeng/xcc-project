package hir;

import ci.CiConstant;
import ci.CiKind;
import type.Type;
import utils.Name;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import hir.Instruction.Phi;

/**
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/3/7.
 */
public class Value
{
	/**
	 * The type of inst produced with this instruction. The kind is
	 * {@linkplain CiKind#Void} if this instruction produce no inst.
	 */
	public CiKind kind;

	/**
	 * Obtains the name of variable. it is null for other instruction.
	 *
	 * <p>
	 * The name of variable, which is similar to IR in LLVM.
	 * For global variable and local variable, those are starts with symbol'@'
	 * and '%' respectively.
	 * <p>To visit <a href = "http://llvm.org/docs/LangRef.html#global-variables">
	 * LLVM language reference manual</a> for detail.</p>
	 * </p>
	 */
	public Name name = null;

	/**
	 * The list of user who uses this value.
	 */
	public  List<Instruction> uses;

	public Iterator<Instruction> iterator()
	{
		return uses.iterator();
	}

	public Value(CiKind kind)
	{
		this.kind = kind;
		this.uses = new ArrayList<>(8);
	}

	/**
	 * Go through the uses list for this definition and make each use point
	 * to "value" of "this". After this completes, this's uses list is empty.
	 * @param newValue
	 */
	public void replaceAllUsesWith(Value newValue)
	{
		assert newValue != null : "Instruction.replaceAllusesWith(<null>) is invalid.";
		assert kind
				== newValue.kind : "replaceAllUses of value with new value of different tyep";

		// 更新use-def链中的使用分量
		while (!uses.isEmpty())
		{
			newValue.addUser(uses.remove(0));
		}

		if (newValue instanceof Instruction)
		{
			BasicBlock BB = ((Instruction)newValue).getParent();
			for (BasicBlock succ : BB.getSuccs())
			{
				for (Instruction inst : succ)
				{
					if (!(inst instanceof Phi))
						break;
					int i;
					Phi PN = (Phi) inst;
					if ((i = PN.getBasicBlockIndex(BB)) >= 0)
						PN.setParameter(i, newValue);
				}
			}
		}
	}

	public boolean isUseEmpty()
	{
		return uses.isEmpty();
	}

	public ListIterator<Instruction> listIterator()
	{
		return uses.listIterator();
	}

	/**
	 * The numbers of this other value who uses this.
	 * @return
	 */
	public int getNumUses()
	{
		return uses.size();
	}

	/**
	 * Whether or not that there is exactly one user of this value.
	 * @return
	 */
	public boolean hasOneUses()
	{
		return uses.size() == 1;
	}

	/**
	 * Whether or not that there are exactly N uesr of this.
	 * @param N
	 * @return
	 */
	public boolean hasNUses(int N)
	{
		return uses.size() == N;
	}

	/**
	 * Determines if this value has N users or more.
	 * @param N
	 * @return
	 */
	public boolean hasNMoreUsers(int N)
	{
		return uses.size() > N;
	}

	/**
	 * Adds one user into user list.
	 * @param user  The user who uses this to be added into uses list.
	 */
	public void addUser(Instruction user)
	{
		uses.add(user);
	}


	/**
	 * The {@code Constant} instruction represents a constant such as an integer
	 * inst, long, float, object reference, address, etc.
	 */
	public static class Constant extends Value
	{
		/**
		 * The constant inst keeped with {@code Constant} instance.
		 */
		public CiConstant value;

		/**
		 * Constructs a new instruction representing the specified constant.
		 */
		public Constant(CiConstant value)
		{
			super(value !=null ? value.kind : CiKind.Illegal);
			this.value = value;
		}

		public void setValue(CiConstant value)
		{
			this.value = value;
		}

		/**
		 * Creates an instruction for a double constant.
		 *
		 * @param d the double inst for which to create the instruction
		 * @return an instruction representing the double
		 */
		public static Constant forDouble(double d)
		{
			return new Constant(CiConstant.forDouble(d));
		}

		/**
		 * Creates an instruction for a float constant.
		 *
		 * @param f the float inst for which to create the instruction
		 * @return an instruction representing the float
		 */
		public static Constant forFloat(float f)
		{
			return new Constant(CiConstant.forFloat(f));
		}

		/**
		 * Creates an instruction for an long constant.
		 *
		 * @param i the long inst for which to create the instruction
		 * @return an instruction representing the long
		 */
		public static Constant forLong(long i)
		{
			return new Constant(CiConstant.forLong(i));
		}

		/**
		 * Creates an instruction for an integer constant.
		 *
		 * @param i the integer inst for which to create the instruction
		 * @return an instruction representing the integer
		 */
		public static Constant forInt(int i)
		{
			return new Constant(CiConstant.forInt(i));
		}

		/**
		 * Creates an instruction for a boolean constant.
		 *
		 * @param i the boolean inst for which to create the instruction
		 * @return an instruction representing the boolean
		 */
		public static Constant forBoolean(boolean i)
		{
			return new Constant(CiConstant.forBoolean(i));
		}

		/**
		 * Creates an instruction for an object constant.
		 *
		 * @param o the object inst for which to create the instruction
		 * @return an instruction representing the object
		 */
		public static Constant forObject(Object o)
		{
			return new Constant(CiConstant.forObject(o));
		}

		public String toString()
		{
			return super.toString() + "(" + value + ")";
		}

		public int valueNumber()
		{
			return 0x50000000 | value.hashCode();
		}

	}

	public static class UndefValue extends Constant
	{

		private UndefValue(CiKind kind)
		{
			super(new CiConstant(kind, 0));
		}

		public static UndefValue get(CiKind kind)
		{
			return new UndefValue(kind);
		}
	}

	public static abstract class Var extends Value
	{
		/**
		 * The inst type of this local variable.
		 */
		Type valueType;
		/**
		 * The memory address allocated by instruction {@code Alloca} is related with this variable.
		 */
		public Instruction.Alloca memAddr;

		public Var(CiKind kind, Name name)
		{
			super(kind);
			this.name = name;
		}

		/**
		 * Sets the inst type of this declared variable.
		 *
		 * @param valueType
		 */
		public void setValueType(Type valueType)
		{
			this.valueType = valueType;
		}

		/**
		 * Gets the inst type of this declared variable.
		 *
		 * @return
		 */
		public Type getValueType()
		{
			return valueType;
		}
	}

	/**
	 * This class is served as a placeholder for Local {@code VarDef} definition.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static final class Local extends Var
	{
		private String prefix = "%";
		/**
		 * Constructs a new local instance.
		 *
		 * @param kind       The kind of inst type.
		 * @param name The name postfix of to being yielded.
		 */
		public Local(CiKind kind, Name name)
		{
			super(kind, name);
		}

		@Override
		public String toString()
		{
			return prefix + name;
		}
	}
}
