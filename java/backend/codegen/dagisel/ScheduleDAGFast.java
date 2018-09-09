/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.MachineFunction;
import backend.target.TargetInstrDesc;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine.CodeGenOpt;
import backend.target.TargetRegisterClass;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import tools.Pair;
import tools.Util;

import java.util.*;

import static backend.target.TargetOperandInfo.OperandConstraint.TIED_TO;

public class ScheduleDAGFast extends ScheduleDAGSDNodes {
  private LinkedList<SUnit> availableQueue;

  private int numLiveRegs;
  private SUnit[] liveRegDefs;
  private int[] liveRegCycles;

  public ScheduleDAGFast(MachineFunction mf) {
    super(mf);
    availableQueue = new LinkedList<>();
  }

  public void schedule() {
    if (Util.DEBUG)
      System.err.println("********** List Scheduling ************");
    availableQueue.clear();
    numLiveRegs = 0;
    liveRegDefs = new SUnit[tri.getNumRegs()];
    liveRegCycles = new int[tri.getNumRegs()];

    // Step#1: Build scheduling DAG graph.
    buildSchedGraph();
    if (Util.DEBUG) {
      sunits.forEach(su -> su.dumpAll(this));
    }
    // Step#2: Starts to schedule the DAG from bottom to up.
    listScheduleBottomUp();
  }

  public void addPred(SUnit su, SDep d) {
    su.addPred(d);
  }

  public void removePred(SUnit su, SDep d) {
    su.removePred(d);
  }

  private void releasePred(SDep predEdge) {
    SUnit predSU = predEdge.getSUnit();
    --predSU.numSuccsLeft;

    if (predSU.numSuccsLeft == 0 && !Objects.equals(predSU, entrySU)) {
      predSU.isAvailable = true;
      availableQueue.push(predSU);
    }
  }

  private void releasePredecessors(SUnit su, int curCycle) {
    for (SDep dep : su.preds) {
      releasePred(dep);
      if (dep.isAssignedRegDep()) {
        if (liveRegDefs[dep.getReg()] == null) {
          ++numLiveRegs;
          liveRegDefs[dep.getReg()] = dep.getSUnit();
          liveRegCycles[dep.getReg()] = curCycle;
        }
      }
    }
  }

  private void scheduleNodeBottomUp(SUnit su, int cycle) {
    if (Util.DEBUG) {
      System.err.printf("*** Scheduling [%d]: %n", cycle);
      su.dump(this);
    }

    Util.assertion(cycle >= su.getHeight());
    su.setHeightToAtLeast(cycle);
    sequence.add(su);

    releasePredecessors(su, cycle);

    for (SDep dep : su.succs) {
      if (dep.isAssignedRegDep()) {
        int reg = dep.getReg();
        if (liveRegCycles[reg] == dep.getSUnit().getHeight()) {
          Util.assertion(numLiveRegs > 0);
          Util.assertion(Objects.equals(liveRegDefs[reg], su));
          --numLiveRegs;
          liveRegDefs[reg] = null;
          liveRegCycles[reg] = 0;
        }
      }
    }
    su.isScheduled = true;
  }

