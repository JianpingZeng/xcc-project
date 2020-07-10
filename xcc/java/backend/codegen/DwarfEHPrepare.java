/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.codegen;

import backend.pass.AnalysisResolver;
import backend.pass.FunctionPass;
import backend.support.LLVMContext;
import backend.target.TargetMachine;
import backend.type.FunctionType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Value.UndefValue;

import java.util.ArrayList;

public class DwarfEHPrepare implements FunctionPass {
    private AnalysisResolver resolver;
    private TargetMachine tm;
    /**
     * The declaration of function _Unwind_Resume or the target equivalent one.
     */
    private Constant unwindFunction;

    @Override
    public AnalysisResolver getAnalysisResolver() {
        return resolver;
    }

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver) {
        this.resolver = resolver;
    }

    public static FunctionPass createDwarfEHPreparePass(TargetMachine tm) {
        return new DwarfEHPrepare(tm);
    }

    private DwarfEHPrepare(TargetMachine tm) { this.tm = tm; }

    @Override
    public boolean runOnFunction(Function f) {
        return insertUnwindResumeCalls(f);
    }

    private void getOrCreateUnwindFunction(Function fn) {
        if(unwindFunction == null) {
            LLVMContext ctx = fn.getContext();
            ArrayList<Type> argTy = new ArrayList<>();
            argTy.add(Type.getInt8PtrTy(ctx, 0));
            FunctionType fty = FunctionType.get(Type.getVoidTy(ctx), argTy, false);
            String name = tm.getSubtarget().getTargetLowering().getLibCallName(RTLIB.UNWIND_RESUME);
            unwindFunction = fn.getParent().getOrInsertFunction(name, fty);
        }
    }

    private Instruction getExceptionObject(ResumeInst ri) {
        Value v = ri.operand(0);
        Instruction exnObj = null;
        InsertValueInst selIVI = v instanceof InsertValueInst ? (InsertValueInst)v : null;
        Instruction selObj = null;
        InsertValueInst excIVI = null;
        boolean eraseIVIs = false;
        if (selIVI != null) {
            if (selIVI.getNumIndices() == 1 && selIVI.getIndices()[0] == 1) {
                Value tmp = selIVI.operand(0);
                excIVI = tmp instanceof InsertValueInst ? (InsertValueInst)tmp : null;
                if (excIVI != null && excIVI.operand(0) instanceof UndefValue &&
                    excIVI.getNumIndices() == 1 && excIVI.getIndices()[0] == 0) {
                    exnObj = (Instruction) excIVI.operand(1);
                    selObj = (Instruction) selIVI.operand(1);
                    eraseIVIs = true;
                }
            }
        }

        if (exnObj == null)
            exnObj = new ExtractValueInst(ri.operand(0), new int[]{0}, "exn.obj", ri);
        ri.eraseFromParent();
        if (eraseIVIs) {
            if (selIVI.getNumIndices() == 0)
                selIVI.eraseFromParent();
            if (excIVI.getNumUses() == 0)
                excIVI.eraseFromParent();
            if (selObj != null && selObj.getNumUses() == 0)
                selObj.eraseFromParent();
        }
        return exnObj;
    }

    private boolean insertUnwindResumeCalls(Function f) {
        boolean usesNewEH = false;
        ArrayList<ResumeInst> resumes = new ArrayList<>();
        for (BasicBlock bb : f) {
            TerminatorInst ti = bb.getTerminator();
            if (ti == null) continue;
            if (ti instanceof ResumeInst)
                resumes.add((ResumeInst) ti);
            else if (ti instanceof InvokeInst)
                usesNewEH = ((InvokeInst)ti).getUnwindDest().isLandingPad();
        }

        if (resumes.isEmpty())
            return usesNewEH;

        getOrCreateUnwindFunction(f);
        LLVMContext ctx = f.getContext();
        int resumeSize = resumes.size();
        if (resumeSize == 1) {
            // Instead of creating a new BB and PHI node, just append the call to
            // _Unwind_Resume to the end of the single resume block.
            ResumeInst ri = resumes.get(0);
            BasicBlock unwindBB = ri.getParent();
            Instruction exnObj = getExceptionObject(ri);
            // call the _Unwind_Resume function.
            CallInst ci = CallInst.create(unwindFunction, new Value[]{exnObj}, "", unwindBB);
            ci.setCallingConv(tm.getSubtarget().getTargetLowering().getLibCallCallingConv(RTLIB.UNWIND_RESUME));
            new UnreachableInst(ctx, unwindBB);
            return true;
        }

        BasicBlock unwindBB = BasicBlock.createBasicBlock(ctx, "unwind_resume", f);
        PhiNode pn = new PhiNode(Type.getInt8PtrTy(ctx, 0), resumeSize, "exn.obj", unwindBB);
        for (ResumeInst ri : resumes) {
            BasicBlock riBB = ri.getParent();
            new BranchInst(unwindBB, riBB);
            Instruction exnObj = getExceptionObject(ri);
            pn.addIncoming(exnObj, riBB);
        }

        CallInst ci = CallInst.create(unwindFunction, new Value[]{pn}, "", unwindBB);
        ci.setCallingConv(tm.getSubtarget().getTargetLowering().getLibCallCallingConv(RTLIB.UNWIND_RESUME));
        new UnreachableInst(ctx, unwindBB);
        return true;
    }

    @Override
    public String getPassName() {
        return "Dwarf Exception handling preparation";
    }
}
