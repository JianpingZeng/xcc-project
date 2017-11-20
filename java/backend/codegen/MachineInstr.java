package backend.codegen;

import backend.support.FormattedOutputStream;
import backend.target.TargetInstrDesc;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.value.Value;
import tools.OutParamWrapper;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;

import static backend.target.TargetOperandInfo.OperandConstraint.TIED_TO;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;
import static backend.target.TargetInstrInfo.INLINEASM;

/**
 * Representation of each machine instruction.
 *
 * <p>MachineOpCode must be an enum, defined separately for each target.
 * E.g., It is defined in .
 * </p>
 * <p>There are 2 kinds of operands:
 * <ol>
 * <li>Explicit operands of the machine instruction in vector operands[].
 *  And the more important is that the format of MI is compatible with Intel
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
     * The value records the sum of implicitOps and real operands.
     * Note that it is different with operands.size() which reflect current
     * number of operands been added into operands list, but totalOperands means
     * finally number after complete operands add.
     */
	private int totalOperands;

	/**
	 * Return true if it's illegal to add a new operand.
	 *
	 * @return
	 */
	private boolean operandsComplete()
	{
		int numOperands = tid.getNumOperands();
		if (!tid.isVariadic() && getNumOperands() - numImplicitOps >= numOperands)
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
        totalOperands = numImplicitOps + tid.getNumOperands();
		operands = new ArrayList<>();

		if (!noImp)
			addImplicitDefUseOperands();
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
		boolean isImpReg = mo.isRegister() && mo.isImplicit();
		assert isImpReg || !operandsComplete() :
                "Try to add an operand to a machine instr that is already done!";

		MachineRegisterInfo regInfo = getRegInfo();
        // If we are adding the operand to the end of the list, our job is simpler.
        // This is true most of the time, so this is a reasonable optimization.
		if (isImpReg || numImplicitOps == 0)
        {
            if (operands.isEmpty() || operands.size() < totalOperands)
            {
                mo.setParentMI(this);
                operands.add(mo);
                if (mo.isRegister())
                    mo.addRegOperandToRegInfo(regInfo);
                return;
            }
        }
        // Otherwise, we have to insert a real operand before any implicit ones.
        int opNo = operands.size() - numImplicitOps;
		if (regInfo == null)
        {
            mo.setParentMI(this);
            operands.add(opNo, mo);

            if (mo.isRegister())
                mo.addRegOperandToRegInfo(null);
        }
        else if (operands.size() < totalOperands)
        {
            for (int i = opNo; i < operands.size(); i++)
            {
                assert operands.get(i).isRegister():"Should only be an implicit register!";
                operands.get(i).removeRegOperandFromRegInfo();
            }

            operands.add(opNo, mo);
            mo.setParentMI(this);
            if (mo.isRegister())
                mo.addRegOperandToRegInfo(regInfo);

            // re-add all the implicit ops.
            for (int i = opNo+1; i < operands.size(); i++)
            {
                assert operands.get(i).isRegister():"Should only be an implicit register!";
                operands.get(i).addRegOperandToRegInfo(regInfo);
            }
        }
        else
        {
            removeRegOperandsFromUseList();
            operands.add(opNo, mo);
            mo.setParentMI(this);
            addRegOperandsToUseLists(regInfo);
        }
	}

	public void removeOperand(int opNo)
	{
		assert opNo >= 0 && opNo < operands.size();

		int size = operands.size();
		if (opNo == size - 1)
		{
			if (operands.get(size - 1).isRegister() && operands.get(size - 1).isOnRegUseList())
				operands.get(size - 1).removeRegOperandFromRegInfo();

			operands.remove(size - 1);
			return;
		}

		MachineRegisterInfo regInfo = getRegInfo();
		if (regInfo != null)
		{
			for (int i = opNo, e = operands.size(); i != e; i++)
			{
				if (operands.get(i).isRegister())
					operands.get(i).removeRegOperandFromRegInfo();
			}
		}
		operands.remove(opNo);

		if (regInfo != null)
		{
			for (int i = opNo, e = operands.size(); i != e; i++)
			{
				if (operands.get(i).isRegister())
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
			if (!mo.isRegister() || !mo.isImplicit())
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

	public boolean killsRegister(int reg)
	{
		return killsRegister(reg, null);
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

	/**
	 * Returns the MachineOperand that is a use of
	 * the specific register or -1 if it is not found. It further tightening
	 * the search criteria to a use that kills the register if isKill is true.
	 * @param reg
	 * @param isKill
	 * @param tri
	 * @return
	 */
	public int findRegisterUseOperandIdx(int reg,
            boolean isKill,
			TargetRegisterInfo tri)
	{
	    for (int i = 0, e = getNumOperands(); i != e; i++)
        {
            MachineOperand mo = getOperand(i);
            if (!mo.isUse() || !mo.isRegister())
                continue;

            int moreg = mo.getReg();
            if (moreg == 0)
                continue;
            if (moreg == reg || (tri != null && isPhysicalRegister(moreg))
                    && isPhysicalRegister(reg))
            {
                if (!isKill || mo.isKill())
                    return i;
            }
        }
        return -1;
	}

	public MachineOperand findRegisterUseOperand(int reg, boolean isKill, TargetRegisterInfo tri)
	{
		int idx = findRegisterUseOperandIdx(reg, isKill, tri);
		return idx == -1 ? null : getOperand(idx);

	}

    /**
     * Returns the MachineOperand that is a def of the specific register or -1
     * if it is not found. It further tightening
     * the search criteria to a use that kills the register if isDead is true.
     * @param reg
     * @param isDead
     * @param tri
     * @return
     */
	public int findRegisterDefOperandIdx(
	        int reg,
            boolean isDead,
			TargetRegisterInfo tri)
	{
        for (int i = 0, e = getNumOperands(); i != e; i++)
        {
            MachineOperand mo = getOperand(i);
            if (!mo.isDef() || !mo.isRegister())
                continue;

            int moreg = mo.getReg();
            if (moreg == 0)
                continue;
            if (moreg == reg || (tri != null && isPhysicalRegister(moreg))
                    && isPhysicalRegister(reg))
            {
                if (!isDead || mo.isDead())
                    return i;
            }
        }
        return -1;
	}

    /**
     * Find the index of the first operand in the
     * operand list that is used to represent the predicate. It returns -1 if
     * none is found.
     * @return
     */
	public int findFirstPredOperandIdx()
	{
        TargetInstrDesc tid = getDesc();
        if (tid.isPredicable())
        {
            for (int i = 0, e = getNumOperands(); i != e; i++)
                if (tid.opInfo[i].isPredicate())
                    return i;
        }
        return -1;
	}

    /**
     * Given the index of a register def operand,
     * check if the register def is tied to a source operand, due to either
     * two-address elimination or inline assembly constraints. Returns the
     * first tied use operand index by reference is useOpIdx is not null.
     * @param defOpIdx
     * @param useOpIdx
     * @return
     */
	public boolean isRegTiedToUseOperand(int defOpIdx, OutParamWrapper<Integer> useOpIdx)
	{
        if (getOpcode() == INLINEASM)
        {
            assert false : "Unsupported inline asm!";
            return false;
        }

        assert getOperand(defOpIdx).isDef() : "defOpIdx is not a def!";
        TargetInstrDesc TID = getDesc();
        for (int i = 0, e = TID.getNumOperands(); i != e; ++i)
        {
            MachineOperand MO = getOperand(i);
            if (MO.isRegister() && MO.isUse()
                    && TID.getOperandConstraint(i, TIED_TO) == (int) defOpIdx)
            {
                if (useOpIdx != null)
                    useOpIdx.set(i);
                return true;
            }
        }
        return false;
    }

    public boolean isRegTiedToDefOperand(int useOpIdx)
    {
        return isRegTiedToDefOperand(useOpIdx, null);
    }
    /**
     * Return true if the operand of the specified index
     * is a register use and it is tied to an def operand. It also returns the def
     * operand index by reference.
     * @param useOpIdx
     * @param defOpIdx
     * @return
     */
	public boolean isRegTiedToDefOperand(int useOpIdx, OutParamWrapper<Integer> defOpIdx)
	{
        if (getOpcode() == INLINEASM)
        {
            assert false : "Unsupported inline asm!";
            return false;
        }

        TargetInstrDesc tid = getDesc();
        if (useOpIdx >= tid.getNumOperands())
            return false;
        MachineOperand mo = getOperand(useOpIdx);
        if (!mo.isRegister() || !mo.isUse())
            return false;
        int defIdx = tid.getOperandConstraint(useOpIdx, TIED_TO);
        if (defIdx == -1)
            return false;
        if (defOpIdx != null)
            defOpIdx.set(defIdx);
        return true;
    }

    /**
     * Copies kill / dead operand properties from MI.
     * @param mi
     */
	public void copyKillDeadInfo(MachineInstr mi)
	{
		for (int i = 0, e = mi.getNumOperands(); i != e; i++)
        {
            MachineOperand mo = mi.getOperand(i);
            if (!mo.isRegister() || (!mo.isKill() && !mo.isDead()))
                continue;
            for (int j = 0, ee = getNumOperands(); j != ee; j++)
            {
                MachineOperand mop = getOperand(j);
                if (!mop.isIdenticalTo(mo))
                    continue;
                if (mo.isKill())
                    mop.setIsKill(true);
                else
                    mo.setIsDead(true);
                break;
            }
        }
	}

    /**
     * Copies predicate operand(s) from MI.
     * @param mi
     */
	public void copyPredicates(MachineInstr mi)
	{
		TargetInstrDesc tid = getDesc();
		if (!tid.isPredicable())
		    return;
		for (int i =0; i < getNumOperands(); i++)
        {
            if (tid.opInfo[i].isPredicate())
                addOperand(mi.getOperand(i));
        }
	}

    boolean addRegisterKilled(int IncomingReg, TargetRegisterInfo RegInfo)
    {
        return addRegisterKilled(IncomingReg, RegInfo, false);
    }

	/// addRegisterKilled - We have determined MI kills a register. Look for the
	/// operand that uses it and mark it as IsKill. If AddIfNotFound is true,
	/// add a implicit operand if it's not found. Returns true if the operand
	/// exists / is added.
	public boolean addRegisterKilled(int IncomingReg, TargetRegisterInfo RegInfo,
			boolean AddIfNotFound)
	{
        boolean isPhysReg = isPhysicalRegister(IncomingReg);
        int[] alias = RegInfo.getAliasSet(IncomingReg);
        boolean hasAliases = isPhysReg && alias != null && alias.length >= 0;
        boolean Found = false;
        LinkedList<Integer> DeadOps = new LinkedList<>();
        for (int i = 0, e = getNumOperands(); i != e; ++i) {
            MachineOperand MO = getOperand(i);
            if (!MO.isRegister() || !MO.isUse() || MO.isUndef())
                continue;
            int Reg = MO.getReg();
            if (Reg == 0)
                continue;

            if (Reg == IncomingReg) {
                if (!Found) {
                    if (MO.isKill())
                        // The register is already marked kill.
                        return true;
                    if (isPhysReg && isRegTiedToDefOperand(i))
                        // Two-address uses of physregs must not be marked kill.
                        return true;
                    MO.setIsKill(true);
                    Found = true;
                }
            } else if (hasAliases && MO.isKill() &&
                    isPhysicalRegister(Reg)) {
                // A super-register kill already exists.
                if (RegInfo.isSuperRegister(IncomingReg, Reg))
                    return true;
                if (RegInfo.isSubRegister(IncomingReg, Reg))
                    DeadOps.add(i);
            }
        }

        // Trim unneeded kill operands.
        while (!DeadOps.isEmpty())
        {
            int OpIdx = DeadOps.removeLast();
            if (getOperand(OpIdx).isImplicit())
                removeOperand(OpIdx);
            else
                getOperand(OpIdx).setIsKill(false);
        }

        // If not found, this means an alias of one of the operands is killed. Add a
        // new implicit operand if required.
        if (!Found && AddIfNotFound)
        {
            addOperand(MachineOperand.createReg(
                    IncomingReg,
                    false /*IsDef*/,
                    true  /*IsImp*/,
                    true  /*IsKill*/,
                    false,
                    false,
                    false,
                    0));
            return true;
        }
        return Found;
	}

    boolean addRegisterDead(int IncomingReg, TargetRegisterInfo RegInfo)
    {
        return addRegisterDead(IncomingReg, RegInfo, false);
    }

	/// addRegisterDead - We have determined MI defined a register without a use.
	/// Look for the operand that defines it and mark it as IsDead. If
	/// addIfNotFound is true, add a implicit operand if it's not found. Returns
	/// true if the operand exists / is added.
	public boolean addRegisterDead(
	        int incomingReg,
            TargetRegisterInfo regInfo,
			boolean addIfNotFound)
	{
	    boolean isPhyReg = isPhysicalRegister(incomingReg);
	    int[] alias = regInfo.getAliasSet(incomingReg);
	    boolean hasAlias = isPhyReg && alias != null && alias.length > 0;
	    boolean found = false;

        LinkedList<Integer> deadOps = new LinkedList<>();
        for (int i = 0, e = getNumOperands(); i != e; i++)
        {
            MachineOperand mo = getOperand(i);
            if (!mo.isRegister() || !mo.isDef())
                continue;
            int reg = mo.getReg();
            if (reg == 0)
                continue;

            if (reg == incomingReg)
            {
                if (!found)
                {
                    if (mo.isDead())
                        return true;
                    mo.setIsDead(true);
                    found = true;
                }
            }
            else if (hasAlias && mo.isDead() &&
                    isPhysicalRegister(reg))
            {
                if(regInfo.isSubRegister(incomingReg, reg))
                {
                    return true;
                }
                if (regInfo.getSubRegisters(incomingReg) != null &&
                        regInfo.getSuperRegisters(reg) != null &&
                        regInfo.isSubRegister(incomingReg, reg))
                {
                    deadOps.add(i);
                }
            }
        }

        while (!deadOps.isEmpty())
        {
            int opIdx = deadOps.removeLast();
            if (getOperand(opIdx).isImplicit())
                removeOperand(opIdx);
            else
                getOperand(opIdx).setIsDead(false);
        }

        if (found || !addIfNotFound)
            return found;

        addOperand(MachineOperand.createReg(incomingReg,
                true  /*IsDef*/,
                true  /*IsImp*/,
                false /*IsKill*/,
                true  /*IsDead*/,
                false, false, 0));
        return true;
	}

	/// isSafeToMove - Return true if it is safe to move this instruction. If
	/// SawStore is set to true, it means that there is a store (or call) between
	/// the instruction's location and its intended destination.
	public boolean isSafeToMove(TargetInstrInfo tii,
            OutParamWrapper<Boolean> sawStore)
	{
	    if (tid.mayStore() || tid.isCall())
        {
            sawStore.set(true);
            return false;
        }

        if (tid.isTerminator() || tid.hasUnmodeledSideEffects())
            return false;

	    if (tid.mayLoad() && !tii.isInvariantLoad(this))
	        return !sawStore.get() && !hasVolatileMemoryRef();

	    return true;
	}

	/// isSafeToReMat - Return true if it's safe to rematerialize the specified
	/// instruction which defined the specified register instead of copying it.
	public boolean isSafeToReMat(TargetInstrInfo tii, int dstReg)
	{
	    OutParamWrapper<Boolean> sawStore = new OutParamWrapper<>(false);

	    if (!getDesc().isRematerializable() ||
                !tii.isTriviallyReMaterializable(this) ||
                !isSafeToMove(tii, sawStore))
        {
            return false;
        }

        for (int i = 0, e = getNumOperands(); i != e; i++)
        {
            MachineOperand mo = getOperand(i);
            if (!mo.isRegister())
                continue;

            if (mo.isUse())
                return false;
            else if (!mo.isDead() && mo.getReg() != dstReg)
                return false;
        }
        return true;
	}

    /**
     * Return true if this instruction may have a
     * volatile memory reference, or if the information describing the
     * memory reference is not available. Return false if it is known to
     * have no volatile memory references.
     * @return
     */
	public boolean hasVolatileMemoryRef()
	{
	    if (!tid.mayStore() &&
                !tid.mayLoad() &&
                !tid.isCall() &&
                !tid.hasUnmodeledSideEffects())
	        return false;

	    if (memOperands.isEmpty())
	        return true;

	    for (MachineMemOperand mmo : memOperands)
        {
            if (mmo.isVolatile())
                return true;
        }
        return false;
    }

    public void print(FormattedOutputStream os, TargetMachine tm)
    {
	    int startOp = 0;
	    if (getNumOperands()!= 0 && getOperand(0).isRegister() &&
			    getOperand(0).isDef())
	    {
		    getOperand(0).print(os, tm);
		    os.print(" = ");
		    ++startOp;
	    }

	    os.printf(getDesc().getName());

	    for (int i = startOp, e = getNumOperands(); i != e; i++)
	    {
		    if (i != startOp)
			    os.print(",");
		    os.print(" ");
		    getOperand(i).print(os, tm);
	    }

	    if (!memOperands.isEmpty())
	    {
		    for (MachineMemOperand mmo : memOperands)
		    {
			    Value v = mmo.getValue();

			    assert mmo.isLoad() || mmo.isStore() :"SV has to be a load, store or both";

			    if (mmo.isVolatile())
				    os.print("Volatile ");
			    if (mmo.isLoad())
				    os.printf("LD");
			    if (mmo.isStore())
				    os.printf("ST");

			    os.printf("(%d,%d) [", mmo.getSize(), mmo.getAlignment());
			    if (v == null)
			    {
				    os.print("<unknown>");
			    }
			    else if (!v.getName().isEmpty())
			    {
				    os.print(v.getName());
			    }
			    else
				    v.print(os);

			    os.printf(" + %d]", mmo.getOffset());
		    }
	    }
	    os.println();
    }

	public void print(PrintStream os, TargetMachine tm)
	{
	    try(FormattedOutputStream out = new FormattedOutputStream(os))
	    {
	    	print(out, tm);
	    }
	    catch (Exception e)
	    {
	    	e.printStackTrace();
	    }
    }

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
        for (int i = 0, e = operands.size(); i != e; i++)
        {
            if (operands.get(i).isRegister())
                operands.get(i).removeRegOperandFromRegInfo();
        }
	}

	/**
	 * Add all of the register operands in
	 * this instruction from their respective use lists.  This requires that the
	 * operands not be on their use lists yet.
	 * @param regInfo
	 */
    public void addRegOperandsToUseLists(MachineRegisterInfo regInfo)
	{
		for (int i = 0, e = operands.size(); i != e; i++)
        {
            if (operands.get(i).isRegister())
                operands.get(i).addRegOperandToRegInfo(regInfo);
        }
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

	public boolean hasOneMemOperand()
	{
		return !memOperands.isEmpty();
	}

	public MachineMemOperand getMemOperand(int index)
	{
		assert index >= 0 && index < memOperands.size();
		return memOperands.get(index);
	}

	public void setMachineOperandReg(int idx, int physReg)
	{
		assert idx >= 0 && idx < getNumOperands();
		assert isPhysicalRegister(physReg);
		assert getOperand(idx).isRegister();
		getOperand(idx).setReg(physReg);
	}

	public int index()
	{
		return getParent().getInsts().indexOf(this);
	}
}
