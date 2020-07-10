/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.target.TargetLowering;
import tools.APInt;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This file implements the {@linkplain SelectionDAG#legalizeVectors} method.
 * <br></br>
 * The vector legalizer looks for vector operations which might needs to be
 * scalarized and legalizes them. This is a separate step from legalize because
 * scalarizing can introduce illegal types. For instance, suppose we have an
 * ISD.SDIV of type v2i64 on x86-32. The type is legal(for example, addition
 * on v2i64 is legal), but ISD.SDIV is illegal, so we have to unroll the operation,
 * which introduces nodes with the illegal type i64 which must be expanded.
 * Similarly, suppose we have an ISD.SRA of type v16i8 on PowerPC; the operation
 * must be unrolled, which introduces nodes with the illegal type i8 which must be
 * promoted.
 * <br></br>
 * This does not legalize vector manipulation like ISD.BUILD_VECTOR, or operation
 * that happen to take a vector which are custom-lowered; the legalization for
 * such operations never produces nodes with illegal types, so it's fine to
 * put off legalizing them until {@linkplain SelectionDAG#legalize} method runs.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class VectorLegalizer {
  private SelectionDAG dag;
  private TargetLowering tli;
  private boolean changed;

  private HashMap<SDValue, SDValue> legalizeNodes;

  private void addLegalizedOperand(SDValue from, SDValue to) {
    legalizeNodes.put(from, to);
    if (!from.equals(to))
      legalizeNodes.put(to, to);
  }

  private SDValue legalizeOp(SDValue op) {
    // If this SDValue has been legalized, just return it's corresponding legalized value.
    if (legalizeNodes.containsKey(op))
      return legalizeNodes.get(op);

    SDNode node = op.getNode();
    SDValue[] ops = new SDValue[node.getNumOperands()];
    for (int i = 0, e = node.getNumOperands(); i < e; i++)
      ops[i] = legalizeOp(node.getOperand(i));

    SDValue result = dag.updateNodeOperands(op.getValue(0), ops);
    boolean hasVectorValue = false;
    for (int i = 0, e = node.getNumValues(); i < e; i++)
      hasVectorValue = node.getValueType(i).isVector();

    if (!hasVectorValue)
      return translateLegalizeResults(op, result);

    EVT queryType = new EVT();
    switch (op.getOpcode()) {
      case ISD.ADD:
      case ISD.SUB:
      case ISD.MUL:
      case ISD.SDIV:
      case ISD.UDIV:
      case ISD.SREM:
      case ISD.UREM:
      case ISD.FADD:
      case ISD.FSUB:
      case ISD.FMUL:
      case ISD.FDIV:
      case ISD.FREM:
      case ISD.AND:
      case ISD.OR:
      case ISD.XOR:
      case ISD.SHL:
      case ISD.SRA:
      case ISD.SRL:
      case ISD.ROTL:
      case ISD.ROTR:
      case ISD.CTTZ:
      case ISD.CTLZ:
      case ISD.CTPOP:
      case ISD.SELECT:
      case ISD.SELECT_CC:
      case ISD.VSETCC:
      case ISD.ZERO_EXTEND:
      case ISD.ANY_EXTEND:
      case ISD.TRUNCATE:
      case ISD.SIGN_EXTEND:
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
      case ISD.FNEG:
      case ISD.FABS:
      case ISD.FSQRT:
      case ISD.FSIN:
      case ISD.FCOS:
      case ISD.FPOWI:
      case ISD.FPOW:
      case ISD.FLOG:
      case ISD.FLOG2:
      case ISD.FLOG10:
      case ISD.FEXP:
      case ISD.FEXP2:
      case ISD.FCEIL:
      case ISD.FTRUNC:
      case ISD.FRINT:
      case ISD.FNEARBYINT:
      case ISD.FFLOOR:
        queryType = node.getValueType(0);
        break;
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
        queryType = node.getOperand(0).getValueType();
        break;
      default:
        return translateLegalizeResults(op, result);
    }

    switch (tli.getOperationAction(node.getOpcode(), queryType)) {
      case Legal:
        break;
      case Promote:
        result = promoteVectorOp(op);
        changed = true;
        break;
      case Custom: {
        SDValue temp = tli.lowerOperation(op, dag);
        if (temp.getNode() != null) {
          result = temp;
          break;
        }
        // Fall through
      }
      case Expand:
        switch (node.getOpcode()) {
          case ISD.FNEG:
            result = expandFNEG(op);
            break;
          case ISD.VSETCC:
            result = unrollVSETCC(op);
            break;
          default:
            result = unrollVectorOp(op);
            break;
        }
        break;
    }
    if (!result.equals(op)) {
      result = legalizeOp(result);
      changed = true;
    }

    addLegalizedOperand(op, result);
    return result;
  }

  private SDValue translateLegalizeResults(SDValue op, SDValue result) {
    for (int i = 0, e = op.getNode().getNumValues(); i < e; i++)
      addLegalizedOperand(op.getValue(i), result.getValue(i));
    return result.getValue(result.getResNo());
  }

  private SDValue unrollVectorOp(SDValue op) {
    EVT vt = op.getValueType();
    Util.assertion(op.getNode().getNumValues() == 1, "Can't unroll a vector with multiple results!");
    int ne = vt.getVectorNumElements();
    EVT eltVT = vt.getVectorElementType();

    ArrayList<SDValue> scalars = new ArrayList<>();
    SDValue[] ops = new SDValue[op.getNumOperands()];
    for (int i = 0; i < ne; i++) {
      for (int j = 0, e = op.getNumOperands(); j < e; j++) {
        SDValue operand = op.getOperand(j);
        EVT opVT = operand.getValueType();
        if (opVT.isVector()) {
          EVT operandEltVT = opVT.getVectorElementType();
          ops[j] = dag.getNode(ISD.EXTRACT_VECTOR_ELT, operandEltVT, operand,
              dag.getConstant(i, new EVT(MVT.i32), false));
        } else
          ops[j] = operand;
      }

      switch (op.getOpcode()) {
        case ISD.SHL:
        case ISD.SRA:
        case ISD.SRL:
        case ISD.ROTL:
        case ISD.ROTR:
          scalars.add(dag.getNode(op.getOpcode(), eltVT, ops[0],
              dag.getShiftAmountOperand(ops[1])));
          break;
        default:
          scalars.add(dag.getNode(op.getOpcode(), eltVT, ops));
          break;
      }
    }
    return dag.getNode(ISD.BUILD_VECTOR, vt, scalars);
  }

  private SDValue unrollVSETCC(SDValue op) {
    EVT vt = op.getValueType();
    int numElts = vt.getVectorNumElements();
    EVT eltVT = vt.getVectorElementType();
    SDValue lhs = op.getOperand(0), rhs = op.getOperand(1), cc = op.getOperand(2);
    EVT tempEltVT = lhs.getValueType().getVectorElementType();
    SDValue[] ops = new SDValue[numElts];
    for (int i = 0; i < numElts; i++) {
      SDValue lhsElt = dag.getNode(ISD.EXTRACT_VECTOR_ELT, tempEltVT, lhs,
          dag.getIntPtrConstant(i));
      SDValue rhsElt = dag.getNode(ISD.EXTRACT_VECTOR_ELT, tempEltVT, rhs,
          dag.getIntPtrConstant(i));
      ops[i] = dag.getNode(ISD.SETCC, new EVT(tli.getSetCCResultType(tempEltVT)),
          lhsElt, rhsElt, cc);
      ops[i] = dag.getNode(ISD.SELECT, vt, ops[i],
          dag.getConstant(APInt.getAllOnesValue(eltVT.getSizeInBits()), eltVT, false),
          dag.getConstant(0, eltVT, false));
    }
    return dag.getNode(ISD.BUILD_VECTOR, vt, ops);

  }

  private SDValue expandFNEG(SDValue op) {
    if (tli.isOperationLegalOrCustom(ISD.FSUB, op.getValueType())) {
      SDValue zero = dag.getConstantFP(-0.0, op.getValueType(), false);
      return dag.getNode(ISD.FSUB, op.getValueType(), zero, op.getOperand(0));
    }
    return unrollVectorOp(op);
  }

  private SDValue promoteVectorOp(SDValue op) {
    EVT vt = op.getValueType();
    Util.assertion(op.getNode().getNumValues() == 1, "Can't promote a vector with multiple results!");
    EVT nvt = tli.getTypeToTransformTo(dag.getContext(), vt);
    SDValue[] ops = new SDValue[op.getNumOperands()];
    for (int j = 0, e = op.getNumOperands(); j < e; j++) {
      if (op.getOperand(j).getValueType().isVector())
        ops[j] = dag.getNode(ISD.BIT_CONVERT, nvt, op.getOperand(j));
      else
        ops[j] = op.getOperand(j);
    }
    op = dag.getNode(op.getOpcode(), nvt, ops);
    return dag.getNode(ISD.BIT_CONVERT, vt, op);
  }

  public boolean run() {
    // First, we assign a topologial order to SelectionDAG so that we
    // can traverse the SelectionDAG in bottom up. Importantly, this method
    // would avoids redundant recurse and memory usage.
    dag.assignTopologicalOrder();
    for (SDNode node : dag.allNodes) {
      legalizeOp(new SDValue(node, 0));
    }

    SDValue oldRoot = dag.getRoot();
    Util.assertion(legalizeNodes.containsKey(oldRoot), "Root didn't get legalized!");
    dag.setRoot(legalizeNodes.get(oldRoot));

    legalizeNodes.clear();
    dag.removeDeadNodes();
    return changed;
  }

  public VectorLegalizer(SelectionDAG dag) {
    this.dag = dag;
    tli = dag.getTargetLoweringInfo();
    changed = false;
    legalizeNodes = new HashMap<>();
  }
}
