package backend.value;

import backend.ir.AllocationInst;
import backend.support.*;
import backend.type.*;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static backend.value.Instruction.CmpInst.Predicate.*;
import static backend.value.Operator.*;

/**
 * This class is an abstract representation of Quadruple. In this class,
 * subclass of @ {@code Instruction} represents arithmetic and logical
 * operation, control flow operators,PhiNode assignment, function calling
 * conditional statement.
 *
 * @author Jianping Zeng
 * @version 1.0
 * @see BasicBlock
 * @see User
 * @see Value
 * @see Use
 */
public abstract class Instruction extends User {
  protected Operator opcode;

  /**
   * The basic block containing this Value.
   */
  protected BasicBlock parent;

  public Instruction(Type ty,
                     Operator opc,
                     String name,
                     Instruction insertBefore) {
    super(ty, ValueKind.InstructionVal + opc.index);
    opcode = opc;
    if (insertBefore != null) {
      Util.assertion((insertBefore.getParent() != null), "Instruction to insert before is not in a basic block");

      int idx = insertBefore.getParent().indexOf(insertBefore);
      insertBefore.getParent().insertBefore(this, idx);
      setParent(insertBefore.getParent());
    }
    setName(name);
  }

  public Instruction(Type ty,
                     Operator opc,
                     String name,
                     BasicBlock insertAtEnd) {
    super(ty, ValueKind.InstructionVal + opc.index);
    opcode = opc;
    parent = insertAtEnd;
    setName(name);
    // append this instruction into the basic block
    Util.assertion((insertAtEnd != null), "Basic block to append to may not be NULL!");

    insertAtEnd.appendInst(this);
  }

  public Instruction(
      Type ty,
      Operator op,
      String name) {
    this(ty, op, name, (Instruction) null);
  }

  /**
   * A simple wrapper function to given a better assert failure message
   * on bad indexes for a {@linkplain GetElementPtrInst} instruction.
   *
   * @param ty
   * @return
   */
  public static Type checkType(Type ty) {
    Util.assertion(ty != null, "Invalid GetElementPtrInst indices for type!");
    return ty;
  }

