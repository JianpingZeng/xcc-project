package backend.codegen;

import tools.Util;
import backend.codegen.MachineRegisterInfo.DefUseChainIterator;
import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.target.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import tools.BitMap;
import tools.Pair;

import java.util.*;

import static backend.target.TargetRegisterInfo.*;

/**
 * This pass performs a pass of performing local register allocation on Machine
 * Basic Block.
 * @author Xlous.zeng
 * @version 0.1
 */
public class RegAllocLocal extends MachineFunctionPass
{
	private MachineFunction mf;
	private TargetMachine tm;
	private TargetRegisterInfo regInfo;
	private TargetInstrInfo instrInfo;

	/**
	 * Maps SSA virtual register to its frame index into the stack where
	 * there values are spilled.
	 */
	private TIntIntHashMap stackSlotForVirReg;

	/**
	 * Mapping virtual register to physical register.
	 */
	private HashMap<Integer, Integer> virToPhyRegMap;

	/**
	 * Mapping physical register to virtual register assigned to it.
     * The value of element is -2 indicates this physical register can't
     * allocatable (like, ESP). -1 indicates it is free. 0 indicates it is
     * free but reserved , also can't be allocated (like, fixed register EAX).
     * value greater than 0 is the virtual register associated with this.
	 */
	private int[] phyRegUsed;

	private boolean[] usedInMultipleBlocks;

	private MachineRegisterInfo mri;

	/**
	 * Statics data for performance evaluation.
	 */
	public static final IntStatistic NumSpilled =
            new IntStatistic("NumSpilled", "Number of spilled code");
	public static final IntStatistic NumReloaded =
            new IntStatistic("NumReloaded", "Number of reloaded code");

    private boolean[] virRegModified;

	public RegAllocLocal()
	{
		stackSlotForVirReg = new TIntIntHashMap();
		virToPhyRegMap = new HashMap<>();
	}

	private int getStackSlotForVirReg(int virReg, TargetRegisterClass rc)
	{
		// Find the location virReg would belong.
		if (stackSlotForVirReg.containsKey(virReg))
		{
			return stackSlotForVirReg.get(virReg);
		}

		// allocate a new stack object on stack frame of current mf.
		int frameIdx = mf.getFrameInfo().createStackObject(rc);
		stackSlotForVirReg.put(virReg, frameIdx);
		return frameIdx;
	}

	/**
	 * This method updates some auxiliary data structure so that we know
	 * phyReg is in the properly container for virReg. The phyReg must be
	 * not used for anything else.
	 * @param virReg
	 * @param phyReg
	 */
	private void assignVirToPhyReg(int virReg, int phyReg)
	{
		Util.assertion(phyRegUsed[phyReg] == -1, "phyreg is already assigned!");

		phyRegUsed[phyReg] = virReg;
		virToPhyRegMap.put(virReg, phyReg);
		markPhyRegRecentlyUsed(phyReg);
	}

	private int getReg(MachineBasicBlock mbb, int insertPos, int virReg)
	{
		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		// first check to see if we have a free register.
		int phyReg = getFreeReg(rc);

		if (phyReg == 0)
		{
			Util.assertion(!phyRegsUseOrder.isEmpty(), "No allocatable registers");


			// Spill a the least used physical register into memory.
			for (int i = 0; phyReg == 0; i++)
			{
				Util.assertion(i != phyRegsUseOrder.size(),  "Can't find a register of the appropriate class");


				int r = phyRegsUseOrder.get(i);
                Util.assertion(r != -1, "Physical register in phyRegsUseOrder, but it not allocated!");

                // We just can spill those physical register occupied with virtual
                // register
				if (phyRegUsed[r] > 0)
                {
                    if (rc.contains(r))
                    {
                        phyReg = r;
                        break;
                    }
                    else
                    {
                        int[] aliases = regInfo.getAliasSet(r);
                        if (aliases != null && aliases.length > 0)
                        {
                            for (int aliasReg : aliases)
                            {
                                if (rc.contains(aliasReg)
                                        && phyRegUsed[aliasReg] != 0
                                        && phyRegUsed[aliasReg] != -2)
                                {
                                    phyReg = aliasReg;
                                    break;
                                }
                            }
                        }
                    }
                }
			}

			Util.assertion(phyReg != 0, "Physical register not be assigned");

			spillPhyReg(mbb, insertPos, phyReg, false);
		}

		// now that we know which register we need to assign to this.
		assignVirToPhyReg(virReg, phyReg);
		return phyReg;
	}

