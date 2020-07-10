/*
 * Extremely Compiler Collection
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.value;

import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.StructType;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static backend.value.ValueKind.InlineAsmVal;

/**
 * This class is designed to provide an abstract on Inline assembly widely used by
 * several production compiler, such as GCC and Clang. In general, the inline assembly
 * can only be used as a callee called by 'call' or 'invoke' instruction, which accepts
 * the same number of arguments specified in the constraints string list.
 */
public class InlineAsm extends Value {
  private static class InlineAsmKeyType {
    private String asmString, constraintString;
    private boolean hasSideEffects;
    private boolean isAlignStack;

    private InlineAsmKeyType(String asmString,
                             String constraintString,
                             boolean hasSideEffects,
                             boolean isAlignStack) {
      this.asmString = asmString;
      this.constraintString = constraintString;
      this.hasSideEffects = hasSideEffects;
      this.isAlignStack = isAlignStack;
    }

    @Override
    public int hashCode() {
      return (asmString.hashCode() << 23) | (constraintString.hashCode() << 17) |
          ((hasSideEffects ? 1 : 0) << 13) | (isAlignStack ? 1 : 0) << 7;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (obj == this) return true;
      if (getClass() != obj.getClass()) return false;
      InlineAsmKeyType key = (InlineAsmKeyType) obj;
      return Objects.equals(asmString, key.asmString) &&
          Objects.equals(constraintString, key.constraintString) &&
          hasSideEffects == key.hasSideEffects &&
          isAlignStack == key.isAlignStack;
    }
  }

  private static final HashMap<InlineAsmKeyType, InlineAsm>
      uniqueInlineAsmMap = new HashMap<>();

  private String asmString, constraintString;
  private boolean hasSideEffects;
  private boolean isAlignStack;

  private InlineAsm(PointerType pty,
                    String asmString,
                    String constraintString,
                    boolean hasSideEffects,
                    boolean isAlignStack) {
    super(pty, InlineAsmVal);
    this.asmString = asmString;
    this.constraintString = constraintString;
    this.hasSideEffects = hasSideEffects;
    this.isAlignStack = isAlignStack;
    Util.assertion(verify(getFunctionType(),
        constraintString),
        "Function type not legal for constraints");
  }

  public String getAsmString() {
    return asmString;
  }

  public String getConstraintString() {
    return constraintString;
  }

  public boolean hasSideEffects() {
    return hasSideEffects;
  }

  public boolean isAlignStack() {
    return isAlignStack;
  }

  // Constraints string parsing.
  private enum ConstraintPrefix {
    IsInput,  // 'x'
    IsOutput, // '=x'
    IsClobber,// '~x'
  }

  private static class ConstraintInfo {
    /**
     * the basic type of the constraint: input, output or clobber.
     */
    ConstraintPrefix type;

    /**
     * "&": output operand writes result before inputs are all
     * read. This is only ever set for an output operand.
     */
    boolean isEarlyClobber;

    int matchingInput;

    boolean hasMatchingInput() {
      return matchingInput != -1;
    }

    boolean isCommutative;

    boolean isIndirect;

    ArrayList<String> codes;

    private ConstraintInfo() {
      type = ConstraintPrefix.IsInput;
      isEarlyClobber = false;
      matchingInput = -1;
      isCommutative = false;
      isIndirect = false;
      codes = new ArrayList<>();
    }

    boolean parse(String str, ArrayList<ConstraintInfo> constraintsSoFar) {
      int i = 0, e = str.length();
      if (str.charAt(i) == '~') {
        type = ConstraintPrefix.IsClobber;
        ++i;
      }
      else if (str.charAt(i) == '=') {
        ++i;
        type = ConstraintPrefix.IsOutput;
      }

      if (str.charAt(i) == '*') {
        isIndirect = true;
        ++i;
      }

      // just prefix, no other constraints.
      if (i == e) return true;

      // parse modifiers
      boolean doneWithModifiers = false;
      while (!doneWithModifiers) {
        switch (str.charAt(i)) {
          default:
            doneWithModifiers = true;
            break;
          case '&':
            // Early clobber.
            if (type != ConstraintPrefix.IsOutput ||
                isEarlyClobber)
              return true;
            isEarlyClobber = true;
            break;
          case '%':
            if (type == ConstraintPrefix.IsClobber ||
                isCommutative)
              return true;
            isCommutative = true;
            break;
          case '#': // comment
          case '*':
            // register preferences.
            return true;
        }

        if (!doneWithModifiers) {
          ++i;
          if (i == e) return true;
        }
      }
      // Parse the various constraints.
      while (i != e) {
        if (str.charAt(i) == '{') {
          // Physical register reference.
          // Find the end of the register name.
          int constraintEnd = str.indexOf(i, '}');
          if (constraintEnd == -1) return true; // no '}' and only '{foo'
          codes.add(str.substring(i, constraintEnd));
          i = constraintEnd + 1;
        }
        else if (Character.isDigit(str.charAt(i))) {
          // Maximal munch numbers.
          int numStart = i;
          while (i != e && Character.isDigit(str.charAt(i)))
            ++i;
          String res = str.substring(numStart, i);
          codes.add(res);
          int n = Integer.valueOf(res);
          if (n >= constraintsSoFar.size() || constraintsSoFar.get(n).type != ConstraintPrefix.IsOutput ||
              type != ConstraintPrefix.IsInput)
            return true;

          // If Operand N already has a matching input, reject this.  An output
          // can't be constrained to the same value as multiple inputs.
          if (constraintsSoFar.get(n).hasMatchingInput())
            return true;

          constraintsSoFar.get(n).matchingInput = constraintsSoFar.size();
        }
        else {
          // Single letter constraint.
          codes.add(str.substring(i, i+1));
          ++i;
        }
      }
      return false;
    }
  }

