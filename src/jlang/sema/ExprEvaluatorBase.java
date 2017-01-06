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

import jlang.ast.*;
import jlang.ast.Tree.*;
import tools.OutParamWrapper;
import tools.Util;
import java.math.BigDecimal;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ExprEvaluatorBase<RetTy> extends jlang.ast.StmtVisitor<RetTy>
{
    protected abstract RetTy success(final APValue v, final Expr e);

    protected abstract RetTy error(final Expr expr);

    @Override
    public RetTy visitCallExpr(CallExpr expr)
    {
        return null;
    }

    protected abstract RetTy visitCastExpr(CastExpr expr);

    @Override
    public RetTy visitTree(Tree stmt)
    {
        Util.shouldNotReachHere("Expression evaluator shouldn't called on Stmt!");
        return null;
    }
    @Override
    public RetTy visitConditionalExpr(ConditionalExpr expr)
    {
        OutParamWrapper<Boolean> boolResult = new OutParamWrapper<>();
        if (!handleConversionToBool(expr.getCond(), boolResult))
            return error(expr);

        Expr evalExpr = boolResult.get() ? expr.getTrueExpr()
                : expr.getFalseExpr();
        return visit(evalExpr);
    }
    @Override
    public RetTy visitParenExpr(ParenExpr expr)
    {
        return visit(expr.subExpr);
    }

    @Override
    public RetTy visitInitListExpr(InitListExpr expr)
    {
        return error(expr);
    }

    // Primary expression.
    @Override
    public RetTy visitDeclRefExpr(DeclRefExpr expr)
    {
        return error(expr);
    }

    static boolean handleConversionToBool(
            Expr e,
            OutParamWrapper<Boolean> boolResult)
    {
        if (e.getType().isIntegralOrEnumerationType())
        {
            OutParamWrapper<APSInt> intResult = new OutParamWrapper<>();
            if (!evaluateInteger(e, intResult))
                return false;

            boolResult.set(intResult.get().ne(0));
            return true;
        }
        else if (e.getType().isRealType())
        {
            OutParamWrapper<BigDecimal> floatResult = new OutParamWrapper<>();
            if (!evaluateFloat(e, floatResult))
                return false;
            boolResult.set(floatResult.get().equals(0));
            return true;
        }
        else if (e.getType().isPointerType())
        {
            OutParamWrapper<LValue> pointerResult = new OutParamWrapper<LValue>();
            if (!evaluatePointer(e, pointerResult))
                return false;
            return evaluatePointerValueAsBool(pointerResult.get(), boolResult);
        }
        return false;
    }

    /**
     * Evaluates the specified expression whether it only is an integral constant.
     * If the result of the {@code expr} is evaluated as integral constant, then
     * return true. Otherwise, return false.
     * @param expr
     * @param result
     * @return
     */
    public static boolean evaluateInteger(Expr expr, OutParamWrapper<APSInt> result)
    {
        assert expr.getType().isIntegralOrEnumerationType();

        OutParamWrapper<APValue> val = new OutParamWrapper<>();
        if (!evaluateIntegerOrLValue(expr, val) || !val.get().isInt())
            return false;
        result.set(val.get().getInt());
        return true;
    }

    /**
     * Evaluates the specified expression whether it is an integral constant or
     * {@linkplain LValue}.
     * If the result of the {@code expr} is evaluated as integral constant, then
     * return true. Otherwise, return false.
     * @param expr
     * @param result
     * @return
     */
    public static boolean evaluateIntegerOrLValue(
            Expr expr,
            OutParamWrapper<APValue> result)
    {
        assert expr.getType().isIntegralOrEnumerationType();
        return new IntExprEvaluator(result).visit(expr);
    }

    public static boolean evaluateFloat(Expr e, OutParamWrapper<BigDecimal> result)
    {
        assert e.getType().isRealType();
        return new FloatExprEvaluator(result).visit(e);
    }

    public static boolean evaluatePointer(
            Expr e,
            OutParamWrapper<LValue> reslut)
    {
        assert e.getType().isPointerType();
        return new PointerExprEvaluator(reslut).visit(e);
    }

    public static boolean evaluatePointerValueAsBool(
            LValue val,
            OutParamWrapper<Boolean> res)
    {
        final Expr base = val.getLValueBase();

        if (base == null)
        {
            res.set(val.getLValueOffset() != 0);
            return true;
        }

        // Requires the base expression to be a global l-value.
        if (!isGlobalLValue(base)) return false;

        res.set(true);

        if (!(base instanceof DeclRefExpr))
            return true;

        DeclRefExpr declRef = (DeclRefExpr)base;
        if (declRef.getDecl() instanceof Decl.VarDecl)
        {
            // TODO  If it's a weak jlang.symbol, it isn't constant-evaluable.
            return false;
        }

        return true;
    }

    public static boolean isGlobalLValue(Expr e)
    {
        if (e == null) return true;

        if (e instanceof DeclRefExpr)
        {
            DeclRefExpr dre = (DeclRefExpr)e;
            if (dre.getDecl() instanceof Decl.FunctionDecl)
                return true;
            if (dre.getDecl() instanceof Decl.VarDecl)
                return ((Decl.VarDecl)dre.getDecl()).hasGlobalStorage();
            return false;
        }

        if (e instanceof CompoundLiteralExpr)
            return ((CompoundLiteralExpr)e).isFileScope();

        return true;
    }

    /**
     * The top level evaluate method.
     * @param result
     * @param e
     * @return
     */
    public static boolean evaluate(Expr.EvalResult result, Expr e)
    {
        if (e.getType().isIntegralOrEnumerationType())
        {
            OutParamWrapper<APValue> x = new OutParamWrapper<>(result.getValue());
            if (!new IntExprEvaluator(x).visit(e))
                return false;
            result.val = x.get();
            if (result.getValue().isLValue() &&
                    !isGlobalLValue(result.val.getLValueBase()))
            {
                return false;
            }

        }
        else if (e.getType().isPointerType())
        {
            LValue lv = new LValue();
            OutParamWrapper<LValue>  x = new OutParamWrapper<>(lv);
            if (!evaluatePointer(e, x))
                return false;
            lv = x.get();
            if (!isGlobalLValue(lv.getLValueBase()))
                return false;
            result.val = lv.moveInto();
        }
        else if (e.getType().isRealType())
        {
            BigDecimal f = BigDecimal.ZERO;
            OutParamWrapper<BigDecimal> x = new OutParamWrapper<>(f);
            if (!evaluateFloat(e, x))
                return false;

            f = x.get();
            result.val = new APValue(f);
        }
        else if (e.getType().isComplexType())
        {
            // TODO
            return false;
        }
        else
            return false;

        return true;
    }

    public static boolean evaluateLValue(Expr expr, OutParamWrapper<LValue> result)
    {
        return new LValueExprEvaluator(result).visit(expr);
    }
}
