/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

import backend.codegen.MachineOperand;
import backend.target.TargetRegisterInfo;
import tools.Util;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LiveInterval
{
    int register;
    LiveRange first;
    LiveRange last;
    TreeSet<UsePoint> usePoints;
    LiveInterval splitParent;
    TreeSet<LiveInterval> splitChildren;

    /**
     * A comparator used for sorting the use points list.
     */
    private final static Comparator<UsePoint> UsePointComparator =
            Comparator.comparingInt(o -> o.id);

    /**
     * Indicates if a move instruction should be inserted at the splitting position.
     */
    boolean insertedMove;

    public LiveInterval()
    {
        register = 0;
        first = LiveRange.EndMarker;
        last = first;
        usePoints = new TreeSet<>(UsePointComparator);
        splitChildren = new TreeSet<>();
    }

    public void addRange(int from, int to)
    {
        Util.assertion(from < to, "Invalid range!");
        Util.assertion(first == LiveRange.EndMarker || to < first.next.start,
                "Not inserting at begining of interval");
        Util.assertion(from <= first.end, "Not inserting at begining of interval");
        if (first.start <= to)
        {
            // Join intersecting LiveRanges.
            Util.assertion(first != LiveRange.EndMarker);
            first.start = Math.min(from, first.start);
            first.end = Math.max(to, first.end);
        }
        else
        {
            // create a new LiveRange.
            last = first;
            first = new LiveRange(from, to, first);
        }
    }

    public LiveRange getFirst()
    {
        return first;
    }

    public LiveRange getLast()
    {
        return last;
    }

    public void addUsePoint(int numMI, MachineOperand mo)
    {
        usePoints.add(new UsePoint(numMI, mo));
    }

    public void print(PrintStream os, TargetRegisterInfo tri)
    {
        System.err.println("******** Live Intervals ********");
        System.err.printf("%s: ", isPhysicalRegister(register) ?
                        tri.getName(register) : "%reg" + register);
        LiveRange r = first;
        while (r != LiveRange.EndMarker)
        {
            r.dump();
            if (r.next != null)
                System.err.print(",");
            r = r.next;
        }
        System.err.println();
    }
    public void dump(TargetRegisterInfo tri)
    {
        print(System.err, tri);
    }

    public boolean isExpiredAt(int pos)
    {
        return getLast().end < pos;
    }

    public boolean isLiveAt(int pos)
    {
        if (pos < first.start || pos > last.end)
            return false;

        LiveRange itr = first;
        while (itr != LiveRange.EndMarker)
        {
            if (itr.contains(pos))
                return true;
            itr = itr.next;
        }
        return false;
    }

    public boolean intersect(LiveInterval cur)
    {
        Util.assertion(cur != null);
        if (cur.beginNumber() > endNumber())
            return false;
        if (cur.endNumber() < beginNumber())
            return false;

        return intersectAt(cur) != -1;
    }

    public int intersectAt(LiveInterval cur)
    {
        return first.intersectsAt(cur.first);
    }

    public int beginNumber()
    {
        return first.start;
    }

    public int endNumber()
    {
        return last.end;
    }

    public boolean isSplitParent()
    {
        return splitParent == this;
    }

    public boolean isSplitChildren()
    {
        return !isSplitParent();
    }

    public LiveInterval getSplitParent()
    {
        Util.assertion(isSplitParent());
        return splitParent;
    }

    public TreeSet<LiveInterval> getSplitChildren()
    {
        return splitChildren;
    }

    public int getUsePointBefore(int pos)
    {
        UsePoint up = usePoints.floor(new UsePoint(pos, null));
        Util.assertion(up != null);
        return up.id;
    }

    private LiveInterval newSplitChild(WimmerLinearScanRegAllocator allocator)
    {
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
     * @param splitPos
     * @param regAlloc
     * @return
     */
    public LiveInterval split(int splitPos,
            WimmerLinearScanRegAllocator regAlloc)
    {
        LiveInterval child = newSplitChild(regAlloc);

        // find the live range where splitPos resides
        LiveRange cur = first, prev = null;
        while (cur != LiveRange.EndMarker && cur.end <= splitPos)
        {
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
        if (cur.start < splitPos)
        {
            child.first = new LiveRange(splitPos, cur.end, cur.next);
            cur.end = splitPos;
            cur.next = LiveRange.EndMarker;
        }
        else
        {
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

        while (itr.hasNext())
        {
            UsePoint up = itr.next();
            if (up.id >= splitPos)
            {
                childUsePoints.add(up);
                itr.remove();
            }
        }
        child.usePoints = childUsePoints;
        return child;
    }

    public boolean hasHoleBetween(int from, int to)
    {
        Util.assertion(from < to);
        if (to <= beginNumber()) return false;
        if (from >= endNumber()) return false;
        return first.intersectsAt(new LiveRange(from, to, null)) == -1;
    }

    public void setInsertedMove()
    {
        insertedMove = true;
    }

    public boolean isInsertedMove()
    {
        return insertedMove;
    }
}
