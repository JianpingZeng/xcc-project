package backend.codegen;

import backend.codegen.MachineOperand.RegState;
import backend.target.TargetInstrDesc;
import backend.value.ConstantFP;
import backend.value.GlobalValue;

/**
 * This is a convenient helper class for creating a machine instruction on
 * specified target machine, e.g.X86.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineInstrBuilder
{
	private MachineInstr mi;

	public MachineInstrBuilder(MachineInstr instr)
	{
		mi = instr;
	}

	public MachineInstr getMInstr()
	{
		return mi;
	}

	public MachineInstrBuilder addReg(int regNo)
	{
		return addReg(regNo, 0, 0);
	}

	public MachineInstrBuilder addReg(int regNo, int flags)
	{
		return addReg(regNo, flags, 0);
	}

	public MachineInstrBuilder addReg(int regNo, int flags, int subReg)
	{
		assert (flags & 0x1) == 0:
				"Passing in 'true' to addReg is forbidden! Use enums instead.";
		mi.addOperand(MachineOperand.createReg(regNo,
				(flags & RegState.Define) != 0,
				(flags & RegState.Implicit) != 0,
				(flags & RegState.Kill) != 0,
				(flags & RegState.Dead) != 0,
				(flags & RegState.Undef) != 0,
				(flags & RegState.EarlyClobber) != 0,
				subReg));

		return this;
	}

	public MachineInstrBuilder addImm(long val)
	{
		mi.addOperand(MachineOperand.createImm(val));
		return this;
	}

	public MachineInstrBuilder addFPImm(ConstantFP val)
	{
		mi.addOperand(MachineOperand.createFPImm(val));
		return this;
	}

	public MachineInstrBuilder addMBB(MachineBasicBlock mbb)
	{
		return addMBB(mbb, 0);
	}

	public MachineInstrBuilder addMBB(MachineBasicBlock mbb, int targetFlags)
	{
		mi.addOperand(MachineOperand.createMBB(mbb, targetFlags));
		return this;
	}

	public MachineInstrBuilder addFrameIndex(int idx)
	{
		mi.addOperand(MachineOperand.createFrameIndex(idx));
		return this;
	}

	public MachineInstrBuilder addConstantPoolIndex(int idx,
			int offset,
			int targetFlags)
	{
		mi.addOperand(MachineOperand.createConstantPoolIndex(idx, offset, targetFlags));
		return this;
	}

	public MachineInstrBuilder addJumpTableIndex(int idx,
			int targetFlags)
	{
		mi.addOperand(MachineOperand.createJumpTableIndex(idx, targetFlags));
		return this;
	}

	public MachineInstrBuilder addGlobalAddress(GlobalValue gv,
			long offset,
			int targetFlags)
	{
		mi.addOperand(MachineOperand.createGlobalAddress(gv, offset, targetFlags));
		return this;
	}

	public MachineInstrBuilder addExternalSymbol(String symName,
			long offset,
			int targetFlags)
	{
		mi.addOperand(MachineOperand.createExternalSymbol(symName, offset, targetFlags));
		return this;
	}

	public MachineInstrBuilder addOperand(MachineOperand mo)
	{
		mi.addOperand(mo);
		return this;
	}

	public static MachineInstrBuilder buildMI(MachineBasicBlock mbb,
			int insertPos,
			TargetInstrDesc desc)
	{
		MachineInstr mi = new MachineInstr(desc);
		mbb.insert(insertPos, mi);
		return new MachineInstrBuilder(mi);
	}

	public static MachineInstrBuilder buildMI(MachineBasicBlock mbb,
			TargetInstrDesc tid,
			int destReg)
	{
		MachineInstr mi = new MachineInstr(mbb, tid);
		mbb.addLast(mi);
		return new MachineInstrBuilder(mi).addReg(destReg, RegState.Define);
	}

	public static MachineInstrBuilder buildMI(MachineBasicBlock mbb,
			int insertPos,
			TargetInstrDesc tid,
			int destReg)
	{
		MachineInstr mi = new MachineInstr(mbb, tid);
		mbb.insert(insertPos, mi);
		return new MachineInstrBuilder(mi).addReg(destReg, RegState.Define);
	}

	public static MachineInstrBuilder buildMI(TargetInstrDesc desc)
	{
		return new MachineInstrBuilder(new MachineInstr(desc));
	}

	public static MachineInstrBuilder buildMI(TargetInstrDesc desc, int destReg)
	{
		return new MachineInstrBuilder(new MachineInstr(desc)).
				addReg(destReg, RegState.Define);
	}

	public static MachineInstrBuilder buildMI(MachineBasicBlock mbb, TargetInstrDesc desc)
	{
		return new MachineInstrBuilder(new MachineInstr(mbb, desc));
	}

	public MachineInstrBuilder addMemOperand(MachineMemOperand mmo)
	{
		mi.addMemOperand(mmo);
		return this;
	}
}
