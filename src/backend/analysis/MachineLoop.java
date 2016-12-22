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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineLoop extends LoopBase<MachineBasicBlock, MachineLoop>
{
    public MachineLoop(MachineBasicBlock block)
    {
        super(block);
    }

    @Override
    public int getLoopDepth()
    {
        int d = 1;
        for (MachineLoop cur = outerLoop; cur!= null; cur = cur.outerLoop)
            d++;
        return d;
    }

    @Override
    public boolean isLoopExitBlock(MachineBasicBlock bb)
    {
        if (!contains(bb))
            return false;
        for (Iterator<MachineBasicBlock> itr = bb.succIterator(); itr.hasNext();)
        {
            if (!contains(itr.next()))
                return true;
        }
        return false;
    }

    @Override
    public int getNumBackEdges()
    {
        int numBackEdges = 0;
        Iterator<MachineBasicBlock> itr = getHeaderBlock().predIterator();
        while (itr.hasNext())
        {
            if (contains(itr.next()))
                ++numBackEdges;
        }
        return numBackEdges;
    }

    @Override
    public MachineBasicBlock getPreheader()
    {
        // keep track of blocks outside the loop branching to the header
        MachineBasicBlock out = getLoopPredecessor();
        if (out == null) return null;

        // make sure there is exactly one exit out of the preheader
        if (out.getNumSuccessors() > 1)
            return null;
        // the predecessor has exactly one successor, so it is
        // a preheader.
        return out;
    }

    @Override
    protected MachineBasicBlock getLoopPredecessor()
    {
        MachineBasicBlock header = getHeaderBlock();
        MachineBasicBlock outer = null;
        for (Iterator<MachineBasicBlock> predItr = header.predIterator(); predItr.hasNext();)
        {
            MachineBasicBlock pred = predItr.next();
            if (!contains(pred))
            {
                if (outer != null && outer != pred)
                    return null;
                outer = pred;
            }
        }
        return outer;
    }

    /**
     * Return true if the specified machine loop contained in this.
     * @param loop
     * @return
     */
    public boolean contains(MachineLoop loop)
    {
        if (loop == null) return false;
        if (loop == this) return true;
        return contains(loop.outerLoop);
    }

    @Override
    public void replaceChildLoopWith(MachineLoop newOne, MachineLoop oldOne)
    {
        assert newOne != null && oldOne != null;
        assert oldOne.outerLoop == this;
        assert newOne.outerLoop == null;

        assert subLoops.contains(oldOne) :"oldOne loop not contained in current";
        int idx = subLoops.indexOf(oldOne);
        newOne.outerLoop = this;
        subLoops.set(idx, newOne);
    }

    @Override
    public void addChildLoop(MachineLoop loop)
    {
        assert loop != null && loop.outerLoop == null;
        loop.outerLoop = this;
        subLoops.add(loop);
    }

    @Override
    public void addBasicBlockIntoLoop(MachineBasicBlock bb,
            LoopInfoBase<MachineBasicBlock, MachineLoop> li)
    {
        assert blocks.isEmpty() || li.getLoopFor(getHeaderBlock()) != null
                :"Incorrect LI specifed for this loop";
        assert bb != null;
        assert li.getLoopFor(bb) == null;

        li.getBBMap().put(bb, this);
        MachineLoop l = this;
        while (l != null)
        {
            l.blocks.add(bb);
            l = l.getParentLoop();
        }
    }

    @Override
    public void print(OutputStream os, int depth)
    {
        try (PrintWriter writer = new PrintWriter(os))
        {
            writer.print(String.format("%" + depth * 2 + "s", " "));
            writer.printf("Loop at depth: %d, containing: ", getLoopDepth());
            for (int i = 0, e = blocks.size(); i < e; i++)
            {
                if (i != 0)
                    writer.print(",");
                MachineBasicBlock bb = blocks.get(i);
                writer.printf("Block#%s", bb.getNumber());
                if (bb == getHeaderBlock())
                    writer.print("<header>");
                if (isLoopExitBlock(bb))
                    writer.print("<exit>");
            }
            writer.println();
            for (MachineLoop subLoop : subLoops)
                subLoop.print(os, depth + 2);
        }
    }
    @Override
    public void dump()
    {
        print(System.err, 0);
    }
}
