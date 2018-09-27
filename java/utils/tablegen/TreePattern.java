package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.codegen.MVT;
import tools.Error;
import tools.Util;
import utils.tablegen.CodeGenIntrinsic.ModRefType;
import utils.tablegen.Init.*;
import utils.tablegen.RecTy.IntRecTy;

import java.io.PrintStream;
import java.util.*;

import static utils.tablegen.CodeGenTarget.getValueType;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class TreePattern {
  private ArrayList<TreePatternNode> trees = new ArrayList<>();

  private Record theRecord;

  private ArrayList<String> args = new ArrayList<>();

  private CodeGenDAGPatterns cdp;

  private boolean isInputPattern;
  private TypeInfer infer;
  private boolean error;

  /**
   * This is a collection comprosiing of named tree node in this pattern tree.
   */
  private HashMap<String, ArrayList<TreePatternNode>> namedNodes;

  public TreePattern(Record theRec, ListInit rawPat, boolean isInput,
                     CodeGenDAGPatterns cdp) {
    theRecord = theRec;
    this.cdp = cdp;
    isInputPattern = isInput;
    infer = new TypeInfer(this);
    namedNodes = new HashMap<>();

    for (int i = 0, e = rawPat.getSize(); i != e; i++)
      trees.add(parseTreePattern(rawPat.getElement(i), ""));
  }

  public TreePattern(Record theRec, Init pat, boolean isInput,
                     CodeGenDAGPatterns cdp) {
    theRecord = theRec;
    this.cdp = cdp;
    isInputPattern = isInput;
    infer = new TypeInfer(this);
    trees.add(parseTreePattern(pat, ""));
    namedNodes = new HashMap<>();
  }

  public TreePattern(Record theRec, TreePatternNode pat, boolean isInput,
                     CodeGenDAGPatterns cdp) {
    theRecord = theRec;
    this.cdp = cdp;
    isInputPattern = isInput;
    infer = new TypeInfer(this);
    trees.add(pat);
    namedNodes = new HashMap<>();
  }

  public HashMap<String, ArrayList<TreePatternNode>> getNamedNodes() {
    if (namedNodes.isEmpty())
      computeNamedNodes();

    return namedNodes;
  }

  public TypeInfer getTypeInfer() { return infer; }

  public boolean hasError() {
    return error;
  }

  public ArrayList<TreePatternNode> getTrees() {
    return trees;
  }

  public int getNumTrees() {
    return trees.size();
  }

  public TreePatternNode getTree(int index) {
    return trees.get(index);
  }

  public TreePatternNode getOnlyTree() {
    Util.assertion(trees.size() == 1, "Doesn't have exactly one pattern!");
    return trees.get(0);
  }

  public Record getRecord() {
    return theRecord;
  }

  public int getNumArgs() {
    return args.size();
  }

  public String getArgName(int idx) {
    Util.assertion(idx >= 0 && idx < args.size());
    return args.get(idx);
  }

  public ArrayList<String> getArgList() {
    return args;
  }

  public CodeGenDAGPatterns getDAGPatterns() {
    return cdp;
  }

  public void inlinePatternFragments() {
    for (int i = 0, e = trees.size(); i != e; i++)
      trees.set(i, trees.get(i).inlinePatternFragments(this));
  }

  private void computeNamedNodes(TreePatternNode root) {
    if (root.getName() != null && !root.getName().isEmpty()) {
      String name = root.getName();
      if (!namedNodes.containsKey(name))
        namedNodes.put(name, new ArrayList<>());

      namedNodes.get(name).add(root);
    }

    for (int i = 0, e = root.getNumChildren(); i < e; i++)
      computeNamedNodes(root.getChild(i));
  }

  private void computeNamedNodes() {
    trees.forEach(n -> computeNamedNodes(n));
  }
  /**
   * This the version of {@linkplain #inferAllTypes(HashMap)} with an
   * argument default to null.
   * @return
   */
  public boolean inferAllTypes() {
    return inferAllTypes(null);
  }

  static boolean simplifyTree(TreePatternNode n) {
    if (n.isLeaf()) return false;

    if (n.getOperator().getName().equals("bitconvert") &&
    n.getExtType(0).isValueTypeByHwMode(false) &&
    n.getExtType(0) == n.getChild(0).getExtType(0) &&
    n.getName().isEmpty()) {
      n = n.getChild(0);
      simplifyTree(n);
      return true;
    }

    boolean madeChanged = false;
    for (int i = 0,e = n.getNumChildren();i < e; i++) {
      TreePatternNode child = n.getChild(i);
      madeChanged |= simplifyTree(child);
      n.setChild(i, child);
    }
    return madeChanged;
  }

  /**
   * Infer/propagate as many types throughout the expression
   * patterns as possible.
   *
   * @param inNamedTypes
   * @return Return true if all types are inferred, false
   * otherwise.  Throw an exception if a type contradiction is found.
   */
  public boolean inferAllTypes(HashMap<String, ArrayList<TreePatternNode>> inNamedTypes) {
    if (namedNodes.isEmpty())
      computeNamedNodes();

    boolean changed = true;
    while (changed) {
      changed = false;

      for (TreePatternNode node : trees) {
        changed |= node.applyTypeConstraints(this, false);
        this.dump();

        //changed |= simplifyTree(node);
      }

      for (Map.Entry<String, ArrayList<TreePatternNode>> pair : namedNodes.entrySet()) {
        ArrayList<TreePatternNode> nodes = pair.getValue();

        if (inNamedTypes != null) {
          if (!inNamedTypes.containsKey(pair.getKey())) {
            error(String.format("Node '%s' in output pattern but not input pattern",
                pair.getKey()));
            return true;
          }

          ArrayList<TreePatternNode> inNodes = inNamedTypes.get(pair.getKey());
          for (TreePatternNode node : inNodes) {
            if (node  == trees.get(0) && node.isLeaf()) {
              DefInit di = node.getLeafValue() instanceof DefInit ?
                  (DefInit)node.getLeafValue() : null;
              if (di != null && (di.getDef().isSubClassOf("RegisterClass") ||
                  di.getDef().isSubClassOf("RegisterOperand")))
                continue;
            }

            Util.assertion(node.getNumTypes() == 1 &&
                inNodes.get(0).getNumTypes() == 1, "FIXME: can't name multiple ressult nodes yet");
            changed |= node.updateNodeType(0, inNodes.get(0).getExtType(0), this);
          }
        }

        if (nodes.size() > 1) {
          for (int i = 0, e = nodes.size() -1; i < e; i++) {
            TreePatternNode n1 = nodes.get(i), n2 = nodes.get(i+1);
            Util.assertion(n1.getNumTypes() == 1 && n2.getNumTypes() == 1,
                "FIXME: can't name multiple result nodes as yet");
            changed |= n1.updateNodeType(0, n2.getExtType(0), this);
            changed |= n2.updateNodeType(0, n1.getExtType(0), this);
          }
        }
      }
    }

    boolean hasUnresolvedTypes = false;
    for (TreePatternNode node : trees)
      hasUnresolvedTypes |= node.containsUnresolvedType(this);

    return !hasUnresolvedTypes;
  }

  public void error(String msg) {
    if (hasError())
      return;
    dump();
    Error.printError("In " + theRecord.getName() + ": " + msg);
    error = true;
  }

  public void print(PrintStream os) {
    os.printf(getRecord().getName());
    if (!args.isEmpty()) {
      os.printf("(%s", args.get(0));
      for (int i = 1; i != args.size(); i++)
        os.printf(", %s", args.get(i));

      os.printf(")");
    }
    os.printf(":");

    if (trees.size() > 1) {
      os.printf("[\n");
    }
    for (int i = 0, e = trees.size(); i != e; i++) {
      os.printf("\t");
      trees.get(i).print(os);
      os.println();
    }

    if (trees.size() > 1)
      os.println();
  }

  public void dump() {
    print(System.err);
  }

  /**
   * Parse the given PatFrag initialized by {@code theInit}, and recursively parse it's children
   * node as needed. The {@code theInit} looks like this:
   * <pre>
   *   def zextloadi1  : PatFrag<(ops node:$ptr), (zextload node:$ptr), [{
   *     return ((LoadSDNode)n).getMemoryVT().getSimpleVT().simpleVT == MVT.i1;
   *   }]>;
   * (zextload node:$ptr) is a DAGInit to this PatFrag, and it's operator is also a PatFrag def.
   * </pre>
   * @param theInit The initial PatFrag for this TreePattern.
   * @param opName The name of this argument.
   * @return
   */
  private TreePatternNode parseTreePattern(Init theInit, String opName) {

    if (theInit instanceof DefInit) {
      DefInit di = (DefInit) theInit;
      Record r = di.getDef();
      if (r.isSubClassOf("SDNode") || r.isSubClassOf("PatFrag"))
        return parseTreePattern(new DagInit(di, null,
            new ArrayList<>()), opName);
      TreePatternNode res = new TreePatternNode(di, 1);
      if (r.getName().equals("node") && !opName.isEmpty())
        args.add(opName);

      res.setName(opName);
      return res;
    }
    if (theInit instanceof Init.UnsetInit) {
      if (opName.isEmpty())
        error("'?' argument requires a name to match with operand list");
      TreePatternNode res = new TreePatternNode(theInit, 1);
      args.add(opName);
      res.setName(opName);
      return res;
    }
    if (theInit instanceof IntInit) {
      IntInit ii = (IntInit) theInit;
      if (!opName.isEmpty())
        error("Constant int argument should not have a name!");
      return new TreePatternNode(ii, 1);
    }

    if (theInit instanceof BitsInit) {
      BitsInit bi = (BitsInit) theInit;
      Init ii = bi.convertInitializerTo(new IntRecTy());
      if (ii == null || !(ii instanceof IntInit))
        error("Bits value must be constant!");
      return parseTreePattern(ii, opName);
    }

    if (!(theInit instanceof DagInit))
      Error.printFatalError("Pattern has unexpected init kind");

    DagInit dag = (DagInit) theInit;

    if (!(dag.getOperator() instanceof DefInit))
      error("Pattern has unexpected operator type!");

    DefInit opDef = (DefInit) dag.getOperator();
    Record operator = opDef.getDef();

    if (operator.isSubClassOf("ValueType")) {
      // If the operator is a ValueType, then this must be "type cast" of a leaf
      // node.
      if (dag.getNumArgs() != 1)
        error("Type cast only takes one operand!");

      TreePatternNode newRes = parseTreePattern(dag.getArg(0), dag.getArgName(0));

      Util.assertion(newRes.getNumTypes() ==1, "FIXME: Unhandled!");
      newRes.updateNodeType(0, getValueType(operator), this);
      if (!opName.isEmpty())
        error("ValueType cast should not have a name!");
      return newRes;
    }

    // Verify that this is something that makes sense for an operator.
    if (!operator.isSubClassOf("PatFrag") &&
        !operator.isSubClassOf("SDNode") &&
        !operator.isSubClassOf("Instruction") &&
        !operator.isSubClassOf("SDNodeXForm") &&
        !operator.isSubClassOf("Intrinsic") &&
        !Objects.equals(operator.getName(), "set") &&
        !Objects.equals(operator.getName(), "implicit") &&
        !Objects.equals(operator.getName(), "parallel")) {
      error("Unrecognized node '" + operator.getName() + "'!");
    }

    if (isInputPattern && (operator.isSubClassOf("Instruction")
        || operator.isSubClassOf("SDNodeXForm")))
      error("Cannot use '" + operator.getName() + "' in an input pattern!");

    LinkedList<TreePatternNode> children = new LinkedList<>();

    for (int i = 0, e = dag.getNumArgs(); i != e; i++) {
      Init arg = dag.getArg(i);
      String argName = dag.getArgName(i);
      children.add(parseTreePattern(arg, argName));
    }
    if (operator.isSubClassOf("Intrinsic")) {
      CodeGenIntrinsic cgi = getDAGPatterns().getIntrinsic(operator);
      int iid = getDAGPatterns().getIntrinsicID(operator)+1;

      if (cgi.is.retVTs.isEmpty())
        operator = getDAGPatterns().getIntrinsicVoidSDNode();
      else if (cgi.modRef != ModRefType.NoMem)
        operator = getDAGPatterns().getIntrinsicWChainSDNode();
      else
        operator = getDAGPatterns().getIntrinsicWOChainSDNode();

      TreePatternNode iidNode = new TreePatternNode(new IntInit(iid), 1);
      children.addFirst(iidNode);
    }

    int numResults = getNumNodeResults(operator, getDAGPatterns());
    TreePatternNode result = new TreePatternNode(operator, children, numResults);
    result.setName(opName);
    if (dag.getName() != null) {
      Util.assertion(result.getName().isEmpty());
      result.setName(dag.getName());
    }

    return result;
  }

  public static int getNumNodeResults(Record operator, CodeGenDAGPatterns cdp) {
    if (operator.getName().equals("set") ||
        operator.getName().equals("implicit"))
      return 0;

    if (operator.isSubClassOf("Intrinsic"))
      return cdp.getIntrinsic(operator).is.retVTs.size();

    if (operator.isSubClassOf("SDNode"))
      return cdp.getSDNodeInfo(operator).getNumResults();
    if (operator.isSubClassOf("PatFrag")) {
      TreePattern tp = cdp.getPatternFragmentIfExisting(operator);
      if (tp != null)
        return tp.getOnlyTree().getNumTypes();

      DagInit tree = operator.getValueAsDag("Fragment");
      Record op = null;
      if (tree != null && tree.getOperator() instanceof DefInit)
        op = ((DefInit)tree.getOperator()).getDef();
      Util.assertion(op != null, "Invalid Fragment");
      return getNumNodeResults(op, cdp);
    }

    if (operator.isSubClassOf("Instruction")) {
      CodeGenInstruction cgi = cdp.getTarget().getInstruction(operator.getName());
      int numDefsToAdd = cgi.numDefs != 0 ? 1 : 0;

      // Add on one implicit def if it has a resolvable type.
      if (cgi.hasOneImplicitDefWithKnownVT(cdp.getTarget()) != MVT.Other)
        ++numDefsToAdd;

      return numDefsToAdd;
    }

    if (operator.isSubClassOf("SDNodeXForm"))
      return 1;
    operator.dump();
    System.err.println("Unhandled node in getNumNodeResults");
    System.exit(-1);
    return -1;
  }
}