  /**
   * Erases this instruction from it's parent basic block.
   */
  public void eraseFromParent() {
    Util.assertion(parent != null, "The basic block where the instruction reside to be erased!");

    parent.removeInst(this);
    parent = null;
    // FIXME, remove this instruction from def-use.
    for (int i = 0; i < getNumOfOperands(); i++) {
      operand(i).killUse(this);
    }
    operandList.clear();

    if (usesList != null && !usesList.isEmpty()) {
      for (Use u : usesList) {
        Instruction inst = ((Instruction) u.getUser());
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
          if (inst.operand(i) == this)
            inst.setOperand(i, UndefValue.get(getType()));
        }
      }
    }
  }

  /**
   * Inserts an specified instruction into basic block immediately before
   * specified instruction.
   *
   * @param insertPos the position where this instruction will be inserted before.
   */
  public void insertAfter(Instruction insertPos) {
    Util.assertion((insertPos != null));
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
  public void insertBefore(Instruction insertPos) {
    Util.assertion(insertPos != null);
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
  public String toString() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream os = new PrintStream(baos)) {
      print(os);
      return baos.toString();
    }
  }

  public boolean mayHasSideEffects() {
    return mayWriteMemory();
  }

  public boolean mayWriteMemory() {
    switch (getOpcode()) {
      case Store:
        return true;
      case Load:
        return ((LoadInst) this).isVolatile;
      default:
        return false;
    }
  }

  public boolean mayReadMemory() {
    switch (opcode) {
      default:
        return false;
      case Load:
        return true;
      case Store:
        return ((StoreInst) this).isVolatile();
    }
  }

  @Override
  public Instruction clone() {
    setParent(null);
    return this;
  }

  public int getIndexToBB() {
    return getParent().indexOf(this);
  }

  public boolean isIdenticalTo(Instruction otherInst) {
    return isIdenticalToWhenDefined(otherInst);
  }

  public boolean isIdenticalToWhenDefined(Instruction otherInst) {
    if (getOpcode() != otherInst.getOpcode()
        || getNumOfOperands() != otherInst.getNumOfOperands()
        || getType() != otherInst.getType()
        || !getClass().equals(otherInst.getClass()))
      return false;

    for (int i = 0, e = getNumOfOperands(); i < e; i++)
      if (operand(i) != otherInst.operand(i))
        return false;

    if (this instanceof LoadInst) {
      LoadInst li = (LoadInst) this;
      return li.isVolatile == ((LoadInst) otherInst).isVolatile;
    }
    if (this instanceof StoreInst) {
      return ((StoreInst) this).isVolatile() ==
          ((StoreInst) otherInst).isVolatile();
    }
    if (this instanceof CmpInst) {
      return ((CmpInst) this).getPredicate() ==
          ((CmpInst) otherInst).getPredicate();
    }
    return true;
  }

  public void moveBefore(Instruction insertPos) {
    int idx = insertPos.getParent().indexOf(insertPos);
    insertPos.getParent().insertBefore(this, idx);
    this.eraseFromParent();
  }

  public boolean isTerminator() {
    return isTerminator(getOpcode());
  }

  public boolean isBinaryOp() {
    return isBinaryOp(getOpcode());
  }

  public boolean isShift() {
    return isShift(getOpcode());
  }

  public boolean isCast() {
    return isCast(getOpcode());
  }

  public String getOpcodeName() {
    return opcode.opName;
  }

  static boolean isTerminator(Operator opcode) {
    return opcode.ordinal() >= Ret.ordinal() && opcode.ordinal() <= Switch.ordinal();
  }

  static boolean isBinaryOp(Operator opcode) {
    return opcode.ordinal() >= Add.ordinal() && opcode.ordinal() <= AShr.ordinal();
  }

  /**
   * Determine if the Opcode is one of the shift instructions.
   *
   * @param Opcode
   * @return
   */
  static boolean isShift(Operator Opcode) {
    return Opcode.ordinal() >= Shl.ordinal()
        && Opcode.ordinal() <= AShr.ordinal();
  }

  /**
   * Return true if this is a logical shift left or a logical
   * shift right.
   *
   * @return
   */
  public boolean isLogicalShift() {
    return getOpcode() == Shl || getOpcode() == LShr;
  }

  /**
   * Return true if this is an arithmetic shift right.
   *
   * @return
   */
  public boolean isArithmeticShift() {
    return getOpcode() == AShr;
  }

  /**
   * Determine if the opcode is one of the CastInst instructions.
   *
   * @param opcode
   * @return
   */
  static boolean isCast(Operator opcode) {
    return opcode.ordinal() >= Trunc.ordinal()
        && opcode.ordinal() <= BitCast.ordinal();
  }

  /**
   * <pre>
   * Return true if the instruction is associative:
   *   Associative operators satisfy:  x op (y op z) === (x op y) op z
   * </pre>
   * In LLVM, the Add, Mul, And, Or, and Xor operators are associative, when
   * not applied to floating point types.
   *
   * @return
   */
  public boolean isAssociative() {
    return isAssociative(getOpcode(), getType());
  }

  private static boolean isAssociative(Operator op, Type ty) {
    if (ty == LLVMContext.Int1Ty || ty.equals(LLVMContext.Int8Ty) || ty.equals(
        LLVMContext.Int16Ty)
        || ty.equals(LLVMContext.Int32Ty) || ty.equals(
        LLVMContext.Int64Ty)) {
      switch (op) {
        case Add:
        case Mul:
        case And:
        case Or:
        case Xor:
          return true;
        default:
          return false;
      }
    } else
      return false;
  }

  /**
   * <pre>
   * Return true if the instruction is commutative:
   *
   *   Commutative operators satisfy: (x op y) === (y op x)
   * </pre>
   * In LLVM, these are the associative operators.
   *
   * @return
   */
  public boolean isCommutative() {
    return isCommutative(getOpcode());
  }

  private static boolean isCommutative(Operator op) {
    switch (op) {
      case Add:
      case Mul:
      case And:
      case Or:
      case Xor:
        return true;
      default:
        return false;
    }
  }

  public Operator getOpcode() {
    return opcode;
  }

  public void setOpcode(Operator opc) {
    opcode = opc;
  }

  /**
   * Obtains the basic block which holds this instruction.
   *
   * @return
   */
  public BasicBlock getParent() {
    return parent;
  }

  /**
   * Updates the basic block holds multiple instructions.
   *
   * @param bb
   */
  public void setParent(BasicBlock bb) {
    this.parent = bb;
  }

  public boolean isSafeToSpecutativelyExecute() {
    for (int i = 0, e = getNumOfOperands(); i < e; i++) {
      if (operand(i) instanceof Constant) {
        Constant cn = (Constant) operand(i);
        if (cn.canTrap()) return false;
      }
    }

    switch (getOpcode()) {
      default:
        return true;
      case UDiv:
      case URem:
        return operand(1) instanceof ConstantInt &&
            !((ConstantInt) operand(1)).isNullValue();
      case SDiv:
      case SRem:
        return operand(1) instanceof ConstantInt &&
            !((ConstantInt) operand(1)).isNullValue() &&
            !((ConstantInt) ((ConstantInt) operand(1))).isAllOnesValue();
      case Load: {
        if (((LoadInst) this).isVolatile())
          return false;
        if (operand(0) instanceof AllocationInst)
          return true;
        if (operand(0) instanceof GlobalVariable) {
          GlobalVariable gv = (GlobalVariable) operand(0);
          return !gv.hasExternalLinkage();
        }
        return false;
      }
      case Call:
        return false;
      case Alloca:
      case Malloc:
      case Phi:
      case Store:
      case Free:
      case Ret:
      case Br:
      case Switch:
      case Unreachable:
        return false; // Misc instructions which have effects
    }
  }

  /**
   * set the metadata of the specified kind to the specified node.
   * @param mdk
   * @param node
   */
  public void setMetadata(int mdk, MDNode node) {
    // TODO, fake 12/19/2018
  }

  /**
   * The abstract base class definition for unary operator.
   */
  public static class UnaryInstruction extends Instruction {
    private void initialize(Value op, User user) {
      reserve(1);
      setOperand(0, op, user);
    }

    /**
     * Constructs unary operation.
     *
     * @param ty     The inst ty of ret.
     * @param opcode The operator code for this instruction.
     * @param op     The sole LIROperand.
     */
    public UnaryInstruction(
        Type ty,
        Operator opcode,
        Value op,
        String name,
        Instruction insertBefore) {
      super(ty, opcode, name, insertBefore);
      initialize(op, this);
    }

    /**
     * Constructs unary operation.
     *
     * @param ty     The inst ty of ret.
     * @param opcode The operator code for this instruction.
     * @param op     The sole LIROperand.
     */
    public UnaryInstruction(
        Type ty,
        Operator opcode,
        Value op,
        String name) {
      this(ty, opcode, op, name, (Instruction) null);
    }

    /**
     * @param ty
     * @param opcode
     * @param op
     * @param name
     * @param insertAtEnd
     */
    public UnaryInstruction(Type ty,
                            Operator opcode,
                            Value op,
                            String name,
                            BasicBlock insertAtEnd) {
      super(ty, opcode, name, insertAtEnd);
      initialize(op, this);
    }

    @Override
    public int valueNumber() {
      return Util.hash1(getOpcode().index, operand(0));
    }
  }

  /**
   * This class just for binary operation definition.
   *
   * @author Jianping Zeng
   */
  public static class BinaryOps extends Instruction {
    /**
     * Indicates the operation (such as udiv, sdiv, lshr, and ashr)
     * doesn't have extra bits to been destroyed.
     */
    private boolean isExact;
    private boolean hasNoUnsignedWrap;
    private boolean hasNoSignedWrap;

    public BinaryOps(
        Type ty,
        Operator opcode,
        Value lhs,
        Value rhs,
        String name) {
      this(ty, opcode, lhs, rhs, name, (Instruction) null);
    }

    public BinaryOps(
        Type ty,
        Operator opcode,
        Value lhs,
        Value rhs,
        String name,
        Instruction insertBefore) {
      super(ty, opcode, name, insertBefore);
      reserve(2);
      Util.assertion(lhs.getType().equals(rhs.getType()),
          "Can not create binary operation with two operands of differing jlang.type.");

      init(lhs, rhs);
    }

    public BinaryOps(
        Type ty,
        Operator opcode,
        Value lhs,
        Value rhs,
        String name,
        BasicBlock insertAtEnd) {
      super(ty, opcode, name, insertAtEnd);
      Util.assertion(lhs.getType().equals(rhs.getType()),
          "Can not create binary operation with two operands of differing jlang.type.");

      init(lhs, rhs);
    }

    private void init(Value x, Value y) {
      setOperand(0, x, this);
      setOperand(1, y, this);
    }

    public static BinaryOps create(
        Operator op,
        Value lhs,
        Value rhs,
        String name,
        Instruction insertBefore) {
      Util.assertion(lhs.getType().equals(rhs.getType()),
          "Cannot create binary operator with two operands of differing type!");

      return new BinaryOps(lhs.getType(), op, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps create(
        Operator op,
        Value lhs,
        Value rhs,
        String name) {
      Util.assertion(lhs.getType() == rhs.getType(),
          "Cannot create binary operator with two operands of differing type!");

      return new BinaryOps(lhs.getType(), op, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps create(
        Operator op,
        Value lhs,
        Value rhs,
        String name,
        BasicBlock insertAtEnd) {
      Util.assertion(lhs.getType() == rhs.getType(),
          "Cannot create binary operator with two operands of differing type!");

      return new BinaryOps(lhs.getType(), op, lhs, rhs, name, insertAtEnd);
    }

    public void setIsExact(boolean val) {
      this.isExact = val;
    }

    //=====================================================================//
    //               The  first version with default insertBefore.         //
    public static BinaryOps createAdd(Value lhs, Value rhs, String name) {
      return create(Operator.Add, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createFAdd(Value lhs, Value rhs, String name) {
      return create(Operator.FAdd, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createSub(Value lhs, Value rhs, String name) {
      return create(Operator.Sub, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createFSub(Value lhs, Value rhs, String name) {
      return create(Operator.FSub, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createMul(Value lhs, Value rhs, String name) {
      return create(Operator.Mul, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createFMul(Value lhs, Value rhs, String name) {
      return create(Operator.FMul, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createUDiv(Value lhs, Value rhs, String name) {
      return create(Operator.UDiv, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createSDiv(Value lhs, Value rhs, String name) {
      return create(Operator.SDiv, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createFDiv(Value lhs, Value rhs, String name) {
      return create(Operator.FDiv, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createURem(Value lhs, Value rhs, String name) {
      return create(Operator.URem, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createSRem(Value lhs, Value rhs, String name) {
      return create(Operator.SRem, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createFRem(Value lhs, Value rhs, String name) {
      return create(Operator.FRem, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createShl(Value lhs, Value rhs, String name) {
      return create(Operator.Shl, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createLShr(Value lhs, Value rhs, String name) {
      return create(Operator.LShr, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createAShr(Value lhs, Value rhs, String name) {
      return create(Operator.AShr, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createAnd(Value lhs, Value rhs, String name) {
      return create(Operator.And, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createOr(Value lhs, Value rhs, String name) {
      return create(Operator.Or, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOps createXor(Value lhs, Value rhs, String name) {
      return create(Operator.Xor, lhs, rhs, name, (Instruction) null);
    }

    //=====================================================================//
    //                 The second version with insertAtEnd argument.       //
    public static BinaryOps createAdd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Add, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createFAdd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FAdd, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createSub(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Sub, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createFSub(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FSub, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createMul(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FMul, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createFMul(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FMul, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createUDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.UDiv, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createSDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.SDiv, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createFDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FDiv, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createURem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.URem, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createSRem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.SRem, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createFRem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FRem, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createShl(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Shl, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createLShr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.LShr, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createAShr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.AShr, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createAnd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.And, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createOr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Or, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOps createXor(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Xor, lhs, rhs, name, insertAtEnd);
    }


    //=====================================================================//
    //                   The third version with insertBefore argument.     //
    public static BinaryOps createAdd(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Add, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createFAdd(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FAdd, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createSub(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Sub, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createFSub(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FSub, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createMul(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FMul, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createFMul(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FMul, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createUDiv(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.UDiv, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createSDiv(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.SDiv, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createFDiv(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FDiv, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createURem(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.URem, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createSRem(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.SRem, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createFRem(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FRem, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createShl(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Shl, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createLShr(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.LShr, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createAShr(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.AShr, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createAnd(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.And, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createOr(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Or, lhs, rhs, name, insertBefore);
    }

    public static BinaryOps createXor(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Xor, lhs, rhs, name, insertBefore);
    }


    // ====================================================================//
    //   Some helper method for create unary operator with Bianry inst.    //
    public static BinaryOps createNeg(Value op, String name, Instruction insertBefore) {
      Value zero = ConstantInt.getNullValue(op.getType());
      return new BinaryOps(op.getType(), Sub, zero, op, name, insertBefore);
    }

    public static BinaryOps createNeg(Value op, String name, BasicBlock insertAtEnd) {
      Value zero = ConstantInt.getNullValue(op.getType());
      return new BinaryOps(op.getType(), Sub, zero, op, name, insertAtEnd);
    }

    public static BinaryOps createNeg(Value op) {
      Value zero = ConstantInt.getNullValue(op.getType());
      return new BinaryOps(op.getType(), Sub, zero, op, "");
    }

    public static BinaryOps createFNeg(Value op, String name, Instruction insertBefore) {
      Value zero = ConstantFP.getNullValue(op.getType());
      return new BinaryOps(op.getType(), FSub, zero, op, name, insertBefore);
    }

    public static BinaryOps createFNeg(Value op) {
      Value zero = ConstantInt.getNullValue(op.getType());
      return new BinaryOps(op.getType(), FSub, zero, op, "");
    }

    public static BinaryOps createFNeg(Value op, String name, BasicBlock insertAtEnd) {
      Value zero = ConstantFP.getNullValue(op.getType());
      return new BinaryOps(op.getType(), FSub, zero, op, name, insertAtEnd);
    }

    public static BinaryOps createNot(Value op, String name, Instruction insertBefore) {
      Constant one = Constant.getAllOnesValue(op.getType());
      return new BinaryOps(op.getType(), Xor, one, op, name, insertBefore);
    }

    public static BinaryOps createNot(Value op) {
      Constant one = Constant.getAllOnesValue(op.getType());
      return new BinaryOps(op.getType(), Xor, one, op, "");
    }

    public static BinaryOps createNot(Value op, String name, BasicBlock insertAtEnd) {
      Constant one = Constant.getAllOnesValue(op.getType());
      return new BinaryOps(op.getType(), Xor, one, op, name, insertAtEnd);
    }


    /**
     * This method is used for attempting to swap the two operands of this
     * binary instruction.
     *
     * @return Returns true if this binary operation is not commutative.
     */
    public boolean swapOperands() {
      if (!isCommutative())
        return true;
      Value temp = operand(0);
      setOperand(0, operand(1), this);
      setOperand(1, temp, this);
      return false;
    }

    @Override
    public int valueNumber() {
      return Util.hash2(getOpcode().index, operand(0), operand(1));
    }

    @Override
    public boolean equals(Object other) {
      if (other == null)
        return false;
      if (other == this)
        return true;
      if (!(other instanceof BinaryOps))
        return false;

      BinaryOps op = (BinaryOps) other;
      Value x = operand(0);
      Value y = operand(1);
      return getType() == op.getType() && getOpcode()
          .equals(op.getOpcode())
          && x.equals(op.operand(0))
          && y.equals(op.operand(1));
    }

    private static boolean isConstantAllOnes(Value val) {
      if (!(val instanceof ConstantInt))
        return false;
      ConstantInt ci = (ConstantInt) val;
      return ci.isAllOnesValue();
    }

    /**
     * Checks to see if the specified value is a binary operation that could be converted to
     * not operation.
     *
     * @param val
     * @return
     */
    public static boolean isNot(Value val) {
      if (val instanceof Instruction) {
        Instruction inst = (Instruction) val;
        if (inst.getOpcode() == Operator.Xor &&
            (isConstantAllOnes(inst.operand(1)) ||
                isConstantAllOnes(inst.operand(0))))
          return true;
      }
      return false;
    }

    public void setHasNoUnsignedWrap(boolean val) {
      this.hasNoUnsignedWrap = val;
    }

    public boolean getHasNoUnsignedWrap() {
      return hasNoUnsignedWrap;
    }

    public void setHasNoSignedWrap(boolean val) {
      this.hasNoSignedWrap = val;
    }

    public boolean getHasNoSignedWrap() {
      return hasNoSignedWrap;
    }
  }

  public static boolean isNeg(Value val) {
    if (val instanceof Instruction) {
      Instruction inst = (Instruction) val;
      if (inst.getOpcode() == Operator.Sub &&
          inst.operand(0).isNullConstant())
        return true;
    }
    return false;
  }

  public static boolean isFNeg(Value val) {
    if (val instanceof Instruction) {
      Instruction inst = (Instruction) val;
      if (inst.getOpcode() == Operator.FSub &&
          inst.operand(0).isNullConstant())
        return true;
    }
    return false;
  }

  public static class CastInst extends UnaryInstruction {
    protected CastInst(Type ty,
                       Operator opcode,
                       Value x,
                       String name) {
      super(ty, opcode, x, name);
    }

    protected CastInst(Type ty,
                       Operator opcode,
                       Value x,
                       String name,
                       Instruction insertBefore) {
      super(ty, opcode, x, name, insertBefore);
    }

    protected CastInst(Type ty,
                       Operator opcode,
                       Value x,
                       String name,
                       BasicBlock insertAtEnd) {
      super(ty, opcode, x, name, insertAtEnd);
    }

    public static CastInst createIntegerCast(
        Value value, Type destTy,
        boolean isSigned) {
      return createIntegerCast(value, destTy, isSigned, "", null);
    }

    public static CastInst createIntegerCast(
        Value value,
        Type destTy,
        boolean isSigned,
        String instName,
        Instruction insertBefore) {
      Util.assertion(value.getType().isIntegerType() && destTy.isIntegerType(), "Invalid type!");

      int srcBits = value.getType().getScalarSizeBits();
      int destBits = destTy.getScalarSizeBits();
      Operator opcode = srcBits == destBits ? BitCast
          : srcBits > destBits ? Trunc
          : (isSigned ? SExt : ZExt);

      return create(opcode, value, destTy, instName, insertBefore);
    }

    public static CastInst create(Operator opcode, Value value,
                                  Type ty, String name, Instruction insertBefore) {
      switch (opcode) {
        case Trunc:
          return new TruncInst(value, ty, name, insertBefore);
        case ZExt:
          return new ZExtInst(value, ty, name, insertBefore);
        case SExt:
          return new SExtInst(value, ty, name, insertBefore);
        case FPTrunc:
          return new FPTruncInst(value, ty, name, insertBefore);
        case FPExt:
          return new FPExtInst(value, ty, name, insertBefore);
        case UIToFP:
          return new UIToFPInst(value, ty, name, insertBefore);
        case SIToFP:
          return new SIToFPInst(value, ty, name, insertBefore);
        case FPToUI:
          return new FPToUIInst(value, ty, name, insertBefore);
        case FPToSI:
          return new FPToSIInst(value, ty, name, insertBefore);
        case PtrToInt:
          return new PtrToIntInst(value, ty, name, insertBefore);
        case IntToPtr:
          return new IntToPtrInst(value, ty, name, insertBefore);
        case BitCast:
          return new BitCastInst(value, ty, name, insertBefore);
        default:
          Util.assertion(false, "Invalid opcode provided!");
      }
      return null;
    }

    public static Operator getCastOpcode(Value val, boolean srcIsSigned,
                                         Type destTy, boolean destIsSigned) {
      Type srcTy = val.getType();
      int srcBits = srcTy.getScalarSizeBits();
      int destBits = destTy.getScalarSizeBits();

      Util.assertion(srcTy.isFirstClassType() && destTy.isFirstClassType(), "Only first class types are casted");


      if (destTy.isIntegral()) {
        if (srcTy.isIntegral()) {
          if (destBits < srcBits)
            return Operator.Trunc;
          else if (destBits > srcBits) {
            if (srcIsSigned)
              return Operator.SExt;
            else
              return Operator.ZExt;
          } else
            return Operator.BitCast;
        } else if (srcTy.isFloatingPointType()) {
          if (destIsSigned)
            return Operator.FPToSI;
          else
            return Operator.FPToUI;
        } else {
          Util.assertion(srcTy instanceof PointerType, "Casting from a value that is not first-class type");

          return Operator.PtrToInt;
        }
      } else if (destTy.isFloatingPointType()) {
        if (srcTy.isIntegral()) {
          if (srcIsSigned)
            return Operator.SIToFP;
          else
            return Operator.UIToFP;
        } else if (srcTy.isFloatingPointType()) {
          if (destBits > srcBits)
            return Operator.FPExt;
          else if (destBits < srcBits)
            return Operator.FPTrunc;
          else
            return Operator.BitCast;
        }
      } else if (destTy.isPointerType()) {
        if (srcTy.isPointerType())
          return Operator.BitCast;
        else if (srcTy.isIntegral())
          return Operator.IntToPtr;
        else
          Util.assertion(false, "Casting pointer to other than pointer type!");
      } else {
        Util.assertion(false, "Casting to type that is not first-class type!");
      }
      return Operator.BitCast;
    }

    /**
     * This method used for checking validate of casting a value to specified
     * destination type.
     *
     * @param opc
     * @param op
     * @param destTy
     * @return Return true if the specified cast operation is valid, otherwise
     * return false.
     */
    public static boolean castIsValid(Operator opc, Value op, Type destTy) {
      Type srcTy = op.getType();
      if (!srcTy.isFirstClassType() || !destTy.isFirstClassType())
        return false;

      int srcBits = srcTy.getScalarSizeBits();
      int destBits = destTy.getScalarSizeBits();
      switch (opc) {
        case Trunc:
          return srcTy.isInteger() && destTy.isInteger() &&
              srcBits > destBits;
        case SExt:
          return srcTy.isInteger() && destTy.isInteger() &&
              srcBits < destBits;
        case ZExt:
          return srcTy.isInteger() && destTy.isInteger() &&
              srcBits < destBits;
        case FPTrunc:
          return srcTy.isFloatingPointType() && destTy.isFloatingPointType()
              && srcBits > destBits;
        case FPExt:
          return srcTy.isFloatingPointType() && destTy.isFloatingPointType()
              && srcBits < destBits;
        case FPToSI:
        case FPToUI:
          return srcTy.isFloatingPointType() && destTy.isInteger();
        case SIToFP:
        case UIToFP:
          return srcTy.isInteger() && destTy.isFloatingPointType();
        case BitCast:
          // bit cast is no op in machine level, but we should check
          // both type is same when one is of type PointerType
          if ((srcTy instanceof PointerType) != (destTy instanceof PointerType))
            return false;

          return srcTy.getPrimitiveSizeInBits() == destTy.getPrimitiveSizeInBits();
        default:
          return false;   // input error
      }
    }
  }

  public static class UIToFPInst extends CastInst {
    public UIToFPInst(Value x, Type ty, String name) {
      super(ty, UIToFP, x, name);
    }

    public UIToFPInst(Value x, Type ty, String name,
                      Instruction insertBefore) {
      super(ty, UIToFP, x, name, insertBefore);
    }

    public UIToFPInst(Value x, Type ty, String name,
                      BasicBlock insertAtEnd) {
      super(ty, UIToFP, x, name, insertAtEnd);
    }
  }

  public static class SIToFPInst extends CastInst {
    public SIToFPInst(Value x, Type ty, String name) {
      super(ty, SIToFP, x, name);
    }

    public SIToFPInst(Value x, Type ty, String name,
                      Instruction insertBefore) {
      super(ty, SIToFP, x, name, insertBefore);
    }

    public SIToFPInst(Value x, Type ty, String name,
                      BasicBlock insertAtEnd) {
      super(ty, SIToFP, x, name, insertAtEnd);
    }
  }

  public static class TruncInst extends CastInst {
    public TruncInst(Value x, Type ty, String name) {
      super(ty, Trunc, x, name);
    }

    public TruncInst(Value x, Type ty, String name,
                     Instruction insertBefore) {
      super(ty, Trunc, x, name, insertBefore);
    }

    public TruncInst(Value x, Type ty, String name,
                     BasicBlock insertAtEnd) {
      super(ty, Trunc, x, name, insertAtEnd);
    }
  }

  public static class ZExtInst extends CastInst {
    public ZExtInst(Value x, Type ty, String name) {
      super(ty, ZExt, x, name);
    }

    public ZExtInst(Value x, Type ty, String name,
                    Instruction insertBefore) {
      super(ty, ZExt, x, name, insertBefore);
    }

    public ZExtInst(Value x, Type ty, String name,
                    BasicBlock insertAtEnd) {
      super(ty, ZExt, x, name, insertAtEnd);
    }
  }

  public static class SExtInst extends CastInst {
    public SExtInst(Value x, Type ty, String name) {
      super(ty, SExt, x, name);
    }

    public SExtInst(Value x, Type ty, String name,
                    Instruction insertBefore) {
      super(ty, SExt, x, name, insertBefore);
    }

    public SExtInst(Value x, Type ty, String name,
                    BasicBlock insertAtEnd) {
      super(ty, SExt, x, name, insertAtEnd);
    }
  }

  public static class FPTruncInst extends CastInst {
    public FPTruncInst(Value x, Type ty, String name) {
      super(ty, FPTrunc, x, name);
    }

    public FPTruncInst(Value x, Type ty, String name,
                       Instruction insertBefore) {
      super(ty, FPTrunc, x, name, insertBefore);
    }

    public FPTruncInst(Value x, Type ty, String name,
                       BasicBlock insertAtEnd) {
      super(ty, FPTrunc, x, name, insertAtEnd);
    }
  }

  public static class FPExtInst extends CastInst {
    public FPExtInst(Value x, Type ty, String name) {
      super(ty, FPExt, x, name);
    }

    public FPExtInst(Value x, Type ty, String name,
                     Instruction insertBefore) {
      super(ty, FPExt, x, name, insertBefore);
    }

    public FPExtInst(Value x, Type ty, String name,
                     BasicBlock insertAtEnd) {
      super(ty, FPExt, x, name, insertAtEnd);
    }
  }

  public static class FPToUIInst extends CastInst {
    public FPToUIInst(Value x, Type ty, String name) {
      super(ty, FPToUI, x, name);
    }

    public FPToUIInst(Value x, Type ty, String name,
                      Instruction insertBefore) {
      super(ty, FPToUI, x, name, insertBefore);
    }

    public FPToUIInst(Value x, Type ty, String name,
                      BasicBlock insertAtEnd) {
      super(ty, FPToUI, x, name, insertAtEnd);
    }
  }

  public static class FPToSIInst extends CastInst {
    public FPToSIInst(Value x, Type ty, String name) {
      super(ty, FPToSI, x, name);
    }

    public FPToSIInst(Value x, Type ty, String name,
                      Instruction insertBefore) {
      super(ty, FPToSI, x, name, insertBefore);
    }

    public FPToSIInst(Value x, Type ty, String name,
                      BasicBlock insertAtEnd) {
      super(ty, FPToSI, x, name, insertAtEnd);
    }
  }

  public static class PtrToIntInst extends CastInst {
    public PtrToIntInst(Value x, Type ty, String name) {
      super(ty, PtrToInt, x, name);
    }

    public PtrToIntInst(Value x, Type ty, String name,
                        Instruction insertBefore) {
      super(ty, PtrToInt, x, name, insertBefore);
    }

    public PtrToIntInst(Value x, Type ty, String name,
                        BasicBlock insertAtEnd) {
      super(ty, PtrToInt, x, name, insertAtEnd);
    }
  }

  public static class IntToPtrInst extends CastInst {
    public IntToPtrInst(Value x, Type ty, String name) {
      super(ty, IntToPtr, x, name);
    }

    public IntToPtrInst(Value x, Type ty, String name,
                        Instruction insertBefore) {
      super(ty, IntToPtr, x, name, insertBefore);
    }

    public IntToPtrInst(Value x, Type ty, String name,
                        BasicBlock insertAtEnd) {
      super(ty, IntToPtr, x, name, insertAtEnd);
    }
  }

  public static class BitCastInst extends CastInst {
    public BitCastInst(Value x, Type ty, String name) {
      super(ty, BitCast, x, name);
    }

    public BitCastInst(Value x, Type ty, String name,
                       Instruction insertBefore) {
      super(ty, BitCast, x, name, insertBefore);
    }

    public BitCastInst(Value x, Type ty, String name,
                       BasicBlock insertAtEnd) {
      super(ty, BitCast, x, name, insertAtEnd);
    }
  }

  /**
   * This is a base class for the comparison instructions.
   */
  public static abstract class CmpInst extends Instruction {
    protected Predicate pred;

    protected CmpInst(
        Type ty,
        Operator op,
        Predicate pred,
        Value lhs,
        Value rhs,
        String name,
        Instruction insertBefore) {
      super(ty, op, name, insertBefore);
      init(pred, lhs, rhs);
    }

    protected CmpInst(
        Type ty,
        Operator op,
        Predicate pred,
        Value lhs,
        Value rhs,
        String name,
        BasicBlock insertAtEnd) {
      super(ty, op, name, insertAtEnd);
      init(pred, lhs, rhs);
    }

    private void init(Predicate pred, Value lhs, Value rhs) {
      reserve(2);
      setOperand(0, lhs, this);
      setOperand(1, rhs, this);
      this.pred = pred;
    }

    public Predicate getPredicate() {
      return pred;
    }

    public void setPredicate(Predicate newPred) {
      pred = newPred;
    }

    public Predicate getInversePredicate() {
      return getInversePredicate(pred);
    }

    public Predicate getSwappedPredicate() {
      return getSwappedPredicate(pred);
    }

    public static Predicate getInversePredicate(Predicate pred) {
      switch (pred) {
        default:
          Util.assertion(false, "Undefined cmp predicate!");
        case ICMP_EQ:
          return ICMP_NE;
        case ICMP_NE:
          return ICMP_EQ;
        case ICMP_UGT:
          return ICMP_ULE;
        case ICMP_ULT:
          return ICMP_UGE;
        case ICMP_ULE:
          return ICMP_UGT;
        case ICMP_UGE:
          return ICMP_ULT;
        case ICMP_SGT:
          return ICMP_SLE;
        case ICMP_SLT:
          return ICMP_SGE;
        case ICMP_SGE:
          return ICMP_SLT;
        case ICMP_SLE:
          return ICMP_SGT;

        case FCMP_OEQ:
          return FCMP_ONE;
        case FCMP_ONE:
          return FCMP_OEQ;
        case FCMP_OGT:
          return FCMP_OLE;
        case FCMP_OLT:
          return FCMP_OGE;
        case FCMP_OLE:
          return FCMP_OGT;
        case FCMP_OGE:
          return FCMP_OLT;
        case FCMP_UEQ:
          return FCMP_UNE;
        case FCMP_UNE:
          return FCMP_UEQ;
        case FCMP_UGT:
          return FCMP_ULE;
        case FCMP_ULT:
          return FCMP_UGE;
        case FCMP_UGE:
          return FCMP_ULT;
        case FCMP_ULE:
          return FCMP_UGT;
        case FCMP_ORD:
          return FCMP_UNO;
        case FCMP_UNO:
          return FCMP_ORD;
        case FCMP_TRUE:
          return FCMP_FALSE;
        case FCMP_FALSE:
          return FCMP_TRUE;
      }
    }

    public static Predicate getSwappedPredicate(Predicate pred) {
      switch (pred) {
        default:
          Util.assertion(false, "Undefined cmp predicate!");
        case ICMP_EQ:
        case ICMP_NE:
        case FCMP_FALSE:
        case FCMP_TRUE:
        case FCMP_OEQ:
        case FCMP_ONE:
        case FCMP_UEQ:
        case FCMP_UNE:
        case FCMP_ORD:
        case FCMP_UNO:
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

    public boolean isCommutative() {
      if (this instanceof ICmpInst)
        return ((ICmpInst) this).isCommutative();
      return ((FCmpInst) this).isCommutative();
    }

    public boolean isEquality() {
      if (this instanceof ICmpInst)
        return ((ICmpInst) this).isEquality();
      return ((FCmpInst) this).isEquality();
    }

    public boolean isRelational() {
      if (this instanceof ICmpInst)
        return ((ICmpInst) this).isRelational();
      return ((FCmpInst) this).isRelational();
    }

    public void swapOperands() {
      if (this instanceof ICmpInst)
        ((ICmpInst) this).swapOperands();
      else
        ((FCmpInst) this).swapOperands();
    }

    public static boolean isUnsigned(Predicate pred) {
      switch (pred) {
        default:
          return false;
        case ICMP_ULT:
        case ICMP_ULE:
        case ICMP_UGT:
        case ICMP_UGE:
          return true;
      }
    }

    public static boolean isSigned(Predicate pred) {
      switch (pred) {
        default:
          return false;
        case ICMP_SLT:
        case ICMP_SLE:
        case ICMP_SGT:
        case ICMP_SGE:
          return true;
      }
    }

    public static boolean isOrdered(Predicate pred) {
      switch (pred) {
        default:
          return false;
        case FCMP_OEQ:
        case FCMP_ONE:
        case FCMP_OGT:
        case FCMP_OLT:
        case FCMP_OGE:
        case FCMP_OLE:
        case FCMP_ORD:
          return true;
      }
    }

    public static boolean isUnOrdered(Predicate pred) {
      switch (pred) {
        default:
          return false;
        case FCMP_UEQ:
        case FCMP_UNE:
        case FCMP_UGT:
        case FCMP_ULT:
        case FCMP_UGE:
        case FCMP_ULE:
        case FCMP_UNO:
          return true;
      }
    }

    public static CmpInst create(Operator opcode, Predicate predicate,
                                 Value newOp1, Value newOp2,
                                 String name, Instruction insertBefore) {
      if (opcode == ICmp) {
        return new ICmpInst(predicate, newOp1, newOp2, name, insertBefore);
      } else {
        return new FCmpInst(predicate, newOp1, newOp2, name,
            insertBefore);
      }
    }

    /**
     * This enumeration lists the possible predicates for CmpInst subclasses.
     * Values in the range 0-31 are reserved for FCmpInst, while values in the
     * range 32-64 are reserved for ICmpInst. This is necessary to ensure the
     * predicate values are not overlapping between the classes.
     */
    public enum Predicate {
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
      ICMP_EQ,  /// equal
      ICMP_NE,  /// not equal
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
  public static class FCmpInst extends CmpInst {

    public FCmpInst(Predicate pred, Value lhs,
                    Value rhs, String name, Instruction insertBefore) {
      super(LLVMContext.Int1Ty, FCmp, pred, lhs, rhs, name, insertBefore);
      Util.assertion(pred.compareTo(Predicate.LAST_FCMP_PREDICATE) <= 0, "Invalid FCmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to FCmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isFloatingPointType(), "Invalid operand types for FCmp instruction");

    }

    public FCmpInst(Predicate pred, Value lhs,
                    Value rhs, String name, BasicBlock insertAtEnd) {
      super(LLVMContext.Int1Ty, FCmp, pred, lhs, rhs, name, insertAtEnd);
      Util.assertion(pred.compareTo(Predicate.LAST_FCMP_PREDICATE) <= 0, "Invalid FCmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to FCmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isFloatingPointType(), "Invalid operand types for FCmp instruction");

    }

    public FCmpInst(Predicate pred, Value lhs, Value rhs) {
      super(LLVMContext.Int1Ty, FCmp, pred, lhs, rhs, "", (Instruction) null);
      Util.assertion(pred.compareTo(Predicate.LAST_FCMP_PREDICATE) <= 0, "Invalid FCmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to FCmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isFloatingPointType(), "Invalid operand types for FCmp instruction");

    }

    public FCmpInst(Predicate pred, Value lhs, Value rhs, String name) {
      super(LLVMContext.Int1Ty, FCmp, pred, lhs, rhs, name, (Instruction) null);
      Util.assertion(pred.compareTo(Predicate.LAST_FCMP_PREDICATE) <= 0, "Invalid FCmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to FCmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isFloatingPointType(), "Invalid operand types for FCmp instruction");

    }

    public boolean isEquality() {
      return pred == FCMP_OEQ || pred == FCMP_ONE ||
          pred == FCMP_UEQ || pred == FCMP_UNE;
    }

    public boolean isCommutative() {
      return isEquality() ||
          pred == FCMP_FALSE ||
          pred == FCMP_TRUE ||
          pred == FCMP_ORD ||
          pred == FCMP_UNO;
    }

    @Override
    public boolean isRelational() {
      return !isEquality();
    }

    @Override
    public void swapOperands() {
      pred = getSwappedPredicate();
      Use u = operandList.get(0);
      operandList.set(0, operandList.get(1));
      operandList.set(1, u);
    }
  }

  /**
   * This instruction compares its operands according to the predicate given
   * to the constructor. It only operates on integers or pointers. The operands
   * must be identical types.
   */
  public static class ICmpInst extends CmpInst {
    public ICmpInst(Predicate pred, Value lhs,
                    Value rhs, String name, Instruction insertBefore) {
      super(LLVMContext.Int1Ty, ICmp, pred, lhs, rhs, name, insertBefore);
      Util.assertion(pred.compareTo(Predicate.LAST_ICMP_PREDICATE) <= 0, "Invalid ICmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to ICmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isIntegerType() || lhs.getType() instanceof PointerType, "Invalid operand types for ICmp instruction");

    }

    public ICmpInst(Predicate pred, Value lhs,
                    Value rhs, String name, BasicBlock insertAtEnd) {
      super(LLVMContext.Int1Ty, ICmp, pred, lhs, rhs, name, insertAtEnd);
      Util.assertion(pred.compareTo(Predicate.LAST_ICMP_PREDICATE) <= 0, "Invalid ICmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to ICmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isIntegerType(), "Invalid operand types for ICmp instruction");

    }

    public ICmpInst(Predicate pred, Value lhs,
                    Value rhs) {
      super(LLVMContext.Int1Ty, ICmp, pred, lhs, rhs, "", (Instruction) null);
      Util.assertion(pred.compareTo(Predicate.LAST_ICMP_PREDICATE) <= 0, "Invalid ICmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to ICmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isIntegerType(), "Invalid operand types for ICmp instruction");

    }

    public ICmpInst(Predicate pred, Value lhs,
                    Value rhs, String name) {
      super(LLVMContext.Int1Ty, ICmp, pred, lhs, rhs, name, (Instruction) null);
      Util.assertion(pred.compareTo(Predicate.LAST_ICMP_PREDICATE) <= 0, "Invalid ICmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to ICmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isIntegerType() || lhs.getType().isPointerType(), "Invalid operand types for ICmp instruction");

    }

    public static Predicate getSignedPredicate(Predicate pred) {
      switch (pred) {
        default:
          Util.assertion(false, "Undefined icmp predicate!");
        case ICMP_EQ:
        case ICMP_NE:
        case ICMP_SGT:
        case ICMP_SLT:
        case ICMP_SGE:
        case ICMP_SLE:
          return pred;
        case ICMP_UGT:
          return ICMP_SGT;
        case ICMP_ULT:
          return ICMP_SLT;
        case ICMP_UGE:
          return ICMP_SGE;
        case ICMP_ULE:
          return ICMP_SLE;
      }
    }

    public Predicate getSignedPredicate() {
      return getSignedPredicate(pred);
    }

    public static Predicate getUnsignedPredicate(Predicate pred) {
      switch (pred) {
        default:
          Util.assertion(false, "Undefined icmp predicate!");
        case ICMP_EQ:
        case ICMP_NE:
        case ICMP_UGT:
        case ICMP_ULT:
        case ICMP_UGE:
        case ICMP_ULE:
          return pred;
        case ICMP_SGT:
          return ICMP_UGT;
        case ICMP_SLT:
          return ICMP_ULT;
        case ICMP_SGE:
          return ICMP_UGE;
        case ICMP_SLE:
          return ICMP_ULE;
      }
    }

    @Override
    public boolean isEquality() {
      return pred == ICMP_EQ || pred == ICMP_NE;
    }

    @Override
    public boolean isCommutative() {
      return isEquality();
    }

    @Override
    public boolean isRelational() {
      return !isEquality();
    }

    public boolean isSignedPredicate() {
      return isSignedPredicate(pred);
    }

    public static boolean isSignedPredicate(Predicate pred) {
      switch (pred) {
        default:
          Util.assertion(false, ("Undefined icmp predicate!"));
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
    public void swapOperands() {
      pred = getSwappedPredicate();
      Use u = operandList.get(0);
      operandList.set(0, operandList.get(1));
      operandList.set(1, u);
    }

    /**
     * Return true if the specified compare predicate is
     * true when both operands are equal
     *
     * @param pred
     * @return
     */
    public static boolean isTrueWhenEqual(Predicate pred) {
      return pred == ICMP_EQ || pred == ICMP_UGE
          || pred == ICMP_SGE || pred == ICMP_ULE
          || pred == ICMP_SLE;
    }

    public boolean isTrueWhenEqual() {
      return isTrueWhenEqual(getPredicate());
    }
  }

  /**
   * TerminatorInst - Subclasses of this class are all able to terminate
   * a basic block.  Thus, these are all the flow control jlang.type of operations.
   *
   * @author Jianping Zeng
   * @version 0.4
   */
  public static abstract class TerminatorInst extends Instruction {
    TerminatorInst(
        Operator opcode,
        String instName,
        Instruction insertBefore) {
      super(LLVMContext.VoidTy, opcode, instName, insertBefore);
    }

    TerminatorInst(Type ty,
                   Operator opcode,
                   String instName,
                   Instruction insertBefore) {
      super(ty, opcode, instName, insertBefore);
    }


    TerminatorInst(Operator opcode,
                   String instName,
                   BasicBlock insertAtEnd) {
      super(LLVMContext.VoidTy, opcode, instName, insertAtEnd);
    }

    /**
     * obtains the successor at specified index position.
     *
     * @param index
     * @return
     */
    public abstract BasicBlock getSuccessor(int index);

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
    public abstract void setSuccessor(int index, BasicBlock bb);
  }

  /**
   * An abstract representation of branch instruction.
   *
   * @author Jianping Zeng
   */
  public final static class BranchInst extends TerminatorInst {
    /**
     * Constructs a unconditional Branch instruction.
     * BranchInst(BasicBlock parent) - 'br B'
     *
     * @param ifTrue       the branch TargetData.
     * @param insertBefore
     */
    public BranchInst(BasicBlock ifTrue, Instruction insertBefore) {
      super(Operator.Br, "", insertBefore);
      reserve(1);
      setOperand(0, ifTrue, this);
    }

    public BranchInst(BasicBlock ifTrue, String name, Instruction insertBefore) {
      super(Operator.Br, name, insertBefore);
      reserve(1);
      setOperand(0, ifTrue, this);
    }

    /**
     * Constructs a branch instruction.
     * <p>
     * BranchInst(BasicBlock parent) - 'br B'
     *
     * @param ifTrue the TargetData of this branch.
     */
    public BranchInst(BasicBlock ifTrue) {
      this(ifTrue, (Instruction) null);
    }

    public BranchInst(BasicBlock ifTrue, String name) {
      this(ifTrue, name, (Instruction) null);
    }

    /**
     * BranchInst(BB* T, BB *F, Value *C, Inst *I) - 'br C, T, F', insert before I
     *
     * @param ifTrue
     * @param ifFalse
     * @param cond
     */
    public BranchInst(BasicBlock ifTrue, BasicBlock ifFalse, Value cond) {
      this(ifTrue, ifFalse, cond, (Instruction) null);
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
                      Instruction insertBefore) {
      super(Operator.Br, "", insertBefore);
      reserve(3);
      setOperand(0, ifTrue, this);
      setOperand(1, ifFalse, this);
      setOperand(2, cond, this);
    }

    public BranchInst(BasicBlock ifTrue, BasicBlock ifFalse, Value cond,
                      BasicBlock insertAtEnd) {
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
    public BranchInst(BasicBlock ifTrue, BasicBlock insertAtEnd) {
      super(Operator.Br, "", insertAtEnd);
      setOperand(0, ifTrue, this);
    }

    public boolean isUnconditional() {
      return getNumOfOperands() == 1;
    }

    public boolean isConditional() {
      return getNumOfOperands() == 3;
    }

    public Value getCondition() {
      Util.assertion((isConditional()), "can not get a condition of uncondition branch");
      return operand(2);
    }

    public void setCondition(Value cond) {
      Util.assertion((cond != null), "can not update condition with null");
      Util.assertion((isConditional()), "can not set condition of uncondition branch");
      setOperand(2, cond, this);
    }

    public BranchInst clone() {
      if (isConditional())
        return new BranchInst(getSuccessor(0), getSuccessor(1), getCondition());
      else
        return new BranchInst(getSuccessor(0), getName());
    }

    /**
     * obtains the successors at specified position.
     *
     * @param index
     * @return
     */
    @Override
    public BasicBlock getSuccessor(int index) {
      Util.assertion((index >= 0 && index < getNumOfSuccessors()));
      return (BasicBlock) operand(index);
    }

    /**
     * obtains the number of successors of this branch instruction.
     *
     * @return
     */
    @Override
    public int getNumOfSuccessors() {
      return isConditional() ? 2 : 1;
    }

    @Override
    public void setSuccessor(int index, BasicBlock bb) {
      Util.assertion((index >= 0 && index < getNumOfSuccessors() && bb != null));
      setOperand(index, bb, this);
    }

    /**
     * Swaps the successor of the branch instruction.
     */
    public void swapSuccessor() {
      Util.assertion(isConditional(), "can not swap successor of uncondition branch");
      {
        Value temp = operand(0);
        setOperand(0, operand(1));
        setOperand(1, temp);
      }
    }

    /**
     * Change the current branch to an unconditional branch targetting the
     * specified block.
     *
     * @param dest
     */
    public void setUnconditionalDest(BasicBlock dest) {
      ArrayList<Use> list = new ArrayList<>();
      list.add(getOperand(0));
      operandList = list;
      setOperand(0, dest);
    }
  }

  /**
   * This {@code ReturnInst} class definition.
   * ReturnStmt a value (possibly void), from a function.
   * Execution does not continue in this function any longer.
   *
   * @author Jianping Zeng
   */
  public static class ReturnInst extends TerminatorInst {
    public ReturnInst() {
      this(null, "", (Instruction) null);
    }

    public ReturnInst(Value val) {
      this(val, "", (Instruction) null);
    }

    /**
     * Constructs a new return instruction with return inst.
     *
     * @param retValue The return inst produce for this instruction, return
     *                 void if ret is {@code null}.
     */
    public ReturnInst(Value retValue, String name, Instruction insertBefore) {
      super(Operator.Ret, name, insertBefore);
      if (retValue != null) {
        reserve(1);
        setOperand(0, retValue, this);
      }
    }

    public ReturnInst(Value retValue, String name, BasicBlock insertAtEnd) {
      super(Operator.Ret, name, insertAtEnd);
      if (retValue != null) {
        reserve(1);
        setOperand(0, retValue, this);
      }
    }

    /**
     * Gets the instruction that produces the ret for the return.
     *
     * @return the instruction producing the ret
     */
    public Value getReturnValue() {
      return getNumOfOperands() != 0 ? operand(0) : null;
    }

    @Override
    public BasicBlock getSuccessor(int index) {
      Util.assertion(true, "ReturnInst has no successors!");
      return null;
    }

    @Override
    public int getNumOfSuccessors() {
      return 0;
    }

    @Override
    public void setSuccessor(int index, BasicBlock bb) {
      Util.assertion(true, ("ReturnInst has no successors!"));
    }
  }

  /**
   * Immediately exit the current function, unwinding the stack
   * until an invoke instruction is found.
   */
  public static class UnWindInst extends TerminatorInst {

    public UnWindInst(Instruction insertBefore) {
      super(Unwind, "", insertBefore);
    }
    public UnWindInst() {
      this((Instruction)null);
    }

    public UnWindInst(BasicBlock insertAtEnd) {
      super(Unwind, "", insertAtEnd);
    }

    @Override
    public BasicBlock getSuccessor(int index) {
      Util.shouldNotReachHere("UnWindInst doesn't have successor!");
      return null;
    }

    @Override
    public int getNumOfSuccessors() {
      return 0;
    }

    @Override
    public void setSuccessor(int index, BasicBlock bb) {
      Util.shouldNotReachHere("UnWindInst doesn't have successor!");
    }
  }

  /**
   * FunctionProto invocation instruction.
   *
   * @author Jianping Zeng
   */
  public static class CallInst extends Instruction {
    // Returns the operand number of the first argument
    private final int ArgumentOffset = 1;
    private CallingConv callingConv;
    private boolean tailCall;
    private AttrList attributes;

    public CallInst(Value[] args, Value target) {
      this(args, target, "");
    }

    public CallInst(Value callee, List<Value> args) {
      this(callee, args, "");
    }

    public CallInst(Value callee, List<Value> args,
                    String name) {
      this(callee, args, name, null);
    }

    public CallInst(Value callee, List<Value> args,
                    String name, Instruction insertBefore) {
      super(((FunctionType) ((PointerType) callee.getType()).
              getElementType()).getReturnType(),
          Operator.Call, name, insertBefore);

      reserve(ArgumentOffset + args.size());
      Util.assertion((getNumOfOperands() == ArgumentOffset + args.size()), "NumOperands not set up?");

      setOperand(0, callee, this);
      int idx = ArgumentOffset;
      for (Value arg : args) {
        setOperand(idx++, arg, this);
      }
    }

    /**
     * Constructs a new method calling instruction.
     *
     * @param args   The input arguments.
     * @param target The called method.
     */
    public CallInst(Value[] args, Value target, String name) {
      this(args, target, name, (Instruction) null);
    }

    /**
     * Constructs a new method calling instruction.
     *
     * @param args   The input arguments.
     * @param target The called method.
     */
    public CallInst(Value[] args, Value target,
                    String name, Instruction insertBefore) {
      super(((FunctionType) ((PointerType) target.getType()).
              getElementType()).getReturnType(),
          Operator.Call, name, insertBefore);
      init(target, args);
    }

    public CallInst(Value[] args, Value target,
                    String name, BasicBlock insertAtEnd) {
      super(((FunctionType) ((PointerType) target.getType()).
              getElementType()).getReturnType(),
          Operator.Call, name, insertAtEnd);
      init(target, args);
    }


    private void init(Value target, Value[] args) {
      reserve(ArgumentOffset + args.length);
      Util.assertion((getNumOfOperands() == ArgumentOffset + args.length), "NumOperands not set up?");

      Util.assertion(target instanceof Function, "Callee is not Function?");
      Function fn = (Function) target;
      setOperand(0, target, this);
      int idx = ArgumentOffset;
      for (Value arg : args) {
        setOperand(idx++, arg, this);
      }
      callingConv = fn.getCallingConv();
      attributes = fn.getAttributes();
    }

    public int getNumsOfArgs() {
      return getNumOfOperands() - ArgumentOffset;
    }

    public void setArgument(int index, Value val) {
      Util.assertion(index >= 0 && index < getNumsOfArgs());
      setOperand(index + ArgumentOffset, val, this);
    }

    public Value argumentAt(int index) {
      Util.assertion(index >= 0 && index < getNumsOfArgs());
      return operand(index + ArgumentOffset);
    }

    /**
     * Return the called function, if return null indicates this is an indirect call.
     * @return
     */
    public Function getCalledFunction() {
      return operand(0) instanceof Function ? (Function) operand(0) : null;
    }

    public Value getCalledValue() {
      return operand(0);
    }

    public static CallSite get(Value val) {
      if (val instanceof CallInst) {
        return new CallSite((CallInst) val);
      }
      return new CallSite();
    }

    public CallingConv getCallingConv() {
      return callingConv;
    }

    public void setCallingConv(CallingConv callingConv) {
      this.callingConv = callingConv;
    }

    public void setTailCall(boolean tailCall) {
      this.tailCall = tailCall;
    }

    public void setAttributes(AttrList attributes) {
      this.attributes = attributes;
    }

    public AttrList getAttributes() {
      return attributes;
    }

    public boolean hasFnAttr(int n) {
      return attributes.paramHasAttr(0, n);
    }

    public void addFnAttr(int n) {
      addAttribute(0, n);
    }

    public void removeFnAttr(int n) {
      removeAttribute(0, n);
    }

    private void addAttribute(int index, int attr) {
      // TODO: 2017/11/27
    }

    private void removeAttribute(int index, int attr) {
      // TODO: 2017/11/27
    }

    public boolean paramHasAttr(int i, int attr) {
      return attributes.paramHasAttr(i, attr);
    }

    public int getParamAlignment(int index) {
      return attributes.getParamAlignment(index);
    }

    /// @brief Determine if the function does not access memory.
    public boolean doesNotAccessMemory() {
      return hasFnAttr(Attribute.ReadNone);
    }

    public void setDoesNotAccessMemory() {
      setDoesNotAccessMemory(true);
    }

    public void setDoesNotAccessMemory(boolean doesNotAccessMemory) {
      if (doesNotAccessMemory)
        addFnAttr(Attribute.ReadNone);
      else
        removeFnAttr(Attribute.ReadNone);
    }

    /// @brief Determine if the function does not access or only reads memory.
    public boolean onlyReadsMemory() {
      return doesNotAccessMemory() || hasFnAttr(Attribute.ReadOnly);
    }

    public void setOnlyReadsMemory() {
      setOnlyReadsMemory(true);
    }

    public void setOnlyReadsMemory(boolean OnlyReadsMemory) {
      if (OnlyReadsMemory)
        addFnAttr(Attribute.ReadOnly);
      else
        removeFnAttr(Attribute.ReadOnly | Attribute.ReadNone);
    }

    /// @brief Determine if the function cannot return.
    public boolean doesNotReturn() {
      return hasFnAttr(Attribute.NoReturn);
    }

    public void setDoesNotReturn() {
      setDoesNotReturn(true);
    }

    public void setDoesNotReturn(boolean DoesNotReturn) {
      if (DoesNotReturn)
        addFnAttr(Attribute.NoReturn);
      else
        removeFnAttr(Attribute.NoReturn);
    }

    /// @brief Determine if the function cannot unwind.
    public boolean doesNotThrow() {
      return hasFnAttr(Attribute.NoUnwind);
    }

    public void setDoesNotThrow() {
      setDoesNotThrow(true);
    }

    public void setDoesNotThrow(boolean DoesNotThrow) {
      if (DoesNotThrow)
        addFnAttr(Attribute.NoUnwind);
      else
        removeFnAttr(Attribute.NoUnwind);
    }

    /// @brief Determine if the function returns a structure through first
    /// pointer argument.
    public boolean hasStructRetAttr() {
      return paramHasAttr(1, Attribute.StructRet);
    }

    /// @brief Determine if the parameter does not alias other parameters.
    /// @param n The parameter to check. 1 is the first parameter, 0 is the return
    public boolean doesNotAlias(int n) {
      return paramHasAttr(n, Attribute.NoAlias);
    }

    public void setDoesNotAlias(int n) {
      setDoesNotAlias(n, true);
    }

    public void setDoesNotAlias(int n, boolean DoesNotAlias) {
      if (DoesNotAlias)
        addAttribute(n, Attribute.NoAlias);
      else
        removeAttribute(n, Attribute.NoAlias);
    }

    /// @brief Determine if the parameter can be captured.
    /// @param n The parameter to check. 1 is the first parameter, 0 is the return
    public boolean doesNotCapture(int n) {
      return paramHasAttr(n, Attribute.NoCapture);
    }

    public void setDoesNotCapture(int n) {
      setDoesNotCapture(n, true);
    }

    public void setDoesNotCapture(int n, boolean DoesNotCapture) {
      if (DoesNotCapture)
        addAttribute(n, Attribute.NoCapture);
      else
        removeAttribute(n, Attribute.NoCapture);
    }

    public boolean isTailCall() {
      return tailCall;
    }
  }

  public static class SwitchInst extends TerminatorInst {
    private int lowKey, highKey;
    private final int offset = 2;
    private int numOps;

    /**
     * Constructs a new SwitchInst instruction with specified inst jlang.type.
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
                      String name) {
      this(condV, defaultBB, numCases, name, null);
    }

    /**
     * Constructs a new SwitchInst instruction with specified inst jlang.type.
     *
     * @param condV        the value of selector.
     * @param defaultBB    The default jump block when no other case match.
     * @param insertBefore
     */
    public SwitchInst(Value condV,
                      BasicBlock defaultBB,
                      int numCases,
                      String name,
                      Instruction insertBefore) {
      super(Operator.Switch, name, insertBefore);
      init(condV, defaultBB, numCases);
    }


    /**
     * Initialize some arguments, like add switch value and default into
     * Operand list.
     */
    private void init(Value cond, BasicBlock defaultBB, int numCases) {
      // the 2 indicates what number of default basic block and default value.
      reserve(offset + numCases);
      setOperand(0, cond, this);
      setOperand(1, defaultBB, this);
      numOps = 2;
    }

    public int getNumOfOperands() {
      return numOps;
    }

    public void addCase(Constant caseVal, BasicBlock targetBB) {
      int opNo = getNumOfCases();
      setOperand(opNo*2, caseVal, this);
      setOperand(opNo*2 + 1, targetBB, this);
      numOps += 2;
    }

    public void removeCase(int idx) {
      Util.assertion((idx != 0), "Cannot remove the default case!");
      Util.assertion((idx * 2 < getNumOfOperands()), "Successor index out of range!!!");

      // unlink the last value.
      operandList.remove(idx*2);
      operandList.remove(idx*2 + 1);
      numOps -= 2;
    }

    /**
     * Gets the default basic block where default case clause resides.
     *
     * @return The default basic block.
     */
    public BasicBlock getDefaultBlock() {
      return (BasicBlock) operand(1);
    }

    // Accessor Methods for SwitchStmt stmt
    public Value getCondition() {
      return operand(0);
    }

    public void setCondition(Value val) {
      setOperand(0, val, this);
    }

    public int getNumOfCases() {
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
    public Constant getCaseValues(int index) {
      Util.assertion(index >= 0 && index < getNumOfCases(), "Illegal case value to get");

      return getSuccessorValue(index);
    }

    public int findCaseValue(Constant val) {
      for (int i = 1; i < getNumOfCases(); i++) {
        if (getCaseValues(i) == val)
          return i;
      }
      return 0;
    }

    public Constant findCaseDest(BasicBlock bb) {
      if (bb == getDefaultBlock()) return null;

      Constant res = null;
      for (int i = 0; i < getNumOfCases(); i++) {
        if (getSuccessor(i) == bb) {
          if (res != null) return null;
          else res = getCaseValues(i);

        }
      }
      return res;
    }

    public ConstantInt getSuccessorValue(int index) {
      Util.assertion(index >= 0 && index < getNumOfSuccessors(),
          "Successor value index out of range for switch");

      return (ConstantInt) operand(2 * index);
    }

    public BasicBlock getSuccessor(int index) {
      Util.assertion(index >= 0 && index < getNumOfSuccessors(),
          "Successor index out of range for switch");

      return (BasicBlock) operand(2 * index + 1);

    }

    public void setSuccessor(int index, BasicBlock newBB) {
      Util.assertion(index >= 0 && index < getNumOfSuccessors(),
          "Successor index out of range for switch");

      setOperand(index * 2 + 1, newBB, this);
    }

    // setSuccessorValue - Updates the value associated with the specified
    // successor.
    public void setSuccessorValue(int idx, Constant SuccessorValue) {
      Util.assertion((idx >= 0 && idx < getNumOfSuccessors()),
          "Successor # out of range!");

      setOperand(idx * 2, SuccessorValue, this);
    }

    public SwitchInst clone() {
      SwitchInst inst = new SwitchInst(getCondition(),
          getDefaultBlock(), getNumOfCases(), name);
      inst.operandList = new ArrayList<>(inst.operandList);
      return inst;
    }

    /**
     * Obtains the number of successors.
     *
     * @return
     */
    @Override
    public int getNumOfSuccessors() {
      return getNumOfOperands() >> 1;
    }
  }

  /**
   * The {@code PhiNode} instruction represents the merging of data flow in the
   * instruction graph. It refers to a join block and a variable.
   *
   * @author Jianping Zeng
   */
  public static class PhiNode extends Instruction {
    private int numOperands;

    public PhiNode(Type ty,
                   int numReservedValues,
                   String name) {
      this(ty, numReservedValues, name, (Instruction) null);
    }

    public PhiNode(Type ty,
                   int numReservedValues,
                   String name,
                   Instruction insertBefore) {
      super(ty, Operator.Phi, name, insertBefore);
      reserve(numReservedValues * 2);
    }

    public PhiNode(Type ty,
                   String name,
                   Instruction insertBefore) {
      super(ty, Phi, name, insertBefore);
      reserve(4);
    }

    public PhiNode(Type type,
                   int numReservedValue,
                   String name,
                   BasicBlock insertAtEnd) {
      super(type, Operator.Phi, name, insertAtEnd);
      reserve(numReservedValue * 2);
    }

    /**
     * Appends a pair that consists of both value and block into argument list.
     *
     * @param value The instruction that phi parameter to be inserted
     * @param block The according block of corresponding phi parameter.
     */
    public void addIncoming(Value value, BasicBlock block) {
      Util.assertion(value != null, "Phi node got a null value");
      Util.assertion(block != null, "Phi node got a null basic block");
      Util.assertion(value.getType().equals(getType()), "All of operands of Phi must be same type.");
      if (numOperands + 2 > getNumOfOperands()) {
        operandList.add(null);
        operandList.add(null);
      }
      operandList.set(numOperands, new Use(value, this));
      operandList.set(numOperands + 1, new Use(block, this));
      numOperands += 2;
    }

    /**
     * Gets the inputed parameter at given position.
     *
     * @param index The position where input parameter will be obtained.
     * @return The input parameter at specified position.
     */
    public Value getIncomingValue(int index) {
      Util.assertion(index >= 0 && index < getNumberIncomingValues(),
          "The index is beyond out the num of list");
      return operand(index << 1);
    }

    public void setIncomingValue(int index, Value val) {
      Util.assertion(index >= 0 && index < getNumberIncomingValues(),
          "The index is beyond out the num of list");
      setOperand(index << 1, val, this);
    }

    public Value getIncomingValueForBlock(BasicBlock bb) {
      int idx = getBasicBlockIndex(bb);
      Util.assertion(idx >= 0, "Invalid basic block argument");
      return getIncomingValue(idx);
    }

    /**
     * Gets the input block at given position.
     *
     * @param index The position where input block will be obtained.
     * @return The input block at specified position.
     */
    public BasicBlock getIncomingBlock(int index) {
      Util.assertion(index >= 0 && index < getNumberIncomingValues(),
          "The index is beyond out the num of list");
      return (BasicBlock) operand(index * 2 + 1);
    }

    public void setIncomingBlock(int index, BasicBlock bb) {
      Util.assertion(index >= 0 && index < getNumberIncomingValues(),
          "The index is beyond out the num of list");
      setOperand(index * 2 + 1, bb, this);
    }

    public Value removeIncomingValue(int index, boolean deletePhiIfEmpty) {
      Util.assertion(index >= 0 && index < getNumberIncomingValues(),
          "The index is beyond out the num of list");
      Value old = operand(index * 2);
      int numOps = getNumOfOperands();
      for (int i = (index + 1) * 2; i < numOps; i++) {
        operandList.set(i - 2, operandList.get(i));
        operandList.set(i - 2 + 1, operandList.get(i + 1));
      }
      numOperands -= 2;
      operandList.set(numOps - 2, null);
      operandList.set(numOps - 2 + 1, null);

      // delete this phi node if it has zero entities.
      if (numOps == 2 && deletePhiIfEmpty) {
        replaceAllUsesWith(UndefValue.get(getType()));
        eraseFromParent();
      }
      return old;
    }

    public Value removeIncomingValue(BasicBlock bb) {
      return removeIncomingValue(bb, true);
    }

    public Value removeIncomingValue(BasicBlock bb, boolean deletePhiIfEmpty) {
      int index = getBasicBlockIndex(bb);
      Util.assertion(index >= 0, "invalid basic block argument to remove");
      return removeIncomingValue(index, deletePhiIfEmpty);
    }

    public int getBasicBlockIndex(BasicBlock basicBlock) {
      Util.assertion((basicBlock != null), "PhiNode.getBasicBlockIndex(<null>) is invalid");

      for (int i = 0; i < getNumberIncomingValues(); i++) {
        if (getIncomingBlock(i) == basicBlock)
          return i;
      }
      return -1;
    }

    @Override
    public int getNumOfOperands() {
      return numOperands;
    }

    /**
     * Obtains the numbers of incoming value of phi node.
     *
     * @return
     */
    public int getNumberIncomingValues() {
      return getNumOfOperands() >> 1;
    }

    /**
     * hasConstantValue - If the specified PHI node always merges
     * together the same value, return the value, otherwise return null.
     *
     * @return
     */
    public Value hasConstantValue() {
      Value val = getIncomingValue(0);
      for (int i = 1, e = getNumberIncomingValues(); i < e; i++) {
        if (getIncomingValue(i) != val)
          return null;
      }
      return val;
    }

    /**
     * Gets the basic block corresponding to the use {@code u}.
     *
     * @param u
     * @return
     */
    public BasicBlock getIncomingBlock(Use u) {
      Util.assertion(u.getUser().equals(this));
      return ((Instruction) u.getValue()).getParent();
    }

    @Override
    public PhiNode clone() {
      return (PhiNode) super.clone();
    }
  }

  /**
   * This class was served functionally as allocating memory on the stack frame.
   * <b>Note that </b>all of backend.heap allocation is accomplished by invoking the
   * C language library function as yet.
   */
  public static class AllocaInst extends AllocationInst {
    /**
     * Creates a new {@linkplain AllocaInst} Module that allocates memory
     * for specified {@Type ty} and the numbers of to be allocated
     * element.
     *
     * @param ty        The data ty of allocated data which is instance of
     * @param arraySize The number of elements if allocating is used for
     *                  array.
     * @param name      The getIdentifier of this instruction for debugging.
     */
    public AllocaInst(Type ty,
                      Value arraySize,
                      int alignment,
                      String name,
                      Instruction insertBefore) {
      super(ty, Operator.Alloca,
          arraySize == null ? ConstantInt.get(LLVMContext.Int32Ty, 1) :
              arraySize, alignment, name, insertBefore);

      Util.assertion(getArraySize().getType() == LLVMContext.Int32Ty, "Alloca array getNumOfSubLoop != UnsignedIntTy");

    }

    public AllocaInst(Type ty, Value arraySize, int alignment) {
      this(ty, arraySize, alignment, "", null);
    }

    public AllocaInst(Type ty,
                      Value arraySize,
                      String name) {
      this(ty, arraySize, 0, name, null);
    }

    public AllocaInst(Type ty,
                      String name,
                      Instruction insertBefore) {
      this(ty, null, 0, name, insertBefore);
    }

    /**
     * Checks if this is a allocation of array not not.
     * Return true if the array getTypeSize is not 1.
     *
     * @return
     */
    public boolean isArrayAllocation() {
      return !operand(0).equals(ConstantInt.get(LLVMContext.Int32Ty, 1));
    }

    public Type getAllocatedType() {
      return getType().getElementType();
    }

    /**
     * Gets the instruction that produced the num argument.
     */
    public Value getArraySize() {
      return operand(0);
    }

    /**
     * Checks if this alloca instruction is in the entry block of function and
     * has a constant allocated size.
     *
     * @return
     */
    public boolean isStaticAlloca() {
      if (getParent() != getParent().getParent().getEntryBlock())
        return false;
      return getArraySize() instanceof ConstantInt;
    }
  }

  /**
   * An instruction for writing data into memory.
   */
  public static class StoreInst extends Instruction {
    private boolean isVolatile;
    private int alignment;


    public StoreInst(Value value, Value ptr, boolean isVolatile, int align) {
      this(value, ptr, isVolatile, align, "", null);
    }

    public StoreInst(Value value, Value ptr, boolean isVolatile,
                     int align, String name, Instruction insertBefore) {
      super(LLVMContext.VoidTy, Operator.Store, name, insertBefore);
      init(value, ptr);
      setIsVolatile(isVolatile);
      setAlignment(align);
    }

    /**
     * Constructs a new store instruction.
     *
     * @param value The inst to being writed into memory.
     * @param ptr   The targetAbstractLayer memory address where inst stores.
     */
    public StoreInst(Value value,
                     Value ptr,
                     String name,
                     Instruction insertBefore) {
      this(value, ptr, false, 0, name, insertBefore);
    }

    /**
     * Constructs a new store instruction.
     *
     * @param value The inst to being writed into memory.
     * @param ptr   The targetAbstractLayer memory address where inst stores.
     */
    public StoreInst(
        Value value,
        Value ptr,
        String name) {
      this(value, ptr, false, 0, name, null);
    }

    public StoreInst(Value value,
                     Value ptr,
                     String name,
                     BasicBlock insertAtEnd) {
      super(LLVMContext.VoidTy, Operator.Store, name, insertAtEnd);
      setIsVolatile(false);
      setAlignment(0);
      init(value, ptr);
    }

    private void init(Value value, Value ptr) {
      Util.assertion(value != null, "The value written into memory must be not null.");

      Util.assertion(ptr != null, "The memory address of StoreInst must be not null.");

      Util.assertion(ptr.getType().isPointerType(), "the destination of StoreInst must be AllocaInst!");

      reserve(2);
      setOperand(0, value, this);
      setOperand(1, ptr, this);
    }

    public Value getValueOperand() {
      return operand(0);
    }

    public Value getPointerOperand() {
      return operand(1);
    }

    public int getPointerOperandIndex() {
      return 1;
    }

    public boolean isVolatile() {
      return isVolatile;
    }

    public void setIsVolatile(boolean isVolatile) {
      this.isVolatile = isVolatile;
    }

    public void setAlignment(int align) {
      Util.assertion((align & (align - 1)) == 0, "Alignment must be power of 2");
      alignment = align;
    }

    public int getAlignment() {
      return alignment;
    }
  }

  /**
   * An instruction for reading data from memory.
   */
  public static class LoadInst extends UnaryInstruction {
    private boolean isVolatile;
    private int alignment;


    public LoadInst(Value from, String name, Instruction insertBefore) {
      super(((PointerType) from.getType()).getElementType(),
          Operator.Load,
          from,
          name,
          insertBefore);
      assertOK();
    }

    public LoadInst(Value from, String name, boolean isVolatile, int align) {
      super(((PointerType) from.getType()).getElementType(),
          Operator.Load, from, name, (Instruction) null);
      setName(name);
      setAlignment(align);
      setIsVolatile(isVolatile);
      assertOK();
    }

    public LoadInst(Value from, String name, BasicBlock insertAtEnd) {
      super(((PointerType) from.getType()).getElementType(),
          Operator.Load, from, name, insertAtEnd);
      assertOK();
    }

    private void assertOK() {
      Util.assertion(operand(0).getType().isPointerType()
          , "Ptr must have pointer type.");
    }

    public Value getPointerOperand() {
      return operand(0);
    }

    public int getPointerOperandIndex() {
      return 0;
    }

    public boolean isVolatile() {
      return isVolatile;
    }

    public void setIsVolatile(boolean isVolatile) {
      this.isVolatile = isVolatile;
    }

    public void setAlignment(int align) {
      Util.assertion((align & (align - 1)) == 0, "Alignment must be power of 2!");
      alignment = align;
    }

    public int getAlignment() {
      return alignment;
    }
  }

  /**
   * A instruction for type-safe pointer arithmetic to access elements of arrays and structs.
   */
  public static class GetElementPtrInst extends Instruction {
    private boolean inbounds;

    public GetElementPtrInst(Value ptr,
                             Value idx,
                             String name,
                             Instruction insertBefore) {
      super(PointerType.getUnqual(checkType(getIndexedType(ptr.getType(), idx))),
          GetElementPtr, name, insertBefore);
      reserve(2);
      init(ptr, idx);
    }

    public GetElementPtrInst(Value ptr, Value idx, String name, BasicBlock insertAtEnd) {
      super(PointerType.getUnqual(checkType(getIndexedType(ptr.getType(), idx))),
          GetElementPtr, name, insertAtEnd);
      reserve(2);
      init(ptr, idx);
    }

    public GetElementPtrInst(Value ptr, Value idx, String name) {
      this(ptr, idx, name, (Instruction) null);
    }

    public GetElementPtrInst(Value ptr, Value idx) {
      this(ptr, idx, "", (Instruction) null);
    }

    public GetElementPtrInst(Value ptr, List<Value> indices, String name) {
      this(ptr, indices, name, null);
    }

    public GetElementPtrInst(Value ptr, List<Value> indices,
                             String name, Instruction insertBefore) {
      super(PointerType.getUnqual(checkType(getIndexedType(ptr.getType(), indices))),
          GetElementPtr, name, insertBefore);
      reserve(indices.size());
      setOperand(0, ptr, this);
      int i = 1;
      for (Value idx : indices) {
        setOperand(i++, idx, this);
      }
    }

    private void init(Value ptr, Value idx) {
      Util.assertion(getNumOfOperands() == 2, "NumOperands not initialized.");
      setOperand(0, ptr, this);
      setOperand(1, idx, this);
    }

    public static Type getIndexedType(Type ptrType, List<Value> indices) {
      // It is not pointer type.
      if (!(ptrType instanceof PointerType))
        return null;
      PointerType pt = (PointerType) ptrType;

      Type aggTy = pt.getElementType();
      if (indices.isEmpty()) return aggTy;
      if (!aggTy.isSized() && !aggTy.isAbstract())
        return null;
      int idx = 1;
      for (int e = indices.size(); idx < e; idx++) {
        if (!(aggTy instanceof CompositeType))
          return null;
        CompositeType ct = (CompositeType) aggTy;
        if (ct instanceof PointerType) return null;
        Value index = indices.get(idx);
        if (!ct.indexValid(index)) return null;
        aggTy = ct.getTypeAtIndex(index);
      }

      return idx == indices.size() ? aggTy : null;
    }

    public static Type getIndexedType(Type ptrType, Value idx) {
      // It is not pointer type.
      if (!(ptrType instanceof PointerType))
        return null;
      PointerType pt = (PointerType) ptrType;

      // Check the pointer index.
      if (!pt.indexValid(idx))
        return null;

      return pt.getElementType();
    }

    /**
     * Overload to return most specific pointer type.
     *
     * @return
     */
    @Override
    public PointerType getType() {
      return (PointerType) super.getType();
    }

    public Value getPointerOperand() {
      return operand(0);
    }

    public PointerType getPointerOperandType() {
      return (PointerType) getPointerOperand().getType();
    }

    public boolean hasIndices() {
      return getNumOfOperands() > 1;
    }

    public int getIndexBegin() {
      return 1;
    }

    public int getIndexEnd() {
      return getNumOfOperands();
    }

    public void setInbounds(boolean inbounds) {
      this.inbounds = inbounds;
    }

    public boolean getInbounds() {
      return inbounds;
    }

    /**
     * Checks to see if all indices in this gep instruction are
     * constant.
     *
     * @return
     */
    public boolean hasAllConstantIndices() {
      for (int i = 1, e = getNumOfOperands(); i < e; i++) {
        if (!(operand(i) instanceof ConstantInt))
          return false;
      }
      return true;
    }
  }

  /**
   * This class represents the va_arg llvm instruction, which returns
   * an argument of the specified type given a va_list and increments that list
   */
  public static class VAArgInst extends UnaryInstruction {
    public VAArgInst(Value list,
                     Type ty) {
      this(list, ty, "");
    }

    public VAArgInst(Value list,
                     Type ty,
                     String name) {
      this(list, ty, name, (Instruction) null);
    }

    public VAArgInst(Value list,
                     Type ty,
                     String name,
                     Instruction insertBefore) {
      super(ty, VAArg, list, name, insertBefore);
    }

    public VAArgInst(Value list,
                     Type ty,
                     String name,
                     BasicBlock insertAtEnd) {
      super(ty, VAArg, list, name, insertAtEnd);
    }
  }

  /**
   * This instruction extracts a single (scalar) element from a VectorType value
   */
  public static class ExtractElementInst extends Instruction {

    public ExtractElementInst(Value vec, Value index) {
      this(vec, index, "");
    }

    public ExtractElementInst(Value vec,
                              Value index,
                              String name) {
      this(vec, index, name, (Instruction)null);
    }

    public ExtractElementInst(Value vec, Value index,
                              String name,
                              Instruction insertBefore) {
      super(((VectorType)vec.getType()).getElementType(), ExtractElement, name, insertBefore);
      Util.assertion(isValidOperands(vec, index), "invalid extractelement instruction operands!");
      reserve(2);
      setOperand(0, new Use(vec, this));
      setOperand(1, new Use(index, this));
    }

    public ExtractElementInst(Value vec, Value index,
                              String name,
                              BasicBlock insertAtEnd) {
      super(((VectorType)vec.getType()).getElementType(), ExtractElement, name, insertAtEnd);
      Util.assertion(isValidOperands(vec, index), "invalid extractelement instruction operands!");
      reserve(2);
      setOperand(0, new Use(vec, this));
      setOperand(1, new Use(index, this));
    }

    public static boolean isValidOperands(Value vec, Value index) {
      if (!vec.getType().isVectorTy())
        return false;

      if (index.getType().isIntegerType(32))
        return false;
      return true;
    }

    public Value getVectorOperand() { return operand(0); }
    public Value getIndexOperand() { return operand(1); }
    public VectorType getVectorTypeOperandType() {
      return (VectorType) getVectorOperand().getType();
    }
  }

  /**
   * This instruction inserts a single (scalar) element into a VectorType value
   */
  public static class InsertElementInst extends Instruction {

    public InsertElementInst(Value vec,
                             Value newElt,
                             Value idx,
                             String name) {
      this(vec, newElt, idx, name, (Instruction)null);
    }

    public InsertElementInst(Value vec,
                             Value newElt,
                             Value idx) {
      this(vec, newElt, idx, "");
    }

    public InsertElementInst(Value vec,
                             Value newElt,
                             Value idx,
                             String name,
                             Instruction insertBefore) {
      super(vec.getType(), InsertElement, name, insertBefore);
      Util.assertion(isValidOperands(vec, newElt, idx), "invalid insertelement instruction operands!");
      reserve(3);
      setOperand(0, new Use(vec, this));
      setOperand(1, new Use(newElt, this));
      setOperand(2, new Use(idx, this));
    }

    public InsertElementInst(Value vec, Value newElt,
                             Value idx,
                             String name,
                             BasicBlock insertAtEnd) {
      super(vec.getType(), InsertElement, name, insertAtEnd);
      Util.assertion(isValidOperands(vec, newElt, idx), "invalid insertelement instruction operands!");
      reserve(3);
      setOperand(0, new Use(vec, this));
      setOperand(1, new Use(newElt, this));
      setOperand(2, new Use(idx, this));
    }

    public static boolean isValidOperands(Value vec, Value newElt, Value idx) {
      if (!vec.getType().isVectorTy())
        return false;
      if (!newElt.getType().equals( ((VectorType)vec.getType()).getElementType()))
        return false;
      if (!idx.getType().isIntegerType(32))
        return false;
      return true;
    }

    public VectorType getType() {
      return (VectorType)super.getType();
    }
  }

  public static class ShuffleVectorInst extends Instruction {

    public ShuffleVectorInst(Value v1,
                             Value v2,
                             Value mask) {
      this(v1, v2, mask, "");
    }

    public ShuffleVectorInst(Value v1,
                             Value v2,
                             Value mask,
                             String name) {
      this(v1, v2, mask, name, (Instruction)null);
    }

    public ShuffleVectorInst(Value v1,
                             Value v2,
                             Value mask,
                             String name,
                             Instruction insertBefore) {
      super(VectorType.get(((VectorType)v1.getType()).getElementType(),
          ((VectorType)v1.getType()).getNumElements()),
          ShuffleVector,
          name, insertBefore);
      reserve(3);
      setOperand(0, new Use(v1, this));
      setOperand(1, new Use(v2, this));
      setOperand(2, new Use(mask, this));
    }

    public ShuffleVectorInst(Value v1,
                             Value v2,
                             Value mask,
                             String name,
                             BasicBlock insertAtEnd) {
      super(VectorType.get(((VectorType)v1.getType()).getElementType(),
          ((VectorType)v1.getType()).getNumElements()),
          ShuffleVector,
          name, insertAtEnd);
      reserve(3);
      setOperand(0, new Use(v1, this));
      setOperand(1, new Use(v2, this));
      setOperand(2, new Use(mask, this));
    }

    public static boolean isValidOperands(Value v1, Value v2, Value mask) {
      if (!v1.getType().isVectorTy() || !v1.getType().equals(v2.getType()))
        return false;

      VectorType maskTy = mask.getType() instanceof VectorType ?
          (VectorType) mask.getType() : null;
      if (!(mask instanceof Constant) || maskTy == null ||
          !maskTy.getElementType().isIntegerType(32))
        return false;
      return true;
    }

    public VectorType getType() {
      return (VectorType)super.getType();
    }

    public int getMaskValue(int i) {
      Constant mask = (Constant) operand(2);
      if (mask instanceof UndefValue) return -1;
      if (mask instanceof ConstantAggregateZero) return 0;
      ConstantVector maskCV = (ConstantVector) mask;
      Util.assertion(i < maskCV.getNumOfOperands(), "index out of range!");
      if (maskCV.operand(i) instanceof UndefValue)
        return -1;
      return (int) ((ConstantInt)maskCV.operand(i)).getZExtValue();
    }
  }

  /**
   * This instruction represents the operation that extracting a value of the
   * structure or array type.
   */
  public static class ExtractValueInst extends Instruction {

    private int[] indices;
    private void init(Value agg, int[] idxs, String name) {
      Util.assertion(idxs.length == 1, "the number of operands must be two!");
      reserve(1);
      setOperand(0, new Use(agg, this));
      setName(name);
      indices = idxs;
    }
    public ExtractValueInst(Value agg,
                            int[] indices) {
      this(agg, indices, "");
    }

    public ExtractValueInst(Value agg,
                            int[] indices,
                            String name) {
      this(agg, indices, name, (Instruction)null);
    }

    public ExtractValueInst(Value agg,
                           int[] indices,
                           String name,
                           Instruction insertBefore) {
      super(checkType(getIndexedType(agg.getType(), indices)), ExtractValue, name, insertBefore);
      init(agg, indices, name);
    }

    public ExtractValueInst(Value agg,
                           int[] indices,
                           String name,
                           BasicBlock insertAtEnd) {
      super(checkType(getIndexedType(agg.getType(), indices)), ExtractValue, name, insertAtEnd);
      init(agg, indices, name);
    }

    public int[] getIndices() { return indices; }

    public Value getAggregateOperand() {
      return operand(0);
    }

    public static int getAggregateOperandIndex() { return 0; }

    public static Type getIndexedType(Type agg,
                                      int[] indices) {
      int curIdx = 0;
      for (; curIdx < indices.length; ++curIdx) {
        CompositeType ct = null;
        if (agg instanceof CompositeType) {
          ct = (CompositeType) agg;
        }
        if (ct == null || ct.isPointerType() || ct.isVectorTy())
          return null;

        int index = indices[curIdx];
        if (!ct.indexValid(index)) return null;
        agg = ct.getTypeAtIndex(index);
      }
      return curIdx == indices.length ? agg : null;
    }

    public static Type getIndexedType(Type agg,
                                      int idx) {
      return getIndexedType(agg, new int[]{idx});
    }

    public int getNumIndices() { return indices.length; }

    public boolean hasIndices() { return true; }
  }

  /**
   * This instruction represents the action that inserting a value into a aggregate value.
   */
  public static class InsertValueInst extends Instruction {
    private int[] indices;

    private void init(Value agg, Value val, int[] idxs, String name) {
      Util.assertion(idxs.length == 2, "the number of operands must be two!");
      reserve(2);
      setOperand(0, agg);
      setOperand(1, val);
      setName(name);
      indices = idxs;
    }

    public InsertValueInst(Value agg,
                           Value val,
                           int[] indices) {
      this(agg, val, indices, "");
    }

    public InsertValueInst(Value agg,
                           Value val,
                           int[] indices,
                           String name) {
      this(agg, val, indices, name, (Instruction)null);
    }

    public InsertValueInst(Value agg,
                           Value val,
                           int[] indices,
                           String name,
                           Instruction insertBefore) {
      super(agg.getType(), InsertValue, name, insertBefore);
      init(agg, val, indices, name);
    }

    public InsertValueInst(Value agg,
                           Value val,
                           int[] indices,
                           String name,
                           BasicBlock insertAtEnd) {
      super(agg.getType(), InsertValue, name, insertAtEnd);
      init(agg, val, indices, name);
    }

    public int[] getIndices() { return indices; }

    public Value getAggregateOperand() {
      return operand(0);
    }

    public static int getAggregateOperandIndex() { return 0; }

    public Value getInsertedValueOperand() { return operand(1); }

    public static int getInsertedValueOperandIndex() { return 1; }

    public int getNumIndices() { return indices.length; }

    public boolean hasIndices() { return true; }
  }
}
