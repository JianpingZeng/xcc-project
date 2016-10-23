package lir.backend.ia32;

import hir.*;
import hir.BasicBlock;

import java.util.HashMap;
import lir.LIRGenerator;
import lir.LIRItem;
import lir.StackFrame;
import lir.ci.LIRKind;
import lir.ci.LIRRegisterValue;
import lir.ci.LIRValue;
import lir.ci.LIRVariable;
import utils.NumUtil;
import utils.Util;
import compiler.*;

/**
 * @author Xlous.zeng
 */
public final class IA32LIRGenerator extends LIRGenerator
{
	private static final LIRRegisterValue EAX_I = IA32.eax.asValue(LIRKind.Int);
	private static final LIRRegisterValue EAX_L = IA32.eax.asValue(LIRKind.Long);
	private static final LIRRegisterValue EDX_I = IA32.edx.asValue(LIRKind.Int);
	private static final LIRRegisterValue EDX_L = IA32.edx.asValue(LIRKind.Long);

	private static final LIRRegisterValue LDIV_TMP = EDX_L;

	/**
	 * The register in which MUL puts the getReturnValue for 32-bit multiplication.
	 */
	private static final LIRRegisterValue LMUL_OUT = EAX_L;

	private static final LIRRegisterValue SHIFT_COUNT_IN = IA32.ecx
			.asValue(LIRKind.Int);

	protected static final LIRValue ILLEGAL = LIRValue.IllegalValue;

	public IA32LIRGenerator(Backend backend)
	{
		super(backend);
	}

	protected void traceBlockEntry(BasicBlock block)
	{

	}

	protected void traceBlockExit(BasicBlock block)
	{

	}

	// memory access

	public void visitAlloca(Instruction.AllocaInst inst)
	{
		LIRValue result = createResultVirtualRegister(inst);
		assert inst.getArraySize().isConstant() :
				"AllocaInst instruction 'getArraySize' is not a constant " + inst.getArraySize();

		int len = inst.getArraySize().asLIRConstant().asInt();

		// the memory getTypeSize occupied of one element in Byte
		int elemSize = backend.machineInfo.sizeInBytes(inst.kind);

		StackFrame.StackBlock stackBlock = backend.frameMap()
				.reserveStackBlock(len * elemSize, false);
		allocaStackBlock.put(inst, stackBlock);

		lir.alloca(stackBlock, result);
	}

	/**
	 * This hashmap just for mapping from AllocaInst instruction to its allocated
	 * Stack CompoundStmt.
	 */
	private HashMap<Instruction.AllocaInst, StackFrame.StackBlock> allocaStackBlock
			= new HashMap<>();

	/**
	 * Implements store instructions in terms of the IA32 'mov' instruction.
	 * @param inst
	 */
	public void visitStoreInst(Instruction.StoreInst inst)
	{
		LIRVariable valReg = createResultVirtualRegister(inst.value);
		StackFrame.StackBlock stackBlock = allocaStackBlock.get(inst.dest);
		assert stackBlock != null: "AllocaInst isn't allocated in stack";
		lir.move(valReg, backend.frameMap().toStackAddress(stackBlock));
	}

	/**
	 * Implement load instructions in terms of the IA32 'mov' instruction. The
	 * load and store instructions are the only place where we need to worry about
	 * the momory layout of the targetAbstractLayer machine.
	 * @param inst
	 */
	public void visitLoadInst(Instruction.LoadInst inst)
	{
		LIRVariable srcReg = createResultVirtualRegister(inst.from);
		StackFrame.StackBlock stackBlock = allocaStackBlock.get(inst.from);

		assert stackBlock != null: "AllocaInst isn't allocated in stack";

		lir.move(backend.frameMap().toStackAddress(stackBlock), srcReg);
	}

