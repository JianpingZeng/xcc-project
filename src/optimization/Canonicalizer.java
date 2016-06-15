package optimization;

import hir.*;
import hir.Instruction.*;
import hir.Value.Constant;
import lir.ci.LIRConstant;
import lir.ci.LIRKind;
import utils.Util;

/**
 * <p>This file defines a class for folding an instruction into a constant.
 * Also, putting constants on the right side of commutative operators for
 * Strength reduction.
 * <p>
 * <p>This file is a member of <a href={@docRoot/optimization}>Machine Independence
 * Optimization</a>.
 *
 * @author Xlous.zeng
 * @see DCE
 * @see ConstantProp
 * @see GVN
 * @see UCE
 */
public class Canonicalizer extends ValueVisitor
{
	private Value result;

	/**
	 * This method attempts to fold specified exprssion to constant.
	 * If successfully, the constant result is result returned, otherwise, null
	 * is desired.
	 *
	 * @param inst The instruction to be folded.
	 * @return The result constant if successfully, otherwise, null returned.
	 */
	public Value constantFoldInstruction(Instruction inst)
	{
		// handle phi nodes here
		if (inst instanceof Phi)
		{
			Constant commonValue = null;
			Phi PH = (Phi) inst;
			for (int i = 0; i < PH.getNumberIncomingValues(); i++)
			{
				Value incomingValue = PH.getIncomingValue(i);

				// if the incoming value is undef and then skip it.
				// Note that while we could skip the valeu if th is equal to the
				// phi node itself because that would break the rules that constant
				// folding only applied if all operands are constant.
				if (incomingValue instanceof Value.UndefValue)
					continue;

				// get rid of it, if the incoming value is not a constant
				if (!(incomingValue instanceof Constant))
					return null;

				Constant constant = (Constant) incomingValue;
				// folds the phi's operands
				if (commonValue != null && constant != commonValue)
					return null;
				commonValue = constant;
			}

			return commonValue != null ?
					commonValue :
					Value.UndefValue.get(PH.kind);
		}

		// handles other instruction here.
		inst.accept(this);
		return result;
	}

	/**
	 * Visits {@code ShiftOp} with visitor pattern.
	 *
	 * @param inst The ShiftOp to be visited.
	 */
	public void visitShiftOp(ShiftOp inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code Negate} with vistor pattern.
	 *
	 * @param inst The inst to be visited.
	 */
	public void visitNegate(Negate inst)
	{
		Value v = inst.x;
		if (v instanceof Constant)
		{
			setIntConstant(-v.asConstant().asInt());
		}
	}

