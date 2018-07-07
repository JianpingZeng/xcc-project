package backend.codegen;

import tools.Util;
import backend.pass.AnalysisUsage;
import backend.target.*;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.BitMap;

import static backend.target.TargetRegisterInfo.isVirtualRegister;

/**
 * This pass performs a pass of very simple register allocation, reloading operand
 * from stack slot into physical register and spill it into memory back.
 * It not keeps track of values in register across instructions.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RegAllocSimple extends MachineFunctionPass
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
	 * Keep track of what physical register is used.
	 */
	private tools.BitMap regUsed;

	/**
	 * Maps RegClass => which index we can take a register
	 * from. Since this is a simple register allocator, when we need a register
	 * of a certain class, we just take the next available one.
	 */
	private TObjectIntHashMap<TargetRegisterClass> regClassIdx;

	/**
	 * Statics data for performance evaluation.
	 */
	private int numSpilled;
	private int numReloaded;

	private int getStackSlotForVirReg(int virReg, TargetRegisterClass rc)
	{
		// Find the location virReg would belong.
		if (stackSlotForVirReg.containsKey(virReg))
		{
			return stackSlotForVirReg.get(virReg);
		}

		// allocate a new stack object on stack frame of curent mf.
		int frameIdx = mf.getFrameInfo().createStackObject(rc);
		stackSlotForVirReg.put(virReg, frameIdx);
		return frameIdx;
	}

	private int getFreeReg(int virReg)
	{
		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		int[] allocatableRegs = rc.getAllocatableRegs(mf);
		while(true)
		{
		    if (!regClassIdx.contains(rc))
            {
                regClassIdx.put(rc, 1);
                int physReg = allocatableRegs[0];
                if (!regUsed.get(physReg))
                {
                    mf.getMachineRegisterInfo().setPhysRegUsed(physReg);
                    return physReg;
                }
            }
            else
            {
                int regIdx = regClassIdx.get(rc);
                regClassIdx.put(rc, regIdx + 1);
                Util.assertion(regIdx < allocatableRegs.length,  "Not enough register for allocation.");
                int phyReg = rc.getRegister(regIdx);

                if (!regUsed.get(phyReg))
                {
                    mf.getMachineRegisterInfo().setPhysRegUsed(phyReg);
                    return phyReg;
                }
            }
		}
	}

	private int reloadVirReg(MachineBasicBlock mbb, int insertPos, int virReg)
	{
		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		int frameIdx = getStackSlotForVirReg(virReg, rc);
		int phyReg = getFreeReg(virReg);

		instrInfo.loadRegFromStackSlot(mbb, insertPos, phyReg, frameIdx, rc);

		// add the count for reloaded.
		++numReloaded;
		return phyReg;
	}

	private void spillVirReg(MachineBasicBlock mbb, int insertPos,
			int virReg, int phyReg)
	{
		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		int frameIdx = getStackSlotForVirReg(virReg, rc);
		boolean isKilled = !(insertPos < mbb.size() &&
				mbb.getInstAt(insertPos).readsRegister(phyReg, regInfo));
		instrInfo.storeRegToStackSlot(mbb, insertPos, phyReg, isKilled, frameIdx, rc);

		// After spill, it is needed to free used physical register
        regUsed.set(phyReg, false);
        regClassIdx.put(rc, regClassIdx.get(rc) - 1);
		// add count for spilled.
		++numSpilled;
	}

	private void allocateBasicBlock(MachineBasicBlock mbb)
	{
        TIntIntHashMap virToPhyRegMap = new TIntIntHashMap();
		for (int i = 0; i < mbb.size(); i++)
		{
			MachineInstr mi = mbb.getInstAt(i);
			regUsed.clear();
            virToPhyRegMap.clear();

			int opcode = mi.getOpcode();
			TargetInstrDesc desc = tm.getInstrInfo().get(opcode);
			if (desc.implicitUses != null && desc.implicitUses.length > 0)
				for (int useReg : desc.implicitUses)
					regUsed.set(useReg);

			if (desc.implicitDefs != null && desc.implicitDefs.length > 0)
				for (int defReg : desc.implicitDefs)
					regUsed.set(defReg);

			// loop over all operands, assign physical register for it.
			for (int j = mi.getNumOperands() - 1; j >= 0; j--)
			{
				MachineOperand op = mi.getOperand(j);
				// just handle virtual register.
				if (op.isRegister() && op.getReg() != 0 && isVirtualRegister(op.getReg()))
				{
					int virtualReg = op.getReg();

					// make sure the same virtual register maps to the same physical
					// register in any given instruction
                    int phyReg;
                    if (!virToPhyRegMap.containsKey(virtualReg))
					{
						if (op.isDef())
						{
							if (instrInfo.get(opcode).isConvertibleTo3Addr() && j == 0)
							{
								// This maps a = b + c into b+= c, and save b into a.
								Util.assertion(mi.getOperand(1).isRegister()										&& mi.getOperand(1).getReg()!=0
										&& mi.getOperand(1).isUse(), "Two address instruction invalid!");


								phyReg = mi.getOperand(1).getReg();
							}
							else
							{
								phyReg = getFreeReg(virtualReg);
							}
							++i;
							spillVirReg(mbb, i, virtualReg, phyReg);
							--i;
						}
						else
						{
							phyReg = reloadVirReg(mbb, i,virtualReg);
							virToPhyRegMap.put(virtualReg, phyReg);
						}
					}
                    phyReg = virToPhyRegMap.get(virtualReg);
					op.setReg(phyReg);
				}
			}
		}
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

		stackSlotForVirReg = new TIntIntHashMap();
		regUsed = new BitMap();
		regClassIdx = new TObjectIntHashMap<>();

		for (MachineBasicBlock mbb : mf.getBasicBlocks())
			allocateBasicBlock(mbb);

		stackSlotForVirReg.clear();
		return true;
	}

	@Override
	public String getPassName()
	{
		return "Simple Register Allocator";
	}

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(PhiElimination.class);
		au.addRequired(TwoAddrInstructionPass.class);
		super.getAnalysisUsage(au);
	}

	public static RegAllocSimple createSimpleRegAllocator()
	{
		return new RegAllocSimple();
	}
}
