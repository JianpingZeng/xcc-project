/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.support;

import backend.value.MDNode;
import tools.OutRef;
import tools.Util;

/**
 * Debug location id. This is carried by Instruction, SDNode, and
 * MachineInstr to compactly encode file/line/scope information for
 * an operation.
 */
public class DebugLoc {
  /**
   * This is 32 bit value encodes the line and column number for
   * the location, encoded as 24 bits for line and 8 bits for col.
   * A value of 0 for either meanas unknown.
   */
  int lineCol;
  /**
   * This is an opaque ID for scope/inlineAttr information,
   * decoded by LLVMContext. 0 means unknown.
   */
  int scopeIdx;

  public DebugLoc() {
    lineCol = 0;
    scopeIdx = 0;
  }

  public static DebugLoc get(int line, int col,
                             MDNode scope,
                             MDNode inlineAt) {

  }

  public static DebugLoc getFromDILocation(MDNode n) {}

  public boolean isUnknown() {
    return scopeIdx == 0;
  }

  public int getLine() {
    return (lineCol << 8) >>> 8;
  }

  public int getCol() {
    return lineCol >>> 24;
  }

  public MDNode getScope() {

  }

  public MDNode getInlineAt() {}

  public void getScopeAndInlineAt(OutRef<MDNode> scope,
                                  OutRef<MDNode> ia,
                                  LLVMContext ctx) {
    if (scopeIdx == 0) {
      scope.set(null);
      ia.set(null);
      return;
    }

    if (scopeIdx > 0) {
      // Positive ScopeIdx is an index into ScopeRecords, which has no inlined-at
      // position specified.
      Util.assertion(Integer.compareUnsigned(scopeIdx,
          ctx.scopeRecords.size()) <= 0, "Invalid scopeIdx!");
      scope.set(ctx.scopeRecords.get(scopeIdx-1));
      ia.set(null);
      return;
    }

    // Otherwise, the index is in the ScopeInlinedAtRecords array.
    Util.assertion(Integer.compareUnsigned(-scopeIdx,
        ctx.scopeInlineAtRecords.size()) <= 0, "Invalid scopeIdx!");
    scope.set(ctx.scopeInlineAtRecords.get(-scopeIdx-1).first);
    ia.set(ctx.scopeInlineAtRecords.get(-scopeIdx-1).second);
  }

  public MDNode getAsMDNode() {}

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    DebugLoc loc = (DebugLoc)obj;
    return lineCol == loc.lineCol && scopeIdx == loc.scopeIdx;
  }
}
