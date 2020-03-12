package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.codegen.*;
import backend.debug.DebugLoc;
import backend.mc.MCInstrDesc;
import backend.mc.MCRegisterClass;
import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantInt;
import tools.OutRef;
import tools.Util;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.MachineInstrBuilder.getDefRegState;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
class Thumb1RegisterInfo extends ARMGenRegisterInfo {
  Thumb1RegisterInfo(ARMSubtarget subtarget, int mode) {
    super(subtarget, mode);
  }

  @Override
  public MCRegisterClass getPointerRegClass(int kind) {
    return ARMGenRegisterInfo.tGPRRegisterClass;
  }

  /**
   * Materialize that value {@param val} by emit a load reading that
   * constant from constant pool to the destination register.
   * @param mbb
   * @param mbbi
   * @param dl
   * @param destReg
   * @param subIdx
   * @param val
   * @param pred
   * @param predReg
   * @param miFlags
   * @return
   */
  @Override
  public int emitLoadConstantPool(MachineBasicBlock mbb,
                                  int mbbi, DebugLoc dl,
                                  int destReg, int subIdx,
                                  int val, ARMCC.CondCodes pred,
                                  int predReg, int miFlags) {
    MachineFunction mf = mbb.getParent();
    MachineConstantPool constantPool = mf.getConstantPool();
    Constant c= ConstantInt.get(Type.getInt32Ty(mf.getFunction().getContext()), val);
    int idx = constantPool.getConstantPoolIndex(c, 4);

    buildMI(mbb, mbbi, dl, subtarget.getInstrInfo().get(ARMGenInstrNames.tLDRpci))
        .addReg(destReg, getDefRegState(true), subIdx)
        .addConstantPoolIndex(idx, 0, 0).addImm(pred.ordinal()).addReg(predReg)
        .setMIFlags(miFlags);
    return ++mbbi;
  }

  @Override
  public void eliminateFrameIndex(MachineFunction mf,
                                  int spAdj, MachineInstr mi,
                                  RegScavenger rs) {
    MachineBasicBlock mbb = mi.getParent();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    DebugLoc dl = mi.getDebugLoc();
    MachineFrameInfo mfi = mf.getFrameInfo();
    int ii = mi.getIndexInMBB();

    int i = 0;
    while (i < mi.getNumOperands() && !mi.getOperand(i).isFrameIndex()) {
      ++i;
    }

    Util.assertion(i != mi.getNumOperands(), "Instr doesn't have a frame index operand");

    int frameReg = ARMGenRegisterNames.SP;
    int frameIndex = mi.getOperand(i).getIndex();
    int offset = mfi.getObjectOffset(frameIndex) +
        mfi.getStackSize() + spAdj;

    if (afi.isGPRCalleeSavedArea1Frame(frameIndex))
      offset -= afi.getGPRCalleeSavedArea1Offset();
    else if (afi.isGPRCalleeSavedArea2Frame(frameIndex))
      offset -= afi.getGPRCalleeSavedArea2Offset();
    else if (mfi.hasVarSizedObjects()) {
      Util.assertion(spAdj == 0 && subtarget.getFrameLowering().hasFP(mf),
          "unexpected");
      if (!hasBasePointer(mf)) {
        frameReg = getFrameRegister(mf);
        offset -= afi.getFramePtrSpillOffset();
      }
      else
        frameReg = getBaseRegister();
    }

    Util.assertion(afi.isThumbFunction(), "this elimiateFrameIndex only supports thumb1");

    OutRef<Integer> offsetRef = new OutRef<>(offset);
    if (rewriteFrameIndex(mi, i, frameReg, offsetRef, subtarget.getInstrInfo()))
      return;

    offset = offsetRef.get();
    // if we got here, the immediate doesn't fit into the instruction. We have
    // to provide a register.
    int opcode = mi.getOpcode();
    MCInstrDesc mcid = subtarget.getInstrInfo().get(opcode);

    int predIdx = mi.findFirstPredOperandIdx();
    if (predIdx != -1)
      removeOperands(mi, predIdx);

    if (mcid.mayLoad()) {
      // use the destination register to materialize sp + offset.
      int tmpReg = mi.getOperand(0).getReg();
      boolean usePR = false;
      if (opcode == ARMGenInstrNames.tLDRspi) {
        if (frameReg == ARMGenRegisterNames.SP) {
          ii = emitThumbRegPlusImmInReg(mbb, ii, dl, tmpReg, frameReg,
              offset, false, subtarget.getInstrInfo(), this);
        }
        else {
          ii = emitLoadConstantPool(mbb, ii, dl, tmpReg, 0, offset, ARMCC.CondCodes.AL, 0);
          usePR = true;
        }
      }
      else {
        ii = emitThumbRegPlusImmediate(mbb, ii, dl, tmpReg, frameReg, offset, subtarget.getInstrInfo(),
            this);
      }

      mi.setDesc(subtarget.getInstrInfo().get(usePR ? ARMGenInstrNames.tLDRr : ARMGenInstrNames.tLDRi));
      mi.getOperand(i).changeToRegister(tmpReg, false, false, true, false, false);
      if (usePR) {
        mi.getOperand(i+1).changeToRegister(frameReg, false);
      }
    }
    else if (mcid.mayStore()) {
      // create a virtual register computing the destination address of store.
      int vreg = mf.getMachineRegisterInfo().createVirtualRegister(ARMGenRegisterInfo.tGPRRegisterClass);
      boolean useRR = false;
      if (opcode == ARMGenInstrNames.tSTRspi) {
        if (frameReg == ARMGenRegisterNames.SP) {
          ii = emitThumbRegPlusImmInReg(mbb, ii, dl, vreg, frameReg, offset, false,
              subtarget.getInstrInfo(), this);
        }
        else {
          ii = emitLoadConstantPool(mbb, ii, dl, vreg, 0, offset, ARMCC.CondCodes.AL, 0);
          useRR = true;
        }
      }
      else
        ii = emitThumbRegPlusImmediate(mbb, ii, dl, vreg, frameReg, offset,
            subtarget.getInstrInfo(), this);

      mi.setDesc(subtarget.getInstrInfo().get(useRR ? ARMGenInstrNames.tSTRr : ARMGenInstrNames.tSTRi));
      mi.getOperand(i).changeToRegister(vreg, false, false, true, false, false);
      if (useRR) {
        // Use [reg, reg] addrmode. Replace the immediate operand w/ the frame
        // register. The offset is already handled in the vreg value.
        mi.getOperand(i + 1).changeToRegister(frameReg, false);
      }
    }
    else
      Util.assertion("unexpected opcode!");

    if (mi.getDesc().isPredicable()) {
      MachineInstrBuilder mib = new MachineInstrBuilder(mi);
      addDefaultPred(mib);
    }
  }

