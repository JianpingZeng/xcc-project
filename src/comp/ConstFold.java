package comp;

import java.util.List;
import type.Type;
import type.TypeTags;
import utils.Context;

/**
 * A auxiliary class for constant folding, used by attribute phares.
 *
 * @author JianpingZeng
 * @version 1.0
 */
public class ConstFold implements TypeTags, OpCodes
{
	private static final Context.Key constFoldKey = new Context.Key();
	private Symtab syms;

	public static ConstFold instance(Context context)
	{
		ConstFold instance = (ConstFold) context.get(constFoldKey);
		if (instance == null) instance = new ConstFold(context);
		return instance;
	}

	public ConstFold(Context context)
	{
		context.put(constFoldKey, this);
		syms = Symtab.instance(context);
	}

	static Integer minusOne = new Integer(-1);
	static Integer zero = new Integer(0);
	static Integer one = new Integer(1);

	/**
	 * Convert boolean to integer (true = 1, false = 0).
	 */
	private static Integer b2i(boolean b)
	{
		return b ? one : zero;
	}

	private static int intValue(Object x)
	{
		return ((Number) x).intValue();
	}

	private static long longValue(Object x)
	{
		return ((Number) x).longValue();
	}

	private static float floatValue(Object x)
	{
		return ((Number) x).floatValue();
	}

	private static double doubleValue(Object x)
	{
		return ((Number) x).doubleValue();
	}

	/**
	 * Fold binary or unary operation, returning constant type reflecting the
	 * operations result. Return null if fold failed due to an arithmetic
	 * exception.
	 * @param opcode The operation's opcode instruction.
	 * 
	 * @param argtypes The operation's argument types (a list of length 1 or 2).
	 *            Argument types are assumed to have non-null constValue's.
	 */
	Type fold(int opcode, List<Type> argtypes)
	{
		int argCount = argtypes.size();
		if (argCount == 1)
			return fold1(opcode, argtypes.get(0));
		else if (argCount == 2)
			return fold2(opcode, argtypes.get(0), argtypes.get(1));
		else
			throw new AssertionError();
	}

	/**
	 * Fold unary operation.
	 * @param opcode The operation's opcode instruction (usually a byte code),
	 *            as entered by class Symtab. opcode's ifeq to ifge are for
	 *            postprocessing xcmp; ifxx pairs of instructions.
	 * @param operand The operation's operand type. Argument types are assumed
	 *            to have non-null constValue's.
	 */
	Type fold1(int opcode, Type operand)
	{
		try
		{
			Object od = operand.constValue;
			switch (opcode)
			{
				case nop:
					return operand;

				case ineg:
					return syms.intType.constType(new Integer(-intValue(od)));

				case ixor:
					return syms.intType.constType(new Integer(~intValue(od)));

				case bool_not:
					return syms.boolType.constType(b2i(intValue(od) == 0));

				case ifeq:
					return syms.boolType.constType(b2i(intValue(od) == 0));

				case ifne:
					return syms.boolType.constType(b2i(intValue(od) != 0));

				case iflt:
					return syms.boolType.constType(b2i(intValue(od) < 0));

				case ifgt:
					return syms.boolType.constType(b2i(intValue(od) > 0));

				case ifle:
					return syms.boolType.constType(b2i(intValue(od) <= 0));

				case ifge:
					return syms.boolType.constType(b2i(intValue(od) >= 0));

				case lneg:
					return syms.longType.constType(new Long(-longValue(od)));

				case lxor:
					return syms.longType.constType(new Long(~longValue(od)));

				case fneg:
					return syms.floatType.constType(new Float(-floatValue(od)));

				case dneg:
					return syms.doubleType.constType(new Double(
					        -doubleValue(od)));

				default:
					return null;

			}
		}
		catch (ArithmeticException e)
		{
			return null;
		}
	}

