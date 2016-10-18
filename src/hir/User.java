package hir;

import lir.ci.LIRKind;

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

    public final Operator opcode;

    /**
     * The name of this instruction.
     */
    protected String instName;

    /**
     * The basic block containing this Value.
     */
    protected BasicBlock bb;
    /**
     * This array with element of type Value represents all operands.
     */
	protected Value[] reservedOperands;
    /**
     * the number of values used by this user.
     */
    protected int numOperands = -1;

	public User(LIRKind kind, Operator opcode,
            String instName, int numOperands)
	{
		super(kind);
        this.opcode = opcode;
        this.instName = instName;
        this.id = -1;
        assert (numOperands >= 0);
        this.reservedOperands = new Value[numOperands];
	}

    /**
     * Obtains a reference to the operand at index position.
     * @param index the position indexed to target element.
     * @return the index-th operand.
     */
	public Value operand(int index)
    {
        assert (index >= 0 && index < numOperands);
        return reservedOperands[index];
    }

    /**
     * set element at specified position with {@code val}
     * @param index
     * @param val
     */
    public void setOperand(int index, Value val)
    {
        assert (index >= 0 && index < numOperands);
        reservedOperands[index] = val;
    }

    /**
     * obtains the number of reservedOperands of this instruction.
     * @return
     */
    public int getNumOfOperands()
    {
        return numOperands;
    }
}
