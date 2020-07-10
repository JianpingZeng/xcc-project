/*
 * Extremely Compiler Collection
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

import backend.analysis.MachineLoopInfo;
import backend.codegen.pbqp.*;
import backend.pass.AnalysisUsage;
import backend.support.MachineFunctionPass;
import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.BitMap;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.Map;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * This file define a pass that takes responsibility for assginming virtual register
 * to physical register by Partitioned Boolean Qruadran Programming method.
 * <p>
 * It maps the register allocation into a PBQP problem and solve it by reduction
 * algorithm. Then map the solution of PBQP back to register assignment.
 * If you want to known more information about PBQP register allocation in details,
 * following two papers are useful to understand the working flow of PBQP algorithm:
 * <ol>
 * <li>Eckstein, Erik, and E. Eckstein. "Register allocation for irregular architectures.
 * "Joint Conference on Languages, Compilers and TOOLS for Embedded Systems: Software
 * and Compilers for Embedded Systems ACM, 2002:139-148.
 * </li>
 * <li>Lang, Hames, and B. Scholz. "Nearly Optimal Register Allocation with PBQP.
 * "Joint Modular Languages Conference Springer Berlin Heidelberg, 2006:346-361.
 * </li>
 * </ol>
 * </p>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class RegAllocPBQP extends MachineFunctionPass {
  public static RegAllocPBQP createPBQPRegisterAllocator() {
    return new RegAllocPBQP();
  }

  private MachineFunction mf;
  private LiveIntervalAnalysis li;
  private LiveStackSlot ls;
  private MachineLoopInfo ml;
  private ArrayList<LiveInterval> virtIntervalToBeHandled;
  private ArrayList<LiveInterval> emptyIntervalToBeHandled;
  private PhysRegTracker pst;
  private TargetRegisterInfo tri;
  private TargetInstrInfo tii;
  private MachineRegisterInfo mri;
  private LiveInterval[] li2Nodes;
  private TObjectIntHashMap<LiveInterval> node2LI;
  private BitMap[] allowedRegs;
  private VirtRegMap vrm;

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.addRequired(LiveIntervalAnalysis.class);
    au.addRequired(LiveIntervalCoalescing.class);
    au.addRequired(LiveStackSlot.class);
    au.addPreserved(LiveStackSlot.class);
    au.addRequired(MachineLoopInfo.class);
    au.addPreserved(MachineLoopInfo.class);
    super.getAnalysisUsage(au);
  }

  private void initIntervalsSet() {
    for (Map.Entry<Integer, LiveInterval> ins : li.reg2LiveInterval.entrySet()) {
      if (isPhysicalRegister(ins.getKey()))
        continue;

      if (ins.getValue().isEmpty())
        emptyIntervalToBeHandled.add(ins.getValue());
      else
        virtIntervalToBeHandled.add(ins.getValue());
    }
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    // Initialize virtualinterval to be handled list and empty list
    this.mf = mf;
    li = (LiveIntervalAnalysis) getAnalysisToUpDate(LiveIntervalAnalysis.class);
    ls = (LiveStackSlot) getAnalysisToUpDate(LiveStackSlot.class);
    ml = (MachineLoopInfo) getAnalysisToUpDate(MachineLoopInfo.class);
    virtIntervalToBeHandled = new ArrayList<>();
    emptyIntervalToBeHandled = new ArrayList<>();
    tri = mf.getSubtarget().getRegisterInfo();
    pst = new PhysRegTracker(tri);
    tii = mf.getSubtarget().getInstrInfo();
    mri = mf.getMachineRegisterInfo();
    vrm = new VirtRegMap(mf);

    initIntervalsSet();

    if (!virtIntervalToBeHandled.isEmpty()) {
      boolean pbqpAllocComplete = false;
      int round = 0;
      while (!pbqpAllocComplete) {
        if (Util.DEBUG)
          System.err.println("PBQP RegAlloc round " + round + ":");
        // construct PBQP problem
        PBQPGraph problem = constructPBQPProblem();
        if (Util.DEBUG) {
          System.err.print("Allowed Register: [");
          int i = 0, e = allowedRegs[0].size();
          for (int reg = allowedRegs[0].findFirst(); reg > 0; reg = allowedRegs[0].findNext(reg+1)) {
            System.err.printf("%s", tri.getName(reg));
            if (i < e - 1)
              System.err.print(',');
            ++i;
          }
          System.err.println("]");
          for (PBQPVector node : problem.nodeCosts)
            node.dump();
          System.err.println();

          for (i = 0; i < problem.numNodes; i++) {
            for (AdjNode adj = problem.adjList[i]; adj != null; adj = adj.next) {
              if (i < adj.adj) {
                System.err.printf("Edge<n%d, n%d>:\n", i, adj.adj);
                adj.cost.dump();
                System.err.println();
              }
            }
          }
        }

        // solve it
        HeuristicSolver solver = new HeuristicSolver();
        PBQPSolution solution = solver.solve(problem);
        pbqpAllocComplete = mapPBQPToRegAlloc(solution);
        round++;
      }
    }

    // Finalize allocation, allocate emptry ranges.
    finalizeAlloc();

    virtIntervalToBeHandled.clear();
    emptyIntervalToBeHandled.clear();
    VirtRegRewriter regRewriter = VirtRegRewriter.createVirtRegRewriter();
    regRewriter.runOnMachineFunction(mf, vrm);
    return false;
  }

  @Override
  public String getPassName() {
    return "PBQP register allocator Pass";
  }

  /**
   * Walks through specified machine function to compute coalscing register pair
   * and it's benefit.
   *
   * @return
   */
  private TObjectDoubleHashMap<Pair<Integer, Integer>> constructCoalesceMap() {
    TObjectDoubleHashMap<Pair<Integer, Integer>> map = new TObjectDoubleHashMap<>();
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      for (MachineInstr mi : mbb.getInsts()) {
        OutRef<Integer> src, dest;
        src = new OutRef<>();
        dest = new OutRef<>();
        if (!tii.isMoveInstr(mi, src, dest, null, null))
          continue;

        int srcRegister = src.get();
        int destRegister = dest.get();

        // If the source and destination of move instr is same, it's not
        // needed to perform coalscing.
        if (srcRegister == destRegister)
          continue;

        // Can not coalescing two physical register.
        boolean isSrcPhyReg = isPhysicalRegister(srcRegister);
        boolean isDestPhyReg = isPhysicalRegister(destRegister);
        if (isSrcPhyReg && isDestPhyReg)
          continue;

        MCRegisterClass srcRC = isSrcPhyReg ?
            tri.getPhysicalRegisterRegClass(srcRegister) :
            mri.getRegClass(srcRegister);
        MCRegisterClass destRC = isDestPhyReg ?
            tri.getPhysicalRegisterRegClass(destRegister) :
            mri.getRegClass(destRegister);
        if (srcRC != destRC)
          continue;

        if (isSrcPhyReg)
          if (!tri.getAllocatableSet(mf, srcRC).get(srcRegister))
            continue;
        if (isDestPhyReg)
          if (!tri.getAllocatableSet(mf, destRC).get(destRegister))
            continue;

        LiveInterval srcInterval = li.getInterval(srcRegister);
        LiveInterval destInterval = li.getInterval(destRegister);
        if (srcInterval.overlaps(destInterval))
          continue;

        double benefit = Math.pow(10, ml.getLoopDepth(mbb));
        map.put(Pair.get(srcRegister, destRegister), benefit);
        map.put(Pair.get(destRegister, srcRegister), benefit);
      }
    }
    return map;
  }

  private PBQPGraph constructPBQPProblem() {
    TObjectDoubleHashMap<Pair<Integer, Integer>> coalsceMap;
    PBQPGraph graph;

    li2Nodes = new LiveInterval[virtIntervalToBeHandled.size()];
    virtIntervalToBeHandled.toArray(li2Nodes);

    node2LI = new TObjectIntHashMap<>();
    for (int i = 0; i < li2Nodes.length; i++) {
      node2LI.put(li2Nodes[i], i);
    }

    allowedRegs = new BitMap[virtIntervalToBeHandled.size()];
    ArrayList<LiveInterval> phyIntervals = new ArrayList<>();

    for (Map.Entry<Integer, LiveInterval> ins : li.reg2LiveInterval.entrySet()) {
      if (isPhysicalRegister(ins.getKey())) {
        pst.addRegUse(ins.getKey());
        phyIntervals.add(ins.getValue());
      }
    }

    // construct coalesce map
    coalsceMap = constructCoalesceMap();

    graph = new PBQPGraph(li2Nodes.length);

    if (Util.DEBUG)
      System.err.println("Number of PBQP graph node is:" + li2Nodes.length);

    for (int node = 0; node < li2Nodes.length; node++) {
      LiveInterval interval = li2Nodes[node];
      BitMap isAllowed = tri.getAllocatableSet(mf, mri.getRegClass(interval.register));

      // Remove some physical register that conflicts with physical interval
      // from isAllowed
      for (LiveInterval phyInt : phyIntervals) {
        if (!phyInt.overlaps(li2Nodes[node]))
          continue;

        // If this physical register coalesces with virtual register, so
        // it is ok.
        if (coalsceMap.containsKey(Pair.get(phyInt.register, interval.register)))
          continue;

        if (isAllowed.get(phyInt.register))
          isAllowed.clear(phyInt.register);

        int[] alias = tri.getAliasSet(phyInt.register);
        if (alias != null && alias.length > 0) {
          for (int aliasReg : alias) {
            if (isAllowed.get(aliasReg))
              isAllowed.clear(aliasReg);
          }
        }
      }

      allowedRegs[node] = isAllowed;
      float weight = li2Nodes[node].weight;
      double spillCost = weight != 0.0f ? weight : Double.MIN_VALUE;
      graph.addNodeCosts(node,
          constructCostVector(interval.register, allowedRegs[node],
              coalsceMap,
              spillCost));
    }

    for (int i = 0; i < li2Nodes.length; i++) {
      LiveInterval li1 = li2Nodes[i];
      for (int j = i + 1; j < li2Nodes.length; j++) {
        LiveInterval li2 = li2Nodes[j];
        Pair<Integer, Integer> regPair =
            Pair.get(li1.register, li2.register);
        PBQPMatrix m;
        if (coalsceMap.containsKey(regPair)) {
          if (Util.DEBUG)
            System.err.println("Node n" + i + " coalesce with node n" + j);

          m = buildCoalscingEdgeCosts(allowedRegs[i], allowedRegs[j],
              coalsceMap.get(regPair));
        } else if (li2Nodes[i].overlaps(li2Nodes[j])) {
          if (Util.DEBUG)
            System.err.println("Node n" + i + " interference with node n" + j);

          m = buildInterferenceEdgeCosts(allowedRegs[i], allowedRegs[j]);
        } else
          continue;
        graph.addEdgeCosts(i, j, m);
      }
    }

    return graph;
  }

  private PBQPMatrix buildCoalscingEdgeCosts(BitMap allowedReg1,
                                             BitMap allowedReg2,
                                             double cost) {
    PBQPMatrix m = new PBQPMatrix(allowedReg1.size() + 1, allowedReg2.size() + 1);
    boolean isAllZero = true;

    int i = 1;
    for (int reg1 = allowedReg1.findFirst(); reg1 > 0; reg1 = allowedReg1.findNext(reg1+1)) {
      int j = 1;
      for (int reg2 = allowedReg2.findFirst(); reg2 > 0; reg2 = allowedReg2.findNext(reg2+1)) {
        if (reg1 == reg2) {
          m.set(i, j, -cost);
          isAllZero = false;
        }
        j++;
      }
      i++;
    }
    if (isAllZero)
      return null;
    return m;
  }

  private PBQPMatrix buildInterferenceEdgeCosts(BitMap allowedReg1,
                                                BitMap allowedReg2) {
    PBQPMatrix m = new PBQPMatrix(allowedReg1.size() + 1, allowedReg2.size() + 1);
    boolean isAllZero = true;

    int i = 1;
    for (int reg1 = allowedReg1.findFirst(); reg1 > 0; reg1 = allowedReg1.findNext(reg1+1)) {
      int j = 1;
      for (int reg2 = allowedReg2.findFirst(); reg2 > 0; reg2 = allowedReg2.findNext(reg2+1)) {
        // If the row/column regs are identical or alias insert an infinity.
        if (tri.regsOverlap(reg1, reg2)) {
          m.set(i, j, Double.MAX_VALUE);
          isAllZero = false;
        }
        j++;
      }
      i++;
    }
    if (isAllZero)
      return null;
    return m;
  }

  private PBQPVector constructCostVector(int vreg,
                                         BitMap allowedReg,
                                         TObjectDoubleHashMap<Pair<Integer, Integer>> coalsceMap,
                                         double spillCost) {
    PBQPVector cost = new PBQPVector(allowedReg.size() + 1);
    cost.set(0, spillCost);

    int i = 0;
    for (int preg = allowedReg.findFirst(); preg > 0; preg = allowedReg.findNext(preg+1)) {
      Pair<Integer, Integer> regPair = Pair.get(preg, vreg);
      if (coalsceMap.containsKey(regPair)) {
        cost.set(i + 1, -coalsceMap.get(regPair));
      }
      ++i;
    }
    return cost;
  }

  /**
   * Create a live Interval for a stack slot if the specified live interval has
   * been spilled.
   *
   * @param cur
   * @param ls
   * @param li
   * @param mri
   * @param vrm
   */
  private static void addStackInterval(
      LiveInterval cur,
      LiveStackSlot ls,
      LiveIntervalAnalysis li,
      MachineRegisterInfo mri,
      VirtRegMap vrm) {
    if (!vrm.hasStackSlot(cur.register))
      return;
    int ss = vrm.getStackSlot(cur.register);
    MCRegisterClass rc = mri.getRegClass(cur.register);
    LiveInterval slotInterval = ls.getOrCreateInterval(ss, rc);
    int valNumber;
    if (slotInterval.hasAtLeastOneValue())
      valNumber = slotInterval.getRange(0).valId;
    else
      valNumber = slotInterval.getNextValue();

    LiveInterval regInterval = li.getInterval(cur.register);
    slotInterval.mergeRangesInAsValue(regInterval, valNumber);
  }

  private boolean mapPBQPToRegAlloc(PBQPSolution solution) {
    boolean neededAnotherRound = false;
    for (int node = 0; node < li2Nodes.length; node++) {
      LiveInterval interval = li2Nodes[node];
      int virReg = interval.register;
      int idx = solution.get(node);
      if (idx != 0) {
        vrm.assignVirt2Phys(virReg, idx);
        if (Util.DEBUG)
          System.err.printf("Assign %%reg%d to virtual register %%%s%n",
              virReg, tri.getName(idx));
      } else {
        // This live interval been spilled into stack.
        int slot = vrm.assignVirt2StackSlot(virReg);
        ArrayList<LiveInterval> newIS = li.addIntervalsForSpills(interval, vrm, slot);
        addStackInterval(interval, ls, li, mri, vrm);

        if (Util.DEBUG) {
          interval.print(System.err, tri);
          System.err.printf(" Spilled with weight %f%n", interval.weight);
        }
        virtIntervalToBeHandled.addAll(newIS);
        neededAnotherRound = true;
      }
    }
    return !neededAnotherRound;
  }

  private void finalizeAlloc() {
    for (LiveInterval interval : emptyIntervalToBeHandled) {
      int phyReg = interval.register;
      if (phyReg == 0) {
        phyReg = tri.getAllocatableSet(mf, mri.getRegClass(interval.register)).findFirst();
        Util.assertion(phyReg > 0, "No available physical register in PBQP register allocator!");
      }
      vrm.assignVirt2Phys(interval.register, phyReg);
    }

    MachineBasicBlock entryMBB = mf.getEntryBlock();
    ArrayList<MachineBasicBlock> liveMBBs = new ArrayList<>();
    for (int i = 0, e = li.getNumIntervals(); i < e; i++) {
      LiveInterval interval = li.getInterval(i);
      if (interval == null) continue;
      int reg;
      if (isPhysicalRegister(interval.register))
        reg = interval.register;
      else if (vrm.isAssignedReg(interval.register))
        reg = vrm.getPhys(interval.register);
      else
        continue;

      if (reg == 0)
        continue;


      for (LiveRange range : interval.ranges) {
        if (li.findLiveinMBBs(range.start, range.end, liveMBBs)) {
          for (MachineBasicBlock mbb : liveMBBs) {
            if (mbb != entryMBB && !mbb.getLiveIns().contains(reg))
              mbb.addLiveIn(reg);
          }
        }
      }
      liveMBBs.clear();
    }
  }
}
