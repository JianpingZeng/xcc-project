/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package jlang.cparser;

import tools.Util;
import jlang.ast.Tree;
import jlang.clex.IdentifierInfo;
import jlang.support.SourceLocation;

import static jlang.cparser.Designator.DesignatorKind.ArrayDesignator;
import static jlang.cparser.Designator.DesignatorKind.ArrayRangeDesignator;
import static jlang.cparser.Designator.DesignatorKind.FieldDesignator;

/**
 * This class is a discriminated union which holds the various
 * different sorts of designators possible.  A Designation is an array of
 * these.  An example of a designator are things like this:
 *     [8] .field [47]        // C99 designation: 3 designators
 *     [8 ... 47]  field:     // GNU extensions: 2 designators
 * These occur in initializers, e.g.:
 *  int a[10] = {2, 4, [8]=9, 10};
 * @author Xlous.zeng
 * @version 0.1
 */
public class Designator
{
    public enum DesignatorKind
    {
        FieldDesignator,
        ArrayDesignator,
        ArrayRangeDesignator
    }

    private DesignatorKind kind;

    public static class FieldDesignatorInfo
    {
        IdentifierInfo ii;
        SourceLocation dotLoc;
        SourceLocation nameLoc;

        public FieldDesignatorInfo(
                IdentifierInfo ii,
                SourceLocation dotLoc,
                SourceLocation nameLoc)
        {
            this.ii = ii;
            this.dotLoc = dotLoc;
            this.nameLoc = nameLoc;
        }
    }

    public static class ArrayDesignatorInfo
    {
        ActionResult<Tree.Expr> index;
        SourceLocation lBracketLoc;
        SourceLocation rBracketLoc;

        public ArrayDesignatorInfo(
                ActionResult<Tree.Expr> index,
                SourceLocation lBracketLoc,
                SourceLocation rBracketLoc)
        {
            this.index = index;
            this.lBracketLoc = lBracketLoc;
            this.rBracketLoc = rBracketLoc;
        }
    }

    public static class ArrayRangeDesignatorInfo
    {
        ActionResult<Tree.Expr> startIdx;
        ActionResult<Tree.Expr> endIdx;
        SourceLocation lBracketLoc;
        SourceLocation ellipsisLoc;
        SourceLocation rBracketLoc;

        public ArrayRangeDesignatorInfo(
                ActionResult<Tree.Expr> startIdx,
                ActionResult<Tree.Expr> endIdx,
                SourceLocation lBracketLoc,
                SourceLocation ellipsisLoc,
                SourceLocation rBracketLoc)
        {
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.lBracketLoc = lBracketLoc;
            this.ellipsisLoc = ellipsisLoc;
            this.rBracketLoc = rBracketLoc;
        }
    }

    /**
     * A generic information about above three kinds.
     */
    private Object info;

    public DesignatorKind getKind()
    {
        return kind;
    }

    public boolean isFieldDesignator()
    {
        return kind == FieldDesignator;
    }

    public boolean isArrayDesignator()
    {
        return kind == ArrayDesignator;
    }

    public boolean isArrayRangeDesignator()
    {
        return kind == ArrayRangeDesignator;
    }

    public FieldDesignatorInfo getFieldInfo()
    {
        Util.assertion( isFieldDesignator());
        return (FieldDesignatorInfo)info;
    }

    public ArrayDesignatorInfo getArrayInfo()
    {
        Util.assertion( isArrayDesignator());
        return (ArrayDesignatorInfo)info;
    }

    public ArrayRangeDesignatorInfo getArrayRangeInfo()
    {
        Util.assertion( isArrayRangeDesignator());
        return (ArrayRangeDesignatorInfo)info;
    }

    public static Designator getField(
            IdentifierInfo ii,
            SourceLocation dotLoc,
            SourceLocation nameLoc)
    {
        Designator d = new Designator();
        d.info = new FieldDesignatorInfo(ii, dotLoc, nameLoc);
        d.kind = DesignatorKind.FieldDesignator;
        return d;
    }

    public static Designator getArray(
            ActionResult<Tree.Expr> index,
            SourceLocation lBracketLoc,
            SourceLocation rBracketLoc)
    {
        Designator d = new Designator();
        d.info = new ArrayDesignatorInfo(index, lBracketLoc, rBracketLoc);
        d.kind = DesignatorKind.ArrayDesignator;
        return d;
    }

    public static Designator getArrayRange(
            ActionResult<Tree.Expr> startIdx,
            ActionResult<Tree.Expr> endIdx,
            SourceLocation lBracketLoc,
            SourceLocation ellipsisLoc,
            SourceLocation rBracketLoc)
    {
        Designator d = new Designator();
        d.info = new ArrayRangeDesignatorInfo(startIdx,
                endIdx, lBracketLoc, ellipsisLoc, rBracketLoc);
        d.kind = DesignatorKind.ArrayRangeDesignator;
        return d;
    }

    public void clearExprs()
    {
        switch (kind)
        {
            case FieldDesignator: return;
            case ArrayDesignator:
                ((ArrayDesignatorInfo)info).index = null;
                return;
            case ArrayRangeDesignator:
                ((ArrayRangeDesignatorInfo)info).startIdx = null;
                ((ArrayRangeDesignatorInfo)info).endIdx = null;
                return;
        }
    }

    public IdentifierInfo getField()
    {
        Util.assertion(isFieldDesignator(), "Invalid accessor");
        return getFieldInfo().ii;
    }

    public SourceLocation getDotLoc()
    {
        Util.assertion( isFieldDesignator());
        return getFieldInfo().dotLoc;
    }

    public SourceLocation getFieldLoc()
    {
        Util.assertion( isFieldDesignator());
        return getFieldInfo().nameLoc;
    }

    public ActionResult<Tree.Expr> getArrayIndex()
    {
        Util.assertion( isArrayDesignator());
        return getArrayInfo().index;
    }

    public ActionResult<Tree.Expr> getArrayRangeStart()
    {
        Util.assertion( isArrayRangeDesignator());
        return getArrayRangeInfo().startIdx;
    }

    public ActionResult<Tree.Expr> getArrayRangeEnd()
    {
        Util.assertion( isArrayRangeDesignator());
        return getArrayRangeInfo().endIdx;
    }

    public SourceLocation getLBracketLoc()
    {
        Util.assertion( isArrayDesignator() || isArrayRangeDesignator());
        if (isArrayDesignator())
            return getArrayInfo().lBracketLoc;
        else
            return getArrayRangeInfo().lBracketLoc;
    }

    public SourceLocation getRBracketLoc()
    {
        Util.assertion( isArrayDesignator() || isArrayRangeDesignator());
        if (isArrayDesignator())
            return getArrayInfo().rBracketLoc;
        else
            return getArrayRangeInfo().rBracketLoc;
    }

    public SourceLocation getEllipsisLoc()
    {
        Util.assertion( isArrayRangeDesignator());
        return getArrayRangeInfo().ellipsisLoc;
    }
}
