package backend.codegen;

import backend.target.TargetMachine;
import backend.value.ConstantFP;
import backend.value.GlobalValue;
import tools.Util;

import java.io.PrintStream;

import static backend.codegen.MachineOperand.MachineOperandType.*;

/**
 * Purpose:
 *   Representation of each machine instruction operand.
 *   This class is designed so that you can allocate a vector of operands
 *   first and initialize each one later.
 *
 *   E.g, for this VM instruction:
 *		ptr = alloca type, numElements
 *   we generate 2 machine instructions on the SPARC:
 *
 *		mul Constant, Numelements -> Reg
 *		add %sp, Reg -> Ptr
 *
 *   Each instruction has 3 operands, listed above.  Of those:
 *   -	Reg, NumElements, and Ptr are of operand type MO_Register.
 *   -	Constant is of operand type MO_SignExtendedImmed on the SPARC.
 *
 *   For the register operands, the virtual register type is as follows:
 *
 *   -  Reg will be of virtual register type MO_MInstrVirtualReg.  The field
 *	MachineInstr* minstr will point to the instruction that computes reg.
 *
 *   -	%sp will be of virtual register type MO_MachineReg.
 *	The field regNum identifies the machine register.
 *
 *   -	NumElements will be of virtual register type MO_VirtualReg.
 *	The field Value value identifies the value.
 *
 *   -	Ptr will also be of virtual register type MO_VirtualReg.
 *	Again, the field Value value identifies the value.
 *
 * @author Xlous.zeng
 * @version 0.1
 */

public class MachineOperand
{
    public interface RegState
	{
		int Define = 0x2;             /// This machine operand is only written by the instruction
		int Implicit = 0x4;
		int Kill = 0x8;
		int Dead = 0x10;
		int Undef = 0x20;
		int EarlyClobber = 0x40;
		int ImplicitDefine = Implicit | Define;
		int ImplicitKill = Implicit | Kill;
	}

	public enum MachineOperandType
	{
		MO_Register,        // register for *value
		MO_Immediate,
		MO_FPImmediate,
		MO_MachineBasicBlock,       // MachineBasicBlock reference
		MO_FrameIndex,              // Abstract Stack Frame Index
		MO_ConstantPoolIndex,       // Address of indexed Constant in Constant Pool
		MO_JumpTableIndex,
		MO_ExternalSymbol,          // Name of external global symbol
		MO_GlobalAddress,           // Address of a global value
	}

	// Bit fields of the flags variable used for different operand properties
	interface Kind
	{
		int DEFFLAG = 0x01;       // this is a def but not a use of the operand
		int DEFUSEFLAG = 0x02;       // this is both a def and a use
		int HIFLAG32 = 0x04;       // operand is %hi32(value_or_immedVal)
		int LOFLAG32 = 0x08;       // operand is %lo32(value_or_immedVal)
		int HIFLAG64 = 0x10;       // operand is %hi64(value_or_immedVal)
		int LOFLAG64 = 0x20;       // operand is %lo64(value_or_immedVal)
		int PCRELATIVE = 0x40;     // Operand is relative to PC, not a global address

		int USEDEFMASK = 0x03;
	}

	// ConstantVal for a non-address immediate.
	// Virtual register for an SSA operand,
	//   including hidden operands required for
	//   the generated machine code.
	// LLVM global for MO_GlobalAddress.

	private long immedVal;        // Constant value for an explicit ant
	private MachineBasicBlock mbb;     // For MO_MachineBasicBlock type
	// For constant FP.
	private ConstantFP cfp;

	private class RegOp
	{
		int regNo;
		MachineOperand prev;   // Access list for register.
		MachineOperand next;
	}
	// For register operand.
	private RegOp reg;

	private class Val
	{
		int index;
		String symbolName;
		GlobalValue gv;
	}
	// For Offset and an object identifier. this represent the object as with
	// an optional offset from it.
	private class OffsetedInfo
	{
		Val val;
		long offset;
	}

	private OffsetedInfo offsetedInfo;


	private String SymbolName;    // For MO_ExternalSymbol type

	private MachineOperandType opKind;  // Pack into 8 bits efficiently after flags.

	private int subReg;

	private int targetFlags;

	private boolean isDef;

	private boolean isImp;

	private boolean isKill;

	private boolean isDead;

	private boolean isUndef;

	/**
	 * True if this MO_Register 'def' operand is written to
	 * by the MachineInstr before all input registers are read.  This is used to
	 * model the GCC inline asm '&' constraint modifier.
	 */
	private boolean isEarlyClobber;

	/**
	 * The machine instruction in which this machine operand is embedded.
	 */
	private MachineInstr parentMI;