  private static void removeOperands(MachineInstr mi, int idx) {
    int op = idx;
    for (int e = mi.getNumOperands(); idx != e; ++idx)
      mi.removeOperand(op);
  }

  public boolean rewriteFrameIndex(MachineInstr mi,
                                   int frameRegIdx,
                                   int frameReg,
                                   OutRef<Integer> offset,
                                   ARMInstrInfo tii) {
    int ii = mi.getIndexInMBB();
    MachineBasicBlock mbb = mi.getParent();
    DebugLoc dl = mi.getDebugLoc();
    int opcode = mi.getOpcode();
    MCInstrDesc mcid = tii.get(opcode);
    int addrMode = mcid.tSFlags & ARMII.AddrModeMask;

    if (opcode == ARMGenInstrNames.tADDrSPi) {
      offset.set(offset.get() + (int)mi.getOperand(frameRegIdx+1).getImm());
      int numBits = 0;
      int scale = 1;
      if (frameReg != ARMGenRegisterNames.SP) {
        opcode = ARMGenInstrNames.tADDi3;
        numBits = 3;
      }
      else {
        numBits = 8;
        scale = 4;
        Util.assertion((offset.get() & 3) == 0, "thumb1 add/sub sp, #imm immediate must be multiple of 4!");
      }

      OutRef<Integer> predReg = new OutRef<>(0);
      if (offset.get() == 0 && getInstrPredicate(mi, predReg) == ARMCC.CondCodes.AL) {
        mi.setDesc(tii.get(ARMGenInstrNames.tMOVr));
        mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
        // remove offset.
        mi.removeOperand(frameRegIdx+1);
        return true;
      }

      // common case: small offset, fits into instruction.
      int mask = (1<< numBits) -1;
      if (((offset.get() / scale) & ~mask) == 0) {
        // replace the frameindex with sp/fp.
        if (opcode == ARMGenInstrNames.tADDi3) {
          mi.setDesc(tii.get(opcode));
          removeOperands(mi, frameRegIdx);
          MachineInstrBuilder mib = new MachineInstrBuilder(mi);
          addDefaultPred(addDefaultT1CC(mib).addReg(frameReg)
              .addImm(offset.get()/scale));
        }
        else {
          mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);;
          mi.getOperand(frameRegIdx+1).changeToImmediate(offset.get()/scale);
        }
        return true;
      }

      int destReg = mi.getOperand(0).getReg();
      int bytes = offset.get() > 0 ? offset.get() : -offset.get();
      int numMIs = calcNumMI(opcode, 0, bytes, numBits, scale);
      // MI would expand into a large number of instructions. Don' try to simplify
      // the immediate.
      if (numMIs > 2) {
        ii = emitThumbRegPlusImmediate(mbb, ii, dl, destReg, frameReg, offset.get(), tii, this);
        mbb.remove(ii);
        return true;
      }

      if (offset.get() > 0) {
        // translate r0 = add sp, imm to
        // r0 = add sp, 255*4
        // r0 = r0 + (imm - 255*4)
        if (opcode == ARMGenInstrNames.tADDi3) {
          mi.setDesc(tii.get(opcode));
          removeOperands(mi, frameRegIdx);
          MachineInstrBuilder mib = new MachineInstrBuilder(mi);
          addDefaultPred(addDefaultT1CC(mib).addReg(frameReg).addImm(mask));
        }
        else {
          mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
          mi.getOperand(frameRegIdx+1).changeToImmediate(mask);
        }
        offset.set(offset.get() - mask * scale);
        emitThumbRegPlusImmediate(mbb, ii + 1, dl, destReg, destReg, offset.get(), tii, this);
      }
      else {
        // translate r0 = add sp, -imm to
        // r0 = -imm (this is then translated into a series of instructons)
        // r0 = add r0, sp
        ii = emitThumbConstant(mbb, ii, destReg, offset.get(), tii, this, dl);
        mi.setDesc(tii.get(ARMGenInstrNames.tADDhirr));
        mi.getOperand(frameRegIdx).changeToRegister(destReg, false, false, true, false, false);
        mi.getOperand(frameRegIdx+1).changeToRegister(frameReg, false);
      }
      return true;
    }
    else {
      if (addrMode != ARMII.AddrMode.AddrModeT1_s.ordinal())
        Util.assertion("unsupported addressing mode!");

      int immIdx = frameRegIdx + 1;
      int instrOffsets = (int)mi.getOperand(immIdx).getImm();
      int numBits = frameReg == ARMGenRegisterNames.SP ? 8 : 5;
      int scale = 4;

      offset.set(offset.get() + instrOffsets * scale);
      Util.assertion((offset.get() & (scale - 1)) == 0, "can't encode this offset!");

      MachineOperand immOp = mi.getOperand(immIdx);
      int immOffset = offset.get() / scale;
      int mask = (1 << numBits) - 1;

      if (offset.get() <= mask * scale) {
        mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
        immOp.changeToImmediate(immOffset);

        int newOpc = convertToNonSPOpcode(opcode);
        if (newOpc != opcode && frameReg != ARMGenRegisterNames.SP)
          mi.setDesc(tii.get(newOpc));

        return true;
      }

      numBits = 5;
      mask = (1 << numBits) -1;

      if (opcode == ARMGenInstrNames.tLDRspi || opcode == ARMGenInstrNames.tSTRspi) {
        immOp.changeToImmediate(0);
      }
      else {
        immOffset = immOffset & mask;
        immOp.changeToImmediate(immOffset);
        offset.set(offset.get() & ~(mask * scale));
      }
    }
    return offset.get() == 0;
  }

  private static int emitThumbConstant(MachineBasicBlock mbb,
                                       int mbbi, int destReg,
                                       int imm, ARMInstrInfo tii,
                                       Thumb1RegisterInfo tri,
                                       DebugLoc dl) {
    boolean isSub = imm < 0;
    if (isSub) imm = -imm;

    int chunk = (1 << 8) -1;
    int thisVal = imm > chunk ? chunk : imm;
    imm -= thisVal;
    addDefaultPred(addDefaultT1CC(buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.tMOVi8), destReg)
        .addImm(thisVal)));
    if (imm > 0)
      return emitThumbRegPlusImmediate(mbb, mbbi, dl, destReg, destReg, imm, tii, tri);
    if (isSub) {
      MCInstrDesc mcid = tii.get(ARMGenInstrNames.tRSB);
      addDefaultPred(addDefaultT1CC(buildMI(mbb, mbbi, dl, mcid, destReg).addReg(destReg, MachineOperand.RegState.Kill)));
    }
    return mbbi;
  }

  private static int convertToNonSPOpcode(int opcode) {
    switch (opcode) {
      case ARMGenInstrNames.tLDRspi:
        return ARMGenInstrNames.tLDRi;
      case ARMGenInstrNames.tSTRspi:
        return ARMGenInstrNames.tSTRi;
    }
    return opcode;
  }
}
