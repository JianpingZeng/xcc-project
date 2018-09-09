package backend.transform.scalars.instructionCombine;

/*
 * Extremely C language Compiler
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

import backend.value.*;

public class NotPattern implements Pattern {
  private Pattern op;

  private NotPattern(Pattern op) {
    this.op = op;
  }

  @Override
  public boolean match(Value valueToMatch) {
    if (valueToMatch instanceof Instruction) {
      Instruction inst = (Instruction) valueToMatch;
      if (inst.getOpcode() == Operator.Xor)
        return matchIfNot(inst.operand(0), inst.operand(1));
    }
    if (valueToMatch instanceof ConstantExpr) {
      ConstantExpr ce = (ConstantExpr) valueToMatch;
      if (ce.getOpcode() == Operator.Xor)
        return matchIfNot(ce.operand(0), ce.operand(1));
    }
    if (valueToMatch instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) valueToMatch;
      return op.match(ConstantExpr.getNot(ci));
    }
    return false;
  }

  private boolean matchIfNot(Value lhs, Value rhs) {
    if (rhs instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) rhs;
      return ci.isAllOnesValue() && op.match(lhs);
    }
    if (lhs instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) lhs;
      return ci.isAllOnesValue() && op.match(rhs);
    }
    if (rhs instanceof ConstantVector) {
      ConstantVector cv = (ConstantVector) rhs;
      return cv.isAllOnesValue() && op.match(lhs);
    }
    if (lhs instanceof ConstantVector) {
      ConstantVector cv = (ConstantVector) lhs;
      return cv.isAllOnesValue() && op.match(rhs);
    }
    return false;
  }

  public static Pattern mNot(Pattern op) {
    return new NotPattern(op);
  }
}
