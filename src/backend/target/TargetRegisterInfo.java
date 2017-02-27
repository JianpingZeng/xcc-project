package backend.target;

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import gnu.trove.list.array.TIntArrayList;

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
	 */
	private TargetRegisterClass[] phyRegClasses;

	/**
	 * Register classes of target machine.
	 */
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

	protected TargetRegisterInfo(TargetRegisterDesc[] desc,
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

	protected TargetRegisterInfo(TargetRegisterDesc[] desc,
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
	public TargetRegisterDesc get(int regNo)
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
	 * Return the symbolic target specified getName for the specified physical register.
	 *
	 * @param regNo
	 * @return
	 */
	public String getName(int regNo)
	{
		return get(regNo).name;
	}

	public abstract int[] getCalleeRegisters();

	public int getNumRegClasses()
	{
		return regClasses.length;
	}

	/**
	 * Obtains the allocatable machine register set for the specified target.
	 * @param mf
	 * @return
	 */
	public TIntArrayList getAllocatableSet(MachineFunction mf)
	{
		TIntArrayList list = new TIntArrayList();
		for (TargetRegisterClass regClass : regClasses)
			for (int i = regClass.allocatableBegin(mf),
			     e = regClass.allocatableEnd(mf); i < e; i++)
			{
				list.add(regClass.regs[i]);
			}
		return list;
	}

	/**
	 * Gets the number of machine registers in the specified target.
	 * @return
	 */
	public int getNumRegs() {return desc.length;}

	/**
	 * Checks to see if the specified register number represents a machine
	 * register.
	 * @param regNo
	 * @return true if the {@code regNo} is a machine register.
	 */
	public boolean isPhysicalRegister(int regNo)
	{
		assert regNo > 0:"This is a invalid register number!";
		return regNo < FirstVirtualRegister;
	}
	/**
	 * Checks to see if the specified register number represents a virtual
	 * register.
	 * @param regNo
	 * @return true if the {@code regNo} is a virtual register.
	 */
	public boolean isVirtualRegister(int regNo)
	{
		assert regNo > 0:"This is a invalid register number!";
		return regNo >= FirstVirtualRegister;
	}

	//===--------------------------------------------------------------------===//
	// Interfaces used by the register allocator and stack frame manipulation
	// passes to move data around between registers, immediates and memory.
	//

	/**
	 * Inserts a machine isntr into machine basic block and return the next
	 * insertion position.
	 * @param mbb
	 * @param mbbi
	 * @param srcReg
	 * @param FrameIndex
	 * @param rc
	 * @return
	 */
	public abstract int storeRegToStackSlot(MachineBasicBlock mbb, int mbbi,
			int srcReg, int FrameIndex, TargetRegisterClass rc);

	public abstract int loadRegFromStackSlot(MachineBasicBlock mbb, int mbbi,
			int destReg, int FrameIndex, TargetRegisterClass rc);

	public abstract int copyRegToReg(MachineBasicBlock mbb, int mbbi,
			int destReg, int srcReg, TargetRegisterClass rc);

	/**
	 * This method return the opcode of the frame setup instructions if
	 * they exist (-1 otherwise).  Some targets use pseudo instructions in order
	 * to abstract away the difference between operating with a frame pointer
	 * and operating without, through the use of these two instructions.
	 * @return
	 */
	public int getCallFrameSetupOpcode()
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
	public int getCallFrameDestroyOpcode()
	{
		return callFrameDestroyOpCode;
	}

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
		assert (getCallFrameSetupOpcode() == -1 && getCallFrameDestroyOpcode() == -1)
				: "eliminateCallFramePseudoInstr must be implemented if using"
						+ " call frame setup/destroy pseudo instructions!";
		assert false : "Call Frame Pseudo Instructions do not exist on this target!";
	}
	/**
	 * This method is called immediately before the specified functions frame
	 * layout (MF.getFrameInfo()) is finalized.  Once the frame is finalized,
	 * MO_FrameIndex operands are replaced with direct ants.  This method is
	 * optional.
	 */
	public abstract void processFunctionBeforeFrameFinalized(MachineFunction mf);

	/*
	 * eliminateFrameIndex - This method must be overridden to eliminate abstract
	 * frame indices from instructions which may use them.  The instruction
	 * referenced by the iterator contains an MO_FrameIndex operand which must be
	 * eliminated by this method.  This method may modify or replace the
	 * specified instruction, as long as it keeps the iterator pointing the the
	 * finished product.
	 */
	public abstract void eliminateFrameIndex(MachineFunction mf, MachineBasicBlock mbb, int ii);

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
