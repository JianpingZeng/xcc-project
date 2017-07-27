package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.target.TargetRegisterInfo;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LiveInterval implements Comparable<LiveInterval>
{
    int register;
    float weight;
    ArrayList<LiveRange> ranges;
    private int numValues;

    public LiveInterval(int reg, float weight)
    {
        this.register = reg;
        this.weight = weight;
        ranges = new ArrayList<>();
    }

    public boolean isEmpty()
    {
        return ranges.isEmpty();
    }

    public void addRange(LiveRange range)
    {
        // todo insert in the order.
        ranges.add(range);
    }

    public void removeRange(int begin, int end)
    {
        // TODO: 17-7-27
    }

    public int getNextValue()
    {
        return numValues++;
    }

    public boolean containsOneValue()
    {
        return numValues == 1;
    }

    public ArrayList<LiveRange> getRanges()
    {
        return ranges;
    }

    public LiveRange getRange(int idx)
    {
        assert idx >= 0 && idx < ranges.size();
        return ranges.get(idx);
    }

    public int endNumber()
    {
        assert !isEmpty();
        return ranges.get(ranges.size() - 1).end;
    }

    public int beginNumber()
    {
        assert !isEmpty();
        return ranges.get(0).start;
    }

    /**
     * Advance the specified iterator to point to the LiveRange
     * containing the specified position, or end() if the position is past the
     * end of the interval.  If no LiveRange contains this position, but the
     * position is in a hole, this method returns an iterator pointing the the
     * LiveRange immediately after the hole.
     * @param idxToRange
     * @param pos
     * @return
     */
    public int advanceTo(int idxToRange, int pos)
    {
        if (pos >= endNumber())
            return ranges.size();
        while (ranges.get(idxToRange).end <= pos)
            idxToRange++;
        return idxToRange;
    }

    public boolean expireAt(int index)
    {
        return index >= endNumber();
    }

    /**
     * An example for liveAt():
     *
     * this = [1,4), liveAt(0) will return false. The instruction defining this
     * spans slots [0,3]. The interval belongs to an spilled definition of the
     * variable it represents. This is because slot 1 is used (def slot) and spans
     * up to slot 3 (store slot).
     * @param index
     * @return
     */
    public boolean isLiveAt(int index)
    {
        int found = upperBound(ranges, index);

        if (found == 0)
            return false;

        --found;
        return ranges.get(found).contains(index);
    }

    private static int upperBound(ArrayList<LiveRange> ranges, int key)
    {
        int idx = 0;
        for (LiveRange r : ranges)
        {
            if (!(r.start < key))
                return idx;
        }
        return -1;
    }

    public LiveRange getLiveRangeContaining(int idx)
    {
        int found = upperBound(ranges, idx);
        if (found > 0)
        {
            LiveRange lr = ranges.get(found - 1);
            if (lr.contains(idx))
                return lr;
        }
        return null;
    }

    public boolean joinable(LiveInterval other, int copyIdx)
    {}

    public boolean getOverlappingRanges(LiveInterval other,
            int cppyIdx,
            ArrayList<LiveRange> ranges)
    {

    }

    public boolean overlaps(LiveInterval other)
    {

    }

    public boolean overlapsFrom(LiveInterval other, int idxToRange)
    {}

    private void addRangeFrom(LiveRange lr, int idxToRange)
    {
        // TODO: 17-7-27
    }

    public void join(LiveInterval other, int copyIdx)
    {
        // TODO: 17-7-27
    }

    private void extendIntervalEndTo(int idxToRange, int newEnd)
    {
        // TODO: 17-7-27
    }

    private void extendIntervalStartTo(int idxToRange, int newEnd)
    {
        // TODO: 17-7-27
    }

    public void print(PrintStream os, TargetRegisterInfo tri)
    {
        if (tri != null && TargetRegisterInfo.isPhysicalRegister(register))
        {
            os.print(tri.getName(register));
        }
        else
            os.printf("%%reg%d", register);
        os.printf(",%f", weight);

        if (isEmpty())
        {
            os.print("EMPTY");
        }
        else
        {
            os.print(" = ");
            ranges.forEach(r->r.print(os));
        }
    }

    public void dump()
    {
        print(System.err, null);
    }

    @Override
    public int compareTo(LiveInterval o)
    {
        if (beginNumber() < o.beginNumber()
                || (beginNumber() == o.beginNumber() && endNumber() < o.endNumber()))
            return -1;

        if (beginNumber() == o.beginNumber() && endNumber() == o.endNumber())
            return 0;

        return 1;
    }

    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj);
    }
}
