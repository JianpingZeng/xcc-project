/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import tools.Util;

/**
 * A PBQP problem solver using heuristic algorithm.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class HeuristicSolver implements PBQPSolver {
  /**
   * Check vector & matrix dimensions
   *
   * @param problem
   */
  private void checkPBQP(PBQPGraph problem) {
    for (int u = 0; u < problem.numNodes; u++) {
      Util.assertion(problem.nodeCosts[u] != null);
      for (AdjNode adj = problem.adjList[u]; adj != null; adj = adj.next) {
        int v = adj.adj;
        Util.assertion(v >= 0 && v <= problem.numNodes);
        if (u < v) {
          PBQPMatrix cost = adj.cost;
          Util.assertion(cost.rows == problem.nodeCosts[u].getLength() && cost.columns == problem.nodeCosts[v].getLength());

        }
      }
    }
  }

  private PBQPGraph graph;

  @Override
  public PBQPSolution solve(PBQPGraph problem) {
    Util.assertion(problem != null);
    /* check vector & matrix dimensions */
    checkPBQP(problem);

    graph = problem;
    if (Util.DEBUG) {
      System.err.println("############## Simplify PBQP problem ###################");

      // Step#1: Eliminate trivial nodes that with cost vectors of length one.
      System.err.println("Step#1: eliminate trivial nodes");
    }
    eliminateTrivialNodes();

    if (Util.DEBUG) {
      System.err.println("Step#2: eliminate independent edges");
    }
    eliminateIndependentEdges();

    // create bucket list for graph parsing.
    if (Util.DEBUG)
      System.err.println("Step#3: create bucket list for graph parsing");
    createBucketlist();

    // reduce phase
    if (Util.DEBUG)
      System.err.println("Step#4: Reduce PBQP graph");
    reducePBQPGraph();

    // Solve trivial graph.
    if (Util.DEBUG)
      System.err.println("Step#5: Solve trivial graph");
    solveTrivialNodes();

    // Back propagation phase
    if (Util.DEBUG)
      System.err.println("Step#6: Back propagation");
    backPropagate();
    problem.solved = true;
    return new PBQPSolution(problem.solution);
  }

  private void eliminateTrivialNodes() {
    for (int u = 0; u < graph.numNodes; u++) {
      if (graph.nodeCosts[u] != null && graph.nodeCosts[u].getLength() == 1)
        disconnectTrivialNode(u);
    }
  }

  private void disconnectTrivialNode(int u) {
    for (AdjNode adj = graph.adjList[u]; adj != null; adj = adj.next) {
      int v = adj.adj;
      Util.assertion(v >= 0 && v < graph.numNodes);

      PBQPMatrix costs = graph.getCostMatrix(u, v);
      graph.nodeCosts[v].add(new PBQPVector(costs.getRows(0)));
      graph.deleteEdge(u, v);
    }
  }

  private void eliminateIndependentEdges() {
    graph.changed = false;
    for (int u = 0; u < graph.numNodes; u++) {
      for (AdjNode adj = graph.adjList[u]; adj != null; adj = adj.next) {
        int v = adj.adj;
        Util.assertion(v >= 0 && v < graph.numNodes);
        if (u < v) {
          simplifyEdge(u, v);
        }
      }
    }
  }

  private void simplifyEdge(int u, int v) {
    if (u > v) {
      u = u ^ v;
      v = u ^ v;
      u = u ^ v;
    }
    PBQPMatrix costs = graph.getCostMatrix(u, v);
    boolean isZero = normalizeMatrix(costs, graph.nodeCosts[u], graph.nodeCosts[v]);
    if (isZero) {
      graph.deleteEdge(u, v);
      graph.changed = true;
    }
  }

  private boolean normalizeMatrix(PBQPMatrix costs, PBQPVector u, PBQPVector v) {
    int rows = costs.rows;
    int columns = costs.columns;
    Util.assertion(rows == u.getLength() && columns == v.getLength());

    for (int r = 0; r < rows; r++) {
      double min = costs.getRowMin(r);
      u.set(r, u.get(r) + min);
      if (min != Double.MAX_VALUE)
        costs.subRow(r, min);
      else {
        costs.setRow(r, 0.0);
      }
    }

    for (int c = 0; c < rows; c++) {
      double min = costs.getColumnMin(c);
      v.set(c, v.get(c) + min);
      if (min != Double.MAX_VALUE)
        costs.subCol(c, min);
      else {
        costs.setCol(c, 0.0);
      }
    }
    return costs.isZero();
  }

  private void createBucketlist() {
    int maxDeg = 2;
    for (int u = 0; u < graph.numNodes; u++) {
      int deg = graph.nodeDegrees[u] = graph.getDeg(u);
      if (deg > maxDeg)
        maxDeg = deg;
    }
    graph.maxDegree = maxDeg;
    graph.bucketList = new BucketNode[maxDeg + 1];

    // insert nodes to the bucketlist
    for (int u = 0; u < graph.numNodes; u++)
      createBucket(u, graph.nodeDegrees[u]);
  }

  private void createBucket(int u, int degree) {
    Util.assertion(u >= 0 && u < graph.numNodes);
    BucketNode node = new BucketNode(u);
    graph.bucketNodes[u] = node;
    graph.addToBucketList(node, degree);
  }

  private void reducePBQPGraph() {
    Util.assertion(graph.bucketList != null);
    int round = 1;
    int u;
    while (true) {
      if (graph.bucketList[1] != null) {
        u = graph.popNode(1);
        if (Util.DEBUG) {
          System.err.printf("Round %d, RI-Reduction of Node n%d%n",
              round++, u);
        }
        applyRI(u);
      } else if (graph.bucketList[2] != null) {
        u = graph.popNode(2);
        if (Util.DEBUG) {
          System.err.printf("Round %d, RII-Reduction of Node n%d%n",
              round++, u);
        }
        applyRII(u);
      } else if ((u = graph.popMaxNode()) != -1) {
        if (Util.DEBUG) {
          System.err.printf("Round %d, RN-Reduction of Node n%d%n",
              round++, u);
        }
        applyRN(u);
      } else {
        break;
      }
    }
  }

  /**
   * Solve trivial nodes of degree zero.
   */
  private void solveTrivialNodes() {
    Util.assertion(graph.bucketList != null);
    while (graph.bucketList[0] != null) {
      int u = graph.popNode(0);
      Util.assertion(u >= 0 && u < graph.numNodes);
      graph.solution[u] = graph.nodeCosts[u].minIndex();
      graph.min += graph.nodeCosts[u].get(graph.solution[u]);
    }
  }

  private void backPropagate() {
    Util.assertion(graph.stack != null);
    Util.assertion(graph.stackPtr < graph.numNodes);

    for (int i = graph.stackPtr - 1; i >= 0; i--) {
      int u = graph.stack[i];
      Util.assertion(u >= 0 && u < graph.numNodes);
      graph.reinsertNode(u);
      if (graph.solution[u] == -1) {
        graph.determineSolution(u);
      }
    }
  }

  private void applyRI(int u) {
    Util.assertion(u >= 0 && u < graph.numNodes);
    Util.assertion(graph.adjList[u] != null);
    Util.assertion(graph.adjList[u].next == null);
    int v = graph.adjList[u].adj;
    Util.assertion(v >= 0 && v < graph.numNodes);

    int uLen = graph.nodeCosts[u].getLength();
    int vLen = graph.nodeCosts[v].getLength();

    PBQPVector vecU = graph.nodeCosts[u];
    PBQPMatrix costMat = graph.getCostMatrix(v, u);
    Util.assertion(costMat != null);

    PBQPVector delta = new PBQPVector(vLen);

    for (int i = 0; i < vLen; i++) {
      double min = costMat.get(i, 0) + vecU.get(0);
      for (int j = 1; j < uLen; j++) {
        double c = costMat.get(i, j) + vecU.get(j);
        if (c < min)
          min = c;
      }
      delta.set(i, min);
    }

    graph.nodeCosts[v].add(delta);

    graph.removeNode(u);
    graph.reorderAdjNode(u);
    Util.assertion(graph.stackPtr < graph.numNodes);
    graph.stack[graph.stackPtr++] = u;
  }

  private void applyRII(int u) {
    Util.assertion(u >= 0 && u < graph.numNodes);
    Util.assertion(graph.adjList[u] != null);
    Util.assertion(graph.adjList[u].next != null);
    Util.assertion(graph.adjList[u].next.next == null);

    int y = graph.adjList[u].adj;
    int z = graph.adjList[u].next.adj;
    Util.assertion(y >= 0 && y < graph.numNodes);
    Util.assertion(z >= 0 && z < graph.numNodes);

    int ulen = graph.nodeCosts[u].getLength();
    int ylen = graph.nodeCosts[y].getLength();
    int zlen = graph.nodeCosts[z].getLength();

    PBQPVector vecU = graph.nodeCosts[u];
    PBQPMatrix matYU = graph.getCostMatrix(y, u);
    PBQPMatrix matZU = graph.getCostMatrix(z, u);
    Util.assertion(matYU != null && matZU != null);

    PBQPMatrix delta = new PBQPMatrix(ylen, zlen);

    for (int i = 0; i < ylen; i++) {
      for (int j = 0; j < zlen; j++) {
        double min = matYU.get(i, 0) + matZU.get(j, 0) + vecU.get(0);
        for (int k = 1; k < ulen; k++) {
          double c = matYU.get(i, k) + matZU.get(j, k) + vecU.get(k);
          if (c < min)
            min = c;
        }
        delta.set(i, j, min);
      }
    }

    graph.addEdgeCosts(y, z, delta);

    graph.removeNode(u);
    simplifyEdge(y, z);
    graph.reorderAdjNode(u);
    Util.assertion(graph.stackPtr < graph.numNodes);
    graph.stack[graph.stackPtr++] = u;
  }

  private void applyRN(int u) {
    Util.assertion(u >= 0 && u < graph.numNodes);
    Util.assertion(graph.nodeCosts[u] != null);
    int ulen = graph.nodeCosts[u].getLength();

    // After simplification of RN rule no optimality can be guaranted.
    graph.optimal = false;

    double min = 0;
    int minSol = -1;
    // determine local minimum
    for (int sol = 0; sol < ulen; sol++) {
      double h = graph.nodeCosts[u].get(sol);
      for (AdjNode adj = graph.adjList[u]; adj != null; adj = adj.next) {
        int y = adj.adj;
        PBQPMatrix mat = graph.getCostMatrix(u, y);
        PBQPVector vec = graph.nodeCosts[y].clone();

        Util.assertion(mat != null);
        mat.addRow(sol, vec);
        h += vec.min();
      }
      if (h < min || sol == 0) {
        min = h;
        minSol = sol;
      }
    }

    Util.assertion(minSol >= 0 && minSol < ulen);
    graph.solution[u] = minSol;

    graph.min += graph.nodeCosts[u].get(minSol);
    for (AdjNode adj = graph.adjList[u]; adj != null; adj = adj.next) {
      int y = adj.adj;
      PBQPMatrix mat = graph.getCostMatrix(u, y);
      mat.addRow(minSol, graph.nodeCosts[y]);
    }
    Util.assertion(graph.stackPtr < graph.numNodes);
    graph.stack[graph.stackPtr++] = u;
    graph.removeNode(u);
    graph.reorderAdjNode(u);
  }
}
