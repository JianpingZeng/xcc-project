/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.target.x86;

import backend.codegen.*;
import backend.codegen.dagisel.*;
import backend.codegen.dagisel.SDNode.*;
import backend.support.Attribute;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetMachine.CodeModel;
import backend.value.Function;
import backend.value.GlobalValue;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;
import java.util.Objects;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.dagisel.LoadExtType.EXTLOAD;
import static backend.codegen.dagisel.LoadExtType.NON_EXTLOAD;
import static backend.codegen.dagisel.MemIndexedMode.UNINDEXED;
import static backend.target.x86.X86ISelAddressMode.BaseType.FrameIndexBase;
import static backend.target.x86.X86ISelAddressMode.BaseType.RegBase;

public abstract class X86DAGISel extends SelectionDAGISel {
  protected X86TargetLowering tli;
  protected X86Subtarget subtarget;

  public X86DAGISel(X86TargetMachine tm, TargetMachine.CodeGenOpt optLevel) {
    super(tm, optLevel);
    subtarget = tm.getSubtarget();
    tli = tm.getTargetLowering();
  }

  /**
   * This hook allows targets to hack on the graph before
   * instruction selection starts.
   */
  @Override
  public void preprocessISelDAG() {
    Function f = mf.getFunction();
    optForSize = f.hasFnAttr(Attribute.OptimizeForSize);
    if (optLevel != TargetMachine.CodeGenOpt.None)
      preprocessForFPConvert();
  }

  public static X86DAGISel createX86DAGISel(X86TargetMachine tm, TargetMachine.CodeGenOpt level) {
    return new X86GenDAGISel(tm, level);
  }

  @Override
  public String getPassName() {
    return "X86 DAG To DAG Instruction Selector";
  }

  @Override
  public void emitFunctionEntryCode(Function fn, MachineFunction mf) {
    // If this is a main function, emit special code for it.
    MachineBasicBlock mbb = mf.getEntryBlock();
    if (fn.hasExternalLinkage() && fn.getName().equals("main")) {
      emitSpecialCodeForMain(mbb, mf.getFrameInfo());
    }
  }

  @Override
  public boolean isLegalAndProfitableToFold(SDNode node, SDNode use, SDNode root) {
    if (optLevel == TargetMachine.CodeGenOpt.None) return false;

    if (Objects.equals(use, root)) {
      switch (use.getOpcode()) {
        default:
          break;
        case ISD.ADD:
        case ISD.ADDC:
        case ISD.ADDE:
        case ISD.AND:
        case ISD.XOR:
        case ISD.OR: {
          SDValue op1 = use.getOperand(1);

          // If the other operand is a 8-bit immediate we should fold the immediate
          // instead. This reduces code size.
          // e.g.
          // movl 4(%esp), %eax
          // addl $4, %eax
          // vs.
          // movl $4, %eax
          // addl 4(%esp), %eax
          // The former is 2 bytes shorter. In case where the increment is 1, then
          // the saving can be 4 bytes (by using incl %eax).
          if (op1.getNode() instanceof ConstantSDNode) {
            ConstantSDNode imm = (ConstantSDNode) op1.getNode();
            if (imm.getAPIntValue().isSignedIntN(8))
              return false;
          }

          // If the other operand is a TLS address, we should fold it instead.
          // This produces
          // movl    %gs:0, %eax
          // leal    i@NTPOFF(%eax), %eax
          // instead of
          // movl    $i@NTPOFF, %eax
          // addl    %gs:0, %eax
          // if the block also has an access to a second TLS address this will save
          // a load.
          if (op1.getOpcode() == X86ISD.Wrapper) {
            SDValue val = op1.getOperand(0);
            if (val.getOpcode() == ISD.TargetGlobalTLSAddress)
              return false;
          }
        }
      }
    }
    // Call the super's method to cope with the generic situation.
    return super.isLegalAndProfitableToFold(node, use, root);
  }

  protected SDNode selectAtomic64(SDNode node, int opc) {
    SDValue chain = node.getOperand(0);
    SDValue in1 = node.getOperand(1);
    SDValue in2 = node.getOperand(2);
    SDValue in3 = node.getOperand(3);
    SDValue[] temp = new SDValue[5];
    if (!selectAddr(in1, in2, temp))
      return null;

    SDValue lsi = node.getOperand(4);
    SDValue[] ops = new SDValue[9];
    System.arraycopy(temp, 0, ops, 0, 5);
    ops[5] = in2;
    ops[6] = in3;
    ops[7] = lsi;
    ops[8] = chain;
    EVT[] vts = {new EVT(MVT.i32), new EVT(MVT.i32), new EVT(MVT.Other)};
    return curDAG.getTargetNode(opc, vts, ops);
  }

