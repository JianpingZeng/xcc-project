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

package backend.support;

import backend.type.IntegerType;
import backend.type.LLVMTypeID;
import backend.type.Type;
import backend.value.Instruction;
import backend.value.MDNode;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Pair;

import java.util.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class LLVMContext {
  public static final Type VoidTy = new Type(LLVMTypeID.VoidTyID);
  public static final Type LabelTy = new Type(LLVMTypeID.LabelTyID);
  public static final Type MetadataTy = new Type(LLVMTypeID.MetadataTyID);
  public static final IntegerType Int1Ty = IntegerType.get(1);
  public static final IntegerType Int8Ty = IntegerType.get(8);
  public static final IntegerType Int16Ty = IntegerType.get(16);
  public static final IntegerType Int32Ty = IntegerType.get(32);
  public static final IntegerType Int64Ty = IntegerType.get(64);
  public static final Type FloatTy = new Type(LLVMTypeID.FloatTyID);
  public static final Type DoubleTy = new Type(LLVMTypeID.DoubleTyID);
  public static final Type FP128Ty = new Type(LLVMTypeID.FP128TyID);
  public static final Type X86_FP80Ty = new Type(LLVMTypeID.X86_FP80TyID);
  public static final Type PPC_FP128Ty = new Type(LLVMTypeID.PPC_FP128TyID);

  // Pinned metadata names, which always have the same value. This is a compile-time performance
  // optimization, not a correctness optimization.
  public static int MD_dbg = 0;

  private static final TreeMap<String, Integer> customMDKindNamesMap = new TreeMap<>();
  public ArrayList<Pair<MDNode, MDNode>> scopeInlineAtRecords;
  public TObjectIntHashMap<Pair<MDNode, MDNode>> scopeInlineAtIdx;
  public ArrayList<MDNode> scopeRecords;
  public TObjectIntHashMap<MDNode> scopeRecordIdx;
  public HashMap<Instruction, ArrayList<Pair<Integer, MDNode>>> metadataStore;
  private static final LLVMContext globalContext = new LLVMContext();
  public static LLVMContext getGlobalContext() { return globalContext; }

  private LLVMContext() {
    scopeInlineAtRecords = new ArrayList<>();
    scopeInlineAtIdx = new TObjectIntHashMap<>();
    scopeRecords = new ArrayList<>();
    scopeRecordIdx = new TObjectIntHashMap<>();
    metadataStore = new HashMap<>();
  }

  public int getMDKindID(String name) {
    if (customMDKindNamesMap.containsKey(name))
      return customMDKindNamesMap.get(name);

    int val = customMDKindNamesMap.size();
    customMDKindNamesMap.put(name, val);
    return val;
  }

  public int getOrAddScopeRecordIdxEntry(MDNode scope, int existingIdx) {
    // If we already have an entry for this scope, return it.
    if (scopeRecordIdx.containsKey(scope))
      return scopeRecordIdx.get(scope);

    if (existingIdx != 0) {
      scopeRecordIdx.put(scope, existingIdx);
      return existingIdx;
    }

    // otherwise, add an new entry.
    existingIdx = scopeRecords.size() + 1;
    scopeRecords.add(scope);
    scopeRecordIdx.put(scope, existingIdx);
    return existingIdx;
  }

  public int getOrAddScopeInlinedAtIdxEntry(MDNode scope, MDNode inlineAt, int existingIdx) {
    // If there is entry existing, return it.
    Pair<MDNode, MDNode> key = Pair.get(scope, inlineAt);
    if (scopeInlineAtIdx.containsKey(key))
      return scopeInlineAtIdx.get(scope);

    // if the input existingIdx is not zero, use it.
    if (existingIdx != 0) {
      scopeInlineAtIdx.put(key, existingIdx);
      return existingIdx;
    }

    // Otherwise, add an new one.
    existingIdx = scopeInlineAtRecords.size() - 1;
    scopeInlineAtRecords.add(key);
    scopeInlineAtIdx.put(key, existingIdx);
    return existingIdx;
  }

  public void getMDKindNames(ArrayList<String> names) {
    String[] temp = new String[customMDKindNamesMap.size()];
    for (Map.Entry<String, Integer> entry : customMDKindNamesMap.entrySet()) {
      temp[entry.getValue()] = entry.getKey();
    }
    names.clear();
    names.addAll(Arrays.asList(temp));
  }
}
