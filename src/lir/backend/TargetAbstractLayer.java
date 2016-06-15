package lir.backend;

import driver.Backend;
import hir.Method;
import lir.LIRAssembler;
import lir.LIRGenerator;
import lir.backend.amd64.AMD64;
import lir.backend.amd64.AMD64TargetAbstractLayer;
import lir.backend.x86.X86;
import lir.backend.x86.X86TargetAbstractLayer;

/**
 * @author Xlous.zeng
 */
public abstract class TargetAbstractLayer
{
	public final Backend backend;

	protected TargetAbstractLayer(Backend backend)
	{
		this.backend = backend;
	}

	public static TargetAbstractLayer create(Architecture arch, Backend backend)
	{
		if (arch instanceof AMD64)
			return new AMD64TargetAbstractLayer(backend);
		if (arch instanceof X86)
			return new X86TargetAbstractLayer(backend);
		return null;
	}

	public abstract LIRGenerator newLIRGenerator(Method m);
	public abstract LIRAssembler newLIRAssember();

}
