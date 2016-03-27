package optimization;

import hir.*;
import hir.Instruction.Return;
import hir.Instruction.StoreInst;

import java.util.*;

/**
 * This file defines a class that  performs useless instruction elimination and
 * dead code elimination.
 * <p>
 *     Dead code elimination perform a single pass over the function removing
 *     instructions that are obviously useless.
 * </p>
 * <p>
 *    At the sweep stage, a collection of another peephole control flow optimizations
 *    will be performed. For instance:
 *    <br>
 *    1.Removes basic block with no predecessors.
 *    <br>
 *    2.Merges a basic with it's predecessor if there is only one and the predeccessor
 *    just only have one successor.
 *    <br>
 * </p>
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/3/8.
 */
public class DCE
{
	/**
	 * The list where all critical instruction in HIR term resides.
	 */
	private LinkedList<Instruction> criticalInst;
	/**
	 * The list that contains more than one critical instruction.
	 */
	private LinkedList<BasicBlock> usefulBlocks;

	/**
	 * The list contains all of no dead instruction.
	 */
	private HashSet<Instruction> liveInsts;

	private Method m;

	private DominatorTree DT;

	/**
	 * Default constructor.
	 *
	 * @param m The method where {@code DCE} will be performed.
	 */
	public DCE(Method m)
	{
		this.criticalInst = new LinkedList<>();
		this.usefulBlocks = new LinkedList<>();
		this.liveInsts = new HashSet<>();
		this.m = m;
		this.DT = new DominatorTree(true, m);
		this.DT.recalculate();
	}

	/**
	 * Performs a single pass dead code elimination over specified method with
	 * follow Mark-Sweep algorithm:
	 * <br>
	 * <b>i.Initial stage</b> that obtains a critical instruction set over function
	 * <br>
	 * <b>ii.Mark stage</b>
	 * <br>
	 * <b>iii.Sweep stage</b>
	 */
	public void run()
	{
		// 1.Initialization stage
		initCriticalInst();
		LinkedList<Instruction> worklist = new LinkedList<>(criticalInst);
		MarkVisitor marker = new MarkVisitor();
		// 2.Mark stage
		while (!worklist.isEmpty())
		{
			Instruction curr = worklist.removeLast();
			marker.mark(curr);

			// marks branch instruction.
			markBranch(curr, worklist);
		}
		// 3. Sweep stage
		sweep();

		// peephole optimization
		eliminateDeadBlock();
	}

	/**
	 * Performs sweep stage that removes useless instruction and replace the
	 * branch with an unconditional branch to the nearest and useful dominate
	 * block.
	 */
	private void sweep()
	{
		for (BasicBlock BB : m)
		{
			for (Instruction inst : BB)
			{
				if (!liveInsts.contains(inst))
				{
					// for branch instruction in the basic block, it is special
					// that replaces it with an unconditional branch to it's useful
					// and nearest dominate block.
					if (inst instanceof Instruction.Branch)
					{
						BasicBlock nearestDom = findNearestUsefulPostDom(BB);
						if (nearestDom == BasicBlock.USELESSBLOCK)
							continue;
						Instruction.Goto go = new Instruction.Goto(nearestDom);
						inst.insertBefore(go);
						inst.eraseFromBasicBlock();
					}
					// the function invocation instruction is handled specially
					// for conservative and safe.
					else if (! (inst instanceof Instruction.Invoke))
						inst.eraseFromBasicBlock();
				}
			}
		}
	}

	private void eliminateDeadBlock()
	{
		for (BasicBlock BB: m)
		{
			if (BB.getNumOfPreds() == 0)
			{
				BB.eraseFromParent();
			}
			if (BB.getNumOfPreds() == 1 )
			{
				BasicBlock pred = BB.getPreds().get(0);
				if (pred.getNumOfSuccs() == 0)
					merge(pred, BB);
			}
		}
	}

