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

package backend.codegen.dagisel;

import backend.codegen.MachineInstr;
import backend.mc.MCRegisterClass;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

import static backend.codegen.dagisel.SDep.Kind.Data;

public class SUnit {
  private SDNode node;
  private MachineInstr instr;

  public SUnit originNode;

  public ArrayList<SDep> preds;
  public ArrayList<SDep> succs;

  public int nodeNum;
  public int nodeQueueId;
  public int latency;
  /**
   * number of Data dependency preds.
   */
  public int numPreds;
  /**
   * number of Data dependency succs.
   */
  public int numSuccs;
  public int numPredsLeft;
  public int numSuccsLeft;
  public boolean isTwoAddress;
  public boolean isCommutable;
  public boolean hasPhysRegDefs;
  public boolean hasPhysRegClobbers;
  public boolean isPending;
  public boolean isAvailable;
  public boolean isScheduled;
  public boolean isScheduleHigh;
  public boolean isCloned;

  private boolean isDepthCurrent;
  private boolean isHeightCurrent;
  private int depth;
  private int height;

  public MCRegisterClass copyDstRC, copySrcRC;

  /**
   * Constructor for DAGNode based Scheduler(pre-ra).
   */
  public SUnit(SDNode node, int nodeNum) {
    this.node = node;
    this.nodeNum = nodeNum;
    preds = new ArrayList<>();
    succs = new ArrayList<>();
  }

  /**
   * Constructor for MachineInstr based Scheduler(post-ra).
   */
  public SUnit(MachineInstr instr, int nodeNum) {
    this.instr = instr;
    this.nodeNum = nodeNum;
    preds = new ArrayList<>();
    succs = new ArrayList<>();
  }

  /**
   * Constructor for placeholder SUnit.
   */
  public SUnit() {
    preds = new ArrayList<>();
    succs = new ArrayList<>();
  }

  public void setNode(SDNode n) {
    Util.assertion(instr == null);
    node = n;
  }

  public SDNode getNode() {
    Util.assertion(instr == null);
    return node;
  }

  public void setInstr(MachineInstr mi) {
    Util.assertion(node == null);
    instr = mi;
  }

  public MachineInstr getInstr() {
    Util.assertion(node == null);
    return instr;
  }

  /**
   * This adds specified edge as a pred of the current
   * node if not added already. It also adds the current
   * node as successor of the specified node.
   */
  public void addPred(SDep d) {
    if (preds.contains(d))
      return;

    SDep p = d.clone();
    p.setSUnit(this);
    SUnit n = d.getSUnit();

    if (d.getDepKind() == Data) {
      ++numPreds;
      ++n.numSuccs;
    }
    if (!n.isScheduled)
      ++numPredsLeft;
    if (!isScheduled)
      ++n.numSuccsLeft;
    preds.add(d);
    n.succs.add(p);
    if (p.getLatency() != 0) {
      setDepthDirty();
      n.setHeightDirty();
    }
  }

  /**
   * Remove the specified SDep from the preds of current node.
   * Terminates directly if the specified SDep doesn't exists in
   * preds set.
   */
  public void removePred(SDep d) {
    for (int i = 0, e = preds.size(); i < e; i++) {
      if (Objects.equals(preds.get(i), e)) {
        boolean foundSucc = false;
        // remove the corresponding succ SDep from pred.
        SDep p = d.clone();
        p.setSUnit(this);
        SUnit n = d.getSUnit();
        for (int j = 0, sz = n.succs.size(); j < sz; j++) {
          if (Objects.equals(p, n.succs.get(j))) {
            foundSucc = true;
            n.succs.remove(j);
            break;
          }
        }
        Util.assertion(foundSucc, "Mismatch pred/succ pair!");
        preds.remove(i);

        if (d.getDepKind() == Data) {
          --numPreds;
          --n.numSuccs;
        }
        if (!n.isScheduled)
          --numPredsLeft;
        if (isScheduled)
          --n.numSuccsLeft;
        if (p.getLatency() != 0) {
          setDepthDirty();
          n.setHeightDirty();
        }
        return;
      }
    }
  }

  public int getDepth() {
    if (!isDepthCurrent) {
      computeDepth();
    }
    return depth;
  }

  public int getHeight() {
    if (!isHeightCurrent)
      computeHeight();
    return height;
  }

  public void setDepthToAtLeast(int newDepth) {
    if (newDepth <= getDepth())
      return;
    setDepthDirty();
    depth = newDepth;
    isDepthCurrent = true;
  }

  public void setHeightToAtLeast(int newHeight) {
    if (newHeight <= getHeight())
      return;
    setHeightDirty();
    height = newHeight;
    isHeightCurrent = true;
  }

