package hir;

import lir.ci.LIRKind;
import utils.Log;
import utils.Util;

/**
 * This class is an abstract representation of Quadruple. In this class,
 * subclass of @ {@code Instruction} represents arithmetic and logical
 * operation, control flow operators,PhiNode assignment, function calling
 * conditional statement.
 *
 * @author Xlous.zeng
 * @version 1.0
 * @see BasicBlock
 * @see User
 * @see Value
 * @see Use
 */
public abstract class Instruction extends User
{
    public Instruction(LIRKind kind, Operator opcode, String instName,
            int numOperands, Instruction insertBefore)
    {
        super(kind, opcode, instName, numOperands);

        if (insertBefore != null)
        {
            assert (insertBefore.getParent()
                    != null) : "Instruction to insert before is not in a basic block";
            insertBefore.getParent().insertBefore(this, insertBefore);
        }
    }

    public Instruction(LIRKind kind, Operator opcode, String instName,
            int numOperands, BasicBlock insertAtEnd)
    {
        super(kind, opcode, instName, numOperands);

        // append this instruction into the basic block
        assert (insertAtEnd
                != null) : "Basic block to append to may not be NULL!";
        insertAtEnd.appendInst(this);
    }

    public Instruction(LIRKind kind, Operator opcode, String instName,
            int numOperands)
    {
        this(kind, opcode, instName, numOperands, (Instruction) null);
    }

    public Instruction(LIRKind kind, Operator opcode, int numOperands)
    {
        this(kind, opcode, "", numOperands);
    }

    /**
     * Obtains the basic block which holds this instruction.
     *
     * @return
     */
    public BasicBlock getParent()
    {
        return bb;
    }

    /**
     * Updates the basic block holds multiple instructions.
     *
     * @param bb
     */
    public void setParent(BasicBlock bb)
    {
        this.bb = bb;
    }

    /**
     * Erases this instruction from it's parent basic block.
     */
    public void eraseFromBasicBlock()
    {
        assert (this.bb == null)
                : "The basic block where the instruction reside to be erased!";
        bb.removeInst(this);
    }

    /**
     * Inserts an specified instruction into basic block immediately before
     * specified instruction.
     *
     * @param insertPos the position where this instruction will be inserted before.
     */
    public void insertAfter(Instruction insertPos)
    {
        assert (insertPos != null);
        BasicBlock bb = insertPos.getParent();
        int index = bb.lastIndexOf(insertPos);
        if (index >= 0 && index < bb.size())
            bb.insertAt(this, index + 1);
    }

    /**
     * Inserts an instruction into the instructions list before this itself.
     *
     * @param insertPos An instruction to be inserted.
     */
    public void insertBefore(Instruction insertPos)
    {
        assert (insertPos != null);
        BasicBlock bb = insertPos.getParent();
        int index = bb.lastIndexOf(insertPos);
        if (index >= 0 && index < bb.size())
            bb.insertAt(this, index);
    }

    /**
     * Gets the text format of this Instruction.
     *
     * @return
     */
    public String toString()
    {
        return opcode != null ? opcode.opName : "";
    }

    /**
     * Links the user value and its user.
     *
     * @param v The used {@code Value}.
     * @param u The {@code Instruction} or other instance of {@code User}.
     */
    void use(Value v, User u)
    {
        assert v != null && u != null;
        v.addUse(new Use(v, u));
    }

    /**
     * Obtains the name of this instruction.
     *
     * @return return the name of this instruction.
     */
    public String name()
    {
        return instName;
    }

    /**
     * An interface for InstructionVisitor invoking.
     *
     * @param visitor The instance of InstructionVisitor.
     */
    public abstract void accept(InstructionVisitor visitor);

    /**
     * The abstract base class definition for unary operator.
     */
    public static class Op1 extends Instruction
    {
        /**
         * Constructs unary operation.
         *
         * @param kind   The inst kind of ret.
         * @param opcode The operator code for this instruction.
         * @param x      The sole LIROperand.
         */
        public Op1(LIRKind kind, Operator opcode, Value x,
                String name, Instruction insertBefore)
        {
            super(kind, opcode, name, 1, insertBefore);
            setOperand(0, x);
            use(x, this);
        }

        /**
         * Constructs unary operation.
         *
         * @param kind   The inst kind of ret.
         * @param opcode The operator code for this instruction.
         * @param x      The sole LIROperand.
         */
        public Op1(LIRKind kind, Operator opcode, Value x,
                String name)
        {
            this(kind, opcode, x, name, (Instruction)null);
        }