	/**
	 * Folds the type conversion operation.
	 *
	 * @param inst The conversion instruction to be folded.
	 */
	public void visitConvert(Instruction.Convert inst)
	{
		Value val = inst.x;
		if (val.isConstant())
		{
			// folds conversions between two constant
			switch (inst.opcode)
			{
				case I2B:
					setIntConstant((byte) val.asConstant().asInt());
					return;
				case I2S:
					setIntConstant((short) val.asConstant().asInt());
					return;
				case I2C:
					setIntConstant((char) val.asConstant().asInt());
					return;
				case I2L:
					setLongConstant(val.asConstant().asInt());
					return;
				case I2F:
					setFloatConstant((float) val.asConstant().asInt());
					return;
				case L2I:
					setIntConstant((int) val.asConstant().asLong());
					return;
				case L2F:
					setFloatConstant((float) val.asConstant().asLong());
					return;
				case L2D:
					;
					setDoubleConstant((double) val.asConstant().asDouble());
					return;
				case F2D:
					setDoubleConstant((double) val.asConstant().asFloat());
					return;
				case F2I:
					setIntConstant((int) val.asConstant().asFloat());
					return;
				case F2L:
					setLongConstant((long) val.asConstant().asFloat());
					return;
				case D2F:
					setFloatConstant((float) val.asConstant().asDouble());
					return;
				case D2I:
					setIntConstant((int) val.asConstant().asDouble());
					return;
				case D2L:
					setLongConstant((long) val.asConstant().asDouble());
					return;
			}
			// finished
		}
		LIRKind kind = LIRKind.Illegal;
		// chained converts instruction like this (V)((T)val), where ((T)val) is
		// represented as val.
		if (val instanceof Convert)
		{
			Convert c = (Convert) val;
			// where T is kind.
			switch (c.opcode)
			{
				case I2B:
					kind = LIRKind.Byte;
					break;
				case I2S:
					kind = LIRKind.Short;
					break;
				case I2C:
					kind = LIRKind.Char;
					break;
			}

			if (kind != LIRKind.Illegal)
			{
				switch (inst.opcode)
				{
					case I2B:
						if (kind == LIRKind.Byte)
						{
							setCanonical(val);
						}
						break;
					case I2S:
						if (kind == LIRKind.Byte || kind == LIRKind.Short)
						{
							setCanonical(val);
						}
						break;
					case I2C:
						if (kind == LIRKind.Char)
						{
							setCanonical(val);
						}
						break;
				}
			}
			//(V)(x op2 y), where (x op2 y) is represented as val.
			if (val instanceof Op2)
			{
				// check if the operation was IAND with a constant; it may have narrowed the value already
				Op2 op = (Op2) val;
				// constant should be on right hand side if there is one
				if (op.opcode == Operator.IAnd && op.y.isConstant())
				{
					int safebits = 0;
					int mask = op.y.asConstant().asInt();
					// Checkstyle: off
					switch (inst.opcode)
					{
						case I2B:
							safebits = 0x7f;
							break;
						case I2S:
							safebits = 0x7fff;
							break;
						case I2C:
							safebits = 0xffff;
							break;
					}
					// Checkstyle: on
					if (safebits != 0 && (mask & ~safebits) == 0)
					{
						// the mask already cleared all the upper bits necessary.
						setCanonical(val);
					}
				}
			}
		}
	}

	private Value setCanonical(Value val)
	{
		return result = val;
	}

	private Value setIntConstant(int val)
	{
		return result = Constant.forInt(val);
	}

	private Value setLongConstant(long val)
	{
		return result = Constant.forLong(val);
	}

	private Value setFloatConstant(float val)
	{
		return result = Constant.forFloat(val);
	}

	private Value setDoubleConstant(Double val)
	{
		return result = Constant.forDouble(val);
	}

	/**
	 * Attempts to fold the binary operator on two integer inputed.
	 *
	 * @param opcode The operator performed on x and y.
	 * @param x      The first LIROperand of operation.
	 * @param y      The second LIROperand of operation.
	 * @return An {@code Integer} instance representing the folding result
	 * of two integer, if it is foldable. Otherwise, return null.
	 */
	private Integer foldIntOp2(Operator opcode, int x, int y)
	{
		switch (opcode)
		{
			case IAdd:
				return x + y;
			case ISub:
				return x - y;
			case IDiv:
				return x / y;
			case IMul:
				return y == 0 ? null : x * y;
			case IMod:
				return y == 0 ? null : x & y;
			case IAnd:
				return x & y;
			case IOr:
				return x | y;
			case IXor:
				return x ^ y;
			case IShl:
				return x << y;
			case IShr:
				return x >> y;
			case IUShr:
				return x >>> y;
		}
		return null;
	}

	/**
	 * Attempts to fold the binary operator on two long integer inputed.
	 *
	 * @param opcode The operator performed on x and y.
	 * @param x      The first LIROperand of operation.
	 * @param y      The second LIROperand of operation.
	 * @return An {@code Long} instance representing the folding result
	 * of two long integer, if it is foldable. Otherwise, return null.
	 */
	private Long foldLongOp2(Operator opcode, long x, long y)
	{
		switch (opcode)
		{
			case LAdd:
				return x + y;
			case LSub:
				return x - y;
			case LDiv:
				return x / y;
			case LMul:
				return y == 0 ? null : x * y;
			case LMod:
				return y == 0 ? null : x & y;
			case LAnd:
				return x & y;
			case LOr:
				return x | y;
			case LXor:
				return x ^ y;
			case LShl:
				return x << y;
			case LShr:
				return x >> y;
			case LUShr:
				return x >>> y;
		}
		return null;
	}

