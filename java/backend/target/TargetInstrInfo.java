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
	/**
	 * Invariant opcodes: All instruction sets have these as their low opcodes.
	 */
    public static final int PHI = 0;
    public static final int INLINEASM = 1;
    public static final int DBG_LABEL = 2;
    public static final int EH_LABEL = 3;
    public static final int GC_LABEL = 4;
    public static final int DECLARE = 5;

    /// EXTRACT_SUBREG - This instruction takes two operands: a register
    /// that has subregisters, and a subregister index. It returns the
    /// extracted subregister value. This is commonly used to implement
    /// truncation operations on target architectures which support it.
    public static final int EXTRACT_SUBREG = 6;

    /// INSERT_SUBREG - This instruction takes three operands: a register
    /// that has subregisters, a register providing an insert value, and a
    /// subregister index. It returns the value of the first register with
    /// the value of the second register inserted. The first register is
    /// often defined by an IMPLICIT_DEF, as is commonly used to implement
    /// anyext operations on target architectures which support it.
    public static final int INSERT_SUBREG = 7;

    /// IMPLICIT_DEF - This is the MachineInstr-level equivalent of undef.
    public static final int IMPLICIT_DEF = 8;

    /// SUBREG_TO_REG - This instruction is similar to INSERT_SUBREG except
    /// that the first operand is an immediate integer constant. This constant
    /// is often zero, as is commonly used to implement zext operations on
    /// target architectures which support it, such as with x86-64 (with
    /// zext from i32 to i64 via implicit zero-extension).
    public static final int SUBREG_TO_REG = 9;

    /// COPY_TO_REGCLASS - This instruction is a placeholder for a plain
    /// register-to-register copy into a specific register class. This is only
    /// used between instruction selection and MachineInstr creation, before
    /// virtual registers have been created for all the instructions, and it's
    /// only needed in cases where the register classes implied by the
    /// instructions are insufficient. The actual MachineInstrs to perform
    /// the copy are emitted with the TargetInstrInfo::copyRegToReg hook.
    public static final int COPY_TO_REGCLASS = 10;

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
		return (get(opCode).flags & TID.M_NOP_FLAG) != 0;
	}

	public boolean isBranch(int opCode)
	{
		return (get(opCode).flags & TID.M_BRANCH_FLAG) != 0;
	}

	public boolean isCall(int opCode)
	{
		return (get(opCode).flags & TID.M_CALL_FLAG) != 0;
	}

	public boolean isReturn(int opCode)
	{
		return (get(opCode).flags & TID.M_RET_FLAG) != 0;
	}

	public boolean isControlFlow(int opCode)
	{
		return (get(opCode).flags & TID.M_BRANCH_FLAG) != 0
				|| (get(opCode).flags & TID.M_CALL_FLAG) != 0
				|| (get(opCode).flags & TID.M_RET_FLAG) != 0;
	}

	public boolean isArith(int opCode)
	{
		return (get(opCode).flags & TID.M_ARITH_FLAG) != 0;
	}

	public boolean isCCInstr(int opCode)
	{
		return (get(opCode).flags & TID.M_CC_FLAG) != 0;
	}

	public boolean isLogical(int opCode)
	{
		return (get(opCode).flags & TID.M_LOGICAL_FLAG) != 0;
	}

	public boolean isIntInstr(int opCode)
	{
		return (get(opCode).flags & TID.M_INT_FLAG) != 0;
	}

	public boolean isFloatInstr(int opCode)
	{
		return (get(opCode).flags & TID.M_FLOAT_FLAG) != 0;
	}

	public boolean isConditional(int opCode)
	{
		return (get(opCode).flags & TID.M_CONDL_FLAG) != 0;
	}

	public boolean isLoad(int opCode)
	{
		return (get(opCode).flags & TID.M_LOAD_FLAG) != 0;
	}

	public boolean isPrefetch(int opCode)
	{
		return (get(opCode).flags & TID.M_PREFETCH_FLAG) != 0;
	}

	public boolean isLoadOrPrefetch(int opCode)
	{
		return (get(opCode).flags & TID.M_LOAD_FLAG) != 0
				|| (get(opCode).flags & TID.M_PREFETCH_FLAG) != 0;
	}

	public boolean isStore(int opCode)
	{
		return (get(opCode).flags & TID.M_STORE_FLAG) != 0;
	}

	public boolean isMemoryAccess(int opCode)
	{
		return (get(opCode).flags & TID.M_LOAD_FLAG) != 0
				|| (get(opCode).flags & TID.M_PREFETCH_FLAG) != 0
				|| (get(opCode).flags & TID.M_STORE_FLAG) != 0;
	}

	public boolean isDummyPhiInstr(int opCode)
	{
		return (get(opCode).flags & TID.M_DUMMY_PHI_FLAG) != 0;
	}

	public boolean isPseudoInstr(int opCode)
	{
		return (get(opCode).flags & TID.M_PSEUDO_FLAG) != 0;
	}

	public boolean isTwoAddrInstr(int opCode)
	{
		return (get(opCode).flags & TID.M_2_ADDR_FLAG) != 0;
	}

	public boolean isTerminatorInstr(int Opcode)
	{
		return (get(Opcode).flags & TID.M_TERMINATOR_FLAG) != 0;
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
