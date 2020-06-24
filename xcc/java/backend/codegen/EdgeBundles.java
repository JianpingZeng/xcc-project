/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

package backend.codegen;

import backend.pass.AnalysisUsage;
import backend.support.IntEqClasses;
import backend.support.MachineFunctionPass;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import tools.Util;

import java.util.Iterator;

/**
 * The EdgeBundles analysis forms equivalence classes of CFG edges such that all
 * edges leaving a machine basic block are in the same equivalence set.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class EdgeBundles extends MachineFunctionPass {
  private MachineFunction mf;
  /**
   * <pre>
   * Each edge bundle is an equivalence class. The keys are:
   *  mbb.getNumber()*2  ---> Ingoing bundle
   *  mbb.getNumber()*2+1---> Outgoing bundle
   * </pre>
   */
  private IntEqClasses ec;

  /**
   * Maps the equivalence id to a list of basic block numbers.
   * It size is same as number equivalence set.
   */
  private TIntArrayList[] blocks;


  private TIntIntHashMap groupID;

  private int nextID;

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
    super.getAnalysisUsage(au);
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    this.mf = mf;
    ec = new IntEqClasses(mf.getNumBlocks() * 2);
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      int outEdge = mbb.getNumber() * 2 + 1;
      // Join the outgoing
      for (Iterator<MachineBasicBlock> itr = mbb.succIterator(); itr.hasNext(); ) {
        ec.join(outEdge, itr.next().getNumber() * 2);
      }
    }
    blocks = new TIntArrayList[getNumBundles()];
    for (int i = 0, e = getNumBundles(); i < e; i++)
      blocks[i] = new TIntArrayList();

    groupID = new TIntIntHashMap();
    for (int i = 0; i < ec.getNumIds(); i++) {
      int leading = ec.findLeader(i);
      if (!groupID.containsKey(leading))
        groupID.put(leading, nextID++);
    }

    for (int i = 0, e = mf.getNumBlocks(); i < e; i++) {
      int b0 = getBundles(i, false);
      int b1 = getBundles(i, true);
      blocks[b0].add(i);
      if (b1 != b0)
        blocks[b1].add(i);
    }
    nextID = 0;
    return false;
  }

  public int getBundles(int n, boolean out) {
    int leading = ec.findLeader(n * 2 + (out ? 1 : 0));
    Util.assertion(groupID.containsKey(leading));
    return groupID.get(leading);
  }

  public int getNumBundles() {
    return ec.getNumClasses();
  }

  public TIntArrayList getBlocks(int bundle) {
    Util.assertion(bundle >= 0 && bundle < blocks.length, "Illegal bundle or not initialize blocks yet?");

    return blocks[bundle];
  }

  @Override
  public String getPassName() {
    return "Edge Bundles Pass on Machine CFG";
  }
}
