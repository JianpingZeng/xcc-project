package lir.backend;

import java.util.EnumMap;

import lir.ci.CalleeSaveLayout;
import lir.ci.CallingConvention;
import lir.ci.CallingConvention.Type;
import lir.ci.LIRKind;
import lir.ci.LIRRegister;
import lir.ci.LIRRegister.RegisterFlag;
import lir.ci.RegisterAttributes;

/**
 * A register configuration binds roles and {@linkplain lir.ci.RegisterAttributes
 * attributes} to physical LIRRegisters.
 *
 * @author Xlous.zeng
 */
public interface RegisterConfig
{
	/**
	 * Gets the register to be used for returning a value of a given kind.
	 */
	LIRRegister getReturnRegister(LIRKind kind);

	/**
	 * Gets the register to which {@link LIRRegister#Frame} and {@link LIRRegister
	 * #CallerFrame} are bound.
	 */
	LIRRegister getFrameRegister();

	LIRRegister getScratchRegister();

	/**
	 * Gets the calling convention describing how arguments are passed.
	 *
	 * @param type       the type of calling convention being requested
	 * @param parameters the types of the arguments of the call
	 * @param target     the targetAbstractLayer platform
	 * @param stackOnly  ignore LIRRegisters
	 */
	CallingConvention getCallingConvention(CallingConvention.Type type, LIRKind[] parameters,
			TargetMachine target, boolean stackOnly);

	/**
	 * Gets the ordered set of LIRRegisters that are can be used to pass parameters
	 * according to a given calling convention.
	 *
	 * @param type the type of calling convention
	 * @param flag specifies whether LIRRegisters for {@linkplain LIRRegister.RegisterFlag#
	 *              CPU integral} or {@linkplain} RegisterFlag#FPU floating
	 *              point} parameters are being requested
	 * @return the ordered set of LIRRegisters that may be used to pass parameters
	 *              in a call conforming to {@code type}
	 */
	LIRRegister[] getCallingConventionRegisters(Type type, LIRRegister.RegisterFlag flag);

	/**
	 * Gets the set of LIRRegisters that can be used by the register allocator.
	 */
	LIRRegister[] getAllocatableRegisters();

	/**
	 * Gets the set of LIRRegisters that can be used by the register allocator,
	 * {@linkplain LIRRegister#categorize(LIRRegister[]) categorized} by register
	 * {@linkplain RegisterFlag flags}.
	 *
	 * @return a map from each {@link RegisterFlag} constant to the list of
	 * {@linkplain #getAllocatableRegisters() allocatable} LIRRegisters for which
	 * the flag is RegisterFlag setted}
	 */
	EnumMap<RegisterFlag, LIRRegister[]> getCategorizedAllocatableRegisters();

	/**
	 * Gets the LIRRegisters whose VALUES must be preserved by a method across any call it makes.
	 */
	LIRRegister[] getCallerSaveRegisters();

	/**
	 * Gets the layout of the callee save area of this register configuration.
	 *
	 * @return {@code null} if there is no callee save area
	 */
	CalleeSaveLayout getCalleeSaveLayout();

	/**
	 * Gets a map from register {@linkplain LIRRegister#number numbers} to register
	 * {@linkplain RegisterAttributes attributes} for this register configuration.
	 *
	 * @return an array where an element at index i holds the attributes of the register whose number is i
	 * @see LIRRegister#categorize(LIRRegister[])
	 */
	RegisterAttributes[] getAttributesMap();

	/**
	 * Gets the register corresponding to a runtime-defined role.
	 *
	 * @param id the identifier of a runtime-defined register role
	 * @return the register playing the role specified by {@code id}
	 */
	LIRRegister getRegisterForRole(int id);
}
