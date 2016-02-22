package hir;

import type.Type;
import ci.*;

/**
 * This class is an abstract representation of Quadruple. In this class,
 * subclass of @ {@code Instruction} represents arithmetic and logical
 * operation, control flow operators,Phi assignment, function calling
 * conditional statement.
 * 
 * @author Jianping Zeng
 * @see BasicBlock
 * @version `1.0
 */
public abstract class Instruction implements  Cloneable
{
	/** The numbers of reference count with initial value 1. */

	public long refs = 1;
	/** Mainly for register allocation. */
	public int id;
	/**
	 * The type of value produced with this instruction. The kind is
	 * {@linkplain CiKind#Void} if this instruction produce no value.
	 */
	public CiKind kind;

	public Instruction(CiKind kind)
	{
		this.kind = kind;
		this.id = -1;
	}

	/**
	 * An interface for QuadVisitor invoking.
	 * @param visitor The instance of QuadVisitor.
	 */
	public abstract void accept(QuadVisitor visitor);

	/**
	 * Gets the text format of this Instruction.
	 * @return
	 */
	public String toString()
	{
		return "";
	}

	 @Override public Instruction clone() throws CloneNotSupportedException
	 {
		 throw new CloneNotSupportedException();
	 }

	/**
	 * The abstract base class definition for unary operator.
	 */
	public static abstract class Op1 extends Instruction
	{
		/**
		 * The field represents first operand of this instruction.
		 */
		Instruction x;

		/**
		 * Constructs unary operation.
		 * @param kind The value kind of result.
		 * @param x The sole operand.
		 */
		public Op1(CiKind kind, Instruction x)
		{
			super(kind);
			this.x = x;
		}

	}

	/**
	 * This class just for binary operation definition.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static abstract class Op2 extends Instruction
	{
		/**
		 * The two field represents first or second operand of this instruction
		 * respectively.
		 */
		Instruction x, y;