  private SUnit copyAndMoveSuccessors(SUnit su) {
    if (su.getNode().getFlaggedNode() != null)
      return null;

    SDNode n = su.getNode();
    if (n == null)
      return null;

    SUnit newSU = null;
    boolean tryUnFold = false;
    for (int i = 0, e = n.getNumValues(); i < e; i++) {
      EVT vt = n.getValueType(i);
      if (vt.getSimpleVT().simpleVT == MVT.Flag)
        return null;
      else if (vt.getSimpleVT().simpleVT == MVT.Other)
        tryUnFold = true;
    }

    for (int i = 0, e = n.getNumOperands(); i < e; i++) {
      SDValue op = n.getOperand(i);
      EVT vt = op.getNode().getValueType(op.getResNo());
      if (vt.getSimpleVT().simpleVT == MVT.Flag)
        return null;
    }

    if (tryUnFold) {
      ArrayList<SDNode> newNodes = new ArrayList<>();
      if (!tii.unfoldMemoryOperand(dag, n, newNodes))
        return null;

      Util.assertion(newNodes.size() == 2, "Expected a load folding node!");

      n = newNodes.get(1);
      SDNode loadNode = newNodes.get(0);
      int numVals = n.getNumValues();
      int oldNumVals = su.getNode().getNumValues();
      for (int i = 0; i < numVals; i++)
        dag.replaceAllUsesOfValueWith(new SDValue(su.getNode(), i), new SDValue(n, i), null);

      dag.replaceAllUsesOfValueWith(new SDValue(su.getNode(), oldNumVals - 1),
          new SDValue(loadNode, 1), null);

      SUnit su2 = newSUnit(n);
      Util.assertion(n.getNodeID() == -1);
      n.setNodeID(su2.nodeNum);

      TargetInstrDesc tid = tii.get(n.getMachineOpcode());
      for (int i = 0; i < tid.getNumOperands(); i++) {
        if (tid.getOperandConstraint(i, TIED_TO) != -1) {
          su2.isTwoAddress = true;
          break;
        }
      }
      if (tid.isCommutable())
        su2.isCommutable = true;

      boolean isNewLoad = true;
      SUnit loadSU = null;
      if (loadNode.getNodeID() != -1) {
        loadSU = sunits.get(loadNode.getNodeID());
        isNewLoad = false;
      } else {
        loadSU = newSUnit(loadNode);
        loadNode.setNodeID(loadSU.nodeNum);
      }

      SDep chainPred = new SDep();
      ArrayList<SDep> chainSuccs = new ArrayList<>();
      ArrayList<SDep> loadPreds = new ArrayList<>();
      ArrayList<SDep> nodePreds = new ArrayList<>();
      ArrayList<SDep> nodeSuccs = new ArrayList<>();
      for (SDep d : su.preds) {
        if (d.isCtrl())
          chainPred = d;
        else if (d.getSUnit().getNode() != null &&
            d.getSUnit().getNode().isOperandOf(loadNode))
          loadPreds.add(d);
        else
          nodePreds.add(d);
      }

      for (SDep d : su.succs) {
        if (d.isCtrl())
          chainSuccs.add(d);
        else
          nodeSuccs.add(d);
      }

      if (chainPred.getSUnit() != null) {
        removePred(su, chainPred);
        if (isNewLoad)
          addPred(loadSU, chainPred);
      }

      for (SDep d : loadPreds) {
        removePred(su, d);
        if (isNewLoad)
          addPred(loadSU, d);
      }
      for (SDep d : nodePreds) {
        removePred(su, d);
        addPred(su2, d);
      }
      for (SDep d : nodeSuccs) {
        SUnit succDep = d.getSUnit();
        d.setSUnit(su);
        removePred(succDep, d);
        d.setSUnit(su2);
        addPred(succDep, d);
      }
      for (SDep d : chainSuccs) {
        SUnit succDep = d.getSUnit();
        d.setSUnit(su);
        removePred(succDep, d);
        if (isNewLoad) {
          d.setSUnit(loadSU);
          addPred(succDep, d);
        }
      }
      if (isNewLoad)
        addPred(su2, new SDep(loadSU, SDep.Kind.Order, loadSU.latency, 0));

      if (su2.numSuccsLeft == 0) {
        su2.isAvailable = true;
        return su2;
      }
      su = su2;
    }

    newSU = clone(su);

    for (SDep d : su.preds) {
      if (!d.isArtificial())
        addPred(newSU, d);
    }

    ArrayList<Pair<SUnit, SDep>> delDeps = new ArrayList<>();
    for (SDep d : su.succs) {
      if (d.isArtificial())
        continue;
      SUnit succSU = d.getSUnit();
      if (succSU.isScheduled) {
        SDep dd = d.clone();
        dd.setSUnit(newSU);
        addPred(succSU, dd);
        dd.setSUnit(su);
        delDeps.add(Pair.get(succSU, dd));
      }
    }
    delDeps.forEach(itr -> {
      removePred(itr.first, itr.second);
    });
    return newSU;
  }

  private void insertCopiesAndMoveSuccs(SUnit su, int reg, TargetRegisterClass dstRC,
                                        TargetRegisterClass srcRC, ArrayList<SUnit> copies) {
    SUnit copyFromSU = newSUnit(null);
    copyFromSU.copyDstRC = dstRC;
    copyFromSU.copySrcRC = srcRC;

    SUnit copyToSU = newSUnit(null);
    copyToSU.copySrcRC = srcRC;
    copyToSU.copyDstRC = dstRC;

    ArrayList<Pair<SUnit, SDep>> deps = new ArrayList<>();
    for (SDep d : su.succs) {
      if (d.isArtificial())
        continue;
      SUnit succSU = d.getSUnit();
      if (succSU.isScheduled) {
        SDep de = d.clone();
        d.setSUnit(copyToSU);
        addPred(succSU, de);
        deps.add(Pair.get(succSU, d));
      }
    }

    deps.forEach(d -> {
      removePred(d.first, d.second);
    });

    addPred(copyFromSU, new SDep(su, SDep.Kind.Data, su.latency, reg));
    addPred(copyToSU,
        new SDep(copyFromSU, SDep.Kind.Data, copyFromSU.latency, 0));

    copies.add(copyFromSU);
    copies.add(copyToSU);
  }

  static EVT getPhysicalRegisterVT(SDNode n, int reg, TargetInstrInfo tii) {
    TargetInstrDesc tid = tii.get(n.getMachineOpcode());
    Util.assertion(tid.implicitDefs != null && tid.implicitDefs.length > 0);
    int numRes = tid.getNumDefs();
    for (int def : tid.implicitDefs) {
      if (def == reg)
        break;
      ++numRes;
    }
    return n.getValueType(numRes);
  }


