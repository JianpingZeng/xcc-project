package backend.analysis;
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

import tools.Util;
import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionPass;
import backend.pass.AnalysisUsage;
import backend.support.DepthFirstOrder;
import backend.support.LoopInfoBase;

import java.util.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineLoop extends MachineFunctionPass
    implements LoopInfoBase<MachineBasicBlock, MachineLoopInfo>
{
    private HashMap<MachineBasicBlock, MachineLoopInfo> bbMap = new HashMap<>();

    private ArrayList<MachineLoopInfo> topLevelLoops = new ArrayList<>();

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        Util.assertion( au != null);
        au.setPreservedAll();
        au.addRequired(MachineDomTree.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public String getPassName()
    {
        return "Machine Natural MachineLoopInfo tree Construction";
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
        calculate((MachineDomTree) getAnalysisToUpDate(MachineDomTree.class));
        return false;
    }

    private void calculate(MachineDomTree dt)
    {
        MachineBasicBlock rootNode = dt.getRootNode().getBlock();

        ArrayList<MachineBasicBlock> dfList = DepthFirstOrder.reversePostOrder(rootNode);
        for (MachineBasicBlock bb : dfList)
        {
            MachineLoopInfo loop = considerForLoop(bb, dt);
            if (loop != null)
                topLevelLoops.add(loop);
        }
    }

    private MachineLoopInfo considerForLoop(MachineBasicBlock bb, MachineDomTree dt)
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

        MachineLoopInfo l = new MachineLoopInfo(bb);
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
                MachineLoopInfo subLoop = getLoopFor(cur);
                if (subLoop != null)
                {
                    if (subLoop.getHeaderBlock() == cur && isNotAlreadyContainedIn(subLoop, l))
                    {
                        Util.assertion( subLoop.getParentLoop() != null && subLoop.getParentLoop() != l);
                        MachineLoopInfo subParentLoop = subLoop.outerLoop;
                        Util.assertion( subParentLoop.getSubLoops().contains(subLoop));
                        subParentLoop.subLoops.remove(subLoop);

                        subLoop.setParentLoop(l);
                        l.subLoops.add(subLoop);
                    }
                }

                l.addBasicBlockIntoLoop(cur, this);
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
            MachineLoopInfo newLoop = considerForLoop(block, dt);
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

        HashMap<MachineBasicBlock, MachineLoopInfo> containingLoops = new HashMap<>();
        for (int i = 0; i < l.subLoops.size(); i++)
        {
            MachineLoopInfo childLoop = l.subLoops.get(i);
            Util.assertion( childLoop.getParentLoop() == l);

            MachineLoopInfo containedLoop;
            if ((containedLoop = containingLoops.get(childLoop.getHeaderBlock())) != null)
            {
                moveSiblingLoopInto(childLoop, containedLoop);
                --i;
            }
            else
            {
                for (int b = 0, e = childLoop.blocks.size(); b < e; b++)
                {
                    MachineLoopInfo blockLoop = containingLoops.get(childLoop.blocks.get(i));
                    if (blockLoop == null)
                        blockLoop = childLoop;
                    else if (blockLoop != childLoop)
                    {
                        for (int j = 0, sz = blockLoop.blocks.size(); j < sz; j++)
                        {
                            containingLoops.put(blockLoop.blocks.get(j), childLoop);

                            moveSiblingLoopInto(blockLoop, childLoop);
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
    private void moveSiblingLoopInto(MachineLoopInfo newChild, MachineLoopInfo newParent)
    {
        MachineLoopInfo oldParent = newChild.getParentLoop();
        Util.assertion( oldParent != null && oldParent == newParent.getParentLoop());

        Util.assertion(oldParent.subLoops.contains(newChild), "Parent field incorrent!");
        oldParent.subLoops.remove(newChild);
        newParent.subLoops.add(newChild);
        newChild.setParentLoop(null);

        insertLoopInto(newChild, newParent);
    }

    private void insertLoopInto(MachineLoopInfo child, MachineLoopInfo parent)
    {
        MachineBasicBlock header = child.getHeaderBlock();
        Util.assertion(parent.contains(header),  "This loop should not be inserted here");

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

    private boolean isNotAlreadyContainedIn(MachineLoopInfo subLoop, MachineLoopInfo parentLoop)
    {
        if (subLoop == null) return true;
        if (subLoop == parentLoop) return false;
        return isNotAlreadyContainedIn(subLoop.getParentLoop(), parentLoop);
    }

    @Override
    public HashMap<MachineBasicBlock, MachineLoopInfo> getBBMap()
    {
        return bbMap;
    }

    @Override
    public ArrayList<MachineLoopInfo> getTopLevelLoop()
    {
        return topLevelLoops;
    }

    @Override
    public int getLoopDepth(MachineBasicBlock bb)
    {
        MachineLoopInfo ml = getLoopFor(bb);
        return ml != null ? ml.getLoopDepth() : 0;
    }

    @Override
    public boolean isLoopHeader(MachineBasicBlock bb)
    {
        MachineLoopInfo ml = getLoopFor(bb);
        return ml != null && ml.getHeaderBlock() == bb;
    }

    @Override
    public void ensureIsTopLevel(MachineLoopInfo loop, String msg)
    {
        Util.assertion(loop.getParentLoop() == null,  msg);
    }

    @Override
    public void removeBlock(MachineBasicBlock mbb)
    {
        if (bbMap.containsKey(mbb))
        {
            MachineLoopInfo loop = bbMap.get(mbb);
            while(loop != null)
            {
                loop.removeBlockFromLoop(mbb);
                loop = loop.getParentLoop();
            }
            bbMap.remove(mbb);
        }
    }
}
