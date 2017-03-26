package jlang.sema;
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

/**
 * Represents the result of getName lookup up.
 * @author Xlous.zeng
 * @version 0.1
 */

import java.util.ArrayList;

import static jlang.sema.LookupResult.LookupResultKind.Ambiguous;
import static jlang.sema.LookupResult.LookupResultKind.Found;
import static jlang.sema.LookupResult.LookupResultKind.NotFound;

public class LookupResult
{
    enum LookupResultKind
    {
        /***
         * No entity found met the criteria.
         */
        NotFound,

        /**
         * Name lookup up found a single declaration that met the
         * criteria. getFoundDecl() will return this declaration.
         */
        Found,

        Ambiguous,
    }
    private String foundName;
    private int nameLoc;
    private LookupResultKind resultKind;
    private Sema semaRef;
    private Sema.LookupNameKind lookupKind;
    private ArrayList<Decl.NamedDecl> decls;

    /**
     * Indicates the getName space which this getName in.
     * There are four cases:
     * 1. Ordinary getName; 2.Tag getName; 3.Member getName; 4.Label getName.
     */
    private IdentifierNamespace IDNS;

    public Decl.NamedDecl getFoundDecl()
    {
        return decls.get(0);
    }

    public boolean isEmpty()
    {
        return decls.isEmpty();
    }

    /**
     * ReturnStmt true if the found result is ambiguous.
     *
     * @return
     */
    public boolean isAmbiguous()
    {
        return getResultKind() == Ambiguous;
    }

    /**
     * ReturnStmt true if the found result is certainly deterministic.
     *
     * @return
     */
    public boolean isSingleResult()
    {
        return getResultKind() == Found;
    }

    /**
     * Obtains the found result.
     *
     * @return
     */
    public LookupResultKind getResultKind()
    {
        return resultKind;
    }

    public String getLookupName()
    {
        return foundName;
    }

    public Sema.LookupNameKind getLookupKind()
    {
        return lookupKind;
    }

    /**
     * Returns the identifier namespace mask for this lookup.
     * @return
     */
    public IdentifierNamespace getIdentifierNamespace()
    {
        return IDNS;
    }

    public boolean isInIdentifierNamespace(IdentifierNamespace ns)
    {
        return IDNS == ns;
    }

    public boolean hasTagIdentifierNamespace()
    {
        return isTagIdentifierNamespace(IDNS);
    }

    public static boolean isTagIdentifierNamespace(IdentifierNamespace ns)
    {
        return ns == IdentifierNamespace.IDNS_Tag;
    }

    /**
     * Add a declaration to these results and set the lookup result as Found.
     * @param decl
     */
    public void addDecl(Decl.NamedDecl decl)
    {
        decls.add(decl);
        resultKind = Found;
    }

    /**
     * Resolve the result kind of this kind.
     */
    public void resolveKind()
    {
        int n = decls.size();
        if (n == 0)
        {
            assert resultKind == NotFound;
            return;
        }

        // Only a single one found.
        if (n == 1)
        {
            assert resultKind == Found;
            return;
        }
        else
        {
            semaRef.parser.syntaxError(nameLoc, "Resolve getName %s failed", foundName);
            return;
        }
    }

    LookupResult(Sema semaRef, String name, int nameLoc,
            Sema.LookupNameKind lookupNameKind)
    {
        resultKind = NotFound;
        foundName = name;
        this.nameLoc = nameLoc;
        this.semaRef = semaRef;
        decls = new ArrayList<>(8);
    }

    public void clear()
    {
        resultKind = NotFound;
        decls.clear();
    }

    public int getNameLoc()
    {
        return nameLoc;
    }
}
