/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
import backend.target.TargetLowering;
import backend.type.Type;
import backend.value.ConstantInt;
import backend.value.Value;
import gnu.trove.list.array.TIntArrayList;
import tools.APFloat;
import tools.APInt;
import tools.Pair;
import tools.Util;

import java.util.*;
import java.util.stream.Collectors;

import static backend.codegen.dagisel.CondCode.*;
import static backend.codegen.dagisel.DAGTypeLegalizer.NodeIdFlags.*;
import static backend.target.TargetLowering.BooleanContent.ZeroOrNegativeOneBooleanContent;
import static backend.target.TargetLowering.LegalizeAction.Custom;

/**
 * This takes an arbitrary SelectionDAG as input and transform on it until
 * only value types the target machine can handle are left. This involves
 * promoting small sizes to large one or splitting large into multiple small
 * values.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class DAGTypeLegalizer {
  private TargetLowering tli;
  private SelectionDAG dag;

  public interface NodeIdFlags {
    int ReadyToProcess = 0;
    int NewNode = -1;
    int Unanalyzed = -2;
    int Processed = -3;
  }

  private enum LegalizeAction {
    Legal,
    PromotedInteger,
    ExpandInteger,
    SoftenFloat,
    ExpandFloat,
    ScalarizeVector,
    SplitVector,
    WidenVector
  }

  private ValueTypeAction valueTypeActions;

  private HashMap<SDValue, SDValue> promotedIntegers;

  private HashMap<SDValue, Pair<SDValue, SDValue>> expandedIntegers;

  private HashMap<SDValue, SDValue> softenedFloats;

  private HashMap<SDValue, Pair<SDValue, SDValue>> expandedFloats;

  private HashMap<SDValue, SDValue> scalarizedVectors;

  private HashMap<SDValue, Pair<SDValue, SDValue>> splitVectors;

  private HashMap<SDValue, SDValue> widenedVectors;

  private HashMap<SDValue, SDValue> replacedValues;

  private LinkedList<SDNode> worklist;

  public DAGTypeLegalizer(SelectionDAG dag) {
    tli = dag.getTargetLoweringInfo();
    this.dag = dag;
    valueTypeActions = tli.getValueTypeActions();
    promotedIntegers = new HashMap<>();
    expandedIntegers = new HashMap<>();
    expandedFloats = new HashMap<>();
    softenedFloats = new HashMap<>();
    scalarizedVectors = new HashMap<>();
    splitVectors = new HashMap<>();
    widenedVectors = new HashMap<>();
    replacedValues = new HashMap<>();
    worklist = new LinkedList<>();
  }

  private LegalizeAction getTypeAction(EVT vt) {
    switch (valueTypeActions.getTypeAction(dag.getContext(), vt)) {
      default:
        Util.assertion("Unknown legalize action!");
        return null;
      case Legal:
        return LegalizeAction.Legal;
      case Promote:
        // promote can means two different situations:
        // 1. promote a small value to large one, like i8 -> i32.
        // 2. widen short vector value, such as v3i32 -> v4i32.
        return !vt.isVector() ? LegalizeAction.PromotedInteger :
            LegalizeAction.WidenVector;
      case Expand:
        // Expand can means:
        // 1. split scalar in half
        // 2. convert a float to an integer.
        // 3. scalarize a single-element vector
        // 4. split a vector in two elements.
        if (!vt.isVector()) {
          if (vt.isInteger())
            return LegalizeAction.ExpandInteger;
          else if (vt.getSizeInBits() ==
              tli.getTypeToTransformTo(dag.getContext(), vt).getSizeInBits())
            return LegalizeAction.SoftenFloat;
          else
            return LegalizeAction.ExpandFloat;
        } else if (vt.getVectorNumElements() == 1)
          return LegalizeAction.ScalarizeVector;
        else
          return LegalizeAction.SplitVector;
    }
  }

  private boolean isTypeLegal(EVT vt) {
    return valueTypeActions.getTypeAction(dag.getContext(), vt) ==
        TargetLowering.LegalizeAction.Legal;
  }

  private boolean ignoreNodeResults(SDNode n) {
    return n.getOpcode() == ISD.TargetConstant;
  }

  public boolean run() {
    boolean changed = false;
    HandleSDNode dummy = new HandleSDNode(dag.getRoot());
    dummy.setNodeID(Unanalyzed);

    dag.setRoot(new SDValue());

    for (SDNode node : dag.allNodes) {
      if (node.getNumOperands() == 0) {
        node.setNodeID(ReadyToProcess);
        worklist.push(node);
      } else
        node.setNodeID(Unanalyzed);
    }
    if (Util.DEBUG) {
      for (SDNode n : dag.allNodes) {
        n.dump();
        System.err.printf(" %d%n", n.getNodeID());
      }
    }
    while (!worklist.isEmpty()) {
      if (Util.DEBUG)
        performExpensiveChecks();

      SDNode n = worklist.pop();
      Util.assertion(n.getNodeID() == ReadyToProcess,
          "Node should be ready if on worklist!");
      nodeDone:
      {
        if (!ignoreNodeResults(n)) {
          for (int i = 0, numResults = n.getNumValues(); i < numResults; i++) {
            EVT resultVT = n.getValueType(i);
            LegalizeAction action = getTypeAction(resultVT);
            switch (action) {
              default:
                Util.assertion("Unknown action '%s'!".format(action.name()));
                break;
              case Legal:
                break;
              case PromotedInteger:
                promotedIntegerResult(n, i);
                changed = true;
                break nodeDone;
              case ExpandInteger:
                expandIntegerResult(n, i);
                break nodeDone;
              case SoftenFloat:
                softenFloatResult(n, i);
                break nodeDone;
              case ExpandFloat:
                expandFloatResult(n, i);
                break nodeDone;
              case ScalarizeVector:
                scalarizeVectorResult(n, i);
                break nodeDone;
              case SplitVector:
                splitVectorResult(n, i);
                break nodeDone;
              case WidenVector:
                widenVectorResult(n, i);
                break nodeDone;
            }
          }
        }

        int numOperands = n.getNumOperands();
        boolean needsReanalyzing = false;
        int i = 0;
        for (; i < numOperands; i++) {
          if (ignoreNodeResults(n.getOperand(i).getNode()))
            continue;

          EVT opVT = n.getOperand(i).getValueType();
          switch (getTypeAction(opVT)) {
            default:
              Util.assertion(false, "Unknown action!");
            case Legal:
              continue;
            case PromotedInteger:
              needsReanalyzing = promoteIntegerOperand(n, i);
              changed = true;
              break;
            case ExpandInteger:
              needsReanalyzing = expandIntegerOperand(n, i);
              changed = true;
              break;
            case SoftenFloat:
              needsReanalyzing = softenFloatOperand(n, i);
              changed = true;
              break;
            case ExpandFloat:
              needsReanalyzing = expandFloatOperand(n, i);
              changed = true;
              break;
            case ScalarizeVector:
              needsReanalyzing = scalarizeVectorOperand(n, i);
              changed = true;
              break;
            case SplitVector:
              needsReanalyzing = splitVectorOperand(n, i);
              changed = true;
              break;
            case WidenVector:
              needsReanalyzing = widenVectorOperand(n, i);
              changed = true;
              break;
          }
          break;
        }

        if (needsReanalyzing) {
          Util.assertion(n.getNodeID() == ReadyToProcess, "Node ID recaculated?");
          n.setNodeID(NewNode);

          SDNode nn = analyzeNewNode(n);
          if (nn.equals(n)) {
            continue;
          }

          Util.assertion(n.getNumValues() == nn.getNumValues(), "Node morphing changed the number of results!");

          for (int j = 0, e = n.getNumValues(); j < e; j++) {
            replaceValueWithHelper(new SDValue(n, i), new SDValue(nn, i));
          }
          Util.assertion(n.getNodeID() == NewNode, "Unexpected node state!");
          continue;
        }

        if (i == numOperands) {
          if (Util.DEBUG) {
            System.err.print("Legally typed node: ");
            n.dump(dag);
            System.err.println();
          }
        }
      }

      Util.assertion(n.getNodeID() == ReadyToProcess, "Node ID recaculated?");
      n.setNodeID(Processed);
      for (SDUse use : n.useList) {
        SDNode user = use.getUser();
        if (user.isDeleted())
          continue;

        int nodeId = user.getNodeID();
        if (nodeId > 0) {
          user.setNodeID(nodeId - 1);
          if (nodeId - 1 == ReadyToProcess)
            worklist.push(user);

          continue;
        }

        if (nodeId == NewNode)
          continue;

        Util.assertion(nodeId == Unanalyzed, String.format("Unknown node ID in function '%s'!",
            dag.getMachineFunction().getFunction().getName()));
        user.setNodeID(user.getNumOperands() - 1);

        if (user.getNumOperands() == 1)
          worklist.push(user);
      }
    }
    if (Util.DEBUG)
      performExpensiveChecks();
    dag.setRoot(dummy.getValue());
    dummy.dropOperands();
    dag.removeDeadNodes();
    return changed;
  }

  public void nodeDeletion(SDNode oldOne, SDNode newOne) {
    expungeNode(oldOne);
    expungeNode(newOne);
    for (int i = 0, e = oldOne.getNumValues(); i < e; i++)
      replacedValues.put(new SDValue(oldOne, i), new SDValue(newOne, i));
  }

  private SDNode analyzeNewNode(SDNode n) {
    if (n.getNodeID() != NewNode && n.getNodeID() != Unanalyzed)
      return n;

    expungeNode(n);

    ArrayList<SDValue> newOps = new ArrayList<>();
    int numProcessed = 0;
    for (int i = 0, e = n.getNumOperands(); i < e; i++) {
      SDValue origOp = n.getOperand(i);
      SDValue op = origOp.clone();

      op = analyzeNewValue(op);
      if (op.getNode().getNodeID() == Processed)
        ++numProcessed;

      if (!newOps.isEmpty())
        newOps.add(op);
      else if (!op.equals(origOp)) {
        for (int j = 0; j < i; j++)
          newOps.add(n.getOperand(j));
        newOps.add(op);
      }
    }
    if (!newOps.isEmpty()) {
      SDNode nn = dag.updateNodeOperands(new SDValue(n, 0),
          newOps).getNode();
      if (!nn.equals(n)) {
        n.setNodeID(NewNode);
        if (nn.getNodeID() != NewNode && nn.getNodeID() != Unanalyzed) {
          return nn;
        }

        n = nn;
        expungeNode(n);
      }
    }

    n.setNodeID(n.getNumOperands() - numProcessed);
    if (n.getNodeID() == ReadyToProcess)
      worklist.push(n);

    return n;
  }

  private void expungeNode(SDNode n) {
    if (n.getNodeID() != NewNode)
      return;


    int i = 0, e = n.getNumValues();
    for (; i < e; i++) {
      if (replacedValues.containsKey(new SDValue(n, i)))
        break;
    }
    if (i == e)
      return;
    for (Map.Entry<SDValue, SDValue> pair : promotedIntegers.entrySet()) {
      Util.assertion(!pair.getKey().getNode().equals(n));
      pair.setValue(remapValue(pair.getValue()));
    }

    for (Map.Entry<SDValue, SDValue> pair : softenedFloats.entrySet()) {
      Util.assertion(!pair.getKey().getNode().equals(n));
      pair.setValue(remapValue(pair.getValue()));
    }

    for (Map.Entry<SDValue, SDValue> pair : scalarizedVectors.entrySet()) {
      Util.assertion(!pair.getKey().getNode().equals(n));
      pair.setValue(remapValue(pair.getValue()));
    }

    for (Map.Entry<SDValue, SDValue> pair : widenedVectors.entrySet()) {
      Util.assertion(!pair.getKey().getNode().equals(n));
      pair.setValue(remapValue(pair.getValue()));
    }

    for (Map.Entry<SDValue, Pair<SDValue, SDValue>> pair : expandedIntegers.entrySet()) {
      Util.assertion(!pair.getKey().getNode().equals(n));
      pair.getValue().first = remapValue(pair.getValue().first);
      pair.getValue().second = remapValue(pair.getValue().second);
    }

    for (Map.Entry<SDValue, Pair<SDValue, SDValue>> pair : expandedFloats.entrySet()) {
      Util.assertion(!pair.getKey().getNode().equals(n));
      pair.getValue().first = remapValue(pair.getValue().first);
      pair.getValue().second = remapValue(pair.getValue().second);
    }

    for (Map.Entry<SDValue, Pair<SDValue, SDValue>> pair : splitVectors.entrySet()) {
      Util.assertion(!pair.getKey().getNode().equals(n));
      pair.getValue().first = remapValue(pair.getValue().first);
      pair.getValue().second = remapValue(pair.getValue().second);
    }

    for (Map.Entry<SDValue, SDValue> pair : replacedValues.entrySet()) {
      pair.setValue(remapValue(pair.getValue()));
    }
    for (int j = 0, sz = n.getNumValues(); j < sz; j++)
      replacedValues.remove(new SDValue(n, j));
  }

  private SDValue analyzeNewValue(SDValue val) {
    val.setNode(analyzeNewNode(val.getNode()));
    if (val.getNode().getNodeID() == Processed)
      val = remapValue(val);
    return val;
  }

  private void performExpensiveChecks() {
    // TODO: 18-5-17
  }

  private SDValue remapValue(SDValue val) {
    if (replacedValues.containsKey(val)) {
      val = replacedValues.get(val);
      val = remapValue(val);
      Util.assertion(val.getNode().getNodeID() != NewNode, "Mapped to new node!");
    }
    return val;
  }

  private SDValue bitConvertToInteger(SDValue op) {
    int bitWidth = op.getValueType().getSizeInBits();
    return dag.getNode(ISD.BIT_CONVERT, EVT.getIntegerVT(dag.getContext(), bitWidth), op);
  }

  private SDValue bitConvertVectorToIntegerVector(SDValue op) {
    Util.assertion(op.getValueType().isVector(), "Only applies to vectors!");
    int eltWidth = op.getValueType().getVectorElementType().getSizeInBits();
    EVT eltVT = EVT.getIntegerVT(dag.getContext(), eltWidth);
    int numElts = op.getValueType().getVectorNumElements();
    return dag.getNode(ISD.BIT_CONVERT, EVT.getVectorVT(dag.getContext(), eltVT, numElts),
        op);
  }

  private SDValue createStackStoreLoad(SDValue op, EVT destVT) {
    SDValue stackPtr = dag.createStackTemporary(op.getValueType(), destVT);
    SDValue store = dag.getStore(dag.getEntryNode(), op, stackPtr, null, 0, false, 0);
    return dag.getLoad(destVT, store, stackPtr, null, 0);
  }

  private boolean customLowerNode(SDNode n, EVT vt, boolean legalizeResult) {
    if (tli.getOperationAction(n.getOpcode(), vt) != Custom) {
      return false;
    }

    ArrayList<SDValue> results = new ArrayList<>();
    if (legalizeResult)
      tli.replaceNodeResults(n, results, dag);
    else
      tli.lowerOperationWrapper(n, results, dag);

    if (results.isEmpty())
      return false;

    Util.assertion(results.size() == n.getNumValues());
    for (int i = 0, e = results.size(); i < e; i++)
      replaceValueWith(new SDValue(n, i), results.get(i));
    return true;
  }

  private SDValue getVectorElementPointer(SDValue vecPtr, EVT eltVT, SDValue index) {
    if (index.getValueType().bitsGT(new EVT(tli.getPointerTy())))
      index = dag.getNode(ISD.TRUNCATE, new EVT(tli.getPointerTy()), index);
    else
      index = dag.getNode(ISD.ZERO_EXTEND, new EVT(tli.getPointerTy()), index);
    int eltSize = eltVT.getSizeInBits() / 8;
    index = dag.getNode(ISD.MUL, index.getValueType(), index,
        dag.getConstant(eltSize, index.getValueType(), false));
    return dag.getNode(ISD.ADD, index.getValueType(), index, vecPtr);
  }

  private SDValue joinIntegers(SDValue lo, SDValue hi) {
    EVT loVT = lo.getValueType(), hiVT = hi.getValueType();
    EVT nvt = EVT.getIntegerVT(dag.getContext(), loVT.getSizeInBits() + hiVT.getSizeInBits());
    lo = dag.getNode(ISD.ZERO_EXTEND, nvt, lo);
    hi = dag.getNode(ISD.ANY_EXTEND, nvt, hi);
    hi = dag.getNode(ISD.SHL, nvt, hi, dag.getConstant(loVT.getSizeInBits(),
        new EVT(tli.getPointerTy()), false));
    return dag.getNode(ISD.OR, nvt, lo, hi);
  }

  private SDValue promoteTargetBoolean(SDValue boolVal, EVT vt) {
    int extendCode = 0;
    switch (tli.getBooleanContents()) {
      default:
        Util.assertion(false, "Unknown booleanConstant");
        break;
      case UndefinedBooleanContent:
        extendCode = ISD.ANY_EXTEND;
        break;
      case ZeroOrOneBooleanContent:
        extendCode = ISD.ZERO_EXTEND;
        break;
      case ZeroOrNegativeOneBooleanContent:
        extendCode = ISD.SIGN_EXTEND;
        break;
    }
    return dag.getNode(extendCode, vt, boolVal);
  }

  private void replaceValueWith(SDValue from, SDValue to) {
    expungeNode(from.getNode());
    to = analyzeNewValue(to);

    replacedValues.put(from, to);
    replaceValueWithHelper(from, to);
  }

  private void replaceValueWithHelper(SDValue from, SDValue to) {
    Util.assertion(!from.getNode().equals(to.getNode()), "Potential legalization loop!");

    to = analyzeNewValue(to);

    LinkedList<SDNode> nodesToAnalyze = new LinkedList<>();
    NodeUpdateListener nul = new NodeUpdateListener(this, nodesToAnalyze);
    dag.replaceAllUsesOfValueWith(from, to, nul);

    while (!nodesToAnalyze.isEmpty()) {
      SDNode n = nodesToAnalyze.pop();
      if (n.getNodeID() != NewNode)
        continue;

      SDNode nn = analyzeNewNode(n);
      if (!nn.equals(n)) {
        Util.assertion(nn.getNodeID() != NewNode, "Analysis resulted in newNode!");
        Util.assertion(n.getNumValues() == nn.getNumValues(), "Node morphing changed the number of results!");
        for (int i = 0, e = n.getNumValues(); i < e; i++) {
          SDValue oldVal = new SDValue(n, i);
          SDValue newVal = new SDValue(nn, i);
          if (nn.getNodeID() == Processed)
            remapValue(newVal);
          dag.replaceAllUsesOfValueWith(oldVal, newVal, nul);
        }
      }
    }
  }

  /**
   * Returned array represents the low value and high value respectively.
   *
   * @param op
   * @return
   */
  private SDValue[] splitInteger(SDValue op) {
    EVT halfVT = EVT.getIntegerVT(dag.getContext(), op.getValueType().getSizeInBits() / 2);
    return splitInteger(op, halfVT, halfVT);
  }

  /**
   * Returned array represents the low value and high value respectively.
   *
   * @param op
   * @param loVT
   * @param hiVT
   * @return
   */
  private SDValue[] splitInteger(SDValue op, EVT loVT, EVT hiVT) {
    Util.assertion(loVT.getSizeInBits() + hiVT.getSizeInBits() == op.getValueType().getSizeInBits());

    SDValue[] res = new SDValue[2];
    res[0] = dag.getNode(ISD.TRUNCATE, loVT, op);
    res[1] = dag.getNode(ISD.SRL, op.getValueType(), op,
        dag.getConstant(loVT.getSizeInBits(), new EVT(tli.getPointerTy()),
            false));
    res[1] = dag.getNode(ISD.TRUNCATE, hiVT, res[1]);
    return res;
  }

  private SDValue getPromotedInteger(SDValue op) {
    SDValue promotedOp = promotedIntegers.get(op);
    promotedOp = remapValue(promotedOp);
    Util.assertion(promotedOp != null && promotedOp.getNode() != null);
    promotedIntegers.put(op, promotedOp);
    return promotedOp;
  }

  private void setPromotedIntegers(SDValue op, SDValue result) {
    result = analyzeNewValue(result);
    Util.assertion(!promotedIntegers.containsKey(op), "Node is already promoted!");
    promotedIntegers.put(op, result);
  }

  private SDValue sextPromotedInteger(SDValue op) {
    EVT oldVT = op.getValueType();
    op = getPromotedInteger(op);
    return dag.getNode(ISD.SIGN_EXTEND_INREG,
        op.getValueType(), op, dag.getValueType(oldVT));
  }

  private SDValue zextPromotedInteger(SDValue op) {
    EVT oldVT = op.getValueType();
    op = getPromotedInteger(op);
    return dag.getZeroExtendInReg(op, oldVT);
  }

  private void promotedIntegerResult(SDNode n, int resNo) {
    if (Util.DEBUG) {
      System.err.print("Promote integer result:");
      n.dump(dag);
      System.err.println();
    }
    SDValue res = new SDValue();
    if (customLowerNode(n, n.getValueType(resNo), true))
      return;

    switch (n.getOpcode()) {
      default:
        if (Util.DEBUG) {
          System.err.printf("promoteIntegerResult #%d: ", resNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to promote operator!");
        break;
      case ISD.AssertSext:
        res = promoteIntResAssertSext(n);
        break;
      case ISD.AssertZext:
        res = promoteIntResAssertZext(n);
        break;
      case ISD.BIT_CONVERT:
        res = promoteIntResBitConvert(n);
        break;
      case ISD.BSWAP:
        res = promoteIntResBSWAP(n);
        break;
      case ISD.BUILD_PAIR:
        res = promoteIntResBuildPair(n);
        break;
      case ISD.Constant:
        res = promoteIntResConstant(n);
        break;
      case ISD.CONVERT_RNDSAT:
        res = promoteIntResConvertRndsat(n);
        break;
      case ISD.CTLZ:
        res = promoteIntResCTLZ(n);
        break;
      case ISD.CTPOP:
        res = promoteIntResCTPOP(n);
        break;
      case ISD.CTTZ:
        res = promoteIntResCTTZ(n);
        break;
      case ISD.EXTRACT_VECTOR_ELT:
        res = promoteIntResExtractVectorElt(n);
        break;
      case ISD.LOAD:
        res = promoteIntResLoad((LoadSDNode) n);
        break;
      case ISD.SELECT:
        res = promoteIntResSELECT(n);
        break;
      case ISD.SELECT_CC:
        res = promoteIntResSELECTCC(n);
        break;
      case ISD.SETCC:
        res = promoteIntResSETCC(n);
        break;
      case ISD.SHL:
        res = promoteIntResSHL(n);
        break;
      case ISD.SIGN_EXTEND_INREG:
        res = promoteIntResSignExtendInreg(n);
        break;
      case ISD.SRA:
        res = promoteIntResSRA(n);
        break;
      case ISD.SRL:
        res = promoteIntResSRL(n);
        break;
      case ISD.TRUNCATE:
        res = promoteIntResTruncate(n);
        break;
      case ISD.UNDEF:
        res = promoteIntResUNDEF(n);
        break;
      case ISD.VAARG:
        res = promoteIntResVAARG(n);
        break;
      case ISD.SIGN_EXTEND:
      case ISD.ZERO_EXTEND:
      case ISD.ANY_EXTEND:
        res = promoteIntResIntExtend(n);
        break;
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
        res = promoteIntResFPToXInt(n);
        break;
      case ISD.AND:
      case ISD.OR:
      case ISD.XOR:
      case ISD.ADD:
      case ISD.SUB:
      case ISD.MUL:
        res = promoteIntResSimpleIntBinOp(n);
        break;
      case ISD.SDIV:
      case ISD.SREM:
        res = promoteIntResSDIV(n);
        break;
      case ISD.UDIV:
      case ISD.UREM:
        res = promoteIntResUDIV(n);
        break;
      case ISD.SADDO:
      case ISD.SSUBO:
        res = promoteIntResSADDSUBO(n, resNo);
        break;
      case ISD.UADDO:
      case ISD.USUBO:
        res = promoteIntResUADDSUBO(n, resNo);
        break;
      case ISD.SMULO:
      case ISD.UMULO:
        res = promoteIntResXMULO(n, resNo);
        break;
      case ISD.ATOMIC_LOAD_ADD:
      case ISD.ATOMIC_LOAD_SUB:
      case ISD.ATOMIC_LOAD_AND:
      case ISD.ATOMIC_LOAD_OR:
      case ISD.ATOMIC_LOAD_XOR:
      case ISD.ATOMIC_LOAD_NAND:
      case ISD.ATOMIC_LOAD_MIN:
      case ISD.ATOMIC_LOAD_MAX:
      case ISD.ATOMIC_LOAD_UMIN:
      case ISD.ATOMIC_LOAD_UMAX:
      case ISD.ATOMIC_SWAP:
        res = promoteIntResAtomic1((AtomicSDNode) n);
        break;
      case ISD.ATOMIC_CMP_SWAP:
        res = promoteIntResAtomic2((AtomicSDNode) n);
        break;
    }
    if (res.getNode() != null)
      setPromotedIntegers(new SDValue(n, resNo), res);
  }

  private SDValue promoteIntResAssertSext(SDNode n) {
    SDValue op = sextPromotedInteger(n.getOperand(0));
    return dag.getNode(ISD.AssertSext, op.getValueType(), op, n.getOperand(1));
  }

  private SDValue promoteIntResAssertZext(SDNode n) {
    SDValue op = sextPromotedInteger(n.getOperand(0));
    return dag.getNode(ISD.AssertZext, op.getValueType(), op, n.getOperand(1));
  }

  private SDValue promoteIntResAtomic1(AtomicSDNode n) {
    SDValue op2 = sextPromotedInteger(n.getOperand(2));
    SDValue res = dag.getAtomic(n.getOpcode(),
        n.getMemoryVT(),
        n.getChain(),
        n.getBasePtr(),
        op2, n.getSrcValue(),
        n.getAlignment());

    replaceValueWith(new SDValue(n, 1), res.getValue(1));
    return res;
  }

  private SDValue promoteIntResAtomic2(AtomicSDNode n) {
    SDValue op2 = getPromotedInteger(n.getOperand(2));
    SDValue op3 = getPromotedInteger(n.getOperand(3));
    SDValue res = dag.getAtomic(n.getOpcode(),
        n.getMemoryVT(),
        n.getChain(),
        n.getBasePtr(),
        op2, op3, n.getSrcValue(), n.getAlignment());
    replaceValueWith(new SDValue(n, 1), res.getValue(1));
    return res;
  }

  private SDValue promoteIntResBitConvert(SDNode n) {
    SDValue inOp = n.getOperand(0);
    EVT inVT = inOp.getValueType();
    EVT ninVT = tli.getTypeToTransformTo(dag.getContext(), inVT);
    EVT outVT = n.getValueType(0);
    EVT noutVT = tli.getTypeToTransformTo(dag.getContext(), outVT);
    switch (getTypeAction(inVT)) {
      default:
        Util.assertion("Unknown type action!");
        break;
      case Legal:
      case ExpandInteger:
      case ExpandFloat:
        break;
      case PromotedInteger:
        if (noutVT.bitsEq(ninVT))
          return dag.getNode(ISD.BIT_CONVERT, noutVT,
              getPromotedInteger(inOp));
        break;
      case SoftenFloat:
        return dag.getNode(ISD.ANY_EXTEND, noutVT, getSoftenedFloat(inOp));
      case ScalarizeVector:
        return dag.getNode(ISD.ANY_EXTEND, noutVT,
            bitConvertToInteger(getScalarizedVector(inOp)));
      case SplitVector: {
        SDValue[] res = getSplitVector(n.getOperand(0));
        SDValue lo = bitConvertToInteger(res[0]);
        SDValue hi = bitConvertToInteger(res[1]);
        if (tli.isBigEndian()) {
          SDValue temp = lo;
          lo = hi;
          hi = temp;
        }
        inOp = dag.getNode(ISD.ANY_EXTEND,
            EVT.getIntegerVT(dag.getContext(), noutVT.getSizeInBits()),
            joinIntegers(lo, hi));
        return dag.getNode(ISD.BIT_CONVERT, noutVT, inOp);
      }
      case WidenVector:
        if (outVT.bitsEq(ninVT))
          return dag.getNode(ISD.BIT_CONVERT, outVT,
              getWidenedVector(inOp));
        break;
    }
    return dag.getNode(ISD.ANY_EXTEND, noutVT,
        createStackStoreLoad(inOp, outVT));
  }

  private SDValue promoteIntResBSWAP(SDNode n) {
    SDValue op = getPromotedInteger(n.getOperand(0));
    EVT outVT = n.getValueType(0);
    EVT opVT = op.getValueType();
    int diffBits = opVT.getSizeInBits() - outVT.getSizeInBits();
    return dag.getNode(ISD.SRL, opVT, dag.getNode(ISD.BSWAP, opVT, op),
        dag.getConstant(diffBits, new EVT(tli.getPointerTy()), false));
  }

  private SDValue promoteIntResBuildPair(SDNode n) {
    return dag.getNode(ISD.ANY_EXTEND, tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0)),
        joinIntegers(n.getOperand(0), n.getOperand(1)));
  }

  private SDValue promoteIntResConstant(SDNode n) {
    EVT vt = n.getValueType(0);
    int opc = vt.isByteSized() ? ISD.SIGN_EXTEND : ISD.ZERO_EXTEND;
    return dag.getNode(opc,
        tli.getTypeToTransformTo(dag.getContext(), vt),
        new SDValue(n, 0));
  }

  private SDValue promoteIntResConvertRndsat(SDNode n) {
    CvtCode cc = ((CvtRndSatSDNode) n).getCvtCode();
    Util.assertion(cc == CvtCode.CVT_SS || cc == CvtCode.CVT_SU || cc == CvtCode.CVT_US || cc == CvtCode.CVT_UU ||
        cc == CvtCode.CVT_SF || cc == CvtCode.CVT_UF);

    EVT outVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    return dag.getConvertRndSat(outVT,
        n.getOperand(0), n.getOperand(1),
        n.getOperand(2),
        n.getOperand(3),
        n.getOperand(4),
        cc);
  }

  private SDValue promoteIntResCTLZ(SDNode n) {
    SDValue op = zextPromotedInteger(n.getOperand(0));
    EVT outVT = n.getValueType(0);
    EVT opVT = op.getValueType();

    op = dag.getNode(ISD.CTLZ, opVT, op);
    return dag.getNode(ISD.SUB, opVT, op,
        dag.getConstant(opVT.getSizeInBits() - outVT.getSizeInBits(),
            opVT, false));
  }

  private SDValue promoteIntResCTTZ(SDNode n) {
    SDValue op = getPromotedInteger(n.getOperand(0));
    EVT opVT = op.getValueType();
    EVT outVT = n.getValueType(0);

    APInt topBit = new APInt(opVT.getSizeInBits(), 0);
    topBit.set(opVT.getSizeInBits());
    op = dag.getNode(ISD.OR, opVT, op, dag.getConstant(topBit, opVT, false));
    return dag.getNode(ISD.CTTZ, opVT, op);
  }

  private SDValue promoteIntResCTPOP(SDNode n) {
    SDValue op = zextPromotedInteger(n.getOperand(0));
    return dag.getNode(ISD.CTPOP, op.getValueType(), op);
  }

  private SDValue promoteIntResExtractVectorElt(SDNode n) {
    EVT outVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    return dag.getNode(ISD.EXTRACT_VECTOR_ELT, outVT,
        n.getOperand(0), n.getOperand(1));
  }

  private SDValue promoteIntResFPToXInt(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    int newOpc = n.getOpcode();
    if (n.getOpcode() == ISD.FP_TO_UINT &&
        !tli.isOperationLegal(ISD.FP_TO_UINT, nvt) &&
        !tli.isOperationLegal(ISD.FP_TO_SINT, nvt))
      newOpc = ISD.FP_TO_SINT;
    SDValue res = dag.getNode(newOpc, nvt, n.getOperand(0));
    return dag.getNode(n.getOpcode() == ISD.FP_TO_UINT ?
            ISD.AssertZext : ISD.AssertSext, nvt, res,
        dag.getValueType(n.getValueType(0)));
  }

  private SDValue promoteIntResIntExtend(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    if (getTypeAction(n.getOperand(0).getValueType()) == LegalizeAction.PromotedInteger) {
      SDValue res = getPromotedInteger(n.getOperand(0));
      Util.assertion(res.getValueType().bitsLE(nvt), "Extension doesn't make sense!");

      if (nvt.equals(res.getValueType())) {
        if (n.getOpcode() == ISD.SIGN_EXTEND)
          return dag.getNode(ISD.SIGN_EXTEND_INREG, nvt, res,
              dag.getValueType(n.getOperand(0).getValueType()));
        if (n.getOpcode() == ISD.ZERO_EXTEND)
          return dag.getZeroExtendInReg(res, n.getOperand(0).getValueType());
        Util.assertion(n.getOpcode() == ISD.ANY_EXTEND, "Unknown integer extension!");
        return res;
      }
    }
    return dag.getNode(n.getOpcode(), nvt, n.getOperand(0));
  }

  private SDValue promoteIntResLoad(LoadSDNode n) {
    Util.assertion(n.isUNINDEXEDLoad(), "Indexed load during type legalization!");
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    LoadExtType ext = n.isNONExtLoad() ? LoadExtType.EXTLOAD : n.getExtensionType();
    SDValue res = dag.getExtLoad(ext,
        nvt, n.getChain(), n.getBasePtr(), n.getSrcValue(),
        n.getSrcValueOffset(),
        n.getMemoryVT(), n.isVolatile(),
        n.getAlignment());
    replaceValueWith(new SDValue(n, 1), res.getValue(1));
    return res;
  }

  private SDValue promoteIntResOverflow(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(1));
    EVT[] valueVTs = {n.getValueType(0), nvt};
    SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
    SDValue res = dag.getNode(n.getOpcode(), dag.getVTList(valueVTs), ops);
    replaceValueWith(new SDValue(n, 0), res);
    return new SDValue(res.getNode(), 1);
  }

  private SDValue promoteIntResSADDSUBO(SDNode n, int resNo) {
    if (resNo == 1)
      return promoteIntResOverflow(n);

    SDValue lhs = sextPromotedInteger(n.getOperand(0));
    SDValue rhs = sextPromotedInteger(n.getOperand(1));
    EVT origVT = n.getOperand(0).getValueType();
    EVT promotedVT = lhs.getValueType();

    int opc = n.getOpcode() == ISD.SADDO ? ISD.ADD : ISD.SUB;
    SDValue res = dag.getNode(opc, promotedVT, lhs, rhs);

    SDValue off = dag.getNode(ISD.SIGN_EXTEND_INREG,
        promotedVT, res, dag.getValueType(origVT));
    off = dag.getSetCC(n.getValueType(1), off, res, CondCode.SETNE);
    replaceValueWith(new SDValue(n, 1), off);
    return res;
  }

  private SDValue promoteIntResSDIV(SDNode n) {
    SDValue lhs = sextPromotedInteger(n.getOperand(0));
    SDValue rhs = sextPromotedInteger(n.getOperand(1));
    return dag.getNode(n.getOpcode(), lhs.getValueType(),
        lhs, rhs);
  }

  private SDValue promoteIntResSELECT(SDNode n) {
    SDValue lhs = getPromotedInteger(n.getOperand(1));
    SDValue rhs = getPromotedInteger(n.getOperand(2));
    return dag.getNode(ISD.SELECT, lhs.getValueType(),
        n.getOperand(0), lhs, rhs);
  }

  private SDValue promoteIntResSELECTCC(SDNode n) {
    SDValue lhs = getPromotedInteger(n.getOperand(2));
    SDValue rhs = getPromotedInteger(n.getOperand(3));
    return dag.getNode(ISD.SELECT_CC, lhs.getValueType(),
        n.getOperand(0), n.getOperand(1), lhs, rhs,
        n.getOperand(4));
  }

  private SDValue promoteIntResSETCC(SDNode n) {
    EVT svt = new EVT(tli.getSetCCResultType(n.getOperand(0).getValueType()));
    Util.assertion(isTypeLegal(svt), "Illegal SetCC type!");
    SDValue setcc = dag.getNode(ISD.SETCC, svt, n.getOperand(0),
        n.getOperand(1), n.getOperand(2));
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    Util.assertion(nvt.bitsLE(svt), "Integer type overpromoted!");
    return dag.getNode(ISD.TRUNCATE, nvt, setcc);
  }

  private SDValue promoteIntResSHL(SDNode n) {
    return dag.getNode(ISD.SHL, tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0)),
        getPromotedInteger(n.getOperand(0)), n.getOperand(1));
  }

  private SDValue promoteIntResSimpleIntBinOp(SDNode n) {
    SDValue lhs = getPromotedInteger(n.getOperand(0));
    SDValue rhs = getPromotedInteger(n.getOperand(1));
    return dag.getNode(n.getOpcode(), lhs.getValueType(), lhs, rhs);
  }

  private SDValue promoteIntResSignExtendInreg(SDNode n) {
    SDValue op = getPromotedInteger(n.getOperand(0));
    return dag.getNode(ISD.SIGN_EXTEND_INREG, op.getValueType(),
        op, n.getOperand(1));
  }

  private SDValue promoteIntResSRA(SDNode n) {
    SDValue res = sextPromotedInteger(n.getOperand(0));
    return dag.getNode(ISD.SRA, res.getValueType(), res, n.getOperand(1));
  }

  private SDValue promoteIntResSRL(SDNode n) {
    EVT vt = n.getValueType(0);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), vt);
    SDValue res = zextPromotedInteger(n.getOperand(0));
    return dag.getNode(ISD.SRL, nvt, res, n.getOperand(1));
  }

  private SDValue promoteIntResTruncate(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue res = new SDValue();
    switch (getTypeAction(n.getOperand(0).getValueType())) {
      default:
        Util.shouldNotReachHere("Unknown type action!");
        break;
      case Legal:
      case ExpandInteger:
        res = n.getOperand(0);
        break;
      case PromotedInteger:
        res = getPromotedInteger(n.getOperand(0));
        break;
    }
    return dag.getNode(ISD.TRUNCATE, nvt, res);
  }

  private SDValue promoteIntResUADDSUBO(SDNode n, int resNo) {
    if (resNo == 1)
      return promoteIntResOverflow(n);

    SDValue lhs = zextPromotedInteger(n.getOperand(0));
    SDValue rhs = zextPromotedInteger(n.getOperand(1));
    EVT ovt = n.getOperand(0).getValueType();
    EVT nvt = lhs.getValueType();
    int opc = n.getOpcode() == ISD.UADDO ? ISD.ADD : ISD.SUB;
    SDValue res = dag.getNode(opc, nvt, lhs, rhs);

    SDValue ofl = dag.getZeroExtendInReg(res, ovt);
    ofl = dag.getSetCC(n.getValueType(1), ofl, res, CondCode.SETNE);

    replaceValueWith(new SDValue(n, 1), ofl);
    return res;
  }

  private SDValue promoteIntResUDIV(SDNode n) {
    SDValue lhs = zextPromotedInteger(n.getOperand(0));
    SDValue rhs = zextPromotedInteger(n.getOperand(1));
    return dag.getNode(n.getOpcode(), lhs.getValueType(),
        lhs, rhs);
  }

  private SDValue promoteIntResUNDEF(SDNode n) {
    return dag.getUNDEF(tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0)));
  }

  private SDValue promoteIntResVAARG(SDNode n) {
    SDValue chain = n.getOperand(0);
    SDValue ptr = n.getOperand(1);
    EVT vt = n.getValueType(0);
    EVT regVT = tli.getRegisterType(dag.getContext(), vt);
    int numRegs = tli.getNumRegisters(dag.getContext(), vt);

    SDValue[] parts = new SDValue[numRegs];
    for (int i = 0; i < numRegs; i++) {
      parts[i] = dag.getVAArg(regVT, chain, ptr, n.getOperand(2));
      chain = parts[i].getValue(1);
    }

    if (tli.isBigEndian()) {
      Util.reverse(parts);
    }
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue res = dag.getNode(ISD.ZERO_EXTEND, nvt, parts[0]);
    for (int i = 1; i < numRegs; i++) {
      SDValue part = dag.getNode(ISD.ZERO_EXTEND, nvt, parts[i]);
      part = dag.getNode(ISD.SHL, nvt, part, dag.getConstant(i * regVT.getSizeInBits(),
          new EVT(tli.getPointerTy()), false));
      res = dag.getNode(ISD.OR, nvt, res, part);
    }
    replaceValueWith(new SDValue(n, 1), chain);
    return res;
  }

  private SDValue promoteIntResXMULO(SDNode n, int resNo) {
    Util.assertion(resNo == 1);
    return promoteIntResOverflow(n);
  }

  private boolean promoteIntegerOperand(SDNode node, int opNo) {
    if (Util.DEBUG) {
      System.err.print("Promote integer operand: ");
      node.dump(dag);
      System.err.println();
    }
    SDValue res = new SDValue();
    if (customLowerNode(node, node.getOperand(opNo).getValueType(), false))
      return false;

    switch (node.getOpcode()) {
      default:
        System.err.printf("promoteIntegerOperand op#%d: ", opNo);
        Util.shouldNotReachHere("Don't know how to promote this operator's operand!");
        break;
      case ISD.ANY_EXTEND:
        res = promoteOpAnyExtend(node);
        break;
      case ISD.BIT_CONVERT:
        res = promoteOpBitConvert(node);
        break;
      case ISD.BR_CC:
        res = promoteOpBRCC(node, opNo);
        break;
      case ISD.BRCOND:
        res = promoteOpBRCond(node, opNo);
        break;
      case ISD.BUILD_PAIR:
        res = promoteOpBuildPair(node);
        break;
      case ISD.BUILD_VECTOR:
        res = promoteOpBuildVector(node);
        break;
      case ISD.CONVERT_RNDSAT:
        res = promoteOpConvertRndsat(node);
        break;
      case ISD.INSERT_VECTOR_ELT:
        res = promoteOpInsertVectorElt(node, opNo);
        break;
      case ISD.MEMBARRIER:
        res = promoteOpMemBarrier(node);
        break;
      case ISD.SCALAR_TO_VECTOR:
        res = promoteOpScalarToVector(node);
        break;
      case ISD.SELECT:
        res = promoteOpSelect(node, opNo);
        break;
      case ISD.SELECT_CC:
        res = promoteOpSelectCC(node, opNo);
        break;
      case ISD.SETCC:
        res = promoteOpSetCC(node, opNo);
        break;
      case ISD.SIGN_EXTEND:
        res = promoteOpSignExtend(node);
        break;
      case ISD.SINT_TO_FP:
        res = promoteOpSINTToFP(node);
        break;
      case ISD.STORE:
        res = promoteOpStore((StoreSDNode) node, opNo);
        break;
      case ISD.TRUNCATE:
        res = promoteOpTruncate(node);
        break;
      case ISD.UINT_TO_FP:
        res = promoteOpUINTToFP(node);
        break;
      case ISD.ZERO_EXTEND:
        res = promoteOpZeroExtend(node);
        break;
      case ISD.SHL:
      case ISD.SRA:
      case ISD.SRL:
      case ISD.ROTL:
      case ISD.ROTR:
        res = promoteOpShift(node);
        break;
    }
    if (res.getNode() == null)
      return false;

    if (res.getNode().equals(node))
      return true;

    Util.assertion(res.getValueType().equals(node.getValueType(0)) && node.getNumValues() == 1);

    replaceValueWith(new SDValue(node, 0), res);
    return false;
  }

  private SDValue promoteOpAnyExtend(SDNode node) {
    SDValue op = getPromotedInteger(node.getOperand(0));
    return dag.getNode(ISD.ANY_EXTEND, node.getValueType(0), op);
  }

  private SDValue promoteOpBitConvert(SDNode node) {
    return createStackStoreLoad(node.getOperand(0), node.getValueType(0));
  }

  private SDValue promoteOpBuildPair(SDNode node) {
    EVT ovt = node.getOperand(0).getValueType();
    SDValue lo = zextPromotedInteger(node.getOperand(0));
    SDValue hi = getPromotedInteger(node.getOperand(1));
    Util.assertion(lo.getValueType().equals(node.getValueType(0)));
    hi = dag.getNode(ISD.SHL, node.getValueType(0), hi,
        dag.getConstant(ovt.getSizeInBits(), new EVT(tli.getPointerTy()),
            false));
    return dag.getNode(ISD.OR, node.getValueType(0), lo, hi);
  }

  private SDValue promoteOpBRCC(SDNode node, int opNo) {
    Util.assertion(opNo == 2);
    SDValue lhs = node.getOperand(2);
    SDValue rhs = node.getOperand(3);
    SDValue[] res = promoteSetCCOperands(lhs, rhs,
        ((CondCodeSDNode) node.getOperand(1).getNode()).getCondition());
    lhs = res[0];
    rhs = res[1];
    return dag.updateNodeOperands(new SDValue(node, 0),
        node.getOperand(0), node.getOperand(1), lhs, rhs,
        node.getOperand(4));

  }

  private SDValue promoteOpBRCond(SDNode node, int opNo) {
    Util.assertion(opNo == 1);
    EVT svt = new EVT(tli.getSetCCResultType(new EVT(MVT.Other)));
    SDValue cond = promoteTargetBoolean(node.getOperand(1), svt);
    return dag.updateNodeOperands(new SDValue(node, 0),
        node.getOperand(0), cond, node.getOperand(2));
  }

  private SDValue promoteOpBuildVector(SDNode node) {
    EVT vecVT = node.getValueType(0);
    int numElts = vecVT.getVectorNumElements();
    Util.assertion((numElts & 1) == 0);
    Util.assertion(node.getOperand(0).getValueType().getSizeInBits() >= node.getValueType(0).getVectorElementType().getSizeInBits());


    SDValue[] ops = new SDValue[numElts];
    for (int i = 0; i < numElts; i++)
      ops[i] = getPromotedInteger(node.getOperand(i));
    return dag.updateNodeOperands(new SDValue(node, 0), ops);
  }

  private SDValue promoteOpConvertRndsat(SDNode node) {
    CvtCode cc = ((CvtRndSatSDNode) node).getCvtCode();
    Util.assertion(cc == CvtCode.CVT_SS || cc == CvtCode.CVT_SU || cc == CvtCode.CVT_US || cc == CvtCode.CVT_UU ||
        cc == CvtCode.CVT_FS || cc == CvtCode.CVT_FU);

    SDValue inOp = getPromotedInteger(node.getOperand(0));
    return dag.getConvertRndSat(node.getValueType(0),
        inOp, node.getOperand(1),
        node.getOperand(2),
        node.getOperand(3),
        node.getOperand(4),
        cc);
  }

  private SDValue promoteOpInsertVectorElt(SDNode node, int opNo) {
    if (opNo == 1) {
      Util.assertion(node.getOperand(1).getValueType().getSizeInBits() >= node.getValueType(0).getVectorElementType().getSizeInBits());

      return dag.updateNodeOperands(new SDValue(node, 0),
          node.getOperand(0), getPromotedInteger(node.getOperand(1)),
          node.getOperand(2));
    }
    Util.assertion(opNo == 2);
    SDValue idx = zextPromotedInteger(node.getOperand(2));
    return dag.updateNodeOperands(new SDValue(node, 0),
        node.getOperand(0),
        node.getOperand(1), idx);
  }

  private SDValue promoteOpMemBarrier(SDNode node) {
    SDValue[] ops = new SDValue[6];
    ops[0] = node.getOperand(0);
    for (int i = 0; i < ops.length; i++) {
      SDValue flag = getPromotedInteger(node.getOperand(0));
      ops[i] = dag.getZeroExtendInReg(flag, new EVT(MVT.i1));
    }
    return dag.updateNodeOperands(new SDValue(node, 0), ops);
  }

  private SDValue promoteOpScalarToVector(SDNode node) {
    return dag.updateNodeOperands(new SDValue(node, 0),
        getPromotedInteger(node.getOperand(0)));
  }

  private SDValue promoteOpSelect(SDNode node, int opNo) {
    Util.assertion(opNo == 0);
    EVT svt = new EVT(tli.getSetCCResultType(node.getOperand(1).getValueType()));
    SDValue cond = promoteTargetBoolean(node.getOperand(0), svt);
    return dag.updateNodeOperands(new SDValue(node, 0), cond,
        node.getOperand(1), node.getOperand(2));
  }

  private SDValue promoteOpSelectCC(SDNode node, int opNo) {
    Util.assertion(opNo == 0);
    SDValue lhs = node.getOperand(0);
    SDValue rhs = node.getOperand(1);
    promoteSetCCOperands(lhs, rhs, ((CondCodeSDNode) node.getOperand(4).getNode()).getCondition());
    return dag.updateNodeOperands(new SDValue(node, 0), lhs, rhs,
        node.getOperand(2), node.getOperand(3), node.getOperand(4));
  }

  private SDValue promoteOpSetCC(SDNode node, int opNo) {
    Util.assertion(opNo == 0);
    SDValue lhs = node.getOperand(0);
    SDValue rhs = node.getOperand(1);
    SDValue[] res = promoteSetCCOperands(lhs, rhs, ((CondCodeSDNode) node.getOperand(2).getNode()).getCondition());
    lhs = res[0];
    rhs = res[1];
    return dag.updateNodeOperands(new SDValue(node, 0), lhs, rhs,
        node.getOperand(2));
  }

  private SDValue promoteOpShift(SDNode node) {
    return dag.updateNodeOperands(new SDValue(node, 0), node.getOperand(0),
        zextPromotedInteger(node.getOperand(1)));
  }

  private SDValue promoteOpSignExtend(SDNode node) {
    SDValue op = getPromotedInteger(node.getOperand(0));
    op = dag.getNode(ISD.ANY_EXTEND, node.getValueType(0), op);
    return dag.getNode(ISD.SIGN_EXTEND_INREG, op.getValueType(),
        op, dag.getValueType(node.getOperand(0).getValueType()));
  }

  private SDValue promoteOpSINTToFP(SDNode node) {
    return dag.updateNodeOperands(new SDValue(node, 0),
        sextPromotedInteger(node.getOperand(0)));
  }

  private SDValue promoteOpStore(StoreSDNode node, int opNo) {
    Util.assertion(node.isUNINDEXEDStore(), "Indexed store during type legalizer?");
    SDValue ch = node.getChain(), ptr = node.getBasePtr();
    int svOffset = node.getSrcValueOffset();
    int alignment = node.getAlignment();
    boolean isVolatile = node.isVolatile();
    SDValue val = getPromotedInteger(node.getValue());
    return dag.getTruncStore(ch, val, ptr, node.getSrcValue(),
        svOffset, node.getMemoryVT(), isVolatile, alignment);
  }

  private SDValue promoteOpTruncate(SDNode node) {
    SDValue op = getPromotedInteger(node.getOperand(0));
    return dag.getNode(ISD.TRUNCATE, node.getValueType(0), op);
  }

  private SDValue promoteOpUINTToFP(SDNode node) {
    return dag.updateNodeOperands(new SDValue(node, 0),
        zextPromotedInteger(node.getOperand(0)));
  }

  private SDValue promoteOpZeroExtend(SDNode node) {
    SDValue op = getPromotedInteger(node.getOperand(0));
    op = dag.getNode(ISD.ANY_EXTEND, node.getValueType(0), op);
    return dag.getZeroExtendInReg(op, node.getOperand(0).getValueType());
  }

  private SDValue[] promoteSetCCOperands(SDValue lhs, SDValue rhs, CondCode cc) {
    switch (cc) {
      default:
        Util.shouldNotReachHere("Unknown integer comparsion!");
        break;
      case SETEQ:
      case SETNE:
      case SETUGE:
      case SETUGT:
      case SETULE:
      case SETULT:
        lhs = zextPromotedInteger(lhs);
        rhs = zextPromotedInteger(rhs);
        break;
      case SETGE:
      case SETGT:
      case SETLT:
      case SETLE:
        lhs = sextPromotedInteger(lhs);
        rhs = sextPromotedInteger(rhs);
        break;
    }
    return new SDValue[]{lhs, rhs};
  }

  private SDValue[] getExpandedInteger(SDValue op) {
    if (!expandedIntegers.containsKey(op)) {
      expandedIntegers.put(op, Pair.get(new SDValue(), new SDValue()));
    }
    Pair<SDValue, SDValue> entry = expandedIntegers.get(op);
    entry.first = remapValue(entry.first);
    entry.second = remapValue(entry.second);
    Util.assertion(entry.first.getNode() != null, "Operand isn't expanded");
    return new SDValue[]{entry.first, entry.second};

  }

  private void setExpandedIntegers(SDValue op, SDValue lo, SDValue hi) {
    Util.assertion(lo.getValueType().equals(tli.getTypeToTransformTo(dag.getContext(), op.getValueType())) && hi.getValueType().equals(lo.getValueType()), "Invalid type for expanded integer");

    lo = analyzeNewValue(lo);
    hi = analyzeNewValue(hi);
    Util.assertion(!expandedIntegers.containsKey(op), "Node already expanded!");
    expandedIntegers.put(op, Pair.get(lo, hi));
  }

  private void expandIntegerResult(SDNode n, int resNo) {
    if (Util.DEBUG) {
      System.err.print("Expand integer result: ");
      n.dump(dag);
      System.err.println();
    }
    SDValue[] res = null;
    if (customLowerNode(n, n.getValueType(resNo), true))
      return;

    switch (n.getOpcode()) {
      default:
        if (Util.DEBUG) {
          System.err.printf("expandIntegerResult #%d: ", resNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't how to expand the result of this operator");
        break;
      case ISD.MERGE_VALUES:
        res = splitRes_MERGE_VALUES(n);
        break;
      case ISD.SELECT:
        res = splitRes_SELECT(n);
        break;
      case ISD.SELECT_CC:
        res = splitRes_SELECT_CC(n);
        break;
      case ISD.UNDEF:
        res = splitRes_UNDEF(n);
        break;
      case ISD.BIT_CONVERT:
        res = expandRes_BIT_CONVERT(n);
        break;
      case ISD.BUILD_PAIR:
        res = expandRes_BUILD_PAIR(n);
        break;
      case ISD.EXTRACT_ELEMENT:
        res = expandRes_EXTRACT_ELEMENT(n);
        break;
      case ISD.EXTRACT_VECTOR_ELT:
        res = expandRes_EXTRACT_VECTOR_ELT(n);
        break;
      case ISD.VAARG:
        res = expandRes_VAARG(n);
        break;
      case ISD.ANY_EXTEND:
        res = expandIntResAnyExtend(n);
        break;
      case ISD.AssertSext:
        res = expandIntResAssertSext(n);
        break;
      case ISD.AssertZext:
        res = expandIntResAssertZext(n);
        break;
      case ISD.BSWAP:
        res = expandIntResBSWAP(n);
        break;
      case ISD.Constant:
        res = expandIntResConstant(n);
        break;
      case ISD.CTLZ:
        res = expandIntResCTLZ(n);
        break;
      case ISD.CTPOP:
        res = expandIntResCTPOP(n);
        break;
      case ISD.CTTZ:
        res = expandIntResCTTZ(n);
        break;
      case ISD.FP_TO_SINT:
        res = expandIntResFPToSINT(n);
        break;
      case ISD.FP_TO_UINT:
        res = expandIntResFPToUINT(n);
        break;
      case ISD.LOAD:
        res = expandIntResLoad((LoadSDNode) n);
        break;
      case ISD.MUL:
        res = expandIntResMul(n);
        break;
      case ISD.SDIV:
        res = expandIntResSDIV(n);
        break;
      case ISD.SIGN_EXTEND:
        res = expandIntResSignExtend(n);
        break;
      case ISD.SIGN_EXTEND_INREG:
        res = expandIntResSignExtendInreg(n);
        break;
      case ISD.SREM:
        res = expandIntResSREM(n);
        break;
      case ISD.TRUNCATE:
        res = expandIntResTruncate(n);
        break;
      case ISD.UDIV:
        res = expandIntResUDIV(n);
        break;
      case ISD.UREM:
        res = expandIntResUREM(n);
        break;
      case ISD.ZERO_EXTEND:
        res = expandIntResZeroExtend(n);
        break;

      case ISD.AND:
      case ISD.OR:
      case ISD.XOR:
        res = expandIntResLogical(n);
        break;
      case ISD.ADD:
      case ISD.SUB:
        res = expandIntResAddSub(n);
        break;
      case ISD.ADDC:
      case ISD.SUBC:
        res = expandIntResAddSubc(n);
        break;
      case ISD.ADDE:
      case ISD.SUBE:
        res = expandIntResAddSube(n);
        break;
      case ISD.SHL:
      case ISD.SRA:
      case ISD.SRL:
        res = expandIntResShift(n);
        break;
    }
    Util.assertion(res != null && res.length == 2, "Illegal status!");
    SDValue lo = res[0];
    SDValue hi = res[1];
    if (lo.getNode() != null)
      setExpandedIntegers(new SDValue(n, resNo), lo, hi);
  }

  private SDValue[] expandIntResAnyExtend(SDNode n) {
    SDValue[] res = new SDValue[2];
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = n.getOperand(0);
    if (op.getValueType().bitsLE(nvt)) {
      res[0] = dag.getNode(ISD.ANY_EXTEND, nvt, op);
      res[1] = dag.getUNDEF(nvt);
    } else {
      Util.assertion(getTypeAction(op.getValueType()) == LegalizeAction.PromotedInteger, "Only know how to promote this result!");

      SDValue t = getPromotedInteger(op);
      Util.assertion(t.getValueType().equals(n.getValueType(0)), "Operand over promoted!");
      res = splitInteger(t);
    }
    return res;
  }

  private SDValue[] expandIntResAssertSext(SDNode n) {
    SDValue[] res;
    res = getExpandedInteger(n.getOperand(0));
    Util.assertion(res.length == 2, "Illegal status!");
    EVT nvt = res[0].getValueType();
    EVT evt = ((VTSDNode) n.getOperand(1).getNode()).getVT();
    int nvtBits = nvt.getSizeInBits();
    int evtBits = evt.getSizeInBits();

    if (nvtBits < evtBits)
      res[1] = dag.getNode(ISD.AssertSext,
          nvt, res[1], dag.getValueType(EVT.getIntegerVT(dag.getContext(), 
              evtBits - nvtBits)));
    else {
      res[0] = dag.getNode(ISD.AssertSext, nvt, res[0], dag.getValueType(evt));
      res[1] = dag.getNode(ISD.SRA, nvt, res[0], dag.getConstant(nvtBits - 1,
          new EVT(tli.getPointerTy()), false));
    }
    return res;
  }

  private SDValue[] expandIntResAssertZext(SDNode n) {
    SDValue[] res;
    res = getExpandedInteger(n.getOperand(0));
    Util.assertion(res.length == 2, "Illegal status!");
    EVT nvt = res[0].getValueType();
    EVT evt = ((VTSDNode) n.getOperand(1).getNode()).getVT();
    int nvtBits = nvt.getSizeInBits();
    int evtBits = evt.getSizeInBits();

    if (nvtBits < evtBits)
      res[1] = dag.getNode(ISD.AssertZext,
          nvt, res[1], dag.getValueType(EVT.getIntegerVT(dag.getContext(), 
              evtBits - nvtBits)));
    else {
      res[0] = dag.getNode(ISD.AssertZext, nvt, res[0], dag.getValueType(evt));
      res[1] = dag.getConstant(0, nvt, false);
    }
    return res;
  }

  private SDValue[] expandIntResConstant(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    int nvtBits = nvt.getSizeInBits();
    APInt cst = ((ConstantSDNode) n).getAPIntValue();
    SDValue[] res = new SDValue[2];
    res[0] = dag.getConstant(new APInt(cst).trunc(nvtBits), nvt, false);
    res[1] = dag.getConstant(cst.lshr(nvtBits).trunc(nvtBits), nvt, false);
    return res;
  }

  private SDValue[] expandIntResCTLZ(SDNode n) {
    SDValue[] res = getExpandedInteger(n.getOperand(0));
    EVT nvt = res[0].getValueType();
    SDValue hiNotZero = dag.getSetCC(
        new EVT(tli.getSetCCResultType(nvt)),
        res[1], dag.getConstant(0, nvt, false), CondCode.SETNE);
    SDValue loLZ = dag.getNode(ISD.CTLZ, nvt, res[0]);
    SDValue hiLZ = dag.getNode(ISD.CTLZ, nvt, res[1]);
    res[0] = dag.getNode(ISD.SELECT, nvt, hiNotZero, hiLZ,
        dag.getNode(ISD.ADD, nvt, loLZ,
            dag.getConstant(nvt.getSizeInBits(), nvt, false)));
    res[1] = dag.getConstant(0, nvt, false);
    return res;
  }

  private SDValue[] expandIntResCTPOP(SDNode n) {
    SDValue[] res = getExpandedInteger(n.getOperand(0));
    EVT nvt = res[0].getValueType();
    res[0] = dag.getNode(ISD.ADD, nvt, dag.getNode(ISD.CTPOP, nvt, res[0]),
        dag.getNode(ISD.CTPOP, nvt, res[1]));
    res[1] = dag.getConstant(0, nvt, false);
    return res;
  }

  private SDValue[] expandIntResCTTZ(SDNode n) {
    SDValue[] res = getExpandedInteger(n.getOperand(0));
    EVT nvt = res[0].getValueType();

    SDValue loNotZero = dag.getSetCC(new EVT(tli.getSetCCResultType(nvt)),
        res[0], dag.getConstant(0, nvt, false), CondCode.SETNE);
    SDValue loLZ = dag.getNode(ISD.CTTZ, nvt, res[0]);
    SDValue hiLZ = dag.getNode(ISD.CTTZ, nvt, res[1]);
    res[0] = dag.getNode(ISD.SELECT, nvt, loNotZero, loLZ,
        dag.getNode(ISD.ADD, nvt, hiLZ, dag.getConstant(nvt.getSizeInBits(),
            nvt, false)));
    res[1] = dag.getConstant(0, nvt, false);
    return res;
  }

  private SDValue[] expandIntResLoad(LoadSDNode n) {
    if (n.isNormalLoad()) {
      return expandRes_NormalLoad(n);
    }

    Util.assertion(n.isUNINDEXEDLoad(), "indexed load during type legalization!");

    EVT vt = n.getValueType(0);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), vt);
    SDValue ch = n.getChain();
    SDValue ptr = n.getBasePtr();
    LoadExtType ext = n.getExtensionType();
    int svOffset = n.getSrcValueOffset();
    int alignment = n.getAlignment();
    boolean isVolatile = n.isVolatile();

    Util.assertion(nvt.isByteSized(), "expanded type not byte sized");
    SDValue lo, hi;
    if (n.getMemoryVT().bitsLE(nvt)) {
      EVT evt = n.getMemoryVT();
      lo = dag.getExtLoad(ext, nvt, ch, ptr, n.getSrcValue(),
          svOffset, evt, isVolatile, alignment);
      ch = lo.getValue(1);
      if (ext == LoadExtType.SEXTLOAD) {
        int loSize = lo.getValueType().getSizeInBits();
        hi = dag.getNode(ISD.SRA, nvt, lo, dag.getConstant(loSize - 1,
            new EVT(tli.getPointerTy()), false));
      } else if (ext == LoadExtType.ZEXTLOAD) {
        hi = dag.getConstant(0, nvt, false);
      } else {
        Util.assertion(ext == LoadExtType.EXTLOAD, "Unknown extload!");
        hi = dag.getUNDEF(nvt);
      }
    } else if (tli.isLittleEndian()) {
      lo = dag.getLoad(nvt, ch, ptr, n.getSrcValue(), svOffset, isVolatile,
          alignment);
      int excessBits = n.getMemoryVT().getSizeInBits() - nvt.getSizeInBits();
      EVT nevt = EVT.getIntegerVT(dag.getContext(), excessBits);
      int incrementSize = nvt.getSizeInBits() / 8;
      ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
          dag.getIntPtrConstant(incrementSize));
      hi = dag.getExtLoad(ext, nvt, ch, ptr, n.getSrcValue(),
          svOffset + incrementSize, nevt,
          isVolatile, Util.minAlign(alignment, incrementSize));
      ch = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
          lo.getValue(1), hi.getValue(1));
    } else {
      EVT evt = n.getMemoryVT();
      int ebytes = evt.getStoreSizeInBits() / 8;
      int increemntSize = nvt.getSizeInBits() / 8;
      int excessBits = (ebytes - increemntSize) << 3;

      hi = dag.getExtLoad(ext, nvt, ch, ptr, n.getSrcValue(), svOffset,
          EVT.getIntegerVT(dag.getContext(), evt.getSizeInBits() - excessBits),
          isVolatile, alignment);
      ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr, dag.getIntPtrConstant(increemntSize));
      lo = dag.getExtLoad(LoadExtType.ZEXTLOAD, nvt, ch, ptr, n.getSrcValue(),
          svOffset + increemntSize,
          EVT.getIntegerVT(dag.getContext(), excessBits),
          isVolatile, Util.minAlign(alignment, increemntSize));

      ch = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo.getValue(1),
          hi.getValue(1));

      if (excessBits < nvt.getSizeInBits()) {
        lo = dag.getNode(ISD.OR, nvt, lo, dag.getNode(ISD.SHL, nvt, hi,
            dag.getConstant(excessBits, new EVT(tli.getPointerTy()), false)));
        hi = dag.getNode(ext == LoadExtType.SEXTLOAD ?
            ISD.SRA : ISD.SRL, nvt, hi, dag.getConstant(nvt.getSizeInBits() -
            excessBits, new EVT(tli.getPointerTy()), false));
      }
      replaceValueWith(new SDValue(n, 1), ch);
    }
    return new SDValue[]{lo, hi};
  }

  private SDValue makeLibCall(RTLIB libCall, EVT retVT,
                              SDValue[] ops, boolean isSigned) {
    ArrayList<ArgListEntry> args = new ArrayList<>(ops.length);
    for (int i = 0; i < ops.length; i++) {
      ArgListEntry entry = new ArgListEntry();
      entry.node = ops[i];
      entry.ty = entry.node.getValueType().getTypeForEVT(dag.getContext());
      entry.isSExt = isSigned;
      entry.isZExt = !isSigned;
      args.add(entry);
    }
    SDValue callee = dag.getExternalSymbol(tli.getLibCallName(libCall),
        new EVT(tli.getPointerTy()));
    Type retTy = retVT.getTypeForEVT(dag.getContext());
    Pair<SDValue, SDValue> callInfo =
        tli.lowerCallTo(dag.getContext(), dag.getEntryNode(), retTy, isSigned,
            !isSigned, false, false, 0,
            tli.getLibCallCallingConv(libCall), false, true,
            callee, args, dag);
    return callInfo.first;
  }

  private SDValue[] expandIntResSignExtend(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = n.getOperand(0);
    SDValue lo, hi;
    if (op.getValueType().bitsLE(nvt)) {
      lo = dag.getNode(ISD.SIGN_EXTEND, nvt, n.getOperand(0));
      int loSize = nvt.getSizeInBits();
      hi = dag.getNode(ISD.SRA, nvt, lo, dag.getConstant(loSize - 1,
          new EVT(tli.getPointerTy()), false));
    } else {
      Util.assertion(getTypeAction(op.getValueType()) == LegalizeAction.PromotedInteger);
      SDValue res = getPromotedInteger(op);
      Util.assertion(res.getValueType().equals(n.getValueType(0)), "Operand over promoted!");
      SDValue[] t = splitInteger(res);
      lo = t[0];
      hi = t[1];
      int excessBits = op.getValueType().getSizeInBits() - nvt.getSizeInBits();
      hi = dag.getNode(ISD.SIGN_EXTEND_INREG, hi.getValueType(),
          hi, dag.getValueType(EVT.getIntegerVT(dag.getContext(), excessBits)));
    }
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandIntResSignExtendInreg(SDNode n) {
    SDValue[] res = getExpandedInteger(n.getOperand(0));
    EVT vt = ((VTSDNode) n.getOperand(1).getNode()).getVT();

    if (vt.bitsLE(res[0].getValueType())) {
      res[0] = dag.getNode(ISD.SIGN_EXTEND_INREG, res[0].getValueType(),
          res[0], n.getOperand(1));
      res[1] = dag.getNode(ISD.SRA, res[1].getValueType(), res[0],
          dag.getConstant(res[1].getValueType().getSizeInBits() - 1,
              new EVT(tli.getPointerTy()), false));
    } else {
      int excessBits = vt.getSizeInBits() - res[0].getValueType().getSizeInBits();
      res[1] = dag.getNode(ISD.SIGN_EXTEND_INREG, res[1].getValueType(),
          res[1], dag.getValueType(EVT.getIntegerVT(dag.getContext(), excessBits)));
    }
    return res;
  }

  private SDValue[] expandIntResTruncate(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue lo, hi;
    lo = dag.getNode(ISD.TRUNCATE, nvt, n.getOperand(0));
    hi = dag.getNode(ISD.SRL, n.getOperand(0).getValueType(), n.getOperand(0),
        dag.getConstant(nvt.getSizeInBits(), new EVT(tli.getPointerTy()), false));
    hi = dag.getNode(ISD.TRUNCATE, nvt, hi);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandIntResZeroExtend(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = n.getOperand(0);
    SDValue lo, hi;
    if (op.getValueType().bitsLE(nvt)) {
      lo = dag.getNode(ISD.ZERO_EXTEND, nvt, n.getOperand(0));
      hi = dag.getConstant(0, nvt, false);
    } else {
      Util.assertion(getTypeAction(op.getValueType()) == LegalizeAction.PromotedInteger, "Don't know how to handle this result!");

      SDValue res = getPromotedInteger(op);
      Util.assertion(res.getValueType().equals(n.getValueType(0)), "Operand over promoted!");
      SDValue[] t = splitInteger(res);
      lo = t[0];
      hi = t[1];
      int excessBits = op.getValueType().getSizeInBits() - nvt.getSizeInBits();
      hi = dag.getZeroExtendInReg(hi, EVT.getIntegerVT(dag.getContext(), excessBits));
    }
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandIntResFPToSINT(SDNode n) {
    EVT vt = n.getValueType(0);
    SDValue op = n.getOperand(0);
    RTLIB libCall = tli.getFPTOSINT(op.getValueType(), vt);
    Util.assertion(libCall != RTLIB.UNKNOWN_LIBCALL, "Unexpected fp-to-sint conversion!");
    return splitInteger(makeLibCall(libCall, vt, new SDValue[]{op}, true));
  }

  private SDValue[] expandIntResFPToUINT(SDNode n) {
    EVT vt = n.getValueType(0);
    SDValue op = n.getOperand(0);
    RTLIB libCall = tli.getFPTOUINT(op.getValueType(), vt);
    Util.assertion(libCall != RTLIB.UNKNOWN_LIBCALL, "Unexpected fp-to-uint conversion!");
    return splitInteger(makeLibCall(libCall, vt, new SDValue[]{op}, true));
  }

  private SDValue[] expandIntResLogical(SDNode n) {
    SDValue[] res0 = getExpandedInteger(n.getOperand(0));
    SDValue[] res1 = getExpandedInteger(n.getOperand(1));
    return new SDValue[]{
        dag.getNode(n.getOpcode(), res0[0].getValueType(), res0[0], res1[0]),
        dag.getNode(n.getOpcode(), res0[0].getValueType(), res0[1], res1[1])
    };
  }

  private SDValue[] expandIntResAddSub(SDNode n) {
    SDValue[] res = new SDValue[2];
    SDValue[] lhsT = getExpandedInteger(n.getOperand(0));
    SDValue[] rhsT = getExpandedInteger(n.getOperand(1));
    EVT nvt = lhsT[0].getValueType();

    SDValue[] loOps = {lhsT[0], rhsT[0]};
    SDValue[] hiOps = {lhsT[1], rhsT[1], null};

    boolean hasCarry = tli.isOperationLegalOrCustom(n.getOpcode() == ISD.ADD ?
        ISD.ADDC : ISD.SUBC, tli.getTypeToExpandTo(dag.getContext(), nvt));
    if (hasCarry) {
      SDVTList vts = dag.getVTList(nvt, new EVT(MVT.Glue));
      if (n.getOpcode() == ISD.ADD) {
        res[0] = dag.getNode(ISD.ADDC, vts, loOps);
        hiOps[2] = res[0].getValue(1);
        res[1] = dag.getNode(ISD.ADDE, vts, hiOps);
      } else {
        res[0] = dag.getNode(ISD.SUBC, vts, loOps);
        hiOps[2] = res[0].getValue(1);
        res[1] = dag.getNode(ISD.SUBE, vts, hiOps);
      }
    } else {
      if (n.getOpcode() == ISD.ADD) {
        res[0] = dag.getNode(ISD.ADD, nvt, loOps);
        res[1] = dag.getNode(ISD.ADD, nvt, hiOps);
        SDValue cmp1 = dag.getSetCC(new EVT(tli.getSetCCResultType(nvt)),
            res[0], loOps[0], CondCode.SETULT);
        SDValue carry1 = dag.getNode(ISD.SELECT, nvt, cmp1,
            dag.getConstant(1, nvt, false),
            dag.getConstant(0, nvt, false));
        SDValue cmp2 = dag.getSetCC(new EVT(tli.getSetCCResultType(nvt)),
            res[0], loOps[1], CondCode.SETULT);
        SDValue carry2 = dag.getNode(ISD.SELECT, nvt, cmp2,
            dag.getConstant(1, nvt, false),
            carry1);
        res[1] = dag.getNode(ISD.ADD, nvt, res[1], carry2);
      } else {
        res[0] = dag.getNode(ISD.SUB, nvt, loOps);
        res[1] = dag.getNode(ISD.SUBC, nvt, hiOps);
        SDValue cmp = dag.getSetCC(new EVT(tli.getSetCCResultType(
            loOps[0].getValueType())),
            loOps[0], loOps[1], CondCode.SETULT);
        SDValue borrow = dag.getNode(ISD.SELECT, nvt, cmp,
            dag.getConstant(1, nvt, false),
            dag.getConstant(0, nvt, false));
        res[1] = dag.getNode(ISD.SUB, nvt, res[1], borrow);
      }
    }
    return res;
  }

  private SDValue[] expandIntResAddSubc(SDNode n) {
    SDValue[] res = new SDValue[2];
    SDValue[] lhsT = getExpandedInteger(n.getOperand(0));
    SDValue[] rhsT = getExpandedInteger(n.getOperand(1));
    SDVTList vts = dag.getVTList(lhsT[0].getValueType(), new EVT(MVT.Glue));
    SDValue[] loOps = {lhsT[0], rhsT[0]};
    SDValue[] hiOps = {lhsT[1], rhsT[1], null};

    if (n.getOpcode() == ISD.ADDC) {
      res[0] = dag.getNode(ISD.ADDC, vts, loOps);
      hiOps[2] = res[0].getValue(1);
      res[1] = dag.getNode(ISD.ADDE, vts, hiOps);
    } else {
      res[0] = dag.getNode(ISD.SUBC, vts, loOps);
      hiOps[2] = res[0].getValue(1);
      res[1] = dag.getNode(ISD.SUBE, vts, hiOps);
    }

    replaceValueWith(new SDValue(n, 1), res[1].getValue(1));
    return res;
  }

  private SDValue[] expandIntResAddSube(SDNode n) {
    SDValue[] res = new SDValue[2];
    SDValue[] lhsT = getExpandedInteger(n.getOperand(0));
    SDValue[] rhsT = getExpandedInteger(n.getOperand(1));
    SDVTList vts = dag.getVTList(lhsT[0].getValueType(), new EVT(MVT.Glue));
    SDValue[] loOps = {lhsT[0], rhsT[0], n.getOperand(2)};
    SDValue[] hiOps = {lhsT[1], rhsT[1]};

    res[0] = dag.getNode(n.getOpcode(), vts, loOps);
    hiOps[2] = res[0].getValue(1);
    res[1] = dag.getNode(n.getOpcode(), vts, hiOps);
    replaceValueWith(new SDValue(n, 1), res[1].getValue(1));
    return res;
  }

  private SDValue[] expandIntResBSWAP(SDNode n) {
    SDValue[] res = getExpandedInteger(n.getOperand(0));
    return new SDValue[]{dag.getNode(ISD.BSWAP, res[0].getValueType(), res[0]),
        dag.getNode(ISD.BSWAP, res[1].getValueType(), res[1])
    };
  }

  private SDValue[] expandIntResMul(SDNode n) {
    EVT vt = n.getValueType(0);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), vt);
    boolean hasMULHS = tli.isOperationLegal(ISD.MULHS, nvt);
    boolean hasMULHU = tli.isOperationLegal(ISD.MULHU, nvt);
    boolean hasSMUL_LOHI = tli.isOperationLegal(ISD.SMUL_LOHI, nvt);
    boolean hasUMUL_LOHI = tli.isOperationLegal(ISD.UMUL_LOHI, nvt);
    SDValue lo = new SDValue(), hi = new SDValue();
    if (hasMULHU || hasMULHS || hasUMUL_LOHI || hasSMUL_LOHI) {
      SDValue[] res0 = getExpandedInteger(n.getOperand(0));
      SDValue[] res1 = getExpandedInteger(n.getOperand(1));
      int outerBitsize = vt.getSizeInBits();
      int innerBitsize = nvt.getSizeInBits();
      int lhssb = dag.computeNumSignBits(n.getOperand(0));
      int rhssb = dag.computeNumSignBits(n.getOperand(1));

      APInt highMask = APInt.getHighBitsSet(outerBitsize, innerBitsize);
      if (dag.maskedValueIsZero(n.getOperand(0), highMask) && dag
          .maskedValueIsZero(n.getOperand(1), highMask)) {
        if (hasUMUL_LOHI) {
          return new SDValue[]{lo = dag.getNode(ISD.UMUL_LOHI,
              dag.getVTList(nvt, nvt), res0[0], res1[0]),
              new SDValue(lo.getNode(), 1)};
        }
        if (hasSMUL_LOHI) {
          return new SDValue[]{
              dag.getNode(ISD.MUL, nvt, res0[0], res1[0]),
              dag.getNode(ISD.MULHU, nvt, res0[1], res1[1])};
        }
      }
      if (lhssb > innerBitsize && rhssb > innerBitsize) {
        if (hasSMUL_LOHI) {
          return new SDValue[]{lo = dag.getNode(ISD.SMUL_LOHI,
              dag.getVTList(nvt, nvt), res0[0], res1[0]),
              new SDValue(lo.getNode(), 1)};
        }
        if (hasMULHS) {
          return new SDValue[]{
              dag.getNode(ISD.MUL, nvt, res0[0], res1[0]),
              dag.getNode(ISD.MULHS, nvt, res0[0], res1[0])};
        }
      }
      if (hasUMUL_LOHI) {
        SDValue umulLOHI = dag
            .getNode(ISD.UMUL_LOHI, dag.getVTList(nvt, nvt), res0[0], res1[0]);
        lo = umulLOHI;
        hi = umulLOHI.getValue(1);
        res1[1] = dag.getNode(ISD.MUL, nvt, res0[0], res1[1]);
        res0[1] = dag.getNode(ISD.MUL, nvt, res0[1], res1[0]);
        hi = dag.getNode(ISD.ADD, nvt, hi, res0[1]);
        hi = dag.getNode(ISD.ADD, nvt, hi, res0[1]);
        return new SDValue[]{lo, hi};
      }
      if (hasMULHU) {
        lo = dag.getNode(ISD.MUL, nvt, res0[0], res1[0]);
        hi = dag.getNode(ISD.MULHU, nvt, res0[0], res1[0]);
        res1[1] = dag.getNode(ISD.MUL, nvt, res0[0], res1[1]);
        res0[1] = dag.getNode(ISD.MUL, nvt, res0[1], res1[0]);
        hi = dag.getNode(ISD.ADD, nvt, hi, res1[1]);
        hi = dag.getNode(ISD.ADD, nvt, hi, res0[1]);
        return new SDValue[]{lo, hi};
      }
    }

    RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
    switch (vt.getSimpleVT().simpleVT) {
      case MVT.i16:
        lc = RTLIB.MUL_I16;
        break;
      case MVT.i32:
        lc = RTLIB.MUL_I32;
        break;
      case MVT.i64:
        lc = RTLIB.MUL_I64;
        break;
      case MVT.i128:
        lc = RTLIB.MUL_I128;
        break;
    }
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported mul!");
    SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
    return splitInteger(makeLibCall(lc, vt, ops, true));
  }

  private SDValue[] expandIntResSDIV(SDNode n) {
    EVT vt = n.getValueType(0);
    RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
    switch (vt.getSimpleVT().simpleVT) {
      case MVT.i16:
        lc = RTLIB.SDIV_I16;
        break;
      case MVT.i32:
        lc = RTLIB.SDIV_I32;
        break;
      case MVT.i64:
        lc = RTLIB.SDIV_I64;
        break;
      case MVT.i128:
        lc = RTLIB.SDIV_I128;
        break;
    }
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported SDIV!");
    SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
    return splitInteger(makeLibCall(lc, vt, ops, true));
  }

  private SDValue[] expandIntResSREM(SDNode n) {
    EVT vt = n.getValueType(0);
    RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
    switch (vt.getSimpleVT().simpleVT) {
      case MVT.i16:
        lc = RTLIB.SREM_I16;
        break;
      case MVT.i32:
        lc = RTLIB.SREM_I32;
        break;
      case MVT.i64:
        lc = RTLIB.SREM_I64;
        break;
      case MVT.i128:
        lc = RTLIB.SREM_I128;
        break;
    }
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported SREM!");
    SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
    return splitInteger(makeLibCall(lc, vt, ops, true));
  }

  private SDValue[] expandIntResUDIV(SDNode n) {
    EVT vt = n.getValueType(0);
    RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
    switch (vt.getSimpleVT().simpleVT) {
      case MVT.i16:
        lc = RTLIB.UDIV_I16;
        break;
      case MVT.i32:
        lc = RTLIB.UDIV_I32;
        break;
      case MVT.i64:
        lc = RTLIB.UDIV_I64;
        break;
      case MVT.i128:
        lc = RTLIB.UDIV_I128;
        break;
    }
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported UDIV!");
    SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
    return splitInteger(makeLibCall(lc, vt, ops, true));
  }

  private SDValue[] expandIntResUREM(SDNode n) {
    EVT vt = n.getValueType(0);
    RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
    switch (vt.getSimpleVT().simpleVT) {
      case MVT.i16:
        lc = RTLIB.UREM_I16;
        break;
      case MVT.i32:
        lc = RTLIB.UREM_I32;
        break;
      case MVT.i64:
        lc = RTLIB.UREM_I64;
        break;
      case MVT.i128:
        lc = RTLIB.UREM_I128;
        break;
    }
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported UREM!");
    SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
    return splitInteger(makeLibCall(lc, vt, ops, true));
  }

  private SDValue[] expandIntResShift(SDNode n) {
    EVT vt = n.getValueType(0);
    if (n.getOperand(1).getNode() instanceof ConstantSDNode) {
      ConstantSDNode cn = (ConstantSDNode) n.getOperand(1).getNode();
      return expandShiftByConstant(n, cn.getZExtValue());
    }
    SDValue[] res = new SDValue[2];
    if (expandShiftWithKnownAmountBit(n, res))
      return res;

    int partsOpc;
    if (n.getOpcode() == ISD.SHL)
      partsOpc = ISD.SHL_PARTS;
    else if (n.getOpcode() == ISD.SRL)
      partsOpc = ISD.SRL_PARTS;
    else {
      Util.assertion(n.getOpcode() == ISD.SRA);
      partsOpc = ISD.SRA_PARTS;
    }

    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), vt);
    TargetLowering.LegalizeAction action = tli.getOperationAction(partsOpc, nvt);
    if ((action == TargetLowering.LegalizeAction.Legal && tli.isTypeLegal(nvt)) ||
        action == Custom) {
      SDValue[] t = getExpandedInteger(n.getOperand(0));

      SDValue[] ops = {t[0], t[1], n.getOperand(1)};
      vt = t[0].getValueType();
      res[0] = dag.getNode(partsOpc, dag.getVTList(vt, vt), ops);
      res[1] = res[0].getValue(1);
      return res;
    }

    RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
    boolean isSigned = false;
    switch (n.getOpcode()) {
      case ISD.SHL: {
        switch (vt.getSimpleVT().simpleVT) {
          case MVT.i16:
            lc = RTLIB.SHL_I16;
            break;
          case MVT.i32:
            lc = RTLIB.SHL_I32;
            break;
          case MVT.i64:
            lc = RTLIB.SHL_I64;
            break;
          case MVT.i128:
            lc = RTLIB.SHL_I128;
            break;
        }
        break;
      }
      case ISD.SRL: {
        switch (vt.getSimpleVT().simpleVT) {
          case MVT.i16:
            lc = RTLIB.SRL_I16;
            break;
          case MVT.i32:
            lc = RTLIB.SRL_I32;
            break;
          case MVT.i64:
            lc = RTLIB.SRL_I64;
            break;
          case MVT.i128:
            lc = RTLIB.SRL_I128;
            break;
        }
        break;
      }
      default:
        Util.assertion(n.getOpcode() == ISD.SRA);
        isSigned = true;
        switch (vt.getSimpleVT().simpleVT) {
          case MVT.i16:
            lc = RTLIB.SRA_I16;
            break;
          case MVT.i32:
            lc = RTLIB.SRA_I32;
            break;
          case MVT.i64:
            lc = RTLIB.SRA_I64;
            break;
          case MVT.i128:
            lc = RTLIB.SRA_I128;
            break;
        }
        break;
    }
    if (lc != RTLIB.UNKNOWN_LIBCALL && tli.getLibCallName(lc) != null) {
      SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
      return splitInteger(makeLibCall(lc, vt, ops, isSigned));
    }

    if (!expandShiftWithUnknownAmountBit(n, res)) {
      Util.shouldNotReachHere("Unsupported shift!");
    }
    return res;
  }

  private SDValue[] expandShiftByConstant(SDNode n, long amt) {
    SDValue[] res = new SDValue[2];
    SDValue[] t = getExpandedInteger(n.getOperand(0));
    EVT nvt = t[0].getValueType();
    int vtBits = n.getValueType(0).getSizeInBits();
    int nvtBits = nvt.getSizeInBits();
    EVT shTy = n.getOperand(1).getValueType();
    if (n.getOpcode() == ISD.SHL) {
      if (amt > vtBits) {
        res[0] = res[1] = dag.getConstant(0, nvt, false);
      } else if (amt > nvtBits) {
        res[0] = dag.getConstant(0, nvt, false);
        res[1] = dag.getNode(ISD.SHL, nvt, t[0],
            dag.getConstant(amt - nvtBits, shTy, false));
      } else if (amt == nvtBits) {
        res[0] = dag.getConstant(0, nvt, false);
        res[1] = t[0];
      } else if (amt == 1 && tli.isOperationLegal(ISD.ADDC,
          tli.getTypeToExpandTo(dag.getContext(), nvt))) {
        SDVTList vts = dag.getVTList(nvt, new EVT(MVT.Glue));
        SDValue[] ops = {t[0], t[0]};
        res[0] = dag.getNode(ISD.ADDC, vts, ops);
        SDValue[] ops3 = {t[1], t[1], res[0].getValue(1)};
        res[1] = dag.getNode(ISD.ADDE, vts, ops3);
      } else {
        res[0] = dag.getNode(ISD.SHL, nvt, t[0], dag.getConstant(amt, shTy, false));
        res[1] = dag.getNode(ISD.OR, nvt, dag.getNode(ISD.SHL,
            nvt, t[1], dag.getConstant(amt, shTy, false)),
            dag.getNode(ISD.SRL, nvt, t[0],
                dag.getConstant(nvtBits - amt, shTy, false)));
      }
      return res;
    }
    if (n.getOpcode() == ISD.SRL) {
      if (amt > vtBits) {
        res[0] = dag.getConstant(0, nvt, false);
        res[1] = dag.getConstant(0, nvt, false);
      } else if (amt > nvtBits) {
        res[0] = dag.getNode(ISD.SRL, nvt,
            t[1], dag.getConstant(amt - nvtBits, shTy, false));
        res[1] = dag.getConstant(0, nvt, false);
      } else if (amt == nvtBits) {
        res[0] = t[1];
        res[1] = dag.getConstant(0, nvt, false);
      } else {
        res[0] = dag.getNode(ISD.OR, nvt,
            dag.getNode(ISD.SRL, nvt, t[0], dag.getConstant(amt, shTy, false)),
            dag.getNode(ISD.SHL, nvt, t[1], dag.getConstant(nvtBits - amt, shTy, false)));
        res[1] = dag.getNode(ISD.SRL, nvt, t[1], dag.getConstant(amt, shTy, false));
      }
      return res;
    }
    Util.assertion(n.getOpcode() == ISD.SRA, "Unkown shift!");
    if (amt > vtBits)
      res[0] = res[1] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(nvtBits - 1, shTy, false));
    else if (amt > nvtBits) {
      res[0] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(amt - nvtBits, shTy, false));
      res[1] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(nvtBits - 1, shTy, false));
    } else if (amt == nvtBits) {
      res[0] = t[1];
      res[1] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(nvtBits - 1, shTy, false));
    } else {
      res[0] = dag.getNode(ISD.OR, nvt, dag.getNode(ISD.SRL, nvt, t[0],
          dag.getConstant(amt, shTy, false)),
          dag.getNode(ISD.SHL, nvt, t[1], dag.getConstant(nvtBits - amt, shTy, false)));
      res[1] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(amt, shTy, false));
    }
    return res;
  }

  private boolean expandShiftWithKnownAmountBit(SDNode n, SDValue[] res) {
    SDValue amt = n.getOperand(1);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    EVT shTy = amt.getValueType();
    int shBits = shTy.getSizeInBits();
    int nvtBits = nvt.getSizeInBits();
    Util.assertion(Util.isPowerOf2(nvtBits), "Expanded integer type size must be power of two!");
    APInt highBitMask = APInt.getHighBitsSet(shBits, shBits - Util.log2(nvtBits));
    APInt[] t = new APInt[2];
    dag.computeMaskedBits(n.getOperand(1), highBitMask, t, 0);
    APInt knownZero = t[0], knownOne = t[1];

    if ((knownZero.or(knownOne).and(highBitMask).eq(0)))
      return false;

    SDValue[] tt = getExpandedInteger(n.getOperand(0));

    if (knownOne.intersects(highBitMask)) {
      amt = dag.getNode(ISD.AND, shTy, amt,
          dag.getConstant(highBitMask.not(), shTy, false));
      switch (n.getOpcode()) {
        default:
          Util.shouldNotReachHere("Unknown shift!");
          break;
        case ISD.SHL:
          res[0] = dag.getConstant(0, nvt, false);
          res[1] = dag.getNode(ISD.SHL, nvt, tt[0], amt);
          return true;
        case ISD.SRL:
          res[1] = dag.getConstant(0, nvt, false);
          res[0] = dag.getNode(ISD.SRL, nvt, tt[1], amt);
          return true;
        case ISD.SRA:
          res[1] = dag.getNode(ISD.SRA, nvt, tt[1], dag.getConstant(nvtBits - 1, shTy, false));
          res[0] = dag.getNode(ISD.SRA, nvt, tt[1], amt);
          return true;
      }
    }
    return false;
  }

  private boolean expandShiftWithUnknownAmountBit(SDNode n, SDValue[] res) {
    SDValue amt = n.getOperand(1);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    EVT shTy = amt.getValueType();
    int nvtBits = nvt.getSizeInBits();
    Util.assertion(Util.isPowerOf2(nvtBits), "Expanded integer type size not a power of two!");

    SDValue[] t = getExpandedInteger(n.getOperand(0));
    SDValue nvBitsNode = dag.getConstant(nvtBits, shTy, false);
    SDValue amt2 = dag.getNode(ISD.SUB, shTy, nvBitsNode, amt);
    SDValue cmp = dag.getSetCC(new EVT(tli.getSetCCResultType(shTy)),
        amt, nvBitsNode, CondCode.SETULT);

    SDValue lo1, hi1, lo2, hi2;
    switch (n.getOpcode()) {
      case ISD.SHL:
        lo1 = dag.getConstant(0, nvt, false);
        hi1 = dag.getNode(ISD.SHL, nvt, t[0], amt);
        lo2 = dag.getNode(ISD.SHL, nvt, t[0], amt);
        hi2 = dag.getNode(ISD.OR, nvt,
            dag.getNode(ISD.SHL, nvt, t[1], amt),
            dag.getNode(ISD.SRL, nvt, t[0], amt2));
        res[0] = dag.getNode(ISD.SELECT, nvt, cmp, lo1, lo2);
        res[1] = dag.getNode(ISD.SELECT, nvt, cmp, hi1, hi2);
        return true;
      case ISD.SRL:
        hi1 = dag.getConstant(0, nvt, false);
        lo1 = dag.getNode(ISD.SRL, nvt, t[1], amt);
        hi2 = dag.getNode(ISD.SRL, nvt, t[1], amt);
        lo2 = dag.getNode(ISD.OR, nvt,
            dag.getNode(ISD.SRL, nvt, t[0], amt),
            dag.getNode(ISD.SHL, nvt, t[1], amt2));
        res[0] = dag.getNode(ISD.SELECT, nvt, cmp, lo1, lo2);
        res[1] = dag.getNode(ISD.SELECT, nvt, cmp, hi1, hi2);
        return true;
      case ISD.SRA:
        hi1 = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(nvtBits - 1, shTy, false));
        lo1 = dag.getNode(ISD.SRA, nvt, t[1], amt);

        hi2 = dag.getNode(ISD.SRA, nvt, t[1], amt);
        lo2 = dag.getNode(ISD.OR, nvt,
            dag.getNode(ISD.SRL, nvt, t[0], amt),
            dag.getNode(ISD.SHL, nvt, t[1], amt2));

        res[0] = dag.getNode(ISD.SELECT, nvt, cmp, lo1, lo2);
        res[1] = dag.getNode(ISD.SELECT, nvt, cmp, hi1, hi2);
        return true;
    }
    return false;
  }

  private boolean expandIntegerOperand(SDNode n, int opNo) {
    if (Util.DEBUG) {
      System.err.print("Expand integer operand: ");
      n.dump(dag);
      System.err.println();
    }
    SDValue res = new SDValue();

    if (customLowerNode(n, n.getOperand(opNo).getValueType(), false))
      return false;

    switch (n.getOpcode()) {
      default:
        if (Util.DEBUG) {
          System.err.printf("expandIntegerOperand op#%d: ", opNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to expand this operator's operand");
        break;
      case ISD.BIT_CONVERT:
        res = expandOp_BIT_CONVERT(n);
        break;
      case ISD.BR_CC:
        res = expandIntOpBRCC(n);
        break;
      case ISD.BUILD_VECTOR:
        res = expandIntOpBuildVector(n);
        break;
      case ISD.EXTRACT_ELEMENT:
        res = expandIntOpExtractElement(n);
        break;
      case ISD.INSERT_VECTOR_ELT:
        res = expandOp_INSERT_VECTOR_ELT(n);
        break;
      case ISD.SCALAR_TO_VECTOR:
        res = expandOp_SCALAR_TO_VECTOR(n);
        break;
      case ISD.SELECT_CC:
        res = expandIntOpSelectCC(n);
        break;
      case ISD.SETCC:
        res = expandIntOpSetCC(n);
        break;
      case ISD.SINT_TO_FP:
        res = expandIntOpSINTToFP(n);
        break;
      case ISD.STORE:
        res = expandIntOpStore((StoreSDNode) n, opNo);
        break;
      case ISD.TRUNCATE:
        res = expandIntOpTruncate(n);
        break;
      case ISD.UINT_TO_FP:
        res = expandIntOpUINTToFP(n);
        break;
      case ISD.SHL:
      case ISD.SRA:
      case ISD.SRL:
      case ISD.ROTL:
      case ISD.ROTR:
        res = expandIntOpShift(n);
        break;
    }
    if (res.getNode() == null) return false;

    if (res.getNode().equals(n))
      return true;

    Util.assertion(res.getValueType().equals(n.getValueType(0)) && n.getNumValues() == 1, "Invalid operand expansion!");

    replaceValueWith(new SDValue(n, 0), res);
    return false;
  }

  private SDValue expandIntOpBitConvert(SDNode n) {
    Util.assertion(false, "Unimplemented!");
    return null;
  }

  private SDValue expandIntOpBRCC(SDNode n) {
    SDValue newLHS = n.getOperand(2);
    SDValue newRHS = n.getOperand(3);
    CondCode cc = ((CondCodeSDNode) n.getOperand(1).getNode()).getCondition();
    SDValue[] t = integerExpandSetCCOperands(newLHS, newRHS, cc);
    newLHS = t[0];
    newRHS = t[1];
    if (newRHS.getNode() == null) {
      newRHS = dag.getConstant(0, newLHS.getValueType(), false);
      cc = CondCode.SETNE;
    }
    return dag.updateNodeOperands(new SDValue(n, 0), n.getOperand(0),
        dag.getCondCode(cc), newLHS, newRHS, n.getOperand(4));
  }

  private SDValue expandIntOpBuildVector(SDNode n) {
    // The vector type is legal but the element type needs expansion.
    EVT vecVT = n.getValueType(0);
    int numElts = vecVT.getVectorNumElements();
    EVT oldVT = n.getOperand(0).getValueType();
    EVT newVT = tli.getTypeToTransformTo(dag.getContext(), oldVT);
    Util.assertion(oldVT.equals(vecVT.getVectorElementType()),
        "BUILD_VECTOR operand type doesn't match vector element type!");

    // Build a vector of twice the length out of the expanded elements.
    // For example <3 x i64> -> <6 x i32>.
    SDValue[] newElts = new SDValue[numElts*2];
    for (int i = 0; i < numElts; i++) {
      SDValue[] res = getExpandedOp(n.getOperand(i));
      if (tli.isBigEndian())
        Util.reverse(res);

      System.arraycopy(res, 0, newElts, i, 2);
    }

    SDValue newVec = dag.getNode(ISD.BUILD_VECTOR,
        EVT.getVectorVT(dag.getContext(), newVT, newElts.length),
        newElts);
    // convert the new vector to the old vector type.
    return dag.getNode(ISD.BIT_CONVERT, vecVT, newVec);
  }

  private SDValue expandIntOpExtractElement(SDNode n) {
    SDValue[] res = getExpandedOp(n.getOperand(0));
    return ((ConstantSDNode)n.getOperand(1).getNode()).getZExtValue() != 0 ? res[1] : res[0];
  }

  private SDValue expandIntOpSelectCC(SDNode n) {
    SDValue newLHS = n.getOperand(0);
    SDValue newRHS = n.getOperand(1);
    CondCode cc = ((CondCodeSDNode) n.getOperand(4).getNode()).getCondition();
    SDValue[] t = integerExpandSetCCOperands(newLHS, newRHS, cc);
    newLHS = t[0];
    newRHS = t[1];
    if (newRHS.getNode() == null) {
      newRHS = dag.getConstant(0, newLHS.getValueType(), false);
      cc = CondCode.SETNE;
    }
    return dag.updateNodeOperands(new SDValue(n, 0),
        newLHS, newRHS, n.getOperand(2), n.getOperand(3),
        dag.getCondCode(cc));
  }

  private SDValue expandIntOpSetCC(SDNode n) {
    SDValue newLHS = n.getOperand(0);
    SDValue newRHS = n.getOperand(1);
    CondCode cc = ((CondCodeSDNode) n.getOperand(2).getNode()).getCondition();
    SDValue[] t = integerExpandSetCCOperands(newLHS, newRHS, cc);
    newLHS = t[0];
    newRHS = t[1];
    if (newRHS.getNode() == null) {
      Util.assertion(newLHS.getValueType().equals(n.getValueType(0)), "Unexected setcc expansion!");
      return newLHS;
    }
    return dag.updateNodeOperands(new SDValue(n, 0),
        newLHS, newRHS, dag.getCondCode(cc));
  }

  private SDValue expandIntOpShift(SDNode n) {
    SDValue[] t = getExpandedInteger(n.getOperand(1));
    return dag.updateNodeOperands(new SDValue(n, 0), n.getOperand(0),
        t[0]);
  }

  private SDValue expandIntOpSINTToFP(SDNode n) {
    SDValue op = n.getOperand(0);
    EVT destVT = n.getValueType(0);
    RTLIB lc = tli.getSINTTOFP(op.getValueType(), destVT);
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL);
    return makeLibCall(lc, destVT, new SDValue[]{op}, true);
  }

  private SDValue expandIntOpStore(StoreSDNode n, int opNo) {
    if (n.isNormalStore())
      return expandOp_NormalStore(n, opNo);

    Util.assertion(n.isUNINDEXEDStore(), "Indexed store during type legalization?");
    Util.assertion(opNo == 1, "Can only expand the stored value so far?");

    EVT vt = n.getOperand(1).getValueType();
    EVT evt = tli.getTypeToTransformTo(dag.getContext(), vt);
    SDValue ch = n.getChain();
    SDValue ptr = n.getBasePtr();
    int svOffset = n.getSrcValueOffset();
    int alignment = n.getAlignment();
    boolean isVolatile = n.isVolatile();
    SDValue lo, hi;

    Util.assertion(evt.isByteSized(), "Expanded type not byte sized!");
    if (n.getMemoryVT().bitsLE(evt)) {
      SDValue[] t = getExpandedInteger(n.getValue());
      lo = t[0];
      hi = t[1];
      return dag.getTruncStore(ch, lo, ptr, n.getSrcValue(), svOffset, n.getMemoryVT(), isVolatile, alignment);
    } else if (tli.isLittleEndian()) {
      SDValue[] t = getExpandedInteger(n.getValue());
      lo = t[0];
      hi = t[1];
      lo = dag.getStore(ch, lo, ptr, n.getSrcValue(), svOffset,
          isVolatile, alignment);
      int excessBit = n.getMemoryVT().getSizeInBits() - evt.getSizeInBits();
      EVT nevt = EVT.getIntegerVT(dag.getContext(), excessBit);

      int incrementSize = evt.getSizeInBits() / 8;
      ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
          dag.getIntPtrConstant(incrementSize));
      hi = dag.getTruncStore(ch, hi, ptr, n.getSrcValue(),
          svOffset + incrementSize, nevt,
          isVolatile, Util.minAlign(alignment, incrementSize));
      return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo, hi);
    } else {
      SDValue[] t = getExpandedInteger(n.getValue());
      lo = t[0];
      hi = t[1];
      EVT extVT = n.getMemoryVT();
      int bytes = extVT.getStoreSizeInBits() / 8;
      int incrementSize = evt.getSizeInBits() / 8;
      int excessBits = (bytes - incrementSize) * 8;
      EVT hiVT = EVT.getIntegerVT(dag.getContext(), extVT.getSizeInBits() - excessBits);

      if (excessBits < evt.getSizeInBits()) {
        hi = dag.getNode(ISD.SHL, evt, hi, dag.getConstant(evt.getSizeInBits()
            - excessBits, new EVT(tli.getPointerTy()), false));
        hi = dag.getNode(ISD.OR, evt, hi, dag.getNode(ISD.SRL,
            evt, lo, dag.getConstant(excessBits, new EVT(tli.getPointerTy()), false)));
      }

      hi = dag.getTruncStore(ch, hi, ptr, n.getSrcValue(), svOffset, hiVT, isVolatile, alignment);
      ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr, dag.getIntPtrConstant(incrementSize));
      lo = dag.getTruncStore(ch, lo, ptr, n.getSrcValue(), svOffset + incrementSize,
          EVT.getIntegerVT(dag.getContext(), excessBits), isVolatile, Util.minAlign(alignment, incrementSize));
      return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo, hi);
    }
  }

  private SDValue expandIntOpTruncate(SDNode n) {
    SDValue[] t = getExpandedInteger(n.getOperand(0));
    return dag.getNode(ISD.TRUNCATE, n.getValueType(0), t[0]);
  }

  private SDValue expandIntOpUINTToFP(SDNode n) {
    SDValue op = n.getOperand(0);
    EVT srcVT = op.getValueType();
    EVT destVT = n.getValueType(0);

    if (tli.getOperationAction(ISD.SINT_TO_FP, srcVT) == Custom) {
      SDValue signedConv = dag.getNode(ISD.SINT_TO_FP, destVT, op);
      signedConv = tli.lowerOperation(signedConv, dag);

      long f32TwoE32 = 0x4F800000L;
      long f32TwoE64 = 0x5F800000L;
      long f32TwoE128 = 0x7F800000L;
      APInt ff = new APInt(32, 0);
      if (srcVT.getSimpleVT().simpleVT == MVT.i32)
        ff = new APInt(32, f32TwoE32);
      else if (srcVT.getSimpleVT().simpleVT == MVT.i64)
        ff = new APInt(32, f32TwoE64);
      else if (srcVT.getSimpleVT().simpleVT == MVT.i128)
        ff = new APInt(32, f32TwoE128);
      else
        Util.assertion(false, "Unsupported UINT_TO_FP!");

      SDValue lo, hi;
      SDValue[] t = getExpandedInteger(op);
      lo = t[0];
      hi = t[1];
      SDValue signSet = dag.getSetCC(new EVT(tli.getSetCCResultType(hi.getValueType())),
          hi, dag.getConstant(0, hi.getValueType(), false),
          CondCode.SETLT);
      SDValue fudgePtr = dag.getConstantPool(ConstantInt.get(dag.getContext(), ff.zext(64)),
          new EVT(tli.getPointerTy()), 0, 0, false, 0);

      SDValue zero = dag.getIntPtrConstant(0);
      SDValue four = dag.getIntPtrConstant(4);
      if (tli.isBigEndian()) {
        SDValue tt = zero;
        zero = four;
        four = tt;
      }
      SDValue offset = dag.getNode(ISD.SELECT, zero.getValueType(), signSet,
          zero, four);
      int alignment = ((ConstantPoolSDNode) fudgePtr.getNode()).getAlignment();
      fudgePtr = dag.getNode(ISD.ADD, new EVT(tli.getPointerTy()), fudgePtr, offset);
      alignment = Math.min(alignment, 4);

      SDValue fudge = dag.getExtLoad(LoadExtType.EXTLOAD, destVT,
          dag.getEntryNode(), fudgePtr, null, 0, new EVT(MVT.f32),
          false, alignment);
      return dag.getNode(ISD.FADD, destVT, signedConv, fudge);
    }
    RTLIB lc = tli.getUINTTOFP(srcVT, destVT);
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Don't know how to expand this UINT_TO_FP!");
    return makeLibCall(lc, destVT, new SDValue[]{op}, true);
  }

  private SDValue[] integerExpandSetCCOperands(SDValue newLHS, SDValue newRHS, CondCode cc) {
    SDValue[] lhsT, rhsT;
    lhsT = getExpandedInteger(newLHS);
    rhsT = getExpandedInteger(newRHS);

    SDValue lhslo = lhsT[0], lhshi = lhsT[1], rhslo = rhsT[0], rhshi = rhsT[1];
    EVT vt = newLHS.getValueType();
    if (cc == CondCode.SETEQ || cc == CondCode.SETNE) {
      if (rhslo.equals(rhshi)) {
        if (rhslo.getNode() instanceof ConstantSDNode) {
          ConstantSDNode csd = (ConstantSDNode) rhslo.getNode();
          if (csd.isAllOnesValue()) {
            newLHS = dag.getNode(ISD.AND, lhslo.getValueType(), lhslo, lhshi);
            newRHS = rhslo;
            return new SDValue[]{newLHS, newRHS};
          }
        }
      }

      newLHS = dag.getNode(ISD.XOR, lhslo.getValueType(), lhslo, rhslo);
      newRHS = dag.getNode(ISD.XOR, lhslo.getValueType(), lhshi, rhshi);
      newLHS = dag.getNode(ISD.OR, newLHS.getValueType(), newLHS, newRHS);
      newRHS = dag.getConstant(0, newLHS.getValueType(), false);
      return new SDValue[]{newLHS, newRHS};
    }

    if (newRHS.getNode() instanceof ConstantSDNode) {
      ConstantSDNode csd = (ConstantSDNode) newRHS.getNode();
      if ((cc == CondCode.SETLT && csd.isNullValue()) ||
          cc == CondCode.SETGT && csd.isAllOnesValue()) {
        newLHS = lhshi;
        newRHS = rhshi;
        return new SDValue[]{newLHS, newRHS};
      }
    }

    CondCode lowCC;
    switch (cc) {
      default:
        Util.shouldNotReachHere();
      case SETLT:
      case SETULT:
        lowCC = CondCode.SETULT;
        break;
      case SETGT:
      case SETUGT:
        lowCC = CondCode.SETUGT;
        break;
      case SETLE:
      case SETULE:
        lowCC = SETULE;
        break;
      case SETGE:
      case SETUGE:
        lowCC = SETUGE;
        break;
    }
    DAGCombinerInfo dagCombinerInfo = new DAGCombinerInfo(dag, false, true, true, null);
    SDValue temp1, temp2;
    temp1 = tli.simplifySetCC(new EVT(tli.getSetCCResultType(lhslo.getValueType())),
        lhslo, rhslo, lowCC, false, dagCombinerInfo);
    if (temp1.getNode() == null)
      temp1 = dag.getSetCC(new EVT(tli.getSetCCResultType(lhslo.getValueType())),
          lhslo, rhslo, lowCC);
    temp2 = tli.simplifySetCC(new EVT(tli.getSetCCResultType(lhshi.getValueType())),
        lhshi, rhshi, lowCC, false, dagCombinerInfo);
    if (temp2.getNode() == null)
      temp2 = dag.getNode(ISD.SETCC, new EVT(tli.getSetCCResultType(lhshi.getValueType())),
          lhshi, rhshi, dag.getCondCode(cc));

    ConstantSDNode temp1C = temp1.getNode() instanceof ConstantSDNode ? (ConstantSDNode) temp1.getNode() : null;
    ConstantSDNode temp2C = temp2.getNode() instanceof ConstantSDNode ? (ConstantSDNode) temp2.getNode() : null;
    if ((temp1C != null && temp1C.isNullValue()) ||
        (temp2C != null && temp2C.isNullValue() &&
            (cc == SETLE || cc == SETGE || cc == SETUGE || cc == SETULE)) ||
        (temp2C != null && temp2C.getAPIntValue().eq(1) &&
            (cc == SETLT || cc == SETGT || cc == SETUGT || cc == SETULT))) {
      newLHS = temp2;
      newRHS = new SDValue();
      return new SDValue[]{newLHS, newRHS};
    }

    newLHS = tli.simplifySetCC(new EVT(tli.getSetCCResultType(lhshi.getValueType())),
        lhshi, rhshi, SETEQ, false, dagCombinerInfo);

    if (newLHS.getNode() == null)
      newLHS = dag.getSetCC(new EVT(tli.getSetCCResultType(lhshi.getValueType())),
          lhshi, rhshi, SETEQ);
    newLHS = dag.getNode(ISD.SELECT, temp1.getValueType(), newLHS, temp1, temp2);
    newRHS = new SDValue();
    return new SDValue[]{newLHS, newRHS};
  }

  private SDValue getSoftenedFloat(SDValue op) {
    if (!softenedFloats.containsKey(op))
      softenedFloats.put(op, new SDValue());
    SDValue softenedOp = softenedFloats.get(op);
    softenedOp = remapValue(softenedOp);
    softenedFloats.put(op, softenedOp);
    Util.assertion(softenedOp.getNode() != null, "Operand wasn't converted to integer?");
    return softenedOp;
  }

  private void setSoftenedFloat(SDValue op, SDValue result) {
    Util.assertion(result.getValueType().equals(tli.getTypeToTransformTo(dag.getContext(), op.getValueType())));
    result = analyzeNewValue(result);

    Util.assertion(!softenedFloats.containsKey(op), "Node already converted to integer!");
    softenedFloats.put(op, result);
  }

  private void softenFloatResult(SDNode n, int resNo) {
    if (Util.DEBUG) {
      System.err.printf("Soften float result: %d: ", resNo);
      n.dump(dag);
      System.err.println();
    }
    SDValue res = new SDValue();
    switch (n.getOpcode()) {
      default:
        if (Util.DEBUG) {
          System.err.printf("softenFloatResult #%d: ", resNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to soften the result of this oeprator!");
        break;
      case ISD.BIT_CONVERT:
        res = softenFloatRes_BIT_CONVERT(n);
        break;
      case ISD.BUILD_PAIR:
        res = softenFloatRes_BUILD_PAIR(n);
        break;
      case ISD.ConstantFP:
        res = softenFloatRes_ConstantFP((ConstantFPSDNode) n);
        break;
      case ISD.EXTRACT_VECTOR_ELT:
        res = softenFloatRes_EXTRACT_VECTOR_ELT(n);
        break;
      case ISD.FABS:
        res = softenFloatRes_FABS(n);
        break;
      case ISD.FADD:
        res = softenFloatRes_FADD(n);
        break;
      case ISD.FCEIL:
        res = softenFloatRes_FCEIL(n);
        break;
      case ISD.FCOPYSIGN:
        res = softenFloatRes_FCOPYSIGN(n);
        break;
      case ISD.FCOS:
        res = softenFloatRes_FCOS(n);
        break;
      case ISD.FDIV:
        res = softenFloatRes_FDIV(n);
        break;
      case ISD.FEXP:
        res = softenFloatRes_FEXP(n);
        break;
      case ISD.FEXP2:
        res = softenFloatRes_FEXP2(n);
        break;
      case ISD.FFLOOR:
        res = softenFloatRes_FFLOOR(n);
        break;
      case ISD.FLOG:
        res = softenFloatRes_FLOG(n);
        break;
      case ISD.FLOG2:
        res = softenFloatRes_FLOG2(n);
        break;
      case ISD.FLOG10:
        res = softenFloatRes_FLOG10(n);
        break;
      case ISD.FMUL:
        res = softenFloatRes_FMUL(n);
        break;
      case ISD.FNEARBYINT:
        res = softenFloatRes_FNEARBYINT(n);
        break;
      case ISD.FNEG:
        res = softenFloatRes_FNEG(n);
        break;
      case ISD.FP_EXTEND:
        res = softenFloatRes_FP_EXTEND(n);
        break;
      case ISD.FP_ROUND:
        res = softenFloatRes_FP_ROUND(n);
        break;
      case ISD.FPOW:
        res = softenFloatRes_FPOW(n);
        break;
      case ISD.FPOWI:
        res = softenFloatRes_FPOWI(n);
        break;
      case ISD.FREM:
        res = softenFloatRes_FREM(n);
        break;
      case ISD.FRINT:
        res = softenFloatRes_FRINT(n);
        break;
      case ISD.FSIN:
        res = softenFloatRes_FSIN(n);
        break;
      case ISD.FSQRT:
        res = softenFloatRes_FSQRT(n);
        break;
      case ISD.FSUB:
        res = softenFloatRes_FSUB(n);
        break;
      case ISD.FTRUNC:
        res = softenFloatRes_FTRUNC(n);
        break;
      case ISD.LOAD:
        res = softenFloatRes_LOAD(n);
        break;
      case ISD.SELECT:
        res = softenFloatRes_SELECT(n);
        break;
      case ISD.SELECT_CC:
        res = softenFloatRes_SELECT_CC(n);
        break;
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
        res = softenFloatRes_XINT_TO_FP(n);
        break;
      case ISD.UNDEF:
        res = softenFloatRes_UNDEF(n);
        break;
      case ISD.VAARG:
        res = softenFloatRes_VAARG(n);
        break;
    }
    if (res.getNode() != null)
      setSoftenedFloat(new SDValue(n, resNo), res);
  }

  private SDValue softenFloatRes_BIT_CONVERT(SDNode n) {
    return bitConvertToInteger(n.getOperand(0));
  }

  private SDValue softenFloatRes_BUILD_PAIR(SDNode n) {
    return dag.getNode(ISD.BUILD_PAIR,
        tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0)),
        bitConvertToInteger(n.getOperand(0)),
        bitConvertToInteger(n.getOperand(1)));
  }

  private SDValue softenFloatRes_ConstantFP(ConstantFPSDNode n) {
    return dag.getConstant(n.getValueAPF().bitcastToAPInt(),
        tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0)), false);
  }

  private SDValue softenFloatRes_EXTRACT_VECTOR_ELT(SDNode n) {
    SDValue newOP = bitConvertVectorToIntegerVector(n.getOperand(0));
    return dag.getNode(ISD.EXTRACT_VECTOR_ELT,
        newOP.getValueType().getVectorElementType(),
        newOP, n.getOperand(1));
  }

  private SDValue softenFloatRes_FABS(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    int size = nvt.getSizeInBits();

    SDValue mask = dag.getConstant(APInt.getAllOnesValue(size).clear(size - 1),
        nvt, false);
    SDValue op = getSoftenedFloat(n.getOperand(0));
    return dag.getNode(ISD.AND, nvt, op, mask);
  }

  public static RTLIB getFPLibCall(EVT vt, RTLIB callF32,
                                   RTLIB callF64, RTLIB callF80,
                                   RTLIB callPPCF128) {
    int simpleVT = vt.getSimpleVT().simpleVT;
    RTLIB[] libs = {callF32, callF64, callF80, callPPCF128};
    if (simpleVT >= MVT.f32 && simpleVT <= MVT.ppcf128)
      return libs[simpleVT - MVT.f32];
    return RTLIB.UNKNOWN_LIBCALL;
  }

  private SDValue softenFloatRes_FADD(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(0)),
        getSoftenedFloat(n.getOperand(1))};
    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.ADD_F32,
        RTLIB.ADD_F64,
        RTLIB.ADD_F80,
        RTLIB.ADD_PPCF128),
        nvt, ops, false);
  }

  private SDValue softenFloatRes_FCEIL(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.CEIL_F32,
        RTLIB.CEIL_F64,
        RTLIB.CEIL_F80,
        RTLIB.CEIL_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FCOPYSIGN(SDNode n) {
    SDValue lhs = getSoftenedFloat(n.getOperand(0));
    SDValue rhs = getSoftenedFloat(n.getOperand(0));

    EVT lvt = lhs.getValueType();
    EVT rvt = rhs.getValueType();

    int lsize = lvt.getSizeInBits();
    int rsize = rvt.getSizeInBits();
    SDValue signedBit = dag.getNode(ISD.SHL, rvt, dag.getConstant(1, rvt, false),
        dag.getConstant(rsize - 1, new EVT(tli.getShiftAmountTy()), false));
    signedBit = dag.getNode(ISD.AND, rvt, rhs, signedBit);

    int sizeDiff = rvt.getSizeInBits() - lvt.getSizeInBits();
    if (sizeDiff > 0) {
      signedBit = dag.getNode(ISD.SRL, rvt, signedBit,
          dag.getConstant(sizeDiff, new EVT(tli.getShiftAmountTy()), false));
      signedBit = dag.getNode(ISD.TRUNCATE, lvt, signedBit);
    } else if (sizeDiff < 0) {
      signedBit = dag.getNode(ISD.ANY_EXTEND, lvt, signedBit);
      signedBit = dag.getNode(ISD.SHL, lvt, signedBit, dag.getNode(-sizeDiff,
          new EVT(tli.getShiftAmountTy())));
    }

    SDValue mask = dag.getNode(ISD.SHL, lvt, dag.getConstant(1, lvt, false),
        dag.getConstant(lsize - 1, new EVT(tli.getShiftAmountTy()), false));
    mask = dag.getNode(ISD.SUB, lvt, mask, dag.getConstant(1, lvt, false));
    lhs = dag.getNode(ISD.AND, lvt, lhs, mask);
    return dag.getNode(ISD.OR, lvt, lhs, signedBit);
  }

  private SDValue softenFloatRes_FCOS(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));
    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.COS_F32,
        RTLIB.COS_F64,
        RTLIB.COS_F80,
        RTLIB.COS_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FDIV(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(0)),
        getSoftenedFloat(n.getOperand(1))};
    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.DIV_F32,
        RTLIB.DIV_F64,
        RTLIB.DIV_F80,
        RTLIB.DIV_PPCF128),
        nvt, ops, false);
  }

  private SDValue softenFloatRes_FEXP(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.EXP_F32,
        RTLIB.EXP_F64,
        RTLIB.EXP_F80,
        RTLIB.EXP_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FEXP2(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.EXP2_F32,
        RTLIB.EXP2_F64,
        RTLIB.EXP2_F80,
        RTLIB.EXP2_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FFLOOR(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.FLOOR_F32,
        RTLIB.FLOOR_F64,
        RTLIB.FLOOR_F80,
        RTLIB.FLOOR_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FLOG(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.LOG_F32,
        RTLIB.LOG_F64,
        RTLIB.LOG_F80,
        RTLIB.LOG_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FLOG2(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.LOG2_F32,
        RTLIB.LOG2_F64,
        RTLIB.LOG2_F80,
        RTLIB.LOG2_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FLOG10(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.LOG10_F32,
        RTLIB.LOG10_F64,
        RTLIB.LOG10_F80,
        RTLIB.LOG10_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FMUL(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(0)),
        getSoftenedFloat(n.getOperand(1))};
    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.MUL_F32,
        RTLIB.MUL_F64,
        RTLIB.MUL_F80,
        RTLIB.MUL_PPCF128),
        nvt, ops, false);
  }

  private SDValue softenFloatRes_FNEARBYINT(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = getSoftenedFloat(n.getOperand(0));

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.NEARBYINT_F32,
        RTLIB.NEARBYINT_F64,
        RTLIB.NEARBYINT_F80,
        RTLIB.NEARBYINT_PPCF128),
        nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FNEG(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {dag.getConstantFP(-0.0, n.getValueType(0), false),
        getSoftenedFloat(n.getOperand(0))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.SUB_F32,
        RTLIB.SUB_F64,
        RTLIB.SUB_F80,
        RTLIB.SUB_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_FP_EXTEND(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = n.getOperand(0);
    RTLIB lc = tli.getFPEXT(op.getValueType(), n.getValueType(0));
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL);
    return makeLibCall(lc, nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FP_ROUND(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue op = n.getOperand(0);
    RTLIB lc = tli.getFPROUND(op.getValueType(), n.getValueType(0));
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL);
    return makeLibCall(lc, nvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatRes_FPOW(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(1)),
        getSoftenedFloat(n.getOperand(0))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.POW_F32,
        RTLIB.POW_F64,
        RTLIB.POW_F80,
        RTLIB.POW_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_FPOWI(SDNode n) {
    Util.assertion(n.getOperand(1).getValueType().getSimpleVT().simpleVT == MVT.i32);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(1)),
        getSoftenedFloat(n.getOperand(0))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.POWI_F32,
        RTLIB.POWI_F64,
        RTLIB.POWI_F80,
        RTLIB.POWI_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_FREM(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(1)),
        getSoftenedFloat(n.getOperand(0))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.REM_F32,
        RTLIB.REM_F64,
        RTLIB.REM_F80,
        RTLIB.REM_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_FRINT(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(1))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.RINT_F32,
        RTLIB.RINT_F64,
        RTLIB.RINT_F80,
        RTLIB.RINT_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_FSIN(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(1))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.SIN_F32,
        RTLIB.SIN_F64,
        RTLIB.SIN_F80,
        RTLIB.SIN_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_FSQRT(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(1))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.SQRT_F32,
        RTLIB.SQRT_F64,
        RTLIB.SQRT_F80,
        RTLIB.SQRT_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_FSUB(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(1)),
        getSoftenedFloat(n.getOperand(0))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.SUB_F32,
        RTLIB.SUB_F64,
        RTLIB.SUB_F80,
        RTLIB.SUB_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_FTRUNC(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue[] ops = {getSoftenedFloat(n.getOperand(1))};

    return makeLibCall(getFPLibCall(n.getValueType(0),
        RTLIB.TRUNC_F32,
        RTLIB.TRUNC_F64,
        RTLIB.TRUNC_F80,
        RTLIB.TRUNC_PPCF128),
        nvt,
        ops, false);
  }

  private SDValue softenFloatRes_LOAD(SDNode n) {
    LoadSDNode ld = (LoadSDNode) n;
    EVT vt = n.getValueType(0);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), vt);

    SDValue newL = new SDValue();
    if (ld.getExtensionType() == LoadExtType.NON_EXTLOAD) {
      newL = dag.getLoad(ld.getAddressingMode(), ld.getExtensionType(),
          nvt, ld.getChain(), ld.getBasePtr(), ld.getOffset(),
          ld.getSrcValue(), ld.getSrcValueOffset(),
          nvt, ld.isVolatile(), ld.getAlignment());
      replaceValueWith(new SDValue(n, 1), newL.getValue(1));
      return newL;
    }

    newL = dag.getLoad(ld.getAddressingMode(), LoadExtType.NON_EXTLOAD,
        ld.getMemoryVT(), ld.getChain(), ld.getBasePtr(),
        ld.getOffset(), ld.getSrcValue(), ld.getSrcValueOffset(),
        ld.getMemoryVT(), ld.isVolatile(), ld.getAlignment());
    replaceValueWith(new SDValue(n, 1), newL.getValue(1));
    return bitConvertToInteger(dag.getNode(ISD.FP_ROUND, vt, newL));
  }

  private SDValue softenFloatRes_SELECT(SDNode n) {
    SDValue lhs = getSoftenedFloat(n.getOperand(1));
    SDValue rhs = getSoftenedFloat(n.getOperand(2));
    return dag.getNode(ISD.SELECT, lhs.getValueType(), n.getOperand(0), lhs, rhs);
  }

  private SDValue softenFloatRes_SELECT_CC(SDNode n) {
    SDValue lhs = getSoftenedFloat(n.getOperand(2));
    SDValue rhs = getSoftenedFloat(n.getOperand(3));
    return dag.getNode(ISD.SELECT_CC, lhs.getValueType(),
        n.getOperand(0), n.getOperand(1), lhs, rhs,
        n.getOperand(4));
  }

  private SDValue softenFloatRes_UNDEF(SDNode n) {
    return dag.getUNDEF(tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0)));
  }

  private SDValue softenFloatRes_VAARG(SDNode n) {
    SDValue chain = n.getOperand(0);
    SDValue ptr = n.getOperand(1);
    EVT vt = n.getValueType(0);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), vt);
    SDValue newVAARG = dag.getVAArg(nvt, chain, ptr, n.getOperand(2));

    replaceValueWith(new SDValue(n, 1), newVAARG.getValue(1));
    return newVAARG;
  }

  private SDValue softenFloatRes_XINT_TO_FP(SDNode n) {
    boolean signed = n.getOpcode() == ISD.SINT_TO_FP;
    EVT svt = n.getOperand(0).getValueType();
    EVT rvt = n.getValueType(0);
    EVT nvt = new EVT();

    RTLIB lib = RTLIB.UNKNOWN_LIBCALL;
    for (int t = MVT.FIRST_INTEGER_VALUETYPE;
         t <= MVT.LAST_INTEGER_VALUETYPE && lib == RTLIB.UNKNOWN_LIBCALL;
         ++t) {
      nvt = new EVT(t);
      if (nvt.bitsGE(svt))
        lib = signed ? tli.getSINTTOFP(nvt, rvt) : tli.getUINTTOFP(nvt, rvt);
    }
    Util.assertion(lib != RTLIB.UNKNOWN_LIBCALL);
    SDValue op = dag.getNode(signed ? ISD.SIGN_EXTEND : ISD.ZERO_EXTEND,
        nvt, n.getOperand(0));
    return makeLibCall(lib, tli.getTypeToTransformTo(dag.getContext(), rvt),
        new SDValue[]{op}, false);
  }

  private boolean softenFloatOperand(SDNode n, int opNo) {
    if (Util.DEBUG) {
      System.err.printf("Soften float opereand %d: ", opNo);
      n.dump(dag);
      System.err.println();
    }

    SDValue res = new SDValue();
    switch (n.getOpcode()) {
      default:
        Util.shouldNotReachHere("Don't know how to soften this operator's operand!");
        break;
      case ISD.BIT_CONVERT:
        res = softenFloatOp_BIT_CONVERT(n);
        break;
      case ISD.BR_CC:
        res = softenFloatOp_BR_CC(n);
        break;
      case ISD.FP_ROUND:
        res = softenFloatOp_FP_ROUND(n);
        break;
      case ISD.FP_TO_SINT:
        res = softenFloatOp_FP_TO_SINT(n);
        break;
      case ISD.FP_TO_UINT:
        res = softenFloatOp_FP_TO_UINT(n);
        break;
      case ISD.SELECT_CC:
        res = softenFloatOp_SELECT_CC(n);
        break;
      case ISD.SETCC:
        res = softenFloatOp_SETCC(n);
        break;
      case ISD.STORE:
        res = softenFloatOp_STORE(n, opNo);
        break;
    }
    if (res.getNode() == null)
      return false;

    if (Objects.equals(res.getNode(), n))
      return true;

    Util.assertion(res.getValueType().equals(n.getValueType(0)) && n.getNumValues() == 1, "Invalid operand expansion!");


    replaceValueWith(new SDValue(n, 0), res);
    return false;
  }

  private SDValue softenFloatOp_BIT_CONVERT(SDNode n) {
    return dag.getNode(ISD.BIT_CONVERT, n.getValueType(0),
        getSoftenedFloat(n.getOperand(0)));
  }

  private SDValue softenFloatOp_BR_CC(SDNode n) {
    SDValue newLHS = n.getOperand(2), newRHS = n.getOperand(3);
    CondCode cc = ((CondCodeSDNode) (n.getOperand(1).getNode())).getCondition();
    SDValue[] res = softenSetCCOperands(newLHS, newRHS, cc);
    newLHS = res[0];
    newRHS = res[1];

    if (newRHS.getNode() == null) {
      newRHS = dag.getConstant(0, newLHS.getValueType(), false);
      cc = SETNE;
    }
    return dag.updateNodeOperands(new SDValue(n, 0), n.getOperand(0),
        dag.getCondCode(cc), newLHS, newRHS, n.getOperand(4));
  }

  private SDValue softenFloatOp_FP_ROUND(SDNode n) {
    EVT svt = n.getOperand(0).getValueType();
    EVT rvt = n.getValueType(0);

    RTLIB lc = tli.getFPROUND(svt, rvt);
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "unsupported FP_ROUND libcall!");

    SDValue op = getSoftenedFloat(n.getOperand(0));
    return makeLibCall(lc, rvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatOp_FP_TO_SINT(SDNode n) {
    EVT rvt = n.getValueType(0);
    RTLIB lc = tli.getFPTOSINT(n.getOperand(0).getValueType(), rvt);
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported FP_TO_SINT");
    SDValue op = getSoftenedFloat(n.getOperand(0));
    return makeLibCall(lc, rvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatOp_FP_TO_UINT(SDNode n) {
    EVT rvt = n.getValueType(0);
    RTLIB lc = tli.getFPTOUINT(n.getOperand(0).getValueType(), rvt);
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported FP_TO_UINT");
    SDValue op = getSoftenedFloat(n.getOperand(0));
    return makeLibCall(lc, rvt, new SDValue[]{op}, false);
  }

  private SDValue softenFloatOp_SELECT_CC(SDNode n) {
    SDValue newLHS = n.getOperand(0), newRHS = n.getOperand(1);
    CondCode cc = ((CondCodeSDNode) (n.getOperand(4).getNode())).getCondition();
    SDValue[] res = softenSetCCOperands(newLHS, newRHS, cc);
    newLHS = res[0];
    newRHS = res[1];

    if (newRHS.getNode() == null) {
      newRHS = dag.getConstant(0, newLHS.getValueType(), false);
      cc = SETNE;
    }
    return dag.updateNodeOperands(new SDValue(n, 0),
        newLHS, newRHS,
        n.getOperand(2), n.getOperand(3),
        dag.getCondCode(cc));
  }

  private SDValue softenFloatOp_SETCC(SDNode n) {
    SDValue newLHS = n.getOperand(0), newRHS = n.getOperand(1);
    CondCode cc = ((CondCodeSDNode) (n.getOperand(2).getNode())).getCondition();
    SDValue[] res = softenSetCCOperands(newLHS, newRHS, cc);
    newLHS = res[0];
    newRHS = res[1];

    if (newRHS.getNode() == null) {
      Util.assertion(newLHS.getValueType().equals(n.getValueType(0)), "Unexpeted setcc expansion!");

      return newLHS;
    }

    return dag.updateNodeOperands(new SDValue(n, 0),
        newLHS, newRHS,
        dag.getCondCode(cc));
  }

  private SDValue softenFloatOp_STORE(SDNode n, int opNo) {
    Util.assertion(n.isUNINDEXEDStore(), "Indexed store during type legalization!");
    Util.assertion(opNo == 1, "Can only soften the stored value!");
    StoreSDNode st = (StoreSDNode) n;
    SDValue val = st.getValue();

    if (st.isTruncatingStore())
      val = bitConvertToInteger(dag.getNode(ISD.FP_ROUND, st.getMemoryVT(),
          val, dag.getIntPtrConstant(0)));
    else
      val = getSoftenedFloat(val);

    return dag.getStore(st.getChain(), val, st.getBasePtr(),
        st.getSrcValue(), st.getSrcValueOffset(),
        st.isVolatile(), st.getAlignment());
  }

  private SDValue[] softenSetCCOperands(SDValue newLHS, SDValue newRHS, CondCode cc) {
    SDValue lhsInt = getSoftenedFloat(newLHS);
    SDValue rhsInt = getSoftenedFloat(newRHS);
    EVT vt = newLHS.getValueType();

    boolean isF32 = vt.getSimpleVT().simpleVT == MVT.f32;
    boolean isF64 = vt.getSimpleVT().simpleVT == MVT.f64;
    Util.assertion(isF32 || isF64, "Unsupported setcc type!");

    RTLIB libCall = RTLIB.UNKNOWN_LIBCALL, libCall2 = RTLIB.UNKNOWN_LIBCALL;
    switch (cc) {
      case SETEQ:
      case SETOEQ:
        libCall = isF32 ? RTLIB.OEQ_F32 : RTLIB.OEQ_F64;
        break;
      case SETNE:
      case SETUNE:
        libCall = isF32 ? RTLIB.UNE_F32 : RTLIB.UNE_F64;
        break;
      case SETGE:
      case SETOGE:
        libCall = isF32 ? RTLIB.OGE_F32 : RTLIB.OGE_F64;
        break;
      case SETLT:
      case SETOLT:
        libCall = isF32 ? RTLIB.OLT_F32 : RTLIB.OLT_F64;
        break;
      case SETLE:
      case SETOLE:
        libCall = isF32 ? RTLIB.OLE_F32 : RTLIB.OLE_F64;
        break;
      case SETGT:
      case SETOGT:
        libCall = isF32 ? RTLIB.OGT_F32 : RTLIB.OGT_F64;
        break;
      case SETUO:
        libCall = isF32 ? RTLIB.UO_F32 : RTLIB.UO_F64;
        break;
      case SETO:
        libCall = isF32 ? RTLIB.O_F32 : RTLIB.O_F64;
        break;
      default:
        libCall = isF32 ? RTLIB.UO_F32 : RTLIB.UO_F64;
        switch (cc) {
          case SETONE:
            libCall = isF32 ? RTLIB.OLT_F32 : RTLIB.OLT_F64;
          case SETUGT:
            libCall2 = isF32 ? RTLIB.OGT_F32 : RTLIB.OGT_F64;
            break;
          case SETUGE:
            libCall2 = isF32 ? RTLIB.OGE_F32 : RTLIB.OGE_F64;
            break;
          case SETULT:
            libCall2 = isF32 ? RTLIB.OLT_F32 : RTLIB.OLT_F64;
            break;
          case SETULE:
            libCall2 = isF32 ? RTLIB.OLE_F32 : RTLIB.OLE_F64;
            break;
          case SETUEQ:
            libCall2 = isF32 ? RTLIB.OEQ_F32 : RTLIB.OEQ_F64;
            break;
          default:
            Util.assertion(false, "Don't know how to soften this setcc!");
            break;
        }
        break;
    }

    EVT retVT = new EVT(MVT.i32);
    SDValue[] ops = {lhsInt, rhsInt};
    newLHS = makeLibCall(libCall, retVT, ops, false);
    newRHS = dag.getConstant(0, retVT, false);
    cc = tli.getCmpLibCallCC(libCall);
    if (libCall2 != RTLIB.UNKNOWN_LIBCALL) {
      SDValue temp = dag.getNode(ISD.SETCC,
          new EVT(tli.getSetCCResultType(retVT)),
          newLHS, newRHS, dag.getCondCode(cc));
      newLHS = makeLibCall(libCall2, retVT, ops, false);
      newLHS = dag.getNode(ISD.SETCC, new EVT(tli.getSetCCResultType(retVT)),
          newLHS, newRHS, dag.getCondCode(tli.getCmpLibCallCC(libCall2)));
      newLHS = dag.getNode(ISD.OR, temp.getValueType(), temp, newLHS);
      newRHS = new SDValue();
    }
    return new SDValue[]{newLHS, newRHS};
  }

  private SDValue[] getExpandedFloat(SDValue op) {
    SDValue[] res = new SDValue[2];
    Util.assertion(expandedFloats.containsKey(op));
    Pair<SDValue, SDValue> entry = expandedFloats.get(op);
    Util.assertion(entry != null);
    entry.first = remapValue(entry.first);
    entry.second = remapValue(entry.second);
    Util.assertion(entry.first.getNode() != null);
    expandedFloats.put(op, entry);
    return new SDValue[]{entry.first, entry.second};
  }

  private void setExpandedFloat(SDValue op, SDValue lo, SDValue hi) {
    Util.assertion(lo.getValueType().equals(tli.getTypeToTransformTo(dag.getContext(), op.getValueType())) && hi.getValueType().equals(lo.getValueType()),
        "Invalid type for expanded float!");

    lo = analyzeNewValue(lo);
    hi = analyzeNewValue(hi);

    Util.assertion(!expandedFloats.containsKey(op));
    Pair<SDValue, SDValue> entry = Pair.get(lo, hi);
    expandedFloats.put(op, entry);
  }

  private void expandFloatResult(SDNode n, int resNo) {
    if (Util.DEBUG) {
      System.err.printf("Expand float result: %d", resNo);
      n.dump(dag);
      System.err.println();
    }

    SDValue[] res = new SDValue[2];
    if (customLowerNode(n, n.getValueType(resNo), true))
      return;
    switch (n.getOpcode()) {
      case ISD.MERGE_VALUES:
        res = splitRes_MERGE_VALUES(n);
        break;
      case ISD.UNDEF:
        res = splitRes_UNDEF(n);
        break;
      case ISD.SELECT:
        res = splitRes_SELECT(n);
        break;
      case ISD.SELECT_CC:
        res = splitRes_SELECT_CC(n);
        break;
      case ISD.BIT_CONVERT:
        res = expandRes_BIT_CONVERT(n);
        break;
      case ISD.BUILD_PAIR:
        res = expandRes_BUILD_PAIR(n);
        break;
      case ISD.EXTRACT_ELEMENT:
        res = expandRes_EXTRACT_ELEMENT(n);
        break;
      case ISD.EXTRACT_VECTOR_ELT:
        res = expandRes_EXTRACT_VECTOR_ELT(n);
        break;
      case ISD.VAARG:
        res = expandRes_VAARG(n);
        break;
      case ISD.ConstantFP:
        res = expandFloatRes_ConstantFP(n);
        break;
      case ISD.FABS:
        res = expandFloatRes_FABS(n);
        break;
      case ISD.FADD:
        res = expandFloatRes_FADD(n);
        break;
      case ISD.FCEIL:
        res = expandFloatRes_FCEIL(n);
        break;
      case ISD.FCOS:
        res = expandFloatRes_FCOS(n);
        break;
      case ISD.FDIV:
        res = expandFloatRes_FEXP(n);
        break;
      case ISD.FEXP2:
        res = expandFloatRes_FEXP2(n);
        break;
      case ISD.FFLOOR:
        res = expandFloatRes_FFLOOR(n);
        break;
      case ISD.FLOG:
        res = expandFloatRes_FLOG(n);
        break;
      case ISD.FLOG2:
        res = expandFloatRes_FLOG2(n);
        break;
      case ISD.FLOG10:
        res = expandFloatRes_FLOG10(n);
        break;
      case ISD.FMUL:
        res = expandFloatRes_FMUL(n);
        break;
      case ISD.FNEARBYINT:
        res = expandFloatRes_FNEARBYINT(n);
        break;
      case ISD.FNEG:
        res = expandFloatRes_FNEG(n);
        break;
      case ISD.FP_EXTEND:
        res = expandFloatRes_FP_EXTEND(n);
        break;
      case ISD.FPOW:
        res = expandFloatRes_FPOW(n);
        break;
      case ISD.FPOWI:
        res = expandFloatRes_FPOWI(n);
        break;
      case ISD.FRINT:
        res = expandFloatRes_FRINT(n);
        break;
      case ISD.FSIN:
        res = expandFloatRes_FSIN(n);
        break;
      case ISD.FSQRT:
        res = expandFloatRes_FSQRT(n);
        break;
      case ISD.FSUB:
        res = expandFloatRes_FSUB(n);
        break;
      case ISD.FTRUNC:
        res = expandFloatRes_FTRUNC(n);
        break;
      case ISD.LOAD:
        res = expandFloatRes_LOAD(n);
        break;
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
        res = expandFloatRes_XINT_TO_FP(n);
      default:
        if (Util.DEBUG) {
          System.err.printf("expandFloatResult #%d: ", resNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to expand the result of this operand's operator!");
        break;
    }
    if (res[0].getNode() != null)
      setExpandedFloat(new SDValue(n, resNo), res[0], res[1]);
  }

  private SDValue[] expandFloatRes_ConstantFP(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    Util.assertion(nvt.getSizeInBits() == 64, "Don't know how to expand this float constant!");

    APInt c = ((ConstantFPSDNode) n).getValueAPF().bitcastToAPInt();
    long[] val = c.getRawData();
    return new SDValue[]{dag.getConstantFP(new APFloat(new APInt(
        64, new long[]{val[1]})), nvt, false),
        dag.getConstantFP(new APFloat(new APInt(
            64, new long[]{val[0]})), nvt, false)};
  }

  private SDValue[] expandFloatRes_FABS(SDNode n) {
    Util.assertion(n.getValueType(0).getSimpleVT().simpleVT == MVT.ppcf128, "Only correct for ppcf128!");

    SDValue[] res = getExpandedFloat(n.getOperand(0));
    SDValue hi = dag.getNode(ISD.FABS, res[1].getValueType(), res[1]);
    SDValue lo = res[0];
    lo = dag.getNode(ISD.SELECT_CC, lo.getValueType(), res[1], hi, lo,
        dag.getNode(ISD.FNEG, lo.getValueType(), lo),
        dag.getCondCode(SETEQ));
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandFloatRes_FADD(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.ADD_F32, RTLIB.ADD_F64,
        RTLIB.ADD_F80, RTLIB.ADD_PPCF128);
  }

  private SDValue[] expandFloatRes_FCEIL(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.CEIL_F32, RTLIB.CEIL_F64,
        RTLIB.CEIL_F80, RTLIB.CEIL_PPCF128);
  }

  private SDValue[] expandFloatRes_FCOS(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.COS_F32, RTLIB.COS_F64,
        RTLIB.COS_F80, RTLIB.COS_PPCF128);
  }

  private SDValue[] expandFloatRes_FDIV(SDNode n) {
    return commonExpandFloatResBinary(n, RTLIB.DIV_F32, RTLIB.CEIL_F64,
        RTLIB.CEIL_F80, RTLIB.CEIL_PPCF128);
  }

  private SDValue[] expandFloatRes_FEXP(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.EXP_F32, RTLIB.EXP_F64,
        RTLIB.EXP_F80, RTLIB.EXP_PPCF128);
  }

  private SDValue[] expandFloatRes_FEXP2(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.EXP2_F32, RTLIB.EXP2_F64,
        RTLIB.EXP2_F80, RTLIB.EXP2_PPCF128);
  }

  private SDValue[] expandFloatRes_FFLOOR(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.FLOOR_F32, RTLIB.FLOOR_F64,
        RTLIB.FLOOR_F80, RTLIB.FLOOR_PPCF128);
  }

  private SDValue[] expandFloatRes_FLOG(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.LOG_F32, RTLIB.LOG_F64,
        RTLIB.LOG_F80, RTLIB.LOG_PPCF128);
  }

  private SDValue[] expandFloatRes_FLOG2(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.LOG2_F32, RTLIB.LOG2_F64,
        RTLIB.LOG2_F80, RTLIB.LOG2_PPCF128);
  }

  private SDValue[] expandFloatRes_FLOG10(SDNode n) {
    return commonExpandFloatResUnary(n, RTLIB.LOG10_F32, RTLIB.LOG10_F64,
        RTLIB.LOG10_F80, RTLIB.LOG10_PPCF128);
  }

  private SDValue[] commonExpandFloatResUnary(SDNode n,
                                              RTLIB lcF32, RTLIB lcF64, RTLIB lcF80, RTLIB lcPPCF128) {
    SDValue call = libCallify(getFPLibCall(n.getValueType(0),
        lcF32, lcF64, lcF80, lcPPCF128), n, false);
    return getPairElements(call);
  }

  private SDValue[] commonExpandFloatResBinary(SDNode n,
                                               RTLIB lcF32, RTLIB lcF64, RTLIB lcF80, RTLIB lcPPCF128) {
    SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
    SDValue call = makeLibCall(getFPLibCall(n.getValueType(0),
        lcF32, lcF64, lcF80, lcPPCF128),
        n.getValueType(0), ops, false);
    return getPairElements(call);
  }

  private SDValue[] expandFloatRes_FMUL(SDNode n) {
    return commonExpandFloatResBinary(n,
        RTLIB.MUL_F32,
        RTLIB.MUL_F64,
        RTLIB.MUL_F80,
        RTLIB.MUL_PPCF128);
  }

  private SDValue[] expandFloatRes_FNEARBYINT(SDNode n) {
    return commonExpandFloatResUnary(n,
        RTLIB.NEARBYINT_F32,
        RTLIB.NEARBYINT_F64,
        RTLIB.NEARBYINT_F80,
        RTLIB.NEARBYINT_PPCF128);
  }

  private SDValue[] expandFloatRes_FNEG(SDNode n) {
    SDValue[] t = getExpandedFloat(n.getOperand(0));
    SDValue lo = dag.getNode(ISD.FNEG, t[0].getValueType(), t[0]);
    SDValue hi = dag.getNode(ISD.FNEG, t[1].getValueType(), t[1]);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandFloatRes_FP_EXTEND(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue hi = dag.getNode(ISD.FP_EXTEND, nvt, n.getOperand(0));
    SDValue lo = dag.getConstantFP(new APFloat(new APInt(nvt.getSizeInBits(), 0)), nvt, false);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandFloatRes_FPOW(SDNode n) {
    return commonExpandFloatResUnary(n,
        RTLIB.POW_F32, RTLIB.POW_F64,
        RTLIB.POW_F80, RTLIB.POW_PPCF128);
  }

  private SDValue[] expandFloatRes_FPOWI(SDNode n) {
    return commonExpandFloatResUnary(n,
        RTLIB.POWI_F32, RTLIB.POWI_F64,
        RTLIB.POWI_F80, RTLIB.POWI_PPCF128);
  }

  private SDValue[] expandFloatRes_FRINT(SDNode n) {
    return commonExpandFloatResUnary(n,
        RTLIB.RINT_F32, RTLIB.RINT_F64,
        RTLIB.RINT_F80, RTLIB.RINT_PPCF128);
  }

  private SDValue[] expandFloatRes_FSIN(SDNode n) {
    return commonExpandFloatResUnary(n,
        RTLIB.SIN_F32, RTLIB.SIN_F64,
        RTLIB.SIN_F80, RTLIB.SIN_PPCF128);
  }

  private SDValue[] expandFloatRes_FSQRT(SDNode n) {
    return commonExpandFloatResUnary(n,
        RTLIB.SQRT_F32, RTLIB.SQRT_F64,
        RTLIB.SQRT_F80, RTLIB.SQRT_PPCF128);
  }

  private SDValue[] expandFloatRes_FSUB(SDNode n) {
    return commonExpandFloatResBinary(n,
        RTLIB.SUB_F32, RTLIB.SUB_F64,
        RTLIB.SUB_F80, RTLIB.SUB_PPCF128);
  }

  private SDValue[] expandFloatRes_FTRUNC(SDNode n) {
    return commonExpandFloatResUnary(n,
        RTLIB.TRUNC_F32, RTLIB.TRUNC_F64,
        RTLIB.TRUNC_F80, RTLIB.TRUNC_PPCF128);
  }

  private SDValue[] expandFloatRes_LOAD(SDNode n) {
    if (n.isNormalLoad()) {
      return expandRes_NormalLoad(n);
    }

    Util.assertion(n.isUNINDEXEDLoad(), "Indexed load during type legalization!");
    LoadSDNode ld = (LoadSDNode) n;
    SDValue chain = ld.getChain();
    SDValue ptr = ld.getBasePtr();

    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), ld.getValueType(0));
    Util.assertion(nvt.isByteSized(), "Expanded type not byte sized!");
    Util.assertion(ld.getMemoryVT().bitsLE(nvt), "Float type not round!");

    SDValue hi = dag.getExtLoad(ld.getExtensionType(), nvt, chain, ptr,
        ld.getSrcValue(), ld.getSrcValueOffset(), ld.getMemoryVT(),
        ld.isVolatile(), ld.getAlignment());
    chain = hi.getValue(1);
    SDValue lo = dag.getConstantFP(new APFloat(new APInt(nvt.getSizeInBits(), 0)), nvt, false);
    replaceValueWith(new SDValue(ld, 1), chain);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandFloatRes_XINT_TO_FP(SDNode n) {
    Util.assertion(n.getValueType(0).getSimpleVT().simpleVT == MVT.ppcf128, "Unsupported UINT_TO_FP or SINT_TO_FP?");

    EVT vt = n.getValueType(0);
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), vt);
    SDValue src = n.getOperand(0);
    EVT srcVT = src.getValueType();
    boolean isSigned = n.getOpcode() == ISD.SINT_TO_FP;

    SDValue lo, hi;
    if (srcVT.bitsLE(new EVT(MVT.i32))) {
      src = dag.getNode(isSigned ? ISD.SIGN_EXTEND : ISD.ZERO_EXTEND,
          new EVT(MVT.i32), src);
      lo = dag.getConstantFP(new APFloat(new APInt(nvt.getSizeInBits(), 0)), nvt, false);
      hi = dag.getNode(ISD.SINT_TO_FP, nvt, src);
    } else {
      RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
      if (srcVT.bitsLE(new EVT(MVT.i64))) {
        src = dag.getNode(isSigned ? ISD.SIGN_EXTEND : ISD.ZERO_EXTEND,
            new EVT(MVT.i64), src);
        lc = RTLIB.SINTTOFP_I64_PPCF128;
      } else if (srcVT.bitsLE(new EVT(MVT.i128))) {
        src = dag.getNode(ISD.SIGN_EXTEND, new EVT(MVT.i128), src);
        lc = RTLIB.SINTTOFP_I128_PPCF128;
      }
      Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported SINT_TO_FP or UINT_TO_FP?");
      hi = makeLibCall(lc, vt, new SDValue[]{src}, true);
      SDValue[] t = getPairElements(hi);
      lo = t[0];
      hi = t[1];
    }

    if (isSigned) return new SDValue[]{lo, hi};

    hi = dag.getNode(ISD.BUILD_PAIR, vt, lo, hi);
    srcVT = src.getValueType();

    // x>=0 ? (ppcf128)(iN)x : (ppcf128)(iN)x + 2^n; n=32,64,128.
    long[] twoE32 = {0x41f0000000000000L, 0};
    long[] twoE64 = {0x43f0000000000000L, 0};
    long[] twoE128 = {0x47f0000000000000L, 0};

    long[] parts = null;
    switch (srcVT.getSimpleVT().simpleVT) {
      case MVT.i32:
        parts = twoE32;
        break;
      case MVT.i64:
        parts = twoE64;
        break;
      case MVT.i128:
        parts = twoE128;
        break;
      default:
        Util.assertion(false, "Unsupported SINT_TO_FP or UINT_TO_FP!");
        break;
    }
    lo = dag.getNode(ISD.FADD, vt, hi, dag.getConstantFP(
        new APFloat(new APInt(128, parts)), new EVT(MVT.ppcf128), false));
    lo = dag.getNode(ISD.SELECT_CC, vt, src, dag.getConstant(0, srcVT, false),
        lo, hi, dag.getCondCode(SETLT));
    return getPairElements(lo);
  }

  private boolean expandFloatOperand(SDNode n, int opNo) {
    if (Util.DEBUG) {
      System.err.print("Expand float operand: ");
      n.dump(dag);
      System.err.println();
    }
    SDValue res = new SDValue();
    if (tli.getOperationAction(n.getOpcode(), n.getOperand(opNo).getValueType())
        == Custom) {
      res = tli.lowerOperation(new SDValue(n, 0), dag);
    }

    if (res.getNode() == null) {
      switch (n.getOpcode()) {
        case ISD.BIT_CONVERT:
          res = expandOp_BIT_CONVERT(n);
          break;
        case ISD.BUILD_VECTOR:
          res = expandOp_BUILD_VECTOR(n);
          break;
        case ISD.EXTRACT_ELEMENT:
          res = expandOp_EXTRACT_ELEMENT(n);
          break;
        case ISD.BR_CC:
          res = expandFloatOp_BR_CC(n);
          break;
        case ISD.FP_ROUND:
          res = expandFloatOp_FP_ROUND(n);
          break;
        case ISD.FP_TO_SINT:
          res = expandFloatOp_FP_TO_SINT(n);
          break;
        case ISD.FP_TO_UINT:
          res = expandFloatOp_FP_TO_UINT(n);
          break;
        case ISD.SELECT_CC:
          res = expandFloatOp_SELECT_CC(n);
          break;
        case ISD.SETCC:
          res = expandFloatOp_SETCC(n);
          break;
        case ISD.STORE:
          res = expandFloatOp_STORE((StoreSDNode) n, opNo);
          break;
      }
    }
    if (res.getNode() == null) return false;

    if (res.getNode().equals(n))
      return true;
    Util.assertion(res.getValueType().equals(n.getValueType(0)) && n.getNumValues() == 1, "Invalid operand expansion!");

    replaceValueWith(new SDValue(n, 0), res);
    return false;
  }

  private SDValue expandFloatOp_BR_CC(SDNode n) {
    SDValue newLHS = n.getOperand(2), newRHS = n.getOperand(3);
    CondCode cc = ((CondCodeSDNode) n.getOperand(1).getNode()).getCondition();

    SDValue[] t = floatExpandSetCCOperands(newLHS, newRHS, cc);
    newLHS = t[0];
    newRHS = t[1];

    if (newRHS.getNode() == null) {
      newRHS = dag.getConstant(0, newRHS.getValueType(), false);
      cc = SETNE;
    }

    return dag.updateNodeOperands(new SDValue(n, 0), n.getOperand(0),
        dag.getCondCode(cc), newLHS, newRHS, n.getOperand(4));
  }

  private SDValue expandFloatOp_FP_ROUND(SDNode n) {
    Util.assertion(n.getOperand(0).getValueType().getSimpleVT().simpleVT == MVT.ppcf128, "Just applied for ppcf128!");

    SDValue[] t = getExpandedFloat(n.getOperand(0));
    return dag.getNode(ISD.FP_ROUND, n.getValueType(0), t[1], n.getOperand(1));
  }

  private SDValue expandFloatOp_FP_TO_SINT(SDNode n) {
    EVT rvt = n.getValueType(0);
    if (rvt.getSimpleVT().simpleVT == MVT.i32) {
      Util.assertion(n.getOperand(0).getValueType().getSimpleVT().simpleVT == MVT.ppcf128, "Only applied for ppcf128!");

      SDValue res = dag.getNode(ISD.FP_ROUND_INREG, new EVT(MVT.ppcf128),
          n.getOperand(0), dag.getValueType(new EVT(MVT.f64)));
      res = dag.getNode(ISD.FP_ROUND, new EVT(MVT.f64), res,
          dag.getIntPtrConstant(1));
      return dag.getNode(ISD.FP_TO_SINT, new EVT(MVT.i32), res);
    }

    RTLIB lc = tli.getFPTOSINT(n.getOperand(0).getValueType(), rvt);
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported FP_TO_SINT!");
    return makeLibCall(lc, rvt, new SDValue[]{n.getOperand(0)}, false);
  }

  private SDValue expandFloatOp_FP_TO_UINT(SDNode n) {
    EVT rvt = n.getValueType(0);
    if (rvt.getSimpleVT().simpleVT == MVT.i32) {
      Util.assertion(n.getOperand(0).getValueType().getSimpleVT().simpleVT == MVT.ppcf128, "Only applied for ppcf128!");

      long[] twoE31 = {0x41e0000000000000L, 0};
      APFloat apf = new APFloat(new APInt(128, twoE31));
      SDValue temp = dag.getConstantFP(apf, new EVT(MVT.ppcf128), false);
      // X>=2^31 ? (int)(X-2^31)+0x80000000 : (int)X
      return dag.getNode(ISD.SELECT_CC, new EVT(MVT.i32),
          n.getOperand(0), temp,
          dag.getNode(ISD.ADD, new EVT(MVT.i32),
              dag.getNode(ISD.FP_TO_SINT, new EVT(MVT.i32),
                  dag.getNode(ISD.FSUB, new EVT(MVT.ppcf128),
                      n.getOperand(0), temp))),
          dag.getNode(ISD.FP_TO_SINT, new EVT(MVT.i32), n.getOperand(0)),
          dag.getCondCode(SETGE));
    }

    RTLIB lc = tli.getFPTOUINT(n.getOperand(0).getValueType(), rvt);
    Util.assertion(lc != RTLIB.UNKNOWN_LIBCALL, "Unsupported FP_TO_UINT!");
    return makeLibCall(lc, rvt, new SDValue[]{n.getOperand(0)}, false);
  }

  private SDValue expandFloatOp_SELECT_CC(SDNode n) {
    SDValue newLHS = n.getOperand(0), newRHS = n.getOperand(1);
    CondCode cc = ((CondCodeSDNode) n.getOperand(2).getNode()).getCondition();

    SDValue[] t = floatExpandSetCCOperands(newLHS, newRHS, cc);
    newLHS = t[0];
    newRHS = t[1];

    if (newRHS.getNode() == null) {
      newRHS = dag.getConstant(0, newLHS.getValueType(), false);
      cc = SETNE;
    }

    return dag.updateNodeOperands(new SDValue(n, 0), newLHS, newRHS,
        n.getOperand(2), n.getOperand(3), dag.getCondCode(cc));
  }

  private SDValue expandFloatOp_SETCC(SDNode n) {
    SDValue newLHS = n.getOperand(0), newRHS = n.getOperand(1);
    CondCode cc = ((CondCodeSDNode) n.getOperand(2).getNode()).getCondition();

    SDValue[] t = floatExpandSetCCOperands(newLHS, newRHS, cc);
    newLHS = t[0];
    newRHS = t[1];

    if (newRHS.getNode() == null) {
      Util.assertion(newLHS.getValueType().equals(n.getValueType(0)), "Unexpected setcc expansion!");

      return newLHS;
    }

    return dag.updateNodeOperands(new SDValue(n, 0), newLHS, newRHS,
        dag.getCondCode(cc));
  }

  private SDValue expandFloatOp_STORE(StoreSDNode n, int opNo) {
    if (n.isNormalStore())
      return expandOp_NormalStore(n, opNo);

    Util.assertion(n.isUNINDEXEDStore(), "Indexed store during type legalization!");
    Util.assertion(opNo == 1, "Can only expand the stored value so far!");
    SDValue chain = n.getChain();
    SDValue ptr = n.getBasePtr();

    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValue().getValueType());
    Util.assertion(nvt.isByteSized(), "Expanded type not byte sized!");
    Util.assertion(n.getMemoryVT().bitsLE(nvt), "Float type not round!");

    SDValue[] t = getExpandedOp(n.getValue());
    return dag.getTruncStore(chain, t[1], ptr,
        n.getSrcValue(), n.getSrcValueOffset(), n.getMemoryVT(),
        n.isVolatile(), n.getAlignment());
  }

  private SDValue[] floatExpandSetCCOperands(
      SDValue newLHS, SDValue newRHS,
      CondCode cc) {
    SDValue[] t1, t2;
    t1 = getExpandedFloat(newLHS);
    t2 = getExpandedFloat(newRHS);
    SDValue lhsLO = t1[0], lhsHI = t1[1], rhsLO = t2[0], rhsHI = t2[1];
    EVT vt = newLHS.getValueType();
    Util.assertion(vt.getSimpleVT().simpleVT == MVT.ppcf128, "Unsupported setcc type!");

    SDValue temp1 = dag.getSetCC(new EVT(tli.getSetCCResultType(lhsHI.getValueType())),
        lhsHI, rhsHI, SETOEQ);
    SDValue temp2 = dag.getSetCC(new EVT(tli.getSetCCResultType(lhsLO.getValueType())),
        lhsLO, rhsLO, cc);
    SDValue temp3 = dag.getNode(ISD.AND, temp1.getValueType(), temp1, temp2);

    temp1 = dag.getSetCC(new EVT(tli.getSetCCResultType(lhsHI.getValueType())),
        lhsHI, rhsHI, SETUNE);
    temp2 = dag.getSetCC(new EVT(tli.getSetCCResultType(lhsHI.getValueType())),
        lhsHI, rhsHI, cc);
    temp1 = dag.getNode(ISD.AND, temp1.getValueType(), temp1, temp2);
    newLHS = dag.getNode(ISD.OR, temp1.getValueType(), temp1, temp2);
    newRHS = new SDValue();
    return new SDValue[]{newLHS, newRHS};
  }

  private SDValue getScalarizedVector(SDValue op) {
    SDValue scalarizedOp = scalarizedVectors.get(op);
    scalarizedOp = remapValue(scalarizedOp);
    Util.assertion(scalarizedOp.getNode() != null);
    scalarizedVectors.put(op, scalarizedOp);
    return scalarizedOp;
  }

  private void setScalarizedVector(SDValue op, SDValue result) {
    Util.assertion(result.getValueType().equals(op.getValueType().getVectorElementType()), "Invalid type for scalarized vector!");

    result = analyzeNewValue(result);
    Util.assertion(!scalarizedVectors.containsKey(op));
    scalarizedVectors.put(op, result);
  }

  private void scalarizeVectorResult(SDNode n, int resNo) {
    if (Util.DEBUG) {
      System.err.printf("Scalarize node result %d: ", resNo);
      n.dump(dag);
      System.err.println();
    }
    SDValue res = null;
    switch (n.getOpcode()) {
      case ISD.BIT_CONVERT:
        res = scalarizeVecRes_BIT_CONVERT(n);
        break;
      case ISD.BUILD_VECTOR:
        res = n.getOperand(0);
        break;
      case ISD.CONVERT_RNDSAT:
        res = scalarizeVecRes_CONVERT_RNDSAT(n);
        break;
      case ISD.EXTRACT_SUBVECTOR:
        res = scalarizeVecRes_EXTRACT_SUBVECTOR(n);
        break;
      case ISD.FPOWI:
        res = scalarizeVecRes_FPOWI(n);
        break;
      case ISD.SCALAR_TO_VECTOR:
        res = scalarizeVecRes_SCALAR_TO_VECTOR(n);
        break;
      case ISD.INSERT_VECTOR_ELT:
        res = scalarizeVecRes_INSERT_VECTOR_ELT(n);
        break;
      case ISD.LOAD:
        res = scalarizeVecRes_LOAD((LoadSDNode) n);
        break;
      case ISD.SELECT:
        res = scalarizeVecRes_SELECT(n);
        break;
      case ISD.SELECT_CC:
        res = scalarizeVecRes_SELECT_CC(n);
        break;
      case ISD.SETCC:
        res = scalarizeVecRes_SETCC(n);
        break;
      case ISD.UNDEF:
        res = scalarizeVecRes_UNDEF(n);
        break;
      case ISD.VECTOR_SHUFFLE:
        res = scalarizeVecRes_VECTOR_SHUFFLE(n);
        break;
      case ISD.VSETCC:
        res = scalarizeVecRes_VSETCC(n);
        break;

      case ISD.CTLZ:
      case ISD.CTPOP:
      case ISD.CTTZ:
      case ISD.FABS:
      case ISD.FCOS:
      case ISD.FNEG:
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
      case ISD.FSIN:
      case ISD.FSQRT:
      case ISD.FTRUNC:
      case ISD.FFLOOR:
      case ISD.FCEIL:
      case ISD.FRINT:
      case ISD.FNEARBYINT:
      case ISD.UINT_TO_FP:
      case ISD.SINT_TO_FP:
      case ISD.TRUNCATE:
      case ISD.SIGN_EXTEND:
      case ISD.ZERO_EXTEND:
      case ISD.ANY_EXTEND:
        res = scalarizeVecRes_UnaryOp(n);
        break;

      case ISD.ADD:
      case ISD.AND:
      case ISD.FADD:
      case ISD.FDIV:
      case ISD.FMUL:
      case ISD.FPOW:
      case ISD.FREM:
      case ISD.FSUB:
      case ISD.MUL:
      case ISD.OR:
      case ISD.SDIV:
      case ISD.SREM:
      case ISD.SUB:
      case ISD.UDIV:
      case ISD.UREM:
      case ISD.XOR:
      case ISD.SHL:
      case ISD.SRA:
      case ISD.SRL:
        res = scalarizeVecRes_BinOp(n);
        break;

      default:
        if (Util.DEBUG) {
          System.err.printf("scalarizeVectorResult #%d", resNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to scalarize the result of this operator!");
        break;
    }
    if (res.getNode() != null)
      setScalarizedVector(new SDValue(n, resNo), res);
  }

  private SDValue scalarizeVecRes_BinOp(SDNode n) {
    SDValue lhs = getScalarizedVector(n.getOperand(0));
    SDValue rhs = getScalarizedVector(n.getOperand(1));
    return dag.getNode(n.getOpcode(), lhs.getValueType(), lhs, rhs);
  }

  private SDValue scalarizeVecRes_UnaryOp(SDNode n) {
    EVT destVT = n.getValueType(0).getVectorElementType();
    SDValue op = getScalarizedVector(n.getOperand(0));
    return dag.getNode(n.getOpcode(), destVT, op);
  }

  private SDValue scalarizeVecRes_BIT_CONVERT(SDNode n) {
    EVT newVT = n.getValueType(0).getVectorElementType();
    return dag.getNode(ISD.BIT_CONVERT, newVT, n.getOperand(0));
  }

  private SDValue scalarizeVecRes_CONVERT_RNDSAT(SDNode n) {
    EVT newVT = n.getValueType(0).getVectorElementType();
    SDValue op0 = getScalarizedVector(n.getOperand(0));
    return dag.getConvertRndSat(newVT, op0, dag.getValueType(newVT),
        dag.getValueType(op0.getValueType()),
        n.getOperand(3),
        n.getOperand(4),
        ((CvtRndSatSDNode) n).getCvtCode());
  }

  private SDValue scalarizeVecRes_EXTRACT_SUBVECTOR(SDNode n) {
    return dag.getNode(ISD.EXTRACT_VECTOR_ELT,
        n.getValueType(0).getVectorElementType(),
        n.getOperand(0), n.getOperand(1));
  }

  private SDValue scalarizeVecRes_FPOWI(SDNode n) {
    SDValue op = getScalarizedVector(n.getOperand(0));
    return dag.getNode(ISD.FPOWI, op.getValueType(), op, n.getOperand(1));
  }

  private SDValue scalarizeVecRes_INSERT_VECTOR_ELT(SDNode n) {
    SDValue op = n.getOperand(1);
    EVT eltVT = n.getValueType(0).getVectorElementType();
    if (!op.getValueType().equals(eltVT))
      op = dag.getNode(ISD.TRUNCATE, eltVT, op);
    return op;
  }

  private SDValue scalarizeVecRes_LOAD(LoadSDNode n) {
    Util.assertion(n.isUnindexed(), "Indexed vector load?");
    SDValue result = dag.getLoad(MemIndexedMode.UNINDEXED,
        n.getExtensionType(), n.getValueType(0).getVectorElementType(),
        n.getChain(), n.getBasePtr(), dag.getUNDEF(n.getBasePtr().getValueType()),
        n.getSrcValue(), n.getSrcValueOffset(), n.getMemoryVT().getVectorElementType(),
        n.isVolatile(), n.getAlignment());
    replaceValueWith(new SDValue(n, 1), result.getValue(1));
    return result;
  }

  private SDValue scalarizeVecRes_SCALAR_TO_VECTOR(SDNode n) {
    EVT eltVT = n.getValueType(0).getVectorElementType();
    SDValue inOp = n.getOperand(0);
    if (!inOp.getValueType().equals(eltVT))
      return dag.getNode(ISD.TRUNCATE, eltVT, inOp);
    return inOp;
  }

  private SDValue scalarizeVecRes_SELECT(SDNode n) {
    SDValue lhs = getScalarizedVector(n.getOperand(1));
    return dag.getNode(ISD.SELECT, lhs.getValueType(),
        n.getOperand(0), lhs, getScalarizedVector(n.getOperand(2)));
  }

  private SDValue scalarizeVecRes_SELECT_CC(SDNode n) {
    SDValue lhs = getScalarizedVector(n.getOperand(2));
    return dag.getNode(ISD.SELECT_CC, lhs.getValueType(),
        n.getOperand(0), n.getOperand(1), lhs,
        getScalarizedVector(n.getOperand(3)), n.getOperand(4));
  }

  private SDValue scalarizeVecRes_SETCC(SDNode n) {
    SDValue lhs = getScalarizedVector(n.getOperand(0));
    SDValue rhs = getScalarizedVector(n.getOperand(1));
    return dag.getNode(ISD.SETCC, new EVT(MVT.i1), lhs, rhs, n.getOperand(2));
  }

  private SDValue scalarizeVecRes_UNDEF(SDNode n) {
    return dag.getUNDEF(n.getValueType(0).getVectorElementType());
  }

  private SDValue scalarizeVecRes_VECTOR_SHUFFLE(SDNode n) {
    SDValue arg = n.getOperand(2).getOperand(0);
    if (arg.getOpcode() == ISD.UNDEF)
      return dag.getUNDEF(n.getValueType(0).getVectorElementType());
    int op = ((ConstantSDNode) arg.getNode()).isNullValue() ? 1 : 0;
    return getScalarizedVector(n.getOperand(op));
  }

  private SDValue scalarizeVecRes_VSETCC(SDNode n) {
    SDValue lhs = getScalarizedVector(n.getOperand(0));
    SDValue rhs = getScalarizedVector(n.getOperand(1));
    EVT nvt = n.getValueType(0).getVectorElementType();
    EVT svt = new EVT(tli.getSetCCResultType(lhs.getValueType()));

    SDValue res = dag.getNode(ISD.SETCC, svt, lhs, rhs, n.getOperand(2));
    if (nvt.bitsLE(svt)) {
      if (tli.getBooleanContents() == ZeroOrNegativeOneBooleanContent)
        res = dag.getNode(ISD.SIGN_EXTEND_INREG, svt, res,
            dag.getValueType(new EVT(MVT.i1)));

      return dag.getNode(ISD.TRUNCATE, nvt, res);
    }

    if (tli.getBooleanContents() == ZeroOrNegativeOneBooleanContent)
      res = dag.getNode(ISD.TRUNCATE, new EVT(MVT.i1), res);
    return dag.getNode(ISD.SIGN_EXTEND, nvt, res);
  }

  // Vector Operand Scalarization: <1 x ty> -> ty.
  private boolean scalarizeVectorOperand(SDNode n, int opNo) {
    if (Util.DEBUG) {
      System.err.printf("Scalarize node operand %d: ", opNo);
      n.dump(dag);
      System.err.println();
    }
    SDValue res = new SDValue();
    switch (n.getOpcode()) {
      case ISD.BIT_CONVERT:
        res = scalarizeVecOp_BIT_CONVERT(n);
        break;
      case ISD.CONCAT_VECTORS:
        res = scalarizeVecOp_CONCAT_VECTORS(n);
        break;
      case ISD.EXTRACT_VECTOR_ELT:
        res = scalarizeVecOp_EXTRACT_VECTOR_ELT(n);
        break;
      case ISD.STORE:
        res = scalarizeVecOp_STORE((StoreSDNode) n, opNo);
        break;
      default:
        if (Util.DEBUG) {
          System.err.printf("scalarizeVectorOperand op #%d: ", opNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to scalarize this operator's operand");
        break;
    }
    if (res.getNode() == null) return false;

    if (res.getNode().equals(n))
      return true;

    Util.assertion(res.getValueType().equals(n.getValueType(0)) && n.getNumValues() == 1);

    replaceValueWith(new SDValue(n, 0), res);
    return false;
  }

  private SDValue scalarizeVecOp_BIT_CONVERT(SDNode n) {
    SDValue elt = getScalarizedVector(n.getOperand(0));
    return dag.getNode(ISD.BIT_CONVERT, n.getValueType(0), elt);
  }

  private SDValue scalarizeVecOp_CONCAT_VECTORS(SDNode n) {
    SDValue[] ops = new SDValue[n.getNumOperands()];
    for (int i = 0, e = ops.length; i < e; i++)
      ops[i] = getScalarizedVector(n.getOperand(i));
    return dag.getNode(ISD.BUILD_VECTOR, n.getValueType(0), ops);
  }

  private SDValue scalarizeVecOp_EXTRACT_VECTOR_ELT(SDNode n) {
    SDValue res = getScalarizedVector(n.getOperand(0));
    if (!res.getValueType().equals(n.getValueType(0)))
      res = dag.getNode(ISD.ANY_EXTEND, n.getValueType(0), res);
    return res;
  }

  private SDValue scalarizeVecOp_STORE(StoreSDNode n, int opNo) {
    Util.assertion(n.isUnindexed(), "Indexed store of one element vector?");
    Util.assertion(opNo == 1);
    if (n.isTruncatingStore())
      return dag.getTruncStore(n.getChain(),
          getScalarizedVector(n.getOperand(1)),
          n.getBasePtr(),
          n.getSrcValue(), n.getSrcValueOffset(),
          n.getMemoryVT().getVectorElementType(),
          n.isVolatile(), n.getAlignment());
    return dag.getStore(n.getChain(), getScalarizedVector(n.getOperand(1)),
        n.getBasePtr(), n.getSrcValue(), n.getSrcValueOffset(),
        n.isVolatile(), n.getAlignment());
  }

  private SDValue[] getSplitVector(SDValue op) {
    Pair<SDValue, SDValue> entry = splitVectors.get(op);
    entry.first = remapValue(entry.first);
    entry.second = remapValue(entry.second);
    Util.assertion(entry.first.getNode() != null);
    return new SDValue[]{entry.first, entry.second};
  }

  private void setSplitVector(SDValue op, SDValue lo, SDValue hi) {
    Util.assertion(lo.getValueType().getVectorElementType().equals(op.getValueType().getVectorElementType()) &&
            2 * lo.getValueType().getVectorNumElements() ==
                op.getValueType().getVectorNumElements() &&
            hi.getValueType().equals(lo.getValueType()),
        "Invalid type for split vector!");


    lo = analyzeNewValue(lo);
    hi = analyzeNewValue(hi);

    Util.assertion(!splitVectors.containsKey(op));
    Pair<SDValue, SDValue> entry = Pair.get(lo, hi);
    splitVectors.put(op, entry);
  }

  // Vector Result Splitting: <128 x ty> -> 2 x <64 x ty>.
  private void splitVectorResult(SDNode n, int resNo) {
    if (Util.DEBUG) {
      System.err.print("Split node result: ");
      n.dump(dag);
      System.err.println();
    }
    SDValue[] t = null;
    switch (n.getOpcode()) {
      case ISD.MERGE_VALUES:
        t = splitRes_MERGE_VALUES(n);
        break;
      case ISD.SELECT:
        t = splitRes_SELECT(n);
        break;
      case ISD.SELECT_CC:
        t = splitRes_SELECT_CC(n);
        break;
      case ISD.UNDEF:
        t = splitRes_UNDEF(n);
        break;

      case ISD.BIT_CONVERT:
        t = splitVecRes_BIT_CONVERT(n);
        break;
      case ISD.BUILD_VECTOR:
        t = splitVecRes_BUILD_VECTOR(n);
        break;
      case ISD.CONCAT_VECTORS:
        t = splitVecRes_CONCAT_VECTORS(n);
        break;
      case ISD.CONVERT_RNDSAT:
        t = splitVecRes_CONVERT_RNDSAT(n);
        break;
      case ISD.EXTRACT_SUBVECTOR:
        t = splitVecRes_EXTRACT_SUBVECTOR(n);
        break;
      case ISD.FPOWI:
        t = splitVecRes_FPOWI(n);
        break;
      case ISD.INSERT_VECTOR_ELT:
        t = splitVecRes_INSERT_VECTOR_ELT(n);
        break;
      case ISD.SCALAR_TO_VECTOR:
        t = splitVecRes_SCALAR_TO_VECTOR(n);
        break;
      case ISD.LOAD:
        t = splitVecRes_LOAD((LoadSDNode) (n));
        break;
      case ISD.SETCC:
      case ISD.VSETCC:
        t = splitVecRes_SETCC(n);
        break;
      case ISD.VECTOR_SHUFFLE:
        t = splitVecRes_VECTOR_SHUFFLE((ShuffleVectorSDNode) (n));
        break;

      case ISD.CTTZ:
      case ISD.CTLZ:
      case ISD.CTPOP:
      case ISD.FNEG:
      case ISD.FABS:
      case ISD.FSQRT:
      case ISD.FSIN:
      case ISD.FCOS:
      case ISD.FTRUNC:
      case ISD.FFLOOR:
      case ISD.FCEIL:
      case ISD.FRINT:
      case ISD.FNEARBYINT:
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
      case ISD.TRUNCATE:
      case ISD.SIGN_EXTEND:
      case ISD.ZERO_EXTEND:
      case ISD.ANY_EXTEND:
        t = splitVecRes_UnaryOp(n);
        break;

      case ISD.ADD:
      case ISD.SUB:
      case ISD.MUL:
      case ISD.FADD:
      case ISD.FSUB:
      case ISD.FMUL:
      case ISD.SDIV:
      case ISD.UDIV:
      case ISD.FDIV:
      case ISD.FPOW:
      case ISD.AND:
      case ISD.OR:
      case ISD.XOR:
      case ISD.SHL:
      case ISD.SRA:
      case ISD.SRL:
      case ISD.UREM:
      case ISD.SREM:
      case ISD.FREM:
        t = splitVecRes_BinOp(n);
        break;
      default:
        if (Util.DEBUG) {
          System.err.printf("splitVectorResult #%d", resNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to split the result of this operator!");
        break;
    }
    if (t[0].getNode() != null)
      setSplitVector(new SDValue(n, resNo), t[0], t[1]);
  }

  private SDValue[] splitVecRes_BinOp(SDNode n) {
    SDValue[] lhsT = getSplitVector(n.getOperand(0));
    SDValue[] rhsT = getSplitVector(n.getOperand(1));
    SDValue lo = dag.getNode(n.getOpcode(), lhsT[0].getValueType(), lhsT[0], rhsT[0]);
    SDValue hi = dag.getNode(n.getOpcode(), lhsT[1].getValueType(), lhsT[1], rhsT[1]);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_UnaryOp(SDNode n) {
    EVT[] vts = getSplitDestVTs(n.getValueType(0));
    EVT loVT = vts[0], hiVT = vts[1];

    SDValue lo = null, hi = null;
    EVT inVT = n.getOperand(0).getValueType();
    switch (getTypeAction(inVT)) {
      case Legal: {
        EVT inNVT = EVT.getVectorVT(dag.getContext(), inVT.getVectorElementType(),
            loVT.getVectorNumElements());
        lo = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, n.getOperand(0),
            dag.getIntPtrConstant(0));
        hi = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, n.getOperand(0),
            dag.getIntPtrConstant(inNVT.getVectorNumElements()));
        break;
      }
      case SplitVector: {
        SDValue[] t = getSplitVector(n.getOperand(0));
        lo = t[0];
        hi = t[1];
        break;
      }
      case WidenVector: {
        SDValue inOp = getWidenedVector(n.getOperand(0));
        EVT inNVT = EVT.getVectorVT(dag.getContext(), inVT.getVectorElementType(),
            loVT.getVectorNumElements());
        lo = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, inOp,
            dag.getIntPtrConstant(0));
        hi = dag.getNode(ISD.EXTRACT_SUBVECTOR, inVT, inOp,
            dag.getIntPtrConstant(inNVT.getVectorNumElements()));
        break;
      }
      default:
        Util.shouldNotReachHere("Unexpected type action!");
        break;
    }
    lo = dag.getNode(n.getOpcode(), loVT, lo);
    hi = dag.getNode(n.getOpcode(), hiVT, hi);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_BIT_CONVERT(SDNode n) {
    EVT[] vts = getSplitDestVTs(n.getValueType(0));
    EVT loVT = vts[0], hiVT = vts[1];

    SDValue inOp = n.getOperand(0);
    EVT inVT = inOp.getValueType();
    SDValue lo, hi;
    switch (getTypeAction(inVT)) {
      default:
        Util.assertion(false, "Unknown type action!");
      case Legal:
      case PromotedInteger:
      case SoftenFloat:
      case ScalarizeVector:
        break;
      case ExpandInteger:
      case ExpandFloat: {
        if (loVT.equals(hiVT)) {
          SDValue[] t = getExpandedOp(inOp);
          lo = t[0];
          hi = t[1];
          if (tli.isBigEndian()) {
            SDValue temp = lo;
            lo = hi;
            hi = temp;
          }
          lo = dag.getNode(ISD.BIT_CONVERT, loVT, lo);
          hi = dag.getNode(ISD.BIT_CONVERT, hiVT, hi);
          return new SDValue[]{lo, hi};
        }
        break;
      }
      case SplitVector: {
        SDValue[] t = getSplitVector(inOp);
        lo = dag.getNode(ISD.BIT_CONVERT, loVT, t[0]);
        hi = dag.getNode(ISD.BIT_CONVERT, hiVT, t[1]);
        return new SDValue[]{lo, hi};
      }
    }
    EVT loIntVT = EVT.getIntegerVT(dag.getContext(), loVT.getSizeInBits());
    EVT hiIntVT = EVT.getIntegerVT(dag.getContext(), hiVT.getSizeInBits());
    if (tli.isBigEndian()) {
      EVT temp = loIntVT;
      loIntVT = hiIntVT;
      hiIntVT = temp;
    }

    SDValue[] t = splitInteger(bitConvertToInteger(inOp), loIntVT, hiIntVT);
    lo = t[0];
    hi = t[1];
    if (tli.isBigEndian()) {
      SDValue temp = lo;
      lo = hi;
      hi = temp;
    }
    lo = dag.getNode(ISD.BIT_CONVERT, loVT, lo);
    hi = dag.getNode(ISD.BIT_CONVERT, hiVT, hi);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_BUILD_VECTOR(SDNode n) {
    EVT[] vts = getSplitDestVTs(n.getValueType(0));
    EVT loVT = vts[0], hiVT = vts[1];
    int loNumElts = loVT.getVectorNumElements();
    SDValue[] loOps = new SDValue[loNumElts];
    Util.assertion(n.getNumOperands() >= loNumElts);
    int i = 0;
    for (; i < loNumElts; i++)
      loOps[i] = n.getOperand(i);
    SDValue lo = dag.getNode(ISD.BUILD_VECTOR, loVT, loOps);

    SDValue[] hiOps = new SDValue[n.getNumOperands() - loNumElts];
    int offset = i;
    for (int e = n.getNumOperands(); i < e; i++)
      hiOps[i - offset] = n.getOperand(i);
    SDValue hi = dag.getNode(ISD.BUILD_VECTOR, hiVT, hiOps);
    return new SDValue[]{lo, hi};

  }

  private SDValue[] splitVecRes_CONCAT_VECTORS(SDNode n) {
    Util.assertion((n.getNumOperands() & 1) == 0);
    int numSubvectors = n.getNumOperands() / 2;
    SDValue lo, hi;
    if (numSubvectors == 1) {
      lo = n.getOperand(0);
      hi = n.getOperand(1);
      return new SDValue[]{lo, hi};
    }

    EVT[] vts = getSplitDestVTs(n.getValueType(0));
    EVT loVT = vts[0], hiVT = vts[1];
    SDValue[] loOps = new SDValue[numSubvectors];
    Util.assertion(n.getNumOperands() >= numSubvectors);
    int i = 0;
    for (; i < numSubvectors; i++)
      loOps[i] = n.getOperand(i);
    lo = dag.getNode(ISD.CONCAT_VECTORS, loVT, loOps);

    SDValue[] hiOps = new SDValue[n.getNumOperands() - numSubvectors];
    int offset = i;
    for (int e = n.getNumOperands(); i < e; i++)
      hiOps[i - offset] = n.getOperand(i);
    hi = dag.getNode(ISD.CONCAT_VECTORS, hiVT, hiOps);
    return new SDValue[]{lo, hi};

  }

  private SDValue[] splitVecRes_CONVERT_RNDSAT(SDNode n) {
    EVT[] vts = getSplitDestVTs(n.getValueType(0));
    EVT loVT = vts[0], hiVT = vts[1];

    SDValue dTyOpLo = dag.getValueType(loVT);
    SDValue dTyOpHi = dag.getValueType(hiVT);

    SDValue rndOp = n.getOperand(3);
    SDValue satOp = n.getOperand(4);
    CvtCode cvtCode = ((CvtRndSatSDNode) n).getCvtCode();

    SDValue vLo = null, vHi = null;
    EVT inVT = n.getOperand(0).getValueType();
    switch (getTypeAction(inVT)) {
      case Legal: {
        EVT inNVT = EVT.getVectorVT(dag.getContext(), inVT.getVectorElementType(),
            loVT.getVectorNumElements());
        vLo = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, n.getOperand(0),
            dag.getIntPtrConstant(0));
        vHi = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, n.getOperand(0),
            dag.getIntPtrConstant(inNVT.getVectorNumElements()));
        break;
      }
      case SplitVector: {
        SDValue[] t = getSplitVector(n.getOperand(0));
        vLo = t[0];
        vHi = t[1];
        break;
      }
      case WidenVector: {
        SDValue inOp = getWidenedVector(n.getOperand(0));
        EVT inNVT = EVT.getVectorVT(dag.getContext(), inVT.getVectorElementType(),
            loVT.getVectorNumElements());
        vLo = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, inOp,
            dag.getIntPtrConstant(0));
        vHi = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, inOp,
            dag.getIntPtrConstant(inNVT.getVectorNumElements()));
        break;
      }
      default:
        Util.shouldNotReachHere("Unknown type action!");
        break;
    }
    SDValue sTyOpLo = dag.getValueType(vLo.getValueType());
    SDValue sTyOpHi = dag.getValueType(vHi.getValueType());

    SDValue lo = dag.getConvertRndSat(loVT, vLo, dTyOpLo, sTyOpLo, rndOp,
        satOp, cvtCode);
    SDValue hi = dag.getConvertRndSat(hiVT, vHi, dTyOpHi, sTyOpHi, rndOp,
        satOp, cvtCode);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_EXTRACT_SUBVECTOR(SDNode n) {
    SDValue vec = n.getOperand(0);
    SDValue idx = n.getOperand(1);
    EVT idxVT = idx.getValueType();

    EVT[] vts = getSplitDestVTs(n.getValueType(0));

    SDValue lo = dag.getNode(ISD.EXTRACT_SUBVECTOR, vts[0], vec, idx);
    idx = dag.getNode(ISD.ADD, idxVT, idx, dag.getConstant(vts[0].getVectorNumElements(),
        idxVT, false));
    SDValue hi = dag.getNode(ISD.EXTRACT_SUBVECTOR, vts[1], vec, idx);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_FPOWI(SDNode n) {
    SDValue[] t = getSplitVector(n.getOperand(0));
    SDValue lo = dag.getNode(ISD.FPOWI, t[0].getValueType(), t[0], n.getOperand(1));
    SDValue hi = dag.getNode(ISD.FPOWI, t[1].getValueType(), t[1], n.getOperand(1));
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_INSERT_VECTOR_ELT(SDNode n) {
    SDValue vec = n.getOperand(0);
    SDValue elt = n.getOperand(1);
    SDValue idx = n.getOperand(2);
    SDValue[] t = getSplitVector(vec);
    SDValue lo = t[0], hi = t[1];

    if (idx.getNode() instanceof ConstantSDNode) {
      ConstantSDNode csn = (ConstantSDNode) idx.getNode();
      long idxVal = csn.getZExtValue();
      int loNumElts = lo.getValueType().getVectorNumElements();
      if (idxVal < loNumElts)
        lo = dag.getNode(ISD.INSERT_VECTOR_ELT, lo.getValueType(),
            lo, elt, idx);
      else
        hi = dag.getNode(ISD.INSERT_VECTOR_ELT, hi.getValueType(),
            hi, dag.getIntPtrConstant(idxVal - loNumElts));
      return new SDValue[]{lo, hi};
    }

    EVT vecVT = vec.getValueType();
    EVT eltVT = vecVT.getVectorElementType();
    SDValue stackPtr = dag.createStackTemporary(vecVT);
    SDValue store = dag.getStore(dag.getEntryNode(), vec, stackPtr, null, 0, false, 0);

    SDValue eltPtr = getVectorElementPointer(stackPtr, eltVT, idx);
    int alignment = tli.getTargetData().getPrefTypeAlignment(vecVT.getTypeForEVT(dag.getContext()));
    store = dag.getTruncStore(store, elt, eltPtr, null, 0, eltVT);

    lo = dag.getLoad(lo.getValueType(), store, stackPtr, null, 0);
    int incrementSize = lo.getValueType().getSizeInBits() / 8;
    stackPtr = dag.getNode(ISD.ADD, stackPtr.getValueType(), stackPtr,
        dag.getIntPtrConstant(incrementSize));
    hi = dag.getLoad(hi.getValueType(), store, stackPtr, null,
        0, false,
        Util.minAlign(alignment, incrementSize));
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_LOAD(LoadSDNode n) {
    Util.assertion(n.isUNINDEXEDLoad(), "Indexed load during type legalization!");
    EVT[] vts = getSplitDestVTs(n.getValueType(0));

    LoadExtType extType = n.getExtensionType();
    SDValue chain = n.getChain();
    SDValue ptr = n.getBasePtr();
    SDValue offset = dag.getUNDEF(ptr.getValueType());
    Value sv = n.getSrcValue();
    int svOffset = n.getSrcValueOffset();
    EVT memVT = n.getMemoryVT();
    int alignment = n.getAlignment();
    boolean isVolatile = n.isVolatile();

    EVT[] memVTs = getSplitDestVTs(memVT);
    EVT loMemVT = memVTs[0], hiMemVT = memVTs[1];

    SDValue lo = dag.getLoad(MemIndexedMode.UNINDEXED, extType, vts[0],
        chain, ptr, offset, sv, svOffset, loMemVT, isVolatile, alignment);
    int incrementSize = loMemVT.getSizeInBits() / 8;
    ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
        dag.getIntPtrConstant(incrementSize));
    svOffset += incrementSize;
    alignment = Util.minAlign(alignment, incrementSize);
    SDValue hi = dag.getLoad(MemIndexedMode.UNINDEXED, extType, vts[1],
        chain, ptr, offset, sv, svOffset, hiMemVT, isVolatile, alignment);

    chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo.getValue(1),
        hi.getValue(1));
    replaceValueWith(new SDValue(n, 1), chain);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_SCALAR_TO_VECTOR(SDNode n) {
    EVT[] vts = getSplitDestVTs(n.getValueType(0));
    SDValue lo = dag.getNode(ISD.SCALAR_TO_VECTOR, vts[0], n.getOperand(0));
    SDValue hi = dag.getUNDEF(vts[1]);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitVecRes_SETCC(SDNode n) {
    EVT[] vts = getSplitDestVTs(n.getValueType(0));
    EVT loVT = vts[0], hiVT = vts[1];

    EVT inVT = n.getOperand(0).getValueType();
    SDValue ll, lh, rl, rh;
    EVT inNVT = EVT.getVectorVT(dag.getContext(), inVT.getVectorElementType(),
        loVT.getVectorNumElements());
    ll = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, n.getOperand(0),
        dag.getIntPtrConstant(0));
    lh = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, n.getOperand(0),
        dag.getIntPtrConstant(inNVT.getVectorNumElements()));
    rl = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, n.getOperand(1),
        dag.getIntPtrConstant(0));
    rh = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, n.getOperand(1),
        dag.getIntPtrConstant(inNVT.getVectorNumElements()));
    SDValue lo = dag.getNode(n.getOpcode(), loVT, ll, rl, n.getOperand(2));
    SDValue hi = dag.getNode(n.getOpcode(), hiVT, lh, rh, n.getOperand(2));
    return new SDValue[]{lo, hi};

  }

  private SDValue[] splitVecRes_VECTOR_SHUFFLE(ShuffleVectorSDNode n) {
    SDValue[] t1 = getSplitVector(n.getOperand(0));
    SDValue[] t2 = getSplitVector(n.getOperand(1));

    SDValue[] inputs = {t1[0], t1[1], t2[0], t2[1]};
    EVT newVT = inputs[0].getValueType();
    int newElts = newVT.getVectorNumElements();

    TIntArrayList ops = new TIntArrayList();
    SDValue lo = new SDValue(), hi = new SDValue();
    for (int high = 0; high < 2; high++) {
      SDValue output = high != 0 ? hi : lo;

      int[] inputUsed = {-1, -1};
      int firstMaskIdx = high * newElts;
      boolean useBuildVector = false;
      for (int maskOffset = 0; maskOffset < newElts; ++maskOffset) {
        int idx = n.getMaskElt(firstMaskIdx + maskOffset);
        int input = idx / newElts;

        if (input >= inputs.length) {
          ops.add(-1);
          continue;
        }

        idx -= input * newElts;
        int opNo = 0;
        for (; opNo < inputUsed.length; ++opNo) {
          if (inputUsed[opNo] == input) {
            break;
          } else if (inputUsed[opNo] == -1) {
            inputUsed[opNo] = input;
            break;
          }
        }
        if (opNo >= inputUsed.length) {
          useBuildVector = true;
          break;
        }

        ops.add(idx + opNo * newElts);
      }

      if (useBuildVector) {
        EVT eltVT = newVT.getVectorElementType();
        ArrayList<SDValue> svOps = new ArrayList<>();

        for (int maskOffset = 0; maskOffset < newElts; maskOffset++) {
          int idx = n.getMaskElt(firstMaskIdx + maskOffset);

          int input = idx / newElts;
          if (input >= inputs.length) {
            svOps.add(dag.getUNDEF(eltVT));
            continue;
          }

          idx -= input * newElts;
          svOps.add(dag.getNode(ISD.EXTRACT_VECTOR_ELT, eltVT,
              inputs[input], dag.getIntPtrConstant(idx)));
        }
        output = dag.getNode(ISD.BUILD_VECTOR, newVT, svOps);
      } else if (inputUsed[0] == -1) {
        output = dag.getUNDEF(newVT);
      } else {
        SDValue op0 = inputs[inputUsed[0]];
        SDValue op1 = inputUsed[1] == -1 ? dag.getUNDEF(newVT) :
            inputs[inputUsed[1]];
        output = dag.getVectorShuffle(newVT, op0, op1, ops.toArray());
      }
      if (high != 0)
        hi = output;
      else
        lo = output;
      ops.clear();
    }
    return new SDValue[]{lo, hi};
  }

  // Vector Operand Splitting: <128 x ty> -> 2 x <64 x ty>.
  private boolean splitVectorOperand(SDNode n, int opNo) {
    if (Util.DEBUG) {
      System.err.print("Split node operand: ");
      n.dump(dag);
      System.err.println();
    }
    SDValue res = new SDValue();

    switch (n.getOpcode()) {
      case ISD.BIT_CONVERT:
        res = splitVecOp_BIT_CONVERT(n);
        break;
      case ISD.EXTRACT_SUBVECTOR:
        res = splitVecOp_EXTRACT_SUBVECTOR(n);
        break;
      case ISD.EXTRACT_VECTOR_ELT:
        res = splitVecOp_EXTRACT_VECTOR_ELT(n);
        break;
      case ISD.STORE:
        res = splitVecOp_STORE((StoreSDNode) n, opNo);
        break;

      case ISD.CTTZ:
      case ISD.CTLZ:
      case ISD.CTPOP:
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
      case ISD.TRUNCATE:
      case ISD.SIGN_EXTEND:
      case ISD.ZERO_EXTEND:
      case ISD.ANY_EXTEND:
        res = splitVecOp_UnaryOp(n);
        break;
      default:
        if (Util.DEBUG) {
          System.err.printf("splitVectorOperand op #%d: ", opNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to split this operator's operand!");
        break;
    }

    if (res.getNode() == null)
      return false;

    if (res.getNode().equals(n))
      return true;

    Util.assertion(res.getValueType().equals(n.getValueType(0)) && n.getNumValues() == 1, "Invalid operand expansion!");

    replaceValueWith(new SDValue(n, 0), res);
    return false;
  }

  private SDValue splitVecOp_UnaryOp(SDNode n) {
    EVT resVT = n.getValueType(0);
    SDValue[] t = getSplitVector(n.getOperand(0));
    EVT inVT = t[0].getValueType();

    EVT outVT = EVT.getVectorVT(dag.getContext(), resVT.getVectorElementType(),
        inVT.getVectorNumElements());
    SDValue lo = dag.getNode(n.getOpcode(), outVT, t[0]);
    SDValue hi = dag.getNode(n.getOpcode(), outVT, t[1]);
    return dag.getNode(ISD.CONCAT_VECTORS, resVT, lo, hi);
  }

  private SDValue splitVecOp_BIT_CONVERT(SDNode n) {
    SDValue[] t = getSplitVector(n.getOperand(0));
    SDValue lo = t[0], hi = t[1];

    if (tli.isBigEndian()) {
      SDValue temp = lo;
      lo = hi;
      hi = temp;
    }

    return dag.getNode(ISD.BIT_CONVERT, n.getValueType(0),
        joinIntegers(lo, hi));
  }

  private SDValue splitVecOp_EXTRACT_SUBVECTOR(SDNode n) {
    EVT subVT = n.getValueType(0);
    SDValue idx = n.getOperand(0);
    SDValue[] t = getSplitVector(n.getOperand(0));
    SDValue lo = t[0], hi = t[1];

    long loElts = lo.getValueType().getVectorNumElements();
    long idxVal = ((ConstantSDNode) idx.getNode()).getZExtValue();

    if (idxVal < loElts) {
      Util.assertion(idxVal + subVT.getVectorNumElements() <= loElts);
      return dag.getNode(ISD.EXTRACT_SUBVECTOR, subVT, lo, idx);
    } else {
      return dag.getNode(ISD.EXTRACT_SUBVECTOR, subVT, hi,
          dag.getConstant(idxVal - loElts, idx.getValueType(), false));
    }

  }

  private SDValue splitVecOp_EXTRACT_VECTOR_ELT(SDNode n) {
    SDValue vec = n.getOperand(0);
    SDValue idx = n.getOperand(1);
    EVT vecVT = vec.getValueType();

    if (idx.getNode() instanceof ConstantSDNode) {
      long idxVal = ((ConstantSDNode) idx.getNode()).getZExtValue();
      Util.assertion(idxVal < vecVT.getVectorNumElements());

      SDValue[] t = getSplitVector(vec);
      SDValue lo = t[0], hi = t[1];

      long loElts = lo.getValueType().getVectorNumElements();

      if (idxVal < loElts) {
        return dag.updateNodeOperands(new SDValue(n, 0), lo, idx);
      }
      return dag.updateNodeOperands(new SDValue(n, 0), hi,
          dag.getConstant(idxVal - loElts, idx.getValueType(), false));
    }

    EVT eltVT = vecVT.getVectorElementType();

    SDValue stackPtr = dag.createStackTemporary(vecVT);
    int fi = ((FrameIndexSDNode) stackPtr.getNode()).getFrameIndex();
    Value sv = PseudoSourceValue.getFixedStack(fi);
    SDValue store = dag.getStore(dag.getEntryNode(), vec, stackPtr, sv, 0, false, 0);

    stackPtr = getVectorElementPointer(stackPtr, eltVT, idx);
    return dag.getExtLoad(LoadExtType.EXTLOAD, n.getValueType(0),
        store, stackPtr, sv, 0, eltVT);
  }

  private SDValue splitVecOp_STORE(StoreSDNode n, int opNo) {
    Util.assertion(n.isUnindexed(), "Indexed store of vector?");
    Util.assertion(opNo == 1);
    boolean isTruncating = n.isTruncatingStore();
    SDValue chain = n.getChain();
    SDValue ptr = n.getBasePtr();
    int svOffset = n.getSrcValueOffset();
    EVT memoryVT = n.getMemoryVT();
    int alignment = n.getAlignment();
    boolean isVolatile = n.isVolatile();

    SDValue[] t = getSplitVector(n.getOperand(1));
    SDValue lo = t[0], hi = t[1];
    EVT[] vts = getSplitDestVTs(memoryVT);
    EVT loMemVT = vts[0], hiMemVT = vts[1];

    int incrementSize = loMemVT.getSizeInBits() / 8;
    if (isTruncating)
      lo = dag.getTruncStore(chain, lo, ptr, n.getSrcValue(), svOffset,
          loMemVT, isVolatile, alignment);
    else
      lo = dag.getStore(chain, lo, ptr, n.getSrcValue(), svOffset,
          isVolatile, alignment);
    ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
        dag.getIntPtrConstant(incrementSize));
    if (isTruncating)
      hi = dag.getTruncStore(chain, hi, ptr, n.getSrcValue(),
          svOffset + incrementSize, hiMemVT, isVolatile,
          Util.minAlign(alignment, incrementSize));
    else
      hi = dag.getStore(chain, hi, ptr, n.getSrcValue(), svOffset + incrementSize,
          isVolatile, Util.minAlign(alignment, incrementSize));
    return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo, hi);
  }

  private SDValue getWidenedVector(SDValue op) {
    SDValue widenedOp = widenedVectors.get(op);
    widenedOp = remapValue(widenedOp);
    Util.assertion(widenedOp.getNode() != null);
    widenedVectors.put(op, widenedOp);
    return widenedOp;
  }

  private void setWidenedVector(SDValue op, SDValue result) {
    Util.assertion(result.getValueType().equals(tli.getTypeToTransformTo(dag.getContext(), op.getValueType())), "Invalid type for widened vector!");

    result = analyzeNewValue(result);
    Util.assertion(!widenedVectors.containsKey(op));
    widenedVectors.put(op, result);
  }

  // widen Vector Result Promotion.
  private void widenVectorResult(SDNode n, int resNo) {
    if (Util.DEBUG) {
      System.err.printf("Widen node result %d: ", resNo);
      n.dump(dag);
      System.err.println();
    }

    SDValue res = new SDValue();
    switch (n.getOpcode()) {
      case ISD.BIT_CONVERT:
        res = widenVecRes_BIT_CONVERT(n);
        break;
      case ISD.BUILD_VECTOR:
        res = widenVecRes_BUILD_VECTOR(n);
        break;
      case ISD.CONCAT_VECTORS:
        res = widenVecRes_CONCAT_VECTORS(n);
        break;
      case ISD.CONVERT_RNDSAT:
        res = widenVecRes_CONVERT_RNDSAT(n);
        break;
      case ISD.EXTRACT_SUBVECTOR:
        res = widenVecRes_EXTRACT_SUBVECTOR(n);
        break;
      case ISD.INSERT_VECTOR_ELT:
        res = widenVecRes_INSERT_VECTOR_ELT(n);
        break;
      case ISD.LOAD:
        res = widenVecRes_LOAD(n);
        break;
      case ISD.SCALAR_TO_VECTOR:
        res = widenVecRes_SCALAR_TO_VECTOR(n);
        break;
      case ISD.SELECT:
        res = widenVecRes_SELECT(n);
        break;
      case ISD.SELECT_CC:
        res = widenVecRes_SELECT_CC(n);
        break;
      case ISD.UNDEF:
        res = widenVecRes_UNDEF(n);
        break;
      case ISD.VECTOR_SHUFFLE:
        res = widenVecRes_VECTOR_SHUFFLE((ShuffleVectorSDNode) n);
        break;
      case ISD.VSETCC:
        res = widenVecRes_VSETCC(n);
        break;
      case ISD.ADD:
      case ISD.AND:
      case ISD.BSWAP:
      case ISD.FADD:
      case ISD.FCOPYSIGN:
      case ISD.FDIV:
      case ISD.FMUL:
      case ISD.FPOW:
      case ISD.FPOWI:
      case ISD.FREM:
      case ISD.FSUB:
      case ISD.MUL:
      case ISD.MULHS:
      case ISD.MULHU:
      case ISD.OR:
      case ISD.SDIV:
      case ISD.SREM:
      case ISD.UDIV:
      case ISD.UREM:
      case ISD.SUB:
      case ISD.XOR:
        res = widenVecRes_Binary(n);
        break;

      case ISD.SHL:
      case ISD.SRA:
      case ISD.SRL:
        res = widenVecRes_Shift(n);
        break;

      case ISD.FP_ROUND:
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
      case ISD.TRUNCATE:
      case ISD.SIGN_EXTEND:
      case ISD.ZERO_EXTEND:
      case ISD.ANY_EXTEND:
        res = widenVecRes_Convert(n);
        break;

      case ISD.CTLZ:
      case ISD.CTPOP:
      case ISD.CTTZ:
      case ISD.FABS:
      case ISD.FCOS:
      case ISD.FNEG:
      case ISD.FSIN:
      case ISD.FSQRT:
        res = widenVecRes_Unary(n);
        break;
      default:
        if (Util.DEBUG) {
          System.err.print("Don't know how to widen this operator's result!");
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere();
        break;
    }
    if (res.getNode() != null)
      setWidenedVector(new SDValue(n, resNo), res);
  }

  private SDValue widenVecRes_BIT_CONVERT(SDNode n) {
    SDValue inOp = n.getOperand(0);
    EVT inVT = inOp.getValueType();
    EVT vt = n.getValueType(0);
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), vt);

    switch (getTypeAction(inVT)) {
      default:
        Util.assertion(false, "Unknown type action!");
        break;
      case Legal:
        break;
      case PromotedInteger:
        inOp = getPromotedInteger(inOp);
        inVT = inOp.getValueType();
        if (widenVT.bitsEq(inVT))
          return dag.getNode(ISD.BIT_CONVERT, widenVT, inOp);
        break;
      case SoftenFloat:
      case ExpandInteger:
      case ExpandFloat:
      case ScalarizeVector:
      case SplitVector:
        break;
      case WidenVector:
        inOp = getWidenedVector(inOp);
        inVT = inOp.getValueType();
        if (widenVT.bitsEq(inVT))
          return dag.getNode(ISD.BIT_CONVERT, widenVT, inOp);
        break;
    }
    int widenSize = widenVT.getSizeInBits();
    int inSize = inVT.getSizeInBits();
    if ((widenSize % inSize) == 0) {
      EVT newInVT;
      int newNumElts = widenSize / inSize;
      if (inVT.isVector()) {
        EVT inEltVT = inVT.getVectorElementType();
        newInVT = EVT.getVectorVT(dag.getContext(), inEltVT, widenSize / inEltVT.getSizeInBits());
      } else {
        newInVT = EVT.getVectorVT(dag.getContext(), inVT, newNumElts);
      }
      if (tli.isTypeLegal(newInVT)) {
        SDValue[] ops = new SDValue[newNumElts];
        SDValue undefVal = dag.getUNDEF(inVT);
        ops[0] = inOp;
        Arrays.fill(ops, 1, ops.length, undefVal);

        SDValue newVec;
        if (inVT.isVector())
          newVec = dag.getNode(ISD.CONCAT_VECTORS, newInVT, ops);
        else
          newVec = dag.getNode(ISD.BUILD_VECTOR, newInVT, ops);

        return dag.getNode(ISD.BIT_CONVERT, widenVT, newVec);
      }
    }
    return createStackStoreLoad(inOp, widenVT);
  }

  private SDValue widenVecRes_BUILD_VECTOR(SDNode n) {
    EVT vt = n.getValueType(0);
    EVT eltVT = vt.getVectorElementType();
    int numElts = vt.getVectorNumElements();

    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), vt);
    int widenNumElts = widenVT.getVectorNumElements();

    ArrayList<SDValue> newOps = new ArrayList<>();
    newOps.addAll(Arrays.stream(n.getOperandList()).map(SDUse::get).collect(
        Collectors.toList()));
    for (int i = numElts; i < widenNumElts; i++)
      newOps.add(dag.getUNDEF(eltVT));

    return dag.getNode(ISD.BUILD_VECTOR, widenVT, newOps);
  }

  private SDValue widenVecRes_CONCAT_VECTORS(SDNode n) {
    EVT inVT = n.getOperand(0).getValueType();
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    int widenNumElts = widenVT.getVectorNumElements();
    int numOperands = n.getNumOperands();

    boolean inputWidened = false;
    if (getTypeAction(inVT) != LegalizeAction.WidenVector) {
      if ((widenVT.getVectorNumElements() % inVT.getVectorNumElements()) == 0) {
        int numConcat = widenVT.getVectorNumElements() / inVT.getVectorNumElements();
        SDValue undefVal = dag.getUNDEF(inVT);
        ArrayList<SDValue> ops = new ArrayList<>();
        for (int i = 0; i < numOperands; i++)
          ops.add(n.getOperand(i));
        for (int i = numOperands; i < numConcat; i++)
          ops.add(undefVal);

        return dag.getNode(ISD.CONCAT_VECTORS, widenVT, ops);
      }
    } else {
      inputWidened = true;
      if (widenVT.equals(tli.getTypeToTransformTo(dag.getContext(), inVT))) {
        int i;
        for (i = 1; i < numOperands; i++)
          if (n.getOperand(i).getOpcode() != ISD.UNDEF)
            break;

        if (i > numOperands) {
          return getWidenedVector(n.getOperand(0));
        }

        if (numOperands == 2) {
          TIntArrayList maskOps = new TIntArrayList(widenNumElts);
          for (int j = 0; j < widenNumElts / 2; j++) {
            maskOps.set(i, i);
            maskOps.set(i + widenNumElts / 2, i + widenNumElts);
          }
          return dag.getVectorShuffle(widenVT, getWidenedVector(n.getOperand(0)),
              getWidenedVector(n.getOperand(1)), maskOps.toArray());
        }
      }
    }

    EVT eltVT = widenVT.getVectorElementType();
    int numInElts = inVT.getVectorNumElements();

    ArrayList<SDValue> ops = new ArrayList<>();
    int index = 0;
    for (int i = 0; i < numOperands; i++) {
      SDValue inOp = n.getOperand(i);
      if (inputWidened)
        inOp = getWidenedVector(inOp);
      for (int j = 0; j < numInElts; j++)
        ops.add(dag.getNode(ISD.EXTRACT_VECTOR_ELT, eltVT, inOp,
            dag.getIntPtrConstant(j)));
    }
    SDValue undefVal = dag.getUNDEF(eltVT);
    for (; index < widenNumElts; index++)
      ops.add(undefVal);
    return dag.getNode(ISD.BUILD_VECTOR, widenVT, ops);
  }

  private SDValue widenVecRes_CONVERT_RNDSAT(SDNode n) {
    SDValue inOp = n.getOperand(0);
    SDValue rndOp = n.getOperand(3);
    SDValue satOp = n.getOperand(4);

    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    int widenNumElts = widenVT.getVectorNumElements();

    EVT inVT = inOp.getValueType();
    EVT inEltVT = inVT.getVectorElementType();
    EVT inWidenVT = EVT.getVectorVT(dag.getContext(), inEltVT, widenNumElts);

    SDValue dtyOp = dag.getValueType(widenVT);
    SDValue styOp = dag.getValueType(inWidenVT);
    CvtCode cc = ((CvtRndSatSDNode) n).getCvtCode();

    int inVTNumElts = inVT.getVectorNumElements();
    if (getTypeAction(inVT) == LegalizeAction.WidenVector) {
      inOp = getWidenedVector(inOp);
      inVT = inOp.getValueType();
      inVTNumElts = inVT.getVectorNumElements();
      if (inVTNumElts == widenNumElts)
        return dag.getConvertRndSat(widenVT, inOp, dtyOp, styOp, rndOp,
            satOp, cc);
    }

    if (tli.isTypeLegal(inWidenVT)) {
      if ((widenNumElts % inVTNumElts) == 0) {
        int numConcat = widenNumElts / inVTNumElts;
        SDValue[] ops = new SDValue[numConcat];
        ops[0] = inOp;
        SDValue undefVal = dag.getUNDEF(inVT);
        Arrays.fill(ops, 1, ops.length, undefVal);

        inOp = dag.getNode(ISD.CONCAT_VECTORS, inWidenVT, ops);
        return dag.getConvertRndSat(widenVT, inOp, dtyOp, styOp, rndOp,
            satOp, cc);
      }

      if ((inVTNumElts % widenNumElts) == 0) {
        inOp = dag.getNode(ISD.EXTRACT_SUBVECTOR, inWidenVT, inOp,
            dag.getIntPtrConstant(0));
        return dag.getConvertRndSat(widenVT, inOp, dtyOp, styOp, rndOp,
            satOp, cc);
      }
    }

    ArrayList<SDValue> ops = new ArrayList<>();
    EVT eltVT = widenVT.getVectorElementType();
    dtyOp = dag.getValueType(eltVT);
    styOp = dag.getValueType(inEltVT);

    int minElts = Math.min(inVTNumElts, widenNumElts);
    int i = 0;
    for (; i < minElts; i++) {
      SDValue extVal = dag.getNode(ISD.EXTRACT_VECTOR_ELT, inEltVT, inOp,
          dag.getIntPtrConstant(i));
      ops.add(dag.getConvertRndSat(widenVT, extVal, dtyOp, styOp,
          rndOp, satOp, cc));
    }

    SDValue undefVal = dag.getUNDEF(eltVT);
    for (; i < widenNumElts; i++)
      ops.add(undefVal);

    return dag.getNode(ISD.BUILD_VECTOR, widenVT, ops);
  }

  private SDValue widenVecRes_EXTRACT_SUBVECTOR(SDNode n) {
    EVT vt = n.getValueType(0);
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), vt);
    int widenNumElts = widenVT.getVectorNumElements();
    SDValue inOp = n.getOperand(0);
    SDValue idx = n.getOperand(1);

    if (getTypeAction(inOp.getValueType()) == LegalizeAction.WidenVector)
      inOp = getWidenedVector(inOp);

    EVT inVT = inOp.getValueType();
    boolean isCnt = idx.getNode() instanceof ConstantSDNode;
    if (isCnt) {
      ConstantSDNode csd = (ConstantSDNode) idx.getNode();
      long idxVal = csd.getZExtValue();
      if (idxVal == 0 && inVT.equals(widenVT))
        return inOp;

      int inNumElts = inVT.getVectorNumElements();
      if ((idxVal % widenNumElts) == 0 && idxVal + widenNumElts < inNumElts)
        return dag.getNode(ISD.EXTRACT_SUBVECTOR, widenVT, inOp, idx);
    }

    ArrayList<SDValue> ops = new ArrayList<>();
    EVT eltVT = vt.getVectorElementType();
    EVT idxVT = idx.getValueType();
    int numElts = vt.getVectorNumElements();
    int i;
    if (isCnt) {
      ConstantSDNode csd = (ConstantSDNode) idx.getNode();
      long idxVal = csd.getZExtValue();
      for (i = 0; i < numElts; i++)
        ops.add(dag.getNode(ISD.EXTRACT_VECTOR_ELT, eltVT, inOp,
            dag.getConstant(idxVal + i, idxVT, false)));
    } else {
      ops.add(dag.getNode(ISD.EXTRACT_VECTOR_ELT, eltVT, inOp, idx));
      for (i = 1; i < numElts; i++) {
        SDValue newIdx = dag.getNode(ISD.ADD, idx.getValueType(), idx,
            dag.getConstant(i, idxVT, false));
        ops.add(dag.getNode(ISD.EXTRACT_VECTOR_ELT, eltVT, inOp, newIdx));
      }
    }

    SDValue undefVal = dag.getUNDEF(eltVT);
    for (; i < widenNumElts; i++)
      ops.add(undefVal);
    return dag.getNode(ISD.BUILD_VECTOR, widenVT, ops);
  }

  private SDValue widenVecRes_INSERT_VECTOR_ELT(SDNode n) {
    SDValue inOp = getWidenedVector(n.getOperand(0));
    return dag.getNode(ISD.INSERT_VECTOR_ELT, inOp.getValueType(),
        inOp, n.getOperand(1), n.getOperand(2));
  }

  private SDValue widenVecRes_LOAD(SDNode n) {
    LoadSDNode ld = (LoadSDNode) n;
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), ld.getValueType(0));
    EVT ldVT = ld.getMemoryVT();
    Util.assertion(ldVT.isVector() && widenVT.isVector());

    SDValue chain = ld.getChain();
    SDValue basePtr = ld.getBasePtr();
    int svOffset = ld.getSrcValueOffset();
    int alignment = ld.getAlignment();
    boolean isVolatile = ld.isVolatile();
    Value sv = ld.getSrcValue();
    LoadExtType extType = ld.getExtensionType();

    SDValue result;
    ArrayList<SDValue> ldChain = new ArrayList<>();
    if (extType != LoadExtType.NON_EXTLOAD) {
      EVT eltVT = widenVT.getVectorElementType();
      EVT ldEltVT = ldVT.getVectorElementType();
      int numElts = ldVT.getVectorNumElements();

      int widenNumElts = widenVT.getVectorNumElements();
      SDValue[] ops = new SDValue[widenNumElts];
      int increment = ldEltVT.getSizeInBits() / 8;
      ops[0] = dag.getExtLoad(extType, eltVT, chain, basePtr, sv,
          svOffset, ldEltVT, isVolatile, alignment);
      ldChain.add(ops[0].getValue(1));

      int i = 0, offset = increment;
      for (i = 1; i < numElts; i++, offset += increment) {
        SDValue newBasePtr = dag.getNode(ISD.ADD, basePtr.getValueType(),
            basePtr, dag.getIntPtrConstant(offset));
        ops[i] = dag.getExtLoad(extType, eltVT, chain, newBasePtr, sv,
            svOffset + offset, ldEltVT, isVolatile, alignment);
        ldChain.add(ops[i].getValue(1));
      }

      SDValue undefVal = dag.getUNDEF(eltVT);
      Arrays.fill(ops, i, widenNumElts, undefVal);

      result = dag.getNode(ISD.BUILD_VECTOR, widenVT, ops);
    } else {
      Util.assertion(ldVT.getVectorElementType().equals(widenVT.getVectorElementType()));
      int ldWidth = ldVT.getSizeInBits();
      result = genWidenVectorLoads(ldChain, chain, basePtr, sv, svOffset,
          alignment, isVolatile, ldWidth, widenVT);
    }

    SDValue newChain = new SDValue();
    if (ldChain.size() == 1)
      newChain = ldChain.get(0);
    else {
      newChain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
          ldChain);
    }

    replaceValueWith(new SDValue(n, 1), newChain);
    return result;
  }

  private SDValue widenVecRes_SCALAR_TO_VECTOR(SDNode n) {
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    return dag.getNode(ISD.SCALAR_TO_VECTOR, widenVT, n.getOperand(0));
  }

  private SDValue widenVecRes_SELECT(SDNode n) {
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    int widenNumElts = widenVT.getVectorNumElements();

    SDValue cond = n.getOperand(0);
    EVT condVT = cond.getValueType();
    if (condVT.isVector()) {
      EVT convEltVT = condVT.getVectorElementType();
      EVT condWidenVT = EVT.getVectorVT(dag.getContext(), convEltVT, widenNumElts);
      if (getTypeAction(condVT) == LegalizeAction.WidenVector)
        cond = getWidenedVector(cond);

      if (!cond.getValueType().equals(condWidenVT))
        cond = modifyToType(cond, condWidenVT);
    }

    SDValue inOp1 = getWidenedVector(n.getOperand(1));
    SDValue inOp2 = getWidenedVector(n.getOperand(2));
    Util.assertion(inOp1.getValueType().equals(widenVT) && inOp2.getValueType().equals(widenVT));

    return dag.getNode(ISD.SELECT, widenVT, cond, inOp1, inOp2);
  }

  private SDValue widenVecRes_SELECT_CC(SDNode n) {
    SDValue inOp1 = getWidenedVector(n.getOperand(2));
    SDValue inOp2 = getWidenedVector(n.getOperand(3));
    return dag.getNode(ISD.SELECT_CC, inOp1.getValueType(), n.getOperand(0),
        n.getOperand(1), inOp1, inOp2, n.getOperand(4));
  }

  private SDValue widenVecRes_UNDEF(SDNode n) {
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    return dag.getUNDEF(widenVT);
  }

  private SDValue widenVecRes_VECTOR_SHUFFLE(ShuffleVectorSDNode n) {
    EVT vt = n.getValueType(0);
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), vt);
    int numElts = vt.getVectorNumElements();
    int widenNumElts = widenVT.getVectorNumElements();

    SDValue inOp1 = getWidenedVector(n.getOperand(0));
    SDValue inOp2 = getWidenedVector(n.getOperand(1));

    TIntArrayList newMask = new TIntArrayList();
    for (int i = 0; i < numElts; i++) {
      int idx = n.getMaskElt(i);
      if (idx < numElts)
        newMask.add(idx);
      else
        newMask.add(idx - numElts + widenNumElts);
    }

    for (int i = numElts; i < widenNumElts; i++)
      newMask.add(-1);
    return dag.getVectorShuffle(widenVT, inOp1, inOp2, newMask.toArray());
  }

  private SDValue widenVecRes_VSETCC(SDNode n) {
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));

    int widenNumElts = widenVT.getVectorNumElements();
    SDValue inOp1 = n.getOperand(0);
    EVT inVT = inOp1.getValueType();
    Util.assertion(inVT.isVector(), "Can't widen not vector type!");
    EVT widenInVT = EVT.getVectorVT(dag.getContext(), inVT.getVectorElementType(), widenNumElts);
    inOp1 = getWidenedVector(inOp1);
    SDValue inOp2 = getWidenedVector(n.getOperand(1));

    Util.assertion(inOp1.getValueType().equals(widenInVT) && inOp2.getValueType().equals(widenInVT));

    return dag.getNode(ISD.VSETCC, widenVT, inOp1, inOp2, n.getOperand(2));
  }

  private SDValue widenVecRes_Binary(SDNode n) {
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue inOp1 = getWidenedVector(n.getOperand(0));
    SDValue inOp2 = getWidenedVector(n.getOperand(1));
    return dag.getNode(n.getOpcode(), widenVT, inOp1, inOp2);
  }

  private SDValue widenVecRes_Convert(SDNode n) {
    SDValue inOp = n.getOperand(0);
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    int widenNumElts = widenVT.getVectorNumElements();

    EVT inVT = inOp.getValueType();
    EVT inEltVT = inVT.getVectorElementType();
    EVT inWidenVT = EVT.getVectorVT(dag.getContext(), inEltVT, widenNumElts);

    int opc = n.getOpcode();
    int inVTNumElts = inVT.getVectorNumElements();
    if (getTypeAction(inVT) == LegalizeAction.WidenVector) {
      inOp = getWidenedVector(n.getOperand(0));
      inVT = inOp.getValueType();
      inVTNumElts = inVT.getVectorNumElements();
      if (inVTNumElts == widenNumElts)
        return dag.getNode(opc, widenVT, inOp);
    }

    if (tli.isTypeLegal(inWidenVT)) {
      if ((widenNumElts % inVTNumElts) == 0) {
        int numConcat = widenNumElts / inVTNumElts;
        SDValue[] ops = new SDValue[numConcat];
        ops[0] = inOp;
        SDValue undefVal = dag.getUNDEF(inVT);
        Arrays.fill(ops, 1, ops.length, undefVal);
        return dag.getNode(opc, widenVT, dag.getNode(ISD.CONCAT_VECTORS,
            inWidenVT, ops));
      }

      if ((inVTNumElts % widenNumElts) == 0) {
        return dag.getNode(opc, widenVT,
            dag.getNode(ISD.EXTRACT_SUBVECTOR, inWidenVT,
                inOp, dag.getIntPtrConstant(0)));
      }
    }

    SDValue[] ops = new SDValue[widenNumElts];
    EVT eltVT = widenVT.getVectorElementType();
    int minElts = Math.min(inVTNumElts, widenNumElts);
    int i = 0;
    for (; i < minElts; i++)
      ops[i] = dag.getNode(opc, eltVT, dag.getNode(ISD.EXTRACT_VECTOR_ELT,
          inEltVT, inOp, dag.getIntPtrConstant(i)));

    SDValue undefVal = dag.getUNDEF(eltVT);
    Arrays.fill(ops, i, widenNumElts, undefVal);
    return dag.getNode(ISD.BUILD_VECTOR, widenVT, ops);
  }

  private SDValue widenVecRes_Shift(SDNode n) {
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue inOp = getWidenedVector(n.getOperand(0));
    SDValue shOp = n.getOperand(1);

    EVT shVT = shOp.getValueType();
    if (getTypeAction(shVT) == LegalizeAction.WidenVector) {
      shOp = getWidenedVector(shOp);
      shVT = shOp.getValueType();
    }

    EVT shWidenVT = EVT.getVectorVT(dag.getContext(), shVT.getVectorElementType(),
        widenVT.getVectorNumElements());
    if (!shVT.equals(shWidenVT))
      shOp = modifyToType(shOp, shWidenVT);
    return dag.getNode(n.getOpcode(), widenVT, inOp, shOp);
  }

  private SDValue widenVecRes_Unary(SDNode n) {
    EVT widenVT = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue inOp = getWidenedVector(n.getOperand(0));
    return dag.getNode(n.getOpcode(), widenVT, inOp);
  }

  // widen Vector Operand.
  private boolean widenVectorOperand(SDNode n, int resNo) {
    if (Util.DEBUG) {
      System.err.printf("Widen node operand %d: ", resNo);
      n.dump(dag);
      System.err.println();
    }
    SDValue res = new SDValue();
    switch (n.getOpcode()) {
      default:
        if (Util.DEBUG) {
          System.err.printf("widenVectorOperand op #%d: ", resNo);
          n.dump(dag);
          System.err.println();
        }
        Util.shouldNotReachHere("Don't know how to widen this operand!");
        break;
      case ISD.BIT_CONVERT:
        res = widenVecOp_BIT_CONVERT(n);
        break;
      case ISD.CONCAT_VECTORS:
        res = widenVecOp_CONCAT_VECTORS(n);
        break;
      case ISD.EXTRACT_VECTOR_ELT:
        res = widenVecOp_EXTRACT_VECTOR_ELT(n);
        break;
      case ISD.STORE:
        res = widenVecOp_STORE(n);
        break;

      case ISD.FP_ROUND:
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
      case ISD.TRUNCATE:
      case ISD.SIGN_EXTEND:
      case ISD.ZERO_EXTEND:
      case ISD.ANY_EXTEND:
        res = widenVecOp_Convert(n);
        break;
    }
    if (res.getNode() == null)
      return false;

    if (res.getNode().equals(n))
      return true;

    Util.assertion(res.getValueType().equals(n.getValueType(0)) && n.getNumValues() == 1, "Invalid operand expansion!");

    replaceValueWith(new SDValue(n, 0), res);
    return false;
  }

  private SDValue widenVecOp_BIT_CONVERT(SDNode n) {
    EVT vt = n.getValueType(0);
    SDValue inOp = getWidenedVector(n.getOperand(0));
    EVT inWidenVT = inOp.getValueType();
    int inWidenSize = inWidenVT.getSizeInBits();
    int size = vt.getSizeInBits();
    if ((inWidenSize % size) == 0 && !vt.isVector()) {
      int newNumElts = inWidenSize / size;
      EVT newVT = EVT.getVectorVT(dag.getContext(), vt, newNumElts);
      if (tli.isTypeLegal(newVT)) {
        SDValue bitOp = dag.getNode(ISD.BIT_CONVERT, newVT, inOp);
        return dag.getNode(ISD.EXTRACT_VECTOR_ELT, vt, bitOp,
            dag.getIntPtrConstant(0));
      }
    }
    return createStackStoreLoad(inOp, vt);
  }

  private SDValue widenVecOp_CONCAT_VECTORS(SDNode n) {
    EVT vt = n.getValueType(0);
    EVT eltVT = vt.getVectorElementType();
    int numElts = vt.getVectorNumElements();
    SDValue[] ops = new SDValue[numElts];

    EVT inVT = n.getOperand(0).getValueType();
    int numInElts = inVT.getVectorNumElements();

    int idx = 0;
    int numOperands = n.getNumOperands();
    for (int i = 0; i < numOperands; i++) {
      SDValue inOp = n.getOperand(i);
      if (getTypeAction(inOp.getValueType()) == LegalizeAction.WidenVector) {
        inOp = getWidenedVector(inOp);
      }
      for (int j = 0; j < numInElts; j++)
        ops[idx++] = dag.getNode(ISD.EXTRACT_VECTOR_ELT, eltVT,
            inOp, dag.getIntPtrConstant(j));
    }

    return dag.getNode(ISD.BUILD_VECTOR, vt, ops);
  }

  private SDValue widenVecOp_EXTRACT_VECTOR_ELT(SDNode n) {
    SDValue inOp = getWidenedVector(n.getOperand(0));
    return dag.getNode(ISD.EXTRACT_VECTOR_ELT, n.getValueType(0),
        inOp, n.getOperand(1));
  }

  private SDValue widenVecOp_STORE(SDNode n) {
    StoreSDNode st = (StoreSDNode) n;
    SDValue chain = st.getChain();
    SDValue basePtr = st.getBasePtr();
    Value sv = st.getSrcValue();
    int svOffset = st.getSrcValueOffset();
    boolean isVolatile = st.isVolatile();
    int alignmen = st.getAlignment();
    SDValue valOp = getWidenedVector(st.getValue());

    EVT stVT = st.getMemoryVT();
    EVT valVT = valOp.getValueType();

    Util.assertion(stVT.isVector() && valOp.getValueType().isVector());
    Util.assertion(stVT.bitsLT(valOp.getValueType()));

    ArrayList<SDValue> stChain = new ArrayList<>();
    if (st.isTruncatingStore()) {
      EVT stEltVT = stVT.getVectorElementType();
      EVT valEltVT = valVT.getVectorElementType();
      int increment = valEltVT.getSizeInBits() / 8;
      int numElts = stVT.getVectorNumElements();
      SDValue eop = dag.getNode(ISD.EXTRACT_VECTOR_ELT, valEltVT, valOp,
          dag.getIntPtrConstant(0));
      stChain.add(dag.getTruncStore(chain, eop, basePtr, sv, svOffset,
          stEltVT, isVolatile, alignmen));
      int offset = increment;
      for (int i = 1; i < numElts; i++, offset += increment) {
        SDValue newBasePtr = dag.getNode(ISD.ADD, basePtr.getValueType(),
            basePtr, dag.getIntPtrConstant(offset));
        eop = dag.getNode(ISD.EXTRACT_VECTOR_ELT, valEltVT, valOp,
            dag.getIntPtrConstant(0));
        stChain.add(dag.getTruncStore(chain, eop, newBasePtr, sv,
            svOffset + offset, stEltVT, isVolatile,
            Util.minAlign(alignmen, offset)));
      }
    } else {
      Util.assertion(stVT.getVectorElementType().equals(valVT.getVectorElementType()));
      genWidenVectorStores(stChain, chain, basePtr, sv, svOffset,
          alignmen, isVolatile, valOp, stVT.getSizeInBits());
    }

    if (stChain.size() == 1)
      return stChain.get(0);
    else
      return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), stChain);
  }

  private SDValue widenVecOp_Convert(SDNode n) {
    EVT vt = n.getValueType(0);
    EVT eltVT = vt.getVectorElementType();
    int numElts = vt.getVectorNumElements();
    SDValue inOp = n.getOperand(0);
    if (getTypeAction(inOp.getValueType()) == LegalizeAction.WidenVector)
      inOp = getWidenedVector(inOp);

    EVT inVT = inOp.getValueType();
    EVT inEltVT = inVT.getVectorElementType();

    int opcode = n.getOpcode();
    SDValue[] ops = new SDValue[numElts];
    for (int i = 0; i < numElts; i++)
      ops[i] = dag.getNode(opcode, eltVT, dag.getNode(ISD.EXTRACT_VECTOR_ELT,
          inEltVT, inOp, dag.getIntPtrConstant(i)));

    return dag.getNode(ISD.BUILD_VECTOR, vt, ops);
  }

  private static EVT[] findAssociateWidenVecType(SelectionDAG dag,
                                                 TargetLowering tli, int width, EVT vecVT,
                                                 EVT newEltVT, EVT newVecVT) {
    int eltWidth = width + 1;
    if (tli.isTypeLegal(vecVT)) {
      do {
        Util.assertion(eltWidth > 0);
        eltWidth = 1 << Util.log2(eltWidth - 1);
        newEltVT = EVT.getIntegerVT(dag.getContext(), eltWidth);
        int numElts = vecVT.getSizeInBits() / eltWidth;
        newVecVT = EVT.getVectorVT(dag.getContext(), newEltVT, numElts);
      } while (!tli.isTypeLegal(newVecVT) || vecVT.getSizeInBits() !=
          newVecVT.getSizeInBits());
    } else {
      do {
        Util.assertion(eltWidth > 0);
        eltWidth = 1 << Util.log2(eltWidth - 1);
        newEltVT = EVT.getIntegerVT(dag.getContext(), eltWidth);
        int numElts = vecVT.getSizeInBits() / eltWidth;
        newVecVT = EVT.getVectorVT(dag.getContext(), newEltVT, numElts);
      } while (!tli.isTypeLegal(newEltVT) || vecVT.getSizeInBits() !=
          newVecVT.getSizeInBits());
    }
    return new EVT[]{newEltVT, newVecVT};
  }

  /**
   * Helper function to generate a set of loads to load a vector with a
   * resulting wider type. It takes
   *
   * @param ldChain    list of chains for the load we have generated.
   * @param chain      incoming chain for the ld vector.
   * @param basePtr    base pointer to load from.
   * @param sv         memory disambiguation source value.
   * @param svOffset   memory disambiugation offset.
   * @param alignment  alignment of the memory.
   * @param isVolatile volatile load.
   * @param ldWidth    width of memory that we want to load.
   * @param resType    the wider result result type for the resulting vector.
   * @return
   */
  private SDValue genWidenVectorLoads(ArrayList<SDValue> ldChain, SDValue chain,
                                      SDValue basePtr, Value sv,
                                      int svOffset, int alignment,
                                      boolean isVolatile, int ldWidth,
                                      EVT resType) {
    EVT[] vts = findAssociateWidenVecType(dag, tli, ldWidth, resType, new EVT(), new EVT());
    EVT newEltVT = vts[0];
    EVT newVecVT = vts[1];
    int newEltVTWidth = newEltVT.getSizeInBits();

    SDValue ldOp = dag
        .getLoad(newEltVT, chain, basePtr, sv, svOffset, isVolatile,
            alignment);
    SDValue vecOp = dag.getNode(ISD.SCALAR_TO_VECTOR, newVecVT, ldOp);
    ldChain.add(ldOp.getValue(1));

    if (ldWidth == newEltVTWidth) {
      return dag.getNode(ISD.BIT_CONVERT, resType, vecOp);
    }

    int idx = 1;
    ldWidth -= newEltVTWidth;
    int offset = 0;
    while (ldWidth > 0) {
      int increment = newEltVTWidth / 8;
      offset += increment;
      basePtr = dag.getNode(ISD.ADD, basePtr.getValueType(), basePtr,
          dag.getIntPtrConstant(increment));

      if (ldWidth < newEltVTWidth) {
        int oNewEltVTWidth = newEltVTWidth;
        vts = findAssociateWidenVecType(dag, tli, ldWidth, resType, newEltVT,
            newVecVT);
        newEltVT = vts[0];
        newVecVT = vts[1];
        newEltVTWidth = newEltVT.getSizeInBits();
        idx = idx * (oNewEltVTWidth / newEltVTWidth);
        vecOp = dag.getNode(ISD.BIT_CONVERT, newVecVT, vecOp);
      }

      ldOp = dag.getLoad(newEltVT, chain, basePtr, sv,
          svOffset + offset, isVolatile,
          Util.minAlign(alignment, offset));
      ldChain.add(ldOp.getValue(1));
      vecOp = dag.getNode(ISD.INSERT_VECTOR_ELT, newVecVT, vecOp,
          ldOp, dag.getIntPtrConstant(idx++));
      ldWidth -= newEltVTWidth;
    }
    return dag.getNode(ISD.BIT_CONVERT, resType, vecOp);
  }

  /**
   * Helper function to generate a set of
   * stores to store a widen vector into non widen memory
   *
   * @param stChain    list of chains for the store we have generated.
   * @param chain      incoming chain for the ld vector.
   * @param basePtr    base pointer to store from.
   * @param sv         memory disambiguation source value.
   * @param svOffset   memory disambiugation offset.
   * @param alignment  alignment of the memory.
   * @param isVolatile volatile store.
   * @param valOp      value to store
   * @param stWidth    width of memory that we want to store.
   */
  private void genWidenVectorStores(ArrayList<SDValue> stChain,
                                    SDValue chain,
                                    SDValue basePtr, Value sv,
                                    int svOffset, int alignment,
                                    boolean isVolatile, SDValue valOp,
                                    int stWidth) {
    EVT widenVT = valOp.getValueType();

    EVT[] vts = findAssociateWidenVecType(dag, tli, stWidth, widenVT, new EVT(), new EVT());
    EVT newEltVT = vts[0], newVecVT = vts[1];
    int newEltVTWidth = newEltVT.getSizeInBits();

    SDValue vecOp = dag.getNode(ISD.BIT_CONVERT, newVecVT, valOp);
    SDValue eop = dag.getNode(ISD.EXTRACT_VECTOR_ELT, newEltVT, vecOp,
        dag.getIntPtrConstant(0));
    SDValue stop = dag.getStore(chain, eop, basePtr, sv, svOffset,
        isVolatile, alignment);

    stChain.add(stop);

    if (stWidth == newEltVTWidth)
      return;

    int idx = 1;
    stWidth -= newEltVTWidth;
    int offset = 0;
    while (stWidth > 0) {
      int increment = newEltVTWidth / 8;
      offset += increment;
      basePtr = dag.getNode(ISD.ADD, basePtr.getValueType(), basePtr,
          dag.getIntPtrConstant(increment));

      if (stWidth < newEltVTWidth) {
        int oNewEltVTWidth = newEltVTWidth;
        vts = findAssociateWidenVecType(dag, tli, stWidth, widenVT, newEltVT, newVecVT);
        newEltVT = vts[0];
        newVecVT = vts[1];
        newEltVTWidth = newEltVT.getSizeInBits();
        idx = idx * (oNewEltVTWidth / newEltVTWidth);
        vecOp = dag.getNode(ISD.BIT_CONVERT, newVecVT, vecOp);
      }

      eop = dag.getNode(ISD.EXTRACT_VECTOR_ELT, newVecVT, vecOp,
          dag.getIntPtrConstant(idx++));
      stChain.add(dag.getStore(chain, eop, basePtr, sv, svOffset + offset,
          isVolatile, Util.minAlign(alignment, offset)));
      stWidth -= newEltVTWidth;
    }
  }

  /**
   * Modifies a vector input (widen or narrows) to a vector of NVT.  The
   * input vector must have the same element type as NVT.
   *
   * @param inOp
   * @param nvt
   * @return
   */
  private SDValue modifyToType(SDValue inOp, EVT nvt) {
    EVT inVT = inOp.getValueType();
    Util.assertion(inVT.getVectorElementType().equals(nvt.getVectorElementType()), "Input and widen element type must match!");


    if (inVT.equals(nvt))
      return inOp;

    int inNumElts = inVT.getVectorNumElements();
    int widenNumElts = nvt.getVectorNumElements();
    if (widenNumElts > inNumElts && (widenNumElts % inNumElts) == 0) {
      int numConcat = widenNumElts / inNumElts;
      SDValue[] ops = new SDValue[numConcat];
      SDValue undefVal = dag.getUNDEF(inVT);
      ops[0] = inOp;
      Arrays.fill(ops, 1, numConcat, undefVal);
      return dag.getNode(ISD.CONCAT_VECTORS, nvt, ops);
    }

    if (widenNumElts < inNumElts && (inNumElts % widenNumElts) != 0) {
      return dag.getNode(ISD.EXTRACT_SUBVECTOR, nvt, inOp,
          dag.getIntPtrConstant(0));
    }

    SDValue[] ops = new SDValue[widenNumElts];
    EVT eltVT = nvt.getVectorElementType();
    int minNumElts = Math.min(widenNumElts, inNumElts);
    int idx = 0;
    for (; idx < minNumElts; idx++)
      ops[idx] = dag.getNode(ISD.EXTRACT_VECTOR_ELT, eltVT, inOp,
          dag.getIntPtrConstant(idx));

    SDValue undefVal = dag.getUNDEF(eltVT);
    Arrays.fill(ops, idx, widenNumElts, undefVal);
    return dag.getNode(ISD.BUILD_VECTOR, nvt, ops);
  }

  private SDValue[] getSplitOp(SDValue op) {
    if (op.getValueType().isVector())
      return getSplitVector(op);
    else if (op.getValueType().isInteger())
      return getExpandedInteger(op);
    return getExpandedFloat(op);
  }

  /**
   * Compute the VTs needed for the low/hi parts of a type
   * which is split (or expanded) into two not necessarily identical pieces.
   *
   * @param inVT
   * @return
   */
  private EVT[] getSplitDestVTs(EVT inVT) {
    EVT loVT, hiVT;
    if (!inVT.isVector()) {
      loVT = hiVT = tli.getTypeToTransformTo(dag.getContext(), inVT);
    } else {
      int numElts = inVT.getVectorNumElements();
      Util.assertion((numElts & 1) == 0, "Splitting vector, but not in half!");
      loVT = hiVT = EVT.getVectorVT(dag.getContext(), inVT.getVectorElementType(), numElts / 2);
    }
    return new EVT[]{loVT, hiVT};
  }

  /**
   * Use {@linkplain ISD#EXTRACT_ELEMENT} nodes to extract the low and
   * high parts of the given value.
   *
   * @param pair
   */
  private SDValue[] getPairElements(SDValue pair) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), pair.getValueType());
    SDValue lo = dag.getNode(ISD.EXTRACT_ELEMENT, nvt, pair,
        dag.getIntPtrConstant(0));
    SDValue hi = dag.getNode(ISD.EXTRACT_ELEMENT, nvt, pair,
        dag.getIntPtrConstant(1));
    return new SDValue[]{lo, hi};
  }

  // Generic Result Splitting.
  private SDValue[] splitRes_MERGE_VALUES(SDNode n) {
    int i = 0;
    for (; isTypeLegal(n.getValueType(i)); i++)
      replaceValueWith(new SDValue(n, i),
          new SDValue(n.getOperand(i).getNode(), 0));

    SDValue[] t = getSplitOp(n.getOperand(i));
    int e = n.getNumValues();
    for (++i; i < e; i++) {
      replaceValueWith(new SDValue(n, i),
          new SDValue(n.getOperand(i).getNode(), 0));
    }
    return t;
  }

  private SDValue[] splitRes_SELECT(SDNode n) {
    SDValue[] lhsT = getSplitOp(n.getOperand(1));
    SDValue[] rhsT = getSplitOp(n.getOperand(2));

    SDValue cond = n.getOperand(0);
    SDValue lo = dag.getNode(ISD.SELECT, lhsT[0].getValueType(), cond,
        lhsT[0], rhsT[0]);
    SDValue hi = dag.getNode(ISD.SELECT, lhsT[1].getValueType(), cond,
        lhsT[1], rhsT[1]);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitRes_SELECT_CC(SDNode n) {
    SDValue[] lhsT = getSplitOp(n.getOperand(2));
    SDValue[] rhsT = getSplitOp(n.getOperand(3));

    SDValue lo = dag.getNode(ISD.SELECT_CC, lhsT[0].getValueType(),
        n.getOperand(0), n.getOperand(1),
        lhsT[0], rhsT[0], n.getOperand(4));
    SDValue hi = dag.getNode(ISD.SELECT_CC, lhsT[1].getValueType(),
        n.getOperand(0), n.getOperand(1),
        lhsT[1], rhsT[1], n.getOperand(4));
    return new SDValue[]{lo, hi};
  }

  private SDValue[] splitRes_UNDEF(SDNode n) {
    EVT[] vts = getSplitDestVTs(n.getValueType(0));
    return new SDValue[]{dag.getUNDEF(vts[0]), dag.getUNDEF(vts[1])};
  }

  //===--------------------------------------------------------------------===//
  // Generic Expansion: LegalizeTypesGeneric.cpp
  //===--------------------------------------------------------------------===//

  /**
   * Legalization methods which only use that the illegal type is split into two
   * identical types of half the size, and that the Lo/Hi part is stored first
   * in memory on little/big-endian machines, followed by the Hi/Lo part.  As
   * such they can be used for expanding integers and floats.
   *
   * @param op
   * @return
   */
  private SDValue[] getExpandedOp(SDValue op) {
    if (op.getValueType().isInteger())
      return getExpandedInteger(op);
    return getExpandedFloat(op);
  }

  // Generic Result Expansion.
  private SDValue[] expandRes_BIT_CONVERT(SDNode n) {
    EVT outVT = n.getValueType(0);
    EVT nOutVT = tli.getTypeToTransformTo(dag.getContext(), outVT);
    SDValue inOp = n.getOperand(0);
    EVT inVT = inOp.getValueType();

    SDValue lo, hi;
    SDValue[] t;
    switch (getTypeAction(inVT)) {
      default:
        Util.assertion(false, "Unknown type action!");
      case Legal:
      case PromotedInteger:
        break;
      case SoftenFloat:
        t = splitInteger(getSoftenedFloat(inOp));
        lo = dag.getNode(ISD.BIT_CONVERT, nOutVT, t[0]);
        hi = dag.getNode(ISD.BIT_CONVERT, nOutVT, t[1]);
        return new SDValue[]{lo, hi};
      case ExpandInteger:
      case ExpandFloat:
        t = getExpandedOp(inOp);
        lo = dag.getNode(ISD.BIT_CONVERT, nOutVT, t[0]);
        hi = dag.getNode(ISD.BIT_CONVERT, nOutVT, t[1]);
        return new SDValue[]{lo, hi};
      case SplitVector:
        t = getSplitVector(inOp);
        lo = t[0];
        hi = t[1];
        if (tli.isBigEndian()) {
          SDValue temp = lo;
          lo = hi;
          hi = temp;
        }
        lo = dag.getNode(ISD.BIT_CONVERT, nOutVT, lo);
        hi = dag.getNode(ISD.BIT_CONVERT, nOutVT, hi);
        return new SDValue[]{lo, hi};
      case ScalarizeVector:
        t = splitInteger(bitConvertToInteger(getScalarizedVector(inOp)));
        lo = t[0];
        hi = t[1];
        lo = dag.getNode(ISD.BIT_CONVERT, nOutVT, lo);
        hi = dag.getNode(ISD.BIT_CONVERT, nOutVT, hi);
        return new SDValue[]{lo, hi};
      case WidenVector:
        Util.assertion((inVT.getVectorNumElements() & 1) == 0, "Unsupported BIT_CONVERT!");
        inOp = getWidenedVector(inOp);
        EVT inNVT = EVT.getVectorVT(dag.getContext(), inVT.getVectorElementType(),
            inVT.getVectorNumElements() / 2);
        lo = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, inOp,
            dag.getIntPtrConstant(0));
        hi = dag.getNode(ISD.EXTRACT_SUBVECTOR, inNVT, inOp,
            dag.getIntPtrConstant(0));
        if (tli.isBigEndian()) {
          SDValue temp = lo;
          lo = hi;
          hi = temp;
        }
        lo = dag.getNode(ISD.BIT_CONVERT, nOutVT, lo);
        hi = dag.getNode(ISD.BIT_CONVERT, nOutVT, hi);
        return new SDValue[]{lo, hi};
    }

    if (inVT.isVector() && outVT.isInteger()) {
      EVT nvt = EVT.getVectorVT(dag.getContext(), nOutVT, 2);
      if (isTypeLegal(nvt)) {
        SDValue castInOp = dag.getNode(ISD.BIT_CONVERT, nvt, inOp);
        lo = dag.getNode(ISD.EXTRACT_VECTOR_ELT, nOutVT, castInOp,
            dag.getIntPtrConstant(0));
        hi = dag.getNode(ISD.EXTRACT_VECTOR_ELT, nOutVT, castInOp,
            dag.getIntPtrConstant(1));
        if (tli.isBigEndian()) {
          SDValue temp = lo;
          lo = hi;
          hi = temp;
        }
        return new SDValue[]{lo, hi};
      }
    }

    Util.assertion(nOutVT.isByteSized(), "Expanded type not byte sized!");

    int alignment = tli.getTargetData().getPrefTypeAlignment(nOutVT.getTypeForEVT(dag.getContext()));
    SDValue stackPtr = dag.createStackTemporary(inVT, alignment);
    int fi = ((FrameIndexSDNode) stackPtr.getNode()).getFrameIndex();
    Value sv = PseudoSourceValue.getFixedStack(fi);

    SDValue store = dag.getStore(dag.getEntryNode(), inOp, stackPtr, sv, 0, false, 0);
    lo = dag.getLoad(nOutVT, store, stackPtr, sv, 0);
    int incrementSize = nOutVT.getSizeInBits() / 8;
    stackPtr = dag.getNode(ISD.ADD, stackPtr.getValueType(), stackPtr,
        dag.getIntPtrConstant(incrementSize));
    hi = dag.getLoad(nOutVT, store, stackPtr, sv, incrementSize, false,
        Util.minAlign(alignment, incrementSize));
    if (tli.isBigEndian()) {
      SDValue temp = lo;
      lo = hi;
      hi = temp;
    }
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandRes_BUILD_PAIR(SDNode n) {
    return new SDValue[]{n.getOperand(0), n.getOperand(1)};
  }

  private SDValue[] expandRes_EXTRACT_ELEMENT(SDNode n) {
    SDValue[] t = getExpandedOp(n.getOperand(0));
    SDValue part = ((ConstantSDNode) n.getOperand(1).getNode()).getZExtValue()
        != 0 ? t[1] : t[0];
    Util.assertion(part.getValueType().equals(n.getValueType(0)));
    return getPairElements(part);
  }

  private SDValue[] expandRes_EXTRACT_VECTOR_ELT(SDNode n) {
    SDValue oldVec = n.getOperand(0);
    int oldElts = oldVec.getValueType().getVectorNumElements();
    EVT oldVT = n.getValueType(0);
    EVT newVT = tli.getTypeToTransformTo(dag.getContext(), oldVT);

    SDValue newVec = dag.getNode(ISD.BIT_CONVERT, EVT.getVectorVT(dag.getContext(), newVT, 2 * oldElts),
        oldVec);
    SDValue idx = n.getOperand(1);
    if (idx.getValueType().bitsLT(new EVT(tli.getPointerTy())))
      idx = dag.getNode(ISD.ZERO_EXTEND, new EVT(tli.getPointerTy()), idx);

    idx = dag.getNode(ISD.ADD, idx.getValueType(), idx, idx);
    SDValue lo = dag.getNode(ISD.EXTRACT_VECTOR_ELT, newVT, newVec, idx);
    idx = dag.getNode(ISD.ADD, idx.getValueType(), idx, dag.getConstant(1,
        idx.getValueType(), false));
    SDValue hi = dag.getNode(ISD.EXTRACT_VECTOR_ELT, newVT, newVec, idx);
    if (tli.isBigEndian()) {
      SDValue temp = lo;
      lo = hi;
      hi = temp;
    }
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandRes_NormalLoad(SDNode n) {
    Util.assertion(n.isNormalLoad(), "This method only for normal load!");
    LoadSDNode ld = (LoadSDNode) n;
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), ld.getValueType(0));
    SDValue chain = ld.getChain();
    SDValue ptr = ld.getBasePtr();
    int svOffset = ld.getSrcValueOffset();
    int alignment = ld.getAlignment();
    boolean isVolatile = ld.isVolatile();

    Util.assertion(nvt.isByteSized(), "Expanded type not byte sized!");
    SDValue lo = dag.getLoad(nvt, chain, ptr, ld.getSrcValue(), svOffset,
        isVolatile, alignment);
    int incrementSize = nvt.getSizeInBits() / 8;
    ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
        dag.getIntPtrConstant(incrementSize));
    SDValue hi = dag.getLoad(nvt, chain, ptr, ld.getSrcValue(),
        svOffset + incrementSize, isVolatile,
        Util.minAlign(alignment, incrementSize));
    chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo.getValue(1),
        hi.getValue(1));

    if (tli.isBigEndian()) {
      SDValue temp = lo;
      lo = hi;
      hi = temp;
    }
    replaceValueWith(new SDValue(n, 1), chain);
    return new SDValue[]{lo, hi};
  }

  private SDValue[] expandRes_VAARG(SDNode n) {
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), n.getValueType(0));
    SDValue chain = n.getOperand(0);
    SDValue ptr = n.getOperand(1);
    SDValue lo = dag.getVAArg(nvt, chain, ptr, n.getOperand(2));
    SDValue hi = dag.getVAArg(nvt, lo.getValue(1), ptr, n.getOperand(2));

    if (tli.isBigEndian()) {
      SDValue temp = lo;
      lo = hi;
      hi = temp;
    }
    replaceValueWith(new SDValue(n, 1), hi.getValue(1));
    return new SDValue[]{lo, hi};
  }

  // Generic Operand Expansion.
  private SDValue expandOp_BIT_CONVERT(SDNode n) {
    if (n.getValueType(0).isVector()) {
      EVT ovt = n.getOperand(0).getValueType();
      EVT nvt = EVT.getVectorVT(dag.getContext(), tli.getTypeToTransformTo(dag.getContext(), ovt), 2);

      if (isTypeLegal(nvt)) {
        SDValue[] parts = getExpandedOp(n.getOperand(0));
        if (tli.isBigEndian()) {
          SDValue temp = parts[0];
          parts[0] = parts[1];
          parts[1] = temp;
        }
        SDValue vec = dag.getNode(ISD.BUILD_VECTOR, nvt, parts);
        return dag.getNode(ISD.BIT_CONVERT, n.getValueType(0), vec);
      }
    }

    return createStackStoreLoad(n.getOperand(0), n.getValueType(0));
  }

  private SDValue expandOp_BUILD_VECTOR(SDNode n) {
    EVT vecVT = n.getValueType(0);
    int numElts = vecVT.getVectorNumElements();
    EVT oldVT = n.getOperand(0).getValueType();
    EVT newVT = tli.getTypeToTransformTo(dag.getContext(), oldVT);

    Util.assertion(oldVT.equals(vecVT.getVectorElementType()), "BUILD_VECTOR operand type doesn't match vector element tyep!");

    SDValue[] elts = new SDValue[2 * numElts];
    for (int i = 0; i < numElts; i++) {
      SDValue[] t = getExpandedOp(n.getOperand(i));
      if (tli.isBigEndian()) {
        SDValue temp = t[0];
        t[0] = t[1];
        t[1] = temp;
      }
      System.arraycopy(t, 0, elts, 2 * i, 2);
    }

    SDValue newVec = dag.getNode(ISD.BUILD_VECTOR,
        EVT.getVectorVT(dag.getContext(), newVT, 2 * numElts), elts);
    return dag.getNode(ISD.BIT_CONVERT, vecVT, newVec);
  }

  private SDValue expandOp_EXTRACT_ELEMENT(SDNode n) {
    SDValue[] t = getExpandedOp(n.getOperand(0));
    return ((ConstantSDNode) n.getOperand(1).getNode()).getZExtValue() != 0
        ? t[1] : t[0];
  }

  private SDValue expandOp_INSERT_VECTOR_ELT(SDNode n) {
    EVT vecVT = n.getValueType(0);
    int numElts = vecVT.getVectorNumElements();
    SDValue val = n.getOperand(1);
    EVT oldVT = val.getValueType();
    EVT newVT = tli.getTypeToTransformTo(dag.getContext(), oldVT);

    Util.assertion(oldVT.equals(vecVT.getVectorElementType()));

    EVT newVecVT = EVT.getVectorVT(dag.getContext(), newVT, numElts * 2);
    SDValue newVec = dag.getNode(ISD.BIT_CONVERT, newVecVT, n.getOperand(0));

    SDValue[] t = getExpandedOp(val);
    if (tli.isBigEndian()) {
      SDValue temp = t[0];
      t[0] = t[1];
      t[1] = temp;
    }
    SDValue idx = n.getOperand(2);
    idx = dag.getNode(ISD.ADD, idx.getValueType(), idx, idx);
    newVec = dag.getNode(ISD.INSERT_VECTOR_ELT, newVecVT, newVec, t[0], idx);
    idx = dag.getNode(ISD.ADD, idx.getValueType(), idx, dag.getIntPtrConstant(1));
    newVec = dag.getNode(ISD.INSERT_VECTOR_ELT, newVecVT, newVec, t[1], idx);
    return dag.getNode(ISD.BIT_CONVERT, vecVT, newVec);
  }

  private SDValue expandOp_SCALAR_TO_VECTOR(SDNode n) {
    EVT vt = n.getValueType(0);
    Util.assertion(vt.getVectorElementType().equals(n.getOperand(0).getValueType()));

    int numElts = vt.getVectorNumElements();
    SDValue[] ops = new SDValue[numElts];
    ops[0] = n.getOperand(0);
    SDValue undefVal = dag.getUNDEF(ops[0].getValueType());
    for (int i = 1; i < numElts; i++)
      ops[i] = undefVal;
    return dag.getNode(ISD.BUILD_VECTOR, vt, ops);
  }

  private SDValue expandOp_NormalStore(StoreSDNode n, int opNo) {
    Util.assertion(n.isNormalStore(), "This method only can be for normal store!");
    Util.assertion(opNo == 1);
    StoreSDNode st = (StoreSDNode) n;
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), st.getValue().getValueType());
    SDValue chain = st.getChain();
    SDValue ptr = st.getBasePtr();
    int svOffset = st.getSrcValueOffset();
    int alignment = st.getAlignment();
    boolean isVolatile = st.isVolatile();

    Util.assertion(nvt.isByteSized(), "Expanded type not byte sized!");
    int incrementSize = nvt.getSizeInBits() / 8;

    SDValue[] t = getExpandedOp(st.getValue());
    if (tli.isBigEndian()) {
      SDValue temp = t[0];
      t[0] = t[1];
      t[1] = temp;
    }
    SDValue lo = t[0], hi = t[1];
    lo = dag.getStore(chain, lo, ptr, st.getSrcValue(), svOffset, isVolatile,
        alignment);
    ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
        dag.getIntPtrConstant(incrementSize));
    Util.assertion(isTypeLegal(ptr.getValueType()));
    hi = dag.getStore(chain, hi, ptr, st.getSrcValue(), svOffset + incrementSize,
        isVolatile, Util.minAlign(alignment, incrementSize));
    return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo, hi);
  }

  private SDValue libCallify(RTLIB lc, SDNode n, boolean isSigned) {
    int numOps = n.getNumOperands();
    if (numOps == 0)
      return makeLibCall(lc, n.getValueType(0), null, isSigned);
    else if (numOps == 1) {
      SDValue op = n.getOperand(0);
      return makeLibCall(lc, n.getValueType(0), new SDValue[]{op}, isSigned);
    } else if (numOps == 2) {
      SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
      return makeLibCall(lc, n.getValueType(0), ops, isSigned);
    }
    SDValue[] ops = new SDValue[numOps];
    for (int i = 0; i < numOps; i++)
      ops[i] = n.getOperand(i);
    return makeLibCall(lc, n.getValueType(0), ops, isSigned);
  }
}
