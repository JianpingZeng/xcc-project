package backend.transform.scalars;

import backend.analysis.LiveVariable;
import backend.codegen.*;
import backend.pass.AnalysisUsage;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import backend.target.TargetRegisterClass;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class PhiElimination extends MachineFunctionPass
{
	private TargetInstrInfo instInfo;
	private MachineFunction mf;
	private MachineRegisterInfo mri;
	private TargetRegisterInfo regInfo;
	/**
	 * This method used for performing elimination operation on each PHI node.
	 * @param mf
	 * @return true if the internal structure of machine function has been
	 * changed.
	 */
	@Override
	public boolean runOnMachineFunction(MachineFunction mf)
	{
		boolean changed = false;
		instInfo = mf.getTarget().getInstrInfo();
		this.mf = mf;
		mri = mf.getMachineRegisterInfo();
		regInfo = mf.getTarget().getRegisterInfo();

		for (MachineBasicBlock mbb : mf.getBasicBlocks())
		{
			changed |= eliminatePHINodes(mbb);
		}
		return changed;
	}

	/**
	 * uses the registe to register copy instruction to replace the
	 * PHI instruction.
	 * @param mbb
	 * @return
	 */
	private boolean eliminatePHINodes(MachineBasicBlock mbb)
	{
		if (mbb.isEmpty() || instInfo.isDummyPhiInstr(mbb.getInstAt(0).getOpcode()))
			return false;

		// a arrays whose each element represents the uses count of the specified
		// virtual register.
		int[] vregPHIUsesCount = new int[mri.getLastVirReg()+1];

		// count the use for all of virtual register.
		for (MachineBasicBlock pred : mbb.getPredecessors())
		{
			for (MachineBasicBlock succ : pred.getSuccessors())
			{
				for (int i = 0, sz = succ.size(); i < sz; i++)
				{
					MachineInstr mi = succ.getInstAt(i);
					if (!instInfo.isDummyPhiInstr(mi.getOpcode()))
						break;
					for (int j = 1, e = mi.getNumOperands(); j<e; j += 2)
					{
						vregPHIUsesCount[mi.getOperand(j).getReg()]++;
					}
				}
			}
		}

		// find the first non-phi instruction.
		int firstInstAfterPhi = 0;
		for (; firstInstAfterPhi < mbb.size() && instInfo.isDummyPhiInstr(mbb.
				getInstAt(firstInstAfterPhi).getOpcode()); firstInstAfterPhi++);

		for (; instInfo.isDummyPhiInstr(mbb.getInstAt(0).getOpcode()); )
			lowerPhiNode(mbb, firstInstAfterPhi, vregPHIUsesCount);
		return true;
	}

	/**
	 * Loop over all operands of each PHI node, to replace it with register to
	 * register copy instruction.
	 * @param mbb
	 * @param firstInstAfterPhi
	 * @param vregPHIUsesCount
	 * @return
	 */
	private boolean lowerPhiNode(MachineBasicBlock mbb,
			int firstInstAfterPhi, int[] vregPHIUsesCount)
	{
		MachineInstr phiMI = mbb.getInsts().removeFirst();
		int destReg = phiMI.getOperand(0).getReg();

		TargetRegisterClass rc = mri.getRegClass(destReg);

		// update the def of this incomingReg will be performed at predecessor.
		int incomingReg = mri.createVirtualRegister(rc);

		// creates a register to register copy instruction at the position where
		// indexed by firstInstAfter.
		regInfo.copyRegToReg(mbb, firstInstAfterPhi, destReg, incomingReg, rc);
		LiveVariable la = getAnalysisToUpDate(LiveVariable.class);
		if (la != null)
		{
			MachineInstr copyInst = mbb.getInstAt(firstInstAfterPhi);

			la.addVirtualRegisterKilled(incomingReg, copyInst);

			la.removeVirtualRegisterKilled(phiMI);

			// if the result is dead, update live analysis.
			if (la.registerDefIsDeaded(phiMI, destReg))
			{
				la.addVirtualRegisterDead(destReg, copyInst);
				la.removeVirtualRegisterDead(phiMI);
			}

			// records the defined MO for destReg.
			mri.setDefMO(destReg, copyInst.getOperand(0));
		}

		HashSet<MachineBasicBlock> mbbInsertedInto = new HashSet<>();
		for (int i = phiMI.getNumOperands() - 1; i > 1; i-=2)
		{
			int srcReg = phiMI.getOperand(i-1).getReg();
			assert mri.isVirtualReg(srcReg):
					"Machine PHI Operands must all be virtual registers!";

			MachineBasicBlock opBB = phiMI.getOperand(i).getMBB();

			// avoids duplicate copy insertion.
			if (!mbbInsertedInto.add(opBB))
				continue;

			// Get an iterator pointing to the first terminator in the block (or end()).
			// This is the point where we can insert a copy if we'd like to.
			int idx = opBB.getFirstTerminator();

			regInfo.copyRegToReg(opBB, idx, incomingReg, srcReg, rc);

			// idx++;
			idx++; // make sure the idx always points to the first terminator inst.
			if (la == null) continue;

			LiveVariable.VarInfo srcRegVarInfo = la.getVarInfo(srcReg);

			boolean valueIsLive = vregPHIUsesCount[srcReg] != 0;

			// records the successor blocks which is not contained in aliveBlocks
			// set.
			ArrayList<MachineBasicBlock> opSuccBlocks = new ArrayList<>();

			for (MachineBasicBlock succ : opBB.getSuccessors())
			{
				int succNo = succ.getNumber();
				if (succNo < srcRegVarInfo.aliveBlocks.size()
						&& srcRegVarInfo.aliveBlocks.contains(succNo))
				{
					valueIsLive = true;
					break;
				}

				opSuccBlocks.add(succ);
			}

			// Check to see if this value is live because there is a use in a successor
			// that kills it.
			if (!valueIsLive)
			{
				switch (opSuccBlocks.size())
				{
					case 1:
					{
						MachineBasicBlock succ = opSuccBlocks.get(0);
						for (int j = 0, e = srcRegVarInfo.kills.size(); j<e; j++)
						{
							if (srcRegVarInfo.kills.get(j).getParent() == succ)
							{
								valueIsLive = true;
								break;
							}
						}
						break;
					}
					case 2:
					{
						MachineBasicBlock succ1 = opSuccBlocks.get(0);
						MachineBasicBlock succ2 = opSuccBlocks.get(1);
						for (int j = 0, e = srcRegVarInfo.kills.size(); j<e; j++)
						{
							MachineBasicBlock parent = srcRegVarInfo.kills.get(j).getParent();
							if (parent == succ1 || parent == succ2)
							{
								valueIsLive = true;
								break;
							}
						}
						break;
					}
					default:
					{
						for (int j = 0, e = srcRegVarInfo.kills.size(); j < e; j++)
						{
							MachineBasicBlock parent = srcRegVarInfo.kills.get(j).getParent();
							if (opSuccBlocks.contains(parent))
							{
								valueIsLive = true;
								break;
							}
						}
						break;
					}
				}
			}

			if (!valueIsLive)
			{
				boolean firstTerminatorUsesValue = false;
				if (idx != opBB.size())
				{
					firstTerminatorUsesValue = instructionUsesRegister
							(opBB.getInstAt(idx), srcReg);
				}

				int killInst = !firstTerminatorUsesValue ? idx-1 : idx;

				la.addVirtualRegisterKilled(srcReg, opBB.getInstAt(killInst));

				int opBlockNum = opBB.getNumber();
				if (opBlockNum < srcRegVarInfo.aliveBlocks.size())
					srcRegVarInfo.aliveBlocks.add(opBlockNum);
			}
		}
		return true;
	}

	/**
	 * Return true if the specified machine instr has a
	 * use of the specified register.
	 * @param mi
	 * @param reg
	 * @return
	 */
	private boolean instructionUsesRegister(MachineInstr mi, int reg)
	{
		for (int i = 0, sz = mi.getNumOperands(); i < sz; i++)
		{
			MachineOperand mo = mi.getOperand(i);
			if (mo.isRegister() && mo.getReg() != 0
					&& mo.getReg() == reg && mo.isUse())
				return true;
		}
		return false;
	}

	@Override
	public String getPassName()
	{
		return "PHI Nodes elimination pass";
	}
	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addPreserved(LiveVariable.class);
		super.getAnalysisUsage(au);
	}
}
