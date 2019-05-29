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
import backend.type.VectorType;
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

  private void init(Value cond, Value lhs, Value rhs) {
    Util.assertion(areInvalidOperands(cond, lhs, rhs) == null, "Invalid operands!");
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

  public static String areInvalidOperands(Value op0, Value op1, Value op2) {
    if (!op1.getType().equals(op2.getType()))
      return "both values to select must have same type";

    if (op0.getType() instanceof VectorType) {
      VectorType vty = (VectorType) op0.getType();
      // vector select.
      if (!vty.getElementType().equals(LLVMContext.Int1Ty))
        return "vector select condition element type must be i1";
      if (!(op1.getType() instanceof VectorType))
        return "selected values for vector selection must be vector type";
      VectorType et = (VectorType) op1.getType();
      if (et.getNumElements() != vty.getNumElements())
        return "vector select requires selected vectors to have the same vector length as select condition";
    }
    else if (!op0.getType().equals(LLVMContext.Int1Ty))
      return "select condition must be i1 or <n x i1>";

    return null;
  }
}
