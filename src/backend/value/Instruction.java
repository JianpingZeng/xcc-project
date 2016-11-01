package backend.value;

import backend.hir.BasicBlock;
import backend.hir.InstructionVisitor;
import backend.hir.Operator;
import backend.lir.ci.LIRKind;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.ConstantInt.ConstantUInt;
import tools.Util;

import java.util.ArrayList;

import static backend.type.Type.Int32Ty;

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
    protected Operator opcode;

    public Instruction(Type ty,
            Operator op,
            Instruction insertBefore)
    {
        super(ty, ValueKind.InstructionVal + op.index);
        opcode = op;
        setParent(null);
        if (insertBefore != null)
        {
            assert (insertBefore.getParent()
                    != null) : "Instruction to insert before is not in a basic block";
            insertBefore.getParent().insertBefore(this, insertBefore);
        }
    }

    public Instruction(Type ty,
            Operator op,
            BasicBlock insertAtEnd)
    {
        super(ty, ValueKind.InstructionVal + op.index);
        opcode = op;
        setParent(null);

        // append this instruction into the basic block
        assert (insertAtEnd
                != null) : "Basic block to append to may not be NULL!";
        insertAtEnd.appendInst(this);
    }

    public Instruction(Type ty,
            Operator op)
    {
        this(ty, op, (Instruction) null);
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
         * @param ty   The inst ty of ret.
         * @param opcode The operator code for this instruction.
         * @param x      The sole LIROperand.
         */
        public Op1(Type ty, Operator opcode, Value x,
                String name, Instruction insertBefore)
        {
            super(ty, opcode, insertBefore);
            reserve(1);
            setOperand(0, x);
        }

        /**
         * Constructs unary operation.
         *
         * @param ty   The inst ty of ret.
         * @param opcode The operator code for this instruction.
         * @param x      The sole LIROperand.
         */
        public Op1(Type ty, Operator opcode, Value x,
                String name)
        {
            this(ty, opcode, x, name, (Instruction)null);
        }

        /**
         *
         * @param ty
         * @param opcode
         * @param x
         * @param name
         * @param insertAtEnd
         */
        public  Op1(Type ty, Operator opcode, Value x,
                String name, BasicBlock insertAtEnd)
        {
            super(ty, opcode, insertAtEnd);
            setOperand(0, x);
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
            return getType() == op.getType() && opcode.equals(op.opcode)
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
        public Op2(Type ty, Operator opcode,
                Value x, Value y, String name)
        {
            this(ty, opcode, x, y, name, (Instruction)null);
        }

        public Op2(Type ty, Operator opcode,
                Value x, Value y, String name,
                Instruction insertBefore)
        {
            super(ty, opcode,insertBefore);
            reserve(2);
            assert x.getType() == y.getType()
                    : "Can not create binary operation with two operands of differing frontend.type.";
            init(x, y);
        }

        public Op2(Type ty, Operator opcode,
                Value x, Value y, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, opcode, insertAtEnd);
            assert x.getType() == y.getType()
                    : "Can not create binary operation with two operands of differing frontend.type.";
            init(x, y);
        }

        private void init(Value x, Value y)
        {
            setOperand(0, x);
            setOperand(1, y);
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
            return getType() == op.getType() && opcode.equals(op.opcode)
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

    public static class CastInst extends Op1
    {
        public CastInst(Type ty,
                Operator opcode,
                Value x, String name)
        {
            super(ty, opcode, x, name);
            initialize(x);
        }

        public CastInst(Type ty, Operator opcode,
                Value x, String name,
                Instruction insertBefore)
        {
            super(ty, opcode, x, name, insertBefore);
            initialize(x);
        }

        public CastInst(Type ty, Operator opcode,
                Value x, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, opcode, x, name, insertAtEnd);
            initialize(x);
        }

        private void initialize(Value x)
        {
            reserve(1);
            setOperand(0, x);
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
     * a basic block.  Thus, these are all the flow control frontend.type of operations.
     *
     * @author Xlous.zeng
     * @version 0.1
     */
    public static abstract class TerminatorInst extends Instruction
    {
        TerminatorInst(Operator opcode, String instName,
                Instruction insertBefore)
        {
            super(Type.VoidTy, opcode, insertBefore);
        }

        TerminatorInst(Type ty, Operator opcode, String instName,
                Instruction insertBefore)
        {
            super(ty, opcode, insertBefore);
        }


        TerminatorInst(Operator opcode, String instName,
                BasicBlock insertAtEnd)
        {
            super(Type.VoidTy, opcode, insertAtEnd);
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
         * Constructs a unconditional Branch instruction.
         * BranchInst(BasicBlock bb) - 'br B'
         *
         * @param ifTrue       the branch target.
         * @param insertBefore
         */
        public BranchInst(BasicBlock ifTrue, Instruction insertBefore)
        {
            super(Operator.Br, "", insertBefore);
            reserve(1);
            setOperand(0, ifTrue);
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
            super(Operator.Br, "", insertBefore);
            reserve(3);
            setOperand(0, ifTrue);
            setOperand(1, ifFalse);
            setOperand(2, cond);
        }

        public BranchInst(BasicBlock ifTrue, BasicBlock ifFalse, Value cond,
                BasicBlock insertAtEnd)
        {
            super(Operator.Br, "", insertAtEnd);

            setOperand(0, ifTrue);
            setOperand(1, ifFalse);
            setOperand(2, cond);
        }

        /**
         * BranchInst(BB* B, BB *I) - 'br B'        insert at end
         *
         * @param ifTrue
         * @param insertAtEnd
         */
        public BranchInst(BasicBlock ifTrue, BasicBlock insertAtEnd)
        {
            super(Operator.Br, "", insertAtEnd);
            setOperand(0, ifTrue);
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
        public ReturnInst()
        {
            this(null, "", (Instruction) null);
        }

        /**
         * Constructs a new return instruction with return inst.
         *
         * @param retValue The return inst produce for this instruction, return
         *                 void if ret is {@code null}.
         */
        public ReturnInst(Value retValue, String name, Instruction insertBefore)
        {
            super(Operator.Ret, name, insertBefore);
            if (retValue != null)
            {
                reserve(1);
                setOperand(0, retValue);
            }
        }

        public ReturnInst(Value retValue, String name, BasicBlock insertAtEnd)
        {
            super(Operator.Ret, name, insertAtEnd);
            if (retValue != null)
            {
                reserve(1);
                setOperand(0, retValue);
            }
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
            assert true:"ReturnInst has no successors!";
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
            assert true:("ReturnInst has no successors!");
        }
    }

    /**
     * Function invocation instruction.
     *
     * @author Xlous.zeng
     */
    public static class InvokeInst extends TerminatorInst
    {
        /**
         * Constructs a new method calling instruction.
         *
         * @param args   The input arguments.
         * @param target The called method.
         */
        public InvokeInst(Value[] args, Function target,
                BasicBlock ifNormal, String name)
        {
            this(args, target, ifNormal, name, (Instruction) null);
        }

        /**
         * Constructs a new method calling instruction.
         *
         * @param args   The input arguments.
         * @param target The called method.
         */
        public InvokeInst(Value[] args, Function target,
                BasicBlock ifNormal, String name, Instruction insertBefore)
        {
            super(target.getResultType(), Operator.Invoke, name, insertBefore);
            init(target, ifNormal, args);
        }


        private void init(Function function, BasicBlock ifNormal, Value[] args)
        {
            reserve(2 + args.length);
            assert (getNumOfOperands()
                    == 2 + args.length) : "NumOperands not set up?";
            setOperand(0, function);
            setOperand(1, ifNormal);
            int idx = 2;
            for (Value arg : args)
            {
                setOperand(idx++, arg);
            }
        }

        @Override
        public void accept(InstructionVisitor visitor)
        {
            visitor.visitInvoke(this);
        }

        @Override
        public String toString()
        {
            return name();
        }

        public int getNumsOfArgs()
        {
            return getNumOfOperands() - 2;
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

        @Override
        public BasicBlock suxAt(int index)
        {
            assert (index >= 0 && index < getNumOfSuccessors()) :
                    "Successor " + index + "out of range";
            return (BasicBlock) operand(1 + index);
        }

        @Override
        public int getNumOfSuccessors()
        {
            return 1;
        }

        @Override
        public void setSuxAt(int index, BasicBlock bb)
        {
            assert (index >= 0 && index
                    < getNumOfSuccessors()) : "out of range index to reservedOperands array.";
            assert bb != null : "can not use null to update successor";

            setOperand(index + 1, bb);
        }
    }

    public static class SwitchInst extends TerminatorInst
    {
        private int lowKey, highKey;
        private final int offset = 2;

        /**
         * Constructs a new SwitchInst instruction with specified inst frontend.type.
         * <p>
         * Operand[0]    = Value to switch on
         * Operand[1]    = Default basic block destination
         * Operand[2n  ] = Value to match
         * Operand[2n+1] = BasicBlock to go to on match
         * </p>
         *
         * @param condV     the value of selector.
         * @param defaultBB The default jump block when no other case match.
         * @param numCases  The numbers of case value.
         */
        public SwitchInst(Value condV, BasicBlock defaultBB, int numCases,
                String name)
        {
            this(condV, defaultBB, name, null);
        }

        /**
         * Constructs a new SwitchInst instruction with specified inst frontend.type.
         *
         * @param condV        the value of selector.
         * @param defaultBB    The default jump block when no other case match.
         * @param insertBefore
         */
        public SwitchInst(Value condV, BasicBlock defaultBB,
                String name, Instruction insertBefore)
        {
            super(Operator.Switch, name,insertBefore);
            init(condV, defaultBB);
        }


        /**
         * Initialize some arguments, like add switch value and default into
         * Operand list.
         */
        private void init(Value cond, BasicBlock defaultBB)
        {
            // the 2 indicates what number of default basic block and default value.
            reserve(offset+8);
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

        public void addCase(Constant caseVal, BasicBlock targetBB)
        {
            int opNo = getNumOfCases();
            setOperand(opNo, caseVal);
            setOperand(opNo+1, targetBB);
        }

        public void removeCase(int idx)
        {
            assert(idx != 0) : "Cannot remove the default case!";
            assert(idx*2 < getNumOfOperands()): "Successor index out of range!!!";

            // unlink the last value.
            operandList.remove(idx);
            operandList.remove(idx + 1);
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
        public Value getCondition()
        {
            return operand(0);
        }

        public void setCondition(Value val)
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
            assert index >= 0 && index < getNumOfCases()
                    : "Illegal case value to get";
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
            assert(idx>=0 && idx < getNumOfSuccessors())
                    : "Successor # out of range!";
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

        public SwitchInst clone()
        {
            SwitchInst inst = new SwitchInst(getCondition(), getDefaultBlock(), getNumOfCases(),name);
            inst.operandList = new ArrayList<>(inst.operandList);
            return inst;
        }

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
        public PhiNode(Type ty, int numReservedValues,
                String name)
        {
            this(ty, numReservedValues, name, null);
        }

        public PhiNode(Type ty, int numReservedValues,
                String name, Instruction insertBefore)
        {
            super(ty, Operator.Phi, insertBefore);
            reserve(numReservedValues << 1);
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
            assert value.getType() == getType() : "All of operands of Phi must be same frontend.type.";

            setIncomingValue(getNumberIncomingValues() - 1, value);
            setIncomingBlock(getNumberIncomingValues() - 1, block);
        }

        /**
         * Gets the inputed parameter at given position.
         *
         * @param index The position where input parameter will be obtained.
         * @return The input parameter at specified position.
         */
        public Value getIncomingValue(int index)
        {
            assert index >= 0 && index < getNumberIncomingValues()
                    : "The index is beyond out the num of list";
            return operand(index << 1);
        }

        public void setIncomingValue(int index, Value val)
        {
            assert index >= 0 && index < getNumberIncomingValues()
                    : "The index is beyond out the num of list";
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

        public Value removeIncomingValue(int index, boolean deletePhiIfEmpty)
        {
            assert index >= 0 && index
                    < numOperands : "The index is beyond out the num of list";

            Value old = operand(index << 1);
            operandList.remove(index);
            operandList.remove(index+1);

            // delete this phi node if it has zero entities.
            if (getNumOfOperands()==0 && deletePhiIfEmpty)
            {
                replaceAllUsesWith(Constant.getNullValue(getType()));
                eraseFromBasicBlock();
            }
            return old;
        }

        public Value removeIncomingValue(BasicBlock bb)
        {
            int index = getBasicBlockIndex(bb);
            assert index >= 0 : "invalid basic block argument to remove";
            return removeIncomingValue(index, true);
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
            for (int i = 0; i < getNumberIncomingValues(); i++)
            {
                if (getIncomingBlock(i) == basicBlock)
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
            return getNumOfOperands()>>1;
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
     * <b>Note that </b>all of backend.heap allocation is accomplished by invoking the
     * C language library function as yet.
     */
    public static class AllocaInst extends Instruction
    {
        /**
         * Creates a new {@linkplain AllocaInst} Module that allocates memory
         * for specified {@Type ty} and the numbers of to be allocated
         * element.
         *
         * @param ty The data ty of allocated data which is instance of
         * {@linkplain LIRKind}.
         * @param arraySize  The number of elements if allocating is used for
         *                   array.
         * @param name The name of this instruction for debugging.
         */
        public AllocaInst(Type ty,
                Value arraySize,
                String name,
                Instruction insertBefore)
        {
            super(ty, Operator.Alloca, insertBefore);

            // default to 1.
            if (arraySize == null)
                arraySize = ConstantUInt.get(Int32Ty, 1);

            reserve(1);
            assert arraySize.getType() == Type.Int32Ty
                    :"Alloca array size != UnsignedIntTy";
            setOperand(0, arraySize);
        }

        public AllocaInst(Type ty,
                Value arraySize,
                String name)
        {
            this(ty, arraySize, name,null);
        }

        public AllocaInst(Type ty,
                String name,
                Instruction insertBefore)
        {
            this(ty, null, name, insertBefore);
        }

        /**
         * Checks if this is a allocation of array not not.
         * ReturnStmt true if the array getTypeSize is not 1.
         * @return
         */
        public boolean isArrayAllocation()
        {
            return operand(0) != ConstantUInt.get(Int32Ty, 1);
        }

        public Type getAllocatedType()
        {
            return getType().getElemType();
        }

        /**
         * Gets the instruction that produced the num argument.
         */
        public Value getArraySize()
        {
            return operand(0);
        }

        public backend.type.PointerType getType()
        {
            return (PointerType)super.getType();
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
            super(Type.VoidTy, Operator.Store, insertBefore);
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
            super(Type.VoidTy, Operator.Store, (Instruction)null);
            init(value, ptr);
        }

        public StoreInst(Value value, Value ptr,
                String name,
                BasicBlock insertAtEnd)
        {
            super(Type.VoidTy, Operator.Store, insertAtEnd);
            init(value, ptr);
        }

        private void init(Value value, Value ptr)
        {
            assert value != null: "The value written into memory must be not null.";
            assert ptr != null : "The memory address of StoreInst must be not null.";
            assert ptr.getType().isPointerType()
                    : "the destination of StoreInst must be AllocaInst!";
            reserve(2);
            setOperand(0, value);
            setOperand(1, ptr);
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
        public LoadInst(Type ty, Value from,
                String name, Instruction insertBefore)
        {
            super(ty, Operator.Load, from, name, insertBefore);
            assertOK();
        }

        public LoadInst(Type ty, Value from,
                String name)
        {
            super(ty, Operator.Load, from, name, (Instruction)null);
            assertOK();
        }

        public LoadInst(Type ty, Value from,
                String name, BasicBlock insertAtEnd)
        {
            super(ty, Operator.Load, from, name, insertAtEnd);
            assertOK();
        }

        private void assertOK()
        {
            assert (operand(0).getType().isPointerType())
                    :"Ptr must have pointer type.";
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
