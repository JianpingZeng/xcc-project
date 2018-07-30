package jlang.sema;
/*
 * Extremely C language CompilerInstance
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

import tools.Util;
import tools.APFloat;
import tools.APSInt;
import jlang.ast.Tree;
import jlang.ast.Tree.*;
import tools.OutRef;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class ExprEvaluatorBase<RetTy> extends jlang.ast.StmtVisitor<RetTy>
{
    protected abstract RetTy success(final APValue v, final Expr e);

    protected abstract RetTy error(final Expr expr);
    protected  ASTContext context;

    protected ExprEvaluatorBase(ASTContext ctx)
    {
        context = ctx;
    }

    @Override
    public RetTy visitCallExpr(CallExpr expr)
    {
        return null;
    }

    public RetTy visitTree(Tree stmt)
    {
        Util.shouldNotReachHere("Expression evaluator shouldn't called on Stmt!");
        return null;
    }
    @Override
    public RetTy visitConditionalExpr(ConditionalExpr expr)
    {
        OutRef<Boolean> boolResult = new OutRef<>();
        if (!handleConversionToBool(expr.getCond(), boolResult, context))
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
            OutRef<Boolean> boolResult,
            ASTContext context)
    {
        if (e.getType().isIntegralOrEnumerationType())
        {
            OutRef<APSInt> intResult = new OutRef<>();
            if (!evaluateInteger(e, intResult, context))
                return false;

            boolResult.set(intResult.get().ne(0));
            return true;
        }
        else if (e.getType().isRealType())
        {
            OutRef<APFloat> floatResult = new OutRef<>();
            if (!evaluateFloat(e, floatResult, context))
                return false;
            boolResult.set(floatResult.get().equals(0));
            return true;
        }
        else if (e.getType().isPointerType())
        {
            OutRef<LValue> pointerResult = new OutRef<LValue>();
            if (!evaluatePointer(e, pointerResult, context))
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
    public static boolean evaluateInteger(Expr expr, OutRef<APSInt> result, ASTContext ctx)
    {
        Util.assertion( expr.getType().isIntegralOrEnumerationType());

        OutRef<APValue> val = new OutRef<>();
        if (!evaluateIntegerOrLValue(expr, val, ctx) || !val.get().isInt())
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
            OutRef<APValue> result,
            ASTContext ctx)
    {
        Util.assertion( expr.getType().isIntegralOrEnumerationType());
        return new IntExprEvaluator(result, ctx).visit(expr);
    }

    public static boolean evaluateFloat(Expr e, 
            OutRef<APFloat> result,
            ASTContext ctx)
    {
        Util.assertion( e.getType().isRealType());
        return new FloatExprEvaluator(result, ctx).visit(e);
    }

    public static boolean evaluatePointer(
            Expr e,
            OutRef<LValue> reslut,
            ASTContext context)
    {
        Util.assertion( e.getType().isPointerType());
        return new PointerExprEvaluator(reslut, context).visit(e);
    }

    public static boolean evaluatePointerValueAsBool(
            LValue val,
            OutRef<Boolean> res)
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
    public static boolean evaluate(Expr.EvalResult result, Expr e, ASTContext context)
    {
        if (e.getType().isIntegerType())
        {
            OutRef<APValue> x = new OutRef<>(result.getValue());
            if (!new IntExprEvaluator(x, context).visit(e))
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
            OutRef<LValue> x = new OutRef<>(lv);
            if (!evaluatePointer(e, x,context))
                return false;
            lv = x.get();
            if (!isGlobalLValue(lv.getLValueBase()))
                return false;
            result.val = lv.moveInto();
        }
        else if (e.getType().isRealFloatingType())
        {
            APFloat f = new APFloat(0.0);
            OutRef<APFloat> x = new OutRef<>(f);
            if (!evaluateFloat(e, x, context))
                return false;

            f = x.get();
            result.val = new APValue(f);
        }
        else if (e.getType().isComplexType())
        {
            // TODO
            Util.assertion(false, "Complex type is not supported!");
            return false;
        }
        else
            return false;

        return true;
    }

    public static boolean evaluateLValue(
            Expr expr,
            OutRef<LValue> result,
            ASTContext ctx)
    {
        return new LValueExprEvaluator(result, ctx).visit(expr);
    }
}
