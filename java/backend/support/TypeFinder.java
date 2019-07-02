/*
 * Extremely C language Compiler.
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

package backend.support;

import backend.type.OpaqueType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.*;
import backend.value.Module;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

public class TypeFinder {
  private HashSet<Value> visitedConstants;
  private HashSet<Type> visitedTypes;
  private ArrayList<StructType> structTypes;

  public TypeFinder(ArrayList<StructType> namedTypes) {
    visitedConstants = new HashSet<>();
    visitedTypes = new HashSet<>();
    structTypes = namedTypes;
  }

  public void run(Module m) {
    // Get types for type definition in Module.
    TreeMap<String, Type> st = m.getTypeSymbolTable();
    for (Type ty : st.values()) {
      if (ty instanceof OpaqueType)
        incorporateType(ty.getForwardType());
      else
        incorporateType(ty);
    }

    // Get the type of GlobalVariable.
    for (GlobalVariable gv : m.getGlobalVariableList()) {
      incorporateType(gv.getType());
      if (gv.hasInitializer())
        incorperateValue(gv.getInitializer());
    }

    // Get types from functions.
    for (Function f : m.getFunctionList()) {
      for (BasicBlock bb : f.getBasicBlockList()) {
        for (Instruction inst : bb) {
          incorporateType(inst.getType());
          for (int i = 0, e = inst.getNumOfOperands(); i != e; i++) {
            incorperateValue(inst.operand(i));
          }
        }
      }
    }
  }

  private void incorperateValue(Value val) {
    if (!(val instanceof Constant) || val instanceof GlobalValue)
      return;

    if (!visitedConstants.add(val))
      return;

    incorporateType(val.getType());

    Constant c = (Constant) val;
    for (int i = 0, e = c.getNumOfOperands(); i != e; i++)
      incorperateValue(c.operand(i));
  }

  private void incorporateType(Type ty) {
    if (!visitedTypes.add(ty))
      return;

    if (ty instanceof StructType)
      structTypes.add((StructType) ty);

    // Recursively walk all contained types.
    for (int i = 0, e = ty.getNumContainedTypes(); i != e; i++)
      incorporateType(ty.getContainedType(i));
  }
}
