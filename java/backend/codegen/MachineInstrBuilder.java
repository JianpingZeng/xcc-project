package backend.codegen;

import backend.codegen.MachineOperand.RegState;
import backend.target.TargetInstrDesc;
import backend.value.ConstantFP;
import backend.value.GlobalValue;

import static backend.codegen.MachineMemOperand.MOLoad;
import static backend.codegen.MachineMemOperand.MOStore;

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

	public static MachineInstrBuilder addFrameReference(MachineInstrBuilder mib,
			int fi)
    {
        return addFrameReference(mib, fi, 0);
    }

	/**
     * This function is used to add a reference to the base of
     * an abstract object on the stack frame of the current function.  This
     * reference has base register as the FrameIndex offset until it is resolved.
     * This allows a constant offset to be specified as well...
     * @param mib
     * @param fi
     * @param offset
     * @return
     */
    public static MachineInstrBuilder addFrameReference(MachineInstrBuilder mib,
            int fi,
            int offset)
    {
        MachineInstr mi = mib.getMInstr();
        MachineFunction mf = mi.getParent().getParent();
        MachineFrameInfo mfi = mf.getFrameInfo();
        TargetInstrDesc tii = mi.getDesc();
        int flags = 0;
        if (tii.mayLoad())
        {
            flags |= MOLoad;
        }
        if (tii.mayStore())
            flags |= MOStore;
        MachineMemOperand mmo = new MachineMemOperand(
                null,
                flags,
                mfi.getObjectOffset(fi) + offset,
                mfi.getObjectOffset(fi),
                mfi.getObjectAlignment(fi));
        return addOffset(mib.addFrameIndex(fi), offset).addMemOperand(mmo);
    }

	private static MachineInstrBuilder addLeaOffset(MachineInstrBuilder mib, long offset)
    {
        return mib.addImm(1).addReg(0).addImm(offset);
    }

    public static MachineInstrBuilder addOffset(MachineInstrBuilder mib,
            long offset)
    {
        return addLeaOffset(mib, offset).addReg(0);
    }

    public static MachineInstrBuilder addRegOffset(
            MachineInstrBuilder mib,
            int reg, boolean isKill, long offset)
    {
        return addOffset(mib.addReg(reg, getKillRegState(isKill)), offset);
    }

    public static MachineInstrBuilder addLeaRegOffset(
            MachineInstrBuilder mib,
            int reg, boolean isKill, long offset)
    {
        return addLeaOffset(mib.addReg(reg, getKillRegState(isKill)), offset);
    }

    public static MachineInstrBuilder addRegReg(
            MachineInstrBuilder mib,
            int reg1,
            boolean isKill1,
            int reg2,
            boolean isKill2)
    {
        return mib.addReg(reg1, getKillRegState(isKill1))
                .addImm(1)
                .addReg(reg2, getKillRegState(isKill2))
                .addImm(0);
    }

    /**
     * This function is used to add a reference to the
     * base of a constant value spilled to the per-function constant pool.  The
     * reference uses the abstract ConstantPoolIndex which is retained until
     * either machine code emission or assembly output. In PIC mode on x86-32,
     * the GlobalBaseReg parameter can be used to make this a
     * GlobalBaseReg-relative reference.
     * @param mib
     * @param cpi
     * @param globalBaseReg
     * @param opFlags
     * @return
     */
    public static MachineInstrBuilder addConstantPoolReference(
            MachineInstrBuilder mib, int cpi, int globalBaseReg, int opFlags)
    {
        return mib.addReg(globalBaseReg).addImm(1).addReg(0).
                addConstantPoolIndex(cpi, 0, opFlags).
                addReg(0);
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

	public static MachineInstrBuilder buildMI(
	        MachineBasicBlock mbb,
			int insertPos,
			TargetInstrDesc tid)
	{
		MachineInstr mi = new MachineInstr(tid);
		mbb.insert(insertPos, mi);
		return new MachineInstrBuilder(mi);
	}

    public static MachineInstrBuilder buildMI(
            MachineBasicBlock mbb,
            int insertPos,
            TargetInstrDesc tid,
            int destReg)
    {
        MachineInstr mi = new MachineInstr(tid);
        mbb.insert(insertPos, mi);
        return new MachineInstrBuilder(mi).addReg(destReg, RegState.Define);
    }

    public static MachineInstrBuilder buildMI(
	        MachineBasicBlock mbb,
			TargetInstrDesc tid,
			int destReg)
	{
		MachineInstr mi = new MachineInstr(tid);
        mbb.addLast(mi);
		return new MachineInstrBuilder(mi).addReg(destReg, RegState.Define);
	}

    public static MachineInstrBuilder buildMI(
            MachineBasicBlock mbb,
            TargetInstrDesc tid)
    {
        MachineInstr mi = new MachineInstr(tid);
        mbb.addLast(mi);
        return new MachineInstrBuilder(mi);
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

	public MachineInstrBuilder addMemOperand(MachineMemOperand mmo)
	{
		mi.addMemOperand(mmo);
		return this;
	}

	public static int getDefRegState(boolean b)
	{
		return b ? RegState.Define : 0;
	}

	public static int getImplRegState(boolean b)
	{
		return b ? RegState.Implicit : 0;
	}

	public static int getKillRegState(boolean b)
	{
		return b ? RegState.Kill : 0;
	}

	public static int getDeadRegState(boolean b)
    {
        return b ? RegState.Dead : 0;
    }

	public static int getUndefRegState(boolean b)
	{
		return b ? RegState.Undef : 0;
	}
}
