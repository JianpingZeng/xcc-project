package lir;

import asm.Label;
import driver.Backend;
import hir.*;
import hir.Instruction.Phi;
import lir.alloc.OperandPool;
import lir.alloc.OperandPool.VariableFlag;
import lir.ci.*;
import utils.Util;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import static lir.ci.LIRRegisterValue.IllegalValue;

/**
 * @author Jianping Zeng
 */
public abstract class LIRGenerator extends ValueVisitor
{
	/**
	 * the range of values in a switch statement.
	 */
	private static final class SwitchRange
	{
		final int lowKey;
		int highKey;
		final BasicBlock sux;

		SwitchRange(int lowKey, BasicBlock sux)
		{
			this.lowKey = lowKey;
			this.highKey = lowKey;
			this.sux = sux;
		}
	}

	public static int RangeTestsSwitchDensity = 5;

	public final Backend backend;
	/**
	 * The function to be Lirified.
	 */
	private final Method method;
	private BasicBlock currentBlock;
	private Instruction currentInstr;
	public OperandPool operands;
	protected LIRList lir;
	private final boolean isTwoOperand;
	private List<LIRConstant> constants;
	private List<LIRVariable> variablesForConstants;

	public LIRGenerator(Backend backend, Method method)
	{
		this.backend = backend;
		this.method = method;
		this.isTwoOperand = backend.targetMachine.arch.twoOperandMode();
		constants = new ArrayList<>(16);
		this.operands = new OperandPool(backend.targetMachine);
		variablesForConstants = new ArrayList<>();
	}
	/**
	 * generates LIR instruction for specified basic block.
	 *
	 * @param block
	 */
	public void doBlock(BasicBlock block)
	{
		blockDoProlog(block);
		currentBlock = block;

		for (Iterator<Instruction> itr = block.iterator(); itr.hasNext(); )
		{
			Instruction inst = itr.next();
			doRoot(inst);
		}
		currentBlock = null;
		blockDoEpilog(block);
	}

	private void doRoot(Instruction instr)
	{
		// lacks of debug information and error checking, currently
		this.currentInstr = instr;
		instr.accept(this);
	}

	/**
	 * Inserts prolog code in the entry block of given compiled function.
	 *
	 * @param block
	 */
	private void blockDoProlog(BasicBlock block)
	{
		// print debug information

		assert block.getLIRBlock()
				== null : "Machine block already be computed for this block";
		LIRList lir = new LIRList(this);
		block.setLIR(lir);

		lir.branchDesination(block.label());
		// inserts prolog code for entry of function
		/**if (block == method.getEntryBlock())
		{
			traceBlockEntry(block);
		}*/
	}

	private void blockDoEpilog(BasicBlock block)
	{
		// print debug information
		/*
		if (block == method.getExitBlock())
		{
			// restores register value saved in stack frame
			traceBlockExit(block);
		}
		*/
	}

	/**
	 * This method is designed to lower binary operation into targetAbstractLayer-dependent
	 * instruction.
	 *
	 * @param instr
	 */
	private void lowerOp2(Instruction.Op2 instr)
	{
		assert Util.archKindEqual(instr.x.kind, instr.kind) && Util
				.archKindEqual(instr.y.kind, instr.kind) :
				"Wrong parameter type of: " + instr.getName() + " in: "
						+ instr.opcode.opName;
		switch (instr.kind)
		{
			case Float:
			case Double:
				arithmeticOp2Float(instr);
				return;
			case Long:
				arithmeticOp2Long(instr);
				return;
			case Int:
				arithmeticOp2Int(instr);
				return;
		}
		throw Util.shouldNotReachHere();
	}

	/**
	 * Visits {@code ADD_I} with visitor pattern.
	 *
	 * @param inst The ADD_I to be visited.
	 */
	@Override public void visitArithmeticOp(Instruction.ArithmeticOp inst)
	{
		lowerOp2(inst);
	}

	/**
	 * Visits Logical operation with visitor pattern.
	 *
	 * @param inst The Logical operation to be visited.
	 */
	@Override public void visitLogicOp(Instruction.LogicOp inst)
	{
		doLogicOp(inst);
	}

