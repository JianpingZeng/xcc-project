package backend.transform.scalars;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import backend.value.BasicBlock;
import backend.utils.PredIterator;
import backend.utils.SuccIterator;
import backend.pass.FunctionPass;
import backend.value.Constant;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.TerminatorInst;
import backend.value.Value;

import java.util.*;

import static backend.transform.scalars.ConstantFolder.constantFoldTerminator;

/**
 * This pass defined here for removing the unreachable basic block resides inside
 * FunctionProto.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CFGSimplifyPass implements FunctionPass
{
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
        HashSet<BasicBlock> reachables = new HashSet<>();
        boolean changed = markActiveBlocks(f.getEntryBlock(), reachables);

        // If there are unreachable blocks in current cfg.
        if (reachables.size() != f.getBasicBlockList().size())
        {
            assert reachables.size() < f.getBasicBlockList().size();

            // Loop over all unreachable blocks, and drop off all internal reference.
            for (int i = 0; i < f.getBasicBlockList().size(); i++)
            {
                BasicBlock bb = f.getBasicBlockList().get(i);
                if (!reachables.contains(bb))
                {
                    for (SuccIterator itr = bb.succIterator();itr.hasNext();)
                    {
                        BasicBlock succ = itr.next();
                        if (reachables.contains(succ))
                            succ.removePredecessor(succ);
                    }
                    bb.dropAllReferences();
                }
            }

            for (int i = 1; i < f.getBasicBlockList().size();)
            {
                BasicBlock bb = f.getBasicBlockList().get(i);
                if (!reachables.contains(bb))
                    f.getBasicBlockList().remove(i);
                else
                    i++;
            }
            changed = true;
        }
        boolean localChanged = true;
        while(localChanged)
        {
            localChanged = false;
            // Loop over all of the basic blocks (except the first one) and remove them
            // if they are unneeded.
            for (int i = 1; i < f.getBasicBlockList().size(); i++)
            {
                if (simplifyCFG(f.getBasicBlockList().get(i)))
                {
                    localChanged = false;
                }
            }
            changed |= localChanged;
        }
        return changed;
    }

    private boolean markActiveBlocks(BasicBlock entry,
            HashSet<BasicBlock> reachables)
    {
        if (reachables.contains(entry)) return false;
        reachables.add(entry);

        boolean changed = false;
        changed |= constantFoldTerminator(entry);
        // uses a queue for simulating recursively calling this function for efficiency
        // with standard pre-order method.
        Stack<BasicBlock> worklist = new Stack<>();
        for (SuccIterator sItr = entry.succIterator(); sItr.hasNext();)
            worklist.push(sItr.next());

        while (!worklist.isEmpty())
        {
            BasicBlock currBB = worklist.pop();
            if (!reachables.contains(currBB))
                reachables.add(currBB);

            changed |= constantFoldTerminator(currBB);

            // add all successors block of currBB into worklist.
            for (SuccIterator sItr = currBB.succIterator(); sItr.hasNext();)
                worklist.push(sItr.next());
        }
        return changed;
    }

    /**
     * This function used for do a simplification of a CFG. For example, it
     * adjusts branches to branches to eliminate the extra hop. It eliminates
     * unreachable basic blocks ,and does other "peephole" optimization of the
     * CFG.
     * @param bb
     * @return Returns true if a modification was made.
     */
    private boolean simplifyCFG(BasicBlock bb)
    {
        boolean changed = false;
        Function f = bb.getParent();

        assert bb != null && f != null:"Basic block not embedded in function";
        assert bb.getTerminator() != null:"degnerate basic block is bing simplified";
        assert bb != f.getBasicBlockList().getFirst():"Can not simplify entry block";

        // Removes the basic block which has no predecessors.
        PredIterator<BasicBlock> predItr = bb.predIterator();
        if (!predItr.hasNext() && !bb.hasConstantReference())
        {
            for (SuccIterator succItr = bb.succIterator(); succItr.hasNext();)
            {
                succItr.next().removePredecessor(bb);
            }

            while (!bb.isEmpty())
            {
                Instruction i = bb.getInstList().getLast();
                // If this instruction is used, replaces the uses with
                // arbitrary constant value. Since the control flow can
                // not reach here, we don't care what will be replaced with.
                // And all values contained it must dominate their uses,
                // that all uses will be removed eventually.
                if (!i.isUseEmpty())
                    i.replaceAllUsesWith(Constant.getNullValue(i.getType()));;

                // remove the instruction from basic block contains it.
                bb.getInstList().removeLast();
            }

            f.getBasicBlockList().remove(bb);
            return true;
        }

        // Check to see if we can constant propagate this terminator instruction
        changed |= constantFoldTerminator(bb);

        // Check to see if this block has no non-phi instructions and only a single
        // successor.  If so, replace references to this basic block with references
        // to the successor.
        SuccIterator sucItr = bb.succIterator();
        if (bb.getNumSuccessors() == 1)
        {
            // There is just one successor.
            int i = 0;
            // skips all PhiNode ahead of other instructions.
            while (bb.getInstAt(i) instanceof PhiNode) i++;

            if (bb.getInstAt(i) instanceof TerminatorInst)
            {
                // Terminator instruction is the only one non-phi instruction.
                BasicBlock succ = sucItr.next();

                if (succ != bb)
                {
                    // bb is not in infinite loop!!!
                    if (!propagatePredecessorForPHIs(bb, succ))
                    {
                        // If our successor has PHI nodes, then we need to update them to
                        // include entries for BB's predecessors, not for BB itself.
                        // Be careful though, if this transformation fails (returns true) then
                        // we cannot do this transformation!

                        String oldName = bb.getName();
                        ArrayList<BasicBlock> oldSuccPreds = new ArrayList<>();
                        for (PredIterator itr = succ.predIterator(); predItr.hasNext();)
                            oldSuccPreds.add(itr.next());

                        // Move all PHI nodes in bb to succ if they are alive,
                        // otherwise delete them.
                        for (int j = 0; j< bb.getInstList().size();)
                        {
                            if (!(bb.getInstAt(j) instanceof PhiNode))
                                break;
                            PhiNode pn = (PhiNode)bb.getInstAt(i);
                            if (pn.isUseEmpty()) // remove the useless phi node.
                                bb.getInstList().remove(i);
                            else
                            {
                                // The instruction is alive, so this means that Succ must have
                                // *ONLY* had BB as a predecessor, and the PHI node is still valid
                                // now.  Simply move it into Succ, because we know that BB
                                // strictly dominated Succ.
                                bb.getInstList().remove(i);
                                succ.getInstList().addFirst(pn);

                                //
                                for (int k = 0, e = oldSuccPreds.size(); k < e; k++)
                                {
                                    if (oldSuccPreds.get(i) != bb)
                                        pn.addIncoming(pn, oldSuccPreds.get(i));
                                }
                            }
                            // everything that jumped to bb now goes to succ!!!
                            bb.replaceAllUsesWith(succ);
                            f.getBasicBlockList().remove(bb);

                            if (!oldName.isEmpty() && !succ.hasName())
                                succ.setName(oldName);

                            return true;
                        }
                    }
                }
            }
        }

        // Merge basic blocks into their predecessor if there is only one distinct
        // pred, and if there is only one distinct successor of the predecessor, and
        // if there are no PHI nodes.
        if (!bb.hasConstantReference())
        {
            PredIterator pred_Itr = bb.predIterator();
            BasicBlock onlyPred = pred_Itr.next();
            while(pred_Itr.hasNext())
            {
                if (pred_Itr.next() != onlyPred)
                {
                    onlyPred = null;
                    break;
                }
            }

            BasicBlock onlySucc = null;
            if (onlyPred != null && onlyPred != bb)
            {
                // Check to see if there is only one distinct successor.
                SuccIterator succ_Itr = bb.succIterator();
                onlySucc = succ_Itr.next();
                while (succ_Itr.hasNext())
                {
                    if (onlySucc != succ_Itr.next())
                    {
                        onlySucc = null;
                        break;
                    }
                }

                if (onlySucc != null)
                {
                    TerminatorInst term = onlyPred.getTerminator();

                    // Resolve any PHI nodes at the start of the block.  They are all
                    // guaranteed to have exactly one entry if they exist
                    for (int i = 0; i< bb.getInstList().size(); i++)
                    {
                        if (!(bb.getInstAt(i) instanceof PhiNode))
                            break;
                        PhiNode pn = (PhiNode)bb.getInstAt(i);
                        pn.replaceAllUsesWith(pn.getIncomingValue(0));;
                        bb.getInstList().remove(i);
                    }

                    // Delete the unconditional branch from predecessor.
                    onlyPred.getInstList().removeLast();

                    // Move all definition in the successor to the predecessor.
                    onlyPred.getInstList().addAll(bb.getInstList());

                    bb.replaceAllUsesWith(onlyPred);

                    f.getBasicBlockList().remove(bb);
                    String oldName = bb.getName();
                    if (!oldName.isEmpty() && !onlyPred.hasName())
                        onlyPred.setName(oldName);

                    return true;
                }
            }
        }
        return changed;
    }

    /**
     * Assumption: Succ is the single successor for BB when this method is called.
     * and bb is the one to be removed.
     *
     * This is a little tricky because "Succ" has PHI nodes, which need to
     * have extra slots added to them to hold the merge edges from BB's
     * predecessors, and BB itself might have had PHI nodes in it.  This function
     * returns true (failure) if the Succ BB already has a predecessor that is a
     * predecessor of BB and incoming PHI arguments would not be discernible.
     * @param bb
     * @param succ
     * @return
     */
    private boolean propagatePredecessorForPHIs(BasicBlock bb, BasicBlock succ)
    {
        assert  (bb.succIterator().next() == succ):"Succ is not successor of bb!";

        // There is no phi node in bb.
        if (!(succ.getInstAt(0) instanceof PhiNode))
            return false;

        // If there is more than one predecessor, and there are PHI nodes in
        // the successor, then we need to add incoming edges for the PHI nodes
        ArrayList<BasicBlock> predList = new ArrayList<>();
        for (PredIterator itr = bb.predIterator(); itr.hasNext();)
        {
            predList.add(itr.next());
        }

        // Check to see if one of the predecessors of BB is already a predecessor of
        // Succ.  If so, we cannot do the transformation if there are any PHI nodes
        // with incompatible values coming in from the two edges!
        for (PredIterator predItr = succ.predIterator(); predItr.hasNext(); )
        {
            BasicBlock pred = predItr.next();
            if (predList.contains(pred))
            {
                // Loop over all of the PHI nodes checking to see if there are
                // incompatible values coming in.
                for (int i = 0, e = succ.getNumOfInsts(); i < e; i++)
                {
                    if (!(succ.getInstAt(i) instanceof PhiNode))
                        break;

                    PhiNode pn = (PhiNode)succ.getInstAt(i);;
                    int idx1 = pn.getBasicBlockIndex(bb);
                    int idx2 = pn.getBasicBlockIndex(pred);
                    assert idx1 != -1 && idx2 != -1
                            :"Didn't have entries for predecesssor!";

                    if (pn.getIncomingValue(idx1) != pn.getIncomingValue(idx2))
                        return true;
                }
            }
        }

        // Loop over all of the PHI nodes in the successor BB
        for (int i = 0, e = succ.getNumOfInsts(); i < e; i++)
        {
            if (!(succ.getInstAt(i) instanceof PhiNode))
                break;

            PhiNode pn = (PhiNode)succ.getInstAt(i);
            Value oldVal = pn.removeIncomingValue(bb, false);
            assert oldVal != null:"No enry in PHI for Pred bb!";

            // If this incoming value is one of the PHI nodes in BB...
            if (oldVal instanceof PhiNode && ((PhiNode)oldVal).getParent() == bb)
            {
                PhiNode oldValPn = (PhiNode)oldVal;
                for (BasicBlock pred : predList)
                {
                    pn.addIncoming(oldValPn.getIncomingValueForBlock(pred), pred);
                }
            }
            else
            {
                for (BasicBlock pred : predList)
                {
                    pn.addIncoming(oldVal, pred);
                }
            }
        }
        return false;
    }

    @Override
    public String getPassName()
    {
        return "Simplify CFG pass";
    }
}
