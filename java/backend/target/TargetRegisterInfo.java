package backend.target;

import backend.codegen.*;
import tools.BitMap;
import tools.OutParamWrapper;

import java.util.ArrayList;

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

	private TargetRegisterDesc[] desc;
	/**
	 * Mapping the machine register number to its register class.
     * Fixme, this is not needed.
	 */
	// private TargetRegisterClass[] phyRegClasses;

	/**
	 * Register classes of target machine.
	 */
	private TargetRegisterClass[] regClasses;

	/**
	 * The opcode of setting up stack frame for function being compiled.
	 * If the target machine does not support it, this field will be -1.
	 */
	private int callFrameSetupOpcode;
	/**
	 * The opcode of destroying stack frame for function being compiled.
	 * If the target machine does not support it, this field will be -1.
	 */
	private int callFrameDestroyOpcode;

	protected int[] subregHash;
	protected int[] superregHash;
	protected int[] aliasesHash;
	int subregHashSize;
	int superregHashSize;
	int aliasHashSize;

	protected TargetRegisterInfo(
			TargetRegisterDesc[] desc,
			TargetRegisterClass[] regClasses,
			int callFrameSetupOpCode,
			int callFrameDestroyOpCode,
			int[] subregs, int subregHashSize,
			int[] superregs, int superregHashSize,
			int[] aliases, int aliasHashSize)
	{
		this.desc = desc;
		this.regClasses = regClasses;

		// loop over all register classes, handle each register class
		// and keep track of diagMapping from register to it's register class.
        /**
         * FIXME, a physical register may occurs in multiple reg class, So comment it!
        phyRegClasses = new TargetRegisterClass[desc.length];
		for (int i = 0, e = regClasses.length; i < e; i++)
		{
			for (int j = 0, ee = regClasses[i].getRegSize(); j < ee; j++)
			{
				int reg = regClasses[i].getRegister(j);
				if (phyRegClasses[reg] != null)
                {
                    System.err.print(regClasses[i].getName());
                    System.err.print(": [");
                    for (int r : regClasses[i].getRegs())
                        System.err.print(getName(r)+",");
                    System.err.print("]\n");

                    System.err.print("Before: " + phyRegClasses[reg].getName());
                    System.err.print(": [");
                    for (int r : phyRegClasses[reg].getRegs())
                        System.err.print(getName(r)+",");
                    System.err.print("]\n");
                }
				assert phyRegClasses[reg]
						== null : "register in more than one class!";
				phyRegClasses[reg] = regClasses[i];
			}
		}
		*/
		this.callFrameSetupOpcode = callFrameSetupOpCode;
		this.callFrameDestroyOpcode = callFrameDestroyOpCode;
		subregHash = subregs;
		this.subregHashSize = subregHashSize;
		superregHash = superregs;
		this.superregHashSize = superregHashSize;
		aliasesHash = aliases;
		this.aliasHashSize = aliasHashSize;
	}

	protected TargetRegisterInfo(TargetRegisterDesc[] desc,
			TargetRegisterClass[] phyRegClasses)
	{
		this(desc, phyRegClasses, -1, -1);
	}

	protected TargetRegisterInfo(TargetRegisterDesc[] desc,
			TargetRegisterClass[] regClasses, int callFrameSetupOpCode,
			int callFrameDestroyOpCode)
	{
		this(desc, regClasses, callFrameSetupOpCode, callFrameDestroyOpCode,
				null, 0, null,0, null, 0);
	}

	public static boolean isPhysicalRegister(int reg)
	{
		assert reg != 0 : "this is not a register";
		return reg < FirstVirtualRegister;
	}

	public static boolean isVirtualRegister(int reg)
	{
		assert reg != 0 : "this is not a register";
		return reg >= FirstVirtualRegister;
	}

	public TargetRegisterClass getPhysicalRegisterRegClass(int reg)
	{
		return getPhysicalRegisterRegClass(reg, new EVT(MVT.Other));
	}

	public TargetRegisterClass getPhysicalRegisterRegClass(int reg, EVT vt)
	{
		assert isPhysicalRegister(reg):"reg must be physical register!";

		TargetRegisterClass bestRC = null;
		for (TargetRegisterClass rc : regClasses)
		{
			if ((vt.equals(new EVT(MVT.Other)) || rc.hasType(vt)) &&
					rc.contains(reg) && (bestRC == null || bestRC.hasSuperClass(rc)))
			{
				bestRC = rc;
			}
		}
		return bestRC;
	}

	/**
	 * Toggle the bits that represent allocatable
	 * registers for the specific register class.
	 * @param mf
	 * @param rc
	 * @param r
	 */
	public static void getAllocatableSetForRC(MachineFunction mf,
			TargetRegisterClass rc, BitMap r)
	{
		for (int reg : rc.getAllocableRegs(mf))
			r.set(reg);
	}

	/**
	 * Returns a bitset indexed by register number
	 * indicating if a register is allocatable or not. If a register class is
	 * specified, returns the subset for the class.
	 *
	 * @param mf
	 * @param rc
	 * @return
	 */
	public BitMap getAllocatableSet(MachineFunction mf, TargetRegisterClass rc)
	{
		BitMap allocatable = new BitMap(desc.length);
		if (rc != null)
		{
			getAllocatableSetForRC(mf, rc, allocatable);
			return allocatable;
		}
		for (TargetRegisterClass _rc : regClasses)
			getAllocatableSetForRC(mf, _rc, allocatable);

		return allocatable;
	}

	public BitMap getAllocatableSet(MachineFunction mf)
	{
		return getAllocatableSet(mf, null);
	}

	/**
	 * Obtains the register information indexed with given register number.
	 *
	 * @param regNo
	 * @return
	 */
	public TargetRegisterDesc get(int regNo)
	{
		assert regNo >= 0 && regNo < desc.length;
		return desc[regNo];
	}

	public int[] getAliasSet(int regNo)
	{
		return get(regNo).aliasSet;
	}

	public int[] getSubRegisters(int regNo)
	{
		return get(regNo).subRegs;
	}

	public int[] getSuperRegisters(int regNo)
	{
		return get(regNo).superRegs;
	}

	public String getAsmName(int regNo)
	{
		return get(regNo).asmName;
	}

	public String getName(int regNo)
	{
		return get(regNo).name;
	}

	public int getNumRegs()
	{
		return desc.length;
	}

	/**
	 * Returns true if the two registers are equal or alias each
	 * other. The registers may be virtual register.
	 * @param regA
	 * @param regB
	 * @return
	 */
	public boolean regsOverlap(int regA, int regB)
	{
		if (regA == regB)
			return true;

		if (isVirtualRegister(regA) || isVirtualRegister(regB))
			return false;

		int index = (regA + regB * 37) & (aliasHashSize - 1);

		int probeAmt = 0;
		while (aliasesHash[index * 2] != 0 && aliasesHash[index * 2 + 1] != 0)
		{
			if (aliasesHash[index * 2] == regA && aliasesHash[index * 2 + 1] == regB)
				return true;

			index = (index + probeAmt) & (aliasHashSize - 1);
			probeAmt += 2;
		}
		return false;
	}

	/**
	 * Return true if regB is a sub-register of regA.
	 *
	 * @param regA
	 * @param regB
	 * @return
	 */
	public boolean isSubRegister(int regA, int regB)
	{
		int index = (regA + regB * 37) & (subregHashSize - 1);

		int probeAmt = 2;
		while (subregHash[index * 2] != 0 && subregHash[index * 2 + 1] != 0)
		{
			if (subregHash[index * 2] == regA && subregHash[index * 2 + 1] == regB)
				return true;

			index = (index + probeAmt) & (subregHashSize - 1);
			probeAmt += 2;
		}
		return false;
	}

	/**
	 * Returns true if regB is a super-register of regA.
	 *
	 * @param regA
	 * @param regB
	 * @return
	 */
	public boolean isSuperRegister(int regA, int regB)
	{
		//System.err.printf("%d, %d. %s, %s%n", regA, regB, getName(regA), getName(regB));
		int index = (regA + regB * 37) & (superregHashSize - 1);

		int probeAmt = 2;
		while (superregHash[index * 2] != 0 && superregHash[index * 2 + 1] != 0)
		{
			if (superregHash[index * 2] == regA
					&& superregHash[index * 2 + 1] == regB)
				return true;

			index = (index + probeAmt) & (superregHashSize - 1);
			probeAmt += 2;
		}
		return false;
	}

	public abstract int[] getCalleeSavedRegs(MachineFunction mf);

	public abstract TargetRegisterClass[] getCalleeSavedRegClasses(
			MachineFunction mf);

	public abstract BitMap getReservedRegs(MachineFunction mf);

	/**
	 * Returns the physical register number of sub-register "Index"
	 * for physical register RegNo. Return zero if the sub-register does not
	 * exist.
	 *
	 * @param regNo
	 * @param index
	 * @return
	 */
	public abstract int getSubReg(int regNo, int index);

	public int getMatchingSuperReg(int reg, int subIdx, TargetRegisterClass rc)
	{
		int[] srs = getSuperRegisters(reg);
		for (int sr : srs)
		{
			if (reg == getSubReg(sr, subIdx) && rc.contains(sr))
				return sr;
		}
		return 0;
	}

	public TargetRegisterClass getMatchingSuperRegClass(TargetRegisterClass a,
			TargetRegisterClass b)
	{
		return null;
	}

	public TargetRegisterClass[] getRegClasses()
	{
		return regClasses;
	}

	public int getNumRegClasses()
	{
		return regClasses.length;
	}

	/**
	 * Returns the register class associated with the enumeration
	 * value.  See class TargetOperandInfo.
	 *
	 * @param i
	 * @return
	 */
	public TargetRegisterClass getRegClass(int i)
	{
		assert i >= 0 && i < regClasses.length;
		return regClasses[i];
	}

	/**
	 * Returns a TargetRegisterClass used for pointer
	 * values.  If a target supports multiple different pointer register classes,
	 * kind specifies which one is indicated.
	 *
	 * @param kind
	 * @return
	 */
	public TargetRegisterClass getPointerRegClass(int kind)
	{
		assert false : "Target didn't implement getPointerRegClass!";
		return null;
	}

	/**
	 * Returns a legal register class to copy a register
	 * in the specified class to or from. Returns NULL if it is possible to copy
	 * between a two registers of the specified class.
	 *
	 * @param rc
	 * @return
	 */
	public TargetRegisterClass getCrossCopyRegClass(TargetRegisterClass rc)
	{
		return null;
	}

	/**
	 * Resolves the specified register allocation hint
	 * to a physical register. Returns the physical register if it is successful.
	 *
	 * @param type
	 * @param reg
	 * @param mf
	 * @return
	 */
	public int resolveRegAllocHint(int type, int reg, MachineFunction mf)
	{
		if (type == 0 && reg != 0 && isPhysicalRegister(reg))
			return reg;
		return 0;
	}

	/**
	 * A callback to allow target a chance to update
	 * register allocation hints when a register is "changed" (e.g. coalesced)
	 * to another register. e.g. On ARM, some virtual registers should target
	 * register pairs, if one of pair is coalesced to another register, the
	 * allocation hint of the other half of the pair should be changed to point
	 * to the new register.
	 *
	 * @param reg
	 * @param newReg
	 * @param mf
	 */
	public void updateRegAllocHint(int reg, int newReg, MachineFunction mf)
	{

	}

	public boolean targetHandlessStackFrameRounding()
	{
		return false;
	}

	public boolean requireRegisterScavenging(MachineFunction mf)
	{
		return false;
	}

	public abstract boolean hasFP(MachineFunction mf);

	public boolean hasReservedCallFrame(MachineFunction mf)
	{
		return !mf.getFrameInfo().hasVarSizedObjects();
	}

	public boolean hasReservedSpillSlot(MachineFunction mf, int reg,
			OutParamWrapper<Integer> frameIdx)
	{
		return false;
	}

	public boolean needsStackRealignment(MachineFunction mf)
	{
		return false;
	}

	/**
	 * This method return the opcode of the frame setup instructions if
	 * they exist (-1 otherwise).  Some targets use pseudo instructions in order
	 * to abstract away the difference between operating with a frame pointer
	 * and operating without, through the use of these two instructions.
	 *
	 * @return
	 */
	public int getCallFrameSetupOpcode()
	{
		return callFrameSetupOpcode;
	}

	/**
	 * This method return the opcode of the frame destroy instructions if
	 * they exist (-1 otherwise).  Some targets use pseudo instructions in order
	 * to abstract away the difference between operating with a frame pointer
	 * and operating without, through the use of these two instructions.
	 *
	 * @return
	 */
	public int getCallFrameDestroyOpcode()
	{
		return callFrameDestroyOpcode;
	}

	//===--------------------------------------------------------------------===//
	// Interfaces used by the register allocator and stack frame manipulation
	// passes to move data around between registers, immediates and memory.
	//

	/**
	 * This method is called during prolog/epilog code insertion to eliminate
	 * call frame setup and destroy pseudo instructions (but only if the
	 * Target is using them).  It is responsible for eliminating these
	 * instructions, replacing them with concrete instructions.  This method
	 * need only be implemented if using call frame setup/destroy pseudo
	 * instructions.
	 */
	public void eliminateCallFramePseudoInstr(MachineFunction mf,
			MachineBasicBlock mbb, int idx)
	{
		assert (getCallFrameSetupOpcode() == -1
				&& getCallFrameDestroyOpcode() == -1) :
				"eliminateCallFramePseudoInstr must be implemented if using"
						+ " call frame setup/destroy pseudo instructions!";
		assert false : "Call Frame Pseudo Instructions do not exist on this target!";
	}

	public void processFunctionBeforeCalleeSavedScan(MachineFunction mf)
	{
		processFunctionBeforeCalleeSavedScan(mf, null);
	}
	/**
	 * This method is called immediately
	 * before PrologEpilogInserter scans the physical registers used to determine
	 * what callee saved registers should be spilled. This method is optional.
	 * @param mf
	 * @param rs
	 */
	public void processFunctionBeforeCalleeSavedScan(MachineFunction mf,
			RegScavenger rs)
	{}

	/**
	 * This method is called immediately before the specified functions frame
	 * layout (MF.getFrameInfo()) is finalized.  Once the frame is finalized,
	 * MO_FrameIndex operands are replaced with direct ants.  This method is
	 * optional.
	 */
	public void processFunctionBeforeFrameFinalized(MachineFunction mf)
	{

	}

	/*
	 * eliminateFrameIndex - This method must be overridden to eliminate abstract
	 * frame indices from instructions which may use them.  The instruction
	 * referenced by the iterator contains an MO_FrameIndex operand which must be
	 * eliminated by this method.  This method may modify or replace the
	 * specified instruction, as long as it keeps the iterator pointing the the
	 * finished product.
	 */
	public void eliminateFrameIndex(MachineFunction mf,
			MachineInstr mi)
	{
		eliminateFrameIndex(mf, mi, null);
	}

	public abstract void eliminateFrameIndex(MachineFunction mf,
			MachineInstr mi,
			RegScavenger rs);

	/**
	 * This method insert prologue code into the function.
	 */
	public abstract void emitPrologue(MachineFunction MF);
	/**
	 * This method insert epilogue code into the function.
	 */
	public abstract void emitEpilogue(MachineFunction MF,
			MachineBasicBlock mbb);

	public abstract int getFrameRegister(MachineFunction mf);

	public int getFrameIndexOffset(MachineFunction mf, int fi)
	{
		TargetFrameInfo tfi = mf.getTarget().getFrameInfo();
		MachineFrameInfo mfi = mf.getFrameInfo();
		return (int) (mfi.getObjectOffset(fi) + mfi.getStackSize() -
                        tfi.getLocalAreaOffset() + mfi.getOffsetAdjustment());
	}

	/**
	 * This method should return the register where the return
	 * address can be found.
	 * @return
	 */
	public abstract int getRARegister();

	/**
	 * Returns a list of machine moves that are assumed
	 * on entry to all functions.  Note that LabelID is ignored (assumed to be
	 * the beginning of the function.)
	 * @param moves
	 */
	public void getInitializeFrameState(ArrayList<MachineMove> moves)
	{
        // TODO: 2017/7/27
    }

	/**
	 * Checks if the specified machine instr is a move instr or not.
	 * if it is, return true and store the srcReg, destReg, srcSubReg,
	 * destSubReg into regs in the mentioned order.
	 * @param mi
	 * @param regs
	 * @return
	 */
	public boolean isMoveInstr(MachineInstr mi, int[] regs)
	{
		return false;
	}

	public static TargetRegisterClass getCommonSubClass(
			TargetRegisterClass rc1,
			TargetRegisterClass rc2)
	{
		if (rc1 == rc2) return rc1;
		if (rc1 == null || rc2 == null) return null;

		if (rc2.hasSubClass(rc1)) return rc1;

		TargetRegisterClass bestRC = null;
		for (TargetRegisterClass rc : rc1.getSubClasses())
		{
			if (rc == rc2) return rc;

			if (!rc2.hasSubClass(rc)) continue;

			if (bestRC == null || bestRC.hasSuperClass(rc))
			{
				bestRC = rc;
				continue;
			}

			if (bestRC.hasSubClass(rc))
				continue;
			int nb = bestRC.getNumRegs();
			int ni = rc.getNumRegs();
			if (ni > nb || (ni == nb && rc.getRegSize() < bestRC.getRegSize()))
				bestRC = rc;
		}
		return bestRC;
	}
}
