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

import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class MachineJumpTableInfo {
  private int entrySize;
  private int alignment;
  private ArrayList<MachineJumpTableEntry> jumpTables;

  public MachineJumpTableInfo(int entrySize, int alignment) {
    this.entrySize = entrySize;
    this.alignment = alignment;
    jumpTables = new ArrayList<>();
  }

  public int getJumpTableIndex(ArrayList<MachineBasicBlock> destMBBs) {
    Util.assertion(!destMBBs.isEmpty());
    for (int i = 0, e = jumpTables.size(); i < e; i++)
      if (jumpTables.get(i).mbbs.equals(destMBBs))
        return i;

    jumpTables.add(new MachineJumpTableEntry(destMBBs));
    return jumpTables.size() - 1;
  }

  public boolean isEmpty() {
    return jumpTables.isEmpty();
  }

  public ArrayList<MachineJumpTableEntry> getJumpTables() {
    return jumpTables;
  }

  public void removeJumpTable(int idx) {
    Util.assertion(idx >= 0 && idx < jumpTables.size());
    jumpTables.get(idx).mbbs.clear();
  }

  public int getEntrySize() {
    return entrySize;
  }

  public int getAlignment() {
    return alignment;
  }

  public boolean replaceMBBInJumpTables(MachineBasicBlock oldOne, MachineBasicBlock newOne) {
    boolean madeChange = false;
    for (MachineJumpTableEntry entry : jumpTables) {
      for (int i = 0, e = entry.mbbs.size(); i < e; i++) {
        if (entry.mbbs.get(i).equals(oldOne)) {
          entry.mbbs.set(i, newOne);
          madeChange = true;
        }
      }
    }
    return madeChange;
  }

  public void print(PrintStream os) {
    int i = 0;
    for (MachineJumpTableEntry jt : jumpTables) {
      os.printf("  <jt#%d> has %d entries%n", i, jt.mbbs.size());
      ++i;
    }
  }

  public void dump() {
    print(System.err);
  }
}
