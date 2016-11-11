package frontend.codegen;
/*
 * Xlous C language CompilerInstance
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
import tools.Context;
import tools.Log;

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
    private Context ctx;
    private Options options;
    private Log logger;
    private Module M;
    private HIRModuleGenerator builder;

    public ModuleBuilder(String moduleName, Context ctx)
    {
        logger = Log.instance(ctx);
        options = Options.instance(ctx);
        M = new Module(moduleName);
        this.ctx = ctx;
    }

    /**
     * This method is invoked for initializing this ASTConsumer.
     *
     */
    @Override
    public void initialize()
    {
        builder = new HIRModuleGenerator(ctx, M);
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
     */
    @Override
    public void handleTranslationUnit()
    {
        if (logger.nerrors> 0)
        {
            M = null;
            return;
        }

        if (builder != null)
            builder.release();
    }

    public Module getModule() {return M;}
}
