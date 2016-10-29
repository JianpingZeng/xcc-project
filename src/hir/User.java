package hir;

import lir.ci.LIRKind;
import type.Type;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class User extends Value
{
    /**
     * Mainly for register allocation.
     */
    public int id;

    public Operator opcode;

    /**
     * The name of this instruction.
     */
    protected String instName;

    /**
     * The basic block containing this Value.
     */
    protected BasicBlock bb;

    /**
     * the number of values used by this user.
     */
    protected int numOperands = -1;

    /**
     * This array with element of type Value represents all operands.
     */
    protected ArrayList<Use> operandList;

	public User(Type ty, Operator op)
	{
		super(ty, ValueKind.InstructionVal + op.index);
        opcode = op;
        id = -1;
	}

	protected void reserve(int numOperands)
    {
        assert numOperands>0;
        operandList = new ArrayList<>(numOperands);
    }

    /**
     * Obtains a reference to the operand at index position.
     * @param index the position indexed to target element.
     * @return the index-th operand.
     */
	public Value operand(int index)
    {
        assert (index >= 0 && index < numOperands);
        return operandList.get(index).getValue();
    }

    /**
     * set element at specified position with {@code val}
     * @param index
     * @param val
     */
    public void setOperand(int index, Value val)
    {
        assert (index >= 0 && index < numOperands);
        operandList.get(index).setValue(val);
    }

    /**
     * obtains the number of reservedOperands of this instruction.
     * @return
     */
    public int getNumOfOperands()
    {
        return operandList.size();
    }
}
