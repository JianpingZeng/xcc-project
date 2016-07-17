package lir.backend.amd64;

import hir.Method;
import lir.LIRAssembler;
import lir.LIRGenerator;
import lir.backend.TargetAbstractLayer;
import compiler.*;

/**
 * @author Xlous.zeng
 */
public class AMD64TargetAbstractLayer extends TargetAbstractLayer
{
	public AMD64TargetAbstractLayer(Backend backend)
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
