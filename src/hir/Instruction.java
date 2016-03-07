package hir;

import ci.CiConstant;
import ci.CiKind;
import comp.OpCodes;
import exception.SemanticError;
import type.Type;
import utils.Name;
import utils.Pair;
import utils.Utils;
import java.util.ArrayList;

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
public abstract class Instruction implements Cloneable
{
	/**
	 * The list of all instruction which use this instruction.
	 */
	public ArrayList<Instruction> uses;
	/**
	 * Mainly for register allocation.
	 */
	public int id;
	/**
	 * The type of inst produced with this instruction. The kind is
	 * {@linkplain CiKind#Void} if this instruction produce no inst.
	 */
	public CiKind kind;

	/**
	 * The result of operation over this instruction, it is null if this instruction
	 * no return result.
	 */
	//public Instruction result;

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
	 * Value numbering for entering SSA form.
	 */
	public int number;

	private BasicBlock bb;

	public BasicBlock getParent() {return bb;}
	public void setParent(BasicBlock bb) {this.bb = bb;}

	public Instruction(CiKind kind)
	{
		this.kind = kind;
		this.id = -1;
		this.uses = new ArrayList<>(8);
	}

	/**
	 * An interface for InstructionVisitor invoking.
	 *
	 * @param visitor The instance of InstructionVisitor.
	 */
	public abstract void accept(InstructionVisitor visitor);

	/**
	 * Erases this instruction from it's parent basic block.
	 */
	public void eraseFromBasicBlock()
	{
		assert (this.bb == null) : "The basic block where the instruction reside to be erased!";
		bb.removeInst(this);
	}

	/**
	 * Gets the text format of this Instruction.
	 *
	 * @return
	 */
	public String toString()
	{
		return "";
	}

