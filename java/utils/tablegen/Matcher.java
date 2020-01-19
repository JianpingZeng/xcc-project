package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the namespace of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.MVT;
import tools.Pair;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class Matcher {

  private Matcher next;
  private MatcherKind kind;

  public Matcher(MatcherKind kind) {
    next = null;
    this.kind = kind;
  }

  public Matcher getNext() {
    return next;
  }

  public MatcherKind getKind() {
    return kind;
  }

  public void setNext(Matcher next) {
    this.next = next;
  }

  public boolean isEqual(Matcher m) {
    if (getKind() != m.getKind()) return false;
    return isEqualImpl(m);
  }

  /**
   * Return true if this is a simple predicate that
   * operates on the node or its children without potential side effects or a
   * change of the current node.
   *
   * @return
   */
  public boolean isSimplePredicateNode() {
    switch (getKind()) {
      default:
        return false;
      case CheckSame:
      case CheckChildSame:
      case CheckPatternPredicate:
      case CheckPredicate:
      case CheckOpcode:
      case CheckType:
      case CheckChildType:
      case CheckInteger:
      case CheckChildInteger:
      case CheckCondCode:
      case CheckValueType:
      case CheckAndImm:
      case CheckOrImm:
      case CheckFoldableChainNode:
        return true;
    }
  }

  public boolean isSimplePredicateOrRecordNode() {
    return isSimplePredicateNode() || getKind() == MatcherKind.RecordNode ||
        getKind() == MatcherKind.RecordChild;
  }

  public Matcher unlinkNode(Matcher other) {
    if (this == other) {
      Matcher res = getNext();
      setNext(null);
      return res;
    }

    Matcher cur = this;
    for (; cur != null & cur.getNext() != other; cur = cur.getNext()) {  /*empty*/}

    if (cur == null) return null;
    setNext(other.getNext());
    other.setNext(null);
    return this;
  }

  public boolean canMoveBefore(Matcher other) {
    for (; ; other = other.getNext()) {
      Util.assertion(other != null, "other didn't come before 'this'");
      if (this == other) return true;

      if (!canMoveBeforeNode(other))
        return false;
    }
  }

  public boolean canMoveBeforeNode(Matcher other) {
    if (isSimplePredicateNode())
      return other.isSimplePredicateOrRecordNode();

    if (isSimplePredicateOrRecordNode())
      return isSimplePredicateNode();
    return false;
  }

  public void print(PrintStream os, int indent) {
    printImpl(os, indent);
    if (next != null)
      next.print(os, indent);
  }

  public void printOne(PrintStream os) {
    printImpl(os, 0);
  }

  public void dump() {
    printImpl(System.err, 0);
  }

  public boolean isSafeToRecorderWithPatternPredicate() {
    return false;
  }

  protected abstract void printImpl(PrintStream os, int indent);

  protected abstract boolean isEqualImpl(Matcher m);

  protected abstract int getHashImpl();

  protected boolean isContradictoryImpl(Matcher m) {
    return false;
  }

  /**
   * This attempts to match each of its children to find the first
   * one that successfully matches.  If one child fails, it tries the next child.
   * If none of the children match then this check fails.  It never has a 'next'.
   */
  public static class ScopeMatcher extends Matcher {

    private ArrayList<Matcher> children;

    public ScopeMatcher(ArrayList<Matcher> children) {
      super(MatcherKind.Scope);
      this.children = new ArrayList<>();
      this.children.addAll(children);
    }

    public ScopeMatcher(Matcher[] children) {
      super(MatcherKind.Scope);
      this.children = new ArrayList<>();
      this.children.addAll(Arrays.asList(children));
    }

    public int getNumChildren() {
      return children.size();
    }

    public Matcher getChild(int idx) {
      return children.get(idx);
    }

    public void resetChild(int i, Matcher m) {
      children.set(i, m);
    }

    public Matcher takeChild(int idx) {
      Matcher res = children.get(idx);
      children.set(idx, null);
      return res;
    }

    public void setNumChildren(int nc) {
      if (nc < children.size()) {
        for (int i = nc, e = children.size(); i < e; i++) {
          children.remove(i--);
          --e;
        }
      }
      for (int i = 0; i < nc - getNumChildren(); i++)
        children.add(null);
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.print(Util.fixedLengthString(indent, ' '));
      os.println("Scope");
      for (Matcher m : children) {
        if (m == null)
          os.printf("%sNULL POINTER%n", Util.fixedLengthString(indent + 1, ' '));
        else
          m.print(os, indent + 2);
      }
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return false;
    }

    @Override
    protected int getHashImpl() {
      return 12312;
    }
  }

  /**
   * Save the current node in the operand list.
   */
  public static class RecordMatcher extends Matcher {
    /**
     * This is string indicate why we should record this.
     * This will be printed out as a comment instead of
     * anything semantic.
     */
    private String whatFor;
    /**
     * The slot number in the recordedNodes list that this will
     * be just printed as a comment.
     */
    private int resultNo;

    public RecordMatcher(String whatFor, int resultNo) {
      super(MatcherKind.RecordNode);
      this.whatFor = whatFor;
      this.resultNo = resultNo;
    }

    public String getWhatFor() {
      return whatFor;
    }

    public int getResultNo() {
      return resultNo;
    }

    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sRecord%n", Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return true;
    }

    @Override
    protected int getHashImpl() {
      return 0;
    }
  }

  /**
   * Save a numbered child of the current node, or fail the match
   * if it doesn't exist. This is logically equivalent to:
   * // MoveChild N + RecordNode + MoveParentMatcher.
   */
  public static class RecordChildMatcher extends Matcher {
    private int childNo;

    /**
     * This is string indicate why we should record this.
     * This will be printed out as a comment instead of
     * anything semantic.
     */
    private String whatFor;
    /**
     * The slot number in the recordedNodes list that this will
     * be just printed as a comment.
     */
    private int resultNo;

    public RecordChildMatcher(int childNo, String whatFor,
                              int resultNo) {
      super(MatcherKind.RecordChild);
      this.childNo = childNo;
      this.whatFor = whatFor;
      this.resultNo = resultNo;
    }

    public int getChildNo() {
      return childNo;
    }

    public String getWhatFor() {
      return whatFor;
    }

    public int getResultNo() {
      return resultNo;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sRecordChild: %d%n", Util.fixedLengthString(indent, ' '),
          childNo);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof RecordChildMatcher &&
          ((RecordChildMatcher) m).getChildNo() == getChildNo();
    }

    @Override
    protected int getHashImpl() {
      return childNo;
    }
  }

  /**
   * Save the current node's memref.
   */
  public static class RecordMemRefMatcher extends Matcher {

    public RecordMemRefMatcher() {
      super(MatcherKind.RecordMemRef);
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sRecordMemRef%n", Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return true;
    }

    @Override
    protected int getHashImpl() {
      return 0;
    }
  }

  /**
   * If the current record has a flag input, record it so that is is used
   * as an input to the generated code.
   */
  public static class CaptureFlagInputMatcher extends Matcher {

    public CaptureFlagInputMatcher() {
      super(MatcherKind.CaptureFlagInput);
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sRecordMemRef%n", Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return true;
    }

    @Override
    protected int getHashImpl() {
      return 0;
    }
  }

  /**
   * This tells the interpreter to move into the specified child node.
   */
  public static class MoveChildMatcher extends Matcher {
    private int childNo;

    public MoveChildMatcher(int childNo) {
      super(MatcherKind.MoveChild);
      this.childNo = childNo;
    }

    public int getChildNo() {
      return childNo;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sMoveChild: %d%n", Util.fixedLengthString(indent, ' '),
          childNo);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof MoveChildMatcher &&
          ((MoveChildMatcher) m).getChildNo() == getChildNo();
    }

    @Override
    protected int getHashImpl() {
      return childNo;
    }
  }

  /**
   * This tells interpreter to move to the parent node of current node.
   */
  public static class MoveParentMatcher extends Matcher {

    public MoveParentMatcher() {
      super(MatcherKind.MoveParent);
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sMoveParent%n", Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return true;
    }

    @Override
    protected int getHashImpl() {
      return 0;
    }
  }

  /**
   * This checks to see if this node is exactly the same node as
   * the specified match taht is recorded with 'Record'. This is
   * used when patterns have the same namespace in them, like '(mul GPR:$in, GPR:$in)'.
   */
  public static class CheckSameMatcher extends Matcher {
    private int matchNumber;

    public CheckSameMatcher(int matchNumber) {
      super(MatcherKind.CheckSame);
      this.matchNumber = matchNumber;
    }

    public int getMatchNumber() {
      return matchNumber;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckSame: %d%n", Util.fixedLengthString(indent, ' '),
          matchNumber);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckSameMatcher &&
          ((CheckSameMatcher) m).getMatchNumber() == getMatchNumber();
    }

    @Override
    protected int getHashImpl() {
      return getMatchNumber();
    }
  }

  /**
   * This checks the target-specific predicate to see if the entire
   * pattern is capable of matching. This predicate does not take a
   * node as input. This is used for subtarget feature checks etc.
   */
  public static class CheckPatternPredicateMatcher extends Matcher {

    private String predicate;

    public CheckPatternPredicateMatcher(String pred) {
      super(MatcherKind.CheckPatternPredicate);
      predicate = pred;
    }

    public String getPredicate() {
      return predicate;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckPatternPredicate: %s%n", Util.fixedLengthString(indent, ' '),
          predicate);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckPatternPredicateMatcher &&
          ((CheckPatternPredicateMatcher) m).predicate.equals(predicate);
    }

    @Override
    protected int getHashImpl() {
      return predicate.hashCode();
    }
  }

  /**
   * This checks the target-specific predicate to see if the node
   * is applicable.
   */
  public static class CheckPredicateMatcher extends Matcher {
    private String predName;

    public CheckPredicateMatcher(String pred) {
      super(MatcherKind.CheckPredicate);
      predName = pred;
    }

    public String getPredicateName() {
      return predName;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckPredicate: %s%n", Util.fixedLengthString(indent, ' '),
          predName);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckPredicateMatcher &&
          ((CheckPredicateMatcher) m).predName.equals(predName);
    }

    @Override
    protected int getHashImpl() {
      return predName.hashCode();
    }
  }

  static boolean typesAreContradictory(int t1, int t2) {
    if (t1 == t2) return false;

    if (t1 == MVT.iPTR)
      return !new MVT(t2).isInteger() || new MVT(t2).isVector();
    if (t2 == MVT.iPTR)
      return !new MVT(t1).isInteger() || new MVT(t1).isVector();
    return true;
  }

  /**
   * Checks to see if the current node has the specified opcode, if not
   * it fails to match.
   */
  public static class CheckOpcodeMatcher extends Matcher {

    private SDNodeInfo opcode;

    public CheckOpcodeMatcher(SDNodeInfo opc) {
      super(MatcherKind.CheckOpcode);
      opcode = opc;
    }

    public SDNodeInfo getOpcode() {
      return opcode;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckOpcode: %s%n", Util.fixedLengthString(indent, ' '),
          opcode.getEnumName());
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckOpcodeMatcher &&
          ((CheckOpcodeMatcher) m).opcode.getEnumName().equals(opcode.getEnumName());
    }

    @Override
    protected int getHashImpl() {
      return opcode.getEnumName().hashCode();
    }

    @Override
    protected boolean isContradictoryImpl(Matcher m) {
      if (m instanceof CheckOpcodeMatcher) {
        CheckOpcodeMatcher chk = (CheckOpcodeMatcher) m;
        return !chk.getOpcode().getEnumName().equals(getOpcode().getEnumName());
      }

      if (m instanceof CheckTypeMatcher) {
        CheckTypeMatcher chk = (CheckTypeMatcher) m;
        if (chk.getResNo() >= getOpcode().getNumResults())
          return true;
        int nodeType = getOpcode().getKnownType(chk.getResNo());
        if (nodeType != MVT.Other)
          return typesAreContradictory(nodeType, chk.getType());
      }
      return false;
    }
  }

  /**
   * Switch based on the current node's opcode, dispatching to one
   * matcher per opcode. If the opcode doesn't match nay of the cases,
   * then the match fails. This is semantically equivalent to a Scope
   * node where every child does a CheckOpcode, but it is much faster.
   */
  public static class SwitchOpcodeMatcher extends Matcher {

    private ArrayList<Pair<SDNodeInfo, Matcher>> cases;

    public SwitchOpcodeMatcher(ArrayList<Pair<SDNodeInfo, Matcher>> cases) {
      super(MatcherKind.SwitchOpcode);
      this.cases = new ArrayList<>();
      this.cases.addAll(cases);
    }

    public int getNumCases() {
      return cases.size();
    }

    public SDNodeInfo getCaseOpcode(int idx) {
      return cases.get(idx).first;
    }

    public Matcher getCaseMatcher(int idx) {
      return cases.get(idx).second;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sSwitchOpcode: {%n", Util.fixedLengthString(indent, ' '));
      for (Pair<SDNodeInfo, Matcher> itr : cases) {
        os.printf("%scase %s:%n", Util.fixedLengthString(indent, ' '),
            itr.first.getEnumName());
        itr.second.print(os, indent + 2);
      }
      os.printf("%s}%n", Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return false;
    }

    @Override
    protected int getHashImpl() {
      return 4123;
    }
  }

  /**
   * Checks to see if the current node has specified type at the specified
   * result, if not it fails to match.
   */
  public static class CheckTypeMatcher extends Matcher {
    private int type;
    private int resNo;

    public CheckTypeMatcher(int ty, int resNo) {
      super(MatcherKind.CheckType);
      this.type = ty;
      this.resNo = resNo;
    }

    public int getResNo() {
      return resNo;
    }

    public int getType() {
      return type;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckType %s, ResNo=%d%n", Util.fixedLengthString(indent, ' '),
          MVT.getEnumName(type), resNo);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckTypeMatcher &&
          ((CheckTypeMatcher) m).getType() == type;
    }

    @Override
    protected int getHashImpl() {
      return type;
    }

    @Override
    protected boolean isContradictoryImpl(Matcher m) {
      if (m instanceof CheckTypeMatcher) {
        CheckTypeMatcher chk = (CheckTypeMatcher) m;
        return typesAreContradictory(getType(), chk.getType());
      }
      return false;
    }
  }

  /**
   * Switch based on the current node's type, dispatching to one matcher per
   * case. If the type doesn't match any of the cases, then the match fails.
   * This is semantically equivalent to a Scope node where every child
   * does a CheckType, but it is much faster.
   */
  public static class SwitchTypeMatcher extends Matcher {

    private ArrayList<Pair<Integer, Matcher>> cases;

    public SwitchTypeMatcher(ArrayList<Pair<Integer, Matcher>> cases) {
      super(MatcherKind.SwitchType);
      this.cases = new ArrayList<>();
      this.cases.addAll(cases);
    }

    public int getNumCases() {
      return cases.size();
    }

    public int getCaseType(int idx) {
      return cases.get(idx).first;
    }

    public Matcher getCaseMatcher(int idx) {
      return cases.get(idx).second;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sSwitchType: {%n", Util.fixedLengthString(indent, ' '));
      for (Pair<Integer, Matcher> itr : cases) {
        os.printf("%scase %s:%n", Util.fixedLengthString(indent, ' '),
            MVT.getEnumName(itr.first));
        itr.second.print(os, indent + 2);
      }
      os.printf("%s}%n", Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return false;
    }

    @Override
    protected int getHashImpl() {
      return 4123;
    }
  }

  /**
   * This checks to see if a child node has the specified type, if not the
   * match will fail.
   */
  public static class CheckChildTypeMatcher extends Matcher {

    private int childNo;
    private int type;

    public CheckChildTypeMatcher(int childNo, int type) {
      super(MatcherKind.CheckChildType);
      this.childNo = childNo;
      this.type = type;
    }

    public int getChildNo() {
      return childNo;
    }

    public int getType() {
      return type;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {

    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (!(m instanceof CheckChildTypeMatcher))
        return false;
      CheckChildTypeMatcher chk = (CheckChildTypeMatcher) m;
      return chk.childNo == childNo && chk.type == type;
    }

    @Override
    protected int getHashImpl() {
      return (type << 3) | childNo;
    }

    @Override
    protected boolean isContradictoryImpl(Matcher m) {
      if (!(m instanceof CheckChildTypeMatcher))
        return false;
      CheckChildTypeMatcher chk = (CheckChildTypeMatcher) m;
      if (chk.childNo != childNo)
        return false;
      return typesAreContradictory(type, chk.type);
    }
  }

  /**
   * This checks to see if the current node is a ConstantSDNode with
   * the specified integer value, if not it fails to match.
   */
  public static class CheckIntegerMatcher extends Matcher {

    private long value;

    public CheckIntegerMatcher(long value) {
      super(MatcherKind.CheckInteger);
      this.value = value;
    }

    public long getValue() {
      return value;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckInteger %d%n", Util.fixedLengthString(indent, ' '), value);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckIntegerMatcher &&
          ((CheckIntegerMatcher) m).value == value;
    }

    @Override
    protected int getHashImpl() {
      return (int) (value >> 32) | (int) value;
    }

    @Override
    protected boolean isContradictoryImpl(Matcher m) {
      if (m instanceof CheckIntegerMatcher)
        return ((CheckIntegerMatcher) m).value == value;
      return false;
    }
  }

  /**
   * This checks to see if the current node is a CondCodeSDNode with
   * the specified condition, if not if fails to match.
   */
  public static class CheckCondCodeMatcher extends Matcher {

    private String condcodeName;

    public CheckCondCodeMatcher(String condcode) {
      super(MatcherKind.CheckCondCode);
      condcodeName = condcode;
    }

    public String getCondcodeName() {
      return condcodeName;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckCondCode %s%n", Util.fixedLengthString(indent, ' '),
          condcodeName);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (m instanceof CheckCondCodeMatcher)
        return ((CheckCondCodeMatcher) m).condcodeName.equals(condcodeName);
      return false;
    }

    @Override
    protected int getHashImpl() {
      return condcodeName.hashCode();
    }
  }

  /**
   * Checks to see if the current node is a VTSDnode with the specified
   * type, if not it fails to match.
   */
  public static class CheckValueTypeMatcher extends Matcher {

    private String typeName;

    public CheckValueTypeMatcher(String type) {
      super(MatcherKind.CheckValueType);
      typeName = type;
    }

    public String getTypeName() {
      return typeName;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckValueType %s%n", Util.fixedLengthString(indent, ' '),
          typeName);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckValueTypeMatcher &&
          ((CheckValueTypeMatcher) m).typeName.equals(typeName);
    }

    @Override
    protected int getHashImpl() {
      return typeName.hashCode();
    }
  }

  /**
   * This node runs the specified ComplexPattern on the current node.
   */
  public static class CheckComplexPatMatcher extends Matcher {

    private ComplexPattern compPat;
    private int matchNumber;
    private String name;
    private int firstResult;

    public CheckComplexPatMatcher(ComplexPattern pat,
                                  int matchNumber,
                                  String name,
                                  int firstResult) {
      super(MatcherKind.CheckComplexPat);
      compPat = pat;
      this.matchNumber = matchNumber;
      this.name = name;
      this.firstResult = firstResult;
    }

    public ComplexPattern getPatern() {
      return compPat;
    }

    public int getMatchNumber() {
      return matchNumber;
    }

    public String getName() {
      return name;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return false;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckCompexPat %s%n", Util.fixedLengthString(indent, ' '),
          compPat.getSelectFunc());
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (!(m instanceof CheckComplexPatMatcher))
        return false;
      CheckComplexPatMatcher chk = (CheckComplexPatMatcher) m;
      return chk.compPat == compPat && chk.matchNumber == matchNumber;
    }

    @Override
    protected int getHashImpl() {
      return compPat.hashCode() ^ matchNumber;
    }

    public int getFirstResult() {
      return firstResult;
    }
  }

  /**
   * This checks to see if the current node is an 'and' with the specified
   * equivalent to the specified immediate.
   */
  public static class CheckAndImmMatcher extends Matcher {

    private long value;

    public CheckAndImmMatcher(long val) {
      super(MatcherKind.CheckAndImm);
      value = val;
    }

    public long getValue() {
      return value;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckAndImm %d%n", Util.fixedLengthString(indent, ' '),
          value);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckAndImmMatcher &&
          ((CheckAndImmMatcher) m).value == value;
    }

    @Override
    protected int getHashImpl() {
      return (int) (value >> 32) | (int) value;
    }
  }

  /**
   * This checks to see if the current node is an 'or' with the specified
   * equivalent to the specified immediate.
   */
  public static class CheckOrImmMatcher extends Matcher {

    private long value;

    public CheckOrImmMatcher(long val) {
      super(MatcherKind.CheckOrImm);
      value = val;
    }

    public long getValue() {
      return value;
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckOrImm %d%n", Util.fixedLengthString(indent, ' '),
          value);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckOrImmMatcher &&
          ((CheckOrImmMatcher) m).value == value;
    }

    @Override
    protected int getHashImpl() {
      return (int) (value >> 32) | (int) value;
    }
  }

  /**
   * This checks to see if the current node(which defines a chain operand) is
   * safe to fold into a large pattern.
   */
  public static class CheckFoldableChainNodeMatcher extends Matcher {

    public CheckFoldableChainNodeMatcher() {
      super(MatcherKind.CheckFoldableChainNode);
    }

    @Override
    public boolean isSafeToRecorderWithPatternPredicate() {
      return true;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sCheckFoldableChainNode%n", Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof CheckFoldableChainNodeMatcher;
    }

    @Override
    protected int getHashImpl() {
      return 0;
    }
  }

  /**
   * This creates a new TargetConstant.
   */
  public static class EmitIntegerMatcher extends Matcher {

    private long val;
    private int ty;

    public EmitIntegerMatcher(long value, int type) {
      super(MatcherKind.EmitInteger);
      val = value;
      ty = type;
    }

    public long getValue() {
      return val;
    }

    public int getVT() {
      return ty;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sEmitInteger %d VT=%d%n", Util.fixedLengthString(indent, ' '),
          val, ty);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (m instanceof EmitIntegerMatcher) {
        EmitIntegerMatcher emit = (EmitIntegerMatcher) m;
        return emit.ty == ty && emit.val == val;
      }
      return false;
    }

    @Override
    protected int getHashImpl() {
      return (int) ((val << 4) | ty);
    }
  }

  /**
   * A target constant whose value is represented as a string.
   */
  public static class EmitStringIntegerMatcher extends Matcher {

    private String value;
    private int vt;

    public EmitStringIntegerMatcher(String val, int vt) {
      super(MatcherKind.EmitStringInteger);
      value = val;
      this.vt = vt;
    }

    public int getVT() {
      return vt;
    }

    public String getValue() {
      return value;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sEmitStringInteger %s VT=%d%n",
          Util.fixedLengthString(indent, ' '), value, vt);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (!(m instanceof EmitStringIntegerMatcher))
        return false;
      EmitStringIntegerMatcher emit = (EmitStringIntegerMatcher) m;
      return emit.value.equals(value) && emit.vt == vt;
    }

    @Override
    protected int getHashImpl() {
      return value.hashCode() ^ vt;
    }
  }

  /**
   * This creates a new TargetConstant.
   */
  public static class EmitRegisterMatcher extends Matcher {

    private Record register;
    private int vt;

    public EmitRegisterMatcher(Record reg, int vt) {
      super(MatcherKind.EmitRegister);
      register = reg;
      this.vt = vt;
    }

    public Record getRegister() {
      return register;
    }

    public int getVT() {
      return vt;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sEmitRegister ", Util.fixedLengthString(indent, ' '));
      if (register != null)
        os.print(register.getName());
      else
        os.print("zero_reg");
      os.printf(" VT=%d%n", vt);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (m instanceof EmitRegisterMatcher) {
        EmitRegisterMatcher emit = (EmitRegisterMatcher) m;
        return emit.register == register && emit.vt == vt;
      }
      return false;
    }

    @Override
    protected int getHashImpl() {
      return (register.hashCode() << 4) | vt;
    }
  }

  /**
   * Emit an operation that reads a specified recorded node and converts it
   * from being a ISD.Constant to ISD.TargetConstant, likewise for ConstantFP.
   */
  public static class EmitConvertToTargetMatcher extends Matcher {

    private int slot;

    public EmitConvertToTargetMatcher(int slot) {
      super(MatcherKind.EmitConvertToTarget);
      this.slot = slot;
    }

    public int getSlot() {
      return slot;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sEmitConvertToTarget %d%n", Util.fixedLengthString(indent, ' '),
          slot);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof EmitConvertToTargetMatcher &&
          ((EmitConvertToTargetMatcher) m).slot == slot;
    }

    @Override
    protected int getHashImpl() {
      return slot;
    }
  }

  /**
   * Emit a node that merges a list of input chains together with a token factor.
   * The list of nodes are the nodes in the matched pattern that have chain
   * input/outputs. This node adds all input chains of these nodes if
   * they are not themselves a node in the pattern.
   */
  public static class EmitMergeInputChainsMatcher extends Matcher {

    /**
     * The index of chained node in recordedNodes list.
     */
    private int[] chainNodes;

    public EmitMergeInputChainsMatcher(int[] chainNodes) {
      super(MatcherKind.EmitMergeInputChains);
      this.chainNodes = new int[chainNodes.length];
      System.arraycopy(chainNodes, 0, this.chainNodes, 0, chainNodes.length);
    }

    public int getNumNodes() {
      return chainNodes.length;
    }

    public int getNode(int idx) {
      return chainNodes[idx];
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sEmitMergeInputChains <todo:args>%n",
          Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof EmitMergeInputChainsMatcher &&
          ((EmitMergeInputChainsMatcher) m).chainNodes == chainNodes;
    }

    @Override
    protected int getHashImpl() {
      int res = 0;
      for (int i : chainNodes)
        res = (res << 3) ^ i;
      return res;
    }
  }

  public static class EmitCopyToRegMatcher extends Matcher {

    /**
     * The index in recordedNodes list that will be copied to the physReg.
     */
    private int srcSlot;
    private Record dstPhysReg;

    public EmitCopyToRegMatcher(int srcSlot, Record dstPhysReg) {
      super(MatcherKind.EmitCopyToReg);
      this.srcSlot = srcSlot;
      this.dstPhysReg = dstPhysReg;
    }

    public int getSrcSlot() {
      return srcSlot;
    }

    public Record getDstPhysReg() {
      return dstPhysReg;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sEmitCopyToReg <todo: args>%d",
          Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (!(m instanceof EmitCopyToRegMatcher))
        return false;
      EmitCopyToRegMatcher emit = (EmitCopyToRegMatcher) m;
      return emit.srcSlot == srcSlot && emit.dstPhysReg == dstPhysReg;
    }

    @Override
    protected int getHashImpl() {
      return srcSlot ^ (dstPhysReg.hashCode() << 4);
    }
  }

  /**
   * Emit an operation that runs on SDNodeXForm on a recorded node and
   * records the result.
   */
  public static class EmitNodeXFromMatcher extends Matcher {

    private int slot;
    private Record nodeXForm;

    public EmitNodeXFromMatcher(int slot, Record nodeXForm) {
      super(MatcherKind.EmitNodeXForm);
      this.slot = slot;
      this.nodeXForm = nodeXForm;
    }

    public int getSlot() {
      return slot;
    }

    public Record getNodeXForm() {
      return nodeXForm;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sEmitNodeXForm %s Slot=%d\n",
          Util.fixedLengthString(indent, ' '), nodeXForm.getName(), slot);
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (!(m instanceof EmitNodeXFromMatcher))
        return false;
      EmitNodeXFromMatcher emit = (EmitNodeXFromMatcher) m;
      return emit.nodeXForm == nodeXForm && slot == emit.slot;
    }

    @Override
    protected int getHashImpl() {
      return slot ^ (nodeXForm.hashCode() << 4);
    }
  }

  public static class EmitNodeMatcherCommon extends Matcher {

    protected String opcodeName;
    protected int[] vts;
    protected int[] operands;
    protected boolean hasChain, hasInFlag, hasOutFlag, hasMemRefs;

    /**
     * If this is a fixed arity node, this is set to -1.
     * If this is a varidic node, this is set to the number of fixed arity
     * operands in the root of the pattern.  The rest are appended to this node.
     */
    protected int numFixedArityOperands;

    public EmitNodeMatcherCommon(String opcodeName,
                                 int[] vts,
                                 int[] operands,
                                 boolean hasChain,
                                 boolean hasInFlag,
                                 boolean hasOutFlag,
                                 boolean hasMemRefs,
                                 int numFixedArityOperands,
                                 boolean isMorphNodeTo) {
      super(isMorphNodeTo ? MatcherKind.MorphNodeTo : MatcherKind.EmitNode);
      this.opcodeName = opcodeName;
      this.vts = new int[vts.length];
      System.arraycopy(vts, 0, this.vts, 0, vts.length);
      this.operands = new int[operands.length];
      System.arraycopy(operands, 0, this.operands, 0, operands.length);
      this.hasChain = hasChain;
      this.hasInFlag = hasInFlag;
      this.hasOutFlag = hasOutFlag;
      this.hasMemRefs = hasMemRefs;
      this.numFixedArityOperands = numFixedArityOperands;
    }

    public String getOpcodeName() {
      return opcodeName;
    }

    public int getNumVTs() {
      return vts.length;
    }

    public int getVT(int idx) {
      return vts[idx];
    }

    public int getNumOperands() {
      return operands.length;
    }

    public int getOperand(int idx) {
      return operands[idx];
    }

    public int[] getVtList() {
      return vts;
    }

    public int[] getOperands() {
      return operands;
    }


    public boolean isHasChain() {
      return hasChain;
    }

    public boolean isHasInFlag() {
      return hasInFlag;
    }

    public boolean isHasOutFlag() {
      return hasOutFlag;
    }

    public boolean isHasMemRefs() {
      return hasMemRefs;
    }

    public int getNumFixedArityOperands() {
      return numFixedArityOperands;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      String indentStr = Util.fixedLengthString(indent, ' ');
      os.print(indentStr);
      os.printf("%s%s: <todo flags>", this instanceof MorphNodeToMatcher ?
          "MorphNodeTo: " : "EmitNode: ", opcodeName);

      for (int vt : vts)
        os.printf(" %s", MVT.getEnumName(vt));
      os.print("(");
      for (int op : operands)
        os.printf("%d ", op);
      os.println(")");
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (!(m instanceof EmitNodeMatcherCommon))
        return false;
      EmitNodeMatcherCommon emit = (EmitNodeMatcherCommon) m;
      return emit.opcodeName.equals(opcodeName) &&
          emit.vts.equals(vts) &&
          emit.operands.equals(operands) &&
          emit.hasChain == hasChain &&
          emit.hasInFlag == hasInFlag &&
          emit.hasOutFlag == hasOutFlag &&
          emit.hasMemRefs == hasMemRefs &&
          emit.numFixedArityOperands == numFixedArityOperands;
    }

    @Override
    protected int getHashImpl() {
      return (opcodeName.hashCode() << 4) | operands.length;
    }
  }

  /**
   * This signals a successful match and generates a node.
   */
  public static class EmitNodeMatcher extends EmitNodeMatcherCommon {
    private int firstResultSlot;

    public EmitNodeMatcher(String opcodeName,
                           int[] vts,
                           int[] operands,
                           boolean hasChain,
                           boolean hasInFlag,
                           boolean hasOutFlag,
                           boolean hasMemRefs,
                           int numFixedArityOperands,
                           int firstResultSlot) {
      super(opcodeName, vts, operands, hasChain,
          hasInFlag, hasOutFlag, hasMemRefs,
          numFixedArityOperands, false);
      this.firstResultSlot = firstResultSlot;
    }

    public int getFirstResultSlot() {
      return firstResultSlot;
    }
  }

  public static class MorphNodeToMatcher extends EmitNodeMatcherCommon {

    private PatternToMatch pattern;

    public MorphNodeToMatcher(String opcodeName,
                              int[] vts,
                              int[] operands,
                              boolean hasChain,
                              boolean hasInFlag,
                              boolean hasOutFlag,
                              boolean hasMemRefs,
                              int numFixedArityOperands,
                              PatternToMatch pattern) {
      super(opcodeName, vts, operands, hasChain,
          hasInFlag, hasOutFlag, hasMemRefs,
          numFixedArityOperands, false);
      this.pattern = pattern;
    }

    public PatternToMatch getPattern() {
      return pattern;
    }
  }

  /**
   * This node indicates which non-root nodes in the pattern produce flags.
   * This allows CompleteMatchMatcher to update them with the output flag
   * of the resultant code.
   */
  public static class MarkFlagResultsMatcher extends Matcher {
    private int[] flagResultNodes;

    public MarkFlagResultsMatcher(int[] nodes) {
      super(MatcherKind.MarkFlagResults);
      this.flagResultNodes = new int[nodes.length];
      System.arraycopy(nodes, 0, flagResultNodes, 0, nodes.length);
    }

    public int getNode(int idx) {
      return flagResultNodes[idx];
    }

    public int getNumNodes() {
      return flagResultNodes.length;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      os.printf("%sMarkFlagResults <todo args>", Util.fixedLengthString(indent, ' '));
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      return m instanceof MarkFlagResultsMatcher &&
          ((MarkFlagResultsMatcher) m).flagResultNodes.equals(flagResultNodes);
    }

    @Override
    protected int getHashImpl() {
      int res = 0;
      for (int i : flagResultNodes)
        res = (res << 4) ^ i;
      return res;
    }
  }

  /**
   * Complete a match by replacing the results of the pattern with
   * the newly generated nodes. this also prints a comment indicating
   * the source and result patterns.
   */
  public static class CompleteMatchMatcher extends Matcher {

    private int[] results;
    private PatternToMatch pattern;

    public CompleteMatchMatcher(int[] results, PatternToMatch pat) {
      super(MatcherKind.CompleteMatch);
      this.results = new int[results.length];
      System.arraycopy(results, 0, this.results, 0, results.length);
      pattern = pat;
    }

    public int getNumResults() {
      return results.length;
    }

    public int getResult(int idx) {
      return results[idx];
    }

    public PatternToMatch getPattern() {
      return pattern;
    }

    @Override
    protected void printImpl(PrintStream os, int indent) {
      String indentStr = Util.fixedLengthString(indent, ' ');
      os.printf("%sCompleteMatch <todo args>%n", indentStr);
      os.printf("%sSrc= ", indentStr);
      pattern.getSrcPattern().print(os);
      os.println();
      os.printf("%sDst= ", indentStr);
      pattern.getDstPattern().print(os);
      os.println();
    }

    @Override
    protected boolean isEqualImpl(Matcher m) {
      if (!(m instanceof CompleteMatchMatcher))
        return false;
      CompleteMatchMatcher complete = (CompleteMatchMatcher) m;
      return complete.results.equals(results) &&
          complete.pattern.equals(complete.pattern);
    }

    @Override
    protected int getHashImpl() {
      int res = 0;
      for (int i : results)
        res = (res << 3) ^ i;
      return res ^ (pattern.hashCode() << 8);
    }
  }
}
