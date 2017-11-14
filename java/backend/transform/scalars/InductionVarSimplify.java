package backend.transform.scalars;

import backend.analysis.DomTreeInfo;
import backend.analysis.LoopInfo;
import backend.pass.AnalysisUsage;
import backend.pass.LPPassManager;
import backend.pass.LoopPass;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.AllocaInst;
import backend.value.Instruction.BinaryInstruction;
import backend.value.Instruction.StoreInst;

import java.util.ArrayList;
import java.util.List;

import static backend.transform.utils.ConstantFolder.constantFoldBinaryInstruction;

/**
 * <p>
 * This class presents a functionality used for improving the execution of loop
 * in function which is frequently executed through identifying <a href =
 * "https://en.wikipedia.org/wiki/Induction_variable">Induction Variable</a>
 * which is simplified on arithmetic strength.
 * </p>
 * <pre>
 * we take a sample for instance as following.
 * int a[100];
 * for (int i = 0; i &lt; 100; i++)
 * {
 *   a[i] = 198 - i*2;
 * }
 * 
 * It can be replaced by the following code:
 * 
 * int a[100], t1 = 200;
 * for (int i = 0; i &lt; 100; i++)
 * {
 *   t1 -= 2;
 *   a[i] = t1;
 * }
 * </pre>
 * 
 * @author xlos.zjp
 * @version 0.1
 */
public final class InductionVarSimplify implements LoopPass
{
	private static class IVRecord
	{
		/**
		 * derived induction variable.
		 */
		Value div;
		/**
		 * Base induction variable.
		 */
		Value biv;

		Constant factor;
		Constant diff;

		IVRecord(Value tiv, Value biv, Constant factor, Constant diff)
		{
			this.biv = biv;
			this.div = tiv;
			this.factor = factor;
			this.diff = diff;
		}
	}
	/**
	 * A list isDeclScope all of induction variable records in this function being optimized.
	 */
	private ArrayList<IVRecord> inductionVars;
	private boolean[] marked;

	/**
	 * indicates if a instruction has been performed on strength reduction.
	 */
	private boolean[][] SRdone;

	private LoopInfo li;
	private DomTreeInfo dt;
	boolean changed = false;

