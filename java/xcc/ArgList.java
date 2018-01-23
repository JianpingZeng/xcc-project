/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous zeng.
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

package xcc;

import java.util.ArrayList;

public abstract class ArgList
{
    private ArrayList<Arg> list;

    public ArgList(ArrayList<Arg> list)
    {
        this.list = list;
    }

    public void add(Arg arg)
    {
        list.add(arg);
    }

    public int size()
    {
        return list.size();
    }

    public ArrayList<Arg> getArgs()
    {
        return list;
    }

    public void setArg(ArrayList<Arg> list)
    {
        this.list = list;
    }

    public boolean hasArg(int id)
    {
        return hasArg(id, true);
    }

    public boolean hasArg(int id, boolean claim)
    {
        return getLastArg(id, claim) != null;
    }

    public boolean hasArg(int id1, int id2)
    {
        return hasArg(id1, id2, true);
    }

    public boolean hasArg(int id1, int id2, boolean claim)
    {
        return getLastArg(id1, id2, claim) != null;
    }

    public Arg getLastArg(int id, boolean claim)
    {
        for (int i = list.size() - 1; i >= 0; i--)
        {
            Arg arg = list.get(i);
            if (arg.match(id))
            {
                if (claim)
                    arg.claim();
                return arg;
            }
        }
        return null;
    }

    public Arg getLastArg(int id1, int id2)
    {
        return getLastArg(id1, id2, true);
    }

    public Arg getLastArg(int id1, int id2, boolean claim)
    {
        Arg arg1 = getLastArg(id1, false);
        Arg arg2 = getLastArg(id2, false);
        Arg res;
        if (arg1 != null && arg2 != null)
            res = arg1.getIndex() > arg2.getIndex() ? arg2 : arg1;
        else
            res = arg1 != null ? arg1 : arg2;
        if (claim)
            res.claim();
        return res;
    }

    public abstract String getArgString(int index);
}