	@Override protected void doIfCmp(Instruction.IfOp instr)
	{
		LIRKind kind = instr.x().kind;

		Condition cond = instr.condition();

		LIRItem xitem = new LIRItem(instr.x(), this);
		LIRItem yitem = new LIRItem(instr.y(), this);
		LIRItem xin = xitem;
		LIRItem yin = yitem;

		if (kind.isLong())
		{
			// for longs, only conditions "eql", "neq", "lss", "geq" are valid;
			// mirror for other conditions
			if (cond == Condition.GT || cond == Condition.LE)
			{
				cond = cond.mirror();
				xin = yitem;
				yin = xitem;
			}
			xin.setDestroysRegister();
		}
		xin.loadItem();
		if (kind.isLong() && yin.result().isConstant()
				&& yin.instruction.asLIRConstant().asLong() == 0 && (
				cond == Condition.EQ || cond == Condition.NE))
		{
			// dont load item
		}
		else if (kind.isLong() || kind.isFloat() || kind.isDouble())
		{
			// longs cannot handle constants at right side
			yin.loadItem();
		}

		clearResult(instr);

		LIRValue left = xin.result();
		LIRValue right = yin.result();
		lir.cmp(cond, left, right);
		if (instr.x().kind.isFloat() || instr.x().kind.isDouble())
		{
			lir.branch(cond, right.kind, instr.getTrueTarget(),
					instr.getFalseTarget());
		}
		else
		{
			lir.branch(cond, right.kind, instr.getTrueTarget());
		}
	}

	@Override
	protected boolean canStoreAsConstant(Value v, LIRKind kind)
	{
		if (kind == LIRKind.Short || kind == LIRKind.Char)
		{
			// there is no immediate move of word VALUES in assembler I486 or later.
			return false;
		}
		return v instanceof Value.Constant;
	}

	@Override
	protected boolean canInlineAsConstant(Value v)
	{
		if (v.kind == LIRKind.Long)
		{
            return v.isConstant() && NumUtil.isInt(v.asLIRConstant().asLong());
        }
		return v.kind != LIRKind.Object || v.isNullConstant();
	}

	protected void arithmeticOp2Float(Instruction.Op2 instr)
	{
		LIRItem left = new LIRItem(instr.x, this);
		LIRItem right = new LIRItem(instr.y, this);
		assert !left.isStack() || !right
				.isStack() : "cann't both be momory reservedOperands";

		// both are register LIROperand, swap reservedOperands such that the short-living
		// one is on the left side
		if (instr.opcode.isCommutative() && left.isRegisterOrVariable() &&
				right.isRegisterOrVariable())
		{
			if (liveLonger(instr.x, instr.y))
			{
				LIRItem temp = left;
				left = right;
				right = temp;
			}
		}
		if (left.isRegisterOrVariable() || instr.x.isConstant())
			left.loadItem();
		right.loadItem();

		LIRVariable reg;
		reg = newVariable(instr.kind);
		arithmeticOpFpu(instr.opcode, reg, left.result(), right.result(),
				ILLEGAL);

		setResult(instr, reg);
	}

	private boolean liveLonger(Value x, Value y)
	{
		if (x instanceof Instruction && y instanceof Instruction)
		{
			BasicBlock xb = ((Instruction) x).getParent();
			BasicBlock yb = ((Instruction) y).getParent();

			if (xb == null || yb == null)
				return false;
			return xb.loopDepth < yb.loopDepth;
		}
		else
			return false;
	}

