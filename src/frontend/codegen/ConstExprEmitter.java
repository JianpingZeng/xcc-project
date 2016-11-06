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

import backend.value.Constant;
import frontend.ast.StmtVisitor;
import frontend.ast.Tree;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstExprEmitter extends StmtVisitor<Constant>
{
    private HIRGenModule generator;
    private CodeGenFunction cgf;
    public ConstExprEmitter(HIRGenModule genModule, CodeGenFunction cgf)
    {
        generator = genModule;
        this.cgf = cgf;
    }

    public Constant emitLValue(Tree.Expr expr)
    {
        return null;
    }

    @Override public Constant visitBinMul(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinDiv(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinRem(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinAdd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinSub(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinShl(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinShr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinLT(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinGT(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinLE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinGE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinEQ(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinNE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinAnd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinXor(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinOr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinLAnd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinLOr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinMulAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinDivAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinRemAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinAddAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinSubAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinShlAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinShrAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinAndAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinOrAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinXorAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitBinComma(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryPostInc(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryPostDec(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryPreInc(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryPreDec(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryAddrOf(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryDeref(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryPlus(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryMinus(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryNot(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryLNot(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryReal(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Constant visitUnaryImag(Tree.UnaryExpr expr)
    {
        return null;
    }
}