	/**
	 * Merges the second into first block.
	 * @param first The first block to be merged.
	 * @param second    The second block to be merged.
	 */
	private void merge(BasicBlock first, BasicBlock second)
	{
		for (Instruction inst : second)
		{
			first.appendInst(inst);
		}
		first.removeSuccssor(second);
		for (BasicBlock succ : second.getSuccs())
			first.addSucc(succ);

		// enable the GC.
		second = null;
	}
	/**
	 * Finds the first useful and nearest basic block in the post dominator of
	 * specified Basic Block.
	 * @param BB    The specified basic block.
	 * @return  The nearest and useful post dominator that dominates specified
	 * block.
	 */
	private BasicBlock findNearestUsefulPostDom(BasicBlock BB)
	{
		DominatorTree.DomTreeNode node = DT.getTreeNodeForBlock(BB);
		LinkedList<DominatorTree.DomTreeNode> worklist = new LinkedList<>();
		worklist.add(node.getIDom());
		while (!worklist.isEmpty())
		{
			DominatorTree.DomTreeNode currDOM = worklist.removeLast();
			BasicBlock currBB = currDOM.getBlock();
			if (usefulBlocks.contains(currBB))
				return  currBB;
			worklist.addLast(currDOM.getIDom());
		}
		return BasicBlock.USELESSBLOCK;
	}

	/**
	 * Mark the branch instruction that is the last instruction in the basic block.
	 * @param inst
	 * @param worklist
	 */
	private void markBranch(Instruction inst, LinkedList<Instruction> worklist)
	{
		BasicBlock BB = inst.getParent();
		LinkedList<BasicBlock> rdf = RDF.run(DT, BB);
		for (BasicBlock block : rdf)
		{
			Instruction last = block.lastInst();
			// Only branch instruction will be handled.
			if (last instanceof Instruction.Branch)
			{
				liveInsts.add(last);
				usefulBlocks.add(block);
				worklist.addLast(last);
			}
		}
	}
	/**
	 * Initialize the critical instruction set to be used mark-sweep algorithm.
	 */
	private void initCriticalInst()
	{
		// traverse the CFG with reverse post order.
		Iterator<BasicBlock> itr = m.iterator();
		while (itr.hasNext())
		{
			BasicBlock curr = itr.next();
			for (Instruction inst : curr)
			{
				if (isCritical(inst))
				{
					criticalInst.add(inst);
					usefulBlocks.add(curr);
				}
			}
		}
	}

	/**
	 * Determines whether the specified instruction is a critical or not in term
	 * of HIR.
	 * <br>
	 * In the term of HIR, a critical instruction must meets one of two conditons:
	 * it is either a return statement, or it "may have side effects".
	 * The second condition means that it may write data into memory. That is a
	 * safe and conservative approach due to the difficulty of disambiguating
	 * memory accesses at compile time.
	 *
	 * @param inst
	 * @return
	 */
	private boolean isCritical(Instruction inst)
	{
		if (inst instanceof Return || inst instanceof StoreInst)
			return true;
		else
			return false;
	}

	/**
	 * A concrete instance of super class {@code ValueVisitor}
	 * marks live instruction.
	 */
	private class MarkVisitor extends ValueVisitor
	{
		public void mark(Instruction inst)
		{
			inst.accept(this);
		}

		/**
		 * Visits {@code Instruction} with visitor pattern.
		 *
		 * @param inst The instruction to be visited.
		 */
		public void visitInstruction(Instruction inst)
		{
			assert false;
		}

		private void markBinary(Instruction.Op2 inst)
		{
			if (inst.x instanceof Instruction)
			{
				liveInsts.add((Instruction) inst.x);
				usefulBlocks.add(((Instruction) inst.x).getParent());
			}
			if (inst.y instanceof Instruction)
			{
				liveInsts.add((Instruction) inst.y);
				usefulBlocks.add(((Instruction) inst.y).getParent());
			}
		}

		private void markUnary(Instruction.Op1 inst)
		{
			if (inst.x instanceof Instruction)
			{
				liveInsts.add((Instruction) inst.x);
				usefulBlocks.add(((Instruction) inst.x).getParent());
			}
		}
		/**
		 * Visits {@code ADD_I} with visitor pattern.
		 *
		 * @param inst The ADD_I to be visited.
		 */
		public void visitADD_I(Instruction.ADD_I inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code SUB_I} with visitor pattern.
		 *
		 * @param inst The SUB_I to be visited.
		 */
		public void visitSUB_I(Instruction.SUB_I inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code MUL_I} with visitor pattern.
		 *
		 * @param inst The MUL_I to be visited.
		 */
		public void visitMUL_I(Instruction.MUL_I inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code DIV_I} with visitor pattern.
		 *
		 * @param inst The DIV_I to be visited.
		 */
		public void visitDIV_I(Instruction.DIV_I inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code MOD_I} with visitor pattern.
		 *
		 * @param inst The MOD_I to be visited.
		 */
		public void visitMOD_I(Instruction.MOD_I inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code AND_I} with visitor pattern.
		 *
		 * @param inst The AND_I to be visited.
		 */
		public void visitAND_I(Instruction.AND_I inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code OR_I} with visitor pattern.
		 *
		 * @param inst The OR_I to be visited.
		 */
		public void visitOR_I(Instruction.OR_I inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code XOR_I} with visitor pattern.
		 *
		 * @param inst The XOR_I to be visited.
		 */
		public void visitXOR_I(Instruction.XOR_I inst)
		{
			markBinary(inst);
		}

