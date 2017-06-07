package jlang.ast;
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

import jlang.sema.ASTContext;
import jlang.sema.Decl;

import java.util.ArrayList;

/**
 * This is an abstract base class for client reading AST nodes in dependent of
 * AST producer (e.g. {@linkplain jlang.cparser.Parser}, AST dumper).
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public interface ASTConsumer
{
    /**
     * This method is invoked for initializing this ASTConsumer.
     */
    void initialize(ASTContext ctx);

    /**
     * Handle the specified top level declaration.
     * This method is called by {@linkplain Compiler} to process every top-level
     * decl.
     * <b>Note that</b> decls is a list that chained multiple Declaration, like
     * <code>'int a, b'</code>, there are two declarator chained.
     * @param decls
     */
    void handleTopLevelDecls(ArrayList<Decl> decls);

    /**
     * This method is called when the parsing file for entire translation unit
     * was parsed.
     */
    void handleTranslationUnit();

	/**
     * This callback is invoked each time a TagDecl
     * (e.g. struct, union, enum) is completed.  This allows the client to
     * hack on the type, which can occur at any point in the file (because these
     * can be defined in declspecs).
     * @param tag
     */
    default void handleTagDeclDefinition(Decl.TagDecl tag){}

    default void completeTentativeDefinition(Decl.VarDecl d)
    {
    }
}
