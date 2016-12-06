package backend.codegen;

import backend.target.TargetRegisterInfo;
import backend.value.GlobalValue;
import backend.value.Value;

import java.util.ArrayList;

import static backend.codegen.MachineOperand.Kind.*;
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
	public enum UseType
	{
		Use,             /// This machine operand is only read by the instruction
		Def,             /// This machine operand is only written by the instruction
		UseAndDef        /// This machine operand is read AND written
	}

	public enum MachineOperandType
	{
		MO_VirtualRegister,        // virtual register for *value
		MO_MachineRegister,        // pre-assigned machine register `regNum'
		MO_CCRegister,
		MO_SignExtendedImmed,
		MO_UnextendedImmed,
		MO_PCRelativeDisp,
		MO_MachineBasicBlock,       // MachineBasicBlock reference
		MO_FrameIndex,              // Abstract Stack Frame Index
		MO_ConstantPoolIndex,       // Address of indexed Constant in Constant Pool
		MO_ExternalSymbol,          // Name of external global symbol
		MO_GlobalAddress,           // Address of a global value
	}

	// Bit fields of the flags variable used for different operand properties
	interface Kind
	{
		int DEFFLAG = 0x01,       // this is a def but not a use of the operand
				DEFUSEFLAG = 0x02,       // this is both a def and a use
				HIFLAG32 = 0x04,       // operand is %hi32(value_or_immedVal)
				LOFLAG32 = 0x08,       // operand is %lo32(value_or_immedVal)
				HIFLAG64 = 0x10,       // operand is %hi64(value_or_immedVal)
				LOFLAG64 = 0x20,       // operand is %lo64(value_or_immedVal)
				PCRELATIVE = 0x40,       // Operand is relative to PC, not a global address

		USEDEFMASK = 0x03;
	}

	private Value value;        // BasicBlockVal for a label operand.
	// ConstantVal for a non-address immediate.
	// Virtual register for an SSA operand,
	//   including hidden operands required for
	//   the generated machine code.
	// LLVM global for MO_GlobalAddress.

	private long immedVal;        // Constant value for an explicit ant

	private MachineBasicBlock MBB;     // For MO_MachineBasicBlock type
	private String SymbolName;    // For MO_ExternalSymbol type

	private int flags;                   // see bit field definitions above
	private MachineOperandType opType;  // Pack into 8 bits efficiently after flags.
	private int regNum;                    // register number for an explicit register
	/**
	 * This list records all user of this machine operand for holding SSA form
	 * before register allocation.
	 *
	 * Note that: this property just available for register operand (includes
	 * virtual register and physical register).
	 */
	private ArrayList<MachineOperand> defUseList;
	/**
	 * The machine instruction in which this machine operand is embedded.
	 */
	private MachineInstr parentMI;

	// will be set for a value after reg allocation
	private MachineOperand()
	{
		immedVal = 0;
		flags = 0;
		opType = MO_VirtualRegister;
		regNum = -1;
		defUseList = new ArrayList<>();
	}

	public MachineOperand(long ImmVal, MachineOperandType OpTy)
	{
		immedVal = ImmVal;
		flags = 0;
		opType = OpTy;
		regNum = -1;
	}

	public MachineOperand(int Reg, MachineOperandType OpTy, UseType UseTy)
	{
		immedVal = (0);
		opType = (OpTy);
		regNum = (Reg);
		defUseList = new ArrayList<>();
		switch (UseTy)
		{
			case Use:
				flags = 0;
				break;
			case Def:
				flags = DEFFLAG;
				break;
			case UseAndDef:
				flags = DEFUSEFLAG;
				break;
			default:
				assert false : "Invalid value for UseTy!";
		}
	}

	public MachineOperand(Value V, MachineOperandType OpTy, UseType UseTy)
	{
		this(V, OpTy, UseTy, false);
	}

	public MachineOperand(Value V, MachineOperandType OpTy, UseType UseTy,
			boolean isPCRelative)
	{
		value = (V);
		opType = (OpTy);
		regNum = (-1);
		switch (UseTy)
		{
			case Use:
				flags = 0;
				break;
			case Def:
				flags = DEFFLAG;
				break;
			case UseAndDef:
				flags = DEFUSEFLAG;
				break;
			default:
				assert false : "Invalid value for UseTy!";
		}
		if (isPCRelative)
			flags |= PCRELATIVE;
	}

	public MachineOperand(MachineBasicBlock mbb)
	{
		MBB = (mbb);
		flags = (0);
		opType = (MO_MachineBasicBlock);
		regNum = (-1);
	}

	public MachineOperand(String SymName, boolean isPCRelative)
	{
		SymbolName = (SymName);
		flags = (isPCRelative ? PCRELATIVE : 0);
		opType = (MO_ExternalSymbol);
		regNum = (-1);
	}

	// Accessor methods.  Caller is responsible for checking the
	// operand type before invoking the corresponding accessor.
	//
	public MachineOperandType getType()
	{
		return opType;
	}

	public void setOpType(MachineOperandType newTy) {opType = newTy;}

	/// isPCRelative - This returns the value of the PCRELATIVE flag, which
	/// indicates whether this operand should be emitted as a PC relative value
	/// instead of a global address.  This is used for operands of the forms:
	/// MachineBasicBlock, GlobalAddress, ExternalSymbol
	///
	public boolean isPCRelative()
	{
		return (flags & PCRELATIVE) != 0;
	}

	// This is to finally stop caring whether we have a virtual or machine
	// register -- an easier interface is to simply call both virtual and machine
	// registers essentially the same, yet be able to distinguish when
	// necessary. Thus the instruction selector can just add registers without
	// abandon, and the register allocator won't be confused.
	public boolean isVirtualRegister()
	{
		return (opType == MO_VirtualRegister || opType == MO_MachineRegister)
				&& regNum >= TargetRegisterInfo.FirstVirtualRegister;
	}

	public boolean isPhysicalRegister()
	{
		return (opType == MO_VirtualRegister || opType == MO_MachineRegister)
				&& regNum < TargetRegisterInfo.FirstVirtualRegister;
	}

	public boolean isRegister()
	{
		return isVirtualRegister() || isPhysicalRegister();
	}

	public boolean isMachineRegister()
	{
		return !isVirtualRegister();
	}

	public boolean isMachineBasicBlock()
	{
		return opType == MO_MachineBasicBlock;
	}

	public boolean isPCRelativeDisp()
	{
		return opType == MO_PCRelativeDisp;
	}

	public boolean isImmediate()
	{
		return opType == MO_SignExtendedImmed || opType == MO_UnextendedImmed;
	}

	public boolean isFrameIndex()
	{
		return opType == MO_FrameIndex;
	}

	public boolean isConstantPoolIndex()
	{
		return opType == MO_ConstantPoolIndex;
	}

	public boolean isGlobalAddress()
	{
		return opType == MO_GlobalAddress;
	}

	public boolean isExternalSymbol()
	{
		return opType == MO_ExternalSymbol;
	}

	public Value getVRegValue()
	{
		assert (opType == MO_VirtualRegister || opType == MO_CCRegister ||
				isPCRelativeDisp());
		return value;
	}

	public Value getVRegValueOrNull()
	{
		return (opType == MO_VirtualRegister || opType == MO_CCRegister ||
				isPCRelativeDisp()) ? value : null;
	}

	public int getMachineRegNum()
	{
		assert (opType == MO_MachineRegister);
		return regNum;
	}

	public long getImmedValue()
	{
		assert (isImmediate());
		return immedVal;
	}

	public void setImmedVal(long val)
	{
		assert isImmediate();
		immedVal = val;
	}

	public MachineBasicBlock getMBB()
	{
		assert isMachineBasicBlock() : "Can't get MBB in non-MBB operand!";
		return MBB;
	}

	public int getFrameIndex()
	{
		assert (isFrameIndex());
		return (int) immedVal;
	}

	public int getConstantPoolIndex()
	{
		assert (isConstantPoolIndex());
		return (int) immedVal;
	}

	public GlobalValue getGlobal()
	{
		assert (isGlobalAddress());
		return (GlobalValue) value;
	}

	public String getSymbolName()
	{
		assert (isExternalSymbol());
		return SymbolName;
	}

	public boolean opIsUse()
	{
		return (flags & USEDEFMASK) == 0;
	}

	public boolean opIsDef()
	{
		return (flags & DEFFLAG) != 0;
	}

	public boolean opHiBits32()
	{
		return (flags & HIFLAG32) != 0;
	}

	public boolean opLoBits32()
	{
		return (flags & LOFLAG32) != 0;
	}

	public boolean opHiBits64()
	{
		return (flags & HIFLAG64) != 0;
	}

	public boolean opLoBits64()
	{
		return (flags & LOFLAG64) != 0;
	}

	// used to check if a machine register has been allocated to this operand
	public boolean hasAllocatedReg()
	{
		return (regNum >= 0 && (opType == MO_VirtualRegister
				|| opType == MO_CCRegister ||
				opType == MO_MachineRegister));
	}

	// Construction methods needed for fine-grain control.
	// These must be accessed via corresponding methods in MachineInstr.
	private void markHi32()
	{
		flags |= HIFLAG32;
	}

	private void markLo32()
	{
		flags |= LOFLAG32;
	}

	private void markHi64()
	{
		flags |= HIFLAG64;
	}

	private void markLo64()
	{
		flags |= LOFLAG64;
	}

	// Replaces the Value with its corresponding physical register after
	// register allocation is complete
	private void setRegForValue(int reg)
	{
		assert (opType == MO_VirtualRegister || opType == MO_CCRegister ||
				opType == MO_MachineRegister);
		regNum = reg;
	}

	public void setValue(Value val) {value = val;}

	public void setReg(int regNum) {this.regNum = regNum;}

	// used to get the reg number if when one is allocated
	public int getReg()
	{
		assert (hasAllocatedReg());
		return regNum;
	}

	public void setFlags(int flags)
	{
		this.flags = flags;
	}

	public int getFlags() {return flags;}

	/**
	 * Adds a usage to this machine operand into defUseList.
	 * @param mo
	 */
	public void addUserMO(MachineOperand mo)
	{
		assert mo != null;
		defUseList.add(mo);
	}

	public ArrayList<MachineOperand> getDefUseList()
	{
		assert defUseList != null && isRegister()
				: "Can not call this method for non-register operand";
		return defUseList;
	}
	public MachineInstr getParentMI()
	{
		return parentMI;
	}

	public void setParentMI(MachineInstr parentMI)
	{
		this.parentMI = parentMI;
	}
};
