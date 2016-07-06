package optimization;

import java.util.ArrayList;
import hir.BasicBlock;
import hir.DominatorTree;
import hir.Instruction;
import hir.Instruction.Op2;
import hir.Method;
import hir.Value;

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
		 * dependent induction variable.
		 */
		Instruction tiv;
		/**
		 * Base induction variable.
		 */
		Instruction biv;
		
		int factor;
		int diff;
		
		IVRecord(Instruction tiv, Instruction biv, int factor, int diff)
		{
			this.biv = biv;
			this.tiv = tiv;
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
					findInductionVariable(loop);
					
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
			for (Instruction inst : bb)
			{
				if (inst instanceof Op2)
				{
					Op2 op = (Op2)inst;
					if (ivPattern(inst, op.x, op.y)
							|| ivPattern(op, op.y, op.x))
					{
						inductionVars.add(new IVRecord(op, op, 1, 0));
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
				for (Instruction inst : bb)
				{
					if (inst instanceof Op2)
					{
						Op2 op = (Op2)inst;
						change |= isMulIV(op, op.x, op.y);
						change |= isMulIV(op, op.y, op.x);
						change |= isAddIV(op, op.x, op.y);
						change |= isAddIV(op, op.y, op.x);
					}
				}
			}			
		}while(change);
	}
	
	private boolean ivPattern(Instruction inst, Value op1, Value op2)
	{
		return false;
	}
	private boolean isMulIV(Instruction inst, Value op1, Value op2)
	{
		return false;
	}
	private boolean isAddIV(Instruction inst, Value op1, Value op2)
	{
		return false;
	}
}
