package backend.value;

import backend.debug.DebugLoc;
import backend.ir.AllocationInst;
import backend.support.*;
import backend.type.*;
import gnu.trove.list.array.TIntArrayList;
import tools.Pair;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

  protected boolean hasMetadataHashEntry;

  protected DebugLoc dbgLoc;

  public Instruction(Type ty,
                     Operator opc,
                     String name,
                     Instruction insertBefore) {
    super(ty, ValueKind.InstructionVal + opc.index);
    opcode = opc;
    dbgLoc = new DebugLoc();
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
    dbgLoc = new DebugLoc();
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
    operandList = null;
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
   * @param mdKind
   * @param node
   */
  public void setMetadata(int mdKind, MDNode node) {
    if (node == null && !hasMetadata()) return;

    if (mdKind == LLVMContext.MD_dbg) {
      dbgLoc = DebugLoc.getFromDILocation(node);
      return;
    }

    // handle the case when we are adding/updating metadata on an instruction.
    if (node != null) {
      Util.assertion((getContext().metadataStore.containsKey(this) &&
          !getContext().metadataStore.get(this).isEmpty())  == hasMetadataHashEntry(),
          "hasMetadataHashEntry bit goes wrong!");
      if (!getContext().metadataStore.containsKey(this))
        getContext().metadataStore.put(this, new ArrayList<>());

      Util.assertion(getContext().metadataStore.containsKey(this), "must exist in metadataStore map!");
      ArrayList<Pair<Integer, MDNode>> entries = getContext().metadataStore.get(this);
      if (entries.isEmpty()) {
        setHasMetadataHashEntry(true);
      }
      else {
        // update/replace existing one.
        for (int i = 0, e = entries.size(); i < e; i++) {
          if (entries.get(i).first == mdKind) {
            entries.get(i).second = node;
            return;
          }
        }
      }

      // no replacement, just add it to the list.
      entries.add(Pair.get(mdKind, node));
      return;
    }

    // otherwise, we are going to remove metadata from an instruction.
    Util.assertion(hasMetadataHashEntry() && getContext().metadataStore.containsKey(this),
        "hasMetadataHashEntry bit out of date!");

    ArrayList<Pair<Integer, MDNode>> entries = getContext().metadataStore.get(this);
    if (entries.size() == 1 && entries.get(0).first == mdKind) {
      getContext().metadataStore.remove(this);
      setHasMetadataHashEntry(false);
      return;
    }

    // handle removal of an existing value.
    for (int i = 0, e = entries.size(); i < e; i++) {
      if (entries.get(i).first == mdKind) {
        entries.set(i, entries.get(e - 1));
        entries.remove(e - 1);
        Util.assertion(!entries.isEmpty(), "Removing last entry should be handled above!");
        return;
      }
    }
  }

  private boolean hasMetadata() {
    return !dbgLoc.isUnknown() || hasMetadataHashEntry();
  }

  public boolean hasMetadataOtherThanDebugLoc() {
    return hasMetadataHashEntry();
  }

  public boolean hasMetadataHashEntry() {
    return hasMetadataHashEntry;
  }

  public void setHasMetadataHashEntry(boolean b) {
    hasMetadataHashEntry = b;
  }

  public void setDebugLoc(DebugLoc loc) {
    dbgLoc = loc;
  }

  public DebugLoc getDebugLoc() {
    return dbgLoc;
  }

  public void getAllMetadataOtherThanDebugLoc(ArrayList<Pair<Integer, MDNode>> mds) {
    if (hasMetadataOtherThanDebugLoc())
      getAllMetadataOtherThanDebugLocImpl(mds);
  }

  public void getAllMetadataOtherThanDebugLocImpl(ArrayList<Pair<Integer, MDNode>> result) {
    result.clear();
    Util.assertion(hasMetadataHashEntry() &&
        getContext().metadataStore.containsKey(this), "Shouldn't have called this");
    ArrayList<Pair<Integer, MDNode>> info = getContext().metadataStore.get(this);
    result.addAll(info);
  }

  public void getAllMetadata(ArrayList<Pair<Integer, MDNode>> result) {
    if (hasMetadata()) {
      result.clear();
      // Handle 'dbg' as a special case since it is not stored in the hash table.
      if (!dbgLoc.isUnknown()) {
        result.add(Pair.get(LLVMContext.MD_dbg, dbgLoc.getAsMDNode(getContext())));
        if (!hasMetadataHashEntry())
          return;
      }

      Util.assertion(hasMetadataHashEntry() &&
          getContext().metadataStore.containsKey(this), "shouldn't have called this");
      Util.assertion(!getContext().metadataStore.get(this).isEmpty());
      result.addAll(getContext().metadataStore.get(this));
    }
  }

/*  @Override
  public int hashCode() {
    int res = Util.hash2(getOpcode().index, getNumOfOperands(), opcode);
    res = Util.hash2(res, hasMetadataHashEntry, dbgLoc);
    for (int i = 0, e = getNumOfOperands(); i < e; ++i)
      res = Util.hash1(res, operand(i));
    return res;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this)
      return true;
    if (!(other instanceof Instruction))
      return false;

    Instruction op = (Instruction)other;
    if (op.getOpcode() != getOpcode() ||
            getNumOfOperands() != op.getNumOfOperands()) return false;
    for (int i = 0, e = getNumOfOperands(); i < e; ++i)
      if (!Objects.equals(operand(i), op.operand(i)))
        return false;
    return true;
  }*/

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
  }

  /**
   * This class just for binary operation definition.
   *
   * @author Jianping Zeng
   */
  public static class BinaryOperator extends Instruction {
    private BinaryOperator(
        Type ty,
        Operator opcode,
        Value lhs,
        Value rhs,
        String name) {
      this(ty, opcode, lhs, rhs, name, (Instruction) null);
    }

    private BinaryOperator(
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

    private BinaryOperator(
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

    public static BinaryOperator create(
        Operator op,
        Value lhs,
        Value rhs,
        String name,
        Instruction insertBefore) {
      Util.assertion(lhs.getType().equals(rhs.getType()),
          "Cannot create binary operator with two operands of differing type!");
      if (op == Add || op == Sub || op == Mul || op == Shl)
        return new OverflowingBinaryInstruction(lhs.getType(), op, lhs, rhs, name, insertBefore);
      else if (op == SDiv || op == UDiv || op == LShr || op == AShr)
        return new ExactBinaryInstruction(lhs.getType(), op, lhs, rhs, name, insertBefore);
      else
        return new BinaryOperator(lhs.getType(), op, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator create(
        Operator op,
        Value lhs,
        Value rhs,
        String name) {
      Util.assertion(lhs.getType() == rhs.getType(),
          "Cannot create binary operator with two operands of differing type!");
      if (op == Add || op == Sub || op == Mul || op == Shl)
        return new OverflowingBinaryInstruction(lhs.getType(), op, lhs, rhs, name);
      else if (op == SDiv || op == UDiv || op == LShr || op == AShr)
        return new ExactBinaryInstruction(lhs.getType(), op, lhs, rhs, name);
      else
        return new BinaryOperator(lhs.getType(), op, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator create(
        Operator op,
        Value lhs,
        Value rhs,
        String name,
        BasicBlock insertAtEnd) {
      Util.assertion(lhs.getType() == rhs.getType(),
          "Cannot create binary operator with two operands of differing type!");
      if (op == Add || op == Sub || op == Mul || op == Shl)
        return new OverflowingBinaryInstruction(lhs.getType(), op, lhs, rhs, name, insertAtEnd);
      else if (op == SDiv || op == UDiv || op == LShr || op == AShr)
        return new ExactBinaryInstruction(lhs.getType(), op, lhs, rhs, name, insertAtEnd);
      else
        return new BinaryOperator(lhs.getType(), op, lhs, rhs, name, insertAtEnd);
    }

    //=====================================================================//
    //               The  first version with default insertBefore.         //
    public static BinaryOperator createAdd(Value lhs, Value rhs, String name) {
      return create(Operator.Add, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createFAdd(Value lhs, Value rhs, String name) {
      return create(Operator.FAdd, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createSub(Value lhs, Value rhs, String name) {
      return create(Operator.Sub, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createFSub(Value lhs, Value rhs, String name) {
      return create(Operator.FSub, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createMul(Value lhs, Value rhs, String name) {
      return create(Operator.Mul, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createFMul(Value lhs, Value rhs, String name) {
      return create(Operator.FMul, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createUDiv(Value lhs, Value rhs, String name) {
      return create(Operator.UDiv, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createSDiv(Value lhs, Value rhs, String name) {
      return create(Operator.SDiv, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createFDiv(Value lhs, Value rhs, String name) {
      return create(Operator.FDiv, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createURem(Value lhs, Value rhs, String name) {
      return create(Operator.URem, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createSRem(Value lhs, Value rhs, String name) {
      return create(Operator.SRem, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createFRem(Value lhs, Value rhs, String name) {
      return create(Operator.FRem, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createShl(Value lhs, Value rhs, String name) {
      return create(Operator.Shl, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createLShr(Value lhs, Value rhs, String name) {
      return create(Operator.LShr, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createAShr(Value lhs, Value rhs, String name) {
      return create(Operator.AShr, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createAnd(Value lhs, Value rhs, String name) {
      return create(Operator.And, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createOr(Value lhs, Value rhs, String name) {
      return create(Operator.Or, lhs, rhs, name, (Instruction) null);
    }

    public static BinaryOperator createXor(Value lhs, Value rhs, String name) {
      return create(Operator.Xor, lhs, rhs, name, (Instruction) null);
    }

    //=====================================================================//
    //                 The second version with insertAtEnd argument.       //
    public static BinaryOperator createAdd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Add, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createFAdd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FAdd, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createSub(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Sub, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createFSub(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FSub, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createMul(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FMul, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createFMul(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FMul, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createUDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.UDiv, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createSDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.SDiv, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createFDiv(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FDiv, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createURem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.URem, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createSRem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.SRem, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createFRem(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.FRem, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createShl(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Shl, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createLShr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.LShr, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createAShr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.AShr, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createAnd(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.And, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createOr(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Or, lhs, rhs, name, insertAtEnd);
    }

    public static BinaryOperator createXor(Value lhs, Value rhs, String name, BasicBlock insertAtEnd) {
      return create(Operator.Xor, lhs, rhs, name, insertAtEnd);
    }


    //=====================================================================//
    //                   The third version with insertBefore argument.     //
    public static BinaryOperator createAdd(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Add, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createFAdd(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FAdd, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createSub(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Sub, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createFSub(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FSub, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createMul(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FMul, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createFMul(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FMul, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createUDiv(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.UDiv, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createSDiv(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.SDiv, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createFDiv(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FDiv, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createURem(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.URem, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createSRem(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.SRem, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createFRem(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.FRem, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createShl(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Shl, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createLShr(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.LShr, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createAShr(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.AShr, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createAnd(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.And, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createOr(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Or, lhs, rhs, name, insertBefore);
    }

    public static BinaryOperator createXor(Value lhs, Value rhs, String name, Instruction insertBefore) {
      return create(Operator.Xor, lhs, rhs, name, insertBefore);
    }


    // ====================================================================//
    //   Some helper method for create unary operator with Bianry inst.    //
    public static BinaryOperator createNeg(Value op, String name, Instruction insertBefore) {
      Value zero = ConstantInt.getNullValue(op.getType());
      return BinaryOperator.create( Sub, zero, op, name, insertBefore);
    }

    public static BinaryOperator createNeg(Value op, String name, BasicBlock insertAtEnd) {
      Value zero = ConstantInt.getNullValue(op.getType());
      return BinaryOperator.create( Sub, zero, op, name, insertAtEnd);
    }

    public static BinaryOperator createNeg(Value op) {
      Value zero = ConstantInt.getNullValue(op.getType());
      return BinaryOperator.create(Sub, zero, op, "");
    }

    public static BinaryOperator createFNeg(Value op, String name, Instruction insertBefore) {
      Value zero = ConstantFP.getNullValue(op.getType());
      return BinaryOperator.create(FSub, zero, op, name, insertBefore);
    }

    public static BinaryOperator createFNeg(Value op) {
      Value zero = ConstantInt.getNullValue(op.getType());
      return BinaryOperator.create(FSub, zero, op, "");
    }

    public static BinaryOperator createFNeg(Value op, String name, BasicBlock insertAtEnd) {
      Value zero = ConstantFP.getNullValue(op.getType());
      return BinaryOperator.create(FSub, zero, op, name, insertAtEnd);
    }

    public static BinaryOperator createNot(Value op, String name, Instruction insertBefore) {
      Constant one = Constant.getAllOnesValue(op.getType());
      return BinaryOperator.create(Xor, one, op, name, insertBefore);
    }

    public static BinaryOperator createNot(Value op) {
      Constant one = Constant.getAllOnesValue(op.getType());
      return BinaryOperator.create( Xor, one, op, "");
    }

    public static BinaryOperator createNot(Value op, String name, BasicBlock insertAtEnd) {
      Constant one = Constant.getAllOnesValue(op.getType());
      return BinaryOperator.create( Xor, one, op, name, insertAtEnd);
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
  }

  /**
   * This class is used to encapsulate those binary operators with potential to
   * perform overflow operation, such as add, sub, mul.
   */
  public static class OverflowingBinaryInstruction extends BinaryOperator implements OverflowingBinaryOperator {
    /**
     * Indicates the operation (such as add, sub, mul)
     * doesn't have extra bits to been destroyed.
     */
    private boolean hasNoUnsignedWrap;
    private boolean hasNoSignedWrap;

    private OverflowingBinaryInstruction(Type ty, Operator opcode,
                                         Value lhs, Value rhs,
                                         String name) {
      super(ty, opcode, lhs, rhs, name);
    }

    private OverflowingBinaryInstruction(Type ty,
                                         Operator op,
                                         Value lhs,
                                         Value rhs,
                                         String name,
                                         Instruction insertBefore) {
      super(ty, op, lhs, rhs, name, insertBefore);
    }

    private OverflowingBinaryInstruction(
        Type ty,
        Operator opcode,
        Value lhs,
        Value rhs,
        String name,
        BasicBlock insertAtEnd) {
      super(ty, opcode, lhs, rhs, name, insertAtEnd);
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

  /**
   * This is used to encapsulate the sdiv operator which has potential to
   * indicate whether the operation is exact or not.
   */
  public static class ExactBinaryInstruction extends BinaryOperator implements ExactBinaryOperator {
    private boolean isExact;
    private ExactBinaryInstruction(Type ty, Operator opcode,
                                   Value lhs, Value rhs,
                                   String name) {
      super(ty, opcode, lhs, rhs, name);
    }

    private ExactBinaryInstruction(Type ty, Operator opcode,
                                   Value lhs, Value rhs,
                                   String name, Instruction insertBefore) {
      super(ty, opcode, lhs, rhs, name, insertBefore);
    }

    private ExactBinaryInstruction(Type ty, Operator opcode,
                                   Value lhs, Value rhs,
                                   String name, BasicBlock insertAtEnd) {
      super(ty, opcode, lhs, rhs, name, insertAtEnd);
    }

    @Override
    public boolean isExact() { return isExact; }

    @Override
    public void setIsExact(boolean b) { isExact = b; }
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
      return createIntegerCast(value, destTy, isSigned, "", (Instruction) null);
    }

    public static CastInst createIntegerCast(
        Value value,
        Type destTy,
        boolean isSigned,
        String instName,
        Instruction insertBefore) {
      Util.assertion(value.getType().isIntegerTy() && destTy.isIntegerTy(), "Invalid type!");
      int srcBits = value.getType().getScalarSizeBits();
      int destBits = destTy.getScalarSizeBits();
      Operator opcode = srcBits == destBits ? BitCast
          : srcBits > destBits ? Trunc
          : (isSigned ? SExt : ZExt);

      return create(opcode, value, destTy, instName, insertBefore);
    }

    public static CastInst createIntegerCast(Value value,
                                             Type destTy,
                                             boolean isSigned,
                                             String instName,
                                             BasicBlock insertAtEnd) {
      Util.assertion(value.getType().isIntegerTy() && destTy.isIntegerTy(), "Invalid type!");
      int srcBits = value.getType().getScalarSizeBits();
      int destBits = destTy.getScalarSizeBits();
      Operator opcode = srcBits == destBits ? BitCast
          : srcBits > destBits ? Trunc
          : (isSigned ? SExt : ZExt);

      return create(opcode, value, destTy, instName, insertAtEnd);
    }

    public static CastInst create(Operator opcode,
                                  Value value,
                                  Type ty,
                                  String name,
                                  BasicBlock insertAtEnd) {
      switch (opcode) {
        case Trunc:
          return new TruncInst(value, ty, name, insertAtEnd);
        case ZExt:
          return new ZExtInst(value, ty, name, insertAtEnd);
        case SExt:
          return new SExtInst(value, ty, name, insertAtEnd);
        case FPTrunc:
          return new FPTruncInst(value, ty, name, insertAtEnd);
        case FPExt:
          return new FPExtInst(value, ty, name, insertAtEnd);
        case UIToFP:
          return new UIToFPInst(value, ty, name, insertAtEnd);
        case SIToFP:
          return new SIToFPInst(value, ty, name, insertAtEnd);
        case FPToUI:
          return new FPToUIInst(value, ty, name, insertAtEnd);
        case FPToSI:
          return new FPToSIInst(value, ty, name, insertAtEnd);
        case PtrToInt:
          return new PtrToIntInst(value, ty, name, insertAtEnd);
        case IntToPtr:
          return new IntToPtrInst(value, ty, name, insertAtEnd);
        case BitCast:
          return new BitCastInst(value, ty, name, insertAtEnd);
        default:
          Util.assertion("Invalid opcode provided!");
      }
      return null;
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
          Util.assertion("Invalid opcode provided!");
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
          return srcTy.isIntegerTy() && destTy.isIntegerTy() &&
              srcBits > destBits;
        case SExt:
          return srcTy.isIntegerTy() && destTy.isIntegerTy() &&
              srcBits < destBits;
        case ZExt:
          return srcTy.isIntegerTy() && destTy.isIntegerTy() &&
              srcBits < destBits;
        case FPTrunc:
          return srcTy.isFloatingPointType() && destTy.isFloatingPointType()
              && srcBits > destBits;
        case FPExt:
          return srcTy.isFloatingPointType() && destTy.isFloatingPointType()
              && srcBits < destBits;
        case FPToSI:
        case FPToUI:
          return srcTy.isFloatingPointType() && destTy.isIntegerTy();
        case SIToFP:
        case UIToFP:
          return srcTy.isIntegerTy() && destTy.isFloatingPointType();
        case BitCast:
          // bit cast is no op in machine level, but we should check
          // both type is same when one is of type PointerType
          if ((srcTy instanceof PointerType) != (destTy instanceof PointerType))
            return false;

          return srcTy.getPrimitiveSizeInBits() == destTy.getPrimitiveSizeInBits();
        case PtrToInt:
          return srcTy.isPointerType() && destTy.isIntegerTy();
        case IntToPtr:
          return srcTy.isIntegerTy() && destTy.isPointerType();
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
      FCMP_FALSE(0),  /// 0 0 0 0    Always false (always folded)
      FCMP_OEQ(1),  /// 0 0 0 1    True if ordered and equal
      FCMP_OGT(2),  /// 0 0 1 0    True if ordered and greater than
      FCMP_OGE(3),  /// 0 0 1 1    True if ordered and greater than or equal
      FCMP_OLT(4),  /// 0 1 0 0    True if ordered and less than
      FCMP_OLE(5),  /// 0 1 0 1    True if ordered and less than or equal
      FCMP_ONE(6),  /// 0 1 1 0    True if ordered and operands are unequal
      FCMP_ORD(7),  /// 0 1 1 1    True if ordered (no nans)
      FCMP_UNO(8),  /// 1 0 0 0    True if unordered: isnan(X) | isnan(Y)
      FCMP_UEQ(9),  /// 1 0 0 1    True if unordered or equal
      FCMP_UGT(10),  /// 1 0 1 0    True if unordered or greater than
      FCMP_UGE(11),  /// 1 0 1 1    True if unordered, greater than, or equal
      FCMP_ULT(12),  /// 1 1 0 0    True if unordered or less than
      FCMP_ULE(13),  /// 1 1 0 1    True if unordered, less than, or equal
      FCMP_UNE(14),  /// 1 1 1 0    True if unordered or not equal
      FCMP_TRUE(15),  /// 1 1 1 1    Always true (always folded)
      FIRST_FCMP_PREDICATE(FCMP_FALSE.enumValue()),
      LAST_FCMP_PREDICATE(FCMP_TRUE.enumValue()),
      BAD_FCMP_PREDICATE(FCMP_TRUE.enumValue()+1),

      ICMP_EQ(32),  /// equal
      ICMP_NE(33),  /// not equal
      ICMP_UGT(34),  /// unsigned greater than
      ICMP_UGE(35),  /// unsigned greater or equal
      ICMP_ULT(36),  /// unsigned less than
      ICMP_ULE(37),  /// unsigned less or equal
      ICMP_SGT(38),  /// signed greater than
      ICMP_SGE(39),  /// signed greater or equal
      ICMP_SLT(40),  /// signed less than
      ICMP_SLE(41),  /// signed less or equal
      FIRST_ICMP_PREDICATE(ICMP_EQ.enumValue()),
      LAST_ICMP_PREDICATE(ICMP_SLE.enumValue()),
      BAD_ICMP_PREDICATE(ICMP_SLE.enumValue() + 1);

      public final int id;
      Predicate(int id) { this.id = id; }
      public int enumValue() { return id; }

      public static Predicate getPred(int val) {
        for (Predicate p : values())
          if(p.id == val)
            return p;
        return null;
      }
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
      super(Type.getInt1Ty(lhs.getContext()), FCmp, pred, lhs, rhs, name, insertBefore);
      Util.assertion(pred.compareTo(Predicate.LAST_FCMP_PREDICATE) <= 0, "Invalid FCmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to FCmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isFloatingPointType(), "Invalid operand types for FCmp instruction");

    }

    public FCmpInst(Predicate pred, Value lhs,
                    Value rhs, String name, BasicBlock insertAtEnd) {
      super(Type.getInt1Ty(lhs.getContext()), FCmp, pred, lhs, rhs, name, insertAtEnd);
      Util.assertion(pred.compareTo(Predicate.LAST_FCMP_PREDICATE) <= 0, "Invalid FCmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to FCmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isFloatingPointType(), "Invalid operand types for FCmp instruction");

    }

    public FCmpInst(Predicate pred, Value lhs, Value rhs) {
      super(Type.getInt1Ty(lhs.getContext()), FCmp, pred, lhs, rhs, "", (Instruction) null);
      Util.assertion(pred.compareTo(Predicate.LAST_FCMP_PREDICATE) <= 0, "Invalid FCmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to FCmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isFloatingPointType(), "Invalid operand types for FCmp instruction");

    }

    public FCmpInst(Predicate pred, Value lhs, Value rhs, String name) {
      super(Type.getInt1Ty(lhs.getContext()), FCmp, pred, lhs, rhs, name, (Instruction) null);
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
      Use u = operandList[0];
      operandList[0] = operandList[1];
      operandList[1] = u;
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
      super(Type.getInt1Ty(lhs.getContext()), ICmp, pred, lhs, rhs, name, insertBefore);
      Util.assertion(pred.compareTo(Predicate.LAST_ICMP_PREDICATE) <= 0, "Invalid ICmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to ICmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isIntegerTy() || lhs.getType() instanceof PointerType, "Invalid operand types for ICmp instruction");

    }

    public ICmpInst(Predicate pred, Value lhs,
                    Value rhs, String name, BasicBlock insertAtEnd) {
      super(Type.getInt1Ty(lhs.getContext()), ICmp, pred, lhs, rhs, name, insertAtEnd);
      Util.assertion(pred.compareTo(Predicate.LAST_ICMP_PREDICATE) <= 0, "Invalid ICmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to ICmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isIntegerTy(), "Invalid operand types for ICmp instruction");

    }

    public ICmpInst(Predicate pred, Value lhs,
                    Value rhs) {
      super(Type.getInt1Ty(lhs.getContext()), ICmp, pred, lhs, rhs, "", (Instruction) null);
      Util.assertion(pred.compareTo(Predicate.LAST_ICMP_PREDICATE) <= 0, "Invalid ICmp predicate value");
      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to ICmp instruction are not of the same type!");
      Util.assertion(lhs.getType().isIntOrIntVectorTy() ||
              lhs.getType().isPointerType(), "Invalid operand types for ICmp instruction");
    }

    public ICmpInst(Predicate pred, Value lhs,
                    Value rhs, String name) {
      super(Type.getInt1Ty(lhs.getContext()), ICmp, pred, lhs, rhs, name, (Instruction) null);
      Util.assertion(pred.compareTo(Predicate.LAST_ICMP_PREDICATE) <= 0, "Invalid ICmp predicate value");

      Util.assertion(lhs.getType() == rhs.getType(), "Both operands to ICmp instruction are not of the same type!");

      Util.assertion(lhs.getType().isIntegerTy() || lhs.getType().isPointerType(), "Invalid operand types for ICmp instruction");

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
          Util.assertion("Undefined icmp predicate!");
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
      Use u = operandList[0];
      operandList[0] = operandList[1];
      operandList[1] = u;
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
    protected TerminatorInst(Type ty,
                   Operator opcode,
                   String instName,
                   Instruction insertBefore) {
      super(ty, opcode, instName, insertBefore);
    }


    protected TerminatorInst(Type ty,
                   Operator opcode,
                   String instName,
                   BasicBlock insertAtEnd) {
      super(ty, opcode, instName, insertAtEnd);
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
      super(Type.getVoidTy(ifTrue.getContext()), Operator.Br, "", insertBefore);
      reserve(1);
      setOperand(0, ifTrue, this);
    }

    public BranchInst(BasicBlock ifTrue, String name, Instruction insertBefore) {
      super(Type.getVoidTy(ifTrue.getContext()), Operator.Br, name, insertBefore);
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
      super(Type.getVoidTy(ifTrue.getContext()), Operator.Br, "", insertBefore);
      reserve(3);
      setOperand(0, ifTrue, this);
      setOperand(1, ifFalse, this);
      setOperand(2, cond, this);
    }

    public BranchInst(BasicBlock ifTrue, BasicBlock ifFalse, Value cond,
                      BasicBlock insertAtEnd) {
      super(Type.getVoidTy(ifTrue.getContext()), Operator.Br, "", insertAtEnd);
      reserve(3);
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
      super(Type.getVoidTy(ifTrue.getContext()), Operator.Br, "", insertAtEnd);
      reserve(1);
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
     * Change the current branch to an unconditional branch targeting the
     * specified block.
     *
     * @param dest
     */
    public void setUnconditionalDest(BasicBlock dest) {
      operandList = new Use[1];
      operandList[0] = new Use(dest, this);
      numOps = 1;
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
    public ReturnInst(LLVMContext ctx) {
      this(ctx, null, "", (Instruction) null);
    }

    public ReturnInst(LLVMContext ctx, Value val) {
      this(ctx, val, "", (Instruction) null);
    }

    /**
     * Constructs a new return instruction with return inst.
     *
     * @param retValue The return inst produce for this instruction, return
     *                 void if ret is {@code null}.
     */
    public ReturnInst(LLVMContext ctx, Value retValue, String name, Instruction insertBefore) {
      super(Type.getVoidTy(ctx), Operator.Ret, name, insertBefore);
      if (retValue != null) {
        reserve(1);
        setOperand(0, retValue, this);
      }
    }

    public ReturnInst(LLVMContext ctx, Value retValue, String name, BasicBlock insertAtEnd) {
      super(Type.getVoidTy(ctx), Operator.Ret, name, insertAtEnd);
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

    public UnWindInst(LLVMContext ctx, Instruction insertBefore) {
      super(Type.getVoidTy(ctx), Unwind, "", insertBefore);
    }
    public UnWindInst(LLVMContext ctx) {
      this(ctx, (Instruction)null);
    }

    public UnWindInst(LLVMContext ctx, BasicBlock insertAtEnd) {
      super(Type.getVoidTy(ctx), Unwind, "", insertAtEnd);
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

  public static class ResumeInst extends TerminatorInst {
    private ResumeInst(Value exn) {
      this(exn, (Instruction)null);
    }
    private ResumeInst(Value exn, Instruction insertBefore) {
      super(Type.getVoidTy(exn.getContext()), Resume, "", insertBefore);
      reserve(1);
      setOperand(0, exn, this);
    }

    private ResumeInst(Value exn, BasicBlock insertAtEnd) {
      super(Type.getVoidTy(exn.getContext()), Resume, "", insertAtEnd);
      reserve(1);
      setOperand(0, exn, this);
    }

    public static ResumeInst create(Value exn) {
      return new ResumeInst(exn);
    }

    @Override
    public BasicBlock getSuccessor(int index) {
      Util.shouldNotReachHere("Resume has not successor");
      return null;
    }

    @Override
    public int getNumOfSuccessors() {
      return 0;
    }

    @Override
    public void setSuccessor(int index, BasicBlock bb) {
      Util.shouldNotReachHere("Resume has not successor");
    }
  }

  public static class InvokeInst extends TerminatorInst {
    private AttrList attributes;
    private CallingConv cc;

    public static InvokeInst create(Value func, BasicBlock ifNormal, BasicBlock ifException,
                                    ArrayList<Value> args) {
      return create(func, ifNormal, ifException, args, "");
    }

    public static InvokeInst create(Value func, BasicBlock ifNormal, BasicBlock ifException,
                                    ArrayList<Value> args, String name) {
      return create(func, ifNormal, ifException, args, name, (Instruction)null);
    }

    public static InvokeInst create(Value func, BasicBlock ifNormal, BasicBlock ifException,
               ArrayList<Value> args, String name, Instruction insertBefore) {
      return new InvokeInst(func, ifNormal, ifException, args, name, insertBefore);
    }

    public static InvokeInst create(Value func, BasicBlock ifNormal, BasicBlock ifException,
                       ArrayList<Value> args, String name, BasicBlock insertAtEnd) {
      return new InvokeInst(func, ifNormal, ifException, args, name, insertAtEnd);
    }

    private InvokeInst(Value func, BasicBlock ifNormal, BasicBlock ifException,
                       ArrayList<Value> args, String name, Instruction insertBefore) {
      super(((FunctionType)((PointerType)func.getType()).getElementType()).getReturnType(),
              Invoke, name, insertBefore);
      init(func, ifNormal, ifException, args, name);
    }

    private InvokeInst(Value func, BasicBlock ifNormal, BasicBlock ifException,
                       ArrayList<Value> args, String name, BasicBlock insertAtEnd) {
      super(((FunctionType)((PointerType)func.getType()).getElementType()).getReturnType(),
              Invoke, name, insertAtEnd);
      init(func, ifNormal, ifException, args, name);
    }

    private void init(Value func, BasicBlock ifNormal, BasicBlock ifException,
                      ArrayList<Value> args, String name) {
      reserve(3+args.size());
      operandList[0] = new Use(func, this);
      operandList[1] = new Use(ifNormal, this);
      operandList[2] = new Use(ifException, this);
      int i = 3;
      for (Value arg : args)
        setOperand(i++, arg, this);
      setName(name);
    }

    public int getNumArgOperands() {
      return getNumOfOperands() - 3;
    }

    public BasicBlock getNormalDest() {
      return (BasicBlock) operandList[1].getValue();
    }

    public void setNormalDest(BasicBlock bb) {
      operandList[1].setValue(bb);
    }

    public BasicBlock getUnwindDest() {
      return (BasicBlock) operandList[2].getValue();
    }

    public void setUnwindDest(BasicBlock bb) {
      operandList[2].setValue(bb);
    }

    public Function getCalledFunction() {
      return (Function) operandList[0].getValue();
    }

    public Value getCalledValue() {
      return operandList[0].getValue();
    }

    public void setCalledFunction(Value fn) {
      operandList[0].setValue(fn);
    }

    /**
     * Get the landingpad instruction from the landing pad block (the unwind destination).
     * @return
     */
    public LandingPadInst getLandingPadInst() {
      return (LandingPadInst) getUnwindDest().getInstAt(getUnwindDest().getFirstNonPhi());
    }

    @Override
    public BasicBlock getSuccessor(int index) {
      Util.assertion(index < 2, "successor # out of range for invoke");
      return index == 0 ? getNormalDest() : getUnwindDest();
    }

    @Override
    public int getNumOfSuccessors() {
      return 2;
    }

    @Override
    public void setSuccessor(int index, BasicBlock bb) {
      Util.assertion(index < 2, "successor # out of range for invoke");
      setOperand(index + 1, bb);
    }

    public Value getArgOperand(int i) {
      Util.assertion(i >= 0 && i < getNumArgOperands(), "index out of range");
      return operand(i+3);
    }
    public void setArgOperand(int i, Value v) {
      Util.assertion(i >= 0 && i < getNumArgOperands(), "index out of range");
      setOperand(i+3, v);
    }
    public void setCallingConv(CallingConv cc) { this.cc = cc; }
    public CallingConv getCallingConv() { return cc; }
    public AttrList getAttributes() { return attributes; }
    public void setAttributes(AttrList attrList) { this.attributes = attrList; }
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

    public boolean hasByValArgument() {
      // TODO: 6/4/20
      Util.shouldNotReachHere();
      return false;
    }
  }

  public static class LandingPadInst extends Instruction {
    public enum ClauseType {
      Catch, Filter
    }
    private boolean isCleanup;
    private int numOperands;

    private void init(Value persFn, int numReservedValues, String name) {
      reserve(numReservedValues);
      setName(name);
      numOperands = 1;
      setOperand(0, persFn, this);
      setCleanup(false);
    }

    private LandingPadInst(Type retTy,
                           Value personalityFn,
                           int numReservedValues,
                           String name,
                           Instruction insertBefore) {
      super(retTy, Operator.LandingPad, name, insertBefore);
      init(personalityFn, 1+numReservedValues, name);
    }

    private LandingPadInst(Type retTy,
                           Value personalityFn,
                           int numReservedValues,
                           String name,
                           BasicBlock insertAtEnd) {
      super(retTy, Operator.LandingPad, name, insertAtEnd);
      init(personalityFn, 1+numReservedValues, name);
    }
    public static LandingPadInst create(Type retTy,
                                        Value personalityFn,
                                        int numReservedValues) {
      return create(retTy, personalityFn, numReservedValues, "");
    }

    public static LandingPadInst create(Type retTy,
                                        Value personalityFn,
                                        int numReservedValues,
                                        String name) {
      return create(retTy, personalityFn, numReservedValues, name, (Instruction)null);
    }

    public static LandingPadInst create(Type retTy,
                                        Value personalityFn,
                                        int numReservedValues,
                                        String name,
                                        Instruction insertBefore) {
      return new LandingPadInst(retTy, personalityFn, numReservedValues, name, insertBefore);
    }

    public static LandingPadInst create(Type retTy,
                                        Value personalityFn,
                                        int numReservedValues,
                                        String name,
                                        BasicBlock insertAtEnd) {
      return new LandingPadInst(retTy, personalityFn, numReservedValues, name, insertAtEnd);
    }

    @Override
    public int getNumOfOperands() {
      return numOperands;
    }

    public Value getPersonalityFn() { return operand(0); }
    public boolean isCleanup() { return isCleanup; }
    public void setCleanup(boolean cleanup) { isCleanup = cleanup; }
    public void addClause(Value v) {
      int opNo = getNumOfOperands();
      Util.assertion(opNo < operandList.length);
      ++numOperands;
      setOperand(opNo, v, this);
    }

    public Value getClause(int idx) { return operand(idx + 1);}
    public boolean isCatch(int idx) { return !(operand(idx+1).getType() instanceof ArrayType);}
    public boolean isFilter(int idx) { return operand(idx+1).getType() instanceof ArrayType;}
    public int getNumClauses() { return getNumOfOperands() - 1; }
  }

  /**
   * FunctionProto invocation instruction.
   *
   * @author Jianping Zeng
   */
  public static class CallInst extends Instruction {
    private CallingConv callingConv;
    private boolean tailCall;
    private AttrList attributes;

    protected CallInst(Value[] args, Value target) {
      this(args, target, "");
    }

    protected CallInst(Value callee, List<Value> args) {
      this(callee, args, "");
    }

    protected CallInst(Value callee, List<Value> args,
                    String name) {
      this(callee, args, name, null);
    }

    protected CallInst(Value callee, List<Value> args,
                    String name, Instruction insertBefore) {
      super(((FunctionType) ((PointerType) callee.getType()).
              getElementType()).getReturnType(),
          Operator.Call, name, insertBefore);
      Value[] tmp = new Value[args.size()];
      args.toArray(tmp);
      init(callee, tmp);
    }

    /**
     * Constructs a new method calling instruction.
     *
     * @param args   The input arguments.
     * @param target The called method.
     */
    protected CallInst(Value[] args, Value target, String name) {
      this(args, target, name, (Instruction) null);
    }

    /**
     * Constructs a new method calling instruction.
     *
     * @param args   The input arguments.
     * @param target The called method.
     */
    protected CallInst(Value[] args, Value target,
                    String name, Instruction insertBefore) {
      super(((FunctionType) ((PointerType) target.getType()).
              getElementType()).getReturnType(),
          Operator.Call, name, insertBefore);
      init(target, args);
    }

    protected CallInst(Value[] args, Value target,
                    String name, BasicBlock insertAtEnd) {
      super(((FunctionType) ((PointerType) target.getType()).
              getElementType()).getReturnType(),
          Operator.Call, name, insertAtEnd);
      init(target, args);
    }

    public static CallInst create(Value target, Value[] args, String name, Instruction insertBefore) {
      CallInst ci = null;
      if (target instanceof Function && ((Function)target).isIntrinsicID())
        ci = IntrinsicInst.create(target, args, name, insertBefore);
      return ci == null ? new CallInst(args, target, name, insertBefore) : ci;
    }

    public static CallInst create(Value target, List<Value> args, String name, Instruction insertBefore) {
      Value[] arr = new Value[args.size()];
      args.toArray(arr);
      return create(target, arr, name, insertBefore);
    }

    public static CallInst create(Value target, Value[] args, String name, BasicBlock insertAtEnd) {
      Util.assertion(target instanceof Function);
      Function fn = (Function) target;
      CallInst ci = null;
      if (fn.isIntrinsicID())
        ci = IntrinsicInst.create(target, args, name, insertAtEnd);
      return ci != null ? ci: new CallInst(args, target, name, insertAtEnd);
    }

    public static CallInst create(Value target, List<Value> args, String name, BasicBlock insertAtEnd) {
      Value[] arr = new Value[args.size()];
      args.toArray(arr);
      return create(target, arr, name, insertAtEnd);
    }

    public static Instruction createMalloc(BasicBlock insertAtEnd,
                                           Type intPtrTy,
                                           Type allocTy,
                                           Value allocSize,
                                           Value arraySize,
                                           Function mallocF,
                                           String name) {
      return createMalloc(null, insertAtEnd, intPtrTy, allocTy, allocSize, arraySize, mallocF, name);
    }

    public static Instruction createMalloc(Instruction insertBefore,
                                           BasicBlock insertAtEnd,
                                           Type intPtrTy,
                                           Type allocTy,
                                           Value allocSize,
                                           Value arraySize,
                                           Function mallocF,
                                           String name) {
      Util.assertion((insertBefore == null && insertAtEnd != null) ||
              (insertBefore != null && insertAtEnd == null),
          "createMalloc needs either insertBefore or insertAtEnd");
      // malloc(type) becomes:
      //       bitcast (i8* malloc(typeSize)) to type*
      // malloc(type, arraySize) becomes:
      //       bitcast (i8 *malloc(typeSize*arraySize)) to type*
      if (arraySize == null)
        arraySize = ConstantInt.get(intPtrTy, 1);
      else if (!arraySize.getType().equals(intPtrTy)) {
        if (insertBefore != null)
          arraySize = CastInst.createIntegerCast(arraySize, intPtrTy, false,
              "", insertBefore);
        else
          arraySize = CastInst.createIntegerCast(arraySize, intPtrTy, false,
              "", insertAtEnd);
      }

      if (!isConstantOne(arraySize)) {
        if (isConstantOne(allocSize)) {
          allocSize = arraySize;  // Operand * 1 = Operand
        } else if (arraySize instanceof Constant) {
          Constant co = (Constant) arraySize;
          Constant scale = ConstantExpr.getIntegerCast(co, intPtrTy, false);
          // Malloc arg is constant product of type size and array size
          allocSize = ConstantExpr.getMul(scale, (Constant) allocSize);
        } else {
          // Multiply type size by the array size...
          if (insertBefore != null)
            allocSize = BinaryOperator.createMul(arraySize, allocSize, "mallocsize", insertBefore);
          else
            allocSize = BinaryOperator.createMul(arraySize, allocSize, "mallocsize", insertAtEnd);
        }
      }

      Util.assertion(allocSize.getType().equals(intPtrTy), "malloc arg is wrong size!");
      // create a call to malloc.
      BasicBlock bb = insertBefore != null ? insertBefore.getParent() : insertAtEnd;
      Module m = bb.getParent().getParent();
      Type bpTy = Type.getInt8PtrTy(bb.getContext(), 0);
      if (mallocF == null)
        // prototype malloc as "void *malloc(size_t)"
        mallocF = (Function) m.getOrInsertFunction("malloc", bpTy, intPtrTy, null);
      PointerType allocPtrType = PointerType.getUnqual(allocTy);
      CallInst mcall = null;
      Instruction result = null;
      if (insertBefore != null) {
        mcall = new CallInst(new Value[]{allocSize}, mallocF, "malloccall", insertBefore);
        result = mcall;
        if (!result.getType().equals(allocPtrType)) {
          // create a cast instruction to convert to the right type.
          result = new BitCastInst(mcall, allocPtrType, name);
        }
      }
      else {
        mcall = new CallInst(new Value[] {allocSize}, mallocF, "malloccall");
        result = mcall;
        if (!result.getType().equals(allocPtrType)) {
          insertAtEnd.appendInst(mcall);
          // create a cast instruction to convert to the right type.
          result = new BitCastInst(mcall, allocPtrType, name);
        }
      }
      mcall.setTailCall(true);
      mcall.setCallingConv(mallocF.getCallingConv());
      mallocF.setDoesNotAlias(0);
      Util.assertion(!mcall.getType().isVoidType(), "malloc has void return type!");
      return result;
    }

    /**
     * Return true only if val is constant int 1
     * @param value
     * @return
     */
    private static boolean isConstantOne(Value value) {
      Util.assertion(value != null, "doesn't work with null ptr!");
      return value instanceof ConstantInt && ((ConstantInt)value).isOne();
    }


    private void init(Value target, Value[] args) {
      reserve(1 + args.length);
      Util.assertion((getNumOfOperands() == args.length+1), "NumOperands not set up?");
      operandList[0] = new Use(target, this);
      int idx = 1;
      for (Value arg : args) {
        setOperand(idx++, arg, this);
      }
      attributes = new AttrList();
    }

    public Value getArgOperand(int index) {
      Util.assertion(index >= 0 && index < getNumArgOperands());
      return operand(index+1);
    }

    public void setArgOperand(int index, Value val) {
      Util.assertion(index >= 0 && index < getNumArgOperands());
      setOperand(index+1, val);
    }

    public int getNumArgOperands() { return getNumOfOperands() - 1; }

    /**
     * Return the called function, if return null indicates this is an indirect call.
     * @return
     */
    public Function getCalledFunction() {
      return operandList[0].getValue()  instanceof Function ? (Function)operandList[0].getValue() : null;
    }

    public void setCalledFunction(Value fn) { operandList[0].setValue(fn); }

    public Value getCalledValue() { return operandList[0].getValue(); }

    public void setCalledValue(Value val) { operandList[0].setValue(val); }

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
      if (attributes == null)
        this.attributes = new AttrList();
      else
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
    private int numCases;

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
      super(Type.getVoidTy(condV.getContext()), Operator.Switch, name, insertBefore);
      init(condV, defaultBB, numCases * 2);
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
      this.numCases = 1;
    }

    public void addCase(Constant caseVal, BasicBlock targetBB) {
      int opNo = getNumOfCases();
      setOperand(opNo*2, caseVal, this);
      setOperand(opNo*2 + 1, targetBB, this);
      ++numCases;
    }

    public void removeCase(int idx) {
      Util.assertion((idx != 0), "Cannot remove the default case!");
      Util.assertion((idx * 2 < getNumOfOperands()), "Successor index out of range!!!");

      // unlink the last value.
      if (getNumOfOperands() - idx * 2 + 2 >= 0)
        System.arraycopy(operandList, idx * 2 + 2, operandList, idx * 2 + 2 - 2, getNumOfOperands() - idx * 2 + 2);
      numOps -= 2;
      --numCases;
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
      return numCases;
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
      inst.operandList = new Use[getNumOfOperands()];
      System.arraycopy(operandList, 0, inst.operandList, 0, getNumOfOperands());
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
    private int opIndex;

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
      Util.assertion(opIndex + 2 <= getNumOfOperands(), "no enough space for inserting a pair of incoming value");
      operandList[opIndex] = new Use(value, this);
      operandList[opIndex + 1] = new Use(block, this);
      opIndex += 2;
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
        operandList[i - 2] = operandList[i];
        operandList[i - 1] = operandList[i + 1];
      }
      opIndex -= 2;
      operandList[numOps - 2] = null;
      operandList[numOps - 1] = null;

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
          arraySize == null ? ConstantInt.get(Type.getInt32Ty(ty.getContext()), 1) :
              arraySize, alignment, name, insertBefore);
      Util.assertion(getArraySize().getType().isIntegral(), "The type of allocated size is not i32");
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
      return !operand(0).equals(ConstantInt.get(Type.getInt32Ty(getContext()), 1));
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
      super(Type.getVoidTy(value.getContext()), Operator.Store, name, insertBefore);
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
      super(Type.getVoidTy(value.getContext()), Operator.Store, name, insertAtEnd);
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
  public static class GetElementPtrInst extends Instruction implements GEPOperator {
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
      reserve(indices.size() + 1);
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

    @Override
    public int getPointerOperandIndex() {
      return 0;
    }

    public PointerType getPointerOperandType() {
      return (PointerType) getPointerOperand().getType();
    }

    @Override
    public int getNumIndices() {
      return getNumOfOperands() - 1;
    }

    public boolean hasIndices() {
      return getNumOfOperands() > 1;
    }

    @Override
    public boolean hasAllZeroIndices() {
      for (int i = getIndexBegin(), e = getIndexEnd(); i < e; i++) {
        if (operand(i) instanceof Constant) {
          Constant c = (Constant) operand(i);
          if (!c.isNullValue()) return false;
        }
      }
      return true;
    }

    public int getIndexBegin() {
      return 1;
    }

    public int getIndexEnd() {
      return getNumOfOperands();
    }

    public void setIsInBounds(boolean inbounds) {
      this.inbounds = inbounds;
    }

    public boolean isInBounds() {
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

      if (index.getType().isIntegerTy(32))
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
      if (!idx.getType().isIntegerTy(32))
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
          !maskTy.getElementType().isIntegerTy(32))
        return false;
      return true;
    }

    /**
     * Return the full mask for this instrucion, where each element is the element
     * number and undef's are returned as -1.
     * @param mask
     * @param result
     */
    public static void getShuffleMask(Constant mask, TIntArrayList result) {
      Util.assertion(mask.getType().isVectorTy(), "not a vector constant!");

      if (mask instanceof ConstantVector) {
        ConstantVector conVec = (ConstantVector) mask;
        for (int i = 0, e = conVec.getNumOfOperands(); i < e; ++i) {
          Constant elt = conVec.operand(i);
          if (elt instanceof UndefValue)
            result.add(-1);
          else
            result.add((int) ((ConstantInt)elt).getSExtValue());
        }
        return;
      }

      VectorType vt = (VectorType)mask.getType();
      if (mask instanceof ConstantAggregateZero) {
        for (int i = 0, e = (int) vt.getNumElements(); i < e; ++i)
          result.add(0);
        return;
      }

      Util.shouldNotReachHere("Unknown type, must be constant expr etc!");
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
      reserve(2);
      setOperand(0, agg, this);
      setOperand(1, val, this);
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
                           int idx,
                           String name) {
      this(agg, val, new int[] {idx}, name);
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
