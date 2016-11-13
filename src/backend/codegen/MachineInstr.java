package backend.codegen;

import backend.codegen.MachineOperand.UseType;
import backend.target.TargetInstrInfo;
import backend.value.GlobalValue;
import backend.value.Value;

import java.util.ArrayList;

import static backend.codegen.MachineOperand.MachineOperandType.*;

/**
 * Representation of each machine instruction.
 *
 * <p>MachineOpCode must be an enum, defined separately for each target.
 * E.g., It is defined in .
 * </p>
 * <p>There are 2 kinds of operands:
 * <ol>
 * <li>Explicit operands of the machine instruction in vector operands[].</li>
 * <li>"Implicit operands" are values implicitly used or defined by the
 * machine instruction, such as arguments to a CALL, return value of
 * a CALL (if any), and return value of a RETURN.</li>
 * </ol>
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineInstr
{
	private int              opCode;              // the opcode
	private int         opCodeFlags;         // flags modifying instrn behavior
	private ArrayList<MachineOperand> operands; // the operands
	private int numImplicitRefs;             // number of implicit operands

	/**
	 * Return true if it's illegal to add a new operand.
	 * @return
	 */
	private boolean operandsComplete()
	{
		int numOperands = TargetInstrInfo.MCInstrDescriptors[opCode].numOperands;
		if (numOperands>=0 && numOperands <= getNumOperands())
			return true;
		return false;
	}

	public MachineInstr(int opCode, int numOperands)
	{
		this.opCode = opCode;
		opCodeFlags = 0;
		operands = new ArrayList<>(numOperands);
		numImplicitRefs = 0;
	}

	public MachineInstr(MachineBasicBlock mbb, int opCode, int numOperands)
	{
		this(opCode, numOperands);
		assert mbb != null:"Can not use inserting operation with null basic block";
		mbb.addLast(this);
	}

	public int getOpCode(){return opCode;}
	public int getOpCodeFlags(){return opCodeFlags;}
	public int getNumOperands() {return operands.size() - numImplicitRefs;}
	public int getNumImplicitRefs(){return numImplicitRefs;}

	public MachineOperand getOperand(int index)
	{
		assert index>=0&&index < operands.size();
		return operands.get(index);
	}

	public MachineOperand getImplicitOp(int index)
	{
		assert index>=0&&index< numImplicitRefs:
			"implicit ref out of range!";
		return operands.get(index+operands.size()-numImplicitRefs);
	}

	/**
	 * Access to explicit or implicit operands of the instruction.
	 * This returns the i'th entry in the operand list.
	 * It corresponds to the i'th explicit operand or (i-N)'th implicit
	 * operand, where N indicates the number of all explicit operands.
	 * @param i
	 * @return
	 */
	public MachineOperand getExplOrImplOperand(int i)
	{
		assert i>=0&&i<operands.size();
		return i<getNumOperands() ? getOperand(i)
				: getImplicitOp(i-getNumOperands());
	}

	public Value getImplicitRef(int i)
	{
		return getImplicitOp(i).getVRegValue();
	}

	public void addImplicitRef(Value val, boolean isDef, boolean isDefAndUse)
	{
		numImplicitRefs++;
		addRegOperand(val, isDef, isDefAndUse);
	}

	/**
	 * Add a MO_VirtualRegister operand to the end of the operands list.
	 * @param val
	 * @param isDef
	 * @param isDefAndUse
	 */
	public void addRegOperand(Value val, boolean isDef, boolean isDefAndUse)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(val, MO_VirtualRegister,
				!isDef ? UseType.Use : (isDefAndUse ?
						UseType.UseAndDef : UseType.Def)));
	}

	public void addRegOperand(Value val, UseType utype, boolean isPCRelative)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(val, MO_VirtualRegister,
				utype, isPCRelative));
	}

	public void addCCRegOperand(Value val, UseType utype)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(val, MO_CCRegister,
				utype, false));
	}

	/**
	 * Add a symbolic virtual register reference.
	 * @param reg
	 * @param isDef
	 */
	public void addRegOperand(int reg, boolean isDef)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(reg, MO_VirtualRegister,
				isDef?UseType.Def:UseType.Use));
	}

	/**
	 * Add a symbolic virtual register reference.
	 * @param reg
	 * @param utype
	 */
	public void addRegOperand(int reg, UseType utype)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(reg, MO_VirtualRegister, utype));
	}

	public void addPCDispOperand(Value val)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(val, MO_PCRelativeDisp, UseType.Use));
	}

	/**
	 * Add a physical register operand into operands list.
	 * @param reg
	 * @param isDef
	 */
	public void addMachineRegOperand(int reg, boolean isDef)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(reg, MO_MachineRegister,
				isDef?UseType.Def:UseType.Use));
	}

	/**
	 * Add a physical register operand into operands list.
	 * @param reg
	 * @param utype
	 */
	public void addMachineRegOperand(int reg, UseType utype)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(reg, MO_MachineRegister,utype));
	}

	/**
	 * Add a zero extending immmediate operand.
	 * @param intValue
	 */
	public void addZeroExtImmOperand(long intValue)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(intValue, MO_UnextendedImmed));
	}

	/**
	 * Add a signed extending immmediate operand.
	 * @param intValue
	 */
	public void addSignExtImmOperand(long intValue)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(intValue, MO_SignExtendedImmed));
	}

	/**
	 * Add a machine basic block operand into instruction.
	 * @param mbb
	 */
	public void addMachineBasicBlockOperand(MachineBasicBlock mbb)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(mbb));
	}

	/**
	 * Add an abstract stack frame index operand into instruction.
	 * @param idx
	 */
	public void addFrameIndexOperand(int idx)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(idx, MO_FrameIndex));
	}

	/**
	 * Add a constant pool index operand into instruction.
	 * @param i
	 */
	public void addConstantPoolIndexOperand(int i)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(i, MO_ConstantPoolIndex));
	}

	/**
	 * Add a global address operand into this instruction, like the instr "call _foo".
	 * @param v
	 * @param isPCRelative
	 */
	public void addGlobalAddressOperand(GlobalValue v, boolean isPCRelative)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(v, MO_GlobalAddress, UseType.Use, isPCRelative));
	}

	public void addExternalSymbolOperand(String symbolName, boolean isPCRelative)
	{
		assert !operandsComplete():"Attempting to add an operand to a "
				+ "machine instr is already done";
		operands.add(new MachineOperand(symbolName, isPCRelative));
	}
}
