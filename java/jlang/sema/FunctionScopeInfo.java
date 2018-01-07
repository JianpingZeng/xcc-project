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

import jlang.ast.Tree;
import jlang.ast.Tree.SwitchStmt;

import java.util.Stack;

/**
 * Retains the information of a function or block that is being parsed.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class FunctionScopeInfo
{
    /**
     * Whether this scope information structure defined information for a block.
     */
    public boolean isBlockInfo;
    /**
     * Whether this scope contains any switch or direct goto statement.
     */
    public boolean hasBranchIntoScope;

    /**
     * Whether this scope contains any indirect goto statement.
     */
    public boolean hasIndirectGogo;
    /**
     * This is the current set of active switch statements in the
     * block.
     */
    public Stack<SwitchStmt> switchStack;
    /**
     * The list of return statements that occur within the function or
     * block, if there is any chance of applying the named return value
     * optimization.
     */
    public Stack<Tree.ReturnStmt> returnLists;

    public void setHasBranchIntoScope()
    {
        hasBranchIntoScope = true;
    }

    public void setHasIndirectGoto()
    {
        hasIndirectGogo = true;
    }

    public boolean NeedsScopeChecking()
    {
        return hasIndirectGogo || hasBranchIntoScope;
    }
    public FunctionScopeInfo()
    {
        switchStack = new Stack<>();
        returnLists = new Stack<>();
    }
    /**
     * Clears all of information contained in current parsed scope.
     */
    public void clear()
    {
        hasBranchIntoScope = false;
        hasIndirectGogo = false;
        switchStack.clear();
        returnLists.clear();
    }
}
