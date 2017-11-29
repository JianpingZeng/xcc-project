package backend.analysis;
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

import backend.type.Type;
import backend.value.BasicBlock;
import backend.value.Constant;
import backend.value.ConstantExpr;
import backend.value.ConstantInt;
import tools.Pair;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SCEVMulExpr extends SCEVCommutativeExpr
{
    private SCEVMulExpr(ArrayList<SCEV> ops)
    {
        super(SCEVType.scMulExpr, ops);
    }

    public static SCEV get(ArrayList<SCEV> ops)
    {
        assert !ops.isEmpty() :"Cannot get empty mul!";

        groupByComplexity(ops);

        int idx = 0;
        if(ops.get(0) instanceof SCEVConstant)
        {
            SCEVConstant lhsConst = (SCEVConstant)ops.get(0);
            // c1 * (c2 + v) => c1*c2 + c1*v.
            if(ops.size() == 2)
            {
                if (ops.get(1) instanceof SCEVAddExpr)
                {
                    SCEVAddExpr add = (SCEVAddExpr)ops.get(1);
                    if (add.getNumOperands() == 2
                            && (add.getOperand(0) instanceof SCEVConstant))
                        return SCEVAddExpr.get(SCEVMulExpr.get(lhsConst, add.getOperand(0)),
                                SCEVMulExpr.get(lhsConst, add.getOperand(1)));
                }
            }

            ++idx;

            while (ops.get(idx) instanceof SCEVConstant)
            {
                SCEVConstant rhsConst = (SCEVConstant)ops.get(idx);

                // We found two constants, fold them together!
                Constant fold = ConstantExpr.getMul(lhsConst.getValue(), rhsConst.getValue());
                if (fold instanceof ConstantInt)
                {
                    ConstantInt ci = (ConstantInt)fold;
                    ops.set(0, SCEVConstant.get(ci));
                    ops.remove(idx);
                    if (ops.size() == 1) return ops.get(0);
                    lhsConst = (SCEVConstant)ops.get(0);
                }
                else
                    ++idx;
            }

            // If we are left with a constant one being multiplied, strip it off.
            if (((SCEVConstant)ops.get(0)).getValue().equalsInt(1))
            {
                ops.remove(0);
                --idx;
            }
            else if (((SCEVConstant)ops.get(0)).getValue().isNullValue())
            {
                // If we have a multiply of zero, it will always be zero.
                return ops.get(0);
            }
        }

        // Skip over the add expression until we get to a multiply.
        while (idx < ops.size() && ops.get(idx).getSCEVType().ordinal() < SCEVType.scMulExpr.ordinal())
        {
            idx++;
        }

        if (ops.size() == 1)
            return ops.get(0);

        // If there are mul operands inline them all into this expression.
        if (idx < ops.size())
        {
            boolean deletedMul = false;
            // If we have an mul, expand the mul operands onto the end of the operands
            // list.
            while (ops.get(idx) instanceof SCEVMulExpr)
            {
                SCEVMulExpr mul = (SCEVMulExpr)ops.get(idx);
                ops.addAll(mul.getOperands());
                ops.remove(idx);
                deletedMul = true;
            }

            // If we deleted at least one mul, we added operands to the end of the list,
            // and they are not necessarily sorted.  Recurse to resort and resimplify
            // any operands we just aquired.
            if (deletedMul)
            {
                return get(ops);
            }
        }

        // If there are any add recurrences in the operands list, see if any other
        // added values are loop invariant.  If so, we can fold them into the
        // recurrence.
        while (idx < ops.size() && ops.get(idx).getSCEVType().ordinal() < SCEVType.scAddExpr.ordinal())
            idx++;

        for (; idx < ops.size() && ops.get(idx) instanceof SCEVAddRecExpr; idx++)
        {
            // Scan all of the other operands to this mul and add them to the vector if
            // they are loop invariant w.r.t. the recurrence.
            ArrayList<SCEV> liOps = new ArrayList<>();

            SCEVAddRecExpr addRec = (SCEVAddRecExpr)ops.get(idx);
            for (int i = 0, e = ops.size(); i < e; i++)
            {
                if (ops.get(i).isLoopInvariant(addRec.getLoop()))
                {
                    liOps.add(ops.get(i));
                    ops.remove(i);
                    i--;
                    e--;
                }
            }

            // If we found some loop invariants, fold them into the recurrence.
            if (!liOps.isEmpty())
            {
                ArrayList<SCEV> newOps = new ArrayList<>(addRec.getNumOperands());
                if (liOps.size() == 1)
                {
                    SCEV scale = liOps.get(0);
                    for (int i = 0, e = addRec.getNumOperands(); i < e; i++)
                    {
                        newOps.add(SCEVMulExpr.get(scale, addRec.getOperand(i)));
                    }
                }
                else
                {
                    for (int i = 0, e = addRec.getNumOperands(); i < e; i++)
                    {
                        ArrayList<SCEV> mulOps = new ArrayList<>();
                        mulOps.addAll(liOps);
                        mulOps.add(addRec.getOperand(i));
                        newOps.add(SCEVMulExpr.get(mulOps));
                    }
                }

                SCEV newRec = SCEVAddRecExpr.get(newOps, addRec.getLoop());

                // If all of the other operands were loop invariant, we are done.
                if (ops.size() == 1) return newRec;

                // Otherwise, multiply the folded AddRec by the non-liv parts.
                for (int i = 0; ; i++)
                {
                    if (ops.get(i).equals(addRec))
                    {
                        ops.set(i, newRec);
                        break;
                    }
                }
                return SCEVMulExpr.get(ops);
            }

            // Okay, if there weren't any loop invariants to be folded, check to see if
            // there are multiple AddRec's with the same loop induction variable being
            // multiplied together.  If so, we can fold them.
            for (int i = idx + 1; i < ops.size() && ops.get(i) instanceof  SCEVAddRecExpr; i++)
            {
                if (i != idx)
                {
                    SCEVAddRecExpr otherAddRec = (SCEVAddRecExpr)ops.get(i);;
                    if (addRec.getLoop().equals(otherAddRec.getLoop()))
                    {
                        // F*G -> {A, +, B} * {C, + D} --> {A*C, +, F*D + G*B + B*D}.
                        SCEVAddRecExpr f = addRec, g = otherAddRec;
                        SCEV newStart = SCEVMulExpr.get(f.getStart(), g.getStart());

                        SCEV b = f.getStepRecurrence();
                        SCEV d = g.getStepRecurrence();
                        SCEV newStep = SCEVAddExpr.get(SCEVMulExpr.get(f, d),
                                SCEVMulExpr.get(g, b), SCEVMulExpr.get(b, d));
                        SCEV newAddRec = SCEVAddRecExpr.get(newStart, newStep, f.getLoop());

                        if (ops.size() == 2) return newAddRec;

                        ops.remove(idx);
                        ops.remove(i - 1);
                        ops.add(newAddRec);
                        return SCEVMulExpr.get(ops);
                    }
                }
            }
        }

        ArrayList<SCEV> scevOps = new ArrayList<>();
        scevOps.addAll(ops);
        Pair<SCEVType, ArrayList<SCEV>> key = Pair.get(SCEVType.scMulExpr, scevOps);
        if (!scevCommExprsMap.containsKey(key))
        {
            SCEVCommutativeExpr result = new SCEVMulExpr(ops);
            scevCommExprsMap.put(key, result);
            return result;
        }

        return scevCommExprsMap.get(key);
    }

    public static SCEV get(SCEV lhs, SCEV rhs)
    {
        ArrayList<SCEV> res = new ArrayList<>();
        res.add(lhs);
        res.add(rhs);
        return get(res);
    }

    @Override
    public String getOperationString()
    {
        return "*";
    }

    @Override
    public Type getType()
    {
        return null;
    }

    @Override
    public boolean dominates(BasicBlock bb, DomTree dt)
    {
        // TODO: 17-7-1
        return false;
    }

    @Override
    public void print(PrintStream os)
    {

    }
}
