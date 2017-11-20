package backend.codegen;

import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.target.*;
import gnu.trove.map.hash.TIntIntHashMap;
import tools.BitMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

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
	HashMap<Integer, Integer> virToPhyRegMap;

	/**
	 * Mapping physical register to virtual register assigned to it..
	 */
	HashMap<Integer, Integer> phyRegUsed;

	/**
	 * Statics data for performance evaluation.
	 */
	public static final IntStatistic NumSpilled =
            new IntStatistic("NumSpilled", "Number of spilled code");
	public static final IntStatistic NumReloaded =
            new IntStatistic("NumReloaded", "Number of reloaded code");

    private BitMap virRegModified;

	public RegAllocLocal()
	{
		stackSlotForVirReg = new TIntIntHashMap();
		virToPhyRegMap = new HashMap<>();
		phyRegUsed = new HashMap<>();
        virRegModified = new BitMap();
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
		assert !phyRegUsed.containsKey(phyReg):"phyreg is already assigned!";

		phyRegUsed.put(phyReg, virReg);
		virToPhyRegMap.put(virReg, phyReg);
		phyRegsUseOrder.addLast(phyReg);
	}

	private int getReg(MachineBasicBlock mbb, int insertPos, int virReg)
	{
		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		// first check to see if we have a free register.
		int phyReg = getFreeReg(rc);

		if (phyReg == 0)
		{
			assert !phyRegsUseOrder.isEmpty():"No allocatable registers";


			// Spill a the least used physical register into memory.
			for (int i = 0; phyReg == 0; i++)
			{
				assert i != phyRegsUseOrder.size()
						: "Cann't find a register of the appropriate class";

				int r = phyRegsUseOrder.get(i);

				assert phyRegUsed.containsKey(r)
						: "PhyReg in phyRegsUseOrder, but is not allocated";
				if (phyRegUsed.get(r) != 0)
				{
					if (regInfo.getRegClass(r) == rc)
					{
						phyReg = r;
						break;
					}
					else
					{
						boolean subRegFound = false;
						for (int subReg : regInfo.get(r).subRegs)
                        {
                            if (regInfo.getRegClass(subReg) == rc)
                            {
                                phyReg = subReg;
                                subRegFound = true;
                                break;
                            }
                        }
						if (!subRegFound)
						{
							for (int superReg : regInfo.get(r).superRegs)
								if (regInfo.getRegClass(superReg) == rc)
								{
									phyReg = superReg;
									break;
								}
						}
					}
				}
			}

			assert phyReg != 0:"Physical register not be assigned";

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
		if (phyRegUsed.containsKey(phyReg)) return false;

		for (int subReg : regInfo.get(phyReg).subRegs)
			if (phyRegUsed.containsKey(subReg))
				return false;

		for (int superReg : regInfo.get(phyReg).superRegs)
			if (phyRegUsed.containsKey(superReg))
				return false;

		return true;
	}

	private int getFreeReg(TargetRegisterClass rc)
	{
		for (int phyReg : rc.getAllocableRegs(mf))
		{
			if (isPhyRegAvailable(phyReg))
			{
				assert phyReg!=0:"Can not use register!";
				return phyReg;
			}
		}
		return 0;
	}

	private void markVirRegModified(int virReg, boolean isModified)
	{
		assert virReg >= FirstVirtualRegister;
		virReg -= FirstVirtualRegister;
		virRegModified.set(virReg, isModified);
	}

	private int reloadVirReg(MachineBasicBlock mbb, int insertPos, int virReg)
	{
		// the virtual register already resides in a physical reg.
		if (virToPhyRegMap.containsKey(virReg))
		{
			int phyReg = virToPhyRegMap.get(virReg);
			markPhyRegRecentlyUsed(phyReg);
			return phyReg;
		}

		int phyReg = getReg(mbb, insertPos, virReg);

		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		int frameIdx = getStackSlotForVirReg(virReg, rc);

		// note that this reg is just reloaded.
		markVirRegModified(virReg, false);

		regInfo.loadRegFromStackSlot(mbb, insertPos, phyReg, frameIdx, rc);

		// add the count for reloaded.
		NumReloaded.inc();
		return phyReg;
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
		if (phyRegUsed.containsKey(phyReg))
		{
			if(phyRegUsed.get(phyReg) != 0 || !onlyVirReg)
				spillVirReg(mbb, insertPos, phyRegUsed.get(phyReg), phyReg);
		}
		else
		{
		    int[] alias = regInfo.getAliasSet(phyReg);
		    if (alias != null && alias.length > 0)
            {
                for (int aliasReg : alias)
                {
                    if (phyRegUsed.containsKey(aliasReg) && phyRegUsed.get(aliasReg) != 0)
                        spillVirReg(mbb, insertPos, phyRegUsed.get(aliasReg), aliasReg);

                }
            }
		}
	}

	private boolean isVirRegModified(int virReg)
	{
		assert virReg >= FirstVirtualRegister : "Illegal virReg!";
		assert  virReg - FirstVirtualRegister < virRegModified.size()
				: "Illegal virReg!";
		return virRegModified.get(virReg - FirstVirtualRegister);
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
			regInfo.storeRegToStackSlot(mbb, insertPos, phyReg, frameIdx, rc);

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
		assert !phyRegsUseOrder.isEmpty():"No register used!";

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
		phyRegUsed.remove(phyReg);

		assert phyRegsUseOrder.contains(phyReg)
				: "Remove a phyReg, but it is not in phyRegUseOrder list";
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

	private void allocateBasicBlock(MachineBasicBlock mbb)
	{
		for (int i = 0, size = mbb.size(); i < size; i++)
		{
			MachineInstr mi = mbb.getInstAt(i);

			int opcode = mi.getOpcode();
			TargetInstrDesc desc = tm.getInstrInfo().get(opcode);

			// loop over all implicit used register, to mark it as recently used,
			// so they don't get reallocated.
            if (desc.implicitUses != null && desc.implicitUses.length > 0)
			    for (int useReg : desc.implicitUses)
				    markPhyRegRecentlyUsed(useReg);

			// loop over all operands, assign physical register for it.
			for (int j = 0, e = mi.getNumOperands(); j < e; j++)
			{
				if (mi.getOperand(j).isRegister()
						&& mi.getOperand(j).getReg() != 0
                        && mi.getOperand(j).isUse()
						&& isVirtualRegister(mi.getOperand(j).getReg()))
				{
					int virtReg = mi.getOperand(j).getReg();
					int phyReg = reloadVirReg(mbb, i, virtReg);
					mi.setMachineOperandReg(j, phyReg);
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
                        && isPhysicalRegister(op.getReg())))
				{
					int reg = op.getReg();
					spillPhyReg(mbb, i, reg, true);
					phyRegUsed.put(reg, 0);
					phyRegsUseOrder.addLast(reg);
				}
			}

			// loop over all implicit defs, spilling them as well.
			if (desc.implicitDefs != null && desc.implicitDefs.length > 0)
			{
				for (int impDefReg : desc.implicitDefs)
				{
					spillPhyReg(mbb, i, impDefReg, false);
					phyRegUsed.put(impDefReg, 0);
					phyRegsUseOrder.addLast(impDefReg);
				}
			}

			// loop over all defined virtual register operands,
			// assign physical register for it.
			for (int j = mi.getNumOperands() - 1; j >= 0; j--)
			{
				MachineOperand op = mi.getOperand(j);
                if ((op.isRegister() &&
                        op.getReg() != 0 &&
                        op.isDef() &&
                        isVirtualRegister(op.getReg())))
				{
					int destVirReg = mi.getOperand(j).getReg();
					int destPhyReg = 0;

					// if this destVirReg already is held in physical reg,
					// remove it since it is defined reg.
					if (virToPhyRegMap.containsKey(destVirReg))
					{
						int phyReg = virToPhyRegMap.get(destVirReg);
						virToPhyRegMap.remove(destVirReg);
						removePhyReg(phyReg);
					}
                    TargetInstrDesc tid = instrInfo.get(opcode);
					if (tid.isConvertibleTo3Addr() && j == 0)
					{
						// a = b + c --> b(a) += c;
						assert  mi.getOperand(1).isRegister()
								&& mi.getOperand(1).getReg() != 0
								&& mi.getOperand(1).isUse()
								:"Two address instruction invalid!";

						destPhyReg = mi.getOperand(1).getReg();

						spillPhyReg(mbb, i, destPhyReg, false);
						assignVirToPhyReg(destVirReg, destPhyReg);
					}
					else
					{
						destPhyReg = getReg(mbb, i, destVirReg);
					}
					markVirRegModified(destVirReg, true);
					mi.setMachineOperandReg(j, destPhyReg);
				}
			}
		}

		// find a position of the first non-terminator instruction where
		// some instrs will were inserts after when needed.
		int itr = mbb.size();
		while(itr!=0 && instrInfo.get(mbb.getInstAt(itr-1).getOpcode()).isTerminator())
			--itr;

		// Spill all physical register holding virtual register.
		if (!phyRegUsed.isEmpty())
		{
            Iterator<Integer> mapItr = phyRegUsed.keySet().iterator();
            while (mapItr.hasNext())
            {
                int phyReg = mapItr.next();
                int virReg = phyRegUsed.get(phyReg);
                if (virReg != 0)
                    spillVirReg(mbb, itr, virReg, phyReg);
                else
                    removePhyReg(phyReg);
            }
		}

		phyRegUsed.clear();
		assert virToPhyRegMap.isEmpty():"Virtual register still in phys reg?";

		// Clear any physical register which appear live at the end of the basic
		// block, but which do not hold any virtual registers.  e.g., the stack
		// pointer.
		phyRegsUseOrder.clear();
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
		regInfo = tm.getRegisterInfo();
		instrInfo = tm.getInstrInfo();
		for (MachineBasicBlock mbb : mf.getBasicBlocks())
			allocateBasicBlock(mbb);

		stackSlotForVirReg.clear();
		virRegModified.clear();
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