	@Override
	public String getPassName()
	{
		return "Induction variable simplification pass";
	}

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(DomTreeInfo.class);
		au.addRequired(LoopInfo.class);
		au.addRequired(LoopSimplify.class);
		au.addRequired(UnreachableBlockElim.class);
	}

	@Override
	public boolean runOnLoop(Loop loop, LPPassManager ppm)
	{
		li = (LoopInfo) getAnalysisToUpDate(LoopInfo.class);
		dt = (DomTreeInfo) getAnalysisToUpDate(DomTreeInfo.class);

		this.inductionVars = new ArrayList<>();
		initialize(loop);

		do
		{
			/* 1#	found all of induction variables */
			findInductionVariable(loop);

			/* 2#	perform strength reduction */
			strengthReduction(loop);
			loop = loop.getParentLoop();
		}while (loop != null);
		return changed;
	}

	/**
	 * Initialize some helpful data structure as needed.
	 * @param loop
	 */
	private void initialize(Loop loop)
	{
		List<BasicBlock> list = loop.getBlocks();
		// instantiate a array of jlang.type boolean which keeps track of
		SRdone = new boolean[list.size()][];
		for (BasicBlock bb : list)
			SRdone[bb.getID()] = new boolean[bb.size()];
	}
	/**
	 * This method was served as marking all induction variables in this function.
	 */
	private void findInductionVariable(Loop loop)
	{
		for (BasicBlock bb : loop.getBlocks())
		{
			// search for instructions that compute fundamental induction 
			// variable and accumulate informations about them in inductionVars
			for (Instruction inst : bb)
			{
				if (inst instanceof Instruction.BinaryInstruction)
				{
					Instruction.BinaryInstruction op = (Instruction.BinaryInstruction)inst;
					if (ivPattern(op, op.operand(0), op.operand(1))
							|| ivPattern(op, op.operand(1), op.operand(0)))
					{
						inductionVars.add(new IVRecord(op, op, 
								Constant.getAllOnesValue(op.getType()),
								Constant.getNullValue(op.getType())));
					}
				}
			}
		}
		
		boolean change;
		do
		{
			change = false;
			for (BasicBlock bb : loop.getBlocks())
			{
				// check for dependent induction variables 
				// and accumulate information in list inductionVars.
				for (Instruction inst : bb)
				{
					if (inst instanceof BinaryInstruction)
					{
						Instruction.BinaryInstruction op = (Instruction.BinaryInstruction)inst;
						Value x = op.operand(0), y = op.operand(1);
						change |= isMulIV(op, x, y, loop);
						change |= isMulIV(op, y, x, loop);
						change |= isAddIV(op, x, y, loop);
						change |= isAddIV(op, y, x, loop);
					}
				}
			}			
		}while(change);
	}
	
	private boolean isContainedInductionVars(Instruction inst)
	{
		for (IVRecord rec : inductionVars)
		{
			if (rec.div.equals(inst))
				return true;
		}
		return false;
	}

	/**
	 * <p>Checks if the basic block where the operand defined was out of this loop.</p>
	 * <p>ReturnInst true if it is out of Loop, otherwise false returned.</p>
	 * @param loop
	 * @param operand
	 * @return
	 */
	private boolean reachDefsOut(Loop loop, Instruction operand)
	{
		BasicBlock bb = operand.getParent();
        return !loop.contains(bb);
	}
	
	private boolean isLoopConstant(Value v)
	{
		if (v.isConstant())
			return true;
		else if (v instanceof Instruction)
		{
			Instruction inst = (Instruction)v;
			return reachDefsOut(li.getLoopFor(inst.getParent()), inst);
		}
		else
			return false;
	}
	/**
	 * Performs a pattern matching on specified binary operation.
	 * @param inst
	 * @param op1
	 * @param op2
	 * @return
	 */
	private boolean ivPattern(Instruction.BinaryInstruction inst, Value op1, Value op2)
	{
		return inst == op1 && inst.getOpcode().isAdd()
				&& op2.isConstant()
				&& isLoopConstant(inst)
				&& isContainedInductionVars(inst);
	}
	/**
	 * find out a IVRecord by one value.
	 * IfStmt there no found getReturnValue, then {@code null} will be returned.
	 * Otherwise return the found IVRecord object.
	 * @param val
	 * @return
	 */
	private IVRecord findBaseIVByValue(Value val)
	{
		for (IVRecord record : inductionVars)
		{
			if (record.div.equals(val) && record.div.equals(record.biv)
					&& record.factor.equals(ConstantInt.get(record.factor.getType(), 1))
					&& record.diff.equals(Constant.getNullValue(record.diff.getType())))
			{
				return record;
			}
		}
		return null;
	}
	
	private IVRecord findDependentIVByValue(Value val)
	{
		for (IVRecord record : inductionVars)
		{
			if (record.div.equals(val) && record.div.equals(record.biv))
			{
				return record;
			}
		}
		return null;
	}
	
	/**
	 * <pre>
	 * Assignment types that may generate dependent induction variables.
	 * j = i*e;
	 * j = e*i;
	 * </pre>
	 * @param inst
	 * @param op1
	 * @param op2
	 * @param loop
	 * @return
	 */
	private boolean isMulIV(BinaryInstruction inst, Value op1, Value op2, Loop loop)
	{	
		/* 
		 * Only when inst is a multiple operation and whose first operand
		 * is a loop constant, then continue.
		 * 
		 * Take j = e*i expression for instance, op1 stands for e, op2 likes i,
		 * 
		 * 1#:we try to check if op2(called i) is a basic induction variable.	
		 */	
		if (isLoopConstant(op1) && inst.getOpcode().isMul())
		{
			IVRecord iv = findBaseIVByValue(op2);
			if (iv != null)
			{
				inductionVars.add(new IVRecord(inst, iv.biv, 
						op1.asConstant(), Constant.getNullValue(iv.biv.getType())));
			}
			/* 
			 * 2#: attempting to inspect op2(called i) is a derived induction variable.
			 * At this point, we must make sure about there no other definition of i
			 * outside of loop which reaches the j.
			 */
			else if ((iv = findDependentIVByValue(op2)) != null
					&& !reachDefsOut(loop, (Instruction) op2))
			{
				Operator opcode = op1.getType().isFloatingPointType() ? Operator.FAdd :Operator.Add;
				Constant factor = constantFoldBinaryInstruction(opcode, op1.asConstant(), iv.factor);

				opcode = op1.getType().isFloatingPointType() ? Operator.FMul : Operator.Mul;
				Constant diff = constantFoldBinaryInstruction(opcode, op1.asConstant(), iv.diff);
				assert factor != null && diff != null;

				inductionVars.add(new IVRecord(inst, op2, factor, diff));
			}			
			return true;
		}
		return false;
	}
	/**
	 * <pre>
	 * Assignment types that may generate dependent induction variables.
	 * j = e+i;
	 * j = i-e;
	 * j = e-i;
	 * j = -i; 
	 * </pre>
	 * @param inst
	 * @param op1
	 * @param op2
	 * @param loop
	 * @return
	 */
	private boolean isAddIV(Instruction.BinaryInstruction inst, Value op1, Value op2, Loop loop)
	{
		assert inst.getOpcode().isAdd() || inst.getOpcode().isSub();
		
		/* 
		 * Only when inst is a add operation and whose first operand
		 * is a loop constant, then continue.
		 * 
		 * Take j = e+i expression for instance, op1 stands for e, op2 likes i,
		 * 
		 * 1#:we try to check if op2(called i) is a basic induction variable.	
		 */	
		if (isLoopConstant(op1))
		{
			IVRecord iv = findBaseIVByValue(op2);	
			if (iv != null)
			{
				if (inst.getOpcode().isAdd())
				{
					inductionVars.add(new IVRecord(inst, iv.biv, 
							ConstantInt.get(iv.factor.getType() ,1),
							op1.asConstant()));
				}
				else 
				{
					assert inst.getOpcode().isSub();
					inductionVars.add(new IVRecord(inst, iv.biv, 
							Constant.getAllOnesValue(iv.factor.getType()),
							op1.asConstant()));
				}
			}
			/* 
			 * 2#: attempting to inspect op2(called i) is a derived induction variable.
			 * At this point, we must make sure about there no other definition of i
			 * outside of loop which reaches the j.
			 */
			else if ((iv = findDependentIVByValue(op2)) != null
					&& !reachDefsOut(loop, (Instruction) op2))
			{
				Operator opcode = op1.getType().isFloatingPointType() ? Operator.FAdd : Operator.Add;
				Constant diff = constantFoldBinaryInstruction(opcode, op1.asConstant(), iv.diff);
				assert diff != null;
				if (inst.getOpcode().isAdd())
				{
					inductionVars.add(new IVRecord(inst, op2, iv.factor, diff));
				}
				else 
				{
					assert inst.getOpcode().isSub();
					opcode = iv.factor.getType().isFloatingPointType() ?
							Operator.FSub: Operator.Sub;
					Constant factor = constantFoldBinaryInstruction(opcode,
							ConstantInt.getFalse(), iv.factor);
					assert factor != null;

					opcode = diff.getType().isFloatingPointType() ? Operator.FSub : Operator.Sub;
					diff = constantFoldBinaryInstruction(opcode, op1.asConstant(), diff);
					assert diff != null;
					
					inductionVars.add(new IVRecord(inst, op2, factor, diff));
				}		
			}			
			return true;			
		}
		return false;
	}
	
	/**
	 * this method implements a algorithm witch performs strength reduction on induction 
	 * variables.
	 * @param loop
	 */
	private void strengthReduction(Loop loop)
	{
		/**obtains the pre-header block*/
		BasicBlock preheaderBB = loop.getLoopPreheader();
		
		/** search for uses of induction variable.*/
		for (IVRecord r1 : inductionVars)
		{
			/** basic induction variable*/
			if (r1.factor instanceof ConstantInt
				&& r1.diff instanceof ConstantInt
				&& ((ConstantInt)r1.factor).getValue().eq(1)
				&& ((ConstantInt)r1.diff).getValue().eq(0))
			{
				/**search derived induction variable which use above basic var.*/
        		for (IVRecord r2 : inductionVars)
        		{
        			if (r1.equals(r2)) continue;
        			if (r2.biv.equals(r1.div) && !r2.biv.equals(r2.div))
        			{
        				/**
        				 * 						pre-header block
        				 * j = b*i + c;  ==>    db = d*b;
        				 * 					    tj = b*i;
        				 * 						tj = tj + c;
        				 * 
        				 * 						j = tj;
        				 * 						tj = tj + db;
        				 */
        				Type kind = r2.div.getType();
        				AllocaInst tj = new AllocaInst(kind, ConstantInt.getTrue(), "%tj");
        				AllocaInst db = new AllocaInst(r2.factor.getType(), ConstantInt.getTrue(), "%db");
        				
        				int i = ((Instruction)r2.div).getParent().getID();
        				int j = ((Instruction)r2.div).id;
        				
        				/*
        				 * split their computation between preheader and this use,
        				 * replacing operations by less expensive ones. 
        				 */
        				SRdone[i][j] = true;
        				/** db = d*b; */
        				Operator opcode = db.getType().isFloatingPointType() ? Operator.FMul:Operator.Mul;
        				Instruction.BinaryInstruction t1 = new Instruction.BinaryInstruction(db.getType(), opcode, r1.diff, r2.factor, "mul");
        				StoreInst s1 = new StoreInst(t1, db, "store");
        				Instruction[] insts = {t1, s1};
        				appendPreheader(insts, preheaderBB);
        				
        				/** tj=b*i */
        				opcode = r2.factor.getType().isFloatingPointType() ? Operator.FMul:Operator.Mul;
        				t1 = new Instruction.BinaryInstruction(r2.factor.getType(),
        						opcode,
        						r2.factor, r1.biv, "mul");
        				s1 = new StoreInst(t1, tj, "store");
        				Instruction[] insts2 = {t1, s1};
        				appendPreheader(insts2, preheaderBB);    
      
        				/** tj=tj+c */
				        opcode = tj.getType().isFloatingPointType()?Operator.FAdd:Operator.Add;
        				t1 = new Instruction.BinaryInstruction(tj.getType(), opcode, tj, r2.diff, opcode.opName);
        				s1 = new StoreInst(t1, tj, "");
        				Instruction[] insts3 = {t1, s1};
        				appendPreheader(insts3, preheaderBB);
        				
        				/** tj=tj+db*/
        				t1 = new Instruction.BinaryInstruction(tj.getType(), opcode, tj, db, opcode.opName);
        				s1 = new StoreInst(t1, tj, "");
        				Instruction[] after = {t1, s1};
        				insertAfter((Instruction) r2.div, after);
        				
        				/** replaces all uses of jth instruction by tj.*/
        				r2.div.replaceAllUsesWith(tj);
        				
        				/** remove the jth instruction from the basic block containing it.*/
        				((Instruction) r2.div).eraseFromParent();
        				
        				/** 
        				 * append tj to the class of induction variables based on i
        				 * with linear equation tj = b*i + c.
        				 */
        				opcode = r1.factor.getType().isFloatingPointType() ?Operator.FMul:Operator.Mul;
        				Constant factor = constantFoldBinaryInstruction(opcode, r1.factor, r2.factor);
        				inductionVars.add(new IVRecord(tj, r1.biv, factor, r2.diff));        				        				        			
        			}        			
        		}
			}
		}
	}
	
	/**
	 * Append an instruction have been removed from original basic block
	 * into the last of preheader block.
	 * @param inst
	 * @param preheader
	 */
	@SuppressWarnings("unused")
    private void appendPreheader(Instruction inst, BasicBlock preheader)
	{
		assert inst != null && preheader != null;
		preheader.appendInst(inst);
	}	
	
	/**
	 * Append an instruction have been removed from original basic block
	 * into the last of preheader block.
	 * @param insts
	 * @param preheader
	 */
	private void appendPreheader(Instruction[] insts, BasicBlock preheader)
	{
		assert insts != null && preheader != null;
		for (Instruction inst : insts)
			preheader.appendInst(inst);
	}
	/**
	 * Appends a sorts of instruction into position after {@code TargetData} instruction.
	 * @param target
	 * @param after
	 */
	private void insertAfter(Instruction target, Instruction[] after)
	{
		assert target != null && after!=null;	
		// if the getArraySize of after is not greater zero, just immediately return.
		if (after.length <= 0) return;
		
		target.insertAfter(after[0]);
		for (int i = 1; i < after.length; i++)
		{
			after[i - 1].insertAfter(after[i]);
		}
	}
}
