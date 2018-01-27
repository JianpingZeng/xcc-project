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
    /**
     * The index into Arg being parsed in args list.
     */
    private int index;

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

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int idx)
    {
        index = idx;
    }

    public ArrayList<Arg> getArgs()
    {
        return list;
    }

    public Option getOption(int index)
    {
        assert index >= 0 && index < size();
        return list.get(index).getOption();
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
        if (claim && res != null)
            res.claim();
        return res;
    }

    public abstract String getArgString(int index);

    public Arg getArgs(int index)
    {
        assert index >= 0 && index < list.size();
        return list.get(index);
    }

    public void addAllArgValues(ArrayList<String> cmdArgs, int id)
    {
        addAllArgValues(cmdArgs, id, 0);
    }

    public void addAllArgValues(ArrayList<String> cmdArgs, int id1, int id2)
    {
        for (Arg arg : getArgs())
        {
            if (arg.getOption().matches(id1) || arg.getOption().matches(id2))
            {
                arg.claim();
                for (int i = 0, e = arg.getNumValues(); i < e;i++)
                    cmdArgs.add(arg.getValue(this, i));
            }
        }
    }
    public void addAllArgs(ArrayList<String> cmdArgs, int id, int id2)
    {
        for (Arg arg : getArgs())
        {
            if (arg.getOption().matches(id) || arg.getOption().matches(id2))
            {
                arg.claim();
                for (int i = 0, e = arg.getNumValues(); i < e;i++)
                    cmdArgs.add(arg.getValue(this, i));
            }
        }
    }

    public void addAllArgs(ArrayList<String> cmdArgs, int id)
    {
        addAllArgs(cmdArgs, id, 0);
    }

    public boolean hasFlag(int pos, int neg)
    {
        Arg arg = getLastArg(pos, neg);
        if (arg != null)
            return arg.getOption().matches(pos);
        return true;
    }

    public void addAllArgsTranslated(ArrayList<String> outputs, int id, String optionName, boolean joined)
    {
        for (Arg arg : getArgs())
        {
            if (arg.getOption().matches(id))
            {
                arg.claim();

                if (joined)
                {
                    StringBuilder sb = new StringBuilder(optionName);
                    sb.append(arg.getValue(this, 0));
                    outputs.add(sb.toString());
                }
                else
                {
                    outputs.add(optionName);
                    outputs.add(arg.getValue(this, 0));
                }
            }
        }
    }

    public void addLastArg(ArrayList<String> outputs, int id)
    {
        Arg arg = getLastArg(id, true);
        if (arg != null)
        {
            arg.claim();
            arg.render(this, outputs);
        }
    }
}