	/**
	 * Returns true if the specified physical reg is not used and its alias register
	 * set also are not used.
	 * @param phyReg
	 * @return
	 */
	private boolean isPhyRegAvailable(int phyReg)
	{
		if (phyRegUsed[phyReg] != -1) return false;

		int[] aliasReg = regInfo.getAliasSet(phyReg);
		if (aliasReg != null && aliasReg.length > 0)
        {
            for (int reg : aliasReg)
                if (phyRegUsed[reg] != -1)
                    return false;
        }
		return true;
	}

	private int getFreeReg(TargetRegisterClass rc)
	{
		for (int phyReg : rc.getAllocatableRegs(mf))
		{
			if (isPhyRegAvailable(phyReg))
			{
				Util.assertion(phyReg!=0, "Can not use register!");
				return phyReg;
			}
		}
		return 0;
	}

	private void markVirRegModified(int virReg, boolean isModified)
	{
		Util.assertion( virReg >= FirstVirtualRegister);
		virReg -= FirstVirtualRegister;
		virRegModified[virReg]=isModified;
	}

	private void reloadVirReg(MachineBasicBlock mbb, int insertPos, int virReg,
            MachineInstr mi, int numOps)
	{
		// the virtual register already resides in a physical reg.
		if (virToPhyRegMap.containsKey(virReg))
		{
			int phyReg = virToPhyRegMap.get(virReg);
			markPhyRegRecentlyUsed(phyReg);
            mf.getMachineRegisterInfo().setPhysRegUsed(phyReg);
            mi.setMachineOperandReg(numOps, phyReg);
			return;
		}

		int phyReg = getReg(mbb, insertPos, virReg);

		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		int frameIdx = getStackSlotForVirReg(virReg, rc);

		// note that this reg is just reloaded.
		markVirRegModified(virReg, false);

		instrInfo.loadRegFromStackSlot(mbb, insertPos, phyReg, frameIdx, rc);
		markPhyRegRecentlyUsed(phyReg);
        mf.getMachineRegisterInfo().setPhysRegUsed(phyReg);
        mi.setMachineOperandReg(numOps, phyReg);

		// add the count for reloaded.
		NumReloaded.inc();
    }

	/**
	 * This method spills the specified physical register into the
	 * virtual register slot associated with it.  If {@code onlyVirReg} is set to true,
	 * then the request is ignored if the physical register does not contain a
	 * virtual register.
	 * @param mbb
	 * @param insertPos
	 * @param phyReg
	 * @param onlyVirReg
	 */
	private void spillPhyReg(MachineBasicBlock mbb, int insertPos, int phyReg, boolean onlyVirReg)
	{
        if(phyRegUsed[phyReg] != -1)
        {
            Util.assertion(phyRegUsed[phyReg] != -2, "Non allocatable register used!");
            if (phyRegUsed[phyReg] != 0 || !onlyVirReg)
                spillVirReg(mbb, insertPos, phyRegUsed[phyReg], phyReg);
        }
		else
		{
		    int[] alias = regInfo.getAliasSet(phyReg);
		    if (alias != null && alias.length > 0)
            {
                for (int aliasReg : alias)
                {
                    if (phyRegUsed[aliasReg]!=-1&&phyRegUsed[aliasReg]!=-2)
                        spillVirReg(mbb, insertPos, phyRegUsed[aliasReg], aliasReg);
                }
            }
		}
	}

