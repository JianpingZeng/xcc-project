package backend.support;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Enumerates slot number for unnamed values.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class SlotTracker {
  /**
   * The module for which we are holding slot numbers.
   */
  private Module theModule;
  /**
   * The function for which we are holding slot numbers.
   */
  private Function theFunction;

  /**
   * The MDNode for which we are holding slot numbers.
   */
  private MDNode mNode;

  /**
   * The NamedMDNode for which we are holding slot numbers.
   */
  private NamedMDNode nNode;

  private boolean functionProcessed;
  /**
   * The mapping from Value to its slot number in module level.
   */
  private HashMap<Value, Integer> mMap;
  /**
   * The next slot number of value in module level.
   */
  private int mNext;
  /**
   * The mapping from Value to its slot number in function level.
   */
  private HashMap<Value, Integer> fMap;
  /**
   * The next slot number of value in function level.
   */
  private int fNext;

  private HashMap<Value, Integer> mdnMap;
  private int mdnNext;

  public SlotTracker(Module m) {
    theModule = m;
    mdnMap = new HashMap<>();
    mMap = new HashMap<>();
    fMap = new HashMap<>();
  }

  public SlotTracker(Function f) {
    this.theFunction = f;
    mdnMap = new HashMap<>();
    mMap = new HashMap<>();
    fMap = new HashMap<>();
  }

  public SlotTracker(MDNode mnode) {
    mNode = mnode;
    mdnMap = new HashMap<>();
    mMap = new HashMap<>();
    fMap = new HashMap<>();
  }

  public SlotTracker(NamedMDNode nnode) {
    nNode = nnode;
    mdnMap = new HashMap<>();
    mMap = new HashMap<>();
    fMap = new HashMap<>();
  }

  public int getLocalSlot(Value v) {
    Util.assertion(!(v instanceof Constant),
        "Can't get a constant or global slot with this!");
    initialize();
    return fMap.getOrDefault(v, -1);
  }

  public int getGlobalSlot(GlobalValue gv) {
    initialize();
    return mMap.getOrDefault(gv, -1);
  }

  public void incorporateFunction(Function f) {
    theFunction = f;
    functionProcessed = false;
  }

  /**
   * After calling incorporateFunction, use this method to remove the
   * most recently incorporated function from the SlotTracker. This
   * will reset the state of the machine back to just the module contents.
   */
  public void pruneFunction() {
    fMap.clear();
    theFunction = null;
    functionProcessed = false;
  }

  private void createMetadataSlot(MDNode node) {
    Util.assertion(node != null, "Can't insert a null value into slotTracker!");
    // Don't insert if N is a function-local metadata, these are always printed
    // inline.
    if (!node.isFunctionLocal()) {
      if (mdnMap.containsKey(node))
        return;

      int slot = mdnNext++;
      mdnMap.put(node, slot);
    }

    for (int i = 0, e = node.getNumOperands(); i < e; i++) {
      Value val = node.getOperand(i);
      if (val instanceof MDNode)
        createMetadataSlot((MDNode) val);
    }
  }

  private void processMDNode() {
    mdnNext = 0;
    createMetadataSlot(mNode);
    mNode = null;
  }

  private void processNamedMDNode() {
    mdnNext = 0;
    for (int i = 0, e = nNode.getNumOfOperands(); i < e; i++) {
      MDNode val = nNode.getOperand(i);
      if (val != null)
        createMetadataSlot(val);
    }
    nNode = null;
  }

  public void initialize() {
    if (theModule != null) {
      processModule();
      theModule = null;
    }

    if (theFunction != null && !functionProcessed)
      processFunction();
  }

  private void createModuleSlot(GlobalValue gv) {
    Util.assertion(gv != null);
    Util.assertion(!gv.getType().isVoidType());
    Util.assertion(!gv.hasName());

    int destSlot = mNext++;
    mMap.put(gv, destSlot);
  }

  private void createFunctionSlot(Value v) {
    Util.assertion(v != null);
    Util.assertion(!v.getType().isVoidType());
    Util.assertion(!v.hasName());

    int destSlot = fNext++;
    fMap.put(v, destSlot);
  }

  private void processModule() {
    // Add all of the unnamed global variables to the value table.
    for (GlobalVariable gv : theModule.getGlobalVariableList()) {
      if (!gv.hasName()) {
        createModuleSlot(gv);
      }
    }

    // Add metadata used by named metadata.
    for (NamedMDNode nmd : theModule.getNamedMDList()) {
      for (int i = 0,e = nmd.getNumOfOperands(); i < e; i++)
        createMetadataSlot(nmd.getOperand(i));
    }

    for (Function f : theModule.getFunctionList()) {
      if (!f.hasName())
        createModuleSlot(f);
    }
  }

  private void processFunction() {
    fNext = 0;
    // Add all the function arguments with no names.
    for (Argument arg : theFunction.getArgumentList()) {
      if (!arg.hasName())
        createFunctionSlot(arg);
    }

    ArrayList<Pair<Integer, MDNode>> mdForInst = new ArrayList<>();

    // Add all basic blocks and instructions don't have name.
    for (BasicBlock bb : theFunction.getBasicBlockList()) {
      if (!bb.hasName()) {
        createFunctionSlot(bb);
      }
      for (Instruction inst : bb) {
        if (!inst.getType().isVoidType() && !inst.hasName())
          createFunctionSlot(inst);

        // Intrinsics can directly use metadata.  We allow direct calls to any
        // llvm.foo function here, because the target may not be linked into the
        // optimizer.
        if (inst instanceof Instruction.CallInst) {
          Instruction.CallInst ci = (Instruction.CallInst) inst;
          Function f = ci.getCalledFunction();
          if (f != null && f.getName().startsWith("llvm.")) {
            for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
              if (inst.operand(i) instanceof MDNode)
                createMetadataSlot((MDNode)inst.operand(i));
            }
          }
        }

        // process metedata attached to this instruction.
        inst.getAllMetadata(mdForInst);
        mdForInst.forEach(entry -> {
          createMetadataSlot(entry.second);
        });
        mdForInst.clear();
      }
    }
    functionProcessed = true;
  }

  public int getMetadataSlot(MDNode node) {
    initialize();
    return mdnMap.getOrDefault(node, -1);
  }

  public HashMap<Value, Integer> getMdnMap() {
    return mdnMap;
  }
}
