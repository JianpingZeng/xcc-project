/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.debug;

import backend.support.LLVMContext;
import backend.type.Type;
import backend.value.ConstantInt;
import backend.value.MDNode;
import backend.value.Value;
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

  public static DebugLoc getUnknownLoc() { return new DebugLoc(); }

  public static DebugLoc get(int line, int col,
                             MDNode scope) {
    return get(line, col, scope, null);
  }

  public static DebugLoc get(int line, int col,
                             MDNode scope,
                             MDNode inlineAt) {
    DebugLoc result = new DebugLoc();
    // if there is no such scope where this debug loc is attached,
    // return an unknown debug loc.
    if (scope == null) return result;

    // saturate line and col
    if (col > 255) col = 0;
    if (line >= (1 << 24)) line = 0;
    result.lineCol = line | (col << 24);

    LLVMContext ctx = scope.getContext();
    // If there is no inlined-at location, use the ScopeRecords array.
    if (inlineAt == null)
      result.scopeIdx = ctx.getOrAddScopeRecordIdxEntry(scope, 0);
    else
      result.scopeIdx = ctx.getOrAddScopeInlinedAtIdxEntry(scope, inlineAt, 0);
    return result;
  }

  /**
   * Translate the DILocation quad into a DebugLoc.
   * @param n
   * @return
   */
  public static DebugLoc getFromDILocation(MDNode n) {
    if (n == null || n.getNumOfOperands() != 4)
      return new DebugLoc();

    if (!(n.operand(2) instanceof MDNode))
      return new DebugLoc();
    MDNode scope = (MDNode)n.operand(2);
    int lineNo = 0, colNo = 0;
    if (n.operand(0) instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) n.operand(0);
      lineNo = (int) ci.getZExtValue();
    }
    if (n.operand(1) instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) n.operand(1);
      colNo = (int) ci.getZExtValue();
    }
    MDNode ia = n.operand(3) != null && n.operand(3) instanceof MDNode ? (MDNode)n.operand(3) : null;
    return get(lineNo, colNo, scope, ia);
  }

  public boolean isUnknown() {
    return scopeIdx == 0;
  }

  public int getLine() {
    return (lineCol << 8) >>> 8;
  }

  public int getCol() {
    return lineCol >>> 24;
  }

  /**
   * This returns the scope pointer for this DebugLoc, or null if invalid.
   * @param ctx
   * @return
   */
  public MDNode getScope(LLVMContext ctx) {
    if (scopeIdx == 0) return null;
    if (scopeIdx > 0) {
      Util.assertion(Integer.compareUnsigned(scopeIdx, ctx.scopeRecords.size()) <= 0,
          "Invalid scopeIdx!");
      return ctx.scopeRecords.get(scopeIdx-1);
    }
    Util.assertion(Integer.compareUnsigned(-scopeIdx, ctx.scopeRecords.size()) <= 0,
        "Invalid scopeIdx!");
    return ctx.scopeRecords.get(-scopeIdx-1);
  }

  public MDNode getInlineAt(LLVMContext ctx) {
    if (scopeIdx >= 0) return null;
    Util.assertion(Integer.compareUnsigned(-scopeIdx,
        ctx.scopeInlineAtRecords.size()) <= 0, "Invalid scopeIdx!");
    return ctx.scopeInlineAtRecords.get(-scopeIdx - 1).second;
  }

  public void getScopeAndInlinedAt(OutRef<MDNode> scope,
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

  public MDNode getAsMDNode(LLVMContext ctx) {
    if (isUnknown()) return null;
    OutRef<MDNode> scope = new OutRef<>();
    OutRef<MDNode> ia = new OutRef<>();
    getScopeAndInlinedAt(scope, ia, ctx);
    Util.assertion(scope.get() != null, "If scope is null, this should be isUnknown");
    LLVMContext ctx2 = scope.get().getContext();
    Type int32 = Type.getInt32Ty(ctx2);
    Value[] elts = new Value[] {
        ConstantInt.get(int32, getLine()),
        ConstantInt.get(int32, getCol()),
        scope.get(),
        ia.get()
    };
    return MDNode.get(ctx2, elts);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    DebugLoc loc = (DebugLoc)obj;
    return lineCol == loc.lineCol && scopeIdx == loc.scopeIdx;
  }
}
