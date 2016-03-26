package optimization;

import ci.CiConstant;
import ci.CiKind;
import hir.*;
import hir.Instruction.*;
import hir.Value.Constant;
import utils.Utils;

/**
 * This file defines a class for folding an instruction into a constant.
 * Also, putting constants on the right side of comutative operators for
 * Strength reduction.
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/3/24.
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

		// handles instruction operation
		inst.accept(this);
		return result;
	}

	/**
	 * Visits {@code ADD_I} with visitor pattern.
	 *
	 * @param inst The ADD_I to be visited.
	 */
	public void visitADD_I(Instruction.ADD_I inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code SUB_I} with visitor pattern.
	 *
	 * @param inst The SUB_I to be visited.
	 */
	public void visitSUB_I(Instruction.SUB_I inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code MUL_I} with visitor pattern.
	 *
	 * @param inst The MUL_I to be visited.
	 */
	public void visitMUL_I(Instruction.MUL_I inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code DIV_I} with visitor pattern.
	 *
	 * @param inst The DIV_I to be visited.
	 */
	public void visitDIV_I(Instruction.DIV_I inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code MOD_I} with visitor pattern.
	 *
	 * @param inst The MOD_I to be visited.
	 */
	public void visitMOD_I(Instruction.MOD_I inst)
	{
		handleOp2(inst);
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
	 * Visits {@code AND_I} with visitor pattern.
	 *
	 * @param inst The AND_I to be visited.
	 */
	public void visitAND_I(AND_I inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code OR_I} with visitor pattern.
	 *
	 * @param inst The OR_I to be visited.
	 */
	public void visitOR_I(Instruction.OR_I inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code XOR_I} with visitor pattern.
	 *
	 * @param inst The XOR_I to be visited.
	 */
	public void visitXOR_I(Instruction.XOR_I inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code ADD_L} with visitor pattern.
	 *
	 * @param inst The ADD_L to be visited.
	 */
	public void visitADD_L(Instruction.ADD_L inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code SUB_I} with visitor pattern.
	 *
	 * @param inst The SUB_I to be visited.
	 */
	public void visitSUB_L(Instruction.SUB_L inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code MUL_L} with visitor pattern.
	 *
	 * @param inst The MUL_L to be visited.
	 */
	public void visitMUL_L(Instruction.MUL_L inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code DIV_L} with visitor pattern.
	 *
	 * @param inst The DIV_L to be visited.
	 */
	public void visitDIV_L(Instruction.DIV_L inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code MOD_L} with visitor pattern.
	 *
	 * @param inst The MOD_L to be visited.
	 */
	public void visitMOD_L(Instruction.MOD_L inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code AND_L} with visitor pattern.
	 *
	 * @param inst The AND_L to be visited.
	 */
	public void visitAND_L(AND_L inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code OR_L} with visitor pattern.
	 *
	 * @param inst The OR_L to be visited.
	 */
	public void visitOR_L(Instruction.OR_L inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code XOR_L} with visitor pattern.
	 *
	 * @param inst The XOR_L to be visited.
	 */
	public void visitXOR_L(Instruction.XOR_L inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code ADD_F} with visitor pattern.
	 *
	 * @param inst The ADD_F to be visited.
	 */
	public void visitADD_F(Instruction.ADD_F inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code SUB_F} with visitor pattern.
	 *
	 * @param inst The SUB_F to be visited.
	 */
	public void visitSUB_F(Instruction.SUB_F inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code MUL_F} with visitor pattern.
	 *
	 * @param inst The MUL_F to be visited.
	 */
	public void visitMUL_F(Instruction.MUL_F inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code DIV_F} with visitor pattern.
	 *
	 * @param inst The DIV_F to be visited.
	 */
	public void visitDIV_F(Instruction.DIV_F inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code ADD_D} with visitor pattern.
	 *
	 * @param inst The ADD_D to be visited.
	 */
	public void visitADD_D(Instruction.ADD_D inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code SUB_D} with visitor pattern.
	 *
	 * @param inst The SUB_D to be visited.
	 */
	public void visitSUB_D(Instruction.SUB_D inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code MUL_D} with visitor pattern.
	 *
	 * @param inst The MUL_D to be visited.
	 */
	public void visitMUL_D(Instruction.MUL_D inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code DIV_D} with visitor pattern.
	 *
	 * @param inst The DIV_D to be visited.
	 */
	public void visitDIV_D(Instruction.DIV_D inst)
	{
		handleOp2(inst);
	}

	/**
	 * Visits {@code NEG_I} with vistor pattern.
	 *
	 * @param inst The inst to be visited.
	 */
	public void visitNEG_I(Instruction.NEG_I inst)
	{
		Value v = inst.x;
		if (v instanceof Constant)
		{
			setIntConstant(-v.asConstant().asInt());
		}
	}

	/**
	 * Visits {@code NEG_L} with vistor pattern.
	 *
	 * @param inst The inst to be visited.
	 */
	public void visitNEG_L(Instruction.NEG_L inst)
	{
		Value v = inst.x;
		if (v instanceof Constant)
		{
			setLongConstant(-v.asConstant().asLong());
		}
	}

	/**
	 * Visits {@code NEG_F} with vistor pattern.
	 *
	 * @param inst The inst to be visited.
	 */
	public void visitNEG_F(Instruction.NEG_F inst)
	{
		Value v = inst.x;
		if (v instanceof Constant)
		{
			setFloatConstant(-v.asConstant().asFloat());
		}
	}

	/**
	 * Visits {@code NEG_D} with vistor pattern.
	 *
	 * @param inst The inst to be visited.
	 */
	public void visitNEG_D(Instruction.NEG_D inst)
	{
		Value v = inst.x;
		if (v instanceof Constant)
		{
			setDoubleConstant(-v.asConstant().asDouble());
		}
	}
	public void visitINT_2LONG(Instruction.INT_2LONG inst)
	{
		visitValue(inst);
	}

	public void visitINT_2FLOAT(Instruction.INT_2FLOAT inst)
	{
		visitValue(inst);
	}

	public void visitINT_2DOUBLE(Instruction.INT_2DOUBLE inst)
	{
		visitValue(inst);
	}

	public void visitLONG_2INT(Instruction.LONG_2INT inst)
	{
		visitValue(inst);
	}

	public void visitLONG_2FLOAT(Instruction.LONG_2FLOAT inst)
	{
		visitValue(inst);
	}

	public void visitLONG_2DOUBLE(Instruction.LONG_2DOUBLE inst)
	{
		visitValue(inst);
	}

	public void visitFLOAT_2INT(Instruction.FLOAT_2INT inst)
	{
		visitValue(inst);
	}

	public void visitFLOAT_2LONG(Instruction.FLOAT_2LONG inst)
	{
		visitValue(inst);
	}

	public void visitFLOAT_2DOUBLE(Instruction.FLOAT_2DOUBLE inst)
	{
		visitValue(inst);
	}

	public void visitDOUBLE_2INT(Instruction.DOUBLE_2INT inst)
	{
		visitValue(inst);
	}

	public void visitDOUBLE_2LONG(Instruction.DOUBLE_2LONG inst)
	{
		visitValue(inst);
	}

	public void visitDOUBLE_2FLOAT(Instruction.DOUBLE_2FLOAT inst)
	{
		visitValue(inst);
	}

	public void visitINT_2BYTE(Instruction.INT_2BYTE inst)
	{
		visitValue(inst);
	}

	public void visitINT_2CHAR(Instruction.INT_2CHAR inst)
	{
		visitValue(inst);
	}

	public void visitINT_2SHORT(Instruction.INT_2SHORT inst)
	{
		visitValue(inst);
	}

	public void visitICmp(Instruction.ICmp inst)
	{
		Value x = inst.x;
		Value y = inst.y;
		Operator opcode = inst.opcode;
		// if the two inputs operands are same
		// the reduce it
		if (x == y)
		{
			switch (opcode)
			{
				case ICmpNE:
				case ICmpLT:
				case ICmpGT:
					setIntConstant(0);
					return;
				case ICmpEQ:
				case ICmpLE:
				case ICmpGE:
					setIntConstant(1);
					return;
			}
		}
		if (x.isConstant() && y.isConstant())
		{
			Integer res = foldLongCompare(opcode, x.asConstant().asInt(),
					y.asConstant().asInt());
			assert res != null;
			setIntConstant(res);
			return;
		}
	}

	public void visitLCmp(Instruction.LCmp inst)
	{
		Value x = inst.x;
		Value y = inst.y;
		Operator opcode = inst.opcode;
		// if the two inputs operands are same
		// the reduce it
		if (x == y)
		{
			switch (opcode)
			{
				case LCmpNE:
				case LCmpLT:
				case LCmpGT:
					setIntConstant(0);
					return;
				case LCmpEQ:
				case LCmpLE:
				case LCmpGE:
					setIntConstant(1);
					return;
			}
		}
		if (x.isConstant() && y.isConstant())
		{
			Integer res = foldLongCompare(opcode, x.asConstant().asLong(),
					y.asConstant().asLong());
			assert res != null;
			setIntConstant(res);
			return;
		}
	}

	/**
	 * Folds comparison operation over float point number.
	 * @param inst
	 */
	public void visitFCmp(Instruction.FCmp inst)
	{
		Value x = inst.x;
		Value y = inst.y;

		if (x == y && x.isConstant())
		{
			float xval = x.asConstant().asFloat();
			Integer res = foldFloatCompare(inst.opcode, xval, xval);
			assert res != null : "invalid float point number on float comparion";
			setIntConstant(res);
		}
		if (x.isConstant() && y.isConstant())
		{
			Integer res = foldFloatCompare(inst.opcode, x.asConstant().asFloat(),
					y.asConstant().asFloat());
			assert res != null : "invalid float point number on float comparion";
			setIntConstant(res);
		}
	}

	public void visitDCmp(Instruction.DCmp inst)
	{
		Value x = inst.x;
		Value y = inst.y;

		if (x == y && x.isConstant())
		{
			double xval = x.asConstant().asDouble();
			Integer res = foldDoubleCompare(inst.opcode, xval, xval);
			assert res != null : "invalid double point number on comparion op";
			setIntConstant(res);
		}
		if (x.isConstant() && y.isConstant())
		{
			Integer res = foldDoubleCompare(inst.opcode,
					x.asConstant().asDouble(), y.asConstant().asDouble());
			assert res != null : "invalid double point number on comparion op";
			setIntConstant(res);
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
	 * @param x      The first operand of operation.
	 * @param y      The second operand of operation.
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
	 * @param x      The first operand of operation.
	 * @param y      The second operand of operation.
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
	 * @param x      The first operand of operation.
	 * @param y      The second operand of operation.
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
	 * @param x      The first operand of operation.
	 * @param y      The second operand of operation.
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
									Constant.forInt(Utils.log2(y))));
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
									Constant.forLong(Utils.log2(y))));
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
							Constant.forInt((int) y)));
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
								new AND_L(x.kind, s.x, Constant.forLong(mask)));
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
								new AND_I(x.kind, s.x, Constant.forInt(mask)));
					}
				}
			}
		}
		if (y != shift)
		{
			// (y & mod) != y
			return setCanonical(new ShiftOp(x.kind, opcode, x,
					Constant.forInt((int) shift)));
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
		CiKind kind = inst.kind;
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
					Double val = foldDoubleOp2(opcode, x.asConstant().asDouble(),
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
	 * Swaps the left side operand and right, if the left operand is constant
	 * and this operator of instruction is commutative.
	 * @param inst
	 */
	private void moveConstantToRight(Op2 inst)
	{
		if (inst.x.isConstant() && inst.opcode.isCommutative())
			inst.swapOperands();
	}

	private Integer foldLongCompare(Operator opcode, long x ,long y)
	{
		switch (opcode)
		{
			case ICmpEQ:
			case LCmpEQ:
				return x == y ? 1 : 0;
			case ICmpLE:
			case LCmpLE:
				return x <= y ? 1 : 0;
			case ICmpLT:
			case LCmpLT:
				return x < y ? 1: 0;
			case ICmpNE:
			case LCmpNE:
				return x != y ? 1 : 0;
			case ICmpGE:
			case LCmpGE:
				return x >= y ? 1 : 0;
			case ICmpGT:
			case LCmpGT:
				return x > y ? 1 : 0;
		}
		return null;
	}

	private Integer foldFloatCompare(Operator opcode, float x, float y)
	{
		int result = 0;
		if (x < y)
			result = -1;
		else if (x > y)
			result = 1;
		if(opcode == Operator.FCmpLT)
		{
			if (Float.isNaN(x) || Float.isNaN(y))
				return -1;
		}
		else if (opcode == Operator.FCmpGT)
		{
			if (Float.isNaN(x) || Float.isNaN(y))
				return 1;

			return result;
		}
		// unknown comparison opcode.
		return null;
	}
	private Integer foldDoubleCompare(Operator opcode, double x, double y)
	{
		int result = 0;
		if (x < y)
			result = -1;
		else if (x > y)
			result = 1;
		if(opcode == Operator.DCmpLT)
		{
			if (Double.isNaN(x) || Double.isNaN(y))
				return -1;
		}
		else if (opcode == Operator.FCmpGT)
		{
			if (Double.isNaN(x) || Double.isNaN(y))
				return 1;

			return result;
		}
		// unknown comparison opcode.
		return null;
	}
	public void visitIfCmp_LT(Instruction.IfCmp_LT inst)
	{
		reduceIf(inst);
	}

	public void visitIfCmp_LE(Instruction.IfCmp_LE inst)
	{
		reduceIf(inst);
	}

	public void visitIfCmp_GT(Instruction.IfCmp_GT inst)
	{
		reduceIf(inst);
	}

	public void visitIfCmp_GE(Instruction.IfCmp_GE inst)
	{
		reduceIf(inst);
	}

	public void visitIfCmp_EQ(Instruction.IfCmp_EQ inst)
	{
		reduceIf(inst);
	}

	public void visitIfCmp_NEQ(Instruction.IfCmp_NEQ inst)
	{
		reduceIf(inst);
	}

	private void reduceIf(IntCmp inst)
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
		Operator opcode = inst.opcode;
		if (l.isConstant() && r.isConstant())
		{
			Boolean result = foldCondition(opcode, l.asConstant(), r.asConstant());;
			if (result != null)
			{
				setCanonical(new Goto(inst.successor(result)));
				return;
			}
		}
	}

	/**
	 * Reduces the conditional branch instruction when left side operand is equal
	 * to the right one.
	 * @param inst
	 */
	private void reduceReflexiveIf(IntCmp inst)
	{
		BasicBlock sux = null;
		switch (inst.opcode)
		{
			case IfEQ:
			case IfLE:
			case IfGE:
				sux = inst.successor(true);break;
			case IfNE:
			case IfLT:
			case IfGT:
					sux = inst.successor(false);break;
			default:
					throw new InternalError("should not reach here.");
		}
		setCanonical(new Goto(sux));
	}

	/**
	 * Attempts to fold two constant over the given operator, such as, LT, EQ, NE
	 * etc, and return the result of comparison.
	 * @param opcode    The comparison operator.
	 * @param l The left side operand.
	 * @param r The right side operand.
	 * @return  The result of comparison, return true if comparison successfully
	 * in specified op, otherwise, return false. Return null when the op is illegal.
	 */
	private Boolean foldCondition(Operator opcode, CiConstant l, CiConstant r)
	{
		CiKind lk = l.kind;
		switch (lk)
		{
			case Int:
			{
				int x = l.asInt();
				int y = r.asInt();
				switch (opcode)
				{
					case IfEQ: return x == y ? true : false;
					case IfLE: return x <= y ? true : false;
					case IfGE: return x >= y ? true : false;
					case IfNE: return x != y ? true : false;
					case IfLT: return x < y ? true : false;
					case IfGT: return x > y ? true : false;
				}
				break;
			}
			case Long:
			{
				long x = l.asLong();
				long y = r.asLong();
				switch (opcode)
				{
					case IfEQ: return x == y ? true : false;
					case IfLE: return x <= y ? true : false;
					case IfGE: return x >= y ? true : false;
					case IfNE: return x != y ? true : false;
					case IfLT: return x < y ? true : false;
					case IfGT: return x > y ? true : false;
				}
				break;
			}
		}
		return null;
	}
}
