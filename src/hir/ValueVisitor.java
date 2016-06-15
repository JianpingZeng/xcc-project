package hir;

import hir.Instruction.*;

/**
 * A Value Visitor for Instruction or Value using Visitor pattern.
 * @author Xlous.zeng
 * 
 */
public abstract class ValueVisitor
{
	/**
	 * Visits {@code Instruction} with visitor pattern.
	 * @param inst  The instruction to be visited.
	 */
	public void visitInstruction(Instruction inst) {}

	/**
	 * Visits Arithmetic Operation with visitor pattern.
	 * @param inst  The operation to be visited.
	 */
	public void visitArithmeticOp(ArithmeticOp inst)
	{

	}

	/**
	 * Visits Logical Operation with visitor pattern.
	 * @param inst  The operation to be visited.
	 */
	public void visitLogicOp(LogicOp inst)
	{

	}

	public void visitShiftOp(ShiftOp shiftOp)
	{

	}
	/**
	 * Visits {@code Negate} with vistor pattern.
	 * @param inst  The inst to be visited.
	 */
	public void visitNegate(Negate inst)
	{
		
	}

	public void visitConvert(Convert inst)
	{
		
	}

	public void visitCompare(Cmp inst)
	{

	}

	public void visitIfOp(IfOp inst)
	{
		
	}

	public void visitGoto(Goto inst)
	{
		
	}

	public void visitReturn(Return inst)
	{
		
	}

	public void visitInvoke(Invoke inst)
	{
		
	}

	public void visitPhi(Phi inst)
	{
		
	}

	public void visitAlloca(Alloca inst)
	{
		
	}

	public void visitStoreInst(StoreInst inst)
	{
		
	}

	public void visitLoadInst(LoadInst inst)
	{
		
	}

	/**
	 * Go through the value {@code Value}. Usually, this method is not used
	 * instead of calling to the visitor to it's subclass, like {@code Constant}.
	 * @param val   The instance of {@code Value} to be visited.
	 */
	public void visitValue(Value val)
	{
	}

	/**
	 * Visits a constant that is an instance of concrete subclass of super class
	 * {@code Value}.
	 * @param Const A constant to be visited.
	 */
	public void visitConstant(Value.Constant Const)
	{

	}

	public void visitUndef(Value.UndefValue undef)
	{

	}

	public void visitSwitch(SwitchInst switchInst)
	{

	}
}
