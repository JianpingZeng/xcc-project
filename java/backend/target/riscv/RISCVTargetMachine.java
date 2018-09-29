package backend.target.riscv;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import java.io.OutputStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class RISCVTargetMachine extends LLVMTargetMachine {

  /**
   * All x86 instruction information can be accessed by this.
   */
  private RISCVInstrInfo instrInfo;
  /**
   * A stack frame info class used for organizing data layout of frame when
   * function calling.
   */
  private TargetFrameInfo frameInfo;
  private RISCVSubtarget subtarget;
  private TargetData dataLayout;
  private RISCVTargetLowering tli;
  private RelocModel defRelocModel;

  protected RISCVTargetMachine(Target t, String triple,
                               String fs) {
    super(t, triple);
    subtarget = new RISCVGenSubtarget(triple, fs);
    dataLayout = new TargetData(triple);
    tli = new RISCVTargetLowering(this);
    defRelocModel = getRelocationModel();
  }

  @Override
  public RISCVSubtarget getSubtarget() {
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
  public TargetFrameInfo getFrameInfo() {
    return frameInfo;
  }

  @Override
  public TargetLowering getTargetLowering() {
    return tli;
  }

  public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  public boolean addPreRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  public boolean addPostRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }

  public boolean addPreEmitPass(PassManagerBase pm, CodeGenOpt level) {
    return false;
  }
  /**
   * This pass should be overridden by the target to add
   * a code emitter (without setting flags), if supported.  If this is not
   * supported, 'true' should be returned.
   *
   * @param pm
   * @param level
   * @param mce
   * @return
   */
  public boolean addSimpleCodeEmitter(PassManagerBase pm, CodeGenOpt level,
                                      MachineCodeEmitter mce) {
    return true;
  }

  public boolean addAssemblyEmitter(PassManagerBase pm, CodeGenOpt level,
                                    boolean verbose,
                                    OutputStream os) {
    return true;
  }
}