	/**
	 * Visits {@code Negate} with vistor pattern.
	 *
	 * @param inst The inst to be visited.
	 */
	@Override
	public void visitNegate(Instruction.Negate inst)
	{
		doNegateOp(inst);
	}

	@Override
	public void visitCompare(Instruction.Cmp inst)
	{
		doCompare(inst);
	}

	protected void logicOp(Operator opcode, LIRValue leftOp, LIRValue rightOp,
			LIRValue resultOp)
	{
		if (isTwoOperand && leftOp != resultOp)
		{
			assert rightOp != resultOp : "malformed";
			lir.move(leftOp, resultOp);
			leftOp = resultOp;
		}

		switch (opcode)
		{
			case IAnd:
			case LAnd:
				lir.logicalAnd(leftOp, rightOp, resultOp);
				break;

			case IOr:
			case LOr:
				lir.logicalOr(leftOp, rightOp, resultOp);
				break;

			case IXor:
			case LXor:
				lir.logicalXor(leftOp, rightOp, resultOp);
				break;

			default:
				Util.shouldNotReachHere();
		}
	}

	public void visitSwitch(Instruction.SwitchInst switchInst)
	{
		int lo = Integer.MAX_VALUE;
		int hi = Integer.MIN_VALUE;
		int defaultIndex = 0;
		int[] labels = new int[switchInst.numsOfCases()];
		Value[] vals = switchInst.getCaseValues();
		for (int i = 0; i < vals.length; i++)
		{
			int val = vals[i].asConstant().asInt();
			labels[i] = val;
			if (val < lo)
				lo = val;
			if (hi < val)
				hi = val;
		}
		switchInst.setLowKey(lo);
		switchInst.setHighKey(hi);

		long tableSpaceCost = 4 + ((long) hi - lo + 1);
		long tableTimeCost = 3;
		long lookupSpaceCost = 3 + 2 * (long) vals.length;
		long lookupTimeCost = vals.length;

		// determines how to implement switch instruction according to case values
		//
		boolean isTableSwitch = vals.length > 0
				&& tableSpaceCost + 3 * tableTimeCost
				<= lookupSpaceCost + 3 * lookupTimeCost;
		LIRItem condV = new LIRItem(vals[defaultIndex], this);
		condV.setDestroysRegister();
		condV.loadItem();
		clearResult(switchInst);

		LIRValue value = condV.result();

		if (isTableSwitch)
		{
			SwitchRange[] ranges = createLookupRanges(switchInst);
			doSwitchRanges(ranges, value, switchInst.getDefaultBlock());
		}
		else
		{
			// first, do not to tackle default case
			for (int idx = 1; idx < vals.length; idx++)
			{
				lir.cmp(Condition.EQ, value, LIRConstant.forInt(idx + lo));
				lir.branch(Condition.EQ, LIRKind.Int, switchInst.targetAt(idx));
			}
			lir.jump(switchInst.getDefaultBlock());
		}
	}

	private void doSwitchRanges(SwitchRange[] x, LIRValue value,
			BasicBlock defaultSux)
	{
		for (int i = 1; i < x.length; i++)
		{
			SwitchRange oneRange = x[i];
			int lowKey = oneRange.lowKey;
			int highKey = oneRange.highKey;
			BasicBlock dest = oneRange.sux;
			if (lowKey == highKey)
			{
				lir.cmp(Condition.EQ, value, LIRConstant.forInt(lowKey));
				lir.branch(Condition.EQ, LIRKind.Int, dest);
			}
			else if (highKey - lowKey == 1)
			{
				lir.cmp(Condition.EQ, value, LIRConstant.forInt(lowKey));
				lir.branch(Condition.EQ, LIRKind.Int, dest);
				lir.cmp(Condition.EQ, value, LIRConstant.forInt(highKey));
				lir.branch(Condition.EQ, LIRKind.Int, dest);
			}
			else
			{
				Label label = new Label();
				lir.cmp(Condition.LT, value, LIRConstant.forInt(lowKey));
				lir.branch(Condition.LT, label);
				lir.cmp(Condition.LE, value, LIRConstant.forInt(highKey));
				lir.branch(Condition.LE, LIRKind.Int, dest);
				lir.branchDestination(label);
			}
		}
		lir.jump(defaultSux);
	}

