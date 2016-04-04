package hir;

import ci.CiKind;
import exception.SemanticError;
import type.Type;
import utils.Pair;
import utils.Utils;

import java.util.HashMap;

/**
 * This class is an abstract representation of Quadruple. In this class,
 * subclass of @ {@code Instruction} represents arithmetic and logical
 * operation, control flow operators,Phi assignment, function calling
 * conditional statement.
 *
 * @author Jianping Zeng
 * @version 1.0
 * @see BasicBlock
 * @see User
 * @see Value
 * @see Use
 */
public abstract class Instruction extends User
{
	/**
	 * Mainly for register allocation.
	 */
	public int id;

	public final Operator opcode;

	/**
	 * The ret of operation over this instruction, it is null if this instruction
	 * no return ret.
	 */
	//public Instruction ret;

	private BasicBlock bb;

	/**
	 * The name of this instruction.
	 */
	protected String instName;

	public BasicBlock getParent()
	{
		return bb;
	}

	public void setParent(BasicBlock bb)
	{
		this.bb = bb;
	}

	public Instruction(CiKind kind, Operator opcode, String instName)
	{
		super(kind);
		this.id = -1;
		this.opcode = opcode;
		this.instName = instName;
	}

	/**
	 * Erases this instruction from it's parent basic block.
	 */
	public void eraseFromBasicBlock()
	{
		assert (this.bb
				== null) : "The basic block where the instruction reside to be erased!";
		bb.removeInst(this);
	}

	/**
	 * An interface for ValueVisitor invoking.
	 *
	 * @param visitor The instance of ValueVisitor.
	 */
	public abstract void accept(ValueVisitor visitor);

	/**
	 * Gets the text format of this Instruction.
	 *
	 * @return
	 */
	public String toString()
	{
		if (opcode != null)
			return opcode.opName;
		else
			return "";
	}

	/**
	 * For value number to determine whether this instruction is equivalent to
	 * that value.
	 *
	 * @param value Targeted instruction to be checked.
	 * @return return false by default.
	 */
	public boolean valueEqual(Instruction value)
	{
		return false;
	}

	/**
	 * For global or local inst numbering with initialization 0.
	 */
	public int valueNumber()
	{
		return 0;
	}

	/**
	 * Inserts an specified instruction into the instructions list after itself.
	 *
	 * @param inst An instruction to be inserted.
	 */
	public void insertAfter(Instruction inst)
	{
		int index = bb.lastIndexOf(inst);
		if (index >= 0 && index < bb.size())
			bb.addInst(inst, index + 1);
	}

	/**
	 * Inserts an instruction into the instructions list before this itself.
	 *
	 * @param inst An instruction to be inserted.
	 */
	public void insertBefore(Instruction inst)
	{
		int index = bb.lastIndexOf(inst);
		if (index >= 0 && index < bb.size())
			bb.addInst(inst, index);
	}

	/**
	 * Links the user value and its user.
	 *
	 * @param v The used {@code Value}.
	 * @param u The {@code Instruction} or other instance of {@code User}.
	 */
	void use(Value v, User u)
	{
		assert v != null && u != null;
		v.addUse(new Use(v, u));
	}

	/**
	 * Obtains the name of this instruction.
	 *
	 * @return
	 */
	public String getName()
	{
		return instName;
	}

	/**
	 * The abstract base class definition for unary operator.
	 */
	public static abstract class Op1 extends Instruction
	{
		/**
		 * The field represents first operand of this instruction.
		 */
		public Value x;

		/**
		 * Constructs unary operation.
		 *
		 * @param kind   The inst kind of ret.
		 * @param opcode The operator code for this instruction.
		 * @param x      The sole operand.
		 */
		public Op1(CiKind kind, Operator opcode, Value x, String name)
		{
			super(kind, opcode, name);
			this.x = x;
			use(x, this);
		}

		@Override public int valueNumber()
		{
			return Utils.hash1(opcode.index, x);
		}

	}

	/**
	 * This class just for binary operation definition.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static abstract class Op2 extends Instruction
	{
		/**
		 * The two field represents first or second operand of this instruction
		 * respectively.
		 */
		public Value x, y;