	// will be set for a value after reg allocation
	private MachineOperand(MachineOperandType k)
	{
		opKind = k;
		parentMI = null;
	}

	// Accessor methods.  Caller is responsible for checking the
	// operand type before invoking the corresponding accessor.
	//
	public MachineOperandType getType()
	{
		return opKind;
	}

	public void setOpKind(MachineOperandType newTy)
	{
		opKind = newTy;
	}

	public int getTargetFlags()
	{
		return targetFlags;
	}

	public void setTargetFlags(int targetFlags)
	{
		this.targetFlags = targetFlags;
	}

	public void addTargetFlag(int f)
	{
		targetFlags |= f;
	}

	public MachineInstr getParent()
	{
		return parentMI;
	}

	public void print(PrintStream os)
	{
		print(os, null);
	}

	public void print(PrintStream os, TargetMachine tm)
	{
		// TODO: 17-7-16
	}


	public boolean isRegister()
	{
		return opKind == MO_Register;
	}

	public boolean isMBB()
	{
		return opKind == MO_MachineBasicBlock;
	}

	public boolean isImm()
	{
		return opKind == MO_Immediate;
	}

	public boolean isFPImm()
	{
		return opKind == MO_FPImmediate;
	}

	public boolean isFrameIndex()
	{
		return opKind == MO_FrameIndex;
	}

	public boolean isConstantPoolIndex()
	{
		return opKind == MO_ConstantPoolIndex;
	}

	public boolean isGlobalAddress()
	{
		return opKind == MO_GlobalAddress;
	}

	public boolean isExternalSymbol()
	{
		return opKind == MO_ExternalSymbol;
	}

	public boolean isJumpTableIndex()
	{
		return opKind == MO_JumpTableIndex;
	}

	public int getReg()
	{
		assert isRegister():"This is not a register operand!";
		return reg.regNo;
	}

