package hir;

/**
 * Interface for the basic block visitor design pattern. Make your visitor
 * object implement this class in order to visit
 * 
 * @see BasicBlock
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 1.0
 */
public interface BasicBlockVisitor {

	/**
	 * Visit a basic block.
	 * 
	 * @param bb
	 *            basic block to visit
	 */
	void visitBasicBlock(BasicBlock bb);

	/**
	 * Empty basic block visitor for easy subclassing.
	 */
	class EmptyVisitor implements BasicBlockVisitor {
		/**
		 * Visit a basic block.
		 * 
		 * @param bb
		 *            basic block to visit
		 */
		public void visitBasicBlock(BasicBlock bb) {
		}
	}

	/**
	 * Control flow graph visitor that visits all basic blocks in the CFG with a
	 * given
	 * 
	 * basic block visitor.
	 * 
	 * @see ControlFlowGraph
	 * @see ControlFlowGraphVisitor
	 */
	class AllBasicBlockVisitor implements ControlFlowGraphVisitor {

		private final BasicBlockVisitor bbv;
		boolean trace;

		/**
		 * Construct a new AllBasicBlockVisitor.
		 * 
		 * @param bbv
		 *            basic block visitor to visit each basic block with.
		 */
		public AllBasicBlockVisitor(BasicBlockVisitor bbv) {
			this.bbv = bbv;
		}

		/**
		 * Construct a new AllBasicBlockVisitor and set the trace flag to be the
		 * specified inst.
		 * 
		 * @param bbv
		 *            basic block visitor to visit each basic block with.
		 * @param trace
		 *            inst of the trace flag
		 */
		public AllBasicBlockVisitor(BasicBlockVisitor bbv, boolean trace) {
			this.bbv = bbv;
			this.trace = trace;
		}

		/**
		 * Visit each of the basic blocks in the given control flow graph.
		 * 
		 * @param cfg
		 *            control flow graph to visit
		 */
		public void visitCFG(ControlFlowGraph cfg) {
			cfg.visitBasicBlocks(bbv);
		}
	}
}
