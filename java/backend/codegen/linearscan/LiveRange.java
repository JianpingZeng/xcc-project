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

import tools.Util;

import java.io.PrintStream;
import java.util.Objects;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class LiveRange implements Comparable<LiveRange>, Cloneable {
  public static final LiveRange EndMarker =
      new LiveRange(Integer.MAX_VALUE, Integer.MAX_VALUE, null);

  int start;
  int end;
  LiveRange next;

  public LiveRange(int defIdx, int killIdx, LiveRange next) {
    start = defIdx;
    end = killIdx;
    this.next = next;
  }

  @Override
  public int compareTo(LiveRange o) {
    if (start == o.start && end == o.end)
      return 0;
    if (start < o.start || (start == o.start && end < o.end))
      return -1;
    return 1;
  }

  public boolean contains(int idx) {
    return start <= idx && idx < end;
  }

  public void print(PrintStream os) {
    os.print(toString());
  }

  public void dump() {
    print(System.err);
  }

  @Override
  public String toString() {
    return String.format("[%d, %d]", start, end);
  }

  /**
   * Determines the first id position where this intersects with r2.
   * If no intersection, return -1.
   *
   * @param r2
   * @return
   */
  public int intersectsAt(LiveRange r2) {
    LiveRange r1 = this;
    Util.assertion(r2 != null && !r2.equals(EndMarker));
    Util.assertion(!r1.equals(EndMarker));

    while (true) {
      if (r1.start < r2.start) {
        if (r1.end <= r2.start) {
          r1 = r1.next;
          if (r1.equals(EndMarker))
            return -1;
        } else {
          return r2.start;
        }
      } else {
        if (r1.start == r2.start)
          return r1.start;
        // Otherwise, r1 > r2.start  <--> r2.start < r1.start
        if (r2.end <= r1.start) {
          r2 = r2.next;
          if (r2.equals(EndMarker))
            return -1;
        } else
          return r1.start;
      }
    }
  }

  @Override
  public LiveRange clone() {
    return new LiveRange(start, end, next != null ? next.clone() : null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (getClass() != obj.getClass()) return false;
    LiveRange lr = (LiveRange) obj;
    return start == lr.start && end == lr.end && Objects.equals(next, lr.next);
  }
}
