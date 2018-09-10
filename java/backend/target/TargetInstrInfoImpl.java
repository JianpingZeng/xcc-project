package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import gnu.trove.list.array.TIntArrayList;
import tools.OutRef;

import java.util.ArrayList;

/**
 * This is the default implementation of
 * RISCVGenInstrInfo, which just provides a couple of default implementations
 * for various methods.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class TargetInstrInfoImpl extends TargetInstrInfo {
  protected TargetInstrInfoImpl(TargetInstrDesc[] desc) {
    super(desc);
  }

  @Override
  public void reMaterialize(MachineBasicBlock mbb,
                            int insertPos,
                            int destReg,
                            int subIdx,
                            MachineInstr origin) {

  }

  public MachineInstr commuteInstruction(MachineInstr mi) {
    return commuteInstruction(mi, false);
  }

  @Override
  public MachineInstr commuteInstruction(MachineInstr mi, boolean newMI) {

    return null;
  }

  @Override
  public boolean findCommutedOpIndices(MachineInstr mi,
                                       OutRef<Integer> srcOpIdx1,
                                       OutRef<Integer> srcOpIdx2) {
    return false;
  }

  @Override
  public MachineInstr foldMemoryOperand(MachineFunction mf, MachineInstr mi,
                                        TIntArrayList ops, int frameIndex) {
    return null;
  }

  @Override
  public MachineInstr foldMemoryOperand(MachineFunction mf, MachineInstr mi,
                                        TIntArrayList ops, MachineInstr loadMI) {
    return null;
  }

  @Override
  public void insertNoop(MachineBasicBlock mbb, int pos) {

  }

  @Override
  public boolean isUnpredicatedTerminator(MachineInstr mi) {
    return false;
  }

  @Override
  public boolean predicateInstruction(MachineInstr mi,
                                      ArrayList<MachineOperand> pred) {
    return false;
  }

  @Override
  public boolean isDeadInstruction(MachineInstr mi) {
    return false;
  }

  @Override
  public int getFunctionSizeInBytes(MachineFunction mf) {
    int totalSize = 0;
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      for (MachineInstr mi : mbb.getInsts()) {
        totalSize += getInstSizeInBytes(mi);
      }
    }
    return totalSize;
  }
}
