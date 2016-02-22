package lir;

import hir.CiValue;

import java.util.ArrayList;
import java.util.List;

import type.Type;


/**
 * This class that represents an operand in Instruction, which are two different
 * variety of subclasses, including literal and variable.
 * 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2015年12月23日 上午9:10:45
 */
public class Operand implements Cloneable
{
	/**
	 * The value of this operand.
	 */
	CiValue value;
	
	/**
	 * The constructor.
	 * @param value
	 */
	public Operand(CiValue value)
    {
		this.value = value;
    }
	
	/**
	 * Evaluates whether given operand that is similar to this operand or not.
	 * @param that A given operand to be estimated.
	 * @return True return if those are similar to each other, otherwise return
	 *         false.
	 */
	public boolean isSimilar(Operand that)
	{
		return false;
	}

	@Override
	public Operand clone() throws CloneNotSupportedException
	{
		return null;
	}

    /**
     * Gets the value of this operand. This may still be a {@linkplain CiVariable}
     * if the register allocator has not yet assigned a register or stack address to the operand.
     *
     */
    public CiValue getValue(Instruction inst) {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
    
	/**
	 * Represents a virtual register operand. Jianping Zeng
	 * <z1215jping@hotmail.com>
	 */
	public static class VariableOperand extends Operand 
	{
		int index;

		public VariableOperand(int index, CiValue value)
		{
			super(value);
			this.index = index;			 
		}
		@Override
		public VariableOperand clone() throws CloneNotSupportedException
		{
			VariableOperand res = null;
			try
			{
				new VariableOperand(this.index, this.value);
			}
			catch (Exception e)
			{
				throw new CloneNotSupportedException();
			}
			return res;
		}

		public boolean isSimilar(Operand that)
		{
			return that instanceof VariableOperand
			        && ((VariableOperand) that).index == this.index 
			        && ((VariableOperand) that).value == this.value;
		}

		public String toString()
		{
			return value.toString();
		}
	}
	/**
     * An address operand with at least one {@linkplain CiVariable variable} constituent.
     */
    static class AddressOperand extends Operand {
        int base;
        int index;

        AddressOperand(int base, int index, CiAddress address) {
            super(address);
            assert base != -1 || index != -1 : "address should have at least one variable part";
            this.base = base;
            this.index = index;
        }

        @Override
        public CiValue getValue(Instruction inst) {
            if (base != -1 || index != -1) {
                CiAddress address = (CiAddress) value;
                CiValue baseOperand = base == -1 ? address.base : inst.allocatorOperands.get(base);
                CiValue indexOperand = index == -1 ? address.index : inst.allocatorOperands.get(index);
                if (address.index.isLegal()) {
                    assert indexOperand.isVariableOrRegister();
                    if (baseOperand.isVariable() || indexOperand.isVariable()) {
                        return address;
                    }
                } else {
                    if (baseOperand.isVariable()) {
                        return address;
                    }
                }
                value = new CiAddress(address.kind, baseOperand, indexOperand, address.scale, address.displacement);
                base = -1;
                index = -1;
            }
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
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
		public Instruction getQuad()
		{
			return this.instruction;
		}

		@Override
		public void attachToQuad(Instruction q)
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
		public List<VariableOperand> params;

		public ParamListOperand(List<VariableOperand> t)
		{
			params = t;
		}

		/**
		 * Updates the parameter with index i. 
		 * @param i
		 * @param b
		 */
		public void set(int i, VariableOperand b)
		{
			params.set(i, b);
			if (b != null) b.attachToQuad(instruction);
		}

		/**
		 * Gets the parameter with index is i.
		 * @param i
		 * @return
		 */
		public VariableOperand get(int i)
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

		public void attachToQuad(Instruction q)
		{
			assert(instruction != null);
			instruction = q;
		}

		public Instruction getQuad()
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
			List<VariableOperand> t2 = new ArrayList<Operand.VariableOperand>();
			for (VariableOperand op : this.params)
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
        public void attachToQuad(Instruction q) { assert( this.instruction != null); instruction = q; }
        /**
         * Get the attached quad of this operand.
         * @return
         */
        public Instruction getQuad() { return instruction; }
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
