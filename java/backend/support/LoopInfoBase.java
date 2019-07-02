package backend.support;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class contains all information about the top level loop structure
 * in the specified function.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface LoopInfoBase<BlockT, LoopT> {
  default LoopT getLoopFor(BlockT block) {
    HashMap<BlockT, LoopT> map = getBBMap();
    if (map.containsKey(block))
      return map.get(block);
    return null;
  }

  HashMap<BlockT, LoopT> getBBMap();

  ArrayList<LoopT> getTopLevelLoop();

  default boolean isNoTopLevelLoop() {
    return getTopLevelLoop().isEmpty();
  }

  int getLoopDepth(BlockT bb);

  boolean isLoopHeader(BlockT bb);

  /**
   * Remove the specified top level loop at the given position.
   *
   * @param index
   * @return
   */
  default LoopT removeTopLevelLoop(int index) {
    ArrayList<LoopT> list = getTopLevelLoop();
    Util.assertion(index >= 0 && index < list.size(), "Indices out of valid range");
    ensureIsTopLevel(list.get(index), "The loop not at top level");
    return list.remove(index);
  }

  /**
   * Change the lop-level loop that contains the block to the another specified
   * loop. This usually used for transformation that restructure the loop
   * hierarchy tree.
   *
   * @param block
   * @param newOne
   */
  default void changeLoopFor(BlockT block, LoopT newOne) {
    LoopT old = getBBMap().get(block);
    Util.assertion(old != null, "Block not in loop as yet!");
    ensureIsTopLevel(newOne, "The loop not at top level");
    ensureIsTopLevel(old, "The loop not at top level");
    getBBMap().replace(block, old, newOne);
  }

  default void replaceTopLevelLoop(LoopT oldLoop, LoopT newLoop) {
    ArrayList<LoopT> list = getTopLevelLoop();
    Util.assertion(list.contains(oldLoop), "Old loop not at top level!");
    int idx = list.indexOf(oldLoop);
    list.set(idx, newLoop);
    ensureIsTopLevel(newLoop, "The loop not at top level");
    ensureIsTopLevel(oldLoop, "The loop not at top level");
  }

  /**
   * Ensure the specified loop's parent loop is null that means it is top level
   * loop.
   *
   * @param loop
   */
  void ensureIsTopLevel(LoopT loop, String msg);

  default void addTopLevelLoop(LoopT loop) {
    ensureIsTopLevel(loop, "Loop already in subloop!");
    ArrayList<LoopT> list = getTopLevelLoop();
    Util.assertion(!list.contains(loop), "The loop already in list!");
    list.add(loop);
  }

  /**
   * Removes the block from all data structures, including all of the loop
   * objects containing it and diagMapping from Block to loop..
   *
   * @param block
   */
  void removeBlock(BlockT block);
}
