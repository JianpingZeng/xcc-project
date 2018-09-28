package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.codegen.MVT;
import gnu.trove.list.array.TIntArrayList;
import tools.Util;
import utils.tablegen.Init.DefInit;
import utils.tablegen.Init.IntInit;

import java.io.PrintStream;
import java.util.*;

import static backend.codegen.MVT.getEnumName;
import static backend.codegen.MVT.iPTR;
import static utils.tablegen.CodeGenTarget.getValueType;
import static utils.tablegen.EEVT.isUnknown;
import static utils.tablegen.SDNP.SDNPCommutative;
import static utils.tablegen.ValueTypeByHwMode.getValueTypeByHwMode;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class TreePatternNode implements Cloneable {

  /**
   * The type of each node result according to it's HwMode selected.
   * In X86 target, the selected HwMode is Default one.
   */
  private ArrayList<TypeSetByHwMode> types;
  /**
   * The record for the operator if this is an interior node.
   */
  private Record operator;
  /**
   * The init value(e.g. '7') for a leaf.
   */
  private Init val;
  /**
   * The name given to this node with the :$foo notation.
   */
  private String name = "";

  /**
   * The predicate functions to execute on this node to check
   * for a match.  If this list is empty, no predicate is involved.
   */
  private ArrayList<String> predicateFns;
  /**
   * The tranformation functions applied to this record before
   * it can be substituted into the resulting instruction on a
   * pattern match.
   */
  private Record transformFn;
  /**
   * The children of this interior node.
   */
  private ArrayList<TreePatternNode> children;

  public TreePatternNode(Record op, List<TreePatternNode> chs, int numResults) {
    types = new ArrayList<>();
    predicateFns = new ArrayList<>();
    operator = op;
    children = new ArrayList<>();
    children.addAll(chs);
    for (; numResults > 0; --numResults)
      types.add(new TypeSetByHwMode());
  }

  public TreePatternNode(Init op, int numResults) {
    types = new ArrayList<>();
    predicateFns = new ArrayList<>();
    val = op;
    children = new ArrayList<>();
    for (; numResults > 0; --numResults)
      types.add(new TypeSetByHwMode());
  }

  public TreePatternNode(Init leaf) {
    types = new ArrayList<>();
    predicateFns = new ArrayList<>();
    val = leaf;
    children = new ArrayList<>();
    types.add(new TypeSetByHwMode());
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isLeaf() {
    return val != null;
  }

  public int getNumTypes() { return types.size(); }

  public ArrayList<TypeSetByHwMode> getExtTypes() { return types; }
  public TypeSetByHwMode getExtType(int resNo) { return types.get(resNo); }
  public void setTypes(ArrayList<TypeSetByHwMode> types) {
    this.types = types;
  }

  public ValueTypeByHwMode getType(int resNo) {
    return types.get(resNo).getValueTypeByHwMode();
  }
  public void setType(int resNo, TypeSetByHwMode info) {
    types.set(resNo, info);
  }

  public int getSimpleType(int resNo) {
    return types.get(resNo).getMachineValueType().simpleVT;
  }

  public boolean hasConcreteType(int resNo) {
    return types.get(resNo).isValueTypeByHwMode(false);
  }

  public boolean isTypeCompleteUnknown(int resNo) {
    return types.get(resNo).isEmpty();
  }

  public Init getLeafValue() {
    Util.assertion(isLeaf());
    return val;
  }

  public Record getOperator() {
    Util.assertion(!isLeaf());
    return operator;
  }

  public int getNumChildren() {
    return children.size();
  }

  public TreePatternNode getChild(int idx) {
    return children.get(idx);
  }

  public void setChild(int idx, TreePatternNode node) {
    children.set(idx, node);
  }

  public ArrayList<String> getPredicateFns() {
    return predicateFns;
  }

  public void clearPredicateFns() {
    predicateFns.clear();
  }

  public void setPredicateFns(ArrayList<String> p) {
    if (p == null || p.isEmpty()) {
      predicateFns = new ArrayList<>();
      return;
    }

    if (predicateFns == null)
      predicateFns = new ArrayList<>();
    predicateFns.clear();
    predicateFns.addAll(p);
  }

  public void addPredicateFn(String fn) {
    Util.assertion(fn != null && !fn.isEmpty(), "Empty predicate string!");
    if (!predicateFns.contains(fn))
      predicateFns.add(fn);
  }

  public Record getTransformFn() {
    return transformFn;
  }

  public void setTransformFn(Record transformFn) {
    this.transformFn = transformFn;
  }

  public CodeGenIntrinsic getIntrinsicInfo(CodeGenDAGPatterns cdp) {
    Record operator = getOperator();
    if (operator != cdp.getIntrinsicVoidSDNode() &&
        operator != cdp.getIntrinsicWChainSDNode() &&
        operator != cdp.getIntrinsicWOChainSDNode())
      return null;

    int iid = (int) ((IntInit) getChild(0).getLeafValue()).getValue();
    return cdp.getIntrinsicInfo(iid);
  }

  public boolean isCommutativeIntrinsic(CodeGenDAGPatterns cdp) {
    CodeGenIntrinsic intrinsic = getIntrinsicInfo(cdp);
    return intrinsic != null && intrinsic.isCommutative;
  }

  public void print(PrintStream os) {
    if (isLeaf())
      getLeafValue().print(os);
    else
      os.printf("(%s", getOperator().getName());
    for (TypeSetByHwMode ts : types) {
      os.print(":");
      os.print(ts.toString());
    }
    if (!isLeaf()) {
      if (getNumChildren() != 0) {
        os.print(" ");
        getChild(0).print(os);
        for (int i = 1, e = getNumChildren(); i != e; i++) {
          os.print(", ");
          getChild(i).print(os);
        }
      }
      os.print(")");
    }

    for (int i = 0, e = predicateFns.size(); i < e; i++)
      os.printf("<<P:%s>>", predicateFns.get(i));
    if (transformFn != null)
      os.printf("<<X:%s>>", transformFn.getName());
    if (!getName().isEmpty())
      os.printf(":$%s", getName());
  }

  public void dump() {
    print(System.err);
  }

  private boolean updateNodeTypeFromInst(int resNo,
                                         Record operand,
                                         TreePattern tp) {
    // The 'unknown' operand indicates that types should be inferred from the
    // context.
    if (operand.isSubClassOf("unknown_class"))
      return false;

    // The Operand class specifies a type directly.
    CodeGenTarget target = tp.getDAGPatterns().getTarget();
    if (operand.isSubClassOf("Operand")) {
      Record r = operand.getValueAsDef("Type");
      return updateNodeType(resNo, getValueTypeByHwMode(r, target.getHwModes()), tp);
    }
    // PointerLikeRegClass has a type that is determined at runtime.
    if (operand.isSubClassOf("PointerLikeRegClass"))
      return updateNodeType(resNo, MVT.iPTR, tp);

    Record rc = null;
    if (operand.isSubClassOf("RegisterClass"))
      rc = operand;
    else if (operand.isSubClassOf("RegisterOperand"))
      rc = operand.getValueAsDef("RegClass");

    Util.assertion(rc != null, "unknown operand type");
    return updateNodeType(resNo,
        new TypeSetByHwMode(target.getRegisterClass(rc).getValueTypes()), tp);
  }

  private boolean isOperandClass(TreePatternNode node, String klass) {
    if (!node.isLeaf())
      return node.getOperator().isSubClassOf(klass);

    DefInit di = node.getLeafValue() instanceof DefInit ?
        (DefInit)node.getLeafValue():null;
    return di != null && di.getDef().isSubClassOf(klass);
  }
  /**
   * Apply all of the type constraints relevant to this node and its children
   * in the tree.  This returns true if it makes a change, false otherwise.
   * If a type contradiction is found, issue an error.
   * @param tp
   * @param notRegisters
   * @return
   */
  public boolean applyTypeConstraints(TreePattern tp, boolean notRegisters) {
    CodeGenDAGPatterns cdp = tp.getDAGPatterns();
    if (isLeaf()) {
      DefInit di = getLeafValue() instanceof DefInit ? (DefInit) getLeafValue() : null;
      if (di != null) {
        boolean changed = false;
        for (int i = 0, e = getNumTypes(); i < e; i++)
          changed |= updateNodeType(i, getImplicitType(di.getDef(), i,
              notRegisters, !hasName(), tp), tp);
        return changed;
      }
      else if (getLeafValue() instanceof IntInit) {
        IntInit ii = (IntInit) getLeafValue();
        // int inits are always integers.
        boolean madeChanged = tp.getTypeInfer().enforceInteger(types.get(0));
        if (!tp.getTypeInfer().isConcrete(types.get(0), false))
          return madeChanged;

        ValueTypeByHwMode vvt = tp.getTypeInfer().getConcrete(types.get(0), false);
        for (Map.Entry<Integer, MVT> itr : vvt.map.entrySet()) {
          int vt = itr.getValue().simpleVT;
          if (vt == MVT.iPTR || vt == MVT.iPTRAny)
            continue;
          int size = itr.getValue().getSizeInBits();
          if (size >= 32)
            continue;

          long signBitAndAbove = ii.getValue() >> (size-1);
          if (signBitAndAbove == -1 || signBitAndAbove == 0 ||
              signBitAndAbove == 1)
            continue;
          tp.error(String.format("Integer value '%s' is out of range for type '%s'!",
              ii.getValue(), getEnumName(vt)));
          break;
        }
        return madeChanged;
      }
      return false;
    }

    CodeGenIntrinsic intrinsic;
    if (getOperator().getName().equals("set")) {
      Util.assertion(getNumChildren() >= 2, "Missing RHS of a set?");
      int nc = getNumChildren();

      TreePatternNode setVal = getChild(nc-1);
      boolean madeChanged = setVal.applyTypeConstraints(tp, notRegisters);

      for (int i = 0; i < nc - 1; i++) {
        TreePatternNode child = getChild(i);
        madeChanged |= child.applyTypeConstraints(tp, notRegisters);
        madeChanged |= child.updateNodeType(0, setVal.getExtType(i), tp);
        // Types of operands must match.
        madeChanged |= setVal.updateNodeType(i, child.getExtType(0), tp);
      }
      return madeChanged;
    } else if (getOperator().getName().equals("implicit") ||
        getOperator().getName().equals("parallel")) {
      Util.assertion(getNumTypes() == 0, "Node doesn't produce a value");

      boolean madeChanged = false;
      for (int i = 0; i < getNumChildren(); i++) {
        madeChanged = getChild(i).applyTypeConstraints(tp, notRegisters);
      }
      return madeChanged;
    } else if (getOperator().getName().equals("COPY_TO_REGCLASS")) {
      boolean madeChanged;
      madeChanged = getChild(0).applyTypeConstraints(tp, notRegisters);
      madeChanged |= getChild(1).applyTypeConstraints(tp, notRegisters);
      madeChanged |= updateNodeType(0, getChild(1).getExtType(0), tp);
      return madeChanged;
    } else if ((intrinsic = getIntrinsicInfo(cdp)) != null) {
      boolean madeChange = false;

      int numRetVTs = intrinsic.is.retVTs.size();
      int numParamVTs = intrinsic.is.paramVTs.size();

      for (int i = 0; i != numRetVTs; i++)
        madeChange |= updateNodeType(i, intrinsic.is.retVTs.get(i), tp);

      if (getNumChildren() != numParamVTs + numRetVTs) {
        tp.error("Intrinsic '" + intrinsic.name + "' expects " + (
            numParamVTs + numRetVTs - 1) + " operands, not " + (
            getNumChildren() - 1) + " operands!");
      }

      madeChange |= getChild(0).updateNodeType(0, iPTR, tp);

      for (int i = numRetVTs, e = getNumChildren(); i != e; i++) {
        int opVT = intrinsic.is.paramVTs.get(i - numRetVTs);
        madeChange |= getChild(i).updateNodeType(0, opVT, tp);
        madeChange |= getChild(i).applyTypeConstraints(tp, notRegisters);
      }

      return madeChange;
    } else if (getOperator().isSubClassOf("SDNode")) {
      SDNodeInfo ni = cdp.getSDNodeInfo(getOperator());

      if (ni.getNumOperands() >= 0 &&
          getNumChildren() != ni.getNumOperands()) {
        tp.error(String.format("%s node requires exactly %d operands",
            getOperator().getName(), ni.getNumOperands()));
        return false;
      }

      boolean madeChanged = false;

      for (int i = 0, e = getNumChildren(); i != e; ++i) {
        madeChanged |= getChild(i).applyTypeConstraints(tp, notRegisters);
      }
      madeChanged |= ni.applyTypeConstraints(this, tp);
      return madeChanged;
    } else if (getOperator().isSubClassOf("Instruction")) {
      // FIXME, 9/28/2018, PseudoCall should be following::
      // PseudoCALL (texternalsym:{ *:[i32] m1:[i32] m2:[i64] }):$func)$52 = void
      // BUt is is "(PseudoCALL (texternalsym:{}):$func)42 zextloadi16"
      DAGInstruction instr = cdp.getInstruction(getOperator());
      boolean madeChanged = false;
      int numResults = instr.getNumResults();

      Util.assertion(numResults <= 1, "Only supports zero or one result instrs!");
      CodeGenInstruction instInfo = cdp.getTarget().getInstruction(getOperator().getName());

      // Apply the result types to the node, these come from the things in the
      // (outs) list of the intruction.
      int numResultsToAdd = Math.min(instInfo.numDefs, instr.getNumResults());
      for (int resNo = 0; resNo < numResultsToAdd; resNo++)
        madeChanged |= updateNodeTypeFromInst(resNo, instr.getResult(resNo), tp);

      // if the instruction has implicit defs, we apply the first one as a result.
      if (!instInfo.implicitDefs.isEmpty()) {
        int vt = instInfo.hasOneImplicitDefWithKnownVT(cdp.getTarget());
        if (vt != MVT.Other)
          madeChanged |= updateNodeType(numResultsToAdd, vt, tp);
      }

      if (getOperator().getName().equals("INSERT_SUBREG")) {
        Util.assertion(getChild(0).getNumTypes() ==1, "FIXME: Unhandled");
        madeChanged |= updateNodeType(0, getChild(0).getExtType(0), tp);
        madeChanged |= getChild(0).updateNodeType(0, getExtType(0), tp);
      }
      else if (getOperator().getName().equals("REG_SEQUENCE")) {
        // We need to do extra, custom typechecking for REG_SEQUENCE since it is
        // variadic.
        int numChild = getNumChildren();
        if (numChild < 3) {
          tp.error("REG_SEQUENCE requires at least 3 operands!");
          return false;
        }

        if (numChild %2 == 0) {
          tp.error("REG_SEQUENCE requires an odd number of operands!");
          return false;
        }
        if (!isOperandClass(getChild(0), "RegisterClass")) {
          tp.error("REG_SEQUENCE requires a RegisterClass for first operands!");
          return false;
        }
        for (int i = 1; i < numChild; i++) {
          TreePatternNode node = getChild(i+1);
          if (!isOperandClass(node, "SubRegIndex")) {
            tp.error(String.format("REG_SEQUENCE requires a SubRegIndex for operand #%d!", i+1));
            return false;
          }
        }
      }
      /*
      if (numResults == 0 || instInfo.numDefs == 0) {
        madeChanged = updateNodeType(0, isVoid, tp);
      } else {
        Record resultNode = instr.getResult(0);
        ValueTypeByHwMode vvt = new ValueTypeByHwMode();
        if (resultNode.isSubClassOf("PointerLikeRegClass")) {
          vvt.getOrCreateTypeForMode(DefaultMode, new MVT(MVT.iPTR));
          madeChanged = updateNodeType(0, vvt, tp);
        } else if (resultNode.getName().equals("unknown")) {
          vvt.getOrCreateTypeForMode(DefaultMode, new MVT(isUnknown));
          madeChanged = updateNodeType(0, vvt, tp);
        } else {
          Util.assertion(resultNode.isSubClassOf("RegisterClass"), "Operands should be register class");
          CodeGenRegisterClass rc = cdp.getTarget().getRegisterClass(resultNode);
          madeChanged = updateNodeType(0, new TypeSetByHwMode(rc.getValueTypes()), tp);
        }
      }*/

      int childNo = 0;
      for (int i = 0, e = instr.getNumOperands(); i != e; i++) {
        Record operandNode = instr.getOperand(i);

        // If the instruction expects a predicate or optional def operand, we
        // codegen this by setting the operand to it's default value if it has a
        // non-empty DefaultOps field.
        if ((operandNode.isSubClassOf("PredicateOperand") ||
            operandNode.isSubClassOf("OptionalDefOperand")) &&
            !cdp.getDefaultOperand(operandNode).defaultOps.isEmpty()) {
          continue;
        }

        if (childNo >= getNumChildren()) {
          tp.error("Instruction '" + getOperator().getName() +
              "' expects more operands than were provided.");
        }

        int vt;
        TreePatternNode child = getChild(childNo++);
        if (operandNode.isSubClassOf("RegisterClass")) {
          CodeGenRegisterClass rc = cdp.getTarget().getRegisterClass(operandNode);
          madeChanged |= child.updateNodeType(0, new TypeSetByHwMode(rc.getValueTypes()), tp);
        } else if (operandNode.isSubClassOf("Operand")) {
          vt = getValueType(operandNode.getValueAsDef("Type"));
          madeChanged |= child.updateNodeType(0, vt, tp);
        } else if (operandNode.isSubClassOf("PointerLikeRegClass")) {
          madeChanged |= child.updateNodeType(0, iPTR, tp);
        } else if (operandNode.getName().equals("unknown")) {
          madeChanged |= child.updateNodeType(0, isUnknown, tp);
        } else {
          Util.assertion("Undefined operand type!");
          System.exit(0);
        }
        madeChanged |= child.applyTypeConstraints(tp, notRegisters);
      }

      if (childNo != getNumChildren())
        tp.error("Instruction '" + getOperator().getName() +
            "' was provided too many operands!");

      return madeChanged;
    }
    else {
      Util.assertion(getOperator().isSubClassOf("SDNodeXForm"), "Undefined node type!");

      if (getNumChildren() != 1) {
        tp.error("Node transform '" + getOperator().getName() +
            "' requires one operand!");
      }

      boolean madeChanged = getChild(0).applyTypeConstraints(tp, notRegisters);
      return madeChanged;
    }
  }

  private boolean hasName() {
    return name != null && !name.isEmpty();
  }

  /**
   * Check to see if the specified record has an implicit
   * type which should be applied to it.  This will infer the type of register
   * references from the register file information, for example.
   *
   * @param r
   * @param notRegisters
   * @param tp
   * @return
   */
  private TypeSetByHwMode getImplicitType(Record r,
                                          int resNo,
                                          boolean notRegisters,
                                          boolean unNamed,
                                          TreePattern tp) {
    if (r.isSubClassOf("RegisterOperand")) {
      Util.assertion(resNo == 0, "Register operand ref only has one result!");
      if (notRegisters)
        return new TypeSetByHwMode();
      Record rec = r.getValueAsDef("RegClass");
      CodeGenTarget target = tp.getDAGPatterns().getTarget();
      return new TypeSetByHwMode(target.getRegisterClass(rec).getValueTypes());
    }
    if (r.isSubClassOf("RegisterClass")) {
      Util.assertion(resNo == 0, "Register class ref only has one result!");
      if (unNamed)
        return new TypeSetByHwMode(MVT.i32);

      // Unknown.
      if (notRegisters)
        return new TypeSetByHwMode();
      CodeGenRegisterClass rc = tp.getDAGPatterns().getTarget().getRegisterClass(r);
      return new TypeSetByHwMode(rc.getValueTypes());
    } else if (r.isSubClassOf("PatFlag")) {
      Util.assertion(resNo == 0, "PatFrag ref only has one result!");
      // Pattern fragment types will be resolved when they are inlined.
      return new TypeSetByHwMode(); // unknown
    } else if (r.isSubClassOf("Register")) {
      Util.assertion(resNo == 0, "Register only produce one result!");
      if (notRegisters)
        return new TypeSetByHwMode(); // unknown.

      CodeGenTarget target = tp.getDAGPatterns().getTarget();
      return new TypeSetByHwMode(target.getRegisterVTs(r));
    } else if (r.isSubClassOf("ValueType")) {
      Util.assertion(resNo == 0, "ValueType only has one result!");
      // Using a VTSDNode or CondCodeSDNode.
      if (unNamed)
        return new TypeSetByHwMode(MVT.Other);
      if (notRegisters)
        return new TypeSetByHwMode(); // unknown.
      CodeGenHwModes cgh = tp.getDAGPatterns().getTarget().getHwModes();
      return new TypeSetByHwMode(getValueTypeByHwMode(r, cgh));
    } else if (r.isSubClassOf("CondCode")) {
      Util.assertion(resNo == 0, "CodeCode only has one result!");
      return new TypeSetByHwMode(MVT.Other);
    }
    else if (r.isSubClassOf("ComplexPattern")) {
      Util.assertion(resNo == 0, "ComplexPattern only has one result!");
      if (notRegisters)
        return new TypeSetByHwMode();
      return new TypeSetByHwMode(tp.getDAGPatterns().getComplexPattern(r).getValueType());
    } else if (r.isSubClassOf("PointerLikeRegClass")) {
      Util.assertion(resNo == 0, "PointerLikeRegClass only has one result!");
      TypeSetByHwMode vts = new TypeSetByHwMode(MVT.iPTR);
      tp.getTypeInfer().expandOverloads(vts);
      return vts;
    } else if (r.getName().equals("node") || r.getName().equals("srcvalue")
        || r.getName().equals("zero_reg")) {
      return new TypeSetByHwMode(); // unknown.
    }

    tp.error("Undefined node flavour used in pattern: " + r.getName());
    return new TypeSetByHwMode(MVT.Other);
  }

  public boolean containsUnresolvedType(TreePattern tp) {

    for (TypeSetByHwMode vts : types) {
      if (!tp.getTypeInfer().isConcrete(vts, true))
        return true;
    }

    for (TreePatternNode node : children)
      if (node.containsUnresolvedType(tp))
        return true;
    return false;
  }

  @Override
  public TreePatternNode clone() {
    TreePatternNode res;
    if (isLeaf())
      res = new TreePatternNode(getLeafValue());
    else {
      ArrayList<TreePatternNode> childs = new ArrayList<>();
      children.forEach(ch -> childs.add(ch.clone()));
      res = new TreePatternNode(getOperator(), childs, getNumTypes());
    }
    res.setName(getName());
    /// FIXME, use deep copy instead of reference assignment. 9/28/2018
    res.types = new ArrayList<>();
    for (TypeSetByHwMode ts : types)
      res.types.add(ts.clone());

    res.setPredicateFns(getPredicateFns());
    res.setTransformFn(getTransformFn());
    return res;
  }

  /**
   * If it is impossible for this pattern to match on this
   * target, fill in Reason and return false.  Otherwise, return true.  This is
   * used as a sanity check for .td files (to prevent people from writing stuff
   * that can never possibly work), and to prevent the pattern permuter from
   * generating stuff that is useless.
   *
   * @param reason
   * @param cdp
   * @return
   */
  public boolean canPatternMatch(StringBuilder reason,
                                 CodeGenDAGPatterns cdp) {
    if (isLeaf()) return true;

    for (TreePatternNode node : children) {
      if (!node.canPatternMatch(reason, cdp))
        return false;
    }

    // If this is an intrinsic, handle cases that would make it not match.  For
    // example, if an operand is required to be an immediate.
    if (getOperator().isSubClassOf("Intrinsic"))
      return true;

    SDNodeInfo nodeInfo = cdp.getSDNodeInfo(getOperator());
    boolean isCommIntrinsic = isCommutativeIntrinsic(cdp);
    if (nodeInfo.hasProperty(SDNPCommutative) || isCommIntrinsic) {
      // Scan all of the operands of the node and make sure that only the last one
      // is a constant node, unless the RHS also is.
      if (!onlyOnRHSOfCommutative(getChild(getNumChildren() - 1))) {
        int skip = isCommIntrinsic ? 1 : 0;
        for (int i = skip; i < getNumChildren() - 1; i++) {
          if (onlyOnRHSOfCommutative(getChild(i))) {
            reason.append("Immediate value must be on the RHS of commutative operators!");
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Return true if this value is only allowed on the
   * RHS of a commutative operation, not the on LHS.
   *
   * @param node
   * @return
   */
  private static boolean onlyOnRHSOfCommutative(TreePatternNode node) {
    if (!node.isLeaf() && node.getOperator().getName().equals("imm"))
      return true;
    if (node.isLeaf() && (node.getLeafValue() instanceof IntInit))
      return true;

    return false;
  }

  /**
   * Return true if this node is recursively
   * isomorphic to the specified node.  For this comparison, the node's
   * entire state is considered. The assigned name is ignored, since
   * nodes with differing names are considered isomorphic. However, if
   * the assigned name is present in the dependent variable set, then
   * the assigned name is considered significant and the node is
   * isomorphic if the names match.
   *
   * @param node
   * @param depVars
   * @return
   */
  public boolean isIsomorphicTo(TreePatternNode node,
                                HashSet<String> depVars) {
    if (node == this) return true;
    if (node.isLeaf() != isLeaf() || !Objects.equals(getExtTypes(), node.getExtTypes())
        || !Objects.equals(getPredicateFns(), node.getPredicateFns()) ||
        !Objects.equals(getTransformFn(), node.getTransformFn()))
      return false;

    if (isLeaf()) {
      DefInit di = getLeafValue() instanceof DefInit ? (DefInit) getLeafValue() : null;
      if (di != null) {
        DefInit ndi = node.getLeafValue() instanceof DefInit ? (DefInit) node.getLeafValue() : null;
        if (ndi != null) {
          return di.getDef().equals(ndi.getDef()) && (!depVars.contains(getName())
              || getName().equals(node.getName()));
        }
      }
      return getLeafValue().equals(node.getLeafValue());
    }

    if (node.getOperator() != getOperator() || node.getNumChildren() != getNumChildren())
      return false;

    for (int i = 0, e = getNumChildren(); i != e; i++)
      if (!getChild(i).isIsomorphicTo(node.getChild(i), depVars))
        return false;

    return true;
  }

  public boolean updateNodeType(int resNo,
                                TypeSetByHwMode set,
                                TreePattern tp) {
    tp.getTypeInfer().expandOverloads(set);
    return tp.getTypeInfer().mergeInTypeInfo(types.get(resNo), set);
  }
  public boolean updateNodeType(int resNo,
                                int simpleVT,
                                TreePattern tp) {
    TypeSetByHwMode set = new TypeSetByHwMode(simpleVT);
    return updateNodeType(resNo, set, tp);
  }
  public boolean updateNodeType(int resNo,
                                ValueTypeByHwMode vvt,
                                TreePattern tp) {
    TypeSetByHwMode set = new TypeSetByHwMode(vvt);
    return updateNodeType(resNo, set, tp);
  }

  private boolean lhsIsSubsetOfRHS(
      TIntArrayList lhs,
      TIntArrayList rhs) {
    if (lhs.size() > rhs.size()) return false;
    for (int i = 0, e = lhs.size(); i != e; i++)
      if (!rhs.contains(lhs.get(i)))
        return false;

    return true;
  }

  /**
   * If this pattern refers to any pattern fragments, inline them into place,
   * giving us a pattern without any PatFrag references.
   *
   * @param pattern
   * @return
   */
  public TreePatternNode inlinePatternFragments(TreePattern pattern) {
    // nothing to de.
    if (isLeaf()) return this;
    Record op = getOperator();
    if (!op.isSubClassOf("PatFrag")) {
      // We the operator is not a subclass of PatFrag which means current
      // operator is a def.
      for (int i = 0, e = getNumChildren(); i != e; i++) {
        TreePatternNode child = getChild(i);
        TreePatternNode newChild = child.inlinePatternFragments(pattern);
        Util.assertion((child.getPredicateFns().isEmpty() ||
                newChild.getPredicateFns().equals(child.getPredicateFns())),
            "Non-empty child predicate clobbered!");
        setChild(i, newChild);
      }
      return this;
    }

    // Otherwise, we found a reference to a fragment. First, look up to the
    // TreePattern record.

    // Replace all references to the formal argument in definition of PatFrag
    // with corresponding TreePatternNode object in use of PatFrag.
    // Uses following example as illustration:
    //
    // def sextloadi1  : PatFrag<(ops node:$ptr), (sextload node:$ptr)>
    // we will replace all reference to the first argument in sextload with
    // TreePatternNode corresponding to 'node'.
    TreePattern frag = pattern.getDAGPatterns().getPatternFragment(op);

    // Verify that we are passing the right number of operands.
    if (frag.getNumArgs() != children.size()) {
      pattern.error("'" + op.getName() + "' fragment requires " +
          frag.getNumArgs() + " operands!");
    }

    // Clone is needed caused we will modify it and doesn't affect the other node.
    //
    // def sextload  : PatFrag<(ops node:$ptr), (unindexedload node:$ptr)>
    // fragTree is TreePatternNode corresponding to unindexedload
    TreePatternNode fragTree = frag.getOnlyTree().clone();

    // children tree node inherits predicate function from it's parent node.
    String code = op.getValueAsCode("Predicate");
    if (code != null && !code.isEmpty())
      fragTree.addPredicateFn("predicate_" + op.getName());

    // Resolve formal arguments to their actual value.
    if (frag.getNumArgs() != 0) {
      // Compute the map of formal to actual arguments.
      HashMap<String, TreePatternNode> argMap = new HashMap<>();
      for (int i = 0, e = frag.getNumArgs(); i != e; i++)
        argMap.put(frag.getArgName(i), getChild(i).inlinePatternFragments(pattern));

      fragTree.substituteFromalArguments(argMap);
    }

    fragTree.setName(getName());
    for (int i = 0, e = getNumTypes(); i < e; i++)
      fragTree.updateNodeType(i, getExtType(i), pattern);

    // Transfer in the old predicateFns.
    getPredicateFns().forEach(fragTree::addPredicateFn);

    // The fragment we inlined could have recursive inlining that is needed.  See
    // if there are any pattern fragments in it and inline them as needed.
    return fragTree.inlinePatternFragments(pattern);
  }

  /**
   * Replace the formal arguments in this tree with actual values specified
   * by ArgMap.
   *
   * @param argMap
   */
  private void substituteFromalArguments(HashMap<String, TreePatternNode> argMap) {
    if (isLeaf())
      return;

    for (int i = 0, e = getNumChildren(); i != e; i++) {
      TreePatternNode child = getChild(i);
      if (child.isLeaf()) {
        Init val = child.getLeafValue();
        if (val instanceof DefInit &&
            ((DefInit) val).getDef().getName().equals("node")) {
          // We found a use of a formal argument, replace it with its
          // value.
          Util.assertion(argMap.containsKey(child.getName()), "Couldn't find formal argument!");
          TreePatternNode newChild = argMap.get(child.getName());
          Util.assertion(child.getPredicateFns().isEmpty() ||
                  newChild.getPredicateFns().equals(child.getPredicateFns()),
              "Non empty child predicate clobbered!");
          setChild(i, newChild);
        }
      } else {
        getChild(i).substituteFromalArguments(argMap);
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass())
      return false;
    TreePatternNode node = (TreePatternNode) obj;
    return Objects.equals(types, node.types) &&
        Objects.equals(operator, node.operator) &&
        Objects.equals(val, node.val) &&
        Objects.equals(name, node.name) &&
        Objects.equals(predicateFns, node.predicateFns) &&
        Objects.equals(transformFn, node.transformFn) &&
        Objects.equals(children, node.children);
  }

  /**
   * Reset as unknown type.
   */
  public void removeTypes() {
    int size = getNumTypes();
    for (int i = 0; i < size; i++)
      types.set(i, new TypeSetByHwMode());
  }
}
