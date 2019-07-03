/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
import backend.value.UniqueConstantValueImpl.MDNodeKeyType;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static backend.value.UniqueConstantValueImpl.getUniqueImpl;
import static backend.value.ValueKind.MDNodeVal;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MDNode extends Value {
  private ArrayList<Value> nodes;
  private boolean functionLocal;
  private int numOperands;

  // FunctionLocal enums.
  enum FunctionLocalness {
    FL_Unknown,
    FL_No,
    FL_Yes
  }

  MDNode(List<Value> vals, boolean isFunctionLocal) {
    super(LLVMContext.MetadataTy, MDNodeVal);
    nodes = new ArrayList<>();
    nodes.addAll(vals);
    functionLocal = isFunctionLocal;
  }

  public static MDNode get(List<Value> vals) {
    return get(vals, FunctionLocalness.FL_Unknown);
  }

  public static MDNode get(Value[] vals) {
    return get(Arrays.asList(vals));
  }

  public static MDNode get(List<Value> vals, FunctionLocalness fl) {
    MDNodeKeyType key = new MDNodeKeyType(vals);
    return getUniqueImpl().getOrCreate(key, fl);
  }

  public static MDNode get(List<Value> vals, boolean isFunctionLocal) {
    return get(vals, isFunctionLocal ? FunctionLocalness.FL_Yes : FunctionLocalness.FL_No);
  }

  public static boolean isFunctionLocalValue(Value v) {
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
    for (int i = 0, e = getNumOperands(); i < e; i++) {
      Function f = getFunctionForValue(getOperand(i));
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

  public int getNumOperands() {
    return nodes.size();
  }

  public Value getOperand(int index) {
    Util.assertion(index >= 0 && index < getNumOperands());
    return nodes.get(index);
  }
}