	private boolean isVirRegModified(int virReg)
	{
		Util.assertion(virReg >= FirstVirtualRegister,  "Illegal virReg!");
		Util.assertion(virReg - FirstVirtualRegister < virRegModified.length,  "Illegal virReg!");

		return virRegModified[virReg - FirstVirtualRegister];
	}

	/**
	 * Spill the specified virtual reg into stack slot associated.
	 * @param mbb
	 * @param insertPos
	 * @param virReg
	 * @param phyReg
	 */
	private void spillVirReg(MachineBasicBlock mbb, int insertPos,
			int virReg, int phyReg)
	{
		if (virReg == 0) return;

		// We just spill those modified virtual register into memory cell.
		if (isVirRegModified(virReg))
		{
			TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
			int frameIdx = getStackSlotForVirReg(virReg, rc);
			boolean isKilled = !(insertPos < mbb.size() &&
					mbb.getInstAt(insertPos).readsRegister(phyReg, regInfo));
			instrInfo.storeRegToStackSlot(mbb, insertPos, phyReg, isKilled, frameIdx, rc);

			// add count for spilled.
			NumSpilled.inc();
		}
		virToPhyRegMap.remove(virReg, phyReg);
		removePhyReg(phyReg);
	}

	/**
	 * A list contains all being used physical register, from first one to last
	 * one, corresponding from least used to most used. It is greatly useful to
	 * sacrifice a physical register for holding another high priority virtual reg.
	 */
	private LinkedList<Integer> phyRegsUseOrder = new LinkedList<>();

	/**
	 * Returns true if they are equal or if the {@code reg1} is in the
	 * alias register set of {@code reg2}.
	 * @param reg1
	 * @param reg2
	 * @return
	 */
	private boolean areRegEqual(int reg1, int reg2)
	{
		if (reg1 == reg2) return true;

		for(int subReg : regInfo.get(reg2).subRegs)
			if (reg1 == subReg)
				return true;

		return false;
	}

	/**
	 * Mark the specified physical register is used and put it on the last position
	 * of phyRegsUseOrder list.
	 * @param reg
	 */
	private void markPhyRegRecentlyUsed(int reg)
	{
		if (phyRegsUseOrder.isEmpty())
        {
            phyRegsUseOrder.push(reg);
            return;
        }

		// the reg is most recently used.
		if (phyRegsUseOrder.getLast() == reg)
			return;

		for (int i = phyRegsUseOrder.size() - 1; i>=0; i--)
		{
		    // Check if reg is  or sub-reg of or same as element in phyRegsUseOrder
			if (areRegEqual(reg, phyRegsUseOrder.get(i)))
			{
				int regMatch = phyRegsUseOrder.get(i);
				phyRegsUseOrder.remove(i);
				// add it to the end of the list which indicates reg is most
				// recently used.
				phyRegsUseOrder.addLast(reg);
				if (regMatch == reg)
					return;  // found a exact match, return early.
			}
		}
	}

	/**
	 * This method marks the specified physical register as no longer be used.
	 * @param phyReg
	 */
	private void removePhyReg(int phyReg)
	{
	    // free this physical register.
	    phyRegUsed[phyReg] = -1;

	    if (phyRegsUseOrder.contains(phyReg))
		    phyRegsUseOrder.remove(Integer.valueOf(phyReg));
	}

	/**
	 * Make sure the specified physical register is available for
	 * use.  If there is currently a value in it, it is either moved out of the way
	 * or spilled to memory.
	 * @param mbb
	 * @param insertPos
	 * @param phyReg
	 */
	private void liberatePhyReg(MachineBasicBlock mbb, int insertPos, int phyReg)
	{
		spillPhyReg(mbb, insertPos, phyReg, false);
	}

    /**
     * Helper function to determine with MachineInstr a precedes MachineInstr
     * b within the same MBB.
     * @param a
     * @param b
     * @return
     */
	private static boolean precedes(MachineInstr a, MachineInstr b)
    {
        if (Objects.equals(a,b))
            return false;

        MachineBasicBlock mbb = a.getParent();
        for (int i = 0, e = mbb.size(); i < e; i++)
        {
            if (Objects.equals(mbb.getInstAt(i), a))
                return true;
            else if (Objects.equals(mbb.getInstAt(i), b))
                return false;
        }
        return false;
    }

