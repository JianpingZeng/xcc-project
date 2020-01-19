/*
 * Extremely C language Compiler
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

package backend.transform.scalars;

import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.support.IntStatistic;
import backend.transform.utils.ConstantFolder;
import backend.value.*;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.SwitchInst;
import backend.value.Value.UndefValue;
import tools.Util;

import java.util.LinkedList;

import static backend.transform.utils.ConstantFolder.constantFoldTerminator;

/**
 * This pass propagates information about conditional expressions through the
 * program, allowing it to eliminate conditional branches in some cases.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class ConditionalPropagate implements FunctionPass {
  public static IntStatistic NumBrThread =
      new IntStatistic("NumBrThread", "Number of CFG edges threaded through program");
  public static IntStatistic NumSwThread =
      new IntStatistic("NumSwThread", "Number of CFG edges threaded through program");
  private boolean madeChange;
  private LinkedList<BasicBlock> deadBlocks;

  private AnalysisResolver resolver;

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  private ConditionalPropagate() {
    super();
    madeChange = false;
    deadBlocks = new LinkedList<>();
  }

  /**
   * A static factory method to create an instance of {@linkplain ConditionalPropagate}.
   *
   * @return
   */
  public static ConditionalPropagate createCondPropagatePass() {
    return new ConditionalPropagate();
  }

  @Override
  public String getPassName() {
    return "Sparse Conditional constant propagate";
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.addRequired(BreakCriticalEdge.class);
  }

  @Override
  public boolean runOnFunction(Function f) {
    boolean everMadeChange = false;
    deadBlocks.clear();

    // Keep iterating while the CFG has been changed.
    do {
      madeChange = false;
      for (BasicBlock bb : f.getBasicBlockList()) {
        simplifyBasicBlock(bb);
      }
      everMadeChange |= madeChange;

    } while (madeChange);

    if (everMadeChange) {
      while (!deadBlocks.isEmpty()) {
        ConstantFolder.deleteDeadBlock(deadBlocks.removeLast());
      }
    }
    return everMadeChange;
  }

  private void simplifyBasicBlock(BasicBlock bb) {
    if (bb.getTerminator() == null)
      return;

    if (bb.getTerminator() instanceof BranchInst) {
      // If this is a conditional branch based on a phi node that is defined in
      // this block, see if we can simplify predecessors of this block.
      BranchInst bi = (BranchInst) bb.getTerminator();
      if (bi.isConditional() && bi.getCondition() instanceof PhiNode
          && ((PhiNode) bi.getCondition()).getParent().equals(bb)) {
        simplifyPredecessor(bi);
      }
    } else if (bb.getTerminator() instanceof SwitchInst) {
      SwitchInst si = (SwitchInst) bb.getTerminator();
      if (si.getCondition() instanceof PhiNode && ((PhiNode) si.getCondition()).
          getParent().equals(bb)) {
        simplifyPredecessor(si);
      }
    }

    // If possible, simplify the terminator of this block.
    if (constantFoldTerminator(bb))
      madeChange = true;

    // If this block ends with a unconditonal branch and the only successor
    // has only this block as predecessor, merge the two blocks together.
    if (bb.getTerminator() instanceof BranchInst) {
      BranchInst bi = (BranchInst) bb.getTerminator();
      if (bi.isUnconditional() && bi.getSuccessor(0).getSinglePredecessor() != null
          && !bi.getSuccessor(0).equals(bb))  // avoiding self-ref circle.
      {
        BasicBlock succ = bi.getSuccessor(0);
        foldSingleEntryPHINodes(succ);

        // Erase this branch inst from its parent basic block.
        bi.eraseFromParent();
        // append all instructions in succ into this basic block.
        bb.getInstList().addAll(succ.getInstList());

        succ.replaceAllUsesWith(bb);
        new UnreachableInst(bb.getContext(), succ);
        deadBlocks.add(succ);
        madeChange = true;
      }
    }
  }

  /**
   * We know that parent has one predecessor. If there are any single-entry
   * phi nodes in it, fold them away. This handles the case when all
   * entries to the phi nodes in a block are guaranteed equal, such as
   * when the block has exactly one predecessor.
   *
   * @param bb
   */
  private void foldSingleEntryPHINodes(BasicBlock bb) {
    if (!(bb.getFirstInst() instanceof PhiNode))
      return;

    while (bb.getFirstInst() instanceof PhiNode) {
      PhiNode pn = (PhiNode) bb.getFirstInst();
      Util.assertion(pn.getNumberIncomingValues() == 1, "Just one incoming value is valid");

      if (!pn.getIncomingValue(0).equals(pn))
        pn.replaceAllUsesWith(pn.getIncomingValue(0));
      else
        pn.replaceAllUsesWith(UndefValue.get(pn.getType()));
      pn.eraseFromParent();
    }
  }

  /**
   * We know that {@code si} switch based on a phi node defined in this
   * block. If the phi node contains constant operands, then the blocks
   * corresponding to those operands can be modified to jump directly to
   * the destination instead of going through this block.
   *
   * @param si
   */
  private void simplifyPredecessor(SwitchInst si) {
    Util.assertion(si.getCondition() instanceof PhiNode);
    PhiNode pn = (PhiNode) si.getCondition();
    if (!pn.hasOneUses())
      return;

    BasicBlock bb = si.getParent();
    if (!bb.getFirstInst().equals(pn))
      return;

    boolean removedPreds = false;
    // Ok, we have this really simple case, walk the PHI operands, looking for
    // constants.  Walk from the end to remove operands from the end when
    // possible, and to avoid invalidating "i".
    for (int i = pn.getNumberIncomingValues() - 1; i >= 0; i--) {
      Value inVal = pn.getIncomingValue(i);
      if (inVal instanceof ConstantInt) {
        Constant ci = (Constant) inVal;
        int destCase = si.findCaseValue(ci);
        revectorBlockTo(pn.getIncomingBlock(i), si.getSuccessor(destCase));
        NumSwThread.inc();
        removedPreds = true;

        if (!si.getCondition().equals(pn))
          return;
      }
    }
  }

  /**
   * Redirect an unconditional branch at the end of {@code fromBB} to the
   * {@code toBB} block, which is one of the successors of it current
   * successor.
   *
   * @param fromBB
   * @param toBB
   */
  private void revectorBlockTo(BasicBlock fromBB, BasicBlock toBB) {
    Util.assertion(fromBB.getTerminator() != null && fromBB.getTerminator() instanceof BranchInst);


    BranchInst fromBr = (BranchInst) fromBB.getTerminator();
    Util.assertion(fromBr.isUnconditional());

    BasicBlock oldSucc = fromBr.getSuccessor(0);

    // OldSucc had multiple successors. If ToBB has multiple predecessors, then
    // the edge between them would be critical, which we already took care of.
    // If ToBB has single operand PHI node then take care of it here.
    foldSingleEntryPHINodes(toBB);

    oldSucc.removePredecessor(fromBB);
    fromBr.setSuccessor(0, toBB);
    madeChange = true;
  }

  /**
   * We know that {@code bi} is a conditional branch based on a phi node
   * defined in the same block. If the phi node contains constant operands,
   * then the blocks corresponding to those operands can be modified to
   * jump directly to the destination instead of going through this block.
   *
   * @param bi
   */
  private void simplifyPredecessor(BranchInst bi) {
    Util.assertion(bi.getCondition() instanceof PhiNode);
    PhiNode pn = (PhiNode) bi.getCondition();

    if (pn.getNumberIncomingValues() == 1) {
      // Eliminate the single-entry phi node.
      foldSingleEntryPHINodes(pn.getParent());
    }

    // This phi node must only have one use that the branch instruction.
    if (!pn.hasOneUses())
      return;

    BasicBlock bb = bi.getParent();
    if (!bb.getFirstInst().equals(pn))
      return;

    // Ok, we have this really simple case, walk the PHI operands, looking for
    // constants.  Walk from the end to remove operands from the end when
    // possible, and to avoid invalidating "i".
    for (int i = pn.getNumberIncomingValues() - 1; i >= 0; i--) {
      Value inVal = pn.getIncomingValue(i);
      if (!revectorBlockTo(pn.getIncomingBlock(i), inVal, bi))
        continue;

      NumBrThread.inc();

      if (!bi.getCondition().equals(pn))
        return;
    }
  }

  private boolean revectorBlockTo(BasicBlock fromBB,
                                  Value cond,
                                  BranchInst bi) {
    if (!(fromBB.getTerminator() instanceof BranchInst))
      return false;

    BranchInst fromBr = (BranchInst) fromBB.getTerminator();
    if (!fromBr.isUnconditional())
      return false;

    // Get the old block we are threading through.
    BasicBlock oldSucc = fromBr.getSuccessor(0);

    if (cond instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) cond;
      BasicBlock toBB = bi.getSuccessor(ci.isZero() ? 1 : 0);

      foldSingleEntryPHINodes(toBB);
      oldSucc.removePredecessor(fromBB);
      fromBr.setSuccessor(0, toBB);
    } else {
      BasicBlock succ0 = bi.getSuccessor(0);

      // Do not perform transform if the new destination has phi node,
      // the transform will add new preds tot he phi's.
      if (succ0.getFirstInst() instanceof PhiNode)
        return false;

      BasicBlock succ1 = bi.getSuccessor(1);
      if (succ1.getFirstInst() instanceof PhiNode)
        return false;

      // Insert the new conditional branch.
      new BranchInst(succ0, succ1, cond, fromBr);

      foldSingleEntryPHINodes(succ0);
      foldSingleEntryPHINodes(succ1);

      oldSucc.removePredecessor(fromBB);

      // Delete the old branch.
      fromBr.eraseFromParent();
    }
    madeChange = true;
    return true;
  }
}
