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
import backend.codegen.dagisel.*;
import backend.pass.FunctionPass;
import backend.pass.Pass;
import backend.pass.RegisterPass;
import backend.target.TargetMachine;
import tools.OutRef;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHidden;
import tools.commandline.OptionHiddenApplicator;

import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * The detailed document of ARM addressing modes can be viewed from the following file.
 * the chapter A5 of ARM Architecture Reference Manual (2005).
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

    switch (opcode) {
      default:
        break;
      case ISD.XOR: {
        // select special operations if XOR node froms integer ABS pattern.
        break;
      }
      case ARMISD.CMOV:
        return selectCMOVOp(node);
      case ARMISD.BRCOND: {
        // Pattern: (ARMbrcond:void (bb:Other):$dst, (imm:i32):$cc)
        // Emits: (Bcc:void (bb:Other):$dst, (imm:i32):$cc)
        // Pattern complexity = 6  cost = 1  size = 0

        // Pattern: (ARMbrcond:void (bb:Other):$dst, (imm:i32):$cc)
        // Emits: (tBcc:void (bb:Other):$dst, (imm:i32):$cc)
        // Pattern complexity = 6  cost = 1  size = 0

        // Pattern: (ARMbrcond:void (bb:Other):$dst, (imm:i32):$cc)
        // Emits: (t2Bcc:void (bb:Other):$dst, (imm:i32):$cc)
        // Pattern complexity = 6  cost = 1  size = 0
        int opc = subtarget.isThumb() ? subtarget.hasThumb2() ?
            ARMGenInstrNames.t2Bcc : ARMGenInstrNames.tBcc : ARMGenInstrNames.Bcc;
        SDValue chain = node.getOperand(0);
        SDValue n1 = node.getOperand(1);
        SDValue n2 = node.getOperand(2);
        SDValue n3 = node.getOperand(3);
        SDValue inFlag = node.getOperand(4);
        // destination block.
        Util.assertion(n1.getOpcode() == ISD.BasicBlock);
        // ARM_CC::CondCodes
        Util.assertion(n2.getOpcode() == ISD.Constant);
        // the CPSR register.
        Util.assertion(n3.getOpcode() == ISD.Register);

        SDValue temp = curDAG.getTargetConstant(((SDNode.ConstantSDNode) n2.getNode()).getZExtValue(),
            new EVT(MVT.i32));
        SDValue[] ops = new SDValue[]{n1, temp, n3, chain, inFlag};
        SDNode resNode = curDAG.getMachineNode(opc, new EVT(MVT.Other), new EVT(MVT.Glue), ops);
        chain = new SDValue(resNode, 0);
        if (node.getNumValues() == 2) {
          inFlag = new SDValue(resNode, 1);
          replaceUses(new SDValue(node, 1), inFlag);
        }
        replaceUses(new SDValue(node, 0), chain);
        return null;
      }
      case ISD.FrameIndex: {
        // select the frame index to the ADDri, FI, 0 which in turn will become ADDri SP, imm.
        int fi = ((SDNode.FrameIndexSDNode)node).getFrameIndex();
        SDValue fin = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
        SDValue al = curDAG.getTargetConstant(ARMCC.CondCodes.AL.ordinal(), new EVT(MVT.i32));
        if (subtarget.isThumb1Only()) {
          SDValue[] ops = {fin, curDAG.getTargetConstant(0, new EVT(MVT.i32)),
              al, curDAG.getRegister(0, new EVT(MVT.i32)), curDAG.getRegister(0, new EVT(MVT.i32))};
          return curDAG.selectNodeTo(node, ARMGenInstrNames.tADDrSPi, new EVT(MVT.i32), ops);
        }
        else {
          int opc = subtarget.isThumb() && subtarget.hasThumb2() ? ARMGenInstrNames.t2ADDri :
              ARMGenInstrNames.ADDri;
          SDValue[] ops = {fin, curDAG.getTargetConstant(0, new EVT(MVT.i32)),
              al, curDAG.getRegister(0, new EVT(MVT.i32)), curDAG.getRegister(0, new EVT(MVT.i32))};
          return curDAG.selectNodeTo(node, opc, new EVT(MVT.i32), ops);
        }
      }
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

  private SDNode selectCMOVOp(SDNode n) {
    EVT vt = n.getValueType(0);
    SDValue falseVal = n.getOperand(0);
    SDValue trueVal = n.getOperand(1);
    SDValue cc = n.getOperand(2);
    SDValue ccr = n.getOperand(3);
    SDValue inflag = n.getOperand(4);
    Util.assertion(cc.getOpcode() == ISD.Constant);
    Util.assertion(ccr.getOpcode() == ISD.Register);

    ARMCC.CondCodes armcc = ARMCC.CondCodes.values()[(int) ((SDNode.ConstantSDNode)cc.getNode()).getZExtValue()];
    if (!subtarget.isThumb1Only() && vt.equals(new EVT(MVT.i32))) {
      // Pattern: (ARMcmov:i32 GPR:i32:$false, so_reg:i32:$true, (imm:i32):$cc)
      // Emits: (MOVCCs:i32 GPR:i32:$false, so_reg:i32:$true, (imm:i32):$cc)
      // Pattern complexity = 18  cost = 1  size = 0
      if (subtarget.isThumb()) {
        SDNode res = selectT2CMOVShiftOp(n, falseVal, trueVal, armcc, ccr, inflag);
        if (res == null)
          res = selectT2CMOVShiftOp(n, trueVal, falseVal, ARMCC.getOppositeCondition(armcc), ccr, inflag);
        if (res != null)
          return res;
      }
      else {
        SDNode res = selectARMCMOVShiftOp(n, falseVal, trueVal, armcc, ccr, inflag);
        if (res == null)
          res = selectARMCMOVShiftOp(n, falseVal, trueVal, ARMCC.getOppositeCondition(armcc), ccr, inflag);
        if (res != null)
          return res;
      }
    }

    // Pattern: (ARMcmov:i32 GPR:i32:$false,
    //             (imm:i32)<<P:Pred_so_imm>>:$true,
    //             (imm:i32):$cc)
    // Emits: (MOVCCi:i32 GPR:i32:$false,
    //           (so_imm:i32 (imm:i32):$true), (imm:i32):$cc)
    // Pattern complexity = 10  cost = 1  size = 0
    if (subtarget.isThumb()) {
      SDNode res = selectT2CMOVImmOp(n, falseVal, trueVal, armcc, ccr, inflag);
      if (res == null)
        res = selectT2CMOVImmOp(n, falseVal, trueVal, ARMCC.getOppositeCondition(armcc), ccr, inflag);
      if (res != null)
        return res;
    }
    else {
      SDNode res = selectARMCMOVImmOp(n, falseVal, trueVal, armcc, ccr, inflag);
      if (res == null)
        res = selectARMCMOVImmOp(n, falseVal, trueVal, ARMCC.getOppositeCondition(armcc), ccr, inflag);
      if (res != null)
        return res;
    }

    // Pattern: (ARMcmov:i32 GPR:i32:$false, GPR:i32:$true, (imm:i32):$cc)
    // Emits: (MOVCCr:i32 GPR:i32:$false, GPR:i32:$true, (imm:i32):$cc)
    // Pattern complexity = 6  cost = 1  size = 0
    //
    // Pattern: (ARMcmov:i32 GPR:i32:$false, GPR:i32:$true, (imm:i32):$cc)
    // Emits: (tMOVCCr:i32 GPR:i32:$false, GPR:i32:$true, (imm:i32):$cc)
    // Pattern complexity = 6  cost = 11  size = 0
    //
    // Also VMOVScc and VMOVDcc.
    SDValue tmp2 = curDAG.getTargetConstant(armcc.ordinal(), new EVT(MVT.i32));
    SDValue[] ops = new SDValue[] {falseVal, trueVal, tmp2, ccr, inflag};
    int opc = 0;
    switch (vt.getSimpleVT().simpleVT) {
      default:
        Util.assertion("Illegal conditional move type!");
      case MVT.i32:
        opc = subtarget.isThumb() ? (subtarget.isThumb2() ?
            ARMGenInstrNames.t2MOVCCr : ARMGenInstrNames.tMOVCCr_pseudo) : ARMGenInstrNames.MOVCCr;
        break;
      case MVT.f32:
        opc = ARMGenInstrNames.VMOVScc;
        break;
      case MVT.f64:
        opc = ARMGenInstrNames.VMOVDcc;
        break;
    }

    return curDAG.selectNodeTo(n, opc, vt, ops);
  }

  private SDNode selectT2CMOVShiftOp(SDNode n, SDValue falseVal, SDValue trueVal,
                                      ARMCC.CondCodes armcc, SDValue ccr, SDValue inflag) {
    SDValue[] tmp = new SDValue[2];
    if (selectT2ShifterOperandReg(trueVal, tmp)) {
      SDValue tmp0 = tmp[0], tmp1 = tmp[1];
      long soVal = ((SDNode.ConstantSDNode)tmp1.getNode()).getZExtValue();
      ARM_AM.ShiftOpc soShOp = ARM_AM.getSORegShOp(soVal);
      int opc = 0;
      switch (soShOp) {
        case lsl: opc = ARMGenInstrNames.t2MOVCClsl; break;
        case lsr: opc = ARMGenInstrNames.t2MOVCClsr; break;
        case asr: opc = ARMGenInstrNames.t2MOVCCasr; break;
        case ror: opc = ARMGenInstrNames.t2MOVCCror; break;
        default:
          Util.shouldNotReachHere("Unknown shifter operator");
          break;
      }
      SDValue soShImm = curDAG.getTargetConstant(ARM_AM.getSORegOffset(soVal), new EVT(MVT.i32));
      SDValue cc = curDAG.getTargetConstant(armcc.ordinal(), new EVT(MVT.i32));
      SDValue[] ops = new SDValue[] {falseVal, tmp0, soShImm, cc, ccr, inflag};
      return curDAG.selectNodeTo(n, opc, new EVT(MVT.i32), ops);
    }
    return null;
  }

  private SDNode selectARMCMOVShiftOp(SDNode n, SDValue falseVal, SDValue trueVal,
                       ARMCC.CondCodes armcc, SDValue ccr, SDValue inflag) {
    SDValue[] tmp = new SDValue[2];
    if (selectImmShifterOperand(trueVal, tmp)) {
      SDValue cpTmp0 = tmp[0], cpTmp2 = tmp[1];
      SDValue cc = curDAG.getTargetConstant(armcc.ordinal(), new EVT(MVT.i32));
      SDValue[] ops = new SDValue[] {falseVal, cpTmp0, cpTmp2, cc, ccr, inflag};
      return curDAG.selectNodeTo(n, ARMGenInstrNames.MOVCCsi, new EVT(MVT.i32), ops);
    }
    SDValue[] tmp1 = new SDValue[3];
    if (selectRegShifterOperand(trueVal, tmp1)) {
      SDValue cpTmp0 = tmp1[0], cpTmp1 = tmp[1], cpTmp2 = tmp1[2];
      SDValue cc = curDAG.getTargetConstant(armcc.ordinal(), new EVT(MVT.i32));
      SDValue[] ops = new SDValue[] {falseVal, cpTmp0, cpTmp1, cpTmp2, cc, ccr, inflag};
      return curDAG.selectNodeTo(n, ARMGenInstrNames.MOVCCsr, new EVT(MVT.i32), ops);
    }
    return null;
  }

  private boolean is_so_imm(long imm) {
    return ARM_AM.getSOImmVal((int) imm) != -1;
  }

  private boolean is_so_imm_not(long imm) {
    return ARM_AM.getSOImmVal((int) ~imm) != -1;
  }

  private boolean is_t2_so_imm(long imm) {
    return ARM_AM.getT2SOImmVal((int) imm) != -1;
  }

  private boolean is_t2_so_imm_not(long imm) {
    return ARM_AM.getT2SOImmVal((int) ~imm) != -1;
  }

  private SDNode selectT2CMOVImmOp(SDNode n, SDValue falseVal, SDValue trueVal,
                                   ARMCC.CondCodes armcc, SDValue ccr, SDValue inflag) {
    if (!(trueVal.getNode() instanceof SDNode.ConstantSDNode))
      return null;

    SDNode.ConstantSDNode t = (SDNode.ConstantSDNode) trueVal.getNode();

    int opc = 0;
    long trueImm = t.getZExtValue();
    if (is_t2_so_imm(trueImm)) {
      opc = ARMGenInstrNames.t2MOVCCi;
    }
    else if (trueImm <= 0xffff) {
      opc = ARMGenInstrNames.t2MOVCCi16;
    }
    else if (is_t2_so_imm_not(trueImm)) {
      trueImm = ~trueImm;
      opc = ARMGenInstrNames.t2MVNCCi;
    }
    else if (trueVal.getNode().hasOneUse() && subtarget.hasV6T2Ops()) {
      // large immediate can be fitted into t2MOVCCi32imm if the armv6 is enabled.
      opc = ARMGenInstrNames.t2MOVCCi32imm;
    }
    if (opc != 0) {
      SDValue trueValue = curDAG.getTargetConstant(trueImm, new EVT(MVT.i32));
      SDValue cc = curDAG.getTargetConstant(armcc.ordinal(), new EVT(MVT.i32));
      SDValue[] ops = {falseVal, trueValue, cc, ccr, inflag};
      return curDAG.selectNodeTo(n, opc, new EVT(MVT.i32), ops);
    }
    return null;
  }

  private SDNode selectARMCMOVImmOp(SDNode n, SDValue falseVal, SDValue trueVal,
                                    ARMCC.CondCodes armcc, SDValue ccr, SDValue inflag) {
    if (!(trueVal.getNode() instanceof SDNode.ConstantSDNode))
      return null;

    SDNode.ConstantSDNode t = (SDNode.ConstantSDNode) trueVal.getNode();

    int opc = 0;
    long trueImm = t.getZExtValue();
    if (is_so_imm(trueImm)) {
      opc = ARMGenInstrNames.MOVCCi;
    }
    else if (trueImm <= 0xffff && subtarget.hasV6T2Ops()) {
      opc = ARMGenInstrNames.t2MOVCCi16;
    }
    else if (is_so_imm_not(trueImm)) {
      trueImm = ~trueImm;
      opc = ARMGenInstrNames.MVNCCi;
    }
    else if (trueVal.getNode().hasOneUse() && (subtarget.hasV6T2Ops() || ARM_AM.isSOImmTwoPartVal((int) trueImm))) {
      // large immediate can be fitted into t2MOVCCi32imm if the armv6 is enabled.
      opc = ARMGenInstrNames.MOVCCi32imm;
    }

    if (opc != 0) {
      SDValue trueValue = curDAG.getTargetConstant(trueImm, new EVT(MVT.i32));
      SDValue cc = curDAG.getTargetConstant(armcc.ordinal(), new EVT(MVT.i32));
      SDValue[] ops = {falseVal, trueValue, cc, ccr, inflag};
      return curDAG.selectNodeTo(n, opc, new EVT(MVT.i32), ops);
    }
    return null;
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
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectRegShifterOperand(SDValue n, SDValue[] tmp) {
    return selectRegShifterOperand(n, tmp, true);
  }

  private boolean isShifterOpProfitable(SDValue shift,
                                               ARM_AM.ShiftOpc shOpc,
                                               long shAmt) {
    if (!subtarget.isCortexA9())
      return true;

    if (shift.hasOneUse())
      return true;

    // R << 2 is free.
    return shOpc == ARM_AM.ShiftOpc.lsl && shAmt == 2;
  }

  protected boolean selectRegShifterOperand(SDValue n, SDValue[] tmp, boolean checkProfitability) {
    if (DisableShifterOp.value)
      return false;

    ARM_AM.ShiftOpc shOpcVal = ARM_AM.getShiftOpcForNode(n.getOpcode());

    // Don't match base register only case. That is matched to a separate
    // lower complexity pattern with explicit register operand.
    if (shOpcVal == ARM_AM.ShiftOpc.no_shift) return false;

    // base register.
    SDValue baseReg = n.getOperand(0);
    int shImmVal = 0;
    if ((n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode))
      return false;

    // shift register.
    SDValue shReg = n.getOperand(1);

    if (checkProfitability && !isShifterOpProfitable(n, shOpcVal, shImmVal))
      return false;
    SDValue opc = curDAG.getTargetConstant(ARM_AM.getSORegShOpc(shOpcVal, shImmVal), new EVT(MVT.i32));
    tmp[0] = baseReg;
    tmp[1] = shReg;
    tmp[2] = opc;
    return true;
  }

  protected boolean selectImmShifterOperand(SDValue n, SDValue[] tmp) {
    return selectImmShifterOperand(n, tmp, true);
  }

  protected boolean selectImmShifterOperand(SDValue n, SDValue[] tmp, boolean checkProfitability) {
    if (DisableShifterOp.value)
      return false;

    ARM_AM.ShiftOpc shOpcVal = ARM_AM.getShiftOpcForNode(n.getOpcode());

    // Don't match base register only case. That is matched to a separate
    // lower complexity pattern with explicit register operand.
    if (shOpcVal == ARM_AM.ShiftOpc.no_shift) return false;

    // base register.
    SDValue baseReg = n.getOperand(0);
    long shImmVal = 0;
    if (!(n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode))
      return false;

    shImmVal = ((SDNode.ConstantSDNode)n.getOperand(1).getNode()).getZExtValue() & 31;
    SDValue opc = curDAG.getTargetConstant(ARM_AM.getSORegShOpc(shOpcVal, shImmVal), new EVT(MVT.i32));
    tmp[0] = baseReg;
    tmp[1] = opc;
    return true;
  }

  protected boolean selectT2ShifterOperandReg(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectShiftRegShifterOperand(SDValue n, SDValue[] tmp) {
    // disable profitability checking.
    return selectRegShifterOperand(n, tmp, false);
  }

  protected boolean selectShiftImmShifterOperand(SDValue n, SDValue[] tmp) {
    // disable profitability checking.
    return selectImmShifterOperand(n, tmp, false);
  }

  /**
   * Check whether a particular node is a constant value representable as
   * (N * Scale) where (N in [{@code rangeMin}, {@code rangeMax}).
   * @param node
   * @param scale
   * @param rangeMin
   * @param rangeMax
   * @param scaledConstant
   * @return
   */
  private static boolean isScaledConstantInRange(SDValue node,
                                                 int scale,
                                                 int rangeMin,
                                                 int rangeMax,
                                                 OutRef<Integer> scaledConstant) {
    Util.assertion(scale >0, "invalid scale");
    if (!(node.getNode() instanceof SDNode.ConstantSDNode))
      return false;

    SDNode.ConstantSDNode cst = (SDNode.ConstantSDNode) node.getNode();
    long c = cst.getZExtValue();
    if ((c % scale) != 0)
      return false;

    c /= scale;
    return c >= rangeMin && c < rangeMax;
  }

  protected boolean selectLdStSOReg(SDValue n, SDValue[] tmp) {
    if (n.getOpcode() == ISD.MUL && (!subtarget.isCortexA9() || n.hasOneUse())) {
      if (n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
        SDNode.ConstantSDNode cstRHS = (SDNode.ConstantSDNode) n.getOperand(1).getNode();
        long rhsc = cstRHS.getZExtValue();
        if ((rhsc & 1) != 0) {
          rhsc = rhsc & ~1;
          ARM_AM.AddrOpc addsub = ARM_AM.AddrOpc.add;
          if (rhsc < 0) {
            addsub = ARM_AM.AddrOpc.sub;
            rhsc = -rhsc;
          }
          if (Util.isPowerOf2(rhsc)) {
            int shAmt = Util.log2(rhsc);
            // base = offset = n.getOperand(0);
            tmp[0] = tmp[1] = n.getOperand(0);
            // opc
            tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM2Opc(addsub, shAmt, ARM_AM.ShiftOpc.lsl), new EVT(MVT.i32));
            return true;
          }
        }
      }
    }

    if (n.getOpcode() != ISD.ADD && n.getOpcode() != ISD.SUB &&
        !curDAG.isBaseWithConstantOffset(n)) {
      return false;
    }

    if (n.getOpcode() == ISD.ADD || n.getOpcode() == ISD.OR) {
      // simple R +- imm12 for LDRi12.
      OutRef<Integer> rhsc = new OutRef<>(0);
      if (isScaledConstantInRange(n.getOperand(1), /*scale*/ 1, -0x100+1, 0x1000, rhsc))
        return false;
    }

    // otherwise R +/- [possibly shifted] R.
    ARM_AM.AddrOpc addsub = n.getOpcode() == ISD.ADD ? ARM_AM.AddrOpc.add : ARM_AM.AddrOpc.sub;
    ARM_AM.ShiftOpc shOpc = ARM_AM.getShiftOpcForNode(n.getOperand(1).getOpcode());
    long shAmt = 0;

    // base
    SDValue base = n.getOperand(0);
    // offset
    SDValue offset = n.getOperand(1);
    if (shOpc != ARM_AM.ShiftOpc.no_shift) {
      // check to see if the rhs of the shift is a constant, if it is not, we can't fold.
      if (n.getOperand(1).getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
        SDNode.ConstantSDNode sh = (SDNode.ConstantSDNode) n.getOperand(1).getOperand(1).getNode();
        shAmt = sh.getZExtValue();
        if (isShifterOpProfitable(offset, shOpc, shAmt))
          offset = n.getOperand(1).getOperand(0);
        else {
          shAmt = 0;
          shOpc = ARM_AM.ShiftOpc.no_shift;
        }
      }
      else
        shOpc = ARM_AM.ShiftOpc.no_shift;
    }

    // try matching (R shl C) + (R).
    if (n.getOpcode() != ISD.SUB && shOpc == ARM_AM.ShiftOpc.no_shift &&
      !(subtarget.isCortexA9() || n.getOperand(0).hasOneUse())) {
      shOpc = ARM_AM.getShiftOpcForNode(n.getOperand(0).getOpcode());
      if (shOpc != ARM_AM.ShiftOpc.no_shift) {
        // check to see if the rhs of the shift is a constant, if it is not, don't fold.
        if (n.getOperand(0).getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
          SDNode.ConstantSDNode sh = (SDNode.ConstantSDNode) n.getOperand(0).getOperand(1).getNode();
          shAmt = sh.getZExtValue();
          if (isShifterOpProfitable(n.getOperand(0), shOpc, shAmt)) {
            offset = n.getOperand(0).getOperand(0);
            base = n.getOperand(1);
          }
          else {
            shAmt = 0;
            shOpc = ARM_AM.ShiftOpc.no_shift;
          }
        }
        else
          shOpc = ARM_AM.ShiftOpc.no_shift;
      }
    }
    tmp[0] = base;
    tmp[1] = offset;
    // opc
    tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM2Opc(addsub, (int) shAmt, shOpc), new EVT(MVT.i32));
    return true;
  }

  protected boolean selectT2AddrModeSoReg(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
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
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectT2AddrModeImm8(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectAddrMode2(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectAddrMode6(SDNode parent, SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectAddrMode6Offset(SDNode op, SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectAddrModePC(SDValue n, SDValue[] tmp) {
    if (n.getOpcode() == ARMISD.PIC_ADD && n.hasOneUse()) {
      // offset
      tmp[0] = n.getOperand(0);
      SDValue n1 = n.getOperand(1);
      // label.
      tmp[1] = curDAG.getTargetConstant(((SDNode.ConstantSDNode)n1.getNode()).getZExtValue(), new EVT(MVT.i32));
      return true;
    }
    return false;
  }

  protected boolean selectAddrOffsetNone(SDValue n, SDValue[] tmp) {
    // tmp[0] = base
    tmp[0] = n;
    return true;
  }

  protected boolean selectAddrMode3Offset(SDNode op, SDValue n, SDValue[] tmp) {
    int opc = op.getOpcode();
    MemIndexedMode am = opc == ISD.LOAD ? ((SDNode.LoadSDNode)n.getNode()).getAddressingMode() :
        ((SDNode.StoreSDNode)n.getNode()).getAddressingMode();
    ARM_AM.AddrOpc addsub = am == MemIndexedMode.PRE_INC || am == MemIndexedMode.POST_INC ?
        ARM_AM.AddrOpc.add : ARM_AM.AddrOpc.sub;
    // tmp[0] = offset
    tmp[0] = n;
    // tmp[1] = opc.
    tmp[1] = curDAG.getTargetConstant(ARM_AM.getAM3Opc(addsub, 0), new EVT(MVT.i32));
    return true;
  }

  protected boolean selectAddrMode2OffsetReg(SDNode op, SDValue n, SDValue[] tmp) {
    int opc = op.getOpcode();
    MemIndexedMode am = opc == ISD.LOAD ? ((SDNode.LoadSDNode)n.getNode()).getAddressingMode() :
        ((SDNode.StoreSDNode)n.getNode()).getAddressingMode();
    ARM_AM.AddrOpc addsub = am == MemIndexedMode.PRE_INC || am == MemIndexedMode.POST_INC ?
        ARM_AM.AddrOpc.add : ARM_AM.AddrOpc.sub;

    OutRef<Integer> val = new OutRef<>(0);
    // 12 bit, [0, 2^12)
    if (isScaledConstantInRange(n, 1, 0, 0x1000, val)) {
      if (addsub == ARM_AM.AddrOpc.sub)
        val.set(-val.get());
      // tmp[0] = offset
      tmp[0] = curDAG.getRegister(0, new EVT(MVT.i32));
      // tmp[1] = opc.
      tmp[1] = curDAG.getTargetConstant(val.get(), new EVT(MVT.i32));
      return true;
    }
    return false;
  }

  protected boolean selectAddrMode2OffsetImm(SDNode op, SDValue n, SDValue[] tmp) {
    int opc = op.getOpcode();
    MemIndexedMode am = opc == ISD.LOAD ? ((SDNode.LoadSDNode)n.getNode()).getAddressingMode() :
        ((SDNode.StoreSDNode)n.getNode()).getAddressingMode();
    ARM_AM.AddrOpc addsub = am == MemIndexedMode.PRE_INC || am == MemIndexedMode.POST_INC ?
        ARM_AM.AddrOpc.add : ARM_AM.AddrOpc.sub;

    OutRef<Integer> val = new OutRef<>(0);
    // 12 bit, [0, 2^12)
    if (isScaledConstantInRange(n, 1, 0, 0x1000, val)) {
      // tmp[0] = offset
      tmp[0] = curDAG.getRegister(0, new EVT(MVT.i32));
      // tmp[1] = opc.
      tmp[1] = curDAG.getTargetConstant(ARM_AM.getAM2Opc(addsub, val.get(),
          ARM_AM.ShiftOpc.no_shift), new EVT(MVT.i32));
      return true;
    }
    return false;
  }

  protected boolean selectAddrMode3(SDValue n, SDValue[] tmp) {
    // Match the address mode 3.
    // tmp[0] = base, tmp[1] = offset , tmp[2] = opc.
    if (n.getOpcode() == ISD.SUB) {
      tmp[0] = n.getOperand(0);
      tmp[1] = n.getOperand(1);
      tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM3Opc(ARM_AM.AddrOpc.sub, 0), new EVT(MVT.i32));
      return true;
    }

    if (!curDAG.isBaseWithConstantOffset(n)) {
      tmp[0] = n;
      if (n.getOpcode() == ISD.FrameIndex) {
        int fi = ((SDNode.FrameIndexSDNode)n.getNode()).getFrameIndex();
        tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
      }
      tmp[1] = curDAG.getRegister(0, new EVT(MVT.i32));
      tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM3Opc(ARM_AM.AddrOpc.add, 0), new EVT(MVT.i32));
      return true;
    }
    // If the RHS is +/- imm8, fold into addr mode.
    OutRef<Integer> rhsc = new OutRef<>(0);
    if (isScaledConstantInRange(n.getOperand(1), /*scale*/1, -256 + 1, 256, rhsc)) {
      tmp[0] = n.getOperand(0);
      if (tmp[0].getOpcode() == ISD.FrameIndex) {
        int fi = ((SDNode.FrameIndexSDNode)tmp[0].getNode()).getFrameIndex();
        tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
      }
      tmp[1] = curDAG.getRegister(0, new EVT(MVT.i32));
      ARM_AM.AddrOpc addsub = ARM_AM.AddrOpc.add;
      if (rhsc.get() < 0) {
        rhsc.set(-rhsc.get());
        addsub = ARM_AM.AddrOpc.sub;
      }
      tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM3Opc(addsub, rhsc.get()), new EVT(MVT.i32));
      return true;
    }

    tmp[0] = n.getOperand(0);
    tmp[1] = n.getOperand(1);
    tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM3Opc(ARM_AM.AddrOpc.add, 0), new EVT(MVT.i32));
    return true;
  }

  protected boolean selectT2AddrModeImm8Offset(SDNode op, SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectThumbAddrModeRI5S4(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectThumbAddrModeImm5S4(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectThumbAddrModeRI5S1(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectThumbAddrModeImm5S1(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectThumbAddrModeRI5S2(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectThumbAddrModeImm5S2(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectThumbAddrModeSP(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectAddrMode5(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  protected boolean selectThumbAddrModeRR(SDValue n, SDValue[] tmp) {
    Util.shouldNotReachHere();
    return false;
  }

  static FunctionPass createARMISelDAG(ARMTargetMachine tm, TargetMachine.CodeGenOpt level) {
    return new ARMGenDAGISel(tm, level);
  }
}
