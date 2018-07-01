package jlang.diag;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Xlous Zeng.
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
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Pair;

import java.util.ArrayList;

import static jlang.diag.Diagnostic.DIAG_UPPER_LIMIT;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CustomDiagInfo
{
    private ArrayList<Pair<Diagnostic.Level, String>> diagInfo;
    private TObjectIntHashMap<Pair<Diagnostic.Level, String>> diagIDs;

    public CustomDiagInfo()
    {
        diagIDs = new TObjectIntHashMap<>();
        diagInfo = new ArrayList<>();
    }

    /**
     * Return the text of the specified custom diagnostic.
     * @param diagID
     * @return
     */
    public String getDescription(int diagID)
    {
        Util.assertion(diagID - DIAG_UPPER_LIMIT < diagInfo.size(), "invalid diagnostic ID");

        return diagInfo.get(diagID - DIAG_UPPER_LIMIT).second;
    }

    public Diagnostic.Level getLevel(int diagID)
    {
        Util.assertion(diagID - DIAG_UPPER_LIMIT < diagInfo.size(), "invalid diagnostic ID");

        return diagInfo.get(diagID - DIAG_UPPER_LIMIT).first;
    }

    public int getOrCreateDiagID(Diagnostic.Level l,
            String message,
            Diagnostic diagnostic)
    {
        Pair<Diagnostic.Level, String> diagDesc = Pair.get(l, message);
        if (diagIDs.containsKey(diagDesc))
            return diagIDs.get(diagDesc);

        // If not, assign a new ID.
        int id = diagInfo.size() + DIAG_UPPER_LIMIT;
        diagIDs.put(diagDesc, id);
        diagInfo.add(diagDesc);

        return id;
    }
}