	/**
	 * Attempts to fold the binary operator on two float point fixed number inputed.
	 *
	 * @param opcode The operator performed on x and y.
	 * @param x      The first LIROperand of operation.
	 * @param y      The second LIROperand of operation.
	 * @return An {@code Float} instance representing the folding result
	 * of two float point number, if it is foldable. Otherwise, return null.
	 */
	private Float foldFloatOp2(Operator opcode, float x, float y)
	{
		switch (opcode)
		{
			case IAdd:
				return x + y;
			case ISub:
				return x - y;
			case IDiv:
				return x / y;
			case IMul:
				return y == 0 ? null : x * y;
		}
		return null;
	}

	/**
	 * Attempts to fold the binary operator on two double point number inputed.
	 *
	 * @param opcode The operator performed on x and y.
	 * @param x      The first LIROperand of operation.
	 * @param y      The second LIROperand of operation.
	 * @return An {@code Double} instance representing the folding result
	 * of two double number, if it is foldable. Otherwise, return null.
	 */
	private Double foldDoubleOp2(Operator opcode, double x, double y)
	{
		switch (opcode)
		{
			case IAdd:
				return x + y;
			case ISub:
				return x - y;
			case IDiv:
				return x / y;
			case IMul:
				return y == 0 ? null : x * y;
		}
		return null;
	}

	private Value reduceIntOp2(Operator opcode, Value x, int y)
	{
		switch (opcode)
		{
			case ISub:
			case IAdd:
				return y == 0 ? setCanonical(x) : null;
			case IMul:
			{
				if (y == 1)
					return setCanonical(x);

				// strength reduction by converting multiple to shift operation.
				if (y > 0 && (y & y - 1) == 0)
				{
					setCanonical(
							new Instruction.ShiftOp(x.kind, Operator.IShl, x,
									Constant.forInt(Util.log2(y)), "IShl"));
				}
				return y == 0 ? setIntConstant(0) : null;
			}
			case IDiv:
				return y == 1 ? setCanonical(x) : null;
			case IMod:
				return y == 1 ? setIntConstant(0) : null;
			case IAnd:
			{
				if (y == -1)
					return setCanonical(x);
				return y == 0 ? setIntConstant(0) : null;
			}
			case IOr:
			{
				if (y == -1)
				{
					return setIntConstant(-1);
				}
				return y == 0 ? setCanonical(x) : null;
			}
			case IXor:
				return y == 0 ? setCanonical(x) : null;
			case IShl:
				return reduceShift(false, opcode, Operator.IUShr, x, y);
			case IShr:
				return reduceShift(false, opcode, Operator.None, x, y);
			case IUShr:
				return reduceShift(false, opcode, Operator.IShl, x, y);
		}
		return null;
	}

	private Value reduceLongOp2(Operator opcode, Value x, long y)
	{
		switch (opcode)
		{
			case LSub:
			case LAdd:
				return y == 0 ? setCanonical(x) : null;
			case LMul:
			{
				if (y == 1)
					return setCanonical(x);

				// strength reduction by converting multiple to shift operation.
				if (y > 0 && (y & y - 1) == 0)
				{
					setCanonical(
							new Instruction.ShiftOp(x.kind, Operator.LShl, x,
									Constant.forLong(Util.log2(y)), "LShl"));
				}
				return y == 0 ? setLongConstant(0) : null;
			}
			case LDiv:
				return y == 1 ? setCanonical(x) : null;
			case LMod:
				return y == 1 ? setLongConstant(0) : null;
			case LAnd:
			{
				if (y == -1)
					return setCanonical(x);
				return y == 0 ? setLongConstant(0) : null;
			}
			case LOr:
			{
				if (y == -1)
				{
					return setLongConstant(-1);
				}
				return y == 0 ? setCanonical(x) : null;
			}
			case LXor:
				return y == 0 ? setCanonical(x) : null;
			case LShl:
				return reduceShift(true, opcode, Operator.IUShr, x, y);
			case LShr:
				return reduceShift(true, opcode, Operator.None, x, y);
			case LUShr:
				return reduceShift(true, opcode, Operator.IShl, x, y);
		}
		return null;
	}

