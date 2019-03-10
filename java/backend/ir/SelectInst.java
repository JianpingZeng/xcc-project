/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.ir;

import backend.support.LLVMContext;
import backend.value.BasicBlock;
import backend.value.Instruction;
import backend.value.Operator;
import backend.value.Value;
import tools.Util;

/**
 * An instruction used for implementing machinism of ternery operator "?:" of
 * C/C++ language in the level of LLVM IR.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class SelectInst extends Instruction {
  /**
   * An checking on whether the specified operands by formal arguments are
   * valid or not.
   *
   * @param cond
   * @param lhs
   * @param rhs
   * @return
   */
  private static boolean areValidOperands(Value cond, Value lhs, Value rhs) {
    if (!lhs.getType().equals(rhs.getType()))
      return false;
    if (!cond.getType().equals(LLVMContext.Int1Ty))
      return false;
    return true;
  }

  private void init(Value cond, Value lhs, Value rhs) {
    Util.assertion(areValidOperands(cond, lhs, rhs), "Invalid operands!");
    reserve(3);
    setOperand(0, cond, this);
    setOperand(1, lhs, this);
    setOperand(2, rhs, this);
  }

  public SelectInst(Value cond, Value lhs, Value rhs,
                    String name,
                    Instruction insertBefore) {
    super(lhs.getType(), Operator.Select, name, insertBefore);
    init(cond, lhs, rhs);
  }

  public SelectInst(Value cond, Value lhs, Value rhs, String name) {
    this(cond, lhs, rhs, name, (Instruction) null);
  }

  public SelectInst(
      Value cond,
      Value lhs,
      Value rhs,
      String name,
      BasicBlock insertAtEnd) {
    super(lhs.getType(), Operator.Select, name, insertAtEnd);
    init(cond, lhs, rhs);
  }

  public Value getCondition() {
    return operand(0);
  }

  public Value getTrueValue() {
    return operand(1);
  }

  public Value getFalseValue() {
    return operand(2);
  }

  @Override
  public SelectInst clone() {
    return new SelectInst(getCondition(), getTrueValue(), getTrueValue(), getName());
  }
}
