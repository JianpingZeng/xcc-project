package lir.linearScan;

/**
 * @author Xlous.zeng
 */

import static lir.linearScan.OperandPool.VariableFlag.MustBeByteRegister;
import hir.Value;

import java.util.ArrayList;
import java.util.BitSet;

import lir.backend.Architecture;
import lir.backend.TargetMachine;
import lir.ci.LIRKind;
import lir.ci.LIRRegister;
import lir.ci.LIRValue;
import lir.ci.LIRVariable;

/**
 * An ordered, 0-based indexable pool of instruction operands for a method being
 * compiled. The physical {@linkplain LIRRegister LIRRegisters} of the platform
 * occupy the front of the pool (starting at index 0) followed by {@linkplain
 * LIRVariable variable} operands. The index of an LIROperand in the pool is its
 * {@linkplain #operandNumber(LIRValue) LIROperand number}.
 * <p>
 * This source code refers to the original HotSpot C1 source code, this pool
 * corresponds to the "flat register file" mentioned in c1_LinearScan.cpp.
 */
public final class OperandPool
{

	public static final int INITIAL_VARIABLE_CAPACITY = 20;

	/**
	 * The physical LIRRegisters occupying the front part of the LIROperand pool.
	 * This is the complete {@linkplain Architecture#LIRRegisters register set}
	 * of the target architecture, not just the allocatable LIRRegisters.
	 */
	private final LIRRegister[] LIRRegisters;

	/**
	 * The variable operands allocated from this pool. The {@linkplain #operandNumber(LIRValue) number}
	 * of the first variable LIROperand in this pool is one greater than the
	 * number of the last physical register LIROperand in the pool.
	 */
	private final ArrayList<LIRVariable> variables;

	/**
	 * Map from a {@linkplain LIRVariable#index variable index} to the instruction
	 * whose result is stored in the denoted variable. This map is only populated
	 * and used if is {@code true}.
	 */
	private final ArrayList<Value> variableDefs;

	/**
	 * The {@linkplain #operandNumber(LIRValue) number} of the first variable
	 * LIROperand {@linkplain #newVariable(LIRKind) allocated} from this pool.
	 */
	private final int firstVariableNumber;

	/**
	 * Records which variable operands have the {@link VariableFlag#MustBeByteRegister} flag set.
	 */
	private BitSet mustBeByteRegister;

	/**
	 * Records which variable operands have the {@link VariableFlag#MustStartInMemory} flag set.
	 */
	private BitSet mustStartInMemory;

	/**
	 * Records which variable operands have the {@link VariableFlag#MustStayInMemory} flag set.
	 */
	private BitSet mustStayInMemory;

	/**
	 * Flags that can be set for {@linkplain LIRValue#isVariable() variable} operands.
	 */
	public enum VariableFlag
	{
		/**
		 * Denotes a variable that needs to be assigned a memory location
		 * at the beginning, but may then be loaded in a register.
		 */
		MustStartInMemory,

		/**
		 * Denotes a variable that needs to be assigned a memory location
		 * at the beginning and never subsequently loaded in a register.
		 */
		MustStayInMemory,

		/**
		 * Denotes a variable that must be assigned to a byte-sized register.
		 */
		MustBeByteRegister;

		public static final VariableFlag[] VALUES = values();
	}

	private static BitSet set(BitSet map, LIRVariable variable)
	{
		if (map == null || map.size() <= variable.index)
		{
			map = new BitSet(2*variable.index + 1);
		}
		map.set(variable.index);
		return map;
	}

	private static boolean get(BitSet map, LIRVariable variable)
	{
		if (map == null || map.size() <= variable.index)
		{
			return false;
		}
		return map.get(variable.index);
	}

	/**
	 * Creates a new LIROperand pool from certainly target machine.
	 *
	 * @param target description of the targetAbstractLayer architecture for a compilation
	 */
	public OperandPool(TargetMachine target)
	{
		LIRRegister[] LIRRegisters = target.arch.LIRRegisters;
		this.firstVariableNumber = LIRRegisters.length;
		this.LIRRegisters = LIRRegisters;
		variables = new ArrayList<>(INITIAL_VARIABLE_CAPACITY);
		variableDefs = new ArrayList<>(INITIAL_VARIABLE_CAPACITY);
	}

