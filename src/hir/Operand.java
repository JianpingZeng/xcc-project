package hir;

import java.util.ArrayList;
import java.util.List;
import hir.RegisterFactory.Register;
import type.Type;


/**
 * This class that represents an operand in Quad, which are two different
 * variety of subclasses, including literal and variable.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2015年12月23日 上午9:10:45
 */
public abstract class Operand implements Cloneable, RHS
{
	/**
	 * The instruction that this operand attached to.
	 */
	protected Quad instruction;

	/**
	 * Gets the quad to which this operand attached.
	 * @return
	 */
	public abstract Quad getQuad();

	/**
	 * Attach a quad instruction to this operand.
	 * @param q
	 */
	public abstract void attachToQuad(Quad q);

	/**
	 * Evaluates whether given operand that is similar to this operand or not.
	 * @param that A given operand to be estimated.
	 * @return True return if those are similar to each other, otherwise return
	 *         false.
	 */
	public abstract boolean isSimilar(Operand that);

	@Override
	public abstract Operand clone() throws CloneNotSupportedException;

	/**
	 * Represents a virtual register operand. Jianping Zeng
	 * <z1215jping@hotmail.com>
	 */
	public static class RegisterOperand extends Operand implements LHS
	{
		private Register register;
		private Type type;

		public RegisterOperand(Register reg, Type type)
		{
			this.register = reg;
			this.type = type;
		}
		/** Gets the register name of this operand. */		
		public String getRegisterName() {return register.toString();}

		public Register getRegister()
		{
			return register;
		}

		public void setRegister(Register r)
		{
			this.register = r;
		}
		
		public Type getType()
		{
			return type;
		}

		public void setType(Type t)
		{
			this.type = t;
		}

		@Override
		public RegisterOperand clone() throws CloneNotSupportedException
		{
			RegisterOperand res = null;
			try
			{
				new RegisterOperand(this.register, this.type);
			}
			catch (Exception e)
			{
				throw new CloneNotSupportedException();
			}
			return res;
		}

		public void attachToQuad(Quad q)
		{
			assert (instruction == null);
			instruction = q;
		}

		public Quad getQuad()
		{
			return instruction;
		}

		public boolean isSimilar(Operand that)
		{
			return that instanceof RegisterOperand
			        && ((RegisterOperand) that).getRegister() == this
			                .getRegister();
		}

		public String toString()
		{
			return register + " " + ((type == null) ? "<g>" : type.toString());
		}
	}

