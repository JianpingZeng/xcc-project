package lir.backend;

import lir.ci.CiKind;

/**
 * Represents the target machine for a compiler, including the CPU architecture,
 * the size of pointers and references, alignment of stacks, caches, etc.
 * @author Jianping Zeng
 */
public class TargetMachine
{
	public Architecture arch;
	/**
	 * The OS page size.
	 */
	public final int pageSize;

	/**
	 * Specifies if this is a multi-processor system.
	 */
	public final boolean isMP;

	/**
	 * The number of {@link #spillSlotSize spill slots} required per kind.
	 */
	private final int[] spillSlotsPerKindMap;

	/**
	 * Specifies if this target supports encoding objects inline in the machine code.
	 */
	public final boolean inlineObjects;

	/**
	 * The spill slot size for values that occupy 1 slot.
	 */
	public final int spillSlotSize;

	/**
	 * The machine word size on this target.
	 */
	public final int wordSize;

	/**
	 * The CiKind to be used for representing raw pointers and CPU registers.
	 */
	public final CiKind wordKind;

	/**
	 * The stack alignment requirement of the platform. For example,
	 * from Appendix D of <a href="http://www.intel.com/Assets/PDF/manual/248966.pdf">
	 *     Intel 64 and IA-32 Architectures Optimization Reference Manual</a>:
	 * <pre>
	 *     "It is important to ensure that the stack frame is aligned to a
	 *      16-byte boundary upon function entry to keep local __m128 data,
	 *      parameters, and XMM register spill locations aligned throughout
	 *      a function invocation."
	 * </pre>
	 */
	public final int stackAlignment;

	/**
	 * <a href="http://docs.sun.com/app/docs/doc/806-0477/6j9r2e2b9?a=view"></>
	 */
	public final int stackBias;

	/**
	 * The cache alignment.
	 */
	public final int cacheAlignment;

	/**
	 * Specifies how {@code long} and {@code double} constants are to be stored
	 * in frames. This is useful for VMs such as HotSpot
	 * where convention the interpreter uses is that the second local
	 * holds the first raw word of the native long or double representation.
	 * This is actually reasonable, since locals and stack arrays
	 * grow downwards in all implementations.
	 * If, on some machine, the interpreter's Java locals or stack
	 * were to grow upwards, the embedded doubles would be word-swapped.)
	 */
	public final boolean debugInfoDoubleWordsInSecondSlot;

	/**
	 * Temporary flag to distinguish between the semantics necessary for HotSpot and Maxine.
	 */
	// TODO This should go away when XIR goes away, and the logic be part of the VM-specific lowering.
	public final boolean invokeSnippetAfterArguments;

	public TargetMachine(Architecture arch, boolean isMP, int spillSlotSize,
			int stackAlignment, int pageSize, int cacheAlignment,
			boolean inlineObjects, boolean debugInfoDoubleWordsInSecondSlot,
			boolean invokeSnippetAfterArguments)
	{
		this.arch = arch;
		this.pageSize = pageSize;
		this.isMP = isMP;
		this.spillSlotSize = spillSlotSize;
		this.wordSize = arch.wordSize;
		if (wordSize == 8)
		{
			this.wordKind = CiKind.Long;
		}
		else
		{
			this.wordKind = CiKind.Int;
		}
		this.stackAlignment = stackAlignment;
		this.stackBias = 0; // TODO: configure with param once SPARC port exists
		this.cacheAlignment = cacheAlignment;
		this.inlineObjects = inlineObjects;
		this.spillSlotsPerKindMap = new int[CiKind.values().length];
		this.debugInfoDoubleWordsInSecondSlot = debugInfoDoubleWordsInSecondSlot;
		this.invokeSnippetAfterArguments = invokeSnippetAfterArguments;

		for (CiKind k : CiKind.values())
		{
			// initialize the number of spill slots required for each kind
			int size = sizeInBytes(k);
			int slots = 0;
			while (slots * spillSlotSize < size)
			{
				slots++;
			}
			spillSlotsPerKindMap[k.ordinal()] = slots;
		}
	}

	/**
	 * Gets the size in bytes of the specified kind for this target.
	 *
	 * @param kind the kind for which to get the size
	 * @return the size in bytes of {@code kind}
	 */
	public int sizeInBytes(CiKind kind)
	{
		// Checkstyle: stop
		switch (kind)
		{
			case Boolean:
				return 1;
			case Byte:
				return 1;
			case Char:
				return 2;
			case Short:
				return 2;
			case Int:
				return 4;
			case Long:
				return 8;
			case Float:
				return 4;
			case Double:
				return 8;
			case Object:
				return wordSize;
			default:
				return 0;
		}
		// Checkstyle: resume
	}

	/**
	 * Gets the number of spill slots for a specified kind in this target.
	 *
	 * @param kind the kind for which to get the spill slot count
	 * @return the number of spill slots for {@code kind}
	 */
	public int spillSlots(CiKind kind)
	{
		return spillSlotsPerKindMap[kind.ordinal()];
	}

	/**
	 * Aligns the given frame size (without return instruction pointer) to the stack
	 * alignment size and return the aligned size (without return instruction pointer).
	 *
	 * @param frameSize the initial frame size to be aligned
	 * @return the aligned frame size
	 */
	public int alignFrameSize(int frameSize)
	{
		int x = frameSize + arch.returnAddressSize + (stackAlignment - 1);
		return (x / stackAlignment) * stackAlignment - arch.returnAddressSize;
	}
}
