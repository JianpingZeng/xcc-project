package backend.transform.scalars;
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
    public static boolean constantFoldTerminator(BasicBlock bb)
    {
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