	private SwitchRange[] createLookupRanges(Instruction.SwitchInst inst)
	{
		int len = inst.numsOfCases();
		ArrayList<SwitchRange> res = new ArrayList<>(len);
		if (len > 0)
		{
			BasicBlock sux = inst.targetAt(0);
			int key = inst.getLowKey();
			BasicBlock defaultBlock = inst.getDefaultBlock();

			// the range for default case clause.
			SwitchRange range = new SwitchRange(key, sux);
			if (sux == defaultBlock)
				res.add(range);

			for (int i = 1; i < len; i++)
			{
				BasicBlock newSux = inst.targetAt(i);
				if (sux == newSux)
				{
					range.highKey = key;
				}
				else
				{
					if (sux != defaultBlock)
					{
						res.add(range);
					}
					range = new SwitchRange(key, newSux);
				}
				sux = newSux;
			}
		}
		return res.toArray(new SwitchRange[res.size()]);
	}

	public void visitIfOp(Instruction.IfOp inst)
	{
		doIfCmp(inst);
	}

	/**
	 * Handles unconditional branch here. Note that since code layout is frozen at
	 * this point, that if we are trying to jump to a block that is the immediate
	 * successor of the current block, we can just make a fall-through.
	 *
	 * @param inst
	 */
	public void visitGoto(Instruction.Goto inst)
	{
		clearResult(inst);
		BasicBlock nextBB = getBlockAfter(inst.getParent());
		// if the target of this instruction is equal to next basic block
		// just fall through rather than redundant jump.
		if (inst.target != nextBB)
		{
			lir.jump(inst.target);
		}
		return;
	}

	/**
	 * Returns the basic block which occurs lexically after the specified one.
	 *
	 * @param block
	 * @return
	 */
	private BasicBlock getBlockAfter(BasicBlock block)
	{
		return block.getNumOfSuccs() >= 1 ? block.succAt(0) : null;
	}

	// the calling to function and return from function

	/**
	 * 'ret' instruction - Here we are interested in meeting the X86 ABI.
	 * As such, we have the following possibilities:
	 * <ol>ret void: No return value, simply emit a 'ret' instruction</ol>
	 * <ol>ret sbyte, ubyte : Extend value into EAX and return</ol>
	 * <ol>ret short, ushort: Extend value into EAX and return</ol>
	 * <ol>ret int, uint    : Move value into EAX and return</ol>
	 * <ol>ret pointer      : Move value into EAX and return</ol>
	 * <ol>ret long, ulong  : Move value into EAX/EDX and return</ol>
	 * <ol>ret float/double : Top of FP stack</ol>
	 */
	public void visitReturn(Instruction.Return inst)
	{
		if (inst.kind.isVoid())
		{
			// obtains the caller, no finished currently
			lir.returnOp(IllegalValue);
		}
		else
		{
			LIRValue reg = resultOperandFor(inst.kind);
			LIRItem result = new LIRItem(inst.result(), this);
			result.loadItemForce(reg);

			lir.returnOp(result.result());
			//LIRValue result = force(inst.result(), operand);
		}
		clearResult(inst);
	}

	/**
	 * The invoke with receiver has following phases:
	 *   a) traverse all arguments -> item-array (invoke_visit_argument)
	 *   b) load each of the items and push on stack
	 *   c) lock result LIRRegisters and emit call operation
	 *
	 * Before issuing a call, we must spill-save all values on stack
	 * that are in caller-save register. "spill-save" moves thos LIRRegisters
	 * either in a free callee-save register or spills them if no free
	 * callee save register is available.
	 *
	 * The problem is where to invoke spill-save.
	 * - if invoked between b) and c), we may lock callee save
	 *   register in "spill-save" that destroys the receiver register
	 *   before f) is executed
 	 */
	public void visitInvoke(Instruction.Invoke inst)
	{
		CallingConvention cc = backend.frameMap().getCallingConvention
				(Util.signatureToKinds(inst.target),
				CallingConvention.Type.JavaCallee);

		// an array of the stack slots where real arguments passing into called function store
		LIRValue[] locations = cc.locations;
		LIRItem[] args = invokeVisitArgument(inst);

		LIRValue[] argValues = new LIRValue[args.length];
		// computes all of real arguments
		for (int i=0; i < argValues.length; i++)
			argValues[i]  = args[i].result();

		// set up the result register
		LIRVariable resultReg = null;
		if (inst.kind != LIRKind.Void)
		{
			resultReg = newVariable(inst.kind);
		}
		// assign the real arguments into specified position
		invokeLoadArguments(inst, args, locations);

		// emit invocation code
		lir.callDirect(inst.target, resultReg, argValues, locations);
	}

