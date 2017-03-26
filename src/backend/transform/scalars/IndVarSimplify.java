package backend.transform.scalars;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:*www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.analysis.*;
import backend.hir.BasicBlock;
import backend.pass.*;
import backend.support.SCEVExpander;
import backend.type.Type;
import backend.value.Instruction;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.CmpInst.Predicate;
import backend.value.Instruction.ICmpInst;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.TerminatorInst;
import backend.value.Use;
import backend.value.Value;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

/**
 * <p>
 * This transformation analyzes and transforms the induction variables (and
 * computations derived from them) into simpler forms suitable for subsequent
 * analysis and transformation.
 * </p>
 * <p>
 * This transformation make the following changes to each loop with an
 * identifiable induction variable:
 *  <ol>
 *      <li>
 *      All loops are transformed to have a SINGLE canonical induction variable
 *      which starts at zero and steps by one.
 *      </li>
 *      <li>The canonical induction variable is guaranteed to be the first PHI node
 *      in the loop header block.
 *      </li>
 *      <li>
 *       Any pointer arithmetic recurrences are raised to use array subscripts.
 *      </li>
 *   </ol>
 * </p>
 * If the trip count of a loop is computable, this pass also makes the following
 * changes:
 * <ol>
 *     <li>
 *         The exit condition for the loop is canonicalized to compare the
 *      induction value against the exit value.  This turns loops like:
 *        <pre>for (i = 7; i*i < 1000; ++i)</pre>
 *        into
 *        <pre>for (i = 0; i != 25; ++i)</pre>
 *     </li>
 *     <li>
 *      Any use outside of the loop of an expression derived from the indvar
 *      is changed to compute the derived value outside of the loop, eliminating
 *      the dependence on the exit value of the induction variable.  If the only
 *      purpose of the loop is to compute the exit value of some derived
 *      expression, this transformation will make the loop dead.
 *     </li>
 * </ol>
 *
 * <p>
 * This transformation should be followed by strength reduction after all of the
 * desired loop transformations have been performed.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IndVarSimplify implements LoopPass
{
    private LoopInfo li;
    private ScalarEvolution se;
    private boolean changed = false;
    private IVUsers iu;
    private DomTreeInfo dt;

    public static final RegisterPass X =
            new RegisterPass("Canonicalize Induction Variables", IndVarSimplify.class);

    public static Pass createIndVarSimplifyPass()
    {
        return new IndVarSimplify();
    }

	/**
     * Sealed with private accessibility.
     */
    private IndVarSimplify()
    {
        super();
    }

    @Override
    public boolean runOnLoop(Loop loop, LPPassManager ppm)
    {
        // If the LoopSimplify form is not available, just return early.
        // A LoopSimplify form must having a preheader, a latch block and
        // dedicated exit blocks, which is required for moving induction
        // variable.
        if (!loop.isLoopSimplifyForm())
            return false;

        li = getAnalysisToUpDate(LoopInfo.class);
        se = getAnalysisToUpDate(ScalarEvolution.class);
        iu = getAnalysisToUpDate(IVUsers.class);
        dt = getAnalysisToUpDate(DomTreeInfo.class);
        changed = false;

        // Firstly, transforms all sub loops nested in current loop processed.
        loop.getSubLoops().forEach(sub->runOnLoop(sub, ppm));

        // If there are any floating-point recurrences, attempt to
        // transform them to use integer recurrences.
        rewriteNonIntegerIVs(loop);

        BasicBlock exitingBlock = loop.getExitingBlock(); // may be null.
        SCEV iterationCount = se.getIterationCount(loop);

        // Create a rewriter object that we will use to transform the code with.
        SCEVExpander rewriter = new SCEVExpander(se);

        // Checks to see if this loop has a computable loop-invariant exit expression.
        // If so, this means that we can compute the final value of any expression
        // that recurrent in the loop, and substitute the exit values from the loop
        // into any instruction outside of the loop that use the final value of the
        // current value.
        if (!(iterationCount instanceof SCEVCouldNotCompute))
            rewriteLoopExitValues(loop, rewriter);

        // Compute the type of the largest recurrence expression, and decide whether
        // a canonical induction variable should be inserted.
        Type largestType = null;
        boolean needCannIV = false;
        if (!(iterationCount instanceof SCEVCouldNotCompute))
        {
            largestType = iterationCount.getType();
            largestType = se.getEffectiveSCEVType(largestType);

            // If we have a known trip count and a single exit block, we'll be
            // rewriting the loop exit test condition below, which requires a
            // canonical induction variable.
            if (exitingBlock != null)
                needCannIV = true;
        }

        for (SCEV stride : iu.ivUsesByStride.keySet())
        {
            Type ty = se.getEffectiveSCEVType(stride.getType());
            if (largestType == null || se.getTypeSizeBits(ty)
                    > se.getTypeSizeBits(largestType))
                largestType = ty;

            if (!iu.ivUsesByStride.get(stride).users.isEmpty())
                needCannIV = true;
        }

        // Now that we know the largest of of the induction variable expressions
        // in this loop, insert a canonical induction variable of the largest getNumOfSubLoop.
        Value indVal = null;
        if (needCannIV)
        {
            PhiNode oldCanIV = loop.getCanonicalInductionVariable();
            if (oldCanIV != null)
            {
                if (se.getTypeSizeBits(oldCanIV.getType())
                        > se.getTypeSizeBits(largestType))
                    oldCanIV.eraseFromBasicBlock();
                else
                    oldCanIV = null;
            }

            indVal = rewriter.getOrCreateCanonicalInductionVariable(loop, largestType);
            changed = true;

            if (oldCanIV != null)
                oldCanIV.insertAfter((Instruction) indVal);
        }

        // If we have a trip count expression, rewrite the loop's exit condition
        // using it.  We can currently only handle loops with a single exit.
        ICmpInst newICmp = null;
        if (!(iterationCount instanceof SCEVCouldNotCompute)
                && exitingBlock != null)
        {
            assert needCannIV :"LinearFunctionTestReplace requires a canonical induction variable";
            Instruction lastInst = exitingBlock.getLastInst();
            if (lastInst instanceof BranchInst)
            {
                BranchInst bi = (BranchInst)lastInst;
                newICmp = linearFunctionTestReplace(loop, iterationCount,
                        indVal, exitingBlock, bi, rewriter);
            }
        }
        // Rewrite IV-derived expressions. Clears the rewriter cache.
        rewriteIVExpressions(loop, largestType, rewriter);

        sinkUnusedInvariants(loop);

        // For completeness, inform IVUsers of the IV use in the newly-created
        // loop exit test instruction.
        if (newICmp != null)
            iu.addUsersIfInteresting((Instruction)newICmp.operand(0));

        // Clean up dead instructions.
        deleteDeadPhis(loop.getHeaderBlock());

        assert loop.isLCSSAForm() :"Indvars did not leave the loop in LCSSA form!";
        return changed;
    }

	/**
     * This method rewrites the loop exit condition expression to be
     * canonical != comparison on incremented loop induction variable.
     * @param loop
     * @param iterationCount
     * @param indVal
     * @param exitBB
     * @param bi
     * @param rewriter
     * @return
     */
    private ICmpInst linearFunctionTestReplace(Loop loop,
            SCEV iterationCount,
            Value indVal,
            BasicBlock exitBB, BranchInst bi,
            SCEVExpander rewriter)
    {
        // If the exiting block is not the same as the backedge block, we must compare
        // against the preincremented value, otherwise we prefer to compare against
        // the post-incremented value.
        Value cmpIndVar = null;
        SCEV rhs = iterationCount;
        if (exitBB == loop.getLoopLatch())
        {
            // Add one to the "backedge-taken" count to get the trip count.
            // If this addition may overflow, we have to be more pessimistic and
            // cast the induction variable before doing the add.
            SCEV zero = se.getIntegerSCEV(0, iterationCount.getType());
            SCEV n = SCEVAddExpr.get(iterationCount, se.getIntegerSCEV(1,
                    iterationCount.getType()));

            if ((n instanceof SCEVConstant && !n.isZero())
                    || se.isLoopGuardedByCond(loop,
                    Predicate.ICMP_NE, n, zero))
            {
                rhs = se.getTruncateOrZeroExtend(n, indVal.getType());
            }
            else
            {
                rhs = se.getTruncateOrZeroExtend(iterationCount, indVal.getType());
                rhs = SCEVAddExpr.get(rhs, se.getIntegerSCEV(1, indVal.getType()));
            }

            // The iterationCount expression contains the number of times that the
            // backedge branches to the loop header.  This is one less than the
            // number of times the loop executes, so use the incremented indvar.
            cmpIndVar = loop.getCanonicalInductionVariable();
        }
        else
        {
            // We have to use the preincremented value.
            rhs = se.getTruncateOrZeroExtend(iterationCount, indVal.getType());
            cmpIndVar = indVal;
        }

        assert rhs.isLoopInvariant(loop) :
                "Computed iteration count is not loop invariant!";
        Value exitCount = rewriter.expandCodeFor(rhs, indVal.getType(), bi);

        Predicate opcode;
        if (loop.contains(bi.suxAt(0)))
            opcode = Predicate.ICMP_NE;
        else
            opcode = Predicate.ICMP_EQ;
        ICmpInst cond = new ICmpInst(opcode, cmpIndVar, exitCount, "exitcond", bi);

        Instruction originCond = (Instruction)bi.getCondition();
        bi.setCondition(cond);
        recursivelyDeleteTriviallyDeadInstuctions(originCond);
        changed = true;
        return cond;
    }

    private void rewriteIVExpressions(Loop loop, Type largestType, SCEVExpander rewriter)
    {
        Stack<Value> deadInst = new Stack<>();

        // Rewrite all induction variable expressions in terms of the canonical
        // induction variable.
        //
        // If there were induction variables of other sizes or offsets, manually
        // add the offsets to the primary induction variable and cast, avoiding
        // the need for the code evaluation methods to insert induction variables
        // of different sizes
        for (SCEV stride : iu.ivUsesByStride.keySet())
        {
            LinkedList<IVStrideUses> list = iu.ivUsesByStride.get(stride).users;
            for (IVStrideUses use : list)
            {
                Value op = use.getOperandValToReplace();
                Type useTy = op.getType();
                Instruction user = use.getUser();

                SCEV ar = iu.getReplacementExpr(use);

                if (!ar.isLoopInvariant(loop) && !stride.isLoopInvariant(loop))
                    continue;

                Instruction insertPt = user;
                if (insertPt instanceof PhiNode)
                {
                    PhiNode phi = (PhiNode)insertPt;
                    for (int i = 0, e = phi.getNumberIncomingValues(); i < e; i++)
                    {
                        if (phi.getIncomingValue(i) == op)
                        {
                            if (insertPt == user)
                                insertPt = phi.getIncomingBlock(i).getTerminator();
                            else
                                insertPt = dt.findNearestCommonDominator(insertPt.getParent(),
                                        phi.getIncomingBlock(i)).getTerminator();
                        }
                    }
                }

                // Now expand it into actual Instructions and patch it into place.
                Value newVal = rewriter.expandCodeFor(ar, useTy, insertPt);

                if (op.hasName())
                    newVal.setName(op.getName());
                user.replaceUsesOfWith(op, newVal);
                use.setOperandValToReplace(newVal);
                changed = true;

                // The old value may be dead now.
                deadInst.add(op);
            }
        }
        rewriter.clear();
        // Now that we're done iterating through lists, clean up any instructions
        // which are now dead.
        while (!deadInst.isEmpty())
        {
            Value val = deadInst.pop();
            if (val instanceof Instruction)
                recursivelyDeleteTriviallyDeadInstuctions(val);
        }
    }

	/**
	 * If there's a single exit block, sink any loop-invariant values that
     * were defined in the preheader but not used inside the loop into the
     * exit block to reduce register pressure in the loop.
     * @param loop
     */
    private void sinkUnusedInvariants(Loop loop)
    {
        BasicBlock exitBlock = loop.getExitingBlock();
        if (exitBlock == null) return;

        Instruction insertPos = exitBlock.getInstAt(exitBlock.getFirstNonPhi());
        BasicBlock preheader = loop.getLoopPreheader();
        for (int i = preheader.size() - 1; i >= 0; i--)
        {
            Instruction curInst = preheader.getInstAt(i);
            if (curInst instanceof PhiNode)
                break;

            if (curInst.mayHasSideEffects() || curInst.mayWriteMemory())
                continue;

            boolean useInLoop = false;

            for (Use u : curInst.getUseList())
            {
                BasicBlock userBB = ((Instruction)u.getUser()).getParent();
                if (u.getUser() instanceof PhiNode)
                {
                    PhiNode p = (PhiNode)u.getUser();
                    userBB = p.getIncomingBlock(u);
                }
                if (userBB == preheader || loop.contains(userBB))
                {
                    useInLoop = true;
                    break;
                }
            }

            // If these is, the def must remain in the preheader.
            if (useInLoop)
                continue;

            // Otherwise, sink it to the exit block.
            curInst.moveBefore(insertPos);
            insertPos = curInst;
        }
    }

	/**
	 * Examine each PHI in the given block and delete it if it
     * is dead. Also recursively delete any operands that become dead as
     * a result. This includes tracing the def-use list from the PHI to see if
     * it is ultimately unused or if it reaches an unused cycle.
     * @param entryBB
     */
    private void deleteDeadPhis(BasicBlock entryBB)
    {
        ArrayList<PhiNode> phis = new ArrayList<>();
        for (Instruction inst : entryBB)
        {
            if (!(inst instanceof PhiNode))
                break;
            phis.add((PhiNode)inst);
        }

        phis.forEach(this:: recursivelyDeleteDeadPhiNode);
    }

	/**
     * If the specified value is an effectively
     * dead PHI node, due to being a def-use chain of single-use nodes that
     * either forms a cycle or is terminated by a trivially dead instruction,
     * delete it.  If that makes any of its operands trivially dead, delete them
     * too, recursively.
     * @param pn
     */
    private void recursivelyDeleteDeadPhiNode(PhiNode pn)
    {
        if (!pn.hasOneUses())
            return;

        Stack<PhiNode> phis = new Stack<>();
        phis.push(pn);
        for (Instruction inst = (Instruction) pn.getUseList().getFirst().getValue();
                inst.hasOneUses() && !inst.mayHasSideEffects();
                inst = (Instruction)inst.getUseList().getFirst().getValue())
        {
            if (inst instanceof PhiNode)
            {
                PhiNode pn2 = (PhiNode)inst;
                // If we find a PHI more than once, we're on a cycle.
                if (!phis.add(pn2))
                {
                    // Break the cycle and delete the PHI and its operands.
                    pn2.replaceAllUsesWith(Value.UndefValue.get(pn2.getType()));
                    recursivelyDeleteTriviallyDeadInstuctions(pn2);
                    break;
                }
            }
        }
    }

	/**
     * Checks to see if this loop has a computable loop-invariant exit expression.
     * If so, this means that we can compute the final value of any expression
     * that recurrent in the loop, and substitute the exit values from the loop
     * into any instruction outside of the loop that use the final value of the
     * current value.
     * @param loop
     * @param rewriter
     */
    private void rewriteLoopExitValues(Loop loop,
            SCEVExpander rewriter)
    {
        assert loop.isLCSSAForm();

        ArrayList<BasicBlock> exitBlocks = loop.getUniqueExitBlocks();

        // Find all values that are computed inside the loop, but used outside of it.
        // Because of LCSSA, these values will only occur in LCSSA PHI Nodes.  Scan
        // the exit blocks of the loop to find them.
        for (int i = 0, e = exitBlocks.size(); i < e; i++)
        {
            BasicBlock exitBB = exitBlocks.get(i);

            // If there are no PHI nodes in this exit block, then no values defined
            // inside the loop are used on this path, skip it.
            Instruction firstInst = exitBB.getFirstInst();
            if (!(firstInst instanceof PhiNode))
                continue;
            PhiNode pn = (PhiNode) firstInst;
            int numPreds = pn.getNumberIncomingValues();

            for (Instruction inst : exitBB)
            {
                if (!(inst instanceof PhiNode))
                    break;
                pn = (PhiNode)inst;
                if (pn.isUseEmpty())
                    continue;
                for (int j = 0; j < numPreds; j++)
                {
                    Value inVal = pn.getIncomingValue(j);
                    // SCEV only supports integer type.
                    if (!(inVal instanceof Instruction)
                            || !(inVal.getType().isPointerType()
                    && (inVal.getType().isIntegerType())))
                        continue;

                    // If this pred is for a subloop, not loop, skip it.
                    if (li.getLoopFor(pn.getIncomingBlock(j)) != loop)
                        continue;

                    inst = (Instruction)inVal;
                    //Check to see if this inval is defined in this loop.
                    if (!loop.contains(inst.getParent()))
                        continue;

                    // Okay, this instruction has a user outside of the current loop
                    // and varies predictably *inside* the loop.  Evaluate the value it
                    // contains when the loop exits, if possible.
                    SCEV exitValue = se.getSCEVAtScope(inst, loop.getParentLoop());
                    if (!exitValue.isLoopInvariant(loop))
                        continue;

                    changed = true;

                    Value exitVal = rewriter.expandCodeFor(exitValue, pn.getType(), inst);
                    pn.setIncomingValue(i, exitVal);

                    // If this instruction is dead now, delete it.
                    recursivelyDeleteTriviallyDeadInstuctions(inst);

                    if (numPreds == 1)
                    {
                        // Replace all uses of this pn with the exitVal,
                        // if these is just one predecessor.
                        pn.replaceAllUsesWith(exitVal);
                        recursivelyDeleteTriviallyDeadInstuctions(pn);
                    }
                }

                if (numPreds != 1)
                {
                    // Clone the PHI and delete the original one. This lets IVUsers and
                    // any other maps purge the original user from their records.
                    PhiNode newPN = pn.clone();
                    newPN.setName(pn.getName());
                    newPN.insertBefore(pn);
                    pn.replaceAllUsesWith(newPN);
                    pn.eraseFromBasicBlock();
                }
            }
        }
    }

	/**
	 * If the specified value {@code valToDel} is trivially dead instruction,
     * delete it and delete its trivially dead operands if possible.
     * @param valToDel
     */
    private void recursivelyDeleteTriviallyDeadInstuctions(Value valToDel)
    {
        if (!(valToDel instanceof Instruction) || valToDel.isUseEmpty()
                || !isInstructionTriviallyDead((Instruction)valToDel))
            return;

        // use the workflow algorithm to replace recursively method.
        Stack<Instruction> deadInsts = new Stack<>();
        Instruction inst = (Instruction) valToDel;
        deadInsts.add(inst);

        while (!deadInsts.isEmpty())
        {
            inst = deadInsts.pop();

            for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
            {
                Value opVal = inst.operand(i);
                inst.setOperand(i, (Value) null);

                if (!opVal.isUseEmpty()) continue;

                if (opVal instanceof Instruction)
                {
                    Instruction op = (Instruction)opVal;
                    if (!isInstructionTriviallyDead(op))
                        deadInsts.add(op);
                }
            }
            inst.eraseFromBasicBlock();
        }
    }

	/**
	 * Return true if the result produced by this instruction is not used
     * by other, and the instruction has no side effect.
     * @param inst
     * @return
     */
    private boolean isInstructionTriviallyDead(Instruction inst)
    {
        if (!inst.isUseEmpty()
                || inst instanceof TerminatorInst
                || inst.mayHasSideEffects())
            return false;

        return true;
    }

	/**
     * Rewrites any floating-point recurrence, attempt to transform
     * them to use integer recurrence.
     * @param loop
     */
    private void rewriteNonIntegerIVs(Loop loop)
    {

    }

    @Override
    public String getPassName()
    {
        return "Induction variable simplification pass";
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(DomTreeInfo.class);
        au.addRequired(LoopInfo.class);
        au.addRequired(ScalarEvolution.class);
        au.addRequired(LoopSimplify.class);
        au.addRequired(LCSSA.class);
        au.addRequired(IVUsers.class);
        au.addPreserved(ScalarEvolution.class);
        au.addPreserved(LoopSimplify.class);
        au.addPreserved(LCSSA.class);
        au.addPreserved(IVUsers.class);
    }
}
