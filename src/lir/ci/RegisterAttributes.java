package lir.ci;

import lir.backend.RegisterConfig;

import java.util.Arrays;

/**
 * A collection of register attributes. The specific attribute values for a
 * register may be local to a compilation context. For example, a {@link RegisterConfig}
 * in use during a compilation will determine which registers are callee saved.
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
	 * Creates a map from register {@linkplain Register#number numbers} to register
	 * {@linkplain RegisterAttributes attributes} for a given register configuration
	 * and set of registers.
	 *
	 * @param registerConfig a register configuration
	 * @param registers      a set of registers
	 * @return an array whose length is the max register number in {@code registers}
	 * plus 1. An element at index i holds the attributes of the register whose
	 * number is i.
	 */
	public static RegisterAttributes[] createMap(
			RegisterConfig registerConfig, Register[] registers)
	{
		RegisterAttributes[] map = new RegisterAttributes[registers.length];
		for (Register reg : registers)
		{
			if (reg != null)
			{
				CalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
				RegisterAttributes attr = new RegisterAttributes(
						Arrays.asList(registerConfig.getCallerSaveRegisters())
								.contains(reg), csl == null ?
						false :
						Arrays.asList(csl.registers).contains(reg),
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
