package backend.analysis;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng
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

import backend.pass.AnalysisResolver;
import backend.pass.LPPassManager;
import backend.pass.LoopPass;
import backend.pass.Pass;
import backend.value.*;
import backend.value.Instruction.PhiNode;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class IVUsers implements LoopPass {
  private Loop loop;
  private LoopInfo li;
  private DomTree dt;
  private ScalarEvolution se;
  private HashSet<Instruction> processed = new HashSet<>();

  /**
   * A list of all tracked IV users of induction variable expressions
   * we are interested in.
   */
  public ArrayList<IVUsersOfOneStride> ivUsers;
  /**
   * A diagMapping from strides to the uses in {@linkplain #ivUsers}.
   */
  public HashMap<SCEV, IVUsersOfOneStride> ivUsesByStride;

  private AnalysisResolver resolver;

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public boolean runOnLoop(Loop loop, LPPassManager ppm) {
    this.loop = loop;
    li = (LoopInfo) getAnalysisToUpDate(LoopInfo.class);
    dt = (DomTree) getAnalysisToUpDate(DomTree.class);
    se = (ScalarEvolution) getAnalysisToUpDate(ScalarEvolution.class);
    BasicBlock header = loop.getHeaderBlock();
    Util.assertion(header != null);

    // Find all of uses of induction variables in this loop, and
    // categorize them by stride. Start by finding all Phi nodes
    // in the header block of this loop. If they are induction
    // variables, inspect there uses.

    // Note that, the induction variable in SSA form is represented by
    // Phi node.
    for (Instruction inst : header) {
      if (!(inst instanceof PhiNode))
        break;
      addUsersIfInteresting(inst);
    }
    return false;
  }

  /**
   * Inspect the specified instruction whether is a SCEV variable, recursively
   * add its user to the {@linkplain #ivUsesByStride} map and return true.
   * Otherwise, return false.
   *
   * @param inst
   */
  public boolean addUsersIfInteresting(Instruction inst) {
    // Avoiding that instruction of typed FP.
    if (!se.isSCEVable(inst.getType()))
      return false;

    // avoiding handle the too bigger integral type.
    if (se.getTypeSizeBits(inst.getType()) > 64)
      return false;

    // Has been processed yet.
    if (!processed.add(inst))
      return true;

    // get the scev for this instruction.
    SCEV ise = se.getSCEV(inst);
    if (ise instanceof SCEVCouldNotCompute) return false;

    Loop useLoop = li.getLoopFor(inst.getParent());
    SCEV start = se.getIntegerSCEV(0, ise.getType());
    SCEV stride = start;
    OutRef<SCEV> startOut = new OutRef<>(start);
    OutRef<SCEV> strideOut = new OutRef<>(stride);
    if (!getSCEVStartAndStride(ise, loop, useLoop, startOut, strideOut, se, dt))
      return false;

    // Updates the returned result.
    start = startOut.get();
    stride = strideOut.get();

    HashSet<Instruction> uniqueUsers = new HashSet<>();
    for (Use u : inst.usesList) {
      Instruction user = (Instruction) u.getUser();
      // Avoiding duplicate inserting.
      if (!uniqueUsers.add(user))
        continue;

      //Don't infinitely recurse on PHI nodes.
      if ((user instanceof PhiNode) && processed.contains(user))
        continue;

      // Descend recursively, but not into PHI nodes outside the current
      // loop. It's important to see the entire expression outside the loop
      // to get choices that depend on addressing mode use right, although
      // we won't consider references ouside the loop in all cases.
      // If User is already in Processed, we don't want to recurse into
      // it again, but do want to record a second reference in the same
      // instruction.
      boolean addUserToIVUsers = false;
      if (li.getLoopFor(user.getParent()) != loop) {
        if (user instanceof PhiNode || processed.contains(user)
            || !addUsersIfInteresting(user)) {
          Util.Debug("Found User in other loops: ", user, "\n",
              "  Of SCEV: ", ise, "\n");
          addUserToIVUsers = true;
        }
      } else if (processed.contains(user) || !addUserToIVUsers) {
        Util.Debug("Found User : ", user, "\n",
            "  Of SCEV: ", ise, "\n");
        addUserToIVUsers = true;
      }

      if (addUserToIVUsers) {
        IVUsersOfOneStride strideUses = null;
        if (!ivUsesByStride.containsKey(stride)) {
          strideUses = new IVUsersOfOneStride(stride);
          ivUsers.add(strideUses);
          ivUsesByStride.put(stride, strideUses);
        } else {
          strideUses = ivUsesByStride.get(stride);
        }

        // Okay, we found a user that we cannot reduce.  Analyze the instruction
        // and decide what to do with it.  If we are a use inside of the loop, use
        // the value before incrementation, otherwise use it after incrementation.
        if (ivUseShouldUsePostIncValue(user, inst, loop, li, dt, this)) {
          SCEV newStart = se.getMinusSCEV(start, stride);
          strideUses.addUser(newStart, user, inst);
          strideUses.users.getLast().setUseOfPostIncrementedValue(true);
          Util.Debug("  Using postinc SCEV, start=", newStart, "\n");
        } else {
          strideUses.addUser(start, user, inst);
        }
      }
    }
    return false;
  }

  private static boolean ivUseShouldUsePostIncValue(Instruction user,
                                                    Instruction iv, Loop loop, LoopInfo li, DomTree dt,
                                                    Pass p) {
    // If the user is in the loop, use the pre-inc value.
    if (loop.contains(user.getParent()))
      return false;

    BasicBlock latchBasic = loop.getLoopLatch();

    // Ok, the user is outside of the loop. If it is dominated by
    // the latch block, use the post-inc value.
    if (dt.dominates(latchBasic, user.getParent()))
      return true;

    // There is one case we have to be careful of: PHI nodes.  These little guys
    // can live in blocks that are not dominated by the latch block, but (since
    // their uses occur in the predecessor block, not the block the PHI lives in)
    // should still use the post-inc value.  Check for this case now.
    if (!(user instanceof PhiNode))
      return false;  // not a phi, not dominated by latch block.
    PhiNode pn = (PhiNode) user;

    // Look at all of the uses of IV by the PHI node.  If any use corresponds to
    // a block that is not dominated by the latch block, give up and use the
    // pre-incremented value.
    int numUses = 0;
    for (int i = 0, e = pn.getNumberIncomingValues(); i < e; i++) {
      if (pn.getIncomingValue(i).equals(iv)) {
        numUses++;
        if (!dt.dominates(latchBasic, pn.getIncomingBlock(i)))
          return false;
      }
    }

    // Okay, all uses of IV by PN are in predecessor blocks that really are
    // dominated by the latch block.  Use the post-incremented value.
    return true;
  }

  /**
   * Computes the start and stride for this instruction, returning false
   * if the expression is not a start/stride pair, or true if it is.
   * The stride must be a loop invariant expression, but the start may be
   * a mixture of loop invariant and loop variant. However, the start cannot
   * contain an AddRec from a different loop, unless that loop is an outer
   * loop of the current loop.
   *
   * @param sh
   * @param loop
   * @param useLoop
   * @param start
   * @param stride
   * @param se
   * @param dt
   * @return
   */
  private static boolean getSCEVStartAndStride(SCEV sh, Loop loop,
                                               Loop useLoop, OutRef<SCEV> start,
                                               OutRef<SCEV> stride,
                                               ScalarEvolution se, DomTree dt) {
    SCEV theAddRec = start.get();

    // If the outer level is an AddExpr, the operands are all starts values
    // except for a nested AddRecExpr.
    if (sh instanceof SCEVAddExpr) {
      SCEVAddExpr ae = (SCEVAddExpr) sh;
      for (int i = 0, e = ae.getNumOperands(); i < e; i++) {
        SCEV op = ae.getOperand(i);
        if (op instanceof SCEVAddRecExpr) {
          SCEVAddRecExpr addRec = (SCEVAddRecExpr) op;
          if (addRec.getLoop().equals(loop))
            theAddRec = SCEVAddExpr.get(addRec, theAddRec);
          else
            // Nested IV of some sort?
            return false;
        } else {
          start.set(SCEVAddExpr.get(start.get(), op));
        }
      }
    } else if (sh instanceof SCEVAddRecExpr)
      theAddRec = sh;
    else
      return false;   // Non analyzable.


    if (!(theAddRec instanceof SCEVAddRecExpr)
        || ((SCEVAddRecExpr) theAddRec).getLoop() != loop)
      return false;

    SCEVAddRecExpr addRec = (SCEVAddRecExpr) theAddRec;

    // Use the getSCEVAtScope to attempt to simplify other loops.
    SCEV addRecStart = addRec.getStart();
    addRecStart = se.getSCEVAtScope(addRecStart, useLoop);
    SCEV addRecStride = addRec.getStepRecurrence();

    if (containsAddRecFromDifferentLoop(addRecStart, loop))
      return false;

    start.set(SCEVAddExpr.get(start.get(), addRecStart));

    // If stride is an instruction, make sure it dominates the loop preheader.
    // Otherwise we could end up with a use before def situation.
    if (!(addRecStride instanceof SCEVConstant)) {
      BasicBlock preheader = loop.getLoopPreheader();
      if (!addRecStride.dominates(preheader, dt))
        return false;
    }

    stride.set(addRecStride);
    return true;
  }

  /**
   * Determine whether expression {@code s} involves a subexpression that is
   * an AddRec from a loop other than L.  An outer loop of L is OK, but not
   * an inner loop nor a disjoint loop.
   *
   * @param s
   * @param loop
   * @return
   */
  private static boolean containsAddRecFromDifferentLoop(SCEV s, Loop loop) {
    if (s instanceof SCEVConstant)
      return false;

    if (s instanceof SCEVCommutativeExpr) {
      SCEVCommutativeExpr commExpr = (SCEVCommutativeExpr) s;
      for (int i = 0, e = commExpr.getNumOperands(); i < e; i++) {
        if (containsAddRecFromDifferentLoop(commExpr.getOperand(i), loop))
          return true;
      }
      return false;
    }

    if (s instanceof SCEVAddRecExpr) {
      SCEVAddRecExpr ae = (SCEVAddRecExpr) s;
      Loop newLoop = ae.getLoop();
      if (newLoop != null) {
        if (newLoop == loop)
          return false;

        // If the newLoop is an outer loop of loop, this is OK.
        if (!newLoop.contains(loop))
          return false;
      }
      return true;
    }

    if (s instanceof SCEVSDivExpr) {
      SCEVSDivExpr divExpr = (SCEVSDivExpr) s;
      return containsAddRecFromDifferentLoop(divExpr.getLHS(), loop)
          || containsAddRecFromDifferentLoop(divExpr.getRHS(), loop);
    }

    return false;
  }

  @Override
  public String getPassName() {
    return "Induction variable users function pass";
  }

  /**
   * Return a SCEV expression which computes the value of the
   * {@linkplain IVStrideUses#operandValToReplace} of the given IVStrideUse.
   *
   * @param u
   * @return
   */
  public SCEV getReplacementExpr(IVStrideUses u) {
    SCEV retVal = se.getIntegerSCEV(0, u.getParent().stride.getType());
    retVal = SCEVAddRecExpr.get(retVal, u.getParent().stride, loop);
    retVal = SCEVAddExpr.get(retVal, u.getOffset());
    if (u.isUseOfPostIncrementedValue())
      retVal = SCEVAddExpr.get(retVal, u.getParent().stride);

    if (!loop.contains(u.getUser().getParent())) {
      SCEV exitVal = se.getSCEVAtScope(retVal, loop.getParentLoop());
      if (exitVal.isLoopInvariant(loop))
        retVal = exitVal;
    }

    return retVal;
  }

  @Override
  public void print(PrintStream os, Module m) {
    os.print("IV Users for loop: ");
    os.print(loop.getHeaderBlock().getName() + " ");
    if (se.hasLoopInvariantIterationCount(loop)) {
      os.printf(" with backedge-taken count %d",
          se.getIterationCount(loop));
    }
    os.println();

    ivUsesByStride.keySet().forEach(stride ->
    {
      Util.assertion(ivUsesByStride.containsKey(stride), "Stride doesn't exits!");
      os.print("  Stride: " + stride.getType().toString() + "  " +
          stride.toString() + "\n");

      IVUsersOfOneStride uses = ivUsesByStride.get(stride);
      for (IVStrideUses u : uses.users) {
        os.print("  ");
        os.print(u.getOperandValToReplace());
        os.print("=");
        os.print(getReplacementExpr(u));
        if (u.isUseOfPostIncrementedValue())
          os.print("post-inc");
        os.print(" in ");
        u.getUser().print(os);
        os.println();
      }
    });
  }
}
