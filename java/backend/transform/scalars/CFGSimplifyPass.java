package backend.transform.scalars;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

import backend.ir.SelectInst;
import backend.pass.AnalysisResolver;
import backend.pass.FunctionPass;
import backend.type.PointerType;
import backend.utils.SuccIterator;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Value.UndefValue;
import tools.Util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Stack;

import static backend.transform.utils.ConstantFolder.constantFoldTerminator;
import static backend.transform.utils.ConstantFolder.deleteDeadBlock;
import static backend.transform.utils.ConstantFolder.recursivelyDeleteTriviallyDeadInstructions;

/**
 * This pass defined here for removing the unreachable basic block resides inside
 * FunctionProto.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CFGSimplifyPass implements FunctionPass
{
    private AnalysisResolver resolver;

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        this.resolver = resolver;
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return resolver;
    }

    private boolean removeUnreachableBlocksFromFn(Function f)
    {
        HashSet<BasicBlock> reachables = new HashSet<>();
        boolean changed = markActiveBlocks(f.getEntryBlock(), reachables);

        // If there are unreachable blocks in current cfg.
        if (reachables.size() != f.size())
        {
            Util.assertion(reachables.size() < f.size());

            // Loop over all unreachable blocks, and drop off all internal reference.
            for (int i = 0; i < f.size(); i++)
            {
                BasicBlock bb = f.getBlockAt(i);
                if (!reachables.contains(bb))
                {
                    for (SuccIterator itr = bb.succIterator();itr.hasNext();)
                    {
                        BasicBlock succ = itr.next();
                        succ.removePredecessor(bb);
                    }
                    bb.dropAllReferences();
                }
            }

            for (int i = 1; i < f.size();)
            {
                BasicBlock bb = f.getBlockAt(i);
                if (!reachables.contains(bb))
                    f.getBasicBlockList().remove(i);
                else
                    i++;
            }
            changed = true;
        }
        return changed;
    }
    /**
     * calling this method to simplify cfg. It is possible that multiple passes
     * are needed loop over function.
     *
     * @param f
     * @return
     */
    @Override
    public boolean runOnFunction(Function f)
    {
        boolean everChanged = removeUnreachableBlocksFromFn(f);
        if (!everChanged) return false;

        boolean changed;
        do
        {
            changed = false;
            // Loop over all of the basic blocks (except the first one) and remove them
            // if they are unneeded.
            for (int i = 1; i < f.size(); i++)
            {
                if (simplifyCFG(f.getBlockAt(i)))
                {
                    changed = true;
                }
            }
            changed |= removeUnreachableBlocksFromFn(f);
        }while (changed);
        return true;
    }

    /**
     * Insert a UnreachableInst before the specified position and mark all instructions after unreachable inst
     * as dead.
     * @param inst
     */
    private void changeToUnreachable(Instruction inst)
    {
        Util.assertion(inst != null);
        BasicBlock parent = inst.getParent();
        Util.assertion(parent != null);

        // remove the predecessor of any successor block of current parent block.
        for (Iterator<BasicBlock> succItr = parent.succIterator(); succItr.hasNext(); )
        {
            BasicBlock succ = succItr.next();
            succ.removePredecessor(parent);
        }
        new UnreachableInst(inst);
        for (int i = inst.getIndexToBB(), e = parent.size(); i < e; i++)
        {
            Instruction curInst = parent.getInstAt(i);
            if (!curInst.isUseEmpty())
                curInst.replaceAllUsesWith(UndefValue.get(curInst.getType()));
            curInst.eraseFromParent();
            --e;
            --i;
        }
    }

    private boolean markActiveBlocks(BasicBlock entry,
            HashSet<BasicBlock> reachables)
    {
        boolean changed = false;

        // uses a queue for simulating recursively calling this function for efficiency
        // with standard pre-order method.
        Stack<BasicBlock> worklist = new Stack<>();
        worklist.push(entry);

        while (!worklist.isEmpty())
        {
            BasicBlock currBB = worklist.pop();
            if (!reachables.contains(currBB))
                reachables.add(currBB);

            // Converts CallInst(StoreInst) without returned value into UnreachableInst
            for (int i = 0, e = currBB.size(); i < e; i++)
            {
                Instruction inst = currBB.getInstAt(i);
                if (inst instanceof CallInst)
                {
                    // Transform the CallInst into UnreachableInst if it haven't returned value.
                    CallInst ci = (CallInst)inst;
                    if (ci.doesNotReturn())
                    {
                        ++i;
                        if (!(currBB.getInstAt(i) instanceof TerminatorInst))
                        {
                            changeToUnreachable(currBB.getInstAt(i));
                            changed = true;
                        }
                        break;
                    }
                }
                else if (inst instanceof StoreInst)
                {
                    StoreInst si = (StoreInst)inst;
                    Value ptr = si.getPointerOperand();
                    if (ptr instanceof UndefValue ||
                            (ptr instanceof ConstantPointerNull &&
                                    ((PointerType)ptr.getType()).getAddressSpace() == 0))
                    {
                        changeToUnreachable(si);
                        changed = true;
                        break;
                    }
                }
            }
            changed |= constantFoldTerminator(currBB);

            // add all successors block of currBB into worklist.
            for (SuccIterator sItr = currBB.succIterator(); sItr.hasNext();)
                worklist.push(sItr.next());
        }
        return changed;
    }

    /**
     * Get the condition as returned value, trueBlock when true path was taken,
     * falseBlock when false path was taken for specified phi-node.
     * trueBlock and falseBlock would be passed through {@code resultBBs}.
     * @param bb
     * @param resultBBs
     * @return
     */
    private Value getIfCondition(BasicBlock bb, BasicBlock[] resultBBs)
    {
        Util.assertion(resultBBs != null && resultBBs.length == 2);
        // We only handle the block exactly with two preds.
        if (bb.getNumPredecessors() != 2) return null;

        BasicBlock pred1 = bb.predAt(0), pred2 = bb.predAt(1);

        if (!(pred1.getTerminator() instanceof BranchInst) ||
                !(pred2.getTerminator() instanceof BranchInst))
            return null;
        BranchInst pred1Br = (BranchInst) pred1.getTerminator();
        BranchInst pred2Br = (BranchInst) pred2.getTerminator();
        // We can't handle the situation that both two preds's branch instruction
        // is conditional.
        if (pred1Br.isConditional() && pred2Br.isConditional())
            return null;

        // Transform following situation:
        //      If Block(pred2)
        //      /        \
        //     /          \
        //truePart(pred1) |
        //     \         |
        //      \       /
        //      current BB
        // to
        //      If Block(pred1)
        //      /        \
        //     /          \
        //truePart(pred2) |
        //     \         |
        //      \       /
        //      current BB
        if (pred2Br.isConditional())
        {
            // swap preds and branch inst.
            BasicBlock t = pred1;
            pred1 = pred2;
            pred2 = t;

            BranchInst br = pred1Br;
            pred1Br = pred2Br;
            pred2Br = br;
        }

        // If we got here, checks if current block is the common successor of pred1 and pred2.
        if (pred1Br.isConditional())
        {
            if (pred1.getNumSuccessors() != 2)
                return null;
            if (pred1.suxAt(0).equals(pred2) && pred1.suxAt(1).equals(bb))
            {
                resultBBs[0] = pred2;
                resultBBs[1] = pred1;
            }
            else if (pred1.suxAt(0).equals(bb) && pred1.suxAt(1).equals(pred2))
            {
                resultBBs[0] = pred1;
                resultBBs[1] = pred2;
            }
            else
                return null;
            return pred1Br.getCondition();
        }

        // If we got here that indicates both two preds's branch instruction aren't conditional.
        // So it should looks like following figure.
        //
        //      commonPred
        //      /        \
        //     /          \
        // true(pred1) false(pred2)
        //     \         |
        //      \       /
        //      current BB
        BasicBlock commonPred = pred1.getSinglePredecessor();
        if (commonPred == null || !commonPred.equals(pred2.getSinglePredecessor()))
            return null;
        if (commonPred.getTerminator() instanceof BranchInst)
        {
            BranchInst br = (BranchInst) commonPred.getTerminator();
            if (br.isUnconditional())
                return null;

            if (br.getSuccessor(0).equals(pred1) && br.getSuccessor(1).equals(pred2))
            {
                resultBBs[0] = pred1;
                resultBBs[1] = pred2;
            }
            else
            {
                resultBBs[0] = pred2;
                resultBBs[1] = pred1;
            }
            return br.getCondition();
        }
        return null;
    }

    private boolean dominatesMergeBlock(Value cond, BasicBlock bb,
            HashSet<Instruction> aggressiveInst)
    {
        // We assume all non-instruction always dominates the merge block.
        if (!(cond instanceof Instruction))
            return true;

        Instruction inst = (Instruction)cond;
        BasicBlock pred = inst.getParent();

        // Currently, we aren't going handle self-loop dominates.
        if (Objects.equals(pred, bb))
            return false;

        if (pred.getTerminator() instanceof BranchInst)
        {
            BranchInst br = (BranchInst)pred.getTerminator();
            if (br.isUnconditional() && br.getSuccessor(0).equals(bb))
            {
                if (aggressiveInst == null) return false;
                if (!inst.isSafeToSpecutativelyExecute())
                    return false;

                switch (inst.getOpcode())
                {
                    default: return false;
                    case Load:
                        // We need to check if there are any other instruction precedes inst
                        // in pred block. Because we want to move inst up to dominates block.
                        if (!inst.equals(pred.getFirstInst()))
                            return false;
                        break;
                    case Add:
                    case Sub:
                    case And:
                    case Or:
                    case Xor:
                    case Shl:
                    case AShr:
                    case LShr:
                    case ICmp:
                        break;
                }

                for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
                {
                    if (!dominatesMergeBlock(inst.operand(i), bb, null))
                        return false;
                }
                // add the inst into aggressiveInst set.
                aggressiveInst.add(inst);
            }
        }
        return true;
    }

    private boolean foldTwoEntitiesPhiNode(PhiNode pn)
    {
        Util.assertion(pn != null);
        BasicBlock bb = pn.getParent();
        // array used for storing ifTrue and ifFalse block.
        BasicBlock[] bbs = new BasicBlock[2];
        Value condVal = getIfCondition(bb, bbs);
        BasicBlock ifTrue = bbs[0], ifFalse = bbs[1];
        if (condVal == null)
            return false;

        HashSet<Instruction> aggressiveInst = new HashSet<>();
        int i = 0, e = bb.size();
        while (i < e && bb.getInstAt(i) instanceof PhiNode)
        {
            PhiNode inst = (PhiNode)bb.getInstAt(i);
            Value val0 = inst.getIncomingValue(0), val1 = inst.getIncomingValue(1);
            if (val0.equals(val1))
            {
                if (!val0.equals(inst))
                    inst.replaceAllUsesWith(val0);
                else
                    inst.replaceAllUsesWith(UndefValue.get(inst.getType()));
            }
            else
            {
                if (!dominatesMergeBlock(val0, bb, aggressiveInst) ||
                        dominatesMergeBlock(val1, bb, aggressiveInst))
                    return false;
            }
        }
        pn = (PhiNode) bb.getFirstInst();
        BasicBlock pred = pn.getIncomingBlock(0);
        BasicBlock ifBlock0 = null, ifBlock1 = null;
        BasicBlock domBlock = null;
        if (pred.getTerminator() instanceof BranchInst)
        {
            BranchInst br = (BranchInst)pred.getTerminator();
            if (br.isUnconditional())
            {
                ifBlock0 = pred;
                domBlock = ifBlock0.predAt(0);
                for (int j = 0, sz = pred.size(); j < sz &&
                        !(pred.getInstAt(j) instanceof TerminatorInst);)
                {
                    // If it isn't worth moving those instruction from if's part up
                    // to dominates block.
                    if (!aggressiveInst.contains(pred.getInstAt(j)))
                        return false;
                }
            }
        }

        pred = pn.getIncomingBlock(1);
        if (pred.getTerminator() instanceof BranchInst)
        {
            BranchInst br = (BranchInst)pred.getTerminator();
            if (br.isUnconditional())
            {
                ifBlock1 = pred;
                domBlock = ifBlock1.predAt(0);
                for (int j = 0, sz = pred.size(); j < sz &&
                        !(pred.getInstAt(j) instanceof TerminatorInst);)
                {
                    // If it isn't worth moving those instruction from if's part up
                    // to dominates block.
                    if (!aggressiveInst.contains(pred.getInstAt(j)))
                        return false;
                }
            }
        }

        // Move all instructions except for terminator up to dom block.
        if (ifBlock0 != null)
        {
            Util.assertion(domBlock != null);
            Instruction term = domBlock.getTerminator();
            Util.assertion(term != null);
            i = 0;
            e = ifBlock0.size();
            for (; i < e && !(ifBlock0.getInstAt(i) instanceof TerminatorInst); i++)
            {
                domBlock.insertBefore(ifBlock0.getInstAt(i), term);
            }
        }
        if (ifBlock1 != null)
        {
            Util.assertion(domBlock != null);
            Instruction term = domBlock.getTerminator();
            Util.assertion(term != null);
            i = 0;
            e = ifBlock1.size();
            for (; i < e && !(ifBlock1.getInstAt(i) instanceof TerminatorInst); i++)
            {
                domBlock.insertBefore(ifBlock1.getInstAt(i), term);
            }
        }
        Instruction curInst;
        while (!bb.isEmpty() && (curInst = bb.getFirstInst()) instanceof PhiNode)
        {
            pn = (PhiNode) curInst;
            Value falseVal = pn.getIncomingValue(Objects.equals(ifBlock0, ifTrue) ? 1:0);
            Value trueVal = pn.getIncomingValue(Objects.equals(ifBlock1, ifFalse) ? 1:0);
            Instruction select = new SelectInst(condVal, trueVal, falseVal, "", pn);
            pn.replaceAllUsesWith(select);
            select.setName(pn.getName());
            pn.eraseFromParent();
        }
        return true;
    }

    private boolean isTerminateInstFirstRelevantInst(BasicBlock bb, TerminatorInst ret)
    {
        int i = 0, e = bb.size();
        for (; i < e && bb.getInstAt(i) instanceof PhiNode; i++){}
        if (i < e - 1) return false;
        if (!bb.getInstAt(i).equals(ret)) return false;
        return true;
    }

    private static void eraseTerminatorInstAndDCECond(TerminatorInst ti)
    {
        if (ti == null) return;
        Value cond = null;
        if (ti instanceof BranchInst)
        {
            BranchInst br = (BranchInst)ti;
            if (br.isConditional())
                cond = br.getCondition();
        }
        else if (ti instanceof SwitchInst)
        {
            cond = ((SwitchInst)ti).getCondition();
        }
        else
            return;
        ti.eraseFromParent();
        if (cond != null)
            recursivelyDeleteTriviallyDeadInstructions(cond);
    }

    /**
     * If we find a conditional branch to two return blocks, we going to try to merge those
     * blocks into a single one, when desired, folding branch instruction to select instruction.
     * @param br
     * @return
     */
    private boolean simplifyCondBranchToTwoReturns(BranchInst br)
    {
        Util.assertion(br != null && br.isConditional(), "Must be a conditional branch");
        BasicBlock curBB = br.getParent();

        BasicBlock retBB1 = br.getSuccessor(0), retBB2 = br.getSuccessor(1);
        ReturnInst rt1 = (ReturnInst) retBB1.getTerminator(), rt2 = (ReturnInst) retBB2.getTerminator();
        Util.assertion(rt1 != null && rt2 != null);

        if (!isTerminateInstFirstRelevantInst(retBB1, rt1))
            return false;
        if (!isTerminateInstFirstRelevantInst(retBB2, rt2))
            return false;

        // If we find there is no return value in this function, just folding the branch into
        // return regarding of other thing.
        if (rt1.getNumOfOperands() == 0)
        {
            retBB1.removePredecessor(curBB);
            retBB2.removePredecessor(curBB);
            new ReturnInst(null, "", br);
            eraseTerminatorInstAndDCECond(br);
            return true;
        }

        // If we going to here, we know situation to be hanled as follows.
        //    br cond, bb1, bb2
        //   /            \
        // retBB1         retBB2
        Value trueVal = rt1.getReturnValue();
        Value falseVal = rt2.getReturnValue();
        if (trueVal instanceof PhiNode && ((PhiNode) trueVal).getParent() == retBB1)
            trueVal = ((PhiNode)trueVal).getIncomingValueForBlock(curBB);
        if (falseVal instanceof PhiNode && ((PhiNode) falseVal).getParent() == retBB2)
            falseVal = ((PhiNode)falseVal).getIncomingValueForBlock(curBB);

        retBB1.removePredecessor(curBB);
        retBB2.removePredecessor(curBB);

        Value cond = br.getCondition();
        // if falseVal == trueVal, select inst isn't needed, just return inst is desired.
        if (trueVal != null)
        {
            // Insert a select if the results differ.
            if (trueVal.equals(falseVal))
            {
                // do nothing.
            }
            else if (!(trueVal instanceof UndefValue) && !(falseVal instanceof UndefValue))
            {
                trueVal = new SelectInst(cond, trueVal, falseVal,"retVal",br);
            }
            else
            {
                if (!(falseVal instanceof UndefValue))
                    trueVal = falseVal;
            }
        }
        Instruction retInst = trueVal != null ? new ReturnInst(trueVal, "ret", br)
                : new ReturnInst(br);
        if (Util.DEBUG)
        {
            System.err.println("Change branch to two return block with: ");
            System.err.print("New Inst: ");
            retInst.dump();
            System.err.println();
            System.err.printf("True Block: %s, False Block: %d\n", retBB1.getName(), retBB2.getName());
        }
        eraseTerminatorInstAndDCECond(br);
        return true;
    }

    /**
     * If this is a returning block with only PHI nodes in it, fold the return
     * instruction into any unconditional branch predecessors.
     * <br></br>
     * If any predecessor is a conditional branch that just selects among
     * different return values, fold the replace the branch/return with a select
     * and return.
     * @param bb
     * @return
     */
    private boolean foldOnlyReturnInBlock(BasicBlock bb)
    {
        Util.assertion(bb != null && bb.getTerminator() instanceof ReturnInst);
        if (!isTerminateInstFirstRelevantInst(bb, bb.getTerminator()))
            return false;

        ReturnInst rt = (ReturnInst)bb.getTerminator();

        // gathers unconditonal and conditional branch instruction from all preds.
        HashSet<BasicBlock> unconditionals = new HashSet<>();
        HashSet<BranchInst> conditionals = new HashSet<>();
        for (Iterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
        {
            BasicBlock pred = itr.next();
            Util.assertion(pred.getTerminator() instanceof BranchInst);
            BranchInst br = (BranchInst) pred.getTerminator();
            if (br.isUnconditional())
                unconditionals.add(pred);
            else
                conditionals.add(br);
        }

        if (!unconditionals.isEmpty())
        {
            // converts unconditional jump in pred to return inst.
            for (BasicBlock pred : unconditionals)
            {
                ReturnInst ret = (ReturnInst) rt.clone();
                TerminatorInst ti = pred.getTerminator();
                Util.assertion(ti != null);
                pred.insertBefore(ti, ret);

                // If the return inst has operands and it's operand is phinode, just do so.
                for (int j = 0, sz = ret.getNumOfOperands(); j < sz; j++)
                {
                    Value op = ret.operand(j);
                    if (op instanceof PhiNode)
                    {
                        PhiNode pn = (PhiNode)op;
                        ret.setOperand(j, pn.getIncomingValueForBlock(pred));
                    }
                }
                bb.removePredecessor(pred);
                ti.eraseFromParent();
            }
            // If after remove all preds, the current block becomes unreachable, so delete it.
            if (bb.getNumPredecessors() <= 0)
                bb.eraseFromParent();
            return true;
        }
        if (!conditionals.isEmpty())
        {
            boolean localChanged = false;
            for (BranchInst br : conditionals)
            {
                if (br.getSuccessor(0).getTerminator() instanceof ReturnInst &&
                    br.getSuccessor(0).getTerminator() instanceof ReturnInst &&
                    simplifyCondBranchToTwoReturns(br))
                    localChanged |= true;
            }
            return localChanged;
        }
        return false;
    }

    /**
     * This function used for do a simplification of a CFG. For example, it
     * adjusts branches to branches to eliminate the extra hop. It eliminates
     * unreachable basic blocks ,and does other "peephole" optimization of the
     * CFG.
     * @param bb    The basic block to be simplified.
     * @return Returns true if a modification was made.
     */
    private boolean simplifyCFG(BasicBlock bb)
    {
        boolean changed = false;
        Function f = bb.getParent();

        Util.assertion(f != null, "Basic block not embedded in function");
        Util.assertion(bb.getTerminator() != null, "degnerate basic block is bing simplified");
        Util.assertion(bb != f.getEntryBlock(), "Can not simplify entry block");

        // Remove basic blocks that have no predecessors... or that just have themself
        // as a predecessor.  These are unreachable.
        int numPreds = bb.getNumPredecessors();
        if (numPreds <= 1 && (numPreds <= 0 || Objects.equals(bb.predAt(0), bb)))
        {
            deleteDeadBlock(bb);
            return true;
        }

        // If there is a trivial two-entry PHI node in this basic block, and we can
        // eliminate it, do so now.
        PhiNode pn = bb.getFirstInst() instanceof PhiNode ? (PhiNode)bb.getFirstInst() : null;
        if (pn != null && pn.getNumberIncomingValues() == 2)
        {
            changed |= foldTwoEntitiesPhiNode(pn);
        }
        // If this is a returning block with only PHI nodes in it, fold the return
        // instruction into any unconditional branch predecessors.

        // If any predecessor is a conditional branch that just selects among
        // different return values, fold the replace the branch/return with a select
        // and return.
        Util.assertion(bb.getTerminator() != null);
        if (bb.getTerminator() instanceof ReturnInst)
            changed |= foldOnlyReturnInBlock(bb);
        else if (bb.getTerminator() instanceof SwitchInst)
        {}
        else if (bb.getTerminator() instanceof BranchInst)
        {}
        else if (bb.getTerminator() instanceof UnreachableInst)
        {}

        return changed;
    }

    @Override
    public String getPassName()
    {
        return "Simplify CFG pass";
    }

    public static FunctionPass createCFGSimplificationPass()
    {
        return new CFGSimplifyPass();
    }
}
