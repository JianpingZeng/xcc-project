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

import backend.pass.AnalysisResolver;
import backend.pass.FunctionPass;
import backend.support.DepthFirstOrder;
import backend.utils.SuccIterator;
import backend.value.BasicBlock;
import backend.value.Constant;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.PhiNode;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class UnreachableBlockElim implements FunctionPass
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

    @Override
    public String getPassName()
    {
        return "Unreachable block elimination pass.";
    }

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
        // marks the reachable block by visiting the CFG in the order of
        // depth-first.
        ArrayList<BasicBlock> visited;
        visited = DepthFirstOrder.reversePostOrder(f.getEntryBlock());

        ArrayList<BasicBlock> deadedBlocks = new ArrayList<>();
        for (BasicBlock cur : f.getBasicBlockList())
        {
            if (!visited.contains(cur))
            {
                deadedBlocks.add(cur);
                Instruction inst = cur.getFirstInst();
                while (inst instanceof PhiNode)
                {
                    PhiNode ph = (PhiNode)inst;
                    ph.replaceAllUsesWith(Constant.getNullValue(ph.getType()));
                    cur.getInstList().removeFirst();
                }
                for (SuccIterator sucItr = cur.succIterator(); sucItr.hasNext();)
                    sucItr.next().removePredecessor(cur);
                cur.dropAllReferences();
            }
        }

        for (BasicBlock bb : deadedBlocks)
            bb.eraseFromParent();

        return deadedBlocks.size() != 0;
    }

    public static UnreachableBlockElim createUnreachableBlockEliminationPass()
    {
        return new UnreachableBlockElim();
    }
}
