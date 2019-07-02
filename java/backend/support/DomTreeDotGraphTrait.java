/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.analysis.DomTree;
import backend.analysis.DomTreeNodeBase;
import backend.analysis.IDomTreeInfo;
import backend.value.BasicBlock;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;

import static backend.support.AssemblyWriter.writeAsOperand;
import static tools.Util.escapeString;

/**
 * A specialized graph trait provider for DomTree.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class DomTreeDotGraphTrait extends DefaultDotGraphTrait<BasicBlock> {
  private DomTree dt;
  private String funcName;

  DomTreeDotGraphTrait(DomTree dt, String fnName) {
    this.dt = dt;
    funcName = fnName;
  }

  @Override
  public String getGraphName() {
    return "Dom Tree for '" + funcName + "' function";
  }

  @Override
  public String getNodeLabel(BasicBlock node, boolean shortName) {
    if (!node.getName().isEmpty())
      return node.getName() + ":";

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream ps = new PrintStream(baos)) {
      writeAsOperand(new PrintStream(baos), node, false, null);
      ps.print(":");
    }
    return baos.toString();
  }

  private static class QueueData {
    DomTreeNodeBase<BasicBlock> curBB;

    QueueData(DomTreeNodeBase<BasicBlock> cur) {
      curBB = cur;
    }
  }

  private PrintStream os;

  @Override
  public void writeNodes(GraphWriter writer) {
    PrintStream os = writer.getOut();
    this.os = os;
    IDomTreeInfo idt = dt.getDomTree();
    DomTreeNodeBase<BasicBlock> root = idt.getRootNode();
    if (root == null)
      return;

    LinkedList<QueueData> queue = new LinkedList<>();
    queue.push(new QueueData(root));
    while (!queue.isEmpty()) {
      DomTreeNodeBase<BasicBlock> cur = queue.poll().curBB;
      Util.assertion(cur != null);
      String nodeAttrites = getNodeAttributes(cur.getBlock());
      os.printf("\tNode0x%x [shape=record,", cur.getBlock().hashCode());
      if (!nodeAttrites.isEmpty())
        os.printf("%s,", nodeAttrites);
      os.print("label=\"{");

      if (!renderGraphFromBottomUp()) {
        os.printf("%s", escapeString(
            getNodeLabel(cur.getBlock(), writer.shortName)));
        if (hasNodeAddressLabel(cur.getBlock()))
          os.printf("|0x%x", cur.getBlock().hashCode());
      }

      // print out the children of the current node.
      ArrayList<DomTreeNodeBase<BasicBlock>> children = cur.getChildren();
      final int e = children.size();
      if (!children.isEmpty()) {
        if (!renderGraphFromBottomUp())
          os.print("|");
        os.print("{");

        if (e <= 64) {
          for (int i = 0; i < e; i++) {
            if (i != 0)
              os.print("|");
            os.printf("<s%d>", i);
          }
        } else {
          os.print("|<s64>truncated...");
        }
        os.print("}");
        if (renderGraphFromBottomUp())
          os.print("|");
      }

      os.println("}\"];");

      // Output all of edge
      int i = 0;
      for (; i < e && i != 64; i++) {
        writeEdge(cur.getBlock(), i, children.get(i).getBlock());
      }
      for (; i < e; i++) {
        writeEdge(cur.getBlock(), 64, children.get(i).getBlock());
      }

      children.forEach(node -> queue.push(new QueueData(node)));
    }
  }

  /**
   * Output an edge from a simple node into the graph
   *
   * @param source
   * @param edgeIdx
   * @param dest
   */
  private void writeEdge(BasicBlock source, int edgeIdx, BasicBlock dest) {
    if (edgeIdx >= 64) return;  // eliminating truncated parts.

    os.printf("\tNode0x%x", source.hashCode());
    if (edgeIdx >= 0)
      os.printf(":s%d", edgeIdx);
    os.printf(" -> Node0x%x", dest.hashCode());

    String attr = getEdgeAttributes(source, dest);
    if (!attr.isEmpty())
      os.printf("[%s]", attr);
    os.println(";");
  }

  @Override
  public void writeFooter(GraphWriter writer) {
    os.println("}");
  }
}
