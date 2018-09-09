package backend.value;

import static backend.value.Operator.Flags.ASSOCIATIVE;
import static backend.value.Operator.Flags.COMMUTATIVE;

/**
 * This file defines a enumerator that isDeclScope all of operators which represents
 * as a integer in Module instruction.
 *
 * @author Jianping Zeng
 */
public enum Operator {
  None("illegal", -1, 0),
  // terminators operation.
  Ret("ret", 0, 0),
  Br("br", Ret.index + 1, 0),
  Switch("switch", Br.index + 1, 0),
  Unreachable("unreachable", Switch.index + 1, 0),
  // binary operator

  // add
  Add("add", Unreachable.index + 1, COMMUTATIVE | ASSOCIATIVE),
  FAdd("fadd", Add.index + 1, COMMUTATIVE | ASSOCIATIVE),

  // subtractive
  Sub("sub", FAdd.index + 1, ASSOCIATIVE),
  FSub("fsub", Sub.index + 1, ASSOCIATIVE),

  // multiple
  Mul("mul", FSub.index + 1, COMMUTATIVE | ASSOCIATIVE),
  FMul("fmul", Mul.index + 1, COMMUTATIVE | ASSOCIATIVE),

  // division
  UDiv("udiv", FMul.index + 1, ASSOCIATIVE),
  SDiv("sdiv", UDiv.index + 1, ASSOCIATIVE),
  FDiv("fdiv", UDiv.index + 1, ASSOCIATIVE),

  // mod operation
  URem("urem", FDiv.index + 1, ASSOCIATIVE),
  SRem("srem", URem.index + 1, ASSOCIATIVE),
  FRem("frem", SRem.index + 1, ASSOCIATIVE),

  // bit-operation
  And("and", SRem.index + 1, ASSOCIATIVE | COMMUTATIVE),

  Or("or", And.index + 1, ASSOCIATIVE | COMMUTATIVE),

  Xor("xor", Or.index + 1, ASSOCIATIVE | COMMUTATIVE),

  // comparison operation
  ICmp("icmp", Xor.index + 1, 0),
  FCmp("fcmp", ICmp.index + 1, 0),

  // shift operation
  Shl("shl", FCmp.index + 1, 0),
  LShr("lshr", Shl.index + 1, 0),
  AShr("ashr", LShr.index + 1, 0),

  // converts operation
  //truncate integers.
  Trunc("trunc", AShr.index + 1, 0),
  // zero extend integers.
  ZExt("zext", Trunc.index + 1, 0),
  // Sign extend integers.
  SExt("sext", ZExt.index + 1, 0),
  // floatint-pint to unsigned integer.
  FPToUI("fptoui", SExt.index + 1, 0),
  // floating point to signed integer.
  FPToSI("fptosi", FPToUI.index + 1, 0),
  // unsigned integer to floating-point.
  UIToFP("uitofp", FPToSI.index + 1, 0),
  // signed integer to floating-point.
  SIToFP("sitofp", UIToFP.index + 1, 0),
  // floating point truncate.
  FPTrunc("fptrunc", SIToFP.index + 1, 0),
  // float point extend.
  FPExt("fpext", FPTrunc.index + 1, 0),
  // pointer to integer.
  PtrToInt("ptrtoint", FPExt.index + 1, 0),
  // Integer to pointer.
  IntToPtr("inttoptr", PtrToInt.index + 1, 0),
  // type cast.
  BitCast("bitcast", IntToPtr.index + 1, 0),

  // memory operation
  Alloca("alloca", BitCast.index + 1, 0),
  Free("free", Alloca.index + 1, 0),
  Malloc("malloc", Free.index + 1, 0),
  Store("store", Malloc.index + 1, 0),
  Load("load", Store.index + 1, 0),

  // other operation
  Phi("phi", Load.index + 1, 0),
  Call("call", Phi.index + 1, 0),
  GetElementPtr("getelementptr", Call.index + 1, 0),

  // Select instruction acts as ?: operator in C language.
  Select("select", GetElementPtr.index + 1, 0);

  public final String opName;
  public final int index;
  private int flags;

  Operator(String name, int index, int flag) {
    this.opName = name;
    this.index = index;
    this.flags |= flag;
  }

  public static Operator BinaryOpsBegin = Add;
  public static Operator BinaryOpsEnd = Trunc;

  public static Operator CastOpsBegin = Trunc;
  public static Operator CastOpsEnd = BitCast;

  static class Flags {
    public static final int COMMUTATIVE = 0x1;
    public static final int ASSOCIATIVE = 0x2;
  }

  /**
   * Determines if this operator is commutative.
   *
   * @return
   */
  public boolean isCommutative() {
    return ((this.flags & 0xff) & COMMUTATIVE) != 0;
  }

  /**
   * Check if this operator is an additive operation. ReturnInst true if
   * condition is satisfied, otherwise return false.
   *
   * @return
   */
  public boolean isAdd() {
    return this.index >= Add.index && this.index <= FAdd.index;
  }

  /**
   * Determines whether this operator is sub operator.
   *
   * @return
   */
  public boolean isSub() {
    return index >= Sub.index && index <= FSub.index;
  }

  /**
   * Determines whether this operator is Multiple operator.
   *
   * @return
   */
  public boolean isMul() {
    return index >= Mul.index && index <= FMul.index;
  }

  public boolean isComparison() {
    return index >= ICmp.index && index <= FCmp.index;
  }

  public boolean isBinaryOps() {
    return index >= BinaryOpsBegin.index && index < BinaryOpsEnd.index;
  }

  public boolean isCastOps() {
    return index >= CastOpsBegin.index && index <= CastOpsEnd.index;
  }

  public boolean isGEP() {
    return this == GetElementPtr;
  }

  public boolean isShift() {
    return index >= Shl.index && index <= AShr.index;
  }
}
