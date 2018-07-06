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
import java.util.ArrayList;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LiveInterval
{
    int regNum;
    LiveRange first;
    ArrayList<UsePoint> usePoints;
    LiveInterval splitParent;
    LiveInterval splitChilden;

    public LiveInterval()
    {
        first = LiveRange.EndMarker;
        usePoints = new ArrayList<>();
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
            first = new LiveRange(from, to, first);
        }
    }

    public LiveRange getFirstRange()
    {
        return first;
    }

    public void addUsePoint(int numMI, MachineOperand mo)
    {
        usePoints.add(new UsePoint(numMI, mo));
    }

    public void print(PrintStream os, TargetRegisterInfo tri)
    {
        System.err.println("******** Live Intervals ********");
        System.err.printf("%s: ", isPhysicalRegister(regNum) ?
                        tri.getName(regNum) : "%reg" + regNum);
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
}
