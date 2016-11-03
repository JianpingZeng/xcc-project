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
 * @author Xlous.zeng
 * @version 0.1
 */
public class ScalarExprEmitter extends StmtVisitor<Value>
{
    private CodeGenFunction cgf;
    private HIRBuilder builder;
    public ScalarExprEmitter(CodeGenFunction cgf)
    {
        this.cgf = cgf;
        builder = cgf.builder;
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

    @Override
    public Value visit(Tree.Expr expr)
    {
        return null;
    }
}