  protected SDNode selectAtomicLoadAdd(SDNode node, EVT vt) {
    if (node.hasAnyUseOfValue(0))
      return null;

    SDValue chain = node.getOperand(0);
    SDValue ptr = node.getOperand(1);
    SDValue val = node.getOperand(2);
    SDValue[] temp = new SDValue[5];
    if (!selectAddr(ptr, ptr, temp))
      return null;

    boolean isInc = false, isDec = false, isSub = false, isCN = false;
    ConstantSDNode cn = val.getNode() instanceof ConstantSDNode ? ((ConstantSDNode) val.getNode()) : null;
    if (cn != null) {
      isCN = true;
      long cnVal = cn.getSExtValue();
      if (cnVal == 1)
        isInc = true;
      else if (cnVal == -1)
        isDec = true;
      else if (cnVal >= 0)
        val = curDAG.getTargetConstant(cnVal, vt);
      else {
        isSub = true;
        val = curDAG.getTargetConstant(-cnVal, vt);
      }
    } else if (val.hasOneUse() && val.getOpcode() == ISD.SUB &&
        X86.isZeroMode(val.getOperand(0))) {
      isSub = true;
      val = val.getOperand(1);
    }

    int opc = 0;
    switch (vt.getSimpleVT().simpleVT) {
      default:
        return null;
      case MVT.i8:
        if (isInc)
          opc = X86GenInstrNames.LOCK_INC8m;
        else if (isDec)
          opc = X86GenInstrNames.LOCK_DEC8m;
        else if (isSub) {
          if (isCN)
            opc = X86GenInstrNames.LOCK_SUB8mi;
          else
            opc = X86GenInstrNames.LOCK_SUB8mr;
        } else {
          if (isCN)
            opc = X86GenInstrNames.LOCK_ADD8mi;
          else
            opc = X86GenInstrNames.LOCK_ADD8mr;
        }
        break;
      case MVT.i16:
        if (isInc)
          opc = X86GenInstrNames.LOCK_INC16m;
        else if (isDec)
          opc = X86GenInstrNames.LOCK_DEC16m;
        else if (isSub) {
          if (isCN) {
            if (predicate_i16immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_SUB16mi8;
            else
              opc = X86GenInstrNames.LOCK_SUB16mi;
          } else
            opc = X86GenInstrNames.LOCK_SUB16mr;
        } else {
          if (isCN) {
            if (predicate_i16immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_ADD16mi8;
            else
              opc = X86GenInstrNames.LOCK_ADD16mi;
          } else
            opc = X86GenInstrNames.LOCK_ADD16mr;
        }
        break;
      case MVT.i32:
        if (isInc)
          opc = X86GenInstrNames.LOCK_INC32m;
        else if (isDec)
          opc = X86GenInstrNames.LOCK_DEC32m;
        else if (isSub) {
          if (isCN) {
            if (predicate_i32immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_SUB32mi8;
            else
              opc = X86GenInstrNames.LOCK_SUB32mi;
          } else
            opc = X86GenInstrNames.LOCK_SUB32mr;
        } else {
          if (isCN) {
            if (predicate_i32immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_ADD32mi8;
            else
              opc = X86GenInstrNames.LOCK_ADD32mi;
          } else
            opc = X86GenInstrNames.LOCK_ADD32mr;
        }
        break;
      case MVT.i64:
        if (isInc)
          opc = X86GenInstrNames.LOCK_INC64m;
        else if (isDec)
          opc = X86GenInstrNames.LOCK_DEC64m;
        else if (isSub) {
          opc = X86GenInstrNames.LOCK_SUB64mr;
          if (isCN) {
            if (predicate_i64immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_SUB64mi8;
            else if (predicate_i64immSExt32(val.getNode()))
              opc = X86GenInstrNames.LOCK_SUB64mi32;
          }
        } else {
          opc = X86GenInstrNames.LOCK_ADD64mr;
          if (isCN) {
            if (predicate_i64immSExt8(val.getNode()))
              opc = X86GenInstrNames.LOCK_ADD64mi8;
            else if (predicate_i64immSExt32(val.getNode()))
              opc = X86GenInstrNames.LOCK_ADD64mi32;
          }
        }
    }
    SDValue undef = new SDValue(curDAG.getTargetNode(TargetInstrInfo.IMPLICIT_DEF, vt), 0);
    SDValue memOp = curDAG.getMemOperand(((MemSDNode) node).getMemOperand());
    if (isInc || isDec) {
      SDValue[] ops = {temp[0], temp[1], temp[2], temp[3], temp[4], memOp, chain};
      SDValue ret = new SDValue(curDAG.getTargetNode(opc, new EVT(MVT.Other), ops), 0);
      SDValue[] retVals = {undef, ret};
      return curDAG.getMergeValues(retVals).getNode();
    } else {
      SDValue[] ops = {temp[0], temp[1], temp[2], temp[3], temp[4], val, memOp, chain};
      SDValue ret = new SDValue(curDAG.getTargetNode(opc, new EVT(MVT.Other), ops), 0);
      SDValue[] retVals = {undef, ret};
      return curDAG.getMergeValues(retVals).getNode();
    }
  }

  public boolean predicate_i16immSExt8(SDNode n) { return false; }

  public boolean predicate_i32immSExt8(SDNode n) { return false; }

  public boolean predicate_i64immSExt8(SDNode n) { return false; }

  public boolean predicate_i64immSExt32(SDNode n) { return false; }

  protected boolean matchSegmentBaseAddress(SDValue val, X86ISelAddressMode am) {
    Util.assertion(val.getOpcode() == X86ISD.SegmentBaseAddress);
    SDValue segment = val.getOperand(0);
    if (am.segment.getNode() == null) {
      am.segment = segment;
      return false;
    }
    return true;
  }

  protected boolean matchLoad(SDValue val, X86ISelAddressMode am) {
    SDValue address = val.getOperand(1);
    if (address.getOpcode() == X86ISD.SegmentBaseAddress &&
        !matchSegmentBaseAddress(address, am))
      return false;

    return true;
  }

  protected boolean matchWrapper(SDValue val, X86ISelAddressMode am) {
    if (am.hasSymbolicDisplacement())
      return true;
    SDValue n0 = val.getOperand(0);
    CodeModel model = tm.getCodeModel();
    if (subtarget.is64Bit() &&
        (model == CodeModel.Small || model == CodeModel.Kernel) &&
        !am.hasBaseOrIndexReg() && val.getOpcode() == X86ISD.WrapperRIP) {
      if (n0.getNode() instanceof GlobalAddressSDNode) {
        GlobalAddressSDNode gv = (GlobalAddressSDNode) n0.getNode();
        long offset = am.disp + gv.getOffset();
        if (!X86.isOffsetSuitableForCodeModel(offset, model)) return true;
        am.gv = gv.getGlobalValue();
        am.disp = (int) offset;
        am.symbolFlags = gv.getTargetFlags();
      } else if (n0.getNode() instanceof ConstantPoolSDNode) {
        ConstantPoolSDNode cp = (ConstantPoolSDNode) n0.getNode();
        long offset = am.disp + cp.getOffset();
        if (!X86.isOffsetSuitableForCodeModel(offset, model)) return true;
        am.cp = cp.getConstantValue();
        am.align = cp.getAlign();
        am.disp = (int) offset;
        am.symbolFlags = cp.getTargetFlags();
      } else if (n0.getNode() instanceof ExternalSymbolSDNode) {
        ExternalSymbolSDNode es = (ExternalSymbolSDNode) n0.getNode();
        am.externalSym = es.getExtSymol();
        am.symbolFlags = es.getTargetFlags();
      } else {
        JumpTableSDNode j = (JumpTableSDNode) n0.getNode();
        am.jti = j.getJumpTableIndex();
        am.symbolFlags = j.getJumpTableIndex();
      }

      if (val.getOpcode() == X86ISD.WrapperRIP)
        am.setBaseReg(curDAG.getRegister(X86GenRegisterNames.RIP, new EVT(MVT.i64)));
      return false;
    }
    if (!subtarget.is64Bit() || ((model == CodeModel.Small || model == CodeModel.Kernel)
        && tm.getRelocationModel() == TargetMachine.RelocModel.Static)) {
      if (n0.getNode() instanceof GlobalAddressSDNode) {
        GlobalAddressSDNode gv = ((GlobalAddressSDNode) n0.getNode());
        am.gv = gv.getGlobalValue();
        am.disp += gv.getOffset();
        am.symbolFlags = gv.getTargetFlags();
      } else if (n0.getNode() instanceof ConstantPoolSDNode) {
        ConstantPoolSDNode cp = (ConstantPoolSDNode) n0.getNode();
        am.cp = cp.getConstantValue();
        am.align = cp.getAlign();
        am.disp += cp.getOffset();
        am.symbolFlags = cp.getTargetFlags();
      } else if (n0.getNode() instanceof ExternalSymbolSDNode) {
        ExternalSymbolSDNode es = (ExternalSymbolSDNode) n0.getNode();
        am.externalSym = es.getExtSymol();
        am.symbolFlags = es.getTargetFlags();
      } else {
        JumpTableSDNode j = (JumpTableSDNode) n0.getNode();
        am.jti = j.getJumpTableIndex();
        am.symbolFlags = j.getTargetFlags();
      }
      return false;
    }
    return true;
  }

  protected boolean matchAddress(SDValue val, X86ISelAddressMode am) {
    if (matchAddressRecursively(val, am, 0))
      return true;

    if (am.scale == 2 && am.baseType == X86ISelAddressMode.BaseType.RegBase &&
        am.base.reg.getNode() == null) {
      am.base.reg = am.indexReg;
      am.scale = 1;
    }

    if (tm.getCodeModel() == CodeModel.Small &&
        subtarget.is64Bit() &&
        am.scale == 1 &&
        am.baseType == X86ISelAddressMode.BaseType.RegBase &&
        am.base.reg.getNode() == null &&
        am.indexReg.getNode() == null &&
        am.symbolFlags == 0 &&
        am.hasSymbolicDisplacement())
      am.base.reg = curDAG.getRegister(X86GenRegisterNames.RIP, new EVT(MVT.i64));

    return false;
  }

  protected boolean matchAddressRecursively(SDValue n, X86ISelAddressMode am, int depth) {
    boolean is64Bit = subtarget.is64Bit();
    if (depth > 5)
      return matchAddressBase(n, am);

    CodeModel m = tm.getCodeModel();
    if (am.isRIPRelative()) {
      if (am.externalSym == null && am.jti != -1)
        return true;

      if (n.getNode() instanceof ConstantSDNode) {
        ConstantSDNode cn = (ConstantSDNode) n.getNode();
        long val = am.disp + cn.getSExtValue();
        if (X86.isOffsetSuitableForCodeModel(val, m, am.hasSymbolicDisplacement())) {
          am.disp = (int) val;
          return false;
        }
      }
      return true;
    }

    switch (n.getOpcode()) {
      default:
        break;
      case ISD.Constant: {
        long val = ((ConstantSDNode) n.getNode()).getSExtValue();
        if (!is64Bit || X86.isOffsetSuitableForCodeModel(am.disp + val,
            m, am.hasSymbolicDisplacement())) {
          am.disp += val;
          return false;
        }
        break;
      }
      case X86ISD.SegmentBaseAddress: {
        if (!matchSegmentBaseAddress(n, am))
          return false;
        break;
      }
      case X86ISD.Wrapper:
      case X86ISD.WrapperRIP: {
        if (!matchWrapper(n, am))
          return false;
        break;
      }
      case ISD.LOAD:
        if (!matchLoad(n, am))
          return false;
        break;
      case ISD.FrameIndex:
        if (am.baseType == RegBase && am.base.reg.getNode() == null) {
          am.baseType = FrameIndexBase;
          am.base.frameIndex = ((FrameIndexSDNode) n.getNode()).getFrameIndex();
          return false;
        }
        break;
      case ISD.SHL:
        if (am.indexReg.getNode() != null || am.scale == 1)
          break;
        if (n.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode csd = (ConstantSDNode) n.getOperand(1).getNode();
          long val = csd.getZExtValue();
          if (val == 1 || val == 2 || val == 3) {
            am.scale = 1 << val;
            SDValue shVal = n.getOperand(0);
            if (shVal.getOpcode() == ISD.ADD && shVal.hasOneUse() &&
                shVal.getOperand(1).getNode() instanceof ConstantSDNode) {
              am.indexReg = shVal.getOperand(0);
              ConstantSDNode addVal = (ConstantSDNode) shVal.getOperand(1).getNode();
              long disp = am.disp + (addVal.getSExtValue() << val);
              if (!is64Bit || X86.isOffsetSuitableForCodeModel(disp,
                  m, am.hasSymbolicDisplacement()))
                am.disp = Math.toIntExact(disp);
              else {
                am.indexReg = shVal;
              }
            } else {
              am.indexReg = shVal;
            }
            return false;
          }
          break;
        }
        // Fall through
      case ISD.SMUL_LOHI:
      case ISD.UMUL_LOHI:
        if (n.getResNo() != 0) break;
        // Fall through
      case ISD.MUL:
      case X86ISD.MUL_IMM:
        // X*[3,5,9] --> X+X*[2,4,8]
        if (am.baseType == RegBase &&
            am.base.reg.getNode() == null &&
            am.indexReg.getNode() == null) {
          if (n.getOperand(1).getNode() instanceof ConstantSDNode) {
            ConstantSDNode csd = (ConstantSDNode) n.getOperand(1).getNode();
            long val = csd.getZExtValue();
            if (val == 3 || val == 5 || val == 9) {
              am.scale = (int) (val - 1);
              SDValue mulVal = n.getOperand(0);
              SDValue reg;
              if (mulVal.getOpcode() == ISD.ADD && mulVal.hasOneUse() &&
                  mulVal.getOperand(1).getNode() instanceof ConstantSDNode) {
                reg = mulVal.getOperand(0);
                ConstantSDNode addVal = (ConstantSDNode) mulVal.getOperand(1).getNode();
                long disp = am.disp + addVal.getSExtValue() * val;

                if (!is64Bit || X86.isOffsetSuitableForCodeModel(disp,
                    m, am.hasSymbolicDisplacement()))
                  am.disp = Math.toIntExact(disp);
                else
                  reg = n.getOperand(0);
              } else {
                reg = n.getOperand(0);
              }

              am.indexReg = am.base.reg = reg;
              return false;
            }
          }
        }
        break;
      case ISD.SUB: {
        X86ISelAddressMode backup = am.clone();
        if (matchAddressRecursively(n.getOperand(0), am, depth + 1)) {
          am = backup;
          break;
        }
        if (am.indexReg.getNode() != null || am.isRIPRelative()) {
          am = backup;
          break;
        }
        int cost = 0;
        SDValue rhs = n.getOperand(1);
        int rhsOpc = rhs.getOpcode();
        if (!rhs.hasOneUse() ||
            rhsOpc == ISD.CopyFromReg ||
            rhsOpc == ISD.TRUNCATE ||
            rhsOpc == ISD.ANY_EXTEND ||
            (rhsOpc == ISD.ZERO_EXTEND &&
                rhs.getOperand(0).getValueType().equals(new EVT(MVT.i32))))
          ++cost;

        if ((am.baseType == RegBase && am.base.reg.getNode() != null &&
            !am.base.reg.getNode().hasOneUse()) ||
            am.baseType == FrameIndexBase) {
          --cost;
        }
        boolean b1 = (am.hasSymbolicDisplacement() && !backup.hasSymbolicDisplacement());
        boolean b2 = am.disp != 0 && backup.disp == 0;
        boolean b3 = am.segment.getNode() != null && backup.segment.getNode() == null;
        if ((b1 ? 1 : 0) + (b2 ? 1 : 0) + (b3 ? 1 : 0) >= 2) {
          --cost;
        }

        if (cost >= 0) {
          am = backup;
          break;
        }

        SDValue zero = curDAG.getConstant(0, n.getValueType(), false);
        SDValue neg = curDAG.getNode(ISD.SUB, n.getValueType(), zero, rhs);
        am.indexReg = neg;
        am.scale = 1;

        if (zero.getNode().getNodeID() == -1 ||
            zero.getNode().getNodeID() > n.getNode().getNodeID()) {
          curDAG.repositionNode(n.getNode(), zero.getNode());
          zero.getNode().setNodeID(n.getNode().getNodeID());
        }
        if (neg.getNode().getNodeID() == -1 ||
            neg.getNode().getNodeID() > n.getNode().getNodeID()) {
          curDAG.repositionNode(n.getNode(), neg.getNode());
          neg.getNode().setNodeID(n.getNode().getNodeID());
        }
        return false;
      }
      case ISD.ADD: {
        X86ISelAddressMode backup = am.clone();
        if (!matchAddressRecursively(n.getOperand(0), am, depth + 1) &&
            !matchAddressRecursively(n.getOperand(1), am, depth + 1)) {
          return false;
        }

        am = backup;

        if (!matchAddressRecursively(n.getOperand(1), am, depth + 1) &&
            !matchAddressRecursively(n.getOperand(0), am, depth + 1))
          return false;

        am = backup;

        if (am.baseType == RegBase &&
            am.base.reg.getNode() == null &&
            am.indexReg.getNode() == null) {
          am.base.reg = n.getOperand(0);
          am.indexReg = n.getOperand(1);
          am.scale = 1;
          return false;
        }
        break;
      }
      case ISD.OR: {
        if (n.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode csd = (ConstantSDNode) n.getOperand(1).getNode();
          X86ISelAddressMode backup = am.clone();
          long offset = csd.getSExtValue();
          if (!matchAddressRecursively(n.getOperand(0), am, depth + 1) &&
              am.gv == null &&
              (!is64Bit || X86.isOffsetSuitableForCodeModel(
                  am.disp + offset, m, am.hasSymbolicDisplacement())) &&
              curDAG.maskedValueIsZero(n.getOperand(0), csd.getAPIntValue())) {
            am.disp += offset;
            return false;
          }
          am = backup;
        }
        break;
      }
      case ISD.AND: {
        SDValue shift = n.getOperand(0);
        if (shift.getNumOperands() != 2) break;

        if (am.indexReg.getNode() != null || am.scale != 1) break;
        SDValue x = shift.getOperand(0);
        if (!(n.getOperand(1).getNode() instanceof ConstantSDNode) ||
            !(shift.getOperand(1).getNode() instanceof ConstantSDNode))
          break;

        ConstantSDNode c2 = (ConstantSDNode) n.getOperand(1).getNode();
        ConstantSDNode c1 = (ConstantSDNode) shift.getOperand(1).getNode();
        // Handle "(X >> (8-C1)) & C2" as "(X >> 8) & 0xff)" if safe. This
        // allows us to convert the shift and and into an h-register extract and
        // a scaled index.
        if (shift.getOpcode() == ISD.SRL && shift.hasOneUse()) {
          long scaleLog = 8 - c1.getZExtValue();
          if (scaleLog > 0 && scaleLog < 4 && c2.getZExtValue() ==
              (0xffL << scaleLog)) {
            SDValue eight = curDAG.getConstant(8, new EVT(MVT.i8), false);
            SDValue mask = curDAG.getConstant(0xff, n.getValueType(), false);
            SDValue srl = curDAG.getNode(ISD.SRL, n.getValueType(), x, eight);
            SDValue and = curDAG.getNode(ISD.AND, n.getValueType(), srl, mask);
            SDValue shlCount = curDAG.getConstant(scaleLog, new EVT(MVT.i8), false);
            SDValue shl = curDAG.getNode(ISD.SHL, n.getValueType(), and, shlCount);

            if (eight.getNode().getNodeID() == -1 ||
                eight.getNode().getNodeID() > x.getNode().getNodeID()) {
              curDAG.repositionNode(x.getNode(), eight.getNode());
              eight.getNode().setNodeID(x.getNode().getNodeID());
            }
            if (mask.getNode().getNodeID() == -1 ||
                mask.getNode().getNodeID() > x.getNode().getNodeID()) {
              curDAG.repositionNode(x.getNode(), mask.getNode());
              mask.getNode().setNodeID(x.getNode().getNodeID());
            }
            if (srl.getNode().getNodeID() == -1 ||
                srl.getNode().getNodeID() > shift.getNode().getNodeID()) {
              curDAG.repositionNode(shift.getNode(), srl.getNode());
              srl.getNode().setNodeID(shift.getNode().getNodeID());
            }
            if (and.getNode().getNodeID() == -1 ||
                and.getNode().getNodeID() > n.getNode().getNodeID()) {
              curDAG.repositionNode(n.getNode(), and.getNode());
              and.getNode().setNodeID(n.getNode().getNodeID());
            }
            if (shlCount.getNode().getNodeID() == -1 ||
                shlCount.getNode().getNodeID() > x.getNode().getNodeID()) {
              curDAG.repositionNode(x.getNode(), shlCount.getNode());
              shlCount.getNode().setNodeID(x.getNode().getNodeID());
            }
            if (shl.getNode().getNodeID() == -1 ||
                shl.getNode().getNodeID() > n.getNode().getNodeID()) {
              curDAG.repositionNode(n.getNode(), shl.getNode());
              shl.getNode().setNodeID(n.getNode().getNodeID());
            }
            curDAG.replaceAllUsesWith(n, shl, null);
            am.indexReg = and;
            am.scale = 1 << scaleLog;
            return false;
          }
        }

        if (shift.getOpcode() != ISD.SHL)
          break;
        if (!n.hasOneUse() || !shift.hasOneUse())
          break;
        long shiftCst = c1.getZExtValue();
        if (shiftCst != 1 && shiftCst != 2 && shiftCst != 3)
          break;

        SDValue newAndMask = curDAG.getNode(ISD.SRL, n.getValueType(),
            new SDValue(c2, 0), new SDValue(c1, 0));
        SDValue newAnd = curDAG.getNode(ISD.AND, n.getValueType(), x,
            newAndMask);
        SDValue newShift = curDAG.getNode(ISD.SHL, n.getValueType(),
            newAnd, new SDValue(c1, 0));

        if (c1.getNodeID() > x.getNode().getNodeID()) {
          curDAG.repositionNode(x.getNode(), c1);
          c1.setNodeID(x.getNode().getNodeID());
        }
        if (newAndMask.getNode().getNodeID() == -1 ||
            newAndMask.getNode().getNodeID() > x.getNode().getNodeID()) {
          curDAG.repositionNode(x.getNode(), newAndMask.getNode());
          newAndMask.getNode().setNodeID(x.getNode().getNodeID());
        }
        if (newAnd.getNode().getNodeID() == -1 ||
            newAnd.getNode().getNodeID() > shift.getNode().getNodeID()) {
          curDAG.repositionNode(shift.getNode(), newAnd.getNode());
          newAnd.getNode().setNodeID(shift.getNode().getNodeID());
        }
        if (newShift.getNode().getNodeID() == -1 ||
            newShift.getNode().getNodeID() > n.getNode().getNodeID()) {
          curDAG.repositionNode(n.getNode(), newShift.getNode());
          newShift.getNode().setNodeID(n.getNode().getNodeID());
        }
        curDAG.replaceAllUsesWith(n, newShift, null);
        am.scale = 1 << shiftCst;
        am.indexReg = newAnd;
        return false;
      }
    }
    return matchAddressBase(n, am);
  }

  protected boolean matchAddressBase(SDValue n, X86ISelAddressMode am) {
    if (am.baseType != X86ISelAddressMode.BaseType.RegBase ||
        am.base.reg.getNode() != null) {
      if (am.indexReg.getNode() == null) {
        am.indexReg = n;
        am.scale = 1;
        return false;
      }

      return true;
    }

    am.baseType = X86ISelAddressMode.BaseType.RegBase;
    am.base.reg = n;
    return false;
  }

  /**
   * comp indicates the base, scale, index, disp and segment for X86 Address mode.
   *
   * @param op
   * @param n
   * @param comp
   * @return
   */
  protected boolean selectAddr(SDValue op, SDValue n, SDValue[] comp) {
    X86ISelAddressMode am = new X86ISelAddressMode();
    boolean done = false;
    if (!n.hasOneUse()) {
      int opc = n.getOpcode();
      if (opc != ISD.Constant && opc != ISD.FrameIndex &&
          opc != X86ISD.Wrapper && opc != X86ISD.WrapperRIP) {
        for (SDUse u : n.getNode().getUseList()) {
          if (u.getNode().getOpcode() == ISD.CopyToReg) {
            matchAddressBase(n, am);
            done = true;
            break;
          }
        }
      }
    }

    if (!done && matchAddress(n, am))
      return false;

    EVT vt = n.getValueType();
    if (am.baseType == X86ISelAddressMode.BaseType.RegBase) {
      if (am.base.reg.getNode() == null)
        am.base.reg = curDAG.getRegister(0, vt);
    }
    if (am.indexReg.getNode() == null)
      am.indexReg = curDAG.getRegister(0, vt);
    getAddressOperands(am, comp);
    return true;
  }

  protected boolean selectLEAAddr(SDValue op, SDValue val, SDValue[] comp) {
    Util.assertion(comp.length == 4);
    X86ISelAddressMode am = new X86ISelAddressMode();
    SDValue copy = am.segment;
    SDValue t = curDAG.getRegister(0, new EVT(MVT.i32));
    am.segment = t;
    if (matchAddress(val, am))
      return false;
    Util.assertion(t.equals(am.segment));
    am.segment = copy;

    EVT vt = val.getValueType();
    int complexity = 0;
    if (am.baseType == X86ISelAddressMode.BaseType.RegBase) {
      if (am.base.reg.getNode() != null)
        complexity = 1;
      else
        am.base.reg = curDAG.getRegister(0, vt);
    } else if (am.baseType == FrameIndexBase)
      complexity = 4;

    if (am.indexReg.getNode() != null)
      ++complexity;
    else
      am.indexReg = curDAG.getRegister(0, vt);

    if (am.scale > 1)
      ++complexity;
    if (am.hasSymbolicDisplacement()) {
      if (subtarget.is64Bit())
        complexity = 4;
      else
        complexity += 2;
    }

    if (am.disp != 0 && (am.base.reg.getNode() != null || am.indexReg.getNode() != null))
      ++complexity;

    if (complexity <= 2)
      return false;

    SDValue[] temp = new SDValue[5];
    temp[4] = new SDValue();    // segment.
    getAddressOperands(am, temp);
    System.arraycopy(comp, 0, temp, 0, 4);
    return true;
  }

  protected boolean selectTLSADDRAddr(SDValue op, SDValue val, SDValue[] comp) {
    Util.assertion(comp.length == 4);
    Util.assertion(val.getOpcode() == ISD.TargetGlobalTLSAddress);
    GlobalAddressSDNode ga = (GlobalAddressSDNode) val.getNode();
    X86ISelAddressMode am = new X86ISelAddressMode();
    am.gv = ga.getGlobalValue();
    am.disp += ga.getOffset();
    am.base.reg = curDAG.getRegister(0, val.getValueType());
    am.symbolFlags = ga.getTargetFlags();
    if (val.getValueType().getSimpleVT().simpleVT == MVT.i32) {
      am.scale = 1;
      am.indexReg = curDAG.getRegister(X86GenRegisterNames.EBX, new EVT(MVT.i32));
    } else {
      am.indexReg = curDAG.getRegister(0, new EVT(MVT.i64));
    }
    SDValue[] temp = new SDValue[5];
    System.arraycopy(comp, 0, temp, 0, 4);
    temp[4] = new SDValue();    // segment.
    getAddressOperands(am, temp);
    return true;
  }

  protected boolean selectScalarSSELoad(SDValue op, SDValue pred, SDValue node, SDValue[] comp) {
    // comp[0]  -- base
    // comp[1]  -- scale
    // comp[2]  -- index
    // comp[3]  -- disp
    // comp[4]  -- segment
    // comp[5]  -- inChain
    // comp[6]  -- outChain

    Util.assertion(comp.length == 7);
    if (node.getOpcode() == ISD.SCALAR_TO_VECTOR) {
      comp[5] = node.getOperand(0).getValue(1);
      if (comp[5].getNode().isNONExtLoad() &&
          comp[5].getValue(0).hasOneUse() &&
          node.hasOneUse() &&
          isLegalAndProfitableToFold(node.getNode(), pred.getNode(),
              op.getNode())) {
        LoadSDNode ld = (LoadSDNode) comp[5].getNode();
        if (!selectAddr(op, ld.getBasePtr(), comp))
          return false;
        comp[6] = ld.getChain();
        return true;
      }
    }

    if (node.getOpcode() == X86ISD.VZEXT_MOVL && node.getNode().hasOneUse() &&
        node.getOperand(0).getOpcode() == ISD.SCALAR_TO_VECTOR &&
        node.getOperand(0).getNode().hasOneUse() &&
        node.getOperand(0).getOperand(0).getNode().isNONExtLoad() &&
        node.getOperand(0).getOperand(0).hasOneUse()) {
      LoadSDNode ld = (LoadSDNode) node.getOperand(0).getOperand(0).getNode();
      if (!selectAddr(op, ld.getBasePtr(), comp))
        return false;
      comp[6] = ld.getChain();
      comp[5] = new SDValue(ld, 1);
      return true;
    }
    return false;
  }

  protected boolean tryFoldLoad(SDValue pred, SDValue node, SDValue[] comp) {
    Util.assertion(comp.length == 5);
    if (node.getNode().isNONExtLoad() &&
        node.getNode().hasOneUse() &&
        isLegalAndProfitableToFold(node.getNode(), pred.getNode(), pred.getNode()))
      return selectAddr(pred, node.getOperand(1), comp);

    return false;
  }

  protected void preprocessForRMW() {
    OutRef<SDValue> x = new OutRef<>();
    for (SDNode node : curDAG.allNodes) {
      if (node.getOpcode() == X86ISD.CALL) {
        /// Also try moving call address load from outside callseq_start to just
        /// before the call to allow it to be folded.
        ///
        ///     [Load chain]
        ///         ^
        ///         |
        ///       [Load]
        ///       ^    ^
        ///       |    |
        ///      /      \--
        ///     /          |
        ///[CALLSEQ_START] |
        ///     ^          |
        ///     |          |
        /// [LOAD/C2Reg]   |
        ///     |          |
        ///      \        /
        ///       \      /
        ///       [CALL]
        SDValue chain = node.getOperand(0);
        SDValue load = node.getOperand(1);
        x.set(chain);
        boolean res = !isCalleeLoad(load, x);
        chain = x.get();
        if (res)
          continue;
        moveBelowCallSeqStart(curDAG, load, new SDValue(node, 0), chain);
        continue;
      }

      if (!node.isNONTRUNCStore())
        continue;
      SDValue chain = node.getOperand(0);

      if (chain.getNode().getOpcode() != ISD.TokenFactor)
        continue;

      SDValue n1 = node.getOperand(1);
      SDValue n2 = node.getOperand(2);
      if ((n1.getValueType().isFloatingPoint() &&
          !n1.getValueType().isVector()) ||
          !n1.hasOneUse())
        continue;

      boolean rmodW = false;
      SDValue load = new SDValue();
      int opc = n1.getNode().getOpcode();
      switch (opc) {
        case ISD.ADD:
        case ISD.MUL:
        case ISD.AND:
        case ISD.OR:
        case ISD.XOR:
        case ISD.ADDC:
        case ISD.ADDE:
        case ISD.VECTOR_SHUFFLE: {
          SDValue n10 = n1.getOperand(0);
          SDValue n11 = n1.getOperand(1);
          x.set(load);
          rmodW = isRMWLoad(n10, chain, n2, x);
          load = x.get();
          if (!rmodW) {
            x.set(load);
            rmodW = isRMWLoad(n11, chain, n2, x);
            load = x.get();
          }
          break;
        }
        case ISD.SUB:
        case ISD.SHL:
        case ISD.SRA:
        case ISD.SRL:
        case ISD.ROTL:
        case ISD.ROTR:
        case ISD.SUBC:
        case ISD.SUBE:
        case X86ISD.SHLD:
        case X86ISD.SHRD: {
          SDValue n10 = n1.getOperand(0);
          x.set(load);
          rmodW = isRMWLoad(n10, chain, n2, x);
          load = x.get();
          break;
        }
      }
      if (rmodW) {
        moveBelowTokenFactor(curDAG, load, new SDValue(node, 0), chain);
      }
    }
  }

  static boolean isRMWLoad(SDValue n, SDValue chain, SDValue address,
                           OutRef<SDValue> load) {
    if (n.getOpcode() == ISD.BIT_CONVERT)
      n = n.getOperand(0);

    LoadSDNode ld = n.getNode() instanceof LoadSDNode ? (LoadSDNode) n.getNode() : null;
    if (ld == null || ld.isVolatile())
      return false;
    if (ld.getAddressingMode() != UNINDEXED)
      return false;

    LoadExtType extType = ld.getExtensionType();
    if (extType != NON_EXTLOAD && extType != EXTLOAD)
      return false;

    if (n.hasOneUse() &&
        n.getOperand(1).equals(address) &&
        n.getNode().isOperandOf(chain.getNode())) {
      load.set(n);
      return true;
    }
    return false;
  }

  static void moveBelowTokenFactor(SelectionDAG curDAG, SDValue load,
                                   SDValue store, SDValue tf) {
    ArrayList<SDValue> ops = new ArrayList<>();
    for (int i = 0, e = tf.getNode().getNumOperands(); i < e; i++) {
      if (load.getNode().equals(tf.getOperand(i).getNode()))
        ops.add(load.getOperand(0));
      else
        ops.add(tf.getOperand(i));
    }
    SDValue newTF = curDAG.updateNodeOperands(tf, ops);
    SDValue newLoad = curDAG.updateNodeOperands(load, newTF, load.getOperand(1),
        load.getOperand(2));
    curDAG.updateNodeOperands(store, newLoad.getValue(1), store.getOperand(1),
        store.getOperand(2), store.getOperand(3));
  }

  static boolean isCalleeLoad(SDValue callee, OutRef<SDValue> chain) {
    if (callee.getNode().equals(chain.get().getNode()) || !callee.hasOneUse())
      return false;
    LoadSDNode ld = callee.getNode() instanceof LoadSDNode ? (LoadSDNode) callee.getNode() : null;
    if (ld == null || ld.isVolatile() || ld.getAddressingMode() != UNINDEXED
        || ld.getExtensionType() != NON_EXTLOAD)
      return false;

    while (chain.get().getOpcode() != ISD.CALLSEQ_START) {
      if (chain.get().hasOneUse())
        return false;

      chain.set(chain.get().getOperand(0));
    }
    if (chain.get().getOperand(0).getNode().equals(callee.getNode()))
      return true;
    if (chain.get().getOperand(0).getOpcode() == ISD.TokenFactor &&
        callee.getValue(1).isOperandOf(chain.get().getOperand(0).getNode()) &&
        callee.getValue(1).hasOneUse())
      return true;

    return false;
  }

  static void moveBelowCallSeqStart(SelectionDAG curDAG, SDValue load,
                                    SDValue call, SDValue callSeqStart) {
    ArrayList<SDValue> ops = new ArrayList<>();
    SDValue chain = callSeqStart.getOperand(0);
    if (chain.getNode().equals(load.getNode()))
      ops.add(load.getOperand(0));
    else {
      Util.assertion(chain.getOpcode() == ISD.TokenFactor);
      for (int i = 0, e = chain.getNumOperands(); i < e; i++) {
        if (chain.getOperand(i).getNode().equals(load.getNode()))
          ops.add(load.getOperand(0));
        else
          ops.add(chain.getOperand(i));
      }

      SDValue newChain = curDAG.getNode(ISD.TokenFactor, new EVT(MVT.Other),
          ops);
      ops.clear();
      ops.add(newChain);
    }

    for (int i = 1, e = callSeqStart.getNumOperands(); i < e; i++)
      ops.add(callSeqStart.getOperand(i));
    curDAG.updateNodeOperands(callSeqStart, ops);
    curDAG.updateNodeOperands(load, call.getOperand(0),
        load.getOperand(1), load.getOperand(2));
    ops.clear();
    ops.add(new SDValue(load.getNode(), 1));
    for (int i = 1, e = call.getNode().getNumOperands(); i < e; i++)
      ops.add(call.getOperand(i));
    curDAG.updateNodeOperands(call, ops);
  }

  protected void preprocessForFPConvert() {
    for (SDNode node : curDAG.allNodes) {
      if (node.getOpcode() != ISD.FP_ROUND && node.getOpcode() != ISD.FP_EXTEND)
        continue;

      EVT srcVT = node.getOperand(0).getValueType();
      EVT dstVT = node.getValueType(0);
      boolean srcIsSSE = tli.isScalarFPTypeInSSEReg(srcVT);
      boolean dstIsSSE = tli.isScalarFPTypeInSSEReg(dstVT);
      if (srcIsSSE && dstIsSSE)
        continue;

      if (!srcIsSSE && !dstIsSSE) {
        if (node.getOpcode() == ISD.FP_EXTEND)
          continue;
        if (node.getConstantOperandVal(1) != 0)
          continue;
      }

      EVT memVT;
      if (node.getOpcode() == ISD.FP_ROUND)
        memVT = dstVT;
      else
        memVT = srcIsSSE ? srcVT : dstVT;

      SDValue memTmp = curDAG.createStackTemporary(memVT);
      SDValue store = curDAG.getTruncStore(curDAG.getEntryNode(), node.getOperand(0),
          memTmp, null, 0, memVT);
      SDValue result = curDAG.getExtLoad(EXTLOAD, dstVT, store, memTmp,
          null, 0, memVT);
    }
  }

  protected void emitSpecialCodeForMain(MachineBasicBlock mbb, MachineFrameInfo mfi) {
    TargetInstrInfo tii = tm.getInstrInfo();
    if (subtarget.isTargetCygMing()) {
      buildMI(mbb, tii.get(X86GenInstrNames.CALLpcrel32)).addExternalSymbol("__main");
    }
  }

  protected void getAddressOperands(X86ISelAddressMode am, SDValue[] comp) {
    // comp[0] -- base
    // comp[1] -- scale
    // comp[2] -- index
    // comp[3] -- disp
    // comp[4] -- segment
    Util.assertion(comp.length == 5);
    comp[0] = am.baseType == FrameIndexBase ?
        curDAG.getTargetFrameIndex(am.base.frameIndex, new EVT(tli.getPointerTy())) :
        am.base.reg;
    comp[1] = getI8Imm(am.scale);
    comp[2] = am.indexReg;

    if (am.gv != null)
      comp[3] = curDAG.getTargetGlobalAddress(am.gv, new EVT(MVT.i32), am.disp,
          am.symbolFlags);
    else if (am.cp != null)
      comp[3] = curDAG.getTargetConstantPool(am.cp, new EVT(MVT.i32),
          am.align, am.disp, am.symbolFlags);
    else if (am.externalSym != null)
      comp[3] = curDAG.getTargetExternalSymbol(am.externalSym, new EVT(MVT.i32), am.symbolFlags);
    else if (am.jti != -1)
      comp[3] = curDAG.getTargetJumpTable(am.jti, new EVT(MVT.i32), am.symbolFlags);
    else
      comp[3] = curDAG.getTargetConstant(am.disp, new EVT(MVT.i32));

    if (am.segment.getNode() != null)
      comp[4] = am.segment;
    else
      comp[4] = curDAG.getRegister(0, new EVT(MVT.i32));
  }

  protected SDValue getI8Imm(int imm) {
    return curDAG.getTargetConstant(imm, new EVT(MVT.i8));
  }

  protected SDValue getI16Imm(int imm) {
    return curDAG.getTargetConstant(imm, new EVT(MVT.i16));
  }

  protected SDValue getI32Imm(int imm) {
    return curDAG.getTargetConstant(imm, new EVT(MVT.i32));
  }

  protected SDNode getGlobalBaseReg() {
    int baseReg = getTargetMachine().getInstrInfo().getGlobalBaseReg(mf);
    return curDAG.getRegister(baseReg, new EVT(tli.getPointerTy())).getNode();
  }

  public X86TargetMachine getTargetMachine() {
    return (X86TargetMachine) super.getTargetMachine();
  }

  public SDNode select_DECLARE(SDValue n) {
    SDValue chain = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    SDValue n2 = n.getOperand(2);
    if (!(n1.getNode() instanceof FrameIndexSDNode)
        || !(n2.getNode() instanceof GlobalAddressSDNode)) {
      cannotYetSelect(n.getNode());
    }
    EVT pty = new EVT(tli.getPointerTy());
    int fi = ((FrameIndexSDNode) n1.getNode()).getFrameIndex();
    GlobalValue gv = ((GlobalAddressSDNode) n2.getNode()).getGlobalValue();
    SDValue tmp1 = curDAG.getTargetFrameIndex(fi, pty);
    SDValue tmp2 = curDAG.getTargetGlobalAddress(gv, pty, 0, 0);
    return curDAG.selectNodeTo(n.getNode(),
        X86GenInstrNames.DECLARE,
        new EVT(MVT.Other), tmp1, tmp2, chain);
  }
}
