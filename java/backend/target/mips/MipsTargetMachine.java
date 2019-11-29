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

import backend.codegen.MachineCodeEmitter;
import backend.passManaging.PassManagerBase;
import backend.target.*;

/**
 * This class is designed to contain some important data structures specific to the
 * Mips target, such as {@linkplain MipsSubtarget}, {@linkplain TargetData},
 * {@linkplain MipsRegisterInfo}, {@linkplain MipsInstrInfo}, {@linkplain MipsTargetLowering},
 * and {@linkplain MipsFrameLowering}, to lower the target independent operations to
 * the target supported instructions. This can happens on different phases, such as,
 * {@linkplain MipsDAGISel} for lowering the DAG operation to machine instruction, etc.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsTargetMachine extends LLVMTargetMachine {
  private MipsSubtarget subtarget;
  private TargetData dataLayout;
  private MipsInstrInfo instrInfo;
  private MipsRegisterInfo registerInfo;
  private MipsFrameLowering frameLowering;
  private MipsTargetLowering tli;

  protected MipsTargetMachine(Target target, String triple,
                              String cpu, String features,
                              RelocModel rm, CodeModel cm,
                              boolean isLittle) {
    super(target, triple);
    subtarget = new MipsSubtarget(this, triple, cpu, features, isLittle);
  }

  @Override
  public MipsSubtarget getSubtarget() {
    return subtarget;
  }

  @Override
  public TargetInstrInfo getInstrInfo() {
    return subtarget.getInstrInfo();
  }

  @Override
  public TargetRegisterInfo getRegisterInfo() {
    return subtarget.getRegisterInfo();
  }

  @Override
  public TargetFrameLowering getFrameLowering() {
    return frameLowering;
  }

  @Override
  public TargetLowering getTargetLowering() {
    return tli;
  }

  public TargetData getTargetData() {
    return dataLayout;
  }

  @Override
  public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  @Override
  public boolean addPreEmitPass(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  @Override
  public boolean addPreRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  @Override
  public boolean addPostRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  @Override
  public boolean addCodeEmitter(PassManagerBase pm, CodeGenOpt level, MachineCodeEmitter mce) {
    return false;
  }
}
