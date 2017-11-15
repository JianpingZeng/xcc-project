package backend.transform.utils;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import backend.value.BasicBlock;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.ICmpInst;
import backend.value.Instruction.SwitchInst;
import backend.value.Instruction.TerminatorInst;
import backend.value.Operator;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.CmpInst.Predicate;
import backend.value.Value.UndefValue;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

/**
 * Performs some local constant folding optimizaiton.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ConstantFolder
{
    /**
     * If a terminator instruction is predicated on a constant value, convert
     * it to an unconditional branch to constant destination.
     * @param bb
     * @return  Return true when folding successfully, otherwise return false.
     */
    public static boolean constantFoldTerminator(BasicBlock bb)
    {
        TerminatorInst ti = bb.getTerminator();
        if (ti == null)
            return false;

        if (ti instanceof BranchInst)
        {
            BranchInst bi = (BranchInst)ti;
            // If this is a conditional branch, we folds the target of
            // this branch to the constant destination.
            if (bi.isUnconditional())
                return false;

            BasicBlock dest1 = bi.getSuccessor(0);
            BasicBlock dest2 = bi.getSuccessor(1);

            if (bi.getCondition() instanceof ConstantInt)
            {
                ConstantInt ci = (ConstantInt) bi.getCondition();
                BasicBlock destination = ci.getZExtValue() != 0 ? dest1 : dest2;
                BasicBlock oldDest = ci.getZExtValue() != 0 ? dest2 : dest1;

                assert bi.getParent() != null :
                        "Terminator not inserted into basic block";
                oldDest.removePredecessor(bi.getParent());

                // Set the unconditional destination, and change the inst to be
                // an unconditional branch.
                bi.setUnconditionalDest(destination);
                return true;
            }
            else if (dest1.equals(dest2))
            {
                // This branch matches something like this:
                //     br bool %cond, label %dest1, label %dest1
                // and changes it into:  br label %dest1
                assert bi.getParent() != null;
                dest1.removePredecessor(bi.getParent());
                bi.setUnconditionalDest(dest1);
            }
        }
        else if (ti instanceof SwitchInst)
        {
            SwitchInst si = (SwitchInst)ti;
            if (si.getCondition() instanceof ConstantInt)
            {
                ConstantInt ci = (ConstantInt)si.getCondition();
                BasicBlock theOnlyDest = si.getSuccessor(0);    // The default dest.
                BasicBlock defaultDest = theOnlyDest;
                assert theOnlyDest.equals(si.getDefaultBlock())
                        : "Default destination is not successor #0?";

                for (int i = 1, e = si.getNumOfSuccessors(); i != e; i++)
                {
                    if (si.getSuccessorValue(i).equals(ci))
                    {
                        theOnlyDest = si.getSuccessor(i);
                        break;
                    }

                    // Check to see if this branch is going to the same as default
                    // destination. If so, elimiate the default block.
                    if (si.getSuccessor(i).equals(defaultDest))
                    {
                        defaultDest.removePredecessor(si.getParent());
                        si.removeCase(i);
                        --i;
                        --e;
                        continue;
                    }

                    if (!si.getSuccessor(i).equals(theOnlyDest))
                        theOnlyDest = null;
                }

                if (ci != null && theOnlyDest == null)
                {
                    theOnlyDest = defaultDest;
                }

                if (theOnlyDest != null)
                {
                    // Create a new branch instruction inserted after switchInst.
                    new BranchInst(theOnlyDest, si);
                    BasicBlock curBB = si.getParent();

                    // Remove entries from PHI nodes which we no longer branch to.
                    for (int i = 0, e = si.getNumOfSuccessors(); i != e; i++)
                    {
                        BasicBlock succ = si.getSuccessor(i);
                        if (succ.equals(theOnlyDest))
                            theOnlyDest = null;
                        else
                            succ.removePredecessor(curBB);
                    }

                    // Remove the switchInst from its basic block.
                    si.eraseFromParent();
                    return true;
                }
                else if (si.getNumOfSuccessors() == 2)
                {
                    // Otherwise, we can fold this switch into a conditional branch
                    // instruction if it has only one non-default destination.
                    Value cond = new ICmpInst(Predicate.ICMP_EQ,
                            si.getCondition(), si.getSuccessorValue(1),
                            "cond", si);
                    new BranchInst(si.getSuccessor(1), si.getSuccessor(0), cond, si);

                    // Delete the old switch.
                    si.eraseFromParent();
                    return true;
                }
            }
        }
        return false;
    }

    public static Constant constantFoldCastInstruction(
            Operator opocde,
            Constant val,
            Type destTy)
    {
        return null;
    }

    public static Constant constantFoldBinaryInstruction(
            Operator opcode,
            Constant op1,
            Constant op2)
    {
        return null;
    }

    public static Constant constantFoldCompareInstruction(
            Predicate predicate,
            Constant c1,
            Constant c2)
    {
        return null;
    }

    public static Constant constantFoldGetElementPtr(
            Constant c,
            TIntArrayList idxs)
    {
        return null;
    }

    /**
     * This method attempts to fold specified expression to constant.
     * If successfully, the constant value returned, otherwise, null returned.
     *
     * @param inst The instruction to be folded.
     * @return The constant returned if successfully, otherwise, null returned.
     */
    public static Constant constantFoldInstruction(Instruction inst)
    {
        // handle phi nodes here
        if (inst instanceof Instruction.PhiNode)
        {
            Constant commonValue = null;
            Instruction.PhiNode PH = (Instruction.PhiNode) inst;
            for (int i = 0; i < PH.getNumberIncomingValues(); i++)
            {
                Value incomingValue = PH.getIncomingValue(i);

                // if the incoming value is undef and then skip it.
                // Note that while we could skip the valeu if th is equal to the
                // phi node itself because that would break the rules that constant
                // folding only applied if all reservedOperands are constant.
                if (incomingValue instanceof UndefValue)
                    continue;

                // get rid of it, if the incoming value is not a constant
                if (!(incomingValue instanceof Constant))
                    return null;

                Constant constant = (Constant) incomingValue;
                // folds the phi's reservedOperands
                if (commonValue != null && constant != commonValue)
                    return null;
                commonValue = constant;
            }

            return commonValue != null ?
                    commonValue :
                    UndefValue.get(PH.getType());
        }

        // handles other instruction here.
        // inst.accept(this);
        return null;
    }

    public static Constant constantFoldFP(FunctionalInterface intf, double v, Type ty)
    {
        return null;
    }

    public static Constant constantFoldCall(Function f, ArrayList<Constant> operands)
    {
        // TODO constant folding on call instruction.
        String name = f.getName();
        Type ty = f.getReturnType();

        if (operands.size() == 1)
        {
            if (operands.get(0) instanceof ConstantFP)
            {
                ConstantFP op = (ConstantFP)operands.get(0);
                double v = op.getValue();
                switch (name)
                {
                    case "acos":
                        return ConstantFP.get(ty, Math.acos(v));
                    case "asin":
                        return ConstantFP.get(ty, Math.asin(v));
                    case "atan":
                        return ConstantFP.get(ty, Math.atan(v));
                    case "ceil":
                        return ConstantFP.get(ty, Math.ceil(v));
                    case "cos":
                        return ConstantFP.get(ty, Math.cos(v));
                    case "cosh":
                        return ConstantFP.get(ty, Math.cosh(v));
                    case "exp":
                        return ConstantFP.get(ty, Math.exp(v));
                    case "fabs":
                        return ConstantFP.get(ty, Math.abs(v));
                    case "log":
                        return ConstantFP.get(ty, Math.log(v));
                    case "log10":
                        return ConstantFP.get(ty, Math.log10(v));
                    case "llvm.sqrt.f32":
                    case "llvm.sqrt.f64":
                        if (v >= -0.0)
                            return ConstantFP.get(ty, Math.sqrt(v));
                        else
                            return ConstantFP.get(ty, 0.0);
                    case "sin":
                        return ConstantFP.get(ty, Math.sin(v));
                    case "sinh":
                        return ConstantFP.get(ty, Math.sinh(v));
                    case "sqrt":
                        if (v >= 0.0)
                            return ConstantFP.get(ty, Math.sqrt(v));
                        break;
                    case "tan":
                        return ConstantFP.get(ty, Math.tan(v));
                    case "tanh":
                        return ConstantFP.get(ty, Math.tanh(v));
                    default:
                        break;
                }
            }
            else if (operands.size() == 2)
            {
                if (operands.get(0) instanceof ConstantFP)
                {
                    ConstantFP op1 = (ConstantFP)operands.get(0);
                    if (operands.get(1) instanceof ConstantFP)
                    {
                        ConstantFP op2 = (ConstantFP)operands.get(1);
                        double op1Val = op1.getValue(), op2Val = op2.getValue();

                        if (name.equals("pow"))
                        {
                            double res = Math.pow(op1Val, op2Val);
                            return ConstantFP.get(ty, res);
                        }
                        else if (name.equals("fmod"))
                        {
                            // TODO fmod intrisinc function.
                            return null;
                        }
                        else if (name.equals("atan2"))
                        {
                            return ConstantFP.get(ty, Math.atan2(op1Val, op2Val));
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean canConstantFoldCallTo(Function f)
    {
        String name = f.getName();
        switch (name)
        {
            case "acos":
            case "asin":
            case "atan":
            case "ceil":
            case "cos":
            case "cosh":
            case "exp":
            case "fabs":
            case "log":
            case "log10":
            case "sin":
            case "sinh":
            case "sqrt":
            case "tan":
            case "tanh":
                return true;
            default:
                return false;
        }
    }
}