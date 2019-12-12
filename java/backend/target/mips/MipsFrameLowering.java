package backend.target.mips;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFrameInfo;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.debug.DebugLoc;
import backend.target.TargetFrameLowering;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import tools.OutRef;
import tools.Util;

import static backend.codegen.MachineInstrBuilder.buildMI;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsFrameLowering extends TargetFrameLowering {
  private MipsSubtarget subtarget;
  public MipsFrameLowering(MipsSubtarget subtarget) {
    super(StackDirection.StackGrowDown, subtarget.hasMips64() ? 16: 8, 0);
    this.subtarget = subtarget;
  }

  @Override
  public void emitPrologue(MachineFunction mf) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    MipsFunctionInfo mipsFuncInfo = (MipsFunctionInfo) mf.getInfo();
    MachineBasicBlock mbb = mf.getEntryBlock();
    int itr = 0;  // a pointer to the first instruction of mbb.
    DebugLoc dl = itr != mbb.size() ? mbb.getInstAt(itr).getDebugLoc() : new DebugLoc();
    boolean isPIC = mf.getTarget().getRelocationModel() == TargetMachine.RelocModel.PIC_;
    MipsInstrInfo tii = subtarget.getInstrInfo();
    MipsRegisterInfo regInfo = subtarget.getRegisterInfo();

    // First, compute final stack size.
    int regSize = subtarget.isGP32bit() ? 4 : 8;
    int stackAlign = getStackAlignment();
    int localVarAreaOffset = mipsFuncInfo.needGPSaveRestore() ?
        mfi.getObjectOffset(mipsFuncInfo.getGPFI() + regSize) :
        mipsFuncInfo.getMaxCallFrameSize();
    int stackSize = Util.roundUp(localVarAreaOffset, stackAlign) +
        Util.roundUp(mfi.getStackSize(), stackAlign);

    // update stack size.
    mfi.setStackSize(stackSize);

    buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.NOREORDER));

    if (isPIC && subtarget.isABI_O32())
      buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.CPLOAD)).addReg(regInfo.getPICCallReg());

    buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.NOMACRO));

    // Adjust stack: addi sp, sp, (-imm)
    buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.ADDiu), MipsGenRegisterNames.SP)
        .addReg(MipsGenRegisterNames.SP).addImm(-stackSize);

    // Save the return address only if the function isn't a leaf one.
    // sw $ra, stack_loc($sp)
    if (mfi.hasCalls()) {
      buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.SW))
          .addReg(MipsGenRegisterNames.RA)
          .addImm(-stackSize).addReg(MipsGenRegisterNames.SP);
    }

    // if frame pointer enabled,save it and set it to point to the stack pointer.
    if (hasFP(mf)) {
      // "move $fp, $sp" at this location.
      buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.ADDu), MipsGenRegisterNames.FP)
          .addReg(MipsGenRegisterNames.SP).addReg(MipsGenRegisterNames.ZERO);
    }

    // restore GP from the saved stack location.
    if (mipsFuncInfo.needGPSaveRestore()) {
      int offset = mfi.getObjectOffset(mipsFuncInfo.getGPFI());
      buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.CPRESTORE)).addImm(offset);

      if (offset >= 0x8000) {
        buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.MACRO));
        buildMI(mbb, itr, dl, tii.get(MipsGenInstrNames.NOMACRO));
      }
    }
  }

  /**
   * expand pair of register and immediate if the immediate doesn't fit in the
   * 16-bit offset field.
   * e.g.
   * <code>
   *   if origImm = 0x10000, origReg = SP.
   *   generate the following sequence of instrs:
   *   lui $at, hi(0x10000)
   *   addu $at, $sp, $at
   *
   *   (newReg, newImm) = ($at, lo(0x10000))
   *   return true.
   * </code>
   * @param origReg
   * @param origImm
   * @param newReg
   * @param newImm
   * @param mbb
   * @param itr
   * @return
   */
  private static boolean expandRegLargeImmPair(int origReg, int origImm,
                                               OutRef<Integer> newReg,
                                               OutRef<Integer> newImm,
                                               MachineBasicBlock mbb,
                                               int itr) {
    if (origImm < 0x8000 && origImm >= -0x8000) {
      newReg.set(origReg);
      newImm.set(origImm);
      return false;
    }

    MachineFunction mf = mbb.getParent();
    TargetInstrInfo tii = mf.getTarget().getInstrInfo();
    DebugLoc dl = mbb.getInstAt(itr).getDebugLoc();
    int immLo = (short)(origImm & 0xffff);
    int immHi = (short)(origImm >>> 16) + (origImm & 0x8000) != 0 ? 1 : 0;

    buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.NOAT));
    buildMI(mbb, itr++, dl, tii.get(MipsGenInstrNames.LUi), MipsGenRegisterNames.AT).addImm(immHi);
    buildMI(mbb, itr, dl, tii.get(MipsGenInstrNames.ADDu), MipsGenRegisterNames.AT)
        .addReg(origReg).addReg(MipsGenRegisterNames.AT);
    newReg.set(MipsGenRegisterNames.AT);
    newImm.set(immLo);
    return true;
  }

  @Override
  public void emitEpilogue(MachineFunction mf, MachineBasicBlock mbb) {
    MipsInstrInfo instrInfo = subtarget.getInstrInfo();
    int idx = mbb.getFirstTerminator();
    MachineInstr mi = mbb.getInstAt(idx);
    MachineFrameInfo mfi = mf.getFrameInfo();
    DebugLoc dl = mi.getDebugLoc();

    int stackSize = mfi.getStackSize();
    if (hasFP(mf)) {
      int itr = idx;
      for (int i = 0, e = mfi.getCalleeSavedInfo().size(); i < e; ++i)
        --itr;

      // insert instruction "move $sp, $fp" at this location.
      buildMI(mbb, itr, dl, instrInfo.get(MipsGenInstrNames.ADDu),
          MipsGenRegisterNames.SP).addReg(MipsGenRegisterNames.FP)
          .addReg(MipsGenRegisterNames.ZERO);
    }

    // adjust stack --> insert addi sp, sp (imm)
    if (stackSize != 0) {
      int newReg = 0;
      int newImm = 0;
      boolean atUsed;
      OutRef<Integer> regRef = new OutRef<>(0), immRef = new OutRef<>(0);
      atUsed = expandRegLargeImmPair(MipsGenRegisterNames.SP, stackSize,
          regRef, immRef, mbb, idx);
      newReg = regRef.get();
      newImm = immRef.get();

      buildMI(mbb, idx++, dl, instrInfo.get(MipsGenInstrNames.ADDiu),
          MipsGenRegisterNames.SP).addReg(newReg).addImm(newImm);

      if (atUsed)
        buildMI(mbb, idx, dl, instrInfo.get(MipsGenInstrNames.ATMACRO));
    }
  }

  @Override
  public boolean hasFP(MachineFunction mf) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    return disableFramePointerElim(mf) || mfi.hasVarSizedObjects() ||
        mfi.isFrameAddressTaken();
  }
}
