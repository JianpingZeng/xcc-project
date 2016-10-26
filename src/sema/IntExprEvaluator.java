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

import ast.Tree;
import ast.Tree.*;
import com.sun.org.apache.xml.internal.security.keys.content.DEREncodedKeyValue;
import com.sun.org.apache.xpath.internal.operations.Bool;
import sema.Decl.EnumConstantDecl;
import sema.Decl.ParamVarDecl;
import sema.Decl.VarDecl;
import type.PointerType;
import type.QualType;
import utils.OutParamWrapper;
import utils.Util;

import java.math.BigDecimal;

/**
 * This class represents a implementation of evaluating whether the value of constant
 * expression is an integer. Otherwise, issue error messages if failed.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IntExprEvaluator extends ExprEvaluatorBase<Boolean>
{
    private OutParamWrapper<APValue> result;

    public IntExprEvaluator(OutParamWrapper<APValue> result)
    {
        this.result = result;
    }

    protected boolean success(final APSInt si, final Expr e)
    {
        assert e.getType().isIntegralOrEnumerationType()
                :"Invalid evaluation result.";
        assert si.isSigned() == e.getType().isSignedIntegerOrEnumerationType()
                :"Invalid evaluation result.";
        assert si.getBitWidth() == e.getIntWidth():"Invalid evaluation result.";

        result.set(new APValue(si));
        return true;
    }

    protected boolean success(final APInt i, final Expr e)
    {
        assert e.getType().isIntegralOrEnumerationType()
                :"Invalid evaluation result.";
        assert i.getBitWidth() == e.getIntWidth():"Invalid evaluation result.";

        result.set(new APValue(new APSInt(i)));
        result.get().getInt().setIsUnsigned(
                e.getType().isUnsignedIntegerOrEnumerationType());
        return true;
    }

    private boolean success(long value, final  Expr e)
    {
        assert e.getType().isIntegralOrEnumerationType()
                :"Invalid evaluation result.";
        result.set(new APValue(APSInt.makeIntValue(value, e.getType())));
        return true;
    }
    @Override
    protected Boolean success(APValue v, Expr e)
    {
        return false;
    }

    @Override
    protected Boolean error(Expr expr)
    {
        return error(expr.getLocation(),
                "invalid sub-expression in IntExprEvaluator",
                expr);
    }

    protected boolean error(int l, String diag, Expr e)
    {
        return false;
    }

    @Override
    protected Boolean visit(Expr expr)
    {
        return false;
    }

    @Override
    public Boolean visitIntegerLiteral(IntegerLiteral e)
    {
        return success(e.getValue(), e);
    }

    @Override
    public Boolean visitCharacterLiteral(CharacterLiteral e)
    {
        return success(e.getValue(), e);
    }

    @Override
    public Boolean visitDeclRefExpr(DeclRefExpr e)
    {
        if (checkReferencedDecl(e, e.getDecl()))
            return true;
        return super.visitDeclRefExpr(e);
    }

    private boolean checkReferencedDecl(Expr e, Decl d)
    {
        // Enums are integer constant expres.
        if (d instanceof EnumConstantDecl)
        {
            EnumConstantDecl ecd = (EnumConstantDecl)d;
            boolean sameSign = ecd.getInitValue().isSigned()
                    ==e.getType().isSignedIntegerOrEnumerationType();
            boolean sameWidth = ecd.getInitValue().getBitWidth()
                    == QualType.getIntWidth(e.getType());
            if (sameSign && sameWidth)
            {
                return success(ecd.getInitValue(), e);
            }
            else
            {
                // Get rid of mismatch (otherwise Success assertions will fail)
                // by computing a new value matching the type of E.
                APSInt val = ecd.getInitValue();
                if (!sameSign)
                    val.setIssigned(!ecd.getInitValue().isSigned());
                if (!sameWidth)
                    val = val.extOrTrunc(QualType.getIntWidth(e.getType()));
                return success(val, e);
            }
        }

        // In C, they can also be folded, although they are not ICEs.
        if (e.getType().getCVRQualifiers() == QualType.CONST_QUALIFIER)
        {
            if (d instanceof ParamVarDecl)
                return false;

            if (d instanceof VarDecl)
            {
                VarDecl vd = (VarDecl)d;
                Expr init = vd.getInit();
                if (init != null)
                {
                    APValue v = vd.getEvaluatedValue();
                    if (v != null)
                    {
                        if (v.isInt())
                            return success(v.getInt(), e);
                        return false;
                    }

                    if (vd.isEvaluatingValue())
                        return false;

                    vd.setEvaluatingValue();

                    Expr.EvalResult evalResult = new Expr.EvalResult();
                    OutParamWrapper<Expr.EvalResult> x = new OutParamWrapper<>(evalResult);
                    if (init.evaluate(x) && !evalResult.hasSideEffects()
                            && evalResult.getValue().isInt())
                    {
                        // Cache the evaluated value in the variable declaration.
                        evalResult = x.get();
                        result.set(evalResult.getValue());
                        vd.setEvaluatedValue(evalResult.getValue());
                        return true;
                    }
                    vd.setEvaluatedValue(new APValue());
                }
            }
        }

        // Otherwise, random variable references are not constants.
        return false;
    }

    @Override
    public Boolean visitMemberExpr(MemberExpr e)
    {
        if (checkReferencedDecl(e, e.getMemberDecl()))
        {
            // Conservatively assume a MemberExpr will have side-effetcs.
            return true;
        }
        return super.visitMemberExpr(e);
    }

    @Override
    public Boolean visitCallExpr(CallExpr expr)
    {
        return error(expr);
    }

    @Override
    public Boolean visitBinaryExpr(BinaryExpr expr)
    {
        if (expr.getOpcode() == BinaryOperatorKind.BO_Comma)
        {
            if (!visit(expr.getRHS()))
                return false;

            // If we can't evaluate the LHS, it might have side effects.
            // mark it.
            return true;
        }

        if (expr.isLogicalOp())
        {
            OutParamWrapper<Boolean> lhsResult = new OutParamWrapper<>()
                    , rhsResult = new OutParamWrapper<>();
            if (handleConversionToBool(expr.getLHS(), lhsResult))
            {
                // make constant folding operation.
                // evaluating the RHS: 0&& RHS -> 0, 1||RHS -> 1
                if (lhsResult.get() == (expr.getOpcode() == BinaryOperatorKind.BO_LOr))
                {
                    return success(lhsResult.get()? 1:0, expr);
                }
                if (handleConversionToBool(expr.getRHS(), rhsResult))
                {
                    if (expr.getOpcode() == BinaryOperatorKind.BO_LOr)
                        return success(lhsResult.get() || rhsResult.get() ? 1:0, expr);
                    else
                        return success(lhsResult.get() && rhsResult.get() ? 1:0, expr);
                }
            }
            else
            {
                if (handleConversionToBool(expr.getRHS(), lhsResult))
                {
                    if (rhsResult.get() == (expr.getOpcode() == BinaryOperatorKind.BO_LOr
                    || !rhsResult.get() == (expr.getOpcode() == BinaryOperatorKind.BO_LAnd)))
                    {
                        return success(rhsResult.get()?1:0, expr);
                    }
                }
            }

            return false;
        }

        QualType lhsTy = expr.getLHS().getType();
        QualType rhsTy = expr.getRHS().getType();

        if (lhsTy.isComplexType())
        {
            assert rhsTy.isComplexType():"Invalid comparison";
            // TODO Clang 3.0 ExprConstant.cpp:1398

        }

        if (lhsTy.isRealType() && rhsTy.isRealType())
        {
            OutParamWrapper<BigDecimal> rhs = new OutParamWrapper<>(new BigDecimal(0.0));
            OutParamWrapper<BigDecimal> lhs = new OutParamWrapper<>(new BigDecimal(0.0));

            if (!evaluateFloat(expr.getRHS(), rhs))
                return false;

            if (!evaluateFloat(expr.getLHS(), lhs))
                return false;

            int compResult = lhs.get().compareTo(rhs.get());
            switch (expr.getOpcode())
            {
                default:
                    Util.shouldNotReachHere("Invalid binary operator!");
                    break;
                case BO_LT:
                    success(compResult<0?1:0, expr);
                    break;
                case BO_GT:
                    success(compResult>0?1:0, expr);
                    break;
                case BO_LE:
                    success(compResult<=0?1:0, expr);
                    break;
                case BO_GE:
                    success(compResult>=0?1:0, expr);
                    break;
                case BO_EQ:
                    success(compResult==0?1:0, expr);
                    break;
                case BO_NE:
                    success(compResult!=0?1:0, expr);
                    break;
            }
        }

        if (lhsTy.isPointerType() && rhsTy.isPointerType())
        {
            if (expr.getOpcode() == BinaryOperatorKind.BO_Sub
                    || expr.isEqualityOp())
            {
                OutParamWrapper<LValue> lhsValue =new OutParamWrapper<>();
                if (!evaluatePointer(expr.getLHS(), lhsValue))
                    return false;

                OutParamWrapper<LValue> rhsValue =new OutParamWrapper<>();
                if (!evaluatePointer(expr.getRHS(), rhsValue))
                    return false;

                // Reject any bases from the normal codepath; we special-case comparisons
                // to null.
                if (lhsValue.get().getLValueBase() != null)
                {
                    if (!expr.isEqualityOp())
                        return false;

                    if (rhsValue.get().getLValueBase() != null
                            || rhsValue.get().getLValueOffset() != 0)
                    {
                        return false;
                    }

                    OutParamWrapper<Boolean> bres = new OutParamWrapper<>();
                    if (evaluatePointerValueAsBool(lhsValue.get(), bres))
                        return false;
                    return success(bres.get()?1:0 ^ (expr.getOpcode()==BinaryOperatorKind.BO_EQ?1:0), expr);
                }
                else if (rhsValue.get().getLValueBase() != null)
                {
                    if (!expr.isEqualityOp())
                        return false;

                    if (lhsValue.get().getLValueBase() != null
                            || lhsValue.get().getLValueOffset() != 0)
                    {
                        return false;
                    }

                    OutParamWrapper<Boolean> bres = new OutParamWrapper<>();
                    if (evaluatePointerValueAsBool(rhsValue.get(), bres))
                        return false;
                    return success(bres.get()?1:0 ^ (expr.getOpcode()==BinaryOperatorKind.BO_EQ?1:0), expr);
                }

                if (expr.getOpcode() == BinaryOperatorKind.BO_Sub)
                {
                    QualType type = expr.getLHS().getType();
                    QualType elemType = type.<PointerType>getAs().getPointeeType();

                    long elemSize = 1;
                    if (!elemType.isVoidType() && !elemType.isFunctionType())
                    {
                        elemSize = elemType.getTypeSize()>>3;
                    }

                    long diff = lhsValue.get().getLValueOffset() - rhsValue.get().getLValueOffset();
                    return success(diff / elemSize, expr);
                }

                boolean result;
                if (expr.getOpcode() == BinaryOperatorKind.BO_EQ)
                {
                    result = lhsValue.get().getLValueOffset() == rhsValue.get().getLValueOffset();
                }
                else
                {
                    result = lhsValue.get().getLValueOffset() !=rhsValue.get().getLValueOffset();
                }
                return success(result?1:0, expr);
            }
        }

        if (!lhsTy.isIntegralOrEnumerationType()
                || !rhsTy.isIntegralOrEnumerationType())
        {
            // We can't continue from here for non-integral types, and they
            // could potentially confuse the following operations.
            return false;
        }

        // The LHS of a constant expr is always evaluated and needed.
        if (!visit(expr.getLHS()))
            return false; // error in sub-expression.

        OutParamWrapper<APValue> rhsVal = new OutParamWrapper<>();
        if (!evaluateIntegerOrLValue(expr.getRHS(), rhsVal))
            return false;

        // Handle cases like (unsigned long)&a + 4.
        if (expr.isAdditiveOp() && result.get().isLValue()
                && rhsVal.get().isInt())
        {
            long offset = result.get().getLValueOffset();
            long additionalOffset = rhsVal.get().getInt().getZExtValue();
            if (expr.getOpcode() == BinaryOperatorKind.BO_Add)
                offset += additionalOffset;
            else
                offset -= additionalOffset;

            result.set(new APValue(result.get().getLValueBase(), offset));;
            return true;
        }


        // Handle cases like 4 + (unsigned long)&a
        if (expr.getOpcode() == BinaryOperatorKind.BO_Add
                && rhsVal.get().isLValue() && result.get().isInt())
        {
            long offset = rhsVal.get().getLValueOffset();
            offset += result.get().getInt().getZExtValue();
            result.set(new APValue(rhsVal.get().getLValueBase(), offset));
            return true;
        }

        // All the following cases expect both operands to be an integer
        if (!result.get().isInt() || !rhsVal.get().isInt())
            return false;

        APSInt rhs = rhsVal.get().getInt();


        switch (expr.getOpcode())
        {
            default:
                return error(expr.getOperatorLoc(),
                        "invalid sub-expression in IntExprEvaluator",
                        expr);
            case BO_Mul: return success(result.get().getInt().multiply(rhs), expr);
            case BO_Add: return success(result.get().getInt().addictive(rhs), expr);
            case BO_Sub: return success(result.get().getInt().subtraction(rhs), expr);
            case BO_And: return success(result.get().getInt().bitAnd(rhs), expr);
            case BO_Xor: return success(result.get().getInt().bitXor(rhs), expr);
            case BO_Or: return success(result.get().getInt().bitOr(rhs), expr);
            case BO_Div:
            {
                if (rhs.eq(0))
                    return error(expr.getOperatorLoc(), "division by zero", expr);
                return success(result.get().getInt().divide(rhs), expr);
            }
            case BO_Rem:
            {
                if (rhs.eq(0))
                    return error(expr.getOperatorLoc(), "division by zero", expr);
                return success(result.get().getInt().remainder(rhs), expr);
            }
            case BO_Shl:
            {
                if (rhs.isSigned() && rhs.isNegative())
                {
                    rhs = rhs.negative();
                }
                int sa = (int)rhs.getLimitedValue(result.get().getInt().getBitWidth() - 1);
                return success(result.get().getInt().shl(sa), expr);
            }
            case BO_Shr:
            {
                if (rhs.isSigned() && rhs.isNegative())
                {
                    rhs = rhs.negative();
                }
                int sa = (int)rhs.getLimitedValue(result.get().getInt().getBitWidth() - 1);
                return success(result.get().getInt().shr(sa), expr);
            }

            case BO_LT: return success(result.get().getInt().lt(rhs)?1:0, expr);
            case BO_GT: return success(result.get().getInt().gt(rhs)?1:0, expr);
            case BO_LE: return success(result.get().getInt().le(rhs)?1:0, expr);
            case BO_GE: return success(result.get().getInt().ge(rhs)?1:0, expr);
            case BO_EQ: return success(result.get().getInt().eq(rhs)?1:0, expr);
            case BO_NE: return success(result.get().getInt().ne(rhs)?1:0, expr);
        }
    }

    @Override
    public Boolean visitUnaryExpr(UnaryExpr expr)
    {
        if (expr.getOpCode() == UnaryOperatorKind.UO_LNot)
        {
            // LNot's operand isn't necessarily an integer, so
            // we handle it specially.
            OutParamWrapper<Boolean> bres = new OutParamWrapper<>();
            if (!handleConversionToBool(expr.getSubExpr(), bres))
                return false;

            return success(bres.get()?0:1, expr);
        }

        // Only handles integral operation.
        if (!expr.getSubExpr().getType().isIntegralOrEnumerationType())
            return false;

        // Let the operand value into 'Result'.
        if (!visit(expr.getSubExpr()))
            return false;

        switch (expr.getOpCode())
        {
            default:
                // Address, indirect, pre/post inc/dec, etc are not valid constant exprs.
                // See C99 6.6p3.
                return error(expr.getLocation(),"invalid sub-expression in IntExprEvaluator", expr);
            case UO_Plus:
                return true;
            case UO_Minus:
                if (!result.get().isInt()) return false;
                return success(result.get().getInt().negative(), expr);
            case UO_Not:
                if (!result.get().isInt()) return false;
                return success(result.get().getInt().not(), expr);
        }
    }

    @Override
    public Boolean visitImplicitCastExpr(ImplicitCastExpr expr)
    {
        return visitCastExpr(expr);
    }

    @Override
    public Boolean visitExplicitCastExpr(ExplicitCastExpr expr)
    {
        return visitCastExpr(expr);
    }

    /**
     *
     * @param expr
     * @return
     */
    private boolean visitCastExpr(CastExpr expr)
    {
        Expr subExpr = expr.getSubExpr();
        QualType destType = expr.getType();
        QualType srcType = subExpr.getType();

        switch (expr.getCastKind())
        {
            case CK_ArrayToPointerDecay:
            case CK_FunctionToPointerDecay:
            case CK_IntegralToPointer:
            case CK_ToVoid:
            case CK_IntegralToFloating:
            case CK_FloatingCast:
            case CK_FloatingRealToComplex:
            case CK_FloatingComplexCast:
            case CK_FloatingComplexToIntegralComplex:
            case CK_IntegralRealToComplex:
            case CK_IntegralComplexCast:
            case CK_IntegralComplexToFloatingComplex:
                Util.shouldNotReachHere("Invalid cast kind for integral value");

            case CK_BitCast:
                return false;
            case CK_LValueToRValue:
            case CK_NoOp:
                return visit(expr.getSubExpr());
            case CK_PointerToBoolean:
            case CK_IntegralToBoolean:
            case CK_FloatingToBoolean:
            case CK_FloatingComplexToBoolean:
            case CK_IntegralComplexToBoolean:
            {
                OutParamWrapper<Boolean> boolResult = new OutParamWrapper<>();
                if (!handleConversionToBool(subExpr, boolResult))
                    return false;
                return success(boolResult.get()?1:0, expr);
            }

            case CK_IntegralCast:
            {
                if (!visit(subExpr))
                    return false;

                if (!result.get().isInt())
                {
                    // Only allow casts of lvalues if they are lossless.
                    return destType.getTypeSize() == srcType.getTypeSize();
                }

                return success(handleIntToIntCast(destType, srcType, result.get().getInt()), expr);
            }

            case CK_PointerToIntegral:
            {
                OutParamWrapper<LValue> lv = new OutParamWrapper<>();
                if (!evaluatePointer(subExpr, lv))
                    return false;

                if (lv.get().getLValueBase() != null)
                {
                    if (destType.getTypeSize() != srcType.getTypeSize())
                        return false;

                     result.set(lv.get().moveInto());
                    return true;
                }

                APSInt asInt = APSInt.makeIntValue(lv.get().getLValueOffset(), srcType);
                return success(handleIntToIntCast(destType, srcType, asInt), expr);
            }

            case CK_IntegralComplexToReal:
            {
                // TODO Clang3.0 ExprConstant.cpp:1907
            }
            case CK_FloatingToIntegral:
            {
                OutParamWrapper<BigDecimal> f = new OutParamWrapper<>();
                if (!evaluateFloat(subExpr, f))
                    return false;

                return success(handleFloatToIntCast(destType, srcType, f.get()), expr);
            }
        }
        Util.shouldNotReachHere("unknown cast result in integral value");
        return false;
    }

    private static APSInt handleIntToIntCast(QualType destType, QualType srcType, APSInt i)
    {
        int destWidth = QualType.getIntWidth(destType);
        APSInt result = i;
        // Figure out if this is a truncate, extend or noop cast.
        // If the input is signed, do a sign extend, noop, or truncate.
        result = result.extOrTrunc(destWidth);
        result.setIsUnsigned(destType.isUnsignedIntegerOrEnumerationType());
        return result;
    }

    private static APSInt handleFloatToIntCast(QualType destType, QualType srcType, BigDecimal val)
    {
        int destWidth = QualType.getIntWidth(destType);

        // Determine whether we are converting to unsigned or signed.
        boolean destIsSigned = destType.isSignedIntegerOrEnumerationType();

        APSInt result = new APSInt(val.intValue(), !destIsSigned);
        if (destIsSigned)
            result.sextOrTrunc(destWidth);
        else
            result.zextOrTrunc(destWidth);
        return result;
    }
}
