package backend.codegen;

import backend.codegen.MachineOperand.UseType;
import backend.value.GlobalValue;
import backend.value.Value;

/**
 * This is a convenient helper class for creating a machine instruction on
 * specified target machine, e.g.X86.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineInstrBuilder
{
	private MachineInstr mi;

	public MachineInstrBuilder(MachineInstr instr) {mi = instr;}

	public MachineInstrBuilder addReg(int regNo){return addReg(regNo, UseType.Use);}

	/**
	 * Adds a new virtual register operand into specified instruction.
	 * @param regNo
	 * @param utype
	 * @return
	 */
	public MachineInstrBuilder addReg(int regNo, UseType utype)
	{
		mi.addRegOperand(regNo, utype);;
		return this;
	}

	/**
	 * Add a backend value that used as a register.
	 * @param val
	 * @param utype
	 * @return
	 */
	public MachineInstrBuilder addReg(Value val, UseType utype)
	{
		mi.addRegOperand(val, utype, false);
		return this;
	}

	public MachineInstrBuilder addCCReg(Value val, UseType utype)
	{
		mi.addCCRegOperand(val, utype);
		return this;
	}

	/**
	 * Add a backend value as an operand into operands list. But
	 * it is worthwhile watching that the val will be defined as a register.
	 * @param val
	 * @return
	 */
	public MachineInstrBuilder addRegDef(Value val)
	{
		return addReg(val, UseType.Def);
	}

	/**
	 * Add a backend value as an operand as a PC-relative displacement.
	 * @param val
	 * @return
	 */
	public MachineInstrBuilder addPCDisp(Value val)
	{
		mi.addPCDispOperand(val);
		return this;
	}

	/**
	 * Add a machine register as an operand.
	 * @param reg
	 * @param utype
	 * @return
	 */
	public MachineInstrBuilder addMReg(int reg, UseType utype)
	{
		mi.addMachineRegOperand(reg, utype);
		return this;
	}

	/**
	 * Add a signed immediate value as an operand.
	 * @param val
	 * @return
	 */
	public MachineInstrBuilder addSImm(long val)
	{
		mi.addSignExtImmOperand(val);
		return this;
	}

	/**
	 * Add a zero extended immediate value as an operand.
	 * @param val
	 * @return
	 */
	public MachineInstrBuilder addZImm(long val)
	{
		mi.addZeroExtImmOperand(val);
		return this;
	}

	/**
	 * Add a machine basic block as an operand of branch instruction.
	 * @param mbb
	 * @return
	 */
	public MachineInstrBuilder addMBB(MachineBasicBlock mbb)
	{
		mi.addMachineBasicBlockOperand(mbb);
		return this;
	}

	/**
	 * Add a stack frame index as an operand, which is usually used for accessing
	 * memory by [%ebp +- index].
	 * @param idx
	 * @return
	 */
	public MachineInstrBuilder addFrameIndex(int idx)
	{
		mi.addFrameIndexOperand(idx);
		return this;
	}

	public MachineInstrBuilder addConstantPool(int index)
	{
		mi.addConstantPoolIndexOperand(index);
		return this;
	}

	/**
	 * Add a global value representing address as an operand of some instr, like
	 * call instr.
	 * @param addr
	 * @param isPCRelative
	 * @return
	 */
	public MachineInstrBuilder addGlobalAddress(GlobalValue addr,
			boolean isPCRelative)
	{
		mi.addGlobalAddressOperand(addr, isPCRelative);
		return this;
	}

	public MachineInstrBuilder addExternalSymbol(String name,
			boolean isPCRelative)
	{
		mi.addExternalSymbolOperand(name, isPCRelative);
		return this;
	}

	/**
	 * Builder interface.  Specify how to create the initial instruction
	 * itself.  NumOperands is the number of operands to the machine instruction to
	 * allow for memory efficient representation of machine instructions.
	 * @param opcode
	 * @param numOperands
	 * @return
	 */
	public static MachineInstrBuilder buildMI(int opcode, int numOperands)
	{
		return new MachineInstrBuilder(new MachineInstr(opcode, numOperands));
	}

	public static MachineInstrBuilder buildMI(int opcode, int numOperands, int destReg)
	{
		return new MachineInstrBuilder(new MachineInstr(opcode, numOperands+1)).
				addReg(destReg, UseType.Def);
	}

	public static MachineInstrBuilder buildMI(MachineBasicBlock mbb, int opcode, int numOperands)
	{
		return new MachineInstrBuilder(new MachineInstr(mbb, opcode, numOperands));
	}

	public static MachineInstrBuilder buildMI(MachineBasicBlock mbb,
			int opcode,
			int numOperands,
			int destReg)
	{
		return new MachineInstrBuilder(new MachineInstr(mbb, opcode, numOperands+1))
				.addReg(destReg, UseType.Def);
	}

	public MachineInstr getMInstr(){return mi;}

	/**
	 * This function add a direct memory reference to this instruction.
	 * that is, a dereference of an address in a register, with no scale, index
	 * or displacement. An example is: [%eax].
	 * @param mib
	 * @param regNo
	 * @return
	 */
	public static MachineInstrBuilder addDirectMem(MachineInstrBuilder mib, int regNo)
	{
		// regNo(1+ NoReg*0).
		return mib.addReg(regNo, UseType.Use).addZImm(0).addReg(0, UseType.Use).addSImm(0);
	}

	/**
	 * This function is used to add a memory reference of the form
	 * [Reg + Offset], i.e., one with no scale or index, but with a
	 * displacement. An example is: DWORD PTR [EAX + 4].
	 * @param mib
	 * @param regNo
	 * @param offset
	 * @return
	 */
	public static MachineInstrBuilder addRegOffset(MachineInstrBuilder mib,
			int regNo, int offset)
	{
		// regNo(offset+ NoReg*0).
		return mib.addReg(regNo, UseType.Use).addZImm(0).addReg(0, UseType.Use).addSImm(offset);
	}

	public static MachineInstrBuilder addFrameReference(MachineInstrBuilder mib, int fi)
	{
		return addFrameReference(mib, fi, 0);
	}

	/**
	 * This function is used to add a reference to the base of
	 * an abstract object on the stack frame of the current function.  This
	 * reference has base register as the FrameIndex offset until it is resolved.
	 * This allows a constant offset to be specified as well...
	 * @param mib
	 * @param fi
	 * @param offset
	 * @return
	 */
	public static MachineInstrBuilder addFrameReference(MachineInstrBuilder mib, int fi, int offset)
	{
		return mib.addFrameIndex(fi).addZImm(1).addReg(0, UseType.Use).addSImm(0);
	}

	/**
	 * This function is used to add a reference to the
	 * base of a constant value spilled to the per-function constant pool.  The
	 * reference has base register ConstantPoolIndex offset which is retained until
	 * either machine code emission or assembly output
	 * @param mib
	 * @param cpi
	 * @param offset
	 * @return
	 */
	public static MachineInstrBuilder addConstantPoolReference(MachineInstrBuilder mib, int cpi, int offset)
	{
		return mib.addConstantPool(cpi).addZImm(1).addReg(0, UseType.Use).addSImm(0);
	}
}
