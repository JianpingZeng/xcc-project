/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

package backend.value;

import backend.support.LLVMContext;
import backend.type.Type;
import backend.value.UniqueConstantValueImpl.MDNodeKeyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static backend.value.UniqueConstantValueImpl.getUniqueImpl;
import static backend.value.ValueKind.MDNodeVal;

/**
 * @author Jianping Zeng
 * @version 0.6
 */
public class MDNode extends User {
  private final boolean functionLocal;
  private int slotID;

  /**
   * A flag indicates whether this MDNode is created for upward reference.
   */
  private boolean isTemporary;

  // FunctionLocal enums.
  enum FunctionLocalness {
    FL_Unknown,
    FL_No,
    FL_Yes
  }

  MDNode(LLVMContext ctx, List<Value> vals, boolean isFunctionLocal) {
    super(Type.getMetadataTy(ctx), MDNodeVal);
    reserve(vals.size());
    functionLocal = isFunctionLocal;
    for (int i = 0, e = vals.size(); i <  e; ++i)
      setOperand(i, vals.get(i));
  }

  public static MDNode get(LLVMContext context, List<Value> vals) {
    return get(context, vals, FunctionLocalness.FL_Unknown);
  }

  public static MDNode get(LLVMContext context, Value[] vals) {
    return get(context, Arrays.asList(vals));
  }

  public static MDNode get(LLVMContext context, List<Value> vals, FunctionLocalness fl) {
    MDNodeKeyType key = new MDNodeKeyType(vals);
    return getUniqueImpl().getOrCreate(context, key, fl);
  }

  public static MDNode get(LLVMContext context, List<Value> vals, boolean isFunctionLocal) {
    return get(context, vals, isFunctionLocal ? FunctionLocalness.FL_Yes : FunctionLocalness.FL_No);
  }

  public static MDNode getTemporary(LLVMContext context, Value[] vals) {
    MDNode res = new MDNode(context, vals!=null ? Arrays.asList(vals) : new ArrayList<>(), false);
    res.isTemporary = true;
    return res;
  }

  static boolean isFunctionLocalValue(Value v) {
    return v instanceof Instruction || v instanceof Argument ||
        v instanceof BasicBlock || (v instanceof MDNode && ((MDNode)v).isFunctionLocal());
  }

  public boolean isFunctionLocal() {
    return functionLocal;
  }

  /**
   * If this metadata node is a function-local metadata, ret the first function.
   * @return
   */
  public Function getFunction() {
    if (!isFunctionLocal()) return null;
    for (int i = 0, e = getNumOfOperands(); i < e; i++) {
      Function f = getFunctionForValue(operand(i));
      if (f != null) return f;
    }
    return null;
  }

  private static Function getFunctionForValue(Value v) {
    if (v == null) return null;
    if (v instanceof Instruction) {
      BasicBlock bb = ((Instruction)v).getParent();
      return bb != null ? bb.getParent() : null;
    }

    if (v instanceof Argument) {
      return ((Argument)v).getParent();
    }
    if (v instanceof BasicBlock) {
      return ((BasicBlock)v).getParent();
    }
    if (v instanceof MDNode) {
      return ((MDNode)v).getFunction();
    }
    return null;
  }

  public boolean isTemporary() { return isTemporary; }

  public void setSlotID(int slotID) { this.slotID = slotID; }

  public int getSlotID() { return slotID; }
}
