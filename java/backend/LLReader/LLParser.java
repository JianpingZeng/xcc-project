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

package backend.LLReader;

import backend.type.OpaqueType;
import backend.type.PATypeHandle;
import backend.type.Type;
import backend.value.GlobalValue;
import backend.value.MetadataBase;
import backend.value.Module;
import gnu.trove.map.hash.TIntObjectHashMap;
import jlang.support.MemoryBuffer;
import tools.Pair;
import tools.SMDiagnostic;
import tools.SourceMgr;
import tools.SourceMgr.SMLoc;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LLParser
{
    public static class UpRefRecord
    {
        SMLoc loc;
        int nestedLevel;
        Type lastContainedTy;
        OpaqueType upRefTy;

        public UpRefRecord(SMLoc loc, int nestedLevel,
                Type lastContainedTy, OpaqueType upRefTy)
        {
            this.loc = loc;
            this.nestedLevel = nestedLevel;
            this.lastContainedTy = lastContainedTy;
            this.upRefTy = upRefTy;
        }
    }

    private LLLexer lexer;
    private Module m;
    private TreeMap<String, Pair<PATypeHandle, SMLoc>> forwardRefTypes;
    private TIntObjectHashMap<Pair<PATypeHandle, SMLoc>> forwardRefTypeIDs;
    private ArrayList<PATypeHandle> numberedTypes;
    private TIntObjectHashMap<MetadataBase> metadataCache;
    private TIntObjectHashMap<Pair<MetadataBase, SMLoc>> forwardRefMDNodes;
    private ArrayList<UpRefRecord> upRefs;

    private TreeMap<String, Pair<GlobalValue, SMLoc>> forwardRefVals;
    private TIntObjectHashMap<Pair<GlobalValue, SMLoc>> forwardRefValIDs;
    private ArrayList<GlobalValue> numberedVals;

    public LLParser(MemoryBuffer buf, SourceMgr smg, SMDiagnostic diag, Module m)
    {
        lexer = new LLLexer(buf, smg, diag);
        this.m = m;
        forwardRefTypes = new TreeMap<>();
        forwardRefTypeIDs = new TIntObjectHashMap<>();
        numberedTypes = new ArrayList<>();
        metadataCache = new TIntObjectHashMap<>();
        forwardRefMDNodes = new TIntObjectHashMap<>();
        upRefs = new ArrayList<>();
        forwardRefVals = new TreeMap<>();
        forwardRefValIDs = new TIntObjectHashMap<>();
        numberedVals = new ArrayList<>();
    }

    /**
     * The entry to parse input ll file.
     * <pre>
     *  module ::= toplevelentity*
     * </pre>
     * @return
     */
    public boolean run()
    {
        // obtain a token.
        lexer.lex();
        return parseTopLevelEntities() || validateEndOfModule();
    }

    private boolean parseTopLevelEntities()
    {
        return true;
    }

    private boolean validateEndOfModule()
    {
        return true;
    }
}
