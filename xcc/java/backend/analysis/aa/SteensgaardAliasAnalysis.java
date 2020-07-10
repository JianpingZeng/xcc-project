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

package backend.analysis.aa;

import backend.ir.AllocationInst;
import backend.ir.SelectInst;
import backend.pass.ModulePass;
import backend.support.CallSite;
import backend.utils.InstVisitor;
import backend.value.*;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.PhiNode;
import backend.value.Module;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.ArrayList;

/**
 * This file defines a class named "SteensGaardAliasAnalysis" in terms of several
 * papers as follows.
 * <ol>
 * <li>"Points-to analysis in almost linear time."</li>
 * <li>Lin, Sheng-Hsiu. Alias Analysis in LLVM. Lehigh University, 2015.</li>
 * </ol>
 * This is a trivial implementation about Steensgaard's paper. I would not to
 * performance some minor optimization, but I would to do in the future.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class SteensgaardAliasAnalysis extends AliasAnalysis implements
    ModulePass, InstVisitor<Void> {
  /**
   * This class used for representing a node in constraint graph.
   * All nodes consists of a constraint graph.
   */
  private static class Node {
    /**
     * This points to it's representative node.
     * I uses a union-find set to operate on constraint.
     */
    int id;
    Node rep;
    Value value;

    Node(int id, Value val) {
      value = val;
      rep = this;
    }

    Node getRepresentativeNode() {
      if (rep == this) return this;
      return rep = rep.getRepresentativeNode();
    }

    boolean isRepresentative() {
      return rep == this;
    }

    void setRepresentative(Node rep) {
      if (this.rep != null)
        this.rep.setRepresentative(rep);
      this.rep = rep;
    }
  }

  /**
   * A maps from value to an integer.
   */
  private TObjectIntHashMap<Value> valueNodes;
  /**
   * A maps from memory object to an integer.
   * Note that We should discriminate the {@linkplain #valueNodes} and this.
   * The {@linkplain #valueNodes} is used for tracking unique id for any LLVM
   * value, for example, global variable, function formal parameter or local
   * variable. But this is only used for recording an unique number of memory
   * object, like global variable, any value allocated by AllocaInst or
   * MallocInst, that can be addressed(in other word, it could be taken address
   * by "&" operator in C-like).
   */
  private TObjectIntHashMap<Value> pointerNodes;
  /**
   * This map used for recording the unique number for function's return value.
   */
  private TObjectIntHashMap<Function> returnNodes;
  /**
   * This map used for recording the unique number for function's vararg value.
   */
  private TObjectIntHashMap<Function> varargNodes;

  private Node[] nodes;
  private Module m;

  SteensgaardAliasAnalysis() {
    valueNodes = new TObjectIntHashMap<>();
    pointerNodes = new TObjectIntHashMap<>();
    returnNodes = new TObjectIntHashMap<>();
    varargNodes = new TObjectIntHashMap<>();
  }

  /**
   * This method couldn't change the control flow graph of being analyzed LLVM
   * IR module. It will performs three steps to transform Alias problem into
   * a constraint-based method, and achieving the alias information by quering
   * those constraints information.
   *
   * @param m
   * @return
   */
  @Override
  public boolean runOnModule(Module m) {
    this.m = m;
    identifyObjects();
    collectConstraints();
    return false;
  }

  private static final int NullObject = 0;
  private static final int NullPtr = 1;
  private static final int Universal = 2;
  private static final int NumSpecialValue = Universal + 1;

  private void identifyObjects() {
    int numObjects;
    numObjects = NumSpecialValue;
    // Handle Global variables.
    for (GlobalVariable gv : m.getGlobalVariableList()) {
      valueNodes.put(gv, numObjects++);
      pointerNodes.put(gv, numObjects++);
    }

    // handle Functions.
    for (Function fn : m.getFunctionList()) {
      // Because function can be treated as the target of function pointer,
      // so we need to keep track of the value id of function.
      valueNodes.put(fn, numObjects++);
      if (fn.getReturnType().isPointerType())
        returnNodes.put(fn, numObjects++);
      if (fn.getFunctionType().isVarArg())
        varargNodes.put(fn, numObjects++);

      // Walk through Function body in a order that doesn't care execution path
      // of program.
      for (BasicBlock bb : fn) {
        Util.assertion(!bb.isEmpty(), "Reaching here, there should not have any empty block!");
        for (Instruction inst : bb) {
          if (!inst.getType().isPointerType()) continue;
          // We just care about those instruction of type pointer.
          valueNodes.put(inst, numObjects++);
          if (inst instanceof AllocationInst)
            pointerNodes.put(inst, numObjects++);
        }
      }
    }
    nodes = new Node[numObjects];
  }

  private Node getValueNode(Value val) {
    Util.assertion(val != null && valueNodes.containsKey(val));
    int id = valueNodes.get(val);
    if (nodes[id] != null) return nodes[id];
    return nodes[id] = new Node(id, val);
  }

  private Node getReturnNode(int id) {
    if (nodes[id] != null) return nodes[id];
    return nodes[id] = new Node(id, null);
  }

  private Node getNullPointerNode() {
    if (nodes[NullPtr] != null)
      return nodes[NullPtr];
    return nodes[NullPtr] = new Node(NullPtr, null);
  }

  private Node getNullObjecteNode() {
    if (nodes[NullObject] != null)
      return nodes[NullObject];
    return nodes[NullObject] = new Node(NullObject, null);
  }

  private Node getPointerNode(Value val) {
    Util.assertion(val != null && pointerNodes.containsKey(val));
    int id = pointerNodes.get(val);
    if (nodes[id] != null) return nodes[id];
    return nodes[id] = new Node(id, val);
  }

  private Node getUniversalValueNode() {
    if (nodes[Universal] != null)
      return nodes[Universal];
    return nodes[Universal] = new Node(Universal, null);
  }

  private Node getValueNodeOfConstant(Constant c) {
    if (c instanceof ConstantPointerNull)
      return getNullPointerNode();
    else if (c instanceof GlobalVariable) {
      getValueNode(c);
    } else if (c instanceof ConstantExpr) {
      ConstantExpr ce = (ConstantExpr) c;
      switch (ce.getOpcode()) {
        case GetElementPtr:
          return getValueNodeOfConstant(ce.operand(0));
        case BitCast:
          return getValueNodeOfConstant(ce.operand(0));
        case IntToPtr:
          return getUniversalValueNode();
      }
    }
    Util.assertion(false, "Unknown constant node!");
    return null;
  }

  private void addConstraintsOnGlobalVariable(Node dest, Constant c) {
    if (c.getType().isSingleValueType())
      dest.setRepresentative(getValueNodeOfConstant(c));
    else if (c.isNullValue())
      dest.setRepresentative(getNullObjecteNode());
    else if (!(c instanceof Value.UndefValue)) {
      Util.assertion(c instanceof ConstantArray || c instanceof ConstantStruct);
      for (int i = 0, e = c.getNumOfOperands(); i < e; i++)
        addConstraintsOnGlobalVariable(dest, c.operand(i));
    }
  }

  private void collectConstraints() {
    for (GlobalVariable gv : m.getGlobalVariableList()) {
      // LLVM IR "@x = global i32 1, align 4" could be abstracted into following
      // constraint.
      Node ptrNode = getPointerNode(gv);
      getValueNode(gv).setRepresentative(ptrNode);
      if (gv.getInitializer() != null)
        addConstraintsOnGlobalVariable(ptrNode, gv.getInitializer());
    }
    // Walk through each function to collect constraints on each LLVM instruction.
    for (Function f : m.getFunctionList())
      visit(f);
  }

  @Override
  public AliasResult alias(Value ptr1, int size1, Value ptr2, int size2) {
    if (ptr1 == null || ptr2 == null)
      return AliasResult.NoAlias;
    if (!ptr1.getType().isPointerType() || !ptr2.getType().isPointerType())
      return AliasResult.NoAlias;
    Node node1 = getPointerNode(ptr1);
    Node node2 = getPointerNode(ptr2);
    if (node1 == null || node2 == null)
      return AliasResult.NoAlias;
    if (node1.getRepresentativeNode() == node2.getRepresentativeNode())
      return AliasResult.MustAlias;
    return AliasResult.NoAlias;
  }

  /**
   * Note that, we just take conservative consideration upon must alias collection.
   * If one node is equivalent with other so that we would think the first node
   * must alias with second one.
   *
   * @param ptr
   * @param retVals
   */
  @Override
  public void getMustAliases(Value ptr, ArrayList<Value> retVals) {
    // Non pointer type would not points to anything.
    if (ptr == null || !ptr.getType().isPointerType())
      return;
    Node node = getPointerNode(ptr);
    if (node == null) return;
    retVals.add(node.value);
    for (Node n : nodes) {
      if (n.getRepresentativeNode() == node)
        retVals.add(n.value);
    }
  }

  @Override
  public boolean pointsToConstantMemory(Value ptr) {
    return false;
  }

  @Override
  public ModRefResult getModRefInfo(CallSite cs1, CallSite cs2) {
    return null;
  }

  @Override
  public boolean hasNoModRefInfoForCalls() {
    return false;
  }

  @Override
  public void deleteValue(Value val) {
    if (val == null || !val.getType().isPointerType())
      return;
    if (!pointerNodes.containsKey(val))
      return;
    int id = pointerNodes.get(val);
    nodes[id] = null;
    pointerNodes.remove(val);
  }

  @Override
  public void copyValue(Value from, Value to) {

  }

  @Override
  public String getPassName() {
    return "Steensgaard's style alias analysis";
  }

  /**
   * We could collapse all return values of Function into a single one when
   * there are many return statement in this LLVM Function.
   * So that this strategy would be helpful for handling CallInst.
   *
   * @param inst
   * @return
   */
  @Override
  public Void visitRet(User inst) {
    Function fn = ((Instruction) inst).getParent().getParent();
    Util.assertion(fn != null, "Instruction isn't attacted into a Function!");
    Util.assertion(returnNodes.containsKey(fn), "ReturnInst must be handled in collectConstraints() method before!");

    Node valueNode = getValueNode(inst);
    Node returnNode = getReturnNode(returnNodes.get(fn));
    returnNode.setRepresentative(valueNode);
    return null;
  }

  @Override
  public Void visitBr(User inst) {
    return null;
  }

  @Override
  public Void visitSwitch(User inst) {
    return null;
  }

  @Override
  public Void visitICmp(User inst) {
    return null;
  }

  @Override
  public Void visitFCmp(User inst) {
    return null;
  }

  @Override
  public Void visitCastInst(User u) {
    Instruction inst = (Instruction) u;
    // We just needs to handle such instruction of type pointer type.
    if (!inst.getType().isPointerType())
      return null;

    switch (inst.getOpcode()) {
      case IntToPtr: {
        Node destNode = getPointerNode(inst);
        destNode.setRepresentative(getUniversalValueNode());
        break;
      }
      case BitCast: {
        Node destNode = getPointerNode(inst);
        destNode.setRepresentative(getPointerNode(inst.operand(0)));
        break;
      }
    }
    return null;
  }

  @Override
  public Void visitAllocationInst(User inst) {
    Node srcNode = getPointerNode(inst);
    Util.assertion(srcNode != null, "The operand of allocation inst isn't collected in identifyObjects method!");
    Node destNode = getValueNode(inst);
    destNode.setRepresentative(srcNode);
    return null;
  }

  /**
   * This LLVM instruction is viewed as "a = *b" constraint in original
   * Steensgaard's paper.
   *
   * @param inst
   * @return
   */
  @Override
  public Void visitLoad(User inst) {
    Node valNode = getValueNode(inst);
    Node ptrNode = getPointerNode(inst.operand(0));
    Node derefNode = ptrNode.getRepresentativeNode();
    Util.assertion(derefNode != null, "Deref Node shouldn't be null!");
    valNode.setRepresentative(derefNode);
    return null;
  }

  /**
   * This LLVM instruction is viewed as "*a = b" constraint in original
   * Steensgaard's paper.
   *
   * @param inst
   * @return
   */
  @Override
  public Void visitStore(User inst) {
    Node valNode = getValueNode(inst.operand(0));
    Node ptrNode = getPointerNode(inst.operand(1));
    Util.assertion(valNode != null && ptrNode != null);

    Node derefNode = ptrNode.getRepresentativeNode();
    if (derefNode == null) {
      ptrNode.setRepresentative(valNode);
      return null;
    }
    derefNode.setRepresentative(valNode);
    return null;
  }

  @Override
  public Void visitCall(User u) {
    CallInst inst = (CallInst) u;
    // We would take two steps to handle constraints on the Call instruction as follows.
    // Step 1: treat all passing argument as copy constraint.
    if (inst.getCalledFunction().isVarArg()) {
      Util.assertion(false, "We currently don't handle vararg function call!");
      return null;
    }
    Function calledFn = inst.getCalledFunction();
    for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
      Value arg = inst.getArgOperand(i);
      if (!arg.getType().isPointerType())
        continue;
      Value param = calledFn.argAt(i);
      getPointerNode(param).setRepresentative(getValueNode(arg));
    }
    // Step 2: treat the return value as copy constraint.
    if (!inst.getType().isPointerType())
      return null;
    Node destNode = getPointerNode(inst);
    Util.assertion(returnNodes.containsKey(calledFn), "Called Function has been identified yet?");
    int retId = returnNodes.get(calledFn);
    Node returnNode = getReturnNode(retId);
    Util.assertion(destNode != null && returnNode != null);
    destNode.setRepresentative(returnNode);
    return null;
  }

  /**
   * Since the field-insensitive of Steensgaard's algorithm, we don't discriminate
   * each field of aggregate object, such as Array, struct or union. So that the
   * pointer node of each aggregate is represented by the first operand's node.
   *
   * @param inst
   * @return
   */
  @Override
  public Void visitGetElementPtr(User inst) {
    Node destNode = getPointerNode(inst);
    Util.assertion(destNode != null, "The GEP instruction isn't handled yet in identifyObjects() method!");
    Node srcNode = getPointerNode(inst.operand(0));
    destNode.setRepresentative(srcNode);
    return null;
  }

  @Override
  public Void visitPhiNode(User u) {
    PhiNode inst = (PhiNode) u;
    // Only those phi node with type of pointer are needed to be handled.
    if (!inst.getType().isPointerType())
      return null;

    Node srcNode = null;
    for (int i = 0, e = inst.getNumberIncomingValues(); i < e; i++) {
      if (srcNode == null)
        srcNode = getValueNode(inst.getIncomingValue(i));
      else {
        srcNode.setRepresentative(getValueNode(inst.getIncomingValue(i)));
      }
    }
    Node destNode = getPointerNode(inst);
    Util.assertion(srcNode != null, "A null phi?");
    destNode.setRepresentative(srcNode);
    return null;
  }

  /**
   * This method works as same as {@linkplain #visitPhiNode(User)}.
   * It simplicitly merge all values into a representative one.
   *
   * @param u
   * @return
   */
  @Override
  public Void visitSelect(User u) {
    SelectInst inst = (SelectInst) u;
    if (!inst.getType().isPointerType())
      return null;
    Node destNode = getPointerNode(inst);
    Node src1Node = getValueNode(inst.getTrueValue());
    Node src2Node = getValueNode(inst.getFalseValue());
    Util.assertion(destNode != null && src1Node != null && src2Node != null);
    src2Node.setRepresentative(src1Node);
    destNode.setRepresentative(src1Node);
    return null;
  }
}
