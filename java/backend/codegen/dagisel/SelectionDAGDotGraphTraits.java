/*
 * Extremely C language Compiler
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

package backend.codegen.dagisel;

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.support.DefaultDotGraphTrait;
import backend.support.GraphWriter;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import static tools.Util.escapeString;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class SelectionDAGDotGraphTraits extends DefaultDotGraphTrait<SDNode> {
  private final SelectionDAG dag;
  private boolean shortName;
  private PrintStream os;

  public SelectionDAGDotGraphTraits(SelectionDAG dag, boolean shortName) {
    this.dag = dag;
    this.shortName = shortName;
  }

  @Override
  public String getGraphName() {
    return dag.getMachineFunction().getFunction().getName();
  }

  @Override
  public String getNodeLabel(SDNode node, boolean shortName) {
    return getNodeLabel(node, dag, shortName);
  }

  public static String getNodeLabel(SDNode node, SelectionDAG dag, boolean shortName) {
    String result = node.getOperationName(dag);
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         PrintStream os = new PrintStream(baos)) {
      node.printDetails(os, dag);
      result += baos.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  @Override
  public String getNodeAttributes(SDNode node) {
    String atts = dag.getGraphAttrs(node);
    if (!atts.isEmpty()) {
      if (atts.contains("shape=")) {
        return "shape=Mrecord," + atts;
      }
      return atts;
    }
    return "shape=Mrecord";
  }

  @Override
  public String getEdgeAttributes(SDNode from, Object to) {
    SDValue dest = (SDValue) to;
    EVT vt = dest.getValueType();
    if (vt.equals(new EVT(MVT.Glue)))
      return "color=red,style=bold";
    else if (vt.equals(new EVT(MVT.Other)))
      return "color=blue,style=dashed";
    return "";
  }

  @Override
  public int getNumEdgeDestLabels(SDNode node) {
    return node.getNumValues();
  }

  @Override
  public boolean hasEdgeDestLabels() {
    return true;
  }

  @Override
  public boolean edgeTargetEdgeSource(SDNode from, Object to) {
    return true;
  }

  @Override
  public int getEdgeTarget(SDNode node, Object to) {
    SDValue dest = (SDValue) to;
    return dest.getResNo();
  }

  @Override
  public boolean renderGraphFromBottomUp() {
    return true;
  }

  @Override
  public boolean hasNodeAddressLabel(SDNode node) {
    return true;
  }

  @Override
  public String getEdgeDestLabel(SDNode node, int i) {
    return node.getValueType(i).getEVTString();
  }

  @Override
  public void writeNodes(GraphWriter writer) {
    this.os = writer.getOut();
    for (SDNode node : dag.allNodes) {
      if (!node.isDeleted())
        writeNode(node);
    }
  }

  private void writeNode(SDNode node) {
    String nodeAttrs = getNodeAttributes(node);
    os.printf("\tNode0x%x [shape=record,", node.hashCode());
    if (!nodeAttrs.isEmpty()) {
      os.print(nodeAttrs);
      os.print(",");
    }
    os.print("label=\"{");
    if (!renderGraphFromBottomUp()) {
      os.print(getNodeLabel(node, shortName));
      if (hasNodeAddressLabel(node))
        os.printf("|%x", node.hashCode());
    }

    if (node.getNumOperands() > 0) {
      if (!renderGraphFromBottomUp())
        os.print("|");
      os.print("{");
      int i = 0, e = node.getNumOperands();
      for (; i < e && i < 64; i++) {
        if (i != 0) os.print("|");
        os.printf("<s%d>%s", i, getEdgeSourceLabel(node, node.getOperand(i)));
      }

      if (i != e)
        os.print("|<s64>truncated...");
      os.print("}");
      if (renderGraphFromBottomUp())
        os.print("|");
    }

    if (renderGraphFromBottomUp()) {
      os.print(escapeString(getNodeLabel(node, shortName)));
      if (hasNodeAddressLabel(node))
        os.printf("|0x%x", node.hashCode());
    }

    if (hasEdgeDestLabels()) {
      os.print("|{");

      int i = 0, e = getNumEdgeDestLabels(node);
      for (; i < e && i != 64; i++) {
        if (i != 0)
          os.print("|");
        os.printf("<d%d>%s", i, getEdgeDestLabel(node, i));
      }

      if (i != e)
        os.print("|<d64>truncated...");
      os.print("}");
    }
    os.println("}\"];");    // finishing printing the "node" line.

    int i = 0, e = node.getNumOperands();
    for (; i < e && i < 64; i++)
      writeEdge(node, i, node.getOperand(i));
    for (; i < e; i++)
      writeEdge(node, 64, node.getOperand(i));
  }

  /**
   * Output an edge from a simple node into the graph
   *
   * @param source
   * @param edgeIdx
   * @param dest
   */
  private void writeEdge(SDNode source, int edgeIdx, SDValue dest) {
    if (edgeIdx >= 64) return;  // eliminating truncated parts.

    SDNode target = dest.getNode();
    if (target != null) {
      int destPort = -1;
      if (edgeTargetEdgeSource(source, dest)) {
        destPort = getEdgeTarget(source, dest);
      }
      emitEdge(source, edgeIdx, dest.getNode(), destPort, getEdgeAttributes(source, dest));
    }
  }

  private void emitEdge(SDNode src, int srcNodePort,
                        SDNode dest, int destNodePort,
                        String attr) {
    if (srcNodePort > 64) return;
    if (destNodePort > 64)
      destNodePort = 64;

    os.printf("\tNode0x%x", src == null ? 0 : src.hashCode());
    if (srcNodePort >= 0)
      os.printf(":s%d", srcNodePort);
    os.printf(" -> Node0x%x", dest == null ? 0 : dest.hashCode());
    if (destNodePort >= 0) {
      if (hasEdgeDestLabels())
        os.printf(":d%d", destNodePort);
      else
        os.printf(":s%d", destNodePort);
    }
    if (!attr.isEmpty()) {
      os.printf("[%s]", attr);
    }
    os.println(";");
  }

  @Override
  public void writeFooter(GraphWriter writer) {
    os.println("}");
  }

  @Override
  public String getEdgeSourceLabel(SDNode to, Object from) {
    return "";
  }

  @Override
  public SelectionDAG getGraphType() {
    return dag;
  }

  @Override
  public void addCustomGraphFeatures() {
    emitSimpleNode(null, "plaintext=circle", "GraphRoot");
    SDValue root = dag.getRoot();
    if (root.getNode() != null) {
      emitEdge(null, -1, root.getNode(), root.getResNo(), "color=blue,style=dashed");
    }
  }

  @Override
  public void emitSimpleNode(Object id, String attr, String label,
                             int numEdgeSources, ArrayList<String> edgeSourceLabels) {
    os.printf("\tNode0x%d[ ", id == null ? 0 : id.hashCode());
    if (!attr.isEmpty())
      os.printf("%s,", attr);
    os.printf(" label=\"");
    if (numEdgeSources > 0)
      os.print("{");
    os.print(Util.escapeString(label));
    if (numEdgeSources > 0) {
      os.print("|{");
      for (int i = 0; i < numEdgeSources; i++) {
        if (i != 0) os.print("|");
        os.printf("<s%d>", i);
        if (edgeSourceLabels != null)
          os.printf(edgeSourceLabels.get(i));
      }
      os.print("}}");
    }
    os.println("\"];");
  }
}