	public int getSubReg()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return subReg;
	}

	public boolean isUse()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return !isDef;
	}

	public boolean isDef()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return isDef;
	}

	public boolean isImplicit()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return isImp;
	}

	public boolean isDead()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return isDead;
	}

	public boolean isKill()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return isKill;
	}

	public boolean isUndef()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return isUndef;
	}

	public boolean isEarlyClobber()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return isEarlyClobber;
	}

	public MachineOperand getNextOperandForReg()
	{
		assert isRegister():"Wrong MachineOperand accessor";
		return reg.next;
	}

	public void setReg(int reg)
	{
		if (getReg() == reg)
			return;

		// Otherwise, we have to change the register.  If this operand is embedded
		// into a machine function, we need to update the old and new register's
		// use/def lists.
		MachineInstr mi = getParent();
		if (mi != null)
		{
			MachineBasicBlock mbb = mi.getParent();
			if (mbb != null)
			{
				MachineFunction mf = mbb.getParent();
				if (mf != null)
				{
					removeRegOperandFromRegInfo();
					this.reg.regNo = reg;
					addRegOperandToRegInfo(mf.getMachineRegisterInfo());
					return;
				}
			}
		}

		// Otherwise, just change the register, no problem.  :)
		this.reg.regNo = reg;
	}

	public void setSubreg(int subreg)
	{
		assert isRegister():"Wrong MachineOperand accessor";
		this.subReg = subreg;
	}

	public void setIsUse(boolean val)
	{
		assert isRegister():"Wrong MachineOperand accessor";
		isDef = !val;
	}

	public void setIsDef(boolean val)
	{
		assert isRegister():"Wrong MachineOperand accessor";
		isDef = val;
	}

	public void setImplicit(boolean val)
	{
		assert isRegister():"Wrong MachineOperand accessor";
		isImp = val;
	}

	public void setIsKill(boolean val)
	{
		assert isRegister():"Wrong MachineOperand accessor";
		isKill = val;
	}

	public void setIsDead(boolean val)
	{
		assert isRegister():"Wrong MachineOperand accessor";
		isDead = val;
	}

	public void setIsUndef(boolean val)
	{
		assert isRegister():"Wrong MachineOperand accessor";
		isUndef = val;
	}

	public void setIsEarlyClobber(boolean val)
	{
		assert isRegister():"Wrong MachineOperand accessor";
		isEarlyClobber = val;
	}

	public long getImm()
	{
		assert isImm() : "Wrong MachineOperand accessor";
		return immedVal;
	}

	public ConstantFP getFPImm()
	{
		assert isFPImm() : "Wrong MachineOperand accessor";
		return cfp;
	}

	public MachineBasicBlock getMBB()
	{
		assert isMBB() : "Can't get mbb in non-mbb operand!";
		return mbb;
	}

	public int getIndex()
	{
		assert isFrameIndex() || isConstantPoolIndex() || isJumpTableIndex()
				: "Wrong MachineOperand accessor";
		return offsetedInfo.val.index;
	}

	public long getOffset()
	{
		assert isGlobalAddress() || isExternalSymbol() || isConstantPoolIndex()
				: "Wrong MachineOperand accessor";
		return offsetedInfo.offset;
	}

	public GlobalValue getGlobal()
	{
		assert isGlobalAddress() : "Wrong MachineOperand accessor";
		return offsetedInfo.val.gv;
	}

	public String getSymbolName()
	{
		assert isExternalSymbol() : "Wrong MachineOperand accessor";
		return offsetedInfo.val.symbolName;
	}

	public void setMbb(MachineBasicBlock mbb)
	{
		this.mbb = mbb;
	}

	public void setImm(long imm)
	{
		assert isImm(): "Wrong MachineOperand accessor";
		immedVal = imm;
	}

	public void setOffset(long offset)
	{
		assert isGlobalAddress() || isExternalSymbol() || isConstantPoolIndex()
				: "Wrong MachineOperand accessor";
		offsetedInfo.offset = offset;
	}

	public void setIndex(int idx)
	{
		assert isFrameIndex() || isConstantPoolIndex() || isJumpTableIndex()
				: "Wrong MachineOperand accessor";
		offsetedInfo.val.index = idx;
	}

	public void setMBB(MachineBasicBlock mbb)
	{
		assert isMBB() : "Wrong MachineOperand accessor";
		this.mbb = mbb;
	}

	public MachineInstr getParentMI()
	{
		return parentMI;
	}

	public void setParentMI(MachineInstr parentMI)
	{
		this.parentMI = parentMI;
	}

	/**
	 * Return true if this operand is identical to the specified
	 * operand. Note: This method ignores isKill and isDead properties.
	 * @param other
	 * @return
	 */
	public boolean isIdenticalTo(MachineOperand other)
	{
		if (getType() != other.getType()
				|| getTargetFlags() != other.getTargetFlags())
			return false;

		switch (getType())
		{
			default:
				Util.shouldNotReachHere("Unrecognized operand type");
				return false;
			case MO_Register:
				return getReg() == other.getReg() && isDef() == other.isDef() &&
						getSubReg() == other.getSubReg();
			case MO_Immediate:
				return getImm() == other.getImm();
			case MO_FPImmediate:
				return getFPImm().equals(other.getFPImm());
			case MO_MachineBasicBlock:
				return getMBB().equals(other.getMBB());
			case MO_FrameIndex:
			case MO_JumpTableIndex:
				return getIndex() == other.getIndex();
			case MO_ConstantPoolIndex:
				return getIndex() == other.getIndex() &&
						getOffset() == other.getOffset();
			case MO_GlobalAddress:
				return getGlobal() == other.getGlobal() &&
						getOffset() == other.getOffset();
			case MO_ExternalSymbol:
				return getSymbolName().equals(other.getSymbolName()) &&
						getOffset() == other.getOffset();
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (this ==obj) return true;
		if (getClass() != obj.getClass())
			return false;

		return isIdenticalTo((MachineOperand)obj);
	}

	/**
	 * Replace this operand with a new immediate operand of
	 * the specified value.  If an operand is known to be an immediate already,
	 * the setImm method should be used.
	 * @param immVal
	 */
	public void changeToImmediate(long immVal)
	{
		if (isRegister() && getParent() != null && getParent().getParent() != null
				&& getParent().getParent().getParent() != null)
			removeRegOperandFromRegInfo();
		opKind = MO_Immediate;
		immedVal = immVal;
	}

	public void removeRegOperandFromRegInfo()
	{
		// Unlink this reg operand from reg def-use linked list.
		assert isOnRegUseList() :"Reg operand is not on a use list";
		MachineOperand nextOp = reg.next;
		reg.prev.reg.next = nextOp;
		if (nextOp != null)
		{
			assert nextOp.getReg() == getReg():"Corrupt reg use/def chain!";
			nextOp.reg.prev = reg.prev;
		}
		reg.prev = null;
		reg.next = null;
	}

	public boolean isOnRegUseList()
	{
		assert isRegister();
		return reg.prev != null;
	}

	public void changeToRegister(int reg,
			boolean isDef)
	{
		changeToRegister(reg, isDef, false, false, false, false);
	}

	/**
	 * Replace this operand with a new register operand of
	 * the specified value.  If an operand is known to be an register already,
	 * the setReg method should be used.
	 * @param reg
	 * @param isDef
	 * @param isImp
	 * @param isKill
	 * @param isDead
	 * @param isUndef
	 */
	public void changeToRegister(int reg,
			boolean isDef,
			boolean isImp,
			boolean isKill,
			boolean isDead,
			boolean isUndef)
	{
		if(isRegister())
		{
			assert !isEarlyClobber;
			setReg(reg);
		}
		else
		{
			opKind = MO_Register;
			this.reg.regNo = reg;

			MachineFunction mf;
			if (getParent() != null)
			{
				if (parentMI.getParent() != null)
					if ((mf = parentMI.getParent().getParent()) != null)
						addRegOperandToRegInfo(mf.getMachineRegisterInfo());
			}
		}

		this.isDef = isDef;
		this.isImp = isImp;
		this.isKill = isKill;
		this.isDead = isDead;
		this.isUndef = isUndef;
		this.isEarlyClobber = false;
		this.subReg = 0;
	}

	/**
	 * Add this register operand to the specified
	 * MachineRegisterInfo.  If it is null, then the next/prev fields should be
	 * explicitly nulled out.
	 * @param regInfo
	 */
	public void addRegOperandToRegInfo(MachineRegisterInfo regInfo)
	{
		assert isRegister():"Can only add reg operand to use lists";
		// FIXME 2017.7.16
		if (regInfo == null)
		{
			reg.prev = null;
			reg.next = null;
			return;
		}

		MachineOperand head = regInfo.getRegUseDefListHead(getReg());

		if (head != null && head.isDef())
		{
			head = head.reg.next;
		}

		reg.next = head;
		if (reg.next != null)
		{
			assert getReg() == reg.next.getReg():"Different regs on the same list!";
			reg.next.reg.prev = reg.next;
		}
		reg.prev = head;
		head = this;
	}

	public static MachineOperand createImm(long val)
	{
		MachineOperand op = new MachineOperand(MO_Immediate);
		op.setImm(val);
		return op;
	}

	public static MachineOperand createFPImm(ConstantFP fp)
	{
		MachineOperand op = new MachineOperand(MO_FPImmediate);
		op.cfp = fp;
		return op;
	}

	public static MachineOperand createReg(int reg,
			boolean isDef,
			boolean isImp)
	{
		return createReg(reg, isDef, isImp, false, false, false, false, 0);
	}

	public static MachineOperand createReg(int reg,
			boolean isDef,
			boolean isImp,
			boolean isKill,
			boolean isDead,
			boolean isUndef,
			boolean isEarlyClobber,
			int subreg)
	{
		MachineOperand op = new MachineOperand(MO_Register);
		op.isDef = isDef;
		op.isImp = isImp;
		op.isKill = isKill;
		op.isDead = isDead;
		op.isUndef = op.isUndef;
		op.isEarlyClobber = isEarlyClobber;
		op.subReg = subreg;
		return op;
	}

	public static MachineOperand createMBB(MachineBasicBlock mbb, int targetFlags)
	{
		MachineOperand op = new MachineOperand(MO_MachineBasicBlock);
		op.setMBB(mbb);
		op.setTargetFlags(targetFlags);
		return op;
	}


	public static MachineOperand createFrameIndex(int idx)
	{
		MachineOperand op = new MachineOperand(MO_FrameIndex);
		op.setIndex(idx);
		return op;
	}

	public static MachineOperand createConstantPoolIndex(int idx, int offset, int targetFlags)
	{
		MachineOperand op = new MachineOperand(MO_ConstantPoolIndex);
		op.setIndex(idx);
		op.setOffset(offset);
		op.setTargetFlags(targetFlags);
		return op;
	}

	public static MachineOperand createJumpTableIndex(int idx, int targetFlags)
	{
		MachineOperand op = new MachineOperand(MO_JumpTableIndex);
		op.setIndex(idx);
		op.setTargetFlags(targetFlags);
		return op;
	}

	public static MachineOperand createGlobalAddress(GlobalValue gv,
			long offset,
			int targetFlags)
	{
		MachineOperand op = new MachineOperand(MO_GlobalAddress);
		op.offsetedInfo.val.gv = gv;
		op.setOffset(offset);
		op.setTargetFlags(targetFlags);
		return op;
	}

	public static MachineOperand createExternalSymbol(String symName,
			long offset,
			int targetFlags)
	{
		MachineOperand op = new MachineOperand(MO_ExternalSymbol);
		op.offsetedInfo.val.symbolName = symName;
		op.setOffset(offset);
		op.setTargetFlags(targetFlags);
		return op;
	}
}
