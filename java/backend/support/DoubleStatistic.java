package backend.support;
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

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class DoubleStatistic extends StatisticBase
{
    private double num;
    public DoubleStatistic(String name, String desc)
    {
        this(0, name, desc);
    }

    public DoubleStatistic(double val, String name, String desc)
    {
        super(name, desc);
        num = val;
    }

    @Override
    public void printValue(PrintStream os)
    {
        os.print(num);
    }

    @Override
    public boolean hasSomeValue()
    {
        return num != 0;
    }

    public void add(int amount)
    {
        num += amount;
    }

    public void sub(int amount)
    {
        num -= amount;
    }

    public void inc()
    {
        ++num;
    }

    public void dec()
    {
        --num;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        return num == ((DoubleStatistic)obj).num;
    }

    @Override
    public int hashCode()
    {
        return super.hashCode() << 11 + (int)num;
    }
}
