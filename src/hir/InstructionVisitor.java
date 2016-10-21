package hir;

import hir.Instruction.Cmp;
import hir.Instruction.InvokeInst;
import hir.Instruction.LoadInst;
import hir.Instruction.PhiNode;
import hir.Instruction.ReturnInst;
import hir.Instruction.StoreInst;
import hir.Instruction.SwitchInst;

/**
 * A Value Visitor for Instruction or Value using Visitor pattern.
 * @author Xlous.zeng
 * 
 */
public abstract class InstructionVisitor
{
	/**
	 * Visits {@code Instruction} with visitor pattern.
	 * @param inst  The instruction to be visited.
	 */
	public void visitInstruction(Instruction inst) {}


	public void visitConvert(Instruction.CastInst inst)
	{
		
	}

	public void visitCompare(Cmp inst)
	{

	}

	public void visitReturn(ReturnInst inst)
	{
		
	}

	public void visitInvoke(InvokeInst inst)
	{
		
	}

	public void visitPhi(PhiNode inst)
	{
		
	}

	public void visitAlloca(Instruction.AllocaInst inst)
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
