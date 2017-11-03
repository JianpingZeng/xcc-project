/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

package backend.transform.ipa;

import backend.pass.ModulePass;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.Function;
import backend.value.Module;

import java.util.ArrayList;

/**
 * This class designed to served as converting malloc&free calls
 * to corresponding instructions.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RaiseAllocations implements ModulePass
{
    private Function mallocFunc;
    private Function freeFunc;
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
            argTys.add(Type.Int64Ty);
            FunctionType mallocType = FunctionType.get(PointerType.getUnqual(
                    Type.Int8Ty), argTys, false);

            // Chck to see if we got the expected malloc
            if (!tyWeHave.equals(mallocType))
            {
                // Check to see if the prototype is wrong, giving us sbyte*(uint) * malloc
                // This handles the common declaration of: 'void *malloc(unsigned);'
                ArrayList<Type> argTys2 = new ArrayList<>();
                argTys2.add(Type.Int32Ty);
                FunctionType mallocType2 = FunctionType.get(PointerType.getUnqual(
                        Type.Int8Ty), argTys2, false);
                if (!tyWeHave.equals(mallocType2))
                {
                    // Check to see if the prototype is missing, giving us
                    // sbyte*(...) * malloc
                    // This handles the common declaration of: 'void *malloc();'
                    FunctionType mallocType3 = FunctionType.get(
                            PointerType.getUnqual(Type.Int8Ty),
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
            argTys.add(PointerType.getUnqual(Type.Int8Ty));
            FunctionType freeType1 = FunctionType.get(Type.VoidTy,
                    argTys, false);
            if (!tyWeHave.equals(freeType1))
            {
                // Check to see if the prototype was forgotten, giving us
                // void (...) * free
                // This handles the common forward declaration of: 'void free();'

                FunctionType freeType2 = FunctionType.get(Type.VoidTy,
                        new ArrayList<>(), true);
                if (!tyWeHave.equals(freeType2))
                {
                    // One last try, check to see if we can find free as
                    // int (...)* free.  This handles the case where NOTHING was declared.
                    FunctionType freeType3 = FunctionType.get(Type.Int64Ty,
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
        return false;
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
