package backend.value;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.type.Type;

import java.util.ArrayList;

import static backend.value.Operator.GetElementPtr;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class GetElementPtrConstantExpr extends ConstantExpr {
  private boolean isInBounds;

  /**
   * Constructs a new instruction representing the specified constants.
   *
   * @param ty
   */
  public GetElementPtrConstantExpr(Constant c, ArrayList<Constant> idxList,
                                   Type ty) {
    super(ty, GetElementPtr);
    reserve(idxList.size() + 1);
    setOperand(0, c, this);
    for (int i = 0, e = idxList.size(); i != e; i++)
      setOperand(i + 1, idxList.get(i), this);
  }

  public void setIsInBounds(boolean isInBounds) {
    this.isInBounds = isInBounds;
  }

  public boolean isInBounds() {
    return isInBounds;
  }
}
