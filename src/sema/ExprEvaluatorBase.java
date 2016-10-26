package sema;
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

import ast.ASTVisitor;
import ast.Tree;
import ast.Tree.*;
import utils.OutParamWrapper;
import utils.Util;
import java.math.BigDecimal;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ExprEvaluatorBase<RetTy> extends ASTVisitor<RetTy>
{
    protected abstract RetTy success(final APValue v, final Expr e);
    protected abstract RetTy error(final Expr expr);

    protected abstract RetTy visit(Expr expr);

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

    static boolean evaluateInteger(Expr e, OutParamWrapper<APSInt> result)
    {
        assert e.getType().isIntegralOrEnumerationType();

        OutParamWrapper<APValue> val = new OutParamWrapper<>();
        if (!evaluateIntegerOrLValue(e, val) || !val.get().isInt())
            return false;
        result.set(val.get().getInt());
        return true;
    }

    static boolean evaluateIntegerOrLValue(Expr e, OutParamWrapper<APValue> result)
    {
        assert e.getType().isIntegralOrEnumerationType();
        return new IntExprEvaluator(result).visit(e);
    }

    static boolean evaluateFloat(Expr e, OutParamWrapper<BigDecimal> result)
    {
        assert e.getType().isRealType();
        return new FloatExprEvaluator(result).visit(e);
    }

    static boolean evaluatePointer(Expr e, OutParamWrapper<LValue> reslut)
    {
        assert e.getType().isPointerType();
        return new PointerExprEvaluator(reslut).visit(e);
    }

    static boolean evaluatePointerValueAsBool(LValue val, OutParamWrapper<Boolean> res)
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
            // TODO  If it's a weak symbol, it isn't constant-evaluable.
            return false;
        }

        return true;
    }

    static boolean isGlobalLValue(Expr e)
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

}
