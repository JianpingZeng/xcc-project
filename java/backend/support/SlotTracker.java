package backend.support;
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

import backend.value.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

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
  private TObjectIntHashMap<Value> mMap;
  /**
   * The next slot number of value in module level.
   */
  private int mNext;
  /**
   * The mapping from Value to its slot number in function level.
   */
  private TObjectIntHashMap<Value> fMap;
  /**
   * The next slot number of value in function level.
   */
  private int fNext;

  private TObjectIntHashMap<Value> mdnMap;
  private int mdnNext;

  public SlotTracker(Module m) {
    theModule = m;
    mdnMap = new TObjectIntHashMap<>();
    mMap = new TObjectIntHashMap<>();
    fMap = new TObjectIntHashMap<>();
  }

  public SlotTracker(Function f) {
    this.theFunction = f;
    mdnMap = new TObjectIntHashMap<>();
    mMap = new TObjectIntHashMap<>();
    fMap = new TObjectIntHashMap<>();
  }

  public SlotTracker(MDNode mnode) {
    mNode = mnode;
    mdnMap = new TObjectIntHashMap<>();
    mMap = new TObjectIntHashMap<>();
    fMap = new TObjectIntHashMap<>();
  }

  public SlotTracker(NamedMDNode nnode) {
    nNode = nnode;
    mdnMap = new TObjectIntHashMap<>();
    mMap = new TObjectIntHashMap<>();
    fMap = new TObjectIntHashMap<>();
  }

  public int getLocalSlot(Value v) {
    Util.assertion(!(v instanceof Constant),
        "Can't get a constant or global slot with this!");
    initialize();
    return fMap.containsKey(v) ? fMap.get(v) : -1;
  }

  public int getGlobalSlot(GlobalValue gv) {
    initialize();
    return mMap.containsKey(gv) ? mMap.get(gv) : -1;
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
    if (mdnMap.containsKey(node))
      return;

    int slot = mdnNext++;
    mdnMap.put(node, slot);

    for (int i = 0, e = node.getNumOfNode(); i < e; i++) {
      Value val = node.getNode(i);
      if (val != null && val instanceof MDNode)
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
    for (int i = 0, e = nNode.getNumOfNode(); i < e; i++) {
      Value val = nNode.getNode(i);
      if (val instanceof MDNode)
        createMetadataSlot((MDNode) val);
    }
    nNode = null;
  }

  public void initialize() {
    if (theModule != null) {
      processModule();
      theModule = null;
    }

    if (theFunction != null && !functionProcessed) {
      processFunction();
    }

    if (mNode != null)
      processMDNode();

    if (nNode != null)
      processNamedMDNode();
  }

  private void createModuleSlot(GlobalValue gv) {
    Util.assertion(gv != null);
    Util.assertion(!gv.getType().equals(LLVMContext.VoidTy));
    Util.assertion(!gv.hasName());

    int destSlot = mNext++;
    mMap.put(gv, destSlot);
  }

  private void createFunctionSlot(Value v) {
    Util.assertion(v != null);
    Util.assertion(!v.getType().equals(LLVMContext.VoidTy));
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

    // Add all basic blocks and instructions don't have name.
    for (BasicBlock bb : theFunction.getBasicBlockList()) {
      if (!bb.hasName()) {
        createFunctionSlot(bb);
      }
      for (Instruction inst : bb) {
        if (!inst.getType().equals(LLVMContext.VoidTy) && !inst.hasName())
          createFunctionSlot(inst);
      }
    }
    functionProcessed = true;
  }

  public int getMetadataSlot(MDNode node) {
    initialize();
    return mdnMap.containsKey(node) ? mdnMap.get(node) : -1;
  }

  public TObjectIntHashMap<Value> getMdnMap() {
    return mdnMap;
  }
}
