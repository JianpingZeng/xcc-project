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

package backend.codegen;

import backend.target.TargetData;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MachineJumpTableInfo {
  public enum JTEntryKind {
    /**
     * ach entry is a plain address of block, e.g.:
     *     .word LBB123
     */
    EK_BlockAddress,

    /**
     * Each entry is an address of block, encoded with a relocation as gp-relative, e.g.:
     *     .gprel32 LBB123
     */
    EK_GPRel32BlockAddress,

    /**
     * Each entry is the address of the block minus the address of the jump table.
     * This is used for PIC jump tables where gprel32 is not supported.  e.g.:
     *     .word LBB123 - LJTI1_2
     * If the .set directive is supported, this is emitted as:
     *     .set L4_5_set_123, LBB123 - LJTI1_2
     *     .word L4_5_set_123
     */
    EK_LabelDifference32,

    /**
     * Each entry is a 32-bit value that is custom lowered by the
     * TargetLowering::LowerCustomJumpTableEntry hook.
     */
    EK_Custom32
  }

  private ArrayList<MachineJumpTableEntry> jumpTables;
  private JTEntryKind entryKind;

  public MachineJumpTableInfo(JTEntryKind kind) {
    entryKind = kind;
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

  public int getEntrySize(TargetData td) {
    switch (getEntryKind()) {
      case EK_BlockAddress:
        return td.getPointerSize();
      case EK_GPRel32BlockAddress:
      case EK_LabelDifference32:
      case EK_Custom32:
        return 4;
      default:
        Util.shouldNotReachHere("unknown jump table encoding!");
        return ~0;
    }
  }

  public JTEntryKind getEntryKind() {
    return entryKind;
  }

  public int getAlignment(TargetData td) {
    switch (getEntryKind()) {
      case EK_BlockAddress:
        return td.getPointerABIAlign();
      case EK_GPRel32BlockAddress:
      case EK_LabelDifference32:
      case EK_Custom32:
        return td.getABITypeAlignment(32);
      default:
        Util.shouldNotReachHere("unknown jump table encoding!");
        return ~0;
    }
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
    if (jumpTables.isEmpty()) return;
    int i = 0;
    os.println("Jump Tables:");
    for (MachineJumpTableEntry jt : jumpTables) {
      os.printf("  jt#%d: ", i++);
      for (int j = 0, e = jt.mbbs.size(); j < e; ++j)
        os.printf(" BB#%d", jt.mbbs.get(j).getNumber());
      os.println();
    }
  }

  public void dump() {
    print(System.err);
  }
}
