package backend.transform.scalars;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.pass.Pass;
import backend.support.LLVMContext;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.ReturnInst;

import java.util.LinkedList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class UnifyFunctionExitNodes implements FunctionPass
{
    private BasicBlock returnBlock;

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
    public static Pass createUnifyFunctionExitNodes()
    {
        return new UnifyFunctionExitNodes();
    }

    @Override
    public String getPassName()
    {
        return "Unify the function exit nodes";
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addPreserved(BreakCriticalEdge.class);
        au.addPreserved(Mem2Reg.class);
        au.addPreserved(LowerSwitch.class);
    }

    /**
     * Unify all exit nodes of the CFG by creating a new
     * BasicBlock, and converting all returns to unconditional branches to this
     * new basic block.  The singular exit node is returned.
     *
     * If there are no return stmts in the FunctionProto, a null pointer is returned.
     * @param f
     * @return
     */
    @Override
    public boolean runOnFunction(Function f)
    {
        LinkedList<BasicBlock> returnBlocks = new LinkedList<>();
        for (BasicBlock bb : f)
            if (bb.getTerminator() instanceof ReturnInst)
                returnBlocks.addLast(bb);

        // Handles the return basic block.
        if (returnBlocks.isEmpty())
        {
            returnBlock = null;
            return false;
        }
        else if (returnBlocks.size() == 1)
        {
            returnBlock = returnBlocks.getFirst();
            return false;
        }
        // Otherwise, there are multiple basic block where return inst lives in.
        // We need to insert PHI node in the new created unified return block
        // for merging multiple incomging value from each return block.
        BasicBlock unifiedBB = BasicBlock.createBasicBlock( "UnifiedReturnBlock", f);
        PhiNode pn = null;

        // If the function has returned of void type.
        if (f.getReturnType().equals(LLVMContext.VoidTy))
            new ReturnInst(null, "UnifiedRetVal", unifiedBB);
        else
        {
            pn = new PhiNode(f.getReturnType(), returnBlocks.size(), "UnifiedRetVal");
            unifiedBB.appendInst(pn);
            new ReturnInst(pn, "UnifiedRetVal", unifiedBB);
        }

        // Fills value in the new created PHI node.
        for (BasicBlock bb : returnBlocks)
        {
            if (pn != null)
                pn.addIncoming(bb.getTerminator().operand(0), bb);

            // Replace the last return instruction in the return block with
            // a unconditional branch to the newly created unified return block.
            bb.getInstList().removeLast();
            new Instruction.BranchInst(unifiedBB,  bb);
        }

        returnBlock = unifiedBB;
        return true;
    }

    /**
     * Returns newly created return basic block of CFG if nonexistence.
     * @return
     */
    public BasicBlock getReturnBlock()
    {
        return returnBlock;
    }
}
