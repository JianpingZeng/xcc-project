package backend.target.arm;
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

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.dagisel.SDNode;
import backend.codegen.dagisel.SDValue;
import backend.codegen.dagisel.SelectionDAGISel;
import backend.pass.FunctionPass;
import backend.pass.Pass;
import backend.pass.RegisterPass;
import backend.target.TargetMachine;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class ARMDAGISel extends SelectionDAGISel {
  protected ARMTargetLowering tli;
  protected ARMSubtarget subtarget;

  protected ARMDAGISel(ARMTargetMachine tm, TargetMachine.CodeGenOpt optLevel) {
    super(tm, optLevel);
    try {
      new RegisterPass("Instruction Selector based on DAG covering", "dag-isel",
          Class.forName("backend.target.arm.ARMGenDAGISel").asSubclass(Pass.class));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    tli = tm.getTargetLowering();
    subtarget = tm.getSubtarget();
  }

  @Override
  public String getPassName() {
    return "ARM Instruction Selection";
  }

  @Override
  public SDNode select(SDNode node) {
    EVT nvt = node.getValueType(0);
    int opcode = node.getOpcode();

    if (Util.DEBUG) {
      System.err.print("Selecting: ");
      node.dump(curDAG);
      System.err.println();
    }
    if (node.isMachineOpecode()) {
      // Already been selected.
      return null;
    }

    SDNode resNode = selectCommonCode(node);
    if (Util.DEBUG) {
      System.err.print("=> ");
      if (resNode == null || resNode.equals(node))
        node.dump(curDAG);
      else
        resNode.dump(curDAG);
      System.err.println();
    }
    return resNode;
  }

  /**
   * register +- 12 bit offset.
   * @param root
   * @param n
   * @param tmp
   * @return
   */
  protected boolean selectAddrRegImm(SDNode root, SDValue n, SDValue[] tmp) {
    // temp = {offset, base}
    tmp[0] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
    if (n.getNode() instanceof SDNode.FrameIndexSDNode) {
      SDNode.FrameIndexSDNode fi = (SDNode.FrameIndexSDNode) n.getNode();
      tmp[1] = curDAG.getTargetFrameIndex(fi.getFrameIndex(), n.getValueType());
    }
    else
      tmp[1] = n;
    // any address fits in a register.
    return true;
  }

  public static FunctionPass createARMISelDAG(ARMTargetMachine tm, TargetMachine.CodeGenOpt level) {
    return new ARMGenDAGISel(tm, level);
  }
}
