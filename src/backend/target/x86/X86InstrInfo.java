package backend.target.x86;

import backend.codegen.MachineInstr;
import backend.codegen.MachineInstrBuilder;
import backend.codegen.MachineOperand.UseType;
import backend.target.TargetInstrInfo;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86InstrInfo extends TargetInstrInfo implements X86InstrSets
{
	/**
	 * A static array for holding all machine instruction for X86 architecture.
	 */
	private static MCInstrDescriptor[] x86Insts =
	{

	};

	public X86InstrInfo()
	{
		super(x86Insts);
	}

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
		return MachineInstrBuilder.buildMI(X86InstrSets.XCHGrr16, 2).
				addReg(X86RegsSet.AX, UseType.UseAndDef).
				addReg( X86RegsSet.AX, UseType.UseAndDef)
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
}
