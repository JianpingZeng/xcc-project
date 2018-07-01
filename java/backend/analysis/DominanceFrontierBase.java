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
import backend.value.BasicBlock;
import backend.pass.FunctionPass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class DominanceFrontierBase implements FunctionPass
{
    protected HashMap<BasicBlock, HashSet<BasicBlock>> frontiers;
    protected ArrayList<BasicBlock> roots;
    protected boolean isPostDominators;

    protected DominanceFrontierBase(boolean isPost)
    {
        frontiers = new HashMap<>();
        roots = new ArrayList<>();
        isPostDominators = isPost;
    }

    public ArrayList<BasicBlock> getRoots()
    {
        return roots;
    }

    public boolean isPostDominators()
    {
        return isPostDominators;
    }

    public HashMap<BasicBlock, HashSet<BasicBlock>> getFrontiers()
    {
        return frontiers;
    }

    public HashSet<BasicBlock> find(BasicBlock bb)
    {
        return frontiers.getOrDefault(bb, null);
    }

    public void addBasicBlock(BasicBlock bb, HashSet<BasicBlock> frontiers)
    {
        Util.assertion(find(bb) != null, "Block already in Dominator frontiers");
        this.frontiers.put(bb, frontiers);
    }

    /**
     * Remove basic block parent's frontiers from frontiers set.
     * @param bb
     */
    public void removeBlock(BasicBlock bb)
    {
        Util.assertion(find(bb) != null, "Block is not in Dominator frontiers");
        frontiers.remove(bb);
    }

    public void addToFrontier(BasicBlock bb, BasicBlock frontierNode)
    {
        Util.assertion(find(bb) != null, "parent is not in frontier set");
        frontiers.get(bb).add(frontierNode);
    }

    public void removeFromFrontier(BasicBlock bb, BasicBlock frontierNode)
    {
        Util.assertion(find(bb) != null, "parent is not in frontier set");
        frontiers.get(bb).remove(frontierNode);
    }

    /**
     * Return fasle if tow domsets match. Otherwise return true.
     * @param ds1
     * @param ds2
     * @return
     */
    public boolean compareDomSet(HashSet<BasicBlock> ds1, HashSet<BasicBlock> ds2)
    {
        HashSet<BasicBlock> tempSet = new HashSet<>(ds2);

        for (BasicBlock bb : ds1)
        {
            if (!tempSet.remove(bb))
                // parent in ds1 but not in ds`.
                return true;
        }

        if (!tempSet.isEmpty())
            // There are other node in ds2 rather than in ds1.
            return true;

        return false;
    }

    /**
     * Return true if the other dominance frontier base matches
     * this dominance frontier base. Otherwise return false.
     * @param other
     * @return
     */
    public boolean compare(DominanceFrontierBase other)
    {
        HashMap<BasicBlock, HashSet<BasicBlock>> tempMaps = new HashMap<>();
        other.getFrontiers().entrySet().forEach(entry ->
        {
            tempMaps.put(entry.getKey(), entry.getValue());
        });

        for (HashMap.Entry<BasicBlock, HashSet<BasicBlock>> entry
                : frontiers.entrySet())
        {
            BasicBlock node = entry.getKey();
            if (other.find(node) == null)
                return true;

            HashSet<BasicBlock> frontiers = entry.getValue();
            if (compareDomSet(frontiers, tempMaps.get(node)))
                return true;
            tempMaps.remove(node);
        }
        if (!tempMaps.isEmpty())
            return true;

        return false;
    }
}
