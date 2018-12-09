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

package backend.codegen.dagisel;

import backend.codegen.*;
import backend.codegen.dagisel.SDNode.*;
import backend.support.CallingConv;
import backend.support.LLVMContext;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.type.Type;
import backend.value.*;
import gnu.trove.list.array.TIntArrayList;
import tools.APFloat;
import tools.APInt;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.codegen.dagisel.LoadExtType.*;
import static tools.Util.bitsToDouble;

/**
 * This takes an arbitrary {@linkplain SelectionDAG} as input and transform it
 * until all operations and types are supported directly by target machine.
 * This involves eliminating value sizes the machine can't handle as well as
 * eliminating operations the machine can't cope with.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class SelectionDAGLegalizer {
  private TargetLowering tli;
  private SelectionDAG dag;
  private TargetMachine.CodeGenOpt optLevel;

  private SDValue lastCALLSEQ_END;

  private boolean isLegalizingCall;

  private enum LegalizeAction {
    Legal,
    Promote,
    Expand
  }

  private ValueTypeAction valueTypeAction;
  private HashMap<SDValue, SDValue> legalizeNodes;

  public SelectionDAGLegalizer(SelectionDAG dag,
                               TargetMachine.CodeGenOpt optLevel) {
    this.dag = dag;
    this.tli = dag.getTargetLoweringInfo();
    this.optLevel = optLevel;
    valueTypeAction = tli.getValueTypeActions();
    legalizeNodes = new HashMap<>();
  }

  private void addLegalizedOperand(SDValue from, SDValue to) {
    legalizeNodes.put(from, to);
    if (from.equals(to))
      legalizeNodes.put(to, to);
  }

  public void legalizeDAG() {
    lastCALLSEQ_END = dag.getEntryNode();
    isLegalizingCall = false;

    dag.assignTopologicalOrder();
    for (int i = 0, e = dag.allNodes.size(); i < e; i++) {
      SDNode node = dag.allNodes.get(i);
      legalizeOp(new SDValue(node, 0));
    }

    SDValue oldRoot = dag.getRoot();
    Util.assertion(legalizeNodes.containsKey(oldRoot), "Root didn't get legalized!");
    dag.setRoot(legalizeNodes.get(oldRoot));

    legalizeNodes.clear();
    dag.removeDeadNodes();
  }

  public TargetLowering.LegalizeAction getTypeAction(EVT vt) {
    return valueTypeAction.getTypeAction(vt);
  }

  public boolean isTypeLegal(EVT vt) {
    return getTypeAction(vt) == TargetLowering.LegalizeAction.Legal;
  }

  private SDValue legalizeOp(SDValue val) {
    if (val.getOpcode() == ISD.TargetConstant)
      return val;

    SDNode node = val.getNode();
    for (int i = 0, e = node.getNumValues(); i < e; i++)
      Util.assertion(getTypeAction(node.getValueType(i)) == TargetLowering.LegalizeAction.Legal, "Unexpected illegal type!");


    for (int i = 0, e = node.getNumOperands(); i < e; i++)
      Util.assertion(isTypeLegal(node.getOperand(i).getValueType()) || node.getOperand(i).getOpcode() == ISD.TargetConstant,
          "Unexpected illegal type!");


    if (legalizeNodes.containsKey(val))
      return legalizeNodes.get(val);

    SDValue temp1, temp2, temp3, temp4;
    SDValue result = val;
    boolean isCustom = false;

    TargetLowering.LegalizeAction action = null;
    boolean simpleFinishLegalizing = true;
    switch (node.getOpcode()) {
      case ISD.INTRINSIC_W_CHAIN:
      case ISD.INTRINSIC_WO_CHAIN:
      case ISD.INTRINSIC_VOID:
      case ISD.VAARG:
      case ISD.STACKSAVE:
        action = tli.getOperationAction(node.getOpcode(),
            new EVT(MVT.Other));
        break;
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
      case ISD.EXTRACT_VECTOR_ELT:
        action = tli.getOperationAction(node.getOpcode(),
            node.getOperand(0).getValueType());
        break;
      case ISD.FP_ROUND_INREG: {
        EVT innerType = ((VTSDNode) node.getOperand(1).getNode()).getVT();
        action = tli.getOperationAction(node.getOpcode(), innerType);
        break;
      }
      case ISD.SELECT_CC:
      case ISD.SETCC:
      case ISD.BR_CC: {
        int ccOp = node.getOpcode() == ISD.SELECT_CC ? 4 :
            node.getOpcode() == ISD.SETCC ? 2 : 1;
        int cmpOp = node.getOpcode() == ISD.BR_CC ? 2 : 0;
        EVT opVT = node.getOperand(cmpOp).getValueType();
        CondCode cc = ((CondCodeSDNode) node.getOperand(ccOp).getNode()).getCondition();
        action = tli.getCondCodeAction(cc, opVT);
        if (action == TargetLowering.LegalizeAction.Legal) {
          if (node.getOpcode() == ISD.SELECT_CC)
            action = tli.getOperationAction(node.getOpcode(), node.getValueType(0));
          else
            action = tli.getOperationAction(node.getOpcode(), opVT);
        }
        break;
      }
      case ISD.LOAD:
      case ISD.STORE:
        simpleFinishLegalizing = false;
        break;
      case ISD.CALLSEQ_END:
      case ISD.CALLSEQ_START:
        simpleFinishLegalizing = false;
        break;
      case ISD.EXTRACT_ELEMENT:
      case ISD.FLT_ROUNDS_:
      case ISD.SADDO:
      case ISD.SSUBO:
      case ISD.UADDO:
      case ISD.USUBO:
      case ISD.SMULO:
      case ISD.UMULO:
      case ISD.FPOWI:
      case ISD.MERGE_VALUES:
      case ISD.EH_RETURN:
      case ISD.FRAME_TO_ARGS_OFFSET:
        action = tli.getOperationAction(node.getOpcode(), node.getValueType(0));
        if (action == TargetLowering.LegalizeAction.Legal)
          action = TargetLowering.LegalizeAction.Expand;
        break;
      case ISD.TRAMPOLINE:
      case ISD.FRAMEADDR:
      case ISD.RETURNADDR:
        action = tli.getOperationAction(node.getOpcode(), node.getValueType(0));
        if (action == TargetLowering.LegalizeAction.Legal)
          action = TargetLowering.LegalizeAction.Custom;
        break;
      case ISD.BUILD_VECTOR:
        simpleFinishLegalizing = false;
        break;
      default:
        if (node.getOpcode() >= ISD.BUILTIN_OP_END)
          action = TargetLowering.LegalizeAction.Legal;
        else
          action = tli.getOperationAction(node.getOpcode(), node.getValueType(0));
        break;
    }
    if (simpleFinishLegalizing) {
      ArrayList<SDValue> ops = new ArrayList<>();
      ArrayList<SDValue> resultVals = new ArrayList<>();
      for (int i = 0, e = node.getNumOperands(); i < e; i++)
        ops.add(legalizeOp(node.getOperand(i)));
      switch (node.getOpcode()) {
        default:
          break;
        case ISD.BR:
        case ISD.BRIND:
        case ISD.BR_JT:
        case ISD.BR_CC:
        case ISD.BRCOND:
          ops.set(0, dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
              ops.get(0), lastCALLSEQ_END));
          ops.set(0, legalizeOp(ops.get(0)));
          lastCALLSEQ_END = dag.getEntryNode();
          break;
        case ISD.SHL:
        case ISD.SRL:
        case ISD.SRA:
        case ISD.ROTL:
        case ISD.ROTR:
          if (!ops.get(1).getValueType().isVector())
            ops.set(1, legalizeOp(dag.getShiftAmountOperand(ops.get(1))));
          break;
      }

      result = dag.updateNodeOperands(result.getValue(0), ops);
      switch (action) {
        case Legal:
          for (int i = 0, e = node.getNumValues(); i < e; i++)
            resultVals.add(result.getValue(i));
          break;
        case Custom:
          temp1 = tli.lowerOperation(result, dag);
          if (temp1.getNode() != null) {
            for (int i = 0, e = node.getNumValues(); i < e; i++) {
              if (e == 1)
                resultVals.add(temp1);
              else
                resultVals.add(temp1.getValue(i));
            }
            break;
          }
          // fall through.
        case Expand:
          expandNode(result.getNode(), resultVals);
          break;
        case Promote:
          promoteNode(result.getNode(), resultVals);
          break;
      }
      if (!resultVals.isEmpty()) {
        for (int i = 0, e = resultVals.size(); i < e; i++) {
          if (!resultVals.get(i).equals(new SDValue(node, i)))
            resultVals.set(i, legalizeOp(resultVals.get(i)));
          addLegalizedOperand(new SDValue(node, i), resultVals.get(i));
        }
        return resultVals.get(val.getResNo());
      }
    }

    switch (node.getOpcode()) {
      case ISD.BUILD_VECTOR:
        switch (tli.getOperationAction(ISD.BUILD_VECTOR, node.getValueType(0))) {
          case Custom:
            temp3 = tli.lowerOperation(result, dag);
            if (temp3.getNode() != null) {
              result = temp3;
              break;
            }
            // fall through.
          case Expand:
            result = expandBuildVector(result.getNode());
            break;
        }
        break;
      case ISD.CALLSEQ_START: {
        SDNode callEnd = findCallEndFromCallStart(node);
        HashSet<SDNode> nodesLeadingTo = new HashSet<>();
        for (int i = 0, e = callEnd.getNumOperands(); i < e; i++)
          legalizeAllNodesNotLeadingTo(callEnd.getOperand(i).getNode(),
              node, nodesLeadingTo);

        temp1 = legalizeOp(node.getOperand(0));
        if (lastCALLSEQ_END.getOpcode() != ISD.TokenFactor) {
          temp1 = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
              temp1, lastCALLSEQ_END);
          temp1 = legalizeOp(temp1);
        }

        if (!temp1.equals(node.getOperand(0))) {
          SDValue[] ops = new SDValue[node.getNumOperands()];
          ops[0] = temp1;
          for (int i = 1; i < ops.length; i++)
            ops[i] = node.getOperand(i);
          result = dag.updateNodeOperands(result, ops);
        }

        addLegalizedOperand(val.getValue(0), result);
        if (node.getNumValues() == 2)
          addLegalizedOperand(val.getValue(1), result.getValue(1));

        Util.assertion(!isLegalizingCall, "Inconsistent sequentialization of calls!");
        lastCALLSEQ_END = new SDValue(callEnd, 0);
        isLegalizingCall = true;
        legalizeOp(lastCALLSEQ_END);
        Util.assertion(!isLegalizingCall);
        return result;
      }
      case ISD.CALLSEQ_END: {
        if (!lastCALLSEQ_END.getNode().equals(node)) {
          legalizeOp(new SDValue(findCallStartFromCallEnd(node), 0));
          Util.assertion(legalizeNodes.containsKey(val), "Legalizing the call start should have legalized this node!");

          return legalizeNodes.get(val);
        }

        temp1 = legalizeOp(node.getOperand(0));
        if (node.getOperand(node.getNumOperands() - 1).getValueType()
            .getSimpleVT().simpleVT != MVT.Flag) {
          if (!temp1.equals(node.getOperand(0))) {
            SDValue[] ops = new SDValue[node.getNumOperands()];
            ops[0] = temp1;
            for (int i = 1; i < ops.length; i++)
              ops[i] = node.getOperand(i);
            result = dag.updateNodeOperands(result, ops);
          }
        } else {
          temp2 = legalizeOp(node.getOperand(node.getNumOperands() - 1));
          if (!temp1.equals(node.getOperand(0)) ||
              !temp2.equals(node.getOperand(node.getNumOperands() - 1))) {
            SDValue[] ops = new SDValue[node.getNumOperands()];
            ops[0] = temp1;
            for (int i = 1; i < ops.length - 1; i++)
              ops[i] = node.getOperand(i);
            ops[ops.length - 1] = temp2;
            result = dag.updateNodeOperands(result, ops);
          }
        }
        Util.assertion(isLegalizingCall, "Call sequence imbalance between start/end");
        isLegalizingCall = false;

        addLegalizedOperand(new SDValue(node, 0), result.getValue(0));
        if (node.getNumValues() == 2)
          addLegalizedOperand(new SDValue(node, 1), result.getValue(1));
        return result.getValue(val.getResNo());
      }
      case ISD.LOAD: {
        LoadSDNode ld = (LoadSDNode) node;
        temp1 = legalizeOp(ld.getChain());
        temp2 = legalizeOp(ld.getBasePtr());

        LoadExtType extType = ld.getExtensionType();
        if (extType == LoadExtType.NON_EXTLOAD) {
          EVT vt = node.getValueType(0);
          result = dag.updateNodeOperands(result, temp1, temp2, ld.getOffset());
          temp3 = result.getValue(0);
          temp4 = result.getValue(1);

          switch (tli.getOperationAction(node.getOpcode(), vt)) {
            default:
              Util.shouldNotReachHere("This action is unsupported yet!");
            case Legal:
              if (!tli.allowsUnalignedMemoryAccesses(ld.getMemoryVT())) {
                Type ty = ld.getMemoryVT().getTypeForEVT();
                int abiAlign = tli.getTargetData().getABITypeAlignment(ty);
                if (ld.getAlignment() < abiAlign) {
                  result = expandUnalignedLoad((LoadSDNode) result.getNode(),
                      dag, tli);
                  temp3 = result.getOperand(0);
                  temp4 = result.getOperand(1);
                  temp3 = legalizeOp(temp3);
                  temp4 = legalizeOp(temp4);
                }
              }
              break;
            case Custom:
              temp1 = tli.lowerOperation(temp3, dag);
              if (temp1.getNode() != null) {
                temp3 = legalizeOp(temp1);
                temp4 = legalizeOp(temp1.getValue(1));
              }
              break;
            case Promote: {
              Util.assertion(vt.isVector(), "Can't promote this load!");
              EVT nvt = tli.getTypeToPromoteType(node.getOpcode(), vt);
              temp1 = dag.getLoad(nvt, temp1, temp2, ld.getSrcValue(),
                  ld.getSrcValueOffset(), ld.isVolatile(),
                  ld.getAlignment());
              temp3 = legalizeOp(dag.getNode(ISD.BIT_CONVERT, vt, temp1));
              temp4 = legalizeOp(temp1.getValue(1));
              break;
            }
          }
          addLegalizedOperand(new SDValue(node, 0), temp3);
          addLegalizedOperand(new SDValue(node, 1), temp4);
          return val.getResNo() != 0 ? temp4 : temp3;
        } else {
          EVT srcVT = ld.getMemoryVT();
          int srcWidth = srcVT.getSizeInBits();
          int svOffset = ld.getSrcValueOffset();
          int alignment = ld.getAlignment();
          boolean isVolatile = ld.isVolatile();

          if (srcWidth != srcVT.getStoreSizeInBits() &&
              (srcVT.getSimpleVT().simpleVT != MVT.i1 ||
                  tli.getLoadExtAction(extType, new EVT(MVT.i1)) == TargetLowering.LegalizeAction.Promote)) {
            int newWidth = srcVT.getStoreSizeInBits();
            EVT nvt = EVT.getIntegerVT(newWidth);
            SDValue ch = new SDValue();
            LoadExtType newExtType = extType == LoadExtType.ZEXTLOAD ?
                LoadExtType.ZEXTLOAD : EXTLOAD;

            result = dag.getExtLoad(newExtType, node.getValueType(0),
                temp1, temp2, ld.getSrcValue(), svOffset,
                nvt, isVolatile, alignment);
            ch = result.getValue(1);

            if (extType == LoadExtType.SEXTLOAD) {
              result = dag.getNode(ISD.SIGN_EXTEND_INREG,
                  result.getValueType(), result, dag.getValueType(srcVT));
            } else if (extType == LoadExtType.ZEXTLOAD || nvt.equals(result.getValueType())) {
              result = dag.getNode(ISD.AssertSext, result.getValueType(), result,
                  dag.getValueType(srcVT));
            }
            temp1 = legalizeOp(result);
            temp2 = legalizeOp(ch);
          } else if ((srcWidth & (srcWidth - 1)) != 0) {
            // If not loading a power-of-2 number of bits, expand as two loads.
            Util.assertion(srcVT.isExtended() && !srcVT.isVector(), "Unsupported extload!");
            int roundWidth = 1 << Util.log2(srcWidth);
            Util.assertion(roundWidth < srcWidth);
            int extraWidth = srcWidth - roundWidth;
            Util.assertion(extraWidth < roundWidth);
            Util.assertion((roundWidth & 7) == 0 && (extraWidth & 7) == 0, "Load size not an integeral number of bytes!");

            EVT roundVT = EVT.getIntegerVT(roundWidth);
            EVT extraVT = EVT.getIntegerVT(extraWidth);
            SDValue lo, hi, ch;
            int incrementSize = 0;
            if (tli.isLittleEndian()) {
              // EXTLOAD:i24 -> ZEXTLOAD:i16 | (shl EXTLOAD@+2:i8, 16)
              // Load the bottom RoundWidth bits.
              lo = dag.getExtLoad(LoadExtType.ZEXTLOAD, node.getValueType(0),
                  temp1, temp2, ld.getSrcValue(), svOffset, roundVT,
                  isVolatile, alignment);
              incrementSize = roundWidth / 8;
              temp2 = dag.getNode(ISD.ADD, temp2.getValueType(), temp2,
                  dag.getIntPtrConstant(incrementSize));
              hi = dag.getExtLoad(extType, node.getValueType(0),
                  temp1, temp2, ld.getSrcValue(), svOffset + incrementSize,
                  extraVT, isVolatile, Util.minAlign(alignment, incrementSize));

              ch = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo.getValue(1),
                  hi.getValue(1));
              hi = dag.getNode(ISD.SHL, hi.getValueType(), hi,
                  dag.getConstant(roundWidth, new EVT(tli.getShiftAmountTy()), false));
              result = dag.getNode(ISD.OR, node.getValueType(0), lo, hi);
            } else {
              // Big endian - avoid unaligned loads.
              // EXTLOAD:i24 -> (shl EXTLOAD:i16, 8) | ZEXTLOAD@+2:i8
              // Load the top RoundWidth bits.
              hi = dag.getExtLoad(extType, node.getValueType(0),
                  temp1, temp2, ld.getSrcValue(), svOffset, roundVT,
                  isVolatile, alignment);
              incrementSize = roundWidth / 8;
              temp2 = dag.getNode(ISD.ADD, temp2.getValueType(), temp2,
                  dag.getIntPtrConstant(incrementSize));
              lo = dag.getExtLoad(LoadExtType.ZEXTLOAD, node.getValueType(0),
                  temp1, temp2, ld.getSrcValue(), svOffset + incrementSize,
                  extraVT, isVolatile, Util.minAlign(alignment, incrementSize));

              ch = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo.getValue(1),
                  hi.getValue(1));
              hi = dag.getNode(ISD.SHL, hi.getValueType(), hi,
                  dag.getConstant(extraWidth, new EVT(tli.getShiftAmountTy()), false));
              result = dag.getNode(ISD.OR, node.getValueType(0), lo, hi);
            }
            temp1 = legalizeOp(result);
            temp2 = legalizeOp(ch);
          } else {
            switch (tli.getLoadExtAction(extType, srcVT)) {
              default:
                Util.shouldNotReachHere("This action isn't supported!");
              case Custom:
                isCustom = true;
              case Legal:
                result = dag.updateNodeOperands(result, temp1, temp2, ld.getOffset());
                temp1 = result.getValue(0);
                temp2 = result.getValue(1);

                if (isCustom) {
                  temp3 = tli.lowerOperation(result, dag);
                  if (temp3.getNode() != null) {
                    temp1 = legalizeOp(temp3);
                    temp2 = legalizeOp(temp3.getValue(1));
                  }
                } else {
                  if (!tli.allowsUnalignedMemoryAccesses(ld.getMemoryVT())) {
                    Type ty = ld.getMemoryVT().getTypeForEVT();
                    int abiAlign = tli.getTargetData().getABITypeAlignment(ty);
                    if (ld.getAlignment() < abiAlign) {
                      result = expandUnalignedLoad((LoadSDNode) result.getNode(),
                          dag, tli);
                      temp1 = result.getOperand(0);
                      temp2 = result.getOperand(1);
                      temp1 = legalizeOp(temp1);
                      temp2 = legalizeOp(temp2);
                    }
                  }
                }
                break;
              case Expand: {
                // f64 = EXTLOAD f32 should expand to LOAD, FP_EXTEND
                if (srcVT.getSimpleVT().simpleVT == MVT.f32 &&
                    node.getValueType(0).getSimpleVT().simpleVT == MVT.f64) {
                  SDValue load = dag.getLoad(srcVT, temp1, temp2, ld.getSrcValue(),
                      ld.getSrcValueOffset(), ld.isVolatile(), ld.getAlignment());
                  result = dag.getNode(ISD.FP_EXTEND, node.getValueType(0), load);
                  temp1 = legalizeOp(result);
                  temp2 = legalizeOp(load.getValue(1));
                  break;
                }
                Util.assertion(extType != EXTLOAD);
                result = dag.getExtLoad(EXTLOAD,
                    node.getValueType(0),
                    temp1, temp2, ld.getSrcValue(),
                    ld.getRawSubclassData(), srcVT,
                    ld.isVolatile(), ld.getAlignment());
                SDValue valRes;
                if (extType == LoadExtType.SEXTLOAD)
                  valRes = dag.getNode(ISD.SIGN_EXTEND_INREG,
                      result.getValueType(), result,
                      dag.getValueType(srcVT));
                else
                  valRes = dag.getZeroExtendInReg(result, srcVT);
                temp1 = legalizeOp(valRes);
                temp2 = legalizeOp(result.getValue(1));
              }
            }

            addLegalizedOperand(new SDValue(node, 0), temp1);
            addLegalizedOperand(new SDValue(node, 1), temp2);
            return val.getResNo() != 0 ? temp2 : temp1;
          }
        }
      }
      case ISD.STORE: {
        Util.assertion(node instanceof StoreSDNode);
        StoreSDNode st = (StoreSDNode) node;
        temp1 = legalizeOp(st.getChain());
        temp2 = legalizeOp(st.getBasePtr());
        int svOffset = st.getSrcValueOffset();
        int alignment = st.getAlignment();
        boolean isVolatile = st.isVolatile();

        if (!st.isTruncatingStore()) {
          SDNode optStore = optimizeFloatStore(st).getNode();
          if (optStore != null) {
            result = new SDValue(optStore, 0);
            break;
          }

          temp3 = legalizeOp(st.getValue());
          result = dag.updateNodeOperands(result, temp1, temp3, temp2,
              st.getOffset());
          EVT vt = temp3.getValueType();
          switch (tli.getOperationAction(ISD.STORE, vt)) {
            default:
              Util.shouldNotReachHere("This action isn't supported yet!");
            case Legal:
              if (!tli.allowsUnalignedMemoryAccesses(st.getMemoryVT())) {
                Type ty = st.getMemoryVT().getTypeForEVT();
                int abiAlign = tli.getTargetData().getABITypeAlignment(ty);
                if (st.getAlignment() < abiAlign) {
                  result = expandUnalignedStore((StoreSDNode) result.getNode(),
                      dag, tli);
                }
              }
              break;
            case Custom:
              temp1 = tli.lowerOperation(result, dag);
              if (temp1.getNode() != null)
                result = temp1;
              break;
            case Promote:
              Util.assertion(vt.isVector(), "Unknown legal promote case!");
              temp3 = dag.getNode(ISD.BIT_CONVERT, tli.getTypeToPromoteType(
                  ISD.STORE, vt), temp3);
              result = dag.getStore(temp1, temp3, temp2, st.getSrcValue(),
                  svOffset, isVolatile, alignment);
              break;
          }
          break;
        } else {
          temp3 = legalizeOp(st.getValue());
          EVT stVT = st.getMemoryVT();
          int stWidth = stVT.getSizeInBits();
          if (stWidth != stVT.getStoreSizeInBits()) {
            EVT nvt = EVT.getIntegerVT(stVT.getStoreSizeInBits());
            temp3 = dag.getZeroExtendInReg(temp3, stVT);
            result = dag.getTruncStore(temp1, temp3, temp2,
                st.getSrcValue(), svOffset, nvt, isVolatile,
                alignment);
          } else if ((stWidth & (stWidth - 1)) != 0) {
            Util.assertion(stVT.isExtended() && !stVT.isVector(), "Unsupported truncstore!");

            int roundWidth = 1 << Util.log2(stWidth);
            Util.assertion(roundWidth < stWidth);
            int extraWidth = stWidth - roundWidth;
            Util.assertion(extraWidth < roundWidth);
            Util.assertion((roundWidth & 7) == 0 && (extraWidth & 7) == 0, "Store size not an integral number of bytes!");

            EVT roundVT = EVT.getIntegerVT(roundWidth);
            EVT extraVT = EVT.getIntegerVT(extraWidth);

            SDValue lo, hi;
            int incrementSize;
            if (tli.isLittleEndian()) {
              // TRUNCSTORE:i24 X -> TRUNCSTORE:i16 X, TRUNCSTORE@+2:i8 (srl X, 16)
              // Store the bottom RoundWidth bits.
              lo = dag.getTruncStore(temp1, temp3, temp2,
                  st.getSrcValue(), svOffset, roundVT,
                  isVolatile, alignment);
              incrementSize = roundWidth / 8;
              temp2 = dag.getNode(ISD.ADD, temp2.getValueType(),
                  temp2, dag.getIntPtrConstant(incrementSize));
              hi = dag.getNode(ISD.SRL, temp3.getValueType(), temp3,
                  dag.getConstant(roundWidth, new EVT(tli.getShiftAmountTy()), false));
              hi = dag.getTruncStore(temp1, hi, temp2, st.getSrcValue(),
                  svOffset + incrementSize, extraVT, isVolatile,
                  Util.minAlign(alignment, incrementSize));
            } else {
              // Big endian - avoid unaligned stores.
              // TRUNCSTORE:i24 X -> TRUNCSTORE:i16 (srl X, 8), TRUNCSTORE@+2:i8 X
              // Store the top RoundWidth bits.
              hi = dag.getNode(ISD.SRL, temp3.getValueType(), temp3,
                  dag.getConstant(extraWidth, new EVT(tli.getShiftAmountTy()), false));
              incrementSize = roundWidth / 8;
              temp2 = dag.getNode(ISD.ADD, temp2.getValueType(), temp2,
                  dag.getIntPtrConstant(incrementSize));
              lo = dag.getTruncStore(temp1, temp3, temp2, st.getSrcValue(),
                  svOffset + incrementSize, extraVT, isVolatile,
                  Util.minAlign(alignment, incrementSize));
            }

            result = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo, hi);
          } else {
            if (!temp1.equals(st.getChain()) || !temp3.equals(st.getValue()) || !temp2
                .equals(st.getBasePtr())) {
              result = dag.updateNodeOperands(result, temp1, temp3,
                  temp2, st.getOffset());
            }

            switch (tli.getTruncStoreAction(st.getValue().getValueType(), stVT)) {
              default:
                Util.shouldNotReachHere("This action is unsupported yet!");
              case Legal:
                if (!tli.allowsUnalignedMemoryAccesses(st.getMemoryVT())) {
                  Type ty = st.getMemoryVT().getTypeForEVT();
                  int abiAlign = tli.getTargetData().getABITypeAlignment(ty);
                  if (st.getAlignment() < abiAlign) {
                    result = expandUnalignedStore((StoreSDNode) result.getNode(),
                        dag, tli);
                  }
                }
                break;
              case Custom:
                result = tli.lowerOperation(result, dag);
                break;
              case Expand:
                Util.assertion(isTypeLegal(stVT));
                temp3 = dag.getNode(ISD.TRUNCATE, stVT, temp3);
                result = dag.getStore(temp1, temp3, temp2, st.getSrcValue(),
                    svOffset, isVolatile, alignment);
                break;
            }
          }
        }
        break;
      }
      default:
        if (Util.DEBUG) {
          System.err.print("NODE: ");
          node.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to legalize this operator!");
        break;
    }
    Util.assertion(result.getValueType().equals(val.getValueType()), "Bad legalization");

    if (!result.equals(val))
      result = legalizeOp(result);

    addLegalizedOperand(val, result);
    return result;
  }

  private static SDValue expandUnalignedStore(StoreSDNode st, SelectionDAG dag,
                                              TargetLowering tli) {
    SDValue chain = st.getChain();
    SDValue ptr = st.getBasePtr();
    SDValue val = st.getValue();
    EVT vt = val.getValueType();
    int alignment = st.getAlignment();
    int svOffset = st.getSrcValueOffset();
    boolean isVolatile = st.isVolatile();
    if (st.getMemoryVT().isFloatingPoint() ||
        st.getMemoryVT().isVector()) {
      EVT inVT = EVT.getIntegerVT(vt.getSizeInBits());
      if (tli.isTypeLegal(inVT)) {
        SDValue result = dag.getNode(ISD.BIT_CONVERT, inVT, val);
        return dag.getStore(chain, result, ptr, st.getSrcValue(),
            svOffset, isVolatile, alignment);
      } else {
        EVT storedVT = st.getMemoryVT();
        EVT regVT = tli.getRegisterType(EVT.getIntegerVT(storedVT.getSizeInBits()));
        int storedBytes = storedVT.getSizeInBits() / 8;
        int regBytes = regVT.getSizeInBits() / 8;
        int numRegs = (storedBytes + regBytes - 1) / regBytes;
        SDValue stackPtr = dag.createStackTemporary(storedVT, regVT);

        SDValue store = dag.getTruncStore(chain, val, stackPtr, null, 0, storedVT);
        SDValue increment = dag.getConstant(regBytes, new EVT(tli.getPointerTy()), false);
        ArrayList<SDValue> stores = new ArrayList<>();
        int offset = 0;

        for (int i = 1; i < numRegs; i++) {
          SDValue load = dag.getLoad(regVT, store, stackPtr, null, 0);
          stores.add(dag.getStore(load.getValue(1), load, ptr,
              st.getSrcValue(), svOffset + offset, isVolatile,
              Util.minAlign(alignment, offset)));
          offset += regBytes;
          stackPtr = dag.getNode(ISD.ADD, stackPtr.getValueType(), stackPtr, increment);
          ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr, increment);
        }

        EVT memVT = EVT.getIntegerVT(8 * (storedBytes - offset));
        SDValue load = dag.getExtLoad(EXTLOAD, regVT, store, stackPtr, null, 0, memVT);
        stores.add(dag.getTruncStore(load.getValue(1), load, ptr, st.getSrcValue(),
            svOffset + offset, memVT, isVolatile, Util.minAlign(alignment, offset)));
        return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), stores);
      }
    }
    EVT newStoredVT = new EVT(st.getMemoryVT().getSimpleVT().simpleVT - 1);
    int numBits = newStoredVT.getSizeInBits();
    int incrementSize = numBits / 8;

    SDValue shiftAmount = dag.getConstant(numBits, new EVT(tli.getShiftAmountTy()), false);
    SDValue lo = val;
    SDValue hi = dag.getNode(ISD.SRL, vt, val, shiftAmount);
    SDValue store1 = dag.getTruncStore(chain, tli.isLittleEndian() ? lo : hi,
        ptr, st.getSrcValue(), svOffset, newStoredVT, isVolatile, alignment);
    ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
        dag.getConstant(incrementSize, new EVT(tli.getPointerTy()), false));
    alignment = Util.minAlign(alignment, incrementSize);
    SDValue store2 = dag.getTruncStore(chain, tli.isLittleEndian() ? hi : lo,
        ptr, st.getSrcValue(), svOffset + incrementSize,
        newStoredVT, isVolatile, alignment);
    return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), store1, store2);
  }

  private static SDValue expandUnalignedLoad(LoadSDNode ld, SelectionDAG dag,
                                             TargetLowering tli) {
    int svOffset = ld.getSrcValueOffset();
    SDValue chain = ld.getChain();
    SDValue ptr = ld.getBasePtr();
    EVT vt = ld.getValueType(0);
    EVT loadedVT = ld.getMemoryVT();
    if (vt.isFloatingPoint() || vt.isVector()) {
      EVT intVT = EVT.getIntegerVT(loadedVT.getSizeInBits());
      if (tli.isTypeLegal(intVT)) {
        SDValue newLoad = dag.getLoad(intVT, chain, ptr, ld.getSrcValue(),
            svOffset, ld.isVolatile(), ld.getAlignment());
        SDValue result = dag.getNode(ISD.BIT_CONVERT, loadedVT, newLoad);
        if (vt.isFloatingPoint() && !loadedVT.equals(vt))
          result = dag.getNode(ISD.FP_EXTEND, vt, result);
        SDValue[] ops = {result, chain};
        return dag.getMergeValues(ops);
      } else {
        EVT regVT = tli.getRegisterType(intVT);
        int loadedBytes = loadedVT.getSizeInBits() / 8;
        int regBytes = regVT.getSizeInBits() / 8;
        int numRegs = (loadedBytes + regBytes - 1) / regBytes;

        SDValue stackBase = dag.createStackTemporary(loadedVT, regVT);
        SDValue increment = dag.getConstant(regBytes, new EVT(tli.getPointerTy()), false);
        ArrayList<SDValue> stores = new ArrayList<>();
        SDValue stackPtr = stackBase;
        int offset = 0;
        for (int i = 1; i < numRegs; i++) {
          SDValue load = dag.getLoad(regVT, chain, ptr, ld.getSrcValue(),
              svOffset + offset, ld.isVolatile(),
              Util.minAlign(ld.getAlignment(), offset));
          stores.add(dag.getStore(load.getValue(1), load, stackPtr, null, 0, false, 0));
          offset += regBytes;
          ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr, increment);
          stackPtr = dag.getNode(ISD.ADD, stackPtr.getValueType(), stackPtr, increment);
        }
        EVT memVT = EVT.getIntegerVT(8 * (loadedBytes - offset));
        SDValue load = dag.getExtLoad(EXTLOAD, regVT, chain, ptr, ld.getSrcValue(),
            svOffset + offset, memVT, ld.isVolatile(),
            Util.minAlign(ld.getAlignment(), offset));

        stores.add(dag.getTruncStore(load.getValue(1), load, stackPtr, null, 0, memVT));
        SDValue tf = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), stores);
        load = dag.getExtLoad(ld.getExtensionType(), vt, tf, stackBase, null, 0, loadedVT);
        SDValue[] ops = {load, tf};
        return dag.getMergeValues(ops);
      }
    }
    Util.assertion(loadedVT.isInteger() && !loadedVT.isVector(), "Unaligned load of unsupported type!");

    int numBits = loadedVT.getSizeInBits();
    EVT newLoadedVT = EVT.getIntegerVT(numBits / 2);
    numBits >>= 1;

    int alignment = ld.getAlignment();
    int incrementSize = numBits / 8;
    LoadExtType extType = ld.getExtensionType();

    if (extType == NON_EXTLOAD)
      extType = ZEXTLOAD;

    SDValue lo, hi;
    if (tli.isLittleEndian()) {
      lo = dag.getExtLoad(ZEXTLOAD, vt, chain, ptr, ld.getSrcValue(),
          svOffset, newLoadedVT, ld.isVolatile(), alignment);
      ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
          dag.getConstant(incrementSize, new EVT(tli.getPointerTy()), false));
      hi = dag.getExtLoad(extType, vt, chain, ptr, ld.getSrcValue(),
          svOffset + incrementSize, newLoadedVT,
          ld.isVolatile(), Util.minAlign(alignment, incrementSize));
    } else {
      hi = dag.getExtLoad(extType, vt, chain, ptr, ld.getSrcValue(),
          svOffset, newLoadedVT, ld.isVolatile(), alignment);
      ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
          dag.getConstant(incrementSize, new EVT(tli.getPointerTy()), false));
      lo = dag.getExtLoad(ZEXTLOAD, vt, chain, ptr, ld.getSrcValue(),
          svOffset + incrementSize, newLoadedVT, ld.isVolatile(),
          Util.minAlign(alignment, incrementSize));
    }
    SDValue shiftAmount = dag.getConstant(numBits, new EVT(tli.getShiftAmountTy()), false);
    SDValue result = dag.getNode(ISD.SHL, vt, hi, shiftAmount);
    result = dag.getNode(ISD.OR, vt, result, lo);
    SDValue tf = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
        lo.getValue(1), hi.getValue(1));
    SDValue[] ops = {result, tf};
    return dag.getMergeValues(ops);
  }

  private static SDNode findCallStartFromCallEnd(SDNode node) {
    Util.assertion(node != null, "Didn't find callseq_start for a call?");

    if (node.getOpcode() == ISD.CALLSEQ_START)
      return node;
    Util.assertion(node.getOperand(0).getValueType().equals(new EVT(MVT.Other)), "Node doesn't have a token chain argument!");

    return findCallStartFromCallEnd(node.getOperand(0).getNode());
  }

  private SDNode findCallEndFromCallStart(SDNode node) {
    int opc = node.getOpcode();
    if (opc == ISD.CALLSEQ_END)
      return node;
    if (node.isUseEmpty())
      return null;
    SDValue theChain = new SDValue(node, node.getNumValues() - 1);
    if (!theChain.getValueType().equals(new EVT(MVT.Other))) {
      theChain = new SDValue(node, 0);
      if (!theChain.getValueType().equals(new EVT(MVT.Other))) {
        for (int i = 1, e = node.getNumValues(); i < e; i++) {
          if (node.getValueType(i).equals(new EVT(MVT.Other))) {
            theChain = new SDValue(node, i);
            break;
          }
        }
        if (!theChain.getValueType().equals(new EVT(MVT.Other)))
          return null;
      }
    }

    for (SDUse u : node.getUseList()) {
      SDNode user = u.getUser();
      for (int i = 0, e = user.getNumOperands(); i < e; i++) {
        if (user.getOperand(i).equals(theChain)) {
          SDNode result = findCallEndFromCallStart(user);
          if (result != null)
            return result;
        }
      }
    }
    return null;
  }

  private SDValue optimizeFloatStore(StoreSDNode st) {
    SDValue temp1 = st.getChain();
    SDValue temp2 = st.getBasePtr();
    SDValue temp3;
    int svOffset = st.getSrcValueOffset();
    boolean isVolatile = st.isVolatile();
    int align = st.getAlignment();
    if (st.getValue().getNode() instanceof ConstantFPSDNode) {
      ConstantFPSDNode fpn = (ConstantFPSDNode) st.getValue().getNode();
      if (fpn.getValueType(0).equals(new EVT(MVT.f32)) &&
          getTypeAction(new EVT(MVT.i32)) == TargetLowering.LegalizeAction.Legal) {
        temp3 = dag.getConstant(fpn.getValueAPF().bitcastToAPInt().zextOrTrunc(32),
            new EVT(MVT.i32), false);
        return dag.getStore(temp1, temp3, temp2, st.getSrcValue(),
            svOffset, isVolatile, align);
      } else if (fpn.getValueType(0).equals(new EVT(MVT.f64))) {
        if (getTypeAction(new EVT(MVT.i64)) == TargetLowering.LegalizeAction.Legal) {
          temp3 = dag.getConstant(fpn.getValueAPF().bitcastToAPInt().
              zextOrTrunc(64), new EVT(MVT.i64), false);
          return dag.getStore(temp1, temp3, temp2, st.getSrcValue(),
              svOffset, isVolatile, align);
        } else if (getTypeAction(new EVT(MVT.i32)) == TargetLowering.LegalizeAction.Legal &&
            !st.isVolatile()) {
          APInt intVal = fpn.getValueAPF().bitcastToAPInt();
          SDValue lo = dag.getConstant(new APInt(intVal).trunc(32), new EVT(MVT.i32), false);
          SDValue hi = dag.getConstant(new APInt(intVal).lshr(32).trunc(32), new EVT(MVT.i32), false);
          if (tli.isBigEndian()) {
            SDValue t = lo;
            lo = hi;
            hi = t;
          }
          lo = dag.getStore(temp1, lo, temp2, st.getSrcValue(),
              svOffset, isVolatile, align);
          temp2 = dag.getNode(ISD.ADD, temp2.getValueType(), temp2,
              dag.getIntPtrConstant(0));
          hi = dag.getStore(temp1, hi, temp2, st.getSrcValue(), svOffset + 4,
              isVolatile, Util.minAlign(align, 4));
          return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo, hi);
        }
      }
    }
    return new SDValue();
  }

  private SDValue performInsertVectorEltInMemory(SDValue vec,
                                                 SDValue val, SDValue idx) {
    SDValue temp3 = idx;
    EVT vt = vec.getValueType();
    EVT eltVT = vt.getVectorElementType();
    EVT idxVT = idx.getValueType();
    EVT ptrVT = new EVT(tli.getPointerTy());
    SDValue stackPtr = dag.createStackTemporary(vt);

    int fi = ((FrameIndexSDNode) stackPtr.getNode()).getFrameIndex();

    SDValue chain = dag.getStore(dag.getEntryNode(), vec, stackPtr,
        PseudoSourceValue.getFixedStack(fi), 0, false, 0);
    int castOpc = idxVT.bitsGT(ptrVT) ? ISD.TRUNCATE : ISD.ZERO_EXTEND;
    temp3 = dag.getNode(castOpc, ptrVT, temp3);
    int eltSize = eltVT.getSizeInBits() / 8;
    temp3 = dag.getNode(ISD.MUL, idxVT, temp3, dag.getConstant(eltSize, idxVT, false));
    SDValue stackPtr2 = dag.getNode(ISD.ADD, idxVT, temp3, stackPtr);
    chain = dag.getTruncStore(chain, val, stackPtr2,
        PseudoSourceValue.getFixedStack(fi), 0, eltVT);
    return dag.getLoad(vt, chain, stackPtr, PseudoSourceValue.getFixedStack(fi), 0);
  }

  private SDValue expandInsertVectorElt(SDValue vec, SDValue val, SDValue idx) {
    if (idx.getNode() instanceof ConstantSDNode) {
      ConstantSDNode cnt = (ConstantSDNode) idx.getNode();
      EVT eltVT = vec.getValueType().getVectorElementType();
      if (val.getValueType().equals(eltVT) ||
          (eltVT.isInteger() && val.getValueType().bitsGE(eltVT))) {
        SDValue scVec = dag.getNode(ISD.SCALAR_TO_VECTOR,
            vec.getValueType(), val);

        int numElts = vec.getValueType().getVectorNumElements();
        int[] ops = new int[numElts];
        for (int i = 0; i < numElts; i++)
          ops[i] = i != cnt.getZExtValue() ? i : numElts;
        return dag.getVectorShuffle(vec.getValueType(), vec, scVec, ops);
      }
    }
    return performInsertVectorEltInMemory(vec, val, idx);
  }

  private SDValue shuffleWithNarrowerEltType(EVT nvt, EVT vt, SDValue n1,
                                             SDValue n2, int[] mask) {
    EVT eltVT = nvt.getVectorElementType();
    int numMaskElts = vt.getVectorNumElements();
    int numDestElts = nvt.getVectorNumElements();
    int numEltsGrowth = numDestElts / numMaskElts;

    Util.assertion(numEltsGrowth != 0);
    if (numEltsGrowth == 1)
      return dag.getVectorShuffle(nvt, n1, n2, mask);

    TIntArrayList newMask = new TIntArrayList();
    for (int i = 0; i < numMaskElts; i++) {
      int idx = mask[i];
      for (int j = 0; j < numEltsGrowth; j++) {
        if (idx < 0)
          newMask.add(-1);
        else
          newMask.add(idx * numEltsGrowth + j);
      }
    }
    Util.assertion(newMask.size() == numDestElts);
    Util.assertion(tli.isShuffleMaskLegal(newMask, nvt));
    return dag.getVectorShuffle(nvt, n1, n2, newMask.toArray());
  }

  private boolean legalizeAllNodesNotLeadingTo(SDNode n, SDNode dest,
                                               HashSet<SDNode> nodesLeadingTo) {
    if (n.equals(dest))
      return true;

    if (nodesLeadingTo.contains(n))
      return true;

    if (legalizeNodes.containsKey(new SDValue(n, 0)))
      return false;

    boolean operandsLeadToDest = false;
    for (int i = 0, e = n.getNumOperands(); i < e; i++) {
      operandsLeadToDest |=
          legalizeAllNodesNotLeadingTo(n.getOperand(i).getNode(),
              dest, nodesLeadingTo);
    }

    if (operandsLeadToDest) {
      nodesLeadingTo.add(n);
      return true;
    }

    legalizeOp(new SDValue(n, 0));
    return false;
  }

  /**
   * values layout as follows.
   * values[0] -- lhs
   * values[1] -- rhs
   * values[2] -- cc
   *
   * @param vt
   * @param values
   */
  private void legalizeSetCCCondCode(EVT vt, SDValue[] values) {
    Util.assertion(values != null && values.length == 3);
    SDValue lhs = values[0], rhs = values[1], cc = values[2];

    EVT opVT = lhs.getValueType();
    CondCode ccCode = ((CondCodeSDNode) cc.getNode()).getCondition();
    switch (tli.getCondCodeAction(ccCode, opVT)) {
      case Legal:
        break;
      case Expand: {
        CondCode cc1 = CondCode.SETCC_INVALID, cc2 = CondCode.SETCC_INVALID;
        int opc = 0;
        switch (ccCode) {
          default:
            Util.shouldNotReachHere("Don't know how to expand this condition!");
          case SETOEQ:
            cc1 = CondCode.SETEQ;
            cc2 = CondCode.SETO;
            opc = ISD.AND;
            break;
          case SETOGT:
            cc1 = CondCode.SETGT;
            cc2 = CondCode.SETO;
            opc = ISD.AND;
            break;
          case SETOGE:
            cc1 = CondCode.SETGE;
            cc2 = CondCode.SETO;
            opc = ISD.AND;
            break;
          case SETOLT:
            cc1 = CondCode.SETLT;
            cc2 = CondCode.SETO;
            opc = ISD.AND;
            break;
          case SETOLE:
            cc1 = CondCode.SETLE;
            cc2 = CondCode.SETO;
            opc = ISD.AND;
            break;
          case SETONE:
            cc1 = CondCode.SETNE;
            cc2 = CondCode.SETO;
            opc = ISD.AND;
            break;
          case SETUEQ:
            cc1 = CondCode.SETEQ;
            cc2 = CondCode.SETUO;
            opc = ISD.OR;
            break;
          case SETUGT:
            cc1 = CondCode.SETGT;
            cc2 = CondCode.SETUO;
            opc = ISD.OR;
            break;
          case SETUGE:
            cc1 = CondCode.SETGE;
            cc2 = CondCode.SETUO;
            opc = ISD.OR;
            break;
          case SETULT:
            cc1 = CondCode.SETLT;
            cc2 = CondCode.SETUO;
            opc = ISD.OR;
            break;
          case SETULE:
            cc1 = CondCode.SETLE;
            cc2 = CondCode.SETUO;
            opc = ISD.OR;
            break;
          case SETUNE:
            cc1 = CondCode.SETNE;
            cc2 = CondCode.SETUO;
            opc = ISD.OR;
            break;
        }
        SDValue setcc1 = dag.getSetCC(vt, lhs, rhs, cc1);
        SDValue setcc2 = dag.getSetCC(vt, lhs, rhs, cc2);
        lhs = dag.getNode(opc, vt, setcc1, setcc2);
        rhs = new SDValue();
        cc = new SDValue();
        break;
      }
    }
    values[0] = lhs;
    values[1] = rhs;
    values[2] = cc;
  }

  private SDValue emitStackConvert(SDValue srcOp, EVT slotVT,
                                   EVT destVT) {
    int srcAlign = tli.getTargetData().getPrefTypeAlignment(
        srcOp.getValueType().getTypeForEVT());
    SDValue ptr = dag.createStackTemporary(slotVT, srcAlign);

    FrameIndexSDNode stackPtrFI = (FrameIndexSDNode) ptr.getNode();
    int fi = stackPtrFI.getFrameIndex();
    Value sv = PseudoSourceValue.getFixedStack(fi);

    int srcSize = srcOp.getValueType().getSizeInBits();
    int slotSize = slotVT.getSizeInBits();
    int destSize = destVT.getSizeInBits();
    int destAlign = tli.getTargetData().getPrefTypeAlignment(destVT.getTypeForEVT());

    SDValue store;
    if (srcSize > slotSize)
      store = dag.getTruncStore(dag.getEntryNode(), srcOp, ptr,
          sv, 0, slotVT, false, srcAlign);
    else {
      Util.assertion(srcSize == slotSize, "Invalid store!");
      store = dag.getStore(dag.getEntryNode(), srcOp, ptr,
          sv, 0, false, srcAlign);
    }

    if (slotSize == destSize) {
      return dag.getLoad(destVT, store, ptr, sv, 0, false, destAlign);
    }
    Util.assertion(slotSize < destSize, "Unknown extension!");
    return dag.getExtLoad(EXTLOAD, destVT, store, ptr, sv,
        0, slotVT, false, destAlign);
  }

  private SDValue expandBuildVector(SDNode node) {
    int numElts = node.getNumOperands();
    SDValue value1 = new SDValue(), value2 = new SDValue();
    EVT vt = node.getValueType(0);
    EVT opVT = node.getOperand(0).getValueType();
    EVT eltVT = vt.getVectorElementType();

    boolean isOnlyLowElt = true;
    boolean moreThanTwoValues = false;
    boolean isConstant = true;
    for (int i = 0; i < numElts; i++) {
      SDValue v = node.getOperand(i);
      if (v.getOpcode() == ISD.UNDEF)
        continue;
      if (i > 0)
        isOnlyLowElt = false;
      if (!(v.getNode() instanceof ConstantFPSDNode) &&
          !(v.getNode() instanceof ConstantSDNode)) {
        isConstant = false;
      }

      if (value1.getNode() == null)
        value1 = v;
      else if (value2.getNode() == null) {
        if (!v.equals(value1))
          value2 = v;
      } else if (!v.equals(value1) && !v.equals(value2))
        moreThanTwoValues = true;
    }

    if (value1.getNode() == null)
      return dag.getUNDEF(vt);

    if (isOnlyLowElt)
      return dag.getNode(ISD.SCALAR_TO_VECTOR, vt, node.getOperand(0));

    if (isConstant) {
      ArrayList<Constant> cv = new ArrayList<>();
      for (int i = 0; i < numElts; i++) {
        SDValue op = node.getOperand(i);
        if (op.getNode() instanceof ConstantFPSDNode) {
          ConstantFPSDNode fp = (ConstantFPSDNode) op.getNode();
          cv.add(fp.getConstantFPValue());
        } else if (op.getNode() instanceof ConstantSDNode) {
          ConstantSDNode cnt = (ConstantSDNode) op.getNode();
          cv.add(cnt.getConstantIntValue());
        } else {
          Util.assertion(op.getOpcode() == ISD.UNDEF);
          Type opTy = opVT.getTypeForEVT();
          cv.add(Value.UndefValue.get(opTy));
        }
      }
    }

    if (!moreThanTwoValues) {
      TIntArrayList shuffleNums = new TIntArrayList();
      for (int i = 0; i < numElts; i++) {
        SDValue v = node.getOperand(i);
        if (v.getOpcode() == ISD.UNDEF)
          continue;
        shuffleNums.add(v.equals(value1) ? 0 : numElts);
      }

      if (tli.isShuffleMaskLegal(shuffleNums, node.getValueType(0))) {
        SDValue vec1 = dag.getNode(ISD.SCALAR_TO_VECTOR, vt, value1);
        SDValue vec2 = value2.getNode() != null ?
            dag.getNode(ISD.SCALAR_TO_VECTOR, vt, value2) :
            dag.getUNDEF(vt);
        return dag.getVectorShuffle(vt, vec1, vec2, shuffleNums.toArray());
      }
    }
    return expandVectorBuildThroughStack(node);
  }

  private SDValue expandScalarToVector(SDNode node) {
    SDValue stackPtr = dag.createStackTemporary(node.getValueType(0));
    FrameIndexSDNode stackPtrFI = (FrameIndexSDNode) stackPtr.getNode();
    int fi = stackPtrFI.getFrameIndex();

    SDValue chain = dag.getTruncStore(dag.getEntryNode(), node.getOperand(0),
        stackPtr, PseudoSourceValue.getFixedStack(fi), 0,
        node.getValueType(0).getVectorElementType());
    return dag.getLoad(node.getValueType(0), chain, stackPtr,
        PseudoSourceValue.getFixedStack(fi), 0);
  }

  private SDValue expandDBGStoppoint(SDNode node) {
    Util.shouldNotReachHere("Shouldn't reach here!");
    return null;
  }

  private void expandDynamicStackAlloc(SDNode node, ArrayList<SDValue> results) {
    int spreg = tli.getStackPointerRegisterToSaveRestore();
    Util.assertion(spreg != 0);
    EVT vt = node.getValueType(0);
    SDValue temp1 = new SDValue(node, 0);
    SDValue temp2 = new SDValue(node, 1);
    SDValue temp3 = node.getOperand(2);
    SDValue chain = temp1.getOperand(0);

    chain = dag.getCALLSEQ_START(chain, dag.getIntPtrConstant(0, true));
    SDValue size = temp2.getOperand(1);
    SDValue sp = dag.getCopyFromReg(chain, spreg, vt);
    chain = sp.getValue(1);
    long align = ((ConstantSDNode) temp3.getNode()).getZExtValue();
    int stackAlign = tli.getTargetMachine().getFrameInfo().getStackAlignment();
    if (align > stackAlign)
      sp = dag.getNode(ISD.AND, vt, sp, dag.getConstant(-align, vt, false));
    temp1 = dag.getNode(ISD.SUB, vt, sp, size);
    chain = dag.getCopyToReg(chain, spreg, temp1);
    temp2 = dag.getCALLSEQ_END(chain, dag.getIntPtrConstant(0, true),
        dag.getIntPtrConstant(0, true), new SDValue());
    results.add(temp1);
    results.add(temp2);
  }

  private SDValue expandFCopySign(SDNode node) {
    SDValue temp1 = node.getOperand(0);
    SDValue temp2 = node.getOperand(1);
    int simpleVT = temp2.getValueType().getSimpleVT().simpleVT;
    Util.assertion(simpleVT == MVT.f32 || simpleVT == MVT.f64);
    SDValue signBit;
    EVT iVT = temp2.getValueType().getSimpleVT().simpleVT == MVT.f64 ?
        new EVT(MVT.i64) : new EVT(MVT.i32);
    if (isTypeLegal(iVT))
      signBit = dag.getNode(ISD.BIT_CONVERT, iVT, temp2);
    else {
      Util.assertion(isTypeLegal(new EVT(tli.getPointerTy())) && (tli.getPointerTy().simpleVT == MVT.i32 ||
              tli.getPointerTy().simpleVT == MVT.i64),
          "Legal type for load?");

      SDValue stackPtr = dag.createStackTemporary(temp2.getValueType());
      SDValue storePtr = stackPtr, loadPtr = stackPtr;
      SDValue ch = dag.getStore(dag.getEntryNode(), temp2, storePtr,
          null, 0, false, 0);
      if (temp2.getValueType().getSimpleVT().simpleVT == MVT.f64 &&
          tli.isLittleEndian()) {
        loadPtr = dag.getNode(ISD.ADD, stackPtr.getValueType(),
            loadPtr, dag.getIntPtrConstant(4));
      }
      signBit = dag.getExtLoad(LoadExtType.SEXTLOAD, new EVT(tli.getPointerTy()),
          ch, loadPtr, null, 0, new EVT(MVT.i32));
    }
    signBit = dag.getSetCC(new EVT(tli.getSetCCResultType(signBit.getValueType())),
        signBit, dag.getConstant(0, signBit.getValueType(), false),
        CondCode.SETLT);
    SDValue absVal = dag.getNode(ISD.FABS, temp1.getValueType(), temp1);
    return dag.getNode(ISD.SELECT, absVal.getValueType(), signBit,
        dag.getNode(ISD.FNEG, absVal.getValueType(), absVal), absVal);
  }

  private SDValue expandLegalIntToFP(SDValue op,
                                     EVT destVT, boolean isSigned) {
    if (op.getValueType().getSimpleVT().simpleVT == MVT.i32) {
      SDValue stackSlot = dag.createStackTemporary(new EVT(MVT.f64));
      SDValue wordOff = dag.getConstant(32, new EVT(tli.getPointerTy()), false);
      SDValue hi = stackSlot;
      SDValue lo = dag.getNode(ISD.ADD, new EVT(tli.getPointerTy()), stackSlot,
          wordOff);
      if (tli.isLittleEndian()) {
        SDValue t = hi;
        hi = lo;
        lo = t;
      }
      SDValue op0Operand = new SDValue();
      if (isSigned) {
        SDValue signBit = dag.getConstant(0x80000000L, new EVT(MVT.i32), false);
        op0Operand = dag.getNode(ISD.XOR, new EVT(MVT.i32), op, signBit);
      } else {
        op0Operand = op;
      }

      SDValue store1 = dag.getStore(dag.getEntryNode(), op0Operand, lo,
          null, 0, false, 0);
      SDValue initialHi = dag.getConstant(0x43000000L, new EVT(MVT.i32), false);
      SDValue store2 = dag.getStore(store1, initialHi, hi, null, 0, false, 0);
      SDValue load = dag.getLoad(new EVT(MVT.i64), store2, stackSlot, null, 0);
      SDValue bias = dag.getConstantFP(isSigned ?
              bitsToDouble(0x4330000080000000L) :
              bitsToDouble(0x4330000000000000L),
          new EVT(MVT.f64), false);
      SDValue sub = dag.getNode(ISD.FSUB, new EVT(MVT.f64), load, bias);
      SDValue result = new SDValue();
      if (destVT.getSimpleVT().simpleVT == MVT.f64)
        result = sub;
      else if (destVT.bitsGT(new EVT(MVT.f64)))
        result = dag.getNode(ISD.FP_ROUND, destVT, sub, dag.getIntPtrConstant(0));
      else if (destVT.bitsGT(new EVT(MVT.f64)))
        result = dag.getNode(ISD.FP_EXTEND, destVT, sub);
      return result;
    }
    Util.assertion(!isSigned, "Legalize can't expand SINT_TO_FP for i64 yet!");
    SDValue temp1 = dag.getNode(ISD.SINT_TO_FP, destVT, op);
    SDValue signSet = dag.getSetCC(new EVT(tli.getSetCCResultType(op.getValueType())),
        op, dag.getConstant(0, op.getValueType(), false),
        CondCode.SETLT);
    SDValue zero = dag.getIntPtrConstant(0), four = dag.getIntPtrConstant(4);
    SDValue cstOffset = dag.getNode(ISD.SELECT, zero.getValueType(),
        signSet, four, zero);

    long ff = 0;
    switch (op.getValueType().getSimpleVT().simpleVT) {
      case MVT.i8:
        ff = 0x43800000L;
        break;
      case MVT.i16:
        ff = 0x47800000L;
        break;
      case MVT.i32:
        ff = 0x4F800000L;
        break;
      case MVT.i64:
        ff = 0x5F800000L;
        break;
      default:
        Util.shouldNotReachHere("Unsupported integer type!");
        break;
    }
    if (tli.isLittleEndian()) {
      ff <<= 32;
    }
    Constant fudgeFactor = ConstantInt.get(LLVMContext.Int64Ty, ff);
    SDValue cpIdx = dag.getConstantPool(fudgeFactor, new EVT(tli.getPointerTy()),
        0, 0, false, 0);
    int align = ((ConstantPoolSDNode) cpIdx.getNode()).getAlign();
    cpIdx = dag.getNode(ISD.ADD, new EVT(tli.getPointerTy()), cpIdx, cstOffset);
    align = Math.min(align, 4);
    SDValue fudgeInReg;
    if (destVT.getSimpleVT().simpleVT == MVT.f32) {
      fudgeInReg = dag.getLoad(new EVT(MVT.f32), dag.getEntryNode(), cpIdx,
          PseudoSourceValue.getConstantPool(), 0, false, align);
    } else {
      fudgeInReg = legalizeOp(dag.getExtLoad(EXTLOAD, destVT,
          dag.getEntryNode(), cpIdx, PseudoSourceValue.getConstantPool(),
          0, new EVT(MVT.f32), false, align));
    }
    return dag.getNode(ISD.FADD, destVT, temp1, fudgeInReg);
  }

  private SDValue promoteLegalIntToFP(SDValue legalOp,
                                      EVT destVT, boolean isSigned) {
    EVT newInTy = legalOp.getValueType();
    int opToUse = 0;

    while (true) {
      newInTy = new EVT(newInTy.getSimpleVT().simpleVT + 1);
      Util.assertion(newInTy.isInteger(), "Ran out of possiblilities!");

      if (tli.isOperationLegalOrCustom(ISD.SINT_TO_FP, newInTy)) {
        opToUse = ISD.SINT_TO_FP;
        break;
      }
      if (isSigned)
        continue;
      if (tli.isOperationLegalOrCustom(ISD.UINT_TO_FP, newInTy)) {
        opToUse = ISD.UINT_TO_FP;
        break;
      }
    }

    return dag.getNode(opToUse, destVT,
        dag.getNode(isSigned ? ISD.SIGN_EXTEND : ISD.ZERO_EXTEND,
            newInTy, legalOp));
  }

  private SDValue promoteLegalFPToInt(SDValue legalOp,
                                      EVT destVT, boolean isSigned) {
    EVT newOutTy = destVT;
    int opToUse = 0;

    while (true) {
      newOutTy = new EVT(newOutTy.getSimpleVT().simpleVT + 1);
      Util.assertion(newOutTy.isInteger(), "Ran out of possiblilities!");

      if (tli.isOperationLegalOrCustom(ISD.FP_TO_SINT, newOutTy)) {
        opToUse = ISD.FP_TO_SINT;
        break;
      }
      if (isSigned)
        continue;
      if (tli.isOperationLegalOrCustom(ISD.FP_TO_UINT, newOutTy)) {
        opToUse = ISD.FP_TO_UINT;
        break;
      }
    }

    SDValue operation = dag.getNode(opToUse, newOutTy, legalOp);
    return dag.getNode(ISD.TRUNCATE, destVT, operation);
  }

  private SDValue expandBSWAP(SDValue op) {
    EVT vt = op.getValueType();
    EVT shVT = new EVT(tli.getShiftAmountTy());
    SDValue temp1, temp2, temp3, temp4, temp5, temp6, temp7, temp8;
    switch (vt.getSimpleVT().simpleVT) {
      case MVT.i16:
        temp2 = dag.getNode(ISD.SHL, vt, op, dag.getConstant(8, shVT, false));
        temp1 = dag.getNode(ISD.SRL, vt, op, dag.getConstant(8, shVT, false));
        return dag.getNode(ISD.OR, vt, temp1, temp2);
      case MVT.i32:
        temp4 = dag.getNode(ISD.SHL, vt, op, dag.getConstant(24, shVT, false));
        temp3 = dag.getNode(ISD.SHL, vt, op, dag.getConstant(8, shVT, false));
        temp2 = dag.getNode(ISD.SRL, vt, op, dag.getConstant(8, shVT, false));
        temp1 = dag.getNode(ISD.SRL, vt, op, dag.getConstant(24, shVT, false));
        temp3 = dag.getNode(ISD.AND, vt, temp3, dag.getConstant(0xFF0000, vt, false));
        temp2 = dag.getNode(ISD.AND, vt, temp2, dag.getConstant(0xFF00, vt, false));
        temp4 = dag.getNode(ISD.OR, vt, temp4, temp3);
        temp2 = dag.getNode(ISD.OR, vt, temp4, temp2);
        return dag.getNode(ISD.OR, vt, temp4, temp2);
      case MVT.i64:
        temp8 = dag.getNode(ISD.SHL, vt, op, dag.getConstant(56, shVT, false));
        temp7 = dag.getNode(ISD.SHL, vt, op, dag.getConstant(40, shVT, false));
        temp6 = dag.getNode(ISD.SHL, vt, op, dag.getConstant(24, shVT, false));
        temp5 = dag.getNode(ISD.SHL, vt, op, dag.getConstant(8, shVT, false));
        temp4 = dag.getNode(ISD.SRL, vt, op, dag.getConstant(8, shVT, false));
        temp3 = dag.getNode(ISD.SRL, vt, op, dag.getConstant(24, shVT, false));
        temp2 = dag.getNode(ISD.SRL, vt, op, dag.getConstant(40, shVT, false));
        temp1 = dag.getNode(ISD.SRL, vt, op, dag.getConstant(56, shVT, false));
        temp7 = dag.getNode(ISD.AND, vt, temp7, dag.getConstant(255L << 48, vt, false));
        temp6 = dag.getNode(ISD.AND, vt, temp6, dag.getConstant(255L << 40, vt, false));
        temp5 = dag.getNode(ISD.AND, vt, temp5, dag.getConstant(255L << 32, vt, false));
        temp4 = dag.getNode(ISD.AND, vt, temp4, dag.getConstant(255L << 24, vt, false));
        temp3 = dag.getNode(ISD.AND, vt, temp3, dag.getConstant(255L << 16, vt, false));
        temp2 = dag.getNode(ISD.AND, vt, temp2, dag.getConstant(255L << 8, vt, false));
        temp8 = dag.getNode(ISD.OR, vt, temp8, temp7);
        temp6 = dag.getNode(ISD.OR, vt, temp6, temp5);
        temp4 = dag.getNode(ISD.OR, vt, temp4, temp3);
        temp2 = dag.getNode(ISD.OR, vt, temp2, temp1);
        temp8 = dag.getNode(ISD.OR, vt, temp8, temp6);
        temp4 = dag.getNode(ISD.OR, vt, temp4, temp2);
        return dag.getNode(ISD.OR, vt, temp8, temp4);
      default:
        Util.shouldNotReachHere("Unhandled expand type BSWAP!");
        return null;
    }
  }

  private SDValue expandBitCount(int opc, SDValue op) {
    switch (opc) {
      default:
        Util.shouldNotReachHere("Can't handle this yet!");
        return null;
      case ISD.CTPOP: {
        long[] mask = {0x5555555555555555L, 0x3333333333333333L,
            0x0F0F0F0F0F0F0F0FL, 0x00FF00FF00FF00FFL,
            0x0000FFFF0000FFFFL, 0x00000000FFFFFFFFL};
        EVT vt = op.getValueType();
        EVT shVT = new EVT(tli.getShiftAmountTy());
        int len = vt.getSizeInBits();
        for (int i = 0; (1 << i) <= len / 2; i++) {
          //x = (x & mask[i][len/8]) + (x >> (1 << i) & mask[i][len/8])
          int eltSize = vt.isVector() ?
              vt.getVectorElementType().getSizeInBits() :
              len;
          SDValue temp2 = dag
              .getConstant(new APInt(eltSize, mask[i]), vt, false);
          SDValue temp3 = dag.getConstant(1L << i, shVT, false);
          op = dag.getNode(ISD.ADD, vt,
              dag.getNode(ISD.AND, vt, op, temp2),
              dag.getNode(ISD.AND, vt,
                  dag.getNode(ISD.SRL, vt, op, temp3), temp2));
        }
        return op;
      }
      case ISD.CTLZ: {
        // for now, we do this:
        // x = x | (x >> 1);
        // x = x | (x >> 2);
        // ...
        // x = x | (x >>16);
        // x = x | (x >>32); // for 64-bit input
        // return popcount(~x);
        // but see also: http://www.hackersdelight.org/HDcode/nlz.cc
        EVT vt = op.getValueType();
        EVT shVT = new EVT(tli.getShiftAmountTy());
        int len = vt.getSizeInBits();
        for (int i = 0; (1L << i) <= len / 2; i++) {
          SDValue temp3 = dag.getConstant(1L << i, shVT, false);
          op = dag.getNode(ISD.OR, vt, op, dag.getNode(ISD.SRL, vt, op, temp3));
        }
        op = dag.getNOT(op, vt);
        return dag.getNode(ISD.CTPOP, vt, op);
      }
      case ISD.CTTZ: {
        EVT vt = op.getValueType();
        SDValue temp3 = dag.getNode(ISD.AND, vt, dag.getNOT(op, vt),
            dag.getNode(ISD.SUB, vt, op, dag.getConstant(1, vt, false)));
        if (!tli.isOperationLegalOrCustom(ISD.CTPOP, vt) &&
            tli.isOperationLegalOrCustom(ISD.CTLZ, vt)) {
          return dag.getNode(ISD.SUB, vt, dag.getConstant(vt.getSizeInBits(), vt, false),
              dag.getNode(ISD.CTLZ, vt, temp3));
        }
        return dag.getNode(ISD.CTPOP, vt, temp3);
      }
    }
  }

  private SDValue expandExtractFromVectorThroughStack(SDValue op) {
    SDValue vec = op.getOperand(0);
    SDValue idx = op.getOperand(1);
    SDValue stackPtr = dag.createStackTemporary(vec.getValueType());
    SDValue chain = dag.getStore(dag.getEntryNode(), vec, stackPtr,
        null, 0, false, 0);

    int eltSize = vec.getValueType().getVectorElementType().getSizeInBits() / 8;
    idx = dag.getNode(ISD.MUL, idx.getValueType(), idx,
        dag.getConstant(eltSize, idx.getValueType(), false));

    if (idx.getValueType().bitsGT(new EVT(tli.getPointerTy())))
      idx = dag.getNode(ISD.TRUNCATE, new EVT(tli.getPointerTy()), idx);
    else
      idx = dag.getNode(ISD.ZERO_EXTEND, new EVT(tli.getPointerTy()), idx);

    stackPtr = dag.getNode(ISD.ADD, idx.getValueType(), idx, stackPtr);
    if (op.getValueType().isVector())
      return dag.getLoad(op.getValueType(), chain, stackPtr, null, 0);
    else
      return dag.getExtLoad(EXTLOAD, op.getValueType(),
          chain, stackPtr, null, 0,
          vec.getValueType().getVectorElementType());
  }

  private SDValue expandVectorBuildThroughStack(SDNode node) {
    EVT vt = node.getValueType(0);
    EVT opVT = node.getOperand(0).getValueType();
    SDValue fiPtr = dag.createStackTemporary(vt);

    int fi = ((FrameIndexSDNode) fiPtr.getNode()).getFrameIndex();
    Value sv = PseudoSourceValue.getFixedStack(fi);

    ArrayList<SDValue> stores = new ArrayList<>();
    int typeByteSize = opVT.getSizeInBits() / 8;
    for (int i = 0, e = node.getNumOperands(); i < e; i++) {
      if (node.getOperand(i).getOpcode() == ISD.UNDEF)
        continue;

      int offset = typeByteSize * i;
      SDValue idx = dag.getConstant(offset, fiPtr.getValueType(), false);
      idx = dag.getNode(ISD.ADD, fiPtr.getValueType(), fiPtr, idx);
      stores.add(dag.getStore(dag.getEntryNode(), node.getOperand(i),
          idx, sv, offset, false, 0));
    }
    SDValue storeChain;
    if (!stores.isEmpty())
      storeChain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
          stores);
    else
      storeChain = dag.getEntryNode();

    return dag.getLoad(vt, storeChain, fiPtr, sv, 0);
  }

  private void expandNode(SDNode node, ArrayList<SDValue> results) {
    SDValue temp1, temp2, temp3, temp4;
    switch (node.getOpcode()) {
      case ISD.CTPOP:
      case ISD.CTLZ:
      case ISD.CTTZ:
        temp1 = expandBitCount(node.getOpcode(), node.getOperand(0));
        results.add(temp1);
        break;
      case ISD.BSWAP:
        results.add(expandBSWAP(node.getOperand(0)));
        break;
      case ISD.FRAMEADDR:
      case ISD.RETURNADDR:
      case ISD.FRAME_TO_ARGS_OFFSET:
        results.add(dag.getConstant(0, node.getValueType(0), false));
        break;
      case ISD.FLT_ROUNDS_:
        results.add(dag.getConstant(1, node.getValueType(0), false));
        break;
      case ISD.EH_RETURN:
      case ISD.DECLARE:
      case ISD.DBG_LABEL:
      case ISD.EH_LABEL:
      case ISD.PREFETCH:
      case ISD.MEMBARRIER:
      case ISD.VAEND:
        results.add(node.getOperand(0));
        break;
      case ISD.DBG_STOPPOINT:
        results.add(expandDBGStoppoint(node));
        break;
      case ISD.DYNAMIC_STACKALLOC:
        expandDynamicStackAlloc(node, results);
        break;
      case ISD.MERGE_VALUES:
        for (int i = 0, e = node.getNumValues(); i < e; i++) {
          results.add(node.getOperand(i));
        }
        break;
      case ISD.UNDEF: {
        EVT vt = node.getValueType(0);
        if (vt.isInteger())
          results.add(dag.getConstant(0, vt, false));
        else if (vt.isFloatingPoint())
          results.add(dag.getConstantFP(0, vt, false));
        else
          Util.shouldNotReachHere("Unknown value type!");
        ;
        break;
      }
      case ISD.TRAP: {
        // if this operation is unsurpported, lower it to abort call.
        ArrayList<ArgListEntry> args = new ArrayList<>();
        Pair<SDValue, SDValue> callResult =
            tli.lowerCallTo(node.getOperand(0),
                LLVMContext.VoidTy,
                false, false, false,
                false, 0,
                CallingConv.C, false,
                true/*isReturnValueUsed = true*/,
                dag.getExternalSymbol("abort", new EVT(tli.getPointerTy())),
                args, dag);
        results.add(callResult.second);
        break;
      }
      case ISD.FP_ROUND:
      case ISD.BIT_CONVERT:
        temp1 = emitStackConvert(node.getOperand(0),
            node.getValueType(0),
            node.getValueType(0));
        results.add(temp1);
        break;
      case ISD.FP_EXTEND:
        temp1 = emitStackConvert(node.getOperand(0),
            node.getOperand(0).getValueType(),
            node.getValueType(0));
        results.add(temp1);
        break;
      case ISD.SIGN_EXTEND_INREG: {
        EVT extraVT = ((VTSDNode) node.getOperand(1).getNode()).getVT();
        int bitsDiff = node.getValueType(0).getSizeInBits() -
            extraVT.getSizeInBits();
        SDValue shiftCst = dag.getConstant(bitsDiff,
            new EVT(tli.getShiftAmountTy()), false);
        temp1 = dag.getNode(ISD.SHL, node.getValueType(0),
            node.getOperand(0), shiftCst);
        temp1 = dag.getNode(ISD.SRA, node.getValueType(0), temp1, shiftCst);
        results.add(temp1);
        break;
      }
      case ISD.FP_ROUND_INREG: {
        EVT extraVT = ((VTSDNode) node.getOperand(1).getNode()).getVT();
        temp1 = emitStackConvert(node.getOperand(0), extraVT,
            node.getValueType(0));
        results.add(temp1);
        break;
      }
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP: {
        temp1 = expandLegalIntToFP(node.getOperand(0),
            node.getValueType(0),
            node.getOpcode() == ISD.SINT_TO_FP);
        results.add(temp1);
        break;
      }
      case ISD.FP_TO_SINT: {
        SDValue trueVal, falseVal;
        EVT vt = node.getOperand(0).getValueType();
        EVT nvt = node.getValueType(0);
        long[] zero = {0, 0};
        APFloat apf = new APFloat(new APInt(vt.getSizeInBits(), 2, zero));
        APInt x = APInt.getSignBit(nvt.getSizeInBits());
        apf.convertFromAPInt(x, false, APFloat.RoundingMode.rmNearestTiesToEven);
        temp1 = dag.getConstantFP(apf, vt, false);
        temp2 = dag.getSetCC(new EVT(tli.getSetCCResultType(vt)),
            node.getOperand(0),
            temp1, CondCode.SETLT);
        trueVal = dag.getNode(ISD.FP_TO_SINT, nvt, node.getOperand(0));
        falseVal = dag.getNode(ISD.FP_TO_SINT, nvt,
            dag.getNode(ISD.FSUB, vt, node.getOperand(0), temp1));
        falseVal = dag.getNode(ISD.XOR, nvt, falseVal,
            dag.getConstant(x, nvt, false));
        temp1 = dag.getNode(ISD.SELECT, nvt, temp2, trueVal, falseVal);
        results.add(temp1);
        break;
      }
      case ISD.VAARG: {
        Value v = ((SrcValueSDNode) node.getOperand(2).getNode()).getValue();
        EVT vt = node.getValueType(0);
        temp1 = node.getOperand(0);
        temp2 = node.getOperand(1);
        SDValue valist = dag.getLoad(
            new EVT(tli.getPointerTy()),
            temp1, temp2, v, 0);
        temp3 = dag.getNode(ISD.ADD, new EVT(tli.getPointerTy()),
            valist, dag.getConstant(tli.getTargetData()
                    .getTypeAllocSize(vt.getTypeForEVT()),
                new EVT(tli.getPointerTy()),
                false));
        temp3 = dag.getStore(valist.getValue(1), temp3, temp2, v, 0, false, 0);
        results.add(dag.getLoad(vt, temp3, valist, null, 0));
        results.add(results.get(0).getValue(1));
        ;
        break;
      }
      case ISD.VACOPY: {
        Value vd = ((SrcValueSDNode) node.getOperand(3).getNode()).getValue();
        Value vs = ((SrcValueSDNode) node.getOperand(4).getNode()).getValue();
        temp1 = dag.getLoad(
            new EVT(tli.getPointerTy()),
            node.getOperand(0),
            node.getOperand(2),
            vs, 0);
        temp1 = dag.getStore(temp1.getValue(1), temp1, node.getOperand(1),
            vd, 0, false, 0);
        results.add(temp1);
        break;
      }
      case ISD.EXTRACT_VECTOR_ELT: {
        if (node.getOperand(0).getValueType().getVectorNumElements() == 1) {
          temp1 = dag.getNode(ISD.BIT_CONVERT, node.getValueType(0),
              node.getOperand(0));
        } else {
          temp1 = expandExtractFromVectorThroughStack(new SDValue(node, 0));
        }
        results.add(temp1);
        break;
      }
      case ISD.EXTRACT_SUBVECTOR: {
        results.add(expandExtractFromVectorThroughStack(new SDValue(node, 0)));
        break;
      }
      case ISD.CONCAT_VECTORS: {
        results.add(expandVectorBuildThroughStack(node));
        break;
      }
      case ISD.SCALAR_TO_VECTOR:
        results.add(expandVectorBuildThroughStack(node));
        break;
      case ISD.INSERT_VECTOR_ELT:
        results.add(expandInsertVectorElt(
            node.getOperand(0),
            node.getOperand(1),
            node.getOperand(2)));
        break;
      case ISD.VECTOR_SHUFFLE: {
        TIntArrayList mask = new TIntArrayList();
        ((ShuffleVectorSDNode) node).getMask(mask);
        ;

        EVT vt = node.getValueType(0);
        EVT eltVT = vt.getVectorElementType();
        int numElts = vt.getVectorNumElements();
        ArrayList<SDValue> ops = new ArrayList<>();
        for (int i = 0; i < numElts; i++) {
          if (mask.get(i) == 0) {
            ops.add(dag.getUNDEF(eltVT));
            continue;
          }
          int idx = mask.get(i);
          if (idx < numElts) {
            ops.add(dag.getNode(ISD.EXTRACT_VECTOR_ELT,
                eltVT,
                node.getOperand(0),
                dag.getIntPtrConstant(idx)));
          } else {
            ops.add(dag.getNode(ISD.EXTRACT_VECTOR_ELT,
                eltVT,
                node.getOperand(1),
                dag.getIntPtrConstant(idx - numElts)));
          }
        }
        temp1 = dag.getNode(ISD.BUILD_VECTOR, vt, ops);
        results.add(temp1);
        break;
      }
      case ISD.EXTRACT_ELEMENT: {
        EVT opTy = node.getOperand(0).getValueType();
        if (((ConstantSDNode) node.getOperand(1).getNode()).getZExtValue() != 0) {
          // 1 > Hi
          temp1 = dag.getNode(ISD.SRL, opTy, node.getOperand(0),
              dag.getConstant(opTy.getSizeInBits() / 2,
                  new EVT(tli.getShiftAmountTy()), false));
          temp1 = dag.getNode(ISD.TRUNCATE, node.getValueType(0), temp1);
        } else {
          temp1 = dag.getNode(ISD.TRUNCATE, node.getValueType(0),
              node.getOperand(0));
        }
        results.add(temp1);
        break;
      }
      case ISD.STACKSAVE: {
        // Expand to CopyFromReg if the target
        // set stackPointerRegisterToSaveRestore.
        int sp = tli.getStackPointerRegisterToSaveRestore();
        if (sp != 0) {
          results.add(dag.getCopyFromReg(
              node.getOperand(0),
              sp,
              node.getValueType(0)));
          results.add(results.get(0).getValue(1));
        } else {
          results.add(dag.getUNDEF(node.getValueType(0)));
          results.add(node.getOperand(0));
        }
        break;
      }
      case ISD.STACKRESTORE: {
        int sp = tli.getStackPointerRegisterToSaveRestore();
        if (sp != 0) {
          results.add(dag.getCopyToReg(node.getOperand(0),
              sp, node.getOperand(1)));
        } else {
          results.add(node.getOperand(0));
        }
        break;
      }
      case ISD.FCOPYSIGN:
        results.add(expandFCopySign(node));
        break;
      case ISD.FNEG:
        // Expand Y = FNEG(X) -> Y = SUB -0.0, X
        temp1 = dag.getConstantFP(-0.0, node.getValueType(0), false);
        temp1 = dag.getNode(ISD.FSUB, node.getValueType(0),
            temp1, node.getOperand(0));
        results.add(temp1);
        break;
      case ISD.FABS: {
        // Expand Y = FABS(X) -> Y = (X >u 0.0) ? X : fneg(X).
        EVT vt = node.getValueType(0);
        temp1 = node.getOperand(0);
        temp2 = dag.getConstantFP(0.0, vt, false);
        temp2 = dag.getSetCC(
            new EVT(tli.getSetCCResultType(temp1.getValueType())),
            temp1, temp2, CondCode.SETUGT);
        temp3 = dag.getNode(ISD.FNEG, vt, temp1);
        temp1 = dag.getNode(ISD.SELECT, vt, temp2, temp1, temp3);
        results.add(temp1);
        break;
      }
      case ISD.FSQRT:
        results.add(expandFPLibCall(node, RTLIB.SQRT_F32, RTLIB.SQRT_F64,
            RTLIB.SQRT_F80, RTLIB.SQRT_PPCF128));
        break;
      case ISD.ConstantFP: {
        ConstantFPSDNode fp = (ConstantFPSDNode) node;
        boolean isLegal = false;
        for (int i = 0, e = tli.getNumLegalFPImmediate(); i < e; i++) {
          if (fp.isExactlyValue(tli.getLegalImmediate(i))) {
            isLegal = true;
            break;
          }
        }
        if (isLegal)
          results.add(new SDValue(node, 0));
        else
          results.add(expandConstantFP(fp, true, dag, tli));
        break;
      }
      default:
        if (Util.DEBUG) {
          node.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Unknown opcode!");
        break;
    }
  }

  /**
   * Expand the constantFP into an integer constant or a load from the
   * constant pool.
   *
   * @param fp
   * @param useCP
   * @param dag
   * @param tli
   * @return
   */
  private static SDValue expandConstantFP(ConstantFPSDNode fp,
                                          boolean useCP, SelectionDAG dag, TargetLowering tli) {
    boolean extend = false;
    EVT vt = fp.getValueType(0);
    ConstantFP c = fp.getConstantFPValue();
    if (!useCP) {
      Util.assertion(vt.equals(new EVT(MVT.f64)) || vt.equals(new EVT(MVT.f32)), "Invalid type expansion!");

      return dag.getConstant(c.getValueAPF().bitcastToAPInt(),
          vt.equals(new EVT(MVT.f64)) ? new EVT(MVT.i64) :
              new EVT(MVT.i32), false);
    }
    EVT origVT = vt;
    EVT svt = vt;
    while (svt.getSimpleVT().simpleVT != MVT.f32) {
      svt = new EVT(svt.getSimpleVT().simpleVT - 1);
      if (fp.isValueValidForType(svt, fp.getValueAPF()) &&
          tli.isLoadExtLegal(EXTLOAD, svt) &&
          tli.shouldShrinkFPConstant(origVT)) {
        Type sty = svt.getTypeForEVT();
        Constant cst = ConstantExpr.getFPTrunc(c, sty);
        c = cst instanceof ConstantFP ? (ConstantFP) cst : null;

        vt = svt;
        extend = true;
      }
    }

    SDValue cpIdx = dag.getConstantPool(c, new EVT(tli.getPointerTy()), 0, 0,
        false, 0);
    int alignment = ((ConstantPoolSDNode) cpIdx.getNode()).getAlign();
    if (extend) {
      return dag.getExtLoad(EXTLOAD, origVT, dag.getEntryNode(),
          cpIdx, PseudoSourceValue.getConstantPool(),
          0, vt, false, alignment);
    }
    return dag.getLoad(origVT, dag.getEntryNode(), cpIdx,
        PseudoSourceValue.getConstantPool(), 0, false, alignment);
  }

  private SDValue expandFPLibCall(SDNode node, RTLIB callF32, RTLIB callF64,
                                  RTLIB callF80, RTLIB callPPCF128) {
    RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
    switch (node.getValueType(0).getSimpleVT().simpleVT) {
      case MVT.f32:
        lc = callF32;
        break;
      case MVT.f64:
        lc = callF64;
        break;
      case MVT.f80:
        lc = callF80;
        break;
      case MVT.ppcf128:
        lc = callPPCF128;
        break;
      default:
        Util.shouldNotReachHere("Unexpected request for libcall!");
        ;
    }
    return expandLibCall(lc, node, false);
  }

  private SDValue expandIntLibCall(SDNode node, boolean isSigned,
                                   RTLIB callI16, RTLIB callI32,
                                   RTLIB callI64, RTLIB callI128) {
    RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
    switch (node.getValueType(0).getSimpleVT().simpleVT) {
      case MVT.i16:
        lc = callI16;
        break;
      case MVT.i32:
        lc = callI32;
        break;
      case MVT.i64:
        lc = callI64;
        break;
      case MVT.i128:
        lc = callI128;
        break;
      default:
        Util.shouldNotReachHere("Unexpected request for libcall!");
        ;
    }
    return expandLibCall(lc, node, isSigned);
  }

  private SDValue expandLibCall(RTLIB lc, SDNode node, boolean isSigned) {
    Util.assertion(isLegalizingCall, "Can't overlap legalization of calle!");
    SDValue inChain = dag.getEntryNode();

    ArrayList<ArgListEntry> args = new ArrayList<>();
    for (int i = 0, e = node.getNumOperands(); i < e; i++) {
      ArgListEntry entry = new ArgListEntry();
      EVT argVT = node.getOperand(i).getValueType();
      Type argTy = argVT.getTypeForEVT();
      entry.node = node.getOperand(i);
      entry.ty = argTy;
      entry.isSExt = isSigned;
      entry.isZExt = !isSigned;
      args.add(entry);
    }

    SDValue callee = dag.getExternalSymbol(tli.getLibCallName(lc),
        new EVT(tli.getPointerTy()));
    Type retTy = node.getValueType(0).getTypeForEVT();
    Pair<SDValue, SDValue> callInfo = tli.lowerCallTo(inChain, retTy,
        isSigned, !isSigned, false, false,
        0, tli.getLibCallCallingConv(lc),
        false, true, callee, args, dag);

    // Legalize the call sequence, starting with the chain.  This will advance
    // the LastCALLSEQ_END to the legalized version of the CALLSEQ_END node that
    // was added by LowerCallTo (guaranteeing proper serialization of calls).
    legalizeOp(callInfo.second);
    return callInfo.first;
  }

  private void promoteNode(SDNode node, ArrayList<SDValue> results) {
    EVT ovt = node.getValueType(0);
    int opc = node.getOpcode();
    if (opc == ISD.UINT_TO_FP ||
        opc == ISD.SINT_TO_FP ||
        opc == ISD.SETCC) {
      ovt = node.getOperand(0).getValueType();
    }
    EVT nvt = tli.getTypeToPromoteType(opc, ovt);
    SDValue temp1 = new SDValue(), temp2 = new SDValue(), temp3;
    switch (opc) {
      case ISD.CTTZ:
      case ISD.CTLZ:
      case ISD.CTPOP: {
        temp1 = dag.getNode(ISD.ZERO_EXTEND, nvt, node.getOperand(0));
        temp1 = dag.getNode(opc, nvt, temp1);
        if (opc == ISD.CTTZ) {
          temp2 = dag.getSetCC(new EVT(tli.getSetCCResultType(nvt)),
              temp1, dag.getConstant(nvt.getSizeInBits(), nvt, false),
              CondCode.SETEQ);
          temp1 = dag.getNode(ISD.SELECT, nvt, temp2,
              dag.getConstant(ovt.getSizeInBits(), nvt, false),
              temp1);
        } else if (opc == ISD.CTLZ) {
          temp1 = dag.getNode(ISD.SUB, nvt, temp1,
              dag.getConstant(nvt.getSizeInBits() -
                  ovt.getSizeInBits(), nvt, false));
        }
        results.add(dag.getNode(ISD.TRUNCATE, ovt, temp1));
        break;
      }
      case ISD.BSWAP: {
        int diffBits = nvt.getSizeInBits() - ovt.getSizeInBits();
        temp1 = dag.getNode(ISD.ZERO_EXTEND, nvt, temp1);
        temp1 = dag.getNode(ISD.BSWAP, nvt, temp1);
        temp1 = dag.getNode(ISD.SRL, nvt, temp1,
            dag.getConstant(diffBits, new EVT(tli.getShiftAmountTy()), false));
        results.add(temp1);
        break;
      }
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
        temp1 = promoteLegalFPToInt(node.getOperand(0),
            node.getValueType(0),
            opc == ISD.FP_TO_SINT);
        results.add(temp1);
        break;
      case ISD.UINT_TO_FP:
      case ISD.SINT_TO_FP:
        temp1 = promoteLegalIntToFP(node.getOperand(0),
            node.getValueType(0), opc == ISD.SINT_TO_FP);
        results.add(temp1);
        break;
      case ISD.AND:
      case ISD.OR:
      case ISD.XOR: {
        int extOp = ISD.UNDEF, truncOp = ISD.UNDEF;
        if (ovt.isVector()) {
          extOp = ISD.BIT_CONVERT;
          truncOp = ISD.BIT_CONVERT;
        } else if (ovt.isInteger()) {
          extOp = ISD.ANY_EXTEND;
          truncOp = ISD.TRUNCATE;
        } else {
          Util.shouldNotReachHere("Unknown promote logic operation");
        }
        temp1 = dag.getNode(extOp, nvt, node.getOperand(0));
        temp2 = dag.getNode(extOp, nvt, node.getOperand(1));
        temp1 = dag.getNode(opc, nvt, temp1, temp2);
        results.add(dag.getNode(truncOp, ovt, temp1));
        break;
      }
      case ISD.SELECT: {
        EVT resVT = node.getValueType(0);
        int extOp = ISD.UNDEF, truncOp = ISD.UNDEF;
        if (resVT.isVector()) {
          extOp = ISD.BIT_CONVERT;
          truncOp = ISD.BIT_CONVERT;
        } else if (resVT.isInteger()) {
          extOp = ISD.ANY_EXTEND;
          truncOp = ISD.TRUNCATE;
        } else {
          extOp = ISD.FP_EXTEND;
          truncOp = ISD.FP_ROUND;
        }
        temp1 = node.getOperand(0);
        temp2 = dag.getNode(extOp, nvt, node.getOperand(1));
        temp3 = dag.getNode(extOp, nvt, node.getOperand(2));
        temp1 = dag.getNode(ISD.SELECT, nvt, temp1, temp2, temp3);
        if (truncOp != ISD.FP_ROUND)
          temp1 = dag.getNode(truncOp, node.getValueType(0), temp1);
        else
          temp1 = dag.getNode(truncOp, node.getValueType(0), temp1,
              dag.getIntPtrConstant(0));

        results.add(temp1);
        break;
      }
      case ISD.VECTOR_SHUFFLE: {
        TIntArrayList mask = new TIntArrayList();
        ((ShuffleVectorSDNode) node).getMask(mask);

        temp1 = dag.getNode(ISD.BIT_CONVERT, nvt, node.getOperand(0));
        temp2 = dag.getNode(ISD.BIT_CONVERT, nvt, node.getOperand(1));

        temp1 = shuffleWithNarrowerEltType(nvt, ovt, temp1, temp2, mask.toArray());
        temp1 = dag.getNode(ISD.BIT_CONVERT, ovt, temp1);
        results.add(temp1);
        break;
      }
      case ISD.SETCC: {
        int extOp = ISD.FP_EXTEND;
        if (nvt.isInteger()) {
          CondCode cc = ((CondCodeSDNode) node.getOperand(2).getNode()).getCondition();
          extOp = cc.isSignedIntSetCC() ? ISD.SIGN_EXTEND : ISD.ZERO_EXTEND;
          ;
        }
        temp1 = dag.getNode(extOp, nvt, node.getOperand(0));
        temp2 = dag.getNode(extOp, nvt, node.getOperand(1));
        results.add(dag.getNode(ISD.SETCC, node.getValueType(0),
            temp1, temp2, node.getOperand(2)));
        break;
      }
    }
  }
}
