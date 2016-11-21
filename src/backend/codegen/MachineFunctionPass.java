package backend.codegen;

import backend.pass.FunctionPass;
import backend.value.Function;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class MachineFunctionPass extends FunctionPass
{
	/**
	 * This method must be overridded by concrete subclass for performing
	 * desired machine code transformation or analysis.
	 * @param mf
	 * @return
	 */
	public abstract boolean runOnMachineFunction(MachineFunction mf);

	/**
	 * This method will be passed by {@linkplain #runOnMachineFunction(MachineFunction)}
	 * @param f
	 * @return
	 */
	@Override
	public boolean runOnFunction(Function f)
	{
		MachineFunction mf = f.getMachineFunc();
		assert mf != null:"Instruction selector did not be runned?";
		return runOnMachineFunction(mf);
	}
}
