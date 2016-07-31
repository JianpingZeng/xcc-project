package lir.backend.x86;

import lir.LIRAssembler;
import lir.LIRGenerator;
import lir.backend.TargetAbstractLayer;
import lir.backend.amd64.AMD64LIRGenerator;
import compiler.*;

/**
 * @author Xlous.zeng
 */
public final class X86TargetAbstractLayer extends TargetAbstractLayer
{
	public X86TargetAbstractLayer(Backend backend)
	{
		super(backend);
	}
	
	@Override 
	public LIRGenerator newLIRGenerator()
	{
		return new AMD64LIRGenerator(backend);
	}

	@Override 
	public LIRAssembler newLIRAssember()
	{
		return null;
	}
}

