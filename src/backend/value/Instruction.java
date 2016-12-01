package backend.value;

import backend.hir.BasicBlock;
import backend.hir.InstVisitor;
import backend.hir.Operator;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import tools.Util;

import java.util.ArrayList;

import static backend.hir.Operator.*;
import static backend.type.Type.Int32Ty;
import static backend.value.Instruction.CmpInst.Predicate.*;

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
    private Operator opcode;

    public Instruction(Type ty,
            Operator op,
            Instruction insertBefore)
    {
        super(ty, ValueKind.InstructionVal + op.index);
        setOpcode(op);
        setParent(null);
        if (insertBefore != null)
        {
            assert (insertBefore.getParent()
                    != null) : "Instruction to insert before is not in a basic block";
            int idx = insertBefore.getParent().indexOf(insertBefore);
            insertBefore.getParent().insertBefore(this, idx);
        }
    }

    public Instruction(Type ty,
            Operator op,
            BasicBlock insertAtEnd)
    {
        super(ty, ValueKind.InstructionVal + op.index);
        setOpcode(op);
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
    public BasicBlock getParent(){return bb;}

    /**
     * Updates the basic block holds multiple instructions.
     *
     * @param bb
     */
    public void setParent(BasicBlock bb){this.bb = bb;}

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
        return getOpcode() != null ? getOpcode().opName : "";
    }

    /**
     * Obtains the getName of this instruction.
     *
     * @return return the getName of this instruction.
     */
    public String name(){return instName;}

    /**
     * An interface for InstVisitor invoking.
     *
     * @param visitor The instance of InstVisitor.
     */
    public abstract void accept(InstVisitor visitor);

    public Operator getOpcode(){return opcode;}

    public void setOpcode(Operator op){opcode = op;}

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
            setOperand(0, x, this);
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
            setOperand(0, x, this);
        }

        @Override
        public int valueNumber()
        {
            return Util.hash1(getOpcode().index, operand(0));
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
            return getType() == op.getType() && getOpcode()
                    .equals(op.getOpcode())
                    && operand(0).equals(op.operand(0));
        }

        /**
         * An interface for InstVisitor invoking.
         *
         * @param visitor The instance of InstVisitor.
         */
        @Override
        public void accept(InstVisitor visitor){}
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
            setOperand(0, x, this);
            setOperand(1, y, this);
        }

        public static Op2 create(Operator op, Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            assert lhs.getType() == rhs.getType()
                    : "Cannot create binary operator with two operands of differing type!";
            return new Op2(lhs.getType(), op, lhs, rhs, name, insertBefore);
        }

        public static Op2 create(Operator op, Value lhs, Value rhs, String name)
        {
            assert lhs.getType() == rhs.getType()
                    : "Cannot create binary operator with two operands of differing type!";
            return new Op2(lhs.getType(), op, lhs, rhs, name, (Instruction)null);
        }

        public static Op2 create(Operator op,
                Value lhs,
                Value rhs,
                String name,
                BasicBlock insertAtEnd)
        {
            assert lhs.getType() == rhs.getType()
                    : "Cannot create binary operator with two operands of differing type!";
            return new Op2(lhs.getType(), op, lhs, rhs, name, insertAtEnd);
        }

        //=====================================================================//
        //               The  first version with default insertBefore.         //
        public static Op2 createAdd(Value lhs, Value rhs, String name)
        {
            return create(Operator.Add, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createFAdd(Value lhs, Value rhs, String name)
        {
            return create(Operator.FAdd, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createSub(Value lhs, Value rhs, String name)
        {
            return create(Operator.Sub, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createFSub(Value lhs, Value rhs, String name)
        {
            return create(Operator.FSub, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createMul(Value lhs, Value rhs, String name)
        {
            return create(Operator.FMul, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createFMul(Value lhs, Value rhs, String name)
        {
            return create(Operator.FMul, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createUDiv(Value lhs, Value rhs, String name)
        {
            return create(Operator.UDiv, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createSDiv(Value lhs, Value rhs, String name)
        {
            return create(Operator.SDiv, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createFDiv(Value lhs, Value rhs, String name)
        {
            return create(Operator.FDiv, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createURem(Value lhs, Value rhs, String name)
        {
            return create(Operator.URem, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createSRem(Value lhs, Value rhs, String name)
        {
            return create(Operator.SRem, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createFRem(Value lhs, Value rhs, String name)
        {
            return create(Operator.FRem, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createShl(Value lhs, Value rhs, String name)
        {
            return create(Operator.Shl, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createLShr(Value lhs, Value rhs, String name)
        {
            return create(Operator.LShr, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createAShr(Value lhs, Value rhs, String name)
        {
            return create(Operator.AShr, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createAnd(Value lhs, Value rhs, String name)
        {
            return create(Operator.And, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createOr(Value lhs, Value rhs, String name)
        {
            return create(Operator.Or, lhs, rhs, name, (Instruction) null);
        }

        public static Op2 createXor(Value lhs, Value rhs, String name)
        {
            return create(Operator.Xor, lhs, rhs, name, (Instruction) null);
        }

        //=====================================================================//
        //                 The second version with insertAtEnd argument.       //
        public static Op2 createAdd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.Add, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createFAdd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.FAdd, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createSub(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.Sub, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createFSub(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.FSub, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createMul(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.FMul, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createFMul(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.FMul, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createUDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.UDiv, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createSDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.SDiv, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createFDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.FDiv, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createURem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.URem, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createSRem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.SRem, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createFRem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.FRem, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createShl(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.Shl, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createLShr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.LShr, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createAShr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.AShr, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createAnd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.And, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createOr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.Or, lhs, rhs, name, insertAtEnd);
        }

        public static Op2 createXor(Value lhs, Value rhs, String name, BasicBlock insertAtEnd)
        {
            return create(Operator.Xor, lhs, rhs, name, insertAtEnd);
        }


        //=====================================================================//
        //                   The third version with insertBefore argument.     //
        public static Op2 createAdd(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.Add, lhs, rhs, name, insertBefore);
        }

        public static Op2 createFAdd(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.FAdd, lhs, rhs, name, insertBefore);
        }

        public static Op2 createSub(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.Sub, lhs, rhs, name, insertBefore);
        }

        public static Op2 createFSub(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.FSub, lhs, rhs, name, insertBefore);
        }

        public static Op2 createMul(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.FMul, lhs, rhs, name, insertBefore);
        }

        public static Op2 createFMul(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.FMul, lhs, rhs, name, insertBefore);
        }

        public static Op2 createUDiv(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.UDiv, lhs, rhs, name, insertBefore);
        }

        public static Op2 createSDiv(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.SDiv, lhs, rhs, name, insertBefore);
        }

        public static Op2 createFDiv(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.FDiv, lhs, rhs, name, insertBefore);
        }

        public static Op2 createURem(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.URem, lhs, rhs, name, insertBefore);
        }

        public static Op2 createSRem(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.SRem, lhs, rhs, name, insertBefore);
        }

        public static Op2 createFRem(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.FRem, lhs, rhs, name, insertBefore);
        }

        public static Op2 createShl(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.Shl, lhs, rhs, name, insertBefore);
        }

        public static Op2 createLShr(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.LShr, lhs, rhs, name, insertBefore);
        }

        public static Op2 createAShr(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.AShr, lhs, rhs, name, insertBefore);
        }

        public static Op2 createAnd(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.And, lhs, rhs, name, insertBefore);
        }

        public static Op2 createOr(Value lhs, Value rhs, String name,Instruction insertBefore)
        {
            return create(Operator.Or, lhs, rhs, name, insertBefore);
        }

        public static Op2 createXor(Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            return create(Operator.Xor, lhs, rhs, name, insertBefore);
        }


        // ====================================================================//
        //   Some helper method for create unary operator with Bianry inst.    //
        public static Op2 createNeg(Value op, String name, Instruction insertBefore)
        {
            Value zero = ConstantInt.getNullValue(op.getType());
            return new Op2(op.getType(), Sub, zero, op, name, insertBefore);
        }

        public static Op2 createNeg(Value op, String name, BasicBlock insertAtEnd)
        {
            Value zero = ConstantInt.getNullValue(op.getType());
            return new Op2(op.getType(), Sub, zero, op, name, insertAtEnd);
        }

        public static Op2 createNeg(Value op)
        {
            Value zero = ConstantInt.getNullValue(op.getType());
            return new Op2(op.getType(), Sub, zero, op, "");
        }

        public static Op2 createFNeg(Value op, String name, Instruction insertBefore)
        {
            Value zero = ConstantFP.getNullValue(op.getType());
            return new Op2(op.getType(), Sub, zero, op, name, insertBefore);
        }

        public static Op2 createFNeg(Value op)
        {
            Value zero = ConstantInt.getNullValue(op.getType());
            return new Op2(op.getType(), Sub, zero, op, "");
        }

        public static Op2 createFNeg(Value op, String name, BasicBlock insertAtEnd)
        {
            Value zero = ConstantFP.getNullValue(op.getType());
            return new Op2(op.getType(), Sub, zero, op, name, insertAtEnd);
        }

        public static Op2 createNot(Value op, String name, Instruction insertBefore)
        {
            Constant one = Constant.getAllOnesValue(op.getType());
            return new Op2(op.getType(), Xor, one, op, name, insertBefore);
        }

        public static Op2 createNot(Value op)
        {
            Constant one = Constant.getAllOnesValue(op.getType());
            return new Op2(op.getType(), Xor, one, op, "");
        }

        public static Op2 createNot(Value op, String name, BasicBlock insertAtEnd)
        {
            Constant one = Constant.getAllOnesValue(op.getType());
            return new Op2(op.getType(), Xor, one, op, name, insertAtEnd);
        }


        /**
         * This method is used for attempting to swap the two operands of this
         * binary instruction.
         */
        public void swapOperands()
        {
            Value temp = operand(0);
            setOperand(0, operand(1), this);
            setOperand(1, temp, this);
        }

        @Override
        public int valueNumber()
        {
            return Util.hash2(getOpcode().index, operand(0), operand(1));
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
            return getType() == op.getType() && getOpcode()
                    .equals(op.getOpcode())
                    && x.equals(op.operand(0))
                    && y.equals(op.operand(1));
        }

        /**
         * An interface for InstVisitor invoking.
         *
         * @param visitor The instance of InstVisitor.
         */
        @Override
        public void accept(InstVisitor visitor)
        {

        }
    }

    public static class CastInst extends Op1
    {
        protected CastInst(Type ty,
                Operator opcode,
                Value x, String name)
        {
            super(ty, opcode, x, name);
            initialize(x);
        }

        protected CastInst(Type ty, Operator opcode,
                Value x, String name,
                Instruction insertBefore)
        {
            super(ty, opcode, x, name, insertBefore);
            initialize(x);
        }

        protected CastInst(Type ty, Operator opcode,
                Value x, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, opcode, x, name, insertAtEnd);
            initialize(x);
        }

        private void initialize(Value x)
        {
            reserve(1);
            setOperand(0, x, this);
        }

        public static CastInst createIntegerCast(
                Value value, Type destTy,
                boolean isSigned)
        {
            assert value.getType().isIntegerType() && destTy.isIntegerType()
                    :"Invalid type!";
            int srcBits = value.getType().getScalarSizeBits();
            int destBits = destTy.getScalarSizeBits();
            Operator opcode = srcBits == destBits? BitCast
                    :srcBits>destBits? Trunc
                    :(isSigned ? SExt : ZExt);

            return create(opcode, value, destTy, "", null);
        }

        public static CastInst create(Operator opcode, Value value,
                Type ty, String name, Instruction insertBefore)
        {
            switch (opcode)
            {
                case Trunc: return new TruncInst(value, ty, name, insertBefore);
                case ZExt: return new ZExtInst(value, ty, name, insertBefore);
                case SExt: return new SExtInst(value, ty, name, insertBefore);
                case FPTrunc: return new FPTruncInst(value, ty, name, insertBefore);
                case FPExt: return new FPExtInst(value, ty, name, insertBefore);
                case UIToFP: return new UIToFPInst(value, ty, name, insertBefore);
                case SIToFP: return new SIToFPInst(value, ty, name, insertBefore);
                case FPToUI: return new FPToUIInst(value, ty, name, insertBefore);
                case FPToSI: return new FPToSIInst(value, ty, name, insertBefore);
                case PtrToInt: return new PtrToIntInst(value, ty, name, insertBefore);
                case IntToPtr: return new IntToPtrInst(value, ty, name, insertBefore);
                case BitCast: return new BitCastInst(value, ty, name, insertBefore);
                default:
                    assert false:"Invalid opcode provided!";
            }
            return null;
        }
    }

    public static class UIToFPInst extends CastInst
    {
        public UIToFPInst(Value x, Type ty, String name)
        {
            super(ty, UIToFP, x, name);
        }

        public UIToFPInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, UIToFP, x, name, insertBefore);
        }

        public UIToFPInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, UIToFP, x, name, insertAtEnd);
        }
    }

    public static class SIToFPInst extends CastInst
    {
        public SIToFPInst(Value x, Type ty, String name)
        {
            super(ty, SIToFP, x, name);
        }

        public SIToFPInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, SIToFP, x, name, insertBefore);
        }

        public SIToFPInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, SIToFP, x, name, insertAtEnd);
        }
    }

    public static class TruncInst extends CastInst
    {
        public TruncInst(Value x, Type ty, String name)
        {
            super(ty, Trunc, x, name);
        }

        public TruncInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, Trunc, x, name, insertBefore);
        }

        public TruncInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, Trunc, x, name, insertAtEnd);
        }
    }

    public static class ZExtInst extends CastInst
    {
        public ZExtInst(Value x, Type ty, String name)
        {
            super(ty, ZExt, x, name);
        }

        public ZExtInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, ZExt, x, name, insertBefore);
        }

        public ZExtInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, ZExt, x, name, insertAtEnd);
        }
    }

    public static class SExtInst extends CastInst
    {
        public SExtInst(Value x, Type ty, String name)
        {
            super(ty, SExt, x, name);
        }

        public SExtInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, SExt, x, name, insertBefore);
        }

        public SExtInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, SExt, x, name, insertAtEnd);
        }
    }

    public static class FPTruncInst extends CastInst
    {
        public FPTruncInst(Value x, Type ty, String name)
        {
            super(ty, FPTrunc, x, name);
        }

        public FPTruncInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, FPTrunc, x, name, insertBefore);
        }

        public FPTruncInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, FPTrunc, x, name, insertAtEnd);
        }
    }

    public static class FPExtInst extends CastInst
    {
        public FPExtInst(Value x, Type ty, String name)
        {
            super(ty, FPExt, x, name);
        }

        public FPExtInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, FPExt, x, name, insertBefore);
        }

        public FPExtInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, FPExt, x, name, insertAtEnd);
        }
    }

    public static class FPToUIInst extends CastInst
    {
        public FPToUIInst(Value x, Type ty, String name)
        {
            super(ty, FPToUI, x, name);
        }

        public FPToUIInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, FPToUI, x, name, insertBefore);
        }

        public FPToUIInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, FPToUI, x, name, insertAtEnd);
        }
    }

    public static class FPToSIInst extends CastInst
    {
        public FPToSIInst(Value x, Type ty, String name)
        {
            super(ty, FPToSI, x, name);
        }

        public FPToSIInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, FPToSI, x, name, insertBefore);
        }

        public FPToSIInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, FPToSI, x, name, insertAtEnd);
        }
    }

    public static class PtrToIntInst extends CastInst
    {
        public PtrToIntInst(Value x, Type ty, String name)
        {
            super(ty, PtrToInt, x, name);
        }

        public PtrToIntInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, PtrToInt, x, name, insertBefore);
        }

        public PtrToIntInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, PtrToInt, x, name, insertAtEnd);
        }
    }

    public static class IntToPtrInst extends CastInst
    {
        public IntToPtrInst(Value x, Type ty, String name)
        {
            super(ty, IntToPtr, x, name);
        }

        public IntToPtrInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, IntToPtr, x, name, insertBefore);
        }

        public IntToPtrInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, IntToPtr, x, name, insertAtEnd);
        }
    }

    public static class BitCastInst extends CastInst
    {
        public BitCastInst(Value x, Type ty, String name)
        {
            super(ty, BitCast, x, name);
        }

        public BitCastInst(Value x, Type ty, String name,
                Instruction insertBefore)
        {
            super(ty, BitCast, x, name, insertBefore);
        }

        public BitCastInst(Value x, Type ty, String name,
                BasicBlock insertAtEnd)
        {
            super(ty, BitCast, x, name, insertAtEnd);
        }
    }

	/**
     * This is a base class for the comparison instructions.
     */
    public static abstract class CmpInst extends Instruction
    {
        protected Predicate pred;
        protected CmpInst(final backend.type.Type ty, Operator op, Predicate pred,
                Value lhs, Value rhs, String name, Instruction insertBefore)
        {
            super(ty, op, insertBefore);
            init(lhs, rhs);
        }

        protected CmpInst(final backend.type.Type ty, Operator op, Predicate pred,
                Value lhs, Value rhs, String name,  BasicBlock insertAtEnd)
        {
            super(ty, op, insertAtEnd);
            init(lhs, rhs);
        }

        private void init(Value lhs, Value rhs)
        {
            reserve(2);
            setOperand(0, lhs, this);
            setOperand(1, rhs, this);
            this.name = name;
            this.pred = pred;
        }

        public Predicate getPredicate() {return pred;}

        public void setPredicate(Predicate newPred) {pred = newPred;}

        public Predicate getInversePredicate()
        {
            return getInversePredicate(pred);
        }

        public Predicate getSwappedPredicate()
        {
            return getSwappedPredicate(pred);
        }

        public static Predicate getInversePredicate(Predicate pred)
        {
            switch (pred)
            {
                default: assert false: ("Unknown cmp predicate!");
                case ICMP_EQ: return ICMP_NE;
                case ICMP_NE: return ICMP_EQ;
                case ICMP_UGT: return ICMP_ULE;
                case ICMP_ULT: return ICMP_UGE;
                case ICMP_ULE: return ICMP_UGT;
                case ICMP_UGE: return ICMP_ULT;
                case ICMP_SGT: return ICMP_SLE;
                case ICMP_SLT: return ICMP_SGE;
                case ICMP_SGE: return ICMP_SLT;
                case ICMP_SLE: return ICMP_SGT;

                case FCMP_OEQ: return FCMP_ONE;
                case FCMP_ONE: return FCMP_OEQ;
                case FCMP_OGT: return FCMP_OLE;
                case FCMP_OLT: return FCMP_OGE;
                case FCMP_OLE: return FCMP_OGT;
                case FCMP_OGE: return FCMP_OLT;
                case FCMP_UEQ: return FCMP_UNE;
                case FCMP_UNE: return FCMP_UEQ;
                case FCMP_UGT: return FCMP_ULE;
                case FCMP_ULT: return FCMP_UGE;
                case FCMP_UGE: return FCMP_ULT;
                case FCMP_ULE: return FCMP_UGT;
                case FCMP_ORD: return FCMP_UNO;
                case FCMP_UNO: return FCMP_ORD;
                case FCMP_TRUE: return FCMP_FALSE;
                case FCMP_FALSE: return FCMP_TRUE;
            }
        }

        public static Predicate getSwappedPredicate(Predicate pred)
        {
            switch (pred)
            {
                default: assert false:"Unknown cmp predicate!";
                case ICMP_EQ:
                case ICMP_NE:
                case FCMP_FALSE: case FCMP_TRUE:
                case FCMP_OEQ: case FCMP_ONE:
                case FCMP_UEQ: case FCMP_UNE:
                case FCMP_ORD: case FCMP_UNO:
                    return pred;

                case ICMP_SGT:
                    return ICMP_SLT;
                case ICMP_SLT:
                    return ICMP_SGT;
                case ICMP_SGE:
                    return ICMP_SLE;
                case ICMP_SLE:
                    return ICMP_SGE;
                case ICMP_UGT:
                    return ICMP_ULT;
                case ICMP_ULT:
                    return ICMP_UGT;
                case ICMP_UGE:
                    return ICMP_ULE;
                case ICMP_ULE:
                    return ICMP_UGE;
                case FCMP_OGT:
                    return FCMP_OLT;
                case FCMP_OLT:
                    return FCMP_OGT;
                case FCMP_OGE:
                    return FCMP_OLE;
                case FCMP_OLE:
                    return FCMP_OGE;
                case FCMP_UGT:
                    return FCMP_ULT;
                case FCMP_ULT:
                    return FCMP_UGT;
                case FCMP_UGE:
                    return FCMP_ULE;
                case FCMP_ULE:
                    return FCMP_UGE;
            }
        }

        public boolean isCommutative()
        {
            if (this instanceof ICmpInst)
                return ((ICmpInst)this).isCommutative();
            return ((FCmpInst)this).isCommutative();
        }

        public boolean isEquality()
        {
            if (this instanceof ICmpInst)
                return ((ICmpInst)this).isEquality();
            return ((FCmpInst)this).isEquality();
        }

        public boolean isRelational()
        {
            if (this instanceof ICmpInst)
                return ((ICmpInst)this).isRelational();
            return ((FCmpInst)this).isRelational();
        }

        public void swapOperands()
        {
            if (this instanceof ICmpInst)
                ((ICmpInst)this).swapOperands();
            else
                ((FCmpInst)this).swapOperands();
        }

        public static boolean isUnsigned(Predicate pred)
        {
            switch (pred)
            {
                default: return false;
                case ICMP_ULT: case ICMP_ULE: case ICMP_UGT:
                case ICMP_UGE: return true;
            }
        }

        public static boolean isSigned(Predicate pred)
        {
            switch (pred)
            {
                default: return false;
                case ICMP_SLT: case ICMP_SLE: case ICMP_SGT:
                case ICMP_SGE: return true;
            }
        }

        public static boolean isOrdered(Predicate pred)
        {
            switch (pred)
            {
                default: return false;
                case FCMP_OEQ: case FCMP_ONE: case FCMP_OGT:
                case FCMP_OLT: case FCMP_OGE: case FCMP_OLE:
                case FCMP_ORD: return true;
            }
        }

        public static boolean isUnOrdered(Predicate pred)
        {
            switch (pred)
            {
                default: return false;
                case FCMP_UEQ: case FCMP_UNE: case FCMP_UGT:
                case FCMP_ULT: case FCMP_UGE: case FCMP_ULE:
                case FCMP_UNO: return true;
            }
        }

	    /**
         * This enumeration lists the possible predicates for CmpInst subclasses.
         * Values in the range 0-31 are reserved for FCmpInst, while values in the
         * range 32-64 are reserved for ICmpInst. This is necessary to ensure the
         * predicate values are not overlapping between the classes.
         */
        public enum Predicate
        {
            // Opcode             U L G E    Intuitive operation
            FCMP_FALSE,  /// 0 0 0 0    Always false (always folded)
            FCMP_OEQ,  /// 0 0 0 1    True if ordered and equal
            FCMP_OGT,  /// 0 0 1 0    True if ordered and greater than
            FCMP_OGE,  /// 0 0 1 1    True if ordered and greater than or equal
            FCMP_OLT,  /// 0 1 0 0    True if ordered and less than
            FCMP_OLE,  /// 0 1 0 1    True if ordered and less than or equal
            FCMP_ONE,  /// 0 1 1 0    True if ordered and operands are unequal
            FCMP_ORD,  /// 0 1 1 1    True if ordered (no nans)
            FCMP_UNO,  /// 1 0 0 0    True if unordered: isnan(X) | isnan(Y)
            FCMP_UEQ,  /// 1 0 0 1    True if unordered or equal
            FCMP_UGT,  /// 1 0 1 0    True if unordered or greater than
            FCMP_UGE,  /// 1 0 1 1    True if unordered, greater than, or equal
            FCMP_ULT,  /// 1 1 0 0    True if unordered or less than
            FCMP_ULE,  /// 1 1 0 1    True if unordered, less than, or equal
            FCMP_UNE,  /// 1 1 1 0    True if unordered or not equal
            FCMP_TRUE,  /// 1 1 1 1    Always true (always folded)
            ICMP_EQ ,  /// equal
            ICMP_NE ,  /// not equal
            ICMP_UGT,  /// unsigned greater than
            ICMP_UGE,  /// unsigned greater or equal
            ICMP_ULT,  /// unsigned less than
            ICMP_ULE,  /// unsigned less or equal
            ICMP_SGT,  /// signed greater than
            ICMP_SGE,  /// signed greater or equal
            ICMP_SLT,  /// signed less than
            ICMP_SLE,  /// signed less or equal
            BAD_ICMP_PREDICATE;

            public static final Predicate FIRST_FCMP_PREDICATE = FCMP_FALSE;
            public static final Predicate LAST_FCMP_PREDICATE = FCMP_TRUE;
            public static final Predicate BAD_FCMP_PREDICATE = ICMP_EQ;

            public static final Predicate FIRST_ICMP_PREDICATE = ICMP_EQ;
            public static final Predicate LAST_ICMP_PREDICATE = ICMP_SLE;

        }
    }

	/**
	 * This instruction compares its operands according to the predicate given
     * to the constructor. It only operates on floating point values or packed
     * vectors of floating point values. The operands must be identical types.
     */
    public static class FCmpInst extends CmpInst
    {

        public FCmpInst(Predicate pred, Value lhs,
                Value rhs, String name, Instruction insertBefore)
        {
            super(lhs.getType(), FCmp, pred, lhs, rhs, name, insertBefore);
            assert pred.compareTo(Predicate.LAST_FCMP_PREDICATE)<=0
                    :"Invalid FCmp predicate value";
            assert lhs.getType() == rhs.getType():
                    "Both operands to FCmp instruction are not of the same type!";
            assert lhs.getType().isFloatingPointType():
                    "Invalid operand types for FCmp instruction";
        }

        public FCmpInst(Predicate pred, Value lhs,
                Value rhs, String name, BasicBlock insertAtEnd)
        {
            super(lhs.getType(), FCmp, pred, lhs, rhs, name, insertAtEnd);
            assert pred.compareTo(Predicate.LAST_FCMP_PREDICATE)<=0
                    :"Invalid FCmp predicate value";
            assert lhs.getType() == rhs.getType():
                    "Both operands to FCmp instruction are not of the same type!";
            assert lhs.getType().isFloatingPointType():
                    "Invalid operand types for FCmp instruction";
        }

        public FCmpInst(Predicate pred, Value lhs, Value rhs)
        {
            super(lhs.getType(), FCmp, pred, lhs, rhs, "", (Instruction)null);
            assert pred.compareTo(Predicate.LAST_FCMP_PREDICATE)<=0
                    :"Invalid FCmp predicate value";
            assert lhs.getType() == rhs.getType():
                    "Both operands to FCmp instruction are not of the same type!";
            assert lhs.getType().isFloatingPointType():
                    "Invalid operand types for FCmp instruction";
        }

        public FCmpInst(Predicate pred, Value lhs, Value rhs, String name)
        {
            super(lhs.getType(), FCmp, pred, lhs, rhs, name, (Instruction)null);
            assert pred.compareTo(Predicate.LAST_FCMP_PREDICATE)<=0
                    :"Invalid FCmp predicate value";
            assert lhs.getType() == rhs.getType():
                    "Both operands to FCmp instruction are not of the same type!";
            assert lhs.getType().isFloatingPointType():
                    "Invalid operand types for FCmp instruction";
        }

        /**
         * An interface for InstVisitor invoking.
         *
         * @param visitor The instance of InstVisitor.
         */
        @Override
        public void accept(InstVisitor visitor)
        {

        }

        public boolean isEquality()
        {
            return pred == FCMP_OEQ || pred == FCMP_ONE ||
                    pred == FCMP_UEQ || pred == FCMP_UNE;
        }

        public boolean isCommutative()
        {
            return isEquality() ||
                    pred == FCMP_FALSE ||
                    pred == FCMP_TRUE ||
                    pred == FCMP_ORD ||
                    pred == FCMP_UNO;
        }

        @Override
        public boolean isRelational()
        {
            return !isEquality();
        }

        @Override
        public void swapOperands()
        {
            pred = getSwappedPredicate();
            operandList.get(0).swap(operandList.get(1));
        }
    }

	/**
	 * This instruction compares its operands according to the predicate given
     * to the constructor. It only operates on integers or pointers. The operands
     * must be identical types.
     *
     */
    public static class ICmpInst extends CmpInst
    {

        public ICmpInst(Predicate pred, Value lhs,
                Value rhs, String name, Instruction insertBefore)
        {
            super(lhs.getType(), ICmp, pred, lhs, rhs, name, insertBefore);
            assert pred.compareTo(Predicate.LAST_ICMP_PREDICATE)<=0
                    :"Invalid ICmp predicate value";
            assert lhs.getType() == rhs.getType():
                    "Both operands to ICmp instruction are not of the same type!";
            assert lhs.getType().isIntegerType():
                    "Invalid operand types for ICmp instruction";
        }

        public ICmpInst( Predicate pred, Value lhs,
                Value rhs, String name, BasicBlock insertAtEnd)
        {
            super(lhs.getType(), ICmp, pred, lhs, rhs, name, insertAtEnd);
            assert pred.compareTo(Predicate.LAST_ICMP_PREDICATE)<=0
                    :"Invalid ICmp predicate value";
            assert lhs.getType() == rhs.getType():
                    "Both operands to ICmp instruction are not of the same type!";
            assert lhs.getType().isIntegerType():
                    "Invalid operand types for ICmp instruction";
        }

        public ICmpInst( Predicate pred, Value lhs,
                Value rhs)
        {
            super(lhs.getType(), ICmp, pred, lhs, rhs, "", (Instruction) null);
            assert pred.compareTo(Predicate.LAST_ICMP_PREDICATE)<=0
                    :"Invalid ICmp predicate value";
            assert lhs.getType() == rhs.getType():
                    "Both operands to ICmp instruction are not of the same type!";
            assert lhs.getType().isIntegerType():
                    "Invalid operand types for ICmp instruction";
        }

        public ICmpInst( Predicate pred, Value lhs,
                Value rhs, String name)
        {
            super(lhs.getType(), ICmp, pred, lhs, rhs, name, (Instruction) null);
            assert pred.compareTo(Predicate.LAST_ICMP_PREDICATE)<=0
                    :"Invalid ICmp predicate value";
            assert lhs.getType() == rhs.getType():
                    "Both operands to ICmp instruction are not of the same type!";
            assert lhs.getType().isIntegerType():
                    "Invalid operand types for ICmp instruction";
        }

        /**
         * An interface for InstVisitor invoking.
         *
         * @param visitor The instance of InstVisitor.
         */
        @Override
        public void accept(InstVisitor visitor)
        {

        }

        public static Predicate getSignedPredicate(Predicate pred)
        {
            switch (pred)
            {
                default:assert false:"Unknown icmp predicate!";
                case ICMP_EQ: case ICMP_NE:
                case ICMP_SGT: case ICMP_SLT: case ICMP_SGE: case ICMP_SLE:
                return pred;
                case ICMP_UGT: return ICMP_SGT;
                case ICMP_ULT: return ICMP_SLT;
                case ICMP_UGE: return ICMP_SGE;
                case ICMP_ULE: return ICMP_SLE;
            }
        }

        public Predicate getSignedPredicate()
        {
            return getSignedPredicate(pred);
        }

        public static Predicate getUnsignedPredicate(Predicate pred)
        {
            switch (pred) {
                default: assert false: "Unknown icmp predicate!";
                case ICMP_EQ: case ICMP_NE:
                case ICMP_UGT: case ICMP_ULT: case ICMP_UGE: case ICMP_ULE:
                    return pred;
                case ICMP_SGT: return ICMP_UGT;
                case ICMP_SLT: return ICMP_ULT;
                case ICMP_SGE: return ICMP_UGE;
                case ICMP_SLE: return ICMP_ULE;
            }
        }
        @Override
        public boolean isEquality()
        {
            return pred == ICMP_EQ || pred == ICMP_NE;
        }
        @Override
        public boolean isCommutative()
        {
            return isEquality();
        }

        @Override
        public boolean isRelational() {return !isEquality();}

        public boolean isSignedPredicate() { return isSignedPredicate(pred);}

        public static boolean isSignedPredicate(Predicate pred)
        {
            switch (pred)
            {
                default:
                    assert false : ("Unknown icmp predicate!");
                case ICMP_SGT:
                case ICMP_SLT:
                case ICMP_SGE:
                case ICMP_SLE:
                    return true;
                case ICMP_EQ:
                case ICMP_NE:
                case ICMP_UGT:
                case ICMP_ULT:
                case ICMP_UGE:
                case ICMP_ULE:
                    return false;
            }
        }

	    /**
         * Exchange the two operands to this instruction in such a way that it does
         * not modify the semantics of the instruction. The predicate value may be
         * changed to retain the same result if the predicate is order dependent
         */
        @Override
        public void swapOperands()
        {
            pred = getSwappedPredicate();
            operandList.get(0).swap(operandList.get(1));
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
         * @param ifTrue       the branch TargetData.
         * @param insertBefore
         */
        public BranchInst(BasicBlock ifTrue, Instruction insertBefore)
        {
            super(Operator.Br, "", insertBefore);
            reserve(1);
            setOperand(0, ifTrue, this);
        }

        /**
         * Constructs a branch instruction.
         * <p>
         * BranchInst(BasicBlock bb) - 'br B'
         *
         * @param ifTrue the TargetData of this branch.
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
         */
        public BranchInst(BasicBlock ifTrue, BasicBlock ifFalse, Value cond)
        {
            this(ifTrue, ifFalse, cond, (Instruction)null);
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
            setOperand(0, ifTrue, this);
            setOperand(1, ifFalse, this);
            setOperand(2, cond, this);
        }

        public BranchInst(BasicBlock ifTrue, BasicBlock ifFalse, Value cond,
                BasicBlock insertAtEnd)
        {
            super(Operator.Br, "", insertAtEnd);

            setOperand(0, ifTrue, this);
            setOperand(1, ifFalse, this);
            setOperand(2, cond, this);
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
            setOperand(0, ifTrue, this);
        }

        public boolean isUnconditional(){return getNumOfOperands() == 1;}

        public boolean isConditional(){return getNumOfOperands() == 3;}

        public Value getCondition()
        {
            assert (isConditional()) : "can not get a condition of uncondition branch";
            return operand(2);
        }

        public void setCondition(Value cond)
        {
            assert (cond != null) : "can not update condition with null";
            assert (isConditional()) : "can not set condition of uncondition branch";
            setOperand(2, cond, this);
        }

        public BranchInst clone()
        {
            return null;
        }

        @Override
        public void accept(InstVisitor visitor)
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
            setOperand(index, bb, this);
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

        public ReturnInst(Value val)
        {
            this(val, "", (Instruction) null);
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
                setOperand(0, retValue, this);
            }
        }

        public ReturnInst(Value retValue, String name, BasicBlock insertAtEnd)
        {
            super(Operator.Ret, name, insertAtEnd);
            if (retValue != null)
            {
                reserve(1);
                setOperand(0, retValue, this);
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
        public void accept(InstVisitor visitor){}

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
    public static class CallInst extends Instruction
    {
        // Returns the operand number of the first argument
        private final int ArgumentOffset = 1;

        public CallInst(Value[] args, Value target)
        {
            this(args, target, "");
        }

        /**
         * Constructs a new method calling instruction.
         *
         * @param args   The input arguments.
         * @param target The called method.
         */
        public CallInst(Value[] args, Value target, String name)
        {
            this(args, target, name, (Instruction) null);
        }

        /**
         * Constructs a new method calling instruction.
         *
         * @param args   The input arguments.
         * @param target The called method.
         */
        public CallInst(Value[] args, Value target,
                String name, Instruction insertBefore)
        {
            super(((FunctionType)((PointerType)target.getType()).
                    getElemType()).getReturnType(),
                    Operator.Call, insertBefore);
            this.name = name.isEmpty()?Operator.Call.opName:name;
            init(target, args);
        }

        public CallInst(Value[] args, Value target,
                String name, BasicBlock insertAtEnd)
        {
            super(((FunctionType)((PointerType)target.getType()).
                    getElemType()).getReturnType(),
                    Operator.Call, insertAtEnd);
            this.name = name.isEmpty()?Operator.Call.opName:name;
            init(target, args);
        }


        private void init(Value function, Value[] args)
        {
            reserve(ArgumentOffset + args.length);
            assert (getNumOfOperands()
                    == ArgumentOffset + args.length) : "NumOperands not set up?";
            setOperand(0, function, this);
            int idx = ArgumentOffset;
            for (Value arg : args)
            {
                setOperand(idx++, arg, this);
            }
        }

        @Override
        public void accept(InstVisitor visitor){}

        @Override
        public String toString()
        {
            return name();
        }

        public int getNumsOfArgs()
        {
            return getNumOfOperands() - ArgumentOffset;
        }

        public void setArgument(int index, Value val)
        {
            assert index + ArgumentOffset >= 0 && index + ArgumentOffset < getNumsOfArgs();
            setOperand(index + ArgumentOffset, val, this);
        }

        public Value argumentAt(int index)
        {
            assert index + ArgumentOffset >= 0 && index + ArgumentOffset < getNumsOfArgs();
            return operand(index + ArgumentOffset);
        }

        public Function getCalledFunction()
        {
            return (Function) operand(0);
        }

        public Value getCalledValue()
        {
            return operand(0);
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
            this(condV, defaultBB, numCases, name, null);
        }

        /**
         * Constructs a new SwitchInst instruction with specified inst frontend.type.
         *
         * @param condV        the value of selector.
         * @param defaultBB    The default jump block when no other case match.
         * @param insertBefore
         */
        public SwitchInst(Value condV,
                BasicBlock defaultBB,
                int numCases,
                String name,
                Instruction insertBefore)
        {
            super(Operator.Switch, name,insertBefore);
            init(condV, defaultBB, numCases);
        }


        /**
         * Initialize some arguments, like add switch value and default into
         * Operand list.
         */
        private void init(Value cond, BasicBlock defaultBB, int numCases)
        {
            // the 2 indicates what number of default basic block and default value.
            reserve(offset+numCases);
            setOperand(0, cond, this);
            setOperand(1, defaultBB, this);
        }

        /**
         * An interface for InstVisitor invoking.
         *
         * @param visitor The instance of InstVisitor.
         */
        @Override
        public void accept(InstVisitor visitor){}

        public void addCase(Constant caseVal, BasicBlock targetBB)
        {
            int opNo = getNumOfCases();
            setOperand(opNo, caseVal, this);
            setOperand(opNo+1, targetBB, this);
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
            setOperand(0, val, this);
        }

        public int getNumOfCases()
        {
            return getNumOfOperands() >> 1;
        }

        /**
         * Search all of the case values for the specified constants.
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
            return (Constant)operand(2*index);
        }

        public BasicBlock getSuccessor(int index)
        {
            assert index >= 0 && index < getNumOfSuccessors()
                    : "Successor index out of range for switch";
            return (BasicBlock) operand(2*index + 1);

        }
        public void setSuccessor(int index, BasicBlock newBB)
        {
            assert index >= 0 && index < getNumOfSuccessors()
                    : "Successor index out of range for switch";
            setOperand(index * 2 + 1, newBB, this);
        }
        // setSuccessorValue - Updates the value associated with the specified
        // successor.
        public void setSuccessorValue(int idx, Constant SuccessorValue)
        {
            assert(idx>=0 && idx < getNumOfSuccessors())
                    : "Successor # out of range!";
            setOperand(idx*2, SuccessorValue, this);
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
            this(ty, numReservedValues, name, (Instruction) null);
        }

        public PhiNode(Type ty, int numReservedValues,
                String name, Instruction insertBefore)
        {
            super(ty, Operator.Phi, insertBefore);
            reserve(numReservedValues);
            this.name = name;
        }

        public PhiNode(Type type, int numReservedValue, String name,
                BasicBlock insertAtEnd)
        {
            super(type, Operator.Phi, insertAtEnd);
            reserve(numReservedValue);
            this.name = name;
        }

        @Override
        public void accept(InstVisitor visitor){}

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
            setOperand(index << 1, val, this);
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
            setOperand((index << 1) + 1, bb, this);
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
            removeIncomingValue(bb, true);
        }

        public Value removeIncomingValue(BasicBlock bb, boolean deletePhiIfEmpty)
        {
            int index = getBasicBlockIndex(bb);
            assert index >= 0 : "invalid basic block argument to remove";
            return removeIncomingValue(index, deletePhiIfEmpty);
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
         * Gets the getName of this phi node.
         *
         * @return
         */
        public String name()
        {
            return instName;
        }

        /**
         *  hasConstantValue - If the specified PHI node always merges
         *  together the same value, return the value, otherwise return null.
         * @return
         */
        public Value hasConstantValue()
        {
            Value val = getIncomingValue(0);
            for (int i = 1, e = getNumberIncomingValues(); i< e; i++)
            {
                if (getIncomingValue(i) != val)
                    return null;
            }
            return val;
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
         * @param arraySize  The number of elements if allocating is used for
         *                   array.
         * @param name The getName of this instruction for debugging.
         */
        public AllocaInst(Type ty,
                Value arraySize,
                String name,
                Instruction insertBefore)
        {
            super(ty, Operator.Alloca, insertBefore);

            // default to 1.
            if (arraySize == null)
                arraySize = ConstantInt.get(Int32Ty, 1);

            reserve(1);
            assert arraySize.getType() == Type.Int32Ty
                    :"Alloca array size != UnsignedIntTy";
            setOperand(0, arraySize, this);
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
            return operand(0) != ConstantInt.get(Int32Ty, 1);
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
        public void accept(InstVisitor visitor){}

        /**
         * Gets the getName of this alloated variable.
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
            setOperand(0, value, this);
            setOperand(1, ptr, this);
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
        public void accept(InstVisitor visitor){}
    }

    /**
     * An instruction for reading data from memory.
     */
    public static class LoadInst extends Op1
    {
        private boolean isVolatile;

        public LoadInst(Value from,
                String name, Instruction insertBefore)
        {
            super(((PointerType)from.getType()).getElemType(),
                    Operator.Load,
                    from,
                    name,
                    insertBefore);
            assertOK();
        }

        public LoadInst(Value from,
                String name)
        {
            super(((PointerType)from.getType()).getElemType(),
                    Operator.Load, from, name, (Instruction)null);
            assertOK();
        }

        public LoadInst(Value from,
                String name, BasicBlock insertAtEnd)
        {
            super(((PointerType)from.getType()).getElemType(),
                    Operator.Load, from, name, insertAtEnd);
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
        public void accept(InstVisitor visitor){}
    }

	/**
	 * A instruction for type-safe pointer arithmetic to access elements of arrays and structs.
     */
    public static class GetElementPtrInst extends Instruction
    {
        public GetElementPtrInst(Value ptr,
                Value idx,
                String name,
                Instruction insertBefore)
        {
            super(PointerType.get(checkType(getIndexedType(ptr.getType(), idx))),
                    GetElementPtr, insertBefore);
            reserve(2);
            init(ptr, idx, name);
        }

        public GetElementPtrInst(Value ptr, Value idx, String name, BasicBlock insertAtEnd)
        {
            super(PointerType.get(checkType(getIndexedType(ptr.getType(), idx))),
                    GetElementPtr, insertAtEnd);
            reserve(2);
            init(ptr, idx, name);
        }

        public GetElementPtrInst(Value ptr, Value idx, String name)
        {
            this(ptr, idx, name, (Instruction)null);
        }

        public GetElementPtrInst(Value ptr, Value idx)
        {
            this(ptr, idx, "", (Instruction)null);
        }

        private void init(Value ptr, Value idx, String name)
        {
            assert getNumOfOperands() == 2:"NumOperands not initialized.";
            setOperand(0, ptr, this);
            setOperand(1, idx, this);
            this.name = name;
        }

        /**
         * A simple wrapper function to given a better assert failure message
         * on bad indexes for a {@linkplain GetElementPtrInst} instruction.
         * @param ty
         * @return
         */
        public static Type checkType(Type ty)
        {
            assert ty!=null:"Invalid GetElementPtrInst indice for type!";
            return ty;
        }

        public static Type getIndexedType(Type ptrType, Value idx)
        {
            // It is not pointer type.
            if (!(ptrType instanceof PointerType))
                return null;
            PointerType pt = (PointerType)ptrType;

            // Check the pointer index.
            if (!pt.indexValid(idx)) return null;

            return pt.getElemType();
        }

	    /**
         * Overload to return most specific pointer type.
         * @return
         */
        @Override
        public PointerType getType()
        {
            return (PointerType) super.getType();
        }

        public Value getPointerOperand()
        {
            return operand(0);
        }

        public PointerType getPointerOperandType()
        {
            return (PointerType)getPointerOperand().getType();
        }

        public boolean hasIndices(){return getNumOfOperands() > 1;}

        /**
         * An interface for InstVisitor invoking.
         *
         * @param visitor The instance of InstVisitor.
         */
        @Override
        public void accept(InstVisitor visitor){}
    }
}
