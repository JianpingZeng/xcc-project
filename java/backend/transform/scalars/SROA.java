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

package backend.transform.scalars;

import backend.analysis.DomTreeInfo;
import backend.analysis.DominanceFrontier;
import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.support.IntStatistic;
import backend.support.LLVMContext;
import backend.transform.utils.PromoteMemToReg;
import backend.type.ArrayType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.AllocaInst;
import backend.value.Instruction.GetElementPtrInst;

import java.util.ArrayList;
import java.util.Stack;

import static backend.transform.utils.PromoteMemToReg.isAllocaPromotable;

/**
 * This file defines a class responsible for performing a well known intra-procedural
 * optimization -- Scalar Replacement of Aggregate. It breaks all members of
 * aggregate into single scalar type variable declaration. For example
 * <pre>
 * struct Agg
 * {
 *     int a;
 *     int b;
 * };
 *
 * int main()
 * {
 *     struct Agg agg = {.a = 1, .b = 2};
 *     agg.a = agg.b + 1;
 *     return 0;
 * }
 * will be transformed into following form.
 * int main()
 * {
 *     int agg_a = 1;
 *     int agg_b = 2;
 *     agg_a = agg_b + 1;
 *     return 0;
 * }
 * </pre>
 * After performed, suitable aggregates would in favour of subsequent optimization.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SROA implements FunctionPass
{
    public static final IntStatistic NumReplaced =
            new IntStatistic("scalarrepl", "Number of allocas broken up");
    public static final IntStatistic NumPromoted =
            new IntStatistic("scalarrepl", "Number of allocas promoted");

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
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(DominanceFrontier.class);
        au.addRequired(DomTreeInfo.class);
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        boolean madeChange = false;
        do
        {
            boolean localChange = promotion(f);
            if (!localChange)
                break;
            madeChange = true;
            localChange = performReplacement(f);
            if (!localChange)
                break;
        }while (true);

        return madeChange;
    }

    @Override
    public String getPassName()
    {
        return "Scalar Replacement of Aggregate";
    }

    private boolean promotion(Function f)
    {
        // Collects all of AllocaInst from entry block.
        BasicBlock entryBB = f.getEntryBlock();

        DomTreeInfo dt = (DomTreeInfo) getAnalysisToUpDate(DomTreeInfo.class);
        DominanceFrontier df = (DominanceFrontier) getAnalysisToUpDate(DominanceFrontier.class);

        boolean changed = false;
        ArrayList<AllocaInst> allocas = new ArrayList<>();
        while (true)
        {
            allocas.clear();
            for (Instruction inst : entryBB)
            {
                if (inst instanceof AllocaInst && isAllocaPromotable((AllocaInst)inst))
                {
                    allocas.add((AllocaInst) inst);
                }
            }

            if (allocas.isEmpty())
                break;

            PromoteMemToReg.promoteMemToReg(allocas, dt, df);
            NumPromoted.add(allocas.size());
            changed = true;
        }
        return changed;
    }

    private boolean performReplacement(Function f)
    {
        // Uses worklist algorithm.
        Stack<AllocaInst> list = new Stack<>();
        for (Instruction inst : f.getEntryBlock())
        {
            if (inst instanceof AllocaInst)
                list.push((AllocaInst)inst);
        }

        boolean changed = false;
        while (!list.isEmpty())
        {
            AllocaInst ai = list.pop();

            // Skip it when AllocaInst is Array allocation or
            // it's allocated type is not both ArrayType and StructType.
            if (ai.isArrayAllocation() ||
                    (!ai.getAllocatedType().isArrayType() &&
                    !ai.getAllocatedType().isStructType()))
            {
                continue;
            }

            if (ai.getAllocatedType().isStructType())
            {
                if (!isSafeStructAllocaToReplace(ai))
                    continue;
            }
            else
            {
                if (!isSafeArrayAllocaToReplace(ai))
                    continue;
            }

            changed =true;

            // So the ai is safe to replaced by scalar variable.
            ArrayList<AllocaInst> scalars = new ArrayList<>();
            if (ai.getAllocatedType().isStructType())
            {
                StructType st = (StructType) ai.getAllocatedType();
                int i = 0;
                for (int e = st.getNumOfElements(); i != e; i++)
                {
                    Type ty = st.getContainedType(i);
                    AllocaInst tmp = new AllocaInst(ty, ai.getName()+"."+ i, ai);
                    scalars.add(tmp);
                    list.push(tmp);
                }
            }
            else
            {
                ArrayType at = (ArrayType) ai.getAllocatedType();
                Type eltType = at.getElementType();
                for (long i = 0, e = at.getNumElements(); i != e; i++)
                {
                    AllocaInst tmp = new AllocaInst(eltType, ai.getName()+"."+ i, ai);
                    scalars.add(tmp);

                    // Recursive handle.
                    list.push(tmp);
                }
            }

            for (Use u : ai.getUseList())
            {
                User user = u.getUser();
                if (user instanceof GetElementPtrInst)
                {
                    GetElementPtrInst gep = (GetElementPtrInst)user;
                    // We now know that the GEP is of the form: GEP <ptr>, 0, <cst>
                    long idx = ((ConstantInt)gep.operand(2)).getZExtValue();
                    Value repValue;
                    AllocaInst allocasToUse = scalars.get((int) idx);
                    if (gep.getNumOfOperands() == 3)
                    {
                        repValue = allocasToUse;
                    }
                    else
                    {
                        String name = gep.getName();
                        ArrayList<Value> args = new ArrayList<>();
                        args.add(ConstantInt.getNullValue(LLVMContext.Int64Ty));
                        for (int i = 3, e = gep.getNumOfOperands(); i != e; i++)
                            args.add(gep.operand(i));

                        String newName = name + ".repl";
                        repValue = new GetElementPtrInst(allocasToUse, args, newName, gep);
                    }

                    gep.replaceAllUsesWith(repValue);
                    gep.eraseFromParent();
                }
                else
                {
                    assert false:"Supported uses of AllocaInst";
                }
            }

            ai.eraseFromParent();
            NumReplaced.inc();
        }
        return changed;
    }

    private boolean isSafeStructAllocaToReplace(AllocaInst ai)
    {
        if (!ai.getAllocatedType().isStructType())
            return false;

        for (Use u : ai.getUseList())
        {
            if (!isSafeUseOfAlloca(u.getUser()))
                return true;

            GetElementPtrInst gep = (GetElementPtrInst)u.getUser();
            if (gep != null && gep.getNumOfOperands() == 3 && !isSafeElementUse(gep))
                return false;
        }
        return true;
    }

    private boolean isSafeUseOfAlloca(User u)
    {
        if (!(u instanceof GetElementPtrInst))
            return false;

        GetElementPtrInst gep = (GetElementPtrInst)u;
        if (gep.getNumOfOperands() <= 2
                || !gep.operand(1).equals(ConstantInt.getNullValue(LLVMContext.Int64Ty))
                || (!(gep.operand(2) instanceof ConstantInt)
                && !(gep.operand(2) instanceof ConstantExpr)))
            return false;

        return true;
    }

    private boolean isSafeElementUse(User inst)
    {
        // Check all of uses of this GEP instruction.
        for (Use u : inst.getUseList())
        {
            User user = u.getUser();
            if (user instanceof Instruction)
            {
                Instruction in = (Instruction)user;
                switch (in.getOpcode())
                {
                    case Load:
                        break;
                    case Store:
                        // Invalid if the value stored into target address is gep.
                        // It should be second operand.
                        if (user.operand(0).equals(inst))
                            return false;
                        break;
                    case GetElementPtr:
                        GetElementPtrInst gep = (GetElementPtrInst) user;
                        if (gep.getNumOfOperands() >= 2 && (!gep.operand(1).isConstant())
                                || !((Constant) gep.operand(1)).isNullValue())
                            return false;

                        if (!isSafeElementUse(gep))
                            return false;
                        break;
                    default:
                        return false;
                }
            }
        }
        return true;
    }

    private boolean isSafeArrayAllocaToReplace(AllocaInst ai)
    {
        if (!ai.getAllocatedType().isArrayType())
            return false;

        long numElts = ((ArrayType)ai.getAllocatedType()).getNumElements();
        for (Use u : ai.getUseList())
        {
            if (!isSafeUseOfAlloca(u.getUser()))
                return true;

            GetElementPtrInst gep = (GetElementPtrInst)u.getUser();
            if (gep != null && gep.getNumOfOperands() >= 3)
            {
                Value op = gep.operand(2);
                if (op.isConstant() && ((ConstantInt)op).getSExtValue() >= numElts)
                    return false;

                if (!isSafeElementUse(gep))
                    return false;
            }
        }
        return true;
    }
}
