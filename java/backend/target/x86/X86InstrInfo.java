package backend.target.x86;

import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.codegen.MachineInstrBuilder;
import backend.codegen.MachineOperand;
import backend.codegen.MachineOperand.RegState;
import backend.target.TID;
import backend.target.TargetInstrDesc;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86InstrInfo extends TargetInstrInfo
{

	private X86RegisterInfo registerInfo;
	public X86InstrInfo()
	{
		super(x86Insts);
		registerInfo = new X86RegisterInfo();
	}

	/**
	 * TargetInstrInfo is a superset of MRegister info.  As such, whenever
	 * a client has an instance of instruction info, it should always be able
	 * to get register info as well (through this method).
	 */
	public TargetRegisterInfo getRegisterInfo() { return registerInfo; }

	/**
	 * Returns the target's implementation of NOP, which is
	 * usually a pseudo-instruction, implemented by a degenerate version of
	 * another instruction, e.g. X86: xchg ax, ax;
	 *
	 * @return
	 */
	@Override
	public MachineInstr createNOPinstr()
	{
		return MachineInstrBuilder.buildMI(X86InstrNames.XCHGrr16, 2).
				addReg(X86RegNames.AX, RegState.UseAndDef).
				addReg( X86RegNames.AX, RegState.UseAndDef)
				.getMInstr();
	}

	/**
	 * Not having a special NOP opcode, we need to know if a given
	 * instruction is interpreted as an `official' NOP instr, i.e., there may be
	 * more than one way to `do nothing' but only one canonical way to slack off.
	 *
	 * @param mi
	 * @return
	 */
	@Override
	public boolean isNOPinstr(MachineInstr mi)
	{
		if (mi.getOpCode() == XCHGrr16)
		{
			MachineOperand op1 = mi.getOperand(0);
			MachineOperand op2 = mi.getOperand(1);
			if (op1.isMachineRegister() && op1.getMachineRegNum() == AX
					&& op2.isMachineRegister() && op2.getMachineRegNum() == AX)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * This function returns the "base" X86 opcode for the
	 * specified opcode number.
	 * @param opcode
	 * @return
	 */
	public int getBaseOpcodeFor(int opcode)
	{
		return get(opcode).tSFlags >> X86II.OpcodeShift;
	}

	int getGlobalBaseReg(MachineFunction mf)
	{
		// TODO: 17-7-18
		return 0;
	}
}
