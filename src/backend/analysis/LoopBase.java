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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class LoopBase<BlockT, LoopT>
{
    /**
     * <p>
     * A sequence of block Id.
     * <br>
     * The first item must be a loop header block. Also all of block
     * id are sorted in descending the {@linkplain #getLoopDepth()} by an
     * instance of {@linkplain Comparator}.
     * </p>
     */
    protected LinkedList<BlockT> blocks;
    /**
     * The index of this loop.
     */
    protected int loopIndex;
    /**
     * A pointer to its outer loop loop which isDeclScope this.
     */
    protected LoopT outerLoop;
    /**
     * An array of its subLoops loop contained in this.
     */
    protected ArrayList<LoopT> subLoops;

    protected LoopBase(BlockT block)
    {
        assert block!= null;
        blocks = new LinkedList<BlockT>();
        blocks.add(block);
        subLoops = new ArrayList<>();
    }

    /**
     * Retrieves the index of a basic block at the specified position where indexed by a index
     * variable.
     * @param index	A index that indexed into specified position where TargetData block located.
     * @return	A basic block.
     */
    public BlockT getBlock(int index)
    {
        assert index >= 0 && index < blocks.size();
        return blocks.get(index);
    }

    /**
     * Obtains the header block of this loop.
     * @return
     */
    public BlockT getHeaderBlock()
    {
        assert blocks != null && !blocks.isEmpty() :"There is no block in loop";
        return blocks.get(0);
    }

    public int getNumOfBlocks()
    {
        return blocks.size();
    }

    /**
     * Obtains the depth of this loop in the loop forest it attached, begins from
     * 1.
     * @return
     */
    public abstract int getLoopDepth();

    public ArrayList<LoopT> getSubLoops()
    {
        return subLoops;
    }

    /**
     * Obtains the depth of this loop in the loop forest it attached, begins from
     * 1.
     * @return
     */
    public List<BlockT> getBlocks()
    {
        return blocks;
    }

    /**
     * Check to see if a basic block is the loop exit block or not on that
     * if the any successor block of the given bb is outside this loop, so that
     * this bb would be a loop exit block..
     * @param bb
     * @return True if the given block is the exit block of this loop, otherwise
     * returned false.
     */
    public abstract boolean isLoopExitBlock(BlockT bb);

    /**
     * Computes the backward edge leading to the header block in the loop.
     * @return
     */
    public abstract int getNumBackEdges();

    /**
     * <p>
     * IfStmt there is a preheader for this loop, return it.  A loop has a preheader
     * if there is only one edge to the header of the loop from outside of the
     * loop.  IfStmt this is the case, the block branching to the header of the loop
     * is the preheader node.
     * </p>
     * <p>This method returns null if there is no preheader for the loop.</p>
     * @return
     */
    public abstract BlockT getPreheader();

    /**
     * IfStmt given loop's header has exactly one predecessor outside of loop,
     * return it, otherwise, return null.
     * @return
     */
    protected abstract BlockT getLoopPredecessor();

    /**
     * IfStmt given basic block is contained in this loop, return true,
     * otherwise false returned.
     * @param block
     * @return
     */
    public boolean contains(BlockT block)
    {
        return blocks.contains(block);
    }

    public void addFirstBlock(BlockT bb)
    {
        assert bb != null : "bb not be null";
        assert !contains(bb) : "duplicated block added";

        blocks.addFirst(bb);
    }

    /**
     * Returns a list of all loop exit block.
     * @return
     */
    public ArrayList<BlockT> getExitBlocks()
    {
        ArrayList<BlockT> exitBlocks = new ArrayList<>(blocks.size());
        blocks.forEach(bb ->
        {
            if (isLoopExitBlock(bb))
                exitBlocks.add(bb);
        });
        return exitBlocks;
    }

    /**
     * If {@linkplain #getExitBlocks()} exactly return one block, then this
     * method will return it, otherwise retunn null;
     * @return
     */
    public BlockT getExitBlock()
    {
        ArrayList<BlockT> res = getExitBlocks();
        if (res.size() == 1)
            return res.get(0);
        return null;
    }

    public void setParentLoop(LoopT newParent)
    {
        outerLoop = newParent;
    }

    public LoopT getParentLoop()
    {
        return outerLoop;
    }

    public void removeChildLoop(int index)
    {
        assert index>= 0 && index < subLoops.size();
        subLoops.remove(index);
    }

    public void addBlockEntry(BlockT bb)
    {
        assert bb != null;
        blocks.add(bb);
    }

    public void removeBlockFromLoop(BlockT bb)
    {
        assert bb != null : "bb not be null";
        assert contains(bb) : "bb must contained in loop";
        blocks.remove(bb);
    }

    public abstract void replaceChildLoopWith(LoopT newOne, LoopT oldOne);
    public abstract void addChildLoop(LoopT loop);
    public abstract void addBasicBlockIntoLoop(BlockT bb, LoopInfoBase<BlockT, LoopT> li);
    public abstract void print(OutputStream os, int depth);
    public abstract void dump();
}
