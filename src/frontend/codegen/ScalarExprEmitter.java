package frontend.codegen;
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

import backend.hir.HIRBuilder;
import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantInt;
import backend.value.Instruction;
import backend.value.Value;
import frontend.ast.StmtVisitor;
import frontend.ast.Tree;
import frontend.sema.Decl;
import frontend.type.QualType;
import sun.awt.SunHints;

/**
 * This class emit code for frontend expression of typed scalar to HIR code.
 * @author Xlous.zeng
 * @version 0.1
 */
public class ScalarExprEmitter extends StmtVisitor<Value>
{
    private CodeGenFunction cgf;
    private HIRBuilder builder;
    private boolean ignoreResultAssign;

    public ScalarExprEmitter(CodeGenFunction cgf)
    {
        this(cgf, false);
    }

    public ScalarExprEmitter(CodeGenFunction cgf, boolean ignoreResultAssign)
    {
        this.cgf = cgf;
        builder = cgf.builder;
        this.ignoreResultAssign = ignoreResultAssign;
    }

    /**
     * Emit a conversion from the specified type to the specified destination
     * type, both of which are backend scalar types.
     * @param v
     * @param srcTy
     * @param destTy
     * @return
     */
    public Value emitScalarConversion(Value v, QualType srcTy, QualType destTy)
    {

    }

	/**
     *  EmitConversionToBool - Convert the specified expression value to a
     * boolean (i1) truth value.  This is equivalent to "Val != 0".
     * @param v
     * @param srcTy
     * @return
     */
    public Value emitScalarConversion(Value v, QualType srcTy)
    {
        // TODO assert srcTy.isCanonical() : "EmitScalarConversion strips typedefs";

        if (srcTy.isRealType())
        {
            // Compare against 0.0 for fp scalar.
            Value zero = Constant.getNullValue(v.getType());
            return builder.createFCmpUNE(v, zero, "tobool");
        }

        assert srcTy.isIntegerType() || srcTy.isPointerType()
                :"Unknown scalar type to convert";

        // Because of the type rules of C, we often end up computing a logical value,
        // then zero extending it to int, then wanting it as a logical value again.
        // Optimize this common case.
        if (v instanceof Instruction.ZExtInst)
        {
            Instruction.ZExtInst zi = (Instruction.ZExtInst)v;
            if (zi.operand(0).getType() == Type.Int1Ty)
            {
                Value result = zi.operand(0);

                // If there aren't any more uses, zap the instruction to save space.
                // Note that there can be more uses, for example if this
                // is the result of an assignment.
                if (zi.isUseEmpty())
                    zi.eraseFromBasicBlock();;

                return result;
            }
        }

        // Compare against an integer or pointer null.
        Value zero = Constant.getNullValue(v.getType());
        return builder.createICmpNE(v, zero, "tobool");
    }

    private Value visitPrePostIncDec(Tree.Expr expr, boolean isInc, boolean isPrec)
    {

    }

    private LValue emitLValue(Tree.Expr expr)
    {
        return cgf.emitLValue(expr);
    }

	/**
	 * Emit the code for loading the value of given expression.
     * @param expr
     * @return
     */
    private Value emitLoadOfLValue(Tree.Expr expr)
    {
        return emitLoadOfLValue(emitLValue(expr), expr.getType());
    }

    private Value emitLoadOfLValue(LValue val, QualType type)
    {
        return cgf.emitLoadOfLValue(val, type).getScalarVal();
    }

    private backend.type.Type convertType(QualType type)
    {return cgf.convertType(type);}

	/**
	 * Converts the specified expression value to a boolean (i1) truth value.
     * @return
     */
    private Value emitConversionToBool(Value src, QualType destTy)
    {
    }

    public Value visitBinMul(Tree.BinaryExpr expr)
    {
        return null;
    }

    public Value visitBinDiv(Tree.BinaryExpr expr)
    {
        return null;
    }

    public Value visitBinRem(Tree.BinaryExpr expr)
    {
        return null;
    }

    public Value visitBinAdd(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinSub(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinShl(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinShr(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinLT(Tree.BinaryExpr expr)
    {
        return null;
    }

    public Value visitBinGT(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinLE(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinGE(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinEQ(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinNE(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinAnd(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinXor(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinOr(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinLAnd(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinLOr(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinMulAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinDivAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinRemAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinAddAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinSubAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinShlAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinShrAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinAndAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinOrAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinXorAssign(Tree.BinaryExpr expr)
    {return null;}

    public Value visitBinComma(Tree.BinaryExpr expr)
    {return null;}

    public Value visitUnaryPostInc(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryPostDec(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryPreInc(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryPreDec(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryAddrOf(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryDeref(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryPlus(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryMinus(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryNot(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryLNot(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryReal(Tree.UnaryExpr expr)
    {return null;}

    public Value visitUnaryImag(Tree.UnaryExpr expr)
    {return null;}

    public Value visitStmt(Tree.Stmt s)
    {
        assert false:"Stmt cann't have a complex type!";
        return null;
    }
    @Override
    public Value visitParenExpr(Tree.ParenExpr expr)
    {return visit(expr.getSubExpr());}

    @Override
    public Value visitIntegerLiteral(Tree.IntegerLiteral expr)
    {return backend.value.ConstantInt.get(expr.getValue());}

    @Override
    public Value visitFloatLiteral(Tree.FloatLiteral expr)
    {return backend.value.ConstantFP.get(expr.getValue());}

    @Override
    public Value visitCharacterLiteral(Tree.CharacterLiteral expr)
    {
        return backend.value.ConstantInt.get(
                convertType(expr.getType()), expr.getValue());
    }

    @Override
    public Value visitDeclRefExpr(Tree.DeclRefExpr expr)
    {
        if (expr.getDecl() instanceof Decl.EnumConstantDecl)
        {
            Decl.EnumConstantDecl ec = (Decl.EnumConstantDecl)expr.getDecl();
            return ConstantInt.get(ec.getInitValue());
        }
        return emitLoadOfLValue(expr);
    }

    @Override
    public Value visitArraySubscriptExpr(Tree.ArraySubscriptExpr expr)
    {

    }

    @Override
    public Value visitMemberExpr(Tree.MemberExpr expr)
    {

    }

    @Override
    public Value visitCompoundLiteralExpr(Tree.CompoundLiteralExpr expr)
    {

    }

    @Override
    public Value visitStringLiteral(Tree.StringLiteral expr)
    {
        return emitLValue(expr).getAddress();
    }

    @Override
    public Value visitInitListExpr(Tree.InitListExpr expr)
    {

    }

    @Override
    public Value visitImplicitCastExpr(Tree.ImplicitCastExpr expr)
    {

    }

    @Override
    public Value visitCallExpr(Tree.CallExpr expr)
    {
        return cgf.emitCallExpr(expr).getScalarVal();
    }
}
