package lir.backend.x86;

import driver.Backend;
import hir.Method;
import lir.LIRAssembler;
import lir.LIRGenerator;
import lir.backend.TargetAbstractLayer;
import lir.backend.amd64.AMD64LIRGenerator;

/**
 * @author Jianping Zeng
 */
public class X86TAL extends TargetAbstractLayer
{
	public X86TAL(Backend backend)
	{
		super(backend);
	}
	@Override public LIRGenerator newLIRGenerator(Method m)
	{
		return new AMD64LIRGenerator(backend, m);
	}

	@Override public LIRAssembler newLIRAssember()
	{
		return null;
	}
}

