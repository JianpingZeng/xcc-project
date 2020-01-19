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

package backend.codegen.linearscan;

import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import backend.target.TargetRegisterInfo;
import tools.Util;

import java.io.PrintStream;
import java.util.*;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class LiveInterval {
  int register;
  LiveRange first;
  LiveRange last;
  TreeSet<UsePoint> usePoints;
  LiveInterval splitParent;
  ArrayList<LiveInterval> splitChildren;

  /**
   * Indicates if a move instruction should be inserted at the splitting position.
   */
  boolean insertedMove;

  /**
   * A comparator used for sorting the use points list.
   */
  private final static Comparator<UsePoint> UsePointComparator =
      new Comparator<UsePoint>() {
        @Override
        public int compare(UsePoint o1, UsePoint o2) {
          if (o1.id < o2.id) return -1;
          if (o1.id > o2.id) return 1;
          MachineOperand mo1 = o1.mo, mo2 = o2.mo;

          Util.assertion(mo1.getParent().equals(mo2.getParent()), "Use a different machine instr");
          if (mo1.equals(mo2)) return 0;

          MachineInstr mi = mo1.getParent();
          int idx1 = -1, idx2 = -1;
          for (int i = 0, e = mi.getNumOperands(); i < e; i++) {
            MachineOperand mo = mi.getOperand(i);
            int reg;
            if (!mo.isRegister() || (reg = mo.getReg()) == 0)
              continue;

            if (mo.equals(mo1)) {
              Util.assertion(idx1 == -1);
              idx1 = i;
            } else if (mo.equals(mo2)) {
              Util.assertion(idx2 == -1);
              idx2 = i;
            }
          }
          Util.assertion(idx1 != -1 && idx2 != -1);
          return idx1 - idx2;
        }
      };

  public LiveInterval() {
    register = 0;
    first = LiveRange.EndMarker.clone();
    last = first;
    usePoints = new TreeSet<>(UsePointComparator);
    splitChildren = new ArrayList<>();
  }

  public TreeSet<UsePoint> getUsePoints() {
    return usePoints;
  }

  public void addRange(int from, int to) {
    Util.assertion(from <= to, "Invalid range!");
    if (first.equals(LiveRange.EndMarker) || to < first.end) {
      first = insertRangeBefore(from, to, first);
    } else {
      LiveRange cur = first;
      while (!cur.equals(LiveRange.EndMarker)) {
        if (to >= cur.end)
          cur = cur.next;
        else
          break;
      }
      cur = insertRangeBefore(from, to, cur);
    }
  }

  /**
   * Insert a live range before specified position.
   *
   * @param from
   * @param to
   * @param cur
   */
  private LiveRange insertRangeBefore(int from, int to, LiveRange cur) {
    Util.assertion(cur.equals(LiveRange.EndMarker) || cur.end == Integer.MAX_VALUE
            || (cur.next != null && to < cur.next.start),
        "Not inserting at begining of interval");
    Util.assertion(from <= cur.end, "Not inserting at begining of interval");
    if (cur.start <= to) {
      // Join intersecting LiveRanges.
      Util.assertion(cur != LiveRange.EndMarker,
          "First range must not be EndMarker for " + "%reg" + register);
      cur.start = Math.min(from, cur.start);
      cur.end = Math.max(to, cur.end);
    } else {
      // create a new LiveRange.
      if (cur == last) {
        cur = last = new LiveRange(from, to, cur);
      } else {
        LiveRange r = new LiveRange(from, to, last.next);
        last.next = r;
        last = r;
      }
    }
    return cur;
  }

  public LiveRange getFirst() {
    if (first.equals(LiveRange.EndMarker)) {
      first = new LiveRange(Integer.MAX_VALUE, Integer.MAX_VALUE, first);
      last = first;
    }
    return first;
  }

  public LiveRange getLast() {
    return last;
  }

  public void addUsePoint(int numMI, MachineOperand mo) {
    usePoints.add(new UsePoint(numMI, mo));
  }

  public void print(PrintStream os, TargetRegisterInfo tri) {
    os.printf("%s: ", isPhysicalRegister(register) ?
        tri.getName(register) : "%reg" + register);
    LiveRange r = first;
    while (r != null && !Objects.equals(r, LiveRange.EndMarker)) {
      r.dump();
      System.err.print(",");
      r = r.next;
    }
    os.print(" Use points: [");
    int i = 0, size = usePoints.size();
    for (UsePoint up : usePoints) {
      System.err.printf("%d", up.id);
      if (i < size - 1)
        os.print(",");
      ++i;
    }
    System.err.println("]");
  }

  public void dump(TargetRegisterInfo tri) {
    print(System.err, tri);
  }

  public boolean isExpiredAt(int pos) {
    return getLast().end <= pos;
  }

  public boolean isLiveAt(int pos) {
    if (pos <= first.start || pos >= last.end)
      return false;

    LiveRange itr = first;
    while (itr != LiveRange.EndMarker) {
      if (itr.contains(pos))
        return true;
      itr = itr.next;
    }
    return false;
  }

  public boolean intersect(LiveInterval cur) {
    Util.assertion(cur != null);
    if (cur.beginNumber() > endNumber())
      return false;
    if (cur.endNumber() < beginNumber())
      return false;

    return intersectAt(cur) != -1;
  }

  public int intersectAt(LiveInterval cur) {
    return first.intersectsAt(cur.first);
  }

  public int beginNumber() {
    return first.start;
  }

  public int endNumber() {
    return last.end;
  }

  public boolean isSplitParent() {
    return splitParent == this;
  }

  public boolean isSplitChildren() {
    return !isSplitParent();
  }

  public LiveInterval getSplitParent() {
    if (isSplitParent())
      return this;
    else
      return splitParent.getSplitParent();
  }

  public void setSplitParent(LiveInterval parent) {
    splitParent = parent;
  }

  public ArrayList<LiveInterval> getSplitChildren() {
    return splitChildren;
  }

  public boolean hasSplitChildren() {
    return splitChildren != null && !splitChildren.isEmpty();
  }

  public LiveInterval getSplitChildBeforeOpId(int id) {
    assert id >= 0 : "invalid id";

    LiveInterval parent = getSplitParent();
    LiveInterval result = null;

    Util.assertion(parent.hasSplitChildren(), "no split children available");
    int len = parent.splitChildren.size();
    for (int i = len - 1; i >= 0; i--) {
      LiveInterval cur = parent.splitChildren.get(i);
      if (cur.endNumber() <= id && (result == null
          || result.endNumber() < cur.endNumber())) {
        result = cur;
      }
    }
    Util.assertion(result != null, "no split child found");
    return result;
  }

  public LiveInterval getSplitChildAtOpId(int id) {
    LiveInterval parent = getSplitParent();
    LiveInterval result = null;
    Util.assertion(parent.hasSplitChildren(), "no split children available");

    int len = parent.splitChildren.size();
    for (int i = len - 1; i >= 0; i--) {
      LiveInterval cur = parent.splitChildren.get(i);
      if (cur.isLiveAt(id))
        result = cur;
    }
    return result;
  }

  /**
   * Get the greatest use point before specified position.
   *
   * @param pos
   * @return
   */
  public int getUsePointBefore(int pos) {
    UsePoint up = usePoints.floor(new UsePoint(pos, null));
    Util.assertion(up != null);
    return up.id;
  }

  /**
   * Get the least use point after specified position.
   *
   * @param pos
   * @return
   */
  public int getUsePointAfter(int pos) {
    UsePoint up = usePoints.ceiling(new UsePoint(pos, null));
    return up != null ? up.id : Integer.MAX_VALUE;
  }

  public int getFirstUse() {
    return usePoints.isEmpty() ? -1 : usePoints.first().id;
  }

  private LiveInterval newSplitChild(WimmerLinearScanRegAllocator allocator) {
    LiveInterval res = new LiveInterval();
    res.register = register;
    res.splitParent = getSplitParent();
    getSplitParent().splitChildren.add(res);
    return res;
  }

  /**
   * Splits this interval at a specified position and returns the remainder as
   * a new <i>child</i> interval of this interval's {@linkplain #getSplitParent()
   * parent} interval.
   * <p>
   * When an interval is split, a bi-directional link is established between
   * the original <i>parent</i> interval and the <i>children</i> intervals
   * that are split off this interval.
   *
   * @param splitPos
   * @param regAlloc
   * @return
   */
  public LiveInterval split(int splitPos,
                            WimmerLinearScanRegAllocator regAlloc) {
    LiveInterval child = newSplitChild(regAlloc);

    // find the live range where splitPos resides
    LiveRange cur = first, prev = null;
    while (cur != LiveRange.EndMarker && cur.end <= splitPos) {
      prev = cur;
      cur = cur.next;
    }
    Util.assertion(cur != LiveRange.EndMarker, "SplitPos after endNumber()!");

    /*
     *     splitPos
     *       |
     * |----------------|
     * ^                ^
     * cur.from      cur.to
     */
    if (cur.start < splitPos) {
      child.first = new LiveRange(splitPos, cur.end, cur.next);
      cur.end = splitPos;
      cur.next = LiveRange.EndMarker;
    } else {
      /*
       * splitPos
       * |
       * |----------------|
       * ^                ^
       * cur.from      cur.to
       * where, splitPos <= cur.from
       */
      child.first = cur;
      Util.assertion(prev != null, "SplitPos before beginNumber()!");
      prev.next = LiveRange.EndMarker;
    }
    // split the use points.
    Iterator<UsePoint> itr = usePoints.iterator();
    TreeSet<UsePoint> childUsePoints = new TreeSet<>();

    while (itr.hasNext()) {
      UsePoint up = itr.next();
      if (up.id >= splitPos) {
        childUsePoints.add(up);
        itr.remove();
      }
    }
    child.usePoints = childUsePoints;
    return child;
  }

  public boolean hasHoleBetween(int from, int to) {
    Util.assertion(from < to);
    if (to <= beginNumber()) return false;
    if (from >= endNumber()) return false;
    return first.intersectsAt(new LiveRange(from, to, null)) == -1;
  }

  public void setInsertedMove() {
    insertedMove = true;
  }

  public boolean isInsertedMove() {
    return insertedMove;
  }

  /**
   * Two intervals are joinable if the either don't overlap at all
   * or if the destination of the copy is a single assignment value, and it
   * only overlaps with one value in the source interval.
   *
   * @param it
   * @param moveIdx
   * @return
   */
  public boolean joinable(LiveInterval it, int moveIdx) {
    LiveRange srcLR = it.getLiveIntervalContains(moveIdx - 1);
    LiveRange dstLR = getLiveIntervalContains(moveIdx);
    Util.assertion(srcLR != null && dstLR != null);
    return srcLR.intersectsAt(dstLR) == -1;
  }

  private LiveRange getLiveIntervalContains(int pos) {
    LiveRange cur = first;
    while (!cur.equals(LiveRange.EndMarker)) {
      if (cur.contains(pos))
        return cur;
      cur = cur.next;
    }
    return null;
  }

  /**
   * Move all live ranges from it into current live interval.
   *
   * @param it
   */
  public void join(LiveInterval it) {
    LiveRange cur = it.first;
    while (cur != null && !cur.equals(LiveRange.EndMarker)) {
      addRange(cur.start, cur.end);
      cur = cur.next;
    }
    usePoints.addAll(it.usePoints);
    if (it.splitChildren != null && !it.splitChildren.isEmpty()) {
      for (LiveInterval child : it.splitChildren) {
        splitChildren.add(child);
        child.setSplitParent(this);
      }
    }
  }

  public void swap(LiveInterval it) {
    int r = register;
    register = it.register;
    it.register = r;

    LiveRange lr = first;
    first = it.first;
    it.first = lr;

    lr = last;
    last = it.last;
    it.last = lr;

    TreeSet<UsePoint> ups = usePoints;
    usePoints = it.usePoints;
    it.usePoints = ups;

    LiveInterval parent = splitParent;
    splitParent = it.splitParent;
    it.splitParent = parent;

    ArrayList<LiveInterval> its = splitChildren;
    splitChildren = it.splitChildren;
    it.splitChildren = its;

    boolean move = insertedMove;
    insertedMove = it.insertedMove;
    it.insertedMove = move;
  }
}
