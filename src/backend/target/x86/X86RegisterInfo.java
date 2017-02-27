package backend.target.x86;

import backend.codegen.*;
import backend.target.TargetRegisterDesc;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import backend.type.Type;
import tools.Util;

import static backend.codegen.MachineInstrBuilder.*;
import static backend.codegen.MachineOperand.MachineOperandType.MO_SignExtendedImmed;
import static backend.codegen.MachineOperand.UseType.Use;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86RegisterInfo extends TargetRegisterInfo implements X86RegNames,
		X86InstrNames
{
	// Now that we have all of the pieces, define the top-level register classes.
	// The order specified in the register list is implicitly defined to be the
	// register allocation order.
	private static int[] X86R8 = {AL, CL, DL, BL, AH, CH, DH, BH};
	private static int[] X86R16 = {AX, CX, DX, BX, SI, DI, BP, SP};
	private static int[] X86R32 = {EAX, ECX, EDX, EBX, ESI, EDI, EBP, ESP};
	private static int[] X86RFP = {FP0, FP1, FP2, FP3, FP4, FP5, FP6};
	private static int[] X86RST = {ST0, ST1, ST2, ST3, ST4, ST5, ST6, ST7};

	public static class X86R8RegisterClass extends TargetRegisterClass
	{
		public X86R8RegisterClass(int rs, int ra, int[] regs)
		{
			super(null, rs, ra, regs);
		}
	}

	public static class X86R16RegisterClass extends TargetRegisterClass
	{
		public X86R16RegisterClass(int rs, int ra, int[] regs)
		{
			super(null, rs, ra, regs);
		}

		@Override
		public int allocatableEnd(MachineFunction mf)
		{
			// If so, don't allocate SP or BP.
			if (hasFP(mf))
				return super.allocatableEnd(mf) - 2;
			else
				// If not, just don't allocate SP.
				return super.allocatableEnd(mf) - 1;
		}
	}

	public static class X86R32RegisterClass extends TargetRegisterClass
	{
		public X86R32RegisterClass(int rs, int ra, int[] regs)
		{
			super(null, rs, ra, regs);
		}
		@Override
		public int allocatableEnd(MachineFunction mf)
		{
			// If so, don't allocate SP or BP.
			if (hasFP(mf))
				return super.allocatableEnd(mf) - 2;
			else
				// If not, just don't allocate SP.
				return super.allocatableEnd(mf) - 1;
		}
	}

	public static class X86RSTRegisterClass extends TargetRegisterClass
	{
		public X86RSTRegisterClass(int rs, int ra, int[] regs)
		{
			super(null, rs, ra, regs);
		}
		@Override
		public int allocatableEnd(MachineFunction mf){return 0;}
	}

	public static class X86RFPRegisterClass extends TargetRegisterClass
	{
		public X86RFPRegisterClass(int rs, int ra, int[] regs)
		{
			super(null, rs, ra, regs);
		}
	}

	private static X86R8RegisterClass x86R8RegClass =
			new X86R8RegisterClass(1, 1, X86R8);

	private static X86R16RegisterClass x86R16RegClass =
			new X86R16RegisterClass(2, 2, X86R16);

	private static X86R32RegisterClass x86R32RegClass =
			new X86R32RegisterClass(4, 4, X86R32);

	private static X86RFPRegisterClass x86RFPClass =
			new X86RFPRegisterClass(4, 4, X86RFP);

	private static X86RSTRegisterClass x86RSTClass =
			new X86RSTRegisterClass(8, 4, X86RST);

	/**
	 * A static array holds all register classes for x86 target machine.
	 */
	private static TargetRegisterClass[] x86RegisterClasses =
			{ x86R8RegClass, x86R16RegClass, x86R32RegClass, x86RSTClass };
	/**
	 * A static array holds all register descriptor information
	 * for each register in x86.
	 */
	private static TargetRegisterDesc[] x86RegInfoDescs =
			{
					// 32bit general register.
				new TargetRegisterDesc("EAX", new int[]{AX, AH, AL}, null, 0, 0),
				new TargetRegisterDesc("EDX", new int[]{DX, DH, DL}, null, 0, 0),
				new TargetRegisterDesc("ESP", new int[]{SP}, null, 0, 0),
				new TargetRegisterDesc("ESI", new int[]{SI}, null, 0, 0),

					// 16bit general register.
				new TargetRegisterDesc("AX", new int[]{AH, AL}, new int[]{EAX}, 0, 0),
				new TargetRegisterDesc("DX", new int[]{DH, DL}, new int[]{EDX}, 0, 0),
				new TargetRegisterDesc("SP", null, new int[]{ESP}, 0, 0),
				new TargetRegisterDesc("SI", null, new int[]{ESI}, 0, 0),

					// 8bit general register.
				new TargetRegisterDesc("AL", null, new int[]{AX, EAX}, 0, 0),
				new TargetRegisterDesc("DL", null, new int[]{EDX}, 0, 0),
				new TargetRegisterDesc("AH", null, new int[]{AX, EAX}, 0, 0),
				new TargetRegisterDesc("DH", null, new int[]{DX, EAX}, 0, 0),
				// Pesudo floating point register.
				new TargetRegisterDesc("FP0", null, null, 0, 0),
				new TargetRegisterDesc("FP1", null, null, 0, 0),
				new TargetRegisterDesc("FP2", null, null, 0, 0),
				new TargetRegisterDesc("FP3", null, null, 0, 0),
				new TargetRegisterDesc("FP4", null, null, 0, 0),
				new TargetRegisterDesc("FP5", null, null, 0, 0),
				new TargetRegisterDesc("FP6", null, null, 0, 0),

				// stack floating point register.
				new TargetRegisterDesc("ST0", null, null, 0, 0),
				new TargetRegisterDesc("ST1", null, null, 0, 0),
				new TargetRegisterDesc("ST2", null, null, 0, 0),
				new TargetRegisterDesc("ST3", null, null, 0, 0),
				new TargetRegisterDesc("ST4", null, null, 0, 0),
				new TargetRegisterDesc("ST5", null, null, 0, 0),
				new TargetRegisterDesc("ST6", null, null, 0, 0),
				new TargetRegisterDesc("ST7", null, null, 0, 0),
			};

	private static int[] calleeSavedRegs = {ESI, EDI, EBX, EBP};

	private static TargetRegisterClass[] calleeSavedRegClasses =
			{x86R32RegClass, x86R32RegClass, x86R32RegClass, x86R32RegClass};

	public X86RegisterInfo()
	{
		super(x86RegInfoDescs, x86RegisterClasses, ADJCALLSTACKDOWN, ADJCALLSTACKUP);
	}

	public static int getIdx(TargetRegisterClass rc)
	{
		if (rc == x86R8RegClass)
			return 0;
		else if (rc == x86R16RegClass)
			return 1;
		else if (rc == x86R32RegClass)
			return 2;
		else if (rc == x86RFPClass || rc == x86RSTClass)
			return 3;
		else
		{
			assert false:"Illegal target register class!";
			return -1;
		}
	}

	@Override
	public int[] getCalleeRegisters(){return calleeSavedRegs;}

	public TargetRegisterClass[] getCalleeSavedRegClasses()
	{
		return calleeSavedRegClasses;
	}

	@Override
	public int storeRegToStackSlot(MachineBasicBlock mbb, int mbbi, int srcReg,
			int FrameIndex, TargetRegisterClass rc)
	{
		int opcode[] = {MOVrm8, MOVrm16, MOVrm32, FSTPr64};
		MachineInstr instr = addFrameReference(buildMI(opcode[getIdx(rc)], 5),
				FrameIndex, 0).addReg(srcReg, Use).getMInstr();
		mbb.insert(mbbi, instr);
		return mbbi + 1;
	}

	@Override
	public int loadRegFromStackSlot(MachineBasicBlock mbb, int mbbi,
			int destReg, int FrameIndex, TargetRegisterClass rc)
	{
		int opcode[] = {MOVmr8, MOVmr16, MOVmr32, FLDr64};
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
		MachineRegisterInfo mri = mbb.getParent().getMachineRegisterInfo();
		mri.setDefMO(destReg, instr.getOperand(0));
		mri.getDefMO(srcReg).getDefUseList().add(instr.getOperand(1));

		mbb.insert(mbbi, instr);
		return mbbi + 1;
	}

	/**
	 * This method is called during prolog/epilog code insertion to eliminate
	 * call frame setup and destroy pseudo instructions (but only if the
	 * Target is using them).  It is responsible for eliminating these
	 * instructions, replacing them with concrete instructions.  This method
	 * need only be implemented if using call frame setup/destroy pseudo
	 * instructions.
	 */
	@Override
	public void eliminateCallFramePseudoInstr(MachineFunction mf,
			MachineBasicBlock mbb, int idx)
	{
		MachineInstr newOne = null, old = mbb.getInstAt(idx);

		if (hasFP(mf))
		{
			// If we have a frame pointer, turn the adjcallstackup instruction into a
			// 'sub ESP, <amt>' and the adjcallstackdown instruction into 'add ESP,
			// <amt>'
			long amount = old.getOperand(0).getImmedValue();
			if (amount != 0)
			{
				int align = mf.getTargetMachine().getFrameInfo().getStackAlignment();
				amount = Util.roundUp(amount, align);

				// stack setup pseudo instrcution.
				if (old.getOpCode() == X86InstrNames.ADJCALLSTACKDOWN)
				{
					newOne = buildMI(X86InstrNames.SUBri32, 2, X86RegNames.ESP).
							addReg(X86RegNames.ESP).
							addZImm(amount).getMInstr();
				}
				else
				{
					assert (old.getOpCode() == X86InstrNames.ADJCALLSTACKUP);
					// stack destroy pseudo instruction.
					newOne = buildMI(X86InstrNames.ADDri32, 2, X86RegNames.ESP).
							addReg(X86RegNames.ESP).
							addZImm(amount).getMInstr();
				}
			}
		}
		if (newOne != null)
			mbb.replace(idx, newOne);
		else
			mbb.erase(idx);
	}
	/**
	 * This method is called immediately before the specified functions frame
	 * layout (MF.getFrameInfo()) is finalized.  Once the frame is finalized,
	 * MO_FrameIndex operands are replaced with direct ants.  This method is
	 * optional.
	 */
	@Override
	public void processFunctionBeforeFrameFinalized(
			MachineFunction mf)
	{
		if (hasFP(mf))
		{
			// creates a stack object for saving EBP.
			int frameIndex = mf.getFrameInfo().createStackObject(4, 4);
			assert frameIndex == mf.getFrameInfo().getObjectIndexEnd() - 1
					:"The slot for EBP must be last";
		}
	}

	@Override
	public void eliminateFrameIndex(MachineFunction mf,
			MachineBasicBlock mbb, int ii)
	{
		MachineInstr mi = mbb.getInstAt(ii);
		int i = 0;
		while(!mi.getOperand(i).isFrameIndex())
		{
			i++;
			assert i < mi.getNumOperands():"Instr have not frame index operand!";
		}

		int frameIndex = mi.getOperand(i).getFrameIndex();
		mi.setMachineOperandReg(i, hasFP(mf)? X86RegNames.EBP : X86RegNames.ESP);

		int offset = mf.getFrameInfo().getObjectOffset(frameIndex) +
				(int)mi.getOperand(i+3).getImmedValue() + 4;

		if (!hasFP(mf))
			offset += mf.getFrameInfo().getStackSize();

		mi.setMachineOperandConst(i+3, MO_SignExtendedImmed, offset);
	}

	/**
	 * Return true if the specified function should have a dedicatedd stack pointer
	 * register. This is true if function has variable sized objects or if frame
	 * pointer elimination is disabled.
	 *
	 * the frame pointer is usually EBP in X86 target machine.
	 * @param mf
	 * @return
	 */
	public static boolean hasFP(MachineFunction mf)
	{
		return mf.getFrameInfo().hasVarSizedObjects();
	}

	/**
	 * This method insert prologue code into the function.
	 * @param mf
	 */
	@Override
	public void emitPrologue(MachineFunction mf)
	{
		MachineBasicBlock mbb = mf.getEntryBlock();
		int mbbi = 0;  // a index position where a new instr will inserts.
		MachineFrameInfo mfi = mf.getFrameInfo();
		MachineInstr mi;

		// Get the number of bytes to allocate from the FrameInfo.
		int numBytes = mfi.getStackSize();
		if (hasFP(mf))
		{
			// get the offset of the stack slot for the %ebp register.
			// Note that: this offset is away from ESP.
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
				return x86R8RegClass;
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
