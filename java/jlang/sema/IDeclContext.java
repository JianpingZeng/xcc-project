package jlang.sema;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import jlang.clex.IdentifierInfo;
import jlang.cparser.DeclKind;

import java.util.List;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
interface IDeclContext
{
    DeclKind getKind();

    String getDeclKindName();

    ASTContext getParentASTContext();

    /**
     * Gets the parent declaration context enclosing this.
     * @return
     */
    IDeclContext getParent();

    /**
     * Add the declaration D into this context.
     * <p>
     * This routine should be invoked when the declaration D has first
     * been declared, to place D into the context where it was
     * (lexically) defined. Every declaration must be added to one
     * (and only one!) context, where it can be visited via
     * [decls_begin(), decls_end()). Once a declaration has been added
     * to its lexical context, the corresponding DeclContext owns the
     * declaration.
      * </p>
     * <p>
     * If D is also a NamedDecl, it will be made visible within its
     * semantic context via makeDeclVisibleInContext.
     </p>
     * @param decl
     */
    void addDecl(Decl decl);

    void addHiddenDecl(Decl d);

    void removeDecl(Decl decl);

    boolean isFunction();

    /**
     * Determines if this context is file context.
     * @return
     */
    boolean isFileContext();

    boolean isTranslationUnit();

    boolean isRecord();

    /**
     * Determines whether this context is a
     * "transparent" context, meaning that the members declared in this
     * context are semantically declared in the nearest enclosing
     * non-transparent (opaque) context but are lexically declared in
     * this context. For example, consider the enumerators of an
     * enumeration type:
     * <pre>
     * enum E 
     * {
     *      Val1 
     * };
     * </pre>
     * Here, E is a transparent context, so its enumerator (Val1) will
     * appear (semantically) that it is in the same context of E.
     * @return
     */
    boolean isTransparentContext();

    /**
     * Checks if this Declaration context encloses the specified dc.
     * @param dc
     * @return  Return true if this IDeclContext encloses the given dc,
     * otherwise return false.
     */
    default boolean encloses(IDeclContext dc)
    {
        for (; dc != null; dc = dc.getParent())
        {
            if (dc == this)
                return true;
        }
        return false;
    }

    /**
     * Retrieve the innermost non-transparent
     * context of this context, which corresponds to the innermost
     * location from which name lookup can find the entities in this context.
     * @return
     */
    IDeclContext getLookupContext();
    /**
     * isDeclInContext method return true if given {@code decl} is declared in
     * the current DeclContext.
     * @param decl
     * @return
     */
    boolean isDeclInContext(Decl decl);
    
    boolean isEmpty();

    List<Decl> getDeclsInContext();

    /**
     * The number of decls stored in this DeclContext.
     * @return
     */
    int getDeclCounts();

    /**
     * Obtains the specified Decl object indexed by {@code idx} in this DeclContext.
     * @param idx
     * @return
     */
    Decl getDeclAt(int idx);

    /**
     * Find the declarations (if any) with the given Name in
     * this context. Returns a list that contains all of
     * the declarations with this name, with object, function, member,
     * and enumerator names preceding any stmtClass name. Note that this
     * routine will not look into parent contexts.
     * @param name
     * @return
     */
    Decl.NamedDecl[] lookup(IdentifierInfo name);
    
    /**
    * Makes a declaration visible within this context.
    *
    * This routine makes the declaration D visible to name lookup
    * within this context and, if this is a transparent context,
    * within its parent contexts up to the first enclosing
    * non-transparent context. Making a declaration visible within a
    * context does not transfer ownership of a declaration, and a
    * declaration can be visible in many contexts that aren't its
    * lexical context.
    *
    * If D is a redeclaration of an existing declaration that is
    * visible from this context, as determined by
    * NamedDecl::declarationReplaces, the previous declaration will be
    * replaced with D.
     */
    void makeDeclVisibleInContext(Decl.NamedDecl nd);

    /**
     * There may be many different declarations of the same entity
     * (including forward declarations of classes, multiple definitions
     * of namespaces, etc.), each with a different set of declarations.
     * This routine returns the "primary" DeclContext structure, which will
     * contain the information needed to perform name lookup into this context.
     * @return
     */
    IDeclContext getPrimaryContext();
}
