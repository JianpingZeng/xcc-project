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

package backend.support;

import backend.value.Value;
import java.util.TreeMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ValueSymbolTable
{
    private TreeMap<String, Value> map;
    private int lastUnique;

    public ValueSymbolTable()
    {
        map = new TreeMap<>();
        lastUnique = 0;
    }

    /**
     * Remove the value associate with the specified name from {@linkplain #map}.
     * @param name  The name to be handled.
     * @return  The old value if the value associated with name exists in map.
     */
    public Value removeValueName(String name)
    {
        // don't operate.
        if (name == null)
            return null;
        return map.remove(name);
    }

    /**
     * This method attempts to create a value name and insert
     * it into the symbol table with the specified name.  If it conflicts, it
     * auto-renames the name and returns that instead.
     * @param name
     * @param value
     * @return
     */
    public String createValueName(String name, Value value)
    {
        // In the common case, the name is not already in the symbol table.
        if (!map.containsKey(name))
        {
            map.put(name, value);
            return name;
        }

        // Otherwise, there is a naming conflict. Rename this value.
        while (true)
        {
            String uniqueName = name + (++lastUnique);
            if (!map.containsKey(uniqueName))
            {
                map.put(uniqueName, value);
                return uniqueName;
            }
        }
    }

    public Value getValue(String name)
    {
        if (name == null || name.isEmpty())
            return null;
        return map.get(name);
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public int size()
    {
        return map.size();
    }

    public TreeMap<String, Value> getMap()
    {
        return map;
    }

    public void dump()
    {
        // TODO: 17-11-7
    }
}
