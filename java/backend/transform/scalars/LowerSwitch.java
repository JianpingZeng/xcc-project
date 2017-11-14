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

import backend.pass.FunctionPass;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Instruction.CmpInst.Predicate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This file defines a class behaves replacing all switch into chained
 * branch instruction.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LowerSwitch implements FunctionPass
{
    public static LowerSwitch createLowerSwitchPass() {return new LowerSwitch();}

    @Override
    public String getPassName()
    {
        return "X86 Lower switch pass";
    }

    private static class CaseRange
    {
        Constant low, high;
        BasicBlock bb;

        CaseRange(Constant l, Constant h, BasicBlock block)
        {
            low = l;
            high = h;
            bb = block;
        }
    }

    /**
     * The comparison function for sorting the switch case values in the
     * vector.
     */
    private static Comparator<CaseRange> CaseCmp
            = (c1, c2) ->
    {
        ConstantInt ci1 = (ConstantInt)c1.low;
        ConstantInt ci2 = (ConstantInt)c2.high;
        return ci1.getValue().slt(ci2.getValue()) ? -1
                : ci1.getValue().eq(ci2.getValue()) ? 0
                : -1;
    };

    /**
     * To run this pass on a module, we simply call runOnFunction once for
     * each module.
     *
     * @param f
     * @return
     */
    @Override
    public boolean runOnFunction(Function f)
    {
        boolean changed = false;
        for (BasicBlock bb : f.getBasicBlockList())
        {
            TerminatorInst ti = bb.getTerminator();
            if (ti !=null && ti instanceof SwitchInst)
            {
                changed = true;
                processSwitchInst((SwitchInst)ti);
            }
        }
        return changed;
    }

    /**
     * Replaces the switch instruction with a sequence of branch instruction in
     * a balanced binary search tree.
     * @param swInst
     */
    private void processSwitchInst(SwitchInst swInst)
    {
        BasicBlock curBB = swInst.getParent();
        BasicBlock originBB = curBB;
        Function f = curBB.getParent();
        // gets the default value.
        Value val = swInst.operand(0);
        BasicBlock defaultBB = swInst.getDefaultBlock();

        // If there is only the default destination, don't bother with the code
        // below.
        if (swInst.getNumOfOperands() == 2)
        {
            BranchInst inst = new BranchInst(defaultBB, curBB);
            curBB.getInstList().remove(swInst);
            return;
        }

        // create a new,empty default block so that the new hierachy of if-then
        // statements go to this and phi node are happy.
        BasicBlock newDefaultBB = BasicBlock.createBasicBlock("newDefault", f, null);
        f.getBasicBlockList().add(f.getBasicBlockList().indexOf(defaultBB), newDefaultBB);
        BranchInst inst = new BranchInst(defaultBB, newDefaultBB);

        // If there is an entry in any PHI nodes for the default edge, make sure
        // to update them as well.
        for (Instruction i : defaultBB.getInstList())
        {
            if (!(i instanceof PhiNode))
                break;
            PhiNode pn = (PhiNode)i;
            int blockIdx =pn.getBasicBlockIndex(originBB);
            assert blockIdx != -1:"Switch didn't go to this succcessor?";
            pn.setIncomingBlock(blockIdx, newDefaultBB);
        }

        ArrayList<CaseRange> cases = new ArrayList<>();
        int numCmps = cluserify(cases, swInst);

        BasicBlock switchBlock = switchConvert(cases, val, originBB, defaultBB);

        // We are done with the switch instruction, erase it.
        curBB.getInstList().remove(swInst);

        // Unconditional branch to the new block if then stuff.
        BranchInst br = new BranchInst(switchBlock, originBB);
    }

    /**
     * Transform the simple list of cases into CaseRange list.
     * @param cases
     * @param swInst
     * @return
     */
    private int cluserify(ArrayList<CaseRange> cases, SwitchInst swInst)
    {
        int numCmps = 0;

        for (int i = 1; i< swInst.getNumOfSuccessors(); i++)
        {
            cases.add(new CaseRange(
                    swInst.getSuccessorValue(i),
                    swInst.getSuccessorValue(i),
                    swInst.getSuccessor(i)));
        }
        cases.sort(CaseCmp);

        // Merges into cluster.
        if (cases.size() >=2)
        {
            for (int i =0, j= 1; j< cases.size();)
            {
                long nextVal = ((ConstantInt)cases.get(j).low).getSExtValue();
                long curVal = ((ConstantInt)cases.get(i).high).getSExtValue();
                BasicBlock nextBB = cases.get(j).bb;
                BasicBlock curBB = cases.get(i).bb;

                // If the two neighboring cases go to the same destination, merge
                // them into a single case.
                if (nextVal - curVal == 1 && curBB == nextBB)
                {
                    cases.get(i).high = cases.get(j).high;
                    cases.remove(j);
                }
                else
                {
                    i = j++;
                }
            }

            for (int i = 0, e = cases.size(); i<e; i++, ++numCmps)
            {
                if (cases.get(i).low != cases.get(i).high)
                    // A range counts double, since it requires two compares.
                    ++numCmps;
            }
        }
        return numCmps;
    }

    /**
     * Converts the switch instruction into binary search of the case value.
     * @param cases
     * @param val
     * @param origin
     * @param defaultBB
     * @return
     */
    private BasicBlock switchConvert(
            List<CaseRange> cases,
            Value val, BasicBlock origin,
            BasicBlock defaultBB)
    {
        int size = cases.size();
        if (size == 1)
            return newLeafBlock(cases.get(0), val, origin, defaultBB);

        int mid = size>>1;
        List<CaseRange> lhs = cases.subList(0, mid);
        List<CaseRange> rhs = cases.subList(mid, cases.size());

        CaseRange pivot = cases.get(mid);
        BasicBlock lBranch = switchConvert(lhs, val, origin, defaultBB);
        BasicBlock rBranch = switchConvert(rhs, val, origin, defaultBB);

        // Create a new node that checks if the value is < pivot. Go to the
        // left branch if it is and right branch if not.
        Function f = origin.getParent();
        BasicBlock newNode = BasicBlock.createBasicBlock("NodeBlock", f, null);
        int idx = f.getBasicBlockList().indexOf(origin);
        f.getBasicBlockList().add(++idx, newNode);

        ICmpInst cmp = new ICmpInst(Predicate.ICMP_SLT, val, pivot.low, "Pivot");
        newNode.getInstList().add(cmp);
        BranchInst br = new BranchInst(lBranch, rBranch, cmp, newNode);
        return newNode;
    }

    /**
     * Creates a new leaf block for the binary lookup tree.
     * It checks if the switch's value == the cases' value.
     * If not, then it jumps to the default branch. At the point int the tree,
     * the value cann't be another valid case value, so the jump to default branch
     * is warranted.
     * @param leaf
     * @param val
     * @param origBlock
     * @param defaultBB
     * @return
     */
    private BasicBlock newLeafBlock(CaseRange leaf, Value val,
            BasicBlock origBlock,
            BasicBlock defaultBB)
    {
        Function f = origBlock.getParent();
        BasicBlock newLeaf = BasicBlock.createBasicBlock("leafBlock", f, null);
        int idx = f.getBasicBlockList().indexOf(origBlock);
        f.getBasicBlockList().add(++idx, newLeaf);

        // emit comparison.
        ICmpInst cmp = null;
        if (leaf.low == leaf.high)
        {
            // make the setq instruction.
            cmp = new ICmpInst(Predicate.ICMP_EQ, val, leaf.low, "switchleaf");
        }
        else
        {
            // make the range comp.
            if (((ConstantInt)leaf.low).isMinValue(true/*isSigned*/))
            {
                // value >=low && value<= high ----> value<=high.
                cmp = new ICmpInst(Predicate.ICMP_SLE, val, leaf.high, "switchLeaf");
            }
            if (((ConstantInt)leaf.low).isZero())
            {
                // value >=0 && value<= high ----> value<=high(unsigned).
                cmp = new ICmpInst(Predicate.ICMP_ULE, val, leaf.high, "switchLeaf");
            }
            else
            {
                // emit value - Lo <= high-Lo (unsigned).
                Constant negLo = ConstantExpr.getNeg(leaf.low);
                Instruction add = BinaryInstruction
                        .createAdd(val, negLo, val.getName() +".off",
                        newLeaf);

                Constant upperBound = ConstantExpr.getAdd(negLo, leaf.high);
                cmp = new ICmpInst(Predicate.ICMP_ULE, add, upperBound, "switchLeaf");
            }
        }

        // emit comparison for branch.
        BasicBlock succ = leaf.bb;
        BranchInst inst = new BranchInst(succ, defaultBB, cmp, newLeaf);

        // If there were any PHI nodes in this successor, rewrite one entry
        // from OrigBlock to come from NewLeaf.
        for (Instruction i : succ.getInstList())
        {
            if (!(i instanceof PhiNode))
                break;

            PhiNode pn = (PhiNode)i;
            long range = ((ConstantInt)leaf.high).getSExtValue()
                    - ((ConstantInt)leaf.low).getSExtValue();
            for (long j = 0; j < range; j++)
                pn.removeIncomingValue(origBlock);

            int blockIdx = pn.getBasicBlockIndex(origBlock);
            assert blockIdx != -1:"Switch didn't go to this sucessor!";
            pn.setIncomingBlock(blockIdx, newLeaf);
        }

        return newLeaf;
    }
}
