package jlang.sema;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2017, Xlous
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
 * Represents the result of getIdentifier lookup up.
 * @author Xlous.zeng
 * @version 0.1
 */

import jlang.clex.IdentifierInfo;
import jlang.sema.Sema.LookupNameKind;
import jlang.support.SourceLocation;

import java.util.ArrayList;

import static jlang.sema.IdentifierNamespace.*;
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

    /**
     * A name specified to be found.
     */
    private IdentifierInfo foundName;

    /**
     * The location of found name used here for issuing well diagnose message.
     */
    private SourceLocation nameLoc;

    /**
     * The kind of lookup result.
     */
    private LookupResultKind resultKind;
    /**
     * The sema action instance.
     */
    private Sema semaRef;

    /**
     * The kind of lookup to be performed.
     * For details, see {@linkplain LookupNameKind}.
     */
    private LookupNameKind lookupKind;

    /**
     * All found declaration candidate with respect to the specified name.
     */
    private ArrayList<Decl.NamedDecl> decls;

    /**
     * Indicates the getIdentifier space which this getIdentifier in.
     * There are four cases:
     * <ol>
     *     <li>Ordinary Identifier;</li>
     *     <li>Tag Identifier;</li>
     *     <li>Member Identifier;</li>
     *     <li>Label Identifier;</li>
     * </ol>
     */
    private IdentifierNamespace idns;

    public Decl.NamedDecl getFoundDecl()
    {
        switch (getResultKind())
        {
            case NotFound:
                 break;
            case Found:
                return decls.get(0);
            case Ambiguous:
                assert false:"Name lookup returned an ambiguity that could not be handled";
                break;
        }
        return null;
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

    public IdentifierInfo getLookupName()
    {
        return foundName;
    }

    public LookupNameKind getLookupKind()
    {
        return lookupKind;
    }

    /**
     * Returns the identifier namespace mask for this lookup.
     * @return
     */
    public IdentifierNamespace getIdentifierNamespace()
    {
        return idns;
    }

    public boolean isInIdentifierNamespace(IdentifierNamespace ns)
    {
        return idns == ns;
    }

    public boolean hasTagIdentifierNamespace()
    {
        return isTagIdentifierNamespace(idns);
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
        }
        else
        {
            resultKind = Ambiguous;
        }
    }

    public LookupResult
            (Sema semaRef,
            IdentifierInfo name,
            SourceLocation nameLoc,
            LookupNameKind lookupNameKind)
    {
        resultKind = NotFound;
        foundName = name;
        this.nameLoc = nameLoc;
        this.semaRef = semaRef;
        lookupKind = lookupNameKind;
        idns = null;

        switch (lookupKind)
        {
            case LookupOrdinaryName:
                idns = IDNS_Ordinary;
                break;
            case LookupTagName:
                idns = IDNS_Tag;
                break;
            case LookupMemberName:
                idns = IDNS_Member;
                break;
            case LookupLabelName:
                idns = IDNS_Label;
                break;
        }

        decls = new ArrayList<>(8);
    }

    public void clear()
    {
        resultKind = NotFound;
        decls.clear();
    }

    public SourceLocation getNameLoc()
    {
        return nameLoc;
    }

    public void setLookupName(IdentifierInfo name)
    {
        this.foundName = name;
    }
}