	/**
	 * Allocates a new {@linkplain LIRVariable variable} LIROperand with specified
	 * {@code LIRKind} from {@linkplain OperandPool}.
	 * <p>
	 *     <strong>Note that</strong> if the specified kind is qualified by
	 *     {@linkplain LIRKind#Boolean} or {@linkplain LIRKind#Byte}, the flag of
	 *     this virtual register must be seted with {@linkplain VariableFlag#MustBeByteRegister}
	 * </p>
	 *
	 * @param kind the kind of the variable
	 * @return a new variable
	 */
	public LIRVariable newVariable(LIRKind kind)
	{
		return newVariable(kind, kind == LIRKind.Boolean || kind == LIRKind.Byte ?
				MustBeByteRegister :
				null);
	}

	/**
	 * Creates a new {@linkplain LIRVariable variable} LIROperand with {@link LIRKind}
	 * and {@link OperandPool.VariableFlag}.
	 *
	 * @param kind the kind of the variable
	 * @param flag a flag that is set for the new variable LIROperand (ignored
	 *                if {@code null})
	 * @return a new variable LIROperand
	 */
	public LIRVariable newVariable(LIRKind kind, VariableFlag flag)
	{
		assert kind != LIRKind.Void;

		int varIndex = variables.size();
		LIRVariable var = LIRVariable.get(kind, varIndex);
		if (flag == MustBeByteRegister)
		{
			mustBeByteRegister = set(mustBeByteRegister, var);
		}
		else if (flag == VariableFlag.MustStartInMemory)
		{
			mustStartInMemory = set(mustStartInMemory, var);
		}
		else if (flag == VariableFlag.MustStayInMemory)
		{
			mustStayInMemory = set(mustStayInMemory, var);
		}
		else
		{
			assert flag == null;
		}
		variables.add(var);
		return var;
	}

	/**
	 * Gets the unique number for an LIROperand contained in this pool.
	 *
	 * @param operand an LIROperand
	 * @return the unique number for {@code LIROperand} in the range {@code [0 .. length())}
	 */
	public int operandNumber(LIRValue operand)
	{
		if (operand.isRegister())
		{
			int number = operand.asRegister().number;
			assert number < firstVariableNumber;
			return number;
		}
		assert operand.isVariable();
		return firstVariableNumber + ((LIRVariable) operand).index;
	}

	/**
	 * Gets the LIROperand in this pool denoted by a given LIROperand number.
	 *
	 * @param operandNumber a value that must be in the range {@code [0 .. length())}
	 * @return the LIROperand in this pool denoted by {@code operandNumber}
	 */
	public LIRValue operandFor(int operandNumber)
	{
		if (operandNumber < firstVariableNumber)
		{
			assert operandNumber >= 0;
			return LIRRegisters[operandNumber].asValue();
		}
		int index = operandNumber - firstVariableNumber;
		LIRVariable variable = variables.get(index);
		assert variable.index == index;
		return variable;
	}

	/**
	 * Records that the result of {@code instruction} is stored in {@code result}.
	 *
	 * @param result      the variable storing the result of {@code instruction}
	 * @param instruction an instruction that produces a result (i.e. pushes a
	 *                       value to the stack)
	 */
	public void recordResult(LIRVariable result, Value instruction)
	{
		while (variableDefs.size() <= result.index)
		{
			variableDefs.add(null);
		}
		variableDefs.set(result.index, instruction);
	}

	/**
	 * Gets the instruction whose result is recorded in a given variable.
	 *
	 * @param result the variable storing the result of an instruction
	 * @return the instruction that stores its result in {@code result}
	 */
	public Value instructionForResult(LIRVariable result)
	{
		if (variableDefs.size() > result.index)
		{
			return variableDefs.get(result.index);
		}
		return null;
	}

	public boolean mustStartInMemory(LIRVariable operand)
	{
		return get(mustStartInMemory, operand) || get(mustStayInMemory,
				operand);
	}

	public boolean mustStayInMemory(LIRVariable operand)
	{
		return get(mustStayInMemory, operand);
	}

	public boolean mustBeByteRegister(LIRValue operand)
	{
		return get(mustBeByteRegister, (LIRVariable) operand);
	}

	public void setMustBeByteRegister(LIRVariable operand)
	{
		mustBeByteRegister = set(mustBeByteRegister, operand);
	}

	/**
	 * Gets the number of operands in this pool. This value will increase by 1 for
	 * each new variable LIROperand {@linkplain #newVariable(LIRKind) allocated}
	 * from this pool.
	 */
	public int size()
	{
		return firstVariableNumber + variables.size();
	}

	/**
	 * Gets the highest LIROperand number for a register LIROperand in this pool.
	 * This value will never change for the lifetime of this pool.
	 */
	public int maxRegisterNumber()
	{
		return firstVariableNumber - 1;
	}
}
