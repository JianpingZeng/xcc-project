package utils.tablegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
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

import tools.Util;

/**
 * This is an abstraction that represents the predicates on a PatFrag node.
 * @author Jianping Zeng.
 * @version 0.4
 */
class TreePredicateFn {
  /**
   * This is a TreePatern for the PatFrag that it originally come from.
   */
  private TreePattern patFragRec;

  TreePredicateFn(TreePattern tp) {
    patFragRec = tp;
    Util.assertion(getImmCode().isEmpty() || getPredCode().isEmpty(),
        ".td file corrupt; can't have a node predicate and an imm predicate!");
  }

  TreePattern getOrigPatFragRecord() { return patFragRec; }

  boolean isAlwaysTrue() {
    return getPredCode().isEmpty() && getImmCode().isEmpty();
  }

  boolean isImmediatePattern() { return !getImmCode().isEmpty(); }

  String getImmediatePredicateCode() {
    String result = getImmCode();
    Util.assertion(!result.isEmpty(), "Isn't an immediate pattern!");
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (getClass() != obj.getClass()) return false;
    TreePredicateFn pred = (TreePredicateFn) obj;
    return patFragRec.equals(pred.patFragRec);
  }

  @Override
  public int hashCode() {
    return patFragRec.hashCode();
  }

  String getFnName() {
    return "predicate_" + patFragRec.getRecord().getName();
  }

  String getCodeToRunOnSDNode() {
    // Handle the immediate predicates first.
    String immCode = getImmCode();
    if (!immCode.isEmpty()) {
      String result = "        long imm = ((ConstantSDNode)node).getSExtValue();";
      return result + immCode;
    }

    Util.assertion(!getPredCode().isEmpty(), "don't have any predicate code!");
    String className;
    if (patFragRec.getOnlyTree().isLeaf()){
      className = "SDNode";
    }
    else {
      Record op = patFragRec.getOnlyTree().getOperator();
      className = patFragRec.getDAGPatterns().getSDNodeInfo(op).getSDClassName();
    }
    String result;
    if (className.equals("SDNode"))
      result = "        SDNode n = node;\n";
    else
      result = String.format("        %s n = (%s)node;\n", className, className);
    return result + getPredCode();
  }

  private String getPredCode() {
    return patFragRec.getRecord().getValueAsCode("PredicateCode");
  }
  private String getImmCode() {
    return patFragRec.getRecord().getValueAsCode("ImmediateCode");
  }

  @Override
  public String toString() {
    return getFnName();
  }
}