        /**
         *
         * @param kind
         * @param opcode
         * @param x
         * @param name
         * @param insertAtEnd
         */
        public  Op1(LIRKind kind, Operator opcode, Value x,
                String name, BasicBlock insertAtEnd)
        {
            super(kind, opcode, name, 1, insertAtEnd);
            setOperand(0, x);
            use(x, this);
        }

        @Override
        public int valueNumber()
        {
            return Util.hash1(opcode.index, operand(0));
        }

        @Override
        public boolean equals(Object other)
        {
            if (other == null)
                return false;
            if (other == this)
                return true;
            if (!(other instanceof Op1))
                return false;

            Op1 op = (Op1) other;
            return kind == op.kind && opcode.equals(op.opcode)
                    && operand(0).equals(op.operand(0));
        }

        /**
         * An interface for InstructionVisitor invoking.
         *
         * @param visitor The instance of InstructionVisitor.
         */
        @Override
        public void accept(InstructionVisitor visitor)
        {

        }
    }

    /**
     * This class just for binary operation definition.
     *
     * @author Xlous.zeng
     */
    public static class Op2 extends Instruction
    {
        public Op2(LIRKind kind, Operator opcode,
                Value x, Value y, String name)
        {
            this(kind, opcode, x, y, name, (Instruction)null);
        }

        public Op2(LIRKind kind, Operator opcode,
                Value x, Value y, String name,
                Instruction insertBefore)
        {
            super(kind, opcode, name, 2, insertBefore);
            assert x.kind == y.kind
                    : "Can not create binary operation with two operands of differing type.";
            init(x, y);
        }

        public Op2(LIRKind kind, Operator opcode,
                Value x, Value y, String name,
                BasicBlock insertAtEnd)
        {
            super(kind, opcode, name, 2, insertAtEnd);
            assert x.kind == y.kind
                    : "Can not create binary operation with two operands of differing type.";
            init(x, y);
        }

        private void init(Value x, Value y)
        {
            setOperand(0, x);
            setOperand(1, y);
            use(x, this);
            use(y, this);
        }

        /**
         * This method is used for attempting to swap the two operands of this
         * binary instruction.
         */
        public void swapOperands()
        {
            Value temp = operand(0);
            setOperand(0, operand(1));
            setOperand(1, temp);
        }

        @Override
        public int valueNumber()
        {
            return Util.hash2(opcode.index, operand(0), operand(1));
        }

        @Override
        public boolean equals(Object other)
        {
            if (other == null)
                return false;
            if (other == this)
                return true;
            if (!(other instanceof Op2))
                return false;

            Op2 op = (Op2) other;
            Value x = operand(0);
            Value y = operand(1);
            return kind == op.kind && opcode.equals(op.opcode)
                    && x.equals(op.operand(0))
                    && y.equals(op.operand(1));
        }

        /**
         * An interface for InstructionVisitor invoking.
         *
         * @param visitor The instance of InstructionVisitor.
         */
        @Override
        public void accept(InstructionVisitor visitor)
        {

        }
    }

    public static class Cmp extends Instruction
    {
        private Condition cond;

        /**
         * Creates a instance of different subclass served as different
         * date type according to the date type.
         *
         * @param kind  The ret date type.
         * @param left  The left LIROperand.
         * @param right the right LIROperand.
         * @param cond  The condition object.
         * @return According comparison instruction.
         */
        public Cmp(LIRKind kind, Value left, Value right, Condition cond,
                String name)
        {
            this(kind, left, right, cond, name, (Instruction)null);
        }

        /**
         * Creates a instance of different subclass served as different
         * date type according to the date type.
         *
         * @param kind  The ret date type.
         * @param left  The left LIROperand.
         * @param right the right LIROperand.
         * @param cond  The condition object.
         * @return According comparison instruction.
         */
        public Cmp(LIRKind kind, Value left,
                Value right, Condition cond,
                String name,
                Instruction insertBefore)
        {
            super(kind, Operator.Cmp, name, 2, insertBefore);
            this.cond = cond;
            use(left, this);
            use(right, this);
        }

        /**
         * Creates a instance of different subclass served as different
         * date type according to the date type.
         *
         * @param kind  The ret date type.
         * @param left  The left LIROperand.
         * @param right the right LIROperand.
         * @param cond  The condition object.
         * @return According comparison instruction.
         */
        public Cmp(LIRKind kind, Value left, Value right,
                Condition cond,
                String name,
                BasicBlock insertAtEnd)
        {
            super(kind, Operator.Cmp, name, 2, insertAtEnd);
            this.cond = cond;
            use(left, this);
            use(right, this);
        }

        /**
         * An interface for InstructionVisitor invoking.
         *
         * @param visitor The instance of InstructionVisitor.
         */
        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitCompare(this);
        }

