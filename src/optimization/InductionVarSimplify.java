package optimization;

import java.util.ArrayList;
import java.util.List;

import lir.ci.LIRConstant;
import lir.ci.LIRKind;
import hir.BasicBlock;
import hir.DominatorTree;
import hir.Instruction;
import hir.Instruction.ArithmeticOp;
import hir.Operator;
import hir.Instruction.Alloca;
import hir.Instruction.Op2;
import hir.Instruction.StoreInst;
import hir.Method;
import hir.Value;
import hir.Value.Constant;

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
 * for (int i = 0; i < 100; i++)
 * {
 *   a[i] = 198 - i*2;
 * }
 * 
 * It can be replaced by the following code:
 * 
 * int a[100], t1 = 200;
 * for (int i = 0; i < 100; i++)
 * {
 *   t1 -= 2;
 *   a[i] = t1;
 * }
 * </pre>
 * 
 * @author xlos.zjp
 * @version 0.1
 */
public final class InductionVarSimplify
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
	 * A list contains all of induction variable records in this function being optimized.
	 */
	private ArrayList<IVRecord> inductionVars;
	private Loop[] loops;
	private Loop[] loopIdToLoops;
	private DominatorTree dt;
	private boolean[] marked;
	
	/**
	 * indicates if a instruction has been performed on strength reduction.
	 */
	private boolean[][] SRdone;
	
	/**
	 * A interface to run on method being compiled.
	 * @param method
	 */
	public void runOnLoop(Method method)
	{
		this.inductionVars = new ArrayList<>();		
		initialize(method);
		
		for (Loop loop : loops)
		{
			// ignores some loops were optimized.
			if (!marked[loop.loopIndex])
			{
				do 
				{
					/* 1#	found all of induction variables */
					findInductionVariable(loop);
					
					/* 2#	perform strength reduction */
					strengthReduction(loop);
					loop = loop.outerLoop;
				}while (loop != null);
			}
		}
	}
	/**
	 * Initialize some helpful data structure as needed.
	 * @param method
	 */
	private void initialize(Method method)
	{
		dt = new DominatorTree(method);
		dt.recalculate();
		loops = method.getLoops();
		assert loops != null && loops.length > 0
				: "must performed after loop analysis pass";
		
		int maxLoopIndex = -1;
		for (Loop l : loops)
		{
			if (l.loopIndex > maxLoopIndex)
				maxLoopIndex = l.loopIndex;
		}
		
		loopIdToLoops = new Loop[maxLoopIndex + 1];
		marked = new boolean[loops.length];
		
		// initialize the map IdToLoops
		for (Loop l : loops)
			loopIdToLoops[l.loopIndex] = l;	
		
		// instantiate a array of type boolean which keeps track of 
		SRdone = new boolean[method.cfg.getNumberOfBasicBlocks()][];
		for (BasicBlock bb : method)
			SRdone[bb.getID()] = new boolean[bb.size()];
	}
	/**
	 * This method was served as marking all induction variables in this function.
	 */
	private void findInductionVariable(Loop loop)
	{
		for (BasicBlock bb : loop.blocks)
		{
			// search for instructions that compute fundamental induction 
			// variable and accumulate informations about them in inductionVars
			for (Value inst : bb)
			{
				if (inst instanceof Op2)
				{
					Op2 op = (Op2)inst;
					if (ivPattern(op, op.x, op.y)
							|| ivPattern(op, op.y, op.x))
					{
						inductionVars.add(new IVRecord(op, op, 
								Constant.CONSTANT_INT_1, 
								Constant.CONSTANT_INT_0));
					}
				}
			}
		}
		
		boolean change;
		do
		{
			change = false;
			for (BasicBlock bb : loop.blocks)
			{
				// check for dependent induction variables 
				// and accumulate information in list inductionVars.
				for (Value inst : bb)
				{
					if (inst instanceof Op2)
					{
						Op2 op = (Op2)inst;
						change |= isMulIV(op, op.x, op.y, loop);
						change |= isMulIV(op, op.y, op.x, loop);
						change |= isAddIV(op, op.x, op.y, loop);
						change |= isAddIV(op, op.y, op.x, loop);
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
	 * Obtains the index by which the specified block will be indexed. 
	 * @param blockId	The id of specified basic block.
	 * @param nblocks	The array of block id.
	 * @return If there no block with specified blockId existed in nblocks, return -1
	 * , otherwise, return its index.
	 */
	private int blockIndex(int blockId, List<BasicBlock> blocks)
	{
		for (int i = 0; i < blocks.size(); i++)			
		{
			int Id = blocks.get(i).getID();
			if (blockId == Id)
				return i; 
		}
		return -1;
	}
	/**
	 * <p>Checks if the basic block where the operand defined was out of this loop.</p>
	 * <p>Return true if it is out of Loop, otherwise false returned.</p>
	 * @param blocks
	 * @param operand
	 * @return
	 */
	private boolean reachDefsOut(List<BasicBlock> blocks, Value operand)
	{
			BasicBlock bb = operand.getParent();
			if (blockIndex(bb.getID(), blocks) >= 0)
				return false;
			else 
				return true;			
	}
	
	private boolean isLoopConstant(Value v)
	{
		List<BasicBlock> list = v.getParent().getOuterLoop().blocks;
		return v.isConstant() || reachDefsOut(list, v);
	}
	/**
	 * Performs a pattern matching on specified binary operation.
	 * @param inst
	 * @param op1
	 * @param op2
	 * @return
	 */
	private boolean ivPattern(Op2 inst, Value op1, Value op2)
	{
		return inst == op1 && inst.opcode.isAdd()
				&& op2.isConstant()
				&& isLoopConstant(inst)
				&& isContainedInductionVars(inst);
	}
	/**
	 * find out a IVRecord by one value.
	 * If there no found result, then {@code null} will be returned.
	 * Otherwise return the found IVRecord object.
	 * @param inst
	 * @return
	 */
	private IVRecord findBaseIVByValue(Value val)
	{
		for (IVRecord record : inductionVars)
		{
			if (record.div.equals(val) && record.div.equals(record.biv)
					&& record.factor.equals(Constant.CONSTANT_INT_1) 
					&& record.diff.equals(Constant.CONSTANT_INT_0))
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
	private boolean isMulIV(Op2 inst, Value op1, Value op2, Loop loop)
	{	
		/* 
		 * Only when inst is a multiple operation and whose first operand
		 * is a loop constant, then continue.
		 * 
		 * Take j = e*i expression for instance, op1 stands for e, op2 likes i,
		 * 
		 * 1#:we try to check if op2(called i) is a basic induction variable.	
		 */	
		if (isLoopConstant(op1) && inst.opcode.isMul())
		{
			IVRecord iv = findBaseIVByValue(op2);
			if (iv != null)
			{
				inductionVars.add(new IVRecord(inst, iv.biv, 
						op1.asConstant(), Constant.CONSTANT_INT_0));
			}
			/* 
			 * 2#: attempting to inspect op2(called i) is a derived induction variable.
			 * At this point, we must make sure about there no other definition of i
			 * outside of loop which reaches the j.
			 */
			else if ((iv = findDependentIVByValue(op2)) != null
					&& !reachDefsOut(loop.blocks, op2))
			{
				Constant factor = Constant.multiple(op1.asConstant(), iv.factor);
				Constant diff = Constant.multiple(op1.asConstant(), iv.diff);
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
	private boolean isAddIV(Op2 inst, Value op1, Value op2, Loop loop)
	{
		assert inst.opcode.isAdd() || inst.opcode.isSub();
		
		/* 
		 * Only when inst is a addictive operation and whose first operand
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
				if (inst.opcode.isAdd())
				{
					inductionVars.add(new IVRecord(inst, iv.biv, 
							Constant.CONSTANT_INT_1, op1.asConstant()));
				}
				else 
				{
					assert inst.opcode.isSub();
					inductionVars.add(new IVRecord(inst, iv.biv, 
							Constant.CONSTANT_INT_MINUS_1, op1.asConstant()));
				}
			}
			/* 
			 * 2#: attempting to inspect op2(called i) is a derived induction variable.
			 * At this point, we must make sure about there no other definition of i
			 * outside of loop which reaches the j.
			 */
			else if ((iv = findDependentIVByValue(op2)) != null
					&& !reachDefsOut(loop.blocks, op2))
			{		
				Constant diff = Constant.add(op1.asConstant(), iv.diff);
				if (inst.opcode.isAdd())
				{
					inductionVars.add(new IVRecord(inst, op2, iv.factor, diff));
				}
				else 
				{
					assert inst.opcode.isSub();
					
					Constant factor = Constant.sub(0, iv.factor);
					diff = Constant.sub(op1.asConstant(), diff);
					
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
		BasicBlock preheaderBB = loop.getPreheader();
		
		/** search for uses of induction variable.*/
		for (IVRecord r1 : inductionVars)
		{
			/** basic induction variable*/
			if (r1.factor.value.equals(LIRConstant.INT_1) 
					&& r1.diff.value.equals(LIRConstant.INT_0))
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
        				LIRKind kind = r2.div.kind;
        				Alloca tj = new Alloca(kind, Constant.forInt(1), "%tj");
        				Alloca db = new Alloca(r2.factor.kind, Constant.forInt(1), "%db");
        				
        				int i = r2.div.getParent().getID();
        				int j = ((Instruction)r2.div).id;
        				
        				/*
        				 * split their computation between preheader and this use,
        				 * replacing operations by less expensive ones. 
        				 */
        				SRdone[i][j] = true;
        				/** db = d*b; */
        				ArithmeticOp t1 = new ArithmeticOp(db.kind, 
        						Operator.getMulByKind(db.kind), 
        						r1.diff, r2.factor);
        				StoreInst s1 = new Instruction.StoreInst(t1, db, "");
        				Value[] insts = {t1, s1};
        				appendPreheader(insts, preheaderBB);
        				
        				/** tj=b*i */
        				t1 = new ArithmeticOp(r2.factor.kind, 
        						Operator.getMulByKind(r2.factor.kind), 
        						r2.factor, r1.biv);
        				s1 = new StoreInst(t1, tj, "");
        				Value[] insts2 = {t1, s1};
        				appendPreheader(insts2, preheaderBB);    
      
        				/** tj=tj+c */
        				t1 = new ArithmeticOp(tj.kind, Operator.getAddByKind(tj.kind), tj, r2.diff);        				
        				s1 = new StoreInst(t1, tj, "");
        				Value[] insts3 = {t1, s1};
        				appendPreheader(insts3, preheaderBB);
        				
        				/** tj=tj+db*/
        				t1 = new ArithmeticOp(tj.kind, Operator.getAddByKind(tj.kind), tj, db);        				
        				s1 = new StoreInst(t1, tj, "");
        				Value[] after = {t1, s1};
        				insertAfter(r2.div, after);
        				
        				/** replaces all uses of jth instruction by tj.*/
        				r2.div.replaceAllUsesWith(tj);
        				
        				/** remove the jth instruction from the basic block containing it.*/
        				r2.div.eraseFromBasicBlock();
        				
        				/** 
        				 * append tj to the class of induction variables based on i
        				 * with linear equation tj = b*i + c.
        				 */
        				Constant factor = Constant.multiple(r1.factor, r2.factor); 
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
    private void appendPreheader(Value inst, BasicBlock preheader)
	{
		assert inst != null && preheader != null;
		preheader.appendInst(inst);
	}	
	
	/**
	 * Append an instruction have been removed from original basic block
	 * into the last of preheader block.
	 * @param inst
	 * @param preheader
	 */
	private void appendPreheader(Value[] insts, BasicBlock preheader)
	{
		assert insts != null && preheader != null;
		for (Value inst : insts)
			preheader.appendInst(inst);
	}
	/**
	 * Appends a sorts of instruction into position after {@code target} instruction.
	 * @param target
	 * @param after
	 */
	private void insertAfter(Value target, Value[] after)
	{
		assert target != null && after!=null;	
		// if the length of after is not greater zero, just immediately return.
		if (after.length <= 0) return;
		
		target.insertAfter(after[0]);
		for (int i = 1; i < after.length; i++)
		{
			after[i - 1].insertAfter(after[i]);
		}
	}
}
