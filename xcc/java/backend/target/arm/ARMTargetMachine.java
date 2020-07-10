package backend.target.arm;
/*
 * Extremely Compiler Collection
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

import backend.codegen.MachineCodeEmitter;
import backend.passManaging.FunctionPassManager;
import backend.passManaging.PassManagerBase;
import backend.target.LLVMTargetMachine;
import backend.target.Target;
import backend.target.TargetData;

import java.io.PrintStream;

import static backend.target.arm.ARMConstantPoolIslandPass.createARMConstantIslandPass;
import static backend.target.arm.ARMDAGISel.createARMISelDAG;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMTargetMachine extends LLVMTargetMachine {
  private ARMSubtarget subtarget;

  /**
   * Can only called by subclass.
   *
   * @param t
   * @param triple
   * @param cpu
   * @param features
   */
  protected ARMTargetMachine(Target t, String triple, String cpu,
                             String features, RelocModel rm, CodeModel cm) {
    super(t, triple);
    subtarget = new ARMSubtarget(this, triple, cpu, features, false);
  }

  @Override
  public TargetData getTargetData() {
    return subtarget.getDataLayout();
  }

  @Override
  public ARMSubtarget getSubtarget() {
    return subtarget;
  }

  @Override
  public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level) {
    pm.add(createARMISelDAG(this, level));
    return false;
  }

  @Override
  public boolean addPreRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    return super.addPreRegAlloc(pm, level);
  }

  @Override
  public boolean addPostRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    pm.add(ARMExpandPseudoInsts.createARMExpandPseudoPass());
    return false;
  }

  @Override
  public boolean addPreEmitPass(PassManagerBase pm, CodeGenOpt level) {
    pm.add(createARMConstantIslandPass());
    return false;
  }

  @Override
  public boolean addCodeEmitter(PassManagerBase pm, CodeGenOpt level, MachineCodeEmitter mce) {
    return super.addCodeEmitter(pm, level, mce);
  }

  @Override
  public MachineCodeEmitter addELFWriter(FunctionPassManager pm, PrintStream os) {
    return super.addELFWriter(pm, os);
  }
}
