package backend.target;

import backend.analysis.LiveVariable;
import backend.codegen.*;
import gnu.trove.list.array.TIntArrayList;
import tools.OutParamWrapper;

import java.util.ArrayList;

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
    /// public  registers have been created for all the instructions, and it's
    /// only needed in cases where the register classes implied by the
    /// instructions are insufficient. The actual MachineInstrs to perform
    /// the copy are emitted with the TargetInstrInfo::copyRegToReg hook.
    public static final int COPY_TO_REGCLASS = 10;

	/**
	 * Describing the machine instructions initialized only when the
	 * TargetMachine class is created
	 */
	public static TargetInstrDesc[] targetInstrDescs;

	/**
	 * an array of target instruction.
	 */
	private TargetInstrDesc[] descs;

	public TargetInstrInfo(TargetInstrDesc[] desc)
	{
		descs = desc;
		assert targetInstrDescs == null && desc != null;
		targetInstrDescs = desc;
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
	public TargetInstrDesc get(int opCode)
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

	public boolean isTriviallyReMaterializable(MachineInstr mi)
	{
		return mi.getDesc().isRematerializable() && isReallyTriviallyReMaterializable(mi);
	}

	protected boolean isReallyTriviallyReMaterializable(MachineInstr mi)
	{
		return false;
	}

	public boolean isMoveInstr(MachineInstr mi,
			OutParamWrapper<Integer> srcReg,
			OutParamWrapper<Integer> destReg,
			OutParamWrapper<Integer> srcSubIdx,
			OutParamWrapper<Integer> destSubIdx)
	{
		return false;
	}

	public int isLoadFromStackSlot(MachineInstr mi,
			OutParamWrapper<Integer> frameIndex)
	{
		return 0;
	}

	public int isStoreToStackSlot(MachineInstr mi,
			OutParamWrapper<Integer> frameIndex)
	{
		return 0;
	}

	public abstract void reMaterialize(MachineBasicBlock mbb,
			int insertPos,
			int destReg,
			int subIdx,
			MachineInstr origin);

	public boolean isInvariantLoad(MachineInstr mi)
	{
		return false;
	}

	/**
	 * This method must be implemented by targets that
	 * set the M_CONVERTIBLE_TO_3_ADDR flag.  When this flag is set, the target
	 * may be able to convert a two-address instruction into one or more true
	 * three-address instructions on demand.  This allows the X86 target (for
	 * example) to convert ADD and SHL instructions into LEA instructions if they
	 * would require register copies due to two-addressness.
	 *
	 * This method returns a null pointer if the transformation cannot be
	 * performed, otherwise it returns the last new instruction.
	 * @param mf
	 * @param idxOfBB
	 * @param mbb
	 * @param idxOfInst
	 * @param lv
	 * @return
	 */
	public MachineInstr convertToThreeAddress(MachineFunction mf, int idxOfBB,
			MachineBasicBlock mbb, int idxOfInst,
			LiveVariable lv)
	{
		return null;
	}

	public  MachineInstr commuteInstruction(MachineInstr mi)
	{
		return commuteInstruction(mi, false);
	}

	/**
	 * If a target has any instructions that are commutable,
	 * but require converting to a different instruction or making non-trivial
	 * changes to commute them, this method can overloaded to do this.  The
	 * default implementation of this method simply swaps the first two operands
	 * of MI and returns it.
	 *
	 * If a target wants to make more aggressive changes, they can construct and
	 * return a new machine instruction.  If an instruction cannot commute, it
	 * can also return null.
	 *
	 * If NewMI is true, then a new machine instruction must be created.
	 * @param mi
	 * @param newMI
	 * @return
	 */
	public abstract MachineInstr commuteInstruction(MachineInstr mi, boolean newMI);

	/// findCommutedOpIndices - If specified MI is commutable, return the two
	/// operand indices that would swap value. Return true if the instruction
	/// is not in a form which this routine understands.
	public abstract boolean findCommutedOpIndices(MachineInstr MI,
			OutParamWrapper<Integer> SrcOpIdx1,
		OutParamWrapper<Integer> SrcOpIdx2);

	public  boolean AnalyzeBranch(MachineBasicBlock MBB, MachineBasicBlock TBB,
			MachineBasicBlock FBB,
			ArrayList<MachineOperand> Cond)
	{
		return AnalyzeBranch(MBB, TBB, FBB, Cond, false);
	}
	
	/// AnalyzeBranch - Analyze the branching code at the end of MBB, returning
	/// true if it cannot be understood (e.g. it's a switch dispatch or isn't
	/// implemented for a target).  Upon success, this returns false and returns
	/// with the following information in various cases:
	///
	/// 1. If this block ends with no branches (it just falls through to its succ)
	///    just return false, leaving TBB/FBB null.
	/// 2. If this block ends with only an unconditional branch, it sets TBB to be
	///    the destination block.
	/// 3. If this block ends with an conditional branch and it falls through to
	///    a successor block, it sets TBB to be the branch destination block and
	///    a list of operands that evaluate the condition. These
	///    operands can be passed to other TargetInstrInfo methods to create new
	///    branches.
	/// 4. If this block ends with a conditional branch followed by an
	///    unconditional branch, it returns the 'true' destination in TBB, the
	///    'false' destination in FBB, and a list of operands that evaluate the
	///    condition.  These operands can be passed to other TargetInstrInfo
	///    methods to create new branches.
	///
	/// Note that RemoveBranch and InsertBranch must be implemented to support
	/// cases where this method returns success.
	///
	/// If AllowModify is true, then this routine is allowed to modify the basic
	/// block (e.g. delete instructions after the unconditional branch).
	///
	public  boolean AnalyzeBranch(MachineBasicBlock MBB, MachineBasicBlock TBB,
		MachineBasicBlock FBB,
		ArrayList<MachineOperand> Cond,
		boolean AllowModify)
	{
		return true;
	}

	/// RemoveBranch - Remove the branching code at the end of the specific MBB.
	/// This is only invoked in cases where AnalyzeBranch returns success. It
	/// returns the number of instructions that were removed.
	public  int RemoveBranch(MachineBasicBlock MBB)  
	{
	assert false : "Target didn't implement TargetInstrInfo::RemoveBranch!";
	return 0;
}

	/// InsertBranch - Insert branch code into the end of the specified
	/// MachineBasicBlock.  The operands to this method are the same as those
	/// returned by AnalyzeBranch.  This is only invoked in cases where
	/// AnalyzeBranch returns success. It returns the number of instructions
	/// inserted.
	///
	/// It is also invoked by tail merging to add unconditional branches in
	/// cases where AnalyzeBranch doesn't apply because there was no original
	/// branch to analyze.  At least this much must be implemented, else tail
	/// merging needs to be disabled.
	public  int InsertBranch(MachineBasicBlock MBB,
			MachineBasicBlock TBB,
			MachineBasicBlock FBB,
            ArrayList<MachineOperand> Cond)
	{
		assert false : "Target didn't implement TargetInstrInfo::InsertBranch!";
		return 0;
	}

	/// copyRegToReg - Emit instructions to copy between a pair of registers. It
	/// returns false if the target does not how to copy between the specified
	/// registers.
	public boolean copyRegToReg(MachineBasicBlock mbb,
			int insertPos,
			int dstReg,
			int srcReg,
			TargetRegisterClass dstRC,
			TargetRegisterClass srcRC)
	{
		assert false:"Target didn't implement TargetLowering::copyRegToReg";
		return false;
	}

	/// storeRegToStackSlot - Store the specified register of the given register
	/// class to the specified stack frame index. The store instruction is to be
	/// added to the given machine basic block before the specified machine
	/// instruction. If isKill is true, the register operand is the last use and
	/// must be marked kill.
	public  void storeRegToStackSlot(MachineBasicBlock MBB,
			int pos,
			int SrcReg,
			boolean isKill,
			int FrameIndex,
			TargetRegisterClass rc)
	{
		assert false : "Target didn't implement TargetInstrInfo::storeRegToStackSlot!";
	}

	/// loadRegFromStackSlot - Load the specified register of the given register
	/// class from the specified stack frame index. The load instruction is to be
	/// added to the given machine basic block before the specified machine
	/// instruction.
	public  void loadRegFromStackSlot(MachineBasicBlock MBB,
			int pos,
			int DestReg, 
			int FrameIndex,
			TargetRegisterClass rc)  
	{
		assert false : "Target didn't implement TargetInstrInfo::loadRegFromStackSlot!";
	}

	/// spillCalleeSavedRegisters - Issues instruction(s) to spill all callee
	/// saved registers and returns true if it isn't possible / profitable to do
	/// so by issuing a series of store instructions via
	/// storeRegToStackSlot(). Returns false otherwise.
	public  boolean spillCalleeSavedRegisters(MachineBasicBlock MBB,
		int pos,
        ArrayList<CalleeSavedInfo> CSI)
	{
		return false;
	}

	/// restoreCalleeSavedRegisters - Issues instruction(s) to restore all callee
	/// saved registers and returns true if it isn't possible / profitable to do
	/// so by issuing a series of load instructions via loadRegToStackSlot().
	/// Returns false otherwise.
	public  boolean restoreCalleeSavedRegisters(MachineBasicBlock MBB,
			int pos,
			ArrayList<CalleeSavedInfo> CSI)  
	{
		return false;
	}

	/// foldMemoryOperand - Attempt to fold a load or store of the specified stack
	/// slot into the specified machine instruction for the specified operand(s).
	/// If this is possible, a new instruction is returned with the specified
	/// operand folded, otherwise NULL is returned. The client is responsible for
	/// removing the old instruction and adding the new one in the instruction
	/// stream.
	public abstract MachineInstr foldMemoryOperand(MachineFunction MF,
			MachineInstr MI,
			TIntArrayList Ops,
			int FrameIndex);

	/// foldMemoryOperand - Same as the previous version except it allows folding
	/// of any load and store from / to any address, not just from a specific
	/// stack slot.
	public abstract MachineInstr foldMemoryOperand(MachineFunction MF,
		MachineInstr MI,
		TIntArrayList Ops,
		MachineInstr LoadMI);

	
	/// foldMemoryOperandImpl - Target-dependent implementation for
	/// foldMemoryOperand. Target-independent code in foldMemoryOperand will
	/// take care of adding a MachineMemOperand to the newly created instruction.
	protected MachineInstr foldMemoryOperandImpl(MachineFunction MF,
		MachineInstr MI,
		TIntArrayList Ops,
		int FrameIndex)  
	{
		return null;
	}

	/// foldMemoryOperandImpl - Target-dependent implementation for
	/// foldMemoryOperand. Target-independent code in foldMemoryOperand will
	/// take care of adding a MachineMemOperand to the newly created instruction.
	public MachineInstr foldMemoryOperandImpl(MachineFunction MF,
		MachineInstr MI,
		TIntArrayList ops,
		MachineInstr LoadMI)
	{
		return null;
	}


	/// canFoldMemoryOperand - Returns true for the specified load / store if
	/// folding is possible.
	public boolean canFoldMemoryOperand( MachineInstr MI, TIntArrayList ops)
	{
		return false;
	}

	/// unfoldMemoryOperand - Separate a single instruction which folded a load or
	/// a store or a load and a store into two or more instruction. If this is
	/// possible, returns true as well as the new instructions by reference.
	public  boolean unfoldMemoryOperand(MachineFunction MF,
			MachineInstr MI,
			int Reg,
			boolean UnfoldLoad,
			boolean UnfoldStore,
			ArrayList<MachineInstr> NewMIs)
	{
		return false;
	}


	/// getOpcodeAfterMemoryUnfold - Returns the opcode of the would be new
	/// instruction after load / store are unfolded from an instruction of the
	/// specified opcode. It returns zero if the specified unfolding is not
	/// possible.
	public  int getOpcodeAfterMemoryUnfold(int Opc,
		boolean UnfoldLoad, boolean UnfoldStore)
	{
		return 0;
	}

	/// BlockHasNoFallThrough - Return true if the specified block does not
	/// fall-through into its successor block.  This is primarily used when a
	/// branch is unanalyzable.  It is useful for things like unconditional
	/// indirect branches (jump tables).
	public  boolean BlockHasNoFallThrough( MachineBasicBlock MBB)  {
	return false;
}

	/// ReverseBranchCondition - Reverses the branch condition of the specified
	/// condition list, returning false on success and true if it cannot be
	/// reversed.
	public boolean ReverseBranchCondition(ArrayList<MachineOperand> Cond)
	{
		return true;
	}

	/// insertNoop - Insert a noop into the instruction stream at the specified
	/// point.
	public abstract void insertNoop(MachineBasicBlock MBB, int pos);

	/// isPredicated - Returns true if the instruction is already predicated.
	///
	public  boolean isPredicated( MachineInstr MI)
	{
		return false;
	}

	/// isUnpredicatedTerminator - Returns true if the instruction is a
	/// terminator instruction that has not been predicated.
	public abstract boolean isUnpredicatedTerminator( MachineInstr MI);

	/// PredicateInstruction - Convert the instruction into a predicated
	/// instruction. It returns true if the operation was successful.
	public abstract boolean PredicateInstruction(MachineInstr MI,
                         ArrayList<MachineOperand> Pred);

	/// SubsumesPredicate - Returns true if the first specified predicate
	/// subsumes the second, e.g. GE subsumes GT.
	public boolean SubsumesPredicate( ArrayList<MachineOperand> Pred1,
                          ArrayList<MachineOperand> Pred2)
	{
		return false;
	}

	/// DefinesPredicate - If the specified instruction defines any predicate
	/// or condition code register(s) used for predication, returns true as well
	/// as the definition predicate(s) by reference.
	public  boolean DefinesPredicate(MachineInstr MI,
		ArrayList<MachineOperand> Pred)
	{
		return false;
	}

	/// isSafeToMoveRegClassDefs - Return true if it's safe to move a machine
	/// instruction that defines the specified register class.
	public  boolean isSafeToMoveRegClassDefs( TargetRegisterClass RC)
	{
		return true;
	}

	/// isDeadInstruction - Return true if the instruction is considered dead.
	/// This allows some late codegen passes to delete them.
	public abstract boolean isDeadInstruction( MachineInstr MI);

	/// GetInstSize - Returns the size of the specified Instruction.
	///
	public int GetInstSizeInBytes( MachineInstr MI)
	{
		assert false : "Target didn't implement TargetInstrInfo::GetInstSize!";
		return 0;
	}

	/// GetFunctionSizeInBytes - Returns the size of the specified
	/// MachineFunction.
	///
	public abstract  int GetFunctionSizeInBytes( MachineFunction MF);

	/// Measure the specified inline asm to determine an approximation of its
	/// length.
	// public abstract int getInlineAsmLength(String Str, TargetAsmInfo TAI);
}
