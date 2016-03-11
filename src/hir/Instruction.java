package hir;

import ci.CiKind;
import comp.OpCodes;
import exception.SemanticError;
import type.Type;
import utils.Pair;
import utils.Utils;

/**
 * This class is an abstract representation of Quadruple. In this class,
 * subclass of @ {@code Instruction} represents arithmetic and logical
 * operation, control flow operators,Phi assignment, function calling
 * conditional statement.
 *
 * @author Jianping Zeng
 * @version `1.0
 * @see BasicBlock
 */
public abstract class Instruction extends Value
{
	/**
	 * Mainly for register allocation.
	 */
	public int id;

	/**
	 * The ret of operation over this instruction, it is null if this instruction
	 * no return ret.
	 */
	//public Instruction ret;

	private BasicBlock bb;

	/**
	 * A link to next instruction at the basic block or to itself if it not on
	 * block.
	 */
	private Instruction next = this;

	public BasicBlock getParent() {return bb;}
	public void setParent(BasicBlock bb) {this.bb = bb;}

	public Instruction(CiKind kind)
	{
		super(kind);
		this.id = -1;
	}

	/**
	 * Erases this instruction from it's parent basic block.
	 */
	public void eraseFromBasicBlock()
	{
		assert (this.bb == null) :
				"The basic block where the instruction reside to be erased!";
		bb.removeInst(this);
	}

	/**
	 * An interface for InstructionVisitor invoking.
	 *
	 * @param visitor The instance of InstructionVisitor.
	 */
	public abstract void accept(InstructionVisitor visitor);

	/**
	 * Gets the text format of this Instruction.
	 *
	 * @return
	 */
	public String toString()
	{
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
	 * @param inst  An instruction to be inserted.
	 */
	public void insertAfter(Instruction inst)
	{
		int index = bb.lastIndexOf(inst);
		if (index >= 0 && index < bb.size())
			bb.addInst(inst, index + 1);
	}
	/**
	 * Inserts an instruction into the instructions list before this itself.
	 * @param inst  An instruction to be inserted.
	 */
	public void insertBefore(Instruction inst)
	{
		int index = bb.lastIndexOf(inst);
		if (index >= 0 && index < bb.size())
			bb.addInst(inst, index);
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
		 * @param kind The inst kind of ret.
		 * @param x    The sole operand.
		 */
		public Op1(CiKind kind, Value x)
		{
			super(kind);
			this.x = x;
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

		public Op2(CiKind kind, Value x, Value y)
		{
			super(kind);
			this.x = x;
			this.y = y;
		}

	}

	public static class ADD_I extends Op2
	{
		public ADD_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitADD_I(this);
		}

		@Override public String toString()
		{
			return "ADD_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.iadd, x, y);
		}
	}

	public static class SUB_I extends Op2
	{
		public SUB_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitSUB_I(this);
		}

		@Override public String toString()
		{
			return "SUB_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.isub, x, y);
		}
	}

	public static class MUL_I extends Op2
	{
		public MUL_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitMUL_I(this);
		}

