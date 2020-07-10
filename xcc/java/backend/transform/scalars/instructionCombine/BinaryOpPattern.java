package backend.transform.scalars.instructionCombine;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2017, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.value.ConstantExpr;
import backend.value.Instruction;
import backend.value.Operator;
import backend.value.Value;
import tools.Util;

public class BinaryOpPattern implements Pattern {
  private Operator opc;
  private Pattern lhs;
  private Pattern rhs;

  private BinaryOpPattern(Operator opc, Pattern lhs, Pattern rhs) {
    this.opc = opc;
    this.lhs = lhs;
    this.rhs = rhs;
    Util.assertion(opc.isBinaryOps(), "Non binary ops for BinaryOpPattern.");
  }

  @Override
  public boolean match(Value valueToMatch) {
    if (valueToMatch instanceof Instruction) {
      Instruction inst = (Instruction) valueToMatch;
      if (inst.getOpcode() != opc)
        return false;
      return lhs.match(inst.operand(0)) && rhs.match(inst.operand(1));
    } else if (valueToMatch instanceof ConstantExpr) {
      ConstantExpr ce = (ConstantExpr) valueToMatch;
      if (ce.getOpcode() != opc)
        return false;
      return lhs.match(ce.operand(0)) && rhs.match(ce.operand(1));
    }
    return false;
  }

  public static Pattern mAdd(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.Add, lhs, rhs);
  }

  public static Pattern mFAdd(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.FAdd, lhs, rhs);
  }

  public static Pattern mSub(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.Sub, lhs, rhs);
  }

  public static Pattern mFSub(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.FSub, lhs, rhs);
  }

  public static Pattern mSDiv(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.SDiv, lhs, rhs);
  }

  public static Pattern mUDiv(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.UDiv, lhs, rhs);
  }

  public static Pattern mFDiv(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.FDiv, lhs, rhs);
  }

  public static Pattern mURem(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.URem, lhs, rhs);
  }

  public static Pattern mSRem(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.SRem, lhs, rhs);
  }

  public static Pattern mFRem(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.FRem, lhs, rhs);
  }

  public static Pattern mAnd(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.And, lhs, rhs);
  }

  public static Pattern mOr(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.Or, lhs, rhs);
  }

  public static Pattern mXor(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.Xor, lhs, rhs);
  }

  public static Pattern mShl(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.Shl, lhs, rhs);
  }

  public static Pattern mAShr(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.AShr, lhs, rhs);
  }

  public static Pattern mLShr(Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(Operator.LShr, lhs, rhs);
  }

  public static Pattern mBinaryOps(Operator opc, Pattern lhs, Pattern rhs) {
    return new BinaryOpPattern(opc, lhs, rhs);
  }
}
