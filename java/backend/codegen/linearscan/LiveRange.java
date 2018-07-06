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

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LiveRange implements Comparable<LiveRange>
{
    public static final LiveRange EndMarker =
            new LiveRange(Integer.MAX_VALUE, Integer.MAX_VALUE, null);

    int start;
    int end;
    LiveRange next;

    public LiveRange(int defIdx, int killIdx, LiveRange next)
    {
        start = defIdx;
        end = killIdx;
        this.next = next;
    }

    @Override
    public int compareTo(LiveRange o)
    {
        if (start == o.start && end == o.end)
            return 0;
        if (start < o.start || (start == o.start && end < o.end))
            return -1;
        return 1;
    }

    public boolean contains(int idx)
    {
        return start <= idx && idx <= end;
    }

    public void print(PrintStream os)
    {
        os.printf("[%d,%d)", start, end);
    }

    public void dump()
    {
        print(System.err);
    }
}