	private void invokeLoadArguments(Instruction.Invoke inst,
			LIRItem[] args, LIRValue[] locations)
	{
		assert args.length == locations.length :
				"numbers of argguments and stack slots should be equivalent";
		for (int i = 0; i < args.length; i++)
		{
			LIRItem param = args[i];
			LIRValue loca = locations[i];
			if (loca.isRegister())
			{
				param.loadItemForce(loca);
			}
			else
			{
				LIRAddress addr = loca.asAddress();
				param.loadForStore(addr.kind);
				if (addr.kind == LIRKind.Object)
				{
					lir.move_wide(param.result(), addr);
				}
				else
				{
					if (addr.kind == LIRKind.Long ||
							addr.kind == LIRKind.Double)
					{
						lir.unalignedMove(param.result(), addr);;
					}
					else
					{
						lir.move(param.result(), addr);
					}
				}
			}
		}
	}
	private LIRItem[] invokeVisitArgument(Instruction.Invoke instr)
	{
		ArrayList<LIRItem> argumentItems = new ArrayList<>();
		for (int i = 0; i < instr.getNumsOfArgs(); i++)
		{
			LIRItem param = new LIRItem(instr.argumentAt(i), this);
			argumentItems.add(param);
		}
		return argumentItems.toArray(new LIRItem[argumentItems.size()]);
	}

	// A special instruction, phi function.
	public void visitPhi(Instruction.Phi inst)
	{

	}
	/**
	 * Code for a constant is generated lazily unless the constant is frequently
	 * used and can't be inlined.
	 * {@code Value}.
	 *
	 * @param Const A constant to be visited.
	 */
	public void visitConstant(Value.Constant Const)
	{
		if (canInlineAsConstant(Const))
		{
			setResult(Const, loadConstant(Const));
		}
		else
		{
			LIRValue res = Const.LIROperand;
			if (!res.isLegal())
				res = Const.asConstant();
			if (res.isConstant())
			{
				if (Const.hasOneUses())
				{
					// unpinned constants are handled specially so that they can be
					// put into LIRRegisters when they are used multiple times within a
					// block.  After the block completes their operand will be
					// cleared so that other blocks can't refer to that register.
					LIRVariable reg = createResultVariable(Const);
					lir.move(res, reg);
				}
				else
				{
					Const.setLIROperand(res);
				}
			}
			else
			{
				setResult(Const, (LIRVariable) res);
			}
		}
	}

	private LIRVariable loadConstant(Value.Constant x)
	{
		return loadConstant(x.asConstant(), x.kind);
	}

	protected LIRVariable loadConstant(LIRConstant c, LIRKind kind)
	{
		// XXX: linear search might be kind of slow for big basic blocks
		int index = constants.indexOf(c);

		if (index != -1)
		{
			return variablesForConstants.get(index);
		}
		// first visits, just append it into constants table
		LIRVariable result = newVariable(kind);
		lir.move(c, result);
		constants.add(c);
		variablesForConstants.add(result);
		return result;
	}

	/**
	 * Go through the value {@code Value}. Usually, this method is not used
	 * instead of calling to the visitor to it's subclass, like {@code Constant}.
	 *
	 * @param val The instance of {@code Value} to be visited.
	 */
	public void visitValue(Value val)
	{
		throw Util.shouldNotReachHere();
	}

	public void visitUndef(Value.UndefValue undef)
	{
		throw Util.shouldNotReachHere();
	}

	protected void arithmeticOpFpu(Operator opcode, LIRValue result,
			LIRValue left, LIRValue right, LIRValue temp)
	{
		LIRValue leftOp = left;

		if (isTwoOperand && leftOp != result)
		{
			assert right != result : "malformed";
			lir.move(leftOp, result);
			leftOp = result;
		}

		switch (opcode)
		{
			case DAdd:
			case FAdd:
				lir.add(leftOp, right, result);
				break;
			case FMul:
			case DMul:
				lir.mul(leftOp, right, result);
				break;
			case DSub:
			case FSub:
				lir.sub(leftOp, right, result);
				break;
			case FDiv:
			case DDiv:
				lir.div(leftOp, right, result);
				break;
			default:
				Util.shouldNotReachHere();
		}
	}

