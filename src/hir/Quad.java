package hir;

import java.lang.annotation.Target;

/**
 * This class represents a Quadruple.
 * 
 * @author Jianping Zeng
 * @see BasicBlock
 * @version `1.0
 */
public abstract class Quad {

	/** the number is just used for print debugging information. */
	protected int number;

	public Quad(int number) {
		this.number = number;
	}
	
	public abstract void accept(QuadVisitor visitor);

	
	/** Represents IArithmetic operation. */
	public static abstract class IArithmetic extends Quad
	{
		public IArithmetic(int number) {
			super(number);
		}		
	}
	
	/** Represents float point number Arithmetic operation. */
	public static abstract class FArithmetic extends Quad
	{
		public FArithmetic(int number) {
			super(number);
		}		
	}
	
	/** For integer binary operation.*/
	public static abstract class IBinary extends IArithmetic {

		public IBinary(int number) {
			super(number);
		}
	}

	/** For float number binary operation.*/
	public static abstract class FBinary extends FArithmetic {

		public FBinary(int number) {
			super(number);
		}
	}
	
	
	
	public static class IAdd extends IBinary {

		private Operand src1, src2, dest;
		
		/**
		 * Constructor for add operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public IAdd(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}	
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIAdd(this);
		}
	}
	
	public static class ISub extends IBinary {

		private Operand src1, src2, dest;
		
		/**
		 * Constructor for subtraction operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public ISub(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitISub(this);
		}
	}
	
	public static class IMul extends IBinary {

		private Operand src1, src2, dest;
		
		
		/**
		 * Constructor for multiple operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public IMul(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}	
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIMul(this);
		}
	}
	
	public static class IDiv extends IBinary {

		private Operand src1, src2, dest;
		
		
		/**
		 * Constructor for divsion operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public IDiv(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIDiv(this);
		}
	}
	
	public static class IMod extends IBinary {

		private Operand src1, src2, dest;
		
		
		/**
		 * Constructor for mod operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public IMod(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}	
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIMod(this);
		}
	}
	
	public static class LShift extends IBinary {

		private Operand src1, src2, dest;
		
		
		/**
		 * Constructor for left shift operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public LShift(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}	
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitLShift(this);
		}
	}
	
	public static class RShift extends IBinary {

		private Operand src1, src2, dest;
		
		
		/**
		 * Constructor for arithmetic shift right operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public RShift(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitRShift(this);
		}
	}	
	
	public static class LRShift extends IBinary {

		private Operand src1, src2, dest;
		
		/**
		 * Constructor for logical shift right operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public LRShift(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitLRShift(this);
		}
	}

	
	public static class BitAnd extends IBinary {

		private Operand src1, src2, dest;
		
		/**
		 * Constructor for bitwise and operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public BitAnd(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}	
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitBitAnd(this);
		}
	}
	
	public static class BitOR extends IBinary {

		private Operand src1, src2, dest;
		
		/**
		 * Constructor for bitwise or operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public BitOR(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitBitOR(this);
		}
	}
	
	public static class BitXOR extends IBinary {

		private Operand src1, src2, dest;
		
		/**
		 * Constructor for bitwise exclusive or operator.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param src2	the right hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public BitXOR(int number, Operand src1, Operand src2, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.src2 = src2;
			this.dest = dest;
		}	
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitBitXOR(this);
		}
	}	
	
	
	public static abstract class IUnary extends IArithmetic
	{
		public IUnary(int number) {
			super(number);
		}
		
	}
	
	
	public static class Assign extends IUnary {

		private Operand src1, dest;
		
		/**
		 * Constructor for assignment.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public Assign(int number, Operand src1, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.dest = dest;
		}
		public void accept(QuadVisitor visitor)
		{
			visitor.visitAssign(this);
		}
	}
	
	public static class IMinus extends IUnary 
	{
		private Operand src1, dest;
		
		/**
		 * Constructor for assignment.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public IMinus(int number, Operand src1, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.dest = dest;
		}		
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIMinus(this);
		}
	}
	
	public static class IBitNot extends IUnary 
	{
		private Operand src1, dest;
		
		/**
		 * Constructor for assignment.
		 * 
		 * @param number	the number id.
		 * @param src1	the left hand of this instruction.
		 * @param dest	the target where result stores.
		 */
		public IBitNot(int number, Operand src1, Operand dest) {
			super(number);
			
			this.number = number;
			this.src1 = src1;
			this.dest = dest;
		}		
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIBitNot(this);
		}
	}
	
	public static abstract class Branch extends Quad {
		protected Label target;
		
		public Branch(int number, Label target) {
			super(number);
			this.target = target;
		}
	}
	
	/** Branching when given integer lower than zero. */
	public static class IfLT extends Branch
	{
		private Operand operand;
		
		public IfLT(int number, Operand operand, Label target) {
			super(number, target);
			this.operand = operand;
		}		
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfLT(this);
		}
	}
	
	/** Branching when given integer lower than another. */
	public static class IfCmpLT extends Branch
	{
		private Operand lhs, rhs;
		
		public IfCmpLT(int number, Operand lhs, Operand rhs, Label target) {
			super(number, target);
			this.lhs = lhs;
			this.rhs = rhs;
		}		
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfCmpLT(this);
		}
	}
	
	/** Branching when a integer no greater than another. */
	public static class IfCmpLE extends Branch
	{
		private Operand lhs, rhs;
		
		public IfCmpLE(int number, Operand lhs, Operand rhs, Label target) {
			super(number, target);
			this.lhs = lhs;
			this.rhs = rhs;
		}	
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfCmpLE(this);
		}
	}
	
	/** Branching when a integer no greater than zero. */
	public static class IfLE extends Branch
	{
		private Operand operand;
		
		public IfLE(int number, Operand operand, Label target) {
			super(number, target);
			this.operand = operand;
		}		
			
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfLE(this);
		}
	}
	
	/** Branching when a integer equal than another. */
	public static class IfCmpEQ extends Branch
	{
		private Operand lhs, rhs;
		
		public IfCmpEQ(int number, Operand lhs, Operand rhs, Label target) {
			super(number, target);
			this.lhs = lhs;
			this.rhs = rhs;
		}		
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfCmpEQ(this);
		}
	}
	
	/** Branching when a integer equal than zero. */
	public static class IfEQ extends Branch
	{
		private Operand operand;
		
		public IfEQ(int number, Operand operand, Label target) {
			super(number, target);
			this.operand = operand;
		}		
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfEQ(this);
		}
	}
	
	/** Branching when a integer not equal than another. */
	public static class IfCmpNE extends Branch
	{
		private Operand lhs, rhs;
		
		public IfCmpNE(int number, Operand lhs, Operand rhs, Label target) {
			super(number, target);
			this.lhs = lhs;
			this.rhs = rhs;
		}		
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfCmpNE(this);
		}
	}
	
	/** Branching when a integer not equal than zero. */
	public static class IfNE extends Branch
	{
		private Operand operand;
		
		public IfNE(int number, Operand operand, Label target) {
			super(number, target);
			this.operand = operand;
		}		
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfNE(this);
		}
	}
	
	/** Branching when a integer greater than zero. */
	public static class IfGT extends Branch
	{
		private Operand lhs, rhs;
		
		public IfGT(int number, Operand lhs, Operand rhs, Label target) {
			super(number, target);
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfGT(this);
		}
	}
	
	/** Branching when a integer greater than another. */
	public static class IfCmpGT extends Branch
	{
		private Operand lhs, rhs;
		
		public IfCmpGT(int number, Operand lhs, Operand rhs, Label target) {
			super(number, target);
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfCmpGT(this);
		}
	}
	
	/** Branching when a integer no lower than another. */
	public static class IfCmpGE extends Branch
	{
		private Operand lhs, rhs;
		
		public IfCmpGE(int number, Operand lhs, Operand rhs, Label target) {
			super(number, target);
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfCmpGE(this);
		}
	}
	
	/** Branching when a integer no lower than zero. */
	public static class IfGE extends Branch
	{
		private Operand operand;
		
		public IfGE(int number, Operand operand, Label target) {
			super(number, target);
			this.operand = operand;
		}
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitIfGE(this);
		}
	}
	
	/** A non-condition jump instruction. */
	public static class Goto extends Branch {
		
		/**
		 * Constructor for no-condition jump instruction.
		 * 
		 * @param number	the number id of this instruction.
		 * @param target	the target label.
		 */
		public Goto(int number, Label target) {
			super(number, target);
		}			
		
		public void accept(QuadVisitor visitor)
		{
			visitor.visitGoto(this);
		}
	}	
	/** Represents a return instruction. */
	public static class Ret extends Quad{
		
		public Ret(int number)
		{
			super(number);
		}

		@Override
		public void accept(QuadVisitor visitor) {
			
			visitor.visitRet(this);
		}
	} 
	
	/** Represents a return instruction with integer. */
	public static class IRet extends Quad{
		
		Operand value;
		
		public IRet(int number, Operand value)
		{
			super(number);
			this.value = value;
		}

		@Override
		public void accept(QuadVisitor visitor) {
			
			visitor.visitIRet(this);
		}
	}
	
	/** Represents a return instruction with long integer. */
	public static class LRet extends Quad{
		
		Operand value;
		
		public LRet(int number, Operand value)
		{
			super(number);
			this.value = value;
		}

		@Override
		public void accept(QuadVisitor visitor) {
			
			visitor.visitLRet(this);
		}
	}
	
	public static class Label extends Quad {
		static String BASE = "L";
		static long sequence = 1;
		String name;
		public Label(int number) 
		{			
			super(number);
			this.name = BASE + sequence++;
		}

		@Override
		public void accept(QuadVisitor visitor) {
			visitor.visitLabel(this);
		}		
	}	
}
