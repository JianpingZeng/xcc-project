package lir;

import driver.Backend;
import hir.BasicBlock;
import hir.HIR;
import hir.ValueVisitor;

/**
 * @author Jianping Zeng
 */
public class LIRGenerator extends ValueVisitor
{
	private final Backend backend;
	private final HIR[] hirs;
	private BasicBlock currentBlock;


	public LIRGenerator(Backend backend, HIR[] hirs)
	{
		this.backend = backend;
		this.hirs = hirs;
	}

	public void doBlock(BasicBlock block)
	{

	}

	/**
	 * Inserts prolog code in the entry block of given compiled function.
	 * @param block
	 */
	private void blockDoProlog(BasicBlock block)
	{
		// print debug information

		assert block.getMachineBlock() == null :
				"Machine block already be computed for this block";
		LIRList lir = new LIRList();
		block.setLIR(lir);

		lir.branchDesination(block.label());
	}
}
