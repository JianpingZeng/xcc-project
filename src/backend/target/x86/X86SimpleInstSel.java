package backend.target.x86;

import backend.codegen.*;
import backend.codegen.MachineOperand.UseType;
import backend.hir.BasicBlock;
import backend.hir.InstVisitor;
import backend.hir.Operator;
import backend.pass.FunctionPass;
import backend.target.TargetData;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo.TargetRegisterClass;
import backend.type.SequentialType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.*;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static backend.codegen.MachineInstrBuilder.*;
import static backend.target.x86.X86SimpleInstSel.TypeClass.*;
import static backend.value.Instruction.CmpInst.Predicate.FIRST_ICMP_PREDICATE;
import static backend.value.Instruction.CmpInst.Predicate.LAST_ICMP_PREDICATE;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86SimpleInstSel extends FunctionPass implements InstVisitor<Void>
{
	interface TypeClass
	{
		int i8 = 0;
		int i16 = 1;
		int i32 = 2;
		int i64 = 3;
		int f32 = 4;
		int f64 = 5;
		int f80 = 6;
	}

	static class ValueRecord
	{
		Value val;
		int reg;
		Type ty;
		ValueRecord(int r,Type t)
		{
			reg = r;
			ty = t;
		}

		ValueRecord(Value v)
		{
			val = v;
			reg = 0;
			ty = v.getType();
		}
	}

	private TargetMachine tm;
	private MachineFunction mf;
	private MachineBasicBlock mbb;

	/**
	 * Mapping between SSA value and virtual register
	 * (it just is a non-negative integer).
	 */
	private HashMap<Value, Integer> regMap;

	/**
	 * Mapping between HIR basic block and machine basic block.
	 */
	private HashMap<BasicBlock, MachineBasicBlock> mbbMap;
	/**
	 * The index into the first vararg value of variable argument list.
	 */
	private int varArgsFrameIndex;

	private X86SimpleInstSel(TargetMachine machine)
	{
		tm = machine;
		regMap = new HashMap<>();
		mbbMap = new HashMap<>();
	}
	/**
	 * Top level implementation of instruction selector for x86 target on
	 * entire function.
	 * @param f
	 * @return
	 */
	@Override
	public boolean runOnFunction(Function f)
	{
		mf = new MachineFunction(f, tm);

		// fills mbbMap.
		for (BasicBlock bb : f.getBasicBlockList())
			mbbMap.put(bb, new MachineBasicBlock(bb));

		mbb = mf.getEntryBlock();

		// assigns incoming arguments with virtual register.
		assignArgumentsWithVirtualReg(f);

		// First pass: performs inst selecting expect for Phi node.
		visit(f);

		// Second pass: lowering PHI nodes.
		selectPhiNodes();

		regMap.clear();
		mbbMap.clear();
		return true;
	}

	private static MachineInstrBuilder bmi(MachineBasicBlock mbb,
			int idx, int opcode,
			int numOperands, int destReg)
	{
		assert idx>=0 && idx<mbb.size();
		MachineInstr instr = new MachineInstr(opcode, numOperands+1);
		mbb.insert(idx, instr);
		return new MachineInstrBuilder(instr);
	}

	private static MachineInstrBuilder bmi(MachineBasicBlock mbb,
			int idx, int opcode,
			int numOperands)
	{
		assert idx>=0 && idx<mbb.size();
		MachineInstr instr = new MachineInstr(opcode, numOperands);
		mbb.insert(idx, instr);
		return new MachineInstrBuilder(instr);
	}
	/**
	 * This method returns the next virtual register number we have not yet used.
	 *
	 * Long values are handled somewhat specially, they are always allocated as '
	 * pairs of 32 bit integer values. The register number returned
	 * is the upper 32 bit of the long value.
	 * @param ty
	 * @return
	 */
	private int makeAnotherReg(Type ty)
	{
		assert tm.getRegInfo() instanceof X86RegisterInfo
				:"Current target doesn't have x86 reg info!";
		X86RegisterInfo mri = (X86RegisterInfo) tm.getRegInfo();
		if (ty.getPrimitiveID()== Type.Int64TyID)
		{
			TargetRegisterClass rc = mri.getRegClassForType(Type.Int32Ty);
			mf.getMachineRegisterInfo().createVirtualRegister(rc);
			return mf.getMachineRegisterInfo().createVirtualRegister(rc) - 1;
		}

		TargetRegisterClass rc = mri.getRegClassForType(ty);
		return mf.getMachineRegisterInfo().createVirtualRegister(rc);
	}

	private void emitGEPOperation(int insertPos, Value base,
			List<Use> uselists, int destReg)
	{
		int baseReg = getReg(base, mbb, insertPos);
		TargetData td = tm.getTargetData();
		Type ty = base.getType();

		// GEPs have zero or more indices; we must perform a struct access
		// or array access for each one.
		for (Use u : uselists)
		{
			Value idx = u.getValue();
			int nextReg = baseReg;

			if (ty instanceof StructType)
			{
				StructType sty = (StructType)ty;
				// It is a struct access. idx is the index into structure member
				//
				ConstantInt ci = (ConstantInt)idx;
				assert ci.getType()==Type.Int8Ty;

				// use the TargetData to pick up what the layout of the structure
				// is in memory.  Since the structure index must
				// be constant, we can get its value and use it to find the
				// right byte offset from the StructLayout class's list of
				// structure member offsets.
				int idxValue = (int)ci.getZExtValue();
				int fieldOff = td.getStructLayout(sty).
						memberOffsets.get(idxValue).intValue();

				if(fieldOff != 0)
				{
					nextReg = makeAnotherReg(Type.Int32Ty);
					// Emit an ADD to add fieldOff to the basePtr.
					bmi(mbb, insertPos++, X86InstrSets.ADDri32, 2, nextReg).
							addReg(baseReg, UseType.Use).addZImm(fieldOff);
				}
				// The next type is the member of the structure selected by the
				// index.
				ty = sty.getElementType(idxValue);
			}
			else if (ty instanceof SequentialType)
			{
				SequentialType sty = (SequentialType)ty;
				// It's an array or pointer access: [ArraySize x ElementType].
				// idx is the index into the array.
				// Unlike with structure
				// indices, we may not know its actual value at code-generation
				// time.

				// Most GEP instructions use a [cast (int/uint) to LongTy] as their
				// operand on X86.  Handle this case directly now...
				if (idx instanceof CastInst)
				{
					CastInst ci = (CastInst)idx;
					if (ci.operand(0).getType() == Type.Int32Ty)
						idx = ci.operand(0);
				}

				ty = sty.getElemType();
				int eltSize = (int)td.getTypeSize(ty);

				// If idxReg is a constant, we don't need to generate mul instr.
				if (idx instanceof ConstantInt)
				{
					ConstantInt ci = (ConstantInt)idx;
					if (!ci.isNullValue())
					{
						long offset = eltSize * ci.getZExtValue();
						nextReg = makeAnotherReg(Type.Int32Ty);
						bmi(mbb, insertPos++, X86InstrSets.ADDri32, 2, nextReg)
								.addReg(baseReg).addZImm(offset);
					}
				}
				else if (eltSize == 1)
				{
					// If the element size is 1, we don't have to multiply, just add
					int idxReg = getReg(idx, mbb, insertPos++);
					nextReg = makeAnotherReg(Type.Int32Ty);
					bmi(mbb, insertPos++, X86InstrSets.ADDrr32, 2, nextReg).
							addReg(baseReg).addReg(idxReg);
				}
				else
				{
					// normal slow case, the idx is not a constant and eltSize
					// is not one.
					int idxReg = getReg(idx, mbb, insertPos++);
					int offsetReg = makeAnotherReg(Type.Int32Ty);
					doMultiplyConst(insertPos++, offsetReg, Type.Int32Ty,
							idxReg, eltSize);

					// Emit a ADD to add offsetReg to baseReg.
					nextReg = makeAnotherReg(Type.Int32Ty);
					bmi(mbb, insertPos++, X86InstrSets.ADDrr32, 2, nextReg).
							addReg(baseReg).addReg(offsetReg);
				}
			}

			baseReg = nextReg;
		}

		// After we processed all indices, the result is left in baseReg.
		// Move it to destReg where we were expected to put the result.
		bmi(mbb, insertPos++, X86InstrSets.MOVrr32, 1, destReg).addReg(baseReg);
	}

	private void emitCastOperation(int idx, Value op0,
			Operator opcode, Type ty, int destReg)
	{

	}

	/**
	 * Implement simple binary operators for integral types.
	 * {@code operatorClass} is one of: 0 for Add, 1 for Sub, 2 for And, 3 for
	 * Or, 4 for Xor.
	 * @param insertPos
	 * @param op0
	 * @param op1
	 * @param operatorClass
	 * @param destReg
	 */
	private void emitSimpleBinaryOperation(int insertPos,
			Value op0, Value op1, int operatorClass, int destReg)
	{
		int klass = getClass(op0.getType());
		// Before handling common cass, there are many special cases we should
		// handled for improving performance.

		// sub 0, X ---> neg X.
		if (operatorClass == 1 && klass != i64)
		{
			if (op0 instanceof ConstantInt)
			{
				ConstantInt ci = (ConstantInt)op0;
				if (ci.isNullValue())
				{
					int op1Reg = getReg(op1, mbb, insertPos++);
					assert klass>=i8 && klass<=i32:"Unknown type class for this function";
					int[] opcode = {
							X86InstrSets.NEGr8,
							X86InstrSets.NEGr16,
							X86InstrSets.NEGr32
					};
					bmi(mbb,insertPos, opcode[klass], 1, destReg).addReg(op1Reg);
					return;
				}
			}
		}

		if (!(op1 instanceof ConstantInt) || klass == i64)
		{
			int[][] opcodeTab = {
					// Arithmetic operators
					{ X86InstrSets.ADDrr8, X86InstrSets.ADDrr16, X86InstrSets.ADDrr32, X86InstrSets.FpADD },  // ADD
					{ X86InstrSets.SUBrr8, X86InstrSets.SUBrr16, X86InstrSets.SUBrr32, X86InstrSets.FpSUB },  // SUB

					// Bitwise operators
					{ X86InstrSets.ANDrr8, X86InstrSets.ANDrr16, X86InstrSets.ANDrr32, 0 },  // AND
					{ X86InstrSets.ORrr8, X86InstrSets.ORrr16, X86InstrSets.ORrr32, 0 },  // OR
					{ X86InstrSets.XORrr8, X86InstrSets.XORrr16, X86InstrSets.XORrr32, 0 },  // XOR
			};

			boolean isLong = false;
			if (klass == i64)
			{
				isLong = true;
				klass = i32;
			}

			int opcode = opcodeTab[operatorClass][klass];
			assert opcode != 0:"Floating point argument to logical inst?";
			int op1Reg = getReg(op0, mbb, insertPos++);
			int op2Reg = getReg(op1, mbb, insertPos++);
			bmi(mbb, insertPos++, opcode, 2, destReg).addReg(op1Reg).addReg(op2Reg);

			if (isLong)
			{
				// Handle the upper 32 bits of long values...
				int[] topTab = {X86InstrSets.ADCrr32, X86InstrSets.SBBrr32,
						X86InstrSets.ANDrr32, X86InstrSets.ORrr32, X86InstrSets.XORrr32};
				bmi(mbb, insertPos++, topTab[operatorClass], 2, destReg+1).
						addReg(op1Reg+1).addReg(op2Reg+1);
			}
			return;
		}

		// Special case: op Reg <const> and type is <=i32.
		ConstantInt ci1 = (ConstantInt)op1;
		int op0Reg = getReg(op0, mbb, insertPos++);

		// xor X, -1 --> not X.
		if (operatorClass == 4 && ci1.isAllOnesValue())
		{
			int[] notTab = {X86InstrSets.NOTr8, X86InstrSets.NOTr16, X86InstrSets.NOTr32};
			bmi(mbb, insertPos, notTab[klass], 1, destReg).addReg(op0Reg);
			return;
		}

		// add X, -1 --> dec X.
		if (operatorClass == 0 && ci1.isAllOnesValue())
		{
			int[] decTab = {X86InstrSets.DECr8, X86InstrSets.DECr16, X86InstrSets.DECr32};
			bmi(mbb, insertPos, decTab[klass], 1, destReg).addReg(op0Reg);
			return;
		}

		// add X, 1 --> inc X.
		if (operatorClass == 0 && ci1.equalsInt(1))
		{
			int[] incTab = {X86InstrSets.INCr8, X86InstrSets.INCr16, X86InstrSets.INCr32};
			bmi(mbb, insertPos, incTab[klass], 1, destReg).addReg(op0Reg);
			return;
		}

		int[][] opcodeTab = {
			// Arithmetic operators
			{ X86InstrSets.ADDri8, X86InstrSets.ADDri16, X86InstrSets.ADDri32 },  // ADD
			{ X86InstrSets.SUBri8, X86InstrSets.SUBri16, X86InstrSets.SUBri32 },  // SUB

			// Bitwise operators
			{ X86InstrSets.ANDri8, X86InstrSets.ANDri16, X86InstrSets.ANDri32 },  // AND
			{ X86InstrSets. ORri8, X86InstrSets. ORri16, X86InstrSets. ORri32 },  // OR
			{ X86InstrSets.XORri8, X86InstrSets.XORri16, X86InstrSets.XORri32 },  // XOR
		};

		assert klass < 3:"Invalid TypeClass in emitSimpleBinaryOperation";
		assert operatorClass < 5;
		int opcode = opcodeTab[operatorClass][klass];
		long op1Const = ci1.getZExtValue();
		bmi(mbb, insertPos, opcode, 2, destReg).addReg(op0Reg).addZImm(op1Const);
	}

	/**
	 * Emits instruction used for turn a narrow operand into 32-bit wide operand
	 * , and move it into destination register.
	 * @param destReg
	 * @param arg
	 */
	private void promote32(int destReg, ValueRecord arg)
	{
		int[][] extendOp = {
				// int extended mov.
				{X86InstrSets.MOVZXr32r8, X86InstrSets.MOVZXr32r16, X86InstrSets.MOVrr32},
				// signed extended mov (there is no difference regardless of int).
				{X86InstrSets.MOVSXr32r8, X86InstrSets.MOVSXr32r16, X86InstrSets.MOVrr32}
		};

		int isUnsigned = arg.ty.isUnsigned()?0:1;
		// make sure we have a virtual register number for this arg.
		int reg = arg.val != null? getReg(arg.val) : arg.reg;

		int klass = getClass(arg.ty);
		assert klass >= i8 && klass <= i32 : "Unpromotable operand class";

		buildMI(mbb, extendOp[isUnsigned][klass], 1, destReg).addReg(reg);
	}


	/**
	 * This emits an abstract call instruction, setting up the arguments and
	 * the return value as appropriate.
	 * @param res
	 * @param called
	 * @param args
	 */
	private void doCall(ValueRecord res,
			MachineInstr called,
			ArrayList<ValueRecord> args)
	{
		// count how many number of bytes will be pushed onto stack.
		int numBytes = 0;

		if (!args.isEmpty())
		{
			for (ValueRecord arg: args)
			{
				switch (getClass(arg.ty))
				{
					case i8:
					case i16:
					case i32:
					case f32:
						numBytes += 4;
						break;
					case i64:
					case f64:
						numBytes += 8;
						break;
					default:
						assert false:"Unknown type class!";
				}
			}

			// adjust stack pointer for incoming arguments.
			buildMI(mbb, X86InstrSets.ADJCALLSTACKDOWN, 1).addZImm(numBytes);

			// Arguments go on the stack in reverse order, as specified by the ABI.
			int argOffset = 0;
			for (int i = args.size() - 1; i >= 0; i--)
			{
				ValueRecord arg = args.get(i);
				int argReg = arg.val != null?getReg(arg.val): arg.reg;
				switch (getClass(arg.ty))
				{
					case i8:
					case i16:
					{
						// Promote arg to 32 bits wide into a temporary register...
						int r = makeAnotherReg(Type.Int32Ty);
						promote32(r, arg);
						addRegOffset(buildMI(mbb, X86InstrSets.MOVrm32, 5),
								X86RegsSet.ESP, argOffset).addReg(r);
						break;
					}
					case i32:
						addRegOffset(buildMI(mbb, X86InstrSets.MOVrm32, 5),
								X86RegsSet.ESP, argOffset).addReg(argReg);
						break;
					case i64:
						addRegOffset(buildMI(mbb, X86InstrSets.MOVrm32, 5),
								X86RegsSet.ESP, argOffset).addReg(argReg);
						addRegOffset(buildMI(mbb, X86InstrSets.MOVrm32, 5),
								X86RegsSet.ESP, argOffset + 4).addReg(argReg+1);
						argOffset += 4;
						break;
					case f32:
						addRegOffset(buildMI(mbb, X86InstrSets.FSTr32, 5),
								X86RegsSet.ESP, argOffset).addReg(argReg);
						break;
					case f64:
						addRegOffset(buildMI(mbb, X86InstrSets.FSTr64, 5),
								X86RegsSet.ESP, argOffset).addReg(argReg);
						argOffset += 4;
						break;
					default:
						assert false:"Unknown class!";
				}
				argOffset += 4;
			}
		}
		else
		{
			buildMI(mbb, X86InstrSets.ADJCALLSTACKDOWN, 1).addZImm(0);
		}

		mbb.addLast(called);
		// after calling to called. restore stack frame.
		buildMI(mbb, X86InstrSets.ADJCALLSTACKUP, 1).addZImm(numBytes);

		// If there is a return value, obtain the returned value from %al/%ax/%eax
		// register.
		if (res.ty != Type.VoidTy)
		{
			int destClass = getClass(res.ty);
			switch (destClass)
			{
				case i8:
				case i16:
				case i32:
				{
					// integer results are in %eax.
					int[] retRegMove = {X86InstrSets.MOVrr8,
							X86InstrSets.MOVrr16,
							X86InstrSets.MOVrr32};
					int[] retRegs = {X86RegsSet.AL, X86RegsSet.AX, X86RegsSet.EAX};
					buildMI(mbb, retRegMove[destClass], 1, res.reg).addReg(retRegs[destClass]);
					break;
				}
				case f32:
				case f64:
				case f80:
				{
					// floating-point return instruction.
					buildMI(mbb, X86InstrSets.FpGETRESULT, 1, res.reg);
					break;
				}
				case i64:
				{
					buildMI(mbb, X86InstrSets.MOVrr32, 1, res.reg).addReg(X86RegsSet.EAX);
					buildMI(mbb, X86InstrSets.MOVrr32, 1, res.reg+1).addReg(X86RegsSet.EDX);
					break;
				}
				default:
					assert false:"Unknown type class";
			}
		}
	}

	private void emitDivRemOperation(int idx,
			int op1Reg,
			int op2Reg,
			Type ty,
			boolean isDiv,
			int destReg)
	{
		int klass = getClass(ty);
		// First, handle float-pointing and long integral number.
		switch (klass)
		{
			case f32:
			case f64:
			case f80:
			{
				if (isDiv)
				{
					buildMI(mbb, X86InstrSets.FpDIV, 2, destReg).addReg(op1Reg)
							.addReg(op2Reg);
				}
				else
				{
					MachineInstr call = buildMI(X86InstrSets.CALLpcrel32, 1)
							.addExternalSymbol("fmod", true).getMInstr();
					ArrayList<ValueRecord> args = new ArrayList<>();
					args.add(new ValueRecord(op1Reg, Type.DoubleTy));
					args.add(new ValueRecord(op2Reg, Type.DoubleTy));
					doCall(new ValueRecord(destReg, Type.DoubleTy), call, args);
				}
				return;
			}
			case i64:
			{
				String[] fnName = { "__moddi3", "__divdi3", "__umoddi3",
						"__udivdi3" };
				int nameIdx = (ty.isUnsigned() ? 1 : 0) * 2 + (isDiv ? 1 : 0);
				MachineInstr call = buildMI(X86InstrSets.CALLpcrel32, 1)
						.addExternalSymbol(fnName[nameIdx], true).getMInstr();
				ArrayList<ValueRecord> args = new ArrayList<>();
				args.add(new ValueRecord(op1Reg, Type.Int64Ty));
				args.add(new ValueRecord(op2Reg, Type.Int64Ty));
				doCall(new ValueRecord(destReg, Type.Int64Ty), call, args);
				return;
			}
			case i8:
			case i16:
			case i32:
				break; // small integer, handled below.
			default:
				assert false : "Unkown type class";
		}

		// Second, handle i8, i16, i32 integer.
		int regs[] = { X86RegsSet.AL, X86RegsSet.AX, X86RegsSet.EAX };
		int movOpcode[] = { X86InstrSets.MOVrr8, X86InstrSets.MOVrr16,
				X86InstrSets.MOVrr32 };
		int sarOpcode[] = { X86InstrSets.SARir8, X86InstrSets.SARir16,
				X86InstrSets.SARir32 };
		int clrOpcode[] = { X86InstrSets.XORrr8, X86InstrSets.XORrr16,
				X86InstrSets.XORrr32 };
		int extRegs[] = { X86RegsSet.AH, X86RegsSet.DX, X86RegsSet.EDX };

		int[][] divOpecode = {
				// int division.
				{ X86InstrInfo.DIVr8, X86InstrInfo.DIVr16, X86InstrInfo.DIVr32 },
				// signed disision.
				{ X86InstrInfo.IDIVr8, X86InstrInfo.IDIVr16,
						X86InstrInfo.IDIVr32 }
		};

		boolean isSigned = ty.isSigned();
		int reg = regs[klass];
		int extReg = extRegs[klass];

		// Put the first operand into AL/AX/EAX.
		buildMI(mbb, movOpcode[klass], 1, reg).addReg(op1Reg);

		if (isSigned)
		{
			// emit a sign extension instruction.
			int shiftResult = makeAnotherReg(ty);
			buildMI(mbb, sarOpcode[klass], 2, shiftResult).addReg(op1Reg).addZImm(31);
			buildMI(mbb, movOpcode[klass], 1, extReg).addReg(shiftResult);
		}
		else
		{
			// If int, emit a zeroing instruction.
			buildMI(mbb, clrOpcode[klass], 2, extReg).addReg(extReg).addReg(extReg);
		}

		// Here, the first operand is preparing.
		buildMI(mbb, divOpecode[isSigned?1:0][klass], 1).addReg(op2Reg);

		// for holding the result depending on it is division or remainder.
		int tmpReg = isDiv? reg: extReg;

		buildMI(mbb, movOpcode[klass], 1, destReg).addReg(tmpReg);
	}

	private void doMultiplyConst(int insertPos, int destReg,
			Type ty, int op1Reg, int consOp2)
	{
		int klass = getClass(ty);
		if (Util.isPowerOf2(consOp2))
		{
			int shift = Util.log2(consOp2);
			switch (klass)
			{
				default: assert false:"Unknown class for this function!";
				case TypeClass.i8:
				case TypeClass.i16:
				case TypeClass.i32:
					bmi(mbb, insertPos, X86InstrSets.SHLir32, 2, destReg).
							addReg(op1Reg).addZImm(consOp2);
					return;
			}
		}

		if (klass == TypeClass.i16)
		{
			bmi(mbb, insertPos, X86InstrSets.IMULri16, 2, destReg).
					addReg(op1Reg).addZImm(consOp2);
			return;
		}
		else if (klass == TypeClass.i32)
		{
			bmi(mbb, insertPos, X86InstrSets.IMULri32, 2, destReg).
					addReg(op1Reg).addZImm(consOp2);
			return;
		}

		// Most general case, emit a normal multiply.
		int[] tab = {X86InstrSets.MOVir8, X86InstrSets.MOVir16, X86InstrSets.MOVir32};

		int tempReg = makeAnotherReg(ty);
		bmi(mbb, insertPos, tab[klass], 1, tempReg).addZImm(consOp2);

		doMultiply(insertPos, destReg, ty, op1Reg, tempReg);
	}

	private void doMultiply(int insertPos,
			int destReg,
			Type destTy,
			int op1Reg,
			int op2Reg)
	{
		int klass = getClass(destTy);
		switch (klass)
		{
			case f32:
			case f64:
			case f80:
				bmi(mbb, insertPos, X86InstrSets.FpMUL, 2, destReg).
						addReg(op1Reg).addReg(op2Reg);
				return;
			case i32:
				bmi(mbb, insertPos, X86InstrSets.IMULrr32, 2, destReg).
						addReg(op1Reg).addReg(op2Reg);
				return;
			case i16:
				bmi(mbb, insertPos, X86InstrSets.IMULrr16, 2, destReg).
						addReg(op1Reg).addReg(op2Reg);
				return;
			case i8:
				// Must use the MUL instruction, which forces use of AL...
				bmi(mbb, insertPos++, X86InstrSets.MOVrr8, 1, X86RegsSet.AL).
						addReg(op1Reg);
				bmi(mbb, insertPos++, X86InstrSets.MULr8, 1).addReg(op2Reg);
				bmi(mbb, insertPos++, X86InstrSets.MOVrr8, 1, destReg).
						addReg(X86RegsSet.AL);
				return;
			default:
			case i64:
				assert false:"doMultiply can not operates by long";
		}
	}

	/**
	 * Generates the instruction required for moving constant into specified
	 * virtual register.
	 * @param c
	 * @param reg
	 */
	private void copyConstToReg(int idx, Constant c, int reg)
	{
		if (c instanceof ConstantExpr)
		{
			int category = 0;
			ConstantExpr ce = (ConstantExpr) c;
			switch (ce.getOpCode())
			{
				case GetElementPtr:
					emitGEPOperation(idx, ce, ce.getUseList(), reg);
					return;
				case Trunc:
				case SExt:
				case ZExt:
				case SIToFP:
				case UIToFP:
				case FPExt:
				case FPTrunc:
				case FPToSI:
				case FPToUI:
				case IntToPtr:
				case PtrToInt:
				case BitCast:
					emitCastOperation(idx, ce.operand(0),
							ce.getOpCode(), ce.getType(), reg);
					return;
				case Xor: category++;
				case Or: category++;
				case And: category++;
				case Sub:
				case FSub:
					category++;
				case Add:
				case FAdd:
					emitSimpleBinaryOperation(idx, ce.operand(0),
							ce.operand(1), category, reg);
					return;
				case Mul:
				case FMul:
				{
					int op1Reg = getReg(ce.operand(0), mbb, idx);
					int op2Reg = getReg(ce.operand(1), mbb, idx);
					doMultiply(idx, reg, ce.getType(), op1Reg, op2Reg);
					return;
				}
				case UDiv:
				case SDiv:
				case URem:
				case SRem:
				case FDiv:
				{
					int op1Reg = getReg(ce.operand(0), mbb, idx);
					int op2Reg = getReg(ce.operand(1), mbb, idx);
					boolean isDiv = ce.getOpCode() == Operator.UDiv
							|| ce.getOpCode() == Operator.SDiv
							|| ce.getOpCode() == Operator.FDiv;
					emitDivRemOperation(idx, op1Reg, op2Reg, ce.getType(), isDiv, reg);
					return;
				}
				default:
					assert false:"Unknown constant expr!";
			}
		}

		if (c.getType().isIntegerType())
		{
			int klass = getClass(c.getType());
			if (klass == i64)
			{
				// Copy the value into the register pair.
				long val = ((ConstantInt)(c)).getZExtValue();
				bmi(mbb, idx++, X86InstrSets.MOVir32, 1, reg).addZImm(val & 0xFFFFFFF);
				bmi(mbb, idx++, X86InstrSets.MOVir32, 1, reg+1).addZImm(val >>32);
				return;
			}

			assert klass<=TypeClass.i32:"Invalid type class!";

			int integralOpcodeTab[] = {
				X86InstrSets.MOVir8, X86InstrSets.MOVir16, X86InstrSets.MOVir32
			};

			ConstantInt ci = (ConstantInt)c;
			bmi(mbb, idx++, integralOpcodeTab[klass], 1, reg).addZImm(ci.getZExtValue());
		}
		else if (c instanceof ConstantFP)
		{
			ConstantFP cfp = (ConstantFP)c;
			double val = cfp.getValue();
			if (val == +0.0)
				bmi(mbb, idx++, X86InstrSets.FLD0, 0, reg);
			else if (val == +1.0)
				bmi(mbb, idx++, X86InstrSets.FLD1, 0, reg);
			else
			{
				// Otherwise, we need to spill it into constant pool.
				 MachineConstantPool pool = mf.getConstantPool();
				int cpi = pool.getConstantPoolIndex(cfp);
				Type ty = cfp.getType();
				assert ty == Type.FloatTy || ty==Type.DoubleTy;
				int i = ty == Type.FloatTy?0:1;
				int[] ops = {X86InstrSets.FLDr32, X86InstrSets.FLDr64};
				addConstantPoolReference(bmi(mbb, idx++, ops[i], 4, reg), cpi, 0);
			}
		}
		else if (c instanceof ConstantPointerNull)
		{
			// Copy zero (null pointer) to the register.
			bmi(mbb, idx++, X86InstrSets.MOVir32, 1, reg).addZImm(0);
		}
		else
		{
			System.err.println("Illegal constant:" + c);
			assert  false:"Type not handled yet!";
		}
	}

	/**
	 * This method is similar to getReg(Value val), but the only one
	 * difference is that there are additional instructions maybe
	 * appended into the end of mbb.
	 * @param val
	 * @param mbb
	 * @param idx
	 * @return
	 */
	private int getReg(Value val, MachineBasicBlock mbb, int idx)
	{
		Integer reg = regMap.get(val);
		if (reg == null)
		{
			reg = makeAnotherReg(val.getType());
			regMap.put(val, reg);
		}

		// if this value is a constant, emit the code to copy the constant
		// into register.
		if (val instanceof GlobalValue)
		{
			// move the address of global value into the register.
			bmi(mbb, idx, X86InstrSets.MOVir32, 1, reg).
					addGlobalAddress((GlobalValue)val, false).getMInstr();
		}
		else if (val instanceof Constant)
		{
			copyConstToReg(idx, (Constant)val, reg);
			// remove this constant since it is moved to register.
			regMap.remove(val);
		}
		return reg;
	}

	/**
	 * This method used for turning HIR value to virtual register number.
	 * @param val
	 * @return
	 */
	private int getReg(Value val)
	{
		return getReg(val, mbb, mbb.size()-1);
	}

	private static int getClass(Type ty)
	{
		switch (ty.getPrimitiveID())
		{
			case Type.Int1TyID:
			case Type.Int8TyID:
				return TypeClass.i8;
			case Type.Int16TyID:
				return TypeClass.i16;
			case Type.Int32TyID:
				return TypeClass.i32;
			case Type.Int64TyID:
				return i64;
			case Type.FloatTyID:
				return TypeClass.f32;
			case Type.DoubleTyID:
				return TypeClass.f64;
			default:
				assert false:"Invalid type to getClass()!";
				return TypeClass.i32;  // default to i32.
		}
	}

	private void assignArgumentsWithVirtualReg(Function f)
	{
		// Emit instructions to load the arguments...  On entry to a function on the
		// X86, the stack frame looks like this:
		//
		// [ESP] -- return address
		// [ESP + 4] -- first argument (leftmost lexically)
		// [ESP + 8] -- second argument, if first argument is four bytes in size

		int argOffset = 0;
		MachineFrameInfo mfi = mf.getFrameInfo();

		for (Argument arg : f.getArgumentList())
		{
			int reg = getReg(arg);
			int fi;
			int typeSize = (int)tm.getTargetData().getTypeSize(arg.getType());
			fi = mfi.createFixedObject(typeSize, argOffset);

			switch (getClass(arg.getType()))
			{
				case TypeClass.i8:
					addFrameReference(buildMI(mbb, X86InstrSets.MOVmr8, 4, reg), fi);
					break;
				case TypeClass.i16:
					addFrameReference(buildMI(mbb, X86InstrSets.MOVmr16, 4, reg), fi);
					break;
				case TypeClass.i32:
					addFrameReference(buildMI(mbb, X86InstrSets.MOVmr32, 4, reg), fi);
					break;
				case TypeClass.i64:
					addFrameReference(buildMI(mbb, X86InstrSets.MOVmr32, 4, reg), fi);
					addFrameReference(buildMI(mbb, X86InstrSets.MOVmr32, 4, reg+1), fi, 4);
					break;
				case TypeClass.f32:
					addFrameReference(buildMI(mbb, X86InstrSets.FLDr32, 4, reg), fi);
					break;
				case TypeClass.f64:
					addFrameReference(buildMI(mbb, X86InstrSets.FLDr64, 4, reg), fi);
					break;
				case TypeClass.f80:
					addFrameReference(buildMI(mbb, X86InstrSets.FLDr80, 4, reg), fi);
					argOffset += 2; // f80 requires 2 additional bytes.
					break;
				default:
					assert false:"Unknown type class in assignArgumentsWithVirtualReg()";
					break;
			}
			// advance argument offset to the next slot position.
			argOffset += typeSize;
		}
		if (f.getFunctionType().isVarArgs())
			varArgsFrameIndex = mfi.createFixedObject(1, argOffset);
	}

	/**
	 * Insert machine code to generate phis.  This is tricky
	 * because we have to generate our sources into the source basic blocks, not
	 * the current one.
	 */
	private void selectPhiNodes()
	{
		TargetInstrInfo targetInstrInfo = tm.getInstrInfo();
		Function f = mf.getFunction(); // obtains the HIR function.
		for (BasicBlock bb : f.getBasicBlockList())
		{
			MachineBasicBlock mbb = mbbMap.get(bb);

			// a index into a position offset head of mbb where all phi node
			// will be inserts.
			// We must ensure that all Phi nodes will be inserted prior to
			// other non-Phi instr.
			int numPhis = 0;
			for (int i = 0, e = bb.size();
			     i < e && (bb.getInstAt(i) instanceof PhiNode);
			     i++)
			{
				PhiNode phiNode = (PhiNode)bb.getInstAt(i);
				// creates a machine phi node and insert it into mbb.
				int phiReg = getReg(phiNode);
				MachineInstr phiMI = buildMI(X86InstrSets.PHI, phiNode.getNumOfOperands(),
						phiReg).getMInstr();
				mbb.insert(numPhis++, phiMI);

				// special handling for typed of i64.
				MachineInstr longPhiMI = null;
				if (phiNode.getType() == Type.Int64Ty)
				{
					longPhiMI = buildMI(X86InstrSets.PHI, phiNode.getNumOfOperands(),
							phiReg+1).getMInstr();
					mbb.insert(numPhis++, longPhiMI);
				}

				// with a hashmap for mapping BasicBlock into virtual register
				// number, so that we are only initialize one incoming value
				// for a particular block even if the block has multiply entries
				// int Phi node.
				HashMap<MachineBasicBlock, Integer> phiValues = new HashMap<>();
				for (int j =0, size = phiNode.getNumberIncomingValues();
				     j < size; j++)
				{
					BasicBlock incomingBB = phiNode.getIncomingBlock(i);
					MachineBasicBlock predMBB = mbbMap.get(incomingBB);

					int valReg = 0;
					if (phiValues.containsKey(predMBB))
					{
						// we already inserted an initialization of the register
						// for this predecessor.
						valReg = phiValues.get(predMBB);
					}
					else
					{
						// compute a virtual register for incoming value,
						// and insert it into phiValues.
						Value incomingVal = phiNode.getIncomingValue(i);
						if (incomingVal instanceof Constant)
						{
							// If the incomingVal is a constant, we should insert
							// code in the predecessor to compute a virtual register.

							// skip all machine phi nodes.
							int k = 0;
							for (int sz = predMBB.size();
							     k < sz && predMBB.getInstAt(k).getOpCode() == X86InstrSets.PHI;
							     k++);

							valReg = getReg(incomingVal, predMBB, k);
						}
						else
							valReg = getReg(incomingVal);

						phiValues.put(predMBB, valReg);
					}

					phiMI.addRegOperand(valReg, UseType.Use);
					phiMI.addMachineBasicBlockOperand(predMBB);
					if (longPhiMI != null)
					{
						phiMI.addRegOperand(valReg+1, UseType.Use);
						phiMI.addMachineBasicBlockOperand(predMBB);
					}
				}
			}
		}
	}

	private void visitSimpleBinary(Op2 inst, int operatorClass)
	{
		int destReg = getReg(inst);
		emitSimpleBinaryOperation(mbb.size()-1, inst.operand(0),
				inst.operand(1), operatorClass, destReg);
	}

	private void visitDivRem(Op2 inst, boolean isDiv)
	{
		int op1Reg = getReg(inst.operand(0));
		int op2Reg = getReg(inst.operand(1));
		int resultReg = getReg(inst);

		emitDivRemOperation(mbb.size()-1, op1Reg, op2Reg,
				inst.getType(), isDiv, resultReg);
	}

	/**
	 * Here we are interested in meeting the x86 ABI.  As such,
	 * we have the following possibilities:
	 *
	 *   ret void: No return value, simply emit a 'ret' instruction
	 *   ret sbyte, ubyte : Extend value into EAX and return
	 *   ret short, ushort: Extend value into EAX and return
	 *   ret int, uint    : Move value into EAX and return
	 *   ret pointer      : Move value into EAX and return
	 *   ret long, ulong  : Move value into EAX/EDX and return
	 *   ret float/double : Top of FP stack
	 * @param inst
	 * @return
	 */
	@Override
	public Void visitRet(Instruction.ReturnInst inst)
	{
		if (inst.getNumOfOperands() == 0)
		{
			// emit 'ret'.
			buildMI(mbb, X86InstrSets.RET, 0);
			return null;
		}

		Value retValue = inst.operand(0);
		int retReg = getReg(retValue);
		switch (getClass(retValue.getType()))
		{
			case i8:
			case i16:
			case i32:
				promote32(X86RegsSet.EAX, new ValueRecord(retReg, retValue.getType()));
				// Declare that EAX are live on exit
				buildMI(mbb, X86InstrSets.IMPLICIT_USE, 2).addReg(X86RegsSet.EAX)
						.addReg(X86RegsSet.ESP);
				break;
			case f32:
			case f64:
			case f80:
				buildMI(mbb, X86InstrSets.FpGETRESULT, 1).addReg(retReg);
				// Declare that ST0 are live on exit
				buildMI(mbb, X86InstrSets.IMPLICIT_USE, 2).
						addReg(X86RegsSet.ST0).addReg(X86RegsSet.ESP);
				break;
			case i64:
				buildMI(mbb, X86InstrSets.MOVrr32, 1, X86RegsSet.EAX).
						addReg(retReg);
				buildMI(mbb, X86InstrSets.MOVrr32, 1, X86RegsSet.EDX).
						addReg(retReg+1);
				// Declare that EAX & EDX are live on exit
				buildMI(mbb, X86InstrSets.IMPLICIT_USE, 3).
						addReg(X86RegsSet.EAX).
						addReg(X86RegsSet.EDX).
						addReg(X86RegsSet.ESP);
				break;
			default:
				assert false:"Invalid type class in ret";
		}
		// Emit a 'ret' instruction.
		buildMI(mbb, X86InstrSets.RET, 0);
		return null;
	}
	// Return the basic block which occurs lexically after the
	// specified one.
	private static BasicBlock getBlockAfter(BasicBlock bb)
	{
		Iterator<BasicBlock> itr = bb.getParent().getBasicBlockList().iterator();
		while(itr.hasNext())
		{
			BasicBlock next = itr.next();
			if (next == bb)
			{
				return itr.hasNext()? itr.next():null;
			}
		}
		return null;
	}

	@Override
	public Void visitBr(Instruction.BranchInst inst)
	{
		BasicBlock nextBB = getBlockAfter(inst.getParent());
		if (inst.isUnconditional())
		{
			if (inst.suxAt(0) != nextBB)
				buildMI(mbb, X86InstrSets.JMP, 1).addPCDisp(inst.suxAt(0));
			return null;
		}

		int condReg = getReg(inst.getCondition());
		buildMI(mbb, X86InstrSets.CMPri8, 2).addReg(condReg).addZImm(0);
		if (inst.suxAt(1) == nextBB)
		{
			if (inst.suxAt(0) != nextBB)
				buildMI(mbb, X86InstrSets.JNE, 1).addPCDisp(inst.suxAt(0));
		}
		else
		{
			buildMI(mbb, X86InstrSets.JE, 1).addPCDisp(inst.suxAt(1));

			if (inst.suxAt(0) != nextBB)
				buildMI(mbb, X86InstrSets.JMP, 1).addPCDisp(inst.suxAt(0));
		}
		return null;
	}

	@Override
	public Void visitSwitch(Instruction.SwitchInst inst)
	{
		assert false:"Switch inst have not been lowered into chained br as yet?";
		return null;
	}

	@Override
	public Void visitAdd(Op2 inst)
	{
		visitSimpleBinary(inst, 0);
		return null;
	}

	@Override
	public Void visitFAdd(Op2 inst)
	{
		// TODO
		int op0Reg = getReg(inst.operand(0));
		int op1Reg = getReg(inst.operand(1));
		int destReg = getReg(inst);

		int typeClass = getClass(inst.getType());
		assert typeClass>=f32 && typeClass<=f80:"The type class is not a fp?";
		buildMI(mbb, X86InstrSets.FpADD, 2, destReg).addReg(op0Reg).addReg(op1Reg);
		return null;
	}

	@Override
	public Void visitSub(Op2 inst)
	{
		visitSimpleBinary(inst, 1);
		return null;
	}

	@Override
	public Void visitFSub(Op2 inst)
	{
		// TODO
		int op0Reg = getReg(inst.operand(0));
		int op1Reg = getReg(inst.operand(1));
		int destReg = getReg(inst);

		int typeClass = getClass(inst.getType());
		assert typeClass>=f32 && typeClass<=f80:"The type class is not a fp?";
		buildMI(mbb, X86InstrSets.FpSUB, 2, destReg).addReg(op0Reg).addReg(op1Reg);
		return null;
	}

	@Override
	public Void visitMul(Op2 inst)
	{
		int op0Reg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		if (inst.getType() != Type.Int64Ty)
		{
			// mul op0Reg, const.
			if (inst.operand(1) instanceof ConstantInt)
			{
				ConstantInt ci = (ConstantInt)inst.operand(1);
				// it must be a 32-bit integer.
				int val = (int)ci.getZExtValue();
				doMultiplyConst(mbb.size()-1, destReg, inst.getType(), op0Reg, val);
			}
			else
			{
				// mul op0Reg, op1Reg.
				int op1Reg = getReg(inst.operand(1));
				doMultiply(mbb.size()-1, destReg, inst.getType(), op0Reg, op1Reg);;
			}
		}
		else
		{
			int op1Reg = getReg(inst.operand(1));

			// lower multiply.
			buildMI(mbb, X86InstrSets.MOVrr32, 1, X86RegsSet.EAX).addReg(op0Reg);
			buildMI(mbb, X86InstrSets.MULr32, 1).addReg(op1Reg);

			int overflowReg = makeAnotherReg(Type.Int32Ty);
			buildMI(mbb, X86InstrSets.MOVrr32, 1, destReg).addReg(X86RegsSet.EAX);
			buildMI(mbb, X86InstrSets.MOVrr32, 1, overflowReg).addReg(X86RegsSet.EDX);

			int ahblReg = makeAnotherReg(Type.Int32Ty);
			bmi(mbb, mbb.size()-1, X86InstrSets.IMULrr32, 2, ahblReg).
					addReg(op0Reg+1).addReg(op1Reg);

			int ahbPlusOverlowReg = makeAnotherReg(Type.Int32Ty);
			buildMI(mbb, X86InstrSets.ADDrr32, 2, ahbPlusOverlowReg).
					addReg(ahblReg).addReg(overflowReg);

			int albHReg = makeAnotherReg(Type.Int32Ty);
			bmi(mbb, mbb.size()-1, X86InstrSets.IMULrr32, 2, albHReg).
					addReg(op0Reg).addReg(op1Reg+1);

			buildMI(mbb, X86InstrSets.ADDrr32, 2, destReg+1).
					addReg(ahbPlusOverlowReg).addReg(albHReg);
		}
		return null;
	}

	@Override
	public Void visitFMul(Op2 inst)
	{
		int op0Reg = getReg(inst.operand(0));
		int op1Reg = getReg(inst.operand(1));
		int destReg = getReg(inst);
		doMultiply(mbb.size()-1, destReg, inst.getType(), op0Reg, op1Reg);
		return null;
	}

	@Override
	public Void visitUDiv(Op2 inst)
	{
		visitDivRem(inst, true);
		return null;
	}

	@Override
	public Void visitSDiv(Op2 inst)
	{
		visitDivRem(inst, true);
		return null;
	}

	@Override
	public Void visitFDiv(Op2 inst)
	{
		visitDivRem(inst, true);
		return null;
	}

	@Override
	public Void visitURem(Op2 inst)
	{
		visitDivRem(inst, false);
		return null;
	}

	@Override
	public Void visitSRem(Op2 inst)
	{
		visitDivRem(inst, false);
		return null;
	}

	@Override
	public Void visitFRem(Op2 inst)
	{
		visitDivRem(inst, false);
		return null;
	}

	@Override
	public Void visitAnd(Op2 inst)
	{
		visitSimpleBinary(inst, 2);
		return null;
	}

	@Override
	public Void visitOr(Op2 inst)
	{
		visitSimpleBinary(inst, 3);
		return null;
	}

	@Override
	public Void visitXor(Op2 inst)
	{
		visitSimpleBinary(inst, 4);
		return null;
	}

	private void emitShfitOperator(Op2 inst, boolean isLeftShift)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);
		boolean isSigned = inst.getType().isSigned();
		int klass = getClass(inst.getType());

		int[][] ConstantOperand =
		{
			{ X86InstrSets.SHRir8, X86InstrSets.SHRir16, X86InstrSets.SHRir32, X86InstrSets.SHRDir32 },  // SHR
			{ X86InstrSets.SARir8, X86InstrSets.SARir16, X86InstrSets.SARir32, X86InstrSets.SHRDir32 },  // SAR
			{ X86InstrSets.SHLir8, X86InstrSets.SHLir16, X86InstrSets.SHLir32, X86InstrSets.SHLDir32 },  // SHL
			{ X86InstrSets.SHLir8, X86InstrSets.SHLir16, X86InstrSets.SHLir32, X86InstrSets.SHLDir32 },  // SAL = SHL
		};

		int[][] NonConstantOperand =
		{
			{ X86InstrSets.SHRrCL8, X86InstrSets.SHRrCL16, X86InstrSets.SHRrCL32},  // SHR
			{ X86InstrSets.SARrCL8, X86InstrSets.SARrCL16, X86InstrSets.SARrCL32},  // SAR
			{ X86InstrSets.SHLrCL8, X86InstrSets.SHLrCL16, X86InstrSets.SHLrCL32},  // SHL
			{ X86InstrSets.SHLrCL8, X86InstrSets.SHLrCL16, X86InstrSets.SHLrCL32},  // SAL = SHL
		};

		// Longs, as usual, are handled specially...
		if (klass == i64) {
			// If we have a constant shift, we can generate much more efficient code
			// than otherwise...
			//
			if (inst.operand(1) instanceof ConstantInt)
			{
				ConstantInt cui = (ConstantInt)inst.operand(0);
				int Amount = (int)cui.getZExtValue();
				if (Amount < 32) {
					int[] Opc = ConstantOperand[(isLeftShift?1:0)*2 + (isSigned?1:0)];
					if (isLeftShift) {
						buildMI(mbb, Opc[3], 3,
								destReg+1).addReg(srcReg+1).addReg(srcReg).addZImm(Amount);
						buildMI(mbb, Opc[2], 2, destReg).addReg(srcReg).addZImm(Amount);
					} else {
						buildMI(mbb, Opc[3], 3,
								destReg).addReg(srcReg  ).addReg(srcReg+1).addZImm(Amount);
						buildMI(mbb, Opc[2], 2, destReg+1).addReg(srcReg+1).addZImm(Amount);
					}
				} else {                 // Shifting more than 32 bits
					Amount -= 32;
					if (isLeftShift) {
						buildMI(mbb, X86InstrSets.SHLir32, 2,destReg+1).addReg(srcReg).addZImm(Amount);
						buildMI(mbb, X86InstrSets.MOVir32, 1,destReg  ).addZImm(0);
					} else {
						int Opcode = isSigned ? X86InstrSets.SARir32 : X86InstrSets.SHRir32;
						buildMI(mbb, Opcode, 2, destReg).addReg(srcReg+1).addZImm(Amount);
						buildMI(mbb, X86InstrSets.MOVir32, 1, destReg+1).addZImm(0);
					}
				}
			} else {
				int TmpReg = makeAnotherReg(Type.Int32Ty);

				if (!isLeftShift && isSigned) {
					// If this is a SHR of a Long, then we need to do funny sign extension
					// stuff.  TmpReg gets the value to use as the high-part if we are
					// shifting more than 32 bits.
					buildMI(mbb, X86InstrSets.SARir32, 2, TmpReg).addReg(srcReg).addZImm(31);
				} else {
					// Other shifts use a fixed zero value if the shift is more than 32
					// bits.
					buildMI(mbb, X86InstrSets.MOVir32, 1, TmpReg).addZImm(0);
				}

				// Initialize CL with the shift amount...
				int ShiftAmount = getReg(inst.operand(1));
				buildMI(mbb, X86InstrSets.MOVrr8, 1, X86RegsSet.CL).addReg(ShiftAmount);

				int TmpReg2 = makeAnotherReg(Type.Int32Ty);
				int TmpReg3 = makeAnotherReg(Type.Int32Ty);
				if (isLeftShift) {
					// TmpReg2 = shld inHi, inLo
					buildMI(mbb, X86InstrSets.SHLDrCL32, 2, TmpReg2).addReg(srcReg+1).addReg(srcReg);
					// TmpReg3 = shl  inLo, CL
					buildMI(mbb, X86InstrSets.SHLrCL32, 1, TmpReg3).addReg(srcReg);

					// Set the flags to indicate whether the shift was by more than 32 bits.
					buildMI(mbb, X86InstrSets.TESTri8, 2).addReg(X86RegsSet.CL).addZImm(32);

					// DestHi = (>32) ? TmpReg3 : TmpReg2;
					buildMI(mbb, X86InstrSets.CMOVNErr32, 2,
							destReg+1).addReg(TmpReg2).addReg(TmpReg3);
					// DestLo = (>32) ? TmpReg : TmpReg3;
					buildMI(mbb, X86InstrSets.CMOVNErr32, 2, destReg).addReg(TmpReg3).addReg(TmpReg);
				} else {
					// TmpReg2 = shrd inLo, inHi
					buildMI(mbb, X86InstrSets.SHRDrCL32, 2, TmpReg2).addReg(srcReg).addReg(srcReg+1);
					// TmpReg3 = s[ah]r  inHi, CL
					buildMI(mbb, isSigned ? X86InstrSets.SARrCL32 : X86InstrSets.SHRrCL32, 1, TmpReg3)
							.addReg(srcReg+1);

					// Set the flags to indicate whether the shift was by more than 32 bits.
					buildMI(mbb, X86InstrSets.TESTri8, 2).addReg(X86RegsSet.CL).addZImm(32);

					// DestLo = (>32) ? TmpReg3 : TmpReg2;
					buildMI(mbb, X86InstrSets.CMOVNErr32, 2,
							destReg).addReg(TmpReg2).addReg(TmpReg3);

					// DestHi = (>32) ? TmpReg : TmpReg3;
					buildMI(mbb, X86InstrSets.CMOVNErr32, 2,
							destReg+1).addReg(TmpReg3).addReg(TmpReg);
				}
			}
			return;
		}

		if (inst.operand(1) instanceof ConstantInt)
		{
			ConstantInt cui = (ConstantInt)inst.operand(1);
			// The shift amount is constant, guaranteed to be a ubyte. Get its value.
			assert cui.getType() == Type.Int8Ty : "Shift amount not a ubyte?";

			int[] opc = ConstantOperand[(isLeftShift?1:0)*2+(isSigned?1:0)];
			buildMI(mbb, opc[klass], 2, destReg).addReg(srcReg).addZImm(cui.getZExtValue());
		} else {                  // The shift amount is non-constant.
			buildMI(mbb, X86InstrSets.MOVrr8, 1, X86RegsSet.CL).addReg(getReg(inst.operand(1)));

			int[] opc = ConstantOperand[(isLeftShift?1:0)*2+(isSigned?1:0)];
			buildMI(mbb, opc[klass], 1, destReg).addReg(srcReg);
		}
	}
	@Override
	public Void visitShl(Op2 inst)
	{
		emitShfitOperator(inst, true);
		return null;
	}

	@Override
	public Void visitLShr(Op2 inst)
	{
		emitShfitOperator(inst, false);
		return null;
	}

	@Override
	public Void visitAShr(Op2 inst)
	{
		emitShfitOperator(inst, false);
		return null;
	}

	private int getSetCCNumber(CmpInst.Predicate pred)
	{
		assert pred.ordinal()>= FIRST_ICMP_PREDICATE.ordinal()
				&& pred.ordinal()<= LAST_ICMP_PREDICATE.ordinal()
				:"Invalid predicate for ICmp instr";
		switch (pred)
		{
			case ICMP_EQ:
				return X86InstrSets.SETEr;
			case ICMP_NE:
				return X86InstrSets.SETNEr;
			case ICMP_UGT:
				return X86InstrSets.SETAr;
			case ICMP_UGE:
				return X86InstrSets.SETAEr;
			case ICMP_ULT:
				return X86InstrSets.SETBr;
			case ICMP_ULE:
				return X86InstrSets.SETBEr;
			case ICMP_SGT:
				return X86InstrSets.SETGr;
			case ICMP_SGE:
				return X86InstrSets.SETGEr;
			case ICMP_SLT:
				return X86InstrSets.SETLr;
			case ICMP_SLE:
				return X86InstrSets.SETLEr;
			default:
				return -1;
		}
	}

	@Override
	public Void visitICmp(ICmpInst inst)
	{
		Type ty = inst.getType();
		int klass = getClass(ty);
		int op0Reg = getReg(inst.operand(0));

		assert klass>=i8 && klass<=i64;

		// obtains the set conditional code.
		int cc = getSetCCNumber(inst.getPredicate());

		if (klass>=i8 && klass<=i32)
		{
			if (inst.operand(1) instanceof ConstantInt)
			{
				ConstantInt op1CI = (ConstantInt)inst.operand(1);
				long op1Val = op1CI.getZExtValue();

				if (op1Val == 0 && (ty.isSigned() ||
							cc == X86InstrSets.SETEr ||
							cc == X86InstrSets.SETNEr))
				{
					int[] testTab = {X86InstrSets.TESTrr8, X86InstrSets.TESTrr16,
							X86InstrSets.TESTrr32};
					buildMI(mbb, testTab[klass], 2).addReg(op0Reg).addReg(op0Reg);
					return null;
				}

				int[] testTab = {X86InstrSets.CMPri8, X86InstrSets.CMPri16,
						X86InstrSets.CMPri32};
				buildMI(mbb, testTab[klass], 2).addReg(op0Reg).addZImm(op1Val);
				return null;
			}
		}

		int op1Reg = getReg(inst.operand(1));
		switch (klass)
		{
			case i8:
				buildMI(mbb, X86InstrSets.CMPrr8, 2).addReg(op0Reg).addReg(op1Reg);
				break;
			case i16:
				buildMI(mbb, X86InstrSets.CMPrr16, 2).addReg(op0Reg).addReg(op1Reg);
				break;
			case i32:
				buildMI(mbb, X86InstrSets.CMPrr32, 2).addReg(op0Reg).addReg(op1Reg);
				break;
			case i64:
				// TODO compare 64 bit.
		}
		return null;
	}

	@Override
	public Void visitFCmp(FCmpInst inst)
	{
		// allocates two virtual register number for it's two operands.
		int op0Reg = getReg(inst.operand(0));
		int op1Reg = getReg(inst.operand(1));

		// load it into ST(x) register.
		int klass = getClass(inst.getType());
		switch (klass)
		{
			case f32:
			case f64:
				// float comparison.
				buildMI(mbb, X86InstrSets.FpUCOM, 2).addReg(op0Reg).addReg(op1Reg);
				// save the FPU status register in %al.
				buildMI(mbb, X86InstrSets.FNSTSWr8, 0);
				// store AH into flags register, like sign, zero, auxiliary, carry etc.
				buildMI(mbb, X86InstrSets.SAHF, 1);
				break;
			case f80:
				// TODO
				assert false:"Current f80 is not supported";
		}
		return null;
	}

	@Override
	public Void visitTrunc(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());

		assert  srcClass > destClass && srcClass < f32;

		if (srcClass == i64 && destClass == i32)
		{
			buildMI(mbb, X86InstrSets.MOVrr32, 1, destReg).addReg(srcReg);
			return null;
		}
		int regRegMove[] =
		{
			X86InstrSets.MOVrr8,
			X86InstrSets.MOVrr16, X86InstrSets.MOVrr32,
				X86InstrSets.FpMOV, X86InstrSets.MOVrr32
		};

		// Handle cast of LARGER int to SMALLER int using a move to EAX followed by a
		// move out of AX or AL.
		int areg[] = {X86RegsSet.AL, X86RegsSet.AX, X86RegsSet.EAX, 0, X86RegsSet.EAX};
		buildMI(mbb, regRegMove[srcClass], 1, areg[srcClass]).addReg(srcReg);
		buildMI(mbb, regRegMove[destClass], 1, areg[destClass]).addReg(areg[destReg]);
		return null;
	}

	@Override
	public Void visitZExt(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());

		assert  srcClass < destClass && destClass < f32;

		boolean isLong = destClass == i64;
		if (isLong) destClass = i32;

		int[] opcodes = { X86InstrSets.MOVZXr16r8,
				X86InstrSets.MOVZXr32r8,
				X86InstrSets.MOVZXr32r16,
				X86InstrSets.MOVrr32};

		buildMI(mbb, opcodes[srcClass + destClass - 1], 1, destReg).addReg(srcReg);
		if (isLong)
			buildMI(mbb, X86InstrSets.MOVir32, 1, destReg+1).addZImm(0);
		return null;
	}

	@Override
	public Void visitSExt(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());

		assert  srcClass < destClass && destClass < f32;

		boolean isLong = destClass == i64;
		if (isLong) destClass = i32;

		int[] opcodes = { X86InstrSets.MOVSXr16r8,
				X86InstrSets.MOVSXr32r8,
				X86InstrSets.MOVSXr32r16,
				X86InstrSets.MOVrr32};

		buildMI(mbb, opcodes[srcClass + destClass - 1], 1, destReg).addReg(srcReg);
		if (isLong)
			buildMI(mbb, X86InstrSets.SARir32, 1, destReg+1).
					addReg(destReg).addZImm(31);
		return null;
	}

	private Void emitFPToInt(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());
		Type destTy = inst.getType();

		// Change the floating point control register to use "round towards zero"
		// mode when truncating to an integer value.
		//
		int CWFrameIdx = mf.getFrameInfo().createStackObject(2, 2);
		addFrameReference(buildMI(mbb, X86InstrSets.FNSTCWm16, 4), CWFrameIdx);

		// Load the old value of the high byte of the control word...
		int HighPartOfCW = makeAnotherReg(Type.Int8Ty);
		addFrameReference(buildMI(mbb, X86InstrSets.MOVmr8, 4, HighPartOfCW),
				CWFrameIdx, 1);

		// Set the high part to be round to zero...
		addFrameReference(buildMI(mbb, X86InstrSets.MOVim8, 5), CWFrameIdx, 1)
				.addZImm(12);

		// Reload the modified control word now...
		addFrameReference(buildMI(mbb, X86InstrSets.FLDCWm16, 4), CWFrameIdx);

		// Restore the memory image of control word to original value
		addFrameReference(buildMI(mbb, X86InstrSets.MOVrm8, 5), CWFrameIdx, 1)
				.addReg(HighPartOfCW);

		// We don't have the facilities for directly storing byte sized data to
		// memory.  Promote it to 16 bits.  We also must promote int values to
		// larger classes because we only have signed FP stores.
		int StoreClass = destClass;
		Type storeTy = destTy;
		if (StoreClass == i8 || destTy.isUnsigned())
			switch (StoreClass)
			{
				case i8:
					storeTy = Type.Int1Ty;
					StoreClass = i16;
					break;
				case i16:
					storeTy = Type.Int32Ty;
					StoreClass = i32;
					break;
				case i32:
					storeTy = Type.Int64Ty;
					StoreClass = i64;
					break;
				// The following treatment of i64 may not be perfectly right,
				// but it survives chains of casts of the form
				// double.ulong.double.
				case i64:
					storeTy = Type.Int64Ty;
					StoreClass = i64;
					break;
				default:
					assert false : "Unknown store class!";
			}

		// Spill the integer to memory and reload it from there...
		int FrameIdx = mf.getFrameInfo()
				.createStackObject(storeTy, tm.getTargetData());

		int Op1[] = { 0, X86InstrSets.FISTr16, X86InstrSets.FISTr32, 0,
				X86InstrSets.FISTPr64 };
		addFrameReference(buildMI(mbb, Op1[StoreClass], 5), FrameIdx)
				.addReg(srcReg);

		if (destClass == i64)
		{
			addFrameReference(buildMI(mbb, X86InstrSets.MOVmr32, 4, destReg),
					FrameIdx);
			addFrameReference(
					buildMI(mbb, X86InstrSets.MOVmr32, 4, destReg + 1),
					FrameIdx, 4);
		}
		else
		{
			int Op2[] = { X86InstrSets.MOVmr8, X86InstrSets.MOVmr16,
					X86InstrSets.MOVmr32 };
			addFrameReference(buildMI(mbb, Op2[destClass], 4, destReg),
					FrameIdx);
		}

		// Reload the original control word now...
		addFrameReference(buildMI(mbb, X86InstrSets.FLDCWm16, 4), CWFrameIdx);
		return null;
	}

	@Override
	public Void visitFPToUI(CastInst inst)
	{
		return emitFPToInt(inst);
	}

	@Override
	public Void visitFPToSI(CastInst inst)
	{
		return emitFPToInt(inst);
	}

	private Void emitIntToFP(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());

		// Promote the integer to a type supported by FLD.  We do this because there
		// are no int FLD instructions, so we must promote an int value to
		// a larger signed value, then use FLD on the larger value.
		Type srcTy = inst.operand(0).getType();
		Type promoteType = null;
		int promoteOpcode = 0;
		switch (srcTy.getPrimitiveID())
		{
			case Type.Int1TyID:
			case Type.Int8TyID:
				promoteType = Type.Int16Ty;
				promoteOpcode = srcTy.isSigned()?
						X86InstrSets.MOVSXr16r8: X86InstrSets.MOVZXr16r8;
				break;
			case Type.Int16TyID:
				promoteType = Type.Int32Ty;
				promoteOpcode = srcTy.isSigned()?
						X86InstrSets.MOVSXr32r16:X86InstrSets.MOVZXr32r16;
				break;
			case Type.Int32TyID:
				int tempReg = makeAnotherReg(Type.Int64Ty);
				buildMI(mbb, X86InstrSets.MOVrr32, 1, tempReg).addReg(srcReg);
				buildMI(mbb, X86InstrSets.MOVir32, 1, tempReg+1).addZImm(0);
				srcTy = Type.Int64Ty;
				srcClass = i64;
				srcReg = tempReg;
				break;
			default:
				assert false:"Not implemented: cast long to float point type.";
		}
		if (promoteType != null)
		{
			int tempReg = makeAnotherReg(promoteType);
			buildMI(mbb, srcTy.isSigned()?X86InstrSets.MOVSXr16r8:X86InstrSets.MOVZXr16r8
					, 1, tempReg).addReg(srcReg);
			srcTy = promoteType;
			srcClass = getClass(promoteType);
			srcReg = tempReg;
		}

		int frameIdx = mf.getFrameInfo().createStackObject(srcTy, tm.getTargetData());

		if (srcClass == i64)
		{
			addFrameReference(buildMI(mbb, X86InstrSets.MOVrm32, 5), frameIdx).addReg(srcReg);
			addFrameReference(buildMI(mbb, X86InstrSets.MOVrm32, 5), frameIdx, 4).addReg(srcReg+1);
		}
		else
		{
			int[] op1s = { X86InstrSets.MOVrm8, X86InstrSets.MOVrm16, X86InstrSets.MOVrm32};
			addFrameReference(buildMI(mbb, op1s[srcClass], 5), frameIdx).addReg(srcReg);
		}

		int[] op2s = {0, X86InstrSets.FLDr16, X86InstrSets.FLDr32, 0, X86InstrSets.FLDr64};
		addFrameReference(buildMI(mbb, op2s[srcClass], 5, destReg), frameIdx);
		return null;
	}

	@Override
	public Void visitUIToFP(CastInst inst)
	{
		return emitIntToFP(inst);
	}

	@Override
	public Void visitSIToFP(CastInst inst)
	{
		return emitIntToFP(inst);
	}

	@Override
	public Void visitFPTrunc(CastInst inst)
	{
		// TODO
		return null;
	}

	@Override
	public Void visistFPExt(CastInst inst)
	{
		// TODO
		return null;
	}

	@Override
	public Void visitPtrToInt(CastInst inst)
	{
		// TODO
		return null;
	}

	@Override
	public Void visitIntToPtr(CastInst inst)
	{
		// TODO
		return null;
	}

	@Override
	public Void visitBitCast(CastInst inst)
	{
		int[] opcodes = {X86InstrSets.MOVrr8, X86InstrSets.MOVrr16, X86InstrSets.MOVrr32,
				X86InstrSets.MOVrr32};
		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());
		assert srcClass == destClass && srcClass < f32;

		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);
		buildMI(mbb, opcodes[srcClass], 1, destReg).addReg(srcReg);
		if (srcClass == i64)
		{
			buildMI(mbb, opcodes[srcClass], 1, destReg+1).addReg(srcReg+1);
		}
		return null;
	}

	@Override
	public Void visitAlloca(Instruction.AllocaInst inst)
	{
		Type ty = inst.getAllocatedType();
		int tySize = (int) tm.getTargetData().getTypeSize(ty);

		if (inst.getArraySize() instanceof ConstantInt)
		{
			if (inst.getParent() == inst.getParent().getParent().getEntryBlock())
			{
				ConstantInt ci = (ConstantInt) inst.getArraySize();
				tySize *= ci.getZExtValue();
				int align = tm.getTargetData().getTypeAlign(ty);

				int frameIdx = mf.getFrameInfo().createStackObject(tySize, align);
				addFrameReference(buildMI(mbb, X86InstrSets.LEAr32, 5, getReg(inst)), frameIdx);
			}
		}

		// create a register to hold the temporary result of multipling the type size.
		int totalSizeReg = makeAnotherReg(Type.Int32Ty);
		int srcReg = getReg(inst.getArraySize());

		// totalSize = tySize * array length.
		doMultiplyConst(mbb.size()-1, totalSizeReg, Type.Int32Ty, srcReg, tySize);

		// following instruction for alignment in 16Byte.
		// addedSizeReg = add totalSizeReg, 15.
		int addedSizeReg = makeAnotherReg(Type.Int32Ty);
		buildMI(mbb, X86InstrSets.ADDri32, 2, addedSizeReg).
				addReg(totalSizeReg).addZImm(15);
		// alignSizeReg = and addedSizeReg, ~15.
		int alignSizeReg = makeAnotherReg(Type.Int32Ty);
		buildMI(mbb, X86InstrSets.ANDri32, 2, alignSizeReg).
				addReg(addedSizeReg).addZImm(~15);

		// subtract size from stack pointer, thereby allocating some space.
		buildMI(mbb, X86InstrSets.SUBrr32, 2, X86RegsSet.ESP).
				addReg(X86RegsSet.ESP).addReg(alignSizeReg);

		// Put a pointer to the space into the result register, by copying
		// the stack pointer.
		buildMI(mbb, X86InstrSets.MOVrr32, 1, getReg(inst)).addReg(X86RegsSet.ESP);

		// Inform the frame information that we have just allocated a
		// variable sized object.
		mf.getFrameInfo().createVariableSizedObject();
		return null;
	}

	@Override
	public Void visitLoad(Instruction.LoadInst inst)
	{
		int srcAddReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int klass = getClass(inst.getType());
		if (klass == i64)
		{
			addDirectMem(buildMI(mbb, X86InstrSets.MOVmr32, 4, destReg), srcAddReg);
			addRegOffset(buildMI(mbb, X86InstrSets.MOVmr32, 4, destReg+1), srcAddReg, 4);
		}

		int[] opcodes = {X86InstrSets.MOVmr8, X86InstrSets.MOVmr16,
				X86InstrSets.MOVmr32,
				X86InstrSets.FLDr32,
				X86InstrSets.FLDr64};
		int opcode = opcodes[klass];
		addDirectMem(buildMI(mbb, opcode, 4, destReg), srcAddReg);
		return null;
	}

	@Override
	public Void visitStore(StoreInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destAddrReg = getReg(inst.operand(1));

		int klass = getClass(inst.operand(0).getType());
		if (klass == i64)
		{
			addDirectMem(buildMI(mbb, X86InstrSets.MOVrm32, 1+4), destAddrReg).
					addReg(srcReg);
			addRegOffset(buildMI(mbb, X86InstrSets.MOVrm32, 1+4), destAddrReg, 4).
					addReg(srcReg+1);
		}

		int[] opcodes = {X86InstrSets.MOVrm8, X86InstrSets.MOVrm16,
				X86InstrSets.MOVrm32,
				X86InstrSets.FSTr32,
				X86InstrSets.FSTr64};
		int opcode = opcodes[klass];
		addDirectMem(buildMI(mbb, opcode, 1+4), destAddrReg).addReg(srcReg);
		return null;
	}

	/**
	 * Push args on stack and do a procedure call instruction.
	 * @param inst
	 * @return
	 */
	@Override
	public Void visitCall(CallInst inst)
	{
		MachineInstr call = null;
		Function f = null;
		if ((f = inst.getCalledFunction()) != null)
		{
			// direct call to function.
			// handle intrinsic call.

			// emit a CALL instruction with PC-relative displacement.
			call = buildMI(X86InstrSets.CALLpcrel32, 1).
					addGlobalAddress(f, true).getMInstr();
		}
		else
		{
			// indirect call.
			int reg = getReg(inst.getCalledValue());
			call = buildMI(X86InstrSets.CALLr32, 1).addReg(reg).getMInstr();
		}

		ArrayList<ValueRecord> args = new ArrayList<>();
		for (int i = 0, e = inst.getNumsOfArgs(); i < e; i++)
		{
			args.add(new ValueRecord(inst.argumentAt(i)));
		}
		int destReg = inst.getType() != Type.VoidTy?getReg(inst) : 0;
		doCall(new ValueRecord(destReg, inst.getType()), call, args);
		return null;
	}

	@Override
	public Void visitGetElementPtr(GetElementPtrInst inst)
	{
		int outputReg = getReg(inst);
		emitGEPOperation(mbb.size()-1, inst.operand(0), inst.getUseList(), outputReg);
		return null;
	}

	/**
	 * This method is noop operation. All PHI Node will be processed in a
	 * single special pass.
	 * @param inst
	 * @return
	 */
	@Override
	public Void visitPhiNode(PhiNode inst)
	{
		Util.shouldNotReachHere("Should not reach here!");
		return null;
	}

	@Override
	public String getPassName()
	{
		return "X86 simple instruction selector.";
	}

	/**
	 * Creates a simple instruction selector for x86 target.
	 * @param tm
	 * @return
	 */
	public static X86SimpleInstSel
		createX86SimpleInstructionSelector(TargetMachine tm)
	{
		return new X86SimpleInstSel(tm);
	}
}
