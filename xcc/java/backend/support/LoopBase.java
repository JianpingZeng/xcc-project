package backend.support;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
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

import tools.Util;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class LoopBase<BlockT, LoopT> {
  /**
   * <p>
   * A sequence of block Id.
   * <br>
   * The first item must be a loop header block. Also all of block
   * id are sorted in descending the {@linkplain #getLoopDepth()} by an
   * instance of {@linkplain Comparator}.
   * </p>
   */
  public LinkedList<BlockT> blocks;
  /**
   * The index of this loop.
   */
  protected int loopIndex;
  /**
   * A pointer to its outer loop loop which isDeclScope this.
   */
  public LoopT outerLoop;
  /**
   * An array of its subLoops loop contained in this.
   */
  public ArrayList<LoopT> subLoops;

  protected LoopBase() {
    blocks = new LinkedList<BlockT>();
    subLoops = new ArrayList<>();
  }

  protected LoopBase(BlockT block) {
    Util.assertion(block != null);
    blocks = new LinkedList<BlockT>();
    blocks.add(block);
    subLoops = new ArrayList<>();
  }

  /**
   * Retrieves the index of a basic block at the specified position where indexed by a index
   * variable.
   *
   * @param index A index that indexed into specified position where TargetData block located.
   * @return A basic block.
   */
  public BlockT getBlock(int index) {
    Util.assertion(index >= 0 && index < blocks.size());
    return blocks.get(index);
  }

  /**
   * Obtains the header block of this loop.
   *
   * @return
   */
  public BlockT getHeaderBlock() {
    Util.assertion(blocks != null && !blocks.isEmpty(), "There is no block in loop");
    return blocks.get(0);
  }

  public int getNumOfBlocks() {
    return blocks.size();
  }

  /**
   * Obtains the depth of this loop in the loop forest it attached, begins from
   * 1.
   *
   * @return
   */
  public abstract int getLoopDepth();

  public ArrayList<LoopT> getSubLoops() {
    return subLoops;
  }

  /**
   * Obtains the depth of this loop in the loop forest it attached, begins from
   * 1.
   *
   * @return
   */
  public List<BlockT> getBlocks() {
    return blocks;
  }

  /**
   * Check to see if a basic block is the loop exiting block or not that
   * if the any successor block of the given parent is outside this loop, so that
   * this parent would be a loop exiting block..
   *
   * @param bb
   * @return True if the given block is the exiting block of this loop, otherwise
   * returned false.
   */
  public abstract boolean isLoopExitingBlock(BlockT bb);

  /**
   * Computes the backward edge leading to the header block in the loop.
   *
   * @return
   */
  public abstract int getNumBackEdges();

  /**
   * <p>
   * If there is a preheader for this loop, return it.  A loop has a preheader
   * if there is only one edge to the header of the loop from outside of the
   * loop.  If this is the case, the block branching to the header of the loop
   * is the preheader node.
   * </p>
   * <p>This method returns null if there is no preheader for the loop.</p>
   *
   * @return
   */
  public abstract BlockT getLoopPreheader();

  /**
   * If given loop's header has exactly one predecessor outside of loop,
   * return it, otherwise, return null.
   *
   * @return
   */
  protected abstract BlockT getLoopPredecessor();

  /**
   * If there is a single loop latch block, return it. Otherwise, return null.
   * <b>A latch block is a block where the control flow branch back to the
   * loop header block.
   * </b>
   *
   * @return
   */
  public abstract BlockT getLoopLatch();

  /**
   * If given basic block is contained in this loop, return true,
   * otherwise false returned.
   *
   * @param block
   * @return
   */
  public boolean contains(BlockT block) {
    return blocks.contains(block);
  }

  public void addFirstBlock(BlockT bb) {
    Util.assertion(bb != null, "parent not be null");
    Util.assertion(!contains(bb), "duplicated block added");

    blocks.addFirst(bb);
  }

  /**
   * Returns a list of all basic block contained in the this loop, but its
   * successor is outside of this loop.
   *
   * @return
   */
  public abstract ArrayList<BlockT> getExitingBlocks();

  /**
   * Returns the unique exit blocks list of this loop.
   * <p>
   * The unique exit block means that if there are multiple edge from
   * a block in loop to this exit block, we just count one.
   * </p>
   *
   * @return
   */
  public abstract ArrayList<BlockT> getUniqueExitBlocks();

  /**
   * If there is just one unique exit block of this loop, return it. Otherwise
   * return {@code null}.
   *
   * @return
   */
  public BlockT getUniqueExitBlock() {
    ArrayList<BlockT> list = getUniqueExitBlocks();
    return list.size() == 1 ? list.get(0) : null;
  }

  /**
   * If {@linkplain #getExitingBlocks()} exactly return one block, then this
   * method will return it, otherwise return null;
   *
   * @return
   */
  public BlockT getExitingBlock() {
    ArrayList<BlockT> res = getExitingBlocks();
    if (res.size() == 1)
      return res.get(0);
    return null;
  }

  public void setParentLoop(LoopT newParent) {
    outerLoop = newParent;
  }

  public LoopT getParentLoop() {
    return outerLoop;
  }

  public LoopT removeChildLoop(int index) {
    Util.assertion(index >= 0 && index < subLoops.size());
    return subLoops.remove(index);
  }

  public void addBlockEntry(BlockT bb) {
    Util.assertion(bb != null);
    blocks.add(bb);
  }

  public void removeBlockFromLoop(BlockT bb) {
    Util.assertion(bb != null, "parent not be null");
    Util.assertion(contains(bb), "parent must contained in loop");
    blocks.remove(bb);
  }

  public abstract void replaceChildLoopWith(LoopT newOne, LoopT oldOne);

  public abstract void addChildLoop(LoopT loop);

  public abstract void addBasicBlockIntoLoop(BlockT bb, LoopInfoBase<BlockT, LoopT> li);

  public abstract void print(OutputStream os, int depth);

  public abstract void dump();

  public int getNumOfSubLoop() {
    return getSubLoops().size();
  }
}
