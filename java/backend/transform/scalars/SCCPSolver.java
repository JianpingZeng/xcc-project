package backend.transform.scalars;
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

import backend.utils.InstVisitor;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Value.UndefValue;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * This class implements a well known intra-procedural optimization pass refering
 * to " Wegman, Mark N. and Zadeck, F. Kenneth. Constant Propagation with
 * Conditional Branches."
 * <ul>
 * <li>Assume the LLVM IR value as undefined unless proven otherwise.</li>
 * <li>Assume all of BasicBlock as dead unless proven otherwise.</li>
 * <li>Replace all uses with constant if a value constant proven.</li>
 * <li>Remove all dead instruction in BasicBlock but not to delete
 * since the CFG of function must preserved.</li>
 * </ul>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class SCCPSolver implements InstVisitor<Void> {
  private enum LatticeKind {
    /**
     * The initial status of each LLVM IR vlaue.
     */
    Undefined,

    /**
     * The lattice status of LLVM IR value that represents the
     * value is absolutely assigned with a constant.
     */
    Constant,

    /**
     * A lattice status indices that the associated LLVM IR value is
     * assign with constant multiple times.
     */
    Overdefined
  }

  public static class LatticeStatus {
    /**
     * The kind of lattice value.
     */
    LatticeKind kind;

    /**
     * It is not null if this lattice value is a constant.
     */
    Constant constVal;

    LatticeStatus() {
      kind = LatticeKind.Undefined;
      constVal = null;
    }

    boolean markConstant(Constant val) {
      if (kind != LatticeKind.Constant) {
        kind = LatticeKind.Constant;
        constVal = val;
        return true;
      } else {
        Util.assertion(constVal.equals(val), "undefined up to constant!");
      }
      return false;
    }

    boolean markOverdefined() {
      if (kind == LatticeKind.Overdefined) {
        kind = LatticeKind.Overdefined;
        return true;
      }
      return false;
    }

    public boolean isConstant() {
      return kind == LatticeKind.Constant;
    }

    public boolean isUndefined() {
      return kind == LatticeKind.Undefined;
    }

    public boolean isOverdefined() {
      return kind == LatticeKind.Overdefined;
    }

    public Constant getConstVal() {
      return constVal;
    }
  }

  private HashMap<Value, LatticeStatus> value2LatticeMap;
  private HashSet<BasicBlock> executableBBs;

  private HashSet<Pair<BasicBlock, BasicBlock>> knownFeasibleEdges;

  private Stack<Value> ssaWorklist;

  private Stack<BasicBlock> bbWorklist;

  public SCCPSolver() {
    value2LatticeMap = new HashMap<>();
    executableBBs = new HashSet<>();
    knownFeasibleEdges = new HashSet<>();
    ssaWorklist = new Stack<>();
    bbWorklist = new Stack<>();
  }

  private LatticeStatus getLatticeStatus(Value val) {
    Util.assertion(val != null, "null Value when calling to getLatticeStatus()");
    if (value2LatticeMap.containsKey(val))
      return value2LatticeMap.get(val);

    // Set up the lattice value as Undefined by default.
    LatticeStatus status = new LatticeStatus();
    value2LatticeMap.put(val, status);
    if (val instanceof Constant) {
      if (!(val instanceof UndefValue))
        status.markConstant((Constant) val);
    }
    return status;
  }

  public void markConstant(LatticeStatus ls, Instruction inst, Constant val) {
    if (ls.markConstant(val)) {
      ssaWorklist.push(inst);
    }
  }

  public void markConstant(Instruction inst, Constant val) {
    LatticeStatus ls = getLatticeStatus(inst);
    markConstant(ls, inst, val);
  }

  public void markOverdefined(Value inst) {
    LatticeStatus ls = getLatticeStatus(inst);
    if (ls.markOverdefined()) {
      ssaWorklist.push(inst);
    }
  }

  /**
   * Calling to this method to mark the parent as executable and push it into
   * bbWorklist waiting for sequence operation.
   *
   * @param bb
   */
  public void markBBExecutable(BasicBlock bb) {
    bbWorklist.push(bb);
    executableBBs.add(bb);
  }

  /**
   * This method will be called when lattice status of the given User was
   * changed.
   *
   * @param u
   */
  private void latticeStatusChanged(User u) {
    if (u == null)
      return;

    Instruction inst = (Instruction) u;
    if (executableBBs.contains(inst.getParent()))
      visit(inst);
  }

  /**
   * Starting to solve this sparse dataflow question by walking through
   * the CFG basic block by basic block.
   * <p>
   * The output result of this method should indicates what Lattice value of
   * each LLVM IR value, and whether the basic block is dead or not.
   * </p>
   */
  public void solve() {
    // Iterate control flow graph and SSA graph until both worklist are
    // empty.
    while (!ssaWorklist.isEmpty() || !bbWorklist.isEmpty()) {
      while (!ssaWorklist.isEmpty()) {
        Value inst = ssaWorklist.pop();
        inst.usesList.forEach(u ->
        {
          latticeStatusChanged(u.getUser());
        });
      }

      while (!bbWorklist.isEmpty()) {
        BasicBlock bb = bbWorklist.pop();
        visit(bb);
      }
    }
  }

  /**
   * Gets a HashMap that maps from a lLVM IR value to its corresponding
   * LatticeStatus.
   *
   * @return
   */
  public HashMap<Value, LatticeStatus> getValue2LatticeMap() {
    return value2LatticeMap;
  }

  /**
   * Gets a set contains all BasicBlock is executable.
   *
   * @return
   */
  public HashSet<BasicBlock> getExecutableBBs() {
    return executableBBs;
  }

  //========================================================================//
  // Visitor Implementation
  // In the initial status, the Lattice value of each LLVM IR value is Undefined.
  // It will be promoted to Top status (Overdefined) as the constant information
  // propagation.
  //
  // All of following methods used for handling each kind of LLVM IR instruction.
  // z = x op y, z is Overdefined iif x is constant and y is constant and x != y.
  // z = x op y, z is Constant if x == y and x is constant.
  // z = x op y, z is Overdefined if one of x and y is Overdefined.
  //
  // Another special case is PhiNode.
  // z = phi(x_1, x_2, ..., x_n)
  // If any incoming value of phi is Overdefined, z = Overdefined.
  // Otherwise, each incoming value is Constant,
  // z = Constant iff the lattice value corresponding to each incoming value
  // are Constant and they are same.
  // Otherwise, z = Overdefined.
  //========================================================================//

  public Void visitInstruction(Instruction inst) {
    markOverdefined(inst);
    return null;
  }

  /**
   * Mark the destination basic block as executable.
   *
   * @param from
   * @param to
   */
  private void markEdgeExecutable(BasicBlock from, BasicBlock to) {
    // If this edge already contained in knownFeasibleEdges, just return.
    if (!knownFeasibleEdges.add(Pair.get(from, to)))
      return;

    if (executableBBs.contains(to)) {
      for (Instruction inst : to) {
        // If target node was marked executable, in order to handle
        // the case of destination node is loop header block.
        if (inst instanceof PhiNode)
          visitPhiNode((PhiNode) inst);
      }
    } else {
      executableBBs.add(to);
      bbWorklist.push(to);
    }
  }

  private ArrayList<BasicBlock> getFeasibleSuccessors(TerminatorInst ti) {
    ArrayList<BasicBlock> res = new ArrayList<>();
    if (ti instanceof BranchInst) {
      BranchInst bi = (BranchInst) ti;
      if (bi.isUnconditional()) {
        res.add(bi.getSuccessor(0));
        return res;
      } else {
        LatticeStatus ls = getLatticeStatus(bi.getCondition());
        if (ls.isOverdefined()) {
          res.add(bi.getSuccessor(0));
          res.add(bi.getSuccessor(1));
          return res;
        } else {
          Constant cond = ls.getConstVal();
          int targetIdx = cond.equals(ConstantInt.getFalse()) ? 1 : 0;
          res.add(bi.getSuccessor(targetIdx));
          return res;
        }
      }
    } else if (ti instanceof SwitchInst) {
      SwitchInst si = (SwitchInst) ti;
      LatticeStatus ls = getLatticeStatus(si.getCondition());
      if (ls.isOverdefined()) {
        for (int i = 0, e = si.getNumOfSuccessors(); i != e; i++)
          res.add(si.getSuccessor(i));
        return res;
      } else if (ls.isConstant()) {
        Constant cond = ls.getConstVal();
        for (int i = 0, e = si.getNumOfSuccessors(); i != e; i++) {
          if (si.getSuccessorValue(i).equals(cond)) {
            res.add(si.getSuccessor(i));
            return res;
          }
        }

        res.add(si.getDefaultBlock());
        return res;
      }
    }
    Util.assertion(false, "Unknown terminator instruction");
    return null;
  }

  private void visitTerminatorInst(TerminatorInst ti) {
    ArrayList<BasicBlock> succBB = getFeasibleSuccessors(ti);

    BasicBlock parent = ti.getParent();
    succBB.forEach(to ->
    {
      markEdgeExecutable(parent, to);
    });
  }

  public Void visitBinaryOp(Instruction.BinaryOps inst) {
    LatticeStatus ls = getLatticeStatus(inst);
    if (ls.isOverdefined())
      return null;

    LatticeStatus op1Status = getLatticeStatus(inst.operand(0));
    LatticeStatus op2Status = getLatticeStatus(inst.operand(1));
    if (op1Status.isOverdefined() || op2Status.isOverdefined()) {
      markOverdefined(inst);
      return null;
    }

    if (op1Status.isConstant() && op2Status.isConstant()) {
      Constant val = ConstantExpr.get(inst.getOpcode(), op1Status.constVal, op2Status.constVal);
      markConstant(ls, inst, val);
    }
    return null;
  }

  private void visitCmpInst(Instruction.CmpInst inst) {
    LatticeStatus ls = getLatticeStatus(inst);
    if (ls.isOverdefined())
      return;

    LatticeStatus op1Status = getLatticeStatus(inst.operand(0));
    LatticeStatus op2Status = getLatticeStatus(inst.operand(1));
    if (op1Status.isOverdefined() || op2Status.isOverdefined()) {
      markOverdefined(inst);
      return;
    }

    if (op1Status.isConstant() && op2Status.isConstant()) {
      Constant val = ConstantExpr.getCompare(inst.getPredicate(),
          op1Status.constVal, op2Status.constVal);
      markConstant(ls, inst, val);
    }
  }

  public Void visitCastInst(Instruction.CastInst inst) {
    LatticeStatus ls = getLatticeStatus(inst);
    if (ls.isOverdefined())
      return null;

    LatticeStatus op1Status = getLatticeStatus(inst.operand(0));
    if (op1Status.isOverdefined()) {
      markOverdefined(inst);
      return null;
    }
    if (op1Status.isConstant()) {
      markConstant(ls, inst, ConstantExpr.getCast(inst.getOpcode(),
          op1Status.constVal, inst.getType()));
    }
    return null;
  }

  @Override
  public Void visitRet(User inst) {
    markOverdefined(inst);
    return null;
  }

  @Override
  public Void visitBr(User inst) {
    visitTerminatorInst((TerminatorInst) inst);
    return null;
  }

  @Override
  public Void visitSwitch(User inst) {
    visitTerminatorInst((TerminatorInst) inst);
    return null;
  }

  @Override
  public Void visitAdd(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitFAdd(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitSub(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitFSub(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitMul(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitFMul(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitUDiv(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitSDiv(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitFDiv(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitURem(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitSRem(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitFRem(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitAnd(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitOr(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitXor(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitShl(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitLShr(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitAShr(User inst) {
    visitBinaryOp(inst);
    return null;
  }

  @Override
  public Void visitICmp(User inst) {
    visitCmpInst((Instruction.CmpInst) inst);
    return null;
  }

  @Override
  public Void visitFCmp(User inst) {
    visitCmpInst((Instruction.CmpInst) inst);
    return null;
  }

  @Override
  public Void visitCastInst(User inst) {
    Operator opc = ((CastInst) inst).getOpcode();
    Value v = inst.operand(0);
    LatticeStatus ls = getLatticeStatus(v);
    if (ls.markOverdefined())
      markOverdefined(inst);
    else if (ls.isConstant())
      markConstant((Instruction) inst,
          ConstantExpr.getCast(opc, ls.getConstVal(),
              inst.getType()));
    return null;
  }

  @Override
  public Void visitAlloca(User inst) {
    markOverdefined(inst);
    return null;
  }

  @Override
  public Void visitMalloc(User inst) {
    markOverdefined(inst);
    return null;
  }

  @Override
  public Void visitLoad(User inst) {
    markOverdefined(inst);
    return null;
  }

  @Override
  public Void visitStore(User inst) {
    // Empty statement, skip this StoreInst.
    return null;
  }

  @Override
  public Void visitCall(User inst) {
    markOverdefined(inst);
    return null;
  }

  @Override
  public Void visitGetElementPtr(User u) {
    GetElementPtrInst inst = (GetElementPtrInst) u;
    if (getLatticeStatus(inst).isOverdefined())
      return null;

    ArrayList<Constant> ops = new ArrayList<>();
    for (int i = 0, e = inst.getNumOfOperands(); i != e; i++) {
      Value op = inst.operand(i);
      LatticeStatus ls = getLatticeStatus(op);
      if (ls.isUndefined())
        return null;
      if (ls.isOverdefined()) {
        markOverdefined(inst);
        return null;
      } else if (ls.isConstant()) {
        ops.add(ls.getConstVal());
      }
      Util.assertion(false, "Unknown Lattice value");
    }

    Value base = inst.getPointerOperand();
    LatticeStatus ls = getLatticeStatus(base);
    if (ls.isOverdefined()) {
      markOverdefined(inst);
      return null;
    } else if (ls.isConstant()) {
      markConstant(inst, ConstantExpr.getGetElementPtr(ls.getConstVal(), ops));
      return null;
    }
    Util.assertion(false, "Unknown Lattice value");
    return null;
  }

  /**
   * Checks if the specified target node is feasible or not.
   *
   * @param from
   * @param to
   * @return
   */
  private boolean isEdgeFeasible(BasicBlock from, BasicBlock to) {
    Util.assertion(executableBBs.contains(to), "Destination block must be executable");

    if (!executableBBs.contains(from))
      return false;

    TerminatorInst ti = from.getTerminator();
    if (ti == null)
      return false;

    if (ti instanceof BranchInst) {
      BranchInst bi = (BranchInst) ti;
      if (bi.isUnconditional())
        return true;

      LatticeStatus ls = getLatticeStatus(bi.getCondition());
      if (ls.isOverdefined())
        return true;
      if (ls.isConstant()) {
        Constant cond = ls.getConstVal();
        return bi.getSuccessor(cond.equals(ConstantInt.getFalse()) ? 1 : 0).equals(to);
      } else
        return false;

    } else if (ti instanceof SwitchInst) {
      SwitchInst si = (SwitchInst) ti;
      LatticeStatus ls = getLatticeStatus(si.getCondition());

      // If this condition is over defined, treat it as true anyway.
      if (ls.isOverdefined())
        return true;

      if (ls.isConstant()) {
        Constant cond = ls.getConstVal();
        for (int i = 0, e = si.getNumOfCases(); i != e; i++) {
          if (cond.equals(si.getCaseValues(i))) {
            if (si.getSuccessor(i) == to)
              return true;
          }
        }

        return si.getDefaultBlock() == to;
      } else {
        return false;
      }
    }
    Util.assertion(false, "Unknown terminator instruction.");
    return false;
  }

  @Override
  public Void visitPhiNode(User u) {
    PhiNode inst = (PhiNode) u;
    LatticeStatus phiLS = getLatticeStatus(inst);

    if (phiLS.isOverdefined())
      return null;

    Constant uniqueConstant = null;
    for (int i = 0, e = inst.getNumberIncomingValues(); i != e; i++) {
      Value incoming = inst.getIncomingValue(i);
      LatticeStatus ls = getLatticeStatus(incoming);
      if (ls.isUndefined())
        continue;       // the undefined incoming can not influence result.

      BasicBlock fromBB = inst.getIncomingBlock(i);
      if (isEdgeFeasible(fromBB, inst.getParent())) {
        if (ls.isOverdefined()) {
          markOverdefined(inst);
          return null;
        }

        // Record the constant value of this LLMV IR value.
        if (uniqueConstant == null) {
          uniqueConstant = ls.getConstVal();
        } else {
          Constant cond = ls.getConstVal();
          if (!cond.equals(uniqueConstant)) {
            markOverdefined(inst);
            return null;
          }
        }
      }
    }

    // If we exited the loop, this means that the PHI node only has constant
    // arguments that agree with each other(and OperandVal is the constant) or
    // OperandVal is null because there are no defined incoming arguments.
    // If this is the case, the PHI remains undefined.
    if (uniqueConstant != null) {
      markConstant(phiLS, inst, uniqueConstant);
    }
    return null;
  }

  @Override
  public Void visitSelect(User u) {
    Util.assertion(false, "SCCP not support select instruction currently!");
    return null;
  }
}
