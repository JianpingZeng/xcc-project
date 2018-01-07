/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.transform.ipo;

import backend.ir.FreeInst;
import backend.ir.MallocInst;
import backend.pass.AnalysisResolver;
import backend.pass.ModulePass;
import backend.support.IntStatistic;
import backend.support.LLVMContext;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.CastInst;
import backend.value.Instruction.IntToPtrInst;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

/**
 * This class designed to served as converting malloc&free calls
 * to corresponding instructions.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RaiseAllocations implements ModulePass
{
    public final static IntStatistic NumRaised =
            new IntStatistic("NumRaised", "The number of raised malloc/free instruction");

    private Function mallocFunc;
    private Function freeFunc;

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

    public RaiseAllocations()
    {}

    public void doInitialization(Module m)
    {
        mallocFunc = m.getFunction("malloc");
        if (mallocFunc != null)
        {
            FunctionType tyWeHave = mallocFunc.getFunctionType();

            // Get the expected prototype for malloc
            ArrayList<Type> argTys = new ArrayList<>();
            argTys.add(LLVMContext.Int64Ty);
            FunctionType mallocType = FunctionType.get(PointerType.getUnqual(
                    LLVMContext.Int8Ty), argTys, false);

            // Chck to see if we got the expected malloc
            if (!tyWeHave.equals(mallocType))
            {
                // Check to see if the prototype is wrong, giving us sbyte*(uint) * malloc
                // This handles the common declaration of: 'void *malloc(unsigned);'
                ArrayList<Type> argTys2 = new ArrayList<>();
                argTys2.add(LLVMContext.Int32Ty);
                FunctionType mallocType2 = FunctionType.get(PointerType.getUnqual(
                        LLVMContext.Int8Ty), argTys2, false);
                if (!tyWeHave.equals(mallocType2))
                {
                    // Check to see if the prototype is missing, giving us
                    // sbyte*(...) * malloc
                    // This handles the common declaration of: 'void *malloc();'
                    FunctionType mallocType3 = FunctionType.get(
                            PointerType.getUnqual(LLVMContext.Int8Ty),
                            new ArrayList<>(), true);
                    if (!tyWeHave.equals(mallocType3))
                    {
                        // give up.
                        mallocFunc = null;
                    }
                }
            }
        }

        freeFunc = m.getFunction("free");
        if (freeFunc != null)
        {
            // Get the expected prototype for void free(i8*)
            FunctionType tyWeHave = freeFunc.getFunctionType();

            ArrayList<Type> argTys = new ArrayList<>();
            argTys.add(PointerType.getUnqual(LLVMContext.Int8Ty));
            FunctionType freeType1 = FunctionType.get(LLVMContext.VoidTy,
                    argTys, false);
            if (!tyWeHave.equals(freeType1))
            {
                // Check to see if the prototype was forgotten, giving us
                // void (...) * free
                // This handles the common forward declaration of: 'void free();'

                FunctionType freeType2 = FunctionType.get(LLVMContext.VoidTy,
                        new ArrayList<>(), true);
                if (!tyWeHave.equals(freeType2))
                {
                    // One last try, check to see if we can find free as
                    // int (...)* free.  This handles the case where NOTHING was declared.
                    FunctionType freeType3 = FunctionType.get(LLVMContext.Int64Ty,
                            new ArrayList<>(), true);
                    if (!tyWeHave.equals(freeType3))
                    {
                        // give up.
                        freeFunc = null;
                    }
                }
            }
        }

        // Don't mess with locally defined versions of these functions.
        if (mallocFunc != null && !mallocFunc.isDeclaration())
            mallocFunc = null;
        if (freeFunc != null && !freeFunc.isDeclaration())
            freeFunc = null;
    }

    /**
     * This method does the actual work of converting malloc&free calls
     * to corresponding instructions.
     * @param m
     * @return
     */
    @Override
    public boolean runOnModule(Module m)
    {
        // Find the malloc/free prototypes.
        doInitialization(m);

        boolean changed = false;
        // Step#1, process all of the malloc calls.
        if (mallocFunc != null)
        {
            Stack<User> users = new Stack<>();
            mallocFunc.getUseList().forEach(u->users.push(u.getUser()));
            HashSet<Value> eqPointers = new HashSet<>();
            while (!users.isEmpty())
            {
                User u = users.pop();
                if (u instanceof Instruction)
                {
                    CallInst ci = u instanceof CallInst ? (CallInst)u : null;
                    if (ci != null && ci.getNumsOfArgs() != 0 &&
                            (ci.getCalledFunction().equals(mallocFunc)) ||
                            eqPointers.contains(ci.getCalledFunction()))
                    {
                        Value source = ci.argumentAt(0);

                        // If no prototype was provided for malloc, we may need
                        // to cast the source size.
                        if (!source.getType().equals(LLVMContext.Int32Ty))
                        {
                            source = CastInst.createIntegerCast(source,
                                    LLVMContext.Int32Ty, /*isSigned*/false, "MallocAmtCast",
                                    ci);
                        }
                        MallocInst mi = new MallocInst(LLVMContext.Int8Ty, source, "", ci);
                        mi.setName(ci.getName());
                        ci.replaceAllUsesWith(mi);

                        // Delete this CallInst from basic block.
                        ci.eraseFromParent();
                        changed = true;
                        NumRaised.inc();
                    }
                }
                else if (u instanceof GlobalValue)
                {
                    u.getUseList().forEach(uu->users.push(uu.getUser()));
                    eqPointers.add(u);
                }
                else if (u instanceof ConstantExpr)
                {
                    ConstantExpr ce = (ConstantExpr)u;
                    if (ce.isCast())
                    {
                        ce.getUseList().forEach(uu->users.push(uu.getUser()));
                        eqPointers.add(ce);
                    }
                }
            }
        }

        // process free function.
        if (freeFunc != null)
        {
            Stack<User> users = new Stack<>();
            freeFunc.getUseList().forEach(u->users.push(u.getUser()));
            HashSet<Value> eqPointers = new HashSet<>();
            while (!users.isEmpty())
            {
                User u = users.pop();
                if (u instanceof Instruction)
                {
                    CallInst ci = u instanceof CallInst ? (CallInst)u : null;
                    if (ci != null && ci.getNumsOfArgs() != 0 &&
                            (ci.getCalledFunction().equals(freeFunc)) ||
                            eqPointers.contains(ci.getCalledFunction()))
                    {
                        Value source = ci.argumentAt(0);

                        if (!source.getType().isPointerType())
                        {
                            // Perform IntToPointer cast on array size expresion.
                            source = new IntToPtrInst(source,
                                    PointerType.getUnqual(LLVMContext.Int8Ty),
                                    "freePtrCast", ci);
                        }

                        new FreeInst(source, ci);

                        // Delete the CallInst.
                        if (!ci.getType().equals(LLVMContext.VoidTy))
                        {
                            ci.replaceAllUsesWith(Value.UndefValue.get(ci.getType()));
                        }

                        // Delete this CallInst from basic block.
                        ci.eraseFromParent();
                        changed = true;
                        NumRaised.inc();
                    }
                }
                else if (u instanceof GlobalValue)
                {
                    u.getUseList().forEach(uu->users.push(uu.getUser()));
                    eqPointers.add(u);
                }
                else if (u instanceof ConstantExpr)
                {
                    ConstantExpr ce = (ConstantExpr)u;
                    if (ce.isCast())
                    {
                        ce.getUseList().forEach(uu->users.push(uu.getUser()));
                        eqPointers.add(ce);
                    }
                }
            }
        }

        return changed;
    }

    @Override
    public String getPassName()
    {
        return "Raise allocations from calls to instructions";
    }

    /**
     * A factory method used for creating an instance.
     * @return
     */
    public static RaiseAllocations createRaiseAllocationsPass()
    {
        return new RaiseAllocations();
    }
}