	private Value reduceShift(boolean isLong, Operator opcode, Operator reverse,
			Value x, long y)
	{
		int mod = isLong ? 0x3f : 0x1f;

		long shift = y & mod;
		if (shift == 0)
			return setCanonical(x);

		// handles the operation of shfit chain.
		if (x instanceof ShiftOp)
		{
			ShiftOp s = (ShiftOp) x;

			// like this form ((s op1 z) op2 y)
			// where x is equal to (s op1 z)
			if (s.y.isConstant())
			{
				long z = s.y.asConstant().asLong();
				// if this expression like this (s >> z) >> y
				if (s.opcode == opcode)
				{
					y = y + z;
					if (y > mod)
						return null;
					shift = (y & mod);
					if (shift == 0)
						return setCanonical(x);

					return setCanonical(new ShiftOp(x.kind, s.opcode, s.x,
							Constant.forInt((int) y), s.opcode.opName));
				}
				if (s.opcode == reverse && y == z)
				{
					// this is chained shift as form (s >> z) << y
					if (isLong)
					{
						long mask = -1;
						if (opcode == Operator.LUShr)
						{
							mask = mask >>> y;
						}
						else
						{
							mask = mask << y;
						}
						// reduce to (e & mask)
						return setCanonical(
								new ArithmeticOp(x.kind, Operator.LAnd, s.x,
										Constant.forLong(mask), "LAnd"));
					}
					else
					{
						int mask = -1;
						if (opcode == Operator.IUShr)
						{
							mask = mask >>> y;
						}
						else
						{
							mask = mask << y;
						}
						return setCanonical(
								new ArithmeticOp(x.kind, Operator.IAnd, s.x,
										Constant.forInt(mask), "IAnd"));
					}
				}
			}
		}
		if (y != shift)
		{
			// (y & mod) != y
			return setCanonical(
					new ShiftOp(x.kind, opcode, x, Constant.forInt((int) shift),
							opcode.opName));
		}
		return null;
	}

	private void handleOp2(Op2 inst)
	{
		Value x = inst.x;
		Value y = inst.y;
		Operator opcode = inst.opcode;
		if (x == y)
		{
			switch (opcode)
			{
				case ISub:
					setIntConstant(0);
					return;
				case LSub:
					setLongConstant(0);
					return;
				case IAnd:
				case LAnd:
				case IOr:
				case LOr:
					setCanonical(x);
					return;
				case IXor:
					setIntConstant(0);
					return;
				case LXor:
					setLongConstant(0);
					return;
			}
		}
		LIRKind kind = inst.kind;
		if (x.isConstant() && y.isConstant())
		{
			switch (kind)
			{
				case Int:
				{
					Integer val = foldIntOp2(opcode, x.asConstant().asInt(),
							y.asConstant().asInt());
					if (val != null)
					{
						setIntConstant(val);
						return;
					}
					break;
				}
				case Long:
				{
					Long val = foldLongOp2(opcode, x.asConstant().asLong(),
							y.asConstant().asLong());
					if (val != null)
					{
						setLongConstant(val);
						return;
					}
					break;
				}
				case Float:
				{
					Float val = foldFloatOp2(opcode, x.asConstant().asFloat(),
							y.asConstant().asFloat());
					if (val != null)
					{
						setFloatConstant(val);
						return;
					}
					break;
				}
				case Double:
				{
					Double val = foldDoubleOp2(opcode,
							x.asConstant().asDouble(),
							y.asConstant().asDouble());
					if (val != null)
					{
						setDoubleConstant(val);
						return;
					}
					break;
				}

			}
		}

		// if there is a contant on the left and the operation is commutative,
		// jsut move it to the right
		moveConstantToRight(inst);

		// make expression strength reduction.
		y = inst.y;
		if (y.isConstant())
		{
			switch (kind)
			{
				case Int:
				{
					if (reduceIntOp2(opcode, inst.x, y.asConstant().asInt())
							!= null)
						return;
					break;
				}
				case Long:
				{
					if (reduceLongOp2(opcode, inst.x, y.asConstant().asLong())
							!= null)
						return;
					break;
				}
			}
		}
	}

