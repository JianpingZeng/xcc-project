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

import backend.analysis.aa.AliasAnalysis;
import backend.codegen.*;
import backend.codegen.dagisel.SDNode.*;
import backend.support.DefaultDotGraphTrait;
import backend.support.GraphWriter;
import backend.support.LLVMContext;
import backend.target.TargetData;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.target.TargetMachine.CodeGenOpt;
import backend.target.TargetOpcodes;
import backend.type.ArrayType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.BitCastInst;
import backend.value.Instruction.GetElementPtrInst;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import tools.*;

import java.util.*;

import static backend.codegen.MachineMemOperand.*;
import static backend.codegen.dagisel.ISD.getSetCCSwappedOperands;
import static backend.codegen.dagisel.MemIndexedMode.UNINDEXED;
import static backend.codegen.dagisel.SDNode.*;
import static backend.support.BackendCmdOptions.EnableUnsafeFPMath;
import static backend.target.TargetLowering.BooleanContent.ZeroOrOneBooleanContent;
import static tools.APFloat.CmpResult.*;
import static tools.APFloat.OpStatus.opDivByZero;
import static tools.APFloat.OpStatus.opInvalidOp;
import static tools.APFloat.RoundingMode.rmNearestTiesToEven;
import static tools.APFloat.RoundingMode.rmTowardZero;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class SelectionDAG {
  private TargetMachine target;
  private TargetLowering tli;
  private FunctionLoweringInfo fli;
  private MachineFunction mf;
  private MachineModuleInfo mmi;
  private SDNode entryNode;
  private SDValue root;
  public ArrayList<SDNode> allNodes;
  private ArrayList<SDVTList> vtlist;
  private ArrayList<CondCodeSDNode> condCodeNodes;
  private TIntObjectHashMap<SDNode> cseMap;

  private HashMap<Pair<String, Integer>, SDNode> targetExternalSymbols = new HashMap<>();
  private HashMap<EVT, SDNode> extendedValueTypeNodes = new HashMap<>();
  private ArrayList<SDNode> valueTypeNodes = new ArrayList<>();
  private HashMap<String, SDNode> externalSymbols = new HashMap<>();

  public SelectionDAG(TargetLowering tl, FunctionLoweringInfo fli) {
    target = tl.getTargetMachine();
    tli = tl;
    this.fli = fli;
    mmi = null;
    allNodes = new ArrayList<>();
    vtlist = new ArrayList<>();
    condCodeNodes = new ArrayList<>();
    cseMap = new TIntObjectHashMap<>();
    entryNode = new SDNode(ISD.EntryToken, getVTList(new EVT(MVT.Other)));
    root = getEntryNode();
    add(entryNode);
  }

  private void add(SDNode n) {
    if (allNodes.contains(n)) return;
    allNodes.add(n);
  }

  private void add(int pos, SDNode n) {
    Util.assertion(pos >= 0 && pos < allNodes.size(), "Position to be inserted out of range!");
    if (allNodes.contains(n)) return;
    allNodes.add(pos, n);
  }

  public FunctionLoweringInfo getFunctionLoweringInfo() {
    return fli;
  }

  public TargetLowering getTargetLoweringInfo() {
    return tli;
  }

  public MachineFunction getMachineFunction() {
    return mf;
  }

  public MachineModuleInfo getMachineModuleInfo() {
    return mmi;
  }

  public void init(MachineFunction mf, MachineModuleInfo mmi) {
    this.mmi = mmi;
    this.mf = mf;
  }

  public TargetMachine getTarget() {
    return target;
  }

  public MachineSDNode getMachineNode(int opcode,
                                      EVT vt1,
                                      SDValue... ops) {
    SDVTList vts = getVTList(vt1);
    return getMachineNode(opcode, vts, ops);
  }

  public MachineSDNode getMachineNode(int opcode,
                                      EVT vt1,
                                      EVT vt2,
                                      SDValue... ops) {
    SDVTList vts = getVTList(vt1, vt2);
    return getMachineNode(opcode, vts, ops);
  }

  public MachineSDNode getMachineNode(int opcode,
                                      EVT vt1,
                                      EVT vt2) {
    return getMachineNode(opcode, vt1, vt2, (SDValue[]) null);
  }

  public MachineSDNode getMachineNode(int opcode,
                                      EVT vt1,
                                      EVT vt2,
                                      EVT vt3,
                                      SDValue[] ops) {
    SDVTList vts = getVTList(vt1, vt2, vt3);
    return getMachineNode(opcode, vts, ops);
  }

  public MachineSDNode getMachineNode(int opcode,
                                      EVT vt1,
                                      EVT vt2,
                                      EVT vt3,
                                      EVT vt4,
                                      SDValue[] ops) {
    SDVTList vts = getVTList(vt1, vt2, vt3, vt4);
    return getMachineNode(opcode, vts, ops);
  }

  public MachineSDNode getMachineNode(int opcode,
                                      SDVTList vts,
                                      SDValue[] ops) {
    boolean doCSE = !vts.vts[vts.numVTs-1].equals(new EVT(MVT.Flag));
    MachineSDNode n = null;
    int hash = 0;
    if (doCSE) {
      FoldingSetNodeID id = new FoldingSetNodeID();
      addNodeToIDNode(id, ~opcode, vts, ops, ops.length);
      hash = id.hashCode();
      if (cseMap.containsKey(hash)) {
        return (MachineSDNode) cseMap.get(hash);
      }
    }

    n = new MachineSDNode(~opcode, vts);
    n.initOperands(ops);
    if (doCSE)
      cseMap.put(hash, n);

    allNodes.add(n);
    return n;
  }

  public SDValue getNode(int opc, SDVTList vtlist, ArrayList<SDValue> ops) {
    SDValue[] tempOps = new SDValue[ops.size()];
    ops.toArray(tempOps);
    return getNode(opc, vtlist, tempOps);
  }

  public SDValue getNode(int opc, SDVTList vtList, SDValue... ops) {
    if (vtList.numVTs == 1)
      return getNode(opc, vtList.vts[0], ops);

    SDNode node;
    if (vtList.vts[vtList.numVTs - 1].getSimpleVT().simpleVT != MVT.Flag) {
      FoldingSetNodeID calc = new FoldingSetNodeID();
      addNodeToIDNode(calc, opc, vtList, ops, ops.length);
      int id = calc.computeHash();
      if (cseMap.containsKey(id))
        return new SDValue(cseMap.get(id), 0);

      switch (ops.length) {
        default:
          node = new SDNode(opc, vtList, ops);
          break;
        case 1:
          node = new SDNode.UnarySDNode(opc, vtList, ops[0]);
          break;
        case 2:
          node = new SDNode.BinarySDNode(opc, vtList, ops[0], ops[1]);
          break;
        case 3:
          node = new SDNode.TernarySDNode(opc, vtList, ops[0], ops[1],
              ops[2]);
          break;
      }
      cseMap.put(id, node);
    } else {
      switch (ops.length) {
        default:
          node = new SDNode(opc, vtList, ops);
          break;
        case 1:
          node = new SDNode.UnarySDNode(opc, vtList, ops[0]);
          break;
        case 2:
          node = new SDNode.BinarySDNode(opc, vtList, ops[0], ops[1]);
          break;
        case 3:
          node = new SDNode.TernarySDNode(opc, vtList, ops[0], ops[1],
              ops[2]);
          break;
      }
    }
    add(node);
    return new SDValue(node, 0);
  }

  public SDValue getNode(int opc, ArrayList<EVT> vts) {
    EVT[] temp = new EVT[vts.size()];
    vts.toArray(temp);
    return getNode(opc, getVTList(temp));
  }

  public SDValue getNode(int opc, EVT vt, SDValue... ops) {
    int numOps = ops.length;
    switch (numOps) {
      case 0:
        return getNode(opc, vt);
      case 1:
        return getNode(opc, vt, ops[0]);
      case 2:
        return getNode(opc, vt, ops[0], ops[1]);
      case 3:
        return getNode(opc, vt, ops[0], ops[1], ops[2]);
    }
    switch (opc) {
      default:
        break;
      case ISD.SELECT_CC:
        Util.assertion(numOps == 5, "SELECT_CC takes five operands!");
        Util.assertion(ops[0].getValueType().equals(ops[1].getValueType()), "LHS and RHS of condition must have same type!");

        Util.assertion(ops[2].getValueType().equals(ops[3].getValueType()), "True and False parts of SELECT_CC must have same type!");

        Util.assertion(ops[2].getValueType().equals(vt), "SELECT_CC node must be same type as true/false part!");

        break;
      case ISD.BR_CC:
        Util.assertion(numOps == 5, "BR_CC takes five operands!");
        Util.assertion(ops[2].getValueType().equals(ops[3].getValueType()), "LHS/RHS of comparison should match types!");

        break;
    }

    SDNode node;
    SDVTList vts = getVTList(vt);
    if (!vt.equals(new EVT(MVT.Flag))) {
      FoldingSetNodeID calc = new FoldingSetNodeID();
      addNodeToIDNode(calc, opc, vts, ops);
      int id = calc.computeHash();
      if (cseMap.containsKey(id)) {
        node = cseMap.get(id);
        return new SDValue(node, 0);
      }
      node = new SDNode(opc, vts, ops);
      cseMap.put(id, node);
    } else {
      node = new SDNode(opc, vts, ops);
    }
    add(node);
    return new SDValue(node, 0);
  }

  public SDValue getNode(int opc, EVT vt, ArrayList<SDValue> ops) {
    SDValue[] temps = new SDValue[ops.size()];
    ops.toArray(temps);
    return getNode(opc, vt, temps);
  }

  public SDValue getNode(int opc, EVT vt, SDUse[] ops) {
    switch (ops.length) {
      case 0:
        return getNode(opc, vt);
      case 1:
        return getNode(opc, vt, ops[0].get());
      case 2:
        return getNode(opc, vt, ops[0].get(), ops[1].get());
      case 3:
        return getNode(opc, vt, ops[0].get(), ops[1].get(), ops[2].get());
      default:
        break;
    }
    SDValue[] temp = new SDValue[ops.length];
    Object[] src = Arrays.stream(ops).map(SDUse::get).toArray();
    System.arraycopy(src, 0, temp, 0, ops.length);
    return getNode(opc, vt, temp);
  }

  public SDValue getNode(int opc, EVT vt) {
    FoldingSetNodeID calc = new FoldingSetNodeID();
    addNodeToIDNode(calc, opc, getVTList(vt), null, 0);
    int id = calc.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode node = new SDNode(opc, getVTList(vt));
    cseMap.put(id, node);
    add(node);
    return new SDValue(node, 0);
  }

  public SDValue getNode(int opc, EVT vt, SDValue op0) {
    if (op0.getNode() instanceof ConstantSDNode) {
      ConstantSDNode cn1 = (ConstantSDNode) op0.getNode();
      APInt val = cn1.getAPIntValue();
      int bitwidth = vt.getSizeInBits();
      switch (opc) {
        default:
          break;
        case ISD.SIGN_EXTEND:
          return getConstant(new APInt(val).sextOrTrunc(bitwidth), vt,
              false);
        case ISD.ANY_EXTEND:
        case ISD.ZERO_EXTEND:
        case ISD.TRUNCATE:
          return getConstant(new APInt(val).zextOrTrunc(bitwidth), vt,
              false);
        case ISD.UINT_TO_FP:
        case ISD.SINT_TO_FP: {
          long[] zeros = {0, 0};
          if (vt.equals(new EVT(MVT.ppcf128)))
            break;
          APFloat apf = new APFloat(new APInt(bitwidth, zeros));
          apf.convertFromAPInt(val, opc == ISD.SINT_TO_FP,
              rmNearestTiesToEven);
          return getConstantFP(apf, vt, false);
        }
        case ISD.BIT_CONVERT: {
          if (vt.equals(new EVT(MVT.f32)) && cn1.getValueType(0).equals(new EVT(MVT.i32)))
            return getConstantFP(val.bitsToFloat(), vt, false);
          else if (vt.equals(new EVT(MVT.f64)) && cn1.getValueType(0).equals(new EVT(MVT.i64)))
            return getConstantFP(val.bitsToDouble(), vt, false);
          break;
        }
        case ISD.BSWAP:
          return getConstant(val.byteSwap(), vt, false);
        case ISD.CTPOP:
          return getConstant(val.countPopulation(), vt, false);
        case ISD.CTLZ:
          return getConstant(val.countLeadingZeros(), vt, false);
        case ISD.CTTZ:
          return getConstant(val.countTrailingZeros(), vt, false);
      }
    }

    if (op0.getNode() instanceof ConstantFPSDNode) {
      ConstantFPSDNode fp1 = (ConstantFPSDNode) op0.getNode();
      APFloat val = fp1.getValueAPF().clone();    // make copy
      if (!vt.equals(new EVT(MVT.ppcf128)) && !op0.getValueType().equals(new EVT(MVT.ppcf128))) {
        switch (opc) {
          case ISD.FNEG:
            val.changeSign();
            return getConstantFP(val, vt, false);
          case ISD.FABS:
            val.clearSign();
            return getConstantFP(val, vt, false);
          case ISD.FP_ROUND:
          case ISD.FP_EXTEND: {
            OutRef<Boolean> ignored = new OutRef<>(
                false);
            val.convert(EVTToAPFloatSemantics(vt),
                rmNearestTiesToEven, ignored);
            return getConstantFP(val, vt, false);
          }
          case ISD.FP_TO_SINT:
          case ISD.FP_TO_UINT: {
            long[] x = {0, 0};
            OutRef<Boolean> ignored = new OutRef<>(
                false);
            int opStatus = val
                .convertToInteger(x, vt.getSizeInBits(), opc == ISD.FP_TO_SINT,
                    rmTowardZero, ignored);
            if (opStatus == opInvalidOp)
              break;
            APInt res = new APInt(vt.getSizeInBits(), x);
            return getConstant(res, vt, false);
          }
          case ISD.BIT_CONVERT: {
            if (vt.equals(new EVT(MVT.i32)) && fp1.getValueType(0).equals(new EVT(MVT.f32)))
              return getConstant((int) (val.bitcastToAPInt().getZExtValue()),
                  vt, false);
            if (vt.equals(new EVT(MVT.i64)) && fp1.getValueType(0).equals(new EVT(MVT.f64)))
              return getConstant(val.bitcastToAPInt().getZExtValue(), vt,
                  false);
            break;
          }
        }
      }
    }

    int opOpcode = op0.getNode().getOpcode();
    switch (opc) {
      case ISD.TokenFactor:
      case ISD.MERGE_VALUES:
      case ISD.CONCAT_VECTORS:
        return op0;
      case ISD.FP_ROUND:
      case ISD.FP_EXTEND:
        Util.assertion(vt.isFloatingPoint() && op0.getValueType().isFloatingPoint(), "Invalid FP cast!");

        if (op0.getValueType().equals(vt))
          return op0;
        if (op0.getOpcode() == ISD.UNDEF)
          return getUNDEF(vt);
        break;
      case ISD.SIGN_EXTEND:
        Util.assertion(vt.isInteger() && op0.getValueType().isInteger(), "Invalid Integer cast!");

        if (op0.getValueType().equals(vt))
          return op0;
        Util.assertion(op0.getValueType().bitsLT(vt), "Invalid sext node, dest < src!");

        if (opOpcode == ISD.SIGN_EXTEND || opOpcode == ISD.ZERO_EXTEND)
          return getNode(opOpcode, vt, op0.getNode().getOperand(0));
        break;
      case ISD.ANY_EXTEND:
      case ISD.ZERO_EXTEND:
        Util.assertion(vt.isInteger() && op0.getValueType().isInteger(), "Invalid Integer cast!");

        if (op0.getValueType().equals(vt))
          return op0;
        Util.assertion(op0.getValueType().bitsLT(vt), "Invalid zext node, dest < src!");

        if (opOpcode == ISD.SIGN_EXTEND || opOpcode == ISD.ZERO_EXTEND)
          return getNode(opOpcode, vt, op0.getNode().getOperand(0));
        break;

      case ISD.TRUNCATE:
        Util.assertion(vt.isInteger() && op0.getValueType().isInteger(), "Invalid Integer cast!");

        if (op0.getValueType().equals(vt))
          return op0;
        Util.assertion(op0.getValueType().bitsGT(vt), "Invalid truncate node, dest > src!");

        if (opOpcode == ISD.TRUNCATE)
          return getNode(ISD.TRUNCATE, vt, op0.getNode().getOperand(0));
        else if (opOpcode == ISD.ZERO_EXTEND || opOpcode == ISD.SIGN_EXTEND
            || opOpcode == ISD.ANY_EXTEND) {
          // If the source is smaller than the dest, we still need an extend.
          if (op0.getOperand(0).getValueType().bitsLT(vt))
            return getNode(opOpcode, vt, op0.getOperand(0));
          else if (op0.getOperand(0).getValueType().bitsGT(vt))
            return getNode(ISD.TRUNCATE, vt, op0.getOperand(0));
          else
            return op0.getOperand(0);
        }
        break;
      case ISD.BIT_CONVERT:
        Util.assertion(vt.getSizeInBits() == op0.getValueType().getSizeInBits(), "Can't perform bit conversion between different size!");

        if (vt.equals(op0.getValueType()))
          return op0;
        if (opOpcode == ISD.BIT_CONVERT)
          return getNode(ISD.BIT_CONVERT, vt, op0.getOperand(0));
        if (opOpcode == ISD.UNDEF)
          return getUNDEF(vt);
        break;
      case ISD.SCALAR_TO_VECTOR:
        Util.assertion(vt.isVector() && !op0.getValueType().isVector() && (vt.getVectorElementType().equals(op0.getValueType())
            || (vt.getVectorElementType().isInteger() && op0
            .getValueType().isInteger() && vt.getVectorElementType().bitsLE(op0
            .getValueType()))), "Illegal SCALAR_TO_VECTOR node!");

        if (opOpcode == ISD.UNDEF)
          return getUNDEF(vt);

        if (opOpcode == ISD.EXTRACT_VECTOR_ELT && (op0.getOperand(0).getNode() instanceof ConstantSDNode)
            && op0.getConstantOperandVal(1) == 0 && op0.getOperand(0).getValueType().equals(vt))
          return op0.getOperand(0);
        break;
      case ISD.FNEG:
        // -(X-Y) ==> Y-X is unsafe because -0.0 ！= +0.0 when X equals Y.
        if (EnableUnsafeFPMath.value && op0.getOpcode() == ISD.FSUB) {
          return getNode(ISD.FSUB, vt, op0.getOperand(1), op0.getOperand(0));
        }
        if (opOpcode == ISD.FNEG)   // -(-X) ==> X
          return op0.getOperand(0);
        break;
      case ISD.FABS:
        // abs(-X) ==> abs(X)
        if (opOpcode == ISD.FNEG) {
          return getNode(ISD.FABS, vt, op0.getOperand(0));
        }
        break;
    }

    SDNode node;
    SDVTList vts = getVTList(vt);
    if (!vt.equals(new EVT(MVT.Flag))) {
      FoldingSetNodeID calc = new FoldingSetNodeID();
      addNodeToIDNode(calc, opc, vts, op0);
      int id = calc.computeHash();
      if (cseMap.containsKey(id))
        return new SDValue(cseMap.get(id), 0);
      node = new SDNode.UnarySDNode(opc, vts, op0);
      cseMap.put(id, node);
    } else
      node = new SDNode.UnarySDNode(opc, vts, op0);
    add(node);
    return new SDValue(node, 0);
  }

  public SDValue getUNDEF(EVT vt) {
    return getNode(ISD.UNDEF, vt);
  }

  public SDValue foldArithmetic(int opcode, EVT vt, ConstantSDNode cst1,
                                ConstantSDNode cst2) {
    APInt c1 = cst1.getAPIntValue(), c2 = cst2.getAPIntValue();
    switch (opcode) {
      case ISD.ADD:
        return getConstant(c1.add(c2), vt, false);
      case ISD.SUB:
        return getConstant(c1.sub(c2), vt, false);
      case ISD.MUL:
        return getConstant(c1.mul(c2), vt, false);
      case ISD.SDIV:
        return getConstant(c1.sdiv(c2), vt, false);
      case ISD.UDIV:
        return getConstant(c1.udiv(c2), vt, false);
      case ISD.UREM:
        return getConstant(c1.urem(c2), vt, false);
      case ISD.SREM:
        return getConstant(c1.srem(c2), vt, false);
      case ISD.AND:
        return getConstant(c1.and(c2), vt, false);
      case ISD.OR:
        return getConstant(c1.or(c2), vt, false);
      case ISD.XOR:
        return getConstant(c1.xor(c2), vt, false);
      //todo case ISD.SRA: return getConstant(c1.sra(c2), vt, false);
      case ISD.SHL:
        return getConstant(c1.shl(c2), vt, false);
    }
    return new SDValue();
  }

  public SDValue getNode(int opc, EVT vt, SDValue op0, SDValue op1) {
    ConstantSDNode cn0 = op0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) op0.getNode() :
        null;
    ConstantSDNode cn1 = op1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) op1.getNode() :
        null;

    switch (opc) {
      default:
        break;
      case ISD.TokenFactor:
        Util.assertion(vt.equals(new EVT(MVT.Other)) && op0.getValueType().equals(new EVT(MVT.Other)) && op1.getValueType().equals(new EVT(MVT.Other)));

        // fold trivial token factors.
        if (op0.getOpcode() == ISD.EntryToken)
          return op1;
        if (op1.getOpcode() == ISD.EntryToken)
          return op0;

        if (op1.equals(op0))
          return op0;
        break;

      case ISD.CONCAT_VECTORS:
        if (op0.getOpcode() == ISD.BUILD_VECTOR && op1.getOpcode() == ISD.BUILD_VECTOR) {
          SDValue[] vals = new SDValue[op0.getNumOperands() + op1.getNumOperands()];
          int i = 0;
          for (int j = 0, e = op0.getNumOperands(); j < e; j++)
            vals[i++] = op0.getOperand(j);
          for (int j = 0, e = op1.getNumOperands(); j < e; j++)
            vals[i++] = op1.getOperand(j);

          return getNode(ISD.BUILD_VECTOR, vt, vals);
        }
        break;
      case ISD.AND:
        Util.assertion(vt.isInteger() && op0.getValueType().isInteger() && op1.getValueType().isInteger(), "Binary operator types must match!");

        // X & 0 ==> 0
        if (cn1 != null && cn1.isNullValue())
          return op1;
        // X & -1 ==> X
        if (cn1 != null && cn1.isAllOnesValue())
          return op0;
        break;
      case ISD.OR:
      case ISD.XOR:
      case ISD.ADD:
      case ISD.SUB:
        Util.assertion(vt.isInteger() && op0.getValueType().isInteger() && op1.getValueType().isInteger(), "Binary operator types must match!");

        // (X ^|+- 0) = X
        if (cn1 != null && cn1.isNullValue())
          return op0;
        break;
      case ISD.MUL:
        Util.assertion(vt.isInteger() && op0.getValueType().isInteger() && op1.getValueType().isInteger(), "Binary operator types must match!");

        // X * 0 == 0
        if (cn1 != null && cn1.isNullValue())
          return op0;
        break;
      case ISD.SHL:
      case ISD.SRA:
      case ISD.SRL:
      case ISD.ROTL:
      case ISD.ROTR:
        Util.assertion(vt.equals(op0.getValueType()), "Shift operators return tyep must be the same as their arg");

        Util.assertion(vt.isInteger() && op1.getValueType().isInteger(), "Shift operator only works on integer type!");


        if (vt.equals(new EVT(MVT.i1)))
          return op0;
        break;
    }

    if (cn0 != null) {
      if (cn1 != null) {
        SDValue res = foldArithmetic(opc, vt, cn0, cn1);
        if (res.getNode() != null)
          return res;
      } else {
        if (isCommutativeBinOp(opc)) {
          ConstantSDNode t = cn0;
          cn0 = cn1;
          cn1 = t;
          SDValue temp = op0;
          op0 = op1;
          op1 = temp;
        }
      }
    }

    ConstantFPSDNode fp0 = op0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) op0.getNode() :
        null;
    ConstantFPSDNode fp1 = op1.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) op1.getNode() :
        null;

    if (fp0 != null) {
      if (fp1 == null && isCommutativeBinOp(opc)) {
        ConstantFPSDNode t = fp0;
        fp0 = fp1;
        fp1 = t;
        SDValue temp = op0;
        op0 = op1;
        op1 = temp;
      } else if (fp1 != null && !vt.equals(new EVT(MVT.ppcf128))) {
        APFloat v0 = fp0.getValueAPF(), v1 = fp1.getValueAPF();
        int opStatus;
        switch (opc) {
          case ISD.FADD:
            opStatus = v0.add(v1, rmNearestTiesToEven);
            if (opStatus != opInvalidOp)
              return getConstantFP(v1, vt, false);
            break;
          case ISD.FSUB:
            opStatus = v0.subtract(v1, rmNearestTiesToEven);
            if (opStatus != opInvalidOp)
              return getConstantFP(v1, vt, false);
            break;
          case ISD.FMUL:
            opStatus = v0.multiply(v1, rmNearestTiesToEven);
            if (opStatus != opInvalidOp)
              return getConstantFP(v1, vt, false);
            break;
          case ISD.FDIV:
            opStatus = v0.divide(v1, rmNearestTiesToEven);
            if (opStatus != opInvalidOp && opStatus != opDivByZero)
              return getConstantFP(v1, vt, false);
            break;
          case ISD.FREM:
            opStatus = v0.mod(v1, rmNearestTiesToEven);
            if (opStatus != opInvalidOp && opStatus != opDivByZero)
              return getConstantFP(v1, vt, false);
            break;
          case ISD.FCOPYSIGN:
            v0.copySign(v1);
            return getConstantFP(v0, vt, false);
          default:
            break;
        }
      }
    }

    if (op0.getOpcode() == ISD.UNDEF) {
      if (isCommutativeBinOp(opc)) {
        SDValue temp = op0;
        op0 = op1;
        op1 = temp;
      } else {
        switch (opc) {
          case ISD.FP_ROUND_INREG:
          case ISD.SIGN_EXTEND_INREG:
          case ISD.SUB:
          case ISD.FSUB:
          case ISD.FDIV:
          case ISD.FREM:
          case ISD.SRA:
            return op0;     // fold op(undef, arg2) -> undef
          case ISD.UDIV:
          case ISD.SDIV:
          case ISD.UREM:
          case ISD.SREM:
          case ISD.SRL:
          case ISD.SHL: {
            if (vt.isVector())
              return getConstant(0, vt, false);
            return op1;
          }
        }
      }
    }

    if (op1.getOpcode() == ISD.UNDEF) {
      switch (opc) {
        case ISD.XOR:
          if (op0.getOpcode() == ISD.UNDEF)
            return getConstant(0, vt, false);
          // fallthrough
        case ISD.ADD:
        case ISD.ADDC:
        case ISD.ADDE:
        case ISD.SUB:
        case ISD.UDIV:
        case ISD.SDIV:
        case ISD.UREM:
        case ISD.SREM:
          return op1;       // fold op(arg1, undef) -> undef
        case ISD.FADD:
        case ISD.FSUB:
        case ISD.FMUL:
        case ISD.FDIV:
        case ISD.FREM:
          if (EnableUnsafeFPMath.value)
            return op1;
          break;
        case ISD.MUL:
        case ISD.AND:
        case ISD.SRL:
        case ISD.SHL:
          if (!vt.isVector())
            return getConstant(0, vt, false);
          return op0;
        case ISD.OR:
          if (!vt.isVector())
            return getConstant(APInt.getAllOnesValue(vt.getSizeInBits()), vt,
                false);
          return op0;
        case ISD.SRA:
          return op0;
      }
    }

    SDNode node;
    SDVTList vts = getVTList(vt);
    if (!vt.equals(new EVT(MVT.Flag))) {
      FoldingSetNodeID calc = new FoldingSetNodeID();
      addNodeToIDNode(calc, opc, vts, op0, op1);
      int id = calc.computeHash();
      if (cseMap.containsKey(id))
        return new SDValue(cseMap.get(id), 0);
      node = new SDNode.BinarySDNode(opc, vts, op0, op1);
      cseMap.put(id, node);
    } else
      node = new SDNode.BinarySDNode(opc, vts, op0, op1);
    add(node);
    return new SDValue(node, 0);
  }

  public static boolean isCommutativeBinOp(int opc) {
    switch (opc) {
      case ISD.ADD:
      case ISD.MUL:
      case ISD.MULHU:
      case ISD.MULHS:
      case ISD.SMUL_LOHI:
      case ISD.UMUL_LOHI:
      case ISD.FADD:
      case ISD.FMUL:
      case ISD.AND:
      case ISD.OR:
      case ISD.XOR:
      case ISD.SADDO:
      case ISD.UADDO:
      case ISD.ADDC:
      case ISD.ADDE:
        return true;
      default:
        return false;
    }
  }

  public SDValue getNode(int opc, EVT vt, SDValue op0, SDValue op1, SDValue op2) {
    ConstantSDNode cn0 = op0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) op0.getNode() :
        null;
    ConstantSDNode cn1 = op1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) op1.getNode() :
        null;

    switch (opc) {
      case ISD.CONCAT_VECTORS: {
        if (op0.getOpcode() == ISD.BUILD_VECTOR && op1.getOpcode() == ISD.BUILD_VECTOR
            && op2.getOpcode() == ISD.BUILD_VECTOR) {
          SDValue[] vals = new SDValue[op0.getNumOperands() + op1.getNumOperands()
              + op2.getNumOperands()];
          int i = 0;
          for (int j = 0, e = op0.getNumOperands(); j < e; j++)
            vals[i++] = op0.getOperand(j);
          for (int j = 0, e = op1.getNumOperands(); j < e; j++)
            vals[i++] = op1.getOperand(j);
          for (int j = 0, e = op2.getNumOperands(); j < e; j++)
            vals[i++] = op2.getOperand(j);

          return getNode(ISD.BUILD_VECTOR, vt, vals);
        }
        break;
      }
      case ISD.SELECT: {
        // true, X, Y ==> X
        // false, X, Y ==> Y
        if (cn0 != null) {
          return cn0.getZExtValue() != 0 ? op1 : op2;
        }
        if (op1.equals(op2))
          return op1;
      }
      case ISD.BRCOND:
        if (cn1 != null) {
          return cn1.getZExtValue() != 0 ?
              getNode(ISD.BR, new EVT(MVT.Other), op0, op2) :
              op0;
        }
        break;
      case ISD.BIT_CONVERT:
        if (op0.getValueType().equals(vt))
          return op0;
        break;
    }

    SDNode node;
    SDVTList vts = getVTList(vt);
    if (!vt.equals(new EVT(MVT.Flag))) {
      FoldingSetNodeID calc = new FoldingSetNodeID();
      addNodeToIDNode(calc, opc, vts, op0, op1, op2);
      int id = calc.computeHash();
      if (cseMap.containsKey(id))
        return new SDValue(cseMap.get(id), 0);
      node = new SDNode.TernarySDNode(opc, vts, op0, op1, op2);
      cseMap.put(id, node);
    } else
      node = new SDNode.TernarySDNode(opc, vts, op0, op1, op2);
    add(node);
    return new SDValue(node, 0);
  }

  public SDValue getCondCode(CondCode cond) {
    int idx = cond.ordinal();
    if (idx < condCodeNodes.size()) {
      CondCodeSDNode csd;
      if (condCodeNodes.get(idx) == null) {
        csd = new CondCodeSDNode(cond);
        condCodeNodes.set(idx, csd);
        add(csd);
      } else
        csd = condCodeNodes.get(idx);

      return new SDValue(csd, 0);
    }
    for (int i = condCodeNodes.size(); i <= idx; i++)
      condCodeNodes.add(null);
    CondCodeSDNode node = new CondCodeSDNode(cond);
    condCodeNodes.set(idx, node);
    add(node);
    return new SDValue(node, 0);
  }

  public SDValue getSetCC(EVT vt, SDValue op1, SDValue op2, CondCode cond) {
    return getNode(ISD.SETCC, vt, op1, op2, getCondCode(cond));
  }

  public SDValue getSelectCC(SDValue lhs, SDValue rhs,
                             SDValue trueVal, SDValue falseVal,
                             CondCode cc) {
    return getNode(ISD.SELECT_CC, trueVal.getValueType(), lhs, rhs,
        trueVal, falseVal, getCondCode(cc));
  }

  public SDValue getVSetCC(EVT vt, SDValue lhs, SDValue rhs, CondCode cc) {
    return getNode(ISD.VSETCC, vt, lhs, rhs, getCondCode(cc));
  }

  public SDValue getIntPtrConstant(long amt) {
    return getIntPtrConstant(amt, false);
  }

  public SDValue getIntPtrConstant(long amt, boolean isTarget) {
    return getConstant(amt, new EVT(tli.getPointerTy()), isTarget);
  }

  public SDValue getConstant(boolean val, EVT vt, boolean isTarget) {
    return getConstant(val ? 1 : 0, vt, isTarget);
  }

  public SDValue getConstant(long val, EVT vt, boolean isTarget) {
    EVT eltVt = vt.isVector() ? vt.getVectorElementType() : vt;
    Util.assertion(eltVt.getSizeInBits() >= 64 || (val >> eltVt.getSizeInBits()) + 1 < 2,
        "getConstant with a long value that doesn't fit in type!");

    return getConstant(new APInt(eltVt.getSizeInBits(), val), vt, isTarget);
  }

  public SDValue getConstant(APInt val, EVT vt, boolean isTarget) {
    return getConstant(ConstantInt.get(val), vt, isTarget);
  }

  public SDValue getTargetConstant(long val, EVT vt) {
    return getConstant(val, vt, true);
  }

  public SDValue getTargetConstant(APInt val, EVT vt) {
    return getConstant(val, vt, true);
  }

  public SDValue getTargetConstant(ConstantInt val, EVT vt) {
    return getConstant(val, vt, true);
  }

  public SDVTList getVTList(ArrayList<EVT> vts) {
    EVT[] temp = new EVT[vts.size()];
    vts.toArray(temp);
    return getVTList(temp);
  }

  public SDVTList getVTList(EVT... vts) {
    Util.assertion(vts != null && vts.length > 0, "Can't have an emtpy list!");
    for (int i = vtlist.size() - 1; i >= 0; i--) {
      SDVTList list = vtlist.get(i);
      if (list.vts.length != vts.length)
        continue;
      int j = vts.length - 1;
      for (; j >= 0; j--) {
        if (!vts[j].equals(list.vts[j]))
          break;
      }
      if (j == -1)
        return list;
    }

    SDVTList list = makeVTList(vts);
    vtlist.add(list);
    return list;
  }

  public SDVTList makeVTList(EVT[] vts) {
    SDVTList list = new SDVTList();
    list.vts = vts;
    list.numVTs = vts.length;
    return list;
  }

  public SDValue getConstant(ConstantInt ci, EVT vt, boolean isTarget) {
    Util.assertion(vt.isInteger(), "Can't create FP integer constant");
    EVT eltVT = vt.isVector() ? vt.getVectorElementType() : vt;
    Util.assertion(ci.getBitsWidth() == eltVT.getSizeInBits(),
        "APInt size doesn't match type size!");

    int opc = isTarget ? ISD.TargetConstant : ISD.Constant;
    FoldingSetNodeID id = new FoldingSetNodeID();
    addNodeToIDNode(id, opc, getVTList(eltVT), null, 0);
    id.addInteger(ci.getZExtValue());
    int hash = id.computeHash();
    SDNode n = null;
    if (cseMap.containsKey(hash)) {
      n = cseMap.get(hash);
      if (!vt.isVector())
        return new SDValue(cseMap.get(hash), 0);
    }
    if (n == null) {
      n = new ConstantSDNode(isTarget, ci, eltVT);
      cseMap.put(hash, n);
      add(n);
    }

    SDValue res = new SDValue(n, 0);
    if (vt.isVector()) {
      SDValue[] ops = new SDValue[vt.getVectorNumElements()];
      for (int i = 0; i < ops.length; i++)
        ops[i] = res;

      res = getNode(ISD.BUILD_VECTOR, vt, ops);
    }
    return res;
  }

  public SDValue getConstantFP(APFloat apf, EVT vt, boolean isTarget) {
    return getConstantFP(ConstantFP.get(apf), vt, isTarget);
  }

  public SDValue getConstantFP(ConstantFP val, EVT vt, boolean isTarget) {
    Util.assertion(vt.isFloatingPoint(), "Can't calling getConstantFP method on non-floating");

    EVT eltVT = vt.isVector() ? vt.getVectorElementType() : vt;

    int opc = isTarget ? ISD.TargetConstantFP : ISD.ConstantFP;
    FoldingSetNodeID id = new FoldingSetNodeID();
    addNodeToIDNode(id, opc, getVTList(eltVT), null, 0);
    id.addInteger(val.hashCode());
    int hash = id.computeHash();
    SDNode n = null;
    if (cseMap.containsKey(hash)) {
      n = cseMap.get(hash);
      if (!vt.isVector())
        return new SDValue(cseMap.get(hash), 0);
    }
    if (n == null) {
      n = new ConstantFPSDNode(isTarget, val, eltVT);
      cseMap.put(hash, n);
      add(n);
    }

    SDValue res = new SDValue(n, 0);
    if (vt.isVector()) {
      SDValue[] ops = new SDValue[vt.getVectorNumElements()];
      for (int i = 0; i < ops.length; i++)
        ops[i] = res;

      res = getNode(ISD.BUILD_VECTOR, vt, ops);
    }
    return res;
  }

  public SDValue getConstantFP(float val, EVT vt, boolean isTarget) {
    return getConstantFP(new APFloat(val), vt, isTarget);
  }

  public SDValue getConstantFP(double val, EVT vt, boolean isTarget) {
    return getConstantFP(new APFloat(val), vt, isTarget);
  }

  public SDValue getTargetConstantFP(APFloat val, EVT vt) {
    return getConstantFP(val, vt, true);
  }

  public SDValue getTargetConstantFP(float val, EVT vt) {
    return getConstantFP(val, vt, true);
  }

  public SDValue getTargetConstantFP(double val, EVT vt) {
    return getConstantFP(val, vt, true);
  }

  public SDValue getTargetConstantFP(ConstantFP val, EVT vt) {
    return getConstantFP(val, vt, true);
  }

  public static void addNodeToID(FoldingSetNodeID id, SDNode node) {
    addNodeToIDNode(id, node.getOpcode(), node.getValueList(), null, 0);
    for (SDUse use : node.getOperandList()) {
      id.addInteger(use.getNode().hashCode());
      id.addInteger(use.getResNo());
    }
    addCustomToIDNode(id, node);
  }

  private static void addCustomToIDNode(FoldingSetNodeID id, SDNode node) {
    switch (node.getOpcode()) {
      case ISD.TargetExternalSymbol:
      case ISD.ExternalSymbol:
        Util.shouldNotReachHere(
            "Should only be used on nodes with operands!");
        break;
      default:
        break;
      case ISD.TargetConstant:
      case ISD.Constant:
        id.addInteger(((ConstantSDNode) node).getConstantIntValue().hashCode());
        break;
      case ISD.TargetConstantFP:
      case ISD.ConstantFP:
        id.addInteger(((ConstantFPSDNode) node).getConstantFPValue().hashCode());
        break;
      case ISD.TargetGlobalAddress:
      case ISD.GlobalAddress:
      case ISD.TargetGlobalTLSAddress:
      case ISD.GlobalTLSAddress: {
        SDNode.GlobalAddressSDNode addrNode = (SDNode.GlobalAddressSDNode) node;
        id.addInteger(addrNode.getGlobalValue().hashCode());
        id.addInteger(addrNode.getOffset());
        id.addInteger(addrNode.getTargetFlags());
        break;
      }
      case ISD.BasicBlock:
        id.addInteger(((SDNode.BasicBlockSDNode) node).getBasicBlock().hashCode());
        break;
      case ISD.Register:
        id.addInteger(((SDNode.RegisterSDNode) node).getReg());
        break;
      case ISD.MEMOPERAND:
        MachineMemOperand mo = ((SDNode.MemOperandSDNode) node).getMachineMemOperand();
        mo.profile(id);
        break;
      case ISD.FrameIndex:
      case ISD.TargetFrameIndex:
        id.addInteger(((SDNode.FrameIndexSDNode) node).getFrameIndex());
        break;
      case ISD.JumpTable:
      case ISD.TargetJumpTable:
        id.addInteger(((SDNode.JumpTableSDNode) node).getJumpTableIndex());
        break;
      case ISD.ConstantPool:
      case ISD.TargetConstantPool: {
        SDNode.ConstantPoolSDNode pool = (SDNode.ConstantPoolSDNode) node;
        id.addInteger(pool.getAlign());
        id.addInteger(pool.getOffset());
        if (pool.isMachineConstantPoolValue())
          pool.getMachineConstantPoolValue().addSelectionDAGCSEId(id);
        else
          id.addInteger(pool.getConstantValue().hashCode());
        id.addInteger(pool.getTargetFlags());
        break;
      }
      case ISD.LOAD: {
        SDNode.LoadSDNode load = (SDNode.LoadSDNode) node;
        id.addInteger(load.getMemoryVT().getRawBits().hashCode());
        id.addInteger(load.getRawSubclassData());
        break;
      }
      case ISD.STORE: {
        SDNode.StoreSDNode store = (SDNode.StoreSDNode) node;
        id.addInteger(store.getMemoryVT().getRawBits().hashCode());
        id.addInteger(store.getRawSubclassData());
        break;
      }
      case ISD.ATOMIC_CMP_SWAP:
      case ISD.ATOMIC_SWAP:
      case ISD.ATOMIC_LOAD_ADD:
      case ISD.ATOMIC_LOAD_SUB:
      case ISD.ATOMIC_LOAD_AND:
      case ISD.ATOMIC_LOAD_OR:
      case ISD.ATOMIC_LOAD_XOR:
      case ISD.ATOMIC_LOAD_NAND:
      case ISD.ATOMIC_LOAD_MIN:
      case ISD.ATOMIC_LOAD_MAX:
      case ISD.ATOMIC_LOAD_UMIN:
      case ISD.ATOMIC_LOAD_UMAX: {
        SDNode.AtomicSDNode atomNode = (SDNode.AtomicSDNode) node;
        id.addInteger(atomNode.getMemoryVT().getRawBits().hashCode());
        id.addInteger(atomNode.getRawSubclassData());
        break;
      }
    }
  }

  private static void addNodeToIDNode(FoldingSetNodeID id,
                                      int opc,
                                      SDVTList vtList,
                                      SDValue... ops) {
    addNodeToIDNode(id, opc, vtList, ops, ops.length);
  }

  private static void addNodeToIDNode(FoldingSetNodeID id,
                                      int opc,
                                      SDVTList vtList,
                                      ArrayList<SDValue> ops) {
    SDValue[] temp = new SDValue[ops.size()];
    ops.toArray(temp);
    addNodeToIDNode(id, opc, vtList, temp, temp.length);
  }

  private static void addNodeToIDNode(FoldingSetNodeID id,
                                      int opc,
                                      SDVTList vtList,
                                      SDValue[] ops,
                                      int numOps) {
    id.addInteger(opc);
    id.addInteger(vtList.vts.length);
    for (int i = 0, e = vtList.vts.length; i < e; i++)
      id.addInteger(vtList.vts[i].hashCode());
    for (int i = 0; i < numOps; i++)
      id.addInteger(ops[i].hashCode());
  }

  public void clear() {
    allNodes.clear();
    cseMap.clear();
    Collections.fill(condCodeNodes, null);
    entryNode.useList = null;
    add(entryNode);
    root = getEntryNode();
  }

  public SDValue getEntryNode() {
    return new SDValue(entryNode, 0);
  }

  public SDValue getRoot() {
    return root;
  }

  public void setRoot(SDValue root) {
    Util.assertion(root.getNode() == null || root.getValueType().equals(new EVT(MVT.Other)), "Not a legal root!");

    this.root = root;
  }

  public SDValue getMergeValues(ArrayList<SDValue> ops) {
    if (ops.size() == 1)
      return ops.get(0);

    EVT[] vts = ops.stream().map(op -> op.getValueType()).toArray(EVT[]::new);
    SDValue[] ops_ = ops.stream().toArray(SDValue[]::new);
    return getNode(ISD.MERGE_VALUES, getVTList(vts), ops_);
  }

  public SDValue getMergeValues(SDValue[] ops) {
    EVT[] vts = new EVT[ops.length];
    for (int i = 0, e = ops.length; i < e; i++)
      vts[i] = ops[i].getValueType();

    return getNode(ISD.MERGE_VALUES, getVTList(vts), ops);
  }

  public void removeDeadNodes() {
    HandleSDNode dummy = new HandleSDNode(getRoot());

    ArrayList<SDNode> deadNodes = new ArrayList<>();
    for (SDNode node : allNodes) {
      if (node.isUseEmpty())
        deadNodes.add(node);
    }
    collectDeadNodes(deadNodes);
    deallocateAllNodes();
    setRoot(dummy.getValue());
    dummy.dropOperands();
  }

  public void collectDeadNode(SDNode node) {
    ArrayList<SDNode> nodes = new ArrayList<>();
    nodes.add(node);
    collectDeadNodes(nodes);
  }

  private void deallocateAllNodes() {
    for (int i = allNodes.size() -1 ; i >= 0; i--) {
      if (allNodes.get(i).isDeleted()) {
        allNodes.remove(i);
      }
    }
  }


  /**
   * Remove dead nodes and use variable {@code deallocate} to indicate if we should erase those dead
   * nodes from allNodes list
   * @param deadNodes
   */
  public void collectDeadNodes(ArrayList<SDNode> deadNodes) {
    for (int i = 0, e = deadNodes.size(); i < e; i++) {
      SDNode node = deadNodes.get(0);
      deadNodes.remove(i);
      --e;
      --i;
      if (node == null) continue;

      // Erase the specified SDNode and replace all uses of it with null.
      removeNodeFromCSEMaps(node);
      if (node.operandList != null && node.operandList.length > 0) {
        for (SDUse use : node.operandList) {
          SDNode operand = use.getNode();
          use.set(new SDValue());

          if (operand != null && operand.isUseEmpty()) {
            deadNodes.add(operand);
            ++e;
          }
        }
      }

      // we don't delete it from allNodes list, instead mark it  as DELETED_NODE, so we can
      // ignore it when iterating on it.
      node.markDeleted();
    }
  }

  private boolean removeNodeFromCSEMaps(SDNode node) {
    boolean erased = false;
    if (node == null) return erased;
    switch (node.getOpcode()) {
      case ISD.EntryToken:
        Util.shouldNotReachHere("EntryToken should not be in cseMap!");
        return false;
      case ISD.HANDLENODE:
        return false;
      case ISD.CONDCODE:
        if (condCodeNodes.get(((CondCodeSDNode) node).getCondition().ordinal()) != null) {
          erased = true;
          condCodeNodes.set(((CondCodeSDNode) node).getCondition().ordinal(),
              null);
        }
        break;
      case ISD.TargetExternalSymbol:
        ExternalSymbolSDNode sym = (ExternalSymbolSDNode) node;
        erased = targetExternalSymbols.remove(Pair
            .get(sym.getExtSymol(), sym.getTargetFlags())) != null;
        break;
      case ISD.VALUETYPE:
        EVT vt = ((VTSDNode) node).getVT();
        if (vt.isExtended())
          erased = extendedValueTypeNodes.remove(vt) != null;
        else {
          erased = valueTypeNodes.get(vt.getSimpleVT().simpleVT) != null;
          valueTypeNodes.set(vt.getSimpleVT().simpleVT, null);
        }
        break;
      default:
        // remove it from cseMap.
        int[] keys = cseMap.keys();
        for (int key : keys) {
          if (cseMap.get(key).equals(node)) {
            cseMap.remove(key);
            erased = true;
          }
        }
        break;
    }
    return erased;
  }

  /**
   * This method is similar to {@linkplain #replaceAllUsesOfValueWith(SDValue, SDValue)}
   * other than DAGUpdateListener is null by default.
   *
   * @param oldNode
   * @param newNode
   */
  public void replaceAllUsesOfValueWith(SDValue oldNode, SDValue newNode) {
    replaceAllUsesOfValueWith(oldNode, newNode, null);
  }

  /**
   * Replace any uses of {@code oldNode} with {@code newNode}, leaving uses
   * of other values produced by {@code oldNode.getNode()} alone.
   *
   * @param oldNode
   * @param newNode
   * @param listener
   */
  public void replaceAllUsesOfValueWith(SDValue oldNode, SDValue newNode,
                                        DAGUpdateListener listener) {
    if (Objects.equals(oldNode, newNode))
      return;
    if (oldNode.getNode().getNumValues() == 1) {
      // handle trivial case
      replaceAllUsesWith(oldNode.getNode(), newNode.getNode(), listener);
      return;
    }

    ArrayList<SDUse> useList = oldNode.getNode().useList;
    int i = 0, e = useList.size();

    while (i < e) {
      SDUse u = useList.get(i);
      SDNode user = u.user;
      boolean userRemovedFromCSEMaps = false;

      do {
        u = useList.get(i);
        if (u.getResNo() != oldNode.getResNo()) {
          ++i;
          continue;
        }
        if (!userRemovedFromCSEMaps) {
          removeNodeFromCSEMaps(user);
          userRemovedFromCSEMaps = true;
        }
        ++i;
        u.set(newNode);
        // we need to decrement index i caused by removing use by u.set(newNode)
        --i;
        e = useList.size();
      } while (i < e && useList.get(i).user.equals(user));

      if (!userRemovedFromCSEMaps)
        continue;

      addModifiedNodeToCSEMaps(user, listener);
    }
  }

  public boolean maskedValueIsZero(SDValue op, APInt mask) {
    return maskedValueIsZero(op, mask, 0);
  }

  public boolean maskedValueIsZero(SDValue op, APInt mask, int depth) {
    APInt[] res = new APInt[2];
    computeMaskedBits(op, mask, res, depth);
    Util.assertion(res[0].and(res[1]).eq(0));
    return res[0].and(mask).eq(mask);
  }

  public void computeMaskedBits(SDValue op, APInt mask, APInt[] knownVals, int depth) {
    Util.assertion(knownVals != null && knownVals.length == 2, "Illegal knownVals");

    int bitwidth = mask.getBitWidth();
    Util.assertion(bitwidth == op.getValueType().getSizeInBits());
    knownVals[0] = new APInt(bitwidth, 0);
    knownVals[1] = new APInt(bitwidth, 0);
    if (depth == 6 || mask.eq(0))
      return;

    // For knownZero2 and knownOne2.
    APInt[] knownVals2 = new APInt[2];

    switch (op.getOpcode()) {
      case ISD.Constant:
        knownVals[1] = ((ConstantSDNode) op.getNode()).getAPIntValue().and(mask);
        knownVals[0] = knownVals[1].not().and(mask);
        break;
      case ISD.AND:
        computeMaskedBits(op.getOperand(1), mask, knownVals, depth + 1);
        computeMaskedBits(op.getOperand(0), mask.and(knownVals[0].not()),
            knownVals2, depth + 1);
        Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
        Util.assertion(knownVals2[0].and(knownVals2[1]).eq(0));

        knownVals[1] = knownVals[1].and(knownVals2[1]);
        knownVals[0] = knownVals[0].or(knownVals2[0]);
        break;
      case ISD.OR:
        computeMaskedBits(op.getOperand(1), mask, knownVals, depth + 1);
        computeMaskedBits(op.getOperand(0), mask.and(knownVals[1].not()),
            knownVals2, depth + 1);
        Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
        Util.assertion(knownVals2[0].and(knownVals2[1]).eq(0));

        knownVals[0] = knownVals[0].and(knownVals2[0]);
        knownVals[1] = knownVals[1].or(knownVals2[1]);
        break;
      case ISD.XOR: {
        computeMaskedBits(op.getOperand(1), mask, knownVals, depth + 1);
        computeMaskedBits(op.getOperand(0), mask, knownVals2, depth + 1);
        Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
        Util.assertion(knownVals2[0].and(knownVals2[1]).eq(0));

        APInt knownZeroOut = knownVals[0].and(knownVals2[0]).or(knownVals[1].and(knownVals2[1]));
        knownVals[1] = knownVals[0].and(knownVals2[1]).or(knownVals[1].and(knownVals2[0]));
        knownVals[0] = knownZeroOut;
        break;
      }
      case ISD.MUL: {
        computeMaskedBits(op.getOperand(1), mask, knownVals, depth + 1);
        computeMaskedBits(op.getOperand(0), mask.and(knownVals[0].not()),
            knownVals2, depth + 1);
        Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
        Util.assertion(knownVals2[0].and(knownVals2[1]).eq(0));

        knownVals[0].clear();
        int trailOne = knownVals[0].countTrailingOnes() + knownVals2[0].countTrailingOnes();
        int leadOne = Math.max(
            knownVals[0].countLeadingOnes() + knownVals2[0].countLeadingOnes(), bitwidth) - bitwidth;
        trailOne = Math.min(trailOne, bitwidth);
        leadOne = Math.min(leadOne, bitwidth);

        knownVals[0] = APInt.getLowBitsSet(bitwidth, trailOne)
            .or(APInt.getHighBitsSet(bitwidth, leadOne));
        knownVals[0] = knownVals[0].and(mask);
        break;
      }
      case ISD.UDIV: {
        APInt allOnes = APInt.getAllOnesValue(bitwidth);
        computeMaskedBits(op.getOperand(0), allOnes, knownVals2,
            depth + 1);
        int leadOne = knownVals2[0].countLeadingOnes();

        knownVals2[1].clear();
        knownVals2[0].clear();
        computeMaskedBits(op.getOperand(1), allOnes, knownVals2,
            depth + 1);
        int rhsUnknownLeadingOnes = knownVals2[1].countLeadingZeros();
        if (rhsUnknownLeadingOnes != bitwidth)
          leadOne = Math.min(bitwidth,
              leadOne + bitwidth - rhsUnknownLeadingOnes - 1);
        knownVals[0] = APInt.getHighBitsSet(bitwidth, leadOne).and(mask);
        break;
      }
      case ISD.SELECT: {
        computeMaskedBits(op.getOperand(2), mask, knownVals, depth + 1);
        computeMaskedBits(op.getOperand(1), mask, knownVals2, depth + 1);
        Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
        Util.assertion(knownVals2[0].and(knownVals2[1]).eq(0));

        knownVals[1] = knownVals[1].and(knownVals2[1]);
        knownVals[0] = knownVals[0].and(knownVals2[0]);
        break;
      }
      case ISD.SELECT_CC: {
        computeMaskedBits(op.getOperand(3), mask, knownVals, depth + 1);
        computeMaskedBits(op.getOperand(2), mask, knownVals2, depth + 1);
        Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
        Util.assertion(knownVals2[0].and(knownVals2[1]).eq(0));

        knownVals[1] = knownVals[1].and(knownVals2[1]);
        knownVals[0] = knownVals[0].and(knownVals2[0]);
        break;
      }
      case ISD.SADDO:
      case ISD.UADDO:
      case ISD.SSUBO:
      case ISD.USUBO:
      case ISD.SMULO:
      case ISD.UMULO: {
        if (op.getResNo() != 1)
          break;
      }
      case ISD.SETCC: {
        if (tli.getBooleanContents() == ZeroOrOneBooleanContent
            && bitwidth > 1)
          knownVals[0] =
              knownVals[0].or(APInt.getHighBitsSet(bitwidth, bitwidth - 1));
        break;
      }
      case ISD.SHL:
        // (shl X, C1) & C2 == 0   iff   (X & C2 >>u C1) == 0
        if (op.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode sd = (ConstantSDNode) op.getOperand(1).getNode();
          long shAmt = sd.getZExtValue();
          if (shAmt >= bitwidth)
            break;

          computeMaskedBits(op.getOperand(0), mask.lshr(shAmt),
              knownVals, depth + 1);
          Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
          knownVals[0] = knownVals[0].shl((int) shAmt);
          knownVals[1] = knownVals[1].shl((int) shAmt);
          knownVals[0] = knownVals[0]
              .or(APInt.getLowBitsSet(bitwidth, (int) shAmt));
        }
        break;
      case ISD.SRL: {
        // (ushr X, C1) & C2 == 0   iff  (-1 >> C1) & C2 == 0
        if (op.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode sd = (ConstantSDNode) op.getOperand(1).getNode();
          long shAmt = sd.getZExtValue();
          if (shAmt >= bitwidth)
            break;

          computeMaskedBits(op.getOperand(0), mask.shl((int) shAmt),
              knownVals, depth + 1);
          Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
          knownVals[0] = knownVals[0].lshr((int) shAmt);
          knownVals[1] = knownVals[1].lshr((int) shAmt);
          knownVals[0] = knownVals[0]
              .or(APInt.getHighBitsSet(bitwidth, (int) shAmt)).and(mask);
        }
        break;
      }
      case ISD.SRA:
        // (ushr X, C1) & C2 == 0   iff  (-1 >> C1) & C2 == 0
        if (op.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode sd = (ConstantSDNode) op.getOperand(1).getNode();
          long shAmt = sd.getZExtValue();
          if (shAmt >= bitwidth)
            break;

          APInt inDemandedMask = mask.shl((int) shAmt);
          APInt highBits = APInt.getHighBitsSet(bitwidth, (int) shAmt)
              .and(mask);
          if (highBits.getBoolValue())
            inDemandedMask.orAssign(APInt.getSignBit(bitwidth));

          computeMaskedBits(op.getOperand(0), inDemandedMask,
              knownVals, depth + 1);
          Util.assertion(knownVals[0].and(knownVals[1]).eq(0));

          knownVals[0] = knownVals[0].lshr((int) shAmt);
          knownVals[1] = knownVals[1].lshr((int) shAmt);

          APInt signBit = APInt.getSignBit(bitwidth);
          signBit.lshrAssign((int) shAmt);
          if (knownVals[0].intersects(signBit)) {
            knownVals[0].orAssign(highBits);
          } else if (knownVals[1].intersects(signBit)) {
            knownVals[1].orAssign(highBits);
          }
        }
        break;
      case ISD.SIGN_EXTEND_INREG: {
        EVT vt = ((VTSDNode) op.getOperand(1).getNode()).getVT();
        int bits = vt.getSizeInBits();
        APInt newBits = APInt.getHighBitsSet(bitwidth, bitwidth - bits).and(mask);
        APInt inSignBit = APInt.getSignBit(bits);
        APInt inputDemandedBits = mask.and(APInt.getLowBitsSet(bitwidth, bits));
        inSignBit = inSignBit.zext(bitwidth);
        if (newBits.getBoolValue())
          inputDemandedBits.orAssign(inSignBit);

        computeMaskedBits(op.getOperand(0), inputDemandedBits, knownVals, depth + 1);
        Util.assertion(knownVals[0].add(knownVals[1]).eq(0), "Bits known to be one AND zero?");
        if (knownVals[0].intersects(inSignBit)) {
          knownVals[0].orAssign(newBits);
          knownVals[1].andAssign(newBits.not());
        } else if (knownVals[1].intersects(inSignBit)) {
          knownVals[1].orAssign(newBits);
          knownVals[0].andAssign(newBits.not());
        } else {
          knownVals[0].andAssign(newBits.not());
          knownVals[1].andAssign(newBits.not());
        }
        break;
      }
      case ISD.CTTZ:
      case ISD.CTLZ:
      case ISD.CTPOP: {
        int lowBits = Util.log2(bitwidth) + 1;
        knownVals[0] = APInt.getHighBitsSet(bitwidth, bitwidth - lowBits);
        knownVals[1].clear();
        break;
      }
      case ISD.LOAD: {
        if (op.getNode().isZEXTLoad()) {
          LoadSDNode ld = (LoadSDNode) op.getNode();
          EVT vt = ld.getMemoryVT();
          int memBits = vt.getSizeInBits();
          knownVals[0].orAssign(
              APInt.getHighBitsSet(bitwidth, bitwidth - memBits)).and(mask);
        }
        break;
      }
      case ISD.ZERO_EXTEND: {
        EVT inVT = op.getOperand(0).getValueType();
        int inBits = inVT.getSizeInBits();
        APInt newBits = APInt.getHighBitsSet(bitwidth, bitwidth - inBits).and(mask);
        APInt inMask = new APInt(mask);
        inMask = inMask.trunc(inBits);
        knownVals[0] = knownVals[0].trunc(inBits);
        knownVals[1] = knownVals[1].trunc(inBits);
        computeMaskedBits(op.getOperand(0), inMask, knownVals, depth + 1);
        knownVals[0] = knownVals[0].zext(bitwidth);
        knownVals[1] = knownVals[1].zext(bitwidth);
        knownVals[0].orAssign(newBits);
        break;
      }
      case ISD.SIGN_EXTEND: {
        EVT inVT = op.getOperand(0).getValueType();
        int inBits = inVT.getSizeInBits();
        APInt inSignBits = APInt.getSignBit(inBits);
        APInt newBits = APInt.getHighBitsSet(bitwidth, bitwidth - inBits).and(mask);
        APInt inMask = new APInt(mask);
        inMask = inMask.trunc(inBits);
        if (newBits.getBoolValue())
          inMask.orAssign(inSignBits);

        knownVals[0] = knownVals[0].trunc(inBits);
        knownVals[1] = knownVals[1].trunc(inBits);

        computeMaskedBits(op.getOperand(0), inMask, knownVals, depth + 1);
        boolean signBitKnownZero = knownVals[0].isNegative();
        boolean signBitKnownOne = knownVals[1].isNegative();
        Util.assertion(!(signBitKnownOne && signBitKnownZero));
        inMask = new APInt(mask);
        inMask = inMask.trunc(inBits);
        knownVals[0].andAssign(inMask);
        knownVals[1].andAssign(inMask);

        knownVals[0] = knownVals[0].zext(bitwidth);
        knownVals[1] = knownVals[1].zext(bitwidth);

        if (signBitKnownZero)
          knownVals[0].orAssign(newBits);
        else if (signBitKnownOne)
          knownVals[1].orAssign(newBits);

        break;
      }
      case ISD.ANY_EXTEND: {
        EVT inVT = op.getOperand(0).getValueType();
        int inBits = inVT.getSizeInBits();
        APInt inMask = new APInt(mask);
        inMask = inMask.trunc(inBits);
        knownVals[0] = knownVals[0].trunc(inBits);
        knownVals[1] = knownVals[1].trunc(inBits);
        computeMaskedBits(op.getOperand(0), inMask, knownVals, depth + 1);
        knownVals[0] = knownVals[0].zext(bitwidth);
        knownVals[1] = knownVals[1].zext(bitwidth);
        break;
      }
      case ISD.TRUNCATE: {
        EVT inVT = op.getOperand(0).getValueType();
        int inBits = inVT.getSizeInBits();
        APInt inMask = new APInt(mask);
        inMask = inMask.zext(inBits);
        knownVals[0] = knownVals[0].zext(inBits);
        knownVals[1] = knownVals[1].zext(inBits);
        computeMaskedBits(op.getOperand(0), inMask, knownVals, depth + 1);
        knownVals[0] = knownVals[0].trunc(bitwidth);
        knownVals[1] = knownVals[1].trunc(bitwidth);
        break;
      }
      case ISD.AssertZext: {
        EVT vt = ((VTSDNode) op.getOperand(1).getNode()).getVT();
        APInt inMask = APInt.getLowBitsSet(bitwidth, vt.getSizeInBits());
        computeMaskedBits(op.getOperand(0), mask.and(inMask), knownVals,
            depth + 1);
        knownVals[0].orAssign(inMask.not().and(mask));
        break;
      }
      case ISD.FGETSIGN:
        knownVals[0] = APInt.getHighBitsSet(bitwidth, bitwidth - 1);
        break;
      case ISD.SUB: {
        if (op.getOperand(0).getNode() instanceof ConstantSDNode) {
          ConstantSDNode cn = (ConstantSDNode) op.getOperand(0).getNode();
          if (cn.getAPIntValue().isNonNegative()) {
            int nlz = cn.getAPIntValue().add(1).countLeadingZeros();
            APInt maskV = APInt.getHighBitsSet(bitwidth, nlz + 1);
            computeMaskedBits(op.getOperand(1), maskV, knownVals2,
                depth + 1);

            if (knownVals2[0].and(maskV).eq(maskV)) {
              int nlz2 = cn.getAPIntValue().countLeadingZeros();
              knownVals[0] = APInt.getHighBitsSet(bitwidth, nlz2).and(mask);
            }
          }
        }
        // fall through
      }
      case ISD.ADD: {
        APInt mask2 = APInt.getLowBitsSet(bitwidth, mask.countTrailingOnes());
        computeMaskedBits(op.getOperand(0), mask2, knownVals2, depth + 1);
        Util.assertion(knownVals2[0].and(knownVals[1]).eq(0));
        int knownZeroOut = knownVals2[0].countTrailingOnes();

        computeMaskedBits(op.getOperand(1), mask2, knownVals2, depth + 1);
        Util.assertion(knownVals2[0].and(knownVals[1]).eq(0));
        knownZeroOut = Math.min(knownZeroOut, knownVals2[0].countTrailingOnes());

        knownVals[0].orAssign(APInt.getLowBitsSet(bitwidth, knownZeroOut));
        break;
      }
      case ISD.SREM: {
        if (op.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode cn = (ConstantSDNode) op.getOperand(1).getNode();
          APInt ra = cn.getAPIntValue();
          if (ra.isPowerOf2() || ra.negative().isPowerOf2()) {
            APInt lowBits = ra.isStrictlyPositive() ?
                ra.decrease() :
                ra.not();
            APInt mask2 = lowBits.or(APInt.getSignBit(bitwidth));
            computeMaskedBits(op.getOperand(0), mask2, knownVals2,
                depth + 1);

            if (knownVals2[0].get(bitwidth - 1) || knownVals2[0].and(lowBits).eq(lowBits))
              knownVals2[0].orAssign(lowBits.not());

            knownVals[0].orAssign(knownVals2[0].and(mask));
            Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
          }
        }
        break;
      }
      case ISD.UREM: {
        if (op.getOperand(1).getNode() instanceof ConstantSDNode) {
          ConstantSDNode cn = (ConstantSDNode) op.getOperand(1).getNode();
          APInt ra = cn.getAPIntValue();
          if (ra.isPowerOf2()) {
            APInt lowBits = ra.decrease();
            APInt mask2 = lowBits.and(mask);
            knownVals[0].orAssign(lowBits.negative().and(mask));
            computeMaskedBits(op.getOperand(0), mask2, knownVals,
                depth + 1);
            Util.assertion(knownVals[0].and(knownVals[1]).eq(0));
            break;
          }
        }

        APInt allOnes = APInt.getAllOnesValue(bitwidth);
        computeMaskedBits(op.getOperand(0), allOnes, knownVals,
            depth + 1);
        computeMaskedBits(op.getOperand(1), allOnes, knownVals2,
            depth + 1);

        int leaders = Math.max(knownVals[0].countLeadingOnes(),
            knownVals2[0].countLeadingOnes());
        knownVals[1].clear();
        knownVals[0] = APInt.getHighBitsSet(bitwidth, leaders).and(mask);
        break;
      }
      default:
        if (op.getOpcode() >= ISD.BUILTIN_OP_END) {
          switch (op.getOpcode()) {
            case ISD.INTRINSIC_WO_CHAIN:
            case ISD.INTRINSIC_VOID:
            case ISD.INTRINSIC_W_CHAIN:
              tli.computeMaskedBitsForTargetNode(op, mask,
                  knownVals, this, depth);
          }
        }
    }
  }

  public SDValue getZeroExtendInReg(SDValue op, EVT vt) {
    if (op.getValueType().equals(vt))
      return op;
    APInt imm = APInt.getLowBitsSet(op.getValueSizeInBits(), vt.getSizeInBits());
    return getNode(ISD.AND, op.getValueType(), op,
        getConstant(imm, op.getValueType(), false));
  }

  public SDValue getAtomic(int opcode, EVT memoryVT, SDValue chain,
                           SDValue ptr, SDValue val, Value ptrVal, int alignment) {
    Util.assertion(opcode == ISD.ATOMIC_LOAD_ADD ||
        opcode == ISD.ATOMIC_LOAD_SUB ||
        opcode == ISD.ATOMIC_LOAD_AND ||
        opcode == ISD.ATOMIC_LOAD_OR ||
        opcode == ISD.ATOMIC_LOAD_XOR ||
        opcode == ISD.ATOMIC_LOAD_NAND ||
        opcode == ISD.ATOMIC_LOAD_MIN ||
        opcode == ISD.ATOMIC_LOAD_MAX ||
        opcode == ISD.ATOMIC_LOAD_UMIN ||
        opcode == ISD.ATOMIC_LOAD_UMAX ||
        opcode == ISD.ATOMIC_SWAP);

    EVT vt = val.getValueType();

    if (alignment == 0)
      alignment = getEVTAlignment(memoryVT);

    SDVTList vts = getVTList(vt, new EVT(MVT.Other));
    FoldingSetNodeID compute = new FoldingSetNodeID();
    compute.addInteger(memoryVT.getRawBits().hashCode());
    SDValue[] ops = {chain, ptr, val};
    addNodeToIDNode(compute, opcode, vts, ops);
    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);

    int flags = MOLoad | MachineMemOperand.MOStore;

    // For now, atomics are considered to be volatile always.
    flags |= MOVolatile;
    MachineMemOperand mmo = new MachineMemOperand(ptrVal, flags, 0, memoryVT.getStoreSize(), alignment);
    SDNode n = new AtomicSDNode(opcode, vts, memoryVT, chain, ptr, val, mmo);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getAtomic(int opc, EVT memVT, SDValue chain, SDValue ptr,
                           SDValue cmp, SDValue swap, Value ptrVal, int alignment) {
    Util.assertion(opc == ISD.ATOMIC_CMP_SWAP, "Invalid atomic op!");
    Util.assertion(cmp.getValueType().equals(swap.getValueType()), "Invalid atomic op type!");


    EVT vt = cmp.getValueType();
    if (alignment == 0) {
      alignment = getEVTAlignment(memVT);
    }

    SDVTList vts = getVTList(vt, new EVT(MVT.Other));
    FoldingSetNodeID compute = new FoldingSetNodeID();
    compute.addInteger(memVT.getRawBits().hashCode());
    SDValue[] ops = {chain, ptr, cmp, swap};
    addNodeToIDNode(compute, opc, vts, ops);
    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);

    int flags = MOLoad | MachineMemOperand.MOStore;

    // For now, atomics are considered to be volatile always.
    flags |= MOVolatile;
    MachineMemOperand mmo = new MachineMemOperand(ptrVal, flags, 0, memVT.getStoreSize(), alignment);
    SDNode n = new AtomicSDNode(opc, vts, memVT, chain, ptr, cmp, swap, mmo);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getConvertRndSat(EVT vt, SDValue val, SDValue dty,
                                  SDValue sty, SDValue rnd, SDValue sat, CvtCode cc) {
    if (dty.equals(sty) && (cc == CvtCode.CVT_UU || cc == CvtCode.CVT_SS
        || cc == CvtCode.CVT_FF))
      return val;

    FoldingSetNodeID compute = new FoldingSetNodeID();
    SDVTList vts = getVTList(vt);
    SDValue[] ops = {val, dty, sty, rnd, sat};
    addNodeToIDNode(compute, ISD.CONVERT_RNDSAT, vts, ops);
    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode res = new CvtRndSatSDNode(vt, cc, ops);
    cseMap.put(id, res);
    add(res);
    return new SDValue(res, 0);
  }

  public SDValue getVAArg(EVT vt, SDValue chain, SDValue ptr, SDValue sv) {
    SDValue[] ops = {chain, ptr, sv};
    return getNode(ISD.VAARG, getVTList(vt, new EVT(MVT.Other)), ops);
  }

  public void deleteNode(SDNode node) {
    removeNodeFromCSEMaps(node);
    deleteNodeNotInCSEMap(node);
  }

  public SDValue getMemIntrinsicNode(int opc, SDVTList vts,
                                     ArrayList<SDValue> ops,
                                     EVT memVT, Value srcVal,
                                     int offset, int align,
                                     boolean vol, boolean readMem,
                                     boolean writeMem) {
    MemIntrinsicSDNode n = null;
    SDValue[] temp = new SDValue[ops.size()];
    ops.toArray(temp);
    int Flags = 0;
    if (writeMem)
      Flags |= MOStore;
    if (readMem)
      Flags |= MOLoad;
    if (vol)
      Flags |= MOVolatile;
    MachineMemOperand mmo = new MachineMemOperand(srcVal, Flags, offset,
            memVT.getStoreSize(), align);

    if (vts.vts[vts.numVTs - 1].getSimpleVT().simpleVT != MVT.Flag) {
      FoldingSetNodeID compute = new FoldingSetNodeID();
      addNodeToIDNode(compute, opc, vts, temp);
      int id = compute.computeHash();
      if (cseMap.containsKey(id))
        return new SDValue(cseMap.get(id), 0);

      n = new MemIntrinsicSDNode(opc, vts, temp, memVT, mmo);
      cseMap.put(id, n);
    } else {
      n = new MemIntrinsicSDNode(opc, vts, temp, memVT,mmo);
    }
    allNodes.add(n);
    return new SDValue(n, 0);
  }

  public SDValue getMemset(SDValue chain,
                           SDValue dst,
                           SDValue src,
                           SDValue size,
                           int align,
                           Value destSV,
                           long dstSVOff) {
    if (size.getNode() instanceof ConstantSDNode) {
      ConstantSDNode csd = (ConstantSDNode)size.getNode();
      if (csd.isNullValue())
        return chain;

      SDValue result = getMemsetStores(chain, dst, src, csd.getZExtValue(),
          align, destSV, dstSVOff);
      if (result.getNode() != null)
        return result;
    }

    SDValue result = tli.emitTargetCodeForMemset(this, chain, dst, src, size,
        align, destSV, dstSVOff);
    if (result.getNode() != null)
      return result;

    // emit a library call.
    Type intPtrty = tli.getTargetData().getIntPtrType();
    ArrayList<ArgListEntry> args = new ArrayList<>();
    ArgListEntry entry = new ArgListEntry();
    entry.node = dst;
    entry.ty = intPtrty;
    args.add(entry);
    // extend or truncate the argument to be an i32 value for the call.
    if (src.getValueType().bitsGT(new EVT(MVT.i32)))
      src = getNode(ISD.TRUNCATE, new EVT(MVT.i32), src);
    else
      src = getNode(ISD.ZERO_EXTEND, new EVT(MVT.i32), src);

    entry = new ArgListEntry();
    entry.node = src;
    entry.ty = LLVMContext.Int32Ty;
    entry.isSExt = true;
    args.add(entry);
    entry = new ArgListEntry();
    entry.node = size;
    entry.ty = intPtrty;
    entry.isSExt = false;
    args.add(entry);

    Pair<SDValue, SDValue> callResult =
        tli.lowerCallTo(chain, LLVMContext.VoidTy,
            false, false, false, false, 0,
            tli.getLibCallCallingConv(RTLIB.MEMSET),
            false, false, getExternalSymbol(tli.getLibCallName(RTLIB.MEMSET),
                new EVT(tli.getPointerTy())), args, this);

    return callResult.second;
  }

  private SDValue getMemsetStores(SDValue chain,
                                 SDValue dst,
                                 SDValue src,
                                 long size,
                                 int align,
                                 Value dstSV,
                                 long dstSVOff) {
    TargetLowering tli = getTargetLoweringInfo();
    ArrayList<EVT> memOps = new ArrayList<>();
    OutRef<String> str = new OutRef<>("");
    OutRef<Boolean> copyFromStr = new OutRef<>(false);
    if (!meetsMaxMemopRequirement(memOps, dst, src, tli.getMaxStoresPerMemset(),
        size, align, str, copyFromStr)) {
      return new SDValue();
    }

    ArrayList<SDValue> outChains = new ArrayList<>();
    long dstOff = 0;
    int numMemOps = memOps.size();
    for (int i = 0; i < numMemOps; i++) {
      EVT vt = memOps.get(i);
      int vtSize = vt.getSizeInBits()/8;
      SDValue value = getMemsetValue(src, vt);
      SDValue store = getStore(chain, value, getMemBasePlusOffset(dst, (int) dstOff),
          dstSV, (int) (dstSVOff+dstOff), false, 0);
      outChains.add(store);
      dstOff += vtSize;
    }
    return getNode(ISD.TokenFactor, new EVT(MVT.Other), outChains);
  }

  private SDValue getMemsetValue(SDValue value, EVT vt) {
    int numBits = vt.getScalarType().getSizeInBits();
    if (value.getNode() instanceof ConstantSDNode) {
      ConstantSDNode c = (ConstantSDNode) value.getNode();
      APInt val = new APInt(numBits, c.getZExtValue()&255);
      int shift = 8;
      for (int i = numBits; i >8; i >>>= 1) {
        val = (val.shl(shift)).or(val);
        shift <<= 1;
      }
      if (vt.isInteger())
        return getConstant(val, vt, false);
      return getConstantFP(new APFloat(val), vt, false);
    }
    return value;
  }

  public boolean signBitIsZero(SDValue n, int depth) {
    if (n.getValueType().isVector())
      return false;
    int bitwidth = n.getValueSizeInBits();
    return maskedValueIsZero(n, APInt.getSignBit(bitwidth), depth);
  }

  /**
   * A convenience function for creating TargetOpcodes.EXTRACT_SUBREG nodes.
   * @param srIdx
   * @param vt
   * @param operand
   * @return
   */
  public SDValue getTargetExtractSubreg(int srIdx, EVT vt, SDValue operand) {
    SDValue srIdxVal = getTargetConstant(srIdx, new EVT(MVT.i32));
    SDNode subreg = getMachineNode(TargetOpcodes.EXTRACT_SUBREG, vt, operand, srIdxVal);
    return new SDValue(subreg, 0);
  }

  /**
   * A convenience function for creating TargetOpcodes.INSERT_SUBREG nodes.
   * @param srIdx
   * @param vt
   * @param operand
   * @return
   */
  public SDValue getTargetInsertSubreg(int srIdx, EVT vt,
                                       SDValue operand, SDValue subreg) {
    SDValue srIdxVal = getTargetConstant(srIdx, new EVT(MVT.i32));
    SDNode result = getMachineNode(TargetOpcodes.INSERT_SUBREG, vt,
        operand, subreg, srIdxVal);
    return new SDValue(result, 0);
  }

  public SDValue getGLOBAL_OFFSET_TABLE(EVT vt) {
    return getNode(ISD.GLOBAL_OFFSET_TABLE, vt);
  }

  static class UseMemo {
    SDNode user;
    int index;
    SDUse use;

    public UseMemo(SDNode node, int idx, SDUse u) {
      user = node;
      index = idx;
      use = u;
    }
  }

  Comparator<UseMemo> UseMemoComparator = new Comparator<UseMemo>() {
    @Override
    public int compare(UseMemo o1, UseMemo o2) {
      return o1.user.hashCode() - o2.user.hashCode();
    }
  };

  public void replaceAllUsesOfValuesWith(SDValue[] from, SDValue[] to,
                                         DAGUpdateListener listener) {
    if (from.length == 1) {
      replaceAllUsesOfValueWith(from[0], to[0], listener);
      return;
    }

    ArrayList<UseMemo> uses = new ArrayList<>();
    for (int i = 0; i < from.length; i++) {
      int fromResNo = from[i].getResNo();
      SDNode fromNode = from[i].getNode();
      for (SDUse u : fromNode.useList) {
        if (u.getResNo() == fromResNo) {
          uses.add(new UseMemo(u.getNode(), i, u));
        }
      }
    }

    uses.sort(UseMemoComparator);

    for (int userIndex = 0, e = uses.size(); userIndex < e; userIndex++) {
      SDNode user = uses.get(userIndex).user;
      removeNodeFromCSEMaps(user);
      do {
        int i = uses.get(userIndex).index;
        SDUse use = uses.get(userIndex).use;
        ++userIndex;
        use.set(to[i]);
      } while (userIndex != e && Objects
          .equals(uses.get(userIndex).user, user));

      addModifiedNodeToCSEMaps(user, listener);
    }
  }

  private static boolean doNotCSE(SDNode node) {
    if (node.getValueType(0).getSimpleVT().simpleVT == MVT.Flag)
      return true;

    switch (node.getOpcode()) {
      default:
        break;
      case ISD.HANDLENODE:
      case ISD.DBG_LABEL:
      case ISD.DBG_STOPPOINT:
      case ISD.EH_LABEL:
      case ISD.DECLARE:
        return true;
    }

    for (int i = 0, e = node.getNumValues(); i < e; i++) {
      if (node.getValueType(i).getSimpleVT().simpleVT == MVT.Flag)
        return true;
    }
    return false;
  }

  private void addModifiedNodeToCSEMaps(SDNode node, DAGUpdateListener listener) {
    if (!doNotCSE(node)) {
      FoldingSetNodeID compute = new FoldingSetNodeID();
      addNodeToID(compute, node);
      int id = compute.computeHash();
      if (cseMap.containsKey(id)) {
        SDNode existing = cseMap.get(id);
        replaceAllUsesWith(node, existing, listener);

        if (listener != null)
          listener.nodeDeleted(node, existing);
        deleteNodeNotInCSEMap(node);
      }
    }
    if (listener != null)
      listener.nodeUpdated(node);
  }

  private void deleteNodeNotInCSEMap(SDNode node) {
    Util.assertion(!Objects.equals(node, allNodes.get(0)));
    Util.assertion(node.isUseEmpty());

    node.dropOperands();
    markNodeAsDeallocated(node);
  }

  private void markNodeAsDeallocated(SDNode node) {
    // Mark the node as Deleted for capturing bug.
    node.nodeID = ISD.DELETED_NODE;
  }

  public void replaceAllUsesWith(SDNode oldNode, SDNode newNode,
                                 DAGUpdateListener listener) {
    if (Objects.equals(oldNode, newNode))
      return;

    ArrayList<SDUse> temps = new ArrayList<>();
    temps.addAll(oldNode.getUseList());
    int i = 0, e = temps.size();

    while (i < e) {
      SDUse u = temps.get(i);
      SDNode user = u.user;
      removeNodeFromCSEMaps(user);

      // FIXME, fix this bug that adjust the list when iterating on it.
      // FIXME 6/30/2018
      do {
        u = temps.get(i);
        ++i;
        u.setNode(newNode);
      } while (i < e && temps.get(i).user.equals(user));
      addModifiedNodeToCSEMaps(user, listener);
    }
  }

  public void replaceAllUsesWith(SDNode from, SDValue to,
                                 DAGUpdateListener listener) {
    SDValue[] ops = {to};
    replaceAllUsesWith(from, ops, listener);
  }

  public void replaceAllUsesWith(SDNode from, SDValue to[],
                                 DAGUpdateListener listener) {
    if (from.getNumValues() == 1) {
      replaceAllUsesWith(new SDValue(from, 0), to[0], listener);
      return;
    }

    ArrayList<SDUse> temps = new ArrayList<>();
    temps.addAll(from.useList);

    int i = 0, e = temps.size();

    while (i < e) {
      SDUse u = temps.get(i);
      SDNode user = u.user;
      removeNodeFromCSEMaps(user);

      // FIXME, fix this bug that adjust the list when iterating on it.
      // FIXME 6/30/2018
      do {
        u = temps.get(i);
        ++i;
        u.set(to[u.getResNo()]);
      } while (i < e && temps.get(i).user.equals(user));
      addModifiedNodeToCSEMaps(user, listener);
    }
  }

  public void replaceAllUsesWith(SDValue oldNode, SDValue newNode,
                                 DAGUpdateListener listener) {
    SDNode from = oldNode.getNode();
    Util.assertion(from.getNumValues() == 1 && oldNode.getResNo() == 0, "Can't replace with this method!");

    Util.assertion(!Objects.equals(from, newNode.getNode()), "Can't replace uses of with self!");


    ArrayList<SDUse> temps = new ArrayList<>();
    temps.addAll(from.useList);

    int i = 0, e = temps.size();

    while (i < e) {
      SDUse u = temps.get(i);
      SDNode user = u.user;
      removeNodeFromCSEMaps(user);

      // FIXME, fix this bug that adjust the list when iterating on it.
      // FIXME 6/30/2018
      do {
        u = temps.get(i);
        ++i;
        u.set(newNode);
      } while (i < e && temps.get(i).user.equals(user));
      addModifiedNodeToCSEMaps(user, listener);
    }
  }

  public SDValue getRegister(int reg, int ty) {
    return getRegister(reg, new EVT(ty));
  }

  public SDValue getRegister(int reg, EVT ty) {
    FoldingSetNodeID calc = new FoldingSetNodeID();
    addNodeToIDNode(calc, ISD.Register, getVTList(ty), null, 0);
    calc.addInteger(reg);
    int id = calc.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);

    SDNode node = new RegisterSDNode(ty, reg);
    cseMap.put(id, node);
    add(node);
    return new SDValue(node, 0);
  }

  /**
   * Assign an unique ID for each SDNode in {@linkplain this#allNodes} in
   * the topological order.
   *
   * @return
   */
  public int assignTopologicalOrder() {
    int dagSize = 0;
    int insertPos = 0;
    for (int i = 0, e = allNodes.size(); i < e; i++) {
      SDNode node = allNodes.get(i);
      int degree = node.getNumOperands();
      if (degree == 0) {
        if (i != insertPos) {
          allNodes.remove(i);
          add(insertPos, node);
        }
        ++insertPos;
        node.setNodeID(dagSize++);
      } else {
        // set the temporary Node ID as the in degree.
        node.setNodeID(degree);
      }
    }

    for (int i = 0, e = allNodes.size(); i < e; i++) {
      SDNode node = allNodes.get(i);
      if (node != null && node.getUseSize() > 0) {
        for (SDUse use : node.useList) {
          SDNode user = use.getUser();
          int degree = user.getNodeID();
          --degree;
          if (degree == 0) {
            int userIdx = allNodes.indexOf(user);
            Util.assertion(userIdx != -1, "Illegal status!");
            Util.assertion(insertPos <= userIdx, "Unordered list!");
            if (userIdx != insertPos) {
              allNodes.remove(userIdx);
              add(insertPos, user);
            }
            ++insertPos;
            user.setNodeID(dagSize++);
          } else {
            user.setNodeID(degree);
          }
        }
      }
    }
    SDNode firstNode = allNodes.get(0);
    SDNode lastNode = allNodes.get(allNodes.size() - 1);
    Util.assertion(insertPos == allNodes.size(), "Topological incomplete!");
    Util.assertion(firstNode.getOpcode() == ISD.EntryToken, "First node in allNode is not a entry token!");

    Util.assertion(firstNode.getNodeID() == 0, "First node in allNode does't have zero id!");

    Util.assertion(lastNode.getNodeID() == allNodes.size() - 1, "Last node in topological doesn't have " + (allNodes.size() - 1)
        + " id!");

    Util.assertion(lastNode.isUseEmpty(), "Last node in topological shouldn't have use");
    Util.assertion(dagSize == allNodes.size(), "Node count mismatch!");
    return dagSize;
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt) {
    SDVTList vts = getVTList(vt);
    return selectNodeTo(n, targetOpc, vts, null, 0);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue op1) {
    SDVTList vts = getVTList(vt);
    SDValue[] ops = {op1};
    return selectNodeTo(n, targetOpc, vts, ops, 1);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue op1,
                             SDValue op2) {
    SDVTList vts = getVTList(vt);
    SDValue[] ops = {op1, op2};
    return selectNodeTo(n, targetOpc, vts, ops, 2);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue op1,
                             SDValue op2, SDValue op3) {
    SDVTList vts = getVTList(vt);
    SDValue[] ops = {op1, op2, op3};
    return selectNodeTo(n, targetOpc, vts, ops, 3);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, ArrayList<SDValue> ops) {
    SDValue[] temps = new SDValue[ops.size()];
    ops.toArray(temps);
    return selectNodeTo(n, targetOpc, vt, temps, temps.length);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue[] ops) {
    return selectNodeTo(n, targetOpc, vt, ops, ops.length);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue[] ops,
                             int numOps) {
    SDVTList vts = getVTList(vt);
    return selectNodeTo(n, targetOpc, vts, ops, numOps);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2) {
    SDVTList vts = getVTList(vt1, vt2);
    return selectNodeTo(n, targetOpc, vts, null, 0);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
                             ArrayList<SDValue> ops) {
    SDValue[] temps = new SDValue[ops.size()];
    ops.toArray(temps);
    return selectNodeTo(n, targetOpc, vt1, vt2, temps, temps.length);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
                             SDValue[] ops) {
    return selectNodeTo(n, targetOpc, vt1, vt2, ops, ops.length);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
                             SDValue[] ops, int numOps) {
    SDVTList vts = getVTList(vt1, vt2);
    return selectNodeTo(n, targetOpc, vts, ops, numOps);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
                             EVT vt3, SDValue[] ops, int numOps) {
    SDVTList vts = getVTList(vt1, vt2, vt3);
    return selectNodeTo(n, targetOpc, vts, ops, numOps);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2, EVT vt3, EVT vt4, SDValue[] ops, int numOps) {
    SDVTList vts = getVTList(vt1, vt2, vt3, vt4);
    return selectNodeTo(n, targetOpc, vts, ops, numOps);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
                             SDValue op1) {
    SDVTList vts = getVTList(vt1, vt2);
    SDValue[] ops = {op1};
    return selectNodeTo(n, targetOpc, vts, ops, 1);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
                             SDValue op1, SDValue op2) {
    SDVTList vts = getVTList(vt1, vt2);
    SDValue[] ops = {op1, op2};
    return selectNodeTo(n, targetOpc, vts, ops, 2);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
                             SDValue op1, SDValue op2, SDValue op3) {
    SDVTList vts = getVTList(vt1, vt2);
    SDValue[] ops = {op1, op2, op3};
    return selectNodeTo(n, targetOpc, vts, ops, 3);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
                             EVT vt3, SDValue op1, SDValue op2, SDValue op3) {
    SDVTList vts = getVTList(vt1, vt2, vt3);
    SDValue[] ops = {op1, op2, op3};
    return selectNodeTo(n, targetOpc, vts, ops, 3);
  }

  public SDNode selectNodeTo(SDNode n, int targetOpc, SDVTList vts,
                             SDValue[] ops, int numOps) {
    return morphNodeTo(n, ~targetOpc, vts, ops, numOps);
  }

  public SDNode morphNodeTo(SDNode n,
                            int opc,
                            SDVTList vts,
                            ArrayList<SDValue> ops) {
    SDValue[] tmp = new SDValue[ops.size()];
    ops.toArray(tmp);
    return morphNodeTo(n, opc, vts, tmp, tmp.length);
  }

  public SDNode morphNodeTo(SDNode n, int opc, SDVTList vts, SDValue[] ops,
                            int numOps) {
    int id = 0;
    if (vts.vts[vts.numVTs - 1].getSimpleVT().simpleVT == MVT.Flag) {
      FoldingSetNodeID compute = new FoldingSetNodeID();
      addNodeToIDNode(compute, opc, vts, ops, numOps);
      id = compute.computeHash();
      if (cseMap.containsKey(id))
        return cseMap.get(id);
    }

    if (!removeNodeFromCSEMaps(n)) {
      id = 0;
    }

    n.setOpcode(opc);
    n.valueList = vts.vts;

    HashSet<SDNode> deadNodeSet = new HashSet<>();
    if (n.operandList != null && n.operandList.length > 0) {
      for (SDUse use : n.operandList) {
        SDNode used = use.getNode();
        use.set(new SDValue());
        if (used.isUseEmpty())
          deadNodeSet.add(used);
      }
    }
    n.operandList = new SDUse[numOps];
    for (int i = 0; i < numOps; i++)
      n.operandList[i] = new SDUse();

    for (int i = 0; i < numOps; i++) {
      n.operandList[i].setUser(n);
      n.operandList[i].setInitial(ops[i]);
    }

    ArrayList<SDNode> deadNodes = new ArrayList<>();
    for (SDNode node : deadNodeSet) {
      if (node.isUseEmpty())
        deadNodes.add(node);
    }

    collectDeadNodes(deadNodes);
    if (id != 0)
      cseMap.put(id, n);
    return n;
  }

  public SDValue getMemOperand(MachineMemOperand memOperand) {
    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, ISD.MEMOPERAND, getVTList(new EVT(MVT.Other)),
        null, 0);
    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);

    SDNode n = new MemOperandSDNode(memOperand);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDNode getTargetNode(int opc, EVT vt) {
    return getNode(~opc, vt).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt, SDValue op1) {
    return getNode(~opc, vt, op1).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt, SDValue op1, SDValue op2) {
    return getNode(~opc, vt, op1, op2).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt, SDValue op1, SDValue op2,
                              SDValue op3) {
    return getNode(~opc, vt, op1, op2, op3).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt, ArrayList<SDValue> ops) {
    SDValue[] temp = new SDValue[ops.size()];
    ops.toArray(temp);
    return getTargetNode(opc, vt, temp);
  }

  public SDNode getTargetNode(int opc, EVT vt, SDValue[] ops) {
    return getNode(~opc, vt, ops).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2) {
    return getNode(~opc, getVTList(vt1, vt2)).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue op1) {
    return getNode(~opc, getVTList(vt1, vt2), op1).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue op1, SDValue op2) {
    return getNode(~opc, getVTList(vt1, vt2), op1, op2).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue op1, SDValue op2, SDValue op3) {
    return getNode(~opc, getVTList(vt1, vt2), op1, op2, op3).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue[] ops) {
    return getNode(~opc, getVTList(vt1, vt2), ops).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue[] ops, int len) {
    Util.assertion(len >= 0 && len <= ops.length);
    if (ops.length == len) {
      SDValue[] temp = new SDValue[len];
      System.arraycopy(ops, 0, temp, 0, len);
      ops = temp;
    }

    return getNode(~opc, getVTList(vt1, vt2), ops).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, EVT vt3, SDValue op1,
                              SDValue op2) {
    return getNode(~opc, getVTList(vt1, vt2, vt3), op1, op2).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, EVT vt3, SDValue op1,
                              SDValue op2, SDValue op3) {
    return getNode(~opc, getVTList(vt1, vt2, vt3), op1, op2, op3).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, EVT vt3, SDValue[] ops) {
    return getNode(~opc, getVTList(vt1, vt2, vt3), ops).getNode();
  }

  public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, EVT vt3, EVT vt4,
                              SDValue[] ops) {
    return getNode(~opc, getVTList(vt1, vt2, vt3, vt4), ops).getNode();
  }

  public SDNode getTargetNode(int opc, ArrayList<EVT> resultTys, ArrayList<SDValue> ops) {
    EVT[] vts = new EVT[resultTys.size()];
    resultTys.toArray(vts);
    SDValue[] temp = new SDValue[ops.size()];
    ops.toArray(temp);
    return getNode(~opc, getVTList(vts), temp).getNode();
  }

  public SDNode getTargetNode(int opc, ArrayList<EVT> resultTys, SDValue[] ops) {
    EVT[] vts = new EVT[resultTys.size()];
    resultTys.toArray(vts);
    return getNode(~opc, getVTList(vts), ops).getNode();
  }

  public SDNode getTargetNode(int opc, EVT[] resultTys, SDValue[] ops) {
    return getNode(~opc, getVTList(resultTys), ops).getNode();
  }

  public SDValue getTargetFrameIndex(int fi, EVT vt) {
    return getFrameIndex(fi, vt, true);
  }

  public SDValue getGlobalAddress(GlobalValue gv, EVT vt, long offset,
                                  boolean isTargetGA, int targetFlags) {
    Util.assertion(targetFlags == 0 || isTargetGA, "Can't set target flags on target-independent globals!");

    EVT ptr = new EVT(tli.getPointerTy());
    int bitwidth = ptr.getSizeInBits();
    if (bitwidth < 64)
      offset = (offset << (64 - bitwidth)) >> (64 - bitwidth);
    GlobalVariable gvar = gv instanceof GlobalVariable ?
        (GlobalVariable) gv :
        null;
    int opc;
    if (gvar != null && gvar.isThreadLocal())
      opc = isTargetGA ? ISD.TargetGlobalTLSAddress : ISD.GlobalTLSAddress;
    else
      opc = isTargetGA ? ISD.TargetGlobalAddress : ISD.GlobalAddress;

    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, opc, getVTList(vt), null, 0);
    compute.addInteger(gv.hashCode());
    compute.addInteger(offset);
    compute.addInteger(targetFlags);

    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode n = new GlobalAddressSDNode(opc, vt, gv, offset, targetFlags);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getTargetGlobalAddress(GlobalValue gv, EVT vt, long offset,
                                        int targetFlags) {
    return getGlobalAddress(gv, vt, offset, true, targetFlags);
  }

  public SDValue getFrameIndex(int fi, EVT vt, boolean isTarget) {
    int opc = isTarget ? ISD.TargetFrameIndex : ISD.FrameIndex;
    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, opc, getVTList(vt), null, 0);
    compute.addInteger(fi);

    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode n = new FrameIndexSDNode(fi, vt, isTarget);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getJumpTable(int jti, EVT vt, boolean isTarget, int targetFlags) {
    Util.assertion(targetFlags == 0 || isTarget, "Can't set target flags on target-independent jump table");

    int opc = isTarget ? ISD.TargetJumpTable : ISD.JumpTable;
    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, opc, getVTList(vt), null, 0);
    compute.addInteger(jti);
    compute.addInteger(targetFlags);

    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode n = new JumpTableSDNode(jti, vt, isTarget, 0);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getTargetJumpTable(int jti, EVT vt, int targetFlags) {
    return getJumpTable(jti, vt, true, targetFlags);
  }

  public SDValue getConstantPool(Constant c, EVT vt, int align, int offset,
                                 boolean isTarget, int targetFlags) {
    Util.assertion(targetFlags == 0 || isTarget, "Can't set target flags on target-independent constant pool");

    int opc = isTarget ? ISD.TargetConstantPool : ISD.ConstantPool;
    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, opc, getVTList(vt), null, 0);
    compute.addInteger(c.hashCode());
    compute.addInteger(align);
    compute.addInteger(offset);
    compute.addInteger(targetFlags);

    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode n = new ConstantPoolSDNode(isTarget, c, vt, offset, align,
        targetFlags);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getTargetConstantPool(Constant c, EVT vt, int align, int offset,
                                       int targetFlags) {
    return getConstantPool(c, vt, align, offset, true, targetFlags);
  }

  public SDValue getBasicBlock(MachineBasicBlock mbb) {
    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, ISD.BasicBlock, getVTList(new EVT(MVT.Other)),
        null, 0);
    compute.addInteger(mbb.hashCode());

    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode n = new BasicBlockSDNode(mbb);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getValueType(EVT vt) {
    if (vt.isSimple() && vt.getSimpleVT().simpleVT >= valueTypeNodes.size()) {
      for (int i = valueTypeNodes.size(); i < vt.getSimpleVT().simpleVT + 1; i++)
        valueTypeNodes.add(null);
    }

    SDNode n = vt.isExtended() ? extendedValueTypeNodes.get(vt) :
        valueTypeNodes.get(vt.getSimpleVT().simpleVT);
    if (n != null)
      return new SDValue(n, 0);
    n = new VTSDNode(vt);
    if (vt.isExtended())
      extendedValueTypeNodes.put(vt, n);
    else
      valueTypeNodes.set(vt.getSimpleVT().simpleVT, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getExternalSymbol(String sym, EVT vt) {
    if (externalSymbols.containsKey(sym))
      return new SDValue(externalSymbols.get(sym), 0);
    SDNode n = new ExternalSymbolSDNode(false, vt, sym, 0);
    externalSymbols.put(sym, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getTargetExternalSymbol(String sym, EVT vt, int targetFlags) {
    Pair<String, Integer> key = Pair.get(sym, targetFlags);
    if (targetExternalSymbols.containsKey(key))
      return new SDValue(targetExternalSymbols.get(key), 0);
    SDNode n = new ExternalSymbolSDNode(true, vt, sym, targetFlags);
    targetExternalSymbols.put(key, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getLabel(int opc, SDValue root, int labelID) {
    FoldingSetNodeID compute = new FoldingSetNodeID();
    SDValue[] ops = {root};
    addNodeToIDNode(compute, ISD.BasicBlock, getVTList(new EVT(MVT.Other)),
        ops);
    compute.addInteger(labelID);

    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode n = new LabelSDNode(opc, root, labelID);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getSrcValue(Value val) {
    Util.assertion(val == null || val.getType() instanceof backend.type.PointerType, "SrcValue is not a pointer!");

    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, ISD.SRCVALUE, getVTList(new EVT(MVT.Other)),
        null, 0);
    compute.addInteger(val == null ? 0 : val.hashCode());

    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode n = new SrcValueSDNode(val);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getCopyFromReg(SDValue chain, int reg, EVT vt) {
    SDVTList vts = getVTList(vt, new EVT(MVT.Other));
    SDValue[] ops = {chain, getRegister(reg, vt)};
    return getNode(ISD.CopyFromReg, vts, ops);
  }

  public SDValue getCopyFromReg(SDValue chain, int reg, EVT vt, SDValue flag) {
    SDVTList vts = getVTList(vt, new EVT(MVT.Other));
    if (flag.getNode() != null) {
      SDValue[] ops = {chain, getRegister(reg, vt), flag};
      return getNode(ISD.CopyFromReg, vts, ops);
    } else {
      SDValue[] ops = {chain, getRegister(reg, vt)};
      return getNode(ISD.CopyFromReg, vts, ops);
    }
  }

  public SDValue getCopyToReg(SDValue chain, SDValue reg, SDValue node,
                              SDValue flag) {
    SDVTList vts = getVTList(new EVT(MVT.Other), new EVT(MVT.Flag));

    if (flag.getNode() != null) {
      SDValue[] ops = {chain, reg, node, flag};
      return getNode(ISD.CopyToReg, vts, ops);
    } else {
      SDValue[] ops = {chain, reg, node};
      return getNode(ISD.CopyToReg, vts, ops);
    }
  }

  public SDValue getCopyToReg(SDValue chain, int reg, SDValue node) {
    return getNode(ISD.CopyToReg, new EVT(MVT.Other), chain,
        getRegister(reg, node.getValueType()), node);
  }

  public SDValue getCopyToReg(SDValue chain, int reg, SDValue node, SDValue flag) {
    return getCopyToReg(chain, getRegister(reg, node.getValueType()), node,
        flag);
  }

  public SDValue createStackTemporary(EVT vt) {
    return createStackTemporary(vt, 1);
  }

  public SDValue createStackTemporary(EVT vt, int minAlign) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    int byteSize = vt.getStoreSizeInBits() / 8;
    Type ty = vt.getTypeForEVT();
    int stackAlign = Math
        .max(tli.getTargetData().getPrefTypeAlignment(ty), minAlign);
    int fi = mfi.createStackObject(byteSize, stackAlign);
    return getFrameIndex(fi, new EVT(tli.getPointerTy()), false);
  }

  public SDValue createStackTemporary(EVT vt1, EVT vt2) {
    int bytes = Math.max(vt1.getStoreSizeInBits(), vt2.getStoreSizeInBits())
        / 8;
    Type t1 = vt1.getTypeForEVT();
    Type t2 = vt2.getTypeForEVT();
    TargetData td = getTarget().getTargetData();
    int align = Math.max(td.getPrefTypeAlignment(t1), td.getPrefTypeAlignment(t2));
    MachineFrameInfo frameInfo = getMachineFunction().getFrameInfo();
    int frameInde = frameInfo.createStackObject(bytes, align);
    return getFrameIndex(frameInde, new EVT(tli.getPointerTy()), false);
  }

  public SDValue getTruncStore(SDValue chain, SDValue val, SDValue ptr, Value sv,
                               int svOffset, EVT svt) {
    return getTruncStore(chain, val, ptr, sv, svOffset, svt, false, 0);
  }

  public SDValue getTruncStore(SDValue chain, SDValue val, SDValue ptr, Value sv,
                               int svOffset, EVT svt,
                               boolean isVolatile, int alignment) {
    EVT vt = val.getValueType();
    if (vt.equals(svt))
      return getStore(chain, val, ptr, sv, svOffset, isVolatile, alignment);

    Util.assertion(vt.bitsGT(svt));
    Util.assertion(vt.isInteger() == svt.isInteger());
    if (alignment == 0)
      alignment = getEVTAlignment(vt);

    SDVTList vts = getVTList(new EVT(MVT.Other));
    SDValue undef = getUNDEF(ptr.getValueType());
    SDValue[] ops = {chain, val, ptr, undef};
    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, ISD.STORE, vts, ops);
    compute.addInteger(svt.getRawBits().hashCode());
    compute.addInteger(
        encodeMemSDNodeFlags(1, UNINDEXED, isVolatile, alignment));
    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);

    int flags = MOStore;
    if (isVolatile)
      flags |= MOVolatile;
    MachineMemOperand mmo = new MachineMemOperand(sv, flags, svOffset, svt.getStoreSize(), alignment);
    SDNode n = new StoreSDNode(ops, vts, UNINDEXED, true, svt, mmo);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue getStore(SDValue chain, SDValue val, SDValue ptr, Value sv,
                          int svOffset, boolean isVolatile, int alignment) {
    EVT vt = val.getValueType();
    if (alignment == 0)
      alignment = getEVTAlignment(vt);

    SDVTList vts = getVTList(new EVT(MVT.Other));
    SDValue undef = getUNDEF(ptr.getValueType());
    SDValue[] ops = {chain, val, ptr, undef};
    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, ISD.STORE, vts, ops);
    compute.addInteger(vt.getRawBits().hashCode());
    compute.addInteger(
        encodeMemSDNodeFlags(0, UNINDEXED, isVolatile, alignment));
    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);

    int flags = MOStore;
    if (isVolatile)
      flags |= MOVolatile;
    MachineMemOperand mmo = new MachineMemOperand(sv, flags, svOffset, vt.getStoreSize(), alignment);
    SDNode n = new StoreSDNode(ops, vts, UNINDEXED, false, vt, mmo);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  static int encodeMemSDNodeFlags(int convType, MemIndexedMode am,
                                  boolean isVolatile, int alignment) {
    return convType | (am.ordinal() << 2) | ((isVolatile ? 1 : 0) << 5) | (
        (Util.log2(alignment) + 1) << 6);
  }

  private int getEVTAlignment(EVT vt) {
    Type ty = vt.equals(new EVT(MVT.iPTR)) ?
        PointerType.get(LLVMContext.Int8Ty, 0) :
        vt.getTypeForEVT();
    return tli.getTargetData().getABITypeAlignment(ty);
  }

  public SDValue getExtLoad(LoadExtType extType, EVT vt, SDValue chain,
                            SDValue ptr, Value sv, int svOffset, EVT evt) {
    return getExtLoad(extType, vt, chain, ptr, sv, svOffset, evt, false, 0);
  }

  public SDValue getExtLoad(LoadExtType extType, EVT vt, SDValue chain,
                            SDValue ptr, Value sv, int svOffset, EVT evt,
                            boolean isVolatile,
                            int alignment) {
    SDValue undef = getUNDEF(ptr.getValueType());
    return getLoad(UNINDEXED, extType, vt, chain, ptr, undef, sv, svOffset,
        evt, isVolatile, alignment);
  }

  public SDValue getLoad(MemIndexedMode am,
                         LoadExtType extType, EVT vt,
                         SDValue chain, SDValue ptr,
                         SDValue offset, Value sv,
                         int svOffset, EVT evt,
                         boolean isVolatile, int alignment) {
    if (alignment == 0)
      alignment = getEVTAlignment(vt);

    if (sv == null) {
      if (ptr.getNode() instanceof FrameIndexSDNode) {
        FrameIndexSDNode fiNode = (FrameIndexSDNode) ptr.getNode();
        sv = PseudoSourceValue.getFixedStack(fiNode.getFrameIndex());
      }
    }
    MachineFunction mf = getMachineFunction();
    int flags = MOLoad;
    if (isVolatile)
      flags |= MOVolatile;
    MachineMemOperand mmo = new MachineMemOperand(sv, flags, svOffset,
        evt.getStoreSizeInBits()*8, alignment);
    return getLoad(am, extType, vt, chain, ptr, offset, evt, mmo);
  }

  public SDValue getLoad(MemIndexedMode am,
                         LoadExtType extType,
                         EVT vt,
                         SDValue chain,
                         SDValue ptr,
                         SDValue offset,
                         EVT memVT,
                         MachineMemOperand mmo) {
    if (vt.equals(memVT))
      extType = LoadExtType.NON_EXTLOAD;
    else if (extType == LoadExtType.NON_EXTLOAD)
      Util.assertion(vt.equals(memVT), "non-extending load from different memory type!");
    else {
      Util.assertion(memVT.getScalarType().bitsLT(vt.getScalarType()),
          "Should only be an extending load, not truncating");
      Util.assertion(vt.isInteger() == memVT.isInteger(), "Can't convert from FP to Int or Int -> FP!");
      Util.assertion(vt.isVector() == memVT.isVector(), "Can't use trunc store to convert to or from a vectgor!");
      Util.assertion(!vt.isVector() || vt.getVectorNumElements() == memVT.getVectorNumElements(),
          "Can't use trunc store to change the number of vector elements!");
    }

    boolean indexed = am != UNINDEXED;
    Util.assertion(indexed || offset.getOpcode() == ISD.UNDEF, "Unindexed load with an offset");
    SDVTList vts = indexed ? getVTList(vt, ptr.getValueType(), new EVT(MVT.Other)) : getVTList(vt, new EVT(MVT.Other));
    SDValue[] ops = {chain, ptr, offset};
    FoldingSetNodeID compute = new FoldingSetNodeID();
    addNodeToIDNode(compute, ISD.LOAD, vts, ops, 3);
    compute.addInteger(memVT.getRawBits().hashCode());
    compute.addInteger(encodeMemSDNodeFlags(extType.ordinal(), am, mmo.isVolatile(), mmo.getAlignment()));

    int id = compute.computeHash();
    if (cseMap.containsKey(id))
      return new SDValue(cseMap.get(id), 0);
    SDNode n = new LoadSDNode(ops, vts, am, extType, memVT, mmo);
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }


  public SDValue getLoad(EVT vt, SDValue chain, SDValue ptr, Value sv, int svOffset) {
    return getLoad(vt, chain, ptr, sv, svOffset, false, 0);
  }

  public SDValue getLoad(EVT vt, SDValue chain, SDValue ptr, Value sv, int svOffset,
                         boolean isVolatile, int alignment) {
    SDValue undef = getUNDEF(ptr.getValueType());
    return getLoad(UNINDEXED, LoadExtType.NON_EXTLOAD, vt, chain, ptr, undef,
        sv, svOffset, vt, isVolatile, alignment);
  }

  public SDValue getIndexedLoad(SDValue origLoad, SDValue base, SDValue offset,
                                MemIndexedMode am) {
    LoadSDNode ld = (LoadSDNode) origLoad.getNode();
    Util.assertion(ld.getOffset().getOpcode() == ISD.UNDEF);
    return getLoad(am, ld.getExtensionType(), origLoad.getValueType(),
        ld.getChain(), base, offset, ld.getSrcValue(),
        ld.getRawSubclassData(), ld.getMemoryVT(),
        ld.isVolatile(), ld.getAlignment());
  }

  public SDValue getIndexedStore(SDValue origStore, SDValue base,
                                 SDValue offset, MemIndexedMode am) {
    StoreSDNode st = (StoreSDNode) origStore.getNode();
    Util.assertion(st.getOffset().getOpcode() == ISD.UNDEF);
    SDVTList vts = getVTList(base.getValueType(), new EVT(MVT.Other));
    SDValue[] ops = {st.getChain(), st.getValue(), base, offset};
    FoldingSetNodeID calc = new FoldingSetNodeID();
    addNodeToIDNode(calc, ISD.STORE, vts, ops);
    int id = calc.computeHash();
    if (cseMap.containsValue(id))
      return new SDValue(cseMap.get(id), 0);

    SDNode n = new StoreSDNode(ops, vts, am, st.isTruncatingStore(),
        st.getMemoryVT(), st.getMemOperand());
    cseMap.put(id, n);
    add(n);
    return new SDValue(n, 0);
  }

  public SDValue updateNodeOperands(SDValue node, ArrayList<SDValue> ops) {
    SDValue[] temp = new SDValue[ops.size()];
    ops.toArray(temp);
    return updateNodeOperands(node, temp);
  }

  public SDValue updateNodeOperands(SDValue node, SDValue... ops) {
    SDNode n = node.getNode();
    Util.assertion(n.getNumOperands() == ops.length);
    boolean anyChange = false;
    for (int i = 0; i < ops.length; i++) {
      if (!ops[i].equals(n.getOperand(i))) {
        anyChange = true;
        break;
      }
    }

    if (!anyChange)
      return node;
    FoldingSetNodeID compute = new FoldingSetNodeID();
    SDNode existing = findModifiedNodeSlot(compute, n, ops);
    if (existing != null)
      return new SDValue(existing, node.getResNo());

    for (int i = 0; i < ops.length; i++) {
      if (!n.operandList[i].val.equals(ops[i]))
        n.operandList[i].set(ops[i]);
    }
    cseMap.put(compute.computeHash(), n);
    return node;
  }

  private SDNode findModifiedNodeSlot(FoldingSetNodeID compute, SDNode n,
                                      SDValue[] ops) {
    if (doNotCSE(n))
      return null;

    addNodeToIDNode(compute, n.getOpcode(), n.getValueList(), ops);
    addCustomToIDNode(compute, n);
    int id = compute.computeHash();
    return cseMap.get(id);
  }

  public SDValue getCALLSEQ_START(SDValue chain, SDValue op) {
    SDVTList vts = getVTList(new EVT(MVT.Other), new EVT(MVT.Flag));
    SDValue[] ops = {chain, op};
    return getNode(ISD.CALLSEQ_START, vts, ops);
  }

  public SDValue getCALLSEQ_END(SDValue chain, SDValue op1, SDValue op2,
                                SDValue inFlag) {
    SDVTList vts = getVTList(new EVT(MVT.Other), new EVT(MVT.Flag));
    ArrayList<SDValue> ops = new ArrayList<>();
    ops.add(chain);
    ops.add(op1);
    ops.add(op2);
    if (inFlag.getNode() != null)
      ops.add(inFlag);
    return getNode(ISD.CALLSEQ_END, vts, ops);
  }

  public SDValue getMemcpy(SDValue chain, SDValue dst,
                           SDValue src, SDValue size,
                           int align, boolean alwaysInline,
                           Value dstVal, long dstOff,
                           Value srcVal, long srcOff) {
    ConstantSDNode constantSize = size.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) size.getNode() : null;
    if (constantSize != null) {
      if (constantSize.isNullValue())
        return chain;

      SDValue result = getMemcpyLoadsAndStores(chain, dst, src,
          constantSize.getZExtValue(), align, false, dstVal,
          dstOff, srcVal, srcOff);
      if (result.getNode() != null)
        return result;
    }

    SDValue result = tli.emitTargetCodeForMemcpy(this, chain, dst, src,
        size, align, alwaysInline, dstVal, dstOff, srcVal, srcOff);
    if (result.getNode() != null)
      return result;

    if (alwaysInline) {
      Util.assertion(constantSize != null);
      return getMemcpyLoadsAndStores(chain, dst, src,
          constantSize.getZExtValue(), align, true, dstVal,
          dstOff, srcVal, srcOff);
    }
    // emit call to library function.
    ArrayList<ArgListEntry> args = new ArrayList<>();

    ArgListEntry entry = new ArgListEntry();
    entry.ty = tli.getTargetData().getIntPtrType();
    entry.node = dst;
    args.add(entry);

    entry = new ArgListEntry();
    entry.ty = tli.getTargetData().getIntPtrType();
    entry.node = src;
    args.add(entry);

    entry = new ArgListEntry();
    entry.ty = tli.getTargetData().getIntPtrType();
    entry.node = size;
    args.add(entry);

    Pair<SDValue, SDValue> callResult = tli.lowerCallTo(chain,
        LLVMContext.VoidTy,
        false, false, false, false, 0,
        tli.getLibCallCallingConv(RTLIB.MEMCPY), false,
        false, getExternalSymbol(tli.getLibCallName(RTLIB.MEMCPY),
            new EVT(tli.getPointerTy())), args, this);
    return callResult.second;
  }

  private SDValue getMemcpyLoadsAndStores(SDValue chain, SDValue dst,
                                          SDValue src, long size,
                                          int align, boolean alwaysInline,
                                          Value dstVal, long dstValOff,
                                          Value srcVal, long srcValOff) {
    ArrayList<EVT> memOps = new ArrayList<>();
    int limit = -1;
    if (!alwaysInline)
      limit = tli.getMaxStoresPerMemcpy();
    int destAlign = align;
    OutRef<String> str = new OutRef<>("");
    OutRef<Boolean> copyFromStr = new OutRef<>(false);
    if (!meetsMaxMemopRequirement(memOps, dst, src, limit, size, destAlign,
        str, copyFromStr)) {
      return new SDValue();
    }
    boolean isZeroStr = copyFromStr.get() && str.get().isEmpty();
    ArrayList<SDValue> outChains = new ArrayList<>();
    int numMemOps = memOps.size();
    int srcOffset = 0, destOffset = 0;
    for (int i = 0; i < numMemOps; i++) {
      EVT vt = memOps.get(i);
      int vtSize = vt.getSizeInBits();
      SDValue value, store;
      if (copyFromStr.get() && (isZeroStr || !vt.isVector())) {
        value = getMemsetStringVal(vt, str.get(), srcOffset);
        store = getStore(chain, value,
            getMemBasePlusOffset(dst, destOffset),
            dstVal, (int) (dstValOff + dstValOff),
            false, destAlign);
      } else {
        EVT nvt = tli.getTypeToTransformTo(vt);
        Util.assertion(nvt.bitsGE(vt));
        value = getExtLoad(LoadExtType.EXTLOAD, nvt, chain,
            getMemBasePlusOffset(src, srcOffset),
            srcVal, (int) (srcValOff + srcOffset), vt, false,
            align);
        store = getTruncStore(chain, value,
            getMemBasePlusOffset(dst, (int) dstValOff),
            dstVal, (int) (dstValOff + destOffset), vt, false, destAlign);
      }
      outChains.add(store);
      srcOffset += vtSize;
      destOffset += vtSize;
    }
    return getNode(ISD.TokenFactor, new EVT(MVT.Other),
        outChains);
  }

  private SDValue getMemsetStringVal(EVT vt,
                                     String str,
                                     int offset) {
    // Handle vector with all elements zero.
    if (str.isEmpty()) {
      if (vt.isInteger())
        return getConstant(0, vt, false);
      int numElts = vt.getVectorNumElements();
      EVT eltVT = vt.getVectorElementType().equals(new EVT(MVT.f32))
          ? new EVT(MVT.i32) : new EVT(MVT.i64);
      return getNode(ISD.BIT_CONVERT, vt,
          getConstant(0, EVT.getVectorVT(eltVT, numElts), false));
    }
    Util.assertion(!vt.isVector(), "Can't handle vector type here!");
    int numBits = vt.getSizeInBits();
    int msb = numBits / 8;
    long val = 0;
    if (tli.isLittleEndian())
      offset += msb - 1;
    for (int i = 0; i < msb; i++) {
      val = (val << 8) | (str.charAt(offset));
      offset += tli.isLittleEndian() ? -1 : 1;
    }
    return getConstant(val, vt, false);
  }

  private SDValue getMemBasePlusOffset(SDValue base, int offset) {
    EVT vt = base.getValueType();
    return getNode(ISD.ADD, vt, base, getConstant(offset, vt, false));
  }

  /**
   * Determines if the number of memory ops required to replace the memset/memcpy
   * is below the inlineThreshold. If also returns the types of the sequence of memory
   * ops to perform memset/memcpy.
   *
   * @param memOps
   * @param dst
   * @param src
   * @param limit
   * @param size
   * @param destAlign
   * @param str
   * @param isSrcStr
   * @return
   */
  private boolean meetsMaxMemopRequirement(ArrayList<EVT> memOps,
                                           SDValue dst,
                                           SDValue src,
                                           int limit,
                                           long size,
                                           int destAlign,
                                           OutRef<String> str,
                                           OutRef<Boolean> isSrcStr) {
    isSrcStr.set(isMemSrcFromString(src, str));
    boolean isSrcConst = src.getNode() instanceof ConstantSDNode;
    EVT vt = tli.getOptimalMemOpType(size, destAlign, isSrcConst, isSrcStr.get(), this);
    boolean allowUnalign = tli.allowsUnalignedMemoryAccesses(vt);
    if (!vt.equals(new EVT(MVT.iAny))) {
      Type ty = vt.getTypeForEVT();
      int newAlign = tli.getTargetData().getABITypeAlignment(ty);
      if (newAlign > destAlign && (isSrcConst || allowUnalign)) {
        if (dst.getOpcode() != ISD.FrameIndex) {
          if (allowUnalign)
            vt = new EVT(MVT.iAny);
        } else {
          int fi = ((FrameIndexSDNode) dst.getNode()).getFrameIndex();
          MachineFrameInfo mfi = getMachineFunction().getFrameInfo();
          if (mfi.isFixedObjectIndex(fi)) {
            if (allowUnalign)
              vt = new EVT(MVT.iAny);
          } else {
            if (mfi.getObjectAlignment(fi) < newAlign)
              mfi.setObjectOffset(fi, newAlign);
            destAlign = newAlign;
          }
        }
      }
    }
    if (vt.equals(new EVT(MVT.iAny))) {
      if (tli.allowsUnalignedMemoryAccesses(new EVT(MVT.i64)))
        vt = new EVT(MVT.i64);
      else {
        switch (destAlign & 7) {
          case 0:
            vt = new EVT(MVT.i16);
            break;
          case 2:
            vt = new EVT(MVT.i32);
            break;
          case 4:
            vt = new EVT(MVT.i64);
            break;
          default:
            vt = new EVT(MVT.i8);
            break;
        }
      }
      EVT lvt = new EVT(MVT.i64);
      while (!tli.isTypeLegal(lvt))
        lvt = new EVT(lvt.getSimpleVT().simpleVT + 1);
      Util.assertion(lvt.isInteger());
      if (vt.bitsGT(lvt))
        vt = lvt;
    }

    int numMemOps = 0;
    while (size != 0) {
      int vtSize = vt.getSizeInBits() / 8;
      while (vtSize > size) {
        if (vt.isVector()) {
          vt = new EVT(MVT.i64);
          while (!tli.isTypeLegal(vt))
            vt = new EVT(vt.getSimpleVT().simpleVT - 1);
          vtSize = vt.getSizeInBits() / 8;
        } else {
          vt = new EVT(vt.getSimpleVT().simpleVT - 1);
          vtSize >>= 1;
        }
      }

      if (++numMemOps > limit)
        return false;
      memOps.add(vt);
      size -= vtSize;
    }
    return true;
  }

  private boolean isMemSrcFromString(SDValue src, OutRef<String> str) {
    int srcDelta = 0;
    GlobalAddressSDNode gad = null;
    if (src.getOpcode() == ISD.GlobalAddress)
      gad = (GlobalAddressSDNode) src.getNode();
    else if (src.getOpcode() == ISD.ADD &&
        src.getOperand(0).getOpcode() == ISD.GlobalAddress &&
        src.getOperand(1).getOpcode() == ISD.Constant) {
      gad = (GlobalAddressSDNode) src.getOperand(0).getNode();
      srcDelta = (int) ((ConstantSDNode) src.getOperand(1).getNode()).getZExtValue();
    }
    if (gad == null)
      return false;
    GlobalVariable gv = gad.getGlobalValue() instanceof GlobalVariable ?
        (GlobalVariable) gad.getGlobalValue() : null;

    return gv != null && getConstantStringInfo(gv, str, srcDelta, false);
  }

  private boolean getConstantStringInfo(Value val,
                                        OutRef<String> str,
                                        long offset,
                                        boolean stopAtNul) {
    if (val == null) return false;

    if (val instanceof BitCastInst)
      return getConstantStringInfo(((BitCastInst) val).operand(0),
          str, offset, stopAtNul);
    User gep = null;
    if (val instanceof GetElementPtrInst)
      gep = (GetElementPtrInst) val;
    else if (val instanceof ConstantExpr) {
      ConstantExpr ce = (ConstantExpr) val;
      if (ce.getOpcode() == Operator.BitCast)
        return getConstantStringInfo(ce.operand(0), str, offset, stopAtNul);
      if (ce.getOpcode() != Operator.GetElementPtr)
        return false;
      gep = ce;
    }
    if (gep != null) {
      if (gep.getNumOfOperands() != 3)
        return false;

      // Make sure the index-ee is a pointer to array of i8.
      if (!(gep.operand(0).getType() instanceof PointerType))
        return false;
      PointerType pty = (PointerType) gep.operand(0).getType();
      if (!(pty.getElementType() instanceof ArrayType))
        return false;

      ArrayType at = (ArrayType) pty.getElementType();
      if (!at.getElementType().equals(LLVMContext.Int8Ty))
        return false;

      if (!(gep.operand(1) instanceof ConstantInt))
        return false;
      ConstantInt firstIdx = (ConstantInt) gep.operand(1);
      if (!firstIdx.isZero())
        return false;

      long startIdx = 0;
      if (gep.operand(2) instanceof ConstantInt)
        startIdx = ((ConstantInt) gep.operand(2)).getZExtValue();
      else
        return false;
      return getConstantStringInfo(gep.operand(0), str,
          startIdx + offset, stopAtNul);
    }

    if (!(val instanceof GlobalVariable))
      return false;
    GlobalVariable gv = (GlobalVariable) val;
    if (!gv.isConstant() || !gv.hasDefinitiveInitializer())
      return false;

    Constant init = gv.getInitializer();
    if (init instanceof ConstantAggregateZero) {
      str.set("");
      return true;
    }

    if (!(init instanceof ConstantArray))
      return false;
    ConstantArray ca = (ConstantArray) init;
    if (!ca.getType().getElementType().equals(LLVMContext.Int8Ty))
      return false;

    long numElts = ca.getType().getNumElements();
    if (offset > numElts)
      return false;

    StringBuilder sb = new StringBuilder();
    for (int i = (int) offset; i < numElts; i++) {
      Constant c = ca.operand(i);
      if (!(c instanceof ConstantInt))
        return false;
      ConstantInt ci = (ConstantInt) c;
      if (stopAtNul && ci.isZero())
        return true;
      sb.append((char) ci.getZExtValue());
    }
    str.set(sb.toString());
    return true;
  }

  public SDValue getStackArgumentTokenFactor(SDValue chain) {
    ArrayList<SDValue> argChains = new ArrayList<>();

    argChains.add(chain);
    for (SDUse u : getEntryNode().getNode().operandList) {
      if (u.getUser() instanceof LoadSDNode) {
        LoadSDNode ld = (LoadSDNode) u.getUser();
        if (ld.getBasePtr().getNode() instanceof FrameIndexSDNode) {
          FrameIndexSDNode fi = (FrameIndexSDNode) ld.getBasePtr()
              .getNode();
          if (fi.getFrameIndex() < 0)
            argChains.add(new SDValue(ld, 1));
        }
      }
    }
    return getNode(ISD.TokenFactor, new EVT(MVT.Other), argChains);
  }

  public boolean legalizeTypes() {
    return new DAGTypeLegalizer(this).run();
  }

  public void combine(CombineLevel level, AliasAnalysis aa, CodeGenOpt optLevel) {
    new DAGCombiner(this, aa, optLevel).run(level);
  }

  public boolean legalizeVectors() {
    return new VectorLegalizer(this).run();
  }

  public void legalize(boolean typesNeedLegalizing, CodeGenOpt optLevel) {
    new SelectionDAGLegalizer(this, optLevel).legalizeDAG();
  }

  public int computeNumSignBits(SDValue op) {
    return computeNumSignBits(op, 0);
  }

  public int computeNumSignBits(SDValue op, int depth) {
    Util.assertion(false, "Unimplemented currently!");
    return 0;
  }

  public SDValue getVectorShuffle(EVT vt, SDValue op0, SDValue op1, int[] mask) {
    Util.assertion(op0.getValueType().equals(op1.getValueType()), "Invalid VECTOR_SHUFFLE!");

    Util.assertion(vt.isVector() && op0.getValueType().isVector());
    Util.assertion(vt.getVectorElementType().equals(op0.getValueType().getVectorElementType()));

    if (op0.getOpcode() == ISD.UNDEF && op1.getOpcode() == ISD.UNDEF)
      return getUNDEF(vt);

    int numElts = vt.getVectorNumElements();
    TIntArrayList maskVec = new TIntArrayList();
    for (int val : mask)
      Util.assertion(val < numElts * 2, "Index out of range!");
    maskVec.addAll(mask);

    if (op0.equals(op1)) {
      op1 = getUNDEF(vt);
      for (int i = 0; i < numElts; i++)
        if (maskVec.get(i) >= numElts)
          maskVec.set(i, maskVec.get(i) - numElts);
    }
    Util.shouldNotReachHere("Not implemented!");
    return null;
  }

  public SDValue getShiftAmountOperand(SDValue op) {
    EVT ty = op.getValueType();
    EVT shTy = new EVT(tli.getShiftAmountTy());
    if (ty.equals(shTy) || ty.isVector())
      return op;

    int opc = ty.bitsGT(shTy) ? ISD.TRUNCATE : ISD.ZERO_EXTEND;
    return getNode(opc, shTy, op);
  }

  public SDValue getNOT(SDValue op, EVT vt) {
    EVT eltVT = vt.isVector() ? vt.getVectorElementType() : vt;
    SDValue negOne = getConstant(APInt.getAllOnesValue(eltVT.getSizeInBits()), vt, false);
    return getNode(ISD.XOR, vt, op, negOne);
  }

  public SDValue foldSetCC(EVT vt, SDValue lhs, SDValue rhs, CondCode cc) {
    switch (cc) {
      default:
        break;
      case SETFALSE:
      case SETFALSE2:
        return getConstant(0, vt, false);
      case SETTRUE:
      case SETTRUE2:
        return getConstant(1, vt, false);

      case SETOEQ:
      case SETOGT:
      case SETOGE:
      case SETOLT:
      case SETOLE:
      case SETONE:
      case SETO:
      case SETUO:
      case SETUEQ:
      case SETUNE:
        Util.assertion(!lhs.getValueType().isInteger(), "Illegal setcc for integer!");
        break;
    }
    ConstantSDNode rhsC = rhs.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) (rhs.getNode()) :
        null;
    if (rhsC != null) {
      APInt c2 = rhsC.getAPIntValue();
      ConstantSDNode lhsC = lhs.getNode() instanceof ConstantSDNode ?
          (ConstantSDNode) lhs.getNode() : null;
      if (lhsC != null) {
        APInt c1 = lhsC.getAPIntValue();
        switch (cc) {
          default:
            Util.shouldNotReachHere("Unknown integer setcc!");
          case SETEQ:
            return getConstant(c1.eq(c2) ? 1 : 0, vt, false);
          case SETNE:
            return getConstant(c1.ne(c2) ? 1 : 0, vt, false);
          case SETULT:
            return getConstant(c1.ult(c2) ? 1 : 0, vt, false);
          case SETUGT:
            return getConstant(c1.ugt(c2) ? 1 : 0, vt, false);
          case SETULE:
            return getConstant(c1.ule(c2) ? 1 : 0, vt, false);
          case SETUGE:
            return getConstant(c1.uge(c2) ? 1 : 0, vt, false);
          case SETLT:
            return getConstant(c1.slt(c2) ? 1 : 0, vt, false);
          case SETGT:
            return getConstant(c1.sgt(c2) ? 1 : 0, vt, false);
          case SETLE:
            return getConstant(c1.sle(c2) ? 1 : 0, vt, false);
          case SETGE:
            return getConstant(c1.sge(c2) ? 1 : 0, vt, false);
        }
      }
    }
    ConstantFPSDNode lhsFP = lhs.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) lhs.getNode() : null;
    ConstantFPSDNode rhsFP = rhs.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) rhs.getNode() : null;
    if (lhsFP != null) {
      if (rhsFP != null) {
        // No compile time operations on this type yet.
        if (lhsFP.getValueType(0).getSimpleVT().simpleVT == MVT.ppcf128)
          return new SDValue();

        APFloat.CmpResult r = lhsFP.getValueAPF().compare(rhsFP.getValueAPF());
        switch (cc) {
          default:
            break;
          case SETEQ:
            if (r == cmpUnordered)
              return getUNDEF(vt);
            // fall through
          case SETOEQ:
            return getConstant(r == cmpEqual, vt, false);
          case SETNE:
            if (r == cmpUnordered)
              return getUNDEF(vt);
            // fall through
          case SETONE:
            return getConstant(
                r == cmpGreaterThan || r == cmpLessThan, vt,
                false);
          case SETLT:
            if (r == cmpUnordered)
              return getUNDEF(vt);
            // fall through
          case SETOLT:
            return getConstant(r == cmpLessThan, vt, false);
          case SETGT:
            if (r == cmpUnordered)
              return getUNDEF(vt);
            // fall through
          case SETOGT:
            return getConstant(r == cmpGreaterThan, vt, false);
          case SETLE:
            if (r == cmpUnordered)
              return getUNDEF(vt);
            // fall through
          case SETOLE:
            return getConstant(r == cmpLessThan || r == cmpEqual, vt, false);
          case SETGE:
            if (r == cmpUnordered)
              return getUNDEF(vt);
            // fall through
          case SETOGE:
            return getConstant(r == cmpGreaterThan || r == cmpEqual,
                vt, false);
          case SETO:
            return getConstant(r != cmpUnordered, vt, false);
          case SETUO:
            return getConstant(r == cmpUnordered, vt, false);
          case SETUEQ:
            return getConstant(r == cmpUnordered || r == cmpEqual,
                vt, false);
          case SETUNE:
            return getConstant(r != cmpEqual, vt, false);
          case SETULT:
            return getConstant(r == cmpUnordered || r == cmpLessThan, vt,
                false);
          case SETUGT:
            return getConstant(r == cmpGreaterThan || r == cmpUnordered, vt,
                false);
          case SETULE:
            return getConstant(r != cmpGreaterThan, vt, false);
          case SETUGE:
            return getConstant(r != cmpLessThan, vt, false);
        }
      } else {
        return getSetCC(vt, rhs, lhs, getSetCCSwappedOperands(cc));
      }
    }
    return new SDValue();
  }

  private HashMap<SDNode, String> nodeGraphAttrs = new HashMap<>();

  public void clearGraphAttrs() {
    nodeGraphAttrs.clear();
  }

  public void setGraphAttrs(SDNode node, String attrs) {
    nodeGraphAttrs.put(node, attrs);
  }

  public String getGraphAttrs(SDNode node) {
    return nodeGraphAttrs.getOrDefault(node, "");
  }

  public void setGraphColor(SDNode node, String color) {
    nodeGraphAttrs.put(node, "color=" + color);
  }

  public void viewGraph() {
    viewGraph("");
  }

  public void viewGraph(String title) {
    String funcName = getMachineFunction().getFunction().getName();
    String filename = "dag." + funcName + ".dot";
    DefaultDotGraphTrait trait = DefaultDotGraphTrait.createSelectionDAGTrait(this, false);
    GraphWriter.viewGraph(title, filename, trait);
  }

  public void repositionNode(SDNode position, SDNode n) {
    Util.assertion(position != null);
    if (position.equals(n))
      return;
    int index = allNodes.indexOf(position);
    Util.assertion(index != -1);
    int nIdx = allNodes.indexOf(n);
    Util.assertion(nIdx != -1);
    allNodes.add(index, n);
    if (index < nIdx)
      ++nIdx;
    allNodes.remove(nIdx);
  }

  public SDNode getNodeIfExists(int opc, SDVTList vts, SDValue[] ops) {
    if (!vts.vts[vts.vts.length - 1].equals(new EVT(MVT.Flag))) {
      FoldingSetNodeID calc = new FoldingSetNodeID();
      addNodeToIDNode(calc, opc, vts, ops);
      int id = calc.computeHash();
      if (cseMap.containsValue(id))
        return cseMap.get(id);
    }
    return null;
  }

  public SDValue foldConstantArithmetic(
      int opc, EVT vt,
      ConstantSDNode cst1,
      ConstantSDNode cst2) {
    APInt c1 = cst1.getAPIntValue(), c2 = cst2.getAPIntValue();
    switch (opc) {
      case ISD.ADD:
        return getConstant(c1.add(c2), vt, false);
      case ISD.SUB:
        return getConstant(c1.sub(c2), vt, false);
      case ISD.MUL:
        return getConstant(c1.mul(c2), vt, false);
      case ISD.UDIV:
        if (c2.getBoolValue())
          return getConstant(c1.udiv(c2), vt, false);
        break;
      case ISD.SDIV:
        if (c2.getBoolValue())
          return getConstant(c1.sdiv(c2), vt, false);
        break;
      case ISD.UREM:
        if (c2.getBoolValue())
          return getConstant(c1.urem(c2), vt, false);
        break;
      case ISD.SREM:
        if (c2.getBoolValue())
          return getConstant(c1.srem(c2), vt, false);
        break;
      case ISD.AND:
        return getConstant(c1.and(c2), vt, false);
      case ISD.OR:
        return getConstant(c1.or(c2), vt, false);
      case ISD.XOR:
        return getConstant(c1.xor(c2), vt, false);
      case ISD.SHL:
        return getConstant(c1.shl(c2), vt, false);
      case ISD.SRL:
        return getConstant(c1.lshr(c2), vt, false);
      case ISD.SRA:
        return getConstant(c1.ashr(c2), vt, false);
      case ISD.ROTL:
        return getConstant(c1.rotl(c2), vt, false);
      case ISD.ROTR:
        return getConstant(c1.rotr(c2), vt, false);
    }
    return new SDValue();
  }
}

