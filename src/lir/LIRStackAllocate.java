package lir;

import lir.ci.LIRValue;
import lir.StackFrame.StackBlock;
/**
 * LIR instruction used in translating {@link hir.Instruction.Alloca}.
 *
 * @author Xlous.zeng
 */
public class LIRStackAllocate extends LIRInstruction
{

	public final StackBlock stackBlock;

	/**
	 * Creates an LIR instruction modelling a stack block allocation.
	 *
	 * @param result
	 */
	public LIRStackAllocate(LIRValue result, StackBlock stackBlock)
	{
		super(LIROpcode.Alloca, result, false);
		this.stackBlock = stackBlock;
	}

	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitStackAllocate(stackBlock, this.result());
	}
}