		public Op2(CiKind kind, Operator opcode, Value x, Value y, String name)
		{
			super(kind, opcode, name);
			this.x = x;
			this.y = y;

			assert x.kind
					== y.kind : "Cannot create binary operator with two operands of differing type!";
			use(x, this);
			use(y, this);
		}

		/**
		 * This method is used for attempting to swap the two operands of this
		 * binary instruction.
		 */
		public void swapOperands()
		{
			Value temp = x;
			x = y;
			y = temp;
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(opcode.index, x, y);
		}
	}

	public static class ADD_I extends Op2
	{
		public ADD_I(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.IAnd, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitADD_I(this);
		}
	}

	public static class SUB_I extends Op2
	{
		public SUB_I(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.ISub, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitSUB_I(this);
		}
	}

	public static class MUL_I extends Op2
	{
		public MUL_I(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.IMul, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitMUL_I(this);
		}
	}

	public static class DIV_I extends Op2
	{
		public DIV_I(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.IDiv, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitDIV_I(this);
		}
	}

	public static class MOD_I extends Op2
	{
		public MOD_I(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.IMod, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitMOD_I(this);
		}
	}

	public static class AND_I extends Op2
	{
		public AND_I(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.IAnd, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitAND_I(this);
		}
	}

	public static class OR_I extends Op2
	{
		public OR_I(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.IOr, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitOR_I(this);
		}
	}

	public static class XOR_I extends Op2
	{
		public XOR_I(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.IXor, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitXOR_I(this);
		}
	}

	public static class ShiftOp extends Op2
	{

		public ShiftOp(CiKind kind, Operator opcode, Value x, Value y,
				String name)
		{
			super(kind, opcode, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitShiftOp(this);
		}
	}

	public static class ADD_L extends Op2
	{
		public ADD_L(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.LAdd, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitADD_L(this);
		}
	}

	public static class SUB_L extends Op2
	{
		public SUB_L(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.LSub, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitSUB_L(this);
		}
	}

	public static class MUL_L extends Op2
	{
		public MUL_L(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.LMul, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitMUL_L(this);
		}
	}

	public static class DIV_L extends Op2
	{
		public DIV_L(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.LDiv, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitDIV_L(this);
		}
	}

	public static class MOD_L extends Op2
	{
		public MOD_L(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.LMod, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitMOD_L(this);
		}
	}

	public static class AND_L extends Op2
	{
		public AND_L(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.LAnd, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitAND_L(this);
		}
	}

	public static class OR_L extends Op2
	{
		public OR_L(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.LOr, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitOR_L(this);
		}
	}

	public static class XOR_L extends Op2
	{
		public XOR_L(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.LXor, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitXOR_L(this);
		}
	}

	public static class ADD_F extends Op2
	{
		public ADD_F(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.FAdd, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitADD_F(this);
		}
	}

	public static class SUB_F extends Op2
	{
		public SUB_F(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.FSub, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitSUB_F(this);
		}
	}

	public static class MUL_F extends Op2
	{
		public MUL_F(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.FMul, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitMUL_F(this);
		}
	}

	public static class DIV_F extends Op2
	{
		public DIV_F(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.FDiv, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitDIV_F(this);
		}
	}

	public static class ADD_D extends Op2
	{
		public ADD_D(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.DAdd, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitADD_D(this);
		}
	}

	public static class SUB_D extends Op2
	{
		public SUB_D(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.DSub, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitSUB_D(this);
		}
	}

	public static class MUL_D extends Op2
	{
		public MUL_D(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.DMul, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitMUL_D(this);
		}
	}

	public static class DIV_D extends Op2
	{
		public DIV_D(CiKind kind, Value x, Value y, String name)
		{
			super(kind, Operator.DDiv, x, y, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitDIV_D(this);
		}
	}

	public static class NEG_I extends Op1
	{
		public NEG_I(CiKind kind, Value x, String name)
		{
			super(kind, Operator.INeg, x, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitNEG_I(this);
		}
	}

	public static class NEG_F extends Op1
	{
		public NEG_F(CiKind kind, Value x, String name)
		{
			super(kind, Operator.FNeg, x, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitNEG_F(this);
		}
	}

	public static class NEG_L extends Op1
	{
		public NEG_L(CiKind kind, Value x, String name)
		{
			super(kind, Operator.LNeg, x, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitNEG_L(this);
		}
	}

	public static class NEG_D extends Op1
	{

		public NEG_D(CiKind kind, Value x, String name)
		{
			super(kind, Operator.DNeg, x, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitNEG_D(this);
		}
	}

	public static class Convert extends Op1
	{
		public Convert(CiKind kind, Operator opcode, Value x, String name)
		{
			super(kind, opcode, x, name);
		}

		/**
		 * An interface for ValueVisitor invoking.
		 *
		 * @param visitor The instance of ValueVisitor.
		 */
		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitConvert(this);
		}
	}

	/**
	 * An abstract representation of branch instruction.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static abstract class Branch extends Instruction
	{
		/**
		 * Constructs a new branch instruction with specified inst type.
		 *
		 * @param kind
		 */
		public Branch(CiKind kind, Operator opcode, String name)
		{
			super(kind, opcode, name);
		}
	}

	public static abstract class ConditionalBranch extends Branch
	{
		public BasicBlock trueTarget, falseTarget;

		/**
		 * Constructs a new conditional branch instruction with specified inst type.
		 *
		 * @param kind
		 */
		public ConditionalBranch(CiKind kind, Operator opcode, String name)
		{
			super(kind, opcode, name);
		}
	}

	public static class BR extends ConditionalBranch
	{
		Value x;

		public BR(Value x, BasicBlock trueTarget, BasicBlock falseTarget,
				String name)
		{
			super(CiKind.Illegal, Operator.Br, name);
			this.x = x;
			this.trueTarget = trueTarget;
			this.falseTarget = falseTarget;
		}

		/**
		 * An interface for ValueVisitor invoking.
		 *
		 * @param visitor The instance of ValueVisitor.
		 */
		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitBR(this);
		}
	}

	public static abstract class IntCmp extends ConditionalBranch
	{
		/**
		 * The first operand also that means left-most of this branch operation.
		 */
		Value x;
		/**
		 * The second operand also that means right-most of this branch
		 * operation.
		 */
		Value y;

		IntCmp(Operator opcode, Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget, String name)
		{
			super(CiKind.Illegal, opcode, name);
			this.x = x;
			this.y = y;
			this.trueTarget = trueTarget;
			this.falseTarget = falseTarget;
		}

		/**
		 * Gets the first operand.
		 *
		 * @return
		 */
		public Value x()
		{
			return x;
		}

		/**
		 * The second operand.
		 *
		 * @return
		 */
		public Value y()
		{
			return y;
		}

		/**
		 * Gets the one of two direction corresponding to condition is true.
		 *
		 * @return
		 */
		public BasicBlock getTrueTarget()
		{
			return trueTarget;
		}

		/**
		 * Gets the one of two direction corresponding to condition is false.
		 *
		 * @return
		 */
		public BasicBlock getFalseTarget()
		{
			return falseTarget;
		}

		/**
		 * Return the corresponding target block determinated by condition.
		 * If the condition is true, the True Target is returned, otherwise
		 * return false target.
		 *
		 * @param istrue
		 * @return
		 */
		public BasicBlock successor(boolean istrue)
		{
			return istrue ? trueTarget : falseTarget;
		}
	}

	public static abstract class Cmp extends Op2
	{
		Condition cond;
		private static HashMap<Condition, Operator> intCondOpcodeMap;
		private static HashMap<Condition, Operator> longCondOpcodeMap;
		private static HashMap<Condition, Operator> floatCondOpcodeMap;
		private static HashMap<Condition, Operator> doubleCondOpcodeMap;

		static
		{
			intCondOpcodeMap = new HashMap<>();
			longCondOpcodeMap = new HashMap<>();
			floatCondOpcodeMap = new HashMap<>();
			doubleCondOpcodeMap = new HashMap<>();

			intCondOpcodeMap.put(Condition.EQ, Operator.ICmpEQ);
			intCondOpcodeMap.put(Condition.NE, Operator.ICmpNE);
			intCondOpcodeMap.put(Condition.LT, Operator.ICmpLT);
			intCondOpcodeMap.put(Condition.LE, Operator.ICmpLE);
			intCondOpcodeMap.put(Condition.GT, Operator.ICmpGT);
			intCondOpcodeMap.put(Condition.GE, Operator.ICmpGE);

			longCondOpcodeMap.put(Condition.EQ, Operator.LCmpEQ);
			longCondOpcodeMap.put(Condition.NE, Operator.LCmpNE);
			longCondOpcodeMap.put(Condition.LT, Operator.LCmpLT);
			longCondOpcodeMap.put(Condition.LE, Operator.LCmpLE);
			longCondOpcodeMap.put(Condition.GT, Operator.LCmpGT);
			longCondOpcodeMap.put(Condition.GE, Operator.LCmpGE);

			floatCondOpcodeMap.put(Condition.EQ, Operator.FCmpEQ);
			floatCondOpcodeMap.put(Condition.NE, Operator.FCmpNE);
			floatCondOpcodeMap.put(Condition.LT, Operator.FCmpLT);
			floatCondOpcodeMap.put(Condition.LE, Operator.FCmpLE);
			floatCondOpcodeMap.put(Condition.GT, Operator.FCmpGT);
			floatCondOpcodeMap.put(Condition.GE, Operator.FCmpGE);

			doubleCondOpcodeMap.put(Condition.EQ, Operator.DCmpEQ);
			doubleCondOpcodeMap.put(Condition.NE, Operator.DCmpNE);
			doubleCondOpcodeMap.put(Condition.LT, Operator.DCmpLT);
			doubleCondOpcodeMap.put(Condition.LE, Operator.DCmpLT);
			doubleCondOpcodeMap.put(Condition.GT, Operator.DCmpGT);
			doubleCondOpcodeMap.put(Condition.GE, Operator.DCmpGE);
		}

		private Cmp(CiKind kind, Operator opcode, Value left, Value right,
				Condition cond, String name)
		{
			super(kind, opcode, left, right, name);
			this.cond = cond;
		}

		/**
		 * Creates a instance of different subclass served as different
		 * date type according to the date type.
		 *
		 * @param ty    The ret date type.
		 * @param left  The left operand.
		 * @param right the right operand.
		 * @param cond  The condition object.
		 * @return According comparison instruction.
		 */
		public static Cmp instance(Type ty, Value left, Value right,
				Condition cond, String name)
		{
			CiKind kind = HIRGenerator.type2Kind(ty);

			if (ty.isIntLike())
			{
				return new ICmp(kind, intCondOpcodeMap.get(cond), left, right,
						cond, name);
			}
			else if (ty.equals(Type.LONGType))
			{
				return new LCmp(kind, longCondOpcodeMap.get(cond), left, right,
						cond, name);
			}
			else if (ty.equals(Type.FLOATType))
			{
				return new FCmp(kind, floatCondOpcodeMap.get(cond), left, right,
						cond, name);
			}
			else if (ty.equals(Type.DOUBLEType))
			{
				return new DCmp(kind, doubleCondOpcodeMap.get(cond), left,
						right, cond, name);
			}
			else
			{
				throw new SemanticError(
						"Invalid type in creating cmp instruction.");
			}
		}

	}

	public static class ICmp extends Cmp
	{
		public ICmp(CiKind kind, Operator opcode, Value left, Value right,
				Condition cond, String name)
		{
			super(kind, opcode, left, right, cond, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitICmp(this);
		}
	}

	public static class LCmp extends Cmp
	{
		public LCmp(CiKind kind, Operator opcode, Value left, Value right,
				Condition cond, String name)
		{
			super(kind, opcode, left, right, cond, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitLCmp(this);
		}

		@Override public String toString()
		{
			return super.toString();
		}
	}

	public static class FCmp extends Cmp
	{
		public FCmp(CiKind kind, Operator opcode, Value left, Value right,
				Condition cond, String name)
		{
			super(kind, opcode, left, right, cond, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitFCmp(this);
		}
	}

	public static class DCmp extends Cmp
	{
		public DCmp(CiKind kind, Operator opcode, Value left, Value right,
				Condition cond, String name)
		{
			super(kind, opcode, left, right, cond, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitDCmp(this);
		}
	}

	public static final class IfCmp_LT extends IntCmp
	{
		public IfCmp_LT(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget, String name)
		{
			super(Operator.IfLT, x, y, trueTarget, falseTarget, name);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.< --> >=)
		 *
		 * @return
		 */
		public IfCmp_GE getMirror()
		{
			return new IfCmp_GE(y, x, trueTarget, falseTarget, instName);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitIfCmp_LT(this);
		}

		@Override public String toString()
		{
			return null;
		}
	}

	public static final class IfCmp_LE extends IntCmp
	{
		public IfCmp_LE(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget, String name)
		{
			super(Operator.IfLE, x, y, trueTarget, falseTarget, name);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.<= --> >)
		 *
		 * @return
		 */
		public IfCmp_GT getMirror()
		{
			return new IfCmp_GT(y, x, trueTarget, falseTarget, instName);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitIfCmp_LE(this);
		}
	}

	public static final class IfCmp_GT extends IntCmp
	{
		public IfCmp_GT(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget, String name)
		{
			super(Operator.IfGT, x, y, trueTarget, falseTarget, name);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.> --> <=)
		 *
		 * @return
		 */
		public IfCmp_LE getMirror()
		{
			return new IfCmp_LE(y, x, trueTarget, falseTarget, instName);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitIfCmp_GT(this);
		}
	}

	public static class IfCmp_GE extends IntCmp
	{
		public IfCmp_GE(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget, String name)
		{
			super(Operator.IfGE, x, y, trueTarget, falseTarget, name);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.>= --> <)
		 *
		 * @return
		 */
		public IfCmp_LT getMirror()
		{
			return new IfCmp_LT(y, x, trueTarget, falseTarget, instName);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitIfCmp_GE(this);
		}
	}

	public static final class IfCmp_EQ extends IntCmp
	{
		public IfCmp_EQ(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget, String name)
		{
			super(Operator.IfEQ, x, y, trueTarget, falseTarget, name);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.== --> !=)
		 *
		 * @return
		 */
		public IfCmp_NEQ getMirror()
		{
			return new IfCmp_NEQ(x, y, falseTarget, trueTarget, instName);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitIfCmp_EQ(this);
		}
	}

	public static final class IfCmp_NEQ extends IntCmp
	{
		public IfCmp_NEQ(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget, String name)
		{
			super(Operator.IfNE, x, y, trueTarget, falseTarget, name);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.!= --> ==)
		 *
		 * @return
		 */
		public IfCmp_EQ getMirror()
		{
			return new IfCmp_EQ(y, x, falseTarget, trueTarget, instName);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitIfCmp_NEQ(this);
		}

		@Override public String toString()
		{
			return null;
		}
	}

	/**
	 * The {@code Goto} instruction represents the end of a block that
	 * unconditional branches to another basic block.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class Goto extends Branch
	{
		/**
		 * The jump target of this goto instruction.
		 */
		public BasicBlock target;

		/**
		 * Constructs a new {@code Goto} instruction with specified jump target.
		 *
		 * @param target The target block of this unconditional jump.
		 */
		public Goto(BasicBlock target, String name)
		{
			super(CiKind.Illegal, Operator.Goto, name);
			this.target = target;
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitGoto(this);
		}
	}

	/**
	 * This {@code Return} class definition.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class Return extends Instruction
	{
		Value ret;

		/**
		 * Constructs a new return instruction with return inst.
		 *
		 * @param retValue The return inst produce for this instruction, return
		 *                 void if ret is {@code null}.
		 */
		public Return(Value retValue, String name)
		{
			super(retValue == null ? CiKind.Void : retValue.kind, Operator.Ret,
					name);
			this.ret = retValue;
			if (ret != null)
				use(ret, this);
		}

		/**
		 * Gets the instruction that produces the ret for the return.
		 *
		 * @return the instruction producing the ret
		 */
		public Value result()
		{
			return ret;
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitReturn(this);
		}
	}

	/**
	 * Method invocation instruction.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class Invoke extends Instruction
	{
		/**
		 * The input arguments for function calling.
		 */
		public final Value[] arguments;
		/**
		 * The target of this method calling.
		 */
		public final Method target;

		public Value ret;

		/**
		 * Constructs a new method calling instruction.
		 *
		 * @param result The kind of return ret.
		 * @param args   The input arguments.
		 * @param target The called method.
		 */
		public Invoke(CiKind result, Value[] args, Method target, String name)
		{
			super(result, Operator.Invoke, name);
			this.target = target;
			this.arguments = args;
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitInvoke(this);
		}

		@Override public String toString()
		{
			return null;
		}
	}

	/**
	 * The {@code Phi} instruction represents the merging of data flow in the
	 * instruction graph. It refers to a join block and a variable.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class Phi extends Instruction
	{
		/**
		 * The derived basic block from owned inputed parameter of this Phi
		 * assignment.
		 * <p>
		 * The parameters list of Phi assignment.
		 */
		private Pair<Value, BasicBlock>[] inputs;

		private int currIndex = 0;

		/**
		 * Constructs a new Phi-function instruction.
		 *
		 * @param kind   The kind of ret.
		 * @param args   The input arguments.
		 * @param blocks The one of which basic block array is corresponding to
		 *               an input argument.
		 */
		public Phi(CiKind kind, Value[] args, BasicBlock[] blocks, String name)
		{
			super(kind, Operator.Phi, name);
			assert args.length == blocks.length;
			inputs = new Pair[args.length];
			for (int idx = 0; idx < args.length; idx++)
			{
				inputs[idx] = new Pair<>(args[idx], blocks[idx]);
				// make a chain between value definition and value use
				use(args[idx], this);
			}
		}

		public Phi(CiKind kind, int length)
		{
			super(kind, Operator.Phi, "");
			this.inputs = new Pair[length];
		}

		public Phi(CiKind kind, int length, String nameString)
		{
			super(kind, Operator.Phi, nameString);
			this.inputs = new Pair[length];
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitPhi(this);
		}

		/**
		 * Appends a pair that consists of both value and block into argument list.
		 *
		 * @param value The instruction that phi parameter to be inserted
		 * @param block The according block of corresponding phi parameter.
		 */
		public void addIncoming(Value value, BasicBlock block)
		{
			if (value != null && block != null && currIndex < inputs.length)
			{
				this.inputs[currIndex++] = new Pair<>(value, block);
			}
		}

		/**
		 * Gets the inputed parameter at given position.
		 *
		 * @param index The position where input parameter will be obtained.
		 * @return The input parameter at specified position.
		 */
		public Value getIncomingValue(int index)
		{
			assert index >= 0 && index
					< inputs.length : "The index is beyond out the size of list";
			return inputs[index].fst;
		}

		/**
		 * Gets the input block at given position.
		 *
		 * @param index The position where input block will be obtained.
		 * @return The input block at specified position.
		 */
		public BasicBlock getBasicBlock(int index)
		{
			assert index >= 0 && index
					< inputs.length : "The index is beyond out the size of list";
			return inputs[index].snd;
		}

		/**
		 * Updates the input argument at given position.
		 *
		 * @param index The index into argument to be updated.
		 * @param value
		 */
		public void setParameter(int index, Value value)
		{
			assert index >= 0 && index
					< inputs.length : "The index is beyond out the size of list";

			inputs[index].fst = value;
		}

		/**
		 * Updates the input block at given position.
		 *
		 * @param index The index into block to be updated.
		 * @param block
		 */
		public void setBasicBlock(int index, BasicBlock block)
		{
			assert index >= 0 && index
					< inputs.length : "The index is beyond out the size of list";

			inputs[index].snd = block;
		}

		@Override public String toString()
		{
			return null;
		}

		public int getBasicBlockIndex(BasicBlock basicBlock)
		{
			assert (basicBlock
					!= null) : "Phi.getBasicBlockIndex(<null>) is invalid";
			for (int idx = 0; idx < inputs.length; idx++)
				if (inputs[idx].snd == basicBlock)
					return idx;
			return -1;
		}

		/**
		 * Obtains the numbers of incoming value of phi node.
		 *
		 * @return
		 */
		public int getNumberIncomingValues()
		{
			return currIndex;
		}

		/**
		 * Gets an array that contains all incoming basic blocks.
		 *
		 * @return
		 */
		public BasicBlock[] getAllBasicBlocks()
		{
			BasicBlock[] blocks = new BasicBlock[inputs.length];
			for (int idx = 0; idx < inputs.length; idx++)
				blocks[idx] = inputs[idx].snd;
			return blocks;
		}

		/**
		 * Gets the name of this phi node.
		 *
		 * @return
		 */
		public String getName()
		{
			return instName;
		}
	}

	/**
	 * This class implements allocating memory at stack frame of current
	 * executed function.
	 */
	public static class Alloca extends Instruction
	{
		public Alloca(CiKind kind, String name)
		{
			super(kind, Operator.Alloca, name);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitAlloca(this);
		}

		/**
		 * Gets the name of this alloated variable.
		 *
		 * @return
		 */
		public String getName()
		{
			return instName;
		}

		/**
		 * Determine whether this alloca instruction is promoted into
		 * register or not?
		 *
		 * @return Return true if it is pormotable.
		 */
		public boolean isAllocaPromoteable()
		{
			return true;
		}
	}

	/**
	 * An instruction for writing data into memory.
	 */
	public static class StoreInst extends Instruction
	{
		/**
		 * The inst being writed into destination variable.
		 */
		public Value value;
		/**
		 * The target of writing.
		 */
		public Alloca dest;

		/**
		 * Constructs a new store instruction.
		 *
		 * @param value The inst to being writed into memory.
		 * @param dest  The target memory address where inst stores.
		 */
		public StoreInst(Value value, Alloca dest, String name)
		{
			super(CiKind.Illegal, Operator.Store, name);
			this.value = value;
			this.dest = dest;

			use(value, this);
			use(dest, this);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitStoreInst(this);
		}
	}

	/**
	 * An instruction for reading data from memory.
	 */
	public static class LoadInst extends Instruction
	{
		/**
		 * The source memory where desired inst reading.
		 */
		public Alloca from;

		public LoadInst(CiKind kind, Alloca from, String name)
		{
			super(kind, Operator.Load, name);
			this.from = from;

			if (from != null)
				use(from, this);
		}

		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitLoadInst(this);
		}
	}

	public static class SwitchInst extends Branch
	{
		private Pair<Value, BasicBlock>[] operands;
		private int currIdx = 0;

		/**
		 * Constructs a new SwitchInst instruction with specified inst type.
		 *
		 * @param condV     the value of selector.
		 * @param defaultBB The default jump block when no other case match.
		 * @param numCases  The numbers of case value.
		 */
		public SwitchInst(Value condV, BasicBlock defaultBB, int numCases,
				String name)
		{
			super(CiKind.Illegal, Operator.Switch, name);
			operands = new Pair[1 + numCases];
			operands[currIdx++] = new Pair<>(condV, defaultBB);
		}

		/**
		 * An interface for ValueVisitor invoking.
		 *
		 * @param visitor The instance of ValueVisitor.
		 */
		@Override public void accept(ValueVisitor visitor)
		{
			visitor.visitSwitch(this);
		}

		public void addCase(Value caseVal, BasicBlock targetBB)
		{
			operands[currIdx++] = new Pair<>(caseVal, targetBB);
		}

		/**
		 * Gets the default basic block where default case clause resides.
		 *
		 * @return The default basic block.
		 */
		public BasicBlock getDefaultBlock()
		{
			return this.operands[0].snd;
		}
	}
}
