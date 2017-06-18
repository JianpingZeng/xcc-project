package backend.codegen;

import backend.transform.scalars.PNE;
import backend.transform.scalars.TwoAddrInstruction;
import backend.pass.AnalysisUsage;
import backend.target.TargetInstrInfo;
import backend.target.TargetInstrInfo.TargetInstrDescriptor;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.target.TargetRegisterClass;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.BitMap;

import java.util.HashMap;

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
		int size = rc.getRegSize();
		while(true)
		{
			int regIdx = regClassIdx.get(rc);
			regClassIdx.put(rc, regIdx+1);
			assert  regIdx != size:"Not enough register for allocation.";
			int phyReg = rc.getRegister(regIdx);

			if (!regUsed.get(phyReg))
				return phyReg;
		}
	}

	private int reloadVirReg(MachineBasicBlock mbb, int insertPos, int virReg)
	{
		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		int frameIdx = getStackSlotForVirReg(virReg, rc);
		int phyReg = getFreeReg(virReg);

		regInfo.loadRegFromStackSlot(mbb, insertPos, phyReg, frameIdx, rc);

		// add the count for reloaded.
		++numReloaded;
		return phyReg;
	}

	private void spillVirReg(MachineBasicBlock mbb, int insertPos,
			int virReg, int phyReg)
	{
		TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virReg);
		int frameIdx = getStackSlotForVirReg(virReg, rc);
		regInfo.storeRegToStackSlot(mbb, insertPos, phyReg, frameIdx, rc);

		// add count for spilled.
		++numSpilled;
	}

	private void allocateBasicBlock(MachineBasicBlock mbb)
	{
		for (int i = 0, size = mbb.size(); i < size; i++)
		{
			MachineInstr mi = mbb.getInstAt(i);
			regUsed.clear();

			HashMap<Integer, Integer> virToPhyRegMap = new HashMap<>();

			int opcode = mi.getOpCode();
			TargetInstrDescriptor desc = tm.getInstrInfo().get(opcode);
			for (int useReg : desc.implicitUses)
				regUsed.set(useReg);

			for (int defReg : desc.implicitDefs)
				regUsed.set(defReg);

			// loop over all operands, assign physical register for it.
			for (int j = mi.getNumOperands() - 1; j >= 0; j--)
			{
				MachineOperand op = mi.getOperand(i);
				// just handle virtual register.
				if (op.isVirtualRegister())
				{
					int virtualReg = op.getReg();

					// make sure the same virtual register maps to the same physical
					// register in any given instruction
					int phyReg = virToPhyRegMap.get(virtualReg);
					if (phyReg == 0)
					{
						if (op.opIsDef())
						{
							if (instrInfo.isTwoAddrInstr(opcode) && i == 0)
							{
								// This maps a = b + c into b+= c, and save b into a.
								assert mi.getOperand(1).isRegister()
										&& mi.getOperand(1).getReg()!=0
										&& mi.getOperand(1).opIsUse()
										:"Two address instruction invalid!";

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
					mi.setMachineOperandReg(i, phyReg);
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
		tm = mf.getTargetMachine();
		regInfo = tm.getRegInfo();
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
		au.addRequired(PNE.class);
		au.addRequired(TwoAddrInstruction.class);
		super.getAnalysisUsage(au);
	}

	public static RegAllocSimple createSimpleRegAllocator()
	{
		return new RegAllocSimple();
	}
}
