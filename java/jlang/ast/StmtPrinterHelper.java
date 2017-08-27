/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

package jlang.ast;

import jlang.support.LangOptions;
import tools.Pair;

import java.io.PrintStream;
import java.util.HashMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class StmtPrinterHelper implements PrinterHelper
{
    private HashMap<Tree.Stmt, Pair<Integer, Integer>> stmtMap;
    private int currentBlock;
    private int currentStmt;
    private LangOptions langOpts;

    public StmtPrinterHelper(CFG cfg, LangOptions opts)
    {
        stmtMap = new HashMap<>();
        currentBlock = 0;
        currentStmt = 0;
        langOpts = opts;
    }

    @Override
    public boolean handledStmt(Tree.Stmt stmt, PrintStream os)
    {
        return false;
    }
}