    /**
     * Computes liveness of registers within a basic
     * block, setting the killed/dead flags as appropriate.
     * @param mbb
     */
	private void computeLocalLiveness(MachineBasicBlock mbb)
	{
	    MachineRegisterInfo mri = mf.getMachineRegisterInfo();
        TreeMap<Integer, Pair<MachineInstr, Integer>> lastUseDef = new TreeMap<>();
        for (int i = 0, e = mbb.size(); i < e; i++)
        {
            MachineInstr mi = mbb.getInstAt(i);
            // Handle use operand.
            for (int j = 0, sz = mi.getNumOperands(); j < sz; j++)
            {
                MachineOperand op = mi.getOperand(j);
                if (op.isRegister()&& op.getReg() != 0 && op.isUse())
                {
                    lastUseDef.put(op.getReg(), Pair.get(mi, j));

                    if (isVirtualRegister(op.getReg()))
                        continue;

                    int[] aliases = regInfo.getAliasSet(op.getReg());
                    if (aliases != null && aliases.length > 0)
                    {
                        for (int subReg : aliases)
                        {
                            if (lastUseDef.containsKey(subReg) &&
                                    !lastUseDef.get(subReg).first.equals(mi))
                            {
                                lastUseDef.put(subReg, Pair.get(mi, j));
                            }
                        }
                    }
                }
            }

            for (int j = 0, sz = mi.getNumOperands(); j < sz; j++)
            {
                MachineOperand op = mi.getOperand(j);
                // Defs others than 2-addr redefs _do_ trigger flag changes:
                //   - A def followed by a def is dead
                //   - A use followed by a def is a kill
                if (op.isRegister() && op.getReg() != 0 && op.isDef())
                {
                    if (lastUseDef.containsKey(op.getReg()))
                    {
                        // Check if this is a two address instruction.  If so, then
                        // the def does not kill the use.
                        if (lastUseDef.get(op.getReg()).first.equals(mi) &&
                                mi.isRegTiedToUseOperand(j, null))
                            continue;

                        Pair<MachineInstr, Integer> entry = lastUseDef.get(op.getReg());
                        MachineOperand lastOp = entry.first.getOperand(entry.second);
                        if (lastOp.isDef())
                            lastOp.setIsDead(true);
                        else
                            lastOp.setIsKill(true);
                    }
                    lastUseDef.put(op.getReg(), Pair.get(mi, j));
                }
            }
        }

        // Live-out (of the function) registers contain return values of the function,
        // so we need to make sure they are alive at return time.
        if (!mbb.isEmpty() && mbb.getLastInst().getDesc().isReturn())
        {
            MachineInstr ret = mbb.getLastInst();
            TIntArrayList regs = mri.getLiveOuts();
            if (regs != null && !regs.isEmpty())
            {
                for (int i = 0, e = regs.size(); i < e; i++)
                {
                    if (!ret.readsRegister(regs.get(i), regInfo))
                    {
                        ret.addOperand(MachineOperand.createReg(regs.get(i), false, true));
                        lastUseDef.put(regs.get(i), Pair.get(ret, ret.getNumOperands()-1));
                    }
                }
            }
        }

        // Finally, loop over the final use/def of each reg
        // in the block and determine if it is dead.
        for (Map.Entry<Integer, Pair<MachineInstr, Integer>> entry : lastUseDef.entrySet())
        {
            MachineInstr mi = entry.getValue().first;
            int index = entry.getValue().second;
            MachineOperand mo = mi.getOperand(index);

            boolean isPhyReg = isPhysicalRegister(mo.getReg());

            // Physical register firstly considered not used across block.
            // Virtual register also considered not used in across block if
            // it not used in another block.
            boolean usedOutsideBlock = !isPhyReg &&
                    usedInMultipleBlocks[mo.getReg() - FirstVirtualRegister];
            if (!isPhyReg && !usedOutsideBlock)
            {
                // virtual register only used in current block.
                DefUseChainIterator itr = mri.getRegIterator(mo.getReg());
                while (itr.hasNext())
                {
                    // Two cases:
                    // - used in another block
                    // - used in the same block before it is defined (loop)
                    // Case1#:
                    //          BB1    <--def/use
                    //           |
                    //          BB2    <---use
                    //
                    // Case#2:
                    //         ---- \
                    //         |     |
                    //         v     |
                    //      instr1   |<----use
                    //      instr2   |
                    //      instr3   |
                    //      instr4   |<----def
                    //        |     /
                    //        V---->
                    MachineOperand user = itr.getOpearnd();
                    MachineInstr userMI = itr.getMachineInstr();
                    if (!userMI.getParent().equals(mbb) ||
                            (mo.isDef() && user.isUse() && precedes(userMI, mi)))
                    {
                        usedInMultipleBlocks[mo.getReg()-FirstVirtualRegister] = true;
                        usedOutsideBlock = true;
                        break;
                    }
                    itr.next();
                }
            }

            // Physical registers and those that are not live-out of the block
            // are killed/dead at their last use/def within this block.
            if (isPhyReg || !usedOutsideBlock)
            {
                if (mo.isUse())
                {
                    if (!mi.isRegTiedToDefOperand(index, null))
                        mo.setIsKill(true);
                }
                else
                {
                    mo.setIsDead(true);
                }
            }
        }
    }

