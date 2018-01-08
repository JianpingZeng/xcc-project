/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

package backend.support;

/**
 * This file defines a class used for representing the equivalent class between
 * different integral pair.
 * @author Xlous.zeng
 * @version 0.1
 */
public class IntEqClasses
{
    /**
     * A storage of parent-tree.
     */
    private int[] id;
    /**
     * Number of equivalent classes. It would be reduced after each join.
     */
    private int numClasses;
    /**
     * The number of nodes of each sub-tree rooted in specified index.
     */
    private int[] size;

    public IntEqClasses(int n)
    {
        assert n >= 0:"Can not allocate N(less than 0)'s elements";
        id = new int[n];
        size = new int[n];
        numClasses = n;
        for (int i = 0; i < n; i++)
            id[i] = i;
    }

    public void join(int a, int b)
    {
        int rootA = findLeader(a);
        int rootB = findLeader(b);
        if (rootA == rootB) return;

        if (size[rootA] <= size[rootB])
        {
            id[rootA] = rootB;
            size[rootB] += size[rootA];
        }
        else
        {
            id[rootB] = rootA;
            size[rootB] += size[rootA];
        }
        --numClasses;
    }

    public int findLeader(int a)
    {
        while(a != id[a])
        {
            id[a] = id[id[a]];
            a = id[a];
        }
        return a;
    }

    public int getNumClasses()
    {
        return numClasses;
    }

    public int getNumIds()
    {
        return id.length;
    }

    public int getIDAt(int index)
    {
        assert index >= 0 && index < getNumIds();
        return id[index];
    }
}
