package lir;

import hir.Value;
import lir.ci.LIRKind;
import lir.ci.LIRValue;
import lir.ci.LIRVariable;
import lir.linearScan.OperandPool;
import utils.Util;

/**
 * A helper utility for loading the result of an instruction for being used by another
 * instruction. This helper takes into account the specifics of the consuming
 * instruction such as whether it requires the input LIROperand to be in memory
 * or a register, any register length requirements of the input LIROperand, and
 * whether the usage has the side-effect of overwriting the input LIROperand. To
 * satisfy these constraints, an intermediate LIROperand may be created and move
 * instruction inserted to copy the output of the producer instruction into the
 * intermediate LIROperand.
 *
 * @author Xlous.zeng
 */
public class LIRItem
{

	/**
	 * The instruction whose usage by another instruction is being modeled by this object.
	 * An instruction {@code x} uses instruction {@code y} if the LIROperand result
	 * of {@code y} is an input LIROperand of {@code x}.
	 */
	public Value instruction;

	/**
	 * The LIR context of this helper object.
	 */
	private final LIRGenerator gen;

	/**
	 * The LIROperand holding the result of this item's {@linkplain #instruction}.
	 */
	private LIRValue resultOperand;

	/**
	 * Denotes if the use of the instruction's {@linkplain #resultOperand result LIROperand}
	 * overwrites the value in the LIROperand. That is, the use both uses and defines the
	 * LIROperand. In this case, an {@linkplain #intermediateOperand intermediate LIROperand}
	 * is created for the use so that other consumers of this item's {@linkplain #instruction}
	 * are not impacted.
	 */
	private boolean destructive;

	/**
	 * @see #destructive
	 */
	private LIRValue intermediateOperand;

	public LIRItem(Value value, LIRGenerator gen)
	{
		this.gen = gen;
		setInstruction(value);
	}

	public void setInstruction(Value instruction)
	{
		this.instruction = instruction;
		if (instruction != null)
		{
			resultOperand = gen.makeOperand(instruction);
		}
		else
		{
			resultOperand = LIRValue.IllegalValue;
		}
		intermediateOperand = LIRValue.IllegalValue;
	}

	public LIRItem(LIRGenerator gen)
	{
		this.gen = gen;
		setInstruction(null);
	}

	public void loadItem(LIRKind kind)
	{
		if (kind == LIRKind.Byte || kind == LIRKind.Boolean)
		{
			loadByteItem();
		}
		else
		{
			loadItem();
		}
	}

	public void loadForStore(LIRKind kind)
	{
		if (gen.canStoreAsConstant(instruction, kind))
		{
			resultOperand = instruction.LIROperand();
			if (!resultOperand.isConstant())
			{
				resultOperand = instruction.asLIRConstant();
			}
		}
		else if (kind == LIRKind.Byte || kind == LIRKind.Boolean)
		{
			loadByteItem();
		}
		else
		{
			loadItem();
		}
	}

	public LIRValue result()
	{
		assert !destructive || !resultOperand
				.isRegister() : "shouldn't use setDestroysRegister with physical LIRRegisters";
		if (destructive && (resultOperand.isVariable() || resultOperand
				.isConstant()))
		{
			if (intermediateOperand.isIllegal())
			{
				intermediateOperand = gen.newVariable(instruction.kind);
				gen.lir.move(resultOperand, intermediateOperand);
			}
			return intermediateOperand;
		}
		else
		{
			return resultOperand;
		}
	}

	public void setDestroysRegister()
	{
		destructive = true;
	}

	/**
	 * Determines if the LIROperand is in a stack slot.
	 */
	public boolean isStack()
	{
		return resultOperand.isAddress() || resultOperand.isStackSlot();
	}

	/**
	 * Determines if the LIROperand is in a register or may be
	 * resolved to a register by the register allocator.
	 */
	public boolean isRegisterOrVariable()
	{
		return resultOperand.isVariableOrRegister();
	}

	public void loadByteItem()
	{
		if (gen.backend.targetMachine.arch.isX86())
		{
			loadItem();
			LIRValue res = result();

			if (!res.isVariable() || !gen.operands.mustBeByteRegister(res))
			{
				// make sure that it is a byte register
				assert !instruction.kind.isFloat() && !instruction.kind
						.isDouble() : "can't load floats in byte register";
				LIRValue reg = gen.operands.newVariable(LIRKind.Byte,
						OperandPool.VariableFlag.MustBeByteRegister);
				gen.lir.move(res, reg);
				resultOperand = reg;
			}
		}
		else if (gen.backend.targetMachine.arch.isSPARC())
		{
			loadItem();
		}
		else
		{
			Util.shouldNotReachHere();
		}
	}

	public void loadNonconstant()
	{
		if (gen.backend.targetMachine.arch.isX86())
		{
			LIRValue r = instruction.LIROperand();
			if (r.isConstant())
			{
				resultOperand = r;
			}
			else
			{
				loadItem();
			}
		}
		else if (gen.backend.targetMachine.arch.isSPARC())
		{
			LIRValue r = instruction.LIROperand();
			if (gen.canInlineAsConstant(instruction))
			{
				if (!r.isConstant())
				{
					r = instruction.asLIRConstant();
				}
				resultOperand = r;
			}
			else
			{
				loadItem();
			}
		}
		else
		{
			Util.shouldNotReachHere();
		}
	}

	private void setResult(LIRVariable operand)
	{
		gen.setResult(instruction, operand);
		resultOperand = operand;
	}

	/**
	 * Creates an LIROperand containing the result of {@linkplain #instruction input instruction}.
	 */
	public void loadItem()
	{
		if (result().isIllegal())
		{
			// update the item's result
			resultOperand = instruction.LIROperand();
		}
		LIRValue result = result();
		if (!result.isVariableOrRegister())
		{
			LIRVariable operand;
			operand = gen.newVariable(instruction.kind);
			gen.lir.move(result, operand);
			if (result.isConstant())
			{
				resultOperand = operand;
			}
			else
			{
				setResult(operand);
			}
		}
	}

	@Override
	public String toString()
	{
		return result().toString();
	}

	public void loadItemForce(LIRValue reg)
	{
		LIRValue r = result();
		if (r != reg)
		{
			if (r.kind != reg.kind)
			{
				// moves between different types need an intervening spill slot
				r = gen.forceToSpill(r, reg.kind, true);
			}
			gen.lir.move(r, reg);
			resultOperand = reg;
		}
	}
}

