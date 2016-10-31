package backend.lir;

import backend.lir.backend.MachineInfo;
import driver.Backend;
import hir.Function;
import hir.Instruction;
import backend.lir.ci.*;
import tools.Util;
import java.util.BitSet;

/**
 * This class is used to build the stack frame layout for a compiled method.
 * <p>
 * This is the format of a stack frame on an IA32 platform:
 * 
 * <pre>
 *   Base       Contents
 * 
 *          :                                :
 *          | incoming overflow argument n   |
 *          |     ...                        |
 *          | incoming overflow argument 0   |
 *        --+--------------------------------+--Caller frame
 *          | return address                 |
 *        --+--------------------------------+--
 *        	| old %ebp                       |	  
 * %ebp --> +--------------------------------+                  ---
 *          |                                |                   ^
 *          : callee saved registers         :                   |
 *          |                                |                   |
 *          +--------------------------------+                   |
 *          | alignment padding              |                   |
 *          +--------------------------------+                   |
 *          | ALLOCA block 0                 |                   |
 *          :     ...                        :                   |
 *          | ALLOCA block n                 | Current frame     |
 *          +--------------------------------+                 frame	 
 *          | spill slot 0                   |                 getSize
 *          :     ...                        :                   |
 *          | spill slot n                   |            		 v   
 *          +--------------------------------+
 *          | outgoing function arguments n  |
 *          :	  ...                        :
 *          | outgoing function arguments 0  |                                                               
 *  %esp--> +--------------------------------+----------------  ---
 *
 * </pre>
 * 
 * Note that the getSize of {@link Instruction.AllocaInst ALLOCA} blocks and
 * {@code monitor}s in the frame may be greater than the getSize of a
 * {@linkplain MachineInfo#spillSlotSize spill slot}.
 * 
 * @author xlous.zeng
 * @version 0.1
 */
public final class StackFrame
{
	private final Backend backend;
	private final CallingConvention incomingArguments;

	/**
	 * The final frame getSize. Value is only set after register allocation is
	 * complete.
	 */
	private int frameSize;

	/**
	 * The number of spill slots allocated by the register allocator. The value
	 * {@code -2} means that the getSize of outgoing argument stack slots is not
	 * yet fixed. The value {@code -1} means that the register allocator has
	 * started allocating spill slots and so the getSize of outgoing stack slots
	 * cannot change as outgoing stack slots and spill slots share the same slot
	 * index address space.
	 */
	private int spillSlotCount;

	/**
	 * The amount of memory allocated within the frame for uses of
	 * {@link Instruction.AllocaInst ALLOCA}
	 */
	private int stackBlocksSize;

	/**
	 * The list of stack blocks allocated in this frame.
	 */
	private StackBlock stackBlocks;

	/**
	 * Area occupied by outgoing overflow arguments.
	 */
	private int outgoingSize;

	/**
	 * Creates a new frame map for the specified function.
	 *
	 * @param backend the lir.backend context.
	 * @param function the function being compiled.
	 */
	public StackFrame(Backend backend, Function function)
	{
		this.backend = backend;
		this.frameSize = -1;
		this.spillSlotCount = -2;

		if (function == null)
			incomingArguments = new CallingConvention(new LIRValue[0], 0);
		else
			incomingArguments = getCallingConvention(Util
			        .signatureToKinds(function));
	}

	/**
	 * Gets the calling convention for a call with the specified signature.
	 *
	 * @param signature the signature of the arguments
	 * @return a {@link CallingConvention} instance describing the location of
	 *         parameters and the return value
	 */
	public CallingConvention getCallingConvention(LIRKind[] signature)
	{
		// conform ia32 calling convention, all parameters were lived in stack.
		CallingConvention cc = backend.registerConfig.getCallingConvention(
		        signature, backend.machineInfo, true);
		return cc;
	}

	/**
	 * Gets the calling convention for the incoming arguments to the compiled
	 * method.
	 *
	 * @return the calling convention for incoming arguments
	 */
	public CallingConvention incomingArguments()
	{
		return incomingArguments;
	}

