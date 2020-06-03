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

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.MachineMemOperand;
import backend.codegen.dagisel.*;
import backend.debug.DebugLoc;
import backend.mc.MCInstrDesc;
import backend.pass.FunctionPass;
import backend.pass.Pass;
import backend.pass.RegisterPass;
import backend.target.TargetMachine;
import backend.target.TargetOpcode;
import backend.type.Type;
import backend.value.ConstantInt;
import tools.OutRef;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHidden;
import tools.commandline.OptionHiddenApplicator;

import java.util.ArrayList;

import static backend.intrinsic.Intrinsic.ID.values;
import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * The detailed document of ARM addressing modes can be viewed from the following file.
 * the chapter A5 of ARM Architecture Reference Manual (2005).
 *
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

    subtarget = tm.getSubtarget();
    tli = subtarget.getTargetLowering();
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
        // ARM_CC.CondCodes
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
        int fi = ((SDNode.FrameIndexSDNode) node).getFrameIndex();
        SDValue fin = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
        SDValue al = curDAG.getTargetConstant(ARMCC.CondCodes.AL.ordinal(), new EVT(MVT.i32));
        if (subtarget.isThumb1Only()) {
          SDValue[] ops = {fin, curDAG.getTargetConstant(0, new EVT(MVT.i32)),
              al, curDAG.getRegister(0, new EVT(MVT.i32)), curDAG.getRegister(0, new EVT(MVT.i32))};
          return curDAG.selectNodeTo(node, ARMGenInstrNames.tADDrSPi, new EVT(MVT.i32), ops);
        } else {
          int opc = subtarget.isThumb() && subtarget.hasThumb2() ? ARMGenInstrNames.t2ADDri :
              ARMGenInstrNames.ADDri;
          SDValue[] ops = {fin, curDAG.getTargetConstant(0, new EVT(MVT.i32)),
              al, curDAG.getRegister(0, new EVT(MVT.i32)), curDAG.getRegister(0, new EVT(MVT.i32))};
          return curDAG.selectNodeTo(node, opc, new EVT(MVT.i32), ops);
        }
      }
      case ISD.XOR: {
        // Select special operations if XOR node forms integer ABS pattern
        SDNode resNode = selectABSOp(node);
        if (resNode != null)
          return resNode;
        // Other cases are autogenerated.
        break;
      }
      case ISD.Constant: {
        int Val = (int) ((SDNode.ConstantSDNode) node).getZExtValue();
        boolean UseCP = true;
        if (subtarget.hasThumb2())
          // Thumb2-aware targets have the MOVT instruction, so all immediates can
          // be done with MOV + MOVT, at worst.
          UseCP = false;
        else {
          if (subtarget.isThumb()) {
            UseCP = (Val > 255 &&                          // MOV
                ~Val > 255 &&                         // MOV + MVN
                !ARM_AM.isThumbImmShiftedVal(Val));  // MOV + LSL
          } else
            UseCP = (ARM_AM.getSOImmVal(Val) == -1 &&     // MOV
                ARM_AM.getSOImmVal(~Val) == -1 &&    // MVN
                !ARM_AM.isSOImmTwoPartVal(Val));     // two instrs.
        }

        if (UseCP) {
          SDValue CPIdx = curDAG.getTargetConstantPool(ConstantInt.get(
              Type.getInt32Ty(curDAG.getContext()), Val), new EVT(tli.getPointerTy()), 0, 0, 0);

          SDNode ResNode;
          if (subtarget.isThumb1Only()) {
            SDValue Pred = getAL(curDAG);
            SDValue PredReg = curDAG.getRegister(0, MVT.i32);
            SDValue ops[] = {CPIdx, Pred, PredReg, curDAG.getEntryNode()};
            ResNode = curDAG.getMachineNode(ARMGenInstrNames.tLDRpci, curDAG.getVTList(new EVT(MVT.i32), new EVT(MVT.Other)), ops);
          } else {
            SDValue ops[] = {
                CPIdx,
                curDAG.getTargetConstant(0, new EVT(MVT.i32)),
                getAL(curDAG),
                curDAG.getRegister(0, MVT.i32),
                curDAG.getEntryNode()
            };
            ResNode = curDAG.getMachineNode(ARMGenInstrNames.LDRcp, curDAG.getVTList(new EVT(MVT.i32), new EVT(MVT.Other)),
                ops);
          }
          replaceUses(new SDValue(node, 0), new SDValue(ResNode, 0));
          return null;
        }

        // Other cases are autogenerated.
        break;
      }
      case ISD.SRL: {
        SDNode res = selectV6T2BitfieldExtractOp(node, false);
        if (res != null)
          return res;
        break;
      }
      case ISD.SRA: {
        SDNode res = selectV6T2BitfieldExtractOp(node, true);
        if (res != null)
          return res;
        break;
      }
      case ISD.MUL:
        if (subtarget.isThumb1Only())
          break;
        if (node.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
          SDNode.ConstantSDNode c = (SDNode.ConstantSDNode) node.getOperand(1).getNode();
          int rhsv = (int) c.getZExtValue();
          if (rhsv == 0) break;
          if (Util.isPowerOf2(rhsv - 1)) {  // 2^n+1?
            int shImm = Util.log2(rhsv - 1);
            if (shImm >= 32)
              break;
            SDValue v = node.getOperand(0);
            shImm = (int) ARM_AM.getSORegOpc(ARM_AM.ShiftOpc.lsl, shImm);
            SDValue shImmOp = curDAG.getTargetConstant(shImm, new EVT(MVT.i32));
            SDValue reg0 = curDAG.getRegister(0, MVT.i32);
            if (subtarget.isThumb()) {
              SDValue[] ops = {v, v, shImmOp, getAL(curDAG), reg0, reg0};
              return curDAG.selectNodeTo(node, ARMGenInstrNames.t2ADDrs, new EVT(MVT.i32), ops);
            } else {
              SDValue[] ops = {v, v, shImmOp, getAL(curDAG), reg0, reg0};
              return curDAG.selectNodeTo(node, ARMGenInstrNames.ADDrsi, new EVT(MVT.i32), ops);
            }
          }
          if (Util.isPowerOf2(rhsv + 1)) {  // 2^n-1?
            int ShImm = Util.log2(rhsv + 1);
            if (ShImm >= 32)
              break;
            SDValue V = node.getOperand(0);
            ShImm = (int) ARM_AM.getSORegOpc(ARM_AM.ShiftOpc.lsl, ShImm);
            SDValue ShImmOp = curDAG.getTargetConstant(ShImm, new EVT(MVT.i32));
            SDValue Reg0 = curDAG.getRegister(0, MVT.i32);
            if (subtarget.isThumb()) {
              SDValue ops[] = {V, V, ShImmOp, getAL(curDAG), Reg0, Reg0};
              return curDAG.selectNodeTo(node, ARMGenInstrNames.t2RSBrs, new EVT(MVT.i32), ops);
            } else {
              SDValue ops[] = {V, V, ShImmOp, getAL(curDAG), Reg0, Reg0};
              return curDAG.selectNodeTo(node, ARMGenInstrNames.RSBrsi, new EVT(MVT.i32), ops);
            }
          }
        }
        break;
      case ISD.AND: {
        // Check for int bitfield extract
        SDNode res = selectV6T2BitfieldExtractOp(node, false);
        if (res != null)
          return res;

        // (and (or x, c2), c1) and top 16-bits of c1 and c2 match, lower 16-bits
        // of c1 are 0xffff, and lower 16-bit of c2 are 0. That is, the top 16-bits
        // are entirely contributed by c2 and lower 16-bits are entirely contributed
        // by x. That's equal to (or (and x, 0xffff), (and c1, 0xffff0000)).
        // Select it to: "movt x, ((c1 & 0xffff) >> 16)
        EVT vt = node.getValueType(0);
        if (!vt.equals(new EVT(MVT.i32)))
          break;
        int opc = (subtarget.isThumb() && subtarget.hasThumb2())
            ? ARMGenInstrNames.t2MOVTi16
            : (subtarget.hasV6T2Ops() ? ARMGenInstrNames.MOVTi16 : 0);
        if (opc == 0)
          break;
        SDValue n0 = node.getOperand(0), n1 = node.getOperand(1);
        if (!(n1.getNode() instanceof SDNode.ConstantSDNode))
          break;

        SDNode.ConstantSDNode n1c = (SDNode.ConstantSDNode) n1.getNode();
        if (n0.getOpcode() == ISD.OR && n0.getNode().hasOneUse()) {
          SDValue n2 = n0.getOperand(1);
          if (!(n2.getNode() instanceof SDNode.ConstantSDNode))
            break;

          SDNode.ConstantSDNode n2c = (SDNode.ConstantSDNode) n2.getNode();

          int N1CVal = (int) n1c.getZExtValue();
          int N2CVal = (int) n2c.getZExtValue();
          if ((N1CVal & 0xffff0000) == (N2CVal & 0xffff0000) &&
              (N1CVal & 0xffff) == 0xffff &&
              (N2CVal & 0xffff) == 0x0) {
            SDValue Imm16 = curDAG.getTargetConstant((N2CVal & 0xFFFF0000) >> 16, new EVT(MVT.i32));
            SDValue ops[] = {n0.getOperand(0), Imm16,
                getAL(curDAG), curDAG.getRegister(0, MVT.i32)};
            return curDAG.getMachineNode(opc, vt, ops);
          }
        }
        break;
      }
      case ARMISD.VMOVRRD:
        return curDAG.getMachineNode(ARMGenInstrNames.VMOVRRD, new EVT(MVT.i32), new EVT(MVT.i32),
            node.getOperand(0), getAL(curDAG),
            curDAG.getRegister(0, MVT.i32));
      case ISD.UMUL_LOHI: {
        if (subtarget.isThumb1Only())
          break;
        if (subtarget.isThumb()) {
          SDValue ops[] = {node.getOperand(0), node.getOperand(1),
              getAL(curDAG), curDAG.getRegister(0, MVT.i32),
              curDAG.getRegister(0, MVT.i32)};
          return curDAG.getMachineNode(ARMGenInstrNames.t2UMULL, new EVT(MVT.i32), new EVT(MVT.i32), ops);
        } else {
          SDValue ops[] = {node.getOperand(0), node.getOperand(1),
              getAL(curDAG), curDAG.getRegister(0, MVT.i32),
              curDAG.getRegister(0, MVT.i32)};
          return curDAG.getMachineNode(subtarget.hasV6Ops() ?
              ARMGenInstrNames.UMULL : ARMGenInstrNames.UMULLv5, new EVT(MVT.i32), new EVT(MVT.i32), ops);
        }
      }
      case ISD.SMUL_LOHI: {
        if (subtarget.isThumb1Only())
          break;
        if (subtarget.isThumb()) {
          SDValue ops[] = {node.getOperand(0), node.getOperand(1),
              getAL(curDAG), curDAG.getRegister(0, MVT.i32)};
          return curDAG.getMachineNode(ARMGenInstrNames.t2SMULL, new EVT(MVT.i32), new EVT(MVT.i32), ops);
        } else {
          SDValue ops[] = {node.getOperand(0), node.getOperand(1),
              getAL(curDAG), curDAG.getRegister(0, MVT.i32),
              curDAG.getRegister(0, MVT.i32)};
          return curDAG.getMachineNode(subtarget.hasV6Ops() ?
              ARMGenInstrNames.SMULL : ARMGenInstrNames.SMULLv5, new EVT(MVT.i32), new EVT(MVT.i32), ops);
        }
      }
      case ISD.LOAD: {
        SDNode resNode = null;
        if (subtarget.isThumb() && subtarget.hasThumb2())
          resNode = selectT2IndexedLoad(node);
        else
          resNode = selectARMIndexedLoad(node);
        if (resNode != null)
          return resNode;
        // Other cases are autogenerated.
        break;
      }
      case ARMISD.VZIP: {
        int opc = 0;
        EVT vt = node.getValueType(0);
        switch (vt.getSimpleVT().simpleVT) {
          default:
            return null;
          case MVT.v8i8:
            opc = ARMGenInstrNames.VZIPd8;
            break;
          case MVT.v4i16:
            opc = ARMGenInstrNames.VZIPd16;
            break;
          case MVT.v2f32:
          case MVT.v2i32:
            opc = ARMGenInstrNames.VZIPd32;
            break;
          case MVT.v16i8:
            opc = ARMGenInstrNames.VZIPq8;
            break;
          case MVT.v8i16:
            opc = ARMGenInstrNames.VZIPq16;
            break;
          case MVT.v4f32:
          case MVT.v4i32:
            opc = ARMGenInstrNames.VZIPq32;
            break;
        }
        SDValue pred = getAL(curDAG);
        SDValue predReg = curDAG.getRegister(0, MVT.i32);
        SDValue ops[] = {node.getOperand(0), node.getOperand(1), pred, predReg};
        return curDAG.getMachineNode(opc, vt, vt, ops);
      }
      case ARMISD.VUZP: {
        int opc = 0;
        EVT VT = node.getValueType(0);
        switch (VT.getSimpleVT().simpleVT) {
          default:
            return null;
          case MVT.v8i8:
            opc = ARMGenInstrNames.VUZPd8;
            break;
          case MVT.v4i16:
            opc = ARMGenInstrNames.VUZPd16;
            break;
          case MVT.v2f32:
          case MVT.v2i32:
            opc = ARMGenInstrNames.VUZPd32;
            break;
          case MVT.v16i8:
            opc = ARMGenInstrNames.VUZPq8;
            break;
          case MVT.v8i16:
            opc = ARMGenInstrNames.VUZPq16;
            break;
          case MVT.v4f32:
          case MVT.v4i32:
            opc = ARMGenInstrNames.VUZPq32;
            break;
        }
        SDValue Pred = getAL(curDAG);
        SDValue PredReg = curDAG.getRegister(0, MVT.i32);
        SDValue ops[] = {node.getOperand(0), node.getOperand(1), Pred, PredReg};
        return curDAG.getMachineNode(opc, VT, VT, ops);
      }
      case ARMISD.VTRN: {
        int opc = 0;
        EVT VT = node.getValueType(0);
        switch (VT.getSimpleVT().simpleVT) {
          default:
            return null;
          case MVT.v8i8:
            opc = ARMGenInstrNames.VTRNd8;
            break;
          case MVT.v4i16:
            opc = ARMGenInstrNames.VTRNd16;
            break;
          case MVT.v2f32:
          case MVT.v2i32:
            opc = ARMGenInstrNames.VTRNd32;
            break;
          case MVT.v16i8:
            opc = ARMGenInstrNames.VTRNq8;
            break;
          case MVT.v8i16:
            opc = ARMGenInstrNames.VTRNq16;
            break;
          case MVT.v4f32:
          case MVT.v4i32:
            opc = ARMGenInstrNames.VTRNq32;
            break;
        }
        SDValue Pred = getAL(curDAG);
        SDValue PredReg = curDAG.getRegister(0, MVT.i32);
        SDValue ops[] = {node.getOperand(0), node.getOperand(1), Pred, PredReg};
        return curDAG.getMachineNode(opc, VT, VT, ops);
      }
      case ARMISD.BUILD_VECTOR: {
        EVT VecVT = node.getValueType(0);
        EVT EltVT = VecVT.getVectorElementType();
        int NumElts = VecVT.getVectorNumElements();
        if (EltVT.equals(new EVT(MVT.f64))) {
          Util.assertion(NumElts == 2, "unexpected type for BUILD_VECTOR");
          return pairDRegs(VecVT, node.getOperand(0), node.getOperand(1));
        }
        Util.assertion(EltVT.equals(new EVT(MVT.f32)), "unexpected type for BUILD_VECTOR");
        if (NumElts == 2)
          return pairSRegs(VecVT, node.getOperand(0), node.getOperand(1));
        Util.assertion(NumElts == 4, "unexpected type for BUILD_VECTOR");
        return quadSRegs(VecVT, node.getOperand(0), node.getOperand(1),
            node.getOperand(2), node.getOperand(3));
      }

      case ARMISD.VLD2DUP: {
        int Opcodes[] = {ARMGenInstrNames.VLD2DUPd8Pseudo, ARMGenInstrNames.VLD2DUPd16Pseudo,
            ARMGenInstrNames.VLD2DUPd32Pseudo};
        return selectVLDDup(node, false, 2, Opcodes);
      }

      case ARMISD.VLD3DUP: {
        int Opcodes[] = {ARMGenInstrNames.VLD3DUPd8Pseudo, ARMGenInstrNames.VLD3DUPd16Pseudo,
            ARMGenInstrNames.VLD3DUPd32Pseudo};
        return selectVLDDup(node, false, 3, Opcodes);
      }

      case ARMISD.VLD4DUP: {
        int Opcodes[] = {ARMGenInstrNames.VLD4DUPd8Pseudo, ARMGenInstrNames.VLD4DUPd16Pseudo,
            ARMGenInstrNames.VLD4DUPd32Pseudo};
        return selectVLDDup(node, false, 4, Opcodes);
      }

      case ARMISD.VLD2DUP_UPD: {
        int Opcodes[] = {ARMGenInstrNames.VLD2DUPd8Pseudo_UPD, ARMGenInstrNames.VLD2DUPd16Pseudo_UPD,
            ARMGenInstrNames.VLD2DUPd32Pseudo_UPD};
        return selectVLDDup(node, true, 2, Opcodes);
      }

      case ARMISD.VLD3DUP_UPD: {
        int Opcodes[] = {ARMGenInstrNames.VLD3DUPd8Pseudo_UPD, ARMGenInstrNames.VLD3DUPd16Pseudo_UPD,
            ARMGenInstrNames.VLD3DUPd32Pseudo_UPD};
        return selectVLDDup(node, true, 3, Opcodes);
      }

      case ARMISD.VLD4DUP_UPD: {
        int Opcodes[] = {ARMGenInstrNames.VLD4DUPd8Pseudo_UPD, ARMGenInstrNames.VLD4DUPd16Pseudo_UPD,
            ARMGenInstrNames.VLD4DUPd32Pseudo_UPD};
        return selectVLDDup(node, true, 4, Opcodes);
      }

      case ARMISD.VLD1_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VLD1d8_UPD, ARMGenInstrNames.VLD1d16_UPD,
            ARMGenInstrNames.VLD1d32_UPD, ARMGenInstrNames.VLD1d64_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VLD1q8Pseudo_UPD, ARMGenInstrNames.VLD1q16Pseudo_UPD,
            ARMGenInstrNames.VLD1q32Pseudo_UPD, ARMGenInstrNames.VLD1q64Pseudo_UPD};
        return selectVLD(node, true, 1, DOpcodes, QOpcodes, null);
      }

      case ARMISD.VLD2_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VLD2d8Pseudo_UPD, ARMGenInstrNames.VLD2d16Pseudo_UPD,
            ARMGenInstrNames.VLD2d32Pseudo_UPD, ARMGenInstrNames.VLD1q64Pseudo_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VLD2q8Pseudo_UPD, ARMGenInstrNames.VLD2q16Pseudo_UPD,
            ARMGenInstrNames.VLD2q32Pseudo_UPD};
        return selectVLD(node, true, 2, DOpcodes, QOpcodes, null);
      }

      case ARMISD.VLD3_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VLD3d8Pseudo_UPD, ARMGenInstrNames.VLD3d16Pseudo_UPD,
            ARMGenInstrNames.VLD3d32Pseudo_UPD, ARMGenInstrNames.VLD1d64TPseudo_UPD};
        int QOpcodes0[] = {ARMGenInstrNames.VLD3q8Pseudo_UPD,
            ARMGenInstrNames.VLD3q16Pseudo_UPD,
            ARMGenInstrNames.VLD3q32Pseudo_UPD};
        int QOpcodes1[] = {ARMGenInstrNames.VLD3q8oddPseudo_UPD,
            ARMGenInstrNames.VLD3q16oddPseudo_UPD,
            ARMGenInstrNames.VLD3q32oddPseudo_UPD};
        return selectVLD(node, true, 3, DOpcodes, QOpcodes0, QOpcodes1);
      }

      case ARMISD.VLD4_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VLD4d8Pseudo_UPD, ARMGenInstrNames.VLD4d16Pseudo_UPD,
            ARMGenInstrNames.VLD4d32Pseudo_UPD, ARMGenInstrNames.VLD1d64QPseudo_UPD};
        int QOpcodes0[] = {ARMGenInstrNames.VLD4q8Pseudo_UPD,
            ARMGenInstrNames.VLD4q16Pseudo_UPD,
            ARMGenInstrNames.VLD4q32Pseudo_UPD};
        int QOpcodes1[] = {ARMGenInstrNames.VLD4q8oddPseudo_UPD,
            ARMGenInstrNames.VLD4q16oddPseudo_UPD,
            ARMGenInstrNames.VLD4q32oddPseudo_UPD};
        return selectVLD(node, true, 4, DOpcodes, QOpcodes0, QOpcodes1);
      }

      case ARMISD.VLD2LN_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VLD2LNd8Pseudo_UPD, ARMGenInstrNames.VLD2LNd16Pseudo_UPD,
            ARMGenInstrNames.VLD2LNd32Pseudo_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VLD2LNq16Pseudo_UPD,
            ARMGenInstrNames.VLD2LNq32Pseudo_UPD};
        return selectVLDSTLane(node, true, true, 2, DOpcodes, QOpcodes);
      }

      case ARMISD.VLD3LN_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VLD3LNd8Pseudo_UPD, ARMGenInstrNames.VLD3LNd16Pseudo_UPD,
            ARMGenInstrNames.VLD3LNd32Pseudo_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VLD3LNq16Pseudo_UPD,
            ARMGenInstrNames.VLD3LNq32Pseudo_UPD};
        return selectVLDSTLane(node, true, true, 3, DOpcodes, QOpcodes);
      }

      case ARMISD.VLD4LN_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VLD4LNd8Pseudo_UPD, ARMGenInstrNames.VLD4LNd16Pseudo_UPD,
            ARMGenInstrNames.VLD4LNd32Pseudo_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VLD4LNq16Pseudo_UPD,
            ARMGenInstrNames.VLD4LNq32Pseudo_UPD};
        return selectVLDSTLane(node, true, true, 4, DOpcodes, QOpcodes);
      }

      case ARMISD.VST1_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VST1d8_UPD, ARMGenInstrNames.VST1d16_UPD,
            ARMGenInstrNames.VST1d32_UPD, ARMGenInstrNames.VST1d64_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VST1q8Pseudo_UPD, ARMGenInstrNames.VST1q16Pseudo_UPD,
            ARMGenInstrNames.VST1q32Pseudo_UPD, ARMGenInstrNames.VST1q64Pseudo_UPD};
        return selectVST(node, true, 1, DOpcodes, QOpcodes, null);
      }

      case ARMISD.VST2_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VST2d8Pseudo_UPD, ARMGenInstrNames.VST2d16Pseudo_UPD,
            ARMGenInstrNames.VST2d32Pseudo_UPD, ARMGenInstrNames.VST1q64Pseudo_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VST2q8Pseudo_UPD, ARMGenInstrNames.VST2q16Pseudo_UPD,
            ARMGenInstrNames.VST2q32Pseudo_UPD};
        return selectVST(node, true, 2, DOpcodes, QOpcodes, null);
      }

      case ARMISD.VST3_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VST3d8Pseudo_UPD, ARMGenInstrNames.VST3d16Pseudo_UPD,
            ARMGenInstrNames.VST3d32Pseudo_UPD, ARMGenInstrNames.VST1d64TPseudo_UPD};
        int QOpcodes0[] = {ARMGenInstrNames.VST3q8Pseudo_UPD,
            ARMGenInstrNames.VST3q16Pseudo_UPD,
            ARMGenInstrNames.VST3q32Pseudo_UPD};
        int QOpcodes1[] = {ARMGenInstrNames.VST3q8oddPseudo_UPD,
            ARMGenInstrNames.VST3q16oddPseudo_UPD,
            ARMGenInstrNames.VST3q32oddPseudo_UPD};
        return selectVST(node, true, 3, DOpcodes, QOpcodes0, QOpcodes1);
      }

      case ARMISD.VST4_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VST4d8Pseudo_UPD, ARMGenInstrNames.VST4d16Pseudo_UPD,
            ARMGenInstrNames.VST4d32Pseudo_UPD, ARMGenInstrNames.VST1d64QPseudo_UPD};
        int QOpcodes0[] = {ARMGenInstrNames.VST4q8Pseudo_UPD,
            ARMGenInstrNames.VST4q16Pseudo_UPD,
            ARMGenInstrNames.VST4q32Pseudo_UPD};
        int QOpcodes1[] = {ARMGenInstrNames.VST4q8oddPseudo_UPD,
            ARMGenInstrNames.VST4q16oddPseudo_UPD,
            ARMGenInstrNames.VST4q32oddPseudo_UPD};
        return selectVST(node, true, 4, DOpcodes, QOpcodes0, QOpcodes1);
      }

      case ARMISD.VST2LN_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VST2LNd8Pseudo_UPD, ARMGenInstrNames.VST2LNd16Pseudo_UPD,
            ARMGenInstrNames.VST2LNd32Pseudo_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VST2LNq16Pseudo_UPD,
            ARMGenInstrNames.VST2LNq32Pseudo_UPD};
        return selectVLDSTLane(node, false, true, 2, DOpcodes, QOpcodes);
      }

      case ARMISD.VST3LN_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VST3LNd8Pseudo_UPD, ARMGenInstrNames.VST3LNd16Pseudo_UPD,
            ARMGenInstrNames.VST3LNd32Pseudo_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VST3LNq16Pseudo_UPD,
            ARMGenInstrNames.VST3LNq32Pseudo_UPD};
        return selectVLDSTLane(node, false, true, 3, DOpcodes, QOpcodes);
      }

      case ARMISD.VST4LN_UPD: {
        int DOpcodes[] = {ARMGenInstrNames.VST4LNd8Pseudo_UPD, ARMGenInstrNames.VST4LNd16Pseudo_UPD,
            ARMGenInstrNames.VST4LNd32Pseudo_UPD};
        int QOpcodes[] = {ARMGenInstrNames.VST4LNq16Pseudo_UPD,
            ARMGenInstrNames.VST4LNq32Pseudo_UPD};
        return selectVLDSTLane(node, false, true, 4, DOpcodes, QOpcodes);
      }

      case ISD.INTRINSIC_VOID:
      case ISD.INTRINSIC_W_CHAIN: {
        int IntNo = (int) ((SDNode.ConstantSDNode) node.getOperand(1).getNode()).getZExtValue();
        switch (values()[IntNo]) {
          default:
            break;
          case arm_ldrexd: {
            SDValue memAddr = node.getOperand(2);
            DebugLoc dl = node.getDebugLoc();
            SDValue chain = node.getOperand(0);

            int newOpc = ARMGenInstrNames.LDREXD;
            if (subtarget.isThumb() && subtarget.hasThumb2())
              newOpc = ARMGenInstrNames.t2LDREXD;

            // arm_ldrexd returns a i64 value in {i32, i32}
            ArrayList<EVT> resTys = new ArrayList<>();
            resTys.add(new EVT(MVT.i32));
            resTys.add(new EVT(MVT.i32));
            resTys.add(new EVT(MVT.Other));

            // place arguments in the right order
            SDValue[] ops = {memAddr, getAL(curDAG), curDAG.getRegister(0, new EVT(MVT.i32)), chain};
            SDNode.MachineSDNode ld = curDAG.getMachineNode(newOpc, curDAG.getVTList(resTys), ops);
            // Transfer memoperands.
            ld.setMemRefs(new MachineMemOperand[]{((SDNode.MemIntrinsicSDNode) node).getMemOperand()});

            // Until there's support for specifing explicit register constraints
            // like the use of even/odd register pair, hardcode ldrexd to always
            // use the pair [R0, R1] to hold the load result.
            chain = curDAG.getCopyToReg(curDAG.getEntryNode(), ARMGenRegisterNames.R0,
                new SDValue(ld, 0), new SDValue(null, 0));
            chain = curDAG.getCopyToReg(chain, ARMGenRegisterNames.R1, new SDValue(ld, 1), chain.getValue(1));

            // Remap uses.
            SDValue Glue = chain.getValue(1);
            if (!new SDValue(node, 0).isUseEmpty()) {
              SDValue Result = curDAG.getCopyFromReg(curDAG.getEntryNode(),
                  ARMGenRegisterNames.R0, new EVT(MVT.i32), Glue);
              Glue = Result.getValue(2);
              replaceUses(new SDValue(node, 0), Result);
            }
            if (!new SDValue(node, 1).isUseEmpty()) {
              SDValue Result = curDAG.getCopyFromReg(curDAG.getEntryNode(),
                  ARMGenRegisterNames.R1, new EVT(MVT.i32), Glue);
              Glue = Result.getValue(2);
              replaceUses(new SDValue(node, 1), Result);
            }

            replaceUses(new SDValue(node, 2), new SDValue(ld, 2));
            return null;
          }

          case arm_strexd: {
            DebugLoc dl = node.getDebugLoc();
            SDValue chain = node.getOperand(0);
            SDValue val0 = node.getOperand(2);
            SDValue val1 = node.getOperand(3);
            SDValue memAddr = node.getOperand(4);

            // Until there's support for specifing explicit register constraints
            // like the use of even/odd register pair, hardcode strexd to always
            // use the pair [R2, R3] to hold the i64 (i32, i32) value to be stored.
            chain = curDAG.getCopyToReg(curDAG.getEntryNode(), ARMGenRegisterNames.R2, val0,
                new SDValue(null, 0));
            chain = curDAG.getCopyToReg(chain, ARMGenRegisterNames.R3, val1, chain.getValue(1));

            SDValue Glue = chain.getValue(1);
            val0 = curDAG.getCopyFromReg(curDAG.getEntryNode(), ARMGenRegisterNames.R2, new EVT(MVT.i32), Glue);
            Glue = val0.getValue(1);
            val1 = curDAG.getCopyFromReg(curDAG.getEntryNode(), ARMGenRegisterNames.R3, new EVT(MVT.i32), Glue);

            // Store exclusive double return a i32 value which is the return status
            // of the issued store.
            ArrayList<EVT> resTys = new ArrayList<>();
            resTys.add(new EVT(MVT.i32));
            resTys.add(new EVT(MVT.Other));

            // place arguments in the right order
            SDValue[] ops = new SDValue[]{val0, val1, memAddr, getAL(curDAG), curDAG.getRegister(0, new EVT(MVT.i32)), chain};
            int NewOpc = ARMGenInstrNames.STREXD;
            if (subtarget.isThumb() && subtarget.hasThumb2())
              NewOpc = ARMGenInstrNames.t2STREXD;

            SDNode.MachineSDNode st = curDAG.getMachineNode(NewOpc, curDAG.getVTList(resTys), ops);
            // Transfer memoperands.
            st.setMemRefs(new MachineMemOperand[]{((SDNode.MemIntrinsicSDNode) node).getMemOperand()});
            return st;
          }

          case arm_neon_vld1: {
            int DOpcodes[] = {ARMGenInstrNames.VLD1d8, ARMGenInstrNames.VLD1d16,
                ARMGenInstrNames.VLD1d32, ARMGenInstrNames.VLD1d64};
            int QOpcodes[] = {ARMGenInstrNames.VLD1q8Pseudo, ARMGenInstrNames.VLD1q16Pseudo,
                ARMGenInstrNames.VLD1q32Pseudo, ARMGenInstrNames.VLD1q64Pseudo};
            return selectVLD(node, false, 1, DOpcodes, QOpcodes, null);
          }

          case arm_neon_vld2: {
            int DOpcodes[] = {ARMGenInstrNames.VLD2d8Pseudo, ARMGenInstrNames.VLD2d16Pseudo,
                ARMGenInstrNames.VLD2d32Pseudo, ARMGenInstrNames.VLD1q64Pseudo};
            int QOpcodes[] = {ARMGenInstrNames.VLD2q8Pseudo, ARMGenInstrNames.VLD2q16Pseudo,
                ARMGenInstrNames.VLD2q32Pseudo};
            return selectVLD(node, false, 2, DOpcodes, QOpcodes, null);
          }

          case arm_neon_vld3: {
            int DOpcodes[] = {ARMGenInstrNames.VLD3d8Pseudo, ARMGenInstrNames.VLD3d16Pseudo,
                ARMGenInstrNames.VLD3d32Pseudo, ARMGenInstrNames.VLD1d64TPseudo};
            int QOpcodes0[] = {ARMGenInstrNames.VLD3q8Pseudo_UPD,
                ARMGenInstrNames.VLD3q16Pseudo_UPD,
                ARMGenInstrNames.VLD3q32Pseudo_UPD};
            int QOpcodes1[] = {ARMGenInstrNames.VLD3q8oddPseudo,
                ARMGenInstrNames.VLD3q16oddPseudo,
                ARMGenInstrNames.VLD3q32oddPseudo};
            return selectVLD(node, false, 3, DOpcodes, QOpcodes0, QOpcodes1);
          }

          case arm_neon_vld4: {
            int DOpcodes[] = {ARMGenInstrNames.VLD4d8Pseudo, ARMGenInstrNames.VLD4d16Pseudo,
                ARMGenInstrNames.VLD4d32Pseudo, ARMGenInstrNames.VLD1d64QPseudo};
            int QOpcodes0[] = {ARMGenInstrNames.VLD4q8Pseudo_UPD,
                ARMGenInstrNames.VLD4q16Pseudo_UPD,
                ARMGenInstrNames.VLD4q32Pseudo_UPD};
            int QOpcodes1[] = {ARMGenInstrNames.VLD4q8oddPseudo,
                ARMGenInstrNames.VLD4q16oddPseudo,
                ARMGenInstrNames.VLD4q32oddPseudo};
            return selectVLD(node, false, 4, DOpcodes, QOpcodes0, QOpcodes1);
          }

          case arm_neon_vld2lane: {
            int DOpcodes[] = {ARMGenInstrNames.VLD2LNd8Pseudo, ARMGenInstrNames.VLD2LNd16Pseudo,
                ARMGenInstrNames.VLD2LNd32Pseudo};
            int QOpcodes[] = {ARMGenInstrNames.VLD2LNq16Pseudo, ARMGenInstrNames.VLD2LNq32Pseudo};
            return selectVLDSTLane(node, true, false, 2, DOpcodes, QOpcodes);
          }

          case arm_neon_vld3lane: {
            int DOpcodes[] = {ARMGenInstrNames.VLD3LNd8Pseudo, ARMGenInstrNames.VLD3LNd16Pseudo,
                ARMGenInstrNames.VLD3LNd32Pseudo};
            int QOpcodes[] = {ARMGenInstrNames.VLD3LNq16Pseudo, ARMGenInstrNames.VLD3LNq32Pseudo};
            return selectVLDSTLane(node, true, false, 3, DOpcodes, QOpcodes);
          }

          case arm_neon_vld4lane: {
            int DOpcodes[] = {ARMGenInstrNames.VLD4LNd8Pseudo, ARMGenInstrNames.VLD4LNd16Pseudo,
                ARMGenInstrNames.VLD4LNd32Pseudo};
            int QOpcodes[] = {ARMGenInstrNames.VLD4LNq16Pseudo, ARMGenInstrNames.VLD4LNq32Pseudo};
            return selectVLDSTLane(node, true, false, 4, DOpcodes, QOpcodes);
          }

          case arm_neon_vst1: {
            int DOpcodes[] = {ARMGenInstrNames.VST1d8, ARMGenInstrNames.VST1d16,
                ARMGenInstrNames.VST1d32, ARMGenInstrNames.VST1d64};
            int QOpcodes[] = {ARMGenInstrNames.VST1q8Pseudo, ARMGenInstrNames.VST1q16Pseudo,
                ARMGenInstrNames.VST1q32Pseudo, ARMGenInstrNames.VST1q64Pseudo};
            return selectVST(node, false, 1, DOpcodes, QOpcodes, null);
          }

          case arm_neon_vst2: {
            int DOpcodes[] = {ARMGenInstrNames.VST2d8Pseudo, ARMGenInstrNames.VST2d16Pseudo,
                ARMGenInstrNames.VST2d32Pseudo, ARMGenInstrNames.VST1q64Pseudo};
            int QOpcodes[] = {ARMGenInstrNames.VST2q8Pseudo, ARMGenInstrNames.VST2q16Pseudo,
                ARMGenInstrNames.VST2q32Pseudo};
            return selectVST(node, false, 2, DOpcodes, QOpcodes, null);
          }

          case arm_neon_vst3: {
            int DOpcodes[] = {ARMGenInstrNames.VST3d8Pseudo, ARMGenInstrNames.VST3d16Pseudo,
                ARMGenInstrNames.VST3d32Pseudo, ARMGenInstrNames.VST1d64TPseudo};
            int QOpcodes0[] = {ARMGenInstrNames.VST3q8Pseudo_UPD,
                ARMGenInstrNames.VST3q16Pseudo_UPD,
                ARMGenInstrNames.VST3q32Pseudo_UPD};
            int QOpcodes1[] = {ARMGenInstrNames.VST3q8oddPseudo,
                ARMGenInstrNames.VST3q16oddPseudo,
                ARMGenInstrNames.VST3q32oddPseudo};
            return selectVST(node, false, 3, DOpcodes, QOpcodes0, QOpcodes1);
          }

          case arm_neon_vst4: {
            int DOpcodes[] = {ARMGenInstrNames.VST4d8Pseudo, ARMGenInstrNames.VST4d16Pseudo,
                ARMGenInstrNames.VST4d32Pseudo, ARMGenInstrNames.VST1d64QPseudo};
            int QOpcodes0[] = {ARMGenInstrNames.VST4q8Pseudo_UPD,
                ARMGenInstrNames.VST4q16Pseudo_UPD,
                ARMGenInstrNames.VST4q32Pseudo_UPD};
            int QOpcodes1[] = {ARMGenInstrNames.VST4q8oddPseudo,
                ARMGenInstrNames.VST4q16oddPseudo,
                ARMGenInstrNames.VST4q32oddPseudo};
            return selectVST(node, false, 4, DOpcodes, QOpcodes0, QOpcodes1);
          }

          case arm_neon_vst2lane: {
            int DOpcodes[] = {ARMGenInstrNames.VST2LNd8Pseudo, ARMGenInstrNames.VST2LNd16Pseudo,
                ARMGenInstrNames.VST2LNd32Pseudo};
            int QOpcodes[] = {ARMGenInstrNames.VST2LNq16Pseudo, ARMGenInstrNames.VST2LNq32Pseudo};
            return selectVLDSTLane(node, false, false, 2, DOpcodes, QOpcodes);
          }

          case arm_neon_vst3lane: {
            int DOpcodes[] = {ARMGenInstrNames.VST3LNd8Pseudo, ARMGenInstrNames.VST3LNd16Pseudo,
                ARMGenInstrNames.VST3LNd32Pseudo};
            int QOpcodes[] = {ARMGenInstrNames.VST3LNq16Pseudo, ARMGenInstrNames.VST3LNq32Pseudo};
            return selectVLDSTLane(node, false, false, 3, DOpcodes, QOpcodes);
          }

          case arm_neon_vst4lane: {
            int DOpcodes[] = {ARMGenInstrNames.VST4LNd8Pseudo, ARMGenInstrNames.VST4LNd16Pseudo,
                ARMGenInstrNames.VST4LNd32Pseudo};
            int QOpcodes[] = {ARMGenInstrNames.VST4LNq16Pseudo, ARMGenInstrNames.VST4LNq32Pseudo};
            return selectVLDSTLane(node, false, false, 4, DOpcodes, QOpcodes);
          }
        }
        break;
      }

      case ISD.INTRINSIC_WO_CHAIN: {
        int IntNo = (int) ((SDNode.ConstantSDNode) node.getOperand(0).getNode()).getZExtValue();
        switch (values()[IntNo]) {
          default:
            break;
          case arm_neon_vtbl2:
            return selectVTBL(node, false, 2, ARMGenInstrNames.VTBL2Pseudo);
          case arm_neon_vtbl3:
            return selectVTBL(node, false, 3, ARMGenInstrNames.VTBL3Pseudo);
          case arm_neon_vtbl4:
            return selectVTBL(node, false, 4, ARMGenInstrNames.VTBL4Pseudo);

          case arm_neon_vtbx2:
            return selectVTBL(node, true, 2, ARMGenInstrNames.VTBX2Pseudo);
          case arm_neon_vtbx3:
            return selectVTBL(node, true, 3, ARMGenInstrNames.VTBX3Pseudo);
          case arm_neon_vtbx4:
            return selectVTBL(node, true, 4, ARMGenInstrNames.VTBX4Pseudo);
        }
        break;
      }

      case ARMISD.VTBL1: {
        DebugLoc dl = node.getDebugLoc();
        EVT VT = node.getValueType(0);
        ArrayList<SDValue> ops = new ArrayList<>();

        ops.add(node.getOperand(0));
        ops.add(node.getOperand(1));
        ops.add(getAL(curDAG));                    // Predicate
        ops.add(curDAG.getRegister(0, MVT.i32)); // Predicate Register
        return curDAG.getMachineNode(ARMGenInstrNames.VTBL1, VT);
      }
      case ARMISD.VTBL2: {
        DebugLoc dl = node.getDebugLoc();
        EVT VT = node.getValueType(0);

        // Form a REG_SEQUENCE to force register allocation.
        SDValue V0 = node.getOperand(0);
        SDValue V1 = node.getOperand(1);
        SDValue RegSeq = new SDValue(pairDRegs(new EVT(MVT.v16i8), V0, V1), 0);

        ArrayList<SDValue> ops = new ArrayList<>();
        ops.add(RegSeq);
        ops.add(node.getOperand(2));
        ops.add(getAL(curDAG));                    // Predicate
        ops.add(curDAG.getRegister(0, MVT.i32)); // Predicate Register
        return curDAG.getMachineNode(ARMGenInstrNames.VTBL2Pseudo, VT);
      }

      case ISD.CONCAT_VECTORS:
        return selectConcatVector(node);

      case ARMISD.ATOMOR64_DAG:
        return selectAtomic64(node, ARMGenInstrNames.ATOMOR6432);
      case ARMISD.ATOMXOR64_DAG:
        return selectAtomic64(node, ARMGenInstrNames.ATOMXOR6432);
      case ARMISD.ATOMADD64_DAG:
        return selectAtomic64(node, ARMGenInstrNames.ATOMADD6432);
      case ARMISD.ATOMSUB64_DAG:
        return selectAtomic64(node, ARMGenInstrNames.ATOMSUB6432);
      case ARMISD.ATOMNAND64_DAG:
        return selectAtomic64(node, ARMGenInstrNames.ATOMNAND6432);
      case ARMISD.ATOMAND64_DAG:
        return selectAtomic64(node, ARMGenInstrNames.ATOMAND6432);
      case ARMISD.ATOMSWAP64_DAG:
        return selectAtomic64(node, ARMGenInstrNames.ATOMSWAP6432);
      case ARMISD.ATOMCMPXCHG64_DAG:
        return selectAtomic64(node, ARMGenInstrNames.ATOMCMPXCHG6432);
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

  private SDNode selectConcatVector(SDNode node) {
    //
    EVT vt = node.getValueType(0);
    if (!vt.is128BitVector() || node.getNumOperands() != 2)
      Util.shouldNotReachHere("unexpected CONCAT_VECTORS");
    return pairDRegs(vt, node.getOperand(0), node.getOperand(1));
  }

  private SDNode selectAtomic64(SDNode node, int opc) {
    ArrayList<SDValue> ops = new ArrayList<>();
    ops.add(node.getOperand(1));  // ptr
    ops.add(node.getOperand(2));  // low part of val1.
    ops.add(node.getOperand(3));  // high part of val1.
    if (opc == ARMGenInstrNames.ATOMCMPXCHG6432) {
      ops.add(node.getOperand(4));  // low part of val2.
      ops.add(node.getOperand(5));  // high part of val2.
    }
    // chain.
    ops.add(node.getOperand(0));
    SDNode.SDVTList vts = curDAG.getVTList(new EVT(MVT.i32), new EVT(MVT.i32), new EVT(MVT.Other));
    SDValue[] args = new SDValue[ops.size()];
    ops.toArray(args);
    SDNode.MachineSDNode resNode = curDAG.getMachineNode(opc, vts, args);
    resNode.setMemRefs(new MachineMemOperand[]{((SDNode.MemSDNode)node).getMemOperand()});
    return resNode;
  }

  /**
   * Form 4 consecutive S registers.
   * @param vt
   * @param v0
   * @param v1
   * @param v2
   * @param v3
   * @return
   */
  private SDNode quadSRegs(EVT vt, SDValue v0, SDValue v1, SDValue v2, SDValue v3) {
    SDValue regClass = curDAG.getTargetConstant(ARMGenRegisterInfo.QPR_VFP2RegClassID, new EVT(MVT.i32));
    SDValue subReg0 = curDAG.getTargetConstant(ARMGenRegisterInfo.ssub_0, new EVT(MVT.i32));
    SDValue subReg1 = curDAG.getTargetConstant(ARMGenRegisterInfo.ssub_1, new EVT(MVT.i32));
    SDValue subReg2 = curDAG.getTargetConstant(ARMGenRegisterInfo.ssub_2, new EVT(MVT.i32));
    SDValue subReg3 = curDAG.getTargetConstant(ARMGenRegisterInfo.ssub_3, new EVT(MVT.i32));
    SDValue[] ops = {regClass, v0, subReg0, v1, subReg1, v2, subReg2, v3, subReg3};
    return curDAG.getMachineNode(TargetOpcode.REG_SEQUENCE, vt, ops);
  }

  private SDNode quadDRegs(EVT vt, SDValue v0, SDValue v1, SDValue v2, SDValue v3) {
    SDValue regClass = curDAG.getTargetConstant(ARMGenRegisterInfo.QQPRRegClassID, new EVT(MVT.i32));
    SDValue subReg0 = curDAG.getTargetConstant(ARMGenRegisterInfo.dsub_0, new EVT(MVT.i32));
    SDValue subReg1 = curDAG.getTargetConstant(ARMGenRegisterInfo.dsub_1, new EVT(MVT.i32));
    SDValue subReg2 = curDAG.getTargetConstant(ARMGenRegisterInfo.dsub_2, new EVT(MVT.i32));
    SDValue subReg3 = curDAG.getTargetConstant(ARMGenRegisterInfo.dsub_3, new EVT(MVT.i32));
    SDValue[] ops = {regClass, v0, subReg0, v1, subReg1, v2, subReg2, v3, subReg3};
    return curDAG.getMachineNode(TargetOpcode.REG_SEQUENCE, vt, ops);
  }

  private SDNode quadQRegs(EVT vt, SDValue v0, SDValue v1, SDValue v2, SDValue v3) {
    SDValue regClass = curDAG.getTargetConstant(ARMGenRegisterInfo.QQQQPRRegClassID, new EVT(MVT.i32));
    SDValue subReg0 = curDAG.getTargetConstant(ARMGenRegisterInfo.qsub_0, new EVT(MVT.i32));
    SDValue subReg1 = curDAG.getTargetConstant(ARMGenRegisterInfo.qsub_1, new EVT(MVT.i32));
    SDValue subReg2 = curDAG.getTargetConstant(ARMGenRegisterInfo.qsub_2, new EVT(MVT.i32));
    SDValue subReg3 = curDAG.getTargetConstant(ARMGenRegisterInfo.qsub_3, new EVT(MVT.i32));
    SDValue[] ops = {regClass, v0, subReg0, v1, subReg1, v2, subReg2, v3, subReg3};
    return curDAG.getMachineNode(TargetOpcode.REG_SEQUENCE, vt, ops);
  }

  /**
   * Form a D register from a pair of S registers.
   * @param vt
   * @param v0
   * @param v1
   * @return
   */
  private SDNode pairSRegs(EVT vt, SDValue v0, SDValue v1) {
    SDValue regClass = curDAG.getTargetConstant(ARMGenRegisterInfo.DPR_VFP2RegClassID, new EVT(MVT.i32));
    SDValue subReg0 = curDAG.getTargetConstant(ARMGenRegisterInfo.ssub_0, new EVT(MVT.i32));
    SDValue subReg1 = curDAG.getTargetConstant(ARMGenRegisterInfo.ssub_1, new EVT(MVT.i32));
    SDValue[] ops = {regClass, v0, subReg0, v1, subReg1};
    return curDAG.getMachineNode(TargetOpcode.REG_SEQUENCE, vt, ops);
  }

  /**
   * Form a quad register from a pair of D registers.
   * @param vt
   * @param v0
   * @param v1
   * @return
   */
  private SDNode pairDRegs(EVT vt, SDValue v0, SDValue v1) {
    SDValue regClass = curDAG.getTargetConstant(ARMGenRegisterInfo.QPRRegClassID, new EVT(MVT.i32));
    SDValue subReg0 = curDAG.getTargetConstant(ARMGenRegisterInfo.dsub_0, new EVT(MVT.i32));
    SDValue subReg1 = curDAG.getTargetConstant(ARMGenRegisterInfo.dsub_1, new EVT(MVT.i32));
    SDValue[] ops = {regClass, v0, subReg0, v1, subReg1};
    return curDAG.getMachineNode(TargetOpcode.REG_SEQUENCE, vt, ops);
  }

  /**
   * Form 4 consecutive D registers from a pair of Q registers.
   * @param vt
   * @param v0
   * @param v1
   * @return
   */
  private SDNode pairQRegs(EVT vt, SDValue v0, SDValue v1) {
    SDValue regClass = curDAG.getTargetConstant(ARMGenRegisterInfo.QQPRRegClassID, new EVT(MVT.i32));
    SDValue subReg0 = curDAG.getTargetConstant(ARMGenRegisterInfo.qsub_0, new EVT(MVT.i32));
    SDValue subReg1 = curDAG.getTargetConstant(ARMGenRegisterInfo.qsub_1, new EVT(MVT.i32));
    SDValue[] ops = {regClass, v0, subReg0, v1, subReg1};
    return curDAG.getMachineNode(TargetOpcode.REG_SEQUENCE, vt, ops);
  }

  private boolean selectAddrMode2OffsetImmPre(SDNode op, SDValue n, SDValue[] out) {
    // op is the base ptr of ld, n is the offset part of address expression.
    // out = {offset, opc}
    int opcode = op.getOpcode();
    MemIndexedMode am = opcode == ISD.LOAD ? ((SDNode.LoadSDNode)op).getAddressingMode() :
        ((SDNode.StoreSDNode)op).getAddressingMode();
    ARM_AM.AddrOpc addSub = am == MemIndexedMode.PRE_INC || am == MemIndexedMode.POST_INC ?
        ARM_AM.AddrOpc.add : ARM_AM.AddrOpc.sub;
    OutRef<Integer> val = new OutRef<>(0);
    if (isScaledConstantInRange(n, 1, 0, 0x1000, val)) {
      // 12 bits.
      if (addSub == ARM_AM.AddrOpc.sub)
        val.set(-val.get());

      out[0] = curDAG.getRegister(0, new EVT(MVT.i32));
      out[1] = curDAG.getTargetConstant(val.get(), new EVT(MVT.i32));
      return true;
    }
    return false;
  }

  private SDNode selectARMIndexedLoad(SDNode node) {
    SDNode.LoadSDNode ld = (SDNode.LoadSDNode) node;
    MemIndexedMode mim = ld.getAddressingMode();
    if (mim == MemIndexedMode.UNINDEXED)
      return null;

    EVT loadVT = ld.getMemoryVT();
    boolean isPre = mim == MemIndexedMode.PRE_INC || mim == MemIndexedMode.PRE_DEC;
    int opcode = 0;
    boolean match = false;
    SDValue[] args = new SDValue[2];
    SDValue offset = new SDValue(), amOpc = new SDValue();
    // ldr r0, [r1, +-imm]
    if (loadVT.equals(new EVT(MVT.i32)) && isPre &&
        selectAddrMode2OffsetImmPre(node, ld.getOffset(), args)) {
      offset = args[0];
      amOpc = args[1];
      opcode = ARMGenInstrNames.LDR_PRE_IMM;
      match = true;
    }
    else if (loadVT.equals(new EVT(MVT.i32)) && !isPre &&
        selectAddrMode2OffsetImm(node, ld.getOffset(), args)) {
      offset = args[0];
      amOpc = args[1];
      opcode = ARMGenInstrNames.LDR_POST_IMM;
      match = true;
    }
    else if (loadVT.equals(new EVT(MVT.i32)) &&
        selectAddrMode2OffsetReg(node, ld.getOffset(), args)) {
      offset = args[0];
      amOpc = args[1];
      opcode = !isPre ? ARMGenInstrNames.LDR_POST_REG : ARMGenInstrNames.LDR_PRE_REG;
      match = true;
    }
    else if (loadVT.equals(new EVT(MVT.i16)) &&
        selectAddrMode3Offset(node, ld.getOffset(), args)) {
      offset = args[0];
      amOpc = args[1];
      opcode = ld.getExtensionType() == LoadExtType.SEXTLOAD ?
          (!isPre ? ARMGenInstrNames.LDRSH_POST : ARMGenInstrNames.LDRSH_PRE) :
          (!isPre ? ARMGenInstrNames.LDRH_POST : ARMGenInstrNames.LDRH_PRE);
      match = true;
    }
    else if (loadVT.equals(new EVT(MVT.i8)) || loadVT.equals(new EVT(MVT.i1))) {
      if (ld.getExtensionType() == LoadExtType.SEXTLOAD) {
        if (selectAddrMode3Offset(node, ld.getOffset(), args)){
          offset = args[0];
          amOpc = args[1];
          match = true;
          opcode = isPre ? ARMGenInstrNames.LDRSB_PRE : ARMGenInstrNames.LDRSB_POST;
        }
      }
      else {
        if (isPre && selectAddrMode2OffsetImmPre(node, ld.getOffset(), args)) {
          offset = args[0];
          amOpc = args[1];
          match = true;
          opcode = ARMGenInstrNames.LDRB_PRE_IMM;
        }
        else if (!isPre && selectAddrMode2OffsetImm(node, ld.getOffset(), args)) {
          offset = args[0];
          amOpc = args[1];
          match = true;
          opcode = ARMGenInstrNames.LDRB_POST_IMM;
        }
        else if (selectAddrMode2OffsetReg(node, ld.getOffset(), args)) {
          offset = args[0];
          amOpc = args[1];
          match = true;
          opcode = isPre ? ARMGenInstrNames.LDRB_PRE_REG : ARMGenInstrNames.LDRB_POST_REG;
        }
      }
    }
    if (match) {
      if (opcode == ARMGenInstrNames.LDR_PRE_IMM || opcode == ARMGenInstrNames.LDRB_PRE_IMM) {
        SDValue chain = ld.getChain();
        SDValue base = ld.getBasePtr();
        SDValue[] ops = new SDValue[] {base, amOpc, getAL(curDAG),
            curDAG.getRegister(0, new EVT(MVT.i32)), chain};
        // for pre or post, it updates the address base register.
        return curDAG.getMachineNode(opcode, new EVT(MVT.i32),
            new EVT(MVT.i32), new EVT(MVT.Other), ops);
      }
      else {
        SDValue chain = ld.getChain();
        SDValue base = ld.getBasePtr();
        SDValue[] ops = {base, offset, amOpc, getAL(curDAG), curDAG.getRegister(0, new EVT(MVT.i32)), chain};
        return curDAG.getMachineNode(opcode, new EVT(MVT.i32),
            new EVT(MVT.i32), new EVT(MVT.Other), ops);
      }
    }
    return null;
  }

  private SDNode selectT2IndexedLoad(SDNode node) {
    SDNode.LoadSDNode ld = (SDNode.LoadSDNode) node;
    MemIndexedMode am = ld.getAddressingMode();
    if (am == MemIndexedMode.UNINDEXED)
      return null;

    EVT loadedVT = ld.getMemoryVT();
    boolean isExtLd = ld.getExtensionType() == LoadExtType.SEXTLOAD;
    boolean isPre = am == MemIndexedMode.PRE_INC || am == MemIndexedMode.PRE_DEC;
    int opcode = 0;
    boolean match = false;
    OutRef<SDValue> offset = new OutRef<>();
    if (selectT2AddrModeImm8Offset(node, ld.getOffset(), offset)) {
      switch (loadedVT.getSimpleVT().simpleVT) {
        case MVT.i32:
          opcode = isPre ? ARMGenInstrNames.t2LDR_PRE : ARMGenInstrNames.t2LDR_POST;
          break;
        case MVT.i16:
          opcode = isPre ? ARMGenInstrNames.t2LDRH_PRE : ARMGenInstrNames.t2LDRH_POST;
          break;
        case MVT.i8:
        case MVT.i1:
          opcode = isPre ? ARMGenInstrNames.t2LDRB_PRE : ARMGenInstrNames.t2LDRB_POST;
          break;
        default:
          return null;
      }
      match = true;
    }
    if (match) {
      SDValue chain = ld.getChain();
      SDValue base = ld.getBasePtr();
      SDValue[] ops = {base, offset.get(), getAL(curDAG),
          curDAG.getRegister(0, new EVT(MVT.i32)), chain};
      return curDAG.getMachineNode(opcode, new EVT(MVT.i32), new EVT(MVT.i32), new EVT(MVT.Other), ops);
    }

    return null;
  }

  private boolean isInt32Immediate(SDNode n, OutRef<Long> imm) {
    if (n.getOpcode() == ISD.Constant && n.getValueType(0).equals(new EVT(MVT.i32))) {
      imm.set(((SDNode.ConstantSDNode)n).getZExtValue());
      return true;
    }
    return false;
  }
  private boolean isInt32Immediate(SDValue n, OutRef<Long> imm) {
    return isInt32Immediate(n.getNode(), imm);
  }

  private boolean isOpcWithIntImmediate(SDNode n, int opc, OutRef<Long> imm) {
    return n.getOpcode() == opc && isInt32Immediate(n.getOperand(1).getNode(), imm);
  }

  private SDNode selectV6T2BitfieldExtractOp(SDNode node, boolean isSigned) {
    if (!subtarget.hasV6T2Ops())
      return null;

    int opc = isSigned ? (subtarget.isThumb() ? ARMGenInstrNames.t2SBFX : ARMGenInstrNames.SBFX)
        : (subtarget.isThumb()? ARMGenInstrNames.t2UBFX : ARMGenInstrNames.UBFX);
    // for int extracts, check for a shift right and mask.
    OutRef<Long> andImm = new OutRef<>(0L);
    if (node.getOpcode() == ISD.AND) {
      if (isOpcWithIntImmediate(node, ISD.AND, andImm)) {
        if ((andImm.get() & (andImm.get() + 1)) != 0)
          return null;

        OutRef<Long> srlImm = new OutRef<>(0L);
        if (isOpcWithIntImmediate(node.getOperand(0).getNode(), ISD.SRL, srlImm)) {
          Util.assertion(srlImm.get() > 0 && srlImm.get() < 32, "bad acount in shift node!");
          int width = Util.countTrailingOnes(andImm.get()) - 1;
          long lsb = srlImm.get();
          SDValue reg0 = curDAG.getRegister(0, new EVT(MVT.i32));
          SDValue[] ops = {node.getOperand(0).getOperand(0),
              curDAG.getTargetConstant(lsb, new EVT(MVT.i32)),
              curDAG.getTargetConstant(width, new EVT(MVT.i32)),
              getAL(curDAG)};
          return curDAG.selectNodeTo(node, opc, new EVT(MVT.i32), ops);
        }
      }
      return null;
    }

    // otherwise, we are looking for a shift of a shift.
    // (srl|sra, (shl, X, C1), C2)
    OutRef<Long> shlImm = new OutRef<>(0L);
    if (isOpcWithIntImmediate(node.getOperand(0).getNode(), ISD.SHL, shlImm)) {
      Util.assertion(shlImm.get() >0 && shlImm.get() < 32, "bad amount in shift node!");
      OutRef<Long> srlImm = new OutRef<>(0L);
      if (isInt32Immediate(node.getOperand(1), srlImm)) {
        Util.assertion(srlImm.get() > 0 && srlImm.get() < 32, "bad amount in shift node!");
        long width = 32 - srlImm.get() - 1;
        long lsb = srlImm.get() - shlImm.get();
        if (lsb < 0) return null;

        SDValue reg0 = curDAG.getRegister(0, new EVT(MVT.i32));
        SDValue[] ops = {node.getOperand(0).getOperand(0),
            curDAG.getTargetConstant(lsb, new EVT(MVT.i32)),
            curDAG.getTargetConstant(width, new EVT(MVT.i32)),
            getAL(curDAG), reg0};
        return curDAG.selectNodeTo(node, opc, new EVT(MVT.i32), ops);
      }
    }
    return null;
  }

  /**
   * Return a constantSDNode with the {@linkplain ARMCC.CondCodes#AL} as an immediate.
   * @param curDAG
   * @return
   */
  private static SDValue getAL(SelectionDAG curDAG) {
    return curDAG.getTargetConstant(ARMCC.CondCodes.AL.ordinal(), new EVT(MVT.i32));
  }

  /**
   * ARM instruction selection transform the following code
   * <pre>
   *   Y = sra (X, size(X) - 1);
   *   xor (add (X, Y), Y)
   * </pre>
   * into
   * <pre>
   *   abs(X)
   * </pre>
   * {@linkplain ARMGenInstrNames#ABS} or {@linkplain ARMGenInstrNames#t2ABS}.
   * @param node
   * @return
   */
  private SDNode selectABSOp(SDNode node) {
    SDValue xor0 = node.getOperand(0), xor1 = node.getOperand(1);
    if (DisableARMIntABS.value || subtarget.isThumb1Only()) return null;
    if (xor0.getOpcode() != ISD.ADD || xor1.getOpcode() != ISD.SRA)
      return null;

    SDValue add0 = xor0.getOperand(0), add1 = xor0.getOperand(1);
    SDValue sra0 = xor1.getOperand(0), sra1 = xor1.getOperand(1);
    if (!(sra1.getNode() instanceof SDNode.ConstantSDNode))
      return null;

    SDNode.ConstantSDNode sra1Cst = (SDNode.ConstantSDNode) sra1.getNode();
    long size = sra0.getValueSizeInBits() - 1;
    EVT xType = sra0.getValueType();
    if (add1.equals(xor1) && add0.equals(sra0) &&
        xType.isInteger() && size == sra1Cst.getZExtValue()) {
      int opcode = subtarget.isThumb2() ? ARMGenInstrNames.t2ABS : ARMGenInstrNames.ABS;
      return curDAG.selectNodeTo(node, opcode, node.getValueType(0), add0);
    }
    return null;
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

    ARMCC.CondCodes armcc = ARMCC.CondCodes.values()[(int) ((SDNode.ConstantSDNode) cc.getNode()).getZExtValue()];
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
      } else {
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
    } else {
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
    SDValue[] ops = new SDValue[]{falseVal, trueVal, tmp2, ccr, inflag};
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
      long soVal = ((SDNode.ConstantSDNode) tmp1.getNode()).getZExtValue();
      ARM_AM.ShiftOpc soShOp = ARM_AM.getSORegShOp(soVal);
      int opc = 0;
      switch (soShOp) {
        case lsl:
          opc = ARMGenInstrNames.t2MOVCClsl;
          break;
        case lsr:
          opc = ARMGenInstrNames.t2MOVCClsr;
          break;
        case asr:
          opc = ARMGenInstrNames.t2MOVCCasr;
          break;
        case ror:
          opc = ARMGenInstrNames.t2MOVCCror;
          break;
        default:
          Util.shouldNotReachHere("Unknown shifter operator");
          break;
      }
      SDValue soShImm = curDAG.getTargetConstant(ARM_AM.getSORegOffset(soVal), new EVT(MVT.i32));
      SDValue cc = curDAG.getTargetConstant(armcc.ordinal(), new EVT(MVT.i32));
      SDValue[] ops = new SDValue[]{falseVal, tmp0, soShImm, cc, ccr, inflag};
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
      SDValue[] ops = new SDValue[]{falseVal, cpTmp0, cpTmp2, cc, ccr, inflag};
      return curDAG.selectNodeTo(n, ARMGenInstrNames.MOVCCsi, new EVT(MVT.i32), ops);
    }
    SDValue[] tmp1 = new SDValue[3];
    if (selectRegShifterOperand(trueVal, tmp1)) {
      SDValue cpTmp0 = tmp1[0], cpTmp1 = tmp1[1], cpTmp2 = tmp1[2];
      SDValue cc = curDAG.getTargetConstant(armcc.ordinal(), new EVT(MVT.i32));
      SDValue[] ops = new SDValue[]{falseVal, cpTmp0, cpTmp1, cpTmp2, cc, ccr, inflag};
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
    } else if (trueImm <= 0xffff) {
      opc = ARMGenInstrNames.t2MOVCCi16;
    } else if (is_t2_so_imm_not(trueImm)) {
      trueImm = ~trueImm;
      opc = ARMGenInstrNames.t2MVNCCi;
    } else if (trueVal.getNode().hasOneUse() && subtarget.hasV6T2Ops()) {
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
    } else if (trueImm <= 0xffff && subtarget.hasV6T2Ops()) {
      opc = ARMGenInstrNames.t2MOVCCi16;
    } else if (is_so_imm_not(trueImm)) {
      trueImm = ~trueImm;
      opc = ARMGenInstrNames.MVNCCi;
    } else if (trueVal.getNode().hasOneUse() && (subtarget.hasV6T2Ops() || ARM_AM.isSOImmTwoPartVal((int) trueImm))) {
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

  /**
   * Return true if it's desirable to select a FP MLA / MLS
   * node. VFP / NEON fp VMLA / VMLS instructions have special RAW hazards (at
   * least on current ARM implementations, cortex-A8 and cortex-A9) which should be avoided.
   * @param n
   * @return
   */
  protected boolean hasNoVMLxHazardUse(SDNode n) {
    if (optLevel == TargetMachine.CodeGenOpt.None)
      return false;

    if (!CheckVMLxHazard.value)
      return false;

    if (!subtarget.isCortexA8() && !subtarget.isCortexA9())
      return true;
    SDNode use = n.getUse(0).getNode();
    if (use.getOpcode() == ISD.CopyToReg)
      return true;

    if (use.isMachineOpecode()) {
      MCInstrDesc mcid = subtarget.getInstrInfo().get(use.getMachineOpcode());
      if (mcid.mayStore())
        return true;

      int opcode = mcid.getOpcode();
      if (opcode == ARMGenInstrNames.VMOVRS || opcode == ARMGenInstrNames.VMOVRRD)
        return true;

      // vmlx feeding into another vmlx. We actually want to unfold
      // the use later in the MLxExpansion pass. e.g.
      // vmla
      // vmla (stall 8 cycles)
      //
      // vmul (5 cycles)
      // vadd (5 cycles)
      // vmla
      // This adds up to about 18 - 19 cycles.
      //
      // vmla
      // vmul (stall 4 cycles)
      // vadd adds up to about 14 cycles.
      return subtarget.getInstrInfo().isFpMLxInstruction(opcode);
    }

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
    SDValue opc = curDAG.getTargetConstant(ARM_AM.getSORegOpc(shOpcVal, shImmVal), new EVT(MVT.i32));
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

    shImmVal = ((SDNode.ConstantSDNode) n.getOperand(1).getNode()).getZExtValue() & 31;
    SDValue opc = curDAG.getTargetConstant(ARM_AM.getSORegOpc(shOpcVal, shImmVal), new EVT(MVT.i32));
    tmp[0] = baseReg;
    tmp[1] = opc;
    return true;
  }

  private SDValue getI32Imm(long imm) {
    return curDAG.getTargetConstant(imm, new EVT(MVT.i32));
  }

  protected boolean selectT2ShifterOperandReg(SDValue n, SDValue[] tmp) {
    // tmp = {basereg, opc}
    if (DisableShifterOp.value)
      return false;

    ARM_AM.ShiftOpc shOpcVal = ARM_AM.getShiftOpcForNode(n.getOpcode());
    if (shOpcVal == ARM_AM.ShiftOpc.no_shift) return false;

    tmp[0] = n.getOperand(0);
    int shImmVal = 0;
    if (n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
      shImmVal = (int) (((SDNode.ConstantSDNode)n.getOperand(1).getNode()).getZExtValue() & 31);
      tmp[1] = getI32Imm(ARM_AM.getSORegOpc(shOpcVal, shImmVal));
      return true;
    }

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
   * (node * Scale) where (node in [{@code rangeMin}, {@code rangeMax}).
   *
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
    Util.assertion(scale > 0, "invalid scale");
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
      if (isScaledConstantInRange(n.getOperand(1), /*scale*/ 1, -0x100 + 1, 0x1000, rhsc))
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
      } else
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
          } else {
            shAmt = 0;
            shOpc = ARM_AM.ShiftOpc.no_shift;
          }
        } else
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
    // tmp = {base, offReg, shImm}
    // (R - imm8) should be handled by t2LDRi8. The rest are handled by t2LDRi12.
    if (n.getOpcode() != ISD.ADD && !curDAG.isBaseWithConstantOffset(n))
      return false;

    // leave (R + imm12) for t2LDRi12, (R - imm8) for t2LDRi8.
    if (n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
      SDNode.ConstantSDNode rhs = (SDNode.ConstantSDNode) n.getOperand(1).getNode();
      long rhsc = rhs.getZExtValue();
      // 12 bits
      if (rhsc >= 0 && rhsc < 0x1000) {
        return false;
      }
      else if (rhsc < 0 && rhsc >= -255)
        // 8bits
        return false;
    }

    // look for (R + R) or (R + (R << [1, 2, 3]))
    SDValue base = n.getOperand(0);
    SDValue offReg = n.getOperand(1);

    // swap if it is ((R << C) + R)
    ARM_AM.ShiftOpc shOpcVal = ARM_AM.getShiftOpcForNode(offReg.getOpcode());
    if (shOpcVal != ARM_AM.ShiftOpc.lsl) {
      shOpcVal = ARM_AM.getShiftOpcForNode(base.getOpcode());
      if (shOpcVal == ARM_AM.ShiftOpc.lsl) {
        SDValue t = base;
        base = offReg;
        offReg = t;
      }
    }

    long shAmt = 0;
    if (shOpcVal == ARM_AM.ShiftOpc.lsl) {
      // check to see if the rhs of the shift is a constant. if not, we can't fold it.
      if (offReg.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
        shAmt = ((SDNode.ConstantSDNode)offReg.getOperand(1).getNode()).getZExtValue();
        if (shAmt < 4 && isShifterOpProfitable(offReg, shOpcVal, shAmt))
          offReg = offReg.getOperand(0);
        else {
          shAmt = 0;
          shOpcVal = ARM_AM.ShiftOpc.no_shift;
        }
      }
      else
        shOpcVal = ARM_AM.ShiftOpc.no_shift;
    }

    tmp[0] = base;
    tmp[1] = offReg;
    // shImm
    tmp[2] = curDAG.getTargetConstant(shAmt, new EVT(MVT.i32));
    return true;
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
        if (tmp[0].getOpcode() == ISD.FrameIndex) {
          int fi = ((SDNode.FrameIndexSDNode) tmp[0].getNode()).getFrameIndex();
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
    // tmp = {base, offImm}
    // Match simple R + imm12 operands.
    // Base only
    if (n.getOpcode() != ISD.ADD && n.getOpcode() != ISD.SUB &&
        !curDAG.isBaseWithConstantOffset(n)) {
      if (n.getOpcode() == ISD.FrameIndex) {
        // match frame index.
        long fi = ((SDNode.FrameIndexSDNode) n.getNode()).getFrameIndex();
        tmp[0] = curDAG.getTargetFrameIndex((int) fi, new EVT(tli.getPointerTy()));
        tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
        return true;
      }

      if (n.getOpcode() == ARMISD.Wrapper && !(subtarget.useMovt() &&
          n.getOperand(0).getOpcode() == ISD.TargetGlobalAddress)) {
        tmp[0] = n.getOperand(0);
        if (tmp[0].getOpcode() == ISD.TargetConstantPool)
          return false; // select t2LDRpci instead.
      } else {
        tmp[0] = n;
      }
      tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
      return true;
    }

    if (n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
      SDNode.ConstantSDNode rhs = (SDNode.ConstantSDNode) n.getOperand(1).getNode();
      if (selectT2AddrModeImm8(n, tmp))
        // select t2LDRi8 for (R - imm8)
        return false;

      long rhsC = rhs.getZExtValue();
      if (n.getOpcode() == ISD.SUB)
        rhsC = -rhsC;

      if (rhsC >= 0 && rhsC <= 0x1000) {
        tmp[0] = n.getOperand(0);
        if (tmp[0].getOpcode() == ISD.FrameIndex) {
          int fi = ((SDNode.FrameIndexSDNode)tmp[0].getNode()).getFrameIndex();
          tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
        }
        tmp[1] = curDAG.getTargetConstant(rhsC, new EVT(MVT.i32));
        return true;
      }
    }

    // base only.
    tmp[0] = n;
    tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
    return true;
  }

  protected boolean selectT2AddrModeImm8(SDValue n, SDValue[] tmp) {
    // match the simple R - imm8 operands.
    if (n.getOpcode() != ISD.ADD && n.getOpcode() != ISD.SUB &&
        !curDAG.isBaseWithConstantOffset(n)) {
      return false;
    }

    if ( n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
      SDNode.ConstantSDNode rhs = (SDNode.ConstantSDNode) n.getOperand(1).getNode();
      long rhsC = rhs.getZExtValue();
      if (n.getOpcode() == ISD.SUB)
        rhsC = -rhsC;

      if (rhsC >= -255 && rhsC < 0) {
        // 8 bits
        tmp[0] = n.getOperand(0);
        if (tmp[0].getOpcode() == ISD.FrameIndex) {
          int fi = ((SDNode.FrameIndexSDNode)tmp[0].getNode()).getFrameIndex();
          tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
        }
        tmp[1] = curDAG.getTargetConstant(rhsC, new EVT(MVT.i32));
        return true;
      }
    }
    return false;
  }

  protected boolean selectAddrMode2(SDValue n, SDValue[] tmp) {
    selectAddrMode2Worker(n, tmp);
    return true;
  }

  private enum AddrMode2Type {
    AM2_BASE, // Simple AM2 (+-imm12)
    AM2_SHOP  // Shifter-op AM2
  }

  private AddrMode2Type selectAddrMode2Worker(SDValue n, SDValue[] tmp) {
    // tmp = {base, offset, opc}
    if (n.getOpcode() == ISD.MUL && (!subtarget.isCortexA9() || n.hasOneUse())) {
      if (n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
        SDNode.ConstantSDNode rhs = (SDNode.ConstantSDNode) n.getOperand(1).getNode();
        // X * (3, 5, 9) -> X + X * (2, 4, 8)
        long rhsC = rhs.getZExtValue();
        if ((rhsC & 1) != 0) {
          rhsC = rhsC & ~1;
          ARM_AM.AddrOpc addsub = ARM_AM.AddrOpc.add;
          if (rhsC < 0) {
            addsub = ARM_AM.AddrOpc.sub;
            rhsC = -rhsC;
          }
          if (Util.isPowerOf2(rhsC)) {
            int shAmt = Util.log2(rhsC);
            tmp[0] = tmp[1] = n.getOperand(0);
            tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM2Opc(addsub, shAmt, ARM_AM.ShiftOpc.lsl), new EVT(MVT.i32));
            return AddrMode2Type.AM2_SHOP;
          }
        }
      }
    }

    if (n.getOpcode() != ISD.ADD && n.getOpcode() != ISD.SUB &&
        !curDAG.isBaseWithConstantOffset(n)) {
      tmp[0] = n;
      if (n.getOpcode() == ISD.FrameIndex) {
        int fi = ((SDNode.FrameIndexSDNode)n.getNode()).getFrameIndex();
        tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
      }
      else if (n.getOpcode() == ARMISD.Wrapper && !(subtarget.useMovt() &&
          n.getOperand(0).getOpcode() == ISD.TargetGlobalAddress)) {
        tmp[0] = n.getOperand(0);
      }
      tmp[1] = curDAG.getRegister(0, new EVT(MVT.i32));
      tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM2Opc(ARM_AM.AddrOpc.add, 0, ARM_AM.ShiftOpc.no_shift), new EVT(MVT.i32));
      return AddrMode2Type.AM2_BASE;
    }

    // Match simple R +/- imm12 operands.
    if (n.getOpcode() != ISD.SUB) {
      OutRef<Integer> rhsc = new OutRef<>(0);
      if (isScaledConstantInRange(n.getOperand(1), /*scale*/1, -0x1000+1, 0x1000, rhsc)) {
        tmp[0] = n.getOperand(0);
        if (tmp[0].getOpcode() == ISD.FrameIndex) {
          int fi = ((SDNode.FrameIndexSDNode)n.getOperand(0).getNode()).getFrameIndex();
          tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
        }
        tmp[1] = curDAG.getRegister(0, new EVT(MVT.i32));

        ARM_AM.AddrOpc addsub = ARM_AM.AddrOpc.add;
        if (rhsc.get() < 0) {
          rhsc.set(-rhsc.get());
          addsub = ARM_AM.AddrOpc.sub;
        }
        // opc
        tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM2Opc(addsub, rhsc.get(), ARM_AM.ShiftOpc.no_shift), new EVT(MVT.i32));
        return AddrMode2Type.AM2_BASE;
      }
    }

    if (subtarget.isCortexA9() && !n.hasOneUse()) {
      // Compute R +/- (R << N) and reuse it.
      tmp[0] = n;
      tmp[1] = curDAG.getRegister(0, new EVT(MVT.i32));
      tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM2Opc(ARM_AM.AddrOpc.add, 0, ARM_AM.ShiftOpc.no_shift), new EVT(MVT.i32));
      return AddrMode2Type.AM2_BASE;
    }

    // Otherwise this is R +/- [possibly shifted] R.
    ARM_AM.AddrOpc addsub = n.getOpcode() != ISD.SUB ? ARM_AM.AddrOpc.add : ARM_AM.AddrOpc.sub;
    ARM_AM.ShiftOpc shopcVal = ARM_AM.getShiftOpcForNode(n.getOperand(1).getOpcode());
    long shAmt = 0;
    tmp[0] = n.getOperand(0);
    tmp[1] = n.getOperand(1);

    if (shopcVal != ARM_AM.ShiftOpc.no_shift) {
      // check to see if the RHS of the shift is a constant, if not, wew can't fold it.
      if (n.getOperand(1).getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
        shAmt = ((SDNode.ConstantSDNode)n.getOperand(1).getOperand(1).getNode()).getZExtValue();
        if (isShifterOpProfitable(tmp[1], shopcVal, shAmt))
          tmp[1] = n.getOperand(1).getOperand(0);
        else {
          shAmt = 0;
          shopcVal = ARM_AM.ShiftOpc.no_shift;
        }
      }
      else {
        shopcVal = ARM_AM.ShiftOpc.no_shift;
      }
    }

    // Try matching (R shl C) + (R).
    if (n.getOpcode() != ISD.SUB && shopcVal == ARM_AM.ShiftOpc.no_shift &&
        !(subtarget.isCortexA9() || n.getOperand(0).hasOneUse())) {
      shopcVal = ARM_AM.getShiftOpcForNode(n.getOperand(0).getOpcode());
      if (shopcVal != ARM_AM.ShiftOpc.no_shift) {
        // Check to see if the RHS of the shift is a constant, if not, we can't
        // fold it.
        if (n.getOperand(0).getOperand(1).getNode() instanceof SDNode.ConstantSDNode) {
          shAmt = ((SDNode.ConstantSDNode)n.getOperand(0).getOperand(1).getNode()).getZExtValue();
          if (isShifterOpProfitable(n.getOperand(0), shopcVal, shAmt)) {
            tmp[0] = n.getOperand(1);
            tmp[1] = n.getOperand(0).getOperand(0);
          }
          else {
            shAmt = 0;
            shopcVal = ARM_AM.ShiftOpc.no_shift;
          }
        }
        else {
          shopcVal = ARM_AM.ShiftOpc.no_shift;
        }
      }
    }
    tmp[2] = curDAG.getTargetConstant(ARM_AM.getAM2Opc(addsub, (int) shAmt, shopcVal), new EVT(MVT.i32));
    return AddrMode2Type.AM2_SHOP;
  }

  protected boolean selectAddrMode6(SDNode parent, SDValue n, SDValue[] tmp) {
    // tmp = {add, align}
    tmp[0] = n;

    int alignment = 0;
    if (parent instanceof SDNode.LSBaseSDNode) {
      SDNode.LSBaseSDNode lsn = (SDNode.LSBaseSDNode) parent;
      int memSize = lsn.getMemoryVT().getSizeInBits() / 8;
      int lsnAlign = lsn.getAlignment();
      if (lsnAlign > memSize && memSize > 1)
        alignment = lsnAlign;
    }
    else {
      alignment = ((SDNode.MemIntrinsicSDNode)parent).getAlignment();
    }
    tmp[1] = curDAG.getTargetConstant(alignment, new EVT(MVT.i32));
    return true;
  }

  protected boolean selectAddrMode6Offset(SDNode op, SDValue n, SDValue[] tmp) {
    // tmp = {offset}
    SDNode.LSBaseSDNode ldSt = (SDNode.LSBaseSDNode) op;
    MemIndexedMode am = ldSt.getAddressingMode();
    if (am != MemIndexedMode.POST_INC)
      return false;

    tmp[0] = n;
    if (n.getNode() instanceof SDNode.ConstantSDNode) {
      SDNode.ConstantSDNode nc = (SDNode.ConstantSDNode) n.getNode();
      if (nc.getZExtValue() * 8 == ldSt.getMemoryVT().getSizeInBits())
        tmp[0] = curDAG.getRegister(0, new EVT(MVT.i32));
    }
    return true;
  }

  protected boolean selectAddrModePC(SDValue n, SDValue[] tmp) {
    if (n.getOpcode() == ARMISD.PIC_ADD && n.hasOneUse()) {
      // offset
      tmp[0] = n.getOperand(0);
      SDValue n1 = n.getOperand(1);
      // label.
      tmp[1] = curDAG.getTargetConstant(((SDNode.ConstantSDNode) n1.getNode()).getZExtValue(), new EVT(MVT.i32));
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
    MemIndexedMode am = opc == ISD.LOAD ? ((SDNode.LoadSDNode) n.getNode()).getAddressingMode() :
        ((SDNode.StoreSDNode) n.getNode()).getAddressingMode();
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
    MemIndexedMode am = opc == ISD.LOAD ? ((SDNode.LoadSDNode) n.getNode()).getAddressingMode() :
        ((SDNode.StoreSDNode) n.getNode()).getAddressingMode();
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
    MemIndexedMode am = opc == ISD.LOAD ? ((SDNode.LoadSDNode) n.getNode()).getAddressingMode() :
        ((SDNode.StoreSDNode) n.getNode()).getAddressingMode();
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
        int fi = ((SDNode.FrameIndexSDNode) n.getNode()).getFrameIndex();
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
        int fi = ((SDNode.FrameIndexSDNode) tmp[0].getNode()).getFrameIndex();
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

  protected boolean selectT2AddrModeImm8Offset(SDNode op, SDValue n, SDValue[] offImm) {
    Util.assertion(offImm.length == 1);
    OutRef<SDValue> arg = new OutRef<>();
    boolean res = selectT2AddrModeImm8Offset(op, n, arg);
    offImm[0] = arg.get();
    return res;
  }

  protected boolean selectT2AddrModeImm8Offset(SDNode op, SDValue n, OutRef<SDValue> offImm) {
    int opcode = op.getOpcode();
    MemIndexedMode am = opcode == ISD.LOAD ? ((SDNode.LoadSDNode)op).getAddressingMode() :
        ((SDNode.StoreSDNode)op).getAddressingMode();
    OutRef<Integer> rhsc = new OutRef<>(0);
    // test if the offset is a constant in 8 bits.
    if (isScaledConstantInRange(n, 1, 0, 0x100, rhsc)) {
      offImm.set(am == MemIndexedMode.PRE_INC || am == MemIndexedMode.PRE_DEC ?
          curDAG.getTargetConstant(rhsc.get(), new EVT(MVT.i32)) :
          curDAG.getTargetConstant(-rhsc.get(), new EVT(MVT.i32)));
      return true;
    }
    offImm.set(null);
    return false;
  }

  protected boolean selectThumbAddrModeRI5S1(SDValue n, SDValue[] tmp) {
    return selectThumbAddrModeRI(n, tmp, 1);
  }

  protected boolean selectThumbAddrModeRI5S2(SDValue n, SDValue[] tmp) {
    return selectThumbAddrModeRI(n, tmp, 2);
  }

  protected boolean selectThumbAddrModeRI5S4(SDValue n, SDValue[] tmp) {
    return selectThumbAddrModeRI(n, tmp, 4);
  }

  protected boolean selectThumbAddrModeImm5S1(SDValue n, SDValue[] tmp) {
    return selectThumbAddrModeImm5S(n, 1, tmp);
  }

  protected boolean selectThumbAddrModeImm5S2(SDValue n, SDValue[] tmp) {
    return selectThumbAddrModeImm5S(n, 2, tmp);
  }

  protected boolean selectThumbAddrModeImm5S4(SDValue n, SDValue[] tmp) {
    return selectThumbAddrModeImm5S(n, 4, tmp);
  }

  private boolean selectThumbAddrModeRI(SDValue n, SDValue[] tmp, int scale) {
    if (scale == 4) {
      SDValue[] tmpRes = new SDValue[2];
      // select tLDRspi / tSTRspi instead.
      if (selectThumbAddrModeSP(n, tmpRes))
        return false;

      // select tLDRpci instead.
      if (n.getOpcode() == ARMISD.Wrapper && n.getOperand(0).getOpcode() == ISD.TargetConstantPool)
        return false;
    }

    if (!curDAG.isBaseWithConstantOffset(n))
      return false;

    // thumb doens't have [sp, r] address mode.
    if ((n.getOperand(0).getNode() instanceof SDNode.RegisterSDNode &&
        ((SDNode.RegisterSDNode)n.getOperand(0).getNode()).getReg() == ARMGenRegisterNames.SP) ||
        (n.getOperand(1).getNode() instanceof SDNode.RegisterSDNode &&
            ((SDNode.RegisterSDNode)n.getOperand(1).getNode()).getReg() == ARMGenRegisterNames.SP)) {
      return false;
    }

    OutRef<Integer> rhsc = new OutRef<>(0);
    if (isScaledConstantInRange(n.getOperand(1), scale, 0, 32, rhsc))
      return false;

    tmp[0] = n.getOperand(0);
    tmp[1] = n.getOperand(1);
    return true;
  }

  private boolean selectThumbAddrModeImm5S(SDValue n, int scale, SDValue[] tmp) {
    // tmp = {base, offImm}
    if (scale == 4) {
      SDValue[] tmpRes = new SDValue[2];
      // select tLDRspi / tSTRspi instead.
      if (selectThumbAddrModeSP(n, tmpRes))
        return false;

      // select tLDRpci instead.
      if (n.getOpcode() == ARMISD.Wrapper &&
          n.getOperand(0).getOpcode() == ISD.TargetConstantPool)
        return false;
    }

    if (!curDAG.isBaseWithConstantOffset(n)) {
      if (n.getOpcode() == ARMISD.Wrapper &&
          !(subtarget.useMovt() && n.getOperand(0).getOpcode() == ISD.TargetGlobalAddress)) {
        tmp[0] = n.getOperand(0);
      }
      else
        tmp[0] = n;

      tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
      return true;
    }

    // thumb doens't have [sp, #imm5] address mode for non-zero imm5.
    if ((n.getOperand(0).getNode() instanceof SDNode.RegisterSDNode &&
        ((SDNode.RegisterSDNode)n.getOperand(0).getNode()).getReg() == ARMGenRegisterNames.SP) ||
        (n.getOperand(1).getNode() instanceof SDNode.RegisterSDNode &&
            ((SDNode.RegisterSDNode)n.getOperand(1).getNode()).getReg() == ARMGenRegisterNames.SP)) {
      long lhsc = 0;
      if (n.getOperand(0).getNode() instanceof SDNode.ConstantSDNode)
        lhsc = ((SDNode.ConstantSDNode)n.getOperand(0).getNode()).getZExtValue();

      long rhsc = 0;
      if (n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode)
        rhsc = ((SDNode.ConstantSDNode)n.getOperand(1).getNode()).getZExtValue();

      if (lhsc != 0 || rhsc != 0) return false;
      tmp[0] = n;
      tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
      return true;
    }

    // If the RHS is + imm5 * scale, fold into addr mode.
    OutRef<Integer> rhsc = new OutRef<>(0);
    if (isScaledConstantInRange(n.getOperand(1), scale, 0, 32, rhsc)) {
      tmp[0] = n.getOperand(0);
      tmp[1] = curDAG.getTargetConstant(rhsc.get(), new EVT(MVT.i32));
      return true;
    }

    tmp[0] = n.getOperand(0);
    tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
    return true;
  }

  protected boolean selectThumbAddrModeSP(SDValue n, SDValue[] tmp) {
    // tmp = {base, offImm}
    if (n.getOpcode() == ISD.FrameIndex) {
      int fi = ((SDNode.FrameIndexSDNode)n.getNode()).getFrameIndex();
      tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
      tmp[1] = curDAG.getTargetConstant(0, new EVT(MVT.i32));
      return true;
    }

    if (!curDAG.isBaseWithConstantOffset(n))
      return false;

    if (n.getOperand(0).getOpcode() == ISD.FrameIndex ||
        ((n.getOperand(0).getNode() instanceof SDNode.RegisterSDNode) &&
            ((SDNode.RegisterSDNode) n.getOperand(0).getNode()).getReg() == ARMGenRegisterNames.SP)) {
      // if the rhs is + imm8 * scale, fold into addr mode.
      OutRef<Integer> rhsc = new OutRef<>(0);
      if (isScaledConstantInRange(n.getOperand(1), /*scale*/ 4, 0, 256, rhsc)) {
        tmp[0] = n.getOperand(0);
        if (tmp[0].getOpcode() == ISD.FrameIndex) {
          int fi = ((SDNode.FrameIndexSDNode)n.getOperand(0).getNode()).getFrameIndex();
          tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
        }
        tmp[1] = curDAG.getTargetConstant(rhsc.get(), new EVT(MVT.i32));
        return true;
      }
    }
    return false;
  }

  /**
   * The address mode 5 is designed for Load/Store coprocessor instruction.
   * @param n
   * @param tmp
   * @return
   */
  protected boolean selectAddrMode5(SDValue n, SDValue[] tmp) {
    // tmp = {base, offset}
    if (!curDAG.isBaseWithConstantOffset(n)) {
      tmp[0] = n;
      if (n.getOpcode() == ISD.FrameIndex) {
        int fi = ((SDNode.FrameIndexSDNode)n.getNode()).getFrameIndex();
        tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
      }
      else if (n.getOpcode() == ARMISD.Wrapper && !(subtarget.useMovt() &&
          n.getOperand(0).getOpcode() == ISD.TargetGlobalAddress)) {
        tmp[0] = n.getOperand(0);
      }
      tmp[1] = curDAG.getTargetConstant(ARM_AM.getAM5Opc(ARM_AM.AddrOpc.add, 0), new EVT(MVT.i32));
      return true;
    }

    // if the rhs is +/- imm8 (-255 to 256), fold it into the addr mode.
    OutRef<Integer> rhsc = new OutRef<>(0);
    if (isScaledConstantInRange(n.getOperand(1), /*scale*/4, -255, 256, rhsc)) {
      tmp[0] = n.getOperand(0);
      if (tmp[0].getOpcode() == ISD.FrameIndex) {
        int fi = ((SDNode.FrameIndexSDNode)tmp[0].getNode()).getFrameIndex();
        tmp[0] = curDAG.getTargetFrameIndex(fi, new EVT(tli.getPointerTy()));
      }
      ARM_AM.AddrOpc addsub = ARM_AM.AddrOpc.add;
      if (rhsc.get() < 0) {
        rhsc.set(-rhsc.get());
        addsub = ARM_AM.AddrOpc.sub;
      }
      tmp[1] = curDAG.getTargetConstant(ARM_AM.getAM5Opc(addsub, rhsc.get()), new EVT(MVT.i32));
      return true;
    }

    tmp[0] = n;
    tmp[1] = curDAG.getTargetConstant(ARM_AM.getAM5Opc(ARM_AM.AddrOpc.add, 0), new EVT(MVT.i32));
    return true;
  }

  protected boolean selectThumbAddrModeRR(SDValue n, SDValue[] tmp) {
    // tmp = {base, offset}
    if (n.getOpcode() != ISD.ADD && !curDAG.isBaseWithConstantOffset(n)) {
      if (!(n.getNode() instanceof SDNode.ConstantSDNode) || ((SDNode.ConstantSDNode)n.getNode()).isNullValue())
        return false;

      tmp[0] = tmp[1] = n;
      return true;
    }

    tmp[0] = n.getOperand(0);
    tmp[1] = n.getOperand(1);
    return true;
  }

  private SDNode selectVTBL(SDNode n, boolean isExt, int numVecs, int opc) {
    Util.assertion(numVecs >= 2 && numVecs <= 4, "VTBL numVecs out of range");
    EVT vt = n.getValueType(0);
    int firstTblReg = isExt ? 2 : 1;

    // form a REG_SEQUENCE to force register allocation.
    SDValue regSeq;
    SDValue v0 = n.getOperand(firstTblReg + 0);
    SDValue v1 = n.getOperand(firstTblReg + 1);
    if (numVecs == 2)
      regSeq = new SDValue(pairDRegs(new EVT(MVT.v16i8), v0, v1), 0);
    else {
      SDValue v2 = n.getOperand(firstTblReg + 2);
      SDValue v3 = numVecs == 3 ? new SDValue(curDAG.getMachineNode(TargetOpcode.IMPLICIT_DEF, vt), 0) :
          n.getOperand(firstTblReg + 3);
      regSeq = new SDValue(quadDRegs(new EVT(MVT.v4i64), v0, v1, v2, v3), 0);
    }

    ArrayList<SDValue> ops = new ArrayList<>();
    if (isExt)
      ops.add(n.getOperand(1));

    ops.add(regSeq);
    ops.add(n.getOperand(firstTblReg + numVecs));
    ops.add(getAL(curDAG));
    ops.add(curDAG.getRegister(0, new EVT(MVT.i32)));
    SDValue[] tmp = new SDValue[ops.size()];
    ops.toArray(tmp);
    return curDAG.getMachineNode(opc, vt, tmp);
  }

  private SDValue getVLDSTAlign(SDValue align, int numVecs, boolean is64BitVector) {
    int numRegs = numVecs;
    if (!is64BitVector && numRegs < 3)
      numRegs *= 2;

    long alignment = ((SDNode.ConstantSDNode)align.getNode()).getZExtValue();
    if (alignment >= 32 && numRegs == 4)
      alignment = 32;
    else if (alignment >= 16 && (numRegs == 2 || numRegs == 4))
      alignment = 16;
    else if (alignment >= 8)
      alignment = 8;
    else
      alignment = 0;
    return curDAG.getTargetConstant(alignment, new EVT(MVT.i32));
  }

  private SDNode selectVST(SDNode n, boolean isUpdating, int numVecs,
                           int[] dOpcodes, int[] qOpcodes0, int[] qOpcodes1) {
    Util.assertion(numVecs >= 1 && numVecs <= 4, "VST numVecs out of range!");
    int addrOpIdx = isUpdating ? 1: 2;
    int vec0Idx = 3; // addrOpIdx + (isUpdating ? 2 : 1)
    SDValue[] tmp = new SDValue[2];
    if (!selectAddrMode6(n, n.getOperand(addrOpIdx), tmp))
      return null;

    SDValue memAddr = tmp[0], align = tmp[1];
    MachineMemOperand mmo = ((SDNode.MemIntrinsicSDNode)n).getMemOperand();
    SDValue chain = n.getOperand(0);
    EVT vt = n.getOperand(vec0Idx).getValueType();
    boolean is64BitVector = vt.is64BitVector();
    align = getVLDSTAlign(align, numVecs, is64BitVector);

    int opcodeIndex = 0;
    switch (vt.getSimpleVT().simpleVT) {
      // 64 bit registers
      case MVT.v8i8: opcodeIndex = 0; break;
      case MVT.v4i16: opcodeIndex = 1; break;
      case MVT.v2f32:
      case MVT.v2i32: opcodeIndex = 2; break;
      case MVT.v1i64: opcodeIndex = 3; break;
      // 128 bit registers.
      case MVT.v16i8: opcodeIndex = 0; break;
      case MVT.v8i16: opcodeIndex = 1; break;
      case MVT.v4f32:
      case MVT.v4i32: opcodeIndex = 2; break;
      case MVT.v2i64: opcodeIndex = 3;
        Util.assertion(numVecs == 1, "v2i64 only supported for VST1");
        break;
      default:
        Util.shouldNotReachHere("unhandled vst type!");
    }

    ArrayList<EVT> resTys = new ArrayList<>();
    // updating means we have to update the base register after the VLD or VST.
    if (isUpdating)
      resTys.add(new EVT(MVT.i32));

    resTys.add(new EVT(MVT.Other));

    SDValue pred = getAL(curDAG);
    SDValue reg0 = curDAG.getRegister(0, new EVT(MVT.i32));
    ArrayList<SDValue> ops = new ArrayList<>();

    // Double registers and VST1/VST2 quad registers are directly supported.
    if (is64BitVector || numVecs <= 2) {
      SDValue srcReg;
      if (numVecs == 1) {
        srcReg = n.getOperand(vec0Idx);
      }
      else if (is64BitVector) {
        SDValue v0 = n.getOperand(vec0Idx + 0);
        SDValue v1 = n.getOperand(vec0Idx + 1);
        if (numVecs == 2)
          srcReg = new SDValue(pairDRegs(new EVT(MVT.v2i64), v0, v1), 0);
        else {
          SDValue v2 = n.getOperand(vec0Idx + 2);
          SDValue v3 = numVecs == 3 ? new SDValue(curDAG.getMachineNode(TargetOpcode.IMPLICIT_DEF, vt), 0) :
              n.getOperand(vec0Idx + 3);
          srcReg = new SDValue(quadDRegs(new EVT(MVT.v4i64), v0, v1, v2, v3), 0);
        }
      }
      else {
        SDValue q0 = n.getOperand(vec0Idx);
        SDValue q1 = n.getOperand(vec0Idx + 1);
        srcReg = new SDValue(pairQRegs(new EVT(MVT.v4i64), q0, q1), 0);
      }

      int opc = is64BitVector ? dOpcodes[opcodeIndex] : qOpcodes0[opcodeIndex];
      ops.add(memAddr);
      ops.add(align);
      if (isUpdating) {
        SDValue inc = n.getOperand(addrOpIdx + 1);
        ops.add(inc.getNode() instanceof SDNode.ConstantSDNode ? reg0 : inc);
      }
      ops.add(srcReg);
      ops.add(pred);
      ops.add(reg0);
      ops.add(chain);
      SDValue[] res = new SDValue[ops.size()];
      ops.toArray(res);
      EVT[] vts = new EVT[resTys.size()];
      resTys.toArray(vts);
      SDNode.MachineSDNode vst = curDAG.getMachineNode(opc, curDAG.makeVTList(vts), res);
      // transfer memory operand.
      vst.setMemRefs(new MachineMemOperand[] {mmo});
      return vst;
    }

    // Otherwise, quad registers are stored with two separate instructions,
    // where one stores the even registers and the other stores the odd registers.

    // Form the QQQQ REG_SEQUENCE.
    SDValue v0 = n.getOperand(vec0Idx + 0);
    SDValue v1 = n.getOperand(vec0Idx + 1);
    SDValue v2 = n.getOperand(vec0Idx + 2);
    SDValue v3 = numVecs == 3 ?
        new SDValue(curDAG.getMachineNode(TargetOpcode.IMPLICIT_DEF, vt), 0) :
        n.getOperand(vec0Idx + 3);

    SDValue regSeq = new SDValue(quadQRegs(new EVT(MVT.v8i64), v0, v1, v2, v3), 0);

    // Store the even D registers.  This is always an updating store, so that it
    // provides the address to the second store for the odd subregs.
    SDValue[] opsA = new SDValue[] {memAddr, align, reg0, regSeq, pred, reg0, chain};
    SDNode.MachineSDNode vsta = curDAG.getMachineNode(qOpcodes0[opcodeIndex], memAddr.getValueType(),
        new EVT(MVT.Other), opsA);
    vsta.setMemRefs(new MachineMemOperand[] {mmo});
    chain = new SDValue(vsta, 1);

    ops.add(new SDValue(vsta, 0));
    ops.add(align);
    if (isUpdating) {
      SDValue inc = n.getOperand(addrOpIdx + 1);
      Util.assertion(inc.getNode() instanceof SDNode.ConstantSDNode,
          "only constant post-increment update allowed for VST3/4");
      ops.add(reg0);
    }

    ops.add(regSeq);
    ops.add(pred);
    ops.add(reg0);
    ops.add(chain);

    SDValue[] res = new SDValue[ops.size()];
    ops.toArray(res);
    EVT[] vts = new EVT[resTys.size()];
    resTys.toArray(vts);
    SDNode.MachineSDNode vstb = curDAG.getMachineNode(qOpcodes1[opcodeIndex], curDAG.makeVTList(vts), res);
    vstb.setMemRefs(new MachineMemOperand[]{mmo});
    return vstb;
  }

  private SDNode selectVLD(SDNode n, boolean isUpdating, int numVecs,
                           int[] dOpcodes, int[] qOpcodes0, int[] qOpcodes1) {
    Util.assertion(numVecs >= 1 && numVecs <= 4, "VST numVecs out of range!");
    int addrOpIdx = isUpdating ? 1 : 2;
    SDValue[] tmp = new SDValue[2];
    if (!selectAddrMode6(n, n.getOperand(addrOpIdx), tmp))
      return null;

    SDValue memAddr = tmp[0], align = tmp[1];
    SDValue chain = n.getOperand(0);
    EVT vt = n.getValueType(0);
    boolean is64BitVector = vt.is64BitVector();
    align = getVLDSTAlign(align, numVecs, is64BitVector);

    int opcodeIndex = 0;
    switch (vt.getSimpleVT().simpleVT) {
      // 64 bit registers
      case MVT.v8i8:
        opcodeIndex = 0;
        break;
      case MVT.v4i16:
        opcodeIndex = 1;
        break;
      case MVT.v2f32:
      case MVT.v2i32:
        opcodeIndex = 2;
        break;
      case MVT.v1i64:
        opcodeIndex = 3;
        break;
      // 128 bit registers.
      case MVT.v16i8:
        opcodeIndex = 0;
        break;
      case MVT.v8i16:
        opcodeIndex = 1;
        break;
      case MVT.v4f32:
      case MVT.v4i32:
        opcodeIndex = 2;
        break;
      case MVT.v2i64:
        opcodeIndex = 3;
        Util.assertion(numVecs == 1, "v2i64 only supported for VST1");
        break;
      default:
        Util.shouldNotReachHere("unhandled vst type!");
    }

    EVT resTy;
    if (numVecs == 1)
      resTy = vt;
    else {
      int numResTyElts = numVecs == 3 ? 4 : numVecs;
      if (!is64BitVector)
        numResTyElts *= 2;
      resTy = EVT.getVectorVT(curDAG.getContext(), new EVT(MVT.i64), numResTyElts);
    }

    ArrayList<EVT> resTys = new ArrayList<>();
    // updating means we have to update the base register after the VLD or VST.
    if (isUpdating)
      resTys.add(new EVT(MVT.i32));
    resTys.add(new EVT(MVT.Other));

    SDValue pred = getAL(curDAG);
    SDValue reg0 = curDAG.getRegister(0, new EVT(MVT.i32));
    SDNode.MachineSDNode vld = null;
    ArrayList<SDValue> ops = new ArrayList<>();

    // Double registers and VST1/VST2 quad registers are directly supported.
    if (is64BitVector || numVecs <= 2) {
      int opc = isUpdating ? dOpcodes[opcodeIndex] : qOpcodes0[opcodeIndex];
      ops.add(memAddr);
      ops.add(align);
      if (isUpdating) {
        SDValue inc = n.getOperand(addrOpIdx + 1);
        ops.add(inc.getNode() instanceof SDNode.ConstantSDNode ? reg0 : inc);
      }
      ops.add(pred);
      ops.add(reg0);
      ops.add(chain);
      SDValue[] res = new SDValue[ops.size()];
      ops.toArray(res);
      EVT[] vts = new EVT[resTys.size()];
      resTys.toArray(vts);
      vld = curDAG.getMachineNode(opc, curDAG.makeVTList(vts), res);
    }
    else {
      // Otherwise, quad registers are stored with two separate instructions,
      // where one stores the even registers and the other stores the odd registers.
      EVT addrTy = memAddr.getValueType();
      // load the even subregs
      SDValue implDef = new SDValue(curDAG.getMachineNode(TargetOpcode.IMPLICIT_DEF, resTy), 0);

      SDValue[] opsA = new SDValue[]{memAddr, align, reg0, implDef, pred, reg0, chain};
      SDNode.MachineSDNode vlda = curDAG.getMachineNode(qOpcodes0[opcodeIndex], resTy,
          addrTy, new EVT(MVT.Other), opsA);
      chain = new SDValue(vlda, 2);

      // load the odd subregs.
      ops.add(new SDValue(vlda, 0));
      ops.add(align);

      if (isUpdating) {
        SDValue inc = n.getOperand(addrOpIdx + 1);
        Util.assertion(inc.getNode() instanceof SDNode.ConstantSDNode,
            "only constant post-increment update allowed for VST3/4");
        ops.add(reg0);
      }

      ops.add(new SDValue(vlda, 0));
      ops.add(pred);
      ops.add(reg0);
      ops.add(chain);

      SDValue[] res = new SDValue[ops.size()];
      ops.toArray(res);
      EVT[] vts = new EVT[resTys.size()];
      resTys.toArray(vts);
      vld = curDAG.getMachineNode(qOpcodes1[opcodeIndex], curDAG.makeVTList(vts), res);
    }

    // transfer memory operands.
    MachineMemOperand[] mmos = {((SDNode.MemIntrinsicSDNode)n).getMemOperand()};
    vld.setMemRefs(mmos);

    if (numVecs == 1)
      return vld;

      // extract out the subregisters.
    SDValue superReg = new SDValue(vld, 0);
    int sub0 = is64BitVector ? ARMGenRegisterInfo.dsub_0 : ARMGenRegisterInfo.qsub_0;
    for (int vec = 0; vec < numVecs; ++vec)
      replaceUses(new SDValue(n, vec), curDAG.getTargetExtractSubreg(sub0 + vec, vt, superReg));

    replaceUses(new SDValue(n, numVecs), new SDValue(vld, 1));
    if (isUpdating)
      replaceUses(new SDValue(n, numVecs + 1), new SDValue(vld, 2));
    return null;
  }

  private SDNode selectVLDSTLane(SDNode n, boolean isLoad, boolean isUpdating,
                                 int numVecs, int[] dOpcodes, int[] qOpcodes) {
      Util.assertion(numVecs >= 2 && numVecs <= 4, "VST numVecs out of range!");
      int addrOpIdx = isUpdating ? 1: 2;
      int vec0Idx = 3; // addrOpIdx + (isUpdating ? 2 : 1)
      SDValue[] tmp = new SDValue[2];
      if (!selectAddrMode6(n, n.getOperand(addrOpIdx), tmp))
        return null;

      SDValue memAddr = tmp[0], align = tmp[1];
      MachineMemOperand mmo = ((SDNode.MemIntrinsicSDNode)n).getMemOperand();
      SDValue chain = n.getOperand(0);
      long lane = ((SDNode.ConstantSDNode)n.getOperand(vec0Idx + numVecs).getNode()).getZExtValue();
      EVT vt = n.getOperand(vec0Idx).getValueType();
      boolean is64BitVector = vt.is64BitVector();

      long alignment = 0;
      if (numVecs != 3) {
        alignment = ((SDNode.ConstantSDNode)align.getNode()).getZExtValue();
        int numBytes = numVecs * vt.getVectorElementType().getSizeInBits()/8;
        if (alignment > numBytes)
          alignment = numBytes;
        if (alignment < 8 && alignment < numBytes)
          alignment = 0;

        alignment = (alignment & ~alignment);
        if (alignment == 1)
          alignment = 0;
      }

      align = curDAG.getTargetConstant(alignment, new EVT(MVT.i32));

      int opcodeIndex = 0;
      switch (vt.getSimpleVT().simpleVT) {
        // 64 bit registers
        case MVT.v8i8: opcodeIndex = 0; break;
        case MVT.v4i16: opcodeIndex = 1; break;
        case MVT.v2f32:
        case MVT.v2i32: opcodeIndex = 2; break;

        case MVT.v8i16: opcodeIndex = 0; break;
        case MVT.v4f32:
        case MVT.v4i32: opcodeIndex = 1; break;
        default:
          Util.shouldNotReachHere("unhandled vst type!");
      }

      ArrayList<EVT> resTys = new ArrayList<>();
      if (isLoad) {
        int resTyElts = numVecs == 3 ? 4 : numVecs;
        if (!is64BitVector)
          resTyElts *= 2;
        resTys.add(EVT.getVectorVT(curDAG.getContext(), new EVT(MVT.i64), resTyElts));
      }

      // updating means we have to update the base register after the VLD or VST.
      if (isUpdating)
        resTys.add(new EVT(MVT.i32));

      resTys.add(new EVT(MVT.Other));

      SDValue pred = getAL(curDAG);
      SDValue reg0 = curDAG.getRegister(0, new EVT(MVT.i32));
      ArrayList<SDValue> ops = new ArrayList<>();

      ops.add(memAddr);
      ops.add(align);
      if (isUpdating) {
        SDValue inc = n.getOperand(addrOpIdx + 1);
        ops.add(inc.getNode() instanceof SDNode.ConstantSDNode ? reg0 : inc);
      }

      SDValue superReg;
      SDValue v0 = n.getOperand(vec0Idx + 0);
      SDValue v1 = n.getOperand(vec0Idx + 1);
      if (numVecs == 2) {
        if (is64BitVector)
          superReg = new SDValue(pairDRegs(new EVT(MVT.v2i64), v0, v1), 0);
        else
          superReg = new SDValue(pairQRegs(new EVT(MVT.v4i64), v0, v1), 0);
      }
      else {
        SDValue v2 = n.getOperand(vec0Idx + 2);
        SDValue v3 = numVecs == 3 ? new SDValue(curDAG.getMachineNode(TargetOpcode.IMPLICIT_DEF, vt), 0) :
            n.getOperand(vec0Idx + 3);
        if (is64BitVector)
          superReg = new SDValue(quadDRegs(new EVT(MVT.v4i64), v0, v1, v2, v3), 0);
        else
          superReg = new SDValue(quadQRegs(new EVT(MVT.v8i64), v0, v1, v2, v3), 0);
      }

      ops.add(superReg);
      ops.add(getI32Imm(lane));
      ops.add(pred);
      ops.add(reg0);
      ops.add(chain);

      int opc = is64BitVector ? dOpcodes[opcodeIndex] : qOpcodes[opcodeIndex];
      SDValue[] res = new SDValue[ops.size()];
    ops.toArray(res);
    EVT[] vts = new EVT[resTys.size()];
    resTys.toArray(vts);
    SDNode.MachineSDNode vldLn = curDAG.getMachineNode(opc, curDAG.makeVTList(vts), res);
    // transfer memory operand.
    vldLn.setMemRefs(new MachineMemOperand[] {mmo});
    if (isLoad)
      return vldLn;

    // extract subregisters.
    superReg = new SDValue(vldLn, 0);
    int sub0 = is64BitVector ? ARMGenRegisterInfo.dsub_0 : ARMGenRegisterInfo.qsub_0;
    for (int vec = 0; vec < numVecs; ++vec)
      replaceUses(new SDValue(n, vec), curDAG.getTargetExtractSubreg(sub0 + vec, vt, superReg));

    replaceUses(new SDValue(n, numVecs), new SDValue(vldLn, 1));
    if (isUpdating)
      replaceUses(new SDValue(n, numVecs + 1), new SDValue(vldLn, 2));
    return null;
  }

  private SDNode selectVLDDup(SDNode n, boolean isUpdating, int numVecs, int[] opcodes) {
    Util.assertion(numVecs >=2 && numVecs <= 4, "VLDDup NumVecs out-of-range");

    SDValue[] tmp = new SDValue[2];
    if (!selectAddrMode6(n, n.getOperand(1), tmp))
      return null;

    SDValue memAddr = tmp[0], align = tmp[1];
    MachineMemOperand[] mmo = {((SDNode.MemIntrinsicSDNode)n).getMemOperand()};

    SDValue chain = n.getOperand(0);
    EVT vt = n.getValueType(0);

    long alignment = 0;
    if (numVecs != 3) {
      alignment = ((SDNode.ConstantSDNode)align.getNode()).getZExtValue();
      int numBytes = numVecs * vt.getVectorElementType().getSizeInBits()/8;
      if (alignment > numBytes)
        alignment = numBytes;
      if (alignment < 8 && alignment < numBytes)
        alignment = 0;
      // Alignment must be a power of two; make sure of that.
      alignment = (alignment & -alignment);
      if (alignment == 1)
        alignment = 0;
    }
    align = curDAG.getTargetConstant(alignment, new EVT(MVT.i32));

    int opcodeIndex;
    switch (vt.getSimpleVT().simpleVT) {
      default: Util.shouldNotReachHere("unhandled vld-dup type");
      case MVT.v8i8:  opcodeIndex = 0; break;
      case MVT.v4i16: opcodeIndex = 1; break;
      case MVT.v2f32:
      case MVT.v2i32: opcodeIndex = 2; break;
    }

    SDValue pred = getAL(curDAG);
    SDValue reg0 = curDAG.getRegister(0, MVT.i32);
    SDValue superReg;
    int opc = opcodes[opcodeIndex];
    ArrayList<SDValue> ops = new ArrayList<>();
    ops.add(memAddr);
    ops.add(align);
    if (isUpdating) {
      SDValue inc = n.getOperand(2);
      ops.add(inc.getNode() instanceof SDNode.ConstantSDNode ? reg0 : inc);
    }
    ops.add(pred);
    ops.add(reg0);
    ops.add(chain);

    int resTyElts = (numVecs == 3) ? 4 : numVecs;
    ArrayList<EVT> resTys = new ArrayList<>();
    resTys.add(EVT.getVectorVT(curDAG.getContext(), new EVT(MVT.i64), resTyElts));
    if (isUpdating)
      resTys.add(new EVT(MVT.i32));
    resTys.add(new EVT(MVT.Other));
    SDValue[] res = new SDValue[ops.size()];
    ops.toArray(res);
    SDNode.MachineSDNode vldDup =
        curDAG.getMachineNode(opc, curDAG.getVTList(resTys), res);
    vldDup.setMemRefs(mmo);

    superReg = new SDValue(vldDup, 0);

    // Extract the subregisters.
    int subIdx = ARMGenRegisterInfo.dsub_0;
    for (int vec = 0; vec < numVecs; ++vec)
      replaceUses(new SDValue(n, vec),
          curDAG.getTargetExtractSubreg(subIdx+vec, vt, superReg));
    replaceUses(new SDValue(n, numVecs), new SDValue(vldDup, 1));
    if (isUpdating)
      replaceUses(new SDValue(n, numVecs + 1), new SDValue(vldDup, 2));
    return null;
  }

  static FunctionPass createARMISelDAG(ARMTargetMachine tm, TargetMachine.CodeGenOpt level) {
    return new ARMGenDAGISel(tm, level);
  }
}