	protected void arithmeticOpLong(Operator code, LIRValue result, LIRValue left,
			LIRValue right)
	{
		LIRValue leftOp = left;

		if (isTwoOperand && leftOp != result)
		{
			assert right != result : "malformed";
			lir.move(leftOp, result);
			leftOp = result;
		}

		switch (code)
		{
			case LAdd:
				lir.add(leftOp, right, result);
				break;
			case LMul:
				lir.mul(leftOp, right, result);
				break;
			case LSub:
				lir.sub(leftOp, right, result);
				break;
			default:
				// ldiv and lrem are handled elsewhere
				Util.shouldNotReachHere();
		}
	}

	protected void arithmeticOpInt(Operator code, LIRValue result, LIRValue left,
			LIRValue right, LIRValue tmp)
	{
		LIRValue leftOp = left;

		if (isTwoOperand && leftOp != result)
		{
			assert right != result : "malformed";
			lir.move(leftOp, result);
			leftOp = result;
		}

		switch (code)
		{
			case IAdd:
				lir.add(leftOp, right, result);
				break;
			case IMul:
				boolean didStrengthReduce = false;
				if (right.isConstant())
				{
					LIRConstant rightConstant = (LIRConstant) right;
					int c = rightConstant.asInt();
					if (Util.isPowerOf2(c))
					{
						// do not need tmp here
						lir.shiftLeft(leftOp, Util.log2(c), result);
						didStrengthReduce = true;
					}
					else
					{
						didStrengthReduce = strengthReduceMultiply(leftOp, c,
								result, tmp);
					}
				}
				// we couldn't strength reduce so just emit the multiply
				if (!didStrengthReduce)
				{
					lir.mul(leftOp, right, result);
				}
				break;
			case ISub:
				lir.sub(leftOp, right, result);
				break;
			default:
				// idiv and irem are handled elsewhere
				Util.shouldNotReachHere();
		}
	}

	protected void shiftOp(Operator opcode, LIRValue resultOp, LIRValue value,
			LIRValue count, LIRValue tmp)
	{
		if (isTwoOperand && value != resultOp)
		{
			assert count != resultOp : "malformed";
			lir.move(value, resultOp);
			value = resultOp;
		}

		assert count.isConstant() || count.isVariableOrRegister();
		switch (opcode)
		{
			case IShl:
			case LShl:
				lir.shiftLeft(value, count, resultOp, tmp);
				break;
			case IShr:
			case LShr:
				lir.shiftRight(value, count, resultOp, tmp);
				break;
			case IUShr:
			case LUShr:
				lir.unsignedShiftRight(value, count, resultOp, tmp);
				break;
			default:
				Util.shouldNotReachHere();
		}
	}

	protected abstract void traceBlockEntry(BasicBlock block);

	protected abstract void traceBlockExit(BasicBlock block);

	protected abstract void arithmeticOp2Float(Instruction.Op2 instr);

	protected abstract void arithmeticOp2Long(Instruction.Op2 instr);

	protected abstract void arithmeticOp2Int(Instruction.Op2 instr);

	protected abstract void doNegateOp(Instruction.Op1 instr);

	protected abstract boolean canInlineAsConstant(Value v);

	protected abstract void doCompare(Instruction.Cmp inst);

	protected abstract void doLogicOp(Instruction.Op2 instr);

	protected abstract boolean strengthReduceMultiply(LIRValue left,
			int constant, LIRValue result, LIRValue tmp);

	protected abstract boolean canStoreAsConstant(Value i, LIRKind kind);

	protected abstract void doIfCmp(Instruction.IfOp instr);


