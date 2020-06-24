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

import java.util.Objects;

/**
 * This class represents a condition that has to be satisfied for a pattern
 * to be tried. It is a generalization of a class "Pattern" from Target.td:
 * in addition to the Target.td's predicates, this class can also represent
 * conditions associated with HW modes. Both types will eventually become
 * strings containing Java code to be executed, the difference is in how
 * these strings are generated.
 *
 * @author Jianping Zeng.
 * @version 0.4
 */
class Predicate implements Comparable<Predicate> {
  /**
   * Predicate definition from .td file, null for HW modes.
   */
  Record def;
  /**
   * Feature string for HW mode.
   */
  String features;
  /**
   * The boolean value that condition has to evaluate to for this predicate.
   */
  boolean ifCond;
  /**
   * Does this predicate correspond to HW mode.
   */
  boolean isHwMode;

  public Predicate(Record r, boolean c) {
    def = r;
    ifCond = c;
    isHwMode = false;
    Util.assertion(r.isSubClassOf("Predicate"),
        "Predicate objects should only be created for records derived" +
            "from Predicate class");
  }

  public Predicate(String fs, boolean c) {
    features = fs;
    ifCond = c;
    isHwMode = true;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass())
      return false;
    Predicate p = (Predicate) obj;
    return ifCond == p.ifCond && isHwMode == p.isHwMode &&
        Objects.equals(def, p.def);
  }

  @Override
  public int compareTo(Predicate o) {
    if (isHwMode != o.isHwMode)
      return isHwMode ? 1 : -1;
    Util.assertion((def == null) == (o.def == null),
        "Inconsistency between Def and IsHwMode");
    if (ifCond != o.ifCond)
      return ifCond ? 1 : -1;
    if (def != null)
      return Record.LessRecord.compare(def, o.def);
    return features.compareTo(o.features);
  }

  @Override
  public String toString() {
    return getCondString();
  }

  public void dump() {
    System.err.println(toString());
  }

  /**
   * Returns Java code fragment for checking on condition.
   *
   * @return
   */
  public String getCondString() {
    String res = isHwMode ? "mf.getSubtarget().checkFeatures(\"" +
        features + "\")" : def.getValueAsString("CondString");
    return ifCond ? res : "!(" + res + ")";
  }
}
