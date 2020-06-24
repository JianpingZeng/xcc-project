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

import backend.support.MachineFunctionPass;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import backend.target.TargetSubtarget;
import backend.transform.scalars.DCE;
import gnu.trove.list.array.TIntArrayList;
import tools.BitMap;
import tools.OutRef;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * This class implements a machine function level dead instruction elimination
 * pass similar to {@linkplain DCE}.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class DeadMachineInstructionElim extends MachineFunctionPass {
  private BitMap livePhysReg;
  private TargetInstrInfo tii;
  private MachineRegisterInfo mri;

  /**
   * Checks if the specified machine instruction is useless or not.
   * A dead mi is such that no other mi use it's generated result
   * and it don't have side effect.
   *
   * @param mi
   * @return
   */
  private boolean isDead(MachineInstr mi) {
    OutRef<Boolean> sawStore = new OutRef<>(false);
    if (!mi.isSafeToMove(tii, sawStore) && !mi.isPHI())
      return false;

    // check each operand.
    for (int i = 0, e = mi.getNumOperands(); i < e; i++) {
      MachineOperand mo = mi.getOperand(i);
      if (mo.isRegister() && mo.isDef() && mo.getReg() != 0) {
        int reg = mo.getReg();
        if (isPhysicalRegister(reg) ? livePhysReg.get(reg) :
            mri.hasUseOperand(reg))
          return false;
      }
    }
    return true;
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    TargetSubtarget subtarget = mf.getSubtarget();
    TargetRegisterInfo tri = subtarget.getRegisterInfo();
    tii = subtarget.getInstrInfo();
    mri = mf.getMachineRegisterInfo();

    // First, view all non-allocatable registers as live
    BitMap noAllocatableSet = tri.getAllocatableSet(mf);

    livePhysReg = new BitMap(noAllocatableSet.length());
    // walk through all of basic blocks from bottom to top to compute
    // live register set for making decision about what mi should be
    // removed.
    boolean changed = false;
    TIntArrayList defRegs = new TIntArrayList();
    TIntArrayList useRegs = new TIntArrayList();
    for (int i = mf.getNumBlocks() - 1; i >= 0; --i) {
      MachineBasicBlock mbb = mf.getMBBAt(i);
      livePhysReg.setFrom(noAllocatableSet);

      for (int itr = mbb.size() - 1; itr >= 0; --itr) {
        MachineInstr mi = mbb.getInstAt(itr);

        // Delete useless mi
        if (isDead(mi)) {
          mi.removeFromParent();
          changed = true;
          continue;
        }

        // record the def register.
        defRegs.clear();
        useRegs.clear();
        for (int j = 0, e = mi.getNumOperands(); j < e; j++) {
          MachineOperand mo = mi.getOperand(j);
          if (mo.isRegister() && mo.getReg() != 0 &&
              isPhysicalRegister(mo.getReg())) {
            if (mo.isUse())
              useRegs.add(mo.getReg());
            else
              defRegs.add(mo.getReg());
          }
        }

        for (int j = 0; j < defRegs.size(); j++) {
          livePhysReg.clear(defRegs.get(j));

          for (int alias : tri.getAliasSet(defRegs.get(j)))
            livePhysReg.clear(alias);
        }

        for (int j = 0; j < useRegs.size(); j++) {
          livePhysReg.set(useRegs.get(j));
          for (int alias : tri.getAliasSet(defRegs.get(j)))
            livePhysReg.set(alias);
        }
      }
    }

    livePhysReg.clear();
    return changed;
  }

  @Override
  public String getPassName() {
    return "Dead machine instruction elimination Pass";
  }

  public static MachineFunctionPass createDeadMachineInstructionElimPass() {
    return new DeadMachineInstructionElim();
  }
}
