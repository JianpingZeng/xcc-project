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


import static xcc.Arg.ArgClass.*;

public abstract class Arg
{
    enum ArgClass
    {
        FlagClass,
        PositionalClass,
        JoinedClass,
        SeparateClass,
        CommaJoinedClass,
        JoinedAndSeparateClass
    }

    private boolean claim;

    /**
     * The index to InputArgList.
     */
    private int index;

    private ArgClass kind;
    private Option option;

    private Arg baseArg;

    public Arg(ArgClass kind, Option opt, int index, Arg baseArg)
    {
        this.kind = kind;
        option = opt;
        this.index = index;
        this.baseArg = baseArg;
    }

    public Arg(ArgClass kind, Option opt, int index)
    {
        this(kind, opt, index, null);
    }

    public ArgClass getKind()
    {
        return kind;
    }

    public void claim()
    {
        getBaseArg().claim = true;
    }

    public boolean isClaimed()
    {
        return claim;
    }

    public boolean match(int id)
    {
        return false;
    }

    public int getIndex()
    {
        return index;
    }

    public Option getOption()
    {
        return option;
    }

    public Arg getBaseArg()
    {
        return baseArg != null ? baseArg : this;
    }

    public void setBaseArg(Arg baseArg)
    {
        this.baseArg = baseArg;
    }

    public abstract int getNumValues();

    public abstract String getValue(ArgList list, int index);

    public static class FlagArg extends Arg
    {

        public FlagArg(Option opt, int index, Arg baseArg)
        {
            super(FlagClass, opt, index, baseArg);
        }

        @Override
        public int getNumValues()
        {
            return 0;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            return null;
        }
    }

    public static class PositionalArg extends Arg
    {

        public PositionalArg(Option opt, int index, Arg baseArg)
        {
            super(PositionalClass, opt, index, baseArg);
        }

        @Override
        public int getNumValues()
        {
            return 0;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            return null;
        }
    }

    public static class JoinedArg extends Arg
    {
        public JoinedArg(Option opt, int index, Arg baseArg)
        {
            super(JoinedClass, opt, index, baseArg);
        }

        @Override
        public int getNumValues()
        {
            return 0;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            return null;
        }
    }

    public static class SeparateArg extends Arg
    {
        public SeparateArg(Option opt, int index, Arg baseArg)
        {
            super(SeparateClass, opt, index, baseArg);
        }

        @Override
        public int getNumValues()
        {
            return 0;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            return null;
        }
    }

    public static class CommaJoinedArg extends Arg
    {
        public CommaJoinedArg(Option opt, int index, Arg baseArg)
        {
            super(CommaJoinedClass, opt, index, baseArg);
        }

        @Override
        public int getNumValues()
        {
            return 0;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            return null;
        }
    }

    public static class JoinedAndSeparateArg extends Arg
    {
        public JoinedAndSeparateArg(Option opt, int index, Arg baseArg)
        {
            super(JoinedAndSeparateClass, opt, index, baseArg);
        }

        @Override
        public int getNumValues()
        {
            return 0;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            return null;
        }
    }
}
