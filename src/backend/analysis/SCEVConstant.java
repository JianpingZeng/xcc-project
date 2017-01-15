package backend.analysis;

import backend.hir.BasicBlock;
import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantInt;

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SCEVConstant extends SCEV
{
	private ConstantInt value;
	private SCEVConstant(ConstantInt val)
	{
		super(SCEVType.scConstant);
		value = val;
	}

	public ConstantInt getValue()
	{
		return value;
	}

	/**
	 * Returns true if the value is invariant in the specified loop.
	 *
	 * @param loop
	 * @return
	 */
	@Override
	public boolean isLoopInvariant(Loop loop)
	{
		return true;
	}

	/**
	 * Checks to see whether this SCEV changes value in a known method in the
	 * specified loop. This properly being true implies that the value is variant
	 * in the loop and that we can emit an expression to compute the value of
	 * the expression at any particular loop iteration.
	 *
	 * @param loop
	 * @return
	 */
	@Override
	public boolean hasComputableLoopEvolution(Loop loop)
	{
		return false;
	}

	/**
	 * If this SCEV internally references the symbolic value {@code sym},
	 * construct and return a new SCEV that produces the same value, but
	 * which uses the concrete value {@code concrete} instead of the
	 * symbolic value. If this SCEV does not use the symbolic value,
	 * it returns itself.
	 *
	 * @param sym
	 * @param concrete
	 * @return
	 */
	@Override
	public SCEV replaceSymbolicValuesWithConcrete(SCEV sym,
			SCEV concrete)
	{
		return this;
	}

	/**
	 * Returns the LLVM type of this SCEV value.
	 *
	 * @return
	 */
	@Override
	public Type getType()
	{
		return value.getType();
	}

	@Override
	public void print(PrintStream os)
	{

	}
}
