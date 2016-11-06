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

import backend.value.Value;
import frontend.ast.StmtVisitor;
import frontend.ast.Tree;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class AggExprEmitter extends StmtVisitor
{
    public AggExprEmitter(CodeGenFunction codeGenFunction,
            Value destPtr,
            boolean ignoreResult,
            boolean isInitializer)
    {

    }

    @Override public Object visitBinMul(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinDiv(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinRem(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinAdd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinSub(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinShl(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinShr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinLT(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinGT(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinLE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinGE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinEQ(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinNE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinAnd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinXor(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinOr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinLAnd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinLOr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinMulAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinDivAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinRemAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinAddAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinSubAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinShlAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinShrAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinAndAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinOrAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinXorAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitBinComma(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryPostInc(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryPostDec(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryPreInc(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryPreDec(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryAddrOf(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryDeref(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryPlus(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryMinus(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryNot(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryLNot(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryReal(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Object visitUnaryImag(Tree.UnaryExpr expr)
    {
        return null;
    }
}
