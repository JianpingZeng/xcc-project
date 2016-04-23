package lir.backend.x86;

import lir.backend.RegisterConfig;
import lir.backend.TargetMachine;
import lir.ci.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import static lir.backend.x86.X86.*;

/**
 * @author Jianping Zeng
 */
public final class X86RegisterConfig implements RegisterConfig
{
	/**
	 * The object describing the callee save area of this register configuration.
	 */
	public CalleeSaveLayout csl;

	/**
	 * The minimum register role identifier.
	 */
	public final int minRole;

	/**
	 * The map from register role IDs to LIRRegisters.
	 */
	public final LIRRegister[] registersRoleMap;

	/**
	 * The set of LIRRegisters that can be used by the register allocator.
	 */
	public final LIRRegister[] allocatable;

	/**
	 * The set of LIRRegisters that can be used by the register allocator,
	 * {@linkplain LIRRegister#categorize(LIRRegister[]) categorized} by register
	 * {@linkplain LIRRegister.RegisterFlag flags}.
	 */
	public final EnumMap<LIRRegister.RegisterFlag, LIRRegister[]> categorized;

	/**
	 * The ordered set of LIRRegisters used to pass integral arguments.
	 */
	public final LIRRegister[] cpuParameters;

	/**
	 * The ordered set of LIRRegisters used to pass floating point arguments.
	 */
	public final LIRRegister[] fpuParameters;

	/**
	 * The caller saved LIRRegisters.
	 */
	public final LIRRegister[] callerSave;

	/**
	 * The register to which {@link LIRRegister#Frame} and {@link LIRRegister#CallerFrame}
	 * are bound.
	 */
	public final LIRRegister frame;

	/**
	 * LIRRegister for returning an integral value.
	 */
	public final LIRRegister integralReturn;

	/**
	 * LIRRegister for returning a floating point value.
	 */
	public final LIRRegister floatingPointReturn;

	/**
	 * The map from register {@linkplain LIRRegister#number numbers} to register
	 * {@linkplain RegisterAttributes attributes} for this register configuration.
	 */
	public final RegisterAttributes[] attributesMap;

	/**
	 * The scratch register.
	 */
	public final LIRRegister scratch;

	/**
	 * The frame offset of the first stack argument for each calling convention
	 * {@link CallingConvention.Type}.
	 */
	public final int[] stackArg0Offsets = new int[CallingConvention.Type.VALUES.length];

	private static X86RegisterConfig instance = null;

	/**
	 * Gets only a instance of {@code X86RegisterConfig} by this factory method.
	 * @return
	 */
	public static X86RegisterConfig newInstance()
	{
		if (instance == null)
		{
			instance = new X86RegisterConfig(ebp, eax, xmm0, ebx,
					new LIRRegister[] { eax, ebx, ecx, edx, esi, edi},
					new LIRRegister[] { eax, edx, ecx },
					new LIRRegister[] { ecx, edx, esi, edi},
					new CalleeSaveLayout(0, 4, -1,
							new LIRRegister[] { ebx, esi, edi }),
					ALL_LIR_REGISTERs, Collections
					.<Integer, LIRRegister>emptyMap());
		}
		return instance;
	}

	private X86RegisterConfig(LIRRegister frame, LIRRegister integralReturn,
			LIRRegister floatingPointReturn, LIRRegister scratch,
			LIRRegister[] allocatable, LIRRegister[] callerSave,
			LIRRegister[] parameters, CalleeSaveLayout csl,
			LIRRegister[] allLIRRegisters, Map<Integer, LIRRegister> roles)
	{
		this.frame = frame;
		this.csl = csl;
		this.allocatable = allocatable;
		this.callerSave = callerSave;
		assert !Arrays.asList(allocatable).contains(scratch);
		this.scratch = scratch;
		EnumMap<LIRRegister.RegisterFlag, LIRRegister[]> categorizedParameters = LIRRegister
				.categorize(parameters);
		this.cpuParameters = categorizedParameters.get(LIRRegister.RegisterFlag.CPU);
		this.fpuParameters = categorizedParameters.get(LIRRegister.RegisterFlag.FPU);
		categorized = LIRRegister.categorize(allocatable);
		attributesMap = RegisterAttributes.createMap(this, allLIRRegisters);
		this.floatingPointReturn = floatingPointReturn;
		this.integralReturn = integralReturn;
		int minRoleId = Integer.MAX_VALUE;
		int maxRoleId = Integer.MIN_VALUE;
		for (Map.Entry<Integer, LIRRegister> e : roles.entrySet())
		{
			int id = e.getKey();
			assert id >= 0;
			if (minRoleId > id)
			{
				minRoleId = id;
			}
			if (maxRoleId < id)
			{
				maxRoleId = id;
			}
		}
		registersRoleMap = new LIRRegister[(maxRoleId - minRoleId) + 1];
		for (Map.Entry<Integer, LIRRegister> e : roles.entrySet())
		{
			int id = e.getKey();
			registersRoleMap[id] = e.getValue();
		}
		minRole = minRoleId;
	}