	/**
	 * Gets the frame getSize of the compiled frame.
	 *
	 * @return the getSize in bytes of the frame
	 */
	public int frameSize()
	{
		assert this.frameSize != -1 : "frame getSize not computed yet";
		return frameSize;
	}

	/**
	 * Sets the frame getSize for this frame.
	 *
	 * @param frameSize the frame getSize in bytes
	 */
	public void setFrameSize(int frameSize)
	{
		assert this.frameSize == -1 : "should only be calculated once";
		this.frameSize = frameSize;
	}

	/**
	 * Computes the frame length for this frame, given the number of spill
	 * slots.
	 *
	 * @param spillSlotCount the number of spill slots
	 */
	public void finalizeFrame(int spillSlotCount)
	{
		assert this.spillSlotCount == -1 : "can only be set once";
		assert this.frameSize == -1 : "should only be calculated once";
		assert spillSlotCount >= 0 : "must be positive";

		this.spillSlotCount = spillSlotCount;
		
		int frameSize = offsetToSpillEnd();
		this.frameSize = backend.machineInfo.alignFrameSize(frameSize);
	}

	/**
	 * Converts a stack slot into a stack address.
	 *
	 * @param slot a stack slot
	 * @return a stack address
	 */
	public LIRAddress toStackAddress(StackSlot slot)
	{
		int size = backend.machineInfo.sizeInBytes(slot.kind);
		LIRRegister bp = backend.registerConfig.getFrameRegister();
		if (slot.inCallerFrame())
		{
			int callerFrame = backend.machineInfo.arch.returnAddressSize
			        + bp.spillSlotSize;

			final int callerFrameOffset = slot.index()
			        * backend.machineInfo.spillSlotSize;
			int offset = callerFrame + callerFrameOffset;
			return new LIRAddress(slot.kind, bp.asValue(), offset);
		}
		else
		{
			int offset = offsetForOutgoingOrSpillSlot(slot.index(), size);
			// note that, since the stack of ia32 is growed descended from
			// higher address to lower address.
			return new LIRAddress(slot.kind, bp.asValue(), -offset);
		}
	}

	/**
	 * Gets the stack address within this frame for a given reserved stack
	 * block.
	 *
	 * @param stackBlock the value returned from
	 *            {@link #reserveStackBlock(int, boolean)} identifying the stack
	 *            block
	 * @return a representation of the stack location
	 */
	public LIRAddress toStackAddress(StackBlock stackBlock)
	{
		return new LIRAddress(backend.machineInfo.wordKind,
		        backend.registerConfig.getFrameRegister().asValue(
		                backend.machineInfo.wordKind),
		        offsetForStackBlock(stackBlock));
	}

	/**
	 * Reserves space for stack-based outgoing arguments.
	 *
	 * @param argsSize the amount of space to reserve for stack-based outgoing
	 *            arguments
	 */
	public void reserveOutgoing(int argsSize)
	{
		assert spillSlotCount == -2 : "cannot reserve outgoing stack slot space once register allocation has started";
		if (argsSize > outgoingSize)
		{
			outgoingSize = Util.roundUp(argsSize,
			        backend.machineInfo.spillSlotSize);
		}
	}

	/**
	 * Encapsulates the details of a stack block reserved by a call to
	 * {@link StackFrame#reserveStackBlock(int, boolean)}.
	 */
	public static final class StackBlock
	{
		/**
		 * The length of this stack block.
		 */
		public final int size;

		/**
		 * The offset of this stack block within the frame space reserved for
		 * stack blocks.
		 */
		public final int offset;