	/**
	 * Return true if this is an implicit kill for a read/mod/write register.
	 * Like, update partial register.
	 * @param mi
	 * @param reg
	 * @return
	 */
	private boolean isReadModWriteImplicitKill(MachineInstr mi, int reg)
	{
		for (int i = 0, e = mi.getNumOperands(); i < e; i++)
		{
			MachineOperand op = mi.getOperand(i);
			if (op.isRegister() && op.getReg() == reg && op.isDef() && !op.isDead())
				return true;
		}
		return false;
	}

	private static boolean isReadModWriteImplicitDef(MachineInstr mi, int reg)
    {
        for (int i = 0, e = mi.getNumOperands(); i != e; ++i)
        {
            MachineOperand mo = mi.getOperand(i);
            if (mo.isRegister() && mo.getReg() != 0 && mo.getReg() == reg &&
                    mo.isImplicit() && !mo.isDef() && mo.isKill())
                return true;
        }
        return false;
    }

	private void allocateBasicBlock(MachineBasicBlock mbb)
	{
		if (Util.DEBUG)
			System.err.printf("\nStarting RegAlloc of BB: %s\n", mbb.getBasicBlock().getName());

		// Add live-in registers as active.
		for (int i = 0, e = mbb.getLiveIns().size(); i < e; i++)
		{
			int reg = mbb.getLiveIns().get(i);
			mf.getMachineRegisterInfo().setPhysRegUsed(reg);
			phyRegUsed[reg] = 0;    // it is free but reserved now.
			phyRegsUseOrder.push(reg);
			for (int alieasReg : regInfo.getAliasSet(reg))
			{
				if (phyRegUsed[alieasReg] != -2)
				{
					phyRegsUseOrder.push(alieasReg);
					phyRegUsed[alieasReg] = 0;
					mf.getMachineRegisterInfo().setPhysRegUsed(alieasReg);
				}
			}
		}

		computeLocalLiveness(mbb);

		for (int i = 0; i < mbb.size(); i++)
		{
			MachineInstr mi = mbb.getInstAt(i);
			int opcode = mi.getOpcode();
			TargetInstrDesc desc = tm.getInstrInfo().get(opcode);

			if (Util.DEBUG)
			{
				System.err.printf("\nStarting RegAlloc of:");
				mi.dump();
				for (int j = 0; j < phyRegUsed.length; j++)
				{
                    int usedReg = phyRegUsed[j];
					if (usedReg != -1 && usedReg != -2)
					{
						System.err.printf("[%s, %%reg%d] ",
								regInfo.getName(j), usedReg);
					}
				}
			}

			// loop over all implicit used register, to mark it as recently used,
			// so they don't get reallocated.
            if (desc.implicitUses != null && desc.implicitUses.length > 0)
			    for (int useReg : desc.implicitUses)
				    markPhyRegRecentlyUsed(useReg);

			// Collects killed register set for avoiding redundant spill code.
			TIntArrayList kills = new TIntArrayList();
			for (int j = 0, e = mi.getNumOperands(); j < e; j++)
			{
				MachineOperand mo = mi.getOperand(j);
				if (mo.isRegister() && mo.isKill())
				{
					if (!mo.isImplicit() ||
						// These are extra physical register kills when a sub-register
						// is defined (def of a sub-register is a read/mod/write of the
						// larger registers). Ignore.
						!isReadModWriteImplicitKill(mi, mo.getReg()))
						kills.add(mo.getReg());
				}
			}

            // loop over all operands, assign physical register for it.
            for (int j = 0, e = mi.getNumOperands(); j < e; j++)
            {
                // here we are looking for only used operands (never def&use)
                if (mi.getOperand(j).isRegister()
                        && mi.getOperand(j).getReg() != 0
                        && !mi.getOperand(j).isDef()
                        && isVirtualRegister(mi.getOperand(j).getReg()))
                {
                    int virtReg = mi.getOperand(j).getReg();
                    reloadVirReg(mbb, i, virtReg, mi, j);
                }
            }


			// If this instruction is the last user of this register, kill the
			// value, freeing the register being used, so it doesn't need to be
			// spilled to memory.
			for (int j = 0, e = kills.size(); j < e; j++)
			{
				int virReg = kills.get(j);
				int phyReg = virReg;
				if (isVirtualRegister(virReg))
				{
					phyReg = virToPhyRegMap.get(virReg);
					virToPhyRegMap.remove(virReg);
				}
				else if (phyRegUsed[phyReg] == -2)
                {
                    // ignore the unallocatable register.
                }
				else
				{
                    Util.assertion(phyRegUsed[phyReg] == 0 || phyRegUsed[phyReg] == -1, 							"Silently clearing a virtual register?");

				}

				if (phyReg != 0)
				{
					if (Util.DEBUG)
                    {
                        System.err.printf(" Last use of %s[%%reg%d], removing it from live set\n",
                                regInfo.getName(phyReg), virReg);
                    }
                    removePhyReg(phyReg);
                    int[] aliasReg = regInfo.getAliasSet(phyReg);
                    if (aliasReg != null && aliasReg.length > 0)
                    {
                        for (int subReg : aliasReg)
                        {
                            if (phyRegUsed[subReg] != -2)
                            {
                                if (Util.DEBUG)
                                {
                                    System.err.printf(" Last use of %s[%%reg%d], removing it from live set\n",
                                            regInfo.getName(subReg), virReg);
                                }
                                removePhyReg(subReg);
                            }
                        }
                    }
				}
			}

			// loop over all operands which is defined physical register, and
			// spill it into stack frame for holding another high priority virtual
			// register.
			for (int j = 0, e = mi.getNumOperands(); j < e; j++)
			{
				MachineOperand op = mi.getOperand(j);
				if ((op.isRegister()
                        && op.getReg() != 0
                        && op.isDef()
                        && !op.isImplicit()
                        && !op.isEarlyClobber()
                        && isPhysicalRegister(op.getReg())))
				{
					int reg = op.getReg();
                    // if it is can't been allocatable, like ESP. Skip it.
					if (phyRegUsed[reg] == -2)
					    continue;

                    if (isReadModWriteImplicitDef(mi, op.getReg()))
                        continue;

                    mf.getMachineRegisterInfo().setPhysRegUsed(reg);
					spillPhyReg(mbb, i, reg, true);
					phyRegUsed[reg] = 0;    // it if free but reserved now.
					markPhyRegRecentlyUsed(reg);

					int[] subRegs = regInfo.getSubRegisters(reg);
					if (subRegs != null && subRegs.length > 0)
                    {
                        int num = 0, len = subRegs.length;
                        do
                        {
                            int subReg = subRegs[num];
                            if (phyRegUsed[subReg] != -2)
                            {
                                phyRegUsed[subReg] = 0;
                                markPhyRegRecentlyUsed(subReg);
                                mf.getMachineRegisterInfo().setPhysRegUsed(subReg);
                            }
                            ++num;
                        }while (num < len);
                    }
				}
			}

			// loop over all implicit defs, spilling them as well.
			if (desc.implicitDefs != null && desc.implicitDefs.length > 0)
			{
				for (int impDefReg : desc.implicitDefs)
				{
				    if (phyRegUsed[impDefReg] != -2)
                    {
                        spillPhyReg(mbb, i, impDefReg, false);
                        phyRegUsed[impDefReg] = 0;
                        markPhyRegRecentlyUsed(impDefReg);
                    }
                    mf.getMachineRegisterInfo().setPhysRegUsed(impDefReg);

                    int[] subRegs = regInfo.getSubRegisters(impDefReg);
                    if (subRegs != null && subRegs.length > 0)
                    {
                        int num = 0, len = subRegs.length;
                        do
                        {
                            int subReg = subRegs[num];
                            if (phyRegUsed[subReg] != -2)
                            {
                                phyRegUsed[subReg] = 0;
                                markPhyRegRecentlyUsed(subReg);
                                mf.getMachineRegisterInfo().setPhysRegUsed(subReg);
                            }
                            ++num;
                        }while (num < len);
                    }
				}
			}

			TIntArrayList deadDefs = new TIntArrayList();
			for (int j = 0, e = mi.getNumOperands(); j < e; j++)
			{
				MachineOperand op = mi.getOperand(j);
				if (op.isRegister() && op.isDead())
					deadDefs.add(op.getReg());
			}

			// loop over all defined virtual register operands,
			// assign physical register for it.
			for (int j = mi.getNumOperands() - 1; j >= 0; j--)
			{
				MachineOperand op = mi.getOperand(j);
                if ((op.isRegister() &&
                        op.getReg() != 0 &&
                        op.isDef() &&
                        !op.isEarlyClobber() &&
                        isVirtualRegister(op.getReg())))
				{
					int destVirReg = mi.getOperand(j).getReg();
					int destPhyReg = 0;

					// if this destVirReg already is held in physical reg,
					// remove it since it is defined reg.
					if (virToPhyRegMap.containsKey(destVirReg))
					{
						destPhyReg = virToPhyRegMap.get(destVirReg);
					}
					else
					    destPhyReg = getReg(mbb, i, destVirReg);
					mf.getMachineRegisterInfo().setPhysRegUsed(destPhyReg);
					markVirRegModified(destVirReg, true);
					mi.setMachineOperandReg(j, destPhyReg);
				}
			}

			// If this instruction defines any registers that are immediately dead,
			// kill them now.
			for (int j = 0, e = deadDefs.size(); j < e; j++)
			{
				int virReg = deadDefs.get(j);
				int phyReg = virReg;
				if (isVirtualRegister(virReg))
				{
					phyReg = virToPhyRegMap.get(virReg);
					Util.assertion( phyReg != 0);
					virToPhyRegMap.remove(virReg);
				}
				else if (phyRegUsed[phyReg] == -2)
				{
					// unallocatable register. Ignore it.
					continue;
				}
				if (phyReg != 0)
				{
					if (Util.DEBUG)
					{
						System.err.printf(" Register %s [%%reg%d] is never used, removing it from live set\n",
								regInfo.getName(phyReg), virReg);
					}
					removePhyReg(phyReg);
                    int[] aliasReg = regInfo.getAliasSet(phyReg);
                    if (aliasReg != null && aliasReg.length > 0)
                    {
                        for (int reg : aliasReg)
                        {
                            if (phyRegUsed[reg] != -2)
                            {
                                if (Util.DEBUG)
                                {
                                    System.err.printf(" Last use of %s[%%reg%d], removing it from live set\n",
                                            regInfo.getName(reg), virReg);
                                }
                                removePhyReg(reg);
                            }
                        }
                    }
				}
			}

            // Finally, if this is a noop copy instruction, zap it.  (Except that if
            // the copy is dead, it must be kept to avoid messing up liveness info for
            // the register scavenger.  See pr4100.)
            int[] regs = new int[4];    // srcReg, destReg, srcSubReg, destSubReg.
            if (regInfo.isMoveInstr(mi, regs) && regs[0] == regs[1] && deadDefs.isEmpty())
            {
                mbb.remove(i);
                i -= 1;
            }
		}

		// find a position of the first non-terminator instruction where
		// some instrs will were inserts after when needed.

		int itr = mbb.getFirstTerminator() - 1;

		// Spill all physical register holding virtual register.
        for (int phyReg = 0, e = regInfo.getNumRegs(); phyReg < e; phyReg++)
        {
            // If the specified physical register is allocated, just free it!
            if (phyRegUsed[phyReg] != -1 && phyRegUsed[phyReg] != -2)
            {
                spillVirReg(mbb, itr, phyRegUsed[phyReg], phyReg);
            }
            else
                removePhyReg(phyReg);
        }

        // Clear phyRegUsed, -1 indicates if it is no longer used.
        // using 0 to indicate the specified physical register is fixed allocated.
        // Arrays.fill(phyRegUsed, -1);

		Util.assertion(virToPhyRegMap.isEmpty(), "Virtual register still in phys reg?");

		// Clear any physical register which appear live at the end of the basic
		// block, but which do not hold any virtual registers.  e.g., the stack
		// pointer.
		phyRegsUseOrder.clear();
	}

