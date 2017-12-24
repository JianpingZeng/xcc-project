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

package backend.codegen.pbqp;

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PBQPVector implements Cloneable
{
    private double[] data;

    public PBQPVector(int len)
    {
        data = new double[len];
    }

    public PBQPVector(double[] data)
    {
        this.data = data;
    }

    public void set(int idx, double val)
    {
        assert idx>=0 && idx < data.length;
        data[idx] = val;
    }

    public double get(int idx)
    {
        assert idx>=0 && idx < data.length;
        return data[idx];
    }

    public double[] getData()
    {
        return data;
    }

    public void add(PBQPVector other)
    {
        assert other!= null && other.data.length == data.length;
        for (int i = 0; i < data.length; i++)
            data[i] += other.data[i];
    }

    public void add(double[] row)
    {
        assert row != null && row.length == data.length;
        for (int i = 0; i < data.length; i++)
            data[i] += row[i];
    }

    public int getLength()
    {
        return data.length;
    }

    /**
     * Get the index of minimal element.
     * @return
     */
    public int minIndex()
    {
        double min = Double.MIN_VALUE;
        int minIdx = -1;
        for (int i = 0; i < data.length; i++)
        {
            if (data[i] < min)
            {
                minIdx = i;
                min = data[i];
            }
        }
        return minIdx;
    }

    @Override
    public PBQPVector clone()
    {
        double[] val = new double[data.length];
        System.arraycopy(data, 0, val, 0, val.length);
        return new PBQPVector(val);
    }

    public double min()
    {
        double min = Double.MIN_VALUE;
        for (int i = 0; i < data.length; i++)
        {
            if (data[i] < min)
                min = data[i];
        }
        return min;
    }

    public void print(PrintStream os)
    {
        if (os != null)
        {
            os.printf("[");
            if (data.length != 0)
                os.printf("%f", data[0]);
            for (int i = 1; i < data.length; i++)
                os.printf(", %f", data[i]);
            os.println("]");
        }
    }

    public void dump()
    {
        print(System.err);
    }
}
