package tools.commandline;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import tools.OutRef;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class Opt<T> extends Option<T>
{
    public Opt(Parser<T> parser, Modifier... mods)
    {
        super(parser, NumOccurrences.Optional.value);
        for(Modifier mod : mods)
            mod.apply(this);

        done();
    }

    @Override
    protected boolean handleOccurrence(int pos, String optionName, String arg)
    {
        T val;
        OutRef<T> x = new OutRef<>();
        if (parser.parse(this, optionName, arg, x))
            return true;
        val = x.get();
        setValue(val);
        setPosition(pos);
        return false;
    }

    @Override
    protected ValueExpected getValueExpectedDefault()
    {
        return parser.getValueExpectedFlagDefault();
    }

    @Override
    public void getExtraOptionNames(ArrayList<String> names)
    {
        parser.getExtraOptionNames(names);
    }

    @Override
    public int getOptionWidth()
    {
        return parser.getOptionWidth(this);
    }

    @Override
    public void printOptionInfo(int globalWidth)
    {
        parser.printOptionInfo(this, globalWidth);
    }

    @Override
    public void setInitializer(T val)
    {
        setValue(val);
    }

    private void done()
    {
        addArgument();
        parser.initialize(this);
    }
}