    /**
     * Get the lastly machine inst references the specified reg operand.
     * @param reg
     * @return
     */
	private MachineInstr getLastUseMI(int reg)
    {
        DefUseChainIterator itr = mri.getUseIterator(reg);
        MachineOperand mo = null;
        while (itr.hasNext())
        {
            mo = itr.getOpearnd();
            itr.next();
        }
        return mo == null? null : mo.getParent();
    }

	/**
	 * This method must be overridded by concrete subclass for performing
	 * desired machine code transformation or analysis.
	 *
	 * @param mf
	 * @return
	 */
	@Override
	public boolean runOnMachineFunction(MachineFunction mf)
	{
		this.mf = mf;
		tm = mf.getTarget();
		mri = mf.getMachineRegisterInfo();
        int lastVirReg = mri.getLastVirReg();

		regInfo = tm.getRegisterInfo();
		instrInfo = tm.getInstrInfo();
        BitMap allocatable = regInfo.getAllocatableSet(mf);
        phyRegUsed = new int[allocatable.size()];
        Arrays.fill(phyRegUsed, -1);

        for (int i = 0, e = allocatable.size(); i < e; i++)
        {
            if (!allocatable.get(i))
                phyRegUsed[i] = -2;  // indicates this register can't been allocatable.
        }
        virRegModified = new boolean[lastVirReg-FirstVirtualRegister+1];
		usedInMultipleBlocks = new boolean[lastVirReg - FirstVirtualRegister + 1];

		for (MachineBasicBlock mbb : mf.getBasicBlocks())
			allocateBasicBlock(mbb);

		stackSlotForVirReg.clear();
		return true;
	}

	@Override
	public String getPassName()
	{
		return "Local register allocator";
	}

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(PhiElimination.class);
		au.addRequired(TwoAddrInstructionPass.class);
		super.getAnalysisUsage(au);
	}

	public static RegAllocLocal createLocalRegAllocator()
	{
		return new RegAllocLocal();
	}
}