  private static ArrayList<ConstraintInfo> parseConstraints(String constraintString) {
    ArrayList<ConstraintInfo> result = new ArrayList<>();
    for (int i = 0, e = constraintString.length(); i < e; i++) {
      ConstraintInfo info = new ConstraintInfo();
      int constraintEnd = constraintString.indexOf(',', i);
      if (constraintEnd == i //  like ',,' string.
           || info.parse(constraintString.substring(i, constraintEnd), result)) {
        // error
        result.clear();
        break;
      }

      result.add(info);

      i = constraintEnd;
      if (i != e) {
        ++i;
        // don't allow "xyz,"
        if (i == e) { result.clear(); break; }
      }
    }
    return result;
  }

  private ArrayList<ConstraintInfo> parseConstraints() {
    return parseConstraints(constraintString);
  }

  /**
   * Verify that the specified constraint string is reasonable for the
   * specified function type, and otherwise validate the constraint string.
   * @param functionType
   * @param constraintString
   * @return
   */
  private boolean verify(FunctionType functionType, String constraintString) {
    if (functionType.isVarArg())
      return false;

    ArrayList<ConstraintInfo> constraints = parseConstraints(constraintString);

    // error parsing constraints.
    if (constraints.isEmpty() && !constraintString.isEmpty())
      return false;

    int numOutputs = 0, numInputs = 0, numClobbers = 0;
    int numIndirect = 0;

    for (int i = 0, e = constraints.size(); i < e; i++) {
      switch (constraints.get(i).type) {
        case IsOutput:
          // outputs before inputs and clobbers.
          if ((numIndirect - numIndirect) != 0 || numClobbers != 0)
            return false;

          if (!constraints.get(i).isIndirect) {
            ++numOutputs;
            break;
          }
          ++numIndirect;
          // fall through for indirect outputs.
        case IsInput:
          if (numClobbers != 0) return false;
          ++numIndirect;
          break;
        case IsClobber:
          ++numClobbers;
          break;
      }
    }

    switch (numOutputs) {
      case 0:
        if (!functionType.getReturnType().isVectorTy())
          return false;
        break;
      case 1:
        if (functionType.getReturnType().isStructType())
          return false;
        break;
      case 2:
        if (!(functionType.getReturnType() instanceof StructType))
          return false;

        StructType sty = (StructType) functionType.getReturnType();
        if (sty.getNumOfElements() != numOutputs)
          return false;
        break;
    }
    return functionType.getNumParams() == numInputs;
  }

  private FunctionType getFunctionType() {
    return (FunctionType) ((PointerType)getType()).getElementType();
  }

  public static Value get(FunctionType fnType,
                          String asmString,
                          String constraintString,
                          boolean hasSideEffects,
                          boolean isAlignStack) {
    InlineAsmKeyType key = new InlineAsmKeyType(asmString, constraintString,
        hasSideEffects, isAlignStack);
    if (uniqueInlineAsmMap.containsKey(key))
      return uniqueInlineAsmMap.get(key);
    InlineAsm val = new InlineAsm(PointerType.get(fnType, 0),
        asmString, constraintString, hasSideEffects, isAlignStack);
    uniqueInlineAsmMap.put(key, val);
    return val;
  }

  // These are helper methods for dealing with flags in the INLINEASM SDNode
  // in the backend.

  private static final int Op_InputChain = 0,
      Op_AsmString = 1,
      Op_MDNode = 2,
      Op_IsAlignStack = 3,
      Op_FirstOperand = 4,
      Kind_RegUse = 1,
      Kind_RegDef = 2,
      Kind_Imm = 3,
      Kind_Mem = 4,
      Kind_RegDefEarlyClobber = 6,
      Flag_MatchingOperand = 0x80000000;

  private static int getFlagWord(int kind, int numOps) {
    Util.assertion(((numOps << 3) & ~0xffff) == 0, "Two many inline operands");
    return kind | (numOps << 3);
  }

  private static int getFlagWordForMatchingOp(int inputFlag, int matchingOperandno) {
    return inputFlag | Flag_MatchingOperand | (matchingOperandno << 16);
  }

  private static int getKind(int flags) {
    return flags & 7;
  }

  private static boolean isRegDefKind(int flag) { return flag == Kind_RegDef; }
  private static boolean isImmKind(int flag) { return getKind(flag) == Kind_Imm; }
  private static boolean isMemKind(int flag) { return getKind(flag) == Kind_Mem; }
  private boolean isRegDefEarlyClobberKind(int flag) {
    return getKind(flag) == Kind_RegDefEarlyClobber;
  }

  private static int getNumOperandRegisters(int flag) {
    return (flag & 0xffff) >>> 3;
  }

  private boolean isUseOperandTiedToDef(int flag, OutRef<Integer> idx) {
    idx.set(0);
    if ((flag & Flag_MatchingOperand) == 0)
      return false;
    idx.set((flag & ~Flag_MatchingOperand) >>> 16);
    return true;
  }
}