	/**
	 * Swaps the left side LIROperand and right, if the left LIROperand is constant
	 * and this operator of instruction is commutative.
	 *
	 * @param inst
	 */
	private void moveConstantToRight(Op2 inst)
	{
		if (inst.x.isConstant() && inst.opcode.isCommutative())
			inst.swapOperands();
	}

	private Integer foldLongCompare(Condition cond, long x, long y)
	{
		switch (cond)
		{
			case EQ:
				return x == y ? 1 : 0;
			case LE:
				return x <= y ? 1 : 0;
			case LT:
				return x < y ? 1 : 0;
			case NE:
				return x != y ? 1 : 0;
			case GE:
				return x >= y ? 1 : 0;
			case GT:
				return x > y ? 1 : 0;
		}
		return null;
	}

	private Integer foldFloatCompare(Condition cond, float x, float y)
	{
		int result = 0;
		if (x < y)
			result = -1;
		else if (x > y)
			result = 1;
		if (cond == Condition.LT)
		{
			if (Float.isNaN(x) || Float.isNaN(y))
				return -1;
		}
		else if (cond == Condition.GT)
		{
			if (Float.isNaN(x) || Float.isNaN(y))
				return 1;

			return result;
		}
		// unknown comparison opcode.
		return null;
	}

	private Integer foldDoubleCompare(Condition cond, double x, double y)
	{
		int result = 0;
		if (x < y)
			result = -1;
		else if (x > y)
			result = 1;
		if (cond == Condition.LT)
		{
			if (Double.isNaN(x) || Double.isNaN(y))
				return -1;
		}
		else if (cond ==  Condition.GT)
		{
			if (Double.isNaN(x) || Double.isNaN(y))
				return 1;

			return result;
		}
		// unknown comparison opcode.
		return null;
	}

	private void reduceIf(IfOp inst)
	{
		Value l = inst.x();
		Value r = inst.y();

		if (l == r && !l.kind.isFloatOrDouble())
		{
			// skips the optimization for flort/double
			// due to NaN issue.
			reduceReflexiveIf(inst);
			return;
		}

		if (l.isConstant() && r.isConstant())
		{
			Boolean result = foldCondition(inst.condition(), l.asConstant(),
					r.asConstant());
			;
			if (result != null)
			{
				setCanonical(new Goto(inst.successor(result), "Goto"));
				return;
			}
		}
	}

	/**
	 * Reduces the conditional branch instruction when left side LIROperand is equal
	 * to the right one.
	 *
	 * @param inst
	 */
	private void reduceReflexiveIf(IfOp inst)
	{
		BasicBlock sux = null;
		switch (inst.condition())
		{
			case EQ:
			case LE:
			case GE:
				sux = inst.successor(true);
				break;
			case NE:
			case LT:
			case GT:
				sux = inst.successor(false);
				break;
			default:
				throw new InternalError("should not reach here.");
		}
		setCanonical(new Goto(sux, "Goto"));
	}

