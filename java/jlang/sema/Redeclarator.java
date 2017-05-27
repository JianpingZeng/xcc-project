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

/**
 * Provides common interface for the Decls that can be redeclared.
 * @author Xlous.zeng
 * @version 0.1
 */
public class Redeclarator<T extends Redeclarable<T>> implements Redeclarable<T>
{
    public static class DeclLink<T extends Redeclarable<T>>
    {
        T decl;
        private boolean isLatest;
        DeclLink(T d, boolean isLatest)
        {
            decl = d;
            this.isLatest = isLatest;
        }

        boolean nextIsPrevious()
        {
            return !isLatest;
        }

        boolean nextIsLatest()
        {
            return isLatest;
        }

        T getNext()
        {
            return decl;
        }
    }

    public static class PreviousDeclLink<T extends Redeclarable<T>> extends DeclLink<T>
    {
        PreviousDeclLink(T d)
        {
            super(d, false);
        }
    }

    public static class LatestDeclLink<T extends Redeclarable<T>> extends DeclLink<T>
    {
        LatestDeclLink(T d)
        {
            super(d, true);
        }
    }

    private DeclLink<T> redeclLink;

    public Redeclarator()
    {
        redeclLink = new LatestDeclLink<T>((T)this);
    }

    public T getPreviousDeclaration()
    {
        if(redeclLink.nextIsPrevious())
            return redeclLink.getNext();
        return null;
    }

    public T getFirstDeclaration()
    {
        Redeclarable d = this;
        while (d.getPreviousDeclaration() != null)
        {
            d = d.getPreviousDeclaration();
        }
        return (T)d;
    }

    public void setPreviousDeclaration(T prevDecl)
    {
        Redeclarable first;
        if (prevDecl != null)
        {
            redeclLink = new PreviousDeclLink<T>(prevDecl);
            first = prevDecl.getFirstDeclaration();
            assert ((Redeclarator)first).redeclLink.nextIsLatest() :"Expected first!";
        }
        else
        {
            first = this;
        }

        ((Redeclarator)first).redeclLink = new LatestDeclLink<T>((T)this);
    }

    public DeclLink<T> getRedeclLink()
    {
        return redeclLink;
    }

    @Override
    public boolean hasNext()
    {
        T Next = redeclLink.getNext();
        return Next != this;
    }

    @Override
    public T next()
    {
        T Next = redeclLink.getNext();
        return Next != this ? Next : null;
    }
}
