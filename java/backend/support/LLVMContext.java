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
  public final Type VoidTy;
  public final Type LabelTy;
  public final Type MetadataTy;
  public final IntegerType Int1Ty;
  public final IntegerType Int8Ty;
  public final IntegerType Int16Ty;
  public final IntegerType Int32Ty;
  public final IntegerType Int64Ty;
  public final Type FloatTy;
  public final Type DoubleTy;
  public final Type FP128Ty;
  public final Type X86_FP80Ty;
  public final Type PPC_FP128Ty;

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

    VoidTy = new Type(this, LLVMTypeID.VoidTyID);
    LabelTy = new Type(this, LLVMTypeID.LabelTyID);
    MetadataTy = new Type(this, LLVMTypeID.MetadataTyID);
    Int1Ty = new IntegerType(this, 1);
    Int8Ty = new IntegerType(this, 8);
    Int16Ty = new IntegerType(this, 16);
    Int32Ty = new IntegerType(this, 32);
    Int64Ty = new IntegerType(this, 64);
    FloatTy = new Type(this, LLVMTypeID.FloatTyID);
    DoubleTy = new Type(this, LLVMTypeID.DoubleTyID);
    FP128Ty = new Type(this, LLVMTypeID.FP128TyID);
    X86_FP80Ty = new Type(this, LLVMTypeID.X86_FP80TyID);
    PPC_FP128Ty = new Type(this, LLVMTypeID.PPC_FP128TyID);
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