		@Override
		public void visitShiftOp(Instruction.ShiftOp inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code ADD_L} with visitor pattern.
		 *
		 * @param inst The ADD_L to be visited.
		 */
		public void visitADD_L(Instruction.ADD_L inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code SUB_I} with visitor pattern.
		 *
		 * @param inst The SUB_I to be visited.
		 */
		public void visitSUB_L(Instruction.SUB_L inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code MUL_L} with visitor pattern.
		 *
		 * @param inst The MUL_L to be visited.
		 */
		public void visitMUL_L(Instruction.MUL_L inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code DIV_L} with visitor pattern.
		 *
		 * @param inst The DIV_L to be visited.
		 */
		public void visitDIV_L(Instruction.DIV_L inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code MOD_L} with visitor pattern.
		 *
		 * @param inst The MOD_L to be visited.
		 */
		public void visitMOD_L(Instruction.MOD_L inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code AND_L} with visitor pattern.
		 *
		 * @param inst The AND_L to be visited.
		 */
		public void visitAND_L(Instruction.AND_L inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code OR_L} with visitor pattern.
		 *
		 * @param inst The OR_L to be visited.
		 */
		public void visitOR_L(Instruction.OR_L inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code XOR_L} with visitor pattern.
		 *
		 * @param inst The XOR_L to be visited.
		 */
		public void visitXOR_L(Instruction.XOR_L inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code ADD_F} with visitor pattern.
		 *
		 * @param inst The ADD_F to be visited.
		 */
		public void visitADD_F(Instruction.ADD_F inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code SUB_F} with visitor pattern.
		 *
		 * @param inst The SUB_F to be visited.
		 */
		public void visitSUB_F(Instruction.SUB_F inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code MUL_F} with visitor pattern.
		 *
		 * @param inst The MUL_F to be visited.
		 */
		public void visitMUL_F(Instruction.MUL_F inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code DIV_F} with visitor pattern.
		 *
		 * @param inst The DIV_F to be visited.
		 */
		public void visitDIV_F(Instruction.DIV_F inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code ADD_D} with visitor pattern.
		 *
		 * @param inst The ADD_D to be visited.
		 */
		public void visitADD_D(Instruction.ADD_D inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code SUB_D} with visitor pattern.
		 *
		 * @param inst The SUB_D to be visited.
		 */
		public void visitSUB_D(Instruction.SUB_D inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code MUL_D} with visitor pattern.
		 *
		 * @param inst The MUL_D to be visited.
		 */
		public void visitMUL_D(Instruction.MUL_D inst)
		{
			markBinary(inst);
		}

		/**
		 * Visits {@code DIV_D} with visitor pattern.
		 *
		 * @param inst The DIV_D to be visited.
		 */
		public void visitDIV_D(Instruction.DIV_D inst)
		{
			markBinary(inst);
		}


		/**
		 * Visits {@code NEG_I} with vistor pattern.
		 *
		 * @param inst The inst to be visited.
		 */
		public void visitNEG_I(Instruction.NEG_I inst)
		{
			markUnary(inst);
		}

		/**
		 * Visits {@code NEG_L} with vistor pattern.
		 *
		 * @param inst The inst to be visited.
		 */
		public void visitNEG_L(Instruction.NEG_L inst)
		{
			markUnary(inst);
		}

		/**
		 * Visits {@code NEG_F} with vistor pattern.
		 *
		 * @param inst The inst to be visited.
		 */
		public void visitNEG_F(Instruction.NEG_F inst)
		{
			markUnary(inst);
		}