		@Override public String toString()
		{
			return "MUL_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.imul, x, y);
		}
	}

	public static class DIV_I extends Op2
	{
		public DIV_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitDIV_I(this);
		}

		@Override public String toString()
		{
			return "DIV_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.idiv, x, y);
		}
	}

	public static class MOD_I extends Op2
	{
		public MOD_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitMOD_I(this);
		}

		@Override public String toString()
		{
			return "MOD_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.imod, x, y);
		}
	}

	public static class AND_I extends Op2
	{
		public AND_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitAND_I(this);
		}

		@Override public String toString()
		{
			return "AND_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.iand, x, y);
		}
	}

	public static class OR_I extends Op2
	{
		public OR_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitOR_I(this);
		}

		@Override public String toString()
		{
			return "OR_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.ior, x, y);
		}
	}

	public static class XOR_I extends Op2
	{
		public XOR_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitXOR_I(this);
		}

		@Override public String toString()
		{
			return "XOR_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.ixor, x, y);
		}
	}

	public static class SHL_I extends Op2
	{
		public SHL_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitSHL_I(this);
		}

		@Override public String toString()
		{
			return "SHL_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.ishl, x, y);
		}
	}

	public static class SHR_I extends Op2
	{
		public SHR_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitSHR_I(this);
		}

		@Override public String toString()
		{
			return "SHR_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.ishr, x, y);
		}
	}

	/**
	 * This class served as unsigned sheft right over integer operand.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class USHR_I extends Op2
	{
		public USHR_I(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitUSHR_I(this);
		}

		@Override public String toString()
		{
			return "USHR_I";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.iushr, x, y);
		}
	}

	public static class ADD_L extends Op2
	{
		public ADD_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitADD_L(this);
		}

		@Override public String toString()
		{
			return "ADD_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.ladd, x, y);
		}
	}

	public static class SUB_L extends Op2
	{
		public SUB_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitSUB_L(this);
		}

		@Override public String toString()
		{
			return "SUB_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.lsub, x, y);
		}
	}

	public static class MUL_L extends Op2
	{
		public MUL_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitMUL_L(this);
		}

		@Override public String toString()
		{
			return "MUL_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.lmul, x, y);
		}
	}

	public static class DIV_L extends Op2
	{
		public DIV_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitDIV_L(this);
		}

		@Override public String toString()
		{
			return "DIV_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.ldiv, x, y);
		}
	}

	public static class MOD_L extends Op2
	{
		public MOD_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitMOD_L(this);
		}

		@Override public String toString()
		{
			return "MOD_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.lmod, x, y);
		}
	}

	public static class AND_L extends Op2
	{
		public AND_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitAND_L(this);
		}

		@Override public String toString()
		{
			return "AND_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.land, x, y);
		}
	}

	public static class OR_L extends Op2
	{
		public OR_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitOR_L(this);
		}

		@Override public String toString()
		{
			return "OR_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.lor, x, y);
		}
	}

	public static class XOR_L extends Op2
	{
		public XOR_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitXOR_L(this);
		}

		@Override public String toString()
		{
			return "XOR_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.lxor, x, y);
		}
	}

	public static class SHL_L extends Op2
	{
		public SHL_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitSHL_L(this);
		}

		@Override public String toString()
		{
			return "SHL_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.lshl, x, y);
		}
	}

	public static class SHR_L extends Op2
	{
		public SHR_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitSHR_L(this);
		}

		@Override public String toString()
		{
			return "SHR_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.lshr, x, y);
		}
	}

	/**
	 * This class served as unsigned sheft right over long integer operand.
	 *
	 * @author Jianping Zeng <z1215jping@hotmail.com>
	 */
	public static class USHR_L extends Op2
	{
		public USHR_L(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitUSHR_L(this);
		}

		@Override public String toString()
		{
			return "USHR_L";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.lushr, x, y);
		}
	}

	public static class ADD_F extends Op2
	{
		public ADD_F(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitADD_F(this);
		}

		@Override public String toString()
		{
			return "ADD_F";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.fadd, x, y);
		}
	}

	public static class SUB_F extends Op2
	{
		public SUB_F(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitSUB_F(this);
		}

		@Override public String toString()
		{
			return "SUB_F";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.fsub, x, y);
		}
	}

	public static class MUL_F extends Op2
	{
		public MUL_F(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitMUL_F(this);
		}

		@Override public String toString()
		{
			return "MUL_F";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.fmul, x, y);
		}
	}

	public static class DIV_F extends Op2
	{
		public DIV_F(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitDIV_F(this);
		}

		@Override public String toString()
		{
			return "DIV_F";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.fdiv, x, y);
		}
	}

	public static class MOD_F extends Op2
	{
		public MOD_F(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitMOD_F(this);
		}

		@Override public String toString()
		{
			return "MOD_F";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.fmod, x, y);
		}
	}

	public static class ADD_D extends Op2
	{
		public ADD_D(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitADD_D(this);
		}

		@Override public String toString()
		{
			return "ADD_D";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.dadd, x, y);
		}
	}

	public static class SUB_D extends Op2
	{
		public SUB_D(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitSUB_D(this);
		}

		@Override public String toString()
		{
			return "SUB_D";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.dsub, x, y);
		}
	}

	public static class MUL_D extends Op2
	{
		public MUL_D(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitMUL_D(this);
		}

		@Override public String toString()
		{
			return "MUL_D";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.dmul, x, y);
		}
	}

	public static class DIV_D extends Op2
	{
		public DIV_D(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitDIV_D(this);
		}

		@Override public String toString()
		{
			return "DIV_D";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.ddiv, x, y);
		}
	}

	public static class MOD_D extends Op2
	{
		public MOD_D(CiKind kind, Value x, Value y)
		{
			super(kind, x, y);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitMOD_D(this);
		}

		@Override public String toString()
		{
			return "MOD_D";
		}

		@Override public int valueNumber()
		{
			return Utils.hash2(OpCodes.dmod, x, y);
		}
	}

	public static class NEG_I extends Op1
	{
		public NEG_I(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "NEG_I";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitNEG_I(this);
		}

		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.ineg, x);
		}
	}

	public static class NEG_F extends Op1
	{
		public NEG_F(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "NEG_F";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitNEG_F(this);
		}

		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.fneg, x);
		}
	}

	public static class NEG_L extends Op1
	{
		public NEG_L(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "NEG_L";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitNEG_L(this);
		}

		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.lneg, x);
		}
	}

	public static class NEG_D extends Op1
	{

		public NEG_D(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "NEG_D";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitNEG_D(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.dneg, x);
		}
	}

	public static class INT_2LONG extends Op1
	{

		public INT_2LONG(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2LONG";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitINT_2LONG(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.i2l, x);
		}
	}

	public static class INT_2FLOAT extends Op1
	{
		public INT_2FLOAT(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2FLOAT";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitINT_2FLOAT(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.i2f, x);
		}
	}

	public static class INT_2DOUBLE extends Op1
	{

		public INT_2DOUBLE(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2DOUBLE";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitINT_2DOUBLE(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.i2d, x);
		}
	}

	public static class LONG_2INT extends Op1
	{

		public LONG_2INT(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "LONG_2INT";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitLONG_2INT(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.l2i, x);
		}
	}

	public static class LONG_2FLOAT extends Op1
	{
		public LONG_2FLOAT(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "LONG_2FLOAT";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitLONG_2FLOAT(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.l2f, x);
		}
	}

	public static class LONG_2DOUBLE extends Op1
	{

		public LONG_2DOUBLE(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "LONG_2DOUBLE";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitLONG_2DOUBLE(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.l2d, x);
		}
	}

	public static class FLOAT_2INT extends Op1
	{
		public FLOAT_2INT(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "FLOAT_2INT";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitFLOAT_2INT(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.f2i, x);
		}
	}

	public static class FLOAT_2LONG extends Op1
	{
		public FLOAT_2LONG(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "FLOAT_2LONG";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitFLOAT_2LONG(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.f2l, x);
		}
	}

	public static class FLOAT_2DOUBLE extends Op1
	{

		public FLOAT_2DOUBLE(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "FLOAT_2DOUBLE";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitFLOAT_2DOUBLE(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.f2d, x);
		}
	}

	public static class DOUBLE_2INT extends Op1
	{

		public DOUBLE_2INT(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "DOUBLE_2INT";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitDOUBLE_2INT(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.d2i, x);
		}
	}

	public static class DOUBLE_2LONG extends Op1
	{

		public DOUBLE_2LONG(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "DOUBLE_2LONG";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitDOUBLE_2LONG(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.d2l, x);
		}
	}

	public static class DOUBLE_2FLOAT extends Op1
	{

		public DOUBLE_2FLOAT(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "DOUBLE_2FLOAT";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitDOUBLE_2FLOAT(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.d2f, x);
		}
	}

	public static class INT_2BYTE extends Op1
	{

		public INT_2BYTE(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2BYTE";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitINT_2BYTE(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.int2byte, x);
		}
	}

	public static class INT_2CHAR extends Op1
	{

		public INT_2CHAR(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2CHAR";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitINT_2CHAR(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.int2char, x);
		}
	}

	public static class INT_2SHORT extends Op1
	{

		public INT_2SHORT(CiKind kind, Value x)
		{
			super(kind, x);
		}

		public String toString()
		{
			return "INT_2SHORT";
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitINT_2SHORT(this);
		}
		@Override public int valueNumber()
		{
			return Utils.hash1(OpCodes.int2short, x);
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
		public Branch(CiKind kind)
		{
			super(kind);
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
		public ConditionalBranch(CiKind kind)
		{
			super(kind);
		}
	}

	public static class BR extends ConditionalBranch
	{
		Value x;

		public BR(Value x, BasicBlock trueTarget, BasicBlock falseTarget)
		{
			super(CiKind.Illegal);
			this.x = x;
			this.trueTarget = trueTarget;
			this.falseTarget = falseTarget;
		}

		/**
		 * An interface for InstructionVisitor invoking.
		 *
		 * @param visitor The instance of InstructionVisitor.
		 */
		@Override public void accept(InstructionVisitor visitor)
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

		IntCmp(Value x, Value y, BasicBlock trueTarget,
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
	}
	
	public static abstract class Cmp extends Op2
	{
		Condition cond;
		private Cmp(CiKind kind, Value left, Value right, Condition cond)
        {
			super(kind, left, right);
			this.cond = cond;
        }
		
		/**
		 * Creates a instance of different subclass served as different date type according 
		 * to the date type.  
		 * @param ty	The ret date type.
		 * @param left	The left operand.
		 * @param right	the right operand.
		 * @param cond	The condition object.
		 * @return	According comparison instruction.
		 */
		public static Cmp instance(Type ty, Value left,
				Value right, Condition cond)
		{
			CiKind kind = HIRGenerator.type2Kind(ty);
			if (ty.isIntLike())
			{
				return new ICmp(kind, left, right, cond);
			}
			else if (ty.equals(Type.LONGType))
			{
				return new LCmp(kind, left, right, cond);
			}
			else if (ty.equals(Type.FLOATType))
			{
				return new FCmp(kind, left, right, cond);
			}
			else if (ty.equals(Type.DOUBLEType))
			{
				return new DCmp(kind, left, right, cond);
			}
			else 
			{
				throw new SemanticError("Invalid type in creating cmp instruction.");
			}
		}
		
	}
	
	public static class ICmp extends Cmp
	{
		public ICmp(CiKind kind, Value left, Value right, Condition cond)
        {
			super(kind, left, right, cond);
        }

		@Override
        public void accept(InstructionVisitor visitor)
        {
	        visitor.visitICmp(this);
        }
		
		@Override
		public String toString()
		{	
		    return super.toString();
		}
	}

	public static class LCmp extends Cmp
	{
		public LCmp(CiKind kind, Value left, Value right, Condition cond)
        {
			super(kind, left, right, cond);
        }

		@Override
        public void accept(InstructionVisitor visitor)
        {
	        visitor.visitLCmp(this);
        }
		
		@Override
		public String toString()
		{	
		    return super.toString();
		}
	}
	
	public static class FCmp extends Cmp
	{
		public FCmp(CiKind kind, Value left, Value right, Condition cond)
        {
			super(kind, left, right, cond);
        }

		@Override
        public void accept(InstructionVisitor visitor)
        {
	        visitor.visitFCmp(this);
        }
		
		@Override
		public String toString()
		{	
		    return super.toString();
		}
	}
	
	public static class DCmp extends Cmp
	{
		public DCmp(CiKind kind, Value left, Value right, Condition cond)
        {
			super(kind, left, right, cond);
        }

		@Override
        public void accept(InstructionVisitor visitor)
        {
	        visitor.visitDCmp(this);
        }
		
		@Override
		public String toString()
		{	
		    return super.toString();
		}
	}
	
	public static final class IfCmp_LT extends IntCmp
	{
		public IfCmp_LT(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.< --> >=)
		 *
		 * @return
		 */
		public IfCmp_GE getMirror()
		{
			return new IfCmp_GE(y, x, trueTarget, falseTarget);
		}

		@Override public void accept(InstructionVisitor visitor)
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
				BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.<= --> >)
		 *
		 * @return
		 */
		public IfCmp_GT getMirror()
		{
			return new IfCmp_GT(y, x, trueTarget, falseTarget);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitIfCmp_LE(this);
		}

		@Override public String toString()
		{
			return null;
		}
	}

	public static final class IfCmp_GT extends IntCmp
	{
		public IfCmp_GT(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.> --> <=)
		 *
		 * @return
		 */
		public IfCmp_LE getMirror()
		{
			return new IfCmp_LE(y, x, trueTarget, falseTarget);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitIfCmp_GT(this);
		}

		@Override public String toString()
		{
			return null;
		}
	}

	public static class IfCmp_GE extends IntCmp
	{
		public IfCmp_GE(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.>= --> <)
		 *
		 * @return
		 */
		public IfCmp_LT getMirror()
		{
			return new IfCmp_LT(y, x, trueTarget, falseTarget);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitIfCmp_GE(this);
		}

		@Override public String toString()
		{
			return null;
		}
	}

	public static final class IfCmp_EQ extends IntCmp
	{
		public IfCmp_EQ(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.== --> !=)
		 *
		 * @return
		 */
		public IfCmp_NEQ getMirror()
		{
			return new IfCmp_NEQ(x, y, falseTarget, trueTarget);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitIfCmp_EQ(this);
		}

		@Override public String toString()
		{
			return null;
		}
	}

	public static final class IfCmp_NEQ extends IntCmp
	{
		public IfCmp_NEQ(Value x, Value y, BasicBlock trueTarget,
				BasicBlock falseTarget)
		{
			super(x, y, trueTarget, falseTarget);
		}

		/**
		 * Swaps the operand and reverse the condition (e.g.!= --> ==)
		 *
		 * @return
		 */
		public IfCmp_EQ getMirror()
		{
			return new IfCmp_EQ(y, x, falseTarget, trueTarget);
		}

		@Override public void accept(InstructionVisitor visitor)
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
		public Goto(BasicBlock target)
		{
			super(CiKind.Illegal);
			this.target = target;
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitGoto(this);
		}

		@Override public String toString()
		{
			return null;
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
		 *               void if ret is {@code null}.
		 */
		public Return(Value retValue)
		{
			super(retValue == null ? CiKind.Void : retValue.kind);
			this.ret = retValue;
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

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitReturn(this);
		}

		@Override public String toString()
		{
			return null;
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
		 * @param result     The kind of return ret.
		 * @param args       The input arguments.
		 * @param target     The called method.
		 */
		public Invoke(CiKind result, Value[] args, Method target)
		{
			super(result);
			this.target = target;
			this.arguments = args;
		}

		@Override public void accept(InstructionVisitor visitor)
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
		 * 
		 * The parameters list of Phi assignment.
		 */	
		private Pair<Value, BasicBlock>[] inputs;
		
		private int currIndex = 0;

		private String nameString;

		/**
		 * Constructs a new Phi-function instruction.
		 *
		 * @param kind   The kind of ret.
		 * @param args   The input arguments.
		 * @param blocks The one of which basic block array is corresponding to
		 *               an input argument.
		 */
		public Phi(CiKind kind, Value[] args,
				BasicBlock[] blocks)
		{
			super(kind);
			assert args.length == blocks.length;
			inputs = new Pair[args.length];
			for (int idx = 0; idx < args.length; idx++)
				inputs[idx] = new Pair<>(args[idx], blocks[idx]);
		}
		
		public Phi(CiKind kind, int length)
        {
	        super(kind);
	        this.inputs = new Pair[length];
	        this.nameString = "";
        }

		public Phi(CiKind kind, int length, String nameString)
		{
			super(kind);
			this.inputs = new Pair[length];
			this.nameString = nameString;
		}

		/**
		 * Gets the name of this phi node.
		 * @return
		 */
		public String getName() {return nameString;}

		@Override 
		public void accept(InstructionVisitor visitor)
		{
			visitor.visitPhi(this);
		}

		/**
		 * Appends a pair that consists of both value and block into argument list.
		 * @param value	The instruction that phi parameter to be inserted
		 * @param block	The according block of corresponding phi parameter.
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
		 * @param index	The position where input parameter will be obtained.
		 * @return	The input parameter at specified position.
		 */
		public Value getParameter(int index)
		{
			assert index >= 0 && index < inputs.length 
					: "The index is beyond out the size of list";
			return inputs[index].fst;
		}
		/**
		 * Gets the input block at given position.
		 * @param index	The position where input block will be obtained.
		 * @return	The input block at specified position.
		 */
		public BasicBlock getBasicBlock(int index)
		{
			assert index >= 0 && index < inputs.length
					: "The index is beyond out the size of list";
			return inputs[index].snd;
		}
		
		/**
		 * Updates the input argument at given position.
		 * @param index	The index into argument to be updated.
		 * @param value	
		 */
		public void setParameter(int index, Value value)
		{
			assert index >= 0 && index < inputs.length
					: "The index is beyond out the size of list";

			inputs[index].fst = value;
		}
		
		/**
		 * Updates the input block at given position.
		 * @param index	The index into block to be updated.
		 * @param block
		 */
		public void setBasicBlock(int index, BasicBlock block)
		{
			assert index >= 0 && index < inputs.length
					: "The index is beyond out the size of list";

			inputs[index].snd = block;
		}
		
		@Override public String toString()
		{
			return null;
		}

		public int getBasicBlockIndex(BasicBlock basicBlock)
		{
			assert  (basicBlock != null) :
					"Phi.getBasicBlockIndex(<null>) is invalid";
			for (int idx = 0; idx < inputs.length;idx++)
				if (inputs[idx].snd == basicBlock)
					return idx;
			return -1;
		}

		/**
		 * Obtains the numbers of incoming value of phi node.
		 * @return
		 */
		public int getNumberIncomingValues()
		{
			return currIndex;
		}

		/**
		 * Gets an array that contains all incoming basic blocks.
		 * @return
		 */
		public BasicBlock[] getAllBasicBlocks()
		{
			BasicBlock[] blocks = new BasicBlock[inputs.length];
			for (int idx = 0; idx < inputs.length; idx++)
				blocks[idx] = inputs[idx].snd;
			return blocks;
		}
	}



	/**
	 * This class implements allocating memory at stack frame of current
	 * executed function.
	 */
	public static class Alloca extends Instruction
	{
		private String nameString;
		public Alloca(CiKind kind)
		{
			super(kind);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitAlloca(this);
		}

		/**
		 * Gets the name of this alloated variable.
		 * @return
		 */
		public String getName() {return  nameString;}

		/**
		 * Determine whether this alloca instruction is promoted into
		 * register or not?
		 * @return  Return true if it is pormotable.
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
		public StoreInst(Value value, Alloca dest)
		{
			super(CiKind.Illegal);
			this.value = value;
			this.dest = dest;
		}

		@Override public void accept(InstructionVisitor visitor)
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

		public LoadInst(CiKind kind, Alloca from)
		{
			super(kind);
			this.from = from;
		}

		@Override
		public void accept(InstructionVisitor visitor)
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
		 * @param condV  the value of selector.
		 * @param defaultBB The default jump block when no other case match.
		 * @param numCases  The numbers of case value.
		 */
		public SwitchInst(Value condV, BasicBlock defaultBB, int numCases)
		{
			super(CiKind.Illegal);
			operands = new Pair[1 + numCases];
			operands[currIdx++] = new Pair<>(condV, defaultBB);
		}

		/**
		 * An interface for InstructionVisitor invoking.
		 *
		 * @param visitor The instance of InstructionVisitor.
		 */
		@Override
		public void accept(InstructionVisitor visitor)
		{

		}

		public void addCase(Value caseVal, BasicBlock targetBB)
		{
			operands[currIdx++] = new Pair<>(caseVal, targetBB);
		}

		/**
		 * Gets the default basic block where default case clause resides.
		 * @return  The default basic block.
		 */
		public BasicBlock getDefaultBlock()
		{
			return this.operands[0].snd;
		}
	}
}
