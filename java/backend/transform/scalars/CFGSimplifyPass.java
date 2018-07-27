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
import backend.support.LLVMContext;
import backend.transform.utils.ConstantFolder;
import backend.type.PointerType;
import backend.utils.SuccIterator;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Instruction.CmpInst.Predicate;
import backend.value.Value.UndefValue;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;

import java.util.*;

import static backend.transform.utils.ConstantFolder.*;

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
        if (f == null || f.empty())
            return false;
        boolean everChanged = removeUnreachableBlocksFromFn(f);
        everChanged |= simplifyCFG(f);
        if (!everChanged) return false;

        boolean changed;
        do
        {
            changed = false;
            changed |= simplifyCFG(f);
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
            if (reachables.contains(currBB))
                continue;
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
                        !dominatesMergeBlock(val1, bb, aggressiveInst))
                    return false;
            }
            ++i;
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
            Value trueVal = pn.getIncomingValue(Objects.equals(ifBlock1, ifFalse) ? 0:1);
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

    private boolean simplifyCFG(Function f)
    {
        boolean changed = false;
        // Loop over all of the basic blocks (except the first one) and remove them
        // if they are unneeded.
        ArrayList<BasicBlock> worklist = new ArrayList<>(f.getBasicBlockList());
        for (int i = 1, e = worklist.size(); i < e; i++)
        {
            if (simplifyCFG(worklist.get(i)))
            {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Checks if it is safe to merge bb into succ block.
     * @param bb
     * @param succ
     * @return
     */
    private boolean canPropagatePredecessorsForPHIs(BasicBlock bb, BasicBlock succ)
    {
        Util.assertion(bb.getTerminator() instanceof BranchInst);
        BranchInst br = (BranchInst) bb.getTerminator();
        Util.assertion(br.isUnconditional());
        Util.assertion(br.getSuccessor(0) == succ);

        BasicBlock pred = succ.getSinglePredecessor();
        if (pred != null && pred == bb)
            return true;

        HashSet<BasicBlock> bbPreds = new HashSet<>();
        for (Iterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
            bbPreds.add(itr.next());

        HashSet<BasicBlock> commonPreds = new HashSet<>();
        for (Iterator<BasicBlock> itr = succ.predIterator(); itr.hasNext();)
        {
            BasicBlock b = itr.next();
            if (bbPreds.contains(b))
                commonPreds.add(b);
        }

        if (commonPreds.isEmpty())
            return true;

        // Look at all the phi nodes in Succ, to see if they present a conflict when
        // merging these blocks
        for (int i = 0, e = succ.size(); i < e && succ.getInstAt(i) instanceof PhiNode; i++)
        {
            PhiNode pn = (PhiNode) succ.getInstAt(i);
            Value val = pn.getIncomingValueForBlock(bb);
            PhiNode pn2 = val instanceof PhiNode ? (PhiNode) val : null;
            if (pn2 != null && pn2.getParent() == bb)
            {
                for (BasicBlock commonPred : commonPreds)
                {
                    if (!pn2.getIncomingValueForBlock(commonPred).
                            equals(pn.getIncomingValueForBlock(commonPred)))
                        return false;
                }
            }
            else
            {
                for (BasicBlock commonPred : commonPreds)
                {
                    if (!val.equals(pn.getIncomingValueForBlock(commonPred)))
                        return false;
                }
            }
        }
        return true;
    }

    private boolean tryToSimplifyUncondBranchFromEmptyBlock(BasicBlock bb, BasicBlock succ)
    {
        if (!canPropagatePredecessorsForPHIs(bb, succ)) return false;

        if (succ.getSinglePredecessor() == null)
        {
            for (int i = 0, e = bb.size(); i < e && bb.getInstAt(i) instanceof PhiNode; i++)
            {
                PhiNode pn = (PhiNode)bb.getInstAt(i);
                for (Use u : pn.getUseList())
                {
                    if (u.getUser() instanceof PhiNode)
                    {
                        PhiNode pn2 = (PhiNode)u.getUser();
                        if (pn2.getIncomingBlock(u) != bb)
                            return false;
                    }
                    else
                        return false;
                }
            }
        }

        if (succ.getFirstInst() instanceof PhiNode)
        {
            ArrayList<BasicBlock> preds = new ArrayList<>();
            for (Iterator<BasicBlock> itr = bb.predIterator(); itr.hasNext(); )
                preds.add(itr.next());

            for (int i = 0, e = succ.size(); i < e && succ.getInstAt(i) instanceof PhiNode; i++)
            {
                PhiNode pn = (PhiNode) succ.getInstAt(i);
                Value oldVal = pn.removeIncomingValue(bb, false);
                Util.assertion(oldVal != null, "No entry in PHI node!");

                if (oldVal instanceof PhiNode && ((PhiNode) oldVal).getParent() == bb)
                {
                    PhiNode oldValPN = (PhiNode)oldVal;
                    for (int j = 0, sz = oldValPN.getNumberIncomingValues(); j < sz; j++)
                    {
                        pn.addIncoming(oldValPN.getIncomingValue(j),
                                oldValPN.getIncomingBlock(j));
                    }
                }
                else
                {
                    for (BasicBlock b : preds)
                        pn.addIncoming(oldVal, b);
                }
            }
        }

        if (succ.getSinglePredecessor() != null)
        {
            while (!bb.isEmpty() && bb.getFirstInst() instanceof PhiNode)
            {
                Instruction inst = bb.getFirstInst();
                inst.eraseFromParent();
                succ.insertAt(inst, 0);
            }
        }
        else
        {
            while (!bb.isEmpty() && bb.getFirstInst() instanceof PhiNode)
            {
                Instruction inst = bb.getFirstInst();
                Util.assertion(inst.isUseEmpty());
                inst.eraseFromParent();
            }
        }

        bb.replaceAllUsesWith(succ);
        if (!succ.hasName())
            succ.setName(bb.getName());
        bb.eraseFromParent();
        return true;
    }

    private boolean foldBranchInstAsTermInst(BasicBlock bb)
    {
        Util.assertion(bb != null && bb.getTerminator() instanceof BranchInst);
        BranchInst br = (BranchInst)bb.getTerminator();

        if (br.isUnconditional())
        {
            Instruction firstNotPhi = bb.getInstAt(bb.getFirstNonPhi());
            BasicBlock succ = br.getSuccessor(0);

            if (firstNotPhi == br && bb != succ)
                if (tryToSimplifyUncondBranchFromEmptyBlock(bb, succ))
                    return true;
        }
        else
        {
            // conditional branch to two successors.
            if (isValueEqualityComparison(br) != null)
            {
                // If we have only one predecessor, and if
                // it is branch on the same value as br's condition,
                // check to see if that predecessor totally determines
                // the outcome of this branch.
                BasicBlock onlyPred = bb.getSinglePredecessor();
                if (onlyPred != null)
                {
                    if (simplifyEqualityComparisonWithOnlyPredecessor(br, onlyPred))
                    {
                        simplifyCFG(bb);
                        return true;
                    }
                }

                // Reaches here, this block must be empty, except for the
                // condition and branch
                if (bb.getFirstInst() == br)
                {
                    if (foldValueComparisonIntoPredecessors(br))
                    {
                        simplifyCFG(bb);
                        return true;
                    }
                }
                else if (bb.getFirstInst() == br.getCondition())
                {
                    if (bb.getInstAt(1) == br &&
                            foldValueComparisonIntoPredecessors(br))
                    {
                        simplifyCFG(bb);
                        return true;
                    }
                }
            }

            // If this is a branch on a phi node in the current block, thread control
            // through this block if any PHI node entries are constants.
            if (br.getCondition() instanceof PhiNode)
            {
                PhiNode pn = (PhiNode) br.getCondition();
                if (pn.getParent() == bb && foldCondBranchOnPhi(br))
                {
                    simplifyCFG(bb);
                    return true;
                }
            }
            if (foldBranchToCommonDest(br))
            {
                simplifyCFG(bb);
                return true;
            }
            for (Iterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
            {
                BasicBlock pred = itr.next();
                if(pred.getTerminator() instanceof BranchInst)
                {
                    BranchInst predBR = (BranchInst) pred.getTerminator();
                    if (predBR != br && predBR.isConditional() &&
                            simplifyCondBranchToCondBranch(predBR, br))
                    {
                        simplifyCFG(bb);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * We attempt to simplify this branch to another block has also a conditional branch.
     * @param predBR
     * @param br
     * @return
     */
    private boolean simplifyCondBranchToCondBranch(BranchInst predBR, BranchInst br)
    {
        Util.assertion(predBR.isConditional() && br.isConditional());
        BasicBlock bb = br.getParent();
        Value cond = br.getCondition();

        if (predBR.getCondition().equals(br.getCondition()) &&
                predBR.getSuccessor(0) != predBR.getSuccessor(1))
        {
            BasicBlock singlePred = bb.getSinglePredecessor();
            if (singlePred != null)
            {
                boolean condIsTrue = predBR.getSuccessor(0) == bb;
                br.setCondition(condIsTrue?ConstantInt.getTrue():ConstantInt.getFalse());
                return true;
            }

            if (blockIsSimpleEnoughToThreadThrough(bb))
            {
                PhiNode newPHI = new PhiNode(LLVMContext.Int1Ty, cond.getName()+".pr", bb.getFirstInst());
                for (Iterator<BasicBlock> itr = bb.predIterator(); itr.hasNext(); )
                {
                    BasicBlock pred = itr.next();
                    BranchInst brInt = pred.getTerminator() instanceof BranchInst ?
                            (BranchInst)pred.getTerminator() : null;
                    if (brInt != null && brInt.getCondition().equals(cond) &&
                            brInt.isConditional() && !brInt.equals(br) &&
                            brInt.getSuccessor(0) != brInt.getSuccessor(1))
                    {
                        boolean condIsTrue = brInt.getSuccessor(0) == bb;
                        newPHI.addIncoming(condIsTrue?ConstantInt.getTrue():ConstantInt.getFalse(),
                                pred);
                    }
                    else
                    {
                        newPHI.addIncoming(brInt.getCondition(), pred);
                    }
                }
                br.setCondition(newPHI);
                return true;
            }
        }

        // If this is a conditional branch in an empty block, and if any
        // predecessors is a conditional branch to one of our destinations,
        // fold the conditions into logical ops and one cond br.
        if (bb.size() != 1)
            return false;

        if (cond instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr) cond;
            if (ce.canTrap())
                return false;
        }

        int predBROp, brOp;
        if (predBR.getSuccessor(0) == br.getSuccessor(0))
            predBROp = brOp = 0;
        else if (predBR.getSuccessor(0) == br.getSuccessor(1))
        { predBROp = 0; brOp = 1; }
        else if (predBR.getSuccessor(1) == br.getSuccessor(0))
        { predBROp = 1; brOp = 0; }
        else if (predBR.getSuccessor(1) == br.getSuccessor(1))
            predBROp = brOp = 1;
        else
            return false;

        // avoiding self-loop.
        if (predBR.getSuccessor(predBROp) == bb)
            return false;

        BasicBlock commonDest = predBR.getSuccessor(predBROp);
        int numPhis = 0;
        for (Instruction inst : commonDest)
        {
            if (inst instanceof PhiNode)
                ++numPhis;
            if (numPhis > 2)
                return false;
        }

        BasicBlock otherDest = br.getSuccessor(brOp^1);
        // self-loop
        //   <----|
        //   |    |
        //  bb    |
        //   |---->
        if (otherDest == bb)
        {
            BasicBlock infLoopBlock = BasicBlock.createBasicBlock("infloop", bb.getParent());
            new BranchInst(infLoopBlock, infLoopBlock);
            otherDest = infLoopBlock;
        }

        Value predBRCond = predBR.getCondition();
        if (predBROp != 0)
            predBRCond = BinaryOps.createNot(predBRCond, predBRCond+".not", predBR);

        Value brCond = cond;
        if (brOp != 0)
            brCond = BinaryOps.createNot(brCond, brCond.getName()+".not", br);

        Value mergedCond = BinaryOps.createOr(predBRCond, brCond, "mergebr", predBR);
        predBR.setCondition(mergedCond);
        predBR.setSuccessor(0, commonDest);
        predBR.setSuccessor(1, otherDest);

        // OtherDest may have phi nodes.  If so, add an entry from PBI's
        // block that are identical to the entries for BI's block.
        PhiNode pn;
        for (Instruction inst : otherDest)
        {
            if (!(inst instanceof PhiNode))
                break;
            pn = (PhiNode) inst;
            Value v = pn.getIncomingValueForBlock(bb);
            pn.addIncoming(v, predBR.getParent());
        }

        // We know that the CommonDest already had an edge from PBI to
        // it.  If it has PHIs though, the PHIs may have different
        // entries for BB and PBI's BB.  If so, insert a select to make
        // them agree.
        for (Instruction inst : commonDest)
        {
            if (!(inst instanceof PhiNode))
                break;
            pn = (PhiNode) inst;
            Value bbVal = pn.getIncomingValueForBlock(bb);
            int predIdx = pn.getBasicBlockIndex(predBR.getParent());
            Value predVal = pn.getIncomingValue(predIdx);

            if (!bbVal.equals(predVal))
            {
                bbVal = new SelectInst(predBRCond, bbVal, predVal,
                        predBR.getName()+".mux", predBR);
                pn.setIncomingValue(predIdx, bbVal);
            }
        }
        return true;
    }

    /**
     * Thread through the specified basic block if there is only setcc and branch inst
     * existing in the br's block and there is common successor of pred and current block,
     * we attempt to transform the branch in predecessor to branch based on predication
     * condition, reducing branch to current block.
     * @param br
     * @return
     */
    private boolean foldBranchToCommonDest(BranchInst br)
    {
        BasicBlock bb = br.getParent();
        Util.assertion(bb != null, "Parent of br must not be null!");
        Value c = br.getCondition();
        Util.assertion(c instanceof Instruction, "cond must be instruction");
        Instruction cond = (Instruction) c;
        // we should ensure the condition resides in the same block as br.
        if (cond.getParent() != bb) return false;

        // we should ensure there are setcc and branch inst in current block.
        Instruction firstInst = bb.getFirstInst();
        if (firstInst != cond || (!(firstInst instanceof ICmpInst) &&
                !(firstInst instanceof BinaryOps)) || !cond.hasOneUses())
            return false;

        // the current block must only have 2 instructions, including condition and
        // branch inst.
        if (bb.getInstAt(1) != br)
            return false;

        // avoiding self-loop.
        BasicBlock trueDest = br.getSuccessor(0);
        BasicBlock falseDest = br.getSuccessor(1);
        if (trueDest == bb || falseDest == bb)
            return false;

        if (cond.operand(0) instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr) cond.operand(0);
            if (ce.canTrap())
                return false;
        }
        if (cond.operand(1) instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr) cond.operand(1);
            if (ce.canTrap())
                return false;
        }

        for (Iterator<BasicBlock> itr = bb.predIterator(); itr.hasNext(); )
        {
            BasicBlock pred = itr.next();
            TerminatorInst ti = pred.getTerminator();
            Util.assertion(ti instanceof BranchInst);
            BranchInst predBR = (BranchInst) ti;

            boolean invertPredCond = false;
            Operator opc = null;
            if (predBR.getSuccessor(0) == trueDest)
            {
                //     pred  ...
                //    /     \
                //   |      bb
                //   |   /     \
                //   trueDest  falseDest
                opc = Operator.Or;
            }
            else if (predBR.getSuccessor(1) == trueDest)
            {
                //          pred  ...
                //       /       \
                //      bb         \
                //   /      \        \
                // trueDest falseDest |
                //  |<---------------/
                opc = Operator.Or;
                invertPredCond = true;
            }
            else if (predBR.getSuccessor(0) == falseDest)
            {
                opc = Operator.And;
                invertPredCond = true;
            }
            else if (predBR.getSuccessor(1) == falseDest)
            {
                //          pred  ...
                //       /      \
                //      bb       \
                //   /     \      \
                // trueDest  falseDest
                opc = Operator.Or;
            }
            Util.assertion(opc != null, "unknown situation to be handled!");
            if (invertPredCond)
            {
                Instruction newCond = BinaryOps.createNot(cond, cond.getName()+".not", predBR);
                predBR.setCondition(newCond);
                BasicBlock succ0 = predBR.getSuccessor(0);
                BasicBlock succ1 = predBR.getSuccessor(1);
                predBR.setSuccessor(0, succ1);
                predBR.setSuccessor(1, succ0);
            }
            Instruction v = cond.clone();
            pred.insertBefore(v, predBR);
            cond.setName(cond.getName()+".old");
            Instruction newCond = BinaryOps.create(opc, predBR.getCondition(),
                    v, "or.cond", predBR);
            predBR.setCondition(newCond);
            if (predBR.getSuccessor(0) == bb)
            {
                addPredecessorToBlock(trueDest, pred, bb);
                predBR.setSuccessor(0, trueDest);
            }
            if (predBR.getSuccessor(1) == bb)
            {
                addPredecessorToBlock(falseDest, pred, bb);
                predBR.setSuccessor(1, falseDest);
            }
            return true;
        }
        return false;
    }

    private boolean foldCondBranchOnPhi(BranchInst br)
    {
        BasicBlock bb = br.getParent();
        PhiNode pn = br.getCondition() instanceof PhiNode ?
                (PhiNode)br.getCondition() : null;
        if (pn == null || pn.getParent() != bb || !pn.hasOneUses())
            return false;
        if (pn.getNumberIncomingValues() == 1)
        {
            foldSingleEntryPhiNodes(bb);
            return true;
        }
        // the block must be simple enough.
        if (!blockIsSimpleEnoughToThreadThrough(bb)) return false;

        // So if we reach here, we know the block have multiple predecessors
        // and two successors. Checks to see if any phi nodes are constant value.
        // CFG maybe looks like following figure.
        //  pred1  ... predn
        //     \     /
        //       bb(condition on phinode)
        //       |
        //    realDest
        // transformed into following.
        //  pred1  ...  predn
        //    |         |
        //    \    bb   |
        //      \  |   /
        //       edgeBB
        //       |
        //    realDest

        for (int i = 0, e = pn.getNumberIncomingValues(); i < e; i++)
        {
            Value val = pn.getIncomingValue(i);
            if (val instanceof ConstantInt && val.getType() == LLVMContext.Int1Ty)
            {
                ConstantInt ci = (ConstantInt)val;
                BasicBlock predBlock = pn.getIncomingBlock(i);
                BasicBlock realDest = br.getSuccessor(ci.getZExtValue() !=0? 0 : 1);

                // avoiding self-loop.
                if (realDest == bb) continue;

                BasicBlock edgeBB = BasicBlock.createBasicBlock(
                        realDest.getName()+".critedge",
                        realDest.getParent(), realDest);
                // insert a branch targeting to realDest in the end of edgeBB.
                new BranchInst(realDest, edgeBB);
                // create phi-value connection between realDest and edgeBB.
                for (Instruction inst : realDest)
                {
                    if (!(inst instanceof PhiNode))
                        break;
                    PhiNode n = (PhiNode) inst;
                    Value v = n.getIncomingValueForBlock(bb);
                    n.addIncoming(v, edgeBB);
                }

                // set the inserted position pointer to the begining of edgeBB.
                int insertPtr = 0;
                HashMap<Value, Value> translateMap = new HashMap<>();
                // Track translated values.
                for (Instruction inst : bb)
                {
                    if (inst instanceof PhiNode)
                    {
                        Value v = pn.getIncomingValueForBlock(predBlock);
                        translateMap.put(pn, v);
                    }
                    else
                    {
                        // Clone this instruction.
                        Instruction n = inst.clone();
                        if (inst.hasName())
                            n.setName(inst.getName()+".c");

                        // update all operands refer to phi with given value.
                        for (int j = 0, sz = n.getNumOfOperands(); j < sz;j++)
                        {
                            Value v = n.operand(j);
                            if (translateMap.containsKey(v))
                                n.setOperand(j, translateMap.get(v));
                        }

                        // insert the cloned instruction into edgeBB.
                        Constant c = ConstantFolder.constantFoldInstruction(n);
                        if (c != null)
                        {
                            translateMap.put(inst, c);
                            inst.eraseFromParent();
                        }
                        else
                        {
                            edgeBB.insertAfter(n, insertPtr);
                            ++insertPtr;
                            if (!inst.isUseEmpty())
                                translateMap.put(inst, n);
                        }
                    }
                }

                // Loop over all of the edges from PredBB to BB, changing them to branch
                // to EdgeBB instead.
                TerminatorInst ti = predBlock.getTerminator();
                Util.assertion(ti != null);
                for (int j = 0, sz = predBlock.getNumSuccessors(); j < sz; j++)
                {
                    BasicBlock succ = predBlock.suxAt(j);
                    if (succ == bb)
                    {
                        bb.removePredecessor(predBlock);
                        ti.setSuccessor(j, edgeBB);
                    }
                }
                // recursively simplify this branch.
                foldCondBranchOnPhi(br);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if the specified block is simple enough to be thread
     * through and reduce the complexity of CFG.
     * @param bb
     * @return
     */
    private boolean blockIsSimpleEnoughToThreadThrough(BasicBlock bb)
    {
        BranchInst br = (BranchInst) bb.getTerminator();
        int size = 0;
        for (Instruction inst : bb)
        {
            // can not thread through large block.
            if (size > 10) return false;
            ++size;

            for (Use u : inst.getUseList())
            {
                Instruction uInst = (Instruction) u.getUser();
                if (uInst.getParent() != bb || uInst instanceof PhiNode)
                    return false;
            }
        }
        return true;
    }

    private void foldSingleEntryPhiNodes(BasicBlock bb)
    {
        if (!(bb.getFirstInst() instanceof PhiNode))
            return;

        while (bb.getFirstInst() instanceof PhiNode)
        {
            PhiNode pn = (PhiNode) bb.getFirstInst();
            Value onlyEntry = pn.getIncomingValue(0);
            if (onlyEntry.equals(pn))
                pn.replaceAllUsesWith(UndefValue.get(pn.getType()));
            else
                pn.replaceAllUsesWith(onlyEntry);
            pn.eraseFromParent();
        }
    }

    /**
     * Returns it if the specified terminator instr checks to see if the
     * condition is equal to a constant integer value.
     * @param ti
     * @return
     */
    private Value isValueEqualityComparison(TerminatorInst ti)
    {
        Util.assertion(ti != null);
        if (ti instanceof BranchInst)
        {
            BranchInst br = (BranchInst)ti;
            Value cond = br.getCondition();
            if (br.isConditional() && cond instanceof ICmpInst)
            {
                ICmpInst ci = (ICmpInst)cond;
                if ((ci.getPredicate() == Predicate.ICMP_EQ ||
                        ci.getPredicate() == Predicate.ICMP_NE))
                {
                    if (ci.operand(0) instanceof ConstantInt)
                    {
                        ci.swapOperands();
                    }
                    if (ci.operand(1) instanceof ConstantInt)
                        return ci.operand(0);
                }
            }
        }
        else if (ti instanceof SwitchInst)
        {
            // Don't permit to merge successor into it's predecessor unless there is
            // exactly one predecessor.
            if (ti.getNumOfSuccessors() * ti.getParent().getNumPredecessors() > 128)
                return null;

            return ((SwitchInst) ti).getCondition();
        }
        return null;
    }

    private BasicBlock getValueEqualityComparisonCases(TerminatorInst ti,
            ArrayList<Pair<ConstantInt, BasicBlock>> cases)
    {
        if (ti instanceof SwitchInst)
        {
            SwitchInst si = (SwitchInst) ti;
            for (int i = 1, e = si.getNumOfCases(); i < e; i++)
            {
                cases.add(Pair.get(si.getSuccessorValue(i), si.getSuccessor(i)));
            }
            return si.getDefaultBlock();
        }
        else
        {
            Util.assertion(ti instanceof BranchInst);
            BranchInst br = (BranchInst) ti;
            Util.assertion(br.getCondition() instanceof ICmpInst);
            ICmpInst ic = (ICmpInst) br.getCondition();
            ConstantInt ci = (ConstantInt) ic.operand(1);
            BasicBlock bb = br.getSuccessor(ic.getPredicate() == Predicate.ICMP_EQ ?0:1);
            BasicBlock defaultBB = br.getSuccessor(ic.getPredicate() == Predicate.ICMP_EQ ?1:0);
            cases.add(Pair.get(ci, bb));
            return defaultBB;
        }
    }

    private void eliminateBlockCases(BasicBlock defaultBB, ArrayList<Pair<ConstantInt, BasicBlock>> cases)
    {
        for (int i = 0, e = cases.size(); i< e; i++)
        {
            if (cases.get(i).second == defaultBB)
            {
                cases.remove(i);
                --i;
                --e;
            }
        }
    }

    private static final Comparator<ConstantInt> ConstantIntOrdering = new Comparator<ConstantInt>()
    {
        @Override
        public int compare(ConstantInt o1, ConstantInt o2)
        {
            return o1.getValue().ult(o2.getValue()) ? -1 :
                    o1.getValue().eq(o2.getValue()) ? 0 : 1;
        }
    };

    // sort the case1 and case2.
    private static final Comparator<Pair<ConstantInt, BasicBlock>> Cmp = new Comparator<Pair<ConstantInt, BasicBlock>>()
    {
        @Override
        public int compare(Pair<ConstantInt, BasicBlock> o1, Pair<ConstantInt, BasicBlock> o2)
        {
            return ConstantIntOrdering.compare(o1.first, o2.first);
        }
    };

    private boolean valueOverlaps(ArrayList<Pair<ConstantInt, BasicBlock>> cases1,
                                    ArrayList<Pair<ConstantInt, BasicBlock>> cases2)
    {
        if (cases1.isEmpty() || cases2.isEmpty()) return false;

        if (cases1.size() > cases2.size())
        {
            ArrayList<Pair<ConstantInt, BasicBlock>> t = cases1;
            cases1 = cases2;
            cases2 = t;
        }

        if (cases1.size() == 1)
        {
            for (Pair<ConstantInt, BasicBlock> itr : cases2)
            {
                if (itr.first.getValue().equals(cases1.get(0).first.getValue()))
                    return true;
            }
        }

        cases1.sort(Cmp);
        cases2.sort(Cmp);

        int i = 0, ei = cases1.size(), j = 0, ej = cases2.size();
        while (i < ei && j < ej)
        {
            Pair<ConstantInt, BasicBlock> itr = cases1.get(i);
            Pair<ConstantInt, BasicBlock> itr2 = cases2.get(j);
            if (itr.first.getValue().equals(itr2.first.getValue()))
                return true;
            else if (itr.first.getValue().ult(itr2.first.getValue()))
                ++i;
            else
                ++j;
        }
        return false;
    }

    private boolean simplifyEqualityComparisonWithOnlyPredecessor(TerminatorInst ti, BasicBlock pred)
    {
        Value predVal = isValueEqualityComparison(pred.getTerminator());
        if (predVal == null) return false;

        Value thisVal = isValueEqualityComparison(ti);
        if (thisVal == null) return false;

        if (!Objects.equals(predVal, thisVal)) return false;

        ArrayList<Pair<ConstantInt, BasicBlock>> predCases = new ArrayList<>();
        BasicBlock predDefaultBB = getValueEqualityComparisonCases(pred.getTerminator(), predCases);
        // remove default block for each cases value.
        eliminateBlockCases(predDefaultBB, predCases);

        ArrayList<Pair<ConstantInt, BasicBlock>> thisCases = new ArrayList<>();
        BasicBlock thisDefaultBB = getValueEqualityComparisonCases(ti, thisCases);
        // remove default block for each cases value.
        eliminateBlockCases(thisDefaultBB, thisCases);

        // If the default block of teriminator of pred is targe to tibb, then we can optimize
        // some code away by taking merit of this knowledge.
        BasicBlock tiBB = ti.getParent();
        if (predDefaultBB == tiBB)
        {
            // If we reach here, we know the value is none of those cases listed in
            // predCases list. If there are any cases in thisCases that are in predCases,
            // we can simplify it.
            if (valueOverlaps(predCases, thisCases))
            {
                if (ti instanceof BranchInst)
                {
                    Util.assertion(thisCases.size() == 1);
                    // We know the program will reach thisDefaultBB.
                    new BranchInst(thisDefaultBB, ti);
                    thisCases.get(0).second.removePredecessor(tiBB);
                    eraseTerminatorInstAndDCECond(ti);
                    return true;
                }
            }
        }
        else
        {
            ConstantInt targetCI = null;
            for (Pair<ConstantInt, BasicBlock> itr : predCases)
            {
                if (itr.second == tiBB)
                {
                    if (targetCI == null)
                        targetCI = itr.first;
                    else
                        // We can't handle this case that multiple value to this block.
                        return false;
                }
            }

            Util.assertion(targetCI != null);
            BasicBlock realDestBB = null;
            for (Pair<ConstantInt, BasicBlock> itr : thisCases)
            {
                if (itr.first.equals(targetCI))
                {
                    realDestBB = itr.second;
                    break;
                }
            }

            if (realDestBB == null)
                realDestBB = thisDefaultBB;

            for (Iterator<BasicBlock> itr = tiBB.succIterator(); itr.hasNext(); )
            {
                BasicBlock succ = itr.next();
                if (succ != realDestBB)
                    succ.removePredecessor(tiBB);
            }

            new BranchInst(realDestBB, ti);
            eraseTerminatorInstAndDCECond(ti);
            return true;
        }
        return false;
    }

    /**
     * If the specified terminator's condition looks like "x == 1" or "x != 1". Checks to see if
     * the predecessor of terminator's basic block have this situation too. If so, we try to fold
     * current block into it's predecessor, if succeed, return true. Otherwise return false.
     * @param ti
     * @return
     */
    private boolean foldValueComparisonIntoPredecessors(TerminatorInst ti)
    {
        Value cond = isValueEqualityComparison(ti);
        if (cond == null) return false;
        BasicBlock tiBB = ti.getParent();
        boolean changed = false;

        for (Iterator<BasicBlock> predItr = tiBB.predIterator(); predItr.hasNext(); )
        {
            BasicBlock pred = predItr.next();

            Value cond2 = isValueEqualityComparison(pred.getTerminator());
            if (!cond.equals(cond2)) return false;

            TerminatorInst predTI = pred.getTerminator();
            // If we going here, it indicates the last terminator's condition is same as ti's
            // so we attempt to optimize this program based on this knowledge.
            if (isSafeToMergeTerminator(ti, predTI))
            {
                ArrayList<Pair<ConstantInt, BasicBlock>> bbCases = new ArrayList<>();
                BasicBlock bbDefault = getValueEqualityComparisonCases(ti, bbCases);

                ArrayList<Pair<ConstantInt, BasicBlock>> predCases = new ArrayList<>();
                BasicBlock predDeffault = getValueEqualityComparisonCases(predTI, predCases);

                ArrayList<BasicBlock> newSuccs = new ArrayList<>();
                if (predDeffault == tiBB)
                {
                    TreeSet<ConstantInt> handled = new TreeSet<>(ConstantIntOrdering);
                    for (int i = 0, e = predCases.size(); i < e; i++)
                    {
                        if (predCases.get(i).second != tiBB)
                            handled.add(predCases.get(i).first);
                        else
                        {
                            predCases.remove(i);
                            --i;
                            --e;
                        }
                    }

                    if (predDeffault != bbDefault)
                    {
                        predDeffault.removePredecessor(pred);
                        predDeffault = bbDefault;
                        newSuccs.add(bbDefault);
                    }
                    for (Pair<ConstantInt, BasicBlock> itr : bbCases)
                    {
                        if (!handled.contains(itr.first) &&
                                itr.second != bbDefault)
                        {
                            predCases.add(itr);
                            newSuccs.add(itr.second);
                        }
                    }
                }
                else
                {
                    // if this is not the default destination from predTI, only the
                    // edges in si that ocurrs in predTI with a destination of bb would
                    // be activated.
                    TreeSet<ConstantInt> handled = new TreeSet<>(ConstantIntOrdering);
                    for (int i = 0, e = predCases.size(); i < e; i++)
                    {
                        if (predCases.get(i).second == tiBB)
                        {
                            handled.add(predCases.get(i).first);
                            predCases.remove(i);
                            --i;
                            --e;
                        }
                    }

                    for (Pair<ConstantInt, BasicBlock> itr : bbCases)
                    {
                        if (handled.contains(itr.first))
                        {
                            predCases.add(itr);
                            newSuccs.add(itr.second);
                            handled.remove(itr.first);
                        }
                    }

                    handled.forEach(ci->
                    {
                        predCases.add(Pair.get(ci, bbDefault));
                        newSuccs.add(bbDefault);
                    });
                }

                // Okay, at this point, we know which new successor Pred will get.  Make
                // sure we update the number of entries in the PHI nodes for these
                // successors.
                newSuccs.forEach(succ->
                {
                    addPredecessorToBlock(succ, pred, tiBB);
                });

                SwitchInst si = new SwitchInst(cond, predDeffault, predCases.size(), "", predTI);
                predCases.forEach(itr->
                {
                    si.addCase(itr.first, itr.second);
                });

                eraseTerminatorInstAndDCECond(predTI);

                BasicBlock infLoopBlock = null;
                for (int i = 0, e = si.getNumOfSuccessors(); i < e; i++)
                {
                    if (si.getSuccessor(i) == tiBB)
                    {
                        if (infLoopBlock == null)
                        {
                            infLoopBlock = BasicBlock.createBasicBlock("infloop", tiBB.getParent());
                            new BranchInst(infLoopBlock, infLoopBlock);
                        }
                        si.setSuccessor(i, infLoopBlock);
                    }
                }
                changed = true;
            }
        }
        return changed;
    }

    private void addPredecessorToBlock(BasicBlock succ, BasicBlock pred, BasicBlock bb)
    {
        if (!(succ.getFirstInst() instanceof PhiNode)) return;

        PhiNode pn;
        for (int i = 0, e = succ.size(); i < e && succ.getInstAt(i) instanceof PhiNode; i++)
        {
            pn = (PhiNode) succ.getInstAt(i);
            pn.addIncoming(pn.getIncomingValueForBlock(bb), pred);
        }
    }

    private boolean isSafeToMergeTerminator(TerminatorInst ti, TerminatorInst predTI)
    {
        if (ti == predTI) return false;

        BasicBlock bb = ti.getParent();
        BasicBlock pred = predTI.getParent();

        HashSet<BasicBlock> bbSuccs = new HashSet<>();
        for (Iterator<BasicBlock> itr = bb.succIterator(); itr.hasNext(); )
            bbSuccs.add(itr.next());

        for (Iterator<BasicBlock> itr = pred.succIterator(); itr.hasNext(); )
        {
            BasicBlock commonSucc = itr.next();
            if (bbSuccs.contains(commonSucc))
            {
                int i = 0, e = commonSucc.size();
                while (i < e && commonSucc.getInstAt(i) instanceof PhiNode)
                {
                    PhiNode pn = (PhiNode) commonSucc.getInstAt(i);
                    if (!pn.getIncomingValueForBlock(bb).
                            equals(pn.getIncomingValueForBlock(pred)))
                        return false;
                    ++i;
                }
            }
        }
        return true;
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

        changed |= constantFoldTerminator(bb);

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
        {
            if (foldOnlyReturnInBlock(bb))
                return true;
        }
        else if (bb.getTerminator() instanceof SwitchInst)
        {
            SwitchInst si = (SwitchInst) bb.getTerminator();
            if (isValueEqualityComparison(bb.getTerminator()) != null)
            {
                BasicBlock singlePred = bb.getSinglePredecessor();
                if (singlePred != null && simplifyEqualityComparisonWithOnlyPredecessor(si, singlePred))
                {
                    simplifyCFG(bb);
                    return true;
                }
            }
            // if the switch inst is first one, checks to see if we can fold the block
            // away into any preds.
            if (si == bb.getFirstInst() && foldValueComparisonIntoPredecessors(si))
            {
                simplifyCFG(bb);
                return true;
            }
        }
        else if (bb.getTerminator() instanceof BranchInst)
        {
            if (foldBranchInstAsTermInst(bb))
                return true;
        }
        else if (bb.getTerminator() instanceof UnreachableInst)
        {}

        // Merge basic blocks into their predecessor if there is only one distinct
        // pred, and if there is only one distinct successor of the predecessor, and
        // if there are no PHI nodes.
        //
        if (mergeBlockToPredecessor(bb))
            return true;

        // Otherwise, if this block only has a single predecessor, and if that block
        // is a conditional branch, see if we can hoist any code from this block up
        // into our predecessor.
        BasicBlock onlyPredBB = null;
        for (int i = 0, e = bb.getNumPredecessors(); i < e; i++)
        {
            BasicBlock pred = bb.predAt(i);
            if (onlyPredBB == null)
                onlyPredBB = pred;
            else if (onlyPredBB != pred)
            {
                onlyPredBB = null;
                break;
            }
        }
        if (onlyPredBB != null)
        {
            if (onlyPredBB.getTerminator() instanceof BranchInst)
            {
                BranchInst br = (BranchInst) onlyPredBB.getTerminator();
                if (br.isConditional())
                {
                    // obtain the other block.
                    BasicBlock otherBB = br.getSuccessor(br.getSuccessor(0) == bb ? 1:0);
                    //   pred
                    //  |    |
                    //  v    v
                    //  bb  otherBB
                    if (otherBB.getNumPredecessors() == 1)
                    {
                        // // We have a conditional branch to two blocks that are only reachable
                        // from the condbr.  We know that the condbr dominates the two blocks,
                        // so see if there is any identical code in the "then" and "else"
                        // blocks.  If so, we can hoist it up to the branching block.
                        changed |= hoistThenElseCodeToIf(br);
                    }
                    else
                    {
                        BasicBlock onlySucc = null;
                        for (Iterator<BasicBlock> itr = bb.succIterator(); itr.hasNext(); )
                        {
                            BasicBlock succ = itr.next();
                            if (onlySucc == null)
                                onlySucc = succ;
                            else if (onlySucc != succ)
                            {
                                onlySucc = null;
                                break;
                            }
                        }

                        if (onlySucc == otherBB)
                        {
                            //   pred
                            //  |    |
                            //  v    |
                            //  bb   |
                            //   \   |
                            //  otherBB(onlySucc).
                            // Checks see if we can hoist any code from this block up
                            // to the "if" block
                            changed |= speculativelyExecuteBB(br, bb);
                        }
                    }
                }
            }
        }

        for (Iterator<BasicBlock> itr = bb.predIterator(); itr.hasNext(); )
        {
            BasicBlock pred = itr.next();
            TerminatorInst ti = pred.getTerminator();
            if (ti != null && ti instanceof BranchInst)
            {
                // Change br (X == 0 | X == 1), T, F into a switch instruction.
                BranchInst br = (BranchInst) ti;
                Value val;
                if (br.isConditional() && (val = br.getCondition()) != null &&
                        val instanceof Instruction)
                {
                    Instruction cond = (Instruction) val;
                    OutParamWrapper<Value> x = new OutParamWrapper<>(null);
                    ArrayList<ConstantInt> values = new ArrayList<>();
                    boolean trueWhenEqual = gatherValueComparisons(cond, x, values);
                    Value compVal = x.get();
                    if (compVal != null && compVal.getType().isInteger())
                    {
                        Util.unique(values);
                        values.sort(ConstantIntOrdering);
                        BasicBlock defaultBB = br.getSuccessor(1);
                        BasicBlock edgeBB = br.getSuccessor(0);
                        if (!trueWhenEqual)
                        {
                            BasicBlock t = defaultBB;
                            defaultBB = edgeBB;
                            edgeBB = t;
                        }

                        SwitchInst si = new SwitchInst(compVal, defaultBB, values.size(), "", br);
                        // add all 'case' to the switch instruction.
                        for (ConstantInt value : values)
                        {
                            si.addCase(value, edgeBB);
                        }

                        for (int i = 0, e = edgeBB.size(); i < e && edgeBB.getInstAt(i) instanceof PhiNode; i++)
                        {
                            pn = (PhiNode) edgeBB.getInstAt(i);
                            Value inVal = pn.getIncomingValueForBlock(pred);
                            for (int j = 0, sz = values.size()-1; j < sz; j++)
                                pn.addIncoming(inVal, pred);
                        }
                        eraseTerminatorInstAndDCECond(br);
                        return true;
                    }
                }
            }
        }
        return changed;
    }

    private boolean speculativelyExecuteBB(BranchInst br, BasicBlock bb)
    {
        // TODO: 18-7-20
        return false;
    }

    private boolean gatherValueComparisons(Instruction cond, OutParamWrapper<Value> compVal,
            ArrayList<ConstantInt> values)
    {
        switch (cond.getOpcode())
        {
            case Or:
                compVal.set(gatherConstantSetEQs(cond, values));
                return true;
            case And:
                compVal.set(gatherConstantSetNEs(cond, values));
                return false;
        }
        return false;
    }

    private Value gatherConstantSetEQs(Value cond, ArrayList<ConstantInt> values)
    {
        if (!(cond instanceof Instruction)) return null;
        Instruction inst = (Instruction)cond;
        if (inst.getOpcode() == Operator.ICmp &&
                ((ICmpInst)inst).getPredicate() == Predicate.ICMP_EQ)
        {
            if (inst.operand(1) instanceof ConstantInt)
            {
                values.add((ConstantInt) inst.operand(1));
                return inst.operand(0);
            }
            else if (inst.operand(0) instanceof ConstantInt)
            {
                values.add((ConstantInt) inst.operand(0));
                return inst.operand(1);
            }
        }
        else if (inst.getOpcode() == Operator.Or)
        {
            Value lhs = gatherConstantSetEQs(inst.operand(0), values);
            Value rhs = gatherConstantSetEQs(inst.operand(1), values);
            if (lhs != null && rhs != null && lhs.equals(rhs))
                return lhs;
        }
        return null;
    }

    private Value gatherConstantSetNEs(Value cond, ArrayList<ConstantInt> values)
    {
        if (!(cond instanceof Instruction)) return null;
        Instruction inst = (Instruction)cond;
        if (inst.getOpcode() == Operator.ICmp &&
                ((ICmpInst)inst).getPredicate() == Predicate.ICMP_NE)
        {
            if (inst.operand(1) instanceof ConstantInt)
            {
                values.add((ConstantInt) inst.operand(1));
                return inst.operand(0);
            }
            else if (inst.operand(0) instanceof ConstantInt)
            {
                values.add((ConstantInt) inst.operand(0));
                return inst.operand(1);
            }
        }
        else if (inst.getOpcode() == Operator.And)
        {
            Value lhs = gatherConstantSetNEs(inst.operand(0), values);
            Value rhs = gatherConstantSetNEs(inst.operand(1), values);
            if (lhs != null && rhs != null && lhs.equals(rhs))
                return lhs;
        }
        return null;
    }

    private boolean hoistThenElseCodeToIf(BranchInst br)
    {
        BasicBlock bb1 = br.getSuccessor(0);
        BasicBlock bb2 = br.getSuccessor(1);
        int i = 0, e1 = bb1.size(), j = 0, e2 = bb2.size();
        BasicBlock parentBB = br.getParent();

        if (bb1.getInstAt(i).getOpcode() == Operator.Phi ||
                bb1.getInstAt(i).getOpcode() != bb2.getInstAt(i).getOpcode() ||
                !bb1.getInstAt(i).isIdenticalToWhenDefined(bb2.getInstAt(j)))
            return false;

        TerminatorInst ti = parentBB.getTerminator();
        // As we going here, we know at least one instruction could be hoisted to parentBB.
        HoistTerminator:
        {
            do
            {
                if (bb1.getInstAt(i) instanceof TerminatorInst)
                    break HoistTerminator;

                parentBB.insertBefore(bb1.getInstAt(i), ti);
                bb2.getInstAt(j).replaceAllUsesWith(bb1.getInstAt(i));
                bb1.getInstAt(i).intersectOptionalDataWith(bb2.getInstAt(j));
                bb2.getInstAt(j).eraseFromParent();

            } while (i < e1 && j < e2 && bb1.getInstAt(i).
                    isIdenticalToWhenDefined(bb2.getInstAt(j)));
            return true;
        }

        Instruction inst = bb1.getInstAt(i).clone();
        parentBB.insertBefore(inst, br);
        if (inst.getType() != LLVMContext.VoidTy)
        {
            bb1.getInstAt(i).replaceAllUsesWith(inst);
            bb2.getInstAt(j).replaceAllUsesWith(inst);
            inst.setName(bb1.getInstAt(i).getName());
        }

        // Hoisting one of the terminators from our successor is a great thing.
        // Unfortunately, the successors of the if/else blocks may have PHI nodes in
        // them.  If they do, all PHI entries for BB1/BB2 must agree for all PHI
        // nodes, so we insert select instruction to compute the final result.
        HashMap<Pair<Value, Value>, SelectInst> insertedSelects = new HashMap<>();
        for (Iterator<BasicBlock> itr = bb1.succIterator(); itr.hasNext(); )
        {
            BasicBlock succ = itr.next();
            for (int k = 0, sz = succ.size(); k < sz && succ.getInstAt(k) instanceof PhiNode; ++k)
            {
                PhiNode pn = (PhiNode) succ.getInstAt(k);
                Value bb1V = pn.getIncomingValueForBlock(bb1);
                Value bb2V = pn.getIncomingValueForBlock(bb2);
                if (!bb1V.equals(bb2V))
                {
                    Pair<Value, Value> key = Pair.get(bb1V, bb2V);
                    SelectInst si;
                    if (!insertedSelects.containsKey(key))
                    {
                        si = new SelectInst(br.getCondition(), bb1V, bb2V,
                                bb1V.getName()+"."+bb2V.getName(), inst);
                        insertedSelects.put(key, si);
                    }
                    else
                        si = insertedSelects.get(key);
                    // Make the PHI node use the select for all incoming values for BB1/BB2
                    for (int n = 0, sz2 = pn.getNumberIncomingValues(); n < sz2; ++n)
                    {
                        if (pn.getIncomingBlock(n) == bb1 ||
                                pn.getIncomingBlock(n) == bb2)
                            pn.setIncomingValue(n, si);

                    }
                }
            }
        }

        for (Iterator<BasicBlock> itr = bb1.succIterator(); itr.hasNext(); )
        {
            addPredecessorToBlock(itr.next(), parentBB, bb1);
        }
        eraseTerminatorInstAndDCECond(br);
        return true;
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