  private boolean delayForLiveRegsBottemUp(SUnit su,
                                           TIntArrayList lregs) {
    if (numLiveRegs == 0)
      return false;

    TIntHashSet regAdded = new TIntHashSet();
    for (SDep d : su.preds) {
      if (d.isAssignedRegDep()) {
        int reg = d.getReg();
        if (liveRegDefs[reg] == null && !liveRegDefs[reg].equals(d.getSUnit())) {
          if (regAdded.add(reg))
            lregs.add(reg);
        }

        if (tri.getAliasSet(reg) != null && tri.getAliasSet(reg).length > 0) {
          for (int alias : tri.getAliasSet(reg)) {
            if (liveRegDefs[alias] != null && !liveRegDefs[alias].equals(d.getSUnit())) {
              if (regAdded.add(alias))
                lregs.add(alias);
            }
          }
        }
      }
    }

    for (SDNode node = su.getNode(); node != null; node = node.getFlaggedNode()) {
      if (!node.isMachineOpecode())
        continue;
      TargetInstrDesc tid = tii.get(node.getMachineOpcode());
      if (tid.implicitDefs == null)
        continue;

      for (int defReg : tid.implicitDefs) {
        if (liveRegDefs[defReg] == null && !liveRegDefs[defReg].equals(su)) {
          if (regAdded.add(defReg))
            lregs.add(defReg);
        }

        if (tri.getAliasSet(defReg) != null && tri.getAliasSet(defReg).length > 0) {
          for (int alias : tri.getAliasSet(defReg)) {
            if (liveRegDefs[alias] != null && !liveRegDefs[alias].equals(su)) {
              if (regAdded.add(alias))
                lregs.add(alias);
            }
          }
        }
      }
    }
    return !lregs.isEmpty();
  }

  private void listScheduleBottomUp() {
    int curCycle = 0;

    releasePredecessors(exitSU, curCycle);
    if (!sunits.isEmpty()) {
      SUnit rootSU = sunits.get(dag.getRoot().getNode().getNodeID());
      Util.assertion(rootSU.succs.isEmpty());
      rootSU.isAvailable = true;
      availableQueue.push(rootSU);
    }

    ArrayList<SUnit> notReady = new ArrayList<>();
    HashMap<SUnit, TIntArrayList> lregsMap = new HashMap<>();
    while (!availableQueue.isEmpty()) {
      boolean delayed = false;
      lregsMap.clear();
      SUnit curSU = availableQueue.pop();
      while (curSU != null) {
        TIntArrayList lregs = new TIntArrayList();
        if (!delayForLiveRegsBottemUp(curSU, lregs))
          break;
        delayed = true;
        lregsMap.put(curSU, lregs);

        curSU.isPending = true;
        notReady.add(curSU);
        curSU = availableQueue.pop();
      }

      if (delayed && curSU == null) {
        SUnit trySU = notReady.get(0);
        TIntArrayList lregs = lregsMap.get(trySU);
        Util.assertion(lregs.size() == 1, "Can't cope with this situation!");
        int reg = lregs.get(0);
        SUnit lrDef = liveRegDefs[reg];
        EVT vt = getPhysicalRegisterVT(lrDef.getNode(), reg, tii);
        TargetRegisterClass rc = tri.getPhysicalRegisterRegClass(reg, vt);
        TargetRegisterClass destRC = tri.getCrossCopyRegClass(rc);

        SUnit newDef = null;
        if (destRC != null)
          newDef = copyAndMoveSuccessors(lrDef);
        else
          destRC = rc;

        if (newDef == null) {
          ArrayList<SUnit> copies = new ArrayList<>();
          insertCopiesAndMoveSuccs(lrDef, reg, destRC, rc, copies);
          addPred(trySU, new SDep(copies.get(0), SDep.Kind.Order,
              1, 0, false, false, false));
          newDef = copies.get(copies.size() - 1);
        }

        liveRegDefs[reg] = newDef;
        addPred(newDef, new SDep(trySU, SDep.Kind.Order, 1,
            0, false, false, true));
        trySU.isAvailable = true;
        curSU = newDef;
      }

      for (int i = 0, e = notReady.size(); i < e; i++) {
        notReady.get(i).isPending = false;
        if (notReady.get(i).isAvailable)
          availableQueue.push(notReady.get(i));
      }

      notReady.clear();

      if (curSU != null)
        scheduleNodeBottomUp(curSU, curCycle);
      ++curCycle;
    }

    // Rerverse the order if ti is bottom up.
    Collections.reverse(sequence);
  }

  protected boolean forceUnitLatencies() {
    return true;
  }

  /**
   * A static factory method used for creating an ScheduleFast pass.
   */
  public static ScheduleDAGSDNodes createFastDAGScheduler(SelectionDAGISel isel, CodeGenOpt level) {
    return new ScheduleDAGFast(isel.mf);
  }
}