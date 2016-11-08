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
public class AggExprEmitter extends StmtVisitor<Void>
{
    public AggExprEmitter(CodeGenFunction codeGenFunction,
            Value destPtr,
            boolean ignoreResult,
            boolean isInitializer)
    {

    }

    @Override public Void visitBinMul(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinDiv(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinRem(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinAdd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinSub(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinShl(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinShr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinLT(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinGT(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinLE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinGE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinEQ(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinNE(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinAnd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinXor(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinOr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinLAnd(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinLOr(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinAssign(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitBinMulAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinDivAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinRemAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinAddAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinSubAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinShlAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinShrAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinAndAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinOrAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinXorAssign(Tree.CompoundAssignExpr expr)
    {
        return null;
    }

    @Override public Void visitBinComma(Tree.BinaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryPostInc(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryPostDec(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryPreInc(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryPreDec(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryAddrOf(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryDeref(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryPlus(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryMinus(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryNot(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryLNot(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryReal(Tree.UnaryExpr expr)
    {
        return null;
    }

    @Override public Void visitUnaryImag(Tree.UnaryExpr expr)
    {
        return null;
    }
}
