package backend.value;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2019, Jianping Zeng
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

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class BinaryConstantExpr extends ConstantExpr {
  /**
   * Constructs a new instruction representing the specified constants.
   *
   * @param opcode
   */
  private BinaryConstantExpr(Operator opcode, Constant c1, Constant c2) {
    super(c1.getType(), opcode);
    reserve(2);
    setOperand(0, c1, this);
    setOperand(1, c2, this);
  }

  public static BinaryConstantExpr create(Operator opcode, Constant c1, Constant c2) {
    if (opcode == Operator.Add || opcode == Operator.Sub || opcode == Operator.Mul)
      return new OverflowBinaryConstantExpr(opcode, c1, c2);
    else
      return new BinaryConstantExpr(opcode, c1, c2);
  }

  public static class OverflowBinaryConstantExpr extends BinaryConstantExpr implements OverflowBinaryOperator {
    /**
     * Indicates the operation (such as add, sub, mul)
     * doesn't have extra bits to been destroyed.
     */
    private boolean hasNoUnsignedWrap;
    private boolean hasNoSignedWrap;

    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param opcode
     * @param c1
     * @param c2
     */
    private OverflowBinaryConstantExpr(Operator opcode, Constant c1, Constant c2) {
      super(opcode, c1, c2);
    }

    @Override
    public void setHasNoUnsignedWrap(boolean val) {
      hasNoUnsignedWrap = val;
    }

    @Override
    public boolean getHasNoUnsignedWrap() {
      return hasNoUnsignedWrap;
    }

    @Override
    public void setHasNoSignedWrap(boolean val) {
      hasNoSignedWrap = val;
    }

    @Override
    public boolean getHasNoSignedWrap() {
      return hasNoSignedWrap;
    }
  }
}
