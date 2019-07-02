/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.codegen.MachineFunction;
import backend.codegen.MachineOperand;
import backend.codegen.MachineRegisterInfo;
import tools.Util;

import java.util.ArrayList;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class SimpleVirtualRegSpiller {
  private IntervalLocKeeper ilk;
  private MachineRegisterInfo mri;

  public SimpleVirtualRegSpiller(IntervalLocKeeper ilk, MachineRegisterInfo mri) {
    this.ilk = ilk;
    this.mri = mri;
  }

  public boolean runOnMachineFunction(MachineFunction mf, ArrayList<LiveInterval> handled) {
    if (handled == null || handled.isEmpty())
      return false;

    for (LiveInterval it : handled) {
      if (isPhysicalRegister(it.register))
        continue;
      Util.assertion(ilk.isAssignedPhyReg(it), "No free register?");
      int reg = ilk.getPhyReg(it);
      for (UsePoint up : it.getUsePoints()) {
        MachineOperand mo = up.mo;
        mo.setReg(reg);
        mri.setPhysRegUsed(reg);
      }
    }
    return true;
  }
}
