package lir.backend;

import lir.ci.*;
import java.util.EnumMap;
import static lir.ci.Register.RegisterFlag;
import lir.ci.CallingConvention.Type;

/**
 * A register configuration binds roles and {@linkplain lir.ci.RegisterAttributes
 * attributes} to physical registers.
 *
 * @author Jianping Zeng
 */
public interface RegisterConfig
{
	/**
	 * Gets the register to be used for returning a value of a given kind.
	 */
	Register getReturnRegister(CiKind kind);

	/**
	 * Gets the register to which {@link Register#Frame} and {@link Register
	 * #CallerFrame} are bound.
	 */
	Register getFrameRegister();

	Register getScratchRegister();

	/**
	 * Gets the calling convention describing how arguments are passed.
	 *
	 * @param type       the type of calling convention being requested
	 * @param parameters the types of the arguments of the call
	 * @param target     the target platform
	 * @param stackOnly  ignore registers
	 */
	CallingConvention getCallingConvention(CallingConvention.Type type, CiKind[] parameters,
			TargetMachine target, boolean stackOnly);

	/**
	 * Gets the ordered set of registers that are can be used to pass parameters
	 * according to a given calling convention.
	 *
	 * @param type the type of calling convention
	 * @param flag specifies whether registers for {@linkplain Register.RegisterFlag#
	 *              CPU integral} or {@linkplain} RegisterFlag#FPU floating
	 *              point} parameters are being requested
	 * @return the ordered set of registers that may be used to pass parameters
	 *              in a call conforming to {@code type}
	 */
	Register[] getCallingConventionRegisters(Type type, Register.RegisterFlag flag);

	/**
	 * Gets the set of registers that can be used by the register allocator.
	 */
	Register[] getAllocatableRegisters();

	/**
	 * Gets the set of registers that can be used by the register allocator,
	 * {@linkplain Register#categorize(Register[]) categorized} by register
	 * {@linkplain RegisterFlag flags}.
	 *
	 * @return a map from each {@link RegisterFlag} constant to the list of
	 * {@linkplain #getAllocatableRegisters() allocatable} registers for which
	 * the flag is RegisterFlag setted}
	 */
	EnumMap<RegisterFlag, Register[]> getCategorizedAllocatableRegisters();

	/**
	 * Gets the registers whose values must be preserved by a method across any call it makes.
	 */
	Register[] getCallerSaveRegisters();

	/**
	 * Gets the layout of the callee save area of this register configuration.
	 *
	 * @return {@code null} if there is no callee save area
	 */
	CalleeSaveLayout getCalleeSaveLayout();

	/**
	 * Gets a map from register {@linkplain Register#number numbers} to register
	 * {@linkplain RegisterAttributes attributes} for this register configuration.
	 *
	 * @return an array where an element at index i holds the attributes of the register whose number is i
	 * @see Register#categorize(Register[])
	 */
	RegisterAttributes[] getAttributesMap();

	/**
	 * Gets the register corresponding to a runtime-defined role.
	 *
	 * @param id the identifier of a runtime-defined register role
	 * @return the register playing the role specified by {@code id}
	 */
	Register getRegisterForRole(int id);
}
