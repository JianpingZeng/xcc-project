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

package backend.codegen.pbqp;

import java.util.Arrays;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PBQPGraph
{
    public int numNodes;
    public int maxDegree;
    public boolean solved;
    public boolean optimal;
    public double min;
    public boolean changed;

    public PBQPVector[] nodeCosts;
    public int[] nodeDegrees;
    public int[] solution;
    public AdjNode[] adjList;
    public BucketNode[] bucketNodes;
    public BucketNode[] bucketList;

    public int[] stack;
    public int stackPtr;

    public PBQPGraph(int numNodes)
    {
        assert numNodes > 0;
        this.numNodes = numNodes;
        solved = false;
        optimal = true;
        min = 0;
        maxDegree = 0;
        changed = false;
        stack = new int[numNodes];
        stackPtr = 0;

        adjList = new AdjNode[numNodes];
        nodeDegrees = new int[numNodes];
        solution = new int[numNodes];
        bucketNodes = new BucketNode[numNodes];
        nodeCosts = new PBQPVector[numNodes];
        bucketList = null;
        Arrays.fill(solution, -1);
    }

    public void addNodeCosts(int node, PBQPVector costs)
    {
        assert costs != null;
        assert node >= 0 && node <= numNodes;
        if (nodeCosts[node] == null)
        {
            nodeCosts[node] = costs;
        }
        else
        {
            nodeCosts[node].add(costs);
        }
    }

    public void addEdgeCosts(int node1, int node2, PBQPMatrix costs)
    {
        assert node1 >= 0 && node1 <= numNodes;
        assert node2 >= 0 && node2 <= numNodes;
        assert costs != null;

        PBQPMatrix m;
        // does this edge exists?
        if (node1 == node2)
        {
            addNodeCosts(node1, new PBQPVector(costs.getDiagonalize()));
        }
        else if ((m = getCostMatrix(node1, node2)) != null)
        {
            if (node1 < node2)
                m.add(costs);
            else
                m.add(costs.transpose());
        }
        else
        {
            insertEdge(node1, node2, node1 < node2 ? costs : costs.transpose());
        }
    }

    public PBQPMatrix getCostMatrix(int node1, int node2)
    {
        assert node1 >= 0 && node1 <= numNodes;
        assert node2 >= 0 && node2 <= numNodes;
        if (adjList[node1] == null)
            return null;

        AdjNode head = adjList[node1];
        while (head != null)
        {
            if (head.adj == node2)
                return head.cost;
            head = head.next;
        }
        return null;
    }

    private void insertEdge(int node1, int node2, PBQPMatrix cost)
    {
        AdjNode adj1 = new AdjNode(node2, cost);
        AdjNode adj2 = new AdjNode(node1, cost);
        insertAdjNode(node1, adj1);
        insertAdjNode(node2, adj2);
        adj1.reverse = adj2;
        adj2.reverse = adj1;
    }

    private void insertAdjNode(int node, AdjNode adj)
    {
        assert node >= 0 && node <= numNodes;
        assert adj != null;

        if (adjList[node] == null)
        {
            adjList[node] = adj;
            return;
        }
        // insert the adj as first
        adjList[node].prev = adj;
        adj.next = adjList[node];
        adj.prev = null;
        adjList[node] = adj;
    }

    public AdjNode findAdjNode(int u, int v)
    {
        if (adjList[u] == null) return null;

        for (AdjNode adj = adjList[u]; adj != null; adj = adj.next)
            if (adj.adj == v)
                return adj;
        return null;
    }

    public void deleteEdge(int u, int v)
    {
        assert u >= 0 && u < numNodes;
        assert v >= 0 && v < numNodes;
        AdjNode adj = findAdjNode(u,v);
        assert adj != null;
        assert adj.reverse != null;

        AdjNode reverse = adj.reverse;
        removeAdjNode(u, adj);
        removeAdjNode(v, reverse);
    }

    public void removeAdjNode(int u, AdjNode adj)
    {
        assert u >= 0 && u < numNodes;
        assert adj != null;
        AdjNode prev = adj.prev;
        if (prev == null)
        {
            adjList[u] = adj.next;
        }
        else
        {
            prev.next = adj.next;
        }

        if (adj.next != null)
            adj.next.prev = adj.prev;

        if (adj.reverse != null)
        {
            adj.reverse.reverse = null;
        }
    }

    /**
     * Pop a bucket node of degree from {@linkplain #bucketList}.
     * @param degree
     * @return
     */
    public int popNode(int degree)
    {
        assert degree >= 0 && degree <= maxDegree;
        assert bucketList != null;

        BucketNode bucket = bucketList[degree];
        assert bucket != null;

        removeBucket(bucket);
        return bucket.u;
    }

    public void removeBucket(BucketNode bucket)
    {
        assert bucket != null;
        if (bucket.prev != null)
            bucket.prev.next = bucket.next;
        else
            bucketList[nodeDegrees[bucket.u]] = bucket.next;

        if (bucket.next != null)
            bucket.next.prev = bucket.prev;
        bucket.next = bucket.prev = null;
    }

    public void reinsertNode(int u)
    {
        assert u >= 0 && u < numNodes;
        assert adjList != null;
        for (AdjNode adj = adjList[u]; adj != null; adj = adj.next)
        {
            int v = adj.adj;
            AdjNode adjV = new AdjNode(u, adj.cost);
            insertAdjNode(v, adjV);
        }
    }

    public void determineSolution(int u)
    {
        assert u >= 0 && u < numNodes;
        assert adjList != null;
        assert solution != null;
        PBQPVector vec = nodeCosts[u].clone();
        for (AdjNode adj = adjList[u]; adj != null; adj = adj.next)
        {
            int v = adj.adj;
            int vSol = solution[v];

            PBQPMatrix m = getCostMatrix(v, u);
            assert vSol >= 0 && vSol < nodeCosts[v].getLength();
            vec.add(m.getRows(vSol));
        }
        solution[u] = vec.minIndex();
    }

    public int popMaxNode()
    {
        for (int deg = maxDegree; deg > 2; deg--)
        {
            BucketNode bucket;
            if ((bucket = bucketList[deg]) != null)
            {
                removeBucket(bucket);
                return bucket.u;
            }
        }
        return -1;
    }

    public void removeNode(int u)
    {
        assert u >= 0 && u < numNodes;
        assert adjList != null;

        for (AdjNode adj = adjList[u]; adj != null; adj = adj.next)
            removeAdjNode(adj.adj, adj.reverse);
    }

    /**
     * Adjust adjecent node of specified node according to it's degree
     * @param u
     */
    public void reorderAdjNode(int u)
    {
        assert u >= 0 && u < numNodes;
        assert adjList != null;
        for (AdjNode adj = adjList[u]; adj != null; adj = adj.next)
            reorderNode(adj.adj);
    }

    public void reorderNode(int u)
    {
        int deg = getDeg(u);
        if (deg != nodeDegrees[u])
        {
            removeBucket(bucketNodes[u]);
            addToBucketList(bucketNodes[u], deg);
        }
    }

    public void addToBucketList(BucketNode node, int degree)
    {
        assert node != null;
        assert degree >= 0 && degree <= maxDegree;
        assert bucketList != null;

        nodeDegrees[node.u] = degree;

        node.prev = null;
        node.next = bucketList[degree];
        if (node.next != null)
            node.next.prev = node;

        bucketList[degree] = node;
    }

    public int getDeg(int u)
    {
        assert u >= 0 && u < numNodes;
        int deg = 0;
        for (AdjNode adj = adjList[u]; adj != null; adj = adj.next)
            ++deg;
        return deg;
    }
}
