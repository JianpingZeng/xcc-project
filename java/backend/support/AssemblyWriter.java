/*
 * Extremely C language Compiler.
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

package backend.support;

import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.*;
import backend.value.GlobalValue.LinkageType;
import backend.value.GlobalValue.VisibilityTypes;
import backend.value.Instruction.*;
import backend.value.Instruction.CmpInst.Predicate;
import gnu.trove.iterator.TObjectIntIterator;
import tools.*;

import java.io.PrintStream;
import java.util.ArrayList;

import static backend.support.AssemblyWriter.PrefixType.*;
import static tools.APFloat.RoundingMode.rmNearestTiesToEven;

public class AssemblyWriter {
  private FormattedOutputStream out;
  private Module theModule;
  private TypePrinting typePrinter;
  private ArrayList<Type> numberedTypes;
  private SlotTracker slotTracker;
  private final static int PadToColumns = 50;

  public AssemblyWriter(FormattedOutputStream os, Module m, SlotTracker tracker) {
    out = os;
    theModule = m;
    typePrinter = new TypePrinting();
    numberedTypes = new ArrayList<>();
    slotTracker = tracker;
    if (m != null)
      typePrinter.incorporateType(m);
  }

  enum PrefixType {
    GlobalPrefix, LabelPrefix, LocalPrefix, NoPrefix
  }

  private static void printLLVMName(FormattedOutputStream os, Value val) {
    printLLVMName(os, val.getName(),
        val instanceof GlobalValue ? GlobalPrefix : LocalPrefix);
  }

  private static void printLLVMName(PrintStream os, Value val) {
    printLLVMName(os, val.getName(),
        val instanceof GlobalValue ? GlobalPrefix : LocalPrefix);
  }

  /**
   * Turn the specified name into "LLVM name", which is either
   * prefixed with % or is surrounded with ""'s. Print it now.
   *
   * @param os
   * @param name
   * @param pt
   */
  static void printLLVMName(PrintStream os,
                            String name,
                            PrefixType pt) {
    Util.assertion(name != null && !name.isEmpty(), "Cannot get empty name!");
    switch (pt) {
      default:
        Util.assertion(false, "Unknown PrefixType");
        break;
      case NoPrefix:
        break;
      case GlobalPrefix:
        os.print("@");
        break;
      case LocalPrefix:
      case LabelPrefix:
        os.print("%");
        break;
    }

    boolean needQuotes = Character.isDigit(name.charAt(0));
    if (!needQuotes) {
      for (int i = 0, e = name.length(); i != e; i++) {
        char c = name.charAt(i);
        if (c != '_' && c != '.' && !Character.isJavaIdentifierPart(c)) {
          needQuotes = true;
          break;
        }
      }
    }

    if (!needQuotes) {
      os.print(name);
      return;
    }
    os.printf("\"%s\"", name);
  }

  /**
   * Turn the specified name into "LLVM name", which is either
   * prefixed with % or is surrounded with ""'s. Print it now.
   *
   * @param os
   * @param name
   * @param pt
   */
  static void printLLVMName(FormattedOutputStream os,
                            String name,
                            PrefixType pt) {
    Util.assertion(name != null && !name.isEmpty(), "Cannot get empty name!");
    switch (pt) {
      default:
        Util.assertion(false, "Unknown PrefixType");
        break;
      case NoPrefix:
      case LabelPrefix:
        break;
      case GlobalPrefix:
        os.print("@");
        break;
      case LocalPrefix:
        os.print("%");
        break;
    }

    boolean needQuotes = Character.isDigit(name.charAt(0));
    if (!needQuotes) {
      for (int i = 0, e = name.length(); i != e; i++) {
        char c = name.charAt(i);
        if (c != '_' && c != '.' && !Character.isJavaIdentifierPart(c)) {
          needQuotes = true;
          break;
        }
      }
    }

    if (name.charAt(0) == '"' && name.charAt(name.length()-1) == '"')
      needQuotes = false;
    if (!needQuotes) {
      os.print(name);
      return;
    }
    os.printf("\"%s\"", name);
  }

  public void write(Module m) {
    printModule(m);
  }

  public void write(Function fn) {
    printFunction(fn);
  }

  public void write(GlobalValue gv) {
    if (gv instanceof GlobalVariable) {
      printGlobal((GlobalVariable) gv);
    } else {
      Util.assertion(gv instanceof Function, "Unknown global value kind");
      printFunction((Function) gv);
    }
  }

  public void write(Instruction inst) {
    printInstruction(inst);
  }

  public void write(BasicBlock bb) {
    printBasicBlock(bb);
  }

  /**
   * Output all global variables into ouput stream.
   *
   * @param gv
   */
  private void printGlobal(GlobalVariable gv) {
    writeAsOperandInternal(out, gv, typePrinter, slotTracker);
    out.print(" = ");

    if (!gv.hasInitializer() && gv.hasExternalLinkage())
      out.print("external ");

    printLinkage(gv.getLinkage(), out);
    printVisibility(gv.getVisibility(), out);

    int addressSpace = gv.getType().getAddressSpace();
    if (addressSpace != 0)
      out.printf("addrspace(%d) ", addressSpace);
    out.print(gv.isConstant() ? "constant " : "global ");
    typePrinter.print(gv.getType().getElementType(), out);

    if (gv.hasInitializer()) {
      out.print(" ");
      writeOperand(gv.getInitializer(), false);
    }
    int align = gv.getAlignment();
    if (align != 0) {
      out.printf(", align %d", align);
    }
    printInfoComment(gv);
    out.println();
  }

  private void printInfoComment(Value val) {
    if (!val.getType().equals(LLVMContext.VoidTy)) {
      out.padToColumn(PadToColumns);
      out.print("; <");
      typePrinter.print(val.getType(), out);
      // output number of uses.
      out.printf("> [#uses=%d]", val.getNumUses());
    }
  }

  public static SlotTracker createSlotTracker(Value val) {
    if (val instanceof Argument) {
      return new SlotTracker(((Argument) val).getParent());
    }
    if (val instanceof BasicBlock) {
      BasicBlock bb = (BasicBlock) val;
      return new SlotTracker(bb.getParent());
    }
    if (val instanceof Instruction) {
      Instruction inst = (Instruction) val;
      return new SlotTracker(inst.getParent().getParent());
    }
    if (val instanceof GlobalVariable) {
      GlobalVariable gv = (GlobalVariable) val;
      return new SlotTracker(gv.getParent());
    }
    if (val instanceof Function) {
      return new SlotTracker((Function) val);
    }
    return null;
  }

  public static void writeAsOperandInternal(PrintStream out, Value val,
                                            TypePrinting printer, SlotTracker tracker) {
    if (val.hasName()) {
      printLLVMName(out, val);
      return;
    }

    Constant cv = val instanceof Constant ? (Constant) val : null;
    if (cv != null && !(cv instanceof GlobalValue)) {
      Util.assertion(printer != null, "Constants require TypePrintering");
      writeConstantInternal(out, cv, printer, tracker);
      return;
    }

    char prefix = '%';
    int slot = -1;
    if (tracker == null)
      tracker = createSlotTracker(val);

    if (tracker != null) {
      GlobalValue gv = val instanceof GlobalValue ?
          (GlobalValue) val :
          null;
      if (gv != null) {
        slot = tracker.getGlobalSlot(gv);
        prefix = '@';
      } else {
        slot = tracker.getLocalSlot(val);
      }
    }
    if (slot != -1)
      out.printf("%c%d", prefix, slot);
    else
      out.print("<badref>");
  }

  public static void writeAsOperandInternal(FormattedOutputStream out, Value val,
                                            TypePrinting printer, SlotTracker tracker) {
    if (val.hasName()) {
      printLLVMName(out, val);
      return;
    }

    Constant cv = val instanceof Constant ? (Constant) val : null;
    if (cv != null && !(cv instanceof GlobalValue)) {
      Util.assertion(printer != null, "Constants require TypePrintering");
      writeConstantInternal(out, cv, printer, tracker);
      return;
    }

    char prefix = '%';
    int slot = -1;
    if (tracker == null)
      tracker = createSlotTracker(val);

    if (tracker != null) {
      GlobalValue gv = val instanceof GlobalValue ?
          (GlobalValue) val :
          null;
      if (gv != null) {
        slot = tracker.getGlobalSlot(gv);
        prefix = '@';
      } else {
        slot = tracker.getLocalSlot(val);
      }
    }
    if (slot != -1)
      out.printf("%c%d", prefix, slot);
    else
      out.print("<badref>");
  }

  //===----------------------------------------------------------------------===//
  // Helper Functions
  //===----------------------------------------------------------------------===//
  private static Module getModuleFromVal(Value val) {
    if (val instanceof Argument) {
      Argument arg = (Argument) val;
      return arg.getParent() != null ? arg.getParent().getParent() : null;
    }
    if (val instanceof BasicBlock) {
      BasicBlock bb = (BasicBlock) val;
      return bb.getParent() != null ? bb.getParent().getParent() : null;
    }
    if (val instanceof Instruction) {
      Instruction inst = (Instruction) val;
      Function f = inst.getParent() != null ?
          inst.getParent().getParent() :
          null;
      return f != null ? f.getParent() : null;
    }
    if (val instanceof GlobalValue)
      return ((GlobalValue) val).getParent();
    return null;
  }

  public static void writeAsOperand(PrintStream out, Value val,
                                    boolean printType, Module context) {
    if (!printType && (!(val instanceof Constant)) || val.hasName() || val instanceof GlobalValue) {
      writeAsOperandInternal(out, val, null, null);
      return;
    }

    if (context == null)
      context = getModuleFromVal(val);

    TypePrinting printer = new TypePrinting();
    if (context != null)
      printer.incorporateType(context);
    if (printType) {
      printer.print(val.getType(), out);
      out.print(" ");
    }
    writeAsOperandInternal(out, val, printer, null);
  }

  public static void writeAsOperand(FormattedOutputStream out, Value val,
                                    boolean printType, Module context) {
    if (!printType && (!(val instanceof Constant)) || val.hasName() || val instanceof GlobalValue) {
      writeAsOperandInternal(out, val, null, null);
      return;
    }

    if (context == null)
      context = getModuleFromVal(val);

    TypePrinting printer = new TypePrinting();
    if (context != null)
      printer.incorporateType(context);
    if (printType) {
      printer.print(val.getType(), out);
      out.print(" ");
    }
    writeAsOperandInternal(out, val, printer, null);
  }

  public static void writeConstantInternal(PrintStream out, Constant cv,
                                           TypePrinting printer, SlotTracker tracker) {
    ConstantInt ci = cv instanceof ConstantInt ? (ConstantInt) cv : null;
    if (ci != null) {
      if (ci.getType().equals(LLVMContext.Int1Ty)) {
        out.print(ci.getZExtValue() != 0 ? "true" : "false");
        return;
      }
      ci.getValue().print(out);
      return;
    }

    ConstantFP fp = (cv instanceof ConstantFP) ? (ConstantFP) cv : null;
    if (fp != null) {
      if (fp.getValueAPF().getSemantics() == APFloat.IEEEdouble
          || fp.getValueAPF().getSemantics() == APFloat.IEEEsingle) {
        boolean ignored = false;
        boolean isDouble = fp.getValueAPF().getSemantics() == APFloat.IEEEdouble;
        double val = isDouble ? fp.getValueAPF().convertToDouble() :
            fp.getValueAPF().convertToFloat();
        String strVal = String.valueOf(val);

        if ((strVal.charAt(0) >= '0' && strVal.charAt(0) <= '9')
            || (strVal.charAt(0) == '-' || strVal.charAt(0) == '+')
            && (strVal.charAt(0) >= '0' && strVal.charAt(0) <= '9')) {
          if (Double.parseDouble(strVal) == val) {
            out.print(strVal);
            return;
          }
        }

        APFloat apf = fp.getValueAPF();
        if (!isDouble) {
          OutRef<Boolean> x = new OutRef<>(false);
          apf.convert(APFloat.IEEEdouble, rmNearestTiesToEven, x);
          ignored = x.get();
        }
        out.printf("0x%d", apf.bitcastToAPInt().getZExtValue());
        return;
      }

      // Some form of long double.  These appear as a magic letter identifying
      // the type, then a fixed number of hex digits.
      out.print("0x");
      if (fp.getValueAPF().getSemantics() == APFloat.x87DoubleExtended) {
        out.print("K");
        APInt api = fp.getValueAPF().bitcastToAPInt();
        long[] p = api.getRawData();
        long word = p[1];
        int width = api.getBitWidth();
        int shiftcount = 12;
        for (int j = 0; j < width; j += 4, shiftcount -= 4) {
          int nibble = (int) ((word >> shiftcount) & 15);
          if (nibble < 10)
            out.print((char) (nibble + '0'));
          else {
            out.print((char) (nibble - 10 + 'A'));
          }
          if (shiftcount == 0 && j + 4 < width) {
            word = p[0];
            shiftcount = 64;
            if (width - j - 4 < 64) {
              shiftcount = width - j - 4;
            }
          }
        }
        return;
      } else if (fp.getValueAPF().getSemantics() == APFloat.IEEEquad) {
        out.print("L");
      } else {
        Util.shouldNotReachHere("Unsupported floating point type");
      }

      APInt api = fp.getValueAPF().bitcastToAPInt();
      long[] p = api.getRawData();
      int idx = 0;
      long word = p[idx];
      int shiftcount = 60;
      int width = api.getBitWidth();
      for (int j = 0; j < width; j += 4, shiftcount -= 4) {
        int nibble = (int) ((word >> shiftcount) & 15);
        if (nibble < 10)
          out.print((char) (nibble + '0'));
        else {
          out.print((char) (nibble - 10 + 'A'));
        }
        if (shiftcount == 0 && j + 4 < width) {
          word = p[++idx];
          shiftcount = 64;
          if (width - j - 4 < 64) {
            shiftcount = width - j - 4;
          }
        }
      }
      return;
    }

    if (cv instanceof ConstantAggregateZero) {
      out.print("zeroinitializer");
      return;
    }

    if (cv instanceof ConstantArray) {
      ConstantArray ca = (ConstantArray) cv;
      Type elty = ca.getType().getElementType();
      if (ca.isString()) {
        out.print("c\"");
        out.print(Util.escapedString(ca.getAsString()));
        out.print("\"");
      } else {
        out.print("[");
        if (ca.getNumOfOperands() != 0) {
          printer.print(elty, out);
          out.print(' ');
          writeAsOperandInternal(out, ca.operand(0), printer, tracker);
          for (int i = 1, e = ca.getNumOfOperands(); i != e; i++) {
            out.print(", ");
            printer.print(elty, out);
            out.print(' ');
            writeAsOperandInternal(out, ca.operand(i), printer,
                tracker);
          }
        }
        out.print("]");
      }
      return;
    }

    if (cv instanceof ConstantStruct) {
      ConstantStruct cs = (ConstantStruct) cv;
      if (cs.getType().isPacked())
        out.print('<');
      out.print('{');
      int n = cs.getNumOfOperands();
      if (n > 0) {
        out.print(' ');
        printer.print(cs.operand(0).getType(), out);
        out.print(' ');

        writeAsOperandInternal(out, cs.operand(0), printer, tracker);
        for (int i = 1; i < n; i++) {
          out.print(", ");
          printer.print(cs.operand(i).getType(), out);
          out.print(' ');

          writeAsOperandInternal(out, cs.operand(i), printer, tracker);
        }
        out.print(' ');
      }

      out.print('}');
      if (cs.getType().isPacked())
        out.print('>');
      return;
    }

    if (cv instanceof ConstantPointerNull) {
      out.print("null");
      return;
    }
    if (cv instanceof Value.UndefValue) {
      out.print("undef");
      return;
    }
    if (cv instanceof ConstantExpr) {
      ConstantExpr ce = (ConstantExpr) cv;
      out.print(ce.getOpcode().opName);
      writeOptimizationInfo(out, ce);
      if (ce.isCompare()) {
        out.printf(" %s", getPredicateText(ce.getPredicate()));
      }
      out.print(" (");

      for (int i = 0, e = ce.getNumOfOperands(); i != e; i++) {
        printer.print(ce.operand(i).getType(), out);
        out.print(' ');
        writeAsOperandInternal(out, ce.operand(i), printer, tracker);
        if (i < e - 1)
          out.print(", ");
      }

      if (ce.isCast()) {
        out.print(" to ");
        printer.print(ce.getType(), out);
      }
      out.print(")");
      return;
    }

    out.print("<placeholder or erroneous Constant>");
  }

  public static void writeConstantInternal(FormattedOutputStream out, Constant cv,
                                           TypePrinting printer, SlotTracker tracker) {
    ConstantInt ci = cv instanceof ConstantInt ? (ConstantInt) cv : null;
    if (ci != null) {
      if (ci.getType().equals(LLVMContext.Int1Ty)) {
        out.print(ci.getZExtValue() != 0 ? "true" : "false");
        return;
      }
      ci.getValue().print(out);
      return;
    }

    ConstantFP fp = (cv instanceof ConstantFP) ? (ConstantFP) cv : null;
    if (fp != null) {
      if (fp.getValueAPF().getSemantics() == APFloat.IEEEdouble
          || fp.getValueAPF().getSemantics() == APFloat.IEEEsingle) {
        boolean ignored = false;
        boolean isDouble = fp.getValueAPF().getSemantics() == APFloat.IEEEdouble;
        double val = isDouble ? fp.getValueAPF().convertToDouble() :
            fp.getValueAPF().convertToFloat();
        String strVal = String.valueOf(val);

        if ((strVal.charAt(0) >= '0' && strVal.charAt(0) <= '9')
            || (strVal.charAt(0) == '-' || strVal.charAt(0) == '+')
            && (strVal.charAt(0) >= '0' && strVal.charAt(0) <= '9')) {
          if (Double.parseDouble(strVal) == val) {
            out.print(strVal);
            return;
          }
        }

        APFloat apf = fp.getValueAPF();
        if (!isDouble) {
          OutRef<Boolean> x = new OutRef<>(false);
          apf.convert(APFloat.IEEEdouble, rmNearestTiesToEven, x);
          ignored = x.get();
        }
        out.printf("0x%x", apf.bitcastToAPInt().getZExtValue());
        return;
      }

      // Some form of long double.  These appear as a magic letter identifying
      // the type, then a fixed number of hex digits.
      out.printf("0x");
      if (fp.getValueAPF().getSemantics() == APFloat.x87DoubleExtended) {
        out.printf("K");
        APInt api = fp.getValueAPF().bitcastToAPInt();
        long[] p = api.getRawData();
        long word = p[1];
        int width = api.getBitWidth();
        int shiftcount = 12;
        for (int j = 0; j < width; j += 4, shiftcount -= 4) {
          int nibble = (int) ((word >> shiftcount) & 15);
          if (nibble < 10)
            out.print((char) (nibble + '0'));
          else {
            out.print((char) (nibble - 10 + 'A'));
          }
          if (shiftcount == 0 && j + 4 < width) {
            word = p[0];
            shiftcount = 64;
            if (width - j - 4 < 64) {
              shiftcount = width - j - 4;
            }
          }
        }
        return;
      } else if (fp.getValueAPF().getSemantics() == APFloat.IEEEquad) {
        out.printf("L");
      } else {
        Util.shouldNotReachHere("Unsupported floating point type");
      }

      APInt api = fp.getValueAPF().bitcastToAPInt();
      long[] p = api.getRawData();
      int idx = 0;
      long word = p[idx];
      int shiftcount = 60;
      int width = api.getBitWidth();
      for (int j = 0; j < width; j += 4, shiftcount -= 4) {
        int nibble = (int) ((word >> shiftcount) & 15);
        if (nibble < 10)
          out.print((char) (nibble + '0'));
        else {
          out.print((char) (nibble - 10 + 'A'));
        }
        if (shiftcount == 0 && j + 4 < width) {
          word = p[++idx];
          shiftcount = 64;
          if (width - j - 4 < 64) {
            shiftcount = width - j - 4;
          }
        }
      }
      return;
    }

    if (cv instanceof ConstantAggregateZero) {
      out.printf("zeroinitializer");
      return;
    }

    if (cv instanceof ConstantArray) {
      ConstantArray ca = (ConstantArray) cv;
      Type elty = ca.getType().getElementType();
      if (ca.isString()) {
        out.print("c\"");
        out.print(Util.escapedString(ca.getAsString()));
        out.print("\"");
      } else {
        out.print("[");
        if (ca.getNumOfOperands() != 0) {
          printer.print(elty, out);
          out.print(' ');
          writeAsOperandInternal(out, ca.operand(0), printer, tracker);
          for (int i = 1, e = ca.getNumOfOperands(); i != e; i++) {
            out.print(", ");
            printer.print(elty, out);
            out.print(' ');
            writeAsOperandInternal(out, ca.operand(i), printer,
                tracker);
          }
        }
        out.print("]");
      }
      return;
    }

    if (cv instanceof ConstantStruct) {
      ConstantStruct cs = (ConstantStruct) cv;
      if (cs.getType().isPacked())
        out.print('<');
      out.print('{');
      int n = cs.getNumOfOperands();
      if (n > 0) {
        out.print(' ');
        printer.print(cs.operand(0).getType(), out);
        out.print(' ');

        writeAsOperandInternal(out, cs.operand(0), printer, tracker);
        for (int i = 1; i < n; i++) {
          out.print(", ");
          printer.print(cs.operand(i).getType(), out);
          out.print(' ');

          writeAsOperandInternal(out, cs.operand(i), printer, tracker);
        }
        out.print(' ');
      }

      out.print('}');
      if (cs.getType().isPacked())
        out.print('>');
      return;
    }

    if (cv instanceof ConstantPointerNull) {
      out.print("null");
      return;
    }
    if (cv instanceof Value.UndefValue) {
      out.print("undef");
      return;
    }
    if (cv instanceof ConstantExpr) {
      ConstantExpr ce = (ConstantExpr) cv;
      out.print(ce.getOpcode().opName);
      writeOptimizationInfo(out, ce);
      if (ce.isCompare()) {
        out.printf(" %s", getPredicateText(ce.getPredicate()));
      }
      out.printf(" (");

      for (int i = 0, e = ce.getNumOfOperands(); i != e; i++) {
        printer.print(ce.operand(i).getType(), out);
        out.print(' ');
        writeAsOperandInternal(out, ce.operand(i), printer, tracker);
        if (i < e - 1)
          out.printf(", ");
      }

      if (ce.isCast()) {
        out.printf(" to ");
        printer.print(ce.getType(), out);
      }
      out.printf(")");
      return;
    }

    out.printf("<placeholder or erroneous Constant>");
  }

  private static String getPredicateText(Predicate pred) {
    String res = "unknown";
    switch (pred) {
      case FCMP_FALSE:
        res = "false";
        break;
      case FCMP_OEQ:
        res = "oeq";
        break;
      case FCMP_OGT:
        res = "ogt";
        break;
      case FCMP_OGE:
        res = "oge";
        break;
      case FCMP_OLT:
        res = "olt";
        break;
      case FCMP_OLE:
        res = "ole";
        break;
      case FCMP_ONE:
        res = "one";
        break;
      case FCMP_ORD:
        res = "ord";
        break;
      case FCMP_UNO:
        res = "uno";
        break;
      case FCMP_UEQ:
        res = "ueq";
        break;
      case FCMP_UGT:
        res = "ugt";
        break;
      case FCMP_UGE:
        res = "uge";
        break;
      case FCMP_ULT:
        res = "ult";
        break;
      case FCMP_ULE:
        res = "ule";
        break;
      case FCMP_UNE:
        res = "une";
        break;
      case FCMP_TRUE:
        res = "true";
        break;
      case ICMP_EQ:
        res = "eq";
        break;
      case ICMP_NE:
        res = "ne";
        break;
      case ICMP_SGT:
        res = "sgt";
        break;
      case ICMP_SGE:
        res = "sge";
        break;
      case ICMP_SLT:
        res = "slt";
        break;
      case ICMP_SLE:
        res = "sle";
        break;
      case ICMP_UGT:
        res = "ugt";
        break;
      case ICMP_UGE:
        res = "uge";
        break;
      case ICMP_ULT:
        res = "ult";
        break;
      case ICMP_ULE:
        res = "ule";
        break;
    }
    return res;
  }

  private static void writeOptimizationInfo(FormattedOutputStream out, Value val) {
    // TODO: 2017/10/10
  }

  private static void writeOptimizationInfo(PrintStream out, Value val) {
    // TODO: 2017/10/10
  }

  private static void printLinkage(LinkageType linkage,
                                   FormattedOutputStream out) {
    switch (linkage) {
      case ExternalLinkage:
        break;
      case InternalLinkage:
        out.printf("internal ");
        break;
      case PrivateLinkage:
        out.printf("private ");
        break;
      case LinkerPrivateLinkage:
        out.printf("linker_private ");
        break;
      case CommonLinkage:
        out.printf("common ");
        break;
    }
  }

  private static void printVisibility(VisibilityTypes vt,
                                      FormattedOutputStream out) {
    switch (vt) {
      default:
        Util.assertion(false, "Invalid visibility style");
      case DefaultVisibility:
        break;
      case HiddenVisibility:
        out.printf("hidden ");
        break;
      case ProtectedVisibility:
        out.printf("protected ");
        break;
    }
  }

  private void writeOperand(Value operand, boolean printType) {
    if (operand == null) {
      out.print("<null operand!>");
    } else {
      if (printType) {
        typePrinter.print(operand.getType(), out);
        out.print(" ");
      }
      writeAsOperandInternal(out, operand, typePrinter, slotTracker);
    }
  }

  private void printFunction(Function f) {
    out.println();
    if (f.isDeclaration())
      out.print("declare ");
    else
      out.print("define ");

    printLinkage(f.getLinkage(), out);
    printVisibility(f.getVisibility(), out);

    // print out the calling convention.
    switch (f.getCallingConv()) {
      case C:
        break;  // default.
      case Fast:
        out.print("fastcc ");
        break;
      case Cold:
        out.print("coldcc ");
        break;
      case X86_StdCall:
        out.print("x86_stdcallcc ");
        break;
      case X86_FastCall:
        out.print("x86_fastcallcc ");
        break;
      default:
        out.print("cc " + f.getCallingConv().name());
        break;
    }

    FunctionType ft = f.getFunctionType();
    typePrinter.print(ft.getReturnType(), out);
    out.print(' ');
    writeAsOperandInternal(out, f, typePrinter, slotTracker);
    out.print('(');
    slotTracker.incorporateFunction(f);

    // Loop over all function arguments, print them.
    if (!f.isDeclaration()) {
      for (int i = 0, e = f.getNumOfArgs(); i != e; i++) {
        if (i != 0)
          out.print(", ");
        printArgument(f.argAt(i));
      }
    } else {
      // Otherwise, just print the argument type if this function is a
      // declaration.
      for (int i = 0, e = ft.getNumParams(); i != e; i++) {
        if (i != 0)
          out.print(", ");
        typePrinter.print(ft.getParamType(i), out);
      }
    }
    // Print the ... for variadic function.
    if (f.isVarArg()) {
      if (ft.getNumParams() != 0)
        out.print(",...");
      out.print("...");
    }

    out.print(')');
    if (f.getAlignment() != 0) {
      out.printf(" align %d", f.getAlignment());
    }
    if (f.isDeclaration())
      out.println();
    else {
      out.print(" {");

      // Output all basic blocks.
      for (BasicBlock bb : f.getBasicBlockList()) {
        printBasicBlock(bb);
      }
      out.println("}");
    }

    slotTracker.pruneFunction();
  }

  private void printArgument(Argument arg) {
    typePrinter.print(arg.getType(), out);
    if (arg.hasName()) {
      out.print(' ');
      printLLVMName(out, arg);
    }
  }

  private void printBasicBlock(BasicBlock bb) {
    if (bb.hasName()) {
      out.println();
      printLLVMName(out, bb.getName(), LabelPrefix);
      out.print(':');
    } else if (!bb.isUseEmpty()) {
      out.printf("\n; <label>:");
      int slot = slotTracker.getLocalSlot(bb);
      if (slot != -1)
        out.print(slot);
      else
        out.print("<badref>");
    }

    if (bb.getParent() == null) {
      out.print("; Error: Block without parent!");
    } else if (!bb.equals(bb.getParent().getEntryBlock())) {
      out.padToColumn(PadToColumns);
      // not the entry block.
      out.print(";");
      int numOfPreds = bb.getNumPredecessors();
      if (numOfPreds == 0)
        out.print(" No predecessors!");
      else {
        out.print(" preds = ");
        for (int i = 0; i != numOfPreds; i++) {
          if (i != 0)
            out.print(", ");
          writeOperand(bb.predAt(i), false);
        }
      }
    }

    out.println();

    // Emit each instruction in the basic block.
    for (Instruction inst : bb) {
      printInstruction(inst);
      out.println();
    }
  }

  /**
   * Emit the instruction information.
   *
   * @param inst
   */
  private void printInstruction(Instruction inst) {
    // print out indentation for each instruction.
    out.print(' ');

    if (inst.hasName()) {
      printLLVMName(out, inst);
      out.printf(" = ");
    } else if (!inst.getType().equals(LLVMContext.VoidTy)) {
      int slot = slotTracker.getLocalSlot(inst);
      if (slot == -1) {
        out.print("<badref> = ");
      } else {
        out.printf("%%%d = ", slot);
      }
    }

    // if this is a volatile store or load instruction,
    // just print out the volatile marker.
    if (inst instanceof LoadInst && ((LoadInst) inst).isVolatile()
        || (inst instanceof StoreInst) && ((StoreInst) inst).isVolatile()) {
      out.print("volatile ");
    }
    Operator opc = inst.getOpcode();
    //if (opc != Operator.Store && opc != Operator.Ret && opc != Operator.Br)
    //    out.print(" = ");

    // Print the instruction operator name.
    out.print(inst.getOpcodeName());

    writeOptimizationInfo(out, inst);

    if (inst instanceof CmpInst) {
      CmpInst ci = (CmpInst) inst;
      out.printf(" %s", getPredicateText(ci.getPredicate()));
    }

    // print out the type of operands.
    Value operand = inst.getNumOfOperands() != 0 ? inst.operand(0) : null;

    // Special handling for BranchInst, SwitchInst etc.
    if (inst instanceof BranchInst && ((BranchInst) inst).isConditional()) {
      BranchInst bi = (BranchInst) inst;
      out.print(' ');
      writeOperand(bi.getCondition(), true);
      out.print(", ");
      writeOperand(bi.getSuccessor(0), true);
      out.print(", ");
      writeOperand(bi.getSuccessor(1), true);
    } else if (inst instanceof SwitchInst) {
      out.print(' ');
      writeOperand(operand, true);
      out.print(", ");
      writeOperand(inst.operand(1), true);
      out.println(" [");

      for (int i = 2, e = inst.getNumOfOperands(); i < e; i += 2) {
        out.print("  ");
        writeOperand(inst.operand(i), true);
        out.print(", ");
        writeOperand(inst.operand(i + 1), true);
        out.println();
      }
      out.print(" ]");
    } else if (inst instanceof PhiNode) {
      out.print(' ');
      typePrinter.print(inst.getType(), out);
      out.print(' ');

      for (int op = 0, e = inst.getNumOfOperands(); op != e; op += 2) {
        if (op != 0)
          out.print(", ");
        out.print("[ ");
        writeOperand(inst.operand(op), false);
        out.print(", ");
        writeOperand(inst.operand(op + 1), false);
        out.print(" ]");
      }
    } else if (inst instanceof ReturnInst && operand == null) {
      out.print(" void");
    } else if (inst instanceof ExtractValueInst) {
      out.print(' ');
      writeOperand(inst.operand(0), true);
      ExtractValueInst evi = (ExtractValueInst) inst;
      for (int idx : evi.getIndices()) {
        out.printf(", %d", idx);
      }
    } else if (inst instanceof InsertValueInst) {
      out.print(' ');
      writeOperand(inst.operand(0), true);
      out.print(", ");
      writeOperand(inst.operand(1), true);
      InsertValueInst ivi = (InsertValueInst) inst;
      for (int idx : ivi.getIndices()) {
        out.printf(", %d", idx);
      }
    } else if (inst instanceof CallInst) {
      Util.assertion(operand != null, "No called function for CallInst");

      CallInst ci = (CallInst) inst;
      CallingConv cc = ci.getCallingConv();
      switch (cc) {
        case C:
          break;
        case Fast:
          out.print(" fastcc");
          break;
        case Cold:
          out.print(" coldcc");
          break;
        case X86_StdCall:
          out.print(" x86_stdcallcc");
          break;
        case X86_FastCall:
          out.print(" x86_fastcallcc");
          break;
        default:
          out.print(" cc" + cc.name());
          break;
      }

      PointerType pty = (PointerType) operand.getType();
      FunctionType fty = (FunctionType) pty.getElementType();
      Type retTy = fty.getReturnType();

      out.print(' ');

      if (!fty.isVarArg() && (!(retTy instanceof PointerType)
          || !(((PointerType) (retTy)).getElementType() instanceof FunctionType))) {
        typePrinter.print(retTy, out);
        out.print(' ');
        writeOperand(operand, false);
      } else {
        writeOperand(operand, true);
      }
      out.print('(');
      for (int op = 1, e = inst.getNumOfOperands(); op != e; op++) {
        if (op > 1)
          out.print(", ");
        writeParamOperand(inst.operand(op));
      }
      out.print(')');
    } else if (inst instanceof AllocaInst) {
      AllocaInst ai = (AllocaInst) inst;
      out.print(' ');
      typePrinter.print(ai.getType().getElementType(), out);

      if (ai.getArraySize() != null && ai.isArrayAllocation()) {
        out.print(", ");
        writeOperand(ai.getArraySize(), true);
      }
    } else if (inst instanceof CastInst) {
      if (operand != null) {
        out.print(" ");
        writeOperand(operand, true);
      }
      out.print(" to ");
      typePrinter.print(inst.getType(), out);
    } else if (operand != null) {
      // Print normal instruction.
      boolean printAllTypes = false;
      Type theType = operand.getType();

      if (inst instanceof StoreInst || inst instanceof ReturnInst) {
        printAllTypes = true;
      } else {
        for (int i = 1, e = inst.getNumOfOperands(); i != e; i++) {
          operand = inst.operand(i);
          if (operand != null && !operand.getType().equals(theType)) {
            printAllTypes = true;
            break;
          }
        }
      }
      if (!printAllTypes) {
        out.print(' ');
        typePrinter.print(theType, out);
      }

      out.print(' ');
      for (int i = 0, e = inst.getNumOfOperands(); i != e; i++) {
        if (i != 0)
          out.print(", ");
        writeOperand(inst.operand(i), printAllTypes);
      }
    }

    // print post operand alignment for load/store.
    int align = 0;
    if (inst instanceof LoadInst
        && (align = ((LoadInst) inst).getAlignment()) != 0) {
      out.printf(" ,align %d", align);
    } else if (inst instanceof StoreInst
        && (align = ((StoreInst) inst).getAlignment()) != 0) {
      out.printf(" ,align %d", align);
    }
    printInfoComment(inst);
  }

  private void writeParamOperand(Value op) {
    if (op == null) {
      out.print("<null operand!>");
    } else {
      // print argument tpye.
      typePrinter.print(op.getType(), out);
      out.print(' ');
      writeAsOperandInternal(out, op, typePrinter, slotTracker);
    }
  }

  public void printModule(Module m) {
    if (m.getModuleIdentifier() != null && !m.getModuleIdentifier().isEmpty()) {
      out.printf(";ModuleID = '%s'\n", m.getModuleIdentifier());
    }

    if (m.getDataLayout() != null && !m.getDataLayout().isEmpty()) {
      out.printf("target datalayout = %s\n", m.getDataLayout());
    }
    if (m.getTargetTriple() != null && !m.getTargetTriple().isEmpty()) {
      out.printf("target triple = %s\n", m.getTargetTriple());
    }
    if (m.getModuleInlineAsm() != null && !m.getModuleInlineAsm().isEmpty()) {
      String[] temp = m.getModuleInlineAsm().split("\\n");
      out.print("\nmodule asm ");
      for (String str : temp) {
        if (!str.isEmpty()) {
          out.printf("\"%s\"\n", str);
        }
      }
    }

    // Loop over all symbol, emitting all id's types.
    if (!m.getTypeSymbolTable().isEmpty() || !numberedTypes.isEmpty())
      out.println();

    printTypeIdentities();

    // Emitting all globals.
    if (!m.getGlobalVariableList().isEmpty())
      out.println();

    for (GlobalVariable gv : m.getGlobalVariableList()) {
      printGlobal(gv);
    }

    // Emitting all functions.
    for (Function f : m.getFunctionList()) {
      printFunction(f);
    }
  }

  private void printTypeIdentities() {
    if (theModule.getTypeSymbolTable().isEmpty() &&
        numberedTypes.isEmpty())
          return;

    // Emit all numbered types.
    StructType[] numberedTypes = new StructType[typePrinter.numberedTypes.size()];
    typePrinter.numberedTypes.forEachEntry((key, val) -> {
      numberedTypes[val] = key;
      return true;
    });

    for (int i = 0, e = numberedTypes.length; i != e; i++) {
      out.printf("%%%d = type ", i);

      typePrinter.printStructBody(numberedTypes[i], out);
      out.println();
    }

    // print named types.

    for (StructType sty : typePrinter.namedTypes) {
      printLLVMName(out, sty.getName(), LocalPrefix);
      out.print(" = type ");

      typePrinter.printStructBody(sty, out);
      out.println();
    }
  }

  public static void writeMDNodes(FormattedOutputStream os,
                                  TypePrinting printer,
                                  SlotTracker slotTable) {
    MDNode[] nodes = new MDNode[slotTable.getMdnMap().size()];
    TObjectIntIterator<Value> itr = slotTable.getMdnMap().iterator();
    while (itr.hasNext()) {
      nodes[itr.value()] = (MDNode) itr.key();
    }

    for (int i = 0; i < nodes.length; i++) {
      os.printf("!%d = metadata ", i);
      MDNode node = nodes[i];
      os.printf("!{");
      for (int j = 0, e = node.getNumOfNode(); j < e; j++) {
        Value val = node.getNode(j);
        if (val == null) os.printf("null");
        else if (val instanceof MDNode) {
          MDNode n = (MDNode) val;
          os.printf("metadata !%d", slotTable.getMetadataSlot(n));
        } else {
          printer.print(val.getType(), os);
          os.print(' ');
          writeAsOperandInternal(os, val, printer, slotTable);
        }
        if (j < e - 1)
          os.print(", ");
      }
      os.println("}");
    }
  }
}
