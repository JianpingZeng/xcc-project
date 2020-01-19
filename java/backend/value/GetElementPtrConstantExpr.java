package backend.value;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.type.PointerType;
import backend.type.Type;

import java.util.ArrayList;

import static backend.value.Operator.GetElementPtr;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class GetElementPtrConstantExpr extends ConstantExpr implements GEPOperator {
  private boolean isInBounds;

  /**
   * Constructs a new instruction representing the specified constants.
   *
   * @param ty
   */
  public GetElementPtrConstantExpr(Constant c, ArrayList<Constant> idxList,
                                   Type ty, boolean isInBounds) {
    super(ty, GetElementPtr);
    reserve(idxList.size() + 1);
    setOperand(0, c, this);
    for (int i = 0, e = idxList.size(); i != e; i++)
      setOperand(i + 1, idxList.get(i), this);
    this.isInBounds = isInBounds;
  }

  public void setIsInBounds(boolean isInBounds) {
    this.isInBounds = isInBounds;
  }

  @Override
  public int getIndexBegin() {
    return 1;
  }

  @Override
  public int getIndexEnd() {
    return getNumOfOperands() - 1;
  }

  @Override
  public Value getPointerOperand() {
    return operand(0);
  }

  @Override
  public int getPointerOperandIndex() {
    return 0;
  }

  @Override
  public PointerType getPointerOperandType() {
    return (PointerType) operand(0).getType();
  }

  @Override
  public int getNumIndices() {
    return getNumOfOperands() - 1;
  }

  @Override
  public boolean hasIndices() {
    return getNumIndices() > 0;
  }

  @Override
  public boolean hasAllZeroIndices() {
    for (int i = getIndexBegin(), e = getIndexEnd(); i < e; i++) {
      Constant c = operand(i);
      if (!c.isNullValue())
        return false;
    }
    return true;
  }

  @Override
  public boolean hasAllConstantIndices() {
    return true;
  }

  public boolean isInBounds() {
    return isInBounds;
  }
}
