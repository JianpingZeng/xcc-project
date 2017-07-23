package backend.codegen;

import backend.target.TargetInstrInfo;
import backend.target.TargetInstrDesc;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Representation of each machine instruction.
 *
 * <p>MachineOpCode must be an enum, defined separately for each target.
 * E.g., It is defined in .
 * </p>
 * <p>There are 2 kinds of operands:
 * <ol>
 * <li>Explicit operands of the machine instruction in vector operands[].
 *  And the more important is that the format of MI is compatible with AT&T
 *  assembly, where dest operand is in the leftmost as follows:
 *  op dest, op0, op1;
 *  op dest op0.
 * </li>
 * <li>"Implicit operands" are values implicitly used or defined by the
 * machine instruction, such as arguments to a CALL, return value of
 * a CALL (if any), and return value of a RETURN.</li>
 * </ol>
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineInstr implements Cloneable
{
	private TargetInstrDesc tid;

	private int opCode;              // the opcode
	private int opCodeFlags;         // flags modifying instrn behavior

	/**
	 * |<-----Explicit operands--------->|-----Implicit operands------->|
	 */
	private ArrayList<MachineOperand> operands; // the operands
	private int numImplicitOps;             // number of implicit operands

	private MachineBasicBlock parent;

	private ArrayList<MachineMemOperand> memOperands = new ArrayList<>();

	/**
	 * Return true if it's illegal to add a new operand.
	 *
	 * @return
	 */
	private boolean operandsComplete()
	{
		int numOperands = TargetInstrInfo.targetInstrDescs[opCode].numOperands;
		if (numOperands >= 0 && numOperands <= getNumOperands())
			return true;
		return false;
	}

	public MachineInstr(TargetInstrDesc tid)
	{
		this(tid, false);
	}

	public MachineInstr(TargetInstrDesc tid, boolean noImp)
	{
		this.tid = tid;
		if (!noImp && tid.getImplicitDefs() != null)
		{
			numImplicitOps += tid.getImplicitDefs().length;
		}
		if (!noImp && tid.getImplicitUses() != null)
		{
			numImplicitOps += tid.getImplicitUses().length;
		}

		operands = new ArrayList<>();
		for (int idx = numImplicitOps + tid.getNumOperands(); idx > 0; idx--)
			operands.add(null);
		if (!noImp)
			addImplicitDefUseOperands();
	}

	public MachineInstr(MachineBasicBlock mbb, TargetInstrDesc tid)
	{
		this(tid, false);
		parent = mbb;
		assert mbb
				!= null : "Can not use inserting operation with null basic block";
		mbb.addLast(this);
	}

	public void addImplicitDefUseOperands()
	{
		if (tid.implicitDefs != null)
		{
			for (int i = 0, e = tid.implicitDefs.length; i < e; i++)
				addOperand(MachineOperand
						.createReg(tid.getImplicitDefs()[i], true, true));
		}
		if (tid.implicitUses != null)
		{
			for (int i = 0; i < tid.implicitUses.length; i++)
				addOperand(MachineOperand
						.createReg(tid.implicitUses[i], false, true));
		}
	}

	/**
	 * Add the specified operand to the instruction.  If it is an
	 * implicit operand, it is added to the end of the operand list.  If it is
	 * an explicit operand it is added at the end of the explicit operand list
	 * (before the first implicit operand).
	 *
	 * @param mo
	 */
	public void addOperand(MachineOperand mo)
	{
		// TODO: 17-7-16
	}

	public void removeOperand(int opNo)
	{
		assert opNo >= 0 && opNo < operands.size();

		int size = operands.size();
		if (opNo == size - 1)
		{
			if (operands.get(size - 1).isReg() && operands.get(size - 1).isOnRegUseList())
				operands.get(size - 1).removeRegOperandFromRegInfo();

			operands.remove(size - 1);
			return;
		}

		MachineRegisterInfo regInfo = getRegInfo();
		if (regInfo != null)
		{
			for (int i = opNo, e = operands.size(); i != e; i++)
			{
				if (operands.get(i).isReg())
					operands.get(i).removeRegOperandFromRegInfo();
			}
		}
		operands.remove(opNo);

		if (regInfo != null)
		{
			for (int i = opNo, e = operands.size(); i != e; i++)
			{
				if (operands.get(i).isReg())
					operands.get(i).addRegOperandToRegInfo(regInfo);
			}
		}
	}

	public MachineRegisterInfo getRegInfo()
	{
		return parent == null ? null :
				parent.getParent().getMachineRegisterInfo();
	}

	public int getOpcode()
	{
		return tid.opCode;
	}

	public int getNumOperands()
	{
		return operands.size();
	}

	public MachineOperand getOperand(int index)
	{
		assert index >= 0 && index < getNumOperands();
		return operands.get(index);
	}

	public int getExplicitOperands()
	{
		int numOperands = tid.getNumOperands();
		if (!tid.isVariadic())
			return numOperands;

		for (int i = numOperands, e = getNumOperands(); i != e; i++)
		{
			MachineOperand mo = getOperand(i);
			if (!mo.isReg() || !mo.isImplicit())
				numOperands++;
		}
		return numOperands;
	}

	public boolean isIdenticalTo(MachineInstr other)
	{
		if (other.getOpcode() != getOpcode() || other.getNumOperands() != getNumOperands())
			return false;

		for (int i = 0, e = getNumOperands(); i != e; i++)
			if (!getOperand(i).isIdenticalTo(other.getOperand(i)))
				return false;
		return true;
	}

	public MachineInstr removeFromParent()
	{
		assert getParent() != null;
		parent.remove(this);
		return this;
	}

	public boolean isLabel()
	{
		int op = getOpcode();
		return op == TargetInstrInfo.DBG_LABEL || op == TargetInstrInfo.EH_LABEL
				|| op == TargetInstrInfo.GC_LABEL;
	}

	public boolean isDebugLabel()
	{
		return getOpcode() == TargetInstrInfo.DBG_LABEL;
	}

	public boolean readsRegister(int reg, TargetRegisterInfo tri)
	{
		return findRegisterUseOperandIdx(reg, false, tri) != -1;
	}

	public boolean killsRegister(int reg, TargetRegisterInfo tri)
	{
		return findRegisterUseOperandIdx(reg, true, tri) != -1;
	}

	public boolean modifiedRegister(int reg, TargetRegisterInfo tri)
	{
		return findRegisterDefOperandIdx(reg, false, tri) != -1;
	}

	public boolean registerDefIsDead(int reg, TargetRegisterInfo tri)
	{
		return findRegisterDefOperandIdx(reg, true, tri) != -1;
	}

	public int findRegisterUseOperandIdx(int reg, boolean isKill,
			TargetRegisterInfo tri)
	{
	}

	public int findRegisterDefOperandIdx(int reg, boolean isKill,
			TargetRegisterInfo tri)
	{
	}

	public MachineOperand findRegisterDefOperand(int reg, boolean isDead,
			TargetRegisterInfo tri)
	{
	}

	public int findFirstPredOperandIdx()
	{
	}

	public boolean isRegTiedToUseOperand(int defOpIdx, int[] useOpIdx)
	{
	}

	public boolean isRegTiedToDefOperand(int useOpIdx, int[] defOpIdx)
	{
	}

	public void copyKillDeadInfo(MachineInstr mi)
	{
		// TODO: 17-7-16
	}

	public void copyPredicates(MachineInstr mi)
	{
		// TODO: 17-7-16
	}

    boolean addRegisterKilled(int IncomingReg, TargetRegisterInfo RegInfo)
    {
        return addRegisterKilled(IncomingReg, RegInfo, false);
    }

	/// addRegisterKilled - We have determined MI kills a register. Look for the
	/// operand that uses it and mark it as IsKill. If AddIfNotFound is true,
	/// add a implicit operand if it's not found. Returns true if the operand
	/// exists / is added.
	boolean addRegisterKilled(int IncomingReg, TargetRegisterInfo RegInfo,
			boolean AddIfNotFound)
	{
	}

    boolean addRegisterDead(int IncomingReg, TargetRegisterInfo RegInfo)
    {
        return addRegisterDead(IncomingReg, RegInfo, false);
    }

	/// addRegisterDead - We have determined MI defined a register without a use.
	/// Look for the operand that defines it and mark it as IsDead. If
	/// AddIfNotFound is true, add a implicit operand if it's not found. Returns
	/// true if the operand exists / is added.
	boolean addRegisterDead(int IncomingReg, TargetRegisterInfo RegInfo,
			boolean AddIfNotFound)
	{
	}

	/// isSafeToMove - Return true if it is safe to move this instruction. If
	/// SawStore is set to true, it means that there is a store (or call) between
	/// the instruction's location and its intended destination.
	boolean isSafeToMove(TargetInstrInfo TII, boolean SawStore)
	{
	}

	/// isSafeToReMat - Return true if it's safe to rematerialize the specified
	/// instruction which defined the specified register instead of copying it.
	boolean isSafeToReMat(TargetInstrInfo TII, int DstReg)
	{
	}

	/// hasVolatileMemoryRef - Return true if this instruction may have a
	/// volatile memory reference, or if the information describing the
	/// memory reference is not available. Return false if it is known to
	/// have no volatile memory references.
	boolean hasVolatileMemoryRef()
	{}

	public void print(PrintStream os, TargetMachine tm)
	{}

	public void dump()
	{
		print(System.err, null);
	}

	public void setDesc(TargetInstrDesc tid)
	{
		this.tid = tid;
	}

	/**
	 * Unlink all of the register operands in
	 * this instruction from their respective use lists.  This requires that the
	 * operands already be on their use lists
	 */
	private void removeRegOperandsFromUseList()
	{
		// TODO: 17-7-16
	}

	/**
	 * Add all of the register operands in
	 * this instruction from their respective use lists.  This requires that the
	 * operands not be on their use lists yet.
	 * @param regInfo
	 */
	private void addRegOperandsToUseLists(MachineRegisterInfo regInfo)
	{
		// TODO: 17-7-16
	}

	public MachineBasicBlock getParent()
	{
		return parent;
	}

	public void setParent(MachineBasicBlock mbb) {parent = mbb;}

	public TargetInstrDesc getDesc()
	{
		return tid;
	}

	public void addMemOperand(MachineMemOperand mmo)
	{
		memOperands.add(mmo);
	}

	public int getIndexOf(MachineOperand op)
	{
		return operands.indexOf(op);
	}

	@Override
	public MachineInstr clone()
	{
		MachineInstr res = new MachineInstr(tid);
		res.numImplicitOps = 0;
		res.parent = null;

		// Add operands
		for (int i = 0; i != getNumOperands(); i++)
			res.addOperand(getOperand(i));

		res.numImplicitOps = numImplicitOps;

		// Add memory operands.
		for (MachineMemOperand mmo : memOperands)
			res.addMemOperand(mmo);
		return res;
	}
}
