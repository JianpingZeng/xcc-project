package jlang.sema;
/*
 * Xlous C language CompilerInstance
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

import jlang.ast.Tree;
import jlang.ast.Tree.Expr;
import jlang.type.PointerType;
import jlang.type.QualType;
import tools.APSInt;
import tools.OutParamWrapper;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PointerExprEvaluator extends ExprEvaluatorBase<Boolean>
{
    private OutParamWrapper<LValue> result;
    public PointerExprEvaluator(OutParamWrapper<LValue> result, ASTContext ctx)
    {
        super(ctx);
        this.result = result;
    }

    private boolean success(final Expr e)
    {
        result.get().base = e;
        result.get().offset = 0;
        return true;
    }

    @Override
    protected Boolean success(APValue v, Expr e)
    {
        result.get().setFrom(v);
        return true;
    }

    @Override
    protected Boolean error(Expr expr)
    {
        return false;
    }
    @Override
    public Boolean visitBinaryExpr(Tree.BinaryExpr expr)
    {
        if (expr.getOpcode() != BinaryOperatorKind.BO_Add
                && expr.getOpcode() != BinaryOperatorKind.BO_Sub)
            return false;

        Expr pExpr = expr.getLHS();
        Expr iExpr = expr.getRHS();

        // let the expr of jlang.type pointer jlang.type reside at left side
        // (pointer) +- (rhs).
        if (iExpr.getType().isPointerType())
        {
            Expr temp = pExpr;
            pExpr = iExpr;
            iExpr = temp;
        }

        // If the pExpr is not pointer jlang.type, return false.
        if (!evaluatePointer(pExpr, result, context))
            return false;

        // So that, the pExpr must be pointer jlang.type
        APSInt offset = new APSInt();
        OutParamWrapper<APSInt> x = new OutParamWrapper<>(offset);
        if (!evaluateInteger(iExpr, x, context))
            return false;
        offset = x.get();

        // Reaching here, the binary operation is certainly consists of
        // Ptr +- int.
        long additionalOffset = offset.isSigned()?offset.getSExtValue()
                : offset.getZExtValue();

        // Compute the new offset in the appropriate width.
        QualType pointeeType = context.getAs(pExpr.getType(), PointerType.class).getPointeeType();
        long sizeOfPointee;

        // Explicitly handle GNU void* and function pointer arithmetic extensions.
        if (pointeeType.isVoidType() || pointeeType.isFunctionType())
            sizeOfPointee = 1;
        else
            sizeOfPointee = context.getTypeSize(pointeeType);

        if (expr.getOpcode() == BinaryOperatorKind.BO_Add)
            result.get().offset += additionalOffset*sizeOfPointee;
        else
            result.get().offset -= additionalOffset*sizeOfPointee;

        return true;
    }

    public Boolean visitUnaryExpr(Tree.UnaryExpr expr)
    {
        // just handle '&' operator.
        if (expr.getOpCode() == UnaryOperatorKind.UO_AddrOf)
        {
            return evaluateLValue(expr.getSubExpr(), result, context);
        }
        return false;
    }

    protected Boolean visitCastExpr(Tree.CastExpr expr)
    {
        final Expr subExp = expr.getSubExpr();
        switch (expr.getCastKind())
        {
            default:break;
            case CK_NoOp:
            case CK_BitCast:
                return visit(subExp);

            case CK_NullToPointer:
            {
                result.get().base = null;
                result.get().offset = 0;
                return true;
            }

            case CK_IntegralToPointer:
            {
                APValue value = new APValue();
                OutParamWrapper<APValue> x = new OutParamWrapper<>(value);
                if (!evaluateIntegerOrLValue(subExp, x, context))
                    break;

                value = x.get();
                if (value.isInt())
                {
                    value.setInt(value.getInt().extOrTrunc((int)context.getTypeSize(expr.getType())));
                    result.get().base = null;
                    result.get().offset = value.getInt().getZExtValue();
                    return true;
                }
                else
                {
                    // Cast is of an lvalue, no need to change value.
                    result.get().base = value.getLValueBase();
                    result.get().offset = value.getLValueOffset();
                    return true;
                }
            }

            case CK_ArrayToPointerDecay:
            case CK_FunctionToPointerDecay:
                return evaluateLValue(subExp, result, context);
        }
        return false;
    }

    @Override
    public Boolean visitImplicitCastExpr(Tree.ImplicitCastExpr expr)
    {
        return visitCastExpr(expr);
    }

    @Override
    public Boolean visitExplicitCastExpr(Tree.ExplicitCastExpr expr)
    {
        return visitCastExpr(expr);
    }

    @Override
    public Boolean visitCallExpr(Tree.CallExpr expr)
    {
        return super.visitCallExpr(expr);
    }
}
