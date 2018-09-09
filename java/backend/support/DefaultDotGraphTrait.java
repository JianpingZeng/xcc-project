/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
import backend.codegen.dagisel.ScheduleDAG;
import backend.codegen.dagisel.ScheduleDAGDotTraits;
import backend.codegen.dagisel.SelectionDAG;
import backend.codegen.dagisel.SelectionDAGDotGraphTraits;
import backend.value.Function;

import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class DefaultDotGraphTrait<T> {
  public static DefaultDotGraphTrait createDomTreeTrait(DomTree dt, String funcName) {
    return new DomTreeDotGraphTrait(dt, funcName);
  }

  public static DefaultDotGraphTrait createCFGTrait(Function fn) {
    return new CFGDotGraphTrait(fn);
  }

  public static DefaultDotGraphTrait createSelectionDAGTrait(
      SelectionDAG dag, boolean shortName) {
    return new SelectionDAGDotGraphTraits(dag, shortName);
  }

  public static DefaultDotGraphTrait createScheduleDAGTrait(
      ScheduleDAG dag, boolean shortName) {
    return new ScheduleDAGDotTraits(dag, shortName);
  }

  public String getGraphName() {
    return "";
  }

  public boolean renderGraphFromBottomUp() {
    return false;
  }

  public String getGraphProperties(Object node) {
    return "";
  }

  public String getNodeLabel(T node, boolean shortName) {
    return "";
  }

  public boolean hasNodeAddressLabel(T node) {
    return false;
  }

  public String getNodeAttributes(T node) {
    return "";
  }

  public String getEdgeAttributes(T from, Object to) {
    return "";
  }

  public String getEdgeSourceLabel(T to, Object from) {
    return "";
  }

  public boolean edgeTargetEdgeSource(T from, Object to) {
    return false;
  }

  public int getEdgeTarget(T node, Object to) {
    return 0;
  }

  public boolean hasEdgeDestLabels() {
    return false;
  }

  public int getNumEdgeDestLabels(T node) {
    return 0;
  }

  public String getEdgeDestLabel(T node, int i) {
    return "";
  }

  public void addCustomGraphFeatures() {
  }

  public void writeFooter(GraphWriter writer) {

  }

  public void writeNodes(GraphWriter writer) {

  }

  public Object getGraphType() {
    return null;
  }

  public void emitSimpleNode(Object id, String attr, String label) {
    emitSimpleNode(id, attr, label, 0);
  }

  public void emitSimpleNode(Object id, String attr, String label,
                             int numEdgeSources) {
    emitSimpleNode(id, attr, label, numEdgeSources, null);
  }

  public void emitSimpleNode(Object id, String attr, String label,
                             int numEdgeSources, ArrayList<String> edgeSourceLabels) {

  }
}
