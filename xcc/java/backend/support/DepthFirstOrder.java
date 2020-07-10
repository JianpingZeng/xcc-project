package backend.support;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
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

import backend.analysis.DomTreeNodeBase;
import backend.codegen.MachineBasicBlock;
import backend.utils.SuccIterator;
import backend.value.BasicBlock;
import tools.Pair;

import java.util.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class DepthFirstOrder {
  /**
   * Computes the reverse post order for the specified CFG from the start node.
   * The reverse post order of Basic Block is restored in returned list.
   *
   * @param start
   * @return
   */
  public static ArrayList<BasicBlock> reversePostOrder(BasicBlock start) {
    ArrayList<BasicBlock> visited = new ArrayList<>(postOrder(start));
    Collections.reverse(visited);
    return visited;
  }

  /**
   * Computes the reverse post order for the specified CFG from the start node.
   * The reverse post order of Basic Block is restored in returned list.
   *
   * @param start
   * @return
   */
  public static ArrayList<MachineBasicBlock> reversePostOrder(
      MachineBasicBlock start) {
    ArrayList<MachineBasicBlock> visited = new ArrayList<>(postOrder(start));
    Collections.reverse(visited);
    return visited;
  }

  public static LinkedList<BasicBlock> postOrder(BasicBlock startBlock) {
    LinkedList<BasicBlock> res = new LinkedList<>();
    visit(startBlock, res);
    return res;
  }

  private static void visit(BasicBlock bb, LinkedList<BasicBlock> res) {
    if (bb == null)
      return;

    HashSet<BasicBlock> visited = new HashSet<>();
    // the second part of pair represents if the node is visited twice,
    // we need to visit it's child nodes first then visit the node itself.
    // if the second part is true, the node has been visited before.
    Stack<Pair<BasicBlock, Boolean>> worklist = new Stack<>();
    worklist.push(Pair.get(bb, false));
    visited.add(bb);
    while (!worklist.isEmpty()) {
      Pair<BasicBlock, Boolean> cur = worklist.peek();
      if (!cur.second) {
        // visit it first time.
        for (SuccIterator itr = cur.first.succIterator(); itr.hasNext(); ) {
          BasicBlock succ = itr.next();
          if (visited.add(succ))
            worklist.push(Pair.get(succ, false));
        }
        cur.second = true;
      } else  {
        // we visit it second time.
        res.add(cur.first);
        worklist.pop();
      }
    }
  }

  public static List<MachineBasicBlock> postOrder(MachineBasicBlock start) {
    ArrayList<MachineBasicBlock> res = new ArrayList<>();
    visit(start, res);
    return res;
  }

  private static void visit(MachineBasicBlock bb,
                            ArrayList<MachineBasicBlock> result) {
    if (bb == null)
      return;

    HashSet<MachineBasicBlock> visited = new HashSet<>();
    // the second part of pair represents if the node is visited twice,
    // we need to visit it's child nodes first then visit the node itself.
    // if the second part is true, the node has been visited before.
    Stack<Pair<MachineBasicBlock, Boolean>> worklist = new Stack<>();
    worklist.push(Pair.get(bb, false));
    while (!worklist.isEmpty()) {
      Pair<MachineBasicBlock, Boolean> cur = worklist.peek();
      if (!cur.second) {
        // visit it first time.
        for (int i = 0, e = cur.first.getNumSuccessors(); i < e; ++i) {
          MachineBasicBlock succ = cur.first.suxAt(i);
          if (visited.add(succ))
            worklist.push(Pair.get(succ, false));
        }
        cur.second = true;
      } else  {
        // we visit it second time.
        result.add(cur.first);
        worklist.pop();
      }
    }
  }

  public static LinkedList<DomTreeNodeBase<BasicBlock>> dfTraversal(
      DomTreeNodeBase<BasicBlock> entryNode) {
    LinkedList<DomTreeNodeBase<BasicBlock>> ret = new LinkedList<>();
    Stack<DomTreeNodeBase<BasicBlock>> stack = new Stack<>();
    stack.push(entryNode);
    ArrayList<DomTreeNodeBase<BasicBlock>> temps = new ArrayList<>();

    while (!stack.isEmpty()) {
      DomTreeNodeBase<BasicBlock> cur = stack.pop();
      ret.add(cur);

      temps.clear();
      Collections.copy(temps, cur.getChildren());

      Collections.reverse(temps);
      temps.forEach(stack::push);
    }
    return ret;
  }

  private static void visitDFS(MachineBasicBlock start,
                               ArrayList<MachineBasicBlock> result,
                               HashSet<MachineBasicBlock> visited) {
    if (visited.add(start)) {
      result.add(start);
      start.getSuccessors().forEach(succ -> visitDFS(succ, result, visited));
    }
  }

  public static ArrayList<MachineBasicBlock> dfs(MachineBasicBlock entry) {
    ArrayList<MachineBasicBlock> result = new ArrayList<>();
    HashSet<MachineBasicBlock> visited = new HashSet<>();
    visitDFS(entry, result, visited);
    return result;
  }

  private static void visitDFS(BasicBlock start,
                               ArrayList<BasicBlock> result,
                               HashSet<BasicBlock> visited) {
    if (visited.add(start)) {
      result.add(start);
      SuccIterator itr = start.succIterator();
      while (itr.hasNext()) {
        visitDFS(itr.next(), result, visited);
      }
    }
  }

  public static ArrayList<BasicBlock> dfs(BasicBlock entry) {
    ArrayList<BasicBlock> result = new ArrayList<>();
    HashSet<BasicBlock> visited = new HashSet<>();
    visitDFS(entry, result, visited);
    return result;
  }

  public static ArrayList<MachineBasicBlock> dfTraversal(MachineBasicBlock entry) {
    return reversePostOrder(entry);
  }

  public static ArrayList<BasicBlock> dfTraversal(BasicBlock entry) {
    return reversePostOrder(entry);
  }
}
