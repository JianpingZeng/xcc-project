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

package backend.codegen.dagisel;

import tools.Util;

import java.util.LinkedList;

import static backend.codegen.dagisel.DAGTypeLegalizer.NodeIdFlags.NewNode;
import static backend.codegen.dagisel.DAGTypeLegalizer.NodeIdFlags.Processed;
import static backend.codegen.dagisel.DAGTypeLegalizer.NodeIdFlags.ReadyToProcess;

/**
 * This class is a DAGUpdateListener that listens for updates to nodes and recomputes their ready state.
 * @author Jianping Zeng
 * @version 0.4
 */
public class NodeUpdateListener implements DAGUpdateListener {
  private DAGTypeLegalizer legalizer;
  private LinkedList<SDNode> nodesToAnalyze;

  public NodeUpdateListener(DAGTypeLegalizer legalizer, LinkedList<SDNode> nodesToAnalyze) {
    this.legalizer = legalizer;
    this.nodesToAnalyze = nodesToAnalyze;
  }

  @Override
  public void nodeDeleted(SDNode node, SDNode e) {
    Util.assertion(node.getNodeID() != ReadyToProcess &&
        node.getNodeID() != Processed, "Invalid node ID for RAUW deletion!");
    Util.assertion(e != null, "node is not replaced?");
    legalizer.nodeDeletion(node, e);

    // In theory, the deleted node could also have been scheduled for analysis.
    // So remove it from the set of nodes which will be re-analyzed.
    nodesToAnalyze.remove(node);

    // In general nothing needs to be done for E, since it didn't change but
    // only gained new uses.  However N -> E was just added to ReplacedValues,
    // and the result of a ReplacedValues mapping is not allowed to be marked
    // NewNode.  So if E is marked NewNode, then it needs to be analyzed.
    if (e.getNodeID() == NewNode)
      nodesToAnalyze.add(e);
  }

  @Override
  public void nodeUpdated(SDNode node) {
    // Node updates can mean pretty much anything.  It is possible that an
    // operand was set to something already processed (f.e.) in which case
    // this node could become ready.  Recompute its flags.
    Util.assertion(node.getNodeID() != ReadyToProcess &&
        node.getNodeID() != Processed, "Invalid node ID for RAUW deletion!");
    node.setNodeID(NewNode);
    nodesToAnalyze.add(node);
  }
}
