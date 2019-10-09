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

import backend.analysis.aa.AliasAnalysis;
import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.MachineFrameInfo;
import backend.codegen.PseudoSourceValue;
import backend.codegen.dagisel.SDNode.*;
import backend.target.TargetData;
import backend.target.TargetLowering;
import backend.target.TargetLowering.TargetLoweringOpt;
import backend.target.TargetMachine.CodeGenOpt;
import backend.type.ArrayType;
import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantArray;
import gnu.trove.list.array.TIntArrayList;
import tools.APFloat;
import tools.APInt;
import tools.OutRef;
import tools.Util;

import java.util.*;

import static backend.codegen.dagisel.MemIndexedMode.*;
import static backend.codegen.dagisel.SelectionDAG.isCommutativeBinOp;
import static backend.support.BackendCmdOptions.EnableUnsafeFPMath;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class DAGCombiner {
  private SelectionDAG dag;
  private CombineLevel level;
  private CodeGenOpt optLevel;
  private boolean legalOprations;
  private boolean legalTypes;
  private LinkedList<SDNode> workList;
  private AliasAnalysis aa;
  private TargetLowering tli;

  public DAGCombiner(SelectionDAG dag, AliasAnalysis aa,
                     CodeGenOpt optLevel) {
    this.dag = dag;
    this.aa = aa;
    this.optLevel = optLevel;
    workList = new LinkedList<>();
    tli = dag.getTargetLoweringInfo();
  }

  public void run(CombineLevel level) {
    this.level = level;
    legalOprations = level.compareTo(CombineLevel.NoIllegalOperations) >= 0;
    legalTypes = level.compareTo(CombineLevel.NoIllegalTypes) >= 0;

    // assigns a topological order on DAG.
    dag.assignTopologicalOrder();
    for (SDNode n : dag.allNodes)
      workList.push(n);

    // create a dummy node that adds a reference to the root node, preventing
    // it from begin deleted, and tracking any change of root.
    SDNode.HandleSDNode dummy = new SDNode.HandleSDNode(dag.getRoot());

    dag.setRoot(new SDValue());
    while (!workList.isEmpty()) {
      SDNode n = workList.pop();
      if (n.isUseEmpty() && !n.equals(dummy)) {
        // if this node has no uses, so it is dead.
        for (int i = 0, e = n.getNumOperands(); i < e; i++)
          addToWorkList(n.getOperand(i).getNode());

        dag.deleteNode(n);
        continue;
      }
      SDValue rv = combine(n);
      if (rv.getNode() == null || rv.getNode().equals(n))
        continue;

      Util.assertion(n.getOpcode() != ISD.DELETED_NODE && rv.getNode().getOpcode() != ISD.DELETED_NODE,
          "Node was deleted but visit returned new node!");


      WorklistRemover remover = new WorklistRemover(this);
      if (n.getNumValues() == rv.getNode().getNumValues())
        dag.replaceAllUsesWith(n, rv.getNode(), remover);
      else {
        Util.assertion(n.getValueType(0).equals(rv.getValueType()) && n.getNumValues() == 1, "Type mismatch!");

        dag.replaceAllUsesWith(n, rv, remover);
      }

      addToWorkList(rv.getNode());
      addUsersToWorklist(rv.getNode());

      for (int i = 0, e = n.getNumOperands(); i < e; i++)
        addToWorkList(n.getOperand(i).getNode());

      if (n.isUseEmpty()) {
        removeFromWorkList(n);
        dag.deleteNode(n);
      }
    }
    dag.setRoot(dummy.getValue());
    dummy.dropOperands();
    dag.removeDeadNodes();
  }

  private SDValue combine(SDNode n) {
    SDValue rv = visit(n);
    if (rv.getNode() == null) {
      Util.assertion(n.getOpcode() != ISD.DELETED_NODE, "Node was deleted but visit returned null!");

      if (n.getOpcode() >= ISD.BUILTIN_OP_END ||
          tli.hasTargetDAGCombine(n.getOpcode())) {
        DAGCombinerInfo combineInfo = new DAGCombinerInfo(dag,
            !legalTypes, !legalOprations, false, this);
        rv = tli.performDAGCombine(n, combineInfo);
      }
    }

    if (rv.getNode() == null && isCommutativeBinOp(n.getOpcode()) &&
        n.getNumValues() == 1) {
      SDValue n0 = n.getOperand(0);
      SDValue n1 = n.getOperand(1);
      if (n0.getNode() instanceof ConstantSDNode ||
          !(n1.getNode() instanceof ConstantSDNode)) {
        SDValue[] ops = {n1, n0};
        SDNode cseNode = dag.getNodeIfExists(n.getOpcode(), n.getValueList(),
            ops);
        if (cseNode != null)
          return new SDValue(cseNode, 0);
      }
    }
    return rv;
  }

  private SDValue visit(SDNode n) {
    switch (n.getOpcode()) {
      default:
        break;
      case ISD.TokenFactor:
        return visitTokenFactor(n);
      case ISD.MERGE_VALUES:
        return visitMERGE_VALUES(n);
      case ISD.ADD:
        return visitADD(n);
      case ISD.SUB:
        return visitSUB(n);
      case ISD.ADDC:
        return visitADDC(n);
      case ISD.ADDE:
        return visitADDE(n);
      case ISD.MUL:
        return visitMUL(n);
      case ISD.SDIV:
        return visitSDIV(n);
      case ISD.UDIV:
        return visitUDIV(n);
      case ISD.SREM:
        return visitSREM(n);
      case ISD.UREM:
        return visitUREM(n);
      case ISD.MULHU:
        return visitMULHU(n);
      case ISD.MULHS:
        return visitMULHS(n);
      case ISD.SMUL_LOHI:
        return visitSMUL_LOHI(n);
      case ISD.UMUL_LOHI:
        return visitUMUL_LOHI(n);
      case ISD.SDIVREM:
        return visitSDIVREM(n);
      case ISD.UDIVREM:
        return visitUDIVREM(n);
      case ISD.AND:
        return visitAND(n);
      case ISD.OR:
        return visitOR(n);
      case ISD.XOR:
        return visitXOR(n);
      case ISD.SHL:
        return visitSHL(n);
      case ISD.SRA:
        return visitSRA(n);
      case ISD.SRL:
        return visitSRL(n);
      case ISD.CTLZ:
        return visitCTLZ(n);
      case ISD.CTTZ:
        return visitCTTZ(n);
      case ISD.CTPOP:
        return visitCTPOP(n);
      case ISD.SELECT:
        return visitSELECT(n);
      case ISD.SELECT_CC:
        return visitSELECT_CC(n);
      case ISD.SETCC:
        return visitSETCC(n);
      case ISD.SIGN_EXTEND:
        return visitSIGN_EXTEND(n);
      case ISD.ZERO_EXTEND:
        return visitZERO_EXTEND(n);
      case ISD.ANY_EXTEND:
        return visitANY_EXTEND(n);
      case ISD.SIGN_EXTEND_INREG:
        return visitSIGN_EXTEND_INREG(n);
      case ISD.TRUNCATE:
        return visitTRUNCATE(n);
      case ISD.BIT_CONVERT:
        return visitBIT_CONVERT(n);
      case ISD.BUILD_PAIR:
        return visitBUILD_PAIR(n);
      case ISD.FADD:
        return visitFADD(n);
      case ISD.FSUB:
        return visitFSUB(n);
      case ISD.FMUL:
        return visitFMUL(n);
      case ISD.FDIV:
        return visitFDIV(n);
      case ISD.FREM:
        return visitFREM(n);
      case ISD.FCOPYSIGN:
        return visitFCOPYSIGN(n);
      case ISD.SINT_TO_FP:
        return visitSINT_TO_FP(n);
      case ISD.UINT_TO_FP:
        return visitUINT_TO_FP(n);
      case ISD.FP_TO_SINT:
        return visitFP_TO_SINT(n);
      case ISD.FP_TO_UINT:
        return visitFP_TO_UINT(n);
      case ISD.FP_ROUND:
        return visitFP_ROUND(n);
      case ISD.FP_ROUND_INREG:
        return visitFP_ROUND_INREG(n);
      case ISD.FP_EXTEND:
        return visitFP_EXTEND(n);
      case ISD.FNEG:
        return visitFNEG(n);
      case ISD.FABS:
        return visitFABS(n);
      case ISD.BRCOND:
        return visitBRCOND(n);
      case ISD.BR_CC:
        return visitBR_CC(n);
      case ISD.LOAD:
        return visitLOAD(n);
      case ISD.STORE:
        return visitSTORE(n);
      case ISD.INSERT_VECTOR_ELT:
        return visitINSERT_VECTOR_ELT(n);
      case ISD.EXTRACT_VECTOR_ELT:
        return visitEXTRACT_VECTOR_ELT(n);
      case ISD.BUILD_VECTOR:
        return visitBUILD_VECTOR(n);
      case ISD.CONCAT_VECTORS:
        return visitCONCAT_VECTORS(n);
      case ISD.VECTOR_SHUFFLE:
        return visitVECTOR_SHUFFLE(n);
    }
    return new SDValue();
  }

  private SDValue visitVECTOR_SHUFFLE(SDNode n) {
    EVT vt = n.getValueType(0);
    int numElts = vt.getVectorNumElements();

    SDValue n0 = n.getOperand(0);
    Util.assertion(n0.getValueType().getVectorNumElements() == numElts,
        "Vector shuffle must be normalized in DAG");

    // If it is a splat, check if the argument vector is another splat or a
    // build_vector with all scalar elements the same.
    ShuffleVectorSDNode svn = (ShuffleVectorSDNode) n;
    if (svn.isSplat() && svn.getSplatIndex() < numElts) {
      SDNode v = n0.getNode();

      if (v.getOpcode() == ISD.BIT_CONVERT) {
        SDValue convInput = v.getOperand(0);
        if (convInput.getValueType().isVector() &&
            convInput.getValueType().getVectorNumElements() == numElts)
          v = convInput.getNode();
      }

      // If this is a bit convert that changes the element type of the vector but
      // not the number of vector elements, look through it.  Be careful not to
      // look though conversions that change things like v4f32 to v2f64.
      if (v.getOpcode() == ISD.BUILD_VECTOR) {
        Util.assertion(v.getNumOperands() == numElts,
            "BUILD_VECTOR has wrong number of operands");
        SDValue base = new SDValue();
        boolean allSame = true;
        for (int i = 0; i < numElts; ++i) {
          if (v.getOperand(i).getOpcode() != ISD.UNDEF) {
            base = v.getOperand(i);
            break;
          }
        }
        // Splat of <u, u, u, u>, return <u, u, u, u>
        if (base.getNode() == null)
          return n0;
        for (int i = 0; i < numElts; i++) {
          if (!v.getOperand(i).equals(base)) {
            allSame = false;
            break;
          }
        }

        // Splat of<x, x, x, x>, return <x, x, x, x>
        if (allSame)
          return n0;
      }
    }
    return new SDValue();
  }

  private SDValue visitCONCAT_VECTORS(SDNode n) {
    // If we only have one input vector, we don't need to do any concatenation.
    if (n.getNumOperands() == 1)
      return n.getOperand(0);

    return new SDValue();
  }

  private SDValue visitBUILD_VECTOR(SDNode n) {
    int numInScalars = n.getNumOperands();
    // The type of first produced value
    EVT vt = n.getValueType(0);

    // Check to see if this is a BUILD_VECTOR of a bunch of EXTRACT_VECTOR_ELT
    // operations.  If so, and if the EXTRACT_VECTOR_ELT vector inputs come from
    // at most two distinct vectors, turn this into a shuffle node.
    SDValue vecIn1 = new SDValue(), vecIn2 = new SDValue();
    for (int i = 0; i < numInScalars; i++) {
      // ignores undef inputs.
      if (n.getOperand(i).getOpcode() == ISD.UNDEF) continue;

      // if this input is something other than a EXTRACT_VECTOR_ELT with a
      // constant index, bail out.
      if (n.getOperand(i).getOpcode() != ISD.EXTRACT_VECTOR_ELT ||
          !(n.getOperand(i).getOperand(1).getNode() instanceof ConstantSDNode)) {
        vecIn1 = vecIn2 = new SDValue();
        break;
      }

      // if the input vector type disagrees with the result of the build_vector,
      // we can't make a shuffle.
      SDValue extractedFromVec = n.getOperand(i).getOperand(0);
      if (!extractedFromVec.getValueType().equals(vt)) {
        vecIn1 = vecIn2 = new SDValue();
        break;
      }

      // otherwise, remember this, we allow up to two distinct input vectors.
      if (extractedFromVec == vecIn1 || extractedFromVec == vecIn2)
        continue;

      if (vecIn1.getNode() == null)
        vecIn1 = extractedFromVec;
      else if (vecIn2.getNode() == null)
        vecIn2 = extractedFromVec;
      else {
        // too many inputs.
        vecIn1 = vecIn2 = new SDValue();
        break;
      }
    }

    // If everything is good, we can make a shuffle operation.
    if (vecIn1.getNode() != null) {
      TIntArrayList mask = new TIntArrayList();
      for (int i = 0; i < numInScalars; ++i) {
        if (n.getOperand(i).getOpcode() == ISD.UNDEF) {
          mask.add(-1);
          continue;
        }

        // if extracting from the first vector, just use the index directly
        SDValue extract = n.getOperand(i);
        SDValue extVal = extract.getOperand(1);
        if (extract.getOperand(0).equals(vecIn1)) {
          long extIndex = ((ConstantSDNode) extVal.getNode()).getZExtValue();
          if (extIndex > vt.getVectorNumElements())
            return new SDValue();

          mask.add((int) extIndex);
          continue;
        }
        // otherwise, use index + vector size
        int idx = (int) ((ConstantSDNode) extVal.getNode()).getZExtValue();
        mask.add(idx + numInScalars);
      }

      // add input and size info
      if (!isTypeLegal(vt))
        return new SDValue();

      // return the new VECTOR_SHUFFLE node.
      SDValue op1 = vecIn2.getNode() != null ? vecIn2 : dag.getUNDEF(vt);
      return dag.getVectorShuffle(vt, vecIn1, op1, mask.toArray());
    }

    return new SDValue();
  }

  /**
   * This method returns true if we are running before type legalization phase
   * or if the specified vt is legal.
   *
   * @param vt
   * @return
   */
  private boolean isTypeLegal(EVT vt) {
    if (!legalTypes) return true;
    return tli.isTypeLegal(vt);
  }

  private SDValue visitEXTRACT_VECTOR_ELT(SDNode n) {
    // (vextract (scalar_to_vector val, 0) -> val
    SDValue inVec = n.getOperand(0);
    if (inVec.getOpcode() == ISD.SCALAR_TO_VECTOR) {
      // Check if the result type doesn't match the inserted element type. A
      // SCALAR_TO_VECTOR may truncate the inserted element and the
      // EXTRACT_VECTOR_ELT may widen the extracted vector.
      SDValue inOp = inVec.getOperand(0);
      EVT nvt = n.getValueType(0);
      if (!inOp.getValueType().equals(nvt)) {
        Util.assertion(inOp.getValueType().isInteger() && nvt.isInteger());
        return dag.getSExtOrTrunc(inOp, nvt);
      }
      return inOp;
    }

    // Perform only after legalization to ensure build_vector / vector_shuffle
    // optimizations have already been done.
    if (!legalOprations) return new SDValue();

    // (vextract (v4f32 load $addr), c) -> (f32 load $addr+c*size)
    // (vextract (v4f32 s2v (f32 load $addr)), c) -> (f32 load $addr+c*size)
    // (vextract (v4f32 shuffle (load $addr), <1,u,u,u>), 0) -> (f32 load $addr)
    SDValue eltNo = n.getOperand(1);

    if (eltNo.getNode() instanceof ConstantSDNode) {
      int elt = (int) ((ConstantSDNode) eltNo.getNode()).getZExtValue();
      boolean newLoad = false;
      boolean bcNumEltsChanged = false;
      EVT vt = inVec.getValueType();
      EVT extVT = vt.getVectorElementType();
      EVT lvt = extVT;

      if (inVec.getOpcode() == ISD.BIT_CONVERT) {
        EVT bcvt = inVec.getOperand(0).getValueType();
        if (!bcvt.isVector() || extVT.bitsGT(bcvt.getVectorElementType()))
          return new SDValue();
        if (vt.getVectorNumElements() != bcvt.getVectorNumElements())
          bcNumEltsChanged = true;
        inVec = inVec.getOperand(0);
        extVT = bcvt.getVectorElementType();
        newLoad = true;
      }

      LoadSDNode ln0 = null;
      ShuffleVectorSDNode svn = null;
      if (inVec.getNode().isNormalLoad())
        ln0 = (LoadSDNode) inVec.getNode();
      else if (inVec.getOpcode() == ISD.SCALAR_TO_VECTOR &&
          inVec.getOperand(0).getValueType().equals(extVT) &&
          inVec.getOperand(0).getNode().isNormalLoad()) {
        ln0 = (LoadSDNode) inVec.getOperand(0).getNode();
      }
      else if (inVec.getNode() instanceof ShuffleVectorSDNode) {
        svn = (ShuffleVectorSDNode) inVec.getNode();
        // (vextract (vector_shuffle (load $addr), v2, <1, u, u, u>), 1)
        // =>
        // (load $addr+1*size)

        // If the bit convert changed the number of elements, it is unsafe
        // to examine the mask.
        if (bcNumEltsChanged)
          return new SDValue();

        // Select the input vector, guarding against out of range extract vector.
        int numElts = vt.getVectorNumElements();
        int idx = (elt > numElts) ? -1 : svn.getMaskElt(elt);
        inVec = (idx < numElts) ? inVec.getOperand(0) : inVec.getOperand(1);

        if (inVec.getOpcode() == ISD.BIT_CONVERT)
          inVec = inVec.getOperand(0);
        if (inVec.getNode().isNormalLoad()) {
          ln0 = (LoadSDNode) inVec.getNode();
          elt = idx < numElts ? idx : idx - numElts;
        }
      }

      if (ln0 == null || !ln0.hasNumUsesOfValue(1, 0) || ln0.isVolatile())
        return new SDValue();

      // If Idx was -1 above, Elt is going to be -1, so just return undef.
      if (elt == -1)
        return dag.getUNDEF(lvt);

      int align = ln0.getAlignment();
      if (newLoad) {
        // Check the resultant load doesn't need a higher alignment than the
        // original load.
        int newAlign = tli.getTargetData().getABITypeAlignment(lvt.getTypeForEVT(dag.getContext()));
        if (newAlign > align || !tli.isOperationLegalOrCustom(ISD.LOAD, lvt))
          return new SDValue();

        align = newAlign;
      }

      SDValue newPtr = ln0.getBasePtr();
      int ptrOff = 0;
      if (elt != 0) {
        ptrOff = lvt.getSizeInBits()*elt/8;
        EVT ptrType = newPtr.getValueType();
        if (tli.isBigEndian())
          ptrOff = vt.getSizeInBits() / 8 - ptrOff;
        newPtr = dag.getNode(ISD.ADD, ptrType, newPtr, dag.getConstant(ptrOff, ptrType, false));
      }

      return dag.getLoad(lvt, ln0.getChain(), newPtr, ln0.getSrcValue(), ptrOff, ln0.isVolatile(), align);
    }
    return new SDValue();
  }

  private SDValue visitINSERT_VECTOR_ELT(SDNode n) {
    SDValue inVec = n.getOperand(0);
    SDValue inVal = n.getOperand(1);
    SDValue eltNo = n.getOperand(2);
    // if the inserted value is an UNDEF, just use the input vector.
    if (inVal.getOpcode() == ISD.UNDEF)
      return inVec;

    // rseult type
    EVT vt = inVec.getValueType();
    // If we can't generate a legal BUILD_VECTOR, exit
    if (legalOprations && !tli.isOperationLegal(ISD.BUILD_VECTOR, vt))
      return new SDValue();

    // Check that we know which element is being inserted
    if (eltNo.getNode() instanceof ConstantSDNode)
      return new SDValue();

    int elt = (int) ((ConstantSDNode)eltNo.getNode()).getZExtValue();
    // Check that the operand is a BUILD_VECTOR (or UNDEF, which can essentially
    // be converted to a BUILD_VECTOR).  Fill in the Ops vector with the
    // vector elements.
    ArrayList<SDValue> ops = new ArrayList<>();
    if (inVec.getOpcode() == ISD.BUILD_VECTOR) {
      for (int i = 0; i < inVec.getNumOperands(); ++i)
        ops.add(inVec.getOperand(i));
    }
    else if (inVec.getOpcode() == ISD.UNDEF) {
      int numElts = vt.getVectorNumElements();
      for (int i = 0; i < numElts; i++)
        ops.add(dag.getUNDEF(inVal.getValueType()));
    }
    else
      return new SDValue();

    // insert the element.
    if (elt < ops.size()) {
      // All the operands of BUILD_VECTOR must have the same type;
      // we enforce that here.
      EVT opVT = ops.get(0).getValueType();
      if (!inVal.getValueType().equals(opVT))
        inVal = dag.getAnyExtOrTrunc(inVal, opVT);
      ops.set(elt, inVal);
    }
    // build an new vector.
    return dag.getNode(ISD.BUILD_VECTOR, vt, ops);
  }

  /**
   * Look for sequence of load/op/store where op is
   * one of 'or', 'or', and 'and' of immediates. If
   * 'op' is only touching some of the loaded bits,
   * try narrowing the load and store if it would
   * end up being a win for performance or code size.
   *
   * @param n
   * @return
   */
  private SDValue reduceLoadOpStoreWidth(SDNode n) {
    StoreSDNode st = (StoreSDNode) n;
    if (st.isVolatile())
      return new SDValue();

    SDValue chain = st.getValue();
    SDValue val = st.getValue();
    SDValue ptr = st.getBasePtr();
    EVT vt = val.getValueType();

    if (st.isTruncatingStore() || vt.isVector() ||
        !val.hasOneUse())
      return new SDValue();

    int opc = val.getOpcode();
    if ((opc != ISD.OR && opc != ISD.XOR && opc != ISD.AND) ||
        val.getOperand(1).getOpcode() != ISD.Constant)
      return new SDValue();

    SDValue n0 = val.getOperand(0);
    if (n0.getNode().isNormalLoad() && n0.hasOneUse()) {
      LoadSDNode ld = (LoadSDNode) n0.getNode();
      if (!ld.getBasePtr().equals(ptr))
        return new SDValue();

      SDValue n1 = val.getOperand(1);
      int bitwidth = n1.getValueSizeInBits();
      APInt imm = ((ConstantSDNode) n1.getNode()).getAPIntValue();
      if (opc == ISD.AND)
        imm = imm.xor(APInt.getAllOnesValue(bitwidth));
      if (imm.eq(0) || imm.isAllOnesValue())
        return new SDValue();
      int shAmt = imm.countTrailingZeros();
      int msb = bitwidth - imm.countLeadingZeros() - 1;
      int newBW = (int) Util.nextPowerOf2(msb - shAmt);
      EVT newVT = EVT.getIntegerVT(dag.getContext(), newBW);
      while (newBW < bitwidth && !(tli.isOperationLegalOrCustom(opc, newVT) &&
          tli.isNarrowingProfitable(vt, newVT))) {
        newBW = (int) Util.nextPowerOf2(newBW);
        newVT = EVT.getIntegerVT(dag.getContext(), newBW);
      }
      if (newBW >= bitwidth)
        return new SDValue();

      // If the lab changed doesn't start at the type bitwidth
      // boundary, start at the previous one.
      if ((shAmt % newBW) != 0) {
        shAmt = ((shAmt + newBW - 1) / newBW) * newBW - newBW;
      }
      APInt mask = APInt.getBitsSet(bitwidth, shAmt, shAmt + newBW);
      if (imm.and(mask).eq(imm)) {
        APInt newImm = imm.and(mask).lshr(shAmt).trunc(newBW);
        if (opc == ISD.AND)
          newImm = newImm.xor(APInt.getAllOnesValue(newBW));
        long ptrOff = shAmt / 8;
        if (tli.isBigEndian()) {
          ptrOff = (bitwidth + 7 - newBW) / 8 - ptrOff;
        }
        int newAlign = Util.minAlign(ld.getAlignment(), (int) ptrOff);
        if (newAlign < tli.getTargetData().getABITypeAlignment(
            newVT.getTypeForEVT(dag.getContext())))
          return new SDValue();

        SDValue newPtr = dag.getNode(ISD.ADD, ptr.getValueType(),
            ptr, dag.getConstant(ptrOff, ptr.getValueType(), false));
        SDValue newLD = dag.getLoad(newVT, ld.getChain(),
            newPtr, ld.getSrcValue(), ld.getSrcValueOffset(),
            ld.isVolatile(), newAlign);
        SDValue newVal = dag.getNode(opc, newVT, newLD,
            dag.getConstant(newImm, newVT, false));
        SDValue newST = dag.getStore(newLD.getValue(1), newVal, newPtr,
            st.getSrcValue(), st.getSrcValueOffset(),
            false, newAlign);

        addToWorkList(newPtr.getNode());
        addToWorkList(newLD.getNode());
        addToWorkList(newVal.getNode());
        WorklistRemover remover = new WorklistRemover(this);
        dag.replaceAllUsesOfValueWith(n0.getValue(1),
            newLD.getValue(1), remover);
        return newST;

      }
    }
    return new SDValue();
  }

  private SDValue visitSTORE(SDNode n) {
    StoreSDNode st = (StoreSDNode) n;
    SDValue chain = st.getChain();
    SDValue value = st.getValue();
    SDValue ptr = st.getBasePtr();

    if (optLevel != CodeGenOpt.None && st.isUnindexed()) {
      int align = inferAlignment(ptr);
      if (align != 0) {
        return dag.getTruncStore(chain, value, ptr,
            st.getSrcValue(), st.getSrcValueOffset(),
            st.getMemoryVT(), st.isVolatile(), align);
      }
    }

    // If this is a store of a bit convert, store the input value
    // if the resultant store doesn't need a higher alignment than
    // origin.
    if (value.getOpcode() == ISD.BIT_CONVERT && !st.isTruncatingStore() &&
        st.isUnindexed()) {
      int originAlign = st.getAlignment();
      EVT vt = value.getOperand(0).getValueType();
      int align = tli.getTargetData().getABITypeAlignment(vt.getTypeForEVT(dag.getContext()));
      if (align == originAlign && ((!legalOprations && !st.isVolatile()) ||
          tli.isOperationLegalOrCustom(ISD.STORE, vt))) {
        return dag.getStore(chain, value.getOperand(0),
            ptr, st.getSrcValue(), st.getSrcValueOffset(),
            st.isVolatile(), originAlign);
      }
    }

    // turn 'store float 1.0, ptr' -> 'store int 0x12345678, ptr'
    if (value.getNode() instanceof ConstantFPSDNode) {
      ConstantFPSDNode fp = (ConstantFPSDNode) value.getNode();
      if (value.getOpcode() != ISD.TargetConstantFP) {
        SDValue temp;
        switch (fp.getValueType(0).getSimpleVT().simpleVT) {
          default:
            Util.shouldNotReachHere("Unknown FP type!");
            break;
          case MVT.f80:
          case MVT.f128:
          case MVT.ppcf128:
            break;
          case MVT.f32:
            if (((tli.isTypeLegal(new EVT(MVT.i32)) || !legalTypes) &&
                !st.isVolatile()) || tli.isOperationLegalOrCustom(ISD.STORE, new EVT(MVT.i32))) {
              temp = dag.getConstant(fp.getValueAPF().bitcastToAPInt().getZExtValue(), new EVT(MVT.i32), false);
              return dag.getStore(chain, temp, ptr, st.getSrcValue(),
                  st.getSrcValueOffset(), st.isVolatile(),
                  st.getAlignment());
            }
            break;
          case MVT.f64:
            if (((tli.isTypeLegal(new EVT(MVT.i64)) || !legalTypes) &&
                !st.isVolatile()) || tli.isOperationLegalOrCustom(ISD.STORE, new EVT(MVT.i64))) {
              temp = dag.getConstant(fp.getValueAPF().bitcastToAPInt().getZExtValue(), new EVT(MVT.i64), false);
              return dag.getStore(chain, temp, ptr, st.getSrcValue(),
                  st.getSrcValueOffset(), st.isVolatile(),
                  st.getAlignment());
            } else if (!st.isVolatile() && tli.isOperationLegalOrCustom(ISD.STORE, new EVT(MVT.i32))) {
              // Many FP stores are not made apprent util after
              // legalization, e.g. for 64-bit integer store into
              // two 32-bits stores.
              long val = fp.getValueAPF().bitcastToAPInt().getZExtValue();
              SDValue lo = dag.getConstant(val & 0xFFFFFFFF, new EVT(MVT.i32), false);
              SDValue hi = dag.getConstant(val >> 32, new EVT(MVT.i32), false);
              if (tli.isBigEndian()) {
                SDValue t = lo;
                lo = hi;
                hi = t;
              }
              int svoffset = st.getSrcValueOffset();
              int alignment = st.getAlignment();
              boolean isVolatile = st.isVolatile();

              SDValue st0 = dag.getStore(chain, lo, ptr,
                  st.getSrcValue(), svoffset, isVolatile, alignment);
              ptr = dag.getNode(ISD.ADD, ptr.getValueType(),
                  ptr, dag.getConstant(4, ptr.getValueType(), false));
              svoffset = 4;
              alignment = Util.minAlign(alignment, 4);
              SDValue st1 = dag.getStore(chain, hi, ptr,
                  st.getSrcValue(), svoffset, isVolatile, alignment);
              return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), st0, st1);
            }
            break;
        }
      }
    }

    // Attempts to transform n to an indexed store node.
    if (combineToPreIndexedLoadStore(n) || combineToPostIndexedLoadStore(n))
      return new SDValue(n, 0);

    if (st.isTruncatingStore() && st.isUnindexed() &&
        value.getValueType().isInteger()) {
      SDValue shorter = getDemandedBits(value,
          APInt.getLowBitsSet(value.getValueSizeInBits(),
              st.getMemoryVT().getSizeInBits()));
      addToWorkList(shorter.getNode());
      if (shorter.getNode() != null) {
        return dag.getTruncStore(chain, shorter, ptr,
            st.getSrcValue(), st.getSrcValueOffset(),
            st.getMemoryVT(), st.isVolatile(),
            st.getAlignment());
      }

      // Otherwise, check to see if we can simplify this operation
      // with simplifyDemandedBits function, which only works if the
      // value has a single use.
      if (simplifyDemandedBits(value, APInt.getLowBitsSet(
          value.getValueSizeInBits(), st.getMemoryVT().getSizeInBits()))) {
        return new SDValue(n, 0);
      }
    }

    // if this is a load followed by a store to the same location,
    // the store is dead.
    if (value.getNode() instanceof LoadSDNode) {
      LoadSDNode ld = (LoadSDNode) value.getNode();
      if (ld.getBasePtr().equals(ptr) &&
          st.getMemoryVT().equals(ld.getMemoryVT()) &&
          st.isUnindexed() && !st.isVolatile() &&
          chain.reachesChainWithoutSideEffects(new SDValue(ld, 1)))
        return chain;
    }

    // If this is a FP_ROUND or TRUNC followed by a store, fold this
    // into a truncating store. We can do this even if this is already
    // a truncstore.
    int valOpc = value.getOpcode();
    if ((valOpc == ISD.FP_ROUND || valOpc == ISD.TRUNCATE) &&
        value.hasOneUse() && st.isUnindexed() &&
        tli.isTruncStoreLegal(value.getOperand(0).getValueType(),
            st.getMemoryVT())) {
      return dag.getTruncStore(chain, value.getOperand(0),
          ptr, st.getSrcValue(), st.getSrcValueOffset(),
          st.getMemoryVT(), st.isVolatile(), st.getAlignment());
    }

    return reduceLoadOpStoreWidth(n);
  }

  private int inferAlignment(SDValue ptr) {
    int frameIndex = 1 << 31;
    long frameOffset = 0;
    if (ptr.getNode() instanceof FrameIndexSDNode) {
      frameIndex = ((FrameIndexSDNode) ptr.getNode()).getFrameIndex();
    } else if (ptr.getOpcode() == ISD.ADD &&
        ptr.getOperand(1).getNode() instanceof ConstantSDNode &&
        ptr.getOperand(0).getNode() instanceof FrameIndexSDNode) {
      frameIndex = ((FrameIndexSDNode) ptr.getOperand(0).getNode()).getFrameIndex();
      frameOffset = ptr.getConstantOperandVal(1);
    }

    if (frameIndex != (1 << 31)) {
      MachineFrameInfo mfi = dag.getMachineFunction().getFrameInfo();
      if (mfi.isFixedObjectIndex(frameIndex)) {
        long objectOffset = mfi.getObjectOffset(frameIndex) + frameOffset;
        int stackAlign = tli.getTargetMachine().getFrameLowering().getStackAlignment();
        int align = Util.minAlign(stackAlign, (int) objectOffset);
        int fiInfoAlign = Util.minAlign(mfi.getObjectAlignment(frameIndex), (int) frameOffset);
        return Math.max(align, fiInfoAlign);
      }
    }
    return 0;
  }

  private SDValue visitLOAD(SDNode n) {
    LoadSDNode ld = (LoadSDNode) n;
    SDValue chain = ld.getChain();
    SDValue ptr = ld.getBasePtr();

    // Try to infer better alignment information than the load already has.
    if (optLevel != CodeGenOpt.None && ld.isUnindexed()) {
      int align = inferAlignment(ptr);
      if (align != 0 && align > ld.getAlignment()) {
        return dag.getExtLoad(ld.getExtensionType(), ld.getValueType(0),
            chain, ptr, ld.getSrcValue(), ld.getSrcValueOffset(), ld.getMemoryVT(),
            ld.isVolatile(), align);
      }
    }

    // If load is not volatile and there are no uses of the loaded value (and
    // the updated indexed value in case of indexed loads), change uses of the
    // chain value into uses of the chain input (i.e. delete the dead load).
    if (!ld.isVolatile()) {
      if (n.getValueType(1).equals(new EVT(MVT.Other))) {
        // unindexed load
        if (n.hasNumUsesOfValue(0, 0)) {
          // It's not safe to use the two value CombineTo variant here. e.g.
          // v1, chain2 = load chain1, loc
          // v2, chain3 = load chain2, loc
          // v3         = add v2, c
          // Now we replace use of chain2 with chain1.  This makes the second load
          // isomorphic to the one we are deleting, and thus makes this load live.
          WorklistRemover remover = new WorklistRemover(this);
          dag.replaceAllUsesOfValueWith(new SDValue(n, 1), chain, remover);
          if (n.isUseEmpty()) {
            removeFromWorkList(n);
            dag.deleteNode(n);
          }
          return new SDValue(n, 0);
        }
      } else {
        // indexed loads.
        Util.assertion(n.getValueType(2).equals(new EVT(MVT.Other)), "Malformed indexed load?");
        if (n.hasNumUsesOfValue(0, 0) && n.hasNumUsesOfValue(0, 1)) {
          SDValue undef = dag.getUNDEF(n.getValueType(0));
          WorklistRemover remover = new WorklistRemover(this);
          dag.replaceAllUsesOfValueWith(new SDValue(n, 0), undef, remover);
          dag.replaceAllUsesOfValueWith(new SDValue(n, 1),
              dag.getUNDEF(n.getValueType(1)), remover);
          dag.replaceAllUsesOfValueWith(new SDValue(n, 2), chain, remover);
          removeFromWorkList(n);
          dag.deleteNode(n);
          return new SDValue(n, 0);
        }
      }
    }

    // If this load is directly stored, replace the load value with the stored
    if (ld.getExtensionType() == LoadExtType.NON_EXTLOAD &&
        !ld.isVolatile()) {
      if (chain.getNode().isNONTRUNCStore()) {
        StoreSDNode st = (StoreSDNode) chain.getNode();
        if (st.getBasePtr().equals(ptr) &&
            st.getValue().getValueType().equals(n.getValueType(0))) {
          return combineTo(n, chain.getOperand(1), chain);
        }
      }
    }

    if (combineToPreIndexedLoadStore(n) || combineToPostIndexedLoadStore(n))
      return new SDValue(n, 0);

    return new SDValue();
  }

  /**
   * Try turning a load / store into a
   * pre-indexed load / store when the base pointer is an add or subtract
   * and it has other uses besides the load / store. After the
   * transformation, the new indexed load / store has effectively folded
   * the add / subtract in and all of its other uses are redirected to the
   * new load / store.
   *
   * @param n
   * @return
   */
  private boolean combineToPreIndexedLoadStore(SDNode n) {
    if (!legalOprations) return false;

    boolean isLoad = true;
    SDValue ptr;
    EVT vt;
    if (n instanceof LoadSDNode) {
      LoadSDNode ld = (LoadSDNode) n;
      if (ld.isIndexed()) return false;
      vt = ld.getMemoryVT();
      if (!tli.isIndexedLoadLegal(PRE_INC, vt) &&
          !tli.isIndexedLoadLegal(PRE_DEC, vt))
        return false;
      ptr = ld.getBasePtr();
    } else if (n instanceof StoreSDNode) {
      StoreSDNode st = (StoreSDNode) n;
      if (st.isIndexed()) return false;
      vt = st.getMemoryVT();
      if (!tli.isIndexedStoreLegal(PRE_INC, vt) &&
          !tli.isIndexedStoreLegal(PRE_DEC, vt))
        return false;
      ptr = st.getBasePtr();
      isLoad = false;
    } else {
      return false;
    }

    if ((ptr.getOpcode() != ISD.ADD && ptr.getOpcode() != ISD.SUB) ||
        ptr.hasOneUse())
      return false;
    OutRef<SDValue> x = new OutRef<>(new SDValue());
    OutRef<SDValue> x2 = new OutRef<>(new SDValue());
    OutRef<MemIndexedMode> x3 = new OutRef<>(UNINDEXED);
    if (!tli.isPreIndexedAddressPart(n, x, x2, x3, dag))
      return false;

    SDValue basePtr = x.get(), offset = x2.get();
    MemIndexedMode am = x3.get();
    if (offset.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) offset.getNode()).isNullValue())
      return false;

    if (basePtr.getNode() instanceof FrameIndexSDNode ||
        basePtr.getNode() instanceof RegisterSDNode) {
      return false;
    }

    if (!isLoad) {
      SDValue val = ((StoreSDNode) n).getValue();
      if (val.equals(basePtr) || basePtr.getNode().isPredecessorOf(val.getNode()))
        return false;
    }

    boolean realUse = false;
    for (SDUse u : ptr.getNode().getUseList()) {
      SDNode user = u.getUser();
      if (user.equals(n))
        continue;
      if (user.isPredecessorOf(n))
        return false;

      if (!((user.getOpcode() == ISD.LOAD &&
          ((LoadSDNode) user).getBasePtr().equals(ptr))) ||
          (user.getOpcode() == ISD.STORE &&
              ((StoreSDNode) user).getBasePtr().equals(ptr)))
        realUse = true;
    }

    if (!realUse) return false;

    SDValue result;
    if (isLoad)
      result = dag.getIndexedLoad(new SDValue(n, 0), basePtr, offset, am);
    else
      result = dag.getIndexedStore(new SDValue(n, 0), basePtr, offset, am);

    WorklistRemover remover = new WorklistRemover(this);
    if (isLoad) {
      dag.replaceAllUsesOfValueWith(new SDValue(n, 0), result.getValue(0),
          remover);
      dag.replaceAllUsesOfValueWith(new SDValue(n, 1), result.getValue(2),
          remover);
    } else {
      dag.replaceAllUsesOfValueWith(new SDValue(n, 0), result.getValue(1),
          remover);
    }
    dag.deleteNode(n);
    dag.replaceAllUsesOfValueWith(ptr, result.getValue(isLoad ? 1 : 0),
        remover);
    removeFromWorkList(ptr.getNode());
    dag.deleteNode(ptr.getNode());
    return true;
  }

  /**
   * Try to combine a load / store with a
   * add / sub of the base pointer node into a post-indexed load / store.
   * The transformation folded the add / subtract into the new indexed
   * load / store effectively and all of its uses are redirected to the
   * new load / store.
   *
   * @param n
   * @return
   */
  private boolean combineToPostIndexedLoadStore(SDNode n) {
    if (!legalOprations) return false;

    boolean isLoad = true;
    SDValue ptr;
    EVT vt;
    if (n instanceof LoadSDNode) {
      LoadSDNode ld = (LoadSDNode) n;
      if (ld.isIndexed()) return false;
      vt = ld.getMemoryVT();
      if (!tli.isIndexedLoadLegal(POST_INC, vt) &&
          !tli.isIndexedLoadLegal(POST_DEC, vt))
        return false;
      ptr = ld.getBasePtr();
    } else if (n instanceof StoreSDNode) {
      StoreSDNode st = (StoreSDNode) n;
      if (st.isIndexed()) return false;
      vt = st.getMemoryVT();
      if (!tli.isIndexedStoreLegal(POST_INC, vt) &&
          !tli.isIndexedStoreLegal(POST_DEC, vt))
        return false;
      ptr = st.getBasePtr();
      isLoad = false;
    } else {
      return false;
    }

    if (ptr.hasOneUse())
      return false;

    for (SDUse u : ptr.getNode().getUseList()) {
      SDNode user = u.getUser();
      if (user.equals(n) || (user.getOpcode() != ISD.ADD &&
          user.getOpcode() != ISD.SUB))
        continue;

      OutRef<SDValue> x = new OutRef<>(new SDValue());
      OutRef<SDValue> x2 = new OutRef<>(new SDValue());
      OutRef<MemIndexedMode> x3 = new OutRef<>(UNINDEXED);
      boolean res = !tli.isPreIndexedAddressPart(n, x, x2, x3, dag);
      SDValue basePtr = x.get(), offset = x2.get();
      MemIndexedMode am = x3.get();
      if (res) {
        if (ptr.equals(offset)) {
          SDValue t = offset;
          offset = ptr;
          ptr = t;
        }
        if (!ptr.equals(basePtr))
          continue;
        if (offset.getNode() instanceof ConstantSDNode &&
            ((ConstantSDNode) offset.getNode()).isNullValue())
          continue;

        if (basePtr.getNode() instanceof FrameIndexSDNode ||
            basePtr.getNode() instanceof RegisterSDNode)
          continue;

        boolean tryNext = false;
        for (SDUse u2 : basePtr.getNode().getUseList()) {
          SDNode user2 = u2.getUser();
          if (user2.equals(ptr.getNode()))
            continue;

          if (user2.getOpcode() == ISD.ADD || user2.getOpcode() == ISD.SUB) {
            boolean realUse = false;
            for (SDUse use : user2.getUseList()) {
              SDNode useUse = use.getUser();
              if (!((useUse.getOpcode() == ISD.LOAD &&
                  ((LoadSDNode) useUse).getBasePtr().getNode().equals(user2)) ||
                  (useUse.getOpcode() == ISD.STORE &&
                      ((StoreSDNode) useUse).getBasePtr().getNode().equals(user2)))) {
                realUse = true;
              }
            }
            if (!realUse) {
              tryNext = true;
              break;
            }
          }
        }

        if (tryNext) continue;

        if (!user.isPredecessorOf(n) && !n.isPredecessorOf(user)) {
          SDValue result = isLoad ?
              dag.getIndexedLoad(new SDValue(n, 0), basePtr, offset, am) :
              dag.getIndexedStore(new SDValue(n, 0), basePtr, offset, am);

          WorklistRemover remover = new WorklistRemover(this);
          if (isLoad) {
            dag.replaceAllUsesOfValueWith(new SDValue(n, 0), result.getValue(0),
                remover);
            dag.replaceAllUsesOfValueWith(new SDValue(n, 1), result.getValue(2),
                remover);
          } else {
            dag.replaceAllUsesOfValueWith(new SDValue(n, 0), result.getValue(1),
                remover);
          }
          dag.deleteNode(n);
          dag.replaceAllUsesOfValueWith(new SDValue(user, 0),
              result.getValue(isLoad ? 1 : 0),
              remover);
          removeFromWorkList(user);
          dag.deleteNode(user);
        }
      }
    }
    return true;
  }

  private SDValue visitBR_CC(SDNode n) {
    CondCodeSDNode ccn = (CondCodeSDNode) n.getOperand(1).getNode();
    SDValue lhs = n.getOperand(2), rhs = n.getOperand(3);
    SDValue simplify = simplifySetCC(new EVT(tli.getSetCCResultType(lhs.getValueType())),
        lhs, rhs, ccn.getCondition(), false);
    if (simplify.getNode() != null)
      addToWorkList(simplify.getNode());

    if (simplify.getNode() instanceof ConstantSDNode &&
        !((ConstantSDNode) simplify.getNode()).isNullValue()) {
      if (!((ConstantSDNode) simplify.getNode()).isNullValue())
        return dag.getNode(ISD.BR, new EVT(MVT.Other), n.getOperand(0), n.getOperand(4));
      else
        return dag.getNode(ISD.BR, new EVT(MVT.Other), n.getOperand(0));
    }
    if (simplify.getNode() != null && simplify.getOpcode() == ISD.SETCC) {
      // to setcc
      return dag.getNode(ISD.BR_CC, new EVT(MVT.Other), n.getOperand(0),
          simplify.getOperand(2), simplify.getOperand(0), simplify.getOperand(1),
          n.getOperand(4));
    }
    return new SDValue();
  }

  private SDValue visitBRCOND(SDNode n) {
    SDValue chain = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    SDValue n2 = n.getOperand(2);
    ConstantSDNode n1C = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    if (n1C != null && n1C.isNullValue())
      return chain;

    if (n1C != null && n1C.getAPIntValue().eq(1))
      return dag.getNode(ISD.BR, new EVT(MVT.Other), chain, n2);
    if (n1.getOpcode() == ISD.SETCC &&
        tli.isOperationLegalOrCustom(ISD.BR_CC, new EVT(MVT.Other))) {
      return dag.getNode(ISD.BR_CC, new EVT(MVT.Other), chain, n1.getOperand(2),
          n1.getOperand(0), n1.getOperand(1), n2);
    }
    if (n1.hasOneUse() && n1.getOpcode() == ISD.SRL) {
      // Match this pattern so that we can generate simpler code:
      //
      //   %a = ...
      //   %b = and i32 %a, 2
      //   %c = srl i32 %b, 1
      //   brcond i32 %c ...
      //
      // into
      //
      //   %a = ...
      //   %b = and %a, 2
      //   %c = setcc eq %b, 0
      //   brcond %c ...
      //
      // This applies only when the AND constant value has one bit set and the
      // SRL constant is equal to the log2 of the AND constant. The back-end is
      // smart enough to convert the result into a TEST/JMP sequence.
      SDValue op0 = n1.getOperand(0);
      SDValue op1 = n1.getOperand(1);

      if (op0.getOpcode() == ISD.AND &&
          op0.hasOneUse() &&
          op1.getOpcode() == ISD.Constant) {
        SDValue andOp0 = op0.getOperand(0);
        SDValue andOp1 = op0.getOperand(1);
        if (andOp1.getOpcode() == ISD.Constant) {
          APInt andCst = ((ConstantSDNode) andOp1.getNode()).getAPIntValue();
          if (andCst.isPowerOf2() &&
              ((ConstantSDNode) op1.getNode()).getAPIntValue().eq(andCst.logBase2())) {
            SDValue setcc = dag.getSetCC(new EVT(tli.getSetCCResultType(op0.getValueType())),
                op0, dag.getConstant(0, op0.getValueType(), false), CondCode.SETNE);

            dag.replaceAllUsesOfValueWith(n1, setcc, null);
            removeFromWorkList(n1.getNode());
            dag.deleteNode(n1.getNode());
            return dag.getNode(ISD.BRCOND, new EVT(MVT.Other), chain, setcc, n2);
          }
        }
      }
    }

    return new SDValue();
  }

  private SDValue visitFABS(SDNode n) {
    SDValue n0 = n.getOperand(0);
    ConstantFPSDNode fp = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    EVT vt = n.getValueType(0);
    if (fp != null && !vt.equals(new EVT(MVT.ppcf128)))
      return dag.getNode(ISD.FABS, vt, n0);

    if (n0.getOpcode() == ISD.FABS)
      return n0.getOperand(0);
    if (n0.getOpcode() == ISD.FNEG || n0.getOpcode() == ISD.FCOPYSIGN)
      return dag.getNode(ISD.FABS, n0.getOperand(0).getValueType(), n0.getOperand(0));
    if (n0.getOpcode() == ISD.BIT_CONVERT && n0.hasOneUse() &&
        n0.getOperand(0).getValueType().isInteger() &&
        !n0.getOperand(0).getValueType().isVector()) {
      SDValue integer = n0.getOperand(0);
      EVT intVT = integer.getValueType();
      if (intVT.isInteger() && !intVT.isVector()) {
        integer = dag.getNode(ISD.AND, intVT, integer,
            dag.getConstant(APInt.getSignBit(intVT.getSizeInBits()).not(), intVT, false));
        addToWorkList(integer.getNode());
        return dag.getNode(ISD.BIT_CONVERT, n.getValueType(0), integer);
      }
    }
    return new SDValue();
  }

  /**
   * Return 1 if we can compute the negated form of the
   * specified expression for the same cost as the expression itself, or 2 if we
   * can compute the negated form more cheaply than the expression itself.
   *
   * @param op
   * @param legalOpration
   * @return
   */
  private int isNegatibleForFree(SDValue op, boolean legalOpration) {
    return isNegatibleForFree(op, legalOpration, 0);
  }

  private int isNegatibleForFree(SDValue op, boolean legalOpration, int depth) {
    if (op.getValueType().equals(new EVT(MVT.ppcf128)))
      return 0;

    if (op.getOpcode() == ISD.FNEG) return 2;
    if (!op.hasOneUse()) return 0;

    if (depth > 6) return 0;
    switch (op.getOpcode()) {
      default:
        return 0;
      case ISD.ConstantFP:
        return legalOpration ? 0 : 1;
      case ISD.FADD:
        // We can't turn -(A-B) into B-A when we honor signed zeros.
        return EnableUnsafeFPMath.value ? 1 : 0;
      case ISD.FMUL:
      case ISD.FDIV:
        if (!EnableUnsafeFPMath.value) return 0;
        int v = isNegatibleForFree(op.getOperand(0), legalOpration, depth + 1);
        if (v != 0) return v;
        return isNegatibleForFree(op.getOperand(1), legalOpration, depth + 1);
      case ISD.FP_EXTEND:
      case ISD.FP_ROUND:
      case ISD.FSIN:
        return isNegatibleForFree(op.getOperand(0), legalOpration, depth + 1);
    }
  }

  private SDValue getNegatedExpression(SDValue op, boolean legalOpration) {
    return getNegatedExpression(op, legalOpration, 0);
  }

  /**
   * If isNegatibleForFree returns true, this function returns the newly negated expression.
   *
   * @param op
   * @param legalOpration
   * @param depth
   * @return
   */
  private SDValue getNegatedExpression(SDValue op, boolean legalOpration, int depth) {
    if (op.getOpcode() == ISD.FNEG) return op.getOperand(0);

    Util.assertion(op.hasOneUse(), "Unknown reuse!");
    Util.assertion(depth <= 6, "getNegatedExpression doesn't match isNegatibleForFree");
    switch (op.getOpcode()) {
      default:
        Util.shouldNotReachHere("Unknown code!");
        return new SDValue();
      case ISD.ConstantFP: {
        APFloat f = ((ConstantFPSDNode) op.getNode()).getValueAPF().clone();
        f.changeSign();
        return dag.getConstantFP(f, op.getValueType(), false);
      }
      case ISD.FADD: {
        Util.assertion(EnableUnsafeFPMath.value);
        if (isNegatibleForFree(op.getOperand(0), legalOpration, depth + 1) != 0)
          return dag.getNode(ISD.FSUB, op.getValueType(),
              getNegatedExpression(op.getOperand(0), legalOpration, depth + 1),
              op.getOperand(1));

        return dag.getNode(ISD.FSUB, op.getValueType(),
            getNegatedExpression(op.getOperand(1), legalOpration, depth + 1),
            op.getOperand(0));
      }
      case ISD.FSUB: {
        Util.assertion(EnableUnsafeFPMath.value);
        ConstantFPSDNode fp = op.getOperand(0).getNode() instanceof ConstantFPSDNode ?
            (ConstantFPSDNode) op.getOperand(0).getNode() : null;

        if (fp != null && fp.getValueAPF().isZero())
          return op.getOperand(1);

        return dag.getNode(ISD.FSUB, op.getValueType(), op.getOperand(1), op.getOperand(0));
      }
      case ISD.FMUL:
      case ISD.FDIV: {
        if (isNegatibleForFree(op.getOperand(0), legalOpration, depth + 1) != 0) {
          return dag.getNode(op.getOpcode(), op.getValueType(),
              getNegatedExpression(op.getOperand(0), legalOpration, depth + 1),
              op.getOperand(1));
        }
        return dag.getNode(op.getOpcode(), op.getValueType(), op.getOperand(0),
            getNegatedExpression(op.getOperand(1), legalOpration, depth + 1));
      }
      case ISD.FP_EXTEND:
      case ISD.FSIN:
        return dag.getNode(op.getOpcode(), op.getValueType(),
            getNegatedExpression(op.getOperand(0), legalOpration, depth + 1));
      case ISD.FP_ROUND:
        return dag.getNode(ISD.FP_ROUND, op.getValueType(),
            getNegatedExpression(op.getOperand(0), legalOpration, depth + 1),
            op.getOperand(1));
    }
  }

  private SDValue visitFNEG(SDNode n) {
    SDValue n0 = n.getOperand(0);
    if (isNegatibleForFree(n0, legalOprations) != 0)
      return getNegatedExpression(n0, legalOprations);

    if (n0.getOpcode() == ISD.BIT_CONVERT && n0.hasOneUse() &&
        n0.getOperand(0).getValueType().isInteger() &&
        !n0.getOperand(0).getValueType().isVector()) {
      SDValue integer = n0.getOperand(0);
      EVT intVT = integer.getValueType();
      if (intVT.isInteger() && !intVT.isVector()) {
        integer = dag.getNode(ISD.XOR, intVT, integer,
            dag.getConstant(APInt.getSignBit(intVT.getSizeInBits()), intVT, false));
        addToWorkList(integer.getNode());
        return dag.getNode(ISD.BIT_CONVERT, n.getValueType(0), integer);
      }
    }

    return new SDValue();
  }

  private SDValue visitFP_EXTEND(SDNode n) {
    SDValue n0 = n.getOperand(0);
    ConstantFPSDNode fp = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    EVT vt = n.getValueType(0);
    if (n.hasOneUse() && n.getUse(0).getUser().getOpcode() == ISD.FP_ROUND)
      return new SDValue();

    if (fp != null && !vt.equals(new EVT(MVT.ppcf128)))
      return dag.getNode(ISD.FP_EXTEND, vt, n0);

    // Turn fp_extend(fp_round(X, 1)) -> x since the fp_round doesn't affect the
    // value of X.
    if (n0.getOpcode() == ISD.FP_ROUND &&
        n0.getNode().getConstantOperandVal(1) == 1) {
      SDValue in = n0.getOperand(0);
      if (in.getValueType().equals(vt)) return in;

      if (vt.bitsLT(in.getValueType()))
        return dag.getNode(ISD.FP_ROUND, vt, in, n0.getOperand(1));
      return dag.getNode(ISD.FP_EXTEND, vt, in);
    }

    if (n0.getNode().isNONExtLoad() && n0.hasOneUse() &&
        ((!legalOprations && !((LoadSDNode) n0.getNode()).isVolatile()) ||
            tli.isLoadExtLegal(LoadExtType.EXTLOAD, n0.getValueType()))) {
      LoadSDNode ld = (LoadSDNode) n0.getNode();
      SDValue extLoad = dag.getExtLoad(LoadExtType.EXTLOAD, vt, ld.getChain(),
          ld.getBasePtr(), ld.getSrcValue(), ld.getSrcValueOffset(),
          n0.getValueType(), ld.isVolatile(), ld.getAlignment());

      combineTo(n, extLoad, true);
      combineTo(n0.getNode(), dag.getNode(ISD.FP_ROUND, n0.getValueType(),
          extLoad, dag.getIntPtrConstant(1)),
          extLoad.getValue(1), true);
      return new SDValue(n, 0);
    }

    return new SDValue();
  }

  private SDValue visitFP_ROUND_INREG(SDNode n) {
    SDValue n0 = n.getOperand(0);
    ConstantFPSDNode fp = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    EVT vt = n.getValueType(0);
    EVT evt = ((VTSDNode) n.getOperand(1).getNode()).getVT();

    if (fp != null && (tli.isTypeLegal(evt) || !legalTypes)) {
      SDValue round = dag.getConstantFP(fp.getValueAPF(), evt, false);
      return dag.getNode(ISD.FP_EXTEND, vt, round);
    }
    return new SDValue();
  }

  private SDValue visitFP_ROUND(SDNode n) {
    SDValue n0 = n.getOperand(0);
    ConstantFPSDNode fp = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    EVT vt = n.getValueType(0);
    if (fp != null && !n0.getValueType().equals(new EVT(MVT.ppcf128)))
      return dag.getNode(ISD.FP_ROUND, vt, n0);

    if (n0.getOpcode() == ISD.FP_EXTEND && vt.equals(n0.getOperand(0).getValueType()))
      return n0.getOperand(0);

    if (n0.getOpcode() == ISD.FP_ROUND) {
      boolean isTrunc = n.getConstantOperandVal(1) == 1 &&
          n0.getNode().getConstantOperandVal(1) == 1;
      return dag.getNode(ISD.FP_ROUND, vt, n0.getOperand(0),
          dag.getIntPtrConstant(isTrunc ? 1 : 0));
    }

    if (n0.getOpcode() == ISD.FCOPYSIGN && n0.hasOneUse()) {
      SDValue temp = dag.getNode(ISD.FP_ROUND, vt, n0.getOperand(0));
      addToWorkList(temp.getNode());
      return dag.getNode(ISD.FCOPYSIGN, vt, temp, n0.getOperand(1));
    }
    return new SDValue();
  }

  private SDValue visitFP_TO_UINT(SDNode n) {
    SDValue n0 = n.getOperand(0);
    ConstantFPSDNode fp = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    EVT vt = n.getValueType(0);

    if (fp != null && !vt.equals(new EVT(MVT.ppcf128)))
      return dag.getNode(ISD.FP_TO_UINT, vt, n0);

    return new SDValue();
  }

  private SDValue visitFP_TO_SINT(SDNode n) {
    SDValue n0 = n.getOperand(0);
    ConstantFPSDNode fp = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    EVT vt = n.getValueType(0);

    if (fp != null)
      return dag.getNode(ISD.FP_TO_SINT, vt, n0);

    return new SDValue();
  }

  private SDValue visitUINT_TO_FP(SDNode n) {
    SDValue n0 = n.getOperand(0);
    ConstantSDNode c0 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    EVT vt = n.getValueType(0);
    EVT opVT = n0.getValueType();

    if (c0 != null && !opVT.equals(new EVT(MVT.ppcf128)))
      return dag.getNode(ISD.UINT_TO_FP, vt, n0);

    if (!tli.isOperationLegalOrCustom(ISD.UINT_TO_FP, opVT) &&
        tli.isOperationLegalOrCustom(ISD.SINT_TO_FP, opVT)) {
      if (dag.signBitIsZero(n0, 0))
        return dag.getNode(ISD.SINT_TO_FP, vt, n0);
    }

    return new SDValue();
  }

  private SDValue visitSINT_TO_FP(SDNode n) {
    SDValue n0 = n.getOperand(0);
    ConstantSDNode c0 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    EVT vt = n.getValueType(0);
    EVT opVT = n0.getValueType();

    if (c0 != null && !opVT.equals(new EVT(MVT.ppcf128)))
      return dag.getNode(ISD.SINT_TO_FP, vt, n0);

    if (!tli.isOperationLegalOrCustom(ISD.SINT_TO_FP, opVT) &&
        tli.isOperationLegalOrCustom(ISD.UINT_TO_FP, opVT)) {
      if (dag.signBitIsZero(n0, 0))
        return dag.getNode(ISD.UINT_TO_FP, vt, n0);
    }
    return new SDValue();
  }

  private SDValue visitFCOPYSIGN(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantFPSDNode fp0 = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    ConstantFPSDNode fp1 = n1.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n1.getNode() : null;
    EVT vt = n.getValueType(0);

    if (fp0 != null && fp1 != null && !vt.equals(new EVT(MVT.ppcf128))) {
      return dag.getNode(ISD.FCOPYSIGN, vt, n0, n1);
    }

    if (fp1 != null) {
      APFloat v = fp1.getValueAPF();
      if (!v.isNegative()) {
        if (!legalOprations || tli.isOperationLegal(ISD.FABS, vt))
          return dag.getNode(ISD.FABS, vt, n0);
      } else {
        if (!legalOprations || tli.isOperationLegal(ISD.FNEG, vt))
          return dag.getNode(ISD.FNEG, vt,
              dag.getNode(ISD.FABS, vt, n0));
      }
    }

    // copysign(fabs(x), y) -> copysign(x, y)
    // copysign(fneg(x), y) -> copysign(x, y)
    // copysign(copysign(x,z), y) -> copysign(x, y)
    if (n0.getOpcode() == ISD.FABS || n0.getOpcode() == ISD.FNEG ||
        n0.getOpcode() == ISD.FCOPYSIGN) {
      return dag.getNode(ISD.FCOPYSIGN, vt, n0.getOperand(0), n1);
    }

    if (n1.getOpcode() == ISD.FABS)
      return dag.getNode(ISD.FABS, vt, n0);
    if (n1.getOpcode() == ISD.FCOPYSIGN)
      return dag.getNode(ISD.FCOPYSIGN, vt, n0, n1.getOperand(1));

    if (n1.getOpcode() == ISD.FP_EXTEND) {
      return dag.getNode(ISD.FCOPYSIGN, vt, n0, n1.getOperand(0));
    }

    return new SDValue();
  }

  private SDValue visitFREM(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantFPSDNode fp0 = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    ConstantFPSDNode fp1 = n1.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n1.getNode() : null;
    EVT vt = n.getValueType(0);

    if (fp0 != null && fp1 != null & !vt.equals(new EVT(MVT.ppcf128)))
      return dag.getNode(ISD.FREM, vt, n0, n1);

    return new SDValue();
  }

  private SDValue visitFDIV(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantFPSDNode fp0 = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    ConstantFPSDNode fp1 = n1.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n1.getNode() : null;
    EVT vt = n.getValueType(0);

    if (vt.isVector()) {
      SDValue res = simplifyVBinOp(n);
      if (res.getNode() != null) return res;
    }

    // fold (fdiv c1, c2) -> c1/c2
    if (fp0 != null && fp1 != null && !vt.equals(new EVT(MVT.ppcf128))) {
      return dag.getNode(ISD.FDIV, vt, n0, n1);
    }
    // (fdiv (fneg X), (fneg Y)) -> (fdiv X, Y)
    int lhsNeg = isNegatibleForFree(n0, legalOprations);
    int rhsNeg = isNegatibleForFree(n1, legalOprations);
    if (lhsNeg == 2 && rhsNeg == 2) {
      return dag.getNode(ISD.FDIV, vt, getNegatedExpression(n0, legalOprations),
          getNegatedExpression(n1, legalOprations));
    }

    return new SDValue();
  }

  private SDValue visitFMUL(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantFPSDNode fp0 = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    ConstantFPSDNode fp1 = n1.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n1.getNode() : null;
    EVT vt = n.getValueType(0);

    if (vt.isVector()) {
      SDValue res = simplifyVBinOp(n);
      if (res.getNode() != null) return res;
    }

    // fold (fmul c1, c2) -> c1*c2
    if (fp0 != null && fp1 != null && !vt.equals(new EVT(MVT.ppcf128)))
      return dag.getNode(ISD.FMUL, vt, n0, n1);

    // canonicalize constant to RHS
    if (fp0 != null && fp1 == null)
      return dag.getNode(ISD.FMUL, vt, n1, n0);

    // fold (fmul A, 0) -> 0
    if (EnableUnsafeFPMath.value && fp1 != null && fp1.getValueAPF().isZero())
      return n1;

    // fold (fmul A, 0) -> 0, vector edition.
    if (EnableUnsafeFPMath.value && fp1 != null && ISD.isBuildVectorAllZeros(n1.getNode()))
      return n1;

    // fold (fmul X, 2.0) -> (fadd X, X)
    if (fp1 != null && fp1.isExactlyValue(+2.0))
      return dag.getNode(ISD.FADD, vt, n0, n0);

    // fold (fmul X, -1.0) -> (fneg X)
    if (fp1 != null && fp1.isExactlyValue(-1.0) &&
        (!legalOprations || tli.isOperationLegal(ISD.FNEG, vt)))
      return dag.getNode(ISD.FNEG, vt, n0);

    // fold (fmul (fneg X), (fneg Y)) -> (fmul X, Y)
    int lhsNeg = isNegatibleForFree(n0, legalOprations);
    int rhsNeg = isNegatibleForFree(n1, legalOprations);
    if (lhsNeg == 2 && rhsNeg == 2) {
      return dag.getNode(ISD.MUL, vt,
          getNegatedExpression(n0, legalOprations),
          getNegatedExpression(n1, legalOprations));
    }
    // If allowed, fold (fmul (fmul x, c1), c2) -> (fmul x, (fmul c1, c2))
    if (EnableUnsafeFPMath.value && fp1 != null &&
        n0.getOpcode() == ISD.FMUL &&
        n0.getOperand(1).getNode() instanceof ConstantFPSDNode) {
      ConstantFPSDNode n01FP = (ConstantFPSDNode) n0.getOperand(1).getNode();
      return dag.getNode(ISD.FMUL, vt, n0.getOperand(0),
          dag.getNode(ISD.FMUL, vt, n0.getOperand(1), n1));
    }
    return new SDValue();
  }

  private SDValue visitFSUB(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantFPSDNode fp0 = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    ConstantFPSDNode fp1 = n1.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n1.getNode() : null;
    EVT vt = n.getValueType(0);

    if (vt.isVector()) {
      SDValue res = simplifyVBinOp(n);
      if (res.getNode() != null) return res;
    }

    // fold (fsub c1, c2) -> c1-c2
    if (fp0 != null && fp1 != null && !vt.equals(new EVT(MVT.ppcf128))) {
      return dag.getNode(ISD.FSUB, vt, n0, n1);
    }
    // fold (fsub A, 0) -> A
    if (EnableUnsafeFPMath.value && fp1 != null && fp1.getValueAPF().isZero())
      return n0;

    // fold (fsub 0, B) -> -B
    if (EnableUnsafeFPMath.value && fp0 != null && fp0.getValueAPF().isZero()) {
      if (isNegatibleForFree(n1, legalOprations) != 0)
        return getNegatedExpression(n1, legalOprations);
      if (!legalOprations || tli.isOperationLegal(ISD.FNEG, vt))
        return dag.getNode(ISD.FNEG, vt, n1);
    }

    // fold (fsub A, (fneg B)) -> (fadd A, B)
    if (isNegatibleForFree(n1, legalOprations) != 0) {
      return dag.getNode(ISD.FADD, vt, n0, getNegatedExpression(n1, legalOprations));
    }
    return new SDValue();
  }

  private SDValue visitFADD(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantFPSDNode fp0 = n0.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n0.getNode() : null;
    ConstantFPSDNode fp1 = n1.getNode() instanceof ConstantFPSDNode ?
        (ConstantFPSDNode) n1.getNode() : null;
    EVT vt = n.getValueType(0);

    if (vt.isVector()) {
      SDValue res = simplifyVBinOp(n);
      if (res.getNode() != null) return res;
    }

    if (fp0 != null && fp1 != null && !vt.equals(new EVT(MVT.ppcf128))) {
      return dag.getNode(ISD.FADD, vt, n0, n1);
    }
    if (fp0 != null && fp1 == null)
      return dag.getNode(ISD.FADD, vt, n1, n0);
    if (EnableUnsafeFPMath.value && fp1 != null && fp1.getValueAPF().isZero())
      return n0;
    if (isNegatibleForFree(n1, legalOprations) == 2)
      return dag.getNode(ISD.FSUB, vt, n0, getNegatedExpression(n1, legalOprations));
    if (isNegatibleForFree(n0, legalOprations) == 2)
      return dag.getNode(ISD.FSUB, vt, n1, getNegatedExpression(n0, legalOprations));

    if (EnableUnsafeFPMath.value && fp1 != null &&
        n0.getOpcode() == ISD.FADD &&
        n0.hasOneUse() && n0.getOperand(1).getNode() instanceof ConstantFPSDNode) {
      return dag.getNode(ISD.FADD, vt, n0.getOperand(0),
          dag.getNode(ISD.FADD, vt, n0.getOperand(1), n1));
    }

    return new SDValue();
  }

  private SDValue visitBUILD_PAIR(SDNode n) {
    EVT vt = n.getValueType(0);
    return combineConsecutiveLoads(n, vt);
  }

  private SDNode getBuildPairElt(SDNode n, int i) {
    SDValue elt = n.getOperand(i);
    if (elt.getOpcode() != ISD.MERGE_VALUES)
      return elt.getNode();
    return elt.getOperand(elt.getResNo()).getNode();
  }

  private SDValue combineConsecutiveLoads(SDNode n, EVT vt) {
    Util.assertion(n.getOpcode() == ISD.BUILD_PAIR);
    SDNode t = getBuildPairElt(n, 0);
    LoadSDNode ld1 = t instanceof LoadSDNode ?
        (LoadSDNode) t : null;
    t = getBuildPairElt(n, 1);
    LoadSDNode ld2 = t instanceof LoadSDNode ?
        (LoadSDNode) t : null;
    if (ld1 == null || ld2 == null || !ld1.isNONExtLoad() ||
        !ld1.hasOneUse())
      return new SDValue();

    EVT ld1VT = ld1.getValueType(0);
    MachineFrameInfo mfi = dag.getMachineFunction().getFrameInfo();
    if (ld2.isNONExtLoad() && ld2.hasOneUse() &&
        !ld1.isVolatile() &&
        !ld2.isVolatile() &&
        tli.isConsecutiveLoad(ld2, ld1, ld1VT.getSizeInBits() / 8, 1, mfi)) {
      int align = ld1.getAlignment();
      int newAlign = tli.getTargetData().getABITypeAlignment(
          vt.getTypeForEVT(dag.getContext()));
      if (newAlign <= align &&
          (!legalOprations ||
              tli.isOperationLegal(ISD.LOAD, vt))) {
        return dag.getLoad(vt, ld1.getChain(),
            ld1.getBasePtr(),
            ld1.getSrcValue(),
            ld1.getSrcValueOffset(),
            false, align);
      }
    }
    return new SDValue();
  }

  private SDValue visitBIT_CONVERT(SDNode n) {
    return new SDValue();
  }

  private SDValue getDemandedBits(SDValue v, APInt mask) {
    switch (v.getOpcode()) {
      default:
        break;
      case ISD.OR:
      case ISD.XOR:
        if (dag.maskedValueIsZero(v.getOperand(0), mask))
          return v.getOperand(1);
        if (dag.maskedValueIsZero(v.getOperand(1), mask))
          return v.getOperand(0);
        break;
      case ISD.SRL:
        if (!v.hasOneUse())
          break;
        if (v.getOperand(1).getNode() instanceof ConstantSDNode) {
          long amt = ((ConstantSDNode) v.getOperand(1).getNode()).getZExtValue();
          if (amt >= mask.getBitWidth()) break;
          APInt newMask = mask.shl((int) amt);
          SDValue simplifyLHS = getDemandedBits(v.getOperand(0), newMask);
          if (simplifyLHS.getNode() != null)
            return dag.getNode(ISD.SRL, v.getValueType(), simplifyLHS,
                v.getOperand(1));
        }
    }
    return new SDValue();
  }

  private SDValue visitTRUNCATE(SDNode n) {
    SDValue n0 = n.getOperand(0);
    EVT vt = n.getValueType(0);

    if (n0.getValueType().equals(n.getValueType(0)))
      return n0;

    if (n0.getNode() instanceof ConstantSDNode)
      return dag.getNode(ISD.TRUNCATE, vt, n0);

    if (n0.getOpcode() == ISD.TRUNCATE)
      return dag.getNode(ISD.TRUNCATE, vt, n0.getOperand(0));

    int n0Opc = n0.getOpcode();
    if (n0Opc == ISD.ZERO_EXTEND || n0Opc == ISD.SIGN_EXTEND ||
        n0Opc == ISD.ANY_EXTEND) {
      if (n0.getOperand(0).getValueType().bitsLT(vt))
        return dag.getNode(n0.getOpcode(), vt, n0.getOperand(0));
      else if (n0.getOperand(0).getValueType().bitsGT(vt))
        return dag.getNode(ISD.TRUNCATE, vt, n0.getOperand(0));
      else
        return n0.getOperand(0);
    }

    SDValue shorter = getDemandedBits(n0, APInt.getLowBitsSet(n0.getValueSizeInBits(),
        vt.getSizeInBits()));
    if (shorter.getNode() != null)
      return dag.getNode(ISD.TRUNCATE, vt, shorter);

    return reduceLoadWidth(n);
  }

  /**
   * If the result of a wider load is shifted to right of N
   * bits and then truncated to a narrower type and where N is a multiple
   * of number of bits of the narrower type, transform it to a narrower load
   * from address + N / num of bits of new type. If the result is to be
   * extended, also fold the extension to form a extending load.
   *
   * @param n
   * @return
   */
  private SDValue reduceLoadWidth(SDNode n) {
    int opc = n.getOpcode();
    LoadExtType extType = LoadExtType.NON_EXTLOAD;
    SDValue n0 = n.getOperand(0);
    EVT vt = n.getValueType(0);
    EVT evt = vt;

    if (vt.isVector())
      return new SDValue();

    if (opc == ISD.SIGN_EXTEND_INREG) {
      extType = LoadExtType.SEXTLOAD;
      evt = ((VTSDNode) n.getOperand(1).getNode()).getVT();
      if (legalOprations && !tli.isLoadExtLegal(LoadExtType.SEXTLOAD, evt))
        return new SDValue();
    }

    int evtBits = evt.getSizeInBits();
    int shAmt = 0;
    if (n0.getOpcode() == ISD.SRL && n0.hasOneUse()) {
      if (n0.getOperand(1).getNode() instanceof ConstantSDNode) {
        ConstantSDNode c = (ConstantSDNode) n0.getOperand(1).getNode();
        shAmt = (int) c.getZExtValue();
        if ((shAmt & (evtBits - 1)) == 0) {
          n0 = n.getOperand(0);
          if ((n0.getValueType().getSizeInBits() & (evtBits - 1)) != 0)
            return new SDValue();
        }
      }
    }

    if (n0.getNode() instanceof LoadSDNode &&
        n0.hasOneUse() &&
        evt.isRound() &&
        ((LoadSDNode) n0.getNode()).getMemoryVT().getSizeInBits() > evtBits &&
        !((LoadSDNode) n0.getNode()).isVolatile()) {
      LoadSDNode ld = (LoadSDNode) n0.getNode();
      EVT ptrType = n0.getOperand(1).getValueType();

      if (tli.isBigEndian()) {
        int lvtStoreBits = ld.getMemoryVT().getStoreSizeInBits();
        int evtStoreBits = evt.getStoreSizeInBits();
        shAmt = lvtStoreBits - evtStoreBits - shAmt;
      }

      int ptrOff = shAmt / 8;
      int newAlign = Util.minAlign(ld.getAlignment(), ptrOff);
      SDValue newPtr = dag.getNode(ISD.ADD, ptrType, ld.getBasePtr(),
          dag.getConstant(ptrOff, ptrType, false));

      addToWorkList(newPtr.getNode());
      SDValue load = extType == LoadExtType.NON_EXTLOAD ?
          dag.getLoad(vt, ld.getChain(), newPtr,
              ld.getSrcValue(), ld.getSrcValueOffset() + ptrOff,
              ld.isVolatile(), ld.getAlignment())
          : dag.getExtLoad(extType, vt, ld.getChain(),
          newPtr, ld.getSrcValue(), ld.getSrcValueOffset() + ptrOff,
          evt, ld.isVolatile(), ld.getAlignment());

      WorklistRemover remover = new WorklistRemover(this);
      dag.replaceAllUsesOfValueWith(n0.getValue(1), load.getValue(1),
          remover);
      return load;
    }
    return new SDValue();
  }

  private SDValue visitSIGN_EXTEND_INREG(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    EVT destVT = n.getValueType(0);
    EVT srcVT = ((VTSDNode) n1.getNode()).getVT();
    int destVTBits = destVT.getSizeInBits();
    int srcVTBits = srcVT.getSizeInBits();

    if (n0.getNode() instanceof ConstantSDNode || n0.getOpcode() == ISD.UNDEF)
      return dag.getNode(ISD.SIGN_EXTEND_INREG, destVT, n0, n1);

    if (dag.computeNumSignBits(n0) >= destVTBits - srcVTBits + 1)
      return n0;

    // fold (sext_in_reg (sext x)) -> (sext x)
    // fold (sext_in_reg (aext x)) -> (sext x)
    // if x is small enough.
    if (n0.getOpcode() == ISD.SIGN_EXTEND || n0.getOpcode() == ISD.ANY_EXTEND) {
      SDValue n00 = n0.getOperand(0);
      if (n00.getValueType().getSizeInBits() < destVTBits)
        return dag.getNode(ISD.SIGN_EXTEND, destVT, n00, n1);
    }

    // fold (sext_in_reg x) -> (zext_in_reg x) if the sign bit is known zero.
    if (dag.maskedValueIsZero(n0, APInt.getBitsSet(destVTBits, srcVTBits - 1, srcVTBits)))
      return dag.getZeroExtendInReg(n0, srcVT);

    // fold operands of sext_in_reg based on knowledge that the top bits are not
    // demanded.
    if (simplifyDemandedBits(new SDValue(n, 0)))
      return new SDValue(n, 0);

    // fold (sext_in_reg (load x)) -> (smaller sextload x)
    // fold (sext_in_reg (srl (load x), c)) -> (smaller sextload (x+c/evtbits))
    SDValue narrowLoad = reduceLoadWidth(n);
    if (narrowLoad.getNode() != null)
      return narrowLoad;

    // fold (sext_in_reg (srl X, 24), i8) -> (sra X, 24)
    // fold (sext_in_reg (srl X, 23), i8) -> (sra X, 23) iff possible.
    // We already fold "(sext_in_reg (srl X, 25), i8) -> srl X, 25" above.
    if (n0.getOpcode() == ISD.SRL) {
      if (n0.getOperand(1).getNode() instanceof ConstantSDNode) {
        ConstantSDNode shAmt = (ConstantSDNode) n0.getOperand(1).getNode();
        if (shAmt.getZExtValue() + srcVTBits <= destVTBits) {
          int inSignBits = dag.computeNumSignBits(n0.getOperand(0));
          if (destVTBits - shAmt.getZExtValue() - srcVTBits < inSignBits)
            return dag.getNode(ISD.SRA, destVT, n0.getOperand(0),
                n0.getOperand(1));
        }
      }
    }

    // fold (sext_inreg (extload x)) -> (sextload x) iff load has only one use
    if (n0.getNode().isExtLoad() && n0.getNode().isUNINDEXEDLoad() &&
        n0.getNode().hasOneUse() &&
        srcVT.equals(((LoadSDNode) n0.getNode()).getMemoryVT()) &&
        ((!legalOprations && !((LoadSDNode) n0.getNode()).isVolatile()) ||
        tli.isLoadExtLegal(LoadExtType.SEXTLOAD, srcVT))) {
      LoadSDNode ld = (LoadSDNode) n0.getNode();
      SDValue extLoad = dag.getExtLoad(LoadExtType.SEXTLOAD, destVT,
          ld.getChain(), ld.getBasePtr(), ld.getSrcValue(),
          ld.getSrcValueOffset(), srcVT, ld.isVolatile(),
          ld.getAlignment());
      combineTo(n, extLoad, true);
      combineTo(n0.getNode(), extLoad, extLoad.getValue(1));
      return new SDValue(n, 0);
    }

    // fold (sext_inreg (zextload x)) -> (sextload x) iff load has one use
    if (n0.getNode().isZEXTLoad() && n0.getNode().isUNINDEXEDLoad() &&
        n0.hasOneUse() && srcVT.equals(((LoadSDNode) n0.getNode()).getMemoryVT()) &&
        ((!legalOprations && !((LoadSDNode) n0.getNode()).isVolatile()) ||
            tli.isLoadExtLegal(LoadExtType.SEXTLOAD, srcVT))) {
      LoadSDNode ld = (LoadSDNode) n0.getNode();
      SDValue extLoad = dag.getExtLoad(LoadExtType.SEXTLOAD, destVT,
          ld.getChain(), ld.getBasePtr(), ld.getSrcValue(),
          ld.getSrcValueOffset(), srcVT, ld.isVolatile(),
          ld.getAlignment());
      combineTo(n, extLoad, true);
      combineTo(n0.getNode(), extLoad, extLoad.getValue(1));
      return new SDValue(n, 0);
    }
    return new SDValue();
  }

  private SDValue visitANY_EXTEND(SDNode n) {
    SDValue n0 = n.getOperand(0);
    EVT vt = n.getValueType(0);

    // fold (aext c1) -> c1
    if (n0.getNode() instanceof ConstantSDNode)
      return dag.getNode(ISD.ANY_EXTEND, vt, n0);

    // fold (aext (aext x)) -> (aext x)
    // fold (aext (zext x)) -> (zext x)
    // fold (aext (sext x)) -> (sext x)
    if (n0.getOpcode() == ISD.ZERO_EXTEND ||
        n0.getOpcode() == ISD.ANY_EXTEND ||
        n0.getOpcode() == ISD.SIGN_EXTEND)
      return dag.getNode(n0.getOpcode(), vt, n0.getOperand(0));

    // fold (aext (truncate (load x))) -> (aext (smaller load x))
    // fold (aext (truncate (srl (load x), c))) -> (aext (small load (x+c/n)))
    if (n0.getOpcode() == ISD.TRUNCATE) {
      SDValue narrowLoad = reduceLoadWidth(n0.getNode());
      if (narrowLoad.getNode() != null) {
        if (!narrowLoad.getNode().equals(n0.getNode()))
          combineTo(n0.getNode(), narrowLoad, true);
        return dag.getNode(ISD.ANY_EXTEND, vt, narrowLoad);
      }
    }

    // fold (aext (truncate x))
    if (n0.getOpcode() == ISD.TRUNCATE) {
      SDValue op = n0.getOperand(0);
      if (op.getValueType().equals(vt))
        return op;
      if (op.getValueType().bitsGT(vt))
        return dag.getNode(ISD.TRUNCATE, vt, op);
      return dag.getNode(ISD.ANY_EXTEND, vt, op);
    }

    // Fold (aext (and (trunc x), cst)) -> (and x, cst)
    // if the trunc is not free.
    if (n0.getOpcode() == ISD.AND &&
        n0.getOperand(0).getOpcode() == ISD.TRUNCATE &&
        n0.getOperand(1).getOpcode() == ISD.Constant &&
        !tli.isTruncateFree(n0.getOperand(0).getOperand(0).getValueType(),
            n0.getValueType())) {
      SDValue x = n0.getOperand(0).getOperand(0);
      if (x.getValueType().bitsLT(vt))
        x = dag.getNode(ISD.ANY_EXTEND, vt, x);
      else if (x.getValueType().bitsGT(vt))
        x = dag.getNode(ISD.TRUNCATE, vt, x);

      APInt mask = ((ConstantSDNode) n0.getOperand(1).getNode()).getAPIntValue();
      mask = mask.zext(vt.getSizeInBits());
      return dag.getNode(ISD.AND, vt, x, dag.getConstant(mask, vt, false));
    }

    // fold (aext (load x)) -> (aext (truncate (extload x)))
    if (n0.getNode().isNONExtLoad() && ((!legalOprations &&
        ((LoadSDNode) n0.getNode()).isVolatile()) ||
        tli.isLoadExtLegal(LoadExtType.EXTLOAD, n0.getValueType()))) {
      boolean doXform = true;
      ArrayList<SDNode> setccs = new ArrayList<>();
      if (!n0.hasOneUse())
        doXform = extendUsesToFormExtLoad(n, n0, ISD.ANY_EXTEND, setccs);
      if (doXform) {
        LoadSDNode ld = (LoadSDNode) n0.getNode();
        SDValue extLoad = dag.getExtLoad(LoadExtType.EXTLOAD, vt,
            ld.getChain(), ld.getBasePtr(), ld.getSrcValue(),
            ld.getSrcValueOffset(), n0.getValueType(),
            ld.isVolatile(), ld.getAlignment());

        combineTo(n, extLoad, true);
        SDValue trunc = dag.getNode(ISD.TRUNCATE, n0.getValueType(), extLoad);
        combineTo(n0.getNode(), trunc, extLoad.getValue(1));
        for (SDNode cc : setccs) {
          ArrayList<SDValue> ops = new ArrayList<>();
          for (int j = 0; j < 2; j++) {
            SDValue sop = cc.getOperand(j);
            if (sop.equals(trunc))
              ops.add(extLoad);
            else
              ops.add(dag.getNode(ISD.ANY_EXTEND, vt, sop));
          }
          ops.add(cc.getOperand(2));
          combineTo(cc, dag.getNode(ISD.SETCC, cc.getValueType(0),
              ops), true);
        }
        return new SDValue(n, 0);
      }
    }

    // fold (aext (zextload x)) -> (aext (truncate (zextload x)))
    // fold (aext (sextload x)) -> (aext (truncate (sextload x)))
    // fold (aext ( extload x)) -> (aext (truncate (extload  x)))
    if (n0.getOpcode() == ISD.LOAD & !n0.getNode().isNONExtLoad() &&
        n0.getNode().isUNINDEXEDLoad() && n0.hasOneUse()) {
      LoadSDNode ld = (LoadSDNode) n0.getNode();
      EVT evt = ld.getMemoryVT();

      SDValue extLoad = dag.getExtLoad(LoadExtType.EXTLOAD, vt,
          ld.getChain(), ld.getBasePtr(), ld.getSrcValue(),
          ld.getSrcValueOffset(), evt,
          ld.isVolatile(), ld.getAlignment());

      combineTo(n, extLoad, true);
      SDValue trunc = dag.getNode(ISD.TRUNCATE, n0.getValueType(), extLoad);
      combineTo(n0.getNode(), trunc, extLoad.getValue(1));
      return new SDValue(n, 0);
    }

    // aext(setcc x,y,cc) -> select_cc x, y, 1, 0, cc
    if (n0.getOpcode() == ISD.SETCC) {
      SDValue scc = simplifySelectCC(n0.getOperand(0), n0.getOperand(1),
          dag.getConstant(1, vt, false),
          dag.getConstant(0, vt, false),
          ((CondCodeSDNode) n0.getOperand(2).getNode()).getCondition(), true);
      if (scc.getNode() != null)
        return scc;
    }

    return new SDValue();
  }

  private SDValue visitZERO_EXTEND(SDNode n) {
    SDValue n0 = n.getOperand(0);
    EVT vt = n.getValueType(0);

    // fold (zext c1) -> c1
    if (n0.getNode() instanceof ConstantSDNode)
      return dag.getNode(ISD.ZERO_EXTEND, vt, n0);

    // fold (zext (zext x)) -> (zext x)
    // fold (zext (aext x)) -> (zext x)
    if (n0.getOpcode() == ISD.ZERO_EXTEND ||
        n0.getOpcode() == ISD.ANY_EXTEND)
      return dag.getNode(ISD.ZERO_EXTEND, vt, n0.getOperand(0));

    // fold (zext (truncate (load x))) -> (zext (smaller load x))
    // fold (zext (truncate (srl (load x), c))) -> (zext (small load (x+c/n)))
    if (n0.getOpcode() == ISD.TRUNCATE) {
      SDValue narrowLoad = reduceLoadWidth(n0.getNode());
      if (narrowLoad.getNode() != null) {
        if (!narrowLoad.getNode().equals(n0.getNode()))
          combineTo(n0.getNode(), narrowLoad, true);
        return dag.getNode(ISD.ZERO_EXTEND, vt, narrowLoad);
      }
    }
    // fold (zext (truncate x)) -> (and x, mask)
    if (n0.getOpcode() == ISD.TRUNCATE &
        (!legalOprations || tli.isOperationLegal(ISD.AND, vt))) {
      SDValue op = n0.getOperand(0);
      if (vt.bitsGT(op.getValueType()))
        op = dag.getNode(ISD.ANY_EXTEND, vt, op);
      else if (op.getValueType().bitsGT(vt))
        op = dag.getNode(ISD.TRUNCATE, vt, op);
      return dag.getZeroExtendInReg(op, n0.getValueType());
    }

    // Fold (zext (and (trunc x), cst)) -> (and x, cst),
    // if either of the casts is not free.
    if (n0.getOpcode() == ISD.AND &&
        n0.getOperand(0).getOpcode() == ISD.TRUNCATE &&
        n0.getOperand(1).getOpcode() == ISD.Constant &&
        (!tli.isTruncateFree(n0.getOperand(0).getOperand(0).getValueType(),
            n0.getValueType()) ||
            !tli.isZExtFree(n0.getValueType(), vt))) {
      SDValue x = n0.getOperand(0).getOperand(0);
      if (x.getValueType().bitsLT(vt))
        x = dag.getNode(ISD.ANY_EXTEND, vt, x);
      else if (x.getValueType().bitsGT(vt))
        x = dag.getNode(ISD.TRUNCATE, vt, x);
      APInt mask = ((ConstantSDNode) n0.getOperand(1).getNode()).getAPIntValue();
      mask = mask.zext(vt.getSizeInBits());
      return dag.getNode(ISD.AND, vt, x, dag.getConstant(mask, vt, false));
    }

    // fold (zext (load x)) -> (zext (truncate (zextload x)))
    if (n0.getNode().isNONExtLoad() && ((!legalOprations &&
        ((LoadSDNode) n0.getNode()).isVolatile()) ||
        tli.isLoadExtLegal(LoadExtType.ZEXTLOAD, n0.getValueType()))) {
      boolean doXform = true;
      ArrayList<SDNode> setccs = new ArrayList<>();
      if (!n0.hasOneUse())
        doXform = extendUsesToFormExtLoad(n, n0, ISD.ZERO_EXTEND, setccs);
      if (doXform) {
        LoadSDNode ld = (LoadSDNode) n0.getNode();
        SDValue extLoad = dag.getExtLoad(LoadExtType.ZEXTLOAD, vt,
            ld.getChain(), ld.getBasePtr(), ld.getSrcValue(),
            ld.getSrcValueOffset(), n0.getValueType(),
            ld.isVolatile(), ld.getAlignment());
        combineTo(n, extLoad, true);
        SDValue trunc = dag.getNode(ISD.TRUNCATE, n0.getValueType(), extLoad);
        combineTo(n0.getNode(), trunc, extLoad.getValue(1));
        for (SDNode cc : setccs) {
          ArrayList<SDValue> ops = new ArrayList<>();
          for (int j = 0; j < 2; j++) {
            SDValue sop = cc.getOperand(j);
            if (sop.equals(trunc))
              ops.add(extLoad);
            else
              ops.add(dag.getNode(ISD.ZERO_EXTEND, vt, sop));
          }
          ops.add(cc.getOperand(2));
          combineTo(cc, dag.getNode(ISD.SETCC, cc.getValueType(0),
              ops), true);
        }
        return new SDValue(n, 0);
      }
    }

    // fold (zext (zextload x)) -> (zext (truncate (zextload x)))
    // fold (zext ( extload x)) -> (zext (truncate (zextload x)))
    if ((n0.getNode().isZEXTLoad() || n0.getNode().isExtLoad()) &&
        n0.getNode().isUNINDEXEDLoad() && n0.hasOneUse()) {
      LoadSDNode ld = (LoadSDNode) n0.getNode();
      EVT evt = ld.getMemoryVT();
      if ((!legalOprations && !ld.isVolatile()) ||
          tli.isLoadExtLegal(LoadExtType.ZEXTLOAD, evt)) {
        SDValue extLoad = dag.getExtLoad(LoadExtType.ZEXTLOAD, vt,
            ld.getChain(), ld.getBasePtr(), ld.getSrcValue(),
            ld.getSrcValueOffset(), evt, ld.isVolatile(),
            ld.getAlignment());
        combineTo(n, extLoad, true);
        combineTo(n0.getNode(), dag.getNode(ISD.TRUNCATE,
            n0.getValueType(), extLoad), extLoad.getValue(1));
        return new SDValue(n, 0);
      }
    }

    // zext(setcc x,y,cc) -> select_cc x, y, 1, 0, cc
    if (n0.getOpcode() == ISD.SETCC) {
      SDValue scc = simplifySelectCC(n0.getOperand(0), n0.getOperand(1),
          dag.getConstant(1, vt, false),
          dag.getConstant(0, vt, false),
          ((CondCodeSDNode) n0.getOperand(2).getNode()).getCondition(), true);
      if (scc.getNode() != null)
        return scc;
    }

    return new SDValue();
  }

  private SDValue visitSIGN_EXTEND(SDNode n) {
    SDValue n0 = n.getOperand(0);
    EVT vt = n.getValueType(0);

    if (n0.getNode() instanceof ConstantSDNode)
      return dag.getNode(ISD.SIGN_EXTEND, vt, n0);

    if (n0.getOpcode() == ISD.SIGN_EXTEND ||
        n0.getOpcode() == ISD.ANY_EXTEND)
      return dag.getNode(ISD.SIGN_EXTEND, vt, n0.getOperand(0));

    if (n0.getOpcode() == ISD.TRUNCATE) {
      // fold (sext (truncate (load x))) -> (sext (smaller load x))
      // fold (sext (truncate (srl (load x), c))) -> (sext (smaller load (x+c/n)))
      SDValue narrowLoad = reduceLoadWidth(n0.getNode());
      if (narrowLoad.getNode() != null) {
        if (!narrowLoad.getNode().equals(n0.getNode()))
          combineTo(n0.getNode(), narrowLoad, true);
        return new SDValue(n, 0);
      }

      // See if the value being truncated is already sign extended.  If so, just
      // eliminate the trunc/sext pair.
      SDValue op = n0.getOperand(0);
      int opBits = op.getValueSizeInBits();
      int midBits = n0.getValueSizeInBits();
      int destBits = vt.getSizeInBits();
      int numSignBits = dag.computeNumSignBits(op);
      if (opBits == destBits) {
        // Op is i32, Mid is i8, and Dest is i32.  If Op has more than 24 sign
        // bits, it is already ready.
        if (numSignBits > destBits - midBits)
          return op;
      } else if (opBits < destBits) {
        // Op is i32, Mid is i8, and Dest is i64.  If Op has more than 24 sign
        // bits, just sext from i32.
        if (numSignBits > opBits - midBits)
          return dag.getNode(ISD.SIGN_EXTEND, vt, op);
      } else {
        if (numSignBits > opBits - midBits)
          return dag.getNode(ISD.TRUNCATE, vt, op);
      }

      // fold (sext (truncate x)) -> (sextinreg x).
      if (!legalOprations || tli.isOperationLegal(ISD.SIGN_EXTEND_INREG,
          n0.getValueType())) {
        if (op.getValueType().bitsLT(vt))
          op = dag.getNode(ISD.ANY_EXTEND, vt, op);
        else if (op.getValueType().bitsGT(vt))
          op = dag.getNode(ISD.TRUNCATE, vt, op);
        return dag.getNode(ISD.SIGN_EXTEND_INREG, vt, op,
            dag.getValueType(n0.getValueType()));
      }
    }

    // fold (sext (load x)) -> (sext (truncate (sextload x)))
    if (n0.getNode().isNONExtLoad() && ((!legalOprations &&
        ((LoadSDNode) n0.getNode()).isVolatile()) ||
        tli.isLoadExtLegal(LoadExtType.SEXTLOAD, n0.getValueType()))) {
      boolean doXform = true;
      ArrayList<SDNode> setccs = new ArrayList<>();
      if (!n0.hasOneUse())
        doXform = extendUsesToFormExtLoad(n, n0, ISD.SIGN_EXTEND, setccs);
      if (doXform) {
        LoadSDNode ld = (LoadSDNode) n0.getNode();
        SDValue extLoad = dag.getExtLoad(LoadExtType.SEXTLOAD, vt,
            ld.getChain(), ld.getBasePtr(), ld.getSrcValue(),
            ld.getSrcValueOffset(), n0.getValueType(),
            ld.isVolatile(), ld.getAlignment());
        combineTo(n, extLoad, true);
        SDValue trunc = dag.getNode(ISD.TRUNCATE, n0.getValueType(), extLoad);
        combineTo(n0.getNode(), trunc, extLoad.getValue(1));
        for (SDNode cc : setccs) {
          ArrayList<SDValue> ops = new ArrayList<>();
          for (int j = 0; j < 2; j++) {
            SDValue sop = cc.getOperand(j);
            if (sop.equals(trunc))
              ops.add(extLoad);
            else
              ops.add(dag.getNode(ISD.SIGN_EXTEND, vt, sop));
          }
          ops.add(cc.getOperand(2));
          combineTo(cc, dag.getNode(ISD.SETCC, cc.getValueType(0),
              ops), true);
        }
        return new SDValue(n, 0);
      }
    }

    if ((n0.getNode().isSEXTLoad() || n0.getNode().isExtLoad()) &&
        n0.getNode().isUNINDEXEDLoad() && n0.hasOneUse()) {
      LoadSDNode ld = (LoadSDNode) n0.getNode();
      EVT evt = ld.getMemoryVT();
      if ((!legalOprations && !ld.isVolatile()) ||
          tli.isLoadExtLegal(LoadExtType.SEXTLOAD, evt)) {
        SDValue extLoad = dag.getExtLoad(LoadExtType.SEXTLOAD, vt,
            ld.getChain(), ld.getBasePtr(), ld.getSrcValue(),
            ld.getSrcValueOffset(), evt, ld.isVolatile(),
            ld.getAlignment());
        combineTo(n, extLoad, true);
        combineTo(n0.getNode(), dag.getNode(ISD.TRUNCATE,
            n0.getValueType(), extLoad), extLoad.getValue(1));
        return new SDValue(n, 0);
      }
    }

    if (n0.getOpcode() == ISD.SETCC) {
      // sext(setcc) -> sext_in_reg(vsetcc) for vectors.
      if (vt.isVector() && vt.getSizeInBits() == n0.getOperand(0).getValueSizeInBits() &&
          !legalOprations) {
        return dag.getVSetCC(vt, n0.getOperand(0), n0.getOperand(1),
            ((CondCodeSDNode) n0.getOperand(2).getNode()).getCondition());
      }

      SDValue negOne = dag.getConstant(APInt.getAllOnesValue(vt.getSizeInBits()), vt, false);
      SDValue scc = simplifySelectCC(n0.getOperand(0), n0.getOperand(1),
          negOne, dag.getConstant(0, vt, false),
          ((CondCodeSDNode) n0.getOperand(2).getNode()).getCondition(), true);
      if (scc.getNode() != null)
        return scc;

      EVT setCCTy = new EVT(tli.getSetCCResultType(vt));
      if (!legalOprations || tli.isOperationLegal(ISD.SETCC, setCCTy)) {
        return dag.getNode(ISD.SELECT, vt,
            dag.getSetCC(setCCTy, n0.getOperand(0),
                n0.getOperand(1), ((CondCodeSDNode)n0.getOperand(2).getNode()).getCondition()),
            negOne, dag.getConstant(0, vt, false));
      }
    }

    // fold (sext x) -> (zext x) if the sign bit is known zero.
    if ((!legalOprations || tli.isOperationLegal(ISD.ZERO_EXTEND, vt)) &&
        dag.signBitIsZero(n0))
      return dag.getNode(ISD.ZERO_EXTEND, vt, n0);

    return new SDValue();
  }

  private boolean extendUsesToFormExtLoad(SDNode n,
                                          SDValue n0,
                                          int extOpc,
                                          ArrayList<SDNode> extendNodes) {
    boolean hasCopyToRegUses = false;
    boolean isTruncFree = tli.isTruncateFree(n.getValueType(0), n0.getValueType());
    for (SDUse u : n0.getNode().getUseList()) {
      SDNode user = u.getUser();
      if (user.equals(n)) continue;
      if (u.getResNo() != n0.getResNo()) continue;
      if (extOpc != ISD.ANY_EXTEND && user.getOpcode() == ISD.SETCC) {
        CondCode cc = ((CondCodeSDNode) user.getOperand(2).getNode()).getCondition();
        if (extOpc == ISD.ZERO_EXTEND && cc.isSignedIntSetCC())
          return false;

        boolean add = false;
        for (int i = 0; i < 2; i++) {
          SDValue useOp = user.getOperand(i);
          if (useOp.equals(n0))
            continue;
          if ((useOp.getNode() instanceof ConstantSDNode))
            return false;
          add = true;
        }
        if (add)
          extendNodes.add(user);
        continue;
      }

      if (!isTruncFree)
        return false;
      if (user.getOpcode() == ISD.CopyToReg)
        hasCopyToRegUses = true;
    }

    if (hasCopyToRegUses) {
      boolean bothLiveOut = false;
      for (SDUse u : n.getUseList()) {
        if (u.getResNo() == 0 && u.getUser().getOpcode() == ISD.CopyToReg) {
          bothLiveOut = true;
          break;
        }
      }
      if (bothLiveOut)
        return extendNodes.size() != 0;
    }
    return true;
  }

  private SDValue simplifySetCC(EVT vt, SDValue op0,
                                SDValue op1, CondCode cond) {
    return simplifySetCC(vt, op0, op1, cond, true);
  }

  private SDValue simplifySetCC(EVT vt,
                                SDValue op0,
                                SDValue op1,
                                CondCode cond,
                                boolean foldBooleans) {
    DAGCombinerInfo dci = new DAGCombinerInfo(dag, !legalTypes,
        !legalOprations, false, this);
    return tli.simplifySetCC(vt, op0, op1, cond, foldBooleans, dci);
  }

  private SDValue visitSETCC(SDNode n) {
    return simplifySetCC(n.getValueType(0), n.getOperand(0), n.getOperand(1),
        ((CondCodeSDNode) n.getOperand(2).getNode()).getCondition());
  }

  private SDValue visitSELECT_CC(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    SDValue n2 = n.getOperand(2);
    SDValue n3 = n.getOperand(3);
    SDValue n4 = n.getOperand(4);
    CondCode cc = ((CondCodeSDNode) n4.getNode()).getCondition();

    if (n2.equals(n3)) return n2;
    SDValue scc = simplifySetCC(
        new EVT(tli.getSetCCResultType(n0.getValueType())),
        n0, n1, cc, false);
    if (scc.getNode() != null)
      addToWorkList(scc.getNode());

    if (scc.getNode() instanceof ConstantSDNode) {
      ConstantSDNode ssd = (ConstantSDNode) scc.getNode();
      if (!ssd.isNullValue())
        return n2;
      return n3;
    }

    if (scc.getNode() != null && scc.getOpcode() == ISD.SETCC) {
      return dag.getNode(ISD.SELECT_CC, n2.getValueType(),
          scc.getOperand(0), scc.getOperand(1), n2, n3,
          scc.getOperand(2));
    }
    if (simplifySelectOps(n, n2, n3))
      return new SDValue(n, 0);
    return simplifySelectCC(n0, n1, n2, n3, cc);
  }

  private SDValue visitSELECT(SDNode n) {
    SDValue n0 = n.getOperand(0); // condition
    SDValue n1 = n.getOperand(1); // lhs
    SDValue n2 = n.getOperand(2); // rhs
    ConstantSDNode c0 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c1 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    ConstantSDNode c2 = n2.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n2.getNode() : null;
    EVT vt = n.getValueType(0);
    EVT vt0 = n0.getValueType();

    // fold (select C, X, X) -> X
    if (n1.equals(n2)) return n1;

    // fold (select true, X, Y) -> X
    if (n0.getNode() instanceof ConstantSDNode &&
        !((ConstantSDNode) n0.getNode()).isNullValue()) {
      return n1;
    }

    // fold (select false, X, Y) -> Y
    if (n0.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) n0.getNode()).isNullValue()) {
      return n2;
    }
    // fold (select C, 1, X) -> (or C, X)
    if (n0.getNode() instanceof ConstantSDNode &&
        n1.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) n1.getNode()).getAPIntValue().eq(1)) {
      return dag.getNode(ISD.OR, vt, n0, n2);
    }
    // fold (select C, 0, 1) -> (xor C, 1)
    if (n0.getNode() instanceof ConstantSDNode &&
        n1.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) n1.getNode()).getAPIntValue().eq(0) &&
        n2.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) n2.getNode()).getAPIntValue().eq(1)) {
      return dag.getNode(ISD.XOR, vt, n0, n2);
    }
    // fold (select C, 0, X) -> (and (not C), X)
    if (n0.getNode() instanceof ConstantSDNode &&
        n1.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) n1.getNode()).getAPIntValue().eq(0)) {
      APInt cc = ((ConstantSDNode) n0.getNode()).getAPIntValue();
      return dag.getNode(ISD.AND, vt, dag.getConstant(cc.not(), vt0, false), n2);
    }
    // fold (select C, X, 1) -> (or (not C), X)
    if (n0.getNode() instanceof ConstantSDNode &&
        n2.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) n2.getNode()).getAPIntValue().eq(1)) {
      APInt cc = ((ConstantSDNode) n0.getNode()).getAPIntValue();
      return dag.getNode(ISD.OR, vt, dag.getConstant(cc.not(), vt0, false), n1);
    }
    // fold (select C, X, 0) -> (and C, X)
    if (n0.getNode() instanceof ConstantSDNode &&
        n2.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) n2.getNode()).getAPIntValue().eq(0)) {
      return dag.getNode(ISD.AND, vt, n0, n1);
    }

    // fold (select X, X, Y) -> (or X, Y)
    // fold (select X, 1, Y) -> (or X, Y)
    if (vt.equals(new EVT(MVT.i1)) && (n0.equals(n1) ||
        (n1.getNode() instanceof ConstantSDNode &&
        ((ConstantSDNode) n1.getNode()).getAPIntValue().eq(1)))) {
      return dag.getNode(ISD.OR, vt, n0, n2);
    }

    // fold (select X, Y, X) -> (and X, Y)
    // fold (select X, Y, 0) -> (and X, Y)
    if (vt.equals(new EVT(MVT.i1)) && (n0.equals(n2) ||
        (n2.getNode() instanceof ConstantSDNode &&
            ((ConstantSDNode) n2.getNode()).isNullValue()))) {
      return dag.getNode(ISD.AND, vt, n0, n1);
    }

    // If we can fold this based on the true/false value, do so.
    if (simplifySelectOps(n, n1, n2))
      return new SDValue(n, 0);

    // fold selects based on a setcc into other things, such as min/max/abs
    if (n0.getOpcode() == ISD.SETCC) {
      if (tli.isOperationLegalOrCustom(ISD.SELECT_CC, new EVT(MVT.Other)) &&
          tli.isOperationLegalOrCustom(ISD.SELECT, vt)) {
        return dag.getNode(ISD.SELECT_CC, vt,
            n0.getOperand(0),
            n0.getOperand(1),
            n1, n2, n0.getOperand(2));
      }
      return simplifySelect(n0, n1, n2);
    }
    return new SDValue();
  }

  private boolean simplifySelectOps(SDNode sel, SDValue lhs, SDValue rhs) {
    if (lhs.getOpcode() == rhs.getOpcode() &&
        lhs.hasOneUse() && rhs.hasOneUse()) {
      if (lhs.getOpcode() == ISD.LOAD &&
          !((LoadSDNode) lhs.getNode()).isVolatile() &&
          !((LoadSDNode) rhs.getNode()).isVolatile() &&
          lhs.getOperand(0).equals(rhs.getOperand(0))) {
        LoadSDNode ld1 = (LoadSDNode) lhs.getNode();
        LoadSDNode ld2 = (LoadSDNode) rhs.getNode();
        if (ld1.getMemoryVT().equals(ld2.getMemoryVT())) {
          SDValue addr = new SDValue();
          if (sel.getOpcode() == ISD.SELECT) {
            if (!ld1.isPredecessorOf(sel.getOperand(0).getNode()) &&
                !ld2.isPredecessorOf(sel.getOperand(0).getNode())) {
              addr = dag.getNode(ISD.SELECT,
                  ld1.getBasePtr().getValueType(),
                  sel.getOperand(0),
                  ld1.getBasePtr(),
                  ld2.getBasePtr());
            }
          } else {
            if (!ld1.isPredecessorOf(sel.getOperand(0).getNode()) &&
                !ld2.isPredecessorOf(sel.getOperand(0).getNode()) &&
                !ld1.isPredecessorOf(sel.getOperand(1).getNode()) &&
                !ld2.isPredecessorOf(sel.getOperand(1).getNode())) {
              addr = dag.getNode(ISD.SELECT_CC,
                  ld1.getBasePtr().getValueType(),
                  sel.getOperand(0), sel.getOperand(1),
                  ld1.getBasePtr(), ld2.getBasePtr(),
                  sel.getOperand(4));
            }
          }

          if (addr.getNode() != null) {
            SDValue load = new SDValue();
            if (ld1.getExtensionType() == LoadExtType.NON_EXTLOAD) {
              load = dag.getLoad(sel.getValueType(0),
                  ld1.getChain(), addr, ld1.getSrcValue(),
                  ld1.getSrcValueOffset(), ld1.isVolatile(),
                  ld1.getAlignment());
            } else {
              load = dag.getExtLoad(ld1.getExtensionType(),
                  sel.getValueType(0),
                  ld1.getChain(),
                  addr,
                  ld1.getSrcValue(),
                  ld1.getSrcValueOffset(),
                  ld1.getMemoryVT(),
                  ld1.isVolatile(),
                  ld1.getAlignment());
            }
            combineTo(sel, load, true);
            combineTo(lhs.getNode(), load.getValue(0), load.getValue(1), true);
            combineTo(rhs.getNode(), load.getValue(0), load.getValue(1), true);
          }
        }
      }
    }
    return false;
  }

  private SDValue simplifySelect(SDValue n0, SDValue n1, SDValue n2) {
    Util.assertion(n0.getOpcode() == ISD.SETCC, "First argument must be a SetCC node!");

    SDValue scc = simplifySelectCC(n0.getOperand(0),
        n0.getOperand(1), n1, n2,
        ((CondCodeSDNode) n0.getOperand(2).getNode()).getCondition());
    if (scc.getNode() != null) {
      if (scc.getOpcode() == ISD.SELECT_CC) {
        SDValue setcc = dag.getNode(ISD.SETCC, n0.getValueType(),
            scc.getOperand(0), scc.getOperand(1),
            scc.getOperand(4));
        addToWorkList(setcc.getNode());
        return dag.getNode(ISD.SELECT, scc.getValueType(),
            scc.getOperand(2), scc.getOperand(3), setcc);
      }
      return new SDValue();
    }
    return new SDValue();
  }

  private SDValue simplifySelectCC(SDValue n0, SDValue n1, SDValue n2, SDValue n3,
                                   CondCode cc) {
    return simplifySelectCC(n0, n1, n2, n3, cc, false);
  }

  private SDValue simplifySelectCC(SDValue n0, SDValue n1, SDValue n2, SDValue n3,
                                   CondCode cc, boolean notExtCompare) {
    if (n2.equals(n3)) return n2;
    EVT vt = n2.getValueType();
    ConstantSDNode c1 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    ConstantSDNode c2 = n2.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n2.getNode() : null;
    ConstantSDNode c3 = n3.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n3.getNode() : null;
    SDValue scc = simplifySetCC(
        new EVT(tli.getSetCCResultType(n0.getValueType())),
        n0, n1, cc, false);
    if (scc.getNode() != null)
      addToWorkList(scc.getNode());
    ConstantSDNode sccc = scc.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) scc.getNode() : null;

    if (sccc != null && !sccc.isNullValue())
      return n2;
    if (sccc != null && sccc.isNullValue())
      return n3;

    if (n1.getNode() instanceof ConstantFPSDNode) {
      ConstantFPSDNode fps = (ConstantFPSDNode) n1.getNode();
      // Allow either -0.0 or 0.0
      if (fps.getValueAPF().isZero()) {
        if ((cc == CondCode.SETGE || cc == CondCode.SETGT) &&
            n0.equals(n2) && n3.getOpcode() == ISD.FNEG &&
            n2.equals(n3.getOperand(0))) {
          return dag.getNode(ISD.FABS, vt, n0);
        }

        if ((cc == CondCode.SETLT || cc == CondCode.SETLE) &&
            n0.equals(n3) && n2.getOpcode() == ISD.FNEG &&
            n2.getOperand(0).equals(n3)) {
          return dag.getNode(ISD.FABS, vt, n3);
        }
      }
    }

    if (n2.getNode() instanceof ConstantFPSDNode) {
      ConstantFPSDNode tv = (ConstantFPSDNode) n2.getNode();
      if (n3.getNode() instanceof ConstantFPSDNode) {
        ConstantFPSDNode fv = (ConstantFPSDNode) n3.getNode();
        if (tli.isTypeLegal(n2.getValueType()) &&
            (tli.getOperationAction(ISD.ConstantFP, n2.getValueType()) !=
                TargetLowering.LegalizeAction.Legal) &&
            (tv.hasOneUse() || fv.hasOneUse())) {
          Constant[] elts = {
              fv.getConstantFPValue(),
              tv.getConstantFPValue()
          };
          Type fpTy = elts[0].getType();
          TargetData td = tli.getTargetData();

          Constant ca = ConstantArray.get(ArrayType.get(fpTy, 2), elts);
          SDValue cpIdx = dag.getConstantPool(ca, new EVT(tli.getPointerTy()),
              td.getPrefTypeAlignment(fpTy), 0, false, 0);
          int alignment = ((ConstantPoolSDNode) cpIdx.getNode()).getAlignment();

          SDValue zero = dag.getIntPtrConstant(0);
          long eltSize = td.getTypeAllocSize(elts[0].getType());
          SDValue one = dag.getIntPtrConstant(1);
          SDValue cond = dag.getSetCC(new EVT(tli.getSetCCResultType(n0.getValueType())),
              n0, n1, cc);

          SDValue cstOffset = dag.getNode(ISD.SELECT, zero.getValueType(),
              cond, one, zero);
          cpIdx = dag.getNode(ISD.ADD, new EVT(tli.getPointerTy()), cpIdx, cstOffset);
          return dag.getLoad(tv.getValueType(0),
              dag.getEntryNode(), cpIdx,
              PseudoSourceValue.getConstantPool(), 0,
              false, alignment);
        }
      }
    }

    if (c1 != null && c3 != null && c3.isNullValue() &
        cc == CondCode.SETLT &&
        n0.getValueType().isInteger() &&
        n2.getValueType().isInteger() &&
        (c1.isNullValue() || (c1.getAPIntValue().eq(1) &&
            n0.equals(n2)))) {
      EVT xTy = n0.getValueType();
      EVT aTy = n2.getValueType();
      if (xTy.bitsGT(aTy)) {
        if (c2 != null && c2.getAPIntValue().and(c2.getAPIntValue().sub(1)).eq(0)) {
          long shCtv = c2.getAPIntValue().logBase2();
          SDValue shCt = dag.getConstant(shCtv, getShiftAmountTy(), false);
          SDValue shift = dag.getNode(ISD.SRL,
              xTy, n0, shCt);
          addToWorkList(shift.getNode());

          if (xTy.bitsGT(aTy)) {
            shift = dag.getNode(ISD.TRUNCATE, aTy, shift);
            addToWorkList(shift.getNode());
          }
          return dag.getNode(ISD.AND, aTy, shift, n2);
        }

        SDValue shift = dag.getNode(ISD.SRA, xTy, n0,
            dag.getConstant(xTy.getSizeInBits() - 1,
                getShiftAmountTy(), false));
        addToWorkList(shift.getNode());
        if (xTy.bitsGT(aTy)) {
          shift = dag.getNode(ISD.TRUNCATE, aTy, shift);
          addToWorkList(shift.getNode());
        }
        return dag.getNode(ISD.AND, aTy, shift, n2);
      }
    }

    if (c2 != null && c3 != null && c3.isNullValue() &&
        c2.getAPIntValue().isPowerOf2() &&
        tli.getBooleanContents() == TargetLowering.BooleanContent.ZeroOrOneBooleanContent) {
      if (notExtCompare && c2.getAPIntValue().eq(1))
        return new SDValue();

      SDValue temp, sc;
      if (legalTypes) {
        sc = dag.getSetCC(new EVT(tli.getSetCCResultType(
            n0.getValueType())), n0, n1, cc);
        if (n2.getValueType().bitsGT(sc.getValueType()))
          temp = dag.getZeroExtendInReg(sc, n2.getValueType());
        else
          temp = dag.getNode(ISD.ZERO_EXTEND, n2.getValueType(), sc);
      } else {
        sc = dag.getSetCC(new EVT(MVT.i1), n0, n1, cc);
        temp = dag.getNode(ISD.ZERO_EXTEND, n2.getValueType(), sc);
      }

      addToWorkList(sc.getNode());
      addToWorkList(temp.getNode());

      if (c2.getAPIntValue().eq(1))
        return temp;

      return dag.getNode(ISD.SHL, n2.getValueType(), temp,
          dag.getConstant(c2.getAPIntValue().logBase2(),
              getShiftAmountTy(), false));
    }

    if (c1 != null && c1.isNullValue() && (cc == CondCode.SETLT ||
        cc == CondCode.SETLE) && n0.equals(n3) &&
        n2.getOpcode() == ISD.SUB && n0.equals(n2.getOperand(1)) &&
        n2.getOperand(0).equals(n1) && n0.getValueType().isInteger()) {
      EVT xType = n0.getValueType();
      SDValue shift = dag.getNode(ISD.SRA, xType, n0,
          dag.getConstant(xType.getSizeInBits() - 1,
              getShiftAmountTy(), false));
      SDValue add = dag.getNode(ISD.ADD, xType, n0, shift);
      addToWorkList(shift.getNode());
      ;
      addToWorkList(add.getNode());
      return dag.getNode(ISD.XOR, xType, add, shift);
    }

    if (c1 != null && c1.isAllOnesValue() && cc == CondCode.SETGT &&
        n0.equals(n2) && n3.getOpcode() == ISD.SUB &&
        n0.equals(n3.getOperand(1))) {
      if (n3.getOperand(0).getNode() instanceof ConstantSDNode) {
        ConstantSDNode csd = (ConstantSDNode) n3.getOperand(0).getNode();
        EVT xType = n0.getValueType();
        if (csd.isNullValue() && xType.isInteger()) {
          SDValue shift = dag.getNode(ISD.SRA, xType, n0,
              dag.getConstant(xType.getSizeInBits() - 1,
                  getShiftAmountTy(), false));
          SDValue add = dag.getNode(ISD.ADD, xType, n0, shift);
          addToWorkList(shift.getNode());
          addToWorkList(add.getNode());
          return dag.getNode(ISD.XOR, xType, add, shift);
        }
      }
    }
    return new SDValue();
  }

  private SDValue visitCTPOP(SDNode n) {
    SDValue n0 = n.getOperand(0);
    EVT vt = n.getValueType(0);
    if (n0.getNode() instanceof ConstantSDNode)
      return dag.getNode(ISD.CTPOP, vt, n0);

    return new SDValue();
  }

  private SDValue visitCTTZ(SDNode n) {
    SDValue n0 = n.getOperand(0);
    EVT vt = n.getValueType(0);
    if (n0.getNode() instanceof ConstantSDNode)
      return dag.getNode(ISD.CTTZ, vt, n0);

    return new SDValue();
  }

  private SDValue visitCTLZ(SDNode n) {
    SDValue n0 = n.getOperand(0);
    EVT vt = n.getValueType(0);
    if (n0.getNode() instanceof ConstantSDNode)
      return dag.getNode(ISD.CTLZ, vt, n0);

    return new SDValue();
  }

  private SDValue visitSRL(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c0 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c1 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();
    int opSizeInBits = vt.getSizeInBits();

    if (c0 != null && c1 != null) {
      return dag.foldConstantArithmetic(ISD.SRL, vt, c0, c1);
    }
    if (c0 != null && c0.isNullValue())
      return n0;
    if (c1 != null && c1.getZExtValue() >= opSizeInBits)
      return dag.getUNDEF(vt);
    if (c1 != null && c1.isNullValue())
      return n0;
    if (c1 != null && dag.maskedValueIsZero(new SDValue(n, 0),
        APInt.getAllOnesValue(opSizeInBits)))
      return dag.getConstant(0, vt, false);

    if (c1 != null && n0.getOpcode() == ISD.SRL &&
        n0.getOperand(1).getOpcode() == ISD.Constant) {
      long t1 = ((ConstantSDNode) n0.getOperand(1).getNode()).getZExtValue();
      long t2 = c1.getZExtValue();
      if ((t1 + t2) > opSizeInBits)
        return dag.getConstant(0, vt, false);
      return dag.getNode(ISD.SRL, vt, n0.getOperand(0),
          dag.getConstant(t1 + t2, n1.getValueType(), false));
    }

    if (c1 != null && n0.getOpcode() == ISD.ANY_EXTEND) {
      EVT smallVT = n0.getOperand(0).getValueType();
      if (c1.getZExtValue() >= smallVT.getSizeInBits())
        return dag.getUNDEF(vt);

      SDValue smallShift = dag.getNode(ISD.SRL, smallVT, n0.getOperand(0), n1);
      addToWorkList(smallShift.getNode());
      return dag.getNode(ISD.ANY_EXTEND, vt, smallShift);
    }

    if (c1 != null && c1.getZExtValue() + 1 == vt.getSizeInBits() &&
        n0.getOpcode() == ISD.SRA) {
      return dag.getNode(ISD.SRL, vt, n0.getOperand(0), n1);
    }

    if (c1 != null && n0.getOpcode() == ISD.CTLZ &&
        c1.getAPIntValue().eq(Util.log2(vt.getSizeInBits()))) {
      APInt[] knownVals = new APInt[2];
      APInt mask = APInt.getAllOnesValue(vt.getSizeInBits());
      dag.computeMaskedBits(n0.getOperand(0), mask, knownVals, 0);
      APInt knownZero = knownVals[0];
      APInt knownOne = knownVals[1];
      if (knownOne.getBoolValue()) return dag.getConstant(0, vt, false);

      APInt unknownBits = knownZero.not().and(mask);
      if (unknownBits.eq(0)) return dag.getConstant(1, vt, false);

      if (unknownBits.and(unknownBits.sub(1)).eq(0)) {
        int shAmt = unknownBits.countTrailingZeros();
        SDValue op = n0.getOperand(0);
        if (shAmt != 0) {
          op = dag.getNode(ISD.SRL, vt, op,
              dag.getConstant(shAmt, getShiftAmountTy(), false));
          addToWorkList(op.getNode());
        }
        return dag.getNode(ISD.XOR, vt, op, dag.getConstant(1, vt, false));
      }
    }

    if (n1.getOpcode() == ISD.TRUNCATE &&
        n1.getOperand(0).getOpcode() == ISD.AND &&
        n1.hasOneUse() && n1.getOperand(0).hasOneUse()) {
      SDValue n101 = n1.getOperand(0).getOperand(1);
      if (n101.getNode() instanceof ConstantSDNode) {
        ConstantSDNode cst = (ConstantSDNode) n101.getNode();
        EVT truncVT = n1.getValueType();
        SDValue n100 = n1.getOperand(0).getOperand(0);
        APInt truncVal = cst.getAPIntValue();
        truncVal = truncVal.trunc(truncVT.getSizeInBits());
        return dag.getNode(ISD.SRL, vt, n0, dag.getNode(ISD.AND, truncVT,
            dag.getNode(ISD.TRUNCATE, truncVT, n100),
            dag.getConstant(truncVal, truncVT, false)));
      }
    }

    if (c1 != null && simplifyDemandedBits(new SDValue(n, 0)))
      return new SDValue(n, 0);
    return c1 != null ? visitShiftByConstant(n, c1.getZExtValue()) : new SDValue();
  }

  private SDValue visitSRA(SDNode n) {

    return new SDValue();
  }

  private SDValue visitSHL(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c0 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c1 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();
    int opSizeInBits = vt.getSizeInBits();

    if (c0 != null && c1 != null) {
      return dag.foldConstantArithmetic(ISD.SHL, vt, c0, c1);
    }
    if (c0 != null && c0.isNullValue())
      return n0;
    if (c1 != null && c1.getZExtValue() >= opSizeInBits)
      return dag.getUNDEF(vt);
    if (c1 != null && c1.isNullValue())
      return n0;
    if (dag.maskedValueIsZero(new SDValue(n, 0), APInt.getAllOnesValue(vt.getSizeInBits()))) {
      return dag.getConstant(0, vt, false);
    }
    if (n1.getOpcode() == ISD.TRUNCATE &&
        n1.getOperand(0).getOpcode() == ISD.AND &&
        n1.hasOneUse() && n1.getOperand(0).hasOneUse()) {
      SDValue n101 = n1.getOperand(0).getOperand(1);
      if (n101.getNode() instanceof ConstantSDNode) {
        ConstantSDNode n101C = (ConstantSDNode) n101.getNode();
        EVT truncVT = n1.getValueType();
        SDValue n100 = n1.getOperand(0).getOperand(0);
        APInt trunc = n101C.getAPIntValue();
        trunc = trunc.trunc(truncVT.getSizeInBits());
        return dag.getNode(ISD.SHL, vt, n0,
            dag.getNode(ISD.AND, truncVT, dag.getNode(ISD.TRUNCATE, truncVT, n100),
                dag.getConstant(trunc, truncVT, false)));
      }
    }

    if (c1 != null && n0.getOpcode() == ISD.SHL &&
        n0.getOperand(1).getOpcode() == ISD.Constant) {
      long t1 = ((ConstantSDNode) n0.getOperand(1).getNode()).getZExtValue();
      long t2 = c1.getZExtValue();
      if ((t1 + t2) > opSizeInBits)
        return dag.getConstant(0, vt, false);
      return dag.getNode(ISD.SHL, vt, n0.getOperand(0),
          dag.getConstant(t1 + t2, n1.getValueType(), false));
    }

    if (c1 != null && n0.getOpcode() == ISD.SRL &&
        n0.getOperand(1).getOpcode() == ISD.Constant) {
      long t1 = ((ConstantSDNode) n0.getOperand(1).getNode()).getZExtValue();
      if (t1 < vt.getSizeInBits()) {
        long t2 = c1.getZExtValue();
        SDValue hiBitsMask = dag.getConstant(APInt.getHighBitsSet(vt.getSizeInBits(),
            (int) (vt.getSizeInBits() - t1)), vt, false);
        SDValue mask = dag.getNode(ISD.AND, vt, n0.getOperand(0), hiBitsMask);
        if (t2 > t1)
          return dag.getNode(ISD.SHL, vt, mask, dag.getConstant(t2 - t1, n1.getValueType(), false));
        else
          return dag.getNode(ISD.SRL, vt, mask, dag.getConstant(t1 - t2, n1.getValueType(), false));
      }
    }

    if (c1 != null && n0.getOpcode() == ISD.SRA && n1.equals(n0.getOperand(1))) {
      SDValue hiBitsMask = dag.getConstant(APInt.getHighBitsSet(vt.getSizeInBits(),
          (int) (vt.getSizeInBits() - c1.getZExtValue())), vt, false);
      return dag.getNode(ISD.AND, vt, n0.getOperand(0), hiBitsMask);
    }

    return c1 != null ? visitShiftByConstant(n, c1.getZExtValue()) : new SDValue();
  }

  private SDValue visitShiftByConstant(SDNode n, long amt) {
    SDNode lhs = n.getOperand(0).getNode();
    if (!lhs.hasOneUse()) return new SDValue();

    boolean highBits = false;
    switch (lhs.getOpcode()) {
      default:
        return new SDValue();
      case ISD.OR:
      case ISD.XOR:
        highBits = false;
        break;
      case ISD.AND:
        highBits = true;
        break;
      case ISD.ADD:
        if (n.getOpcode() != ISD.SHL)
          return new SDValue();
        highBits = false;
        break;
    }

    if (!(lhs.getOperand(1).getNode() instanceof ConstantSDNode))
      return new SDValue();

    ConstantSDNode binOpCst = (ConstantSDNode) lhs.getOperand(1).getNode();
    SDNode binOpLhsVal = lhs.getOperand(0).getNode();
    int binOpc = binOpLhsVal.getOpcode();
    if ((binOpc != ISD.SHL && binOpc != ISD.SRA && binOpc != ISD.SRL) ||
        !(binOpLhsVal.getOperand(1).getNode() instanceof ConstantSDNode))
      return new SDValue();

    EVT vt = n.getValueType(0);
    if (n.getOpcode() == ISD.SRA) {
      boolean binOpRhsSignSet = binOpCst.getAPIntValue().isNegative();
      if (binOpRhsSignSet != highBits)
        return new SDValue();
    }

    SDValue newRhs = dag.getNode(n.getOpcode(), n.getValueType(0), lhs.getOperand(1), n.getOperand(1));
    SDValue newShift = dag.getNode(n.getOpcode(), vt, lhs.getOperand(0), n.getOperand(1));
    return dag.getNode(lhs.getOpcode(), vt, newShift, newRhs);
  }

  private SDValue visitXOR(SDNode n) {
    SDValue op0 = n.getOperand(0);
    SDValue op1 = n.getOperand(1);
    ConstantSDNode op0C = op0.getNode() instanceof ConstantSDNode
        ? (ConstantSDNode) op0.getNode() : null;
    ConstantSDNode op1C = op1.getNode() instanceof ConstantSDNode
        ? (ConstantSDNode) op1.getNode() : null;
    EVT vt = op0.getValueType();
    if (vt.isVector()) {
      SDValue foldedOp = simplifyVBinOp(n);
      if (foldedOp.getNode() != null)
        return foldedOp;
    }

    if (op0.getOpcode() == ISD.UNDEF && op1.getOpcode() == ISD.UNDEF)
      return dag.getConstant(0, vt, false);
    if (op0.getOpcode() == ISD.UNDEF)
      return op0;
    if (op1.getOpcode() == ISD.UNDEF)
      return op1;
    // fold (xor c1, c2) -> c1 ^ c2.
    if (op0C != null && op1C != null)
      return dag.foldConstantArithmetic(ISD.XOR, vt, op0C, op1C);
    if (op0C != null && op1C == null)
      return dag.getNode(ISD.XOR, vt, op1, op0);
    if (op1C != null && op1C.isNullValue())
      return op0;
    SDValue rxor = reassociateOps(ISD.XOR, op0, op1);
    if (rxor.getNode() != null)
      return rxor;

    // fold !(x cc y) -> (x !cc y)
    SDValue[] res = new SDValue[3];
    if (op1C != null && op1C.getAPIntValue().eq(1) && isSetCCEquivalent(op0, res)) {
      boolean isInt = res[0].getValueType().isInteger();
      CondCode notCC = ISD.getSetCCInverse(((CondCodeSDNode) res[2].getNode()).getCondition(),
          isInt);
      if (!legalOprations || tli.isCondCodeLegal(notCC, res[0].getValueType())) {
        switch (op0.getOpcode()) {
          default:
            Util.shouldNotReachHere("Unhandled SetCC Equivalent");
            break;
          case ISD.SETCC:
            return dag.getSetCC(vt, res[0], res[1], notCC);
          case ISD.SELECT_CC:
            return dag.getSelectCC(res[0], res[1], op0.getOperand(2),
                op0.getOperand(3), notCC);
        }
      }
    }

    // fold (not (zext (setcc x, y))) -> (zext (not (setcc x, y)))
    if (op1C != null && op1C.getAPIntValue().eq(1) &&
        op0.getOpcode() == ISD.ZERO_EXTEND &&
        op0.hasOneUse() && isSetCCEquivalent(op0.getOperand(0), res)) {
      SDValue v = op0.getOperand(0);
      v = dag.getNode(ISD.XOR, v.getValueType(), v,
          dag.getConstant(1, v.getValueType(), false));
      addToWorkList(v.getNode());
      return dag.getNode(ISD.ZERO_EXTEND, vt, v);
    }

    // fold (not (or x, y)) -> (and (not x), (not y)) iff x or y are setcc
    if (op1C != null && op1C.getAPIntValue().eq(1) && vt.getSimpleVT().simpleVT == MVT.i1 &&
        (op0.getOpcode() == ISD.OR || op0.getOpcode() == ISD.AND)) {
      SDValue lhs = op0.getOperand(0), rhs = op0.getOperand(1);
      if (isOneUseSetCC(rhs) || isOneUseSetCC(rhs)) {
        int newOpc = op0.getOpcode() == ISD.AND ? ISD.OR : ISD.AND;
        lhs = dag.getNode(ISD.XOR, vt, lhs, op1);
        rhs = dag.getNode(ISD.XOR, vt, rhs, op1);
        addToWorkList(lhs.getNode());
        addToWorkList(rhs.getNode());
        return dag.getNode(newOpc, vt, lhs, rhs);
      }
    }

    // fold (not (or x, y)) -> (and (not x), (not y)) iff x or y are constants
    if (op1C != null && op1C.isAllOnesValue() &&
        (op0.getOpcode() == ISD.OR || op1.getOpcode() == ISD.AND)) {
      SDValue lhs = op0.getOperand(0), rhs = op0.getOperand(1);
      if (rhs.getNode() instanceof ConstantSDNode ||
          lhs.getNode() instanceof ConstantSDNode) {
        int newOpc = op0.getOpcode() == ISD.AND ? ISD.OR : ISD.AND;
        lhs = dag.getNode(ISD.XOR, vt, lhs, op1);
        rhs = dag.getNode(ISD.XOR, vt, rhs, op1);
        addToWorkList(lhs.getNode());
        addToWorkList(rhs.getNode());
        return dag.getNode(newOpc, vt, lhs, rhs);
      }
    }
    // fold (xor (xor x, c1), c2) -> (xor x, (xor c1, c2))
    if (op1C != null && op0.getOpcode() == ISD.XOR) {
      ConstantSDNode op00C = op0.getOperand(0).getNode() instanceof ConstantSDNode
          ? (ConstantSDNode) op0.getOperand(0).getNode() : null;
      ConstantSDNode op01C = op0.getOperand(1).getNode() instanceof ConstantSDNode
          ? (ConstantSDNode) op0.getOperand(1).getNode() : null;
      if (op00C != null) {
        dag.getNode(ISD.XOR, vt, op0.getOperand(1),
            dag.getConstant(op1C.getAPIntValue().xor(op00C.getAPIntValue()),
                vt, false));
      }
      if (op01C != null) {
        dag.getNode(ISD.XOR, vt, op0.getOperand(0),
            dag.getConstant(op1C.getAPIntValue().xor(op01C.getAPIntValue()),
                vt, false));
      }
    }
    // fold (xor x, x) -> 0
    if (op0.equals(op1)) {
      if (!vt.isVector())
        return dag.getConstant(0, vt, false);
      else if (!legalOprations || tli.isOperationLegal(ISD.BUILD_VECTOR, vt)) {
        SDValue el = dag.getConstant(0, vt.getVectorElementType(), false);
        SDValue[] ops = new SDValue[vt.getVectorNumElements()];
        Arrays.fill(ops, el);
        return dag.getNode(ISD.BUILD_VECTOR, vt, ops);
      }
    }

    // Simplify: xor (op x...), (op y...)  -> (op (xor x, y))
    if (op0.getOpcode() == op1.getOpcode()) {
      SDValue tmp = simplifyBinOpWithSamOpcodeHands(n);
      if (tmp.getNode() != null)
        return tmp;
    }
    if (!vt.isVector() && simplifyDemandedBits(new SDValue(n, 0)))
      return new SDValue(n, 0);

    return new SDValue();
  }

  private boolean simplifyDemandedBits(SDValue op) {
    APInt demanded = APInt.getAllOnesValue(op.getValueSizeInBits());
    return simplifyDemandedBits(op, demanded);
  }

  private boolean simplifyDemandedBits(SDValue op, APInt demanded) {
    TargetLoweringOpt tlo = new TargetLoweringOpt(dag);
    APInt[] res = new APInt[2];
    if (!tli.simplifyDemandedBits(op, demanded, tlo, res, 0))
      return false;

    addToWorkList(op.getNode());
    commitTargetLoweringOpt(tlo);
    return true;
  }

  private SDValue simplifyBinOpWithSamOpcodeHands(SDNode n) {
    SDValue n0 = n.getOperand(0), n1 = n.getOperand(1);
    EVT vt = n0.getValueType();
    Util.assertion(n0.getOpcode() == n1.getOpcode(), "Bad input!");

    // For each of OP in AND/OR/XOR:
    // fold (OP (zext x), (zext y)) -> (zext (OP x, y))
    // fold (OP (sext x), (sext y)) -> (sext (OP x, y))
    // fold (OP (aext x), (aext y)) -> (aext (OP x, y))
    // fold (OP (trunc x), (trunc y)) -> (trunc (OP x, y)) (if trunc isn't free)
    if ((n0.getOpcode() == ISD.ZERO_EXTEND ||
        n0.getOpcode() == ISD.ANY_EXTEND ||
        n0.getOpcode() == ISD.SIGN_EXTEND ||
        (n0.getOpcode() == ISD.TRUNCATE &&
            !tli.isTruncateFree(n0.getOperand(0).getValueType(), vt))) &&
        n0.getOperand(0).getValueType().equals(n1.getOperand(0).getValueType()) &&
        (!legalOprations || tli.isOperationLegal(n.getOpcode(),
            n0.getOperand(0).getValueType()))) {
      SDValue orNode = dag.getNode(n.getOpcode(), n0.getOperand(0).getValueType(),
          n0.getOperand(0), n1.getOperand(0));
      addToWorkList(orNode.getNode());
      return dag.getNode(n0.getOpcode(), vt, orNode);
    }

    // For each of OP in SHL/SRL/SRA/AND...
    //   fold (and (OP x, z), (OP y, z)) -> (OP (and x, y), z)
    //   fold (or  (OP x, z), (OP y, z)) -> (OP (or  x, y), z)
    //   fold (xor (OP x, z), (OP y, z)) -> (OP (xor x, y), z)
    if ((n0.getOpcode() == ISD.SHL || n0.getOpcode() == ISD.SRL ||
        n0.getOpcode() == ISD.SRA || n0.getOpcode() == ISD.AND) &&
        n0.getOperand(1).equals(n1.getOperand(1))) {
      SDValue orNode = dag.getNode(n.getOpcode(), n0.getOperand(0).getValueType(),
          n0.getOperand(0), n1.getOperand(0));
      addToWorkList(orNode.getNode());
      return dag.getNode(n0.getOpcode(), vt, orNode, n0.getOperand(1));
    }
    return new SDValue();
  }

  private static boolean isOneUseSetCC(SDValue n) {
    SDValue[] res = new SDValue[3];
    return isSetCCEquivalent(n, res) && n.hasOneUse();
  }

  private SDValue reassociateOps(int opc, SDValue n0, SDValue n1) {
    EVT vt = n0.getValueType();
    if (n0.getOpcode() == opc && n0.getOperand(1).getNode() instanceof ConstantSDNode) {
      if (n1.getNode() instanceof ConstantSDNode) {
        // reassoc. (op (op x, c1), c2) -> (op x, (op c1, c2))
        SDValue opNode = dag.foldArithmetic(opc, vt,
            (ConstantSDNode) n0.getOperand(1).getNode(),
            (ConstantSDNode) n1.getNode());
        return dag.getNode(opc, vt, n0.getOperand(0), opNode);
      } else if (n0.hasOneUse()) {
        // reassoc. (op (op x, c1), y) -> (op (op x, y), c1) iff x+c1 has one use
        SDValue opNode = dag.getNode(opc, vt, n0.getOperand(0), n1);
        addToWorkList(opNode.getNode());
        return dag.getNode(opc, vt, opNode, n0.getOperand(1));
      }
    }

    if (n1.getOpcode() == opc && n1.getOperand(1).getNode() instanceof ConstantSDNode) {
      if (n0.getNode() instanceof ConstantSDNode) {
        // reassoc. (op c2, (op x, c1)) -> (op x, (op c1, c2))
        SDValue opNode = dag.foldConstantArithmetic(opc, vt,
            (ConstantSDNode) n1.getOperand(1).getNode(),
            (ConstantSDNode) n0.getNode());
        return dag.getNode(opc, vt, n1.getOperand(0), opNode);
      } else if (n1.hasOneUse()) {
        // reassoc. (op y, (op x, c1)) -> (op (op x, y), c1) iff x+c1 has one use
        SDValue opNode = dag.getNode(opc, vt, n1.getOperand(0), n0);
        addToWorkList(opNode.getNode());
        return dag.getNode(opc, vt, opNode, n1.getOperand(1));
      }
    }
    return new SDValue();
  }

  private static boolean isSetCCEquivalent(SDValue n, SDValue[] res) {
    if (n.getOpcode() == ISD.SETCC) {
      for (int i = 0; i < 3; i++)
        res[i] = n.getOperand(i);
      return true;
    }
    if (n.getOpcode() == ISD.SELECT_CC &&
        n.getOperand(2).getOpcode() == ISD.Constant &&
        n.getOperand(3).getOpcode() == ISD.Constant &&
        ((ConstantSDNode) n.getOperand(2).getNode()).getAPIntValue().eq(1) &&
        ((ConstantSDNode) n.getOperand(3).getNode()).isNullValue()) {
      res[0] = n.getOperand(0);
      res[1] = n.getOperand(1);
      res[2] = n.getOperand(4);
      return true;
    }
    return false;
  }

  private SDValue visitOR(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n1.getValueType();
    int bitwidth = vt.getSizeInBits();

    if (vt.isVector()) {
      SDValue foldedVOp = simplifyVBinOp(n);
      if (foldedVOp.getNode() != null) return foldedVOp;
    }
    // fold (or x, undef) -> -1
    if (n0.getOpcode() == ISD.UNDEF || n1.getOpcode() == ISD.UNDEF)
      return dag.getConstant(APInt.getAllOnesValue(bitwidth), vt, false);
    // fold (or c0, c1) -> c0|c1
    if (c1 != null && c2 != null)
      return dag.foldConstantArithmetic(ISD.OR, vt, c1, c2);
    // fold (or x, 0) -> x
    if (c2 != null && c2.isNullValue())
      return n0;

    // fold (or x, -1) -> -1
    if (c2 != null && c2.isAllOnesValue())
      return n1;

    // fold (or x, c) -> c iff (x & ~c) == 0
    if (c2 != null && dag.maskedValueIsZero(n0, c2.getAPIntValue().not()))
      return n1;
    // reassociate or
    SDValue res = reassociateOps(ISD.OR, n0, n1);
    if (res.getNode() != null)
      return res;

    // Canonicalize (or (and X, c1), c2) -> (and (or X, c2), c1|c2)
    if (c2 != null && n0.getOpcode() == ISD.AND &&
        n0.getOperand(1).getNode() instanceof ConstantSDNode) {
      ConstantSDNode c = (ConstantSDNode) n0.getOperand(1).getNode();
      SDValue or = dag.getNode(ISD.OR, vt, n0.getOperand(0), n1);
      return dag.getNode(ISD.AND, vt, or,
          dag.foldConstantArithmetic(ISD.OR, vt, c, c2));
    }

    // fold (or (setcc x), (setcc y)) -> (setcc (or x, y))
    SDValue[] setcc1 = new SDValue[3];
    SDValue[] setcc2 = new SDValue[3];
    if (isSetCCEquivalent(n0, setcc1) && isSetCCEquivalent(n1, setcc2)) {
      CondCode cc1 = ((CondCodeSDNode) setcc1[2].getNode()).getCondition();
      CondCode cc2 = ((CondCodeSDNode) setcc2[2].getNode()).getCondition();
      SDValue ll = setcc1[0];
      SDValue lr = setcc1[1];
      SDValue rl = setcc2[0];
      SDValue rr = setcc2[1];
      if (lr.equals(rr) && lr.getNode() instanceof ConstantSDNode &&
          cc1.equals(cc2) &&
          ll.getValueType().isInteger()) {
        // fold (or (setne X, 0), (setne Y, 0)) -> (setne (or X, Y), 0)
        // fold (or (setlt X, 0), (setlt Y, 0)) -> (setne (or X, Y), 0)
        if (((ConstantSDNode) lr.getNode()).isNullValue() &&
            (cc2 == CondCode.SETNE || cc2 == CondCode.SETLT)) {
          SDValue or = dag.getNode(ISD.OR, lr.getValueType(), ll, rl);
          addToWorkList(or.getNode());
          return dag.foldSetCC(vt, or, lr, cc2);
        }
        // fold (or (setne X, -1), (setne Y, -1)) -> (setne (and X, Y), -1)
        // fold (or (setgt X, -1), (setgt Y  -1)) -> (setgt (and X, Y), -1)
        if (((ConstantSDNode) lr.getNode()).isAllOnesValue() &&
            (cc2 == CondCode.SETNE || cc2 == CondCode.SETGT)) {
          SDValue and = dag.getNode(ISD.AND, lr.getValueType(), ll, rl);
          addToWorkList(and.getNode());
          return dag.getSetCC(vt, and, lr, cc2);
        }
      }
      if (ll.equals(rr) && lr.equals(rl)) {
        cc2 = ISD.getSetCCSwappedOperands(cc2);
        SDValue t = rl;
        rl = rr;
        rr = t;
      }
      if (ll.equals(rl) && lr.equals(rr)) {
        boolean isInteger = ll.getValueType().isInteger();
        CondCode result = ISD.getSetCCOrOperation(cc1, cc2, isInteger);
        if (result != CondCode.SETCC_INVALID &&
            (!legalOprations || tli.isCondCodeLegal(result, ll.getValueType()))) {
          return dag.getSetCC(n0.getValueType(), ll, lr, result);
        }
      }
    }
    // Simplify: (or (op x...), (op y...))  -> (op (or x, y))
    if (n0.getOpcode() == n1.getOpcode()) {
      SDValue tmp = simplifyBinOpWithSamOpcodeHands(n);
      if (tmp.getNode() != null) return tmp;
    }
    // (or (and X, C1), (and Y, C2))  -> (and (or X, Y), C3) if possible.
    if (n0.getOpcode() == n1.getOpcode() && n0.getOpcode() == ISD.AND &&
        n0.getOperand(1).getNode() instanceof ConstantSDNode &&
        n1.getOperand(1).getNode() instanceof ConstantSDNode &&
        (n0.hasOneUse() || n1.hasOneUse())) {
      APInt lhsMask = ((ConstantSDNode) n0.getOperand(1).getNode()).getAPIntValue();
      APInt rhsMask = ((ConstantSDNode) n1.getOperand(1).getNode()).getAPIntValue();
      if (dag.maskedValueIsZero(n0.getOperand(0), rhsMask.and(lhsMask.not())) &&
          dag.maskedValueIsZero(n1.getOperand(0), lhsMask.and(rhsMask.not()))) {
        SDValue x = dag.getNode(ISD.OR, vt, n0.getOperand(0), n1.getOperand(0));
        return dag.getNode(ISD.AND, vt, x, dag.getConstant(lhsMask.or(rhsMask), vt, false));
      }
    }
    // See if this is some rotate idiom.
    SDNode t = matchRotate(n0, n1);
    if (t != null)
      return new SDValue(t, 0);
    return new SDValue();
  }

  private SDNode matchRotate(SDValue lhs, SDValue rhs) {
    EVT vt = lhs.getValueType();
    if (!tli.isTypeLegal(vt)) return null;

    boolean hasROTL = tli.isOperationLegalOrCustom(ISD.ROTL, vt);
    boolean hasROTR = tli.isOperationLegalOrCustom(ISD.ROTR, vt);
    if (!hasROTL && !hasROTR) return null;

    SDValue[] lhsRes = {new SDValue(), new SDValue()};
    if (!matchRotateHalf(lhs, lhsRes))
      return null;

    SDValue[] rhsRes = {new SDValue(), new SDValue()};
    if (!matchRotateHalf(rhs, rhsRes))
      return null;

    if (!Objects.equals(lhsRes[0].getOperand(0), rhsRes[0].getOperand(0)))
      return null;

    if (lhsRes[0].getOpcode() == rhsRes[0].getOpcode())
      return null;

    if (rhsRes[0].getOpcode() == ISD.SHL) {
      SDValue t = lhs;
      lhs = rhs;
      rhs = t;
      t = lhsRes[0];
      lhsRes[0] = rhsRes[0];
      lhsRes[0] = t;

      t = lhsRes[1];
      lhsRes[1] = rhsRes[1];
      rhsRes[1] = t;
    }

    int opSizeInBits = vt.getSizeInBits();
    SDValue lhsShiftArg = lhsRes[0].getOperand(0);
    SDValue lhsShiftAmt = lhsRes[0].getOperand(1);
    SDValue rhsShiftAmt = rhsRes[0].getOperand(1);

    // fold (or (shl x, C1), (srl x, C2)) -> (rotl x, C1)
    // fold (or (shl x, C1), (srl x, C2)) -> (rotr x, C2)
    if (lhsShiftAmt.getOpcode() == ISD.Constant &&
        rhsShiftAmt.getOpcode() == ISD.Constant) {
      long lshVal = ((ConstantSDNode) lhsShiftAmt.getNode()).getZExtValue();
      long rshVal = ((ConstantSDNode) rhsShiftAmt.getNode()).getZExtValue();
      if ((lshVal + rshVal) != opSizeInBits)
        return null;

      SDValue rot;
      if (hasROTL)
        rot = dag.getNode(ISD.ROTL, vt, lhsShiftArg, lhsShiftAmt);
      else
        rot = dag.getNode(ISD.ROTR, vt, lhsShiftArg, rhsShiftAmt);

      if (lhsRes[1].getNode() != null || rhsRes[1].getNode() != null) {
        APInt mask = APInt.getAllOnesValue(opSizeInBits);
        if (lhsRes[1].getNode() != null) {
          APInt rhsBits = APInt.getLowBitsSet(opSizeInBits, (int) lshVal);
          mask.andAssign(((ConstantSDNode) lhsRes[1].getNode()).getAPIntValue().or(rhsBits));
        }
        if (rhsRes[1].getNode() != null) {
          APInt lhsBits = APInt.getHighBitsSet(opSizeInBits, (int) rshVal);
          mask.andAssign(((ConstantSDNode) rhsRes[1].getNode()).getAPIntValue().or(lhsBits));
        }
        rot = dag.getNode(ISD.AND, vt, rot, dag.getConstant(mask, vt, false));
      }
      return rot.getNode();
    }

    // If there is a mask here, and we have a variable shift, we can't be sure
    // that we're masking out the right stuff.
    if (lhsRes[1].getNode() != null || rhsRes[1].getNode() != null)
      return null;

    // fold (or (shl x, y), (srl x, (sub 32, y))) -> (rotl x, y)
    // fold (or (shl x, y), (srl x, (sub 32, y))) -> (rotr x, (sub 32, y))
    if (rhsShiftAmt.getOpcode() == ISD.SUB &&
        lhsShiftAmt.equals(rhsShiftAmt.getOperand(1))) {
      if (rhsShiftAmt.getOperand(0).getNode() instanceof ConstantSDNode) {
        ConstantSDNode c = (ConstantSDNode) rhsShiftAmt.getOperand(0).getNode();
        if (c.getAPIntValue().eq(opSizeInBits)) {
          if (hasROTL)
            return dag.getNode(ISD.ROTL, vt, lhsShiftArg, lhsShiftAmt).getNode();
          else
            return dag.getNode(ISD.ROTR, vt, lhsShiftArg, rhsShiftAmt).getNode();
        }
      }
    }

    // fold (or (shl x, (sub 32, y)), (srl x, r)) -> (rotr x, y)
    // fold (or (shl x, (sub 32, y)), (srl x, r)) -> (rotl x, (sub 32, y))
    if (lhsShiftAmt.getOpcode() == ISD.SUB &&
        rhsShiftAmt.equals(lhsShiftAmt.getOperand(1))) {
      if (lhsShiftAmt.getOperand(0).getNode() instanceof ConstantSDNode) {
        ConstantSDNode c = (ConstantSDNode) lhsShiftAmt.getOperand(0).getNode();
        if (c.getAPIntValue().eq(opSizeInBits)) {
          if (hasROTR)
            return dag.getNode(ISD.ROTR, vt, lhsShiftArg, rhsShiftAmt).getNode();
          else
            return dag.getNode(ISD.ROTR, vt, lhsShiftArg, lhsShiftAmt).getNode();
        }
      }
    }

    // Look for sign/zext/any-extended or truncate cases:
    if ((lhsShiftAmt.getOpcode() == ISD.SIGN_EXTEND ||
        lhsShiftAmt.getOpcode() == ISD.ZERO_EXTEND ||
        lhsShiftAmt.getOpcode() == ISD.ANY_EXTEND ||
        lhsShiftAmt.getOpcode() == ISD.TRUNCATE) &&
        (rhsShiftAmt.getOpcode() == ISD.SIGN_EXTEND ||
            rhsShiftAmt.getOpcode() == ISD.ZERO_EXTEND ||
            rhsShiftAmt.getOpcode() == ISD.ANY_EXTEND ||
            rhsShiftAmt.getOpcode() == ISD.TRUNCATE)) {
      SDValue lextOp0 = lhsShiftAmt.getOperand(0);
      SDValue rextOp0 = rhsShiftAmt.getOperand(0);
      if (rextOp0.getOpcode() == ISD.SUB &&
          rextOp0.getOperand(1).equals(lextOp0)) {
        // fold (or (shl x, (*ext y)), (srl x, (*ext (sub 32, y)))) ->
        //   (rotl x, y)
        // fold (or (shl x, (*ext y)), (srl x, (*ext (sub 32, y)))) ->
        //   (rotr x, (sub 32, y))
        if (rextOp0.getOperand(0).getNode() instanceof ConstantSDNode) {
          ConstantSDNode c = (ConstantSDNode) rextOp0.getOperand(0).getNode();
          if (c.getAPIntValue().eq(opSizeInBits)) {
            return dag.getNode(hasROTL ? ISD.ROTL : ISD.ROTR, vt, lhsShiftArg,
                hasROTL ? lhsShiftAmt : rhsShiftAmt).getNode();
          }
        }
      } else if (lextOp0.getOpcode() == ISD.SUB &&
          rextOp0.equals(lextOp0.getOperand(1))) {
        // fold (or (shl x, (*ext (sub 32, y))), (srl x, (*ext y))) ->
        //   (rotr x, y)
        // fold (or (shl x, (*ext (sub 32, y))), (srl x, (*ext y))) ->
        //   (rotl x, (sub 32, y))
        if (lextOp0.getOperand(0).getNode() instanceof ConstantSDNode) {
          ConstantSDNode c = (ConstantSDNode) lextOp0.getOperand(0).getNode();
          if (c.getAPIntValue().eq(opSizeInBits)) {
            return dag.getNode(hasROTR ? ISD.ROTR : ISD.ROTL, vt, lhsShiftArg,
                hasROTR ? rhsShiftAmt : lhsShiftAmt).getNode();
          }
        }
      }
    }
    return null;
  }

  /**
   * Match "(X shl/srl V1) & V2" where V2 may not be present.
   *
   * @param op
   * @param res res[0] represents shift and res[1] represents mask.
   * @return
   */
  private static boolean matchRotateHalf(SDValue op, SDValue[] res) {
    Util.assertion(res != null && res.length == 2);
    if (op.getOpcode() == ISD.AND) {
      SDValue rhs = op.getOperand(1);
      if (rhs.getNode() instanceof ConstantSDNode) {
        res[1] = rhs;
        op = op.getOperand(0);
      } else
        return false;
    }
    if (op.getOpcode() == ISD.SHL || op.getOpcode() == ISD.SRL) {
      res[0] = op;
      return true;
    }
    return false;
  }

  private SDValue visitAND(SDNode n) {
    SDValue op0 = n.getOperand(0);
    SDValue op1 = n.getOperand(1);
    ConstantSDNode c0 = op0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) op0.getNode() : null;
    ConstantSDNode c1 = op1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) op1.getNode() : null;
    EVT vt = op1.getValueType();
    int bitwidth = vt.getSizeInBits();

    if (vt.isVector()) {
      SDValue foldedVOp = simplifyVBinOp(n);
      if (foldedVOp.getNode() != null) return foldedVOp;
    }

    if (op0.getOpcode() == ISD.UNDEF || op1.getOpcode() == ISD.UNDEF) {
      return dag.getConstant(0, vt, false);
    }
    if (c0 != null && c1 != null)
      return dag.foldConstantArithmetic(ISD.AND, vt, c0, c1);
    if (c0 != null && c1 == null)
      return dag.getNode(ISD.AND, vt, op1, op0);
    if (c1 != null && c1.isAllOnesValue())
      return op0;
    if (c1 != null && dag.maskedValueIsZero(new SDValue(n, 0),
        APInt.getAllOnesValue(bitwidth)))
      return dag.getConstant(0, vt, false);
    SDValue rand = reassociateOps(ISD.AND, op0, op1);
    if (rand.getNode() != null)
      return rand;
    // fold (and (or x, 0xFFFF), 0xFF) -> 0xFF
    if (c1 != null && op0.getOpcode() == ISD.OR) {
      if (op0.getOperand(1).getNode() instanceof ConstantSDNode) {
        ConstantSDNode op01C = (ConstantSDNode) op0.getOperand(1).getNode();
        if (op01C.getAPIntValue().and(c1.getAPIntValue()).eq(c1.getAPIntValue()))
          return op1;
      }
    }

    // fold (and (any_ext V), c) -> (zero_ext V) if 'and' only clears top bits.
    if (c1 != null && op0.getOpcode() == ISD.ANY_EXTEND) {
      SDValue op00 = op0.getOperand(0);
      APInt mask = c1.getAPIntValue().not();
      mask = mask.trunc(op00.getValueSizeInBits());
      if (dag.maskedValueIsZero(op00, mask)) {
        SDValue zext = dag.getNode(ISD.ZERO_EXTEND, op0.getValueType(),
            op00);
        combineTo(n, zext, true);
        combineTo(op0.getNode(), zext, true);
        return new SDValue(n, 0);
      }
    }
    // fold (and (setcc x), (setcc y)) -> (setcc (and x, y))
    SDValue[] setccRes = new SDValue[3], setccRes2 = new SDValue[3];
    if (isSetCCEquivalent(op0, setccRes) && isSetCCEquivalent(op1, setccRes2)) {
      CondCode cc1 = ((CondCodeSDNode) setccRes[2].getNode()).getCondition();
      CondCode cc2 = ((CondCodeSDNode) setccRes2[2].getNode()).getCondition();
      if (setccRes[1].equals(setccRes2[1]) &&
          setccRes[1].getNode() instanceof ConstantSDNode &&
          setccRes[0].getValueType().isInteger()) {
        // fold (and (seteq X, 0), (seteq Y, 0)) -> (seteq (or X, Y), 0)
        if (((ConstantSDNode) setccRes[1].getNode()).isNullValue() &&
            cc2 == CondCode.SETEQ) {
          SDValue orNode = dag.getNode(ISD.OR, setccRes[1].getValueType(),
              setccRes[0], setccRes2[0]);
          addToWorkList(orNode.getNode());
          return dag.getSetCC(vt, orNode, setccRes[1], cc2);
        }
        // fold (and (seteq X, -1), (seteq Y, -1)) -> (seteq (and X, Y), -1)
        if (((ConstantSDNode) setccRes[1].getNode()).isAllOnesValue() &&
            cc2 == CondCode.SETEQ) {
          SDValue andNode = dag.getNode(ISD.AND, setccRes[1].getValueType(),
              setccRes[0], setccRes2[0]);
          addToWorkList(andNode.getNode());
          return dag.getSetCC(vt, andNode, setccRes[1], cc2);
        }
        // fold (and (setgt X,  -1), (setgt Y,  -1)) -> (setgt (or X, Y), -1)
        if (((ConstantSDNode) setccRes[1].getNode()).isAllOnesValue() &&
            cc2 == CondCode.SETGT) {
          SDValue orNode = dag.getNode(ISD.OR, setccRes[1].getValueType(),
              setccRes[0], setccRes2[0]);
          addToWorkList(orNode.getNode());
          return dag.getSetCC(vt, orNode, setccRes[1], cc2);
        }
      }

      // canonicalize equivalent to ll == rl
      if (setccRes[0].equals(setccRes2[1]) && setccRes[1].equals(setccRes2[0])) {
        cc2 = ISD.getSetCCSwappedOperands(cc2);
        SDValue t = setccRes2[0];
        setccRes2[0] = setccRes2[1];
        setccRes2[1] = t;
      }
      if (setccRes[0].equals(setccRes2[0]) && setccRes[1].equals(setccRes2[1])) {
        boolean isInteger = setccRes[0].getValueType().isInteger();
        CondCode result = ISD.getSetCCAndOperation(cc1, cc2, isInteger);
        if (result != CondCode.SETCC_INVALID &&
            (!legalOprations || tli.isCondCodeLegal(result, setccRes[0].getValueType())))
          return dag.getSetCC(op0.getValueType(), setccRes[0], setccRes[1], result);
      }
    }

    // Simplify: (and (op x...), (op y...))  -> (op (and x, y))
    if (op0.getOpcode() == op1.getOpcode()) {
      SDValue tmp = simplifyBinOpWithSamOpcodeHands(n);
      if (tmp.getNode() != null) return tmp;
    }

    // TODO 9/18/2019, this is a bug so comment it!!!
    //if (!vt.isVector() && simplifyDemandedBits(new SDValue(n, 0)))
    //  return new SDValue(n, 0);

    // fold (zext_inreg (extload x)) -> (zextload x)
    if (op0.getNode().isExtLoad() && op0.getNode().isUNINDEXEDLoad()) {
      LoadSDNode ld = (LoadSDNode) op0.getNode();
      EVT evt = ld.getMemoryVT();
      bitwidth = op1.getValueSizeInBits();
      if (dag.maskedValueIsZero(op1, APInt.getHighBitsSet(bitwidth,
          bitwidth - evt.getSizeInBits())) &&
          ((!legalOprations && !ld.isVolatile()) ||
              tli.isLoadExtLegal(LoadExtType.ZEXTLOAD, evt))) {
        SDValue extLd = dag.getExtLoad(LoadExtType.ZEXTLOAD,
            vt, ld.getChain(), ld.getBasePtr(),
            ld.getSrcValue(), ld.getSrcValueOffset(),
            evt, ld.isVolatile(), ld.getAlignment());
        addToWorkList(n);
        combineTo(op0.getNode(), extLd, extLd.getValue(1), true);
        return new SDValue(n, 0);
      }
    }

    // fold (and (load x), 255) -> (zextload x, i8)
    // fold (and (extload x, i16), 255) -> (zextload x, i8)
    if (c1 != null && op0.getOpcode() == ISD.LOAD) {
      LoadSDNode ld = (LoadSDNode) op0.getNode();
      if (ld.getExtensionType() == LoadExtType.SEXTLOAD &&
          ld.isUnindexed() && op0.hasOneUse() &&
          !ld.isVolatile()) {
        EVT extVT = new EVT(MVT.Other);
        int activeBits = c1.getAPIntValue().getActiveBits();
        if (activeBits > 0 && APInt.isMask(activeBits, c1.getAPIntValue())) {
          extVT = EVT.getIntegerVT(dag.getContext(), activeBits);
        }
        EVT loadedVT = ld.getMemoryVT();
        if (!extVT.equals(new EVT(MVT.Other)) && loadedVT.bitsGT(extVT) &&
            extVT.isRound() && (!legalOprations ||
            tli.isLoadExtLegal(LoadExtType.ZEXTLOAD, extVT))) {
          EVT ptrTy = op0.getOperand(1).getValueType();
          int lvtStoreBytes = loadedVT.getStoreSizeInBits() / 8;
          int evtStoreBytes = extVT.getStoreSizeInBits() / 8;
          int ptrOff = lvtStoreBytes - evtStoreBytes;
          int alignment = ld.getAlignment();
          SDValue newPtr = ld.getBasePtr();
          if (tli.isBigEndian()) {
            newPtr = dag.getNode(ISD.ADD, ptrTy, newPtr,
                dag.getConstant(ptrOff, ptrTy, false));
            alignment = Util.minAlign(alignment, ptrOff);
          }
          addToWorkList(newPtr.getNode());
          SDValue load = dag.getExtLoad(LoadExtType.ZEXTLOAD,
              vt, ld.getChain(), newPtr, ld.getSrcValue(),
              ld.getSrcValueOffset(), extVT, ld.isVolatile(),
              alignment);
          addToWorkList(n);
          combineTo(op0.getNode(), load, load.getValue(1), true);
          return new SDValue(n, 0);
        }
      }
    }
    return new SDValue();
  }

  private SDValue visitUDIVREM(SDNode n) {
    SDValue res = simplifyNodeWithTwoResults(n, ISD.UDIV, ISD.UREM);
    if (res.getNode() != null) return res;
    return new SDValue();
  }

  private SDValue visitSDIVREM(SDNode n) {
    SDValue res = simplifyNodeWithTwoResults(n, ISD.SDIV, ISD.SREM);
    if (res.getNode() != null) return res;
    return new SDValue();
  }

  private SDValue visitUMUL_LOHI(SDNode n) {
    SDValue res = simplifyNodeWithTwoResults(n, ISD.MUL, ISD.MULHU);
    if (res.getNode() != null) return res;
    return new SDValue();
  }

  private SDValue simplifyNodeWithTwoResults(SDNode n, int loOp, int hiOp) {
    boolean hiExists = n.hasAnyUseOfValue(1);
    if (!hiExists && (!legalOprations || tli.isOperationLegal(loOp, n.getValueType(0)))) {
      SDValue res = dag.getNode(loOp, n.getValueType(0), n.getOperandList());
      return combineTo(n, res, res, true);
    }

    boolean loExists = n.hasAnyUseOfValue(0);
    if (!loExists && (!legalOprations || tli.isOperationLegal(hiOp, n.getValueType(1)))) {
      SDValue res = dag.getNode(hiOp, n.getValueType(1), n.getOperandList());
      return combineTo(n, res, res, true);
    }

    if (loExists && hiExists)
      return new SDValue();

    if (loExists) {
      SDValue lo = dag.getNode(loOp, n.getValueType(0), n.getOperandList());
      addToWorkList(lo.getNode());
      SDValue loOpt = combine(lo.getNode());
      if (loOpt.getNode() != null && !loOpt.getNode().equals(lo.getNode()) &&
          (!legalOprations || tli.isOperationLegal(loOpt.getOpcode(), loOpt.getValueType())))
        return combineTo(n, loOpt, loOpt, true);
    }

    if (hiExists) {
      SDValue hi = dag.getNode(hiOp, n.getValueType(1), n.getOperandList());
      addToWorkList(hi.getNode());
      SDValue hiOpt = combine(hi.getNode());
      if (hiOpt.getNode() != null && !hiOpt.equals(hi) &&
          (!legalOprations || tli.isOperationLegal(hiOpt.getOpcode(), hiOpt.getValueType())))
        return combineTo(n, hiOpt, hiOpt, true);
    }
    return new SDValue();
  }

  private SDValue visitSMUL_LOHI(SDNode n) {
    SDValue res = simplifyNodeWithTwoResults(n, ISD.MUL, ISD.MULHS);
    if (res.getNode() != null) return res;
    return new SDValue();
  }

  private SDValue visitMULHS(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();

    if (c2 != null && c2.isNullValue())
      return n1;
    if (c2 != null && c2.getAPIntValue().eq(1))
      return dag.getNode(ISD.SRA, n0.getValueType(), n0,
          dag.getConstant(n0.getValueType().getSizeInBits() - 1,
              getShiftAmountTy(), false));

    if (n0.getOpcode() == ISD.UNDEF || n1.getOpcode() == ISD.UNDEF)
      return dag.getConstant(0, vt, false);

    return new SDValue();
  }

  private SDValue visitMULHU(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();

    if (c2 != null && c2.isNullValue())
      return n1;
    if (c2 != null && c2.getAPIntValue().eq(1))
      return dag.getConstant(0, n0.getValueType(), false);

    if (n0.getOpcode() == ISD.UNDEF || n1.getOpcode() == ISD.UNDEF)
      return dag.getConstant(0, vt, false);

    return new SDValue();
  }

  private SDValue visitUREM(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();

    if (c1 != null && c2 != null && !c2.isNullValue()) {
      return dag.foldConstantArithmetic(ISD.UREM, vt, c1, c2);
    }
    if (c2 != null && !c2.isNullValue() && !c2.getAPIntValue().isPowerOf2())
      return dag.getNode(ISD.AND, vt, n0, dag.getConstant(c2.getAPIntValue().sub(1), vt, false));

    if (n1.getOpcode() == ISD.SHL && n1.getOperand(0).getNode() instanceof ConstantSDNode) {
      ConstantSDNode shc = (ConstantSDNode) n1.getOperand(0).getNode();
      if (shc.getAPIntValue().isPowerOf2()) {
        SDValue add = dag.getNode(ISD.ADD, vt, n1,
            dag.getConstant(APInt.getAllOnesValue(vt.getSizeInBits()), vt, false));
        addToWorkList(add.getNode());
        return dag.getNode(ISD.AND, vt, n0, add);
      }
    }

    if (c2 != null && !c2.isNullValue()) {
      SDValue div = dag.getNode(ISD.UDIV, vt, n0, n1);
      addToWorkList(div.getNode());
      SDValue optimizedDiv = combine(div.getNode());
      if (optimizedDiv.getNode() != null &&
          !optimizedDiv.getNode().equals(div.getNode())) {
        SDValue mul = dag.getNode(ISD.MUL, vt, optimizedDiv, n1);
        SDValue sub = dag.getNode(ISD.SUB, vt, n0, mul);
        addToWorkList(mul.getNode());
        return sub;
      }
    }

    if (n0.getOpcode() == ISD.UNDEF)
      return dag.getConstant(0, vt, false);
    if (n1.getOpcode() == ISD.UNDEF)
      return n1;
    return new SDValue();
  }

  private SDValue visitSREM(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();
    if (c1 != null && c2 != null && !c2.isNullValue()) {
      return dag.foldConstantArithmetic(ISD.SREM, vt, c1, c2);
    }
    if (!vt.isVector() && dag.signBitIsZero(n1, 0) && dag.signBitIsZero(n0, 0)) {
      return dag.getNode(ISD.UREM, vt, n0, n1);
    }

    if (c2 != null && !c2.isNullValue()) {
      SDValue div = dag.getNode(ISD.SDIV, vt, n0, n1);
      addToWorkList(div.getNode());
      SDValue optimizedDiv = combine(div.getNode());
      if (optimizedDiv.getNode() != null &&
          !optimizedDiv.getNode().equals(div.getNode())) {
        SDValue mul = dag.getNode(ISD.MUL, vt, optimizedDiv, n1);
        SDValue sub = dag.getNode(ISD.SUB, vt, n0, mul);
        addToWorkList(mul.getNode());
        return sub;
      }
    }

    if (n0.getOpcode() == ISD.UNDEF)
      return dag.getConstant(0, vt, false);
    if (n1.getOpcode() == ISD.UNDEF)
      return n1;
    return new SDValue();
  }

  private SDValue visitUDIV(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();

    if (vt.isVector()) {
      SDValue foldedOp = simplifyVBinOp(n);
      if (foldedOp.getNode() != null) return foldedOp;
      return new SDValue();
    }
    // fold (udiv c1, c2) -> c1/c2
    if (c1 != null && c2 != null && !c2.isNullValue()) {
      return dag.foldConstantArithmetic(ISD.UDIV, vt, c1, c2);
    }
    // fold (udiv x, (1 << c)) -> x >>u c
    if (c2 != null && c2.getAPIntValue().isPowerOf2()) {
      return dag.getNode(ISD.SRL, vt, n0,
          dag.getConstant(c2.getAPIntValue().logBase2(),
              getShiftAmountTy(), false));
    }
    // fold (udiv x, (shl c, y)) -> x >>u (log2(c)+y) iff c is power of 2
    if (n1.getOpcode() == ISD.SHL &&
        n1.getOperand(0).getNode() instanceof ConstantSDNode) {
      ConstantSDNode c = (ConstantSDNode) n1.getOperand(0).getNode();
      if (c.getAPIntValue().isPowerOf2()) {
        EVT addVT = n1.getOperand(1).getValueType();
        SDValue add = dag.getNode(ISD.ADD, addVT, n1.getOperand(1),
            dag.getConstant(c.getAPIntValue().logBase2(),
                addVT, false));
        return dag.getNode(ISD.SRL, vt, n0, add);
      }
    }

    // fold (udiv x, c) -> alternate
    if (c2 != null && !c2.isNullValue() && !tli.isIntDivCheap()) {
      SDValue op = buildUDIV(n);
      if (op.getNode() != null) return op;
    }

    if (n0.getOpcode() == ISD.UNDEF)
      return dag.getConstant(0, vt, false);
    if (n1.getOpcode() == ISD.UNDEF)
      return n1;

    return new SDValue();
  }

  private SDValue buildUDIV(SDNode n) {
    ArrayList<SDNode> built = new ArrayList<>();
    SDValue s = tli.buildUDIV(n, dag, built);
    built.forEach(this::addToWorkList);
    return s;
  }

  private SDValue visitSDIV(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();

    if (vt.isVector()) {
      SDValue foldedOp = simplifyVBinOp(n);
      if (foldedOp.getNode() != null) return foldedOp;
      return new SDValue();
    }
    // fold (sdiv c1, c2) -> c1/c2
    if (c1 != null && c2 != null && !c2.isNullValue()) {
      return dag.foldConstantArithmetic(ISD.SDIV, vt, c1, c2);
    }
    // fold (sdiv c1, 1) --> c1
    if (c2 != null && c2.getSExtValue() == 1)
      return n0;
    // fold (sdiv X, -1) -> 0-X
    if (c2 != null && c2.isAllOnesValue())
      return dag.getNode(ISD.SUB, vt, dag.getConstant(0, vt, false),
          n0);
    // If we know the sign bits of both operands are zero, strength reduce to a
    // udiv instead.  Handles (X&15) /s 4 -> X&15 >> 2
    if (!vt.isVector()) {
      if (dag.signBitIsZero(n1, 0) && dag.signBitIsZero(n0, 0))
        return dag.getNode(ISD.UDIV, n1.getValueType(), n0, n1);
    }
    // fold (sdiv X, pow2) -> simple ops after legalize
    if (c2 != null && !c2.isNullValue() && !tli.isIntDivCheap() &&
        (Util.isPowerOf2(c2.getSExtValue()) || Util.isPowerOf2(-c2.getSExtValue()))) {
      if (tli.isIntDivCheap())
        return new SDValue();

      long pow2 = c2.getSExtValue();
      long abs2 = pow2 > 0 ? pow2 : -pow2;
      int lg2 = Util.log2(abs2);
      SDValue sgn = dag.getNode(ISD.SRA, vt, n0, dag.getConstant(vt.getSizeInBits() - 1,
          getShiftAmountTy(), false));
      addToWorkList(sgn.getNode());

      // add (n0 < 0) ? abs2 - 1 : 0
      SDValue srl = dag.getNode(ISD.SRL, vt, sgn,
          dag.getConstant(vt.getSizeInBits() - lg2,
              getShiftAmountTy(), false));
      SDValue add = dag.getNode(ISD.ADD, vt, n0, srl);
      addToWorkList(srl.getNode());
      addToWorkList(add.getNode());
      SDValue sra = dag.getNode(ISD.SRA, vt, add,
          dag.getConstant(lg2, getShiftAmountTy(), false));
      if (pow2 > 0)
        return sra;
      addToWorkList(sra.getNode());
      return dag.getNode(ISD.SUB, vt, dag.getConstant(0, vt, false), sra);
    }

    if (c2 != null && (c2.getSExtValue() < -1 || c2.getSExtValue() > 1) &&
        !tli.isIntDivCheap()) {
      SDValue op = buildSDIV(n);
      if (op.getNode() != null) return op;
    }
    if (n0.getOpcode() == ISD.UNDEF)
      return dag.getConstant(0, vt, false);
    if (n1.getOpcode() == ISD.UNDEF)
      return n1;

    return new SDValue();
  }

  private SDValue buildSDIV(SDNode n) {
    ArrayList<SDNode> nodes = new ArrayList<>();
    SDValue s = tli.buildSDIV(n, dag, nodes);

    for (SDNode node : nodes) {
      addToWorkList(node);
    }
    return s;
  }

  private SDValue visitMUL(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();

    if (vt.isVector()) {
      SDValue foldedOp = simplifyVBinOp(n);
      if (foldedOp.getNode() != null) return foldedOp;
      return new SDValue();
    }

    // (mul x, undef) or (mul undef, x) --> 0
    if (n0.getOpcode() == ISD.UNDEF || n1.getOpcode() == ISD.UNDEF)
      return dag.getConstant(0, vt, false);
    // fold (mul c1, c2) --> c1*c2
    if (c1 != null && c2 != null)
      return dag.foldConstantArithmetic(ISD.MUL, vt, c1, c2);
    // reassociate the two operand and move the constant to the right.
    if (c1 != null && c2 == null)
      return dag.getNode(ISD.MUL, vt, n1, n0);
    // fold (mul x, 0) --> 0
    if (c2 != null && c2.isNullValue())
      return n1;
    // fold (mul x, -1) -> 0-x
    if (c2 != null && c2.isAllOnesValue())
      return dag.getNode(ISD.SUB, vt, dag.getConstant(0, vt, false), n0);
    if (c2 != null && c2.getAPIntValue().isPowerOf2()) {
      // fold (mul x, (1 << c)) -> x << c
      return dag.getNode(ISD.SHL, vt, n0, dag.getConstant(c2.getAPIntValue().logBase2(),
          getShiftAmountTy(), false));
    }
    if (c2 != null && c2.getAPIntValue().negative().isPowerOf2()) {
      // fold (mul x, -(1 << c)) -> -(x << c) or (-x) << c
      long log2Val = c2.getAPIntValue().negative().logBase2();
      return dag.getNode(ISD.SUB, vt,
          dag.getConstant(0, vt, false),
          dag.getNode(ISD.SHL, vt, n0,
              dag.getConstant(log2Val, vt, false)));
    }
    // (mul (shl X, c1), c2) -> (mul X, c2 << c1)
    if (c2 != null && n0.getOpcode() == ISD.SHL &&
        n0.getOperand(1).getNode() instanceof ConstantSDNode) {
      ConstantSDNode n01C = (ConstantSDNode) n0.getOperand(1).getNode();
      return dag.getNode(ISD.MUL, vt, n0.getOperand(0),
          dag.getConstant(c2.getAPIntValue().shl(n01C.getAPIntValue()), vt, false));
    }
    // Change (mul (shl X, C), Y) -> (shl (mul X, Y), C) when the shift has one
    // use.
    SDValue x = new SDValue(), y = new SDValue();
    if (n0.getOpcode() == ISD.SHL && n0.getOperand(1).getNode() instanceof ConstantSDNode &&
        n0.hasOneUse()) {
      x = n0;
      y = n1;
    } else if (n1.getOpcode() == ISD.SHL && n1.getOperand(1).getNode() instanceof ConstantSDNode &&
        n1.hasOneUse()) {
      x = n1;
      y = n0;
    }
    if (x.getNode() != null) {
      return dag.getNode(ISD.SHL, vt, dag.getNode(ISD.MUL, vt, x.getOperand(0), y),
          x.getOperand(1));
    }
    // fold (mul (add x, c1), c2) -> (add (mul x, c2), c1*c2)
    if (c2 != null && n0.getOpcode() == ISD.ADD && n0.hasOneUse() &&
        n0.getOperand(1).getNode() instanceof ConstantSDNode) {
      return dag.getNode(ISD.ADD, vt,
          dag.getNode(ISD.MUL, vt, n0.getOperand(0), n1),
          dag.getNode(ISD.MUL, vt, n0.getOperand(1), n1));
    }
    SDValue rmul = reassociateOps(ISD.MUL, n0, n1);
    return rmul.getNode() != null ? rmul : new SDValue();
  }

  private EVT getShiftAmountTy() {
    return legalTypes ? new EVT(tli.getShiftAmountTy()) : new EVT(tli.getPointerTy());
  }

  private SDValue visitADDE(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();
    SDValue carryIn = n.getOperand(2);

    if (c1 != null && c2 == null) {
      return dag.getNode(ISD.ADDE, n.getValueList(), n1, n0, carryIn);
    }
    // fold (adde X, Y, false) --> (addc  X, Y).
    if (carryIn.getOpcode() == ISD.CARRY_FALSE)
      return dag.getNode(ISD.ADDC, n.getValueList(), n0, n1);
    return new SDValue();
  }

  private SDValue visitADDC(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();
    if (n.hasNumUsesOfValue(0, 1)) {
      return combineTo(n, dag.getNode(ISD.ADD, vt, n1, n0),
          dag.getNode(ISD.CARRY_FALSE, new EVT(MVT.Glue)), true);
    }
    if (c1 != null && c2 == null) {
      return dag.getNode(ISD.ADDC, n.getValueList(), n1, n0);
    }
    APInt[] lhs = new APInt[2];
    APInt[] rhs = new APInt[2];
    APInt mask = APInt.getAllOnesValue(vt.getSizeInBits());
    dag.computeMaskedBits(n0, mask, lhs, 0);
    if (lhs[0].getBoolValue()) {
      dag.computeMaskedBits(n1, mask, rhs, 0);
      if (rhs[0].and(lhs[0].not().and(mask)).eq(lhs[0].not().and(mask)) ||
          lhs[0].and(rhs[0].not().and(mask)).eq(rhs[0].not().and(mask))) {
        return combineTo(n, dag.getNode(ISD.OR, vt, n0, n1),
            dag.getNode(ISD.CARRY_FALSE, new EVT(MVT.Glue)), true);
      }
    }

    return new SDValue();
  }

  private SDValue visitSUB(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();

    if (vt.isVector()) {
      SDValue foldedOp = simplifyVBinOp(n);
      if (foldedOp.getNode() != null)
        return foldedOp;
    }

    // fold (sub x, x) --> 0
    if (n0.equals(n1))
      return dag.getConstant(0, n.getValueType(0), false);
    if (c1 != null && c2 != null)
      return dag.foldConstantArithmetic(ISD.SUB, vt, c1, c2);
    if (c2 != null)
      return dag.getNode(ISD.ADD, vt, n0,
          dag.getConstant(c2.getAPIntValue().negative(), vt, false));
    // fold (A+B)-A -> B
    if (n0.getOpcode() == ISD.ADD && n0.getOperand(0).equals(n1))
      return n0.getOperand(1);

    // fold (A+B)-B -> A
    if (n0.getOpcode() == ISD.ADD && n0.getOperand(1).equals(n1))
      return n0.getOperand(0);
    // fold ((A+(B+or-C))-B) -> A+or-C
    if (n0.getOpcode() == ISD.ADD) {
      int opc = n0.getOperand(1).getOpcode();
      if ((opc == ISD.ADD || opc == ISD.SUB) &&
          n1.equals(n0.getOperand(1).getOperand(0)))
        return dag.getNode(opc, vt, n0.getOperand(0),
            n0.getOperand(1).getOperand(1));
    }
    // fold ((A+(C+B))-B) -> A+C
    if (n0.getOpcode() == ISD.ADD &&
        n0.getOperand(1).getOpcode() == ISD.ADD &&
        n1.equals(n0.getOperand(1).getOperand(1))) {
      return dag.getNode(ISD.ADD, vt, n0.getOperand(0),
          n0.getOperand(1).getOperand(0));
    }
    // fold ((A-(B-C))-C) -> A-B
    if (n0.getOpcode() == ISD.SUB &&
        n0.getOperand(1).getOpcode() == ISD.SUB &&
        n0.getOperand(1).getOperand(1).equals(n1)) {
      return dag.getNode(ISD.SUB, vt, n0.getOperand(0),
          n0.getOperand(1).getOperand(0));
    }
    // If either operand of a sub is undef, the result is undef
    if (n0.getOpcode() == ISD.UNDEF)
      return n0;
    if (n1.getOpcode() == ISD.UNDEF)
      return n1;
    // If the relocation model supports it, consider symbol offsets.
    if (n0.getNode() instanceof GlobalAddressSDNode) {
      GlobalAddressSDNode ga = (GlobalAddressSDNode) n0.getNode();
      if (!legalOprations && tli.isOffsetFoldingLegal(ga)) {
        if (c2 != null && ga.getOpcode() == ISD.GlobalAddress)
          return dag.getGlobalAddress(ga.getGlobalValue(),
              vt, ga.getOffset() - c2.getSExtValue(), false, 0);

        if (n1.getNode() instanceof GlobalAddressSDNode) {
          GlobalAddressSDNode gad = (GlobalAddressSDNode) n1.getNode();
          if (ga.getGlobalValue().equals(gad.getGlobalValue()))
            return dag.getConstant(ga.getOffset() -
                -gad.getOffset(), vt, false);
        }
      }
    }
    return new SDValue();
  }

  private SDValue visitADD(SDNode n) {
    SDValue n0 = n.getOperand(0);
    SDValue n1 = n.getOperand(1);
    ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n0.getNode() : null;
    ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n1.getNode() : null;
    EVT vt = n0.getValueType();

    // fold vector ops.
    if (vt.isVector()) {
      SDValue foldedOp = simplifyVBinOp(n);
      if (foldedOp.getNode() != null)
        return foldedOp;
    }

    // (add x, undef) --> undef
    if (n0.getOpcode() == ISD.UNDEF)
      return n0;
    if (n1.getOpcode() == ISD.UNDEF)
      return n1;
    if (c1 != null && c2 != null) {
      return dag.foldConstantArithmetic(ISD.ADD, vt, c1, c2);
    }
    if (c1 != null && c2 == null) {
      return dag.getNode(ISD.ADD, vt, n1, n0);
    }
    if (c2 != null && c2.isNullValue())
      return n0;
    if (n0.getNode() instanceof GlobalAddressSDNode) {
      // (add sym, c) --> sym + c
      GlobalAddressSDNode ga = (GlobalAddressSDNode) n0.getNode();
      if (!legalOprations && tli.isOffsetFoldingLegal(ga) &&
          c2 != null && ga.getOpcode() == ISD.GlobalAddress) {
        return dag.getGlobalAddress(ga.getGlobalValue(),
            vt, ga.getOffset() + c2.getSExtValue(), false, 0);
      }
    }
    // fold ((c1-A)+c2) -> (c1+c2)-A
    if (c2 != null && n0.getOpcode() == ISD.SUB) {
      if (n0.getOperand(0).getNode() instanceof ConstantSDNode) {
        ConstantSDNode lhsOp0C = (ConstantSDNode) n0.getOperand(0).getNode();
        return dag.getNode(ISD.SUB, vt, dag.getConstant(c2.getAPIntValue().
            add(lhsOp0C.getAPIntValue()), vt, false), n0.getOperand(1));
      }
    }
    // reassociate add
    SDValue radd = reassociateOps(ISD.ADD, n0, n1);
    if (radd.getNode() != null)
      return radd;
    // fold ((0-A) + B) -> B-A
    if (n0.getOpcode() == ISD.SUB &&
        n0.getOperand(0).getNode() instanceof ConstantSDNode) {
      ConstantSDNode csd = (ConstantSDNode) n0.getOperand(0).getNode();
      if (csd.isNullValue())
        return dag.getNode(ISD.SUB, vt, n1, n0.getOperand(1));
    }
    // fold (A + (0-B)) -> A-B
    if (n1.getOpcode() == ISD.SUB &&
        n1.getOperand(0).getNode() instanceof ConstantSDNode) {
      ConstantSDNode csd = (ConstantSDNode) n1.getOperand(0).getNode();
      if (csd.isNullValue())
        return dag.getNode(ISD.SUB, vt, n0, n1.getOperand(1));
    }
    // fold (A+(B-A)) -> B
    if (n1.getOpcode() == ISD.SUB &&
        n1.getOperand(1).equals(n0))
      return n1.getOperand(1);
    // fold ((B-A)+A) -> B
    if (n0.getOpcode() == ISD.SUB &&
        n0.getOperand(1).equals(n1))
      return n0.getOperand(0);
    // fold (A+(B-(A+C))) to (B-C)
    if (n1.getOpcode() == ISD.SUB &&
        n1.getOperand(1).getOpcode() == ISD.ADD &&
        n1.getOperand(1).getOperand(0).equals(n0)) {
      return dag.getNode(ISD.SUB, vt, n1.getOperand(0),
          n1.getOperand(1).getOperand(1));
    }
    // fold (A+(B-(C+A))) to (B-C)
    if (n1.getOpcode() == ISD.SUB &&
        n1.getOperand(1).getOpcode() == ISD.ADD &&
        n1.getOperand(1).getOperand(1).equals(n0)) {
      return dag.getNode(ISD.SUB, vt, n1.getOperand(0),
          n1.getOperand(1).getOperand(0));
    }
    // fold (A+((B-A)+or-C)) to (B+or-C)
    int opc = n1.getOpcode();
    if ((opc == ISD.ADD || opc == ISD.SUB) &&
        n1.getOperand(0).getOpcode() == ISD.SUB) {
      if (n1.getOperand(0).getOperand(1).equals(n0))
        return dag.getNode(opc, vt, n1.getOperand(0).getOperand(0),
            n1.getOperand(1));
    }
    // fold (A-B)+(C-D) to (A+C)-(B+D) when A or C is constant
    if (n0.getOpcode() == ISD.SUB && n1.getOpcode() == ISD.SUB) {
      if (n0.getOperand(0).getNode() instanceof ConstantSDNode ||
          n1.getOperand(0).getNode() instanceof ConstantSDNode) {
        SDValue t0 = n0.getOperand(0);
        SDValue t1 = n0.getOperand(1);
        SDValue t2 = n1.getOperand(0);
        SDValue t3 = n1.getOperand(1);
        return dag.getNode(ISD.SUB, vt,
            dag.getNode(ISD.ADD, vt, t0, t2),
            dag.getNode(ISD.ADD, vt, t1, t3));
      }
    }
    // TODO 9/18/2019, this is a bug so comment it!!!
    //if (!vt.isVector() && simplifyDemandedBits(new SDValue(n, 0)))
    //  return new SDValue(n, 0);

    // fold (a+b) -> (a|b) iff a and b share no bits.
    if (vt.isInteger() && !vt.isVector()) {
      APInt mask = APInt.getAllOnesValue(vt.getSizeInBits());
      APInt[] lhs = new APInt[2];
      APInt[] rhs = new APInt[2];
      dag.computeMaskedBits(n0, mask, lhs, 0);
      if (lhs[0].getBoolValue()) {
        dag.computeMaskedBits(n1, mask, rhs, 0);
        if (rhs[0].and(lhs[0].not().and(mask)).eq(lhs[0].not().and(mask)) ||
            lhs[0].and(rhs[0].not().and(mask)).eq(rhs[0].not().and(mask))) {
          return dag.getNode(ISD.OR, vt, n0, n1);
        }
      }
    }

    // fold (add (shl (add x, c1), c2), ) -> (add (add (shl x, c2), c1<<c2), )
    if (n0.getOpcode() == ISD.SHL && n0.getNode().hasOneUse()) {
      SDValue result = combineShlAndConstant(n0, n1);
      if (result.getNode() != null) return result;
    }
    if (n1.getOpcode() == ISD.SHL && n1.getNode().hasOneUse()) {
      SDValue result = combineShlAndConstant(n1, n0);
      if (result.getNode() != null) return result;
    }
    return new SDValue();
  }

  private SDValue combineShlAndConstant(SDValue n0, SDValue n1) {
    EVT vt = n0.getValueType();
    SDValue n00 = n0.getOperand(0);
    SDValue n01 = n0.getOperand(1);
    ConstantSDNode n01C = n01.getNode() instanceof ConstantSDNode ?
        (ConstantSDNode) n01.getNode() : null;
    if (n01C != null && n00.getOpcode() == ISD.ADD &&
        n00.getNode().hasOneUse() &&
        n00.getOperand(1).getNode() instanceof ConstantSDNode) {
      n0 = dag.getNode(ISD.ADD, vt,
          dag.getNode(ISD.SHL, vt, n00.getOperand(0), n01),
          dag.getNode(ISD.SHL, vt, n00.getOperand(1), n01));
      return dag.getNode(ISD.ADD, vt, n0, n1);
    }
    return new SDValue();
  }

  private SDValue visitMERGE_VALUES(SDNode n) {
    WorklistRemover remover = new WorklistRemover(this);
    do {
      for (int i = 0, e = n.getNumOperands(); i < e; i++)
        dag.replaceAllUsesOfValueWith(new SDValue(n, i),
            n.getOperand(i), remover);
    } while (!n.isUseEmpty());
    removeFromWorkList(n);
    dag.deleteNode(n);
    return new SDValue(n, 0);
  }

  private static SDValue getInputChainForNode(SDNode n) {
    int numOps = n.getNumOperands();
    if (numOps > 0) {
      if (n.getOperand(0).getValueType().getSimpleVT().simpleVT == MVT.Other)
        return n.getOperand(0);
      else if (n.getOperand(numOps - 1).getValueType().getSimpleVT().simpleVT == MVT.Other)
        return n.getOperand(numOps - 1);
      for (int i = 1; i < numOps - 1; i++)
        if (n.getOperand(i).getValueType().getSimpleVT().simpleVT == MVT.Other)
          return n.getOperand(i);
    }
    return new SDValue();
  }

  private SDValue visitTokenFactor(SDNode n) {
    // If n has two operands, where one has on input
    // chain equal to the other, so 'other' is redundant.
    if (n.getNumOperands() == 2) {
      if (getInputChainForNode(n.getOperand(0).getNode()).equals(n.getOperand(1)))
        return n.getOperand(0);
      if (getInputChainForNode(n.getOperand(1).getNode()).equals(n.getOperand(1)))
        return n.getOperand(1);
    }

    ArrayList<SDNode> tfs = new ArrayList<>();
    ArrayList<SDValue> ops = new ArrayList<>();
    HashSet<SDNode> seenOps = new HashSet<>();
    boolean changed = false;

    tfs.add(n);
    // Iterate through token factors.  The TFs grows when new token factors are
    // encountered.
    for (int i = 0, e = tfs.size(); i < e; i++) {
      SDNode tf = tfs.get(i);
      for (int j = 0, sz = tf.getNumOperands(); j < sz; j++) {
        SDValue op = tf.getOperand(j);
        switch (op.getOpcode()) {
          case ISD.EntryToken:
            changed = true;
            break;
          case ISD.TokenFactor:
          default:
            if (seenOps.add(op.getNode()))
              ops.add(op);
            else
              changed = true;
            break;
        }
      }
    }
    SDValue result = new SDValue();
    if (changed) {
      if (ops.isEmpty())
        result = dag.getEntryNode();
      else
        result = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
            ops);
      return combineTo(n, result, false);
    }
    return result;
  }

  /**
   * Returns a vector_shuffle if it able to transform
   * an AND to a vector_shuffle with the destination vector and a zero vector.
   * <pre>
   * e.g. AND V, <0xffffffff, 0, 0xffffffff, 0>. ==> vector_shuffle V, Zero, <0, 4, 2, 4>.
   * </pre>
   *
   * @param n
   * @return
   */
  private SDValue xformToShuffleWithZero(SDNode n) {
    EVT vt = n.getValueType(0);
    SDValue lhs = n.getOperand(0);
    SDValue rhs = n.getOperand(1);
    if (n.getOpcode() == ISD.AND) {
      if (rhs.getOpcode() == ISD.BIT_CONVERT)
        rhs = rhs.getOperand(0);
      if (rhs.getOpcode() == ISD.BUILD_VECTOR) {
        TIntArrayList indices = new TIntArrayList();
        int numElts = rhs.getNumOperands();
        for (int i = 0; i < numElts; i++) {
          SDValue elt = rhs.getOperand(i);
          if (!(elt.getNode() instanceof ConstantSDNode))
            return new SDValue();
          ConstantSDNode csd = (ConstantSDNode) elt.getNode();
          if (csd.isAllOnesValue())
            indices.add(i);
          else if (csd.isNullValue())
            indices.add(numElts);
          else
            return new SDValue();
        }

        EVT rvt = rhs.getValueType();
        if (!tli.isVectorClearMaskLegal(indices, rvt))
          return new SDValue();

        EVT evt = rvt.getVectorElementType();
        SDValue[] zeroOps = new SDValue[rvt.getVectorNumElements()];
        for (int i = 0; i < zeroOps.length; i++)
          zeroOps[i] = dag.getConstant(0, evt, false);
        SDValue zero = dag.getNode(ISD.BUILD_VECTOR, rvt, zeroOps);
        lhs = dag.getNode(ISD.BIT_CONVERT, rvt, lhs);
        SDValue shuf = dag.getVectorShuffle(rvt, lhs, zero, indices.toArray());
        return dag.getNode(ISD.BIT_CONVERT, vt, shuf);
      }
    }
    return new SDValue();
  }

  private SDValue simplifyVBinOp(SDNode n) {
    if (legalOprations) return new SDValue();

    EVT vt = n.getValueType(0);
    Util.assertion(vt.isVector(), "simplifyVBinOp only works for vector type!");

    EVT eltVT = vt.getVectorElementType();
    SDValue lhs = n.getOperand(0);
    SDValue rhs = n.getOperand(1);
    SDValue shuffle = xformToShuffleWithZero(n);
    if (shuffle.getNode() != null)
      return shuffle;

    if (lhs.getOpcode() == ISD.BUILD_VECTOR && rhs.getOpcode() == ISD.BUILD_VECTOR) {
      ArrayList<SDValue> ops = new ArrayList<>();
      for (int i = 0, e = lhs.getNumOperands(); i < e; i++) {
        SDValue lhsOp = lhs.getOperand(i);
        SDValue rhsOp = rhs.getOperand(i);
        int lhsOpc = lhsOp.getOpcode(), rhsOpc = rhsOp.getOpcode();
        if ((lhsOpc != ISD.UNDEF &&
            lhsOpc != ISD.Constant &&
            lhsOpc != ISD.ConstantFP) ||
            (rhsOpc != ISD.UNDEF &&
                rhsOpc != ISD.Constant &&
                rhsOpc != ISD.ConstantFP)) {
          break;
        }

        if (n.getOpcode() == ISD.SDIV || n.getOpcode() == ISD.UDIV ||
            n.getOpcode() == ISD.FDIV) {
          if ((rhsOpc == ISD.Constant && ((ConstantSDNode) rhsOp.getNode()).isNullValue()) ||
              (rhsOpc == ISD.ConstantFP && ((ConstantFPSDNode) rhsOp.getNode()).getValueAPF().isZero())) {
            break;
          }
        }
        SDValue res = dag.getNode(n.getOpcode(), eltVT, lhsOp, rhsOp);
        Util.assertion(res.getOpcode() == ISD.UNDEF || res.getOpcode() == ISD.Constant ||
            res.getOpcode() == ISD.ConstantFP, "Scalar binop didn't be folded!");

        ops.add(res);
        addToWorkList(res.getNode());
      }
      if (ops.size() == lhs.getNumOperands()) {
        EVT evt = lhs.getValueType();
        return dag.getNode(ISD.BUILD_VECTOR, evt, ops);
      }
    }

    return new SDValue();
  }

  public void addToWorkList(SDNode n) {
    if (n == null) return;
    removeFromWorkList(n);
    workList.push(n);
  }

  public void removeFromWorkList(SDNode n) {
    workList.remove(n);
  }

  public SDValue combineTo(SDNode n, SDValue[] to, boolean addTo) {
    Util.assertion(n.getNumValues() == to.length);
    WorklistRemover remover = new WorklistRemover(this);
    dag.replaceAllUsesWith(n, to, remover);
    if (addTo) {
      for (SDValue v : to) {
        if (v.getNode() != null) {
          addToWorkList(v.getNode());
          addUsersToWorklist(v.getNode());
        }
      }
    }

    if (n.isUseEmpty()) {
      removeFromWorkList(n);
      dag.deleteNode(n);
    }
    return new SDValue(n, 0);
  }

  public SDValue combineTo(SDNode n, ArrayList<SDValue> to, boolean addTo) {
    SDValue[] temp = new SDValue[to.size()];
    to.toArray(temp);
    return combineTo(n, temp, addTo);
  }

  public SDValue combineTo(SDNode n, SDValue res, boolean addTo) {
    ArrayList<SDValue> vals = new ArrayList<>();
    vals.add(res);
    return combineTo(n, vals, addTo);
  }

  public SDValue combineTo(SDNode n, SDValue res0, SDValue res1) {
    return combineTo(n, res0, res1, true);
  }

  public SDValue combineTo(SDNode n, SDValue res0, SDValue res1, boolean addTo) {
    ArrayList<SDValue> vals = new ArrayList<>();
    vals.add(res0);
    vals.add(res1);
    return combineTo(n, vals, addTo);
  }

  public static class WorklistRemover implements DAGUpdateListener {
    private DAGCombiner combiner;

    public WorklistRemover(DAGCombiner cmb) {
      combiner = cmb;
    }

    @Override
    public void nodeDeleted(SDNode node, SDNode e) {
      combiner.removeFromWorkList(node);
    }

    @Override
    public void nodeUpdated(SDNode node) {
      // ignore updates.
    }
  }

  public void commitTargetLoweringOpt(TargetLoweringOpt tlo) {
    WorklistRemover remover = new WorklistRemover(this);
    dag.replaceAllUsesOfValueWith(tlo.oldVal, tlo.newVal, remover);

    addToWorkList(tlo.newVal.getNode());
    addUsersToWorklist(tlo.newVal.getNode());

    if (tlo.oldVal.getNode().isUseEmpty()) {
      removeFromWorkList(tlo.oldVal.getNode());

      for (int i = 0, e = tlo.oldVal.getNode().getNumOperands(); i < e; i++) {
        if (tlo.oldVal.getNode().getOperand(i).getNode().hasOneUse())
          addToWorkList(tlo.oldVal.getNode().getOperand(i).getNode());
      }
      dag.deleteNode(tlo.oldVal.getNode());
    }
  }

  private void addUsersToWorklist(SDNode node) {
    for (SDUse u : node.useList) {
      addToWorkList(u.getNode());
    }
  }
}
