package backend.target.x86;

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFrameInfo;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.target.TargetRegisterInfo;
import backend.type.Type;

import static backend.codegen.MachineInstrBuilder.*;
import static backend.codegen.MachineOperand.UseType.Use;

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
				FrameIndex, 0).addReg(srcReg, Use).getMInstr();
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
				Use).getMInstr();
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
	 * Return true if the specified function should have a dedicatedd stack pointer
	 * register. This is true if function has variable sized objects or if frame
	 * pointer elimination is disabled.
	 * @param mf
	 * @return
	 */
	private boolean hasFP(MachineFunction mf)
	{
		return true;//mf.getFrameInfo().hasVarSizedObjects();
	}

	/**
	 * This method insert prologue code into the function.
	 * @param mf
	 */
	@Override
	public void emitPrologue(MachineFunction mf)
	{
		MachineBasicBlock mbb = mf.getFirst();
		int mbbi = 0;  // a index position where a new instr will inserts.
		MachineFrameInfo mfi = mf.getFrameInfo();
		MachineInstr mi;

		// Get the number of bytes to allocate from the FrameInfo.
		int numBytes = mfi.getStackSize();
		if (hasFP(mf))
		{
			// get the offset of the stack slot for the %ebp register.
			int ebpOffset = mfi.getObjectOffset(mfi.getObjectIndexEnd()-1) + 4;
			if (numBytes!=0)
			{
				// adjust stack pointer: %esp -= numBytes.
				mi = buildMI(SUBri32, 2, ESP).addReg(ESP, Use).addZImm(numBytes).getMInstr();
				mbb.insert(mbbi++, mi);
			}

			// Save %ebp into the properly stack slot.
			// mov %ebp, ebpOffset+numBytes(%esp).
			mi = addRegOffset(buildMI(MOVrm32, 5), ESP, ebpOffset+numBytes).
					addReg(EBP, Use).getMInstr();
			mbb.insert(mbbi++, mi);

			// Update %ebp with new base value.
			if (numBytes == 0)
				mi = buildMI(MOVrr32, 2, EBP).addReg(ESP, Use).getMInstr();
			else
				mi = addRegOffset(buildMI(LEAr32, 5, EBP), ESP, numBytes).getMInstr();

			mbb.insert(mbbi++, mi);
		}
		else
		{
			// When we have no frame pointer, we reserve argument space for call sites
			// in the function immediately on entry to the current function.  This
			// eliminates the need for add/sub ESP brackets around call sites.
			numBytes += mfi.getMaxCallFrameSize();

			// round the size to a multiple of the alignment.
			int align = mf.getTargetMachine().getFrameInfo().getStackAlignment();
			numBytes = ((numBytes + 4) + align - 1) / align * align - 4;

			// update the frame info to pretend that this is part of stack.
			mfi.setStackSize(numBytes);

			if (numBytes != 0)
			{
				// adjust stack pointer: %esp -= numbetes.
				mi = buildMI(SUBri32, ESP).addReg(ESP, Use).addZImm(numBytes).getMInstr();
				mbb.insert(mbbi++, mi);
			}
		}
	}

	/**
	 * This method insert epilogue code into the function.
	 *
	 * @param mf
	 * @param mbb
	 */
	@Override
	public void emitEpilogue(MachineFunction mf,
			MachineBasicBlock mbb)
	{
		MachineFrameInfo mfi = mf.getFrameInfo();
		// get the position where epilogue code will inserts after.
		int mbbi = mbb.size()-1;
		MachineInstr mi;
		assert mbb.getInstAt(mbbi).getOpCode() == RET
				:"Can only insert epilogue code into returning blocks";

		if (hasFP(mf))
		{
			// get the offset of the stack slot for the %ebp register.
			// which is guaranteed to be the last slot by processFunctionBeforeFrameFinalized().
			int ebpOffset = mfi.getObjectOffset(mfi.getObjectIndexEnd()-1)+4;
			// mov %ebp, %esp.
			mi = buildMI(MOVrr32, 1, ESP).addReg(EBP, Use).getMInstr();
			mbb.insert(mbbi++, mi);

			// mov offset(%esp), %ebp.
			mi = addRegOffset(buildMI(MOVmr32, 5, EBP), ESP, ebpOffset).getMInstr();
			mbb.insert(mbbi++, mi);
		}
		else
		{
			// get the number of bytes allocated from the frameInfo.
			int numBytes = mfi.getStackSize();
			if (numBytes!=0)
			{
				// addjust stack size: %esp += numBytes (up)
				mi = buildMI(ADDri32, 2, ESP).addReg(ESP, Use).addZImm(numBytes).getMInstr();
				mbb.insert(mbbi++, mi);
			}
		}
	}

	/**
	 * Obtains the register class is enough to hold the specified data of typed
	 * {@code ty}.
	 * @param ty
	 * @return
	 */
	public TargetRegisterClass getRegClassForType(Type ty)
	{
		switch (ty.getPrimitiveID())
		{
			case Type.Int1TyID:
			case Type.Int8TyID:
				return x86R8GegClass;
			case Type.Int16TyID:
				return x86R16RegClass;
			case Type.Int32TyID:
			case Type.PointerTyID:
				return x86R32RegClass;
			case Type.Int64TyID:
				assert false:"Long type cannot filled in register!";
				return null;
			case Type.FloatTyID:
			case Type.DoubleTyID:
				return x86RFPClass;
			default:
				assert false:"Invalid type for regClass!";
				return null;
		}
	}
}
