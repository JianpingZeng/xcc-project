package lir.ci;

import java.util.Arrays;

import lir.backend.RegisterConfig;

/**
 * A collection of register attributes. The specific attribute VALUES for a
 * register may be local to a compilation context. For example, a {@link RegisterConfig}
 * in use during a compilation will determine which LIRRegisters are callee saved.
 */
public class RegisterAttributes
{

	/**
	 * Denotes a register whose value preservation (if required) across a call is
	 * the responsibility of the caller.
	 */
	public final boolean isCallerSave;

	/**
	 * Denotes a register whose value preservation (if required) across a call is
	 * the responsibility of the callee.
	 */
	public final boolean isCalleeSave;

	/**
	 * Denotes a register that is available for use by a register allocator.
	 */
	public final boolean isAllocatable;

	/**
	 * Denotes a register guaranteed to be non-zero if read in compiled Java code.
	 * For example, a register dedicated to holding the current thread.
	 */
	public boolean isNonZero;

	public RegisterAttributes(boolean isCallerSave, boolean isCalleeSave,
			boolean isAllocatable)
	{
		this.isCallerSave = isCallerSave;
		this.isCalleeSave = isCalleeSave;
		this.isAllocatable = isAllocatable;
	}

	public static final RegisterAttributes NONE = new RegisterAttributes(false,
			false, false);

	/**
	 * Creates a map from register {@linkplain LIRRegister#number numbers} to register
	 * {@linkplain RegisterAttributes attributes} for a given register configuration
	 * and set of LIRRegisters.
	 *
	 * @param registerConfig a register configuration
	 * @param LIRRegisters      a set of LIRRegisters
	 * @return an array whose getArraySize is the max register number in {@code LIRRegisters}
	 * plus 1. An element at index i holds the attributes of the register whose
	 * number is i.
	 */
	public static RegisterAttributes[] createMap(
			RegisterConfig registerConfig, LIRRegister[] LIRRegisters)
	{
		RegisterAttributes[] map = new RegisterAttributes[LIRRegisters.length];
		for (LIRRegister reg : LIRRegisters)
		{
			if (reg != null)
			{
				CalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
				RegisterAttributes attr = new RegisterAttributes(
						Arrays.asList(registerConfig.getCallerSaveRegisters())
								.contains(reg),
                        csl != null && Arrays.asList(csl.LIRRegisters)
                                .contains(reg),
						Arrays.asList(registerConfig.getAllocatableRegisters())
								.contains(reg));
				if (map.length <= reg.number)
				{
					map = Arrays.copyOf(map, reg.number + 1);
				}
				map[reg.number] = attr;
			}
		}
		for (int i = 0; i < map.length; i++)
		{
			if (map[i] == null)
			{
				map[i] = NONE;
			}
		}
		return map;
	}
}
