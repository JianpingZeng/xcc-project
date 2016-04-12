package lir.backend;

import driver.Backend;
import lir.LIRAssembler;
import lir.LIRGenerator;

/**
 * @author Jianping Zeng
 */
public abstract class Target
{
	public final Backend backend;

	private Target(Backend backend)
	{
		this.backend = backend;
	}

	public static Target create(TargetMachine target)
	{
		return null;
	}

	public abstract LIRGenerator newLIRGenerator();
	public abstract LIRAssembler newLIRAssember();

}
