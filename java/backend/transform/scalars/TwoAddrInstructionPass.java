package backend.transform.scalars;

import backend.analysis.LiveVariable;
import backend.analysis.MachineDomTreeInfo;
import backend.analysis.MachineLoopInfo;
import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionPass;
import backend.pass.AnalysisUsage;

/**
 * <pre>
 * This file implements the TwoAddress instruction pass which is used
 * by most register allocators. Two-Address instructions are rewritten
 * from:
 *
 *     A = B op C
 *
 * to:
 *
 *     A = B
 *     A op= C
 * </pre>
 * <p>
 * Note that if a register allocator chooses to use this pass, that it
 * has to be capable of handling the non-SSA nature of these rewritten
 * virtual registers.
 * </p>
 * <p>
 * It is also worth noting that the duplicate operand of the two
 * address instruction is removed.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TwoAddrInstructionPass extends MachineFunctionPass
{
	@Override
	public String getPassName()
	{
		return "Two addr instruction pass";
	}

	@Override
	public boolean runOnMachineFunction(MachineFunction mf)
	{
		return false;
	}

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(LiveVariable.class);
		au.addPreserved(LiveVariable.class);
		au.addPreserved(MachineLoopInfo.class);
		au.addPreserved(MachineDomTreeInfo.class);
		au.addRequired(PhiElimination.class);
		super.getAnalysisUsage(au);
	}
}
