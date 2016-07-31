package lir.backend.amd64;

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
	@Override public LIRGenerator newLIRGenerator()
	{
		return new AMD64LIRGenerator(backend);
	}

	@Override public LIRAssembler newLIRAssember()
	{
		return null;
	}
}
