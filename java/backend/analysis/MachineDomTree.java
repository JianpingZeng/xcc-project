package backend.analysis;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng
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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.pass.AnalysisUsage;
import backend.support.MachineFunctionPass;

import java.util.ArrayList;

/**
 * Represents a machine function pass for computing the dominator tree on
 * MachineFunction using class {@linkplain MachineDomTreeInfoCooper}.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class MachineDomTree extends MachineFunctionPass {
  private IMachineDomTreeInfo dt;

  public MachineDomTree() {
    dt = new MachineDomTreeInfoCooper();
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
    super.getAnalysisUsage(au);
  }

  @Override
  public String getPassName() {
    return "MachineDominator Tree Construction";
  }

  /**
   * This method must be overridded by concrete subclass for performing
   * desired machine code transformation or analysis.
   *
   * @param mf
   * @return
   */
  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    dt.recalculate(mf);
    return false;
  }

  /**
   * Returns the root blocks of current CFG. This may includes multiple blocks
   * if we compute the post dominator. For forward dominators, this will always
   * be a single blcok.
   *
   * @return
   */
  public ArrayList<MachineBasicBlock> getRoots() {
    return dt.getRoots();
  }

  public MachineBasicBlock getRoot() {
    return dt.getRootNode().getBlock();
  }

  public DomTreeNodeBase<MachineBasicBlock> getRootNode() {
    return dt.getRootNode();
  }

  public boolean dominates(MachineBasicBlock mbb1, MachineBasicBlock mbb2) {
    return dt.dominates(mbb1, mbb2);
  }

  public boolean dominates(DomTreeNodeBase<MachineBasicBlock> node1,
                           DomTreeNodeBase<MachineBasicBlock> node2) {
    return dt.dominates(node1, node2);
  }

  /**
   * Computes if the second machine instruction is dominated by
   * the first one. This perform special checks if a and b  are in the
   * same block.
   *
   * @param a
   * @param b
   * @return
   */
  public boolean dominates(MachineInstr a, MachineInstr b) {
    MachineBasicBlock mbb1 = a.getParent();
    MachineBasicBlock mbb2 = b.getParent();

    if (mbb1 != mbb2) return dominates(mbb1, mbb2);

    // Loop through the basic block until we find a or b.
    int i = 0, e = mbb1.size();
    MachineInstr mi = null;
    for (; i < e && (mi = mbb1.getInstAt(i)) != a
        && mi != b; i++)
      ;
    return i != e && mi == a;
  }

  public boolean strictDominates(DomTreeNodeBase<MachineBasicBlock> node1,
                                 DomTreeNodeBase<MachineBasicBlock> node2) {
    return dt.strictDominate(node1, node2);
  }

  public boolean strictDominates(MachineBasicBlock bb1, MachineBasicBlock bb2) {
    return dt.strictDominate(bb1, bb2);
  }

  public MachineBasicBlock findNearestCommonDominator(
      MachineBasicBlock bb1,
      MachineBasicBlock bb2) {
    return dt.findNearestCommonDominator(bb1, bb2);
  }

  public DomTreeNodeBase<MachineBasicBlock> getNode(MachineBasicBlock bb) {
    return dt.getTreeNodeForBlock(bb);
  }

  public void eraseNode(MachineBasicBlock bb) {
    dt.eraseNode(bb);
  }
}
