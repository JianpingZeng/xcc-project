package backend.analysis;
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

import backend.value.BasicBlock;
import backend.value.Function;

import java.util.ArrayList;

/**
 * This file defines an interface for providing various of useful methods to
 * compute Dominator Tree and Immediately Dominator.
 * <p>
 * The client of this class should implements this by concrete subclass.
 * </p>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface IDomTreeInfo {

  /**
   * Recalculate - compute a dominator tree for the given function.
   */
  void recalculate(Function f);

  /**
   * Returns the root blocks of current CFG. This may include multiple blocks
   * if we are computing post dominators. For forward dominators, this wil always
   * be a single block (the entry block of CFG).
   *
   * @return
   */
  ArrayList<BasicBlock> getRoots();

  /**
   * This returns the entry dominator tree node of the CFG attached to the
   * function. IfStmt this tree represents the post-dominator relation for a
   * function, however, this root may be a node with the block == null. This
   * is teh case when there are multiple exit nodes from a particular function.
   *
   * @return
   */
  DomTreeNodeBase<BasicBlock> getRootNode();

  /**
   * Gets the corresponding dominator tree node for specifed basic block.
   *
   * @param bb
   * @return
   */
  DomTreeNodeBase<BasicBlock> getTreeNodeForBlock(BasicBlock bb);

  /**
   * Returns true if analysis based on postdoms.
   *
   * @return
   */
  boolean isPostDominators();

  /**
   * Determine whether A dominates B.
   *
   * @param A
   * @param B
   * @return ReturnInst true iff A dominates B.
   */
  boolean dominates(DomTreeNodeBase<BasicBlock> A,
                    DomTreeNodeBase<BasicBlock> B);

  boolean dominates(BasicBlock A, BasicBlock B);

  /**
   * ReturnInst true if B dominated by A, but A != B.
   *
   * @param A
   * @param B
   * @return
   */
  boolean strictDominate(DomTreeNodeBase<BasicBlock> A,
                         DomTreeNodeBase<BasicBlock> B);

  boolean strictDominate(BasicBlock a, BasicBlock b);

  /**
   * Determines whether BB is reachable from the entry block of a function.
   *
   * @param BB
   * @return
   */
  boolean isReachableFromEntry(BasicBlock BB);

  boolean isReachableFromEntry(DomTreeNodeBase<BasicBlock> node);

  /**
   * Gets the dominated block of given block.
   *
   * @param block
   * @return
   */
  BasicBlock getIDom(BasicBlock block);

  /**
   * Finds the nearest common dominator block ,if there is no such block return
   * null.
   *
   * @param bb1
   * @param bb2
   * @return
   */
  BasicBlock findNearestCommonDominator(BasicBlock bb1, BasicBlock bb2);

  /**
   * Removes a node from  the dominator tree. Block must not
   * domiante any other blocks. Removes node from its immediate dominator's
   * children list. Deletes dominator node associated with basic block BB.
   *
   * @param bb
   */
  void eraseNode(BasicBlock bb);

  /**
   * newBB is split and now it has one successor.
   * Update the dominator tree to reflect this effect.
   *
   * @param newBB
   */
  void splitBlock(BasicBlock newBB);

  /**
   * Add a new node to the dominator tree information.  This
   * creates a new node as a child of DomBB dominator node,linking it into
   * the children list of the immediate dominator.
   *
   * @param bb
   * @param idom
   * @return
   */
  DomTreeNodeBase<BasicBlock> addNewBlock(BasicBlock bb, BasicBlock idom);

  /**
   * Updates the dominator tree information when immediate dominator node changes.
   *
   * @param oldIDom
   * @param newIDom
   */
  void changeIDom(DomTreeNodeBase<BasicBlock> oldIDom,
                  DomTreeNodeBase<BasicBlock> newIDom);

  /**
   * Updates the dominator tree information when immediate dominator node changes.
   *
   * @param oldIDomBB
   * @param newIDomBB
   */
  void changeIDom(BasicBlock oldIDomBB, BasicBlock newIDomBB);
}
