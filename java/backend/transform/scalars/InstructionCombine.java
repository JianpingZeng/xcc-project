package backend.transform.scalars;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Jianping Zeng.
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
import backend.transform.scalars.instructionCombine.Combiner;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Value;
import tools.Util;

import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class InstructionCombine implements FunctionPass
{
    private AnalysisResolver resolver;
    private Combiner combiner;

    public InstructionCombine()
    {
        combiner = new Combiner();
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        if (f == null || f.empty()) return false;
        BasicBlock entryBB = f.getEntryBlock();
        HashSet<BasicBlock> reachable = new HashSet<>(DepthFirstOrder.dfTraversal(entryBB));

        boolean everChanged = false;
        for (Iterator<BasicBlock> itr = f.getBasicBlockList().iterator(); itr.hasNext();)
        {
            BasicBlock bb = itr.next();
            if (!reachable.contains(bb))
            {
                // delete the dead block.
                Util.assertion(bb.isUseEmpty(), "Unreachable block must no use");
                itr.remove();
                bb.eraseFromParent();
                everChanged = true;
                continue;
            }
            for (int i = 0, e = bb.size(); i < e; i++)
            {
                Instruction inst = bb.getInstAt(i);
                // There are three status returned,
                // (1). null indicates the specified instruction is dead
                // and should be removed from enclosing block.
                // (2). as same as inst indicates that no transformation was performed.
                // (3). Otherwise calling to replaceAllUsesWith() to update it.
                Value res = combiner.visit(inst);
                if (res == null)
                {
                    --i;
                    --e;
                    everChanged = true;
                }
                else if (!res.equals(inst))
                {
                    res.replaceAllUsesWith(inst);
                    inst.eraseFromParent();
                    everChanged = true;
                }
            }
        }
        return everChanged;
    }

    @Override
    public String getPassName()
    {
        return "Instruction Combiner";
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return resolver;
    }

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        this.resolver = resolver;
    }

    public static FunctionPass createInstructionCombinePass()
    {
        return new InstructionCombine();
    }
}
