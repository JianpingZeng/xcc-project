package backend.value;

import backend.hir.BasicBlock;
import backend.hir.InstructionVisitor;
import backend.hir.SuccIterator;
import backend.lir.ci.LIRConstant;
import backend.lir.ci.LIRKind;
import backend.lir.ci.LIRValue;
import backend.type.Type;

import java.util.*;

/**
 * @author xlous.zeng
 * @version 0.1
 */
public class Value implements Cloneable
{
	/**
	 * The frontend.type of inst produced with this instruction. The kind is
	 * {@linkplain LIRKind#Void} if this instruction produce no inst.
	 */
	public LIRKind kind;

	/**
	 * Machine specific.
	 */
	public LIRValue LIROperand;

	/**
	 * Obtains the name of variable. it is null for other instruction.
	 *
	 * <p>
	 * The name of variable, which is similar to IR in LLVM.
	 * For global variable and local variable, those are starts with frontend.symbol'@'
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
	
	/**
	 * For value number to determine whether this instruction is equivalent to
	 * that value.
	 *
	 * @param value Targeted instruction to be checked.
	 * @return return false by default.
	 */
	public boolean valueEqual(Value value)
	{
		return this.kind == value.kind && LIROperand.equals(value.LIROperand);
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
		assert kind == newValue.kind
                : "replaceAllUses of value with new value of different frontend.type";

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

	public void accept(InstructionVisitor visitor)
	{
		visitor.visitValue(this);
	}

	public boolean isConstant()
	{
		return this instanceof Constant;
	}

	/**
	 * Converts the instance of this class to a constant if this class
	 * is the subclass of {@code Constant}, otherwise, the null is returned.
	 * @return
	 */
	public LIRConstant asLIRConstant()
	{
		if (this instanceof Constant)
			return ((Constant)this).value;
		else
			return null;
	}
	
	public Constant asConstant()
	{
		if (this instanceof Constant)
			return ((Constant)this);
		else
			return null;
	}

	public void setLIROperand(LIRValue LIROperand)
	{
		assert this.LIROperand.isIllegal() :
				"LIROperand can not be setted twice";
		assert LIROperand != null && LIROperand.isLegal() :
				"LIROperand must be legal";
		assert LIROperand.kind != this.kind;
		this.LIROperand = LIROperand;
	}

	/**
	 * Obtains the corresponding machine-specific operation getReturnValue of this instruction.
	 * @return
	 */
	public LIRValue LIROperand()
	{
		return LIROperand;
	}

	public void clearLIROperand()
	{
		this.LIROperand = LIRValue.IllegalValue;
	}

	public final boolean isNullConstant()
	{
		return this instanceof Constant && ((Constant) this).value.isNull();
	}
	
	@Override
	public Value clone()
	{
	    return new Value(ty, subclassID);
	}

	/**
	 * The {@code Constant} instruction represents a constant such as an integer
	 * inst, long, float, object reference, address, etc.
	 */
	public static class Constant extends User
	{
		public static Constant CONSTANT_INT_0 = Constant.forInt(0);
		public static Constant CONSTANT_INT_1 = Constant.forInt(1);
		public static Constant CONSTANT_INT_MINUS_1 = Constant.forInt(-1);
		
		/**
		 * The constant inst keeped with {@code Constant} instance.
		 */
		public LIRConstant value;

		/**
		 * Constructs a new instruction representing the specified constant.
		 */
		public Constant(Type ty, int valueKind)
        {
            super(ty, valueKind);
        }

		public void setValue(LIRConstant value)
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
			return new Constant(LIRConstant.forDouble(d));
		}

		/**
		 * Creates an instruction for a float constant.
		 *
		 * @param f the float inst for which to create the instruction
		 * @return an instruction representing the float
		 */
		public static Constant forFloat(float f)
		{
			return new Constant(LIRConstant.forFloat(f));
		}

		/**
		 * Creates an instruction for an long constant.
		 *
		 * @param i the long inst for which to create the instruction
		 * @return an instruction representing the long
		 */
		public static Constant forLong(long i)
		{
			return new Constant(LIRConstant.forLong(i));
		}

		/**
		 * Creates an instruction for an integer constant.
		 *
		 * @param i the integer inst for which to create the instruction
		 * @return an instruction representing the integer
		 */
		public static Constant forInt(int i)
		{
			return new Constant(LIRConstant.forInt(i));
		}

		/**
		 * Creates an instruction for a boolean constant.
		 *
		 * @param i the boolean inst for which to create the instruction
		 * @return an instruction representing the boolean
		 */
		public static Constant forBoolean(boolean i)
		{
			return new Constant(LIRConstant.forBoolean(i));
		}

		/**
		 * Creates an instruction for an object constant.
		 *
		 * @param o the object inst for which to create the instruction
		 * @return an instruction representing the object
		 */
		public static Constant forObject(Object o)
		{
			return new Constant(LIRConstant.forObject(o));
		}

		public String toString()
		{
			return super.toString() + "(" + value + ")";
		}

		public int valueNumber()
		{
			return 0x50000000 | value.hashCode();
		}

		public Constant clone()
		{
			return new Constant(this.value);
		}

		public void accept(InstructionVisitor visitor)
		{
			visitor.visitConstant(this);
		}
		
		@Override
		public boolean equals(Object other)
		{
			if (other == null) return false;
			if (other == this) return true;
			if (!(other instanceof Constant))
				return false;
			Constant c = (Constant)other;
			return c.value.equals(c.value);
		}
		/**
		 * ReturnInst the production getReturnValue of both Constant, c1 and c2.
		 * @param c1
		 * @param c2
		 */
		public static Constant multiple(Constant c1, Constant c2)
		{
			assert c1.kind.isPrimitive() && c2.kind.isPrimitive()
				:"No non-primitive frontend.type allowed for induction variable";
			long l1 = c1.value.asPrimitive();
			long l2 = c2.value.asPrimitive();
			
			return Constant.forLong(l1 * l2);
		}
		/**
		 * ReturnInst the sum of both Constant, c1 and c2.
		 * @param c1
		 * @param c2
		 */
		public static Constant add(Constant c1, Constant c2)
		{
			assert c1.kind.isPrimitive() && c2.kind.isPrimitive()
			:"No non-primitive frontend.type allowed for induction variable";
			long l1 = c1.value.asPrimitive();
			long l2 = c2.value.asPrimitive();
		
			return Constant.forLong(l1 + l2);
		}
		
		public static Constant sub(Constant c1, Constant c2)
		{
			assert c1.kind.isPrimitive() && c2.kind.isPrimitive()
			:"No non-primitive frontend.type allowed for induction variable";
			long l1 = c1.value.asPrimitive();
			long l2 = c2.value.asPrimitive();
		
			return Constant.forLong(l1 - l2);
		}
		
		public static Constant sub(int c1, Constant c2)
		{
			assert c2.kind.isPrimitive()
			:"No non-primitive frontend.type allowed for induction variable";
			long l2 = c2.value.asPrimitive();
		
			return Constant.forLong(c1 - l2);
		}

        public static Value getNullValue(Type type)
        {
            switch (type.getTypeClass())
            {
                case TypeClass.Bool:
                    return ConstantBool.False;
                case TypeClass.Char:
                    return ConstantInt.ConstantSInt.get(Type.CharTy, 0);
                case TypeClass.UnsignedChar:
                    return ConstantInt.ConstantUInt.get(Type.UnsignedCharTy, 0);
                case TypeClass.Short:
                    return ConstantInt.ConstantSInt.get(Type.ShortTy, 0);
                case TypeClass.UnsignedShort:
                    return ConstantInt.ConstantUInt.get(Type.UnsignedShortTy, 0);
                case TypeClass.Int:
                    return ConstantInt.ConstantSInt.get(Type.IntTy, 0);
                case TypeClass.UnsignedInt:
                    return ConstantInt.ConstantUInt.get(Type.UnsignedIntTy, 0);
                case TypeClass.LongInteger:
                    return ConstantInt.ConstantSInt.get(Type.LongTy, 0);
                case TypeClass.UnsignedLong:
                    return ConstantInt.ConstantUInt.get(Type.UnsignedLongTy, 0);
                case TypeClass.Real:
                    // TODO
                    return null;
            }
            return null;
        }
    }

	public static class UndefValue extends Constant
	{

		private UndefValue(LIRKind kind)
		{
			super(new LIRConstant(kind, 0));
		}

		public static UndefValue get(LIRKind kind)
		{
			return new UndefValue(kind);
		}

		public UndefValue clone()
		{
			return new UndefValue(this.kind);
		}

		public void accept(InstructionVisitor visitor)
		{
			visitor.visitUndef(this);
		}
	}

	public int valueNumber()
    {
	    return 0;
    }
}
