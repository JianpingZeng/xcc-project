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
import gnu.trove.map.hash.TIntIntHashMap;

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

    /**
     * Add the specified LiveRange to this interval, merging
     * intervals as appropriate.
     * @param range
     */
    public void addRange(LiveRange range)
    {
        addRangeFrom(range, 0);
    }

    public void removeRange(int begin, int end)
    {
        int idx = upperBound(ranges, 0, begin);
        assert idx != -1 :"Range is not in interval";
        --idx;

        assert ranges.get(idx).contains(begin)
                && ranges.get(idx).contains(end-1)
                : "Range is not entirely in interval!";

        if (ranges.get(idx).start == begin)
        {
            if (ranges.get(idx).end == end)
                ranges.remove(idx);
            else
                ranges.get(idx).start = end;
            return;
        }

        if (ranges.get(idx).end == end)
        {
            ranges.get(idx).end = begin;
            return;
        }

        int oldEnd = ranges.get(idx).end;
        ranges.get(idx).end = begin;

        ranges.add(idx+1, new LiveRange(end, oldEnd, ranges.get(idx).valId));
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

    public boolean expiredAt(int index)
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
        int found = upperBound(ranges,0, index);

        if (found == 0)
            return false;

        --found;
        return ranges.get(found).contains(index);
    }

    /**
     * Get the index to upper bound in the specified list.
     * @param ranges
     * @param start
     * @param end
     * @param key
     * @return
     */
    private static int upperBound(ArrayList<LiveRange> ranges, int start, int end, int key)
    {
        for (int i = start; i != end; i++)
        {
            LiveRange r = ranges.get(i);
            if (!(r.start < key))
                return i;
        }
        return end;
    }

    private static int upperBound(ArrayList<LiveRange> ranges, int start, int key)
    {
        return upperBound(ranges, start, ranges.size(), key);
    }

    public LiveRange getLiveRangeContaining(int idx)
    {
        int found = upperBound(ranges,0, idx);
        if (found > 0)
        {
            LiveRange lr = ranges.get(found - 1);
            if (lr.contains(idx))
                return lr;
        }
        return null;
    }

    public boolean joinable(LiveInterval other, int copyIdx)
    {
        LiveRange sourceLR = other.getLiveRangeContaining(copyIdx);
        LiveRange destLR = getLiveRangeContaining(copyIdx);
        assert sourceLR != null && destLR != null:"Not joining due to copy?";
        int otherValIdx = sourceLR.valId;
        int thisValIdx = destLR.valId;

        int i = 0, ie = ranges.size(), j = 0, je = other.ranges.size();

        if (ranges.get(i).start < other.ranges.get(j).start)
        {
            i = upperBound(ranges, i, ie, other.ranges.get(j).start);
            if (i != -1) --i;
        }
        else if (other.ranges.get(j).start < ranges.get(i).start)
        {
            j = upperBound(other.ranges, j, je, ranges.get(i).start);
            if (j != -1)
                --j;
        }

        while (i != ie && j != je)
        {
            if (ranges.get(i).start == other.ranges.get(j).start)
            {
                if (ranges.get(i).valId != thisValIdx
                        || other.ranges.get(j).valId != otherValIdx)
                {
                    return false;
                }
            }
            else if (ranges.get(i).start < other.ranges.get(j).start)
            {
                if (ranges.get(i).end > other.ranges.get(j).start)
                {
                    if (ranges.get(i).valId != thisValIdx
                            || other.ranges.get(j).valId != otherValIdx)
                    {
                        return false;
                    }
                }
            }
            else
            {
                if (other.ranges.get(j).end > ranges.get(i).start)
                {
                    if (ranges.get(i).valId != thisValIdx
                            || other.ranges.get(j).valId != otherValIdx)
                        return false;
                }
            }
            if (ranges.get(i).end < other.ranges.get(j).end)
                ++i;
            else
                ++j;
        }
        return true;
    }

    public void getOverlapingRanges(
            LiveInterval other,
            int copyIdx,
            ArrayList<LiveRange> ranges)
    {
        LiveRange sourceLR = other.getLiveRangeContaining(copyIdx);
        LiveRange destLR = getLiveRangeContaining(copyIdx);
        assert sourceLR != null && destLR != null:"Not joining due to copy?";
        int otherValIdx = sourceLR.valId;
        int thisValIdx = destLR.valId;

        int i = 0, ie = ranges.size(), j = 0, je = other.ranges.size();

        if (ranges.get(i).start < other.ranges.get(j).start)
        {
            i = upperBound(ranges, i, ie, other.ranges.get(j).start);
            if (i != -1) --i;
        }
        else if (other.ranges.get(j).start < ranges.get(i).start)
        {
            j = upperBound(other.ranges, j, je, ranges.get(i).start);
            if (j != -1)
                --j;
        }

        while (i != ie && j != je)
        {
            if (ranges.get(i).start == other.ranges.get(j).start)
            {
                if (ranges.get(i).valId != thisValIdx
                        || other.ranges.get(j).valId != otherValIdx)
                {
                    ranges.add(ranges.get(i));
                }
            }
            else if (ranges.get(i).start < other.ranges.get(j).start)
            {
                if (ranges.get(i).end > other.ranges.get(j).start)
                {
                    if (ranges.get(i).valId != thisValIdx
                            || other.ranges.get(j).valId != otherValIdx)
                    {
                        ranges.add(ranges.get(i));
                    }
                }
            }
            else
            {
                if (other.ranges.get(j).end > ranges.get(i).start)
                {
                    if (ranges.get(i).valId != thisValIdx
                            || other.ranges.get(j).valId != otherValIdx)
                        ranges.add(ranges.get(i));
                }
            }

            if (ranges.get(i).end < other.ranges.get(j).end)
                ++i;
            else
                ++j;
        }
    }

    /**
     * An example for overlaps():
     *
     * 0: A = ...
     * 4: B = ...
     * 8: C = A + B ;; last use of A
     *
     * The live intervals should look like:
     *
     * A = [3, 11)
     * B = [7, x)
     * C = [11, y)
     *
     * A->overlaps(C) should return false since we want to be able to join
     * A and C.
     * @param other
     * @return
     */
    public boolean overlaps(LiveInterval other)
    {
        int i = 0, ie = ranges.size();
        int j = 0, je = other.ranges.size();

        if (ranges.get(i).start < other.ranges.get(j).start)
        {
            i = upperBound(ranges, i, ie, other.ranges.get(j).start);
            if (i != -1) --i;
        }
        else if (other.ranges.get(j).start < ranges.get(i).start)
        {
            j = upperBound(other.ranges, j, je, ranges.get(i).start);
            if (j != -1) --j;
        }
        else
            return true;

        while (i != ie && j != je)
        {
            if (ranges.get(i).start == other.ranges.get(j).start)
                return true;

            if (ranges.get(i).start > other.ranges.get(j).start)
            {
                int temp = i;
                i = j;
                j = temp;

                temp = ie;
                ie = je;
                je = temp;
                ArrayList<LiveRange> t = ranges;
                ranges = other.ranges;
                other.ranges = t;
            }
            assert ranges.get(i).start < other.ranges.get(j).start;

            if (ranges.get(i).end > other.ranges.get(j).start)
                return true;
            ++i;
        }

        return false;
    }

    public boolean overlapsFrom(LiveInterval other, int startPos)
    {
        int i = 0, ie = ranges.size();
        int j = startPos, je = other.ranges.size();

        assert (other.ranges.get(startPos).start <= ranges.get(i).start
                || startPos == 0) && (startPos != other.ranges.size());

        if (ranges.get(i).start < other.ranges.get(j).start)
        {
            i = upperBound(ranges, i, ie, other.ranges.get(j).start);
            if (i != -1) --i;
        }
        else if (other.ranges.get(j).start < ranges.get(i).start)
        {
            ++startPos;
            if (startPos != other.ranges.size() &&
                    other.ranges.get(startPos).start <= ranges.get(i).start)
            {
                assert startPos < other.ranges.size() && i < ranges.size();
                j = upperBound(other.ranges, j, je, ranges.get(i).start);
                if (j != -1) --j;
            }
        }
        else
            return true;

        if (j == je)
            return false;

        while (i != ie)
        {
            if (ranges.get(i).start > other.ranges.get(j).start)
            {
                int temp = i;
                i = j;
                j = temp;

                temp = ie;
                ie = je;
                je = temp;
                ArrayList<LiveRange> t = ranges;
                ranges = other.ranges;
                other.ranges = t;
            }

            if (ranges.get(i).end > other.ranges.get(j).start)
                return true;
            ++i;
        }

        return false;
    }

    /**
     * Insert the specified LiveRange into ranges list in the order of ascending
     * live range.
     * @param lr
     * @param idxToRange
     * @return
     */
    private int addRangeFrom(LiveRange lr, int idxToRange)
    {
        int start = lr.start, end = lr.end;
        int idx = upperBound(ranges, idxToRange, start);

        if (idx != 0)
        {
            int prior = idx-1;
            if (lr.valId == ranges.get(prior).valId)
            {
                if (ranges.get(prior).start <= start && ranges.get(prior).end >= start)
                {
                    extendIntervalEndTo(prior, end);
                    return prior;
                }
            }
            else
            {
                assert ranges.get(prior).end <= start:
                        "Can not overlap two LiveRanges with differing valID";
            }
        }

        // Otherwise, if this range ends in the middle of, or right next to, another
        // interval, merge it into that interval.
        if (idx != ranges.size())
        {
            if (lr.valId == ranges.get(idx).valId)
            {
                if (ranges.get(idx).start <= end)
                {
                    idx = extendIntervalStartTo(idx, start);

                    if (end > ranges.get(idx).end)
                        extendIntervalEndTo(idx, end);
                    return idx;
                }
            }
            else
            {
                assert ranges.get(idx).start >= end:
                        "Cannot overlap two LiveRanges with differing valID";
            }
        }
        // Otherwise, this is just a new range that doesn't interact with anything.
        // Insert it.
        ranges.add(idx, lr);
        return idx;
    }

    public void join(LiveInterval other, int copyIdx)
    {
        LiveRange sourceLR = other.getLiveRangeContaining(copyIdx);
        LiveRange destLR = getLiveRangeContaining(copyIdx);
        assert sourceLR != null && destLR != null:"Not joining due to copy?";
        int mergedSrcValIdx = sourceLR.valId;
        int mergedDstValIdx = destLR.valId;

        if (other.ranges.size() < ranges.size())
        {
            int t = mergedSrcValIdx;
            mergedSrcValIdx = mergedDstValIdx;
            mergedDstValIdx = t;

            t = numValues;
            numValues = other.numValues;
            other.numValues = t;

            ArrayList<LiveRange> list = ranges;
            ranges = other.ranges;
            other.ranges = list;
        }

        int insertPos = 0;
        TIntIntHashMap dst2SrcIdxMap = new TIntIntHashMap();
        for (LiveRange  r : other.ranges)
        {
            if (r.valId == mergedSrcValIdx)
                r.valId = mergedDstValIdx;
            else
            {
                int nv = 0;
                if (!dst2SrcIdxMap.containsKey(r.valId))
                    nv = getNextValue();
                dst2SrcIdxMap.put(r.valId, nv);
                r.valId = nv;
            }

            insertPos = addRangeFrom(r, insertPos);
        }
        weight += other.weight;
    }

    private void extendIntervalEndTo(int idxToRange, int newEnd)
    {
        assert idxToRange < ranges.size():"Not a valid interval!";

        int valId = ranges.get(idxToRange).valId;

        int mergeTo = idxToRange + 1;
        for (; mergeTo != ranges.size() && newEnd >= ranges.get(mergeTo).end; ++mergeTo)
        {
            assert ranges.get(mergeTo).valId == valId
                    : "Cannot merge with differing values!";
        }

        ranges.get(idxToRange).end = Math.max(newEnd, ranges.get(mergeTo-1).end);

        for (int i = idxToRange+1; i != mergeTo; i++)
            ranges.remove(i);
    }

    private int extendIntervalStartTo(int idxToRange, int newStart)
    {
        assert idxToRange != ranges.size():"Not a valid interval!";
        int valId = ranges.get(idxToRange).valId;

        int mergeTo = idxToRange;
        do
        {
            if (mergeTo == 0)
            {
                ranges.get(idxToRange).start = newStart;
                for (int i = mergeTo; i != idxToRange; ++i)
                    ranges.remove(i);
                return idxToRange;
            }
            assert ranges.get(mergeTo).valId == valId;
            --mergeTo;
        }while (newStart <= ranges.get(mergeTo).start);

        if (ranges.get(mergeTo).end >= newStart && ranges.get(mergeTo).valId == valId)
        {
            ranges.get(mergeTo).end = ranges.get(idxToRange).end;
        }
        else
        {
            // Otherwise, extend the interval right after.
            ++mergeTo;
            ranges.get(mergeTo).start = newStart;
            ranges.get(mergeTo).end = ranges.get(idxToRange).end;
        }

        for (int i = mergeTo +1; i != idxToRange + 1; i++)
            ranges.remove(i);

        return mergeTo;
    }

    public void print(PrintStream os, TargetRegisterInfo tri)
    {
        if (tri != null && TargetRegisterInfo.isPhysicalRegister(register))
        {
            os.print(tri.getName(register));
        }
        else
            os.printf("%%reg%d", register);
        os.printf(",%.2f", weight);

        if (isEmpty())
        {
            os.print("EMPTY");
        }
        else
        {
            os.print(" = ");
            ranges.forEach(r->r.print(os));
        }
        os.println();
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

    public void swap(LiveInterval other)
    {
        int t = register;
        register = other.register;
        other.register = t;

        float w = weight;
        weight = other.weight;
        other.weight = w;

        ArrayList<LiveRange> temp = new ArrayList<>();
        temp.addAll(ranges);
        ranges.clear();

        ranges.addAll(other.ranges);

        other.ranges.clear();
        other.ranges.addAll(temp);

        int num = numValues;
        numValues = other.numValues;
        other.numValues = num;
    }
}
