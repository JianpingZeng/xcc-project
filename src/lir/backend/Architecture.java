package lir.backend;

import lir.ci.LIRRegister;
import lir.ci.LIRRegister.RegisterFlag;

import java.util.EnumMap;
import java.util.HashMap;

/**
 * /**
 * Represents a CPU architecture, including information such as its endianness, CPU
 * LIRRegisters, word width, etc.
 *
 * @author Jianping Zeng
 */
public abstract class Architecture
{
	/**
	 * The number of bits required in a bit map covering all the LIRRegisters that
	 * may store references. The bit position of a register in the map is the
	 * register's {@linkplain LIRRegister#number number}.
	 */
	public final int registerReferenceMapBitCount;

	/**
	 * Represents the natural length of words (typically LIRRegisters and pointers)
	 * of this architecture, in bytes.
	 */
	public final int wordSize;

	/**
	 * The name of this architecture (e.g. "AMD64", "SPARCv9").
	 */
	public final String name;

	/**
	 * Array of all available LIRRegisters on this architecture. The index of each
	 * register in this array is equal to its {@linkplain LIRRegister#number number}.
	 */
	public final LIRRegister[] LIRRegisters;

	/**
	 * Map of all LIRRegisters keyed by their {@linkplain LIRRegister#name names}.
	 */
	public final HashMap<String, LIRRegister> registersByName;

	/**
	 * The byte ordering can be either little or big endian.
	 */
	public final ByteOrder byteOrder;

	/**
	 * Offset in bytes from the beginning of a call instruction to the displacement.
	 */
	public final int machineCodeCallDisplacementOffset;

	/**
	 * The length of the return address pushed to the stack by a call instruction.
	 * A value of 0 denotes that call linkage uses LIRRegisters instead (e.g. SPARC).
	 */
	public final int returnAddressSize;

	private final EnumMap<LIRRegister.RegisterFlag, LIRRegister[]> registersByTypeAndEncoding;

	/**
	 * Gets the register for a given {@linkplain LIRRegister#encoding encoding} and type.
	 *
	 * @param encoding a register value as used in a machine instruction
	 * @param type     the type of the register
	 */
	public LIRRegister registerFor(int encoding, LIRRegister.RegisterFlag type)
	{
		LIRRegister[] regs = registersByTypeAndEncoding.get(type);
		assert encoding >= 0 && encoding < regs.length;
		LIRRegister reg = regs[encoding];
		assert reg != null;
		return reg;
	}

	protected Architecture(String name, int wordSize, ByteOrder byteOrder,
			LIRRegister[] LIRRegisters, int nativeCallDisplacementOffset,
			int registerReferenceMapBitCount,
			int returnAddressSize)
	{
		this.name = name;
		this.LIRRegisters = LIRRegisters;
		this.wordSize = wordSize;
		this.byteOrder = byteOrder;
		this.machineCodeCallDisplacementOffset = nativeCallDisplacementOffset;
		this.registerReferenceMapBitCount = registerReferenceMapBitCount;
		this.returnAddressSize = returnAddressSize;

		registersByName = new HashMap<String, LIRRegister>(LIRRegisters.length);
		for (LIRRegister LIRRegister : LIRRegisters)
		{
			registersByName.put(LIRRegister.name, LIRRegister);
			assert LIRRegisters[LIRRegister.number] == LIRRegister;
		}

		registersByTypeAndEncoding = new EnumMap<RegisterFlag, LIRRegister[]>(
				RegisterFlag.class);
		EnumMap<RegisterFlag, LIRRegister[]> categorizedRegs = LIRRegister
				.categorize(LIRRegisters);
		for (RegisterFlag type : RegisterFlag.values())
		{
			LIRRegister[] regs = categorizedRegs.get(type);
			int max = LIRRegister.maxRegisterEncoding(regs);
			LIRRegister[] regsByEnc = new LIRRegister[max + 1];
			for (LIRRegister reg : regs)
			{
				regsByEnc[reg.encoding] = reg;
			}
			registersByTypeAndEncoding.put(type, regsByEnc);
		}
	}

	/**
	 * Converts this architecture to a string.
	 *
	 * @return the string representation of this architecture
	 */
	@Override public final String toString()
	{
		return name.toLowerCase();
	}

	/**
	 * Checks whether this is a 32-bit architecture.
	 *
	 * @return {@code true} if this architecture is 32-bit
	 */
	public final boolean is32bit()
	{
		return wordSize == 4;
	}

	/**
	 * Checks whether this is a 64-bit architecture.
	 *
	 * @return {@code true} if this architecture is 64-bit
	 */
	public final boolean is64bit()
	{
		return wordSize == 8;
	}

	// The following methods are architecture specific and not dependent on state
	// stored in this class. They have convenient default implementations.

	/**
	 * Checks whether this architecture's normal arithmetic instructions use a
	 * two-LIROperand form (e.g. x86 which overwrites one LIROperand register with the
	 * result when adding).
	 *
	 * @return {@code true} if this architecture uses two-LIROperand mode
	 */
	public boolean twoOperandMode()
	{
		return false;
	}

	// TODO: Why enumerate the concrete subclasses here rather
	// than use instanceof comparisons in code that cares?

	/**
	 * Checks whether the architecture is x86.
	 *
	 * @return {@code true} if the architecture is x86
	 */
	public boolean isX86()
	{
		return false;
	}

	/**
	 * Checks whether the architecture is SPARC.
	 *
	 * @return {@code true} if the architecture is SPARC
	 */
	public boolean isSPARC()
	{
		return false;
	}

}
