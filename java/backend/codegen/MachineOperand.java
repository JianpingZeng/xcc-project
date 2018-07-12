package backend.codegen;

import tools.Util;
import tools.FormattedOutputStream;
import backend.support.LLVMContext;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.value.ConstantFP;
import backend.value.GlobalValue;

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

	static class RegOp
	{
		int regNo;
		MachineOperand prev;   // Access list for register.
		MachineOperand next;

		RegOp(int regNo)
        {
            this(regNo, null, null);
        }

        RegOp(int regNo, MachineOperand prev, MachineOperand next)
        {
            this.regNo = regNo;
            this.prev = prev;
            this.next = next;
        }

		public void clear()
		{
			regNo = 0;
			prev = null;
			next = null;
		}
	}
	// For register operand.
	RegOp reg;

	private static class Val
	{
		int index;
		String symbolName;
		GlobalValue gv;

		public Val()
        {
            index = 0;
            symbolName = "";
            gv = null;
        }
	}
	// For Offset and an object identifier. this represent the object as with
	// an optional offset from it.
	private static class OffsetedInfo
	{
		Val val;
		long offset;

		OffsetedInfo()
        {
            val = new Val();
            offset = 0;
        }
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

	public void setParent(MachineInstr parentMI)
	{
		this.parentMI = parentMI;
	}

	public void print(PrintStream os)
	{
		print(os, null);
	}

	public void print(PrintStream os, TargetMachine tm)
	{
		try(FormattedOutputStream out = new FormattedOutputStream(os))
		{
			print(out, tm);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void print(FormattedOutputStream os, TargetMachine tm)
	{
		switch (getType())
		{
			case MO_Register:
			{
				if (getReg() == 0 || TargetRegisterInfo.isVirtualRegister(getReg()))
				{
					os.printf("%%reg%d", getReg());
				}
				else
				{
					// The used register is physical register.
					if (tm == null)
					{
						MachineInstr mi = getParent();
						if (mi != null)
						{
							MachineBasicBlock mbb = mi.getParent();
							if (mbb != null)
							{
								MachineFunction mf = mbb.getParent();
								if (mf != null)
									tm = mf.getTarget();
							}
						}
					}
					if (tm != null)
						os.printf("%%%s", tm.getRegisterInfo().getName(getReg()));
					else
						os.printf("%%mreg%d", getReg());
				}
				if (getSubReg() != 0)
					os.printf(":%d", getSubReg());

				if (isDef() || isKill() || isDead() || isImplicit() || isUndef()
						|| isEarlyClobber())
				{
					os.printf("<");
					boolean needComma = false;
					if (isImplicit())
					{
						if (needComma)
							os.printf(",");
						os.printf(isDef() ? "imp-def" : "imp-use");
						needComma = true;
					}
					else if (isDef())
					{
						if (needComma)
							os.printf(",");
						if (isEarlyClobber())
							os.printf("earlyclobber");
						os.printf("def");
						needComma = true;
					}
					if (isKill() || isDead() || isUndef())
					{
						if (needComma)
							os.printf(",");
						if (isKill())
							os.printf("kill");
						if (isDead())
							os.printf("dead");
						if (isUndef())
						{
							if (isKill() || isDead())
								os.printf(",");
							os.printf("undef");
						}
					}
					os.printf(">");
				}
				break;
			}
			case MO_Immediate:
				os.print(getImm());
				break;
			case MO_FPImmediate:
				if (getFPImm().getType().equals(LLVMContext.FloatTy))
					os.print(getFPImm().getValueAPF().convertToFloat());
				else
					os.print(getFPImm().getValueAPF().convertToDouble());
				break;
			case MO_MachineBasicBlock:
				os.printf("mbb<%s,0x%x>", getMBB().getBasicBlock().getName(),
						getMBB().hashCode());
				break;
			case MO_FrameIndex:
				os.printf("<fi#%d>", getIndex());
				break;
			case MO_ConstantPoolIndex:
				os.printf("<cp#%d", getIndex());
				if (getOffset() != 0)
					os.printf("+%d", getOffset());
				os.printf(">");
				break;
			case MO_JumpTableIndex:
				os.printf("<ji#^d>", getIndex());
				break;
			case MO_GlobalAddress:
				os.printf("<ga:%s", getGlobal().getName());
				if (getOffset() != 0)
					os.printf("+%d", getOffset());
				os.print(">");
				break;
			case MO_ExternalSymbol:
				os.printf("<es:%s", getSymbolName());
				if(getOffset() != 0)
					os.printf("+%d", getOffset());
				os.print(">");
				break;
			default:
				Util.shouldNotReachHere("Unrecognized operand type");
				break;
		}
		int tf = getTargetFlags();
		if (tf != 0)
			os.printf("[TF=%d]", tf);
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
		Util.assertion(isRegister(), "This is not a register operand!");
		return reg.regNo;
	}

	public int getSubReg()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		return subReg;
	}

	public boolean isUse()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		return !isDef;
	}

	public boolean isDef()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		return isDef;
	}

	public boolean isImplicit()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		return isImp;
	}

	public boolean isDead()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		return isDead;
	}

	public boolean isKill()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		return isKill;
	}

	public boolean isUndef()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		return isUndef;
	}

	public boolean isEarlyClobber()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		return isEarlyClobber;
	}

	public MachineOperand getNextOperandForReg()
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
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
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		this.subReg = subreg;
	}

	public void setIsUse(boolean val)
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		isDef = !val;
	}

	public void setIsDef(boolean val)
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		isDef = val;
	}

	public void setImplicit(boolean val)
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		isImp = val;
	}

	public void setIsKill(boolean val)
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		isKill = val;
	}

	public void setIsDead(boolean val)
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		isDead = val;
	}

	public void setIsUndef(boolean val)
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		isUndef = val;
	}

	public void setIsEarlyClobber(boolean val)
	{
		Util.assertion(isRegister(), "Wrong MachineOperand accessor");
		isEarlyClobber = val;
	}

	public long getImm()
	{
		Util.assertion(isImm(),  "Wrong MachineOperand accessor");
		return immedVal;
	}

	public ConstantFP getFPImm()
	{
		Util.assertion(isFPImm(),  "Wrong MachineOperand accessor");
		return cfp;
	}

	public MachineBasicBlock getMBB()
	{
		Util.assertion(isMBB(),  "Can't get mbb in non-mbb operand!");
		return mbb;
	}

	public int getIndex()
	{
		Util.assertion(isFrameIndex() || isConstantPoolIndex() || isJumpTableIndex(),  "Wrong MachineOperand accessor");

		return offsetedInfo.val.index;
	}

	public long getOffset()
	{
		Util.assertion(isGlobalAddress() || isExternalSymbol() || isConstantPoolIndex(),  "Wrong MachineOperand accessor");

		return offsetedInfo.offset;
	}

	public GlobalValue getGlobal()
	{
		Util.assertion(isGlobalAddress(),  "Wrong MachineOperand accessor");
		return offsetedInfo.val.gv;
	}

	public String getSymbolName()
	{
		Util.assertion(isExternalSymbol(),  "Wrong MachineOperand accessor");
		return offsetedInfo.val.symbolName;
	}

	public void setImm(long imm)
	{
		Util.assertion(isImm(),  "Wrong MachineOperand accessor");
		immedVal = imm;
	}

	public void setOffset(long offset)
	{
		Util.assertion(isGlobalAddress() || isExternalSymbol() || isConstantPoolIndex(),  "Wrong MachineOperand accessor");

		offsetedInfo.offset = offset;
	}

	public void setIndex(int idx)
	{
		Util.assertion(isFrameIndex() || isConstantPoolIndex() || isJumpTableIndex(),  "Wrong MachineOperand accessor");

		offsetedInfo.val.index = idx;
	}

	public void setMBB(MachineBasicBlock mbb)
	{
		Util.assertion(isMBB(),  "Wrong MachineOperand accessor");
		this.mbb = mbb;
	}

	/**
	 * Return true if this operand is identical to the specified
	 * operand. Note: This method ignores isDeclare and isDead properties.
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
		Util.assertion(isOnRegUseList(), "Reg operand is not on a use list");
        MachineRegisterInfo regInfo = parentMI.getRegInfo();
        MachineOperand head = regInfo.getRegUseDefListHead(reg.regNo);
        if (head.equals(this))
        {
        	if (head.reg.next != null)
        	    head.reg.next.reg.prev = null;
            regInfo.updateRegUseDefListHead(getReg(), head.reg.next);
        }
        else
        {
            if (reg.prev != null)
            {
                Util.assertion(reg.prev.getReg() == getReg(),  "Corrupt reg use/def chain!");

                reg.prev.reg.next = reg.next;
            }
            if (reg.next != null)
            {
                Util.assertion(reg.next.getReg() == getReg(),  "Corrupt reg use/def chain!");

                reg.next.reg.prev = reg.prev;
            }
        }
		reg.prev = null;
		reg.next = null;
	}

    /**
     * Return true if this operand is on a register use/def list
     * or false if not.  This can only be called for register operands that are
     * part of a machine instruction.
     * @return
     */
	public boolean isOnRegUseList()
	{
		Util.assertion(isRegister(), "Can only add reg operand to use lists");
		if (parentMI != null)
        {
            MachineRegisterInfo regInfo = parentMI.getRegInfo();
            MachineOperand head = regInfo.getRegUseDefListHead(reg.regNo);
            if (head != null)
                return true;
        }
        return false;
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
			Util.assertion( !isEarlyClobber);
			setReg(reg);
		}
		else
		{
			opKind = MO_Register;
			this.reg = new RegOp(reg);

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
	 * Add this register operand to the specified MachineRegisterInfo.
     * If it is null, then the next/prev fields should be explicitly
     * nulled out.
	 * @param regInfo
	 */
	public void addRegOperandToRegInfo(MachineRegisterInfo regInfo)
	{
	    // FIXME 2017/11/18, this method works inproperly.
		Util.assertion(isRegister(), "Can only add reg operand to use lists");

        // If the reginfo pointer is null, just explicitly null out or next/prev
        // pointers, to ensure they are not garbage.
		if (regInfo == null)
		{
			reg.prev = null;
			reg.next = null;
			return;
		}
        // Otherwise, add this operand to the head of the registers use/def list.
		MachineOperand head = regInfo.getRegUseDefListHead(getReg());
		if (head == null)
        {
            // If the head node is null, set current op as head node.
	        this.reg.prev = null;
	        this.reg.next = null;
            regInfo.updateRegUseDefListHead(getReg(),this);
            return;
        }

        if (isDef())
        {
            if (!head.isDef())
            {
	            // insert the current node as head
	            reg.next = head;
	            reg.prev = null;
	            head.reg.prev = this;
	            regInfo.updateRegUseDefListHead(getReg(), this);
	            return;
            }
        }
        // Insert this machine operand into where immediately after head node.
        //  [    ] ---> [] -------->NULL
        //  [head] <--- []
        //  |    }      []
        //              ^ (prev)  ^  insert here (cur)
        MachineOperand cur = head, prev = head;
        while (cur != null)
        {
        	prev = cur;
            cur = cur.reg.next;
        }

        prev.reg.next = this;
        this.reg.prev = prev;
        this.reg.next = null;
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
		op.isUndef = isUndef;
		op.isEarlyClobber = isEarlyClobber;
		op.reg = new RegOp(reg);
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
		op.offsetedInfo = new OffsetedInfo();
		op.setIndex(idx);
		return op;
	}

	public static MachineOperand createConstantPoolIndex(int idx, int offset, int targetFlags)
	{
		MachineOperand op = new MachineOperand(MO_ConstantPoolIndex);
        op.offsetedInfo = new OffsetedInfo();
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
        op.offsetedInfo = new OffsetedInfo();
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
        op.offsetedInfo = new OffsetedInfo();
		op.offsetedInfo.val.symbolName = symName;
		op.setOffset(offset);
		op.setTargetFlags(targetFlags);
		return op;
	}
}