	/**
	 * A abstract base class for constant operand. A sorts of subclasses is
	 * served as describing many variant of it with different storage size, like
	 * 4B integer and 8B long or double type.
	 * 
	 * Note that the constant operand only be served as right hand size of
	 * assignment.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static abstract class ConstOperand extends Operand
	{
		/**
		 * A abstract method for get the wrapper of this constant.
		 * @return
		 */
		public abstract Object getWrapped();
	}

	/**
	 * An concrete subclass of {@link ConstOperand} that represents an constant
	 * with size 4B.
	 * 
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static abstract class Const4Operand extends ConstOperand
	{
		/**
		 * Gets the bits of it's storage size.
		 * @return
		 */
		public abstract int getBits();
	}

	/**
	 * An concrete subclass of {@link ConstOperand} that represents an constant
	 * with size 8B.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static abstract class Const8Operand extends ConstOperand
	{
		/**
		 * Gets the bits of it's storage size.
		 * @return
		 */
		public abstract long getBits();
	}

	/**
	 * An concrete subclass of base {@link ConstOperand} just for describing
	 * integer constant. Note that all types including byte, char, short, and
	 * int, are converted into integer.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class IConstOperand extends Const4Operand
	{
		/**
		 * It indicates the value of this integer constant.
		 */
		int value;

		/**
		 * Constructor.
		 * @param value
		 */
		public IConstOperand(int value)
		{
			super();
			this.value = value;
		}

		@Override
		public Quad getQuad()
		{
			return instruction;
		}

		@Override
		public void attachToQuad(Quad q)
		{
			this.instruction = q;
		}

		@Override
		public boolean isSimilar(Operand that)
		{
			return (that instanceof IConstOperand && ((IConstOperand) that).value == this.value);
		}

		@Override
		public int getBits()
		{
			return value;
		}

		/**
		 * Obtains the constant value of this operand.
		 */
		public int getValue()
		{
			return this.value;
		}

		/**
		 * Modify it's constant value with argument v.
		 * @param v
		 */
		public void setValue(int v)
		{
			this.value = v;
		}

		@Override
		public Object getWrapped()
		{
			return new Integer(value);

		}

		@Override
		public Operand clone() throws CloneNotSupportedException
		{
			IConstOperand res = null;
			try
			{
				res = new IConstOperand(value);
			}
			catch (Exception e)
			{
				throw new CloneNotSupportedException();
			}
			return res;
		}

		@Override
        public Type getType()
        {
	        return Type.INTType;
        }
	}

	/**
	 * An concrete subclass of super class {@link Const4Operand} for describing
	 * float point id.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class FConstOperand extends Const4Operand
	{
		/**
		 * It indicates the value of this integer constant.
		 */
		private float value;

		public FConstOperand(float value)
		{
			super();
			this.value = value;
		}

		@Override
		public int getBits()
		{
			return Float.floatToRawIntBits(value);
		}

		@Override
		public Object getWrapped()
		{
			return new Float(value);
		}

		@Override
		public Quad getQuad()
		{
			return instruction;
		}

		@Override
		public void attachToQuad(Quad q)
		{
			instruction = q;
		}

		@Override
		public boolean isSimilar(Operand that)
		{
			return (that instanceof FConstOperand && ((FConstOperand) that).value == this.value);
		}

		@Override
		public Operand clone() throws CloneNotSupportedException
		{
			FConstOperand res = null;
			try
			{
				res = new FConstOperand(value);
			}
			catch (Exception e)
			{
				throw new CloneNotSupportedException();
			}
			return res;
		}

		@Override
        public Type getType()
        {
	        return Type.FLOATType;
        }
	}

	/**
	 * An concrete subclass of super class {@link Const8Operand} for describing
	 * long integer.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class LConstOperand extends Const8Operand
	{
		private long value;

		public LConstOperand(long value)
		{
			super();
			this.value = value;
		}

		@Override
		public Quad getQuad()
		{
			return instruction;
		}

		@Override
		public void attachToQuad(Quad q)
		{
			this.instruction = q;
		}

		@Override
		public boolean isSimilar(Operand that)
		{
			return (that instanceof LConstOperand && ((LConstOperand) that).value == this.value);
		}

		@Override
		public long getBits()
		{
			return value;
		}

		@Override
		public Object getWrapped()
		{
			return new Long(value);
		}

		@Override
		public Operand clone() throws CloneNotSupportedException
		{
			LConstOperand res = null;
			try
			{
				res = new LConstOperand(value);
			}
			catch (Exception e)
			{
				throw new CloneNotSupportedException();
			}
			return res;
		}

		@Override
        public Type getType()
        {
	        return Type.LONGType;
        }
	}

	/**
	 * An concrete subclass of super class {@link Const8Operand} for describing
	 * double-precise fixed point id.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class DConstOperand extends Const8Operand
	{
		private double value;

		public DConstOperand(double value)
		{
			super();
			this.value = value;
		}

		@Override
		public Quad getQuad()
		{
			return instruction;
		}

		@Override
		public void attachToQuad(Quad q)
		{
			this.instruction = q;
		}

		@Override
		public boolean isSimilar(Operand that)
		{
			return (that instanceof DConstOperand && ((DConstOperand) that).value == this.value);
		}

		@Override
		public long getBits()
		{
			return Double.doubleToRawLongBits(value);
		}

		@Override
		public Object getWrapped()
		{
			return new Double(value);
		}

		@Override
		public Operand clone() throws CloneNotSupportedException
		{
			DConstOperand res = null;
			try
			{
				res = new DConstOperand(value);
			}
			catch (Exception e)
			{
				throw new CloneNotSupportedException();
			}
			return res;
		}
		
		@Override
        public Type getType()
        {
	        return Type.DOUBLEType;
        }
	}

	/**
	 * An special operand just for the target of jump statement. after creating
	 * CFG and basic block, so that target of jump must be a block.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class TargetOperand extends Operand
	{
		/**
		 * The target of jump statement.
		 */
		private BasicBlock target;

		public TargetOperand(BasicBlock t)
		{
			target = t;
		}

		public BasicBlock getTarget()
		{
			return target;
		}

		public void setTarget(BasicBlock f)
		{
			this.target = f;
		}

		public String toString()
		{
			return target.toString();
		}

		public void attachToQuad(Quad q)
		{
			assert (instruction == null);
			instruction = q;
		}

		public Quad getQuad()
		{
			return instruction;
		}

		public boolean isSimilar(Operand that)
		{
			return that instanceof TargetOperand
			        && ((TargetOperand) that).getTarget() == this.getTarget();
		}

		@Override
		public Operand clone() throws CloneNotSupportedException
		{
			TargetOperand res = null;
			try
			{
				res = new TargetOperand(target);
			}
			catch (Exception e)
			{
				throw new CloneNotSupportedException();
			}
			return res;
		}
		
		@Override
        public Type getType()
        {
	        return null;
        }
	}

	public static class MethodOperand extends Operand
	{
		public Method target;

		public MethodOperand(Method m)
		{
			this.target = m;
		}

		@Override
		public Quad getQuad()
		{
			return this.instruction;
		}

		@Override
		public void attachToQuad(Quad q)
		{
			assert q != null;
			this.instruction = q;
		}

		@Override
		public boolean isSimilar(Operand that)
		{
			return (that instanceof MethodOperand 
					&& ((MethodOperand) that).target == this.target);
		}

		@Override
		public Operand clone() throws CloneNotSupportedException
		{
			MethodOperand res = null;
			try
			{
				res = new MethodOperand(target);
			}
			catch (Exception e)
			{
				throw new CloneNotSupportedException();
			}
			return res;
		}

		public String toString()
		{
			return target.toString();
		}
		
		@Override
        public Type getType()
        {
			return null;
        }
	}

	/**
	 * Represents the real parameter list when method invoked.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class ParamListOperand extends Operand
	{
		/**
		 * The real parameter list.
		 */
		public List<RegisterOperand> params;

		public ParamListOperand(List<RegisterOperand> t)
		{
			params = t;
		}

		/**
		 * Updates the parameter with index i. 
		 * @param i
		 * @param b
		 */
		public void set(int i, RegisterOperand b)
		{
			params.set(i, b);
			if (b != null) b.attachToQuad(instruction);
		}

		/**
		 * Gets the parameter with index is i.
		 * @param i
		 * @return
		 */
		public RegisterOperand get(int i)
		{
			return params.get(i);
		}
		/**
		 * Obtains the length of parameter list.
		 * @return
		 */
		public int length()
		{
			return params.size();
		}

		/**
		 * Gets the numbers of word of parameter list.
		 * @return
		 */
		public int words()
		{
			int total = 0;
			for (int i = 0; i < params.size(); ++i)
			{
				++total;
			}
			return total;
		}

		public String toString()
		{
			StringBuilder sb = new StringBuilder("(");
			if (params.size() > 0)
			{
				sb.append(params.get(0));
				for (int i = 1; i < params.size(); ++i)
				{
					sb.append(", ");
					sb.append(params.get(i));
				}
			}
			sb.append(")");
			return sb.toString();
		}

		public void attachToQuad(Quad q)
		{
			assert(instruction != null);
			instruction = q;
		}

		public Quad getQuad()
		{
			return instruction;
		}

		public boolean isSimilar(Operand that)
		{
			return false;
		}

		@Override
		public Operand clone() throws CloneNotSupportedException
		{
			List<RegisterOperand> t2 = new ArrayList<Operand.RegisterOperand>();
			for (RegisterOperand op : this.params)
				t2.add(op.clone());			
			return new ParamListOperand(t2);
		}
		
		@Override
        public Type getType()
        {
	        return null;
        }
	}

	public static class BasicBlockTableOperand extends Operand
	{
		private List<BasicBlock> tables;
		
		public BasicBlockTableOperand(List<BasicBlock> blocks)
        {
	        super();
	        this.tables = blocks;
        }

		/**
		 * Gets the numbers of basic block of this {@link BasicBlockOperand}.
		 * @return
		 */
		public int size()
		{
			return tables.size();
		}
		
        public void set(int i, BasicBlock b) { tables.set(i, b); }
        
        public BasicBlock get(int i) { return tables.get(i); }
        
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("{ ");
            if (tables.size() > 0) {
                sb.append(tables.get(0));
                for (int i=1; i<tables.size(); ++i) {
                    sb.append(", ");
                    sb.append(tables.get(i));
                }
            }
            sb.append(" }");
            return sb.toString();
        }
        @Override
        public void attachToQuad(Quad q) { assert( this.instruction != null); instruction = q; }
        /**
         * Get the attached quad of this operand.
         * @return
         */
        public Quad getQuad() { return instruction; }
        @Override
        public boolean isSimilar(Operand that) { return false; }

		@Override
        public Operand clone() throws CloneNotSupportedException
        {
			List<BasicBlock> t2 = new ArrayList<>();
			for (BasicBlock bb : this.tables)
				t2.add(bb);
			return new BasicBlockTableOperand(t2);
        }		
		@Override
        public Type getType()
        {
	        return null;
        }
	}
}
