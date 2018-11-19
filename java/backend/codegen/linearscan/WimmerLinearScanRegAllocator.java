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

package backend.codegen.linearscan;

import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoop;
import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineRegisterInfo;
import backend.pass.AnalysisUsage;
import backend.support.MachineFunctionPass;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import tools.BitMap;
import tools.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * This class designed for implementing an advancing linear scan register allocator
 * with live internal splitting.
 * <p>
 * If you want to understand more details about this allocator, please
 * refer to the following papers:
 * <pre>
 * 1. Linear Scan Register Allocation for the Java HotSpot™ Client Compiler, Christian Wimmer.
 * 2. Kotzmann, Thomas, et al. "Design of the Java HotSpot™ client compiler for Java 6." ACM
 *    Transactions on Architecture and Code Optimization (TACO) 5.1 (2008): 7.
 * 3. Wimmer, Christian, and Hanspeter Mössenböck. "Optimized interval splitting in a linear scan register allocator."
 *    Proceedings of the 1st ACM/USENIX international conference on Virtual execution environments. ACM, 2005.
 * 4. Wimmer, Christian, and Michael Franz. "Linear scan register allocation on SSA form."
 *    Proceedings of the 8th annual IEEE/ACM international symposium on Code generation and optimization. ACM, 2010.
 * </pre>
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public final class WimmerLinearScanRegAllocator extends MachineFunctionPass {
  public static WimmerLinearScanRegAllocator createWimmerLinearScanRegAlloc() {
    return new WimmerLinearScanRegAllocator();
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.addRequired(LiveIntervalAnalysis.class);
    au.addRequired(SimpleRegisterCoalescer.class);

    // Obtains the loop information used for assigning a spilling weight to
    // each live interval. The more nested, the more weight.
    au.addPreserved(MachineDomTree.class);
    au.addRequired(MachineDomTree.class);
    au.addPreserved(MachineLoop.class);
    au.addRequired(MachineLoop.class);

    super.getAnalysisUsage(au);
  }

  private TreeSet<LiveInterval> unhandled;
  private ArrayList<LiveInterval> handled;
  private ArrayList<LiveInterval> active;
  private ArrayList<LiveInterval> inactive;
  private ArrayList<LiveInterval> fixed;
  private LiveIntervalAnalysis li;
  private TargetRegisterInfo tri;
  private MachineRegisterInfo mri;
  private IntervalLocKeeper ilk;
  private MachineFunction mf;
  private MachineLoop ml;
  private MoveResolver resolver;
  private TargetInstrInfo tii;
  private LiveInterval cur;
  private int position;
  private SimpleVirtualRegSpiller spiller;

  public WimmerLinearScanRegAllocator() {
    unhandled = new TreeSet<>(Comparator.comparingInt(o -> o.first.start));
    handled = new ArrayList<>();
    active = new ArrayList<>();
    inactive = new ArrayList<>();
    fixed = new ArrayList<>();
  }

  public LiveIntervalAnalysis getLiveIntervals() {
    return li;
  }

  public TargetRegisterInfo getRegisterInfo() {
    return tri;
  }

  public IntervalLocKeeper getIntervalLocKeeper() {
    return ilk;
  }

  public TargetInstrInfo getInstrInfo() {
    return tii;
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    this.mf = mf;
    tri = mf.getTarget().getRegisterInfo();
    tii = mf.getTarget().getInstrInfo();
    mri = mf.getMachineRegisterInfo();
    ilk = new IntervalLocKeeper(mf);
    resolver = new MoveResolver(this);

    li = (LiveIntervalAnalysis) getAnalysisToUpDate(LiveIntervalAnalysis.class);
    ml = (MachineLoop) getAnalysisToUpDate(MachineLoop.class);
    spiller = new SimpleVirtualRegSpiller(ilk, mri);

    Util.assertion(ml != null);
    Util.assertion(li != null);

    // Step #1: initialize the interval list.
    initIntervalSet();

    if (Util.DEBUG) {
      System.err.println("******** Unhandled Intervals ********\n");
      for (LiveInterval interval : unhandled)
        interval.dump(tri);
    }

    // Step #2: walk the intervals
    linearScan();

    // after linearscan, if there are any interval contained in active or inactive,
    // remove ti from original list and add it into handled list.
    if (!active.isEmpty())
      handled.addAll(active);
    if (!inactive.isEmpty())
      handled.addAll(inactive);

    // Step #3: resolve move
    resolveDataflow();

    if (Util.DEBUG) {
      System.err.println("******** Handled Intervals ***********");
      for (LiveInterval it : handled) {
        it.dump(tri);
        if (isPhysicalRegister(it.register))
          System.err.printf("            fixed phy reg<%s>%n", tri.getName(it.register));
        else if (ilk.isAssignedPhyReg(it))
          System.err.printf("         assigned phy reg<%s>%n", tri.getName(ilk.getPhyReg(it)));
        else
          System.err.printf("         assigned slot <fi#%d>%n", ilk.getStackSlot(it));
      }
    }
    // Step #4: rewrite the instruction's operand according to it's assigned location.
    spiller.runOnMachineFunction(mf, handled);
    clear();
    return true;
  }

  private void clear() {
    unhandled.clear();
    handled.clear();
    active.clear();
    inactive.clear();
    fixed.clear();
    li = null;
    tri = null;
    mri = null;
    ilk = null;
    mf = null;
    ml = null;
    resolver = null;
    tii = null;
    cur = null;
    position = -1;
    spiller = null;
  }

  private void prehandled(int position) {
    // check for intervals in active that are expired or inactive.
    for (int i = 0, e = active.size(); i < e; i++) {
      LiveInterval it = active.get(i);
      if (it.isExpiredAt(position)) {
        active.remove(i);
        --i;
        --e;

        handled.add(it);
      } else if (!it.isLiveAt(position)) {
        active.remove(i);
        --i;
        --e;
        inactive.add(it);
      }
    }

    // checks for intervals in inactive that are expired or active.
    for (int i = 0, e = inactive.size(); i < e; i++) {
      LiveInterval it = inactive.get(i);
      if (it.isExpiredAt(position)) {
        inactive.remove(i);
        --i;
        --e;
        handled.add(it);
      } else if (it.isLiveAt(position)) {
        inactive.remove(i);
        --i;
        --e;
        active.add(it);
      }
    }
  }

  private void linearScan() {
    while (!unhandled.isEmpty()) {
      cur = unhandled.pollFirst();
      Util.assertion(cur != null);

      position = cur.getFirst().start;

      // pre-handling, like move expired interval from active to handled list.
      prehandled(position);

      // if we find a free register, we are done: assign this virtual to
      // the free physical register and add this interval to the active
      // list.
      int phyReg = getFreePhysReg(cur);
      if (phyReg != 0) {
        ilk.assignInterval2Phys(cur, phyReg);
        active.add(cur);
        continue;
      }

      if (Util.DEBUG) {
        System.err.print("no free register\n");
        System.err.print("\ttry to assign a blocked physical register");
        cur.print(System.err, tri);
        System.err.println(":");
      }

      phyReg = allocateBlockedRegister(cur);
      if (phyReg != 0) {
        ilk.assignInterval2Phys(cur, phyReg);
        active.add(cur);
      } else {
        // Otherwise, current interval would be splitted and spill it's right part
        // into stack.
        ilk.assignInterval2StackSlot(cur);
        handled.add(cur);
      }
    }
  }

  private int allocateBlockedRegister(LiveInterval cur) {
    TargetRegisterClass rc = mri.getRegClass(cur.register);
    int[] allocatableRegs = rc.getAllocableRegs(mf);
    int[] freeUntilPos = new int[tri.getNumRegs()];
    int[] blockPosBy = new int[tri.getNumRegs()];

    // set the free position of all free physical register as
    // integral max value.
    for (int reg : allocatableRegs) {
      freeUntilPos[reg] = Integer.MAX_VALUE;
      blockPosBy[reg] = Integer.MAX_VALUE;
    }
    for (LiveInterval it : active) {
      if (!isPhysicalRegister(it.register))
        freeUntilPos[ilk.getPhyReg(it)] = cur.getUsePointAfter(cur.beginNumber());
      else
        blockPosBy[it.register] = 0;
    }
    for (LiveInterval it : inactive) {
      if (!it.intersect(cur))
        continue;

      if (!isPhysicalRegister(it.register))
        freeUntilPos[ilk.getPhyReg(it)] = cur.getUsePointAfter(cur.beginNumber());
      else
        blockPosBy[it.register] = it.intersectAt(cur);
    }


    int reg = -1;
    int max = -1;
    for (int i = 0; i < freeUntilPos.length; i++) {
      if (freeUntilPos[i] > max) {
        max = freeUntilPos[i];
        reg = i;
      }
    }
    Util.assertion(reg != -1, "No physical register found!");
    int firstUseOfCurr = cur.getFirstUse();
    if (freeUntilPos[reg] < firstUseOfCurr) {
      // all active and inactive interval are used before first use of current interval.
      // so we need to spill current interval and split it at an optimal position
      // before firstUseOfCurr.
      LiveInterval splitedChild = splitBeforeUsage(cur, freeUntilPos[reg] + 1, firstUseOfCurr);
      unhandled.add(splitedChild);

      // return 0 indicates current interval would be assigned with a stack slot.
      return 0;
    }
    int splitPos = blockPosBy[reg];
    boolean needsSplit = splitPos <= cur.endNumber();
    Util.assertion(splitPos > 0);
    Util.assertion(needsSplit || splitPos > cur.beginNumber());

    // register not available for full interval : so split it, assign the current interval to reg.
    ilk.assignInterval2Phys(cur, reg);
    if (needsSplit)
      unhandled.add(splitIntervalWhenPartialAvailable(cur, splitPos));

    // perform splitting and spilling for all affected intervals
    splitAndSpillIntersectingInterval(reg);
    return reg;
  }

  private void splitAndSpillIntersectingInterval(int reg) {
    for (Iterator<LiveInterval> itr = active.iterator(); itr.hasNext(); ) {
      LiveInterval it = itr.next();
      int allocatedReg = isPhysicalRegister(it.register) ? it.register :
          ilk.getPhyReg(it);
      if (allocatedReg != reg)
        continue;
      if (it.intersect(cur)) {
        itr.remove();
        splitAndSpill(it, true);
      }
    }
    for (Iterator<LiveInterval> itr = inactive.iterator(); itr.hasNext(); ) {
      LiveInterval it = itr.next();
      int allocatedReg = isPhysicalRegister(it.register) ? it.register :
          ilk.getPhyReg(it);
      if (allocatedReg != reg)
        continue;
      if (it.intersect(cur)) {
        itr.remove();
        splitAndSpill(it, false);
      }
    }
  }

  private void splitAndSpill(LiveInterval it, boolean isActive) {
    if (isActive) {
      int minSplitPos = position + 1;
      int maxSplitPos = Math.min(it.getUsePointAfter(minSplitPos), it.endNumber());
      LiveInterval splitted = splitBeforeUsage(it, minSplitPos, maxSplitPos);
      unhandled.add(splitted);
      splitForSpilling(it);
    } else {
      Util.assertion(it.hasHoleBetween(position - 1, position + 1));
      unhandled.add(splitBeforeUsage(it, position + 1, position + 1));
    }
  }

  /**
   * <pre>
   * split an interval at the optimal position between minSplitPos and
   * maxSplitPos in two parts:
   * 1) the left part has already a location assigned
   * 2) the right part is always on the stack and therefore ignored in further processing
   * </pre>
   *
   * @param it
   */
  private void splitForSpilling(LiveInterval it) {
    int maxSplitPos = position;
    int minSplitPos = Math.max(it.getUsePointBefore(maxSplitPos) + 1, it.beginNumber());

    // the whole interval is never used, so spill it entirely to memory
    if (minSplitPos == it.beginNumber()) {
      Util.assertion(it.getFirstUse() > position);
      ilk.assignInterval2StackSlot(it);

      LiveInterval parent = it;
      while (parent != null && parent.isSplitChildren()) {
        parent = parent.getSplitChildBeforeOpId(parent.beginNumber());
        if (ilk.isAssignedPhyReg(parent)) {
          if (parent.getFirstUse() == Integer.MAX_VALUE) {
            // parent is never used, so kick it out of its assigned register
            ilk.assignInterval2StackSlot(parent);
          } else {
            // exit!
            parent = null;
          }
        }
      }
    } else {
      int optimalSplitPos = findOptimalSplitPos(it, minSplitPos, maxSplitPos);
      LiveInterval splittedChild = it.split(optimalSplitPos, this);
      ilk.assignInterval2StackSlot(splittedChild);
      insertMove(optimalSplitPos, it, splittedChild);
    }
  }

  /**
   * Insert a move instruction at the specified position from source interval to
   * destination interval.
   *
   * @param insertedPos
   * @param srcIt
   * @param destIt
   */
  private void insertMove(int insertedPos, LiveInterval srcIt, LiveInterval destIt) {
    // output all moves here. When source and target are equal, the move is
    // optimized away later in assignRegNums
    insertedPos = (insertedPos + 1) & ~1;
    MachineBasicBlock mbb = li.getBlockAtId(insertedPos);
    Util.assertion(mbb != null);

    int index = li.getIndexAtMBB(insertedPos) - li.getIndexAtMBB(li.mi2Idx.get(mbb.getFirstInst()));
    Util.assertion(index >= 0 && index < mbb.size());
    resolver.insertMoveInstr(mbb, index - 1);
    resolver.addMapping(srcIt, destIt);
  }

  private int getFreePhysReg(LiveInterval cur) {
    TargetRegisterClass rc = mri.getRegClass(cur.register);
    int[] allocatableRegs = rc.getAllocableRegs(mf);
    int[] freeUntilPos = new int[tri.getNumRegs()];
    // set the free position of all free physical register as
    // integral max value.
    for (int reg : allocatableRegs) {
      freeUntilPos[reg] = Integer.MAX_VALUE;
    }
    // set the free position of that register occupied by active
    // as 0.
    for (LiveInterval it : active) {
      int reg = isPhysicalRegister(it.register) ?
          it.register : ilk.getPhyReg(it);
      freeUntilPos[reg] = 0;
    }
    for (LiveInterval it : inactive) {
      if (it.intersect(cur)) {
        int reg = isPhysicalRegister(it.register) ?
            it.register : ilk.getPhyReg(it);
        freeUntilPos[reg] = it.intersectAt(cur);
      }
    }

    int reg = -1;
    int max = -1;
    for (int i = 0; i < freeUntilPos.length; i++) {
      if (freeUntilPos[i] > max) {
        max = freeUntilPos[i];
        reg = i;
      }
    }
    Util.assertion(reg != -1, "No physical register found!");

    if (freeUntilPos[reg] == 0)
      // allocation failed.
      return 0;
    else if (freeUntilPos[reg] > cur.getLast().end) {
      // assign this reg to current interval.
      return reg;
    } else {
      // register available for first part of current interval.
      // split current at optimal position before freePos[reg].
      unhandled.add(splitIntervalWhenPartialAvailable(cur, freeUntilPos[reg]));
      return reg;
    }
  }

  /**
   * Split the it at an optimal position before regAvaliableUntil.
   *
   * @param it
   * @param regAvaliableUntil
   */
  private LiveInterval splitIntervalWhenPartialAvailable(LiveInterval it, int regAvaliableUntil) {
    int minSplitPos = Math.max(it.getUsePointBefore(regAvaliableUntil), it.beginNumber());
    return splitBeforeUsage(it, minSplitPos, regAvaliableUntil);
  }

  /**
   * <pre>
   * Find an optimal position, between minSplitPos and maxSplitPos, can be used
   * for splitting the specified LiveInterval.
   *
   * Then left part of it will be assigned a physical register and
   * right part will be inserted into unhandled list for following alloacted.
   * </pre>
   *
   * @param it
   * @param minSplitPos
   * @param maxSplitPos
   */
  private LiveInterval splitBeforeUsage(LiveInterval it, int minSplitPos, int maxSplitPos) {
    Util.assertion(minSplitPos < maxSplitPos);
    Util.assertion(minSplitPos > cur.beginNumber());
    Util.assertion(maxSplitPos < cur.endNumber());
    Util.assertion(minSplitPos > cur.beginNumber());

    int optimalSplitPos = findOptimalSplitPos(it, minSplitPos, maxSplitPos);
    Util.assertion(optimalSplitPos >= minSplitPos &&
        optimalSplitPos <= maxSplitPos);
    if (optimalSplitPos == cur.endNumber()) {
      // If the optimal split position is at the end of current interval,
      // so splitting is not at all necessary.
      return null;
    }
    LiveInterval rightPart = it.split(optimalSplitPos, this);
    rightPart.setInsertedMove();
    return rightPart;
  }

  private int findOptimalSplitPos(
      MachineBasicBlock minBlock,
      MachineBasicBlock maxBlock,
      int maxSplitPos) {
    // Try to split at end of maxBlock. If this would be after
    // maxSplitPos, then use the begin of maxBlock
    int optimalSplitPos = li.mi2Idx.get(maxBlock.getLastInst()) + 2;
    if (optimalSplitPos > maxSplitPos) {
      optimalSplitPos = li.mi2Idx.get(maxBlock.getFirstInst());
    }

    int fromBlockId = minBlock.getNumber();
    int toBlockId = maxBlock.getNumber();
    int minLoopDepth = ml.getLoopDepth(maxBlock);

    for (int i = toBlockId - 1; i >= fromBlockId; i--) {
      MachineBasicBlock curBB = mf.getMBBAt(i);
      int depth = ml.getLoopDepth(curBB);
      if (depth < minLoopDepth) {
        minLoopDepth = depth;
        optimalSplitPos = li.mi2Idx.get(curBB.getLastInst()) + 2;
      }
    }
    return optimalSplitPos;
  }

  private int findOptimalSplitPos(LiveInterval it, int minSplitPos, int maxSplitPos) {
    if (minSplitPos == maxSplitPos)
      return minSplitPos;
    MachineBasicBlock minBlock = li.getBlockAtId(minSplitPos - 1);
    MachineBasicBlock maxBlock = li.getBlockAtId(maxSplitPos - 1);

    if (minBlock.equals(maxBlock)) {
      return maxSplitPos;
    }
    if (it.hasHoleBetween(maxSplitPos - 1, maxSplitPos) &&
        !li.isBlockBegin(maxSplitPos)) {
      // Do not move split position if the interval has a hole before
      // maxSplitPos. Intervals resulting from Phi-Functions have
      // more than one definition with a hole before each definition.
      // When the register is needed for the second definition, an
      // earlier reloading is unnecessary.
      return maxSplitPos;
    } else {
      return findOptimalSplitPos(minBlock, maxBlock, maxSplitPos);
    }
  }

  private void initIntervalSet() {
    for (LiveInterval it : li.intervals.values()) {
      if (isPhysicalRegister(it.register)) {
        active.add(it);
        mri.setPhysRegUsed(it.register);
      } else
        unhandled.add(it);
    }
  }

  private void resolveDataflow() {
    // find the position where a move instruction needs to be inserted.
    TreeSet<MachineBasicBlock> uniqueMBBs = new TreeSet<>(Comparator.comparingInt(Object::hashCode));
    BitMap[] liveIns = li.liveIns;
    Util.assertion(liveIns.length == mf.getNumBlocks());

    for (MachineBasicBlock cur : mf.getBasicBlocks()) {
      if (!cur.succIsEmpty()) {
        for (Iterator<MachineBasicBlock> itr = cur.succIterator(); itr.hasNext(); ) {
          MachineBasicBlock succ = itr.next();
          // avoiding handle a block multiple times.
          if (!uniqueMBBs.add(succ))
            continue;
          BitMap liveIn = liveIns[succ.getNumber()];
          if (liveIn.isEmpty())
            continue;

          for (int reg = liveIn.findFirst(); reg != -1; reg = liveIn.findNext(reg)) {
            LiveInterval parent = li.intervals.get(reg);
            Util.assertion(parent != null);
            parent = parent.getSplitParent();

            LiveInterval srcIt = parent.getSplitChildAtOpId(li.mi2Idx.get(cur.getLastInst()));
            LiveInterval dstIt = parent.getSplitChildAtOpId(li.mi2Idx.get(succ.getFirstInst()));
            if (!srcIt.equals(dstIt)) {
              resolver.addMapping(srcIt, dstIt);
            }
          }

          // find a position to insert a move instruction.
          findPosAndInsertMove(cur, succ);
          // resolve map
          resolver.resolveMappings();
        }
        uniqueMBBs.clear();
      }
    }
  }

  private void findPosAndInsertMove(MachineBasicBlock src, MachineBasicBlock dst) {
    if (src.getNumSuccessors() <= 1) {
      // insert a move instruction at the end of source basic block.
      resolver.insertMoveInstr(src, src.getLastInst().index());
    } else {
      // insert a move instruction at the begining of destination block.
      resolver.insertMoveInstr(dst, dst.getFirstInst().index());
    }
  }

  @Override
  public String getPassName() {
    return "Wimmer-Style Linear scan register allocator";
  }
}