		/**
		 * Specifies if this block holds object VALUES.
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
	 * @param refs specifies if the block is references
	 * @return a descriptor of the reserved block that can be used with
	 *         {@link #toStackAddress(StackBlock)} once register allocation is
	 *         complete and the length of the frame has been
	 *         {@linkplain #finalizeFrame(int) finalized}.
	 */
	public StackBlock reserveStackBlock(int size, boolean refs)
	{
		int wordSize = backend.machineInfo.wordSize;
		assert (size % wordSize) == 0;
		StackBlock block = new StackBlock(stackBlocks, size, stackBlocksSize,
		        refs);
		stackBlocksSize += size;
		stackBlocks = block;
		return block;
	}

	/**
	 * Obtains the offset of stack block saved in stack to base pointer(%ebp).
	 * @param stackBlock
	 * @return
	 */
	private int offsetForStackBlock(StackBlock stackBlock)
	{
		assert stackBlock.offset >= 0
		        && stackBlock.offset + stackBlock.size <= stackBlocksSize : "invalid stack block";
		int offset = offsetToStackBlocks() + stackBlock.offset;
		assert offset <= offsetToStackBlocksEnd() : "spill outside of frame";
		return offset;
	}

	/**
	 * Gets the stack pointer offset for a driver spill slot.
	 *
	 * @param slotIndex the index of the stack slot within the slot index space
	 *            reserved for
	 * @param size
	 */
	private int offsetForOutgoingOrSpillSlot(int slotIndex, int size)
	{
		assert slotIndex >= 0
		        && slotIndex < (initialSpillSlot() + spillSlotCount) : "invalid spill slot";
		int offset = slotIndex * backend.machineInfo.spillSlotSize;
		assert offset <= offsetToSpillEnd() : "slot outside of frame";
		return offset;
	}

	/**
	 * The offset to the beginning of spill area from stack pointer(%sp).
	 * @return
	 */
	private int offsetToSpillArea()
	{
		return offsetToStackBlocksEnd();
	}

	/**
	 * The offset to the ending of spill area from stack pointer(%sp).
	 * @return
	 */
	private int offsetToSpillEnd()
	{
		return offsetToSpillArea() + spillSlotCount
		        * backend.machineInfo.spillSlotSize;
	}

	/**
	 * The offset to the beginning of stack block area from stack pointer(%sp).
	 * @return
	 */
	private int offsetToStackBlocks()
	{
		return offsetToCalleeSaveAreaEnd();
	}

	/**
	 * The offset to the ending position of stack block area from stack
	 * pointer(%sp).
	 * @return
	 */
	private int offsetToStackBlocksEnd()
	{
		return offsetToStackBlocks() + stackBlocksSize;
	}

	public int offsetToCalleeSaveAreaStart()
	{
		return 0;
	}

	public int offsetToCalleeSaveAreaEnd()
	{
		CalleeSaveLayout csl = backend.registerConfig.getCalleeSaveLayout();
		if (csl != null)
		{
			return offsetToCalleeSaveAreaStart() + csl.size;
		}
		else
		{
			return offsetToCalleeSaveAreaStart();
		}
	}

	/**
	 * Gets the index of the first available spill slot relative to the base of
	 * the frame. After this call, no further outgoing stack slots can be
	 * {@linkplain #reserveOutgoing(int) reserved}.
	 *
	 * @return the index of the first available spill slot
	 */
	public int initialSpillSlot()
	{
		if (spillSlotCount == -2)
		{
			spillSlotCount = -1;
		}
		return (outgoingSize) / backend.machineInfo.spillSlotSize;
	}

	/**
	 * Initializes a ref map that covers all the slots in the frame.
	 */
	public BitSet initFrameRefMap()
	{
		int frameSize = frameSize();
		int frameWords = frameSize / backend.machineInfo.spillSlotSize;
		BitSet frameRefMap = new BitSet(frameWords);
		for (StackBlock sb = stackBlocks; sb != null; sb = sb.next)
		{
			if (sb.refs)
			{
				int firstSlot = offsetForStackBlock(sb)
				        / backend.machineInfo.wordSize;
				int words = sb.size / backend.machineInfo.wordSize;
				for (int i = 0; i < words; i++)
				{
					frameRefMap.set(firstSlot + i);
				}
			}
		}
		return frameRefMap;
	}
}