		/**
		 * Visits {@code NEG_D} with vistor pattern.
		 *
		 * @param inst The inst to be visited.
		 */
		public void visitNEG_D(Instruction.NEG_D inst)
		{
			markUnary(inst);
		}

		public void visitConvert(Instruction.INT_2LONG inst)
		{
			markUnary(inst);
		}

		public void visitINT_2FLOAT(Instruction.INT_2FLOAT inst)
		{
			markUnary(inst);
		}

		public void visitINT_2DOUBLE(Instruction.INT_2DOUBLE inst)
		{
			markUnary(inst);
		}

		public void visitLONG_2INT(Instruction.LONG_2INT inst)
		{
			markUnary(inst);
		}

		public void visitLONG_2FLOAT(Instruction.LONG_2FLOAT inst)
		{
			markUnary(inst);
		}

		public void visitLONG_2DOUBLE(Instruction.LONG_2DOUBLE inst)
		{
			markUnary(inst);
		}

		public void visitFLOAT_2INT(Instruction.FLOAT_2INT inst)
		{
			markUnary(inst);
		}

		public void visitFLOAT_2LONG(Instruction.FLOAT_2LONG inst)
		{
			markUnary(inst);
		}

		public void visitFLOAT_2DOUBLE(Instruction.FLOAT_2DOUBLE inst)
		{
			markUnary(inst);
		}

		public void visitDOUBLE_2INT(Instruction.DOUBLE_2INT inst)
		{
			markUnary(inst);
		}

		public void visitDOUBLE_2LONG(Instruction.DOUBLE_2LONG inst)
		{
			markUnary(inst);
		}

		public void visitDOUBLE_2FLOAT(Instruction.DOUBLE_2FLOAT inst)
		{
			markUnary(inst);
		}

		public void visitINT_2BYTE(Instruction.INT_2BYTE inst)
		{
			markUnary(inst);
		}

		public void visitINT_2CHAR(Instruction.INT_2CHAR inst)
		{
			markUnary(inst);
		}

		public void visitINT_2SHORT(Instruction.INT_2SHORT inst)
		{
			markUnary(inst);
		}

		public void visitBR(Instruction.BR inst)
		{
			visitInstruction(inst);
		}

		public void visitICmp(Instruction.ICmp inst)
		{
			markBinary(inst);
		}

		public void visitLCmp(Instruction.LCmp inst)
		{
			markBinary(inst);
		}

		public void visitFCmp(Instruction.FCmp inst)
		{
			markBinary(inst);
		}

		public void visitDCmp(Instruction.DCmp inst)
		{
			markBinary(inst);
		}

		public void visitIfCmp_LT(Instruction.IfCmp_LT inst)
		{
			visitInstruction(inst);
		}

		public void visitIfCmp_LE(Instruction.IfCmp_LE inst)
		{
			visitInstruction(inst);
		}

		public void visitIfCmp_GT(Instruction.IfCmp_GT inst)
		{
			visitInstruction(inst);
		}

		public void visitIfCmp_GE(Instruction.IfCmp_GE inst)
		{
			visitInstruction(inst);
		}

		public void visitIfCmp_EQ(Instruction.IfCmp_EQ inst)
		{
			visitInstruction(inst);
		}

		public void visitIfCmp_NEQ(Instruction.IfCmp_NEQ inst)
		{
			visitInstruction(inst);
		}

		public void visitGoto(Instruction.Goto inst)
		{
			visitInstruction(inst);
		}

		public void visitReturn(Return inst)
		{
			visitInstruction(inst);
		}

		public void visitInvoke(Instruction.Invoke inst)
		{
			visitInstruction(inst);
		}

		public void visitPhi(Instruction.Phi inst)
		{
			BasicBlock[] blocks = inst.getAllBasicBlocks();
			for (int idx =0; idx < blocks.length; idx++)
			{
				Instruction lastInst = blocks[idx].lastInst();
				if (lastInst instanceof Instruction.Branch)
				{
					liveInsts.add(lastInst);
					usefulBlocks.add(blocks[idx]);
				}
			}
		}

		public void visitAlloca(Instruction.Alloca inst)
		{
			visitInstruction(inst);
		}

		public void visitStoreInst(StoreInst inst)
		{
			visitInstruction(inst);
		}

		public void visitLoadInst(Instruction.LoadInst inst)
		{
			visitInstruction(inst);
		}
	}
}
