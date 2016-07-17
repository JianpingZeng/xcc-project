package hir;

import hir.Instruction.Alloca;
import hir.Instruction.ArithmeticOp;
import hir.Instruction.Cmp;
import hir.Instruction.Convert;
import hir.Instruction.Goto;
import hir.Instruction.IfOp;
import hir.Instruction.Invoke;
import hir.Instruction.LoadInst;
import hir.Instruction.LogicOp;
import hir.Instruction.Negate;
import hir.Instruction.Phi;
import hir.Instruction.Return;
import hir.Instruction.ShiftOp;
import hir.Instruction.StoreInst;
import hir.Instruction.SwitchInst;

/**
 * A Instruction Visitor for Instruction using Visitor pattern.
 * @author Xlous.zeng
 * 
 */
public abstract class InstructionVisitor
{
	/**
	 * Visits {@code Instruction} with visitor pattern.
	 * @param inst  The instruction to be visited.
	 */
	public void visitInstruction(Instruction inst) { assert false;}

	public void visitArithmeticOp(ArithmeticOp inst)
    {
	    visitInstruction(inst);
    }
	
	public void visitLogicOp(LogicOp inst)
    {
	    visitInstruction(inst);
    }
	
	public void visitShiftOp(ShiftOp inst)
    {
	    visitInstruction(inst);
    }
	
	public void visitNegate(Negate inst)
    {
	    visitInstruction(inst);
    }
	
	public void visitConvert(Convert inst)
    {
	    visitInstruction(inst);
    }
	
	public void visitIfOp(IfOp inst)
    {
	    visitInstruction(inst);
    }
	
	public void visitCmp(Cmp inst)
    {
	    visitInstruction(inst);
    }
	
	public void visitGoto(Goto inst)
	{
		visitInstruction(inst);
	}

	public void visitReturn(Return inst)
	{
		visitInstruction(inst);
	}

	public void visitInvoke(Invoke inst)
	{
		visitInstruction(inst);
	}

	public void visitPhi(Phi inst)
	{
		visitInstruction(inst);
	}

	public void visitAlloca(Alloca inst)
	{
		visitInstruction(inst);
	}

	public void visitStoreInst(StoreInst inst)
	{
		visitInstruction(inst);
	}

	public void visitLoadInst(LoadInst inst)
	{
		visitInstruction(inst);
	}
	
	public void visitSwitch(SwitchInst inst)
    {
	    visitInstruction(inst);
    }
}
