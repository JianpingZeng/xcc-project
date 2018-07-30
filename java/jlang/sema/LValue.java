package jlang.sema;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Xlous
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

import tools.Util;
import jlang.ast.Tree;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class LValue
{
    Tree.Expr base;
    long offset;

    public final Tree.Expr getLValueBase() { return base;}

    public long getLValueOffset() { return  offset; }

    public APValue moveInto()
    {
        return new APValue(base, offset);
    }

    public void setFrom(final APValue v)
    {
        Util.assertion( v.isLValue());
        base = v.getLValueBase();
        offset= v.getLValueOffset();
    }
}
