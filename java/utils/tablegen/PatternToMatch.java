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

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * @author Jianping Zeng
 * @version 0.1
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
    StringBuilder predicateCheck = new StringBuilder();
    TreeSet<Predicate> predLists = new TreeSet<>();
    predLists.addAll(preds);

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
}
