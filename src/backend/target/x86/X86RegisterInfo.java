package backend.target.x86;

import backend.codegen.*;
import backend.codegen.MachineOperand.UseType;
import backend.target.TargetRegisterInfo;

import static backend.codegen.MachineInstrBuilder.addFrameReference;
import static backend.codegen.MachineInstrBuilder.buildMI;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86RegisterInfo extends TargetRegisterInfo implements X86RegsSet, X86InstrSets
{
	// Now that we have all of the pieces, define the top-level register classes.
	// The order specified in the register list is implicitly defined to be the
	// register allocation order.
	private static TargetRegisterClass x86R8GegClass =
			new TargetRegisterClass(1, 1, new int[]{AL, CL, DL, BL, AH, CH, DH, BH});

	private static TargetRegisterClass x86R16RegClass =
			new TargetRegisterClass(2, 2, new int[]{AX, CX, DX, BX, SI, DI, BP, SP});

	private static TargetRegisterClass x86R32RegClass =
			new TargetRegisterClass(4, 4, new int[]{EAX, ECX, EDX, EBX, ESI, EDI, ESP});

	private static TargetRegisterClass x86RFPClass =
			new TargetRegisterClass(4, 4, new int[]{FP0, FP1, FP2, FP3, FP4, FP5, FP6});

	/**
	 * A static array holds all register classes for x86 target machine.
	 */
	private static TargetRegisterClass[] x86RegisterClasses =
			{
				x86R8GegClass, x86R16RegClass, x86R32RegClass
			};
	/**
	 * A static array holds all register descriptor information
	 * for each register in x86.
	 */
	private static MCRegisterDesc[] x86RegInfoDescs =
			{
					// 32bit general register.
				new MCRegisterDesc("EAX", new int[]{AX, AH, AL}, null, 0, 0),
				new MCRegisterDesc("EDX", new int[]{DX, DH, DL}, null, 0, 0),
				new MCRegisterDesc("ESP", new int[]{SP}, null, 0, 0),
				new MCRegisterDesc("ESI", new int[]{SI}, null, 0, 0),

					// 16bit general register.
				new MCRegisterDesc("AX", new int[]{AH, AL}, new int[]{EAX}, 0, 0),
				new MCRegisterDesc("DX", new int[]{DH, DL}, new int[]{EDX}, 0, 0),
				new MCRegisterDesc("SP", null, new int[]{ESP}, 0, 0),
				new MCRegisterDesc("SI", null, new int[]{ESI}, 0, 0),

					// 8bit general register.
				new MCRegisterDesc("AL", null, new int[]{AX, EAX}, 0, 0),
				new MCRegisterDesc("DL", null, new int[]{EDX}, 0, 0),
				new MCRegisterDesc("AH", null, new int[]{AX, EAX}, 0, 0),
				new MCRegisterDesc("DH", null, new int[]{DX, EAX}, 0, 0),
				// Pesudo floating point register.
				new MCRegisterDesc("FP0", null, null, 0, 0),
				new MCRegisterDesc("FP1", null, null, 0, 0),
				new MCRegisterDesc("FP2", null, null, 0, 0),
				new MCRegisterDesc("FP3", null, null, 0, 0),
				new MCRegisterDesc("FP4", null, null, 0, 0),
				new MCRegisterDesc("FP5", null, null, 0, 0),
				new MCRegisterDesc("FP6", null, null, 0, 0),
				new MCRegisterDesc("FP7", null, null, 0, 0),

				// stack floating point register.
				new MCRegisterDesc("ST0", null, null, 0, 0),
				new MCRegisterDesc("ST1", null, null, 0, 0),
				new MCRegisterDesc("ST2", null, null, 0, 0),
				new MCRegisterDesc("ST3", null, null, 0, 0),
				new MCRegisterDesc("ST4", null, null, 0, 0),
				new MCRegisterDesc("ST5", null, null, 0, 0),
				new MCRegisterDesc("ST6", null, null, 0, 0),
				new MCRegisterDesc("ST7", null, null, 0, 0),
			};

	public X86RegisterInfo()
	{
		super(x86RegInfoDescs, x86RegisterClasses, ADJCALLSTACKDOWN, ADJCALLSTACKUP);
	}

	public static int getIdx(TargetRegisterClass rc)
	{
		switch (rc.getRegSize())
		{
			default: assert false:"Invalid data size!";
			case 1: return 0;
			case 2: return 1;
			case 4: return 2;
			case 10: return 3;
		}
	}

	@Override
	public int[] getCalleeRegisters()
	{
		return new int[0];
	}

	@Override
	public int storeRegToStackSlot(MachineBasicBlock mbb, int mbbi, int srcReg,
			int FrameIndex, TargetRegisterClass rc)
	{
		int opcode[] = {MOVrm8, MOVrm16, MOVrm32, FSTPr80};
		MachineInstr instr = addFrameReference(buildMI(opcode[getIdx(rc)], 5),
				FrameIndex, 0).addReg(srcReg, UseType.Use).getMInstr();
		mbb.insert(mbbi, instr);
		return mbbi + 1;
	}

	@Override public int loadRegFromStackSlot(MachineBasicBlock mbb, int mbbi,
			int destReg, int FrameIndex, TargetRegisterClass rc)
	{
		int opcode[] = {MOVmr8, MOVmr16, MOVmr32, FLDr80};
		MachineInstr instr = addFrameReference(buildMI(opcode[getIdx(rc)], 4, destReg),
				FrameIndex, 0).getMInstr();
		mbb.insert(mbbi, instr);
		return mbbi + 1;
	}

	@Override
	public int copyRegToReg(MachineBasicBlock mbb, int mbbi, int destReg,
			int srcReg, TargetRegisterClass rc)
	{
		int opcode[] = {MOVrr8, MOVrr16, MOVrr32, FpMOV};
		MachineInstr instr = buildMI(opcode[getIdx(rc)], 1, destReg).addReg(srcReg,
				UseType.Use).getMInstr();
		mbb.insert(mbbi, instr);
		return mbbi + 1;
	}

	/**
	 * processFunctionBeforeFrameFinalized - This method is called immediately
	 * before the specified functions frame layout (MF.getFrameInfo()) is
	 * finalized.  Once the frame is finalized, MO_FrameIndex operands are
	 * replaced with direct ants.  This method is optional.
	 *
	 * @param mf
	 */
	@Override public void processFunctionBeforeFrameFinalized(
			MachineFunction mf)
	{

	}

	@Override public void eliminateFrameIndex(MachineFunction mf, int ii)
	{

	}

	/**
	 * This method insert prologue code into the function.
	 *
	 * @param mf
	 */
	@Override public void emitPrologue(MachineFunction mf)
	{
		MachineBasicBlock mbb = mf.getFirst();
		MachineFrameInfo mfi = mf.getFrameInfo();
		MachineInstr mi;

		// Get the number of bytes to allocate from the FrameInfo.
		int numBytes = mfi.getStackSize();
		if ()
	}

	/**
	 * This method insert epilogue code into the function.
	 *
	 * @param MF
	 * @param mbb
	 */
	@Override public void emitEpilogue(MachineFunction MF,
			MachineBasicBlock mbb)
	{

	}
}
