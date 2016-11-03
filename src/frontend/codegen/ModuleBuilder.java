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

import backend.hir.BasicBlock;
import backend.hir.Module;
import frontend.ast.ASTConsumer;
import driver.Options;
import frontend.sema.ASTContext;
import frontend.sema.Decl;

import java.util.ArrayList;

/**
 * <p>
 * This class is responsible for transforming external declaration in a
 * translation unit, like global variable declaration and function definition
 * , into implementation corresponding to Higher Level Representation.
 * </p>
 * <p>
 * For function definition, each statement contained in it would be lowered into
 * HIR instruction contained in {@linkplain BasicBlock}.
 * For the same methodology, global variable declaration would be viewed as
 * Constant value in HIR.
 * </p>
 * <p>
 * As the consequence, a {@linkplain Module} would be obtained which holds many
 * element, for example, Functions in HIR perspective, global Constants.
 * </p>
 * @see ASTConsumer
 * @see ASTContext
 * @author Xlous.zeng
 * @version 0.1
 */
public class ModuleBuilder extends ASTConsumer
{
    private ASTContext ctx;
    private Options options;
    private Module M;
    private HIRGenModule builder;

    public ModuleBuilder(String moduleName, Options options)
    {
        this.options = options;
        M = new Module(moduleName);
    }

    /**
     * This method is invoked for initializing this ASTConsumer provided with
     * instance ctx of {@linkplain ASTContext}.
     *
     * @param context
     */
    @Override
    public void initialize(ASTContext context)
    {
        ctx = context;
        builder = new HIRGenModule(context, options, M);
    }

    /**
     * Handle the specified top level declaration.
     * This method is called by {@linkplain Compiler} to process every top-level
     * decl.
     * <b>Note that</b> decls is a list that chained multiple Declaration, like
     * <code>'int a, b'</code>, there are two declarator chained.
     *
     * @param decls
     */
    @Override
    public void handleTopLevelDecls(ArrayList<Decl> decls)
    {
        for (Decl d : decls)
        {
            // Make sure that emits all elements for each decl.
            builder.emitTopLevelDecl(d);
        }
    }

    /**
     * This method is called when the parsing file for entire translation unit
     * was parsed.
     *
     * @param ctx
     */
    @Override
    public void handleTranslationUnit(ASTContext ctx)
    {

    }
}
