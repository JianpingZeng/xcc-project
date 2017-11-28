package backend.LLReader;
/*
 * Xlous C language Compiler
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

import backend.type.Type;
import backend.value.*;
import tools.Pair;
import tools.SourceMgr.SMLoc;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class PerFunctionState
{
    private LLParser parser;
    private Function fn;
    private TreeMap<String, Pair<Value, SMLoc>> forwardRefVals;
    private TreeMap<Integer, Pair<Value, SMLoc>> forwardRefValIDs;
    private ArrayList<Value> numberedVals;

    public PerFunctionState(LLParser p, Function f)
    {
        parser = p;
        fn = f;
        forwardRefVals = new TreeMap<>();
        forwardRefValIDs = new TreeMap<>();
        numberedVals = new ArrayList<>();

        // insert unnamed arguments into the numberedVals list.
        for (Argument arg : fn.getArgumentList())
        {
            if (!arg.hasName())
                numberedVals.add(arg);
        }
    }

    public boolean verifyFunctionComplete()
    {
        return false;
    }

    public BasicBlock defineBB(String name, SMLoc nameLoc)
    {
        return null;
    }

    public boolean setInstName(int nameID, String nameStr, SMLoc nameLoc,
            Instruction instruction)
    {
        return false;
    }

    public Value getVal(int id, Type ty, SMLoc loc)
    {
        // TODO: 2017/11/28
    }

    public Value getVal(String name, Type ty, SMLoc loc)
    {

    }
}
