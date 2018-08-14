package backend.transform.ipo;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.support.Attribute;
import backend.support.CallSite;
import backend.support.CallingConv;
import backend.type.PointerType;
import backend.type.VectorType;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.IntrinsicInst.DbgInfoIntrinsic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class InlineCostAnalyzer
{
    /**
     * Caches weight of each formal parametes of callee
     * for computing inlining weight
     */
    private static class ArgInfo
    {
        int allocaWeight;
        int constWeight;
        public ArgInfo(int allocaWeight, int constWeight)
        {
            this.allocaWeight = allocaWeight;
            this.constWeight = constWeight;
        }
    }
    /**
     * Caches some useful information for each function which would be
     * used by {@linkplain InlineCostAnalyzer} to determine whether the
     * function should be inlined or not.
     */
    private static class FunctionInliningInfo
    {
        boolean neverInline;
        boolean useDynamicAllocas;
        public int numInsts;
        int numVectorInsts;
        public int numBlocks;
        ArrayList<ArgInfo> weights;

        FunctionInliningInfo()
        {
            weights = new ArrayList<>();
        }

        private void clear()
        {
            neverInline = false;
            useDynamicAllocas = false;
            numInsts = 0;
            numVectorInsts = 0;
            numBlocks = 0;
            weights.clear();
        }

        public void analyzeFunctionInlineInfo(Function target)
        {
            clear();
            for (BasicBlock bb : target)
            {
                for (Instruction inst : bb)
                {
                    if (inst instanceof PhiNode || inst instanceof DbgInfoIntrinsic)
                        continue;

                    if (inst instanceof CallInst)
                    {
                        CallInst ci = (CallInst) inst;
                        String funcName = ci.getCalledFunction().getName();
                        if (ci.getCalledFunction().isDeclaration() &&
                                (funcName.equals("setjmp") || funcName.equals("_setjmp")))
                        {
                            neverInline = true;
                            return;
                        }
                        if (!(ci instanceof IntrinsicInst))
                            numInsts += 5;
                    }
                    else if (inst instanceof AllocaInst)
                    {
                        AllocaInst ai = (AllocaInst) inst;
                        if (!ai.isStaticAlloca())
                            useDynamicAllocas = true;
                    }
                    else if (inst.getType() instanceof VectorType)
                        ++numVectorInsts;
                    else if (inst.getOpcode() == Operator.IntToPtr  ||
                            inst.getOpcode() == Operator.PtrToInt)
                    {
                        // don't count the trivial cast instruction between
                        // integer and pointer.
                        continue;
                    }
                    else if (inst.getOpcode() == Operator.GetElementPtr)
                    {
                        GetElementPtrInst gep = (GetElementPtrInst) inst;
                        if (gep.hasAllConstantIndices())
                            continue;
                    }
                    ++numInsts;
                }
            }
            numBlocks = target.size();

            // collects weight for each formal parameter.
            for (Argument arg : target.getArgumentList())
            {
                weights.add(new ArgInfo(countProfitInlineAlloca(arg),
                        countProfitInlineConst(arg)));
            }
        }

        private int countProfitInlineConst(Value val)
        {
            int profit = 0;
            for (Use u : val.getUseList())
            {
                User user = u.getUser();
                // inline a branch instruction would be a good plus.
                if (user instanceof BranchInst)
                    profit += 40;
                else if (user instanceof SwitchInst)
                    profit += (((SwitchInst)user).getNumOfSuccessors()-1)*40;
                else if (user instanceof CallInst)
                {
                    CallInst ci = (CallInst) user;
                    // eliminate indirect call would be a big plus.
                    profit += ci.getCalledValue().equals(val) ? 500 : 0;
                }
                else
                {
                    Instruction inst = (Instruction) user;
                    boolean allOperandsConstant = true;
                    if (inst.mayReadMemory() || inst.mayHasSideEffects() ||
                            inst instanceof AllocaInst)
                        continue;
                    for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
                    {
                        Value op = inst.operand(i);
                        if (!(op instanceof Constant) || !op.equals(val))
                        {
                            allOperandsConstant = false;
                            break;
                        }
                    }
                    if (allOperandsConstant)
                    {
                        profit += 7;
                        profit += countProfitInlineConst(inst);
                    }
                }
            }
            return profit;
        }

        private int countProfitInlineAlloca(Value val)
        {
            if (!(val.getType() instanceof PointerType))
                return 0;
            int profit = 0;
            for (Use u : val.getUseList())
            {
                User user = u.getUser();
                if (user instanceof LoadInst || user instanceof StoreInst)
                    profit += 5;
                else if (user instanceof GetElementPtrInst)
                {
                    GetElementPtrInst gep = (GetElementPtrInst) user;
                    if (!gep.hasAllConstantIndices())
                        profit += countProfitInlineAlloca(gep) + 15;
                }
                else
                    return 0;
            }
            return profit;
        }
    }
    private HashMap<Function, FunctionInliningInfo> cachedFunctionInfos;

    public InlineCostAnalyzer()
    {
        cachedFunctionInfos = new HashMap<>();
    }

    public float getInlineFudgeFactor(CallSite cs)
    {
        Function callee = cs.getCalledFunction();
        Function caller = cs.getCaller();
        CallInst ci = cs.getInstruction();
        FunctionInliningInfo info = cachedFunctionInfos.get(callee);
        if (info == null)
        {
            info = new FunctionInliningInfo();
            info.analyzeFunctionInlineInfo(callee);
        }
        float factor = 1.0f;
        if (info.numBlocks == 1)
            factor += 0.5f;
        if (info.numVectorInsts > info.numInsts/2)
            factor += 2.0f;
        else if (info.numVectorInsts > info.numInsts*0.1)
            factor += 1.5;
        return factor;
    }

    public InlineCost getInlineCost(CallSite cs, HashSet<Function> neverInlined)
    {
        Function callee = cs.getCalledFunction();
        Function caller = cs.getCaller();
        CallInst ci = cs.getInstruction();
        BasicBlock bb = ci.getParent();

        // if we have an indirect call, we must not inline this call site.
        if (callee == null || callee.hasFnAttr(Attribute.NoInline) ||
                neverInlined.contains(callee))
            return InlineCost.getNever();

        FunctionInliningInfo info = new FunctionInliningInfo();
        if (!cachedFunctionInfos.containsKey(callee))
        {
            info.analyzeFunctionInlineInfo(callee);
        }
        else
            info = cachedFunctionInfos.get(callee);
        if (info.neverInline)
            return InlineCost.getNever();

        if (callee.hasFnAttr(Attribute.AlwaysInline) && !callee.isDeclaration())
            return InlineCost.getAlways();

        if (info.useDynamicAllocas)
        {
            FunctionInliningInfo callerInfo;
            if (!cachedFunctionInfos.containsKey(caller))
            {
                callerInfo = new FunctionInliningInfo();
                callerInfo.analyzeFunctionInlineInfo(caller);
            }
            else
                callerInfo = cachedFunctionInfos.get(caller);

            if (!callerInfo.useDynamicAllocas)
                return InlineCost.getNever();
        }

        int cost = 0;
        // If the callee function is defined in current file and will not
        // be used in another file, so that we should inline it.
        if (callee.hasLocalLinkage() && callee.hasOneUses())
            cost -= 15000;
        // If the calling convention is "Cold", should not inline it.
        if (callee.getCallingConv() == CallingConv.Cold)
            cost += 2000;
        Instruction afterInst = bb.getInstAt(ci.getIndexToBB()+1);
        if (afterInst != null && afterInst instanceof UnreachableInst)
            cost += 10000;

        for (int i = 0, e = cs.getNumOfArguments(); i < e; i++)
        {
            Value arg = cs.getArgument(i);
            // decrease by 20.
            cost -= 20;
            if (arg instanceof Function)
                cost -= 100;
            else if (arg instanceof AllocaInst)
                cost -= info.weights.get(i).allocaWeight;
            else if (arg instanceof Constant)
                cost -= info.weights.get(i).constWeight;
        }

        // don't inline the callee into a large function.
        // don't inline a big callee function into caller.
        cost += caller.size()/15;
        cost += info.numInsts*5;
        return InlineCost.get(cost);
    }
}
