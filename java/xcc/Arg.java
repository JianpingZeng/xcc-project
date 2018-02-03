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
        return getOption().getID() == id;
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


    public String getAsString(ArgList args)
    {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> outputs = new ArrayList<>();
        render(args, outputs);
        for (int i = 0, e = outputs.size(); i < e; i++)
        {
            if (i != 0)
                sb.append(" ");
            sb.append(outputs.get(i));
        }
        return sb.toString();
    }

    public void renderAsInput(ArgList args, ArrayList<String> outputs)
    {
        if (!getOption().isNoOptAsInput())
        {
            render(args, outputs);
        }

        for (int i = 0, e = getNumValues(); i < e; i++)
            outputs.add(getValue(args, i));
    }

    public abstract void render(ArgList args, ArrayList<String> outputs);


    public static class FlagArg extends Arg
    {
        public FlagArg(Option opt, int index)
        {
            this(opt, index, null);
        }

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
            assert false:"Invalid index!";
            return null;
        }

        @Override
        public void render(ArgList args, ArrayList<String> outputs)
        {
            outputs.add(args.getArgString(getIndex()));
        }
    }

    public static class PositionalArg extends Arg
    {
        public PositionalArg(Option opt, int index)
        {
            this(opt, index, null);
        }

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
            return list.getArgString(getIndex());
        }

        @Override
        public void render(ArgList args, ArrayList<String> outputs)
        {
            outputs.add(args.getArgString(getIndex()));
        }
    }

    public static class JoinedArg extends Arg
    {
        public JoinedArg(Option opt, int index)
        {
            this(opt, index, null);
        }

        public JoinedArg(Option opt, int index, Arg baseArg)
        {
            super(JoinedClass, opt, index, baseArg);
        }

        @Override
        public int getNumValues()
        {
            return 1;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            assert index < getNumValues();
            return list.getArgString(getIndex()).substring(getOption().getName().length());
        }

        @Override
        public void render(ArgList args, ArrayList<String> outputs)
        {
            if (getOption().isForceSeparateRender())
            {
                outputs.add(getOption().getName());
                outputs.add(getValue(args, 0));
            }
            else
            {
                outputs.add(args.getArgString(getIndex()));
            }
        }
    }

    public static class SeparateArg extends Arg
    {
        private int numValues;

        public SeparateArg(Option opt, int index, int numValues)
        {
            this(opt, index, numValues, null);
        }

        public SeparateArg(Option opt, int index, int numValues, Arg baseArg)
        {
            super(SeparateClass, opt, index, baseArg);
            this.numValues = numValues;
            assert numValues > 0;
        }

        @Override
        public int getNumValues()
        {
            return numValues;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            assert index < getNumValues();
            return list.getArgString(getIndex()+index+1);
        }

        @Override
        public void render(ArgList args, ArrayList<String> outputs)
        {
            if (getOption().isForceJoinRender())
            {
                assert getNumValues() == 1;
                String joind = getOption().getName();
                joind += args.getArgString(getIndex());
                outputs.add(joind);
            }
            else
            {
                outputs.add(args.getArgString(getIndex()));
                for (int i = 0, e = getNumValues(); i < e; i++)
                    outputs.add(args.getArgString(getIndex() + i + 1));
            }
        }
    }

    public static class CommaJoinedArg extends Arg
    {
        private ArrayList<String> values;
        public CommaJoinedArg(Option opt, int index, String val)
        {
            this(opt, index, val, null);
        }

        public CommaJoinedArg(Option opt, int index, String val, Arg baseArg)
        {
            super(CommaJoinedClass, opt, index, baseArg);
            values = new ArrayList<>();
            int i = 0;
            int prev = i;
            int len = val.length();
            while (true)
            {
                if (i >= len)
                    break;
                if (val.charAt(i) == ',')
                {
                    values.add(val.substring(prev, i));
                    prev = i+1;
                }
                ++i;
            }
        }

        @Override
        public int getNumValues()
        {
            return values.size();
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            assert index < getNumValues();
            return values.get(index);
        }

        @Override
        public void render(ArgList args, ArrayList<String> outputs)
        {
            outputs.add(args.getArgString(getIndex()));
        }
    }

    public static class JoinedAndSeparateArg extends Arg
    {
        public JoinedAndSeparateArg(Option opt, int index)
        {
            this(opt, index, null);
        }

        public JoinedAndSeparateArg(Option opt, int index, Arg baseArg)
        {
            super(JoinedAndSeparateClass, opt, index, baseArg);
        }

        @Override
        public int getNumValues()
        {
            return 2;
        }

        @Override
        public String getValue(ArgList list, int index)
        {
            assert index < getNumValues();
            if (index == 0)
                return list.getArgString(index).substring(getOption().getName().length());

            return list.getArgString(getIndex()+1);
        }

        @Override
        public void render(ArgList args, ArrayList<String> outputs)
        {
            outputs.add(args.getArgString(getIndex()));
            outputs.add(args.getArgString(getIndex() + 1));
        }
    }
}
