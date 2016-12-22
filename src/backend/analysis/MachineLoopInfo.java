package backend.analysis;
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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionPass;
import backend.pass.AnalysisUsage;
import backend.support.DepthFirstOrder;

import java.util.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineLoopInfo extends MachineFunctionPass
    implements LoopInfoBase<MachineBasicBlock, MachineLoop>
{
    private HashMap<MachineBasicBlock, MachineLoop> bbMap = new HashMap<>();

    private ArrayList<MachineLoop> topLevelLoops = new ArrayList<>();

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        assert au != null;
        au.addRequired(MachineDominatorTree.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public String getPassName()
    {
        return "Machine Natural MachineLoop tree Construction";
    }

    /**
     * This method must be overridded by concrete subclass for performing
     * desired machine code transformation or analysis.
     *
     * @param mf
     * @return
     */
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        calculate(getAnalysisToUpDate(MachineDominatorTree.class));
        return false;
    }

    private void calculate(MachineDominatorTree dt)
    {
        MachineBasicBlock rootNode = dt.getRootNode().getBlock();

        Set<MachineBasicBlock> dfList = DepthFirstOrder.reversePostOrder(rootNode);
        for (MachineBasicBlock bb : dfList)
        {
            MachineLoop loop = considerForLoop(bb, dt);
            if (loop != null)
                topLevelLoops.add(loop);
        }
    }

    private MachineLoop considerForLoop(MachineBasicBlock bb, MachineDominatorTree dt)
    {
        if (bbMap.containsKey(bb))
            return null;

        Stack<MachineBasicBlock> todoStack = new Stack<>();
        Iterator<MachineBasicBlock> itr = bb.predIterator();
        while (itr.hasNext())
        {
            MachineBasicBlock pred = itr.next();
            if (dt.dominates(bb, pred))
                todoStack.push(pred);
        }

        if (todoStack.isEmpty()) return null;

        MachineLoop l = new MachineLoop(bb);
        bbMap.put(bb, l);

        MachineBasicBlock entryBlock = bb.getParent().getEntryBlock();
        while (!todoStack.isEmpty())
        {
            MachineBasicBlock cur = todoStack.pop();
            // The current block is not contained in loop as yet,
            // and it is reachable from entry block.
            if (!l.contains(cur) && dt.dominates(entryBlock, cur))
            {
                // Check to see if this block already belongs to a loop.  If this occurs
                // then we have a case where a loop that is supposed to be a child of
                // the current loop was processed before the current loop.  When this
                // occurs, this child loop gets added to a part of the current loop,
                // making it a sibling to the current loop.  We have to reparent this
                // loop.
                MachineLoop subLoop = getLoopFor(cur);
                if (subLoop != null)
                {
                    if (subLoop.getHeaderBlock() == cur && isNotAlreadyContainedIn(subLoop, l))
                    {
                        assert subLoop.getParentLoop() != null && subLoop.getParentLoop() != l;
                        MachineLoop subParentLoop = subLoop.outerLoop;
                        assert subParentLoop.getSubLoops().contains(subLoop);
                        subParentLoop.subLoops.remove(subLoop);

                        subLoop.setParentLoop(l);
                        l.subLoops.add(subLoop);
                    }
                }

                l.blocks.add(cur);
                for (Iterator<MachineBasicBlock> predItr = cur.predIterator();
                     predItr.hasNext();)
                {
                    todoStack.push(predItr.next());
                }
            }
        }

        // If there are any loops nested within this loop, create them.
        for (MachineBasicBlock block : l.blocks)
        {
            MachineLoop newLoop = considerForLoop(block, dt);
            if (newLoop != null)
            {
                l.subLoops.add(newLoop);
                newLoop.setParentLoop(l);
            }
        }

        // Add the basic blocks that comprise this loop to the BBMap so that this
        // loop can be found for them.
        for (MachineBasicBlock block : l.blocks)
        {
            if (!bbMap.containsKey(block))
            {
                bbMap.put(block, l);
            }
        }

        HashMap<MachineBasicBlock, MachineLoop> containingLoops = new HashMap<>();
        for (int i = 0; i < l.subLoops.size(); i++)
        {
            MachineLoop childLoop = l.subLoops.get(i);
            assert childLoop.getParentLoop() == l;

            MachineLoop containedLoop;
            if ((containedLoop = containingLoops.get(childLoop.getHeaderBlock())) != null)
            {
                moveSiblingLoopInto(childLoop, containedLoop);
                --i;
            }
            else
            {
                for (int b = 0, e = childLoop.blocks.size(); b < e; b++)
                {
                    MachineLoop blockLoop = containingLoops.get(childLoop.blocks.get(i));
                    if (blockLoop == null)
                        blockLoop = childLoop;
                    else if (blockLoop != childLoop)
                    {
                        MachineLoop subLoop = blockLoop;
                        for (int j = 0, sz = subLoop.blocks.size(); j < sz; j++)
                        {
                            containingLoops.put(subLoop.blocks.get(j), childLoop);

                            moveSiblingLoopInto(subLoop, childLoop);
                            --i;
                        }
                    }
                }
            }
        }

        return l;
    }

    /**
     * This method moves the newChild loop to live inside of the newParent,
     * instead of being a slibing of it.
     * @param newChild
     * @param newParent
     */
    private void moveSiblingLoopInto(MachineLoop newChild, MachineLoop newParent)
    {
        MachineLoop oldParent = newChild.getParentLoop();
        assert oldParent != null && oldParent == newParent.getParentLoop();

        assert oldParent.subLoops.contains(newChild) :"Parent field incorrent!";
        oldParent.subLoops.remove(newChild);
        newParent.subLoops.add(newChild);
        newChild.setParentLoop(null);

        insertLoopInto(newChild, newParent);
    }

    private void insertLoopInto(MachineLoop child, MachineLoop parent)
    {
        MachineBasicBlock header = child.getHeaderBlock();
        assert parent.contains(header) : "This loop should not be inserted here";

        // Check to see if it belongs in a child loop...
        for (int i = 0, e = parent.subLoops.size(); i < e; i++)
        {
            if (parent.subLoops.get(i).contains(header))
            {
                insertLoopInto(child, parent.subLoops.get(i));
                return;
            }
        }

        parent.subLoops.add(child);
        child.setParentLoop(parent);
    }

    private boolean isNotAlreadyContainedIn(MachineLoop subLoop, MachineLoop parentLoop)
    {
        if (subLoop == null) return true;
        if (subLoop == parentLoop) return false;
        return isNotAlreadyContainedIn(subLoop.getParentLoop(), parentLoop);
    }

    @Override
    public HashMap<MachineBasicBlock, MachineLoop> getBBMap()
    {
        return bbMap;
    }

    @Override
    public ArrayList<MachineLoop> getTopLevelLoop()
    {
        return topLevelLoops;
    }

    @Override
    public int getLoopDepth(MachineBasicBlock bb)
    {
        MachineLoop ml = getLoopFor(bb);
        return ml != null ? ml.getLoopDepth() : 0;
    }

    @Override
    public boolean isLoopHeader(MachineBasicBlock bb)
    {
        MachineLoop ml = getLoopFor(bb);
        return ml != null && ml.getHeaderBlock() == bb;
    }

    @Override
    public void ensureIsTopLevel(MachineLoop loop, String msg)
    {
        assert loop.getParentLoop() == null : msg;
    }

    @Override
    public void removeBlock(MachineBasicBlock mbb)
    {
        if (bbMap.containsKey(mbb))
        {
            MachineLoop loop = bbMap.get(mbb);
            while(loop != null)
            {
                loop.removeBlockFromLoop(mbb);
                loop = loop.getParentLoop();
            }
            bbMap.remove(mbb);
        }
    }
}
