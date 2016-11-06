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
import backend.value.Value;
import frontend.ast.StmtVisitor;
import frontend.ast.Tree;
import frontend.type.QualType;

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
        return null;
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
        return (Value)new Object();
    }
}
