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

package backend.codegen;

import backend.analysis.LiveVariables;
import backend.analysis.MachineLoopInfo;
import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.support.MachineFunctionPass;
import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import gnu.trove.map.hash.TIntIntHashMap;
import tools.OutRef;
import tools.Pair;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.Initializer;
import tools.commandline.OptionNameApplicator;

import java.util.ArrayList;

import static backend.codegen.LiveIntervalAnalysis.getDefIndex;
import static backend.support.PrintMachineFunctionPass.createMachineFunctionPrinterPass;
import static backend.target.TargetOptions.PrintMachineCode;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;
import static backend.target.TargetRegisterInfo.isVirtualRegister;
import static tools.commandline.Desc.desc;

/**
 * This file defines a class takes responsibility for performing register coalescing
 * on live interval to eliminate redundant move instruction.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class LiveIntervalCoalescing extends MachineFunctionPass {
  public static IntStatistic numJoins =
      new IntStatistic("liveIntervals", "Number of intervals joins performed");

  public static IntStatistic numIntervalsAfter =
      new IntStatistic("liveIntervals", "Number of intervals after coalescing");

  public static IntStatistic numPeep =
      new IntStatistic("liveIntervals", "Number of identit moves eliminated after coalescing");

  public static BooleanOpt EnableJoining = new BooleanOpt(
      OptionNameApplicator.optionName("join-liveintervals"),
      desc("Join compatible live interval"),
      Initializer.init(true));


  private MachineFunction mf;
  private TargetMachine tm;
  private LiveIntervalAnalysis li;
  private TargetRegisterInfo tri;
  private MachineRegisterInfo mri;
  private LiveVariables lv;
  public TIntIntHashMap r2rMap = new TIntIntHashMap();

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    Util.assertion(au != null);
    au.addPreserved(MachineLoopInfo.class);
    au.addPreserved(LiveIntervalAnalysis.class);
    au.addPreserved(LiveVariables.class);
    au.addPreserved(PhiElimination.class);
    au.addPreserved(TwoAddrInstructionPass.class);
    super.getAnalysisUsage(au);
  }

  /**
   * Returns the representative of this register.
   *
   * @param reg
   * @return
   */
  public int rep(int reg) {
    if (r2rMap.containsKey(reg))
      return rep(r2rMap.get(reg));

    return reg;
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    this.mf = mf;
    tm = mf.getTarget();
    li = (LiveIntervalAnalysis) getAnalysisToUpDate(LiveIntervalAnalysis.class);
    tri = tm.getRegisterInfo();
    mri = mf.getMachineRegisterInfo();
    lv = (LiveVariables) getAnalysisToUpDate(LiveVariables.class);

    joinIntervals();

    numIntervalsAfter.add(li.getNumIntervals());

    // perform a final pass over the instructions and compute spill
    // weights, coalesce virtual registers and remove identity moves
    MachineLoopInfo loopInfo = (MachineLoopInfo) getAnalysisToUpDate(MachineLoopInfo.class);
    TargetInstrInfo tii = tm.getInstrInfo();

    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      int loopDepth = loopInfo.getLoopDepth(mbb);

      for (int i = 0; i < mbb.size(); i++) {
        MachineInstr mi = mbb.getInstAt(i);
        OutRef<Integer> srcReg = new OutRef<>(0);
        OutRef<Integer> dstReg = new OutRef<>(0);
        int regRep;

        // If the move will be an identify move delete it.
        if (tii.isMoveInstr(mi, srcReg, dstReg, null, null)
            && (regRep = rep(srcReg.get())) == rep(dstReg.get())
            && regRep != 0) {
          // Remove from def list.
          // LiveInterval interval = getOrCreateInterval(regRep);
          if (li.mi2Idx.containsKey(mi)) {
            li.putIndex2MI(li.mi2Idx.get(mi) /
                LiveIntervalAnalysis.InstrSlots.NUM, null);
            li.mi2Idx.remove(mi);
          }
          mi.removeFromParent();
          --i;
          numPeep.inc();
        } else {
          for (int j = 0, e = mi.getNumOperands(); j < e; j++) {
            MachineOperand mo = mi.getOperand(j);
            if (mo.isRegister() && mo.getReg() != 0 && isVirtualRegister(mo.getReg())) {
              // Replace register with representative register.
              int reg = rep(mo.getReg());
              if (reg != mo.getReg())
                mi.setMachineOperandReg(j, reg);

              LiveInterval interval = li.getInterval(reg);
              if (interval == null)
                continue;
              interval.weight += ((mo.isUse() ? 1 : 0) + (mo.isDef() ? 1 : 0)) + Math.pow(10, loopDepth);
            }
          }
        }
      }
    }

    if (PrintMachineCode.value) {
      createMachineFunctionPrinterPass(System.err,
          "# *** IR dump after register coalescing ***:\n")
          .runOnMachineFunction(mf);
    }
    // The r2rMap is not cleared after this pass be run which causes performancing
    // incorrect interval coalescing on %vreg1027 and %vreg1029.
    r2rMap.clear();
    return true;
  }

  @Override
  public String getPassName() {
    return "Live Interval Coalescing Pass";
  }

  private void joinIntervals() {
    if (Util.DEBUG)
      System.err.println("************ Joining Intervals *************");

    MachineLoopInfo loopInfo = (MachineLoopInfo) getAnalysisToUpDate(MachineLoopInfo.class);
    if (loopInfo != null) {
      if (loopInfo.isNoTopLevelLoop()) {
        // If there are no loops in the function, join intervals in function
        // order.
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
          joinIntervalsInMachineBB(mbb);
      } else {
        // Otherwise, join intervals in inner loops before other intervals.
        // Unfortunately we can't just iterate over loop hierarchy here because
        // there may be more MBB's than BB's.  Collect MBB's for sorting.
        ArrayList<Pair<Integer, MachineBasicBlock>> mbbs = new ArrayList<>();
        mf.getBasicBlocks().forEach(mbb -> {
          mbbs.add(Pair.get(loopInfo.getLoopDepth(mbb),
              mbb));
        });

        // Sort mbb by loop depth.
        mbbs.sort((lhs, rhs) ->
        {
          if (lhs.first > rhs.first)
            return -1;
          if (lhs.first.equals(rhs.first) && lhs.second.getNumber() < rhs.second.getNumber())
            return 0;
          return 1;
        });

        // Finally, joi intervals in loop nest order.
        for (Pair<Integer, MachineBasicBlock> pair : mbbs) {
          joinIntervalsInMachineBB(pair.second);
        }
      }
    }

    if (Util.DEBUG) {
      System.err.println("**** Register mapping ***");
      for (int key : r2rMap.keys()) {
        System.err.printf(" reg %d -> reg %d\n", key, r2rMap.get(key));
      }
      System.err.println("\n**** After Register coalescing ****");
      li.print(System.err, mf.getFunction().getParent());
    }
  }

  private void joinIntervalsInMachineBB(MachineBasicBlock mbb) {
    if (Util.DEBUG)
      System.err.printf("%s:\n", mbb.getBasicBlock().getName());
    TargetInstrInfo tii = tm.getInstrInfo();

    for (MachineInstr mi : mbb.getInsts()) {
      if (Util.DEBUG) {
        System.err.printf("%d\t", li.getInstructionIndex(mi));
        mi.print(System.err, tm);
      }

      // we only join virtual registers with allocatable
      // physical registers since we do not have liveness information
      // on not allocatable physical registers
      OutRef<Integer> srcReg = new OutRef<>(0);
      OutRef<Integer> dstReg = new OutRef<>(0);
      if (tii.isMoveInstr(mi, srcReg, dstReg, null, null)
          && (isVirtualRegister(srcReg.get())
          || lv.getAllocatablePhyRegs().get(srcReg.get()))
          && (isVirtualRegister(dstReg.get())
          || lv.getAllocatablePhyRegs().get(dstReg.get()))) {
        // Get representative register.
        int regA = rep(srcReg.get());
        int regB = rep(dstReg.get());

        if (regA == regB)
          continue;

        // If there are both physical register, we can not join them.
        if (isPhysicalRegister(regA) && isPhysicalRegister(regB)) {
          continue;
        }

        // If they are not of the same register class, we cannot join them.
        if (differingRegisterClasses(regA, regB)) {
          continue;
        }

        LiveInterval intervalA = li.getInterval(regA);
        LiveInterval intervalB = li.getInterval(regB);
        Util.assertion(intervalA.register == regA && intervalB.register == regB, "Regitser mapping is horribly broken!");


        if (Util.DEBUG) {
          System.err.print("Inspecting ");
          intervalA.print(System.err, tri);
          System.err.print(" and ");
          intervalB.print(System.err, tri);
          System.err.print(": ");
        }

        // If two intervals contain a single value and are joined by a copy, it
        // does not matter if the intervals overlap, they can always be joined.
        boolean triviallyJoinable = intervalA.containsOneValue() &&
            intervalB.containsOneValue();

        int midDefIdx = getDefIndex(li.getInstructionIndex(mi));
        if ((triviallyJoinable || intervalB.joinable(intervalA, midDefIdx))
            && !overlapsAliases(intervalA, intervalB)) {
          intervalB.join(intervalA, midDefIdx);

          if (!isPhysicalRegister(regA)) {
            r2rMap.remove(regA);
            r2rMap.put(regA, regB);
            li.reg2LiveInterval.remove(regA, li.reg2LiveInterval.get(regA));
          } else {
            r2rMap.put(regB, regA);
            intervalB.register = regA;
            intervalA.swap(intervalB);
            li.reg2LiveInterval.remove(regB, li.reg2LiveInterval.get(regB));
          }
          numJoins.inc();
        } else {
          if (Util.DEBUG)
            System.err.println("Interference!");
        }
      }
    }
  }


  /**
   * @param src
   * @param dest
   * @return
   */
  private boolean overlapsAliases(LiveInterval src, LiveInterval dest) {
    if (!isPhysicalRegister(src.register)) {
      if (!isPhysicalRegister(dest.register))
        return false;       // Virtual register never aliased.
      LiveInterval temp = src;
      src = dest;
      dest = temp;
    }

    Util.assertion(isPhysicalRegister(src.register), "First interval describe a physical register");


    for (int alias : tri.getAliasSet(src.register)) {
      LiveInterval aliasLI = li.getInterval(alias);
      if (aliasLI == null)
        continue;
      if (dest.overlaps(aliasLI))
        return true;
    }
    return false;
  }

  /**
   * Return true if the two specified registers belong to different register
   * classes.  The registers may be either phys or virt regs.
   *
   * @param regA
   * @param regB
   * @return
   */
  private boolean differingRegisterClasses(int regA, int regB) {
    MCRegisterClass rc = null;
    if (isPhysicalRegister(regA)) {
      Util.assertion(isVirtualRegister(regB), "Can't consider two physical register");
      rc = mri.getRegClass(regB);
      return !rc.contains(regA);
    }

    // Compare against the rc for the second reg.
    rc = mri.getRegClass(regA);
    if (isVirtualRegister(regB))
      return !rc.equals(mri.getRegClass(regB));
    else
      return !rc.contains(regB);
  }
}
