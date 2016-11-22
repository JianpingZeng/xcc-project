package backend.target;

import backend.codegen.MachineInstr;

/**
 * This class is an Interface to description of machine instructions, which
 * used for representing the information about machine instruction which
 * specified with target.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TargetInstrInfo
{
	public final static int M_NOP_FLAG = 1 << 0;
	public final static int M_BRANCH_FLAG = 1 << 1;
	public final static int M_CALL_FLAG = 1 << 2;
	public final static int M_RET_FLAG = 1 << 3;
	public final static int M_ARITH_FLAG = 1 << 4;
	public final static int M_CC_FLAG = 1 << 6;
	public final static int M_LOGICAL_FLAG = 1 << 6;
	public final static int M_INT_FLAG = 1 << 7;
	public final static int M_FLOAT_FLAG = 1 << 8;
	public final static int M_CONDL_FLAG = 1 << 9;
	public final static int M_LOAD_FLAG = 1 << 10;
	public final static int M_PREFETCH_FLAG = 1 << 11;
	public final static int M_STORE_FLAG = 1 << 12;
	public final static int M_DUMMY_PHI_FLAG = 1 << 13;
	public final static int M_PSEUDO_FLAG = 1 << 14;       // Pseudo instruction
	// 3-addr instructions which really work like 2-addr ones, eg. X86 add/sub
	public final static int M_2_ADDR_FLAG = 1 << 15;

	// M_TERMINATOR_FLAG - Is this instruction part of the terminator for a basic
	// block?  Typically this is things like return and branch instructions.
	// Various passes use this to insert code into the bottom of a basic block, but
	// before control flow occurs.
	public final static int M_TERMINATOR_FLAG = 1 << 16;

	/**
	 * Describing the machine instructions initialized only when the
	 * TargetMachine class is created
	 */
	public static TargetInstrDescriptor[] TargetInstrDescriptors;

	public static class TargetInstrDescriptor
	{
		/**
		 * The opcode of this instruction specfified with target machine.
		 */
		public int opCode;
		/**
		 * Assembly language mnemonic for the opcode.
		 */
		public String name;
		/**
		 * Number of args; -1 if variable #args
		 */
		public int numOperands;
		/**
		 * Position of the result; -1 if no result
		 */
		public int resultPos;
		/**
		 * Number of delay slots after instruction
		 */
		public int numDelaySlots;
		/**
		 * Latency in machine cycles
		 */
		public int latency;
		/**
		 * flags identifying machine instr class
		 */
		public int flags;
		/**
		 * Target Specific Flag values
		 */
		public int tSFlags;
		/**
		 * Registers implicitly read by this instr
		 */
		public int[] implicitUses;
		/**
		 * Registers implicitly defined by this instr
		 */
		public int[] implicitDefs;

		public TargetInstrDescriptor(int opcode, String name, int numOprs,
				int resPos, int flags, int TSFlags,
				int[] implUses, int[] implDefs)
		{
			opCode = opcode;
			this.name = name;
			numOperands = numOprs;
			resultPos =  resPos;
			this.numDelaySlots =0;
			this.latency = 0;
			this.flags = flags;
			tSFlags = TSFlags;
			implicitUses = implUses;
			implicitDefs = implDefs;
		}

		/**
		 * The constructor that creats an instance of class {@linkplain TargetInstrDescriptor}
		 * with the specified several parameters.
		 * @param opcode    The opcode.
		 * @param name      The instruction memonic.
		 * @param numOprs   The number of operands are desired.
		 * @param flags     The flags indicating machine instruction class.
		 * @param TSFlags   The target-specified flags.
		 * @param implUses  The implicitly used register.
		 * @param implDefs  The implicit registers defined by this instruction.
		 */
		public TargetInstrDescriptor(int opcode, String name, int numOprs,
				int flags, int TSFlags, int[] implUses, int[] implDefs)
		{
			this(opcode, name, numOprs, -1, flags, TSFlags, implUses, implDefs);
		}
	}

	/**
	 * an array of target instruction.
	 */
	private TargetInstrDescriptor[] descs;

	public TargetInstrInfo(TargetInstrDescriptor[] desc)
	{
		descs = desc;
		assert TargetInstrDescriptors == null && desc != null;
		TargetInstrDescriptors = desc;
	}

	public int getNumTotalOpCodes()
	{
		return descs.length;
	}

	/**
	 * Return the machine instruction descriptor that corresponds to the
	 * specified instruction opcode.
	 *
	 * @param opCode
	 * @return
	 */
	public TargetInstrDescriptor get(int opCode)
	{
		assert opCode >= 0 && opCode < getNumTotalOpCodes();
		return descs[opCode];
	}

	public String getName(int opCode)
	{
		return get(opCode).name;
	}

	public int getNumOperands(int opCode)
	{
		return get(opCode).numOperands;
	}

	public int getResultPos(int opCode)
	{
		return get(opCode).resultPos;
	}

	public int getNumDelaySlots(int opCode)
	{
		return get(opCode).numDelaySlots;
	}

	public int[] getImplicitUses(int opCode)
	{
		return get(opCode).implicitUses;
	}

	public int[] getImplicitDefs(int opCode)
	{
		return get(opCode).implicitDefs;
	}

	//=======Query instruction class flags according to the machine. //
	//=======independent flags listed above =========================//
	public boolean isNop(int opCode)
	{
		return (get(opCode).flags & M_NOP_FLAG) != 0;
	}

	public boolean isBranch(int opCode)
	{
		return (get(opCode).flags & M_BRANCH_FLAG) != 0;
	}

	public boolean isCall(int opCode)
	{
		return (get(opCode).flags & M_CALL_FLAG) != 0;
	}

	public boolean isReturn(int opCode)
	{
		return (get(opCode).flags & M_RET_FLAG) != 0;
	}

	public boolean isControlFlow(int opCode)
	{
		return (get(opCode).flags & M_BRANCH_FLAG) != 0
				|| (get(opCode).flags & M_CALL_FLAG) != 0
				|| (get(opCode).flags & M_RET_FLAG) != 0;
	}

	public boolean isArith(int opCode)
	{
		return (get(opCode).flags & M_ARITH_FLAG) != 0;
	}

	public boolean isCCInstr(int opCode)
	{
		return (get(opCode).flags & M_CC_FLAG) != 0;
	}

	public boolean isLogical(int opCode)
	{
		return (get(opCode).flags & M_LOGICAL_FLAG) != 0;
	}

	public boolean isIntInstr(int opCode)
	{
		return (get(opCode).flags & M_INT_FLAG) != 0;
	}

	public boolean isFloatInstr(int opCode)
	{
		return (get(opCode).flags & M_FLOAT_FLAG) != 0;
	}

	public boolean isConditional(int opCode)
	{
		return (get(opCode).flags & M_CONDL_FLAG) != 0;
	}

	public boolean isLoad(int opCode)
	{
		return (get(opCode).flags & M_LOAD_FLAG) != 0;
	}

	public boolean isPrefetch(int opCode)
	{
		return (get(opCode).flags & M_PREFETCH_FLAG) != 0;
	}

	public boolean isLoadOrPrefetch(int opCode)
	{
		return (get(opCode).flags & M_LOAD_FLAG) != 0
				|| (get(opCode).flags & M_PREFETCH_FLAG) != 0;
	}

	public boolean isStore(int opCode)
	{
		return (get(opCode).flags & M_STORE_FLAG) != 0;
	}

	public boolean isMemoryAccess(int opCode)
	{
		return (get(opCode).flags & M_LOAD_FLAG) != 0
				|| (get(opCode).flags & M_PREFETCH_FLAG) != 0
				|| (get(opCode).flags & M_STORE_FLAG) != 0;
	}

	public boolean isDummyPhiInstr(int opCode)
	{
		return (get(opCode).flags & M_DUMMY_PHI_FLAG) != 0;
	}

	public boolean isPseudoInstr(int opCode)
	{
		return (get(opCode).flags & M_PSEUDO_FLAG) != 0;
	}

	public boolean isTwoAddrInstr(int opCode)
	{
		return (get(opCode).flags & M_2_ADDR_FLAG) != 0;
	}

	public boolean isTerminatorInstr(int Opcode)
	{
		return (get(Opcode).flags & M_TERMINATOR_FLAG) != 0;
	}

	/**
	 * Returns the target's implementation of NOP, which is
	 * usually a pseudo-instruction, implemented by a degenerate version of
	 * another instruction, e.g. X86: xchg ax, ax;
	 * @return
	 */
	public abstract MachineInstr createNOPinstr();

	/**
	 * Not having a special NOP opcode, we need to know if a given
	 * instruction is interpreted as an `official' NOP instr, i.e., there may be
	 * more than one way to `do nothing' but only one canonical way to slack off.
	 * @param mi
	 * @return
	 */
	public abstract boolean isNOPinstr(MachineInstr mi);
}
