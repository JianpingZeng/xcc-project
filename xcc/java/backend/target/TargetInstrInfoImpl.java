package backend.target;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.codegen.*;
import backend.codegen.MachineOperand.RegState;
import backend.mc.MCInstrDesc;
import backend.mc.MCOperandInfo;
import backend.support.ErrorHandling;
import gnu.trove.list.array.TIntArrayList;
import tools.OutRef;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.MachineInstrBuilder.getDeadRegState;
import static backend.codegen.MachineInstrBuilder.getKillRegState;
import static backend.codegen.PseudoSourceValue.getFixedStack;

/**
 * This is the default implementation of
 * TargetInstrInfo, which just provides a couple of default implementations
 * for various methods.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class TargetInstrInfoImpl extends TargetInstrInfo {
  protected TargetInstrInfoImpl(int frameSetupOp,
                                int frameDestroyOp) {
    super(frameSetupOp, frameDestroyOp);
  }

  @Override
  public void reMaterialize(MachineBasicBlock mbb,
                            int insertPos,
                            int destReg,
                            int subIdx,
                            MachineInstr origin) {
    MachineInstr mi = origin.clone();
    //mi.substituteRegister(mi.getOperand(0).getReg(), destReg, subIdx, mbb.getParent().getTarget().getRegisterInfo());
    MachineOperand mo = mi.getOperand(0);
    mo.setReg(destReg);
    mo.setSubreg(subIdx);
    mbb.insert(insertPos, mi);
  }

  public MachineInstr commuteInstruction(MachineInstr mi) {
    return commuteInstruction(mi, false);
  }

  @Override
  public MachineInstr commuteInstruction(MachineInstr mi, boolean newMI) {
    MCInstrDesc mcid = mi.getDesc();

    // No idea how to commute this instruction. Target should implement its own.
    boolean hasDefs = mcid.getNumDefs() > 0;
    if (hasDefs && !mi.getOperand(0).isRegister())
      return null;

    OutRef<Integer> idx1Ref = new OutRef<>(), idx2Ref = new OutRef<>();
    if (!findCommutedOpIndices(mi, idx1Ref, idx2Ref)) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      mi.print(new PrintStream(baos), mi.getParent().getParent().getTarget());
      ErrorHandling.reportFatalError("Doesn't know how to commute: " + baos.toString());
    }

    int idx1 = idx1Ref.get(), idx2 = idx2Ref.get();
    Util.assertion(mi.getOperand(idx1).isRegister() && mi.getOperand(idx2).isRegister(),
        "This only knows how to commute register operands!");
    int reg0 = hasDefs ? mi.getOperand(0).getReg() : 0;
    int reg1 = mi.getOperand(idx1).getReg();
    int reg2 = mi.getOperand(idx2).getReg();
    boolean reg1IsKill = mi.getOperand(idx1).isKill();
    boolean reg2IsKill = mi.getOperand(idx2).isKill();

    // If destination is tied to either of the commuted source register, then
    // it must be updated.
    if (hasDefs && reg0 == reg1 &&
        mcid.getOperandConstraint(idx1, MCOperandInfo.OperandConstraint.TIED_TO) == 0) {
      reg2IsKill = false;
      reg0 = reg2;
    }
    else if (hasDefs && reg0 == reg2 &&
        mcid.getOperandConstraint(idx2, MCOperandInfo.OperandConstraint.TIED_TO) == 0) {
      reg1IsKill = false;
      reg0 = reg1;
    }

    if (newMI) {
      // Create a new instruction.
      boolean reg0IsDead = mi.getOperand(0).isDead();
      if (hasDefs)
        return buildMI(mi.getDesc(), mi.getDebugLoc())
            .addReg(reg0, RegState.Define | getDeadRegState(reg0IsDead))
            .addReg(reg2, getKillRegState(reg2IsKill))
            .addReg(reg1, getKillRegState(reg2IsKill))
            .getMInstr();
      else
        return buildMI(mi.getDesc(), mi.getDebugLoc())
            .addReg(reg2, getKillRegState(reg2IsKill))
            .addReg(reg1, getKillRegState(reg2IsKill))
            .getMInstr();
    }

    if (hasDefs)
      mi.getOperand(0).setReg(reg0);

    mi.getOperand(idx2).setReg(reg1);
    mi.getOperand(idx2).setIsKill(reg2IsKill);

    mi.getOperand(idx1).setReg(reg1);
    mi.getOperand(idx1).setIsKill(reg1IsKill);
    return mi;
  }

  @Override
  public boolean findCommutedOpIndices(MachineInstr mi,
                                       OutRef<Integer> srcOpIdx1,
                                       OutRef<Integer> srcOpIdx2) {
    MCInstrDesc mcid = mi.getDesc();
    if (!mcid.isCommutable())
      return false;

    // This assumes v0 = op v1, v2 and commuting would swap v1 and v2. If this
    // is not true, then the target must implement this.
    srcOpIdx1.set(mcid.getNumDefs());
    srcOpIdx2.set(srcOpIdx1.get() + 1);
    if (!mi.getOperand(srcOpIdx1.get()).isRegister() ||
        !mi.getOperand(srcOpIdx2.get()).isRegister())
      return false;

    return true;
  }

  @Override
  public MachineInstr foldMemoryOperand(MachineFunction mf, MachineInstr mi,
                                        TIntArrayList ops, int frameIndex) {
    int flags = 0;
    for (int i = 0, e = ops.size(); i < e; ++i) {
     MachineOperand mo = mi.getOperand(ops.get(i));
     if (mo.isDef())
       flags |= MachineMemOperand.MOStore;
     else
       flags |= MachineMemOperand.MOLoad;
    }

    MachineBasicBlock mbb = mi.getParent();
    MachineInstr newMI = foldMemoryOperandImpl(mf, mi, ops, frameIndex);
    if (newMI != null) {
      Util.assertion((flags & MachineMemOperand.MOStore) == 0 &&
          !newMI.getDesc().mayStore(), "folded a def to a non-store");
      Util.assertion((flags & MachineMemOperand.MOLoad) == 0 &&
          !newMI.getDesc().mayLoad(), "folded a def to a non-load");
      MachineFrameInfo mfi = mf.getFrameInfo();
      Util.assertion(mfi.getObjectOffset(frameIndex) != -1);
      MachineMemOperand mmo = new MachineMemOperand(getFixedStack(frameIndex),
          flags, frameIndex, mfi.getObjectSize(frameIndex), mfi.getObjectAlignment(frameIndex));
      newMI.addMemOperand(mmo);
      mbb.insert(mi, newMI);
      return newMI;
    }
    return null;
  }

  @Override
  public MachineInstr foldMemoryOperand(MachineFunction mf, MachineInstr mi,
                                        TIntArrayList ops, MachineInstr loadMI) {
    Util.assertion(loadMI.getDesc().canFoldAsLoad(), "loadMI isn't foldable!");

    MachineBasicBlock mbb = mi.getParent();
    MachineInstr newMI = foldMemoryOperandImpl(mf, mi, ops, loadMI);
    if (newMI == null) return null;

    mbb.insert(mi, newMI);
    newMI.setMemOperands(loadMI.getMemOperands());
    return newMI;
  }

  @Override
  public boolean predicateInstruction(MachineInstr mi,
                                      ArrayList<MachineOperand> pred) {
    boolean madeChange = false;
    MCInstrDesc mcid = mi.getDesc();
    if (!mcid.isPredicable())
      return false;

    for (int i = 0, e = mi.getNumOperands(); i < e; ++i) {
      if (mcid.opInfo[i].isPredicate()) {
        MachineOperand mo = mi.getOperand(i);
        if (mo.isRegister()) {
          mo.setReg(pred.get(i).getReg());
          madeChange = true;
        }
        else if (mo.isImm()) {
          mo.setImm(pred.get(i).getImm());
          madeChange = true;
        }
        else if (mo.isMBB()) {
          mo.setMBB(pred.get(i).getMBB());
          madeChange = true;
        }
      }
    }
    return madeChange;
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