	protected void arithmeticOp2Long(Instruction.Op2 instr)
	{
		Operator opcode = instr.opcode;
		// emit inline 64-bit code
		if (opcode == Operator.LDiv || opcode == Operator.LMod)
		{
			LIRValue dividend = force(instr.x, EAX_L); // dividend must be in RAX
			LIRValue divisor = load(
					instr.y);            // divisor can be in any (other) register

			LIRValue result = createResultVirtualRegister(instr);
			LIRValue resultReg;
			if (opcode == Operator.LDiv)
			{
				resultReg = EDX_L; // remainder getReturnValue is produced in rdx
				lir.lrem(dividend, divisor, resultReg, LDIV_TMP);
			}
			else if (opcode == Operator.LMod)
			{
				resultReg = EAX_L; // division getReturnValue is produced in rax
				lir.ldiv(dividend, divisor, resultReg, LDIV_TMP);
			}
			else
			{
				throw Util.shouldNotReachHere();
			}

			lir.move(resultReg, result);
		}
		else if (opcode == Operator.LMul)
		{
			LIRItem right = new LIRItem(instr.y, this);

			// right register is destroyed by the long mul, so it must be
			// copied to a new register.
			right.setDestroysRegister();

			LIRValue left = load(instr.x);
			right.loadItem();

			arithmeticOpLong(opcode, LMUL_OUT, left, right.result());
			LIRValue result = createResultVirtualRegister(instr);
			lir.move(LMUL_OUT, result);
		}
		else
		{
			LIRItem right = new LIRItem(instr.y, this);

			LIRValue left = load(instr.x);
			// don't load constants to save register
			right.loadNonconstant();
			createResultVirtualRegister(instr);
			arithmeticOpLong(opcode, instr.LIROperand(), left, right.result());
		}
	}

	protected LIRValue force(Value instr, LIRValue operand)
	{
		LIRValue result = makeOperand(instr);
		if (result != operand)
		{
			assert result.kind != LIRKind.Illegal;
			if (!Util.archKindEqual(result.kind, operand.kind))
			{
				// moves between different types need an intervening spill slot
				LIRValue tmp = forceToSpill(result, operand.kind, false);
				lir.move(tmp, operand);
			}
			else
			{
				lir.move(result, operand);
			}
		}
		return operand;
	}

	protected void arithmeticOp2Int(Instruction.Op2 instr)
	{
		Operator opcode = instr.opcode;
		if (opcode == Operator.IDiv || opcode == Operator.IMod)
		{
			// emit code for integer division or modulus

			// dividend must be in RAX
			LIRValue dividend = force(instr.x, EAX_I);

			// divisor can be in any (other) register
			LIRValue divisor = load(instr.y);

			// idiv and irem use rdx in their implementation so the
			// register allocator must not assign it to an interval that overlaps
			// this division instruction.
			LIRRegisterValue tmp = EDX_I;

			LIRValue result = createResultVirtualRegister(instr);

			LIRValue resultReg;
			if (opcode == Operator.IMod)
			{
				resultReg = tmp; // remainder getReturnValue is produced in rdx
				lir.irem(dividend, divisor, resultReg, tmp);
			}
			else if (opcode == Operator.IDiv)
			{
				resultReg = EAX_I; // division getReturnValue is produced in rax
				lir.idiv(dividend, divisor, resultReg, tmp);
			}
			else
			{
				throw Util.shouldNotReachHere();
			}

			lir.move(resultReg, result);
		}
		else
		{
			// emit code for other integer operations
			LIRItem left = new LIRItem(instr.x, this);
			LIRItem right = new LIRItem(instr.y, this);
			LIRItem leftArg = left;
			LIRItem rightArg = right;
			if (opcode.isCommutative() && left.isStack() && right
					.isRegisterOrVariable())
			{
				// swap them if left is real stack (or cached) and right is real
				// register(not cached)
				leftArg = right;
				rightArg = left;
			}

			leftArg.loadItem();

			// do not need to load right, as we can handle stack and constants
			if (opcode == Operator.IMul)
			{
				// check if we can use shift instead
				boolean useConstant = false;
				boolean useTmp = false;
				if (rightArg.result().isConstant())
				{
					int iconst = rightArg.instruction.asLIRConstant().asInt();
					if (iconst > 0)
					{
						if (Util.isPowerOf2(iconst))
						{
							useConstant = true;
						}
						else if (Util.isPowerOf2(iconst - 1) || Util
								.isPowerOf2(iconst + 1))
						{
							useConstant = true;
							useTmp = true;
						}
					}
				}
				if (!useConstant)
				{
					rightArg.loadItem();
				}
				LIRValue tmp = ILLEGAL;
				if (useTmp)
				{
					tmp = newVariable(LIRKind.Int);
				}
				createResultVirtualRegister(instr);

				arithmeticOpInt(opcode, instr.LIROperand(), leftArg.result(),
						rightArg.result(), tmp);
			}
			else
			{
				createResultVirtualRegister(instr);
				LIRValue tmp = ILLEGAL;
				arithmeticOpInt(opcode, instr.LIROperand(), leftArg.result(),
						rightArg.result(), tmp);
			}
		}
	}

