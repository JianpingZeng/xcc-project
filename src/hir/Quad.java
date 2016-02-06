package hir;

import java.util.ArrayList;
import type.Type;
import hir.Operand.BasicBlockTableOperand;
import hir.Operand.MethodOperand;
import hir.Operand.ParamListOperand;
import hir.Operand.RHS;
import hir.Operand.RegisterOperand;
import hir.Operand.TargetOperand;
import hir.QuadVisitor;
import hir.RegisterFactory.Register;

/**
 * This class is an abstract representation of Quadruple.
 * In this class, a subclass {@link Phi} was introduced for handling 
 * insertion of Phi assignment statement at control flow joint node.
 * 
 * @author Jianping Zeng
 * @see BasicBlock
 * @version `1.0
 */
public abstract class Quad
{

	/** the id is just used for print debugging information. */
	public int id;

	public Quad(int number)
	{
		this.id = number;
	}
	/**
	 * An interface for QuadVisitor invoking.
	 * @param visitor	The instance of QuadVisitor.
	 */
	public abstract void accept(QuadVisitor visitor);

	/**
	 * An abstract quad instruction representation for Assign instruction.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static abstract class Assign extends Quad
	{
		public RegisterOperand result;
		public RHS operand1;

		public static Assign create(int id, Register result, RHS operand1,
		        Type t)
		{
			if (t.isIntLike())
			    return new Assign_I(id, new RegisterOperand(result, t),
			            operand1);
			if (t == Type.FLOATType)
			    return new Assign_F(id, new RegisterOperand(result, t),
			            operand1);
			if (t == Type.LONGType)
			    return new Assign_L(id, new RegisterOperand(result, t),
			            operand1);
			if (t == Type.DOUBLEType)
			    return new Assign_D(id, new RegisterOperand(result, t),
			            operand1);
			return null;
		}

		protected Assign(int id, RegisterOperand result, RHS operand1)
		{
			super(id);
		}
	}

	/**
	 * An concrete quad instruction representation for Assign instruction with
	 * integer type.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Assign_I extends Assign
	{
		public Assign_I(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	/**
	 * An concrete quad instruction representation for Assign instruction with
	 * float type.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Assign_F extends Assign
	{
		public Assign_F(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	/**
	 * An concrete quad instruction representation for Assign instruction with
	 * long type.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Assign_L extends Assign
	{
		public Assign_L(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	/**
	 * An concrete quad instruction representation for Assign instruction with
	 * double type.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Assign_D extends Assign
	{
		public Assign_D(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
		}

		@Override
		public void accept(QuadVisitor visitor)
		{
			// TODO Auto-generated method stub

		}
	}

	/**
	 * An abstract base class for binary operator. There are sorts of different
	 * subclass served as Intermediate Representation for different binary
	 * operator.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static abstract class Binary extends Quad
	{
		/**
		 * The result of this quad instruction.
		 */
		public RegisterOperand result;
		/**
		 * The first and second operand of this quad.
		 */
		public RHS operand1, operand2;

