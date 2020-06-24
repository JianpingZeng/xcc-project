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

import backend.codegen.MachineFrameInfo;
import backend.codegen.MachineFunction;
import backend.target.TargetRegisterInfo;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class IntervalLocKeeper {
  private TObjectIntHashMap<LiveInterval> interval2StackSlot;
  private TObjectIntHashMap<LiveInterval> interval2PhyReg;
  private MachineFrameInfo mfi;
  private TargetRegisterInfo tri;

  public IntervalLocKeeper(MachineFunction mf) {
    mfi = mf.getFrameInfo();
    tri = mf.getSubtarget().getRegisterInfo();
    interval2PhyReg = new TObjectIntHashMap<>();
    interval2StackSlot = new TObjectIntHashMap<>();
  }

  public void assignInterval2Phys(LiveInterval it, int phyReg) {
    Util.assertion(it != null);
    Util.assertion(!interval2StackSlot.containsKey(it),
        "Can't assign an interval resides in stack to register");
    Util.assertion(!interval2PhyReg.containsKey(it), "Can't assign interval twice");
    Util.assertion(TargetRegisterInfo.isPhysicalRegister(phyReg));
    interval2PhyReg.put(it, phyReg);
  }

  public int assignInterval2StackSlot(LiveInterval it) {
    Util.assertion(it != null);
    int reg = it.register;
    Util.assertion(TargetRegisterInfo.isVirtualRegister(reg));
    Util.assertion(!interval2PhyReg.containsKey(it));
    Util.assertion(!interval2StackSlot.containsKey(it));
    int fi = mfi.createStackObject(tri.getRegClass(reg));
    interval2StackSlot.put(it, fi);
    return fi;
  }

  public boolean isAssignedPhyReg(LiveInterval it) {
    return interval2PhyReg.containsKey(it);
  }

  public boolean isAssignedStackSlot(LiveInterval it) {
    return interval2StackSlot.containsKey(it);
  }

  public int getPhyReg(LiveInterval it) {
    Util.assertion(isAssignedPhyReg(it));
    return interval2PhyReg.get(it);
  }

  public int getStackSlot(LiveInterval it) {
    Util.assertion(isAssignedStackSlot(it));
    return interval2StackSlot.get(it);
  }
}
