package backend.lir;

import hir.Instruction;
import backend.lir.ci.LIRValue;

/**
 * LIR instruction used in translating {@link Instruction.AllocaInst}.
 *
 * @author Xlous.zeng
 */
public class LIRStackAllocate extends LIRInstruction
{

	public final StackFrame.StackBlock stackBlock;

	/**
	 * Creates an LIR instruction modelling a stack block allocation.
	 *
	 * @param result
	 */
	public LIRStackAllocate(LIRValue result, StackFrame.StackBlock stackBlock)
	{
		super(LIROpcode.Alloca, result, false);
		this.stackBlock = stackBlock;
	}

	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitStackAllocate(stackBlock, this.getReturnValue());
	}
}