	public X86RegisterConfig(X86RegisterConfig src, CalleeSaveLayout csl)
	{
		this.frame = src.frame;
		this.csl = csl;
		this.allocatable = src.allocatable;
		this.callerSave = src.callerSave;
		this.scratch = src.scratch;
		this.cpuParameters = src.cpuParameters;
		this.fpuParameters = src.fpuParameters;
		this.categorized = src.categorized;
		this.attributesMap = src.attributesMap;
		this.floatingPointReturn = src.floatingPointReturn;
		this.integralReturn = src.integralReturn;
		this.registersRoleMap = src.registersRoleMap;
		this.minRole = src.minRole;
		System.arraycopy(src.stackArg0Offsets, 0, stackArg0Offsets, 0,
				stackArg0Offsets.length);
	}
	/**
	 * Gets the register to be used for returning a value of a given kind.
	 *
	 * @param kind
	 */
	public LIRRegister getReturnRegister(LIRKind kind)
	{
		if (kind.isDouble() || kind.isFloat())
		{
			return floatingPointReturn;
		}
		return integralReturn;
	}

	public LIRRegister getFrameRegister()
	{
		return frame;
	}

	public LIRRegister getScratchRegister()
	{
		return scratch;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation assigns all available LIRRegisters to parameters before
	 * assigning any stack slots to parameters.
	 */
	public CallingConvention getCallingConvention(CallingConvention.Type type,
			LIRKind[] parameters, TargetMachine target, boolean stackOnly)
	{
		LIRValue[] locations = new LIRValue[parameters.length];

		int currentGeneral = 0;
		int currentXMM = 0;
		int firstStackIndex =
				(stackArg0Offsets[type.ordinal()]) / target.spillSlotSize;
		int currentStackIndex = firstStackIndex;

		for (int i = 0; i < parameters.length; i++)
		{
			final LIRKind kind = parameters[i];

			switch (kind)
			{
				case Byte:
				case Boolean:
				case Short:
				case Char:
				case Int:
				case Long:
				case Object:
					if (!stackOnly && currentGeneral < cpuParameters.length)
					{
						LIRRegister LIRRegister = cpuParameters[currentGeneral++];
						locations[i] = LIRRegister.asValue(kind);
					}
					break;

				case Float:
				case Double:
					if (!stackOnly && currentXMM < fpuParameters.length)
					{
						LIRRegister LIRRegister = fpuParameters[currentXMM++];
						locations[i] = LIRRegister.asValue(kind);
					}
					break;

				default:
					throw new InternalError(
							"Unexpected parameter kind: " + kind);
			}

			if (locations[i] == null)
			{
				locations[i] = StackSlot
						.get(kind.stackKind(), currentStackIndex, !type.out);
				currentStackIndex += target.spillSlots(kind);
			}
		}

		return new CallingConvention(locations,
				(currentStackIndex - firstStackIndex) * target.spillSlotSize);
	}

	public LIRRegister[] getCallingConventionRegisters(CallingConvention.Type type,
			LIRRegister.RegisterFlag flag)
	{
		if (flag == LIRRegister.RegisterFlag.CPU)
		{
			return cpuParameters;
		}
		assert flag == LIRRegister.RegisterFlag.FPU;
		return fpuParameters;
	}

	public LIRRegister[] getAllocatableRegisters()
	{
		return allocatable;
	}

	public EnumMap<LIRRegister.RegisterFlag, LIRRegister[]>
	getCategorizedAllocatableRegisters()
	{
		return categorized;
	}

	public LIRRegister[] getCallerSaveRegisters()
	{
		return callerSave;
	}

	public CalleeSaveLayout getCalleeSaveLayout()
	{
		return csl;
	}

	public RegisterAttributes[] getAttributesMap()
	{
		return attributesMap;
	}

	public LIRRegister getRegisterForRole(int id)
	{
		return registersRoleMap[id - minRole];
	}

	@Override public String toString()
	{
		StringBuilder roleMap = new StringBuilder();
		for (int i = 0; i < registersRoleMap.length; ++i)
		{
			LIRRegister reg = registersRoleMap[i];
			if (reg != null)
			{
				if (roleMap.length() != 0)
				{
					roleMap.append(", ");
				}
				roleMap.append(i + minRole).append(" -> ").append(reg);
			}
		}
		StringBuilder stackArg0OffsetsMap = new StringBuilder();
		for (CallingConvention.Type t : CallingConvention.Type.VALUES)
		{
			if (stackArg0OffsetsMap.length() != 0)
			{
				stackArg0OffsetsMap.append(", ");
			}
			stackArg0OffsetsMap.append(t).append(" -> ")
					.append(stackArg0Offsets[t.ordinal()]);
		}
		String res = String.format("Allocatable: " + Arrays
				.toString(getAllocatableRegisters()) + "%n" +
				"CallerSave:  " + Arrays.toString(getCallerSaveRegisters())
				+ "%n" +
				"CalleeSave:  " + getCalleeSaveLayout() + "%n" +
				"CPU Params:  " + Arrays.toString(cpuParameters) + "%n" +
				"FPU Params:  " + Arrays.toString(fpuParameters) + "%n" +
				"VMRoles:     " + roleMap + "%n" +
				"stackArg0:   " + stackArg0OffsetsMap + "%n" +
				"Scratch:     " + getScratchRegister() + "%n");
		return res;
	}
}