	/**
	 * Forces the result of a given instruction to be available in a given operand,
	 * inserting move instructions if necessary.
	 *
	 * @param instruction an instruction that produces a {@linkplain Value#LIROperand() result}
	 * @param operand     the operand in which the result of {@code instruction}
	 *                    must be available
	 * @return {@code operand}
	 */
	protected LIRValue force(Value instruction, LIRValue operand)
	{
		LIRValue result = makeOperand(instruction);
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
				// just move the result of instruction into operation being used
				// when both kind of two LIROperand is equivalent
				lir.move(result, operand);
			}
		}
		return operand;
	}

	/**
	 * Gets the ABI specific operand used to return a value of a given kind from
	 * a method.
	 *
	 * @param kind the kind of value being returned
	 * @return the operand representing the ABI defined location used return a
	 * value of kind {@code kind}
	 */
	protected LIRValue resultOperandFor(LIRKind kind)
	{
		if (kind == LIRKind.Void)
		{
			return IllegalValue;
		}
		LIRRegister returnLIRRegister = backend.registerConfig
				.getReturnRegister(kind);
		return returnLIRRegister.asValue(kind);
	}

	protected LIRValue forceToSpill(LIRValue value, LIRKind kind,
			boolean mustStayOnStack)
	{
		assert value.isLegal() : "value should not be illegal";
		if (!value.isVariableOrRegister())
		{
			// force into a variable that must start in memory
			LIRValue r = operands.newVariable(value.kind, mustStayOnStack ?
					VariableFlag.MustStayInMemory :
					VariableFlag.MustStartInMemory);
			lir.move(value, r);
			value = r;
		}

		// create a spill location
		LIRValue tmp = operands.newVariable(kind, mustStayOnStack ?
				VariableFlag.MustStayInMemory :
				VariableFlag.MustStartInMemory);
		// move from register to spill
		lir.move(value, tmp);
		return tmp;
	}

	public LIRVariable newVariable(LIRKind kind)
	{
		return operands.newVariable(kind);
	}

	public void setResult(Value x, LIRVariable opr)
	{
		x.setLIROperand(opr);
	}

	protected void clearResult(Instruction x)
	{
		assert !x.hasOneUses() : "can't have use";
		x.clearLIROperand();
	}

	/**
	 * Ensures that an operand has been {@linkplain Value#setLIROperand(LIRValue)}
	 * initialized for storing the result of an {@code Value} instance.
	 *
	 * @param val an instance of {@code Value} that produces a result value.
	 */
	public LIRValue makeOperand(Value val)
	{
		LIRValue operand = val.LIROperand;
		if (operand.isIllegal())
		{
			if (val instanceof Instruction.Phi)
			{
				// a phi may not have an operand yet if it is for an exception block
				operand = operandForPhi((Instruction.Phi) val);
			}
			else if (val instanceof Value.Constant)
			{
				operand = operandForInstruction(val);
			}
		}
		// the value must be a constant or have a valid operand
		assert operand.isLegal() : "this root has not been visited yet";
		return operand;
	}

	private LIRValue operandForPhi(Phi phi)
	{
		if (phi.LIROperand().isIllegal())
		{
			// allocate a variable for this phi
			LIRVariable operand = newVariable(phi.kind);
			setResult(phi, operand);
		}
		return phi.LIROperand();
	}

	LIRValue operandForInstruction(Value x)
	{
		LIRValue operand = x.LIROperand();
		if (operand.isIllegal())
		{
			if (x instanceof Value.Constant)
			{
				x.setLIROperand(x.asConstant());
			}
			else
			{
				assert x instanceof Phi;
				//|| x instanceof Local : "only for Phi and Local";
				// allocate a variable for this local or phi
				createResultVariable(x);
			}
		}
		return x.LIROperand();
	}

	/**
	 * Allocates a variable LIROperand to hold the result of a given instruction.
	 * This can only be performed once for any given instruction.
	 *
	 * @param x an instruction that produces a result
	 * @return the variable assigned to hold the result produced by {@code x}
	 */
	protected LIRVariable createResultVariable(Value x)
	{
		LIRVariable operand = newVariable(x.kind);
		setResult(x, operand);
		return operand;
	}

	/**
	 * Loads the result of specified {@code Value} into virtual resiger to be
	 * assigned into physical register.
	 * @param val
	 * @return
	 */
	protected LIRValue load(Value val)
	{
		LIRValue result = makeOperand(val);
		if (!result.isVariableOrRegister())
		{
			LIRVariable operand = newVariable(val.kind);
			lir.move(result, operand);
			return operand;
		}
		return result;
	}
}