        /**
         * obtains the predicate condition.
         * @return
         */
        public Condition condition()
        {
            return this.cond;
        }

        /**
         * return the inverse predicate for current instruction's predicate.
         * @return
         */
        public Condition getInverseCondition()
        {
            return cond.negate();
        }

        /**
         * This just a convenience that swaps the operands and adjust predicate
         * according to retain the same comparison.
         */
        public void swapOperand()
        {
            Util.swap(operand(0), operand(1));
            if (!cond.isCommutative())
            {
                cond = cond.negate();
            }
        }

        public boolean isCommutative()
        {
            return cond.isCommutative();
        }
    }

    public static class CastInst extends Op1
    {
        public CastInst(LIRKind kind, Operator opcode,
                Value x, String name)
        {
            super(kind, opcode, x, name);
        }

        public CastInst(LIRKind kind, Operator opcode,
                Value x, String name,
                Instruction insertBefore)
        {
            super(kind, opcode, x, name, insertBefore);
        }

        public CastInst(LIRKind kind, Operator opcode,
                Value x, String name,
                BasicBlock insertAtEnd)
        {
            super(kind, opcode, x, name, insertAtEnd);
        }

        /**
         * An interface for InstructionVisitor invoking.
         *
         * @param visitor The instance of InstructionVisitor.
         */
        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitConvert(this);
        }
    }

    /**
     * TerminatorInst - Subclasses of this class are all able to terminate
     * a basic block.  Thus, these are all the flow control type of operations.
     *
     * @author Xlous.zeng
     * @version 0.1
     */
    public static abstract class TerminatorInst extends Instruction
    {
        TerminatorInst(LIRKind kind, Operator opcode, String instName,
                int numOperands, Instruction insertBefore)
        {
            super(kind, opcode, instName, numOperands, insertBefore);
        }

        TerminatorInst(LIRKind kind, Operator opcode, String instName,
                int numOperands, BasicBlock insertAtEnd)
        {
            super(kind, opcode, instName, numOperands, insertAtEnd);
        }

        /**
         * obtains the successor at specified index position.
         *
         * @param index
         * @return
         */
        public abstract BasicBlock suxAt(int index);

        /**
         * Obtains the number of successors.
         *
         * @return
         */
        public abstract int getNumOfSuccessors();

        /**
         * Updates basic block at specified index position.
         *
         * @param index
         * @param bb
         */
        public abstract void setSuxAt(int index, BasicBlock bb);
    }

    /**
     * An abstract representation of branch instruction.
     *
     * @author Xlous.zeng
     */
    public final static class BranchInst extends TerminatorInst
    {
        /**
         * Constructs a Branch instruction.
         * BranchInst(BasicBlock bb) - 'br B'
         *
         * @param ifTrue       the branch target.
         * @param insertBefore
         */
        public BranchInst(BasicBlock ifTrue, Instruction insertBefore)
        {
            super(LIRKind.Void, Operator.Br, "", 1, insertBefore);
            setOperand(0, ifTrue);
            use(ifTrue, this);
        }

        /**
         * Constructs a branch instruction.
         * <p>
         * BranchInst(BasicBlock bb) - 'br B'
         *
         * @param ifTrue the target of this branch.
         */
        public BranchInst(BasicBlock ifTrue)
        {
            this(ifTrue, (Instruction) null);
        }

        /**
         * BranchInst(BB* T, BB *F, Value *C, Inst *I) - 'br C, T, F', insert before I
         *
         * @param ifTrue
         * @param ifFalse
         * @param cond
         * @param insertBefore
         */
        public BranchInst(BasicBlock ifTrue, BasicBlock ifFalse, Value cond,
                Instruction insertBefore)
        {
            super(LIRKind.Void, Operator.Br, "", 3, insertBefore);
            setOperand(0, ifTrue);
            setOperand(1, ifFalse);
            setOperand(2, cond);

            // add use of this
            use(ifTrue, this);
            use(ifFalse, this);
            use(cond, this);
        }

        public BranchInst(BasicBlock ifTrue, BasicBlock ifFalse, Value cond,
                BasicBlock insertAtEnd)
        {
            super(LIRKind.Void, Operator.Br, "", 3, insertAtEnd);

            setOperand(0, ifTrue);
            setOperand(1, ifFalse);
            setOperand(2, cond);
            // add use of this
            use(ifTrue, this);
            use(ifFalse, this);
            use(cond, this);
        }

        /**
         * BranchInst(BB* B, BB *I) - 'br B'        insert at end
         *
         * @param ifTrue
         * @param insertAtEnd
         */
        public BranchInst(BasicBlock ifTrue, BasicBlock insertAtEnd)
        {
            super(LIRKind.Void, Operator.Br, "", 1, insertAtEnd);
            setOperand(0, ifTrue);
            // add use of this
            use(ifTrue, this);
        }

        public boolean isUnconditional()
        {
            return getNumOfOperands() == 1;
        }

        public boolean isConditional()
        {
            return getNumOfOperands() == 3;
        }

        public Value getCondition()
        {
            assert (isConditional()) : "can not get a condition of uncondition branch";
            return operand(2);
        }

        public void setCondition(Value cond)
        {
            assert (cond != null) : "can not update condition with null";
            assert (isConditional()) : "can not set condition of uncondition branch";
            setOperand(2, cond);
        }

        public BranchInst clone()
        {
            return null;
        }

        @Override
        public void accept(InstructionVisitor visitor)
        {

        }

        /**
         * obtains the successors at specified position.
         *
         * @param index
         * @return
         */
        @Override
        public BasicBlock suxAt(int index)
        {
            assert (index >= 0 && index < getNumOfSuccessors());
            return (BasicBlock) operand(index);
        }

        /**
         * obtains the number of successors of this branch instruction.
         *
         * @return
         */
        @Override
        public int getNumOfSuccessors()
        {
            return isConditional() ? 2 : 1;
        }

        @Override
        public void setSuxAt(int index, BasicBlock bb)
        {
            assert (index >= 0 && index < getNumOfSuccessors() && bb != null);
            setOperand(index, bb);
        }

        /**
         * Swaps the successor of the branch instruction.
         */
        public void swapSuccessor()
        {
            assert isConditional() : "can not swap successor of uncondition branch";
            Util.swap(operand(0), operand(1));
        }
    }

    /**
     * This {@code ReturnInst} class definition.
     * ReturnStmt a value (possibly void), from a function.
     * Execution does not continue in this function any longer.
     *
     * @author Xlous.zeng
     */
    public static class ReturnInst extends TerminatorInst
    {
        private Log log;

        public ReturnInst(Log log)
        {
            this(null, "", (Instruction) null, log);
        }

        public ReturnInst(String name, Log log)
        {
            this(null, name, (Instruction) null, log);
        }

        /**
         * Constructs a new return instruction with return inst.
         *
         * @param retValue The return inst produce for this instruction, return
         *                 void if ret is {@code null}.
         */
        public ReturnInst(Value retValue, String name, Instruction insertBefore,
                Log log)
        {
            super(retValue == null ? LIRKind.Void : retValue.kind, Operator.Ret,
                    name, retValue != null ? 1 : 0, insertBefore);
            this.log = log;
            if (retValue != null)
                use(retValue, this);
        }

        public ReturnInst(Value retValue, String name, BasicBlock insertAtEnd,
                Log log)
        {
            super(retValue != null ? retValue.kind : LIRKind.Void, Operator.Ret,
                    name, retValue != null ? 1 : 0, insertAtEnd);
            this.log = log;
            if (retValue != null)
                use(retValue, this);
        }

        /**
         * Gets the instruction that produces the ret for the return.
         *
         * @return the instruction producing the ret
         */
        public Value getReturnValue()
        {
            return getNumOfOperands() != 0 ? operand(0) : null;
        }

        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitReturn(this);
        }

        @Override
        public BasicBlock suxAt(int index)
        {
            log.unreachable("ReturnInst has no successors!");
            return null;
        }

        @Override
        public int getNumOfSuccessors()
        {
            return 0;
        }

        @Override
        public void setSuxAt(int index, BasicBlock bb)
        {
            log.unreachable("ReturnInst has no successors!");
        }
    }

    /**
     * Function invocation instruction.
     *
     * @author Xlous.zeng
     */
    public static class InvokeInst extends TerminatorInst
    {
        public Value ret;

        /**
         * Constructs a new method calling instruction.
         *
         * @param result The kind of return ret.
         * @param args   The input arguments.
         * @param target The called method.
         */
        public InvokeInst(LIRKind result, Value[] args, Function target,
                BasicBlock ifNormal, String name)
        {
            this(result, args, target, ifNormal, name, (Instruction) null);
        }

        /**
         * Constructs a new method calling instruction.
         *
         * @param result The kind of return ret.
         * @param args   The input arguments.
         * @param target The called method.
         */
        public InvokeInst(LIRKind result, Value[] args, Function target,
                BasicBlock ifNormal, String name, Instruction insertBefore)
        {
            super(result, Operator.Invoke, name, args.length + 2, insertBefore);
            init(target, ifNormal, args);
        }

        /**
         * Constructs a new method calling instruction.
         *
         * @param result The kind of return ret.
         * @param args   The input arguments.
         * @param target The called method.
         */
        public InvokeInst(LIRKind result, Value[] args, Function target,
                BasicBlock ifNormal, String name, BasicBlock insertAtEnd)
        {
            super(result, Operator.Invoke, name, args.length + 1, insertAtEnd);
            init(target, ifNormal, args);
        }

        private void init(Function function, BasicBlock ifNormal, Value[] args)
        {
            assert (getNumOfOperands()
                    == 2 + args.length) : "NumOperands not set up?";
            setOperand(0, function);
            setOperand(1, ifNormal);
            System.arraycopy(args, 0, reservedOperands, 2, args.length);
            for (Value arg : args)
            {
                use(arg, this);
            }
        }

        @Override public void accept(InstructionVisitor visitor)
        {
            visitor.visitInvoke(this);
        }

        @Override public String toString()
        {
            return name();
        }

        public int getNumsOfArgs()
        {
            return reservedOperands.length - 2;
        }

        public void setArgument(int index, Value val)
        {
            assert index >= 0 && index < getNumsOfArgs();
            setOperand(index + 1 + getNumOfSuccessors(), val);
        }

        public Value argumentAt(int i)
        {
            assert i >= 0 && i < getNumsOfArgs();
            return operand(i + 1 + getNumOfSuccessors());
        }

        public Function getCalledFunction()
        {
            return (Function) operand(0);
        }

        public Value getCalledValue()
        {
            return operand(0);
        }

        @Override public BasicBlock suxAt(int index)
        {
            assert (index >= 0 && index < getNumOfSuccessors()) :
                    "Successor " + index + "out of range";
            return (BasicBlock) operand(1 + index);
        }

        @Override public int getNumOfSuccessors()
        {
            return 1;
        }

        @Override public void setSuxAt(int index, BasicBlock bb)
        {
            assert (index >= 0 && index
                    < getNumOfSuccessors()) : "out of range index to reservedOperands array.";
            assert bb != null : "can not use null to update successor";

            setOperand(index + 1, bb);
        }
    }

    public static class SwitchInst extends TerminatorInst
    {
        private int reservedSpaces;
        private int currIdx = 0;
        private int lowKey, highKey;
        private int offset = 2;

        /**
         * Constructs a new SwitchInst instruction with specified inst type.
         *
         * @param condV     the value of selector.
         * @param defaultBB The default jump block when no other case match.
         * @param numCases  The numbers of case value.
         */
        public SwitchInst(Value condV, BasicBlock defaultBB, int numCases,
                String name)
        {
            this(condV, defaultBB, numCases, name, (Instruction) null);
        }

        /**
         * Constructs a new SwitchInst instruction with specified inst type.
         *
         * @param condV        the value of selector.
         * @param defaultBB    The default jump block when no other case match.
         * @param numCases     The numbers of case value.
         * @param insertBefore
         */
        public SwitchInst(Value condV, BasicBlock defaultBB, int numCases,
                String name, Instruction insertBefore)
        {
            super(LIRKind.Void, Operator.Switch, name, 2 * numCases + 2,
                    insertBefore);
            init(condV, defaultBB, 2 * numCases + 2);
            //operands = new Pair[1 + numCases];
            //operands[currIdx++] = new Pair<>(condV, defaultBB);
        }

        /**
         * Constructs a new SwitchInst instruction with specified inst type.
         *
         * @param condV     the value of selector.
         * @param defaultBB The default jump block when no other case match.
         * @param numCases  The numbers of case value.
         */
        public SwitchInst(Value condV, BasicBlock defaultBB, int numCases,
                String name, BasicBlock insertAtEnd)
        {
            super(LIRKind.Void, Operator.Switch, name, numCases + 2,
                    insertAtEnd);
            init(condV, defaultBB, 2 * numCases + 2);
        }

        /**
         * Initialize some arguments, like add switch value and default into
         * Operand list.
         */
        private void init(Value cond, BasicBlock defaultBB, int reservedSpaces)
        {
            this.reservedSpaces = reservedSpaces;
            numOperands = 2;
            setOperand(0, cond);
            setOperand(1, defaultBB);
        }

        /**
         * An interface for InstructionVisitor invoking.
         *
         * @param visitor The instance of InstructionVisitor.
         */
        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitSwitch(this);
        }

        private void growOperands()
        {
            int newLen = 2 + reservedSpaces << 1;
            Value[] newArray = new Value[newLen];
            System.arraycopy(reservedOperands, 0, newArray, 0, 2+reservedSpaces);
            reservedOperands = newArray;
        }

        public void addCase(Constant caseVal, BasicBlock targetBB)
        {
            int OpNo = numOperands;
            if (OpNo+2 > reservedSpaces)
                growOperands();  // Get more space!
            // Initialize some new operands.
            assert(OpNo+1 < reservedSpaces) : "Growing didn't work!";
            numOperands = OpNo+2;
            setOperand(OpNo+offset, caseVal);
            setOperand(OpNo+1+offset, targetBB);
        }
        public void removeCase(int idx)
        {
            assert(idx != 0) : "Cannot remove the default case!";
            assert(idx*2 < getNumOfOperands()): "Successor index out of range!!!";

            int NumOps = getNumOfOperands();

            // Overwrite this case with the end of the list.
            if ((idx + 1) * 2 != NumOps)
            {
                setOperand(idx * 2, operand(NumOps - 2));
                setOperand(idx * 2 + 1, operand(NumOps - 1));
            }

            // Nuke the last value.
            setOperand(NumOps-2, null);
            setOperand(NumOps-1, null);
            numOperands = NumOps-2;
        }
        /**
         * Gets the default basic block where default case clause resides.
         *
         * @return The default basic block.
         */
        public BasicBlock getDefaultBlock()
        {
            return (BasicBlock) operand(1);
        }

        // Accessor Methods for SwitchStmt stmt
        Value getCondition()
        {
            return operand(0);
        }

        void setCondition(Value val)
        {
            setOperand(0, val);
        }

        public int getNumOfCases()
        {
            return getNumOfOperands() >> 1;
        }

        /**
         * Search all of the case values for the specified constant.
         * IfStmt it is explicitly handled, return the case number of it, otherwise
         * return 0 to indicate that it is handled by the default handler.
         *
         * @param index
         * @return
         */
        public Constant getCaseValues(int index)
        {
            assert index >= 0
                    && index < getNumOfCases() : "Illegal case value to get";
            return getSuccessorValue(index);
        }

        public int findCaseValue(Constant val)
        {
            for (int i = 1; i < getNumOfCases(); i++)
            {
                if (getCaseValues(i) == val)
                    return i;
            }
            return 0;
        }

        public Constant findCaseDest(BasicBlock bb)
        {
            if (bb == getDefaultBlock()) return null;

            Constant res = null;
            for (int i = 0; i < getNumOfCases(); i++)
            {
                if (getSuccessor(i) == bb)
                {
                    if (res != null) return null;
                    else res = getCaseValues(i);

                }
            }
            return res;
        }

        public Constant getSuccessorValue(int index)
        {
            assert index >= 0 && index < getNumOfSuccessors()
                    : "Successor value index out of range for switch";
            return (Constant)operand(2*index + offset);
        }

        public BasicBlock getSuccessor(int index)
        {
            assert index >= 0 && index < getNumOfSuccessors()
                    : "Successor index out of range for switch";
            return (BasicBlock) operand(2*index + 1 + offset);

        }
        public void setSuccessor(int index, BasicBlock newBB)
        {
            assert index >= 0 && index < getNumOfSuccessors()
                    : "Successor index out of range for switch";
            setOperand(index * 2 + 1 + offset, newBB);
        }
        // setSuccessorValue - Updates the value associated with the specified
        // successor.
        public void setSuccessorValue(int idx, Constant SuccessorValue)
        {
            assert(idx < getNumOfSuccessors()) : "Successor # out of range!";
            setOperand(idx*2 + offset, SuccessorValue);
        }

        public int getLowKey()
        {
            return lowKey;
        }

        public int getHighKey()
        {
            return highKey;
        }

        public void setLowKey(int lowKey)
        {
            this.lowKey = lowKey;
        }

        public void setHighKey(int highKey)
        {
            this.highKey = highKey;
        }

        /**
        public SwitchInst clone()
        {
            Pair<Value, BasicBlock>[] copy = Arrays
                    .copyOf(operands, operands.getArraySize);
            SwitchInst inst = new SwitchInst(null, null, 0, name.toString());
            inst.currIdx = this.currIdx;
            inst.highKey = this.highKey;
            inst.lowKey = this.lowKey;
            inst.operands = copy;
            return inst;
        }*/

        /**
         * obtains the successor at specified index position.
         *
         * @param index
         * @return
         */
        @Override
        public BasicBlock suxAt(int index)
        {
            return getSuccessor(index);
        }

        /**
         * Obtains the number of successors.
         *
         * @return
         */
        @Override
        public int getNumOfSuccessors()
        {
            return getNumOfOperands() >> 1;
        }

        /**
         * Updates basic block at specified index position.
         *
         * @param index
         * @param bb
         */
        @Override
        public void setSuxAt(int index, BasicBlock bb)
        {
            setSuccessor(index, bb);
        }
    }

    /**
     * The {@code PhiNode} instruction represents the merging of data flow in the
     * instruction graph. It refers to a join block and a variable.
     *
     * @author Xlous.zeng
     */
    public static class PhiNode extends Instruction
    {
        public PhiNode(LIRKind kind, int numReservedValues,
                String name)
        {
            this(kind, numReservedValues, name, null);
        }

        public PhiNode(LIRKind kind, int numReservedValues,
                String name, Instruction insertBefore)
        {
            super(kind, Operator.Phi, name, numReservedValues << 1, insertBefore);
            numOperands = 0;
        }

        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitPhi(this);
        }

        /**
         * Appends a pair that consists of both value and block into argument list.
         *
         * @param value The instruction that phi parameter to be inserted
         * @param block The according block of corresponding phi parameter.
         */
        public void addIncoming(Value value, BasicBlock block)
        {
            assert value != null : "Phi node got a null value";
            assert block != null : "Phi node got a null basic block";
            assert value.kind == kind : "All of operands of Phi must be same type.";
            if (numOperands*2 == reservedOperands.length)
                growOperands();
            numOperands += 1;
            setIncomingValue(numOperands - 1, value);
            setIncomingBlock(numOperands - 1, block);
        }

        private void growOperands()
        {
            int len = reservedOperands.length << 1;
            Value[] newArray = new Value[len];
            System.arraycopy(reservedOperands, 0, newArray, 0, numOperands);
            reservedOperands = newArray;
        }

        /**
         * Gets the inputed parameter at given position.
         *
         * @param index The position where input parameter will be obtained.
         * @return The input parameter at specified position.
         */
        public Value getIncomingValue(int index)
        {
            assert index >= 0 && index
                    < numOperands : "The index is beyond out the num of list";
            return operand(index << 1);
        }

        public void setIncomingValue(int index, Value val)
        {
            assert index >= 0 && index
                    < numOperands : "The index is beyond out the num of list";
            setOperand(index << 1, val);
        }

        public Value getIncomingValueForBlock(BasicBlock bb)
        {
           int idx = getBasicBlockIndex(bb);
            assert idx >= 0 : "Invalid basic block argument";
            return getIncomingValue(idx);
        }

        /**
         * Gets the input block at given position.
         *
         * @param index The position where input block will be obtained.
         * @return The input block at specified position.
         */
        public BasicBlock getIncomingBlock(int index)
        {
            assert index >= 0 && index
                    < numOperands : "The index is beyond out the num of list";
            return (BasicBlock) operand(index << 1 + 1);
        }

        public void setIncomingBlock(int index, BasicBlock bb)
        {
            assert index >= 0 && index
                    < numOperands : "The index is beyond out the num of list";
            setOperand((index << 1) + 1, bb);
        }

        public Value removeIncomingValue(int index)
        {
            assert index >= 0 && index
                    < numOperands : "The index is beyond out the num of list";

            Value old = operand(index << 1);
            for (int i = index; i < numOperands - 1; i++)
            {
                setOperand(i, operand(i + 2));
            }
            numOperands--;
            return old;
        }

        public Value removeIncomingValue(BasicBlock bb)
        {
            int index = getBasicBlockIndex(bb);
            assert index >= 0 : "invalid basic block argument to remove";
            return removeIncomingValue(index);
        }

        @Override
        public String toString()
        {
            return instName;
        }

        public int getBasicBlockIndex(BasicBlock basicBlock)
        {
            assert (basicBlock != null)
                    : "PhiNode.getBasicBlockIndex(<null>) is invalid";
            for (int i = 0; i < numOperands; i++)
            {
                if (operand(i + 1) == basicBlock)
                    return i;
            }
            return -1;
        }

        /**
         * Obtains the numbers of incoming value of phi node.
         *
         * @return
         */
        public int getNumberIncomingValues()
        {
            return getNumOfOperands();
        }
        /**
         * Gets the name of this phi node.
         *
         * @return
         */
        public String name()
        {
            return instName;
        }
    }

    /**
     * This class was served functionally as allocating memory on the stack frame.
     * <b>Note that </b>all of heap allocation is accomplished by invoking the
     * C language library function as yet.
     */
    public static class AllocaInst extends Op1
    {
        /**
         * Creates a new {@linkplain AllocaInst} Module that allocates memory
         * for specified {@LIRKind kind} and the numbers of to be allocated
         * element.
         *
         * @param kind The data kind of allocated data which is instance of
         * {@linkplain LIRKind}.
         * @param arraySize  The number of elements if allocating is used for
         *                   array.
         * @param name The name of this instruction for debugging.
         */
        public AllocaInst(LIRKind kind,
                Value arraySize,
                String name,
                Instruction insertBefore)
        {
            super(kind, Operator.Alloca,
                    getAISize(arraySize),
                    name, insertBefore);
        }

        public AllocaInst(LIRKind kind,
                String name,
                Instruction insertBefore)
        {
            super(kind, Operator.Alloca,
                    getAISize(null), name,
                    insertBefore);
        }

        public AllocaInst(LIRKind kind,
                Value arraySize,
                String name,
                BasicBlock insertAtEnd)
        {
            super(kind, Operator.Alloca,
                    getAISize(arraySize),
                    name, insertAtEnd);
        }

        public AllocaInst(LIRKind kind,
                String name,
                BasicBlock insertAtEnd)
        {
            super(kind, Operator.Alloca,
                    getAISize(null),
                    name, insertAtEnd);
        }

        private static Value getAISize(Value arraySize)
        {
            Value res = arraySize == null ? Constant.forInt(1) : null;
            if (arraySize != null)
            {
                assert !(arraySize instanceof BasicBlock)
                        : "Basic block be passed into allocation getTypeSize parameter!";
                assert arraySize.isConstant() :
                        "Allocation array getTypeSize is not an integral.";
                res = arraySize;
            }
            return res;
        }

        /**
         * Checks if this is a allocation of array not not.
         * ReturnStmt true if the array getTypeSize is not 1.
         * @return
         */
        public boolean isArrayAllocation()
        {
            return operand(0).asConstant().value.asInt() != 1;
        }

        /**
         * Gets the instruction that produced the num argument.
         */
        public Value getArraySize()
        {
            return operand(0);
        }

        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitAlloca(this);
        }

        /**
         * Gets the name of this alloated variable.
         *
         * @return
         */
        public String name()
        {
            return instName;
        }

        /**
         * Determine whether this alloca instruction is promoted into
         * register or not?
         *
         * @return ReturnInst true if it is promotable.
         */
        public boolean isAllocaPromotable()
        {
            return !isArrayAllocation();
        }
    }

    /**
     * An instruction for writing data into memory.
     */
    public static class StoreInst extends Instruction
    {
        /**
         * Constructs a new store instruction.
         *
         * @param value The inst to being writed into memory.
         * @param ptr  The targetAbstractLayer memory address where inst stores.
         */
        public StoreInst(Value value, Value ptr,
                String name,
                Instruction insertBefore)
        {
            super(LIRKind.Illegal, Operator.Store, name, 2, insertBefore);
            init(value, ptr);
        }

        /**
         * Constructs a new store instruction.
         *
         * @param value The inst to being writed into memory.
         * @param ptr  The targetAbstractLayer memory address where inst stores.
         */
        public StoreInst(Value value, Value ptr,
                String name)
        {
            super(LIRKind.Illegal, Operator.Store, name, 2, (Instruction)null);
            init(value, ptr);
        }

        public StoreInst(Value value, Value ptr,
                String name,
                BasicBlock insertAtEnd)
        {
            super(LIRKind.Illegal, Operator.Store, name, 2, insertAtEnd);
            init(value, ptr);
        }

        private void init(Value value, Value ptr)
        {
            assert value != null: "The value written into memory must be not null.";
            assert ptr != null : "The memory address of StoreInst must be not null.";
            assert ptr instanceof AllocaInst
                    : "the destination of StoreInst must be AllocaInst!";
            setOperand(0, value);
            setOperand(1, ptr);
            use(value, this);
            use(ptr, this);
        }

        public Value getValueOperand()
        {
            return operand(0);
        }

        public Value getPointerOperand()
        {
            return operand(1);
        }

        public int getPointerOperandIndex()
        {
            return 1;
        }

        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitStoreInst(this);
        }
    }

    /**
     * An instruction for reading data from memory.
     */
    public static class LoadInst extends Op1
    {
        public LoadInst(LIRKind kind, Value from,
                String name, Instruction insertBefore)
        {
            super(kind, Operator.Load, from, name, insertBefore);
            use(from, this);
        }

        public LoadInst(LIRKind kind, Value from,
                String name)
        {
            super(kind, Operator.Load, from, name, (Instruction)null);
            use(from, this);
        }

        public LoadInst(LIRKind kind, Value from,
                String name, BasicBlock insertAtEnd)
        {
            super(kind, Operator.Load, from, name, insertAtEnd);
            use(from, this);
        }

        public Value getPointerOperand()
        {
            return operand(1);
        }

        public int getPointerOperandIndex()
        {
            return 0;
        }

        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitLoadInst(this);
        }
    }
}
