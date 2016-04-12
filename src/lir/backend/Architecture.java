package lir.backend;

import lir.ci.Register;
import lir.ci.Register.RegisterFlag;

import java.util.EnumMap;
import java.util.HashMap;

/**
 * /**
 * Represents a CPU architecture, including information such as its endianness, CPU
 * registers, word width, etc.
 *
 * @author Jianping Zeng
 */
public abstract class Architecture
{
	/**
	 * The number of bits required in a bit map covering all the registers that
	 * may store references. The bit position of a register in the map is the
	 * register's {@linkplain Register#number number}.
	 */
	public final int registerReferenceMapBitCount;

	/**
	 * Represents the natural size of words (typically registers and pointers)
	 * of this architecture, in bytes.
	 */
	public final int wordSize;

	/**
	 * The name of this architecture (e.g. "AMD64", "SPARCv9").
	 */
	public final String name;

	/**
	 * Array of all available registers on this architecture. The index of each
	 * register in this array is equal to its {@linkplain Register#number number}.
	 */
	public final Register[] registers;

	/**
	 * Map of all registers keyed by their {@linkplain Register#name names}.
	 */
	public final HashMap<String, Register> registersByName;

	/**
	 * The byte ordering can be either little or big endian.
	 */
	public final ByteOrder byteOrder;

	/**
	 * Offset in bytes from the beginning of a call instruction to the displacement.
	 */
	public final int machineCodeCallDisplacementOffset;

	/**
	 * The size of the return address pushed to the stack by a call instruction.
	 * A value of 0 denotes that call linkage uses registers instead (e.g. SPARC).
	 */
	public final int returnAddressSize;

	private final EnumMap<Register.RegisterFlag, Register[]> registersByTypeAndEncoding;

	/**
	 * Gets the register for a given {@linkplain Register#encoding encoding} and type.
	 *
	 * @param encoding a register value as used in a machine instruction
	 * @param type     the type of the register
	 */
	public Register registerFor(int encoding, Register.RegisterFlag type)
	{
		Register[] regs = registersByTypeAndEncoding.get(type);
		assert encoding >= 0 && encoding < regs.length;
		Register reg = regs[encoding];
		assert reg != null;
		return reg;
	}

	protected Architecture(String name, int wordSize, ByteOrder byteOrder,
			Register[] registers, int nativeCallDisplacementOffset,
			int registerReferenceMapBitCount,
			int returnAddressSize)
	{
		this.name = name;
		this.registers = registers;
		this.wordSize = wordSize;
		this.byteOrder = byteOrder;
		this.machineCodeCallDisplacementOffset = nativeCallDisplacementOffset;
		this.registerReferenceMapBitCount = registerReferenceMapBitCount;
		this.returnAddressSize = returnAddressSize;

		registersByName = new HashMap<String, Register>(registers.length);
		for (Register register : registers)
		{
			registersByName.put(register.name, register);
			assert registers[register.number] == register;
		}

		registersByTypeAndEncoding = new EnumMap<RegisterFlag, Register[]>(
				RegisterFlag.class);
		EnumMap<RegisterFlag, Register[]> categorizedRegs = Register
				.categorize(registers);
		for (RegisterFlag type : RegisterFlag.values())
		{
			Register[] regs = categorizedRegs.get(type);
			int max = Register.maxRegisterEncoding(regs);
			Register[] regsByEnc = new Register[max + 1];
			for (Register reg : regs)
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
