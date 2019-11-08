package utils.tablegen;
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

import backend.codegen.MVT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import static utils.tablegen.SDNP.SDNPHasChain;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class PatternToMatch {
  /**
   * Top level predicate condition to match.
   */
  public ArrayList<Predicate> preds;
  public Record srcRecord;
  public TreePatternNode srcPattern;
  public TreePatternNode dstPattern;
  public ArrayList<Record> dstRegs;
  public int addedComplexity;
  public boolean forceMode;

  public PatternToMatch(Record r,
                        ArrayList<Predicate> preds,
                        TreePatternNode src,
                        TreePatternNode dst,
                        ArrayList<Record> dstRegs,
                        int complexity) {
    srcRecord = r;
    this.preds = new ArrayList<>();
    this.preds.addAll(preds);
    srcPattern = src.clone();
    dstPattern = dst.clone();
    this.dstRegs = new ArrayList<>();
    this.dstRegs.addAll(dstRegs);
    addedComplexity = complexity;
    forceMode = false;
  }

  private static int getPatternSize(TreePatternNode pat, CodeGenDAGPatterns cgp) {
    int size = 3;
    if (pat.isLeaf() && pat.getLeafValue() instanceof Init.IntInit)
      size += 2;

    ComplexPattern cp = nodeGetComplexPattern(pat, cgp);
    if (cp != null)
      size += cp.getNumOperands() * 3;

    if (!pat.getPredicateFns().isEmpty())
      ++size;

    for (int i = 0, e = pat.getNumChildren(); i < e; i++) {
      TreePatternNode child = pat.getChild(i);
      if (!child.isLeaf() && child.getNumTypes() > 0) {
        TypeSetByHwMode vt = child.getExtType(0);
        if (vt.getMachineValueType().simpleVT != MVT.Other)
          size += getPatternSize(child, cgp);
      } else if (child.isLeaf()) {
        if (child.getLeafValue() instanceof Init.IntInit)
          size += 5;
        else if (nodeIsComplexPattern(child))
          size += getPatternSize(child, cgp);
        else if (!child.getPredicateFns().isEmpty())
          size++;
      }
    }
    return size;
  }

  private static int getResultPatternCost(TreePatternNode inst, CodeGenDAGPatterns cgp) {
    if (inst.isLeaf()) return 0;

    int cost = 0;
    Record opc = inst.getOperator();
    if (opc.isSubClassOf("Instruction")) {
      ++cost;
      CodeGenInstruction cgInst = cgp.getTarget().getInstruction(opc.getName());
      if (cgInst.usesCustomInserter)
        cost += 10;
    }
    for (int i = 0, e = inst.getNumChildren(); i < e; i++)
      cost += getResultPatternCost(inst.getChild(i), cgp);
    return cost;
  }

  private static int getResultPatternSize(TreePatternNode node, CodeGenDAGPatterns cgp) {
    if (node.isLeaf()) return 0;

    int size = 0;
    Record opc = node.getOperator();
    if (opc.isSubClassOf("Instruction"))
      size += opc.getValueAsInt("CodeSize");
    for (int i = 0, e = node.getNumChildren(); i < e; i++)
      size += getResultPatternSize(node.getChild(i), cgp);
    return size;
  }

  private static boolean nodeIsComplexPattern(TreePatternNode node) {
    return (node.isLeaf() && (node.getLeafValue() instanceof Init.DefInit)
        && ((Init.DefInit) node.getLeafValue()).getDef().isSubClassOf("ComplexPattern"));
  }

  static boolean disableComplexPatternForNotOpt(TreePatternNode node, CodeGenDAGPatterns cgp) {
    boolean isStore = !node.isLeaf() && getOpcodeName(node.getOperator(), cgp).equals("ISD.STORE");
    if (!isStore && nodeHasProperty(node, SDNPHasChain, cgp))
      return false;

    for (int i = 0, e = node.getNumChildren(); i < e; i++) {
      if (patternHasProperty(node.getChild(i), SDNPHasChain, cgp))
        return true;
    }
    return false;
  }

  private static ComplexPattern nodeGetComplexPattern(TreePatternNode node,
                                                      CodeGenDAGPatterns cgp) {
    if (node.isLeaf() && (node.getLeafValue() instanceof Init.DefInit)
        && ((Init.DefInit) node.getLeafValue()).getDef().isSubClassOf("ComplexPattern")) {
      return cgp.getComplexPattern(((Init.DefInit) node.getLeafValue()).getDef());
    }
    return null;
  }

  private static boolean patternHasProperty(TreePatternNode node, int prop, CodeGenDAGPatterns cgp) {
    if (nodeHasProperty(node, prop, cgp))
      return true;

    for (int i = 0, e = node.getNumChildren(); i < e; i++) {
      TreePatternNode child = node.getChild(i);
      if (patternHasProperty(child, prop, cgp))
        return true;
    }
    return false;
  }

  private static boolean nodeHasProperty(TreePatternNode node, int prop, CodeGenDAGPatterns cgp) {
    if (node.isLeaf()) {
      ComplexPattern cp = nodeGetComplexPattern(node, cgp);
      if (cp != null)
        return cp.hasProperty(prop);
      return false;
    }
    Record rec = node.getOperator();
    if (!rec.isSubClassOf("SDNode")) return false;
    return cgp.getSDNodeInfo(rec).hasProperty(prop);
  }

  private static String getOpcodeName(Record opc, CodeGenDAGPatterns cgp) {
    return cgp.getSDNodeInfo(opc).getEnumName();
  }

  private static void removeAllTypes(TreePatternNode node) {
    node.removeTypes();
    if (!node.isLeaf()) {
      for (int i = 0, e = node.getNumChildren(); i < e; i++)
        removeAllTypes(node.getChild(i));
    }
  }

  public Record getSrcRecord() {
    return srcRecord;
  }

  public ArrayList<Predicate> getPredicates() {
    return preds;
  }

  public TreePatternNode getSrcPattern() {
    return srcPattern;
  }

  public ArrayList<Record> getDstRegs() {
    return dstRegs;
  }

  public TreePatternNode getDstPattern() {
    return dstPattern;
  }

  public int getAddedComplexity() {
    return addedComplexity;
  }

  /**
   * Return a single string containing all of this
   * pattern's predicates concatenated with "&&" operators.
   *
   * @return
   * @throws Exception
   */
  public String getPredicateCheck() {
    TreeSet<Predicate> predLists = new TreeSet<>(preds);
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (Predicate p : predLists) {
      if (i != 0)
        sb.append(" && ");
      sb.append("(").append(p.getCondString()).append(")");
      ++i;
    }
    return sb.toString();
  }

  public void dump() {
    System.err.print("Pattern: ");
    srcPattern.dump();
    System.err.print("\nResult:");
    dstPattern.dump();
    System.err.println();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (obj.getClass() != getClass())
      return false;
    PatternToMatch pat = (PatternToMatch) obj;
    return preds.equals(pat.preds) &&
        srcPattern.equals(pat.srcPattern) && dstPattern.equals(pat.dstPattern)
        && dstRegs.equals(pat.dstRegs) && addedComplexity == pat.addedComplexity;
  }

  static class PatternSortingPredicate implements Comparator<PatternToMatch> {
    private CodeGenDAGPatterns cgp;

    PatternSortingPredicate(CodeGenDAGPatterns cgp) {
      this.cgp = cgp;
    }

    @Override
    public int compare(PatternToMatch lhs,
                       PatternToMatch rhs) {
      try {
        int lhsSize = getPatternSize(lhs.getSrcPattern(), cgp);
        int rhsSize = getPatternSize(rhs.getSrcPattern(), cgp);
        lhsSize += lhs.getAddedComplexity();
        rhsSize += rhs.getAddedComplexity();
        if (lhsSize > rhsSize) return -1;
        if (lhsSize < rhsSize) return 1;

        int lhsCost = getResultPatternCost(lhs.getDstPattern(), cgp);
        int rhsCost = getResultPatternCost(rhs.getDstPattern(), cgp);
        if (lhsCost > rhsCost) return -1;
        if (lhsCost < rhsCost) return 1;

        return getResultPatternSize(rhs.getDstPattern(), cgp) -
            getResultPatternSize(lhs.getDstPattern(), cgp);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return 0;
    }
  }
}
