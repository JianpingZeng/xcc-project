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

import static xcc.JlangFlags.RenderAsInput;
import static xcc.JlangFlags.RenderJoined;
import static xcc.OptionID.OPT_INVALID;
import static xcc.OptionID.OPT_LastOption;
import static xcc.OptionKind.*;

public class OptTable
{
    private Option[] options;
    private int firstSearchableOption;
    private OptionInfo[] infos = OptionInfo.values();
    public OptTable()
    {
        options = new Option[infos.length];
        for (int i = OptionID.OPT__unknown_ + 2; i < OPT_LastOption; i++)
        {
            if (getOption(i).getKind() != OptionClass.GroupClass)
            {
                firstSearchableOption = i;
                break;
            }
        }
        assert firstSearchableOption < getNumOptions();
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
            case KIND_CommaJoined:
                res = new Option.CommaJoinedOption(id, info.optionName, group, alias);
                break;
            case KIND_Flag:
                res = new Option.FlagOption(id, info.optionName, group, alias);
                break;
            case KIND_Input:
                res = Option.InputOption.staticFactory();
                break;
            case KIND_Joined:
                res = new Option.JoinedOption(id,info.optionName, group, alias);
                break;
            case KIND_JoinedAndSeparate:
                res = new Option.JoinedAndSeparatedOption(id, info.optionName, group, alias);
                break;
            case KIND_JoinedOrSeparate:
                res = new Option.JoinedOrSeparatedOption(id, info.optionName, group, alias);
                break;
            case KIND_MultiArg:
                res = new Option.MultArgsOption(id, info.optionName, group, alias, info.param);
                break;
            case KIND_Separate:
                res = new Option.SeparateOption(id, info.optionName, group, alias);
                break;
            case KIND_Unknown:
                res = Option.UnknownOption.staticFactory();
                break;
            default:
                assert false:"Unknown Option kind!";
        }
        assert res != null;
        int flags = info.flags;
        if ((flags & JlangFlags.CC1Option) != 0)
            res.setCC1Option(true);
        if ((flags & JlangFlags.CLOption) != 0)
            res.setCLOption(true);
        if ((flags & JlangFlags.CoreOption) != 0)
            res.setCoreOption(true);
        if ((flags & JlangFlags.DriverOption) != 0)
            res.setDriverOption(true);
        if ((flags & JlangFlags.LinkerInput) != 0)
            res.setLinkerInput(true);
        if ((flags & JlangFlags.NoArgumentUnused) != 0)
            res.setNoArgumentUnused(true);
        if ((flags & JlangFlags.Unsupported) != 0)
            res.setUnsupported(true);
        if ((flags & JlangFlags.HelpHidden) != 0)
            res.setHelpHidden(true);
        if ((flags & JlangFlags.NoForward) != 0)
            res.setNoForward(true);
        if ((flags & RenderJoined) != 0)
            res.setRenderJoined(true);
        if ((flags & RenderAsInput) != 0)
            res.setRenderAsInput(true);
        return res;
    }

    public Arg parseOneArg(ArgList argList)
    {
      int idx = argList.getIndex();
      String name = argList.getArgString(idx);
      if (!name.startsWith("-"))
      {
        argList.setIndex(idx+1);
        return new PositionalArg(getOption(OPT__input_), idx);
      }

      OptionInfo[] opts = OptionInfo.values();

      // looking for the specified OptionInfo matches with option name.
      int i = 0;
      for (; i < opts.length; i++)
        if (opts[i].optionName.equals(name))
          target = opts[i];

      // If specified Option not found, just treat it as PositionalArg with
      // OPT__unknown_.
      if (i == opts.length)
      {
        argList.setIndex(idx+1);
        return new PositionalArg(getOption(OPT__unknown_), idx);
      }
      Option opt = getOption(i-1);
      return opt.accept(argList);
    }
}
