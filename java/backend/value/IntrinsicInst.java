package backend.value;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.intrinsic.Intrinsic;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class IntrinsicInst extends Instruction.CallInst {
  public IntrinsicInst() {
    super((Value[]) null, null);
  }

  public Intrinsic.ID getIntrinsicID() {
    return getCalledFunction().getIntrinsicID();
  }

  /**
   * A common base class for llvm debug information.
   */
  public static abstract class DbgInfoIntrinsic extends IntrinsicInst {
    public static Value castOperand(Value val) {
      if (val instanceof ConstantExpr) {
        ConstantExpr ce = (ConstantExpr) val;
        if (ce.isCast())
          return ce.operand(0);
      }
      return null;
    }

    public static Value stripCast(Value val) {
      Value res = castOperand(val);
      if (res != null)
        res = stripCast(res);
      else if (val instanceof GlobalVariable) {
        GlobalVariable gv = (GlobalVariable) val;
        if (gv.hasInitializer()) {
          res = castOperand(gv.getInitializer());
          if (res != null)
            res = stripCast(res);
        }
      }
      return res;
    }
  }
}
