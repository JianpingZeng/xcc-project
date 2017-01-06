package backend.transform;

import backend.analysis.LiveVariable;
import backend.analysis.LiveVariable.VarInfo;
import backend.codegen.*;
import backend.pass.AnalysisUsage;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.target.TargetRegisterInfo.TargetRegisterClass;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TwoAddrInstruction extends MachineFunctionPass
{
	/**
	 * This method must be overridden by concrete subclass for performing
	 * desired machine code transformation or analysis.
	 *
	 * @param mf
	 * @return
	 */
	@Override
	public boolean runOnMachineFunction(MachineFunction mf)
	{
		boolean changed = false;
		TargetMachine tm = mf.getTargetMachine();
		TargetRegisterInfo regInfo = tm.getRegInfo();
		TargetInstrInfo instInfo = tm.getInstrInfo();
		LiveVariable la = getAnalysisToUpDate(LiveVariable.class);
		MachineRegisterInfo mri = mf.getMachineRegisterInfo();

		for (MachineBasicBlock mbb : mf.getBasicBlocks())
		{
			for (int i = 0; i < mbb.size(); i++)
			{
				MachineInstr mi = mbb.getInstAt(i);
				if (!instInfo.isTwoAddrInstr(mi.getOpCode()))
					continue;

				int regA = mi.getOperand(0).getReg();
				assert mi.getOperand(0).isVirtualRegister() && regA != 0;
				int regB = mi.getOperand(1).getReg();
				assert mi.getOperand(1).isVirtualRegister() && regB != 0
						&& mi.getOperand(1).opIsUse();

				// if this already is a two address instruction, skip it.
				if (regA != regB)
				{
					/* TODO handle commutable operator.
					if (!la.killRegister(mi, regB))
					{
						TargetInstrDescriptor desc = instInfo.get(mi.getOpCode());
						if (desc.tSFlags & X86InstrInfo.M_COMMUTABLE)
						assert mi.getOperand(2).isRegister()
								&& mi.getOperand(2).getReg() != 0:
								"Two addr instruction must be a register.";
						if (instInfo.)
					}*/

					TargetRegisterClass rc = mri.getRegClass(regA);
					regInfo.copyRegToReg(mbb, i, regA, regB, rc);

					MachineInstr copyMI = mbb.getInstAt(i);

					// update live variable for regA.
					VarInfo varInfoRegA = la.getVarInfo(regA);
					if (la.removeVirtualRegisterKilled(regB, mi))
						la.addVirtualRegisterKilled(regB, copyMI);

					if (la.removeVirtualRegisterDead(regB, mi))
						la.addVirtualRegisterDead(regB, copyMI);

					// replace all occurrences of regB with regA.
					for (int j = 1; j < mi.getNumOperands(); j++)
					{
						if (mi.getOperand(j).isRegister() &&
								mi.getOperand(j).getReg() == regB)
							mi.setMachineOperandReg(j, regA);
					}
				}
				assert mi.getOperand(0).opIsDef();
				mi.getOperand(0).setUse();
				mi.removeOperand(1);
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public String getPassName()
	{
		return "Two addr instruction pass";
	}

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(LiveVariable.class);
		au.addPreserved(LiveVariable.class);
		au.addRequired(PNE.class);
		super.getAnalysisUsage(au);
	}
}
