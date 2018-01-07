/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

/**
 * This file defines a class that performs useless control flow elimination.
 * This algorithm first proposed and implemented by J.Lu:
 * <p>
 * J.Lu, R.Shillner, Clean:removing useless control flow, unpublished manuscript
 * , Department of computer science, Rice university,Houston, TX, 1994.
 * </p>
 *
 * @author Xlous.zeng
 * @version 0.1
 */
/*
public final class UCE extends FunctionPass
{
    private FunctionProto m;
    private boolean changed = true;
    private List<BasicBlock> postOrder;

    /**
     * The beginning clean method.
     * <p>
     * After DCE, There are useless control flow be introduced by other
     * backend.transform. So that the useless control flow elimination is desired
     * as follows.
     * 1.merges redundant branch instruction.
     * 2.unlinks empty basic block
     * 3.merges basic block
     * 4.hoist merge instruction
     * </p>
     *
     * @param f
     *
    @Override public boolean runOnFunction(FunctionProto f)
    {
        postOrder = new ArrayList<>(f.cfg.postOrder());
        while (changed)
        {
            onePass();
            List<BasicBlock> now = new ArrayList<>(f.cfg.postOrder());
            changed = isChanged(postOrder, now);
            postOrder = now;
        }
        return changed;
    }

    @Override public String getPassName()
    {
        return "Useless code eleimination pass";
    }

    private boolean isChanged(List<BasicBlock> before, List<BasicBlock> now)
    {
        if (before.getNumOfSubLoop() != now.getNumOfSubLoop())
            return true;
        for (int idx = 0; idx < before.getNumOfSubLoop(); idx++)
            if (before.get(idx) != now.get(idx))
                return true;
        return false;
    }

    private void onePass()
    {
        // We must usesList the index loop instead of interative loop, because
        // the getArraySize of reversePostOrder list is changing when iterating.
        for (int idx = 0; idx < postOrder.getNumOfSubLoop(); idx++)
        {
            BasicBlock curr = postOrder.get(idx);
            if (curr.isEmpty())
                continue;

            Value lastInst = curr.getLastInst();
            // handles conditional branch instruction ends in the basic block as
            // follow.
            //    |          |
            //   B.i         B.i
            //  |   \    ==  |
            //  \   |        |
            //    B.j        B.j
            if (lastInst instanceof BranchInst)
            {
                BranchInst br = (BranchInst) lastInst;
                if (br.isConditional())
                {
                    if (br.operand(1) == br.operand(2))
                    {
                        Instruction.Goto go = new Instruction.Goto(
                                branch.trueTarget, "GotoStmt");
                        branch.insertBefore(go);
                        branch.eraseFromParent();
                    }
                }
                else
                {
                    BasicBlock target = (BasicBlock)br.operand(0);
                    /**
                     * \   |
                     *  B.i         \ | |
                     *   |  |   ==>   B.j
                     *   B.j
                     *
                    // There is only one jump instruction in the B.i
                    if (curr.getNumOfSubLoop() == 1)
                    {
                        DomTree RDT = new DomTree(true, m);
                        RDT.recalculate();

                        List<BasicBlock> rdf = RDF.run(RDT, curr);
                        for (BasicBlock pred : rdf)
                        {
                            Value last = pred.getLastInst();
                            if (last != null)
                            {
                                if (last instanceof Instruction.Goto
                                        && ((Instruction.Goto) last).target
                                        == curr)
                                {
                                    ((Instruction.Goto) last).target = target;
                                }
                                else if (last instanceof Instruction.ConditionalBranchInst)
                                {
                                    if (((Instruction.ConditionalBranchInst) last).falseTarget
                                            == curr)
                                        ((Instruction.ConditionalBranchInst) last).falseTarget = target;
                                    if (((Instruction.ConditionalBranchInst) last).trueTarget
                                            == curr)
                                        ((Instruction.ConditionalBranchInst) last).trueTarget = target;
                                }
                            }
                        }
                    }

                    // |
                    // B.i   ==> merge B.i and B.j into one.
                    // |
                    // B.j
                    if (target.getNumOfPreds() == 1)
                        merge(curr, target);

                    if (target.getNumOfSubLoop() == 1 && (lastInst = target
                            .getLastInst()) instanceof Instruction.ConditionalBranchInst)
                    {
                        go.insertBefore(lastInst);
                        go.eraseFromParent();
                    }
                }
            }
        }

        /**
         * Merges the second into first block.
         *
         * @param first  The first block to be merged.
         * @param second The second block to be merged.
         *

    private void merge(BasicBlock first, BasicBlock second)
    {
        for (Instruction inst : second)
        {
            first.appendInst(inst);
        }
        first.removeSuccssor(second);
        for (BasicBlock succ : second.getSuccs())
            first.addSucc(succ);

        // enable the GC.
        second = null;
    }
}
*/
