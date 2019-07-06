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

import backend.intrinsic.Intrinsic;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class IntrinsicInst extends Instruction.CallInst {
  protected IntrinsicInst(Value target, Value[] args, String name, Instruction insertBefore) {
    super(args, target, name, insertBefore);
  }

  protected IntrinsicInst(Value target, Value[] args, String name, BasicBlock insertAtEnd) {
    super(args, target, name, insertAtEnd);
  }

  public Intrinsic.ID getIntrinsicID() {
    return getCalledFunction().getIntrinsicID();
  }

  public static IntrinsicInst create(Value target, Value[] args, String name, Instruction insertBefore) {
    Util.assertion(target instanceof Function);
    Function fn = (Function) target;
    Util.assertion(fn.isIntrinsicID());
    switch (fn.getIntrinsicID()) {
      case dbg_declare:
        return new DbgDeclareInst(target, args, name, insertBefore);
      case dbg_value:
        return new DbgValueInst(target, args, name, insertBefore);
      default:
        Util.shouldNotReachHere("Unknown intrinsic function ID!");
        return null;
    }
  }

  public static IntrinsicInst create(Value target, Value[] args, String name, BasicBlock insertAtEnd) {
    Util.assertion(target instanceof Function);
    Function fn = (Function) target;
    Util.assertion(fn.isIntrinsicID());
    switch (fn.getIntrinsicID()) {
      case dbg_declare:
        return new DbgDeclareInst(target, args, name, insertAtEnd);
      case dbg_value:
        return new DbgValueInst(target, args, name, insertAtEnd);
      default:
        Util.shouldNotReachHere("Unknown intrinsic function ID!");
        return null;
    }
  }


  /**
   * A common base class for llvm debug information.
   */
  public static abstract class DbgInfoIntrinsic extends IntrinsicInst {
    protected DbgInfoIntrinsic(Value target, Value[] args, String name, Instruction insertBefore) {
      super(target, args, name, insertBefore);
    }

    protected DbgInfoIntrinsic(Value target, Value[] args, String name, BasicBlock insertAtEnd) {
      super(target, args, name, insertAtEnd);
    }

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

  public static class DbgDeclareInst extends DbgInfoIntrinsic {
    private DbgDeclareInst(Value target, Value[] args, String name, Instruction insertBefore) {
      super(target, args, name, insertBefore);
    }

    private DbgDeclareInst(Value target, Value[] args, String name, BasicBlock insertAtEnd) {
      super(target, args, name, insertAtEnd);
    }

    public MDNode getVariable() { return (MDNode) getArgOperand(1); }
    public Value getAddress() {
      return getArgOperand(0) instanceof MDNode ? getArgOperand(0) : null;
    }
  }

  public static class DbgValueInst extends DbgInfoIntrinsic {
    private DbgValueInst(Value target, Value[] args, String name, Instruction insertBefore) {
      super(target, args, name, insertBefore);
    }

    private DbgValueInst(Value target, Value[] args, String name, BasicBlock insertAtEnd) {
      super(target, args, name, insertAtEnd);
    }

    public Value getValue() {
      return ((MDNode)getArgOperand(0)).getOperand(0);
    }
    public long getOffset() { return ((ConstantInt)getArgOperand(1)).getZExtValue(); }
    public MDNode getVariable() { return (MDNode) getArgOperand(2); }
  }
}
