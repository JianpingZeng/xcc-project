package lir;

import driver.Backend;
import hir.Method;
import lir.ci.*;
import lir.ci.CallingConvention.Type;
import utils.Util;
import java.util.BitSet;

/**
 * This class is used to build the stack frame layout for a compiled method.
 * <p>
 * This is the format of a stack frame on an x86 (i.e. IA32 or X64) platform:
 * <pre>
 *   Base       Contents
 *
 *          :                                :
 *          | incoming overflow argument n   |
 *          |     ...                        |
 *          | incoming overflow argument 0   |
 *          +--------------------------------+ Caller frame
 *          |                                |
 *          : custom area*                   :
 *          |                                |
 *   -------+--------------------------------+---------------------
 *          | return address                 |
 *          +--------------------------------+                  ---
 *          |                                |                   ^
 *          : callee save area               :                   |
 *          |                                |                   |
 *          +--------------------------------+                   |
 *          | alignment padding              |                   |
 *          +--------------------------------+                   |
 *          | ALLOCA block n                 |                   |
 *          :     ...                        :                   |
 *          | ALLOCA block 0                 | Current frame     |
 *          +--------------------------------+    ---            |
 *          | spill slot n                   |     ^           frame
 *          :     ...                        :     |           length
 *          | spill slot 0                   |  shared           |
 *          +- - - - - - - - - - - - - - - - +   slot            |
 *          | outgoing overflow argument n   |  indexes          |
 *          |     ...                        |     |             |
 *          | outgoing overflow argument 0   |     v             |
 *          +--------------------------------+    ---            |
 *          |                                |                   |
 *          : custom area                    :                   |
 *    %sp   |                                |                   v
 *   -------+--------------------------------+----------------  ---
 *
 * </pre>
 * Note that the length of {@link hir.Instruction.Alloca ALLOCA} blocks and
 * {@code monitor}s in the frame may be greater than the length of a {@linkplain
 * lir.backend.TargetMachine#spillSlotSize spill slot}. Note also that the layout of
 * the caller frame shown only applies if the caller was also compiled with C1X.
 * In particular, native frames won't have a custom area if the native ABI specifies
 * that stack arguments are at the bottom of the frame (e.g. System V ABI on AMD64).
 */
public final class StackFrame
{

	private final Backend backend;
	private final CallingConvention incomingArguments;

	/**
	 * The final frame length.
	 * Value is only set after register allocation is complete.
	 */
	private int frameSize;

	/**
	 * The number of spill slots allocated by the register allocator.
	 * The value {@code -2} means that the length of outgoing argument stack slots
	 * is not yet fixed. The value {@code -1} means that the register
	 * allocator has started allocating spill slots and so the length of
	 * outgoing stack slots cannot change as outgoing stack slots and
	 * spill slots share the same slot index address space.
	 */
	private int spillSlotCount;

	/**
	 * The amount of memory allocated within the frame for uses of
	 * {@link hir.Instruction.Alloca ALLOCA}
	 */
	private int stackBlocksSize;

	/**
	 * The list of stack blocks allocated in this frame.
	 */
	private StackBlock stackBlocks;

	/**
	 * Area occupied by outgoing overflow arguments.
	 * This value is adjusted as calling conventions for outgoing calls are retrieved.
	 */
	private int outgoingSize;

	/**
	 * Creates a new frame map for the specified method.
	 *
	 * @param backend the lir.backend context
	 * @param method      the outermost method being compiled
	 */
	public StackFrame(Backend backend, Method method)
	{
		this.backend = backend;
		this.frameSize = -1;
		this.spillSlotCount = -2;

		if (method == null)
		{
			incomingArguments = new CallingConvention(new LIRValue[0], 0);
		}
		else
		{
			incomingArguments = getCallingConvention(
					Util.signatureToKinds(method), Type.JavaCallee);
		}
	}

	/**
	 * Gets the calling convention for a call with the specified signature.
	 *
	 * @param type      the type of calling convention being requested
	 * @param signature the signature of the arguments
	 * @return a {@link CallingConvention} instance describing the location of parameters and the return value
	 */
	public CallingConvention getCallingConvention(LIRKind[] signature,
			Type type)
	{
		CallingConvention cc = backend.registerConfig
				.getCallingConvention(type, signature, backend.targetMachine,
						false);
		if (type == Type.RuntimeCall)
		{
			assert cc.stackSize
					== 0 : "runtime call should not have stack arguments";
		}
		else if (type.out)
		{
			assert frameSize == -1 : "frame length must not yet be fixed!";
			reserveOutgoing(cc.stackSize);
		}
		return cc;
	}

