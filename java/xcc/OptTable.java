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

import xcc.Option.OptionGroup;

import static xcc.OptionID.OPT_INVALID;
import static xcc.OptionID.OPT_LastOption;

public class OptTable
{
    private Option[] options;
    private int firstSearchableOption;
    private OptionInfo[] infos = OptionInfo.values();
    public OptTable()
    {
        options = new Option[infos.length];
        for (int i = OptionID.OPT__unknown_ + 1; i < OPT_LastOption; i++)
        {

        }
    }

    public int getNumOptions()
    {
        return infos.length;
    }

    public String getOptionName(int id)
    {
        return infos[id].optionName;
    }

    public String getHelpText(int id)
    {
        return infos[id].helpText;
    }

    public String getMetaVarName(int id)
    {
        return infos[id].metaVarName;
    }

    public Option getOption(int id)
    {
        if (id == OPT_INVALID)
            return null;

        assert id < getNumOptions() - 1;
        if (options[id - 1] != null)
            return options[id - 1];
        else
        {
            return options[id - 1] = constructOption(id);
        }
    }

    public Option constructOption(int id)
    {
        OptionInfo info = infos[id];
        Option opt =  getOption(info.group);
        OptionGroup group = opt instanceof OptionGroup ? (OptionGroup)opt : null;
        Option alias = getOption(info.alias);

        Option res = null;
        switch (info.kind)
        {
            case OptionKind.KIND_Input:
                break;
        }
        return res;
    }
}
