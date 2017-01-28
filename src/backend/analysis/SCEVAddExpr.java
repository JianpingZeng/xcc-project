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
import backend.value.Constant;
import backend.value.ConstantExpr;
import backend.value.ConstantInt;
import tools.Pair;

import java.io.PrintStream;
import java.util.ArrayList;

import static backend.analysis.SCEV.SCEVType.scAddRecExpr;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SCEVAddExpr extends SCEVCommutativeExpr
{
    private ArrayList<SCEV> ops;

    private SCEVAddExpr(ArrayList<SCEV> ops)
    {
        super(SCEVType.scAddExpr, ops);
    }

    public static SCEV get(ArrayList<SCEV> ops)
    {
        assert !ops.isEmpty() : "Cannot get empty add!";

        groupByComplexity(ops);

        // If there are any constants, fold them together.
        int idx = 0;
        if (ops.get(0) instanceof SCEVConstant)
        {
            SCEVConstant lhsConst = (SCEVConstant) ops.get(0);
            ++idx;
            assert idx < ops.size();

            while (ops.get(idx) instanceof SCEVConstant)
            {
                SCEVConstant rhsConst = (SCEVConstant) ops.get(idx);

                // We found two constants, fold them together!
                Constant fold = ConstantExpr
                        .getAdd(lhsConst.getValue(), rhsConst.getValue());
                if (fold instanceof ConstantInt)
                {
                    ConstantInt ci = (ConstantInt) fold;
                    ops.set(0, SCEVConstant.get(ci));
                    ops.remove(idx);
                    if (ops.size() == 1)
                        return ops.get(0);
                    lhsConst = (SCEVConstant) ops.get(0);
                }
                else
                    ++idx;
            }

            // If we are left with a constant one being multiplied, strip it off.
            if (((SCEVConstant) ops.get(0)).getValue().isNullValue())
            {
                ops.remove(0);
                --idx;
            }
        }

        if (ops.size() == 1)
            return ops.get(0);

        // Okay, check to see if the same value occurs in the operand list twice.  If
        // so, merge them together into an multiply expression.  Since we sorted the
        // list, these values are required to be adjacent.
        Type Ty = ops.get(0).getType();
        for (int i = 0, e = ops.size() - 1; i != e; ++i)
        {
            if (ops.get(i) == ops.get(i + 1))
            {      //  X + Y + Y  -->  X + Y*2
                // Found a match, merge the two values into a multiply, and add any
                // remaining values to the result.
                SCEV Two = ScalarEvolution.getIntegerSCEV(2, Ty);
                SCEV mul = SCEVMulExpr.get(ops.get(i), Two);
                if (ops.size() == 2)
                    return mul;
                for (int j = i; j < i + 2; j++)
                    ops.remove(j);

                ops.add(mul);
                return SCEVAddExpr.get(ops);
            }
        }

        // Okay, now we know the first non-constant operand.  If there are add
        // operands they would be next.
        if (idx < ops.size())
        {
            boolean deletedMul = false;
            // If we have an mul, expand the mul operands onto the end of the operands
            // list.
            while (ops.get(idx) instanceof SCEVAddExpr)
            {
                SCEVMulExpr mul = (SCEVMulExpr) ops.get(idx);
                ops.addAll(mul.getOperands());
                ops.remove(idx);
                deletedMul = true;
            }

            // If we deleted at least one add, we added operands to the end of the list,
            // and they are not necessarily sorted.  Recurse to resort and resimplify
            // any operands we just aquired.
            if (deletedMul)
            {
                return get(ops);
            }
        }

        // Skip over the add expression until we get to a multiply.
        while (idx < ops.size()
                && ops.get(idx).getSCEVType().ordinal() < SCEVType.scMulExpr
                .ordinal())
            idx++;

        // If we are adding something to a multiply expression, make sure the
        // something is not already an operand of the multiply.  If so, merge it into
        // the multiply.
        for (; idx < ops.size() && ops.get(idx) instanceof SCEVMulExpr; idx++)
        {
            SCEVMulExpr mul = (SCEVMulExpr) ops.get(idx);
            for (int mulOp = 0, e = mul.getNumOperands(); mulOp < e; mulOp++)
            {
                SCEV mulOpSCEV = mul.getOperand(mulOp);

                for (int i = 0, sz = ops.size(); i < sz; i++)
                {
                    if (mulOpSCEV.equals(ops.get(i))
                            && !(mulOpSCEV instanceof SCEVConstant))
                    {
                        // Fold W + X + (X * Y * Z)  -->  W + (X * ((Y*Z)+1))
                        SCEV innerMul = mul.getOperand(mulOp == 0 ? 1 : 0);
                        if (mul.getNumOperands() != 2)
                        {
                            // If the multiply has more than two operands, we must get the
                            // Y*Z term.
                            ArrayList<SCEV> mulOps = new ArrayList<>();
                            mulOps.addAll(mul.getOperands());
                            mulOps.remove(mulOp);
                            innerMul = SCEVMulExpr.get(mulOps);
                        }
                        SCEV one = ScalarEvolution.getIntegerSCEV(1, Ty);
                        SCEV addOne = SCEVAddExpr.get(innerMul, one);
                        SCEV outerMul = SCEVMulExpr.get(addOne, ops.get(i));
                        if (ops.size() == 2)
                            return outerMul;
                        if (i < idx)
                        {
                            ops.remove(i);
                            ops.remove(idx - 1);
                        }
                        else
                        {
                            ops.remove(idx);
                            ops.remove(i - 1);
                        }
                        ops.add(outerMul);
                        return SCEVAddExpr.get(ops);
                    }
                }

                // Check this multiply against other multiplies being added together.
                for (int otherMulIdx = idx + 1; otherMulIdx < ops.size() && ops
                        .get(otherMulIdx) instanceof SCEVMulExpr; otherMulIdx++)
                {
                    SCEVMulExpr otherMul = (SCEVMulExpr) ops.get(otherMulIdx);
                    // If MulOp occurs in otherMul, we can fold the two multiplies
                    // together.
                    for (int oMulOp = 0, sz = otherMul.getNumOperands();
                         oMulOp != sz; ++oMulOp)
                    {
                        if (otherMul.getOperand(oMulOp) == mulOpSCEV)
                        {
                            // Fold X + (A*B*C) + (A*D*E) --> X + (A*(B*C+D*E))
                            SCEV innerMul1 = mul.getOperand(mulOp == 0 ? 1 : 0);
                            if (mul.getNumOperands() != 2)
                            {
                                ArrayList<SCEV> mulOps = new ArrayList<>();
                                mulOps.addAll(mul.getOperands());
                                mulOps.remove(mulOp);
                                innerMul1 = SCEVMulExpr.get(mulOps);
                            }
                            SCEV innerMul2 = otherMul
                                    .getOperand(oMulOp == 0 ? 1 : 0);
                            if (otherMul.getNumOperands() != 2)
                            {
                                ArrayList<SCEV> mulOps = new ArrayList<>();
                                mulOps.addAll(otherMul.getOperands());
                                mulOps.remove(oMulOp);
                                innerMul2 = SCEVMulExpr.get(mulOps);
                            }
                            SCEV innerMulSum = SCEVAddExpr
                                    .get(innerMul1, innerMul2);
                            SCEV outerMul = SCEVMulExpr
                                    .get(mulOpSCEV, innerMulSum);
                            if (ops.size() == 2)
                                return outerMul;
                            ops.remove(idx);
                            ops.remove(otherMulIdx - 1);
                            ops.add(outerMul);
                            return SCEVAddExpr.get(ops);
                        }
                    }
                }
            }
        }

        // If there are any add recurrences in the operands list, see if any other
        // added values are loop invariant.  If so, we can fold them into the
        // recurrence.
        while (idx < ops.size()
                && ops.get(idx).getSCEVType().ordinal() < scAddRecExpr
                .ordinal())
            ++idx;

        // Scan over all recurrences, trying to fold loop invariants into them.
        for (;
             idx < ops.size() && ops.get(idx) instanceof SCEVAddRecExpr; ++idx)
        {
            // Scan all of the other operands to this add and add them to the vector if
            // they are loop invariant w.r.t. the recurrence.
            ArrayList<SCEV> liOps = new ArrayList<>();
            SCEVAddRecExpr addRec = (SCEVAddRecExpr) ops.get(idx);
            for (int i = 0, e = ops.size(); i != e; ++i)
                if (ops.get(i).isLoopInvariant(addRec.getLoop()))
                {
                    liOps.add(ops.get(i));
                    ops.remove(i);
                    --i;
                    --e;
                }

            // If we found some loop invariants, fold them into the recurrence.
            if (!liOps.isEmpty())
            {
                //  NLI + LI + { Start,+,Step}  -->  NLI + { LI+Start,+,Step }
                liOps.add(addRec.getStart());

                ArrayList<SCEV> AddRecOps = new ArrayList<>();
                AddRecOps.addAll(addRec.getOperands());
                AddRecOps.set(0, SCEVAddExpr.get(liOps));

                SCEV NewRec = SCEVAddRecExpr.get(AddRecOps, addRec.getLoop());
                // If all of the other operands were loop invariant, we are done.
                if (ops.size() == 1)
                    return NewRec;

                // Otherwise, add the folded addRec by the non-liv parts.
                for (int i = 0; ; ++i)
                    if (ops.get(i) == addRec)
                    {
                        ops.set(i, NewRec);
                        break;
                    }
                return SCEVAddExpr.get(ops);
            }

            // Okay, if there weren't any loop invariants to be folded, check to see if
            // there are multiple addRec's with the same loop induction variable being
            // added together.  If so, we can fold them.
            for (int otherIdx = idx + 1; otherIdx < ops.size() && ops
                    .get(otherIdx) instanceof SCEVAddRecExpr; ++otherIdx)
                if (otherIdx != idx)
                {
                    SCEVAddRecExpr otherAddRec = (SCEVAddRecExpr) ops
                            .get(otherIdx);
                    if (addRec.getLoop().equals(otherAddRec.getLoop()))
                    {
                        // Other + {A,+,B} + {C,+,D}  -->  Other + {A+C,+,B+D}
                        ArrayList<SCEV> newOps = new ArrayList<>();
                        newOps.addAll(addRec.getOperands());
                        for (int i = 0, e = otherAddRec.getNumOperands();
                             i != e; ++i)
                        {
                            if (i >= newOps.size())
                            {
                                newOps.addAll(otherAddRec.getOperands()
                                        .subList(i,
                                                otherAddRec.getNumOperands()));
                                break;
                            }
                            newOps.set(i, SCEVAddExpr.get(newOps.get(i),
                                    otherAddRec.getOperand(i)));
                        }
                        SCEV NewAddRec = SCEVAddRecExpr
                                .get(newOps, addRec.getLoop());

                        if (ops.size() == 2)
                            return NewAddRec;

                        ops.remove(idx);
                        ops.remove(otherIdx - 1);
                        ops.add(NewAddRec);
                        return SCEVAddExpr.get(ops);
                    }
                }

            // Otherwise couldn't fold anything into this recurrence.  Move onto the
            // next one.
        }
        ArrayList<SCEV> scevOps = new ArrayList<>();
        scevOps.addAll(ops);
        Pair<SCEVType, ArrayList<SCEV>> key = Pair
                .get(SCEVType.scMulExpr, scevOps);
        if (!scevCommExprsMap.containsKey(key))
        {
            SCEVCommutativeExpr result = new SCEVAddExpr(ops);
            scevCommExprsMap.put(key, result);
            return result;
        }

        return scevCommExprsMap.get(key);
    }

    public static SCEV get(SCEV first, SCEV second)
    {
        ArrayList<SCEV> list = new ArrayList<>();
        list.add(first);
        list.add(second);
        return get(list);
    }

    public static SCEV get(SCEV op0, SCEV op1, SCEV op2)
    {
        ArrayList<SCEV> list = new ArrayList<>();
        list.add(op0);
        list.add(op1);
        list.add(op2);
        return get(list);
    }

    @Override public String getOperationString()
    {
        return "+";
    }

    @Override public void print(PrintStream os)
    {

    }
}
