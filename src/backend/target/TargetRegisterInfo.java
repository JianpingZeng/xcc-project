package backend.target;

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;

/**
 * This file describes an abstract interface used to get information about a
 * target machines register file.  This information is used for a variety of
 * purposed, especially register allocation.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TargetRegisterInfo
{
	/**
	 * This record contains all of the information known about a
	 * particular register.  The subRegs field (if not null) is an array of
	 * registers that are sub-register of the specified register. The superRegs
	 * field (if not null) is an array of registers that are super-register of
	 * the specified register. This is needed for architectures like X86 which
	 * have AL alias AX alias EAX. Registers that this does not apply to simply
	 * should set this to null.
	 */
	public static class MCRegisterDesc
	{
		/**
		 * Assembly language name for the register.
		 */
		String name;
		/**
		 * Register Alias Set, described above
		 */
		int[] subRegs;

		int[] superRegs;
		/**
		 * flags identifying register properties (below).
		 */
		int flags;
		/**
		 * Target Specific flags.
		 */
		int tSFlags;

		public MCRegisterDesc(String Name, int[] SubRegs,
				int[] SuperRegs, int Flags, int TSFlags)
		{
			name = Name;
			subRegs = SubRegs;
			superRegs = SuperRegs;
			flags = Flags;
			tSFlags = TSFlags;
		}
	}

	public static class TargetRegisterClass
	{
		/**
		 * The register size and alignment in Bytes.
		 */
		private int regSize, regAlign;
		private int[] regs;

		public TargetRegisterClass(int rs, int ra, int[] regs)
		{
			regSize = rs;
			regAlign = ra;
			this.regs = regs;
		}

		public int getNumRegs()
		{
			return regs.length;
		}

		public int getRegister(int i)
		{
			assert i >= 0 && i < regs.length;
			return regs[i];
		}

		/**
		 * Return the size of the register in bytes, which is also the size
		 * of a stack slot allocated to hold a spilled copy of this register.
		 *
		 * @return
		 */
		public int getRegSize()
		{
			return regSize;
		}

		/**
		 * Return the minimum required alignment for a register of
		 * this class.
		 *
		 * @return
		 */
		public int getRegAlign()
		{
			return regAlign;
		}
	}

	//=================================================================//
	// The member of class TargetRegisterInfo.

	/**
	 * This is used as the destination register for instructions that do not
	 * produce a value.
	 */
	public static final int NoRegister = 0;

	/**
	 * This is the first register number that is
	 * considered to be a 'public abstract' register, which is part of the SSA
	 * namespace.  This must be the same for all targets, which means that each
	 * target is limited to 1024 registers.
	 */
	public static final int FirstVirtualRegister = 1024;

	
	private MCRegisterDesc[] desc;
	private TargetRegisterClass[] phyRegClasses;

	private TargetRegisterClass[] regClasses;
	/**
	 * The opcode of setting up stack frame for function being compiled.
	 * If the target machine does not support it, this field will be -1.
	 */
	private int callFrameSetupOpCode;
	/**
	 * The opcode of destroying stack frame for function being compiled.
	 * If the target machine does not support it, this field will be -1.
	 */
	private int callFrameDestroyOpCode;

	protected TargetRegisterInfo(MCRegisterDesc[] desc,
			TargetRegisterClass[] regClasses, int callFrameSetupOpCode,
			int callFrameDestroyOpCode)
	{
		this.desc = desc;
		this.regClasses = regClasses;
		phyRegClasses = new TargetRegisterClass[desc.length];

		// loop over all register classes, handle each register class
		// and keep track of mapping from register to it's register class.
		for (int i = 0, e = regClasses.length; i < e; i++)
		{
			for (int j = 0, ee = regClasses[i].getNumRegs(); j < ee; j++)
			{
				int reg = regClasses[i].getRegister(j);
				assert phyRegClasses[reg] == null :
						"register in more than one class!";
				phyRegClasses[reg] = regClasses[i];
			}
		}
		this.callFrameSetupOpCode = callFrameSetupOpCode;
		this.callFrameDestroyOpCode = callFrameDestroyOpCode;
	}

	protected TargetRegisterInfo(MCRegisterDesc[] desc,
			TargetRegisterClass[] phyRegClasses)
	{
		this(desc, phyRegClasses, -1, -1);
	}

	/**
	 * Obtains the register information indexed with given register number.
	 *
	 * @param regNo
	 * @return
	 */
	public MCRegisterDesc get(int regNo)
	{
		assert regNo >= 0 && regNo < desc.length;
		return desc[regNo];
	}

	public TargetRegisterClass getRegClass(int regNo)
	{
		assert regNo >= 0 && regNo < desc.length;
		return phyRegClasses[regNo];
	}

	/**
	 * Return the set of registers aliased with specified register, or null
	 * list if there have none.
	 *
	 * @param regNo
	 * @return
	 */
	public int[] getSubRegSet(int regNo){return get(regNo).subRegs;}

	public int[] getSuperRegSet(int regNo) {return get(regNo).superRegs;}

	/**
	 * Return the symbolic target specified name for the specified physical register.
	 *
	 * @param regNo
	 * @return
	 */
	public String getName(int regNo)
	{
		return get(regNo).name;
	}

	public abstract int[] getCalledRegisters();

	public int getNumRegClasses()
	{
		return regClasses.length;
	}

	//===--------------------------------------------------------------------===//
	// Interfaces used by the register allocator and stack frame manipulation
	// passes to move data around between registers, immediates and memory.
	//

	public abstract void storeRegToStackSlot(MachineBasicBlock mbb, int mbbi,
			int srcReg, int FrameIndex, TargetRegisterClass rc);

	public abstract void loadRegFromStackSlot(MachineBasicBlock mbb, int mbbi,
			int destReg, int FrameIndex, TargetRegisterClass rc);

	public abstract void copyRegToReg(MachineBasicBlock mbb, int mbbi,
			int destReg, int srcReg, TargetRegisterClass rc);

	/**
	 * This method return the opcode of the frame setup instructions if
	 * they exist (-1 otherwise).  Some targets use pseudo instructions in order
	 * to abstract away the difference between operating with a frame pointer
	 * and operating without, through the use of these two instructions.
	 * @return
	 */
	int getCallFrameSetupOpcode()
	{
		return callFrameSetupOpCode;
	}
	/**
	 * This method return the opcode of the frame destroy instructions if
	 * they exist (-1 otherwise).  Some targets use pseudo instructions in order
	 * to abstract away the difference between operating with a frame pointer
	 * and operating without, through the use of these two instructions.
	 * @return
	 */
	int getCallFrameDestroyOpcode()
	{
		return callFrameDestroyOpCode;
	}

	/**
	 * This method is called during prolog/epilog
	 * code insertion to eliminate call frame setup and destroy pseudo
	 * instructions (but only if the Target is using them).  It is responsible
	 * for eliminating these instructions, replacing them with concrete
	 * instructions.  This method need only be implemented if using call frame
	 * setup/destroy pseudo instructions.
	 */
	public void eliminateCallFramePseudoInstr(MachineFunction mf,
			MachineBasicBlock mbb, int I)
	{
		assert (getCallFrameSetupOpcode() == -1 && getCallFrameDestroyOpcode() == -1)
				: "eliminateCallFramePseudoInstr must be implemented if using"
						+ " call frame setup/destroy pseudo instructions!";
		assert false : "Call Frame Pseudo Instructions do not exist on this target!";
	}
	/**
	 * processFunctionBeforeFrameFinalized - This method is called immediately
	 * before the specified functions frame layout (MF.getFrameInfo()) is
	 * finalized.  Once the frame is finalized, MO_FrameIndex operands are
	 * replaced with direct ants.  This method is optional.
	 */
	public abstract void processFunctionBeforeFrameFinalized(
			MachineFunction mf);

	/*
	 * eliminateFrameIndex - This method must be overridden to eliminate abstract
	 * frame indices from instructions which may use them.  The instruction
	 * referenced by the iterator contains an MO_FrameIndex operand which must be
	 * eliminated by this method.  This method may modify or replace the
	 * specified instruction, as long as it keeps the iterator pointing the the
	 * finished product.
	 */
	public abstract void eliminateFrameIndex(MachineFunction mf, int ii);

	/**
	 * This method insert prologue code into the function.
	 */
	public abstract void emitPrologue(MachineFunction MF);
	/**
	 * This method insert epilogue code into the function.
	 */
	public abstract void emitEpilogue(MachineFunction MF,
			MachineBasicBlock mbb);
}