		public Binary(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id);
			this.result = result;
			this.operand1 = operand1;
			this.operand2 = operand2;
		}

		/**
		 * Estimates whether this quad has side effect or not. return true if
		 * done , otherwise return false.
		 * @return
		 */
		public boolean hasSideEffects()
		{
			return false;
		}
	}

	public static class ADD_I extends Binary
	{
		public ADD_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class SUB_I extends Binary
	{
		public SUB_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class MUL_I extends Binary
	{
		public MUL_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class DIV_I extends Binary
	{
		public DIV_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class MOD_I extends Binary
	{
		public MOD_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class AND_I extends Binary
	{
		public AND_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class OR_I extends Binary
	{
		public OR_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class XOR_I extends Binary
	{
		public XOR_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class SHL_I extends Binary
	{
		public SHL_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class SHR_I extends Binary
	{
		public SHR_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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
	public static class USHR_I extends Binary
	{
		public USHR_I(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class ADD_L extends Binary
	{
		public ADD_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class SUB_L extends Binary
	{
		public SUB_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class MUL_L extends Binary
	{
		public MUL_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class DIV_L extends Binary
	{
		public DIV_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class MOD_L extends Binary
	{
		public MOD_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class AND_L extends Binary
	{
		public AND_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class OR_L extends Binary
	{
		public OR_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class XOR_L extends Binary
	{
		public XOR_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class SHL_L extends Binary
	{
		public SHL_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class SHR_L extends Binary
	{
		public SHR_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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
	public static class USHR_L extends Binary
	{
		public USHR_L(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class ADD_F extends Binary
	{
		public ADD_F(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class SUB_F extends Binary
	{
		public SUB_F(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class MUL_F extends Binary
	{
		public MUL_F(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class DIV_F extends Binary
	{
		public DIV_F(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class MOD_F extends Binary
	{
		public MOD_F(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class ADD_D extends Binary
	{
		public ADD_D(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class SUB_D extends Binary
	{
		public SUB_D(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class MUL_D extends Binary
	{
		public MUL_D(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class DIV_D extends Binary
	{
		public DIV_D(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	public static class MOD_D extends Binary
	{
		public MOD_D(int id, RegisterOperand result, RHS operand1, RHS operand2)
		{
			super(id, result, operand1, operand2);
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

	/**
	 * This is abstract class for describing unary instruction.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */

	public abstract static class Unary extends Quad
	{
		/**
		 * The result of this quad.
		 */
		public RegisterOperand result;
		/**
		 * The first and only operand.
		 */
		public RHS operand1;

		public Unary(int id, RegisterOperand result, RHS operand1)
		{
			super(id);
			this.result = result;
			this.operand1 = operand1;
		}

		/**
		 * Estimates whether this quad has side effect or not. return true if
		 * done , otherwise return false.
		 * @return
		 */
		public boolean hasSideEffects()
		{
			return false;
		}
	}

	public static class NEG_I extends Unary
	{

		public NEG_I(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class NEG_F extends Unary
	{
		public NEG_F(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class NEG_L extends Unary
	{
		public NEG_L(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class NEG_D extends Unary
	{

		public NEG_D(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class INT_2LONG extends Unary
	{

		public INT_2LONG(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class INT_2FLOAT extends Unary
	{
		public INT_2FLOAT(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class INT_2DOUBLE extends Unary
	{

		public INT_2DOUBLE(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class LONG_2INT extends Unary
	{

		public LONG_2INT(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class LONG_2FLOAT extends Unary
	{
		public LONG_2FLOAT(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class LONG_2DOUBLE extends Unary
	{

		public LONG_2DOUBLE(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class FLOAT_2INT extends Unary
	{
		public FLOAT_2INT(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class FLOAT_2LONG extends Unary
	{
		public FLOAT_2LONG(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class FLOAT_2DOUBLE extends Unary
	{

		public FLOAT_2DOUBLE(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class DOUBLE_2INT extends Unary
	{

		public DOUBLE_2INT(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class DOUBLE_2LONG extends Unary
	{

		public DOUBLE_2LONG(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class DOUBLE_2FLOAT extends Unary
	{

		public DOUBLE_2FLOAT(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class INT_2BYTE extends Unary
	{

		public INT_2BYTE(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class INT_2CHAR extends Unary
	{

		public INT_2CHAR(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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

	public static class INT_2SHORT extends Unary
	{

		public INT_2SHORT(int id, RegisterOperand result, RHS operand1)
		{
			super(id, result, operand1);
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
	 * An representation of abstract brach instruction.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static abstract class Branch extends Quad
	{
		public Branch(int number)
        {
	        super(number);
        }

		public boolean hasSideEffects()
		{
			return true;
		}
	}
    public abstract static class IntIfCmp extends Branch 
    {
		public TargetOperand target;
		public RHS operand1, operand2;
		public IntIfCmp(int id, RHS operand1, RHS operand2, TargetOperand target)
		{
			super(id);
			this.target = target;
			this.operand1 = operand1;
			this.operand2 = operand2;
		}
    }
	
	public static class IfCmp_LT extends IntIfCmp
	{
		public IfCmp_LT(int id, RHS operand1, RHS operand2, TargetOperand target)
		{
			super(id, operand1, operand2, target);
		}

		@Override
        public void accept(QuadVisitor visitor)
        {
	        // TODO Auto-generated method stub
	        
        }
	}
	
	public static class IfCmp_LE extends IntIfCmp
	{
		public IfCmp_LE(int id, RHS operand1, RHS operand2, TargetOperand target)
		{
			super(id, operand1, operand2, target);
		}

		@Override
        public void accept(QuadVisitor visitor)
        {
	        // TODO Auto-generated method stub
	        
        }
	}
	
	public static class IfCmp_GT extends IntIfCmp
	{
		public IfCmp_GT(int id, RHS operand1, RHS operand2, TargetOperand target)
		{
			super(id, operand1, operand2, target);
		}

		@Override
        public void accept(QuadVisitor visitor)
        {
	        // TODO Auto-generated method stub
	        
        }
	}
	
	public static class IfCmp_GE extends IntIfCmp
	{
		public IfCmp_GE(int id, RHS operand1, RHS operand2, TargetOperand target)
		{
			super(id, operand1, operand2, target);
		}

		@Override
        public void accept(QuadVisitor visitor)
        {
	        // TODO Auto-generated method stub
	        
        }
	}
	
	public static class IfCmp_EQ extends IntIfCmp
	{
		public IfCmp_EQ(int id, RHS operand1, RHS operand2, TargetOperand target)
		{
			super(id, operand1, operand2, target);
		}

		@Override
        public void accept(QuadVisitor visitor)
        {
	        // TODO Auto-generated method stub
	        
        }
	}
	
	public static class IfCmp_NEQ extends IntIfCmp
	{
		public IfCmp_NEQ(int id, RHS operand1, RHS operand2, TargetOperand target)
		{
			super(id, operand1, operand2, target);
		}

		@Override
        public void accept(QuadVisitor visitor)
        {
	        // TODO Auto-generated method stub
	        
        }
	}
	
	public static class Goto extends Branch
	{
		public TargetOperand target;
		public Goto(int id, TargetOperand target)
        {
			super(id);
			this.target = target;
        }
		@Override
        public void accept(QuadVisitor visitor)
        {
	        
        }		
	}
	
	public static class Return extends Quad
	{
		public RHS operand1;
		public Return(int id, RHS operand1)
        {
			super(id);
			this.operand1 = operand1;
        }
		@Override
        public void accept(QuadVisitor visitor)
        {
	        // TODO Auto-generated method stub
	        
        }
	}
	
	/**
	 * Method invocation instruction.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Invoke extends Quad
	{
		/**
		 * The return value of invoked method.
		 */
		public RegisterOperand ret;
		
		/**
		 * Invoked method.
		 */
		public MethodOperand method;
		/**
		 * The real parameter list when invoking method.
		 */
		public ParamListOperand params;
		public Invoke(int id, RegisterOperand ret, MethodOperand m, ParamListOperand params)
        {
			super(id);
			this.ret = ret;
			this.params = params;
        }
		
		@Override
		public void accept(QuadVisitor visitor)
		{
				    
		}
	}
	
	/**
	 * This a special subclass of base class {@link Quad} just only for Phi-assignment
	 * to be inserted where dominant frontier of this basic block where global variable
	 * defines.
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 *
	 */
	public static class Phi extends Quad
	{
		/**
		 * The return value
		 */
		public RegisterOperand res;
		
		/**
		 * The parameters list of Phi assignment.
		 */
		public ParamListOperand params;
		
		/**
		 * The derived basic block from owned inputed parameter of this Phi assignment.
		 */
		public BasicBlockTableOperand derivedBlocks;
		
		public Phi(int id, RegisterOperand res, int lenght)
        {
			super(id);
			this.res = res;
			this.params = new ParamListOperand(new ArrayList<>(lenght));
			this.derivedBlocks = new BasicBlockTableOperand(new ArrayList<>(lenght));		
        }
		
		@Override
		public void accept(QuadVisitor visitor)
		{
				    
		}
	}
}
