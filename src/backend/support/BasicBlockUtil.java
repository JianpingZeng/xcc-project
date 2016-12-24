package backend.support;
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

import backend.analysis.DomTreeInfo;
import backend.hir.BasicBlock;
import backend.opt.DominatorFrontier;
import backend.pass.Pass;
import backend.value.Instruction;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.PhiNode;
import backend.value.Value;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class BasicBlockUtil
{
    /**
     * This method transforms bb by introducing a new basic block into the function,
     * and moving some of the predecessors of bb to be predecessors of the new block.
     * The new predecessors are indicated by preds list. The new block is given a
     * suffix of 'suffix'.
     *
     * @param bb
     * @param preds
     * @param suffix
     * @param pass
     * @return
     */
    public static BasicBlock splitBlockPredecessors(BasicBlock bb,
            ArrayList<BasicBlock> preds, String suffix,
            Pass pass)
    {
        // create a new basic block, insert it right before the original block.
        BasicBlock newBB = BasicBlock.createBasicBlock(-1, bb.getName()+suffix, bb.getParent());

        // the new block have a unconditional branch to the origin block.
        BranchInst bi = new BranchInst(bb, newBB);

        // Move the edges from Preds to point to NewBB instead of BB.
        preds.forEach(pred ->
        {
            pred.getTerminator().replaceUsesOfWith(bb, newBB);
        });

        // update dominator tree and dominator frontier info.
        DomTreeInfo dt = pass != null ? pass.getAnalysisToUpDate(DomTreeInfo.class): null;
        DominatorFrontier df = pass != null ? pass.getAnalysisToUpDate(DominatorFrontier.class): null;
        if (dt != null)
            dt.splitBlock(newBB);
        if (df != null)
            df.splitBlock(newBB);

        // Insert a new PHI node into newBB for every PHI node in bb and that new PHI
        // node becomes an incoming value for bb's phi node.  However, if the preds
        // list is empty, we need to insert dummy entries into the PHI nodes in bb to
        // account for the newly created predecessor.
        if (preds.isEmpty())
        {
            // insert dummy values as the incoming value.
            for (Instruction inst : bb)
            {
                if (inst instanceof PhiNode)
                {
                    ((PhiNode)inst).addIncoming(Value.UndefValue.get(inst.getType()), newBB);
                }
                break;
            }
            return newBB;
        }

        // Otherwise, create a new PHI node in newBB for each PHI node in bb.
        for (Instruction inst : bb)
        {
            if(!(inst instanceof PhiNode))break;

            PhiNode pn = (PhiNode)inst;

            // check to see if all of the values coming in are the same.
            // If so, we don't need to create a new PHI node.
            Value inVal = pn.getIncomingBlock(0);
            for (int i = 1; i < preds.size(); i++)
            {
                if (inVal != pn.getIncomingValue(i))
                {
                    inVal = null;
                    break;
                }
            }
            if (inVal != null)
            {
                // If all incoming values for the ph are the same, just don't
                // make a new PHI. Instead, just removes the value from old
                // PHI node.
                for (int i = 0, e = preds.size(); i < e; i++)
                {
                    pn.removeIncomingValue(i, false);
                }
            }
            else
            {
                // If the values coming into the block are not the same, we need a PHI.
                // Create the new PHI node, insert it into NewBB at the end of the block
                PhiNode newPHI = new PhiNode(pn.getType(),
                        pn.getNumberIncomingValues(),
                        pn.getName()+".ph", bi);

                // Move all of the PHI values for 'preds' to the new PHI.
                for (int i = 0, e = preds.size(); i < e; i++)
                {
                    Value val = pn.removeIncomingValue(i, false);
                    newPHI.addIncoming(val, preds.get(i));
                }
                inVal = newPHI;
            }

            // Add an incoming value for the new created preheader block of
            // loop.
            pn.addIncoming(inVal, newBB);

            // check to see if we can eliminate this phi node.
            Value v = null;
            if ((v = pn.hasConstantValue()) != null)
            {
                if (!(v instanceof Instruction) ||
                        dt == null
                        || dt.dominates((Instruction)v, pn))
                {
                    pn.replaceAllUsesWith(v);
                    pn.eraseFromBasicBlock();
                }
            }
        }
        return newBB;
    }
}