	protected void doNegateOp(Instruction.Op1 instr)
	{
		LIRItem value = new LIRItem(instr.x, this);
		value.setDestroysRegister();
		value.loadItem();
		LIRVariable reg = newVariable(instr.kind);
		lir.negate(value.result(), reg);
		setResult(instr, reg);
	}

	@Override
	protected boolean strengthReduceMultiply(LIRValue left, int c,
			LIRValue result, LIRValue tmp)
	{
		if (tmp.isLegal())
		{
			if (Util.isPowerOf2(c + 1))
			{
				lir.move(left, tmp);
				lir.shiftLeft(left, Util.log2(c + 1), left);
				lir.sub(left, tmp, result);
				return true;
			}
			else if (Util.isPowerOf2(c - 1))
			{
				lir.move(left, tmp);
				lir.shiftLeft(left, Util.log2(c - 1), left);
				lir.add(left, tmp, result);
				return true;
			}
		}
		return false;
	}

	protected void doLogicOp(Instruction.Op2 instr)
	{
		// when an operand with use count 1 is the left operand, then it is
		// likely that no more for 2-operand LIR form is necessary.
		Operator op = instr.opcode;
		if (op.isCommutative() && !instr.y.isConstant()
				&& instr.x.getNumUses() > instr.y.getNumUses())
		{
			instr.swapOperands();
		}

		LIRItem left = new LIRItem(instr.x, this);
		LIRItem right = new LIRItem(instr.y, this);

		left.loadItem();
		right.loadNonconstant();
		LIRVariable reg = createResultVirtualRegister(instr);
		logicOp(op, left.result(), right.result(), reg);
	}

	/**
	 * Visits {@code ShiftOp} with visitor pattern.
	 *
	 * @param inst The ShiftOp to be visited.
	 */
	public void visitShiftOp(Instruction.ShiftOp inst)
	{
		// count must always be in rcx
		LIRValue count = makeOperand(inst.y);
		boolean mustLoadCount = !count.isConstant() || inst.kind == LIRKind.Long;
		if (mustLoadCount)
		{
			// count for long must be in register
			count = force(inst.y, SHIFT_COUNT_IN);
		}

		LIRValue value = load(inst.x);
		LIRValue reg = createResultVirtualRegister(inst);

		shiftOp(inst.opcode, reg, value, count, ILLEGAL);
	}

	/**
	 * Converts data from specified type to targetAbstractLayer type upon platform dependent
	 * instruction, like IA32 or SPARC.
	 * @param inst
	 */
	@Override
	public void visitConvert(Instruction.CastInst inst)
	{
		LIRValue input = load(inst.x);
		LIRVariable result = newVariable(inst.kind);

		lir.convert(inst.opcode, input, result);
		setResult(inst, result);
	}

	protected void doCompare(Instruction.Cmp inst)
	{
		// The arguments are already supposed to be of the same tyep
		LIRItem left = new LIRItem(inst.x, this);
		LIRItem right = new LIRItem(inst.y, this);
		if (!inst.kind.isVoid() && inst.x.kind.isLong())
			left.setDestroysRegister();

		left.loadItem();
		right.loadItem();
		LIRValue reg = createResultVirtualRegister(inst);
		if (inst.x.kind.isFloatOrDouble())
		{
			lir.fcmp2int(left.result(), right.result(), reg);
		}
		else if (inst.x.kind.isLong())
		{
			lir.lcmp2int(left.result(), right.result(), reg);
		}
		else
		{
			Util.unimplemented();
		}
	}
}
