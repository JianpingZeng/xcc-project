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

import static xcc.OptionInfo.OPTION__unknown_Unknown;

public abstract class Option
{
    private int id;
    private OptionClass kind;
    private String name;
    private OptionGroup group;
    private Option alias;
    private boolean unsupported;
    private boolean linkerInput;
    private boolean noOptAsInput;
    private boolean forceSeparateRender;
    private boolean forceJoinRender;
    private boolean driverOption;
    private boolean noArgumentUnused;

    public Option(OptionClass kind, int id, String name,
                  OptionGroup group, Option alias)
    {
        this.kind = kind;
        this.id = id;
        this.name = name;
        this.group = group;
        this.alias = alias;
    }

    public int getID()
    {
        return id;
    }

    public OptionClass getKind()
    {
        return kind;
    }

    public String getName()
    {
        return name;
    }

    public OptionGroup getGroup()
    {
        return group;
    }

    public Option getAlias()
    {
        if (alias == null) return this;
        return alias;
    }

    public boolean isUnsupported()
    {
        return unsupported;
    }

    public boolean isLinkerInput()
    {
        return linkerInput;
    }

    public boolean isNoOptAsInput()
    {
        return noOptAsInput;
    }

    public boolean isForceSeparateRender()
    {
        return forceSeparateRender;
    }

    public boolean isForceJoinRender()
    {
        return forceJoinRender;
    }

    public boolean isDriverOption()
    {
        return driverOption;
    }

    public boolean isNoArgumentUnused()
    {
        return noArgumentUnused;
    }

    public abstract Arg accept(InputArgList list, int index);

    public static class OptionGroup extends Option
    {
        public OptionGroup(int id, String name, OptionGroup group)
        {
            super(OptionClass.GroupClass, id, name, group, null);
        }

        @Override
        public Arg accept(InputArgList list, int index)
        {
            assert false;
            return null;
        }
    }

    public static class InputOption extends Option
    {
        public InputOption()
        {
            super(OptionClass.InputClass, OptionID.OPT__input_,
                    OptionInfo.OPTION__input_Input.optionName, null, null);
        }

        @Override
        public Arg accept(InputArgList list, int index)
        {
            assert false;
            return null;
        }
    }

    public static class UnknownOption extends Option
    {
        public UnknownOption()
        {
            super(OptionClass.UnknownClass, OptionID.OPT__unknown_,
                    OPTION__unknown_Unknown.optionName, null, null);
        }

        @Override
        public Arg accept(InputArgList list, int index) {
            return null;
        }
    }

    public static class FlagOption extends Option
    {

        public FlagOption(int id, String name,
                          OptionGroup group, Option alias)
        {
            super(OptionClass.FlagClass, id, name, group, alias);
        }

        @Override
        public Arg accept(InputArgList list, int index) {
            return null;
        }
    }

    public static class JoinedOption extends Option
    {
        public JoinedOption(int id, String name,
                            OptionGroup group, Option alias)
        {
            super(OptionClass.JoinedClass, id, name, group, alias);
        }

        @Override
        public Arg accept(InputArgList list, int index)
        {
            return null;
        }
    }

    public static class SeparateOption extends Option
    {
        public SeparateOption(int id, String name,
                              OptionGroup group, Option alias)
        {
            super(OptionClass.SeparateClass, id, name, group, alias);
        }

        @Override
        public Arg accept(InputArgList list, int index)
        {
            return null;
        }
    }

    public static class CommaJoinedOption extends Option
    {
        public CommaJoinedOption(int id, String name,
                                 OptionGroup group, Option alias)
        {
            super(OptionClass.CommaJoinedClass, id, name, group, alias);
        }

        @Override
        public Arg accept(InputArgList list, int index)
        {
            return null;
        }
    }

    public static class MultArgsOption extends Option
    {
        public MultArgsOption(int id, String name,
                              OptionGroup group, Option alias)
        {
            super(OptionClass.MultiArgClass, id, name, group, alias);
        }
        @Override
        public Arg accept(InputArgList list, int index)
        {
            return null;
        }
    }

    public static class JoinedOrSeparatedOption extends Option
    {
        public JoinedOrSeparatedOption(int id, String name,
                                       OptionGroup group, Option alias)
        {
            super(OptionClass.JoinedOrSeparateClass, id, name, group, alias);
        }

        @Override
        public Arg accept(InputArgList list, int index)
        {
            return null;
        }
    }

    public static class JoinedAndSeparatedOption extends Option
    {
        public JoinedAndSeparatedOption(int id, String name,
                                        OptionGroup group, Option alias)
        {
            super(OptionClass.JoinedAndSeparateClass, id, name, group, alias);
        }

        @Override
        public Arg accept(InputArgList list, int index)
        {
            return null;
        }
    }
}