	/**
	 * Fold binary operation.
	 * @param opcode The operation's opcode instruction (usually a byte code),
	 *            as entered by class Symtab. opcode's ifeq to ifge are for
	 *            postprocessing xcmp; ifxx pairs of instructions.
	 * @param left The type of the operation's left operand.
	 * @param right The type of the operation's right operand.
	 */
	Type fold2(int opcode, Type left, Type right)
	{
		try
		{
			if (opcode > preMask)
			{
				Type t1 = fold2(opcode >> preShift, left, right);
				return (t1.constValue == null) ? t1 : fold1(opcode
				        & preMask, t1);
			}
			else
			{
				Object l = left.constValue;
				Object r = right.constValue;
				switch (opcode)
				{
					case iadd:
						return syms.intType.constType(new Integer(intValue(l)
						        + intValue(r)));

					case isub:
						return syms.intType.constType(new Integer(intValue(l)
						        - intValue(r)));

					case imul:
						return syms.intType.constType(new Integer(intValue(l)
						        * intValue(r)));

					case idiv:
						return syms.intType.constType(new Integer(intValue(l)
						        / intValue(r)));

					case imod:
						return syms.intType.constType(new Integer(intValue(l)
						        % intValue(r)));

					case iand:
						return (left.tag == BOOL ? syms.boolType
						        : syms.intType).constType(new Integer(
						        intValue(l) & intValue(r)));

					case bool_and:
						return syms.boolType
						        .constType(b2i((intValue(l) & intValue(r)) != 0));

					case ior:
						return (left.tag == BOOL ? syms.boolType
						        : syms.intType).constType(new Integer(
						        intValue(l) | intValue(r)));

					case bool_or:
						return syms.boolType
						        .constType(b2i((intValue(l) | intValue(r)) != 0));

					case ixor:
						return (left.tag == BOOL ? syms.boolType
						        : syms.intType).constType(new Integer(
						        intValue(l) ^ intValue(r)));

					case ishl:

					case ishll:
						return syms.intType.constType(new Integer(
						        intValue(l) << intValue(r)));

					case ishr:

					case ishrl:
						return syms.intType.constType(new Integer(
						        intValue(l) >> intValue(r)));

					case iushr:

					case iushrl:
						return syms.intType.constType(new Integer(
						        intValue(l) >>> intValue(r)));

					case if_icmpeq:
						return syms.boolType
						        .constType(b2i(intValue(l) == intValue(r)));

					case if_icmpne:
						return syms.boolType
						        .constType(b2i(intValue(l) != intValue(r)));

					case if_icmplt:
						return syms.boolType
						        .constType(b2i(intValue(l) < intValue(r)));

					case if_icmpgt:
						return syms.boolType
						        .constType(b2i(intValue(l) > intValue(r)));

					case if_icmple:
						return syms.boolType
						        .constType(b2i(intValue(l) <= intValue(r)));

					case if_icmpge:
						return syms.boolType
						        .constType(b2i(intValue(l) >= intValue(r)));

					case ladd:
						return syms.longType.constType(new Long(longValue(l)
						        + longValue(r)));

					case lsub:
						return syms.longType.constType(new Long(longValue(l)
						        - longValue(r)));

					case lmul:
						return syms.longType.constType(new Long(longValue(l)
						        * longValue(r)));

					case ldiv:
						return syms.longType.constType(new Long(longValue(l)
						        / longValue(r)));

					case lmod:
						return syms.longType.constType(new Long(longValue(l)
						        % longValue(r)));

					case land:
						return syms.longType.constType(new Long(longValue(l)
						        & longValue(r)));

					case lor:
						return syms.longType.constType(new Long(longValue(l)
						        | longValue(r)));

					case lxor:
						return syms.longType.constType(new Long(longValue(l)
						        ^ longValue(r)));

					case lshl:

					case lshll:
						return syms.longType.constType(new Long(
						        longValue(l) << intValue(r)));

					case lshr:

					case lshrl:
						return syms.longType.constType(new Long(
						        longValue(l) >> intValue(r)));

					case lushr:
						return syms.longType.constType(new Long(
						        longValue(l) >>> intValue(r)));

					case lcmp:
						if (longValue(l) < longValue(r))
							return syms.intType.constType(minusOne);
						else if (longValue(l) > longValue(r))
							return syms.intType.constType(one);
						else
							return syms.intType.constType(zero);

					case fadd:
						return syms.floatType.constType(new Float(floatValue(l)
						        + floatValue(r)));

					case fsub:
						return syms.floatType.constType(new Float(floatValue(l)
						        - floatValue(r)));

					case fmul:
						return syms.floatType.constType(new Float(floatValue(l)
						        * floatValue(r)));

					case fdiv:
						return syms.floatType.constType(new Float(floatValue(l)
						        / floatValue(r)));

					case fmod:
						return syms.floatType.constType(new Float(floatValue(l)
						        % floatValue(r)));

					case fcmpg:

					case fcmpl:
						if (floatValue(l) < floatValue(r))
							return syms.intType.constType(minusOne);
						else if (floatValue(l) > floatValue(r))
							return syms.intType.constType(one);
						else if (floatValue(l) == floatValue(r))
							return syms.intType.constType(zero);
						else if (opcode == fcmpg)
							return syms.intType.constType(one);
						else
							return syms.intType.constType(minusOne);

					case dadd:
						return syms.doubleType.constType(new Double(
						        doubleValue(l) + doubleValue(r)));

					case dsub:
						return syms.doubleType.constType(new Double(
						        doubleValue(l) - doubleValue(r)));

					case dmul:
						return syms.doubleType.constType(new Double(
						        doubleValue(l) * doubleValue(r)));

					case ddiv:
						return syms.doubleType.constType(new Double(
						        doubleValue(l) / doubleValue(r)));

					case dmod:
						return syms.doubleType.constType(new Double(
						        doubleValue(l) % doubleValue(r)));

					case dcmpg:

					case dcmpl:
						if (doubleValue(l) < doubleValue(r))
							return syms.intType.constType(minusOne);
						else if (doubleValue(l) > doubleValue(r))
							return syms.intType.constType(one);
						else if (doubleValue(l) == doubleValue(r))
							return syms.intType.constType(zero);
						else if (opcode == dcmpg)
							return syms.intType.constType(one);
						else
							return syms.intType.constType(minusOne);

					default:
						return null;

				}
			}
		}
		catch (ArithmeticException e)
		{
			return null;
		}
	}

	/**
	 * Coerce constant type to target type.
	 * 
	 * @param sourcetype The source type of the coercion, which is assumed to be
	 *            a constant type compatible with target type.
	 * @param targettype The target type of the coercion.
	 */
	Type coerce(Type sourcetype, Type targettype)
	{
		if (sourcetype.basetype() == targettype.basetype()) return sourcetype;

		if (sourcetype.tag <= DOUBLE)
		{
			Object n = sourcetype.constValue;

			switch (targettype.tag)
			{
				case BYTE:
					return syms.byteType.constType(new Integer(
					        (byte) intValue(n)));

				case CHAR:
					return syms.charType.constType(new Integer(
					        (char) intValue(n)));

				case SHORT:
					return syms.shortType.constType(new Integer(
					        (short) intValue(n)));

				case INT:
					return syms.intType.constType(new Integer(intValue(n)));

				case LONG:
					return syms.longType.constType(new Long(longValue(n)));

				case FLOAT:
					return syms.floatType.constType(new Float(floatValue(n)));

				case DOUBLE:
					return syms.doubleType
					        .constType(new Double(doubleValue(n)));

			}
		}
		return targettype;
	}
}