	/**
	 * Gets the calling convention for the incoming arguments to the compiled method.
	 *
	 * @return the calling convention for incoming arguments
	 */
	public CallingConvention incomingArguments()
	{
		return incomingArguments;
	}

	/**
	 * Gets the frame length of the compiled frame.
	 *
	 * @return the length in bytes of the frame
	 */
	public int frameSize()
	{
		assert this.frameSize != -1 : "frame length not computed yet";
		return frameSize;
	}

	/**
	 * Sets the frame length for this frame.
	 *
	 * @param frameSize the frame length in bytes
	 */
	public void setFrameSize(int frameSize)
	{
		assert this.frameSize == -1 : "should only be calculated once";
		this.frameSize = frameSize;
	}

	/**
	 * Computes the frame length for this frame, given the number of spill slots.
	 *
	 * @param spillSlotCount the number of spill slots
	 */
	public void finalizeFrame(int spillSlotCount)
	{
		assert this.spillSlotCount == -1 : "can only be set once";
		assert this.frameSize == -1 : "should only be calculated once";
		assert spillSlotCount >= 0 : "must be positive";

		this.spillSlotCount = spillSlotCount;
		int frameSize = offsetToStackBlocksEnd();
		CalleeSaveLayout csl = backend.registerConfig
				.getCalleeSaveLayout();
		if (csl != null)
		{
			frameSize += csl.size;
		}
		this.frameSize = backend.targetMachine.alignFrameSize(frameSize);
	}

	/**
	 * Converts a stack slot into a stack address.
	 *
	 * @param slot a stack slot
	 * @return a stack address
	 */
	public LIRAddress toStackAddress(StackSlot slot)
	{
		int size = backend.targetMachine.sizeInBytes(slot.kind);
		if (slot.inCallerFrame())
		{
			int callerFrame =
					frameSize() + backend.targetMachine.arch.returnAddressSize;
			final int callerFrameOffset =
					slot.index() * backend.targetMachine.spillSlotSize;
			int offset = callerFrame + callerFrameOffset;
			return new LIRAddress(slot.kind, LIRRegister.Frame.asValue(), offset);
		}
		else
		{
			int offset = offsetForOutgoingOrSpillSlot(slot.index(), size);
			return new LIRAddress(slot.kind, LIRRegister.Frame.asValue(), offset);
		}
	}

	/**
	 * Gets the stack address within this frame for a given reserved stack block.
	 *
	 * @param stackBlock the value returned from {@link #reserveStackBlock(int, boolean)} identifying the stack block
	 * @return a representation of the stack location
	 */
	public LIRAddress toStackAddress(StackBlock stackBlock)
	{
		return new LIRAddress(backend.targetMachine.wordKind,
				backend.registerConfig.getFrameRegister()
						.asValue(backend.targetMachine.wordKind),
				offsetForStackBlock(stackBlock));
	}

	/**
	 * Reserves space for stack-based outgoing arguments.
	 *
	 * @param argsSize the amount of space to reserve for stack-based outgoing arguments
	 */
	public void reserveOutgoing(int argsSize)
	{
		assert spillSlotCount == -2 :
				"cannot reserve outgoing stack slot space once register allocation has started";
		if (argsSize > outgoingSize)
		{
			outgoingSize = Util
					.roundUp(argsSize, backend.targetMachine.spillSlotSize);
		}
	}

	/**
	 * Encapsulates the details of a stack block reserved by a call to {@link StackFrame#reserveStackBlock(int, boolean)}.
	 */
	public static final class StackBlock
	{
		/**
		 * The length of this stack block.
		 */
		public final int size;

		/**
		 * The offset of this stack block within the frame space reserved for stack blocks.
		 */
		public final int offset;

		/**
		 * Specifies if this block holds object values.
		 */
		public final boolean refs;

		public final StackBlock next;