	/**
	 * Attempts to fold two constant over the given operator, such as, LT, EQ, NE
	 * etc, and return the result of comparison.
	 *
	 * @param cond The comparison operator.
	 * @param l      The left side LIROperand.
	 * @param r      The right side LIROperand.
	 * @return The result of comparison, return true if comparison successfully
	 * in specified op, otherwise, return false. Return null when the op is illegal.
	 */
	private Boolean foldCondition(Condition cond, LIRConstant l, LIRConstant r)
	{
		LIRKind lk = l.kind;
		switch (lk)
		{
			case Int:
			{
				int x = l.asInt();
				int y = r.asInt();
				switch (cond)
				{
					case EQ:
						return x == y;
					case LE:
						return x <= y;
					case GE:
						return x >= y;
					case NE:
						return x != y;
					case LT:
						return x < y;
					case GT:
						return x > y;
				}
				break;
			}
			case Long:
			{
				long x = l.asLong();
				long y = r.asLong();
				switch (cond)
				{
					case EQ:
						return x == y;
					case LE:
						return x <= y;
					case GE:
						return x >= y;
					case NE:
						return x != y;
					case LT:
						return x < y;
					case GT:
						return x > y;
				}
				break;
			}
		}
		return null;
	}

	/**
	 * Visits Arithmetic Operation with visitor pattern.
	 *
	 * @param inst The operation to be visited.
	 */
	public void visitArithmeticOp(ArithmeticOp inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits Logical Operation with visitor pattern.
	 *
	 * @param inst The operation to be visited.
	 */
	public void visitLogicOp(LogicOp inst)
	{
		handleOp2(inst);
	}

	public void visitCompare(Cmp inst)
	{
		assert inst.kind != LIRKind.Void;

		Value x = inst.x;
		Value y = inst.y;

		Condition cond = inst.condition();
		// if the two operands are same
		// then reduce it
		if (x == y)
		{
			switch (inst.kind)
			{
				case Int:
				case Long:
					setIntConstant(0);
					break;
				case Float:
					if (x.isConstant())
					{
						float xval = x.asConstant().asFloat();
						Integer res = foldFloatCompare(cond, x.asConstant().asFloat(), y.asConstant().asFloat());
						assert res
								!= null : "invalid comparison operation in float";
						setIntConstant(res);
					}
					break;
				case Double:
					if (x.isConstant())
					{
						double xval = x.asConstant().asDouble();
						Integer res = foldDoubleCompare(cond, x.asConstant().asDouble(), y.asConstant().asDouble());
						assert res != null : "invalid comparison in double";
						setIntConstant(res);
					}
					break;
			}
		}
		if (x.isConstant() && y.isConstant())
		{
			// both x and y are the constant
			switch (inst.kind)
			{
				case Long:
				case Int:
				{
					setIntConstant(
							foldLongCompare(cond, x.asConstant().asLong(),
									x.asConstant().asLong()));
					break;
				}
				case Float:
				{
					Integer val = foldFloatCompare(cond,
							x.asConstant().asFloat(), y.asConstant().asFloat());
					assert val
							!= null : "invalid operation in float comparison";
					setIntConstant(val);
					break;
				}
				case Double:
				{
					Integer val = foldDoubleCompare(cond,
							x.asConstant().asDouble(),
							y.asConstant().asDouble());
					assert val
							!= null : "invalid operation in float comparison";
					setIntConstant(val);
					break;
				}
			}
		}
		assert Util.archKindEqual(inst.kind, result.kind);
	}

	/**
	 * Go through the value {@code Value}. Usually, this method is not used
	 * instead of calling to the visitor to it's subclass, like {@code Constant}.
	 *
	 * @param val The instance of {@code Value} to be visited.
	 */
	public void visitValue(Value val)
	{
		Util.shouldNotReachHere();
	}


	public void visitUndef(Value.UndefValue undef)
	{
		Util.shouldNotReachHere();
	}
}
