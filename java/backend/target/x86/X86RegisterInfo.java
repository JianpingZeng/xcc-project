package backend.target.x86;

import backend.codegen.*;
import backend.codegen.MachineOperand.RegState;
import backend.target.TargetFrameInfo;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterClass;
import backend.type.Type;
import tools.BitMap;
import tools.OutParamWrapper;
import tools.Util;

import java.util.ArrayList;

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
						ADJCALLSTACKDOWN32,
				tm.getSubtarget().is64Bit() ?
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
		switch(regNo) 
		{
			case RAX: case EAX: case AX: case AL: return N86.EAX;
			case RCX: case ECX: case CX: case CL: return N86.ECX;
			case RDX: case EDX: case DX: case DL: return N86.EDX;
			case RBX: case EBX: case BX: case BL: return N86.EBX;
			case RSP: case ESP: case SP: case SPL: case AH:
				return N86.ESP;
			case RBP: case EBP: case BP: case BPL: case CH:
				return N86.EBP;
			case RSI: case ESI: case SI: case SIL: case DH:
				return N86.ESI;
			case RDI: case EDI: case DI: case DIL: case BH:
				return N86.EDI;

			case R8:  case R8D:  case R8W:  case R8B:
				return N86.EAX;
			case R9:  case R9D:  case R9W:  case R9B:
				return N86.ECX;
			case R10: case R10D: case R10W: case R10B:
				return N86.EDX;
			case R11: case R11D: case R11W: case R11B:
				return N86.EBX;
			case R12: case R12D: case R12W: case R12B:
				return N86.ESP;
			case R13: case R13D: case R13W: case R13B:
				return N86.EBP;
			case R14: case R14D: case R14W: case R14B:
				return N86.ESI;
			case R15: case R15D: case R15W: case R15B:
				return N86.EDI;

			case ST0: case ST1: case ST2: case ST3:
			case ST4: case ST5: case ST6: case ST7:
				return regNo-ST0;

			case XMM0: case XMM8: case MM0:
				return 0;
			case XMM1: case XMM9: case MM1:
				return 1;
			case XMM2: case XMM10: case MM2:
				return 2;
			case XMM3: case XMM11: case MM3:
				return 3;
			case XMM4: case XMM12: case MM4:
				return 4;
			case XMM5: case XMM13: case MM5:
				return 5;
			case XMM6: case XMM14: case MM6:
				return 6;
			case XMM7: case XMM15: case MM7:
				return 7;

			default:
				assert isVirtualRegister(regNo) : "Undefined physical register!";
				Util.shouldNotReachHere("Register allocator hasn't allocated reg correctly yet!");
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
	 * @param regNum
	 * @param isEH
	 * @return
	 */
	public int getDwarfRegNum(int regNum, boolean isEH)
	{
		assert false:"Shoult not reaching here!";
		return 0;
	}

	public TargetRegisterClass getMatchingSuperRegClass(TargetRegisterClass a,
			TargetRegisterClass b,
			int subIdx)
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
                else if (b == GR8_ABCD_LRegisterClass
                        || b == GR8_ABCD_HRegisterClass)
                {
                    if (a == GR64RegisterClass || a == GR64_ABCDRegisterClass
                            || a == GR64_NOREXRegisterClass
                            || a == GR64_NOSPRegisterClass
                            || a == GR64_NOREX_NOSPRegisterClass)
                        return GR64_ABCDRegisterClass;
                    else if (a == GR32RegisterClass
                            || a == GR32_ABCDRegisterClass
                            || a == GR32_NOREXRegisterClass
                            || a == GR32_NOSPRegisterClass)
                        return GR32_ABCDRegisterClass;
                    else if (a == GR16RegisterClass
                            || a == GR16_ABCDRegisterClass
                            || a == GR16_NOREXRegisterClass)
                        return GR16_ABCDRegisterClass;
                }
                else if (b == GR8_NOREXRegisterClass)
                {
                    if (a == GR64RegisterClass || a == GR64_NOREXRegisterClass
                            || a == GR64_NOSPRegisterClass
                            || a == GR64_NOREX_NOSPRegisterClass)
                        return GR64_NOREXRegisterClass;
                    else if (a == GR64_ABCDRegisterClass)
                        return GR64_ABCDRegisterClass;
                    else if (a == GR32RegisterClass
                            || a == GR32_NOREXRegisterClass
                            || a == GR32_NOSPRegisterClass)
                        return GR32_NOREXRegisterClass;
                    else if (a == GR32_ABCDRegisterClass)
                        return GR32_ABCDRegisterClass;
                    else if (a == GR16RegisterClass
                            || a == GR16_NOREXRegisterClass)
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
                            || a == GR64_NOREXRegisterClass
                            || a == GR64_NOSPRegisterClass
                            || a == GR64_NOREX_NOSPRegisterClass)
                        return GR64_ABCDRegisterClass;
                    else if (a == GR32RegisterClass
                            || a == GR32_ABCDRegisterClass
                            || a == GR32_NOREXRegisterClass
                            || a == GR32_NOSPRegisterClass)
                        return GR32_ABCDRegisterClass;
                    else if (a == GR16RegisterClass
                            || a == GR16_ABCDRegisterClass
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
                            || a == GR64_NOREXRegisterClass
                            || a == GR64_NOSPRegisterClass
                            || a == GR64_NOREX_NOSPRegisterClass)
                        return GR64_ABCDRegisterClass;
                    else if (a == GR32RegisterClass
                            || a == GR32_ABCDRegisterClass
                            || a == GR32_NOREXRegisterClass
                            || a == GR32_NOSPRegisterClass)
                        return GR32_ABCDRegisterClass;
                }
                else if (b == GR16_NOREXRegisterClass)
                {
                    if (a == GR64RegisterClass || a == GR64_NOREXRegisterClass
                            || a == GR64_NOSPRegisterClass
                            || a == GR64_NOREX_NOSPRegisterClass)
                        return GR64_NOREXRegisterClass;
                    else if (a == GR64_ABCDRegisterClass)
                        return GR64_ABCDRegisterClass;
                    else if (a == GR32RegisterClass
                            || a == GR32_NOREXRegisterClass
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
                            || a == GR64_NOREXRegisterClass
                            || a == GR64_NOSPRegisterClass
                            || a == GR64_NOREX_NOSPRegisterClass)
                        return GR64_ABCDRegisterClass;
                }
                else if (b == GR32_NOREXRegisterClass)
                {
                    if (a == GR64RegisterClass || a == GR64_NOREXRegisterClass
                            || a == GR64_NOSPRegisterClass
                            || a == GR64_NOREX_NOSPRegisterClass)
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

	@Override
	public TargetRegisterClass getPointerRegClass(int kind)
	{
		switch (kind)
		{
			default:Util.shouldNotReachHere("Unexpected kind in getPointerRegClass()!");
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

	@Override
	public int[] getCalleeSavedRegs(MachineFunction mf)
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

	@Override
	public boolean needsStackRealignment(MachineFunction mf)
	{
		MachineFrameInfo mfi = mf.getFrameInfo();

		return EnableRealignStack.value && mfi.getMaxAlignment() > stackAlign &&
				!mfi.hasVarSizedObjects();
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

		for (int i = mfi.getObjectIndexBegin(), e = mfi.getObjectIndexEnd(); i != e; i++)
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

		int maxAlign = Math.max(mfi.getMaxAlignment(), calculateMaxStackAlignment(mfi));

		mfi.setMaxCallFrameSize(maxAlign);

		// TODO: 17-7-20 Should not reaching here
		assert false:"Should not reaching here";
	}

	public void emitCalleeSavedFrameMoves(MachineFunction mf, int labelId,
			int framePtr)
	{
		// TODO: 17-7-20
		assert false:"Should not reaching here";
	}

	@Override
	public int getRARegister()
	{
		return is64Bit? RIP : EIP;
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
				assert (-(offset + stackSize)) % align == 0;
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
        else if (rc == GR64_NOREXRegisterClass
                || rc == GR64_NOREX_NOSPRegisterClass)
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

    private static int getLoadRegOpcode(int DestReg,
            TargetRegisterClass rc,
            boolean isStackAligned,
            X86TargetMachine tm)
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
        else if (rc == GR64_NOREXRegisterClass
                || rc == GR64_NOREX_NOSPRegisterClass)
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
			MachineBasicBlock mbb, int idx)
	{
		MachineInstr newOne = null, old = mbb.getInstAt(idx);
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
					newOne = buildMI(tii.get(is64Bit ? SUB64ri32 : SUB32ri), stackPtr).
							addReg(stackPtr).
							addImm(amount).getMInstr();
				}
				else
				{
					assert old.getOpcode() == getCallFrameDestroyOpcode();

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
					mbb.insert(idx, newOne);
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
				int opc = (calleeAmt < 128 ) ?
						(is64Bit ? SUB64ri8 : SUB32ri8) :
						(is64Bit ? SUB64ri32 : SUB32ri);
				newOne = buildMI(tii.get(opc), stackPtr).
						addReg(stackPtr).addImm(calleeAmt).
						getMInstr();

				// The EFLAGS implicit def is dead.
				newOne.getOperand(3).setIsDead(true);
				mbb.insert(idx, newOne);
			}
		}
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
    public void eliminateFrameIndex(MachineFunction mf, MachineBasicBlock mbb,
            int ii, RegScavenger rs)
    {
	    assert ii == 0:"Unexpected";

	    MachineInstr mi = mbb.getInstAt(ii);
	    int i = 0;
	    while(!mi.getOperand(i).isFrameIndex())
	    {
		    i++;
		    assert i < mi.getNumOperands():"Instr have not frame index operand!";
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
		    int offset = getFrameIndexOffset(mf, frameIndex) +
				    (int) mi.getOperand(i + 3).getImm();
		    mi.getOperand(i + 3).changeToImmediate(offset);
	    }
	    else
	    {
		    long offset = getFrameIndexOffset(mf, frameIndex) +
				    mi.getOperand(i+3).getOffset();
		    mi.getOperand(i+3).setOffset(offset);
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

    @Override
    public int getSubReg(int regNo, int index)
    {
        return 0;
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
	public boolean hasFP(MachineFunction mf)
	{
		MachineFrameInfo mfi = mf.getFrameInfo();
		return (DisableFramePointerElim.value ||
				needsStackRealignment(mf) ||
				mfi.hasVarSizedObjects() ||
				mfi.isFrameAddressTaken());
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
			if (numBytes != 0)
			{
				// adjust stack pointer: %esp -= numBytes.
				mi = buildMI(tii.get(is64Bit? SUB64ri32 : SUB32ri), stackPtr).
						addReg(stackPtr, RegState.Kill).
						addImm(numBytes).
						getMInstr();
				// The EFLAGS implicit def is dead.
				mi.getOperand(3).setIsDead(true);
				mbb.insert(mbbi++, mi);
			}

			// Save %ebp into the properly stack slot.
			// mov %ebp, ebpOffset+numBytes(%esp).
			buildMI(mbb, mbbi++, tii.get(is64Bit ? PUSH64r : PUSH32r)).
					addReg(framePtr, RegState.Kill).getMInstr();

			// Update %ebp with new base value.

			buildMI(mbb, mbbi++, tii.get(is64Bit? MOV64rr:MOV32rr), framePtr).
				addReg(stackPtr).getMInstr();
		}
		else
		{
			// When we have no frame pointer, we reserve argument space for call sites
			// in the function immediately on entry to the current function.  This
			// eliminates the need for add/sub ESP brackets around call sites.
			numBytes += mfi.getMaxCallFrameSize();

			// round the getNumOfSubLoop to a multiple of the alignment.
			int align = mf.getTarget().getFrameInfo().getStackAlignment();
			numBytes = ((numBytes + 4) + align - 1) / align * align - 4;

			// update the frame info to pretend that this is part of stack.
			mfi.setStackSize(numBytes);

			if (numBytes != 0)
			{
				// adjust stack pointer: %esp -= numbetes.
				int opc = numBytes < 128 ?
						is64Bit ? SUB64ri8 : SUB32ri8 :
						is64Bit ? SUB64ri32 : SUB32ri;
				mi = buildMI(mbb, mbbi++, tii.get(opc), stackPtr).
						addReg(stackPtr, RegState.Kill).
						addImm(numBytes).getMInstr();
				mi.getOperand(3).setIsDead(true);
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
		assert mbb.getInstAt(mbbi).getOpcode() == RET
				:"Can only insert epilogue code into returning blocks";

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
		assert false:"Should not reaching here";
		return null;
	}
}
