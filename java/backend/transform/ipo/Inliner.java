package backend.transform.ipo;
/*
 * Extremely C language Compiler.
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
import backend.analysis.CallGraph;
import backend.analysis.CallGraphNode;
import backend.intrinsic.Intrinsic;
import backend.pass.AnalysisUsage;
import backend.pass.CallGraphSCCPass;
import backend.support.Attribute;
import backend.support.CallSite;
import backend.support.LLVMContext;
import backend.target.TargetData;
import backend.transform.utils.ClonedCodeInfo;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Value.UndefValue;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.transform.utils.PruningFunctionCloner.cloneAndPruneFunctionInfo;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class Inliner extends CallGraphSCCPass
{
    /**
     * A value used for controlling if we should inline the specified call site.
     * set it's value default to 0.
     */
    protected int inlineThreshold;
    protected HashSet<Function> neverInlined;
    protected InlineCostAnalyzer analyzer;

    public Inliner()
    {
        this(0);
    }

    public Inliner(int threshold)
    {
        inlineThreshold = threshold;
        neverInlined = new HashSet<>();
    }

    public int getInlineThreshold() { return inlineThreshold; }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(TargetData.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public boolean runOnSCC(ArrayList<CallGraphNode> nodes)
    {
        if (nodes == null || nodes.isEmpty())
            return false;
        CallGraph cg = (CallGraph) getAnalysisToUpDate(CallGraph.class);
        TargetData td = (TargetData) getAnalysisToUpDate(TargetData.class);

        // Firstly, we collects unique function in SCC and avoiding duplicate
        HashSet<Function> funcs = new HashSet<>();
        for (CallGraphNode n : nodes)
        {
            Function f = n.getFunction();
            Util.assertion(f !=null, "Can't handle null function");
            funcs.add(f);
        }

        // Secondly, collects call sites.
        ArrayList<CallSite> uniqueCSs = new ArrayList<>();
        for (Function f : funcs)
        {
            for (BasicBlock bb : f)
                for (Instruction inst : bb)
                {
                    if (!(inst instanceof CallInst))
                        continue;
                    uniqueCSs.add(new CallSite((CallInst) inst));
                }
        }

        // re-insert those call graph node that haven't call to other function
        // in the first of uniqueCSs list.
        for (int i = 0, size = uniqueCSs.size(); i < size; i++)
        {
            CallSite cs = uniqueCSs.get(i);
            Function f = cs.getCalledFunction();
            if (f != null && funcs.contains(f))
            {
                uniqueCSs.remove(i);
                uniqueCSs.add(i, uniqueCSs.get(size-1));
                uniqueCSs.remove(size-1);
            }
        }
        // recording which formal parameter in called function will be replaced
        // by which actual argument.
        boolean changed = false;
        boolean localChanged;
        while (true)
        {
            localChanged = false;
            for (int i = 0, size = uniqueCSs.size(); i < size; i++) {
                CallSite cs = uniqueCSs.get(i);
                Function f = cs.getCalledFunction();
                // We shouldn't inline those declared function.
                if (f.isDeclaration()) {
                    uniqueCSs.remove(i);
                    i--;
                    size--;
                    continue;
                }
                // perform inlining on function from bottom to top.
                // A       B
                //  \     /
                //   v   v
                //    C
                // inlining C first and then A and B.
                if (shouldInline(cs)) {
                    if (inlineCallIfPossible(cs, cg, td, funcs)) {
                        uniqueCSs.remove(i);
                        --i;
                        --size;
                        localChanged = true;
                        changed = true;
                    }
                }
            }
            if (!localChanged) break;
        }
        return changed;
    }

    @Override
    public boolean doFinalization(CallGraph cg)
    {
        removeDeadFunctions(cg, neverInlined);
        return true;
    }

    public boolean shouldInline(CallSite cs)
    {
        InlineCost cost = analyzer.getInlineCost(cs, neverInlined);
        float fudgeFactor = analyzer.getInlineFudgeFactor(cs);
        if (cost.isNeverInline())
            return false;
        if (cost.isAlwaysInline())
            return true;
        return cost.getCost() <= fudgeFactor*inlineThreshold;
    }

    private static boolean inlineFunction(CallSite cs, CallGraph cg, TargetData td)
    {
        CallInst ci = cs.getInstruction();
        BasicBlock origBB = ci.getParent();

        Util.assertion(origBB != null && origBB.getParent() != null,
                "call instruction is not in function?");

        Function callee = cs.getCalledFunction();
        if (callee == null || callee.isDeclaration() ||
                // Can not handle vararg function.
                callee.getFunctionType().isVarArg())
            return false;

        Function caller = cs.getCaller();
        Module m = caller.getParent();
        // If this call to the callee is not a tail call flag,
        // we have to clear the tail flag of any caller that we
        // will inline.
        boolean mustClearTailCallFlag = !ci.isTailCall();

        // If the call to the callee can't throw any exception,
        // set the caller with nounwind flag.
        boolean markNounwind = ci.doesNotThrow();

        int argIdx = 0;
        HashMap<Value, Value> formalValToActual = new HashMap<>();
        for (Argument param : callee.getArgumentList())
        {
            Value actual = cs.getArgument(argIdx);
            // if this argument is passed by value, we can't directly
            // replace reference to formal parameter with actual argument.
            // Instead of we should use memcpy function to handle this situation.
            if (cs.paramHasAttr(argIdx + 1, Attribute.ByVal) &&
                    !callee.onlyReadsMemory())
            {
                Type aggTy = ((PointerType)param.getType()).getElementType();
                Type voidPtrTy = PointerType.getUnqual(LLVMContext.Int8Ty);

                int align = 1;
                if (td != null)
                    align = td.getPrefTypeAlignment(aggTy);
                Value newAlloca = new AllocaInst(aggTy, null, align,
                        param.getName(), caller.getEntryBlock().getFirstInst());;
                Type[] tys = {LLVMContext.Int64Ty};

                Function memcpy = Intrinsic.getDeclaration(m, Intrinsic.ID.memcpy, tys);
                // converting the type of newAlloca and actual argument into void pointer.
                BitCastInst destPtr = new BitCastInst(newAlloca, voidPtrTy, "tmpdest", ci);
                BitCastInst srcPtr = new BitCastInst(actual, voidPtrTy, "tmpsrc", ci);

                Value size = null;
                if (td != null)
                    size = ConstantInt.get(LLVMContext.Int64Ty, td.getTypeAllocSize(aggTy));
                else
                    size = ConstantExpr.getSizeOf(aggTy);

                Value[] callArgs = {destPtr, srcPtr, size, ConstantInt.get(LLVMContext.Int32Ty, 1)};
                CallInst memcpyCallInst = new CallInst(callArgs, memcpy, "", ci);

                if (cg != null)
                {
                    CallGraphNode memcpyNode = cg.getOrInsertFunction(memcpy);
                    CallGraphNode callerNode = cg.getNode(caller);
                    callerNode.addCalledFunction(memcpyCallInst, memcpyNode);
                }
                actual = newAlloca;
            }
            formalValToActual.put(param, actual);
            ++argIdx;
        }
        Util.assertion(argIdx == callee.getNumOfArgs(),
                "Mismatch number of arguments and parameters");

        int lastBlockIdx = caller.size()-1;
        ArrayList<ReturnInst> returns = new ArrayList<>();
        ClonedCodeInfo inlinedFunctionInfo = cloneAndPruneFunctionInfo(caller, callee,
                        formalValToActual, returns, ".i", td);
        int firstNewBlockIdx = lastBlockIdx+1;
        BasicBlock firstNewBlock = caller.getBlockAt(firstNewBlockIdx);
        if (cg != null)
            updateCallGraphAfterInlining(ci, firstNewBlock, formalValToActual, cg);

        // move alloca instruction in the entry of callee into entry of
        // caller.
        BasicBlock entryBB = caller.getEntryBlock();
        for (int i = 0, e = firstNewBlock.size(); i < e; i++)
        {
            Instruction inst = firstNewBlock.getInstAt(i);
            if (inst instanceof AllocaInst)
            {
                if (inst.isUseEmpty())
                {
                    inst.eraseFromParent();
                    --e;
                    --i;
                    continue;
                }
                entryBB.insertBefore(inst, 0);
            }
            else break;
        }

        // If the inlined code contained dynamic alloca instructions, wrap the inlined
        // code with llvm.stacksave/llvm.stackrestore intrinsics.
        if (inlinedFunctionInfo.containsDynamicAllocas)
        {
            Constant stackSave = Intrinsic.getDeclaration(m, Intrinsic.ID.stacksave);
            Constant stackRestore = Intrinsic.getDeclaration(m, Intrinsic.ID.stackrestore);

            CallGraphNode stackSaveNode = null, stackRestoreNode = null, callerNode = null;
            if (cg != null)
            {
                stackSaveNode = cg.getOrInsertFunction((Function) stackSave);
                stackRestoreNode = cg.getOrInsertFunction((Function) stackRestore);
                callerNode = cg.getNode(caller);
            }

            CallInst savePtr = new CallInst(stackSave, null, "savestack", firstNewBlock.getFirstInst());
            if (cg != null)
                callerNode.addCalledFunction(savePtr, stackSaveNode);

            // Insert a call to llvm.stackrestore before any return instructions in the
            // inlined function.
            for (Instruction ret : returns)
            {
                Value[] args = {savePtr};
                CallInst restorePtr = new CallInst(args, stackRestore, "restorestack", ret);
                if (cg != null)
                    callerNode.addCalledFunction(restorePtr, stackRestoreNode);
            }
        }

        // If we are inlining tail call instruction through a call site that isn't
        // marked 'tail', we must remove the tail marker for any calls in the inlined
        // code.  Also, calls inlined through a 'nounwind' call site should be marked
        // 'nounwind'.
        if (inlinedFunctionInfo.containsCalls &&
                (mustClearTailCallFlag || markNounwind))
        {
            for (int i = firstNewBlockIdx, size = caller.size(); i < size; ++i)
            {
                BasicBlock bb = caller.getBlockAt(i);
                for (Instruction inst : bb)
                {
                    if (inst instanceof CallInst)
                    {
                        ci = (CallInst) inst;
                        if (mustClearTailCallFlag)
                            ci.setTailCall(false);
                        if (markNounwind)
                            ci.setDoesNotThrow();
                    }
                }
            }
        }

        // If we are inlining through a 'nounwind' call site then any inlined 'unwind'
        // instructions are unreachable.
        /*
        // TODO 8/17/2018, UnwindInst;
        if (inlinedFunctionInfo.containsUnwinds && markNounwind)
        {
            for (int i = firstNewBlockIdx, size = caller.size(); i < size; ++i)
            {
                BasicBlock bb = caller.getBlockAt(i);
                TerminatorInst ti = bb.getTerminatorInst();
                if (ti instanceof UnwindInst)
                {
                    new UnreachableInst(ti);
                    ti.eraseFromParent();
                }
            }
        }*/

        // If we cloned in _exactly one_ basic block, and if that block ends in a
        // return instruction, we splice the body of the inlined callee directly into
        // the calling basic block.
        if (returns.size() == 1 && firstNewBlockIdx+1 == caller.size())
        {
            origBB.appendInstAfter(ci, firstNewBlock.getInstList());
            caller.getBasicBlockList().removeLast();

            if (!ci.isUseEmpty())
            {
                ReturnInst ret = returns.get(0);
                if (ci.equals(ret.getReturnValue()))
                    ci.replaceAllUsesWith(UndefValue.get(ci.getType()));
                else
                    ci.replaceAllUsesWith(ret.getReturnValue());
            }

            // Since we are now done with the Call/Invoke, we can delete it.
            ci.eraseFromParent();

            returns.get(0).eraseFromParent();
            return true;
        }

        // Otherwise, we have the normal case, of more than one block to inline or
        // multiple return sites.

        // We want to clone the entire callee function into the hole between the
        // "starter" and "ender" blocks.  How we accomplish this depends on whether
        // this is an invoke instruction or a call instruction.
        BasicBlock afterCallBlock = origBB.splitBasicBlock(ci, origBB.getName()+".exit");
        TerminatorInst ti = origBB.getTerminator();
        Util.assertion(ti != null && ti instanceof BranchInst);
        BranchInst br = (BranchInst) ti;
        br.setOperand(0, firstNewBlock);

        // set return value
        if (returns.size() > 1)
        {
            PhiNode retPN = null;
            // add incoming value for phinode.
            if (!ci.isUseEmpty())
            {
                retPN = new PhiNode(ci.getType(), returns.size(), "ret.phi",
                        afterCallBlock.getFirstInst());
                ci.replaceAllUsesWith(retPN);
                new ReturnInst(retPN, "phi.ret", afterCallBlock);
            }
            if (retPN != null) {
                for (Instruction ret : returns) {
                    retPN.addIncoming(((ReturnInst) ret).getReturnValue(), ret.getParent());
                    new BranchInst(afterCallBlock, ret);
                    ret.eraseFromParent();
                }
            }
        }
        else if (!returns.isEmpty())
        {
            Value retVal = returns.get(0).getReturnValue();
            if (!ci.isUseEmpty())
            {
                if (ci.equals(retVal))
                    ci.replaceAllUsesWith(UndefValue.get(ci.getType()));
                else
                    ci.replaceAllUsesWith(retVal);
            }
            BasicBlock retBB = returns.get(0).getParent();
            retBB.appendInstBefore(returns.get(0), afterCallBlock.getInstList());
            afterCallBlock.replaceAllUsesWith(retBB);
        }
        else if (!ci.isUseEmpty())
        {
            // if there is not return instruction in callee, but the call to callee has been used by
            // instruction, so that we should replace call with UndefValue.
            ci.replaceAllUsesWith(UndefValue.get(ci.getType()));
            new ReturnInst(afterCallBlock);
        }

        // reaches here, we should delete the call instruction.
        ci.eraseFromParent();
        Util.assertion(br.isUnconditional(), "splitBasicBlock broken.");
        // we should fold the origBB to the entry of callee if possible
        // in order to reduce some degree of complexity of CFG.
        BasicBlock calleeEntry = br.getSuccessor(0);
        origBB.appendInstBefore(br, calleeEntry.getInstList());

        // replace all uses with origBB.
        calleeEntry.replaceAllUsesWith(origBB);
        // remove the unconditional branch.
        br.eraseFromParent();

        // we need to erase the calleeEntry, since it is not needed at all.
        calleeEntry.eraseFromParent();
        return true;
    }

    private static void updateCallGraphAfterInlining(CallInst ci,
                                                     BasicBlock firstNewBlock,
                                                     HashMap<Value, Value> valueMap,
                                                     CallGraph cg)
    {
        // TODO: 2018/8/17
    }

    /**
     * Inline the specified function into caller if possible
     * (like, inline cost is less than given threshold, than
     * it will not significantly increase performance overhead).
     *
     * If inlining successfully, return true. Otherwise return false.
     * @param cs
     * @param cg
     * @param td
     * @param funcs
     * @return
     */
    public boolean inlineCallIfPossible(CallSite cs,
                                        CallGraph cg,
                                        TargetData td,
                                        HashSet<Function> funcs)
    {
        if (!inlineFunction(cs, cg, td)) return false;

        Function callee = cs.getCalledFunction();
        Function caller = cs.getCaller();
        if (callee.hasFnAttr(Attribute.StackProtectReq))
            caller.addFnAttr(Attribute.StackProtectReq);
        else if (callee.hasFnAttr(Attribute.StackProtect) &&
                !caller.hasFnAttr(Attribute.StackProtectReq))
            caller.addFnAttr(Attribute.StackProtect);

        // if we inlined the last possible call site to the function,
        // delete the function body right now.
        if (callee.isUseEmpty() && callee.hasLocalLinkage() &&
                !funcs.contains(callee))
        {
            CallGraphNode node = cg.getNode(callee);
            node.removeAllCalledFunctions();

            resetCachedCostInfo(node.getFunction());
            cg.removeFunctionFromModule(node);
        }
        return true;
    }

    private void resetCachedCostInfo(Function f)
    {
        // TODO: 2018/8/15
    }
    public void removeDeadFunctions(CallGraph cg, HashSet<Function> deadFuncs)
    {
        // TODO: 2018/8/13
    }

    public abstract InlineCost getInlineCost(CallSite cs);
    public abstract float getInlineFudgeFactor(CallSite cs);
}