  public void setDepthDirty() {
    if (!isDepthCurrent) return;
    Stack<SUnit> worklist = new Stack<>();
    worklist.push(this);
    do {
      SUnit su = worklist.pop();
      su.isDepthCurrent = false;
      succs.forEach(succ ->
      {
        SUnit succSU = succ.getSUnit();
        if (succSU.isDepthCurrent)
          worklist.push(succSU);
      });
    }
    while (!worklist.isEmpty());
  }

  public void setHeightDirty() {
    if (!isHeightCurrent) return;
    Stack<SUnit> worklist = new Stack<>();
    worklist.push(this);
    do {
      SUnit su = worklist.pop();
      su.isHeightCurrent = false;
      preds.forEach(pred ->
      {
        SUnit predSU = pred.getSUnit();
        if (predSU.isHeightCurrent)
          worklist.push(predSU);
      });
    }
    while (!worklist.isEmpty());
  }

  public boolean isPred(SUnit u) {
    for (SDep d : preds) {
      if (Objects.equals(d.getSUnit(), u))
        return true;
    }
    return false;
  }

  public boolean isSucc(SUnit u) {
    for (SDep d : succs) {
      if (Objects.equals(d.getSUnit(), u))
        return true;
    }
    return false;
  }

  public void dump(ScheduleDAG dag) {
    System.err.printf("SU(%d):", nodeNum);
    dag.dumpNode(this);
  }

  public void dumpAll(ScheduleDAG dag) {
    dump(dag);

    System.err.printf("  # preds left      : %d%n", numPredsLeft);
    System.err.printf("  # succs left      : %d%n", numSuccsLeft);
    System.err.printf("  Latency           : %d%n", latency);
    System.err.printf("  Depth             : %d%n", depth);
    System.err.printf("  Height            : %d%n", height);

    if (!preds.isEmpty()) {
      System.err.println("  Predecessors:");
      for (SDep d : preds) {
        System.err.print("    ");
        switch (d.getDepKind()) {
          case Data:
            System.err.print("val   ");
            break;
          case Anti:
            System.err.print("anti  ");
            break;
          case Output:
            System.err.print("output");
            break;
          case Order:
            System.err.print("ch    ");
            break;
        }
        System.err.print("#");
        System.err.printf("0x%x - SU(%d)", d.getSUnit().hashCode(),
            d.getSUnit().nodeNum);
        if (d.isArtificial())
          System.err.print(" *");
        System.err.printf(": Latency=%d", d.getLatency());
        System.err.println();
      }
    }
    if (!succs.isEmpty()) {
      System.err.println("  Successors:");
      for (SDep d : succs) {
        System.err.print("    ");
        switch (d.getDepKind()) {
          case Data:
            System.err.print("val   ");
            break;
          case Anti:
            System.err.print("anti  ");
            break;
          case Output:
            System.err.print("output");
            break;
          case Order:
            System.err.print("ch    ");
            break;
        }
        System.err.print("#");
        System.err.printf("0x%x - SU(%d)", d.getSUnit().hashCode(),
            d.getSUnit().nodeNum);
        if (d.isArtificial())
          System.err.print(" *");
        System.err.printf(": Latency=%d", d.getLatency());
        System.err.println();
      }
    }
    System.err.println();
  }

  public void print(PrintStream os, ScheduleDAG dag) {
    // TODO
  }

  private void computeHeight() {
    Stack<SUnit> worklist = new Stack<>();
    worklist.push(this);
    do {
      SUnit su = worklist.peek();
      boolean done = true;
      int maxSuccHeight = 0;
      for (SDep d : succs) {
        SUnit succSU = d.getSUnit();
        if (succSU.isHeightCurrent) {
          maxSuccHeight = Math.max(maxSuccHeight,
              succSU.height + d.getLatency());
        } else {
          done = false;
          worklist.add(succSU);
        }
      }
      if (done) {
        worklist.pop();
        if (maxSuccHeight != su.height) {
          su.setHeightDirty();
          su.height = maxSuccHeight;
        }
        su.isHeightCurrent = true;
      }
    }
    while (!worklist.isEmpty());
  }

  private void computeDepth() {
    Stack<SUnit> worklist = new Stack<>();
    worklist.push(this);
    do {
      SUnit su = worklist.peek();
      boolean done = true;
      int maxPredDepth = 0;
      for (SDep d : preds) {
        SUnit predSU = d.getSUnit();
        if (predSU.isDepthCurrent) {
          maxPredDepth = Math.max(maxPredDepth,
              predSU.getDepth() + d.getLatency());
        } else {
          done = false;
          worklist.add(predSU);
        }
      }
      if (done) {
        worklist.pop();
        if (maxPredDepth != su.getDepth()) {
          su.setDepthDirty();
          su.depth = maxPredDepth;
        }
        su.isDepthCurrent = true;
      }
    }
    while (!worklist.isEmpty());
  }
}