package backend.target.x86;

import tools.Util;
import backend.codegen.*;
import backend.codegen.MachineOperand.RegState;
import backend.support.Attribute;
import backend.target.TargetData;
import backend.target.TargetFrameInfo;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterClass;
import backend.type.Type;
import backend.value.Function;
import tools.BitMap;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.MachineInstrBuilder.addRegOffset;
import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.target.TargetOptions.DisableFramePointerElim;
import static backend.target.TargetOptions.EnableRealignStack;
import static backend.target.x86.X86GenInstrNames.*;
import static backend.target.x86.X86GenRegisterNames.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86RegisterInfo extends X86GenRegisterInfo
{
	/**
	 * Native X86 register numbers
	 */
	public interface N86
	{
		int EAX = 0;
		int ECX = 1;
		int EDX = 2;
		int EBX = 3;
		int ESP = 4;
		int EBP = 5;
		int ESI = 6;
		int EDI = 7;
	}

	/**
	 * The index of various sized subregister classes. Note that
	 * these indices must be kept in sync with the class indices in the
	 * X86RegisterInfo.td file.
	 */
	public final static int SUBREG_8BIT = 1;
	public final static int SUBREG_8BIT_HI = 2;
	public final static int SUBREG_16BIT = 3;
	public final static int SUBREG_32BIT = 4;

	private X86TargetMachine tm;
	private TargetInstrInfo tii;

	/**
	 * Is the target 64-bits.
	 */
	private boolean is64Bit;

	/**
	 * Is the target on of win64 flavours
	 */
	private boolean isWin64;

	/**
	 * Stack slot size in bytes.
	 */
	private int slotSize;

	/**
	 * Default stack alignment.
	 */
	private int stackAlign;

	/**
	 * X86 physical register used as stack ptr.
	 */
	private int stackPtr;
	/**
	 * X86 physical register used as frame ptr.
	 */
	private int framePtr;

	public X86RegisterInfo(X86TargetMachine tm, TargetInstrInfo tii)
	{
		super(tm.getSubtarget().is64Bit() ?
				ADJCALLSTACKDOWN64 :
				ADJCALLSTACKDOWN32, tm.getSubtarget().is64Bit() ?
				ADJCALLSTACKUP64 :
				ADJCALLSTACKUP32);
		this.tm = tm;
		this.tii = tii;
		X86Subtarget subtarget = tm.getSubtarget();
		is64Bit = subtarget.is64Bit();
		isWin64 = subtarget.isTargetWin64();
		stackAlign = tm.getFrameInfo().getStackAlignment();

		if (is64Bit)
		{
			slotSize = 8;
			stackPtr = RSP;
			framePtr = RBP;
		}
		else
		{
			slotSize = 4;
			stackPtr = ESP;
			framePtr = EBP;
		}
	}

	public static int getX86RegNum(int regNo)
	{
		switch (regNo)
		{
			case RAX:
			case EAX:
			case AX:
			case AL:
				return N86.EAX;
			case RCX:
			case ECX:
			case CX:
			case CL:
				return N86.ECX;
			case RDX:
			case EDX:
			case DX:
			case DL:
				return N86.EDX;
			case RBX:
			case EBX:
			case BX:
			case BL:
				return N86.EBX;
			case RSP:
			case ESP:
			case SP:
			case SPL:
			case AH:
				return N86.ESP;
			case RBP:
			case EBP:
			case BP:
			case BPL:
			case CH:
				return N86.EBP;
			case RSI:
			case ESI:
			case SI:
			case SIL:
			case DH:
				return N86.ESI;
			case RDI:
			case EDI:
			case DI:
			case DIL:
			case BH:
				return N86.EDI;

			case R8:
			case R8D:
			case R8W:
			case R8B:
				return N86.EAX;
			case R9:
			case R9D:
			case R9W:
			case R9B:
				return N86.ECX;
			case R10:
			case R10D:
			case R10W:
			case R10B:
				return N86.EDX;
			case R11:
			case R11D:
			case R11W:
			case R11B:
				return N86.EBX;
			case R12:
			case R12D:
			case R12W:
			case R12B:
				return N86.ESP;
			case R13:
			case R13D:
			case R13W:
			case R13B:
				return N86.EBP;
			case R14:
			case R14D:
			case R14W:
			case R14B:
				return N86.ESI;
			case R15:
			case R15D:
			case R15W:
			case R15B:
				return N86.EDI;

			case ST0:
			case ST1:
			case ST2:
			case ST3:
			case ST4:
			case ST5:
			case ST6:
			case ST7:
				return regNo - ST0;

			case XMM0:
			case XMM8:
			case MM0:
				return 0;
			case XMM1:
			case XMM9:
			case MM1:
				return 1;
			case XMM2:
			case XMM10:
			case MM2:
				return 2;
			case XMM3:
			case XMM11:
			case MM3:
				return 3;
			case XMM4:
			case XMM12:
			case MM4:
				return 4;
			case XMM5:
			case XMM13:
			case MM5:
				return 5;
			case XMM6:
			case XMM14:
			case MM6:
				return 6;
			case XMM7:
			case XMM15:
			case MM7:
				return 7;

			default:
				Util.assertion(isVirtualRegister(regNo),  "Undefined physical register!");
				Util.shouldNotReachHere(
						"Register allocator hasn't allocated reg correctly yet!");
				return 0;
		}
	}

	public int getStackAlignment()
	{
		return stackAlign;
	}

	/**
	 * allows modification of X86GenRegisterInfo::getDwarfRegNum
	 * (created by TableGen) for target dependencies.
	 *
	 * @param regNum
	 * @param isEH
	 * @return
	 */
	public int getDwarfRegNum(int regNum, boolean isEH)
	{
		Util.assertion(false,  "Shoult not reaching here!");
		return 0;
	}

	public TargetRegisterClass getMatchingSuperRegClass(TargetRegisterClass a,
			TargetRegisterClass b, int subIdx)
	{
		switch (subIdx)
		{
			default:
				return null;
			case 1:
				// 8-bit
				if (b == GR8RegisterClass)
				{
					if (a.getRegSize() == 2 || a.getRegSize() == 4
							|| a.getRegSize() == 8)
						return a;
				}
				else if (b == GR8_ABCD_LRegisterClass || b == GR8_ABCD_HRegisterClass)
				{
					if (a == GR64RegisterClass || a == GR64_ABCDRegisterClass
							|| a == GR64_NOREXRegisterClass || a == GR64_NOSPRegisterClass
							|| a == GR64_NOREX_NOSPRegisterClass)
						return GR64_ABCDRegisterClass;
					else if (a == GR32RegisterClass || a == GR32_ABCDRegisterClass
							|| a == GR32_NOREXRegisterClass || a == GR32_NOSPRegisterClass)
						return GR32_ABCDRegisterClass;
					else if (a == GR16RegisterClass || a == GR16_ABCDRegisterClass
							|| a == GR16_NOREXRegisterClass)
						return GR16_ABCDRegisterClass;
				}
				else if (b == GR8_NOREXRegisterClass)
				{
					if (a == GR64RegisterClass || a == GR64_NOREXRegisterClass
							|| a == GR64_NOSPRegisterClass || a == GR64_NOREX_NOSPRegisterClass)
						return GR64_NOREXRegisterClass;
					else if (a == GR64_ABCDRegisterClass)
						return GR64_ABCDRegisterClass;
					else if (a == GR32RegisterClass || a == GR32_NOREXRegisterClass
							|| a == GR32_NOSPRegisterClass)
						return GR32_NOREXRegisterClass;
					else if (a == GR32_ABCDRegisterClass)
						return GR32_ABCDRegisterClass;
					else if (a == GR16RegisterClass || a == GR16_NOREXRegisterClass)
						return GR16_NOREXRegisterClass;
					else if (a == GR16_ABCDRegisterClass)
						return GR16_ABCDRegisterClass;
				}
				break;
			case 2:
				// 8-bit hi
				if (b == GR8_ABCD_HRegisterClass)
				{
					if (a == GR64RegisterClass || a == GR64_ABCDRegisterClass
							|| a == GR64_NOREXRegisterClass || a == GR64_NOSPRegisterClass
							|| a == GR64_NOREX_NOSPRegisterClass)
						return GR64_ABCDRegisterClass;
					else if (a == GR32RegisterClass || a == GR32_ABCDRegisterClass
							|| a == GR32_NOREXRegisterClass || a == GR32_NOSPRegisterClass)
						return GR32_ABCDRegisterClass;
					else if (a == GR16RegisterClass || a == GR16_ABCDRegisterClass
							|| a == GR16_NOREXRegisterClass)
						return GR16_ABCDRegisterClass;
				}
				break;
			case 3:
				// 16-bit
				if (b == GR16RegisterClass)
				{
					if (a.getRegSize() == 4 || a.getRegSize() == 8)
						return a;
				}
				else if (b == GR16_ABCDRegisterClass)
				{
					if (a == GR64RegisterClass || a == GR64_ABCDRegisterClass
							|| a == GR64_NOREXRegisterClass || a == GR64_NOSPRegisterClass
							|| a == GR64_NOREX_NOSPRegisterClass)
						return GR64_ABCDRegisterClass;
					else if (a == GR32RegisterClass || a == GR32_ABCDRegisterClass
							|| a == GR32_NOREXRegisterClass || a == GR32_NOSPRegisterClass)
						return GR32_ABCDRegisterClass;
				}
				else if (b == GR16_NOREXRegisterClass)
				{
					if (a == GR64RegisterClass || a == GR64_NOREXRegisterClass
							|| a == GR64_NOSPRegisterClass || a == GR64_NOREX_NOSPRegisterClass)
						return GR64_NOREXRegisterClass;
					else if (a == GR64_ABCDRegisterClass)
						return GR64_ABCDRegisterClass;
					else if (a == GR32RegisterClass || a == GR32_NOREXRegisterClass
							|| a == GR32_NOSPRegisterClass)
						return GR32_NOREXRegisterClass;
					else if (a == GR32_ABCDRegisterClass)
						return GR64_ABCDRegisterClass;
				}
				break;
			case 4:
				// 32-bit
				if (b == GR32RegisterClass || b == GR32_NOSPRegisterClass)
				{
					if (a.getRegSize() == 8)
						return a;
				}
				else if (b == GR32_ABCDRegisterClass)
				{
					if (a == GR64RegisterClass || a == GR64_ABCDRegisterClass
							|| a == GR64_NOREXRegisterClass || a == GR64_NOSPRegisterClass
							|| a == GR64_NOREX_NOSPRegisterClass)
						return GR64_ABCDRegisterClass;
				}
				else if (b == GR32_NOREXRegisterClass)
				{
					if (a == GR64RegisterClass || a == GR64_NOREXRegisterClass
							|| a == GR64_NOSPRegisterClass || a == GR64_NOREX_NOSPRegisterClass)
						return GR64_NOREXRegisterClass;
					else if (a == GR64_ABCDRegisterClass)
						return GR64_ABCDRegisterClass;
				}
				break;
		}
		return null;
	}

	public TargetRegisterClass getPointerRegClass()
	{
		return getPointerRegClass(0);
	}

	@Override public TargetRegisterClass getPointerRegClass(int kind)
	{
		switch (kind)
		{
			default:
				Util.shouldNotReachHere(
						"Unexpected kind in getPointerRegClass()!");
			case 0:
				if (tm.getSubtarget().is64Bit())
					return GR64RegisterClass;
				return GR32RegisterClass;
			case 1:
				if (tm.getSubtarget().is64Bit())
					return GR64_NOSPRegisterClass;
				return GR32_NOSPRegisterClass;
		}
	}

	@Override
	public TargetRegisterClass getCrossCopyRegClass(TargetRegisterClass rc)
	{
		if (rc == CCRRegisterClass)
		{
			if (is64Bit)
				return GR64RegisterClass;
			else
				return GR32RegisterClass;
		}
		return null;
	}

	@Override public int[] getCalleeSavedRegs(MachineFunction mf)
	{
		final int[] calleeSavedRegs32Bit = { ESI, EDI, EBX, EBP };

		final int[] calleeSavedRegs32EHRet = { EAX, EDX, ESI, EDI, EBX, EBP };

		final int[] calleeSavedRegs64Bit = { RBX, R12, R13, R14, R15, RBP };

		final int[] calleeSavedRegs64EHRet = { RAX, RDX, RBX, R12, R13, R14,
				R15, RBP };

		final int[] calleeSavedRegsWin64 = { RBX, RBP, RDI, RSI, R12, R13, R14,
				R15, XMM6, XMM7, XMM8, XMM9, XMM10, XMM11, XMM12, XMM13, XMM14,
				XMM15 };

		if (is64Bit)
		{
			if (isWin64)
				return calleeSavedRegsWin64;
			else
				return calleeSavedRegs32Bit;
		}
		else
			return calleeSavedRegs32Bit;
	}

	@Override
	public TargetRegisterClass[] getCalleeSavedRegClasses(MachineFunction mf)
	{
		final TargetRegisterClass[] calleeSavedRegClasses32Bit = {
				GR32RegisterClass, GR32RegisterClass, GR32RegisterClass,
				GR32RegisterClass };
		final TargetRegisterClass[] calleeSavedRegClasses32EHRet = {
				GR32RegisterClass, GR32RegisterClass, GR32RegisterClass,
				GR32RegisterClass, GR32RegisterClass, GR32RegisterClass };
		final TargetRegisterClass[] calleeSavedRegClasses64Bit = {
				GR64RegisterClass, GR64RegisterClass, GR64RegisterClass,
				GR64RegisterClass, GR64RegisterClass, GR64RegisterClass };
		final TargetRegisterClass[] calleeSavedRegClasses64EHRet = {
				GR64RegisterClass, GR64RegisterClass, GR64RegisterClass,
				GR64RegisterClass, GR64RegisterClass, GR64RegisterClass,
				GR64RegisterClass, GR64RegisterClass };
		final TargetRegisterClass[] calleeSavedRegClassesWin64 = {
				GR64RegisterClass, GR64RegisterClass, GR64RegisterClass,
				GR64RegisterClass, GR64RegisterClass, GR64RegisterClass,
				GR64RegisterClass, GR64RegisterClass, VR128RegisterClass,
				VR128RegisterClass, VR128RegisterClass, VR128RegisterClass,
				VR128RegisterClass, VR128RegisterClass, VR128RegisterClass,
				VR128RegisterClass, VR128RegisterClass, VR128RegisterClass };
		if (is64Bit)
		{
			if (isWin64)
				return calleeSavedRegClassesWin64;
			else
				return calleeSavedRegClasses64Bit;
		}
		else
			return calleeSavedRegClasses32Bit;
	}

	@Override public boolean needsStackRealignment(MachineFunction mf)
	{
		MachineFrameInfo mfi = mf.getFrameInfo();

		return EnableRealignStack.value && mfi.getMaxAlignment() > stackAlign
				&& !mfi.hasVarSizedObjects();
	}

	@Override
    public boolean hasReservedCallFrame(MachineFunction mf)
	{
		return !mf.getFrameInfo().hasVarSizedObjects();
	}

	@Override
    public boolean hasReservedSpillSlot(MachineFunction mf, int reg,
			OutParamWrapper<Integer> frameIdx)
	{
		if (reg == framePtr && hasFP(mf))
		{
			frameIdx.set(mf.getFrameInfo().getObjectIndexBegin());
			return true;
		}
		return false;
	}

	@Override
	public void processFunctionBeforeCalleeSavedScan(MachineFunction mf)
	{
		processFunctionBeforeCalleeSavedScan(mf, null);
	}

	private static int calculateMaxStackAlignment(MachineFrameInfo mfi)
	{
		int maxAlign = 0;

		for (int i = mfi.getObjectIndexBegin(), e = mfi.getObjectIndexEnd();
		     i != e; i++)
		{
			if (mfi.isDeadObjectIndex(i))
				continue;
			int align = mfi.getObjectAlignment(i);
			maxAlign = Math.max(maxAlign, align);
		}

		return maxAlign;
	}

	@Override
	public void processFunctionBeforeCalleeSavedScan(MachineFunction mf,
			RegScavenger rs)
	{
		MachineFrameInfo mfi = mf.getFrameInfo();

		int maxAlign = Math
				.max(mfi.getMaxAlignment(), calculateMaxStackAlignment(mfi));

		mfi.setMaxAlignment(maxAlign);

		if (hasFP(mf))
		{
			TargetFrameInfo tfi = mf.getTarget().getFrameInfo();
			int frameIndex = mfi.createFixedObject(slotSize,
					-slotSize + tfi.getLocalAreaOffset());
			Util.assertion(frameIndex == mfi					.getObjectIndexBegin(),  "Slot for EBP register must be last in order to be found!");

		}
	}

	public void emitCalleeSavedFrameMoves(MachineFunction mf, int labelId,
			int framePtr)
	{
		// TODO: 17-7-20
		Util.assertion(false,  "Should not reaching here");
	}

	@Override public int getRARegister()
	{
		return is64Bit ? RIP : EIP;
	}

	@Override
    public int getFrameRegister(MachineFunction mf)
	{
		return hasFP(mf) ? framePtr : stackPtr;
	}

	@Override
    public int getFrameIndexOffset(MachineFunction mf, int fi)
	{
		TargetFrameInfo tfi = mf.getTarget().getFrameInfo();
		MachineFrameInfo mfi = mf.getFrameInfo();
		int offset = mfi.getObjectOffset(fi) - tfi.getLocalAreaOffset();
		int stackSize = mfi.getStackSize();

		if (needsStackRealignment(mf))
		{
			if (fi < 0)
			{
				offset += slotSize;
			}
			else
			{
				int align = mfi.getObjectAlignment(fi);
				Util.assertion( (-(offset + stackSize)) % align == 0);
				align = 0;
				return offset + stackSize;
			}
		}
		else
		{
			if (!hasFP(mf))
				return offset + stackSize;

			offset += slotSize;
		}
		return offset;
	}

	public void getInitialFrameState(ArrayList<MachineMove> moves)
	{
		int stackGrowth = is64Bit ? -8 : -4;

		MachineLocation dst = new MachineLocation(MachineLocation.VirtualFP);
		MachineLocation src = new MachineLocation(stackPtr, stackGrowth);
		moves.add(new MachineMove(0, dst, src));

		MachineLocation csdst = new MachineLocation(stackPtr, stackGrowth);
		MachineLocation cssrc = new MachineLocation(getRARegister());
		moves.add(new MachineMove(0, csdst, cssrc));
	}

	/**
	 * Test if the given register is a physical h register.
	 *
	 * @param reg
	 * @return
	 */
	private static boolean isHReg(int reg)
	{
		return GR8_ABCD_HRegisterClass.contains(reg);
	}

	private static int getStoreRegOpcode(int srcReg, TargetRegisterClass rc,
			boolean isStackAligned, X86TargetMachine tm)
	{
		int opc = 0;
		if (rc == GR64RegisterClass || rc == GR64_NOSPRegisterClass)
		{
			opc = MOV64mr;
		}
		else if (rc == GR32RegisterClass || rc == GR32_NOSPRegisterClass)
		{
			opc = MOV32mr;
		}
		else if (rc == GR16RegisterClass)
		{
			opc = MOV16mr;
		}
		else if (rc == GR8RegisterClass)
		{
			// Copying to or from a physical H register on x86-64 requires a NOREX
			// move.  Otherwise use a normal move.
			if (isHReg(srcReg) && tm.getSubtarget().is64Bit())
				opc = MOV8mr_NOREX;
			else
				opc = MOV8mr;
		}
		else if (rc == GR64_ABCDRegisterClass)
		{
			opc = MOV64mr;
		}
		else if (rc == GR32_ABCDRegisterClass)
		{
			opc = MOV32mr;
		}
		else if (rc == GR16_ABCDRegisterClass)
		{
			opc = MOV16mr;
		}
		else if (rc == GR8_ABCD_LRegisterClass)
		{
			opc = MOV8mr;
		}
		else if (rc == GR8_ABCD_HRegisterClass)
		{
			if (tm.getSubtarget().is64Bit())
				opc = MOV8mr_NOREX;
			else
				opc = MOV8mr;
		}
		else if (rc == GR64_NOREXRegisterClass || rc == GR64_NOREX_NOSPRegisterClass)
		{
			opc = MOV64mr;
		}
		else if (rc == GR32_NOREXRegisterClass)
		{
			opc = MOV32mr;
		}
		else if (rc == GR16_NOREXRegisterClass)
		{
			opc = MOV16mr;
		}
		else if (rc == GR8_NOREXRegisterClass)
		{
			opc = MOV8mr;
		}
		else if (rc == RFP80RegisterClass)
		{
			opc = ST_FpP80m;   // pops
		}
		else if (rc == RFP64RegisterClass)
		{
			opc = ST_Fp64m;
		}
		else if (rc == RFP32RegisterClass)
		{
			opc = ST_Fp32m;
		}
		else if (rc == FR32RegisterClass)
		{
			opc = MOVSSmr;
		}
		else if (rc == FR64RegisterClass)
		{
			opc = MOVSDmr;
		}
		else if (rc == VR128RegisterClass)
		{
			// If stack is realigned we can use aligned stores.
			opc = isStackAligned ? MOVAPSmr : MOVUPSmr;
		}
		else if (rc == VR64RegisterClass)
		{
			opc = MMX_MOVQ64mr;
		}
		else
		{
			Util.shouldNotReachHere("Undefined regclass");
		}

		return opc;
	}

	private static int getLoadRegOpcode(int DestReg, TargetRegisterClass rc,
			boolean isStackAligned, X86TargetMachine tm)
	{
		int opc = 0;
		if (rc == GR64RegisterClass || rc == GR64_NOSPRegisterClass)
		{
			opc = MOV64rm;
		}
		else if (rc == GR32RegisterClass || rc == GR32_NOSPRegisterClass)
		{
			opc = MOV32rm;
		}
		else if (rc == GR16RegisterClass)
		{
			opc = MOV16rm;
		}
		else if (rc == GR8RegisterClass)
		{
			// Copying to or from a physical H register on x86-64 requires a NOREX
			// move.  Otherwise use a normal move.
			if (isHReg(DestReg) && tm.getSubtarget().is64Bit())
				opc = MOV8rm_NOREX;
			else
				opc = MOV8rm;
		}
		else if (rc == GR64_ABCDRegisterClass)
		{
			opc = MOV64rm;
		}
		else if (rc == GR32_ABCDRegisterClass)
		{
			opc = MOV32rm;
		}
		else if (rc == GR16_ABCDRegisterClass)
		{
			opc = MOV16rm;
		}
		else if (rc == GR8_ABCD_LRegisterClass)
		{
			opc = MOV8rm;
		}
		else if (rc == GR8_ABCD_HRegisterClass)
		{
			if (tm.getSubtarget().is64Bit())
				opc = MOV8rm_NOREX;
			else
				opc = MOV8rm;
		}
		else if (rc == GR64_NOREXRegisterClass || rc == GR64_NOREX_NOSPRegisterClass)
		{
			opc = MOV64rm;
		}
		else if (rc == GR32_NOREXRegisterClass)
		{
			opc = MOV32rm;
		}
		else if (rc == GR16_NOREXRegisterClass)
		{
			opc = MOV16rm;
		}
		else if (rc == GR8_NOREXRegisterClass)
		{
			opc = MOV8rm;
		}
		else if (rc == RFP80RegisterClass)
		{
			opc = LD_Fp80m;
		}
		else if (rc == RFP64RegisterClass)
		{
			opc = LD_Fp64m;
		}
		else if (rc == RFP32RegisterClass)
		{
			opc = LD_Fp32m;
		}
		else if (rc == FR32RegisterClass)
		{
			opc = MOVSSrm;
		}
		else if (rc == FR64RegisterClass)
		{
			opc = MOVSDrm;
		}
		else if (rc == VR128RegisterClass)
		{
			// If stack is realigned we can use aligned loads.
			opc = isStackAligned ? MOVAPSrm : MOVUPSrm;
		}
		else if (rc == VR64RegisterClass)
		{
			opc = MMX_MOVQ64rm;
		}
		else
		{
			Util.shouldNotReachHere("Unknown regclass");
		}

		return opc;
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
			MachineInstr old)
	{
		MachineInstr newOne = null;
		MachineBasicBlock mbb = old.getParent();
		if (!hasReservedCallFrame(mf))
		{
			// If we have a frame pointer, turn the adjcallstackup instruction into a
			// 'sub ESP, <amt>' and the adjcallstackdown instruction into 'add ESP,
			// <amt>'
			long amount = old.getOperand(0).getImm();
			if (amount != 0)
			{
				int align = mf.getTarget().getFrameInfo().getStackAlignment();
				amount = Util.roundUp(amount, align);

				// stack setup pseudo instrcution.
				if (old.getOpcode() == getCallFrameSetupOpcode())
				{
					newOne = buildMI(tii.get(is64Bit ? SUB64ri32 : SUB32ri),
							stackPtr).
							addReg(stackPtr).
							addImm(amount).getMInstr();
				}
				else
				{
					Util.assertion( old.getOpcode() == getCallFrameDestroyOpcode());

					long calleeAmt = old.getOperand(1).getImm();
					amount -= calleeAmt;
					if (amount != 0)
					{
						int opc = amount < 128 ?
								(is64Bit ? ADD64ri8 : ADD32ri8) :
								(is64Bit ? ADD64ri32 : ADD32ri);
						// stack destroy pseudo instruction.
						newOne = buildMI(tii.get(opc), stackPtr).
								addReg(stackPtr).
								addImm(amount).getMInstr();
					}
				}
				if (newOne != null)
				{
					// The EFLAGS implicit def is dead.
					newOne.getOperand(3).setIsDead(true);

					// Replace the pseudo instruction with a new instruction.
					mbb.insert(old, newOne);
				}
			}
		}
		else if (old.getOpcode() == getCallFrameDestroyOpcode())
		{
			// If we are performing frame pointer elimination and if the callee pops
			// something off the stack pointer, add it back.  We do this until we have
			// more advanced stack pointer tracking ability.
			long calleeAmt = old.getOperand(1).getImm();
			if (calleeAmt != 0)
			{
				int opc = (calleeAmt < 128) ?
						(is64Bit ? SUB64ri8 : SUB32ri8) :
						(is64Bit ? SUB64ri32 : SUB32ri);
				newOne = buildMI(tii.get(opc), stackPtr).
						addReg(stackPtr).addImm(calleeAmt).
						getMInstr();

				// The EFLAGS implicit def is dead.
				newOne.getOperand(3).setIsDead(true);
				mbb.insert(old, newOne);
			}
		}
		old.removeFromParent();
	}

	/**
	 * This method is called immediately before the specified functions frame
	 * layout (MF.getFrameInfo()) is finalized.  Once the frame is finalized,
	 * MO_FrameIndex operands are replaced with direct ants.  This method is
	 * optional.
	 */
	@Override public void processFunctionBeforeFrameFinalized(
			MachineFunction mf)
	{
		if (hasFP(mf))
		{
			// creates a stack object for saving EBP.
			int frameIndex = mf.getFrameInfo().createStackObject(4, 4);
			Util.assertion(frameIndex == mf.getFrameInfo().getObjectIndexEnd()					- 1,  "The slot for EBP must be last");

		}
	}

	@Override
    public void eliminateFrameIndex(MachineFunction mf, MachineInstr mi,
			RegScavenger rs)
	{
		int i = 0;
		while (!mi.getOperand(i).isFrameIndex())
		{
			i++;
			Util.assertion(i < mi.getNumOperands(),  "Instr have not frame index operand!");

		}

		int frameIndex = mi.getOperand(i).getIndex();
		int basePtr;
		if (needsStackRealignment(mf))
			basePtr = frameIndex < 0 ? framePtr : stackPtr;
		else
			basePtr = hasFP(mf) ? framePtr : stackPtr;

		mi.getOperand(i).changeToRegister(basePtr, false);
		if (mi.getOperand(i + 3).isImm())
		{
			int offset = getFrameIndexOffset(mf, frameIndex) + (int) mi
					.getOperand(i + 3).getImm();
			mi.getOperand(i + 3).changeToImmediate(offset);
		}
		else
		{
			long offset =
					getFrameIndexOffset(mf, frameIndex) + mi.getOperand(i + 3).getOffset();
			mi.getOperand(i + 3).setOffset(offset);
		}
	}

	@Override
	public BitMap getReservedRegs(MachineFunction mf)
	{
		BitMap reserved = new BitMap(getNumRegs());

		// Set the stack-pointer register and its aliases as reserved.
		reserved.set(RSP);
		reserved.set(ESP);
		reserved.set(SP);
		reserved.set(SPL);

		// Set the frame-pointer register and its aliases as reserved if needed.
		if (hasFP(mf))
		{
			reserved.set(RBP);
			reserved.set(EBP);
			reserved.set(BP);
			reserved.set(BPL);
		}

		// Mark the x87 stack registers as reserved, since they don't behave normally
		// with respect to liveness. We don't fully model the effects of x87 stack
		// pushes and pops after stackification.
		reserved.set(ST0);
		reserved.set(ST1);
		reserved.set(ST2);
		reserved.set(ST3);
		reserved.set(ST4);
		reserved.set(ST5);
		reserved.set(ST6);
		reserved.set(ST7);
		return reserved;
	}

	/**
	 * Return true if the specified function should have a dedicated stack pointer
	 * register. This is true if function has variable sized objects or if frame
	 * pointer elimination is disabled.
	 * <p>
	 * the frame pointer is usually EBP in X86 target machine.
	 *
	 * @param mf
	 * @return
	 */
	public boolean hasFP(MachineFunction mf)
	{
		MachineFrameInfo mfi = mf.getFrameInfo();
		return (DisableFramePointerElim.value || needsStackRealignment(mf)
				|| mfi.hasVarSizedObjects() || mfi.isFrameAddressTaken());
	}

	/**
	 * This method insert prologue code into the function. Such as push callee-saved
	 * registers onto the stack, which automatically adjust the stack pointer.
	 * Adjust the stack pointer to space for local variables. Also emit labels
	 * used by the exception handler to generate the exception handling frames.
	 *
	 * @param mf
	 */
	@Override
    public void emitPrologue(MachineFunction mf)
	{
		MachineBasicBlock mbb = mf.getEntryBlock();
		Function fn = mf.getFunction();
		int mbbi = 0;  // a index position where a new instr will inserts.
		MachineFrameInfo mfi = mf.getFrameInfo();
		MachineInstr mi;
		X86MachineFunctionInfo x86FI = (X86MachineFunctionInfo) mf.getInfo();
		int maxAlign = mfi.getMaxAlignment();
		int stackSize = mfi.getStackSize();
		boolean hasFP = hasFP(mf);
		X86Subtarget subtarget = (X86Subtarget) mf.getTarget().getSubtarget();

		int tailCallReturnAddrDelta = x86FI.getTCReturnAddrDelta();
		if (tailCallReturnAddrDelta < 0)
		{
			x86FI.setCalleeSavedFrameSize(x86FI.getCalleeSavedFrameSize() - tailCallReturnAddrDelta);
		}

		if (is64Bit && !fn.hasFnAttr(Attribute.NoRedZone) &&
                !needsStackRealignment(mf) && !mfi.hasVarSizedObjects()
				&& !mfi.hasCalls() && !subtarget.isTargetWin64())
		{
			int minSize = x86FI.getCalleeSavedFrameSize();
			if (hasFP)
				minSize += slotSize;
			stackSize = Math.max(minSize, stackSize > 128 ? stackSize - 128 : 0);
			mfi.setStackSize(stackSize);
		}
		else if (subtarget.isTargetWin64())
		{
			stackSize += 32;
			mfi.setStackSize(stackSize);
		}

		if (tailCallReturnAddrDelta < 0)
		{
			mi = buildMI(mbb, mbbi, tii.get(is64Bit ? SUB64ri32 : SUB32ri),
					stackPtr).addReg(stackPtr).addImm(-tailCallReturnAddrDelta).getMInstr();
			mi.getOperand(3).setIsDead(true);   // The EFLAGS implicit def is dead.
		}

		// Get the number of bytes to allocate from the FrameInfo.
		int numBytes = 0;
		TargetData td = tm.getTargetData();

		int stackGrowth = (tm.getFrameInfo().getStackGrowDirection()
				== TargetFrameInfo.StackDirection.StackGrowUp ?
				td.getPointerSize() : -td.getPointerSize());

		if (hasFP)
		{
			// get the offset of the stack slot for the %ebp register.
			// Note that: this offset is away from ESP.
			int frameSize = stackSize - slotSize;
			if (needsStackRealignment(mf))
				frameSize = (frameSize + maxAlign - 1) / maxAlign * maxAlign;
			numBytes = frameSize - x86FI.getCalleeSavedFrameSize();
			mfi.setOffsetAdjustment(-numBytes);

			buildMI(mbb, mbbi, tii.get(is64Bit ? PUSH64r : PUSH32r))
					.addReg(framePtr, RegState.Kill);
			buildMI(mbb, mbbi, tii.get(is64Bit ? MOV64rr : MOV32rr), framePtr).addReg(stackPtr);

			// mark the frameptr as live-in in every block excepts the entry.
			for (int i = 1, e = mf.getNumBlockIDs(); i < e; i++)
			{
				mf.getBasicBlocks().get(i).addLiveIn(framePtr);

				// realign stack.
				if (needsStackRealignment(mf))
				{
					mi = buildMI(mbb, mbbi,
							tii.get(is64Bit ? AND64ri32 : AND32ri), stackPtr)
							.addReg(stackPtr).addImm(-maxAlign).getMInstr();
					mi.getOperand(3).setIsDead(true);
				}
			}
		}
		else
		{
			numBytes = stackSize - x86FI.getCalleeSavedFrameSize();
		}

		boolean pushedRegs = false;
		int stackOffset = 2 * stackGrowth;
		while (mbbi < mbb.size() && mbb.getInstAt(mbbi).getOpcode() == PUSH32r
				|| mbb.getInstAt(mbbi).getOpcode() == PUSH64r)
		{
			pushedRegs = true;
			++mbbi;
		}
		if (numBytes >= 4096 && subtarget.isTargetCygMing())
		{
			boolean isEAXLive = false;
			for (Pair<Integer, Integer> intPair : mf.getMachineRegisterInfo().getLiveIns())
			{
				int reg = intPair.first;
				isEAXLive = reg == EAX || reg == AX || reg == AH || reg == AL;
			}
			if (!isEAXLive)
			{
				buildMI(mbb, mbbi, tii.get(MOV32ri), EAX).addImm(numBytes);
				buildMI(mbb, mbbi, tii.get(CALLpcrel32))
						.addExternalSymbol("_alloca", 0, 0);
			}
			else
			{
				buildMI(mbb, mbbi, tii.get(PUSH32r)).addReg(EAX, RegState.Kill);

				buildMI(mbb, mbbi, tii.get(MOV32ri), EAX).addImm(numBytes - 4);
				buildMI(mbb, mbbi, tii.get(CALLpcrel32))
						.addExternalSymbol("_alloca", 0, 0);

				mi = addRegOffset(buildMI(tii.get(MOV32rm), EAX), stackPtr,
						false, numBytes - 4).getMInstr();
				mbb.insert(mbbi, mi);
			}
		}
		else if (numBytes != 0)
		{
			OutParamWrapper<Integer> x = new OutParamWrapper<>(mbbi);
			numBytes -= mergeSPUpdates(mbb, x, stackPtr, true);
			mbbi = x.get();
			x = new OutParamWrapper<>(numBytes);
			mergeSPUpdatesDown(mbb, new OutParamWrapper<>(mbbi), stackPtr, x);
			numBytes = x.get();

			if (numBytes != 0)
				emitSPUpdate(mbb, mbbi, stackPtr, -numBytes, is64Bit, tii);
		}
	}

	private int mergeSPUpdates(
			MachineBasicBlock mbb,
			OutParamWrapper<Integer> mbbi,
			int stackPtr,
			boolean doMergeWithPrevious)
	{
		if ((doMergeWithPrevious && mbbi.get() == 0) ||
				(!doMergeWithPrevious && mbbi.get() == mbb.size()))
		{
			return 0;
		}

		int prev = doMergeWithPrevious ? mbbi.get()-1 : mbbi.get();
		int next = doMergeWithPrevious ? -1 : mbbi.get()+1;
		int opc = mbb.getInstAt(prev).getOpcode();
		int offset = 0;
		if ((opc== ADD64ri32 || opc == ADD64ri8 ||
				opc == ADD32ri || opc == ADD32ri8) &&
				mbb.getInstAt(prev).getOperand(0).getReg() == stackPtr)
		{
			offset += mbb.getInstAt(prev).getOperand(2).getImm();
			mbb.remove(prev);
			if (!doMergeWithPrevious)
				mbbi.set(next);
		}
		else if ((opc == SUB64ri32 || opc == SUB64ri8 ||
		opc == SUB32ri || opc == SUB32ri8) && mbb.getInstAt(prev).getOperand(0)
				.getReg() == stackPtr)

		{
			offset -= mbb.getInstAt(prev).getOperand(2).getImm();
			mbb.remove(prev);
			if (!doMergeWithPrevious)
				mbbi.set(next);
		}
		return offset;
	}

	private void mergeSPUpdatesDown(
			MachineBasicBlock mbb,
			OutParamWrapper<Integer> mbbi,
			int stackPtr,
			OutParamWrapper<Integer> numBytes)
	{
	}

	private void emitSPUpdate(MachineBasicBlock mbb,
			int mbbi,
			int stackPtr, int numBytes, boolean is64Bit,
			TargetInstrInfo tii)
	{
		boolean isSub = numBytes < 0;
		long offset = isSub ? -numBytes : numBytes;
		int opc = isSub
				? ((offset < 128) ?
			(is64Bit ? SUB64ri8 : SUB32ri8) :
			(is64Bit ? SUB64ri32 : SUB32ri))
		: ((offset < 128) ?
			(is64Bit ? ADD64ri8 : ADD32ri8) :
			(is64Bit ? ADD64ri32 : ADD32ri));
		long chunk = (1L << 31) - 1;
		while (offset != 0)
		{
			long thisVal = (offset > chunk) ? chunk : offset;
			MachineInstr mi = buildMI(mbb, mbbi,
					tii.get(opc), stackPtr).addReg(stackPtr)
					.addImm(thisVal).getMInstr();
			mi.getOperand(3).setIsDead(true);
			offset -= thisVal;
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
		Util.assertion(mbb.getInstAt(mbbi).getOpcode() == RET, "Can only insert epilogue code into returning blocks");


		if (hasFP(mf))
		{
			// get the offset of the stack slot for the %ebp register.
			// which is guaranteed to be the last slot by processFunctionBeforeFrameFinalized().
			int ebpOffset = mfi.getObjectOffset(mfi.getObjectIndexEnd()-1)+4;
			// mov %ebp, %esp.
			mi = buildMI(mbb, mbbi++, tii.get(is64Bit ? MOV64rr : MOV32rr), stackPtr).
					addReg(EBP, RegState.Kill).getMInstr();
			mi.getOperand(3).setIsDead(true);

			// mov offset(%esp), %ebp.
			mi = buildMI(mbb, mbbi, tii.get(is64Bit ? MOV64rm : MOV32rm), framePtr).
					addReg(stackPtr).addImm(ebpOffset).getMInstr();
		}
		else
		{
			// get the number of bytes allocated from the frameInfo.
			int numBytes = mfi.getStackSize();
			if (numBytes!=0)
			{
				// addjust stack getNumOfSubLoop: %esp += numBytes (up)
				int opc = numBytes < 128 ?
						is64Bit ? ADD64ri8 : ADD32ri8 :
						is64Bit ? AND64ri32 : ADD32ri;
				mi = buildMI(mbb, mbbi, tii.get(opc), stackPtr).
						addReg(stackPtr, RegState.Kill).
						addImm(numBytes).getMInstr();
				mi.getOperand(3).setIsDead(true);
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
		Util.assertion(false, "Should not reaching here");
		return null;
	}
	/**
	 * Checks if the specified machine instr is a move instr or not.
	 * if it is, return true and store the srcReg, destReg, srcSubReg,
	 * destSubReg into regs in the mentioned order.
	 * @param mi
	 * @param regs
	 * @return
	 */
	@Override
	public boolean isMoveInstr(MachineInstr mi, int[] regs)
	{
		switch (mi.getOpcode())
		{
			default:
				return false;
			case X86GenInstrNames.MOV8rr:
			case X86GenInstrNames.MOV8rr_NOREX:
			case X86GenInstrNames.MOV16rr:
			case X86GenInstrNames.MOV32rr:
			case X86GenInstrNames.MOV64rr:
			case X86GenInstrNames.MOVSSrr:
			case X86GenInstrNames.MOVSDrr:

				// FP Stack register class copies
			case X86GenInstrNames.MOV_Fp3232:
			case X86GenInstrNames.MOV_Fp6464:
			case X86GenInstrNames.MOV_Fp8080:
			case X86GenInstrNames.MOV_Fp3264:
			case X86GenInstrNames.MOV_Fp3280:
			case X86GenInstrNames.MOV_Fp6432:
			case X86GenInstrNames.MOV_Fp8032:

			case X86GenInstrNames.FsMOVAPSrr:
			case X86GenInstrNames.FsMOVAPDrr:
			case X86GenInstrNames.MOVAPSrr:
			case X86GenInstrNames.MOVAPDrr:
			case X86GenInstrNames.MOVDQArr:
			case X86GenInstrNames.MOVSS2PSrr:
			case X86GenInstrNames.MOVSD2PDrr:
			case X86GenInstrNames.MOVPS2SSrr:
			case X86GenInstrNames.MOVPD2SDrr:
			case X86GenInstrNames.MMX_MOVQ64rr:
				Util.assertion(mi.getNumOperands() >= 2 &&						mi.getOperand(0).isRegister() &&
						mi.getOperand(1).isRegister(), 
						"invalid register-register move instruction");

				regs[0] = mi.getOperand(1).getReg();
				regs[1] = mi.getOperand(0).getReg();
				regs[2] = mi.getOperand(1).getSubReg();
				regs[3] = mi.getOperand(0).getSubReg();
				return true;
		}
	}
}
