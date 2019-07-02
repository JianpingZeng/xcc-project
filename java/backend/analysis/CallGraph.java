package backend.analysis;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.pass.AnalysisResolver;
import backend.pass.ModulePass;
import backend.support.CallSite;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.CallInst;
import backend.value.Module;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.io.PrintStream;
import java.util.*;

/**
 * This pass would be used for running immediately before
 * {@linkplain backend.pass.CallGraphSCCPass} to provide a
 * call graph for each {@linkplain Module}. So that you can
 * figure out which function will calls this function and which
 * functions this function would be called by.
 *
 * @author Jianping Zeng.
 * @version 0.4
 */
public class CallGraph implements ModulePass {
  protected Module mod;
  private AnalysisResolver resolver;
  protected HashMap<Function, CallGraphNode> functionMap;

  /**
   * The main function or null.
   */
  protected CallGraphNode root;
  /**
   * ExternalCallingNode represents the call node to external function
   * (declared in current file, but defined in other file).
   */
  protected CallGraphNode externalCallingNode;

  private CallGraphNode[] nodes;

  public CallGraph() {
    functionMap = new HashMap<>();
  }

  public Module getModule() {
    return mod;
  }

  public HashMap<Function, CallGraphNode> getFunctionMap() {
    return functionMap;
  }

  public CallGraphNode getExternalCallingNode() {
    return externalCallingNode;
  }

  public CallGraphNode getRoot() {
    return root;
  }

  /**
   * Gets a call graph node associated with specified function. If the
   * specified function is not existed then creating a new node and
   * return it.
   *
   * @param f
   * @return
   */
  public CallGraphNode getOrInsertFunction(Function f) {
    if (f == null) return null;
    if (functionMap.containsKey(f))
      return functionMap.get(f);
    CallGraphNode node = new CallGraphNode(f);
    functionMap.put(f, node);
    return node;
  }

  public Function removeFunctionFromModule(Function f) {
    return removeFunctionFromModule(getOrInsertFunction(f));
  }

  /**
   * Delete the specified call graph node from SCC. Note that this
   * function only will remove those function that has no calls to
   * other function.
   *
   * @param node
   * @return
   */
  public Function removeFunctionFromModule(CallGraphNode node) {
    if (node == null || nodes == null || nodes.length <= 0)
      return null;
    for (int i = 0, e = nodes.length; i < e; i++) {
      CallGraphNode cg = nodes[i];
      if (cg == node && cg.isEmpty()) {
        nodes[i] = null;
        for (CallGraphNode n : nodes) {
          if (n != null && n.containsCalledFunction(node))
            n.removeCalledFunction(node);
        }
        return node.getFunction();
      }
    }
    return null;
  }

  @Override
  public boolean runOnModule(Module m) {
    mod = m;
    nodes = new CallGraphNode[m.getNumFunctions()];
    if (nodes.length <= 0)
      return false;

    TObjectIntHashMap<Function> funcNumbers = new TObjectIntHashMap<>();
    int i = 0;
    for (Function f : m.getFunctionList()) {
      nodes[i] = new CallGraphNode(f);
      if (f.isMain())
        root = nodes[i];
      funcNumbers.put(f, i++);
    }

    for (Function f : m.getFunctionList()) {
      Util.assertion(funcNumbers.containsKey(f));
      int idx = funcNumbers.get(f);
      CallGraphNode node = nodes[idx] = getOrInsertFunction(f);

      for (BasicBlock bb : f) {
        for (Instruction inst : bb) {
          if (!(inst instanceof CallInst))
            continue;
          CallInst ci = (CallInst) inst;
          Function target = ci.getCalledFunction();
          CallGraphNode targetNode = getOrInsertFunction(target);
          Util.assertion(targetNode != null, "call to null function");
          node.addCalledFunction(new CallSite(ci), targetNode);
        }
      }
    }
    return false;
  }

  @Override
  public String getPassName() {
    return "Collecting CallGraph over Module Pass";
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  public SCCIterator getSCCIterator() {
    return new SCCIterator();
  }

  public void dump() {
  }

  public void print(PrintStream os, Module m) {
  }

  public CallGraphNode getNode(Function f) {
    Util.assertion(f != null && functionMap.containsKey(f));
    return functionMap.get(f);
  }

  public final class SCCIterator implements Iterator<ArrayList<CallGraphNode>> {
    private ArrayList<HashSet<CallGraphNode>> sccs;
    /**
     * The index pointer to the current node being accessed.
     */
    private int index;

    private SCCIterator() {
      sccs = new ArrayList<>();
      HashSet<CallGraphNode> visited = new HashSet<>();
      for (CallGraphNode n : nodes) {
        if (n == null || visited.contains(n)) continue;
        sccs.add(collectsSCC(n, visited));
      }
      index = 0;
    }

    private HashSet<CallGraphNode> collectsSCC(CallGraphNode root, HashSet<CallGraphNode> visited) {
      HashSet<CallGraphNode> res = new HashSet<>();
      Stack<CallGraphNode> worklist = new Stack<>();
      worklist.push(root);
      while (!worklist.isEmpty()) {
        CallGraphNode cur = worklist.pop();
        if (visited.contains(cur))
          continue;
        visited.add(cur);
        res.add(cur);
        for (int i = 0, e = cur.size(); i < e; i++) {
          CallGraphNode n = cur.getCallGraphNodeAt(i);
          worklist.push(n);
        }
      }
      return res;
    }

    @Override
    public boolean hasNext() {
      return index < sccs.size();
    }

    @Override
    public ArrayList<CallGraphNode> next() {
      Util.assertion(hasNext(), "must ensure hasNext return true before calling this function!");
      ArrayList<CallGraphNode> res = new ArrayList<>();
      res.addAll(sccs.get(index++));
      return res;
    }
  }
}