	@Override public Instruction clone() throws CloneNotSupportedException
	{
		super.clone();
		throw new CloneNotSupportedException();
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
	 * Go through the uses list for this definition and make each use point
	 * to "value" of "this". After this completes, this's uses list is empty.
	 * @param value
	 */
	public void replaceAllUsesWith(Instruction value)
	{
		assert value != null : "Instruction.replaceAllusesWith(<null>) is invalid.";
		assert kind == value.kind :
				"replaceAllUses of value with new value of different tyep";

		// 更新use-def链中的使用分量
		// 暂时未完成

		BasicBlock BB = value.getParent();
		for (BasicBlock succ : BB.getSuccs())
		{
			for(Instruction inst : succ)
			{
				if (!(inst instanceof Phi))
					break;
				int i;
				Phi PN = (Phi)inst;
				if ((i = PN.getBasicBlockIndex(BB)) >= 0)
					PN.setParameter(i, value);
			}
		}
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
		 *
		 * @param kind The inst kind of result.
		 * @param x    The sole operand.
		 */
		public Op1(CiKind kind, Instruction x)
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
		public SUB_I(CiKind kind, Instruction x, Instruction y)
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
		public MUL_I(CiKind kind, Instruction x, Instruction y)
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
		public DIV_I(CiKind kind, Instruction x, Instruction y)
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
		public MOD_I(CiKind kind, Instruction x, Instruction y)
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
		public AND_I(CiKind kind, Instruction x, Instruction y)
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
		public OR_I(CiKind kind, Instruction x, Instruction y)
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
		public XOR_I(CiKind kind, Instruction x, Instruction y)
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
		public SHL_I(CiKind kind, Instruction x, Instruction y)
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
		public SHR_I(CiKind kind, Instruction x, Instruction y)
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
		public USHR_I(CiKind kind, Instruction x, Instruction y)
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
		public ADD_L(CiKind kind, Instruction x, Instruction y)
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
		public SUB_L(CiKind kind, Instruction x, Instruction y)
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
		public MUL_L(CiKind kind, Instruction x, Instruction y)
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
		public DIV_L(CiKind kind, Instruction x, Instruction y)
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
		public MOD_L(CiKind kind, Instruction x, Instruction y)
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
		public AND_L(CiKind kind, Instruction x, Instruction y)
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
		public OR_L(CiKind kind, Instruction x, Instruction y)
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
		public XOR_L(CiKind kind, Instruction x, Instruction y)
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
		public SHL_L(CiKind kind, Instruction x, Instruction y)
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
		public SHR_L(CiKind kind, Instruction x, Instruction y)
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
		public USHR_L(CiKind kind, Instruction x, Instruction y)
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
		public ADD_F(CiKind kind, Instruction x, Instruction y)
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
		public SUB_F(CiKind kind, Instruction x, Instruction y)
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
		public MUL_F(CiKind kind, Instruction x, Instruction y)
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
		public DIV_F(CiKind kind, Instruction x, Instruction y)
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
		public MOD_F(CiKind kind, Instruction x, Instruction y)
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
		public ADD_D(CiKind kind, Instruction x, Instruction y)
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
		public SUB_D(CiKind kind, Instruction x, Instruction y)
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
		public MUL_D(CiKind kind, Instruction x, Instruction y)
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
		public DIV_D(CiKind kind, Instruction x, Instruction y)
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
		public MOD_D(CiKind kind, Instruction x, Instruction y)
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
		public NEG_I(CiKind kind, Instruction x)
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
		public NEG_F(CiKind kind, Instruction x)
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
		public NEG_L(CiKind kind, Instruction x)
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

		public NEG_D(CiKind kind, Instruction x)
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

		public INT_2LONG(CiKind kind, Instruction x)
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
		public INT_2FLOAT(CiKind kind, Instruction x)
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

		public INT_2DOUBLE(CiKind kind, Instruction x)
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

		public LONG_2INT(CiKind kind, Instruction x)
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
		public LONG_2FLOAT(CiKind kind, Instruction x)
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

		public LONG_2DOUBLE(CiKind kind, Instruction x)
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
		public FLOAT_2INT(CiKind kind, Instruction x)
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
		public FLOAT_2LONG(CiKind kind, Instruction x)
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

		public FLOAT_2DOUBLE(CiKind kind, Instruction x)
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

		public DOUBLE_2INT(CiKind kind, Instruction x)
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

		public DOUBLE_2LONG(CiKind kind, Instruction x)
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

		public DOUBLE_2FLOAT(CiKind kind, Instruction x)
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

		public INT_2BYTE(CiKind kind, Instruction x)
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

		public INT_2CHAR(CiKind kind, Instruction x)
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

		public INT_2SHORT(CiKind kind, Instruction x)
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

	public static class BR extends Branch
	{
		Instruction x;
		BasicBlock trueTarget, falseTarget;

		public BR(Instruction x, BasicBlock trueTarget, BasicBlock falseTarget)
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
		 *
		 * @return
		 */
		public Instruction x()
		{
			return x;
		}

		/**
		 * The second operand.
		 *
		 * @return
		 */
		public Instruction y()
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
		private Cmp(CiKind kind, Instruction left, Instruction right, Condition cond)
        {
			super(kind, left, right);
			this.cond = cond;
        }
		
		/**
		 * Creates a instance of different subclass served as different date type according 
		 * to the date type.  
		 * @param ty	The result date type.
		 * @param left	The left operand.
		 * @param right	the right operand.
		 * @param cond	The condition object.
		 * @return	According comparison instruction.
		 */
		public static Cmp instance(Type ty, Instruction left, 
				Instruction right, Condition cond)
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
		public ICmp(CiKind kind, Instruction left, Instruction right, Condition cond)
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
		public LCmp(CiKind kind, Instruction left, Instruction right, Condition cond)
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
		public FCmp(CiKind kind, Instruction left, Instruction right, Condition cond)
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
		public DCmp(CiKind kind, Instruction left, Instruction right, Condition cond)
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
		public IfCmp_LT(Instruction x, Instruction y, BasicBlock trueTarget,
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
		public IfCmp_LE(Instruction x, Instruction y, BasicBlock trueTarget,
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
		public IfCmp_GT(Instruction x, Instruction y, BasicBlock trueTarget,
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
		public IfCmp_GE(Instruction x, Instruction y, BasicBlock trueTarget,
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
		public IfCmp_EQ(Instruction x, Instruction y, BasicBlock trueTarget,
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
		public IfCmp_NEQ(Instruction x, Instruction y, BasicBlock trueTarget,
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
		BasicBlock target;

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
	public static class Return extends Branch
	{
		Instruction result;

		/**
		 * Constructs a new return instruction with return inst.
		 *
		 * @param result The return inst produce for this instruction, return
		 *               void if result is {@code null}.
		 */
		public Return(Instruction result)
		{
			super(result == null ? CiKind.Void : result.kind);
			this.result = result;
		}

		/**
		 * Gets the instruction that produces the result for the return.
		 *
		 * @return the instruction producing the result
		 */
		public Instruction result()
		{
			return result;
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
		public final Instruction[] arguments;
		/**
		 * The target of this method calling.
		 */
		public final Method target;

		public Instruction ret;

		/**
		 * Constructs a new method calling instruction.
		 *
		 * @param result     The kind of return result.
		 * @param args       The input arguments.
		 * @param target     The called method.
		 */
		public Invoke(CiKind result, Instruction[] args, Method target)
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
		private ArgBlockPair[] inputs;
		
		private int currIndex = 0;

		private String nameString;

		/**
		 * Constructs a new Phi-function instruction.
		 *
		 * @param kind   The kind of result.
		 * @param args   The input arguments.
		 * @param blocks The one of which basic block array is corresponding to
		 *               an input argument.
		 */
		public Phi(CiKind kind, Instruction[] args,
				BasicBlock[] blocks)
		{
			super(kind);
			assert args.length == blocks.length;
			inputs = new ArgBlockPair[args.length];
			for (int idx = 0; idx < args.length; idx++)
				inputs[idx] = new ArgBlockPair(args[idx], blocks[idx]);			
		}
		
		public Phi(CiKind kind, int length)
        {
	        super(kind);
	        this.inputs = new ArgBlockPair[length];
	        this.nameString = "";
        }

		public Phi(CiKind kind, int length, String nameString)
		{
			super(kind);
			this.inputs = new ArgBlockPair[length];
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
		public void addIncoming(Instruction value, BasicBlock block)
		{
			if (value != null && block != null && currIndex < inputs.length)
			{
				
				this.inputs[currIndex++] = new ArgBlockPair(value, block);
			}
		}
		
		/**
		 * Gets the inputed parameter at given position.
		 * @param index	The position where input parameter will be obtained.
		 * @return	The input parameter at specified position.
		 */
		public Instruction getParameter(int index)
		{
			assert index >= 0 && index < inputs.length 
					: "The index is beyond out the size of list";
			return inputs[index].arg;
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
			return inputs[index].block;
		}
		
		/**
		 * Updates the input argument at given position.
		 * @param index	The index into argument to be updated.
		 * @param value	
		 */
		public void setParameter(int index, Instruction value)
		{
			assert index >= 0 && index < inputs.length
					: "The index is beyond out the size of list";
			
			ArgBlockPair old = inputs[index];
			old.arg = value;
			inputs[index] =  old;
		}
		
		/**
		 * Updates the input block at given position.
		 * @param index	The index into block to be updated.
		 * @param block
		 */
		public void setBlock(int index, BasicBlock block)
		{
			assert index >= 0 && index < inputs.length
					: "The index is beyond out the size of list";
			
			ArgBlockPair old = inputs[index];
			old.block = block;
			inputs[index] = old;
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
				if (inputs[idx].block == basicBlock)
					return idx;
			return -1;
		}

		public int getNumberIncomingValues()
		{
			return inputs.length;
		}

		/**
		 * This class for a pair of phi parameter and according block.
		 * @author Jianping Zeng <z1215jping@hotmail.com>
		 */
		public static class ArgBlockPair 
		{
			public Instruction arg;
			public BasicBlock block;
			
			public ArgBlockPair(Instruction arg, BasicBlock block)
			{
				this.arg = arg;
				this.block = block;
			}
		}
	}

	/**
	 * The {@code Constant} instruction represents a constant such as an integer
	 * inst, long, float, object reference, address, etc.
	 */
	public static class Constant extends Instruction
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

		@Override
		public int valueNumber()
		{
			return 0x50000000 | value.hashCode();
		}

		public boolean valueEqual(Instruction i)
		{
			return i instanceof Constant && ((Constant) i).value
					.equivalent(this.value);
		}

		@Override public void accept(InstructionVisitor visitor)
		{
			visitor.visitConstant(this);
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

	public static abstract class Var extends Instruction
	{
		/**
		 * The inst type of this local variable.
		 */
		Type valueType;
		/**
		 * The memory address allocated by instruction {@code Alloca} is related with this variable.
		 */
		public Alloca memAddr;

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
		public void accept(InstructionVisitor visitor)
		{

		}

		@Override
		public String toString()
		{
			return prefix + name;
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
		public Instruction value;
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
		public StoreInst(Instruction value, Alloca dest)
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

	public static class SwitchInst extends Branch
	{
		private Pair<Instruction, BasicBlock>[] operands;
		private int currIdx = 0;
		/**
		 * Constructs a new SwitchInst instruction with specified inst type.
		 * @param condV  the value of selector.
		 * @param defaultBB The default jump block when no other case match.
		 * @param numCases  The numbers of case value.
		 */
		public SwitchInst(Instruction condV, BasicBlock defaultBB, int numCases)
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

		public void addCase(Instruction caseVal, BasicBlock targetBB)
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