		public Op2(CiKind kind, Instruction x, Instruction y)
		{
			super(kind);
			this.x = x;
			this.y = y;
		}

	}

	public static class ADD_I extends Op2
	{
		public ADD_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "ADD_I";
		}
	}

	public static class SUB_I extends Op2
	{
		public SUB_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "SUB_I";
		}
	}

	public static class MUL_I extends Op2
	{
		public MUL_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "MUL_I";
		}
	}

	public static class DIV_I extends Op2
	{
		public DIV_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "DIV_I";
		}
	}

	public static class MOD_I extends Op2
	{
		public MOD_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "MOD_I";
		}
	}

	public static class AND_I extends Op2
	{
		public AND_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "AND_I";
		}
	}

	public static class OR_I extends Op2
	{
		public OR_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "OR_I";
		}
	}

	public static class XOR_I extends Op2
	{
		public XOR_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "XOR_I";
		}
	}

	public static class SHL_I extends Op2
	{
		public SHL_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "SHL_I";
		}
	}

	public static class SHR_I extends Op2
	{
		public SHR_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "SHR_I";
		}
	}

	/**
	 * This class served as unsigned sheft right over integer operand.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class USHR_I extends Op2
	{
		public USHR_I(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "USHR_I";
		}
	}

	public static class ADD_L extends Op2
	{
		public ADD_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "ADD_L";
		}
	}

	public static class SUB_L extends Op2
	{
		public SUB_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "SUB_L";
		}
	}

	public static class MUL_L extends Op2
	{
		public MUL_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "MUL_L";
		}
	}

	public static class DIV_L extends Op2
	{
		public DIV_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "DIV_L";
		}
	}

	public static class MOD_L extends Op2
	{
		public MOD_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "MOD_L";
		}
	}

	public static class AND_L extends Op2
	{
		public AND_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "AND_L";
		}
	}

	public static class OR_L extends Op2
	{
		public OR_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "OR_L";
		}
	}

	public static class XOR_L extends Op2
	{
		public XOR_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "XOR_L";
		}
	}

	public static class SHL_L extends Op2
	{
		public SHL_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "SHL_L";
		}
	}

	public static class SHR_L extends Op2
	{
		public SHR_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "SHR_L";
		}
	}

	/**
	 * This class served as unsigned sheft right over long integer operand.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class USHR_L extends Op2
	{
		public USHR_L(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "USHR_L";
		}
	}

	public static class ADD_F extends Op2
	{
		public ADD_F(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "ADD_F";
		}
	}

	public static class SUB_F extends Op2
	{
		public SUB_F(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "SUB_F";
		}
	}

	public static class MUL_F extends Op2
	{
		public MUL_F(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "MUL_F";
		}
	}

	public static class DIV_F extends Op2
	{
		public DIV_F(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "DIV_F";
		}
	}

	public static class MOD_F extends Op2
	{
		public MOD_F(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "MOD_F";
		}
	}

	public static class ADD_D extends Op2
	{
		public ADD_D(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "ADD_D";
		}
	}

	public static class SUB_D extends Op2
	{
		public SUB_D(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "SUB_D";
		}
	}

	public static class MUL_D extends Op2
	{
		public MUL_D(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "MUL_D";
		}
	}

	public static class DIV_D extends Op2
	{
		public DIV_D(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "DIV_D";
		}
	}

	public static class MOD_D extends Op2
	{
		public MOD_D(CiKind kind, Instruction x, Instruction y)
		{
			super(kind, x, y);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			return "MOD_D";
		}
	}

	public static class NEG_I extends Op1
	{

		public NEG_I(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "NEG_I";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class NEG_F extends Op1
	{
		public NEG_F(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "NEG_F";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class NEG_L extends Op1
	{
		public NEG_L(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "NEG_L";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class NEG_D extends Op1
	{

		public NEG_D(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "NEG_D";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class INT_2LONG extends Op1
	{

		public INT_2LONG(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2LONG";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class INT_2FLOAT extends Op1
	{
		public INT_2FLOAT(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2FLOAT";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class INT_2DOUBLE extends Op1
	{

		public INT_2DOUBLE(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2DOUBLE";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class LONG_2INT extends Op1
	{

		public LONG_2INT(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "LONG_2INT";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class LONG_2FLOAT extends Op1
	{
		public LONG_2FLOAT(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "LONG_2FLOAT";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class LONG_2DOUBLE extends Op1
	{

		public LONG_2DOUBLE(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "LONG_2DOUBLE";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class FLOAT_2INT extends Op1
	{
		public FLOAT_2INT(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "FLOAT_2INT";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class FLOAT_2LONG extends Op1
	{
		public FLOAT_2LONG(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "FLOAT_2LONG";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class FLOAT_2DOUBLE extends Op1
	{

		public FLOAT_2DOUBLE(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "FLOAT_2DOUBLE";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class DOUBLE_2INT extends Op1
	{

		public DOUBLE_2INT(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "DOUBLE_2INT";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class DOUBLE_2LONG extends Op1
	{

		public DOUBLE_2LONG(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "DOUBLE_2LONG";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

	}

	public static class DOUBLE_2FLOAT extends Op1
	{

		public DOUBLE_2FLOAT(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "DOUBLE_2FLOAT";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class INT_2BYTE extends Op1
	{

		public INT_2BYTE(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2BYTE";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class INT_2CHAR extends Op1
	{

		public INT_2CHAR(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2CHAR";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	public static class INT_2SHORT extends Op1
	{

		public INT_2SHORT(CiKind kind, Instruction x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2SHORT";
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	/**
	 * An abstract representation of branch instruction.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static abstract class Branch extends Instruction
	{
		/**
		 * Constructs a new branch instruction with specified value type.
		 * @param kind
		 */
		public Branch(CiKind kind)
		{
			super(kind);
		}
	}

	public static abstract class IntCmp extends Branch
	{
		/**
		 * The first operand also that means left-most of this branch operation.
		 */
		Instruction x;
		/**
		 * The second operand also that means right-most of this branch
		 * operation.
		 */
		Instruction y;

		BasicBlock trueTarget, falseTarget;

		IntCmp(Instruction x, Instruction y, BasicBlock trueTarget,
				BasicBlock falseTarget)
		{
			super(CiKind.Illegal);
			this.x = x;
			this.y = y;
			this.trueTarget = trueTarget;
			this.falseTarget = falseTarget;
		}

		/**
		 * Gets the first operand.
		 * @return
		 */
		public Instruction x()
		{
			return x;
		}

		/**
		 * The second operand.
		 * @return
		 */
		public Instruction y()
		{
			return y;
		}

		/**
		 * Gets the one of two direction corresponding to condition is true.
		 * @return
		 */
		public BasicBlock getTrueTarget()
		{
			return trueTarget;
		}

		/**
		 * Gets the one of two direction corresponding to condition is false.
		 * @return
		 */
		public BasicBlock getFalseTarget()
		{
			return falseTarget;
		}
	}

	public static final class IfCmp_LT extends IntCmp
	{
		public IfCmp_LT(Instruction x, Instruction y, BasicBlock trueTarget,
		        BasicBlock falseTarget)
		{
			super(x, y ,trueTarget, falseTarget);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.< --> >=)
		 * @return
		 */
		public IfCmp_GE getMirror()
		{
			return new IfCmp_GE(y, x, trueTarget, falseTarget);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static final class IfCmp_LE extends IntCmp
	{
		public IfCmp_LE(Instruction x, Instruction y, BasicBlock trueTarget,
		        BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.<= --> >)
		 * @return
		 */
		public IfCmp_GT getMirror()
		{
			return new IfCmp_GT(y, x, trueTarget, falseTarget);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static final class IfCmp_GT extends IntCmp
	{
		public IfCmp_GT(Instruction x, Instruction y, BasicBlock trueTarget,
		        BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}
		/**
		 * Swaps the operand and reverse the condition (e.g.> --> <=)
		 * @return
		 */
		public IfCmp_LE getMirror()
		{
			return new IfCmp_LE(y, x, trueTarget, falseTarget);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static class IfCmp_GE extends IntCmp
	{
		public IfCmp_GE(Instruction x, Instruction y, BasicBlock trueTarget,
		        BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}
		/**
		 * Swaps the operand and reverse the condition (e.g.>= --> <)
		 * @return
		 */
		public IfCmp_LT getMirror()
		{
			return new IfCmp_LT(y, x, trueTarget, falseTarget);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static final class IfCmp_EQ extends IntCmp
	{
		public IfCmp_EQ(Instruction x, Instruction y, BasicBlock trueTarget,
		        BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}
		/**
		 * Swaps the operand and reverse the condition (e.g.== --> !=)
		 * @return
		 */
		public IfCmp_NEQ getMirror()
		{
			return new IfCmp_NEQ(x, y, falseTarget, trueTarget);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static final class IfCmp_NEQ extends IntCmp
	{
		public IfCmp_NEQ(Instruction x, Instruction y, BasicBlock trueTarget,
		        BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}
		/**
		 * Swaps the operand and reverse the condition (e.g.!= --> ==)
		 * @return
		 */
		public IfCmp_EQ getMirror()
		{
			return new IfCmp_EQ(y, x, falseTarget, trueTarget);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * The {@code Goto} instruction represents the end of a block that
	 * unconditional branches to another basic block.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Goto extends Branch
	{
		/**
		 * The jump target of this goto instruction.
		 */
		BasicBlock target;

		/**
		 * Constructs a new {@code Goto} instruction with specified jump target.
		 * @param target The target block of this unconditional jump.
		 */
		public Goto(BasicBlock target)
		{
			super(CiKind.Illegal);
			this.target = target;
		}

		@Override
		public void accept(QuadVisitor visitor)
		{

		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * This {@code Return} class definition.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Return extends Branch
	{
		Instruction result;

		/**
		 * Constructs a new return instruction with return value.
		 * @param result The return value produce for this instruction, return
		 *            void if result is {@code null}.
		 */
		public Return(Instruction result)
		{
			super(result == null ? CiKind.Void : result.kind);
			this.result = result;
		}

		/**
		 * Gets the instruction that produces the result for the return.
		 * @return the instruction producing the result
		 */
		public Instruction result()
		{
			return result;
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * Method invocation instruction.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Invoke extends Instruction
	{
		/**
		 * The input arguments for function calling.
		 */
		public final Instruction[] arguments;
		/**
		 * The target of this method calling.
		 */
		public final Method target;

		/**
		 * Constructs a new method calling instruction.
		 * @param result The kind of return result.
		 * @param args The input arguments.
		 * @param target The called method.
		 * @param returnType
		 */
		public Invoke(CiKind result, Instruction[] args, Method target)
		{
			super(result);
			this.target = target;
			this.arguments = args;
		}

		@Override
		public void accept(QuadVisitor visitor)
		{

		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * The {@code Phi} instruction represents the merging of data flow in the
	 * instruction graph. It refers to a join block and a variable.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Phi extends Instruction
	{
		/**
		 * The return value
		 */
		public Instruction result;

		/**
		 * The parameters list of Phi assignment.
		 */
		public Instruction[] args;

		/**
		 * The derived basic block from owned inputed parameter of this Phi
		 * assignment.
		 */
		public BasicBlock[] derivedBlocks;

		/**
		 * Constructs a new Phi-function instruction.
		 * @param kind The kind of result.
		 * @param result The return result of this instruction.
		 * @param args The input arguments.
		 * @param blocks The one of which basic block array is corresponding to
		 *            an input argument.
		 */
		public Phi(CiKind kind, Instruction result, Instruction[] args,
		        BasicBlock[] blocks)
		{
			super(kind);
			this.result = result;
			this.args = args;
			this.derivedBlocks = blocks;
		}

		@Override
		public void accept(QuadVisitor visitor)
		{

		}

		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * The {@code Constant} instruction represents a constant such as an integer
	 * value, long, float, object reference, address, etc.
	 */
	public final static class Constant extends Instruction
	{
		/**
		 * The constant value keeped with {@code Constant} instance.
		 */
		public CiConstant value;

		/**
		 * Constructs a new instruction representing the specified constant.
		 */
		public Constant(CiConstant value)
		{
			super(value.kind);
			this.value = value;
		}

		public void setValue(CiConstant value)
		{
			this.value = value;
		}

		/**
		 * Creates an instruction for a double constant.
		 * @param d the double value for which to create the instruction
		 * @return an instruction representing the double
		 */
		public static Constant forDouble(double d)
		{
			return new Constant(CiConstant.forDouble(d));
		}

		/**
		 * Creates an instruction for a float constant.
		 * @param f the float value for which to create the instruction
		 * @return an instruction representing the float
		 */
		public static Constant forFloat(float f)
		{
			return new Constant(CiConstant.forFloat(f));
		}

		/**
		 * Creates an instruction for an long constant.
		 * @param i the long value for which to create the instruction
		 * @return an instruction representing the long
		 */
		public static Constant forLong(long i)
		{
			return new Constant(CiConstant.forLong(i));
		}

		/**
		 * Creates an instruction for an integer constant.
		 * @param i the integer value for which to create the instruction
		 * @return an instruction representing the integer
		 */
		public static Constant forInt(int i)
		{
			return new Constant(CiConstant.forInt(i));
		}

		/**
		 * Creates an instruction for a boolean constant.
		 * @param i the boolean value for which to create the instruction
		 * @return an instruction representing the boolean
		 */
		public static Constant forBoolean(boolean i)
		{
			return new Constant(CiConstant.forBoolean(i));
		}

		/**
		 * Creates an instruction for an object constant.
		 * @param o the object value for which to create the instruction
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

		public boolean valueEqual(Instruction i)
		{
			return i instanceof Constant
			        && ((Constant) i).value.equivalent(this.value);
		}

		@Override
        public void accept(QuadVisitor visitor)
        {
	        
        }
	}

	/**
	 * This class implements allocating memory at stack frame of current
	 * executed function.
	 */
	public static class Alloca extends Instruction
	{
		public Alloca(CiKind kind)
		{
			super(kind);
		}

		@Override public void accept(QuadVisitor visitor)
		{

		}
	}

	public static abstract class Var extends Instruction
	{
		/**
		 * The value type of this local variable.
		 */
		Type valueType;
		/**
		 * The memory address allocated by instruction {@code Alloca} is related with this variable.
		 */
		final Alloca memAddr;
		/**
		 * The name of variable, which is similar to IR in LLVM.
		 * For global variable and local variable, those are starts with symbol'@'
		 * and '%' respectively.
		 * <p>
		 * To visit <a hrep = "http://llvm.org/docs/LangRef.html#global-variables"></a> for detail.
		 * </p>
		 */
		String name;

		public Var(CiKind kind, Alloca mem, String name)
		{
			super(kind);
			this.memAddr = mem;
			this.name = name;
		}

		/**
		 * Sets the value type of this declared variable.
		 * @param valueType
		 */
		public void setValueType(Type valueType)
		{
			this.valueType = valueType;
		}
		/**
		 * Gets the value type of this declared variable.
		 * @return
		 */
		public Type getValueType()
		{
			return valueType;
		}
	}

	/**
	 * This class is served as a placeholder for Local {@code VarDef} definition.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static final class Local extends Var
	{
		/**
		 * Constructs a new local instance.
		 * @param kind  The kind of value type.
		 * @param mem   The memory address associated with instance.
		 * @param namePrefix The name postfix of to being yielded.
		 */
		public Local(CiKind kind, Alloca mem, String namePrefix)
		{
			super(kind, mem, "%" + namePrefix);
		}
		
		@Override
        public void accept(QuadVisitor visitor)
        {
	        
        }
	}
	/**
	 * An instruction for writing data into memory.
	 */
	public static class StoreInst extends Instruction
	{
		/**
		 * The value being writed into destination variable.
		 */
		private Instruction value;
		/**
		 * The target of writing.
		 */
		private Local dest;

		/**
		 * Constructs a new store instruction.
		 * @param value The value to being writed into memory.
		 * @param dest  The target memory address where value stores.
		 */
		public StoreInst(Instruction value, Local dest)
		{
			super(CiKind.Illegal);
			this.value = value;
			this.dest = dest;
		}

		@Override public void accept(QuadVisitor visitor)
		{

		}
	}

	/**
	 * An instruction for reading data from memory.
	 */
	public static class LoadInst extends  Instruction
	{
		/**
		 * The source memory where desired value reading.
		 */
		private Local from;

		public LoadInst(CiKind kind, Local from)
		{
			super(kind);
			this.from = from;
		}

		@Override
		public void accept(QuadVisitor visitor)
		{

		}
	}

	/**
	 * This class represents move instruction.
	 */
	public static final class Move extends Op2
	{
		/**
		 * Constructs a new move instruction.
		 * @param kind  The kind of result.
		 * @param src   The source of move.
		 * @param dest  The target of move.
		 */
		public Move(CiKind kind, Instruction src, Instruction dest)
		{
			super(kind, src, dest);
		}

		/**
		 * An interface for QuadVisitor invoking.
		 *
		 * @param visitor The instance of QuadVisitor.
		 */
		@Override public void accept(QuadVisitor visitor)
		{

		}
	}
}