		public StackBlock(StackBlock next, int size, int offset, boolean refs)
		{
			this.size = size;
			this.offset = offset;
			this.next = next;
			this.refs = refs;
		}
	}

	/**
	 * Reserves a block of memory in the frame of the method being compiled.
	 *
	 * @param size the number of bytes to reserve
	 * @param refs specifies if the block is all references
	 * @return a descriptor of the reserved block that can be used with
	 * {@link #toStackAddress(StackBlock)} once register
	 * allocation is complete and the length of the frame has been
	 * {@linkplain #finalizeFrame(int) finalized}.
	 */
	public StackBlock reserveStackBlock(int size, boolean refs)
	{
		int wordSize = backend.targetMachine.wordSize;
		assert (size % wordSize) == 0;
		StackBlock block = new StackBlock(stackBlocks, size, stackBlocksSize,
				refs);
		stackBlocksSize += size;
		stackBlocks = block;
		return block;
	}

	private int offsetForStackBlock(StackBlock stackBlock)
	{
		assert stackBlock.offset >= 0 && stackBlock.offset + stackBlock.size
				<= stackBlocksSize : "invalid stack block";
		int offset = offsetToStackBlocks() + stackBlock.offset;
		assert offset <= (frameSize()
				- stackBlock.size) : "spill outside of frame";
		return offset;
	}

	/**
	 * Gets the stack pointer offset for a outgoing stack argument or compiler spill slot.
	 *
	 * @param slotIndex the index of the stack slot within the slot index space reserved for
	 * @param size
	 */
	private int offsetForOutgoingOrSpillSlot(int slotIndex, int size)
	{
		assert slotIndex >= 0 && slotIndex < (initialSpillSlot()
				+ spillSlotCount) : "invalid spill slot";
		int offset = slotIndex * backend.targetMachine.spillSlotSize;
		assert offset <= (frameSize() - size) : "slot outside of frame";
		return offset;
	}
	private int offsetToSpillArea()
	{
		return outgoingSize + customAreaSize();
	}

	private int offsetToSpillEnd()
	{
		return offsetToSpillArea()
				+ spillSlotCount * backend.targetMachine.spillSlotSize;
	}

	private int offsetToMonitors()
	{
		return offsetToSpillEnd();
	}

	public int customAreaSize()
	{
		return backend.runtime.getCustomStackAreaSize();
	}

	public int offsetToCustomArea()
	{
		return 0;
	}

	private int offsetToStackBlocks()
	{
		return offsetToMonitorsEnd();
	}

	private int offsetToStackBlocksEnd()
	{
		return offsetToStackBlocks() + stackBlocksSize;
	}

	public int offsetToCalleeSaveAreaStart()
	{
		CalleeSaveLayout csl = backend.registerConfig
				.getCalleeSaveLayout();
		if (csl != null)
		{
			return offsetToCalleeSaveAreaEnd() - csl.size;
		}
		else
		{
			return offsetToCalleeSaveAreaEnd();
		}
	}

	public int offsetToCalleeSaveAreaEnd()
	{
		return frameSize;
	}

	/**
	 * Gets the index of the first available spill slot relative to the base of the frame.
	 * After this call, no further outgoing stack slots can be {@linkplain #reserveOutgoing(int) reserved}.
	 *
	 * @return the index of the first available spill slot
	 */
	public int initialSpillSlot()
	{
		if (spillSlotCount == -2)
		{
			spillSlotCount = -1;
		}
		return (outgoingSize + customAreaSize())
				/ backend.targetMachine.spillSlotSize;
	}

	/**
	 * Initializes a ref map that covers all the slots in the frame.
	 */
	public BitSet initFrameRefMap()
	{
		int frameSize = frameSize();
		int frameWords = frameSize / backend.targetMachine.spillSlotSize;
		BitSet frameRefMap = new BitSet(frameWords);
		for (StackBlock sb = stackBlocks; sb != null; sb = sb.next)
		{
			if (sb.refs)
			{
				int firstSlot =
						offsetForStackBlock(sb) / backend.targetMachine.wordSize;
				int words = sb.size / backend.targetMachine.wordSize;
				for (int i = 0; i < words; i++)
				{
					frameRefMap.set(firstSlot + i);
				}
			}
		}
		return frameRefMap;
	}

}

