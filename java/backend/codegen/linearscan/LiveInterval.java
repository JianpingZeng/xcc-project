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
    LiveInterval splitChilden;
    /**
     * Indicates if a move instruction should be inserted at the splitting position.
     */
    boolean insertedMove;

    public LiveInterval()
    {
        first = LiveRange.EndMarker;
        last = LiveRange.EndMarker;
        usePoints = new TreeSet<>(Comparator.comparingInt(o -> o.id));
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
        // TODO: 18-7-6
        return false;
    }

    public boolean interset(LiveInterval cur)
    {
        // TODO: 18-7-6
        return false;
    }

    public int intersetAt(LiveInterval cur)
    {
        // TODO: 18-7-6
        return 0;
    }

    public int beginNumber()
    {
        return first.start;
    }

    public int endNumber()
    {
        return last.end;
    }

    public int getUsePointBefore(int pos)
    {
        UsePoint up = usePoints.floor(new UsePoint(pos, null));
        Util.assertion(up != null);
        return up.id;
    }

    public LiveInterval split(int pos,
            WimmerLinearScanRegAllocator regAlloc)
    {
        return null;
    }

    public boolean hasHoleBetween(int from, int to)
    {
        return false;
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
