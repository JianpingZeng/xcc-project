package backend.transform.utils;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

import backend.support.LLVMContext;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.CmpInst.Predicate;
import backend.value.Instruction.ICmpInst;
import backend.value.Instruction.SwitchInst;
import backend.value.Instruction.TerminatorInst;
import backend.value.Value.UndefValue;
import gnu.trove.list.array.TIntArrayList;
import tools.APFloat;
import tools.APInt;
import tools.OutParamWrapper;
import tools.Util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

import static backend.intrinsic.Intrinsic.ID.stacksave;

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

                Util.assertion(bi.getParent() != null, "Terminator not inserted into basic block");

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
                Util.assertion( bi.getParent() != null);
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
                Util.assertion(theOnlyDest.equals(si.getDefaultBlock()),  "Default destination is not successor #0?");

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

    /**
     * Constant folding to avoid duplicate computation.
     * @param opc
     * @param val
     * @param destTy
     * @return
     */
    public static Constant constantFoldCastInstruction(
            Operator opc,
            Constant val,
            Type destTy)
    {
        if (val instanceof UndefValue)
        {
            // zext(undef) ==> 0
            if (opc == Operator.ZExt || opc == Operator.SExt ||
                    opc == Operator.UIToFP || opc == Operator.SIToFP)
                return Constant.getNullValue(destTy);
            return UndefValue.get(destTy);
        }

        if (val.getType().equals(LLVMContext.FP128Ty) ||
                destTy.equals(LLVMContext.FP128Ty))
            return null;

        switch (opc)
        {
            case FPTrunc:
            case FPExt:
                if (val instanceof ConstantFP)
                {
                    ConstantFP fp = (ConstantFP)val;
                    OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);
                    APFloat v = fp.getValueAPF();
                    v.convert(destTy.equals(LLVMContext.FloatTy) ? APFloat.IEEEsingle :
                                destTy.equals(LLVMContext.DoubleTy) ? APFloat.IEEEdouble:
                                destTy.equals(LLVMContext.X86_FP80Ty) ? APFloat.x87DoubleExtended:
                                destTy.equals(LLVMContext.FP128Ty) ? APFloat.IEEEquad :
                                APFloat.Bogus, APFloat.RoundingMode.rmNearestTiesToEven,
                                ignored);
                    return ConstantFP.get(v);
                }
                return null;    // can't fold.
        }
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
        if (c1 instanceof ConstantInt && c2 instanceof ConstantInt)
        {
            APInt v1 = ((ConstantInt)c1).getValue();
            APInt v2 = ((ConstantInt)c2).getValue();
            switch (predicate)
            {
                case ICMP_EQ:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.eq(v2)?1:0);
                case ICMP_NE:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.ne(v2)?1:0);
                case ICMP_SLT:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.slt(v2)?1:0);
                case ICMP_SGT:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.sgt(v2)?1:0);
                case ICMP_SLE:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.sle(v2)?1:0);
                case ICMP_SGE:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.sge(v2)?1:0);
                case ICMP_ULT:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.ult(v2)?1:0);
                case ICMP_UGT:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.ugt(v2)?1:0);
                case ICMP_ULE:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.ule(v2)?1:0);
                case ICMP_UGE:
                    return ConstantInt.get(LLVMContext.Int1Ty, v1.uge(v2)?1:0);
            }
        }
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
                ConstantFP op = (ConstantFP) operands.get(0);
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

    public static Constant createICmp(Predicate pred, Constant lhs, Constant rhs)
    {
        return ConstantExpr.getCompare(pred, lhs, rhs);
    }

    /**
     * Delete the specified dead basic block.
     * @param bb    A dead basic block to be removed from CFG.
     */
    public static void deleteDeadBlock(BasicBlock bb)
    {
        LinkedList<Instruction> list = bb.getInstList();
        while (!list.isEmpty())
        {
            Instruction inst = list.getFirst();
            list.removeFirst();
            if (inst == null)
                continue;
            if (inst.hasOneUses())
                inst.replaceAllUsesWith(UndefValue.get(inst.getType()));
            inst.eraseFromParent();
        }
    }

    /**
     * Delete the dead instruction and it's operands.
     * @param val
     */
    public static void recursivelyDeleteTriviallyDeadInstructions(Value val)
    {
        if (!(val instanceof Instruction)) return;
        Instruction inst = (Instruction)val;
        if (!inst.isUseEmpty() || !isInstructionTriviallyDead(inst))
            return;

        Stack<Instruction> worklist = new Stack<>();
        worklist.push(inst);
        while (!worklist.isEmpty())
        {
            inst = worklist.pop();
            inst.eraseFromParent();

            for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
            {
                val = inst.operand(0);
                if (!val.isUseEmpty()) continue;
                if (!(val instanceof Instruction)) continue;
                if (isInstructionTriviallyDead((Instruction) val))
                    worklist.push((Instruction) val);
            }
        }
    }

    /**
     * Return true if the value produced by the specified instruction is not used.
     * @param inst
     * @return
     */
    private static boolean isInstructionTriviallyDead(Instruction inst)
    {
        if (!inst.isUseEmpty() || inst instanceof TerminatorInst)
            return false;

        if (!inst.mayHasSideEffects())
            return true;
        if (inst instanceof IntrinsicInst)
        {
            IntrinsicInst ii = (IntrinsicInst)inst;
            if (ii.getIntrinsicID() == stacksave)
                return true;
        }
        return false;
    }
}
