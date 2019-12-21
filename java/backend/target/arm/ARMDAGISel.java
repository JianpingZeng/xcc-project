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
import backend.codegen.dagisel.ISD;
import backend.codegen.dagisel.SDNode;
import backend.codegen.dagisel.SDValue;
import backend.codegen.dagisel.SelectionDAGISel;
import backend.pass.FunctionPass;
import backend.pass.Pass;
import backend.pass.RegisterPass;
import backend.target.TargetMachine;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHidden;
import tools.commandline.OptionHiddenApplicator;

import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class ARMDAGISel extends SelectionDAGISel {
  static final BooleanOpt DisableShifterOp = new BooleanOpt(
      optionName("disable-shifter-opt"),
      new OptionHiddenApplicator(OptionHidden.Hidden),
      desc("Disable isel of shifter-opt"),
      init(false));

  static final BooleanOpt CheckVMLxHazard = new BooleanOpt(
      optionName("check-vmlx-hazard"),
      new OptionHiddenApplicator(OptionHidden.Hidden),
      desc("Check fp vmla / vmls hazard at isel time"),
      init(true));

  static final BooleanOpt DisableARMIntABS = new BooleanOpt(
      optionName("disable-arm-int-abs"),
      new OptionHiddenApplicator(OptionHidden.Hidden),
      desc("Enable / disable ARM integer abs transform"),
      init(false));

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
   *
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
    } else
      tmp[1] = n;
    // any address fits in a register.
    return true;
  }

  protected boolean hasNoVMLxHazardUse(SDNode n) {
    return false;
  }

  protected boolean selectRegShifterOperand(SDValue n, SDValue[] tmp) {
    if (DisableShifterOp.value)
      return false;

    ARM_AM.ShiftOpc shOpcVal = ARM_AM.getShiftOpcForNode(n.getOpcode());

    return false;
  }

  protected boolean selectImmShifterOperand(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectT2ShifterOperandReg(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectShiftRegShifterOperand(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectShiftImmShifterOperand(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectLdStSOReg(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectT2AddrModeSoReg(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrModeImm12(SDValue n, SDValue[] tmp) {
    // tmp = {base, offImm}
    // Match simple R + imm12 operands.
    if (n.getOpcode() != ISD.ADD && n.getOpcode() != ISD.SUB &&
        !curDAG.isBaseWithConstantOffset(n)) {
      // match frame index.
      if (n.getOpcode() == ISD.FrameIndex) {
        int fi = ((SDNode.FrameIndexSDNode) (n.getNode())).getFrameIndex();
        tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
        tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
        return true;
      }
      if (n.getOpcode() == ARMISD.Wrapper &&
          !(subtarget.useMovt() && n.getOperand(0).getOpcode() == ISD.TargetGlobalAddress)) {
        tmp[0] = n.getOperand(0);
      } else
        tmp[0] = n;
      tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
      return true;
    }

    if (n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
      SDNode.ConstantSDNode rhs = (SDNode.ConstantSDNode) n.getOperand(1).getNode();
      long rhsc = rhs.getZExtValue();
      if (n.getOpcode() == ISD.SUB)
        rhsc = -rhsc;

      if (rhsc >= 0 && rhsc < 0x1000) {
        // 12 bits
        tmp[0] = n.getOperand(0);
        if(tmp[0].getOpcode() == ISD.FrameIndex) {
          int fi = ((SDNode.FrameIndexSDNode)tmp[0].getNode()).getFrameIndex();
          tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
        }
        tmp[1] = curDAG.getTargetConstant(rhsc, new EVT(MVT.i32));
        return true;
      }
    }
    // Base only.
    tmp[0] = n;
    tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
    return true;
  }

  protected boolean selectT2AddrModeImm12(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectT2AddrModeImm8(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrMode2(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrMode6(SDNode parent, SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrMode6Offset(SDNode op, SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrModePC(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrOffsetNone(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrMode3Offset(SDNode op, SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrMode2OffsetReg(SDNode op, SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrMode2OffsetImm(SDNode op, SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrMode3(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectT2AddrModeImm8Offset(SDNode op, SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectThumbAddrModeRI5S4(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectThumbAddrModeImm5S4(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectThumbAddrModeRI5S1(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectThumbAddrModeImm5S1(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectThumbAddrModeRI5S2(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectThumbAddrModeImm5S2(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectThumbAddrModeSP(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectAddrMode5(SDValue n, SDValue[] tmp) {
    return false;
  }

  protected boolean selectThumbAddrModeRR(SDValue n, SDValue[] tmp) {
    return false;
  }

  static FunctionPass createARMISelDAG(ARMTargetMachine tm, TargetMachine.CodeGenOpt level) {
    return new ARMGenDAGISel(tm, level);
  }
}
