package backend.analysis;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;

import java.util.ArrayList;

/**
 * Defines a class for computing dominator tree info on Function using
 * class {@linkplain DomTreeInfo}.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public final class DomTree implements FunctionPass {
  private IDomTreeInfo dt;

  private AnalysisResolver resolver;

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  public DomTree() {
    dt = new DomTreeInfoCooper();
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
  }

  @Override
  public String getPassName() {
    return "Dominator Tree Construction";
  }

  /**
   * This method must be overridded by concrete subclass for performing
   * desired machine code transformation or analysis.
   *
   * @param f
   * @return
   */
  @Override
  public boolean runOnFunction(Function f) {
    if (f == null || f.empty())
      return false;
    dt.recalculate(f);
    return false;
  }

  /**
   * Returns the root blocks of current CFG. This may includes multiple blocks
   * if we compute the post dominator. For forward dominators, this will always
   * be a single blcok.
   *
   * @return
   */
  public ArrayList<BasicBlock> getRoots() {
    return dt.getRoots();
  }

  public BasicBlock getRoot() {
    return dt.getRootNode().getBlock();
  }

  public DomTreeNodeBase<BasicBlock> getRootNode() {
    return dt.getRootNode();
  }

  public boolean dominates(BasicBlock mbb1, BasicBlock mbb2) {
    return dt.dominates(mbb1, mbb2);
  }

  public boolean dominates(DomTreeNodeBase<BasicBlock> node1,
                           DomTreeNodeBase<BasicBlock> node2) {
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
  public boolean dominates(Instruction a, Instruction b) {
    BasicBlock bb1 = a.getParent();
    BasicBlock bb2 = b.getParent();

    if (bb1 != bb2) return dominates(bb1, bb2);

    // Loop through the basic block until we find a or b.
    int i = 0, e = bb1.size();
    Instruction inst = null;
    for (; i < e && (inst = bb1.getInstAt(i)) != a
        && inst != b; i++)
      ;
    return i != e && inst == a;
  }

  public boolean strictDominates(DomTreeNodeBase<BasicBlock> node1,
                                 DomTreeNodeBase<BasicBlock> node2) {
    return dt.strictDominate(node1, node2);
  }

  public boolean strictDominates(BasicBlock bb1, BasicBlock bb2) {
    return dt.strictDominate(bb1, bb2);
  }

  public BasicBlock findNearestCommonDominator(
      BasicBlock bb1,
      BasicBlock bb2) {
    return dt.findNearestCommonDominator(bb1, bb2);
  }

  public DomTreeNodeBase<BasicBlock> getNode(BasicBlock bb) {
    return dt.getTreeNodeForBlock(bb);
  }

  public void eraseNode(BasicBlock bb) {
    dt.eraseNode(bb);
  }

  public IDomTreeInfo getDomTree() {
    return dt;
  }

  /**
   * newBB is split and now it has one successor.
   * Update the dominator tree to reflect this effect.
   *
   * @param newBB
   */
  public void splitBlock(BasicBlock newBB) {
    dt.splitBlock(newBB);
  }

  public boolean isReachableFromEntry(BasicBlock bb) {
    return dt.isReachableFromEntry(bb);
  }

  /**
   * Add a new node to the dominator tree. This create a new dominator node
   * as the child of domBB node, linking it into the children list of immediate
   * dominator.
   *
   * @param bb
   * @param domBB
   * @return
   */
  public DomTreeNodeBase<BasicBlock> addNewBlock(BasicBlock bb, BasicBlock domBB) {
    return dt.addNewBlock(bb, domBB);
  }

  public void changeIDom(BasicBlock block, BasicBlock newIDom) {
    dt.changeIDom(block, newIDom);
  }

  public void changeIDom(DomTreeNodeBase<BasicBlock> node,
                         DomTreeNodeBase<BasicBlock> newIDom) {
    dt.changeIDom(node, newIDom);
  }
}
